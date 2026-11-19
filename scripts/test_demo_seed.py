"""Integration check for the optional local demo data."""

import json
import os
import unittest
from datetime import date, timedelta
from pathlib import Path
from urllib.request import Request, urlopen


API_BASE = os.getenv("FITOS_API_BASE", "http://localhost:18081/api").rstrip("/")


def api(path, token=None, payload=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    data = json.dumps(payload).encode() if payload is not None else None
    with urlopen(Request(f"{API_BASE}{path}", data=data, headers=headers), timeout=15) as response:
        return json.load(response)


@unittest.skipUnless(os.getenv("FITOS_ADMIN_PASSWORD"), "FITOS_ADMIN_PASSWORD is required")
class DemoSeedTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        auth = api("/auth/login", payload={
            "email": "admin@fitos.com",
            "password": os.environ["FITOS_ADMIN_PASSWORD"],
            "studioId": 1,
        })
        cls.token = auth["accessToken"]

    def test_demo_accounts_and_business_data(self):
        users = api("/users", self.token)
        emails = {item["email"] for item in users}
        self.assertTrue({
            "manager.demo@example.com", "reception.demo@example.com",
            "reception.mai.demo@example.com", "reception.khoa.demo@example.com",
            "sarah.mitchell@fitos.com", "linh.demo@example.com",
            "minh.demo@example.com", "an.demo@example.com",
            "hoa.demo@example.com", "phuc.demo@example.com",
            "quynh.demo@example.com", "tuan.demo@example.com", "bao.demo@example.com",
        }.issubset(emails))
        self.assertGreaterEqual(len(users), 14)
        for email in ("reception.demo@example.com", "reception.mai.demo@example.com",
                      "reception.khoa.demo@example.com"):
            self.assertIn("ROLE_RECEPTIONIST", next(item for item in users if item["email"] == email)["roles"])

        leads = api("/leads?size=100", self.token)["content"]
        self.assertGreaterEqual(sum("[FITOS DEMO]" in (item.get("notes") or "") for item in leads), 17)
        lead_statuses = {item["email"]: item["status"] for item in leads}
        self.assertEqual(lead_statuses["lead.huy.demo@example.com"], "CONTACTED")
        self.assertEqual(lead_statuses["lead.vy.demo@example.com"], "INTERESTED")
        self.assertEqual(lead_statuses["lead.nam.demo@example.com"], "TRIAL")
        self.assertEqual(lead_statuses["lead.ha.demo@example.com"], "NEGOTIATING")
        receptionist_ids = {item["id"] for item in users if "ROLE_RECEPTIONIST" in item["roles"]}
        self.assertGreaterEqual(len({item.get("assignedToUserId") for item in leads
                                     if "[FITOS DEMO]" in (item.get("notes") or "")}
                                    & receptionist_ids), 3)

        customers = api("/customers?size=100", self.token)["content"]
        self.assertTrue({"linh.demo@example.com", "minh.demo@example.com", "an.demo@example.com",
                         "hoa.demo@example.com", "phuc.demo@example.com", "quynh.demo@example.com",
                         "tuan.demo@example.com", "bao.demo@example.com"}.issubset(
            {item["email"] for item in customers}))

        orders = api("/orders?size=100", self.token)["content"]
        self.assertGreaterEqual(sum(item["status"] == "PAID" and "[FITOS DEMO]" in (item.get("notes") or "")
                                    for item in orders), 10)

        start = date.today()
        sessions = api(f"/sessions?from={start}&to={start + timedelta(days=14)}", self.token)
        self.assertGreaterEqual(sum(item.get("room") == "FITOS Demo" for item in sessions), 12)

        bookings = api("/bookings?size=100", self.token)["content"]
        self.assertGreaterEqual(len(bookings), 8)

        products = api("/retail/products?size=100", self.token)["content"]
        self.assertTrue({"DEMO-ISO-500", "DEMO-PROTEIN-BAR", "DEMO-SHAKER-750"}.issubset(
            {item["sku"] for item in products}))
        retail_orders = api("/retail/orders?size=100", self.token)["content"]
        self.assertGreaterEqual(sum("[FITOS DEMO]" in (item.get("notes") or "")
                                    for item in retail_orders), 8)
        pt_sessions = api(f"/pt-sessions?from={start}&to={start + timedelta(days=14)}", self.token)
        self.assertGreaterEqual(len(pt_sessions), 3)

    def test_demo_accounts_can_log_in(self):
        credentials_file = Path(__file__).resolve().parents[1] / ".dsh" / "demo-accounts.json"
        credentials = json.loads(credentials_file.read_text(encoding="utf-8"))
        for email in credentials["accounts"]:
            with self.subTest(email=email):
                auth = api("/auth/login", payload={
                    "email": email, "password": credentials["password"], "studioId": 1,
                })
                self.assertEqual(auth["email"], email)
                self.assertTrue(auth["accessToken"])

    def test_member_training_progress_and_checkin_data(self):
        credentials_file = Path(__file__).resolve().parents[1] / ".dsh" / "demo-accounts.json"
        credentials = json.loads(credentials_file.read_text(encoding="utf-8"))
        member = api("/auth/login", payload={
            "email": "linh.demo@example.com", "password": credentials["password"], "studioId": 1,
        })
        token = member["accessToken"]
        self.assertGreaterEqual(len(api("/workout-plans", token)), 1)
        sessions = api("/workout-sessions/me", token)
        self.assertGreaterEqual(len(sessions), 1)
        self.assertTrue(any(session.get("logs") for session in sessions))
        self.assertGreaterEqual(len(api("/body-metrics/me", token)), 2)
        self.assertGreaterEqual(len(api("/attendance/me", token)), 1)


if __name__ == "__main__":
    unittest.main()
