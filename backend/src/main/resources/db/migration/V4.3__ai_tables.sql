-- ============================================================
-- V4.3__ai_tables.sql
-- Layer 1 - Task #15 (Người D)
-- Bảng lưu trữ AI conversations, messages, recommendations
-- Dùng JSONB để lưu structured output từ LLM
-- ============================================================

-- Mỗi phiên chat với AI = 1 conversation
CREATE TABLE IF NOT EXISTS ai_conversations (
    id          BIGSERIAL   PRIMARY KEY,
    customer_id BIGINT      NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    service     VARCHAR(20) NOT NULL,
    -- CRM | COMMERCE | OPERATIONS | WORKOUT
    title       VARCHAR(200),
    -- tóm tắt tự động (optional)
    started_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    ended_at    TIMESTAMP,
    metadata    JSONB        NOT NULL DEFAULT '{}'
    -- lưu context: workout_plan_id, current_goal, v.v.
);

-- Từng tin nhắn trong conversation
CREATE TABLE IF NOT EXISTS ai_messages (
    id              BIGSERIAL   PRIMARY KEY,
    conversation_id BIGINT      NOT NULL REFERENCES ai_conversations(id) ON DELETE CASCADE,
    role            VARCHAR(10) NOT NULL CHECK (role IN ('user', 'assistant', 'system')),
    content         TEXT        NOT NULL,
    tokens_used     INTEGER,
    model_used      VARCHAR(50),
    -- gpt-4o-mini, gpt-4o, v.v.
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- Gợi ý từ AI (structured JSON output) — có thể ACCEPT hoặc DISMISS
CREATE TABLE IF NOT EXISTS ai_recommendations (
    id              BIGSERIAL   PRIMARY KEY,
    customer_id     BIGINT      NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    service         VARCHAR(20) NOT NULL,
    -- CRM | COMMERCE | OPERATIONS | WORKOUT
    rec_type        VARCHAR(50) NOT NULL,
    -- WORKOUT_PLAN_CHANGE | EXERCISE_SWAP | INTENSITY_ADJUST | NUTRITION_TIP | ...
    recommendation  JSONB       NOT NULL,
    -- structured JSON từ LLM, ví dụ:
    -- { "suggested_changes": [...], "reason": "...", "priority": "HIGH" }
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    -- PENDING | ACCEPTED | DISMISSED | EXPIRED
    expires_at      TIMESTAMP,
    acted_at        TIMESTAMP,
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_ai_conv_customer        ON ai_conversations(customer_id);
CREATE INDEX IF NOT EXISTS idx_ai_conv_service         ON ai_conversations(service);
CREATE INDEX IF NOT EXISTS idx_ai_conv_started         ON ai_conversations(started_at DESC);
CREATE INDEX IF NOT EXISTS idx_ai_messages_conv        ON ai_messages(conversation_id);
CREATE INDEX IF NOT EXISTS idx_ai_messages_created     ON ai_messages(created_at);
CREATE INDEX IF NOT EXISTS idx_ai_rec_customer         ON ai_recommendations(customer_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ai_rec_status           ON ai_recommendations(status);
-- GIN index để query bên trong JSONB (tìm theo rec_type, priority, v.v.)
CREATE INDEX IF NOT EXISTS idx_ai_rec_jsonb            ON ai_recommendations USING GIN(recommendation);
