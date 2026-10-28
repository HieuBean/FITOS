'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import api from '@/lib/api';
import RoleGuard from '@/components/RoleGuard';

import { mapCrmFunnel, mapRevenueChart, type DashboardStatsDto } from '@/lib/contracts';

type DashboardStats = DashboardStatsDto;
type MonthlyRevenue = ReturnType<typeof mapRevenueChart>[number];
type CrmFunnel = ReturnType<typeof mapCrmFunnel>;

export default function DashboardPage() {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [revenueData, setRevenueData] = useState<MonthlyRevenue[]>([]);
  const [funnel, setFunnel] = useState<CrmFunnel | null>(null);
  const [activeTab, setActiveTab] = useState<'OVERVIEW' | 'FUNNEL_ANALYTICS'>('OVERVIEW');
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchDashboardData = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const [statsRes, revRes, funnelRes] = await Promise.all([
        api.get('/dashboard/stats'),
        api.get('/dashboard/revenue-chart'),
        api.get('/dashboard/crm-funnel'),
      ]);

      setStats(statsRes.data);
      setRevenueData(mapRevenueChart(revRes.data));
      setFunnel(mapCrmFunnel(funnelRes.data));
    } catch (err) {
      console.error('Failed to fetch dashboard data:', err);
      setStats(null);
      setRevenueData([]);
      setFunnel(null);
      setError('Không thể tải dashboard. Vui lòng thử lại.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const maxBarValue = Math.max(
    ...revenueData.map((d) => d.revenue),
    1
  );

  return (
    <RoleGuard allowedRoles={['ADMIN', 'MANAGER']}>
      <div className="min-h-screen bg-[#F5F5F0]">
      {/* Tiêu đề trang */}
      <div className="bg-white border-b border-[#DADCD3] px-8 py-6">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[#526B00] mb-1">
              QUẢN LÝ STUDIO · CHI NHÁNH TRUNG TÂM
            </p>
            <h1 className="text-2xl font-black text-[#111310] tracking-tight">Bảng Điều Khiển Điều Hành</h1>
            <p className="text-sm text-[#5E625A] mt-0.5">
              Chỉ số KPI vận hành, tốc độ tăng trưởng doanh thu và hiệu quả chuyển đổi theo thời gian thực
            </p>
          </div>

          <div className="flex items-center gap-3">
            <Link
              href="/dashboard/create-lead"
              className="bg-[#D7FF3F] text-[#111310] font-bold text-xs uppercase tracking-[0.05em] rounded-lg px-4 py-2.5 hover:bg-[#C4E82E] active:scale-[0.99] transition-all shadow-sm flex items-center gap-1.5"
            >
              <span className="text-sm font-black">+</span>
              <span>Thêm khách tiềm năng</span>
            </Link>
          </div>
        </div>

        {/* Tab Switcher: Overview vs Funnel Analytics (Khớp 100% đặc tả Task #78) */}
        <div className="flex items-center gap-2 mt-6 pt-4 border-t border-[#DADCD3]">
          <button
            onClick={() => setActiveTab('OVERVIEW')}
            className={`px-4 py-2 rounded-lg text-xs font-bold uppercase tracking-wider transition-all cursor-pointer ${
              activeTab === 'OVERVIEW'
                ? 'bg-[#171A16] text-[#D7FF3F] shadow-sm'
                : 'text-[#5E625A] hover:bg-[#F5F5F0] hover:text-[#111310]'
            }`}
          >
            📊 Tổng quan vận hành
          </button>
          <button
            onClick={() => setActiveTab('FUNNEL_ANALYTICS')}
            className={`px-4 py-2 rounded-lg text-xs font-bold uppercase tracking-wider transition-all cursor-pointer ${
              activeTab === 'FUNNEL_ANALYTICS'
                ? 'bg-[#171A16] text-[#D7FF3F] shadow-sm'
                : 'text-[#5E625A] hover:bg-[#F5F5F0] hover:text-[#111310]'
            }`}
          >
            📈 Phân tích phễu chuyển đổi (Funnel Analytics)
          </button>
        </div>
      </div>

      {/* Nội dung Dashboard */}
      <div className="p-8 space-y-6">
        {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
        {isLoading && <p className="text-sm text-[#5E625A]">Đang tải số liệu...</p>}
        {activeTab === 'OVERVIEW' ? (
          <>
            {/* Hàng 1: 4 Thẻ KPI chính */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
              <div className="bg-white border border-[#DADCD3] rounded-2xl p-5 shadow-sm">
                <p className="text-[11px] font-bold uppercase tracking-[0.08em] text-[#8A8E85] mb-2">
                  Doanh thu tháng này
                </p>
                <p className="text-3xl font-black text-[#111310]">
                  {(stats?.revenueThisMonth ?? 0).toLocaleString()} <span className="text-base font-bold text-[#8A8E85]">VND</span>
                </p>
                <p className="text-xs text-[#526B00] mt-1.5 font-bold flex items-center gap-1">
                  <span>Tiền thu từ thanh toán thành công</span>
                </p>
              </div>

              <div className="bg-white border border-[#DADCD3] rounded-2xl p-5 shadow-sm">
                <p className="text-[11px] font-bold uppercase tracking-[0.08em] text-[#8A8E85] mb-2">
                  Hội Viên Đang Hoạt Động
                </p>
                <p className="text-3xl font-black text-[#111310]">
                  {(stats?.activeMembers ?? 0).toLocaleString()}
                </p>
                <p className="text-xs text-[#526B00] mt-1.5 font-bold flex items-center gap-1">
                  <span>Membership còn hiệu lực</span>
                </p>
              </div>

              <div className="bg-white border border-[#DADCD3] rounded-2xl p-5 shadow-sm">
                <p className="text-[11px] font-bold uppercase tracking-[0.08em] text-[#8A8E85] mb-2">
                  Lớp Học Hôm Nay
                </p>
                <p className="text-3xl font-black text-[#111310]">
                  {stats?.classesToday ?? 0} <span className="text-base font-bold text-[#8A8E85]">lớp</span>
                </p>
                <p className="text-xs text-[#5E625A] mt-1.5 font-medium">
                  {stats?.bookedSpotsToday ?? 0} / {stats?.totalCapacityToday ?? 0} chỗ đã đặt
                </p>
              </div>

              <div className="bg-white border border-[#DADCD3] rounded-2xl p-5 shadow-sm">
                <p className="text-[11px] font-bold uppercase tracking-[0.08em] text-[#8A8E85] mb-2">
                  Tỷ Lệ Lấp Đầy Lớp
                </p>
                <p className="text-3xl font-black text-[#111310]">
                  {stats?.classFillRate ?? 0}%
                </p>
                <p className="text-xs text-[#526B00] mt-1.5 font-bold flex items-center gap-1">
                  <span>Tính từ lịch lớp hôm nay</span>
                </p>
              </div>
            </div>

            {/* Hàng 2: 4 Thẻ KPI phụ */}
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              <div className="bg-white border border-[#DADCD3] rounded-xl p-4 shadow-sm">
                <p className="text-[10px] font-bold uppercase tracking-[0.08em] text-[#8A8E85] mb-1">
                  Chuyển Đổi Lead
                </p>
                <p className="text-2xl font-black text-[#111310]">
                  {stats?.leadConversionRate ?? 0}%
                </p>
                <p className="text-[11px] text-[#5E625A] mt-0.5">Lead tạo trong tháng này</p>
              </div>

              <div className="bg-white border border-[#DADCD3] rounded-xl p-4 shadow-sm">
                <p className="text-[10px] font-bold uppercase tracking-[0.08em] text-[#8A8E85] mb-1">
                  Lead Mới Tháng Này
                </p>
                <p className="text-2xl font-black text-[#111310]">
                  {stats?.newLeadsThisMonth ?? 0}
                </p>
                <p className="text-[11px] text-[#526B00] mt-0.5 font-semibold">Dữ liệu CRM</p>
              </div>

              <div className="bg-white border border-[#DADCD3] rounded-xl p-4 shadow-sm">
                <p className="text-[10px] font-bold uppercase tracking-[0.08em] text-[#8A8E85] mb-1">
                  Đã Chuyển Đổi
                </p>
                <p className="text-2xl font-black text-[#111310]">
                  {funnel?.convertedCount ?? 0}
                </p>
                <p className="text-[11px] text-[#526B00] mt-0.5 font-semibold">Từ lead</p>
              </div>

              <div className="bg-white border border-[#DADCD3] rounded-xl p-4 shadow-sm">
                <p className="text-[10px] font-bold uppercase tracking-[0.08em] text-[#8A8E85] mb-1">
                  Lead Ở Bước Tập Thử
                </p>
                <p className="text-2xl font-black text-[#111310]">
                  {funnel?.trialsCount ?? 0}
                </p>
                <p className="text-[11px] text-[#5E625A] mt-0.5">Theo trạng thái CRM</p>
              </div>
            </div>

            {/* Hàng 3: Biểu đồ doanh thu & Phễu CRM */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              {/* Biểu đồ cột Doanh thu */}
              <div className="bg-white border border-[#DADCD3] rounded-2xl p-6 shadow-sm">
                <div className="flex items-center justify-between mb-6">
                  <div>
                    <p className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#526B00] mb-0.5">
                      HIỆU SUẤT TÀI CHÍNH
                    </p>
                    <h2 className="text-base font-bold text-[#111310]">
                      Doanh thu 6 tháng gần nhất (VND)
                    </h2>
                  </div>
                  <div className="flex items-center gap-3 text-xs">
                    <span className="flex items-center gap-1.5 text-[#111310] font-semibold">
                      <span className="w-3 h-3 rounded bg-[#171A16] inline-block"></span>
                      Thực tế
                    </span>
                  </div>
                </div>

                <div className="h-56 flex items-end justify-between gap-3 pt-6 pb-2 px-2 border-b border-[#DADCD3]">
                  {revenueData.map((item) => {
                    const actualHeight = Math.round((item.revenue / maxBarValue) * 100);
                    return (
                      <div key={item.month} className="flex-1 flex flex-col items-center h-full justify-end group">
                        <div className="w-full flex items-end justify-center gap-1.5 h-full">
                          <div
                            className="w-5 bg-[#171A16] rounded-t-md transition-all duration-300 relative group-hover:bg-black"
                            style={{ height: `${actualHeight}%` }}
                          >
                            <div className="absolute -top-7 left-1/2 -translate-x-1/2 bg-[#171A16] text-[#D7FF3F] text-[10px] font-mono font-bold px-1.5 py-0.5 rounded opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap pointer-events-none z-10">
                              {item.revenue.toLocaleString()} VND
                            </div>
                          </div>
                        </div>
                        <span className="text-xs font-bold text-[#5E625A] mt-2">{item.month}</span>
                      </div>
                    );
                  })}
                </div>
                <div className="flex items-center justify-between text-xs text-[#8A8E85] mt-3">
                  <span>Theo dõi 6 tháng gần nhất</span>
                  <span className="text-[#526B00] font-bold">Dữ liệu thanh toán thực tế</span>
                </div>
              </div>

              <div className="bg-white border border-[#DADCD3] rounded-2xl p-6 shadow-sm">
                <p className="text-[10px] font-bold uppercase text-[#526B00]">VẬN HÀNH HÔM NAY</p>
                <h2 className="text-base font-bold text-[#111310] mt-1">Lấp đầy lớp</h2>
                <p className="text-3xl font-black mt-6">{stats?.bookedSpotsToday ?? 0} / {stats?.totalCapacityToday ?? 0}</p>
                <p className="text-sm text-[#5E625A]">chỗ đã đặt trên các lớp hôm nay</p>
                <p className="text-sm font-bold text-[#526B00] mt-3">{stats?.classFillRate ?? 0}% công suất</p>
              </div>
            </div>

            <div className="bg-white border border-[#DADCD3] rounded-2xl p-6 shadow-sm">
              <h2 className="text-base font-bold text-[#111310]">Công việc CRM</h2>
              <p className="text-sm text-[#5E625A] mt-2">Xem và chăm sóc các lead đang cần theo dõi trong danh sách CRM.</p>
              <Link href="/dashboard/leads" className="inline-block text-sm font-bold text-[#526B00] mt-3">Mở danh sách lead →</Link>
            </div>
          </>
        ) : (
          /* Tab Phân Tích Phễu Chuyển Đổi (Funnel Analytics Tab - Khớp 1.0h 🟡 Task #78) */
          <div className="space-y-6">
            <div className="bg-white border border-[#DADCD3] rounded-2xl p-6 shadow-sm">
              <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-6">
                <div>
                  <p className="text-[10px] font-bold uppercase tracking-[0.1em] text-[#526B00] mb-0.5">
                    PHÂN TÍCH CHUYÊN SÂU
                  </p>
                  <h2 className="text-lg font-bold text-[#111310]">
                    Phễu Chuyển Đổi Khách Hàng (Customer Acquisition Funnel)
                  </h2>
                  <p className="text-xs text-[#5E625A] mt-0.5">
                    Đo lường mức độ hao hụt và tỷ lệ giữ chân từ lần chạm đầu tiên đến hội viên thân thiết
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <span className="text-xs font-bold text-[#526B00] bg-[#D7FF3F]/30 px-3 py-1.5 rounded-lg border border-[#D7FF3F]">
                    Tỷ lệ chuyển đổi tổng: {funnel?.conversionRate ?? 0}%
                  </span>
                </div>
              </div>

              {/* Phễu chi tiết 3 tầng */}
              <div className="space-y-6 my-6 max-w-4xl mx-auto">
                {/* Tầng 1: Inbound Leads */}
                <div className="bg-[#F5F5F0] border border-[#DADCD3] rounded-2xl p-5">
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center gap-3">
                      <div className="w-8 h-8 rounded-lg bg-[#171A16] text-[#D7FF3F] font-black text-sm flex items-center justify-center">
                        1
                      </div>
                      <div>
                        <h3 className="text-sm font-bold text-[#111310]">Khách Tiềm Năng Tiếp Cận (Inbound Leads)</h3>
                        <p className="text-xs text-[#5E625A]">Từ Walk-in, Instagram, Giới thiệu & Quảng cáo</p>
                      </div>
                    </div>
                    <span className="text-base font-black text-[#111310] font-mono">
                      {funnel?.leadsCount ?? 0} khách (100%)
                    </span>
                  </div>
                  <div className="w-full h-3 rounded-full bg-white overflow-hidden border border-[#DADCD3]">
                    <div className="h-full bg-[#171A16] rounded-full w-full"></div>
                  </div>
                </div>

                {/* Tầng 2: Trials */}
                <div className="bg-[#F5F5F0] border border-[#DADCD3] rounded-2xl p-5">
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center gap-3">
                      <div className="w-8 h-8 rounded-lg bg-[#D7FF3F] text-[#111310] font-black text-sm flex items-center justify-center">
                        2
                      </div>
                      <div>
                        <h3 className="text-sm font-bold text-[#111310]">Lên Lịch Tập Thử (Studio Trial Scheduled)</h3>
                        <p className="text-xs text-[#5E625A]">Đã tham gia trải nghiệm lớp hoặc buổi 1-on-1</p>
                      </div>
                    </div>
                    <span className="text-base font-black text-[#526B00] font-mono">
                      {funnel?.trialsCount ?? 0} khách ({funnel?.leadsCount ? Math.round((funnel.trialsCount / funnel.leadsCount) * 100) : 0}%)
                    </span>
                  </div>
                  <div className="w-full h-3 rounded-full bg-white overflow-hidden border border-[#DADCD3]">
                    <div
                      className="h-full bg-[#D7FF3F] rounded-full"
                      style={{
                        width: `${funnel?.leadsCount ? Math.round((funnel.trialsCount / funnel.leadsCount) * 100) : 0}%`,
                      }}
                    ></div>
                  </div>
                </div>

                {/* Tầng 3: Members */}
                <div className="bg-[#F5F5F0] border border-[#DADCD3] rounded-2xl p-5">
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center gap-3">
                      <div className="w-8 h-8 rounded-lg bg-emerald-500 text-white font-black text-sm flex items-center justify-center">
                        3
                      </div>
                      <div>
                        <h3 className="text-sm font-bold text-[#111310]">Lead Đã Chuyển Thành Customer</h3>
                        <p className="text-xs text-[#5E625A]">Chưa đồng nghĩa đã mua membership</p>
                      </div>
                    </div>
                    <span className="text-base font-black text-[#111310] font-mono">
                      {(funnel?.convertedCount ?? 0).toLocaleString()} customer
                    </span>
                  </div>
                  <div className="w-full h-3 rounded-full bg-white overflow-hidden border border-[#DADCD3]">
                    <div className="h-full bg-emerald-500 rounded-full" style={{ width: `${funnel?.leadsCount ? Math.round((funnel.convertedCount / funnel.leadsCount) * 100) : 0}%` }}></div>
                  </div>
                </div>
              </div>

            </div>
          </div>
        )}
      </div>
    </div>
    </RoleGuard>
  );
}
