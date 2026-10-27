import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import {
  analyzeBankSms,
  computeSmsFingerprint,
  createDefaultBankProfiles,
  evaluateSampleForProfile,
  loadBankProfiles,
  maskSensitiveBankData,
  normalizeSmsPreserveLines,
  parseBankSms,
  revalidateBankProfile,
  saveBankProfiles,
} from '../persianUtils.ts';
import type { BankProfile, ExpenseItem } from '../../types.ts';

function getActiveProfiles(): BankProfile[] {
  const profiles = createDefaultBankProfiles();
  return profiles.map((p) => ({
    ...p,
    status: 'ACTIVE',
    samples:
      p.samples.length > 0
        ? p.samples.map((s) => ({ ...s, userVerified: true }))
        : [
            {
              id: `verified_${p.id}`,
              rawText: 'verified_placeholder',
              isSynthetic: true,
              parseStatus: 'VALID_TRANSACTION',
              messageCategory: 'SUCCESSFUL_TRANSACTION',
              detectedKind: 'WITHDRAWAL',
              extractedCurrency: 'RIAL',
              extractedAmountTomans: 50000,
              reason: 'Verified for test',
              userVerified: true,
            },
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
    // Note the hyphen in 1403/08/14-11:35 which previously caused deposit to be misread or vice versa
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
    // In Mellat SMS, 610433******1234 appears BEFORE the amount
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

    // In Saderat compact SMS, 12-digit account number 300325283197 appears on line 1
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
    // Neither selectedBankId nor matching senderId provided
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
    assert.equal(tomanRes.amountInTomans, 50000); // NOT divided by 10!
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
    // Mixed Arabic digits (٥٠٠٬٠٠٠) and Arabic Kaf/Yeh (بانك ملت / پيگيري)
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

    // Passing Refah SMS while 'mellat' is selected must be rejected
    const wrongBankRes = analyzeBankSms(refahSms, {
      selectedBankId: 'mellat',
      senderId: '98200033',
      profiles,
    });
    assert.equal(wrongBankRes.status, 'NEEDS_REVIEW');
    assert.equal(wrongBankRes.canAutoRegister, false);

    // Passing Refah SMS while 'refah' is selected succeeds
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
    // Missing ریال/تومان on the transaction amount
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

    // Remove the withdrawal template that sample_mellat_1 depends on
    const modified = revalidateBankProfile({
      ...mellat,
      templates: mellat.templates.filter(
        (t) => t.id !== 'mellat_withdrawal_v1'
      ),
    });

    assert.equal(modified.version, 2);
    assert.equal(modified.status, 'NEEDS_REVISION');
    assert.equal(modified.samples[0].userVerified, false);

    // Ensure unverified profile blocks auto-registration of new messages
    const newMsgCheck = analyzeBankSms(mellat.samples[0].rawText, {
      selectedBankId: 'mellat',
      senderId: '98200033',
      profiles: [modified],
    });
    assert.equal(newMsgCheck.canAutoRegister, false);
  });

  it('Regression: Unverified default profile MUST require manual review before auto-registration', () => {
    const defaultProfiles = createDefaultBankProfiles(); // status is NEEDS_VERIFICATION
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
});
