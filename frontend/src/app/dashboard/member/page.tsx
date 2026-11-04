import Link from 'next/link';

const overviewCards = [
  { label: 'Next class', value: 'HIIT 06:00', sub: 'Friday 18 Jul' },
  { label: 'Weekly workouts', value: '3 / 4', sub: 'goal sessions' },
  { label: 'Current streak', value: '12 days', sub: '🔥 Keep it up' },
  { label: 'Membership', value: 'Perform', sub: 'Renews 14 Aug' },
];

const weekDays = [
  { day: 'Mon', status: 'done' },
  { day: 'Tue', status: 'done' },
  { day: 'Wed', status: 'done' },
  { day: 'Thu', status: 'scheduled' },
  { day: 'Fri', status: 'scheduled' },
  { day: 'Sat', status: 'rest' },
  { day: 'Sun', status: 'rest' },
];

const quickActions = [
  { label: 'Book a class', description: 'Find and reserve your next session', href: '/dashboard/member/book', emoji: '🗓️' },
  { label: 'Log workout', description: 'Record sets, reps and track live session', href: '/dashboard/member/workout-log', emoji: '💪' },
  { label: 'View progress', description: 'Check your streak, chart and PRs', href: '/dashboard/member/progress', emoji: '📈' },
  { label: 'Chat with AI coach', description: 'Get personalised training advice', href: '/dashboard/member/ai-coach', emoji: '🤖' },
];

const recentActivity = [
  { icon: '💪', title: 'Workout logged', desc: 'Upper body strength · 55 min', time: 'Today, 07:30' },
  { icon: '🗓️', title: 'Class booked', desc: 'HIIT Circuit · Fri 18 Jul 06:00', time: 'Yesterday' },
  { icon: '💪', title: 'Workout logged', desc: 'Lower body hypertrophy · 60 min', time: '13 Jul' },
  { icon: '🏆', title: 'Personal record', desc: 'Squat — 102.5 kg', time: '12 Jul' },
  { icon: '💪', title: 'Workout logged', desc: 'Push day · 50 min', time: '11 Jul' },
];

const statusStyle: Record<string, string> = {
  done: 'bg-[#D7FF3F] text-[#526B00] border-[#D7FF3F]',
  scheduled: 'bg-white text-[#5E625A] border-[#DADCD3]',
  rest: 'bg-[#F0F0EB] text-[#8A8E85] border-[#DADCD3]',
};

export default function MemberPage() {
  return (
    <div className="min-h-screen bg-[#F5F5F0]">
      {/* Page Header */}
      <div className="bg-white border-b border-[#DADCD3] px-8 py-6">
        <div className="flex items-start justify-between">
          <div>
            <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[#526B00] mb-1">
              MEMBER · CITY CENTRE
            </p>
            <h1 className="text-2xl font-bold text-[#111310]">Welcome back, Jane</h1>
            <p className="text-sm text-[#5E625A] mt-1">Tuesday 15 July 2025</p>
          </div>
          <Link href="/dashboard/member/book" className="bg-[#D7FF3F] text-[#111310] font-semibold rounded-lg px-4 py-2 text-sm hover:opacity-90">
            Book a class
          </Link>
        </div>
      </div>

      <div className="p-8 space-y-6">
        {/* Today overview */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {overviewCards.map((card) => (
            <div key={card.label} className="bg-white border border-[#DADCD3] rounded-xl p-5">
              <p className="text-[11px] font-semibold uppercase tracking-[0.08em] text-[#8A8E85] mb-2">{card.label}</p>
              <p className="text-xl font-bold text-[#111310]">{card.value}</p>
              <p className="text-xs text-[#5E625A] mt-1">{card.sub}</p>
            </div>
          ))}
        </div>

        {/* Workout week strip */}
        <div className="bg-white border border-[#DADCD3] rounded-xl p-5">
          <p className="text-xs font-semibold uppercase tracking-[0.08em] text-[#8A8E85] mb-3">This week</p>
          <div className="flex gap-2">
            {weekDays.map((d) => (
              <div
                key={d.day}
                className={`flex-1 text-center rounded-lg border px-2 py-3 text-xs font-semibold ${statusStyle[d.status]}`}
              >
                <p>{d.day}</p>
                <p className="mt-1 font-normal text-[10px]">
                  {d.status === 'done' ? '✓' : d.status === 'scheduled' ? '·' : '—'}
                </p>
              </div>
            ))}
          </div>
        </div>

        <div className="grid grid-cols-3 gap-4">
          {/* Quick actions */}
          <div className="col-span-2 space-y-1">
            <p className="text-xs font-semibold uppercase tracking-[0.08em] text-[#8A8E85] mb-3">Quick actions</p>
            <div className="grid grid-cols-3 gap-4">
              {quickActions.map((action) => (
                <Link
                  key={action.label}
                  href={action.href}
                  className="bg-white border border-[#DADCD3] rounded-xl p-5 hover:border-[#526B00] transition-colors"
                >
                  <p className="text-2xl mb-3">{action.emoji}</p>
                  <p className="text-sm font-semibold text-[#111310] mb-1">{action.label}</p>
                  <p className="text-xs text-[#5E625A]">{action.description}</p>
                </Link>
              ))}
            </div>
          </div>

          {/* Recent activity */}
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.08em] text-[#8A8E85] mb-3">Recent activity</p>
            <div className="bg-white border border-[#DADCD3] rounded-xl divide-y divide-[#DADCD3]">
              {recentActivity.map((item, i) => (
                <div key={i} className="flex items-start gap-3 px-4 py-3">
                  <span className="text-base mt-0.5">{item.icon}</span>
                  <div className="flex-1 min-w-0">
                    <p className="text-xs font-semibold text-[#111310]">{item.title}</p>
                    <p className="text-xs text-[#5E625A] truncate">{item.desc}</p>
                  </div>
                  <p className="text-[10px] text-[#8A8E85] whitespace-nowrap">{item.time}</p>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
