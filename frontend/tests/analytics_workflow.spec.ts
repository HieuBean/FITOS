import { expect, test, type Page } from '@playwright/test';
import { readFileSync } from 'node:fs';
import path from 'node:path';

const frontend = process.env.FITOS_TEST_FRONTEND_URL ?? 'http://127.0.0.1:3000';
const credentials = JSON.parse(readFileSync(path.resolve('..', '.dsh', 'demo-accounts.json'), 'utf8'));

test.beforeEach(() => test.skip(process.env.FITOS_ROLE_SEPARATION_TEST !== '1', 'Local demo test only'));

async function loginManager(page: Page) {
  await page.goto(`${frontend}/login`);
  await page.getByRole('button', { name: /Studio/ }).click();
  await page.locator('#login-email').fill('manager.demo@example.com');
  await page.locator('#login-password').fill(credentials.password);
  await page.locator('button[type="submit"]').click();
  await page.waitForURL('**/dashboard');
}

test('product search uses the loaded list without a request for each keystroke', async ({ page }) => {
  await loginManager(page);
  let productRequests = 0;
  page.on('request', (request) => {
    if (request.url().includes('/api/retail/products?') && request.method() === 'GET') productRequests += 1;
  });
  await page.goto(`${frontend}/dashboard/retail`);
  await expect(page.getByRole('searchbox', { name: 'Tìm sản phẩm' })).toBeVisible();
  await expect.poll(() => productRequests).toBeGreaterThan(0);
  const loadedRequests = productRequests;
  await page.getByRole('searchbox', { name: 'Tìm sản phẩm' }).fill('WHEY');
  await page.getByRole('searchbox', { name: 'Tìm sản phẩm' }).fill('WHEY-CONC');
  await page.waitForTimeout(300);
  expect(productRequests).toBe(loadedRequests);
});

test('product search ignores Vietnamese accents and case and filters inclusive sale prices', async ({ page }) => {
  await loginManager(page);
  await page.goto(`${frontend}/dashboard/retail`);
  const products = page.getByRole('region', { name: 'Danh sách sản phẩm' });
  const search = products.getByRole('searchbox', { name: 'Tìm sản phẩm' });
  await expect(products.getByText('Bình lắc Shaker 600ml')).toBeVisible();
  await search.fill('binh lac');
  await expect(products.getByText('Bình lắc Shaker 600ml')).toBeVisible();
  await expect(products.getByText('Bình lắc FITOS Demo 750ml')).toBeVisible();
  await search.fill('BINH LAC');
  await expect(products.getByText('Bình lắc Shaker 600ml')).toBeVisible();
  await search.fill('day deo the');
  await expect(products.getByText('Dây đeo thẻ hội viên')).toBeVisible();
  await search.fill('binh lac');
  await products.getByRole('spinbutton', { name: 'Giá bán từ' }).fill('100000');
  await products.getByRole('spinbutton', { name: 'Giá bán đến' }).fill('150000');
  await expect(products.getByText('Bình lắc Shaker 600ml')).toBeVisible();
  await expect(products.getByText('Bình lắc FITOS Demo 750ml')).toHaveCount(0);
  await products.getByRole('spinbutton', { name: 'Giá bán từ' }).fill('120000');
  await products.getByRole('spinbutton', { name: 'Giá bán đến' }).fill('120000');
  await expect(products.getByText('Bình lắc Shaker 600ml')).toBeVisible();
  await products.getByRole('spinbutton', { name: 'Giá bán từ' }).fill('120001');
  await expect(products.getByRole('alert')).toContainText('Khoảng giá bán không hợp lệ');
});

test('invoice main list opens detail', async ({ page }) => {
  await loginManager(page);
  await page.goto(`${frontend}/dashboard/invoices`);
  const list = page.getByRole('region', { name: 'Danh sách hóa đơn' });
  const detail = page.getByRole('region', { name: 'Chi tiết hóa đơn' });
  await expect(list.getByRole('button').first()).toBeVisible();
  await expect(detail.getByRole('table')).toBeVisible();
});

test('manager report page shows two invoice rows and separate POS and plan totals', async ({ page }) => {
  await loginManager(page);
  await page.goto(`${frontend}/dashboard/reports`);
  const report = page.getByRole('main', { name: 'Trang báo cáo thống kê' });
  await expect(report).toBeVisible();
  await expect(report.getByRole('heading', { name: 'Tổng tiền hóa đơn theo khoảng ngày' })).toBeVisible();
  await expect(report.getByLabel('Từ ngày')).toBeVisible();
  await expect(report.getByLabel('Đến ngày')).toBeVisible();
  const table = report.getByRole('table');
  await expect(table.getByRole('row')).toHaveCount(3);
  await expect(table.getByRole('columnheader', { name: 'POS' })).toBeVisible();
  await expect(table.getByRole('columnheader', { name: 'Gói' })).toBeVisible();
  await expect(table.getByRole('row', { name: /Bán hàng/ })).toBeVisible();
  await expect(table.getByRole('row', { name: /Nhập hàng/ })).toBeVisible();
  const responsePromise = page.waitForResponse((response) =>
    response.url().includes('/api/analytics/invoice-range?')
      && response.url().includes('to=2026-01-01') && response.status() === 200);
  await report.getByLabel('Đến ngày').fill('2026-01-01');
  await report.getByRole('button', { name: 'Thống kê' }).click();
  const response = await responsePromise;
  const data = await response.json();
  expect(data.from).toBe('2026-01-01');
  expect(data.to).toBe('2026-01-01');
  expect(Number(data.salesTotal)).toBe(Number(data.posSales) + Number(data.packageSales));
  expect(Number(data.invoiceTotal)).toBe(Number(data.salesTotal) + Number(data.purchaseTotal));
});

test('statistics has four tabs with filters, result tables and export actions', async ({ page }) => {
  await loginManager(page);
  await page.goto(`${frontend}/dashboard/reports`);
  const report = page.getByRole('main', { name: 'Trang báo cáo thống kê' });
  const tabs = report.getByRole('tablist', { name: 'Các mục thống kê' });
  await expect(tabs.getByRole('tab')).toHaveCount(4);
  await expect(report.getByRole('tab', { name: 'Theo ngày' })).toHaveAttribute('aria-selected', 'true');
  await expect(report.getByRole('button', { name: 'Thống kê' })).toBeVisible();
  await expect(report.getByRole('button', { name: 'Xuất Excel' })).toBeVisible();
  await expect(report.getByRole('button', { name: 'Xuất PDF' })).toBeVisible();

  await report.getByRole('tab', { name: 'Thu–Chi–LN' }).click();
  await expect(report.getByRole('radio', { name: 'Quý' })).toBeChecked();
  await expect(report.getByRole('row', { name: /Lợi nhuận/ })).toBeVisible();
  await report.getByRole('row', { name: /Tổng thu \(HD\)/ }).getByRole('cell').nth(3).dblclick();
  await expect(report.getByRole('dialog', { name: 'Hóa đơn tạo nên số liệu' })).toBeVisible();
  await report.getByRole('dialog').getByRole('button', { name: 'Đóng' }).click();
  await report.getByRole('radio', { name: 'Tháng' }).check();
  await expect(report.getByRole('columnheader', { name: 'T12' })).toBeVisible();

  await report.getByRole('tab', { name: 'Theo NV / KH' }).click();
  await expect(report.getByRole('combobox', { name: 'Thống kê theo' })).toHaveValue('employees');
  await report.getByRole('combobox', { name: 'Thống kê theo' }).selectOption('customers');
  await expect(report.getByRole('table')).toBeVisible();

  await report.getByRole('tab', { name: 'Theo Sản phẩm' }).click();
  await expect(report.getByRole('searchbox', { name: 'Tìm sản phẩm' })).toBeVisible();
  await expect(report.getByRole('columnheader', { name: 'Q4' })).toBeVisible();
});

test('date statistics returns paged sale and purchase documents and available years', async ({ page }) => {
  await loginManager(page);
  const token = await page.evaluate(() => sessionStorage.getItem('fitos_access_token'));
  const headers = { Authorization: `Bearer ${token}` };
  const years = await page.request.get('http://127.0.0.1:18081/api/analytics/available-years', { headers });
  expect(years.ok()).toBeTruthy();
  expect((await years.json()).years).toContain(2026);
  const transactions = await page.request.get('http://127.0.0.1:18081/api/analytics/transactions?from=2026-01-01&to=2026-11-18&page=0&size=20', { headers });
  expect(transactions.ok()).toBeTruthy();
  const data = await transactions.json();
  expect(data.totalElements).toBeGreaterThan(0);
  expect(data.content.some((row: { type: string }) => row.type === 'PURCHASE')).toBeTruthy();
  expect(data.content.some((row: { type: string }) => row.type === 'POS' || row.type === 'PACKAGE')).toBeTruthy();
  const purchases = await page.request.get('http://127.0.0.1:18081/api/analytics/transactions?from=2026-01-01&to=2026-11-18&kind=PURCHASE', { headers });
  const purchaseData = await purchases.json();
  expect(purchaseData.content.length).toBeGreaterThan(0);
  expect(purchaseData.content.every((row: { type: string }) => row.type === 'PURCHASE')).toBeTruthy();
});

test('statistics downloads Excel and PDF for the applied filters', async ({ page }) => {
  await loginManager(page);
  await page.goto(`${frontend}/dashboard/reports`);
  const report = page.getByRole('main', { name: 'Trang báo cáo thống kê' });
  await expect(report.getByRole('button', { name: 'Xuất Excel' })).toBeEnabled();
  const excelPromise = page.waitForEvent('download');
  await report.getByRole('button', { name: 'Xuất Excel' }).click();
  expect((await excelPromise).suggestedFilename()).toMatch(/\.xlsx$/);
  const pdfPromise = page.waitForEvent('download');
  await report.getByRole('button', { name: 'Xuất PDF' }).click();
  expect((await pdfPromise).suggestedFilename()).toMatch(/\.pdf$/);
});

test('simple search remains after removing advanced filters', async ({ page }) => {
  await loginManager(page);
  await page.goto(`${frontend}/dashboard/retail`);
  await expect(page.getByRole('searchbox', { name: 'Tìm sản phẩm' })).toBeVisible();
  await expect(page.getByRole('combobox', { name: 'Lọc theo danh mục' })).toBeVisible();
  await expect(page.getByRole('button', { name: '+ Điều kiện' })).toHaveCount(0);
  await page.getByRole('searchbox', { name: 'Tìm sản phẩm' }).fill('FITOS_NO_MATCH_9E7B');
  await expect(page.getByText('Không tìm thấy sản phẩm phù hợp')).toBeVisible();
  await page.goto(`${frontend}/dashboard/invoices`);
  await expect(page.getByRole('searchbox', { name: 'Tìm hóa đơn' })).toBeVisible();
  await expect(page.getByRole('button', { name: '+ Điều kiện' })).toHaveCount(0);
  await page.getByRole('searchbox', { name: 'Tìm hóa đơn' }).fill('FITOS_NO_MATCH_9E7B');
  await page.getByRole('button', { name: 'Tìm', exact: true }).click();
  await expect(page.getByText('Không có hóa đơn phù hợp.')).toBeVisible();
  await page.getByRole('button', { name: 'Xóa tìm kiếm' }).click();
  await page.setViewportSize({ width: 375, height: 812 });
  await expect(page.getByRole('searchbox', { name: 'Tìm hóa đơn' })).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(375);
});

test('header opens purchase receipts while product creation stays in product actions', async ({ page }) => {
  await loginManager(page);
  await page.goto(`${frontend}/dashboard/retail`);
  await expect(page.getByRole('button', { name: '+ Thêm sản phẩm' })).toBeVisible();
  await page.getByRole('button', { name: 'Phiếu nhập hàng' }).click();
  await expect(page.getByRole('tab', { name: 'Phiếu nhập' })).toHaveAttribute('aria-selected', 'true');
  await expect(page.getByRole('heading', { name: /Phiếu nhập hàng/ }).first()).toBeVisible();
});

test('new product form keeps sale price, VAT, and low stock warning', async ({ page }) => {
  await loginManager(page);
  await page.goto(`${frontend}/dashboard/retail`);
  await page.getByRole('button', { name: '+ Thêm sản phẩm' }).click();
  const dialog = page.getByRole('dialog', { name: 'Thêm sản phẩm' });
  await expect(dialog.locator('#pf-cost')).toHaveCount(0);
  await expect(dialog.locator('#pf-sell')).toBeVisible();
  await expect(dialog.locator('#pf-initial')).toHaveCount(0);
  await expect(dialog.locator('#pf-vat')).toBeVisible();
  await expect(dialog.locator('#pf-threshold')).toBeVisible();
  await expect(dialog.getByText('Giá bán đã gồm VAT')).toBeVisible();
});
