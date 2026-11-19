"""Populate the running local FITOS studio with repeatable demo data.

Usage: set FITOS_ADMIN_PASSWORD, then run `python scripts/seed_demo.py`.
The generated account password is kept in the Git-ignored .dsh/demo-accounts.json.
"""

import json
import os
import secrets
from datetime import date, timedelta
from pathlib import Path
from urllib.error import HTTPError
from urllib.parse import urlsplit
from urllib.request import Request, urlopen


ROOT = Path(__file__).resolve().parents[1]
API_BASE = os.getenv("FITOS_API_BASE", "http://localhost:18081/api").rstrip("/")
STUDIO_ID = 1
MARKER = "[FITOS DEMO]"
CREDENTIALS_FILE = ROOT / ".dsh" / "demo-accounts.json"

ACCOUNTS = [
    {"email": "manager.demo@example.com", "fullName": "Demo Quản lý", "phone": "0000000101", "role": "ROLE_MANAGER"},
    {"email": "reception.demo@example.com", "fullName": "Demo Lễ tân", "phone": "0000000102", "role": "ROLE_RECEPTIONIST"},
    {"email": "reception.mai.demo@example.com", "fullName": "Demo Lễ tân Mai", "phone": "0000000103", "role": "ROLE_RECEPTIONIST"},
    {"email": "reception.khoa.demo@example.com", "fullName": "Demo Lễ tân Khoa", "phone": "0000000104", "role": "ROLE_RECEPTIONIST"},
    {"email": "sarah.mitchell@fitos.com", "fullName": "Sarah Mitchell", "phone": "0911223344", "role": "ROLE_TRAINER"},
    {"email": "linh.demo@example.com", "fullName": "Demo Hội viên Linh", "phone": "0000000111", "role": "ROLE_MEMBER"},
    {"email": "minh.demo@example.com", "fullName": "Demo Hội viên Minh", "phone": "0000000112", "role": "ROLE_MEMBER"},
    {"email": "an.demo@example.com", "fullName": "Demo Hội viên An", "phone": "0000000113", "role": "ROLE_MEMBER"},
    {"email": "hoa.demo@example.com", "fullName": "Demo Hội viên Hòa", "phone": "0000000114", "role": "ROLE_MEMBER"},
    {"email": "phuc.demo@example.com", "fullName": "Demo Hội viên Phúc", "phone": "0000000115", "role": "ROLE_MEMBER"},
    {"email": "quynh.demo@example.com", "fullName": "Demo Hội viên Quỳnh", "phone": "0000000116", "role": "ROLE_MEMBER"},
    {"email": "tuan.demo@example.com", "fullName": "Demo Hội viên Tuấn", "phone": "0000000117", "role": "ROLE_MEMBER"},
    {"email": "bao.demo@example.com", "fullName": "Demo Hội viên Bảo", "phone": "0000000118", "role": "ROLE_MEMBER"},
]

LEADS = [
    ("Demo Khách Mai", "0000000201", "lead.mai.demo@example.com", "WEIGHT_LOSS", "WEBSITE", "NEW"),
    ("Demo Khách Huy", "0000000202", "lead.huy.demo@example.com", "MUSCLE_GAIN", "FACEBOOK", "CONTACTED"),
    ("Demo Khách Vy", "0000000203", "lead.vy.demo@example.com", "GENERAL_FITNESS", "INSTAGRAM", "INTERESTED"),
    ("Demo Khách Nam", "0000000204", "lead.nam.demo@example.com", "STRENGTH", "REFERRAL", "TRIAL"),
    ("Demo Khách Hà", "0000000205", "lead.ha.demo@example.com", "MOBILITY", "WALK_IN", "NEGOTIATING"),
    ("Demo Khách Trúc", "0000000206", "lead.truc.demo@example.com", "WEIGHT_LOSS", "FACEBOOK", "NEW"),
    ("Demo Khách Bình", "0000000207", "lead.binh.demo@example.com", "STRENGTH", "WEBSITE", "CONTACTED"),
    ("Demo Khách My", "0000000208", "lead.my.demo@example.com", "GENERAL_FITNESS", "INSTAGRAM", "INTERESTED"),
    ("Demo Khách Duy", "0000000209", "lead.duy.demo@example.com", "MUSCLE_GAIN", "REFERRAL", "TRIAL"),
    ("Demo Khách Lan", "0000000210", "lead.lan.demo@example.com", "MOBILITY", "WALK_IN", "NEGOTIATING"),
    ("Demo Khách Sơn", "0000000211", "lead.son.demo@example.com", "WEIGHT_LOSS", "FACEBOOK", "NEW"),
    ("Demo Khách Nhi", "0000000212", "lead.nhi.demo@example.com", "GENERAL_FITNESS", "WEBSITE", "CONTACTED"),
    ("Demo Khách Kiên", "0000000213", "lead.kien.demo@example.com", "STRENGTH", "INSTAGRAM", "INTERESTED"),
    ("Demo Khách Thảo", "0000000214", "lead.thao.demo@example.com", "MOBILITY", "REFERRAL", "TRIAL"),
    ("Demo Khách Đức", "0000000215", "lead.duc.demo@example.com", "MUSCLE_GAIN", "WALK_IN", "NEGOTIATING"),
    ("Demo Khách Yến", "0000000216", "lead.yen.demo@example.com", "WEIGHT_LOSS", "FACEBOOK", "NEW"),
    ("Demo Khách Long", "0000000217", "lead.long.demo@example.com", "STRENGTH", "WEBSITE", "CONTACTED"),
]

DEMO_PRODUCTS = [
    ("DEMO-ISO-500", "Nước điện giải FITOS Demo 500ml", "CHAI", 10000, 18000, 50, 8),
    ("DEMO-PROTEIN-BAR", "Thanh protein FITOS Demo", "CAI", 18000, 32000, 15, 20),
    ("DEMO-SHAKER-750", "Bình lắc FITOS Demo 750ml", "CAI", 45000, 79000, 20, 5),
]


def request(method, path, token=None, payload=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    data = json.dumps(payload, ensure_ascii=False).encode("utf-8") if payload is not None else None
    req = Request(f"{API_BASE}{path}", data=data, headers=headers, method=method)
    try:
        with urlopen(req, timeout=20) as response:
            body = response.read()
            return json.loads(body) if body else None
    except HTTPError as error:
        raise RuntimeError(f"{method} {path} returned HTTP {error.code}") from error


def get(path, token=None):
    return request("GET", path, token)


def post(path, payload, token=None):
    return request("POST", path, token, payload)


def patch(path, payload, token):
    return request("PATCH", path, token, payload)


def demo_password():
    account_emails = [account["email"] for account in ACCOUNTS]
    if CREDENTIALS_FILE.exists():
        contents = json.loads(CREDENTIALS_FILE.read_text(encoding="utf-8"))
        if contents.get("accounts") != account_emails:
            contents["accounts"] = account_emails
            CREDENTIALS_FILE.write_text(json.dumps(contents, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        return contents["password"]
    CREDENTIALS_FILE.parent.mkdir(exist_ok=True)
    password = "Demo!" + secrets.token_urlsafe(12)
    contents = {"password": password, "accounts": account_emails}
    CREDENTIALS_FILE.write_text(json.dumps(contents, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return password


def main():
    if urlsplit(API_BASE).hostname not in {"localhost", "127.0.0.1"}:
        raise SystemExit("Demo seed chỉ được chạy với API localhost.")
    admin_password = os.getenv("FITOS_ADMIN_PASSWORD")
    if not admin_password:
        raise SystemExit("Đặt FITOS_ADMIN_PASSWORD trước khi chạy demo seed.")
    auth = post("/auth/login", {"email": "admin@fitos.com", "password": admin_password, "studioId": STUDIO_ID})
    token = auth["accessToken"]
    password = demo_password()

    users = {user["email"]: user for user in get("/users", token)}
    for account in ACCOUNTS:
        if account["email"] in users:
            continue
        payload = {key: account[key] for key in ("email", "fullName", "phone")}
        payload["password"] = password
        if account["role"] == "ROLE_MEMBER":
            payload["studioId"] = STUDIO_ID
            post("/auth/register", payload)
        else:
            payload["role"] = account["role"]
            post("/users", payload, token)
        print(f"Created account: {account['email']}")
    users = {user["email"]: user for user in get("/users", token)}

    existing_leads = {lead["email"]: lead for lead in get("/leads?size=100", token)["content"]}
    reception_emails = ["reception.demo@example.com", "reception.mai.demo@example.com", "reception.khoa.demo@example.com"]
    for index, (name, phone, email, goal, source, status) in enumerate(LEADS):
        lead = existing_leads.get(email)
        if lead is None:
            lead = post("/leads", {
                "fullName": name, "phone": phone, "email": email, "goal": goal,
                "source": source, "notes": f"{MARKER} Khách tiềm năng để trải nghiệm CRM",
                "assignedToUserId": users[reception_emails[index % len(reception_emails)]]["id"],
            }, token)
            print(f"Created lead: {email}")
        if status != "NEW" and lead["status"] == "NEW":
            patch(f"/leads/{lead['id']}/status", {"status": status, "notes": "Dữ liệu mẫu"}, token)
            print(f"Set lead status: {email} -> {status}")
        if index >= 5:
            activities = get(f"/leads/{lead['id']}/activities", token)
            activity_note = f"{MARKER} Cuộc gọi tư vấn cho {email}"
            if not any(item.get("notes") == activity_note for item in activities):
                post(f"/leads/{lead['id']}/activities", {"activityType": "CALL", "notes": activity_note}, token)
                print(f"Created lead activity: {email}")

    customers = {customer["email"]: customer for customer in get("/customers?size=100", token)["content"]}
    plans = get("/plans", token)
    membership_plans = [plan for plan in plans if plan["planType"] == "MEMBERSHIP" and plan["isActive"]]
    pt_plans = [plan for plan in plans if plan["planType"] == "PT_PACKAGE" and plan["isActive"]]
    if not membership_plans:
        raise RuntimeError("Không tìm thấy gói hội viên đang bán")

    orders = {order.get("notes"): order for order in get("/orders?size=100", token)["content"]}
    sale_specs = [
        ("linh.demo@example.com", membership_plans[0]["id"], "membership-linh"),
        ("minh.demo@example.com", membership_plans[min(1, len(membership_plans) - 1)]["id"], "membership-minh"),
        ("an.demo@example.com", membership_plans[0]["id"], "membership-an"),
        ("hoa.demo@example.com", membership_plans[min(1, len(membership_plans) - 1)]["id"], "membership-hoa"),
        ("phuc.demo@example.com", membership_plans[0]["id"], "membership-phuc"),
        ("quynh.demo@example.com", membership_plans[min(1, len(membership_plans) - 1)]["id"], "membership-quynh"),
        ("tuan.demo@example.com", membership_plans[0]["id"], "membership-tuan"),
        ("bao.demo@example.com", membership_plans[min(1, len(membership_plans) - 1)]["id"], "membership-bao"),
    ]
    if pt_plans:
        sale_specs.append(("linh.demo@example.com", pt_plans[0]["id"], "pt-linh"))
        sale_specs.append(("hoa.demo@example.com", pt_plans[min(1, len(pt_plans) - 1)]["id"], "pt-hoa"))
        sale_specs.append(("tuan.demo@example.com", pt_plans[0]["id"], "pt-tuan"))
    for email, plan_id, key in sale_specs:
        note = f"{MARKER} {key}"
        order = orders.get(note)
        if order is None:
            order = post("/orders", {
                "customerId": customers[email]["id"], "items": [{"planId": plan_id, "quantity": 1}],
                "notes": note,
            }, token)
            print(f"Created order: {key}")
        if order["status"] == "PENDING":
            post("/payments", {"orderId": order["id"], "paymentMethod": "CASH"}, token)
            print(f"Paid sample order: {key}")

    pending_note = f"{MARKER} pending-membership-bao"
    if pending_note not in orders:
        post("/orders", {
            "customerId": customers["bao.demo@example.com"]["id"],
            "items": [{"planId": membership_plans[0]["id"], "quantity": 1}],
            "notes": pending_note,
        }, token)
        print("Created pending sample order: pending-membership-bao")

    categories = get("/retail/categories?includeInactive=true", token)
    demo_category = next((category for category in categories if category["name"] == "FITOS Demo"), None)
    if demo_category is None:
        demo_category = post("/retail/categories", {
            "name": "FITOS Demo", "description": "Sản phẩm mẫu cho kiểm thử POS", "sortOrder": 90,
        }, token)
        print("Created retail category: FITOS Demo")
    products = {product["sku"]: product for product in get("/retail/products?size=100&activeOnly=false", token)["content"]}
    stock_movements = get("/retail/inventory/movements?size=100", token)["content"]
    for sku, name, unit, cost, price, quantity, threshold in DEMO_PRODUCTS:
        product = products.get(sku)
        if product is None:
            product = post("/retail/products", {
                "sku": sku, "name": name, "categoryId": demo_category["id"],
                "unit": unit, "costPrice": cost, "sellPrice": price,
                "lowStockThreshold": threshold, "trackInventory": True,
            }, token)
            products[sku] = product
            print(f"Created retail product: {sku}")
        stock_note = f"{MARKER} stock-{sku}"
        if not any(item.get("productId") == product["id"] and item.get("note") == stock_note
                   for item in stock_movements):
            movement = post("/retail/inventory/stock-in", {
                "productId": product["id"], "quantity": quantity, "unitCost": cost,
                "note": stock_note,
            }, token)
            stock_movements.append(movement)
            print(f"Stocked retail product: {sku}")

    retail_orders = {order.get("notes"): order for order in get("/retail/orders?size=100", token)["content"]}
    retail_specs = [
        ("linh.demo@example.com", "DEMO-ISO-500", 2, "CASH", "iso-linh"),
        ("minh.demo@example.com", "DEMO-PROTEIN-BAR", 1, "CARD", "bar-minh"),
        ("an.demo@example.com", "DEMO-SHAKER-750", 1, "CASH", "shaker-an"),
        ("hoa.demo@example.com", "DEMO-ISO-500", 1, "CASH", "iso-hoa"),
        ("phuc.demo@example.com", "DEMO-PROTEIN-BAR", 2, "CARD", "bar-phuc"),
        ("quynh.demo@example.com", "DEMO-SHAKER-750", 1, "CASH", "shaker-quynh"),
        ("tuan.demo@example.com", "DEMO-ISO-500", 3, "CASH", "iso-tuan"),
        ("bao.demo@example.com", "DEMO-PROTEIN-BAR", 1, "CASH", "bar-bao"),
    ]
    for email, sku, quantity, method, key in retail_specs:
        note = f"{MARKER} retail-{key}"
        if note in retail_orders:
            continue
        post("/retail/orders", {
            "customerId": customers[email]["id"],
            "items": [{"productId": products[sku]["id"], "quantity": quantity}],
            "paymentMethod": method,
            "notes": note,
            "clientRequestId": f"fitos-demo-retail-{key}",
        }, token)
        print(f"Created paid POS order: {key}")

    classes = get("/classes", token)
    trainers = get("/trainers", token)
    if not classes or not trainers:
        raise RuntimeError("Thiếu lớp hoặc huấn luyện viên mẫu")
    today = date.today()
    sessions = get(f"/sessions?from={today}&to={today + timedelta(days=14)}", token)
    for offset in range(1, 15):
        session_date = str(today + timedelta(days=offset))
        class_id = classes[(offset - 1) % len(classes)]["id"]
        start_time = "09:00:00" if offset % 2 else "17:00:00"
        match = next((item for item in sessions if item["room"] == "FITOS Demo"
                      and item["classId"] == class_id and item["sessionDate"] == session_date
                      and item["startTime"].startswith(start_time[:5])), None)
        if match:
            continue
        session = post("/sessions", {
            "classId": class_id, "trainerId": trainers[(offset - 1) % len(trainers)]["id"],
            "sessionDate": session_date, "startTime": start_time,
            "endTime": "10:00:00" if offset % 2 else "18:00:00",
            "room": "FITOS Demo", "capacity": 12,
        }, token)
        sessions.append(session)
        print(f"Created session: {session_date} {start_time}")

    demo_sessions = sorted((item for item in sessions if item["room"] == "FITOS Demo"
                            and item["status"] == "SCHEDULED"), key=lambda item: (item["sessionDate"], item["startTime"]))
    bookings = get("/bookings?size=100", token)["content"]
    booking_emails = (
        "linh.demo@example.com", "minh.demo@example.com", "an.demo@example.com",
        "hoa.demo@example.com", "phuc.demo@example.com", "quynh.demo@example.com",
        "tuan.demo@example.com", "bao.demo@example.com",
    )
    for email, session in zip(booking_emails, demo_sessions):
        customer_id = customers[email]["id"]
        if any(item["customerId"] == customer_id and item["sessionId"] == session["id"]
               and item["status"] != "CANCELLED" for item in bookings):
            continue
        post("/bookings", {"customerId": customer_id, "sessionId": session["id"]}, token)
        print(f"Booked demo class: {email}")

    pt_sessions = get(f"/pt-sessions?from={today}&to={today + timedelta(days=14)}", token)
    pt_specs = [
        ("linh.demo@example.com", 2, 0),
        ("hoa.demo@example.com", 3, 1),
        ("tuan.demo@example.com", 4, 2),
        ("linh.demo@example.com", 5, 3),
    ]
    for email, offset, trainer_index in pt_specs:
        customer_id = customers[email]["id"]
        session_date = str(today + timedelta(days=offset))
        if any(item["customerId"] == customer_id and item["sessionDate"] == session_date
               and item["startTime"].startswith("13:00") for item in pt_sessions):
            continue
        packages = get(f"/pt-packages/me?customerId={customer_id}", token)
        package = next((item for item in packages if item["status"] == "ACTIVE"), None)
        if package is None:
            raise RuntimeError(f"Thiếu gói PT đã thanh toán cho {email}")
        pt_session = post("/pt-sessions", {
            "trainerId": trainers[trainer_index % len(trainers)]["id"],
            "customerId": customer_id, "ptPackageId": package["id"],
            "sessionDate": session_date, "startTime": "13:00:00", "endTime": "14:00:00",
            "trainingNotes": f"{MARKER} Buổi PT mẫu cho {email}",
        }, token)
        pt_sessions.append(pt_session)
        print(f"Created PT session: {email} {session_date}")

    sarah = next((trainer for trainer in trainers if trainer["email"] == "sarah.mitchell@fitos.com"), None)
    if sarah and customers["linh.demo@example.com"]["id"] not in sarah["customerIds"]:
        post(f"/trainers/{sarah['id']}/assign?customerId={customers['linh.demo@example.com']['id']}", {}, token)
        print("Assigned member to Sarah Mitchell")

    # Member-facing training records are created through the same APIs as the UI.
    member_token = post("/auth/login", {
        "email": "linh.demo@example.com", "password": password, "studioId": STUDIO_ID,
    })["accessToken"]
    trainer_token = post("/auth/login", {
        "email": "sarah.mitchell@fitos.com", "password": password, "studioId": STUDIO_ID,
    })["accessToken"]
    exercise_catalog = get("/exercises", trainer_token)
    if len(exercise_catalog) < 3:
        raise RuntimeError("Thiếu bài tập mẫu cho giáo án")
    member_id = customers["linh.demo@example.com"]["id"]
    sample_plan_name = f"{MARKER} Giáo án toàn thân căn bản"
    member_plans = get("/workout-plans", member_token)
    plan = next((item for item in member_plans if item["name"] == sample_plan_name), None)
    if plan is None:
        plan = post("/workout-plans", {
            "customerId": member_id,
            "trainerId": sarah["id"] if sarah else None,
            "name": sample_plan_name,
            "description": "Ba buổi mỗi tuần để tập kỹ thuật và tăng sức bền.",
            "goal": "GENERAL_FITNESS",
            "startDate": str(today),
            "endDate": str(today + timedelta(days=56)),
            "exercises": [
                {"exerciseId": exercise["id"], "dayOfWeek": day, "sets": 3,
                 "reps": 10, "restSecs": 60, "sortOrder": index}
                for index, (exercise, day) in enumerate(zip(exercise_catalog[:3], (1, 3, 5)))
            ],
        }, trainer_token)
        print("Created sample workout plan")
    if plan["status"] == "DRAFT":
        plan = patch(f"/workout-plans/{plan['id']}/activate", {}, trainer_token)
        print("Activated sample workout plan")

    sample_session_note = f"{MARKER} Buổi tập thử giáo án"
    workout_sessions = get("/workout-sessions/me", member_token)
    workout_session = next((item for item in workout_sessions
                            if item.get("notes") == sample_session_note), None)
    if workout_session is None:
        workout_session = post("/workout-sessions", {
            "customerId": member_id, "workoutPlanId": plan["id"], "notes": sample_session_note,
        }, member_token)
        print("Started sample workout session")
    existing_sets = {(item["exerciseId"], item["setNumber"])
                     for item in workout_session.get("logs", [])}
    for set_number in (1, 2):
        exercise_id = exercise_catalog[0]["id"]
        if (exercise_id, set_number) not in existing_sets:
            post("/workout-logs", {
                "workoutSessionId": workout_session["id"], "exerciseId": exercise_id,
                "setNumber": set_number, "repsActual": 10, "weightKg": 20,
                "rpe": 6 + set_number, "notes": MARKER,
            }, member_token)
            print(f"Logged sample workout set {set_number}")
    if workout_session["status"] != "COMPLETED":
        patch(f"/workout-sessions/{workout_session['id']}/complete", {
            "caloriesBurned": 220, "notes": sample_session_note,
        }, member_token)
        print("Completed sample workout session")

    metric_dates = {item["recordedDate"] for item in get("/body-metrics/me", member_token)}
    for offset, weight, body_fat, muscle in ((14, 68.5, 22.0, 27.5), (0, 67.8, 21.2, 28.0)):
        recorded = str(today - timedelta(days=offset))
        if recorded not in metric_dates:
            post("/body-metrics/me", {
                "recordedDate": recorded, "weightKg": weight, "heightCm": 168,
                "bodyFatPercent": body_fat, "muscleMassKg": muscle,
                "notes": f"{MARKER} Chỉ số cơ thể",
            }, member_token)
            print(f"Recorded sample body metric: {recorded}")

    reception_token = post("/auth/login", {
        "email": "reception.demo@example.com", "password": password, "studioId": STUDIO_ID,
    })["accessToken"]
    sample_checkin_note = f"{MARKER} Lượt điểm danh tại quầy"
    existing_attendance = get("/attendance?size=100", reception_token)["content"]
    if not any(item.get("customerId") == member_id and item.get("notes") == sample_checkin_note
               for item in existing_attendance):
        post("/checkin", {
            "customerId": member_id, "method": "DESK", "notes": sample_checkin_note,
        }, reception_token)
        print("Created sample member check-in")

    print("Done. Credentials: .dsh/demo-accounts.json")


if __name__ == "__main__":
    main()
