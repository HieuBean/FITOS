CREATE TABLE purchase_receipts (
    id BIGSERIAL PRIMARY KEY,
    studio_id BIGINT NOT NULL REFERENCES studios(id) ON DELETE CASCADE,
    receipt_no VARCHAR(50) NOT NULL,
    supplier_name VARCHAR(150) NOT NULL,
    note VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'CONFIRMED')),
    total_amount NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (total_amount >= 0),
    source_transaction_id BIGINT UNIQUE,
    created_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    confirmed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_purchase_receipts_studio_no UNIQUE (studio_id, receipt_no),
    CONSTRAINT uq_purchase_receipts_id_studio UNIQUE (id, studio_id)
);

CREATE INDEX idx_purchase_receipts_report
    ON purchase_receipts (studio_id, confirmed_at) WHERE status = 'CONFIRMED';

CREATE TABLE purchase_receipt_items (
    id BIGSERIAL PRIMARY KEY,
    studio_id BIGINT NOT NULL,
    receipt_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_cost NUMERIC(12,2) NOT NULL CHECK (unit_cost >= 0),
    line_total NUMERIC(12,2) NOT NULL CHECK (line_total >= 0),
    CONSTRAINT fk_purchase_items_receipt FOREIGN KEY (receipt_id, studio_id)
        REFERENCES purchase_receipts (id, studio_id) ON DELETE CASCADE,
    CONSTRAINT fk_purchase_items_product FOREIGN KEY (product_id, studio_id)
        REFERENCES products (id, studio_id),
    CONSTRAINT uq_purchase_item_product UNIQUE (receipt_id, product_id)
);

CREATE INDEX idx_purchase_items_product ON purchase_receipt_items (studio_id, product_id);

ALTER TABLE inventory_transactions ADD COLUMN purchase_receipt_id BIGINT;
ALTER TABLE inventory_transactions ADD CONSTRAINT fk_inventory_purchase_receipt
    FOREIGN KEY (purchase_receipt_id, studio_id)
    REFERENCES purchase_receipts (id, studio_id);
CREATE INDEX idx_inventory_purchase_receipt ON inventory_transactions (purchase_receipt_id);

-- Historical stock entries are represented as confirmed one-line receipts.
-- No inventory quantity or cost is changed by this migration.
INSERT INTO purchase_receipts
    (studio_id, receipt_no, supplier_name, note, status, total_amount,
     source_transaction_id, created_by, confirmed_at, created_at, updated_at)
SELECT tx.studio_id, 'PNH-CU-' || tx.id, 'Không xác định (dữ liệu cũ)',
       tx.note, 'CONFIRMED', tx.quantity_delta * COALESCE(tx.unit_cost, 0),
       tx.id, tx.created_by, tx.created_at, tx.created_at, tx.created_at
FROM inventory_transactions tx
WHERE tx.transaction_type = 'STOCK_IN';

INSERT INTO purchase_receipt_items
    (studio_id, receipt_id, product_id, quantity, unit_cost, line_total)
SELECT tx.studio_id, receipt.id, tx.product_id, tx.quantity_delta,
       COALESCE(tx.unit_cost, 0), tx.quantity_delta * COALESCE(tx.unit_cost, 0)
FROM inventory_transactions tx
JOIN purchase_receipts receipt ON receipt.source_transaction_id = tx.id;

UPDATE inventory_transactions tx
SET purchase_receipt_id = receipt.id
FROM purchase_receipts receipt
WHERE receipt.source_transaction_id = tx.id;
