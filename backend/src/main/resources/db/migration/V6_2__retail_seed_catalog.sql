-- ============================================================================
-- V6.2: Danh mục bán lẻ mẫu cho studio lâu nhất (giá VND, thị trường Việt Nam).
-- Idempotent: chạy lại không nhân đôi dữ liệu.
-- ============================================================================

-- 1) Danh mục ---------------------------------------------------------------
INSERT INTO product_categories (studio_id, name, description, sort_order)
SELECT s.id, v.name, v.description, v.sort_order
FROM (SELECT id FROM studios ORDER BY id LIMIT 1) s
CROSS JOIN (VALUES
    ('Thực phẩm bổ sung', 'Whey, creatine, BCAA, pre-workout', 1),
    ('Nước uống',         'Nước khoáng, isotonic, tăng lực, trà', 2),
    ('Dụng cụ tập luyện', 'Găng tay, đai lưng, dây kháng lực, bình lắc', 3),
    ('Phụ kiện',          'Khăn tập, dây đeo thẻ hội viên', 4)
) AS v(name, description, sort_order)
ON CONFLICT (studio_id, name) DO NOTHING;

-- 2) Sản phẩm ---------------------------------------------------------------
INSERT INTO products (studio_id, category_id, sku, barcode, name, unit,
                      cost_price, sell_price, vat_rate, stock_quantity, low_stock_threshold,
                      description)
SELECT s.id, c.id, v.sku, v.barcode, v.name, v.unit,
       v.cost_price, v.sell_price, v.vat_rate, v.stock_quantity, v.low_stock,
       v.descr
FROM (SELECT id FROM studios ORDER BY id LIMIT 1) s
CROSS JOIN (VALUES
    ('WHEY-CONC-2K',   'Thực phẩm bổ sung', '8935001100011', 'Whey Protein Concentrate 2kg',  'HOP',  1150000, 1450000, 5,  12, 3, 'Whey concentrate 24g protein/khẩu phần, vị socola, hộp 2kg (khoảng 60 khẩu phần).'),
    ('WHEY-ISO-18',    'Thực phẩm bổ sung', '8935001100028', 'Whey Isolate 1.8kg',            'HOP',  1520000, 1890000, 5,   8, 2, 'Whey isolate hấp thu nhanh, gần như không lactose, hộp 1.8kg.'),
    ('CREA-300',       'Thực phẩm bổ sung', '8935001100035', 'Creatine Monohydrate 300g',     'HOP',   390000,  550000, 5,  20, 5, 'Creatine monohydrate dạng bột mịn, 300g, tăng sức mạnh hiếu khí.'),
    ('BCAA-300',       'Thực phẩm bổ sung', '8935001100042', 'BCAA 2:1:1 300g',               'HOP',   520000,  690000, 5,  10, 3, 'BCAA tỉ lệ 2:1:1, hỗ trợ phục hồi cơ, vị chanh, 300g.'),
    ('PREWO-300',      'Thực phẩm bổ sung', '8935001100059', 'Pre-Workout 300g',              'HOP',   560000,  750000, 5,   6, 2, 'Pre-workout có caffeine và beta-alanine, dùng 30 phút trước tập.'),
    ('WAT-AQUA-500',   'Nước uống',         '8935001100066', 'Nước khoáng Aquafina 500ml',    'CHAI',    5000,   10000, 5, 120,24, 'Nước khoáng đóng chai 500ml, bán lẻ tại quầy.'),
    ('DRK-STING-330',  'Nước uống',         '8935001100073', 'Nước tăng lực Sting 330ml',     'CHAI',    7000,   12000,10,  96,24, 'Nước tăng lực vị dâu, chai 330ml.'),
    ('DRK-REVIVE-500', 'Nước uống',         '8935001100080', 'Nước isotonic Revive 500ml',    'CHAI',    9000,   15000,10,  80,20, 'Nước bù điện giải sau tập, chai 500ml.'),
    ('DRK-C2-500',     'Nước uống',         '8935001100097', 'Trà xanh C2 500ml',             'CHAI',    7500,   12000,10,  60,18, 'Trà xanh đóng chai 500ml, ít đường.'),
    ('ACC-GLOVE-01',   'Dụng cụ tập luyện', '8935001100103', 'Găng tay tập gym có đệm',       'DOI',   150000,  250000,10,  15, 3, 'Găng tay hở ngón, đệm lòng bàn tay, chống trượt khi tạ nặng.'),
    ('ACC-BELT-4I',    'Dụng cụ tập luyện', '8935001100110', 'Đai lưng tập tạ 4 inch',        'CAI',   260000,  420000,10,  10, 2, 'Đai lưng da tổng hợp 4 inch, hỗ trợ squat và deadlift nặng.'),
    ('ACC-BAND-S5',    'Dụng cụ tập luyện', '8935001100127', 'Bộ dây kháng lực 5 mức',        'BO',    190000,  350000,10,  12, 3, 'Bộ 5 dây kháng lực từ nhẹ đến nặng, kèm túi đựng.'),
    ('ACC-SHAKER-600', 'Dụng cụ tập luyện', '8935001100134', 'Bình lắc Shaker 600ml',         'CAI',    65000,  120000,10,  30, 5, 'Bình lắc 3 ngăn, nắp chống rò, dung tích 600ml.'),
    ('ACC-TOWEL-MF',   'Phụ kiện',          '8935001100141', 'Khăn tập microfiber',           'CAI',    45000,   90000,10,  40, 8, 'Khăn microfiber thấm hút nhanh, kích thước 40x80cm.'),
    ('ACC-STRAP-ID',   'Phụ kiện',          '8935001100158', 'Dây đeo thẻ hội viên',          'CAI',    20000,   50000,10,  50,10, 'Dây đeo thẻ có logo FITOS, dùng cho thẻ hội viên.')
) AS v(sku, category_name, barcode, name, unit, cost_price, sell_price, vat_rate,
       stock_quantity, low_stock, descr)
JOIN product_categories c ON c.studio_id = s.id AND c.name = v.category_name
ON CONFLICT (studio_id, sku) DO NOTHING;

-- 3) Ghi nhận tồn đầu kỳ để lịch sử kho khớp với stock_quantity ------------
INSERT INTO inventory_transactions (studio_id, product_id, transaction_type,
                                    quantity_delta, stock_before, stock_after,
                                    unit_cost, note)
SELECT p.studio_id, p.id, 'STOCK_IN', p.stock_quantity, 0, p.stock_quantity,
       p.cost_price, 'Tồn đầu kỳ (seed V6.2)'
FROM products p
WHERE p.studio_id = (SELECT id FROM studios ORDER BY id LIMIT 1)
  AND p.sku IN ('WHEY-CONC-2K','WHEY-ISO-18','CREA-300','BCAA-300','PREWO-300',
                'WAT-AQUA-500','DRK-STING-330','DRK-REVIVE-500','DRK-C2-500',
                'ACC-GLOVE-01','ACC-BELT-4I','ACC-BAND-S5','ACC-SHAKER-600',
                'ACC-TOWEL-MF','ACC-STRAP-ID')
  AND p.stock_quantity > 0
  AND NOT EXISTS (
      SELECT 1 FROM inventory_transactions t
      WHERE t.product_id = p.id
        AND t.transaction_type = 'STOCK_IN'
        AND t.stock_before = 0
  );
