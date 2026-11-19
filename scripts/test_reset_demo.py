"""Destructive local integration test: mutate demo data, then restore baseline."""

import os
import subprocess
import sys
import unittest
from pathlib import Path

from seed_demo import get, patch, post


ROOT = Path(__file__).resolve().parents[1]


@unittest.skipUnless(os.getenv("FITOS_RUN_RESET_TEST") == "1", "Explicit local reset test only")
class ResetDemoTest(unittest.TestCase):
    def test_restores_modified_and_new_rows(self):
        admin_password = os.environ["FITOS_ADMIN_PASSWORD"]
        auth = post("/auth/login", {
            "email": "admin@fitos.com", "password": admin_password, "studioId": 1,
        })
        token = auth["accessToken"]
        leads = get("/leads?size=100", token)["content"]
        huy = next(item for item in leads if item["email"] == "lead.huy.demo@example.com")
        if huy["status"] != "TRIAL":
            patch(f"/leads/{huy['id']}/status", {"status": "TRIAL", "notes": "reset test"}, token)
        if not any(item["email"] == "reset.probe@example.com" for item in leads):
            post("/leads", {
                "fullName": "Reset Probe", "phone": "0000000999",
                "email": "reset.probe@example.com", "notes": "Reset test row",
            }, token)

        result = subprocess.run(
            [sys.executable, str(ROOT / "scripts" / "reset_demo.py"), "--yes"],
            cwd=ROOT, capture_output=True, text=True, timeout=120,
        )
        self.assertEqual(result.returncode, 0, result.stderr[-1000:])

        auth = post("/auth/login", {
            "email": "admin@fitos.com", "password": admin_password, "studioId": 1,
        })
        leads = get("/leads?size=100", auth["accessToken"])["content"]
        self.assertEqual(next(item for item in leads if item["email"] == "lead.huy.demo@example.com")["status"],
                         "CONTACTED")
        self.assertFalse(any(item["email"] == "reset.probe@example.com" for item in leads))


if __name__ == "__main__":
    unittest.main()
