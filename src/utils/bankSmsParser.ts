// Iranian Bank SMS Parsing Engine
// Deterministic on-device parsing for Iranian Bank SMS messages
// Ported from com.example.sms.* (BankSmsParser.kt, BankSmsPatternRegistry.kt, BankSmsNormalizer.kt)

import { BankSmsParseResult } from '../types/lifeOs';
import { toEnglishDigits } from './persianDateUtils';

export interface BankPatternRule {
  templateId: string;
  transactionType: 'INCOME' | 'EXPENSE' | 'TRANSFER' | 'FEE';
  regex: RegExp;
  defaultCategory: string;
  isRial: boolean;
  confidence: number;
}

export interface BankDefinition {
  bankCode: string;
  bankName: string;
  knownSenders: string[];
  bodyKeywords: string[];
  patterns: BankPatternRule[];
}

export const OTP_KEYWORDS = [
  'رمز پویا', 'رمز دوم', 'کد تایید', 'کد ورود', 'کد فعالسازی',
  'کد امنیتی', 'کد یکبار مصرف', 'otp', 'verification code'
];

export const FAILED_KEYWORDS = [
  'تراکنش ناموفق', 'عدم موفقیت', 'تراکنش نامعتبر', 'موجودی ناکافی',
  'خطای تراکنش', 'ناموفق', 'لغو تراکنش'
];

export const PROMOTIONAL_KEYWORDS = [
  'جشنواره', 'تسهیلات ویژه', 'وام فوری', 'قرعه کشی', 'افتتاح حساب آنلاین',
  'کارت هدیه', 'امتیاز باشگاه'
];

export const BANKS: BankDefinition[] = [
  {
    bankCode: 'MELLAT',
    bankName: 'بانک ملت',
    knownSenders: ['mellat', 'bankmellat', '981000', '+981000', '982000', '+982000'],
    bodyKeywords: ['بانک ملت', 'ملت'],
    patterns: [
      {
        templateId: 'MELLAT_WITHDRAWAL_V1',
        transactionType: 'EXPENSE',
        regex: /برداشت(?:\s+مبلغ)?\s*[:\s]*([0-9,،\.]+)\s*(ریال|تومان)?/,
        defaultCategory: 'خرید و خدمات',
        isRial: true,
        confidence: 0.98
      },
      {
        templateId: 'MELLAT_DEPOSIT_V1',
        transactionType: 'INCOME',
        regex: /واریز(?:\s+مبلغ)?\s*[:\s]*([0-9,،\.]+)\s*(ریال|تومان)?/,
        defaultCategory: 'درآمد و حقوق',
        isRial: true,
        confidence: 0.98
      },
      {
        templateId: 'MELLAT_POS_V1',
        transactionType: 'EXPENSE',
        regex: /خرید(?:\s+با\s+کارت)?(?:\s+[0-9\*]+)?\s*[\n\r]*\s*مبلغ\s*[:\s]*([0-9,،\.]+)\s*(ریال|تومان)?/,
        defaultCategory: 'خرید',
        isRial: true,
        confidence: 0.98
      }
    ]
  },
  {
    bankCode: 'MELLI',
    bankName: 'بانک ملی ایران',
    knownSenders: ['melli', 'bankmelli', 'bmi', '982000', '+982000'],
    bodyKeywords: ['بانک ملی', 'ملی ایران', 'ملی'],
    patterns: [
      {
        templateId: 'MELLI_WITHDRAWAL_V1',
        transactionType: 'EXPENSE',
        regex: /(?:برداشت|خرید)(?:\s+مبلغ)?\s*[:\s]*([0-9,،\.]+)\s*(ریال|تومان)?/,
        defaultCategory: 'خرید',
        isRial: true,
        confidence: 0.98
      },
      {
        templateId: 'MELLI_DEPOSIT_V1',
        transactionType: 'INCOME',
        regex: /واریز(?:\s+مبلغ)?\s*[:\s]*([0-9,،\.]+)\s*(ریال|تومان)?/,
        defaultCategory: 'درآمد',
        isRial: true,
        confidence: 0.98
      },
      {
        templateId: 'MELLI_TRANSFER_V1',
        transactionType: 'TRANSFER',
        regex: /انتقال(?:\s+وجه)?\s*[:\s]*([0-9,،\.]+)\s*(ریال|تومان)?/,
        defaultCategory: 'انتقال بین حسابی',
        isRial: true,
        confidence: 0.95
      }
    ]
  },
  {
    bankCode: 'BLUBANK',
    bankName: 'بلوبانک سامان',
    knownSenders: ['blubank', 'bluebank', 'blu', '989000', '+989000'],
    bodyKeywords: ['بلوبانک', 'بلو بانک', 'blubank', 'بلو'],
    patterns: [
      {
        templateId: 'BLU_EXPENSE_V1',
        transactionType: 'EXPENSE',
        regex: /برداشت\s+([0-9,،\.]+)\s*(ریال|تومان)?(?:\s+بابت\s+([^\n]+))?/,
        defaultCategory: 'خرید روزمره',
        isRial: false, // BluBank usually sends in Tomans
        confidence: 0.98
      },
      {
        templateId: 'BLU_INCOME_V1',
        transactionType: 'INCOME',
        regex: /واریز\s+([0-9,،\.]+)\s*(ریال|تومان)?(?:\s+بابت\s+([^\n]+))?/,
        defaultCategory: 'درآمد',
        isRial: false,
        confidence: 0.98
      },
      {
        templateId: 'BLU_TRANSFER_V1',
        transactionType: 'TRANSFER',
        regex: /انتقال(?:\s+وجه)?\s+([0-9,،\.]+)\s*(ریال|تومان)?/,
        defaultCategory: 'انتقال حساب',
        isRial: false,
        confidence: 0.98
      }
    ]
  },
  {
    bankCode: 'SAMAN',
    bankName: 'بانک سامان',
    knownSenders: ['saman', 'banksaman', 'sb'],
    bodyKeywords: ['بانک سامان', 'سامان'],
    patterns: [
      {
        templateId: 'SAMAN_DEPOSIT_V1',
        transactionType: 'INCOME',
        regex: /واریز(?:\s+به\s+حساب)?[\n\r\s]*مبلغ\s*[:\s]*([0-9,،\.]+)\s*(ریال|تومان)?/,
        defaultCategory: 'درآمد',
        isRial: true,
        confidence: 0.96
      },
      {
        templateId: 'SAMAN_WITHDRAWAL_V1',
        transactionType: 'EXPENSE',
        regex: /برداشت(?:\s+از\s+حساب)?[\n\r\s]*مبلغ\s*[:\s]*([0-9,،\.]+)\s*(ریال|تومان)?/,
        defaultCategory: 'خرید',
        isRial: true,
        confidence: 0.96
      }
    ]
  },
  {
    bankCode: 'PASARGAD',
    bankName: 'بانک پاسارگاد',
    knownSenders: ['pasargad', 'bpi', '9810002010'],
    bodyKeywords: ['بانک پاسارگاد', 'پاسارگاد'],
    patterns: [
      {
        templateId: 'PASARGAD_WITHDRAWAL_V1',
        transactionType: 'EXPENSE',
        regex: /برداشت(?:\s+مبلغ)?\s*[:\s]*([0-9,،\.]+)\s*(ریال|تومان)?/,
        defaultCategory: 'خرید و خدمات',
        isRial: true,
        confidence: 0.96
      },
      {
        templateId: 'PASARGAD_DEPOSIT_V1',
        transactionType: 'INCOME',
        regex: /واریز(?:\s+مبلغ)?\s*[:\s]*([0-9,،\.]+)\s*(ریال|تومان)?/,
        defaultCategory: 'درآمد',
        isRial: true,
        confidence: 0.96
      }
    ]
  },
  {
    bankCode: 'TEJARAT',
    bankName: 'بانک تجارت',
    knownSenders: ['tejarat', 'tejaratbank'],
    bodyKeywords: ['بانک تجارت', 'تجارت'],
    patterns: [
      {
        templateId: 'TEJARAT_WITHDRAWAL_V1',
        transactionType: 'EXPENSE',
        regex: /(?:برداشت|خرید)\s*[:\s]*([0-9,،\.]+)\s*(ریال|تومان)?/,
        defaultCategory: 'خرید',
        isRial: true,
        confidence: 0.95
      },
      {
        templateId: 'TEJARAT_DEPOSIT_V1',
        transactionType: 'INCOME',
        regex: /واریز\s*[:\s]*([0-9,،\.]+)\s*(ریال|تومان)?/,
        defaultCategory: 'درآمد',
        isRial: true,
        confidence: 0.95
      }
    ]
  },
  {
    bankCode: 'SEPAH',
    bankName: 'بانک سپه',
    knownSenders: ['sepah', 'banksepah'],
    bodyKeywords: ['بانک سپه', 'سپه', 'انصار', 'کوثر', 'قوامین'],
    patterns: [
      {
        templateId: 'SEPAH_WITHDRAWAL_V1',
        transactionType: 'EXPENSE',
        regex: /(?:برداشت|خرید)\s*[:\s]*([0-9,،\.]+)\s*(ریال|تومان)?/,
        defaultCategory: 'خرید',
        isRial: true,
        confidence: 0.95
      },
      {
        templateId: 'SEPAH_DEPOSIT_V1',
        transactionType: 'INCOME',
        regex: /واریز\s*[:\s]*([0-9,،\.]+)\s*(ریال|تومان)?/,
        defaultCategory: 'درآمد',
        isRial: true,
        confidence: 0.95
      }
    ]
  },
  {
    bankCode: 'SADERAT',
    bankName: 'بانک صادرات ایران',
    knownSenders: ['bsi', 'saderat'],
    bodyKeywords: ['بانک صادرات', 'صادرات'],
    patterns: [
      {
        templateId: 'SADERAT_EXPENSE_V1',
        transactionType: 'EXPENSE',
        regex: /(?:برداشت|خرید|کسر)\s*[:\s]*([0-9,،\.]+)\s*(ریال|تومان)?/,
        defaultCategory: 'خرید',
        isRial: true,
        confidence: 0.95
      },
      {
        templateId: 'SADERAT_DEPOSIT_V1',
        transactionType: 'INCOME',
        regex: /واریز\s*[:\s]*([0-9,،\.]+)\s*(ریال|تومان)?/,
        defaultCategory: 'درآمد',
        isRial: true,
        confidence: 0.95
      }
    ]
  },
  {
    bankCode: 'RESALAT',
    bankName: 'بانک قرض‌الحسنه رسالت',
    knownSenders: ['resalat', 'rqbank'],
    bodyKeywords: ['بانک رسالت', 'رسالت', 'قرض الحسنه رسالت'],
    patterns: [
      {
        templateId: 'RESALAT_EXPENSE_V1',
        transactionType: 'EXPENSE',
        regex: /برداشت\s*[:\s]*([0-9,،\.]+)\s*(ریال|تومان)?/,
        defaultCategory: 'خرید',
        isRial: true,
        confidence: 0.95
      },
      {
        templateId: 'RESALAT_DEPOSIT_V1',
        transactionType: 'INCOME',
        regex: /واریز\s*[:\s]*([0-9,،\.]+)\s*(ریال|تومان)?/,
        defaultCategory: 'درآمد',
        isRial: true,
        confidence: 0.95
      }
    ]
  }
];

export class BankSmsParser {
  static normalize(raw: string): string {
    const englishDigits = toEnglishDigits(raw);
    return englishDigits
      .replace(/[\u200B-\u200D\uFEFF]/g, '')
      .replace(/[\t\r]/g, ' ')
      .replace(/\s+/g, ' ')
      .trim();
  }

  static findBank(sender: string, text: string): BankDefinition | null {
    const lowerSender = sender.toLowerCase();
    const lowerText = text.toLowerCase();

    for (const bank of BANKS) {
      if (lowerSender && bank.knownSenders.some(s => lowerSender.includes(s))) {
        return bank;
      }
      if (bank.bodyKeywords.some(k => lowerText.includes(k))) {
        return bank;
      }
    }
    return null;
  }

  static computeFingerprint(
    bankCode: string,
    transactionType: string,
    amountToman: number,
    cardOrAccountMask: string,
    trackingCode: string,
    timeBucket: string
  ): string {
    const combined = `${bankCode}_${transactionType}_${amountToman}_${cardOrAccountMask}_${trackingCode}_${timeBucket}`;
    // Simple deterministic hash
    let hash = 0;
    for (let i = 0; i < combined.length; i++) {
      const char = combined.charCodeAt(i);
      hash = ((hash << 5) - hash) + char;
      hash |= 0;
    }
    return `sms_fp_${Math.abs(hash).toString(16)}`;
  }

  static parse(
    rawSms: string,
    senderAddress = '',
    timestampMillis: number = Date.now()
  ): BankSmsParseResult {
    const normalized = this.normalize(rawSms);

    if (!normalized) {
      return {
        status: 'UNRECOGNIZED',
        bankName: 'نامشخص',
        bankCode: 'UNKNOWN',
        transactionType: 'UNKNOWN',
        rawAmountDetected: 0,
        wasInRial: false,
        amountInToman: 0,
        timestampMillis,
        confidence: 0,
        recognitionReasons: ['متن پیامک خالی است'],
        suggestedTitle: 'پیامک ناشناخته',
        suggestedCategory: 'سایر',
        fingerprint: ''
      };
    }

    const lowerText = normalized.toLowerCase();
    const lowerSender = senderAddress.toLowerCase();

    // 1. Check for OTP / Security SMS
    const hasOtpKeyword = OTP_KEYWORDS.some(k => lowerText.includes(k) || lowerSender.includes(k));
    const hasExplicitTx = lowerText.includes('برداشت') || lowerText.includes('واریز') || lowerText.includes('خرید');

    if (hasOtpKeyword && !hasExplicitTx) {
      return {
        status: 'IGNORED_OTP',
        bankName: 'سرویس رمز پویا',
        bankCode: 'OTP',
        transactionType: 'UNKNOWN',
        rawAmountDetected: 0,
        wasInRial: false,
        amountInToman: 0,
        timestampMillis,
        confidence: 1.0,
        recognitionReasons: ['پیامک رمز یکبار مصرف (OTP) تشخیص داده شد و نادیده گرفته شد'],
        suggestedTitle: 'رمز پویا',
        suggestedCategory: 'امنیت',
        fingerprint: ''
      };
    }

    // 2. Check for Failed Transactions
    if (FAILED_KEYWORDS.some(k => lowerText.includes(k))) {
      return {
        status: 'IGNORED_NON_FINANCIAL',
        bankName: 'تراکنش ناموفق',
        bankCode: 'FAILED_TX',
        transactionType: 'UNKNOWN',
        rawAmountDetected: 0,
        wasInRial: false,
        amountInToman: 0,
        timestampMillis,
        confidence: 0.95,
        recognitionReasons: ['تراکنش ناموفق است و در حساب مالی اعمال نمی‌شود'],
        suggestedTitle: 'تراکنش ناموفق',
        suggestedCategory: 'سایر',
        fingerprint: ''
      };
    }

    // 3. Check for Promotional messages
    if (PROMOTIONAL_KEYWORDS.some(k => lowerText.includes(k)) && !hasExplicitTx) {
      return {
        status: 'IGNORED_NON_FINANCIAL',
        bankName: 'اطلاع‌رسانی تبلیغاتی',
        bankCode: 'PROMO',
        transactionType: 'UNKNOWN',
        rawAmountDetected: 0,
        wasInRial: false,
        amountInToman: 0,
        timestampMillis,
        confidence: 0.90,
        recognitionReasons: ['پیامک تبلیغاتی یا تسهیلات بدون تراکنش'],
        suggestedTitle: 'پیام تبلیغاتی',
        suggestedCategory: 'سایر',
        fingerprint: ''
      };
    }

    // 4. Identify Bank
    const matchedBank = this.findBank(senderAddress, normalized);
    const bankName = matchedBank ? matchedBank.bankName : 'بانک عضو شتاب';
    const bankCode = matchedBank ? matchedBank.bankCode : 'SHETAB';

    // 5. Extract Card/Account Mask
    const cardMatch = normalized.match(/(?:کارت|حساب|به کارت|از کارت|به حساب|از حساب|\*)\s*([0-9\*xX]{4,16})/);
    let cardMask = '';
    if (cardMatch && cardMatch[1]) {
      const digits = cardMatch[1].replace(/\D/g, '');
      cardMask = digits.length >= 4 ? digits.slice(-4) : digits;
    }

    // 6. Extract Tracking/Reference Code
    const trackingMatch = normalized.match(/(?:پیگیری|مرجع|رهگیری|کد پیگیری|شماره ارجاع)\s*[:\s]*([0-9a-zA-Z]+)/);
    const trackingCode = trackingMatch ? trackingMatch[1] : '';

    // 7. Extract Balance (مانده / موجودی)
    const balanceMatch = normalized.match(/(?:مانده|موجودی)(?:\s+کل|\s+حساب|\s+کارت)?\s*[:\s]*([0-9,،\.]+)\s*(ریال|تومان)?/);
    let balanceRaw: number | null = null;
    let balanceWasInRial = true;
    if (balanceMatch && balanceMatch[1]) {
      const cleaned = balanceMatch[1].replace(/[,،]/g, '');
      const parsed = parseFloat(cleaned);
      if (!isNaN(parsed)) {
        balanceRaw = parsed;
        if (balanceMatch[2] && balanceMatch[2].includes('تومان')) {
          balanceWasInRial = false;
        }
      }
    }

    // 8. Try Specific Bank Pattern Rules
    let detectedRawAmount: number | null = null;
    let wasInRial = true;
    let detectedTxType: 'INCOME' | 'EXPENSE' | 'TRANSFER' | 'FEE' | 'UNKNOWN' = 'UNKNOWN';
    let matchedTemplateId = 'GENERIC';
    let suggestedCat = 'خرید روزمره';
    const reasons: string[] = [];

    if (matchedBank) {
      for (const rule of matchedBank.patterns) {
        const m = normalized.match(rule.regex);
        if (m && m[1]) {
          const cleaned = m[1].replace(/[,،]/g, '');
          const val = parseFloat(cleaned);
          if (!isNaN(val) && val > 0) {
            detectedRawAmount = val;
            detectedTxType = rule.transactionType;
            matchedTemplateId = rule.templateId;
            suggestedCat = rule.defaultCategory;
            if (m[2] && m[2].includes('تومان')) {
              wasInRial = false;
            } else {
              wasInRial = rule.isRial;
            }
            reasons.push(`الگوی اختصاصی ${rule.templateId} منطبق شد`);
            break;
          }
        }
      }
    }

    // 9. Generic Fallback
    if (detectedRawAmount === null) {
      const isExplicitToman = lowerText.includes('تومان') || lowerText.includes('تومن');
      wasInRial = !isExplicitToman;

      const isWithdrawal = lowerText.includes('برداشت') || lowerText.includes('خرید') || lowerText.includes('کاهش') || lowerText.includes('پرداخت');
      const isDeposit = lowerText.includes('واریز') || lowerText.includes('افزایش') || lowerText.includes('بستانکار') || lowerText.includes('سود');
      const isTransfer = lowerText.includes('انتقال') || lowerText.includes('پایا') || lowerText.includes('ساتنا') || lowerText.includes('کارت به کارت');

      if (isTransfer) {
        detectedTxType = 'TRANSFER';
      } else if (isDeposit && !isWithdrawal) {
        detectedTxType = 'INCOME';
      } else if (isWithdrawal && !isDeposit) {
        detectedTxType = 'EXPENSE';
      } else if (lowerText.includes('+') && !lowerText.includes('-')) {
        detectedTxType = 'INCOME';
      } else if (lowerText.includes('-') && !lowerText.includes('+')) {
        detectedTxType = 'EXPENSE';
      }

      // Look for amount in lines
      const numberMatches = normalized.match(/([0-9]{1,3}(?:[,،][0-9]{3})+|[0-9]{4,12})/g);
      if (numberMatches) {
        for (const numStr of numberMatches) {
          const val = parseFloat(numStr.replace(/[,،]/g, ''));
          // Avoid balance, card number, or dates
          if (balanceRaw !== null && Math.abs(val - balanceRaw) < 1) continue;
          if (val > 100) {
            detectedRawAmount = val;
            reasons.push('مبلغ از اعداد تراکنش استخراج شد');
            break;
          }
        }
      }
    }

    // 10. Check if only Balance Inquiry
    if (detectedRawAmount === null && balanceRaw !== null) {
      const balanceToman = balanceWasInRial ? balanceRaw / 10 : balanceRaw;
      return {
        status: 'IGNORED_NON_FINANCIAL',
        bankName,
        bankCode,
        transactionType: 'UNKNOWN',
        rawAmountDetected: 0,
        wasInRial: balanceWasInRial,
        amountInToman: 0,
        balanceInToman: Math.round(balanceToman),
        cardOrAccountMask: cardMask,
        timestampMillis,
        confidence: 0.90,
        recognitionReasons: ['پیامک صرفاً اعلام موجودی حساب است'],
        suggestedTitle: `موجودی ${bankName}`,
        suggestedCategory: 'اطلاع‌رسانی',
        fingerprint: ''
      };
    }

    if (detectedRawAmount === null || detectedRawAmount <= 0) {
      return {
        status: 'UNRECOGNIZED',
        bankName,
        bankCode,
        transactionType: detectedTxType,
        rawAmountDetected: 0,
        wasInRial,
        amountInToman: 0,
        timestampMillis,
        confidence: 0,
        recognitionReasons: ['هیچ مبلغ معتبری شناسایی نشد'],
        suggestedTitle: `پیامک ${bankName}`,
        suggestedCategory: 'سایر',
        fingerprint: ''
      };
    }

    const amountInToman = Math.round(wasInRial ? detectedRawAmount / 10 : detectedRawAmount);
    const balanceInToman = balanceRaw !== null ? Math.round(balanceWasInRial ? balanceRaw / 10 : balanceRaw) : undefined;

    const suggestedTitle = detectedTxType === 'INCOME'
      ? `واریز به ${bankName}`
      : detectedTxType === 'EXPENSE'
      ? `خرید / برداشت ${bankName}`
      : detectedTxType === 'TRANSFER'
      ? `انتقال وجه ${bankName}`
      : `تراکنش ${bankName}`;

    const confidence = matchedTemplateId !== 'GENERIC' && detectedTxType !== 'UNKNOWN' ? 0.98 : 0.85;
    const status = confidence >= 0.80 && detectedTxType !== 'UNKNOWN' ? 'SUCCESS' : 'NEEDS_REVIEW';

    const timeBucket = Math.floor(timestampMillis / (1000 * 60 * 60)).toString();
    const fingerprint = this.computeFingerprint(bankCode, detectedTxType, amountInToman, cardMask, trackingCode, timeBucket);

    return {
      status,
      bankName,
      bankCode,
      transactionType: detectedTxType,
      rawAmountDetected: detectedRawAmount,
      wasInRial,
      amountInToman,
      balanceInToman,
      cardOrAccountMask: cardMask,
      trackingCode,
      suggestedTitle,
      suggestedCategory: suggestedCat,
      timestampMillis,
      confidence,
      recognitionReasons: reasons,
      fingerprint
    };
  }
}
