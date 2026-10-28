'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';

export default function PaymentConfirmedPage() {
  const router = useRouter();
  const searchParams = useSearchParams();

  const orderRef = searchParams?.get('orderRef') || 'ORD-20251025-9821';
  const customerName = searchParams?.get('customerName') || 'Jane Doe';
  const customerEmail = searchParams?.get('customerEmail') || 'jane.doe@email.com';
  const planName = searchParams?.get('planName') || 'Perform — All Access';
  const amount = Number(searchParams?.get('amount') || 490000);
  const method = searchParams?.get('method') || 'CARD';

  const [countdown, setCountdown] = useState(10);
  const [autoRedirect, setAutoRedirect] = useState(true);

  useEffect(() => {
    if (!autoRedirect) return;
    if (countdown <= 0) {
      router.push('/dashboard/sales');
      return;
    }
    const timer = setInterval(() => {
      setCountdown((prev) => prev - 1);
    }, 1000);
    return () => clearInterval(timer);
  }, [countdown, autoRedirect, router]);

  return (
    <div className="min-h-screen bg-[#F5F5F0] flex items-center justify-center p-4">
      <div className="bg-white border border-[#DADCD3] rounded-2xl p-8 md:p-10 w-full max-w-lg text-center shadow-lg">
        {/* Animated Checkmark */}
        <div className="w-20 h-20 bg-[#D7FF3F] rounded-full flex items-center justify-center mx-auto mb-5 shadow-sm">
          <svg width="34" height="34" viewBox="0 0 32 32" fill="none">
            <path
              d="M6 16L13 23L26 9"
              stroke="#111310"
              strokeWidth="3.5"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        </div>

        <h1 className="text-2xl font-black text-[#111310] mb-1">Payment Confirmed</h1>
        <p className="text-sm text-[#5E625A] mb-6">
          Membership activated successfully · Order Reference: <span className="font-mono font-bold text-[#111310]">{orderRef}</span>
        </p>

        {/* Receipt Box */}
        <div className="bg-[#F5F5F0] border border-[#DADCD3] rounded-xl p-5 mb-6 text-left">
          <div className="flex items-center gap-3 mb-4 pb-3 border-b border-[#DADCD3]">
            <div className="w-10 h-10 rounded-full bg-[#171A16] text-[#D7FF3F] flex items-center justify-center text-sm font-bold">
              {customerName.charAt(0)}
            </div>
            <div>
              <p className="text-sm font-bold text-[#111310]">{customerName}</p>
              <p className="text-xs text-[#5E625A]">{customerEmail}</p>
            </div>
          </div>

          <div className="space-y-2 text-xs">
            <div className="flex justify-between">
              <span className="text-[#8A8E85]">Purchased Plan</span>
              <span className="font-bold text-[#111310]">{planName}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-[#8A8E85]">Paid Amount</span>
              <span className="font-bold text-[#111310]">₫{amount.toLocaleString()}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-[#8A8E85]">Payment Method</span>
              <span className="font-semibold text-[#111310]">{method}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-[#8A8E85]">Status</span>
              <span className="font-bold text-[#526B00] bg-[#D7FF3F] px-2 py-0.5 rounded-full">
                ACTIVE
              </span>
            </div>
          </div>
        </div>

        {/* Countdown message */}
        <div className="mb-6 flex items-center justify-center gap-2 text-xs text-[#5E625A]">
          {autoRedirect ? (
            <>
              <span>Redirecting to Sales in <strong>{countdown}s</strong>...</span>
              <button
                onClick={() => setAutoRedirect(false)}
                className="text-[#526B00] underline font-medium"
              >
                Pause
              </button>
            </>
          ) : (
            <span>Auto-redirect paused</span>
          )}
        </div>

        {/* Action Buttons */}
        <div className="flex flex-col sm:flex-row gap-3">
          <Link
            href="/dashboard/sales"
            className="flex-1 bg-[#171A16] text-[#ECEDE6] font-semibold rounded-lg px-4 py-2.5 text-sm text-center hover:opacity-90"
          >
            View Sales Table
          </Link>
          <Link
            href="/dashboard/create-sale"
            className="flex-1 bg-[#D7FF3F] text-[#111310] font-bold rounded-lg px-4 py-2.5 text-sm text-center hover:opacity-90"
          >
            + Create Another Sale
          </Link>
        </div>
      </div>
    </div>
  );
}
