export type PortalRole = 'ADMIN' | 'MANAGER' | 'RECEPTIONIST' | 'TRAINER' | 'MEMBER';

const allowedPaths: Record<PortalRole, readonly string[]> = {
  ADMIN: ['/dashboard/users'],
  MANAGER: ['/dashboard', '/dashboard/plans', '/dashboard/retail', '/dashboard/staff', '/dashboard/reports', '/dashboard/invoices'],
  RECEPTIONIST: [
    '/dashboard/leads', '/dashboard/create-lead', '/dashboard/sales', '/dashboard/create-sale',
    '/dashboard/pos', '/dashboard/retail-orders', '/dashboard/invoices', '/dashboard/schedule', '/dashboard/bookings',
    '/dashboard/customers', '/dashboard/payment-confirmed',
  ],
  TRAINER: ['/dashboard/trainer', '/dashboard/workout-builder', '/dashboard/schedule', '/dashboard/bookings', '/dashboard/customers'],
  MEMBER: ['/dashboard/member'],
};

export function canAccessDashboardPath(role: string | undefined, pathname: string): boolean {
  const normalizedRole = role?.toUpperCase() as PortalRole | undefined;
  const paths = normalizedRole && allowedPaths[normalizedRole];
  if (!paths) return false;
  return paths.some((path) => pathname === path || (path !== '/dashboard' && pathname.startsWith(`${path}/`)));
}

export function portalHome(role: string | undefined): string {
  switch (role?.toUpperCase()) {
    case 'ADMIN': return '/dashboard/users';
    case 'RECEPTIONIST': return '/dashboard/leads';
    case 'TRAINER': return '/dashboard/trainer';
    case 'MEMBER': return '/dashboard/member';
    default: return '/dashboard';
  }
}
