'use client';
import Link from 'next/link';
import { usePathname } from 'next/navigation';

const navGroups = [
  {
    label: 'OVERVIEW',
    links: [{ href: '/dashboard', label: 'Dashboard' }],
  },
  {
    label: 'GROW',
    links: [
      { href: '/dashboard/leads', label: 'CRM Leads' },
      { href: '/dashboard/create-lead', label: 'Create Lead' },
      { href: '/dashboard/sales', label: 'Sales' },
      { href: '/dashboard/create-sale', label: 'Create Sale' },
    ],
  },
  {
    label: 'OPERATE',
    links: [
      { href: '/dashboard/schedule', label: 'Schedule' },
      { href: '/dashboard/bookings', label: 'Bookings' },
      { href: '/dashboard/customers', label: 'Customers' },
    ],
  },
  {
    label: 'TRAINER',
    links: [
      { href: '/dashboard/trainer', label: 'Trainer Day' },
      { href: '/dashboard/workout-builder', label: 'Workout Builder' },
      { href: '/dashboard/reports', label: 'Reports & Stats' },
    ],
  },
  {
    label: 'MEMBER',
    links: [
      { href: '/dashboard/member', label: 'Member Home' },
      { href: '/dashboard/member/workout-log', label: 'Workout Log' },
      { href: '/dashboard/member/progress', label: 'Progress & PRs' },
      { href: '/dashboard/member/ai-coach', label: 'AI Coach' },
      { href: '/dashboard/member/book', label: 'Book' },
      { href: '/dashboard/member/payments', label: 'Payments' },
      { href: '/dashboard/member/profile', label: 'Profile' },
    ],
  },
];

export default function Sidebar() {
  const pathname = usePathname();

  return (
    <aside className="fixed left-0 top-0 h-screen w-[232px] bg-[#171A16] flex flex-col z-40 border-r border-white/10">
      {/* Brand */}
      <div className="pt-5 px-4 pb-4 border-b border-white/10">
        <div className="flex items-center gap-2.5 mb-1">
          <div className="w-7 h-7 bg-[#D7FF3F] rounded-lg flex items-center justify-center">
            <span className="font-black text-xs text-[#111310]">F</span>
          </div>
          <span className="font-black text-[#ECEDE6] text-base tracking-tight">FITOS</span>
        </div>
        <p className="text-xs text-[#8A8E85] pl-9">City Centre Studio</p>
      </div>
      {/* Nav */}
      <nav className="flex-1 overflow-y-auto px-3 py-4 space-y-5">
        {navGroups.map((group) => (
          <div key={group.label}>
            <p className="text-[10px] font-semibold uppercase tracking-[0.12em] text-[#5E625A] px-3 mb-1.5">{group.label}</p>
            <div className="space-y-0.5">
              {group.links.map(({ href, label }) => {
                const isActive = pathname === href || (href !== '/dashboard' && pathname.startsWith(href));
                return (
                  <Link
                    key={href}
                    href={href}
                    className={`flex items-center gap-3 rounded-lg px-3 py-2 text-sm transition-all ${
                      isActive
                        ? 'bg-[#D7FF3F] text-[#111310] font-semibold'
                        : 'text-[#8A8E85] hover:text-white hover:bg-white/5'
                    }`}
                  >
                    {label}
                  </Link>
                );
              })}
            </div>
          </div>
        ))}
      </nav>
      {/* Footer */}
      <div className="px-4 py-4 border-t border-white/10">
        <div className="flex items-center gap-3 mb-3">
          <div className="w-8 h-8 rounded-full bg-[#D7FF3F] flex items-center justify-center">
            <span className="text-xs font-bold text-[#111310]">JD</span>
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-xs font-semibold text-[#ECEDE6] truncate">Jane Doe</p>
            <p className="text-[10px] text-[#8A8E85] truncate">Studio Manager</p>
          </div>
        </div>
        <button className="w-full text-left text-xs text-[#8A8E85] hover:text-white transition-colors px-1">
          Sign out
        </button>
      </div>
    </aside>
  );
}
