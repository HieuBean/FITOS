import { expect, test } from '@playwright/test';
import { readFileSync } from 'node:fs';
import path from 'node:path';

const frontend = process.env.FITOS_TEST_FRONTEND_URL ?? 'http://127.0.0.1:3000';
const credentials = JSON.parse(readFileSync(path.resolve('..', '.dsh', 'demo-accounts.json'), 'utf8'));

test.beforeEach(() => test.skip(process.env.FITOS_ROLE_SEPARATION_TEST !== '1', 'Local demo test only'));

test('POS product search ignores case and Vietnamese accents', async ({ page }) => {
  await page.goto(`${frontend}/login`);
  await page.getByRole('button', { name: /Studio/ }).click();
  await page.locator('#login-email').fill('reception.demo@example.com');
  await page.locator('#login-password').fill(credentials.password);
  await page.locator('button[type="submit"]').click();
  await page.waitForURL('**/dashboard/**');
  await page.goto(`${frontend}/dashboard/pos`);

  const search = page.locator('#pos-search');
  await expect(page.getByText('Bình lắc Shaker 600ml')).toBeVisible();
  const categories = page.getByRole('tablist', { name: 'Danh mục sản phẩm' });
  await categories.getByRole('button', { name: 'Nước uống' }).click();
  await expect(page.getByText('Nước khoáng Aquafina 500ml')).toBeVisible();
  await expect(page.getByText('Bình lắc Shaker 600ml')).toHaveCount(0);
  await categories.getByRole('button', { name: 'Tất cả' }).click();
  await search.fill('binh lac');
  await page.waitForTimeout(500);
  await expect(page.getByText('Bình lắc Shaker 600ml')).toBeVisible();
  await search.fill('BINH LAC');
  await page.waitForTimeout(500);
  await expect(page.getByText('Bình lắc Shaker 600ml')).toBeVisible();
  await search.fill('day deo the');
  await page.waitForTimeout(500);
  await expect(page.getByText('Dây đeo thẻ hội viên')).toBeVisible();
  await search.fill('ACC-SHAKER-600');
  await search.press('Enter');
  await expect(search).toHaveValue('');
  await expect(page.getByRole('complementary', { name: 'Giỏ hàng' }).getByText('Bình lắc Shaker 600ml')).toBeVisible();
  const cart = page.getByRole('complementary', { name: 'Giỏ hàng' });
  const quantity = cart.getByRole('spinbutton', { name: 'Số lượng Bình lắc Shaker 600ml' });
  await quantity.fill('3');
  await quantity.press('Enter');
  await expect(quantity).toHaveValue('3');
  await expect(cart.getByText('360.000 ₫').first()).toBeVisible();
  await quantity.fill('999');
  await quantity.press('Enter');
  await expect(quantity).toHaveValue('3');
  await expect(page.getByText(/Chỉ còn 30 cái cho "Bình lắc Shaker 600ml"/)).toBeVisible();
  await quantity.fill('');
  await quantity.press('Enter');
  await expect(quantity).toHaveValue('3');
  await quantity.fill('0');
  await quantity.press('Enter');
  await expect(quantity).toHaveCount(0);
});
