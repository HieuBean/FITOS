'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import apiClient from '@/lib/api';

interface CustomerItem {
  id: number;
  fullName: string;
  email: string;
  phone: string;
  fitnessGoal: string;
  createdAt: string;
  membership?: {
    id: number;
    planName: string;
    status: string;
    endDate: string;
    daysRemaining: number;
  } | null;
}

const statusPill: Record<string, string> = {
  ACTIVE: 'bg-emerald-50 text-emerald-700 border border-emerald-200',
  PAUSED: 'bg-amber-50 text-amber-700 border border-amber-200',
  EXPIRED: 'bg-rose-50 text-rose-700 border border-rose-200',
  CANCELLED: 'bg-gray-100 text-gray-700 border border-gray-200',
  NONE: 'bg-gray-50 text-gray-500 border border-gray-200',
};

export default function CustomersPage() {
  const [customers, setCustomers] = useState<CustomerItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');

  useEffect(() => {
    async function fetchCustomers() {
      setLoading(true);
      try {
        const res = await apiClient.get('/customers', {
          params: {
            search: search || undefined,
            membershipStatus: statusFilter || undefined,
          },
        });
        setCustomers(res.data.content || []);
      } catch (err) {
        // Fallback demo data
        setCustomers([
          {
            id: 1,
            fullName: 'Sophie Turner',
            email: 'sophie.turner@email.com',
            phone: '+84 901 234 567',
            fitnessGoal: 'Hypertrophy & Endurance',
            createdAt: '2025-03-01T08:00:00Z',
            membership: {
              id: 101,
              planName: 'Perform',
              status: 'ACTIVE',
              endDate: '2025-11-01',
              daysRemaining: 7,
            },
          },
          {
            id: 2,
            fullName: 'James Hargreaves',
            email: 'j.hargreaves@email.com',
            phone: '+84 902 345 678',
            fitnessGoal: 'Weight Loss',
            createdAt: '2025-01-15T09:00:00Z',
            membership: {
              id: 102,
              planName: 'Move',
              status: 'ACTIVE',
              endDate: '2025-11-15',
              daysRemaining: 21,
            },
          },
          {
            id: 3,
            fullName: 'Priya Sharma',
            email: 'priya.sharma@email.com',
            phone: '+84 903 456 789',
            fitnessGoal: 'Strength Training',
            createdAt: '2025-02-10T10:00:00Z',
            membership: {
              id: 103,
              planName: 'PT Focus',
              status: 'ACTIVE',
              endDate: '2025-11-10',
              daysRemaining: 16,
            },
          },
          {
            id: 4,
            fullName: 'Harry Wilson',
            email: 'h.wilson@email.com',
            phone: '+84 904 567 890',
            fitnessGoal: 'Rehab & Mobility',
            createdAt: '2024-12-12T07:30:00Z',
            membership: {
              id: 104,
              planName: 'Perform',
              status: 'PAUSED',
              endDate: '2025-12-01',
              daysRemaining: 37,
            },
          },
          {
            id: 5,
            fullName: 'Isabella Moore',
            email: 'i.moore@email.com',
            phone: '+84 905 678 901',
            fitnessGoal: 'Cardio Fitness',
            createdAt: '2024-09-15T11:00:00Z',
            membership: {
              id: 105,
              planName: 'Move',
              status: 'EXPIRED',
              endDate: '2025-09-15',
              daysRemaining: 0,
            },
          },
        ]);
      } finally {
        setLoading(false);
      }
    }

    const timer = setTimeout(fetchCustomers, 250);
    return () => clearTimeout(timer);
  }, [search, statusFilter]);

  return (
    <div className="min-h-screen bg-[#F5F5F0]">
      {/* Page Header */}
      <div className="bg-white border-b border-[#DADCD3] px-8 py-6">
        <div className="flex items-start justify-between">
          <div>
            <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[#526B00] mb-1">
              OPERATE · MEMBERS
            </p>
            <h1 className="text-2xl font-bold text-[#111310]">Customers</h1>
            <p className="text-sm text-[#5E625A] mt-1">
              Search and view full 360° member records
            </p>
          </div>
          <Link
            href="/dashboard/create-sale"
            className="bg-[#D7FF3F] text-[#111310] font-semibold rounded-lg px-4 py-2 text-sm hover:opacity-90"
          >
            + New Sale
          </Link>
        </div>
      </div>

      {/* Page Content */}
      <div className="p-8">
        {/* Filters and Search Bar */}
        <div className="flex flex-col md:flex-row gap-4 justify-between items-center mb-6">
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search by name, email or phone..."
            className="w-full md:max-w-md bg-white border border-[#DADCD3] rounded-lg px-4 py-2.5 text-sm text-[#111310] placeholder-[#8A8E85] focus:outline-none focus:ring-2 focus:ring-[#D7FF3F]"
          />

          <div className="flex gap-2 w-full md:w-auto overflow-x-auto">
            {[
              { id: '', label: 'All' },
              { id: 'ACTIVE', label: 'Active' },
              { id: 'PAUSED', label: 'Paused' },
              { id: 'EXPIRED', label: 'Expired' },
              { id: 'NONE', label: 'No Membership' },
            ].map((tab) => (
              <button
                key={tab.id}
                onClick={() => setStatusFilter(tab.id)}
                className={`px-3.5 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap transition-colors ${
                  statusFilter === tab.id
                    ? 'bg-[#171A16] text-[#ECEDE6]'
                    : 'bg-white border border-[#DADCD3] text-[#5E625A] hover:bg-black/5'
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>
        </div>

        {/* Table */}
        <div className="bg-white border border-[#DADCD3] rounded-xl overflow-hidden shadow-sm">
          {/* Header */}
          <div className="grid grid-cols-[2fr_2fr_1.5fr_1fr_1.2fr_1fr] px-6 py-3.5 bg-[#F5F5F0]">
            {['Name', 'Email', 'Plan', 'Status', 'Expiry Date', 'Actions'].map((col) => (
              <span key={col} className="text-[11px] font-semibold uppercase tracking-[0.08em] text-[#8A8E85]">
                {col}
              </span>
            ))}
          </div>

          {/* Rows */}
          {loading ? (
            <div className="p-8 text-center text-sm text-[#8A8E85]">Loading customers...</div>
          ) : customers.length === 0 ? (
            <div className="p-8 text-center text-sm text-[#8A8E85]">No customers found matching filter.</div>
          ) : (
            <div className="divide-y divide-[#DADCD3]">
              {customers.map((c) => {
                const status = c.membership?.status || 'NONE';
                return (
                  <div
                    key={c.id}
                    className="grid grid-cols-[2fr_2fr_1.5fr_1fr_1.2fr_1fr] px-6 items-center py-4 hover:bg-[#F9FFDF]/30 transition-colors"
                  >
                    <Link
                      href={`/dashboard/customers/${c.id}`}
                      className="flex items-center gap-3 group"
                    >
                      <div className="w-8 h-8 rounded-full bg-[#171A16] text-[#D7FF3F] flex items-center justify-center text-xs font-bold flex-shrink-0">
                        {c.fullName.charAt(0)}
                      </div>
                      <span className="text-sm font-semibold text-[#111310] group-hover:text-[#526B00] truncate">
                        {c.fullName}
                      </span>
                    </Link>

                    <span className="text-sm text-[#5E625A] truncate">{c.email}</span>
                    <span className="text-sm font-medium text-[#111310]">
                      {c.membership?.planName || '—'}
                    </span>

                    <div>
                      <span className={`text-xs font-semibold px-2.5 py-0.5 rounded-full ${statusPill[status]}`}>
                        {status}
                      </span>
                    </div>

                    <span className="text-sm text-[#5E625A]">
                      {c.membership?.endDate || '—'}
                    </span>

                    <div>
                      <Link
                        href={`/dashboard/customers/${c.id}`}
                        className="text-xs font-semibold text-[#526B00] hover:underline"
                      >
                        View 360° →
                      </Link>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
