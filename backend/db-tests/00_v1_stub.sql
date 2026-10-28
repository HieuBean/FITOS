-- Stub tối thiểu cho V1 của Người A (chỉ dùng để test, KHÔNG commit)
CREATE TABLE users (id BIGSERIAL PRIMARY KEY, email VARCHAR(255) UNIQUE NOT NULL);
CREATE TABLE customers (id BIGSERIAL PRIMARY KEY, user_id BIGINT REFERENCES users(id), full_name VARCHAR(100));
INSERT INTO users (email) VALUES ('sales@fitos.vn');
INSERT INTO customers (full_name) VALUES ('Nguyen Van Test');
