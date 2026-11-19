'use client';

import { useState } from 'react';
import { useAuth } from '@/context/AuthContext';

export default function DemoResetButton() {
  const { user } = useAuth();
  const [resetting, setResetting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (process.env.NEXT_PUBLIC_FITOS_DEMO_MODE !== '1' || user?.role !== 'ADMIN') {
    return null;
  }

  async function resetDemo() {
    if (!window.confirm('Khôi phục dữ liệu mẫu ban đầu? Mọi thay đổi kiểm thử trong database FITOS sẽ bị xóa.')) {
      return;
    }
    setResetting(true);
    setError(null);
    try {
      const token = sessionStorage.getItem('fitos_access_token');
      const response = await fetch('/api/demo/reset', {
        method: 'POST',
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      });
      const result = await response.json();
      if (!response.ok) {
        throw new Error(result.message || 'Không thể khôi phục dữ liệu mẫu.');
      }
      window.location.reload();
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Không thể khôi phục dữ liệu mẫu.');
      setResetting(false);
    }
  }

  return (
    <div className="flex flex-col items-end gap-1">
      <button
        type="button"
        onClick={resetDemo}
        disabled={resetting}
        aria-busy={resetting}
        className="min-h-11 rounded-lg border border-[#DADCD3] bg-white px-4 text-xs font-bold text-[#111310] hover:bg-[#F5F5F0] focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#526B00] disabled:cursor-wait disabled:opacity-60"
      >
        {resetting ? 'Đang khôi phục…' : 'Khôi phục dữ liệu mẫu'}
      </button>
      {error && <p role="alert" className="max-w-64 text-right text-xs text-red-700">{error}</p>}
    </div>
  );
}
