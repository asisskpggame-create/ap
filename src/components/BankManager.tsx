import React, { useState } from 'react';
import {
  X,
  Building2,
  Plus,
  CheckCircle2,
  AlertTriangle,
  ShieldCheck,
  Trash2,
  Sparkles,
  Send,
  FileCheck2,
  RefreshCw,
  Check,
  XCircle,
} from 'lucide-react';
import type {
  BankProfile,
  BankSmsSampleRecord,
  BankSmsTemplateRule,
  SmsMessageCategory,
  SmsTransactionKind,
  StructuredSmsAnalysisResult,
} from '../types';
import {
  analyzeBankSms,
  evaluateSampleForProfile,
  formatToman,
  getPersianDateString,
  maskSensitiveBankData,
  revalidateBankProfile,
  translateParseStatus,
  translateProfileStatus,
  translateTransactionKind,
} from '../utils/persianUtils';

function translateCategory(cat: SmsMessageCategory): string {
  switch (cat) {
    case 'SUCCESSFUL_TRANSACTION':
      return 'تراکنش مالی موفق';
    case 'FAILED_TRANSACTION':
      return 'تراکنش ناموفق';
    case 'OTP':
      return 'رمز پویا / امنیتی';
    case 'PROMOTIONAL':
      return 'تبلیغاتی / جشنواره';
    case 'SECURITY_ALERT':
      return 'هشدار ورود / امنیتی';
    case 'INFORMATIONAL':
      return 'اطلاع‌رسانی / مانده‌گیری';
    default:
      return 'نامشخص';
  }
}

interface BankManagerProps {
  isOpen: boolean;
  onClose: () => void;
  profiles: BankProfile[];
  onUpdateProfiles: (profiles: BankProfile[]) => void;
  selectedBankId: string;
  onSelectBankId: (bankId: string) => void;
}

export const BankManager: React.FC<BankManagerProps> = ({
  isOpen,
  onClose,
  profiles,
  onUpdateProfiles,
  selectedBankId,
  onSelectBankId,
}) => {
  const [activeTab, setActiveTab] = useState<'samples' | 'rules' | 'new_bank'>('samples');

  // Sample testing state
  const [sampleSender, setSampleSender] = useState('');
  const [sampleText, setSampleText] = useState('');
  const [previewResult, setPreviewResult] = useState<StructuredSmsAnalysisResult | null>(null);
  const [feedbackMsg, setFeedbackMsg] = useState<{
    type: 'success' | 'warning' | 'error';
    text: string;
  } | null>(null);

  // Sender addition state
  const [newSenderInput, setNewSenderInput] = useState('');

  // Custom template addition state
  const [newRuleName, setNewRuleName] = useState('');
  const [newRuleKind, setNewRuleKind] = useState<SmsTransactionKind>('PURCHASE');
  const [newRuleKeywords, setNewRuleKeywords] = useState('برداشت, خرید, پرداخت');

  // New Bank creation state
  const [customBankName, setCustomBankName] = useState('');
  const [customBankSenders, setCustomBankSenders] = useState('');
  const [customDebitKeywords, setCustomDebitKeywords] = useState('برداشت, خرید, انتقال');
  const [customCreditKeywords, setCustomCreditKeywords] = useState('واریز, واریزی');

  if (!isOpen) return null;

  const currentProfile =
    profiles.find((p) => p.id === selectedBankId) || profiles[0];

  const verifiedTransactionsCount = currentProfile
    ? currentProfile.samples.filter(
        (s) =>
          s.userVerified &&
          s.parseStatus === 'VALID_TRANSACTION' &&
          Boolean(s.matchedTemplateId)
      ).length
    : 0;

  const handleAnalyzeSamplePreview = (e: React.FormEvent) => {
    e.preventDefault();
    if (!sampleText.trim() || !currentProfile) return;

    const result = analyzeBankSms(sampleText, {
      selectedBankId: currentProfile.id,
      senderId: sampleSender.trim() || currentProfile.senders[0] || undefined,
      profiles,
      allowUnverifiedPreview: true,
    });

    setPreviewResult(result);
    if (result.status === 'VALID_TRANSACTION') {
      setFeedbackMsg({
        type: 'success',
        text: 'تحلیل نمونه با موفقیت انجام شد. لطفاً مقادیر استخراج‌شده را بررسی و در صورت صحت، تأیید کنید.',
      });
    } else if (result.status === 'NOT_A_TRANSACTION') {
      setFeedbackMsg({
        type: 'warning',
        text: `این پیام تراکنش مالی نیست (${translateCategory(result.messageCategory)}): ${result.reason}`,
      });
    } else {
      setFeedbackMsg({
        type: 'error',
        text: `نیازمند بررسی یا اصلاح الگو: ${result.reason}`,
      });
    }
  };

  const handleVerifyAndSaveSample = (verified: boolean) => {
    if (!currentProfile || !previewResult || !sampleText.trim()) return;

    const evaluatedSample = evaluateSampleForProfile(
      currentProfile,
      sampleText.trim(),
      sampleSender.trim() || undefined,
      false
    );

    const newSample: BankSmsSampleRecord = {
      ...evaluatedSample,
      userVerified: verified && previewResult.status === 'VALID_TRANSACTION',
      verifiedAt: verified ? getPersianDateString() : undefined,
    };

    const updatedSenders = [...currentProfile.senders];
    if (
      verified &&
      sampleSender.trim() &&
      !updatedSenders.includes(sampleSender.trim())
    ) {
      updatedSenders.push(sampleSender.trim());
    }

    const updatedSamples = [newSample, ...currentProfile.samples];
    const hasVerifiedValidSample = updatedSamples.some(
      (s) =>
        s.userVerified &&
        s.parseStatus === 'VALID_TRANSACTION' &&
        Boolean(s.matchedTemplateId)
    );

    const nextProfile: BankProfile = {
      ...currentProfile,
      senders: updatedSenders,
      samples: updatedSamples,
      status:
        currentProfile.status === 'ACTIVE' && hasVerifiedValidSample
          ? 'ACTIVE'
          : hasVerifiedValidSample
          ? 'NEEDS_VERIFICATION'
          : 'NEEDS_SAMPLES',
      updatedAt: getPersianDateString(),
    };

    onUpdateProfiles(
      profiles.map((p) => (p.id === currentProfile.id ? nextProfile : p))
    );

    setFeedbackMsg({
      type: verified ? 'success' : 'warning',
      text: verified
        ? 'نمونه پیامک واقعی با موفقیت تأیید و در پروفایل بانک ذخیره شد. اکنون می‌توانید ثبت خودکار را فعال کنید.'
        : 'نمونه به‌عنوان تأییدنشده ثبت شد تا در اصلاح الگوها بررسی شود.',
    });
    setSampleText('');
    setPreviewResult(null);
  };

  const handleToggleExistingSampleVerify = (sampleId: string) => {
    if (!currentProfile) return;

    const updatedSamples = currentProfile.samples.map((s) => {
      if (s.id !== sampleId) return s;
      const nextVerified = !s.userVerified;
      return {
        ...s,
        userVerified: nextVerified && s.parseStatus === 'VALID_TRANSACTION',
        verifiedAt: nextVerified ? getPersianDateString() : undefined,
      };
    });

    const hasVerified = updatedSamples.some(
      (s) =>
        s.userVerified &&
        s.parseStatus === 'VALID_TRANSACTION' &&
        Boolean(s.matchedTemplateId)
    );

    const updatedProfile: BankProfile = {
      ...currentProfile,
      samples: updatedSamples,
      status: hasVerified
        ? currentProfile.status === 'ACTIVE'
          ? 'ACTIVE'
          : 'NEEDS_VERIFICATION'
        : 'NEEDS_VERIFICATION',
      updatedAt: getPersianDateString(),
    };

    onUpdateProfiles(
      profiles.map((p) => (p.id === currentProfile.id ? updatedProfile : p))
    );
  };

  const handleDeleteSample = (sampleId: string) => {
    if (!currentProfile) return;
    const updatedSamples = currentProfile.samples.filter(
      (s) => s.id !== sampleId
    );
    const hasVerified = updatedSamples.some(
      (s) =>
        s.userVerified &&
        s.parseStatus === 'VALID_TRANSACTION' &&
        Boolean(s.matchedTemplateId)
    );
    const updatedProfile: BankProfile = {
      ...currentProfile,
      samples: updatedSamples,
      status: hasVerified ? currentProfile.status : 'NEEDS_SAMPLES',
      updatedAt: getPersianDateString(),
    };
    onUpdateProfiles(
      profiles.map((p) => (p.id === currentProfile.id ? updatedProfile : p))
    );
  };

  const handleToggleProfileActive = () => {
    if (!currentProfile) return;

    if (currentProfile.status === 'ACTIVE') {
      const deactivated: BankProfile = {
        ...currentProfile,
        status:
          verifiedTransactionsCount > 0 ? 'NEEDS_VERIFICATION' : 'NEEDS_SAMPLES',
        updatedAt: getPersianDateString(),
      };
      onUpdateProfiles(
        profiles.map((p) => (p.id === currentProfile.id ? deactivated : p))
      );
      setFeedbackMsg({
        type: 'warning',
        text: `ثبت خودکار برای «${currentProfile.bankName}» غیرفعال شد. پیامک‌های بعدی به حالت بررسی دستی می‌روند.`,
      });
      return;
    }

    if (verifiedTransactionsCount === 0) {
      setFeedbackMsg({
        type: 'error',
        text: 'برای فعال‌سازی پردازش خودکار، حداقل یک نمونه پیامک تراکنش معتبر باید توسط شما آزمایش و تأیید شده باشد.',
      });
      return;
    }

    const activated: BankProfile = {
      ...currentProfile,
      status: 'ACTIVE',
      updatedAt: getPersianDateString(),
    };

    onUpdateProfiles(
      profiles.map((p) => (p.id === currentProfile.id ? activated : p))
    );
    setFeedbackMsg({
      type: 'success',
      text: `پروفایل «${currentProfile.bankName}» (نسخه v${currentProfile.version}) فعال شد! پیامک‌های منطبق با الگوی تأییدشده به‌صورت خودکار ثبت می‌شوند.`,
    });
  };

  const handleAddSender = (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentProfile || !newSenderInput.trim()) return;
    const sender = newSenderInput.trim();
    if (currentProfile.senders.includes(sender)) return;

    const draftProfile: BankProfile = {
      ...currentProfile,
      senders: [...currentProfile.senders, sender],
    };
    const revalidated = revalidateBankProfile(draftProfile);
    onUpdateProfiles(
      profiles.map((p) => (p.id === currentProfile.id ? revalidated : p))
    );
    setNewSenderInput('');
    setFeedbackMsg({
      type: 'success',
      text: `فرستنده «${sender}» اضافه شد و نمونه‌های قبلی با نسخه v${revalidated.version} بازآزمایی شدند.`,
    });
  };

  const handleRemoveSender = (sender: string) => {
    if (!currentProfile) return;
    const draftProfile: BankProfile = {
      ...currentProfile,
      senders: currentProfile.senders.filter((s) => s !== sender),
    };
    const revalidated = revalidateBankProfile(draftProfile);
    onUpdateProfiles(
      profiles.map((p) => (p.id === currentProfile.id ? revalidated : p))
    );
  };

  const handleAddCustomTemplate = (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentProfile || !newRuleName.trim()) return;

    const keywords = newRuleKeywords
      .split(/[,،]/)
      .map((k) => k.trim())
      .filter(Boolean);

    if (keywords.length === 0) return;

    const kwAlternation = keywords.join('|');
    const newRule: BankSmsTemplateRule = {
      id: `${currentProfile.id}_custom_${Date.now()}`,
      version: currentProfile.version + 1,
      name: newRuleName.trim(),
      expectedKind: newRuleKind,
      directionAnchors: keywords,
      amountPatterns: [
        `(?:مبلغ|${kwAlternation})\\s*:?\\s*([\\d,]+)\\s*(ریال|تومان)`,
        `([\\d,]+)\\s*(ریال|تومان)`,
      ],
      currencySource: 'EXPLICIT_UNIT',
      balancePatterns: [`(?:مانده|موجودی)\\s*:?\\s*([\\d,]+)\\s*(ریال|تومان)?`],
      trackingPatterns: [`(?:پیگیری|ردیف|مرجع|سند)\\s*:?\\s*([\\d\\w]+)`],
      isCustom: true,
    };

    const draftProfile: BankProfile = {
      ...currentProfile,
      templates: [...currentProfile.templates, newRule],
    };
    const revalidated = revalidateBankProfile(draftProfile);
    onUpdateProfiles(
      profiles.map((p) => (p.id === currentProfile.id ? revalidated : p))
    );
    setNewRuleName('');
    setFeedbackMsg({
      type: 'success',
      text: `الگوی «${newRule.name}» اضافه شد (نسخه v${revalidated.version}). لطفاً یک نمونه پیامک واقعی برای تأیید آن ثبت کنید.`,
    });
  };

  const handleCreateCustomBank = (e: React.FormEvent) => {
    e.preventDefault();
    if (!customBankName.trim()) return;

    const id = `custom_bank_${Date.now()}`;
    const senders = customBankSenders
      .split(/[,،\s]+/)
      .map((s) => s.trim())
      .filter(Boolean);
    const debitKws = customDebitKeywords
      .split(/[,،]/)
      .map((k) => k.trim())
      .filter(Boolean);
    const creditKws = customCreditKeywords
      .split(/[,،]/)
      .map((k) => k.trim())
      .filter(Boolean);

    const debitAnchors = debitKws.length > 0 ? debitKws : ['برداشت', 'خرید', 'انتقال'];
    const creditAnchors = creditKws.length > 0 ? creditKws : ['واریز', 'واریزی'];

    const newBank: BankProfile = {
      id,
      bankName: customBankName.trim(),
      isCustom: true,
      status: 'DRAFT',
      version: 1,
      senders,
      requireSenderMatch: false,
      updatedAt: getPersianDateString(),
      templates: [
        {
          id: `${id}_debit_v1`,
          version: 1,
          name: `برداشت استاندارد ${customBankName.trim()}`,
          expectedKind: 'WITHDRAWAL',
          directionAnchors: debitAnchors,
          amountPatterns: [
            `(?:مبلغ|${debitAnchors.join('|')})\\s*:?\\s*([\\d,]+)\\s*(ریال|تومان)`,
            `([\\d,]+)\\s*(ریال|تومان)`,
          ],
          currencySource: 'EXPLICIT_UNIT',
          balancePatterns: [`(?:مانده|موجودی)\\s*:?\\s*([\\d,]+)\\s*(ریال|تومان)?`],
          trackingPatterns: [`(?:پیگیری|ردیف|مرجع)\\s*:?\\s*([\\d\\w]+)`],
          isCustom: true,
        },
        {
          id: `${id}_credit_v1`,
          version: 1,
          name: `واریز استاندارد ${customBankName.trim()}`,
          expectedKind: 'DEPOSIT',
          directionAnchors: creditAnchors,
          amountPatterns: [
            `(?:مبلغ|${creditAnchors.join('|')})\\s*:?\\s*([\\d,]+)\\s*(ریال|تومان)`,
            `([\\d,]+)\\s*(ریال|تومان)`,
          ],
          currencySource: 'EXPLICIT_UNIT',
          balancePatterns: [`(?:مانده|موجودی)\\s*:?\\s*([\\d,]+)\\s*(ریال|تومان)?`],
          trackingPatterns: [`(?:پیگیری|ردیف|مرجع)\\s*:?\\s*([\\d\\w]+)`],
          isCustom: true,
        },
      ],
      samples: [],
    };

    const nextProfiles = [...profiles, newBank];
    onUpdateProfiles(nextProfiles);
    onSelectBankId(newBank.id);
    setCustomBankName('');
    setCustomBankSenders('');
    setActiveTab('samples');
    setFeedbackMsg({
      type: 'success',
      text: `بانک «${newBank.bankName}» در حالت پیش‌نویس ساخته شد. اکنون یک نمونه پیامک واقعی وارد و تأیید کنید تا فعال شود.`,
    });
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-6 bg-slate-950/80 backdrop-blur-md animate-fade-in"
      role="dialog"
      aria-modal="true"
      aria-label="مدیریت پروفایل بانک‌ها و نمونه‌های پیامک"
    >
      <div className="relative w-full max-w-4xl max-h-[90vh] flex flex-col rounded-3xl bg-slate-900 border border-cyan-500/30 shadow-2xl shadow-cyan-950/50 overflow-hidden">
        {/* Modal Header */}
        <div className="flex items-center justify-between px-6 py-4 bg-slate-950/80 border-b border-slate-800">
          <div className="flex items-center space-x-3 space-x-reverse">
            <div className="p-2.5 rounded-2xl bg-cyan-500/15 border border-cyan-500/30 text-cyan-400">
              <Building2 className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-base sm:text-lg font-bold text-slate-100">
                مدیریت بانک‌های من و آزمایشگاه نمونه پیامک
              </h2>
              <p className="text-[11px] text-slate-400">
                انتخاب دستی بانک، تعریف سرشماره، تست نمونه پیامک واقعی و فعال‌سازی ثبت خودکار
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            className="p-2 rounded-xl bg-slate-800/80 text-slate-400 hover:text-white hover:bg-slate-700 transition"
            aria-label="بستن پنجره مدیریت بانک‌ها"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Top Bar: Bank Selector + Navigation Tabs */}
        <div className="px-6 py-3 bg-slate-900/90 border-b border-slate-800 flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center space-x-2 space-x-reverse flex-1 min-w-[220px]">
            <label className="text-xs text-slate-400 shrink-0">بانک انتخابی:</label>
            <select
              value={currentProfile?.id || ''}
              onChange={(e) => {
                onSelectBankId(e.target.value);
                setPreviewResult(null);
                setFeedbackMsg(null);
              }}
              className="flex-1 bg-slate-950 border border-slate-700 rounded-xl px-3 py-2 text-xs text-slate-100 focus:outline-none focus:border-cyan-500"
            >
              {profiles.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.bankName} — ({translateProfileStatus(p.status)} | v{p.version})
                </option>
              ))}
            </select>
          </div>

          <div className="flex items-center gap-1.5 bg-slate-950 p-1 rounded-xl border border-slate-800">
            <button
              type="button"
              onClick={() => setActiveTab('samples')}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium transition ${
                activeTab === 'samples'
                  ? 'bg-cyan-500 text-slate-950 font-bold'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              نمونه پیامک و تأیید ({currentProfile?.samples.length || 0})
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('rules')}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium transition ${
                activeTab === 'rules'
                  ? 'bg-cyan-500 text-slate-950 font-bold'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              فرستنده‌ها و الگوها ({currentProfile?.templates.length || 0})
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('new_bank')}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium flex items-center space-x-1 space-x-reverse transition ${
                activeTab === 'new_bank'
                  ? 'bg-emerald-500 text-slate-950 font-bold'
                  : 'text-emerald-400 hover:bg-emerald-500/10'
              }`}
            >
              <Plus className="w-3.5 h-3.5" />
              <span>افزودن بانک جدید</span>
            </button>
          </div>
        </div>

        {/* Scrollable Modal Body */}
        <div className="flex-1 overflow-y-auto p-6 space-y-5">
          {/* Selected Bank Summary & Activation Bar */}
          {currentProfile && activeTab !== 'new_bank' && (
            <div className="p-4 rounded-2xl bg-slate-950/90 border border-slate-800 flex flex-wrap items-center justify-between gap-4">
              <div className="space-y-1">
                <div className="flex items-center space-x-2 space-x-reverse">
                  <span className="font-bold text-slate-100 text-sm">
                    {currentProfile.bankName}
                  </span>
                  <span
                    className={`text-[10px] px-2.5 py-0.5 rounded-full font-bold ${
                      currentProfile.status === 'ACTIVE'
                        ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
                        : 'bg-amber-500/20 text-amber-300 border border-amber-500/30'
                    }`}
                  >
                    {translateProfileStatus(currentProfile.status)}
                  </span>
                  <span className="text-[10px] text-slate-500 font-mono">
                    نسخه الگو: v{currentProfile.version}
                  </span>
                </div>
                <p className="text-xs text-slate-400">
                  سرشماره‌های مجاز:{' '}
                  <span className="text-slate-300 font-mono">
                    {currentProfile.senders.length > 0
                      ? currentProfile.senders.join(' ، ')
                      : 'تعریف نشده'}
                  </span>{' '}
                  | نمونه‌های تراکنش تأییدشده:{' '}
                  <span className="text-cyan-400 font-bold">
                    {verifiedTransactionsCount} مورد
                  </span>
                </p>
              </div>

              <button
                type="button"
                onClick={handleToggleProfileActive}
                className={`px-4 py-2 rounded-xl text-xs font-bold flex items-center space-x-1.5 space-x-reverse transition ${
                  currentProfile.status === 'ACTIVE'
                    ? 'bg-rose-500/20 text-rose-300 border border-rose-500/30 hover:bg-rose-500/30'
                    : verifiedTransactionsCount > 0
                    ? 'bg-emerald-500 text-slate-950 shadow-lg shadow-emerald-500/20 hover:bg-emerald-400'
                    : 'bg-slate-800 text-slate-400 cursor-not-allowed'
                }`}
              >
                <ShieldCheck className="w-4 h-4" />
                <span>
                  {currentProfile.status === 'ACTIVE'
                    ? 'غیرفعال‌سازی ثبت خودکار'
                    : 'فعال‌سازی پردازش خودکار این بانک'}
                </span>
              </button>
            </div>
          )}

          {/* Feedback Alert */}
          {feedbackMsg && (
            <div
              className={`p-3.5 rounded-2xl text-xs flex items-center justify-between gap-2 ${
                feedbackMsg.type === 'success'
                  ? 'bg-emerald-500/15 text-emerald-300 border border-emerald-500/30'
                  : feedbackMsg.type === 'warning'
                  ? 'bg-amber-500/15 text-amber-300 border border-amber-500/30'
                  : 'bg-rose-500/15 text-rose-300 border border-rose-500/30'
              }`}
            >
              <span>{feedbackMsg.text}</span>
              <button
                type="button"
                onClick={() => setFeedbackMsg(null)}
                className="text-slate-400 hover:text-white"
              >
                <X className="w-4 h-4" />
              </button>
            </div>
          )}

          {/* TAB 1: Samples & Live Verification */}
          {activeTab === 'samples' && currentProfile && (
            <div className="space-y-6">
              {/* Sample Submission Form */}
              <form
                onSubmit={handleAnalyzeSamplePreview}
                className="p-4 rounded-2xl bg-slate-950/60 border border-slate-800 space-y-3"
              >
                <div className="flex items-center justify-between">
                  <h3 className="text-xs font-bold text-cyan-400 flex items-center space-x-1.5 space-x-reverse">
                    <Sparkles className="w-4 h-4" />
                    <span>آزمایش و ثبت نمونه پیامک واقعی برای {currentProfile.bankName}</span>
                  </h3>
                  <span className="text-[10px] text-slate-500">
                    مرحله ۱: تحلیل نمونه ← مرحله ۲: تأیید کاربر
                  </span>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                  <div>
                    <label className="text-[11px] text-slate-400 block mb-1">
                      سرشماره / فرستنده پیامک (اختیاری)
                    </label>
                    <input
                      type="text"
                      value={sampleSender}
                      onChange={(e) => setSampleSender(e.target.value)}
                      placeholder={
                        currentProfile.senders[0]
                          ? `مثلاً: ${currentProfile.senders[0]}`
                          : 'مثلاً: 9820004000'
                      }
                      className="w-full bg-slate-900 border border-slate-800 rounded-xl px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-cyan-500 font-mono"
                    />
                  </div>
                  <div className="sm:col-span-2">
                    <label className="text-[11px] text-slate-400 block mb-1">
                      متن کامل پیامک واقعی بانک
                    </label>
                    <textarea
                      rows={3}
                      value={sampleText}
                      onChange={(e) => setSampleText(e.target.value)}
                      placeholder="متن پیامک واقعی بانک را دقیقاً اینجا جای‌گذاری کنید..."
                      className="w-full bg-slate-900 border border-slate-800 rounded-xl p-3 text-xs text-slate-200 focus:outline-none focus:border-cyan-500 leading-relaxed"
                    />
                  </div>
                </div>

                <div className="flex justify-end">
                  <button
                    type="submit"
                    className="px-4 py-2 rounded-xl bg-cyan-500 hover:bg-cyan-400 text-slate-950 font-bold text-xs flex items-center space-x-1.5 space-x-reverse transition"
                  >
                    <Send className="w-3.5 h-3.5" />
                    <span>تحلیل نمونه و مشاهده خروجی</span>
                  </button>
                </div>
              </form>

              {/* Live Extraction Preview Card */}
              {previewResult && (
                <div className="p-4 rounded-2xl bg-slate-950 border-2 border-cyan-500/40 space-y-4">
                  <div className="flex items-center justify-between border-b border-slate-800 pb-2.5">
                    <span className="text-xs font-bold text-slate-100">
                      نتیجه تحلیل سیستم روی نمونه شما (پیش‌نمایش قبل از ذخیره)
                    </span>
                    <span
                      className={`text-[11px] px-2.5 py-0.5 rounded-full font-bold ${
                        previewResult.status === 'VALID_TRANSACTION'
                          ? 'bg-emerald-500/20 text-emerald-300'
                          : previewResult.status === 'NOT_A_TRANSACTION'
                          ? 'bg-rose-500/20 text-rose-300'
                          : 'bg-amber-500/20 text-amber-300'
                      }`}
                    >
                      {translateParseStatus(previewResult.status)}
                    </span>
                  </div>

                  <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs">
                    <div className="p-2.5 rounded-xl bg-slate-900 border border-slate-800">
                      <span className="text-[10px] text-slate-500 block">
                        دسته‌بندی پیام:
                      </span>
                      <span className="font-bold text-slate-200">
                        {translateCategory(previewResult.messageCategory)}
                      </span>
                    </div>
                    <div className="p-2.5 rounded-xl bg-slate-900 border border-slate-800">
                      <span className="text-[10px] text-slate-500 block">
                        نوع تراکنش:
                      </span>
                      <span className="font-bold text-cyan-400">
                        {translateTransactionKind(previewResult.transactionKind)}
                      </span>
                    </div>
                    <div className="p-2.5 rounded-xl bg-slate-900 border border-slate-800">
                      <span className="text-[10px] text-slate-500 block">
                        مبلغ تراکنش (تومان):
                      </span>
                      <span className="font-bold text-emerald-400">
                        {previewResult.amountInTomans
                          ? formatToman(previewResult.amountInTomans)
                          : 'استخراج نشد'}
                      </span>
                      {previewResult.amountOriginal && (
                        <span className="block text-[10px] text-slate-500">
                          (مقدار خام: {previewResult.amountOriginal.toLocaleString('fa-IR')}{' '}
                          {previewResult.currency === 'RIAL' ? 'ریال' : 'تومان'})
                        </span>
                      )}
                    </div>
                    <div className="p-2.5 rounded-xl bg-slate-900 border border-slate-800">
                      <span className="text-[10px] text-slate-500 block">
                        مانده حساب (تفکیک‌شده):
                      </span>
                      <span className="font-bold text-slate-300">
                        {previewResult.balanceInTomans !== undefined
                          ? formatToman(previewResult.balanceInTomans)
                          : '—'}
                      </span>
                    </div>
                  </div>

                  <div className="text-[11px] text-slate-400 bg-slate-900/70 p-2.5 rounded-xl border border-slate-800/80 flex flex-wrap items-center justify-between gap-2">
                    <span>علت تشخیص: {previewResult.reason}</span>
                    {previewResult.trackingCode && (
                      <span className="font-mono text-slate-300">
                        کد پیگیری: {previewResult.trackingCode}
                      </span>
                    )}
                  </div>

                  <div className="flex flex-wrap items-center justify-end gap-2 pt-1">
                    <button
                      type="button"
                      onClick={() => handleVerifyAndSaveSample(false)}
                      className="px-3.5 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs flex items-center space-x-1 space-x-reverse transition"
                    >
                      <XCircle className="w-4 h-4 text-amber-400" />
                      <span>خروجی نادرست است (ذخیره برای اصلاح الگو)</span>
                    </button>
                    {previewResult.status === 'VALID_TRANSACTION' && (
                      <button
                        type="button"
                        onClick={() => handleVerifyAndSaveSample(true)}
                        className="px-4 py-2 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold text-xs flex items-center space-x-1.5 space-x-reverse shadow-lg shadow-emerald-500/20 transition"
                      >
                        <Check className="w-4 h-4" />
                        <span>تأیید صحت خروجی و ذخیره در پروفایل بانک</span>
                      </button>
                    )}
                  </div>
                </div>
              )}

              {/* Saved Samples List */}
              <div className="space-y-2.5">
                <h4 className="text-xs font-bold text-slate-300 flex items-center space-x-1.5 space-x-reverse">
                  <FileCheck2 className="w-4 h-4 text-cyan-400" />
                  <span>نمونه‌های ثبت‌شده برای {currentProfile.bankName}</span>
                </h4>

                {currentProfile.samples.length === 0 ? (
                  <div className="p-6 rounded-2xl bg-slate-950/40 border border-slate-800/80 text-center text-xs text-slate-500">
                    هنوز نمونه پیامکی برای این بانک ثبت نشده است. یک نمونه واقعی بالا وارد کنید.
                  </div>
                ) : (
                  <div className="space-y-2.5">
                    {currentProfile.samples.map((sample) => (
                      <div
                        key={sample.id}
                        className="p-3.5 rounded-2xl bg-slate-950/70 border border-slate-800 flex flex-col sm:flex-row sm:items-center justify-between gap-3"
                      >
                        <div className="space-y-1.5 flex-1">
                          <div className="flex flex-wrap items-center gap-2">
                            <span
                              className={`text-[10px] px-2 py-0.5 rounded-full font-bold ${
                                sample.userVerified
                                  ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
                                  : 'bg-amber-500/20 text-amber-300 border border-amber-500/30'
                              }`}
                            >
                              {sample.userVerified
                                ? 'تأییدشده توسط کاربر'
                                : sample.isSynthetic
                                ? 'نمونه آزمایشی پیش‌فرض (تأییدنشده)'
                                : 'در انتظار تأیید کاربر'}
                            </span>
                            <span className="text-[11px] font-bold text-cyan-400">
                              {translateTransactionKind(sample.detectedKind)}
                            </span>
                            {sample.extractedAmountTomans && (
                              <span className="text-xs font-bold text-emerald-400">
                                {formatToman(sample.extractedAmountTomans)}
                              </span>
                            )}
                            {sample.extractedBalanceTomans !== undefined && (
                              <span className="text-[10px] text-slate-400">
                                (مانده: {formatToman(sample.extractedBalanceTomans)})
                              </span>
                            )}
                          </div>
                          <p className="text-[11px] text-slate-400 font-mono whitespace-pre-line bg-slate-900/90 p-2.5 rounded-xl border border-slate-800/60">
                            {maskSensitiveBankData(sample.rawText)}
                          </p>
                        </div>

                        <div className="flex sm:flex-col items-center justify-end gap-2 shrink-0">
                          {sample.parseStatus === 'VALID_TRANSACTION' && (
                            <button
                              type="button"
                              onClick={() =>
                                handleToggleExistingSampleVerify(sample.id)
                              }
                              className={`px-3 py-1.5 rounded-xl text-[11px] font-bold flex items-center space-x-1 space-x-reverse transition ${
                                sample.userVerified
                                  ? 'bg-slate-800 text-amber-300 hover:bg-slate-700'
                                  : 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 hover:bg-emerald-500/30'
                              }`}
                            >
                              <CheckCircle2 className="w-3.5 h-3.5" />
                              <span>
                                {sample.userVerified ? 'لغو تأیید' : 'تأیید این نمونه'}
                              </span>
                            </button>
                          )}
                          <button
                            type="button"
                            onClick={() => handleDeleteSample(sample.id)}
                            className="p-1.5 rounded-lg text-slate-500 hover:text-rose-400 hover:bg-rose-500/10 transition"
                            title="حذف نمونه"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          )}

          {/* TAB 2: Senders & Template Rules */}
          {activeTab === 'rules' && currentProfile && (
            <div className="space-y-6">
              {/* Senders Management */}
              <div className="p-4 rounded-2xl bg-slate-950/60 border border-slate-800 space-y-3">
                <h3 className="text-xs font-bold text-slate-200">
                  سرشماره‌ها و فرستنده‌های مجاز ({currentProfile.bankName})
                </h3>
                <div className="flex flex-wrap gap-2">
                  {currentProfile.senders.map((sender) => (
                    <span
                      key={sender}
                      className="inline-flex items-center space-x-1.5 space-x-reverse px-3 py-1 rounded-xl bg-slate-900 border border-slate-700 text-xs font-mono text-cyan-300"
                    >
                      <span>{sender}</span>
                      <button
                        type="button"
                        onClick={() => handleRemoveSender(sender)}
                        className="text-slate-500 hover:text-rose-400"
                      >
                        <X className="w-3.5 h-3.5" />
                      </button>
                    </span>
                  ))}
                </div>
                <form onSubmit={handleAddSender} className="flex gap-2">
                  <input
                    type="text"
                    value={newSenderInput}
                    onChange={(e) => setNewSenderInput(e.target.value)}
                    placeholder="افزودن سرشماره جدید (مثلاً 9820004000 یا BANKMELLAT)"
                    className="flex-1 bg-slate-900 border border-slate-800 rounded-xl px-3 py-2 text-xs text-slate-200 font-mono focus:outline-none focus:border-cyan-500"
                  />
                  <button
                    type="submit"
                    className="px-4 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-cyan-300 text-xs font-bold transition"
                  >
                    افزودن سرشماره
                  </button>
                </form>
              </div>

              {/* Existing Templates List */}
              <div className="space-y-2.5">
                <div className="flex items-center justify-between">
                  <h3 className="text-xs font-bold text-slate-200">
                    الگوهای استخراج پیامک ({currentProfile.templates.length})
                  </h3>
                  <span className="text-[10px] text-slate-400 flex items-center space-x-1 space-x-reverse">
                    <RefreshCw className="w-3 h-3" />
                    <span>تغییر الگوها به‌طور خودکار نمونه‌های قبلی را بازآزمایی می‌کند</span>
                  </span>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  {currentProfile.templates.map((tpl) => (
                    <div
                      key={tpl.id}
                      className="p-3.5 rounded-2xl bg-slate-950/70 border border-slate-800 space-y-2 text-xs"
                    >
                      <div className="flex items-center justify-between">
                        <span className="font-bold text-slate-200">{tpl.name}</span>
                        <span className="text-[10px] px-2 py-0.5 rounded-full bg-slate-800 text-slate-400 font-mono">
                          v{tpl.version}
                        </span>
                      </div>
                      <div className="text-[11px] text-slate-400 space-y-1">
                        <div>
                          نوع تراکنش:{' '}
                          <span className="text-cyan-400">
                            {translateTransactionKind(tpl.expectedKind)}
                          </span>
                        </div>
                        <div>
                          نشانگرهای جهت:{' '}
                          <span className="text-slate-300">
                            {tpl.directionAnchors.join(' ، ')}
                          </span>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* Add Custom Template Form */}
              <form
                onSubmit={handleAddCustomTemplate}
                className="p-4 rounded-2xl bg-slate-950/60 border border-slate-800 space-y-3"
              >
                <h4 className="text-xs font-bold text-cyan-400">
                  افزودن الگوی جدید برای {currentProfile.bankName}
                </h4>
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                  <div>
                    <label className="text-[11px] text-slate-400 block mb-1">
                      عنوان الگو
                    </label>
                    <input
                      type="text"
                      value={newRuleName}
                      onChange={(e) => setNewRuleName(e.target.value)}
                      placeholder="مثلاً: برداشت کارتخوان اختصاصی"
                      className="w-full bg-slate-900 border border-slate-800 rounded-xl px-3 py-2 text-xs text-slate-200"
                    />
                  </div>
                  <div>
                    <label className="text-[11px] text-slate-400 block mb-1">
                      نوع تراکنش
                    </label>
                    <select
                      value={newRuleKind}
                      onChange={(e) =>
                        setNewRuleKind(e.target.value as SmsTransactionKind)
                      }
                      className="w-full bg-slate-900 border border-slate-800 rounded-xl px-3 py-2 text-xs text-slate-200"
                    >
                      <option value="PURCHASE">خرید / پرداخت</option>
                      <option value="WITHDRAWAL">برداشت وجه</option>
                      <option value="DEPOSIT">واریز وجه</option>
                      <option value="TRANSFER">انتقال وجه</option>
                    </select>
                  </div>
                  <div>
                    <label className="text-[11px] text-slate-400 block mb-1">
                      کلیدواژه‌های تشخیص (با کاما)
                    </label>
                    <input
                      type="text"
                      value={newRuleKeywords}
                      onChange={(e) => setNewRuleKeywords(e.target.value)}
                      className="w-full bg-slate-900 border border-slate-800 rounded-xl px-3 py-2 text-xs text-slate-200"
                    />
                  </div>
                </div>
                <div className="flex justify-end">
                  <button
                    type="submit"
                    className="px-4 py-2 rounded-xl bg-cyan-500 text-slate-950 font-bold text-xs"
                  >
                    افزودن الگو و بازآزمایی نمونه‌ها
                  </button>
                </div>
              </form>
            </div>
          )}

          {/* TAB 3: Add New Custom Bank */}
          {activeTab === 'new_bank' && (
            <form
              onSubmit={handleCreateCustomBank}
              className="p-5 rounded-2xl bg-slate-950/70 border border-emerald-500/30 space-y-4"
            >
              <div className="flex items-center space-x-2 space-x-reverse text-emerald-400">
                <Plus className="w-5 h-5" />
                <h3 className="text-sm font-bold">
                  تعریف بانک یا مؤسسه مالی جدید
                </h3>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="text-xs text-slate-400 block mb-1">
                    نام بانک یا نئوبانک
                  </label>
                  <input
                    type="text"
                    required
                    value={customBankName}
                    onChange={(e) => setCustomBankName(e.target.value)}
                    placeholder="مثلاً: بانک کارآفرین / نئوبانک توبانک"
                    className="w-full bg-slate-900 border border-slate-800 rounded-xl px-3.5 py-2 text-xs text-slate-100 focus:outline-none focus:border-emerald-500"
                  />
                </div>
                <div>
                  <label className="text-xs text-slate-400 block mb-1">
                    سرشماره‌های پیامک (با کاما جدا کنید)
                  </label>
                  <input
                    type="text"
                    value={customBankSenders}
                    onChange={(e) => setCustomBankSenders(e.target.value)}
                    placeholder="مثلاً: 2000900, KARAFARIN"
                    className="w-full bg-slate-900 border border-slate-800 rounded-xl px-3.5 py-2 text-xs text-slate-100 font-mono focus:outline-none focus:border-emerald-500"
                  />
                </div>
                <div>
                  <label className="text-xs text-slate-400 block mb-1">
                    کلیدواژه‌های برداشت
                  </label>
                  <input
                    type="text"
                    value={customDebitKeywords}
                    onChange={(e) => setCustomDebitKeywords(e.target.value)}
                    className="w-full bg-slate-900 border border-slate-800 rounded-xl px-3.5 py-2 text-xs text-slate-100"
                  />
                </div>
                <div>
                  <label className="text-xs text-slate-400 block mb-1">
                    کلیدواژه‌های واریز
                  </label>
                  <input
                    type="text"
                    value={customCreditKeywords}
                    onChange={(e) => setCustomCreditKeywords(e.target.value)}
                    className="w-full bg-slate-900 border border-slate-800 rounded-xl px-3.5 py-2 text-xs text-slate-100"
                  />
                </div>
              </div>

              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setActiveTab('samples')}
                  className="px-4 py-2 rounded-xl bg-slate-800 text-slate-300 text-xs"
                >
                  انصراف
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold text-xs shadow-lg shadow-emerald-500/20 transition"
                >
                  ایجاد پروفایل بانک و رفتن به مرحله ثبت نمونه
                </button>
              </div>
            </form>
          )}
        </div>

        {/* Modal Footer */}
        <div className="px-6 py-3 bg-slate-950/90 border-t border-slate-800 flex items-center justify-between text-[11px] text-slate-400">
          <div className="flex items-center space-x-2 space-x-reverse">
            <AlertTriangle className="w-3.5 h-3.5 text-amber-400 shrink-0" />
            <span>
              ثبت خودکار تراکنش تنها برای بانک‌های در وضعیت «فعال (آماده ثبت خودکار)» انجام می‌شود.
            </span>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 font-medium transition"
          >
            بستن
          </button>
        </div>
      </div>
    </div>
  );
};
