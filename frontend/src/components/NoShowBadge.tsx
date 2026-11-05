'use client';

import React, { useCallback, useEffect, useState } from 'react';
import api from '@/lib/api';

export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH';

interface NoShowRisk {
  sessionId?: number;
  customerId?: number | null;
  probability: number;
  riskLevel: RiskLevel | string;
  reason: string;
  source?: string;
}

const styles: Record<RiskLevel, { chip: string; bar: string; label: string; dot: string; bg: string }> = {
  HIGH: {
    chip: 'bg-rose-50 text-rose-700 border-rose-200',
    bar: 'bg-rose-500',
    dot: 'bg-rose-500 animate-pulse',
    bg: 'bg-rose-50 border-rose-200 text-rose-700',
    label: 'Nguy cơ cao (High Risk)',
  },
  MEDIUM: {
    chip: 'bg-amber-50 text-amber-800 border-amber-200',
    bar: 'bg-amber-500',
    dot: 'bg-amber-500',
    bg: 'bg-amber-50 border-amber-200 text-amber-700',
    label: 'Trung bình (Medium Risk)',
  },
  LOW: {
    chip: 'bg-emerald-50 text-emerald-800 border-emerald-200',
    bar: 'bg-emerald-500',
    dot: 'bg-emerald-500',
    bg: 'bg-emerald-50 border-emerald-200 text-emerald-700',
    label: 'Thấp (Low Risk)',
  },
};

export interface NoShowBadgeProps {
  sessionId?: number;
  customerId?: number;
  autoLoad?: boolean;
  compact?: boolean;
  probability?: number | null;
  riskLevel?: 'LOW' | 'MEDIUM' | 'HIGH';
  showPercentage?: boolean;
}

/**
 * Task #94 & #96 — Unified NoShowBadge
 * Supports both direct probability/riskLevel injection and async sessionId fetching.
 */
export default function NoShowBadge({
  sessionId,
  customerId,
  autoLoad = true,
  compact = false,
  probability,
  riskLevel,
  showPercentage = true,
}: NoShowBadgeProps) {
  const [risk, setRisk] = useState<NoShowRisk | null>(null);
  const [isLoading, setIsLoading] = useState(sessionId != null && probability == null && autoLoad);
  const [failed, setFailed] = useState(false);

  const load = useCallback(async () => {
    if (!sessionId) return;
    setIsLoading(true);
    setFailed(false);
    try {
      const { data } = await api.post<NoShowRisk>('/ai/noshow-risk', {
        sessionId,
        customerId: customerId ?? null,
      });
      setRisk(data);
    } catch (err) {
      console.warn('Không lấy được dự đoán no-show:', err);
      setRisk(null);
      setFailed(true);
    } finally {
      setIsLoading(false);
    }
  }, [sessionId, customerId]);

  useEffect(() => {
    if (probability == null && sessionId != null && autoLoad) {
      load();
    }
  }, [sessionId, probability, autoLoad, load]);

  // If direct probability or riskLevel provided
  if (probability != null || riskLevel) {
    let level: RiskLevel = riskLevel || 'LOW';
    let percentage = 0;

    if (probability != null) {
      percentage = Math.round(probability * 100);
      if (probability >= 0.6) {
        level = 'HIGH';
      } else if (probability >= 0.3) {
        level = 'MEDIUM';
      } else {
        level = 'LOW';
      }
    }

    const currentStyle = styles[level] || styles.LOW;

    return (
      <span
        className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold border ${currentStyle.bg}`}
        title={`AI No-show prediction: ${percentage}% likelihood of cancellation or no-show`}
      >
        <span className={`w-1.5 h-1.5 rounded-full ${currentStyle.dot}`} />
        <span>{currentStyle.label}</span>
        {showPercentage && probability != null && (
          <span className="opacity-75 font-normal">({percentage}%)</span>
        )}
      </span>
    );
  }

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
