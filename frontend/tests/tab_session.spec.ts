import { test, expect, type Page } from '@playwright/test';
import { readFileSync } from 'node:fs';
import path from 'node:path';

const baseUrl = process.env.FITOS_TEST_FRONTEND_URL ?? 'http://127.0.0.1:3000';

test('dashboard redirects a tab without a session to login', async ({ page }) => {
  await page.goto(`${baseUrl}/dashboard`);
  await page.waitForURL(/\/login\?redirect=/);
  await expect(page.getByText('Chào mừng trở lại')).toBeVisible();
});

async function signIn(page: Page, email: string, password: string) {
  await page.goto(`${baseUrl}/login`);
  await page.getByRole('button', { name: /Studio/ }).click();
  await page.locator('#login-email').fill(email);
  await page.locator('#login-password').fill(password);
  await page.locator('button[type="submit"]').click();
  await page.waitForURL(email === 'admin@fitos.com' ? '**/dashboard/users' : '**/dashboard');
}

test('admin and manager stay in their own tabs after reload and logout', async ({ context, page }) => {
  test.skip(!process.env.FITOS_ADMIN_PASSWORD, 'Local admin password required');
  const credentials = JSON.parse(readFileSync(path.resolve('..', '.dsh', 'demo-accounts.json'), 'utf8'));
  const manager = await context.newPage();

  await signIn(page, 'admin@fitos.com', process.env.FITOS_ADMIN_PASSWORD!);
  await signIn(manager, 'manager.demo@example.com', credentials.password);

  await page.reload();
  await expect(page.getByText('Quản trị viên')).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Tài khoản & Phân quyền' })).toBeVisible();

  await manager.reload();
  await expect(manager.getByText('Quản lý Studio', { exact: true })).toBeVisible();
  await expect(manager.getByRole('link', { name: 'Tài khoản & Phân quyền' })).toHaveCount(0);

  await manager.getByRole('button', { name: /đăng xuất/i }).click();
  await manager.waitForURL(/\/login(?:\?|$)/);
  await page.reload();
  await expect(page.getByText('Quản trị viên')).toBeVisible();
});
