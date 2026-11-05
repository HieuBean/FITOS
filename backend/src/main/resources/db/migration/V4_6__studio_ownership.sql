-- Existing single-studio data belongs to the oldest studio. Legacy application
-- writes still use that studio by default until the application selects one.
INSERT INTO studios (name, address)
SELECT 'FITOS Default', 'Legacy studio'
WHERE NOT EXISTS (SELECT 1 FROM studios);

DO $$
DECLARE
    legacy_studio_id BIGINT;
    table_name TEXT;
    relation RECORD;
BEGIN
    SELECT MIN(id) INTO legacy_studio_id FROM studios;

    FOREACH table_name IN ARRAY ARRAY[
        'users', 'leads', 'lead_activities', 'audit_logs', 'customers',
        'service_plans', 'orders', 'order_items', 'payments', 'memberships',
        'pt_packages', 'trainers', 'trainer_assignments', 'classes',
        'class_sessions', 'bookings', 'attendance', 'pt_sessions',
        'workout_plans', 'workout_plan_exercises', 'workout_sessions',
        'workout_logs', 'body_metrics', 'ai_conversations', 'ai_messages',
        'ai_recommendations'
    ] LOOP
        EXECUTE format('ALTER TABLE %I ADD COLUMN studio_id BIGINT', table_name);
    END LOOP;

    UPDATE users SET studio_id = legacy_studio_id;
    UPDATE leads SET studio_id = legacy_studio_id;
    UPDATE customers SET studio_id = legacy_studio_id;
    UPDATE service_plans SET studio_id = legacy_studio_id;
    UPDATE trainers SET studio_id = legacy_studio_id;
    UPDATE classes SET studio_id = legacy_studio_id;

    UPDATE lead_activities x SET studio_id = p.studio_id FROM leads p WHERE x.lead_id = p.id;
    UPDATE audit_logs x SET studio_id = COALESCE(p.studio_id, legacy_studio_id)
        FROM (SELECT id, studio_id FROM users) p WHERE x.user_id = p.id;
    UPDATE audit_logs SET studio_id = legacy_studio_id WHERE studio_id IS NULL;
    UPDATE orders x SET studio_id = p.studio_id FROM customers p WHERE x.customer_id = p.id;
    UPDATE order_items x SET studio_id = p.studio_id FROM orders p WHERE x.order_id = p.id;
    UPDATE payments x SET studio_id = p.studio_id FROM orders p WHERE x.order_id = p.id;
    UPDATE memberships x SET studio_id = p.studio_id FROM customers p WHERE x.customer_id = p.id;
    UPDATE pt_packages x SET studio_id = p.studio_id FROM customers p WHERE x.customer_id = p.id;
    UPDATE trainer_assignments x SET studio_id = p.studio_id FROM trainers p WHERE x.trainer_id = p.id;
    UPDATE class_sessions x SET studio_id = p.studio_id FROM classes p WHERE x.class_id = p.id;
    UPDATE bookings x SET studio_id = p.studio_id FROM class_sessions p WHERE x.session_id = p.id;
    UPDATE attendance x SET studio_id = p.studio_id FROM customers p WHERE x.customer_id = p.id;
    UPDATE pt_sessions x SET studio_id = p.studio_id FROM customers p WHERE x.customer_id = p.id;
    UPDATE workout_plans x SET studio_id = p.studio_id FROM customers p WHERE x.customer_id = p.id;
    UPDATE workout_plan_exercises x SET studio_id = p.studio_id FROM workout_plans p WHERE x.plan_id = p.id;
    UPDATE workout_sessions x SET studio_id = p.studio_id FROM customers p WHERE x.customer_id = p.id;
    UPDATE workout_logs x SET studio_id = p.studio_id FROM workout_sessions p WHERE x.session_id = p.id;
    UPDATE body_metrics x SET studio_id = p.studio_id FROM customers p WHERE x.customer_id = p.id;
    UPDATE ai_conversations x SET studio_id = p.studio_id FROM customers p WHERE x.customer_id = p.id;
    UPDATE ai_messages x SET studio_id = p.studio_id FROM ai_conversations p WHERE x.conversation_id = p.id;
    UPDATE ai_recommendations x SET studio_id = p.studio_id FROM customers p WHERE x.customer_id = p.id;

    FOREACH table_name IN ARRAY ARRAY[
        'users', 'leads', 'lead_activities', 'audit_logs', 'customers',
        'service_plans', 'orders', 'order_items', 'payments', 'memberships',
        'pt_packages', 'trainers', 'trainer_assignments', 'classes',
        'class_sessions', 'bookings', 'attendance', 'pt_sessions',
        'workout_plans', 'workout_plan_exercises', 'workout_sessions',
        'workout_logs', 'body_metrics', 'ai_conversations', 'ai_messages',
        'ai_recommendations'
    ] LOOP
        EXECUTE format('ALTER TABLE %I ALTER COLUMN studio_id SET NOT NULL', table_name);
        EXECUTE format('ALTER TABLE %I ALTER COLUMN studio_id SET DEFAULT %s', table_name, legacy_studio_id);
        EXECUTE format('ALTER TABLE %I ADD CONSTRAINT fk_%s_studio FOREIGN KEY (studio_id) REFERENCES studios(id)', table_name, table_name);
        EXECUTE format('ALTER TABLE %I ADD CONSTRAINT uq_%s_id_studio UNIQUE (id, studio_id)', table_name, table_name);
        EXECUTE format('CREATE INDEX idx_%s_studio ON %I (studio_id)', table_name, table_name);
    END LOOP;

    ALTER TABLE service_plans DROP CONSTRAINT IF EXISTS service_plans_name_key;
    ALTER TABLE service_plans ADD CONSTRAINT uq_service_plans_studio_name UNIQUE (studio_id, name);
    CREATE INDEX idx_leads_studio_status ON leads (studio_id, status);
    CREATE INDEX idx_class_sessions_studio_date ON class_sessions (studio_id, session_date);
    CREATE INDEX idx_payments_studio_status_paid ON payments (studio_id, status, paid_at);

    -- Enforce that references cannot point at a row belonging to another studio.
    FOR relation IN SELECT * FROM (VALUES
        ('leads', 'assigned_to', 'users', 'SET NULL'),
        ('leads', 'converted_customer_id', 'customers', 'SET NULL'),
        ('lead_activities', 'lead_id', 'leads', 'CASCADE'),
        ('lead_activities', 'user_id', 'users', 'SET NULL'),
        ('audit_logs', 'user_id', 'users', 'SET NULL'),
        ('customers', 'user_id', 'users', 'SET NULL'),
        ('customers', 'lead_id', 'leads', 'SET NULL'),
        ('orders', 'customer_id', 'customers', 'CASCADE'),
        ('order_items', 'order_id', 'orders', 'CASCADE'),
        ('order_items', 'plan_id', 'service_plans', 'RESTRICT'),
        ('payments', 'order_id', 'orders', 'RESTRICT'),
        ('payments', 'customer_id', 'customers', 'RESTRICT'),
        ('memberships', 'customer_id', 'customers', 'CASCADE'),
        ('memberships', 'plan_id', 'service_plans', 'RESTRICT'),
        ('memberships', 'order_id', 'orders', 'SET NULL'),
        ('pt_packages', 'customer_id', 'customers', 'CASCADE'),
        ('pt_packages', 'plan_id', 'service_plans', 'RESTRICT'),
        ('pt_packages', 'order_id', 'orders', 'SET NULL'),
        ('trainers', 'user_id', 'users', 'SET NULL'),
        ('trainer_assignments', 'trainer_id', 'trainers', 'CASCADE'),
        ('trainer_assignments', 'customer_id', 'customers', 'CASCADE'),
        ('class_sessions', 'class_id', 'classes', 'CASCADE'),
        ('class_sessions', 'trainer_id', 'trainers', 'SET NULL'),
        ('bookings', 'customer_id', 'customers', 'CASCADE'),
        ('bookings', 'session_id', 'class_sessions', 'CASCADE'),
        ('attendance', 'customer_id', 'customers', 'CASCADE'),
        ('attendance', 'booking_id', 'bookings', 'SET NULL'),
        ('attendance', 'class_session_id', 'class_sessions', 'SET NULL'),
        ('attendance', 'checked_in_by', 'users', 'SET NULL'),
        ('pt_sessions', 'trainer_id', 'trainers', 'CASCADE'),
        ('pt_sessions', 'customer_id', 'customers', 'CASCADE'),
        ('pt_sessions', 'pt_package_id', 'pt_packages', 'SET NULL'),
        ('workout_plans', 'customer_id', 'customers', 'CASCADE'),
        ('workout_plans', 'trainer_id', 'trainers', 'SET NULL'),
        ('workout_plan_exercises', 'plan_id', 'workout_plans', 'CASCADE'),
        ('workout_sessions', 'customer_id', 'customers', 'CASCADE'),
        ('workout_sessions', 'plan_id', 'workout_plans', 'SET NULL'),
        ('workout_logs', 'session_id', 'workout_sessions', 'CASCADE'),
        ('workout_logs', 'plan_exercise_id', 'workout_plan_exercises', 'SET NULL'),
        ('body_metrics', 'customer_id', 'customers', 'CASCADE'),
        ('ai_conversations', 'customer_id', 'customers', 'CASCADE'),
        ('ai_messages', 'conversation_id', 'ai_conversations', 'CASCADE'),
        ('ai_recommendations', 'customer_id', 'customers', 'CASCADE'),
        ('ai_recommendations', 'conversation_id', 'ai_conversations', 'SET NULL')
    ) AS r(child_table, child_column, parent_table, delete_action) LOOP
        EXECUTE format(
            'ALTER TABLE %I ADD CONSTRAINT fk_tenant_%s_%s FOREIGN KEY (%I, studio_id) REFERENCES %I (id, studio_id) ON DELETE %s',
            relation.child_table, relation.child_table, relation.child_column,
            relation.child_column, relation.parent_table,
            CASE WHEN relation.delete_action = 'SET NULL'
                THEN format('SET NULL (%I)', relation.child_column)
                ELSE relation.delete_action END
        );
    END LOOP;
END $$;
