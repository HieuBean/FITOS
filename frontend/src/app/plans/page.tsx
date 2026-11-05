'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import Header from '@/components/Header';
import Footer from '@/components/Footer';
import apiClient from '@/lib/api';

interface Plan {
  id: number;
  name: string;
  planType: string;
  price: number;
  durationDays: number;
  description?: string;
  features?: string[];
  highlight?: boolean;
}

const defaultPlans: Plan[] = [
  {
    id: 1,
    name: 'Move',
    planType: 'MEMBERSHIP',
    price: 290000,
    durationDays: 30,
    description: 'Perfect for getting started with gym floor fitness',
    features: ['Up to 8 classes/month', 'Mobile app access', 'Workout tracking', 'Locker & towel service'],
    highlight: false,
  },
  {
    id: 2,
    name: 'Perform',
    planType: 'MEMBERSHIP',
    price: 490000,
    durationDays: 30,
    description: 'Our most popular plan for passionate athletes',
    features: ['Unlimited group classes', 'AI Coach workout analysis', 'Priority class booking', 'Monthly body composition scan', 'Guest passes (2/month)'],
    highlight: true,
  },
  {
    id: 3,
    name: 'PT Focus',
    planType: 'PT_PACKAGE',
    price: 1200000,
    durationDays: 45,
    description: 'Maximum results with personal 1-on-1 coaching',
    features: ['Unlimited classes included', '4 PT sessions/month', 'Custom periodized program', 'Dedicated Master Trainer', 'Nutrition habit coaching'],
    highlight: false,
  },
];

const ptPackages = [
  { sessions: '4 sessions', price: '₫1,200,000', per: 'per pack', note: '₫300,000/session' },
  { sessions: '8 sessions', price: '₫2,000,000', per: 'per pack', note: '₫250,000/session — best value' },
  { sessions: '12 sessions', price: '₫2,700,000', per: 'per pack', note: '₫225,000/session' },
];

const faqs = [
  { q: 'Is there a joining fee?', a: 'No — never. Just your chosen membership plan.' },
  { q: 'Can I switch or upgrade plans?', a: 'Yes, upgrade or switch at any time directly in your member portal.' },
  { q: 'Can I pause my membership?', a: 'Yes! Memberships can be paused anytime with remaining days saved.' },
  { q: 'Do PT sessions roll over?', a: 'Unused PT sessions roll over for up to one month.' },
  { q: 'What payment methods do you accept?', a: 'Credit/Debit cards, VNPay, Momo, and bank transfer.' },
  { q: 'Can I book classes on the app?', a: 'Yes, full booking and cancelation is built right into your dashboard.' },
];

export default function PlansPage() {
  const [plans, setPlans] = useState<Plan[]>(defaultPlans);

  useEffect(() => {
    async function loadPlans() {
      try {
        const res = await apiClient.get('/plans');
        if (Array.isArray(res.data) && res.data.length > 0) {
          const mapped = res.data.map((p: any, idx: number) => ({
            ...p,
            highlight: idx === 1 || p.name.toLowerCase().includes('perform'),
          }));
          setPlans(mapped);
        }
      } catch (e) {
        // use defaultPlans
      }
    }
    loadPlans();
  }, []);

  return (
    <>
      <Header />
      <main className="pt-16">
        {/* Hero Section */}
        <section className="bg-[#F5F5F0] py-20 border-b border-[#DADCD3]">
          <div className="max-w-[1280px] mx-auto px-8 md:px-10">
            <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[#526B00] mb-3">
              MEMBERSHIP PLANS & PRICING
            </p>
            <h1 className="text-4xl md:text-[52px] font-black text-[#111310] tracking-tight mb-4">
              Choose your plan
            </h1>
            <p className="text-base md:text-lg text-[#5E625A] max-w-lg mb-8">
              Flexible memberships designed for every athlete. No hidden fees, cancel or pause anytime.
            </p>
            <div className="inline-flex items-center gap-2 bg-[#D7FF3F] rounded-full px-4 py-1.5 shadow-sm">
              <span className="text-xs font-bold text-[#111310]">
                ⚡ SPECIAL OFFER — ZERO JOINING FEES
              </span>
            </div>
          </div>
        </section>

        {/* Pricing Cards */}
        <section className="bg-white py-20">
          <div className="max-w-[1280px] mx-auto px-8 md:px-10">
            <div className="grid grid-cols-1 md:grid-cols-3 gap-8 max-w-5xl mx-auto">
              {plans.map((p) => (
                <div
                  key={p.id}
                  className={`rounded-2xl p-8 border transition-all duration-200 flex flex-col justify-between ${
                    p.highlight
                      ? 'bg-[#171A16] border-[#D7FF3F] shadow-xl md:-translate-y-2'
                      : 'bg-white border-[#DADCD3] shadow-sm hover:border-[#171A16]'
                  }`}
                >
                  <div>
                    {p.highlight && (
                      <div className="inline-block bg-[#D7FF3F] text-[#111310] text-[10px] font-extrabold uppercase tracking-wider px-3 py-1 rounded-full mb-4">
                        Most Popular
                      </div>
                    )}
                    <h2
                      className={`text-xl font-black mb-1 ${
                        p.highlight ? 'text-white' : 'text-[#111310]'
                      }`}
                    >
                      {p.name}
                    </h2>
                    <p
                      className={`text-xs mb-6 ${
                        p.highlight ? 'text-[#8A8E85]' : 'text-[#5E625A]'
                      }`}
                    >
                      {p.description}
                    </p>

                    <div className="flex items-baseline gap-1 mb-8">
                      <span
                        className={`text-3xl md:text-4xl font-extrabold ${
                          p.highlight ? 'text-[#D7FF3F]' : 'text-[#111310]'
                        }`}
                      >
                        ₫{Number(p.price).toLocaleString()}
                      </span>
                      <span
                        className={`text-xs ${
                          p.highlight ? 'text-[#8A8E85]' : 'text-[#5E625A]'
                        }`}
                      >
                        /{p.durationDays} days
                      </span>
                    </div>

                    <ul className="space-y-3 mb-8">
                      {(p.features || ['Full studio access', 'Free locker', 'Mobile app']).map(
                        (f, idx) => (
                          <li
                            key={idx}
                            className={`flex items-start gap-2.5 text-xs font-medium ${
                              p.highlight ? 'text-[#ECEDE6]' : 'text-[#5E625A]'
                            }`}
                          >
                            <span className={p.highlight ? 'text-[#D7FF3F]' : 'text-[#526B00]'}>
                              ✓
                            </span>
                            <span>{f}</span>
                          </li>
                        )
                      )}
                    </ul>
                  </div>

                  <Link
                    href={`/register?planId=${p.id}`}
                    className={`block w-full text-center font-bold rounded-lg px-5 py-3 text-sm transition-colors ${
                      p.highlight
                        ? 'bg-[#D7FF3F] text-[#111310] hover:bg-[#C4E82E]'
                        : 'bg-[#171A16] text-[#ECEDE6] hover:bg-black/90'
                    }`}
                  >
                    Select Plan →
                  </Link>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* PT Packages Section */}
        <section className="bg-[#F5F5F0] py-16 border-t border-[#DADCD3]">
          <div className="max-w-[1280px] mx-auto px-8 md:px-10">
            <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[#526B00] mb-2">
              1-ON-1 COACHING
            </p>
            <h2 className="text-3xl font-black text-[#111310] mb-8">Personal Training Packages</h2>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              {ptPackages.map((pt) => (
                <div
                  key={pt.sessions}
                  className="bg-white border border-[#DADCD3] rounded-xl p-6 shadow-sm"
                >
                  <h3 className="text-base font-bold text-[#111310] mb-1">{pt.sessions}</h3>
                  <p className="text-xs text-[#8A8E85] mb-4">{pt.note}</p>
                  <div className="flex items-baseline gap-1">
                    <span className="text-2xl font-black text-[#111310]">{pt.price}</span>
                    <span className="text-xs text-[#5E625A]">{pt.per}</span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </section>

        {/* FAQ Section */}
        <section className="bg-white py-16 border-t border-[#DADCD3]">
          <div className="max-w-[1280px] mx-auto px-8 md:px-10">
            <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[#526B00] mb-2">
              FREQUENTLY ASKED QUESTIONS
            </p>
            <h2 className="text-3xl font-black text-[#111310] mb-10">Questions about plans</h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6 max-w-4xl">
              {faqs.map((f, i) => (
                <div key={i} className="bg-[#F5F5F0] rounded-xl p-6">
                  <h3 className="text-sm font-bold text-[#111310] mb-2">{f.q}</h3>
                  <p className="text-xs text-[#5E625A] leading-relaxed">{f.a}</p>
                </div>
              ))}
            </div>
          </div>
        </section>
      </main>
      <Footer />
    </>
  );
}
