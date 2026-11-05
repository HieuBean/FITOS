'use client';

import React, { useState } from 'react';
import type { ClassSessionDto } from '@/lib/contracts';

interface Props {
  session: ClassSessionDto;
  onConfirm: (session: ClassSessionDto) => Promise<void>;
  onClose: () => void;
  /** Hiển thị thêm dự đoán no-show nếu được truyền vào. */
  noShowSlot?: React.ReactNode;
}

/**
 * Task #95 — modal xác nhận đặt chỗ. Hiển thị lại thông tin buổi học (lớp, HLV,
 * giờ, số chỗ còn) và chỉ gọi API khi người dùng bấm xác nhận.
 */
export default function BookConfirmModal({ session, onConfirm, onClose, noShowSlot }: Props) {
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const confirm = async () => {
    setSubmitting(true);
    setError(null);
    try {
      await onConfirm(session);
    } catch (err: unknown) {
      const message =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        'Không đặt được chỗ. Vui lòng thử lại.';
      setError(message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#111310]/50 p-4">
      <div
        className="w-full max-w-md bg-white rounded-2xl shadow-xl overflow-hidden"
        role="dialog"
        aria-modal="true"
        aria-labelledby="book-confirm-title"
      >
        <div className="px-6 py-4 border-b border-[#DADCD3] flex items-start justify-between gap-3">
          <div>
            <h2 id="book-confirm-title" className="text-base font-bold text-[#111310]">
              Xác nhận đặt chỗ
            </h2>
            <p className="text-xs text-[#8A8E85] mt-0.5">Kiểm tra lại thông tin trước khi đặt</p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="text-[#8A8E85] hover:text-[#111310] text-sm"
            aria-label="Đóng"
          >
            ✕
          </button>
        </div>

        <div className="px-6 py-5 space-y-3">
          <div className="flex items-center gap-2">
            <span
              className="h-2.5 w-2.5 rounded-full flex-shrink-0"
              style={{ backgroundColor: session.colorCode || '#D7FF3F' }}
              aria-hidden="true"
            />
            <p className="text-lg font-bold text-[#111310]">{session.className}</p>
          </div>

          <dl className="grid grid-cols-2 gap-3 text-sm">
            <Item label="Khung giờ" value={`${session.startTime?.slice(0, 5)} – ${session.endTime?.slice(0, 5)}`} />
            <Item label="Phòng" value={session.room || 'Studio 1'} />
            <Item label="Huấn luyện viên" value={session.trainerName || 'Chưa xếp HLV'} />
            <Item label="Ngày" value={session.sessionDate} />
          </dl>

          <div className="rounded-lg bg-[#F5F5F0] border border-[#DADCD3] px-4 py-3">
            <div className="flex items-center justify-between text-sm">
              <span className="text-[#5E625A]">Chỗ còn trống</span>
              <span
                className={`font-bold ${session.availableSpots <= 3 ? 'text-red-600' : 'text-[#111310]'}`}
              >
                {session.availableSpots}/{session.capacity}
              </span>
            </div>
          </div>

          {noShowSlot}

          {error && (
            <p role="alert" className="text-xs text-red-700 bg-red-50 border border-red-200 rounded-lg px-3 py-2">
              {error}
            </p>
          )}

          {session.availableSpots <= 0 && (
            <p className="text-xs text-red-700">Lớp đã hết chỗ — không thể đặt.</p>
          )}
        </div>

        <div className="px-6 py-4 border-t border-[#DADCD3] flex justify-end gap-2">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 text-sm font-semibold text-[#5E625A] hover:text-[#111310]"
          >
            Huỷ
          </button>
          <button
            type="button"
            onClick={confirm}
            disabled={submitting || session.availableSpots <= 0}
            className="px-4 py-2 text-sm font-bold bg-[#D7FF3F] text-[#111310] rounded-lg hover:bg-[#C4E82E] disabled:opacity-50"
          >
            {submitting ? 'Đang đặt…' : 'Xác nhận đặt chỗ'}
          </button>
        </div>
      </div>
    </div>
  );
}

function Item({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-[10px] font-semibold uppercase tracking-[0.08em] text-[#8A8E85]">{label}</dt>
      <dd className="text-sm font-semibold text-[#111310] mt-0.5">{value}</dd>
    </div>
  );
}
