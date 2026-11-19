"""Capture the current local FITOS DB as the demo reset baseline."""

import argparse
import os

from reset_demo import CONTAINER, DATABASE, DB_USER, SNAPSHOT, docker, verify_target


def capture(replace=False):
    if SNAPSHOT.exists() and not replace:
        raise RuntimeError("Demo baseline already exists; pass --replace to overwrite it")
    verify_target()
    SNAPSHOT.parent.mkdir(exist_ok=True)
    docker("exec", CONTAINER, "pg_dump", "-U", DB_USER, "-d", DATABASE,
           "-Fc", "-f", "/tmp/fitos-demo-capture.dump")
    temporary = SNAPSHOT.with_suffix(".dump.tmp")
    docker("cp", f"{CONTAINER}:/tmp/fitos-demo-capture.dump", str(temporary))
    docker("cp", str(temporary), f"{CONTAINER}:/tmp/fitos-demo-check.dump")
    listing = docker("exec", CONTAINER, "pg_restore", "-l", "/tmp/fitos-demo-check.dump")
    if "flyway_schema_history" not in listing or "users" not in listing:
        temporary.unlink(missing_ok=True)
        raise RuntimeError("Captured dump is missing the FITOS schema")
    os.replace(temporary, SNAPSHOT)
    print("Demo baseline captured at .dsh/demo-baseline.dump")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--replace", action="store_true", help="Overwrite the existing demo baseline")
    args = parser.parse_args()
    capture(args.replace)
