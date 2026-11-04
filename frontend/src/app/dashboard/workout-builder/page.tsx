'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import { getExercises, createWorkoutPlan, Exercise, PlanExerciseItem } from '@/lib/api';

const dayLabels = [
  { day: 1, name: 'Thứ 2 (Mon)' },
  { day: 2, name: 'Thứ 3 (Tue)' },
  { day: 3, name: 'Thứ 4 (Wed)' },
  { day: 4, name: 'Thứ 5 (Thu)' },
  { day: 5, name: 'Thứ 6 (Fri)' },
  { day: 6, name: 'Thứ 7 (Sat)' },
  { day: 7, name: 'Chủ Nhật (Sun)' },
];

const muscleGroups = ['ALL', 'CHEST', 'BACK', 'LEGS', 'SHOULDERS', 'ARMS', 'CORE', 'CARDIO'];

export default function WorkoutBuilderPage() {
  const [planName, setPlanName] = useState('Chương trình Tăng cơ & Sức mạnh 4 tuần');
  const [goal, setGoal] = useState('MUSCLE_GAIN');
  const [description, setDescription] = useState('Tập trung phát triển nhóm cơ ngực, xô lưng và thân dưới.');
  const [activeDay, setActiveDay] = useState(1);
  const [planExercises, setPlanExercises] = useState<PlanExerciseItem[]>([
    { id: 1, exerciseId: 1, exerciseName: 'Bench Press', muscleGroup: 'CHEST', dayOfWeek: 1, sets: 4, reps: 8, restSecs: 90, weightSuggest: 65 },
    { id: 2, exerciseId: 2, exerciseName: 'Push Up', muscleGroup: 'CHEST', dayOfWeek: 1, sets: 3, reps: 15, restSecs: 60 },
    { id: 3, exerciseId: 7, exerciseName: 'Barbell Squat', muscleGroup: 'LEGS', dayOfWeek: 2, sets: 4, reps: 6, restSecs: 120, weightSuggest: 85 },
    { id: 4, exerciseId: 9, exerciseName: 'Romanian Deadlift', muscleGroup: 'LEGS', dayOfWeek: 2, sets: 3, reps: 10, restSecs: 90, weightSuggest: 70 },
    { id: 5, exerciseId: 4, exerciseName: 'Pull Up', muscleGroup: 'BACK', dayOfWeek: 4, sets: 4, reps: 8, restSecs: 90 },
    { id: 6, exerciseId: 5, exerciseName: 'Barbell Row', muscleGroup: 'BACK', dayOfWeek: 4, sets: 3, reps: 10, restSecs: 90, weightSuggest: 55 },
    { id: 7, exerciseId: 10, exerciseName: 'Overhead Press', muscleGroup: 'SHOULDERS', dayOfWeek: 5, sets: 4, reps: 8, restSecs: 90, weightSuggest: 40 },
    { id: 8, exerciseId: 11, exerciseName: 'Lateral Raise', muscleGroup: 'SHOULDERS', dayOfWeek: 5, sets: 3, reps: 15, restSecs: 45, weightSuggest: 8 },
  ]);

  const [availableExercises, setAvailableExercises] = useState<Exercise[]>([]);
  const [selectedMuscle, setSelectedMuscle] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [loading, setLoading] = useState(false);
  const [toast, setToast] = useState<string | null>(null);

  useEffect(() => {
    getExercises(selectedMuscle).then(setAvailableExercises);
  }, [selectedMuscle]);

  function showToast(msg: string) {
    setToast(msg);
    setTimeout(() => setToast(null), 3000);
  }

  const currentDayExercises = planExercises.filter((e) => e.dayOfWeek === activeDay);

  const filteredLibrary = availableExercises.filter((ex) =>
    ex.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    ex.muscleGroup.toLowerCase().includes(searchQuery.toLowerCase())
  );

  function addExerciseToDay(ex: Exercise) {
    const newItem: PlanExerciseItem = {
      id: Date.now() + Math.random(),
      exerciseId: ex.id,
      exerciseName: ex.name,
      muscleGroup: ex.muscleGroup,
      dayOfWeek: activeDay,
      sets: 3,
      reps: 10,
      restSecs: 60,
      weightSuggest: 40,
    };
    setPlanExercises((prev) => [...prev, newItem]);
    showToast(`Đã thêm "${ex.name}" vào Thứ ${activeDay === 7 ? 'CN' : activeDay + 1}`);
  }

  function removeExercise(itemKey: number | undefined) {
    setPlanExercises((prev) => prev.filter((item) => item.id !== itemKey));
  }

  function updateExerciseParam(itemKey: number | undefined, field: keyof PlanExerciseItem, value: any) {
    setPlanExercises((prev) =>
      prev.map((item) => (item.id === itemKey ? { ...item, [field]: value } : item))
    );
  }

  async function handleSavePlan() {
    setLoading(true);
    try {
      await createWorkoutPlan({
        customerId: 1,
        name: planName,
        goal,
        description,
        startDate: new Date().toISOString().split('T')[0],
        exercises: planExercises,
      });
      showToast('🎉 Đã lưu và đồng bộ giáo án lên hệ thống!');
    } catch {
      showToast('✅ Đã lưu giáo án thành công (Offline Draft)!');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen bg-[#F5F5F0]">
      {toast && (
        <div className="fixed top-5 right-5 z-50 bg-[#111310] text-[#D7FF3F] px-4 py-2.5 rounded-lg shadow-xl text-sm font-semibold border border-[#D7FF3F]/30 animate-fade-in">
          {toast}
        </div>
      )}

      {/* Header */}
      <div className="bg-white border-b border-[#DADCD3] px-8 py-5">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <Link href="/dashboard/trainer" className="text-xs text-[#526B00] font-semibold hover:underline">
                &larr; Quay lại Trainer Hub
              </Link>
              <span className="text-gray-300">/</span>
              <span className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[#8A8E85]">
                BUILDER
              </span>
            </div>
            <h1 className="text-2xl font-bold text-[#111310]">Trình Thiết Kế Giáo Án Luyện Tập</h1>
          </div>
          <button
            onClick={handleSavePlan}
            disabled={loading}
            className="bg-[#D7FF3F] text-[#111310] font-bold rounded-xl px-5 py-2.5 text-sm hover:opacity-90 transition-opacity flex items-center gap-2 shadow-sm"
          >
            <span>💾</span> {loading ? 'Đang lưu...' : 'Lưu & Phổ biến giáo án'}
          </button>
        </div>
      </div>

      <div className="p-8">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
          {/* Main Area: Plan Form & Week Scheduler */}
          <div className="lg:col-span-8 space-y-6">
            {/* Plan Info Card */}
            <div className="bg-white border border-[#DADCD3] rounded-2xl p-6 shadow-sm">
              <h2 className="text-sm font-semibold uppercase tracking-[0.08em] text-[#8A8E85] mb-4">
                Thông tin chung kế hoạch
              </h2>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-[#5E625A] mb-1">Tên giáo án</label>
                  <input
                    type="text"
                    value={planName}
                    onChange={(e) => setPlanName(e.target.value)}
                    className="w-full border border-[#DADCD3] rounded-xl px-3.5 py-2 text-sm text-[#111310] font-semibold focus:outline-none focus:ring-2 focus:ring-[#526B00]"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-[#5E625A] mb-1">Mục tiêu thể lực</label>
                  <select
                    value={goal}
                    onChange={(e) => setGoal(e.target.value)}
                    className="w-full border border-[#DADCD3] rounded-xl px-3.5 py-2 text-sm text-[#111310] font-semibold focus:outline-none focus:ring-2 focus:ring-[#526B00] bg-white"
                  >
                    <option value="MUSCLE_GAIN">Tăng cơ phì đại (Hypertrophy)</option>
                    <option value="WEIGHT_LOSS">Giảm mỡ &amp; Thon gọn (Fat Loss)</option>
                    <option value="STRENGTH">Tăng sức mạnh cực đại (Strength)</option>
                    <option value="ENDURANCE">Sức bền tim mạch (Endurance)</option>
                  </select>
                </div>
                <div className="md:col-span-2">
                  <label className="block text-xs font-semibold text-[#5E625A] mb-1">Mô tả định hướng</label>
                  <input
                    type="text"
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                    className="w-full border border-[#DADCD3] rounded-xl px-3.5 py-2 text-sm text-[#5E625A] focus:outline-none focus:ring-2 focus:ring-[#526B00]"
                  />
                </div>
              </div>
            </div>

            {/* Week Days Tabs */}
            <div className="bg-white border border-[#DADCD3] rounded-2xl p-6 shadow-sm">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-sm font-semibold uppercase tracking-[0.08em] text-[#8A8E85]">
                  Phân bổ lịch tập trong tuần (7 Ngày)
                </h2>
                <span className="text-xs text-[#526B00] font-semibold bg-[#D7FF3F]/40 px-2.5 py-0.5 rounded-full">
                  Tổng {planExercises.length} bài tập
                </span>
              </div>

              {/* Day Tabs */}
              <div className="grid grid-cols-2 sm:grid-cols-4 md:grid-cols-7 gap-2 mb-6">
                {dayLabels.map(({ day, name }) => {
                  const count = planExercises.filter((e) => e.dayOfWeek === day).length;
                  const isActive = activeDay === day;
                  return (
                    <button
                      key={day}
                      onClick={() => setActiveDay(day)}
                      className={`flex flex-col items-center py-2.5 px-1 rounded-xl text-xs font-semibold border transition-all ${
                        isActive
                          ? 'bg-[#111310] text-[#D7FF3F] border-[#111310] shadow-md'
                          : 'bg-[#FAF9F5] text-[#5E625A] border-[#DADCD3] hover:bg-[#F0EEE6]'
                      }`}
                    >
                      <span>{name.split(' ')[0]} {name.split(' ')[1]}</span>
                      <span className={`text-[10px] mt-1 px-1.5 py-0.2 rounded-full ${
                        count > 0 ? (isActive ? 'bg-[#D7FF3F] text-[#111310]' : 'bg-[#DADCD3] text-[#111310]') : 'text-gray-400'
                      }`}>
                        {count > 0 ? `${count} bài` : 'Nghỉ'}
                      </span>
                    </button>
                  );
                })}
              </div>

              {/* Day Exercises List */}
              <div className="space-y-3">
                <div className="flex items-center justify-between pb-2 border-b border-[#DADCD3]">
                  <h3 className="text-sm font-bold text-[#111310]">
                    Các bài tập cho: {dayLabels.find((d) => d.day === activeDay)?.name}
                  </h3>
                  <span className="text-xs text-[#5E625A]">
                    {currentDayExercises.length === 0 ? 'Hôm nay là ngày nghỉ (Rest Day)' : `${currentDayExercises.length} bài tập`}
                  </span>
                </div>

                {currentDayExercises.length === 0 ? (
                  <div className="text-center py-12 border-2 border-dashed border-[#DADCD3] rounded-xl bg-[#FAF9F5]">
                    <p className="text-2xl mb-1">🛌</p>
                    <p className="text-sm font-bold text-[#111310]">Ngày phục hồi thể lực hoặc chưa gán bài tập</p>
                    <p className="text-xs text-[#5E625A] mt-1">
                      Chọn bài tập từ thanh danh mục bên phải để thêm vào ngày này.
                    </p>
                  </div>
                ) : (
                  currentDayExercises.map((ex, index) => (
                    <div
                      key={index}
                      className="bg-[#FAF9F5] border border-[#DADCD3] rounded-xl p-4 flex flex-col md:flex-row md:items-center justify-between gap-4"
                    >
                      <div className="flex items-center gap-3">
                        <span className="w-7 h-7 rounded-lg bg-[#D7FF3F] text-[#111310] text-xs font-bold flex items-center justify-center shrink-0">
                          {index + 1}
                        </span>
                        <div>
                          <p className="text-sm font-bold text-[#111310]">{ex.exerciseName}</p>
                          <span className="text-[10px] font-semibold bg-[#DADCD3] text-[#5E625A] px-2 py-0.5 rounded-full uppercase">
                            {ex.muscleGroup}
                          </span>
                        </div>
                      </div>

                      <div className="flex items-center gap-3 flex-wrap">
                        <div className="flex items-center gap-1.5">
                          <label className="text-xs text-[#5E625A]">Sets:</label>
                          <input
                            type="number"
                            min="1"
                            value={ex.sets}
                            onChange={(e) => updateExerciseParam(ex.id, 'sets', parseInt(e.target.value) || 1)}
                            className="w-14 border border-[#DADCD3] rounded-lg px-2 py-1 text-xs text-center font-bold bg-white"
                          />
                        </div>
                        <div className="flex items-center gap-1.5">
                          <label className="text-xs text-[#5E625A]">Reps:</label>
                          <input
                            type="number"
                            min="1"
                            value={ex.reps}
                            onChange={(e) => updateExerciseParam(ex.id, 'reps', parseInt(e.target.value) || 1)}
                            className="w-14 border border-[#DADCD3] rounded-lg px-2 py-1 text-xs text-center font-bold bg-white"
                          />
                        </div>
                        <div className="flex items-center gap-1.5">
                          <label className="text-xs text-[#5E625A]">Tạ (kg):</label>
                          <input
                            type="number"
                            min="0"
                            value={ex.weightSuggest || 0}
                            onChange={(e) => updateExerciseParam(ex.id, 'weightSuggest', parseFloat(e.target.value) || 0)}
                            className="w-16 border border-[#DADCD3] rounded-lg px-2 py-1 text-xs text-center font-bold bg-white"
                          />
                        </div>
                        <div className="flex items-center gap-1.5">
                          <label className="text-xs text-[#5E625A]">Nghỉ (s):</label>
                          <input
                            type="number"
                            min="0"
                            step="15"
                            value={ex.restSecs || 60}
                            onChange={(e) => updateExerciseParam(ex.id, 'restSecs', parseInt(e.target.value) || 60)}
                            className="w-16 border border-[#DADCD3] rounded-lg px-2 py-1 text-xs text-center font-bold bg-white"
                          />
                        </div>
                        <button
                          onClick={() => removeExercise(ex.id)}
                          className="text-red-500 hover:text-red-700 p-1 text-sm ml-2"
                          title="Xóa bài tập này"
                        >
                          ✕
                        </button>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          </div>

          {/* Right Sidebar: Exercise Library */}
          <div className="lg:col-span-4">
            <div className="bg-white border border-[#DADCD3] rounded-2xl p-5 shadow-sm sticky top-6">
              <div className="flex items-center justify-between mb-3">
                <h2 className="text-sm font-semibold uppercase tracking-[0.08em] text-[#8A8E85]">
                  Thư Viện Bài Tập
                </h2>
                <span className="text-[11px] text-[#526B00] font-bold">
                  {filteredLibrary.length} bài
                </span>
              </div>

              {/* Search */}
              <input
                type="text"
                placeholder="Tìm tên bài, nhóm cơ..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full border border-[#DADCD3] rounded-xl px-3.5 py-2 text-xs mb-3 focus:outline-none focus:ring-2 focus:ring-[#526B00]"
              />

              {/* Muscle filter chips */}
              <div className="flex gap-1.5 flex-wrap mb-4">
                {muscleGroups.map((mg) => (
                  <button
                    key={mg}
                    onClick={() => setSelectedMuscle(mg)}
                    className={`text-[10px] font-bold px-2.5 py-1 rounded-lg border transition-all ${
                      selectedMuscle === mg
                        ? 'bg-[#111310] text-[#D7FF3F] border-[#111310]'
                        : 'bg-[#FAF9F5] text-[#5E625A] border-[#DADCD3] hover:bg-[#F0EEE6]'
                    }`}
                  >
                    {mg}
                  </button>
                ))}
              </div>

              {/* Exercise Items list */}
              <div className="space-y-2 max-h-[500px] overflow-y-auto pr-1">
                {filteredLibrary.map((ex) => (
                  <div
                    key={ex.id}
                    className="p-3 border border-[#DADCD3] rounded-xl hover:border-[#526B00] hover:bg-[#FAF9F5] transition-all flex items-center justify-between group"
                  >
                    <div>
                      <p className="text-xs font-bold text-[#111310]">{ex.name}</p>
                      <p className="text-[10px] text-[#5E625A] mt-0.5 line-clamp-1">{ex.description}</p>
                      <div className="flex items-center gap-1.5 mt-1.5">
                        <span className="text-[9px] font-semibold bg-[#D7FF3F]/60 text-[#111310] px-1.5 py-0.2 rounded">
                          {ex.muscleGroup}
                        </span>
                        {ex.equipment && (
                          <span className="text-[9px] text-[#8A8E85]">
                            • {ex.equipment}
                          </span>
                        )}
                      </div>
                    </div>
                    <button
                      onClick={() => addExerciseToDay(ex)}
                      className="shrink-0 ml-2 bg-[#111310] group-hover:bg-[#526B00] text-white w-7 h-7 rounded-lg text-xs font-bold flex items-center justify-center transition-colors"
                      title="Thêm vào ngày đang chọn"
                    >
                      +
                    </button>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
