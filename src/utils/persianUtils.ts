// Persian text normalizer and formatting utilities

export function formatToman(amount: number): string {
  try {
    return new Intl.NumberFormat('fa-IR').format(amount) + ' تومان';
  } catch {
    return amount.toLocaleString() + ' تومان';
  }
}

export function formatPersianNumber(num: number): string {
  const persianDigits = ['۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹'];
  return num.toString().replace(/\d/g, (x) => persianDigits[parseInt(x)]);
}

// Persian text normalization (matching PersianTextNormalizer.kt)
export function normalizePersianText(input: string): string {
  if (!input) return '';
  return input
    .replace(/ك/g, 'ک')
    .replace(/ي/g, 'ی')
    .replace(/۰/g, '0')
    .replace(/۱/g, '1')
    .replace(/۲/g, '2')
    .replace(/۳/g, '3')
    .replace(/۴/g, '4')
    .replace(/۵/g, '5')
    .replace(/۶/g, '6')
    .replace(/۷/g, '7')
    .replace(/۸/g, '8')
    .replace(/۹/g, '9')
    .trim()
    .toLowerCase();
}

// Simulated Bank SMS Parser matching BankSmsParser.kt
export interface ParsedSmsResult {
  amount: number;
  merchant: string;
  bankName: string;
  type: 'withdrawal' | 'deposit';
  raw: string;
}

export function parseBankSms(smsText: string): ParsedSmsResult | null {
  const normalized = smsText;
  
  // Detect bank name
  let bankName = 'بانک سامان/ملت';
  if (normalized.includes('ملت')) bankName = 'بانک ملت';
  else if (normalized.includes('سامان')) bankName = 'بانک سامان';
  else if (normalized.includes('ملی')) bankName = 'بانک ملی';
  else if (normalized.includes('صادرات')) bankName = 'بانک صادرات';
  else if (normalized.includes('پاسارگاد')) bankName = 'بانک پاسارگاد';

  // Extract amount pattern (e.g. 1,500,000 ریال or تومان)
  const amountMatch = normalized.match(/([\d,]+)\s*(ریال|تومان)/);
  if (!amountMatch) return null;

  let rawAmountStr = amountMatch[1].replace(/,/g, '');
  let amount = parseInt(rawAmountStr, 10);
  if (isNaN(amount)) return null;

  // If expressed in Rials, convert to Tomans
  if (amountMatch[2] === 'ریال') {
    amount = Math.floor(amount / 10);
  }

  // Extract merchant or description
  let merchant = 'خرید کارت بانکی';
  if (normalized.includes('خرید')) merchant = 'خرید فروشگاهی / اینترنتی';
  else if (normalized.includes('انتقال')) merchant = 'انتقال وجه کارت به کارت';
  else if (normalized.includes('برداشت')) merchant = 'برداشت وجه از خودپرداز';
  else if (normalized.includes('واریز')) merchant = 'واریز حقوق / دریافت وجه';

  return {
    amount,
    merchant,
    bankName,
    type: normalized.includes('واریز') ? 'deposit' : 'withdrawal',
    raw: smsText
  };
}

// Get current Jalali date approximation or Persian formatted string
export function getPersianDateString(): string {
  // Approximate conversion or standard display for demo
  return '۱۴۰۵/۰۸/۰۵ (۰۵ آبان)';
}
