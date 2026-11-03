'use client';

import React, { useCallback, useEffect, useState } from 'react';
import api from '@/lib/api';

export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH';

interface NoShowRisk {
  sessionId: number;
  customerId?: number | null;
  probability: number;
  riskLevel: RiskLevel | string;
  reason: string;
  source: string;
}

const styles: Record<RiskLevel, { chip: string; bar: string; label: string }> = {
  HIGH: {
    chip: 'bg-red-100 text-red-700 border-red-200',
    bar: 'bg-red-500',
    label: 'Nguy cơ cao',
  },
  MEDIUM: {
    chip: 'bg-amber-100 text-amber-800 border-amber-200',
    bar: 'bg-amber-500',
    label: 'Trung bình',
  },
  LOW: {
    chip: 'bg-emerald-100 text-emerald-800 border-emerald-200',
    bar: 'bg-emerald-500',
    label: 'Thấp',
  },
};

interface Props {
  sessionId: number;
  customerId?: number;
  /** Gọi API ngay khi mount (mặc định true). */
  autoLoad?: boolean;
  compact?: boolean;
}

/**
 * Task #96 — badge dự đoán no-show. Gọi `/api/ai/noshow-risk`, hiển thị xác suất,
 * mức rủi ro và lý do; lỗi API không được làm vỡ bảng bookings.
 */
export default function NoShowBadge({ sessionId, customerId, autoLoad = true, compact = false }: Props) {
  const [risk, setRisk] = useState<NoShowRisk | null>(null);
  const [isLoading, setIsLoading] = useState(autoLoad);
  const [failed, setFailed] = useState(false);

  const load = useCallback(async () => {
    setIsLoading(true);
    setFailed(false);
    try {
      const { data } = await api.post<NoShowRisk>('/ai/noshow-risk', {
        sessionId,
        customerId: customerId ?? null,
      });
      setRisk(data);
    } catch (err) {
      // Dự đoán là thông tin phụ — im lặng và ẩn badge
      console.warn('Không lấy được dự đoán no-show:', err);
      setRisk(null);
      setFailed(true);
    } finally {
      setIsLoading(false);
    }
  }, [sessionId, customerId]);

  useEffect(() => {
    if (autoLoad) {
      load();
    }
  }, [autoLoad, load]);

  if (isLoading) {
    return <span className="text-[10px] text-[#8A8E85]">đang tính…</span>;
  }
  if (failed || !risk) {
    return null;
  }

  const level = (String(risk.riskLevel).toUpperCase() as RiskLevel) in styles
    ? (String(risk.riskLevel).toUpperCase() as RiskLevel)
    : 'LOW';
  const percent = Math.round(risk.probability * 100);

  if (compact) {
    return (
      <span
        className={`inline-flex items-center gap-1 rounded-full border px-2 py-0.5 text-[10px] font-semibold ${styles[level].chip}`}
        title={risk.reason}
      >
        <span className={`h-1.5 w-1.5 rounded-full ${styles[level].bar}`} aria-hidden="true" />
        {percent}%
      </span>
    );
  }

  return (
    <div
      className={`rounded-lg border px-3 py-2 ${styles[level].chip}`}
      title={`${risk.reason}\nNguồn: ${risk.source === 'ai-operations' ? 'ai-operations' : 'rule-based (fallback)'}`}
    >
      <div className="flex items-center justify-between gap-3">
        <span className="text-[11px] font-bold">{styles[level].label}</span>
        <span className="text-[11px] font-bold tabular-nums">{percent}%</span>
      </div>
      <div className="mt-1.5 h-1.5 w-full overflow-hidden rounded-full bg-white/60">
        <div className={`h-full rounded-full ${styles[level].bar}`} style={{ width: `${percent}%` }} />
      </div>
      <p className="mt-1 text-[10px] leading-tight opacity-80 line-clamp-2">{risk.reason}</p>
    </div>
  );
}
