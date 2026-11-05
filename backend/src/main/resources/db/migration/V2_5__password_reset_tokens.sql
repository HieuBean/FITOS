-- =====================================================================
-- V2_5 — Password reset tokens (Task #20 — Người B)
-- Phụ thuộc: users (V1 — Người A)
-- Chỉ lưu SHA-256 của token (không lưu token thô) → lộ DB cũng không reset được mật khẩu.
-- =====================================================================

CREATE TABLE password_reset_tokens (
    id           BIGSERIAL    PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    token_hash   CHAR(64)     NOT NULL,                -- hex SHA-256
    expires_at   TIMESTAMPTZ  NOT NULL,
    used_at      TIMESTAMPTZ,
    requested_ip VARCHAR(45),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_password_reset_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_password_reset_expiry CHECK (expires_at > created_at)
);

CREATE INDEX idx_password_reset_user_created ON password_reset_tokens (user_id, created_at DESC);
