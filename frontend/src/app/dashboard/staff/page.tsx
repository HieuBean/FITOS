'use client';

import { useCallback, useEffect, useState } from 'react';
import api from '@/lib/api';
import RoleGuard from '@/components/RoleGuard';

interface Receptionist { id: number; fullName: string; email: string; phone: string | null; isActive: boolean }
interface Lead { id: number; fullName: string; assignedToUserId: number | null }
interface Trainer {
  id: number; fullName: string; phone: string; email: string | null;
  specialization: string | null; experienceYears: number; isActive: boolean;
}
interface TrainerForm { fullName: string; phone: string; email: string; specialization: string; experienceYears: string; isActive: boolean }
const emptyForm: TrainerForm = { fullName: '', phone: '', email: '', specialization: '', experienceYears: '1', isActive: true };

export default function StaffPage() {
  const [receptionists, setReceptionists] = useState<Receptionist[]>([]);
  const [trainers, setTrainers] = useState<Trainer[]>([]);
  const [leads, setLeads] = useState<Lead[]>([]);
  const [leadId, setLeadId] = useState('');
  const [receptionistId, setReceptionistId] = useState('');
  const [assigning, setAssigning] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [editing, setEditing] = useState<number | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [form, setForm] = useState<TrainerForm>(emptyForm);
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [staff, coaches, leadPage] = await Promise.all([
        api.get<Receptionist[]>('/staff/receptionists'),
        api.get<Trainer[]>('/trainers', { params: { activeOnly: false } }),
        api.get<{ content: Lead[] }>('/leads', { params: { size: 100, sort: 'createdAt,desc' } }),
      ]);
      setReceptionists(staff.data);
      setTrainers(coaches.data);
      setLeads(leadPage.data.content ?? []);
    } catch { setError('Không tải được danh sách nhân sự.'); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { void load(); }, [load]);

  const openNew = () => { setEditing(null); setForm(emptyForm); setFormOpen(true); setError(null); };
  const openEdit = (trainer: Trainer) => {
    setEditing(trainer.id);
    setForm({ fullName: trainer.fullName, phone: trainer.phone, email: trainer.email ?? '', specialization: trainer.specialization ?? '', experienceYears: String(trainer.experienceYears ?? 1), isActive: trainer.isActive });
    setFormOpen(true); setError(null);
  };
  const save = async (event: React.FormEvent) => {
    event.preventDefault(); setSaving(true); setError(null);
    const payload = { ...form, email: form.email || null, specialization: form.specialization || null, experienceYears: Number(form.experienceYears) || 1 };
    try {
      if (editing) await api.put(`/trainers/${editing}`, payload);
      else await api.post('/trainers', payload);
      setFormOpen(false); await load();
    } catch { setError('Không lưu được hồ sơ huấn luyện viên.'); }
    finally { setSaving(false); }
  };

  const assignLead = async (event: React.FormEvent) => {
    event.preventDefault(); setAssigning(true); setError(null);
    try {
      await api.patch(`/leads/${leadId}/assign`, { assignedToUserId: Number(receptionistId) });
      await load();
      setLeadId(''); setReceptionistId('');
    } catch { setError('Không phân công được khách tiềm năng.'); }
    finally { setAssigning(false); }
  };

  return <RoleGuard allowedRoles={['MANAGER']}>
    <div className="min-h-screen bg-[#F5F5F0]">
      <header className="border-b border-[#DADCD3] bg-white px-6 py-6 md:px-8">
        <p className="text-xs font-semibold uppercase tracking-widest text-[#526B00]">Quản lý studio</p>
        <h1 className="mt-1 text-2xl font-bold">Lễ tân & Huấn luyện viên</h1>
        <p className="mt-1 text-sm text-[#5E625A]">Theo dõi nhân sự và cập nhật hồ sơ HLV. Tài khoản đăng nhập do Admin quản trị.</p>
      </header>
      <main className="space-y-6 p-6 md:p-8">
        {loading && <p role="status">Đang tải nhân sự…</p>}
        {error && <p role="alert" className="rounded-lg bg-red-50 p-4 text-red-700">{error}</p>}
        {!loading && <>
          <section className="rounded-xl border border-[#DADCD3] bg-white p-5" aria-labelledby="reception-title">
            <h2 id="reception-title" className="text-lg font-bold">Lễ tân ({receptionists.length})</h2>
            {receptionists.length === 0 ? <p className="mt-3 text-sm text-[#5E625A]">Chưa có nhân viên lễ tân trong studio.</p> :
              <ul className="mt-3 divide-y divide-[#DADCD3]">{receptionists.map((person) => <li key={person.id} className="flex flex-wrap items-center justify-between gap-2 py-3 text-sm"><div><p className="font-semibold">{person.fullName}</p><p className="text-[#5E625A]">{person.email} · {person.phone || 'Chưa có số điện thoại'}</p></div><span className={person.isActive ? 'text-emerald-700' : 'text-red-700'}>{person.isActive ? 'Tài khoản hoạt động' : 'Đã khóa tài khoản'}</span></li>)}</ul>}
            {leads.length > 0 && receptionists.some((person) => person.isActive) && <form onSubmit={assignLead} className="mt-5 flex flex-wrap items-end gap-3 border-t border-[#DADCD3] pt-4">
              <label className="text-sm">Phân công khách tiềm năng (100 khách mới nhất)<select required value={leadId} onChange={(event) => setLeadId(event.target.value)} className="mt-1 block min-h-11 rounded-lg border border-[#DADCD3] bg-white px-3"><option value="">Chọn khách</option>{leads.map((lead) => <option key={lead.id} value={lead.id}>{lead.fullName}</option>)}</select></label>
              <label className="text-sm">Lễ tân phụ trách<select required value={receptionistId} onChange={(event) => setReceptionistId(event.target.value)} className="mt-1 block min-h-11 rounded-lg border border-[#DADCD3] bg-white px-3"><option value="">Chọn lễ tân</option>{receptionists.filter((person) => person.isActive).map((person) => <option key={person.id} value={person.id}>{person.fullName}</option>)}</select></label>
              <button type="submit" disabled={assigning} className="min-h-11 rounded-lg bg-[#D7FF3F] px-4 font-semibold disabled:opacity-50">{assigning ? 'Đang phân công…' : 'Phân công'}</button>
            </form>}
          </section>
          <section className="rounded-xl border border-[#DADCD3] bg-white p-5" aria-labelledby="trainer-title">
            <div className="flex flex-wrap items-center justify-between gap-3"><h2 id="trainer-title" className="text-lg font-bold">Huấn luyện viên ({trainers.length})</h2><button type="button" onClick={openNew} className="min-h-11 rounded-lg bg-[#D7FF3F] px-4 font-semibold">Thêm hồ sơ HLV</button></div>
            {trainers.length === 0 ? <p className="mt-3 text-sm text-[#5E625A]">Chưa có hồ sơ HLV.</p> :
              <ul className="mt-3 divide-y divide-[#DADCD3]">{trainers.map((trainer) => <li key={trainer.id} className="flex flex-wrap items-center justify-between gap-3 py-3 text-sm"><div><p className="font-semibold">{trainer.fullName}</p><p className="text-[#5E625A]">{trainer.specialization || 'Chưa ghi chuyên môn'} · {trainer.phone}</p><p className="text-xs text-[#6B6F66]">{trainer.isActive ? 'Đang hoạt động' : 'Ngừng hoạt động'}</p></div><button type="button" onClick={() => openEdit(trainer)} className="min-h-11 rounded-lg border border-[#DADCD3] px-4 font-semibold">Sửa hồ sơ</button></li>)}</ul>}
          </section>
        </>}
      </main>
      {formOpen && <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"><form onSubmit={save} role="dialog" aria-modal="true" aria-labelledby="trainer-form-title" className="max-h-[90vh] w-full max-w-md space-y-3 overflow-y-auto rounded-2xl bg-white p-6">
        <h2 id="trainer-form-title" className="text-lg font-bold">{editing ? 'Sửa hồ sơ HLV' : 'Thêm hồ sơ HLV'}</h2>
        {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
        <label className="block text-sm">Họ tên<input required value={form.fullName} onChange={(event) => setForm({ ...form, fullName: event.target.value })} className="mt-1 w-full rounded-lg border p-3" /></label>
        <label className="block text-sm">Số điện thoại<input required value={form.phone} onChange={(event) => setForm({ ...form, phone: event.target.value })} className="mt-1 w-full rounded-lg border p-3" /></label>
        <label className="block text-sm">Email<input type="email" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} className="mt-1 w-full rounded-lg border p-3" /></label>
        <label className="block text-sm">Chuyên môn<input value={form.specialization} onChange={(event) => setForm({ ...form, specialization: event.target.value })} className="mt-1 w-full rounded-lg border p-3" /></label>
        <label className="block text-sm">Số năm kinh nghiệm<input type="number" min="0" max="80" value={form.experienceYears} onChange={(event) => setForm({ ...form, experienceYears: event.target.value })} className="mt-1 w-full rounded-lg border p-3" /></label>
        <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={form.isActive} onChange={(event) => setForm({ ...form, isActive: event.target.checked })} /> Đang hoạt động</label>
        <div className="flex justify-end gap-2 pt-2"><button type="button" onClick={() => setFormOpen(false)} className="min-h-11 px-4">Hủy</button><button type="submit" disabled={saving} className="min-h-11 rounded-lg bg-[#D7FF3F] px-4 font-semibold disabled:opacity-50">{saving ? 'Đang lưu…' : 'Lưu hồ sơ'}</button></div>
      </form></div>}
    </div>
  </RoleGuard>;
}
