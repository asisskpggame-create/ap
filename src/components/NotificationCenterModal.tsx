import React, { useState } from 'react';
import {
  Bell,
  BellRing,
  Check,
  CheckCheck,
  Trash2,
  X,
  Sparkles,
  Smartphone,
  MessageSquare,
  CheckCircle2,
  AlertTriangle,
  SlidersHorizontal,
  Send,
  Zap,
  Filter,
  Eye,
  PlusCircle,
  Ban,
  Volume2,
} from 'lucide-react';
import type {
  AppNotification,
  BankProfile,
  CurrencyUnit,
  ExpenseItem,
  NotificationSettings,
  NotificationSourceType,
  SmsTransactionKind,
} from '../types';
import {
  buildValidatedExpenseEntry,
  evaluateSampleForProfile,
  formatToman,
  getPersianDateString,
  revalidateBankProfile,
  translateTransactionKind,
} from '../utils/persianUtils';
import { BankEmblemSvg } from './BankVisualSelectorSheet';

interface NotificationCenterModalProps {
  isOpen: boolean;
  onClose: () => void;
  notifications: AppNotification[];
  settings: NotificationSettings;
  bankProfiles: BankProfile[];
  expenses: ExpenseItem[];
  onMarkAsRead: (id: string) => void;
  onMarkAllAsRead: () => void;
  onClearNotifications: (onlyRead?: boolean) => void;
  onDeleteNotification: (id: string) => void;
  onUpdateNotification: (updated: AppNotification) => void;
  onUpdateSettings: (settings: NotificationSettings) => void;
  onRegisterExpenseFromNotification: (
    notificationId: string,
    entry: Omit<ExpenseItem, 'id'>
  ) => void;
  onUpdateBankProfiles: (profiles: BankProfile[]) => void;
  onSimulateIncomingNotification: (payload: {
    rawText: string;
    senderId: string;
    bankId?: string;
    sourceType: NotificationSourceType;
    sourceAppLabel: string;
  }) => void;
  onNavigateTab: (tab: any) => void;
}

const QUICK_CATEGORIES = [
  'خرید روزانه',
  'غذا و کافه',
  'حمل و نقل',
  'انتقال وجه',
  'قبوض و خدمات',
  'درآمد / واریز',
];

const SIMULATED_NOTIFICATION_PRESETS: {
  label: string;
  bankId: string;
  senderId: string;
  sourceType: NotificationSourceType;
  sourceAppLabel: string;
  rawText: string;
  badgeColor: string;
}[] = [
  {
    label: 'نوتیفیکیشن بلوبانک (خرید کارتی)',
    bankId: 'blu',
    senderId: 'BLU',
    sourceType: 'BANK_APP_PUSH',
    sourceAppLabel: 'بلوبانک (ir.samanpr.blu)',
    rawText:
      'بلو\nخرید کارتی\nمبلغ: ۲,۴۵۰,۰۰۰ ریال\nاز حساب شما پرید\nمانده: ۱۸,۷۵۰,۰۰۰ ریال\n۱۴۰۵/۰۸/۰۵ ۲۰:۱۵',
    badgeColor: 'border-sky-500/40 text-sky-300 bg-sky-500/10',
  },
  {
    label: 'نوتیفیکیشن بلوبانک (انتقال پل)',
    bankId: 'blu_pol',
    senderId: 'BLU',
    sourceType: 'BANK_APP_PUSH',
    sourceAppLabel: 'بلوبانک حالت پل (ir.samanpr.blu)',
    rawText:
      'بلو\nانتقال پل\nمبلغ: ۶,۸۰۰,۰۰۰ ریال\nمانده: ۱۱,۹۵۰,۰۰۰ ریال\nکد پیگیری: ۷۴۸۲۹۱\n۱۴۰۵/۰۸/۰۵ ۲۰:۱۸',
    badgeColor: 'border-emerald-500/40 text-emerald-300 bg-emerald-500/10',
  },
  {
    label: 'پیامک بانک ملی (برداشت سداد)',
    bankId: 'melli',
    senderId: '98300017',
    sourceType: 'SMS_INBOX',
    sourceAppLabel: 'پیامک سیم‌کارت (98300017)',
    rawText:
      'بانک ملی ایران\nبرداشت: ۴,۲۰۰,۰۰۰ ریال\nحساب: ۰۱۰۹۸۷۶۵۴۳۰۰۱\nمانده: ۹,۵۰۰,۰۰۰ ریال\n۰۸/۰۵-۲۰:۲۲',
    badgeColor: 'border-amber-500/40 text-amber-300 bg-amber-500/10',
  },
  {
    label: 'پیامک بانک ملت (خرید سنگین)',
    bankId: 'mellat',
    senderId: '98200033',
    sourceType: 'SMS_INBOX',
    sourceAppLabel: 'پیامک سیم‌کارت (98200033)',
    rawText:
      'بانک ملت\nبرداشت از: ۶۱۰۴۳۳******۱۲۳۴\nمبلغ: ۳۵,۰۰۰,۰۰۰ ریال\nمانده: ۴,۲۰۰,۰۰۰ ریال\nتاریخ: ۱۴۰۵/۰۸/۰۵\nساعت: ۲۰:۲۵\nپیگیری: ۵۵۱۲۹۰',
    badgeColor: 'border-rose-500/40 text-rose-300 bg-rose-500/10',
  },
  {
    label: 'نوتیفیکیشن ویپاد (نیازمند تأیید)',
    bankId: 'pasargad',
    senderId: 'Wepod',
    sourceType: 'BANK_APP_PUSH',
    sourceAppLabel: 'ویپاد پاسارگاد (com.my.wepod)',
    rawText:
      'بانک پاسارگاد - ویپاد\nبرداشت مبلغ ۱,۸۰۰,۰۰۰ ریال\nکارت: ۵۰۲۲۲۹******۴۴۱۱\nمانده: ۷,۱۰۰,۰۰۰ ریال\n۱۴۰۵/۰۸/۰۵ ۲۰:۳۰',
    badgeColor: 'border-purple-500/40 text-purple-300 bg-purple-500/10',
  },
  {
    label: 'تست فیلتر امنیتی: رمز پویا (OTP)',
    bankId: 'mellat',
    senderId: '98200033',
    sourceType: 'SMS_INBOX',
    sourceAppLabel: 'پیامک سیم‌کارت (98200033)',
    rawText:
      'بانک ملت\nرمز پویا: ۶۳۹۱۸۴\nمبلغ: ۵,۰۰۰,۰۰۰ ریال\nکارت: ۶۱۰۴******۱۲۳۴',
    badgeColor: 'border-slate-600 text-slate-300 bg-slate-800/80',
  },
];

export const NotificationCenterModal: React.FC<NotificationCenterModalProps> = ({
  isOpen,
  onClose,
  notifications,
  settings,
  bankProfiles,
  expenses,
  onMarkAsRead,
  onMarkAllAsRead,
  onClearNotifications,
  onDeleteNotification,
  onUpdateNotification,
  onUpdateSettings,
  onRegisterExpenseFromNotification,
  onUpdateBankProfiles,
  onSimulateIncomingNotification,
  onNavigateTab,
}) => {
  const [activeFilter, setActiveFilter] = useState<
    'all' | 'pending_action' | 'bank_receipt' | 'smart_alerts' | 'filtered_logs'
  >('all');
  const [showSimulator, setShowSimulator] = useState(false);

  // Custom simulator state
  const [customSimBankId, setCustomSimBankId] = useState('melli');
  const [customSimSender, setCustomSimSender] = useState('98300017');
  const [customSimSource, setCustomSimSource] =
    useState<NotificationSourceType>('BANK_APP_PUSH');
  const [customSimText, setCustomSimText] = useState('');

  // Inline review overrides per notification ID
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editBankId, setEditBankId] = useState<string>('');
  const [editKind, setEditKind] = useState<SmsTransactionKind>('PURCHASE');
  const [editCurrency, setEditCurrency] = useState<CurrencyUnit>('RIAL');
  const [editAmountRaw, setEditAmountRaw] = useState<string>('');
  const [selectedCategories, setSelectedCategories] = useState<
    Record<string, string>
  >({});
  const [actionFeedback, setActionFeedback] = useState<{
    id: string;
    type: 'success' | 'error';
    text: string;
  } | null>(null);

  if (!isOpen) return null;

  const unreadCount = notifications.filter((n) => !n.read).length;
  const pendingActionCount = notifications.filter(
    (n) => n.actionState === 'PENDING_USER_ACTION'
  ).length;
  const filteredLogsCount = notifications.filter(
    (n) => n.actionState === 'FILTERED_NON_FINANCIAL'
  ).length;

  const filteredList = notifications.filter((item) => {
    if (activeFilter === 'pending_action') {
      return item.actionState === 'PENDING_USER_ACTION';
    }
    if (activeFilter === 'bank_receipt') {
      return (
        item.category === 'bank_receipt' &&
        item.actionState !== 'FILTERED_NON_FINANCIAL'
      );
    }
    if (activeFilter === 'smart_alerts') {
      return (
        item.category === 'financial_alert' ||
        item.category === 'tasks_habits' ||
        item.category === 'health_system'
      );
    }
    if (activeFilter === 'filtered_logs') {
      return item.actionState === 'FILTERED_NON_FINANCIAL';
    }
    return true;
  });

  const handleOpenInlineEdit = (notif: AppNotification) => {
    setEditingId(notif.id);
    setEditBankId(notif.analysisResult?.bankId || notif.bankId || 'melli');
    setEditKind(
      notif.analysisResult?.transactionKind &&
        notif.analysisResult.transactionKind !== 'UNKNOWN'
        ? notif.analysisResult.transactionKind
        : 'PURCHASE'
    );
    setEditCurrency(
      notif.analysisResult?.currency &&
        notif.analysisResult.currency !== 'UNKNOWN'
        ? notif.analysisResult.currency
        : 'RIAL'
    );
    setEditAmountRaw(
      notif.analysisResult?.amountOriginal !== undefined
        ? String(notif.analysisResult.amountOriginal)
        : ''
    );
    setActionFeedback(null);
  };

  const handleConfirmRegisterNotification = (
    notif: AppNotification,
    verifyTemplatePermanently = false
  ) => {
    if (!notif.analysisResult) return;

    const chosenCategory =
      selectedCategories[notif.id] || notif.suggestedCategory || 'تراکنش بانکی';

    const isInlineEdited = editingId === notif.id;
    const rawAmountNum = isInlineEdited
      ? parseInt(editAmountRaw.replace(/,/g, ''), 10)
      : notif.analysisResult.amountOriginal;

    const overrides = isInlineEdited
      ? {
          bankId: editBankId || notif.analysisResult.bankId,
          transactionKind: editKind,
          currency: editCurrency,
          amountOriginal: rawAmountNum,
        }
      : {
          bankId: notif.analysisResult.bankId || notif.bankId,
          transactionKind: notif.analysisResult.transactionKind,
          currency: notif.analysisResult.currency,
          amountOriginal: notif.analysisResult.amountOriginal,
        };

    const validated = buildValidatedExpenseEntry(
      notif.analysisResult,
      overrides,
      bankProfiles,
      expenses
    );

    if (!validated.ok || !validated.entry) {
      handleOpenInlineEdit(notif);
      setActionFeedback({
        id: notif.id,
        type: 'error',
        text:
          validated.error ||
          'برای ثبت تراکنش، لطفاً نوع تراکنش، واحد پول و مبلغ را در فرم زیر مشخص کنید.',
      });
      return;
    }

    const finalEntry: Omit<ExpenseItem, 'id'> = {
      ...validated.entry,
      category: chosenCategory,
    };

    onRegisterExpenseFromNotification(notif.id, finalEntry);

    if (verifyTemplatePermanently && notif.rawText) {
      const targetBankId = overrides.bankId || notif.bankId;
      const targetProfile = bankProfiles.find((p) => p.id === targetBankId);
      if (targetProfile) {
        const evaluatedSample = evaluateSampleForProfile(
          targetProfile,
          notif.rawText,
          notif.senderId,
          true
        );
        if (
          evaluatedSample.parseStatus === 'VALID_TRANSACTION' &&
          evaluatedSample.matchedTemplateId
        ) {
          const updatedProfile = revalidateBankProfile({
            ...targetProfile,
            samples: [evaluatedSample, ...targetProfile.samples],
            updatedAt: getPersianDateString(),
          });
          onUpdateBankProfiles(
            bankProfiles.map((p) =>
              p.id === updatedProfile.id ? updatedProfile : p
            )
          );
        }
      }
    }

    setEditingId(null);
    setActionFeedback({
      id: notif.id,
      type: 'success',
      text: verifyTemplatePermanently
        ? 'تراکنش ثبت شد و الگوی این بانک نیز برای ثبت خودکار فیش‌های بعدی تأیید گردید.'
        : 'فیش بانکی با موفقیت در لیست تراکنش‌ها ثبت شد.',
    });
  };

  const handleRejectNotificationReceipt = (notif: AppNotification) => {
    onUpdateNotification({
      ...notif,
      read: true,
      actionState: 'REJECTED_BY_USER',
    });
    if (editingId === notif.id) {
      setEditingId(null);
    }
  };

  const handleRequestBrowserPermission = async () => {
    if (typeof window === 'undefined' || !('Notification' in window)) {
      alert('مرورگر فعلی از Web Notification API پشتیبانی نمی‌کند.');
      return;
    }
    const perm = await window.Notification.requestPermission();
    if (perm === 'granted') {
      onUpdateSettings({ ...settings, browserPushEnabled: true });
      new window.Notification('Personal Life OS — نوتیفیکیشن فعال شد', {
        body: 'از این پس فیش‌های بانکی و هشدارهای هوشمند روی سیستم شما نمایش داده می‌شوند.',
      });
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-md p-2 sm:p-4 animate-fadeIn">
      <div className="bg-slate-900 border border-slate-800 rounded-3xl w-full max-w-4xl max-h-[92vh] flex flex-col shadow-2xl shadow-cyan-950/40 overflow-hidden">
        {/* Top Modal Header */}
        <div className="px-5 py-4 border-b border-slate-800 bg-slate-950/70 flex items-center justify-between gap-3">
          <div className="flex items-center space-x-3 space-x-reverse">
            <div className="w-11 h-11 rounded-2xl bg-gradient-to-tr from-cyan-500/20 to-emerald-500/20 border border-cyan-500/30 flex items-center justify-center text-cyan-400 relative">
              <BellRing className="w-5 h-5" />
              {unreadCount > 0 && (
                <span className="absolute -top-1 -right-1 w-5 h-5 rounded-full bg-rose-500 text-white text-[10px] font-extrabold flex items-center justify-center shadow">
                  {unreadCount}
                </span>
              )}
            </div>
            <div>
              <div className="flex items-center gap-2 flex-wrap">
                <h2 className="text-base sm:text-lg font-extrabold text-slate-100">
                  مرکز نوتیفیکیشن و خواننده هوشمند فیش‌ها
                </h2>
                <span
                  className={`text-[10px] px-2.5 py-0.5 rounded-full font-bold flex items-center gap-1 ${
                    settings.bankNotificationListener
                      ? 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/30'
                      : 'bg-rose-500/15 text-rose-400 border border-rose-500/30'
                  }`}
                >
                  <span
                    className={`w-1.5 h-1.5 rounded-full ${
                      settings.bankNotificationListener
                        ? 'bg-emerald-400 animate-ping'
                        : 'bg-rose-400'
                    }`}
                  />
                  <span>
                    {settings.bankNotificationListener
                      ? 'شنونده نوتیفیکیشن بانکی فعال'
                      : 'شنونده نوتیفیکیشن متوقف'}
                  </span>
                </span>
              </div>
              <p className="text-[11px] text-slate-400 mt-0.5">
                دریافت زنده فیش‌ها از نوتیفیکیشن بانک‌ها (بلوبانک، ویپاد، بام و...) و پیامک سیم‌کارت همراه با لاگ امنیتی
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => setShowSimulator((prev) => !prev)}
              className={`px-3 py-2 rounded-xl text-xs font-bold flex items-center space-x-1.5 space-x-reverse transition border ${
                showSimulator
                  ? 'bg-cyan-500 text-slate-950 border-cyan-400 shadow-lg shadow-cyan-500/20'
                  : 'bg-slate-800/90 text-cyan-300 border-cyan-500/30 hover:bg-slate-800'
              }`}
            >
              <Zap className="w-3.5 h-3.5" />
              <span>تست دریافت نوتیفیکیشن فیش</span>
            </button>

            <button
              type="button"
              onClick={onClose}
              className="p-2 rounded-xl bg-slate-800/80 text-slate-400 hover:text-white transition"
              aria-label="بستن مرکز نوتیفیکیشن"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Listener Quick Control Bar */}
        <div className="px-5 py-3 bg-slate-900/90 border-b border-slate-800/80 flex flex-wrap items-center justify-between gap-3 text-xs">
          <div className="flex flex-wrap items-center gap-4">
            <label className="flex items-center space-x-2 space-x-reverse cursor-pointer">
              <input
                type="checkbox"
                checked={settings.bankNotificationListener}
                onChange={(e) =>
                  onUpdateSettings({
                    ...settings,
                    bankNotificationListener: e.target.checked,
                  })
                }
                className="w-4 h-4 rounded border-slate-700 text-emerald-500 focus:ring-emerald-500 bg-slate-950"
              />
              <span className="text-slate-200 font-semibold">
                خواندن خودکار نوتیفیکیشن بانک‌ها و پیامک‌ها
              </span>
            </label>

            <label className="flex items-center space-x-2 space-x-reverse cursor-pointer">
              <input
                type="checkbox"
                checked={settings.autoRegisterVerifiedNotifications}
                onChange={(e) =>
                  onUpdateSettings({
                    ...settings,
                    autoRegisterVerifiedNotifications: e.target.checked,
                  })
                }
                className="w-4 h-4 rounded border-slate-700 text-cyan-500 focus:ring-cyan-500 bg-slate-950"
              />
              <span className="text-slate-300">
                ثبت خودکار فیش‌های دارای پروفایل و قالب تأییدشده
              </span>
            </label>
          </div>

          <div className="flex items-center gap-2">
            {unreadCount > 0 && (
              <button
                type="button"
                onClick={onMarkAllAsRead}
                className="px-2.5 py-1.5 rounded-lg bg-slate-800 text-cyan-300 hover:bg-slate-700 transition flex items-center space-x-1 space-x-reverse text-[11px]"
              >
                <CheckCheck className="w-3.5 h-3.5" />
                <span>خواندن همه ({unreadCount})</span>
              </button>
            )}
            {notifications.length > 0 && (
              <button
                type="button"
                onClick={() => onClearNotifications(true)}
                className="px-2.5 py-1.5 rounded-lg bg-slate-800 text-slate-300 hover:text-rose-300 hover:bg-rose-500/10 transition flex items-center space-x-1 space-x-reverse text-[11px]"
              >
                <Trash2 className="w-3.5 h-3.5" />
                <span>پاکسازی خوانده‌شده‌ها</span>
              </button>
            )}
          </div>
        </div>

        {/* Collapsible Live Notification Simulator Drawer */}
        {showSimulator && (
          <div className="p-4 bg-gradient-to-b from-cyan-950/40 to-slate-900 border-b border-cyan-500/30 space-y-3 animate-fadeIn">
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-2 space-x-reverse text-cyan-300 text-xs font-bold">
                <Smartphone className="w-4 h-4 text-cyan-400" />
                <span>
                  شبیه‌ساز دریافت نوتیفیکیشن بانکی (مانند دریافت واقعی در گوشی از بلوبانک، ملی، ملت و ویپاد)
                </span>
              </div>
              <button
                type="button"
                onClick={handleRequestBrowserPermission}
                className="text-[11px] px-2.5 py-1 rounded-lg bg-cyan-500/20 text-cyan-300 border border-cyan-500/30 hover:bg-cyan-500/30 transition flex items-center space-x-1 space-x-reverse"
              >
                <Volume2 className="w-3 h-3" />
                <span>فعال‌سازی اعلان ویندوز/مرورگر</span>
              </button>
            </div>

            <div className="flex flex-wrap gap-2">
              {SIMULATED_NOTIFICATION_PRESETS.map((preset, idx) => (
                <button
                  key={idx}
                  type="button"
                  onClick={() =>
                    onSimulateIncomingNotification({
                      rawText: preset.rawText,
                      senderId: preset.senderId,
                      bankId: preset.bankId,
                      sourceType: preset.sourceType,
                      sourceAppLabel: preset.sourceAppLabel,
                    })
                  }
                  className={`text-[11px] px-3 py-1.5 rounded-xl border font-semibold transition hover:scale-[1.01] flex items-center space-x-1.5 space-x-reverse ${preset.badgeColor}`}
                >
                  <Sparkles className="w-3 h-3" />
                  <span>{preset.label}</span>
                </button>
              ))}
            </div>

            {/* Custom Notification Sender */}
            <div className="bg-slate-950/80 border border-slate-800 rounded-2xl p-3 space-y-2.5">
              <div className="text-[11px] text-slate-400 font-semibold">
                ارسال نوتیفیکیشن سفارشی جهت تست خواننده فیش:
              </div>
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
                <select
                  value={customSimBankId}
                  onChange={(e) => setCustomSimBankId(e.target.value)}
                  className="bg-slate-900 border border-slate-700 rounded-xl px-2.5 py-1.5 text-xs text-slate-200"
                >
                  {bankProfiles.map((bp) => (
                    <option key={bp.id} value={bp.id}>
                      {bp.bankName}
                    </option>
                  ))}
                </select>

                <select
                  value={customSimSource}
                  onChange={(e) =>
                    setCustomSimSource(e.target.value as NotificationSourceType)
                  }
                  className="bg-slate-900 border border-slate-700 rounded-xl px-2.5 py-1.5 text-xs text-slate-200"
                >
                  <option value="BANK_APP_PUSH">
                    نوتیفیکیشن اپلیکیشن بانک (Push)
                  </option>
                  <option value="SMS_INBOX">
                    نوتیفیکیشن پیامک سیم‌کارت (SMS)
                  </option>
                </select>

                <input
                  type="text"
                  value={customSimSender}
                  onChange={(e) => setCustomSimSender(e.target.value)}
                  placeholder="سرشماره یا نام پکیج (مثلاً 98300017)"
                  className="bg-slate-900 border border-slate-700 rounded-xl px-2.5 py-1.5 text-xs text-slate-200 font-mono"
                  dir="ltr"
                />
              </div>

              <div className="flex gap-2">
                <input
                  type="text"
                  value={customSimText}
                  onChange={(e) => setCustomSimText(e.target.value)}
                  placeholder="متن نوتیفیکیشن فیش بانکی را اینجا بنویسید یا جای‌گذاری کنید..."
                  className="flex-1 bg-slate-900 border border-slate-700 rounded-xl px-3 py-2 text-xs text-slate-200"
                />
                <button
                  type="button"
                  onClick={() => {
                    if (!customSimText.trim()) return;
                    const bankObj = bankProfiles.find(
                      (b) => b.id === customSimBankId
                    );
                    onSimulateIncomingNotification({
                      rawText: customSimText.trim(),
                      senderId: customSimSender.trim() || 'UNKNOWN',
                      bankId: customSimBankId,
                      sourceType: customSimSource,
                      sourceAppLabel:
                        customSimSource === 'BANK_APP_PUSH'
                          ? `همراه بانک ${bankObj?.bankName || ''}`
                          : `پیامک (${customSimSender || 'نامشخص'})`,
                    });
                    setCustomSimText('');
                  }}
                  className="px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold flex items-center space-x-1.5 space-x-reverse shrink-0 transition"
                >
                  <Send className="w-3.5 h-3.5" />
                  <span>ارسال نوتیفیکیشن</span>
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Filter Tabs (Mirza-style Logs & Smart Categories) */}
        <div className="px-5 pt-3 bg-slate-950/40 border-b border-slate-800 flex items-center gap-2 overflow-x-auto">
          {[
            {
              id: 'all',
              label: 'همه اعلان‌ها',
              count: notifications.length,
            },
            {
              id: 'pending_action',
              label: 'در انتظار تأیید / ثبت فیش',
              count: pendingActionCount,
              highlight: pendingActionCount > 0,
            },
            {
              id: 'bank_receipt',
              label: 'فیش‌های بانکی و پیامک',
              count: notifications.filter(
                (n) =>
                  n.category === 'bank_receipt' &&
                  n.actionState !== 'FILTERED_NON_FINANCIAL'
              ).length,
            },
            {
              id: 'smart_alerts',
              label: 'هشدارهای مالی و روزانه',
              count: notifications.filter(
                (n) =>
                  n.category === 'financial_alert' ||
                  n.category === 'tasks_habits' ||
                  n.category === 'health_system'
              ).length,
            },
            {
              id: 'filtered_logs',
              label: 'لاگ فیلترشده (OTP / تبلیغاتی)',
              count: filteredLogsCount,
            },
          ].map((tab) => {
            const active = activeFilter === tab.id;
            return (
              <button
                key={tab.id}
                type="button"
                onClick={() => setActiveFilter(tab.id as any)}
                className={`pb-3 px-3 text-xs font-bold border-b-2 transition whitespace-nowrap flex items-center space-x-1.5 space-x-reverse ${
                  active
                    ? 'border-cyan-400 text-cyan-400'
                    : 'border-transparent text-slate-400 hover:text-slate-200'
                }`}
              >
                <span>{tab.label}</span>
                <span
                  className={`text-[10px] px-1.5 py-0.5 rounded-full ${
                    tab.highlight
                      ? 'bg-amber-500 text-slate-950 font-extrabold'
                      : active
                      ? 'bg-cyan-500/20 text-cyan-300'
                      : 'bg-slate-800 text-slate-400'
                  }`}
                >
                  {tab.count}
                </span>
              </button>
            );
          })}
        </div>

        {/* Notification Feed List */}
        <div className="flex-1 overflow-y-auto p-4 sm:p-5 space-y-3">
          {filteredList.length === 0 ? (
            <div className="text-center py-14 space-y-3">
              <div className="w-14 h-14 rounded-2xl bg-slate-800/80 border border-slate-700/60 mx-auto flex items-center justify-center text-slate-500">
                <Bell className="w-7 h-7" />
              </div>
              <p className="text-sm font-bold text-slate-300">
                هیچ نوتیفیکیشنی در این بخش وجود ندارد
              </p>
              <p className="text-xs text-slate-500 max-w-md mx-auto">
                با زدن دکمه «تست دریافت نوتیفیکیشن فیش» در بالا، می‌توانید نحوه عملکرد خواننده نوتیفیکیشن بلوبانک، ملی، ملت و فیلتر ضد رمز پویا را آزمایش کنید.
              </p>
            </div>
          ) : (
            filteredList.map((notif) => {
              const isBank = notif.category === 'bank_receipt';
              const isPending = notif.actionState === 'PENDING_USER_ACTION';
              const isAutoRegistered = notif.actionState === 'AUTO_REGISTERED';
              const isManuallyRegistered =
                notif.actionState === 'REGISTERED_MANUALLY';
              const isFiltered =
                notif.actionState === 'FILTERED_NON_FINANCIAL';
              const isRejected = notif.actionState === 'REJECTED_BY_USER';
              const chosenCat =
                selectedCategories[notif.id] ||
                notif.suggestedCategory ||
                'خرید روزانه';

              return (
                <div
                  key={notif.id}
                  onClick={() => {
                    if (!notif.read) onMarkAsRead(notif.id);
                  }}
                  className={`rounded-2xl p-4 border transition relative ${
                    !notif.read
                      ? 'bg-slate-900/95 border-cyan-500/40 shadow-lg shadow-cyan-950/30'
                      : 'bg-slate-950/60 border-slate-800/80'
                  }`}
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex items-start space-x-3 space-x-reverse">
                      {/* Icon or Bank Emblem */}
                      <div className="w-11 h-11 rounded-2xl bg-slate-900 border border-slate-800 flex items-center justify-center shrink-0 overflow-hidden p-1">
                        {notif.bankId ? (
                          <BankEmblemSvg
                            presetId={notif.bankId}
                            className="w-9 h-9"
                          />
                        ) : notif.category === 'financial_alert' ? (
                          <AlertTriangle className="w-5 h-5 text-amber-400" />
                        ) : (
                          <BellRing className="w-5 h-5 text-cyan-400" />
                        )}
                      </div>

                      <div className="space-y-1">
                        {/* Badges Row */}
                        <div className="flex flex-wrap items-center gap-1.5">
                          {!notif.read && (
                            <span className="w-2 h-2 rounded-full bg-cyan-400" />
                          )}
                          <span className="text-xs font-extrabold text-slate-100">
                            {notif.title}
                          </span>

                          {notif.sourceAppLabel && (
                            <span
                              className={`text-[10px] px-2 py-0.5 rounded-full font-medium flex items-center space-x-1 space-x-reverse ${
                                notif.sourceType === 'BANK_APP_PUSH'
                                  ? 'bg-sky-500/15 text-sky-300 border border-sky-500/30'
                                  : 'bg-slate-800 text-slate-300 border border-slate-700'
                              }`}
                            >
                              {notif.sourceType === 'BANK_APP_PUSH' ? (
                                <Smartphone className="w-2.5 h-2.5" />
                              ) : (
                                <MessageSquare className="w-2.5 h-2.5" />
                              )}
                              <span>{notif.sourceAppLabel}</span>
                            </span>
                          )}

                          {isAutoRegistered && (
                            <span className="text-[10px] px-2 py-0.5 rounded-full bg-emerald-500/15 text-emerald-300 border border-emerald-500/30 font-bold flex items-center gap-1">
                              <CheckCircle2 className="w-3 h-3" />
                              <span>ثبت خودکار شده</span>
                            </span>
                          )}
                          {isManuallyRegistered && (
                            <span className="text-[10px] px-2 py-0.5 rounded-full bg-cyan-500/15 text-cyan-300 border border-cyan-500/30 font-bold flex items-center gap-1">
                              <Check className="w-3 h-3" />
                              <span>تأیید و ثبت شده</span>
                            </span>
                          )}
                          {isPending && (
                            <span className="text-[10px] px-2 py-0.5 rounded-full bg-amber-500/20 text-amber-300 border border-amber-500/40 font-bold">
                              در انتظار تأیید / ثبت فیش
                            </span>
                          )}
                          {isFiltered && (
                            <span className="text-[10px] px-2 py-0.5 rounded-full bg-slate-800 text-slate-400 border border-slate-700 font-semibold flex items-center gap-1">
                              <Filter className="w-2.5 h-2.5" />
                              <span>فیلتر شده (غیرمالی / OTP)</span>
                            </span>
                          )}
                          {isRejected && (
                            <span className="text-[10px] px-2 py-0.5 rounded-full bg-rose-500/15 text-rose-300 border border-rose-500/30 font-semibold">
                              رد شده توسط کاربر
                            </span>
                          )}
                        </div>

                        <p className="text-xs text-slate-300 leading-relaxed">
                          {notif.body}
                        </p>

                        {isBank && notif.analysisResult && (
                          <div className="flex flex-wrap items-center gap-x-3 gap-y-1 pt-1 text-[11px] text-slate-400">
                            {notif.analysisResult.amountInTomans !==
                              undefined && (
                              <span>
                                مبلغ تشخیص‌داده‌شده:{' '}
                                <strong className="text-emerald-400">
                                  {formatToman(
                                    notif.analysisResult.amountInTomans
                                  )}
                                </strong>
                              </span>
                            )}
                            {notif.analysisResult.transactionKind !==
                              'UNKNOWN' && (
                              <span>
                                نوع:{' '}
                                <strong className="text-cyan-300">
                                  {translateTransactionKind(
                                    notif.analysisResult.transactionKind
                                  )}
                                </strong>
                              </span>
                            )}
                            {notif.analysisResult.balanceInTomans !==
                              undefined && (
                              <span>
                                مانده حساب:{' '}
                                <strong className="text-slate-200">
                                  {formatToman(
                                    notif.analysisResult.balanceInTomans
                                  )}
                                </strong>
                              </span>
                            )}
                            {notif.analysisResult.matchedTemplateName && (
                              <span>
                                قالب:{' '}
                                <strong className="text-slate-300">
                                  {notif.analysisResult.matchedTemplateName}
                                </strong>
                              </span>
                            )}
                          </div>
                        )}

                        {notif.rawText && (
                          <div className="mt-2 p-2.5 rounded-xl bg-slate-900/90 border border-slate-800/90 text-[11px] text-slate-400 font-mono whitespace-pre-line">
                            {notif.rawText}
                          </div>
                        )}
                      </div>
                    </div>

                    <div className="flex flex-col items-end gap-2 shrink-0">
                      <span className="text-[10px] text-slate-500">
                        {notif.timestamp}
                      </span>
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          onDeleteNotification(notif.id);
                        }}
                        className="p-1.5 rounded-lg text-slate-500 hover:text-rose-400 hover:bg-rose-500/10 transition"
                        title="حذف اعلان"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>

                  {isPending && notif.analysisResult && (
                    <div className="mt-3 pt-3 border-t border-slate-800/80 space-y-3">
                      <div className="flex flex-wrap items-center gap-1.5">
                        <span className="text-[10px] text-slate-400 ml-1">
                          دسته‌بندی فیش:
                        </span>
                        {QUICK_CATEGORIES.map((cat) => (
                          <button
                            key={cat}
                            type="button"
                            onClick={() =>
                              setSelectedCategories((prev) => ({
                                ...prev,
                                [notif.id]: cat,
                              }))
                            }
                            className={`text-[10px] px-2.5 py-1 rounded-lg border transition ${
                              chosenCat === cat
                                ? 'bg-cyan-500/20 text-cyan-300 border-cyan-500/40 font-bold'
                                : 'bg-slate-900 text-slate-400 border-slate-800 hover:text-slate-200'
                            }`}
                          >
                            {cat}
                          </button>
                        ))}
                      </div>

                      {editingId === notif.id && (
                        <div className="grid grid-cols-1 sm:grid-cols-4 gap-2 p-3 rounded-xl bg-slate-900 border border-amber-500/30 text-xs">
                          <div>
                            <label className="text-[10px] text-slate-400 block mb-1">
                              بانک *:
                            </label>
                            <select
                              value={editBankId}
                              onChange={(e) => setEditBankId(e.target.value)}
                              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-2 py-1.5 text-xs text-slate-200"
                            >
                              {bankProfiles.map((bp) => (
                                <option key={bp.id} value={bp.id}>
                                  {bp.bankName}
                                </option>
                              ))}
                            </select>
                          </div>

                          <div>
                            <label className="text-[10px] text-slate-400 block mb-1">
                              نوع تراکنش *:
                            </label>
                            <select
                              value={editKind}
                              onChange={(e) =>
                                setEditKind(
                                  e.target.value as SmsTransactionKind
                                )
                              }
                              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-2 py-1.5 text-xs text-cyan-300"
                            >
                              <option value="PURCHASE">خرید / پرداخت</option>
                              <option value="WITHDRAWAL">برداشت وجه</option>
                              <option value="DEPOSIT">واریز به حساب</option>
                              <option value="TRANSFER">انتقال وجه</option>
                              <option value="FEE">کارمزد</option>
                            </select>
                          </div>

                          <div>
                            <label className="text-[10px] text-slate-400 block mb-1">
                              واحد پول *:
                            </label>
                            <select
                              value={editCurrency}
                              onChange={(e) =>
                                setEditCurrency(e.target.value as CurrencyUnit)
                              }
                              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-2 py-1.5 text-xs text-slate-200"
                            >
                              <option value="RIAL">ریال (÷۱۰ تومان)</option>
                              <option value="TOMAN">تومان</option>
                            </select>
                          </div>

                          <div>
                            <label className="text-[10px] text-slate-400 block mb-1">
                              مبلغ خام *:
                            </label>
                            <input
                              type="number"
                              value={editAmountRaw}
                              onChange={(e) => setEditAmountRaw(e.target.value)}
                              className="w-full bg-slate-950 border border-slate-700 rounded-lg px-2 py-1.5 text-xs text-emerald-400 font-bold"
                            />
                          </div>
                        </div>
                      )}

                      {actionFeedback && actionFeedback.id === notif.id && (
                        <div
                          className={`p-2.5 rounded-xl text-xs ${
                            actionFeedback.type === 'success'
                              ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
                              : 'bg-rose-500/20 text-rose-300 border border-rose-500/30'
                          }`}
                        >
                          {actionFeedback.text}
                        </div>
                      )}

                      <div className="flex flex-wrap items-center justify-between gap-2">
                        <span className="text-[11px] text-amber-300/90">
                          علت توقف ثبت خودکار: {notif.analysisResult.reason}
                        </span>

                        <div className="flex flex-wrap items-center gap-2">
                          <button
                            type="button"
                            onClick={() =>
                              handleRejectNotificationReceipt(notif)
                            }
                            className="px-3 py-1.5 rounded-xl bg-rose-500/15 border border-rose-500/30 text-rose-300 text-xs hover:bg-rose-500/25 transition flex items-center space-x-1 space-x-reverse"
                          >
                            <Ban className="w-3.5 h-3.5" />
                            <span>رد فیش</span>
                          </button>

                          {editingId !== notif.id && (
                            <button
                              type="button"
                              onClick={() => handleOpenInlineEdit(notif)}
                              className="px-3 py-1.5 rounded-xl bg-slate-800 text-slate-200 text-xs hover:bg-slate-700 transition flex items-center space-x-1 space-x-reverse"
                            >
                              <SlidersHorizontal className="w-3.5 h-3.5" />
                              <span>اصلاح مقادیر</span>
                            </button>
                          )}

                          <button
                            type="button"
                            onClick={() =>
                              handleConfirmRegisterNotification(notif, true)
                            }
                            className="px-3 py-1.5 rounded-xl bg-cyan-500/20 border border-cyan-500/40 text-cyan-300 text-xs font-bold hover:bg-cyan-500/30 transition flex items-center space-x-1 space-x-reverse"
                          >
                            <Sparkles className="w-3.5 h-3.5" />
                            <span>ثبت + تأیید دائمی الگوی بانک</span>
                          </button>

                          <button
                            type="button"
                            onClick={() =>
                              handleConfirmRegisterNotification(notif, false)
                            }
                            className="px-3.5 py-1.5 rounded-xl bg-[#1f7a38] hover:bg-[#19662e] text-white text-xs font-extrabold transition flex items-center space-x-1 space-x-reverse shadow-md shadow-emerald-950/50"
                          >
                            <PlusCircle className="w-3.5 h-3.5" />
                            <span>تأیید و ثبت فیش</span>
                          </button>
                        </div>
                      </div>
                    </div>
                  )}

                  {notif.targetTab && !isPending && (
                    <div className="mt-2.5 flex justify-end">
                      <button
                        type="button"
                        onClick={() => {
                          onMarkAsRead(notif.id);
                          onNavigateTab(notif.targetTab);
                          onClose();
                        }}
                        className="text-[11px] px-3 py-1 rounded-lg bg-slate-800 text-cyan-400 hover:bg-slate-700 transition flex items-center space-x-1 space-x-reverse"
                      >
                        <Eye className="w-3 h-3" />
                        <span>مشاهده بخش مرتبط</span>
                      </button>
                    </div>
                  )}
                </div>
              );
            })
          )}
        </div>
      </div>
    </div>
  );
};

interface FloatingNotificationBannerProps {
  notification: AppNotification | null;
  onDismiss: () => void;
  onOpenCenter: () => void;
  onQuickApprove?: (notif: AppNotification) => void;
}

export const FloatingNotificationBanner: React.FC<
  FloatingNotificationBannerProps
> = ({ notification, onDismiss, onOpenCenter, onQuickApprove }) => {
  if (!notification) return null;

  const canQuickApprove =
    notification.actionState === 'PENDING_USER_ACTION' &&
    notification.analysisResult &&
    notification.analysisResult.transactionKind !== 'UNKNOWN' &&
    notification.analysisResult.currency !== 'UNKNOWN' &&
    Boolean(notification.analysisResult.amountInTomans);

  return (
    <div className="fixed top-4 left-1/2 -translate-x-1/2 z-[60] w-[94%] max-w-lg animate-fadeIn">
      <div className="bg-slate-900/95 backdrop-blur-xl border border-cyan-500/40 rounded-2xl p-4 shadow-2xl shadow-cyan-950/60 flex items-start justify-between gap-3">
        <div className="flex items-start space-x-3 space-x-reverse">
          <div className="w-10 h-10 rounded-xl bg-slate-950 border border-slate-800 flex items-center justify-center shrink-0 p-1">
            {notification.bankId ? (
              <BankEmblemSvg
                presetId={notification.bankId}
                className="w-8 h-8"
              />
            ) : (
              <BellRing className="w-5 h-5 text-cyan-400 animate-bounce" />
            )}
          </div>
          <div>
            <div className="flex items-center gap-2 flex-wrap">
              <span className="text-xs font-extrabold text-slate-100">
                {notification.title}
              </span>
              {notification.sourceAppLabel && (
                <span className="text-[10px] px-2 py-0.5 rounded-full bg-cyan-500/15 text-cyan-300 border border-cyan-500/30">
                  {notification.sourceAppLabel}
                </span>
              )}
            </div>
            <p className="text-xs text-slate-300 mt-1 line-clamp-2">
              {notification.body}
            </p>
            <div className="flex items-center gap-2 mt-2.5">
              {canQuickApprove && onQuickApprove && (
                <button
                  type="button"
                  onClick={() => onQuickApprove(notification)}
                  className="px-3 py-1 rounded-lg bg-[#1f7a38] hover:bg-[#19662e] text-white text-[11px] font-bold transition"
                >
                  + ثبت سریع فیش
                </button>
              )}
              <button
                type="button"
                onClick={onOpenCenter}
                className="px-3 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-cyan-300 text-[11px] font-semibold transition"
              >
                مشاهده در مرکز نوتیفیکیشن
              </button>
            </div>
          </div>
        </div>

        <button
          type="button"
          onClick={onDismiss}
          className="p-1 rounded-lg text-slate-400 hover:text-white"
        >
          <X className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
};
