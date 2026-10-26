import React, { useState } from 'react';
import {
  Wallet,
  ArrowDownLeft,
  ArrowUpRight,
  RefreshCw,
  Plus,
  CreditCard,
  HandCoins,
  Coins,
  TrendingUp,
  Search,
  Filter,
  CheckCircle,
  Clock,
  Trash2,
  Calendar,
  Sparkles
} from 'lucide-react';
import { useLifeOs } from '../../context/LifeOsContext';
import { ExpenseEntry, TransactionType } from '../../types/lifeOs';
import {
  formatFinancialAmount,
  formatCompactCurrency,
  formatEpochDay,
  calculateSafeToSpend,
  currentLocalEpochDay,
  toPersianDigits
} from '../../utils/persianDateUtils';

interface DailyLogScreenProps {
  onOpenAddExpense: () => void;
  onOpenSmsScanner: () => void;
}

export const DailyLogScreen: React.FC<DailyLogScreenProps> = ({
  onOpenAddExpense,
  onOpenSmsScanner
}) => {
  const {
    expenses,
    deleteExpense,
    recordDebtRepayment,
    preferences
  } = useLifeOs();

  const [activeTab, setActiveTab] = useState<'TRANSACTIONS' | 'DEBTS' | 'ACCOUNTS' | 'TRENDS'>('TRANSACTIONS');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<string>('ALL');

  // Repayment modal state
  const [repaymentTarget, setRepaymentTarget] = useState<ExpenseEntry | null>(null);
  const [repayAmountInput, setRepayAmountInput] = useState('');

  const todayEpoch = currentLocalEpochDay();

  // Financial aggregates
  const totalIncome = expenses
    .filter(e => e.transactionType === 'INCOME')
    .reduce((sum, e) => sum + e.amount, 0);

  const totalExpense = expenses
    .filter(e => e.transactionType === 'EXPENSE')
    .reduce((sum, e) => sum + e.amount, 0);

  const netSavings = totalIncome - totalExpense;

  const safeToSpend = calculateSafeToSpend(
    preferences.monthlyBudgetTarget,
    totalExpense,
    todayEpoch
  );

  // Debts and Receivables
  const debtEntries = expenses.filter(e => e.transactionType === 'DEBT' || e.transactionType === 'DEBT_SETTLED');
  const receivableEntries = expenses.filter(e => e.transactionType === 'RECEIVABLE' || e.transactionType === 'RECEIVABLE_SETTLED');

  const totalOutstandingDebt = debtEntries
    .filter(e => e.transactionType === 'DEBT')
    .reduce((sum, e) => sum + Math.max(0, e.amount - (e.paidAmount || 0)), 0);

  const totalOutstandingReceivable = receivableEntries
    .filter(e => e.transactionType === 'RECEIVABLE')
    .reduce((sum, e) => sum + Math.max(0, e.amount - (e.paidAmount || 0)), 0);

  // Filtered transactions
  const filteredTransactions = expenses.filter(e => {
    const matchesSearch = e.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      e.category.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (e.notes && e.notes.toLowerCase().includes(searchQuery.toLowerCase()));

    const matchesCat = selectedCategory === 'ALL' || e.category === selectedCategory;

    return matchesSearch && matchesCat;
  });

  const handleOpenRepayModal = (entry: ExpenseEntry) => {
    setRepaymentTarget(entry);
    const remaining = Math.max(0, entry.amount - (entry.paidAmount || 0));
    setRepayAmountInput(remaining.toString());
  };

  const handleConfirmRepayment = () => {
    if (!repaymentTarget) return;
    const amount = parseFloat(repayAmountInput.replace(/[,،]/g, '')) || 0;
    if (amount <= 0) return;
    recordDebtRepayment(repaymentTarget.id, amount);
    setRepaymentTarget(null);
    setRepayAmountInput('');
  };

  return (
    <div className="space-y-5 pb-24 max-w-7xl mx-auto">
      {/* Top Summary Bento Cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
        {/* Income Card */}
        <div className="glass-panel p-4 rounded-2xl border border-white/5 space-y-1">
          <div className="flex items-center justify-between text-xs text-slate-400">
            <span>درآمد کل ماه</span>
            <div className="w-6 h-6 rounded-lg bg-emerald-500/10 text-emerald-400 flex items-center justify-center">
              <ArrowDownLeft className="w-3.5 h-3.5" />
            </div>
          </div>
          <div className="text-base sm:text-xl font-bold text-slate-100">
            {formatFinancialAmount(totalIncome, preferences.defaultCurrency)}
          </div>
          <div className="text-[11px] text-emerald-400/90 font-medium">
            تراکنش‌های ورودی ثبت‌شده
          </div>
        </div>

        {/* Expense Card */}
        <div className="glass-panel p-4 rounded-2xl border border-white/5 space-y-1">
          <div className="flex items-center justify-between text-xs text-slate-400">
            <span>مخارج کل ماه</span>
            <div className="w-6 h-6 rounded-lg bg-rose-500/10 text-rose-400 flex items-center justify-center">
              <ArrowUpRight className="w-3.5 h-3.5" />
            </div>
          </div>
          <div className="text-base sm:text-xl font-bold text-slate-100">
            {formatFinancialAmount(totalExpense, preferences.defaultCurrency)}
          </div>
          <div className="text-[11px] text-rose-400/90 font-medium">
            مجموع هزینه‌های خروجی
          </div>
        </div>

        {/* Net Balance Card */}
        <div className="glass-panel p-4 rounded-2xl border border-white/5 space-y-1">
          <div className="flex items-center justify-between text-xs text-slate-400">
            <span>تراز و پس‌انداز</span>
            <div className="w-6 h-6 rounded-lg bg-teal-500/10 text-teal-400 flex items-center justify-center">
              <Wallet className="w-3.5 h-3.5" />
            </div>
          </div>
          <div className={`text-base sm:text-xl font-bold ${netSavings >= 0 ? 'text-teal-300' : 'text-amber-400'}`}>
            {formatFinancialAmount(netSavings, preferences.defaultCurrency)}
          </div>
          <div className="text-[11px] text-slate-400">
            درآمد منهای مخارج جاری
          </div>
        </div>

        {/* Safe-to-Spend Card */}
        <div className="glass-panel p-4 rounded-2xl border border-teal-500/20 bg-teal-950/20 space-y-1">
          <div className="flex items-center justify-between text-xs text-teal-300">
            <span className="font-semibold">سقف خرج امن امروز</span>
            <div className="w-6 h-6 rounded-lg bg-teal-500/20 text-teal-300 flex items-center justify-center">
              <Sparkles className="w-3.5 h-3.5" />
            </div>
          </div>
          <div className="text-base sm:text-xl font-extrabold text-teal-200">
            {formatFinancialAmount(safeToSpend.safeDailyAmount, preferences.defaultCurrency)}
          </div>
          <div className="text-[11px] text-teal-300/80">
            {toPersianDigits(safeToSpend.daysRemainingInMonth)} روز باقیمانده تا پایان ماه
          </div>
        </div>
      </div>

      {/* Action Bar & Subtabs */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3">
        {/* Subtabs */}
        <div className="flex rounded-xl bg-slate-900/80 p-1 border border-white/5 text-xs overflow-x-auto">
          <button
            onClick={() => setActiveTab('TRANSACTIONS')}
            className={`px-3 py-1.5 rounded-lg whitespace-nowrap transition cursor-pointer ${
              activeTab === 'TRANSACTIONS'
                ? 'bg-teal-500/20 text-teal-300 font-bold border border-teal-500/30'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            تراکنش‌ها ({toPersianDigits(expenses.length)})
          </button>
          <button
            onClick={() => setActiveTab('DEBTS')}
            className={`px-3 py-1.5 rounded-lg whitespace-nowrap transition cursor-pointer ${
              activeTab === 'DEBTS'
                ? 'bg-amber-500/20 text-amber-300 font-bold border border-amber-500/30'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            طلب و بدهی ({toPersianDigits(debtEntries.length + receivableEntries.length)})
          </button>
          <button
            onClick={() => setActiveTab('ACCOUNTS')}
            className={`px-3 py-1.5 rounded-lg whitespace-nowrap transition cursor-pointer ${
              activeTab === 'ACCOUNTS'
                ? 'bg-cyan-500/20 text-cyan-300 font-bold border border-cyan-500/30'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            حساب‌ها و انتقال
          </button>
          <button
            onClick={() => setActiveTab('TRENDS')}
            className={`px-3 py-1.5 rounded-lg whitespace-nowrap transition cursor-pointer ${
              activeTab === 'TRENDS'
                ? 'bg-purple-500/20 text-purple-300 font-bold border border-purple-500/30'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            روندها و بودجه
          </button>
        </div>

        {/* Action Buttons */}
        <div className="flex items-center gap-2">
          <button
            onClick={onOpenSmsScanner}
            className="flex-1 sm:flex-initial px-3.5 py-2 rounded-xl bg-slate-800/80 hover:bg-slate-700 text-xs font-semibold text-teal-300 border border-teal-500/30 hover:border-teal-500/50 transition flex items-center justify-center gap-1.5 cursor-pointer shadow-lg shadow-teal-500/5"
          >
            <CreditCard className="w-4 h-4" />
            <span>اسکنر پیامک بانکی</span>
          </button>
          <button
            onClick={onOpenAddExpense}
            className="flex-1 sm:flex-initial px-4 py-2 rounded-xl glass-button-primary text-xs font-bold flex items-center justify-center gap-1.5 cursor-pointer"
          >
            <Plus className="w-4 h-4" />
            <span>ثبت تراکنش</span>
          </button>
        </div>
      </div>

      {/* Tab 1: Transactions List */}
      {activeTab === 'TRANSACTIONS' && (
        <div className="space-y-3">
          {/* Filters Bar */}
          <div className="glass-panel p-3 rounded-xl border border-white/5 flex flex-col sm:flex-row gap-2">
            <div className="relative flex-1">
              <Search className="w-4 h-4 text-slate-400 absolute right-3 top-2.5" />
              <input
                type="text"
                value={searchQuery}
                onChange={e => setSearchQuery(e.target.value)}
                placeholder="جستجو در عنوان، دسته‌بندی یا یادداشت..."
                className="w-full rounded-lg bg-slate-900/60 border border-white/5 pr-9 pl-3 py-1.5 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-teal-500/40"
              />
            </div>
            <div className="flex items-center gap-2 overflow-x-auto">
              <Filter className="w-4 h-4 text-slate-400 shrink-0" />
              <button
                onClick={() => setSelectedCategory('ALL')}
                className={`px-2.5 py-1 text-xs rounded-lg whitespace-nowrap transition cursor-pointer ${
                  selectedCategory === 'ALL'
                    ? 'bg-teal-500/20 text-teal-300 font-semibold border border-teal-500/30'
                    : 'bg-slate-800/40 text-slate-400 hover:text-slate-200'
                }`}
              >
                همه دسته‌ها
              </button>
              {preferences.customExpenseCategories.map((cat, i) => (
                <button
                  key={i}
                  onClick={() => setSelectedCategory(cat)}
                  className={`px-2.5 py-1 text-xs rounded-lg whitespace-nowrap transition cursor-pointer ${
                    selectedCategory === cat
                      ? 'bg-teal-500/20 text-teal-300 font-semibold border border-teal-500/30'
                      : 'bg-slate-800/40 text-slate-400 hover:text-slate-200'
                  }`}
                >
                  {cat}
                </button>
              ))}
            </div>
          </div>

          {/* Transaction Cards */}
          {filteredTransactions.length === 0 ? (
            <div className="glass-panel p-10 rounded-2xl text-center space-y-2 border border-white/5">
              <Wallet className="w-10 h-10 text-slate-500 mx-auto" />
              <p className="text-sm font-semibold text-slate-300">هیچ تراکنشی یافت نشد</p>
              <p className="text-xs text-slate-500">
                می‌توانید با دکمه «ثبت تراکنش» یا اسکن پیامک بانکی، اولین تراکنش خود را ثبت کنید.
              </p>
            </div>
          ) : (
            <div className="space-y-2">
              {filteredTransactions.map(tx => {
                const isIncome = tx.transactionType === 'INCOME';
                const isTransfer = tx.transactionType === 'TRANSFER';
                const isDebt = tx.transactionType === 'DEBT' || tx.transactionType === 'DEBT_SETTLED';
                const isRec = tx.transactionType === 'RECEIVABLE' || tx.transactionType === 'RECEIVABLE_SETTLED';

                return (
                  <div
                    key={tx.id}
                    className="glass-panel glass-card-hover p-3.5 rounded-xl border border-white/5 flex items-center justify-between gap-3"
                  >
                    <div className="flex items-center gap-3">
                      {/* Icon */}
                      <div
                        className={`w-10 h-10 rounded-xl flex items-center justify-center shrink-0 ${
                          isIncome
                            ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                            : isTransfer
                            ? 'bg-cyan-500/10 text-cyan-400 border border-cyan-500/20'
                            : isDebt
                            ? 'bg-amber-500/10 text-amber-400 border border-amber-500/20'
                            : isRec
                            ? 'bg-purple-500/10 text-purple-400 border border-purple-500/20'
                            : 'bg-rose-500/10 text-rose-400 border border-rose-500/20'
                        }`}
                      >
                        {isIncome && <ArrowDownLeft className="w-5 h-5" />}
                        {isTransfer && <RefreshCw className="w-5 h-5" />}
                        {isDebt && <Coins className="w-5 h-5" />}
                        {isRec && <HandCoins className="w-5 h-5" />}
                        {!isIncome && !isTransfer && !isDebt && !isRec && <ArrowUpRight className="w-5 h-5" />}
                      </div>

                      {/* Title & metadata */}
                      <div className="space-y-0.5">
                        <div className="flex items-center gap-2">
                          <h3 className="text-xs sm:text-sm font-semibold text-slate-100">
                            {tx.title}
                          </h3>
                          <span className="text-[10px] px-1.5 py-0.5 rounded-md bg-slate-800 text-slate-400 border border-white/5">
                            {tx.category}
                          </span>
                          {tx.inputSource === 'SMS_SCANNER' && (
                            <span className="text-[9px] px-1.5 py-0.2 rounded bg-teal-500/10 text-teal-300 border border-teal-500/20">
                              پیامک
                            </span>
                          )}
                        </div>
                        <div className="flex items-center gap-2 text-[11px] text-slate-400">
                          <span className="flex items-center gap-1">
                            <Calendar className="w-3 h-3 text-slate-500" />
                            {formatEpochDay(tx.localEpochDay, preferences.calendarDisplay)}
                          </span>
                          {tx.notes && (
                            <span className="truncate max-w-[200px] text-slate-500 hidden sm:inline">
                              • {tx.notes}
                            </span>
                          )}
                        </div>
                      </div>
                    </div>

                    {/* Amount & Actions */}
                    <div className="flex items-center gap-3">
                      <div className="text-left">
                        <div
                          className={`text-xs sm:text-sm font-bold ${
                            isIncome
                              ? 'text-emerald-400'
                              : isTransfer
                              ? 'text-cyan-300'
                              : isDebt
                              ? 'text-amber-400'
                              : isRec
                              ? 'text-purple-300'
                              : 'text-slate-100'
                          }`}
                        >
                          {isIncome ? '+' : isTransfer ? '' : '-'}{formatFinancialAmount(tx.amount, tx.currency)}
                        </div>
                        {isTransfer && tx.sourceAccount && tx.destinationAccount && (
                          <div className="text-[10px] text-slate-400">
                            {tx.sourceAccount} ← {tx.destinationAccount}
                          </div>
                        )}
                      </div>

                      <button
                        onClick={() => deleteExpense(tx.id)}
                        title="حذف تراکنش"
                        className="p-1.5 rounded-lg text-slate-500 hover:text-rose-400 hover:bg-rose-500/10 transition cursor-pointer"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}

      {/* Tab 2: Debts & Receivables */}
      {activeTab === 'DEBTS' && (
        <div className="space-y-4">
          {/* Summary */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div className="glass-panel p-4 rounded-xl border border-amber-500/20 space-y-1">
              <span className="text-xs text-amber-300 font-semibold flex items-center gap-1.5">
                <Coins className="w-4 h-4" />
                کل بدهی‌های باقیمانده شما
              </span>
              <div className="text-xl font-bold text-slate-100">
                {formatFinancialAmount(totalOutstandingDebt, 'TOMAN')}
              </div>
              <p className="text-[11px] text-slate-400">
                مبالغی که باید به بانک یا اشخاص پرداخت و تسویه کنید.
              </p>
            </div>

            <div className="glass-panel p-4 rounded-xl border border-purple-500/20 space-y-1">
              <span className="text-xs text-purple-300 font-semibold flex items-center gap-1.5">
                <HandCoins className="w-4 h-4" />
                کل طلب‌های باقیمانده شما
              </span>
              <div className="text-xl font-bold text-slate-100">
                {formatFinancialAmount(totalOutstandingReceivable, 'TOMAN')}
              </div>
              <p className="text-[11px] text-slate-400">
                مبالغی که طلبکار هستید و باید دریافت کنید.
              </p>
            </div>
          </div>

          {/* Debts list */}
          <div className="space-y-2">
            <h3 className="text-xs font-bold text-slate-300 px-1">
              فهرست بدهی‌ها و اقساط ({toPersianDigits(debtEntries.length)})
            </h3>
            {debtEntries.length === 0 ? (
              <div className="glass-panel p-6 rounded-xl text-center text-xs text-slate-500">
                هیچ بدهی فعالی ثبت نشده است.
              </div>
            ) : (
              debtEntries.map(entry => {
                const paid = entry.paidAmount || 0;
                const remaining = Math.max(0, entry.amount - paid);
                const percent = Math.min(100, Math.round((paid / entry.amount) * 100));
                const isSettled = remaining <= 0;

                return (
                  <div
                    key={entry.id}
                    className="glass-panel p-4 rounded-xl border border-white/5 space-y-2.5"
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <div className="w-8 h-8 rounded-lg bg-amber-500/10 text-amber-400 flex items-center justify-center">
                          <Coins className="w-4 h-4" />
                        </div>
                        <div>
                          <h4 className="text-xs sm:text-sm font-semibold text-slate-100">
                            {entry.title}
                          </h4>
                          <span className="text-[11px] text-slate-400">
                            {entry.notes || 'بدهی ثبت‌شده'}
                          </span>
                        </div>
                      </div>
                      <div className="text-left">
                        <div className="text-xs sm:text-sm font-bold text-slate-200">
                          {formatFinancialAmount(entry.amount, 'TOMAN')}
                        </div>
                        <span
                          className={`text-[10px] px-2 py-0.5 rounded-full font-medium ${
                            isSettled
                              ? 'bg-emerald-500/20 text-emerald-300'
                              : 'bg-amber-500/20 text-amber-300'
                          }`}
                        >
                          {isSettled ? 'تسویه کامل' : `مانده: ${formatCompactCurrency(remaining)}`}
                        </span>
                      </div>
                    </div>

                    {/* Progress Bar */}
                    <div className="space-y-1">
                      <div className="flex justify-between text-[11px] text-slate-400">
                        <span>پرداخت‌شده: {formatCompactCurrency(paid)}</span>
                        <span>{toPersianDigits(percent)}٪</span>
                      </div>
                      <div className="w-full h-1.5 rounded-full bg-slate-800 overflow-hidden">
                        <div
                          className="h-full bg-amber-400 rounded-full transition-all duration-300"
                          style={{ width: `${percent}%` }}
                        />
                      </div>
                    </div>

                    {/* Action button */}
                    {!isSettled && (
                      <div className="pt-1 flex justify-end">
                        <button
                          onClick={() => handleOpenRepayModal(entry)}
                          className="px-3 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-xs font-semibold text-teal-300 border border-teal-500/20 transition cursor-pointer"
                        >
                          ثبت پرداخت قسط / تسویه
                        </button>
                      </div>
                    )}
                  </div>
                );
              })
            )}
          </div>

          {/* Receivables list */}
          <div className="space-y-2 pt-2">
            <h3 className="text-xs font-bold text-slate-300 px-1">
              فهرست طلب‌ها و استقراض به دیگران ({toPersianDigits(receivableEntries.length)})
            </h3>
            {receivableEntries.length === 0 ? (
              <div className="glass-panel p-6 rounded-xl text-center text-xs text-slate-500">
                هیچ طلب فعالی ثبت نشده است.
              </div>
            ) : (
              receivableEntries.map(entry => {
                const paid = entry.paidAmount || 0;
                const remaining = Math.max(0, entry.amount - paid);
                const percent = Math.min(100, Math.round((paid / entry.amount) * 100));
                const isSettled = remaining <= 0;

                return (
                  <div
                    key={entry.id}
                    className="glass-panel p-4 rounded-xl border border-white/5 space-y-2.5"
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <div className="w-8 h-8 rounded-lg bg-purple-500/10 text-purple-400 flex items-center justify-center">
                          <HandCoins className="w-4 h-4" />
                        </div>
                        <div>
                          <h4 className="text-xs sm:text-sm font-semibold text-slate-100">
                            {entry.title}
                          </h4>
                          <span className="text-[11px] text-slate-400">
                            {entry.notes || 'طلب ثبت‌شده'}
                          </span>
                        </div>
                      </div>
                      <div className="text-left">
                        <div className="text-xs sm:text-sm font-bold text-slate-200">
                          {formatFinancialAmount(entry.amount, 'TOMAN')}
                        </div>
                        <span
                          className={`text-[10px] px-2 py-0.5 rounded-full font-medium ${
                            isSettled
                              ? 'bg-emerald-500/20 text-emerald-300'
                              : 'bg-purple-500/20 text-purple-300'
                          }`}
                        >
                          {isSettled ? 'تسویه کامل' : `مانده: ${formatCompactCurrency(remaining)}`}
                        </span>
                      </div>
                    </div>

                    {/* Progress Bar */}
                    <div className="space-y-1">
                      <div className="flex justify-between text-[11px] text-slate-400">
                        <span>دریافت‌شده: {formatCompactCurrency(paid)}</span>
                        <span>{toPersianDigits(percent)}٪</span>
                      </div>
                      <div className="w-full h-1.5 rounded-full bg-slate-800 overflow-hidden">
                        <div
                          className="h-full bg-purple-400 rounded-full transition-all duration-300"
                          style={{ width: `${percent}%` }}
                        />
                      </div>
                    </div>

                    {!isSettled && (
                      <div className="pt-1 flex justify-end">
                        <button
                          onClick={() => handleOpenRepayModal(entry)}
                          className="px-3 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-xs font-semibold text-teal-300 border border-teal-500/20 transition cursor-pointer"
                        >
                          ثبت وصول طلب
                        </button>
                      </div>
                    )}
                  </div>
                );
              })
            )}
          </div>
        </div>
      )}

      {/* Tab 3: Accounts & Transfers */}
      {activeTab === 'ACCOUNTS' && (
        <div className="space-y-4">
          <div className="glass-panel p-4 rounded-xl border border-white/5 space-y-3">
            <h3 className="text-xs font-bold text-slate-200">
              حساب‌های بانکی و کارت‌های مالی تعریف‌شده
            </h3>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
              {preferences.financialAccounts.map((acc, i) => (
                <div key={i} className="p-3 rounded-xl bg-slate-900/60 border border-white/5 space-y-1">
                  <div className="flex items-center gap-2 text-xs font-semibold text-teal-300">
                    <CreditCard className="w-4 h-4" />
                    <span>{acc}</span>
                  </div>
                  <span className="text-[11px] text-slate-400 block">
                    آماده برای انتقال وجه و خرید
                  </span>
                </div>
              ))}
            </div>
            <p className="text-[11px] text-slate-400">
              * انتقال بین این حساب‌ها صرفاً جابجایی موجودی بوده و به عنوان درآمد یا هزینه مجدد شمرده نمی‌شود.
            </p>
          </div>

          {/* Transfers history */}
          <div className="space-y-2">
            <h4 className="text-xs font-bold text-slate-300">تاریخچه انتقال وجه بین حساب‌ها</h4>
            {expenses.filter(e => e.transactionType === 'TRANSFER').length === 0 ? (
              <div className="glass-panel p-6 rounded-xl text-center text-xs text-slate-500">
                هیچ انتقال حسابی ثبت نشده است.
              </div>
            ) : (
              expenses
                .filter(e => e.transactionType === 'TRANSFER')
                .map(tx => (
                  <div
                    key={tx.id}
                    className="glass-panel p-3 rounded-xl border border-white/5 flex items-center justify-between text-xs"
                  >
                    <div className="flex items-center gap-2.5">
                      <RefreshCw className="w-4 h-4 text-cyan-400" />
                      <div>
                        <span className="font-semibold text-slate-200 block">{tx.title}</span>
                        <span className="text-[10px] text-slate-400">
                          {formatEpochDay(tx.localEpochDay, preferences.calendarDisplay)}
                        </span>
                      </div>
                    </div>
                    <span className="font-bold text-cyan-300">
                      {formatFinancialAmount(tx.amount, 'TOMAN')}
                    </span>
                  </div>
                ))
            )}
          </div>
        </div>
      )}

      {/* Tab 4: Spending Trends & Budget */}
      {activeTab === 'TRENDS' && (
        <div className="space-y-4">
          <div className="glass-panel p-5 rounded-2xl border border-white/5 space-y-4">
            <div className="flex items-center justify-between">
              <div className="space-y-0.5">
                <h3 className="text-sm font-bold text-slate-100 flex items-center gap-2">
                  <TrendingUp className="w-4 h-4 text-teal-400" />
                  تحلیل بودجه و پیش‌بینی خرج امن (Safe-to-Spend)
                </h3>
                <p className="text-xs text-slate-400">
                  فرمول استاندارد Personal Life OS با ذخیره بافر ایمنی ۱۰ درصدی
                </p>
              </div>
              <span
                className={`text-xs px-2.5 py-1 rounded-full font-bold ${
                  safeToSpend.budgetHealth === 'HEALTHY'
                    ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
                    : safeToSpend.budgetHealth === 'MODERATE'
                    ? 'bg-amber-500/20 text-amber-300 border border-amber-500/30'
                    : 'bg-rose-500/20 text-rose-300 border border-rose-500/30'
                }`}
              >
                {safeToSpend.budgetHealth === 'HEALTHY' && 'وضعیت بودجه: عالی'}
                {safeToSpend.budgetHealth === 'MODERATE' && 'وضعیت بودجه: متناسب'}
                {safeToSpend.budgetHealth === 'CRITICAL' && 'هشدار: مصرف بالا'}
                {safeToSpend.budgetHealth === 'OVER_BUDGET' && 'کسری بودجه'}
              </span>
            </div>

            {/* Budget Progress Meter */}
            <div className="space-y-2 p-4 rounded-xl bg-slate-900/60 border border-white/5">
              <div className="flex justify-between text-xs">
                <span className="text-slate-400">
                  مجموع خرج شده: {formatCompactCurrency(totalExpense)}
                </span>
                <span className="text-slate-300 font-semibold">
                  سقف هدف ماه: {formatCompactCurrency(preferences.monthlyBudgetTarget)}
                </span>
              </div>
              <div className="w-full h-3 rounded-full bg-slate-800 overflow-hidden">
                <div
                  className={`h-full rounded-full transition-all duration-500 ${
                    safeToSpend.budgetHealth === 'HEALTHY'
                      ? 'bg-gradient-to-r from-teal-500 to-emerald-400'
                      : safeToSpend.budgetHealth === 'MODERATE'
                      ? 'bg-gradient-to-r from-teal-500 to-amber-400'
                      : 'bg-gradient-to-r from-amber-500 to-rose-500'
                  }`}
                  style={{
                    width: `${Math.min(100, Math.round((totalExpense / preferences.monthlyBudgetTarget) * 100))}%`
                  }}
                />
              </div>
              <div className="flex justify-between text-[11px] text-slate-500">
                <span>مانده بودجه تا پایان ماه: {formatCompactCurrency(Math.max(0, safeToSpend.remainingBudget))}</span>
                <span>روزهای باقیمانده ماه: {toPersianDigits(safeToSpend.daysRemainingInMonth)} روز</span>
              </div>
            </div>

            {/* 7-Day Spending Bar Chart Simulation */}
            <div className="space-y-2 pt-2">
              <h4 className="text-xs font-bold text-slate-300">نمودار روند مخارج ۷ روز اخیر</h4>
              <div className="grid grid-cols-7 gap-2 items-end h-32 p-3 bg-slate-900/50 rounded-xl border border-white/5">
                {[6, 5, 4, 3, 2, 1, 0].map(offset => {
                  const dayEpoch = todayEpoch - offset;
                  const dayExpense = expenses
                    .filter(e => e.transactionType === 'EXPENSE' && e.localEpochDay === dayEpoch)
                    .reduce((sum, e) => sum + e.amount, 0);

                  const maxDaily = Math.max(1, safeToSpend.safeDailyAmount * 2);
                  const barHeight = Math.min(100, Math.round((dayExpense / maxDaily) * 100));

                  return (
                    <div key={offset} className="flex flex-col items-center gap-1 h-full justify-end">
                      <div
                        className="w-full rounded-t-md bg-teal-500/30 hover:bg-teal-500/50 border-t border-teal-400/40 transition"
                        style={{ height: `${Math.max(8, barHeight)}%` }}
                        title={`${formatCompactCurrency(dayExpense)}`}
                      />
                      <span className="text-[10px] text-slate-400">
                        {offset === 0 ? 'امروز' : `${toPersianDigits(offset)}ر پیش`}
                      </span>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Partial Repayment / Settlement Modal */}
      {repaymentTarget && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-md">
          <div className="glass-panel w-full max-w-sm rounded-2xl border border-white/10 p-5 space-y-4">
            <h3 className="text-sm font-bold text-slate-100">
              ثبت پرداخت و تسویه: {repaymentTarget.title}
            </h3>
            <div>
              <label className="text-xs text-slate-400 block mb-1">
                مبلغ پرداختی (تومان):
              </label>
              <input
                type="text"
                value={repayAmountInput}
                onChange={e => setRepayAmountInput(e.target.value)}
                className="w-full rounded-xl bg-slate-900 border border-white/10 px-3 py-2 text-sm font-bold text-slate-100"
              />
            </div>
            <div className="flex justify-end gap-2">
              <button
                onClick={() => setRepaymentTarget(null)}
                className="px-3 py-1.5 text-xs text-slate-400 hover:text-white"
              >
                انصراف
              </button>
              <button
                onClick={handleConfirmRepayment}
                className="px-4 py-1.5 rounded-xl glass-button-primary text-xs font-semibold"
              >
                تأیید پرداخت
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
