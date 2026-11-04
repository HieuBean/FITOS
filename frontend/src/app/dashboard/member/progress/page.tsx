'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import { getProgressSummary, ProgressSummary } from '@/lib/api';

const monthlyFrequencies = [
  { month: 'Tháng 5', count: 14, target: 16 },
  { month: 'Tháng 6', count: 18, target: 16 },
  { month: 'Tháng 7', count: 16, target: 16 },
  { month: 'Tháng 8', count: 12, target: 16 },
  { month: 'Tháng 9', count: 19, target: 16 },
  { month: 'Tháng 10', count: 21, target: 16 },
];

export default function ProgressPage() {
  const [progress, setProgress] = useState<ProgressSummary | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getProgressSummary(1).then((data) => {
      setProgress(data);
      setLoading(false);
    });
  }, []);

  const streak = progress?.streak ?? 12;
  const totalSessions = progress?.totalSessionsCompleted ?? 48;
  const thisMonth = progress?.sessionsThisMonth ?? 9;
  const prs = progress?.personalRecords ?? [
    { exerciseId: 1, exerciseName: 'Bench Press', maxWeightKg: 85, maxReps: 5 },
    { exerciseId: 7, exerciseName: 'Barbell Squat', maxWeightKg: 120, maxReps: 4 },
    { exerciseId: 9, exerciseName: 'Romanian Deadlift', maxWeightKg: 140, maxReps: 5 },
    { exerciseId: 10, exerciseName: 'Overhead Press', maxWeightKg: 55, maxReps: 6 },
  ];

  const maxCount = Math.max(...monthlyFrequencies.map((m) => m.count), 24);

  return (
    <div className="min-h-screen bg-[#F5F5F0]">
      {/* Header */}
      <div className="bg-white border-b border-[#DADCD3] px-8 py-6">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[#526B00] mb-1">
              MEMBER · PROGRESS &amp; ANALYTICS
            </p>
            <h1 className="text-2xl font-bold text-[#111310]">Tiến Độ &amp; Kỷ Lục Thể Lực</h1>
            <p className="text-sm text-[#5E625A] mt-1">Theo dõi chuỗi đi tập, biểu đồ tần suất và các mốc tạ kỷ lục (PRs)</p>
          </div>
          <div className="flex items-center gap-3">
            <Link
              href="/dashboard/member/workout-log"
              className="bg-[#D7FF3F] text-[#111310] font-bold rounded-xl px-4 py-2 text-sm hover:opacity-90 transition-opacity shadow-sm"
            >
              📝 Bắt đầu ghi log buổi tập
            </Link>
          </div>
        </div>
      </div>

      <div className="p-8 space-y-8">
        {/* KPI Stats Row */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div className="bg-white border border-[#DADCD3] rounded-2xl p-5 shadow-sm">
            <p className="text-[11px] font-semibold uppercase tracking-[0.08em] text-[#8A8E85] mb-1">
              Chuỗi đi tập liên tục
            </p>
            <div className="flex items-baseline gap-2">
              <span className="text-3xl font-black text-[#111310]">{streak}</span>
              <span className="text-sm font-bold text-[#526B00]">ngày 🔥</span>
            </div>
            <p className="text-xs text-[#526B00] mt-1.5 font-medium">Phong độ cao nhất tháng này!</p>
          </div>

          <div className="bg-white border border-[#DADCD3] rounded-2xl p-5 shadow-sm">
            <p className="text-[11px] font-semibold uppercase tracking-[0.08em] text-[#8A8E85] mb-1">
              Tổng số buổi tập
            </p>
            <div className="flex items-baseline gap-2">
              <span className="text-3xl font-black text-[#111310]">{totalSessions}</span>
              <span className="text-sm font-semibold text-[#5E625A]">buổi</span>
            </div>
            <p className="text-xs text-[#5E625A] mt-1.5">Tích lũy từ khi tham gia</p>
          </div>

          <div className="bg-white border border-[#DADCD3] rounded-2xl p-5 shadow-sm">
            <p className="text-[11px] font-semibold uppercase tracking-[0.08em] text-[#8A8E85] mb-1">
              Buổi tập trong tháng
            </p>
            <div className="flex items-baseline gap-2">
              <span className="text-3xl font-black text-[#111310]">{thisMonth}</span>
              <span className="text-sm font-semibold text-[#5E625A]">/ 16 mục tiêu</span>
            </div>
            <div className="w-full bg-[#FAF9F5] border border-[#DADCD3] h-2 rounded-full mt-2 overflow-hidden">
              <div
                className="bg-[#D7FF3F] h-full rounded-full transition-all duration-500"
                style={{ width: `${Math.min(100, (thisMonth / 16) * 100)}%` }}
              />
            </div>
          </div>

          <div className="bg-white border border-[#DADCD3] rounded-2xl p-5 shadow-sm">
            <p className="text-[11px] font-semibold uppercase tracking-[0.08em] text-[#8A8E85] mb-1">
              Kỷ lục cá nhân (PRs)
            </p>
            <div className="flex items-baseline gap-2">
              <span className="text-3xl font-black text-[#111310]">{prs.length}</span>
              <span className="text-sm font-bold text-[#526B00]">bài phá PR 🏆</span>
            </div>
            <p className="text-xs text-[#526B00] mt-1.5 font-medium">+2 bài so với tháng trước</p>
          </div>
        </div>

        {/* Pure CSS Bar Chart: Workout Frequency */}
        <div className="bg-white border border-[#DADCD3] rounded-2xl p-6 shadow-sm">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 mb-6">
            <div>
              <h2 className="text-sm font-bold uppercase tracking-[0.08em] text-[#111310]">
                Tần Suất Đi Tập 6 Tháng Gần Nhất (Pure CSS Bar Chart)
              </h2>
              <p className="text-xs text-[#5E625A] mt-0.5">Số buổi thực tế hoàn thành so với mục tiêu hàng tháng (16 buổi/tháng)</p>
            </div>
            <div className="flex items-center gap-4 text-xs font-semibold">
              <span className="flex items-center gap-1.5">
                <span className="w-3 h-3 rounded bg-[#111310] inline-block" /> Hoàn thành
              </span>
              <span className="flex items-center gap-1.5">
                <span className="w-3 h-3 rounded bg-[#D7FF3F] inline-block" /> Đạt &amp; Vượt mục tiêu
              </span>
            </div>
          </div>

          {/* Bar Chart Container */}
          <div className="h-56 flex items-end gap-6 sm:gap-10 pt-6 px-4 border-b border-[#DADCD3]">
            {monthlyFrequencies.map((m, idx) => {
              const heightPercent = (m.count / maxCount) * 100;
              const isExceed = m.count >= m.target;
              return (
                <div key={idx} className="flex-1 flex flex-col items-center h-full justify-end group relative">
                  {/* Tooltip on Hover */}
                  <div className="opacity-0 group-hover:opacity-100 transition-opacity absolute -top-8 bg-[#111310] text-[#D7FF3F] text-[10px] font-bold py-1 px-2 rounded pointer-events-none whitespace-nowrap shadow-lg">
                    {m.count} buổi ({isExceed ? 'Vượt chỉ tiêu' : 'Chưa đạt'})
                  </div>

                  <span className="text-xs font-bold text-[#111310] mb-1.5">
                    {m.count}
                  </span>

                  {/* Bar */}
                  <div
                    className={`w-full max-w-[48px] rounded-t-xl transition-all duration-700 ease-out ${
                      isExceed ? 'bg-[#D7FF3F] border border-[#526B00]/30' : 'bg-[#111310]'
                    }`}
                    style={{ height: `${heightPercent}%` }}
                  />

                  {/* Month Label */}
                  <span className="text-xs font-semibold text-[#5E625A] mt-3 whitespace-nowrap">
                    {m.month}
                  </span>
                </div>
              );
            })}
          </div>
        </div>

        {/* PRs Table & AI Summary Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
          {/* PRs Table */}
          <div className="lg:col-span-7 bg-white border border-[#DADCD3] rounded-2xl p-6 shadow-sm">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-sm font-semibold uppercase tracking-[0.08em] text-[#8A8E85]">
                Bảng Kỷ Lục Sức Mạnh Cá Nhân (PRs)
              </h2>
              <span className="text-xs font-bold text-[#526B00] bg-[#D7FF3F]/40 px-2.5 py-0.5 rounded-full">
                1RM &amp; Max Load
              </span>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="bg-[#FAF9F5] border-b border-[#DADCD3] text-xs font-semibold text-[#8A8E85] uppercase">
                  <tr>
                    <th className="px-4 py-3">Bài tập</th>
                    <th className="px-4 py-3">Mức tạ cao nhất</th>
                    <th className="px-4 py-3">Số reps</th>
                    <th className="px-4 py-3 text-right">Đánh giá</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#DADCD3]">
                  {prs.map((pr, index) => (
                    <tr key={index} className="hover:bg-[#FAF9F5] transition-colors">
                      <td className="px-4 py-3.5 font-bold text-[#111310]">
                        {pr.exerciseName}
                      </td>
                      <td className="px-4 py-3.5">
                        <span className="text-base font-black text-[#111310]">{pr.maxWeightKg}</span>
                        <span className="text-xs text-[#5E625A] ml-1">kg</span>
                      </td>
                      <td className="px-4 py-3.5 font-semibold text-[#5E625A]">
                        {pr.maxReps} reps
                      </td>
                      <td className="px-4 py-3.5 text-right">
                        <span className="text-xs font-bold bg-[#D7FF3F] text-[#111310] px-2.5 py-1 rounded-full">
                          +5% Tiến bộ
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* AI Summary Card */}
          <div className="lg:col-span-5 bg-gradient-to-br from-[#111310] to-[#20251F] text-white rounded-2xl p-6 shadow-lg border border-[#D7FF3F]/20 flex flex-col justify-between">
            <div>
              <div className="flex items-center gap-2 mb-3">
                <span className="w-6 h-6 rounded-md bg-[#D7FF3F] text-[#111310] font-black text-xs flex items-center justify-center">
                  AI
                </span>
                <span className="text-xs font-bold text-[#D7FF3F] uppercase tracking-wider">
                  FITOS AI Coach · Tóm tắt thể lực
                </span>
              </div>

              <h3 className="text-lg font-bold text-white mb-3">
                Tiến độ Hypertrophy đạt 112% kỳ vọng!
              </h3>

              <div className="space-y-3 text-xs text-gray-300 leading-relaxed">
                <p>
                  • <strong>Tần suất &amp; Phục hồi:</strong> Bạn duy trì chuỗi {streak} ngày đi tập liên tục rất ấn tượng. Khối lượng tạ bài Squat và Deadlift đang tăng đều đặn theo nguyên tắc Progressive Overload.
                </p>
                <p>
                  • <strong>Điểm cần chú ý:</strong> Nhóm cơ ngực trên đang có dấu hiệu chững tạ ở mốc 85kg. Nên tăng thời gian nghỉ giữa các hiệp nặng lên 120s và thêm 1 bài Incline Dumbbell Press.
                </p>
                <p>
                  • <strong>Gợi ý dinh dưỡng:</strong> Bổ sung 25g protein và 500ml nước điện giải ngay sau buổi tập để tối ưu hóa phục hồi cơ bắp.
                </p>
              </div>
            </div>

            <div className="mt-6 pt-4 border-t border-white/10 flex items-center justify-between">
              <span className="text-[11px] text-gray-400">Được cập nhật tự động từ log buổi tập</span>
              <Link
                href="/dashboard/member/ai-coach"
                className="text-xs font-bold text-[#D7FF3F] hover:underline flex items-center gap-1"
              >
                Hỏi AI Coach chi tiết &rarr;
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
