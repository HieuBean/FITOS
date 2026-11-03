'use client';

import React, { useCallback, useEffect, useState } from 'react';
import api from '@/lib/api';
import MembershipBadge from '@/components/MembershipBadge';
import { formatDate, type MembershipDto } from '@/lib/contracts';

const riskStyles: Record<string, string> = {
  HIGH: 'bg-red-100 text-red-700 border-red-200',
  MEDIUM: 'bg-amber-100 text-amber-800 border-amber-200',
  LOW: 'bg-emerald-100 text-emerald-800 border-emerald-200',
};

const riskLabels: Record<string, string> = {
  HIGH: 'Nguy cơ cao',
  MEDIUM: 'Cần theo dõi',
  LOW: 'An toàn',
};

interface Props {
  days?: number;
  /** Cho phép chọn số ngày cảnh báo (mặc định 7). */
  showRangeSelector?: boolean;
}

/**
 * Task #88 — widget cảnh báo gia hạn: gọi /memberships/expiring, hiển thị mức rủi ro
 * kèm hành động nhanh (gọi khách / gia hạn).
 */
export default function RenewalRiskWidget({ days = 7, showRangeSelector = true }: Props) {
  const [range, setRange] = useState(days);
  const [items, setItems] = useState<MembershipDto[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [renewingId, setRenewingId] = useState<number | null>(null);

  const load = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const { data } = await api.get('/memberships/expiring', { params: { days: range } });
      setItems(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('Không tải được danh sách sắp hết hạn:', err);
      setItems([]);
      setError('Không tải được danh sách gia hạn.');
    } finally {
      setIsLoading(false);
    }
  }, [range]);

  useEffect(() => {
    load();
  }, [load]);

  const renew = async (id: number) => {
    setRenewingId(id);
    try {
      await api.post(`/memberships/${id}/renew`, {});
      await load();
    } catch (err) {
      console.error('Gia hạn thất bại:', err);
      setError('Gia hạn thất bại — kiểm tra lại gói dịch vụ.');
    } finally {
      setRenewingId(null);
    }
  };

  return (
    <section className="bg-white border border-[#DADCD3] rounded-xl shadow-sm">
      <header className="flex items-center justify-between px-5 py-4 border-b border-[#DADCD3]">
        <div>
          <h2 className="text-sm font-bold text-[#111310]">Cảnh báo gia hạn</h2>
          <p className="text-[11px] text-[#8A8E85] mt-0.5">
            Gói hết hạn trong {range} ngày tới — ưu tiên xử lý nguy cơ cao
          </p>
        </div>
        {showRangeSelector && (
          <div className="flex items-center gap-1">
            {[7, 14, 30].map((option) => (
              <button
                key={option}
                type="button"
                onClick={() => setRange(option)}
                className={`px-2.5 py-1 rounded-lg text-[11px] font-bold transition-colors cursor-pointer ${
                  range === option
                    ? 'bg-[#171A16] text-[#D7FF3F]'
                    : 'text-[#5E625A] hover:bg-[#F5F5F0]'
                }`}
              >
                {option} ngày
              </button>
            ))}
          </div>
        )}
      </header>

      {error && <p className="px-5 py-3 text-xs text-red-700" role="alert">{error}</p>}

      {isLoading ? (
        <div className="p-8 text-center">
          <div className="w-6 h-6 border-2 border-[#171A16] border-t-[#D7FF3F] rounded-full animate-spin mx-auto" />
        </div>
      ) : items.length === 0 ? (
        <div className="p-8 text-center">
          <p className="text-sm font-semibold text-[#111310]">Không có gói nào sắp hết hạn</p>
          <p className="text-xs text-[#8A8E85] mt-1">Tất cả hội viên trong khoảng {range} ngày đều ổn.</p>
        </div>
      ) : (
        <ul className="divide-y divide-[#DADCD3]">
          {items.map((item) => (
            <li key={item.id} className="flex items-center gap-3 px-5 py-3">
              <div className="flex-1 min-w-0">
                <p className="text-sm font-semibold text-[#111310] truncate">{item.customerName}</p>
                <p className="text-[11px] text-[#8A8E85]">
                  {item.planName} · hết hạn {formatDate(item.endDate)}
                </p>
              </div>
              <span
                className={`text-[10px] font-bold px-2 py-0.5 rounded-full border ${
                  riskStyles[item.riskLevel] || riskStyles.LOW
                }`}
                title={item.riskReason}
              >
                {riskLabels[item.riskLevel] || item.riskLevel}
              </span>
              <MembershipBadge status={item.status} daysRemaining={item.daysRemaining} compact />
              <button
                type="button"
                onClick={() => renew(item.id)}
                disabled={renewingId === item.id}
                className="text-[11px] font-bold px-2.5 py-1.5 rounded-lg bg-[#D7FF3F] text-[#111310] hover:bg-[#C4E82E] disabled:opacity-50 transition-colors cursor-pointer"
              >
                {renewingId === item.id ? 'Đang gia hạn…' : 'Gia hạn'}
              </button>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
