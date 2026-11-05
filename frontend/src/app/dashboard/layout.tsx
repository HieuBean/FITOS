import Sidebar from '@/components/Sidebar';
import ProtectedRoute from '@/components/ProtectedRoute';

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  return (
    <ProtectedRoute>
      <div className="flex min-h-screen bg-[#F5F5F0]">
        <Sidebar />
        <main className="flex-1 ml-[232px] min-h-screen">
          {children}
        </main>
      </div>
    </ProtectedRoute>
  );
}
