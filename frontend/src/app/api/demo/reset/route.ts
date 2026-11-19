import { execFile } from 'node:child_process';
import path from 'node:path';
import { promisify } from 'node:util';
import { NextRequest, NextResponse } from 'next/server';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

const run = promisify(execFile);
let resetInProgress = false;

export async function POST(request: NextRequest) {
  if (process.env.FITOS_DEMO_RESET_ENABLED !== '1') {
    return NextResponse.json({ message: 'Không tìm thấy chức năng này.' }, { status: 404 });
  }

  const origin = request.headers.get('origin');
  let originUrl: URL | null = null;
  try {
    originUrl = origin ? new URL(origin) : null;
  } catch {
    // Malformed Origin headers are never accepted.
  }
  if (!['localhost', '127.0.0.1'].includes(request.nextUrl.hostname)
      || !originUrl || originUrl.protocol !== 'http:'
      || !['localhost', '127.0.0.1'].includes(originUrl.hostname)
      || originUrl.port !== request.nextUrl.port) {
    return NextResponse.json({ message: 'Chỉ dùng trong ứng dụng local.' }, { status: 403 });
  }

  const authorization = request.headers.get('authorization');
  if (!authorization?.startsWith('Bearer ')) {
    return NextResponse.json({ message: 'Vui lòng đăng nhập bằng tài khoản quản trị.' }, { status: 401 });
  }

  const backendUrl = process.env.FITOS_DEMO_BACKEND_URL ?? 'http://127.0.0.1:18081/api';
  if (!['localhost', '127.0.0.1'].includes(new URL(backendUrl).hostname)) {
    return NextResponse.json({ message: 'Backend demo phải chạy trên máy local.' }, { status: 503 });
  }

  let profile: { roles?: string[] };
  try {
    const response = await fetch(`${backendUrl}/profile/me`, {
      headers: { Authorization: authorization },
      cache: 'no-store',
    });
    if (!response.ok) {
      return NextResponse.json({ message: 'Phiên đăng nhập không hợp lệ.' }, { status: 401 });
    }
    profile = await response.json();
  } catch {
    return NextResponse.json({ message: 'Không kết nối được backend demo.' }, { status: 503 });
  }
  if (!profile.roles?.includes('ROLE_PLATFORM_ADMIN')) {
    return NextResponse.json({ message: 'Chỉ quản trị viên hệ thống được khôi phục dữ liệu.' }, { status: 403 });
  }
  if (resetInProgress) {
    return NextResponse.json({ message: 'Dữ liệu mẫu đang được khôi phục.' }, { status: 409 });
  }

  resetInProgress = true;
  try {
    const script = path.resolve(process.cwd(), '..', 'scripts', 'reset_demo.py');
    await run('python', [script, '--yes'], {
      cwd: path.resolve(process.cwd(), '..'),
      windowsHide: true,
      timeout: 120_000,
      maxBuffer: 64 * 1024,
    });
    return NextResponse.json({ message: 'Đã khôi phục dữ liệu mẫu ban đầu.' });
  } catch (error) {
    console.error('Demo reset failed:', error);
    return NextResponse.json({ message: 'Khôi phục thất bại. Xem log máy chủ.' }, { status: 500 });
  } finally {
    resetInProgress = false;
  }
}
