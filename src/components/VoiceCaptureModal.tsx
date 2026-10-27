import React, { useState } from 'react';
import { Mic, X, CheckCircle, AlertCircle, ShieldAlert } from 'lucide-react';
import {
  BankProfile,
  ExpenseItem,
  StructuredSmsAnalysisResult,
} from '../types';
import {
  analyzeBankSms,
  formatToman,
  translateTransactionKind,
} from '../utils/persianUtils';

interface VoiceCaptureModalProps {
  isOpen: boolean;
  onClose: () => void;
  bankProfiles: BankProfile[];
  existingExpenses: ExpenseItem[];
  onAddExpense: (expense: Omit<ExpenseItem, 'id'>) => void;
  onAddQuickTask: (title: string) => void;
}

export const VoiceCaptureModal: React.FC<VoiceCaptureModalProps> = ({
  isOpen,
  onClose,
  bankProfiles,
  existingExpenses,
  onAddExpense,
  onAddQuickTask,
}) => {
  const [mode, setMode] = useState<'sms' | 'voice'>('sms');
  const [selectedBankId, setSelectedBankId] = useState<string>(
    bankProfiles[0]?.id || 'mellat'
  );
  const [senderId, setSenderId] = useState('');
  const [inputText, setInputText] = useState('');
  const [feedback, setFeedback] = useState<{
    type: 'success' | 'warning' | 'error';
    text: string;
  } | null>(null);
  const [pendingReview, setPendingReview] =
    useState<StructuredSmsAnalysisResult | null>(null);

  if (!isOpen) return null;

  const commitTransaction = (res: StructuredSmsAnalysisResult) => {
    if (!res.amountInTomans || !res.bankName) return;
    onAddExpense({
      title: res.merchant || 'تراکنش بانکی',
      amount: res.amountInTomans,
      category:
        res.financialType === 'income' ? 'واریز / درآمد بانکی' : 'بانکی / پیامک',
      date: '۱۴۰۵/۰۸/۰۵',
      type: res.financialType || 'expense',
      isSmsParsed: true,
      bankId: res.bankId,
      bankName: res.bankName,
      trackingCode: res.trackingCode,
      balanceAfter: res.balanceInTomans,
      messageFingerprint: res.messageFingerprint,
      templateId: res.matchedTemplateId,
    });
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!inputText.trim()) return;

    if (mode === 'voice') {
      onAddQuickTask(inputText.trim());
      setFeedback({
        type: 'success',
        text: 'یادداشت / وظیفه صوتی با موفقیت در لیست وظایف ثبت شد.',
      });
      setTimeout(() => {
        setFeedback(null);
        setInputText('');
        onClose();
      }, 1200);
      return;
    }

    // SMS Mode: Use safe staged analyzer
    const analysis = analyzeBankSms(inputText, {
      selectedBankId,
      senderId: senderId.trim() || undefined,
      profiles: bankProfiles,
      existingExpenses,
    });

    if (analysis.isDuplicate) {
      setPendingReview(null);
      setFeedback({
        type: 'error',
        text: `ثبت نشد (تکراری): ${analysis.reason}`,
      });
      return;
    }

    if (analysis.status === 'VALID_TRANSACTION' && analysis.canAutoRegister) {
      commitTransaction(analysis);
      setPendingReview(null);
      setFeedback({
        type: 'success',
        text: `تراکنش ${translateTransactionKind(
          analysis.transactionKind
        )} به مبلغ ${formatToman(analysis.amountInTomans || 0)} (${
          analysis.bankName
        }) ثبت شد.`,
      });
      setTimeout(() => {
        setFeedback(null);
        setInputText('');
        onClose();
      }, 1400);
    } else if (analysis.status === 'NEEDS_REVIEW') {
      setPendingReview(analysis);
      setFeedback({
        type: 'warning',
        text: `نیازمند بررسی دستی: ${analysis.reason}`,
      });
    } else {
      setPendingReview(null);
      setFeedback({
        type: 'error',
        text: `تراکنش مالی تشخیص داده نشد: ${analysis.reason}`,
      });
    }
  };

  const handleConfirmPendingReview = () => {
    if (
      !pendingReview ||
      !pendingReview.amountInTomans ||
      !pendingReview.bankName
    ) {
      return;
    }
    commitTransaction(pendingReview);
    setPendingReview(null);
    setFeedback({
      type: 'success',
      text: `تراکنش پس از تأیید دستی شما به مبلغ ${formatToman(
        pendingReview.amountInTomans
      )} ثبت شد.`,
    });
    setTimeout(() => {
      setFeedback(null);
      setInputText('');
      onClose();
    }, 1200);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md animate-fadeIn">
      <div className="glass-card rounded-3xl p-6 w-full max-w-lg space-y-5 relative border border-cyan-500/30">
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
            <h3 className="text-lg font-bold text-slate-100">
              اسکن امن پیامک بانکی و ثبت سریع وظیفه
            </h3>
            <p className="text-xs text-cyan-400">
              تفکیک صریح اسکن پیامک بانکی از یادداشت صوتی
            </p>
          </div>
        </div>

        {/* Mode Switcher */}
        <div className="grid grid-cols-2 gap-2 bg-slate-900 p-1 rounded-xl border border-slate-800">
          <button
            type="button"
            onClick={() => {
              setMode('sms');
              setFeedback(null);
              setPendingReview(null);
            }}
            className={`py-2 rounded-lg text-xs font-bold transition ${
              mode === 'sms'
                ? 'bg-cyan-500 text-slate-950'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            اسکن پیامک بانکی
          </button>
          <button
            type="button"
            onClick={() => {
              setMode('voice');
              setFeedback(null);
              setPendingReview(null);
            }}
            className={`py-2 rounded-lg text-xs font-bold transition ${
              mode === 'voice'
                ? 'bg-cyan-500 text-slate-950'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            ثبت سریع وظیفه / یادداشت
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-3">
          {mode === 'sms' && (
            <div className="grid grid-cols-2 gap-2">
              <div>
                <label className="text-[11px] text-slate-400 block mb-1">
                  انتخاب بانک *
                </label>
                <select
                  value={selectedBankId}
                  onChange={(e) => setSelectedBankId(e.target.value)}
                  className="w-full bg-slate-900 border border-slate-700/80 rounded-xl px-2.5 py-2 text-xs text-slate-200"
                >
                  {bankProfiles.map((bp) => (
                    <option key={bp.id} value={bp.id}>
                      {bp.bankName}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="text-[11px] text-slate-400 block mb-1">
                  سرشماره فرستنده (اختیاری)
                </label>
                <input
                  type="text"
                  value={senderId}
                  onChange={(e) => setSenderId(e.target.value)}
                  placeholder="مثلاً: 98200033"
                  className="w-full bg-slate-900 border border-slate-700/80 rounded-xl px-2.5 py-2 text-xs text-slate-200"
                />
              </div>
            </div>
          )}

          <div>
            <textarea
              rows={4}
              value={inputText}
              onChange={(e) => setInputText(e.target.value)}
              placeholder={
                mode === 'sms'
                  ? 'متن کامل پیامک بانکی را اینجا وارد کنید...'
                  : 'عنوان وظیفه یا یادداشت صوتی خود را بنویسید...'
              }
              className="w-full bg-slate-900 border border-slate-700/80 rounded-2xl p-3.5 text-xs text-slate-200 focus:outline-none focus:border-cyan-500"
            />
          </div>

          {feedback && (
            <div
              className={`p-3 rounded-xl text-xs flex items-start space-x-2 space-x-reverse ${
                feedback.type === 'success'
                  ? 'bg-emerald-500/20 border border-emerald-500/30 text-emerald-300'
                  : feedback.type === 'warning'
                  ? 'bg-amber-500/20 border border-amber-500/30 text-amber-300'
                  : 'bg-rose-500/20 border border-rose-500/30 text-rose-300'
              }`}
            >
              {feedback.type === 'success' ? (
                <CheckCircle className="w-4 h-4 shrink-0 mt-0.5" />
              ) : feedback.type === 'warning' ? (
                <ShieldAlert className="w-4 h-4 shrink-0 mt-0.5" />
              ) : (
                <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
              )}
              <span>{feedback.text}</span>
            </div>
          )}

          {pendingReview &&
            pendingReview.amountInTomans &&
            pendingReview.bankName && (
              <div className="p-3 rounded-xl bg-slate-950 border border-amber-500/40 flex items-center justify-between text-xs">
                <div className="text-slate-300">
                  <span>
                    {pendingReview.bankName} •{' '}
                    {translateTransactionKind(pendingReview.transactionKind)}:{' '}
                  </span>
                  <strong className="text-amber-400">
                    {formatToman(pendingReview.amountInTomans)}
                  </strong>
                </div>
                <button
                  type="button"
                  onClick={handleConfirmPendingReview}
                  className="px-3 py-1.5 rounded-lg bg-amber-500 text-slate-950 font-bold text-xs"
                >
                  تأیید و ثبت دستی
                </button>
              </div>
            )}

          <div className="flex items-center justify-end space-x-3 space-x-reverse">
            <button
              type="button"
              onClick={onClose}
              className="px-5 py-2.5 rounded-xl bg-slate-800 text-slate-300 text-xs hover:bg-slate-700 transition"
            >
              انصراف
            </button>
            <button
              type="submit"
              className="px-6 py-2.5 rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 text-white font-bold text-xs shadow-lg shadow-cyan-500/25 hover:opacity-95 transition"
            >
              {mode === 'sms' ? 'تحلیل امن پیامک' : 'ثبت وظیفه'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
