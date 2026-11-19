'use client';

import { useEffect, useMemo, useState } from 'react';
import RoleGuard from '@/components/RoleGuard';
import api from '@/lib/api';
import { formatCurrency } from '@/lib/contracts';

type Tab = 'day' | 'period' | 'people' | 'products';
type PeriodMode = 'quarters' | 'months' | 'years';
type Period = { label: string; income: number; expense: number; balance: number };
type MatrixRow = { id: number | null; name: string; values: number[]; total: number };
type Matrix = { columns: string[]; rows: MatrixRow[]; totals: number[]; grandTotal: number };
type Overview = { from: string; to: string; year: number; rangeIncome: number; rangeExpense: number;
  rangeBalance: number; months: Period[]; quarters: Period[]; years: Period[];
  employees: Matrix; customers: Matrix; products: Matrix };
type Range = { posSales: number; packageSales: number; salesTotal: number; purchaseTotal: number; invoiceTotal: number };
type Transaction = { id: number; detailId: number | null; type: 'POS' | 'PACKAGE' | 'POS_RETURN' | 'PURCHASE';
  reference: string; partyName: string; employeeName: string; at: string; amount: number };
type TransactionPage = { content: Transaction[]; totalElements: number; page: number; size: number };
type Drill = { label: string; from: string; to: string; kind?: 'SALE' | 'PURCHASE';
  employeeId?: number; customerId?: number; productId?: number };
type Detail = { invoice?: { reference: string; customerName: string; totalAmount: number; netAmount: number };
  items?: { id: number; name?: string; productName?: string; quantity: number; totalPrice?: number; lineTotal?: number }[];
  receiptNo?: string; supplierName?: string; totalAmount?: number };

const tabs: { id: Tab; label: string }[] = [
  { id: 'day', label: 'Theo ngày' }, { id: 'period', label: 'Thu–Chi–LN' },
  { id: 'people', label: 'Theo NV / KH' }, { id: 'products', label: 'Theo Sản phẩm' },
];
const inputClass = 'h-10 rounded-lg border border-[#D2D7CA] bg-white px-3 text-sm text-[#171B16] focus:border-[#657F00] focus:outline-none focus:ring-2 focus:ring-[#D7FF3F]';
const buttonClass = 'h-10 rounded-lg px-4 text-sm font-semibold focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#526B00] disabled:opacity-50';
const money = (value: number | null | undefined) => formatCurrency(Number(value ?? 0));
const today = () => new Intl.DateTimeFormat('sv-SE', { timeZone: 'Asia/Ho_Chi_Minh' }).format(new Date());
const dayEnd = (year: number, month: number) => new Date(Date.UTC(year, month, 0)).getUTCDate();
const dateOnly = (value: string) => new Intl.DateTimeFormat('sv-SE', { timeZone: 'Asia/Ho_Chi_Minh' }).format(new Date(value));

function SummaryCard({ label, value, tone = 'plain' }: { label: string; value: string; tone?: 'plain' | 'positive' | 'negative' }) {
  return <div className="rounded-xl border border-[#DEE2D5] bg-[#F9FAF6] p-4">
    <p className="text-sm text-[#5A6254]">{label}</p>
    <p className={`mt-2 text-xl font-bold tabular-nums ${tone === 'positive' ? 'text-[#246D44]' : tone === 'negative' ? 'text-[#B23732]' : 'text-[#171B16]'}`}>{value}</p>
  </div>;
}

function MatrixTable({ matrix, firstLabel, filter = '', onCellDoubleClick }: { matrix: Matrix; firstLabel: string;
  filter?: string; onCellDoubleClick?: (row: MatrixRow, column: number) => void }) {
  const [sort, setSort] = useState<{ index: number; ascending: boolean }>({ index: -1, ascending: false });
  const rows = useMemo(() => {
    const matching = matrix.rows.filter((row) => row.name.toLocaleLowerCase('vi').includes(filter.toLocaleLowerCase('vi')));
    return [...matching].sort((a, b) => {
      const left = sort.index < 0 ? a.total : a.values[sort.index];
      const right = sort.index < 0 ? b.total : b.values[sort.index];
      return (Number(left) - Number(right)) * (sort.ascending ? 1 : -1) || a.name.localeCompare(b.name, 'vi');
    });
  }, [matrix.rows, filter, sort]);
  const totals = filter ? matrix.columns.map((_, index) => rows.reduce((sum, row) => sum + Number(row.values[index] ?? 0), 0)) : matrix.totals;
  const grandTotal = filter ? rows.reduce((sum, row) => sum + Number(row.total), 0) : matrix.grandTotal;
  const changeSort = (index: number) => setSort((current) => ({ index, ascending: current.index === index ? !current.ascending : false }));
  return <div className="overflow-x-auto rounded-xl border border-[#E0E4DA]">
    <table className="w-full min-w-[620px] text-sm">
      <thead className="bg-[#F2F5EA] text-[#37422C]"><tr>
        <th scope="col" className="sticky left-0 bg-[#F2F5EA] px-4 py-3 text-left font-bold">{firstLabel}</th>
        {matrix.columns.map((column, index) => <th key={column} scope="col" className="px-3 py-3 text-right">
          <button type="button" onClick={() => changeSort(index)} className="font-bold underline-offset-2 hover:underline" aria-label={`Sắp xếp theo ${column}`}>{column}</button>
        </th>)}
        <th scope="col" className="px-4 py-3 text-right"><button type="button" onClick={() => changeSort(-1)} className="font-bold">TC</button></th>
      </tr></thead>
      <tbody>
        {rows.map((row) => <tr key={row.id ?? row.name} className="border-t border-[#E7EAE2] even:bg-[#FAFBF8]">
          <th scope="row" className="sticky left-0 bg-inherit px-4 py-3 text-left font-medium">{row.name}</th>
          {row.values.map((value, index) => <td key={index} onDoubleClick={() => onCellDoubleClick?.(row, index)} title="Bấm đúp để xem hóa đơn" className="cursor-pointer px-3 py-3 text-right font-mono text-xs tabular-nums hover:bg-[#EEF5DD]">{money(value)}</td>)}
          <td className="px-4 py-3 text-right font-mono text-xs font-bold tabular-nums">{money(row.total)}</td>
        </tr>)}
        <tr className="sticky bottom-0 border-t-2 border-[#B8C4A2] bg-[#F1F9DC] font-bold">
          <th scope="row" className="sticky left-0 bg-[#F1F9DC] px-4 py-3 text-left">TC</th>
          {totals.map((value, index) => <td key={index} className="px-3 py-3 text-right font-mono text-xs tabular-nums">{money(value)}</td>)}
          <td className="px-4 py-3 text-right font-mono text-xs tabular-nums">{money(grandTotal)}</td>
        </tr>
      </tbody>
    </table>
    {rows.length === 0 && <p className="p-4 text-sm text-[#697165]">Không có dữ liệu trong khoảng đã chọn.</p>}
  </div>;
}

function ReportsContent() {
  const current = today();
  const currentYear = Number(current.slice(0, 4));
  const [tab, setTab] = useState<Tab>('day');
  const [from, setFrom] = useState(`${currentYear}-01-01`);
  const [to, setTo] = useState(current);
  const [year, setYear] = useState(currentYear);
  const [fromYear, setFromYear] = useState(currentYear - 2);
  const [toYear, setToYear] = useState(currentYear);
  const [availableYears, setAvailableYears] = useState<number[]>([currentYear]);
  const [mode, setMode] = useState<PeriodMode>('quarters');
  const [peopleType, setPeopleType] = useState<'employees' | 'customers'>('employees');
  const [productSearch, setProductSearch] = useState('');
  const [applied, setApplied] = useState({ from, to, year, fromYear, toYear, version: 0 });
  const [overview, setOverview] = useState<Overview | null>(null);
  const [range, setRange] = useState<Range | null>(null);
  const [transactions, setTransactions] = useState<TransactionPage | null>(null);
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState<Transaction | null>(null);
  const [detail, setDetail] = useState<Detail | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [exporting, setExporting] = useState(false);
  const [drill, setDrill] = useState<Drill | null>(null);
  const [drillPage, setDrillPage] = useState(0);
  const [drillRows, setDrillRows] = useState<TransactionPage | null>(null);
  const [drillError, setDrillError] = useState(false);
  const invalidDates = !from || !to || from > to;
  const invalidYears = fromYear > toYear;

  useEffect(() => {
    let active = true;
    api.get<{ years: number[] }>('/analytics/available-years').then(({ data }) => {
      if (!active || !data.years?.length) return;
      setAvailableYears(data.years);
      setFromYear(data.years[0]);
      setToYear(data.years[data.years.length - 1]);
      setApplied((value) => ({ ...value, fromYear: data.years[0], toYear: data.years[data.years.length - 1] }));
    }).catch(() => { if (active) setError('Không tải được danh sách năm có dữ liệu.'); });
    return () => { active = false; };
  }, []);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError('');
    Promise.all([
      api.get<Overview>('/analytics/overview', { params: { from: applied.from, to: applied.to,
        year: applied.year, fromYear: applied.fromYear, toYear: applied.toYear } }),
      api.get<Range>('/analytics/invoice-range', { params: { from: applied.from, to: applied.to } }),
    ]).then(([overviewResult, rangeResult]) => {
      if (active) { setOverview(overviewResult.data); setRange(rangeResult.data); setLoading(false); }
    }).catch(() => { if (active) { setError('Không tải được thống kê. Vui lòng thử lại.'); setLoading(false); } });
    return () => { active = false; };
  }, [applied]);

  useEffect(() => {
    let active = true;
    api.get<TransactionPage>('/analytics/transactions', { params: { from: applied.from, to: applied.to, page, size: 20 } })
      .then(({ data }) => {
        if (!active) return;
        setTransactions(data);
        setSelected((currentSelection) => data.content.find((row) => row.id === currentSelection?.id && row.type === currentSelection.type) ?? data.content[0] ?? null);
      }).catch(() => { if (active) { setTransactions(null); setSelected(null); setError('Không tải được danh sách chứng từ.'); } });
    return () => { active = false; };
  }, [applied.from, applied.to, applied.version, page]);

  useEffect(() => {
    if (!selected) { setDetail(null); setDetailLoading(false); return; }
    let active = true;
    setDetail(null);
    setDetailError(false);
    setDetailLoading(true);
    const url = selected.type === 'PURCHASE' ? `/purchase-receipts/${selected.detailId}` : `/invoices/${selected.detailId}`;
    api.get<Detail>(url).then(({ data }) => { if (active) setDetail(data); })
      .catch(() => { if (active) setDetailError(true); })
      .finally(() => { if (active) setDetailLoading(false); });
    return () => { active = false; };
  }, [selected]);

  useEffect(() => {
    if (!drill) { setDrillRows(null); return; }
    let active = true;
    setDrillError(false);
    api.get<TransactionPage>('/analytics/transactions', { params: { ...drill, label: undefined, page: drillPage, size: 20 } })
      .then(({ data }) => { if (active) setDrillRows(data); })
      .catch(() => { if (active) { setDrillRows(null); setDrillError(true); } });
    return () => { active = false; };
  }, [drill, drillPage]);

  function openDrill(value: Drill) { setDrillRows(null); setDrillPage(0); setDrill(value); }

  function periodDates(period: Period): Pick<Drill, 'from' | 'to'> {
    if (mode === 'years') return { from: `${period.label}-01-01`, to: `${period.label}-12-31` };
    const month = mode === 'months' ? Number(period.label.slice(0, 2)) : (Number(period.label.slice(1)) - 1) * 3 + 1;
    const lastMonth = mode === 'quarters' ? month + 2 : month;
    return { from: `${applied.year}-${String(month).padStart(2, '0')}-01`,
      to: `${applied.year}-${String(lastMonth).padStart(2, '0')}-${dayEnd(applied.year, lastMonth)}` };
  }

  function applyFilters() {
    if ((tab === 'day' && invalidDates) || (tab === 'people' && invalidYears)) return;
    setPage(0);
    setApplied({ from, to, year, fromYear, toYear, version: applied.version + 1 });
  }

  async function exportReport(extension: 'xlsx' | 'pdf') {
    if (exporting) return;
    setExporting(true);
    try {
      const exportFrom = tab === 'day' ? applied.from : tab === 'people' ? `${applied.fromYear}-01-01` : `${applied.year}-01-01`;
      const exportTo = tab === 'day' ? applied.to : tab === 'people' ? `${applied.toYear}-12-31` : `${applied.year}-12-31`;
      const response = await api.get(`/analytics/export.${extension}`, {
        params: { from: exportFrom, to: exportTo, year: applied.year,
          fromYear: applied.fromYear, toYear: applied.toYear }, responseType: 'blob',
      });
      const url = URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.download = `fitos-thong-ke-${applied.year}.${extension}`;
      link.click();
      URL.revokeObjectURL(url);
    } catch { setError(`Không xuất được tệp ${extension.toUpperCase()}. Vui lòng thử lại.`); }
    finally { setExporting(false); }
  }

  const periods = overview?.[mode] ?? [];
  const matrix = tab === 'products' ? overview?.products : overview?.[peopleType];
  const maxChart = Math.max(1, ...periods.flatMap((item) => [Number(item.income), Number(item.expense)]));
  const yearOptions = availableYears.includes(year) ? availableYears : [...availableYears, year].sort((a, b) => a - b);

  return <main aria-label="Trang báo cáo thống kê" className="min-h-screen bg-[#F5F5F0] text-[#171B16]">
    <div className="border-b border-[#DADDD3] bg-white px-4 py-6 md:px-8">
      <div className="mx-auto max-w-7xl">
        <h1 className="text-2xl font-bold">Thống kê</h1>
        <p className="mt-1 text-sm text-[#5E625A]">Theo dõi hóa đơn, thu chi và doanh thu của studio.</p>
        <div role="tablist" aria-label="Các mục thống kê" className="mt-6 flex gap-1 overflow-x-auto border-b border-[#DEE2D5]">
          {tabs.map((item) => <button key={item.id} type="button" role="tab" id={`tab-${item.id}`}
            aria-controls={`panel-${item.id}`} aria-selected={tab === item.id}
            onClick={() => setTab(item.id)} className={`whitespace-nowrap border-b-2 px-4 py-3 text-sm font-semibold ${tab === item.id ? 'border-[#171B16] text-[#171B16]' : 'border-transparent text-[#596154] hover:text-[#171B16]'}`}>{item.label}</button>)}
        </div>
      </div>
    </div>

    <div className="mx-auto max-w-7xl space-y-5 px-4 py-6 md:px-8">
      <section aria-label="Bộ lọc thống kê" className="rounded-xl border border-[#DADDD3] bg-white p-4 md:p-5">
        <div className="flex flex-wrap items-end gap-3">
          {tab === 'day' && <div className="flex flex-col gap-1"><div className="flex flex-wrap gap-3">
            <label className="flex flex-col gap-1 text-sm font-medium">Từ ngày<input type="date" value={from} onChange={(event) => setFrom(event.target.value)} aria-invalid={invalidDates} className={inputClass} /></label>
            <label className="flex flex-col gap-1 text-sm font-medium">Đến ngày<input type="date" value={to} onChange={(event) => setTo(event.target.value)} aria-invalid={invalidDates} className={inputClass} /></label>
          </div>{invalidDates && <p role="alert" className="text-xs text-red-700">Chọn đủ ngày; Từ ngày không được sau Đến ngày.</p>}</div>}
          {(tab === 'period' || tab === 'products') && <label className="flex flex-col gap-1 text-sm font-medium">Năm<select value={year} onChange={(event) => setYear(Number(event.target.value))} className={inputClass}>
            {yearOptions.map((item) => <option key={item} value={item}>{item}</option>)}
          </select></label>}
          {tab === 'period' && <fieldset className="flex items-center gap-3 pb-2 text-sm"><legend className="sr-only">Thống kê theo kỳ</legend>
            {([['quarters', 'Quý'], ['months', 'Tháng'], ['years', 'Năm']] as const).map(([value, label]) => <label key={value} className="flex items-center gap-1.5"><input type="radio" name="period-mode" checked={mode === value} onChange={() => setMode(value)} />{label}</label>)}
          </fieldset>}
          {tab === 'people' && <>
            <label className="flex flex-col gap-1 text-sm font-medium">Thống kê theo<select value={peopleType} onChange={(event) => setPeopleType(event.target.value as 'employees' | 'customers')} className={inputClass}>
              <option value="employees">Nhân viên</option><option value="customers">Khách hàng</option>
            </select></label>
            <label className="flex flex-col gap-1 text-sm font-medium">Từ năm<select value={fromYear} onChange={(event) => setFromYear(Number(event.target.value))} className={inputClass}>{availableYears.map((item) => <option key={item} value={item}>{item}</option>)}</select></label>
            <label className="flex flex-col gap-1 text-sm font-medium">Đến năm<select value={toYear} onChange={(event) => setToYear(Number(event.target.value))} className={inputClass}>{availableYears.map((item) => <option key={item} value={item}>{item}</option>)}</select></label>
          </>}
          {tab === 'products' && <label className="flex min-w-[220px] flex-1 flex-col gap-1 text-sm font-medium">Tìm sản phẩm<input type="search" value={productSearch} onChange={(event) => setProductSearch(event.target.value)} placeholder="Tên sản phẩm" className={inputClass} /></label>}
          <button type="button" onClick={applyFilters} disabled={loading || (tab === 'day' && invalidDates) || (tab === 'people' && invalidYears)} className={`${buttonClass} bg-[#D7FF3F] text-[#171B16]`}>Thống kê</button>
          <button type="button" onClick={() => setApplied((value) => ({ ...value, version: value.version + 1 }))} disabled={loading} className={`${buttonClass} border border-[#D2D7CA] bg-white`}>Làm mới</button>
        </div>
        {tab === 'people' && invalidYears && <p role="alert" className="mt-2 text-sm text-red-700">Từ năm không được sau Đến năm.</p>}
      </section>

      {error && <p role="alert" className="rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700">{error}</p>}
      {loading && <p role="status" className="text-sm text-[#5E625A]">Đang tải thống kê...</p>}

      {tab === 'day' && <section role="tabpanel" id="panel-day" aria-labelledby="tab-day" className="space-y-5">
        <h2 className="text-xl font-bold">Tổng tiền hóa đơn theo khoảng ngày</h2>
        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-5">
          <SummaryCard label="Bán POS" value={money(range?.posSales)} />
          <SummaryCard label="Bán gói" value={money(range?.packageSales)} />
          <SummaryCard label="Nhập hàng" value={money(range?.purchaseTotal)} />
          <SummaryCard label="Tổng giá trị hóa đơn" value={money(range?.invoiceTotal)} />
          <SummaryCard label="Số chứng từ" value={String(transactions?.totalElements ?? 0)} />
        </div>
        <p className="text-sm text-[#5E625A]">{transactions?.totalElements ?? 0} chứng từ trong khoảng ngày. Bán hàng đã trừ hoàn tiền POS; tổng bán và nhập không phải lợi nhuận.</p>
        <div className="overflow-x-auto rounded-xl border border-[#DADDD3] bg-white">
          <table className="w-full min-w-[620px] text-sm"><thead className="bg-[#F2F5EA]"><tr>
            <th scope="col" className="px-4 py-3 text-left">Loại hóa đơn</th><th scope="col" className="px-4 py-3 text-right">POS</th><th scope="col" className="px-4 py-3 text-right">Gói</th><th scope="col" className="px-4 py-3 text-right">Tổng tiền</th>
          </tr></thead><tbody>
            <tr className="border-t border-[#E7EAE2]"><th scope="row" className="px-4 py-3 text-left">Bán hàng</th><td className="px-4 py-3 text-right font-mono text-xs">{money(range?.posSales)}</td><td className="px-4 py-3 text-right font-mono text-xs">{money(range?.packageSales)}</td><td className="px-4 py-3 text-right font-mono text-xs font-bold">{money(range?.salesTotal)}</td></tr>
            <tr className="border-t border-[#E7EAE2] bg-[#FAFBF8]"><th scope="row" className="px-4 py-3 text-left">Nhập hàng</th><td className="px-4 py-3 text-right">—</td><td className="px-4 py-3 text-right">—</td><td className="px-4 py-3 text-right font-mono text-xs font-bold">{money(range?.purchaseTotal)}</td></tr>
          </tbody></table>
        </div>
        <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_minmax(280px,0.8fr)]">
          <section aria-label="Danh sách chứng từ" className="rounded-xl border border-[#DADDD3] bg-white p-4">
            <h3 className="font-bold">Danh sách hóa đơn và phiếu nhập</h3>
            {!transactions?.content.length ? <p className="mt-4 text-sm text-[#697165]">Không có dữ liệu trong khoảng đã chọn.</p> : <div className="mt-3 max-h-[470px] space-y-2 overflow-y-auto">
              {transactions.content.map((row) => <button key={`${row.type}-${row.id}`} type="button" onClick={() => setSelected(row)} aria-pressed={selected?.id === row.id && selected.type === row.type}
                className={`flex w-full items-center justify-between gap-3 rounded-lg border p-3 text-left text-sm ${selected?.id === row.id && selected.type === row.type ? 'border-[#657F00] bg-[#F1F9DC]' : 'border-[#E0E4DA] hover:bg-[#F9FAF6]'}`}>
                <span><strong className="block">{row.reference}</strong><span className="mt-1 block text-xs text-[#626A5E]">{row.type === 'PURCHASE' ? 'Nhập hàng' : row.type === 'PACKAGE' ? 'Bán gói' : row.type === 'POS_RETURN' ? 'Hoàn tiền POS' : 'Bán POS'} · {row.partyName} · {dateOnly(row.at)}</span></span>
                <span className="whitespace-nowrap font-mono text-xs font-bold">{money(row.amount)}</span>
              </button>)}
            </div>}
            <div className="mt-4 flex items-center justify-between border-t border-[#E0E4DA] pt-3 text-sm">
              <button type="button" disabled={page === 0} onClick={() => setPage((value) => value - 1)} className="disabled:opacity-40">Trước</button>
              <span>Trang {page + 1} / {Math.max(1, Math.ceil((transactions?.totalElements ?? 0) / 20))}</span>
              <button type="button" disabled={(page + 1) * 20 >= (transactions?.totalElements ?? 0)} onClick={() => setPage((value) => value + 1)} className="disabled:opacity-40">Sau</button>
            </div>
          </section>
          <section aria-label="Chi tiết chứng từ" className="rounded-xl border border-[#DADDD3] bg-white p-4">
            <h3 className="font-bold">Chi tiết chứng từ</h3>
            {!selected ? <p className="mt-4 text-sm text-[#697165]">Chọn một chứng từ ở danh sách bên trái.</p> : <>
              <div className="mt-3 rounded-lg bg-[#F5F7F1] p-4 text-sm">
                <p className="font-bold">{selected.reference}</p><p className="mt-1">{selected.partyName}</p>
                <p className="mt-1 text-[#5E625A]">{dateOnly(selected.at)} · {selected.employeeName}</p>
                <p className="mt-3 font-bold">{money(selected.amount)}</p>
              </div>
              {detailLoading ? <p className="mt-4 text-sm text-[#697165]">Đang tải chi tiết...</p> : detailError ? <p role="alert" className="mt-4 text-sm text-red-700">Không tải được chi tiết chứng từ.</p> : detail?.items?.length ? <table className="mt-4 w-full text-sm"><thead><tr><th className="py-2 text-left">Mặt hàng</th><th className="py-2 text-right">SL</th><th className="py-2 text-right">Thành tiền</th></tr></thead><tbody>{detail.items.map((item) => <tr key={item.id} className="border-t border-[#E7EAE2]"><td className="py-2">{item.name ?? item.productName ?? '—'}</td><td className="py-2 text-right">{item.quantity}</td><td className="py-2 text-right font-mono text-xs">{money(item.totalPrice ?? item.lineTotal)}</td></tr>)}</tbody></table> : <p className="mt-4 text-sm text-[#697165]">Không có dòng chi tiết.</p>}
            </>}
          </section>
        </div>
      </section>}

      {tab === 'period' && <section role="tabpanel" id="panel-period" aria-labelledby="tab-period" className="space-y-5">
        {periods.every((row) => Number(row.income) === 0 && Number(row.expense) === 0) && <p className="rounded-lg border border-[#DEE2D5] bg-white p-4 text-sm text-[#697165]">Không có dữ liệu trong khoảng đã chọn.</p>}
        <div className="grid gap-3 sm:grid-cols-3"><SummaryCard label="Tổng thu" value={money(periods.reduce((sum, row) => sum + Number(row.income), 0))} /><SummaryCard label="Tổng chi" value={money(periods.reduce((sum, row) => sum + Number(row.expense), 0))} /><SummaryCard label="Lợi nhuận (thu − chi)" value={money(periods.reduce((sum, row) => sum + Number(row.balance), 0))} tone={periods.reduce((sum, row) => sum + Number(row.balance), 0) < 0 ? 'negative' : 'positive'} /></div>
        <div className="overflow-x-auto rounded-xl border border-[#DADDD3] bg-white"><table className="w-full min-w-[700px] text-sm"><thead className="bg-[#F2F5EA]"><tr><th scope="col" className="px-4 py-3 text-left">{mode === 'years' ? 'Năm' : applied.year}</th>{periods.map((row) => <th scope="col" key={row.label} className="px-3 py-3 text-right">{mode === 'months' ? `T${row.label.slice(0, 2)}` : row.label}</th>)}<th scope="col" className="px-4 py-3 text-right">TC</th></tr></thead><tbody>
          {([['Tổng thu (HD)', 'income'], ['Tổng chi (PNH)', 'expense'], ['Lợi nhuận', 'balance']] as const).map(([label, key]) => { const total = periods.reduce((sum, row) => sum + Number(row[key]), 0); return <tr key={key} className="border-t border-[#E7EAE2] even:bg-[#FAFBF8]"><th scope="row" className="whitespace-nowrap px-4 py-3 text-left font-semibold">{label}</th>{periods.map((row) => <td key={row.label} onDoubleClick={() => openDrill({ label: `${label} · ${row.label}`, ...periodDates(row), kind: key === 'income' ? 'SALE' : key === 'expense' ? 'PURCHASE' : undefined })} title="Bấm đúp để xem hóa đơn" className={`cursor-pointer px-3 py-3 text-right font-mono text-xs tabular-nums hover:bg-[#EEF5DD] ${key === 'balance' ? Number(row.balance) < 0 ? 'text-[#B23732]' : 'text-[#246D44]' : ''}`}>{money(row[key])}</td>)}<td className={`px-4 py-3 text-right font-mono text-xs font-bold ${key === 'balance' ? total < 0 ? 'text-[#B23732]' : 'text-[#246D44]' : ''}`}>{money(total)}</td></tr>; })}
        </tbody></table></div>
        <section aria-label="Biểu đồ thu chi" className="rounded-xl border border-[#DADDD3] bg-white p-5"><h3 className="font-bold">Thu và chi theo kỳ</h3><div className="mt-5 flex h-48 min-w-0 items-end gap-2 overflow-x-auto border-b border-[#DADDD3] pb-1">{periods.map((row) => <div key={row.label} className="flex h-full min-w-[58px] flex-1 flex-col justify-end"><div className="flex h-[160px] items-end justify-center gap-1"><div title={`${row.label} thu ${money(row.income)}`} className="w-4 rounded-t bg-[#8BBE43]" style={{ height: `${Math.max(2, Number(row.income) / maxChart * 150)}px` }} /><div title={`${row.label} chi ${money(row.expense)}`} className="w-4 rounded-t bg-[#3A5B61]" style={{ height: `${Math.max(2, Number(row.expense) / maxChart * 150)}px` }} /></div><span className="mt-1 text-center text-xs">{mode === 'months' ? `T${row.label.slice(0, 2)}` : row.label}</span></div>)}</div><p className="mt-3 text-xs text-[#5E625A]"><span className="mr-4 text-[#598326]">■ Thu</span><span className="text-[#3A5B61]">■ Chi</span></p></section>
        <p className="text-xs text-[#697165]">Lợi nhuận ở đây là thu trừ tiền nhập hàng, chưa tính lương, mặt bằng và các chi phí khác.</p>
      </section>}

      {tab === 'people' && matrix && <section role="tabpanel" id="panel-people" aria-labelledby="tab-people" className="space-y-5">
        <div className="grid gap-3 sm:grid-cols-2"><SummaryCard label="Tổng tiền hóa đơn" value={money(matrix.grandTotal)} /><SummaryCard label={peopleType === 'employees' ? 'Nhân viên có doanh thu' : 'Khách hàng có hóa đơn'} value={String(matrix.rows.length)} /></div>
        <MatrixTable matrix={matrix} firstLabel={peopleType === 'employees' ? 'Nhân viên' : 'Khách hàng'} onCellDoubleClick={(row, column) => openDrill({ label: `${row.name} · ${matrix.columns[column]}`, from: `${matrix.columns[column]}-01-01`, to: `${matrix.columns[column]}-12-31`, kind: 'SALE', [peopleType === 'employees' ? 'employeeId' : 'customerId']: row.id ?? -1 })} />
      </section>}

      {tab === 'products' && overview && <section role="tabpanel" id="panel-products" aria-labelledby="tab-products" className="space-y-5">
        <div className="grid gap-3 sm:grid-cols-2"><SummaryCard label="Doanh thu sản phẩm" value={money(overview.products.grandTotal)} /><SummaryCard label="Sản phẩm có giao dịch" value={String(overview.products.rows.length)} /></div>
        <MatrixTable matrix={overview.products} firstLabel="Sản phẩm" filter={productSearch} onCellDoubleClick={(row, column) => { const startMonth = column * 3 + 1; const endMonth = startMonth + 2; openDrill({ label: `${row.name} · Q${column + 1}`, from: `${applied.year}-${String(startMonth).padStart(2, '0')}-01`, to: `${applied.year}-${String(endMonth).padStart(2, '0')}-${dayEnd(applied.year, endMonth)}`, kind: 'SALE', productId: row.id ?? undefined }); }} />
        <section className="rounded-xl border border-[#DADDD3] bg-white p-5"><h3 className="font-bold">Top 5 sản phẩm theo doanh thu</h3><div className="mt-4 space-y-3">{overview.products.rows.slice(0, 5).map((row) => <div key={row.id} className="text-sm"><div className="mb-1 flex justify-between gap-4"><span>{row.name}</span><strong className="font-mono text-xs">{money(row.total)}</strong></div><div className="h-2 rounded-full bg-[#EEF1E8]"><div className="h-2 rounded-full bg-[#8BBE43]" style={{ width: `${Math.max(0, Number(row.total) / Math.max(1, Number(overview.products.rows[0]?.total)) * 100)}%` }} /></div></div>)}{overview.products.rows.length === 0 && <p className="text-sm text-[#697165]">Không có dữ liệu trong khoảng đã chọn.</p>}</div></section>
      </section>}

      <footer className="flex flex-wrap items-center justify-between gap-3 border-t border-[#DADDD3] pt-5"><p className="text-xs text-[#697165]">Tệp xuất chứa đầy đủ các mục thống kê với bộ lọc đã áp dụng.</p><div className="flex gap-2"><button type="button" disabled={exporting || loading} onClick={() => void exportReport('xlsx')} className={`${buttonClass} border border-[#D2D7CA] bg-white`}>Xuất Excel</button><button type="button" disabled={exporting || loading} onClick={() => void exportReport('pdf')} className={`${buttonClass} border border-[#D2D7CA] bg-white`}>Xuất PDF</button></div></footer>
    </div>
    {drill && <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"><section role="dialog" aria-modal="true" aria-label="Hóa đơn tạo nên số liệu" className="max-h-[80vh] w-full max-w-2xl overflow-y-auto rounded-2xl bg-white p-5 shadow-xl">
      <div className="flex items-start justify-between gap-4"><div><h2 className="text-lg font-bold">Hóa đơn tạo nên số liệu</h2><p className="mt-1 text-sm text-[#5E625A]">{drill.label} · {drill.from} đến {drill.to}</p></div><button type="button" onClick={() => setDrill(null)} className="rounded-lg border border-[#D2D7CA] px-3 py-2 text-sm">Đóng</button></div>
      {drillError ? <p role="alert" className="mt-4 text-sm text-red-700">Không tải được chứng từ tạo nên số liệu.</p> : !drillRows ? <p className="mt-4 text-sm">Đang tải chứng từ...</p> : drillRows.content.length === 0 ? <p className="mt-4 text-sm text-[#697165]">Không có dữ liệu trong khoảng đã chọn.</p> : <div className="mt-4 divide-y divide-[#E7EAE2]">{drillRows.content.map((row) => <div key={`${row.type}-${row.id}`} className="flex justify-between gap-4 py-3 text-sm"><div><strong>{row.reference}</strong><p className="mt-1 text-xs text-[#697165]">{row.partyName} · {dateOnly(row.at)}</p></div><strong className="whitespace-nowrap font-mono text-xs">{money(row.amount)}</strong></div>)}</div>}
      <div className="mt-4 flex justify-between border-t border-[#E7EAE2] pt-3 text-sm"><button type="button" disabled={drillPage === 0} onClick={() => setDrillPage((value) => value - 1)} className="disabled:opacity-40">Trước</button><span>{drillRows?.totalElements ?? 0} chứng từ · Trang {drillPage + 1}</span><button type="button" disabled={(drillPage + 1) * 20 >= (drillRows?.totalElements ?? 0)} onClick={() => setDrillPage((value) => value + 1)} className="disabled:opacity-40">Sau</button></div>
    </section></div>}
  </main>;
}

export default function ReportsPage() {
  return <RoleGuard allowedRoles={['MANAGER']}><ReportsContent /></RoleGuard>;
}
