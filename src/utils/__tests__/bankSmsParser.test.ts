import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import {
  analyzeBankSms,
  buildValidatedExpenseEntry,
  computeSmsFingerprint,
  createDefaultBankProfiles,
  createPresetBankProfile,
  evaluateSampleForProfile,
  evaluateSenderPolicy,
  isTemplateVerifiedForAutoRegister,
  loadBankProfiles,
  maskSensitiveBankData,
  normalizeSmsPreserveLines,
  parseBankSms,
  resolveFinancialTypeFromKind,
  revalidateBankProfile,
  saveBankProfiles,
  updateBankTemplateRule,
} from '../persianUtils.ts';
import type { BankProfile, ExpenseItem } from '../../types.ts';

function getActiveProfiles(): BankProfile[] {
  const profiles = createDefaultBankProfiles();
  return profiles.map((p) => ({
    ...p,
    status: 'ACTIVE',
    templates: p.templates.map((t) => ({
      ...t,
      verifiedByUser: true,
      verifiedVersion: t.version,
    })),
    samples: [
      ...p.samples.map((s) => ({
        ...s,
        matchedTemplateVersion: 1,
        userVerified: true,
      })),
      ...p.templates
        .filter((t) => !p.samples.some((s) => s.matchedTemplateId === t.id))
        .map((t) => ({
          id: `verified_${p.id}_${t.id}`,
          rawText: 'verified_template_sample',
          isSynthetic: true,
          parseStatus: 'VALID_TRANSACTION' as const,
          messageCategory: 'SUCCESSFUL_TRANSACTION' as const,
          detectedKind: t.expectedKind,
          extractedCurrency: 'RIAL' as const,
          extractedAmountTomans: 50000,
          matchedTemplateId: t.id,
          matchedTemplateVersion: t.version,
          reason: 'Verified for template in test suite',
          userVerified: true,
        })),
    ],
  }));
}

describe('Bank SMS Parser Security & Accuracy Suite (20 Mandatory Scenarios + Regressions)', () => {
  it('1. Promotional SMS containing money amounts MUST NOT become a transaction', () => {
    const profiles = getActiveProfiles();
    const promoSms =
      'بانک ملت\nجشنواره پاییزی! با خرید حداقل ۵۰۰,۰۰۰ ریال در قرعه کشی ۵۰۰,۰۰۰,۰۰۰ ریال شرکت کنید. لغو11';
    const res = analyzeBankSms(promoSms, {
      selectedBankId: 'mellat',
      senderId: '98200033',
      profiles,
    });
    assert.equal(res.status, 'NOT_A_TRANSACTION');
    assert.equal(res.messageCategory, 'PROMOTIONAL');
    assert.equal(res.canAutoRegister, false);
    assert.equal(parseBankSms(promoSms, { selectedBankId: 'mellat', profiles }), null);
  });

  it('2. OTP / Dynamic password SMS containing numbers and transaction amount MUST NOT become a transaction', () => {
    const profiles = getActiveProfiles();
    const otpSms =
      'بانک ملت\nرمز پویا: ۸۴۹۲۰۱\nمبلغ: ۱,۵۰۰,۰۰۰ ریال\nکارت: ۶۱۰۴۳۳******۱۲۳۴';
    const res = analyzeBankSms(otpSms, {
      selectedBankId: 'mellat',
      senderId: '98200033',
      profiles,
    });
    assert.equal(res.status, 'NOT_A_TRANSACTION');
    assert.equal(res.messageCategory, 'OTP');
    assert.equal(res.canAutoRegister, false);
  });

  it('3. Informational SMS containing balance or account number without transaction action MUST NOT become a transaction', () => {
    const profiles = getActiveProfiles();
    const infoSms =
      'بانک ملی ایران\nمشتری گرامی، حساب: ۰۱*****۱۲۳۴\nمانده: ۴۵,۸۹۰,۰۰۰ ریال\n۱۴۰۳/۰۸/۱۴-۱۱:۳۵';
    const res = analyzeBankSms(infoSms, {
      selectedBankId: 'melli',
      senderId: '98100064',
      profiles,
    });
    assert.equal(res.status, 'NOT_A_TRANSACTION');
    assert.equal(res.messageCategory, 'INFORMATIONAL');
    assert.equal(res.canAutoRegister, false);
  });

  it('4. Successful transaction SMS matching a verified template on an ACTIVE profile MUST be accurately parsed', () => {
    const profiles = getActiveProfiles();
    const mellatSms =
      'بانک ملت\nبرداشت از: ۶۱۰۴۳۳******۱۲۳۴\nمبلغ: ۵۰۰,۰۰۰ ریال\nمانده: ۸,۴۲۰,۰۰۰ ریال\nتاریخ: ۱۴۰۳/۰۹/۱۰\nساعت: ۱۸:۴۵\nپیگیری: ۹۸۷۶۵۴';
    const res = analyzeBankSms(mellatSms, {
      selectedBankId: 'mellat',
      senderId: '98200033',
      profiles,
    });
    assert.equal(res.status, 'VALID_TRANSACTION');
    assert.equal(res.transactionKind, 'WITHDRAWAL');
    assert.equal(res.currency, 'RIAL');
    assert.equal(res.amountOriginal, 500000);
    assert.equal(res.amountInTomans, 50000);
    assert.equal(res.balanceInTomans, 842000);
    assert.equal(res.trackingCode, '987654');
    assert.equal(res.canAutoRegister, true);
  });

  it('5. Deposit MUST NOT be misclassified as Withdrawal and vice versa (even with hyphen in date/time)', () => {
    const profiles = getActiveProfiles();
    const melliDeposit =
      'بانک ملی ایران\nواریز: ۱,۲۵۰,۰۰۰ ریال\nبه: ۰۱*****۱۲۳۴\nمانده: ۴۵,۸۹۰,۰۰۰ ریال\n۱۴۰۳/۰۸/۱۴-۱۱:۳۵\nپیگیری: ۸۸۷۷۱۱';
    const depRes = analyzeBankSms(melliDeposit, {
      selectedBankId: 'melli',
      senderId: '98100064',
      profiles,
    });
    assert.equal(depRes.status, 'VALID_TRANSACTION');
    assert.equal(depRes.transactionKind, 'DEPOSIT');
    assert.equal(depRes.financialType, 'income');
    assert.equal(depRes.amountInTomans, 125000);

    const melliWithdrawal =
      'بانک ملی ایران\nبرداشت: ۹۰۰,۰۰۰ ریال\nاز: ۰۱*****۱۲۳۴\nمانده: ۴۴,۹۹۰,۰۰۰ ریال\n۱۴۰۳/۰۸/۱۴-۱۲:۱۰\nپیگیری: ۸۸۷۷۱۲';
    const wdRes = analyzeBankSms(melliWithdrawal, {
      selectedBankId: 'melli',
      senderId: '98100064',
      profiles,
    });
    assert.equal(wdRes.status, 'VALID_TRANSACTION');
    assert.equal(wdRes.transactionKind, 'WITHDRAWAL');
    assert.equal(wdRes.financialType, 'expense');
    assert.equal(wdRes.amountInTomans, 90000);
  });

  it('6. Transaction amount and account balance MUST NEVER be confused with each other', () => {
    const profiles = getActiveProfiles();
    const bluSms =
      'بلو\nانتقال پل\nمبلغ: ۱,۵۰۰,۰۰۰ ریال\nبه: علیرضا یوسفی\nمانده: ۶۲,۶۶۱,۴۵۲ ریال\nکد پیگیری: ۹۸۷۶۵۴۳۲۱\n1403/09/10 11:15';
    const res = analyzeBankSms(bluSms, {
      selectedBankId: 'blu',
      senderId: 'BLU',
      profiles,
    });
    assert.equal(res.status, 'VALID_TRANSACTION');
    assert.equal(res.amountInTomans, 150000);
    assert.equal(res.balanceInTomans, 6266145);
    assert.notEqual(res.amountInTomans, res.balanceInTomans);
  });

  it('7. Card prefix, account number, and tracking code MUST NEVER be extracted as transaction amount', () => {
    const profiles = getActiveProfiles();
    const mellatSms =
      'بانک ملت\nبرداشت از: ۶۱۰۴۳۳******۱۲۳۴\nمبلغ: ۵۰۰,۰۰۰ ریال\nمانده: ۸,۴۲۰,۰۰۰ ریال\nپیگیری: ۹۸۷۶۵۴';
    const res = analyzeBankSms(mellatSms, {
      selectedBankId: 'mellat',
      senderId: '98200033',
      profiles,
    });
    assert.equal(res.amountInTomans, 50000);
    assert.notEqual(res.amountOriginal, 610433);
    assert.notEqual(res.amountOriginal, 987654);

    const saderatCompact =
      '300325283197\n1,698,500+\n1403/8/5-11:43\nمانده:32,317,981';
    const sadRes = analyzeBankSms(saderatCompact, {
      selectedBankId: 'saderat',
      senderId: '98200060',
      profiles,
    });
    assert.equal(sadRes.status, 'VALID_TRANSACTION');
    assert.equal(sadRes.transactionKind, 'DEPOSIT');
    assert.equal(sadRes.amountOriginal, 1698500);
    assert.equal(sadRes.amountInTomans, 169850);
    assert.equal(sadRes.balanceInTomans, 3231798);
  });

  it('8. Failed transaction SMS MUST NOT be registered as a successful transaction', () => {
    const profiles = getActiveProfiles();
    const failedSms =
      'بانک ملت\nبرداشت ناموفق از: ۶۱۰۴۳۳******۱۲۳۴\nمبلغ: ۵۰۰,۰۰۰ ریال\nعدم موجودی کافی\nپیگیری: ۱۱۱۲۲۲';
    const res = analyzeBankSms(failedSms, {
      selectedBankId: 'mellat',
      senderId: '98200033',
      profiles,
    });
    assert.equal(res.status, 'NOT_A_TRANSACTION');
    assert.equal(res.messageCategory, 'FAILED_TRANSACTION');
    assert.equal(res.transactionKind, 'FAILED');
    assert.equal(res.canAutoRegister, false);
  });

  it('9. SMS from an unknown or incompatible sender MUST be flagged for manual review (NEEDS_REVIEW)', () => {
    const profiles = getActiveProfiles();
    const mellatSms =
      'بانک ملت\nبرداشت از: ۶۱۰۴۳۳******۱۲۳۴\nمبلغ: ۵۰۰,۰۰۰ ریال\nمانده: ۸,۴۲۰,۰۰۰ ریال\nپیگیری: ۹۸۷۶۵۴';
    const res = analyzeBankSms(mellatSms, {
      selectedBankId: 'mellat',
      senderId: '+989120009999', // Unregistered personal number
      profiles,
    });
    assert.equal(res.senderMatched, false);
    assert.equal(res.status, 'NEEDS_REVIEW');
    assert.equal(res.canAutoRegister, false);
  });

  it('10. Unselected bank MUST NOT be guessed from message text alone', () => {
    const profiles = getActiveProfiles();
    const mellatSms =
      'بانک ملت\nبرداشت از: ۶۱۰۴۳۳******۱۲۳۴\nمبلغ: ۵۰۰,۰۰۰ ریال\nمانده: ۸,۴۲۰,۰۰۰ ریال\nپیگیری: ۹۸۷۶۵۴';
    const res = analyzeBankSms(mellatSms, {
      profiles,
    });
    assert.equal(res.status, 'NEEDS_REVIEW');
    assert.equal(res.bankId, undefined);
    assert.equal(res.canAutoRegister, false);
  });

  it('11. Unknown / unsupported format MUST NOT auto-register as a transaction', () => {
    const profiles = getActiveProfiles();
    const weirdFormatSms =
      'بانک ملت\nعملیات ویژه برداشت وجه بدون قالب استاندارد ۱۲۳۴۵۶۷۸۹';
    const res = analyzeBankSms(weirdFormatSms, {
      selectedBankId: 'mellat',
      senderId: '98200033',
      profiles,
    });
    assert.equal(res.status, 'UNSUPPORTED_FORMAT');
    assert.equal(res.canAutoRegister, false);
  });

  it('12. Rial and Toman units MUST be handled accurately without double conversion', () => {
    const profiles = getActiveProfiles();
    const rialSms =
      'بانک ملت\nبرداشت از: ۶۱۰۴۳۳******۱۲۳۴\nمبلغ: ۵۰۰,۰۰۰ ریال\nمانده: ۸,۴۲۰,۰۰۰ ریال\nپیگیری: ۱۱۱';
    const rialRes = analyzeBankSms(rialSms, {
      selectedBankId: 'mellat',
      senderId: '98200033',
      profiles,
    });
    assert.equal(rialRes.currency, 'RIAL');
    assert.equal(rialRes.amountOriginal, 500000);
    assert.equal(rialRes.amountInTomans, 50000);

    const tomanSms =
      'بانک ملت\nبرداشت از: ۶۱۰۴۳۳******۱۲۳۴\nمبلغ: ۵۰,۰۰۰ تومان\nمانده: ۸۴۲,۰۰۰ تومان\nپیگیری: ۲۲۲';
    const tomanRes = analyzeBankSms(tomanSms, {
      selectedBankId: 'mellat',
      senderId: '98200033',
      profiles,
    });
    assert.equal(tomanRes.currency, 'TOMAN');
    assert.equal(tomanRes.amountOriginal, 50000);
    assert.equal(tomanRes.amountInTomans, 50000);
    assert.equal(tomanRes.balanceInTomans, 842000);
  });

  it('13. Two real transactions with the exact same amount MUST NOT be falsely deduplicated', () => {
    const profiles = getActiveProfiles();
    const sms1 =
      'بانک ملت\nبرداشت از: ۶۱۰۴۳۳******۱۲۳۴\nمبلغ: ۵۰۰,۰۰۰ ریال\nمانده: ۸,۴۲۰,۰۰۰ ریال\nتاریخ: ۱۴۰۳/۰۹/۱۰\nساعت: ۱۸:۴۵\nپیگیری: ۱۱۱۱۱۱';
    const sms2 =
      'بانک ملت\nبرداشت از: ۶۱۰۴۳۳******۱۲۳۴\nمبلغ: ۵۰۰,۰۰۰ ریال\nمانده: ۷,۹۲۰,۰۰۰ ریال\nتاریخ: ۱۴۰۳/۰۹/۱۰\nساعت: ۱۹:۱۰\nپیگیری: ۲۲۲۲۲۲';

    const res1 = analyzeBankSms(sms1, {
      selectedBankId: 'mellat',
      senderId: '98200033',
      profiles,
      existingExpenses: [],
    });
    assert.equal(res1.isDuplicate, false);
    assert.equal(res1.canAutoRegister, true);

    const recordedExpense1: ExpenseItem = {
      id: 'exp_1',
      title: res1.merchant,
      amount: res1.amountInTomans!,
      category: 'بانکی / پیامک',
      date: '۱۴۰۳/۰۹/۱۰',
      bankId: res1.bankId,
      bankName: res1.bankName,
      trackingCode: res1.trackingCode,
      messageFingerprint: res1.messageFingerprint,
    };

    const res2 = analyzeBankSms(sms2, {
      selectedBankId: 'mellat',
      senderId: '98200033',
      profiles,
      existingExpenses: [recordedExpense1],
    });
    assert.notEqual(res1.messageFingerprint, res2.messageFingerprint);
    assert.equal(res2.isDuplicate, false);
    assert.equal(res2.canAutoRegister, true);
  });

  it('14. Repeating the exact same SMS MUST be detected as duplicate and blocked from second registration', () => {
    const profiles = getActiveProfiles();
    const sms =
      'بانک ملت\nبرداشت از: ۶۱۰۴۳۳******۱۲۳۴\nمبلغ: ۵۰۰,۰۰۰ ریال\nمانده: ۸,۴۲۰,۰۰۰ ریال\nتاریخ: ۱۴۰۳/۰۹/۱۰\nساعت: ۱۸:۴۵\nپیگیری: ۹۸۷۶۵۴';

    const firstPass = analyzeBankSms(sms, {
      selectedBankId: 'mellat',
      senderId: '98200033',
      profiles,
      existingExpenses: [],
    });
    assert.equal(firstPass.isDuplicate, false);
    assert.equal(firstPass.canAutoRegister, true);

    const existing: ExpenseItem[] = [
      {
        id: 'exp_dup_1',
        title: firstPass.merchant,
        amount: firstPass.amountInTomans!,
        category: 'بانکی / پیامک',
        date: '۱۴۰۳/۰۹/۱۰',
        bankId: firstPass.bankId,
        bankName: firstPass.bankName,
        trackingCode: firstPass.trackingCode,
        messageFingerprint: firstPass.messageFingerprint,
      },
    ];

    const secondPass = analyzeBankSms(sms, {
      selectedBankId: 'mellat',
      senderId: '98200033',
      profiles,
      existingExpenses: existing,
    });
    assert.equal(secondPass.isDuplicate, true);
    assert.equal(secondPass.canAutoRegister, false);
  });

  it('15. Persian/Arabic digits, Arabic Yeh/Kaf, ZWNJ, and thousands separators MUST normalize cleanly', () => {
    const profiles = getActiveProfiles();
    const mixedSms =
      'بانك ملت\nبرداشت از: ٦١٠٤٣٣******١٢٣٤\nمبلغ: ٥٠٠٬٠٠٠ ريال\nمانده: ٨,٤٢٠,٠٠٠ ريال\nپيگيري: ٩٨٧٦٥٤';
    const res = analyzeBankSms(mixedSms, {
      selectedBankId: 'mellat',
      senderId: '98200033',
      profiles,
    });
    assert.equal(res.status, 'VALID_TRANSACTION');
    assert.equal(res.amountInTomans, 50000);
    assert.equal(res.balanceInTomans, 842000);
    assert.equal(res.trackingCode, '987654');
  });

  it('16. Verified samples and bank profiles MUST persist across storage reload', () => {
    const memoryStore: Record<string, string> = {};
    const mockStorage = {
      getItem: (k: string) => memoryStore[k] ?? null,
      setItem: (k: string, v: string) => {
        memoryStore[k] = v;
      },
    };

    const initial = createDefaultBankProfiles();
    const mellat = initial.find((p) => p.id === 'mellat')!;
    mellat.samples[0].userVerified = true;
    mellat.status = 'ACTIVE';

    saveBankProfiles(initial, mockStorage);
    const reloaded = loadBankProfiles(mockStorage);
    const reloadedMellat = reloaded.find((p) => p.id === 'mellat')!;

    assert.equal(reloadedMellat.status, 'ACTIVE');
    assert.equal(reloadedMellat.samples[0].userVerified, true);
  });

  it('17. Rules of one bank MUST NOT alter or accept SMS messages belonging to another bank', () => {
    const profiles = getActiveProfiles();
    const refahSms =
      'رفاه حساب 62****13\nبرداشت1,960,000R کد2000\nخرید فروشگاهی\nبمانده1,231,509R';

    const wrongBankRes = analyzeBankSms(refahSms, {
      selectedBankId: 'mellat',
      senderId: '98200033',
      profiles,
    });
    assert.equal(wrongBankRes.status, 'NEEDS_REVIEW');
    assert.equal(wrongBankRes.canAutoRegister, false);

    const rightBankRes = analyzeBankSms(refahSms, {
      selectedBankId: 'refah',
      senderId: 'RefahBank',
      profiles,
    });
    assert.equal(rightBankRes.status, 'VALID_TRANSACTION');
    assert.equal(rightBankRes.amountInTomans, 196000);
    assert.equal(rightBankRes.balanceInTomans, 123150);
  });

  it('18. Incomplete SMS or amount without currency unit MUST go to manual review (NEEDS_REVIEW)', () => {
    const profiles = getActiveProfiles();
    const ambiguousUnitSms =
      'بانک ملت\nبرداشت از: ۶۱۰۴۳۳******۱۲۳۴\nمبلغ: ۵۰۰,۰۰۰\nمانده: ۸,۴۲۰,۰۰۰ ریال\nپیگیری: ۹۸۷۶۵۴';
    const res = analyzeBankSms(ambiguousUnitSms, {
      selectedBankId: 'mellat',
      senderId: '98200033',
      profiles,
    });
    assert.equal(res.status, 'NEEDS_REVIEW');
    assert.equal(res.currency, 'UNKNOWN');
    assert.equal(res.canAutoRegister, false);
  });

  it('19. Multiple templates within a single bank profile MUST operate independently', () => {
    const profiles = getActiveProfiles();
    const bluPol =
      'بلو\nانتقال پل\nمبلغ: ۱,۵۰۰,۰۰۰ ریال\nبه: علیرضا یوسفی\nمانده: ۶۲,۶۶۱,۴۵۲ ریال\nکد پیگیری: ۹۸۷۶۵۴۳۲۱';
    const bluColloquial =
      'بلو\nبرداشت پول\nعلیرضا عزیز، 900,000 ریال از حساب شما پرید.\nموجودی: 64,161,452 ریال\n۱۰:۴۵\n۱۴۰۳.۰۸.۰۵';
    const bluStandard =
      'بلوبانک\nبرداشت: 450,000 ریال\nاز کارت: 621986******1234\nمانده: 3,420,000 ریال\n1403/09/12 18:24';

    const resPol = analyzeBankSms(bluPol, {
      selectedBankId: 'blu',
      senderId: 'BLU',
      profiles,
    });
    const resCol = analyzeBankSms(bluColloquial, {
      selectedBankId: 'blu',
      senderId: 'BLU',
      profiles,
    });
    const resStd = analyzeBankSms(bluStandard, {
      selectedBankId: 'blu',
      senderId: 'BluBank',
      profiles,
    });

    assert.equal(resPol.matchedTemplateId, 'blu_pol_transfer_v1');
    assert.equal(resPol.transactionKind, 'TRANSFER');
    assert.equal(resPol.amountInTomans, 150000);

    assert.equal(resCol.matchedTemplateId, 'blu_colloquial_withdrawal_v1');
    assert.equal(resCol.transactionKind, 'WITHDRAWAL');
    assert.equal(resCol.amountInTomans, 90000);

    assert.equal(resStd.matchedTemplateId, 'blu_standard_withdrawal_v1');
    assert.equal(resStd.transactionKind, 'WITHDRAWAL');
    assert.equal(resStd.amountInTomans, 45000);
  });

  it('20. Modifying or removing a template rule in a profile increments version and revalidates verified samples', () => {
    const profiles = getActiveProfiles();
    const mellat = profiles.find((p) => p.id === 'mellat')!;
    assert.equal(mellat.version, 1);
    assert.equal(mellat.status, 'ACTIVE');

    const modified = revalidateBankProfile({
      ...mellat,
      templates: mellat.templates.filter(
        (t) => t.id !== 'mellat_withdrawal_v1'
      ),
    });

    assert.equal(modified.version, 2);
    assert.equal(modified.status, 'NEEDS_REVISION');
    assert.equal(modified.samples[0].userVerified, false);

    const newMsgCheck = analyzeBankSms(mellat.samples[0].rawText, {
      selectedBankId: 'mellat',
      senderId: '98200033',
      profiles: [modified],
    });
    assert.equal(newMsgCheck.canAutoRegister, false);
  });

  it('Regression: Unverified default profile MUST require manual review before auto-registration', () => {
    const defaultProfiles = createDefaultBankProfiles();
    const mellatSms =
      'بانک ملت\nبرداشت از: ۶۱۰۴۳۳******۱۲۳۴\nمبلغ: ۵۰۰,۰۰۰ ریال\nمانده: ۸,۴۲۰,۰۰۰ ریال\nپیگیری: ۹۸۷۶۵۴';
    const res = analyzeBankSms(mellatSms, {
      selectedBankId: 'mellat',
      senderId: '98200033',
      profiles: defaultProfiles,
    });
    assert.equal(res.status, 'NEEDS_REVIEW');
    assert.equal(res.canAutoRegister, false);
    assert.equal(res.amountInTomans, 50000);
  });

  it('Regression: Sensitive 16-digit card numbers are masked by maskSensitiveBankData', () => {
    const raw = 'کارت 6104331234569876 مبلغ 500,000 ریال';
    const masked = maskSensitiveBankData(raw);
    assert.equal(masked.includes('6104331234569876'), false);
    assert.equal(masked.includes('610433******9876'), true);
  });

  // ============================================================================
  // Follow-up Audit Mandatory Regressions (Items 1 - 5)
  // ============================================================================

  it('Audit Regression 1: Ambiguous NEEDS_REVIEW message with known amount and bank MUST NOT register when transactionKind is UNKNOWN (UNKNOWN never defaults to expense)', () => {
    const profiles = getActiveProfiles();
    assert.equal(resolveFinancialTypeFromKind('UNKNOWN'), undefined);
    assert.equal(resolveFinancialTypeFromKind('FAILED'), undefined);

    // Even if an analysis result has bankId, bankName, and amountInTomans, if transactionKind is UNKNOWN it MUST fail validation
    const ambiguousAnalysis = analyzeBankSms(
      'بانک ملت\nبرداشت از: ۶۱۰۴۳۳******۱۲۳۴\nمبلغ: ۵۰۰,۰۰۰\nمانده: ۸,۴۲۰,۰۰۰ ریال',
      {
        selectedBankId: 'mellat',
        senderId: '98200033',
        profiles,
      }
    );
    assert.equal(ambiguousAnalysis.status, 'NEEDS_REVIEW');
    assert.equal(ambiguousAnalysis.transactionKind, 'UNKNOWN');

    // Attempt to confirm without fixing transactionKind or currency MUST fail
    const invalidConfirm1 = buildValidatedExpenseEntry(
      {
        ...ambiguousAnalysis,
        amountInTomans: 50000, // Even if amountInTomans is present
      },
      undefined,
      profiles
    );
    assert.equal(invalidConfirm1.ok, false);
    assert.equal(invalidConfirm1.entry, undefined);

    // Attempt to confirm with currency fixed to RIAL, but transactionKind still UNKNOWN MUST fail
    const invalidConfirm2 = buildValidatedExpenseEntry(
      ambiguousAnalysis,
      {
        bankId: 'mellat',
        transactionKind: 'UNKNOWN',
        currency: 'RIAL',
        amountOriginal: 500000,
      },
      profiles
    );
    assert.equal(invalidConfirm2.ok, false);
    assert.equal(invalidConfirm2.entry, undefined);

    // Only when user explicitly specifies valid transactionKind (e.g. DEPOSIT) and currency (RIAL) does it succeed
    const validConfirm = buildValidatedExpenseEntry(
      ambiguousAnalysis,
      {
        bankId: 'mellat',
        transactionKind: 'DEPOSIT',
        currency: 'RIAL',
        amountOriginal: 500000,
      },
      profiles
    );
    assert.equal(validConfirm.ok, true);
    assert.equal(validConfirm.entry?.type, 'income');
    assert.equal(validConfirm.entry?.amount, 50000);
  });

  it('Audit Regression 2: A verified template MUST NOT activate another unverified template within the same ACTIVE bank profile', () => {
    const defaultProfiles = createDefaultBankProfiles();
    const mellat = defaultProfiles.find((p) => p.id === 'mellat')!;

    // User verifies ONLY the withdrawal sample (mellat_withdrawal_v1), NOT mellat_deposit_v1
    const partiallyVerifiedMellat: BankProfile = {
      ...mellat,
      status: 'ACTIVE',
      samples: mellat.samples.map((s) =>
        s.matchedTemplateId === 'mellat_withdrawal_v1'
          ? { ...s, userVerified: true, matchedTemplateVersion: 1 }
          : s
      ),
    };

    const wdTemplate = partiallyVerifiedMellat.templates.find(
      (t) => t.id === 'mellat_withdrawal_v1'
    )!;
    const depTemplate = partiallyVerifiedMellat.templates.find(
      (t) => t.id === 'mellat_deposit_v1'
    )!;

    assert.equal(
      isTemplateVerifiedForAutoRegister(partiallyVerifiedMellat, wdTemplate),
      true
    );
    assert.equal(
      isTemplateVerifiedForAutoRegister(partiallyVerifiedMellat, depTemplate),
      false
    );

    // Withdrawal SMS matches the verified template -> auto-registers
    const wdSms =
      'بانک ملت\nبرداشت از: ۶۱۰۴۳۳******۱۲۳۴\nمبلغ: ۵۰۰,۰۰۰ ریال\nمانده: ۸,۴۲۰,۰۰۰ ریال\nپیگیری: ۹۸۷۶۵۴';
    const wdRes = analyzeBankSms(wdSms, {
      selectedBankId: 'mellat',
      senderId: '98200033',
      profiles: [partiallyVerifiedMellat],
    });
    assert.equal(wdRes.status, 'VALID_TRANSACTION');
    assert.equal(wdRes.canAutoRegister, true);

    // Deposit SMS matches mellat_deposit_v1 which has NO verified sample -> MUST go to NEEDS_REVIEW and NOT auto-register
    const depSms =
      'بانک ملت\nواریز به: ۶۱۰۴۳۳******۱۲۳۴\nمبلغ: ۹۰۰,۰۰۰ ریال\nمانده: ۹,۳۲۰,۰۰۰ ریال\nپیگیری: ۹۸۷۶۵۵';
    const depRes = analyzeBankSms(depSms, {
      selectedBankId: 'mellat',
      senderId: '98200033',
      profiles: [partiallyVerifiedMellat],
    });
    assert.equal(depRes.matchedTemplateId, 'mellat_deposit_v1');
    assert.equal(depRes.status, 'NEEDS_REVIEW');
    assert.equal(depRes.canAutoRegister, false);
  });

  it('Audit Regression 3: Changing a template version or updating a template rule invalidates its prior verification', () => {
    const profiles = getActiveProfiles();
    const mellat = profiles.find((p) => p.id === 'mellat')!;
    const wdSms =
      'بانک ملت\nبرداشت از: ۶۱۰۴۳۳******۱۲۳۴\nمبلغ: ۵۰۰,۰۰۰ ریال\nمانده: ۸,۴۲۰,۰۰۰ ریال\nپیگیری: ۹۸۷۶۵۴';

    // Initially verified at template version 1
    const beforeUpdate = analyzeBankSms(wdSms, {
      selectedBankId: 'mellat',
      senderId: '98200033',
      profiles: [mellat],
    });
    assert.equal(beforeUpdate.status, 'VALID_TRANSACTION');
    assert.equal(beforeUpdate.canAutoRegister, true);

    // Update mellat_withdrawal_v1 via updateBankTemplateRule -> increments template version to 2 and invalidates prior verification
    const updatedMellat = updateBankTemplateRule(
      mellat,
      'mellat_withdrawal_v1',
      {
        name: 'قالب برداشت بهسازان ملت (ویرایش‌شده)',
      }
    );

    const updatedWdTemplate = updatedMellat.templates.find(
      (t) => t.id === 'mellat_withdrawal_v1'
    )!;
    assert.equal(updatedWdTemplate.version, 2);
    assert.equal(
      isTemplateVerifiedForAutoRegister(updatedMellat, updatedWdTemplate),
      false
    );

    const afterUpdate = analyzeBankSms(wdSms, {
      selectedBankId: 'mellat',
      senderId: '98200033',
      profiles: [updatedMellat],
    });
    assert.equal(afterUpdate.status, 'NEEDS_REVIEW');
    assert.equal(afterUpdate.canAutoRegister, false);
  });

  it('Audit Regression 4: Incompatible sender OR missing mandatory sender MUST block auto-registration', () => {
    const profiles = getActiveProfiles();
    const saman = profiles.find((p) => p.id === 'saman')!;
    // Saman has requireSenderMatch = true
    assert.equal(saman.requireSenderMatch, true);

    const samanSms =
      'بانک سامان\nخرید: ۳۵۰,۰۰۰ ریال\nاز کارت: ۶۲۱۹۸۶******۱۱۱۱\nمانده: ۴,۵۰۰,۰۰۰ ریال\nکد ارجاع: ۷۷۸۸۹۹';

    // 4a. Missing sender when requireSenderMatch === true -> MUST block auto-registration
    const missingSenderPolicy = evaluateSenderPolicy(saman, undefined, false);
    assert.equal(missingSenderPolicy.senderMatched, false);
    assert.equal(missingSenderPolicy.policyMode, 'MISSING_REQUIRED_SENDER');

    const resMissingSender = analyzeBankSms(samanSms, {
      selectedBankId: 'saman',
      senderId: undefined,
      profiles,
    });
    assert.equal(resMissingSender.senderMatched, false);
    assert.equal(resMissingSender.status, 'NEEDS_REVIEW');
    assert.equal(resMissingSender.canAutoRegister, false);

    // 4b. Incompatible sender -> MUST block auto-registration
    const mismatchPolicy = evaluateSenderPolicy(saman, '989121112233', false);
    assert.equal(mismatchPolicy.senderMatched, false);
    assert.equal(mismatchPolicy.policyMode, 'MISMATCHED_SENDER');

    const resWrongSender = analyzeBankSms(samanSms, {
      selectedBankId: 'saman',
      senderId: '989121112233',
      profiles,
    });
    assert.equal(resWrongSender.senderMatched, false);
    assert.equal(resWrongSender.status, 'NEEDS_REVIEW');
    assert.equal(resWrongSender.canAutoRegister, false);

    // 4c. Compatible registered sender -> succeeds
    const resValidSender = analyzeBankSms(samanSms, {
      selectedBankId: 'saman',
      senderId: 'SamanBank',
      profiles,
    });
    assert.equal(resValidSender.senderMatched, true);
    assert.equal(resValidSender.status, 'VALID_TRANSACTION');
    assert.equal(resValidSender.canAutoRegister, true);
  });

  it('Audit Regression 5: Duplicate SMS registration remains blocked in both auto-registration and manual review confirmation', () => {
    const profiles = getActiveProfiles();
    const sms =
      'بانک ملت\nبرداشت از: ۶۱۰۴۳۳******۱۲۳۴\nمبلغ: ۵۰۰,۰۰۰ ریال\nمانده: ۸,۴۲۰,۰۰۰ ریال\nتاریخ: ۱۴۰۳/۰۹/۱۰\nساعت: ۱۸:۴۵\nپیگیری: ۹۸۷۶۵۴';

    const first = analyzeBankSms(sms, {
      selectedBankId: 'mellat',
      senderId: '98200033',
      profiles,
      existingExpenses: [],
    });
    const firstValidated = buildValidatedExpenseEntry(first, undefined, profiles);
    assert.equal(firstValidated.ok, true);

    const existingExpenses: ExpenseItem[] = [
      {
        id: 'exp_existing_1',
        ...firstValidated.entry!,
      },
    ];

    const duplicateAnalysis = analyzeBankSms(sms, {
      selectedBankId: 'mellat',
      senderId: '98200033',
      profiles,
      existingExpenses,
    });
    assert.equal(duplicateAnalysis.isDuplicate, true);
    assert.equal(duplicateAnalysis.canAutoRegister, false);

    // Even if someone attempts manual review confirmation on a duplicate result, buildValidatedExpenseEntry blocks it
    const manualAttemptOnDuplicate = buildValidatedExpenseEntry(
      duplicateAnalysis,
      {
        bankId: 'mellat',
        transactionKind: 'WITHDRAWAL',
        currency: 'RIAL',
        amountOriginal: 500000,
      },
      profiles,
      existingExpenses
    );
    assert.equal(manualAttemptOnDuplicate.ok, false);
    assert.equal(manualAttemptOnDuplicate.entry, undefined);

    // Even if an SMS initially entered NEEDS_REVIEW (e.g. unselected bank so trackingCode duplicate wasn't flagged at Stage 4),
    // once the user selects 'mellat' in manual review, buildValidatedExpenseEntry catches the duplicate against existingExpenses
    const unselectedBankAnalysis = analyzeBankSms(sms, {
      profiles,
      existingExpenses: [], // simulate stale analysis where isDuplicate was false
    });
    const manualAttemptWithExistingList = buildValidatedExpenseEntry(
      {
        ...unselectedBankAnalysis,
        trackingCode: '987654',
        isDuplicate: false,
      },
      {
        bankId: 'mellat',
        transactionKind: 'WITHDRAWAL',
        currency: 'RIAL',
        amountOriginal: 500000,
      },
      profiles,
      existingExpenses
    );
    assert.equal(manualAttemptWithExistingList.ok, false);
    assert.equal(manualAttemptWithExistingList.entry, undefined);
  });

  it('Audit Regression 6: Template version and sample verification persist across reload, and stale/mismatched template versions in storage are sanitized on load', () => {
    const memoryStore: Record<string, string> = {};
    const mockStorage = {
      getItem: (k: string) => memoryStore[k] ?? null,
      setItem: (k: string, v: string) => {
        memoryStore[k] = v;
      },
    };

    const initial = createDefaultBankProfiles();
    const mellat = initial.find((p) => p.id === 'mellat')!;

    // Verify withdrawal template at version 2
    mellat.status = 'ACTIVE';
    mellat.templates = mellat.templates.map((t) =>
      t.id === 'mellat_withdrawal_v1'
        ? { ...t, version: 2, verifiedByUser: true, verifiedVersion: 2 }
        : t
    );
    mellat.samples = mellat.samples.map((s) =>
      s.matchedTemplateId === 'mellat_withdrawal_v1'
        ? { ...s, userVerified: true, matchedTemplateVersion: 2 }
        : s
    );

    saveBankProfiles(initial, mockStorage);
    const reloaded = loadBankProfiles(mockStorage);
    const reloadedMellat = reloaded.find((p) => p.id === 'mellat')!;
    const reloadedWdTpl = reloadedMellat.templates.find(
      (t) => t.id === 'mellat_withdrawal_v1'
    )!;
    const reloadedDepTpl = reloadedMellat.templates.find(
      (t) => t.id === 'mellat_deposit_v1'
    )!;

    assert.equal(reloadedMellat.status, 'ACTIVE');
    assert.equal(reloadedWdTpl.version, 2);
    assert.equal(reloadedWdTpl.verifiedByUser, true);
    assert.equal(reloadedWdTpl.verifiedVersion, 2);
    assert.equal(
      isTemplateVerifiedForAutoRegister(reloadedMellat, reloadedWdTpl),
      true
    );
    assert.equal(
      isTemplateVerifiedForAutoRegister(reloadedMellat, reloadedDepTpl),
      false
    );

    // Now simulate a corrupted/stale storage state where template version is 3,
    // but sample matchedTemplateVersion is still 2, while profile status claims to be ACTIVE
    const staleProfiles = reloaded.map((p) =>
      p.id === 'mellat'
        ? {
            ...p,
            status: 'ACTIVE' as const,
            templates: p.templates.map((t) =>
              t.id === 'mellat_withdrawal_v1'
                ? { ...t, version: 3, verifiedByUser: true, verifiedVersion: 3 }
                : t
            ),
          }
        : p
    );
    saveBankProfiles(staleProfiles, mockStorage);

    const sanitizedReload = loadBankProfiles(mockStorage);
    const sanitizedMellat = sanitizedReload.find((p) => p.id === 'mellat')!;
    const sanitizedWdTpl = sanitizedMellat.templates.find(
      (t) => t.id === 'mellat_withdrawal_v1'
    )!;

    // Sanitizer must detect version mismatch (tpl v3 vs sample v2), mark sample & template unverified, and downgrade ACTIVE status
    assert.equal(sanitizedWdTpl.verifiedByUser, false);
    assert.equal(sanitizedWdTpl.verifiedVersion, undefined);
    assert.equal(sanitizedMellat.samples[0].userVerified, false);
    assert.equal(sanitizedMellat.status, 'NEEDS_VERIFICATION');
    assert.equal(
      isTemplateVerifiedForAutoRegister(sanitizedMellat, sanitizedWdTpl),
      false
    );
  });

  it('Audit Regression 7: Enabling requireSenderMatch invalidates previously verified samples that had no senderId', () => {
    const defaultProfiles = createDefaultBankProfiles();
    const mellat = defaultProfiles.find((p) => p.id === 'mellat')!;

    // Suppose user verified a sample WITHOUT senderId while requireSenderMatch was false
    const profileWithSenderlessSample: BankProfile = {
      ...mellat,
      status: 'ACTIVE',
      requireSenderMatch: false,
      samples: mellat.samples.map((s) => ({
        ...s,
        senderId: undefined,
        userVerified: true,
        matchedTemplateVersion: 1,
      })),
    };

    // Now user enables requireSenderMatch = true and profile is revalidated
    const revalidated = revalidateBankProfile({
      ...profileWithSenderlessSample,
      requireSenderMatch: true,
    });

    assert.equal(revalidated.status, 'NEEDS_REVISION');
    assert.equal(revalidated.samples[0].userVerified, false);
    const wdTpl = revalidated.templates.find(
      (t) => t.id === 'mellat_withdrawal_v1'
    )!;
    assert.equal(isTemplateVerifiedForAutoRegister(revalidated, wdTpl), false);
  });

  it('Audit Regression 8: Visual Bank Selector presets (Melli 98300017, BluBank Pol Mode, Gardeshgari, Keshavarzi) bind custom sender and parse accurately', () => {
    const melliPreset = createPresetBankProfile('melli', '98300017');
    assert.equal(melliPreset.id, 'melli');
    assert.equal(melliPreset.senders[0], '98300017');

    const bluPolPreset = createPresetBankProfile('blu_pol', 'BLU');
    assert.equal(bluPolPreset.id, 'blu_pol');
    assert.equal(bluPolPreset.bankName, 'بلوبانک حالت پل');

    const gardeshgariPreset = createPresetBankProfile('gardeshgari', '982000110');
    const keshavarziPreset = createPresetBankProfile('keshavarzi', '9820009102');

    const profiles = [melliPreset, bluPolPreset, gardeshgariPreset, keshavarziPreset];

    const bluPolRes = analyzeBankSms(
      'بلو\nانتقال پل\nمبلغ: ۱,۵۰۰,۰۰۰ ریال\nبه: علیرضا یوسفی\nمانده: ۶۲,۶۶۱,۴۵۲ ریال\nکد پیگیری: ۹۸۷۶۵۴۳۲۱',
      {
        selectedBankId: 'blu_pol',
        senderId: 'BLU',
        profiles,
        allowUnverifiedPreview: true,
      }
    );
    assert.equal(bluPolRes.status, 'VALID_TRANSACTION');
    assert.equal(bluPolRes.transactionKind, 'TRANSFER');
    assert.equal(bluPolRes.amountInTomans, 150000);

    const gardeshgariRes = analyzeBankSms(
      'بانک گردشگری\nبرداشت: ۸۵۰,۰۰۰ ریال\nمانده: ۱۲,۴۰۰,۰۰۰ ریال\nپیگیری: ۴۴۵۵۶۶',
      {
        selectedBankId: 'gardeshgari',
        senderId: '982000110',
        profiles,
        allowUnverifiedPreview: true,
      }
    );
    assert.equal(gardeshgariRes.status, 'VALID_TRANSACTION');
    assert.equal(gardeshgariRes.transactionKind, 'WITHDRAWAL');
    assert.equal(gardeshgariRes.amountInTomans, 85000);

    const keshavarziRes = analyzeBankSms(
      'بانک کشاورزی\nبرداشت: ۶۲۰,۰۰۰ ریال\nمانده: ۹,۸۰۰,۰۰۰ ریال\nپیگیری: ۷۷۸۸۹۹',
      {
        selectedBankId: 'keshavarzi',
        senderId: '9820009102',
        profiles,
        allowUnverifiedPreview: true,
      }
    );
    assert.equal(keshavarziRes.status, 'VALID_TRANSACTION');
    assert.equal(keshavarziRes.transactionKind, 'WITHDRAWAL');
    assert.equal(keshavarziRes.amountInTomans, 62000);
  });
});
