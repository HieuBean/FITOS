const API_BASE = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

export interface Exercise {
  id: number;
  name: string;
  description: string;
  muscleGroup: string;
  equipment?: string;
  difficulty: string;
  videoUrl?: string;
  imageUrl?: string;
}

export interface PlanExerciseItem {
  id?: number;
  exerciseId: number;
  exerciseName?: string;
  muscleGroup?: string;
  dayOfWeek: number;
  sets: number;
  reps: number;
  durationSecs?: number;
  restSecs?: number;
  weightSuggest?: number;
  notes?: string;
}

export interface WorkoutPlan {
  id?: number;
  customerId: number;
  trainerId?: number;
  name: string;
  description?: string;
  goal?: string;
  status?: string;
  startDate?: string;
  endDate?: string;
  exercises?: PlanExerciseItem[];
}

export interface ProgressSummary {
  customerId: number;
  streak: number;
  totalSessionsCompleted: number;
  sessionsThisMonth: number;
  personalRecords: {
    exerciseId: number;
    exerciseName: string;
    maxWeightKg: number;
    maxReps: number;
  }[];
}

export interface WorkoutSession {
  id: number;
  customerId: number;
  workoutPlanId?: number;
  startedAt: string;
  completedAt?: string;
  status: string;
  totalDurationSecs?: number;
  caloriesBurned?: number;
  notes?: string;
  logs?: WorkoutLogItem[];
}

export interface WorkoutLogItem {
  id: number;
  exerciseId: number;
  exerciseName: string;
  muscleGroup: string;
  setNumber: number;
  repsActual: number;
  weightKg?: number;
  durationSecs?: number;
  rpe?: number;
  notes?: string;
  loggedAt: string;
}

// === EXERCISES ===
export async function getExercises(muscleGroup?: string): Promise<Exercise[]> {
  try {
    const url = muscleGroup && muscleGroup !== 'ALL'
      ? `${API_BASE}/api/exercises?muscleGroup=${encodeURIComponent(muscleGroup)}&size=50`
      : `${API_BASE}/api/exercises?size=50`;
    const res = await fetch(url);
    if (!res.ok) throw new Error('Failed to fetch exercises');
    const data = await res.json();
    return data.content || data;
  } catch (err) {
    console.warn('API getExercises fallback:', err);
    return [
      { id: 1, name: 'Bench Press', description: 'Nằm ngửa đẩy tạ thẳng lên', muscleGroup: 'CHEST', equipment: 'BARBELL', difficulty: 'INTERMEDIATE', videoUrl: 'https://assets.mixkit.co/videos/preview/mixkit-man-working-out-with-a-barbell-in-a-gym-43093-large.mp4' },
      { id: 2, name: 'Push Up', description: 'Chống đẩy cơ ngực và tay sau', muscleGroup: 'CHEST', equipment: 'BODYWEIGHT', difficulty: 'BEGINNER' },
      { id: 3, name: 'Incline Dumbbell Press', description: 'Đẩy tạ đôi trên ghế dốc', muscleGroup: 'CHEST', equipment: 'DUMBBELL', difficulty: 'INTERMEDIATE' },
      { id: 4, name: 'Pull Up', description: 'Đu xà kéo xô lưng', muscleGroup: 'BACK', equipment: 'BODYWEIGHT', difficulty: 'INTERMEDIATE' },
      { id: 5, name: 'Barbell Row', description: 'Kéo tạ đòn cúi người tập cơ lưng', muscleGroup: 'BACK', equipment: 'BARBELL', difficulty: 'INTERMEDIATE' },
      { id: 6, name: 'Lat Pulldown', description: 'Kéo cáp xô', muscleGroup: 'BACK', equipment: 'CABLE', difficulty: 'BEGINNER' },
      { id: 7, name: 'Barbell Squat', description: 'Ngồi xổm gánh tạ đòn', muscleGroup: 'LEGS', equipment: 'BARBELL', difficulty: 'INTERMEDIATE' },
      { id: 8, name: 'Leg Press', description: 'Đẩy bàn đạp tạ máy', muscleGroup: 'LEGS', equipment: 'MACHINE', difficulty: 'BEGINNER' },
      { id: 9, name: 'Romanian Deadlift', description: 'Deadlift đùi sau và mông', muscleGroup: 'LEGS', equipment: 'BARBELL', difficulty: 'INTERMEDIATE' },
      { id: 10, name: 'Overhead Press', description: 'Đẩy tạ qua đầu tập cơ vai', muscleGroup: 'SHOULDERS', equipment: 'BARBELL', difficulty: 'INTERMEDIATE' },
      { id: 11, name: 'Lateral Raise', description: 'Dang tạ đôi hai bên vai', muscleGroup: 'SHOULDERS', equipment: 'DUMBBELL', difficulty: 'BEGINNER' },
      { id: 12, name: 'Bicep Curl', description: 'Cuộn tạ tay trước', muscleGroup: 'ARMS', equipment: 'DUMBBELL', difficulty: 'BEGINNER' },
      { id: 13, name: 'Tricep Dip', description: 'Chống xà kép bắp tay sau', muscleGroup: 'ARMS', equipment: 'BODYWEIGHT', difficulty: 'INTERMEDIATE' },
      { id: 14, name: 'Plank', description: 'Giữ thân ván siết cơ lõi', muscleGroup: 'CORE', equipment: 'BODYWEIGHT', difficulty: 'BEGINNER' },
      { id: 15, name: 'Treadmill Run', description: 'Chạy bộ biến tốc trên máy', muscleGroup: 'CARDIO', equipment: 'MACHINE', difficulty: 'BEGINNER' }
    ];
  }
}

// === WORKOUT PLANS ===
export async function getWorkoutPlans(customerId = 1): Promise<WorkoutPlan[]> {
  try {
    const res = await fetch(`${API_BASE}/api/workout-plans?customerId=${customerId}`);
    if (!res.ok) throw new Error('Failed to fetch plans');
    return await res.json();
  } catch (err) {
    console.warn('API getWorkoutPlans fallback:', err);
    return [
      {
        id: 1,
        customerId,
        name: '4-Day Hypertrophy Split',
        goal: 'MUSCLE_GAIN',
        status: 'ACTIVE',
        description: 'Chương trình tăng cơ 4 ngày / tuần tiêu chuẩn',
        startDate: '2026-10-01',
        endDate: '2026-12-31',
        exercises: [
          { exerciseId: 1, exerciseName: 'Bench Press', muscleGroup: 'CHEST', dayOfWeek: 1, sets: 4, reps: 8, restSecs: 90, weightSuggest: 70 },
          { exerciseId: 7, exerciseName: 'Barbell Squat', muscleGroup: 'LEGS', dayOfWeek: 2, sets: 4, reps: 6, restSecs: 120, weightSuggest: 90 },
          { exerciseId: 4, exerciseName: 'Pull Up', muscleGroup: 'BACK', dayOfWeek: 4, sets: 4, reps: 8, restSecs: 90 },
          { exerciseId: 10, exerciseName: 'Overhead Press', muscleGroup: 'SHOULDERS', dayOfWeek: 5, sets: 3, reps: 10, restSecs: 60, weightSuggest: 45 },
        ]
      }
    ];
  }
}

export async function createWorkoutPlan(plan: WorkoutPlan): Promise<WorkoutPlan> {
  const res = await fetch(`${API_BASE}/api/workout-plans`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(plan),
  });
  if (!res.ok) throw new Error('Failed to create workout plan');
  return await res.json();
}

export async function activateWorkoutPlan(planId: number): Promise<void> {
  const res = await fetch(`${API_BASE}/api/workout-plans/${planId}/activate`, {
    method: 'PATCH',
  });
  if (!res.ok) throw new Error('Failed to activate workout plan');
}

// === PROGRESS & SESSIONS ===
export async function getProgressSummary(customerId = 1): Promise<ProgressSummary> {
  try {
    const res = await fetch(`${API_BASE}/api/progress/me?customerId=${customerId}`);
    if (!res.ok) throw new Error('Failed to fetch progress');
    return await res.json();
  } catch (err) {
    console.warn('API getProgressSummary fallback:', err);
    return {
      customerId,
      streak: 12,
      totalSessionsCompleted: 48,
      sessionsThisMonth: 9,
      personalRecords: [
        { exerciseId: 1, exerciseName: 'Bench Press', maxWeightKg: 85, maxReps: 5 },
        { exerciseId: 7, exerciseName: 'Barbell Squat', maxWeightKg: 120, maxReps: 4 },
        { exerciseId: 9, exerciseName: 'Romanian Deadlift', maxWeightKg: 140, maxReps: 5 },
        { exerciseId: 10, exerciseName: 'Overhead Press', maxWeightKg: 55, maxReps: 6 },
      ]
    };
  }
}

export async function getWorkoutSessions(customerId = 1): Promise<WorkoutSession[]> {
  try {
    const res = await fetch(`${API_BASE}/api/workout-sessions/me?customerId=${customerId}`);
    if (!res.ok) throw new Error('Failed to fetch sessions');
    return await res.json();
  } catch (err) {
    console.warn('API getWorkoutSessions fallback:', err);
    return [
      { id: 1, customerId, startedAt: '2026-10-28T09:00:00', completedAt: '2026-10-28T10:15:00', status: 'COMPLETED', totalDurationSecs: 4500, caloriesBurned: 420, notes: 'Tập ngực & tay sau rất sung' },
      { id: 2, customerId, startedAt: '2026-10-26T17:30:00', completedAt: '2026-10-26T18:35:00', status: 'COMPLETED', totalDurationSecs: 3900, caloriesBurned: 510, notes: 'Squat tăng thêm 5kg' },
      { id: 3, customerId, startedAt: '2026-10-24T08:00:00', completedAt: '2026-10-24T09:00:00', status: 'COMPLETED', totalDurationSecs: 3600, caloriesBurned: 380, notes: 'Kéo xô lưng hoàn thành tốt' },
    ];
  }
}

export async function startWorkoutSession(customerId = 1, workoutPlanId?: number): Promise<WorkoutSession> {
  const res = await fetch(`${API_BASE}/api/workout-sessions`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ customerId, workoutPlanId, notes: 'Buổi tập tại phòng gym' }),
  });
  if (!res.ok) throw new Error('Failed to start session');
  return await res.json();
}

export async function logWorkoutSet(sessionId: number, exerciseId: number, setNumber: number, repsActual: number, weightKg?: number, rpe?: number): Promise<any> {
  const res = await fetch(`${API_BASE}/api/workout-logs`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      workoutSessionId: sessionId,
      exerciseId,
      setNumber,
      repsActual,
      weightKg,
      rpe,
      notes: 'Log từ app',
    }),
  });
  if (!res.ok) throw new Error('Failed to log set');
  return await res.json();
}

export async function completeWorkoutSession(sessionId: number, caloriesBurned = 350, notes = 'Hoàn thành buổi tập xuất sắc'): Promise<WorkoutSession> {
  const res = await fetch(`${API_BASE}/api/workout-sessions/${sessionId}/complete`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ caloriesBurned, notes }),
  });
  if (!res.ok) throw new Error('Failed to complete session');
  return await res.json();
}

// === AI COACH ===
export async function sendAiChat(message: string, customerId = 1, conversationId?: number) {
  try {
    const res = await fetch(`${API_BASE}/api/ai/chat`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ customerId, message, conversationId }),
    });
    if (!res.ok) throw new Error('Failed to chat with AI');
    return await res.json();
  } catch (err) {
    console.warn('API sendAiChat fallback:', err);
    return {
      conversationId: conversationId || 1,
      reply: `Tôi đã nhận được câu hỏi "${message}". Lời khuyên thể lực: Duy trì nhịp thở khi gồng core, bổ sung đủ 2L nước mỗi ngày và khởi động kỹ 5 phút trước khi nâng tạ nặng!`,
      suggestions: ['Lập lịch tập ngực & vai', 'Thực đơn giảm mỡ', 'Cách chỉnh form Squat'],
      fallback: true
    };
  }
}

export async function requestAiRecommendation(customerId = 1, goal = 'MUSCLE_GAIN', targetMuscle = 'CHEST', fitnessLevel = 'INTERMEDIATE') {
  try {
    const res = await fetch(`${API_BASE}/api/ai/recommend`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ customerId, goal, targetMuscleGroup: targetMuscle, currentFitnessLevel: fitnessLevel }),
    });
    if (!res.ok) throw new Error('Failed to get recommendation');
    return await res.json();
  } catch (err) {
    console.warn('API requestAiRecommendation fallback:', err);
    return {
      recommendationId: 101,
      title: 'Đề xuất tối ưu hóa buổi tập Ngực & Tay sau',
      rawJson: JSON.stringify({
        suggested_changes: [
          'Thêm 1 set Bench Press tạ nặng (4 reps RPE 9)',
          'Thay bài Cable Fly bằng Incline Dumbbell Press',
          'Nghỉ 90 giây giữa các hiệp nặng'
        ],
        reason: 'Tăng kích thích phì đại cơ ngực trên dựa trên thể trạng hiện tại'
      }),
      status: 'PENDING'
    };
  }
}

export async function acceptAiRecommendation(recId: number) {
  const res = await fetch(`${API_BASE}/api/ai/recommend/${recId}/accept`, {
    method: 'PATCH'
  });
  return res.ok;
}

export async function dismissAiRecommendation(recId: number) {
  const res = await fetch(`${API_BASE}/api/ai/recommend/${recId}/dismiss`, {
    method: 'PATCH'
  });
  return res.ok;
}

// === PT SESSIONS ===
export async function completePTSession(sessionId: number): Promise<boolean> {
  try {
    const res = await fetch(`${API_BASE}/api/pt-sessions/${sessionId}/complete`, {
      method: 'PATCH'
    });
    return res.ok;
  } catch (err) {
    console.warn('API completePTSession fallback:', err);
    return true;
  }
}


