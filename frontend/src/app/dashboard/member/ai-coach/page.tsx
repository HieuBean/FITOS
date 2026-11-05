'use client';

import { useState, useRef, useEffect } from 'react';
import { useAuth } from '@/context/AuthContext';
import api from '@/lib/api';

interface Message {
  role: 'ai' | 'user';
  text: string;
  isSafe?: boolean;
  warning?: string | null;
  suggestedActions?: string[];
  timestamp?: string;
}

interface ExerciseChange {
  action: string;
  exercise_name: string;
  replacement_or_modification: string;
  reason: string;
}

interface RecommendationData {
  analysis: string;
  suggested_changes: ExerciseChange[];
  recovery_advice: string;
  confidence_score: number;
}

const QUICK_PROMPTS = [
  '🦵 Lên giáo án tập chân an toàn lưng',
  '💪 Giáo án tập ngực & tay sau hôm nay',
  '⚡ Đang mỏi cơ, có nên tập Active Recovery?',
  '⚠️ Đau khớp gối nên thay bài squat bằng gì?',
  '🚫 Dùng trenbolone có tăng cơ an toàn không?',
];

export default function AiCoachPage() {
  const { user } = useAuth();
  const [messages, setMessages] = useState<Message[]>([
    {
      role: 'ai',
      text: `Chào ${user?.fullName || 'bạn'}! Tôi là FITOS AI Workout Coach. Tôi sẽ dùng thông tin thể lực có trong hồ sơ của bạn để tư vấn. Bạn muốn hỏi về bài tập nào?`,
      isSafe: true,
      timestamp: new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }),
    },
  ]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [recommendation, setRecommendation] = useState<RecommendationData | null>(null);
  const [recommendationError, setRecommendationError] = useState('');
  const [showRecModal, setShowRecModal] = useState(false);
  const [isLoadingRec, setIsLoadingRec] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, isLoading]);

  async function sendMessage(textToSend?: string) {
    const messageText = (textToSend || input).trim();
    if (!messageText || isLoading) return;

    const timeStr = new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });

    // Append user message
    const userMsg: Message = { role: 'user', text: messageText, timestamp: timeStr };
    setMessages((prev) => [...prev, userMsg]);
    if (!textToSend) setInput('');
    setIsLoading(true);

    try {
      const { data } = await api.post('/ai/chat', {
        message: messageText,
        conversation_history: messages.slice(-10).map((m) => ({
          role: m.role === 'ai' ? 'assistant' : 'user',
          content: m.text,
        })),
      });
      setMessages((prev) => [
        ...prev,
        {
          role: 'ai',
          text: data.reply,
          isSafe: data.is_safe,
          warning: data.safety_warning,
          suggestedActions: data.suggested_actions,
          timestamp: new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }),
        },
      ]);
    } catch (err: any) {
      console.error('Lỗi khi gọi AI Workout service:', err);
      setMessages((prev) => [
        ...prev,
        {
          role: 'ai',
          text: 'Không thể gửi câu hỏi lúc này. Vui lòng thử lại sau.',
          isSafe: false,
          timestamp: new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }),
        },
      ]);
    } finally {
      setIsLoading(false);
    }
  }

  async function fetchRecommendation() {
    setIsLoadingRec(true);
    setRecommendation(null);
    setRecommendationError('');
    setShowRecModal(true);
    try {
      const { data } = await api.post('/ai/recommend', {});
      setRecommendation(data);
    } catch (e) {
      console.error('Lỗi khi tải đề xuất giáo án:', e);
      setRecommendationError('Không thể tải đề xuất lúc này. Vui lòng thử lại sau.');
    } finally {
      setIsLoadingRec(false);
    }
  }

  function handleKeyDown(e: React.KeyboardEvent<HTMLInputElement>) {
    if (e.key === 'Enter') sendMessage();
  }

  return (
    <div className="flex flex-col h-[calc(100vh-64px)] bg-[#F5F5F0]">
      {/* Header */}
      <div className="bg-white border-b border-[#DADCD3] px-6 py-3.5 shrink-0 flex items-center justify-between">
        <div>
          <div className="flex items-center gap-2">
            <span className="text-[10px] font-bold uppercase tracking-wider text-[#526B00] bg-[#D7FF3F]/30 px-2 py-0.5 rounded">
              LAYER 4 · AI WORKOUT COACH
            </span>
          </div>
          <h1 className="text-base font-bold text-[#111310] flex items-center gap-2 mt-0.5">
            Huấn luyện viên Thể hình AI (FITOS Coach)
          </h1>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={fetchRecommendation}
            className="flex items-center gap-1.5 bg-[#171A16] hover:bg-black text-[#D7FF3F] text-xs font-bold px-3.5 py-2 rounded-xl transition-all shadow-sm"
          >
            <span>✨</span> Đề xuất Điều chỉnh Giáo án
          </button>
        </div>
      </div>

      {/* Main chat window */}
      <div className="flex-1 overflow-y-auto px-6 py-4 space-y-4">
        {messages.map((m, idx) => (
          <div
            key={idx}
            className={`flex flex-col ${m.role === 'user' ? 'items-end' : 'items-start'} max-w-3xl ${
              m.role === 'user' ? 'ml-auto' : 'mr-auto'
            }`}
          >
            <div className="flex items-center gap-2 mb-1 px-1">
              <span className="text-[11px] font-bold text-[#5E625A]">
                {m.role === 'user' ? (user?.fullName || 'Bạn') : 'FITOS AI Coach'}
              </span>
              {m.timestamp && <span className="text-[10px] text-[#8A8E85]">{m.timestamp}</span>}
            </div>

            <div
              className={`rounded-2xl px-4 py-3 text-sm leading-relaxed max-w-full ${
                m.role === 'user'
                  ? 'bg-[#171A16] text-[#ECEDE6] rounded-tr-xs'
                  : 'bg-white border border-[#DADCD3] text-[#111310] rounded-tl-xs shadow-xs'
              }`}
            >
              {m.text}

              {/* Warning box if unsafe request */}
              {m.warning && (
                <div className="mt-2.5 p-2.5 bg-amber-50 border border-amber-200 rounded-lg text-xs text-amber-900 flex items-start gap-2">
                  <span className="text-sm">⚠️</span>
                  <span>{m.warning}</span>
                </div>
              )}

              {/* Action pills */}
              {m.suggestedActions && m.suggestedActions.length > 0 && (
                <div className="mt-3 pt-2.5 border-t border-gray-100 flex flex-wrap gap-1.5">
                  {m.suggestedActions.map((act, aIdx) => (
                    <button
                      key={aIdx}
                      onClick={() => sendMessage(act)}
                      className="text-[11px] bg-[#F5F5F0] hover:bg-[#D7FF3F] text-[#111310] font-medium px-2.5 py-1 rounded-md border border-[#DADCD3] transition-colors"
                    >
                      {act}
                    </button>
                  ))}
                </div>
              )}
            </div>
          </div>
        ))}

        {isLoading && (
          <div className="flex items-center gap-2 text-xs text-[#5E625A] bg-white border border-[#DADCD3] px-4 py-2.5 rounded-2xl w-fit">
            <div className="w-2 h-2 rounded-full bg-[#526B00] animate-ping" />
            HLV AI đang suy nghĩ và kiểm tra an toàn...
          </div>
        )}
        <div ref={messagesEndRef} />
      </div>

      {/* Quick Prompts Bar */}
      <div className="bg-white border-t border-[#DADCD3] px-6 py-2 flex items-center gap-2 overflow-x-auto shrink-0 scrollbar-none">
        <span className="text-[11px] font-bold text-[#8A8E85] shrink-0 uppercase tracking-wider">Gợi ý nhanh:</span>
        {QUICK_PROMPTS.map((prompt, i) => (
          <button
            key={i}
            onClick={() => sendMessage(prompt)}
            disabled={isLoading}
            className="text-xs bg-[#F5F5F0] hover:bg-[#D7FF3F]/40 border border-[#DADCD3] text-[#111310] px-3 py-1 rounded-full whitespace-nowrap transition-colors disabled:opacity-40"
          >
            {prompt}
          </button>
        ))}
      </div>

      {/* Input bar */}
      <div className="bg-white border-t border-[#DADCD3] p-4 flex gap-3 shrink-0">
        <input
          type="text"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          disabled={isLoading}
          placeholder="Hỏi HLV AI về kỹ thuật bài tập, điều chỉnh mức tạ, đau mỏi cơ..."
          className="flex-1 border border-[#DADCD3] rounded-xl px-4 py-2.5 text-sm text-[#111310] bg-[#F5F5F0] focus:bg-white focus:outline-none focus:ring-2 focus:ring-[#D7FF3F] focus:border-transparent transition-all"
        />
        <button
          onClick={() => sendMessage()}
          disabled={!input.trim() || isLoading}
          className="bg-[#D7FF3F] text-[#111310] font-bold rounded-xl px-6 py-2.5 text-sm hover:opacity-90 disabled:opacity-40 disabled:cursor-not-allowed transition-all shadow-sm"
        >
          Gửi
        </button>
      </div>

      {/* Recommendation Modal / Drawer */}
      {showRecModal && (
        <div className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4 backdrop-blur-sm">
          <div className="bg-white rounded-2xl max-w-2xl w-full p-6 shadow-2xl border border-[#DADCD3] max-h-[85vh] overflow-y-auto">
            <div className="flex items-center justify-between pb-4 border-b border-[#DADCD3]">
              <div>
                <span className="text-[11px] font-bold text-[#526B00] uppercase tracking-wider bg-[#D7FF3F]/40 px-2 py-0.5 rounded">
                  TASK #123 · STRUCTURED RECOMMENDATION
                </span>
                <h3 className="text-lg font-bold text-[#111310] mt-1">Đề xuất Tối ưu Giáo án Thể lực</h3>
              </div>
              <button
                onClick={() => setShowRecModal(false)}
                className="w-8 h-8 rounded-full bg-gray-100 hover:bg-gray-200 text-gray-700 flex items-center justify-center font-bold"
              >
                ✕
              </button>
            </div>

            {isLoadingRec ? (
              <div className="py-12 text-center text-sm text-[#5E625A]">
                <div className="w-8 h-8 border-4 border-[#D7FF3F] border-t-black rounded-full animate-spin mx-auto mb-3" />
                Đang tải đề xuất...
              </div>
            ) : recommendationError ? (
              <p className="py-8 text-sm text-red-700">{recommendationError}</p>
            ) : recommendation ? (
              <div className="mt-4 space-y-4">
                <div className="bg-[#F5F5F0] p-4 rounded-xl border border-[#DADCD3]">
                  <p className="text-xs font-bold uppercase tracking-wider text-[#526B00] mb-1">
                    🔍 Đánh giá Cơ sinh học:
                  </p>
                  <p className="text-sm text-[#111310] leading-relaxed">{recommendation.analysis}</p>
                </div>

                <div>
                  <p className="text-xs font-bold uppercase tracking-wider text-[#111310] mb-2">
                    📋 Danh sách bài tập điều chỉnh (Suggested Changes):
                  </p>
                  <div className="space-y-2">
                    {recommendation.suggested_changes.map((item, idx) => (
                      <div key={idx} className="bg-white border border-[#DADCD3] p-3.5 rounded-xl shadow-xs">
                        <div className="flex items-center justify-between">
                          <span className="text-xs font-bold text-red-600 line-through">
                            ❌ {item.exercise_name}
                          </span>
                          <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-emerald-100 text-emerald-800">
                            {item.action}
                          </span>
                        </div>
                        <div className="text-sm font-semibold text-[#111310] mt-1.5 flex items-center gap-1.5">
                          <span>➔ Thay bằng:</span>
                          <span className="text-[#526B00] font-bold">{item.replacement_or_modification}</span>
                        </div>
                        <p className="text-xs text-[#5E625A] mt-1">
                          💡 <em>Lý do an toàn: {item.reason}</em>
                        </p>
                      </div>
                    ))}
                  </div>
                </div>

                <div className="bg-emerald-50 border border-emerald-200 p-3.5 rounded-xl text-xs text-emerald-900">
                  <strong>🌿 Lời khuyên phục hồi cơ bắp:</strong> {recommendation.recovery_advice}
                </div>

                <div className="flex justify-end pt-2">
                  <button
                    onClick={() => setShowRecModal(false)}
                    className="bg-[#171A16] text-[#D7FF3F] text-xs font-bold px-5 py-2.5 rounded-lg hover:bg-black"
                  >
                    Đóng
                  </button>
                </div>
              </div>
            ) : null}
          </div>
        </div>
      )}
    </div>
  );
}
