import React from 'react';
import {
  Settings as SettingsIcon,
  Download,
  Shield,
  Info,
  Database,
  BellRing,
  Smartphone,
  Sliders,
  Moon,
  CheckCircle2,
  AlertTriangle,
} from 'lucide-react';
import type { NotificationSettings } from '../types';
import { formatToman } from '../utils/persianUtils';

interface SettingsScreenProps {
  onExportData: () => void;
  onResetData: () => void;
  notificationSettings: NotificationSettings;
  onUpdateNotificationSettings: (settings: NotificationSettings) => void;
  onOpenNotificationCenter: () => void;
}

export const SettingsScreen: React.FC<SettingsScreenProps> = ({
  onExportData,
  onResetData,
  notificationSettings,
  onUpdateNotificationSettings,
  onOpenNotificationCenter,
}) => {
  const updateField = <K extends keyof NotificationSettings>(
    key: K,
    value: NotificationSettings[K]
  ) => {
    onUpdateNotificationSettings({
      ...notificationSettings,
      [key]: value,
    });
  };

  const toggleAllowedSource = (
    sourceKey: keyof NotificationSettings['allowedBankSources']
  ) => {
    onUpdateNotificationSettings({
      ...notificationSettings,
      allowedBankSources: {
        ...notificationSettings.allowedBankSources,
        [sourceKey]: !notificationSettings.allowedBankSources[sourceKey],
      },
    });
  };

  return (
    <div className="max-w-3xl mx-auto space-y-6 animate-fadeIn">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-100 flex items-center space-x-2 space-x-reverse">
            <SettingsIcon className="w-7 h-7 text-cyan-400" />
            <span>تنظیمات، نوتیفیکیشن و پشتیبان‌گیری</span>
          </h2>
          <p className="text-xs text-slate-400 mt-1">
            مدیریت سرویس خواننده نوتیفیکیشن بانکی، هشدارهای مالی، امنیت و داده‌های Personal Life OS
          </p>
        </div>

        <button
          type="button"
          onClick={onOpenNotificationCenter}
          className="px-4 py-2.5 rounded-2xl bg-cyan-500/20 border border-cyan-500/40 text-cyan-300 text-xs font-extrabold hover:bg-cyan-500/30 transition flex items-center space-x-2 space-x-reverse self-start sm:self-auto"
        >
          <BellRing className="w-4 h-4 text-cyan-400" />
          <span>باز کردن مرکز نوتیفیکیشن و لاگ فیش‌ها</span>
        </button>
      </div>

      <div className="space-y-5">
        {/* Card 1: Smart Bank Notification & Receipt Listener Settings */}
        <div className="glass-card rounded-2xl p-6 space-y-5 border border-cyan-500/25">
          <div className="flex items-center justify-between">
            <h3 className="font-bold text-slate-100 text-base flex items-center space-x-2 space-x-reverse">
              <BellRing className="w-5 h-5 text-cyan-400" />
              <span>سرویس خواننده نوتیفیکیشن بانکی و فیش‌ها</span>
            </h3>
            <span
              className={`text-[11px] px-3 py-1 rounded-full font-bold flex items-center gap-1.5 ${
                notificationSettings.bankNotificationListener
                  ? 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/30'
                  : 'bg-rose-500/15 text-rose-400 border border-rose-500/30'
              }`}
            >
              <span
                className={`w-2 h-2 rounded-full ${
                  notificationSettings.bankNotificationListener
                    ? 'bg-emerald-400 animate-pulse'
                    : 'bg-rose-400'
                }`}
              />
              <span>
                {notificationSettings.bankNotificationListener
                  ? 'دسترسی نوتیفیکیشن‌خوان فعال'
                  : 'غیرفعال'}
              </span>
            </span>
          </div>

          <p className="text-xs text-slate-400 leading-relaxed">
            برای خواندن دقیق فیش‌های بانکی بدون نیاز به دسترسی مستقیم پیامک یا برای نئوبانک‌هایی که نوتیفیکیشن ارسال می‌کنند (مانند بلوبانک و ویپاد)، سرویس شنونده نوتیفیکیشن فیش‌ها را در لحظه دریافت و با قواعد امن پروفایل بانک مطابقت می‌دهد.
          </p>

          {/* Main Toggles */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <label className="p-3.5 rounded-xl bg-slate-900/80 border border-slate-800 flex items-start space-x-3 space-x-reverse cursor-pointer hover:border-cyan-500/30 transition">
              <input
                type="checkbox"
                checked={notificationSettings.bankNotificationListener}
                onChange={(e) =>
                  updateField('bankNotificationListener', e.target.checked)
                }
                className="mt-0.5 w-4 h-4 rounded border-slate-700 text-cyan-500 focus:ring-cyan-500 bg-slate-950"
              />
              <div>
                <div className="text-xs font-bold text-slate-200">
                  پایش زنده نوتیفیکیشن‌های بانکی و پیامک
                </div>
                <div className="text-[11px] text-slate-400 mt-0.5">
                  دریافت آنی فیش‌های واریز و برداشت از نوار اعلان دستگاه
                </div>
              </div>
            </label>

            <label className="p-3.5 rounded-xl bg-slate-900/80 border border-slate-800 flex items-start space-x-3 space-x-reverse cursor-pointer hover:border-cyan-500/30 transition">
              <input
                type="checkbox"
                checked={notificationSettings.autoRegisterVerifiedNotifications}
                onChange={(e) =>
                  updateField(
                    'autoRegisterVerifiedNotifications',
                    e.target.checked
                  )
                }
                className="mt-0.5 w-4 h-4 rounded border-slate-700 text-emerald-500 focus:ring-emerald-500 bg-slate-950"
              />
              <div>
                <div className="text-xs font-bold text-slate-200">
                  ثبت خودکار فیش‌های دارای الگوی تأییدشده
                </div>
                <div className="text-[11px] text-slate-400 mt-0.5">
                  در صورت غیرفعال بودن، قبل از ثبت هر فیش از شما در نوتیفیکیشن تأیید می‌گیرد
                </div>
              </div>
            </label>

            <label className="p-3.5 rounded-xl bg-slate-900/80 border border-slate-800 flex items-start space-x-3 space-x-reverse cursor-pointer hover:border-cyan-500/30 transition">
              <input
                type="checkbox"
                checked={notificationSettings.filterOtpAndPromotional}
                onChange={(e) =>
                  updateField('filterOtpAndPromotional', e.target.checked)
                }
                className="mt-0.5 w-4 h-4 rounded border-slate-700 text-cyan-500 focus:ring-cyan-500 bg-slate-950"
              />
              <div>
                <div className="text-xs font-bold text-slate-200">
                  فیلتر هوشمند رمز پویا (OTP) و تبلیغات
                </div>
                <div className="text-[11px] text-slate-400 mt-0.5">
                  مسدودسازی خودکار پیامک‌های رمز دوم و انتقال آن‌ها به تب لاگ فیلترشده
                </div>
              </div>
            </label>

            <label className="p-3.5 rounded-xl bg-slate-900/80 border border-slate-800 flex items-start space-x-3 space-x-reverse cursor-pointer hover:border-cyan-500/30 transition">
              <input
                type="checkbox"
                checked={notificationSettings.showFloatingHeadsUpBanner}
                onChange={(e) =>
                  updateField('showFloatingHeadsUpBanner', e.target.checked)
                }
                className="mt-0.5 w-4 h-4 rounded border-slate-700 text-cyan-500 focus:ring-cyan-500 bg-slate-950"
              />
              <div>
                <div className="text-xs font-bold text-slate-200">
                  نمایش بنر شناور نوتیفیکیشن بالای صفحه
                </div>
                <div className="text-[11px] text-slate-400 mt-0.5">
                  نمایش کارت سریع ثبت فیش به محض دریافت نوتیفیکیشن جدید
                </div>
              </div>
            </label>
          </div>

          {/* Allowed Source Apps (Package Filter) */}
          <div className="pt-2 space-y-2.5">
            <div className="flex items-center space-x-2 space-x-reverse text-xs font-bold text-slate-200">
              <Smartphone className="w-4 h-4 text-cyan-400" />
              <span>اپلیکیشن‌ها و منابع مجاز خواندن نوتیفیکیشن (فیلتر پکیج‌ها):</span>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5">
              {[
                {
                  key: 'smsInbox' as const,
                  label: 'پیامک سیم‌کارت (SMS)',
                  pkg: 'com.google.android.apps.messaging',
                },
                {
                  key: 'bluBankApp' as const,
                  label: 'اپلیکیشن بلوبانک (Blu)',
                  pkg: 'ir.samanpr.blu',
                },
                {
                  key: 'wepodApp' as const,
                  label: 'ویپاد پاسارگاد (Wepod)',
                  pkg: 'com.my.wepod',
                },
                {
                  key: 'melliBamApp' as const,
                  label: 'همراه بام ملی (Bam)',
                  pkg: 'ir.bmi.bam.native',
                },
                {
                  key: 'mellatPlusApp' as const,
                  label: 'همراه بانک ملت',
                  pkg: 'ir.bankmellat.mobile',
                },
                {
                  key: 'otherBankApps' as const,
                  label: 'سایر همراه بانک‌ها و آپ',
                  pkg: 'Bankino / Tobank / 733',
                },
              ].map((src) => {
                const active = notificationSettings.allowedBankSources[src.key];
                return (
                  <button
                    key={src.key}
                    type="button"
                    onClick={() => toggleAllowedSource(src.key)}
                    className={`p-3 rounded-xl border text-right transition flex items-center justify-between ${
                      active
                        ? 'bg-cyan-500/10 border-cyan-500/40 text-slate-100'
                        : 'bg-slate-950/60 border-slate-800 text-slate-500'
                    }`}
                  >
                    <div>
                      <div className="text-xs font-bold">{src.label}</div>
                      <div className="text-[10px] font-mono opacity-75">
                        {src.pkg}
                      </div>
                    </div>
                    <CheckCircle2
                      className={`w-4 h-4 shrink-0 ${
                        active ? 'text-cyan-400' : 'text-slate-700'
                      }`}
                    />
                  </button>
                );
              })}
            </div>
          </div>
        </div>

        {/* Card 2: Financial Threshold Alerts & Daily Smart Reminders */}
        <div className="glass-card rounded-2xl p-6 space-y-4">
          <h3 className="font-bold text-slate-100 text-base flex items-center space-x-2 space-x-reverse">
            <Sliders className="w-5 h-5 text-amber-400" />
            <span>آستانه هشدارهای هوشمند مالی و یادآورهای روزانه</span>
          </h3>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="p-4 rounded-xl bg-slate-900/80 border border-slate-800 space-y-2">
              <label className="text-xs font-bold text-amber-300 flex items-center justify-between">
                <span>سقف هشدار تراکنش / برداشت سنگین (تومان)</span>
                <AlertTriangle className="w-4 h-4 text-amber-400" />
              </label>
              <input
                type="number"
                step={100000}
                value={notificationSettings.highExpenseThresholdTomans}
                onChange={(e) =>
                  updateField(
                    'highExpenseThresholdTomans',
                    Math.max(0, parseInt(e.target.value || '0', 10))
                  )
                }
                className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3 py-2 text-sm text-slate-100 font-bold"
              />
              <p className="text-[11px] text-slate-400">
                فعلی: {formatToman(notificationSettings.highExpenseThresholdTomans)} — هر برداشتی بالاتر از این مبلغ، نوتیفیکیشن هشدار فوری ایجاد می‌کند.
              </p>
            </div>

            <div className="p-4 rounded-xl bg-slate-900/80 border border-slate-800 space-y-2">
              <label className="text-xs font-bold text-rose-300 flex items-center justify-between">
                <span>هشدار افت موجودی حساب (تومان)</span>
                <AlertTriangle className="w-4 h-4 text-rose-400" />
              </label>
              <input
                type="number"
                step={100000}
                value={notificationSettings.lowBalanceThresholdTomans}
                onChange={(e) =>
                  updateField(
                    'lowBalanceThresholdTomans',
                    Math.max(0, parseInt(e.target.value || '0', 10))
                  )
                }
                className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3 py-2 text-sm text-slate-100 font-bold"
              />
              <p className="text-[11px] text-slate-400">
                فعلی: {formatToman(notificationSettings.lowBalanceThresholdTomans)} — اگر مانده حساب استخراج‌شده از پیامک کمتر از این مقدار شود هشدار می‌دهد.
              </p>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-2">
            <label className="p-3 rounded-xl bg-slate-900/70 border border-slate-800 flex items-center space-x-2.5 space-x-reverse cursor-pointer">
              <input
                type="checkbox"
                checked={notificationSettings.taskDueReminders}
                onChange={(e) =>
                  updateField('taskDueReminders', e.target.checked)
                }
                className="w-4 h-4 rounded border-slate-700 text-cyan-500 bg-slate-950"
              />
              <span className="text-xs text-slate-200 font-medium">
                اعلان وظایف فوری (Urgent)
              </span>
            </label>

            <label className="p-3 rounded-xl bg-slate-900/70 border border-slate-800 flex items-center space-x-2.5 space-x-reverse cursor-pointer">
              <input
                type="checkbox"
                checked={notificationSettings.habitStreakReminders}
                onChange={(e) =>
                  updateField('habitStreakReminders', e.target.checked)
                }
                className="w-4 h-4 rounded border-slate-700 text-cyan-500 bg-slate-950"
              />
              <span className="text-xs text-slate-200 font-medium">
                محافظ زنجیره عادت‌ها
              </span>
            </label>

            <label className="p-3 rounded-xl bg-slate-900/70 border border-slate-800 flex items-center space-x-2.5 space-x-reverse cursor-pointer">
              <input
                type="checkbox"
                checked={notificationSettings.healthHydrationReminders}
                onChange={(e) =>
                  updateField('healthHydrationReminders', e.target.checked)
                }
                className="w-4 h-4 rounded border-slate-700 text-cyan-500 bg-slate-950"
              />
              <span className="text-xs text-slate-200 font-medium">
                یادآور نوشیدن آب و خواب
              </span>
            </label>
          </div>

          {/* Quiet Hours */}
          <div className="p-3.5 rounded-xl bg-slate-900/80 border border-slate-800 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <label className="flex items-center space-x-2.5 space-x-reverse cursor-pointer">
              <input
                type="checkbox"
                checked={notificationSettings.quietHoursEnabled}
                onChange={(e) =>
                  updateField('quietHoursEnabled', e.target.checked)
                }
                className="w-4 h-4 rounded border-slate-700 text-indigo-500 bg-slate-950"
              />
              <Moon className="w-4 h-4 text-indigo-400" />
              <div>
                <div className="text-xs font-bold text-slate-200">
                  ساعات سکوت شبانه (مزاحم نشوید)
                </div>
                <div className="text-[11px] text-slate-400">
                  در این بازه زمانی بنر شناور نمایش داده نمی‌شود و فیش‌ها بی‌صدا در لاگ ذخیره می‌گردند
                </div>
              </div>
            </label>

            {notificationSettings.quietHoursEnabled && (
              <div className="flex items-center gap-2 text-xs">
                <input
                  type="time"
                  value={notificationSettings.quietHoursStart}
                  onChange={(e) =>
                    updateField('quietHoursStart', e.target.value)
                  }
                  className="bg-slate-950 border border-slate-700 rounded-lg px-2 py-1 text-slate-200"
                />
                <span className="text-slate-400">تا</span>
                <input
                  type="time"
                  value={notificationSettings.quietHoursEnd}
                  onChange={(e) => updateField('quietHoursEnd', e.target.value)}
                  className="bg-slate-950 border border-slate-700 rounded-lg px-2 py-1 text-slate-200"
                />
              </div>
            )}
          </div>
        </div>

        {/* Data Management Card */}
        <div className="glass-card rounded-2xl p-6 space-y-4">
          <h3 className="font-bold text-slate-100 text-base flex items-center space-x-2 space-x-reverse">
            <Database className="w-5 h-5 text-cyan-400" />
            <span>مدیریت داده‌ها و فضای ذخیره‌سازی محلی</span>
          </h3>
          <p className="text-xs text-slate-400">
            می‌توانید تمام اطلاعات وظایف، هزینه‌ها، پروفایل بانک‌ها، نوتیفیکیشن‌ها و یادداشت‌های خود را پشتیبان‌گیری کنید یا بازنشانی نمایید.
          </p>

          <div className="flex flex-wrap gap-4 pt-2">
            <button
              onClick={onExportData}
              className="flex items-center space-x-2 space-x-reverse px-5 py-3 rounded-xl bg-cyan-500/20 text-cyan-400 border border-cyan-500/30 text-sm font-semibold hover:bg-cyan-500/30 transition"
            >
              <Download className="w-4 h-4" />
              <span>خروجی گرفتن از اطلاعات (JSON)</span>
            </button>
            <button
              onClick={() => {
                if (
                  confirm('آیا از بازنشانی اطلاعات به حالت اولیه اطمینان دارید؟')
                ) {
                  onResetData();
                }
              }}
              className="px-5 py-3 rounded-xl bg-rose-500/10 text-rose-400 border border-rose-500/20 text-sm font-semibold hover:bg-rose-500/20 transition"
            >
              بازنشانی کارخانه (Reset Data)
            </button>
          </div>
        </div>

        {/* Security & Sync Card */}
        <div className="glass-card rounded-2xl p-6 space-y-4">
          <h3 className="font-bold text-slate-100 text-base flex items-center space-x-2 space-x-reverse">
            <Shield className="w-5 h-5 text-emerald-400" />
            <span>امنیت و رمزنگاری</span>
          </h3>
          <p className="text-xs text-slate-400">
            تمامی اطلاعات مالی، لاگ نوتیفیکیشن‌های بانکی، یادداشت‌های صندوق امن و داده‌های سلامت شما در حافظه محلی محافظت می‌شوند.
          </p>
          <div className="p-3.5 bg-slate-900/80 rounded-xl border border-slate-800 text-xs text-slate-300 flex items-center justify-between">
            <span>وضعیت موتور امنیتی فیش‌خوان و نوتیفیکیشن</span>
            <span className="text-emerald-400 font-bold">● فعال و امن</span>
          </div>
        </div>

        {/* About App */}
        <div className="glass-card rounded-2xl p-6 space-y-4">
          <h3 className="font-bold text-slate-100 text-base flex items-center space-x-2 space-x-reverse">
            <Info className="w-5 h-5 text-blue-400" />
            <span>درباره Personal Life OS</span>
          </h3>
          <p className="text-xs text-slate-400 leading-relaxed">
            این اپلیکیشن با پشتیبانی کامل از زبان فارسی، تقویم جلالی، اسکنر مرحله‌ای پیامک و نوتیفیکیشن‌های بانکی، دستیار هوشمند جمینای و صندوق امن طراحی شده است.
          </p>
          <div className="text-[11px] text-slate-500">
            نسخه: ۱.۶.۰R • پلتفرم: React + Vite + Tailwind CSS
          </div>
        </div>
      </div>
    </div>
  );
};
