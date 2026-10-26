// Persian Calendar, Jalali Algorithms, Numeral Normalization and Financial Calculations
// Ported from PersianDateAndUnitUtils.kt and WorldClassOsUtils.kt

export interface JalaliDate {
  year: number;
  month: number; // 1..12
  day: number; // 1..31
}

const persianDigits = ['۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹'];
const arabicDigits = ['٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩'];

export const jalaliMonthNamesFa = [
  'فروردین', 'اردیبهشت', 'خرداد', 'تیر', 'مرداد', 'شهریور',
  'مهر', 'آبان', 'آذر', 'دی', 'بهمن', 'اسفند'
];

export const gregorianMonthNamesEn = [
  'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
  'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'
];

export const weekdayNamesFa: Record<number, string> = {
  1: 'دوشنبه',
  2: 'سه‌شنبه',
  3: 'چهارشنبه',
  4: 'پنج‌شنبه',
  5: 'جمعه',
  6: 'شنبه',
  7: 'یکشنبه'
};

export function toPersianDigits(input: string | number): string {
  const str = String(input);
  let res = '';
  for (let i = 0; i < str.length; i++) {
    const ch = str[i];
    if (ch >= '0' && ch <= '9') {
      res += persianDigits[ch.charCodeAt(0) - 48];
    } else {
      res += ch;
    }
  }
  return res;
}

export function toEnglishDigits(input: string): string {
  let res = '';
  for (let i = 0; i < input.length; i++) {
    const ch = input[i];
    const pIdx = persianDigits.indexOf(ch);
    const aIdx = arabicDigits.indexOf(ch);
    if (pIdx >= 0) {
      res += String(pIdx);
    } else if (aIdx >= 0) {
      res += String(aIdx);
    } else {
      res += ch;
    }
  }
  return res;
}

export function gregorianToEpochDay(year: number, month: number, day: number): number {
  const y = year;
  const m = month;
  const d = day;
  const yAdj = m <= 2 ? y - 1 : y;
  const era = yAdj >= 0 ? Math.floor(yAdj / 400) : Math.floor((yAdj - 399) / 400);
  const yoe = yAdj - era * 400;
  const doy = Math.floor((153 * (m > 2 ? m - 3 : m + 9) + 2) / 5) + d - 1;
  const doe = yoe * 365 + Math.floor(yoe / 4) - Math.floor(yoe / 100) + doy;
  return era * 146097 + doe - 719468;
}

export function epochDayToGregorian(epochDay: number): { year: number; month: number; day: number } {
  const z = epochDay + 719468;
  const era = z >= 0 ? Math.floor(z / 146097) : Math.floor((z - 146096) / 146097);
  const doe = z - era * 146097;
  const yoe = Math.floor((doe - Math.floor(doe / 1460) + Math.floor(doe / 36524) - Math.floor(doe / 146096)) / 365);
  const y = yoe + era * 400;
  const doy = doe - (365 * yoe + Math.floor(yoe / 4) - Math.floor(yoe / 100));
  const mp = Math.floor((5 * doy + 2) / 153);
  const d = Math.floor(doy - (153 * mp + 2) / 5 + 1);
  const m = Math.floor(mp < 10 ? mp + 3 : mp - 9);
  const year = Math.floor(m <= 2 ? y + 1 : y);
  return { year, month: m, day: d };
}

/**
 * Converts Gregorian date to Solar Hijri (Jalali)
 */
export function gregorianToJalali(gy: number, gm: number, gd: number): JalaliDate {
  const gDaysInMonth = [0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334];
  const gy2 = gm > 2 ? gy + 1 : gy;
  let days = 355666 + (365 * gy) + Math.floor((gy2 + 3) / 4) - Math.floor((gy2 + 99) / 100) + Math.floor((gy2 + 399) / 400) + gd + gDaysInMonth[gm - 1];
  let jy = -1595 + (33 * Math.floor(days / 12053));
  days %= 12053;
  jy += 4 * Math.floor(days / 1461);
  days %= 1461;
  if (days > 365) {
    jy += Math.floor((days - 1) / 365);
    days = (days - 1) % 365;
  }
  let jm: number;
  let jd: number;
  if (days < 186) {
    jm = 1 + Math.floor(days / 31);
    jd = 1 + (days % 31);
  } else {
    jm = 7 + Math.floor((days - 186) / 30);
    jd = 1 + ((days - 186) % 30);
  }
  return { year: jy, month: jm, day: jd };
}

export function epochDayToJalali(epochDay: number): JalaliDate {
  const { year, month, day } = epochDayToGregorian(epochDay);
  return gregorianToJalali(year, month, day);
}

export function currentLocalEpochDay(): number {
  const now = new Date();
  return gregorianToEpochDay(now.getFullYear(), now.getMonth() + 1, now.getDate());
}

/**
 * ISO day of week for epochDay: 1 = Monday .. 7 = Sunday
 * (1970-01-01 was Thursday = 4)
 */
export function dayOfWeekIso(epochDay: number): number {
  const mod = ((epochDay + 3) % 7);
  return (mod < 0 ? mod + 7 : mod) + 1;
}

export function formatEpochDay(
  epochDay: number,
  calendarDisplay: 'JALALI' | 'GREGORIAN' = 'JALALI',
  usePersianDigits = true
): string {
  if (epochDay < 0) return 'بدون موعد';
  const dow = weekdayNamesFa[dayOfWeekIso(epochDay)] || '';
  if (calendarDisplay === 'GREGORIAN') {
    const { year, month, day } = epochDayToGregorian(epochDay);
    const mName = gregorianMonthNamesEn[month - 1] || `${month}`;
    const raw = `${dow}، ${day} ${mName} ${year}`;
    return usePersianDigits ? toPersianDigits(raw) : raw;
  } else {
    const j = epochDayToJalali(epochDay);
    const mName = jalaliMonthNamesFa[j.month - 1] || `${j.month}`;
    const raw = `${dow}، ${j.day} ${mName} ${j.year}`;
    return usePersianDigits ? toPersianDigits(raw) : raw;
  }
}

export function formatTimeMinutes(minutesFromMidnight: number, usePersianDigits = true): string {
  if (minutesFromMidnight < 0) return '';
  const h = Math.floor(minutesFromMidnight / 60);
  const m = minutesFromMidnight % 60;
  const raw = `${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}`;
  return usePersianDigits ? toPersianDigits(raw) : raw;
}

export function formatFinancialAmount(
  amount: number,
  currency = 'TOMAN',
  usePersian = true
): string {
  const rounded = Math.round(amount);
  const formattedWithCommas = rounded.toLocaleString('en-US');
  const unitLabel = currency === 'TOMAN' ? 'تومان' : currency === 'IRR' ? 'ریال' : currency;
  const result = `${formattedWithCommas} ${unitLabel}`;
  return usePersian ? toPersianDigits(result) : result;
}

export function formatCompactCurrency(amount: number, usePersian = true): string {
  if (amount >= 1_000_000) {
    const val = (amount / 1_000_000).toFixed(1).replace('.0', '');
    const res = `${val} میلیون تومان`;
    return usePersian ? toPersianDigits(res) : res;
  } else if (amount >= 1_000) {
    const val = (amount / 1_000).toFixed(0);
    const res = `${val} هزار تومان`;
    return usePersian ? toPersianDigits(res) : res;
  }
  const res = `${Math.round(amount)} تومان`;
  return usePersian ? toPersianDigits(res) : res;
}

/**
 * Calculates Safe-to-Spend for today based on monthly budget, expenses so far,
 * and remaining days in the Jalali month.
 */
export function calculateSafeToSpend(
  monthlyBudget: number,
  totalExpenseThisMonth: number,
  todayEpochDay: number = currentLocalEpochDay()
): {
  safeDailyAmount: number;
  remainingBudget: number;
  daysRemainingInMonth: number;
  budgetHealth: 'HEALTHY' | 'MODERATE' | 'CRITICAL' | 'OVER_BUDGET';
} {
  const jToday = epochDayToJalali(todayEpochDay);
  const daysInJalaliMonth = jToday.month <= 6 ? 31 : jToday.month <= 11 ? 30 : 29;
  const daysPassed = jToday.day;
  const daysRemaining = Math.max(1, daysInJalaliMonth - daysPassed + 1);

  const remainingBudget = monthlyBudget - totalExpenseThisMonth;

  if (remainingBudget <= 0) {
    return {
      safeDailyAmount: 0,
      remainingBudget,
      daysRemainingInMonth: daysRemaining,
      budgetHealth: 'OVER_BUDGET'
    };
  }

  // 10% safety buffer
  const bufferedRemaining = remainingBudget * 0.90;
  const safeDaily = Math.round(bufferedRemaining / daysRemaining);

  const spentRatio = totalExpenseThisMonth / monthlyBudget;
  const monthProgressRatio = daysPassed / daysInJalaliMonth;

  let health: 'HEALTHY' | 'MODERATE' | 'CRITICAL' | 'OVER_BUDGET' = 'HEALTHY';
  if (spentRatio > 1.0) {
    health = 'OVER_BUDGET';
  } else if (spentRatio > monthProgressRatio + 0.2) {
    health = 'CRITICAL';
  } else if (spentRatio > monthProgressRatio + 0.05) {
    health = 'MODERATE';
  }

  return {
    safeDailyAmount: safeDaily,
    remainingBudget,
    daysRemainingInMonth: daysRemaining,
    budgetHealth: health
  };
}
