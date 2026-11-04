'use client';

import { Exercise } from '@/lib/api';

interface ExerciseDetailDrawerProps {
  exercise: Exercise | null;
  isOpen: boolean;
  onClose: () => void;
}

export default function ExerciseDetailDrawer({ exercise, isOpen, onClose }: ExerciseDetailDrawerProps) {
  if (!isOpen || !exercise) return null;

  return (
    <div className="fixed inset-0 z-50 overflow-hidden">
      {/* Backdrop */}
      <div
        onClick={onClose}
        className="absolute inset-0 bg-black/50 backdrop-blur-sm transition-opacity animate-fade-in"
      />

      <div className="fixed inset-y-0 right-0 max-w-full flex pl-10">
        <div className="w-screen max-w-md bg-white shadow-2xl flex flex-col border-l border-[#DADCD3] animate-slide-left">
          {/* Header */}
          <div className="px-6 py-5 bg-[#FAF9F5] border-b border-[#DADCD3] flex items-center justify-between">
            <div>
              <span className="text-[10px] font-bold uppercase tracking-wider text-[#526B00] bg-[#D7FF3F] px-2 py-0.5 rounded-full">
                {exercise.muscleGroup}
              </span>
              <h2 className="text-xl font-black text-[#111310] mt-1">{exercise.name}</h2>
            </div>
            <button
              onClick={onClose}
              className="w-8 h-8 rounded-full bg-white border border-[#DADCD3] text-[#5E625A] hover:text-[#111310] flex items-center justify-center text-sm font-bold shadow-sm"
            >
              ✕
            </button>
          </div>

          {/* Body */}
          <div className="flex-1 overflow-y-auto p-6 space-y-6">
            {/* Video preview / player */}
            <div>
              <p className="text-xs font-bold uppercase tracking-wider text-[#8A8E85] mb-2">
                Video Hướng Dẫn Kỹ Thuật Động Tác
              </p>
              <div className="w-full aspect-video bg-[#111310] rounded-xl overflow-hidden border border-[#DADCD3] relative flex items-center justify-center">
                {exercise.videoUrl ? (
                  <video
                    src={exercise.videoUrl}
                    controls
                    className="w-full h-full object-cover"
                    poster="https://images.unsplash.com/photo-1534438327276-14e5300c3a48?q=80&w=800&auto=format&fit=crop"
                  />
                ) : (
                  <div className="text-center p-4">
                    <span className="text-3xl mb-1 block">🎥</span>
                    <p className="text-xs text-white font-bold">{exercise.name} Execution Guide</p>
                    <p className="text-[10px] text-gray-400 mt-0.5">Mô phỏng 3D form chuẩn khớp và nhịp thở</p>
                  </div>
                )}
              </div>
            </div>

            {/* Badges / Specs */}
            <div className="grid grid-cols-2 gap-3">
              <div className="bg-[#FAF9F5] border border-[#DADCD3] p-3 rounded-xl">
                <span className="text-[10px] text-[#8A8E85] uppercase font-bold block">Thiết bị tập</span>
                <span className="text-xs font-bold text-[#111310] mt-0.5 block">{exercise.equipment || 'Tạ đơn / Tạ đòn'}</span>
              </div>
              <div className="bg-[#FAF9F5] border border-[#DADCD3] p-3 rounded-xl">
                <span className="text-[10px] text-[#8A8E85] uppercase font-bold block">Độ khó</span>
                <span className="text-xs font-bold text-[#526B00] mt-0.5 block">{exercise.difficulty}</span>
              </div>
            </div>

            {/* Description */}
            <div>
              <p className="text-xs font-bold uppercase tracking-wider text-[#8A8E85] mb-1.5">
                Mô tả &amp; Định hướng cơ học
              </p>
              <p className="text-xs text-[#5E625A] leading-relaxed bg-[#FAF9F5] p-3.5 rounded-xl border border-[#DADCD3]">
                {exercise.description || 'Bài tập trọng tâm nhằm kích thích sợi cơ tối đa và tăng cường độ vững chắc của khớp.'}
              </p>
            </div>

            {/* Checklist form cues */}
            <div>
              <p className="text-xs font-bold uppercase tracking-wider text-[#8A8E85] mb-2">
                Quy Tắc Chuẩn Form (Cues)
              </p>
              <div className="space-y-2 text-xs text-[#111310]">
                <div className="flex items-start gap-2 bg-[#FAF9F5] p-2.5 rounded-lg border border-[#DADCD3]">
                  <span className="text-[#526B00] font-black">1.</span>
                  <span><strong>Set-up:</strong> Khóa bả vai về sau và siết chặt cơ bụng (Bracing) trước khi bắt đầu reps đầu tiên.</span>
                </div>
                <div className="flex items-start gap-2 bg-[#FAF9F5] p-2.5 rounded-lg border border-[#DADCD3]">
                  <span className="text-[#526B00] font-black">2.</span>
                  <span><strong>Nhịp thở:</strong> Hít sâu gồng cơ bụng khi hạ tạ (Eccentric), thở ra dứt khoát khi đẩy tạ lên (Concentric).</span>
                </div>
                <div className="flex items-start gap-2 bg-[#FAF9F5] p-2.5 rounded-lg border border-[#DADCD3]">
                  <span className="text-[#526B00] font-black">3.</span>
                  <span><strong>Biên độ:</strong> Di chuyển hết tầm vận động (Full ROM) có kiểm soát, tránh thả tạ tự do làm mất sức căng cơ.</span>
                </div>
              </div>
            </div>
          </div>

          {/* Footer */}
          <div className="p-4 bg-[#FAF9F5] border-t border-[#DADCD3]">
            <button
              onClick={onClose}
              className="w-full bg-[#111310] text-[#D7FF3F] text-xs font-bold py-2.5 rounded-xl hover:bg-black transition-colors"
            >
              Đã hiểu kỹ thuật — Đóng hướng dẫn
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

