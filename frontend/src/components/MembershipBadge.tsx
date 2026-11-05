'use client';

import React from 'react';
import type { MembershipStatus } from '@/lib/contracts';

const styles: Record<MembershipStatus, string> = {
  ACTIVE: 'bg-emerald-100 text-emerald-800 border-emerald-200',
  PAUSED: 'bg-amber-100 text-amber-800 border-amber-200',
  EXPIRED: 'bg-zinc-200 text-zinc-700 border-zinc-300',
  CANCELLED: 'bg-red-100 text-red-700 border-red-200',
  PENDING: 'bg-blue-100 text-blue-800 border-blue-200',
};

const labels: Record<MembershipStatus, string> = {
  ACTIVE: 'Đang hoạt động',
  PAUSED: 'Tạm dừng',
  EXPIRED: 'Hết hạn',
  CANCELLED: 'Đã huỷ',
  PENDING: 'Chờ kích hoạt',
};

const dots: Record<MembershipStatus, string> = {
  ACTIVE: 'bg-emerald-500',
  PAUSED: 'bg-amber-500',
  EXPIRED: 'bg-zinc-500',
  CANCELLED: 'bg-red-500',
  PENDING: 'bg-blue-500',
};

interface Props {
  status: MembershipStatus | string;
  daysRemaining?: number;
  compact?: boolean;
}

/** Task #87 — badge trạng thái gói hội viên, dùng chung ở mọi bảng. */
export default function MembershipBadge({ status, daysRemaining, compact = false }: Props) {
  const key = (String(status || 'PENDING').toUpperCase() as MembershipStatus) in styles
    ? (String(status).toUpperCase() as MembershipStatus)
    : 'PENDING';

  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full border font-semibold ${
        compact ? 'px-2 py-0.5 text-[10px]' : 'px-2.5 py-1 text-[11px]'
      } ${styles[key]}`}
      title={labels[key]}
    >
      <span className={`h-1.5 w-1.5 rounded-full ${dots[key]}`} aria-hidden="true" />
      {labels[key]}
      {key === 'ACTIVE' && typeof daysRemaining === 'number' && (
        <span className="opacity-70">· {Math.max(0, daysRemaining)} ngày</span>
      )}
    </span>
  );
}
