'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import api from '@/lib/api';
import RoleGuard from '@/components/RoleGuard';
import { Lead } from '../page';
import { mapLead, mapLeadActivity } from '@/lib/contracts';

export interface LeadActivity {
  id: number;
  leadId: number;
  staffId?: number;
  staffName?: string;
  activityType: 'CALL' | 'MESSAGE' | 'TRIAL' | 'NOTE' | 'FOLLOW_UP' | 'CONSULTATION' | 'OTHER';
  content: string;
  createdAt: string;
}

const statusOptions = [
  { key: 'NEW', label: 'Mới' },
  { key: 'CONTACTED', label: 'Đã liên hệ' },
  { key: 'TRIAL', label: 'Tập thử' },
  { key: 'CONVERTED', label: 'Đã chuyển đổi' },
  { key: 'LOST', label: 'Mất dấu' },
] as const;

const activityIcons: Record<string, string> = {
  CALL: '📞',
  MESSAGE: '✉️',
  TRIAL: '🏋️',
  NOTE: '📝',
  FOLLOW_UP: '🔄',
  CONSULTATION: '📝',
  OTHER: '•',
};

const activityTypeLabels: Record<string, string> = {
  CALL: 'Cuộc gọi',
  MESSAGE: 'Tin nhắn',
  TRIAL: 'Buổi tập thử (Trial)',
  NOTE: 'Ghi chú tư vấn (Consultation)',
  FOLLOW_UP: 'Theo dõi / đổi trạng thái',
  CONSULTATION: 'Tư vấn',
  OTHER: 'Khác',
};

export default function LeadDetailPage() {
  const params = useParams();
  const id = params?.id as string;

  const [lead, setLead] = useState<Lead | null>(null);
  const [activities, setActivities] = useState<LeadActivity[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isUpdatingStatus, setIsUpdatingStatus] = useState(false);

  // Modal thêm hoạt động
  const [showActivityModal, setShowActivityModal] = useState(false);
  const [activityType, setActivityType] = useState<LeadActivity['activityType']>('NOTE');
  const [activityContent, setActivityContent] = useState('');
  const [isAddingActivity, setIsAddingActivity] = useState(false);
  const [feedbackMsg, setFeedbackMsg] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const fetchLeadData = async () => {
    setIsLoading(true);
    try {
      const [leadRes, actRes] = await Promise.all([
        api.get(`/leads/${id}`),
        api.get(`/leads/${id}/activities`),
      ]);
      setLead(mapLead(leadRes.data));
      setActivities(actRes.data.map(mapLeadActivity));
    } catch (err) {
      console.error('Failed to load lead details from backend:', err);
      setLead(null);
      setActivities([]);
      setFeedbackMsg({ type: 'error', text: 'Không thể tải hồ sơ lead từ máy chủ.' });
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    if (id) {
      fetchLeadData();
    }
  }, [id]);

  // Cập nhật nhanh trạng thái
  const handleStatusChange = async (newStatus: Lead['status']) => {
    if (!lead || lead.status === newStatus) return;

    setIsUpdatingStatus(true);
    try {
      if (newStatus === 'CONVERTED') {
        const converted = await api.post(`/leads/${id}/convert`);
        setLead(mapLead(converted.data));
      } else {
        await api.patch(`/leads/${id}/status`, { status: newStatus });
        setLead((prev) => (prev ? { ...prev, status: newStatus } : null));
      }

      const actRes = await api.get(`/leads/${id}/activities`);
      setActivities(actRes.data.map(mapLeadActivity));

      setFeedbackMsg({ type: 'success', text: `Đã cập nhật trạng thái Lead thành "${newStatus}"` });
    } catch (err) {
      console.error('Failed to update status:', err);
      setFeedbackMsg({ type: 'error', text: 'Không thể cập nhật trạng thái trên máy chủ.' });
    } finally {
      setIsUpdatingStatus(false);
    }
  };

  // Thêm hoạt động mới
  const handleAddActivity = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!activityContent.trim()) return;

    setIsAddingActivity(true);
    setFeedbackMsg(null);

    try {
      await api.post(`/leads/${id}/activities`, {
        activityType,
        notes: activityContent.trim(),
      });

      setActivityContent('');
      setShowActivityModal(false);
      setFeedbackMsg({ type: 'success', text: 'Đã ghi nhận hoạt động thành công!' });

      const actRes = await api.get(`/leads/${id}/activities`);
      setActivities(actRes.data.map(mapLeadActivity));
    } catch (err: any) {
      console.error('Failed to add activity:', err);
      setFeedbackMsg({ type: 'error', text: 'Không thể lưu hoạt động trên máy chủ.' });
    } finally {
      setIsAddingActivity(false);
    }
  };

  const openActivityModalWithType = (type: LeadActivity['activityType']) => {
    setActivityType(type);
    setShowActivityModal(true);
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-[#F5F5F0] flex items-center justify-center">
        <div className="text-center">
          <div className="w-10 h-10 border-3 border-[#171A16] border-t-[#D7FF3F] rounded-full animate-spin mx-auto mb-3"></div>
          <p className="text-sm font-semibold text-[#5E625A]">Đang tải hồ sơ khách hàng...</p>
        </div>
      </div>
    );
  }

  if (!lead) {
    return (
      <div className="min-h-screen bg-[#F5F5F0] p-8 text-center">
        <h2 className="text-xl font-bold text-[#111310] mb-2">Không tìm thấy khách hàng</h2>
        <Link href="/dashboard/leads" className="text-sm text-[#526B00] font-bold hover:underline">
          ← Quay lại danh sách Lead
        </Link>
      </div>
    );
  }

  const fullName = `${lead.lastName} ${lead.firstName}`.trim();

  return (
    <RoleGuard allowedRoles={['ADMIN', 'MANAGER', 'RECEPTIONIST']}>
      <div className="min-h-screen bg-[#F5F5F0]">
      {/* Tiêu đề trang */}
      <div className="bg-white border-b border-[#DADCD3] px-8 py-6">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <Link
                href="/dashboard/leads"
                className="text-xs font-semibold text-[#5E625A] hover:text-[#111310] transition-colors"
              >
                ← Danh sách Lead
              </Link>
              <span className="text-xs text-[#DADCD3]">/</span>
              <span className="text-xs font-semibold text-[#526B00]">Hồ sơ #{lead.id}</span>
            </div>
            <div className="flex items-center gap-3">
              <h1 className="text-2xl font-black text-[#111310] tracking-tight">{fullName}</h1>
              <span className="text-xs font-medium bg-[#F5F5F0] px-2.5 py-1 rounded-md text-[#5E625A] border border-[#DADCD3]">
                {lead.source}
              </span>
            </div>
          </div>

          {/* Quick Actions & Giai đoạn Phễu */}
          <div className="flex flex-wrap items-center gap-3">
            <button
              onClick={() => openActivityModalWithType('NOTE')}
              className="bg-[#171A16] text-[#D7FF3F] hover:bg-black font-bold text-xs uppercase tracking-wider px-3.5 py-2 rounded-lg flex items-center gap-1.5 shadow-sm cursor-pointer"
            >
              <span>+</span>
              <span>Ghi nhận tư vấn</span>
            </button>
            <button
              onClick={() => openActivityModalWithType('TRIAL')}
              className="bg-[#D7FF3F] text-[#111310] hover:bg-[#C4E82E] font-bold text-xs uppercase tracking-wider px-3.5 py-2 rounded-lg flex items-center gap-1.5 shadow-sm cursor-pointer"
            >
              <span>🏋️</span>
              <span>Lên lịch tập thử</span>
            </button>

            <div className="flex items-center gap-2 bg-[#F5F5F0] p-1.5 rounded-xl border border-[#DADCD3]">
              <span className="text-xs font-bold uppercase tracking-[0.06em] text-[#8A8E85] pl-1.5">
                Giai đoạn:
              </span>
              <select
                disabled={isUpdatingStatus}
                value={lead.status}
                onChange={(e) => handleStatusChange(e.target.value as any)}
                className="bg-white border border-[#DADCD3] font-bold text-xs rounded-lg px-2.5 py-1 text-[#111310] focus:outline-none focus:ring-2 focus:ring-[#D7FF3F] cursor-pointer"
              >
                {statusOptions.map((st) => (
                  <option key={st.key} value={st.key}>
                    {st.label}
                  </option>
                ))}
              </select>
            </div>
          </div>
        </div>
      </div>

      {/* Nội dung hồ sơ */}
      <div className="p-8 max-w-7xl">
        {feedbackMsg && (
          <div
            className={`mb-6 p-4 rounded-xl text-xs font-medium flex items-center justify-between shadow-sm ${
              feedbackMsg.type === 'success'
                ? 'bg-emerald-50 text-emerald-800 border border-emerald-200'
                : 'bg-red-50 text-red-800 border border-red-200'
            }`}
          >
            <span>{feedbackMsg.text}</span>
            <button onClick={() => setFeedbackMsg(null)} className="font-bold text-sm cursor-pointer">
              ✕
            </button>
          </div>
        )}

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Cột trái: Thông tin & Điểm AI (1 Cột) */}
          <div className="space-y-6">
            {/* Card AI Opportunity */}
            <div className="bg-[#171A16] text-[#ECEDE6] rounded-2xl p-6 shadow-sm border border-white/10 relative overflow-hidden">
              <div className="flex items-center justify-between mb-4">
                <span className="text-[11px] font-bold uppercase tracking-[0.1em] text-[#D7FF3F]">
                  Đánh Giá Tiềm Năng Bằng AI
                </span>
                <span className="w-2 h-2 rounded-full bg-[#D7FF3F] animate-pulse"></span>
              </div>

              <div className="flex items-center gap-4 mb-4">
                <div className="w-16 h-16 rounded-2xl bg-[#D7FF3F] text-[#111310] flex flex-col items-center justify-center font-black flex-shrink-0">
                  <span className="text-2xl leading-none">{lead.opportunityScore}</span>
                  <span className="text-[9px] uppercase tracking-wider">Điểm</span>
                </div>
                <div>
                  <p className="text-sm font-bold text-white">
                    {lead.opportunityScore >= 80
                      ? 'Xác Suất Chuyển Đổi Rất Cao'
                      : lead.opportunityScore >= 50
                      ? 'Mức Độ Tương Tác Vừa Phải'
                      : 'Cần Thêm Thời Gian Chăm Sóc'}
                  </p>
                  <p className="text-xs text-[#8A8E85] mt-0.5">
                    Phân tích bởi mô hình máy học FITOS
                  </p>
                </div>
              </div>

              <div className="bg-white/5 rounded-xl p-3.5 border border-white/10 text-xs text-[#DADCD3] leading-relaxed">
                {lead.scoreReason ||
                  'Khách hàng thể hiện tín hiệu tích cực về nhu cầu tập luyện, phù hợp cao với giáo án lớp nhóm và sẵn sàng tham gia trải nghiệm.'}
              </div>
            </div>

            {/* Chi tiết liên hệ */}
            <div className="bg-white border border-[#DADCD3] rounded-2xl p-6 shadow-sm space-y-4">
              <div className="flex items-center justify-between pb-2 border-b border-[#DADCD3]">
                <h2 className="text-sm font-bold uppercase tracking-[0.06em] text-[#111310]">
                  Thông Tin Khách Hàng
                </h2>
                <span className="text-xs font-bold text-[#526B00]">Chi nhánh Trung Tâm</span>
              </div>

              <div>
                <p className="text-[10px] font-bold uppercase tracking-[0.08em] text-[#8A8E85]">Email</p>
                <div className="flex items-center justify-between mt-0.5">
                  <p className="text-sm font-semibold text-[#111310]">{lead.email || '—'}</p>
                  {lead.email && (
                    <a href={`mailto:${lead.email}`} className="text-xs text-[#526B00] hover:underline font-bold">
                      Gửi thư
                    </a>
                  )}
                </div>
              </div>

              <div>
                <p className="text-[10px] font-bold uppercase tracking-[0.08em] text-[#8A8E85]">Số điện thoại</p>
                <div className="flex items-center justify-between mt-0.5">
                  <p className="text-sm font-semibold text-[#111310]">{lead.phone || '—'}</p>
                  {lead.phone && (
                    <a href={`tel:${lead.phone}`} className="text-xs text-[#526B00] hover:underline font-bold">
                      Gọi ngay
                    </a>
                  )}
                </div>
              </div>

              <div>
                <p className="text-[10px] font-bold uppercase tracking-[0.08em] text-[#8A8E85]">Gói tập quan tâm</p>
                <span className="inline-block mt-1 bg-[#D7FF3F]/30 text-[#435702] font-bold text-xs px-2.5 py-1 rounded-md border border-[#D7FF3F]">
                  {lead.interest}
                </span>
              </div>

              <div>
                <p className="text-[10px] font-bold uppercase tracking-[0.08em] text-[#8A8E85]">Nhân sự phụ trách</p>
                <p className="text-sm font-semibold text-[#111310] mt-0.5">
                  {lead.assignedStaffName || 'Chưa chỉ định'}
                </p>
              </div>

              <div>
                <p className="text-[10px] font-bold uppercase tracking-[0.08em] text-[#8A8E85]">Ghi chú ban đầu</p>
                <p className="text-xs text-[#5E625A] mt-1 bg-[#F5F5F0] p-3 rounded-lg border border-[#DADCD3] leading-relaxed">
                  {lead.notes || 'Chưa có ghi chú nào.'}
                </p>
              </div>

              {/* Chuyển đổi thành hội viên */}
              <div className="pt-2">
                <button
                  onClick={() => handleStatusChange('CONVERTED')}
                  className="w-full bg-[#171A16] hover:bg-black text-[#D7FF3F] font-bold text-xs uppercase py-2.5 rounded-lg transition-colors flex items-center justify-center gap-1.5 cursor-pointer"
                >
                  <span>🎉</span>
                  <span>Chuyển đổi thành hội viên</span>
                </button>
              </div>
            </div>
          </div>

          {/* Cột phải: Dòng thời gian & Thêm hoạt động (2 Cột) */}
          <div className="lg:col-span-2 space-y-6">
            {/* Quick Action Logger Bar */}
            <div className="bg-white border border-[#DADCD3] rounded-2xl p-6 shadow-sm">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-4">
                <div>
                  <h2 className="text-sm font-bold uppercase tracking-[0.06em] text-[#111310]">
                    Ghi Nhận Hoạt Động & Trao Đổi
                  </h2>
                  <p className="text-xs text-[#5E625A] mt-0.5">Lưu trữ toàn bộ tiến trình tư vấn và chăm sóc khách hàng</p>
                </div>
                <button
                  onClick={() => setShowActivityModal(true)}
                  className="bg-[#D7FF3F] text-[#111310] font-bold text-xs uppercase px-4 py-2 rounded-lg hover:bg-[#C4E82E] transition-colors flex items-center gap-1.5 self-start sm:self-auto cursor-pointer"
                >
                  <span>+</span>
                  <span>Mở Form Ghi Nhận</span>
                </button>
              </div>

              {/* Action chips */}
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 pt-2">
                <button
                  onClick={() => openActivityModalWithType('CALL')}
                  className="p-3 bg-[#F5F5F0] hover:bg-[#DADCD3] rounded-xl border border-[#DADCD3] text-left transition-colors cursor-pointer"
                >
                  <p className="text-lg mb-1">📞</p>
                  <p className="text-xs font-bold text-[#111310]">Cuộc gọi</p>
                  <p className="text-[10px] text-[#5E625A]">Gọi điện trao đổi</p>
                </button>

                <button
                  onClick={() => openActivityModalWithType('NOTE')}
                  className="p-3 bg-[#F5F5F0] hover:bg-[#DADCD3] rounded-xl border border-[#DADCD3] text-left transition-colors cursor-pointer"
                >
                  <p className="text-lg mb-1">📝</p>
                  <p className="text-xs font-bold text-[#111310]">Tư vấn</p>
                  <p className="text-[10px] text-[#5E625A]">Ghi chú tư vấn</p>
                </button>

                <button
                  onClick={() => openActivityModalWithType('TRIAL')}
                  className="p-3 bg-[#F5F5F0] hover:bg-[#DADCD3] rounded-xl border border-[#DADCD3] text-left transition-colors cursor-pointer"
                >
                  <p className="text-lg mb-1">🏋️</p>
                  <p className="text-xs font-bold text-[#111310]">Tập thử</p>
                  <p className="text-[10px] text-[#5E625A]">Buổi trải nghiệm</p>
                </button>

                <button
                  onClick={() => openActivityModalWithType('MESSAGE')}
                  className="p-3 bg-[#F5F5F0] hover:bg-[#DADCD3] rounded-xl border border-[#DADCD3] text-left transition-colors cursor-pointer"
                >
                  <p className="text-lg mb-1">✉️</p>
                  <p className="text-xs font-bold text-[#111310]">Email</p>
                  <p className="text-[10px] text-[#5E625A]">Gửi thông tin gói</p>
                </button>
              </div>
            </div>

            {/* Dòng thời gian lịch sử tương tác */}
            <div className="bg-white border border-[#DADCD3] rounded-2xl p-6 shadow-sm">
              <h2 className="text-sm font-bold uppercase tracking-[0.06em] text-[#111310] mb-6">
                Dòng Thời Gian Tương Tác & Lịch Sử Liên Lạc
              </h2>

              {activities.length === 0 ? (
                <div className="text-center py-8 text-xs text-[#8A8E85]">
                  Chưa có hoạt động nào được ghi nhận. Hãy lưu cuộc gọi hoặc ghi chú tư vấn đầu tiên!
                </div>
              ) : (
                <div className="relative pl-6 space-y-6 before:absolute before:left-2.5 before:top-2 before:bottom-2 before:w-0.5 before:bg-[#DADCD3]">
                  {activities.map((act) => (
                    <div key={act.id} className="relative flex items-start gap-4">
                      {/* Icon mốc sự kiện */}
                      <div className="absolute -left-6 w-5 h-5 rounded-full bg-[#171A16] text-[#D7FF3F] text-[10px] flex items-center justify-center font-bold ring-4 ring-white">
                        {activityIcons[act.activityType] || '•'}
                      </div>

                      {/* Thẻ nội dung */}
                      <div className="bg-[#F5F5F0] border border-[#DADCD3] rounded-xl p-4 flex-1">
                        <div className="flex items-center justify-between mb-1.5">
                          <span className="text-xs font-bold text-[#111310]">
                            {act.staffName || 'Nhân sự Studio'} ·{' '}
                            <span className="text-[#526B00] uppercase tracking-wider text-[10px]">
                              {activityTypeLabels[act.activityType] || act.activityType}
                            </span>
                          </span>
                          <span className="text-[10px] text-[#8A8E85]">
                            {act.createdAt ? new Date(act.createdAt).toLocaleString('vi-VN') : 'Vừa xong'}
                          </span>
                        </div>
                        <p className="text-xs text-[#5E625A] leading-relaxed whitespace-pre-wrap">
                          {act.content}
                        </p>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Modal Thêm Hoạt Động Mới (Log Consultation & Trial) */}
      {showActivityModal && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl border border-[#DADCD3] max-w-lg w-full p-6 shadow-xl animate-fadeIn">
            <div className="flex items-center justify-between mb-4 pb-2 border-b border-[#DADCD3]">
              <h3 className="text-base font-bold text-[#111310]">Ghi Nhận Hoạt Động Lead</h3>
              <button
                onClick={() => setShowActivityModal(false)}
                className="text-[#8A8E85] hover:text-[#111310] font-bold text-sm cursor-pointer"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleAddActivity}>
              {/* Chọn loại tương tác */}
              <div className="mb-4">
                <label className="block text-xs font-bold uppercase tracking-[0.06em] text-[#8A8E85] mb-2">
                  Loại Tương Tác
                </label>
                <div className="grid grid-cols-2 gap-2">
                  {(['NOTE', 'TRIAL', 'CALL', 'MESSAGE'] as const).map((type) => (
                    <button
                      key={type}
                      type="button"
                      onClick={() => setActivityType(type)}
                      className={`px-3 py-2 rounded-lg text-xs font-bold uppercase transition-all flex items-center gap-2 cursor-pointer border ${
                        activityType === type
                          ? 'bg-[#171A16] text-[#D7FF3F] border-[#171A16] shadow-sm'
                          : 'bg-[#F5F5F0] text-[#5E625A] border-[#DADCD3] hover:bg-[#DADCD3]'
                      }`}
                    >
                      <span>{activityIcons[type]}</span>
                      <span>{activityTypeLabels[type]}</span>
                    </button>
                  ))}
                </div>
              </div>

              <div className="mb-4">
                <label className="block text-xs font-bold uppercase tracking-[0.06em] text-[#8A8E85] mb-2">
                  Nội Dung Chi Tiết
                </label>
                <textarea
                  required
                  rows={4}
                  value={activityContent}
                  onChange={(e) => setActivityContent(e.target.value)}
                  placeholder={`Ghi lại kết quả, phản hồi của khách hoặc lưu ý sau ${activityTypeLabels[activityType].toLowerCase()}...`}
                  className="w-full border border-[#DADCD3] rounded-lg px-3.5 py-2.5 text-sm text-[#111310] placeholder:text-[#8A8E85] bg-white focus:outline-none focus:ring-2 focus:ring-[#D7FF3F] focus:border-transparent transition-all resize-none"
                />
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowActivityModal(false)}
                  className="px-4 py-2 border border-[#DADCD3] rounded-lg text-xs font-bold text-[#5E625A] hover:bg-[#F5F5F0] cursor-pointer"
                >
                  Hủy bỏ
                </button>
                <button
                  type="submit"
                  disabled={isAddingActivity}
                  className="px-5 py-2 bg-[#D7FF3F] text-[#111310] font-bold rounded-lg text-xs uppercase hover:bg-[#C4E82E] transition-all flex items-center gap-1.5 shadow-sm cursor-pointer disabled:opacity-60"
                >
                  {isAddingActivity ? 'Đang lưu...' : 'Lưu Hoạt Động'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
    </RoleGuard>
  );
}
