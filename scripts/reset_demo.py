"""Restore the local FITOS database from .dsh/demo-baseline.dump.

This replaces every row and schema object in the dedicated local `fitos` DB.
Run only with `--yes`; the dashboard's local-only API calls this script.
"""

import argparse
import shutil
import subprocess
import sys
import time
from pathlib import Path
from urllib.error import URLError
from urllib.request import urlopen


ROOT = Path(__file__).resolve().parents[1]
SNAPSHOT = ROOT / ".dsh" / "demo-baseline.dump"
CONTAINER = "fitos-postgres"
DATABASE = "fitos"
DB_USER = "fitos_user"


def docker(*args):
    result = subprocess.run(["docker", *args], capture_output=True, text=True, timeout=60)
    if result.returncode:
        raise RuntimeError(f"Docker command failed: {' '.join(args[:2])}: {result.stderr[-400:]}")
    return result.stdout.strip()


def verify_target():
    if not shutil.which("docker"):
        raise RuntimeError("Docker CLI is unavailable")
    service = docker("inspect", "--format", '{{ index .Config.Labels "com.docker.compose.service" }}', CONTAINER)
    project = docker("inspect", "--format", '{{ index .Config.Labels "com.docker.compose.project" }}', CONTAINER)
    if service != "postgres" or project != "fitos":
        raise RuntimeError("Target container is not the FITOS PostgreSQL service")
    current = docker("exec", CONTAINER, "psql", "-U", DB_USER, "-d", "postgres", "-At", "-c",
                     f"SELECT datname FROM pg_database WHERE datname = '{DATABASE}'")
    if current != DATABASE:
        raise RuntimeError("FITOS database was not found")


def restore():
    if not SNAPSHOT.is_file() or SNAPSHOT.stat().st_size < 1024:
        raise RuntimeError("Demo baseline dump is missing or empty")
    verify_target()
    docker("cp", str(SNAPSHOT), f"{CONTAINER}:/tmp/fitos-demo-restore.dump")
    listing = docker("exec", CONTAINER, "pg_restore", "-l", "/tmp/fitos-demo-restore.dump")
    if "flyway_schema_history" not in listing or "users" not in listing:
        raise RuntimeError("Demo baseline dump does not contain the FITOS schema")

    docker("exec", CONTAINER, "psql", "-U", DB_USER, "-d", "postgres", "-v", "ON_ERROR_STOP=1",
           "-c", f"DROP DATABASE {DATABASE} WITH (FORCE)")
    docker("exec", CONTAINER, "psql", "-U", DB_USER, "-d", "postgres", "-v", "ON_ERROR_STOP=1",
           "-c", f"CREATE DATABASE {DATABASE} OWNER {DB_USER}")
    docker("exec", CONTAINER, "pg_restore", "-U", DB_USER, "-d", DATABASE,
           "--no-owner", "--no-acl", "--exit-on-error", "/tmp/fitos-demo-restore.dump")

    for _ in range(30):
        try:
            with urlopen("http://localhost:18081/api/studios", timeout=2) as response:
                if response.status == 200:
                    print("Demo database restored; backend is ready")
                    return
        except (URLError, TimeoutError):
            pass
        time.sleep(1)
    raise RuntimeError("Database restored, but backend did not become ready")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--yes", action="store_true", help="Confirm replacement of the local FITOS database")
    args = parser.parse_args()
    if not args.yes:
        parser.error("Pass --yes to replace the local FITOS database")
    try:
        restore()
    except Exception as error:
        print(f"Demo restore failed: {error}", file=sys.stderr)
        raise SystemExit(1)
