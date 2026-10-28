'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useAuth } from '@/context/AuthContext';

interface NavItem {
  href: string;
  label: string;
}

interface NavGroup {
  label: string;
  roles: string[];
  links: NavItem[];
}

const allNavGroups: NavGroup[] = [
  {
    label: 'TỔNG QUAN',
    roles: ['ADMIN', 'MANAGER'],
    links: [{ href: '/dashboard', label: 'Bảng điều khiển' }],
  },
  {
    label: 'TĂNG TRƯỞNG & CRM',
    roles: ['ADMIN', 'MANAGER', 'RECEPTIONIST'],
    links: [
      { href: '/dashboard/leads', label: 'Khách tiềm năng (CRM)' },
      { href: '/dashboard/create-lead', label: 'Thêm khách tiềm năng' },
      { href: '/dashboard/sales', label: 'Đơn hàng & Doanh số' },
      { href: '/dashboard/create-sale', label: 'Tạo đơn mới' },
    ],
  },
  {
    label: 'VẬN HÀNH STUDIO',
    roles: ['ADMIN', 'MANAGER', 'RECEPTIONIST', 'TRAINER'],
    links: [
      { href: '/dashboard/schedule', label: 'Lịch biểu phòng tập' },
      { href: '/dashboard/bookings', label: 'Danh sách đặt chỗ' },
      { href: '/dashboard/customers', label: 'Quản lý hội viên' },
    ],
  },
  {
    label: 'HUẤN LUYỆN VIÊN',
    roles: ['ADMIN', 'MANAGER', 'TRAINER'],
    links: [
      { href: '/dashboard/trainer', label: 'Lịch HLV trong ngày' },
      { href: '/dashboard/workout-builder', label: 'Thiết kế giáo án' },
    ],
  },
  {
    label: 'HỘI VIÊN',
    roles: ['ADMIN', 'MEMBER'],
    links: [
      { href: '/dashboard/member', label: 'Trang cá nhân' },
      { href: '/dashboard/member/book', label: 'Đặt lịch tập' },
      { href: '/dashboard/member/progress', label: 'Tiến trình tập luyện' },
      { href: '/dashboard/member/ai-coach', label: 'Trợ lý HLV AI' },
      { href: '/dashboard/member/payments', label: 'Lịch sử thanh toán' },
      { href: '/dashboard/member/profile', label: 'Hồ sơ tài khoản' },
    ],
  },
];

export default function Sidebar() {
  const pathname = usePathname();
  const { user, logout } = useAuth();

  const currentRole = (user?.role || 'MEMBER').toUpperCase();

  // Lọc danh mục điều hướng theo đúng vai trò người dùng (RBAC)
  const visibleGroups = allNavGroups.filter((group) => {
    if (currentRole === 'ADMIN') return true;
    return group.roles.includes(currentRole);
  });

  const getInitials = (name?: string) => {
    if (!name) return 'U';
    const parts = name.trim().split(' ');
    if (parts.length >= 2) {
      return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    }
    return name.slice(0, 2).toUpperCase();
  };

  const getRoleLabel = (role?: string) => {
    switch (role?.toUpperCase()) {
      case 'ADMIN':
        return 'Quản trị viên';
      case 'MANAGER':
        return 'Quản lý Studio';
      case 'RECEPTIONIST':
        return 'Lễ tân / CRM';
      case 'TRAINER':
        return 'Huấn luyện viên';
      case 'MEMBER':
        return 'Hội viên';
      default:
        return 'Hội viên';
    }
  };

  const getStudioSubtitle = () => {
    if (currentRole === 'MEMBER') return 'Cổng Hội Viên · Chi nhánh TT';
    if (currentRole === 'TRAINER') return 'Không gian HLV · Chi nhánh TT';
    return 'Quản lý Studio · Chi nhánh TT';
  };

  return (
    <aside className="fixed left-0 top-0 h-screen w-[232px] bg-[#171A16] flex flex-col z-40 border-r border-white/10">
      {/* Brand */}
      <div className="pt-5 px-4 pb-4 border-b border-white/10">
        <Link href="/" className="flex items-center gap-2.5 mb-1 hover:opacity-90 transition-opacity">
          <div className="w-7 h-7 bg-[#D7FF3F] rounded-lg flex items-center justify-center">
            <span className="font-black text-xs text-[#111310]">F</span>
          </div>
          <span className="font-black text-[#ECEDE6] text-base tracking-tight">FITOS</span>
        </Link>
        <p className="text-xs text-[#8A8E85] pl-9 truncate">{getStudioSubtitle()}</p>
      </div>

      {/* Nav Groups được phân quyền chặt chẽ */}
      <nav className="flex-1 overflow-y-auto px-3 py-4 space-y-5">
        {visibleGroups.map((group) => (
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

      {/* Footer Profile & Logout */}
      <div className="px-4 py-4 border-t border-white/10">
        <div className="flex items-center gap-3 mb-3">
          <div className="w-8 h-8 rounded-full bg-[#D7FF3F] flex items-center justify-center">
            <span className="text-xs font-bold text-[#111310]">{getInitials(user?.fullName)}</span>
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-xs font-semibold text-[#ECEDE6] truncate">{user?.fullName || 'Người dùng'}</p>
            <p className="text-[10px] text-[#8A8E85] truncate">{getRoleLabel(user?.role)}</p>
          </div>
        </div>
        <button
          onClick={() => logout()}
          className="w-full text-left text-xs text-[#8A8E85] hover:text-red-400 transition-colors px-1 flex items-center justify-between cursor-pointer"
        >
          <span>Đăng xuất</span>
          <span className="text-[10px]">→</span>
        </button>
      </div>
    </aside>
  );
}
