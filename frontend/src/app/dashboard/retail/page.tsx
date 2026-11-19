'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import api from '@/lib/api';
import RoleGuard from '@/components/RoleGuard';
import PurchaseReceiptsPanel from '@/components/PurchaseReceiptsPanel';
import { formatCurrency } from '@/lib/contracts';
import { calculateStoredSellPrice, preVatPriceFromGross, type OriginalRetailPrice } from '@/lib/retailPrice';
import {
  CategoryDto,
  InventoryMovementDto,
  LowStockDto,
  ProductDto,
  ProductUnit,
  UNIT_LABELS,
  UNIT_OPTIONS,
  adjustStock,
  createCategory,
  createProduct,
  inventoryMovements,
  listCategories,
  lowStock,
  searchProducts,
  stockIn,
  toggleProduct,
  updateProduct,
} from '@/lib/retailApi';

type Tab = 'products' | 'categories' | 'stock-in' | 'purchase' | 'movements' | 'low-stock';

const TABS: { id: Tab; label: string }[] = [
  { id: 'products', label: 'Sản phẩm' },
  { id: 'categories', label: 'Danh mục' },
  { id: 'purchase', label: 'Phiếu nhập' },
  { id: 'movements', label: 'Lịch sử kho' },
  { id: 'low-stock', label: 'Tồn thấp' },
];

const retailCurrency = new Intl.NumberFormat('vi-VN', {
  style: 'currency',
  currency: 'VND',
  maximumFractionDigits: 2,
});

function normalizeProductSearch(value: string): string {
  return value.toLocaleLowerCase('vi-VN').normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd');
}

interface ProductForm {
  id?: number;
  sku: string;
  barcode: string;
  name: string;
  description: string;
  categoryId: string;
  unit: ProductUnit;
  costPrice: string;
  netSellPrice: string;
  vatRate: string;
  originalPrice?: OriginalRetailPrice;
  lowStockThreshold: string;
  trackInventory: boolean;
  isActive: boolean;
}

const emptyForm: ProductForm = {
  sku: '',
  barcode: '',
  name: '',
  description: '',
  categoryId: '',
  unit: 'CAI',
  costPrice: '',
  netSellPrice: '',
  vatRate: '0',
  lowStockThreshold: '5',
  trackInventory: true,
  isActive: true,
};

export default function RetailPage() {
  return (
    <RoleGuard allowedRoles={['MANAGER']}>
      <RetailAdmin />
    </RoleGuard>
  );
}

function RetailAdmin() {
  const [tab, setTab] = useState<Tab>('products');
  const [categories, setCategories] = useState<CategoryDto[]>([]);
  const [products, setProducts] = useState<ProductDto[]>([]);
  const [movements, setMovements] = useState<InventoryMovementDto[]>([]);
  const [low, setLow] = useState<LowStockDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const [search, setSearch] = useState('');
  const [filterCategory, setFilterCategory] = useState<string>('');
  const [minSellPrice, setMinSellPrice] = useState('');
  const [maxSellPrice, setMaxSellPrice] = useState('');
  const minPrice = minSellPrice.trim() === '' ? null : Number(minSellPrice);
  const maxPrice = maxSellPrice.trim() === '' ? null : Number(maxSellPrice);
  const priceError = (minPrice !== null && (!Number.isFinite(minPrice) || minPrice < 0))
    || (maxPrice !== null && (!Number.isFinite(maxPrice) || maxPrice < 0))
    || (minPrice !== null && maxPrice !== null && minPrice > maxPrice);
  const visibleProducts = useMemo(() => {
    const keyword = normalizeProductSearch(search.trim());
    return products.filter((product) =>
      (!filterCategory || product.categoryId === Number(filterCategory)) &&
      (!keyword || [product.name, product.sku, product.barcode ?? '']
        .some((value) => normalizeProductSearch(value).includes(keyword))) &&
      (priceError || minPrice === null || Number(product.sellPrice) >= minPrice) &&
      (priceError || maxPrice === null || Number(product.sellPrice) <= maxPrice));
  }, [products, search, filterCategory, minPrice, maxPrice, priceError]);

  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState<ProductForm>(emptyForm);
  const [saving, setSaving] = useState(false);
  const customerPrice = calculateStoredSellPrice(form.netSellPrice, form.vatRate, form.originalPrice);

  const [stockProduct, setStockProduct] = useState<ProductDto | null>(null);
  const [stockQty, setStockQty] = useState('');
  const [stockCost, setStockCost] = useState('');
  const [showAdjust, setShowAdjust] = useState(false);
  const [adjustQty, setAdjustQty] = useState('');
  const [adjustReason, setAdjustReason] = useState('');
  const [newCategory, setNewCategory] = useState('');

  const loadAll = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [cats, firstPage, moves, lowItems] = await Promise.all([
        listCategories(true),
        searchProducts({ activeOnly: false, size: 100, page: 0 }),
        inventoryMovements({ size: 50 }),
        lowStock(),
      ]);
      const allProducts = [...firstPage.content];
      for (let page = 1; page < firstPage.totalPages; page += 1) {
        const next = await searchProducts({ activeOnly: false, size: 100, page });
        allProducts.push(...next.content);
      }
      setCategories(cats);
      setProducts(allProducts);
      setMovements(moves.content);
      setLow(lowItems);
    } catch (err) {
      console.error('Không tải được dữ liệu bán lẻ:', err);
      setError('Không tải được dữ liệu hàng hoá. Kiểm tra kết nối rồi thử lại.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const timer = setTimeout(loadAll, 250);
    return () => clearTimeout(timer);
  }, [loadAll]);

  const flash = (msg: string) => {
    setNotice(msg);
    setTimeout(() => setNotice(null), 4000);
  };

  // ---- sản phẩm ------------------------------------------------------------
  const openCreate = () => {
    setForm(emptyForm);
    setShowForm(true);
  };

  const openEdit = (p: ProductDto) => {
    const netSellPrice = preVatPriceFromGross(Number(p.sellPrice), Number(p.vatRate) || 0);
    const vatRate = String(p.vatRate ?? 0);
    setForm({
      id: p.id,
      sku: p.sku,
      barcode: p.barcode || '',
      name: p.name,
      description: p.description || '',
      categoryId: p.categoryId ? String(p.categoryId) : '',
      unit: p.unit,
      costPrice: String(p.costPrice ?? ''),
      netSellPrice,
      vatRate,
      originalPrice: { gross: Number(p.sellPrice), net: netSellPrice, vat: vatRate },
      lowStockThreshold: String(p.lowStockThreshold ?? 5),
      trackInventory: p.trackInventory,
      isActive: p.isActive,
    });
    setShowForm(true);
  };

  const submitProduct = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.sku.trim() || !form.name.trim()) {
      setError('Nhập SKU và tên sản phẩm');
      return;
    }
    if (customerPrice === null || (form.isActive && customerPrice <= 0)) {
      setError('Nhập giá bán chưa VAT hợp lệ và thuế suất VAT từ 0 đến 100%');
      return;
    }
    const vatRate = Number(form.vatRate);
    if (!Number.isFinite(vatRate) || vatRate < 0 || vatRate > 100) {
      setError('VAT phải nằm trong khoảng từ 0 đến 100%');
      return;
    }
    setSaving(true);
    setError(null);
    const payload = {
      sku: form.sku.trim().toUpperCase(),
      barcode: form.barcode.trim() || null,
      name: form.name.trim(),
      description: form.description.trim() || null,
      categoryId: form.categoryId ? Number(form.categoryId) : null,
      unit: form.unit,
      costPrice: form.id ? Number(form.costPrice) || 0 : 0,
      sellPrice: customerPrice,
      vatRate,
      lowStockThreshold: Number(form.lowStockThreshold) || 0,
      trackInventory: form.trackInventory,
      isActive: form.isActive,
    };
    try {
      if (form.id) {
        await updateProduct(form.id, payload);
        flash(`Đã cập nhật "${payload.name}"`);
      } else {
        await createProduct(payload);
        flash(`Đã tạo "${payload.name}"`);
      }
      setShowForm(false);
      setForm(emptyForm);
      await loadAll();
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Không lưu được sản phẩm');
    } finally {
      setSaving(false);
    }
  };

  const doToggle = async (p: ProductDto) => {
    try {
      await toggleProduct(p.id);
      await loadAll();
      flash(`${p.isActive ? 'Đã ngừng bán' : 'Đã mở bán lại'} "${p.name}"`);
    } catch {
      setError('Không đổi được trạng thái sản phẩm');
    }
  };

  // ---- nhập kho ------------------------------------------------------------
  const openStockIn = (p: ProductDto) => {
    setStockProduct(p);
    setStockQty('');
    setStockCost(String(p.costPrice ?? ''));
    setTab('purchase');
  };

  const exportProducts = async () => {
    try {
      const response = await api.get('/retail/products/workbook', { responseType: 'blob' });
      const url = URL.createObjectURL(response.data);
      const link = document.createElement('a');
      link.href = url;
      link.download = 'fitos-products.xlsx';
      link.click();
      URL.revokeObjectURL(url);
    } catch { setError('Không xuất được Excel sản phẩm'); }
  };

  const importProducts = async (file: File) => {
    setSaving(true); setError(null);
    try {
      const body = new FormData();
      body.append('file', file);
      const response = await api.post<{ created: number; updated: number }>('/retail/products/workbook', body,
        { headers: { 'Content-Type': 'multipart/form-data' } });
      flash(`Excel: thêm ${response.data.created}, cập nhật ${response.data.updated} sản phẩm`);
      await loadAll();
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Không nhập được Excel sản phẩm');
    } finally { setSaving(false); }
  };

  const submitStockIn = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!stockProduct) return;
    const qty = Number(stockQty);
    if (!qty || qty <= 0) {
      setError('Số lượng nhập phải lớn hơn 0');
      return;
    }
    setSaving(true);
    try {
      await stockIn({
        productId: stockProduct.id,
        quantity: qty,
        unitCost: Number(stockCost) || 0,
        note: `Nhập kho ${stockProduct.name}`,
      });
      flash(`Đã nhập ${qty} ${UNIT_LABELS[stockProduct.unit] || ''} "${stockProduct.name}"`);
      setStockProduct(null);
      await loadAll();
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Nhập kho thất bại');
    } finally {
      setSaving(false);
    }
  };

  const submitAdjust = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!stockProduct) return;
    if (adjustReason.trim().length < 5) {
      setError('Lý do điều chỉnh phải có ít nhất 5 ký tự');
      return;
    }
    setSaving(true);
    try {
      await adjustStock({
        productId: stockProduct.id,
        newQuantity: Number(adjustQty) || 0,
        reason: adjustReason.trim(),
      });
      flash(`Đã điều chỉnh tồn "${stockProduct.name}"`);
      setShowAdjust(false);
      setStockProduct(null);
      setAdjustReason('');
      await loadAll();
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Điều chỉnh thất bại');
    } finally {
      setSaving(false);
    }
  };

  // ---- danh mục ------------------------------------------------------------
  const submitCategory = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newCategory.trim()) return;
    setSaving(true);
    try {
      await createCategory({ name: newCategory.trim() });
      setNewCategory('');
      await loadAll();
      flash('Đã thêm danh mục');
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Không thêm được danh mục');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#F5F5F0]">
      <header className="bg-white border-b border-[#DADCD3] px-4 sm:px-6 lg:px-8 py-4">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
          <div>
            <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[#526B00] mb-1">
              Bán hàng
            </p>
            <h1 className="text-xl sm:text-2xl font-black text-[#111310] tracking-tight">
              Hàng hoá &amp; tồn kho
            </h1>
          </div>
          <div className="flex items-center gap-2">
            <Link
              href="/dashboard/pos"
              className="h-11 px-4 inline-flex items-center rounded-lg bg-[#111310] text-sm font-bold text-white hover:brightness-110"
            >
              Mở POS
            </Link>
            <button
              type="button"
              onClick={() => { setStockProduct(null); setTab('purchase'); }}
              className="h-11 px-4 rounded-lg bg-[#D7FF3F] text-sm font-bold text-[#111310] hover:brightness-95"
            >
              Phiếu nhập hàng
            </button>
          </div>
        </div>

        <div className="mt-4 flex gap-1 overflow-x-auto border-b border-[#DADCD3] -mb-px" role="tablist">
          {TABS.map((t) => (
            <button
              key={t.id}
              type="button"
              role="tab"
              aria-selected={tab === t.id}
              onClick={() => { if (t.id === 'purchase') setStockProduct(null); setTab(t.id); }}
              className={`shrink-0 h-11 px-4 text-sm font-semibold border-b-2 transition-colors ${
                tab === t.id
                  ? 'border-[#111310] text-[#111310]'
                  : 'border-transparent text-[#5E625A] hover:text-[#111310]'
              }`}
            >
              {t.label}
              {t.id === 'low-stock' && low.length > 0 && (
                <span className="ml-1.5 inline-flex items-center justify-center min-w-[18px] h-[18px] px-1 rounded-full bg-[#8C1D1D] text-white text-[10px] font-bold">
                  {low.length}
                </span>
              )}
            </button>
          ))}
        </div>
      </header>

      <div className="px-4 sm:px-6 lg:px-8 py-5 space-y-4">
        {error && (
          <div role="alert" className="rounded-lg border border-[#E7B4B4] bg-[#FDECEC] px-3 py-2 text-xs font-medium text-[#8C1D1D]">
            {error}
            <button type="button" onClick={() => setError(null)} className="ml-3 underline font-semibold">
              Đóng
            </button>
          </div>
        )}
        {notice && (
          <div role="status" className="rounded-lg border border-[#C6D89A] bg-[#F3F9E1] px-3 py-2 text-xs font-medium text-[#3F4A12]">
            {notice}
          </div>
        )}

        {/* ================================ SẢN PHẨM ================================ */}
        {tab === 'products' && (
          <section aria-label="Danh sách sản phẩm">
            <div className="mb-3 flex flex-wrap items-center gap-2">
              <input
                type="search"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Tìm theo tên, SKU, mã vạch"
                aria-label="Tìm sản phẩm"
                className="h-11 min-w-[220px] flex-[2] rounded-lg border border-[#DADCD3] bg-white px-3 text-sm focus:outline-none focus:ring-2 focus:ring-[#526B00]"
              />
              <select
                value={filterCategory}
                onChange={(e) => setFilterCategory(e.target.value)}
                aria-label="Lọc theo danh mục"
                className="h-11 px-3 rounded-lg border border-[#DADCD3] bg-white text-sm focus:outline-none focus:ring-2 focus:ring-[#526B00]"
              >
                <option value="">Tất cả danh mục</option>
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>{c.name}</option>
                ))}
              </select>
              <div className="flex flex-wrap items-center gap-2" role="group" aria-label="Khoảng giá bán đã gồm VAT">
                <input type="number" min="0" step="1000" inputMode="numeric" value={minSellPrice}
                  onChange={(event) => setMinSellPrice(event.target.value)} aria-label="Giá bán từ"
                  placeholder="Giá từ" aria-invalid={priceError}
                  className="h-11 w-28 rounded-lg border border-[#DADCD3] bg-white px-3 text-sm focus:outline-none focus:ring-2 focus:ring-[#526B00]" />
                <span aria-hidden="true" className="text-sm text-[#5E625A]">–</span>
                <input type="number" min="0" step="1000" inputMode="numeric" value={maxSellPrice}
                  onChange={(event) => setMaxSellPrice(event.target.value)} aria-label="Giá bán đến"
                  placeholder="Giá đến" aria-invalid={priceError}
                  className="h-11 w-28 rounded-lg border border-[#DADCD3] bg-white px-3 text-sm focus:outline-none focus:ring-2 focus:ring-[#526B00]" />
              </div>
              <button type="button" onClick={exportProducts}
                className="h-11 rounded-lg border border-[#DADCD3] bg-white px-4 text-sm font-semibold">Xuất Excel</button>
              <label className="inline-flex h-11 cursor-pointer items-center rounded-lg border border-[#DADCD3] bg-white px-4 text-sm font-semibold">
                Nhập Excel
                <input type="file" accept=".xlsx" className="sr-only" disabled={saving}
                  onChange={(event) => { const file = event.target.files?.[0]; if (file) void importProducts(file); event.target.value = ''; }} />
              </label>
              <button type="button" onClick={openCreate}
                className="h-11 rounded-lg bg-[#D7FF3F] px-4 text-sm font-bold text-[#111310] hover:brightness-95">
                + Thêm sản phẩm
              </button>
            </div>

            {priceError && <p role="alert" className="mb-3 text-sm text-red-700">Khoảng giá bán không hợp lệ. Nhập số từ 0 trở lên và đảm bảo Giá từ không lớn hơn Giá đến.</p>}

            {loading ? (
              <div className="h-40 rounded-xl bg-white border border-[#DADCD3] animate-pulse" />
            ) : visibleProducts.length === 0 ? (
              <div className="rounded-xl border border-dashed border-[#DADCD3] bg-white px-6 py-12 text-center">
                <p className="text-sm font-semibold text-[#111310]">{products.length ? 'Không tìm thấy sản phẩm phù hợp' : 'Chưa có sản phẩm nào'}</p>
                {!products.length && <p className="mt-1 text-xs text-[#5E625A]">Bấm “Thêm sản phẩm” để tạo mặt hàng đầu tiên.</p>}
              </div>
            ) : (
              <div className="rounded-xl border border-[#DADCD3] bg-white overflow-x-auto">
                <table className="w-full min-w-[820px] text-sm">
                  <thead>
                    <tr className="border-b border-[#DADCD3] bg-[#F9FAF6] text-left">
                      <th scope="col" className="px-4 py-3 text-[11px] font-bold uppercase tracking-wide text-[#5E625A]">Sản phẩm</th>
                      <th scope="col" className="px-4 py-3 text-[11px] font-bold uppercase tracking-wide text-[#5E625A]">Danh mục</th>
                      <th scope="col" className="px-4 py-3 text-right text-[11px] font-bold uppercase tracking-wide text-[#5E625A]">Giá vốn</th>
                      <th scope="col" className="px-4 py-3 text-right text-[11px] font-bold uppercase tracking-wide text-[#5E625A]">Giá bán</th>
                      <th scope="col" className="px-4 py-3 text-center text-[11px] font-bold uppercase tracking-wide text-[#5E625A]">Tồn</th>
                      <th scope="col" className="px-4 py-3 text-right text-[11px] font-bold uppercase tracking-wide text-[#5E625A]">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody>
                    {visibleProducts.map((p) => (
                      <tr key={p.id} className="border-b border-[#EDEEE8] last:border-0 hover:bg-[#F9FAF6]">
                        <td className="px-4 py-3">
                          <p className="font-semibold text-[#111310]">{p.name}</p>
                          <p className="text-xs text-[#5E625A]">
                            {p.sku}
                            {p.barcode && ` · ${p.barcode}`}
                          </p>
                          {!p.isActive && (
                            <span className="inline-block mt-1 text-[10px] font-bold uppercase tracking-wide text-[#8C1D1D] bg-[#FDECEC] px-1.5 py-0.5 rounded">
                              Ngừng bán
                            </span>
                          )}
                        </td>
                        <td className="px-4 py-3 text-[#5E625A]">{p.categoryName || '—'}</td>
                        <td className="px-4 py-3 text-right text-[#5E625A]">{formatCurrency(Number(p.costPrice))}</td>
                        <td className="px-4 py-3 text-right font-semibold text-[#111310]">
                          {formatCurrency(Number(p.sellPrice))}
                        </td>
                        <td className="px-4 py-3 text-center">
                          <span
                            className={`inline-block min-w-[52px] px-2 py-1 rounded-full text-xs font-bold ${
                              p.outOfStock
                                ? 'bg-[#FDECEC] text-[#8C1D1D]'
                                : p.lowStock
                                  ? 'bg-[#FDF6E7] text-[#8A5B00]'
                                  : 'bg-[#F3F9E1] text-[#3F4A12]'
                            }`}
                          >
                            {p.trackInventory ? `${p.stockQuantity} ${UNIT_LABELS[p.unit] || ''}` : '—'}
                          </span>
                        </td>
                        <td className="px-4 py-3">
                          <div className="flex items-center justify-end gap-1">
                            <button
                              type="button"
                              onClick={() => openStockIn(p)}
                              className="h-11 px-3 rounded-md border border-[#DADCD3] text-xs font-semibold text-[#111310] hover:bg-[#F5F5F0]"
                            >
                              Nhập kho
                            </button>
                            <button
                              type="button"
                              onClick={() => openEdit(p)}
                              className="h-11 px-3 rounded-md border border-[#DADCD3] text-xs font-semibold text-[#111310] hover:bg-[#F5F5F0]"
                            >
                              Sửa
                            </button>
                            <button
                              type="button"
                              onClick={() => doToggle(p)}
                              className="h-11 px-3 rounded-md text-xs font-semibold text-[#5E625A] hover:bg-[#F5F5F0]"
                            >
                              {p.isActive ? 'Ngừng' : 'Mở'}
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        )}

        {/* ================================ DANH MỤC ================================ */}
        {tab === 'categories' && (
          <section aria-label="Danh mục sản phẩm" className="space-y-4">
            <form onSubmit={submitCategory} className="flex gap-2">
              <label htmlFor="new-cat" className="sr-only">Tên danh mục mới</label>
              <input
                id="new-cat"
                value={newCategory}
                onChange={(e) => setNewCategory(e.target.value)}
                placeholder="Tên danh mục mới (vd. Thực phẩm bổ sung)"
                className="flex-1 h-11 px-3 rounded-lg border border-[#DADCD3] bg-white text-sm focus:outline-none focus:ring-2 focus:ring-[#526B00]"
              />
              <button
                type="submit"
                disabled={saving || !newCategory.trim()}
                className="h-11 px-4 rounded-lg bg-[#111310] text-sm font-bold text-white disabled:opacity-40"
              >
                Thêm
              </button>
            </form>

            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
              {categories.map((c) => (
                <div key={c.id} className="rounded-xl border border-[#DADCD3] bg-white p-4">
                  <p className="font-bold text-[#111310]">{c.name}</p>
                  {c.description && <p className="mt-1 text-xs text-[#5E625A]">{c.description}</p>}
                  <p className="mt-2 text-xs text-[#5E625A]">{c.productCount} sản phẩm</p>
                  {!c.isActive && (
                    <span className="mt-2 inline-block text-[10px] font-bold uppercase text-[#8C1D1D] bg-[#FDECEC] px-1.5 py-0.5 rounded">
                      Đang ẩn
                    </span>
                  )}
                </div>
              ))}
            </div>
          </section>
        )}

        {/* ================================ NHẬP KHO ================================ */}
        {tab === 'stock-in' && (
          <section aria-label="Nhập kho" className="space-y-4">
            <form onSubmit={submitStockIn} className="rounded-xl border border-[#DADCD3] bg-white p-4 space-y-3">
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <div className="sm:col-span-1">
                  <label htmlFor="si-product" className="text-xs font-semibold text-[#111310]">Sản phẩm</label>
                  <select
                    id="si-product"
                    value={stockProduct?.id ?? ''}
                    onChange={(e) => {
                      const p = products.find((x) => x.id === Number(e.target.value)) || null;
                      setStockProduct(p);
                      if (p) setStockCost(String(p.costPrice ?? ''));
                    }}
                    className="mt-1 w-full h-11 px-3 rounded-lg border border-[#DADCD3] bg-white text-sm focus:outline-none focus:ring-2 focus:ring-[#526B00]"
                  >
                    <option value="">— Chọn sản phẩm —</option>
                    {products.map((p) => (
                      <option key={p.id} value={p.id}>{p.name} (còn {p.stockQuantity})</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label htmlFor="si-qty" className="text-xs font-semibold text-[#111310]">Số lượng nhập</label>
                  <input
                    id="si-qty"
                    type="number"
                    min={1}
                    value={stockQty}
                    onChange={(e) => setStockQty(e.target.value)}
                    className="mt-1 w-full h-11 px-3 rounded-lg border border-[#DADCD3] bg-white text-sm focus:outline-none focus:ring-2 focus:ring-[#526B00]"
                  />
                </div>
                <div>
                  <label htmlFor="si-cost" className="text-xs font-semibold text-[#111310]">Giá nhập / đơn vị</label>
                  <input
                    id="si-cost"
                    type="number"
                    min={0}
                    value={stockCost}
                    onChange={(e) => setStockCost(e.target.value)}
                    className="mt-1 w-full h-11 px-3 rounded-lg border border-[#DADCD3] bg-white text-sm focus:outline-none focus:ring-2 focus:ring-[#526B00]"
                  />
                </div>
              </div>
              <div className="flex flex-wrap gap-2">
                <button
                  type="submit"
                  disabled={saving || !stockProduct}
                  className="h-11 px-5 rounded-lg bg-[#D7FF3F] text-sm font-bold text-[#111310] disabled:opacity-40"
                >
                  Nhập kho
                </button>
                {stockProduct && (
                  <button
                    type="button"
                    onClick={() => {
                      setAdjustQty(String(stockProduct.stockQuantity));
                      setShowAdjust(true);
                    }}
                    className="h-11 px-4 rounded-lg border border-[#DADCD3] text-sm font-semibold text-[#111310] hover:bg-[#F5F5F0]"
                  >
                    Điều chỉnh tồn
                  </button>
                )}
              </div>
              {stockProduct && (
                <p className="text-xs text-[#5E625A]">
                  Tồn hiện tại: <strong className="text-[#111310]">{stockProduct.stockQuantity}</strong>{' '}
                  {UNIT_LABELS[stockProduct.unit] || ''} · Giá vốn:{' '}
                  <strong className="text-[#111310]">{formatCurrency(Number(stockProduct.costPrice))}</strong>
                </p>
              )}
            </form>
          </section>
        )}

        {tab === 'purchase' && <PurchaseReceiptsPanel products={products}
          initialProductId={stockProduct?.id}
          onConfirmed={async () => { setStockProduct(null); await loadAll(); }} />}

        {/* ================================ LỊCH SỬ KHO ================================ */}
        {tab === 'movements' && (
          <section aria-label="Lịch sử biến động kho">
            {movements.length === 0 ? (
              <div className="rounded-xl border border-dashed border-[#DADCD3] bg-white px-6 py-12 text-center">
                <p className="text-sm font-semibold text-[#111310]">Chưa có biến động kho</p>
              </div>
            ) : (
              <div className="rounded-xl border border-[#DADCD3] bg-white overflow-x-auto">
                <table className="w-full min-w-[760px] text-sm">
                  <thead>
                    <tr className="border-b border-[#DADCD3] bg-[#F9FAF6] text-left">
                      <th scope="col" className="px-4 py-3 text-[11px] font-bold uppercase tracking-wide text-[#5E625A]">Thời gian</th>
                      <th scope="col" className="px-4 py-3 text-[11px] font-bold uppercase tracking-wide text-[#5E625A]">Loại</th>
                      <th scope="col" className="px-4 py-3 text-[11px] font-bold uppercase tracking-wide text-[#5E625A]">Sản phẩm</th>
                      <th scope="col" className="px-4 py-3 text-center text-[11px] font-bold uppercase tracking-wide text-[#5E625A]">Δ</th>
                      <th scope="col" className="px-4 py-3 text-center text-[11px] font-bold uppercase tracking-wide text-[#5E625A]">Trước → Sau</th>
                      <th scope="col" className="px-4 py-3 text-[11px] font-bold uppercase tracking-wide text-[#5E625A]">Ghi chú</th>
                    </tr>
                  </thead>
                  <tbody>
                    {movements.map((m) => (
                      <tr key={m.id} className="border-b border-[#EDEEE8] last:border-0">
                        <td className="px-4 py-3 text-xs text-[#5E625A] whitespace-nowrap">
                          {new Date(m.createdAt).toLocaleString('vi-VN')}
                        </td>
                        <td className="px-4 py-3">
                          <MovementBadge type={m.transactionType} />
                        </td>
                        <td className="px-4 py-3 font-semibold text-[#111310]">{m.productName || '—'}</td>
                        <td className={`px-4 py-3 text-center font-bold ${m.quantityDelta > 0 ? 'text-[#3F4A12]' : 'text-[#8C1D1D]'}`}>
                          {m.quantityDelta > 0 ? `+${m.quantityDelta}` : m.quantityDelta}
                        </td>
                        <td className="px-4 py-3 text-center text-xs text-[#5E625A]">
                          {m.stockBefore} → {m.stockAfter}
                        </td>
                        <td className="px-4 py-3 text-xs text-[#5E625A] max-w-[280px] truncate" title={m.note || ''}>
                          {m.note || '—'}
                          {m.orderReference && <span className="ml-1 font-semibold">({m.orderReference})</span>}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        )}

        {/* ================================ TỒN THẤP ================================ */}
        {tab === 'low-stock' && (
          <section aria-label="Mặt hàng tồn thấp">
            {low.length === 0 ? (
              <div className="rounded-xl border border-dashed border-[#DADCD3] bg-white px-6 py-12 text-center">
                <p className="text-sm font-semibold text-[#111310]">Tất cả mặt hàng đều đủ tồn</p>
                <p className="mt-1 text-xs text-[#5E625A]">Không có sản phẩm nào dưới ngưỡng cảnh báo.</p>
              </div>
            ) : (
              <div className="rounded-xl border border-[#DADCD3] bg-white overflow-x-auto">
                <table className="w-full min-w-[640px] text-sm">
                  <thead>
                    <tr className="border-b border-[#DADCD3] bg-[#F9FAF6] text-left">
                      <th scope="col" className="px-4 py-3 text-[11px] font-bold uppercase tracking-wide text-[#5E625A]">Sản phẩm</th>
                      <th scope="col" className="px-4 py-3 text-center text-[11px] font-bold uppercase tracking-wide text-[#5E625A]">Tồn</th>
                      <th scope="col" className="px-4 py-3 text-center text-[11px] font-bold uppercase tracking-wide text-[#5E625A]">Ngưỡng</th>
                      <th scope="col" className="px-4 py-3 text-center text-[11px] font-bold uppercase tracking-wide text-[#5E625A]">Mức độ</th>
                      <th scope="col" className="px-4 py-3 text-right text-[11px] font-bold uppercase tracking-wide text-[#5E625A]">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody>
                    {low.map((i) => (
                      <tr key={i.id} className="border-b border-[#EDEEE8] last:border-0">
                        <td className="px-4 py-3">
                          <p className="font-semibold text-[#111310]">{i.name}</p>
                          <p className="text-xs text-[#5E625A]">{i.sku}{i.categoryName && ` · ${i.categoryName}`}</p>
                        </td>
                        <td className="px-4 py-3 text-center font-bold text-[#111310]">
                          {i.stockQuantity} {UNIT_LABELS[i.unit] || ''}
                        </td>
                        <td className="px-4 py-3 text-center text-[#5E625A]">{i.lowStockThreshold}</td>
                        <td className="px-4 py-3 text-center">
                          <span
                            className={`inline-block px-2 py-1 rounded-full text-[11px] font-bold ${
                              i.severity === 'OUT_OF_STOCK'
                                ? 'bg-[#FDECEC] text-[#8C1D1D]'
                                : 'bg-[#FDF6E7] text-[#8A5B00]'
                            }`}
                          >
                            {i.severity === 'OUT_OF_STOCK' ? 'Đã hết' : 'Sắp hết'}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-right">
                          <button
                            type="button"
                            onClick={() => {
                              const p = products.find((x) => x.id === i.id);
                              if (p) openStockIn(p);
                            }}
                            className="h-11 px-3 rounded-md border border-[#DADCD3] text-xs font-semibold text-[#111310] hover:bg-[#F5F5F0]"
                          >
                            Nhập thêm
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        )}
      </div>

      {/* Modal sản phẩm */}
      {showForm && (
        <div className="fixed inset-0 z-50 flex items-start justify-center p-4 bg-black/40 overflow-y-auto" role="dialog" aria-modal="true" aria-labelledby="product-form-title">
          <form onSubmit={submitProduct} className="w-full max-w-lg my-8 rounded-2xl bg-white p-5">
            <h2 id="product-form-title" className="text-base font-black text-[#111310]">
              {form.id ? 'Sửa sản phẩm' : 'Thêm sản phẩm'}
            </h2>

            <div className="mt-4 grid grid-cols-1 sm:grid-cols-2 gap-3">
              <Field label="SKU" htmlFor="pf-sku" required>
                <input
                  id="pf-sku"
                  value={form.sku}
                  onChange={(e) => setForm((s) => ({ ...s, sku: e.target.value }))}
                  placeholder="WHEY-CONC-2K"
                  className="mt-1 w-full h-11 px-3 rounded-lg border border-[#DADCD3] text-sm focus:outline-none focus:ring-2 focus:ring-[#526B00]"
                />
              </Field>
              <Field label="Mã vạch" htmlFor="pf-barcode">
                <input
                  id="pf-barcode"
                  value={form.barcode}
                  onChange={(e) => setForm((s) => ({ ...s, barcode: e.target.value }))}
                  placeholder="8935001100011"
                  className="mt-1 w-full h-11 px-3 rounded-lg border border-[#DADCD3] text-sm focus:outline-none focus:ring-2 focus:ring-[#526B00]"
                />
              </Field>
              <div className="sm:col-span-2">
                <Field label="Tên sản phẩm" htmlFor="pf-name" required>
                  <input
                    id="pf-name"
                    value={form.name}
                    onChange={(e) => setForm((s) => ({ ...s, name: e.target.value }))}
                    className="mt-1 w-full h-11 px-3 rounded-lg border border-[#DADCD3] text-sm focus:outline-none focus:ring-2 focus:ring-[#526B00]"
                  />
                </Field>
              </div>
              <Field label="Danh mục" htmlFor="pf-category">
                <select
                  id="pf-category"
                  value={form.categoryId}
                  onChange={(e) => setForm((s) => ({ ...s, categoryId: e.target.value }))}
                  className="mt-1 w-full h-11 px-3 rounded-lg border border-[#DADCD3] text-sm focus:outline-none focus:ring-2 focus:ring-[#526B00]"
                >
                  <option value="">— Không chọn —</option>
                  {categories.map((c) => (
                    <option key={c.id} value={c.id}>{c.name}</option>
                  ))}
                </select>
              </Field>
              <Field label="Đơn vị" htmlFor="pf-unit">
                <select
                  id="pf-unit"
                  value={form.unit}
                  onChange={(e) => setForm((s) => ({ ...s, unit: e.target.value as ProductUnit }))}
                  className="mt-1 w-full h-11 px-3 rounded-lg border border-[#DADCD3] text-sm focus:outline-none focus:ring-2 focus:ring-[#526B00]"
                >
                  {UNIT_OPTIONS.map((u) => (
                    <option key={u.value} value={u.value}>{u.label}</option>
                  ))}
                </select>
              </Field>
              {form.id && <Field label="Giá vốn" htmlFor="pf-cost">
                <input
                  id="pf-cost"
                  type="number"
                  min={0}
                  value={form.costPrice}
                  onChange={(e) => setForm((s) => ({ ...s, costPrice: e.target.value }))}
                  className="mt-1 w-full h-11 px-3 rounded-lg border border-[#DADCD3] text-sm focus:outline-none focus:ring-2 focus:ring-[#526B00]"
                />
              </Field>}
              <Field label="Giá bán (chưa VAT)" htmlFor="pf-sell" required>
                <input
                  id="pf-sell"
                  type="number"
                  min={0}
                  step="0.01"
                  value={form.netSellPrice}
                  onChange={(e) => setForm((s) => ({ ...s, netSellPrice: e.target.value }))}
                  className="mt-1 w-full h-11 px-3 rounded-lg border border-[#DADCD3] text-sm focus:outline-none focus:ring-2 focus:ring-[#526B00]"
                />
              </Field>
              <Field label="VAT (%)" htmlFor="pf-vat">
                <input
                  id="pf-vat"
                  type="number"
                  min={0}
                  max={100}
                  step="0.01"
                  value={form.vatRate}
                  onChange={(e) => setForm((s) => ({ ...s, vatRate: e.target.value }))}
                  className="mt-1 w-full h-11 px-3 rounded-lg border border-[#DADCD3] text-sm focus:outline-none focus:ring-2 focus:ring-[#526B00]"
                />
              </Field>
              <div className="sm:col-span-2 rounded-lg border border-[#DADCD3] bg-[#F5F5F0] px-4 py-3" role="status" aria-live="polite">
                <div className="flex items-center justify-between gap-3">
                  <span className="text-sm font-semibold text-[#5E625A]">Giá bán đã gồm VAT</span>
                  <strong className="text-base text-[#111310]">
                    {customerPrice === null
                      ? '—'
                      : retailCurrency.format(customerPrice)}
                  </strong>
                </div>
              </div>
              <Field label="Ngưỡng cảnh báo tồn" htmlFor="pf-threshold">
                <input
                  id="pf-threshold"
                  type="number"
                  min={0}
                  value={form.lowStockThreshold}
                  onChange={(e) => setForm((s) => ({ ...s, lowStockThreshold: e.target.value }))}
                  className="mt-1 w-full h-11 px-3 rounded-lg border border-[#DADCD3] text-sm focus:outline-none focus:ring-2 focus:ring-[#526B00]"
                />
              </Field>
              {!form.id && <p className="self-center text-xs text-[#5E625A]">
                Giá vốn và tồn kho được ghi nhận khi xác nhận phiếu nhập.
              </p>}
            </div>

            <div className="mt-4 flex flex-wrap gap-4">
              <label className="inline-flex items-center gap-2 text-xs font-semibold text-[#111310]">
                <input
                  type="checkbox"
                  checked={form.trackInventory}
                  onChange={(e) => setForm((s) => ({ ...s, trackInventory: e.target.checked }))}
                  className="h-4 w-4"
                />
                Quản lý tồn kho
              </label>
              <label className="inline-flex items-center gap-2 text-xs font-semibold text-[#111310]">
                <input
                  type="checkbox"
                  checked={form.isActive}
                  onChange={(e) => setForm((s) => ({ ...s, isActive: e.target.checked }))}
                  className="h-4 w-4"
                />
                Đang bán
              </label>
            </div>

            <div className="mt-5 flex gap-2">
              <button
                type="button"
                onClick={() => setShowForm(false)}
                className="flex-1 h-11 rounded-lg border border-[#DADCD3] text-sm font-semibold text-[#111310] hover:bg-[#F5F5F0]"
              >
                Huỷ
              </button>
              <button
                type="submit"
                disabled={saving}
                className="flex-1 h-11 rounded-lg bg-[#111310] text-sm font-bold text-white disabled:opacity-40"
              >
                {saving ? 'Đang lưu…' : 'Lưu'}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Modal điều chỉnh tồn */}
      {showAdjust && stockProduct && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40" role="dialog" aria-modal="true" aria-labelledby="adjust-title">
          <form onSubmit={submitAdjust} className="w-full max-w-sm rounded-2xl bg-white p-5">
            <h2 id="adjust-title" className="text-base font-black text-[#111310]">Điều chỉnh tồn kho</h2>
            <p className="mt-1 text-xs text-[#5E625A]">{stockProduct.name}</p>
            <div className="mt-4 space-y-3">
              <Field label={`Tồn thực tế (hiện tại ${stockProduct.stockQuantity})`} htmlFor="adj-qty" required>
                <input
                  id="adj-qty"
                  type="number"
                  min={0}
                  value={adjustQty}
                  onChange={(e) => setAdjustQty(e.target.value)}
                  className="mt-1 w-full h-11 px-3 rounded-lg border border-[#DADCD3] text-sm focus:outline-none focus:ring-2 focus:ring-[#526B00]"
                />
              </Field>
              <Field label="Lý do điều chỉnh" htmlFor="adj-reason" required>
                <input
                  id="adj-reason"
                  value={adjustReason}
                  onChange={(e) => setAdjustReason(e.target.value)}
                  placeholder="vd. Kiểm kê phát hiện hư hỏng"
                  className="mt-1 w-full h-11 px-3 rounded-lg border border-[#DADCD3] text-sm focus:outline-none focus:ring-2 focus:ring-[#526B00]"
                />
              </Field>
            </div>
            <div className="mt-5 flex gap-2">
              <button
                type="button"
                onClick={() => { setShowAdjust(false); setStockProduct(null); }}
                className="flex-1 h-11 rounded-lg border border-[#DADCD3] text-sm font-semibold text-[#111310]"
              >
                Huỷ
              </button>
              <button
                type="submit"
                disabled={saving}
                className="flex-1 h-11 rounded-lg bg-[#111310] text-sm font-bold text-white disabled:opacity-40"
              >
                Xác nhận
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Modal nhập kho nhanh từ bảng tồn thấp */}
      {stockProduct && !showAdjust && tab !== 'stock-in' && tab !== 'purchase' && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40" role="dialog" aria-modal="true" aria-labelledby="quick-stock-title">
          <form onSubmit={submitStockIn} className="w-full max-w-sm rounded-2xl bg-white p-5">
            <h2 id="quick-stock-title" className="text-base font-black text-[#111310]">Nhập kho</h2>
            <p className="mt-1 text-xs text-[#5E625A]">
              {stockProduct.name} · tồn hiện tại {stockProduct.stockQuantity}
            </p>
            <div className="mt-4 space-y-3">
              <Field label="Số lượng nhập" htmlFor="qs-qty" required>
                <input
                  id="qs-qty"
                  type="number"
                  min={1}
                  value={stockQty}
                  onChange={(e) => setStockQty(e.target.value)}
                  className="mt-1 w-full h-11 px-3 rounded-lg border border-[#DADCD3] text-sm focus:outline-none focus:ring-2 focus:ring-[#526B00]"
                />
              </Field>
              <Field label="Giá nhập / đơn vị" htmlFor="qs-cost">
                <input
                  id="qs-cost"
                  type="number"
                  min={0}
                  value={stockCost}
                  onChange={(e) => setStockCost(e.target.value)}
                  className="mt-1 w-full h-11 px-3 rounded-lg border border-[#DADCD3] text-sm focus:outline-none focus:ring-2 focus:ring-[#526B00]"
                />
              </Field>
            </div>
            <div className="mt-5 flex gap-2">
              <button
                type="button"
                onClick={() => setStockProduct(null)}
                className="flex-1 h-11 rounded-lg border border-[#DADCD3] text-sm font-semibold text-[#111310]"
              >
                Huỷ
              </button>
              <button
                type="submit"
                disabled={saving}
                className="flex-1 h-11 rounded-lg bg-[#D7FF3F] text-sm font-bold text-[#111310] disabled:opacity-40"
              >
                Nhập
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}

function Field({
  label,
  htmlFor,
  required,
  children,
}: {
  label: string;
  htmlFor: string;
  required?: boolean;
  children: React.ReactNode;
}) {
  return (
    <div>
      <label htmlFor={htmlFor} className="text-xs font-semibold text-[#111310]">
        {label}
        {required && <span className="text-[#8C1D1D]"> *</span>}
      </label>
      {children}
    </div>
  );
}

function MovementBadge({ type }: { type: string }) {
  const map: Record<string, { label: string; cls: string }> = {
    STOCK_IN: { label: 'Nhập kho', cls: 'bg-[#F3F9E1] text-[#3F4A12]' },
    SALE_OUT: { label: 'Bán ra', cls: 'bg-[#E8EEF9] text-[#1E3A6B]' },
    RETURN_IN: { label: 'Trả hàng', cls: 'bg-[#FDF6E7] text-[#8A5B00]' },
    ADJUSTMENT: { label: 'Điều chỉnh', cls: 'bg-[#F0F0EC] text-[#4A4E46]' },
    CANCEL_RETURN: { label: 'Huỷ đơn', cls: 'bg-[#FDECEC] text-[#8C1D1D]' },
  };
  const m = map[type] || { label: type, cls: 'bg-[#F0F0EC] text-[#4A4E46]' };
  return (
    <span className={`inline-block px-2 py-1 rounded text-[11px] font-bold ${m.cls}`}>{m.label}</span>
  );
}
