-- V1.1: Users, Roles, User_Roles & Initial Seed Admin
-- Author: Member A (Auth & Security)

CREATE TABLE IF NOT EXISTS roles (
    id   SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    phone         VARCHAR(20),
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id INT    NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_users_email     ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_is_active ON users(is_active);

-- Seed system roles
INSERT INTO roles (name) VALUES 
('ROLE_ADMIN'),
('ROLE_MANAGER'),
('ROLE_RECEPTIONIST'),
('ROLE_TRAINER'),
('ROLE_MEMBER')
ON CONFLICT (name) DO NOTHING;

-- Seed default System Admin (password: Admin@123 -> BCrypt hash)
INSERT INTO users (email, password_hash, full_name, phone, is_active)
VALUES (
    'admin@fitos.com',
    '$2a$10$WpQjY/7rG4p4q3m4E7wL3.cI1X6y8U4Z3i8V5M2z7B9Q0k1L2N3O.',
    'System Admin',
    '0901234567',
    TRUE
)
ON CONFLICT (email) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r
WHERE u.email = 'admin@fitos.com' AND r.name = 'ROLE_ADMIN'
ON CONFLICT DO NOTHING;
