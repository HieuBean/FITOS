'use client';

import React, { useCallback, useEffect, useState } from 'react';
import api from '@/lib/api';
import RoleGuard from '@/components/RoleGuard';
import { formatCurrency, type PlanDto } from '@/lib/contracts';

const emptyForm = {
  name: '',
  planType: 'MEMBERSHIP' as 'MEMBERSHIP' | 'PT_PACKAGE',
  price: '',
  durationDays: '30',
  sessionCount: '',
  description: '',
  features: '',
};

/** Task #85 — trang quản lý gói dịch vụ (bật/tắt bán, tạo mới). */
export default function PlansPage() {
  const [plans, setPlans] = useState<PlanDto[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showModal, setShowModal] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [saving, setSaving] = useState(false);
  const [busyId, setBusyId] = useState<number | null>(null);

  const load = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const { data } = await api.get('/plans', { params: { includeInactive: true } });
      setPlans(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('Không tải được danh sách gói:', err);
      setError('Không tải được danh sách gói dịch vụ.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const toggle = async (plan: PlanDto) => {
    setBusyId(plan.id);
    try {
      await api.patch(`/plans/${plan.id}/toggle`);
      await load();
    } catch (err) {
      console.error('Bật/tắt gói thất bại:', err);
      setError('Không đổi được trạng thái gói.');
    } finally {
      setBusyId(null);
    }
  };

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    setSaving(true);
    setError(null);
    try {
      await api.post('/plans', {
        name: form.name.trim(),
        planType: form.planType,
        price: Number(form.price),
        durationDays: Number(form.durationDays),
        sessionCount: form.planType === 'PT_PACKAGE' ? Number(form.sessionCount || 0) : null,
        description: form.description || null,
        features: form.features
          .split('\n')
          .map((line) => line.trim())
          .filter(Boolean),
        isActive: true,
      });
      setShowModal(false);
      setForm(emptyForm);
      await load();
    } catch (err: unknown) {
      const message =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        'Tạo gói thất bại.';
      setError(message);
    } finally {
      setSaving(false);
    }
  };

  const memberships = plans.filter((plan) => plan.planType === 'MEMBERSHIP');
  const packages = plans.filter((plan) => plan.planType === 'PT_PACKAGE');

  return (
    <RoleGuard allowedRoles={['ADMIN', 'MANAGER']}>
      <div className="min-h-screen bg-[#F5F5F0]">
        <div className="bg-white border-b border-[#DADCD3] px-8 py-6">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
            <div>
              <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[#526B00] mb-1">
                KINH DOANH · GÓI DỊCH VỤ
              </p>
              <h1 className="text-2xl font-bold text-[#111310] tracking-tight">Gói hội viên &amp; Gói PT</h1>
              <p className="text-sm text-[#5E625A] mt-0.5">
                Giá, thời hạn và tính năng dùng chung cho mọi đơn hàng trong studio
              </p>
            </div>
            <button
              type="button"
              onClick={() => setShowModal(true)}
              className="bg-[#D7FF3F] text-[#111310] font-bold rounded-lg px-4 py-2.5 text-sm hover:bg-[#C4E82E] transition-all shadow-sm"
            >
              + Tạo gói mới
            </button>
          </div>
        </div>

        <div className="p-8 space-y-6">
          {error && (
            <p role="alert" className="text-sm text-red-700 bg-red-50 border border-red-200 rounded-lg px-4 py-3">
              {error}
            </p>
          )}

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <StatCard label="Tổng số gói" value={String(plans.length)} hint="Bao gồm gói đã ngừng bán" />
            <StatCard
              label="Gói hội viên"
              value={String(memberships.length)}
              hint={`${memberships.filter((plan) => plan.isActive).length} đang bán`}
            />
            <StatCard
              label="Gói PT"
              value={String(packages.length)}
              hint={`${packages.filter((plan) => plan.isActive).length} đang bán`}
            />
          </div>

          <div className="bg-white border border-[#DADCD3] rounded-xl overflow-hidden shadow-sm">
            <div className="grid grid-cols-[2fr_1fr_1fr_1.2fr_1.6fr_0.9fr] px-6 py-3.5 bg-[#F5F5F0] border-b border-[#DADCD3] text-[11px] font-bold uppercase tracking-[0.08em] text-[#8A8E85]">
              <span>Tên gói</span>
              <span>Loại</span>
              <span>Giá</span>
              <span>Thời hạn</span>
              <span>Tính năng</span>
              <span className="text-right">Trạng thái</span>
            </div>

            {isLoading ? (
              <div className="p-12 text-center">
                <div className="w-8 h-8 border-2 border-[#171A16] border-t-[#D7FF3F] rounded-full animate-spin mx-auto" />
              </div>
            ) : plans.length === 0 ? (
              <div className="p-12 text-center">
                <p className="text-sm font-bold text-[#111310]">Chưa có gói dịch vụ</p>
                <p className="text-xs text-[#8A8E85] mt-1">Bấm “Tạo gói mới” để thêm gói đầu tiên.</p>
              </div>
            ) : (
              <div className="divide-y divide-[#DADCD3]">
                {plans.map((plan) => (
                  <div
                    key={plan.id}
                    className="grid grid-cols-[2fr_1fr_1fr_1.2fr_1.6fr_0.9fr] px-6 py-4 items-center hover:bg-[#F9F9F7] transition-colors"
                  >
                    <div className="min-w-0">
                      <p className="text-sm font-bold text-[#111310] truncate">{plan.name}</p>
                      <p className="text-[11px] text-[#8A8E85] truncate">{plan.description || '—'}</p>
                    </div>
                    <span className="text-xs font-semibold text-[#5E625A]">{plan.planType}</span>
                    <span className="text-sm font-bold text-[#111310]">{formatCurrency(plan.price)}</span>
                    <span className="text-xs text-[#5E625A]">
                      {plan.durationDays} ngày
                      {plan.sessionCount ? ` · ${plan.sessionCount} buổi` : ''}
                    </span>
                    <div className="flex flex-wrap gap-1 pr-2">
                      {plan.features.slice(0, 2).map((feature) => (
                        <span
                          key={feature}
                          className="text-[10px] bg-[#F5F5F0] border border-[#DADCD3] rounded-full px-2 py-0.5 text-[#5E625A]"
                        >
                          {feature}
                        </span>
                      ))}
                      {plan.features.length > 2 && (
                        <span className="text-[10px] text-[#8A8E85]">+{plan.features.length - 2}</span>
                      )}
                    </div>
                    <div className="text-right">
                      <button
                        type="button"
                        onClick={() => toggle(plan)}
                        disabled={busyId === plan.id}
                        className={`text-[11px] font-bold px-2.5 py-1 rounded-full border transition-colors cursor-pointer disabled:opacity-50 ${
                          plan.isActive
                            ? 'bg-emerald-100 text-emerald-800 border-emerald-200'
                            : 'bg-zinc-100 text-zinc-600 border-zinc-300'
                        }`}
                      >
                        {plan.isActive ? 'Đang bán' : 'Ngừng bán'}
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {showModal && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#111310]/50 p-4">
            <form
              onSubmit={submit}
              className="w-full max-w-lg bg-white rounded-2xl shadow-xl overflow-hidden"
            >
              <div className="px-6 py-4 border-b border-[#DADCD3]">
                <h2 className="text-base font-bold text-[#111310]">Tạo gói dịch vụ mới</h2>
              </div>
              <div className="px-6 py-5 space-y-3 max-h-[70vh] overflow-y-auto">
                <Field label="Tên gói">
                  <input
                    required
                    value={form.name}
                    onChange={(event) => setForm({ ...form, name: event.target.value })}
                    className="w-full border border-[#DADCD3] rounded-lg px-3 py-2 text-sm"
                    placeholder="Perform Monthly"
                  />
                </Field>
                <Field label="Loại gói">
                  <select
                    value={form.planType}
                    onChange={(event) =>
                      setForm({ ...form, planType: event.target.value as 'MEMBERSHIP' | 'PT_PACKAGE' })
                    }
                    className="w-full border border-[#DADCD3] rounded-lg px-3 py-2 text-sm bg-white"
                  >
                    <option value="MEMBERSHIP">MEMBERSHIP</option>
                    <option value="PT_PACKAGE">PT_PACKAGE</option>
                  </select>
                </Field>
                <div className="grid grid-cols-2 gap-3">
                  <Field label="Giá (VND)">
                    <input
                      required
                      type="number"
                      min={0}
                      value={form.price}
                      onChange={(event) => setForm({ ...form, price: event.target.value })}
                      className="w-full border border-[#DADCD3] rounded-lg px-3 py-2 text-sm"
                    />
                  </Field>
                  <Field label="Thời hạn (ngày)">
                    <input
                      required
                      type="number"
                      min={1}
                      value={form.durationDays}
                      onChange={(event) => setForm({ ...form, durationDays: event.target.value })}
                      className="w-full border border-[#DADCD3] rounded-lg px-3 py-2 text-sm"
                    />
                  </Field>
                </div>
                {form.planType === 'PT_PACKAGE' && (
                  <Field label="Số buổi PT">
                    <input
                      required
                      type="number"
                      min={1}
                      value={form.sessionCount}
                      onChange={(event) => setForm({ ...form, sessionCount: event.target.value })}
                      className="w-full border border-[#DADCD3] rounded-lg px-3 py-2 text-sm"
                    />
                  </Field>
                )}
                <Field label="Mô tả">
                  <textarea
                    rows={2}
                    value={form.description}
                    onChange={(event) => setForm({ ...form, description: event.target.value })}
                    className="w-full border border-[#DADCD3] rounded-lg px-3 py-2 text-sm"
                  />
                </Field>
                <Field label="Tính năng (mỗi dòng một tính năng)">
                  <textarea
                    rows={3}
                    value={form.features}
                    onChange={(event) => setForm({ ...form, features: event.target.value })}
                    className="w-full border border-[#DADCD3] rounded-lg px-3 py-2 text-sm"
                    placeholder={'Unlimited classes\nAI coaching'}
                  />
                </Field>
              </div>
              <div className="px-6 py-4 border-t border-[#DADCD3] flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="px-4 py-2 text-sm font-semibold text-[#5E625A] hover:text-[#111310]"
                >
                  Huỷ
                </button>
                <button
                  type="submit"
                  disabled={saving}
                  className="px-4 py-2 text-sm font-bold bg-[#D7FF3F] text-[#111310] rounded-lg hover:bg-[#C4E82E] disabled:opacity-50"
                >
                  {saving ? 'Đang lưu…' : 'Lưu gói'}
                </button>
              </div>
            </form>
          </div>
        )}
      </div>
    </RoleGuard>
  );
}

function StatCard({ label, value, hint }: { label: string; value: string; hint: string }) {
  return (
    <div className="bg-white border border-[#DADCD3] rounded-xl p-5 shadow-sm">
      <p className="text-[11px] font-semibold uppercase tracking-[0.08em] text-[#8A8E85] mb-1.5">{label}</p>
      <p className="text-3xl font-black text-[#111310]">{value}</p>
      <p className="text-xs text-[#5E625A] mt-1">{hint}</p>
    </div>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="block">
      <span className="text-[11px] font-semibold uppercase tracking-[0.08em] text-[#8A8E85]">{label}</span>
      <div className="mt-1">{children}</div>
    </label>
  );
}
