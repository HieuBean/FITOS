'use client';

import React, { useCallback, useEffect, useMemo, useState } from 'react';
import api from '@/lib/api';
import RoleGuard from '@/components/RoleGuard';
import type { ClassSessionDto, ScheduleSuggestionDto } from '@/lib/contracts';

const DAY_LABELS = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'];

function startOfWeek(date: Date): Date {
  const clone = new Date(date);
  const day = clone.getDay(); // 0 = Chủ nhật
  const diff = day === 0 ? -6 : 1 - day;
  clone.setDate(clone.getDate() + diff);
  clone.setHours(0, 0, 0, 0);
  return clone;
}

function addDays(date: Date, days: number): Date {
  const clone = new Date(date);
  clone.setDate(clone.getDate() + days);
  return clone;
}

function toIso(date: Date): string {
  return date.toISOString().slice(0, 10);
}

/** Task #89 — lịch tuần lấy dữ liệu thật từ /api/sessions + banner gợi ý AI (#95). */
export default function SchedulePage() {
  const [weekStart, setWeekStart] = useState<Date>(() => startOfWeek(new Date()));
  const [sessions, setSessions] = useState<ClassSessionDto[]>([]);
  const [suggestion, setSuggestion] = useState<ScheduleSuggestionDto | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showSuggestion, setShowSuggestion] = useState(true);

  const weekDays = useMemo(
    () => Array.from({ length: 7 }, (_, index) => addDays(weekStart, index)),
    [weekStart],
  );

  const load = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const { data } = await api.get('/sessions', {
        params: { from: toIso(weekStart), to: toIso(addDays(weekStart, 6)) },
      });
      setSessions(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('Không tải được lịch tuần:', err);
      setSessions([]);
      setError('Không tải được lịch tuần từ máy chủ.');
    } finally {
      setIsLoading(false);
    }
  }, [weekStart]);

  const loadSuggestion = useCallback(async () => {
    try {
      const { data } = await api.get<ScheduleSuggestionDto>('/ai/schedule-suggestions', {
        params: { date: toIso(new Date()) },
      });
      setSuggestion(data);
    } catch (err) {
      // Banner là tính năng gợi ý — lỗi không được chặn trang
      console.warn('Không tải được gợi ý lịch:', err);
      setSuggestion(null);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    loadSuggestion();
  }, [loadSuggestion]);

  const todayIso = toIso(new Date());

  const topSuggestion = suggestion?.suggestions?.[0];

  return (
    <RoleGuard allowedRoles={['ADMIN', 'MANAGER', 'RECEPTIONIST', 'TRAINER']}>
      <div className="min-h-screen bg-[#F5F5F0]">
        <div className="bg-white border-b border-[#DADCD3] px-8 py-6">
          <div className="flex items-start justify-between gap-4">
            <div>
              <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[#526B00] mb-1">
                VẬN HÀNH · LỊCH BIỂU
              </p>
              <h1 className="text-2xl font-bold text-[#111310]">Lịch phòng tập</h1>
              <p className="text-sm text-[#5E625A] mt-1">
                Tuần {weekStart.toLocaleDateString('vi-VN')} – {addDays(weekStart, 6).toLocaleDateString('vi-VN')}
              </p>
            </div>
            <div className="text-right">
              <p className="text-[11px] uppercase tracking-[0.08em] text-[#8A8E85]">Tổng buổi trong tuần</p>
              <p className="text-2xl font-black text-[#111310]">{sessions.length}</p>
            </div>
          </div>
        </div>

        <div className="p-8">
          {error && (
            <p role="alert" className="mb-4 text-sm text-red-700 bg-red-50 border border-red-200 rounded-lg px-4 py-3">
              {error}
            </p>
          )}

          {/* Task #95 — banner gợi ý AI cho hôm nay */}
          {showSuggestion && topSuggestion && (
            <div className="mb-6 bg-[#171A16] rounded-xl px-5 py-4 flex items-start gap-4">
              <div className="w-9 h-9 rounded-lg bg-[#D7FF3F] flex items-center justify-center flex-shrink-0">
                <span className="text-sm font-black text-[#111310]">AI</span>
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-xs font-bold uppercase tracking-[0.08em] text-[#D7FF3F]">
                  Gợi ý lấp lịch hôm nay
                </p>
                <p className="text-sm text-[#ECEDE6] mt-1">
                  <span className="font-semibold">
                    {topSuggestion.slot} · {topSuggestion.className}
                  </span>{' '}
                  — {topSuggestion.reason}
                </p>
                <p className="text-[10px] text-[#8A8E85] mt-1">
                  Nguồn: {suggestion?.source === 'ai-operations' ? 'ai-operations' : 'rule-based (fallback)'}
                </p>
              </div>
              <button
                type="button"
                onClick={() => setShowSuggestion(false)}
                className="text-[#8A8E85] hover:text-white text-sm"
                aria-label="Đóng gợi ý"
              >
                ✕
              </button>
            </div>
          )}

          <div className="flex items-center justify-center gap-4 mb-6">
            <button
              type="button"
              onClick={() => setWeekStart(addDays(weekStart, -7))}
              className="w-8 h-8 rounded-lg flex items-center justify-center border border-[#DADCD3] bg-white text-[#5E625A] text-base font-bold hover:bg-[#F5F5F0]"
            >
              ‹
            </button>
            <button
              type="button"
              onClick={() => setWeekStart(startOfWeek(new Date()))}
              className="text-xs font-semibold text-[#526B00] hover:underline"
            >
              Tuần này
            </button>
            <button
              type="button"
              onClick={() => setWeekStart(addDays(weekStart, 7))}
              className="w-8 h-8 rounded-lg flex items-center justify-center border border-[#DADCD3] bg-white text-[#5E625A] text-base font-bold hover:bg-[#F5F5F0]"
            >
              ›
            </button>
          </div>

          {isLoading ? (
            <div className="p-12 text-center">
              <div className="w-8 h-8 border-2 border-[#171A16] border-t-[#D7FF3F] rounded-full animate-spin mx-auto" />
            </div>
          ) : (
            <div className="grid grid-cols-7 gap-3">
              {weekDays.map((day, index) => {
                const iso = toIso(day);
                const isToday = iso === todayIso;
                const daySessions = sessions.filter((session) => session.sessionDate === iso);
                return (
                  <div key={iso}>
                    <div className={`text-center py-2 rounded-lg mb-3 ${isToday ? 'bg-[#D7FF3F]' : 'bg-transparent'}`}>
                      <p
                        className={`text-xs font-semibold uppercase tracking-[0.08em] ${
                          isToday ? 'text-[#526B00]' : 'text-[#8A8E85]'
                        }`}
                      >
                        {DAY_LABELS[index]}
                      </p>
                      <p className={`text-lg font-bold ${isToday ? 'text-[#526B00]' : 'text-[#111310]'}`}>
                        {day.getDate()}
                      </p>
                    </div>

                    {daySessions.map((session) => (
                      <div
                        key={session.id}
                        className="bg-white border border-[#DADCD3] rounded-lg p-2.5 mb-2 text-xs border-l-4"
                        style={{ borderLeftColor: session.colorCode || '#D7FF3F' }}
                      >
                        <p className="font-semibold text-[#111310] truncate">{session.className}</p>
                        <p className="text-[#8A8E85] mt-0.5">{session.startTime?.slice(0, 5)}</p>
                        <p className="text-[#5E625A] truncate">{session.trainerName || 'Chưa xếp HLV'}</p>
                        <p className="text-[#526B00] font-semibold mt-1">
                          {session.bookedCount}/{session.capacity}
                        </p>
                      </div>
                    ))}

                    {daySessions.length === 0 && (
                      <div className="h-10 flex items-center justify-center">
                        <span className="text-xs text-[#DADCD3]">—</span>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </RoleGuard>
  );
}
