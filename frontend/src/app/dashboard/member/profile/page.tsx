'use client';

import React, { useEffect, useState } from 'react';
import api from '@/lib/api';
import MembershipBadge from '@/components/MembershipBadge';
import { formatDate, type MembershipDto } from '@/lib/contracts';

interface ProfileResponse {
  userId: number;
  email: string;
  fullName: string;
  phone?: string | null;
  roles: string[];
  studioId?: number | null;
  customer?: {
    id: number;
    fullName: string;
    phone: string;
    email?: string | null;
    dob?: string | null;
    gender?: string | null;
    fitnessGoal?: string | null;
    emergencyContactName?: string | null;
    emergencyContactPhone?: string | null;
  } | null;
}

const GOALS = [
  { value: 'WEIGHT_LOSS', label: 'Giảm cân' },
  { value: 'MUSCLE_GAIN', label: 'Tăng cơ' },
  { value: 'STRENGTH', label: 'Tăng sức mạnh' },
  { value: 'MOBILITY', label: 'Linh hoạt / giãn cơ' },
  { value: 'GENERAL_FITNESS', label: 'Sức khoẻ tổng quát' },
];

/** Task #80 — hồ sơ cá nhân thật: xem, sửa và đổi mật khẩu. */
export default function ProfilePage() {
  const [profile, setProfile] = useState<ProfileResponse | null>(null);
  const [membership, setMembership] = useState<MembershipDto | null>(null);
  const [form, setForm] = useState({
    fullName: '',
    phone: '',
    dob: '',
    gender: '',
    fitnessGoal: '',
    emergencyContactName: '',
    emergencyContactPhone: '',
  });
  const [passwords, setPasswords] = useState({ currentPassword: '', newPassword: '' });
  const [isLoading, setIsLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [changingPassword, setChangingPassword] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      setIsLoading(true);
      try {
        const { data } = await api.get<ProfileResponse>('/profile/me');
        if (cancelled) return;
        setProfile(data);
        setForm({
          fullName: data.fullName ?? '',
          phone: data.phone ?? '',
          dob: data.customer?.dob ?? '',
          gender: data.customer?.gender ?? '',
          fitnessGoal: data.customer?.fitnessGoal ?? '',
          emergencyContactName: data.customer?.emergencyContactName ?? '',
          emergencyContactPhone: data.customer?.emergencyContactPhone ?? '',
        });

        try {
          const membershipRes = await api.get('/memberships/me');
          if (!cancelled && Array.isArray(membershipRes.data) && membershipRes.data.length > 0) {
            const items = membershipRes.data as MembershipDto[];
            setMembership(items.find((item) => item.status === 'ACTIVE') ?? items[0]);
          }
        } catch {
          // Hội viên chưa có gói — không chặn trang
        }
      } catch (err) {
        console.error('Không tải được hồ sơ:', err);
        if (!cancelled) setError('Không tải được hồ sơ cá nhân.');
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    };
    load();
    return () => {
      cancelled = true;
    };
  }, []);

  const save = async (event: React.FormEvent) => {
    event.preventDefault();
    setSaving(true);
    setError(null);
    setNotice(null);
    try {
      const { data } = await api.put<ProfileResponse>('/profile/me', {
        fullName: form.fullName.trim(),
        phone: form.phone.trim() || null,
        dob: form.dob || null,
        gender: form.gender || null,
        fitnessGoal: form.fitnessGoal || null,
        emergencyContactName: form.emergencyContactName || null,
        emergencyContactPhone: form.emergencyContactPhone || null,
      });
      setProfile(data);
      setNotice('Đã lưu hồ sơ.');
    } catch (err: unknown) {
      const message =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        'Lưu hồ sơ thất bại.';
      setError(message);
    } finally {
      setSaving(false);
    }
  };

  const changePassword = async (event: React.FormEvent) => {
    event.preventDefault();
    setChangingPassword(true);
    setError(null);
    setNotice(null);
    try {
      await api.put('/profile/password', passwords);
      setPasswords({ currentPassword: '', newPassword: '' });
      setNotice('Đã đổi mật khẩu.');
    } catch (err: unknown) {
      const message =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
        'Đổi mật khẩu thất bại.';
      setError(message);
    } finally {
      setChangingPassword(false);
    }
  };

  const initials = (form.fullName || profile?.email || 'U')
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase())
    .join('');

  return (
    <div className="min-h-screen bg-[#F5F5F0]">
      <div className="bg-white border-b border-[#DADCD3] px-8 py-6">
        <div className="flex items-start justify-between gap-4">
          <div>
            <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[#526B00] mb-1">
              HỘI VIÊN · HỒ SƠ
            </p>
            <h1 className="text-2xl font-bold text-[#111310]">Hồ sơ của tôi</h1>
            <p className="text-sm text-[#5E625A] mt-1">Quản lý thông tin cá nhân và bảo mật tài khoản</p>
          </div>
          <button
            type="submit"
            form="profile-form"
            disabled={saving || isLoading}
            className="bg-[#D7FF3F] text-[#111310] font-semibold rounded-lg px-4 py-2 text-sm hover:bg-[#C4E82E] disabled:opacity-50"
          >
            {saving ? 'Đang lưu…' : 'Lưu thay đổi'}
          </button>
        </div>
      </div>

      <div className="p-8 space-y-4">
        {error && (
          <p role="alert" className="text-sm text-red-700 bg-red-50 border border-red-200 rounded-lg px-4 py-3">
            {error}
          </p>
        )}
        {notice && (
          <p className="text-sm text-emerald-800 bg-emerald-50 border border-emerald-200 rounded-lg px-4 py-3">
            {notice}
          </p>
        )}

        {isLoading ? (
          <div className="p-12 text-center">
            <div className="w-8 h-8 border-2 border-[#171A16] border-t-[#D7FF3F] rounded-full animate-spin mx-auto" />
          </div>
        ) : (
          <div className="flex gap-6 items-start">
            <div className="flex-1 space-y-5">
              <div className="bg-white border border-[#DADCD3] rounded-xl p-5 flex items-center gap-5">
                <div className="w-20 h-20 rounded-full bg-[#D7FF3F] flex items-center justify-center text-2xl font-bold text-[#111310] shrink-0">
                  {initials || 'U'}
                </div>
                <div className="min-w-0">
                  <p className="text-lg font-bold text-[#111310] truncate">{form.fullName || 'Chưa đặt tên'}</p>
                  <p className="text-sm text-[#5E625A] truncate">{profile?.email}</p>
                  <p className="text-[11px] text-[#8A8E85] mt-1">
                    {profile?.roles?.map((role) => role.replace('ROLE_', '')).join(' · ') || 'MEMBER'}
                  </p>
                </div>
              </div>

              <form id="profile-form" onSubmit={save} className="bg-white border border-[#DADCD3] rounded-xl p-6">
                <h2 className="text-sm font-semibold text-[#111310] mb-5">Thông tin cá nhân</h2>

                <div className="grid grid-cols-2 gap-4 mb-4">
                  <Field label="Họ và tên">
                    <input
                      required
                      value={form.fullName}
                      onChange={(event) => setForm({ ...form, fullName: event.target.value })}
                      className="w-full border border-[#DADCD3] rounded-lg px-3 py-2.5 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-[#D7FF3F]"
                    />
                  </Field>
                  <Field label="Số điện thoại">
                    <input
                      value={form.phone}
                      onChange={(event) => setForm({ ...form, phone: event.target.value })}
                      className="w-full border border-[#DADCD3] rounded-lg px-3 py-2.5 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-[#D7FF3F]"
                    />
                  </Field>
                </div>

                <div className="mb-4">
                  <Field label="Email (không đổi được)">
                    <input
                      readOnly
                      value={profile?.email ?? ''}
                      className="w-full border border-[#DADCD3] rounded-lg px-3 py-2.5 text-sm bg-[#F5F5F0] text-[#5E625A]"
                    />
                  </Field>
                </div>

                <div className="grid grid-cols-2 gap-4 mb-4">
                  <Field label="Ngày sinh">
                    <input
                      type="date"
                      value={form.dob}
                      onChange={(event) => setForm({ ...form, dob: event.target.value })}
                      className="w-full border border-[#DADCD3] rounded-lg px-3 py-2.5 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-[#D7FF3F]"
                    />
                  </Field>
                  <Field label="Giới tính">
                    <select
                      value={form.gender}
                      onChange={(event) => setForm({ ...form, gender: event.target.value })}
                      className="w-full border border-[#DADCD3] rounded-lg px-3 py-2.5 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-[#D7FF3F]"
                    >
                      <option value="">Chưa chọn</option>
                      <option value="MALE">Nam</option>
                      <option value="FEMALE">Nữ</option>
                      <option value="OTHER">Khác</option>
                    </select>
                  </Field>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <Field label="Liên hệ khẩn cấp">
                    <input
                      value={form.emergencyContactName}
                      onChange={(event) => setForm({ ...form, emergencyContactName: event.target.value })}
                      className="w-full border border-[#DADCD3] rounded-lg px-3 py-2.5 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-[#D7FF3F]"
                    />
                  </Field>
                  <Field label="SĐT khẩn cấp">
                    <input
                      value={form.emergencyContactPhone}
                      onChange={(event) => setForm({ ...form, emergencyContactPhone: event.target.value })}
                      className="w-full border border-[#DADCD3] rounded-lg px-3 py-2.5 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-[#D7FF3F]"
                    />
                  </Field>
                </div>
              </form>

              <form onSubmit={changePassword} className="bg-white border border-[#DADCD3] rounded-xl p-6">
                <h2 className="text-sm font-semibold text-[#111310] mb-5">Đổi mật khẩu</h2>
                <div className="grid grid-cols-2 gap-4">
                  <Field label="Mật khẩu hiện tại">
                    <input
                      required
                      type="password"
                      value={passwords.currentPassword}
                      onChange={(event) =>
                        setPasswords({ ...passwords, currentPassword: event.target.value })
                      }
                      className="w-full border border-[#DADCD3] rounded-lg px-3 py-2.5 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-[#D7FF3F]"
                    />
                  </Field>
                  <Field label="Mật khẩu mới (≥ 6 ký tự)">
                    <input
                      required
                      minLength={6}
                      type="password"
                      value={passwords.newPassword}
                      onChange={(event) => setPasswords({ ...passwords, newPassword: event.target.value })}
                      className="w-full border border-[#DADCD3] rounded-lg px-3 py-2.5 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-[#D7FF3F]"
                    />
                  </Field>
                </div>
                <button
                  type="submit"
                  disabled={changingPassword}
                  className="mt-4 bg-[#171A16] text-[#ECEDE6] font-semibold rounded-lg px-4 py-2 text-sm hover:opacity-90 disabled:opacity-50"
                >
                  {changingPassword ? 'Đang cập nhật…' : 'Cập nhật mật khẩu'}
                </button>
              </form>
            </div>

            <aside className="w-80 shrink-0 space-y-4">
              <div className="bg-white border border-[#DADCD3] rounded-xl p-5">
                <h3 className="text-sm font-semibold text-[#111310] mb-4">Mục tiêu tập luyện</h3>
                <div className="space-y-2">
                  {GOALS.map((goal) => (
                    <label
                      key={goal.value}
                      className={`flex items-center gap-3 cursor-pointer rounded-lg px-2 py-1.5 border ${
                        form.fitnessGoal === goal.value
                          ? 'border-[#526B00] bg-[#D7FF3F]/20'
                          : 'border-transparent hover:bg-[#F5F5F0]'
                      }`}
                    >
                      <input
                        type="radio"
                        name="fitnessGoal"
                        value={goal.value}
                        checked={form.fitnessGoal === goal.value}
                        onChange={(event) => setForm({ ...form, fitnessGoal: event.target.value })}
                        className="accent-[#526B00]"
                      />
                      <span className="text-sm text-[#111310]">{goal.label}</span>
                    </label>
                  ))}
                </div>
              </div>

              <div className="bg-white border border-[#DADCD3] rounded-xl p-5">
                <h3 className="text-sm font-semibold text-[#111310] mb-3">Gói tập</h3>
                {membership ? (
                  <>
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-sm text-[#5E625A]">Gói</span>
                      <span className="text-sm font-semibold text-[#111310]">{membership.planName}</span>
                    </div>
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-sm text-[#5E625A]">Trạng thái</span>
                      <MembershipBadge
                        status={membership.status}
                        daysRemaining={membership.daysRemaining}
                        compact
                      />
                    </div>
                    <div className="flex items-center justify-between">
                      <span className="text-sm text-[#5E625A]">Hết hạn</span>
                      <span className="text-sm font-semibold text-[#111310]">
                        {formatDate(membership.endDate)}
                      </span>
                    </div>
                  </>
                ) : (
                  <p className="text-xs text-[#8A8E85]">Chưa có gói tập — liên hệ lễ tân để đăng ký.</p>
                )}
              </div>
            </aside>
          </div>
        )}
      </div>
    </div>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="block">
      <span className="block text-xs font-semibold uppercase tracking-[0.08em] text-[#8A8E85] mb-1.5">
        {label}
      </span>
      {children}
    </label>
  );
}
