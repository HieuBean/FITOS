'use client';

import React from 'react';
import { useAuth } from '@/context/AuthContext';
import Link from 'next/link';

interface RoleGuardProps {
  allowedRoles: string[];
  children: React.ReactNode;
  fallback?: React.ReactNode;
}

export default function RoleGuard({ allowedRoles, children, fallback }: RoleGuardProps) {
  const { user, isLoading } = useAuth();

  if (isLoading) {
    return (
      <div className="p-8 flex items-center justify-center">
        <div className="w-8 h-8 border-3 border-[#171A16] border-t-[#D7FF3F] rounded-full animate-spin"></div>
      </div>
    );
  }

  const userRole = (user?.role || '').toUpperCase();
  const normalizedAllowed = allowedRoles.map((r) => r.toUpperCase());

  // ADMIN can access anything
  const hasAccess = userRole === 'ADMIN' || normalizedAllowed.includes(userRole);

  if (!hasAccess) {
    if (fallback) return <>{fallback}</>;

    return (
      <div className="min-h-[60vh] flex items-center justify-center p-6">
        <div className="bg-white border border-[#DADCD3] rounded-2xl p-8 max-w-md text-center shadow-sm">
          <div className="w-12 h-12 bg-red-100 text-red-600 rounded-full flex items-center justify-center mx-auto mb-4 text-xl font-bold">
            !
          </div>
          <h2 className="text-xl font-bold text-[#111310] mb-2">Quyền truy cập bị giới hạn</h2>
          <p className="text-sm text-[#5E625A] mb-6">
            Mục này yêu cầu một trong các vai trò: <span className="font-semibold text-[#111310]">{allowedRoles.join(', ')}</span>.
            Vai trò hiện tại của bạn là <span className="font-semibold text-[#526B00]">{user?.role || 'Khách'}</span>.
          </p>
          <div className="flex justify-center gap-3">
            <Link
              href={user?.role === 'MEMBER' ? '/dashboard/member' : '/dashboard'}
              className="bg-[#D7FF3F] text-[#111310] font-semibold px-5 py-2.5 rounded-lg text-sm hover:opacity-90 transition-opacity"
            >
              Về không gian làm việc của bạn
            </Link>
          </div>
        </div>
      </div>
    );
  }

  return <>{children}</>;
}
