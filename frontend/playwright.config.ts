import { defineConfig, devices } from '@playwright/test';

const targetUrl = process.env.FITOS_TEST_FRONTEND_URL ?? 'http://127.0.0.1:3000';

export default defineConfig({
  testDir: './tests',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: 'html',
  use: {
    baseURL: targetUrl,
    trace: 'on-first-retry',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: {
    command: process.env.FITOS_TEST_FRONTEND_URL ? 'npm run start -- -H 127.0.0.1 -p 13000' : 'npm run dev',
    url: targetUrl,
    reuseExistingServer: !process.env.CI,
  },
});
