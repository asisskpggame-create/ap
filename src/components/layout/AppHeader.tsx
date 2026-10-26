import React from 'react';
import {
  Shield,
  ShieldCheck,
  Settings,
  Plus,
  Flame,
  Wallet,
  Sparkles,
  Calendar
} from 'lucide-react';
import { useLifeOs } from '../../context/LifeOsContext';
import { formatEpochDay, currentLocalEpochDay, calculateSafeToSpend, formatCompactCurrency } from '../../utils/persianDateUtils';

interface AppHeaderProps {
  onOpenSettings: () => void;
  onQuickAdd: () => void;
  onOpenVoice: () => void;
}

export const AppHeader: React.FC<AppHeaderProps> = ({
  onOpenSettings,
  onQuickAdd,
  onOpenVoice
}) => {
  const {
    activeSection,
    preferences,
    expenses,
    isVaultLocked,
    lockVault
  } = useLifeOs();

  const todayEpoch = currentLocalEpochDay();
  const dateFormatted = formatEpochDay(todayEpoch, preferences.calendarDisplay);

  // Calculate Safe-to-Spend for this month
  const totalExpensesThisMonth = expenses
    .filter(e => e.transactionType === 'EXPENSE')
    .reduce((sum, e) => sum + e.amount, 0);

  const safeToSpend = calculateSafeToSpend(
    preferences.monthlyBudgetTarget,
    totalExpensesThisMonth,
    todayEpoch
  );

  const sectionTitles: Record<string, { title: string; subtitle: string }> = {
    HOME: { title: 'داشبورد اجرایی', subtitle: 'خلاصه وضعیت هوشمند زندگی امروز' },
    DAILY_LOG: { title: 'مدیریت مالی و تراکنش‌ها', subtitle: 'درآمدها، هزینه‌ها، بدهی‌ها و حساب‌ها' },
    GOALS_AND_HABITS: { title: 'اهداف، عادت‌ها و سلامت', subtitle: 'تسک‌ها، زنجیره عادت و پایش سلامتی' },
    AI_ASSISTANT: { title: 'دستیار هوشمند فارسی', subtitle: 'تحلیل داده‌ها، ثبت صوتی و بررسی هوشمند' }
  };

  const currentInfo = sectionTitles[activeSection] || sectionTitles.HOME;

  return (
    <header className="sticky top-0 z-30 w-full glass-panel border-b border-white/10 px-4 py-3 sm:px-6">
      <div className="max-w-7xl mx-auto flex items-center justify-between gap-3">
        {/* Brand & Section Title */}
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-teal-500 to-emerald-400 p-[1px] shadow-lg shadow-teal-500/20 flex items-center justify-center">
            <div className="w-full h-full bg-[#0b0f17] rounded-[11px] flex items-center justify-center text-teal-400">
              <Sparkles className="w-5 h-5 animate-pulse" />
            </div>
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-base sm:text-lg font-bold text-slate-100 tracking-tight">
                {currentInfo.title}
              </h1>
              <span className="hidden md:inline-flex text-[11px] px-2 py-0.5 rounded-full bg-teal-500/10 text-teal-300 border border-teal-500/20 font-medium">
                Personal Life OS v1.5
              </span>
            </div>
            <p className="text-xs text-slate-400 hidden sm:block">
              {currentInfo.subtitle}
            </p>
          </div>
        </div>

        {/* Center / Right controls */}
        <div className="flex items-center gap-2 sm:gap-3">
          {/* Persian Date Badge */}
          <div className="hidden lg:flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-800/60 border border-white/5 text-xs text-slate-300">
            <Calendar className="w-3.5 h-3.5 text-teal-400" />
            <span>{dateFormatted}</span>
          </div>

          {/* Safe-to-Spend Pill */}
          <div
            title="سقف مخارج امن برای امروز"
            className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-xl bg-teal-950/40 border border-teal-500/30 text-xs font-medium text-teal-300"
          >
            <Wallet className="w-3.5 h-3.5 text-teal-400 shrink-0" />
            <span className="hidden sm:inline text-[11px] text-teal-400/80">خرج امن امروز:</span>
            <span className="font-semibold text-teal-200">
              {formatCompactCurrency(safeToSpend.safeDailyAmount)}
            </span>
          </div>

          {/* Voice Prompt Button */}
          <button
            onClick={onOpenVoice}
            title="ورودی صوتی فارسی"
            className="p-2 sm:px-3 sm:py-1.5 rounded-xl bg-slate-800/80 hover:bg-slate-700/80 border border-white/10 text-slate-200 hover:text-white transition flex items-center gap-1.5 text-xs font-medium cursor-pointer"
          >
            <Flame className="w-4 h-4 text-amber-400" />
            <span className="hidden sm:inline">ثبت سریع صوتی</span>
          </button>

          {/* Quick Add Button */}
          <button
            onClick={onQuickAdd}
            title="افزودن سریع"
            className="p-2 rounded-xl glass-button-primary cursor-pointer flex items-center justify-center shrink-0"
          >
            <Plus className="w-4 h-4" />
          </button>

          {/* Vault Lock button */}
          {preferences.vaultLockEnabled && (
            <button
              onClick={lockVault}
              title="قفل گاوصندوق"
              className="p-2 rounded-xl bg-slate-800/80 hover:bg-slate-700/80 border border-white/10 text-amber-400 hover:text-amber-300 transition cursor-pointer"
            >
              {isVaultLocked ? <Shield className="w-4 h-4" /> : <ShieldCheck className="w-4 h-4" />}
            </button>
          )}

          {/* Settings button */}
          <button
            onClick={onOpenSettings}
            title="تنظیمات سیستم و شخصی‌سازی"
            className="p-2 rounded-xl bg-slate-800/80 hover:bg-slate-700/80 border border-white/10 text-slate-300 hover:text-white transition cursor-pointer"
          >
            <Settings className="w-4 h-4" />
          </button>
        </div>
      </div>
    </header>
  );
};
