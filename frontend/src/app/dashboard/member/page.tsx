'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { useAuth } from '@/context/AuthContext';
import api from '@/lib/api';

export interface MemberProgress {
  customerName?: string;
  membershipPlan?: string;
  membershipStatus?: string;
  membershipExpiry?: string;
  currentStreakDays: number;
  workoutsThisMonth: number;
  currentWeightKg?: number;
  weightChangeKg?: number;
  latestBodyFatPct?: number;
  personalRecords?: Array<{ exerciseName: string; maxWeightKg: number; reps: number }>;
}

interface MemberActivity {
  kind: 'CHECK_IN' | 'WORKOUT';
  title: string;
  occurredAt: string;
}

export default function MemberHomePage() {
  const { user } = useAuth();
  const [data, setData] = useState<MemberProgress | null>(null);
  const [activities, setActivities] = useState<MemberActivity[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Form ghi nhận số đo
  const [showMetricModal, setShowMetricModal] = useState(false);
  const [weightKg, setWeightKg] = useState('');
  const [bodyFatPct, setBodyFatPct] = useState('');
  const [isSubmittingMetric, setIsSubmittingMetric] = useState(false);

  const fetchProgress = async () => {
    setIsLoading(true);
    try {
      setError(null);
      const [progressRes, metricsRes, activityRes] = await Promise.all([
        api.get('/progress/me'),
        api.get('/body-metrics/me'),
        api.get('/member/activity'),
      ]);
      setActivities(Array.isArray(activityRes.data) ? activityRes.data : []);
      const progress = progressRes.data;
      const latestMetric = Array.isArray(metricsRes.data) ? metricsRes.data[0] : null;
      setData({
        customerName: user?.fullName,
        currentStreakDays: progress.currentStreakDays ?? 0,
        workoutsThisMonth: progress.workoutsThisMonth ?? 0,
        currentWeightKg: progress.currentWeightKg ?? undefined,
        weightChangeKg: progress.weightChangeKg ?? undefined,
        latestBodyFatPct: latestMetric?.bodyFatPercent ?? undefined,
        personalRecords: progress.personalRecords ?? [],
      });
    } catch (err) {
      console.error('Failed to load member progress:', err);
      setData(null);
      setActivities([]);
      setError('Không thể tải tiến độ tập luyện.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchProgress();
  }, []);

  const handleSaveMetric = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!weightKg) return;

    setIsSubmittingMetric(true);
    try {
      await api.post('/body-metrics/me', {
        weightKg: parseFloat(weightKg),
        bodyFatPercent: bodyFatPct ? parseFloat(bodyFatPct) : undefined,
      });

      setShowMetricModal(false);
      setWeightKg('');
      setBodyFatPct('');
      await fetchProgress();
    } catch (err) {
      console.error('Failed to save metric', err);
      setError('Không thể lưu chỉ số. Vui lòng thử lại.');
    } finally {
      setIsSubmittingMetric(false);
    }
  };

  const memberName = data?.customerName || user?.fullName || 'Hội viên';

  return (
    <div className="min-h-screen bg-[#F5F5F0]">
      <div className="bg-white border-b border-[#DADCD3] px-8 py-6">
        <p className="text-[11px] font-semibold uppercase tracking-widest text-[#526B00]">TRUNG TÂM HỘI VIÊN</p>
        <h1 className="text-2xl font-black text-[#111310] mt-1">Chào mừng trở lại, {memberName}!</h1>
        <p className="text-sm text-[#5E625A] mt-1">Theo dõi kết quả tập luyện từ dữ liệu đã ghi nhận.</p>
      </div>
      <div className="p-8 max-w-7xl space-y-6">
        {isLoading && <p className="text-sm text-[#5E625A]">Đang tải tiến độ...</p>}
        {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="bg-white border border-[#DADCD3] rounded-2xl p-6">
            <p className="text-xs font-bold uppercase text-[#8A8E85]">Buổi tập tháng này</p>
            <p className="text-3xl font-black mt-2">{data?.workoutsThisMonth ?? 0}</p>
          </div>
          <div className="bg-white border border-[#DADCD3] rounded-2xl p-6">
            <p className="text-xs font-bold uppercase text-[#8A8E85]">Chuỗi ngày tập hiện tại</p>
            <p className="text-3xl font-black mt-2">{data?.currentStreakDays ?? 0} ngày</p>
          </div>
          <div className="bg-white border border-[#DADCD3] rounded-2xl p-6">
            <div className="flex justify-between gap-2">
              <p className="text-xs font-bold uppercase text-[#8A8E85]">Cân nặng gần nhất</p>
              <button onClick={() => setShowMetricModal(true)} className="text-xs font-bold text-[#526B00]">+ Ghi chỉ số</button>
            </div>
            <p className="text-3xl font-black mt-2">{data?.currentWeightKg != null ? `${data.currentWeightKg} kg` : 'Chưa có dữ liệu'}</p>
            {data?.weightChangeKg != null && <p className="text-xs text-[#5E625A] mt-2">Thay đổi từ lần đầu: {data.weightChangeKg} kg</p>}
            {data?.latestBodyFatPct != null && <p className="text-xs text-[#5E625A] mt-1">Mỡ cơ thể: {data.latestBodyFatPct}%</p>}
          </div>
        </div>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <div className="bg-white border border-[#DADCD3] rounded-2xl p-6">
            <h2 className="text-base font-bold text-[#111310]">Kỷ lục cá nhân</h2>
            {data?.personalRecords?.length ? (
              <ul className="mt-4 divide-y divide-[#DADCD3]">
                {data.personalRecords.map((record) => (
                  <li key={record.exerciseName} className="py-2 flex justify-between text-sm">
                    <span>{record.exerciseName}</span>
                    <span className="font-bold">{record.maxWeightKg} kg × {record.reps}</span>
                  </li>
                ))}
              </ul>
            ) : <p className="text-sm text-[#8A8E85] mt-4">Chưa có nhật ký tập để tính kỷ lục.</p>}
          </div>
          <div className="bg-white border border-[#DADCD3] rounded-2xl p-6">
            <h2 className="text-base font-bold text-[#111310]">Thao tác nhanh</h2>
            <div className="flex flex-wrap gap-3 mt-4">
              <Link href="/dashboard/member/progress" className="px-4 py-2 rounded-lg bg-[#D7FF3F] text-sm font-bold">Xem tiến độ</Link>
              <Link href="/dashboard/member/ai-coach" className="px-4 py-2 rounded-lg bg-[#171A16] text-white text-sm font-bold">AI Coach</Link>
            </div>
          </div>
        </div>
        <section className="bg-white border border-[#DADCD3] rounded-2xl p-6">
          <h2 className="text-base font-bold text-[#111310]">Hoạt động gần đây</h2>
          {activities.length ? (
            <ul className="mt-4 divide-y divide-[#DADCD3]">
              {activities.map((activity, index) => (
                <li key={`${activity.kind}-${activity.occurredAt}-${index}`} className="py-3 flex flex-wrap justify-between gap-2 text-sm">
                  <span className="font-medium">{activity.title}</span>
                  <time className="text-[#5E625A]" dateTime={activity.occurredAt}>
                    {new Date(activity.occurredAt).toLocaleString('vi-VN')}
                  </time>
                </li>
              ))}
            </ul>
          ) : !isLoading && !error ? (
            <p className="text-sm text-[#8A8E85] mt-4">Chưa có hoạt động được ghi nhận.</p>
          ) : null}
        </section>
      </div>
      {showMetricModal && (
        <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4">
          <form onSubmit={handleSaveMetric} className="bg-white rounded-2xl p-6 w-full max-w-sm space-y-4">
            <h2 className="text-lg font-bold">Ghi chỉ số cơ thể</h2>
            <label className="block text-sm">Cân nặng (kg)
              <input type="number" min="1" step="0.1" required value={weightKg} onChange={(e) => setWeightKg(e.target.value)}
                className="w-full border rounded-lg p-2 mt-1" />
            </label>
            <label className="block text-sm">Mỡ cơ thể (%)
              <input type="number" min="0" max="100" step="0.1" value={bodyFatPct} onChange={(e) => setBodyFatPct(e.target.value)}
                className="w-full border rounded-lg p-2 mt-1" />
            </label>
            <div className="flex gap-2">
              <button type="button" onClick={() => setShowMetricModal(false)} className="flex-1 border rounded-lg py-2">Hủy</button>
              <button type="submit" disabled={isSubmittingMetric} className="flex-1 bg-[#D7FF3F] rounded-lg py-2 font-bold">
                {isSubmittingMetric ? 'Đang lưu...' : 'Lưu'}
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}
