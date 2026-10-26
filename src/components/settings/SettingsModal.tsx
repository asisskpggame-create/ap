import React, { useState } from 'react';
import {
  X,
  Settings,
  Shield,
  Palette,
  Calendar,
  Wallet,
  Download,
  Upload,
  Check,
  RotateCcw
} from 'lucide-react';
import { useLifeOs } from '../../context/LifeOsContext';
import { formatCompactCurrency, toPersianDigits } from '../../utils/persianDateUtils';

interface SettingsModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const SettingsModal: React.FC<SettingsModalProps> = ({
  isOpen,
  onClose
}) => {
  const {
    preferences,
    updatePreferences,
    exportDataJson,
    importDataJson
  } = useLifeOs();

  const [monthlyBudget, setMonthlyBudget] = useState(preferences.monthlyBudgetTarget.toString());
  const [waterTarget, setWaterTarget] = useState(preferences.dailyWaterTargetMl.toString());
  const [vaultPin, setVaultPin] = useState(preferences.vaultPinCode);
  const [vaultEnabled, setVaultEnabled] = useState(preferences.vaultLockEnabled);
  const [jsonImportText, setJsonImportText] = useState('');
  const [isImporting, setIsImporting] = useState(false);

  if (!isOpen) return null;

  const handleSave = (e: React.FormEvent) => {
    e.preventDefault();
    updatePreferences({
      monthlyBudgetTarget: parseFloat(monthlyBudget) || 15000000,
      dailyWaterTargetMl: parseInt(waterTarget) || 2500,
      vaultLockEnabled: vaultEnabled,
      vaultPinCode: vaultPin
    });
    onClose();
  };

  const handleDownloadBackup = () => {
    const jsonStr = exportDataJson();
    const blob = new Blob([jsonStr], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `personal-life-os-backup-${new Date().toISOString().slice(0, 10)}.json`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const handleExecuteImport = () => {
    if (!jsonImportText.trim()) return;
    const success = importDataJson(jsonImportText.trim());
    if (success) {
      alert('پشتیبان با موفقیت بازیابی شد!');
      setIsImporting(false);
      onClose();
    } else {
      alert('خطا در خواندن فایل JSON پشتیبان.');
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-md">
      <div className="glass-panel w-full max-w-lg rounded-2xl border border-white/10 shadow-2xl overflow-hidden max-h-[92vh] flex flex-col">
        {/* Header */}
        <div className="px-5 py-4 border-b border-white/10 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Settings className="w-5 h-5 text-teal-400" />
            <h2 className="text-sm sm:text-base font-bold text-slate-100">
              تنظیمات سیستم و شخصی‌سازی Personal Life OS
            </h2>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-lg text-slate-400 hover:text-white cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content Form */}
        <form onSubmit={handleSave} className="p-5 space-y-5 overflow-y-auto">
          {/* Calendar Display */}
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300 flex items-center gap-1.5">
              <Calendar className="w-4 h-4 text-teal-400" />
              سیستم نمایش تقویم:
            </label>
            <div className="grid grid-cols-2 gap-2 text-xs">
              <button
                type="button"
                onClick={() => updatePreferences({ calendarDisplay: 'JALALI' })}
                className={`py-2 rounded-xl transition cursor-pointer font-semibold ${
                  preferences.calendarDisplay === 'JALALI'
                    ? 'bg-teal-500/20 text-teal-300 border border-teal-500/30'
                    : 'bg-slate-900/60 text-slate-400 border border-white/5'
                }`}
              >
                تقویم خورشیدی جلالی (ایران)
              </button>
              <button
                type="button"
                onClick={() => updatePreferences({ calendarDisplay: 'GREGORIAN' })}
                className={`py-2 rounded-xl transition cursor-pointer font-semibold ${
                  preferences.calendarDisplay === 'GREGORIAN'
                    ? 'bg-teal-500/20 text-teal-300 border border-teal-500/30'
                    : 'bg-slate-900/60 text-slate-400 border border-white/5'
                }`}
              >
                تقویم میلادی (Gregorian)
              </button>
            </div>
          </div>

          {/* Budget Setting */}
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300 flex items-center gap-1.5">
              <Wallet className="w-4 h-4 text-emerald-400" />
              سقف بودجه ماهانه (تومان):
            </label>
            <input
              type="number"
              value={monthlyBudget}
              onChange={e => setMonthlyBudget(e.target.value)}
              className="w-full rounded-xl bg-slate-900/80 border border-white/10 px-3 py-2 text-xs sm:text-sm text-slate-100 font-bold"
            />
            <span className="text-[11px] text-slate-400 block">
              معادل: {formatCompactCurrency(parseFloat(monthlyBudget) || 0)}
            </span>
          </div>

          {/* Daily Water Target */}
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300 block">
              هدف نوشیدن آب روزانه (میلی‌لیتر):
            </label>
            <input
              type="number"
              value={waterTarget}
              onChange={e => setWaterTarget(e.target.value)}
              className="w-full rounded-xl bg-slate-900/80 border border-white/10 px-3 py-2 text-xs sm:text-sm text-slate-100"
            />
          </div>

          {/* Vault Lock PIN setup */}
          <div className="space-y-2 p-3.5 rounded-xl bg-slate-900/60 border border-white/5">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-amber-300 flex items-center gap-1.5">
                <Shield className="w-4 h-4" />
                قفل گاوصندوق و امنیت حریم خصوصی (Vault Lock)
              </span>
              <input
                type="checkbox"
                checked={vaultEnabled}
                onChange={e => setVaultEnabled(e.target.checked)}
                className="rounded border-slate-700 text-teal-500 cursor-pointer"
              />
            </div>
            {vaultEnabled && (
              <div className="pt-2 space-y-1">
                <label className="text-[11px] text-slate-300 block">کد پین ۴ رقمی ورود:</label>
                <input
                  type="password"
                  maxLength={4}
                  value={vaultPin}
                  onChange={e => setVaultPin(e.target.value)}
                  placeholder="۱۲۳۴"
                  className="w-32 rounded-lg bg-slate-800 border border-white/10 p-2 text-center text-sm font-mono tracking-widest text-slate-100"
                />
              </div>
            )}
          </div>

          {/* Theme Palette */}
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-300 flex items-center gap-1.5">
              <Palette className="w-4 h-4 text-purple-400" />
              پالت رنگی شیشه‌ای:
            </label>
            <div className="grid grid-cols-2 gap-2 text-xs font-medium">
              {[
                { id: 'OBSIDIAN_TEAL', label: 'آبسیدین فیروزه‌ای (پیش‌فرض)' },
                { id: 'EMERALD_MATRIX', label: 'زمرد و ماتریکس' },
                { id: 'ROYAL_AMETHYST', label: 'آمیتیست بنفش' },
                { id: 'NORDIC_FROST', label: 'یخی نوردیک' }
              ].map(palette => (
                <button
                  key={palette.id}
                  type="button"
                  onClick={() => updatePreferences({ themePalette: palette.id as any })}
                  className={`p-2 rounded-xl text-center transition cursor-pointer ${
                    preferences.themePalette === palette.id
                      ? 'bg-teal-500/20 text-teal-300 border border-teal-500/30 font-bold'
                      : 'bg-slate-900/60 text-slate-400 border border-white/5'
                  }`}
                >
                  {palette.label}
                </button>
              ))}
            </div>
          </div>

          {/* Backup / Export / Import */}
          <div className="space-y-2 p-3.5 rounded-xl bg-slate-900/60 border border-white/5">
            <span className="text-xs font-bold text-slate-300 block">
              پشتیبان‌گیری و انتقال داده‌ها (Zero Data Loss)
            </span>
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={handleDownloadBackup}
                className="flex-1 py-1.5 px-3 rounded-xl bg-slate-800 hover:bg-slate-700 text-xs font-semibold text-teal-300 border border-white/5 flex items-center justify-center gap-1.5 cursor-pointer"
              >
                <Download className="w-3.5 h-3.5" />
                دانلود فایل JSON پشتیبان
              </button>
              <button
                type="button"
                onClick={() => setIsImporting(!isImporting)}
                className="flex-1 py-1.5 px-3 rounded-xl bg-slate-800 hover:bg-slate-700 text-xs font-semibold text-slate-300 border border-white/5 flex items-center justify-center gap-1.5 cursor-pointer"
              >
                <Upload className="w-3.5 h-3.5" />
                بازیابی پشتیبان
              </button>
            </div>

            {isImporting && (
              <div className="pt-2 space-y-2">
                <textarea
                  rows={3}
                  value={jsonImportText}
                  onChange={e => setJsonImportText(e.target.value)}
                  placeholder="محتوای متنی فایل JSON پشتیبان را اینجا پیست کنید..."
                  className="w-full rounded-xl bg-slate-950 border border-white/10 p-2 text-xs font-mono text-slate-200"
                />
                <button
                  type="button"
                  onClick={handleExecuteImport}
                  className="w-full py-1.5 rounded-xl glass-button-primary text-xs font-bold cursor-pointer"
                >
                  اجرای بازیابی اطلاعات
                </button>
              </div>
            )}
          </div>

          {/* Footer Submit */}
          <div className="flex items-center justify-end gap-2 pt-2 border-t border-white/10">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-xs text-slate-400 hover:text-white cursor-pointer"
            >
              انصراف
            </button>
            <button
              type="submit"
              className="px-5 py-2 rounded-xl glass-button-primary text-xs font-semibold flex items-center gap-1.5 cursor-pointer"
            >
              <Check className="w-4 h-4" />
              <span>ذخیره تنظیمات</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
