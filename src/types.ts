export type TabType = 'dashboard' | 'tasks' | 'expenses' | 'health' | 'vault' | 'assistant' | 'settings';

export type TaskCategory = 'work' | 'personal' | 'finance' | 'health' | 'study';
export type TaskPriority = 'low' | 'medium' | 'high' | 'urgent';

export interface TaskItem {
  id: string;
  title: string;
  category: TaskCategory;
  priority: TaskPriority;
  completed: boolean;
  dueDate: string; // Jalali or ISO string
  createdAt: string;
}

export interface HabitItem {
  id: string;
  title: string;
  streak: number;
  completedToday: boolean;
  category: TaskCategory;
}

export type FinancialEntryType = 'expense' | 'income' | 'transfer';

export interface ExpenseItem {
  id: string;
  title: string;
  amount: number; // in Tomans
  category: string;
  date: string;
  type?: FinancialEntryType;
  isSmsParsed?: boolean;
  bankId?: string;
  bankName?: string;
  trackingCode?: string;
  balanceAfter?: number; // in Tomans
  messageFingerprint?: string;
  templateId?: string;
}

export type SmsParseStatus =
  | 'VALID_TRANSACTION'
  | 'NOT_A_TRANSACTION'
  | 'NEEDS_REVIEW'
  | 'UNSUPPORTED_FORMAT';

export type SmsMessageCategory =
  | 'SUCCESSFUL_TRANSACTION'
  | 'FAILED_TRANSACTION'
  | 'OTP'
  | 'PROMOTIONAL'
  | 'SECURITY_ALERT'
  | 'INFORMATIONAL'
  | 'UNKNOWN';

export type SmsTransactionKind =
  | 'DEPOSIT'
  | 'WITHDRAWAL'
  | 'PURCHASE'
  | 'TRANSFER'
  | 'FEE'
  | 'REFUND'
  | 'FAILED'
  | 'UNKNOWN';

export type CurrencyUnit = 'RIAL' | 'TOMAN' | 'UNKNOWN';

export type BankProfileStatus =
  | 'DRAFT'
  | 'NEEDS_SAMPLES'
  | 'NEEDS_VERIFICATION'
  | 'ACTIVE'
  | 'NEEDS_REVISION';

export type TemplateCurrencySource =
  | 'EXPLICIT_UNIT'
  | 'REFAH_R_RIAL'
  | 'COMPACT_SIGNED_RIAL';

export interface BankSmsTemplateRule {
  id: string;
  version: number;
  name: string;
  expectedKind: SmsTransactionKind;
  directionAnchors: string[];
  amountPatterns: string[];
  currencySource: TemplateCurrencySource;
  balancePatterns: string[];
  trackingPatterns: string[];
  structuralAnchors?: string[];
  isCustom?: boolean;
}

export interface BankSmsSampleRecord {
  id: string;
  rawText: string;
  senderId?: string;
  isSynthetic: boolean;
  expectedKind?: SmsTransactionKind;
  parseStatus: SmsParseStatus;
  messageCategory: SmsMessageCategory;
  detectedKind: SmsTransactionKind;
  extractedAmountOriginal?: number;
  extractedCurrency: CurrencyUnit;
  extractedAmountTomans?: number;
  extractedBalanceTomans?: number;
  extractedTrackingCode?: string;
  matchedTemplateId?: string;
  reason: string;
  userVerified: boolean;
  verifiedAt?: string;
}

export interface BankProfile {
  id: string;
  bankName: string;
  isCustom: boolean;
  status: BankProfileStatus;
  version: number;
  senders: string[];
  requireSenderMatch: boolean;
  templates: BankSmsTemplateRule[];
  samples: BankSmsSampleRecord[];
  updatedAt: string;
}

export interface StructuredSmsAnalysisResult {
  status: SmsParseStatus;
  messageCategory: SmsMessageCategory;
  transactionKind: SmsTransactionKind;
  financialType?: FinancialEntryType;
  bankId?: string;
  bankName?: string;
  senderId?: string;
  senderMatched: boolean;
  matchedTemplateId?: string;
  matchedTemplateName?: string;
  matchedTemplateVersion?: number;
  amountOriginal?: number;
  currency: CurrencyUnit;
  amountInTomans?: number;
  balanceInTomans?: number;
  trackingCode?: string;
  merchant: string;
  reason: string;
  messageFingerprint: string;
  isDuplicate: boolean;
  canAutoRegister: boolean;
  rawNormalized: string;
}

export interface HealthLog {
  id: string;
  date: string;
  sleepHours: number;
  sleepQuality: 'poor' | 'fair' | 'good' | 'excellent';
  waterGlasses: number;
  exerciseMinutes: number;
  exerciseType: string;
  caloriesBurned: number;
}

export interface VaultNote {
  id: string;
  title: string;
  content: string;
  category: string;
  updatedAt: string;
  isEncrypted?: boolean;
}

export interface ChatMessage {
  id: string;
  sender: 'user' | 'gemini';
  text: string;
  timestamp: string;
}
