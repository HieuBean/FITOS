import { test, expect } from '@playwright/test';

for (const path of ['/login', '/register']) {
  test(`password can be shown and hidden on ${path}`, async ({ page }) => {
    const baseUrl = process.env.FITOS_TEST_FRONTEND_URL ?? 'http://127.0.0.1:3000';
    await page.goto(`${baseUrl}${path}`);

    const password = page.locator('input[name="password"]');
    await password.fill('sample-password');
    await expect(password).toHaveAttribute('type', 'password');

    await page.getByRole('button', { name: 'Hiện mật khẩu' }).click();
    await expect(password).toHaveAttribute('type', 'text');
    await expect(password).toHaveValue('sample-password');

    await page.getByRole('button', { name: 'Ẩn mật khẩu' }).click();
    await expect(password).toHaveAttribute('type', 'password');
    await expect(password).toHaveValue('sample-password');
  });
}
