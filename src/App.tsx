import React, { useState, useEffect } from 'react';
import { Sidebar } from './components/Sidebar';
import { DashboardScreen } from './components/DashboardScreen';
import { TasksScreen } from './components/TasksScreen';
import { ExpensesScreen } from './components/ExpensesScreen';
import { HealthScreen } from './components/HealthScreen';
import { VaultScreen } from './components/VaultScreen';
import { AssistantScreen } from './components/AssistantScreen';
import { SettingsScreen } from './components/SettingsScreen';
import { VoiceCaptureModal } from './components/VoiceCaptureModal';
import {
  NotificationCenterModal,
  FloatingNotificationBanner,
} from './components/NotificationCenterModal';
import {
  TabType,
  TaskItem,
  HabitItem,
  ExpenseItem,
  HealthLog,
  VaultNote,
  ChatMessage,
  BankProfile,
  AppNotification,
  NotificationSettings,
  NotificationSourceType,
} from './types';
import {
  analyzeBankSms,
  buildValidatedExpenseEntry,
  formatToman,
  loadBankProfiles,
  saveBankProfiles,
  translateTransactionKind,
} from './utils/persianUtils';

const DEFAULT_NOTIFICATION_SETTINGS: NotificationSettings = {
  enabled: true,
  bankNotificationListener: true,
  autoRegisterVerifiedNotifications: true,
  showFloatingHeadsUpBanner: true,
  filterOtpAndPromotional: true,
  allowedBankSources: {
    smsInbox: true,
    bluBankApp: true,
    wepodApp: true,
    melliBamApp: true,
    mellatPlusApp: true,
    otherBankApps: true,
  },
  highExpenseThresholdTomans: 2000000,
  lowBalanceThresholdTomans: 1000000,
  taskDueReminders: true,
  habitStreakReminders: true,
  healthHydrationReminders: true,
  browserPushEnabled: false,
  quietHoursEnabled: false,
  quietHoursStart: '23:00',
  quietHoursEnd: '07:00',
};

export default function App() {
  const [currentTab, setCurrentTab] = useState<TabType>('dashboard');
  const [isRtl, setIsRtl] = useState(true);
  const [isOpenMobile, setIsOpenMobile] = useState(false);
  const [isVoiceModalOpen, setIsVoiceModalOpen] = useState(false);
  const [isNotificationCenterOpen, setIsNotificationCenterOpen] = useState(false);
  const [floatingBannerNotif, setFloatingBannerNotif] =
    useState<AppNotification | null>(null);

  // Persistent Bank Profiles
  const [bankProfiles, setBankProfiles] = useState<BankProfile[]>(() =>
    loadBankProfiles()
  );

  // Persistent Notification Settings
  const [notificationSettings, setNotificationSettings] =
    useState<NotificationSettings>(() => {
      const saved = localStorage.getItem('life_os_notification_settings');
      if (saved) {
        try {
          return { ...DEFAULT_NOTIFICATION_SETTINGS, ...JSON.parse(saved) };
        } catch {
          /* ignore */
        }
      }
      return DEFAULT_NOTIFICATION_SETTINGS;
    });

  // Persistent State with localStorage
  const [tasks, setTasks] = useState<TaskItem[]>(() => {
    const saved = localStorage.getItem('life_os_tasks');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch {
        /* ignore */
      }
    }
    return [
      {
        id: '1',
        title: 'بررسی و تایید گزارش مالی شرکت',
        category: 'work',
        priority: 'urgent',
        completed: false,
        dueDate: '۱۴۰۵/۰۸/۰۶',
        createdAt: '1405/08/01',
      },
      {
        id: '2',
        title: 'ورزش هوازی و بدن‌سازی (۴۵ دقیقه)',
        category: 'health',
        priority: 'high',
        completed: true,
        dueDate: '۱۴۰۵/۰۸/۰۵',
        createdAt: '1405/08/01',
      },
      {
        id: '3',
        title: 'مطالعه کتاب معماری نرم‌افزار',
        category: 'study',
        priority: 'medium',
        completed: false,
        dueDate: '۱۴۰۵/۰۸/۰۸',
        createdAt: '1405/08/02',
      },
      {
        id: '4',
        title: 'پرداخت اقساط وام و بررسی حساب‌ها',
        category: 'finance',
        priority: 'urgent',
        completed: false,
        dueDate: '۱۴۰۵/۰۸/۰۷',
        createdAt: '1405/08/03',
      },
    ];
  });

  const [habits, setHabits] = useState<HabitItem[]>(() => {
    const saved = localStorage.getItem('life_os_habits');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch {
        /* ignore */
      }
    }
    return [
      {
        id: 'h1',
        title: 'مطالعه ۳۰ دقیقه روزانه',
        streak: 7,
        completedToday: true,
        category: 'study',
      },
      {
        id: 'h2',
        title: 'ورزش و نرمش صبحگاهی',
        streak: 5,
        completedToday: false,
        category: 'health',
      },
      {
        id: 'h3',
        title: 'نوشیدن ۸ لیوان آب',
        streak: 12,
        completedToday: true,
        category: 'health',
      },
    ];
  });

  const [expenses, setExpenses] = useState<ExpenseItem[]>(() => {
    const saved = localStorage.getItem('life_os_expenses');
    if (saved) {
      try {
        const parsed = JSON.parse(saved);
        if (Array.isArray(parsed)) {
          return parsed.filter(
            (item) =>
              item &&
              typeof item.title === 'string' &&
              typeof item.amount === 'number' &&
              Number.isFinite(item.amount)
          );
        }
      } catch {
        /* ignore */
      }
    }
    return [
      {
        id: 'e1',
        title: 'خرید از فروشگاه رفاه',
        amount: 1250000,
        category: 'خرید عمومی',
        date: '۱۴۰۵/۰۸/۰۵',
        type: 'expense',
        isSmsParsed: true,
        bankId: 'mellat',
        bankName: 'بانک ملت',
      },
      {
        id: 'e2',
        title: 'کافه و میان‌وعده کاری',
        amount: 350000,
        category: 'غذا و رستوران',
        date: '۱۴۰۵/۰۸/۰۴',
        type: 'expense',
        isSmsParsed: false,
      },
      {
        id: 'e3',
        title: 'شارژ کارت مترو و تاکسی',
        amount: 180000,
        category: 'حمل و نقل',
        date: '۱۴۰۵/۰۸/۰۳',
        type: 'expense',
        isSmsParsed: true,
        bankId: 'saman',
        bankName: 'بانک سامان',
      },
    ];
  });

  // Persistent Smart Notifications & Bank Receipt Logs
  const [notifications, setNotifications] = useState<AppNotification[]>(() => {
    const saved = localStorage.getItem('life_os_notifications');
    if (saved) {
      try {
        const parsed = JSON.parse(saved);
        if (Array.isArray(parsed)) return parsed;
      } catch {
        /* ignore */
      }
    }

    const pendingWepodSms =
      'ویپاد\nخرید کارتی\nمبلغ: ۳,۴۵۰,۰۰۰ ریال\nمانده: ۴۱,۲۰۰,۰۰۰ ریال\n۱۴۰۵/۰۸/۰۵ ۱۹:۴۰';
    const pendingWepodAnalysis = analyzeBankSms(pendingWepodSms, {
      selectedBankId: 'wepod',
      senderId: 'Wepod',
      profiles: loadBankProfiles(),
      existingExpenses: [],
    });

    const autoBluSms =
      'بلو\nخرید کارتی\nمبلغ: ۱,۸۵۰,۰۰۰ ریال\nاز حساب شما پرید\nمانده: ۲۴,۵۰۰,۰۰۰ ریال\n۱۴۰۵/۰۸/۰۵ ۱۸:۱۰';
    const autoBluAnalysis = analyzeBankSms(autoBluSms, {
      selectedBankId: 'blu',
      senderId: 'BLU',
      profiles: loadBankProfiles(),
      existingExpenses: [],
    });

    const otpSms =
      'بانک ملت\nرمز دوم پویا: ۷۳۹۱۰۴\nمبلغ: ۴,۵۰۰,۰۰۰ ریال\nکارت: ۶۱۰۴******۱۲۳۴';
    const otpAnalysis = analyzeBankSms(otpSms, {
      selectedBankId: 'mellat',
      senderId: '98200033',
      profiles: loadBankProfiles(),
      existingExpenses: [],
    });

    return [
      {
        id: 'notif_wepod_pending_1',
        title: 'فیش جدید نیازمند تأیید — ویپاد (پاسارگاد)',
        body: 'برداشت ۳۴۵,۰۰۰ تومان در نوتیفیکیشن ویپاد شناسایی شد. به دلیل تأییدنشدن قالب این بانک، منتظر تأیید ۱-کلیکی شماست.',
        rawText: pendingWepodSms,
        timestamp: '۱۹:۴۰',
        read: false,
        priority: 'high',
        category: 'bank_receipt',
        sourceType: 'BANK_APP_PUSH',
        sourceAppLabel: 'اپلیکیشن ویپاد (ir.bpi.wepod)',
        senderId: 'Wepod',
        bankId: 'wepod',
        bankName: 'ویپاد (بانک پاسارگاد)',
        actionState: 'PENDING_USER_ACTION',
        analysisResult: pendingWepodAnalysis,
        suggestedCategory: 'خرید روزانه',
        targetTab: 'expenses',
      },
      {
        id: 'notif_blu_auto_2',
        title: 'ثبت خودکار فیش — بلوبانک (سامان)',
        body: 'خرید کارتی به مبلغ ۱۸۵,۰۰۰ تومان با موفقیت از طریق شنونده نوتیفیکیشن ثبت شد.',
        rawText: autoBluSms,
        timestamp: '۱۸:۱۰',
        read: false,
        priority: 'medium',
        category: 'bank_receipt',
        sourceType: 'BANK_APP_PUSH',
        sourceAppLabel: 'بلوبانک (ir.samanpr.blu)',
        senderId: 'BLU',
        bankId: 'blu',
        bankName: 'بلوبانک (سامان)',
        actionState: 'AUTO_REGISTERED',
        analysisResult: autoBluAnalysis,
        suggestedCategory: 'خرید روزانه',
        targetTab: 'expenses',
      },
      {
        id: 'notif_task_urgent_3',
        title: 'یادآور تسک فوری مالی و کاری',
        body: 'تسک «پرداخت اقساط وام و بررسی حساب‌ها» و «بررسی گزارش مالی شرکت» نزدیک سررسید هستند.',
        timestamp: '۱۶:۳۰',
        read: false,
        priority: 'urgent',
        category: 'tasks_habits',
        sourceType: 'SMART_ENGINE',
        sourceAppLabel: 'موتور هوشمند یادآوری',
        actionState: 'INFO_ONLY',
        targetTab: 'tasks',
      },
      {
        id: 'notif_otp_filtered_4',
        title: 'فیلتر امنیتی رمز پویا (OTP) — بانک ملت',
        body: 'پیامک حاوی رمز دوم پویا تشخیص داده شد و برای جلوگیری از خطای ثبت مالی مسدود گردید.',
        rawText: otpSms,
        timestamp: '۱۴:۲۲',
        read: true,
        priority: 'low',
        category: 'bank_receipt',
        sourceType: 'SMS_INBOX',
        sourceAppLabel: 'صندوق پیامک (98200033)',
        senderId: '98200033',
        bankId: 'mellat',
        bankName: 'بانک ملت',
        actionState: 'FILTERED_NON_FINANCIAL',
        analysisResult: otpAnalysis,
      },
    ];
  });

  const [healthLog, setHealthLog] = useState<HealthLog>(() => {
    const saved = localStorage.getItem('life_os_health');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch {
        /* ignore */
      }
    }
    return {
      id: 'hl1',
      date: '۱۴۰۵/۰۸/۰۵',
      sleepHours: 7.5,
      sleepQuality: 'good',
      waterGlasses: 6,
      exerciseMinutes: 45,
      exerciseType: 'بدن‌سازی و هوازی',
      caloriesBurned: 480,
    };
  });

  const [vaultNotes, setVaultNotes] = useState<VaultNote[]>(() => {
    const saved = localStorage.getItem('life_os_vault');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch {
        /* ignore */
      }
    }
    return [
      {
        id: 'v1',
        title: 'رمز کیف پول سخت‌افزاری',
        content: 'Seed: apple banana cherry ... secret pin: 9942',
        category: 'حساب‌های بانکی',
        updatedAt: '۱۴۰۵/۰۸/۰۱',
        isEncrypted: true,
      },
      {
        id: 'v2',
        title: 'یادداشت محرمانه بورسی',
        content: 'رمز عبور کارگزاری مفید و حساب‌های صرافی',
        category: 'رمزهای عبور',
        updatedAt: '۱۴۰۵/۰۸/۰۳',
        isEncrypted: true,
      },
    ];
  });

  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      id: 'm1',
      sender: 'gemini',
      text: 'سلام! من دستیار هوشمند جمینای شما در Personal Life OS هستم. تمام داده‌ها، لاگ نوتیفیکیشن فیش‌ها و پروفایل‌های بانکی شما به صورت خودکار و امن در حافظه محلی ذخیره می‌شوند. چطور می‌توانم کمک کنم؟',
      timestamp: '۱۰:۰۰',
    },
  ]);

  // Save to localStorage whenever state changes
  useEffect(() => {
    saveBankProfiles(bankProfiles);
  }, [bankProfiles]);

  useEffect(() => {
    localStorage.setItem(
      'life_os_notification_settings',
      JSON.stringify(notificationSettings)
    );
  }, [notificationSettings]);

  useEffect(() => {
    localStorage.setItem('life_os_notifications', JSON.stringify(notifications));
  }, [notifications]);

  useEffect(() => {
    localStorage.setItem('life_os_tasks', JSON.stringify(tasks));
  }, [tasks]);

  useEffect(() => {
    localStorage.setItem('life_os_habits', JSON.stringify(habits));
  }, [habits]);

  useEffect(() => {
    localStorage.setItem('life_os_expenses', JSON.stringify(expenses));
  }, [expenses]);

  useEffect(() => {
    localStorage.setItem('life_os_health', JSON.stringify(healthLog));
  }, [healthLog]);

  useEffect(() => {
    localStorage.setItem('life_os_vault', JSON.stringify(vaultNotes));
  }, [vaultNotes]);

  // Handlers for Tasks
  const handleAddTask = (task: Omit<TaskItem, 'id' | 'createdAt'>) => {
    const newItem: TaskItem = {
      ...task,
      id: Date.now().toString(),
      createdAt: '۱۴۰۵/۰۸/۰۵',
    };
    setTasks((prev) => [newItem, ...prev]);
  };

  const handleToggleTask = (id: string) => {
    setTasks((prev) =>
      prev.map((t) => (t.id === id ? { ...t, completed: !t.completed } : t))
    );
  };

  const handleDeleteTask = (id: string) => {
    setTasks((prev) => prev.filter((t) => t.id !== id));
  };

  // Handlers for Habits
  const handleToggleHabit = (id: string) => {
    setHabits((prev) =>
      prev.map((h) =>
        h.id === id
          ? {
              ...h,
              completedToday: !h.completedToday,
              streak: !h.completedToday
                ? h.streak + 1
                : Math.max(0, h.streak - 1),
            }
          : h
      )
    );
  };

  const handleAddHabit = (title: string) => {
    const newHabit: HabitItem = {
      id: Date.now().toString(),
      title,
      streak: 1,
      completedToday: false,
      category: 'health',
    };
    setHabits((prev) => [newHabit, ...prev]);
  };

  // Handlers for Expenses (final storage gate with strict validation & idempotency)
  const handleAddExpense = (expense: Omit<ExpenseItem, 'id'>) => {
    if (
      !expense ||
      typeof expense.title !== 'string' ||
      !expense.title.trim() ||
      typeof expense.amount !== 'number' ||
      !Number.isFinite(expense.amount) ||
      expense.amount <= 0
    ) {
      return;
    }
    const allowedTypes = ['expense', 'income', 'transfer'];
    if (expense.type !== undefined && !allowedTypes.includes(expense.type)) {
      return;
    }
    if (expense.isSmsParsed) {
      if (
        !expense.type ||
        !allowedTypes.includes(expense.type) ||
        !expense.bankId ||
        !expense.bankName
      ) {
        return;
      }
    }
    setExpenses((prev) => {
      if (
        expense.messageFingerprint &&
        prev.some((e) => e.messageFingerprint === expense.messageFingerprint)
      ) {
        return prev;
      }
      if (
        expense.isSmsParsed &&
        expense.trackingCode &&
        expense.bankId &&
        prev.some(
          (e) =>
            e.bankId === expense.bankId &&
            e.trackingCode === expense.trackingCode &&
            e.amount === expense.amount
        )
      ) {
        return prev;
      }
      const newItem: ExpenseItem = {
        ...expense,
        id: Date.now().toString() + '_' + Math.random().toString(36).slice(2, 5),
      };
      return [newItem, ...prev];
    });
  };

  const handleDeleteExpense = (id: string) => {
    setExpenses((prev) => prev.filter((e) => e.id !== id));
  };

  // Handlers for Notification Center & Bank Receipt Listener
  const handleMarkNotificationRead = (id: string) => {
    setNotifications((prev) =>
      prev.map((n) => (n.id === id ? { ...n, read: true } : n))
    );
  };

  const handleMarkAllNotificationsRead = () => {
    setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
  };

  const handleClearNotifications = (onlyRead?: boolean) => {
    if (onlyRead) {
      setNotifications((prev) => prev.filter((n) => !n.read));
    } else {
      setNotifications([]);
    }
  };

  const handleDeleteNotification = (id: string) => {
    setNotifications((prev) => prev.filter((n) => n.id !== id));
  };

  const handleUpdateNotification = (updated: AppNotification) => {
    setNotifications((prev) =>
      prev.map((n) => (n.id === updated.id ? updated : n))
    );
  };

  const handleRegisterExpenseFromNotification = (
    notificationId: string,
    entry: Omit<ExpenseItem, 'id'>
  ) => {
    handleAddExpense(entry);
    setNotifications((prev) =>
      prev.map((n) =>
        n.id === notificationId
          ? {
              ...n,
              read: true,
              actionState: 'REGISTERED_MANUALLY',
            }
          : n
      )
    );
    if (floatingBannerNotif?.id === notificationId) {
      setFloatingBannerNotif(null);
    }
  };

  const handleSimulateIncomingNotification = (payload: {
    rawText: string;
    senderId: string;
    bankId?: string;
    sourceType: NotificationSourceType;
    sourceAppLabel: string;
  }) => {
    if (!notificationSettings.enabled || !notificationSettings.bankNotificationListener) {
      return;
    }

    const nowTime = new Date().toLocaleTimeString('fa-IR', {
      hour: '2-digit',
      minute: '2-digit',
    });

    const analysis = analyzeBankSms(payload.rawText, {
      selectedBankId: payload.bankId,
      senderId: payload.senderId.trim() || undefined,
      profiles: bankProfiles,
      existingExpenses: expenses,
    });

    const resolvedBankName =
      analysis.bankName ||
      bankProfiles.find((p) => p.id === payload.bankId)?.bankName ||
      'بانک / پیامک دریافتی';

    // 1. Filtered non-financial / OTP / Promotional
    if (
      analysis.status === 'NOT_A_TRANSACTION' ||
      analysis.messageCategory === 'OTP' ||
      analysis.messageCategory === 'PROMOTIONAL'
    ) {
      if (!notificationSettings.filterOtpAndPromotional) return;
      const filteredNotif: AppNotification = {
        id: 'notif_' + Date.now(),
        title: `فیلتر امنیتی (${
          analysis.messageCategory === 'OTP' ? 'رمز پویا OTP' : 'پیام غیرمالی'
        }) — ${resolvedBankName}`,
        body: analysis.reason,
        rawText: payload.rawText,
        timestamp: nowTime,
        read: false,
        priority: 'low',
        category: 'bank_receipt',
        sourceType: payload.sourceType,
        sourceAppLabel: payload.sourceAppLabel,
        senderId: payload.senderId,
        bankId: analysis.bankId || payload.bankId,
        bankName: resolvedBankName,
        actionState: 'FILTERED_NON_FINANCIAL',
        analysisResult: analysis,
      };
      setNotifications((prev) => [filteredNotif, ...prev]);
      return;
    }

    // 2. Valid & Verified -> Auto-register if enabled
    if (
      analysis.status === 'VALID_TRANSACTION' &&
      analysis.canAutoRegister &&
      notificationSettings.autoRegisterVerifiedNotifications &&
      !analysis.isDuplicate
    ) {
      const validated = buildValidatedExpenseEntry(
        analysis,
        undefined,
        bankProfiles,
        expenses
      );

      if (validated.ok && validated.entry) {
        handleAddExpense(validated.entry);
        const autoNotif: AppNotification = {
          id: 'notif_' + Date.now(),
          title: `ثبت خودکار فیش — ${resolvedBankName}`,
          body: `${translateTransactionKind(
            analysis.transactionKind
          )} به مبلغ ${formatToman(
            validated.entry.amount
          )} به‌صورت خودکار از نوتیفیکیشن دریافتی ثبت شد.`,
          rawText: payload.rawText,
          timestamp: nowTime,
          read: false,
          priority: 'medium',
          category: 'bank_receipt',
          sourceType: payload.sourceType,
          sourceAppLabel: payload.sourceAppLabel,
          senderId: payload.senderId,
          bankId: analysis.bankId || payload.bankId,
          bankName: resolvedBankName,
          actionState: 'AUTO_REGISTERED',
          analysisResult: analysis,
          suggestedCategory: validated.entry.category,
          targetTab: 'expenses',
        };

        const generatedList: AppNotification[] = [autoNotif];

        // Check High Expense Threshold Alert
        if (
          validated.entry.type === 'expense' &&
          notificationSettings.highExpenseThresholdTomans > 0 &&
          validated.entry.amount >= notificationSettings.highExpenseThresholdTomans
        ) {
          generatedList.unshift({
            id: 'notif_alert_high_' + Date.now(),
            title: `⚠️ هشدار تراکنش سنگین (${resolvedBankName})`,
            body: `تراکنش ${formatToman(
              validated.entry.amount
            )} از سقف هشدار تعیین‌شده شما (${formatToman(
              notificationSettings.highExpenseThresholdTomans
            )}) فراتر رفت.`,
            timestamp: nowTime,
            read: false,
            priority: 'urgent',
            category: 'financial_alert',
            sourceType: 'SMART_ENGINE',
            sourceAppLabel: 'پایشگر هوشمند سقف تراکنش',
            bankId: analysis.bankId || payload.bankId,
            bankName: resolvedBankName,
            actionState: 'INFO_ONLY',
            targetTab: 'expenses',
          });
        }

        // Check Low Balance Threshold Alert
        if (
          analysis.balanceInTomans !== undefined &&
          notificationSettings.lowBalanceThresholdTomans > 0 &&
          analysis.balanceInTomans <= notificationSettings.lowBalanceThresholdTomans
        ) {
          generatedList.unshift({
            id: 'notif_alert_lowbal_' + Date.now(),
            title: `🔔 هشدار افت موجودی حساب (${resolvedBankName})`,
            body: `مانده حساب شما به ${formatToman(
              analysis.balanceInTomans
            )} رسید که کمتر از مرز هشدار (${formatToman(
              notificationSettings.lowBalanceThresholdTomans
            )}) است.`,
            timestamp: nowTime,
            read: false,
            priority: 'high',
            category: 'financial_alert',
            sourceType: 'SMART_ENGINE',
            sourceAppLabel: 'پایشگر مانده حساب',
            bankId: analysis.bankId || payload.bankId,
            bankName: resolvedBankName,
            actionState: 'INFO_ONLY',
            targetTab: 'expenses',
          });
        }

        setNotifications((prev) => [...generatedList, ...prev]);
        if (
          notificationSettings.showFloatingHeadsUpBanner &&
          !notificationSettings.quietHoursEnabled
        ) {
          setFloatingBannerNotif(generatedList[0]);
        }
        return;
      }
    }

    // 3. Pending User Action (Needs confirmation, template approval, or duplicate notice)
    const pendingNotif: AppNotification = {
      id: 'notif_' + Date.now(),
      title: `فیش بانکی در انتظار بررسی — ${resolvedBankName}`,
      body:
        analysis.amountInTomans !== undefined
          ? `مبلغ ${formatToman(analysis.amountInTomans)} (${translateTransactionKind(
              analysis.transactionKind
            )}) دریافت شد. ${analysis.reason}`
          : analysis.reason,
      rawText: payload.rawText,
      timestamp: nowTime,
      read: false,
      priority: 'high',
      category: 'bank_receipt',
      sourceType: payload.sourceType,
      sourceAppLabel: payload.sourceAppLabel,
      senderId: payload.senderId,
      bankId: analysis.bankId || payload.bankId,
      bankName: resolvedBankName,
      actionState: 'PENDING_USER_ACTION',
      analysisResult: analysis,
      suggestedCategory: 'خرید روزانه',
      targetTab: 'expenses',
    };

    setNotifications((prev) => [pendingNotif, ...prev]);
    if (
      notificationSettings.showFloatingHeadsUpBanner &&
      !notificationSettings.quietHoursEnabled
    ) {
      setFloatingBannerNotif(pendingNotif);
    }
  };

  // Handlers for Health
  const handleUpdateHealth = (log: Partial<HealthLog>) => {
    setHealthLog((prev) => ({ ...prev, ...log }));
  };

  // Handlers for Vault
  const handleAddVaultNote = (note: Omit<VaultNote, 'id' | 'updatedAt'>) => {
    const newNote: VaultNote = {
      ...note,
      id: Date.now().toString(),
      updatedAt: '۱۴۰۵/۰۸/۰۵',
    };
    setVaultNotes((prev) => [newNote, ...prev]);
  };

  const handleDeleteVaultNote = (id: string) => {
    setVaultNotes((prev) => prev.filter((n) => n.id !== id));
  };

  // Handlers for Assistant Chat
  const handleSendMessage = (userText: string, aiText: string) => {
    const userMsg: ChatMessage = {
      id: Date.now().toString(),
      sender: 'user',
      text: userText,
      timestamp: new Date().toLocaleTimeString('fa-IR', {
        hour: '2-digit',
        minute: '2-digit',
      }),
    };
    const aiMsg: ChatMessage = {
      id: (Date.now() + 1).toString(),
      sender: 'gemini',
      text: aiText,
      timestamp: new Date().toLocaleTimeString('fa-IR', {
        hour: '2-digit',
        minute: '2-digit',
      }),
    };
    setMessages((prev) => [...prev, userMsg, aiMsg]);
  };

  // Export Data
  const handleExportData = () => {
    const data = {
      tasks,
      habits,
      expenses,
      healthLog,
      vaultNotes,
      bankProfiles,
      notifications,
      notificationSettings,
    };
    const blob = new Blob([JSON.stringify(data, null, 2)], {
      type: 'application/json',
    });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'personal-life-os-backup.json';
    a.click();
  };

  const handleResetData = () => {
    localStorage.clear();
    window.location.reload();
  };

  const unreadNotificationsCount = notifications.filter((n) => !n.read).length;
  const pendingReceiptNotificationsCount = notifications.filter(
    (n) => n.actionState === 'PENDING_USER_ACTION'
  ).length;

  return (
    <div
      className={`min-h-screen bg-slate-950 text-slate-100 flex flex-col lg:flex-row ${
        isRtl ? 'rtl' : 'ltr'
      }`}
      dir={isRtl ? 'rtl' : 'ltr'}
    >
      {/* Floating Heads-up Notification Banner */}
      <FloatingNotificationBanner
        notification={floatingBannerNotif}
        onDismiss={() => setFloatingBannerNotif(null)}
        onOpenCenter={() => {
          setFloatingBannerNotif(null);
          setIsNotificationCenterOpen(true);
        }}
        onQuickApprove={(notif) => {
          if (!notif.analysisResult) return;
          const validated = buildValidatedExpenseEntry(
            notif.analysisResult,
            undefined,
            bankProfiles,
            expenses
          );
          if (validated.ok && validated.entry) {
            handleRegisterExpenseFromNotification(notif.id, {
              ...validated.entry,
              category: notif.suggestedCategory || validated.entry.category,
            });
          } else {
            setFloatingBannerNotif(null);
            setIsNotificationCenterOpen(true);
          }
        }}
      />

      {/* Sidebar Navigation */}
      <Sidebar
        currentTab={currentTab}
        onSelectTab={setCurrentTab}
        isRtl={isRtl}
        onToggleRtl={() => setIsRtl(!isRtl)}
        isOpenMobile={isOpenMobile}
        onToggleMobile={() => setIsOpenMobile(!isOpenMobile)}
        unreadNotificationsCount={unreadNotificationsCount}
        pendingReceiptNotificationsCount={pendingReceiptNotificationsCount}
        onOpenNotificationCenter={() => setIsNotificationCenterOpen(true)}
      />

      {/* Main Content Area */}
      <main className="flex-1 p-4 lg:p-8 overflow-y-auto max-w-7xl mx-auto w-full">
        {currentTab === 'dashboard' && (
          <DashboardScreen
            tasks={tasks}
            habits={habits}
            expenses={expenses}
            healthLog={healthLog}
            onOpenVoiceModal={() => setIsVoiceModalOpen(true)}
            onOpenNotificationCenter={() => setIsNotificationCenterOpen(true)}
            unreadNotificationsCount={unreadNotificationsCount}
            pendingReceiptNotificationsCount={pendingReceiptNotificationsCount}
            onNavigate={setCurrentTab}
            onToggleTask={handleToggleTask}
            onToggleHabit={handleToggleHabit}
          />
        )}

        {currentTab === 'tasks' && (
          <TasksScreen
            tasks={tasks}
            habits={habits}
            onAddTask={handleAddTask}
            onToggleTask={handleToggleTask}
            onDeleteTask={handleDeleteTask}
            onToggleHabit={handleToggleHabit}
            onAddHabit={handleAddHabit}
          />
        )}

        {currentTab === 'expenses' && (
          <ExpensesScreen
            expenses={expenses}
            onAddExpense={handleAddExpense}
            onDeleteExpense={handleDeleteExpense}
            bankProfiles={bankProfiles}
            onUpdateBankProfiles={setBankProfiles}
            onOpenNotificationCenter={() => setIsNotificationCenterOpen(true)}
            pendingNotificationCount={pendingReceiptNotificationsCount}
          />
        )}

        {currentTab === 'health' && (
          <HealthScreen
            healthLog={healthLog}
            onUpdateHealth={handleUpdateHealth}
          />
        )}

        {currentTab === 'vault' && (
          <VaultScreen
            vaultNotes={vaultNotes}
            onAddNote={handleAddVaultNote}
            onDeleteNote={handleDeleteVaultNote}
          />
        )}

        {currentTab === 'assistant' && (
          <AssistantScreen
            messages={messages}
            onSendMessage={handleSendMessage}
          />
        )}

        {currentTab === 'settings' && (
          <SettingsScreen
            onExportData={handleExportData}
            onResetData={handleResetData}
            notificationSettings={notificationSettings}
            onUpdateNotificationSettings={setNotificationSettings}
            onOpenNotificationCenter={() => setIsNotificationCenterOpen(true)}
          />
        )}
      </main>

      {/* Quick Voice / SMS Capture Modal */}
      <VoiceCaptureModal
        isOpen={isVoiceModalOpen}
        onClose={() => setIsVoiceModalOpen(false)}
        bankProfiles={bankProfiles}
        existingExpenses={expenses}
        onAddExpense={handleAddExpense}
        onAddQuickTask={(taskTitle) => {
          handleAddTask({
            title: taskTitle,
            category: 'personal',
            priority: 'medium',
            completed: false,
            dueDate: '۱۴۰۵/۰۸/۱۰',
          });
        }}
      />

      {/* Smart Notification Center & Bank Receipt Logs Modal */}
      <NotificationCenterModal
        isOpen={isNotificationCenterOpen}
        onClose={() => setIsNotificationCenterOpen(false)}
        notifications={notifications}
        settings={notificationSettings}
        bankProfiles={bankProfiles}
        expenses={expenses}
        onMarkAsRead={handleMarkNotificationRead}
        onMarkAllAsRead={handleMarkAllNotificationsRead}
        onClearNotifications={handleClearNotifications}
        onDeleteNotification={handleDeleteNotification}
        onUpdateNotification={handleUpdateNotification}
        onUpdateSettings={setNotificationSettings}
        onRegisterExpenseFromNotification={handleRegisterExpenseFromNotification}
        onUpdateBankProfiles={setBankProfiles}
        onSimulateIncomingNotification={handleSimulateIncomingNotification}
        onNavigateTab={setCurrentTab}
      />
    </div>
  );
}
