'use client';

import { useState } from 'react';
import { sendAiChat, requestAiRecommendation, acceptAiRecommendation, dismissAiRecommendation } from '@/lib/api';

interface Message {
  role: 'user' | 'assistant';
  content: string;
  time?: string;
}

interface RecommendationData {
  id: number;
  title: string;
  changes: string[];
  reason: string;
  status: 'PENDING' | 'ACCEPTED' | 'DISMISSED';
}

const initialMessages: Message[] = [
  {
    role: 'assistant',
    content: "Chào bạn! Tôi là Trợ Lý Thể Lực FITOS AI. Tôi được trang bị thông tin về thông số cơ thể, mục tiêu tập luyện và lịch sử các buổi tập gần nhất của bạn. Bạn cần tư vấn về giáo án tập luyện, kỹ thuật động tác hay chế độ dinh dưỡng hôm nay?",
    time: 'Vừa xong',
  },
];

const quickPrompts = [
  'Đánh giá tiến độ tập luyện của tôi tuần này',
  'Tôi bị mỏi lưng dưới khi Squat, cần khắc phục thế nào?',
  'Tối ưu hóa giáo án tăng cơ ngực và tay sau',
  'Thực đơn dinh dưỡng trước và sau buổi tập',
];

export default function AiCoachPage() {
  const [messages, setMessages] = useState<Message[]>(initialMessages);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [recLoading, setRecLoading] = useState(false);
  const [recommendation, setRecommendation] = useState<RecommendationData | null>({
    id: 101,
    title: 'Đề xuất tối ưu hóa buổi tập Ngực & Tay sau',
    changes: [
      'Tăng thêm 1 set nặng cho bài Bench Press (mốc 85kg x 4 reps)',
      'Thay bài Dumbbell Fly bằng Incline Dumbbell Press để nhấn mạnh ngực trên',
      'Nghỉ 90 giây giữa các hiệp nặng để hồi phục ATP'
    ],
    reason: 'Phân tích dữ liệu 4 tuần gần nhất cho thấy bạn đang chững tạ ở mốc 85kg bài Bench Press.',
    status: 'PENDING'
  });
  const [toast, setToast] = useState<string | null>(null);

  function triggerToast(msg: string) {
    setToast(msg);
    setTimeout(() => setToast(null), 3500);
  }

  async function handleSend(textToSend?: string) {
    const text = (textToSend || input).trim();
    if (!text || loading) return;

    const userMsg: Message = {
      role: 'user',
      content: text,
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    };

    setMessages((prev) => [...prev, userMsg]);
    setInput('');
    setLoading(true);

    try {
      const data = await sendAiChat(text, 1);
      const botMsg: Message = {
        role: 'assistant',
        content: data.reply,
        time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      };
      setMessages((prev) => [...prev, botMsg]);
    } catch {
      const botMsg: Message = {
        role: 'assistant',
        content: 'Xin lỗi, hệ thống AI đang trong giây lát bận rộn. Bạn vui lòng thử lại sau ít giây nhé!',
        time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      };
      setMessages((prev) => [...prev, botMsg]);
    } finally {
      setLoading(false);
    }
  }

  async function handleGetNewRecommendation() {
    setRecLoading(true);
    try {
      const res = await requestAiRecommendation(1, 'MUSCLE_GAIN', 'CHEST', 'INTERMEDIATE');
      const parsed = typeof res.rawJson === 'string' ? JSON.parse(res.rawJson) : res.rawJson;
      setRecommendation({
        id: res.recommendationId || Date.now(),
        title: res.title || 'Đề xuất giáo án tối ưu từ AI',
        changes: parsed.suggested_changes || ['Tăng 1 set bài tập chính', 'Nghỉ 90s giữa các hiệp'],
        reason: parsed.reason || 'Dựa trên phân tích thể trạng gần nhất',
        status: 'PENDING',
      });
      triggerToast('✨ Đã sinh đề xuất mới từ AI Workout Coach!');
    } catch {
      triggerToast('Đã làm mới đề xuất AI (Demo)');
    } finally {
      setRecLoading(false);
    }
  }

  async function handleAcceptRec() {
    if (!recommendation) return;
    try {
      await acceptAiRecommendation(recommendation.id);
    } catch {
      // Demo fallback
    }
    setRecommendation((prev) => (prev ? { ...prev, status: 'ACCEPTED' } : null));
    triggerToast('🎉 Đã chấp thuận! Giáo án đã được tự động cập nhật.');
  }

  async function handleDismissRec() {
    if (!recommendation) return;
    try {
      await dismissAiRecommendation(recommendation.id);
    } catch {
      // Demo fallback
    }
    setRecommendation((prev) => (prev ? { ...prev, status: 'DISMISSED' } : null));
    triggerToast('Đã bỏ qua đề xuất này.');
  }

  return (
    <div className="flex flex-col h-screen bg-[#F5F5F0]">
      {toast && (
        <div className="fixed top-5 right-5 z-50 bg-[#111310] text-[#D7FF3F] px-4 py-2.5 rounded-lg shadow-xl text-sm font-semibold border border-[#D7FF3F]/30 animate-fade-in">
          {toast}
        </div>
      )}

      {/* Header */}
      <div className="bg-white border-b border-[#DADCD3] px-8 py-4 shrink-0 flex items-center justify-between">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.1em] text-[#526B00]">
            MEMBER · HYBRID AI ASSISTANT
          </p>
          <h1 className="text-xl font-bold text-[#111310] flex items-center gap-2">
            AI Workout Coach
            <span className="text-[10px] font-bold bg-[#D7FF3F] text-[#111310] px-2 py-0.5 rounded-full uppercase">
              Online · Context Injected
            </span>
          </h1>
        </div>
        <button
          onClick={handleGetNewRecommendation}
          disabled={recLoading}
          className="text-xs bg-[#111310] hover:bg-black text-[#D7FF3F] font-bold px-3.5 py-2 rounded-xl transition-colors flex items-center gap-1.5 shadow-sm"
        >
          <span>⚡</span> {recLoading ? 'Đang phân tích...' : 'Yêu cầu AI phân tích giáo án'}
        </button>
      </div>

      {/* Chat Area & Recommendations Split */}
      <div className="flex-1 overflow-hidden grid grid-cols-1 lg:grid-cols-12 gap-0">
        {/* Chat Feed */}
        <div className="lg:col-span-8 flex flex-col h-full border-r border-[#DADCD3] bg-[#FAF9F5]">
          {/* Messages scrollable container */}
          <div className="flex-1 overflow-y-auto p-6 space-y-4">
            {messages.map((msg, index) => {
              const isUser = msg.role === 'user';
              return (
                <div
                  key={index}
                  className={`flex ${isUser ? 'justify-end' : 'justify-start'}`}
                >
                  <div
                    className={`max-w-[85%] rounded-2xl p-4 shadow-sm text-sm ${
                      isUser
                        ? 'bg-[#111310] text-white rounded-br-none'
                        : 'bg-white border border-[#DADCD3] text-[#111310] rounded-bl-none'
                    }`}
                  >
                    {!isUser && (
                      <div className="flex items-center gap-1.5 mb-1.5">
                        <span className="w-4 h-4 rounded bg-[#D7FF3F] text-[#111310] text-[9px] font-black flex items-center justify-center">
                          AI
                        </span>
                        <span className="text-[11px] font-bold text-[#526B00]">FITOS AI Coach</span>
                      </div>
                    )}
                    <p className="whitespace-pre-line leading-relaxed">{msg.content}</p>
                    {msg.time && (
                      <p className={`text-[10px] mt-2 text-right ${isUser ? 'text-gray-400' : 'text-gray-400'}`}>
                        {msg.time}
                      </p>
                    )}
                  </div>
                </div>
              );
            })}

            {loading && (
              <div className="flex justify-start">
                <div className="bg-white border border-[#DADCD3] rounded-2xl rounded-bl-none p-4 shadow-sm text-xs text-[#5E625A] flex items-center gap-2">
                  <span className="w-2 h-2 rounded-full bg-[#526B00] animate-ping" />
                  AI Coach đang đối chiếu context thể lực và suy nghĩ...
                </div>
              </div>
            )}
          </div>

          {/* Quick Prompts Strip */}
          <div className="p-3 bg-white border-t border-[#DADCD3] overflow-x-auto flex gap-2">
            {quickPrompts.map((prompt, i) => (
              <button
                key={i}
                onClick={() => handleSend(prompt)}
                className="shrink-0 text-xs bg-[#FAF9F5] hover:bg-[#D7FF3F]/40 border border-[#DADCD3] text-[#111310] px-3 py-1.5 rounded-full font-medium transition-colors"
              >
                {prompt}
              </button>
            ))}
          </div>

          {/* Input Box */}
          <div className="p-4 bg-white border-t border-[#DADCD3] flex gap-2">
            <input
              type="text"
              placeholder="Đặt câu hỏi về bài tập, kỹ thuật, dinh dưỡng..."
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleSend()}
              className="flex-1 border border-[#DADCD3] rounded-xl px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-[#526B00]"
            />
            <button
              onClick={() => handleSend()}
              disabled={loading || !input.trim()}
              className="bg-[#D7FF3F] text-[#111310] font-bold rounded-xl px-5 py-2.5 text-sm hover:opacity-90 disabled:opacity-40 transition-opacity"
            >
              Gửi
            </button>
          </div>
        </div>

        {/* Right Panel: Structured AI Recommendation Card */}
        <div className="lg:col-span-4 p-6 bg-white overflow-y-auto">
          <div className="mb-4">
            <h2 className="text-sm font-bold uppercase tracking-[0.08em] text-[#8A8E85]">
              Đề Xuất Giáo Án Cá Nhân Hóa (Structured Card)
            </h2>
            <p className="text-xs text-[#5E625A] mt-0.5">Sinh tự động bởi LLM + Phân tích thể lực hội viên</p>
          </div>

          {recommendation ? (
            <div
              className={`border rounded-2xl p-5 shadow-sm transition-all ${
                recommendation.status === 'ACCEPTED'
                  ? 'border-emerald-300 bg-emerald-50/50'
                  : recommendation.status === 'DISMISSED'
                  ? 'border-gray-200 bg-gray-50 opacity-60'
                  : 'border-[#526B00]/30 bg-[#FAF9F5]'
              }`}
            >
              <div className="flex items-start justify-between gap-2 mb-3">
                <h3 className="text-sm font-bold text-[#111310]">{recommendation.title}</h3>
                <span
                  className={`text-[10px] font-bold px-2 py-0.5 rounded-full uppercase ${
                    recommendation.status === 'ACCEPTED'
                      ? 'bg-emerald-200 text-emerald-800'
                      : recommendation.status === 'DISMISSED'
                      ? 'bg-gray-200 text-gray-700'
                      : 'bg-[#D7FF3F] text-[#111310]'
                  }`}
                >
                  {recommendation.status}
                </span>
              </div>

              <p className="text-xs text-[#5E625A] mb-4 leading-relaxed">
                {recommendation.reason}
              </p>

              <div className="space-y-2 mb-5">
                <p className="text-xs font-bold text-[#111310] uppercase tracking-wider">
                  Các điều chỉnh gợi ý:
                </p>
                {recommendation.changes.map((change, idx) => (
                  <div key={idx} className="flex items-start gap-2 text-xs text-[#111310]">
                    <span className="text-[#526B00] font-bold">✓</span>
                    <span>{change}</span>
                  </div>
                ))}
              </div>

              {recommendation.status === 'PENDING' ? (
                <div className="flex gap-2">
                  <button
                    onClick={handleAcceptRec}
                    className="flex-1 bg-[#111310] hover:bg-black text-[#D7FF3F] text-xs font-bold py-2 rounded-xl transition-colors"
                  >
                    Chấp nhận áp dụng
                  </button>
                  <button
                    onClick={handleDismissRec}
                    className="px-3 border border-[#DADCD3] text-[#5E625A] hover:bg-gray-100 text-xs font-medium rounded-xl transition-colors"
                  >
                    Bỏ qua
                  </button>
                </div>
              ) : (
                <p className="text-xs font-semibold text-center py-1 text-[#5E625A]">
                  {recommendation.status === 'ACCEPTED'
                    ? '✓ Đã đồng bộ vào kế hoạch tập luyện của bạn'
                    : 'Đã từ chối đề xuất này'}
                </p>
              )}
            </div>
          ) : (
            <div className="text-center py-12 border-2 border-dashed border-[#DADCD3] rounded-2xl bg-[#FAF9F5]">
              <p className="text-2xl mb-1">💡</p>
              <p className="text-xs text-[#5E625A]">Chưa có đề xuất mới. Bấm nút phía trên để yêu cầu AI phân tích!</p>
            </div>
          )}

          <div className="mt-6 p-4 rounded-xl bg-[#FAF9F5] border border-[#DADCD3]">
            <h4 className="text-xs font-bold text-[#111310] mb-1">🛡️ Safety &amp; Guardrails</h4>
            <p className="text-[11px] text-[#5E625A] leading-relaxed">
              Trợ lý AI FITOS được tích hợp bộ lọc danh mục chất cấm và chấn thương. Tuyệt đối không tư vấn steroid, thuốc kê đơn hoặc bài tập vượt ngưỡng an toàn y khoa.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
