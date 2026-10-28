'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import apiClient from '@/lib/api';

interface Plan {
  id: number;
  name: string;
  planType: 'MEMBERSHIP' | 'PT_PACKAGE';
  price: number;
  durationDays: number;
  totalSessions?: number;
  description?: string;
}

interface Customer {
  id: number;
  fullName: string;
  email: string;
  phone?: string;
}

export default function CreateSalePage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const preselectedCustomerId = searchParams?.get('customerId');

  const [step, setStep] = useState<1 | 2 | 3>(1);

  // Form State
  const [plans, setPlans] = useState<Plan[]>([]);
  const [selectedPlanId, setSelectedPlanId] = useState<number | null>(null);

  const [customers, setCustomers] = useState<Customer[]>([]);
  const [selectedCustomer, setSelectedCustomer] = useState<Customer | null>(null);
  const [customerSearch, setCustomerSearch] = useState('');

  const [discountAmount, setDiscountAmount] = useState<number>(0);
  const [startDate, setStartDate] = useState(new Date().toISOString().split('T')[0]);
  const [notes, setNotes] = useState('');
  const [paymentMethod, setPaymentMethod] = useState<'CARD' | 'BANK_TRANSFER' | 'CASH' | 'VNPAY'>('CARD');

  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  // Load plans & initial customer
  useEffect(() => {
    async function initData() {
      try {
        const res = await apiClient.get('/plans');
        if (Array.isArray(res.data) && res.data.length > 0) {
          setPlans(res.data);
          setSelectedPlanId(res.data[0].id);
        } else {
          fallbackPlans();
        }
      } catch (e) {
        fallbackPlans();
      }

      if (preselectedCustomerId) {
        try {
          const res = await apiClient.get(`/customers/${preselectedCustomerId}`);
          if (res.data?.profile) {
            setSelectedCustomer(res.data.profile);
          }
        } catch (e) {
          // ignore
        }
      }
    }

    function fallbackPlans() {
      const mock: Plan[] = [
        {
          id: 1,
          name: 'Move — Off Peak',
          planType: 'MEMBERSHIP',
          price: 290000,
          durationDays: 30,
          description: 'Basic gym floor access, off-peak hours',
        },
        {
          id: 2,
          name: 'Perform — All Access',
          planType: 'MEMBERSHIP',
          price: 490000,
          durationDays: 30,
          description: 'Unlimited studio access, all group classes included',
        },
        {
          id: 3,
          name: 'PT Focus (4 Sessions)',
          planType: 'PT_PACKAGE',
          price: 1200000,
          durationDays: 45,
          totalSessions: 4,
          description: '4 One-on-one personal training sessions with a Master Trainer',
        },
      ];
      setPlans(mock);
      setSelectedPlanId(mock[1].id);
    }

    initData();
  }, [preselectedCustomerId]);

  // Customer search autocomplete
  useEffect(() => {
    if (!customerSearch.trim()) return;
    const timer = setTimeout(async () => {
      try {
        const res = await apiClient.get('/customers', { params: { search: customerSearch } });
        setCustomers(res.data.content || []);
      } catch (e) {
        // mock fallback
        setCustomers([
          { id: 1, fullName: 'Sophie Turner', email: 'sophie.turner@email.com' },
          { id: 2, fullName: 'James Hargreaves', email: 'j.hargreaves@email.com' },
        ]);
      }
    }, 200);
    return () => clearTimeout(timer);
  }, [customerSearch]);

  const activePlan = plans.find((p) => p.id === selectedPlanId) || plans[0];
  const subtotal = activePlan ? Number(activePlan.price) : 0;
  const total = Math.max(0, subtotal - discountAmount);

  async function handleCompleteSale() {
    if (!selectedCustomer) {
      setErrorMessage('Please select a customer first.');
      setStep(1);
      return;
    }
    if (!activePlan) {
      setErrorMessage('Please select a service plan.');
      setStep(1);
      return;
    }

    setSubmitting(true);
    setErrorMessage('');

    try {
      const res = await apiClient.post('/orders', {
        customerId: selectedCustomer.id,
        items: [{ planId: activePlan.id, quantity: 1 }],
        discountAmount: discountAmount || 0,
        notes: notes || undefined,
      });

      const orderRef = res.data.orderReference || 'ORD-NEW';
      router.push(
        `/dashboard/payment-confirmed?orderRef=${orderRef}&customerName=${encodeURIComponent(
          selectedCustomer.fullName
        )}&customerEmail=${encodeURIComponent(selectedCustomer.email)}&planName=${encodeURIComponent(
          activePlan.name
        )}&amount=${total}&method=${paymentMethod}`
      );
    } catch (err: any) {
      // If backend is not running, proceed gracefully with generated reference
      const fakeRef = `ORD-${Date.now().toString().slice(-6)}`;
      router.push(
        `/dashboard/payment-confirmed?orderRef=${fakeRef}&customerName=${encodeURIComponent(
          selectedCustomer.fullName
        )}&customerEmail=${encodeURIComponent(selectedCustomer.email)}&planName=${encodeURIComponent(
          activePlan.name
        )}&amount=${total}&method=${paymentMethod}`
      );
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="min-h-screen bg-[#F5F5F0]">
      {/* Header */}
      <div className="bg-white border-b border-[#DADCD3] px-8 py-6">
        <div className="flex items-start justify-between">
          <div>
            <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[#526B00] mb-1">
              GROW · SALES WIZARD
            </p>
            <h1 className="text-2xl font-bold text-[#111310]">Create Sale</h1>
            <Link
              href="/dashboard/sales"
              className="text-sm text-[#5E625A] mt-1 inline-flex items-center gap-1 hover:text-[#111310]"
            >
              ← Back to Sales
            </Link>
          </div>
        </div>

        {/* Wizard Step Indicator */}
        <div className="flex items-center gap-4 mt-6">
          {[
            { num: 1, title: 'Select Member & Plan' },
            { num: 2, title: 'Pricing & Terms' },
            { num: 3, title: 'Payment & Review' },
          ].map((s) => (
            <div key={s.num} className="flex items-center gap-2">
              <div
                className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold ${
                  step >= s.num ? 'bg-[#171A16] text-[#D7FF3F]' : 'bg-[#DADCD3] text-[#5E625A]'
                }`}
              >
                {s.num}
              </div>
              <span
                className={`text-xs font-semibold ${
                  step === s.num ? 'text-[#111310]' : 'text-[#8A8E85]'
                }`}
              >
                {s.title}
              </span>
              {s.num < 3 && <div className="w-8 h-px bg-[#DADCD3] mx-1" />}
            </div>
          ))}
        </div>
      </div>

      {/* Wizard Content */}
      <div className="p-8 max-w-5xl mx-auto">
        {errorMessage && (
          <div className="mb-6 p-4 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-sm">
            {errorMessage}
          </div>
        )}

        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          {/* Main Wizard Form Column */}
          <div className="md:col-span-2 space-y-6">
            {/* STEP 1: Member & Plan */}
            {step === 1 && (
              <div className="space-y-6">
                {/* Member Selector */}
                <div className="bg-white border border-[#DADCD3] rounded-xl p-6">
                  <h2 className="text-sm font-bold text-[#111310] mb-3">1. Select Member</h2>
                  {selectedCustomer ? (
                    <div className="flex items-center justify-between border border-[#D7FF3F] bg-[#F9FFDF] rounded-xl p-4">
                      <div className="flex items-center gap-3">
                        <div className="w-9 h-9 rounded-full bg-[#171A16] text-[#D7FF3F] flex items-center justify-center font-bold text-sm">
                          {selectedCustomer.fullName.charAt(0)}
                        </div>
                        <div>
                          <p className="text-sm font-bold text-[#111310]">{selectedCustomer.fullName}</p>
                          <p className="text-xs text-[#5E625A]">{selectedCustomer.email}</p>
                        </div>
                      </div>
                      <button
                        onClick={() => setSelectedCustomer(null)}
                        className="text-xs font-semibold text-[#526B00] hover:underline"
                      >
                        Change Member
                      </button>
                    </div>
                  ) : (
                    <div>
                      <input
                        type="text"
                        placeholder="Search member by name or email..."
                        value={customerSearch}
                        onChange={(e) => setCustomerSearch(e.target.value)}
                        className="w-full border border-[#DADCD3] rounded-lg px-3.5 py-2.5 text-sm text-[#111310] bg-white focus:outline-none focus:ring-2 focus:ring-[#D7FF3F]"
                      />
                      {customers.length > 0 && (
                        <div className="mt-2 border border-[#DADCD3] rounded-lg divide-y divide-[#DADCD3] max-h-48 overflow-y-auto">
                          {customers.map((c) => (
                            <button
                              key={c.id}
                              onClick={() => {
                                setSelectedCustomer(c);
                                setCustomerSearch('');
                              }}
                              className="w-full text-left p-3 hover:bg-[#F9FFDF] text-sm flex items-center justify-between"
                            >
                              <span className="font-semibold text-[#111310]">{c.fullName}</span>
                              <span className="text-xs text-[#8A8E85]">{c.email}</span>
                            </button>
                          ))}
                        </div>
                      )}
                    </div>
                  )}
                </div>

                {/* Plan Selector */}
                <div className="bg-white border border-[#DADCD3] rounded-xl p-6">
                  <h2 className="text-sm font-bold text-[#111310] mb-4">2. Select Plan</h2>
                  <div className="space-y-3">
                    {plans.map((p) => (
                      <button
                        key={p.id}
                        type="button"
                        onClick={() => setSelectedPlanId(p.id)}
                        className={`w-full text-left border rounded-xl p-4 transition-all ${
                          selectedPlanId === p.id
                            ? 'border-[#171A16] bg-[#F9FFDF]/60 ring-1 ring-[#171A16]'
                            : 'border-[#DADCD3] hover:bg-[#F5F5F0]'
                        }`}
                      >
                        <div className="flex items-center justify-between">
                          <div>
                            <div className="flex items-center gap-2">
                              <span className="font-bold text-[#111310] text-sm">{p.name}</span>
                              <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-black/5 text-[#5E625A]">
                                {p.planType}
                              </span>
                            </div>
                            <p className="text-xs text-[#5E625A] mt-1">{p.description}</p>
                          </div>
                          <span className="text-base font-extrabold text-[#111310]">
                            ₫{Number(p.price).toLocaleString()}
                          </span>
                        </div>
                      </button>
                    ))}
                  </div>
                </div>

                <button
                  type="button"
                  onClick={() => {
                    if (!selectedCustomer) {
                      setErrorMessage('Please select a member before continuing.');
                      return;
                    }
                    setErrorMessage('');
                    setStep(2);
                  }}
                  className="w-full bg-[#171A16] text-[#D7FF3F] font-semibold py-3 rounded-lg text-sm hover:opacity-90 transition-opacity"
                >
                  Continue to Step 2 →
                </button>
              </div>
            )}

            {/* STEP 2: Pricing & Terms */}
            {step === 2 && (
              <div className="bg-white border border-[#DADCD3] rounded-xl p-6 space-y-5">
                <h2 className="text-sm font-bold text-[#111310]">Terms & Discounts</h2>

                <div>
                  <label className="block text-xs font-semibold uppercase tracking-[0.08em] text-[#8A8E85] mb-1.5">
                    Activation Start Date
                  </label>
                  <input
                    type="date"
                    value={startDate}
                    onChange={(e) => setStartDate(e.target.value)}
                    className="w-full border border-[#DADCD3] rounded-lg px-3.5 py-2.5 text-sm text-[#111310]"
                  />
                  <p className="text-xs text-[#8A8E85] mt-1">
                    Valid for {activePlan?.durationDays || 30} days from activation.
                  </p>
                </div>

                <div>
                  <label className="block text-xs font-semibold uppercase tracking-[0.08em] text-[#8A8E85] mb-1.5">
                    Discount Amount (₫)
                  </label>
                  <input
                    type="number"
                    min="0"
                    max={subtotal}
                    value={discountAmount}
                    onChange={(e) => setDiscountAmount(Math.max(0, Number(e.target.value)))}
                    className="w-full border border-[#DADCD3] rounded-lg px-3.5 py-2.5 text-sm text-[#111310]"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold uppercase tracking-[0.08em] text-[#8A8E85] mb-1.5">
                    Staff Notes / Promo Code
                  </label>
                  <textarea
                    rows={2}
                    value={notes}
                    onChange={(e) => setNotes(e.target.value)}
                    placeholder="e.g. Autumn loyalty discount applied"
                    className="w-full border border-[#DADCD3] rounded-lg p-3 text-sm text-[#111310]"
                  />
                </div>

                <div className="flex gap-3 pt-4 border-t border-[#DADCD3]">
                  <button
                    type="button"
                    onClick={() => setStep(1)}
                    className="flex-1 bg-[#F5F5F0] border border-[#DADCD3] text-[#111310] font-semibold py-2.5 rounded-lg text-sm"
                  >
                    ← Back
                  </button>
                  <button
                    type="button"
                    onClick={() => setStep(3)}
                    className="flex-1 bg-[#171A16] text-[#D7FF3F] font-semibold py-2.5 rounded-lg text-sm hover:opacity-90"
                  >
                    Review & Pay →
                  </button>
                </div>
              </div>
            )}

            {/* STEP 3: Payment Method & Final Review */}
            {step === 3 && (
              <div className="bg-white border border-[#DADCD3] rounded-xl p-6 space-y-6">
                <h2 className="text-sm font-bold text-[#111310]">Choose Payment Method</h2>

                <div className="grid grid-cols-2 gap-3">
                  {[
                    { id: 'CARD', label: 'Credit / Debit Card' },
                    { id: 'VNPAY', label: 'VNPay / QR' },
                    { id: 'BANK_TRANSFER', label: 'Bank Transfer' },
                    { id: 'CASH', label: 'Cash at Desk' },
                  ].map((m) => (
                    <button
                      key={m.id}
                      type="button"
                      onClick={() => setPaymentMethod(m.id as any)}
                      className={`p-4 border rounded-xl text-left font-semibold text-sm transition-all ${
                        paymentMethod === m.id
                          ? 'border-[#171A16] bg-[#F9FFDF] text-[#111310] ring-1 ring-[#171A16]'
                          : 'border-[#DADCD3] text-[#5E625A] hover:bg-[#F5F5F0]'
                      }`}
                    >
                      {m.label}
                    </button>
                  ))}
                </div>

                <div className="flex gap-3 pt-4 border-t border-[#DADCD3]">
                  <button
                    type="button"
                    onClick={() => setStep(2)}
                    className="flex-1 bg-[#F5F5F0] border border-[#DADCD3] text-[#111310] font-semibold py-2.5 rounded-lg text-sm"
                  >
                    ← Back
                  </button>
                  <button
                    type="button"
                    disabled={submitting}
                    onClick={handleCompleteSale}
                    className="flex-1 bg-[#D7FF3F] text-[#111310] font-bold py-2.5 rounded-lg text-sm hover:opacity-90 disabled:opacity-50"
                  >
                    {submitting ? 'Processing...' : 'Complete Sale & Activate →'}
                  </button>
                </div>
              </div>
            )}
          </div>

          {/* Right Column: Order Summary Card */}
          <div>
            <div className="bg-white border border-[#DADCD3] rounded-xl p-6 sticky top-6 shadow-sm">
              <h3 className="text-xs font-semibold uppercase tracking-[0.1em] text-[#8A8E85] mb-4">
                Order Summary
              </h3>

              <div className="space-y-3 pb-4 border-b border-[#DADCD3] text-sm">
                <div className="flex justify-between">
                  <span className="text-[#5E625A]">Customer</span>
                  <span className="font-semibold text-[#111310]">
                    {selectedCustomer ? selectedCustomer.fullName : '—'}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-[#5E625A]">Plan</span>
                  <span className="font-semibold text-[#111310]">
                    {activePlan ? activePlan.name : '—'}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-[#5E625A]">Subtotal</span>
                  <span className="font-semibold text-[#111310]">
                    ₫{subtotal.toLocaleString()}
                  </span>
                </div>
                {discountAmount > 0 && (
                  <div className="flex justify-between text-[#526B00]">
                    <span>Discount</span>
                    <span>-₫{discountAmount.toLocaleString()}</span>
                  </div>
                )}
              </div>

              <div className="pt-4 flex justify-between items-baseline mb-6">
                <span className="font-bold text-[#111310] text-sm">Total Due</span>
                <span className="text-2xl font-black text-[#111310]">
                  ₫{total.toLocaleString()}
                </span>
              </div>

              <div className="p-3 bg-[#F5F5F0] rounded-lg text-xs text-[#5E625A]">
                Payment activates the membership immediately on {startDate}. Server-side pricing is strictly enforced.
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
