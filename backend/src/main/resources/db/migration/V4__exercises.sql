-- ============================================================
-- V4__exercises.sql
-- Layer 1 - Task #12 (Người D)
-- Bảng danh mục bài tập: exercises
-- ============================================================

CREATE TABLE IF NOT EXISTS exercises (
    id           BIGSERIAL    PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    description  TEXT,
    muscle_group VARCHAR(50)  NOT NULL,
    -- CHEST | BACK | LEGS | SHOULDERS | ARMS | CORE | CARDIO | FULL_BODY
    equipment    VARCHAR(50),
    -- BARBELL | DUMBBELL | MACHINE | BODYWEIGHT | CABLE | KETTLEBELL | BAND | OTHER
    difficulty   VARCHAR(20)  NOT NULL DEFAULT 'BEGINNER',
    -- BEGINNER | INTERMEDIATE | ADVANCED
    video_url    TEXT,
    image_url    TEXT,
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Index tìm kiếm theo nhóm cơ và độ khó
CREATE INDEX IF NOT EXISTS idx_exercises_muscle_group ON exercises(muscle_group);
CREATE INDEX IF NOT EXISTS idx_exercises_difficulty   ON exercises(difficulty);
CREATE INDEX IF NOT EXISTS idx_exercises_is_active    ON exercises(is_active);

-- ============================================================
-- Seed 30 bài tập mẫu (Task #56 của C nhưng đặt ở đây cho tiện)
-- ============================================================
INSERT INTO exercises (name, description, muscle_group, equipment, difficulty) VALUES
-- CHEST
('Bench Press',        'Nằm ngửa, đẩy tạ thẳng lên',                'CHEST',     'BARBELL',    'INTERMEDIATE'),
('Push Up',            'Chống đẩy tay rộng hơn vai',                 'CHEST',     'BODYWEIGHT', 'BEGINNER'),
('Dumbbell Fly',       'Mở rộng tay ngang tạ đôi',                   'CHEST',     'DUMBBELL',   'INTERMEDIATE'),
('Incline Press',      'Đẩy tạ trên ghế nghiêng 30–45 độ',           'CHEST',     'BARBELL',    'INTERMEDIATE'),
-- BACK
('Pull Up',            'Đu xà kéo cơ lưng',                          'BACK',      'BODYWEIGHT', 'INTERMEDIATE'),
('Barbell Row',        'Kéo tạ đòn sát ngực tư thế cúi người',       'BACK',      'BARBELL',    'INTERMEDIATE'),
('Lat Pulldown',       'Kéo thanh ngang xuống trước ngực',           'BACK',      'CABLE',      'BEGINNER'),
('Deadlift',           'Kéo tạ từ sàn lên, tổng hợp toàn thân',     'BACK',      'BARBELL',    'ADVANCED'),
-- LEGS
('Squat',              'Ngồi xuống – đứng lên với tạ đòn',           'LEGS',      'BARBELL',    'INTERMEDIATE'),
('Leg Press',          'Đẩy sàn máy tạ chân',                        'LEGS',      'MACHINE',    'BEGINNER'),
('Lunge',              'Bước lunge xen kẽ hai chân',                 'LEGS',      'DUMBBELL',   'BEGINNER'),
('Romanian Deadlift',  'Deadlift chân thẳng tập cơ đùi sau',         'LEGS',      'BARBELL',    'INTERMEDIATE'),
('Leg Curl',           'Gập gối trên máy tập cơ đùi sau',            'LEGS',      'MACHINE',    'BEGINNER'),
('Calf Raise',         'Nhón gót tập cơ bắp chân',                   'LEGS',      'MACHINE',    'BEGINNER'),
-- SHOULDERS
('Overhead Press',     'Đẩy tạ thẳng qua đầu',                       'SHOULDERS', 'BARBELL',    'INTERMEDIATE'),
('Lateral Raise',      'Nâng tạ ngang hai bên',                      'SHOULDERS', 'DUMBBELL',   'BEGINNER'),
('Front Raise',        'Nâng tạ về phía trước',                      'SHOULDERS', 'DUMBBELL',   'BEGINNER'),
('Arnold Press',       'Đẩy tạ đôi xoay cổ tay',                    'SHOULDERS', 'DUMBBELL',   'INTERMEDIATE'),
-- ARMS
('Bicep Curl',         'Cuộn tạ đôi bắp tay trước',                  'ARMS',      'DUMBBELL',   'BEGINNER'),
('Tricep Dip',         'Chống tay sau ghế dips bắp tay sau',         'ARMS',      'BODYWEIGHT', 'BEGINNER'),
('Hammer Curl',        'Cuộn tạ đôi tư thế cầm búa',                 'ARMS',      'DUMBBELL',   'BEGINNER'),
('Skull Crusher',      'Đẩy tạ đòn EZ bar sau gáy nằm ngửa',        'ARMS',      'BARBELL',    'INTERMEDIATE'),
-- CORE
('Plank',              'Giữ tư thế ván gỗ thẳng người',              'CORE',      'BODYWEIGHT', 'BEGINNER'),
('Crunch',             'Gập bụng cơ bản',                            'CORE',      'BODYWEIGHT', 'BEGINNER'),
('Leg Raise',          'Nâng chân thẳng từ nằm ngửa',               'CORE',      'BODYWEIGHT', 'INTERMEDIATE'),
('Russian Twist',      'Xoay người cầm tạ tư thế ngồi',              'CORE',      'DUMBBELL',   'INTERMEDIATE'),
-- CARDIO
('Treadmill Run',      'Chạy bộ trên máy tốc độ 8–12 km/h',         'CARDIO',    'MACHINE',    'BEGINNER'),
('Jump Rope',          'Nhảy dây liên tục',                           'CARDIO',    'BAND',       'BEGINNER'),
('Rowing Machine',     'Chèo thuyền trên máy',                       'CARDIO',    'MACHINE',    'INTERMEDIATE'),
('Burpee',             'Kết hợp nhảy – chống đẩy – đứng dậy',       'CARDIO',    'BODYWEIGHT', 'ADVANCED');

