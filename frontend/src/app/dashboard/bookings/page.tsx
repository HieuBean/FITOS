'use client';

import React, { useCallback, useEffect, useMemo, useState } from 'react';
import api from '@/lib/api';
import RoleGuard from '@/components/RoleGuard';
import NoShowBadge from '@/components/NoShowBadge';
import { extractPageContent, formatDate, type BookingDto } from '@/lib/contracts';

const statusStyles: Record<string, string> = {
  CONFIRMED: 'bg-emerald-100 text-emerald-800 border-emerald-200',
  PENDING: 'bg-amber-100 text-amber-800 border-amber-200',
  CANCELLED: 'bg-zinc-100 text-zinc-600 border-zinc-300',
  COMPLETED: 'bg-blue-100 text-blue-800 border-blue-200',
  NO_SHOW: 'bg-red-100 text-red-700 border-red-200',
};

const statusLabels: Record<string, string> = {
  CONFIRMED: 'Đã xác nhận',
  PENDING: 'Chờ duyệt',
  CANCELLED: 'Đã huỷ',
  COMPLETED: 'Hoàn thành',
  NO_SHOW: 'Không đến',
};

/** Task #90 — danh sách đặt chỗ thật + điểm danh nhanh tại quầy. */
export default function BookingsPage() {
  const [bookings, setBookings] = useState<BookingDto[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [status, setStatus] = useState<string>('ALL');
  const [busyId, setBusyId] = useState<number | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const load = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const params: Record<string, unknown> = { size: 100 };
      if (status !== 'ALL') params.status = status;
      const { data } = await api.get('/bookings', { params });
      setBookings(extractPageContent<BookingDto>(data));
    } catch (err) {
      console.error('Không tải được danh sách đặt chỗ:', err);
      setBookings([]);
      setError('Không tải được danh sách đặt chỗ.');
    } finally {
      setIsLoading(false);
    }
  }, [status]);

  useEffect(() => {
    load();
  }, [load]);

  const stats = useMemo(
    () => ({
      total: bookings.length,
      checkedIn: bookings.filter((booking) => booking.checkedIn).length,
      noShow: bookings.filter((booking) => booking.status === 'NO_SHOW').length,
      cancelled: bookings.filter((booking) => booking.status === 'CANCELLED').length,
    }),
    [bookings],
  );

  const checkIn = async (booking: BookingDto) => {
    setBusyId(booking.id);
    setNotice(null);
    try {
      await api.post('/checkin', { bookingId: booking.id, method: 'DESK' });
      setNotice(`Đã điểm danh ${booking.customerName}.`);
      await load();
    } catch (err: unknown) {
      const message =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        'Điểm danh thất bại.';
      setError(message);
    } finally {
      setBusyId(null);
    }
  };

  const cancel = async (booking: BookingDto) => {
    setBusyId(booking.id);
    try {
      await api.patch(`/bookings/${booking.id}/cancel`, { reason: 'Huỷ tại quầy' });
      await load();
    } catch (err) {
      console.error('Huỷ đặt chỗ thất bại:', err);
      setError('Không huỷ được lượt đặt chỗ này.');
    } finally {
      setBusyId(null);
    }
  };

  const markNoShow = async (booking: BookingDto) => {
    setBusyId(booking.id);
    try {
      await api.patch(`/bookings/${booking.id}/no-show`);
      await load();
    } catch (err) {
      console.error('Đánh dấu no-show thất bại:', err);
      setError('Không đánh dấu được no-show.');
    } finally {
      setBusyId(null);
    }
  };

  return (
    <RoleGuard allowedRoles={['ADMIN', 'MANAGER', 'RECEPTIONIST', 'TRAINER']}>
      <div className="min-h-screen bg-[#F5F5F0]">
        <div className="bg-white border-b border-[#DADCD3] px-8 py-6">
          <div className="flex items-start justify-between gap-4">
            <div>
              <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[#526B00] mb-1">
                VẬN HÀNH · ĐẶT CHỖ
              </p>
              <h1 className="text-2xl font-bold text-[#111310]">Danh sách đặt chỗ</h1>
              <p className="text-sm text-[#5E625A] mt-1">Điểm danh tại quầy và theo dõi tỉ lệ no-show</p>
            </div>
            <button
              type="button"
              onClick={load}
              className="border border-[#DADCD3] bg-white text-[#111310] font-semibold rounded-lg px-4 py-2 text-sm hover:bg-[#F5F5F0]"
            >
              Làm mới
            </button>
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

          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {[
              { label: 'Tổng lượt đặt', value: stats.total, hint: 'Trong bộ lọc hiện tại' },
              { label: 'Đã điểm danh', value: stats.checkedIn, hint: 'Có mặt tại studio' },
              { label: 'Không đến', value: stats.noShow, hint: 'Cần nhắc lịch' },
              { label: 'Đã huỷ', value: stats.cancelled, hint: 'Đã giải phóng chỗ' },
            ].map((item) => (
              <div key={item.label} className="bg-white border border-[#DADCD3] rounded-xl p-5 shadow-sm">
                <p className="text-[11px] font-semibold uppercase tracking-[0.08em] text-[#8A8E85] mb-1.5">
                  {item.label}
                </p>
                <p className="text-3xl font-black text-[#111310]">{item.value}</p>
                <p className="text-xs text-[#5E625A] mt-1">{item.hint}</p>
              </div>
            ))}
          </div>

          <div className="bg-white border border-[#DADCD3] rounded-xl p-4 flex items-center gap-1.5 overflow-x-auto shadow-sm">
            {['ALL', 'CONFIRMED', 'COMPLETED', 'NO_SHOW', 'CANCELLED'].map((option) => (
              <button
                key={option}
                type="button"
                onClick={() => setStatus(option)}
                className={`px-3 py-1.5 rounded-lg text-xs font-bold uppercase tracking-[0.05em] transition-all whitespace-nowrap ${
                  status === option
                    ? 'bg-[#171A16] text-[#D7FF3F] shadow-sm'
                    : 'text-[#5E625A] hover:bg-[#F5F5F0] hover:text-[#111310]'
                }`}
              >
                {option === 'ALL' ? 'Tất cả' : statusLabels[option] ?? option}
              </button>
            ))}
          </div>

          <div className="bg-white border border-[#DADCD3] rounded-xl overflow-hidden shadow-sm">
            <div className="grid grid-cols-[1.6fr_1.6fr_1.2fr_1fr_1.4fr] px-6 py-3.5 bg-[#F5F5F0] border-b border-[#DADCD3] text-[11px] font-bold uppercase tracking-[0.08em] text-[#8A8E85]">
              <span>Hội viên</span>
              <span>Lớp &amp; khung giờ</span>
              <span>Ngày tập</span>
              <span>Trạng thái</span>
              <span className="text-right">Thao tác</span>
            </div>

            {isLoading ? (
              <div className="p-12 text-center">
                <div className="w-8 h-8 border-2 border-[#171A16] border-t-[#D7FF3F] rounded-full animate-spin mx-auto" />
              </div>
            ) : bookings.length === 0 ? (
              <div className="p-12 text-center">
                <p className="text-sm font-bold text-[#111310]">Chưa có lượt đặt chỗ nào</p>
                <p className="text-xs text-[#8A8E85] mt-1">Hội viên đặt lớp sẽ xuất hiện ở đây.</p>
              </div>
            ) : (
              <div className="divide-y divide-[#DADCD3]">
                {bookings.map((booking) => (
                  <div
                    key={booking.id}
                    className="grid grid-cols-[1.6fr_1.6fr_1.2fr_1fr_1.4fr] px-6 py-4 items-center hover:bg-[#F9F9F7]"
                  >
                    <div className="min-w-0">
                      <p className="text-sm font-semibold text-[#111310] truncate">{booking.customerName}</p>
                      <p className="text-[11px] text-[#8A8E85]">
                        {booking.checkedIn ? 'Đã điểm danh' : 'Chưa điểm danh'}
                      </p>
                    </div>
                    <div className="min-w-0">
                      <p className="text-sm text-[#111310] truncate">{booking.className}</p>
                      <p className="text-[11px] text-[#8A8E85]">{booking.startTime?.slice(0, 5)}</p>
                    </div>
                    <div className="flex flex-col gap-1">
                      <span className="text-xs text-[#5E625A]">{formatDate(booking.sessionDate)}</span>
                      {booking.status === 'CONFIRMED' && !booking.checkedIn && (
                        <NoShowBadge
                          sessionId={booking.sessionId}
                          customerId={booking.customerId}
                          compact
                        />
                      )}
                    </div>
                    <span
                      className={`justify-self-start text-[11px] font-semibold px-2.5 py-1 rounded-full border ${
                        statusStyles[booking.status] || 'bg-gray-100 text-gray-700'
                      }`}
                    >
                      {statusLabels[booking.status] || booking.status}
                    </span>
                    <div className="flex justify-end gap-1.5">
                      {!booking.checkedIn && booking.status === 'CONFIRMED' && (
                        <button
                          type="button"
                          onClick={() => checkIn(booking)}
                          disabled={busyId === booking.id}
                          className="text-[11px] font-bold px-2.5 py-1.5 rounded-lg bg-[#D7FF3F] text-[#111310] hover:bg-[#C4E82E] disabled:opacity-50"
                        >
                          Điểm danh
                        </button>
                      )}
                      {booking.status === 'CONFIRMED' && (
                        <>
                          <button
                            type="button"
                            onClick={() => markNoShow(booking)}
                            disabled={busyId === booking.id}
                            className="text-[11px] font-semibold px-2.5 py-1.5 rounded-lg border border-[#DADCD3] text-[#5E625A] hover:bg-[#F5F5F0] disabled:opacity-50"
                          >
                            No-show
                          </button>
                          <button
                            type="button"
                            onClick={() => cancel(booking)}
                            disabled={busyId === booking.id}
                            className="text-[11px] font-semibold px-2.5 py-1.5 rounded-lg border border-[#DADCD3] text-red-600 hover:bg-red-50 disabled:opacity-50"
                          >
                            Huỷ
                          </button>
                        </>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </RoleGuard>
  );
}
