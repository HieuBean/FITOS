'use client';

import React, { useEffect } from 'react';
import { useAuth } from '@/context/AuthContext';
import { useRouter, usePathname } from 'next/navigation';
import Link from 'next/link';
import { canAccessDashboardPath, portalHome } from '@/lib/portalAccess';

export default function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { user, isLoading } = useAuth();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (!isLoading && !user) {
      router.push(`/login?redirect=${encodeURIComponent(pathname)}`);
    } else if (!isLoading && user?.role === 'ADMIN' && pathname !== '/dashboard/users') {
      router.replace('/dashboard/users');
    }
  }, [user, isLoading, router, pathname]);

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-[#F5F5F0]">
        <div className="flex flex-col items-center gap-3">
          <div className="w-10 h-10 border-4 border-[#171A16] border-t-[#D7FF3F] rounded-full animate-spin"></div>
          <p className="text-sm font-medium text-[#5E625A]">Đang xác thực phiên làm việc...</p>
        </div>
      </div>
    );
  }

  if (!user) {
    return null;
  }

  if (user.role === 'ADMIN' && pathname !== '/dashboard/users') {
    return null;
  }

  if (!canAccessDashboardPath(user.role, pathname)) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[#F5F5F0] p-6">
        <div className="max-w-md rounded-2xl border border-[#DADCD3] bg-white p-8 text-center">
          <h1 className="text-xl font-bold text-[#111310]">Quyền truy cập bị giới hạn</h1>
          <p className="mt-2 text-sm text-[#5E625A]">Chức năng này không thuộc cổng làm việc của bạn.</p>
          <Link href={portalHome(user.role)} className="mt-5 inline-block rounded-lg bg-[#D7FF3F] px-5 py-3 font-semibold text-[#111310]">
            Về trang làm việc
          </Link>
        </div>
      </div>
    );
  }

  return <>{children}</>;
}
