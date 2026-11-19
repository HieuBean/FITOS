import { expect, test, type Page } from '@playwright/test';

const frontend = process.env.FITOS_TEST_FRONTEND_URL ?? 'http://127.0.0.1:3000';
const existingProduct = {
  id: 42,
  sku: 'VAT-EDIT-42',
  name: 'Sản phẩm kiểm tra VAT',
  unit: 'CAI',
  unitLabel: 'cái',
  costPrice: 70000,
  sellPrice: 110000,
  vatRate: 10,
  stockQuantity: 5,
  lowStockThreshold: 2,
  trackInventory: true,
  isActive: true,
  lowStock: false,
  outOfStock: false,
};

async function openRetail(page: Page) {
  await page.addInitScript(() => {
    sessionStorage.setItem('fitos_access_token', 'retail-price-test-token');
    sessionStorage.setItem('fitos_user', JSON.stringify({
      id: 1, email: 'manager@example.com', fullName: 'Manager', role: 'MANAGER', studioId: 1,
    }));
  });
  await page.route('**/api/retail/**', async (route) => {
    const { pathname } = new URL(route.request().url());
    const method = route.request().method();
    if (method === 'GET' && pathname.endsWith('/retail/products')) {
      await route.fulfill({ json: { content: [existingProduct], totalElements: 1, totalPages: 1 } });
    } else if (method === 'GET' && pathname.endsWith('/retail/categories')) {
      await route.fulfill({ json: [] });
    } else if (method === 'GET' && pathname.endsWith('/retail/inventory/movements')) {
      await route.fulfill({ json: { content: [], totalElements: 0 } });
    } else if (method === 'GET' && pathname.endsWith('/retail/inventory/low-stock')) {
      await route.fulfill({ json: [] });
    } else if (method === 'POST' || method === 'PUT') {
      await route.fulfill({ json: { ...existingProduct, id: method === 'POST' ? 43 : 42 } });
    } else {
      await route.abort();
    }
  });
  await page.goto(`${frontend}/dashboard/retail`);
  await expect(page.getByRole('heading', { name: 'Hàng hoá & tồn kho' })).toBeVisible();
}

test('new product previews the VAT-inclusive sale price without a cost or initial stock', async ({ page }) => {
  await openRetail(page);
  await page.getByRole('button', { name: '+ Thêm sản phẩm' }).click();
  await page.locator('#pf-sku').fill('VAT-NEW-43');
  await page.locator('#pf-name').fill('Sản phẩm mới');
  await expect(page.locator('#pf-cost')).toHaveCount(0);
  await page.locator('#pf-sell').fill('100000');
  await expect(page.locator('#pf-initial')).toHaveCount(0);
  await page.locator('#pf-vat').fill('10');
  await expect(page.getByRole('dialog').getByRole('status')).toContainText('110.000');
  await page.locator('#pf-threshold').fill('7');
  const request = page.waitForRequest((r) => r.method() === 'POST' && new URL(r.url()).pathname.endsWith('/retail/products'));
  await page.getByRole('dialog').getByRole('button', { name: 'Lưu' }).click();
  expect((await request).postDataJSON()).toMatchObject({
    costPrice: 0, sellPrice: 110000, vatRate: 10, lowStockThreshold: 7, isActive: true,
  });
});

test('editing an existing product does not add VAT twice', async ({ page }) => {
  await openRetail(page);
  await page.getByRole('row', { name: /VAT-EDIT-42/ }).getByRole('button', { name: 'Sửa' }).click();
  await expect(page.locator('#pf-sell')).toHaveValue('100000');
  await expect(page.getByRole('dialog').getByRole('status')).toContainText('110.000');
  const request = page.waitForRequest((r) => r.method() === 'PUT' && new URL(r.url()).pathname.endsWith('/retail/products/42'));
  await page.getByRole('dialog').getByRole('button', { name: 'Lưu' }).click();
  expect((await request).postDataJSON()).toMatchObject({ sellPrice: 110000, vatRate: 10 });
});

test('changing VAT recalculates the final customer price', async ({ page }) => {
  await openRetail(page);
  await page.getByRole('row', { name: /VAT-EDIT-42/ }).getByRole('button', { name: 'Sửa' }).click();
  await page.locator('#pf-vat').fill('8');
  await expect(page.getByRole('dialog').getByRole('status')).toContainText('108.000');
  const request = page.waitForRequest((r) => r.method() === 'PUT' && new URL(r.url()).pathname.endsWith('/retail/products/42'));
  await page.getByRole('dialog').getByRole('button', { name: 'Lưu' }).click();
  expect((await request).postDataJSON()).toMatchObject({ sellPrice: 108000, vatRate: 8 });
});
