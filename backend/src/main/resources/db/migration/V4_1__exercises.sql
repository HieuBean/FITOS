-- V4.1: Exercises table & 30 seed exercises
-- Author: Member D (Workout & AI)

CREATE TABLE IF NOT EXISTS exercises (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    category VARCHAR(50) NOT NULL, -- 'COMPOUND', 'UPPER_PUSH', 'UPPER_PULL', 'LOWER_BODY', 'CORE', 'CARDIO'
    muscle_group VARCHAR(100) NOT NULL,
    equipment VARCHAR(50) NOT NULL, -- 'Barbell', 'Dumbbell', 'Machine', 'Cable', 'Bodyweight'
    instruction TEXT,
    video_url VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_exercises_category ON exercises(category);
CREATE INDEX IF NOT EXISTS idx_exercises_muscle_group ON exercises(muscle_group);

-- Seed 30 standard gym exercises
INSERT INTO exercises (name, category, muscle_group, equipment, instruction) VALUES
('Barbell Back Squat', 'COMPOUND', 'Quads, Glutes', 'Barbell', 'Place bar on upper traps, descend until thighs parallel with floor, drive back up.'),
('Barbell Front Squat', 'COMPOUND', 'Quads, Core', 'Barbell', 'Rest bar on front deltoids, keep torso upright, squat deep and rise.'),
('Conventional Deadlift', 'COMPOUND', 'Hamstrings, Glutes, Back', 'Barbell', 'Hinge at hips, grip bar shoulder-width, drive through heels and stand tall.'),
('Romanian Deadlift', 'COMPOUND', 'Hamstrings, Glutes', 'Barbell', 'Keep slight knee bend, push hips back, lower bar below knees, squeeze glutes to return.'),
('Leg Press', 'LOWER_BODY', 'Quads, Glutes', 'Machine', 'Place feet shoulder-width on platform, release safety, lower under control, press up.'),
('Bulgarian Split Squat', 'LOWER_BODY', 'Quads, Glutes', 'Dumbbell', 'Elevate rear foot on bench, descend front leg until knee at 90 degrees, push up.'),
('Leg Extension', 'LOWER_BODY', 'Quads', 'Machine', 'Sit on machine with pad against shins, extend legs until straight, pause and lower.'),
('Lying Leg Curl', 'LOWER_BODY', 'Hamstrings', 'Machine', 'Lie face down, curl weight toward glutes, squeeze hamstrings, slowly lower.'),
('Standing Calf Raise', 'LOWER_BODY', 'Calves', 'Machine', 'Place balls of feet on edge, lower heels for full stretch, push up onto toes.'),
('Barbell Bench Press', 'COMPOUND', 'Chest, Triceps, Front Delts', 'Barbell', 'Lie on bench, lower bar to mid-chest with control, press upward locking arms.'),
('Incline Dumbbell Press', 'UPPER_PUSH', 'Upper Chest, Front Delts', 'Dumbbell', 'Set bench at 30 degrees, press dumbbells upward over upper chest, lower under control.'),
('Dips', 'UPPER_PUSH', 'Chest, Triceps', 'Bodyweight', 'Support body on parallel bars, lower until shoulders below elbows, push back up.'),
('Overhead Press (OHP)', 'COMPOUND', 'Shoulders, Triceps', 'Barbell', 'Stand tall, press bar overhead until arms locked, lower to collarbone.'),
('Dumbbell Shoulder Press', 'UPPER_PUSH', 'Shoulders, Triceps', 'Dumbbell', 'Seated upright, press dumbbells vertically overhead, lower to ear level.'),
('Dumbbell Lateral Raise', 'UPPER_PUSH', 'Side Delts', 'Dumbbell', 'Raise dumbbells out to sides until parallel to floor, slight bend in elbows.'),
('Face Pull', 'UPPER_PULL', 'Rear Delts, Rotator Cuff', 'Cable', 'Attach rope to high pulley, pull towards face while rotating hands back.'),
('Pull-up', 'COMPOUND', 'Lats, Biceps, Upper Back', 'Bodyweight', 'Pronated grip wider than shoulders, pull chest to bar, lower fully.'),
('Chin-up', 'UPPER_PULL', 'Lats, Biceps', 'Bodyweight', 'Supinated shoulder-width grip, pull chin over bar, lower with control.'),
('Barbell Bent-Over Row', 'COMPOUND', 'Back, Lats, Rhomboids', 'Barbell', 'Torso at 45 degrees, pull bar into lower ribcage, squeeze shoulder blades.'),
('Lat Pulldown', 'UPPER_PULL', 'Lats', 'Cable', 'Grip wide bar, pull down to upper chest, squeeze lats, return slowly.'),
('Seated Cable Row', 'UPPER_PULL', 'Mid Back, Lats', 'Cable', 'Sit upright, pull V-bar to abdomen, retract scapulae, stretch forward.'),
('Barbell Bicep Curl', 'UPPER_PULL', 'Biceps', 'Barbell', 'Stand upright, curl bar towards shoulders keeping elbows stationary, lower slowly.'),
('Incline Dumbbell Curl', 'UPPER_PULL', 'Biceps', 'Dumbbell', 'Set bench at 45 degrees, let arms hang, curl dumbbells with full supination.'),
('Triceps Rope Pushdown', 'UPPER_PUSH', 'Triceps', 'Cable', 'Grip rope on high pulley, extend arms downward spreading rope at bottom.'),
('Skull Crushers', 'UPPER_PUSH', 'Triceps', 'Barbell', 'Lie on bench with EZ bar, bend elbows lowering bar to forehead, press up.'),
('Plank', 'CORE', 'Core, Transverse Abdominis', 'Bodyweight', 'Hold forearm plank position keeping body in a straight line from head to heels.'),
('Hanging Leg Raise', 'CORE', 'Lower Abs, Hip Flexors', 'Bodyweight', 'Hang from bar, raise toes towards bar without swinging, lower under control.'),
('Cable Woodchopper', 'CORE', 'Obliques', 'Cable', 'Rotate torso diagonally across body pulling cable, engage rotational core.'),
('Treadmill 5K Run', 'CARDIO', 'Cardiovascular, Legs', 'Machine', 'Continuous tempo run on treadmill at aerobic heart rate zone.'),
('Rowing Machine Intervals', 'CARDIO', 'Full Body, Cardiovascular', 'Machine', 'High-intensity rowing intervals: 500m sprint, 1 min rest.')
ON CONFLICT (name) DO NOTHING;
