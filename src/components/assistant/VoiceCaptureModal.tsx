import React, { useState, useEffect } from 'react';
import {
  Mic,
  MicOff,
  X,
  Sparkles,
  CheckCircle,
  AlertCircle,
  RefreshCw,
  Coins
} from 'lucide-react';
import { useLifeOs } from '../../context/LifeOsContext';
import { TransactionIntentEngine } from '../../utils/transactionIntentEngine';
import { formatFinancialAmount, currentLocalEpochDay, toPersianDigits } from '../../utils/persianDateUtils';

interface VoiceCaptureModalProps {
  isOpen: boolean;
  onClose: () => void;
}

const VOICE_PRESETS = [
  '۲۵۰ هزار تومن اسنپ دادم',
  'از علی ۱ میلیون تومن قرض گرفتم',
  '۶۰۰ تومن بابت فروش جمینای گرفتم',
  '۵ میلیون به حساب پس‌انداز کارت به کارت کردم',
  '۵۰۰ میلی‌لیتر آب خوردم'
];

export const VoiceCaptureModal: React.FC<VoiceCaptureModalProps> = ({
  isOpen,
  onClose
}) => {
  const { addExpense, addConsumption, preferences } = useLifeOs();

  const [isListening, setIsListening] = useState(false);
  const [transcript, setTranscript] = useState(VOICE_PRESETS[0]);
  const [intentResult, setIntentResult] = useState(() =>
    TransactionIntentEngine.detectIntent(VOICE_PRESETS[0])
  );
  const [savedSuccess, setSavedSuccess] = useState(false);

  useEffect(() => {
    if (isOpen) {
      setSavedSuccess(false);
      setIntentResult(TransactionIntentEngine.detectIntent(transcript));
    }
  }, [isOpen, transcript]);

  if (!isOpen) return null;

  const handleSimulateVoice = (phrase: string) => {
    setIsListening(true);
    setTranscript('در حال شنیدن صدای فارسی...');
    setTimeout(() => {
      setTranscript(phrase);
      setIsListening(false);
      const res = TransactionIntentEngine.detectIntent(phrase);
      setIntentResult(res);
    }, 700);
  };

  const handleApplyAction = () => {
    const todayEpoch = currentLocalEpochDay();

    if (transcript.includes('آب')) {
      const ml = transcript.includes('۵۰۰') ? 500 : 250;
      addConsumption({
        itemName: 'آب آشامیدنی',
        category: 'WATER',
        quantity: ml,
        unit: 'ml',
        timestampMillis: Date.now(),
        localEpochDay: todayEpoch,
        notes: 'ثبت صوتی سریع',
        inputSource: 'VOICE'
      });
      setSavedSuccess(true);
      setTimeout(onClose, 1000);
      return;
    }

    if (intentResult.type !== 'UNKNOWN' && intentResult.amount > 0) {
      addExpense({
        title: intentResult.title || intentResult.category,
        category: intentResult.category,
        amount: intentResult.amount,
        currency: 'TOMAN',
        timestampMillis: Date.now(),
        localEpochDay: todayEpoch,
        notes: `[VOICE] متن صوت: ${transcript}`,
        inputSource: 'VOICE',
        transactionType: intentResult.type,
        paidAmount: intentResult.type === 'DEBT' || intentResult.type === 'RECEIVABLE' ? 0 : intentResult.amount
      });
      setSavedSuccess(true);
      setTimeout(onClose, 1000);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-md">
      <div className="glass-panel w-full max-w-lg rounded-3xl border border-teal-500/30 p-6 space-y-5 shadow-2xl relative overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-xl bg-teal-500/20 text-teal-300 flex items-center justify-center">
              <Mic className="w-4 h-4" />
            </div>
            <div>
              <h3 className="text-sm sm:text-base font-bold text-slate-100">
                شیت دریافت صوتی هوشمند
              </h3>
              <p className="text-xs text-slate-400">
                پشتیبانی کامل از جملات محاوره‌ای فارسی و تبدیل به عمل
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-lg text-slate-400 hover:text-white cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Audio Wave / Mic Pulse Center */}
        <div className="text-center py-4 space-y-3">
          <div className="relative inline-flex items-center justify-center">
            <div className={`w-20 h-20 rounded-full bg-teal-500/20 flex items-center justify-center ${isListening ? 'pulse-glow' : ''}`}>
              <div className="w-14 h-14 rounded-full bg-teal-500 text-slate-950 flex items-center justify-center shadow-lg shadow-teal-500/40">
                <Mic className="w-7 h-7" />
              </div>
            </div>
          </div>

          <p className="text-xs text-teal-300 font-semibold">
            {isListening ? 'در حال دریافت صوت...' : 'متن شناسایی‌شده:'}
          </p>

          <div className="p-3 rounded-2xl bg-slate-900/80 border border-white/10 text-sm font-semibold text-slate-100">
            «{transcript}»
          </div>
        </div>

        {/* Presets to test voice commands */}
        <div className="space-y-1.5">
          <label className="text-[11px] text-slate-400 font-medium">
            تست نمونه جملات صوتی محاوره‌ای:
          </label>
          <div className="flex flex-wrap gap-1.5">
            {VOICE_PRESETS.map((p, idx) => (
              <button
                key={idx}
                onClick={() => handleSimulateVoice(p)}
                className="px-2.5 py-1 text-xs rounded-xl bg-slate-800/80 hover:bg-slate-700 text-slate-300 border border-white/5 cursor-pointer"
              >
                {p}
              </button>
            ))}
          </div>
        </div>

        {/* Live Intent Breakdown */}
        {intentResult.type !== 'UNKNOWN' && (
          <div className="p-3.5 rounded-2xl bg-slate-900/60 border border-teal-500/20 space-y-2 text-xs">
            <div className="flex items-center justify-between">
              <span className="font-semibold text-teal-300 flex items-center gap-1">
                <Sparkles className="w-3.5 h-3.5" />
                تحلیل قصد (TransactionIntentEngine):
              </span>
              <span className="text-[10px] px-2 py-0.5 rounded-full bg-teal-500/20 text-teal-300 font-bold">
                {toPersianDigits(Math.round(intentResult.confidence * 100))}٪ اطمینان
              </span>
            </div>
            <div className="grid grid-cols-2 gap-2 text-slate-300">
              <div>
                نوع: <span className="font-bold text-slate-100">{intentResult.type}</span>
              </div>
              <div>
                مبلغ: <span className="font-bold text-emerald-400">{formatFinancialAmount(intentResult.amount, 'TOMAN')}</span>
              </div>
              <div>
                دسته: <span className="font-bold text-slate-100">{intentResult.category}</span>
              </div>
              {intentResult.person && (
                <div>
                  شخص: <span className="font-bold text-slate-100">{intentResult.person}</span>
                </div>
              )}
            </div>
          </div>
        )}

        {/* Footer Actions */}
        <div className="flex items-center justify-end gap-2 pt-2 border-t border-white/10">
          <button
            onClick={onClose}
            className="px-4 py-2 text-xs text-slate-400 hover:text-white cursor-pointer"
          >
            انصراف
          </button>
          <button
            onClick={handleApplyAction}
            className={`px-5 py-2 rounded-xl text-xs font-bold transition flex items-center gap-1.5 cursor-pointer ${
              savedSuccess
                ? 'bg-emerald-500 text-slate-950'
                : 'glass-button-primary'
            }`}
          >
            <CheckCircle className="w-4 h-4" />
            <span>{savedSuccess ? 'ثبت شد!' : 'تأیید و ذخیره در سیستم'}</span>
          </button>
        </div>
      </div>
    </div>
  );
};
