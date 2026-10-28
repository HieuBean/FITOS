'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import api from '@/lib/api';
import RoleGuard from '@/components/RoleGuard';

export default function CreateLeadPage() {
  const router = useRouter();

  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [source, setSource] = useState('Khách đến trực tiếp');
  const [interest, setInterest] = useState('PERFORM');
  const [notes, setNotes] = useState('');

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);

    if (!firstName.trim() || !lastName.trim()) {
      setErrorMsg('Vui lòng nhập cả họ và tên của khách hàng.');
      return;
    }

    if (!phone.trim()) {
      setErrorMsg('Vui lòng nhập số điện thoại để liên hệ.');
      return;
    }

    setIsSubmitting(true);

    try {
      const sourceMap: Record<string, string> = {
        'Khách đến trực tiếp': 'WALK_IN',
        'Instagram': 'INSTAGRAM',
        'Facebook': 'FACEBOOK',
        'Người quen giới thiệu': 'REFERRAL',
        'Website': 'WEBSITE',
        'Quảng cáo Google': 'ADVERTISEMENT',
        'Khác': 'OTHER',
      };

      const goalMap: Record<string, string> = {
        'MOVE': 'GENERAL_FITNESS',
        'PERFORM': 'STRENGTH',
        'PT_FOCUS': 'WEIGHT_LOSS',
      };

      const fullName = `${lastName} ${firstName}`.trim();

      const payload = {
        fullName,
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        email: email.trim(),
        phone: phone.trim(),
        source: sourceMap[source] || 'WALK_IN',
        goal: goalMap[interest] || 'GENERAL_FITNESS',
        interest,
        notes: notes.trim() || undefined,
      };

      const res = await api.post('/leads', payload);
      const createdLead = res.data;

      if (createdLead?.id) {
        router.push(`/dashboard/leads/${createdLead.id}`);
      } else {
        router.push('/dashboard/leads');
      }
    } catch (err: any) {
      console.error('Failed to create lead:', err);
      const msg = err.response?.data?.message || 'Không thể lưu thông tin. Vui lòng kiểm tra lại kết nối.';
      setErrorMsg(msg);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <RoleGuard allowedRoles={['ADMIN', 'MANAGER', 'RECEPTIONIST']}>
      <div className="min-h-screen bg-[#F5F5F0]">
      {/* Tiêu đề trang */}
      <div className="bg-white border-b border-[#DADCD3] px-8 py-6">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[#526B00] mb-1">
              TĂNG TRƯỞNG · PHỄU CRM
            </p>
            <h1 className="text-2xl font-bold text-[#111310] tracking-tight">Thêm Khách Tiềm Năng Mới</h1>
            <Link
              href="/dashboard/leads"
              className="text-xs font-semibold text-[#5E625A] hover:text-[#111310] transition-colors inline-flex items-center gap-1.5 mt-1"
            >
              <span>←</span>
              <span>Quay lại danh sách Lead</span>
            </Link>
          </div>
        </div>
      </div>

      {/* Nội dung form */}
      <div className="p-8 max-w-5xl">
        {errorMsg && (
          <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-xl text-xs text-red-700 flex items-start gap-2 shadow-sm">
            <span className="font-bold">✕</span>
            <span>{errorMsg}</span>
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="flex flex-col lg:flex-row gap-6">
            {/* Cột trường nhập liệu */}
            <div className="flex-1 bg-white border border-[#DADCD3] rounded-2xl p-7 shadow-sm">
              <h2 className="text-base font-bold text-[#111310] mb-5 pb-3 border-b border-[#DADCD3]">
                Thông Tin Khách Hàng
              </h2>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
                <div>
                  <label className="block text-xs font-semibold uppercase tracking-[0.06em] text-[#8A8E85] mb-1.5">
                    Họ & tên đệm <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    required
                    value={lastName}
                    onChange={(e) => setLastName(e.target.value)}
                    placeholder="Nguyễn Thị"
                    className="w-full border border-[#DADCD3] rounded-lg px-3.5 py-2.5 text-sm text-[#111310] placeholder:text-[#8A8E85] bg-white focus:outline-none focus:ring-2 focus:ring-[#D7FF3F] focus:border-transparent transition-all"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold uppercase tracking-[0.06em] text-[#8A8E85] mb-1.5">
                    Tên chính <span className="text-red-500">*</span>
                  </label>
                  <input
                    id="firstName"
                    name="firstName"
                    type="text"
                    required
                    value={firstName}
                    onChange={(e) => setFirstName(e.target.value)}
                    placeholder="Phương"
                    className="w-full border border-[#DADCD3] rounded-lg px-3.5 py-2.5 text-sm text-[#111310] placeholder:text-[#8A8E85] bg-white focus:outline-none focus:ring-2 focus:ring-[#D7FF3F] focus:border-transparent transition-all"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
                <div>
                  <label className="block text-xs font-semibold uppercase tracking-[0.06em] text-[#8A8E85] mb-1.5">
                    Địa chỉ Email
                  </label>
                  <input
                    id="email"
                    name="email"
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="phuong.nguyen@example.com"
                    className="w-full border border-[#DADCD3] rounded-lg px-3.5 py-2.5 text-sm text-[#111310] placeholder:text-[#8A8E85] bg-white focus:outline-none focus:ring-2 focus:ring-[#D7FF3F] focus:border-transparent transition-all"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold uppercase tracking-[0.06em] text-[#8A8E85] mb-1.5">
                    Số điện thoại
                  </label>
                  <input
                    type="tel"
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                    placeholder="0908 123 456"
                    className="w-full border border-[#DADCD3] rounded-lg px-3.5 py-2.5 text-sm text-[#111310] placeholder:text-[#8A8E85] bg-white focus:outline-none focus:ring-2 focus:ring-[#D7FF3F] focus:border-transparent transition-all"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
                <div>
                  <label className="block text-xs font-semibold uppercase tracking-[0.06em] text-[#8A8E85] mb-1.5">
                    Nguồn tiếp cận
                  </label>
                  <select
                    value={source}
                    onChange={(e) => setSource(e.target.value)}
                    className="w-full border border-[#DADCD3] rounded-lg px-3.5 py-2.5 text-sm text-[#111310] bg-white focus:outline-none focus:ring-2 focus:ring-[#D7FF3F] focus:border-transparent transition-all"
                  >
                    <option value="Khách đến trực tiếp">Khách đến trực tiếp (Walk-in)</option>
                    <option value="Instagram">Instagram</option>
                    <option value="Facebook">Facebook</option>
                    <option value="Người quen giới thiệu">Người quen giới thiệu</option>
                    <option value="Website">Website</option>
                    <option value="Quảng cáo Google">Quảng cáo Google</option>
                    <option value="Khác">Nguồn khác</option>
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-semibold uppercase tracking-[0.06em] text-[#8A8E85] mb-1.5">
                    Gói tập quan tâm
                  </label>
                  <select
                    value={interest}
                    onChange={(e) => setInterest(e.target.value)}
                    className="w-full border border-[#DADCD3] rounded-lg px-3.5 py-2.5 text-sm text-[#111310] bg-white focus:outline-none focus:ring-2 focus:ring-[#D7FF3F] focus:border-transparent transition-all"
                  >
                    <option value="MOVE">Gói Move (Tập luyện linh hoạt)</option>
                    <option value="PERFORM">Gói Perform (Không giới hạn lớp)</option>
                    <option value="PT_FOCUS">Gói PT Focus (1 Kèm 1 Chuyên sâu)</option>
                  </select>
                </div>
              </div>

              <div className="mb-2">
                <label className="block text-xs font-semibold uppercase tracking-[0.06em] text-[#8A8E85] mb-1.5">
                  Ghi chú tư vấn ban đầu
                </label>
                <textarea
                  rows={4}
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  placeholder="Mục tiêu vóc dáng, khung giờ tập mong muốn, tiền sử chấn thương..."
                  className="w-full border border-[#DADCD3] rounded-lg px-3.5 py-2.5 text-sm text-[#111310] placeholder:text-[#8A8E85] bg-white focus:outline-none focus:ring-2 focus:ring-[#D7FF3F] focus:border-transparent transition-all resize-none"
                />
              </div>
            </div>

            {/* Cột phân công & Lời khuyên AI */}
            <div className="w-full lg:w-80 shrink-0 space-y-4">

              <div className="bg-white border border-[#DADCD3] rounded-2xl p-5 shadow-sm">
                <h3 className="text-xs font-bold uppercase tracking-[0.08em] text-[#111310] mb-3 flex items-center gap-1.5">
                  <span>⚡</span>
                  <span>Gợi Ý Chuyển Đổi Tối Ưu</span>
                </h3>
                <ul className="space-y-3 text-xs text-[#5E625A]">
                  <li className="flex gap-2">
                    <span className="text-[#526B00] font-bold">•</span>
                    <span>
                      <strong className="text-[#111310]">Tốc độ phản hồi:</strong> Gọi điện hoặc nhắn tin trong 15 phút đầu tiên giúp tăng tỷ lệ chốt gấp 4 lần.
                    </span>
                  </li>
                  <li className="flex gap-2">
                    <span className="text-[#526B00] font-bold">•</span>
                    <span>
                      <strong className="text-[#111310]">Tặng buổi tập thử:</strong> Khách hàng hoàn thành 1 buổi trải nghiệm có tỷ lệ mua thẻ tới 68%.
                    </span>
                  </li>
                  <li className="flex gap-2">
                    <span className="text-[#526B00] font-bold">•</span>
                    <span>
                      <strong className="text-[#111310]">Đánh giá AI:</strong> Sau khi lưu, thuật toán FITOS sẽ tự động chấm điểm tiềm năng chuyển đổi.
                    </span>
                  </li>
                </ul>
              </div>
            </div>
          </div>

          {/* Thanh nút bấm */}
          <div className="mt-6 flex items-center justify-end gap-3 pt-4 border-t border-[#DADCD3]">
            <Link
              href="/dashboard/leads"
              className="px-5 py-2.5 rounded-lg text-xs font-bold uppercase tracking-[0.05em] text-[#111310] bg-white hover:bg-[#F5F5F0] border border-[#DADCD3] transition-colors"
            >
              Hủy bỏ
            </Link>
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-6 py-2.5 rounded-lg text-xs font-bold uppercase tracking-[0.05em] bg-[#D7FF3F] text-[#111310] hover:bg-[#C4E82E] active:scale-[0.99] transition-all shadow-sm flex items-center gap-2 disabled:opacity-60 cursor-pointer"
            >
              {isSubmitting ? (
                <>
                  <div className="w-3.5 h-3.5 border-2 border-[#111310] border-t-transparent rounded-full animate-spin"></div>
                  <span>Đang lưu...</span>
                </>
              ) : (
                <span>Lưu & Chấm Điểm AI</span>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
    </RoleGuard>
  );
}
