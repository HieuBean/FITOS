import api from '@/lib/api';
import { extractPageContent } from '@/lib/contracts';

// ==========================================
// TYPES — MODULE BÁN LẺ / POS
// ==========================================

export type ProductUnit = 'VIEN' | 'CHAI' | 'HOP' | 'CAI' | 'DOI' | 'BO' | 'GOI' | 'KG';

export const UNIT_LABELS: Record<string, string> = {
  VIEN: 'viên',
  CHAI: 'chai',
  HOP: 'hộp',
  CAI: 'cái',
  DOI: 'đôi',
  BO: 'bộ',
  GOI: 'gói',
  KG: 'kg',
};

export const UNIT_OPTIONS: { value: ProductUnit; label: string }[] = [
  { value: 'HOP', label: 'Hộp' },
  { value: 'CHAI', label: 'Chai' },
  { value: 'CAI', label: 'Cái' },
  { value: 'DOI', label: 'Đôi' },
  { value: 'BO', label: 'Bộ' },
  { value: 'GOI', label: 'Gói' },
  { value: 'VIEN', label: 'Viên' },
  { value: 'KG', label: 'Kg' },
];

export const PAYMENT_METHODS: { value: string; label: string; short: string }[] = [
  { value: 'CASH', label: 'Tiền mặt', short: 'TM' },
  { value: 'CARD', label: 'Thẻ ngân hàng', short: 'Thẻ' },
  { value: 'BANK_TRANSFER', label: 'Chuyển khoản', short: 'CK' },
  { value: 'VNPAY', label: 'VNPay', short: 'VNPay' },
  { value: 'MOMO', label: 'MoMo', short: 'MoMo' },
  { value: 'OTHER', label: 'Khác', short: 'Khác' },
];

export interface CategoryDto {
  id: number;
  name: string;
  description?: string | null;
  sortOrder: number;
  isActive: boolean;
  productCount: number;
}

export interface ProductDto {
  id: number;
  sku: string;
  barcode?: string | null;
  name: string;
  description?: string | null;
  categoryId?: number | null;
  categoryName?: string | null;
  unit: ProductUnit;
  unitLabel: string;
  costPrice: number;
  sellPrice: number;
  vatRate: number;
  stockQuantity: number;
  lowStockThreshold: number;
  trackInventory: boolean;
  imageUrl?: string | null;
  isActive: boolean;
  lowStock: boolean;
  outOfStock: boolean;
}

export interface InventoryMovementDto {
  id: number;
  transactionType: string;
  productId: number;
  productName?: string | null;
  unit?: string | null;
  quantityDelta: number;
  stockBefore: number;
  stockAfter: number;
  unitCost?: number | null;
  orderId?: number | null;
  orderReference?: string | null;
  note?: string | null;
  createdByName?: string | null;
  createdAt: string;
}

export interface LowStockDto {
  id: number;
  sku: string;
  name: string;
  categoryName?: string | null;
  unit: string;
  stockQuantity: number;
  lowStockThreshold: number;
  shortage: number;
  severity: 'LOW' | 'OUT_OF_STOCK';
}

export interface RetailOrderItemDto {
  id: number;
  itemType: 'PRODUCT' | 'PLAN' | 'BOOKING';
  productId?: number | null;
  planId?: number | null;
  sessionId?: number | null;
  itemName: string;
  unit?: string | null;
  unitLabel?: string | null;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
  vatRate: number;
  vatAmount: number;
}

export interface RetailOrderDto {
  id: number;
  orderReference: string;
  orderType: string;
  status: string;
  customerId: number;
  customerName?: string | null;
  customerPhone?: string | null;
  subtotalAmount: number;
  discountAmount: number;
  taxAmount: number;
  totalAmount: number;
  itemCount: number;
  items: RetailOrderItemDto[];
  paymentMethod?: string | null;
  paymentStatus?: string | null;
  cashReceived?: number | null;
  changeDue?: number | null;
  notes?: string | null;
  parentOrderId?: number | null;
  paidAt?: string | null;
  createdAt: string;
  idempotentReplay?: boolean;
}

export interface ReceiptDto {
  studioName: string;
  studioAddress?: string | null;
  studioPhone?: string | null;
  orderReference: string;
  issuedAt: string;
  cashierName?: string | null;
  customerName?: string | null;
  customerPhone?: string | null;
  items: RetailOrderItemDto[];
  subtotal: number;
  discount: number;
  vat: number;
  total: number;
  paymentMethod?: string | null;
  cashReceived?: number | null;
  changeDue?: number | null;
  note?: string | null;
}

export interface DailySalesSummaryDto {
  date: string;
  grossRevenue: number;
  returnAmount: number;
  netRevenue: number;
  vatTotal: number;
  cogs: number;
  grossProfit: number;
  orderCount: number;
  itemCount: number;
  byPaymentMethod: { method: string; amount: number; count: number }[];
  byCategory: { categoryName: string; quantity: number; amount: number }[];
  topProducts: { productId: number; name: string; quantity: number; amount: number }[];
}

/** Một dòng trong giỏ hàng ở màn hình POS. */
export interface CartLine {
  key: string;
  productId: number;
  name: string;
  unit: string;
  unitLabel: string;
  unitPrice: number;
  quantity: number;
  stockQuantity: number;
  vatRate: number;
}

// ==========================================
// API — CATALOG
// ==========================================

export async function listCategories(includeInactive = false): Promise<CategoryDto[]> {
  const res = await api.get('/retail/categories', { params: { includeInactive } });
  return Array.isArray(res.data) ? res.data : [];
}

export async function createCategory(payload: {
  name: string;
  description?: string;
  sortOrder?: number;
  isActive?: boolean;
}): Promise<CategoryDto> {
  const res = await api.post('/retail/categories', payload);
  return res.data;
}

export async function updateCategory(
  id: number,
  payload: { name: string; description?: string; sortOrder?: number; isActive?: boolean },
): Promise<CategoryDto> {
  const res = await api.put(`/retail/categories/${id}`, payload);
  return res.data;
}

export async function toggleCategory(id: number): Promise<CategoryDto> {
  const res = await api.patch(`/retail/categories/${id}/toggle`);
  return res.data;
}

export async function searchProducts(params: {
  search?: string;
  categoryId?: number | null;
  activeOnly?: boolean;
  page?: number;
  size?: number;
}): Promise<{ content: ProductDto[]; totalElements: number; totalPages: number }> {
  const res = await api.get('/retail/products', {
    params: {
      search: params.search || undefined,
      categoryId: params.categoryId ?? undefined,
      activeOnly: params.activeOnly ?? true,
      page: params.page ?? 0,
      size: params.size ?? 50,
    },
  });
  const content = extractPageContent<ProductDto>(res.data);
  return {
    content,
    totalElements: res.data?.totalElements ?? content.length,
    totalPages: res.data?.totalPages ?? 1,
  };
}

export async function getProductByBarcode(barcode: string): Promise<ProductDto | null> {
  try {
    const res = await api.get(`/retail/products/barcode/${encodeURIComponent(barcode)}`);
    return res.data;
  } catch (err: any) {
    if (err?.response?.status === 404) return null;
    throw err;
  }
}

export async function getProduct(id: number): Promise<ProductDto> {
  const res = await api.get(`/retail/products/${id}`);
  return res.data;
}

export async function createProduct(payload: Record<string, unknown>): Promise<ProductDto> {
  const res = await api.post('/retail/products', payload);
  return res.data;
}

export async function updateProduct(id: number, payload: Record<string, unknown>): Promise<ProductDto> {
  const res = await api.put(`/retail/products/${id}`, payload);
  return res.data;
}

export async function toggleProduct(id: number): Promise<ProductDto> {
  const res = await api.patch(`/retail/products/${id}/toggle`);
  return res.data;
}

// ==========================================
// API — INVENTORY
// ==========================================

export async function stockIn(payload: {
  productId: number;
  quantity: number;
  unitCost: number;
  note?: string;
}): Promise<InventoryMovementDto> {
  const res = await api.post('/retail/inventory/stock-in', payload);
  return res.data;
}

export async function adjustStock(payload: {
  productId: number;
  newQuantity: number;
  reason: string;
}): Promise<InventoryMovementDto> {
  const res = await api.post('/retail/inventory/adjust', payload);
  return res.data;
}

export async function lowStock(): Promise<LowStockDto[]> {
  const res = await api.get('/retail/inventory/low-stock');
  return Array.isArray(res.data) ? res.data : [];
}

export async function inventoryMovements(params: {
  productId?: number | null;
  page?: number;
  size?: number;
}): Promise<{ content: InventoryMovementDto[]; totalElements: number }> {
  const res = await api.get('/retail/inventory/movements', {
    params: { productId: params.productId ?? undefined, page: params.page ?? 0, size: params.size ?? 30 },
  });
  const content = extractPageContent<InventoryMovementDto>(res.data);
  return { content, totalElements: res.data?.totalElements ?? content.length };
}

// ==========================================
// API — ORDERS (POS)
// ==========================================

export async function createRetailOrder(payload: {
  customerId: number;
  items: { productId?: number; planId?: number; sessionId?: number; quantity: number }[];
  discountAmount?: number;
  discountCode?: string;
  paymentMethod?: string;
  cashReceived?: number;
  notes?: string;
  clientRequestId?: string;
}): Promise<RetailOrderDto> {
  const res = await api.post('/retail/orders', payload);
  return res.data;
}

export async function payRetailOrder(
  orderId: number,
  payload: { paymentMethod: string; transactionRef?: string; cashReceived?: number },
): Promise<RetailOrderDto> {
  const res = await api.post(`/retail/orders/${orderId}/pay`, payload);
  return res.data;
}

export async function listRetailOrders(params: {
  status?: string;
  page?: number;
  size?: number;
}): Promise<{ content: RetailOrderDto[]; totalElements: number }> {
  const res = await api.get('/retail/orders', {
    params: { status: params.status || undefined, page: params.page ?? 0, size: params.size ?? 20 },
  });
  const content = extractPageContent<RetailOrderDto>(res.data);
  return { content, totalElements: res.data?.totalElements ?? content.length };
}

export async function getRetailOrder(id: number): Promise<RetailOrderDto> {
  const res = await api.get(`/retail/orders/${id}`);
  return res.data;
}

export async function getReceipt(id: number): Promise<ReceiptDto> {
  const res = await api.get(`/retail/orders/${id}/receipt`);
  return res.data;
}

export async function cancelRetailOrder(id: number): Promise<RetailOrderDto> {
  const res = await api.patch(`/retail/orders/${id}/cancel`);
  return res.data;
}

export async function returnRetailOrder(
  id: number,
  payload: { items: { orderItemId: number; quantity: number }[]; reason: string },
): Promise<RetailOrderDto> {
  const res = await api.post(`/retail/orders/${id}/return`, payload);
  return res.data;
}

export async function quickCustomer(payload: {
  fullName: string;
  phone: string;
}): Promise<{ id: number; fullName: string; phone: string; created: boolean }> {
  const res = await api.post('/retail/orders/quick-customer', payload);
  return res.data;
}

export async function dailySalesSummary(date?: string): Promise<DailySalesSummaryDto> {
  const res = await api.get('/retail/reports/daily', { params: { date } });
  return res.data;
}
