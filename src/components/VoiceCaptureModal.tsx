import React, { useState } from 'react';
import { Mic, X, MessageSquareText, Sparkles, CheckCircle } from 'lucide-react';
import { parseBankSms } from '../utils/persianUtils';

interface VoiceCaptureModalProps {
  isOpen: boolean;
  onClose: () => void;
  onAddExpenseFromSms: (expense: { title: string; amount: number; bankName: string }) => void;
}

export const VoiceCaptureModal: React.FC<VoiceCaptureModalProps> = ({
  isOpen,
  onClose,
  onAddExpenseFromSms
}) => {
  const [inputText, setInputText] = useState('');
  const [resultMsg, setResultMsg] = useState('');

  if (!isOpen) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!inputText.trim()) return;

    // Try parsing as bank SMS or voice intent
    const parsed = parseBankSms(inputText);
    if (parsed) {
      onAddExpenseFromSms({
        title: parsed.merchant,
        amount: parsed.amount,
        bankName: parsed.bankName
      });
      setResultMsg(`تراکنش ${parsed.merchant} به مبلغ ${parsed.amount} تومان ثبت شد.`);
      setTimeout(() => {
        setResultMsg('');
        setInputText('');
        onClose();
      }, 1500);
    } else {
      setResultMsg('یادداشت صوتی / متنی با موفقیت ذخیره شد.');
      setTimeout(() => {
        setResultMsg('');
        setInputText('');
        onClose();
      }, 1500);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md animate-fadeIn">
      <div className="glass-card rounded-3xl p-6 w-full max-w-lg space-y-6 relative border border-cyan-500/30">
        <button 
          onClick={onClose}
          className="absolute top-5 left-5 text-slate-400 hover:text-white p-1.5 rounded-full bg-slate-800/80"
        >
          <X className="w-5 h-5" />
        </button>

        <div className="flex items-center space-x-3 space-x-reverse">
          <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-cyan-500 to-blue-600 flex items-center justify-center text-white shadow-lg shadow-cyan-500/30">
            <Mic className="w-6 h-6 animate-pulse" />
          </div>
          <div>
            <h3 className="text-lg font-bold text-slate-100">ضبط صوت سریع و اسکن پیامک</h3>
            <p className="text-xs text-cyan-400">شبیه‌ساز ضبط ویس یا چسباندن پیامک بانکی</p>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <textarea
              rows={4}
              value={inputText}
              onChange={e => setInputText(e.target.value)}
              placeholder="متن پیامک بانکی یا یادداشت صوتی خود را اینجا بنویسید (مثلا: خرید از فروشگاه رفاه 500,000 ریال)..."
              className="w-full bg-slate-900 border border-slate-700/80 rounded-2xl p-4 text-sm text-slate-200 focus:outline-none focus:border-cyan-500"
            />
          </div>

          {resultMsg && (
            <div className="p-3 bg-emerald-500/20 border border-emerald-500/30 text-emerald-300 rounded-xl text-xs flex items-center space-x-2 space-x-reverse">
              <CheckCircle className="w-4 h-4" />
              <span>{resultMsg}</span>
            </div>
          )}

          <div className="flex items-center justify-end space-x-3 space-x-reverse">
            <button
              type="button"
              onClick={onClose}
              className="px-5 py-2.5 rounded-xl bg-slate-800 text-slate-300 text-sm hover:bg-slate-700 transition"
            >
              انصراف
            </button>
            <button
              type="submit"
              className="px-6 py-2.5 rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 text-white font-medium text-sm shadow-lg shadow-cyan-500/25 hover:opacity-95 transition"
            >
              پردازش و ثبت هوشمند
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
