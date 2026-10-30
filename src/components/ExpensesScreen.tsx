import React, { useState } from 'react';
import {
  Plus,
  MessageSquareText,
  TrendingUp,
  Trash2,
  CheckCircle,
  Building2,
  AlertCircle,
  Sparkles,
  Settings2,
  ShieldAlert,
  Check,
  BellRing,
} from 'lucide-react';
import {
  BankProfile,
  CurrencyUnit,
  ExpenseItem,
  SmsTransactionKind,
  StructuredSmsAnalysisResult,
} from '../types';
import {
  analyzeBankSms,
  buildValidatedExpenseEntry,
  formatToman,
  translateParseStatus,
  translateProfileStatus,
  translateTransactionKind,
} from '../utils/persianUtils';
import { BankManager } from './BankManager';

interface ExpensesScreenProps {
  expenses: ExpenseItem[];
  onAddExpense: (expense: Omit<ExpenseItem, 'id'>) => void;
  onDeleteExpense: (id: string) => void;
  bankProfiles: BankProfile[];
  onUpdateBankProfiles: (profiles: BankProfile[]) => void;
  onOpenNotificationCenter?: () => void;
  pendingNotificationCount?: number;
}

export const ExpensesScreen: React.FC<ExpensesScreenProps> = ({
  expenses,
  onAddExpense,
  onDeleteExpense,
  bankProfiles,
  onUpdateBankProfiles,
  onOpenNotificationCenter,
  pendingNotificationCount = 0,
}) => {
  const [selectedBankId, setSelectedBankId] = useState<string>(
    bankProfiles[0]?.id || 'mellat'
  );
  const [senderIdInput, setSenderIdInput] = useState('');
  const [smsInput, setSmsInput] = useState('');
  const [analysisResult, setAnalysisResult] =
    useState<StructuredSmsAnalysisResult | null>(null);
  const [statusBanner, setStatusBanner] = useState<{
    type: 'success' | 'warning' | 'error';
    text: string;
  } | null>(null);

  // Editable overrides for NEEDS_REVIEW so ambiguous/incomplete fields can be fixed or rejected
  const [reviewBankId, setReviewBankId] = useState<string>('');
  const [reviewKind, setReviewKind] = useState<SmsTransactionKind>('UNKNOWN');
  const [reviewCurrency, setReviewCurrency] = useState<CurrencyUnit>('UNKNOWN');
  const [reviewAmountOriginal, setReviewAmountOriginal] = useState<string>('');

  // Separate Modal state for BankManager so the main screen stays uncluttered
  const [isBankManagerOpen, setIsBankManagerOpen] = useState(false);
  const [bankManagerInitialTab, setBankManagerInitialTab] = useState<'samples' | 'rules' | 'new_bank'>('samples');

  // Manual expense form
  const [title, setTitle] = useState('');
  const [amount, setAmount] = useState('');
  const [category, setCategory] = useState('خرید عمومی');

  const totalSpent = expenses
    .filter((e) => e.type !== 'income' && e.type !== 'transfer')
    .reduce((acc, curr) => acc + curr.amount, 0);

  const activeBanksCount = bankProfiles.filter(
    (p) => p.status === 'ACTIVE'
  ).length;
  const selectedProfile = bankProfiles.find((p) => p.id === selectedBankId);

  const registerFromAnalysis = (res: StructuredSmsAnalysisResult): boolean => {
    const validated = buildValidatedExpenseEntry(
      res,
      undefined,
      bankProfiles,
      expenses
    );
    if (!validated.ok || !validated.entry) {
      setStatusBanner({
        type: 'error',
        text: validated.error || 'اطلاعات تراکنش برای ثبت کافی یا معتبر نیست.',
      });
      return false;
    }
    onAddExpense(validated.entry);
    return true;
  };

  const handleParseSms = (e: React.FormEvent) => {
    e.preventDefault();
    if (!smsInput.trim()) return;

    const res = analyzeBankSms(smsInput, {
      selectedBankId,
      senderId: senderIdInput.trim() || undefined,
      profiles: bankProfiles,
      existingExpenses: expenses,
    });

    setAnalysisResult(res);
    setReviewBankId(res.bankId || '');
    setReviewKind(res.transactionKind);
    setReviewCurrency(res.currency);
    setReviewAmountOriginal(
      res.amountOriginal !== undefined ? String(res.amountOriginal) : ''
    );

    if (res.isDuplicate) {
      setStatusBanner({
        type: 'error',
        text: `ثبت نشد (تکراری): ${res.reason}`,
      });
      return;
    }

    if (res.status === 'VALID_TRANSACTION' && res.canAutoRegister) {
      const registered = registerFromAnalysis(res);
      if (registered) {
        setStatusBanner({
          type: 'success',
          text: `ثبت خودکار موفق: ${translateTransactionKind(
            res.transactionKind
          )} به مبلغ ${formatToman(res.amountInTomans || 0)} در «${
            res.bankName
          }» ثبت شد.`,
        });
        setSmsInput('');
        setAnalysisResult(null);
      }
    } else if (res.status === 'NEEDS_REVIEW') {
      setStatusBanner({
        type: 'warning',
        text: `نیازمند بررسی دستی: ${res.reason}`,
      });
    } else {
      setStatusBanner({
        type: 'error',
        text: `رد شده (${translateParseStatus(res.status)}): ${res.reason}`,
      });
    }
  };

  const handleManualConfirmReview = () => {
    if (!analysisResult) return;

    const parsedRawAmount = reviewAmountOriginal.trim()
      ? parseInt(reviewAmountOriginal.replace(/,/g, ''), 10)
      : analysisResult.amountOriginal;

    const validated = buildValidatedExpenseEntry(
      analysisResult,
      {
        bankId: reviewBankId || analysisResult.bankId,
        transactionKind: reviewKind,
        currency: reviewCurrency,
        amountOriginal: parsedRawAmount,
      },
      bankProfiles,
      expenses
    );

    if (!validated.ok || !validated.entry) {
      setStatusBanner({
        type: 'error',
        text:
          validated.error ||
          'ثبت انجام نشد: لطفاً اطلاعات ناقص (نوع تراکنش، واحد پول، بانک و مبلغ) را تکمیل کنید یا پیام را رد کنید.',
      });
      return;
    }

    onAddExpense(validated.entry);
    setStatusBanner({
      type: 'success',
      text: `تراکنش (${translateTransactionKind(
        reviewKind
      )}) پس از تکمیل و تأیید دستی شما به مبلغ ${formatToman(
        validated.entry.amount
      )} ثبت شد.`,
    });
    setAnalysisResult(null);
    setSmsInput('');
  };

  const handleRejectReview = () => {
    setAnalysisResult(null);
    setSmsInput('');
    setStatusBanner({
      type: 'warning',
      text: 'پیامک مشکوک / نیازمند بررسی به درخواست شما رد شد و در تراکنش‌ها ثبت نگردید.',
    });
  };

  const sampleSmsList = [
    {
      bankId: 'mellat',
      label: 'نمونه آزمایشی: ملت (برداشت)',
      sender: '98200033',
      text: 'بانک ملت\nبرداشت از: ۶۱۰۴۳۳******۱۲۳۴\nمبلغ: ۵۰۰,۰۰۰ ریال\nمانده: ۸,۴۲۰,۰۰۰ ریال\nتاریخ: ۱۴۰۳/۰۹/۱۰\nساعت: ۱۸:۴۵\nپیگیری: ۹۸۷۶۵۴',
    },
    {
      bankId: 'blu',
      label: 'نمونه آزمایشی: بلو (انتقال پل)',
      sender: 'BLU',
      text: 'بلو\nانتقال پل\nمبلغ: ۱,۵۰۰,۰۰۰ ریال\nبه: علیرضا یوسفی\nمانده: ۶۲,۶۶۱,۴۵۲ ریال\nکد پیگیری: ۹۸۷۶۵۴۳۲۱\n1403/09/10 11:15',
    },
    {
      bankId: 'refah',
      label: 'نمونه آزمایشی: رفاه (قالب R)',
      sender: 'RefahBank',
      text: 'رفاه حساب 62****13\nبرداشت1,960,000R کد2000\nخرید فروشگاهی\nبمانده1,231,509R',
    },
    {
      bankId: 'mellat',
      label: 'تست منفی: پیامک رمز پویا (OTP)',
      sender: '98200033',
      text: 'بانک ملت\nرمز پویا: ۸۴۹۲۰۱\nمبلغ: ۱,۵۰۰,۰۰۰ ریال\nکارت: ۶۱۰۴******۱۲۳۴',
    },
    {
      bankId: 'mellat',
      label: 'تست منفی: پیامک تبلیغاتی',
      sender: '98200033',
      text: 'جشنواره پاییزی بانک ملت! با خرید حداقل ۵۰۰,۰۰۰ ریال برنده جایزه ۵۰۰,۰۰۰,۰۰۰ ریالی شوید. لغو11',
    },
  ];

  const handleManualAdd = (e: React.FormEvent) => {
    e.preventDefault();
    const numAmount = parseInt(amount, 10);
    if (!title.trim() || isNaN(numAmount) || numAmount <= 0) return;

    onAddExpense({
      title,
      amount: numAmount,
      category,
      date: '۱۴۰۵/۰۸/۰۵',
      type: 'expense',
    });
    setTitle('');
    setAmount('');
  };

  return (
    <div className="space-y-6 animate-fadeIn">
      {/* Header Row with Clean Trigger Button for BankManager Modal */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-100">
            مدیریت هزینه‌ها و پیامک بانکی
          </h2>
          <p className="text-xs text-slate-400">
            تحلیل مرحله‌ای و امن پیامک‌های بانکی بر اساس پروفایل‌های تأییدشده
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2 self-start sm:self-auto">
          {onOpenNotificationCenter && (
            <button
              type="button"
              onClick={onOpenNotificationCenter}
              className="inline-flex items-center space-x-2 space-x-reverse px-4 py-2.5 rounded-2xl bg-slate-900 border border-amber-500/40 text-amber-300 hover:bg-slate-800/90 transition shadow-lg shadow-amber-950/20 text-xs font-extrabold"
            >
              <BellRing className="w-4 h-4 text-amber-400" />
              <span>نوتیفیکیشن و لاگ فیش‌ها</span>
              {pendingNotificationCount > 0 && (
                <span className="px-2 py-0.5 rounded-full bg-amber-500 text-slate-950 text-[10px] font-extrabold">
                  {pendingNotificationCount} در انتظار
                </span>
              )}
            </button>
          )}

          <button
            type="button"
            onClick={() => {
              setBankManagerInitialTab('new_bank');
              setIsBankManagerOpen(true);
            }}
            className="inline-flex items-center space-x-2 space-x-reverse px-4 py-2.5 rounded-2xl bg-[#1f7a38] hover:bg-[#19662e] text-white transition shadow-lg shadow-emerald-900/20 text-xs font-extrabold"
          >
            <Plus className="w-4 h-4 stroke-[2.5]" />
            <span>افزودن بانک جدید</span>
          </button>

          <button
            type="button"
            onClick={() => {
              setBankManagerInitialTab('samples');
              setIsBankManagerOpen(true);
            }}
            className="inline-flex items-center space-x-2.5 space-x-reverse px-4 py-2.5 rounded-2xl bg-slate-900 border border-cyan-500/40 text-cyan-300 hover:bg-slate-800/90 transition shadow-lg shadow-cyan-500/10 text-xs font-bold"
          >
            <Settings2 className="w-4 h-4 text-cyan-400" />
            <span>مدیریت بانک‌های من</span>
            <span className="px-2 py-0.5 rounded-full bg-cyan-500/20 text-cyan-300 text-[10px]">
              {activeBanksCount} فعال از {bankProfiles.length}
            </span>
          </button>
        </div>
      </div>

      {/* Top Banner: Total Spending */}
      <div className="glass-card rounded-3xl p-6 bg-gradient-to-l from-slate-900 to-amber-950/30 border border-amber-500/20 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <span className="text-xs text-amber-400 font-medium">
            مجموع هزینه‌های ثبت‌شده ماه آبان
          </span>
          <h3 className="text-3xl font-bold text-slate-100 mt-1">
            {formatToman(totalSpent)}
          </h3>
          <p className="text-xs text-slate-400 mt-1">
            تعداد کل رکوردها: {expenses.length} مورد
          </p>
        </div>
        <div className="flex items-center space-x-2 space-x-reverse bg-amber-500/10 text-amber-400 px-4 py-2.5 rounded-2xl border border-amber-500/20 text-xs font-semibold">
          <TrendingUp className="w-4 h-4" />
          <span>موتور ضدتکرار و ضد پیامک تبلیغاتی/OTP فعال</span>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Left Column: Safe Staged Bank SMS Scanner */}
        <div className="glass-card rounded-2xl p-6 space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-2 space-x-reverse text-cyan-400">
              <MessageSquareText className="w-5 h-5" />
              <h3 className="font-bold text-slate-100 text-base">
                اسکنر امن پیامک بانکی
              </h3>
            </div>
            {selectedProfile && (
              <span
                onClick={() => setIsBankManagerOpen(true)}
                className={`cursor-pointer text-[10px] px-2.5 py-1 rounded-full font-semibold ${
                  selectedProfile.status === 'ACTIVE'
                    ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
                    : 'bg-amber-500/20 text-amber-300 border border-amber-500/30'
                }`}
              >
                وضعیت {selectedProfile.bankName}:{' '}
                {translateProfileStatus(selectedProfile.status)}
              </span>
            )}
          </div>

          {/* Bank Selection & Optional Sender Row */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="text-[11px] text-slate-400 block mb-1">
                ۱. انتخاب صریح بانک *
              </label>
              <select
                value={selectedBankId}
                onChange={(e) => {
                  setSelectedBankId(e.target.value);
                  setAnalysisResult(null);
                  setStatusBanner(null);
                }}
                className="w-full bg-slate-900 border border-slate-800 rounded-xl px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-cyan-500"
              >
                <option value="">-- بدون انتخاب بانک (بررسی با فرستنده) --</option>
                {bankProfiles.map((bp) => (
                  <option key={bp.id} value={bp.id}>
                    {bp.bankName} ({translateProfileStatus(bp.status)})
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="text-[11px] text-slate-400 block mb-1">
                ۲. سرشماره فرستنده (اختیاری)
              </label>
              <input
                type="text"
                value={senderIdInput}
                onChange={(e) => setSenderIdInput(e.target.value)}
                placeholder={
                  selectedProfile?.senders[0]
                    ? `مثلاً: ${selectedProfile.senders[0]}`
                    : 'مثلاً: 98200033'
                }
                className="w-full bg-slate-900 border border-slate-800 rounded-xl px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-cyan-500"
              />
            </div>
          </div>

          {/* Quick Synthetic Test Samples */}
          <div className="space-y-1.5">
            <span className="text-[10px] text-slate-400 block">
              نمونه‌های آزمایشی (مثبت و منفی) جهت تست رفتار موتور:
            </span>
            <div className="flex flex-wrap gap-1.5">
              {sampleSmsList.map((sample, idx) => (
                <button
                  key={idx}
                  type="button"
                  onClick={() => {
                    setSelectedBankId(sample.bankId);
                    setSenderIdInput(sample.sender);
                    setSmsInput(sample.text);
                    setAnalysisResult(null);
                    setStatusBanner(null);
                  }}
                  className="text-[10px] bg-slate-900 text-cyan-300 hover:bg-slate-800 px-2.5 py-1 rounded-lg border border-cyan-500/20 transition flex items-center space-x-1 space-x-reverse"
                >
                  <Sparkles className="w-2.5 h-2.5 text-cyan-400" />
                  <span>{sample.label}</span>
                </button>
              ))}
            </div>
          </div>

          <form onSubmit={handleParseSms} className="space-y-3">
            <textarea
              rows={4}
              value={smsInput}
              onChange={(e) => setSmsInput(e.target.value)}
              placeholder="متن پیامک بانکی را وارد کنید..."
              className="w-full bg-slate-900 border border-slate-800 rounded-xl p-3 text-xs text-slate-200 focus:outline-none focus:border-cyan-500"
            />
            <button
              type="submit"
              className="w-full py-2.5 rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 text-white font-bold text-xs shadow-lg shadow-cyan-500/20 hover:opacity-95 transition"
            >
              تحلیل و بررسی امن پیامک
            </button>
          </form>

          {/* Status Message Banner */}
          {statusBanner && (
            <div
              className={`p-3 rounded-xl text-xs flex items-start space-x-2 space-x-reverse ${
                statusBanner.type === 'success'
                  ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
                  : statusBanner.type === 'warning'
                  ? 'bg-amber-500/20 text-amber-300 border border-amber-500/30'
                  : 'bg-rose-500/20 text-rose-300 border border-rose-500/30'
              }`}
            >
              {statusBanner.type === 'success' ? (
                <CheckCircle className="w-4 h-4 shrink-0 mt-0.5" />
              ) : statusBanner.type === 'warning' ? (
                <ShieldAlert className="w-4 h-4 shrink-0 mt-0.5" />
              ) : (
                <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
              )}
              <span className="leading-relaxed">{statusBanner.text}</span>
            </div>
          )}

          {/* Manual Review Confirmation & Correction Card when status === NEEDS_REVIEW */}
          {analysisResult && analysisResult.status === 'NEEDS_REVIEW' && (
            <div className="p-4 rounded-2xl bg-slate-950/90 border border-amber-500/40 space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-xs font-bold text-amber-400">
                  پیش‌نمایش بررسی دستی (ثبت خودکار متوقف شد)
                </span>
                <span className="text-[10px] px-2 py-0.5 rounded-full bg-amber-500/20 text-amber-300">
                  نیازمند تکمیل / تصمیم کاربر
                </span>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5 text-xs bg-slate-900 p-3 rounded-xl border border-slate-800">
                <div>
                  <label className="text-slate-400 block text-[10px] mb-1">
                    بانک *:
                  </label>
                  <select
                    value={reviewBankId}
                    onChange={(e) => setReviewBankId(e.target.value)}
                    className="w-full bg-slate-950 border border-slate-700 rounded-lg px-2 py-1.5 text-xs text-slate-200"
                  >
                    <option value="">-- انتخاب بانک --</option>
                    {bankProfiles.map((bp) => (
                      <option key={bp.id} value={bp.id}>
                        {bp.bankName}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="text-slate-400 block text-[10px] mb-1">
                    نوع تراکنش * (UNKNOWN هرگز هزینه نمی‌شود):
                  </label>
                  <select
                    value={reviewKind}
                    onChange={(e) =>
                      setReviewKind(e.target.value as SmsTransactionKind)
                    }
                    className={`w-full bg-slate-950 border rounded-lg px-2 py-1.5 text-xs ${
                      reviewKind === 'UNKNOWN' || reviewKind === 'FAILED'
                        ? 'border-rose-500/60 text-rose-300'
                        : 'border-slate-700 text-cyan-300'
                    }`}
                  >
                    <option value="UNKNOWN">-- نامشخص (الزامی است انتخاب کنید) --</option>
                    <option value="WITHDRAWAL">برداشت وجه (هزینه)</option>
                    <option value="PURCHASE">خرید / پرداخت (هزینه)</option>
                    <option value="DEPOSIT">واریز به حساب (درآمد)</option>
                    <option value="TRANSFER">انتقال وجه</option>
                    <option value="FEE">کارمزد بانکی (هزینه)</option>
                    <option value="REFUND">برگشت وجه (درآمد)</option>
                  </select>
                </div>

                <div>
                  <label className="text-slate-400 block text-[10px] mb-1">
                    واحد پول *:
                  </label>
                  <select
                    value={reviewCurrency}
                    onChange={(e) =>
                      setReviewCurrency(e.target.value as CurrencyUnit)
                    }
                    className={`w-full bg-slate-950 border rounded-lg px-2 py-1.5 text-xs ${
                      reviewCurrency === 'UNKNOWN'
                        ? 'border-rose-500/60 text-rose-300'
                        : 'border-slate-700 text-slate-200'
                    }`}
                  >
                    <option value="UNKNOWN">-- نامشخص (انتخاب واحد پول) --</option>
                    <option value="RIAL">ریال (تبدیل به تومان ÷ ۱۰)</option>
                    <option value="TOMAN">تومان</option>
                  </select>
                </div>

                <div>
                  <label className="text-slate-400 block text-[10px] mb-1">
                    مبلغ خام تراکنش *:
                  </label>
                  <input
                    type="number"
                    value={reviewAmountOriginal}
                    onChange={(e) => setReviewAmountOriginal(e.target.value)}
                    placeholder="مثلاً: 500000"
                    className="w-full bg-slate-950 border border-slate-700 rounded-lg px-2 py-1.5 text-xs text-emerald-400 font-bold"
                  />
                </div>

                <div className="sm:col-span-2 flex items-center justify-between pt-1 border-t border-slate-800/80 text-[11px]">
                  <span className="text-slate-400">
                    مانده حساب تفکیک‌شده:{' '}
                    <strong className="text-slate-200">
                      {analysisResult.balanceInTomans !== undefined
                        ? formatToman(analysisResult.balanceInTomans)
                        : '—'}
                    </strong>
                  </span>
                  {analysisResult.matchedTemplateName && (
                    <span className="text-slate-400">
                      قالب:{' '}
                      <strong className="text-cyan-300">
                        {analysisResult.matchedTemplateName} (v
                        {analysisResult.matchedTemplateVersion || 1})
                      </strong>
                    </span>
                  )}
                </div>
              </div>

              <div className="flex flex-wrap justify-end gap-2">
                <button
                  type="button"
                  onClick={handleRejectReview}
                  className="px-3 py-1.5 rounded-lg bg-rose-500/20 border border-rose-500/30 text-rose-300 text-xs hover:bg-rose-500/30 transition"
                >
                  رد کردن پیام (عدم ثبت)
                </button>
                <button
                  type="button"
                  onClick={() => setIsBankManagerOpen(true)}
                  className="px-3 py-1.5 rounded-lg bg-slate-800 text-cyan-300 text-xs hover:bg-slate-700 transition"
                >
                  تأیید و فعال‌سازی در مدیریت بانک‌ها
                </button>
                {!analysisResult.isDuplicate && (
                  <button
                    type="button"
                    onClick={handleManualConfirmReview}
                    className="px-3.5 py-1.5 rounded-lg bg-amber-500 text-slate-950 font-bold text-xs flex items-center space-x-1 space-x-reverse hover:bg-amber-400 transition"
                  >
                    <Check className="w-3.5 h-3.5" />
                    <span>تأیید دستی و ثبت با اطلاعات معتبر</span>
                  </button>
                )}
              </div>
            </div>
          )}
        </div>

        {/* Right Column: Manual Expense Entry */}
        <div className="glass-card rounded-2xl p-6 space-y-4">
          <div className="flex items-center space-x-2 space-x-reverse text-amber-400">
            <Plus className="w-5 h-5" />
            <h3 className="font-bold text-slate-100 text-base">
              ثبت دستی هزینه
            </h3>
          </div>

          <form onSubmit={handleManualAdd} className="space-y-3">
            <div>
              <label className="text-xs text-slate-400 block mb-1">
                عنوان / بابت
              </label>
              <input
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="مثلاً: خرید قهوه و میان‌وعده"
                className="w-full bg-slate-900 border border-slate-800 rounded-xl px-4 py-2 text-sm text-slate-200 focus:outline-none focus:border-cyan-500"
              />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="text-xs text-slate-400 block mb-1">
                  مبلغ (تومان)
                </label>
                <input
                  type="number"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  placeholder="مثلاً: 150000"
                  className="w-full bg-slate-900 border border-slate-800 rounded-xl px-4 py-2 text-sm text-slate-200 focus:outline-none focus:border-cyan-500"
                />
              </div>
              <div>
                <label className="text-xs text-slate-400 block mb-1">
                  دسته‌بندی
                </label>
                <select
                  value={category}
                  onChange={(e) => setCategory(e.target.value)}
                  className="w-full bg-slate-900 border border-slate-800 rounded-xl px-3 py-2 text-sm text-slate-200 focus:outline-none focus:border-cyan-500"
                >
                  <option value="خرید عمومی">خرید عمومی</option>
                  <option value="غذا و رستوران">غذا و رستوران</option>
                  <option value="حمل و نقل">حمل و نقل</option>
                  <option value="قبوض و خدمات">قبوض و خدمات</option>
                  <option value="سایر">سایر</option>
                </select>
              </div>
            </div>
            <button
              type="submit"
              className="w-full py-2.5 rounded-xl bg-gradient-to-r from-amber-500 to-orange-600 text-white font-medium text-sm shadow-lg shadow-amber-500/20 hover:opacity-95 transition mt-2"
            >
              ثبت هزینه در لیست
            </button>
          </form>
        </div>
      </div>

      {/* Expenses History List */}
      <div className="glass-card rounded-2xl p-6 space-y-4">
        <h3 className="font-bold text-slate-100 text-base">
          تاریخچه تراکنش‌ها و هزینه‌ها
        </h3>

        <div className="space-y-3">
          {expenses.length === 0 ? (
            <p className="text-xs text-slate-500 text-center py-6">
              هیچ هزینه‌ای ثبت نشده است.
            </p>
          ) : (
            expenses.map((item) => (
              <div
                key={item.id}
                className="bg-slate-900/60 border border-slate-800 rounded-xl p-4 flex items-center justify-between hover:border-cyan-500/30 transition"
              >
                <div className="flex items-center space-x-3 space-x-reverse">
                  <div
                    className={`w-10 h-10 rounded-xl flex items-center justify-center ${
                      item.type === 'income'
                        ? 'bg-emerald-500/10 text-emerald-400'
                        : 'bg-cyan-500/10 text-cyan-400'
                    }`}
                  >
                    <Building2 className="w-5 h-5" />
                  </div>
                  <div>
                    <h4 className="text-sm font-bold text-slate-200">
                      {item.title}
                    </h4>
                    <div className="flex flex-wrap items-center gap-x-2 gap-y-1 text-[10px] text-slate-400 mt-0.5">
                      <span className="text-cyan-400 font-medium">
                        {item.category}
                      </span>
                      <span>•</span>
                      <span>{item.date}</span>
                      {item.bankName && (
                        <>
                          <span>•</span>
                          <span className="text-amber-400 font-semibold">
                            {item.bankName}
                          </span>
                        </>
                      )}
                      {item.trackingCode && (
                        <>
                          <span>•</span>
                          <span className="text-slate-400 font-mono">
                            پیگیری: {item.trackingCode}
                          </span>
                        </>
                      )}
                      {item.balanceAfter !== undefined && (
                        <>
                          <span>•</span>
                          <span className="text-slate-400">
                            مانده: {formatToman(item.balanceAfter)}
                          </span>
                        </>
                      )}
                    </div>
                  </div>
                </div>

                <div className="flex items-center space-x-4 space-x-reverse">
                  <span
                    className={`text-sm font-bold ${
                      item.type === 'income'
                        ? 'text-emerald-400'
                        : 'text-slate-100'
                    }`}
                  >
                    {item.type === 'income' ? '+' : ''}
                    {formatToman(item.amount)}
                  </span>
                  <button
                    onClick={() => onDeleteExpense(item.id)}
                    className="p-2 rounded-lg text-slate-500 hover:text-rose-400 hover:bg-rose-500/10 transition"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      </div>

      {/* Dedicated Floating Modal for BankManager */}
      <BankManager
        isOpen={isBankManagerOpen}
        onClose={() => setIsBankManagerOpen(false)}
        profiles={bankProfiles}
        onUpdateProfiles={onUpdateBankProfiles}
        selectedBankId={selectedBankId || bankProfiles[0]?.id || 'mellat'}
        onSelectBankId={setSelectedBankId}
        initialTab={bankManagerInitialTab}
      />
    </div>
  );
};
