'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import api from '@/lib/api';
import Header from '@/components/Header';
import Footer from '@/components/Footer';
import type { ClassSessionDto, StudioClassDto } from '@/lib/contracts';

const DAY_LABELS = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'];

function startOfWeek(date: Date): Date {
  const clone = new Date(date);
  const day = clone.getDay();
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

/** Task #74 — lịch lớp công khai lấy từ /api/sessions + /api/classes. */
export default function ClassesPage() {
  const [weekStart] = useState(() => startOfWeek(new Date()));
  const [sessions, setSessions] = useState<ClassSessionDto[]>([]);
  const [types, setTypes] = useState<string[]>([]);
  const [activeDay, setActiveDay] = useState(0);
  const [activeType, setActiveType] = useState('ALL');
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const [sessionRes, classRes] = await Promise.all([
        api.get('/sessions', { params: { from: toIso(weekStart), to: toIso(addDays(weekStart, 6)) } }),
        api.get('/classes'),
      ]);
      setSessions(Array.isArray(sessionRes.data) ? sessionRes.data : []);
      const catalog: StudioClassDto[] = Array.isArray(classRes.data) ? classRes.data : [];
      setTypes(Array.from(new Set(catalog.map((item) => item.category))).sort());
    } catch (err) {
      console.error('Không tải được thời khoá biểu:', err);
      setSessions([]);
      setError('Không tải được thời khoá biểu. Vui lòng thử lại sau.');
    } finally {
      setIsLoading(false);
    }
  }, [weekStart]);

  useEffect(() => {
    load();
  }, [load]);

  const dayIso = toIso(addDays(weekStart, activeDay));

  const filtered = useMemo(
    () =>
      sessions.filter(
        (session) =>
          session.sessionDate === dayIso &&
          (activeType === 'ALL' || session.category === activeType),
      ),
    [sessions, dayIso, activeType],
  );

  const weekLabel = `${weekStart.toLocaleDateString('vi-VN')} – ${addDays(weekStart, 6).toLocaleDateString('vi-VN')}`;

  return (
    <>
      <Header />
      <main className="pt-16">
        <section className="bg-[#F5F5F0] py-16">
          <div className="max-w-[1280px] mx-auto px-10">
            <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[#526B00] mb-3">
              THỜI KHOÁ BIỂU
            </p>
            <h1 className="text-[52px] font-bold text-[#111310] mb-4">Tìm lớp của bạn</h1>
            <p className="text-lg text-[#5E625A] max-w-md">
              Tuần {weekLabel} — đặt chỗ trực tiếp trong ứng dụng hội viên.
            </p>
          </div>
        </section>

        <div className="bg-white sticky top-16 z-30 border-b border-[#DADCD3]">
          <div className="max-w-[1280px] mx-auto px-10 py-4">
            <div className="flex items-center gap-2 mb-3">
              {DAY_LABELS.map((label, index) => (
                <button
                  key={label}
                  type="button"
                  onClick={() => setActiveDay(index)}
                  className={`px-4 py-1.5 rounded-full text-xs font-semibold transition-colors ${
                    activeDay === index
                      ? 'bg-[#171A16] text-white'
                      : 'bg-[#F5F5F0] text-[#5E625A] hover:bg-[#DADCD3]'
                  }`}
                >
                  {label} {addDays(weekStart, index).getDate()}
                </button>
              ))}
            </div>
            <div className="flex items-center gap-2 flex-wrap">
              <button
                type="button"
                onClick={() => setActiveType('ALL')}
                className={`px-3 py-1 rounded-full text-xs font-medium transition-colors ${
                  activeType === 'ALL'
                    ? 'bg-[#D7FF3F] text-[#111310]'
                    : 'bg-[#F5F5F0] text-[#8A8E85] hover:bg-[#DADCD3]'
                }`}
              >
                Tất cả
              </button>
              {types.map((type) => (
                <button
                  key={type}
                  type="button"
                  onClick={() => setActiveType(type)}
                  className={`px-3 py-1 rounded-full text-xs font-medium transition-colors ${
                    activeType === type
                      ? 'bg-[#D7FF3F] text-[#111310]'
                      : 'bg-[#F5F5F0] text-[#8A8E85] hover:bg-[#DADCD3]'
                  }`}
                >
                  {type}
                </button>
              ))}
            </div>
          </div>
        </div>

        <section className="bg-white py-10">
          <div className="max-w-[1280px] mx-auto px-10">
            {error && (
              <p role="alert" className="mb-6 text-sm text-red-700 bg-red-50 border border-red-200 rounded-lg px-4 py-3">
                {error}
              </p>
            )}
            {isLoading ? (
              <div className="py-20 text-center">
                <div className="w-8 h-8 border-2 border-[#171A16] border-t-[#D7FF3F] rounded-full animate-spin mx-auto" />
              </div>
            ) : filtered.length === 0 ? (
              <div className="text-center py-20">
                <p className="text-lg font-bold text-[#111310] mb-2">Không có lớp trong ngày này</p>
                <p className="text-sm text-[#8A8E85]">Thử chọn ngày hoặc loại lớp khác.</p>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {filtered.map((session) => (
                  <div key={session.id} className="bg-white border border-[#DADCD3] rounded-xl p-5">
                    <div className="flex items-center justify-between mb-3">
                      <span className="text-[10px] font-bold uppercase tracking-wider text-[#526B00] bg-[#F5F5F0] px-2 py-1 rounded-full">
                        {session.category}
                      </span>
                      <span className="text-xs text-[#8A8E85]">
                        {session.startTime?.slice(0, 5)} · {session.room}
                      </span>
                    </div>
                    <h3 className="text-base font-bold text-[#111310] mb-1">{session.className}</h3>
                    <p className="text-sm text-[#8A8E85] mb-1">{session.trainerName || 'Chưa xếp HLV'}</p>
                    <p className="text-xs text-[#5E625A] mb-4">
                      {session.bookedCount}/{session.capacity} chỗ đã đặt
                    </p>
                    <div className="flex items-center justify-between">
                      <span
                        className={`text-xs font-medium ${
                          session.availableSpots <= 3 ? 'text-red-500' : 'text-[#5E625A]'
                        }`}
                      >
                        {session.availableSpots <= 0 ? 'Hết chỗ' : `Còn ${session.availableSpots} chỗ`}
                      </span>
                      <a
                        href="/register"
                        className="bg-[#D7FF3F] text-[#111310] font-semibold rounded-lg px-4 py-1.5 text-xs hover:bg-[#C4E82E] transition-colors"
                      >
                        Đặt chỗ
                      </a>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </section>

        <section className="bg-[#D7FF3F] py-6">
          <div className="max-w-[1280px] mx-auto px-10 text-center">
            <p className="text-sm font-bold text-[#111310]">
              🎉 Buổi đầu tiên miễn phí cho hội viên mới —{' '}
              <a href="/register" className="underline">
                Đăng ký ngay
              </a>
            </p>
          </div>
        </section>
      </main>
      <Footer />
    </>
  );
}
