'use client';

import React, { useState, useEffect, Suspense } from 'react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { useAuth } from '@/context/AuthContext';
import api from '@/lib/api';

function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const redirectUrl = searchParams.get('redirect');

  const { login } = useAuth();
  const [accountType, setAccountType] = useState<'member' | 'staff'>('member');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [studios, setStudios] = useState<Array<{ id: number; name: string }>>([]);
  const [studioId, setStudioId] = useState<number | undefined>();
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    api.get('/studios').then((res) => {
      const list = Array.isArray(res.data) ? res.data : [];
      setStudios(list);
      if (list.length === 1) setStudioId(list[0].id);
    }).catch(() => setStudios([]));
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email || !password) {
      setErrorMsg('Vui lòng nhập đầy đủ cả email và mật khẩu.');
      return;
    }

    setErrorMsg(null);
    setIsSubmitting(true);

    try {
      const user = await login(email, password, studioId);

      // Chuyển hướng thông minh theo vai trò người dùng (không để Hội viên bị đẩy vào trang Studio)
      if (user.role === 'MEMBER') {
        if (redirectUrl && redirectUrl.startsWith('/dashboard/member')) {
          router.push(redirectUrl);
        } else {
          router.push('/dashboard/member');
        }
      } else if (user.role === 'RECEPTIONIST') {
        if (redirectUrl && !redirectUrl.startsWith('/dashboard/member')) {
          router.push(redirectUrl);
        } else {
          router.push('/dashboard/leads');
        }
      } else if (user.role === 'TRAINER') {
        if (
          redirectUrl &&
          (redirectUrl.startsWith('/dashboard/trainer') ||
            redirectUrl.startsWith('/dashboard/workout') ||
            redirectUrl.startsWith('/dashboard/schedule'))
        ) {
          router.push(redirectUrl);
        } else {
          router.push('/dashboard/trainer');
        }
      } else {
        router.push(redirectUrl || '/dashboard');
      }
    } catch (err: any) {
      console.error('Login error', err);
      const message =
        err.response?.data?.message ||
        (err.response?.status === 401
          ? 'Email hoặc mật khẩu không chính xác.'
          : 'Không thể kết nối đến máy chủ. Vui lòng thử lại.');
      setErrorMsg(message);
    } finally {
      setIsSubmitting(false);
    }
  };


  return (
    <div className="bg-white rounded-2xl border border-[#DADCD3] p-8 md:p-10 w-full max-w-[440px] shadow-sm">
      {/* Tiêu đề Form */}
      <div className="mb-6">
        <h1 className="text-[26px] font-bold text-[#111310] tracking-tight">Chào mừng trở lại</h1>
        <p className="text-sm text-[#5E625A] mt-1">Đăng nhập vào không gian làm việc FITOS của bạn</p>
      </div>

      {/* Thông báo lỗi */}
      {errorMsg && (
        <div className="mb-5 p-3.5 bg-red-50 border border-red-200 rounded-xl text-xs text-red-700 flex items-start gap-2 animate-fadeIn">
          <span className="font-bold">✕</span>
          <span>{errorMsg}</span>
        </div>
      )}

      {/* Chọn Cổng đăng nhập (Hội viên vs Quản lý Studio) */}
      <div className="grid grid-cols-2 gap-1.5 p-1 bg-[#F5F5F0] rounded-xl mb-5 border border-[#DADCD3]">
        <button
          type="button"
          onClick={() => {
            setAccountType('member');
          }}
          className={`py-2 text-xs font-bold rounded-lg transition-all flex items-center justify-center gap-1.5 ${
            accountType === 'member'
              ? 'bg-[#171A16] text-[#D7FF3F] shadow-sm'
              : 'text-[#5E625A] hover:text-[#111310]'
          }`}
        >
          <span>💪</span>
          <span>Cổng Hội Viên</span>
        </button>
        <button
          type="button"
          onClick={() => {
            setAccountType('staff');
            if (email === 'member@fitos.io') setEmail('');
          }}
          className={`py-2 text-xs font-bold rounded-lg transition-all flex items-center justify-center gap-1.5 ${
            accountType === 'staff'
              ? 'bg-[#171A16] text-[#D7FF3F] shadow-sm'
              : 'text-[#5E625A] hover:text-[#111310]'
          }`}
        >
          <span>👔</span>
          <span>Quản Lý Studio</span>
        </button>
      </div>

      {/* Form đăng nhập */}
      <form onSubmit={handleSubmit} className="space-y-4">
        {studios.length > 1 && (
          <div>
            <label className="block text-xs font-semibold text-[#111310] mb-1.5 uppercase">Studio</label>
            <select required value={studioId ?? ''} onChange={(e) => setStudioId(Number(e.target.value))}
              className="w-full border border-[#DADCD3] rounded-lg px-3.5 py-2.5 text-sm">
              <option value="">Chọn Studio</option>
              {studios.map((studio) => <option key={studio.id} value={studio.id}>{studio.name}</option>)}
            </select>
          </div>
        )}
        <div>
          <label className="block text-xs font-semibold text-[#111310] mb-1.5 uppercase tracking-[0.05em]">
            Địa chỉ Email
          </label>
          <input
            type="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder={accountType === 'member' ? 'hoivien@fitos.io' : 'quanly@fitos.io'}
            disabled={isSubmitting}
            className="w-full border border-[#DADCD3] rounded-lg px-3.5 py-2.5 text-sm text-[#111310] placeholder:text-[#8A8E85] bg-white focus:outline-none focus:ring-2 focus:ring-[#D7FF3F] focus:border-transparent transition-all disabled:opacity-60"
          />
        </div>

        <div>
          <div className="flex items-center justify-between mb-1.5">
            <label className="block text-xs font-semibold text-[#111310] uppercase tracking-[0.05em]">
              Mật khẩu
            </label>
            <a href="#" className="text-xs text-[#526B00] font-medium hover:underline">
              Quên mật khẩu?
            </a>
          </div>
          <input
            type="password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="••••••••"
            disabled={isSubmitting}
            className="w-full border border-[#DADCD3] rounded-lg px-3.5 py-2.5 text-sm text-[#111310] placeholder:text-[#8A8E85] bg-white focus:outline-none focus:ring-2 focus:ring-[#D7FF3F] focus:border-transparent transition-all disabled:opacity-60"
          />
        </div>

        <button
          type="submit"
          disabled={isSubmitting}
          className="w-full bg-[#D7FF3F] text-[#111310] font-bold rounded-lg py-3 text-sm hover:bg-[#C4E82E] active:scale-[0.99] transition-all flex items-center justify-center gap-2 mt-2 shadow-sm disabled:opacity-60 cursor-pointer"
        >
          {isSubmitting ? (
            <>
              <div className="w-4 h-4 border-2 border-[#111310] border-t-transparent rounded-full animate-spin"></div>
              <span>Đang đăng nhập...</span>
            </>
          ) : (
            <span>{accountType === 'member' ? 'Vào Cổng Hội Viên' : 'Vào Bảng Điều Hành'}</span>
          )}
        </button>
      </form>


      {/* Đường dẫn đăng ký */}
      <p className="text-center text-sm text-[#5E625A] mt-6">
        Chưa có tài khoản?{' '}
        <Link href="/register" className="text-[#526B00] font-semibold hover:underline">
          Tạo tài khoản ngay
        </Link>
      </p>
    </div>
  );
}

export default function LoginPage() {
  return (
    <div className="min-h-screen flex bg-[#F5F5F0]">
      {/* Cột trái - Thương hiệu & Triết lý */}
      <div className="hidden lg:flex w-[480px] bg-[#171A16] flex-col justify-between p-12 flex-shrink-0 relative overflow-hidden">
        {/* Glow accent */}
        <div className="absolute -top-24 -left-24 w-96 h-96 bg-[#D7FF3F]/10 rounded-full blur-3xl pointer-events-none"></div>

        {/* Logo FITOS */}
        <Link href="/" className="flex items-center gap-2.5 z-10">
          <div className="w-8 h-8 bg-[#D7FF3F] rounded-lg flex items-center justify-center shadow-lg shadow-[#D7FF3F]/20">
            <span className="font-black text-sm text-[#111310]">F</span>
          </div>
          <span className="font-black text-[#ECEDE6] text-xl tracking-tight">FITOS</span>
        </Link>

        {/* Trích dẫn truyền cảm hứng */}
        <div className="z-10 my-auto py-12">
          <div className="bg-white/5 border border-white/10 backdrop-blur-md rounded-2xl p-8 shadow-xl">
            <div className="inline-block bg-[#D7FF3F]/20 text-[#D7FF3F] text-[11px] font-semibold px-2.5 py-1 rounded-full mb-4">
              NỀN TẢNG THÔNG MINH BẰNG AI
            </div>
            <p className="text-[22px] font-bold text-white leading-snug mb-4">
              &quot;Buổi tập tuyệt vời nhất là buổi tập bạn thực sự có mặt. FITOS đồng hành cùng từng studio và hội viên mỗi ngày.&quot;
            </p>
            <div className="flex items-center gap-3">
              <div className="w-8 h-8 rounded-full bg-[#D7FF3F] flex items-center justify-center font-bold text-xs text-[#111310]">
                FT
              </div>
              <div>
                <p className="text-xs font-semibold text-white">Đội ngũ Huấn Luyện FITOS</p>
                <p className="text-[10px] text-[#8A8E85]">Hệ Thống Studio & Flagship Hubs</p>
              </div>
            </div>
          </div>
        </div>

        {/* Thông tin chân trang */}
        <div className="z-10 flex items-center justify-between text-xs text-[#8A8E85]">
          <span>© 2026 Hệ thống FITOS</span>
          <span className="flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-[#D7FF3F] inline-block animate-pulse"></span>
            Hệ thống đang hoạt động
          </span>
        </div>
      </div>

      {/* Cột phải - Form đăng nhập bọc Suspense */}
      <div className="flex-1 flex items-center justify-center px-6 py-12">
        <Suspense
          fallback={
            <div className="bg-white rounded-2xl border border-[#DADCD3] p-10 w-full max-w-[440px] flex justify-center items-center">
              <div className="w-8 h-8 border-3 border-[#171A16] border-t-[#D7FF3F] rounded-full animate-spin"></div>
            </div>
          }
        >
          <LoginForm />
        </Suspense>
      </div>
    </div>
  );
}
