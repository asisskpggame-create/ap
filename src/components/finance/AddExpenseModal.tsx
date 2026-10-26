import React, { useState } from 'react';
import {
  X,
  Plus,
  ArrowDownLeft,
  ArrowUpRight,
  RefreshCw,
  Coins,
  HandCoins,
  Check,
  Calendar
} from 'lucide-react';
import { useLifeOs } from '../../context/LifeOsContext';
import { TransactionType } from '../../types/lifeOs';
import { currentLocalEpochDay, formatFinancialAmount, toPersianDigits } from '../../utils/persianDateUtils';

interface AddExpenseModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const AddExpenseModal: React.FC<AddExpenseModalProps> = ({
  isOpen,
  onClose
}) => {
  const { addExpense, preferences } = useLifeOs();

  const [txType, setTxType] = useState<TransactionType>('EXPENSE');
  const [title, setTitle] = useState('');
  const [amountRaw, setAmountRaw] = useState('');
  const [currencyUnit, setCurrencyUnit] = useState<'TOMAN' | 'THOUSAND_TOMAN' | 'MILLION_TOMAN' | 'IRR'>('TOMAN');
  const [category, setCategory] = useState('خرید روزمره');
  const [sourceAccount, setSourceAccount] = useState(preferences.financialAccounts[0] || 'کارت اصلی');
  const [destinationAccount, setDestinationAccount] = useState(preferences.financialAccounts[1] || 'کارت پس‌انداز');
  const [notes, setNotes] = useState('');
  const [installmentCount, setInstallmentCount] = useState(1);
  const [dueDaysOffset, setDueDaysOffset] = useState(30);

  if (!isOpen) return null;

  const calculateFinalAmountInToman = (): number => {
    const raw = parseFloat(amountRaw.replace(/[,،]/g, '')) || 0;
    if (currencyUnit === 'THOUSAND_TOMAN') {
      return raw * 1000;
    } else if (currencyUnit === 'MILLION_TOMAN') {
      return raw * 1000000;
    } else if (currencyUnit === 'IRR') {
      return Math.round(raw / 10);
    }
    return raw;
  };

  const finalAmount = calculateFinalAmountInToman();

  const handleAddQuickAmount = (delta: number) => {
    const current = calculateFinalAmountInToman();
    const next = current + delta;
    setCurrencyUnit('TOMAN');
    setAmountRaw(next.toString());
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (finalAmount <= 0) return;

    const todayEpoch = currentLocalEpochDay();
    const defaultTitle =
      txType === 'EXPENSE'
        ? category
        : txType === 'INCOME'
        ? 'درآمد و واریزی'
        : txType === 'TRANSFER'
        ? `انتقال از ${sourceAccount} به ${destinationAccount}`
        : txType === 'DEBT'
        ? 'بدهی و استقراض'
        : 'طلبکاری';

    addExpense({
      title: title.trim() || defaultTitle,
      category,
      amount: finalAmount,
      currency: 'TOMAN',
      timestampMillis: Date.now(),
      localEpochDay: todayEpoch,
      notes: notes.trim(),
      inputSource: 'MANUAL',
      transactionType: txType,
      paidAmount: txType === 'DEBT' || txType === 'RECEIVABLE' ? 0 : finalAmount,
      dueEpochDay: txType === 'DEBT' || txType === 'RECEIVABLE' ? todayEpoch + dueDaysOffset : undefined,
      installmentCount: txType === 'DEBT' ? installmentCount : 1,
      sourceAccount: txType === 'TRANSFER' ? sourceAccount : undefined,
      destinationAccount: txType === 'TRANSFER' ? destinationAccount : undefined
    });

    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-md">
      <div className="glass-panel w-full max-w-lg rounded-2xl border border-white/10 shadow-2xl overflow-hidden max-h-[92vh] flex flex-col">
        {/* Header */}
        <div className="px-5 py-3.5 border-b border-white/10 flex items-center justify-between">
          <h2 className="text-sm sm:text-base font-bold text-slate-100 flex items-center gap-2">
            <Coins className="w-4 h-4 text-teal-400" />
            ثبت تراکنش مالی جدید
          </h2>
          <button
            onClick={onClose}
            className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-white/5 transition cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content Form */}
        <form onSubmit={handleSubmit} className="p-5 space-y-4 overflow-y-auto">
          {/* Transaction Type Tabs */}
          <div className="grid grid-cols-5 gap-1 p-1 bg-slate-900/80 rounded-xl border border-white/5 text-xs text-center font-medium">
            <button
              type="button"
              onClick={() => {
                setTxType('EXPENSE');
                setCategory('خرید روزمره');
              }}
              className={`py-1.5 rounded-lg transition cursor-pointer flex flex-col items-center justify-center gap-1 ${
                txType === 'EXPENSE'
                  ? 'bg-rose-500/20 text-rose-300 font-bold border border-rose-500/30'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <ArrowUpRight className="w-3.5 h-3.5 text-rose-400" />
              هزینه
            </button>
            <button
              type="button"
              onClick={() => {
                setTxType('INCOME');
                setCategory('حقوق و دستمزد');
              }}
              className={`py-1.5 rounded-lg transition cursor-pointer flex flex-col items-center justify-center gap-1 ${
                txType === 'INCOME'
                  ? 'bg-emerald-500/20 text-emerald-300 font-bold border border-emerald-500/30'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <ArrowDownLeft className="w-3.5 h-3.5 text-emerald-400" />
              درآمد
            </button>
            <button
              type="button"
              onClick={() => {
                setTxType('TRANSFER');
                setCategory('انتقال بین حسابی');
              }}
              className={`py-1.5 rounded-lg transition cursor-pointer flex flex-col items-center justify-center gap-1 ${
                txType === 'TRANSFER'
                  ? 'bg-cyan-500/20 text-cyan-300 font-bold border border-cyan-500/30'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <RefreshCw className="w-3.5 h-3.5 text-cyan-400" />
              انتقال
            </button>
            <button
              type="button"
              onClick={() => {
                setTxType('DEBT');
                setCategory('بدهی و اقساط');
              }}
              className={`py-1.5 rounded-lg transition cursor-pointer flex flex-col items-center justify-center gap-1 ${
                txType === 'DEBT'
                  ? 'bg-amber-500/20 text-amber-300 font-bold border border-amber-500/30'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <Coins className="w-3.5 h-3.5 text-amber-400" />
              بدهی
            </button>
            <button
              type="button"
              onClick={() => {
                setTxType('RECEIVABLE');
                setCategory('طلب و وام');
              }}
              className={`py-1.5 rounded-lg transition cursor-pointer flex flex-col items-center justify-center gap-1 ${
                txType === 'RECEIVABLE'
                  ? 'bg-purple-500/20 text-purple-300 font-bold border border-purple-500/30'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <HandCoins className="w-3.5 h-3.5 text-purple-400" />
              طلب
            </button>
          </div>

          {/* Amount and Rotary Unit */}
          <div>
            <label className="text-xs font-semibold text-slate-300 block mb-1">
              مبلغ تراکنش:
            </label>
            <div className="flex gap-2">
              <input
                type="text"
                required
                value={amountRaw}
                onChange={e => setAmountRaw(e.target.value)}
                placeholder="مثال: ۲۵۰,۰۰۰"
                className="flex-1 rounded-xl bg-slate-900/80 border border-white/10 px-3 py-2 text-base font-bold text-slate-100 placeholder-slate-500 focus:outline-none focus:border-teal-500/50"
              />
              <select
                value={currencyUnit}
                onChange={e => setCurrencyUnit(e.target.value as any)}
                className="rounded-xl bg-slate-800 border border-white/10 px-2.5 py-2 text-xs font-semibold text-teal-300 focus:outline-none cursor-pointer"
              >
                <option value="TOMAN">تومان</option>
                <option value="THOUSAND_TOMAN">هزار تومان</option>
                <option value="MILLION_TOMAN">میلیون تومان</option>
                <option value="IRR">ریال</option>
              </select>
            </div>

            {/* Final calculated label */}
            {finalAmount > 0 && (
              <div className="text-[11px] text-teal-300/90 mt-1 font-medium flex items-center justify-between">
                <span>مبلغ نهایی معادل:</span>
                <span className="font-bold">
                  {formatFinancialAmount(finalAmount, 'TOMAN')}
                </span>
              </div>
            )}

            {/* Quick Multiplier Buttons */}
            <div className="flex flex-wrap gap-1.5 mt-2">
              <button
                type="button"
                onClick={() => handleAddQuickAmount(50000)}
                className="px-2 py-0.5 rounded-lg bg-slate-800/80 hover:bg-slate-700 text-[11px] text-slate-300 border border-white/5 cursor-pointer"
              >
                +۵۰ هزار
              </button>
              <button
                type="button"
                onClick={() => handleAddQuickAmount(100000)}
                className="px-2 py-0.5 rounded-lg bg-slate-800/80 hover:bg-slate-700 text-[11px] text-slate-300 border border-white/5 cursor-pointer"
              >
                +۱۰۰ هزار
              </button>
              <button
                type="button"
                onClick={() => handleAddQuickAmount(500000)}
                className="px-2 py-0.5 rounded-lg bg-slate-800/80 hover:bg-slate-700 text-[11px] text-slate-300 border border-white/5 cursor-pointer"
              >
                +۵۰۰ هزار
              </button>
              <button
                type="button"
                onClick={() => handleAddQuickAmount(1000000)}
                className="px-2 py-0.5 rounded-lg bg-slate-800/80 hover:bg-slate-700 text-[11px] text-slate-300 border border-white/5 cursor-pointer"
              >
                +۱ میلیون
              </button>
            </div>
          </div>

          {/* Title */}
          <div>
            <label className="text-xs font-semibold text-slate-300 block mb-1">
              عنوان تراکنش (اختیاری):
            </label>
            <input
              type="text"
              value={title}
              onChange={e => setTitle(e.target.value)}
              placeholder="مثال: خرید سوپرمارکت، کرایه اسنپ، شارژ ساختمان..."
              className="w-full rounded-xl bg-slate-900/80 border border-white/10 px-3 py-2 text-xs sm:text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:border-teal-500/50"
            />
          </div>

          {/* Transfer Accounts (if TRANSFER) */}
          {txType === 'TRANSFER' && (
            <div className="grid grid-cols-2 gap-2 p-3 bg-slate-900/60 rounded-xl border border-white/5 text-xs">
              <div>
                <label className="text-slate-400 block mb-1 text-[11px]">از حساب مبدأ:</label>
                <select
                  value={sourceAccount}
                  onChange={e => setSourceAccount(e.target.value)}
                  className="w-full rounded-lg bg-slate-800 border border-white/10 p-1.5 text-xs text-slate-200"
                >
                  {preferences.financialAccounts.map((acc, i) => (
                    <option key={i} value={acc}>{acc}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="text-slate-400 block mb-1 text-[11px]">به حساب مقصد:</label>
                <select
                  value={destinationAccount}
                  onChange={e => setDestinationAccount(e.target.value)}
                  className="w-full rounded-lg bg-slate-800 border border-white/10 p-1.5 text-xs text-slate-200"
                >
                  {preferences.financialAccounts.map((acc, i) => (
                    <option key={i} value={acc}>{acc}</option>
                  ))}
                </select>
              </div>
            </div>
          )}

          {/* Debt / Receivable details */}
          {(txType === 'DEBT' || txType === 'RECEIVABLE') && (
            <div className="grid grid-cols-2 gap-2 p-3 bg-slate-900/60 rounded-xl border border-white/5 text-xs">
              <div>
                <label className="text-slate-400 block mb-1 text-[11px]">موعد تسویه:</label>
                <select
                  value={dueDaysOffset}
                  onChange={e => setDueDaysOffset(parseInt(e.target.value))}
                  className="w-full rounded-lg bg-slate-800 border border-white/10 p-1.5 text-xs text-slate-200"
                >
                  <option value={7}>۱ هفته دیگر</option>
                  <option value={14}>۲ هفته دیگر</option>
                  <option value={30}>۱ ماه دیگر</option>
                  <option value={60}>۲ ماه دیگر</option>
                  <option value={90}>۳ ماه دیگر</option>
                </select>
              </div>
              {txType === 'DEBT' && (
                <div>
                  <label className="text-slate-400 block mb-1 text-[11px]">تعداد اقساط:</label>
                  <input
                    type="number"
                    min={1}
                    max={36}
                    value={installmentCount}
                    onChange={e => setInstallmentCount(parseInt(e.target.value) || 1)}
                    className="w-full rounded-lg bg-slate-800 border border-white/10 p-1.5 text-xs text-slate-200"
                  />
                </div>
              )}
            </div>
          )}

          {/* Category selection */}
          {txType !== 'TRANSFER' && (
            <div>
              <label className="text-xs font-semibold text-slate-300 block mb-1.5">
                دسته‌بندی:
              </label>
              <div className="flex flex-wrap gap-1.5 max-h-24 overflow-y-auto">
                {preferences.customExpenseCategories.map((cat, i) => (
                  <button
                    key={i}
                    type="button"
                    onClick={() => setCategory(cat)}
                    className={`px-2.5 py-1 text-xs rounded-lg transition cursor-pointer ${
                      category === cat
                        ? 'bg-teal-500/25 text-teal-300 font-semibold border border-teal-500/40'
                        : 'bg-slate-800/60 hover:bg-slate-700/60 text-slate-300 border border-white/5'
                    }`}
                  >
                    {cat}
                  </button>
                ))}
              </div>
            </div>
          )}

          {/* Notes */}
          <div>
            <label className="text-xs font-semibold text-slate-300 block mb-1">
              یادداشت / توضیحات:
            </label>
            <input
              type="text"
              value={notes}
              onChange={e => setNotes(e.target.value)}
              placeholder="توضیحات تکمیلی..."
              className="w-full rounded-xl bg-slate-900/80 border border-white/10 px-3 py-1.5 text-xs text-slate-100 placeholder-slate-500 focus:outline-none focus:border-teal-500/50"
            />
          </div>

          {/* Footer Submit */}
          <div className="pt-2 flex items-center justify-end gap-2 border-t border-white/10">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-xs text-slate-400 hover:text-white transition cursor-pointer"
            >
              انصراف
            </button>
            <button
              type="submit"
              disabled={finalAmount <= 0}
              className={`px-5 py-2 rounded-xl text-xs font-semibold flex items-center gap-1.5 transition cursor-pointer ${
                finalAmount > 0
                  ? 'glass-button-primary'
                  : 'bg-slate-800 text-slate-500 cursor-not-allowed'
              }`}
            >
              <Check className="w-4 h-4" />
              <span>ثبت نهایی</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
