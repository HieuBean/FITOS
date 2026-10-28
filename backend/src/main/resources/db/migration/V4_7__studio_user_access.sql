-- Users are global identities; their operational roles and customer profiles are studio scoped.
CREATE TABLE user_studio_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    studio_id BIGINT NOT NULL REFERENCES studios(id) ON DELETE CASCADE,
    role_id INT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, studio_id, role_id)
);

INSERT INTO user_studio_roles (user_id, studio_id, role_id)
SELECT u.id, u.studio_id, ur.role_id
FROM users u JOIN user_roles ur ON ur.user_id = u.id
ON CONFLICT DO NOTHING;

INSERT INTO roles (name) VALUES ('ROLE_PLATFORM_ADMIN') ON CONFLICT (name) DO NOTHING;
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u CROSS JOIN roles r
WHERE u.email = 'admin@fitos.com' AND r.name = 'ROLE_PLATFORM_ADMIN'
ON CONFLICT DO NOTHING;

-- A user may work at several studios and may have a customer profile in each.
ALTER TABLE leads DROP CONSTRAINT IF EXISTS fk_tenant_leads_assigned_to;
ALTER TABLE lead_activities DROP CONSTRAINT IF EXISTS fk_tenant_lead_activities_user_id;
ALTER TABLE audit_logs DROP CONSTRAINT IF EXISTS fk_tenant_audit_logs_user_id;
ALTER TABLE customers DROP CONSTRAINT IF EXISTS fk_tenant_customers_user_id;
ALTER TABLE trainers DROP CONSTRAINT IF EXISTS fk_tenant_trainers_user_id;
ALTER TABLE attendance DROP CONSTRAINT IF EXISTS fk_tenant_attendance_checked_in_by;
ALTER TABLE customers DROP CONSTRAINT IF EXISTS customers_user_id_key;
CREATE UNIQUE INDEX uq_customers_user_studio ON customers (user_id, studio_id) WHERE user_id IS NOT NULL;
