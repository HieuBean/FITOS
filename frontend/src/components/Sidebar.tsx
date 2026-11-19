'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useEffect, useState } from 'react';
import { useAuth } from '@/context/AuthContext';
import { canAccessDashboardPath } from '@/lib/portalAccess';

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
    roles: ['RECEPTIONIST'],
    links: [
      { href: '/dashboard/leads', label: 'Khách tiềm năng (CRM)' },
      { href: '/dashboard/create-lead', label: 'Thêm khách tiềm năng' },
      { href: '/dashboard/sales', label: 'Đơn hàng & Doanh số' },
      { href: '/dashboard/create-sale', label: 'Tạo đơn mới' },
    ],
  },
  {
    label: 'BÁN HÀNG TẠI QUẦY',
    roles: ['RECEPTIONIST'],
    links: [
      { href: '/dashboard/pos', label: 'Bán tại quầy (POS)' },
      { href: '/dashboard/retail-orders', label: 'Đơn bán lẻ' },
    ],
  },
  {
    label: 'HÓA ĐƠN',
    roles: ['MANAGER', 'RECEPTIONIST'],
    links: [{ href: '/dashboard/invoices', label: 'Danh sách & chi tiết hóa đơn' }],
  },
  {
    label: 'DANH MỤC & KHO',
    roles: ['MANAGER'],
    links: [
      { href: '/dashboard/plans', label: 'Gói dịch vụ' },
      { href: '/dashboard/retail', label: 'Sản phẩm & Tồn kho' },
    ],
  },
  {
    label: 'VẬN HÀNH STUDIO',
    roles: ['RECEPTIONIST', 'TRAINER'],
    links: [
      { href: '/dashboard/schedule', label: 'Lịch biểu phòng tập' },
      { href: '/dashboard/bookings', label: 'Danh sách đặt chỗ' },
      { href: '/dashboard/customers', label: 'Quản lý hội viên' },
    ],
  },
  {
    label: 'HUẤN LUYỆN VIÊN',
    roles: ['TRAINER'],
    links: [
      { href: '/dashboard/trainer', label: 'Lịch HLV trong ngày' },
      { href: '/dashboard/workout-builder', label: 'Thiết kế giáo án' },
    ],
  },
  {
    label: 'BÁO CÁO',
    roles: ['MANAGER'],
    links: [{ href: '/dashboard/reports', label: 'Báo cáo & Thống kê' }],
  },
  {
    label: 'QUẢN TRỊ',
    roles: ['ADMIN'],
    links: [{ href: '/dashboard/users', label: 'Tài khoản & Phân quyền' }],
  },
  {
    label: 'NHÂN SỰ',
    roles: ['MANAGER'],
    links: [{ href: '/dashboard/staff', label: 'Lễ tân & Huấn luyện viên' }],
  },
  {
    label: 'HỘI VIÊN',
    roles: ['MEMBER'],
    links: [
      { href: '/dashboard/member', label: 'Trang cá nhân' },
      { href: '/dashboard/member/book', label: 'Đặt lịch tập' },
      { href: '/dashboard/member/workout-log', label: 'Ghi nhật ký tập' },
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
  const [mobileOpen, setMobileOpen] = useState(false);

  // Đóng drawer mỗi khi đổi trang
  useEffect(() => {
    setMobileOpen(false);
  }, [pathname]);

  // Khoá cuộn nền khi drawer đang mở
  useEffect(() => {
    if (!mobileOpen) return;
    const previous = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = previous;
    };
  }, [mobileOpen]);

  const currentRole = (user?.role || 'MEMBER').toUpperCase();

  const visibleGroups = allNavGroups
    .filter((group) => group.roles.includes(currentRole))
    .map((group) => ({
      ...group,
      links: group.links.filter((link) => canAccessDashboardPath(currentRole, link.href)),
    }))
    .filter((group) => group.links.length > 0);

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
    if (currentRole === 'ADMIN') return 'Quản trị tài khoản';
    if (currentRole === 'MEMBER') return 'Cổng Hội Viên · Chi nhánh TT';
    if (currentRole === 'TRAINER') return 'Không gian HLV · Chi nhánh TT';
    return 'Quản lý Studio · Chi nhánh TT';
  };

  // Mục đang mở: khớp chính xác, hoặc là tiền tố của một segment con.
  const isActive = (href: string) => {
    if (pathname === href) return true;
    if (href === '/dashboard') return false;
    return pathname.startsWith(href + '/');
  };

  return (
    <>
      {/* Nút mở menu trên mobile/tablet */}
      <button
        type="button"
        onClick={() => setMobileOpen(true)}
        aria-label="Mở menu điều hướng"
        aria-expanded={mobileOpen}
        aria-controls="fitos-sidebar"
        className="lg:hidden fixed top-3 left-3 z-50 h-11 w-11 rounded-xl bg-[#171A16] text-[#ECEDE6] shadow-lg flex items-center justify-center"
      >
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
          <path d="M4 6h16M4 12h16M4 18h16" strokeLinecap="round" />
        </svg>
      </button>

      {/* Lớp phủ khi drawer mở */}
      {mobileOpen && (
        <div
          className="lg:hidden fixed inset-0 z-40 bg-black/50"
          onClick={() => setMobileOpen(false)}
          aria-hidden="true"
        />
      )}

      <aside
        id="fitos-sidebar"
        className={`fixed left-0 top-0 h-screen w-[232px] bg-[#171A16] flex flex-col z-50 border-r border-white/10 transition-transform duration-200 lg:translate-x-0 lg:z-40 ${
          mobileOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        {/* Brand */}
        <div className="pt-5 px-4 pb-4 border-b border-white/10 flex items-start justify-between gap-2">
          <div className="min-w-0">
            <Link href="/" className="flex items-center gap-2.5 mb-1 hover:opacity-90 transition-opacity">
              <div className="w-7 h-7 bg-[#D7FF3F] rounded-lg flex items-center justify-center">
                <span className="font-black text-xs text-[#111310]">F</span>
              </div>
              <span className="font-black text-[#ECEDE6] text-base tracking-tight">FITOS</span>
            </Link>
            <p className="text-xs text-[#A8ADA2] pl-9 truncate">{getStudioSubtitle()}</p>
          </div>
          <button
            type="button"
            onClick={() => setMobileOpen(false)}
            aria-label="Đóng menu điều hướng"
            className="lg:hidden h-11 w-11 shrink-0 rounded-lg text-[#A8ADA2] hover:text-white hover:bg-white/10 flex items-center justify-center"
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true">
              <path d="M6 6l12 12M18 6L6 18" strokeLinecap="round" />
            </svg>
          </button>
        </div>

        {/* Nav Groups theo vai trò */}
        <nav aria-label="Điều hướng chính" className="flex-1 overflow-y-auto px-3 py-4 space-y-5">
          {visibleGroups.map((group) => (
            <div key={group.label}>
              <p className="text-[10px] font-semibold uppercase tracking-[0.12em] text-[#A8ADA2] px-3 mb-1.5">
                {group.label}
              </p>
              <div className="space-y-0.5">
                {group.links.map(({ href, label }) => {
                  const active = isActive(href);
                  return (
                    <Link
                      key={href}
                      href={href}
                      aria-current={active ? 'page' : undefined}
                      className={`flex items-center gap-3 rounded-lg px-3 py-2 min-h-[44px] text-sm transition-all ${
                        active
                          ? 'bg-[#D7FF3F] text-[#111310] font-semibold'
                          : 'text-[#A8ADA2] hover:text-white hover:bg-white/5'
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
              <p className="text-[10px] text-[#A8ADA2] truncate">{getRoleLabel(user?.role)}</p>
            </div>
          </div>
          <button
            type="button"
            onClick={() => logout()}
            className="w-full text-left text-xs text-[#A8ADA2] hover:text-red-400 transition-colors px-1 py-2 min-h-[44px] flex items-center justify-between cursor-pointer"
          >
            <span>Đăng xuất</span>
            <span className="text-[10px]" aria-hidden="true">→</span>
          </button>
        </div>
      </aside>
    </>
  );
}
