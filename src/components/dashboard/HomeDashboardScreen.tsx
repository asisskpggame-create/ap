import React from 'react';
import {
  Sparkles,
  CheckCircle2,
  Circle,
  Flame,
  Droplets,
  Moon,
  Activity,
  Wallet,
  ArrowUpRight,
  ArrowDownLeft,
  Calendar,
  CreditCard,
  Plus,
  Mic,
  ChevronLeft,
  Check
} from 'lucide-react';
import { useLifeOs } from '../../context/LifeOsContext';
import {
  formatFinancialAmount,
  formatCompactCurrency,
  formatEpochDay,
  calculateSafeToSpend,
  currentLocalEpochDay,
  epochDayToJalali,
  jalaliMonthNamesFa,
  toPersianDigits
} from '../../utils/persianDateUtils';

interface HomeDashboardScreenProps {
  onOpenAddExpense: () => void;
  onOpenAddTask: () => void;
  onOpenSmsScanner: () => void;
  onOpenVoice: () => void;
}

export const HomeDashboardScreen: React.FC<HomeDashboardScreenProps> = ({
  onOpenAddExpense,
  onOpenAddTask,
  onOpenSmsScanner,
  onOpenVoice
}) => {
  const {
    tasks,
    toggleTaskCompleted,
    expenses,
    habits,
    habitRecords,
    consumption,
    sleepRecords,
    exerciseRecords,
    preferences,
    setActiveSection
  } = useLifeOs();

  const todayEpoch = currentLocalEpochDay();
  const jToday = epochDayToJalali(todayEpoch);

  // 1. Next Immediate Action: Critical/High uncompleted task for today
  const pendingTasks = tasks.filter(t => t.status !== 'COMPLETED' && t.status !== 'ARCHIVED');
  const nextAction = pendingTasks.find(t => t.priority === 'CRITICAL') ||
    pendingTasks.find(t => t.priority === 'HIGH') ||
    pendingTasks[0];

  // 2. Financial calculation
  const totalExpense = expenses
    .filter(e => e.transactionType === 'EXPENSE')
    .reduce((sum, e) => sum + e.amount, 0);

  const todayExpense = expenses
    .filter(e => e.transactionType === 'EXPENSE' && e.localEpochDay === todayEpoch)
    .reduce((sum, e) => sum + e.amount, 0);

  const safeToSpend = calculateSafeToSpend(
    preferences.monthlyBudgetTarget,
    totalExpense,
    todayEpoch
  );

  // 3. Habits today
  const completedHabitsToday = habits.filter(h =>
    habitRecords.some(r => r.habitId === h.id && r.localEpochDay === todayEpoch)
  ).length;

  const habitRate = habits.length > 0 ? Math.round((completedHabitsToday / habits.length) * 100) : 0;

  // 4. Water today
  const todayWater = consumption
    .filter(c => c.category === 'WATER' && c.localEpochDay === todayEpoch)
    .reduce((sum, c) => sum + c.quantity, 0);

  const waterPercent = Math.min(100, Math.round((todayWater / preferences.dailyWaterTargetMl) * 100));

  // 5. Days in Jalali month for Heatmap
  const daysInJMonth = jToday.month <= 6 ? 31 : jToday.month <= 11 ? 30 : 29;

  return (
    <div className="space-y-5 pb-24 max-w-7xl mx-auto">
      {/* Hero Executive Header */}
      <div className="glass-panel p-5 sm:p-6 rounded-3xl border border-white/10 relative overflow-hidden">
        <div className="relative z-10 flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
          <div className="space-y-1">
            <div className="flex items-center gap-2">
              <span className="text-xs px-2.5 py-0.5 rounded-full bg-teal-500/20 text-teal-300 border border-teal-500/30 font-semibold">
                وضعیت روزانه فعال
              </span>
              <span className="text-xs text-slate-400">
                {formatEpochDay(todayEpoch, preferences.calendarDisplay)}
              </span>
            </div>
            <h2 className="text-xl sm:text-2xl font-black text-slate-100 tracking-tight">
              سلام، روزت پربار و سرشار از تمرکز!
            </h2>
            <p className="text-xs sm:text-sm text-slate-400">
              سیستم عامل شما همه چیز را برای یک روز منظم آماده کرده است.
            </p>
          </div>

          {/* Quick Action Pill Bar */}
          <div className="flex flex-wrap items-center gap-2 w-full md:w-auto">
            <button
              onClick={onOpenVoice}
              className="flex-1 sm:flex-initial px-3.5 py-2 rounded-xl bg-slate-800/90 hover:bg-slate-700 text-slate-200 text-xs font-semibold flex items-center justify-center gap-1.5 border border-white/10 transition cursor-pointer"
            >
              <Mic className="w-4 h-4 text-amber-400" />
              <span>ثبت صوتی هوشمند</span>
            </button>
            <button
              onClick={onOpenSmsScanner}
              className="flex-1 sm:flex-initial px-3.5 py-2 rounded-xl bg-slate-800/90 hover:bg-slate-700 text-teal-300 text-xs font-semibold flex items-center justify-center gap-1.5 border border-teal-500/20 transition cursor-pointer"
            >
              <CreditCard className="w-4 h-4" />
              <span>اسکنر پیامک</span>
            </button>
            <button
              onClick={onOpenAddExpense}
              className="flex-1 sm:flex-initial px-3.5 py-2 rounded-xl glass-button-primary text-xs font-bold flex items-center justify-center gap-1.5 cursor-pointer"
            >
              <Plus className="w-4 h-4" />
              <span>ثبت تراکنش</span>
            </button>
          </div>
        </div>

        {/* Ambient subtle glow */}
        <div className="absolute -right-16 -top-16 w-56 h-56 rounded-full bg-teal-500/10 blur-3xl pointer-events-none" />
      </div>

      {/* Bento Grid Layout */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {/* Bento 1: Next Immediate Action (اقدام متمرکز بعدی) */}
        <div className="glass-panel p-5 rounded-2xl border border-teal-500/30 bg-teal-950/15 flex flex-col justify-between space-y-4">
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-teal-300 flex items-center gap-1.5">
                <Sparkles className="w-4 h-4 text-teal-400" />
                اقدام متمرکز بعدی (Next Action)
              </span>
              {nextAction && (
                <span className="text-[10px] px-2 py-0.5 rounded-full bg-rose-500/20 text-rose-300 border border-rose-500/30 font-semibold">
                  {nextAction.priority === 'CRITICAL' ? 'فوری و مهم' : 'دارای اولویت'}
                </span>
              )}
            </div>

            {nextAction ? (
              <div className="space-y-1.5 pt-1">
                <h3 className="text-base font-bold text-slate-100">
                  {nextAction.title}
                </h3>
                {nextAction.description && (
                  <p className="text-xs text-slate-300 leading-relaxed">
                    {nextAction.description}
                  </p>
                )}
                <div className="text-[11px] text-teal-400/80 pt-1">
                  دسته‌بندی: {nextAction.category}
                </div>
              </div>
            ) : (
              <div className="py-6 text-center text-xs text-slate-400">
                همه تسک‌های دارای اولویت انجام شده‌اند! عالی هستید.
              </div>
            )}
          </div>

          {nextAction && (
            <div className="pt-2 flex items-center justify-between border-t border-white/5">
              <button
                onClick={() => toggleTaskCompleted(nextAction.id)}
                className="px-3.5 py-1.5 rounded-xl bg-teal-500/20 hover:bg-teal-500/30 text-teal-200 border border-teal-500/40 text-xs font-semibold flex items-center gap-1.5 transition cursor-pointer"
              >
                <Check className="w-4 h-4" />
                <span>علامت‌گذاری به عنوان انجام‌شده</span>
              </button>
              <button
                onClick={() => setActiveSection('GOALS_AND_HABITS')}
                className="text-xs text-slate-400 hover:text-white flex items-center gap-0.5 cursor-pointer"
              >
                <span>مشاهده همه</span>
                <ChevronLeft className="w-3.5 h-3.5" />
              </button>
            </div>
          )}
        </div>

        {/* Bento 2: Safe-to-Spend & Budget Glance */}
        <div className="glass-panel p-5 rounded-2xl border border-white/10 flex flex-col justify-between space-y-4">
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-slate-300 flex items-center gap-1.5">
                <Wallet className="w-4 h-4 text-teal-400" />
                پیش‌بینی خرج امن روزانه (Safe-to-Spend)
              </span>
              <span className="text-[10px] px-1.5 py-0.5 rounded bg-slate-800 text-teal-300 border border-teal-500/20">
                تخمینی با ۱۰٪ بافر
              </span>
            </div>

            <div className="pt-1">
              <div className="text-2xl sm:text-3xl font-extrabold text-teal-300">
                {formatCompactCurrency(safeToSpend.safeDailyAmount)}
              </div>
              <p className="text-xs text-slate-400 mt-1">
                سقف مجاز مخارج امروز بدون نقض بودجه ماهانه {formatCompactCurrency(preferences.monthlyBudgetTarget)}
              </p>
            </div>

            <div className="p-2.5 rounded-xl bg-slate-900/60 border border-white/5 flex items-center justify-between text-xs">
              <span className="text-slate-400">مخارج ثبت‌شده امروز:</span>
              <span className="font-bold text-slate-200">
                {formatFinancialAmount(todayExpense, 'TOMAN')}
              </span>
            </div>
          </div>

          <div className="pt-2 flex items-center justify-between border-t border-white/5 text-xs">
            <span className="text-slate-400">
              {toPersianDigits(safeToSpend.daysRemainingInMonth)} روز مانده تا پایان ماه
            </span>
            <button
              onClick={() => setActiveSection('DAILY_LOG')}
              className="text-teal-400 hover:text-teal-300 font-semibold flex items-center gap-0.5 cursor-pointer"
            >
              <span>جزئیات مالی</span>
              <ChevronLeft className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>

        {/* Bento 3: Habits & Consistency Streak */}
        <div className="glass-panel p-5 rounded-2xl border border-white/10 flex flex-col justify-between space-y-4">
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-slate-300 flex items-center gap-1.5">
                <Flame className="w-4 h-4 text-amber-400" />
                زنجیره عادت‌های روزانه
              </span>
              <span className="text-xs font-bold text-amber-300">
                {toPersianDigits(habitRate)}٪ تکمیل
              </span>
            </div>

            <div className="pt-1 space-y-2">
              <div className="flex items-center justify-between text-xs text-slate-300">
                <span>پیشرفت امروز:</span>
                <span className="font-bold">
                  {toPersianDigits(completedHabitsToday)} از {toPersianDigits(habits.length)} عادت
                </span>
              </div>
              <div className="w-full h-2 rounded-full bg-slate-800 overflow-hidden">
                <div
                  className="h-full bg-gradient-to-r from-amber-500 to-teal-400 rounded-full transition-all duration-300"
                  style={{ width: `${habitRate}%` }}
                />
              </div>
            </div>

            <div className="space-y-1 pt-1">
              {habits.slice(0, 3).map(h => {
                const isDone = habitRecords.some(
                  r => r.habitId === h.id && r.localEpochDay === todayEpoch
                );
                return (
                  <div
                    key={h.id}
                    className="flex items-center justify-between text-xs p-1.5 rounded-lg bg-slate-900/40"
                  >
                    <span className="text-slate-300 truncate max-w-[180px]">{h.title}</span>
                    <span className={`text-[10px] font-bold ${isDone ? 'text-emerald-400' : 'text-slate-500'}`}>
                      {isDone ? 'انجام شد ✓' : 'در انتظار'}
                    </span>
                  </div>
                );
              })}
            </div>
          </div>

          <div className="pt-2 flex items-center justify-between border-t border-white/5 text-xs">
            <span className="text-slate-400">عادت‌های فعال</span>
            <button
              onClick={() => setActiveSection('GOALS_AND_HABITS')}
              className="text-amber-400 hover:text-amber-300 font-semibold flex items-center gap-0.5 cursor-pointer"
            >
              <span>مشاهده و ثبت</span>
              <ChevronLeft className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>

        {/* Bento 4: Health Glance (Water, Sleep, Workout) */}
        <div className="glass-panel p-5 rounded-2xl border border-white/10 md:col-span-2 lg:col-span-1 flex flex-col justify-between space-y-3">
          <div className="space-y-2">
            <span className="text-xs font-bold text-slate-300 flex items-center gap-1.5">
              <Activity className="w-4 h-4 text-emerald-400" />
              پایش تندرستی و سبک زندگی
            </span>

            <div className="grid grid-cols-3 gap-2 pt-1 text-center">
              {/* Water */}
              <div className="p-2.5 rounded-xl bg-slate-900/60 border border-white/5 space-y-1">
                <Droplets className="w-4 h-4 text-teal-400 mx-auto" />
                <span className="text-[10px] text-slate-400 block">مصرف آب</span>
                <span className="text-xs font-bold text-teal-300 block">
                  {toPersianDigits(todayWater)} ml
                </span>
              </div>

              {/* Sleep */}
              <div className="p-2.5 rounded-xl bg-slate-900/60 border border-white/5 space-y-1">
                <Moon className="w-4 h-4 text-purple-400 mx-auto" />
                <span className="text-[10px] text-slate-400 block">خواب دیشب</span>
                <span className="text-xs font-bold text-purple-300 block">
                  {sleepRecords.length > 0 ? `${toPersianDigits((sleepRecords[0].durationMinutes / 60).toFixed(1))} س` : 'ثبت‌نشده'}
                </span>
              </div>

              {/* Exercise */}
              <div className="p-2.5 rounded-xl bg-slate-900/60 border border-white/5 space-y-1">
                <Activity className="w-4 h-4 text-emerald-400 mx-auto" />
                <span className="text-[10px] text-slate-400 block">ورزش امروز</span>
                <span className="text-xs font-bold text-emerald-300 block">
                  {exerciseRecords.length > 0 ? `${toPersianDigits(exerciseRecords[0].durationMinutes)} د` : '۰ د'}
                </span>
              </div>
            </div>
          </div>

          <div className="pt-2 flex items-center justify-between border-t border-white/5 text-xs">
            <span className="text-slate-400">شاخص‌های زیستی</span>
            <button
              onClick={() => setActiveSection('GOALS_AND_HABITS')}
              className="text-emerald-400 hover:text-emerald-300 font-semibold flex items-center gap-0.5 cursor-pointer"
            >
              <span>ثبت آب و خواب</span>
              <ChevronLeft className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>

        {/* Bento 5: Jalali Calendar Heatmap Card (تقویم جلالی ماه جاری) */}
        <div className="glass-panel p-5 rounded-2xl border border-white/10 md:col-span-2 lg:col-span-2 flex flex-col justify-between space-y-3">
          <div className="space-y-1">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-slate-300 flex items-center gap-1.5">
                <Calendar className="w-4 h-4 text-teal-400" />
                نقشه حرارتی تقویم جلالی ({jalaliMonthNamesFa[jToday.month - 1]} {toPersianDigits(jToday.year)})
              </span>
              <span className="text-[11px] text-slate-400">
                امروز: روز {toPersianDigits(jToday.day)} {jalaliMonthNamesFa[jToday.month - 1]}
              </span>
            </div>
            <p className="text-[11px] text-slate-400">
              نمایش تراکم فعالیت‌ها، ثبت هزینه‌ها و تکمیل عادت‌ها در روزهای ماه
            </p>
          </div>

          {/* Grid of days in the month */}
          <div className="grid grid-cols-7 sm:grid-cols-10 gap-1.5 pt-2">
            {Array.from({ length: daysInJMonth }, (_, idx) => {
              const dayNum = idx + 1;
              const isToday = dayNum === jToday.day;
              const isPast = dayNum <= jToday.day;
              const isFuture = dayNum > jToday.day;

              return (
                <div
                  key={dayNum}
                  title={`روز ${toPersianDigits(dayNum)} ${jalaliMonthNamesFa[jToday.month - 1]}`}
                  className={`p-2 rounded-xl text-center flex flex-col items-center justify-center transition border ${
                    isToday
                      ? 'bg-teal-500/25 border-teal-400 text-teal-200 font-bold shadow-[0_0_12px_rgba(20,184,166,0.3)]'
                      : isPast
                      ? 'bg-slate-900/60 border-white/5 text-slate-300 hover:border-teal-500/20'
                      : 'bg-slate-950/40 border-white/5 text-slate-600'
                  }`}
                >
                  <span className="text-xs font-mono">{toPersianDigits(dayNum)}</span>
                  <div
                    className={`w-1.5 h-1.5 rounded-full mt-1 ${
                      isToday
                        ? 'bg-teal-400 animate-pulse'
                        : isPast
                        ? 'bg-teal-500/40'
                        : 'bg-transparent'
                    }`}
                  />
                </div>
              );
            })}
          </div>

          <div className="pt-2 flex items-center justify-between border-t border-white/5 text-[11px] text-slate-400">
            <span>شنبه تا جمعه تقویم رسمی ایران</span>
            <div className="flex items-center gap-2">
              <span className="flex items-center gap-1">
                <span className="w-2 h-2 rounded-full bg-teal-400 inline-block" /> امروز
              </span>
              <span className="flex items-center gap-1">
                <span className="w-2 h-2 rounded-full bg-teal-500/40 inline-block" /> ثبت‌شده
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
