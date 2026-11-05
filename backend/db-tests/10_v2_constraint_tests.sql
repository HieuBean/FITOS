-- Kiểm tra hành vi các ràng buộc. Mỗi case "PHẢI LỖI" in PASS nếu bị DB chặn.
\set ON_ERROR_STOP off
\pset pager off

-- Dữ liệu hợp lệ
INSERT INTO service_plans (name, plan_type, price, duration_days) VALUES ('Gold 30 ngay', 'MEMBERSHIP', 500000, 30);
INSERT INTO service_plans (name, plan_type, price, duration_days, total_sessions) VALUES ('PT 10 buoi', 'PT_PACKAGE', 3000000, 60, 10);
INSERT INTO orders (order_reference, customer_id, subtotal_amount, discount_amount, total_amount) VALUES ('ORD-1', 1, 500000, 50000, 450000);
INSERT INTO order_items (order_id, plan_id, plan_name, unit_price, quantity, line_total) VALUES (1, 1, 'Gold 30 ngay', 500000, 1, 500000);
INSERT INTO payments (order_id, transaction_ref, amount, payment_method, status, paid_at) VALUES (1, 'TXN-1', 450000, 'VNPAY', 'SUCCESS', NOW());
INSERT INTO memberships (customer_id, plan_id, order_id, start_date, end_date) VALUES (1, 1, 1, CURRENT_DATE, CURRENT_DATE + 30);
INSERT INTO pt_packages (customer_id, plan_id, total_sessions, remaining_sessions, start_date, expiry_date) VALUES (1, 2, 10, 10, CURRENT_DATE, CURRENT_DATE + 60);

DO $$
DECLARE
  cases TEXT[][] := ARRAY[
    ['PT_PACKAGE thiếu total_sessions',        $q$INSERT INTO service_plans (name, plan_type, price, duration_days) VALUES ('x1','PT_PACKAGE',1,1)$q$],
    ['MEMBERSHIP có total_sessions',           $q$INSERT INTO service_plans (name, plan_type, price, duration_days, total_sessions) VALUES ('x2','MEMBERSHIP',1,1,5)$q$],
    ['Giá âm',                                 $q$INSERT INTO service_plans (name, plan_type, price, duration_days) VALUES ('x3','MEMBERSHIP',-1,1)$q$],
    ['Trùng order_reference',                  $q$INSERT INTO orders (order_reference, customer_id, subtotal_amount, total_amount) VALUES ('ORD-1',1,0,0)$q$],
    ['Tổng tiền âm (discount > subtotal)',     $q$INSERT INTO orders (order_reference, customer_id, subtotal_amount, discount_amount, total_amount) VALUES ('ORD-X',1,100,200,-100)$q$],
    ['Total lệch subtotal - discount',         $q$INSERT INTO orders (order_reference, customer_id, subtotal_amount, discount_amount, total_amount) VALUES ('ORD-Y',1,100,0,1)$q$],
    ['Customer không tồn tại',                 $q$INSERT INTO orders (order_reference, customer_id, subtotal_amount, total_amount) VALUES ('ORD-Z',999,0,0)$q$],
    ['line_total sai',                         $q$INSERT INTO order_items (order_id, plan_id, plan_name, unit_price, quantity, line_total) VALUES (1,1,'a',100,2,100)$q$],
    ['Webhook trùng transaction_ref',          $q$INSERT INTO payments (order_id, transaction_ref, amount, payment_method) VALUES (1,'TXN-1',450000,'VNPAY')$q$],
    ['2 payment SUCCESS cho 1 đơn',            $q$INSERT INTO payments (order_id, transaction_ref, amount, payment_method, status, paid_at) VALUES (1,'TXN-2',450000,'MOMO','SUCCESS',NOW())$q$],
    ['SUCCESS mà không có paid_at',            $q$INSERT INTO payments (order_id, transaction_ref, amount, payment_method, status) VALUES (1,'TXN-3',1,'CASH','SUCCESS')$q$],
    ['Kích hoạt membership 2 lần cùng đơn',    $q$INSERT INTO memberships (customer_id, plan_id, order_id, start_date, end_date) VALUES (1,1,1,CURRENT_DATE,CURRENT_DATE+30)$q$],
    ['end_date < start_date',                  $q$INSERT INTO memberships (customer_id, plan_id, start_date, end_date) VALUES (1,1,CURRENT_DATE,CURRENT_DATE-1)$q$],
    ['PAUSED nhưng không có paused_at',        $q$UPDATE memberships SET status='PAUSED' WHERE id=1$q$],
    ['remaining_sessions âm',                  $q$UPDATE pt_packages SET remaining_sessions=-1 WHERE id=1$q$],
    ['remaining_sessions > total',             $q$UPDATE pt_packages SET remaining_sessions=11 WHERE id=1$q$],
    ['Xoá plan đang được dùng',                $q$DELETE FROM service_plans WHERE id=1$q$]
  ];
  i INT; passed INT := 0;
BEGIN
  FOR i IN 1 .. array_length(cases, 1) LOOP
    BEGIN
      EXECUTE cases[i][2];
      RAISE NOTICE 'FAIL  % (không bị chặn!)', cases[i][1];
      RAISE EXCEPTION 'rollback';
    EXCEPTION WHEN OTHERS THEN
      IF SQLERRM = 'rollback' THEN CONTINUE; END IF;
      passed := passed + 1;
      RAISE NOTICE 'PASS  % -> %', cases[i][1], SQLERRM;
    END;
  END LOOP;
  RAISE NOTICE '==== % / % case bị chặn đúng ====', passed, array_length(cases, 1);
END $$;

-- Case hợp lệ: pause → resume
UPDATE memberships SET status='PAUSED', paused_at=CURRENT_DATE WHERE id=1;
UPDATE memberships SET status='ACTIVE', paused_at=NULL, total_paused_days=total_paused_days+5, end_date=end_date+5 WHERE id=1;
SELECT id, status, end_date - start_date AS days, total_paused_days FROM memberships;
\dt
