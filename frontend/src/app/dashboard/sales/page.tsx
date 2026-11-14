'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import apiClient from '@/lib/api';
import { formatCurrency } from '@/lib/contracts';

interface SaleItem {
  id: number;
  orderReference: string;
  customerName: string;
  items: Array<{ planName: string }>;
  totalAmount: number;
  status: 'PAID' | 'PENDING' | 'CANCELLED';
  createdAt: string;
  paidAt?: string;
  payments?: Array<{ paymentMethod: string }>;
}

const statusLabel: Record<string, string> = {
  PAID: 'Đã thanh toán',
  PENDING: 'Chờ thanh toán',
  CANCELLED: 'Đã huỷ',
};

const statusPill: Record<string, string> = {
  PAID: 'bg-emerald-50 text-emerald-700 border border-emerald-200',
  PENDING: 'bg-amber-50 text-amber-700 border border-amber-200',
  CANCELLED: 'bg-rose-50 text-rose-700 border border-rose-200',
};

export default function SalesPage() {
  const [sales, setSales] = useState<SaleItem[]>([]);
  const [statusFilter, setStatusFilter] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);

  useEffect(() => {
    async function loadSales() {
      setLoading(true);
      setError(null);
      try {
        const res = await apiClient.get('/orders', {
          params: { status: statusFilter || undefined },
        });
        setSales(res.data.content || []);
      } catch (e) {
        // KHÔNG dùng dữ liệu giả: trước đây trang này hiển thị 5 đơn bịa
        // (Sophie Turner, James Hargreaves…) khi API lỗi, khiến người dùng
        // tưởng đã có doanh thu. Nay báo lỗi rõ ràng.
        console.error('Không tải được danh sách đơn hàng:', e);
        setSales([]);
        setError('Không tải được danh sách đơn hàng. Kiểm tra kết nối rồi thử lại.');
      } finally {
        setLoading(false);
      }
    }

    loadSales();
  }, [statusFilter, refreshKey]);

  // Aggregate stats
  const totalRevenue = sales
    .filter((s) => s.status === 'PAID')
    .reduce((acc, curr) => acc + curr.totalAmount, 0);

  const paidCount = sales.filter((s) => s.status === 'PAID').length;
  const pendingCount = sales.filter((s) => s.status === 'PENDING').length;
  const aov = paidCount > 0 ? Math.round(totalRevenue / paidCount) : 0;

  return (
    <div className="min-h-screen bg-[#F5F5F0]">
      {/* Page Header */}
      <div className="bg-white border-b border-[#DADCD3] px-8 py-6">
        <div className="flex items-start justify-between">
          <div>
            <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[#526B00] mb-1">
              Tăng trưởng · Bán hàng
            </p>
            <h1 className="text-2xl font-bold text-[#111310]">Đơn hàng &amp; Doanh số</h1>
            <p className="text-sm text-[#5E625A] mt-1">
              Theo dõi giao dịch, đơn bán gói và doanh thu của studio
            </p>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <Link
              href="/dashboard/pos"
              className="bg-[#111310] text-white font-bold rounded-lg px-4 py-2 text-sm hover:brightness-110 shadow-sm"
            >
              Bán tại quầy (POS)
            </Link>
            <Link
              href="/dashboard/create-sale"
              className="bg-[#D7FF3F] text-[#111310] font-bold rounded-lg px-4 py-2 text-sm hover:opacity-90 shadow-sm"
            >
              + Tạo đơn mới
            </Link>
          </div>
        </div>
      </div>

      {/* Page Content */}
      <div className="p-8">
        {/* KPI Cards */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-5 mb-8">
          <div className="bg-white border border-[#DADCD3] rounded-xl p-5 shadow-sm">
            <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[#6B6F66] mb-2">
              Paid Revenue
            </p>
            <p className="text-3xl font-extrabold text-[#111310]">
              {formatCurrency(totalRevenue)}
            </p>
            <p className="text-xs text-[#526B00] mt-1 font-medium">▲ Healthy flow</p>
          </div>
          <div className="bg-white border border-[#DADCD3] rounded-xl p-5 shadow-sm">
            <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[#6B6F66] mb-2">
              Paid Transactions
            </p>
            <p className="text-3xl font-extrabold text-[#111310]">{paidCount}</p>
            <p className="text-xs text-[#526B00] mt-1 font-medium">Đơn đã hoàn tất</p>
          </div>
          <div className="bg-white border border-[#DADCD3] rounded-xl p-5 shadow-sm">
            <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[#6B6F66] mb-2">
              Đơn chờ thanh toán
            </p>
            <p className="text-3xl font-extrabold text-amber-600">{pendingCount}</p>
            <p className="text-xs text-[#5E625A] mt-1 font-medium">Đang chờ thu tiền</p>
          </div>
          <div className="bg-white border border-[#DADCD3] rounded-xl p-5 shadow-sm">
            <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[#6B6F66] mb-2">
              Giá trị đơn TB
            </p>
            <p className="text-3xl font-extrabold text-[#111310]">
              {formatCurrency(aov)}
            </p>
            <p className="text-xs text-[#526B00] mt-1 font-medium">Tính trên đơn đã thu</p>
          </div>
        </div>

        {/* Filter bar */}
        <div className="-mx-1 flex gap-2 mb-4 overflow-x-auto pb-1">
          {[
            { id: '', label: 'Tất cả giao dịch' },
            { id: 'PAID', label: 'Đã thanh toán' },
            { id: 'PENDING', label: 'Chờ thanh toán' },
            { id: 'CANCELLED', label: 'Đã huỷ' },
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setStatusFilter(tab.id)}
              className={`px-4 py-1.5 rounded-lg text-xs font-semibold transition-colors shrink-0 whitespace-nowrap ${
                statusFilter === tab.id
                  ? 'bg-[#171A16] text-[#ECEDE6]'
                  : 'bg-white border border-[#DADCD3] text-[#5E625A] hover:bg-black/5'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {error && (
          <div
            role="alert"
            className="mb-4 rounded-lg border border-[#E7B4B4] bg-[#FDECEC] px-3 py-2 text-xs font-medium text-[#8C1D1D]"
          >
            {error}
            <button
              type="button"
              onClick={() => setRefreshKey((k) => k + 1)}
              className="ml-3 underline font-semibold"
            >
              Thử lại
            </button>
          </div>
        )}

        {/* Sales Table */}
        <div className="bg-white border border-[#DADCD3] rounded-xl overflow-hidden shadow-sm">
          {/* Header */}
          <div className="grid grid-cols-[1.5fr_2fr_2fr_1.2fr_1.2fr_1fr] px-6 py-3.5 bg-[#F5F5F0]">
            {['Mã đơn', 'Hội viên', 'Hạng mục', 'Số tiền', 'Phương thức', 'Trạng thái'].map((col) => (
              <span key={col} className="text-[11px] font-semibold uppercase tracking-[0.08em] text-[#6B6F66]">
                {col}
              </span>
            ))}
          </div>

          {/* Rows */}
          {loading ? (
            <div className="p-8 text-center text-sm text-[#6B6F66]">Đang tải đơn hàng…</div>
          ) : (
            <div className="divide-y divide-[#DADCD3]">
              {sales.map((sale) => (
                <div
                  key={sale.id}
                  className="grid grid-cols-[1.5fr_2fr_2fr_1.2fr_1.2fr_1fr] px-6 items-center py-4 hover:bg-[#F9FFDF]/30 transition-colors"
                >
                  <span className="font-mono text-xs font-bold text-[#111310]">
                    {sale.orderReference}
                  </span>

                  <span className="text-sm font-semibold text-[#111310] truncate">
                    {sale.customerName || 'Khách lẻ'}
                  </span>

                  <span className="text-sm text-[#5E625A] truncate">
                    {sale.items?.[0]?.planName || 'Gói dịch vụ'}
                  </span>

                  <span className="text-sm font-extrabold text-[#111310]">
                    {formatCurrency(sale.totalAmount)}
                  </span>

                  <span className="text-xs font-medium text-[#5E625A]">
                    {sale.payments?.[0]?.paymentMethod || '—'}
                  </span>

                  <div>
                    <span
                      className={`text-xs font-semibold px-2.5 py-0.5 rounded-full ${
                        statusPill[sale.status] || 'bg-gray-100 text-gray-700'
                      }`}
                    >
                      {statusLabel[sale.status] || sale.status}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
