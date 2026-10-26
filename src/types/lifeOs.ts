// Personal Life OS TypeScript Models matching Android LifeOsModels.kt

export interface SubtaskItem {
  id: string;
  title: string;
  completed: boolean;
}

export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type TaskStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'ARCHIVED';
export type RecurrenceRule = 'NONE' | 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'CUSTOM';

export interface Task {
  id: string;
  userId?: string;
  title: string;
  description: string;
  category: string;
  tags: string[];
  priority: TaskPriority;
  dueDateEpochDay: number; // local epoch day, or -1
  dueTimeMinutes: number; // 0..1439 or -1
  reminderAtMillis?: number;
  estimatedDurationMinutes: number;
  status: TaskStatus;
  subtasks: SubtaskItem[];
  recurrenceRule: RecurrenceRule;
  recurrenceIntervalDays: number;
  notes: string;
  inputSource: 'MANUAL' | 'VOICE' | 'AI_ASSISTANT' | 'RECURRING';
  sortOrder: number;
  completedAtMillis?: number;
  idempotencyKey?: string;
  actualDurationMinutes?: number;
  linkedDebtId?: string; // Links to installment / debt payment
  createdAt: number;
  updatedAt: number;
}

export type TransactionType =
  | 'EXPENSE'
  | 'INCOME'
  | 'TRANSFER'
  | 'DEBT'
  | 'RECEIVABLE'
  | 'DEBT_SETTLED'
  | 'RECEIVABLE_SETTLED';

export interface ExpenseEntry {
  id: string;
  userId?: string;
  title: string;
  category: string; // FOOD, COFFEE, TRANSPORT, SHOPPING, BILLS, HEALTH, OTHER or custom
  amount: number; // In Toman (default) or base currency
  currency: string; // TOMAN, IRR, EUR, USD, etc.
  timestampMillis: number;
  localEpochDay: number;
  linkedConsumptionId?: string;
  notes: string;
  inputSource: 'MANUAL' | 'VOICE' | 'AI_ASSISTANT' | 'SMS_SCANNER';
  idempotencyKey?: string;
  transactionType: TransactionType;
  paidAmount: number; // For partial debt / receivable payment
  dueEpochDay?: number;
  installmentCount?: number;
  linkedDebtId?: string;
  sourceAccount?: string;
  destinationAccount?: string;
  trackingCode?: string;
  createdAt: number;
  updatedAt: number;
}

export interface ConsumptionEntry {
  id: string;
  userId?: string;
  itemId?: string;
  itemName: string;
  category: 'WATER' | 'COFFEE' | 'TEA' | 'BEVERAGE' | 'FOOD' | 'SUPPLEMENT' | 'CUSTOM';
  quantity: number;
  unit: string; // ml, cups, liters, etc.
  timestampMillis: number;
  localEpochDay: number;
  linkedExpenseId?: string;
  linkedTaskId?: string;
  notes: string;
  inputSource: 'MANUAL' | 'QUICK_ADD' | 'VOICE' | 'AI_ASSISTANT';
  idempotencyKey?: string;
  createdAt: number;
  updatedAt: number;
}

export interface Habit {
  id: string;
  userId?: string;
  title: string;
  category: string;
  scheduleType: 'DAILY' | 'WEEKLY_DAYS' | 'TIMES_PER_WEEK';
  scheduleDays: number[]; // 1=Mon .. 7=Sun, or Saturday-aligned 6=Sat..5=Fri
  targetPerPeriod: number;
  linkedTaskCategory?: string;
  reminderTimeMinutes?: number;
  archived: boolean;
  createdAt: number;
  updatedAt: number;
}

export interface HabitRecord {
  id: string;
  userId?: string;
  habitId: string;
  localEpochDay: number;
  completedCount: number;
  notes?: string;
  createdAt: number;
  updatedAt: number;
}

export interface SleepRecord {
  id: string;
  userId?: string;
  sleepStartMillis: number;
  wakeUpMillis: number;
  durationMinutes: number;
  localEpochDay: number;
  qualityRating: number; // 1..5
  notes?: string;
  inputSource: 'MANUAL' | 'VOICE' | 'AI_ASSISTANT';
  createdAt: number;
  updatedAt: number;
}

export interface ExerciseRecord {
  id: string;
  userId?: string;
  activityType: string; // پیاده‌روی, دویدن, بدنسازی, شنا, دوچرخه
  durationMinutes: number;
  distanceMeters: number;
  intensity: 'LOW' | 'MODERATE' | 'HIGH';
  repetitions?: number;
  timestampMillis: number;
  localEpochDay: number;
  notes?: string;
  inputSource: 'MANUAL' | 'VOICE' | 'AI_ASSISTANT';
  createdAt: number;
  updatedAt: number;
}

export interface ConversationTurn {
  id: string;
  userId?: string;
  role: 'USER' | 'ASSISTANT';
  messageText: string;
  structuredActionJson?: string;
  timestampMillis: number;
  suggestedAction?: {
    type: 'ADD_EXPENSE' | 'ADD_TASK' | 'ADD_WATER' | 'SETTLE_DEBT';
    data: any;
    requiresConfirmation: boolean;
  };
}

export interface UserPreferencesDoc {
  id: string;
  userId?: string;
  language: 'fa' | 'en';
  darkTheme: boolean;
  weekStartDay: number; // 6 = Saturday (Persian standard)
  calendarDisplay: 'JALALI' | 'GREGORIAN';
  defaultCurrency: 'TOMAN' | 'IRR' | 'USD' | 'EUR';
  dailyWaterTargetMl: number;
  dailySleepTargetMinutes: number;
  dailyExerciseTargetMinutes: number;
  retainVoiceTranscripts: boolean;
  retainConversations: boolean;
  notificationsEnabled: boolean;
  onboardingCompleted: boolean;
  monthlyBudgetTarget: number;
  vaultLockEnabled: boolean;
  vaultPinCode: string;
  themePalette: 'OBSIDIAN_TEAL' | 'EMERALD_MATRIX' | 'ROYAL_AMETHYST' | 'AMBER_CYBER' | 'NORDIC_FROST';
  layoutStyle: 'BENTO_GRID' | 'COMPACT_PRO' | 'MINIMAL_ZEN' | 'KANBAN_BOARD';
  dashboardCardOrder: string[];
  customExpenseCategories: string[];
  financialAccounts: string[];
}

export type AppSection = 'DAILY_LOG' | 'GOALS_AND_HABITS' | 'HOME' | 'AI_ASSISTANT';

export interface BankSmsParseResult {
  status: 'SUCCESS' | 'NEEDS_REVIEW' | 'IGNORED_OTP' | 'IGNORED_NON_FINANCIAL' | 'UNRECOGNIZED';
  bankName: string;
  bankCode: string;
  transactionType: 'INCOME' | 'EXPENSE' | 'TRANSFER' | 'FEE' | 'REFUND' | 'UNKNOWN';
  rawAmountDetected: number;
  wasInRial: boolean;
  amountInToman: number;
  balanceInToman?: number;
  cardOrAccountMask?: string;
  trackingCode?: string;
  suggestedTitle: string;
  suggestedCategory: string;
  timestampMillis: number;
  confidence: number;
  recognitionReasons: string[];
  fingerprint: string;
}

export interface TransactionIntent {
  type: 'INCOME' | 'EXPENSE' | 'TRANSFER' | 'RECEIVABLE' | 'DEBT' | 'UNKNOWN';
  amount: number;
  currency: string;
  category: string;
  account?: string;
  person?: string;
  title: string;
  isAmbiguous: boolean;
  confidence: number;
  reasoning: string;
}
