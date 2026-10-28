'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/context/AuthContext';
import api from '@/lib/api';

export default function RegisterPage() {
  const router = useRouter();
  const { register } = useAuth();

  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [password, setPassword] = useState('');
  const [studios, setStudios] = useState<Array<{ id: number; name: string }>>([]);
  const [studioId, setStudioId] = useState<number | undefined>();
  const [agreedToTerms, setAgreedToTerms] = useState(false);

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
    setErrorMsg(null);

    if (!fullName.trim() || !email.trim() || !password) {
      setErrorMsg('Vui lòng điền đầy đủ các thông tin bắt buộc.');
      return;
    }

    if (password.length < 6) {
      setErrorMsg('Mật khẩu phải chứa ít nhất 6 ký tự.');
      return;
    }

    if (!agreedToTerms) {
      setErrorMsg('Vui lòng đồng ý với Điều khoản dịch vụ để tiếp tục.');
      return;
    }

    setIsSubmitting(true);

    try {
      await register({
        fullName: fullName.trim(),
        email: email.trim(),
        phone: phone.trim() || undefined,
        password,
        studioId,
      });
      router.push('/dashboard/member');
    } catch (err: any) {
      console.error('Registration error', err);
      const message =
        err.response?.data?.message ||
        (err.response?.status === 409
          ? 'Tài khoản với địa chỉ email này đã tồn tại.'
          : 'Đăng ký không thành công. Vui lòng thử lại.');
      setErrorMsg(message);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen flex bg-[#F5F5F0]">
      {/* Cột trái */}
      <div className="hidden lg:flex w-[480px] bg-[#171A16] flex-col justify-between p-12 flex-shrink-0 relative overflow-hidden">
        <div className="absolute -top-24 -left-24 w-96 h-96 bg-[#D7FF3F]/10 rounded-full blur-3xl pointer-events-none"></div>

        <Link href="/" className="flex items-center gap-2.5 z-10">
          <div className="w-8 h-8 bg-[#D7FF3F] rounded-lg flex items-center justify-center">
            <span className="font-black text-sm text-[#111310]">F</span>
          </div>
          <span className="font-black text-[#ECEDE6] text-xl tracking-tight">FITOS</span>
        </Link>

        <div className="z-10 my-auto py-12">
          <div className="bg-white/5 border border-white/10 backdrop-blur-md rounded-2xl p-8">
            <span className="inline-block bg-[#D7FF3F]/20 text-[#D7FF3F] text-[11px] font-semibold px-2.5 py-1 rounded-full mb-4">
              CỘNG ĐỒNG & HIỆU SUẤT
            </span>
            <p className="text-[22px] font-bold text-white leading-snug mb-6">
              &quot;Gia nhập cùng hàng ngàn hội viên, huấn luyện viên và studio đang vận hành trên nền tảng FITOS.&quot;
            </p>
            <div className="grid grid-cols-3 gap-4 pt-4 border-t border-white/10">
              <div>
                <p className="text-xl font-black text-[#D7FF3F]">2,400+</p>
                <p className="text-[11px] text-[#8A8E85] mt-0.5">Hội viên thường xuyên</p>
              </div>
              <div>
                <p className="text-xl font-black text-[#D7FF3F]">40+</p>
                <p className="text-[11px] text-[#8A8E85] mt-0.5">Lớp học mỗi tuần</p>
              </div>
              <div>
                <p className="text-xl font-black text-[#D7FF3F]">100%</p>
                <p className="text-[11px] text-[#8A8E85] mt-0.5">Đồng bộ đám mây</p>
              </div>
            </div>
          </div>
        </div>

        <div className="z-10 text-xs text-[#8A8E85]">
          FITOS Studio OS · Nền tảng Quản lý Phòng tập Thế hệ mới
        </div>
      </div>

      {/* Cột phải - Form đăng ký */}
      <div className="flex-1 flex items-center justify-center px-6 py-12">
        <div className="bg-white rounded-2xl border border-[#DADCD3] p-8 md:p-10 w-full max-w-[460px] shadow-sm">
          <div className="mb-6">
            <h1 className="text-[26px] font-bold text-[#111310] tracking-tight">Tạo tài khoản mới</h1>
            <p className="text-sm text-[#5E625A] mt-1">Tạo tài khoản khách hàng. Quyền nhân sự do quản trị viên Studio cấp.</p>
          </div>


          {/* Thông báo lỗi */}
          {errorMsg && (
            <div className="mb-5 p-3.5 bg-red-50 border border-red-200 rounded-xl text-xs text-red-700 flex items-start gap-2">
              <span className="font-bold">✕</span>
              <span>{errorMsg}</span>
            </div>
          )}

          {/* Form */}
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
                Họ và tên <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                required
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                placeholder="Nguyễn Văn An"
                className="w-full border border-[#DADCD3] rounded-lg px-3.5 py-2.5 text-sm text-[#111310] placeholder:text-[#8A8E85] bg-white focus:outline-none focus:ring-2 focus:ring-[#D7FF3F] focus:border-transparent transition-all"
              />
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-semibold text-[#111310] mb-1.5 uppercase tracking-[0.05em]">
                  Email <span className="text-red-500">*</span>
                </label>
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="an.nguyen@fitos.io"
                  className="w-full border border-[#DADCD3] rounded-lg px-3.5 py-2.5 text-sm text-[#111310] placeholder:text-[#8A8E85] bg-white focus:outline-none focus:ring-2 focus:ring-[#D7FF3F] focus:border-transparent transition-all"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-[#111310] mb-1.5 uppercase tracking-[0.05em]">
                  Số điện thoại
                </label>
                <input
                  type="tel"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  placeholder="0901 234 567"
                  className="w-full border border-[#DADCD3] rounded-lg px-3.5 py-2.5 text-sm text-[#111310] placeholder:text-[#8A8E85] bg-white focus:outline-none focus:ring-2 focus:ring-[#D7FF3F] focus:border-transparent transition-all"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-[#111310] mb-1.5 uppercase tracking-[0.05em]">
                Mật khẩu <span className="text-red-500">*</span>
              </label>
              <input
                type="password"
                required
                minLength={6}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Tối thiểu 6 ký tự"
                className="w-full border border-[#DADCD3] rounded-lg px-3.5 py-2.5 text-sm text-[#111310] placeholder:text-[#8A8E85] bg-white focus:outline-none focus:ring-2 focus:ring-[#D7FF3F] focus:border-transparent transition-all"
              />
            </div>

            <label className="flex items-start gap-2.5 cursor-pointer pt-2">
              <input
                type="checkbox"
                checked={agreedToTerms}
                onChange={(e) => setAgreedToTerms(e.target.checked)}
                className="mt-0.5 accent-[#526B00] rounded"
              />
              <span className="text-xs text-[#5E625A] leading-relaxed">
                Tôi đồng ý với{' '}
                <a href="#" className="text-[#526B00] font-semibold hover:underline">
                  Điều khoản dịch vụ
                </a>{' '}
                và{' '}
                <a href="#" className="text-[#526B00] font-semibold hover:underline">
                  Chính sách quyền riêng tư
                </a>.
              </span>
            </label>

            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full bg-[#D7FF3F] text-[#111310] font-bold rounded-lg py-3 text-sm hover:bg-[#C4E82E] active:scale-[0.99] transition-all flex items-center justify-center gap-2 mt-4 shadow-sm disabled:opacity-60 cursor-pointer"
            >
              {isSubmitting ? (
                <>
                  <div className="w-4 h-4 border-2 border-[#111310] border-t-transparent rounded-full animate-spin"></div>
                  <span>Đang khởi tạo tài khoản...</span>
                </>
              ) : (
                <span>Đăng ký ngay</span>
              )}
            </button>
          </form>

          <p className="text-center text-sm text-[#5E625A] mt-6">
            Đã có tài khoản?{' '}
            <Link href="/login" className="text-[#526B00] font-semibold hover:underline">
              Đăng nhập
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
