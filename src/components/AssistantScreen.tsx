import React, { useState } from 'react';
import { Bot, Send, Sparkles, User, Loader2 } from 'lucide-react';
import { ChatMessage } from '../types';
import { GoogleGenAI } from '@google/genai';

interface AssistantScreenProps {
  messages: ChatMessage[];
  onSendMessage: (userText: string, aiText: string) => void;
}

export const AssistantScreen: React.FC<AssistantScreenProps> = ({
  messages,
  onSendMessage
}) => {
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleSend = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!input.trim() || isLoading) return;

    const userText = input;
    setInput('');
    setIsLoading(true);

    let aiResponseText = '';

    try {
      const metaEnv = (import.meta as unknown as { env?: Record<string, string | undefined> }).env;
      const apiKey = metaEnv?.VITE_GEMINI_API_KEY || '';
      if (apiKey) {
        const ai = new GoogleGenAI({ apiKey });
        const response = await ai.models.generateContent({
          model: 'gemini-2.5-flash',
          contents: [
            {
              role: 'user',
              parts: [{ text: `You are Personal Life OS AI Assistant in Persian. Respond helpfully in Persian to: ${userText}` }]
            }
          ]
        });
        aiResponseText = response.text || 'پاسخی دریافت نشد.';
      } else {
        // Intelligent fallback simulation matching Personal Life OS assistant
        await new Promise(r => setTimeout(r, 800));
        if (userText.includes('بودجه') || userText.includes('هزینه')) {
          aiResponseText = 'بر اساس تحلیل تراکنش‌های شما در ماه آبان، بیشترین هزینه‌ها در بخش خرید عمومی بوده است. پیشنهاد می‌کنم بودجه‌بندی هفتگی را فعال کنید.';
        } else if (userText.includes('کار') || userText.includes('وظیفه')) {
          aiResponseText = 'شما ۳ وظیفه مهم با اولویت بالا دارید. پیشنهاد می‌کنم ابتدا گزارش مالی را تکمیل کنید.';
        } else {
          aiResponseText = `من دستیار هوشمند Life OS شما هستم. به درخواست شما («${userText}») رسیدگی شد. همه چیز تحت کنترل است و برنامه‌ها به خوبی پیش می‌روند! 🌟`;
        }
      }
    } catch (err) {
      aiResponseText = 'خطا در ارتباط با هوش مصنوعی. لطفا مجددا تلاش کنید.';
    } finally {
      setIsLoading(false);
      onSendMessage(userText, aiResponseText);
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6 animate-fadeIn h-[calc(100vh-140px)] flex flex-col">
      <div>
        <h2 className="text-2xl font-bold text-slate-100 flex items-center space-x-2 space-x-reverse">
          <Bot className="w-7 h-7 text-cyan-400" />
          <span>دستیار هوشمند جمینای (Gemini AI Assistant)</span>
        </h2>
        <p className="text-xs text-slate-400">مشاوره هوشمند شخصی، تحلیل برنامه‌ها، وظایف و پیشنهادهای بهینه‌سازی زندگی</p>
      </div>

      {/* Chat History Box */}
      <div className="flex-1 glass-card rounded-2xl p-4 overflow-y-auto space-y-4">
        {messages.length === 0 ? (
          <div className="text-center py-16 text-slate-500 space-y-2">
            <Sparkles className="w-10 h-10 mx-auto text-cyan-400 opacity-40 animate-pulse" />
            <p className="text-sm font-medium">گفتگو با دستیار هوشمند را آغاز کنید...</p>
            <p className="text-xs text-slate-600">مثلا بپرسید: «وضعیت هزینه‌های این ماهم چطور است؟» یا «برای افزایش بهره‌وری چه کنم؟»</p>
          </div>
        ) : (
          messages.map(msg => (
            <div 
              key={msg.id}
              className={`flex items-start space-x-3 space-x-reverse ${
                msg.sender === 'user' ? 'flex-row-reverse space-x-reverse' : ''
              }`}
            >
              <div className={`w-9 h-9 rounded-xl flex items-center justify-center shrink-0 ${
                msg.sender === 'user' ? 'bg-cyan-500 text-slate-950 font-bold' : 'bg-blue-600/30 text-cyan-400 border border-cyan-500/30'
              }`}>
                {msg.sender === 'user' ? <User className="w-5 h-5" /> : <Bot className="w-5 h-5" />}
              </div>

              <div className={`max-w-[80%] rounded-2xl p-4 text-sm leading-relaxed ${
                msg.sender === 'user' 
                  ? 'bg-cyan-500 text-slate-950 font-medium rounded-tl-none' 
                  : 'bg-slate-900 border border-slate-800 text-slate-200 rounded-tr-none'
              }`}>
                <p className="whitespace-pre-wrap">{msg.text}</p>
                <span className={`text-[10px] block mt-1.5 ${msg.sender === 'user' ? 'text-slate-800' : 'text-slate-500'}`}>
                  {msg.timestamp}
                </span>
              </div>
            </div>
          ))
        )}

        {isLoading && (
          <div className="flex items-center space-x-3 space-x-reverse">
            <div className="w-9 h-9 rounded-xl bg-blue-600/30 text-cyan-400 flex items-center justify-center border border-cyan-500/30">
              <Bot className="w-5 h-5" />
            </div>
            <div className="glass-card rounded-2xl p-4 text-sm text-cyan-400 flex items-center space-x-2 space-x-reverse">
              <Loader2 className="w-4 h-4 animate-spin" />
              <span>جمینای در حال پردازش و تفکر...</span>
            </div>
          </div>
        )}
      </div>

      {/* Input Form */}
      <form onSubmit={handleSend} className="glass-card rounded-2xl p-2 flex items-center gap-2">
        <input 
          type="text"
          value={input}
          onChange={e => setInput(e.target.value)}
          placeholder="پیام خود را به دستیار هوشمند بنویسید..."
          className="flex-1 bg-transparent border-none px-4 py-2.5 text-sm text-slate-200 focus:outline-none"
        />
        <button
          type="submit"
          disabled={isLoading || !input.trim()}
          className="p-3 rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 text-white shadow-lg shadow-cyan-500/20 hover:opacity-90 disabled:opacity-50 transition"
        >
          <Send className="w-4 h-4" />
        </button>
      </form>
    </div>
  );
};
