'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import api from '@/lib/api';
import { formatCurrency } from '@/lib/contracts';
import type { ProductDto } from '@/lib/retailApi';

interface ReceiptLine { id?: number; productId: number; productName?: string; quantity: number; unitCost: number; lineTotal?: number }
interface Receipt {
  id: number; receiptNo: string; supplierName: string; note?: string | null;
  status: 'DRAFT' | 'CONFIRMED'; totalAmount: number; confirmedAt?: string | null;
  items: ReceiptLine[];
}
interface ReceiptPage { content: Receipt[]; totalElements: number }
interface EditLine { productId: string; quantity: string; unitCost: string }

const blankLine = (): EditLine => ({ productId: '', quantity: '1', unitCost: '' });

export default function PurchaseReceiptsPanel({ products, initialProductId, onConfirmed }: {
  products: ProductDto[]; initialProductId?: number; onConfirmed: () => Promise<void>;
}) {
  const [receipts, setReceipts] = useState<Receipt[]>([]);
  const [selected, setSelected] = useState<Receipt | null>(null);
  const [supplier, setSupplier] = useState('');
  const [note, setNote] = useState('');
  const [lines, setLines] = useState<EditLine[]>([blankLine()]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const load = useCallback(async () => {
    try {
      const response = await api.get<ReceiptPage>('/purchase-receipts', { params: { size: 50 } });
      setReceipts(response.data.content ?? []);
    } catch {
      setError('Không tải được phiếu nhập');
    }
  }, []);
  useEffect(() => { void load(); }, [load]);
  useEffect(() => {
    if (initialProductId && !selected) {
      const product = products.find((item) => item.id === initialProductId);
      setLines([{ productId: String(initialProductId), quantity: '1', unitCost: String(product?.costPrice ?? 0) }]);
    }
  }, [initialProductId, products, selected]);

  const total = useMemo(() => lines.reduce((sum, line) =>
    sum + (Number(line.quantity) || 0) * (Number(line.unitCost) || 0), 0), [lines]);

  const startNew = () => {
    setSelected(null); setSupplier(''); setNote(''); setLines([blankLine()]); setError(''); setNotice('');
  };

  const open = (receipt: Receipt) => {
    setSelected(receipt);
    setSupplier(receipt.supplierName);
    setNote(receipt.note ?? '');
    setLines(receipt.items.map((line) => ({
      productId: String(line.productId), quantity: String(line.quantity), unitCost: String(line.unitCost),
    })));
    setError(''); setNotice('');
  };

  const setLine = (index: number, patch: Partial<EditLine>) => {
    setLines((current) => current.map((line, position) => position === index ? { ...line, ...patch } : line));
  };

  const save = async () => {
    if (!supplier.trim()) { setError('Nhập tên nhà cung cấp'); return; }
    const items = lines.map((line) => ({
      productId: Number(line.productId), quantity: Number(line.quantity), unitCost: Number(line.unitCost),
    }));
    if (items.length === 0 || lines.some((line) => line.unitCost.trim() === '')
      || items.some((item) => !item.productId || !Number.isInteger(item.quantity)
      || item.quantity <= 0 || !Number.isFinite(item.unitCost) || item.unitCost < 0)
      || new Set(items.map((item) => item.productId)).size !== items.length) {
      setError('Chọn sản phẩm không trùng, số lượng nguyên dương và đơn giá nhập hợp lệ'); return;
    }
    setBusy(true); setError('');
    try {
      const payload = { supplierName: supplier.trim(), note: note.trim() || null, items };
      const response = selected
        ? await api.put<Receipt>(`/purchase-receipts/${selected.id}`, payload)
        : await api.post<Receipt>('/purchase-receipts', payload);
      setSelected(response.data);
      setNotice('Đã lưu phiếu nháp. Bấm Xác nhận để cập nhật tồn kho.');
      await load();
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Không lưu được phiếu nhập');
    } finally { setBusy(false); }
  };

  const confirm = async () => {
    if (!selected || selected.status !== 'DRAFT') return;
    setBusy(true); setError('');
    try {
      const response = await api.post<Receipt>(`/purchase-receipts/${selected.id}/confirm`);
      setSelected(response.data);
      setNotice('Đã xác nhận phiếu, cập nhật tồn kho và ghi chi phí nhập hàng.');
      await Promise.all([load(), onConfirmed()]);
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Không xác nhận được phiếu nhập');
    } finally { setBusy(false); }
  };

  const readonly = selected?.status === 'CONFIRMED';
  return <section aria-label="Phiếu nhập hàng" className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_minmax(0,1.6fr)]">
    <div className="rounded-xl border border-[#DADCD3] bg-white p-4">
      <div className="flex items-center justify-between gap-2">
        <h2 className="text-lg font-bold">Phiếu nhập hàng ({receipts.length})</h2>
        <button type="button" onClick={startNew} className="rounded-lg bg-[#D7FF3F] px-3 py-2 text-sm font-bold">+ Lập phiếu</button>
      </div>
      <div className="mt-3 max-h-[560px] space-y-2 overflow-auto">
        {receipts.map((receipt) => <button key={receipt.id} type="button" onClick={() => open(receipt)}
          className={`w-full rounded-lg border p-3 text-left text-sm ${selected?.id === receipt.id ? 'border-[#526B00] bg-[#F3F9E1]' : 'border-[#DADCD3]'}`}>
          <span className="block font-bold">{receipt.receiptNo}</span>
          <span className="block text-[#5E625A]">{receipt.supplierName} · {formatCurrency(Number(receipt.totalAmount))}</span>
          <span className="text-xs">{receipt.status === 'DRAFT' ? 'Nháp' : 'Đã xác nhận'}</span>
        </button>)}
      </div>
    </div>
    <div className="rounded-xl border border-[#DADCD3] bg-white p-4">
      <h2 className="text-lg font-bold">{selected ? selected.receiptNo : 'Lập phiếu nhập mới'}</h2>
      <p className="mt-1 text-sm text-[#5E625A]">Chỉ phiếu đã xác nhận mới tăng tồn và được tính vào tổng chi.</p>
      {error && <p role="alert" className="mt-3 rounded-lg bg-red-50 p-3 text-sm text-red-700">{error}</p>}
      {notice && <p role="status" className="mt-3 rounded-lg bg-lime-50 p-3 text-sm text-lime-800">{notice}</p>}
      <div className="mt-4 grid gap-3 sm:grid-cols-2">
        <label className="text-sm font-semibold">Nhà cung cấp
          <input value={supplier} onChange={(event) => setSupplier(event.target.value)} disabled={readonly}
            className="mt-1 block h-11 w-full rounded-lg border border-[#DADCD3] px-3" />
        </label>
        <label className="text-sm font-semibold">Ghi chú
          <input value={note} onChange={(event) => setNote(event.target.value)} disabled={readonly}
            className="mt-1 block h-11 w-full rounded-lg border border-[#DADCD3] px-3" />
        </label>
      </div>
      <div className="mt-4 space-y-3">
        {lines.map((line, index) => <div key={index} className="grid gap-2 rounded-lg border border-[#DADCD3] p-3 sm:grid-cols-[2fr_1fr_1fr_auto]">
          <label className="text-xs font-semibold">Sản phẩm
            <select value={line.productId} disabled={readonly} onChange={(event) => setLine(index, { productId: event.target.value })}
              className="mt-1 h-11 w-full rounded-lg border border-[#DADCD3] bg-white px-2">
              <option value="">Chọn sản phẩm</option>
              {products.filter((product) => product.trackInventory).map((product) =>
                <option key={product.id} value={product.id}>{product.name}</option>)}
            </select>
          </label>
          <label className="text-xs font-semibold">Số lượng
            <input type="number" min="1" step="1" value={line.quantity} disabled={readonly}
              onChange={(event) => setLine(index, { quantity: event.target.value })}
              className="mt-1 h-11 w-full rounded-lg border border-[#DADCD3] px-2" />
          </label>
          <label className="text-xs font-semibold">Đơn giá nhập
            <input type="number" min="0" step="0.01" value={line.unitCost} disabled={readonly}
              onChange={(event) => setLine(index, { unitCost: event.target.value })}
              className="mt-1 h-11 w-full rounded-lg border border-[#DADCD3] px-2" />
          </label>
          {!readonly && <button type="button" onClick={() => setLines((current) => current.filter((_, i) => i !== index))}
            aria-label={`Xóa dòng ${index + 1}`} className="self-end rounded-lg border border-[#DADCD3] px-3 py-3 text-sm">Xóa</button>}
        </div>)}
        {!readonly && <button type="button" onClick={() => setLines((current) => [...current, blankLine()])}
          className="rounded-lg border border-[#DADCD3] px-4 py-2 text-sm font-semibold">+ Thêm sản phẩm</button>}
      </div>
      <div className="mt-4 flex flex-wrap items-center justify-between gap-3 border-t border-[#DADCD3] pt-4">
        <strong>Tổng chi: {formatCurrency(total)}</strong>
        <div className="flex gap-2">
          {!readonly && <button type="button" disabled={busy} onClick={save}
            className="h-11 rounded-lg border border-[#DADCD3] px-4 font-semibold disabled:opacity-50">Lưu nháp</button>}
          {selected?.status === 'DRAFT' && <button type="button" disabled={busy} onClick={confirm}
            className="h-11 rounded-lg bg-[#D7FF3F] px-4 font-bold disabled:opacity-50">Xác nhận phiếu</button>}
        </div>
      </div>
    </div>
  </section>;
}
