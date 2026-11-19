import { test, expect } from '@playwright/test';
import { readFileSync } from 'node:fs';
import path from 'node:path';

const frontend = process.env.FITOS_TEST_FRONTEND_URL ?? 'http://127.0.0.1:3000';
const backend = process.env.FITOS_DEMO_BACKEND_URL ?? 'http://127.0.0.1:18081/api';
const credentials = JSON.parse(readFileSync(path.resolve('..', '.dsh', 'demo-accounts.json'), 'utf8'));

test.beforeEach(() => {
  test.skip(process.env.FITOS_ADMIN_ACCOUNT_TEST !== '1' || !process.env.FITOS_ADMIN_PASSWORD,
    'Local account-management test only');
});

test('manager cannot call account administration API', async ({ request }) => {
  const login = await request.post(`${backend}/auth/login`, {
    data: { email: 'manager.demo@example.com', password: credentials.password, studioId: 1 },
  });
  expect(login.ok()).toBeTruthy();
  const { accessToken } = await login.json();
  const response = await request.get(`${backend}/users`, {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
  expect(response.status()).toBe(403);
  const create = await request.post(`${backend}/users`, {
    headers: { Authorization: `Bearer ${accessToken}` },
    data: { email: `blocked-${Date.now()}@example.com`, password: 'TemporaryTest2026!', fullName: 'Blocked', role: 'ROLE_MEMBER' },
  });
  expect(create.status()).toBe(403);
});

test('admin lands on accounts and manager cannot open that page', async ({ browser }) => {
  const context = await browser.newContext();
  const admin = await context.newPage();
  await admin.goto(`${frontend}/login`);
  await admin.getByRole('button', { name: /Studio/ }).click();
  await admin.locator('#login-email').fill('admin@fitos.com');
  await admin.locator('#login-password').fill(process.env.FITOS_ADMIN_PASSWORD!);
  await admin.locator('button[type="submit"]').click();
  await admin.waitForURL('**/dashboard/users');
  await expect(admin.getByRole('heading', { name: 'Tài khoản & Phân quyền' })).toBeVisible();
  await expect(admin.getByRole('button', { name: /^Sửa tài khoản/ }).first()).toBeVisible();
  await expect(admin.getByRole('button', { name: /^Xóa tài khoản/ }).first()).toBeVisible();
  await expect(admin.getByRole('navigation', { name: 'Điều hướng chính' })
    .getByRole('link', { name: 'Bảng điều khiển' })).toHaveCount(0);
  await admin.goto(`${frontend}/dashboard`);
  await admin.waitForURL('**/dashboard/users');

  const manager = await context.newPage();
  await manager.goto(`${frontend}/login`);
  await manager.getByRole('button', { name: /Studio/ }).click();
  await manager.locator('#login-email').fill('manager.demo@example.com');
  await manager.locator('#login-password').fill(credentials.password);
  await manager.locator('button[type="submit"]').click();
  await manager.waitForURL('**/dashboard');
  await manager.goto(`${frontend}/dashboard/users`);
  await expect(manager.getByText('Quyền truy cập bị giới hạn')).toBeVisible();
  await context.close();
});

test('admin can create edit and delete an account', async ({ request }) => {
  const login = await request.post(`${backend}/auth/login`, {
    data: { email: 'admin@fitos.com', password: process.env.FITOS_ADMIN_PASSWORD, studioId: 1 },
  });
  expect(login.ok()).toBeTruthy();
  const { accessToken, userId } = await login.json();
  const headers = { Authorization: `Bearer ${accessToken}` };
  const email = `account-test-${Date.now()}@example.com`;
  const updatedEmail = `updated-${email}`;
  const created = await request.post(`${backend}/users`, {
    headers,
    data: { email, password: 'TemporaryTest2026!', fullName: 'Account Test', phone: '0900000000', role: 'ROLE_MEMBER' },
  });
  expect(created.status()).toBe(201);
  const user = await created.json();
  try {
    const updated = await request.put(`${backend}/users/${user.id}`, {
      headers, data: { email: updatedEmail, fullName: 'Updated Account', phone: '0911111111' },
    });
    expect(updated.status()).toBe(200);
    expect((await updated.json()).email).toBe(updatedEmail);
    const promoted = await request.patch(`${backend}/users/${user.id}/role`, {
      headers, data: { role: 'ROLE_ADMIN' },
    });
    expect(promoted.status()).toBe(200);
    expect((await promoted.json()).roles).toContain('ROLE_ADMIN');
    const protectedSelf = await request.delete(`${backend}/users/${userId}`, { headers });
    expect(protectedSelf.status()).toBe(400);
    const deleted = await request.delete(`${backend}/users/${user.id}`, { headers });
    expect(deleted.status()).toBe(204);
    const listed = await (await request.get(`${backend}/users`, { headers })).json();
    expect(listed.some((item: { id: number }) => item.id === user.id)).toBe(false);
  } finally {
    await request.delete(`${backend}/users/${user.id}`, { headers }).catch(() => undefined);
  }
});
