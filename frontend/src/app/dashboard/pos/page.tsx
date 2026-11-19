'use client';

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import Link from 'next/link';
import RoleGuard from '@/components/RoleGuard';
import api from '@/lib/api';
import { formatCurrency } from '@/lib/contracts';
import {
  CartLine,
  CategoryDto,
  ProductDto,
  PAYMENT_METHODS,
  UNIT_LABELS,
  createRetailOrder,
  getProductByBarcode,
  getReceipt,
  listCategories,
  lowStock,
  quickCustomer,
  searchProducts,
  ReceiptDto,
  LowStockDto,
} from '@/lib/retailApi';

const CART_STORAGE_KEY = 'fitos_pos_cart_v1';

function normalizeProductSearch(value: string): string {
  return value.toLocaleLowerCase('vi-VN').normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd');
}

interface CustomerOption {
  id: number;
  fullName: string;
  phone?: string | null;
}

function newCartLine(product: ProductDto, quantity = 1): CartLine {
  return {
    key: `p${product.id}`,
    productId: product.id,
    name: product.name,
    unit: product.unit,
    unitLabel: product.unitLabel || UNIT_LABELS[product.unit] || 'cái',
    unitPrice: Number(product.sellPrice) || 0,
    quantity,
    stockQuantity: product.stockQuantity,
    vatRate: Number(product.vatRate) || 0,
  };
}

export default function PosPage() {
  return (
    <RoleGuard allowedRoles={['RECEPTIONIST']}>
      <PosScreen />
    </RoleGuard>
  );
}

function PosScreen() {
  // ---- danh mục & sản phẩm -------------------------------------------------
  const [categories, setCategories] = useState<CategoryDto[]>([]);
  const [products, setProducts] = useState<ProductDto[]>([]);
  const [activeCategory, setActiveCategory] = useState<number | null>(null);
  const [search, setSearch] = useState('');
  const visibleProducts = useMemo(() => {
    const keyword = normalizeProductSearch(search.trim());
    return products.filter((product) =>
      (activeCategory === null || product.categoryId === activeCategory) &&
      (!keyword || [product.name, product.sku, product.barcode ?? '']
        .some((value) => normalizeProductSearch(value).includes(keyword))));
  }, [products, activeCategory, search]);
  const [loadingProducts, setLoadingProducts] = useState(true);
  const [banner, setBanner] = useState<{ kind: 'error' | 'info'; text: string } | null>(null);

  // ---- giỏ hàng ------------------------------------------------------------
  const [cart, setCart] = useState<CartLine[]>([]);
  const [quantityDrafts, setQuantityDrafts] = useState<Record<number, string>>({});
  const [customer, setCustomer] = useState<CustomerOption | null>(null);
  const [customerQuery, setCustomerQuery] = useState('');
  const [customerOptions, setCustomerOptions] = useState<CustomerOption[]>([]);
  const [showNewCustomer, setShowNewCustomer] = useState(false);
  const [newCustomer, setNewCustomer] = useState({ fullName: '', phone: '' });

  // ---- thanh toán ----------------------------------------------------------
  const [discountAmount, setDiscountAmount] = useState('');
  const [paymentMethod, setPaymentMethod] = useState('CASH');
  const [cashReceived, setCashReceived] = useState('');
  const [notes, setNotes] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [receipt, setReceipt] = useState<ReceiptDto | null>(null);
  const [lowStockItems, setLowStockItems] = useState<LowStockDto[]>([]);

  // clientRequestId sinh một lần cho mỗi giỏ — chống tạo trùng khi retry
  const clientRequestIdRef = useRef<string>('');
  const searchRef = useRef<HTMLInputElement>(null);

  if (!clientRequestIdRef.current) {
    clientRequestIdRef.current =
      typeof crypto !== 'undefined' && crypto.randomUUID
        ? crypto.randomUUID()
        : `pos-${Date.now()}-${Math.random().toString(36).slice(2)}`;
  }

  // ---- nạp dữ liệu ---------------------------------------------------------
  const loadProducts = useCallback(async () => {
    setLoadingProducts(true);
    try {
      const firstPage = await searchProducts({
        activeOnly: true,
        size: 100,
        page: 0,
      });
      const allProducts = [...firstPage.content];
      for (let page = 1; page < firstPage.totalPages; page += 1) {
        const nextPage = await searchProducts({ activeOnly: true, size: 100, page });
        allProducts.push(...nextPage.content);
      }
      setProducts(allProducts);
      setBanner(null);
    } catch (err) {
      console.error('Không tải được sản phẩm:', err);
      setBanner({ kind: 'error', text: 'Không tải được danh sách sản phẩm. Kiểm tra kết nối rồi thử lại.' });
      setProducts([]);
    } finally {
      setLoadingProducts(false);
    }
  }, []);

  useEffect(() => {
    listCategories().then(setCategories).catch(() => setCategories([]));
    lowStock().then(setLowStockItems).catch(() => setLowStockItems([]));
  }, []);

  useEffect(() => {
    const timer = setTimeout(loadProducts, 250);
    return () => clearTimeout(timer);
  }, [loadProducts]);

  // Khôi phục giỏ hàng sau khi F5
  useEffect(() => {
    try {
      const raw = localStorage.getItem(CART_STORAGE_KEY);
      if (raw) {
        const saved = JSON.parse(raw);
        if (Array.isArray(saved?.cart)) setCart(saved.cart);
        if (saved?.customer) setCustomer(saved.customer);
        if (saved?.clientRequestId) clientRequestIdRef.current = saved.clientRequestId;
      }
    } catch {
      /* bỏ qua dữ liệu hỏng */
    }
  }, []);

  useEffect(() => {
    try {
      localStorage.setItem(
        CART_STORAGE_KEY,
        JSON.stringify({ cart, customer, clientRequestId: clientRequestIdRef.current }),
      );
    } catch {
      /* localStorage đầy — không chặn bán hàng */
    }
  }, [cart, customer]);

  // ---- thao tác giỏ --------------------------------------------------------
  const addProduct = useCallback((product: ProductDto) => {
    setCart((prev) => {
      const existing = prev.find((l) => l.productId === product.id);
      const max = product.trackInventory ? product.stockQuantity : Number.MAX_SAFE_INTEGER;
      if (existing) {
        if (existing.quantity + 1 > max) {
          setBanner({
            kind: 'error',
            text: `Chỉ còn ${max} ${existing.unitLabel} cho "${product.name}"`,
          });
          return prev;
        }
        return prev.map((l) =>
          l.productId === product.id ? { ...l, quantity: l.quantity + 1 } : l,
        );
      }
      if (product.outOfStock) {
        setBanner({ kind: 'error', text: `"${product.name}" đã hết hàng` });
        return prev;
      }
      return [...prev, newCartLine(product)];
    });
  }, []);

  const maxQuantity = (line: CartLine) => {
    const product = products.find((item) => item.id === line.productId);
    return product?.trackInventory === false ? Number.MAX_SAFE_INTEGER : (product?.stockQuantity ?? line.stockQuantity);
  };

  const changeQty = (productId: number, delta: number) => {
    setQuantityDrafts((prev) => {
      const next = { ...prev };
      delete next[productId];
      return next;
    });
    setCart((prev) =>
      prev
        .map((l) => {
          if (l.productId !== productId) return l;
          const next = l.quantity + delta;
          const max = maxQuantity(l);
          if (delta > 0 && next > max) {
            setBanner({ kind: 'error', text: `Chỉ còn ${max} ${l.unitLabel} cho "${l.name}"` });
            return l;
          }
          return { ...l, quantity: next };
        })
        .filter((l) => l.quantity > 0),
    );
  };

  const commitQuantity = (line: CartLine) => {
    const draft = quantityDrafts[line.productId];
    if (draft === undefined) return;
    setQuantityDrafts((prev) => {
      const next = { ...prev };
      delete next[line.productId];
      return next;
    });
    if (draft === '') return;
    const quantity = Number(draft);
    if (!Number.isSafeInteger(quantity)) return;
    const max = maxQuantity(line);
    if (quantity > max) {
      setBanner({ kind: 'error', text: `Chỉ còn ${max} ${line.unitLabel} cho "${line.name}"` });
      return;
    }
    setCart((prev) => prev.map((item) => item.productId === line.productId ? { ...item, quantity } : item).filter((item) => item.quantity > 0));
  };

  const removeLine = (productId: number) =>
    setCart((prev) => prev.filter((l) => l.productId !== productId));

  const resetSale = () => {
    setCart([]);
    setCustomer(null);
    setDiscountAmount('');
    setCashReceived('');
    setNotes('');
    setPaymentMethod('CASH');
    setReceipt(null);
    clientRequestIdRef.current =
      typeof crypto !== 'undefined' && crypto.randomUUID
        ? crypto.randomUUID()
        : `pos-${Date.now()}-${Math.random().toString(36).slice(2)}`;
    searchRef.current?.focus();
  };

  // ---- tìm khách -----------------------------------------------------------
  useEffect(() => {
    if (customerQuery.trim().length < 2) {
      setCustomerOptions([]);
      return;
    }
    const timer = setTimeout(async () => {
      try {
        const { data } = await api.get('/customers', {
          params: { search: customerQuery.trim(), size: 8 },
        });
        const list = Array.isArray(data) ? data : data?.content ?? [];
        setCustomerOptions(
          list.map((c: any) => ({ id: c.id, fullName: c.fullName, phone: c.phone })),
        );
      } catch {
        setCustomerOptions([]);
      }
    }, 250);
    return () => clearTimeout(timer);
  }, [customerQuery]);

  const submitNewCustomer = async () => {
    if (!newCustomer.fullName.trim() || !newCustomer.phone.trim()) {
      setBanner({ kind: 'error', text: 'Nhập tên và số điện thoại khách' });
      return;
    }
    try {
      const created = await quickCustomer({
        fullName: newCustomer.fullName.trim(),
        phone: newCustomer.phone.trim(),
      });
      setCustomer({ id: created.id, fullName: created.fullName, phone: created.phone });
      setShowNewCustomer(false);
      setNewCustomer({ fullName: '', phone: '' });
      setCustomerOptions([]);
      setCustomerQuery('');
      setBanner({ kind: 'info', text: `Đã tạo khách: ${created.fullName}` });
    } catch (err: any) {
      setBanner({
        kind: 'error',
        text: err?.response?.data?.message || 'Không tạo được khách hàng',
      });
    }
  };

  // ---- tính tiền -----------------------------------------------------------
  const subtotal = useMemo(
    () => cart.reduce((sum, l) => sum + l.unitPrice * l.quantity, 0),
    [cart],
  );
  const discount = Math.min(Number(discountAmount) || 0, subtotal);
  const total = Math.max(subtotal - discount, 0);
  const vatTotal = useMemo(
    () =>
      cart.reduce((sum, l) => {
        if (!l.vatRate) return sum;
        const lineTotal = l.unitPrice * l.quantity;
        return sum + (lineTotal * l.vatRate) / (100 + l.vatRate);
      }, 0),
    [cart],
  );
  const changeDue = paymentMethod === 'CASH' ? Math.max((Number(cashReceived) || 0) - total, 0) : 0;
  const cashShort = paymentMethod === 'CASH' && cashReceived !== '' && (Number(cashReceived) || 0) < total;

  // ---- barcode: Enter = thêm ngay ------------------------------------------
  const handleSearchKeyDown = async (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key !== 'Enter') return;
    e.preventDefault();
    const code = search.trim();
    if (!code) return;
    // Khớp chính xác barcode/SKU thì thêm luôn (máy quét gõ + Enter)
    const exact = products.find(
      (p) => p.barcode === code || p.sku.toLowerCase() === code.toLowerCase(),
    );
    if (exact) {
      addProduct(exact);
      setSearch('');
      return;
    }
    try {
      const byCode = await getProductByBarcode(code);
      if (byCode) {
        addProduct(byCode);
        setSearch('');
      } else if (visibleProducts.length === 1) {
        addProduct(visibleProducts[0]);
        setSearch('');
      } else {
        setBanner({ kind: 'error', text: `Không tìm thấy mã vạch "${code}"` });
      }
    } catch {
      setBanner({ kind: 'error', text: `Không tìm thấy mã vạch "${code}"` });
    }
  };

  // ---- thanh toán ----------------------------------------------------------
  const checkout = async () => {
    if (cart.length === 0) {
      setBanner({ kind: 'error', text: 'Giỏ hàng đang trống' });
      return;
    }
    if (!customer) {
      setBanner({ kind: 'error', text: 'Chọn khách hàng trước khi thanh toán' });
      return;
    }
    if (paymentMethod === 'CASH' && cashShort) {
      setBanner({ kind: 'error', text: 'Tiền khách đưa nhỏ hơn tổng tiền' });
      return;
    }

    setSubmitting(true);
    setBanner(null);
    try {
      const order = await createRetailOrder({
        customerId: customer.id,
        items: cart.map((l) => ({ productId: l.productId, quantity: l.quantity })),
        discountAmount: discount > 0 ? discount : undefined,
        paymentMethod,
        cashReceived: paymentMethod === 'CASH' ? Number(cashReceived) || total : undefined,
        notes: notes.trim() || undefined,
        clientRequestId: clientRequestIdRef.current,
      });
      const rc = await getReceipt(order.id);
      setReceipt(rc);
      lowStock().then(setLowStockItems).catch(() => {});
      loadProducts();
    } catch (err: any) {
      console.error('Thanh toán thất bại:', err);
      setBanner({
        kind: 'error',
        text: err?.response?.data?.message || 'Thanh toán thất bại. Thử lại hoặc kiểm tra tồn kho.',
      });
    } finally {
      setSubmitting(false);
    }
  };

  // ---- phím tắt ------------------------------------------------------------
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'F2') {
        e.preventDefault();
        searchRef.current?.focus();
      } else if (e.key === 'F4') {
        e.preventDefault();
        checkout();
      } else if (e.key === 'Escape') {
        setReceipt(null);
        setShowNewCustomer(false);
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  });

  return (
    <div className="min-h-screen bg-[#F5F5F0]">
      {/* Header */}
      <header className="bg-white border-b border-[#DADCD3] px-4 sm:px-6 lg:px-8 py-4">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
          <div>
            <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[#526B00] mb-1">
              Bán hàng
            </p>
            <h1 className="text-xl sm:text-2xl font-black text-[#111310] tracking-tight">
              Bán tại quầy (POS)
            </h1>
          </div>
          <div className="flex items-center gap-2">
            <Link
              href="/dashboard/retail"
              className="h-11 px-4 inline-flex items-center rounded-lg border border-[#DADCD3] bg-white text-sm font-semibold text-[#111310] hover:bg-[#F5F5F0]"
            >
              Hàng hoá &amp; tồn kho
            </Link>
            <Link
              href="/dashboard/retail-orders"
              className="h-11 px-4 inline-flex items-center rounded-lg border border-[#DADCD3] bg-white text-sm font-semibold text-[#111310] hover:bg-[#F5F5F0]"
            >
              Đơn bán lẻ
            </Link>
          </div>
        </div>

        {lowStockItems.length > 0 && (
          <div className="mt-3 rounded-lg border border-[#E8C98A] bg-[#FDF6E7] px-3 py-2 text-xs text-[#6B4E12]">
            <strong>{lowStockItems.length} mặt hàng sắp hết:</strong>{' '}
            {lowStockItems.slice(0, 3).map((i) => `${i.name} (còn ${i.stockQuantity})`).join(' · ')}
            {lowStockItems.length > 3 && ` và ${lowStockItems.length - 3} mặt hàng khác`}
          </div>
        )}

        {banner && (
          <div
            role="alert"
            className={`mt-3 rounded-lg px-3 py-2 text-xs font-medium ${
              banner.kind === 'error'
                ? 'border border-[#E7B4B4] bg-[#FDECEC] text-[#8C1D1D]'
                : 'border border-[#C6D89A] bg-[#F3F9E1] text-[#3F4A12]'
            }`}
          >
            {banner.text}
            <button
              type="button"
              onClick={() => setBanner(null)}
              className="ml-3 underline font-semibold"
              aria-label="Đóng thông báo"
            >
              Đóng
            </button>
          </div>
        )}
      </header>

      <div className="px-4 sm:px-6 lg:px-8 py-5 grid grid-cols-1 xl:grid-cols-[1fr_400px] gap-5">
        {/* ============================ CỘT TRÁI: SẢN PHẨM ============================ */}
        <section aria-label="Danh sách sản phẩm">
          {/* Tìm kiếm + mã vạch */}
          <div className="relative">
            <label htmlFor="pos-search" className="sr-only">
              Tìm sản phẩm hoặc quét mã vạch
            </label>
            <input
              id="pos-search"
              ref={searchRef}
              type="search"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              onKeyDown={handleSearchKeyDown}
              placeholder="Quét mã vạch hoặc tìm theo tên / SKU  (F2)"
              autoComplete="off"
              className="w-full h-12 pl-11 pr-4 rounded-xl border border-[#DADCD3] bg-white text-sm text-[#111310] placeholder:text-[#5E625A] focus:outline-none focus:ring-2 focus:ring-[#526B00]"
            />
            <svg
              aria-hidden="true"
              className="absolute left-4 top-1/2 -translate-y-1/2 h-5 w-5 text-[#5E625A]"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
              strokeWidth="1.8"
            >
              <path strokeLinecap="round" strokeLinejoin="round" d="m21 21-4.3-4.3M17 11a6 6 0 1 1-12 0 6 6 0 0 1 12 0Z" />
            </svg>
          </div>

          {/* Danh mục */}
          <div className="mt-3 flex gap-2 overflow-x-auto pb-1" role="tablist" aria-label="Danh mục sản phẩm">
            <CategoryChip
              label="Tất cả"
              active={activeCategory === null}
              onClick={() => setActiveCategory(null)}
            />
            {categories.map((c) => (
              <CategoryChip
                key={c.id}
                label={c.name}
                active={activeCategory === c.id}
                onClick={() => setActiveCategory(c.id)}
              />
            ))}
          </div>

          {/* Lưới sản phẩm */}
          {loadingProducts ? (
            <div className="mt-4 grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3">
              {Array.from({ length: 8 }).map((_, i) => (
                <div key={i} className="h-28 rounded-xl bg-white border border-[#DADCD3] animate-pulse" />
              ))}
            </div>
          ) : visibleProducts.length === 0 ? (
            <div className="mt-4 rounded-xl border border-dashed border-[#DADCD3] bg-white px-6 py-12 text-center">
              <p className="text-sm font-semibold text-[#111310]">Không có sản phẩm phù hợp</p>
              <p className="mt-1 text-xs text-[#5E625A]">
                Thử từ khoá khác, hoặc thêm hàng hoá ở trang Hàng hoá &amp; tồn kho.
              </p>
            </div>
          ) : (
            <div className="mt-4 grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3">
              {visibleProducts.map((p) => {
                const inCart = cart.find((l) => l.productId === p.id)?.quantity ?? 0;
                return (
                  <button
                    key={p.id}
                    type="button"
                    onClick={() => addProduct(p)}
                    disabled={p.outOfStock}
                    aria-label={`Thêm ${p.name} vào giỏ, giá ${formatCurrency(Number(p.sellPrice))}`}
                    className={`relative text-left rounded-xl border bg-white p-3 min-h-[112px] flex flex-col justify-between transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-[#526B00] ${
                      p.outOfStock
                        ? 'border-[#DADCD3] opacity-50 cursor-not-allowed'
                        : 'border-[#DADCD3] hover:border-[#526B00] hover:bg-[#F9FFDF]'
                    }`}
                  >
                    {inCart > 0 && (
                      <span className="absolute top-2 right-2 min-w-[20px] h-5 px-1 rounded-full bg-[#111310] text-white text-[11px] font-bold flex items-center justify-center">
                        {inCart}
                      </span>
                    )}
                    <div>
                      <p className="text-[11px] font-semibold uppercase tracking-wide text-[#526B00] truncate">
                        {p.categoryName || p.sku}
                      </p>
                      <p className="mt-1 text-sm font-bold text-[#111310] leading-snug line-clamp-2">
                        {p.name}
                      </p>
                    </div>
                    <div className="mt-2 flex items-end justify-between gap-2">
                      <span className="text-sm font-black text-[#111310]">
                        {formatCurrency(Number(p.sellPrice))}
                      </span>
                      <span
                        className={`text-[11px] font-semibold ${
                          p.outOfStock ? 'text-[#8C1D1D]' : p.lowStock ? 'text-[#8A5B00]' : 'text-[#5E625A]'
                        }`}
                      >
                        {p.trackInventory ? `còn ${p.stockQuantity}` : 'không quản lý tồn'}
                      </span>
                    </div>
                  </button>
                );
              })}
            </div>
          )}
        </section>

        {/* ============================ CỘT PHẢI: GIỎ HÀNG ============================ */}
        <aside aria-label="Giỏ hàng" className="xl:sticky xl:top-4 xl:self-start">
          <div className="rounded-2xl border border-[#DADCD3] bg-white overflow-hidden">
            {/* Khách hàng */}
            <div className="p-4 border-b border-[#DADCD3]">
              {customer ? (
                <div className="flex items-start justify-between gap-2">
                  <div className="min-w-0">
                    <p className="text-[11px] font-semibold uppercase tracking-[0.08em] text-[#5E625A]">
                      Khách hàng
                    </p>
                    <p className="mt-1 text-sm font-bold text-[#111310] truncate">{customer.fullName}</p>
                    {customer.phone && <p className="text-xs text-[#5E625A]">{customer.phone}</p>}
                  </div>
                  <button
                    type="button"
                    onClick={() => setCustomer(null)}
                    className="shrink-0 h-11 px-3 rounded-lg border border-[#DADCD3] text-xs font-semibold text-[#111310] hover:bg-[#F5F5F0]"
                  >
                    Đổi
                  </button>
                </div>
              ) : (
                <>
                  <label htmlFor="pos-customer" className="text-[11px] font-semibold uppercase tracking-[0.08em] text-[#5E625A]">
                    Khách hàng
                  </label>
                  <input
                    id="pos-customer"
                    type="search"
                    value={customerQuery}
                    onChange={(e) => setCustomerQuery(e.target.value)}
                    placeholder="Tìm theo tên hoặc số điện thoại"
                    className="mt-1 w-full h-11 px-3 rounded-lg border border-[#DADCD3] bg-white text-sm focus:outline-none focus:ring-2 focus:ring-[#526B00]"
                  />
                  {customerOptions.length > 0 && (
                    <ul className="mt-1 max-h-40 overflow-y-auto rounded-lg border border-[#DADCD3] divide-y divide-[#EDEEE8]">
                      {customerOptions.map((c) => (
                        <li key={c.id}>
                          <button
                            type="button"
                            onClick={() => {
                              setCustomer(c);
                              setCustomerOptions([]);
                              setCustomerQuery('');
                            }}
                            className="w-full text-left px-3 py-2 text-sm hover:bg-[#F5F5F0]"
                          >
                            <span className="font-semibold text-[#111310]">{c.fullName}</span>
                            {c.phone && <span className="ml-2 text-xs text-[#5E625A]">{c.phone}</span>}
                          </button>
                        </li>
                      ))}
                    </ul>
                  )}
                  <button
                    type="button"
                    onClick={() => setShowNewCustomer(true)}
                    className="mt-2 text-xs font-semibold text-[#526B00] underline"
                  >
                    + Khách mới (chưa có hồ sơ)
                  </button>
                </>
              )}
            </div>

            {/* Dòng giỏ hàng */}
            <div className="max-h-[38vh] overflow-y-auto">
              {cart.length === 0 ? (
                <div className="px-4 py-10 text-center">
                  <p className="text-sm font-semibold text-[#111310]">Giỏ hàng trống</p>
                  <p className="mt-1 text-xs text-[#5E625A]">Chọn sản phẩm bên trái để bắt đầu bán.</p>
                </div>
              ) : (
                <ul className="divide-y divide-[#EDEEE8]">
                  {cart.map((line) => (
                    <li key={line.key} className="p-3 flex gap-3">
                      <div className="min-w-0 flex-1">
                        <p className="text-sm font-semibold text-[#111310] leading-snug">{line.name}</p>
                        <p className="mt-0.5 text-xs text-[#5E625A]">
                          {formatCurrency(line.unitPrice)} / {line.unitLabel}
                        </p>
                        <div className="mt-1.5 flex items-center gap-1">
                          <button
                            type="button"
                            onClick={() => changeQty(line.productId, -1)}
                            aria-label={`Giảm số lượng ${line.name}`}
                            className="h-11 w-11 rounded-md border border-[#DADCD3] text-base font-bold text-[#111310] hover:bg-[#F5F5F0]"
                          >
                            −
                          </button>
                          <input
                            type="number"
                            inputMode="numeric"
                            min={0}
                            max={maxQuantity(line)}
                            step={1}
                            aria-label={`Số lượng ${line.name}`}
                            value={quantityDrafts[line.productId] ?? line.quantity}
                            onChange={(event) => {
                              if (/^\d*$/.test(event.target.value)) {
                                setQuantityDrafts((prev) => ({ ...prev, [line.productId]: event.target.value }));
                              }
                            }}
                            onBlur={() => commitQuantity(line)}
                            onKeyDown={(event) => {
                              if (event.key === 'Enter') event.currentTarget.blur();
                            }}
                            className="h-11 w-14 rounded-md border border-[#DADCD3] bg-white text-center text-sm font-bold text-[#111310] focus:outline-none focus:ring-2 focus:ring-[#526B00]"
                          />
                          <button
                            type="button"
                            onClick={() => changeQty(line.productId, 1)}
                            aria-label={`Tăng số lượng ${line.name}`}
                            className="h-11 w-11 rounded-md border border-[#DADCD3] text-base font-bold text-[#111310] hover:bg-[#F5F5F0]"
                          >
                            +
                          </button>
                          <button
                            type="button"
                            onClick={() => removeLine(line.productId)}
                            aria-label={`Xoá ${line.name} khỏi giỏ`}
                            className="ml-auto h-11 px-3 rounded-md text-xs font-semibold text-[#8C1D1D] hover:bg-[#FDECEC]"
                          >
                            Xoá
                          </button>
                        </div>
                      </div>
                      <p className="shrink-0 text-sm font-black text-[#111310]">
                        {formatCurrency(line.unitPrice * line.quantity)}
                      </p>
                    </li>
                  ))}
                </ul>
              )}
            </div>

            {/* Tổng kết + thanh toán */}
            <div className="border-t border-[#DADCD3] p-4 space-y-3">
              <div className="flex items-center justify-between text-sm">
                <span className="text-[#5E625A]">Tạm tính</span>
                <span className="font-bold text-[#111310]">{formatCurrency(subtotal)}</span>
              </div>

              <div>
                <label htmlFor="pos-discount" className="text-[11px] font-semibold uppercase tracking-[0.08em] text-[#5E625A]">
                  Giảm giá (VND)
                </label>
                <input
                  id="pos-discount"
                  type="number"
                  min={0}
                  max={subtotal}
                  value={discountAmount}
                  onChange={(e) => setDiscountAmount(e.target.value)}
                  placeholder="0"
                  className="mt-1 w-full h-10 px-3 rounded-lg border border-[#DADCD3] bg-white text-sm focus:outline-none focus:ring-2 focus:ring-[#526B00]"
                />
              </div>

              <p className="text-[11px] text-[#5E625A]">
                Trong đó VAT: {formatCurrency(vatTotal)} (giá đã bao gồm thuế)
              </p>

              <div className="flex items-center justify-between border-t border-[#DADCD3] pt-2">
                <span className="text-sm font-bold text-[#111310]">Tổng cộng</span>
                <span className="text-lg font-black text-[#111310]">{formatCurrency(total)}</span>
              </div>

              {/* Phương thức */}
              <div>
                <p className="text-[11px] font-semibold uppercase tracking-[0.08em] text-[#5E625A] mb-1">
                  Phương thức
                </p>
                <div className="grid grid-cols-3 gap-1.5">
                  {PAYMENT_METHODS.map((m) => (
                    <button
                      key={m.value}
                      type="button"
                      onClick={() => setPaymentMethod(m.value)}
                      aria-pressed={paymentMethod === m.value}
                      className={`h-11 rounded-lg border text-xs font-semibold transition-colors ${
                        paymentMethod === m.value
                          ? 'bg-[#111310] border-[#111310] text-white'
                          : 'bg-white border-[#DADCD3] text-[#111310] hover:bg-[#F5F5F0]'
                      }`}
                    >
                      {m.short}
                    </button>
                  ))}
                </div>
              </div>

              {paymentMethod === 'CASH' && (
                <div>
                  <label htmlFor="pos-cash" className="text-[11px] font-semibold uppercase tracking-[0.08em] text-[#5E625A]">
                    Tiền khách đưa
                  </label>
                  <input
                    id="pos-cash"
                    type="number"
                    min={0}
                    value={cashReceived}
                    onChange={(e) => setCashReceived(e.target.value)}
                    placeholder={String(total)}
                    className={`mt-1 w-full h-10 px-3 rounded-lg border bg-white text-sm focus:outline-none focus:ring-2 focus:ring-[#526B00] ${
                      cashShort ? 'border-[#C0392B]' : 'border-[#DADCD3]'
                    }`}
                  />
                  {cashReceived !== '' && (
                    <p className={`mt-1 text-xs font-semibold ${cashShort ? 'text-[#8C1D1D]' : 'text-[#3F4A12]'}`}>
                      {cashShort ? 'Còn thiếu ' : 'Tiền thối: '}
                      {formatCurrency(cashShort ? total - (Number(cashReceived) || 0) : changeDue)}
                    </p>
                  )}
                </div>
              )}

              <button
                type="button"
                onClick={checkout}
                disabled={submitting || cart.length === 0 || !customer}
                className="w-full h-12 rounded-xl bg-[#D7FF3F] text-[#111310] text-sm font-black uppercase tracking-wide hover:brightness-95 disabled:opacity-40 disabled:cursor-not-allowed focus:outline-none focus-visible:ring-2 focus-visible:ring-[#526B00]"
              >
                {submitting ? 'Đang xử lý…' : `Thanh toán ${formatCurrency(total)} (F4)`}
              </button>

              {cart.length > 0 && (
                <button
                  type="button"
                  onClick={resetSale}
                  className="w-full h-11 rounded-lg text-xs font-semibold text-[#5E625A] hover:bg-[#F5F5F0]"
                >
                  Huỷ giỏ hàng
                </button>
              )}
            </div>
          </div>
        </aside>
      </div>

      {/* Modal khách mới */}
      {showNewCustomer && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40" role="dialog" aria-modal="true" aria-labelledby="new-customer-title">
          <div className="w-full max-w-sm rounded-2xl bg-white p-5">
            <h2 id="new-customer-title" className="text-base font-black text-[#111310]">
              Khách mới tại quầy
            </h2>
            <p className="mt-1 text-xs text-[#5E625A]">
              Chỉ cần tên và số điện thoại để lưu hoá đơn.
            </p>
            <div className="mt-4 space-y-3">
              <div>
                <label htmlFor="nc-name" className="text-xs font-semibold text-[#111310]">Tên khách</label>
                <input
                  id="nc-name"
                  value={newCustomer.fullName}
                  onChange={(e) => setNewCustomer((s) => ({ ...s, fullName: e.target.value }))}
                  className="mt-1 w-full h-11 px-3 rounded-lg border border-[#DADCD3] text-sm focus:outline-none focus:ring-2 focus:ring-[#526B00]"
                />
              </div>
              <div>
                <label htmlFor="nc-phone" className="text-xs font-semibold text-[#111310]">Số điện thoại</label>
                <input
                  id="nc-phone"
                  inputMode="tel"
                  value={newCustomer.phone}
                  onChange={(e) => setNewCustomer((s) => ({ ...s, phone: e.target.value }))}
                  placeholder="09xxxxxxxx"
                  className="mt-1 w-full h-11 px-3 rounded-lg border border-[#DADCD3] text-sm focus:outline-none focus:ring-2 focus:ring-[#526B00]"
                />
              </div>
            </div>
            <div className="mt-5 flex gap-2">
              <button
                type="button"
                onClick={() => setShowNewCustomer(false)}
                className="flex-1 h-11 rounded-lg border border-[#DADCD3] text-sm font-semibold text-[#111310] hover:bg-[#F5F5F0]"
              >
                Huỷ
              </button>
              <button
                type="button"
                onClick={submitNewCustomer}
                className="flex-1 h-11 rounded-lg bg-[#111310] text-sm font-bold text-white hover:brightness-110"
              >
                Lưu khách
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal hoá đơn */}
      {receipt && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40" role="dialog" aria-modal="true" aria-labelledby="receipt-title">
          <div className="w-full max-w-md max-h-[90vh] overflow-y-auto rounded-2xl bg-white">
            <div id="receipt" className="p-6">
              <div className="text-center border-b border-dashed border-[#DADCD3] pb-3">
                <h2 id="receipt-title" className="text-base font-black uppercase tracking-wide text-[#111310]">
                  {receipt.studioName}
                </h2>
                {receipt.studioAddress && (
                  <p className="text-xs text-[#5E625A] mt-0.5">{receipt.studioAddress}</p>
                )}
                {receipt.studioPhone && (
                  <p className="text-xs text-[#5E625A]">ĐT: {receipt.studioPhone}</p>
                )}
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
                <div className="flex justify-between">
                  <span className="text-[#5E625A]">Tạm tính</span>
                  <span className="text-[#111310]">{formatCurrency(Number(receipt.subtotal))}</span>
                </div>
                {Number(receipt.discount) > 0 && (
                  <div className="flex justify-between">
                    <span className="text-[#5E625A]">Giảm giá</span>
                    <span className="text-[#111310]">−{formatCurrency(Number(receipt.discount))}</span>
                  </div>
                )}
                <div className="flex justify-between">
                  <span className="text-[#5E625A]">VAT (đã gồm)</span>
                  <span className="text-[#111310]">{formatCurrency(Number(receipt.vat))}</span>
                </div>
                <div className="flex justify-between text-sm font-black pt-1 border-t border-[#DADCD3]">
                  <span className="text-[#111310]">TỔNG CỘNG</span>
                  <span className="text-[#111310]">{formatCurrency(Number(receipt.total))}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-[#5E625A]">Thanh toán</span>
                  <span className="text-[#111310]">
                    {PAYMENT_METHODS.find((m) => m.value === receipt.paymentMethod)?.label || receipt.paymentMethod}
                  </span>
                </div>
                {receipt.cashReceived != null && (
                  <>
                    <div className="flex justify-between">
                      <span className="text-[#5E625A]">Khách đưa</span>
                      <span className="text-[#111310]">{formatCurrency(Number(receipt.cashReceived))}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-[#5E625A]">Tiền thối</span>
                      <span className="text-[#111310]">{formatCurrency(Number(receipt.changeDue))}</span>
                    </div>
                  </>
                )}
              </div>

              <p className="mt-4 text-center text-[10px] text-[#5E625A]">
                Cảm ơn quý khách và hẹn gặp lại!
              </p>
            </div>

            <div className="flex gap-2 p-4 border-t border-[#DADCD3] bg-[#F9FAF6]">
              <button
                type="button"
                onClick={() => window.print()}
                className="flex-1 h-11 rounded-lg border border-[#DADCD3] bg-white text-sm font-semibold text-[#111310] hover:bg-[#F5F5F0]"
              >
                In hoá đơn
              </button>
              <button
                type="button"
                onClick={resetSale}
                className="flex-1 h-11 rounded-lg bg-[#111310] text-sm font-bold text-white hover:brightness-110"
              >
                Đơn mới
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function CategoryChip({
  label,
  active,
  onClick,
}: {
  label: string;
  active: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={active}
      className={`shrink-0 h-11 px-4 rounded-full border text-xs font-semibold transition-colors ${
        active
          ? 'bg-[#111310] border-[#111310] text-white'
          : 'bg-white border-[#DADCD3] text-[#111310] hover:bg-[#F5F5F0]'
      }`}
    >
      {label}
    </button>
  );
}
