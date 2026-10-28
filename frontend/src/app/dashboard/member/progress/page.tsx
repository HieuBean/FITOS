'use client';

import { useState } from 'react';

const stats = [
  { label: 'Total workouts', value: '48', change: '▲ +4 this month' },
  { label: 'Current Streak', value: '12 days', change: '🔥 Personal best: 21d' },
  { label: 'This Month', value: '14 sessions', change: 'On track for goal' },
  { label: 'Active Days / Wk', value: '4.2 days', change: 'Optimal recovery' },
];

const weeklyFrequency = [
  { week: 'W1 Sep', count: 3, max: 6 },
  { week: 'W2 Sep', count: 4, max: 6 },
  { week: 'W3 Sep', count: 5, max: 6 },
  { week: 'W4 Sep', count: 4, max: 6 },
  { week: 'W1 Oct', count: 5, max: 6 },
  { week: 'W2 Oct', count: 4, max: 6 },
  { week: 'W3 Oct', count: 6, max: 6 },
  { week: 'W4 Oct', count: 5, max: 6 },
];

const prs = [
  { exercise: 'Barbell Back Squat', value: '120 kg', date: '21 Oct 2025', icon: '🏋️', diff: '+5 kg' },
  { exercise: 'Conventional Deadlift', value: '150 kg', date: '18 Oct 2025', icon: '💪', diff: '+10 kg' },
  { exercise: 'Flat Barbell Bench Press', value: '90 kg', date: '14 Oct 2025', icon: '⚡', diff: '+2.5 kg' },
  { exercise: '5KM Treadmill Pace', value: '23:45 min', date: '10 Oct 2025', icon: '🏃', diff: '-45 sec' },
  { exercise: 'Weighted Pull-ups', value: '+15 kg × 6', date: '05 Oct 2025', icon: '🎯', diff: '+1 rep' },
  { exercise: 'Overhead Shoulder Press', value: '62.5 kg', date: '28 Sep 2025', icon: '🔥', diff: '+2.5 kg' },
];

const recentSessions = [
  { date: '24 Oct 2025', type: 'Upper Body Hypertrophy', duration: '55 min', volume: '14,250 kg', notes: 'Hit Bench Press PR' },
  { date: '22 Oct 2025', type: 'Lower Body Strength', duration: '60 min', volume: '18,400 kg', notes: 'Solid 4x5 squats' },
  { date: '20 Oct 2025', type: 'HIIT Conditioning (Class)', duration: '45 min', volume: '—', notes: 'Average HR 158 bpm' },
  { date: '18 Oct 2025', type: 'Deadlift & Posterior Chain', duration: '50 min', volume: '12,900 kg', notes: 'RPE 8.5 on top set' },
];

export default function MemberProgressPage() {
  const [filterPeriod, setFilterPeriod] = useState<'30D' | '90D' | '1Y'>('30D');

  return (
    <div className="min-h-screen bg-[#F5F5F0]">
      {/* Page Header */}
      <div className="bg-white border-b border-[#DADCD3] px-8 py-6">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[#526B00] mb-1">
              MEMBER · ANALYTICS
            </p>
            <h1 className="text-2xl font-bold text-[#111310]">My Progress & PRs</h1>
            <p className="text-sm text-[#5E625A] mt-1">
              Detailed tracking of training frequency, records, and AI coaching insights
            </p>
          </div>

          <div className="flex gap-2">
            {(['30D', '90D', '1Y'] as const).map((period) => (
              <button
                key={period}
                onClick={() => setFilterPeriod(period)}
                className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-colors ${
                  filterPeriod === period
                    ? 'bg-[#171A16] text-[#D7FF3F]'
                    : 'bg-white border border-[#DADCD3] text-[#5E625A] hover:bg-black/5'
                }`}
              >
                {period === '30D' ? 'Last 30 Days' : period === '90D' ? 'Last 90 Days' : 'This Year'}
              </button>
            ))}
          </div>
        </div>
      </div>

      <div className="p-8 max-w-7xl mx-auto space-y-8">
        {/* Top Stats Cards */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {stats.map((stat) => (
            <div key={stat.label} className="bg-white border border-[#DADCD3] rounded-xl p-5 shadow-sm">
              <p className="text-[11px] font-semibold uppercase tracking-[0.08em] text-[#8A8E85] mb-1">
                {stat.label}
              </p>
              <p className="text-3xl font-extrabold text-[#111310]">{stat.value}</p>
              <p className="text-xs text-[#526B00] mt-1 font-medium">{stat.change}</p>
            </div>
          ))}
        </div>

        {/* AI Performance Summary Card */}
        <div className="bg-[#171A16] text-[#ECEDE6] rounded-2xl p-6 md:p-8 border border-[#D7FF3F]/40 shadow-md">
          <div className="flex items-center gap-2.5 mb-3">
            <span className="w-2.5 h-2.5 rounded-full bg-[#D7FF3F] animate-pulse" />
            <span className="text-xs font-bold uppercase tracking-widest text-[#D7FF3F]">
              AI Coach Weekly Summary
            </span>
          </div>
          <h2 className="text-xl font-bold text-white mb-2">
            Outstanding volume consistency — Deadlift +10kg PR
          </h2>
          <p className="text-sm text-[#8A8E85] leading-relaxed max-w-3xl mb-4">
            You completed 5 sessions this week with total tonnage reaching 45,550 kg (+12% vs last week).
            Your recovery markers are optimal with a 12-day active streak. For your next cycle, consider adding
            one extra deload session before testing your Squat 1RM.
          </p>
          <div className="inline-flex items-center gap-2 text-xs font-semibold text-[#D7FF3F] bg-white/10 px-3 py-1.5 rounded-lg">
            <span>💡 Suggested Target: Add 2.5kg to Dumbbell Incline Press next Monday</span>
          </div>
        </div>

        {/* Pure CSS Bar Chart: Workout Frequency */}
        <div className="bg-white border border-[#DADCD3] rounded-xl p-6 shadow-sm">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="text-base font-bold text-[#111310]">Workout Frequency</h2>
              <p className="text-xs text-[#8A8E85]">Sessions completed per week</p>
            </div>
            <div className="flex items-center gap-4 text-xs font-medium text-[#5E625A]">
              <span className="flex items-center gap-1.5">
                <span className="w-3 h-3 rounded bg-[#D7FF3F] border border-[#111310]" /> Goal: 4+ sessions/week
              </span>
            </div>
          </div>

          {/* Bar Chart Container */}
          <div className="h-48 flex items-end justify-between gap-3 pt-6 pb-2 px-4 border-b border-[#DADCD3]">
            {weeklyFrequency.map((item, i) => {
              const heightPct = Math.round((item.count / item.max) * 100);
              const isGoalMet = item.count >= 4;
              return (
                <div key={i} className="flex-1 flex flex-col items-center gap-2 group h-full justify-end">
                  <span className="text-[11px] font-bold text-[#111310] opacity-0 group-hover:opacity-100 transition-opacity">
                    {item.count}d
                  </span>
                  <div className="w-full max-w-[48px] bg-[#F5F5F0] rounded-t-lg h-full flex items-end overflow-hidden">
                    <div
                      style={{ height: `${heightPct}%` }}
                      className={`w-full rounded-t-lg transition-all duration-300 ${
                        isGoalMet ? 'bg-[#D7FF3F] hover:bg-[#C4E82E]' : 'bg-[#526B00]'
                      }`}
                    />
                  </div>
                  <span className="text-[10px] font-semibold text-[#8A8E85] whitespace-nowrap">
                    {item.week}
                  </span>
                </div>
              );
            })}
          </div>
        </div>

        {/* Personal Records Grid */}
        <div className="bg-white border border-[#DADCD3] rounded-xl p-6 shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h2 className="text-base font-bold text-[#111310]">Personal Records (PRs)</h2>
              <p className="text-xs text-[#8A8E85]">Your all-time verified benchmark lifts</p>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {prs.map((pr) => (
              <div
                key={pr.exercise}
                className="border border-[#DADCD3] rounded-xl p-4 hover:border-[#171A16] transition-colors flex items-center justify-between"
              >
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-lg bg-[#F5F5F0] flex items-center justify-center text-xl">
                    {pr.icon}
                  </div>
                  <div>
                    <h3 className="text-xs font-semibold text-[#8A8E85]">{pr.exercise}</h3>
                    <p className="text-lg font-extrabold text-[#111310]">{pr.value}</p>
                    <span className="text-[10px] text-[#5E625A]">{pr.date}</span>
                  </div>
                </div>
                <span className="text-xs font-bold px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200">
                  {pr.diff}
                </span>
              </div>
            ))}
          </div>
        </div>

        {/* Recent Workout Sessions Table */}
        <div className="bg-white border border-[#DADCD3] rounded-xl overflow-hidden shadow-sm">
          <div className="p-5 border-b border-[#DADCD3]">
            <h2 className="text-base font-bold text-[#111310]">Recent Logged Sessions</h2>
          </div>
          <div className="divide-y divide-[#DADCD3]">
            {recentSessions.map((s, idx) => (
              <div key={idx} className="p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                <div>
                  <span className="text-xs text-[#8A8E85] block">{s.date}</span>
                  <span className="text-sm font-bold text-[#111310]">{s.type}</span>
                  <p className="text-xs text-[#5E625A]">{s.notes}</p>
                </div>
                <div className="flex items-center gap-4 text-xs">
                  <span className="font-semibold text-[#111310]">⏱ {s.duration}</span>
                  <span className="font-semibold text-[#526B00]">🏋️ {s.volume}</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
