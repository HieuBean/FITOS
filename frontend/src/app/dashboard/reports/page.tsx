'use client';

import { useState } from 'react';
import Link from 'next/link';

type TabType = 'SALES' | 'MEMBERS' | 'CLASSES';

const salesData = [
  { plan: 'Gói 12 Tháng Gold VIP', count: 42, revenue: '336,000,000 đ', growth: '+18%' },
  { plan: 'Gói 6 Tháng Classic', count: 68, revenue: '272,000,000 đ', growth: '+12%' },
  { plan: 'Gói PT 30 Buổi Cá Nhân', count: 24, revenue: '360,000,000 đ', growth: '+25%' },
  { plan: 'Gói 1 Tháng Trải Nghiệm', count: 85, revenue: '85,000,000 đ', growth: '+5%' },
];

const classesData = [
  { name: 'HIIT Morning Circuit', coach: 'Coach Nam', occupancy: 92, sessions: 24, rating: 4.9 },
  { name: 'Power Vinyasa Yoga', coach: 'Coach Linh', occupancy: 88, sessions: 20, rating: 4.8 },
  { name: 'Rhythm Spinning', coach: 'Coach Tuan', occupancy: 96, sessions: 28, rating: 5.0 },
  { name: 'Pilates Core Rehab', coach: 'Coach Mai', occupancy: 75, sessions: 16, rating: 4.7 },
  { name: 'Boxing Conditioning', coach: 'Coach Huy', occupancy: 84, sessions: 18, rating: 4.8 },
];

const memberStats = [
  { metric: 'Tổng số hội viên hoạt động', value: '412 người', change: '+34 tháng này' },
  { metric: 'Tỷ lệ gia hạn hợp đồng (Retention)', value: '86.4%', change: '+3.2% so với Q2' },
  { metric: 'Tần suất đi tập trung bình', value: '3.6 buổi / tuần', change: 'Mức rất tích cực' },
  { metric: 'Hội viên có nguy cơ rời bỏ (At-risk Churn)', value: '18 người', change: 'Đã gửi voucher AI giữ chân' },
];

export default function ReportsPage() {
  const [activeTab, setActiveTab] = useState<TabType>('SALES');
  const [dateRange, setDateRange] = useState('MONTH');

  function handleExportCSV() {
    let csvContent = 'data:text/csv;charset=utf-8,\uFEFF';
    let filename = `fitos_report_${activeTab.toLowerCase()}_${dateRange.toLowerCase()}.csv`;

    if (activeTab === 'SALES') {
      csvContent += 'Gói Dịch Vụ,Số Lượng Bán,Doanh Thu,Tăng Trưởng\n';
      salesData.forEach((row) => {
        csvContent += `"${row.plan}","${row.count}","${row.revenue}","${row.growth}"\n`;
      });
    } else if (activeTab === 'CLASSES') {
      csvContent += 'Lớp Học,Huấn Luyện Viên,Tỷ Lệ Lấp Đầy,Số Ca Trong Tháng,Đánh Giá\n';
      classesData.forEach((row) => {
        csvContent += `"${row.name}","${row.coach}","${row.occupancy}%","${row.sessions}","${row.rating}/5.0"\n`;
      });
    } else {
      csvContent += 'Chỉ Số,Giá Trị Hiện Tại,Xu Hướng / Ghi Chú\n';
      memberStats.forEach((row) => {
        csvContent += `"${row.metric}","${row.value}","${row.change}"\n`;
      });
    }

    const encodedUri = encodeURI(csvContent);
    const link = document.createElement('a');
    link.setAttribute('href', encodedUri);
    link.setAttribute('download', filename);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  return (
    <div className="min-h-screen bg-[#F5F5F0]">
      {/* Header */}
      <div className="bg-white border-b border-[#DADCD3] px-8 py-6">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[#526B00] mb-1">
              MANAGEMENT · BUSINESS INTELLIGENCE
            </p>
            <h1 className="text-2xl font-bold text-[#111310]">Báo Cáo Hoạt Động &amp; Thống Kê Chuyên Sâu</h1>
            <p className="text-sm text-[#5E625A] mt-1">Phân tích đa chiều doanh thu bán gói, sức chứa lớp học và hành vi hội viên</p>
          </div>
          <div className="flex items-center gap-3">
            <select
              value={dateRange}
              onChange={(e) => setDateRange(e.target.value)}
              className="border border-[#DADCD3] bg-white rounded-xl px-3 py-2 text-xs font-semibold text-[#111310] focus:outline-none focus:ring-2 focus:ring-[#526B00]"
            >
              <option value="WEEK">Tuần này</option>
              <option value="MONTH">Tháng này (Tháng 10)</option>
              <option value="QUARTER">Quý 3 / 2026</option>
              <option value="YEAR">Cả năm 2026</option>
            </select>
            <button
              onClick={handleExportCSV}
              className="bg-[#111310] text-[#D7FF3F] font-bold rounded-xl px-4 py-2 text-xs hover:bg-black transition-colors shadow-sm"
            >
              📥 Xuất báo cáo (CSV)
            </button>
          </div>
        </div>

        {/* Tab Navigation */}
        <div className="flex gap-2 mt-6 border-b border-[#DADCD3] -mb-6">
          <button
            onClick={() => setActiveTab('SALES')}
            className={`pb-3 px-4 text-xs font-bold border-b-2 transition-all ${
              activeTab === 'SALES'
                ? 'border-[#111310] text-[#111310]'
                : 'border-transparent text-[#8A8E85] hover:text-[#111310]'
            }`}
          >
            💰 Doanh Thu &amp; Gói Dịch Vụ
          </button>
          <button
            onClick={() => setActiveTab('MEMBERS')}
            className={`pb-3 px-4 text-xs font-bold border-b-2 transition-all ${
              activeTab === 'MEMBERS'
                ? 'border-[#111310] text-[#111310]'
                : 'border-transparent text-[#8A8E85] hover:text-[#111310]'
            }`}
          >
            👥 Hội Viên &amp; Tỷ Lệ Giữ Chân
          </button>
          <button
            onClick={() => setActiveTab('CLASSES')}
            className={`pb-3 px-4 text-xs font-bold border-b-2 transition-all ${
              activeTab === 'CLASSES'
                ? 'border-[#111310] text-[#111310]'
                : 'border-transparent text-[#8A8E85] hover:text-[#111310]'
            }`}
          >
            🧘 Lớp Học &amp; Tỷ Lệ Lấp Đầy (Occupancy)
          </button>
        </div>
      </div>

      <div className="p-8 space-y-8">
        {/* Tab 1: Sales */}
        {activeTab === 'SALES' && (
          <div className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="bg-white border border-[#DADCD3] rounded-2xl p-5 shadow-sm">
                <p className="text-[11px] font-semibold uppercase text-[#8A8E85]">Tổng doanh thu tháng</p>
                <p className="text-3xl font-black text-[#111310] mt-1">1,053,000,000 đ</p>
                <p className="text-xs text-[#526B00] mt-1 font-semibold">+14.5% so với tháng trước</p>
              </div>
              <div className="bg-white border border-[#DADCD3] rounded-2xl p-5 shadow-sm">
                <p className="text-[11px] font-semibold uppercase text-[#8A8E85]">Hợp đồng mới ký kết</p>
                <p className="text-3xl font-black text-[#111310] mt-1">219 gói</p>
                <p className="text-xs text-[#526B00] mt-1 font-semibold">Tỷ lệ chốt sales: 38%</p>
              </div>
              <div className="bg-white border border-[#DADCD3] rounded-2xl p-5 shadow-sm">
                <p className="text-[11px] font-semibold uppercase text-[#8A8E85]">Giá trị trung bình đơn (AOV)</p>
                <p className="text-3xl font-black text-[#111310] mt-1">4,808,000 đ</p>
                <p className="text-xs text-[#5E625A] mt-1">Gói PT đóng góp 34% tổng tiền</p>
              </div>
            </div>

            <div className="bg-white border border-[#DADCD3] rounded-2xl p-6 shadow-sm">
              <h2 className="text-sm font-bold uppercase tracking-[0.08em] text-[#111310] mb-4">
                Cơ Cấu Doanh Thu Theo Từng Gói Tập
              </h2>
              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm">
                  <thead className="bg-[#FAF9F5] border-b border-[#DADCD3] text-xs font-semibold text-[#8A8E85] uppercase">
                    <tr>
                      <th className="px-6 py-3.5">Tên gói dịch vụ</th>
                      <th className="px-6 py-3.5">Số lượng bán</th>
                      <th className="px-6 py-3.5">Tổng doanh thu</th>
                      <th className="px-6 py-3.5 text-right">Tăng trưởng</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-[#DADCD3]">
                    {salesData.map((item, idx) => (
                      <tr key={idx} className="hover:bg-[#FAF9F5] transition-colors">
                        <td className="px-6 py-4 font-bold text-[#111310]">{item.plan}</td>
                        <td className="px-6 py-4 font-semibold text-[#5E625A]">{item.count} hợp đồng</td>
                        <td className="px-6 py-4 font-black text-[#111310]">{item.revenue}</td>
                        <td className="px-6 py-4 text-right">
                          <span className="text-xs font-bold bg-[#D7FF3F] text-[#111310] px-2.5 py-1 rounded-full">
                            {item.growth}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}

        {/* Tab 2: Members */}
        {activeTab === 'MEMBERS' && (
          <div className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
              {memberStats.map((st, i) => (
                <div key={i} className="bg-white border border-[#DADCD3] rounded-2xl p-5 shadow-sm">
                  <p className="text-[11px] font-semibold uppercase text-[#8A8E85]">{st.metric}</p>
                  <p className="text-2xl font-black text-[#111310] mt-1">{st.value}</p>
                  <p className="text-xs text-[#526B00] mt-1 font-medium">{st.change}</p>
                </div>
              ))}
            </div>

            <div className="bg-white border border-[#DADCD3] rounded-2xl p-6 shadow-sm">
              <h2 className="text-sm font-bold uppercase tracking-[0.08em] text-[#111310] mb-2">
                Phễu Chuyển Đổi &amp; Sức Khỏe Hội Viên (Member Retention)
              </h2>
              <p className="text-xs text-[#5E625A] mb-6">Tích hợp dữ liệu từ AI Commerce &amp; Churn Risk Model</p>

              <div className="space-y-4">
                <div>
                  <div className="flex justify-between text-xs font-bold mb-1">
                    <span>Hội viên đi tập đều đặn (&ge; 3 buổi/tuần)</span>
                    <span className="text-[#526B00]">286 người (69.4%)</span>
                  </div>
                  <div className="w-full bg-[#FAF9F5] border border-[#DADCD3] h-3 rounded-full overflow-hidden">
                    <div className="bg-[#526B00] h-full rounded-full" style={{ width: '69.4%' }} />
                  </div>
                </div>

                <div>
                  <div className="flex justify-between text-xs font-bold mb-1">
                    <span>Hội viên đi tập thưa dần (1-2 buổi/tuần)</span>
                    <span className="text-amber-600">108 người (26.2%)</span>
                  </div>
                  <div className="w-full bg-[#FAF9F5] border border-[#DADCD3] h-3 rounded-full overflow-hidden">
                    <div className="bg-amber-400 h-full rounded-full" style={{ width: '26.2%' }} />
                  </div>
                </div>

                <div>
                  <div className="flex justify-between text-xs font-bold mb-1">
                    <span>Hội viên vắng mặt trên 14 ngày (Nguy cơ Churn cao)</span>
                    <span className="text-red-600">18 người (4.4%)</span>
                  </div>
                  <div className="w-full bg-[#FAF9F5] border border-[#DADCD3] h-3 rounded-full overflow-hidden">
                    <div className="bg-red-500 h-full rounded-full" style={{ width: '4.4%' }} />
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Tab 3: Classes */}
        {activeTab === 'CLASSES' && (
          <div className="space-y-6">
            <div className="bg-white border border-[#DADCD3] rounded-2xl p-6 shadow-sm">
              <div className="flex items-center justify-between mb-4">
                <div>
                  <h2 className="text-sm font-bold uppercase tracking-[0.08em] text-[#111310]">
                    Tỷ Lệ Lấp Đầy &amp; Đánh Giá Các Lớp Nhóm (Occupancy Rate)
                  </h2>
                  <p className="text-xs text-[#5E625A] mt-0.5">Dữ liệu từ Smart Slot Optimizer &amp; Check-in</p>
                </div>
                <span className="text-xs font-bold text-[#526B00] bg-[#D7FF3F]/40 px-3 py-1 rounded-full">
                  Trung bình 87% lấp đầy
                </span>
              </div>

              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm">
                  <thead className="bg-[#FAF9F5] border-b border-[#DADCD3] text-xs font-semibold text-[#8A8E85] uppercase">
                    <tr>
                      <th className="px-6 py-3.5">Lớp tập</th>
                      <th className="px-6 py-3.5">Huấn luyện viên</th>
                      <th className="px-6 py-3.5">Tỷ lệ lấp đầy</th>
                      <th className="px-6 py-3.5">Số ca đã mở</th>
                      <th className="px-6 py-3.5 text-right">Đánh giá</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-[#DADCD3]">
                    {classesData.map((cls, idx) => (
                      <tr key={idx} className="hover:bg-[#FAF9F5] transition-colors">
                        <td className="px-6 py-4 font-bold text-[#111310]">{cls.name}</td>
                        <td className="px-6 py-4 text-[#5E625A] font-medium">{cls.coach}</td>
                        <td className="px-6 py-4">
                          <div className="flex items-center gap-2">
                            <div className="w-24 bg-[#FAF9F5] border border-[#DADCD3] h-2.5 rounded-full overflow-hidden">
                              <div
                                className={`h-full rounded-full ${
                                  cls.occupancy >= 90 ? 'bg-[#526B00]' : 'bg-[#D7FF3F]'
                                }`}
                                style={{ width: `${cls.occupancy}%` }}
                              />
                            </div>
                            <span className="text-xs font-bold text-[#111310]">{cls.occupancy}%</span>
                          </div>
                        </td>
                        <td className="px-6 py-4 text-xs font-semibold text-[#5E625A]">{cls.sessions} ca</td>
                        <td className="px-6 py-4 text-right">
                          <span className="text-xs font-bold bg-[#FAF9F5] border border-[#DADCD3] px-2.5 py-1 rounded-md text-[#111310]">
                            ⭐ {cls.rating}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

