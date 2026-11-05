'use client';

import React, { useState, useEffect, useMemo } from 'react';
import Link from 'next/link';
import api from '@/lib/api';
import RoleGuard from '@/components/RoleGuard';
import { mapLeadPage, type LeadView } from '@/lib/contracts';

export type Lead = LeadView;

const statusBadgeStyles: Record<string, string> = {
  NEW: 'bg-amber-100 text-amber-800 border-amber-200',
  CONTACTED: 'bg-blue-100 text-blue-800 border-blue-200',
  TRIAL: 'bg-[#D7FF3F] text-[#111310] font-bold border-[#c4eb28]',
  CONVERTED: 'bg-emerald-100 text-emerald-800 border-emerald-200',
  LOST: 'bg-zinc-100 text-zinc-600 border-zinc-200',
};

const statusLabels: Record<string, string> = {
  NEW: 'Mới',
  CONTACTED: 'Đã liên hệ',
  TRIAL: 'Tập thử',
  CONVERTED: 'Đã chuyển đổi',
  LOST: 'Mất dấu',
};

export default function LeadsPage() {
  const [leads, setLeads] = useState<Lead[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedStatus, setSelectedStatus] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [debouncedQuery, setDebouncedQuery] = useState('');

  // Debounce tìm kiếm 300ms
  useEffect(() => {
    const handler = setTimeout(() => {
      setDebouncedQuery(searchQuery);
    }, 300);
    return () => clearTimeout(handler);
  }, [searchQuery]);

  // Lấy dữ liệu lead từ máy chủ
  const fetchLeads = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const params: any = {};
      if (selectedStatus !== 'ALL') {
        params.status = selectedStatus;
      }
      if (debouncedQuery.trim()) {
        params.search = debouncedQuery.trim();
      }

      const res = await api.get('/leads', { params });
      setLeads(mapLeadPage(res.data));
    } catch (err) {
      console.error('Failed to load leads from backend:', err);
      setLeads([]);
      setError('Không thể tải danh sách khách tiềm năng. Vui lòng thử lại.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchLeads();
  }, [selectedStatus, debouncedQuery]);

  // Tổng hợp chỉ số thống kê
  const stats = useMemo(() => {
    return {
      total: leads.length,
      contacted: leads.filter((l) => l.status === 'CONTACTED').length,
      trials: leads.filter((l) => l.status === 'TRIAL').length,
      converted: leads.filter((l) => l.status === 'CONVERTED').length,
    };
  }, [leads]);

  const tabs = [
    { key: 'ALL', label: 'Tất cả' },
    { key: 'NEW', label: 'Mới' },
    { key: 'CONTACTED', label: 'Đã liên hệ' },
    { key: 'TRIAL', label: 'Tập thử' },
    { key: 'CONVERTED', label: 'Đã chuyển đổi' },
    { key: 'LOST', label: 'Mất dấu' },
  ];

  const getScoreBadge = (score: number) => {
    if (score >= 80) {
      return (
        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold bg-[#D7FF3F]/30 text-[#435702] border border-[#D7FF3F]">
          🔥 {score}% Cao
        </span>
      );
    }
    if (score >= 50) {
      return (
        <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-50 text-amber-800 border border-amber-200">
          ⚡ {score}% TB
        </span>
      );
    }
    return (
      <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium bg-zinc-100 text-zinc-600 border border-zinc-200">
        {score}% Thấp
      </span>
    );
  };

  return (
    <RoleGuard allowedRoles={['ADMIN', 'MANAGER', 'RECEPTIONIST']}>
      <div className="min-h-screen bg-[#F5F5F0]">
      {/* Tiêu đề trang */}
      <div className="bg-white border-b border-[#DADCD3] px-8 py-6">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[#526B00] mb-1">
              TĂNG TRƯỞNG · PHỄU CRM
            </p>
            <h1 className="text-2xl font-bold text-[#111310] tracking-tight">Khách Tiềm Năng & Cơ Hội</h1>
            <p className="text-sm text-[#5E625A] mt-0.5">
              Quy trình quản lý chuyển đổi và chấm điểm khách tiềm năng tự động từ AI
            </p>
          </div>
          <div className="flex items-center gap-3">
            <Link
              href="/dashboard/create-lead"
              className="bg-[#D7FF3F] text-[#111310] font-bold rounded-lg px-4 py-2.5 text-sm hover:bg-[#C4E82E] active:scale-[0.99] transition-all shadow-sm flex items-center gap-1.5"
            >
              <span className="text-base font-black">+</span>
              <span>Thêm khách tiềm năng</span>
            </Link>
          </div>
        </div>
      </div>

      {/* Nội dung danh sách */}
      <div className="p-8">
        {error && <p role="alert" className="mb-4 text-sm text-red-700">{error}</p>}
        {/* 4 Thẻ KPI tóm tắt */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
          <div className="bg-white border border-[#DADCD3] rounded-xl p-5 shadow-sm">
            <p className="text-[11px] font-semibold uppercase tracking-[0.08em] text-[#8A8E85] mb-1.5">
              Tổng số Lead
            </p>
            <p className="text-3xl font-black text-[#111310]">{stats.total}</p>
            <p className="text-xs text-[#526B00] font-medium mt-1">Đang hiển thị</p>
          </div>

          <div className="bg-white border border-[#DADCD3] rounded-xl p-5 shadow-sm">
            <p className="text-[11px] font-semibold uppercase tracking-[0.08em] text-[#8A8E85] mb-1.5">
              Đang Liên Hệ
            </p>
            <p className="text-3xl font-black text-[#111310]">{stats.contacted}</p>
            <p className="text-xs text-[#5E625A] font-medium mt-1">Trong quy trình tư vấn</p>
          </div>

          <div className="bg-white border border-[#DADCD3] rounded-xl p-5 shadow-sm">
            <p className="text-[11px] font-semibold uppercase tracking-[0.08em] text-[#8A8E85] mb-1.5">
              Lịch Tập Thử
            </p>
            <p className="text-3xl font-black text-[#111310]">{stats.trials}</p>
            <p className="text-xs text-[#526B00] font-medium mt-1">Tỷ lệ chốt gói cao</p>
          </div>

          <div className="bg-white border border-[#DADCD3] rounded-xl p-5 shadow-sm">
            <p className="text-[11px] font-semibold uppercase tracking-[0.08em] text-[#8A8E85] mb-1.5">
              Đã Chuyển Đổi
            </p>
            <p className="text-3xl font-black text-[#111310]">{stats.converted}</p>
            <p className="text-xs text-[#526B00] font-medium mt-1">Đã thành hội viên</p>
          </div>
        </div>

        {/* Thanh công cụ lọc & tìm kiếm */}
        <div className="bg-white border border-[#DADCD3] rounded-xl p-4 mb-6 flex flex-col md:flex-row items-stretch md:items-center justify-between gap-4 shadow-sm">
          {/* Tabs trạng thái */}
          <div className="flex items-center gap-1.5 overflow-x-auto pb-1 md:pb-0">
            {tabs.map((tab) => {
              const isActive = selectedStatus === tab.key;
              return (
                <button
                  key={tab.key}
                  type="button"
                  onClick={() => setSelectedStatus(tab.key)}
                  className={`px-3 py-1.5 rounded-lg text-xs font-bold uppercase tracking-[0.05em] transition-all whitespace-nowrap cursor-pointer ${
                    isActive
                      ? 'bg-[#171A16] text-[#D7FF3F] shadow-sm'
                      : 'text-[#5E625A] hover:bg-[#F5F5F0] hover:text-[#111310]'
                  }`}
                >
                  {tab.label}
                </button>
              );
            })}
          </div>

          {/* Ô tìm kiếm */}
          <div className="relative min-w-[280px]">
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Tìm theo tên, email, số điện thoại..."
              className="w-full border border-[#DADCD3] rounded-lg pl-9 pr-3.5 py-2 text-xs text-[#111310] placeholder:text-[#8A8E85] focus:outline-none focus:ring-2 focus:ring-[#D7FF3F] focus:border-transparent transition-all"
            />
            <span className="absolute left-3 top-2.5 text-[#8A8E85] text-xs">🔍</span>
          </div>
        </div>

        {/* Bảng danh sách Lead */}
        <div className="bg-white border border-[#DADCD3] rounded-xl overflow-hidden shadow-sm">
          {/* Tiêu đề cột */}
          <div className="grid grid-cols-[2fr_1fr_1fr_1.3fr_1.2fr_1fr_0.8fr] px-6 py-3.5 bg-[#F5F5F0] border-b border-[#DADCD3] text-[11px] font-bold uppercase tracking-[0.08em] text-[#8A8E85]">
            <span>Khách Hàng & Liên Hệ</span>
            <span>Nguồn</span>
            <span>Trạng Thái</span>
            <span>Điểm AI Tiềm Năng</span>
            <span>Gói & Ghi Chú</span>
            <span>Nhân Viên Phụ Trách</span>
            <span className="text-right">Thao Tác</span>
          </div>

          {/* Skeleton loading */}
          {isLoading ? (
            <div className="p-12 text-center">
              <div className="w-8 h-8 border-3 border-[#171A16] border-t-[#D7FF3F] rounded-full animate-spin mx-auto mb-3"></div>
              <p className="text-xs text-[#5E625A]">Đang đồng bộ danh sách từ hệ thống CRM...</p>
            </div>
          ) : leads.length === 0 ? (
            <div className="p-12 text-center">
              <div className="text-3xl mb-2">📋</div>
              <p className="text-sm font-bold text-[#111310]">Không tìm thấy khách tiềm năng nào</p>
              <p className="text-xs text-[#5E625A] mt-1">Hãy thử đổi bộ lọc trạng thái hoặc từ khóa tìm kiếm.</p>
            </div>
          ) : (
            <div className="divide-y divide-[#DADCD3]">
              {leads.map((lead) => {
                const fullName = `${lead.lastName} ${lead.firstName}`.trim();
                return (
                  <div
                    key={lead.id}
                    className="grid grid-cols-[2fr_1fr_1fr_1.3fr_1.2fr_1fr_0.8fr] px-6 py-4 items-center hover:bg-[#F9F9F7] transition-colors"
                  >
                    {/* Tên & Liên hệ */}
                    <div className="flex items-center gap-3">
                      <div className="w-9 h-9 rounded-full bg-[#171A16] text-[#D7FF3F] font-bold text-xs flex items-center justify-center flex-shrink-0">
                        {lead.firstName?.[0] || 'K'}
                      </div>
                      <div className="min-w-0">
                        <Link
                          href={`/dashboard/leads/${lead.id}`}
                          className="text-sm font-bold text-[#111310] hover:text-[#526B00] transition-colors truncate block"
                        >
                          {fullName}
                        </Link>
                        <p className="text-[11px] text-[#8A8E85] truncate">{lead.email}</p>
                        <p className="text-[10px] text-[#5E625A] truncate">{lead.phone}</p>
                      </div>
                    </div>

                    {/* Nguồn */}
                    <span className="text-xs text-[#5E625A] font-medium">{lead.source || 'Trực tiếp'}</span>

                    {/* Trạng thái */}
                    <div>
                      <span
                        className={`text-[11px] font-semibold px-2.5 py-1 rounded-full border ${
                          statusBadgeStyles[lead.status] || 'bg-gray-100 text-gray-700'
                        }`}
                      >
                        {statusLabels[lead.status] || lead.status}
                      </span>
                    </div>

                    {/* Điểm AI */}
                    <div>
                      {getScoreBadge(lead.opportunityScore)}
                      {lead.scoreReason && (
                        <p className="text-[10px] text-[#8A8E85] mt-1 truncate max-w-[170px]" title={lead.scoreReason}>
                          {lead.scoreReason}
                        </p>
                      )}
                    </div>

                    {/* Gói & Ghi chú */}
                    <div className="min-w-0 pr-2">
                      <span className="text-xs font-semibold text-[#111310] block">{lead.interest || 'Chung'}</span>
                      <p className="text-[11px] text-[#8A8E85] truncate" title={lead.notes}>
                        {lead.notes || 'Chưa có ghi chú'}
                      </p>
                    </div>

                    {/* Nhân viên */}
                    <span className="text-xs text-[#5E625A]">
                      {lead.assignedStaffName || 'Chưa chỉ định'}
                    </span>

                    {/* Thao tác */}
                    <div className="text-right">
                      <Link
                        href={`/dashboard/leads/${lead.id}`}
                        className="inline-flex items-center gap-1 text-xs font-bold text-[#111310] bg-[#F5F5F0] hover:bg-[#DADCD3] px-3 py-1.5 rounded-lg transition-colors border border-[#DADCD3]"
                      >
                        <span>Chi tiết</span>
                        <span>→</span>
                      </Link>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </div>
    </RoleGuard>
  );
}
