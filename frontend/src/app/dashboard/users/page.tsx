'use client';

import React, { useCallback, useEffect, useMemo, useState } from 'react';
import api from '@/lib/api';
import RoleGuard from '@/components/RoleGuard';
import { formatDate } from '@/lib/contracts';

interface UserRow {
  id: number;
  email: string;
  fullName: string;
  phone?: string | null;
  isActive: boolean;
  studioId?: number | null;
  roles: string[];
  createdAt?: string | null;
}

const ROLE_LABELS: Record<string, string> = {
  ROLE_ADMIN: 'Quản trị',
  ROLE_MANAGER: 'Quản lý',
  ROLE_RECEPTIONIST: 'Lễ tân',
  ROLE_TRAINER: 'HLV',
  ROLE_MEMBER: 'Hội viên',
};

const ROLE_STYLES: Record<string, string> = {
  ROLE_ADMIN: 'bg-[#171A16] text-[#D7FF3F]',
  ROLE_MANAGER: 'bg-purple-100 text-purple-800',
  ROLE_RECEPTIONIST: 'bg-blue-100 text-blue-800',
  ROLE_TRAINER: 'bg-amber-100 text-amber-800',
  ROLE_MEMBER: 'bg-[#F0F0EB] text-[#5E625A]',
};

const emptyForm = {
  email: '',
  password: '',
  fullName: '',
  phone: '',
  role: 'ROLE_RECEPTIONIST',
};

/** Task #98 — quản trị người dùng (ADMIN/MANAGER): list, đổi vai trò, khoá/mở, tạo mới. */
export default function UsersPage() {
  const [users, setUsers] = useState<UserRow[]>([]);
  const [search, setSearch] = useState('');
  const [debounced, setDebounced] = useState('');
  const [roleFilter, setRoleFilter] = useState('ALL');
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<number | null>(null);
  const [showModal, setShowModal] = useState(false);
  const [form, setForm] = useState(emptyForm);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    const handler = setTimeout(() => setDebounced(search), 300);
    return () => clearTimeout(handler);
  }, [search]);

  const load = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const params: Record<string, unknown> = {};
      if (debounced.trim()) params.search = debounced.trim();
      if (roleFilter !== 'ALL') params.role = roleFilter;
      const { data } = await api.get('/users', { params });
      setUsers(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error('Không tải được danh sách người dùng:', err);
      setUsers([]);
      setError('Không tải được danh sách người dùng.');
    } finally {
      setIsLoading(false);
    }
  }, [debounced, roleFilter]);

  useEffect(() => {
    load();
  }, [load]);

  const stats = useMemo(
    () => ({
      total: users.length,
      active: users.filter((user) => user.isActive).length,
      staff: users.filter((user) => user.roles.some((role) => role !== 'ROLE_MEMBER')).length,
      members: users.filter((user) => user.roles.includes('ROLE_MEMBER')).length,
    }),
    [users],
  );

  const changeRole = async (user: UserRow, nextRole: string) => {
    setBusyId(user.id);
    setError(null);
    setNotice(null);
    try {
      await api.patch(`/users/${user.id}/role`, { role: nextRole });
      setNotice(`Đã đổi vai trò của ${user.fullName}.`);
      await load();
    } catch (err: unknown) {
      setError(
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
          'Không đổi được vai trò.',
      );
    } finally {
      setBusyId(null);
    }
  };

  const toggleActive = async (user: UserRow) => {
    setBusyId(user.id);
    setError(null);
    setNotice(null);
    try {
      await api.patch(`/users/${user.id}/status`, { isActive: !user.isActive });
      setNotice(`${user.isActive ? 'Đã khoá' : 'Đã mở khoá'} tài khoản ${user.fullName}.`);
      await load();
    } catch (err: unknown) {
      setError(
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
          'Không đổi được trạng thái tài khoản.',
      );
    } finally {
      setBusyId(null);
    }
  };

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    setSaving(true);
    setError(null);
    try {
      await api.post('/users', {
        email: form.email.trim(),
        password: form.password,
        fullName: form.fullName.trim(),
        phone: form.phone.trim() || null,
        role: form.role,
      });
      setShowModal(false);
      setForm(emptyForm);
      setNotice('Đã tạo tài khoản mới.');
      await load();
    } catch (err: unknown) {
      setError(
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ??
          'Tạo tài khoản thất bại.',
      );
    } finally {
      setSaving(false);
    }
  };

  return (
    <RoleGuard allowedRoles={['ADMIN', 'MANAGER']}>
      <div className="min-h-screen bg-[#F5F5F0]">
        <div className="bg-white border-b border-[#DADCD3] px-8 py-6">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
            <div>
              <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[#526B00] mb-1">
                QUẢN TRỊ · NGƯỜI DÙNG
              </p>
              <h1 className="text-2xl font-bold text-[#111310]">Tài khoản &amp; Phân quyền</h1>
              <p className="text-sm text-[#5E625A] mt-0.5">
                Vai trò áp dụng trong studio hiện tại; tài khoản là danh tính dùng chung
              </p>
            </div>
            <button
              type="button"
              onClick={() => setShowModal(true)}
              className="bg-[#D7FF3F] text-[#111310] font-bold rounded-lg px-4 py-2.5 text-sm hover:bg-[#C4E82E] transition-all shadow-sm"
            >
              + Tạo tài khoản
            </button>
          </div>
        </div>

        <div className="p-8 space-y-6">
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

          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {[
              { label: 'Tổng tài khoản', value: stats.total, hint: 'Trong studio này' },
              { label: 'Đang hoạt động', value: stats.active, hint: 'Có thể đăng nhập' },
              { label: 'Nhân viên', value: stats.staff, hint: 'Quản lý · lễ tân · HLV' },
              { label: 'Hội viên', value: stats.members, hint: 'Dùng cổng hội viên' },
            ].map((item) => (
              <div key={item.label} className="bg-white border border-[#DADCD3] rounded-xl p-5 shadow-sm">
                <p className="text-[11px] font-semibold uppercase tracking-[0.08em] text-[#8A8E85] mb-1.5">
                  {item.label}
                </p>
                <p className="text-3xl font-black text-[#111310]">{item.value}</p>
                <p className="text-xs text-[#5E625A] mt-1">{item.hint}</p>
              </div>
            ))}
          </div>

          <div className="bg-white border border-[#DADCD3] rounded-xl p-4 flex flex-col md:flex-row md:items-center justify-between gap-4 shadow-sm">
            <div className="flex items-center gap-1.5 overflow-x-auto">
              {['ALL', 'ROLE_ADMIN', 'ROLE_MANAGER', 'ROLE_RECEPTIONIST', 'ROLE_TRAINER', 'ROLE_MEMBER'].map(
                (role) => (
                  <button
                    key={role}
                    type="button"
                    onClick={() => setRoleFilter(role)}
                    className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all whitespace-nowrap ${
                      roleFilter === role
                        ? 'bg-[#171A16] text-[#D7FF3F]'
                        : 'text-[#5E625A] hover:bg-[#F5F5F0]'
                    }`}
                  >
                    {role === 'ALL' ? 'Tất cả' : ROLE_LABELS[role]}
                  </button>
                ),
              )}
            </div>
            <input
              type="text"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="Tìm theo tên, email, số điện thoại…"
              className="min-w-[260px] border border-[#DADCD3] rounded-lg px-3.5 py-2 text-xs text-[#111310] placeholder:text-[#8A8E85] focus:outline-none focus:ring-2 focus:ring-[#D7FF3F]"
            />
          </div>

          <div className="bg-white border border-[#DADCD3] rounded-xl overflow-hidden shadow-sm">
            <div className="grid grid-cols-[2fr_1.3fr_1.2fr_1fr_1.6fr] px-6 py-3.5 bg-[#F5F5F0] border-b border-[#DADCD3] text-[11px] font-bold uppercase tracking-[0.08em] text-[#8A8E85]">
              <span>Người dùng</span>
              <span>Vai trò</span>
              <span>Ngày tạo</span>
              <span>Trạng thái</span>
              <span className="text-right">Thao tác</span>
            </div>

            {isLoading ? (
              <div className="p-12 text-center">
                <div className="w-8 h-8 border-2 border-[#171A16] border-t-[#D7FF3F] rounded-full animate-spin mx-auto" />
              </div>
            ) : users.length === 0 ? (
              <div className="p-12 text-center">
                <p className="text-sm font-bold text-[#111310]">Không tìm thấy tài khoản nào</p>
                <p className="text-xs text-[#8A8E85] mt-1">Thử đổi bộ lọc hoặc từ khoá tìm kiếm.</p>
              </div>
            ) : (
              <div className="divide-y divide-[#DADCD3]">
                {users.map((user) => {
                  const primaryRole = user.roles[0] ?? 'ROLE_MEMBER';
                  return (
                    <div
                      key={user.id}
                      className="grid grid-cols-[2fr_1.3fr_1.2fr_1fr_1.6fr] px-6 py-4 items-center hover:bg-[#F9F9F7]"
                    >
                      <div className="flex items-center gap-3 min-w-0">
                        <div className="w-9 h-9 rounded-full bg-[#171A16] text-[#D7FF3F] font-bold text-xs flex items-center justify-center flex-shrink-0">
                          {user.fullName?.[0]?.toUpperCase() ?? 'U'}
                        </div>
                        <div className="min-w-0">
                          <p className="text-sm font-bold text-[#111310] truncate">{user.fullName}</p>
                          <p className="text-[11px] text-[#8A8E85] truncate">{user.email}</p>
                          {user.phone && <p className="text-[10px] text-[#5E625A]">{user.phone}</p>}
                        </div>
                      </div>

                      <div className="flex flex-wrap gap-1">
                        {user.roles.map((role) => (
                          <span
                            key={role}
                            className={`text-[10px] font-semibold px-2 py-0.5 rounded-full ${
                              ROLE_STYLES[role] ?? 'bg-[#F0F0EB] text-[#5E625A]'
                            }`}
                          >
                            {ROLE_LABELS[role] ?? role}
                          </span>
                        ))}
                      </div>

                      <span className="text-xs text-[#5E625A]">{formatDate(user.createdAt)}</span>

                      <span
                        className={`justify-self-start text-[11px] font-semibold px-2.5 py-1 rounded-full border ${
                          user.isActive
                            ? 'bg-emerald-100 text-emerald-800 border-emerald-200'
                            : 'bg-zinc-100 text-zinc-600 border-zinc-300'
                        }`}
                      >
                        {user.isActive ? 'Hoạt động' : 'Đã khoá'}
                      </span>

                      <div className="flex justify-end items-center gap-1.5">
                        <select
                          value={primaryRole}
                          disabled={busyId === user.id}
                          onChange={(event) => changeRole(user, event.target.value)}
                          className="text-[11px] border border-[#DADCD3] rounded-lg px-2 py-1.5 bg-white text-[#111310] disabled:opacity-50"
                          aria-label={`Đổi vai trò của ${user.fullName}`}
                        >
                          {Object.keys(ROLE_LABELS).map((role) => (
                            <option key={role} value={role}>
                              {ROLE_LABELS[role]}
                            </option>
                          ))}
                        </select>
                        <button
                          type="button"
                          onClick={() => toggleActive(user)}
                          disabled={busyId === user.id}
                          className={`text-[11px] font-semibold px-2.5 py-1.5 rounded-lg border disabled:opacity-50 ${
                            user.isActive
                              ? 'border-[#DADCD3] text-red-600 hover:bg-red-50'
                              : 'border-[#DADCD3] text-emerald-700 hover:bg-emerald-50'
                          }`}
                        >
                          {user.isActive ? 'Khoá' : 'Mở'}
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>

        {showModal && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-[#111310]/50 p-4">
            <form onSubmit={submit} className="w-full max-w-md bg-white rounded-2xl shadow-xl overflow-hidden">
              <div className="px-6 py-4 border-b border-[#DADCD3]">
                <h2 className="text-base font-bold text-[#111310]">Tạo tài khoản mới</h2>
                <p className="text-xs text-[#8A8E85] mt-0.5">Tài khoản được gán vai trò trong studio hiện tại</p>
              </div>
              <div className="px-6 py-5 space-y-3">
                <Field label="Họ và tên">
                  <input
                    required
                    value={form.fullName}
                    onChange={(event) => setForm({ ...form, fullName: event.target.value })}
                    className="w-full border border-[#DADCD3] rounded-lg px-3 py-2 text-sm"
                  />
                </Field>
                <Field label="Email">
                  <input
                    required
                    type="email"
                    value={form.email}
                    onChange={(event) => setForm({ ...form, email: event.target.value })}
                    className="w-full border border-[#DADCD3] rounded-lg px-3 py-2 text-sm"
                  />
                </Field>
                <Field label="Số điện thoại">
                  <input
                    value={form.phone}
                    onChange={(event) => setForm({ ...form, phone: event.target.value })}
                    className="w-full border border-[#DADCD3] rounded-lg px-3 py-2 text-sm"
                  />
                </Field>
                <Field label="Mật khẩu ban đầu (≥ 8 ký tự)">
                  <input
                    required
                    minLength={8}
                    type="password"
                    value={form.password}
                    onChange={(event) => setForm({ ...form, password: event.target.value })}
                    className="w-full border border-[#DADCD3] rounded-lg px-3 py-2 text-sm"
                  />
                </Field>
                <Field label="Vai trò">
                  <select
                    value={form.role}
                    onChange={(event) => setForm({ ...form, role: event.target.value })}
                    className="w-full border border-[#DADCD3] rounded-lg px-3 py-2 text-sm bg-white"
                  >
                    {Object.keys(ROLE_LABELS).map((role) => (
                      <option key={role} value={role}>
                        {ROLE_LABELS[role]}
                      </option>
                    ))}
                  </select>
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
                  {saving ? 'Đang tạo…' : 'Tạo tài khoản'}
                </button>
              </div>
            </form>
          </div>
        )}
      </div>
    </RoleGuard>
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
