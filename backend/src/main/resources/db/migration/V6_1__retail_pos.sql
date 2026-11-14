-- ============================================================================
-- V6.1: Module bán lẻ / POS — danh mục sản phẩm, tồn kho, bán hàng tại quầy.
--
-- Chiến lược: TÁI SỬ DỤNG bảng `orders` hiện có (thay vì tạo retail_orders) để
-- giữ nguyên toàn bộ hạ tầng thanh toán, đối soát và báo cáo doanh thu. Thêm
-- `order_type` để phân biệt bán gói / bán lẻ / trả hàng, và mở rộng `order_items`
-- để chứa dòng sản phẩm hoặc dòng đặt lớp (booking) bên cạnh dòng gói dịch vụ.
--
-- Quy ước đa studio (theo V4_6): studio_id NOT NULL + FK + uq_(id,studio_id)
-- + index (studio_id, ...) + FK composite để chống lẫn dữ liệu giữa studio.
-- Các bảng MỚI không đặt DEFAULT cho studio_id (tránh ghi nhầm studio cũ).
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1) DANH MỤC SẢN PHẨM
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS product_categories (
    id          BIGSERIAL PRIMARY KEY,
    studio_id   BIGINT       NOT NULL REFERENCES studios(id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    sort_order  INT          NOT NULL DEFAULT 0,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_product_categories_name CHECK (BTRIM(name) <> ''),
    CONSTRAINT uq_product_categories_studio_name UNIQUE (studio_id, name),
    CONSTRAINT uq_product_categories_id_studio   UNIQUE (id, studio_id)
);

CREATE INDEX IF NOT EXISTS idx_product_categories_studio
    ON product_categories (studio_id, is_active, sort_order);

-- ---------------------------------------------------------------------------
-- 2) SẢN PHẨM
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS products (
    id                  BIGSERIAL PRIMARY KEY,
    studio_id           BIGINT        NOT NULL REFERENCES studios(id) ON DELETE CASCADE,
    category_id         BIGINT,
    sku                 VARCHAR(50)   NOT NULL,
    barcode             VARCHAR(50),
    name                VARCHAR(150)  NOT NULL,
    description         TEXT,
    -- Đơn vị bán: VIEN(viên) CHAI(chai) HOP(hộp) CAI(cái) DOI(đôi) BO(bộ) GOI(gói) KG(kg)
    unit                VARCHAR(10)   NOT NULL DEFAULT 'CAI'
                          CHECK (unit IN ('VIEN','CHAI','HOP','CAI','DOI','BO','GOI','KG')),
    cost_price          NUMERIC(12,2) NOT NULL DEFAULT 0.00 CHECK (cost_price >= 0),
    -- Giá niêm yết ĐÃ BAO GỒM VAT (chuẩn bán lẻ Việt Nam)
    sell_price          NUMERIC(12,2) NOT NULL CHECK (sell_price >= 0),
    vat_rate            NUMERIC(5,2)  NOT NULL DEFAULT 0.00
                          CHECK (vat_rate >= 0 AND vat_rate <= 100),
    stock_quantity      INT           NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    low_stock_threshold INT           NOT NULL DEFAULT 5 CHECK (low_stock_threshold >= 0),
    -- FALSE = mặt hàng không quản lý tồn (phí dịch vụ, công lắp đặt…)
    track_inventory     BOOLEAN       NOT NULL DEFAULT TRUE,
    image_url           VARCHAR(500),
    is_active           BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_products_name    CHECK (BTRIM(name) <> ''),
    CONSTRAINT chk_products_sku     CHECK (BTRIM(sku) <> ''),
    CONSTRAINT chk_products_barcode CHECK (barcode IS NULL OR BTRIM(barcode) <> ''),
    CONSTRAINT uq_products_studio_sku UNIQUE (studio_id, sku),
    CONSTRAINT uq_products_id_studio  UNIQUE (id, studio_id)
);

-- FK danh mục: cùng studio (composite) + cho phép danh mục bị xoá thì sản phẩm
-- vẫn còn (chỉ mất liên kết).
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_products_category') THEN
        ALTER TABLE products
            ADD CONSTRAINT fk_products_category
            FOREIGN KEY (category_id, studio_id)
            REFERENCES product_categories (id, studio_id)
            ON DELETE SET NULL (category_id);
    END IF;
END $$;

-- Mã vạch duy nhất trong từng studio, cho phép nhiều NULL
CREATE UNIQUE INDEX IF NOT EXISTS uq_products_studio_barcode
    ON products (studio_id, barcode) WHERE barcode IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_products_studio_active
    ON products (studio_id, is_active);
CREATE INDEX IF NOT EXISTS idx_products_studio_category
    ON products (studio_id, category_id);
CREATE INDEX IF NOT EXISTS idx_products_studio_stock
    ON products (studio_id, stock_quantity);

-- ---------------------------------------------------------------------------
-- 3) GIAO DỊCH KHO
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS inventory_transactions (
    id               BIGSERIAL PRIMARY KEY,
    studio_id        BIGINT        NOT NULL REFERENCES studios(id) ON DELETE CASCADE,
    product_id       BIGINT        NOT NULL,
    transaction_type VARCHAR(20)   NOT NULL
                       CHECK (transaction_type IN
                         ('STOCK_IN','SALE_OUT','RETURN_IN','ADJUSTMENT','CANCEL_RETURN')),
    -- Có dấu: + nhập, - xuất
    quantity_delta   INT           NOT NULL CHECK (quantity_delta <> 0),
    stock_before     INT           NOT NULL CHECK (stock_before >= 0),
    stock_after      INT           NOT NULL CHECK (stock_after >= 0),
    unit_cost        NUMERIC(12,2) CHECK (unit_cost IS NULL OR unit_cost >= 0),
    order_id         BIGINT,
    note             VARCHAR(255),
    created_by       BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_inventory_stock_math CHECK (stock_after = stock_before + quantity_delta),
    CONSTRAINT uq_inventory_transactions_id_studio UNIQUE (id, studio_id)
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_inventory_product') THEN
        ALTER TABLE inventory_transactions
            ADD CONSTRAINT fk_inventory_product
            FOREIGN KEY (product_id, studio_id)
            REFERENCES products (id, studio_id) ON DELETE RESTRICT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_inventory_order') THEN
        ALTER TABLE inventory_transactions
            ADD CONSTRAINT fk_inventory_order
            FOREIGN KEY (order_id, studio_id)
            REFERENCES orders (id, studio_id) ON DELETE SET NULL (order_id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_inventory_studio_product
    ON inventory_transactions (studio_id, product_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_inventory_studio_type
    ON inventory_transactions (studio_id, transaction_type, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_inventory_order
    ON inventory_transactions (order_id) WHERE order_id IS NOT NULL;

-- Chống trừ kho 2 lần cho cùng một đơn (idempotency ở tầng DB)
CREATE UNIQUE INDEX IF NOT EXISTS uq_inventory_sale_once
    ON inventory_transactions (studio_id, order_id, product_id)
    WHERE transaction_type = 'SALE_OUT' AND order_id IS NOT NULL;

-- ---------------------------------------------------------------------------
-- 4) MỞ RỘNG orders
-- ---------------------------------------------------------------------------
ALTER TABLE orders ADD COLUMN IF NOT EXISTS order_type VARCHAR(20) NOT NULL DEFAULT 'PLAN_SALE';
ALTER TABLE orders DROP CONSTRAINT IF EXISTS chk_orders_order_type;
ALTER TABLE orders ADD CONSTRAINT chk_orders_order_type
    CHECK (order_type IN ('PLAN_SALE','RETAIL','RETAIL_RETURN'));

-- Khoá idempotency cho POS (retry do mất mạng hoặc bấm 2 lần)
ALTER TABLE orders ADD COLUMN IF NOT EXISTS client_request_id VARCHAR(64);
CREATE UNIQUE INDEX IF NOT EXISTS uq_orders_studio_client_request
    ON orders (studio_id, client_request_id) WHERE client_request_id IS NOT NULL;

-- Liên kết đơn trả hàng về đơn gốc
ALTER TABLE orders ADD COLUMN IF NOT EXISTS parent_order_id BIGINT;
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_orders_parent') THEN
        ALTER TABLE orders
            ADD CONSTRAINT fk_orders_parent
            FOREIGN KEY (parent_order_id, studio_id)
            REFERENCES orders (id, studio_id) ON DELETE SET NULL (parent_order_id);
    END IF;
END $$;
CREATE INDEX IF NOT EXISTS idx_orders_parent ON orders (parent_order_id)
    WHERE parent_order_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_orders_studio_type_created
    ON orders (studio_id, order_type, created_at DESC);

-- ---------------------------------------------------------------------------
-- 5) MỞ RỘNG order_items
-- ---------------------------------------------------------------------------
-- BLOCKER: plan_id đang NOT NULL nên không thể thêm dòng sản phẩm bán lẻ.
ALTER TABLE order_items ALTER COLUMN plan_id DROP NOT NULL;

ALTER TABLE order_items ADD COLUMN IF NOT EXISTS item_type VARCHAR(20) NOT NULL DEFAULT 'PLAN';
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS product_id BIGINT;
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS session_id BIGINT;
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS booking_id BIGINT;
-- Ảnh chụp tên hàng tại thời điểm bán (plan_name/line_total là cột legacy, không dùng)
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS item_name VARCHAR(150);
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS unit_cost NUMERIC(12,2) NOT NULL DEFAULT 0.00
    CHECK (unit_cost >= 0);
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS vat_rate NUMERIC(5,2) NOT NULL DEFAULT 0.00
    CHECK (vat_rate >= 0 AND vat_rate <= 100);
ALTER TABLE order_items ADD COLUMN IF NOT EXISTS vat_amount NUMERIC(12,2) NOT NULL DEFAULT 0.00
    CHECK (vat_amount >= 0);

ALTER TABLE order_items DROP CONSTRAINT IF EXISTS chk_order_items_item_type;
ALTER TABLE order_items ADD CONSTRAINT chk_order_items_item_type
    CHECK (item_type IN ('PLAN','PRODUCT','BOOKING'));

-- Mỗi dòng chỉ có đúng một "đích", và đích phải khớp với item_type
ALTER TABLE order_items DROP CONSTRAINT IF EXISTS chk_order_items_target;
ALTER TABLE order_items ADD CONSTRAINT chk_order_items_target CHECK (
       (item_type = 'PLAN'    AND plan_id    IS NOT NULL AND product_id IS NULL AND session_id IS NULL)
    OR (item_type = 'PRODUCT' AND product_id IS NOT NULL AND plan_id    IS NULL AND session_id IS NULL)
    OR (item_type = 'BOOKING' AND session_id IS NOT NULL AND plan_id    IS NULL AND product_id IS NULL)
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_order_items_product') THEN
        ALTER TABLE order_items
            ADD CONSTRAINT fk_order_items_product
            FOREIGN KEY (product_id, studio_id)
            REFERENCES products (id, studio_id) ON DELETE RESTRICT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_order_items_session') THEN
        ALTER TABLE order_items
            ADD CONSTRAINT fk_order_items_session
            FOREIGN KEY (session_id, studio_id)
            REFERENCES class_sessions (id, studio_id) ON DELETE RESTRICT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_order_items_booking') THEN
        ALTER TABLE order_items
            ADD CONSTRAINT fk_order_items_booking
            FOREIGN KEY (booking_id, studio_id)
            REFERENCES bookings (id, studio_id) ON DELETE SET NULL (booking_id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_order_items_studio_product
    ON order_items (studio_id, product_id) WHERE product_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_order_items_order_type
    ON order_items (order_id, item_type);
