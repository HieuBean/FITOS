import { test, expect } from '@playwright/test';
import { readFileSync } from 'node:fs';
import path from 'node:path';

test.describe.configure({ mode: 'serial' });

const baseUrl = process.env.FITOS_TEST_FRONTEND_URL ?? 'http://127.0.0.1:3000';
const backendUrl = process.env.FITOS_DEMO_BACKEND_URL ?? 'http://127.0.0.1:18081/api';

test('demo reset requires an authenticated admin', async ({ request }) => {
  test.skip(process.env.FITOS_DEMO_RESET_TEST !== '1', 'Local demo mode only');
  const response = await request.post(`${baseUrl}/api/demo/reset`, {
    headers: { Origin: baseUrl },
  });
  expect(response.status()).toBe(401);
});

test('demo reset rejects a foreign origin', async ({ request }) => {
  test.skip(process.env.FITOS_DEMO_RESET_TEST !== '1', 'Local demo mode only');
  const response = await request.post(`${baseUrl}/api/demo/reset`, {
    headers: { Origin: 'https://example.com' },
  });
  expect(response.status()).toBe(403);
});

test('manager cannot reset demo data', async ({ request }) => {
  test.skip(process.env.FITOS_DEMO_RESET_TEST !== '1', 'Local demo mode only');
  const credentials = JSON.parse(readFileSync(path.resolve('..', '.dsh', 'demo-accounts.json'), 'utf8'));
  const login = await request.post(`${backendUrl}/auth/login`, {
    data: { email: 'manager.demo@example.com', password: credentials.password, studioId: 1 },
  });
  expect(login.ok()).toBeTruthy();
  const { accessToken } = await login.json();
  const response = await request.post(`${baseUrl}/api/demo/reset`, {
    headers: { Origin: baseUrl, Authorization: `Bearer ${accessToken}` },
  });
  expect(response.status()).toBe(403);
});

test('admin-only local API restores an edited lead', async ({ request }) => {
  test.skip(process.env.FITOS_DEMO_RESET_TEST !== '1' || !process.env.FITOS_ADMIN_PASSWORD,
    'Local demo mode and admin password required');
  const login = await request.post(`${backendUrl}/auth/login`, {
    data: { email: 'admin@fitos.com', password: process.env.FITOS_ADMIN_PASSWORD!, studioId: 1 },
  });
  expect(login.ok()).toBeTruthy();
  const { accessToken: token } = await login.json();
  const headers = { Authorization: `Bearer ${token}` };
  const leads = await (await request.get(`${backendUrl}/leads?size=100`, { headers })).json();
  const lead = leads.content.find((item: { email: string }) => item.email === 'lead.huy.demo@example.com');
  const changed = await request.patch(`${backendUrl}/leads/${lead.id}/status`, {
    headers, data: { status: 'TRIAL', notes: 'demo reset browser test' },
  });
  expect(changed.ok()).toBeTruthy();

  const resetResponse = await request.post(`${baseUrl}/api/demo/reset`, {
    headers: { ...headers, Origin: baseUrl },
  });
  expect(resetResponse.status()).toBe(200);

  const restored = await (await request.get(`${backendUrl}/leads?size=100`, { headers })).json();
  expect(restored.content.find((item: { email: string }) => item.email === 'lead.huy.demo@example.com').status)
    .toBe('CONTACTED');
});
