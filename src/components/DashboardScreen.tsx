import React from 'react';
import { 
  Sparkles, 
  CheckCircle2, 
  Wallet, 
  HeartPulse, 
  TrendingUp, 
  Mic, 
  Plus,
  ArrowUpRight,
  Flame,
  Calendar,
  ShieldCheck,
  BellRing
} from 'lucide-react';
import { TaskItem, HabitItem, ExpenseItem, HealthLog } from '../types';
import { formatToman } from '../utils/persianUtils';

interface DashboardScreenProps {
  tasks: TaskItem[];
  habits: HabitItem[];
  expenses: ExpenseItem[];
  healthLog: HealthLog;
  onOpenVoiceModal: () => void;
  onOpenNotificationCenter?: () => void;
  unreadNotificationsCount?: number;
  pendingReceiptNotificationsCount?: number;
  onNavigate: (tab: any) => void;
  onToggleTask: (id: string) => void;
  onToggleHabit: (id: string) => void;
}

export const DashboardScreen: React.FC<DashboardScreenProps> = ({
  tasks,
  habits,
  expenses,
  healthLog,
  onOpenVoiceModal,
  onOpenNotificationCenter,
  unreadNotificationsCount = 0,
  pendingReceiptNotificationsCount = 0,
  onNavigate,
  onToggleTask,
  onToggleHabit
}) => {
  const pendingTasks = tasks.filter(t => !t.completed);
  const totalExpenses = expenses.reduce((acc, curr) => acc + curr.amount, 0);

  return (
    <div className="space-y-6 animate-fadeIn">
      {/* Top Greeting & Quick Actions Banner */}
      <div className="glass-card rounded-3xl p-6 relative overflow-hidden bg-gradient-to-l from-slate-900 via-slate-900/90 to-cyan-950/40 border border-cyan-500/20">
        <div className="absolute top-0 left-0 w-64 h-64 bg-cyan-500/10 rounded-full blur-3xl pointer-events-none" />
        <div className="relative z-10 flex flex-col md:flex-row items-start md:items-center justify-between gap-6">
          <div className="space-y-2">
            <div className="inline-flex items-center space-x-2 space-x-reverse px-3 py-1 rounded-full bg-cyan-500/10 border border-cyan-500/20 text-cyan-400 text-xs font-medium">
              <Sparkles className="w-3.5 h-3.5 animate-spin" />
              <span>دستیار هوشمند زندگی و شنونده فیش بانکی فعال است (v1.6.0R)</span>
            </div>
            <h2 className="text-2xl md:text-3xl font-bold text-slate-100 tracking-tight">
              روز به‌خیر، دستیار صوتی و نوتیفیکیشن بانکی آماده است 🌟
            </h2>
            <p className="text-sm text-slate-400 max-w-xl">
              امروز ۱۴۰۵/۰۸/۰۵ — تمام حساب‌ها، وظایف و عادت‌های شما منظم و هماهنگ هستند. فیش‌های دریافتی از نوتیفیکیشن بانک‌ها و پیامک‌ها به صورت لحظه‌ای پایش می‌شوند.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-2.5">
            {onOpenNotificationCenter && (
              <button
                onClick={onOpenNotificationCenter}
                className="flex items-center space-x-2 space-x-reverse px-4 py-3 rounded-2xl bg-slate-900/90 border border-amber-500/40 text-amber-300 text-xs font-bold hover:bg-slate-800 transition shadow-lg shadow-amber-950/20"
              >
                <BellRing className="w-4 h-4 text-amber-400" />
                <span>لاگ نوتیفیکیشن فیش‌ها</span>
                {(pendingReceiptNotificationsCount > 0 || unreadNotificationsCount > 0) && (
                  <span className="px-2 py-0.5 rounded-full bg-amber-500 text-slate-950 text-[10px] font-extrabold">
                    {pendingReceiptNotificationsCount > 0
                      ? `${pendingReceiptNotificationsCount} در انتظار`
                      : `${unreadNotificationsCount} جدید`}
                  </span>
                )}
              </button>
            )}
            <button
              onClick={onOpenVoiceModal}
              className="flex items-center space-x-2 space-x-reverse px-5 py-3 rounded-2xl bg-gradient-to-r from-cyan-500 to-blue-600 text-white font-medium text-sm shadow-lg shadow-cyan-500/25 hover:shadow-cyan-500/40 hover:scale-[1.02] transition"
            >
              <Mic className="w-4 h-4" />
              <span>ضبط ویس / اسکن پیامک</span>
            </button>
            <button
              onClick={() => onNavigate('assistant')}
              className="px-4 py-3 rounded-2xl bg-slate-800/80 border border-slate-700/60 text-slate-200 text-sm font-medium hover:bg-slate-800 hover:border-cyan-500/40 transition"
            >
              پاسخ جمینای AI
            </button>
          </div>
        </div>
      </div>

      {/* Metrics Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Card 1: Tasks */}
        <div 
          onClick={() => onNavigate('tasks')}
          className="glass-card glass-card-hover rounded-2xl p-5 cursor-pointer"
        >
          <div className="flex items-center justify-between">
            <span className="text-xs text-slate-400 font-medium">وظایف باقیمانده</span>
            <div className="w-8 h-8 rounded-xl bg-cyan-500/10 text-cyan-400 flex items-center justify-center">
              <CheckCircle2 className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-4">
            <div className="text-2xl font-bold text-slate-100">{pendingTasks.length} از {tasks.length}</div>
            <p className="text-xs text-cyan-400 mt-1 flex items-center">
              <span>مشاهده و مدیریت وظایف</span>
              <ArrowUpRight className="w-3 h-3 mr-1" />
            </p>
          </div>
        </div>

        {/* Card 2: Expenses */}
        <div 
          onClick={() => onNavigate('expenses')}
          className="glass-card glass-card-hover rounded-2xl p-5 cursor-pointer"
        >
          <div className="flex items-center justify-between">
            <span className="text-xs text-slate-400 font-medium">مجموع هزینه‌های ماه</span>
            <div className="w-8 h-8 rounded-xl bg-amber-500/10 text-amber-400 flex items-center justify-center">
              <Wallet className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-4">
            <div className="text-xl font-bold text-slate-100">{formatToman(totalExpenses)}</div>
            <p className="text-xs text-amber-400 mt-1 flex items-center">
              <span>اسکن هوشمند پیامک بانکی</span>
              <ArrowUpRight className="w-3 h-3 mr-1" />
            </p>
          </div>
        </div>

        {/* Card 3: Habits */}
        <div 
          onClick={() => onNavigate('tasks')}
          className="glass-card glass-card-hover rounded-2xl p-5 cursor-pointer"
        >
          <div className="flex items-center justify-between">
            <span className="text-xs text-slate-400 font-medium">زنجیره عادت‌ها</span>
            <div className="w-8 h-8 rounded-xl bg-rose-500/10 text-rose-400 flex items-center justify-center">
              <Flame className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-4">
            <div className="text-2xl font-bold text-slate-100">🔥 ۷ روز</div>
            <p className="text-xs text-rose-400 mt-1">عادت‌های روزانه فعال</p>
          </div>
        </div>

        {/* Card 4: Health */}
        <div 
          onClick={() => onNavigate('health')}
          className="glass-card glass-card-hover rounded-2xl p-5 cursor-pointer"
        >
          <div className="flex items-center justify-between">
            <span className="text-xs text-slate-400 font-medium">وضعیت خواب و سلامت</span>
            <div className="w-8 h-8 rounded-xl bg-emerald-500/10 text-emerald-400 flex items-center justify-center">
              <HeartPulse className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-4">
            <div className="text-2xl font-bold text-slate-100">{healthLog.sleepHours} ساعت</div>
            <p className="text-xs text-emerald-400 mt-1">کیفیت خواب: {healthLog.sleepQuality}</p>
          </div>
        </div>
      </div>

      {/* Two Column Section: Tasks & Habits vs Quick Expenses & Jalali Calendar */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Tasks Quick View */}
        <div className="glass-card rounded-2xl p-6 lg:col-span-2 space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-2 space-x-reverse">
              <CheckCircle2 className="w-5 h-5 text-cyan-400" />
              <h3 className="font-bold text-slate-100 text-base">وظایف مهم امروز</h3>
            </div>
            <button 
              onClick={() => onNavigate('tasks')}
              className="text-xs text-cyan-400 hover:underline"
            >
              مشاهده همه
            </button>
          </div>

          <div className="space-y-3">
            {tasks.slice(0, 4).map(task => (
              <div 
                key={task.id} 
                className={`p-3.5 rounded-xl border transition flex items-center justify-between ${
                  task.completed ? 'bg-slate-900/30 border-slate-800/60 opacity-60' : 'bg-slate-900/70 border-slate-800'
                }`}
              >
                <div className="flex items-center space-x-3 space-x-reverse">
                  <input 
                    type="checkbox"
                    checked={task.completed}
                    onChange={() => onToggleTask(task.id)}
                    className="w-5 h-5 rounded border-slate-700 text-cyan-500 focus:ring-cyan-500 bg-slate-900 cursor-pointer"
                  />
                  <div>
                    <h4 className={`text-sm font-medium ${task.completed ? 'line-through text-slate-500' : 'text-slate-200'}`}>
                      {task.title}
                    </h4>
                    <span className="text-[10px] text-slate-400">{task.category} • سررسید: {task.dueDate}</span>
                  </div>
                </div>
                <span className={`text-[10px] px-2.5 py-1 rounded-full font-medium ${
                  task.priority === 'urgent' ? 'bg-rose-500/20 text-rose-400' :
                  task.priority === 'high' ? 'bg-amber-500/20 text-amber-400' : 'bg-slate-800 text-slate-300'
                }`}>
                  {task.priority}
                </span>
              </div>
            ))}
          </div>
        </div>

        {/* Jalali Calendar & Vault status card */}
        <div className="space-y-6">
          <div className="glass-card rounded-2xl p-6 space-y-4">
            <div className="flex items-center space-x-2 space-x-reverse text-cyan-400">
              <Calendar className="w-5 h-5" />
              <h3 className="font-bold text-slate-100 text-base">تقویم جلالی و هیت‌مپ</h3>
            </div>
            <div className="bg-slate-900/80 rounded-xl p-4 border border-slate-800 text-center space-y-2">
              <p className="text-xs text-slate-400">آبان ۱۴۰۵ — هفته سوم</p>
              <div className="grid grid-cols-7 gap-1.5 pt-2">
                {['ش', 'ی', 'د', 'س', 'چ', 'پ', 'ج'].map((d, i) => (
                  <span key={i} className="text-[10px] text-slate-500 font-bold">{d}</span>
                ))}
                {Array.from({ length: 14 }).map((_, i) => (
                  <div 
                    key={i} 
                    className={`h-6 rounded-md text-[10px] flex items-center justify-center font-medium ${
                      i === 4 ? 'bg-cyan-500 text-slate-950 font-bold' :
                      i % 3 === 0 ? 'bg-cyan-500/30 text-cyan-300' : 'bg-slate-800/60 text-slate-400'
                    }`}
                  >
                    {i + 1}
                  </div>
                ))}
              </div>
            </div>
          </div>

          <div className="glass-card rounded-2xl p-5 flex items-center justify-between">
            <div className="flex items-center space-x-3 space-x-reverse">
              <div className="w-10 h-10 rounded-xl bg-emerald-500/10 text-emerald-400 flex items-center justify-center">
                <ShieldCheck className="w-5 h-5" />
              </div>
              <div>
                <h4 className="text-sm font-bold text-slate-200">صندوق امن</h4>
                <p className="text-xs text-slate-400">رمزنگاری شده و فعال</p>
              </div>
            </div>
            <button
              onClick={() => onNavigate('vault')}
              className="px-3 py-1.5 rounded-lg bg-slate-800 text-xs text-cyan-400 hover:bg-slate-700 transition"
            >
              باز کردن
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
