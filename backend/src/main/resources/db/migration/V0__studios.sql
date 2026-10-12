-- V0: Initial Studios table (for starter demo)
CREATE TABLE IF NOT EXISTS studios (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(255),
    phone VARCHAR(50)
);
