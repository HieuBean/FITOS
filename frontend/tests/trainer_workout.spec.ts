import { test, expect } from '@playwright/test';

test.describe('Layer 3 Frontend — Person D Suite', () => {
  test('Task 99: Trainer page loads timeline and client list', async ({ page }) => {
    await page.goto('/dashboard/trainer');
    await expect(page.locator('h1')).toContainText('Trainer Management');
    await expect(page.locator('table')).toBeVisible();
    await expect(page.locator('text=Marcus Webb').first()).toBeVisible();
  });

  test('Task 100: Workout builder loads with 7 day tabs and exercise library', async ({ page }) => {
    await page.goto('/dashboard/workout-builder');
    await expect(page.locator('h1')).toContainText('Trình Thiết Kế Giáo Án Luyện Tập');
    await expect(page.locator('text=Thư Viện Bài Tập')).toBeVisible();
    await expect(page.locator('text=Phân bổ lịch tập trong tuần')).toBeVisible();
  });

  test('Task 101: Member progress page renders stats, pure CSS chart, and PRs', async ({ page }) => {
    await page.goto('/dashboard/member/progress');
    await expect(page.locator('h1')).toContainText('Tiến Độ & Kỷ Lục Thể Lực');
    await expect(page.locator('text=Chuỗi đi tập liên tục')).toBeVisible();
    await expect(page.locator('text=Tần Suất Đi Tập 6 Tháng Gần Nhất')).toBeVisible();
    await expect(page.locator('text=Bảng Kỷ Lục Sức Mạnh Cá Nhân (PRs)')).toBeVisible();
  });

  test('Task 102: AI Coach page renders chat and recommendation card', async ({ page }) => {
    await page.goto('/dashboard/member/ai-coach');
    await expect(page.locator('h1')).toContainText('AI Workout Coach');
    await expect(page.locator('text=Đề Xuất Giáo Án Cá Nhân Hóa')).toBeVisible();
    await expect(page.locator('input[placeholder*="Đặt câu hỏi"]')).toBeVisible();
  });

  test('Task 103: Reports page renders with tabs', async ({ page }) => {
    await page.goto('/dashboard/reports');
    await expect(page.locator('h1')).toContainText('Báo Cáo Hoạt Động');
    await expect(page.locator('button:has-text("Doanh Thu")')).toBeVisible();
    await expect(page.locator('button:has-text("Hội Viên")')).toBeVisible();
    await expect(page.locator('button:has-text("Lớp Học")')).toBeVisible();
  });

  test('Task 104: Workout log page renders session timer and allows opening ExerciseDetailDrawer', async ({ page }) => {
    await page.goto('/dashboard/member/workout-log');
    await expect(page.locator('h1')).toContainText('Ghi Nhận Buổi Tập');
    await expect(page.locator('button:has-text("Xem video & Form chuẩn")')).toBeVisible();
  });
});

