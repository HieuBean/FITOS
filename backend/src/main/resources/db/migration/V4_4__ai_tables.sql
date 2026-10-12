-- V4.4: AI conversation history & recommendations
-- Author: Member D (Workout & AI)

CREATE TABLE IF NOT EXISTS ai_conversations (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL DEFAULT 'Consultation',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'customers') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fk_ai_conv_customer') THEN
            ALTER TABLE ai_conversations ADD CONSTRAINT fk_ai_conv_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE;
        END IF;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_ai_conv_customer ON ai_conversations(customer_id, updated_at DESC);

CREATE TABLE IF NOT EXISTS ai_messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('user', 'assistant', 'system')),
    content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ai_msg_conv FOREIGN KEY (conversation_id) REFERENCES ai_conversations(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_ai_msg_conv_time ON ai_messages(conversation_id, created_at ASC);

CREATE TABLE IF NOT EXISTS ai_recommendations (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL, -- 'WORKOUT_CHANGE', 'RECOVERY', 'PROGRESSION'
    title VARCHAR(150) NOT NULL,
    explanation TEXT NOT NULL,
    suggested_changes JSONB NOT NULL, -- JSON structure of workout adjustments
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACCEPTED', 'DISMISSED')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMP WITH TIME ZONE
);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'customers') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE constraint_name = 'fk_ai_rec_customer') THEN
            ALTER TABLE ai_recommendations ADD CONSTRAINT fk_ai_rec_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE;
        END IF;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_ai_rec_customer_status ON ai_recommendations(customer_id, status);
CREATE INDEX IF NOT EXISTS idx_ai_rec_changes_gin ON ai_recommendations USING gin(suggested_changes);
