import React, { useState } from 'react';
import { Inbox, Plus, Check, Sparkles } from 'lucide-react';
import type { BankProfile } from '../types';
import { createPresetBankProfile, getPersianDateString, revalidateBankProfile } from '../utils/persianUtils';

export interface BankVisualPreset {
  id: string;
  shortLabel: string;
  cardTitle: string;
  cardGradient: string;
  defaultSenderPlaceholder: string;
  suggestedSenders: { sender: string; label: string; sampleSnippet?: string }[];
}

export const BANK_VISUAL_PRESETS: BankVisualPreset[] = [
  {
    id: 'melli',
    shortLabel: 'ملی',
    cardTitle: 'بانک ملی',
    cardGradient: 'from-[#0a2748] via-[#0c315a] to-[#081f3a]',
    defaultSenderPlaceholder: '98300017',
    suggestedSenders: [
      { sender: '98300017', label: 'سرشماره پیامک بانک ملی (دریافتی پرتکرار)', sampleSnippet: 'بانک ملی ایران - واریز / برداشت' },
      { sender: '98100064', label: 'سامانه سداد بانک ملی ایران', sampleSnippet: 'بانک ملی ایران - مانده: ...' },
      { sender: '100064', label: 'سرشماره کوتاه سداد ملی' },
      { sender: 'BankMelli', label: 'سرشماره حروفی BankMelli' },
    ],
  },
  {
    id: 'blu',
    shortLabel: 'بلو بانک',
    cardTitle: 'بلو بانک',
    cardGradient: 'from-[#0f3460] via-[#1a528a] to-[#0b2447]',
    defaultSenderPlaceholder: 'BLU',
    suggestedSenders: [
      { sender: 'BLU', label: 'سرشماره رسمی بلوبانک (BLU)', sampleSnippet: 'بلو - از حساب شما پرید / به حساب شما نشست' },
      { sender: '98200094', label: 'سرشماره عددی بلوبانک (سامان)' },
      { sender: 'BluBank', label: 'سرشماره حروفی BluBank' },
    ],
  },
  {
    id: 'blu_pol',
    shortLabel: 'بلوبانک حالت پل',
    cardTitle: 'بلوبانک حالت پل',
    cardGradient: 'from-[#102e52] via-[#1e4d82] to-[#091e38]',
    defaultSenderPlaceholder: 'BLU',
    suggestedSenders: [
      { sender: 'BLU', label: 'سرشماره انتقال پل بلوبانک (BLU)', sampleSnippet: 'بلو - انتقال پل - مبلغ: ۱,۵۰۰,۰۰۰ ریال' },
      { sender: '98200094', label: 'سرشماره عددی سامانه پل بلوبانک' },
      { sender: 'BluBank', label: 'سرشماره حروفی BluBank' },
    ],
  },
  {
    id: 'gardeshgari',
    shortLabel: 'گردشگری',
    cardTitle: 'بانک گردشگری',
    cardGradient: 'from-[#1f1d36] via-[#3b1c32] to-[#121024]',
    defaultSenderPlaceholder: '982000110',
    suggestedSenders: [
      { sender: '982000110', label: 'سرشماره اصلی بانک گردشگری', sampleSnippet: 'بانک گردشگری - برداشت / واریز' },
      { sender: '2000110', label: 'سرشماره کوتاه گردشگری' },
      { sender: 'Gardeshgari', label: 'سرشماره حروفی Gardeshgari' },
    ],
  },
  {
    id: 'saderat',
    shortLabel: 'صادرات',
    cardTitle: 'بانک صادرات',
    cardGradient: 'from-[#0b1f47] via-[#142d63] to-[#071430]',
    defaultSenderPlaceholder: '98200060',
    suggestedSenders: [
      { sender: '98200060', label: 'سرشماره سپهر بانک صادرات', sampleSnippet: 'بانک صادرات ایران - برداشت / مانده' },
      { sender: 'Saderat', label: 'سرشماره حروفی Saderat' },
      { sender: 'BSI', label: 'سرشماره حروفی BSI' },
    ],
  },
  {
    id: 'mellat',
    shortLabel: 'ملت',
    cardTitle: 'بانک ملت',
    cardGradient: 'from-[#2a1222] via-[#4a182d] to-[#1a0b15]',
    defaultSenderPlaceholder: '98200033',
    suggestedSenders: [
      { sender: '98200033', label: 'سرشماره بهسازان بانک ملت', sampleSnippet: 'بانک ملت - برداشت از: ۶۱۰۴...' },
      { sender: '200033', label: 'سرشماره کوتاه بانک ملت' },
      { sender: 'BankMellat', label: 'سرشماره حروفی BankMellat' },
    ],
  },
  {
    id: 'tejarat',
    shortLabel: 'تجارت',
    cardTitle: 'بانک تجارت',
    cardGradient: 'from-[#0d224c] via-[#163672] to-[#081633]',
    defaultSenderPlaceholder: '98200050',
    suggestedSenders: [
      { sender: '98200050', label: 'سرشماره اصلی بانک تجارت', sampleSnippet: '*بانک تجارت* - برداشت / واریز' },
      { sender: '200050', label: 'سرشماره کوتاه تجارت' },
      { sender: 'Tejarat', label: 'سرشماره حروفی Tejarat' },
    ],
  },
  {
    id: 'keshavarzi',
    shortLabel: 'کشاورزی',
    cardTitle: 'بانک کشاورزی',
    cardGradient: 'from-[#132d24] via-[#1c4335] to-[#0b1e17]',
    defaultSenderPlaceholder: '9820009102',
    suggestedSenders: [
      { sender: '9820009102', label: 'سرشماره مهرگستر بانک کشاورزی', sampleSnippet: 'بانک کشاورزی - برداشت / مانده' },
      { sender: '20009102', label: 'سرشماره کوتاه کشاورزی' },
      { sender: 'Keshavarzi', label: 'سرشماره حروفی Keshavarzi' },
    ],
  },
  {
    id: 'refah',
    shortLabel: 'رفاه',
    cardTitle: 'بانک رفاه',
    cardGradient: 'from-[#0f2942] via-[#193d61] to-[#091a2c]',
    defaultSenderPlaceholder: '98200071',
    suggestedSenders: [
      { sender: '98200071', label: 'سرشماره عددی بانک رفاه کارگران', sampleSnippet: 'رفاه حساب ... برداشت1,960,000R' },
      { sender: 'RefahBank', label: 'سرشماره حروفی RefahBank' },
    ],
  },
  {
    id: 'pasargad',
    shortLabel: 'پاسارگاد / ویپاد',
    cardTitle: 'بانک پاسارگاد',
    cardGradient: 'from-[#282112] via-[#3d3219] to-[#17130a]',
    defaultSenderPlaceholder: '982000500',
    suggestedSenders: [
      { sender: '982000500', label: 'سرشماره بانک پاسارگاد', sampleSnippet: 'بانک پاسارگاد - واریز / برداشت' },
      { sender: 'Wepod', label: 'سرشماره نئوبانک ویپاد' },
      { sender: 'BankPasargad', label: 'سرشماره حروفی BankPasargad' },
    ],
  },
  {
    id: 'sepah',
    shortLabel: 'سپه',
    cardTitle: 'بانک سپه',
    cardGradient: 'from-[#2b1a16] via-[#422821] to-[#1a0f0c]',
    defaultSenderPlaceholder: '98200020',
    suggestedSenders: [
      { sender: '98200020', label: 'سرشماره داتین بانک سپه', sampleSnippet: 'بانک سپه - برداشت / واریز' },
      { sender: 'BankSepah', label: 'سرشماره حروفی BankSepah' },
    ],
  },
  {
    id: 'custom',
    shortLabel: 'بانک دلخواه',
    cardTitle: 'بانک جدید',
    cardGradient: 'from-[#162238] via-[#1e3150] to-[#0f172a]',
    defaultSenderPlaceholder: '982000',
    suggestedSenders: [
      { sender: '9820000', label: 'سرشماره بانک سامان (9820000)' },
      { sender: '98200014', label: 'سرشماره بانک مسکن (98200014)' },
      { sender: '98200040', label: 'سرشماره بانک پارسیان (98200040)' },
    ],
  },
];

export const BankEmblemSvg: React.FC<{ presetId: string; className?: string }> = ({
  presetId,
  className = 'w-9 h-9',
}) => {
  switch (presetId) {
    case 'melli':
      return (
        <svg viewBox="0 0 48 48" className={className} fill="none">
          <circle cx="24" cy="24" r="18" stroke="#b48327" strokeWidth="2.2" strokeDasharray="3 1.5" />
          <path
            d="M24 10C17.5 10 13 15.5 13 22.5C13 29.5 18 35 24 37C30 35 35 29.5 35 22.5C35 15.5 30.5 10 24 10Z"
            stroke="#c59332"
            strokeWidth="2"
            fill="#fffbeb"
          />
          <path
            d="M24 15L27.5 22.5L24 30L20.5 22.5L24 15Z"
            fill="#b91c1c"
          />
          <circle cx="24" cy="22.5" r="2.3" fill="#fbbf24" />
        </svg>
      );
    case 'blu':
    case 'blu_pol':
      return (
        <svg viewBox="0 0 48 48" className={className} fill="none">
          <rect x="6" y="6" width="36" height="36" rx="12" fill="#4f9cf9" />
          <circle cx="24" cy="14" r="2.4" fill="#ffffff" />
          <path
            d="M19 18V30C19 32.8 21.2 35 24 35C26.8 35 29 32.8 29 30C29 27.2 26.8 25 24 25C22.4 25 21 25.7 20.1 26.8"
            stroke="#ffffff"
            strokeWidth="3.2"
            strokeLinecap="round"
          />
          {presetId === 'blu_pol' && (
            <rect x="27" y="9" width="12" height="7" rx="3.5" fill="#10b981" />
          )}
        </svg>
      );
    case 'gardeshgari':
      return (
        <svg viewBox="0 0 48 48" className={className} fill="none">
          <polygon points="24,8 37,16 37,32 24,40 11,32 11,16" stroke="#b91c1c" strokeWidth="3.2" fill="#fef2f2" />
          <polygon points="24,14 32,19 32,29 24,34 16,29 16,19" stroke="#dc2626" strokeWidth="2.2" />
          <circle cx="24" cy="24" r="4" fill="#1e293b" />
        </svg>
      );
    case 'saderat':
      return (
        <svg viewBox="0 0 48 48" className={className} fill="none">
          <path
            d="M14 11V33C14 35.8 16.2 38 19 38H21V16C21 13.2 18.8 11 16 11H14Z"
            fill="#1e295b"
          />
          <path
            d="M34 37V15C34 12.2 31.8 10 29 10H27V32C27 34.8 29.2 37 32 37H34Z"
            fill="#1e295b"
          />
          <circle cx="24" cy="24" r="4.5" fill="#2563eb" />
        </svg>
      );
    case 'mellat':
      return (
        <svg viewBox="0 0 48 48" className={className} fill="none">
          <path d="M24 8L38 20L32 37H16L10 20L24 8Z" fill="#c8102e" />
          <path d="M24 13L33 21L29 33H19L15 21L24 13Z" stroke="#ffffff" strokeWidth="2" />
          <circle cx="24" cy="24" r="4.8" fill="#f59e0b" />
        </svg>
      );
    case 'tejarat':
      return (
        <svg viewBox="0 0 48 48" className={className} fill="none">
          <circle cx="24" cy="24" r="16" stroke="#1d4ed8" strokeWidth="2.5" strokeDasharray="2.5 2" />
          <circle cx="24" cy="24" r="10" stroke="#1e3a8a" strokeWidth="2.2" />
          <path d="M19 24C19 20.5 21.2 18 24 18C26.8 18 29 20.5 29 24C29 27.5 26.8 30 24 30" stroke="#1d4ed8" strokeWidth="2.8" strokeLinecap="round" />
        </svg>
      );
    case 'keshavarzi':
      return (
        <svg viewBox="0 0 48 48" className={className} fill="none">
          <path d="M24 9V39" stroke="#15803d" strokeWidth="2.8" strokeLinecap="round" />
          <path d="M24 16C19 16 15 19 15 24C19 24 23 21 24 16Z" fill="#ca8a04" />
          <path d="M24 16C29 16 33 19 33 24C29 24 25 21 24 16Z" fill="#ca8a04" />
          <path d="M24 24C19 24 15 27 15 32C19 32 23 29 24 24Z" fill="#15803d" />
          <path d="M24 24C29 24 33 27 33 32C29 32 25 29 24 24Z" fill="#15803d" />
        </svg>
      );
    case 'refah':
      return (
        <svg viewBox="0 0 48 48" className={className} fill="none">
          <circle cx="24" cy="24" r="15" stroke="#0284c7" strokeWidth="3" />
          <path d="M17 26L22 31L32 19" stroke="#0369a1" strokeWidth="3.2" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      );
    case 'pasargad':
      return (
        <svg viewBox="0 0 48 48" className={className} fill="none">
          <rect x="10" y="10" width="28" height="28" rx="7" stroke="#d97706" strokeWidth="2.8" fill="#fffbeb" />
          <circle cx="24" cy="24" r="6" fill="#b45309" />
        </svg>
      );
    case 'sepah':
      return (
        <svg viewBox="0 0 48 48" className={className} fill="none">
          <circle cx="24" cy="24" r="15" stroke="#ea580c" strokeWidth="2.8" />
          <circle cx="24" cy="24" r="6" fill="#9a3412" />
        </svg>
      );
    default:
      return (
        <svg viewBox="0 0 48 48" className={className} fill="none">
          <rect x="9" y="12" width="30" height="24" rx="5" stroke="#0f766e" strokeWidth="2.8" />
          <path d="M24 18V30M18 24H30" stroke="#0f766e" strokeWidth="2.8" strokeLinecap="round" />
        </svg>
      );
  }
};

interface BankVisualSelectorSheetProps {
  profiles: BankProfile[];
  onSaveBankAndSender: (nextProfiles: BankProfile[], selectedId: string, feedbackText: string) => void;
  onCancel: () => void;
}

export const BankVisualSelectorSheet: React.FC<BankVisualSelectorSheetProps> = ({
  profiles,
  onSaveBankAndSender,
  onCancel,
}) => {
  const [selectedPresetId, setSelectedPresetId] = useState<string>('melli');
  const [senderInput, setSenderInput] = useState<string>('');
  const [customBankTitle, setCustomBankTitle] = useState<string>('');
  const [showSenderPicker, setShowSenderPicker] = useState<boolean>(false);

  const activePreset =
    BANK_VISUAL_PRESETS.find((b) => b.id === selectedPresetId) ||
    BANK_VISUAL_PRESETS[0];

  // Collect all unique received/saved senders across profiles + preset suggestions
  const receivedSendersFromProfiles = Array.from(
    new Set(
      profiles.flatMap((p) => [
        ...p.senders,
        ...p.samples.map((s) => s.senderId || '').filter(Boolean),
      ])
    )
  ).filter(Boolean);

  const handleSelectPreset = (preset: BankVisualPreset) => {
    setSelectedPresetId(preset.id);
    // If the profile already exists and has a user-added sender, keep input clean or let user pick/type
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const trimmedSender = senderInput.trim();

    if (selectedPresetId === 'custom') {
      const finalCustomName = customBankTitle.trim() || 'بانک سفارشی جدید';
      const newCustomProfile = createPresetBankProfile(
        'custom',
        trimmedSender || '982000',
        finalCustomName
      );
      onSaveBankAndSender(
        [...profiles, newCustomProfile],
        newCustomProfile.id,
        `بانک «${newCustomProfile.bankName}» با سرشماره «${
          newCustomProfile.senders[0] || 'تعریف‌نشده'
        }» ثبت شد. اکنون نمونه پیامک آن را بررسی و تأیید کنید.`
      );
      return;
    }

    const existing = profiles.find((p) => p.id === selectedPresetId);
    if (existing) {
      const updatedSenders = trimmedSender
        ? Array.from(new Set([trimmedSender, ...existing.senders]))
        : existing.senders;
      const draft: BankProfile = {
        ...existing,
        senders: updatedSenders,
        updatedAt: getPersianDateString(),
      };
      const revalidated = revalidateBankProfile(draft);
      const nextProfiles = profiles.map((p) =>
        p.id === existing.id ? revalidated : p
      );
      onSaveBankAndSender(
        nextProfiles,
        existing.id,
        `پروفایل «${existing.bankName}» با سرشماره «${
          trimmedSender || existing.senders[0] || 'پیش‌فرض'
        }» بروزرسانی و انتخاب شد.`
      );
    } else {
      const created = createPresetBankProfile(selectedPresetId, trimmedSender);
      onSaveBankAndSender(
        [...profiles, created],
        created.id,
        `بانک «${created.bankName}» با سرشماره «${
          created.senders[0] || 'پیش‌فرض'
        }» به لیست بانک‌های شما اضافه شد.`
      );
    }
  };

  const displayCardTitle =
    selectedPresetId === 'custom'
      ? customBankTitle.trim() || 'بانک جدید'
      : activePreset.cardTitle;

  return (
    <form
      onSubmit={handleSubmit}
      className="rounded-3xl bg-white text-slate-800 p-5 sm:p-7 shadow-2xl border border-slate-200 max-w-xl mx-auto space-y-5"
      dir="rtl"
    >
      {/* Header matching screenshot */}
      <div className="text-right space-y-1">
        <h3 className="text-xl sm:text-2xl font-extrabold text-slate-900">
          افزودن بانک جدید
        </h3>
        <p className="text-xs sm:text-sm text-slate-500 font-medium">
          بانک و سرشماره پیامک آن را انتخاب کنید
        </p>
      </div>

      {/* Live Visual Bank Card Preview (Exact layout from screenshot) */}
      <div
        className={`relative w-full h-44 sm:h-48 rounded-2xl bg-gradient-to-br ${activePreset.cardGradient} p-5 sm:p-6 text-white overflow-hidden shadow-xl flex flex-col justify-between select-none`}
      >
        {/* Decorative light arcs matching screenshot */}
        <div className="pointer-events-none absolute -top-12 -right-12 w-56 h-56 rounded-full bg-white/[0.07]" />
        <div className="pointer-events-none absolute -bottom-20 -left-12 w-64 h-64 rounded-full bg-white/[0.05]" />

        {/* Top Row: Bank Emblem + Name on Right */}
        <div className="relative z-10 flex items-center justify-start gap-3">
          <div className="w-12 h-12 rounded-full bg-white shadow-md flex items-center justify-center shrink-0">
            <BankEmblemSvg presetId={selectedPresetId} className="w-9 h-9" />
          </div>
          <span className="text-lg sm:text-xl font-extrabold tracking-tight text-white drop-shadow">
            {displayCardTitle}
          </span>
        </div>

        {/* Middle Left: Metallic Gold EMV Chip */}
        <div className="relative z-10 flex items-center justify-end pl-2">
          <div className="w-12 h-9 rounded-lg bg-gradient-to-br from-[#f6d365] via-[#d4af37] to-[#b38728] border border-amber-200/60 shadow-inner relative overflow-hidden">
            <div className="absolute inset-y-0 left-1/3 w-px bg-amber-950/30" />
            <div className="absolute inset-y-0 right-1/3 w-px bg-amber-950/30" />
            <div className="absolute inset-x-0 top-1/3 h-px bg-amber-950/30" />
            <div className="absolute inset-x-0 bottom-1/3 h-px bg-amber-950/30" />
          </div>
        </div>

        {/* Bottom Right: SMS Sender Label & Live Value or Dots */}
        <div className="relative z-10 text-right space-y-1">
          <span className="block text-xs text-slate-200/90 font-medium">
            سرشماره پیامک
          </span>
          {senderInput.trim() ? (
            <span
              className="block text-base sm:text-lg font-mono font-bold tracking-wider text-white"
              dir="ltr"
            >
              {senderInput.trim()}
            </span>
          ) : (
            <div
              className="flex items-center justify-start gap-1.5 pt-1"
              aria-label="سرشماره وارد نشده"
            >
              {Array.from({ length: 8 }).map((_, idx) => (
                <span
                  key={idx}
                  className="w-2 h-2 rounded-full bg-white/95 inline-block"
                />
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Section: انتخاب بانک (4-Column Visual Grid) */}
      <div className="space-y-2">
        <label className="block text-right text-sm font-bold text-slate-600">
          انتخاب بانک
        </label>

        <div className="rounded-2xl bg-[#f4f6f9] border border-slate-200/80 p-4 grid grid-cols-4 gap-y-4 gap-x-2">
          {BANK_VISUAL_PRESETS.map((preset) => {
            const isSelected = selectedPresetId === preset.id;
            return (
              <button
                key={preset.id}
                type="button"
                onClick={() => handleSelectPreset(preset)}
                className="group flex flex-col items-center justify-start text-center space-y-1.5 focus:outline-none"
              >
                <div
                  className={`w-14 h-14 sm:w-16 sm:h-16 rounded-full bg-white flex items-center justify-center transition-all ${
                    isSelected
                      ? 'ring-[2.5px] ring-[#0c2d57] shadow-md scale-105'
                      : 'border border-slate-200/90 shadow-sm group-hover:border-slate-300'
                  }`}
                >
                  <BankEmblemSvg presetId={preset.id} className="w-9 h-9 sm:w-10 sm:h-10" />
                </div>
                <span
                  className={`text-[11px] sm:text-xs leading-tight ${
                    isSelected
                      ? 'font-extrabold text-slate-900'
                      : 'font-medium text-slate-700'
                  }`}
                >
                  {preset.shortLabel}
                </span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Optional Custom Bank Name input when "بانک دلخواه" is selected */}
      {selectedPresetId === 'custom' && (
        <div className="space-y-1.5">
          <label className="block text-right text-xs font-bold text-slate-600">
            نام بانک یا مؤسسه مالی دلخواه
          </label>
          <input
            type="text"
            required
            value={customBankTitle}
            onChange={(e) => setCustomBankTitle(e.target.value)}
            placeholder="مثلاً: بانک کارآفرین / بانک مسکن / ویپاد"
            className="w-full rounded-2xl bg-[#f4f6f9] border border-slate-300 px-4 py-3 text-sm text-slate-900 focus:outline-none focus:border-emerald-600"
          />
        </div>
      )}

      {/* Section: سرشماره پیامک + دکمه لیست سرشماره‌های دریافت‌شده */}
      <div className="space-y-2">
        <label className="block text-right text-sm font-bold text-slate-600">
          سرشماره پیامک
        </label>

        <div className="flex items-center gap-2.5">
          {/* Input field */}
          <input
            type="text"
            value={senderInput}
            onChange={(e) => setSenderInput(e.target.value)}
            placeholder={`مثلاً: ${activePreset.defaultSenderPlaceholder}`}
            className="flex-1 rounded-2xl bg-[#f4f6f9] border border-slate-200 px-4 py-3.5 text-sm sm:text-base text-slate-800 placeholder-slate-400 focus:outline-none focus:border-[#0c2d57] focus:bg-white transition font-mono text-right"
          />

          {/* Sender List Picker Button (Left side in RTL, matching screenshot) */}
          <button
            type="button"
            onClick={() => setShowSenderPicker((prev) => !prev)}
            title="انتخاب از لیست سرشماره‌های دریافت‌شده"
            aria-label="انتخاب از لیست سرشماره‌های دریافت‌شده"
            className={`w-12 h-12 sm:w-14 sm:h-14 rounded-2xl flex items-center justify-center shrink-0 transition border ${
              showSenderPicker
                ? 'bg-emerald-100 border-emerald-400 text-emerald-800 shadow-inner'
                : 'bg-[#dff1fd] hover:bg-[#cfe9fc] border-sky-200/80 text-[#1b6b3a]'
            }`}
          >
            <Inbox className="w-6 h-6" />
          </button>
        </div>

        <p className="text-[11px] sm:text-xs text-slate-500 text-right">
          می‌توانید از دکمه لیست، سرشماره‌های دریافت‌شده را انتخاب کنید
        </p>

        {/* Expandable Received & Official Senders List Drawer */}
        {showSenderPicker && (
          <div className="mt-2 rounded-2xl bg-slate-50 border border-slate-200 p-3.5 space-y-3 animate-fadeIn">
            <div className="flex items-center justify-between border-b border-slate-200 pb-2">
              <span className="text-xs font-bold text-slate-800 flex items-center gap-1.5">
                <Sparkles className="w-3.5 h-3.5 text-emerald-600" />
                <span>
                  سرشماره‌های پیشنهادی و دریافت‌شده برای «{displayCardTitle}»
                </span>
              </span>
              <button
                type="button"
                onClick={() => setShowSenderPicker(false)}
                className="text-[11px] text-slate-500 hover:text-slate-800"
              >
                بستن لیست
              </button>
            </div>

            <div className="space-y-1.5 max-h-44 overflow-y-auto">
              {activePreset.suggestedSenders.map((item) => {
                const isCurrent = senderInput.trim() === item.sender;
                return (
                  <button
                    key={item.sender}
                    type="button"
                    onClick={() => {
                      setSenderInput(item.sender);
                      setShowSenderPicker(false);
                    }}
                    className={`w-full text-right p-2.5 rounded-xl border flex items-center justify-between gap-2 transition ${
                      isCurrent
                        ? 'bg-emerald-50 border-emerald-400 text-emerald-900'
                        : 'bg-white hover:bg-slate-100 border-slate-200/80 text-slate-700'
                    }`}
                  >
                    <div className="space-y-0.5">
                      <div className="text-xs font-bold text-slate-800">
                        {item.label}
                      </div>
                      {item.sampleSnippet && (
                        <div className="text-[10px] text-slate-500">
                          {item.sampleSnippet}
                        </div>
                      )}
                    </div>
                    <div className="flex items-center gap-1.5 shrink-0">
                      <span className="px-2.5 py-1 rounded-lg bg-slate-100 font-mono text-xs font-bold text-slate-800">
                        {item.sender}
                      </span>
                      {isCurrent && <Check className="w-4 h-4 text-emerald-600" />}
                    </div>
                  </button>
                );
              })}

              {receivedSendersFromProfiles.length > 0 && (
                <div className="pt-2 border-t border-slate-200">
                  <span className="block text-[10px] font-bold text-slate-400 mb-1.5">
                    سایر سرشماره‌های ذخیره‌شده در برنامه:
                  </span>
                  <div className="flex flex-wrap gap-1.5">
                    {receivedSendersFromProfiles.map((snd) => (
                      <button
                        key={snd}
                        type="button"
                        onClick={() => {
                          setSenderInput(snd);
                          setShowSenderPicker(false);
                        }}
                        className="px-2.5 py-1 rounded-lg bg-white border border-slate-200 hover:border-emerald-500 text-xs font-mono text-slate-700"
                      >
                        {snd}
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </div>
        )}
      </div>

      {/* Bottom Action Row: "+ ثبت بانک" (Green) and "انصراف" */}
      <div className="pt-2 grid grid-cols-2 gap-3 items-center">
        <button
          type="submit"
          className="w-full py-3.5 px-5 rounded-2xl bg-[#1f7a38] hover:bg-[#19662e] active:scale-[0.99] text-white font-extrabold text-sm sm:text-base shadow-lg shadow-emerald-900/15 flex items-center justify-center gap-2 transition"
        >
          <Plus className="w-5 h-5 stroke-[2.5]" />
          <span>ثبت بانک</span>
        </button>

        <button
          type="button"
          onClick={onCancel}
          className="w-full py-3.5 px-5 rounded-2xl bg-transparent hover:bg-slate-100 text-slate-600 font-bold text-sm sm:text-base transition text-center"
        >
          انصراف
        </button>
      </div>
    </form>
  );
};
