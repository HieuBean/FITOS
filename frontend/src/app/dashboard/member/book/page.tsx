'use client';

import React, { useCallback, useEffect, useMemo, useState } from 'react';
import api from '@/lib/api';
import RoleGuard from '@/components/RoleGuard';
import BookConfirmModal from '@/components/BookConfirmModal';
import NoShowBadge from '@/components/NoShowBadge';
import { formatDate, type BookingDto, type ClassSessionDto } from '@/lib/contracts';

type Tab = 'CLASSES' | 'PT';

function addDays(date: Date, days: number): Date {
  const clone = new Date(date);
  clone.setDate(clone.getDate() + days);
  return clone;
}

function toIso(date: Date): string {
  const offset = date.getTimezoneOffset();
  return new Date(date.getTime() - offset * 60_000).toISOString().slice(0, 10);
}

/** Task #94 — hội viên tự đặt lớp: date strip + tabs Classes/PT + modal xác nhận. */
export default function BookClassPage() {
  const [tab, setTab] = useState<Tab>('CLASSES');
  const [dayOffset, setDayOffset] = useState(0);
  const [sessions, setSessions] = useState<ClassSessionDto[]>([]);
  const [myBookings, setMyBookings] = useState<BookingDto[]>([]);
  const [category, setCategory] = useState('ALL');
  const [selected, setSelected] = useState<ClassSessionDto | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const days = useMemo(
    () =>
      Array.from({ length: 7 }, (_, index) => {
        const date = addDays(new Date(), index);
        return {
          iso: toIso(date),
          day: date.toLocaleDateString('vi-VN', { weekday: 'short' }),
          dayNumber: date.getDate(),
          month: date.toLocaleDateString('vi-VN', { month: 'short' }),
          isToday: index === 0,
        };
      }),
    [],
  );

  const activeDay = days[Math.min(dayOffset, days.length - 1)];

  const load = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const [sessionRes, bookingRes] = await Promise.all([
        api.get('/sessions', { params: { from: activeDay.iso, to: activeDay.iso } }),
        api.get('/bookings/me'),
      ]);
      setSessions(Array.isArray(sessionRes.data) ? sessionRes.data : []);
      setMyBookings(Array.isArray(bookingRes.data) ? bookingRes.data : []);
    } catch (err) {
      console.error('Không tải được lịch lớp:', err);
      setSessions([]);
      setError('Không tải được lịch lớp. Vui lòng thử lại.');
    } finally {
      setIsLoading(false);
    }
  }, [activeDay.iso]);

  useEffect(() => {
    load();
  }, [load]);

  const categories = useMemo(
    () => ['ALL', ...Array.from(new Set(sessions.map((item) => item.category)))],
    [sessions],
  );

  const bookedSessionIds = useMemo(
    () =>
      new Set(
        myBookings
          .filter((booking) => booking.status !== 'CANCELLED')
          .map((booking) => booking.sessionId),
      ),
    [myBookings],
  );

  const filtered = useMemo(
    () =>
      sessions.filter((session) => category === 'ALL' || session.category === category),
    [sessions, category],
  );

  const confirmBooking = async (session: ClassSessionDto) => {
    await api.post('/bookings', { sessionId: session.id });
    setSelected(null);
    setNotice(`Đã đặt ${session.className} lúc ${session.startTime?.slice(0, 5)} — nhớ điểm danh đúng giờ.`);
    await load();
  };

  const cancelBooking = async (booking: BookingDto) => {
    try {
      await api.patch(`/bookings/${booking.id}/cancel`, { reason: 'Hội viên tự huỷ' });
      setNotice('Đã huỷ lượt đặt chỗ.');
      await load();
    } catch (err) {
      console.error('Huỷ thất bại:', err);
      setError('Không huỷ được lượt đặt chỗ.');
    }
  };

  const upcoming = myBookings.filter(
    (booking) => booking.status === 'CONFIRMED' || booking.status === 'PENDING',
  );

  return (
    <RoleGuard allowedRoles={['MEMBER', 'ADMIN']}>
      <div className="min-h-screen bg-[#F5F5F0]">
        <div className="bg-white border-b border-[#DADCD3] px-8 py-6">
          <div className="flex items-start justify-between gap-4">
            <div>
              <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[#526B00] mb-1">
                HỘI VIÊN · ĐẶT LỊCH
              </p>
              <h1 className="text-2xl font-bold text-[#111310]">Đặt lớp tập</h1>
              <p className="text-sm text-[#5E625A] mt-1">Chọn ngày, chọn lớp và xác nhận đặt chỗ</p>
            </div>
            <div className="text-right">
              <p className="text-[11px] uppercase tracking-[0.08em] text-[#8A8E85]">Lượt đặt sắp tới</p>
              <p className="text-2xl font-black text-[#111310]">{upcoming.length}</p>
            </div>
          </div>

          <div className="flex gap-1.5 mt-5">
            {(['CLASSES', 'PT'] as Tab[]).map((item) => (
              <button
                key={item}
                type="button"
                onClick={() => setTab(item)}
                className={`px-4 py-1.5 rounded-lg text-xs font-bold uppercase tracking-[0.05em] transition-colors ${
                  tab === item
                    ? 'bg-[#171A16] text-[#D7FF3F]'
                    : 'text-[#5E625A] hover:bg-[#F5F5F0]'
                }`}
              >
                {item === 'CLASSES' ? 'Lớp học' : 'PT 1-1'}
              </button>
            ))}
          </div>
        </div>

        <div className="p-8 space-y-6">
          {error && (
            <p role="alert" className="text-sm text-red-700 bg-red-50 border border-red-200 rounded-lg px-4 py-3">
              {error}
            </p>
          )}
          {notice && (
            <p className="text-sm text-emerald-800 bg-emerald-50 border border-emerald-200 rounded-lg px-4 py-3">
              {notice}
            </p>
          )}

          {tab === 'PT' ? (
            <div className="bg-white border border-[#DADCD3] rounded-xl p-8 text-center">
              <p className="text-3xl mb-3">🏋️</p>
              <p className="text-sm font-bold text-[#111310]">Buổi tập PT do huấn luyện viên xếp</p>
              <p className="text-xs text-[#8A8E85] mt-1 max-w-md mx-auto">
                Lịch PT 1-1 được lễ tân hoặc HLV tạo để đảm bảo không trùng ca dạy. Liên hệ quầy để đặt hoặc
                dời buổi tập; mọi thay đổi sẽ hiển thị trong mục Lịch sử thanh toán và Gói tập của bạn.
              </p>
            </div>
          ) : (
            <>
              {/* Date strip */}
              <div className="flex gap-2">
                {days.map((day, index) => (
                  <button
                    key={day.iso}
                    type="button"
                    onClick={() => setDayOffset(index)}
                    className={`flex-1 rounded-xl border py-3 text-center transition-colors ${
                      index === dayOffset
                        ? 'bg-[#171A16] border-[#171A16] text-[#ECEDE6]'
                        : 'bg-white border-[#DADCD3] text-[#5E625A] hover:border-[#526B00]'
                    }`}
                  >
                    <p className="text-[10px] font-semibold uppercase tracking-[0.08em]">{day.day}</p>
                    <p className="text-lg font-bold mt-0.5">{day.dayNumber}</p>
                    <p className="text-[10px] opacity-70">{day.month}</p>
                  </button>
                ))}
              </div>

              {/* Category chips */}
              <div className="flex flex-wrap gap-2">
                {categories.map((item) => (
                  <button
                    key={item}
                    type="button"
                    onClick={() => setCategory(item)}
                    className={`text-xs font-semibold px-4 py-1.5 rounded-full border transition-colors ${
                      category === item
                        ? 'bg-[#D7FF3F] border-[#D7FF3F] text-[#526B00]'
                        : 'bg-white border-[#DADCD3] text-[#5E625A] hover:border-[#526B00]'
                    }`}
                  >
                    {item === 'ALL' ? 'Tất cả' : item}
                  </button>
                ))}
              </div>

              {isLoading ? (
                <div className="p-12 text-center">
                  <div className="w-8 h-8 border-2 border-[#171A16] border-t-[#D7FF3F] rounded-full animate-spin mx-auto" />
                </div>
              ) : filtered.length === 0 ? (
                <div className="bg-white border border-[#DADCD3] rounded-xl p-12 text-center">
                  <p className="text-sm font-bold text-[#111310]">Ngày này chưa có lớp</p>
                  <p className="text-xs text-[#8A8E85] mt-1">Chọn ngày khác trong tuần.</p>
                </div>
              ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                  {filtered.map((session) => {
                    const alreadyBooked = bookedSessionIds.has(session.id);
                    return (
                      <div key={session.id} className="bg-white border border-[#DADCD3] rounded-xl p-5">
                        <div className="flex items-start justify-between mb-3">
                          <span className="text-[10px] font-semibold uppercase tracking-[0.08em] bg-[#F0F0EB] text-[#5E625A] px-2 py-0.5 rounded-full">
                            {session.category}
                          </span>
                          <span className="text-xs font-semibold text-[#526B00] bg-[#D7FF3F] px-2 py-0.5 rounded-full">
                            {session.startTime?.slice(0, 5)}
                          </span>
                        </div>
                        <p className="text-base font-bold text-[#111310] mb-1">{session.className}</p>
                        <p className="text-xs text-[#5E625A] mb-1">
                          với {session.trainerName || 'HLV của studio'}
                        </p>
                        <p className="text-[11px] text-[#8A8E85] mb-4">
                          {session.room} · {formatDate(session.sessionDate)}
                        </p>
                        <div className="flex items-center justify-between">
                          <span
                            className={`text-xs font-medium ${
                              session.availableSpots <= 3 ? 'text-orange-600' : 'text-[#5E625A]'
                            }`}
                          >
                            {session.availableSpots <= 0
                              ? 'Hết chỗ'
                              : `Còn ${session.availableSpots} chỗ`}
                          </span>
                          <button
                            type="button"
                            disabled={alreadyBooked || session.availableSpots <= 0}
                            onClick={() => setSelected(session)}
                            className="bg-[#171A16] text-[#ECEDE6] font-semibold rounded-lg px-3 py-1.5 text-xs hover:opacity-90 disabled:opacity-40"
                          >
                            {alreadyBooked ? 'Đã đặt' : 'Đặt chỗ'}
                          </button>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}

              {/* Lượt đặt sắp tới */}
              {upcoming.length > 0 && (
                <div className="bg-white border border-[#DADCD3] rounded-xl overflow-hidden">
                  <div className="px-5 py-3.5 border-b border-[#DADCD3]">
                    <h2 className="text-sm font-bold text-[#111310]">Lượt đặt sắp tới của bạn</h2>
                  </div>
                  <div className="divide-y divide-[#DADCD3]">
                    {upcoming.map((booking) => (
                      <div key={booking.id} className="flex items-center gap-3 px-5 py-3">
                        <div className="min-w-0 flex-1">
                          <p className="text-sm font-semibold text-[#111310] truncate">{booking.className}</p>
                          <p className="text-[11px] text-[#8A8E85]">
                            {formatDate(booking.sessionDate)} · {booking.startTime?.slice(0, 5)}
                          </p>
                        </div>
                        <NoShowBadge sessionId={booking.sessionId} compact />
                        <button
                          type="button"
                          onClick={() => cancelBooking(booking)}
                          className="text-[11px] font-semibold px-2.5 py-1.5 rounded-lg border border-[#DADCD3] text-red-600 hover:bg-red-50"
                        >
                          Huỷ
                        </button>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </>
          )}
        </div>

        {selected && (
          <BookConfirmModal
            session={selected}
            onConfirm={confirmBooking}
            onClose={() => setSelected(null)}
            noShowSlot={<NoShowBadge sessionId={selected.id} />}
          />
        )}
      </div>
    </RoleGuard>
  );
}
