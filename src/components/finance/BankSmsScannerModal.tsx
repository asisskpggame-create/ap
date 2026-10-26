import React, { useState } from 'react';
import {
  X,
  CreditCard,
  CheckCircle,
  AlertTriangle,
  ShieldAlert,
  ArrowDownLeft,
  ArrowUpRight,
  RefreshCw,
  Copy,
  Sparkles
} from 'lucide-react';
import { useLifeOs } from '../../context/LifeOsContext';
import { BankSmsParser } from '../../utils/bankSmsParser';
import { BankSmsParseResult } from '../../types/lifeOs';
import { formatFinancialAmount, toPersianDigits } from '../../utils/persianDateUtils';

interface BankSmsScannerModalProps {
  isOpen: boolean;
  onClose: () => void;
}

const SAMPLE_SMS_PRESETS = [
  {
    name: 'بانک ملت (خرید پوز)',
    text: `بانک ملت
برداشت مبلغ 850,000 ریال
از کارت: 610433***5432
مانده: 14,200,000 ریال
پیگیری: 894215
1405/08/05 14:20`
  },
  {
    name: 'بلوبانک (واریز حقوق)',
    text: `بلوبانک سامان
واریز 28,500,000 تومان بابت حقوق و دستمزد
به حساب *9812
موجودی: 35,400,000 تومان
کد پیگیری: BLU9827361
1405/08/01`
  },
  {
    name: 'بانک ملی (انتقال وجه)',
    text: `بانک ملی ایران
انتقال وجه 3,000,000 ریال
از حساب 0102***89
مانده حساب: 45,000,000 ریال
شماره ارجاع: 7654321`
  },
  {
    name: 'پیامک رمز پویا (OTP)',
    text: `رمز پویا بانک ملت: 489215
معتبر به مدت 120 ثانیه برای خرید اینترنتی
کارت: *5432`
  },
  {
    name: 'تراکنش ناموفق',
    text: `بانک صادرات ایران
تراکنش ناموفق
موجودی ناکافی در کارت *1122
مبلغ: 500,000 ریال`
  }
];

export const BankSmsScannerModal: React.FC<BankSmsScannerModalProps> = ({
  isOpen,
  onClose
}) => {
  const { addExpense, expenses } = useLifeOs();
  const [smsText, setSmsText] = useState(SAMPLE_SMS_PRESETS[0].text);
  const [parseResult, setParseResult] = useState<BankSmsParseResult>(() =>
    BankSmsParser.parse(SAMPLE_SMS_PRESETS[0].text)
  );
  const [savedSuccess, setSavedSuccess] = useState(false);

  if (!isOpen) return null;

  const handleTextChange = (text: string) => {
    setSmsText(text);
    setSavedSuccess(false);
    const result = BankSmsParser.parse(text);
    setParseResult(result);
  };

  const handleSelectPreset = (presetText: string) => {
    handleTextChange(presetText);
  };

  const handleCommitTransaction = () => {
    if (parseResult.status !== 'SUCCESS' && parseResult.status !== 'NEEDS_REVIEW') return;
    if (parseResult.amountInToman <= 0) return;

    // Check if duplicate fingerprint exists
    const isDuplicate = expenses.some(
      e => e.idempotencyKey && e.idempotencyKey === parseResult.fingerprint
    );

    if (isDuplicate) {
      alert('این پیامک قبلاً به عنوان تراکنش ثبت شده است (جلوگیری از ثبت تکراری).');
      return;
    }

    addExpense({
      title: parseResult.suggestedTitle,
      category: parseResult.suggestedCategory || 'خرید روزمره',
      amount: parseResult.amountInToman,
      currency: 'TOMAN',
      timestampMillis: parseResult.timestampMillis,
      localEpochDay: Math.floor(parseResult.timestampMillis / 86400000),
      notes: `[SMS_PARSER] ${parseResult.bankName} - کارت: ${parseResult.cardOrAccountMask || 'نامشخص'} - پیگیری: ${parseResult.trackingCode || 'ندارد'}`,
      inputSource: 'SMS_SCANNER',
      idempotencyKey: parseResult.fingerprint,
      transactionType: parseResult.transactionType === 'INCOME' ? 'INCOME' : parseResult.transactionType === 'TRANSFER' ? 'TRANSFER' : 'EXPENSE',
      paidAmount: parseResult.amountInToman,
      trackingCode: parseResult.trackingCode
    });

    setSavedSuccess(true);
    setTimeout(() => {
      onClose();
    }, 1200);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-md">
      <div className="glass-panel w-full max-w-xl rounded-2xl border border-white/10 shadow-2xl overflow-hidden max-h-[92vh] flex flex-col">
        {/* Header */}
        <div className="px-5 py-4 border-b border-white/10 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-teal-500/20 text-teal-300 flex items-center justify-center">
              <CreditCard className="w-4 h-4" />
            </div>
            <div>
              <h2 className="text-sm sm:text-base font-bold text-slate-100">
                موتور هوشمند پارسر پیامک بانکی
              </h2>
              <p className="text-xs text-slate-400">
                تشخیص خودکار بانک‌های ملت، ملی، بلو، سامان، پاسارگاد و... (۱۰۰٪ آفلاین)
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-white/5 transition cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="p-5 space-y-4 overflow-y-auto">
          {/* Preset Buttons */}
          <div>
            <label className="text-xs font-semibold text-slate-300 block mb-2">
              نمونه‌های آماده تست بانک‌های ایران:
            </label>
            <div className="flex flex-wrap gap-1.5">
              {SAMPLE_SMS_PRESETS.map((p, idx) => (
                <button
                  key={idx}
                  onClick={() => handleSelectPreset(p.text)}
                  className="px-2.5 py-1 text-xs rounded-lg bg-slate-800/80 hover:bg-slate-700 text-slate-300 border border-white/5 hover:border-teal-500/30 transition cursor-pointer"
                >
                  {p.name}
                </button>
              ))}
            </div>
          </div>

          {/* SMS Textarea */}
          <div>
            <label className="text-xs font-semibold text-slate-300 block mb-1.5">
              متن پیامک بانکی:
            </label>
            <textarea
              rows={4}
              value={smsText}
              onChange={e => handleTextChange(e.target.value)}
              placeholder="متن پیامک تراکنش بانک را اینجا پیست کنید..."
              className="w-full rounded-xl bg-slate-900/80 border border-white/10 p-3 text-xs sm:text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:border-teal-500/50 font-sans"
            />
          </div>

          {/* Parsed Result Card */}
          <div className="rounded-xl border border-white/10 p-4 bg-slate-900/50 space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-slate-400 flex items-center gap-1.5">
                <Sparkles className="w-3.5 h-3.5 text-teal-400" />
                نتیجه تحلیل موتور پارسر:
              </span>
              <span
                className={`text-[11px] px-2 py-0.5 rounded-full font-medium ${
                  parseResult.status === 'SUCCESS'
                    ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
                    : parseResult.status === 'NEEDS_REVIEW'
                    ? 'bg-amber-500/20 text-amber-300 border border-amber-500/30'
                    : parseResult.status === 'IGNORED_OTP'
                    ? 'bg-blue-500/20 text-blue-300 border border-blue-500/30'
                    : 'bg-rose-500/20 text-rose-300 border border-rose-500/30'
                }`}
              >
                {parseResult.status === 'SUCCESS' && 'تراکنش معتبر'}
                {parseResult.status === 'NEEDS_REVIEW' && 'نیاز به بررسی'}
                {parseResult.status === 'IGNORED_OTP' && 'رمز پویا نادیده گرفته شد'}
                {parseResult.status === 'IGNORED_NON_FINANCIAL' && 'غیرمالی / ناموفق'}
                {parseResult.status === 'UNRECOGNIZED' && 'شناسایی نشد'}
              </span>
            </div>

            {/* If Valid Financial Transaction */}
            {(parseResult.status === 'SUCCESS' || parseResult.status === 'NEEDS_REVIEW') && (
              <div className="grid grid-cols-2 gap-2 text-xs">
                <div className="p-2 rounded-lg bg-slate-800/40 border border-white/5">
                  <span className="text-slate-400 block text-[10px]">بانک صادرکننده:</span>
                  <span className="font-semibold text-slate-200 mt-0.5 block">
                    {parseResult.bankName}
                  </span>
                </div>
                <div className="p-2 rounded-lg bg-slate-800/40 border border-white/5">
                  <span className="text-slate-400 block text-[10px]">نوع تراکنش:</span>
                  <span className="font-semibold text-teal-300 mt-0.5 flex items-center gap-1">
                    {parseResult.transactionType === 'INCOME' ? (
                      <>
                        <ArrowDownLeft className="w-3 h-3 text-emerald-400" />
                        واریز (درآمد)
                      </>
                    ) : parseResult.transactionType === 'EXPENSE' ? (
                      <>
                        <ArrowUpRight className="w-3 h-3 text-rose-400" />
                        برداشت (هزینه)
                      </>
                    ) : (
                      <>
                        <RefreshCw className="w-3 h-3 text-cyan-400" />
                        انتقال وجه
                      </>
                    )}
                  </span>
                </div>
                <div className="p-2 rounded-lg bg-slate-800/40 border border-white/5 col-span-2">
                  <span className="text-slate-400 block text-[10px]">مبلغ خالص (تومان):</span>
                  <div className="flex items-baseline justify-between mt-0.5">
                    <span className="text-base font-bold text-slate-100">
                      {formatFinancialAmount(parseResult.amountInToman, 'TOMAN')}
                    </span>
                    {parseResult.wasInRial && (
                      <span className="text-[10px] text-slate-400">
                        (تبدیل‌شده از {toPersianDigits(parseResult.rawAmountDetected.toLocaleString())} ریال)
                      </span>
                    )}
                  </div>
                </div>
                {parseResult.balanceInToman !== undefined && (
                  <div className="p-2 rounded-lg bg-slate-800/40 border border-white/5">
                    <span className="text-slate-400 block text-[10px]">مانده حساب بعد از تراکنش:</span>
                    <span className="font-medium text-slate-300 mt-0.5 block">
                      {formatFinancialAmount(parseResult.balanceInToman, 'TOMAN')}
                    </span>
                  </div>
                )}
                {parseResult.cardOrAccountMask && (
                  <div className="p-2 rounded-lg bg-slate-800/40 border border-white/5">
                    <span className="text-slate-400 block text-[10px]">کارت / حساب:</span>
                    <span className="font-mono text-slate-300 mt-0.5 block">
                      *{parseResult.cardOrAccountMask}
                    </span>
                  </div>
                )}
                {parseResult.trackingCode && (
                  <div className="p-2 rounded-lg bg-slate-800/40 border border-white/5 col-span-2">
                    <span className="text-slate-400 block text-[10px]">کد پیگیری:</span>
                    <span className="font-mono text-slate-300 mt-0.5 block">
                      {parseResult.trackingCode}
                    </span>
                  </div>
                )}
              </div>
            )}

            {/* Ignored or reason */}
            {parseResult.recognitionReasons.length > 0 && (
              <div className="text-[11px] text-slate-400 bg-slate-800/30 p-2 rounded-lg">
                <span className="font-medium text-slate-300">علت ارزیابی: </span>
                {parseResult.recognitionReasons.join(' - ')}
              </div>
            )}
          </div>
        </div>

        {/* Footer Actions */}
        <div className="px-5 py-3 border-t border-white/10 flex items-center justify-between gap-3 bg-slate-900/60">
          <span className="text-[11px] text-slate-400">
            کلید یکتا (Idempotency): {parseResult.fingerprint ? parseResult.fingerprint.slice(0, 14) + '...' : 'ندارد'}
          </span>
          <div className="flex items-center gap-2">
            <button
              onClick={onClose}
              className="px-3 py-1.5 text-xs text-slate-400 hover:text-white transition cursor-pointer"
            >
              انصراف
            </button>
            <button
              disabled={parseResult.amountInToman <= 0 || savedSuccess}
              onClick={handleCommitTransaction}
              className={`px-4 py-2 rounded-xl text-xs font-semibold flex items-center gap-1.5 transition cursor-pointer ${
                savedSuccess
                  ? 'bg-emerald-500 text-slate-950 font-bold'
                  : parseResult.amountInToman > 0
                  ? 'glass-button-primary'
                  : 'bg-slate-800 text-slate-500 cursor-not-allowed'
              }`}
            >
              {savedSuccess ? (
                <>
                  <CheckCircle className="w-4 h-4" />
                  <span>ثبت شد!</span>
                </>
              ) : (
                <>
                  <CheckCircle className="w-4 h-4" />
                  <span>ثبت در تراکنش‌های مالی</span>
                </>
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
