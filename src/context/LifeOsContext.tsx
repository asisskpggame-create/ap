import React, { createContext, useContext, useState, useEffect } from 'react';
import {
  Task,
  ExpenseEntry,
  ConsumptionEntry,
  Habit,
  HabitRecord,
  SleepRecord,
  ExerciseRecord,
  UserPreferencesDoc,
  ConversationTurn,
  AppSection
} from '../types/lifeOs';
import { currentLocalEpochDay } from '../utils/persianDateUtils';

interface LifeOsContextType {
  activeSection: AppSection;
  setActiveSection: (sec: AppSection) => void;
  tasks: Task[];
  addTask: (task: Omit<Task, 'id' | 'createdAt' | 'updatedAt'>) => void;
  updateTask: (task: Task) => void;
  deleteTask: (id: string) => void;
  toggleTaskCompleted: (id: string) => void;
  toggleSubtask: (taskId: string, subtaskId: string) => void;

  expenses: ExpenseEntry[];
  addExpense: (expense: Omit<ExpenseEntry, 'id' | 'createdAt' | 'updatedAt'>) => void;
  updateExpense: (expense: ExpenseEntry) => void;
  deleteExpense: (id: string) => void;
  recordDebtRepayment: (debtId: string, paymentAmount: number) => void;

  consumption: ConsumptionEntry[];
  addConsumption: (entry: Omit<ConsumptionEntry, 'id' | 'createdAt' | 'updatedAt'>) => void;

  habits: Habit[];
  habitRecords: HabitRecord[];
  addHabit: (habit: Omit<Habit, 'id' | 'createdAt' | 'updatedAt'>) => void;
  toggleHabitToday: (habitId: string) => void;
  deleteHabit: (id: string) => void;

  sleepRecords: SleepRecord[];
  addSleepRecord: (record: Omit<SleepRecord, 'id' | 'createdAt' | 'updatedAt'>) => void;

  exerciseRecords: ExerciseRecord[];
  addExerciseRecord: (record: Omit<ExerciseRecord, 'id' | 'createdAt' | 'updatedAt'>) => void;

  preferences: UserPreferencesDoc;
  updatePreferences: (pref: Partial<UserPreferencesDoc>) => void;

  chatMessages: ConversationTurn[];
  addChatMessage: (turn: Omit<ConversationTurn, 'id' | 'timestampMillis'>) => void;
  clearChat: () => void;

  isVaultLocked: boolean;
  unlockVault: (pin: string) => boolean;
  lockVault: () => void;

  exportDataJson: () => string;
  importDataJson: (jsonStr: string) => boolean;
}

const defaultPreferences: UserPreferencesDoc = {
  id: 'default',
  language: 'fa',
  darkTheme: true,
  weekStartDay: 6, // شنبه
  calendarDisplay: 'JALALI',
  defaultCurrency: 'TOMAN',
  dailyWaterTargetMl: 2500,
  dailySleepTargetMinutes: 480,
  dailyExerciseTargetMinutes: 30,
  retainVoiceTranscripts: true,
  retainConversations: true,
  notificationsEnabled: true,
  onboardingCompleted: true,
  monthlyBudgetTarget: 15000000,
  vaultLockEnabled: false,
  vaultPinCode: '',
  themePalette: 'OBSIDIAN_TEAL',
  layoutStyle: 'BENTO_GRID',
  dashboardCardOrder: ['QUICK_BAR', 'FINANCE', 'GOALS', 'HABITS', 'WELLNESS', 'CALENDAR'],
  customExpenseCategories: [
    'غذا و کافه', 'خرید روزمره', 'حمل‌ونقل', 'قبوض و اجاره', 'سلامت و درمان',
    'سرمایه‌گذاری', 'تفریح و سرگرمی', 'آموزش', 'سایر'
  ],
  financialAccounts: [
    'کارت اصلی (بانک ملت)',
    'کارت پس‌انداز (بلوبانک)',
    'کیف پول / نقدی'
  ]
};

const initialTasksSeed: Task[] = [
  {
    id: 'task_1',
    title: 'بررسی اقساط و تسویه قسط ماهانه وام',
    description: 'قسط شماره ۴ وام قرض‌الحسنه رسالت با مبلغ ۲,۵۰۰,۰۰۰ تومان',
    category: 'مالی',
    tags: ['مالی', 'بانک', 'اقساط'],
    priority: 'HIGH',
    dueDateEpochDay: currentLocalEpochDay() + 2,
    dueTimeMinutes: 660, // 11:00 AM
    estimatedDurationMinutes: 15,
    status: 'PENDING',
    subtasks: [
      { id: 'st_1', title: 'بررسی موجودی کارت ملت', completed: true },
      { id: 'st_2', title: 'واریز آنلاین به حساب وام', completed: false },
      { id: 'st_3', title: 'دریافت و ذخیره کد رهگیری', completed: false }
    ],
    recurrenceRule: 'MONTHLY',
    recurrenceIntervalDays: 30,
    notes: 'متصل به بدهی وام',
    inputSource: 'MANUAL',
    sortOrder: 1,
    linkedDebtId: 'exp_debt_1',
    createdAt: Date.now() - 86400000 * 2,
    updatedAt: Date.now()
  },
  {
    id: 'task_2',
    title: 'پیاده‌سازی ماژول‌های نهایی Personal Life OS',
    description: 'تکمیل تمامی قابلیت‌های تبدیل‌شده از اندروید به ری‌اکت با معماری شیشه‌ای و موتور پیامک بانکی',
    category: 'کار و پروژه',
    tags: ['برنامه‌نویسی', 'ری‌اکت', 'هوش مصنوعی'],
    priority: 'CRITICAL',
    dueDateEpochDay: currentLocalEpochDay(),
    dueTimeMinutes: 1020, // 17:00
    estimatedDurationMinutes: 120,
    status: 'IN_PROGRESS',
    subtasks: [
      { id: 'st_4', title: 'موتور پارسر پیامک بانک‌های ایران', completed: true },
      { id: 'st_5', title: 'موتور تشخیص قصد محاوره‌ای فارسی', completed: true },
      { id: 'st_6', title: 'داشبورد بنتو با Safe-to-Spend و تقویم جلالی', completed: true }
    ],
    recurrenceRule: 'NONE',
    recurrenceIntervalDays: 0,
    notes: 'پروژه کلیدی',
    inputSource: 'MANUAL',
    sortOrder: 2,
    createdAt: Date.now() - 86400000,
    updatedAt: Date.now()
  },
  {
    id: 'task_3',
    title: 'تمرین پیاده‌روی هوازی عصرگاهی',
    description: 'حداقل ۳۵ دقیقه پیاده‌روی سریع در پارک برای تنظیم سلامت قلب',
    category: 'سلامت',
    tags: ['سلامت', 'ورزش'],
    priority: 'MEDIUM',
    dueDateEpochDay: currentLocalEpochDay(),
    dueTimeMinutes: 1140, // 19:00
    estimatedDurationMinutes: 40,
    status: 'PENDING',
    subtasks: [],
    recurrenceRule: 'DAILY',
    recurrenceIntervalDays: 1,
    notes: 'همراه با پادکست',
    inputSource: 'MANUAL',
    sortOrder: 3,
    createdAt: Date.now() - 86400000,
    updatedAt: Date.now()
  }
];

const initialExpensesSeed: ExpenseEntry[] = [
  {
    id: 'exp_1',
    title: 'واریز حقوق ماهانه',
    category: 'حقوق و دستمزد',
    amount: 28500000,
    currency: 'TOMAN',
    timestampMillis: Date.now() - 86400000 * 4,
    localEpochDay: currentLocalEpochDay() - 4,
    notes: '[INCOME] واریز حقوق از شرکت',
    inputSource: 'MANUAL',
    transactionType: 'INCOME',
    paidAmount: 28500000,
    createdAt: Date.now() - 86400000 * 4,
    updatedAt: Date.now() - 86400000 * 4
  },
  {
    id: 'exp_2',
    title: 'خرید هفتگی مواد غذایی و سوپرمارکت',
    category: 'خرید روزمره',
    amount: 1450000,
    currency: 'TOMAN',
    timestampMillis: Date.now() - 86400000 * 2,
    localEpochDay: currentLocalEpochDay() - 2,
    notes: 'میوه و اقلام اساسی',
    inputSource: 'MANUAL',
    transactionType: 'EXPENSE',
    paidAmount: 1450000,
    createdAt: Date.now() - 86400000 * 2,
    updatedAt: Date.now() - 86400000 * 2
  },
  {
    id: 'exp_3',
    title: 'کرایه اسنپ به دفتر کار',
    category: 'حمل‌ونقل',
    amount: 95000,
    currency: 'TOMAN',
    timestampMillis: Date.now() - 86400000,
    localEpochDay: currentLocalEpochDay() - 1,
    notes: 'رفت‌وبرگشت',
    inputSource: 'VOICE',
    transactionType: 'EXPENSE',
    paidAmount: 95000,
    createdAt: Date.now() - 86400000,
    updatedAt: Date.now() - 86400000
  },
  {
    id: 'exp_4',
    title: 'کافه و قهوه عصر',
    category: 'غذا و کافه',
    amount: 140000,
    currency: 'TOMAN',
    timestampMillis: Date.now() - 3600000 * 3,
    localEpochDay: currentLocalEpochDay(),
    notes: 'امضای قرارداد',
    inputSource: 'MANUAL',
    transactionType: 'EXPENSE',
    paidAmount: 140000,
    createdAt: Date.now() - 3600000 * 3,
    updatedAt: Date.now() - 3600000 * 3
  },
  {
    id: 'exp_transfer_1',
    title: 'انتقال از کارت اصلی به پس‌انداز',
    category: 'انتقال بین حسابی',
    amount: 4000000,
    currency: 'TOMAN',
    timestampMillis: Date.now() - 86400000 * 3,
    localEpochDay: currentLocalEpochDay() - 3,
    notes: '[TRANSFER] ذخیره در حساب سودآور بلو',
    inputSource: 'MANUAL',
    transactionType: 'TRANSFER',
    sourceAccount: 'کارت اصلی (بانک ملت)',
    destinationAccount: 'کارت پس‌انداز (بلوبانک)',
    paidAmount: 4000000,
    createdAt: Date.now() - 86400000 * 3,
    updatedAt: Date.now() - 86400000 * 3
  },
  {
    id: 'exp_debt_1',
    title: 'وام قرض‌الحسنه رسالت',
    category: 'بدهی و اقساط',
    amount: 15000000,
    currency: 'TOMAN',
    timestampMillis: Date.now() - 86400000 * 15,
    localEpochDay: currentLocalEpochDay() - 15,
    notes: '[DEBT] وام ۱۰ ماهه، هر ماه ۲.۵ میلیون',
    inputSource: 'MANUAL',
    transactionType: 'DEBT',
    paidAmount: 5000000,
    installmentCount: 6,
    dueEpochDay: currentLocalEpochDay() + 2,
    createdAt: Date.now() - 86400000 * 15,
    updatedAt: Date.now() - 86400000 * 15
  },
  {
    id: 'exp_rec_1',
    title: 'طلب از همکار (رضا)',
    category: 'طلب و وام',
    amount: 3200000,
    currency: 'TOMAN',
    timestampMillis: Date.now() - 86400000 * 8,
    localEpochDay: currentLocalEpochDay() - 8,
    notes: '[RECEIVABLE] خرید اشتراکی لپتاپ و لوازم جانبی',
    inputSource: 'MANUAL',
    transactionType: 'RECEIVABLE',
    paidAmount: 1200000,
    dueEpochDay: currentLocalEpochDay() + 10,
    createdAt: Date.now() - 86400000 * 8,
    updatedAt: Date.now() - 86400000 * 8
  }
];

const initialHabitsSeed: Habit[] = [
  {
    id: 'habit_1',
    title: 'نوشیدن آب مداوم (حداقل ۲۵۰۰ میلی‌لیتر)',
    category: 'سلامت',
    scheduleType: 'DAILY',
    scheduleDays: [1, 2, 3, 4, 5, 6, 7],
    targetPerPeriod: 8,
    reminderTimeMinutes: 600,
    archived: false,
    createdAt: Date.now() - 86400000 * 10,
    updatedAt: Date.now()
  },
  {
    id: 'habit_2',
    title: 'مطالعه و یادگیری روزانه (۲۰ صفحه)',
    category: 'رشد فردی',
    scheduleType: 'DAILY',
    scheduleDays: [1, 2, 3, 4, 5, 6, 7],
    targetPerPeriod: 1,
    reminderTimeMinutes: 1320,
    archived: false,
    createdAt: Date.now() - 86400000 * 10,
    updatedAt: Date.now()
  },
  {
    id: 'habit_3',
    title: 'ورزش و حرکات کششی صبحگاهی',
    category: 'ورزش',
    scheduleType: 'DAILY',
    scheduleDays: [1, 2, 3, 4, 5, 6, 7],
    targetPerPeriod: 1,
    reminderTimeMinutes: 450,
    archived: false,
    createdAt: Date.now() - 86400000 * 10,
    updatedAt: Date.now()
  },
  {
    id: 'habit_4',
    title: 'ثبت دقیق تراکنش‌های مالی قبل از خواب',
    category: 'مالی',
    scheduleType: 'DAILY',
    scheduleDays: [1, 2, 3, 4, 5, 6, 7],
    targetPerPeriod: 1,
    reminderTimeMinutes: 1380,
    archived: false,
    createdAt: Date.now() - 86400000 * 10,
    updatedAt: Date.now()
  }
];

const initialHabitRecordsSeed: HabitRecord[] = [
  {
    id: 'hr_1',
    habitId: 'habit_1',
    localEpochDay: currentLocalEpochDay() - 1,
    completedCount: 8,
    createdAt: Date.now() - 86400000,
    updatedAt: Date.now() - 86400000
  },
  {
    id: 'hr_2',
    habitId: 'habit_2',
    localEpochDay: currentLocalEpochDay() - 1,
    completedCount: 1,
    createdAt: Date.now() - 86400000,
    updatedAt: Date.now() - 86400000
  },
  {
    id: 'hr_3',
    habitId: 'habit_3',
    localEpochDay: currentLocalEpochDay() - 1,
    completedCount: 1,
    createdAt: Date.now() - 86400000,
    updatedAt: Date.now() - 86400000
  },
  {
    id: 'hr_4',
    habitId: 'habit_1',
    localEpochDay: currentLocalEpochDay(),
    completedCount: 5,
    createdAt: Date.now(),
    updatedAt: Date.now()
  },
  {
    id: 'hr_5',
    habitId: 'habit_2',
    localEpochDay: currentLocalEpochDay(),
    completedCount: 1,
    createdAt: Date.now(),
    updatedAt: Date.now()
  }
];

const initialConsumptionSeed: ConsumptionEntry[] = [
  {
    id: 'cons_1',
    itemName: 'آب آشامیدنی',
    category: 'WATER',
    quantity: 500,
    unit: 'ml',
    timestampMillis: Date.now() - 3600000 * 5,
    localEpochDay: currentLocalEpochDay(),
    notes: 'آب صبحگاهی',
    inputSource: 'QUICK_ADD',
    createdAt: Date.now() - 3600000 * 5,
    updatedAt: Date.now() - 3600000 * 5
  },
  {
    id: 'cons_2',
    itemName: 'آب آشامیدنی',
    category: 'WATER',
    quantity: 350,
    unit: 'ml',
    timestampMillis: Date.now() - 3600000 * 2,
    localEpochDay: currentLocalEpochDay(),
    notes: 'حین کار',
    inputSource: 'QUICK_ADD',
    createdAt: Date.now() - 3600000 * 2,
    updatedAt: Date.now() - 3600000 * 2
  },
  {
    id: 'cons_3',
    itemName: 'قهوه اسپرسو',
    category: 'COFFEE',
    quantity: 1,
    unit: 'cups',
    timestampMillis: Date.now() - 3600000 * 3,
    localEpochDay: currentLocalEpochDay(),
    notes: 'تک شات',
    inputSource: 'MANUAL',
    createdAt: Date.now() - 3600000 * 3,
    updatedAt: Date.now() - 3600000 * 3
  }
];

const initialSleepSeed: SleepRecord[] = [
  {
    id: 'slp_1',
    sleepStartMillis: Date.now() - 3600000 * 16,
    wakeUpMillis: Date.now() - 3600000 * 8,
    durationMinutes: 460, // 7h 40m
    localEpochDay: currentLocalEpochDay(),
    qualityRating: 5,
    notes: 'خواب عمیق و باکیفیت',
    inputSource: 'MANUAL',
    createdAt: Date.now() - 3600000 * 8,
    updatedAt: Date.now() - 3600000 * 8
  }
];

const initialExerciseSeed: ExerciseRecord[] = [
  {
    id: 'ex_1',
    activityType: 'پیاده‌روی تند',
    durationMinutes: 35,
    distanceMeters: 3100,
    intensity: 'MODERATE',
    timestampMillis: Date.now() - 3600000 * 4,
    localEpochDay: currentLocalEpochDay(),
    notes: 'مسیر پارک محله',
    inputSource: 'MANUAL',
    createdAt: Date.now() - 3600000 * 4,
    updatedAt: Date.now() - 3600000 * 4
  }
];

const initialChatSeed: ConversationTurn[] = [
  {
    id: 'chat_welcome',
    role: 'ASSISTANT',
    messageText: 'سلام! من دستیار هوشمند Personal Life OS هستم. می‌توانید هر فعالیتی مانند «۲۵۰ هزار تومن اسنپ دادم»، «از علی ۱ میلیون قرض گرفتم» یا «تسک خرید قهوه را برای فردا ثبت کن» را به صورت متنی یا صوتی بیان کنید.',
    timestampMillis: Date.now() - 3600000
  }
];

const LifeOsContext = createContext<LifeOsContextType | undefined>(undefined);

export const LifeOsProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [activeSection, setActiveSection] = useState<AppSection>('HOME');

  const [tasks, setTasks] = useState<Task[]>(() => {
    const saved = localStorage.getItem('lifeos_tasks');
    return saved ? JSON.parse(saved) : initialTasksSeed;
  });

  const [expenses, setExpenses] = useState<ExpenseEntry[]>(() => {
    const saved = localStorage.getItem('lifeos_expenses');
    return saved ? JSON.parse(saved) : initialExpensesSeed;
  });

  const [consumption, setConsumption] = useState<ConsumptionEntry[]>(() => {
    const saved = localStorage.getItem('lifeos_consumption');
    return saved ? JSON.parse(saved) : initialConsumptionSeed;
  });

  const [habits, setHabits] = useState<Habit[]>(() => {
    const saved = localStorage.getItem('lifeos_habits');
    return saved ? JSON.parse(saved) : initialHabitsSeed;
  });

  const [habitRecords, setHabitRecords] = useState<HabitRecord[]>(() => {
    const saved = localStorage.getItem('lifeos_habit_records');
    return saved ? JSON.parse(saved) : initialHabitRecordsSeed;
  });

  const [sleepRecords, setSleepRecords] = useState<SleepRecord[]>(() => {
    const saved = localStorage.getItem('lifeos_sleep');
    return saved ? JSON.parse(saved) : initialSleepSeed;
  });

  const [exerciseRecords, setExerciseRecords] = useState<ExerciseRecord[]>(() => {
    const saved = localStorage.getItem('lifeos_exercise');
    return saved ? JSON.parse(saved) : initialExerciseSeed;
  });

  const [preferences, setPreferences] = useState<UserPreferencesDoc>(() => {
    const saved = localStorage.getItem('lifeos_preferences');
    return saved ? JSON.parse(saved) : defaultPreferences;
  });

  const [chatMessages, setChatMessages] = useState<ConversationTurn[]>(() => {
    const saved = localStorage.getItem('lifeos_chat');
    return saved ? JSON.parse(saved) : initialChatSeed;
  });

  const [isVaultLocked, setIsVaultLocked] = useState<boolean>(() => {
    return preferences.vaultLockEnabled && !!preferences.vaultPinCode;
  });

  // Sync to localStorage
  useEffect(() => {
    localStorage.setItem('lifeos_tasks', JSON.stringify(tasks));
  }, [tasks]);

  useEffect(() => {
    localStorage.setItem('lifeos_expenses', JSON.stringify(expenses));
  }, [expenses]);

  useEffect(() => {
    localStorage.setItem('lifeos_consumption', JSON.stringify(consumption));
  }, [consumption]);

  useEffect(() => {
    localStorage.setItem('lifeos_habits', JSON.stringify(habits));
  }, [habits]);

  useEffect(() => {
    localStorage.setItem('lifeos_habit_records', JSON.stringify(habitRecords));
  }, [habitRecords]);

  useEffect(() => {
    localStorage.setItem('lifeos_sleep', JSON.stringify(sleepRecords));
  }, [sleepRecords]);

  useEffect(() => {
    localStorage.setItem('lifeos_exercise', JSON.stringify(exerciseRecords));
  }, [exerciseRecords]);

  useEffect(() => {
    localStorage.setItem('lifeos_preferences', JSON.stringify(preferences));
  }, [preferences]);

  useEffect(() => {
    localStorage.setItem('lifeos_chat', JSON.stringify(chatMessages));
  }, [chatMessages]);

  // Tasks operations
  const addTask = (newTask: Omit<Task, 'id' | 'createdAt' | 'updatedAt'>) => {
    const task: Task = {
      ...newTask,
      id: `task_${Date.now()}_${Math.random().toString(36).substr(2, 4)}`,
      createdAt: Date.now(),
      updatedAt: Date.now()
    };
    setTasks(prev => [task, ...prev]);
  };

  const updateTask = (updated: Task) => {
    setTasks(prev => prev.map(t => (t.id === updated.id ? { ...updated, updatedAt: Date.now() } : t)));
  };

  const deleteTask = (id: string) => {
    setTasks(prev => prev.filter(t => t.id !== id));
  };

  const toggleTaskCompleted = (id: string) => {
    setTasks(prev =>
      prev.map(t => {
        if (t.id === id) {
          const nextStatus = t.status === 'COMPLETED' ? 'PENDING' : 'COMPLETED';
          return {
            ...t,
            status: nextStatus,
            completedAtMillis: nextStatus === 'COMPLETED' ? Date.now() : undefined,
            updatedAt: Date.now()
          };
        }
        return t;
      })
    );
  };

  const toggleSubtask = (taskId: string, subtaskId: string) => {
    setTasks(prev =>
      prev.map(t => {
        if (t.id === taskId) {
          const updatedSubtasks = t.subtasks.map(st =>
            st.id === subtaskId ? { ...st, completed: !st.completed } : st
          );
          return { ...t, subtasks: updatedSubtasks, updatedAt: Date.now() };
        }
        return t;
      })
    );
  };

  // Expenses operations
  const addExpense = (newExpense: Omit<ExpenseEntry, 'id' | 'createdAt' | 'updatedAt'>) => {
    const exp: ExpenseEntry = {
      ...newExpense,
      id: `exp_${Date.now()}_${Math.random().toString(36).substr(2, 4)}`,
      createdAt: Date.now(),
      updatedAt: Date.now()
    };
    setExpenses(prev => [exp, ...prev]);
  };

  const updateExpense = (updated: ExpenseEntry) => {
    setExpenses(prev => prev.map(e => (e.id === updated.id ? { ...updated, updatedAt: Date.now() } : e)));
  };

  const deleteExpense = (id: string) => {
    setExpenses(prev => prev.filter(e => e.id !== id));
  };

  const recordDebtRepayment = (debtId: string, paymentAmount: number) => {
    setExpenses(prev =>
      prev.map(e => {
        if (e.id === debtId) {
          const newPaid = (e.paidAmount || 0) + paymentAmount;
          const isSettled = newPaid >= e.amount - 1;
          const newTxType = isSettled
            ? e.transactionType === 'DEBT'
              ? 'DEBT_SETTLED'
              : 'RECEIVABLE_SETTLED'
            : e.transactionType;
          return {
            ...e,
            paidAmount: newPaid,
            transactionType: newTxType,
            notes: isSettled && !e.notes.includes('[SETTLED]') ? `${e.notes} [SETTLED]` : e.notes,
            updatedAt: Date.now()
          };
        }
        return e;
      })
    );
  };

  // Consumption operations
  const addConsumption = (entry: Omit<ConsumptionEntry, 'id' | 'createdAt' | 'updatedAt'>) => {
    const item: ConsumptionEntry = {
      ...entry,
      id: `cons_${Date.now()}_${Math.random().toString(36).substr(2, 4)}`,
      createdAt: Date.now(),
      updatedAt: Date.now()
    };
    setConsumption(prev => [item, ...prev]);
  };

  // Habits operations
  const addHabit = (newHabit: Omit<Habit, 'id' | 'createdAt' | 'updatedAt'>) => {
    const habit: Habit = {
      ...newHabit,
      id: `habit_${Date.now()}_${Math.random().toString(36).substr(2, 4)}`,
      createdAt: Date.now(),
      updatedAt: Date.now()
    };
    setHabits(prev => [...prev, habit]);
  };

  const toggleHabitToday = (habitId: string) => {
    const today = currentLocalEpochDay();
    const existing = habitRecords.find(r => r.habitId === habitId && r.localEpochDay === today);

    if (existing) {
      // Remove or decrement
      setHabitRecords(prev => prev.filter(r => r.id !== existing.id));
    } else {
      // Add completion
      const newRec: HabitRecord = {
        id: `hr_${Date.now()}_${Math.random().toString(36).substr(2, 4)}`,
        habitId,
        localEpochDay: today,
        completedCount: 1,
        createdAt: Date.now(),
        updatedAt: Date.now()
      };
      setHabitRecords(prev => [...prev, newRec]);
    }
  };

  const deleteHabit = (id: string) => {
    setHabits(prev => prev.filter(h => h.id !== id));
    setHabitRecords(prev => prev.filter(r => r.habitId !== id));
  };

  // Sleep & Exercise
  const addSleepRecord = (record: Omit<SleepRecord, 'id' | 'createdAt' | 'updatedAt'>) => {
    const slp: SleepRecord = {
      ...record,
      id: `slp_${Date.now()}`,
      createdAt: Date.now(),
      updatedAt: Date.now()
    };
    setSleepRecords(prev => [slp, ...prev]);
  };

  const addExerciseRecord = (record: Omit<ExerciseRecord, 'id' | 'createdAt' | 'updatedAt'>) => {
    const ex: ExerciseRecord = {
      ...record,
      id: `ex_${Date.now()}`,
      createdAt: Date.now(),
      updatedAt: Date.now()
    };
    setExerciseRecords(prev => [ex, ...prev]);
  };

  // Preferences
  const updatePreferences = (pref: Partial<UserPreferencesDoc>) => {
    setPreferences(prev => ({ ...prev, ...pref }));
  };

  // Chat
  const addChatMessage = (turn: Omit<ConversationTurn, 'id' | 'timestampMillis'>) => {
    const newTurn: ConversationTurn = {
      ...turn,
      id: `msg_${Date.now()}_${Math.random().toString(36).substr(2, 4)}`,
      timestampMillis: Date.now()
    };
    setChatMessages(prev => [...prev, newTurn]);
  };

  const clearChat = () => {
    setChatMessages([initialChatSeed[0]]);
  };

  // Vault Lock
  const unlockVault = (pin: string): boolean => {
    if (pin === preferences.vaultPinCode || !preferences.vaultLockEnabled) {
      setIsVaultLocked(false);
      return true;
    }
    return false;
  };

  const lockVault = () => {
    if (preferences.vaultLockEnabled && preferences.vaultPinCode) {
      setIsVaultLocked(true);
    }
  };

  // Export & Import
  const exportDataJson = (): string => {
    const payload = {
      version: '1.5.0',
      exportedAt: new Date().toISOString(),
      tasks,
      expenses,
      consumption,
      habits,
      habitRecords,
      sleepRecords,
      exerciseRecords,
      preferences
    };
    return JSON.stringify(payload, null, 2);
  };

  const importDataJson = (jsonStr: string): boolean => {
    try {
      const data = JSON.parse(jsonStr);
      if (data.tasks) setTasks(data.tasks);
      if (data.expenses) setExpenses(data.expenses);
      if (data.consumption) setConsumption(data.consumption);
      if (data.habits) setHabits(data.habits);
      if (data.habitRecords) setHabitRecords(data.habitRecords);
      if (data.sleepRecords) setSleepRecords(data.sleepRecords);
      if (data.exerciseRecords) setExerciseRecords(data.exerciseRecords);
      if (data.preferences) setPreferences(data.preferences);
      return true;
    } catch (e) {
      console.error('Failed to import backup JSON', e);
      return false;
    }
  };

  return (
    <LifeOsContext.Provider
      value={{
        activeSection,
        setActiveSection,
        tasks,
        addTask,
        updateTask,
        deleteTask,
        toggleTaskCompleted,
        toggleSubtask,
        expenses,
        addExpense,
        updateExpense,
        deleteExpense,
        recordDebtRepayment,
        consumption,
        addConsumption,
        habits,
        habitRecords,
        addHabit,
        toggleHabitToday,
        deleteHabit,
        sleepRecords,
        addSleepRecord,
        exerciseRecords,
        addExerciseRecord,
        preferences,
        updatePreferences,
        chatMessages,
        addChatMessage,
        clearChat,
        isVaultLocked,
        unlockVault,
        lockVault,
        exportDataJson,
        importDataJson
      }}
    >
      {children}
    </LifeOsContext.Provider>
  );
};

export const useLifeOs = () => {
  const context = useContext(LifeOsContext);
  if (!context) {
    throw new Error('useLifeOs must be used within a LifeOsProvider');
  }
  return context;
};
