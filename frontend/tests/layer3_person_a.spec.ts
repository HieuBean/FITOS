import { test, expect, Page } from '@playwright/test';

async function mockApi(page: Page, role: 'MEMBER' | 'MANAGER') {
  await page.route('**/api/**', async (route) => {
    const path = new URL(route.request().url()).pathname;
    let body: unknown = {};
    if (path.endsWith('/studios')) body = [{ id: 1, name: 'FITOS Studio' }];
    if (path.endsWith('/auth/login')) body = {
      accessToken: 'test-access-token',
      refreshToken: 'test-refresh-token',
      userId: 9,
      studioId: 1,
      fullName: 'FITOS Test',
      email: 'user@fitos.test',
      roles: role === 'MANAGER' ? ['ROLE_MEMBER', 'ROLE_MANAGER'] : ['ROLE_MEMBER'],
    };
    if (path.endsWith('/progress/me')) body = {
      workoutsThisMonth: 0,
      currentStreakDays: 0,
      longestStreakDays: 0,
      personalRecords: [],
    };
    if (path.endsWith('/body-metrics/me')) body = [];
    if (path.endsWith('/member/activity')) body = [];
    if (path.endsWith('/dashboard/stats')) body = {
      revenueThisMonth: 0, activeMembers: 0, classesToday: 0, bookedSpotsToday: 0,
      totalCapacityToday: 0, classFillRate: 0, newLeadsThisMonth: 0, leadConversionRate: 0,
    };
    if (path.endsWith('/dashboard/revenue-chart')) body = { points: [] };
    if (path.endsWith('/dashboard/crm-funnel')) body = { stages: [] };
    if (path.endsWith('/leads')) body = { content: [], totalElements: 0 };
    if (path.endsWith('/ai/chat')) body = { reply: 'Tư vấn thử nghiệm', is_safe: true, suggested_actions: [] };
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(body) });
  });
}

test('Đăng nhập chỉ hiển thị biểu mẫu thật', async ({ page }) => {
  await mockApi(page, 'MEMBER');
  await page.goto('/login');
  await expect(page.getByRole('heading', { name: 'Chào mừng trở lại' })).toBeVisible();
  await expect(page.locator('input[type="email"]')).toBeVisible();
  await expect(page.locator('#demo-manager-btn')).toHaveCount(0);
  await expect(page.locator('#demo-member-btn')).toHaveCount(0);
});

test('Đăng ký công khai không cho chọn vai trò nhân sự', async ({ page }) => {
  await mockApi(page, 'MEMBER');
  await page.goto('/register');
  await expect(page.getByRole('heading', { name: 'Tạo tài khoản mới' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Nhân sự' })).toHaveCount(0);
});

test('Người chưa đăng nhập bị chuyển về trang đăng nhập', async ({ page }) => {
  await page.goto('/dashboard');
  await expect(page).toHaveURL(/\/login/);
});

for (const role of ['MEMBER', 'MANAGER'] as const) {
  test(`Đăng nhập ${role} chuyển tới trang phù hợp`, async ({ page }) => {
    await mockApi(page, role);
    await page.goto('/login');
    await page.locator('input[type="email"]').fill('user@fitos.test');
    await page.locator('input[type="password"]').fill('Password123');
    await page.locator('button[type="submit"]').click();
    await expect(page).toHaveURL(role === 'MEMBER' ? /\/dashboard\/member$/ : /\/dashboard$/);
    await expect(page.locator('h1').first()).toBeVisible();
  });
}

test('Dashboard và CRM hiển thị trạng thái rỗng từ API', async ({ page }) => {
  await mockApi(page, 'MANAGER');
  await page.goto('/login');
  await page.locator('input[type="email"]').fill('user@fitos.test');
  await page.locator('input[type="password"]').fill('Password123');
  await page.locator('button[type="submit"]').click();
  await expect(page).toHaveURL(/\/dashboard$/);
  await expect(page.getByText('Lead Mới Tháng Này')).toBeVisible();
  await page.goto('/dashboard/leads');
  await expect(page.getByText('Không tìm thấy khách tiềm năng nào')).toBeVisible();
});

test('AI chat chỉ gửi câu hỏi; hồ sơ thể lực lấy ở backend', async ({ page }) => {
  await mockApi(page, 'MEMBER');
  await page.goto('/login');
  await page.locator('input[type="email"]').fill('user@fitos.test');
  await page.locator('input[type="password"]').fill('Password123');
  await page.locator('button[type="submit"]').click();
  await expect(page).toHaveURL(/\/dashboard\/member$/);
  await page.goto('/dashboard/member/ai-coach');
  await page.locator('input[placeholder*="HLV AI"]').fill('Tôi nên khởi động thế nào?');
  const requestPromise = page.waitForRequest((request) => request.url().endsWith('/api/ai/chat'));
  await page.getByRole('button', { name: 'Gửi' }).click();
  const request = await requestPromise;
  const payload = request.postDataJSON();
  expect(payload.message).toBe('Tôi nên khởi động thế nào?');
  expect(payload).not.toHaveProperty('fitness_profile');
  expect(request.headers().authorization).toBe('Bearer test-access-token');
  await expect(page.getByText('Tư vấn thử nghiệm')).toBeVisible();
});

test('Member Home hiển thị hoạt động thật từ API', async ({ page }) => {
  await mockApi(page, 'MEMBER');
  await page.route('**/api/member/activity', (route) => route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify([{ kind: 'CHECK_IN', title: 'Điểm danh lớp Yoga', occurredAt: '2026-10-27T09:00:00Z' }]),
  }));
  await page.goto('/login');
  await page.locator('input[type="email"]').fill('user@fitos.test');
  await page.locator('input[type="password"]').fill('Password123');
  await page.locator('button[type="submit"]').click();
  await expect(page).toHaveURL(/\/dashboard\/member$/);
  await expect(page.getByText('Điểm danh lớp Yoga')).toBeVisible();
});
