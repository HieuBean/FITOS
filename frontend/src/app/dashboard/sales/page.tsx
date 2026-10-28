'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import apiClient from '@/lib/api';

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

const statusPill: Record<string, string> = {
  PAID: 'bg-emerald-50 text-emerald-700 border border-emerald-200',
  PENDING: 'bg-amber-50 text-amber-700 border border-amber-200',
  CANCELLED: 'bg-rose-50 text-rose-700 border border-rose-200',
};

export default function SalesPage() {
  const [sales, setSales] = useState<SaleItem[]>([]);
  const [statusFilter, setStatusFilter] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function loadSales() {
      setLoading(true);
      try {
        const res = await apiClient.get('/orders', {
          params: { status: statusFilter || undefined },
        });
        setSales(res.data.content || []);
      } catch (e) {
        // Fallback mock sales
        setSales([
          {
            id: 1,
            orderReference: 'ORD-20251025-101',
            customerName: 'Sophie Turner',
            items: [{ planName: 'Perform — All Access' }],
            totalAmount: 490000,
            status: 'PAID',
            createdAt: '2025-10-25T14:30:00Z',
            payments: [{ paymentMethod: 'CARD' }],
          },
          {
            id: 2,
            orderReference: 'ORD-20251025-102',
            customerName: 'James Hargreaves',
            items: [{ planName: 'Move — Off Peak' }],
            totalAmount: 290000,
            status: 'PAID',
            createdAt: '2025-10-25T11:15:00Z',
            payments: [{ paymentMethod: 'VNPAY' }],
          },
          {
            id: 3,
            orderReference: 'ORD-20251024-098',
            customerName: 'Priya Sharma',
            items: [{ planName: 'PT Focus (8 Sessions)' }],
            totalAmount: 2400000,
            status: 'PAID',
            createdAt: '2025-10-24T16:00:00Z',
            payments: [{ paymentMethod: 'BANK_TRANSFER' }],
          },
          {
            id: 4,
            orderReference: 'ORD-20251024-092',
            customerName: 'Oliver Webb',
            items: [{ planName: 'Perform — All Access' }],
            totalAmount: 490000,
            status: 'PENDING',
            createdAt: '2025-10-24T09:40:00Z',
          },
          {
            id: 5,
            orderReference: 'ORD-20251023-085',
            customerName: 'Harry Wilson',
            items: [{ planName: 'Move — Off Peak' }],
            totalAmount: 290000,
            status: 'CANCELLED',
            createdAt: '2025-10-23T18:20:00Z',
          },
        ]);
      } finally {
        setLoading(false);
      }
    }

    loadSales();
  }, [statusFilter]);

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
              GROW · SALES
            </p>
            <h1 className="text-2xl font-bold text-[#111310]">Sales & Orders</h1>
            <p className="text-sm text-[#5E625A] mt-1">
              Monitor commerce activity, transactions, and revenue
            </p>
          </div>
          <Link
            href="/dashboard/create-sale"
            className="bg-[#D7FF3F] text-[#111310] font-bold rounded-lg px-4 py-2 text-sm hover:opacity-90 shadow-sm"
          >
            + Create Sale
          </Link>
        </div>
      </div>

      {/* Page Content */}
      <div className="p-8">
        {/* KPI Cards */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-5 mb-8">
          <div className="bg-white border border-[#DADCD3] rounded-xl p-5 shadow-sm">
            <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[#8A8E85] mb-2">
              Paid Revenue
            </p>
            <p className="text-3xl font-extrabold text-[#111310]">
              ₫{totalRevenue.toLocaleString()}
            </p>
            <p className="text-xs text-[#526B00] mt-1 font-medium">▲ Healthy flow</p>
          </div>
          <div className="bg-white border border-[#DADCD3] rounded-xl p-5 shadow-sm">
            <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[#8A8E85] mb-2">
              Paid Transactions
            </p>
            <p className="text-3xl font-extrabold text-[#111310]">{paidCount}</p>
            <p className="text-xs text-[#526B00] mt-1 font-medium">Completed orders</p>
          </div>
          <div className="bg-white border border-[#DADCD3] rounded-xl p-5 shadow-sm">
            <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[#8A8E85] mb-2">
              Pending Orders
            </p>
            <p className="text-3xl font-extrabold text-amber-600">{pendingCount}</p>
            <p className="text-xs text-[#5E625A] mt-1 font-medium">Awaiting checkout</p>
          </div>
          <div className="bg-white border border-[#DADCD3] rounded-xl p-5 shadow-sm">
            <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[#8A8E85] mb-2">
              Avg. Order Value
            </p>
            <p className="text-3xl font-extrabold text-[#111310]">
              ₫{aov.toLocaleString()}
            </p>
            <p className="text-xs text-[#526B00] mt-1 font-medium">Per paying customer</p>
          </div>
        </div>

        {/* Filter bar */}
        <div className="flex gap-2 mb-4">
          {[
            { id: '', label: 'All Transactions' },
            { id: 'PAID', label: 'Paid' },
            { id: 'PENDING', label: 'Pending' },
            { id: 'CANCELLED', label: 'Cancelled' },
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setStatusFilter(tab.id)}
              className={`px-4 py-1.5 rounded-lg text-xs font-semibold transition-colors ${
                statusFilter === tab.id
                  ? 'bg-[#171A16] text-[#ECEDE6]'
                  : 'bg-white border border-[#DADCD3] text-[#5E625A] hover:bg-black/5'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {/* Sales Table */}
        <div className="bg-white border border-[#DADCD3] rounded-xl overflow-hidden shadow-sm">
          {/* Header */}
          <div className="grid grid-cols-[1.5fr_2fr_2fr_1.2fr_1.2fr_1fr] px-6 py-3.5 bg-[#F5F5F0]">
            {['Order Ref', 'Member', 'Plan', 'Amount', 'Method', 'Status'].map((col) => (
              <span key={col} className="text-[11px] font-semibold uppercase tracking-[0.08em] text-[#8A8E85]">
                {col}
              </span>
            ))}
          </div>

          {/* Rows */}
          {loading ? (
            <div className="p-8 text-center text-sm text-[#8A8E85]">Loading orders...</div>
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
                    {sale.customerName || 'Customer'}
                  </span>

                  <span className="text-sm text-[#5E625A] truncate">
                    {sale.items?.[0]?.planName || 'Service Plan'}
                  </span>

                  <span className="text-sm font-extrabold text-[#111310]">
                    ₫{sale.totalAmount.toLocaleString()}
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
                      {sale.status}
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
