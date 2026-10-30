import type {
  BankProfile,
  BankProfileStatus,
  BankSmsSampleRecord,
  BankSmsTemplateRule,
  CurrencyUnit,
  ExpenseItem,
  FinancialEntryType,
  SmsMessageCategory,
  SmsParseStatus,
  SmsTransactionKind,
  StructuredSmsAnalysisResult,
} from '../types';

// ============================================================================
// 1. Formatting & Controlled Persian/Arabic Normalization
// ============================================================================

export function formatToman(amount: number): string {
  const safeAmount = Number.isFinite(amount) ? amount : 0;
  try {
    return new Intl.NumberFormat('fa-IR').format(safeAmount) + ' تومان';
  } catch {
    return safeAmount.toLocaleString() + ' تومان';
  }
}

export function formatPersianNumber(num: number): string {
  const persianDigits = ['۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹'];
  return String(num).replace(/\d/g, (x) => persianDigits[parseInt(x, 10)]);
}

/**
 * Controlled normalization that converts Persian & Arabic digits to Latin,
 * unifies Arabic/Persian letter variants (ک/ی/ه), and normalizes spaces
 * while preserving line breaks (\n) for structural line-by-line parsing.
 */
export function normalizeSmsPreserveLines(input: string): string {
  if (!input) return '';
  const persianDigits = '۰۱۲۳۴۵۶۷۸۹';
  const arabicDigits = '٠١٢٣٤٥٦٧٨٩';

  let out = input
    .replace(/\r\n/g, '\n')
    .replace(/\r/g, '\n')
    .replace(/ك/g, 'ک')
    .replace(/[يیى]/g, 'ی')
    .replace(/ة/g, 'ه')
    .replace(/ؤ/g, 'و')
    .replace(/[إأآ]/g, (m) => (m === 'آ' ? 'آ' : 'ا'))
    .replace(/\u200c/g, ' ') // convert ZWNJ to space for consistent token matching
    .replace(/[\u066C\u060C]/g, ',') // normalize Arabic thousands/comma separators to ','
    .replace(/٬/g, ',');

  for (let i = 0; i < 10; i++) {
    out = out.replace(new RegExp(persianDigits[i], 'g'), String(i));
    out = out.replace(new RegExp(arabicDigits[i], 'g'), String(i));
  }

  return out
    .split('\n')
    .map((line) => line.replace(/[ \t]+/g, ' ').trim())
    .filter((line) => line.length > 0)
    .join('\n');
}

export function normalizePersianText(input: string): string {
  return normalizeSmsPreserveLines(input).replace(/\n/g, ' ').trim().toLowerCase();
}

/**
 * Masks sensitive 16-digit card numbers or long account numbers for safe UI/log display.
 */
export function maskSensitiveBankData(input: string): string {
  if (!input) return '';
  const norm = normalizeSmsPreserveLines(input);
  // Mask 16-digit card numbers (contiguous or hyphen/space separated)
  return norm.replace(/\b(\d{6})[\s-]?(\d{4})[\s-]?(\d{2})(\d{4})\b/g, '$1******$4');
}

/**
 * Deterministic fingerprint for duplicate detection.
 * Incorporates normalized lines so two distinct SMS messages with the same amount
 * (but different tracking code, balance, or time) have distinct fingerprints.
 */
export function computeSmsFingerprint(rawText: string, senderId?: string): string {
  const norm = normalizeSmsPreserveLines(rawText);
  const senderNorm = (senderId || '').trim().toLowerCase();
  const payload = `${senderNorm}|${norm}`;
  let h1 = 0xdeadbeef ^ payload.length;
  let h2 = 0x41c6ce57 ^ payload.length;
  for (let i = 0; i < payload.length; i++) {
    const ch = payload.charCodeAt(i);
    h1 = Math.imul(h1 ^ ch, 2654435761);
    h2 = Math.imul(h2 ^ ch, 1597334677);
  }
  h1 = Math.imul(h1 ^ (h1 >>> 16), 2246822507) ^ Math.imul(h2 ^ (h2 >>> 13), 3266489909);
  h2 = Math.imul(h2 ^ (h2 >>> 16), 2246822507) ^ Math.imul(h1 ^ (h1 >>> 13), 3266489909);
  return 'sms_' + (4294967296 * (2097151 & h2) + (h1 >>> 0)).toString(36);
}

// ============================================================================
// 2. Built-in Bank Profile Catalog & Templates
// ============================================================================

export interface KnownBankHeader {
  bankId: string;
  bankName: string;
  headerAnchors: string[];
}

export const KNOWN_BANK_HEADERS: KnownBankHeader[] = [
  { bankId: 'melli', bankName: 'بانک ملی ایران', headerAnchors: ['بانک ملی ایران', 'بانک ملی'] },
  { bankId: 'mellat', bankName: 'بانک ملت', headerAnchors: ['بانک ملت'] },
  { bankId: 'saderat', bankName: 'بانک صادرات ایران', headerAnchors: ['بانک صادرات ایران', 'بانک صادرات'] },
  { bankId: 'tejarat', bankName: 'بانک تجارت', headerAnchors: ['*بانک تجارت*', 'بانک تجارت'] },
  { bankId: 'sepah', bankName: 'بانک سپه', headerAnchors: ['بانک سپه'] },
  { bankId: 'blu', bankName: 'بلوبانک (Blu)', headerAnchors: ['بلوبانک', 'بلو'] },
  { bankId: 'pasargad', bankName: 'بانک پاسارگاد / ویپاد', headerAnchors: ['بانک پاسارگاد', 'ویپاد'] },
  { bankId: 'refah', bankName: 'بانک رفاه کارگران', headerAnchors: ['بانک رفاه', 'رفاه حساب'] },
  { bankId: 'saman', bankName: 'بانک سامان', headerAnchors: ['بانک سامان'] },
  { bankId: 'maskan', bankName: 'بانک مسکن', headerAnchors: ['بانک مسکن'] },
  { bankId: 'keshavarzi', bankName: 'بانک کشاورزی', headerAnchors: ['بانک کشاورزی'] },
  { bankId: 'parsian', bankName: 'بانک پارسیان', headerAnchors: ['بانک پارسیان'] },
  { bankId: 'gardeshgari', bankName: 'بانک گردشگری', headerAnchors: ['بانک گردشگری'] },
  { bankId: 'mehr_iran', bankName: 'بانک قرض الحسنه مهر ایران', headerAnchors: ['بانک مهر ایران', 'بانک قرض الحسنه مهر ایران'] },
  { bankId: 'resalat', bankName: 'بانک قرض الحسنه رسالت', headerAnchors: ['بانک قرض الحسنه رسالت', 'بانک رسالت'] },
];

export function createDefaultBankProfiles(): BankProfile[] {
  return [
    {
      id: 'mellat',
      bankName: 'بانک ملت',
      isCustom: false,
      status: 'NEEDS_VERIFICATION',
      version: 1,
      senders: ['98200033', 'BankMellat', '200033'],
      requireSenderMatch: false,
      updatedAt: '۱۴۰۵/۰۸/۰۵',
      templates: [
        {
          id: 'mellat_withdrawal_v1',
          version: 1,
          name: 'قالب برداشت بهسازان ملت',
          expectedKind: 'WITHDRAWAL',
          structuralAnchors: ['بانک ملت', 'برداشت'],
          directionAnchors: ['برداشت از', 'برداشت'],
          amountPatterns: ['مبلغ\\s*:\\s*([\\d,]+)\\s*(ریال|تومان)', 'برداشت\\s*:\\s*([\\d,]+)\\s*(ریال|تومان)'],
          currencySource: 'EXPLICIT_UNIT',
          balancePatterns: ['مانده\\s*:\\s*([\\d,]+)\\s*(ریال|تومان)?'],
          trackingPatterns: ['پیگیری\\s*:\\s*([\\d\\w]+)'],
        },
        {
          id: 'mellat_deposit_v1',
          version: 1,
          name: 'قالب واریز بهسازان ملت',
          expectedKind: 'DEPOSIT',
          structuralAnchors: ['بانک ملت', 'واریز'],
          directionAnchors: ['واریز به', 'واریز'],
          amountPatterns: ['مبلغ\\s*:\\s*([\\d,]+)\\s*(ریال|تومان)', 'واریز\\s*:\\s*([\\d,]+)\\s*(ریال|تومان)'],
          currencySource: 'EXPLICIT_UNIT',
          balancePatterns: ['مانده\\s*:\\s*([\\d,]+)\\s*(ریال|تومان)?'],
          trackingPatterns: ['پیگیری\\s*:\\s*([\\d\\w]+)'],
        },
      ],
      samples: [
        {
          id: 'sample_mellat_1',
          rawText: 'بانک ملت\nبرداشت از: ۶۱۰۴۳۳******۱۲۳۴\nمبلغ: ۵۰۰,۰۰۰ ریال\nمانده: ۸,۴۲۰,۰۰۰ ریال\nتاریخ: ۱۴۰۳/۰۹/۱۰\nساعت: ۱۸:۴۵\nپیگیری: ۹۸۷۶۵۴',
          senderId: '98200033',
          isSynthetic: true,
          expectedKind: 'WITHDRAWAL',
          parseStatus: 'VALID_TRANSACTION',
          messageCategory: 'SUCCESSFUL_TRANSACTION',
          detectedKind: 'WITHDRAWAL',
          extractedAmountOriginal: 500000,
          extractedCurrency: 'RIAL',
          extractedAmountTomans: 50000,
          extractedBalanceTomans: 842000,
          extractedTrackingCode: '987654',
          matchedTemplateId: 'mellat_withdrawal_v1',
          reason: 'منطبق با قالب برداشت بهسازان ملت (نمونه آزمایشی - نیازمند تأیید کاربر)',
          userVerified: false,
        },
      ],
    },
    {
      id: 'melli',
      bankName: 'بانک ملی ایران',
      isCustom: false,
      status: 'NEEDS_VERIFICATION',
      version: 1,
      senders: ['98100064', 'BankMelli', '100064'],
      requireSenderMatch: false,
      updatedAt: '۱۴۰۵/۰۸/۰۵',
      templates: [
        {
          id: 'melli_deposit_v1',
          version: 1,
          name: 'قالب واریز سداد ملی',
          expectedKind: 'DEPOSIT',
          structuralAnchors: ['بانک ملی'],
          directionAnchors: ['واریز:'],
          amountPatterns: ['واریز\\s*:\\s*([\\d,]+)\\s*(ریال|تومان)', 'مبلغ\\s*:\\s*([\\d,]+)\\s*(ریال|تومان)'],
          currencySource: 'EXPLICIT_UNIT',
          balancePatterns: ['مانده\\s*:\\s*([\\d,]+)\\s*(ریال|تومان)?'],
          trackingPatterns: ['پیگیری\\s*:\\s*([\\d\\w]+)'],
        },
        {
          id: 'melli_withdrawal_v1',
          version: 1,
          name: 'قالب برداشت سداد ملی',
          expectedKind: 'WITHDRAWAL',
          structuralAnchors: ['بانک ملی'],
          directionAnchors: ['برداشت:', 'خرید:'],
          amountPatterns: ['(?:برداشت|خرید|مبلغ)\\s*:\\s*([\\d,]+)\\s*(ریال|تومان)'],
          currencySource: 'EXPLICIT_UNIT',
          balancePatterns: ['مانده\\s*:\\s*([\\d,]+)\\s*(ریال|تومان)?'],
          trackingPatterns: ['پیگیری\\s*:\\s*([\\d\\w]+)'],
        },
      ],
      samples: [
        {
          id: 'sample_melli_1',
          rawText: 'بانک ملی ایران\nواریز: ۱,۲۵۰,۰۰۰ ریال\nبه: ۰۱*****۱۲۳۴\nمانده: ۴۵,۸۹۰,۰۰۰ ریال\n۱۴۰۳/۰۸/۱۴-۱۱:۳۵\nپیگیری: ۸۸۷۷۱۱',
          senderId: '98100064',
          isSynthetic: true,
          expectedKind: 'DEPOSIT',
          parseStatus: 'VALID_TRANSACTION',
          messageCategory: 'SUCCESSFUL_TRANSACTION',
          detectedKind: 'DEPOSIT',
          extractedAmountOriginal: 1250000,
          extractedCurrency: 'RIAL',
          extractedAmountTomans: 125000,
          extractedBalanceTomans: 4589000,
          extractedTrackingCode: '887711',
          matchedTemplateId: 'melli_deposit_v1',
          reason: 'منطبق با قالب واریز سداد ملی (نمونه آزمایشی - نیازمند تأیید کاربر)',
          userVerified: false,
        },
      ],
    },
    {
      id: 'blu',
      bankName: 'بلوبانک (Blu)',
      isCustom: false,
      status: 'NEEDS_VERIFICATION',
      version: 1,
      senders: ['BLU', 'BluBank', '98200094'],
      requireSenderMatch: false,
      updatedAt: '۱۴۰۵/۰۸/۰۵',
      templates: [
        {
          id: 'blu_pol_transfer_v1',
          version: 1,
          name: 'قالب انتقال پل بلوبانک',
          expectedKind: 'TRANSFER',
          structuralAnchors: ['بلو', 'انتقال پل'],
          directionAnchors: ['انتقال پل'],
          amountPatterns: ['مبلغ\\s*:\\s*([\\d,]+)\\s*(ریال|تومان)'],
          currencySource: 'EXPLICIT_UNIT',
          balancePatterns: ['(?:مانده|موجودی)\\s*:\\s*([\\d,]+)\\s*(ریال|تومان)?'],
          trackingPatterns: ['(?:کد پیگیری|پیگیری)\\s*:\\s*([\\d\\w]+)'],
        },
        {
          id: 'blu_colloquial_withdrawal_v1',
          version: 1,
          name: 'قالب عامیانه برداشت بلوبانک (پرید)',
          expectedKind: 'WITHDRAWAL',
          structuralAnchors: ['بلو', 'از حساب شما پرید'],
          directionAnchors: ['برداشت پول', 'از حساب شما پرید'],
          amountPatterns: ['([\\d,]+)\\s*(ریال|تومان)\\s*از حساب شما پرید'],
          currencySource: 'EXPLICIT_UNIT',
          balancePatterns: ['(?:مانده|موجودی)\\s*:\\s*([\\d,]+)\\s*(ریال|تومان)?'],
          trackingPatterns: ['(?:کد پیگیری|پیگیری)\\s*:\\s*([\\d\\w]+)'],
        },
        {
          id: 'blu_colloquial_deposit_v1',
          version: 1,
          name: 'قالب عامیانه واریز بلوبانک (نشست)',
          expectedKind: 'DEPOSIT',
          structuralAnchors: ['بلو', 'به حساب شما نشست'],
          directionAnchors: ['واریز پول', 'به حساب شما نشست'],
          amountPatterns: ['([\\d,]+)\\s*(ریال|تومان)\\s*به حساب شما نشست'],
          currencySource: 'EXPLICIT_UNIT',
          balancePatterns: ['(?:مانده|موجودی)\\s*:\\s*([\\d,]+)\\s*(ریال|تومان)?'],
          trackingPatterns: ['(?:کد پیگیری|پیگیری)\\s*:\\s*([\\d\\w]+)'],
        },
        {
          id: 'blu_standard_withdrawal_v1',
          version: 1,
          name: 'قالب شتابی برداشت بلوبانک',
          expectedKind: 'WITHDRAWAL',
          structuralAnchors: ['بلوبانک'],
          directionAnchors: ['برداشت:', 'خرید:'],
          amountPatterns: ['(?:برداشت|خرید)\\s*:\\s*([\\d,]+)\\s*(ریال|تومان)'],
          currencySource: 'EXPLICIT_UNIT',
          balancePatterns: ['(?:مانده|موجودی)\\s*:\\s*([\\d,]+)\\s*(ریال|تومان)?'],
          trackingPatterns: ['(?:کد پیگیری|پیگیری)\\s*:\\s*([\\d\\w]+)'],
        },
      ],
      samples: [
        {
          id: 'sample_blu_1',
          rawText: 'بلو\nانتقال پل\nمبلغ: ۱,۵۰۰,۰۰۰ ریال\nبه: علیرضا یوسفی\nمانده: ۶۲,۶۶۱,۴۵۲ ریال\nکد پیگیری: ۹۸۷۶۵۴۳۲۱\n1403/09/10 11:15',
          senderId: 'BLU',
          isSynthetic: true,
          expectedKind: 'TRANSFER',
          parseStatus: 'VALID_TRANSACTION',
          messageCategory: 'SUCCESSFUL_TRANSACTION',
          detectedKind: 'TRANSFER',
          extractedAmountOriginal: 1500000,
          extractedCurrency: 'RIAL',
          extractedAmountTomans: 150000,
          extractedBalanceTomans: 6266145,
          extractedTrackingCode: '987654321',
          matchedTemplateId: 'blu_pol_transfer_v1',
          reason: 'منطبق با قالب انتقال پل بلوبانک (نمونه آزمایشی - نیازمند تأیید کاربر)',
          userVerified: false,
        },
      ],
    },
    {
      id: 'refah',
      bankName: 'بانک رفاه کارگران',
      isCustom: false,
      status: 'NEEDS_VERIFICATION',
      version: 1,
      senders: ['RefahBank', '98200071'],
      requireSenderMatch: false,
      updatedAt: '۱۴۰۵/۰۸/۰۵',
      templates: [
        {
          id: 'refah_special_r_withdrawal_v1',
          version: 1,
          name: 'قالب ویژه R برداشت بانک رفاه',
          expectedKind: 'PURCHASE',
          structuralAnchors: ['رفاه', 'بمانده'],
          directionAnchors: ['برداشت', 'خرید'],
          amountPatterns: ['برداشت\\s*([\\d,]+)\\s*R\\b'],
          currencySource: 'REFAH_R_RIAL',
          balancePatterns: ['بمانده\\s*([\\d,]+)\\s*R?'],
          trackingPatterns: ['کد\\s*([\\d]+)'],
        },
        {
          id: 'refah_special_r_deposit_v1',
          version: 1,
          name: 'قالب ویژه R واریز بانک رفاه',
          expectedKind: 'DEPOSIT',
          structuralAnchors: ['رفاه', 'بمانده'],
          directionAnchors: ['واریز'],
          amountPatterns: ['واریز\\s*([\\d,]+)\\s*R\\b'],
          currencySource: 'REFAH_R_RIAL',
          balancePatterns: ['بمانده\\s*([\\d,]+)\\s*R?'],
          trackingPatterns: ['کد\\s*([\\d]+)'],
        },
      ],
      samples: [],
    },
    {
      id: 'saderat',
      bankName: 'بانک صادرات ایران',
      isCustom: false,
      status: 'NEEDS_VERIFICATION',
      version: 1,
      senders: ['Saderat', '98200060', 'BSI'],
      requireSenderMatch: false,
      updatedAt: '۱۴۰۵/۰۸/۰۵',
      templates: [
        {
          id: 'saderat_standard_withdrawal_v1',
          version: 1,
          name: 'قالب رسمی برداشت سپهر صادرات',
          expectedKind: 'WITHDRAWAL',
          structuralAnchors: ['بانک صادرات'],
          directionAnchors: ['برداشت:'],
          amountPatterns: ['برداشت\\s*:\\s*([\\d,]+)\\s*(ریال|تومان)'],
          currencySource: 'EXPLICIT_UNIT',
          balancePatterns: ['مانده\\s*:\\s*([\\d,]+)\\s*(ریال|تومان)?'],
          trackingPatterns: ['کد پیگیری\\s*:\\s*([\\d\\w]+)'],
        },
        {
          id: 'saderat_standard_deposit_v1',
          version: 1,
          name: 'قالب رسمی واریز سپهر صادرات',
          expectedKind: 'DEPOSIT',
          structuralAnchors: ['بانک صادرات'],
          directionAnchors: ['واریز:'],
          amountPatterns: ['واریز\\s*:\\s*([\\d,]+)\\s*(ریال|تومان)'],
          currencySource: 'EXPLICIT_UNIT',
          balancePatterns: ['مانده\\s*:\\s*([\\d,]+)\\s*(ریال|تومان)?'],
          trackingPatterns: ['کد پیگیری\\s*:\\s*([\\d\\w]+)'],
        },
        {
          id: 'saderat_compact_deposit_v1',
          version: 1,
          name: 'قالب فوق‌فشرده واریز (+) صادرات',
          expectedKind: 'DEPOSIT',
          structuralAnchors: ['مانده:'],
          directionAnchors: ['+'],
          amountPatterns: ['^([\\d,]{4,})\\+$'],
          currencySource: 'COMPACT_SIGNED_RIAL',
          balancePatterns: ['مانده\\s*:\\s*([\\d,]+)'],
          trackingPatterns: [],
        },
        {
          id: 'saderat_compact_withdrawal_v1',
          version: 1,
          name: 'قالب فوق‌فشرده برداشت (-) صادرات',
          expectedKind: 'WITHDRAWAL',
          structuralAnchors: ['مانده:'],
          directionAnchors: ['-'],
          amountPatterns: ['^([\\d,]{4,})-$'],
          currencySource: 'COMPACT_SIGNED_RIAL',
          balancePatterns: ['مانده\\s*:\\s*([\\d,]+)'],
          trackingPatterns: [],
        },
      ],
      samples: [],
    },
    {
      id: 'tejarat',
      bankName: 'بانک تجارت',
      isCustom: false,
      status: 'NEEDS_VERIFICATION',
      version: 1,
      senders: ['Tejarat', '98200050'],
      requireSenderMatch: false,
      updatedAt: '۱۴۰۵/۰۸/۰۵',
      templates: [
        {
          id: 'tejarat_withdrawal_v1',
          version: 1,
          name: 'قالب برداشت بانک تجارت',
          expectedKind: 'WITHDRAWAL',
          structuralAnchors: ['بانک تجارت'],
          directionAnchors: ['برداشت:'],
          amountPatterns: ['برداشت\\s*:\\s*([\\d,]+)\\s*(ریال|تومان)'],
          currencySource: 'EXPLICIT_UNIT',
          balancePatterns: ['مانده\\s*:\\s*([\\d,]+)\\s*(ریال|تومان)?'],
          trackingPatterns: ['(?:پیگیری|کد پیگیری)\\s*:\\s*([\\d\\w]+)'],
        },
        {
          id: 'tejarat_deposit_v1',
          version: 1,
          name: 'قالب واریز بانک تجارت',
          expectedKind: 'DEPOSIT',
          structuralAnchors: ['بانک تجارت'],
          directionAnchors: ['واریز:'],
          amountPatterns: ['واریز\\s*:\\s*([\\d,]+)\\s*(ریال|تومان)'],
          currencySource: 'EXPLICIT_UNIT',
          balancePatterns: ['مانده\\s*:\\s*([\\d,]+)\\s*(ریال|تومان)?'],
          trackingPatterns: ['(?:پیگیری|کد پیگیری)\\s*:\\s*([\\d\\w]+)'],
        },
      ],
      samples: [],
    },
    {
      id: 'sepah',
      bankName: 'بانک سپه (و مؤسسات ادغامی)',
      isCustom: false,
      status: 'NEEDS_VERIFICATION',
      version: 1,
      senders: ['BankSepah', '98200020'],
      requireSenderMatch: false,
      updatedAt: '۱۴۰۵/۰۸/۰۵',
      templates: [
        {
          id: 'sepah_withdrawal_v1',
          version: 1,
          name: 'قالب برداشت داتین سپه',
          expectedKind: 'WITHDRAWAL',
          structuralAnchors: ['بانک سپه'],
          directionAnchors: ['برداشت:'],
          amountPatterns: ['برداشت\\s*:\\s*([\\d,]+)\\s*(ریال|تومان)'],
          currencySource: 'EXPLICIT_UNIT',
          balancePatterns: ['مانده\\s*:\\s*([\\d,]+)\\s*(ریال|تومان)?'],
          trackingPatterns: ['ردیف\\s*:\\s*([\\d\\w]+)'],
        },
        {
          id: 'sepah_deposit_v1',
          version: 1,
          name: 'قالب واریز داتین سپه',
          expectedKind: 'DEPOSIT',
          structuralAnchors: ['بانک سپه'],
          directionAnchors: ['واریز:'],
          amountPatterns: ['واریز\\s*:\\s*([\\d,]+)\\s*(ریال|تومان)'],
          currencySource: 'EXPLICIT_UNIT',
          balancePatterns: ['مانده\\s*:\\s*([\\d,]+)\\s*(ریال|تومان)?'],
          trackingPatterns: ['ردیف\\s*:\\s*([\\d\\w]+)'],
        },
      ],
      samples: [],
    },
    {
      id: 'pasargad',
      bankName: 'بانک پاسارگاد / ویپاد',
      isCustom: false,
      status: 'NEEDS_VERIFICATION',
      version: 1,
      senders: ['BankPasargad', 'Wepod', '982000500'],
      requireSenderMatch: false,
      updatedAt: '۱۴۰۵/۰۸/۰۵',
      templates: [
        {
          id: 'pasargad_deposit_v1',
          version: 1,
          name: 'قالب واریز داتین پاسارگاد / ویپاد',
          expectedKind: 'DEPOSIT',
          structuralAnchors: ['بانک پاسارگاد'],
          directionAnchors: ['واریز مبلغ', 'واریز:'],
          amountPatterns: ['واریز(?:\\s*مبلغ)?\\s*:?\\s*([\\d,]+)\\s*(ریال|تومان)'],
          currencySource: 'EXPLICIT_UNIT',
          balancePatterns: ['موجودی\\s*:\\s*([\\d,]+)\\s*(ریال|تومان)?'],
          trackingPatterns: ['شماره پیگیری\\s*:\\s*([\\d\\w]+)'],
        },
        {
          id: 'pasargad_withdrawal_v1',
          version: 1,
          name: 'قالب برداشت داتین پاسارگاد / ویپاد',
          expectedKind: 'WITHDRAWAL',
          structuralAnchors: ['بانک پاسارگاد'],
          directionAnchors: ['برداشت مبلغ', 'برداشت:'],
          amountPatterns: ['برداشت(?:\\s*مبلغ)?\\s*:?\\s*([\\d,]+)\\s*(ریال|تومان)'],
          currencySource: 'EXPLICIT_UNIT',
          balancePatterns: ['موجودی\\s*:\\s*([\\d,]+)\\s*(ریال|تومان)?'],
          trackingPatterns: ['شماره پیگیری\\s*:\\s*([\\d\\w]+)'],
        },
      ],
      samples: [],
    },
    {
      id: 'saman',
      bankName: 'بانک سامان',
      isCustom: false,
      status: 'NEEDS_VERIFICATION',
      version: 1,
      senders: ['SamanBank', '9820000'],
      requireSenderMatch: true,
      updatedAt: '۱۴۰۵/۰۸/۰۵',
      templates: [
        {
          id: 'saman_purchase_v1',
          version: 1,
          name: 'قالب خرید/برداشت بانک سامان (کد ارجاع)',
          expectedKind: 'PURCHASE',
          structuralAnchors: ['کد ارجاع'],
          directionAnchors: ['خرید:', 'برداشت:'],
          amountPatterns: ['(?:خرید|برداشت)\\s*:\\s*([\\d,]+)\\s*(ریال|تومان)'],
          currencySource: 'EXPLICIT_UNIT',
          balancePatterns: ['مانده\\s*:\\s*([\\d,]+)\\s*(ریال|تومان)?'],
          trackingPatterns: ['کد ارجاع\\s*:\\s*([\\d\\w]+)'],
        },
        {
          id: 'saman_deposit_v1',
          version: 1,
          name: 'قالب واریز بانک سامان (کد ارجاع)',
          expectedKind: 'DEPOSIT',
          structuralAnchors: ['کد ارجاع'],
          directionAnchors: ['واریز:'],
          amountPatterns: ['واریز\\s*:\\s*([\\d,]+)\\s*(ریال|تومان)'],
          currencySource: 'EXPLICIT_UNIT',
          balancePatterns: ['مانده\\s*:\\s*([\\d,]+)\\s*(ریال|تومان)?'],
          trackingPatterns: ['کد ارجاع\\s*:\\s*([\\d\\w]+)'],
        },
      ],
      samples: [],
    },
  ];
}

export function createPresetBankProfile(
  presetId: string,
  senderInput?: string,
  customBankName?: string
): BankProfile {
  const defaults = createDefaultBankProfiles();
  const cleanSenders = (senderInput || '')
    .split(/[,،\s]+/)
    .map((s) => s.trim())
    .filter(Boolean);

  const existingDefault = defaults.find((d) => d.id === presetId);
  if (existingDefault) {
    const mergedSenders = Array.from(
      new Set([...cleanSenders, ...existingDefault.senders])
    );
    return {
      ...existingDefault,
      senders: mergedSenders,
      updatedAt: getPersianDateString(),
    };
  }

  if (presetId === 'blu_pol') {
    const senders = Array.from(
      new Set([...cleanSenders, 'BLU', 'BluBank', '98200094'])
    );
    return {
      id: 'blu_pol',
      bankName: 'بلوبانک حالت پل',
      isCustom: false,
      status: 'NEEDS_VERIFICATION',
      version: 1,
      senders,
      requireSenderMatch: false,
      updatedAt: getPersianDateString(),
      templates: [
        {
          id: 'blu_pol_only_transfer_v1',
          version: 1,
          name: 'قالب انتقال پل بلوبانک (ویژه)',
          expectedKind: 'TRANSFER',
          structuralAnchors: ['بلو', 'انتقال پل'],
          directionAnchors: ['انتقال پل', 'پل'],
          amountPatterns: [
            'مبلغ\\s*:\\s*([\\d,]+)\\s*(ریال|تومان)',
            '([\\d,]+)\\s*(ریال|تومان)',
          ],
          currencySource: 'EXPLICIT_UNIT',
          balancePatterns: ['(?:مانده|موجودی)\\s*:\\s*([\\d,]+)\\s*(ریال|تومان)?'],
          trackingPatterns: ['(?:کد پیگیری|پیگیری)\\s*:\\s*([\\d\\w]+)'],
        },
        {
          id: 'blu_pol_deposit_v1',
          version: 1,
          name: 'قالب واریز پل بلوبانک',
          expectedKind: 'DEPOSIT',
          structuralAnchors: ['بلو'],
          directionAnchors: ['واریز پل', 'به حساب شما نشست', 'واریز پول'],
          amountPatterns: [
            'مبلغ\\s*:\\s*([\\d,]+)\\s*(ریال|تومان)',
            '([\\d,]+)\\s*(ریال|تومان)\\s*به حساب شما نشست',
          ],
          currencySource: 'EXPLICIT_UNIT',
          balancePatterns: ['(?:مانده|موجودی)\\s*:\\s*([\\d,]+)\\s*(ریال|تومان)?'],
          trackingPatterns: ['(?:کد پیگیری|پیگیری)\\s*:\\s*([\\d\\w]+)'],
        },
      ],
      samples: [
        {
          id: 'sample_blu_pol_1',
          rawText:
            'بلو\nانتقال پل\nمبلغ: ۱,۵۰۰,۰۰۰ ریال\nبه: علیرضا یوسفی\nمانده: ۶۲,۶۶۱,۴۵۲ ریال\nکد پیگیری: ۹۸۷۶۵۴۳۲۱\n1403/09/10 11:15',
          senderId: senders[0] || 'BLU',
          isSynthetic: true,
          expectedKind: 'TRANSFER',
          parseStatus: 'VALID_TRANSACTION',
          messageCategory: 'SUCCESSFUL_TRANSACTION',
          detectedKind: 'TRANSFER',
          extractedAmountOriginal: 1500000,
          extractedCurrency: 'RIAL',
          extractedAmountTomans: 150000,
          extractedBalanceTomans: 6266145,
          extractedTrackingCode: '987654321',
          matchedTemplateId: 'blu_pol_only_transfer_v1',
          reason:
            'منطبق با قالب انتقال پل بلوبانک (نمونه آزمایشی - نیازمند تأیید کاربر)',
          userVerified: false,
        },
      ],
    };
  }

  if (presetId === 'gardeshgari') {
    const senders = Array.from(
      new Set([...cleanSenders, '982000110', 'Gardeshgari', '2000110'])
    );
    return {
      id: 'gardeshgari',
      bankName: 'بانک گردشگری',
      isCustom: false,
      status: 'NEEDS_VERIFICATION',
      version: 1,
      senders,
      requireSenderMatch: false,
      updatedAt: getPersianDateString(),
      templates: [
        {
          id: 'gardeshgari_withdrawal_v1',
          version: 1,
          name: 'قالب برداشت بانک گردشگری',
          expectedKind: 'WITHDRAWAL',
          structuralAnchors: ['بانک گردشگری'],
          directionAnchors: ['برداشت:', 'برداشت مبلغ', 'خرید:'],
          amountPatterns: [
            '(?:برداشت|خرید|مبلغ)\\s*:?\\s*([\\d,]+)\\s*(ریال|تومان)',
          ],
          currencySource: 'EXPLICIT_UNIT',
          balancePatterns: ['(?:مانده|موجودی)\\s*:\\s*([\\d,]+)\\s*(ریال|تومان)?'],
          trackingPatterns: ['(?:پیگیری|کد پیگیری|مرجع)\\s*:\\s*([\\d\\w]+)'],
        },
        {
          id: 'gardeshgari_deposit_v1',
          version: 1,
          name: 'قالب واریز بانک گردشگری',
          expectedKind: 'DEPOSIT',
          structuralAnchors: ['بانک گردشگری'],
          directionAnchors: ['واریز:', 'واریز مبلغ'],
          amountPatterns: ['(?:واریز|مبلغ)\\s*:?\\s*([\\d,]+)\\s*(ریال|تومان)'],
          currencySource: 'EXPLICIT_UNIT',
          balancePatterns: ['(?:مانده|موجودی)\\s*:\\s*([\\d,]+)\\s*(ریال|تومان)?'],
          trackingPatterns: ['(?:پیگیری|کد پیگیری|مرجع)\\s*:\\s*([\\d\\w]+)'],
        },
      ],
      samples: [
        {
          id: 'sample_gardeshgari_1',
          rawText:
            'بانک گردشگری\nبرداشت: ۸۵۰,۰۰۰ ریال\nمانده: ۱۲,۴۰۰,۰۰۰ ریال\nپیگیری: ۴۴۵۵۶۶',
          senderId: senders[0] || '982000110',
          isSynthetic: true,
          expectedKind: 'WITHDRAWAL',
          parseStatus: 'VALID_TRANSACTION',
          messageCategory: 'SUCCESSFUL_TRANSACTION',
          detectedKind: 'WITHDRAWAL',
          extractedAmountOriginal: 850000,
          extractedCurrency: 'RIAL',
          extractedAmountTomans: 85000,
          extractedBalanceTomans: 1240000,
          extractedTrackingCode: '445566',
          matchedTemplateId: 'gardeshgari_withdrawal_v1',
          reason:
            'منطبق با قالب برداشت بانک گردشگری (نمونه آزمایشی - نیازمند تأیید کاربر)',
          userVerified: false,
        },
      ],
    };
  }

  if (presetId === 'keshavarzi') {
    const senders = Array.from(
      new Set([...cleanSenders, '9820009102', 'Keshavarzi', '20009102'])
    );
    return {
      id: 'keshavarzi',
      bankName: 'بانک کشاورزی',
      isCustom: false,
      status: 'NEEDS_VERIFICATION',
      version: 1,
      senders,
      requireSenderMatch: false,
      updatedAt: getPersianDateString(),
      templates: [
        {
          id: 'keshavarzi_withdrawal_v1',
          version: 1,
          name: 'قالب برداشت مهرگستر کشاورزی',
          expectedKind: 'WITHDRAWAL',
          structuralAnchors: ['بانک کشاورزی'],
          directionAnchors: ['برداشت:', 'برداشت مبلغ', 'خرید:'],
          amountPatterns: [
            '(?:برداشت|خرید|مبلغ)\\s*:?\\s*([\\d,]+)\\s*(ریال|تومان)',
          ],
          currencySource: 'EXPLICIT_UNIT',
          balancePatterns: ['(?:مانده|موجودی)\\s*:\\s*([\\d,]+)\\s*(ریال|تومان)?'],
          trackingPatterns: ['(?:پیگیری|کد پیگیری|سند)\\s*:\\s*([\\d\\w]+)'],
        },
        {
          id: 'keshavarzi_deposit_v1',
          version: 1,
          name: 'قالب واریز مهرگستر کشاورزی',
          expectedKind: 'DEPOSIT',
          structuralAnchors: ['بانک کشاورزی'],
          directionAnchors: ['واریز:', 'واریز مبلغ'],
          amountPatterns: ['(?:واریز|مبلغ)\\s*:?\\s*([\\d,]+)\\s*(ریال|تومان)'],
          currencySource: 'EXPLICIT_UNIT',
          balancePatterns: ['(?:مانده|موجودی)\\s*:\\s*([\\d,]+)\\s*(ریال|تومان)?'],
          trackingPatterns: ['(?:پیگیری|کد پیگیری|سند)\\s*:\\s*([\\d\\w]+)'],
        },
      ],
      samples: [
        {
          id: 'sample_keshavarzi_1',
          rawText:
            'بانک کشاورزی\nبرداشت: ۶۲۰,۰۰۰ ریال\nمانده: ۹,۸۰۰,۰۰۰ ریال\nپیگیری: ۷۷۸۸۹۹',
          senderId: senders[0] || '9820009102',
          isSynthetic: true,
          expectedKind: 'WITHDRAWAL',
          parseStatus: 'VALID_TRANSACTION',
          messageCategory: 'SUCCESSFUL_TRANSACTION',
          detectedKind: 'WITHDRAWAL',
          extractedAmountOriginal: 620000,
          extractedCurrency: 'RIAL',
          extractedAmountTomans: 62000,
          extractedBalanceTomans: 980000,
          extractedTrackingCode: '778899',
          matchedTemplateId: 'keshavarzi_withdrawal_v1',
          reason:
            'منطبق با قالب برداشت مهرگستر کشاورزی (نمونه آزمایشی - نیازمند تأیید کاربر)',
          userVerified: false,
        },
      ],
    };
  }

  const finalName = (customBankName || 'بانک سفارشی').trim();
  const id = `custom_bank_${Date.now()}`;
  return {
    id,
    bankName: finalName,
    isCustom: true,
    status: 'DRAFT',
    version: 1,
    senders: cleanSenders,
    requireSenderMatch: false,
    updatedAt: getPersianDateString(),
    templates: [
      {
        id: `${id}_debit_v1`,
        version: 1,
        name: `برداشت استاندارد ${finalName}`,
        expectedKind: 'WITHDRAWAL',
        directionAnchors: ['برداشت', 'خرید', 'انتقال'],
        amountPatterns: [
          '(?:مبلغ|برداشت|خرید|انتقال)\\s*:?\\s*([\\d,]+)\\s*(ریال|تومان)',
          '([\\d,]+)\\s*(ریال|تومان)',
        ],
        currencySource: 'EXPLICIT_UNIT',
        balancePatterns: ['(?:مانده|موجودی)\\s*:?\\s*([\\d,]+)\\s*(ریال|تومان)?'],
        trackingPatterns: ['(?:پیگیری|ردیف|مرجع)\\s*:?\\s*([\\d\\w]+)'],
        isCustom: true,
      },
      {
        id: `${id}_credit_v1`,
        version: 1,
        name: `واریز استاندارد ${finalName}`,
        expectedKind: 'DEPOSIT',
        directionAnchors: ['واریز', 'واریزی'],
        amountPatterns: [
          '(?:مبلغ|واریز|واریزی)\\s*:?\\s*([\\d,]+)\\s*(ریال|تومان)',
          '([\\d,]+)\\s*(ریال|تومان)',
        ],
        currencySource: 'EXPLICIT_UNIT',
        balancePatterns: ['(?:مانده|موجودی)\\s*:?\\s*([\\d,]+)\\s*(ریال|تومان)?'],
        trackingPatterns: ['(?:پیگیری|ردیف|مرجع)\\s*:?\\s*([\\d\\w]+)'],
        isCustom: true,
      },
    ],
    samples: [],
  };
}

// ============================================================================
// 3. Staged SMS Security & Classification Pipeline
// ============================================================================

const OTP_PATTERNS: RegExp[] = [
  /رمز\s*پویا/i,
  /رمز\s*دوم/i,
  /رمز\s*یک\s*بار/i,
  /رمز\s*یکبار/i,
  /کد\s*تایید/i,
  /کد\s*تأیید/i,
  /کد\s*امنیتی/i,
  /کد\s*محرمانه/i,
  /کد\s*ورود/i,
  /کد\s*فعال\s*سازی/i,
  /کد\s*فعالسازی/i,
  /\botp\b/i,
  /verification\s*code/i,
  /پویا\s*:\s*\d{4,8}/i,
];

const FAILED_TX_PATTERNS: RegExp[] = [
  /ناموفق/i,
  /نا\s*موفق/i,
  /عدم\s*موفقیت/i,
  /انجام\s*نشد/i,
  /رد\s*شد/i,
  /لغو\s*شد/i,
  /موجودی\s*کافی\s*نیست/i,
  /عدم\s*موجودی/i,
  /خطا\s*در\s*تراکنش/i,
  /خطای\s*تراکنش/i,
];

const PROMO_PATTERNS: RegExp[] = [
  /لغو\s*11/i,
  /قرعه\s*کشی/i,
  /جشنواره/i,
  /شانس\s*برنده/i,
  /تخفیف/i,
  /وام\s*فوری/i,
  /بدون\s*ضامن/i,
  /خرید\s*اقساطی/i,
  /کلیک\s*کنید/i,
  /لینک\s*زیر/i,
  /https?:\/\//i,
  /هدیه\s*ویژه/i,
  /امتیاز\s*باشگاه/i,
];

const SECURITY_ALERT_PATTERNS: RegExp[] = [
  /ورود\s*به\s*همراه\s*بانک/i,
  /ورود\s*به\s*اینترنت\s*بانک/i,
  /ورود\s*به\s*سامانه/i,
  /مسدود\s*شد/i,
  /تغییر\s*رمز/i,
  /هشدار\s*امنیتی/i,
  /تلاش\s*ناموفق/i,
];

/**
 * Classifies non-transaction messages (OTP, Promotional, Failed, Security Alert, Informational)
 * BEFORE any financial extraction takes place.
 */
export function classifyNonTransactionMessage(normalizedLinesText: string): {
  isNonTransaction: boolean;
  category: SmsMessageCategory;
  kind: SmsTransactionKind;
  reason?: string;
} {
  for (const rx of OTP_PATTERNS) {
    if (rx.test(normalizedLinesText)) {
      return {
        isNonTransaction: true,
        category: 'OTP',
        kind: 'UNKNOWN',
        reason: 'این پیامک حاوی رمز یک‌بارمصرف (OTP / رمز پویا) است و تراکنش مالی محسوب نمی‌شود.',
      };
    }
  }

  for (const rx of FAILED_TX_PATTERNS) {
    if (rx.test(normalizedLinesText)) {
      return {
        isNonTransaction: true,
        category: 'FAILED_TRANSACTION',
        kind: 'FAILED',
        reason: 'این پیامک مربوط به یک تراکنش ناموفق است و نباید به‌عنوان تراکنش موفق ثبت شود.',
      };
    }
  }

  for (const rx of PROMO_PATTERNS) {
    if (rx.test(normalizedLinesText)) {
      return {
        isNonTransaction: true,
        category: 'PROMOTIONAL',
        kind: 'UNKNOWN',
        reason: 'این پیامک تبلیغاتی یا اطلاع‌رسانی جشنواره است و تراکنش واقعی نیست.',
      };
    }
  }

  for (const rx of SECURITY_ALERT_PATTERNS) {
    if (rx.test(normalizedLinesText)) {
      return {
        isNonTransaction: true,
        category: 'SECURITY_ALERT',
        kind: 'UNKNOWN',
        reason: 'این پیامک یک هشدار امنیتی یا اعلان ورود به سامانه بانکی است.',
      };
    }
  }

  // Check if message has NO transaction action keywords at all (e.g., pure balance inquiry or notice)
  const hasActionKeyword =
    /(?:واریز|برداشت|خرید|انتقال|کارمزد|برگشت\s*وجه|استرداد|از\s*حساب\s*شما\s*پرید|به\s*حساب\s*شما\s*نشست)/i.test(
      normalizedLinesText
    ) ||
    normalizedLinesText
      .split('\n')
      .some((line) => /^[\d,]{4,}[+-]$/.test(line.trim()));

  if (!hasActionKeyword) {
    return {
      isNonTransaction: true,
      category: 'INFORMATIONAL',
      kind: 'UNKNOWN',
      reason: 'این پیامک فاقد الگوی عملیات مالی (واریز/برداشت/خرید/انتقال) است و صرفاً اطلاع‌رسانی یا اعلام وضعیت است.',
    };
  }

  return {
    isNonTransaction: false,
    category: 'SUCCESSFUL_TRANSACTION',
    kind: 'UNKNOWN',
  };
}

/**
 * Strips non-amount identifiers (dates, times, card numbers, account numbers, tracking codes)
 * from a line or block of text so they can never be mistaken for an amount.
 */
function stripIdentifierTokens(text: string): string {
  return text
    // Dates like 1403/09/10 or 1403.08.05 or 1399/12/26-16:56
    .replace(/\b1[34]\d{2}[\/.-]\d{1,2}[\/.-]\d{1,2}(?:-\d{1,2}:\d{2})?\b/g, ' [DATE] ')
    // Times like 18:45 or 11:35:20
    .replace(/\b\d{1,2}:\d{2}(?::\d{2})?\b/g, ' [TIME] ')
    // Masked card/account numbers like 610433******1234 or 01*****1234 or 62****13
    .replace(/\b\d{2,6}[*×xX]+\d{2,6}\b/g, ' [MASKED_ID] ')
    // Dot-separated account numbers like 311.8000.987654.1
    .replace(/\b\d{1,4}(?:\.\d{2,10}){2,4}\b/g, ' [DOT_ACC] ')
    // Multi-dash deposit numbers like 1-123456-40-3001
    .replace(/\b\d{1,4}-\d{3,8}-\d{1,4}-\d{2,6}\b/g, ' [DASH_ACC] ')
    // Explicit account/card labels with unmasked or masked numbers
    .replace(
      /(?:از\s*کارت|به\s*کارت|شماره\s*کارت|کارت|از\s*حساب|به\s*حساب|شماره\s*حساب|حساب|از\s*سپرده|به\s*سپرده|سپرده)\s*:?\s*[\d*×xX.-]+/g,
      ' [ACC_FIELD] '
    )
    // Explicit tracking/reference/document codes
    .replace(
      /(?:کد\s*پیگیری|شماره\s*پیگیری|شناسه\s*پیگیری|پیگیری|کد\s*ارجاع|شماره\s*ارجاع|ارجاع|کد\s*مرجع|کد\s*رهگیری|شماره\s*سند|سند|ردیف|کد)\s*:?\s*[\d\w/-]+/g,
      ' [TRACK_FIELD] '
    );
}

/**
 * Extracts balance independently and returns both the extracted balance (in Tomans)
 * and the text with all balance segments removed so balance is never confused with transaction amount.
 */
function extractAndRemoveBalance(
  normalizedText: string,
  balancePatterns: string[]
): { balanceInTomans?: number; textWithoutBalance: string } {
  const lines = normalizedText.split('\n');
  let balanceInTomans: number | undefined = undefined;
  const remainingLines: string[] = [];

  const defaultBalanceRx = /(?:مانده\s*حساب|مانده|موجودی|بمانده)\s*:?\s*([\d,]+)\s*(ریال|تومان|R)?/i;

  for (const line of lines) {
    let matchedInLine = false;

    for (const pat of balancePatterns) {
      const rx = new RegExp(pat, 'i');
      const m = line.match(rx);
      if (m && m[1]) {
        const rawVal = parseInt(m[1].replace(/,/g, ''), 10);
        const unit = (m[2] || '').trim();
        if (Number.isFinite(rawVal) && rawVal >= 0) {
          balanceInTomans = unit === 'تومان' ? rawVal : Math.floor(rawVal / 10);
          matchedInLine = true;
          break;
        }
      }
    }

    if (!matchedInLine) {
      const m = line.match(defaultBalanceRx);
      if (m && m[1]) {
        const rawVal = parseInt(m[1].replace(/,/g, ''), 10);
        const unit = (m[2] || '').trim();
        if (Number.isFinite(rawVal) && rawVal >= 0) {
          balanceInTomans = unit === 'تومان' ? rawVal : Math.floor(rawVal / 10);
          matchedInLine = true;
        }
      }
    }

    if (!matchedInLine) {
      remainingLines.push(line);
    } else {
      // Remove the balance clause from the line in case the line also has other info
      const stripped = line.replace(
        /(?:مانده\s*حساب|مانده|موجودی|بمانده)\s*:?\s*[\d,]+\s*(?:ریال|تومان|R)?/gi,
        ''
      ).trim();
      if (stripped.length > 0) {
        remainingLines.push(stripped);
      }
    }
  }

  return {
    balanceInTomans,
    textWithoutBalance: remainingLines.join('\n'),
  };
}

function extractTrackingCode(normalizedText: string, trackingPatterns: string[]): string | undefined {
  for (const pat of trackingPatterns) {
    const rx = new RegExp(pat, 'i');
    const m = normalizedText.match(rx);
    if (m && m[1]) return m[1].trim();
  }
  const fallbackRx =
    /(?:کد\s*پیگیری|شماره\s*پیگیری|شناسه\s*پیگیری|پیگیری|کد\s*ارجاع|شماره\s*ارجاع|ارجاع|کد\s*مرجع|کد\s*رهگیری|شماره\s*سند|سند|ردیف)\s*:\s*([\d\w]+)/i;
  const fm = normalizedText.match(fallbackRx);
  return fm?.[1]?.trim();
}

export interface AnalyzeBankSmsOptions {
  selectedBankId?: string;
  senderId?: string;
  profiles?: BankProfile[];
  existingExpenses?: ExpenseItem[];
  allowUnverifiedPreview?: boolean;
}

/**
 * Main 10-Stage Safe SMS Analysis Engine
 */
export function analyzeBankSms(
  rawText: string,
  options: AnalyzeBankSmsOptions = {}
): StructuredSmsAnalysisResult {
  const normalized = normalizeSmsPreserveLines(rawText);
  const fingerprint = computeSmsFingerprint(rawText, options.senderId);
  const profiles = options.profiles || loadBankProfiles();
  const existingExpenses = options.existingExpenses || [];

  // Stage 1: Empty check
  if (!normalized) {
    return {
      status: 'NOT_A_TRANSACTION',
      messageCategory: 'UNKNOWN',
      transactionKind: 'UNKNOWN',
      senderMatched: false,
      currency: 'UNKNOWN',
      merchant: '',
      reason: 'متن پیامک خالی است.',
      messageFingerprint: fingerprint,
      isDuplicate: false,
      canAutoRegister: false,
      rawNormalized: '',
    };
  }

  // Stage 2: Check for duplicate message in existing expenses
  const isDuplicateByFingerprint = existingExpenses.some(
    (e) => e.messageFingerprint && e.messageFingerprint === fingerprint
  );

  // Stage 3: Non-Transaction Classification (OTP, Failed, Promo, Security, Pure Info)
  const nonTxCheck = classifyNonTransactionMessage(normalized);
  if (nonTxCheck.isNonTransaction) {
    return {
      status: 'NOT_A_TRANSACTION',
      messageCategory: nonTxCheck.category,
      transactionKind: nonTxCheck.kind,
      senderId: options.senderId,
      senderMatched: false,
      currency: 'UNKNOWN',
      merchant: '',
      reason: nonTxCheck.reason || 'پیامک غیرتراکنشی است.',
      messageFingerprint: fingerprint,
      isDuplicate: isDuplicateByFingerprint,
      canAutoRegister: false,
      rawNormalized: normalized,
    };
  }

  // Stage 4: Resolve Bank Profile (Explicit Selection or Unique Sender Match — NEVER guess from text!)
  let profile: BankProfile | undefined = undefined;
  const senderClean = (options.senderId || '').trim().toLowerCase();

  if (options.selectedBankId && options.selectedBankId.trim() !== '') {
    profile = profiles.find((p) => p.id === options.selectedBankId);
    if (!profile) {
      return {
        status: 'UNSUPPORTED_FORMAT',
        messageCategory: 'UNKNOWN',
        transactionKind: 'UNKNOWN',
        bankId: options.selectedBankId,
        senderId: options.senderId,
        senderMatched: false,
        currency: 'UNKNOWN',
        merchant: '',
        reason: 'پروفایل بانک انتخاب‌شده در سیستم یافت نشد.',
        messageFingerprint: fingerprint,
        isDuplicate: isDuplicateByFingerprint,
        canAutoRegister: false,
        rawNormalized: normalized,
      };
    }
  } else if (senderClean) {
    // Only match by sender if sender is explicitly registered in exactly one profile
    const matchingProfiles = profiles.filter((p) =>
      p.senders.some((s) => s.trim().toLowerCase() === senderClean)
    );
    if (matchingProfiles.length === 1) {
      profile = matchingProfiles[0];
    }
  }

  // If no bank was explicitly selected and no registered sender uniquely identified the bank:
  // Do NOT guess the bank from text! Route to NEEDS_REVIEW.
  if (!profile) {
    return {
      status: 'NEEDS_REVIEW',
      messageCategory: 'SUCCESSFUL_TRANSACTION',
      transactionKind: 'UNKNOWN',
      senderId: options.senderId,
      senderMatched: false,
      currency: 'UNKNOWN',
      merchant: 'نیازمند تعیین بانک',
      reason:
        'بانک به صورت صریح انتخاب نشده است و فرستنده پیامک نیز با هیچ پروفایل فعالی مطابقت ندارد. سیستم به صورت حدسی بانک را از متن انتخاب نمی‌کند.',
      messageFingerprint: fingerprint,
      isDuplicate: isDuplicateByFingerprint,
      canAutoRegister: false,
      rawNormalized: normalized,
    };
  }

  // Stage 5: Cross-Bank Conflict Detection
  // Verify the SMS doesn't explicitly carry the header of a different known bank
  for (const kb of KNOWN_BANK_HEADERS) {
    const isSameBankFamily =
      kb.bankId === profile.id ||
      (profile.id === 'blu_pol' && kb.bankId === 'blu');
    if (!isSameBankFamily) {
      const hasOtherBankHeader = kb.headerAnchors.some((anchor) => {
        const firstLine = normalized.split('\n')[0] || '';
        return firstLine === anchor || firstLine.startsWith(anchor + ' ');
      });
      if (hasOtherBankHeader) {
        return {
          status: 'NEEDS_REVIEW',
          messageCategory: 'SUCCESSFUL_TRANSACTION',
          transactionKind: 'UNKNOWN',
          bankId: profile.id,
          bankName: profile.bankName,
          senderId: options.senderId,
          senderMatched: false,
          currency: 'UNKNOWN',
          merchant: '',
          reason: `ناسازگاری بانک: پروفایل «${profile.bankName}» انتخاب شده اما سرخط پیامک مربوط به «${kb.bankName}» است.`,
          messageFingerprint: fingerprint,
          isDuplicate: isDuplicateByFingerprint,
          canAutoRegister: false,
          rawNormalized: normalized,
        };
      }
    }
  }

  // Stage 6: Explicit Sender Compatibility Policy
  const senderPolicy = evaluateSenderPolicy(
    profile,
    options.senderId,
    options.allowUnverifiedPreview
  );
  const senderMatched = senderPolicy.senderMatched;
  const senderMismatchReason = senderPolicy.reason;

  // Stage 7: Template Matching & Independent Extraction
  for (const tpl of profile.templates) {
    // 7a. Check structural anchors
    if (tpl.structuralAnchors && tpl.structuralAnchors.length > 0) {
      const allAnchorsPresent = tpl.structuralAnchors.every((anc) => normalized.includes(anc));
      if (!allAnchorsPresent) continue;
    }

    // 7b. Extract balance independently & strip balance lines
    const { balanceInTomans, textWithoutBalance } = extractAndRemoveBalance(
      normalized,
      tpl.balancePatterns
    );

    // 7c. Check direction anchors on non-balance text
    const hasDirectionAnchor = tpl.directionAnchors.some((dAnc) => {
      if (dAnc === '+' || dAnc === '-') {
        return textWithoutBalance
          .split('\n')
          .some((l) => (dAnc === '+' ? /^[\d,]{4,}\+$/.test(l) : /^[\d,]{4,}-$/.test(l)));
      }
      return textWithoutBalance.includes(dAnc);
    });
    if (!hasDirectionAnchor) continue;

    // 7d. Strip non-amount identifiers (card, account, dates, times, tracking codes)
    const sanitizedForAmount = stripIdentifierTokens(textWithoutBalance);

    // 7e. Match amount using template's amountPatterns
    let extractedOriginal: number | undefined = undefined;
    let detectedCurrency: CurrencyUnit = 'UNKNOWN';

    for (const amtPat of tpl.amountPatterns) {
      const rx = new RegExp(amtPat, 'im');
      const m = sanitizedForAmount.match(rx);
      if (m && m[1]) {
        const numVal = parseInt(m[1].replace(/,/g, ''), 10);
        if (Number.isFinite(numVal) && numVal > 0) {
          extractedOriginal = numVal;
          if (tpl.currencySource === 'EXPLICIT_UNIT') {
            const unitStr = (m[2] || '').trim();
            if (unitStr === 'ریال') detectedCurrency = 'RIAL';
            else if (unitStr === 'تومان') detectedCurrency = 'TOMAN';
            else detectedCurrency = 'UNKNOWN';
          } else if (tpl.currencySource === 'REFAH_R_RIAL') {
            detectedCurrency = 'RIAL';
          } else if (tpl.currencySource === 'COMPACT_SIGNED_RIAL') {
            detectedCurrency = 'RIAL';
          }
          break;
        }
      }
    }

    if (extractedOriginal === undefined) {
      continue;
    }

    // Extract tracking code
    const trackingCode = extractTrackingCode(normalized, tpl.trackingPatterns);

    // Determine currency conversion
    if (detectedCurrency === 'UNKNOWN') {
      return {
        status: 'NEEDS_REVIEW',
        messageCategory: 'SUCCESSFUL_TRANSACTION',
        transactionKind: tpl.expectedKind,
        bankId: profile.id,
        bankName: profile.bankName,
        senderId: options.senderId,
        senderMatched,
        matchedTemplateId: tpl.id,
        matchedTemplateName: tpl.name,
        matchedTemplateVersion: tpl.version,
        amountOriginal: extractedOriginal,
        currency: 'UNKNOWN',
        balanceInTomans,
        trackingCode,
        merchant: buildMerchantTitle(tpl.expectedKind, normalized),
        reason: 'واحد پول (ریال یا تومان) برای مبلغ تراکنش مشخص نیست و نیاز به بررسی دستی دارد.',
        messageFingerprint: fingerprint,
        isDuplicate: isDuplicateByFingerprint,
        canAutoRegister: false,
        rawNormalized: normalized,
      };
    }

    const amountInTomans =
      detectedCurrency === 'RIAL' ? Math.floor(extractedOriginal / 10) : extractedOriginal;

    if (amountInTomans <= 0) {
      return {
        status: 'NEEDS_REVIEW',
        messageCategory: 'SUCCESSFUL_TRANSACTION',
        transactionKind: tpl.expectedKind,
        bankId: profile.id,
        bankName: profile.bankName,
        senderId: options.senderId,
        senderMatched,
        matchedTemplateId: tpl.id,
        matchedTemplateName: tpl.name,
        matchedTemplateVersion: tpl.version,
        amountOriginal: extractedOriginal,
        currency: detectedCurrency,
        amountInTomans: 0,
        balanceInTomans,
        trackingCode,
        merchant: buildMerchantTitle(tpl.expectedKind, normalized),
        reason: 'مبلغ تراکنش پس از تبدیل به تومان نامعتبر یا صفر است و نیاز به بررسی دارد.',
        messageFingerprint: fingerprint,
        isDuplicate: isDuplicateByFingerprint,
        canAutoRegister: false,
        rawNormalized: normalized,
      };
    }

    // Check tracking-code based duplicate on same bank + trackingCode + amount
    const isDuplicateByTracking =
      Boolean(trackingCode) &&
      existingExpenses.some(
        (e) =>
          e.bankId === profile!.id &&
          e.trackingCode === trackingCode &&
          e.amount === amountInTomans
      );
    const isDuplicate = isDuplicateByFingerprint || isDuplicateByTracking;

    // Strictly derive financial entry type from known transaction kind (never default UNKNOWN to expense)
    const financialType = resolveFinancialTypeFromKind(tpl.expectedKind, normalized);

    const merchant = buildMerchantTitle(tpl.expectedKind, normalized);

    // Check sender mismatch or missing required sender
    if (!senderMatched) {
      return {
        status: 'NEEDS_REVIEW',
        messageCategory: 'SUCCESSFUL_TRANSACTION',
        transactionKind: tpl.expectedKind,
        financialType,
        bankId: profile.id,
        bankName: profile.bankName,
        senderId: options.senderId,
        senderMatched: false,
        matchedTemplateId: tpl.id,
        matchedTemplateName: tpl.name,
        matchedTemplateVersion: tpl.version,
        amountOriginal: extractedOriginal,
        currency: detectedCurrency,
        amountInTomans,
        balanceInTomans,
        trackingCode,
        merchant,
        reason:
          senderMismatchReason ||
          'فرستنده پیامک با پروفایل بانک مطابقت ندارد و نیازمند تأیید دستی است.',
        messageFingerprint: fingerprint,
        isDuplicate,
        canAutoRegister: false,
        rawNormalized: normalized,
      };
    }

    // Template-level verification check:
    // Only allow automatic registration if the profile is ACTIVE AND this specific template
    // has been verified for its CURRENT version (tpl.version).
    const isTemplateActive = isTemplateVerifiedForAutoRegister(profile, tpl);

    if (!isTemplateActive && !options.allowUnverifiedPreview) {
      const unverifiedReason =
        profile.status !== 'ACTIVE'
          ? `قالب «${tpl.name}» شناسایی شد، اما پروفایل «${profile.bankName}» هنوز توسط شما تأیید و فعال نشده است (وضعیت: ${translateProfileStatus(profile.status)}).`
          : `قالب «${tpl.name}» (نسخه v${tpl.version}) در «${profile.bankName}» هنوز نمونه تأییدشده برای نسخه فعلی خود ندارد؛ تأیید سایر قالب‌های بانک مجوز ثبت خودکار این قالب نیست.`;

      return {
        status: 'NEEDS_REVIEW',
        messageCategory: 'SUCCESSFUL_TRANSACTION',
        transactionKind: tpl.expectedKind,
        financialType,
        bankId: profile.id,
        bankName: profile.bankName,
        senderId: options.senderId,
        senderMatched: true,
        matchedTemplateId: tpl.id,
        matchedTemplateName: tpl.name,
        matchedTemplateVersion: tpl.version,
        amountOriginal: extractedOriginal,
        currency: detectedCurrency,
        amountInTomans,
        balanceInTomans,
        trackingCode,
        merchant,
        reason: unverifiedReason,
        messageFingerprint: fingerprint,
        isDuplicate,
        canAutoRegister: false,
        rawNormalized: normalized,
      };
    }

    return {
      status: 'VALID_TRANSACTION',
      messageCategory: 'SUCCESSFUL_TRANSACTION',
      transactionKind: tpl.expectedKind,
      financialType,
      bankId: profile.id,
      bankName: profile.bankName,
      senderId: options.senderId,
      senderMatched: true,
      matchedTemplateId: tpl.id,
      matchedTemplateName: tpl.name,
      matchedTemplateVersion: tpl.version,
      amountOriginal: extractedOriginal,
      currency: detectedCurrency,
      amountInTomans,
      balanceInTomans,
      trackingCode,
      merchant,
      reason: isDuplicate
        ? `این پیامک قبلاً ثبت شده است (تکراری - قالب: ${tpl.name}).`
        : `منطبق با قالب تأییدشده «${tpl.name}» (${profile.bankName} - نسخه ${tpl.version}).`,
      messageFingerprint: fingerprint,
      isDuplicate,
      canAutoRegister: isTemplateActive && !isDuplicate && Boolean(financialType),
      rawNormalized: normalized,
    };
  }

  // Stage 8: Check if the message had a direction keyword for this bank, but amount was missing unit or malformed
  const { balanceInTomans, textWithoutBalance } = extractAndRemoveBalance(normalized, []);
  const sanitizedRemainder = stripIdentifierTokens(textWithoutBalance);
  const unitlessAmountMatch = sanitizedRemainder.match(
    /(?:مبلغ|برداشت|واریز|خرید|انتقال|کارمزد)\s*:?\s*([\d,]{3,})(?!\s*(?:ریال|تومان|R|\+|-))/i
  );

  if (unitlessAmountMatch) {
    return {
      status: 'NEEDS_REVIEW',
      messageCategory: 'SUCCESSFUL_TRANSACTION',
      transactionKind: 'UNKNOWN',
      bankId: profile.id,
      bankName: profile.bankName,
      senderId: options.senderId,
      senderMatched,
      amountOriginal: parseInt(unitlessAmountMatch[1].replace(/,/g, ''), 10),
      currency: 'UNKNOWN',
      balanceInTomans,
      merchant: 'تراکنش نیازمند بررسی واحد پول',
      reason:
        'مبلغ تراکنش بدون واحد پول (ریال/تومان) درج شده یا پیامک ناقص است؛ جهت جلوگیری از خطای محاسباتی، نیاز به بررسی دستی دارد.',
      messageFingerprint: fingerprint,
      isDuplicate: isDuplicateByFingerprint,
      canAutoRegister: false,
      rawNormalized: normalized,
    };
  }

  // Stage 9: Unsupported Format for the Selected Bank
  return {
    status: 'UNSUPPORTED_FORMAT',
    messageCategory: 'UNKNOWN',
    transactionKind: 'UNKNOWN',
    bankId: profile.id,
    bankName: profile.bankName,
    senderId: options.senderId,
    senderMatched,
    currency: 'UNKNOWN',
    balanceInTomans,
    merchant: '',
    reason: `ساختار این پیامک با هیچ‌یک از قالب‌های تعریف‌شده برای «${profile.bankName}» مطابقت ندارد.`,
    messageFingerprint: fingerprint,
    isDuplicate: isDuplicateByFingerprint,
    canAutoRegister: false,
    rawNormalized: normalized,
  };
}

/**
 * Explicit Sender Policy Evaluator
 *
 * Policy Specification:
 * 1. When `senderId` is provided:
 *    - If `profile.senders` has registered senders, `senderId` MUST match one of them (case-insensitive).
 *    - Any mismatch results in `senderMatched: false` and blocks automatic registration (`NEEDS_REVIEW`).
 * 2. When `senderId` is omitted/empty:
 *    - If `profile.requireSenderMatch === true`, sender verification is mandatory. Missing `senderId`
 *      (or empty `profile.senders`) results in `senderMatched: false` and blocks automatic registration
 *      (unless `allowUnverifiedPreview` is set during template authoring preview).
 *    - If `profile.requireSenderMatch === false`, omitting `senderId` is explicitly allowed (`OPTIONAL_OMITTED`)
 *      to support manual SMS text paste when the user did not copy the sender header; matching then relies on
 *      the bank's structural header anchors and verified template rules.
 */
export interface SenderPolicyEvaluation {
  senderMatched: boolean;
  policyMode:
    | 'MATCHED_REGISTERED_SENDER'
    | 'MISMATCHED_SENDER'
    | 'MISSING_REQUIRED_SENDER'
    | 'OPTIONAL_OMITTED';
  reason?: string;
}

export function evaluateSenderPolicy(
  profile: BankProfile,
  senderId?: string,
  allowUnverifiedPreview: boolean = false
): SenderPolicyEvaluation {
  const senderClean = (senderId || '').trim().toLowerCase();

  if (senderClean) {
    if (profile.senders.length > 0) {
      const matched = profile.senders.some(
        (s) => s.trim().toLowerCase() === senderClean
      );
      if (!matched) {
        return {
          senderMatched: false,
          policyMode: 'MISMATCHED_SENDER',
          reason: `شناسه فرستنده (${senderId}) با فرستنده‌های تعریف‌شده برای «${profile.bankName}» همخوانی ندارد.`,
        };
      }
      return {
        senderMatched: true,
        policyMode: 'MATCHED_REGISTERED_SENDER',
      };
    }

    // If profile requires sender match but has no registered senders yet, cannot match
    if (profile.requireSenderMatch && !allowUnverifiedPreview) {
      return {
        senderMatched: false,
        policyMode: 'MISSING_REQUIRED_SENDER',
        reason: `برای پروفایل «${profile.bankName}» تطبیق فرستنده الزامی است اما هنوز سرشماره مجازی در پروفایل ثبت نشده است.`,
      };
    }

    return {
      senderMatched: true,
      policyMode: 'MATCHED_REGISTERED_SENDER',
    };
  }

  // senderId is empty / omitted
  if (profile.requireSenderMatch && !allowUnverifiedPreview) {
    return {
      senderMatched: false,
      policyMode: 'MISSING_REQUIRED_SENDER',
      reason: `برای پروفایل «${profile.bankName}» تطبیق شناسه فرستنده الزامی است اما فرستنده مشخص نشده است.`,
    };
  }

  return {
    senderMatched: true,
    policyMode: 'OPTIONAL_OMITTED',
  };
}

/**
 * Template-Level Activation & Version Guard
 *
 * A template rule is eligible for automatic registration ONLY IF:
 * 1. The parent bank profile is in `'ACTIVE'` status.
 * 2. The template itself has not been explicitly marked unverified (`verifiedByUser !== false`).
 * 3. If `template.verifiedVersion` is set, it equals `template.version`.
 * 4. There is at least one user-verified `VALID_TRANSACTION` sample in `profile.samples`
 *    whose `matchedTemplateId === template.id` AND whose `(matchedTemplateVersion ?? 1) === template.version`.
 */
export function isTemplateVerifiedForAutoRegister(
  profile: BankProfile,
  template: BankSmsTemplateRule
): boolean {
  if (profile.status !== 'ACTIVE') {
    return false;
  }
  if (template.verifiedByUser === false) {
    return false;
  }
  if (
    template.verifiedVersion !== undefined &&
    template.verifiedVersion !== template.version
  ) {
    return false;
  }
  return profile.samples.some(
    (s) =>
      s.userVerified === true &&
      s.parseStatus === 'VALID_TRANSACTION' &&
      s.matchedTemplateId === template.id &&
      (s.matchedTemplateVersion ?? 1) === template.version
  );
}

/**
 * Strictly maps a verified SmsTransactionKind to FinancialEntryType.
 * NEVER defaults `'UNKNOWN'` or `'FAILED'` to `'expense'`.
 */
export function resolveFinancialTypeFromKind(
  kind: SmsTransactionKind | undefined,
  rawNormalized: string = ''
): FinancialEntryType | undefined {
  switch (kind) {
    case 'DEPOSIT':
    case 'REFUND':
      return 'income';
    case 'WITHDRAWAL':
    case 'PURCHASE':
    case 'FEE':
      return 'expense';
    case 'TRANSFER':
      return rawNormalized.includes('بین حساب های خود') ? 'transfer' : 'expense';
    case 'UNKNOWN':
    case 'FAILED':
    default:
      return undefined;
  }
}

export interface ManualReviewOverrides {
  bankId?: string;
  bankName?: string;
  transactionKind?: SmsTransactionKind;
  currency?: CurrencyUnit;
  amountOriginal?: number;
  merchant?: string;
}

export interface ValidatedFinancialEntryResult {
  ok: boolean;
  error?: string;
  entry?: Omit<ExpenseItem, 'id'>;
}

/**
 * Validates an SMS analysis result (including `NEEDS_REVIEW` items with optional user corrections)
 * before creating a financial record.
 *
 * Guarantees:
 * - Duplicate messages are blocked.
 * - Non-transaction messages (`NOT_A_TRANSACTION`) are blocked.
 * - Missing bank (`bankId` / `bankName`) is blocked.
 * - `UNKNOWN` or `FAILED` transaction kind is NEVER defaulted to `'expense'` and is blocked.
 * - `UNKNOWN` currency unit is blocked.
 * - Zero, negative, or NaN amount is blocked.
 */
export function buildValidatedExpenseEntry(
  res: StructuredSmsAnalysisResult | null | undefined,
  overrides?: ManualReviewOverrides,
  profiles?: BankProfile[],
  existingExpenses?: ExpenseItem[]
): ValidatedFinancialEntryResult {
  if (!res) {
    return { ok: false, error: 'نتیجه تحلیل پیامک موجود نیست.' };
  }

  if (res.isDuplicate) {
    return {
      ok: false,
      error: 'این پیامک تکراری است و قبلاً ثبت شده است.',
    };
  }

  if (
    existingExpenses &&
    res.messageFingerprint &&
    existingExpenses.some(
      (e) => e.messageFingerprint && e.messageFingerprint === res.messageFingerprint
    )
  ) {
    return {
      ok: false,
      error: 'این پیامک تکراری است (اثرانگشت پیام قبلاً ثبت شده است).',
    };
  }

  if (res.status === 'NOT_A_TRANSACTION') {
    return {
      ok: false,
      error: 'این پیامک غیرتراکنشی است و قابل ثبت به‌عنوان تراکنش مالی نیست.',
    };
  }

  const resolvedBankId = (overrides?.bankId ?? res.bankId ?? '').trim();
  const matchedProfile = profiles?.find((p) => p.id === resolvedBankId);
  const resolvedBankName = (
    overrides?.bankName ??
    matchedProfile?.bankName ??
    res.bankName ??
    ''
  ).trim();

  if (!resolvedBankId || !resolvedBankName) {
    return {
      ok: false,
      error: 'بانک مشخص نیست. لطفاً پیش از تأیید، بانک را به‌طور صریح انتخاب کنید یا پیام را رد کنید.',
    };
  }

  const resolvedKind: SmsTransactionKind =
    overrides?.transactionKind ?? res.transactionKind;

  if (!resolvedKind || resolvedKind === 'UNKNOWN' || resolvedKind === 'FAILED') {
    return {
      ok: false,
      error:
        'نوع تراکنش نامشخص است؛ تراکنش با نوع UNKNOWN هرگز به‌طور پیش‌فرض هزینه ثبت نمی‌شود. لطفاً نوع تراکنش (برداشت/واریز/خرید/انتقال) را مشخص یا پیام را رد کنید.',
    };
  }

  const financialType = resolveFinancialTypeFromKind(
    resolvedKind,
    res.rawNormalized
  );
  if (!financialType) {
    return {
      ok: false,
      error: 'نوع تراکنش برای ثبت مالی معتبر نیست.',
    };
  }

  const resolvedCurrency: CurrencyUnit = overrides?.currency ?? res.currency;
  if (resolvedCurrency !== 'RIAL' && resolvedCurrency !== 'TOMAN') {
    return {
      ok: false,
      error:
        'واحد پول (ریال یا تومان) نامشخص است. لطفاً واحد پول را تعیین یا پیام را رد کنید.',
    };
  }

  const hasAmountOrCurrencyOverride =
    overrides?.amountOriginal !== undefined || overrides?.currency !== undefined;

  let amountInTomans: number | undefined;
  if (hasAmountOrCurrencyOverride) {
    const rawAmt = overrides?.amountOriginal ?? res.amountOriginal;
    if (rawAmt === undefined || !Number.isFinite(rawAmt) || rawAmt <= 0) {
      return {
        ok: false,
        error: 'مبلغ تراکنش نامعتبر است. لطفاً مبلغ صحیح را وارد کنید.',
      };
    }
    amountInTomans =
      resolvedCurrency === 'RIAL' ? Math.floor(rawAmt / 10) : Math.floor(rawAmt);
  } else {
    amountInTomans =
      res.amountInTomans ??
      (res.amountOriginal && Number.isFinite(res.amountOriginal)
        ? resolvedCurrency === 'RIAL'
          ? Math.floor(res.amountOriginal / 10)
          : Math.floor(res.amountOriginal)
        : undefined);
  }

  if (
    amountInTomans === undefined ||
    !Number.isFinite(amountInTomans) ||
    amountInTomans <= 0
  ) {
    return {
      ok: false,
      error: 'مبلغ نهایی تراکنش (به تومان) باید عددی بزرگ‌تر از صفر باشد.',
    };
  }

  // Secondary duplicate check by bankId + trackingCode + amountInTomans (catches manual review completions)
  if (
    existingExpenses &&
    res.trackingCode &&
    existingExpenses.some(
      (e) =>
        e.bankId === resolvedBankId &&
        e.trackingCode === res.trackingCode &&
        e.amount === amountInTomans
    )
  ) {
    return {
      ok: false,
      error: 'تراکنشی با همین بانک، کد پیگیری و مبلغ قبلاً ثبت شده است (تکراری).',
    };
  }

  const resolvedTitle =
    (overrides?.merchant ?? res.merchant ?? '').trim() ||
    buildMerchantTitle(resolvedKind, res.rawNormalized);

  return {
    ok: true,
    entry: {
      title: resolvedTitle,
      amount: amountInTomans,
      category:
        financialType === 'income'
          ? 'واریز / درآمد بانکی'
          : financialType === 'transfer'
          ? 'انتقال بین حساب‌ها'
          : 'بانکی / پیامک',
      date: '۱۴۰۵/۰۸/۰۵',
      type: financialType,
      isSmsParsed: true,
      bankId: resolvedBankId,
      bankName: resolvedBankName,
      trackingCode: res.trackingCode,
      balanceAfter: res.balanceInTomans,
      messageFingerprint: res.messageFingerprint,
      templateId: res.matchedTemplateId,
    },
  };
}

function buildMerchantTitle(kind: SmsTransactionKind, normalizedText: string): string {
  switch (kind) {
    case 'PURCHASE':
      if (normalizedText.includes('پایانه')) return 'خرید از پایانه فروشگاهی';
      return 'خرید فروشگاهی / اینترنتی';
    case 'TRANSFER':
      if (normalizedText.includes('انتقال پل') || normalizedText.includes('پل')) {
        return 'انتقال وجه (سامانه پل)';
      }
      return 'انتقال وجه بانکی';
    case 'DEPOSIT':
      if (normalizedText.includes('ویپاد')) return 'واریز از ویپاد';
      return 'واریز به حساب';
    case 'FEE':
      return 'کارمزد بانکی';
    case 'REFUND':
      return 'برگشت وجه به حساب';
    case 'WITHDRAWAL':
      return 'برداشت وجه از حساب';
    default:
      return 'تراکنش بانکی';
  }
}

export function translateProfileStatus(status: BankProfileStatus): string {
  switch (status) {
    case 'DRAFT':
      return 'پیش‌نویس';
    case 'NEEDS_SAMPLES':
      return 'نیازمند نمونه';
    case 'NEEDS_VERIFICATION':
      return 'نیازمند تأیید نمونه‌ها';
    case 'ACTIVE':
      return 'فعال (آماده ثبت خودکار)';
    case 'NEEDS_REVISION':
      return 'نیازمند بازبینی مجدد';
  }
}

export function translateParseStatus(status: SmsParseStatus): string {
  switch (status) {
    case 'VALID_TRANSACTION':
      return 'تراکنش معتبر و منطبق با قالب';
    case 'NOT_A_TRANSACTION':
      return 'غیرتراکنشی / ناموفق (رد شده)';
    case 'NEEDS_REVIEW':
      return 'نیازمند بررسی و تأیید دستی';
    case 'UNSUPPORTED_FORMAT':
      return 'قالب پشتیبانی‌نشده / ناشناخته';
  }
}

export function translateTransactionKind(kind: SmsTransactionKind): string {
  switch (kind) {
    case 'DEPOSIT':
      return 'واریز';
    case 'WITHDRAWAL':
      return 'برداشت';
    case 'PURCHASE':
      return 'خرید / پرداخت';
    case 'TRANSFER':
      return 'انتقال وجه';
    case 'FEE':
      return 'کارمزد';
    case 'REFUND':
      return 'برگشت وجه';
    case 'FAILED':
      return 'تراکنش ناموفق';
    case 'UNKNOWN':
      return 'نامشخص';
  }
}

// ============================================================================
// 4. Profile Persistence & Lifecycle Management ("مدیریت بانک‌های من")
// ============================================================================

export const BANK_PROFILES_STORAGE_KEY = 'life_os_bank_profiles_v1';

/**
 * Sanitizes and enforces template-version & sample-verification invariants
 * whenever profiles are loaded from storage (e.g. after app reload).
 */
export function sanitizeLoadedBankProfiles(rawProfiles: BankProfile[]): BankProfile[] {
  return rawProfiles
    .filter((p) => p && typeof p.id === 'string' && typeof p.bankName === 'string')
    .map((p) => {
      const safeProfileVersion =
        typeof p.version === 'number' && Number.isFinite(p.version) && p.version >= 1
          ? Math.floor(p.version)
          : 1;
      const safeSenders = Array.isArray(p.senders)
        ? p.senders.filter((s) => typeof s === 'string' && s.trim().length > 0)
        : [];
      const rawTemplates = Array.isArray(p.templates) ? p.templates : [];
      const rawSamples = Array.isArray(p.samples) ? p.samples : [];

      const normalizedTemplates: BankSmsTemplateRule[] = rawTemplates.map((tpl) => ({
        ...tpl,
        version:
          typeof tpl.version === 'number' &&
          Number.isFinite(tpl.version) &&
          tpl.version >= 1
            ? Math.floor(tpl.version)
            : 1,
      }));

      const normalizedSamples: BankSmsSampleRecord[] = rawSamples.map((s) => {
        const matchedTpl = normalizedTemplates.find(
          (t) => t.id === s.matchedTemplateId
        );
        const sampleTplVersion =
          typeof s.matchedTemplateVersion === 'number' &&
          Number.isFinite(s.matchedTemplateVersion) &&
          s.matchedTemplateVersion >= 1
            ? Math.floor(s.matchedTemplateVersion)
            : 1;

        const isVersionMatch =
          Boolean(matchedTpl) &&
          matchedTpl!.version === sampleTplVersion &&
          matchedTpl!.verifiedByUser !== false &&
          (matchedTpl!.verifiedVersion === undefined ||
            matchedTpl!.verifiedVersion === matchedTpl!.version);

        const isSenderValidForSample = Boolean(p.requireSenderMatch)
          ? Boolean(s.senderId && s.senderId.trim()) &&
            safeSenders.some(
              (reg) => reg.trim().toLowerCase() === s.senderId!.trim().toLowerCase()
            )
          : !s.senderId ||
            !s.senderId.trim() ||
            safeSenders.length === 0 ||
            safeSenders.some(
              (reg) => reg.trim().toLowerCase() === s.senderId!.trim().toLowerCase()
            );

        const validUserVerified =
          Boolean(s.userVerified) &&
          s.parseStatus === 'VALID_TRANSACTION' &&
          isVersionMatch &&
          isSenderValidForSample;

        return {
          ...s,
          matchedTemplateVersion: sampleTplVersion,
          userVerified: validUserVerified,
          verifiedAt: validUserVerified ? s.verifiedAt : undefined,
        };
      });

      const syncedTemplates: BankSmsTemplateRule[] = normalizedTemplates.map((tpl) => {
        const hasVerifiedSample = normalizedSamples.some(
          (s) =>
            s.userVerified === true &&
            s.parseStatus === 'VALID_TRANSACTION' &&
            s.matchedTemplateId === tpl.id &&
            (s.matchedTemplateVersion ?? 1) === tpl.version
        );
        return {
          ...tpl,
          verifiedByUser: hasVerifiedSample,
          verifiedVersion: hasVerifiedSample ? tpl.version : undefined,
        };
      });

      const hasAnyVerifiedTemplate = syncedTemplates.some(
        (tpl) => tpl.verifiedByUser === true && tpl.verifiedVersion === tpl.version
      );

      let safeStatus: BankProfileStatus = p.status || 'NEEDS_VERIFICATION';
      if (safeStatus === 'ACTIVE' && !hasAnyVerifiedTemplate) {
        safeStatus =
          normalizedSamples.length > 0 ? 'NEEDS_VERIFICATION' : 'NEEDS_SAMPLES';
      }

      return {
        ...p,
        version: safeProfileVersion,
        senders: safeSenders,
        requireSenderMatch: Boolean(p.requireSenderMatch),
        templates: syncedTemplates,
        samples: normalizedSamples,
        status: safeStatus,
      };
    });
}

export function loadBankProfiles(storage?: Pick<Storage, 'getItem'>): BankProfile[] {
  try {
    const store =
      storage || (typeof localStorage !== 'undefined' ? localStorage : undefined);
    if (store) {
      const raw = store.getItem(BANK_PROFILES_STORAGE_KEY);
      if (raw) {
        const parsed = JSON.parse(raw);
        if (Array.isArray(parsed) && parsed.length > 0) {
          const sanitized = sanitizeLoadedBankProfiles(parsed);
          if (sanitized.length > 0) {
            return sanitized;
          }
        }
      }
    }
  } catch {
    // Fallback to defaults if storage is unavailable or corrupted
  }
  return createDefaultBankProfiles();
}

export function saveBankProfiles(
  profiles: BankProfile[],
  storage?: Pick<Storage, 'setItem'>
): void {
  try {
    const store =
      storage || (typeof localStorage !== 'undefined' ? localStorage : undefined);
    if (store) {
      store.setItem(BANK_PROFILES_STORAGE_KEY, JSON.stringify(profiles));
    }
  } catch {
    // Ignore storage quota errors
  }
}

/**
 * Evaluates a sample SMS against a specific BankProfile and produces a BankSmsSampleRecord.
 */
export function evaluateSampleForProfile(
  profile: BankProfile,
  rawText: string,
  senderId?: string,
  isSynthetic: boolean = false
): BankSmsSampleRecord {
  const analysis = analyzeBankSms(rawText, {
    selectedBankId: profile.id,
    senderId: senderId || profile.senders[0],
    profiles: [profile],
    allowUnverifiedPreview: true,
  });

  return {
    id: 'smp_' + Date.now() + '_' + Math.random().toString(36).slice(2, 6),
    rawText,
    senderId,
    isSynthetic,
    parseStatus: analysis.status,
    messageCategory: analysis.messageCategory,
    detectedKind: analysis.transactionKind,
    extractedAmountOriginal: analysis.amountOriginal,
    extractedCurrency: analysis.currency,
    extractedAmountTomans: analysis.amountInTomans,
    extractedBalanceTomans: analysis.balanceInTomans,
    extractedTrackingCode: analysis.trackingCode,
    matchedTemplateId: analysis.matchedTemplateId,
    matchedTemplateVersion: analysis.matchedTemplateVersion,
    reason: analysis.reason,
    userVerified: false,
  };
}

/**
 * Updates a specific template rule in a bank profile, increments that template's version,
 * invalidates any previous verification for that template, and revalidates the profile.
 */
export function updateBankTemplateRule(
  profile: BankProfile,
  templateId: string,
  patch: Partial<Omit<BankSmsTemplateRule, 'id' | 'version'>>
): BankProfile {
  const updatedTemplates = profile.templates.map((tpl) => {
    if (tpl.id !== templateId) return tpl;
    return {
      ...tpl,
      ...patch,
      version: tpl.version + 1,
      verifiedByUser: false,
      verifiedVersion: undefined,
    };
  });

  return revalidateBankProfile({
    ...profile,
    templates: updatedTemplates,
  });
}

/**
 * Re-runs all samples in a profile when its templates, senders, or sender policy change.
 * Increments profile version and invalidates prior verification for any sample whose
 * matched template was removed, changed version, failed sender policy, or produced different extraction results.
 */
export function revalidateBankProfile(profile: BankProfile): BankProfile {
  const nextVersion = profile.version + 1;
  let regressionDetected = false;

  const updatedSamples = profile.samples.map((sample) => {
    const effectiveSampleSender =
      sample.senderId !== undefined
        ? sample.senderId
        : profile.requireSenderMatch
        ? undefined
        : profile.senders[0];

    const senderCheck = evaluateSenderPolicy(
      profile,
      effectiveSampleSender,
      false
    );

    const reEval = analyzeBankSms(sample.rawText, {
      selectedBankId: profile.id,
      senderId: effectiveSampleSender,
      profiles: [{ ...profile, version: nextVersion }],
      allowUnverifiedPreview: true,
    });

    const matchedTpl = profile.templates.find(
      (t) => t.id === reEval.matchedTemplateId
    );
    const previousTemplateVersion = sample.matchedTemplateVersion ?? 1;
    const currentTemplateVersion = matchedTpl?.version ?? reEval.matchedTemplateVersion;

    const templateVersionUnchanged =
      Boolean(matchedTpl) &&
      reEval.matchedTemplateId === sample.matchedTemplateId &&
      currentTemplateVersion === previousTemplateVersion;

    const consistentWithPrevious =
      senderCheck.senderMatched &&
      templateVersionUnchanged &&
      reEval.status === sample.parseStatus &&
      reEval.transactionKind === sample.detectedKind &&
      reEval.amountInTomans === sample.extractedAmountTomans;

    if (sample.userVerified && !consistentWithPrevious) {
      regressionDetected = true;
    }

    const nextUserVerified = consistentWithPrevious ? sample.userVerified : false;

    return {
      ...sample,
      parseStatus: reEval.status,
      messageCategory: reEval.messageCategory,
      detectedKind: reEval.transactionKind,
      extractedAmountOriginal: reEval.amountOriginal,
      extractedCurrency: reEval.currency,
      extractedAmountTomans: reEval.amountInTomans,
      extractedBalanceTomans: reEval.balanceInTomans,
      extractedTrackingCode: reEval.trackingCode,
      matchedTemplateId: reEval.matchedTemplateId,
      matchedTemplateVersion: currentTemplateVersion,
      reason: senderCheck.senderMatched
        ? reEval.reason
        : senderCheck.reason || reEval.reason,
      userVerified: nextUserVerified,
      verifiedAt: nextUserVerified ? sample.verifiedAt : undefined,
    };
  });

  // Update template-level verified metadata to stay in sync with verified samples
  const syncedTemplates = profile.templates.map((tpl) => {
    const hasVerifiedSampleForCurrentVersion = updatedSamples.some(
      (s) =>
        s.userVerified &&
        s.parseStatus === 'VALID_TRANSACTION' &&
        s.matchedTemplateId === tpl.id &&
        (s.matchedTemplateVersion ?? 1) === tpl.version
    );
    return {
      ...tpl,
      verifiedByUser: hasVerifiedSampleForCurrentVersion,
      verifiedVersion: hasVerifiedSampleForCurrentVersion ? tpl.version : undefined,
    };
  });

  const hasVerifiedValidSample = updatedSamples.some(
    (s) => s.userVerified && s.parseStatus === 'VALID_TRANSACTION'
  );

  let nextStatus: BankProfileStatus = profile.status;
  if (regressionDetected) {
    nextStatus = 'NEEDS_REVISION';
  } else if (updatedSamples.length === 0) {
    nextStatus = 'NEEDS_SAMPLES';
  } else if (!hasVerifiedValidSample) {
    nextStatus = 'NEEDS_VERIFICATION';
  }

  return {
    ...profile,
    version: nextVersion,
    templates: syncedTemplates,
    samples: updatedSamples,
    status: nextStatus,
    updatedAt: getPersianDateString(),
  };
}

// ============================================================================
// 5. Legacy Compatibility Wrapper (Strictly Gated)
// ============================================================================

export interface ParsedSmsResult {
  amount: number;
  merchant: string;
  bankName: string;
  type: 'withdrawal' | 'deposit';
  trackingCode?: string;
  balance?: number;
  status: SmsParseStatus;
  reason: string;
  raw: string;
}

/**
 * Safe wrapper around analyzeBankSms.
 * Returns null unless the message is a VALID_TRANSACTION under an explicit bank/sender match.
 */
export function parseBankSms(
  smsText: string,
  options: AnalyzeBankSmsOptions = {}
): ParsedSmsResult | null {
  const res = analyzeBankSms(smsText, options);
  if (res.status !== 'VALID_TRANSACTION' || !res.amountInTomans || !res.bankName) {
    return null;
  }
  if (res.transactionKind === 'UNKNOWN' || res.transactionKind === 'FAILED') {
    return null;
  }

  return {
    amount: res.amountInTomans,
    merchant: res.merchant,
    bankName: res.bankName,
    type:
      res.transactionKind === 'DEPOSIT' || res.transactionKind === 'REFUND'
        ? 'deposit'
        : 'withdrawal',
    trackingCode: res.trackingCode,
    balance: res.balanceInTomans,
    status: res.status,
    reason: res.reason,
    raw: smsText,
  };
}

export function getPersianDateString(): string {
  return '۱۴۰۵/۰۸/۰۵ (۰۵ آبان)';
}
