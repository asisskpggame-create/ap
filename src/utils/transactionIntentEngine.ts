// On-Device Persian Intent Engine for Financial Transactions and Life Logs
// Ported from com.example.ai.TransactionIntentEngine.kt

import { TransactionIntent } from '../types/lifeOs';
import { toEnglishDigits } from './persianDateUtils';

export class TransactionIntentEngine {
  static detectIntent(
    rawText: string,
    defaultCurrency = 'TOMAN'
  ): TransactionIntent {
    const norm = toEnglishDigits(rawText)
      .replace(/میلیلیتر/g, 'میلی‌لیتر')
      .replace(/پیادهروی/g, 'پیاده‌روی')
      .replace(/برنامهریزی/g, 'برنامه‌ریزی')
      .replace(/انجامشده/g, 'انجام‌شده')
      .trim();

    if (!norm) {
      return {
        type: 'UNKNOWN',
        amount: 0,
        currency: defaultCurrency,
        category: 'نامشخص',
        title: '',
        isAmbiguous: true,
        confidence: 0,
        reasoning: 'متن ورودی خالی است.'
      };
    }

    const lower = norm.toLowerCase();

    // 1. Determine currency
    let currency = 'TOMAN';
    if (lower.includes('دلار') || lower.includes('usd') || lower.includes('$')) {
      currency = 'USD';
    } else if (lower.includes('یورو') || lower.includes('eur') || lower.includes('€')) {
      currency = 'EUR';
    } else if (lower.includes('درهم') || lower.includes('aed')) {
      currency = 'AED';
    } else if (lower.includes('پوند') || lower.includes('gbp') || lower.includes('£')) {
      currency = 'GBP';
    } else if (lower.includes('ریال') || lower.includes('irr')) {
      currency = 'IRR';
    }

    // 2. Extract numeric amount with multiplier words
    const amount = this.extractAmount(lower);

    // 3. Extract person if in loan/debt context
    const personMatch = norm.match(/(?:به|از)\s+([آابپتثجچحخدذرزژسشصضطظعغفقکگلمنوهی\s]{2,20}?)(?:\s+(?:قرض|طلب|بدهکار|طلبکار|پول|وام|دادم|گرفتم)|$)/);
    let person = '';
    if (personMatch && personMatch[1]) {
      const candidate = personMatch[1].trim();
      if (!candidate.includes('حساب') && !candidate.includes('کارت') && !candidate.includes('بانک')) {
        person = candidate;
      }
    }

    // 4. Extract account transition
    const accountMatch = norm.match(/(?:از\s+([^\n]+?)\s+به\s+([^\n]+?)(?:\s+انتقال|\s+کارت|\s+واریز|$))/);
    const account = accountMatch ? `${accountMatch[1].trim()} -> ${accountMatch[2].trim()}` : '';

    // 5. Classify Intent
    const details = this.classifyDetails(lower, norm, amount, person, account);

    const isAmbiguous = amount <= 0 || details.type === 'UNKNOWN' || details.confidence < 0.65;

    return {
      type: details.type,
      amount: Math.round(amount),
      currency,
      category: details.category,
      account,
      person,
      title: details.title,
      isAmbiguous,
      confidence: details.confidence,
      reasoning: details.reason
    };
  }

  private static extractAmount(lower: string): number {
    // 2.5 میلیون و 300 هزار
    const complexMillion = lower.match(/([0-9]+(?:\.[0-9]+)?)\s*میلیون(?:\s*و\s*([0-9]+(?:\.[0-9]+)?)\s*هزار)?/);
    if (complexMillion) {
      const millions = parseFloat(complexMillion[1]) || 0;
      const thousands = complexMillion[2] ? parseFloat(complexMillion[2]) || 0 : 0;
      return (millions * 1_000_000) + (thousands * 1_000);
    }

    if (lower.includes('یک و نیم میلیون') || lower.includes('یک و نیم ملیون')) {
      return 1_500_000;
    }
    if (lower.includes('یک میلیون') || lower.includes('یه میلیون')) {
      return 1_000_000;
    }
    if (lower.includes('پانصد هزار') || lower.includes('پونصد هزار')) {
      return 500_000;
    }

    const millionMatch = lower.match(/([0-9]+(?:\.[0-9]+)?)\s*میلیون/);
    if (millionMatch) {
      return (parseFloat(millionMatch[1]) || 0) * 1_000_000;
    }

    const thousandMatch = lower.match(/([0-9]+(?:\.[0-9]+)?)\s*هزار/);
    if (thousandMatch) {
      return (parseFloat(thousandMatch[1]) || 0) * 1_000;
    }

    const plainMatch = lower.match(/(?<![0-9/:-])([0-9]{1,3}(?:[,،][0-9]{3})+|[0-9]{1,12})(?![0-9/:-])/);
    if (plainMatch) {
      const raw = parseFloat(plainMatch[1].replace(/[,،]/g, '')) || 0;
      // In Iranian spoken Persian, "600 تومن بابت فروش گرفتم" implies 600,000 Tomans
      if (raw >= 10 && raw <= 999 && (lower.includes('تومن') || lower.includes('تومان')) && !lower.includes('تک')) {
        return raw * 1_000;
      }
      return raw;
    }

    return 0;
  }

  private static classifyDetails(
    lower: string,
    norm: string,
    _amount: number,
    person: string,
    account: string
  ): {
    type: 'INCOME' | 'EXPENSE' | 'TRANSFER' | 'RECEIVABLE' | 'DEBT' | 'UNKNOWN';
    category: string;
    title: string;
    confidence: number;
    reason: string;
  } {
    // 1. Transfer
    if (
      lower.includes('انتقال') ||
      lower.includes('کارت به کارت به حساب خودم') ||
      lower.includes('جابجایی بین حساب') ||
      account.length > 0
    ) {
      return {
        type: 'TRANSFER',
        category: 'انتقال بین حسابی',
        title: account ? `انتقال ${account}` : 'انتقال بین حسابی',
        confidence: 0.95,
        reason: 'کلمات کلیدی انتقال بین حساب شخصی تشخیص داده شد'
      };
    }

    // 2. Receivable (طلبکاری)
    if (
      lower.includes('طلب دارم') ||
      lower.includes('طلبکارم') ||
      (lower.includes('قرض دادم') && !lower.includes('گرفتم')) ||
      (lower.includes('از') && lower.includes('گرفتم') && person.length > 0 && !lower.includes('بابت') && !lower.includes('فروش'))
    ) {
      return {
        type: 'RECEIVABLE',
        category: 'طلب و وام',
        title: person ? `طلب از ${person}` : 'طلب / وام پرداختی',
        confidence: 0.92,
        reason: 'دلالت بر طلبکاری یا قرض دادن به شخص دیگر دارد'
      };
    }

    // 3. Debt (بدهی)
    if (
      lower.includes('بدهکارم') ||
      lower.includes('بدهی') ||
      lower.includes('قرض گرفتم') ||
      lower.includes('وام گرفتم') ||
      (lower.includes('به') && lower.includes('دادم') && person.length > 0 && lower.includes('قرض'))
    ) {
      return {
        type: 'DEBT',
        category: 'بدهی و اقساط',
        title: person ? `بدهی به ${person}` : 'بدهی و استقراض',
        confidence: 0.92,
        reason: 'دلالت بر بدهی یا قرض گرفتن از شخص دیگر دارد'
      };
    }

    // 4. Income
    const hasIncomeVerb =
      lower.includes('گرفتم') ||
      lower.includes('دریافت کردم') ||
      lower.includes('واریز شد') ||
      lower.includes('فروختم') ||
      lower.includes('دستمزد') ||
      lower.includes('حقوق') ||
      lower.includes('پاداش') ||
      lower.includes('سود') ||
      lower.includes('درآمد');

    const isServiceRide = lower.includes('اسنپ گرفتم') || lower.includes('تپسی گرفتم') || lower.includes('تاکسی گرفتم');

    if (hasIncomeVerb && !isServiceRide) {
      let title = 'درآمد و واریزی';
      let cat = 'کسب درآمد / فروش';

      if (lower.includes('جمینای') || lower.includes('gemini')) {
        title = 'فروش Gemini';
      } else if (lower.includes('طراحی') || lower.includes('پروژه')) {
        title = 'پروژه و فریلنسری';
      } else if (lower.includes('حقوق')) {
        title = 'حقوق ماهانه';
        cat = 'حقوق و دستمزد';
      } else if (lower.includes('سود')) {
        title = 'سود بانکی و سرمایه';
        cat = 'سرمایه‌گذاری';
      }

      return {
        type: 'INCOME',
        category: cat,
        title,
        confidence: 0.95,
        reason: 'کلمات کلیدی صریح دریافت وجه، فروش یا درآمد'
      };
    }

    // 5. Expense
    const hasExpenseVerb =
      lower.includes('پرداخت') ||
      lower.includes('خریدم') ||
      lower.includes('خرج') ||
      isServiceRide ||
      lower.includes('بنزین') ||
      lower.includes('قهوه') ||
      lower.includes('اجاره') ||
      lower.includes('قبض') ||
      lower.includes('کارمزد') ||
      lower.includes('دادم');

    if (hasExpenseVerb) {
      let cat = 'عمومی';
      let title = 'پرداخت هزینه';

      if (isServiceRide) {
        cat = 'حمل‌ونقل';
        title = 'کرایه تاکسی / اسنپ';
      } else if (lower.includes('قهوه') || lower.includes('کافه')) {
        cat = 'غذا و رستوران';
        title = 'کافه و قهوه';
      } else if (lower.includes('غذا') || lower.includes('ناهار') || lower.includes('شام')) {
        cat = 'غذا و رستوران';
        title = 'غذا و رستوران';
      } else if (lower.includes('سوپرمارکت') || lower.includes('خرید')) {
        cat = 'خرید روزمره';
        title = 'خرید سوپرمارکت';
      } else if (lower.includes('بنزین')) {
        cat = 'حمل‌ونقل';
        title = 'بنزین و سوخت';
      } else if (lower.includes('اینترنت') || lower.includes('اشتراک')) {
        cat = 'قبوض و اشتراک';
        title = 'اینترنت و اشتراک';
      } else if (lower.includes('اجاره')) {
        cat = 'مسکن و اجاره';
        title = 'اجاره مسکن';
      } else if (lower.includes('دارو') || lower.includes('دکتر')) {
        cat = 'سلامت و درمان';
        title = 'دارو و درمان';
      }

      return {
        type: 'EXPENSE',
        category: cat,
        title,
        confidence: 0.95,
        reason: 'کلمات کلیدی صریح پرداخت یا خرید'
      };
    }

    return {
      type: 'UNKNOWN',
      category: 'عمومی',
      title: norm.slice(0, 30),
      confidence: 0.3,
      reason: 'نوع تراکنش مشخص نیست'
    };
  }
}
