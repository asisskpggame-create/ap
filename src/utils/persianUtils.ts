// Persian text normalizer and formatting utilities

export function formatToman(amount: number): string {
  try {
    return new Intl.NumberFormat('fa-IR').format(amount) + ' تومان';
  } catch {
    return amount.toLocaleString() + ' تومان';
  }
}

export function formatPersianNumber(num: number | string): string {
  const persianDigits = ['۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹'];
  return num.toString().replace(/\d/g, (x) => persianDigits[parseInt(x)]);
}

// Persian text normalization
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

export interface ParsedSmsResult {
  amount: number;          // in Tomans
  rawAmount: number;       // in original Rials
  merchant: string;
  bankName: string;
  type: 'withdrawal' | 'deposit';
  balance?: number;        // in Tomans
  trackingCode?: string;
  raw: string;
}

export function parseBankSms(smsText: string): ParsedSmsResult | null {
  if (!smsText || !smsText.trim()) return null;
  const normalized = normalizePersianText(smsText);

  // 1. Detect Bank
  let bankName = 'بانک شتابی';
  if (normalized.includes('بلو') || normalized.includes('blubank')) bankName = 'بلوبانک';
  else if (normalized.includes('ویپاد') || normalized.includes('wepod')) bankName = 'ویپاد (پاسارگاد)';
  else if (normalized.includes('پل') || normalized.includes('انتقال پل')) bankName = 'سامانه پل (پرداخت لحظه‌ای)';
  else if (normalized.includes('ملی') || normalized.includes('سداد')) bankName = 'بانک ملی ایران';
  else if (normalized.includes('ملت') || normalized.includes('بهسازان')) bankName = 'بانک ملت';
  else if (normalized.includes('صادرات') || normalized.includes('سپهر')) bankName = 'بانک صادرات ایران';
  else if (normalized.includes('تجارت')) bankName = 'بانک تجارت';
  else if (normalized.includes('کوثر')) bankName = 'بانک سپه (کوثر سابق)';
  else if (normalized.includes('نور')) bankName = 'بانک ملی (نور سابق)';
  else if (normalized.includes('سپه')) bankName = 'بانک سپه';
  else if (normalized.includes('مسکن') || normalized.includes('ناواکو')) bankName = 'بانک مسکن';
  else if (normalized.includes('کشاورزی') || normalized.includes('مهرگستر')) bankName = 'بانک کشاورزی';
  else if (normalized.includes('توسعه تعاون')) bankName = 'بانک توسعه تعاون';
  else if (normalized.includes('صنعت و معدن')) bankName = 'بانک صنعت و معدن';
  else if (normalized.includes('توسعه صادرات')) bankName = 'بانک توسعه صادرات';
  else if (normalized.includes('پست بانک')) bankName = 'پست بانک ایران';
  else if (normalized.includes('رفاه')) bankName = 'بانک رفاه کارگران';
  else if (normalized.includes('مهر ایران')) bankName = 'بانک مهر ایران';
  else if (normalized.includes('رسالت')) bankName = 'بانک قرض‌الحسنه رسالت';
  else if (normalized.includes('پاسارگاد') || normalized.includes('pasargad')) bankName = 'بانک پاسارگاد';
  else if (normalized.includes('سامان')) bankName = 'بانک سامان';
  else if (normalized.includes('پارسیان') || normalized.includes('لوتوس')) bankName = 'بانک پارسیان';
  else if (normalized.includes('آینده') || normalized.includes('ayandeh')) bankName = 'بانک آینده';
  else if (normalized.includes('شهر') || normalized.includes('shahr')) bankName = 'بانک شهر';
  else if (normalized.includes('گردشگری')) bankName = 'بانک گردشگری';
  else if (normalized.includes('اقتصاد نوین')) bankName = 'بانک اقتصاد نوین';
  else if (normalized.includes('سینا')) bankName = 'بانک سینا';
  else if (normalized.includes('دی') && !normalized.includes('خرید')) bankName = 'بانک دی';
  else if (normalized.includes('کارآفرین')) bankName = 'بانک کارآفرین';
  else if (normalized.includes('ایران زمین')) bankName = 'بانک ایران زمین';
  else if (normalized.includes('سرمایه')) bankName = 'بانک سرمایه';
  else if (normalized.includes('خاورمیانه')) bankName = 'بانک خاورمیانه';
  else if (normalized.includes('ملل') || normalized.includes('عسکریه')) bankName = 'موسسه اعتباری ملل';

  // 2. Transaction Direction
  let isDeposit = false;
  if (
    normalized.includes('واریز') ||
    normalized.includes('افزایش') ||
    normalized.includes('بستانکار') ||
    normalized.includes('نشست') ||
    /[\d,]+\s*\+/.test(normalized)
  ) {
    isDeposit = true;
  } else if (
    normalized.includes('برداشت') ||
    normalized.includes('خرید') ||
    normalized.includes('کسر') ||
    normalized.includes('بدهکار') ||
    normalized.includes('پرید') ||
    /[\d,]+\s*-/.test(normalized)
  ) {
    isDeposit = false;
  }

  // 3. Amount Extraction
  let rawAmount: number | null = null;
  let isTomanUnit = false;

  // Algebraic match (e.g. 1,698,500+)
  const algebraicMatch = normalized.match(/([\d,]{4,})\s*([+-])/);
  if (algebraicMatch) {
    rawAmount = parseInt(algebraicMatch[1].replace(/,/g, ''), 10);
  }

  // Refah R suffix (e.g. برداشت1,960,000R)
  if (rawAmount === null) {
    const refahMatch = normalized.match(/(?:برداشت|واریز|مبلغ)\s*([\d,]{4,})\s*[r]/i);
    if (refahMatch) {
      rawAmount = parseInt(refahMatch[1].replace(/,/g, ''), 10);
    }
  }

  // With currency suffix (ریال / تومان)
  if (rawAmount === null) {
    const currencyMatch = normalized.match(/([\d,]{4,})\s*(ریال|تومان)/);
    if (currencyMatch) {
      rawAmount = parseInt(currencyMatch[1].replace(/,/g, ''), 10);
      isTomanUnit = currencyMatch[2] === 'تومان';
    }
  }

  // Label prefix (مبلغ: ..., برداشت: ...)
  if (rawAmount === null) {
    const labelMatch = normalized.match(/(?:مبلغ|برداشت|واریز|خرید)[\s:]*([\d,]{4,})/);
    if (labelMatch) {
      rawAmount = parseInt(labelMatch[1].replace(/,/g, ''), 10);
    }
  }

  // Generic digits fallback
  if (rawAmount === null) {
    const genericMatch = normalized.match(/([\d,]{5,})/);
    if (genericMatch) {
      rawAmount = parseInt(genericMatch[1].replace(/,/g, ''), 10);
    }
  }

  if (rawAmount === null || isNaN(rawAmount) || rawAmount <= 0) return null;

  const amountInTomans = isTomanUnit ? rawAmount : Math.floor(rawAmount / 10);

  // 4. Balance Extraction
  let balanceInTomans: number | undefined;
  const balanceMatch = normalized.match(/(?:مانده|موجودی|بمانده|مانده حساب)[\s:]*([\d,]{4,})/);
  if (balanceMatch) {
    const rawBal = parseInt(balanceMatch[1].replace(/,/g, ''), 10);
    if (!isNaN(rawBal)) {
      balanceInTomans = normalized.includes('تومان') ? rawBal : Math.floor(rawBal / 10);
    }
  }

  // 5. Tracking Code
  const trackingMatch = normalized.match(/(?:پیگیری|کد ارجاع|شماره پیگیری|ردیف|سند|کد رهگیری|کد مرجع|شماره ارجاع|ارجاع)[\s:]*([a-zA-Z0-9]{4,})/);
  const trackingCode = trackingMatch ? trackingMatch[1] : undefined;

  // 6. Merchant / Action
  let merchant = isDeposit ? 'واریز وجه به حساب' : 'برداشت / خرید کارت';
  if (normalized.includes('پل') || normalized.includes('انتقال پل')) merchant = 'انتقال وجه سامانه پل';
  else if (normalized.includes('پرید')) merchant = 'برداشت از حساب بلوبانک';
  else if (normalized.includes('خرید')) merchant = 'خرید فروشگاهی / اینترنتی';
  else if (normalized.includes('کارت به کارت') || normalized.includes('انتقال به کارت')) merchant = 'انتقال وجه کارت به کارت';
  else if (normalized.includes('پایانه') || normalized.includes('pos')) merchant = 'خرید پایانه فروشگاهی (POS)';
  else if (normalized.includes('خودپرداز') || normalized.includes('atm')) merchant = 'برداشت وجه خودپرداز';
  else if (normalized.includes('حقوق')) merchant = 'واریز حقوق ماهانه';

  return {
    amount: amountInTomans,
    rawAmount,
    merchant,
    bankName,
    type: isDeposit ? 'deposit' : 'withdrawal',
    balance: balanceInTomans,
    trackingCode,
    raw: smsText
  };
}

export function getPersianDateString(): string {
  return '۱۴۰۵/۰۸/۰۵ (۰۵ آبان)';
}
