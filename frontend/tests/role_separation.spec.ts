import { test, expect, type Page } from '@playwright/test';
import { readFileSync } from 'node:fs';
import path from 'node:path';

const frontend = process.env.FITOS_TEST_FRONTEND_URL ?? 'http://127.0.0.1:3000';
const backend = process.env.FITOS_DEMO_BACKEND_URL ?? 'http://127.0.0.1:18081/api';
const credentials = JSON.parse(readFileSync(path.resolve('..', '.dsh', 'demo-accounts.json'), 'utf8'));

test.beforeEach(() => test.skip(process.env.FITOS_ROLE_SEPARATION_TEST !== '1', 'Local role test only'));

async function login(page: Page, email: string) {
  await page.goto(`${frontend}/login`);
  await page.getByRole('button', { name: /Studio/ }).click();
  await page.locator('#login-email').fill(email);
  await page.locator('#login-password').fill(credentials.password);
  await page.locator('button[type="submit"]').click();
}

test('manager portal shows management work and rejects front desk routes', async ({ page }) => {
  await login(page, 'manager.demo@example.com');
  await page.waitForURL('**/dashboard');
  await expect(page.getByRole('link', { name: 'Lễ tân & Huấn luyện viên' })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Báo cáo & Thống kê' })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Gói dịch vụ' })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Bán tại quầy (POS)' })).toHaveCount(0);
  await page.goto(`${frontend}/dashboard/pos`);
  await expect(page.getByRole('heading', { name: 'Quyền truy cập bị giới hạn' })).toBeVisible();
  await page.goto(`${frontend}/dashboard/staff`);
  await expect(page.getByRole('heading', { name: 'Lễ tân & Huấn luyện viên' })).toBeVisible();
  await expect(page.getByText('reception.demo@example.com')).toBeVisible();
  await expect(page.getByText('reception.mai.demo@example.com')).toBeVisible();
  await expect(page.getByText('reception.khoa.demo@example.com')).toBeVisible();
  await page.goto(`${frontend}/dashboard/reports`);
  await expect(page.getByRole('main', { name: 'Trang báo cáo thống kê' })).toBeVisible();
});

test('receptionist portal shows front desk work and rejects management routes', async ({ page }) => {
  await login(page, 'reception.demo@example.com');
  await page.waitForURL('**/dashboard/leads');
  await expect(page.getByRole('link', { name: 'Bán tại quầy (POS)' })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Gói dịch vụ' })).toHaveCount(0);
  await expect(page.getByRole('link', { name: 'Báo cáo & Thống kê' })).toHaveCount(0);
  await page.goto(`${frontend}/dashboard/retail`);
  await expect(page.getByRole('heading', { name: 'Quyền truy cập bị giới hạn' })).toBeVisible();
});

test('backend enforces the same boundary', async ({ request }) => {
  const managerLogin = await request.post(`${backend}/auth/login`, {
    data: { email: 'manager.demo@example.com', password: credentials.password, studioId: 1 },
  });
  const receptionistLogin = await request.post(`${backend}/auth/login`, {
    data: { email: 'reception.demo@example.com', password: credentials.password, studioId: 1 },
  });
  expect(managerLogin.ok()).toBeTruthy();
  expect(receptionistLogin.ok()).toBeTruthy();
  const manager = { Authorization: `Bearer ${(await managerLogin.json()).accessToken}` };
  const receptionist = { Authorization: `Bearer ${(await receptionistLogin.json()).accessToken}` };
  expect((await request.get(`${backend}/staff/receptionists`, { headers: manager })).status()).toBe(200);
  expect((await request.get(`${backend}/staff/receptionists`, { headers: receptionist })).status()).toBe(403);
  expect((await request.post(`${backend}/memberships/1/renew`, { headers: manager, data: {} })).status()).toBe(403);
  expect((await request.patch(`${backend}/plans/1/toggle`, { headers: receptionist })).status()).toBe(403);
  const memberLogin = await request.post(`${backend}/auth/login`, {
    data: { email: 'linh.demo@example.com', password: credentials.password, studioId: 1 },
  });
  expect(memberLogin.ok()).toBeTruthy();
  const { userId: memberId } = await memberLogin.json();
  const leadsResponse = await request.get(`${backend}/leads?size=1`, { headers: manager });
  expect(leadsResponse.ok()).toBeTruthy();
  const leads = (await leadsResponse.json()).content;
  if (leads.length > 0) {
    const invalidAssignment = await request.patch(`${backend}/leads/${leads[0].id}/assign`, {
      headers: manager, data: { assignedToUserId: memberId },
    });
    expect(invalidAssignment.status()).toBe(400);
  }
});
