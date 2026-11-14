import Sidebar from '@/components/Sidebar';
import ProtectedRoute from '@/components/ProtectedRoute';

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  return (
    <ProtectedRoute>
      <div className="flex min-h-screen bg-[#F5F5F0]">
        <Sidebar />
        {/*
          Sidebar là cố định 232px trên desktop. Trước đây <main> luôn giữ ml-[232px],
          nên trên điện thoại (375px) nội dung chỉ còn ~143px và tràn ngang toàn trang.
          Dưới lg: sidebar chuyển thành drawer, main chiếm toàn bộ chiều rộng.
        */}
        <main className="flex-1 min-h-screen w-full min-w-0 pt-16 lg:pt-0 lg:ml-[232px]">
          {children}
        </main>
      </div>
    </ProtectedRoute>
  );
}
