'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import ExerciseDetailDrawer from '@/components/ExerciseDetailDrawer';
import {
  getExercises,
  startWorkoutSession,
  logWorkoutSet,
  completeWorkoutSession,
  Exercise,
  WorkoutLogItem
} from '@/lib/api';

export default function WorkoutLogPage() {
  const [exercises, setExercises] = useState<Exercise[]>([]);
  const [selectedMuscle, setSelectedMuscle] = useState('ALL');
  const [selectedExercise, setSelectedExercise] = useState<Exercise | null>(null);
  const [drawerExercise, setDrawerExercise] = useState<Exercise | null>(null);
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);

  // Active session state
  const [sessionId, setSessionId] = useState<number | null>(null);
  const [sessionActive, setSessionActive] = useState(false);
  const [secondsElapsed, setSecondsElapsed] = useState(0);

  // Set inputs
  const [setNumber, setSetNumber] = useState(1);
  const [reps, setReps] = useState(10);
  const [weightKg, setWeightKg] = useState(60);
  const [rpe, setRpe] = useState(8);

  // Logged sets in current session
  const [loggedSets, setLoggedSets] = useState<WorkoutLogItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [toast, setToast] = useState<string | null>(null);

  useEffect(() => {
    getExercises().then((list) => {
      setExercises(list);
      if (list.length > 0) setSelectedExercise(list[0]);
    });
  }, []);

  // Timer tick
  useEffect(() => {
    let interval: any = null;
    if (sessionActive) {
      interval = setInterval(() => {
        setSecondsElapsed((prev) => prev + 1);
      }, 1000);
    } else {
      clearInterval(interval);
    }
    return () => clearInterval(interval);
  }, [sessionActive]);

  function showToast(msg: string) {
    setToast(msg);
    setTimeout(() => setToast(null), 3000);
  }

  function formatTime(totalSecs: number) {
    const mins = Math.floor(totalSecs / 60);
    const secs = totalSecs % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  }

  async function handleStartSession() {
    setLoading(true);
    try {
      const session = await startWorkoutSession(1, 1);
      setSessionId(session.id || 101);
      setSessionActive(true);
      setSecondsElapsed(0);
      setLoggedSets([]);
      showToast('🔥 Buổi tập đã bắt đầu! Chúc bạn tập luyện sung mãn!');
    } catch {
      setSessionId(Date.now());
      setSessionActive(true);
      setSecondsElapsed(0);
      setLoggedSets([]);
      showToast('🔥 Bắt đầu buổi tập (Offline mode)!');
    } finally {
      setLoading(false);
    }
  }

  async function handleLogSet() {
    if (!selectedExercise) return;
    setLoading(true);
    const currentSessionId = sessionId || 101;

    try {
      await logWorkoutSet(currentSessionId, selectedExercise.id, setNumber, reps, weightKg, rpe);
      const newLogItem: WorkoutLogItem = {
        id: Date.now(),
        exerciseId: selectedExercise.id,
        exerciseName: selectedExercise.name,
        muscleGroup: selectedExercise.muscleGroup,
        setNumber,
        repsActual: reps,
        weightKg,
        rpe,
        loggedAt: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      };
      setLoggedSets((prev) => [newLogItem, ...prev]);
      setSetNumber((prev) => prev + 1);
      showToast(`✅ Đã ghi nhận Set #${setNumber}: ${selectedExercise.name} (${weightKg}kg x ${reps} reps)`);
    } catch {
      const fallbackItem: WorkoutLogItem = {
        id: Date.now(),
        exerciseId: selectedExercise.id,
        exerciseName: selectedExercise.name,
        muscleGroup: selectedExercise.muscleGroup,
        setNumber,
        repsActual: reps,
        weightKg,
        rpe,
        loggedAt: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      };
      setLoggedSets((prev) => [fallbackItem, ...prev]);
      setSetNumber((prev) => prev + 1);
      showToast(`✅ Đã lưu Set #${setNumber} (Demo)`);
    } finally {
      setLoading(false);
    }
  }

  async function handleCompleteSession() {
    if (!sessionId) return;
    setLoading(true);
    try {
      const burnedEst = Math.round((secondsElapsed / 60) * 8.5);
      await completeWorkoutSession(sessionId, burnedEst, 'Hoàn thành buổi tập xuất sắc');
      setSessionActive(false);
      showToast(`🏆 Chúc mừng! Buổi tập đã hoàn tất trong ${formatTime(secondsElapsed)} (~${burnedEst} kcal)!`);
    } catch {
      setSessionActive(false);
      showToast(`🏆 Hoàn tất buổi tập (${formatTime(secondsElapsed)})!`);
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
      <div className="bg-white border-b border-[#DADCD3] px-8 py-6">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <Link href="/dashboard/member/progress" className="text-xs text-[#526B00] font-semibold hover:underline">
                &larr; Xem bảng tiến độ &amp; PRs
              </Link>
              <span className="text-gray-300">/</span>
              <span className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[#8A8E85]">
                WORKOUT LOG
              </span>
            </div>
            <h1 className="text-2xl font-bold text-[#111310]">Ghi Nhận Buổi Tập &amp; Sets/Reps</h1>
          </div>

          {/* Session Timer & Action */}
          <div className="flex items-center gap-3">
            {sessionActive ? (
              <>
                <div className="bg-[#111310] text-[#D7FF3F] px-4 py-2 rounded-xl text-sm font-black flex items-center gap-2 shadow-sm">
                  <span className="w-2.5 h-2.5 rounded-full bg-red-500 animate-ping" />
                  <span>{formatTime(secondsElapsed)}</span>
                </div>
                <button
                  onClick={handleCompleteSession}
                  disabled={loading}
                  className="bg-[#D7FF3F] text-[#111310] font-bold rounded-xl px-4 py-2 text-sm hover:opacity-90 transition-opacity shadow-sm"
                >
                  🏁 Hoàn thành buổi tập
                </button>
              </>
            ) : (
              <button
                onClick={handleStartSession}
                disabled={loading}
                className="bg-[#111310] text-[#D7FF3F] font-bold rounded-xl px-5 py-2.5 text-sm hover:bg-black transition-colors shadow-sm flex items-center gap-2"
              >
                <span>🚀</span> Bắt đầu buổi tập mới
              </button>
            )}
          </div>
        </div>
      </div>

      <div className="p-8">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
          {/* Left Panel: Exercise Selector & Set Logger Form */}
          <div className="lg:col-span-7 space-y-6">
            {/* Set Logger Card */}
            <div className="bg-white border border-[#DADCD3] rounded-2xl p-6 shadow-sm">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-sm font-bold uppercase tracking-[0.08em] text-[#8A8E85]">
                  Thông Số Set Đang Thực Hiện
                </h2>
                {selectedExercise && (
                  <button
                    onClick={() => {
                      setDrawerExercise(selectedExercise);
                      setIsDrawerOpen(true);
                    }}
                    className="text-xs font-bold text-[#526B00] bg-[#D7FF3F]/40 hover:bg-[#D7FF3F] px-3 py-1 rounded-full transition-colors flex items-center gap-1"
                  >
                    <span>🎥</span> Xem video &amp; Form chuẩn
                  </button>
                )}
              </div>

              {/* Exercise Select */}
              <div className="mb-5">
                <div className="flex items-center justify-between mb-1.5">
                  <label className="text-xs font-bold text-[#5E625A]">Chọn bài tập:</label>
                  <div className="flex gap-1 overflow-x-auto max-w-[65%]">
                    {['ALL', 'CHEST', 'BACK', 'LEGS', 'SHOULDERS', 'ARMS', 'CORE'].map((mg) => (
                      <button
                        key={mg}
                        type="button"
                        onClick={() => setSelectedMuscle(mg)}
                        className={`text-[9px] font-bold px-2 py-0.5 rounded transition-colors ${
                          selectedMuscle === mg
                            ? 'bg-[#111310] text-[#D7FF3F]'
                            : 'bg-gray-100 text-[#5E625A] hover:bg-gray-200'
                        }`}
                      >
                        {mg}
                      </button>
                    ))}
                  </div>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 max-h-48 overflow-y-auto p-1 border border-[#DADCD3] rounded-xl bg-[#FAF9F5]">
                  {exercises
                    .filter((ex) => selectedMuscle === 'ALL' || ex.muscleGroup.toUpperCase() === selectedMuscle)
                    .map((ex) => {
                    const isSelected = selectedExercise?.id === ex.id;
                    return (
                      <button
                        key={ex.id}
                        onClick={() => {
                          setSelectedExercise(ex);
                          setSetNumber(1);
                        }}
                        className={`text-left p-2.5 rounded-lg border text-xs font-semibold transition-all flex items-center justify-between ${
                          isSelected
                            ? 'bg-[#111310] text-[#D7FF3F] border-[#111310]'
                            : 'bg-white text-[#111310] border-[#DADCD3] hover:bg-gray-50'
                        }`}
                      >
                        <span className="truncate">{ex.name}</span>
                        <span className={`text-[9px] px-1.5 py-0.2 rounded uppercase ${
                          isSelected ? 'bg-[#D7FF3F] text-[#111310]' : 'bg-[#DADCD3] text-[#5E625A]'
                        }`}>
                          {ex.muscleGroup}
                        </span>
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Set Inputs */}
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-6">
                <div>
                  <label className="block text-xs font-bold text-[#5E625A] mb-1">Hiệp (Set #)</label>
                  <input
                    type="number"
                    min="1"
                    value={setNumber}
                    onChange={(e) => setSetNumber(parseInt(e.target.value) || 1)}
                    className="w-full border border-[#DADCD3] rounded-xl px-3 py-2 text-center text-sm font-black text-[#111310] bg-[#FAF9F5]"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-[#5E625A] mb-1">Số Reps</label>
                  <input
                    type="number"
                    min="1"
                    value={reps}
                    onChange={(e) => setReps(parseInt(e.target.value) || 1)}
                    className="w-full border border-[#DADCD3] rounded-xl px-3 py-2 text-center text-sm font-black text-[#111310] bg-[#FAF9F5]"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-[#5E625A] mb-1">Mức tạ (kg)</label>
                  <input
                    type="number"
                    min="0"
                    step="2.5"
                    value={weightKg}
                    onChange={(e) => setWeightKg(parseFloat(e.target.value) || 0)}
                    className="w-full border border-[#DADCD3] rounded-xl px-3 py-2 text-center text-sm font-black text-[#111310] bg-[#FAF9F5]"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-[#5E625A] mb-1">RPE (1-10)</label>
                  <input
                    type="number"
                    min="1"
                    max="10"
                    value={rpe}
                    onChange={(e) => setRpe(parseInt(e.target.value) || 8)}
                    className="w-full border border-[#DADCD3] rounded-xl px-3 py-2 text-center text-sm font-black text-[#111310] bg-[#FAF9F5]"
                  />
                </div>
              </div>

              {/* Action Button */}
              <button
                onClick={handleLogSet}
                disabled={loading || !selectedExercise}
                className="w-full bg-[#111310] hover:bg-black text-[#D7FF3F] text-sm font-bold py-3 rounded-xl transition-colors shadow-sm flex items-center justify-center gap-2"
              >
                <span>➕</span> Ghi nhận Set #{setNumber} cho bài {selectedExercise?.name}
              </button>
            </div>
          </div>

          {/* Right Panel: Live Logged Sets Feed */}
          <div className="lg:col-span-5">
            <div className="bg-white border border-[#DADCD3] rounded-2xl p-6 shadow-sm">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-sm font-bold uppercase tracking-[0.08em] text-[#8A8E85]">
                  Nhật Ký Các Set Đã Tập ({loggedSets.length})
                </h2>
                {sessionActive && (
                  <span className="text-[10px] font-bold text-[#526B00] bg-[#D7FF3F] px-2 py-0.5 rounded-full uppercase">
                    Live Session
                  </span>
                )}
              </div>

              {loggedSets.length === 0 ? (
                <div className="text-center py-12 border-2 border-dashed border-[#DADCD3] rounded-xl bg-[#FAF9F5]">
                  <p className="text-3xl mb-1">📋</p>
                  <p className="text-sm font-bold text-[#111310]">Chưa có set nào được ghi</p>
                  <p className="text-xs text-[#5E625A] mt-1">
                    Nhập mức tạ, reps và bấm "Ghi nhận Set" sau mỗi hiệp tập xong.
                  </p>
                </div>
              ) : (
                <div className="space-y-2.5 max-h-[500px] overflow-y-auto pr-1">
                  {loggedSets.map((log, index) => (
                    <div
                      key={log.id || index}
                      className="p-3.5 bg-[#FAF9F5] border border-[#DADCD3] rounded-xl flex items-center justify-between"
                    >
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="w-5 h-5 rounded-md bg-[#111310] text-[#D7FF3F] text-[10px] font-black flex items-center justify-center">
                            #{log.setNumber}
                          </span>
                          <span className="text-sm font-bold text-[#111310]">{log.exerciseName}</span>
                        </div>
                        <p className="text-xs text-[#5E625A] mt-1">
                          {log.weightKg ? `${log.weightKg} kg` : 'Bodyweight'} × {log.repsActual} reps · RPE {log.rpe || 8}
                        </p>
                      </div>
                      <span className="text-[11px] font-semibold text-[#8A8E85]">{log.loggedAt}</span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Slide-in technical exercise video drawer */}
      <ExerciseDetailDrawer
        exercise={drawerExercise}
        isOpen={isDrawerOpen}
        onClose={() => setIsDrawerOpen(false)}
      />
    </div>
  );
}

