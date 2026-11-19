'use client';

import { useCallback, useEffect, useState } from 'react';
import api from '@/lib/api';
import RoleGuard from '@/components/RoleGuard';
import { formatCurrency } from '@/lib/contracts';

interface Invoice {
  id: number; reference: string; orderType: string; customerName: string;
  employeeName: string; totalAmount: number; returnedAmount: number;
  netAmount: number; paidAt: string;
}
interface InvoiceItem {
  id: number; itemType: string; productId?: number | null; name: string;
  quantity: number; unitPrice: number; totalPrice: number;
}
interface InvoiceDetail { invoice: Invoice; items: InvoiceItem[]; returns: Invoice[] }
interface InvoicePage { content: Invoice[]; totalElements: number }

export default function InvoicesPage() {
  return <RoleGuard allowedRoles={['MANAGER', 'RECEPTIONIST']}><InvoiceExplorer /></RoleGuard>;
}

function InvoiceExplorer() {
  const [rows, setRows] = useState<Invoice[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [detail, setDetail] = useState<InvoiceDetail | null>(null);
  const [keyword, setKeyword] = useState('');
  const [activeKeyword, setActiveKeyword] = useState('');
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true); setError('');
    try {
      const response = await api.post<InvoicePage>('/invoices/search', {
        page, size: 20, keyword: activeKeyword,
      });
      setRows(response.data.content ?? []);
      setTotal(response.data.totalElements ?? 0);
      setSelectedId((current) => response.data.content.some((item) => item.id === current)
        ? current : response.data.content[0]?.id ?? null);
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Không tải được danh sách hóa đơn');
      setRows([]); setTotal(0); setSelectedId(null);
    } finally { setLoading(false); }
  }, [page, activeKeyword]);
  useEffect(() => { void load(); }, [load]);
  useEffect(() => {
    if (!selectedId) { setDetail(null); return; }
    let cancelled = false;
    setDetailLoading(true);
    api.get<InvoiceDetail>(`/invoices/${selectedId}`).then((response) => {
      if (!cancelled) setDetail(response.data);
    }).catch(() => { if (!cancelled) setError('Không tải được chi tiết hóa đơn'); })
      .finally(() => { if (!cancelled) setDetailLoading(false); });
    return () => { cancelled = true; };
  }, [selectedId]);

  const searchInvoices = () => {
    setPage(0);
    setActiveKeyword(keyword.trim());
  };

  return <div className="min-h-screen bg-[#F5F5F0]">
    <header className="border-b border-[#DADCD3] bg-white px-4 py-5 sm:px-8">
      <p className="text-xs font-semibold uppercase text-[#526B00]">Hóa đơn</p>
      <h1 className="mt-1 text-2xl font-bold">Danh sách & chi tiết hóa đơn</h1>
      <p className="mt-1 text-sm text-[#5E625A]">Đơn bán gói và bán lẻ đã thu tiền. Chọn hóa đơn để xem chi tiết ngay bên cạnh.</p>
      <div className="mt-4 flex flex-wrap items-center gap-2">
        <input type="search" aria-label="Tìm hóa đơn" placeholder="Mã hóa đơn hoặc khách hàng"
          value={keyword} onChange={(event) => setKeyword(event.target.value)}
          onKeyDown={(event) => { if (event.key === 'Enter') searchInvoices(); }}
          className="h-11 min-w-[220px] flex-[2] rounded-lg border border-[#DADCD3] bg-white px-3 text-sm" />
        <button type="button" onClick={searchInvoices} className="h-11 rounded-lg bg-[#D7FF3F] px-4 text-sm font-bold">Tìm</button>
        <button type="button" onClick={() => { setKeyword(''); setActiveKeyword(''); setPage(0); }}
          className="h-11 rounded-lg border border-[#DADCD3] bg-white px-4 text-sm">Xóa tìm kiếm</button>
      </div>
    </header>
    <main className="grid gap-4 p-4 lg:grid-cols-[minmax(320px,1fr)_minmax(400px,1.2fr)] sm:p-6">
      <section aria-label="Danh sách hóa đơn" className="rounded-xl border border-[#DADCD3] bg-white p-4">
        <h2 className="text-lg font-bold">Hóa đơn ({total})</h2>
        {error && <p role="alert" className="mt-3 rounded-lg bg-red-50 p-3 text-sm text-red-700">{error}</p>}
        {loading ? <p className="mt-3 text-sm">Đang tải…</p> : rows.length === 0
          ? <p className="mt-3 text-sm text-[#5E625A]">Không có hóa đơn phù hợp.</p>
          : <div className="mt-3 max-h-[680px] space-y-2 overflow-auto">
            {rows.map((invoice) => <button key={invoice.id} type="button" onClick={() => setSelectedId(invoice.id)}
              aria-pressed={selectedId === invoice.id}
              className={`w-full rounded-lg border p-3 text-left text-sm ${selectedId === invoice.id ? 'border-[#526B00] bg-[#F3F9E1]' : 'border-[#DADCD3]'}`}>
              <span className="flex justify-between gap-3 font-bold"><span>{invoice.reference}</span><span>{formatCurrency(Number(invoice.netAmount))}</span></span>
              <span className="mt-1 block text-xs text-[#5E625A]">{invoice.customerName} · {new Date(invoice.paidAt).toLocaleDateString('vi-VN')}</span>
            </button>)}
          </div>}
        <div className="mt-4 flex items-center justify-between gap-2 border-t border-[#DADCD3] pt-3 text-sm">
          <button type="button" disabled={page === 0 || loading} onClick={() => setPage((value) => value - 1)}
            className="rounded-lg border border-[#DADCD3] px-3 py-2 disabled:opacity-40">Trước</button>
          <span>Trang {page + 1} / {Math.max(1, Math.ceil(total / 20))}</span>
          <button type="button" disabled={(page + 1) * 20 >= total || loading} onClick={() => setPage((value) => value + 1)}
            className="rounded-lg border border-[#DADCD3] px-3 py-2 disabled:opacity-40">Sau</button>
        </div>
      </section>
      <section aria-label="Chi tiết hóa đơn" className="rounded-xl border border-[#DADCD3] bg-white p-4" aria-live="polite">
        <h2 className="text-lg font-bold">Chi tiết hóa đơn</h2>
        {detailLoading ? <p className="mt-3 text-sm">Đang tải chi tiết…</p> : !detail
          ? <p className="mt-3 text-sm text-[#5E625A]">Chọn hóa đơn ở danh sách bên trái.</p>
          : <div className="mt-3 space-y-4 text-sm">
            <div className="grid gap-2 rounded-lg bg-[#F5F5F0] p-4 sm:grid-cols-2">
              <p><strong>Mã:</strong> {detail.invoice.reference}</p>
              <p><strong>Loại:</strong> {detail.invoice.orderType === 'RETAIL' ? 'Bán lẻ' : 'Bán gói'}</p>
              <p><strong>Khách:</strong> {detail.invoice.customerName}</p>
              <p><strong>Nhân viên:</strong> {detail.invoice.employeeName}</p>
              <p><strong>Ngày thu:</strong> {new Date(detail.invoice.paidAt).toLocaleString('vi-VN')}</p>
            </div>
            <div className="overflow-x-auto"><table className="w-full text-left text-sm">
              <thead><tr className="border-b border-[#DADCD3]"><th className="py-2">Mặt hàng</th><th className="py-2 text-right">SL</th><th className="py-2 text-right">Đơn giá</th><th className="py-2 text-right">Thành tiền</th></tr></thead>
              <tbody>{detail.items.map((item) => <tr key={item.id} className="border-b border-[#EDEEE8]">
                <td className="py-2">{item.name}</td><td className="py-2 text-right">{item.quantity}</td>
                <td className="py-2 text-right">{formatCurrency(Number(item.unitPrice))}</td>
                <td className="py-2 text-right">{formatCurrency(Number(item.totalPrice))}</td>
              </tr>)}</tbody>
            </table></div>
            <div className="space-y-1 border-t border-[#DADCD3] pt-3 text-right">
              <p>Tổng hóa đơn: <strong>{formatCurrency(Number(detail.invoice.totalAmount))}</strong></p>
              <p>Đã trả hàng: <strong>{formatCurrency(Number(detail.invoice.returnedAmount))}</strong></p>
              <p>Doanh thu thuần: <strong>{formatCurrency(Number(detail.invoice.netAmount))}</strong></p>
            </div>
            {detail.returns.length > 0 && <div><h3 className="font-bold">Phiếu trả hàng</h3>
              {detail.returns.map((ret) => <p key={ret.id}>{ret.reference}: {formatCurrency(Number(ret.totalAmount))}</p>)}
            </div>}
          </div>}
      </section>
    </main>
  </div>;
}
