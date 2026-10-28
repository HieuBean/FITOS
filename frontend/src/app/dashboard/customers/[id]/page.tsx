'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import apiClient from '@/lib/api';
import NoShowBadge from '@/components/NoShowBadge';

type Tab = 'overview' | 'membership' | 'bookings' | 'workouts' | 'payments' | 'activity';

interface CustomerDetail {
  profile: {
    id: number;
    fullName: string;
    email: string;
    phone: string;
    fitnessGoal: string;
    notes: string;
    createdAt: string;
  };
  currentMembership: {
    planName: string;
    status: string;
    startDate: string;
    endDate: string;
    daysRemaining: number;
  } | null;
  memberships: Array<{
    id: number;
    planName: string;
    status: string;
    startDate: string;
    endDate: string;
    daysRemaining: number;
  }>;
  ptPackages: Array<{
    id: number;
    planName: string;
    totalSessions: number;
    remainingSessions: number;
    status: string;
    expiryDate: string;
  }>;
  orders: Array<{
    id: number;
    orderReference: string;
    totalAmount: number;
    status: string;
    createdAt: string;
  }>;
  bookings: Array<{
    id: number;
    className: string;
    sessionStart: string;
    status: string;
    noShowProbability?: number;
  }>;
  workouts: Array<{
    id: number;
    title: string;
    startedAt: string;
    status: string;
  }>;
  attendance: Array<{
    id: number;
    checkedInAt: string;
    source: string;
  }>;
  stats: {
    totalSpent: number;
    paidOrders: number;
    totalBookings: number;
    visitsLast30Days: number;
    workoutsLast30Days: number;
    lastVisitAt: string | null;
  };
}

export default function CustomerDetailPage() {
  const params = useParams();
  const customerId = params?.id as string;

  const [activeTab, setActiveTab] = useState<Tab>('overview');
  const [data, setData] = useState<CustomerDetail | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function loadCustomer() {
      try {
        const res = await apiClient.get(`/customers/${customerId}`);
        setData(res.data);
      } catch (err) {
        // Fallback mock for demo & offline mode
        setData({
          profile: {
            id: Number(customerId) || 1,
            fullName: 'Sophie Turner',
            email: 'sophie.turner@email.com',
            phone: '+84 901 234 567',
            fitnessGoal: 'Hypertrophy & Functional Endurance',
            notes: 'Prefers morning strength sessions. Mild lower back sensitivity.',
            createdAt: '2025-03-01T08:00:00Z',
          },
          currentMembership: {
            planName: 'Perform — Unlimited',
            status: 'ACTIVE',
            startDate: '2025-03-01',
            endDate: '2025-11-01',
            daysRemaining: 7,
          },
          memberships: [
            {
              id: 101,
              planName: 'Perform — Unlimited',
              status: 'ACTIVE',
              startDate: '2025-03-01',
              endDate: '2025-11-01',
              daysRemaining: 7,
            },
            {
              id: 98,
              planName: 'Move — Starter',
              status: 'EXPIRED',
              startDate: '2024-11-01',
              endDate: '2025-02-28',
              daysRemaining: 0,
            },
          ],
          ptPackages: [
            {
              id: 201,
              planName: 'PT Focus (8 Sessions)',
              totalSessions: 8,
              remainingSessions: 3,
              status: 'ACTIVE',
              expiryDate: '2025-12-15',
            },
          ],
          orders: [
            {
              id: 301,
              orderReference: 'ORD-20250715-9X2',
              totalAmount: 490000,
              status: 'PAID',
              createdAt: '2025-07-15T10:30:00Z',
            },
            {
              id: 289,
              orderReference: 'ORD-20250615-4K1',
              totalAmount: 490000,
              status: 'PAID',
              createdAt: '2025-06-15T09:15:00Z',
            },
          ],
          bookings: [
            {
              id: 401,
              className: 'HIIT Strength Circuit',
              sessionStart: '2025-10-26T07:30:00Z',
              status: 'CONFIRMED',
              noShowProbability: 0.12,
            },
            {
              id: 395,
              className: 'Mobility & Core Flow',
              sessionStart: '2025-10-24T18:00:00Z',
              status: 'ATTENDED',
              noShowProbability: 0.08,
            },
          ],
          workouts: [
            {
              id: 501,
              title: 'Upper Body Hypertrophy (Chest & Back)',
              startedAt: '2025-10-24T08:00:00Z',
              status: 'COMPLETED',
            },
            {
              id: 492,
              title: 'Deadlift & Squat Strength PR',
              startedAt: '2025-10-21T07:45:00Z',
              status: 'COMPLETED',
            },
          ],
          attendance: [
            { id: 601, checkedInAt: '2025-10-24T07:28:00Z', source: 'FRONT_DESK_QR' },
            { id: 594, checkedInAt: '2025-10-21T07:40:00Z', source: 'MOBILE_APP' },
            { id: 588, checkedInAt: '2025-10-18T08:12:00Z', source: 'FRONT_DESK_QR' },
          ],
          stats: {
            totalSpent: 3920000,
            paidOrders: 8,
            totalBookings: 34,
            visitsLast30Days: 12,
            workoutsLast30Days: 10,
            lastVisitAt: '2025-10-24T07:28:00Z',
          },
        });
      } finally {
        setLoading(false);
      }
    }

    loadCustomer();
  }, [customerId]);

  if (loading || !data) {
    return (
      <div className="min-h-screen bg-[#F5F5F0] p-8 flex items-center justify-center">
        <div className="w-8 h-8 border-4 border-[#171A16] border-t-[#D7FF3F] rounded-full animate-spin" />
      </div>
    );
  }

  const { profile, currentMembership, stats } = data;

  const tabs: Array<{ id: Tab; label: string; count?: number }> = [
    { id: 'overview', label: 'Overview' },
    { id: 'membership', label: 'Memberships', count: data.memberships.length },
    { id: 'bookings', label: 'Bookings', count: data.bookings.length },
    { id: 'workouts', label: 'Workouts', count: data.workouts.length },
    { id: 'payments', label: 'Payments & Orders', count: data.orders.length },
    { id: 'activity', label: 'Activity Log', count: data.attendance.length },
  ];

  return (
    <div className="min-h-screen bg-[#F5F5F0]">
      {/* Top Bar Header */}
      <div className="bg-white border-b border-[#DADCD3] px-8 py-6">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <Link
                href="/dashboard/customers"
                className="text-xs font-semibold uppercase tracking-[0.1em] text-[#526B00] hover:underline"
              >
                ← Back to Customers
              </Link>
            </div>
            <div className="flex items-center gap-3">
              <div className="w-11 h-11 rounded-full bg-[#171A16] text-[#D7FF3F] font-bold text-lg flex items-center justify-center">
                {profile.fullName.charAt(0)}
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <h1 className="text-2xl font-bold text-[#111310]">{profile.fullName}</h1>
                  {currentMembership ? (
                    <span className="text-xs font-semibold px-2.5 py-0.5 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200">
                      {currentMembership.status}
                    </span>
                  ) : (
                    <span className="text-xs font-semibold px-2.5 py-0.5 rounded-full bg-gray-100 text-gray-600">
                      NO MEMBERSHIP
                    </span>
                  )}
                </div>
                <p className="text-sm text-[#5E625A]">
                  {profile.email} · {profile.phone || 'No phone'}
                </p>
              </div>
            </div>
          </div>

          {/* Quick Actions */}
          <div className="flex items-center gap-2.5">
            <Link
              href={`/dashboard/create-sale?customerId=${profile.id}`}
              className="bg-[#D7FF3F] text-[#111310] font-semibold rounded-lg px-4 py-2 text-sm hover:opacity-90 transition-opacity"
            >
              + Create Sale
            </Link>
          </div>
        </div>

        {/* Tab Navigation */}
        <div className="flex gap-2 border-t border-[#DADCD3] mt-6 pt-3 overflow-x-auto">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`px-4 py-2 text-sm font-semibold rounded-lg transition-all ${
                activeTab === tab.id
                  ? 'bg-[#171A16] text-[#ECEDE6]'
                  : 'text-[#5E625A] hover:text-[#111310] hover:bg-black/5'
              }`}
            >
              {tab.label}
              {tab.count !== undefined && (
                <span
                  className={`ml-2 text-xs px-2 py-0.5 rounded-full ${
                    activeTab === tab.id ? 'bg-white/20 text-white' : 'bg-[#DADCD3] text-[#5E625A]'
                  }`}
                >
                  {tab.count}
                </span>
              )}
            </button>
          ))}
        </div>
      </div>

      {/* Main Content Area */}
      <div className="p-8 max-w-7xl mx-auto">
        {/* Tab 1: OVERVIEW */}
        {activeTab === 'overview' && (
          <div className="space-y-6">
            {/* KPI Cards */}
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              <div className="bg-white border border-[#DADCD3] rounded-xl p-5">
                <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[#8A8E85] mb-1">
                  Visits (Last 30d)
                </p>
                <p className="text-3xl font-bold text-[#111310]">{stats.visitsLast30Days}</p>
                <p className="text-xs text-[#526B00] mt-1 font-medium">Consistent attendance</p>
              </div>
              <div className="bg-white border border-[#DADCD3] rounded-xl p-5">
                <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[#8A8E85] mb-1">
                  Total Spent
                </p>
                <p className="text-3xl font-bold text-[#111310]">
                  ₫{stats.totalSpent.toLocaleString()}
                </p>
                <p className="text-xs text-[#5E625A] mt-1">{stats.paidOrders} completed orders</p>
              </div>
              <div className="bg-white border border-[#DADCD3] rounded-xl p-5">
                <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[#8A8E85] mb-1">
                  Class Bookings
                </p>
                <p className="text-3xl font-bold text-[#111310]">{stats.totalBookings}</p>
                <p className="text-xs text-[#526B00] mt-1">Active participant</p>
              </div>
              <div className="bg-white border border-[#DADCD3] rounded-xl p-5">
                <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[#8A8E85] mb-1">
                  Expiry Countdown
                </p>
                <p className="text-3xl font-bold text-amber-600">
                  {currentMembership ? `${currentMembership.daysRemaining}d` : '—'}
                </p>
                <p className="text-xs text-amber-700 mt-1 font-medium">Renewal window active</p>
              </div>
            </div>

            {/* Profile & Notes */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              <div className="bg-white border border-[#DADCD3] rounded-xl p-6 md:col-span-2">
                <h3 className="text-base font-bold text-[#111310] mb-4">Member Profile</h3>
                <div className="grid grid-cols-2 gap-4 text-sm">
                  <div>
                    <span className="text-xs text-[#8A8E85] block">Full Name</span>
                    <span className="font-semibold text-[#111310]">{profile.fullName}</span>
                  </div>
                  <div>
                    <span className="text-xs text-[#8A8E85] block">Email</span>
                    <span className="font-semibold text-[#111310]">{profile.email}</span>
                  </div>
                  <div>
                    <span className="text-xs text-[#8A8E85] block">Phone</span>
                    <span className="font-semibold text-[#111310]">{profile.phone || 'N/A'}</span>
                  </div>
                  <div>
                    <span className="text-xs text-[#8A8E85] block">Primary Goal</span>
                    <span className="font-semibold text-[#526B00]">{profile.fitnessGoal || 'General Fitness'}</span>
                  </div>
                </div>

                <div className="mt-6 pt-4 border-t border-[#DADCD3]">
                  <span className="text-xs text-[#8A8E85] block mb-1">Trainer / Staff Notes</span>
                  <p className="text-sm text-[#5E625A] bg-[#F5F5F0] rounded-lg p-3">
                    {profile.notes || 'No special notes recorded.'}
                  </p>
                </div>
              </div>

              {/* PT Package Status */}
              <div className="bg-white border border-[#DADCD3] rounded-xl p-6">
                <h3 className="text-base font-bold text-[#111310] mb-3">PT Sessions</h3>
                {data.ptPackages.length > 0 ? (
                  data.ptPackages.map((pkg) => (
                    <div key={pkg.id} className="border border-[#DADCD3] rounded-lg p-4">
                      <p className="text-sm font-bold text-[#111310]">{pkg.planName}</p>
                      <div className="mt-3 flex items-center justify-between">
                        <span className="text-2xl font-extrabold text-[#526B00]">
                          {pkg.remainingSessions} / {pkg.totalSessions}
                        </span>
                        <span className="text-xs text-[#8A8E85]">sessions left</span>
                      </div>
                      <p className="text-xs text-[#5E625A] mt-2">Expires: {pkg.expiryDate}</p>
                    </div>
                  ))
                ) : (
                  <p className="text-sm text-[#8A8E85]">No active PT package.</p>
                )}
              </div>
            </div>
          </div>
        )}

        {/* Tab 2: MEMBERSHIP */}
        {activeTab === 'membership' && (
          <div className="bg-white border border-[#DADCD3] rounded-xl p-6">
            <h3 className="text-base font-bold text-[#111310] mb-4">Membership History</h3>
            <div className="space-y-4">
              {data.memberships.map((m) => (
                <div
                  key={m.id}
                  className="flex items-center justify-between border border-[#DADCD3] rounded-xl p-5"
                >
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="font-bold text-[#111310]">{m.planName}</span>
                      <span
                        className={`text-xs px-2 py-0.5 rounded-full font-semibold ${
                          m.status === 'ACTIVE'
                            ? 'bg-emerald-50 text-emerald-700'
                            : 'bg-gray-100 text-gray-600'
                        }`}
                      >
                        {m.status}
                      </span>
                    </div>
                    <p className="text-xs text-[#5E625A] mt-1">
                      {m.startDate} → {m.endDate}
                    </p>
                  </div>
                  {m.status === 'ACTIVE' && (
                    <span className="text-sm font-bold text-[#526B00]">
                      {m.daysRemaining} days remaining
                    </span>
                  )}
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Tab 3: BOOKINGS */}
        {activeTab === 'bookings' && (
          <div className="bg-white border border-[#DADCD3] rounded-xl overflow-hidden">
            <div className="p-5 border-b border-[#DADCD3]">
              <h3 className="text-base font-bold text-[#111310]">Class Bookings</h3>
            </div>
            <div className="divide-y divide-[#DADCD3]">
              {data.bookings.map((b) => (
                <div key={b.id} className="p-4 flex items-center justify-between">
                  <div>
                    <p className="text-sm font-bold text-[#111310]">{b.className}</p>
                    <p className="text-xs text-[#5E625A] mt-0.5">
                      {new Date(b.sessionStart).toLocaleString()}
                    </p>
                  </div>
                  <div className="flex items-center gap-3">
                    <NoShowBadge probability={b.noShowProbability} />
                    <span className="text-xs font-semibold px-2 py-1 rounded bg-[#F5F5F0] text-[#111310]">
                      {b.status}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Tab 4: WORKOUTS */}
        {activeTab === 'workouts' && (
          <div className="bg-white border border-[#DADCD3] rounded-xl p-6">
            <h3 className="text-base font-bold text-[#111310] mb-4">Logged Workouts</h3>
            <div className="space-y-3">
              {data.workouts.map((w) => (
                <div
                  key={w.id}
                  className="flex items-center justify-between border border-[#DADCD3] rounded-lg p-4"
                >
                  <div>
                    <p className="text-sm font-semibold text-[#111310]">{w.title}</p>
                    <p className="text-xs text-[#8A8E85]">
                      {new Date(w.startedAt).toLocaleDateString()}
                    </p>
                  </div>
                  <span className="text-xs font-bold text-[#526B00] bg-[#D7FF3F]/30 px-2.5 py-1 rounded-full">
                    {w.status}
                  </span>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Tab 5: PAYMENTS */}
        {activeTab === 'payments' && (
          <div className="bg-white border border-[#DADCD3] rounded-xl overflow-hidden">
            <div className="p-5 border-b border-[#DADCD3]">
              <h3 className="text-base font-bold text-[#111310]">Order & Payment History</h3>
            </div>
            <div className="divide-y divide-[#DADCD3]">
              {data.orders.map((o) => (
                <div key={o.id} className="p-4 flex items-center justify-between">
                  <div>
                    <span className="font-mono text-xs font-bold text-[#111310]">
                      {o.orderReference}
                    </span>
                    <p className="text-xs text-[#8A8E85] mt-0.5">
                      {new Date(o.createdAt).toLocaleDateString()}
                    </p>
                  </div>
                  <div className="flex items-center gap-4">
                    <span className="text-sm font-bold text-[#111310]">
                      ₫{o.totalAmount.toLocaleString()}
                    </span>
                    <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700">
                      {o.status}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Tab 6: ACTIVITY */}
        {activeTab === 'activity' && (
          <div className="bg-white border border-[#DADCD3] rounded-xl p-6">
            <h3 className="text-base font-bold text-[#111310] mb-4">Studio Check-in Timeline</h3>
            <div className="relative pl-6 space-y-6 before:absolute before:left-2 before:top-2 before:bottom-2 before:w-0.5 before:bg-[#DADCD3]">
              {data.attendance.map((a) => (
                <div key={a.id} className="relative">
                  <div className="absolute -left-6 top-1 w-3 h-3 rounded-full bg-[#D7FF3F] border-2 border-[#111310]" />
                  <p className="text-sm font-semibold text-[#111310]">Checked In</p>
                  <p className="text-xs text-[#5E625A]">
                    {new Date(a.checkedInAt).toLocaleString()} · Method: {a.source}
                  </p>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
