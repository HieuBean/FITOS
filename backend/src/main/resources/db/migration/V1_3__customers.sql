-- V1.3: Customers Table & Lead-Customer Link
-- Author: Member A (CRM & Customers)

CREATE TABLE IF NOT EXISTS customers (
    id                      BIGSERIAL PRIMARY KEY,
    user_id                 BIGINT UNIQUE REFERENCES users(id) ON DELETE SET NULL,
    lead_id                 BIGINT UNIQUE REFERENCES leads(id) ON DELETE SET NULL,
    full_name               VARCHAR(100) NOT NULL,
    phone                   VARCHAR(20)  NOT NULL,
    email                   VARCHAR(100),
    dob                     DATE,
    gender                  VARCHAR(20)  CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
    fitness_goal            VARCHAR(50)  CHECK (fitness_goal IN ('WEIGHT_LOSS', 'MUSCLE_GAIN', 'GENERAL_FITNESS', 'STRENGTH', 'MOBILITY', 'OTHER')),
    status                  VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED')),
    emergency_contact_name  VARCHAR(100),
    emergency_contact_phone VARCHAR(20),
    created_at              TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_customers_user_id ON customers(user_id);
CREATE INDEX IF NOT EXISTS idx_customers_phone   ON customers(phone);
CREATE INDEX IF NOT EXISTS idx_customers_status  ON customers(status);

-- Liên kết ngược từ leads.converted_customer_id sang customers(id)
ALTER TABLE leads
    ADD CONSTRAINT fk_leads_converted_customer
    FOREIGN KEY (converted_customer_id) REFERENCES customers(id) ON DELETE SET NULL;
