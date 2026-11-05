-- V2.1: Service Plans Catalog (Memberships & PT Packages)
-- Author: Member B (Commerce & Plans) & Team Merged

CREATE TABLE IF NOT EXISTS service_plans (
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(100)   NOT NULL UNIQUE,
    plan_type      VARCHAR(50)    NOT NULL
                     CHECK (plan_type IN ('MEMBERSHIP', 'PT_PACKAGE')),
    price          NUMERIC(12, 2) NOT NULL CHECK (price >= 0),
    duration_days  INT            NOT NULL CHECK (duration_days > 0),
    session_count  INT            DEFAULT NULL CHECK (session_count IS NULL OR session_count > 0),
    total_sessions INT            DEFAULT NULL CHECK (total_sessions IS NULL OR total_sessions > 0),
    description    TEXT,
    features       JSONB          DEFAULT '[]'::jsonb,
    is_active      BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_service_plans_type_active ON service_plans(plan_type, is_active);

-- Seed standard Membership Plans & PT Packages
INSERT INTO service_plans (name, plan_type, price, duration_days, session_count, total_sessions, description, features) VALUES
(
    'Move',
    'MEMBERSHIP',
    29.00,
    30,
    NULL,
    NULL,
    'Perfect for getting started with studio workouts.',
    '["Up to 8 classes/month", "App access", "Progress tracking", "Community access"]'::jsonb
),
(
    'Perform',
    'MEMBERSHIP',
    49.00,
    30,
    NULL,
    NULL,
    'Our most popular plan with unlimited classes and AI coaching.',
    '["Unlimited classes", "AI coaching", "Priority booking", "Nutrition guidance", "Monthly check-in"]'::jsonb
),
(
    'PT Focus',
    'MEMBERSHIP',
    89.00,
    30,
    NULL,
    NULL,
    'Maximum results with personal training included.',
    '["Unlimited classes", "4 PT sessions/month", "Custom programme", "Body composition tracking", "Direct trainer access"]'::jsonb
),
(
    'PT Starter 5',
    'PT_PACKAGE',
    150.00,
    60,
    5,
    5,
    '5 private personal training sessions for beginners.',
    '["5 1-on-1 PT sessions", "Fitness assessment", "Custom workout prescription"]'::jsonb
),
(
    'PT Pro 10',
    'PT_PACKAGE',
    280.00,
    90,
    10,
    10,
    '10 personal training sessions for steady progression.',
    '["10 1-on-1 PT sessions", "Nutrition review", "Bi-weekly body metrics analysis"]'::jsonb
),
(
    'PT Elite 20',
    'PT_PACKAGE',
    500.00,
    180,
    20,
    20,
    '20 comprehensive coaching sessions for maximum body transformation.',
    '["20 1-on-1 PT sessions", "Dedicated coach", "Full nutrition & macro plan", "24/7 AI & coach chat"]'::jsonb
)
ON CONFLICT (name) DO NOTHING;
