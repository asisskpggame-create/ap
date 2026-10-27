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
    .trim();
}

export interface ParsedSmsResult {
  amount: number;
  merchant: string;
  bankName: string;
  type: 'withdrawal' | 'deposit';
  trackingCode?: string;
  raw: string;
}

export function parseBankSms(smsText: string): ParsedSmsResult | null {
  if (!smsText) return null;
  const normalized = normalizePersianText(smsText);

  // 1. Detect Bank Name
  let bankName = 'بانک مرکزی / شتاب';
  if (normalized.includes('ملت')) bankName = 'بانک ملت';
  else if (normalized.includes('سامان')) bankName = 'بانک سامان';
  else if (normalized.includes('ملی') || normalized.includes('سداد')) bankName = 'بانک ملی ایران';
  else if (normalized.includes('صادرات')) bankName = 'بانک صادرات ایران';
  else if (normalized.includes('تجارت')) bankName = 'بانک تجارت';
  else if (normalized.includes('سپه')) {
    if (normalized.includes('کوثر')) bankName = 'بانک سپه (کوثر سابق)';
    else bankName = 'بانک سپه';
  }
  else if (normalized.includes('بلو') || normalized.includes('بلوبانک')) bankName = 'بلوبانک (Blu)';
  else if (normalized.includes('ویپاد') || normalized.includes('پاسارگاد')) {
    bankName = normalized.includes('ویپاد') ? 'ویپاد (پاسارگاد)' : 'بانک پاسارگاد';
  }
  else if (normalized.includes('مسکن')) bankName = 'بانک مسکن';
  else if (normalized.includes('کشاورزی')) bankName = 'بانک کشاورزی';
  else if (normalized.includes('توسعه تعاون')) bankName = 'بانک توسعه تعاون';
  else if (normalized.includes('صنعت و معدن')) bankName = 'بانک صنعت و معدن';
  else if (normalized.includes('توسعه صادرات')) bankName = 'بانک توسعه صادرات';
  else if (normalized.includes('پست بانک')) bankName = 'پست بانک ایران';
  else if (normalized.includes('رفاه') || normalized.includes('بمانده')) bankName = 'بانک رفاه کارگران';
  else if (normalized.includes('مهر ایران') || normalized.includes('مهرایران')) bankName = 'بانک قرض‌الحسنه مهر ایران';
  else if (normalized.includes('رسالت')) bankName = 'بانک قرض‌الحسنه رسالت';
  else if (normalized.includes('پارسیان')) bankName = 'بانک پارسیان';
  else if (normalized.includes('آینده')) bankName = 'بانک آینده';
  else if (normalized.includes('شهر')) bankName = 'بانک شهر';
  else if (normalized.includes('گردشگری')) bankName = 'بانک گردشگری';
  else if (normalized.includes('اقتصاد نوین')) bankName = 'بانک اقتصاد نوین';
  else if (normalized.includes('سینا')) bankName = 'بانک سینا';
  else if (normalized.includes('دی')) bankName = 'بانک دی';
  else if (normalized.includes('کارآفرین')) bankName = 'بانک کارآفرین';
  else if (normalized.includes('ایران زمین')) bankName = 'بانک ایران زمین';
  else if (normalized.includes('سرمایه')) bankName = 'بانک سرمایه';
  else if (normalized.includes('خاورمیانه')) bankName = 'بانک خاورمیانه';
  else if (normalized.includes('ملل')) bankName = 'مؤسسه اعتباری ملل';

  // 2. Detect Transaction Type (Deposit vs Withdrawal)
  let type: 'withdrawal' | 'deposit' = 'withdrawal';
  if (
    normalized.includes('واریز') ||
    normalized.includes('افزایش') ||
    normalized.includes('بستانکار') ||
    normalized.includes('نشست') ||
    normalized.includes('+') ||
    (normalized.includes('به حساب') && !normalized.includes('برداشت'))
  ) {
    type = 'deposit';
  } else if (
    normalized.includes('برداشت') ||
    normalized.includes('خرید') ||
    normalized.includes('کسر') ||
    normalized.includes('بدهکار') ||
    normalized.includes('پرید') ||
    normalized.includes('-')
  ) {
    type = 'withdrawal';
  }

  // 3. Extract Amount
  const matches = normalized.match(/([\d,٬]+)\s*(ریال|تومان|ر|R|\+)?/i) || normalized.match(/([\d,٬]{4,})/);
  if (!matches) return null;

  const rawAmountStr = matches[1].replace(/,/g, '').replace(/٬/g, '');
  let amount = parseInt(rawAmountStr, 10);
  if (isNaN(amount)) return null;

  const unitMatch = matches[2] || '';
  if (unitMatch.toLowerCase() === 'ریال' || unitMatch.toUpperCase() === 'R' || unitMatch === 'ر' || (!normalized.includes('تومان') && amount > 10000)) {
    if (!normalized.includes('تومان')) {
      amount = Math.floor(amount / 10);
    }
  }

  // 4. Extract Merchant / Transaction description
  let merchant = 'تراکنش بانکی';
  if (normalized.includes('خرید')) merchant = 'خرید فروشگاهی / اینترنتی';
  else if (normalized.includes('انتقال پل') || normalized.includes('پل')) merchant = 'انتقال لحظه‌ای (سامانه پل)';
  else if (normalized.includes('انتقال')) merchant = 'انتقال وجه کارت به کارت';
  else if (normalized.includes('برداشت')) merchant = 'برداشت وجه / خودپرداز';
  else if (normalized.includes('واریز')) merchant = 'واریز وجه / حقوق';
  else if (normalized.includes('پرید')) merchant = 'خرید / کسر وجه';

  // 5. Extract Tracking / Reference Code
  let trackingCode: string | undefined = undefined;
  const trackingMatch = normalized.match(/(?:پیگیری|کد ارجاع|شماره پیگیری|ردیف|سند|ارجاع|کد مرجع|شناسه پیگیری|کد رهگیری)[:\s]*([\d\w]+)/i);
  if (trackingMatch) {
    trackingCode = trackingMatch[1];
  }

  return {
    amount,
    merchant,
    bankName,
    type,
    trackingCode,
    raw: smsText
  };
}

export function getPersianDateString(): string {
  return '۱۴۰۵/۰۸/۰۵ (۰۵ آبان)';
}
