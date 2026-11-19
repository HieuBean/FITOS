'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import RoleGuard from '@/components/RoleGuard';
import { formatCurrency } from '@/lib/contracts';
import {
  DailySalesSummaryDto,
  PAYMENT_METHODS,
  RetailOrderDto,
  cancelRetailOrder,
  dailySalesSummary,
  getReceipt,
  getRetailOrder,
  listRetailOrders,
  payRetailOrder,
  returnRetailOrder,
  ReceiptDto,
} from '@/lib/retailApi';

type StatusFilter = '' | 'PAID' | 'PENDING' | 'CANCELLED' | 'REFUNDED';

const STATUS_LABEL: Record<string, string> = {
  PAID: 'Đã thanh toán',
  PENDING: 'Chờ thanh toán',
  CANCELLED: 'Đã huỷ',
  REFUNDED: 'Đã trả hàng',
  FAILED: 'Thất bại',
  EXPIRED: 'Hết hạn',
};

const STATUS_PILL: Record<string, string> = {
  PAID: 'bg-[#F3F9E1] text-[#3F4A12]',
  PENDING: 'bg-[#FDF6E7] text-[#8A5B00]',
  CANCELLED: 'bg-[#FDECEC] text-[#8C1D1D]',
  REFUNDED: 'bg-[#E8EEF9] text-[#1E3A6B]',
  FAILED: 'bg-[#FDECEC] text-[#8C1D1D]',
  EXPIRED: 'bg-[#F0F0EC] text-[#4A4E46]',
};

export default function RetailOrdersPage() {
  return (
    <RoleGuard allowedRoles={['RECEPTIONIST']}>
      <RetailOrders />
    </RoleGuard>
  );
}

function RetailOrders() {
  const [orders, setOrders] = useState<RetailOrderDto[]>([]);
  const [total, setTotal] = useState(0);
  const [status, setStatus] = useState<StatusFilter>('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [summary, setSummary] = useState<DailySalesSummaryDto | null>(null);

  const [detail, setDetail] = useState<RetailOrderDto | null>(null);
  const [receipt, setReceipt] = useState<ReceiptDto | null>(null);
  const [busy, setBusy] = useState(false);

  // Trả hàng
  const [showReturn, setShowReturn] = useState(false);
  const [returnQty, setReturnQty] = useState<Record<number, string>>({});
  const [returnReason, setReturnReason] = useState('');

  // Thu tiền đơn treo
  const [showPay, setShowPay] = useState(false);
  const [payMethod, setPayMethod] = useState('CASH');
  const [payCash, setPayCash] = useState('');

  const flash = (msg: string) => {
    setNotice(msg);
    setTimeout(() => setNotice(null), 4500);
  };

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [list, sum] = await Promise.all([
        listRetailOrders({ status: status || undefined, size: 50 }),
        dailySalesSummary().catch(() => null),
      ]);
      setOrders(list.content);
      setTotal(list.totalElements);
      setSummary(sum);
    } catch (err) {
      console.error('Không tải được đơn bán lẻ:', err);
      setError('Không tải được danh sách đơn bán lẻ. Kiểm tra kết nối rồi thử lại.');
      setOrders([]);
    } finally {
      setLoading(false);
    }
  }, [status]);

  useEffect(() => {
    load();
  }, [load]);

  const openDetail = async (id: number) => {
    try {
      const full = await getRetailOrder(id);
      setDetail(full);
    } catch {
      setError('Không tải được chi tiết đơn');
    }
  };

  const openReceipt = async (id: number) => {
    try {
      const rc = await getReceipt(id);
      setReceipt(rc);
    } catch {
      setError('Không tải được hoá đơn');
    }
  };

  const doCancel = async (id: number) => {
    if (!window.confirm('Huỷ đơn này? Hàng sẽ được nhập lại kho.')) return;
    setBusy(true);
    try {
      await cancelRetailOrder(id);
      flash('Đã huỷ đơn và hoàn lại kho');
      setDetail(null);
      await load();
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Không huỷ được đơn');
    } finally {
      setBusy(false);
    }
  };

  const doPay = async () => {
    if (!detail) return;
    setBusy(true);
    try {
      await payRetailOrder(detail.id, {
        paymentMethod: payMethod,
        cashReceived: payMethod === 'CASH' ? Number(payCash) || undefined : undefined,
      });
      flash('Đã thu tiền đơn');
      setShowPay(false);
      setDetail(null);
      setPayCash('');
      await load();
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Thu tiền thất bại');
    } finally {
      setBusy(false);
    }
  };

  const doReturn = async () => {
    if (!detail) return;
    const items = Object.entries(returnQty)
      .map(([id, qty]) => ({ orderItemId: Number(id), quantity: Number(qty) || 0 }))
      .filter((i) => i.quantity > 0);
    if (items.length === 0) {
      setError('Chọn số lượng trả cho ít nhất một mặt hàng');
      return;
    }
    if (returnReason.trim().length < 3) {
      setError('Nhập lý do trả hàng');
      return;
    }
    setBusy(true);
    try {
      const ret = await returnRetailOrder(detail.id, { items, reason: returnReason.trim() });
      flash(`Đã tạo đơn trả ${ret.orderReference} — ${formatCurrency(Number(ret.totalAmount))}`);
      setShowReturn(false);
      setReturnQty({});
      setReturnReason('');
      setDetail(null);
      await load();
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Trả hàng thất bại');
    } finally {
      setBusy(false);
    }
  };

  const returnTotal = useMemo(() => {
    if (!detail) return 0;
    return detail.items.reduce((sum, i) => {
      const qty = Number(returnQty[i.id]) || 0;
      return sum + (Number(i.unitPrice) || 0) * qty;
    }, 0);
  }, [detail, returnQty]);

  const productItems = detail?.items.filter((i) => i.itemType === 'PRODUCT') ?? [];

  return (
    <div className="min-h-screen bg-[#F5F5F0]">
      <header className="bg-white border-b border-[#DADCD3] px-4 sm:px-6 lg:px-8 py-4">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
          <div>
            <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[#526B00] mb-1">
              Bán hàng
            </p>
            <h1 className="text-xl sm:text-2xl font-black text-[#111310] tracking-tight">
              Đơn bán lẻ
            </h1>
            <p className="text-xs text-[#5E625A] mt-1">
              Xem lại, thu tiền, huỷ và trả hàng cho đơn bán tại quầy
            </p>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <Link
              href="/dashboard/pos"
              className="h-11 px-4 inline-flex items-center rounded-lg bg-[#111310] text-sm font-bold text-white hover:brightness-110"
            >
              Mở POS
            </Link>
            <Link
              href="/dashboard/retail"
              className="h-11 px-4 inline-flex items-center rounded-lg border border-[#DADCD3] bg-white text-sm font-semibold text-[#111310] hover:bg-[#F5F5F0]"
            >
              Hàng hoá &amp; kho
            </Link>
          </div>
        </div>
      </header>

      <div className="px-4 sm:px-6 lg:px-8 py-5 space-y-4">
        {/* KPI hôm nay */}
        {summary && (
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
            <KpiCard label="Doanh thu hôm nay" value={formatCurrency(Number(summary.grossRevenue))} tone="dark" />
            <KpiCard label="Đã trả hàng" value={formatCurrency(Number(summary.returnAmount))} />
            <KpiCard label="Lãi gộp" value={formatCurrency(Number(summary.grossProfit))} tone="accent" />
            <KpiCard
              label="Số đơn / mặt hàng"
              value={`${summary.orderCount} đơn · ${summary.itemCount} món`}
            />
          </div>
        )}

        {error && (
          <div role="alert" className="rounded-lg border border-[#E7B4B4] bg-[#FDECEC] px-3 py-2 text-xs font-medium text-[#8C1D1D]">
            {error}
            <button type="button" onClick={() => { setError(null); load(); }} className="ml-3 underline font-semibold">
              Thử lại
            </button>
          </div>
        )}
        {notice && (
          <div role="status" className="rounded-lg border border-[#C6D89A] bg-[#F3F9E1] px-3 py-2 text-xs font-medium text-[#3F4A12]">
            {notice}
          </div>
        )}

        {/* Lọc trạng thái */}
        <div className="flex gap-2 overflow-x-auto pb-1">
          {([
            ['', 'Tất cả'],
            ['PAID', 'Đã thanh toán'],
            ['PENDING', 'Chờ thanh toán'],
            ['CANCELLED', 'Đã huỷ'],
            ['REFUNDED', 'Đã trả hàng'],
          ] as [StatusFilter, string][]).map(([value, label]) => (
            <button
              key={value || 'all'}
              type="button"
              onClick={() => setStatus(value)}
              aria-pressed={status === value}
              className={`shrink-0 h-11 px-4 rounded-full border text-xs font-semibold transition-colors ${
                status === value
                  ? 'bg-[#111310] border-[#111310] text-white'
                  : 'bg-white border-[#DADCD3] text-[#111310] hover:bg-[#F5F5F0]'
              }`}
            >
              {label}
            </button>
          ))}
        </div>

        {/* Bảng đơn */}
        {loading ? (
          <div className="h-48 rounded-xl bg-white border border-[#DADCD3] animate-pulse" />
        ) : orders.length === 0 ? (
          <div className="rounded-xl border border-dashed border-[#DADCD3] bg-white px-6 py-14 text-center">
            <p className="text-sm font-semibold text-[#111310]">Chưa có đơn bán lẻ nào</p>
            <p className="mt-1 text-xs text-[#5E625A]">
              Vào <strong>Bán tại quầy (POS)</strong> để tạo đơn đầu tiên.
            </p>
          </div>
        ) : (
          <>
            <p className="text-xs text-[#5E625A]">{total} đơn</p>
            <div className="rounded-xl border border-[#DADCD3] bg-white overflow-x-auto">
              <table className="w-full min-w-[860px] text-sm">
                <thead>
                  <tr className="border-b border-[#DADCD3] bg-[#F9FAF6] text-left">
                    <th scope="col" className="px-4 py-3 text-[11px] font-bold uppercase tracking-wide text-[#5E625A]">Mã đơn</th>
                    <th scope="col" className="px-4 py-3 text-[11px] font-bold uppercase tracking-wide text-[#5E625A]">Khách</th>
                    <th scope="col" className="px-4 py-3 text-[11px] font-bold uppercase tracking-wide text-[#5E625A]">Hàng hoá</th>
                    <th scope="col" className="px-4 py-3 text-right text-[11px] font-bold uppercase tracking-wide text-[#5E625A]">Tổng tiền</th>
                    <th scope="col" className="px-4 py-3 text-center text-[11px] font-bold uppercase tracking-wide text-[#5E625A]">Trạng thái</th>
                    <th scope="col" className="px-4 py-3 text-right text-[11px] font-bold uppercase tracking-wide text-[#5E625A]">Thao tác</th>
                  </tr>
                </thead>
                <tbody>
                  {orders.map((o) => (
                    <tr key={o.id} className="border-b border-[#EDEEE8] last:border-0 hover:bg-[#F9FAF6]">
                      <td className="px-4 py-3">
                        <p className="font-mono text-xs font-bold text-[#111310]">{o.orderReference}</p>
                        <p className="text-[11px] text-[#5E625A]">
                          {new Date(o.createdAt).toLocaleString('vi-VN')}
                        </p>
                      </td>
                      <td className="px-4 py-3">
                        <p className="font-semibold text-[#111310] truncate">{o.customerName || '—'}</p>
                        {o.customerPhone && <p className="text-[11px] text-[#5E625A]">{o.customerPhone}</p>}
                      </td>
                      <td className="px-4 py-3 text-xs text-[#5E625A] max-w-[220px]">
                        <span className="line-clamp-2">
                          {o.items?.map((i) => `${i.itemName} ×${i.quantity}`).join(', ') || '—'}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-right font-bold text-[#111310]">
                        {formatCurrency(Number(o.totalAmount))}
                      </td>
                      <td className="px-4 py-3 text-center">
                        <span className={`inline-block px-2 py-1 rounded-full text-[11px] font-bold ${STATUS_PILL[o.status] || 'bg-[#F0F0EC] text-[#4A4E46]'}`}>
                          {STATUS_LABEL[o.status] || o.status}
                        </span>
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex items-center justify-end gap-1">
                          <button
                            type="button"
                            onClick={() => openDetail(o.id)}
                            className="h-11 px-3 rounded-md border border-[#DADCD3] text-xs font-semibold text-[#111310] hover:bg-[#F5F5F0]"
                          >
                            Chi tiết
                          </button>
                          <button
                            type="button"
                            onClick={() => openReceipt(o.id)}
                            className="h-11 px-3 rounded-md border border-[#DADCD3] text-xs font-semibold text-[#111310] hover:bg-[#F5F5F0]"
                          >
                            Hoá đơn
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}
      </div>

      {/* ======================= MODAL CHI TIẾT ĐƠN ======================= */}
      {detail && (
        <div className="fixed inset-0 z-50 flex items-start justify-center p-4 bg-black/40 overflow-y-auto" role="dialog" aria-modal="true" aria-labelledby="order-detail-title">
          <div className="w-full max-w-xl my-8 rounded-2xl bg-white p-5">
            <div className="flex items-start justify-between gap-3">
              <div>
                <h2 id="order-detail-title" className="text-base font-black text-[#111310]">
                  {detail.orderReference}
                </h2>
                <p className="mt-0.5 text-xs text-[#5E625A]">
                  {new Date(detail.createdAt).toLocaleString('vi-VN')} · {detail.customerName || 'Khách lẻ'}
                  {detail.customerPhone && ` · ${detail.customerPhone}`}
                </p>
              </div>
              <span className={`shrink-0 px-2 py-1 rounded-full text-[11px] font-bold ${STATUS_PILL[detail.status] || 'bg-[#F0F0EC]'}`}>
                {STATUS_LABEL[detail.status] || detail.status}
              </span>
            </div>

            <table className="mt-4 w-full text-xs">
              <thead>
                <tr className="border-b border-[#DADCD3] text-[#5E625A]">
                  <th scope="col" className="text-left py-1.5 font-semibold">Mặt hàng</th>
                  <th scope="col" className="text-center py-1.5 font-semibold w-12">SL</th>
                  <th scope="col" className="text-right py-1.5 font-semibold">Đơn giá</th>
                  <th scope="col" className="text-right py-1.5 font-semibold">T.Tiền</th>
                </tr>
              </thead>
              <tbody>
                {detail.items.map((i) => (
                  <tr key={i.id} className="border-b border-[#EDEEE8]">
                    <td className="py-2 text-[#111310]">
                      {i.itemName}
                      <span className="ml-1 text-[10px] uppercase text-[#5E625A]">{i.itemType}</span>
                    </td>
                    <td className="py-2 text-center text-[#111310]">{i.quantity}</td>
                    <td className="py-2 text-right text-[#5E625A]">{formatCurrency(Number(i.unitPrice))}</td>
                    <td className="py-2 text-right font-semibold text-[#111310]">{formatCurrency(Number(i.totalPrice))}</td>
                  </tr>
                ))}
              </tbody>
            </table>

            <div className="mt-3 space-y-1 text-xs">
              <Row label="Tạm tính" value={formatCurrency(Number(detail.subtotalAmount))} />
              {Number(detail.discountAmount) > 0 && (
                <Row label="Giảm giá" value={`−${formatCurrency(Number(detail.discountAmount))}`} />
              )}
              <Row label="VAT (đã gồm)" value={formatCurrency(Number(detail.taxAmount))} />
              <div className="flex justify-between text-sm font-black pt-1 border-t border-[#DADCD3]">
                <span className="text-[#111310]">Tổng cộng</span>
                <span className="text-[#111310]">{formatCurrency(Number(detail.totalAmount))}</span>
              </div>
              {detail.paymentMethod && (
                <Row
                  label="Thanh toán"
                  value={PAYMENT_METHODS.find((m) => m.value === detail.paymentMethod)?.label || detail.paymentMethod}
                />
              )}
            </div>

            <div className="mt-4 flex flex-wrap gap-2">
              <button
                type="button"
                onClick={() => { setDetail(null); openReceipt(detail.id); }}
                className="h-11 px-4 rounded-lg border border-[#DADCD3] text-sm font-semibold text-[#111310] hover:bg-[#F5F5F0]"
              >
                In hoá đơn
              </button>

              {detail.status === 'PENDING' && (
                <button
                  type="button"
                  onClick={() => { setPayMethod('CASH'); setPayCash(String(detail.totalAmount)); setShowPay(true); }}
                  className="h-11 px-4 rounded-lg bg-[#D7FF3F] text-sm font-bold text-[#111310]"
                >
                  Thu tiền
                </button>
              )}

              {detail.status === 'PENDING' && (
                <button
                  type="button"
                  onClick={() => doCancel(detail.id)}
                  disabled={busy}
                  className="h-11 px-4 rounded-lg border border-[#E7B4B4] text-sm font-semibold text-[#8C1D1D] hover:bg-[#FDECEC] disabled:opacity-40"
                >
                  Huỷ đơn
                </button>
              )}

              {detail.status === 'PAID' && productItems.length > 0 && (
                <button
                  type="button"
                  onClick={() => { setReturnQty({}); setReturnReason(''); setShowReturn(true); }}
                  className="h-11 px-4 rounded-lg border border-[#DADCD3] text-sm font-semibold text-[#111310] hover:bg-[#F5F5F0]"
                >
                  Trả hàng
                </button>
              )}

              <button
                type="button"
                onClick={() => setDetail(null)}
                className="ml-auto h-11 px-4 rounded-lg text-sm font-semibold text-[#5E625A] hover:bg-[#F5F5F0]"
              >
                Đóng
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ======================= MODAL THU TIỀN ======================= */}
      {showPay && detail && (
        <div className="fixed inset-0 z-[60] flex items-center justify-center p-4 bg-black/40" role="dialog" aria-modal="true" aria-labelledby="pay-title">
          <div className="w-full max-w-sm rounded-2xl bg-white p-5">
            <h2 id="pay-title" className="text-base font-black text-[#111310]">Thu tiền đơn {detail.orderReference}</h2>
            <p className="mt-1 text-sm text-[#5E625A]">
              Tổng cần thu: <strong className="text-[#111310]">{formatCurrency(Number(detail.totalAmount))}</strong>
            </p>
            <div className="mt-4 space-y-3">
              <div>
                <label htmlFor="pay-method" className="text-xs font-semibold text-[#111310]">Phương thức</label>
                <select
                  id="pay-method"
                  value={payMethod}
                  onChange={(e) => setPayMethod(e.target.value)}
                  className="mt-1 w-full h-11 px-3 rounded-lg border border-[#DADCD3] text-sm focus:outline-none focus:ring-2 focus:ring-[#526B00]"
                >
                  {PAYMENT_METHODS.map((m) => (
                    <option key={m.value} value={m.value}>{m.label}</option>
                  ))}
                </select>
              </div>
              {payMethod === 'CASH' && (
                <div>
                  <label htmlFor="pay-cash" className="text-xs font-semibold text-[#111310]">Tiền khách đưa</label>
                  <input
                    id="pay-cash"
                    type="number"
                    min={0}
                    value={payCash}
                    onChange={(e) => setPayCash(e.target.value)}
                    className="mt-1 w-full h-11 px-3 rounded-lg border border-[#DADCD3] text-sm focus:outline-none focus:ring-2 focus:ring-[#526B00]"
                  />
                  {Number(payCash) >= Number(detail.totalAmount) && (
                    <p className="mt-1 text-xs font-semibold text-[#3F4A12]">
                      Tiền thối: {formatCurrency(Number(payCash) - Number(detail.totalAmount))}
                    </p>
                  )}
                </div>
              )}
            </div>
            <div className="mt-5 flex gap-2">
              <button type="button" onClick={() => setShowPay(false)} className="flex-1 h-11 rounded-lg border border-[#DADCD3] text-sm font-semibold text-[#111310]">
                Huỷ
              </button>
              <button type="button" onClick={doPay} disabled={busy} className="flex-1 h-11 rounded-lg bg-[#111310] text-sm font-bold text-white disabled:opacity-40">
                {busy ? 'Đang thu…' : 'Xác nhận'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ======================= MODAL TRẢ HÀNG ======================= */}
      {showReturn && detail && (
        <div className="fixed inset-0 z-[60] flex items-start justify-center p-4 bg-black/40 overflow-y-auto" role="dialog" aria-modal="true" aria-labelledby="return-title">
          <div className="w-full max-w-lg my-8 rounded-2xl bg-white p-5">
            <h2 id="return-title" className="text-base font-black text-[#111310]">Trả hàng — {detail.orderReference}</h2>
            <p className="mt-1 text-xs text-[#5E625A]">
              Chỉ trả được hàng hoá (không trả gói tập/suất lớp). Hàng trả sẽ được nhập lại kho.
            </p>

            <div className="mt-4 space-y-2">
              {productItems.map((i) => (
                <div key={i.id} className="flex items-center gap-3 rounded-lg border border-[#DADCD3] p-3">
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-semibold text-[#111310] truncate">{i.itemName}</p>
                    <p className="text-[11px] text-[#5E625A]">
                      Đã mua {i.quantity} · {formatCurrency(Number(i.unitPrice))}
                    </p>
                  </div>
                  <label htmlFor={`rq-${i.id}`} className="sr-only">Số lượng trả {i.itemName}</label>
                  <input
                    id={`rq-${i.id}`}
                    type="number"
                    min={0}
                    max={i.quantity}
                    value={returnQty[i.id] ?? ''}
                    onChange={(e) =>
                      setReturnQty((s) => ({
                        ...s,
                        [i.id]: String(Math.min(Math.max(Number(e.target.value) || 0, 0), i.quantity)),
                      }))
                    }
                    placeholder="0"
                    className="w-20 h-11 px-2 rounded-lg border border-[#DADCD3] text-sm text-center focus:outline-none focus:ring-2 focus:ring-[#526B00]"
                  />
                </div>
              ))}
            </div>

            <div className="mt-3">
              <label htmlFor="return-reason" className="text-xs font-semibold text-[#111310]">Lý do trả hàng</label>
              <input
                id="return-reason"
                value={returnReason}
                onChange={(e) => setReturnReason(e.target.value)}
                placeholder="vd. Khách đổi ý / sản phẩm lỗi"
                className="mt-1 w-full h-11 px-3 rounded-lg border border-[#DADCD3] text-sm focus:outline-none focus:ring-2 focus:ring-[#526B00]"
              />
            </div>

            <p className="mt-3 text-sm font-bold text-[#111310]">
              Tiền hoàn dự kiến: {formatCurrency(returnTotal)}
            </p>

            <div className="mt-4 flex gap-2">
              <button type="button" onClick={() => setShowReturn(false)} className="flex-1 h-11 rounded-lg border border-[#DADCD3] text-sm font-semibold text-[#111310]">
                Huỷ
              </button>
              <button type="button" onClick={doReturn} disabled={busy} className="flex-1 h-11 rounded-lg bg-[#111310] text-sm font-bold text-white disabled:opacity-40">
                {busy ? 'Đang xử lý…' : 'Xác nhận trả'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ======================= MODAL HOÁ ĐƠN ======================= */}
      {receipt && (
        <div className="fixed inset-0 z-[60] flex items-center justify-center p-4 bg-black/40" role="dialog" aria-modal="true" aria-labelledby="rct-title">
          <div className="w-full max-w-md max-h-[90vh] overflow-y-auto rounded-2xl bg-white">
            <div id="receipt" className="p-6">
              <div className="text-center border-b border-dashed border-[#DADCD3] pb-3">
                <h2 id="rct-title" className="text-base font-black uppercase tracking-wide text-[#111310]">
                  {receipt.studioName}
                </h2>
                {receipt.studioAddress && <p className="text-xs text-[#5E625A] mt-0.5">{receipt.studioAddress}</p>}
                {receipt.studioPhone && <p className="text-xs text-[#5E625A]">ĐT: {receipt.studioPhone}</p>}
                <p className="mt-2 text-xs font-bold text-[#111310]">HOÁ ĐƠN BÁN HÀNG</p>
                <p className="text-xs text-[#5E625A]">Số: {receipt.orderReference}</p>
              </div>
              <div className="py-2 text-xs text-[#5E625A] space-y-0.5">
                <p>Khách: {receipt.customerName || '—'}</p>
                {receipt.customerPhone && <p>ĐT: {receipt.customerPhone}</p>}
                <p>Thu ngân: {receipt.cashierName || '—'}</p>
                <p>{new Date(receipt.issuedAt).toLocaleString('vi-VN')}</p>
              </div>
              <table className="w-full text-xs border-t border-dashed border-[#DADCD3]">
                <thead>
                  <tr className="text-[#5E625A]">
                    <th scope="col" className="text-left py-1.5 font-semibold">Mặt hàng</th>
                    <th scope="col" className="text-center py-1.5 font-semibold w-10">SL</th>
                    <th scope="col" className="text-right py-1.5 font-semibold">T.Tiền</th>
                  </tr>
                </thead>
                <tbody>
                  {receipt.items.map((i) => (
                    <tr key={i.id} className="border-t border-[#EDEEE8]">
                      <td className="py-1.5 text-[#111310]">{i.itemName}</td>
                      <td className="py-1.5 text-center text-[#111310]">{i.quantity}</td>
                      <td className="py-1.5 text-right text-[#111310]">{formatCurrency(Number(i.totalPrice))}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <div className="mt-3 pt-2 border-t border-dashed border-[#DADCD3] space-y-1 text-xs">
                <Row label="Tạm tính" value={formatCurrency(Number(receipt.subtotal))} dark />
                {Number(receipt.discount) > 0 && (
                  <Row label="Giảm giá" value={`−${formatCurrency(Number(receipt.discount))}`} dark />
                )}
                <Row label="VAT (đã gồm)" value={formatCurrency(Number(receipt.vat))} dark />
                <div className="flex justify-between text-sm font-black pt-1 border-t border-[#DADCD3]">
                  <span className="text-[#111310]">TỔNG CỘNG</span>
                  <span className="text-[#111310]">{formatCurrency(Number(receipt.total))}</span>
                </div>
              </div>
              <p className="mt-4 text-center text-[10px] text-[#5E625A]">Cảm ơn quý khách và hẹn gặp lại!</p>
            </div>
            <div className="flex gap-2 p-4 border-t border-[#DADCD3] bg-[#F9FAF6]">
              <button type="button" onClick={() => window.print()} className="flex-1 h-11 rounded-lg border border-[#DADCD3] bg-white text-sm font-semibold text-[#111310] hover:bg-[#F5F5F0]">
                In hoá đơn
              </button>
              <button type="button" onClick={() => setReceipt(null)} className="flex-1 h-11 rounded-lg bg-[#111310] text-sm font-bold text-white hover:brightness-110">
                Đóng
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function KpiCard({ label, value, tone }: { label: string; value: string; tone?: 'dark' | 'accent' }) {
  const base = 'rounded-xl border p-4';
  const style =
    tone === 'dark'
      ? 'bg-[#111310] border-[#111310] text-white'
      : tone === 'accent'
        ? 'bg-[#D7FF3F] border-[#C6EC33] text-[#111310]'
        : 'bg-white border-[#DADCD3] text-[#111310]';
  return (
    <div className={`${base} ${style}`}>
      <p className={`text-[11px] font-semibold uppercase tracking-[0.08em] ${tone === 'dark' ? 'text-[#A8ADA2]' : 'text-[#5E625A]'}`}>
        {label}
      </p>
      <p className="mt-1.5 text-lg font-black truncate">{value}</p>
    </div>
  );
}

function Row({ label, value, dark }: { label: string; value: string; dark?: boolean }) {
  return (
    <div className="flex justify-between">
      <span className="text-[#5E625A]">{label}</span>
      <span className={dark ? 'text-[#111310]' : 'text-[#111310]'}>{value}</span>
    </div>
  );
}
