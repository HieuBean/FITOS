'use client';

import React, { useEffect, useMemo, useState } from 'react';
import api from '@/lib/api';
import RoleGuard from '@/components/RoleGuard';
import MembershipBadge from '@/components/MembershipBadge';
import {
  formatCurrency,
  formatDate,
  type MembershipDto,
  type PaymentDto,
} from '@/lib/contracts';

const paymentStatusStyles: Record<string, string> = {
  SUCCESS: 'bg-emerald-100 text-emerald-800',
  PENDING: 'bg-amber-100 text-amber-800',
  FAILED: 'bg-red-100 text-red-700',
  REFUNDED: 'bg-zinc-100 text-zinc-600',
};

const paymentStatusLabels: Record<string, string> = {
  SUCCESS: 'Thành công',
  PENDING: 'Đang xử lý',
  FAILED: 'Thất bại',
  REFUNDED: 'Đã hoàn tiền',
};

/** Task #86 — membership card + lịch sử thanh toán thật của hội viên. */
export default function PaymentsPage() {
  const [memberships, setMemberships] = useState<MembershipDto[]>([]);
  const [payments, setPayments] = useState<PaymentDto[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      setIsLoading(true);
      setError(null);
      try {
        const [membershipRes, paymentRes] = await Promise.all([
          api.get('/memberships/me'),
          api.get('/payments/me'),
        ]);
        if (cancelled) return;
        setMemberships(Array.isArray(membershipRes.data) ? membershipRes.data : []);
        setPayments(Array.isArray(paymentRes.data) ? paymentRes.data : []);
      } catch (err) {
        console.error('Không tải được dữ liệu thanh toán:', err);
        if (!cancelled) setError('Không tải được dữ liệu thanh toán.');
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    };
    load();
    return () => {
      cancelled = true;
    };
  }, []);

  const activeMembership = useMemo(
    () => memberships.find((item) => item.status === 'ACTIVE') ?? memberships[0] ?? null,
    [memberships],
  );

  const totalPaid = useMemo(
    () =>
      payments
        .filter((payment) => payment.status === 'SUCCESS')
        .reduce((sum, payment) => sum + Number(payment.amount || 0), 0),
    [payments],
  );

  return (
    <RoleGuard allowedRoles={['MEMBER', 'ADMIN']}>
      <div className="min-h-screen bg-[#F5F5F0]">
        <div className="bg-white border-b border-[#DADCD3] px-8 py-6">
          <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[#526B00] mb-1">
            HỘI VIÊN · THANH TOÁN
          </p>
          <h1 className="text-2xl font-bold text-[#111310]">Gói tập &amp; Lịch sử thanh toán</h1>
          <p className="text-sm text-[#5E625A] mt-1">Theo dõi thời hạn gói và các giao dịch của bạn</p>
        </div>

        <div className="p-8 space-y-8">
          {error && (
            <p role="alert" className="text-sm text-red-700 bg-red-50 border border-red-200 rounded-lg px-4 py-3">
              {error}
            </p>
          )}

          {isLoading ? (
            <div className="p-12 text-center">
              <div className="w-8 h-8 border-2 border-[#171A16] border-t-[#D7FF3F] rounded-full animate-spin mx-auto" />
            </div>
          ) : (
            <>
              {/* Membership card */}
              <div className="bg-[#171A16] rounded-2xl p-7 text-white">
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[#8A8E85] mb-2">
                      Gói hiện tại
                    </p>
                    <h2 className="text-3xl font-bold text-[#ECEDE6] mb-1">
                      {activeMembership?.planName ?? 'Chưa có gói'}
                    </h2>
                    <p className="text-[#8A8E85] text-sm">
                      {activeMembership
                        ? `${formatDate(activeMembership.startDate)} – ${formatDate(activeMembership.endDate)}`
                        : 'Mua gói để bắt đầu tập luyện'}
                    </p>
                  </div>
                  {activeMembership && (
                    <MembershipBadge
                      status={activeMembership.status}
                      daysRemaining={activeMembership.daysRemaining}
                    />
                  )}
                </div>

                <div className="flex items-end justify-between mt-6 pt-6 border-t border-[#2A2E29] gap-4">
                  <div>
                    <p className="text-[11px] text-[#8A8E85] mb-1">Tổng đã thanh toán</p>
                    <p className="text-2xl font-bold text-[#ECEDE6]">{formatCurrency(totalPaid)}</p>
                    <p className="text-xs text-[#8A8E85] mt-1">
                      {payments.length} giao dịch · {memberships.length} gói đã mua
                    </p>
                  </div>
                  <span className="text-xs text-[#8A8E85] text-right max-w-[220px]">
                    {activeMembership?.riskReason ?? 'Gia hạn tại quầy hoặc qua lễ tân để tiếp tục tập.'}
                  </span>
                </div>
              </div>

              {/* Danh sách gói */}
              {memberships.length > 0 && (
                <div>
                  <p className="text-xs font-semibold uppercase tracking-[0.08em] text-[#8A8E85] mb-3">
                    Các gói của bạn
                  </p>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                    {memberships.map((membership) => (
                      <div
                        key={membership.id}
                        className="bg-white border border-[#DADCD3] rounded-xl px-5 py-4 flex items-center justify-between gap-3"
                      >
                        <div className="min-w-0">
                          <p className="text-sm font-bold text-[#111310] truncate">{membership.planName}</p>
                          <p className="text-[11px] text-[#8A8E85]">
                            Hết hạn {formatDate(membership.endDate)}
                            {membership.pauseDaysUsed > 0 && ` · đã tạm dừng ${membership.pauseDaysUsed} ngày`}
                          </p>
                        </div>
                        <MembershipBadge
                          status={membership.status}
                          daysRemaining={membership.daysRemaining}
                          compact
                        />
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Payment history */}
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.08em] text-[#8A8E85] mb-3">
                  Lịch sử thanh toán
                </p>
                <div className="bg-white border border-[#DADCD3] rounded-xl overflow-hidden">
                  <div className="grid grid-cols-[1.2fr_1.4fr_1fr_0.9fr] px-6 py-3 bg-[#F5F5F0] border-b border-[#DADCD3]">
                    {['Ngày', 'Mã giao dịch', 'Số tiền', 'Trạng thái'].map((col) => (
                      <span key={col} className="text-[11px] font-semibold uppercase tracking-[0.08em] text-[#8A8E85]">
                        {col}
                      </span>
                    ))}
                  </div>
                  {payments.length === 0 ? (
                    <div className="p-10 text-center">
                      <p className="text-sm font-bold text-[#111310]">Chưa có giao dịch nào</p>
                      <p className="text-xs text-[#8A8E85] mt-1">Thanh toán gói tập sẽ hiển thị ở đây.</p>
                    </div>
                  ) : (
                    <div className="divide-y divide-[#DADCD3]">
                      {payments.map((payment) => (
                        <div
                          key={payment.id}
                          className="grid grid-cols-[1.2fr_1.4fr_1fr_0.9fr] px-6 py-3.5 items-center"
                        >
                          <span className="text-sm text-[#5E625A]">
                            {formatDate(payment.paidAt || payment.createdAt)}
                          </span>
                          <span className="text-sm font-medium text-[#111310] truncate">
                            {payment.transactionRef}
                            <span className="block text-[10px] text-[#8A8E85]">{payment.paymentMethod}</span>
                          </span>
                          <span className="text-sm font-semibold text-[#111310]">
                            {formatCurrency(Number(payment.amount))}
                          </span>
                          <span
                            className={`text-xs font-semibold px-2 py-0.5 rounded-full w-fit ${
                              paymentStatusStyles[payment.status] ?? 'bg-[#F0F0EB] text-[#5E625A]'
                            }`}
                          >
                            {paymentStatusLabels[payment.status] ?? payment.status}
                          </span>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            </>
          )}
        </div>
      </div>
    </RoleGuard>
  );
}
