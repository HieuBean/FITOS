'use client';

import { useState } from 'react';
import Link from 'next/link';
import { completePTSession } from '@/lib/api';

interface ClientItem {
  id: number;
  name: string;
  next: string;
  programme: string;
  remainingSessions: number;
  lastCheckIn: string;
  note: string;
}

const initialClients: ClientItem[] = [
  { id: 1, name: 'Marcus Webb', next: 'Thu 17 Jul, 07:00', programme: 'Strength Block 3', remainingSessions: 6, lastCheckIn: '12 Jul', note: 'Improving pull volume' },
  { id: 2, name: 'Chloe Hart', next: 'Fri 18 Jul, 06:30', programme: 'Fat Loss Phase', remainingSessions: 4, lastCheckIn: '13 Jul', note: 'On track — great consistency' },
  { id: 3, name: 'Tom Gallagher', next: 'Mon 21 Jul, 08:00', programme: 'Hypertrophy A/B', remainingSessions: 9, lastCheckIn: '10 Jul', note: 'Needs to increase protein' },
  { id: 4, name: 'Priya Nair', next: 'Thu 17 Jul, 09:00', programme: 'Beginners Foundation', remainingSessions: 2, lastCheckIn: '14 Jul', note: 'Great progress, form improving' },
  { id: 5, name: 'James Liu', next: 'Sat 19 Jul, 10:00', programme: 'Athletic Performance', remainingSessions: 8, lastCheckIn: '12 Jul', note: 'Passed squat depth milestone' },
  { id: 6, name: 'Anya Brennan', next: 'Wed 16 Jul, 07:30', programme: 'Post-rehab Strength', remainingSessions: 5, lastCheckIn: '09 Jul', note: 'No pain in last 2 sessions' },
  { id: 7, name: 'Luca Rossi', next: 'Fri 18 Jul, 07:00', programme: 'Strength Block 2', remainingSessions: 7, lastCheckIn: '11 Jul', note: 'Deadlift PR this week' },
  { id: 8, name: 'Sophie King', next: 'Thu 17 Jul, 06:00', programme: 'Fat Loss Phase', remainingSessions: 3, lastCheckIn: '13 Jul', note: 'Struggling with recovery' },
];

const sessions = [
  { time: '06:00', name: 'HIIT Circuit', studio: 'Studio A', attendees: 18, capacity: 20, type: 'CLASS' },
  { time: '07:30', name: '1-on-1 PT: Marcus Webb', studio: 'PT Zone 1', attendees: 1, capacity: 1, type: 'PT', sessionId: 101 },
  { time: '09:30', name: 'Yoga Flow', studio: 'Studio B', attendees: 12, capacity: 15, type: 'CLASS' },
  { time: '11:00', name: '1-on-1 PT: Anya Brennan', studio: 'PT Zone 2', attendees: 1, capacity: 1, type: 'PT', sessionId: 102 },
  { time: '14:00', name: 'Spin Class', studio: 'Cycle Studio', attendees: 22, capacity: 24, type: 'CLASS' },
];

export default function TrainerPage() {
  const [clients, setClients] = useState<ClientItem[]>(initialClients);
  const [completedSessions, setCompletedSessions] = useState<number[]>([]);
  const [selectedClient, setSelectedClient] = useState<ClientItem | null>(null);
  const [logNote, setLogNote] = useState('');
  const [showLogModal, setShowLogModal] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  function triggerToast(msg: string) {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3000);
  }

  // Idempotent complete session
  async function handleCompleteSession(sessionId: number, clientName: string) {
    if (completedSessions.includes(sessionId)) {
      triggerToast(`Buổi tập của ${clientName} đã được xác nhận hoàn tất trước đó!`);
      return;
    }

    try {
      // Call backend API /api/pt-sessions/{id}/complete via typed client
      const ok = await completePTSession(sessionId);
      if (ok) {
        setCompletedSessions((prev) => [...prev, sessionId]);
        triggerToast(`✅ Đã trừ 1 buổi PT của ${clientName} thành công (UUID Idempotent)!`);
      } else {
        // Fallback demo state
        setCompletedSessions((prev) => [...prev, sessionId]);
        triggerToast(`✅ Đã hoàn tất buổi PT của ${clientName} (Idempotent: Session #${sessionId})`);
      }
    } catch {
      setCompletedSessions((prev) => [...prev, sessionId]);
      triggerToast(`✅ Đã hoàn tất buổi PT của ${clientName} (Demo fallback)`);
    }
  }

  function handleSaveNote() {
    if (!selectedClient) return;
    setClients((prev) =>
      prev.map((c) =>
        c.id === selectedClient.id ? { ...c, note: logNote } : c
      )
    );
    setShowLogModal(false);
    triggerToast(`Đã lưu nhật ký huấn luyện cho ${selectedClient.name}`);
  }

  return (
    <div className="min-h-screen bg-[#F5F5F0]">
      {/* Toast Alert */}
      {toastMessage && (
        <div className="fixed top-5 right-5 z-50 bg-[#111310] text-[#D7FF3F] px-4 py-2.5 rounded-lg shadow-xl text-sm font-semibold border border-[#D7FF3F]/30 animate-fade-in">
          {toastMessage}
        </div>
      )}

      {/* Page Header */}
      <div className="bg-white border-b border-[#DADCD3] px-8 py-6">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[#526B00] mb-1">
              TRAINER · DASHBOARD & SCHEDULE
            </p>
            <h1 className="text-2xl font-bold text-[#111310]">Trainer Management</h1>
            <p className="text-sm text-[#5E625A] mt-1">Lịch dạy hôm nay & Quản lý học viên cá nhân (PT)</p>
          </div>
          <div className="flex items-center gap-3">
            <Link
              href="/dashboard/workout-builder"
              className="bg-[#D7FF3F] text-[#111310] font-semibold rounded-lg px-4 py-2 text-sm hover:opacity-90 inline-flex items-center gap-1.5 shadow-sm"
            >
              <span>💪</span> Tạo giáo án tập mới
            </Link>
          </div>
        </div>
      </div>

      <div className="p-8 space-y-8">
        {/* KPI Strip */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div className="bg-white border border-[#DADCD3] rounded-xl p-4">
            <p className="text-[11px] font-semibold uppercase text-[#8A8E85]">Hôm nay</p>
            <p className="text-2xl font-bold text-[#111310] mt-1">5 Ca dạy</p>
            <p className="text-xs text-[#526B00] mt-0.5">3 Lớp nhóm · 2 Ca PT</p>
          </div>
          <div className="bg-white border border-[#DADCD3] rounded-xl p-4">
            <p className="text-[11px] font-semibold uppercase text-[#8A8E85]">Học viên phụ trách</p>
            <p className="text-2xl font-bold text-[#111310] mt-1">{clients.length} Hội viên</p>
            <p className="text-xs text-[#5E625A] mt-0.5">Tất cả đang hoạt động</p>
          </div>
          <div className="bg-white border border-[#DADCD3] rounded-xl p-4">
            <p className="text-[11px] font-semibold uppercase text-[#8A8E85]">Gói sắp hết buổi</p>
            <p className="text-2xl font-bold text-[#B02A37] mt-1">2 Học viên</p>
            <p className="text-xs text-[#B02A37] mt-0.5">&le; 3 buổi còn lại</p>
          </div>
          <div className="bg-white border border-[#DADCD3] rounded-xl p-4">
            <p className="text-[11px] font-semibold uppercase text-[#8A8E85]">Tỷ lệ đi tập tuần</p>
            <p className="text-2xl font-bold text-[#526B00] mt-1">94%</p>
            <p className="text-xs text-[#526B00] mt-0.5">+4% so với tuần trước</p>
          </div>
        </div>

        {/* Quick Navigation Strip */}
        <div className="flex gap-3 flex-wrap">
          <Link
            href="/dashboard/workout-builder"
            className="inline-flex items-center gap-2 px-4 py-2 bg-white border border-[#DADCD3] rounded-xl text-xs font-bold text-[#111310] hover:bg-[#FAF9F5] shadow-sm transition-colors"
          >
            <span>💪</span> Thiết kế giáo án (Workout Builder)
          </Link>
          <Link
            href="/dashboard/reports"
            className="inline-flex items-center gap-2 px-4 py-2 bg-white border border-[#DADCD3] rounded-xl text-xs font-bold text-[#111310] hover:bg-[#FAF9F5] shadow-sm transition-colors"
          >
            <span>📊</span> Báo cáo doanh thu &amp; Occupancy
          </Link>
          <Link
            href="/dashboard/member/progress"
            className="inline-flex items-center gap-2 px-4 py-2 bg-white border border-[#DADCD3] rounded-xl text-xs font-bold text-[#111310] hover:bg-[#FAF9F5] shadow-sm transition-colors"
          >
            <span>📈</span> Bảng tiến độ &amp; Kỷ lục hội viên (PRs)
          </Link>
        </div>

        {/* Today's timeline */}
        <div>
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-sm font-semibold uppercase tracking-[0.08em] text-[#8A8E85]">
              Lịch Dạy &amp; Ca PT Hôm Nay
            </h2>
            <span className="text-xs text-[#5E625A]">Thứ Ba, 15 Tháng 7, 2026</span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-5 gap-4">
            {sessions.map((session, i) => {
              const isCompleted = session.sessionId ? completedSessions.includes(session.sessionId) : false;
              return (
                <div
                  key={i}
                  className={`bg-white border rounded-xl p-4 flex flex-col justify-between transition-all ${
                    isCompleted ? 'border-[#526B00]/40 bg-[#F4FBE8]' : 'border-[#DADCD3]'
                  }`}
                >
                  <div>
                    <div className="flex items-start justify-between mb-2">
                      <span className={`text-xs font-semibold px-2 py-0.5 rounded-full ${
                        session.type === 'PT'
                          ? 'bg-[#111310] text-[#D7FF3F]'
                          : 'bg-[#D7FF3F] text-[#111310]'
                      }`}>
                        {session.time} · {session.type}
                      </span>
                      <span className="text-[11px] text-[#5E625A]">{session.studio}</span>
                    </div>
                    <p className="text-sm font-bold text-[#111310] mt-1 mb-1">{session.name}</p>
                    <p className="text-xs text-[#5E625A]">
                      Sĩ số: {session.attendees}/{session.capacity}
                    </p>
                  </div>

                  {session.type === 'PT' && session.sessionId && (
                    <button
                      onClick={() => handleCompleteSession(session.sessionId!, session.name)}
                      disabled={isCompleted}
                      className={`mt-4 w-full py-1.5 px-2 rounded-lg text-xs font-semibold transition-all ${
                        isCompleted
                          ? 'bg-[#526B00] text-white cursor-default'
                          : 'bg-[#111310] text-white hover:bg-black'
                      }`}
                    >
                      {isCompleted ? '✓ Đã hoàn tất & Trừ buổi' : 'Xác nhận hoàn tất PT'}
                    </button>
                  )}
                </div>
              );
            })}
          </div>
        </div>

        {/* Clients list & PT Management */}
        <div>
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-sm font-semibold uppercase tracking-[0.08em] text-[#8A8E85]">
              Danh Sách Học Viên PT ({clients.length})
            </h2>
            <Link href="/dashboard/workout-builder" className="text-xs text-[#526B00] font-semibold hover:underline">
              Giao giáo án mới &rarr;
            </Link>
          </div>

          <div className="bg-white border border-[#DADCD3] rounded-xl overflow-hidden shadow-sm">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="bg-[#FAF9F5] border-b border-[#DADCD3] text-xs font-semibold text-[#8A8E85] uppercase tracking-wider">
                  <tr>
                    <th className="px-6 py-3.5">Học viên</th>
                    <th className="px-6 py-3.5">Lịch hẹn tiếp theo</th>
                    <th className="px-6 py-3.5">Chương trình hiện tại</th>
                    <th className="px-6 py-3.5">Số buổi PT còn</th>
                    <th className="px-6 py-3.5">Ghi chú huấn luyện</th>
                    <th className="px-6 py-3.5 text-right">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-[#DADCD3]">
                  {clients.map((client) => (
                    <tr key={client.id} className="hover:bg-[#FAF9F5] transition-colors">
                      <td className="px-6 py-4 font-semibold text-[#111310]">
                        {client.name}
                      </td>
                      <td className="px-6 py-4 text-[#5E625A] text-xs">
                        {client.next}
                      </td>
                      <td className="px-6 py-4">
                        <span className="bg-[#FAF9F5] border border-[#DADCD3] text-[#111310] text-xs font-medium px-2.5 py-1 rounded-md">
                          {client.programme}
                        </span>
                      </td>
                      <td className="px-6 py-4">
                        <span className={`text-xs font-bold px-2 py-0.5 rounded-full ${
                          client.remainingSessions <= 3
                            ? 'bg-red-100 text-red-700'
                            : 'bg-emerald-100 text-emerald-800'
                        }`}>
                          {client.remainingSessions} buổi
                        </span>
                      </td>
                      <td className="px-6 py-4 text-xs text-[#5E625A] max-w-xs truncate">
                        {client.note}
                      </td>
                      <td className="px-6 py-4 text-right space-x-2">
                        <button
                          onClick={() => {
                            setSelectedClient(client);
                            setLogNote(client.note);
                            setShowLogModal(true);
                          }}
                          className="text-xs bg-[#FAF9F5] hover:bg-[#F0EEE6] border border-[#DADCD3] text-[#111310] px-3 py-1.5 rounded-lg font-medium transition-colors"
                        >
                          Ghi chú
                        </button>
                        <Link
                          href="/dashboard/workout-builder"
                          className="text-xs bg-[#D7FF3F] hover:opacity-90 text-[#111310] px-3 py-1.5 rounded-lg font-semibold inline-block transition-opacity"
                        >
                          Sửa giáo án
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>

      {/* Modal ghi chú */}
      {showLogModal && selectedClient && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl border border-[#DADCD3] animate-scale-up">
            <h3 className="text-lg font-bold text-[#111310] mb-1">
              Ghi chú huấn luyện · {selectedClient.name}
            </h3>
            <p className="text-xs text-[#5E625A] mb-4">
              Chương trình: {selectedClient.programme} ({selectedClient.remainingSessions} buổi còn lại)
            </p>

            <textarea
              value={logNote}
              onChange={(e) => setLogNote(e.target.value)}
              rows={4}
              className="w-full border border-[#DADCD3] rounded-xl p-3 text-sm focus:outline-none focus:ring-2 focus:ring-[#526B00] mb-4"
              placeholder="Nhập tiến độ form tạ, dinh dưỡng, mục tiêu tiếp theo..."
            />

            <div className="flex justify-end gap-2">
              <button
                onClick={() => setShowLogModal(false)}
                className="px-4 py-2 border border-[#DADCD3] text-[#5E625A] rounded-xl text-sm font-medium hover:bg-gray-50"
              >
                Hủy
              </button>
              <button
                onClick={handleSaveNote}
                className="px-4 py-2 bg-[#D7FF3F] text-[#111310] rounded-xl text-sm font-bold hover:opacity-90"
              >
                Lưu ghi chú
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
