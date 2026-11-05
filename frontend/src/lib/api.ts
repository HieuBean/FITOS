import axios from 'axios';

const API_BASE = process.env.NEXT_PUBLIC_BACKEND_URL || 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,
});

// Request interceptor to attach JWT token
api.interceptors.request.use(
  (config) => {
    if (typeof window !== 'undefined') {
      const token = localStorage.getItem('fitos_access_token') || localStorage.getItem('fitos_token');
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor for handling 401 unauthorized
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      if (typeof window !== 'undefined') {
        const refreshToken = localStorage.getItem('fitos_refresh_token');
        if (refreshToken) {
          try {
            const res = await axios.post(`${API_BASE}/auth/refresh`, { refreshToken });
            const { accessToken } = res.data;
            localStorage.setItem('fitos_access_token', accessToken);
            document.cookie = `fitos_token=${accessToken}; path=/; max-age=604800; SameSite=Lax`;
            originalRequest.headers.Authorization = `Bearer ${accessToken}`;
            return api(originalRequest);
          } catch (refreshErr) {
            localStorage.removeItem('fitos_access_token');
            localStorage.removeItem('fitos_refresh_token');
            localStorage.removeItem('fitos_user');
            localStorage.removeItem('fitos_token');
            document.cookie = 'fitos_token=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT';
            if (!window.location.pathname.startsWith('/login')) {
              window.location.href = '/login';
            }
          }
        }
      }
    }
    return Promise.reject(error);
  }
);

export const apiClient = api;
export default api;

// ==========================================
// TYPE DEFINITIONS
// ==========================================

export interface Exercise {
  id: number;
  name: string;
  description?: string;
  instruction?: string;
  muscleGroup: string;
  equipment?: string;
  difficulty?: string;
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
  restSeconds?: number;
  targetRpe?: number;
  weightSuggest?: number;
  notes?: string;
}

export interface WorkoutPlan {
  id?: number;
  customerId?: number;
  trainerId?: number;
  name: string;
  description?: string;
  goal?: string;
  difficulty?: string;
  targetDaysPerWeek?: number;
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

export interface WorkoutLogPayload {
  customerId: number;
  workoutPlanId?: number;
  sessionDate?: string;
  durationMinutes: number;
  energyLevel?: number;
  notes?: string;
  items: Array<{
    exerciseId: number;
    setNumber: number;
    weightKg: number;
    repsCompleted: number;
    rpe?: number;
  }>;
}

// ==========================================
// API HELPER FUNCTIONS
// ==========================================

export async function getExercises(muscleGroup?: string): Promise<Exercise[]> {
  try {
    const url = muscleGroup && muscleGroup !== 'ALL'
      ? `${API_BASE}/exercises?muscleGroup=${encodeURIComponent(muscleGroup)}&size=50`
      : `${API_BASE}/exercises?size=50`;
    const res = await fetch(url);
    if (!res.ok) throw new Error('Failed to fetch exercises');
    const data = await res.json();
    return Array.isArray(data) ? data : data.content || [];
  } catch (err) {
    console.warn('API getExercises fallback:', err);
    return [
      { id: 1, name: 'Barbell Bench Press', description: 'Nằm ngửa đẩy tạ đòn thẳng lên', muscleGroup: 'CHEST', equipment: 'Barbell', difficulty: 'INTERMEDIATE' },
      { id: 2, name: 'Incline Dumbbell Press', description: 'Đẩy tạ đơn ghế dốc lên trên', muscleGroup: 'CHEST', equipment: 'Dumbbell', difficulty: 'INTERMEDIATE' },
      { id: 3, name: 'Cable Crossover', description: 'Kéo cáp ép ngực', muscleGroup: 'CHEST', equipment: 'Cable', difficulty: 'BEGINNER' },
      { id: 7, name: 'Barbell Back Squat', description: 'Gánh tạ đòn ngồi xổm sâu', muscleGroup: 'LEGS', equipment: 'Barbell', difficulty: 'ADVANCED' },
      { id: 8, name: 'Leg Press', description: 'Đạp đùi máy nghiêng 45 độ', muscleGroup: 'LEGS', equipment: 'Machine', difficulty: 'BEGINNER' },
      { id: 9, name: 'Romanian Deadlift', description: 'Kéo tạ đòn gập hông kéo căng cơ đùi sau', muscleGroup: 'LEGS', equipment: 'Barbell', difficulty: 'INTERMEDIATE' },
      { id: 10, name: 'Overhead Press (OHP)', description: 'Đẩy tạ đòn qua đầu đứng thẳng', muscleGroup: 'SHOULDERS', equipment: 'Barbell', difficulty: 'INTERMEDIATE' },
      { id: 11, name: 'Lateral Raise', description: 'Dang tạ đơn sang 2 bên phát triển vai giữa', muscleGroup: 'SHOULDERS', equipment: 'Dumbbell', difficulty: 'BEGINNER' },
      { id: 13, name: 'Barbell Row', description: 'Cúi người kéo tạ đòn vào bụng', muscleGroup: 'BACK', equipment: 'Barbell', difficulty: 'INTERMEDIATE' },
      { id: 14, name: 'Lat Pulldown', description: 'Kéo xô máy xuống ngực trên', muscleGroup: 'BACK', equipment: 'Cable', difficulty: 'BEGINNER' },
    ];
  }
}

export async function getWorkoutPlans(customerId = 1): Promise<WorkoutPlan[]> {
  try {
    const res = await fetch(`${API_BASE}/workout-plans?customerId=${customerId}`);
    if (!res.ok) throw new Error('Failed to fetch workout plans');
    const data = await res.json();
    return Array.isArray(data) ? data : data.content || [];
  } catch (err) {
    console.warn('API getWorkoutPlans fallback:', err);
    return [
      {
        id: 1,
        name: 'Giáo án Tăng Cơ 4 Ngày (Upper / Lower)',
        description: 'Tập trung phì đại cơ bắp, cân bằng thân trên và thân dưới',
        goal: 'MUSCLE_GAIN',
        difficulty: 'INTERMEDIATE',
        targetDaysPerWeek: 4,
        status: 'ACTIVE',
        exercises: [
          { exerciseId: 1, exerciseName: 'Barbell Bench Press', muscleGroup: 'CHEST', dayOfWeek: 1, sets: 4, reps: 8, restSecs: 90, targetRpe: 8 },
          { exerciseId: 10, exerciseName: 'Overhead Press (OHP)', muscleGroup: 'SHOULDERS', dayOfWeek: 1, sets: 3, reps: 10, restSecs: 60, targetRpe: 8 },
          { exerciseId: 13, exerciseName: 'Barbell Row', muscleGroup: 'BACK', dayOfWeek: 1, sets: 4, reps: 10, restSecs: 90, targetRpe: 8 },
          { exerciseId: 7, exerciseName: 'Barbell Back Squat', muscleGroup: 'LEGS', dayOfWeek: 2, sets: 4, reps: 6, restSecs: 120, targetRpe: 8 },
          { exerciseId: 9, exerciseName: 'Romanian Deadlift', muscleGroup: 'LEGS', dayOfWeek: 2, sets: 3, reps: 8, restSecs: 90, targetRpe: 7 },
        ]
      },
      {
        id: 2,
        name: 'Giảm Mỡ Đốt Calo Toàn Diện (Full Body HIIT)',
        description: 'Cường độ cao ngắt quãng kết hợp tạ nhẹ giúp siết cơ',
        goal: 'FAT_LOSS',
        difficulty: 'BEGINNER',
        targetDaysPerWeek: 3,
        status: 'ACTIVE',
        exercises: [
          { exerciseId: 8, exerciseName: 'Leg Press', muscleGroup: 'LEGS', dayOfWeek: 1, sets: 3, reps: 15, restSecs: 45, targetRpe: 7 },
          { exerciseId: 14, exerciseName: 'Lat Pulldown', muscleGroup: 'BACK', dayOfWeek: 1, sets: 3, reps: 12, restSecs: 45, targetRpe: 7 },
          { exerciseId: 3, exerciseName: 'Cable Crossover', muscleGroup: 'CHEST', dayOfWeek: 1, sets: 3, reps: 15, restSecs: 45, targetRpe: 7 },
        ]
      }
    ];
  }
}

export async function getWorkoutPlanById(id: number): Promise<WorkoutPlan | null> {
  try {
    const res = await fetch(`${API_BASE}/workout-plans/${id}`);
    if (!res.ok) throw new Error('Failed to fetch plan detail');
    return await res.json();
  } catch (err) {
    console.warn('API getWorkoutPlanById fallback:', err);
    const plans = await getWorkoutPlans();
    return plans.find(p => p.id === id) || plans[0] || null;
  }
}

export async function createWorkoutPlan(plan: WorkoutPlan): Promise<WorkoutPlan> {
  const res = await fetch(`${API_BASE}/workout-plans`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(plan),
  });
  if (!res.ok) throw new Error('Failed to create plan');
  return await res.json();
}

export async function getProgressSummary(customerId = 1): Promise<ProgressSummary> {
  try {
    const res = await fetch(`${API_BASE}/progress/summary?customerId=${customerId}`);
    if (!res.ok) throw new Error('Failed to fetch progress');
    return await res.json();
  } catch (err) {
    console.warn('API getProgressSummary fallback:', err);
    return {
      customerId,
      streak: 12,
      totalSessionsCompleted: 48,
      sessionsThisMonth: 14,
      personalRecords: [
        { exerciseId: 1, exerciseName: 'Barbell Bench Press', maxWeightKg: 85, maxReps: 5 },
        { exerciseId: 7, exerciseName: 'Barbell Back Squat', maxWeightKg: 120, maxReps: 4 },
        { exerciseId: 9, exerciseName: 'Romanian Deadlift', maxWeightKg: 140, maxReps: 5 },
        { exerciseId: 10, exerciseName: 'Overhead Press (OHP)', maxWeightKg: 55, maxReps: 6 },
      ],
    };
  }
}

export async function getRecentWorkoutSessions(customerId = 1): Promise<WorkoutSession[]> {
  try {
    const res = await fetch(`${API_BASE}/workout-sessions?customerId=${customerId}&size=5`);
    if (!res.ok) throw new Error('Failed to fetch recent sessions');
    const data = await res.json();
    return Array.isArray(data) ? data : data.content || [];
  } catch (err) {
    return [
      { id: 1, customerId, startedAt: '2026-10-28T09:00:00', completedAt: '2026-10-28T10:15:00', status: 'COMPLETED', totalDurationSecs: 4500, caloriesBurned: 420, notes: 'Đẩy ngực sung sức, đạt PR' },
      { id: 2, customerId, startedAt: '2026-10-26T17:30:00', completedAt: '2026-10-26T18:35:00', status: 'COMPLETED', totalDurationSecs: 3900, caloriesBurned: 510, notes: 'Squat tăng thêm 5kg' },
      { id: 3, customerId, startedAt: '2026-10-24T08:00:00', completedAt: '2026-10-24T09:00:00', status: 'COMPLETED', totalDurationSecs: 3600, caloriesBurned: 380, notes: 'Kéo xô lưng hoàn thành tốt' },
    ];
  }
}

export async function startWorkoutSession(customerId = 1, workoutPlanId?: number): Promise<WorkoutSession> {
  try {
    const res = await fetch(`${API_BASE}/workout-sessions`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ customerId, workoutPlanId, notes: 'Buổi tập tại phòng gym' }),
    });
    if (!res.ok) throw new Error('Failed to start session');
    return await res.json();
  } catch {
    return {
      id: Date.now(),
      customerId,
      workoutPlanId,
      startedAt: new Date().toISOString(),
      status: 'IN_PROGRESS',
    };
  }
}

export async function logWorkoutSet(sessionId: number, exerciseId: number, setNumber: number, repsActual: number, weightKg?: number, rpe?: number): Promise<any> {
  try {
    const res = await fetch(`${API_BASE}/workout-logs`, {
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
  } catch {
    return { id: Date.now(), workoutSessionId: sessionId, exerciseId, setNumber, repsActual, weightKg, rpe };
  }
}

export async function completeWorkoutSession(sessionId: number, caloriesBurned = 350, notes = 'Hoàn thành buổi tập xuất sắc'): Promise<WorkoutSession> {
  try {
    const res = await fetch(`${API_BASE}/workout-sessions/${sessionId}/complete`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ caloriesBurned, notes }),
    });
    if (!res.ok) throw new Error('Failed to complete session');
    return await res.json();
  } catch {
    return {
      id: sessionId,
      customerId: 1,
      startedAt: new Date(Date.now() - 3600000).toISOString(),
      completedAt: new Date().toISOString(),
      status: 'COMPLETED',
      caloriesBurned,
      notes,
    };
  }
}

export async function saveWorkoutLog(payload: WorkoutLogPayload) {
  const res = await api.post('/workout-logs', payload);
  return res.data;
}

export async function sendAiChat(message: string, customerId = 1, conversationId?: number) {
  try {
    const res = await api.post('/ai/chat', {
      customerId,
      message,
      conversation_history: [{ role: 'user', content: message }]
    });
    return res.data;
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
    const res = await api.post('/ai/recommend', {
      customerId,
      goal,
      targetMuscleGroup: targetMuscle,
      currentFitnessLevel: fitnessLevel
    });
    return res.data;
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
  const res = await api.post(`/ai/recommend/${recId}/accept`);
  return res.status === 200;
}

export async function dismissAiRecommendation(recId: number) {
  const res = await api.post(`/ai/recommend/${recId}/dismiss`);
  return res.status === 200;
}

export async function completePTSession(sessionId: number): Promise<boolean> {
  try {
    const res = await api.patch(`/pt-sessions/${sessionId}/complete`);
    return res.status === 200;
  } catch (err) {
    console.warn('API completePTSession fallback:', err);
    return true;
  }
}
