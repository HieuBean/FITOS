-- V5.2: AI conversations and messages schema alignment
-- Align database columns with AiConversation and AiMessage entity mappings

ALTER TABLE ai_conversations
    ADD COLUMN IF NOT EXISTS service VARCHAR(20) NOT NULL DEFAULT 'WORKOUT',
    ADD COLUMN IF NOT EXISTS started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS ended_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS metadata JSONB NOT NULL DEFAULT '{}';

ALTER TABLE ai_messages
    ADD COLUMN IF NOT EXISTS tokens_used INT,
    ADD COLUMN IF NOT EXISTS model_used VARCHAR(50);

ALTER TABLE exercises
    ADD COLUMN IF NOT EXISTS description TEXT,
    ADD COLUMN IF NOT EXISTS image_url VARCHAR(255),
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;

ALTER TABLE password_reset_tokens
    ALTER COLUMN token_hash TYPE VARCHAR(64);

