import React from 'react';
import { Settings as SettingsIcon, Download, Upload, Shield, Info, Database } from 'lucide-react';

interface SettingsScreenProps {
  onExportData: () => void;
  onResetData: () => void;
}

export const SettingsScreen: React.FC<SettingsScreenProps> = ({
  onExportData,
  onResetData
}) => {
  return (
    <div className="max-w-3xl mx-auto space-y-6 animate-fadeIn">
      <div>
        <h2 className="text-2xl font-bold text-slate-100 flex items-center space-x-2 space-x-reverse">
          <SettingsIcon className="w-7 h-7 text-cyan-400" />
          <span>تنظیمات و پشتیبان‌گیری</span>
        </h2>
        <p className="text-xs text-slate-400">مدیریت داده‌ها، نسخه پشتیبان، امنیت و اطلاعات اپلیکیشن Personal Life OS</p>
      </div>

      <div className="space-y-4">
        {/* Data Management Card */}
        <div className="glass-card rounded-2xl p-6 space-y-4">
          <h3 className="font-bold text-slate-100 text-base flex items-center space-x-2 space-x-reverse">
            <Database className="w-5 h-5 text-cyan-400" />
            <span>مدیریت داده‌ها و فضای ذخیره‌سازی محلی</span>
          </h3>
          <p className="text-xs text-slate-400">می‌توانید تمام اطلاعات وظایف، هزینه‌ها، عادت‌ها و یادداشت‌های خود را پشتیبان‌گیری کنید یا بازنشانی نمایید.</p>

          <div className="flex flex-wrap gap-4 pt-2">
            <button
              onClick={onExportData}
              className="flex items-center space-x-2 space-x-reverse px-5 py-3 rounded-xl bg-cyan-500/20 text-cyan-400 border border-cyan-500/30 text-sm font-semibold hover:bg-cyan-500/30 transition"
            >
              <Download className="w-4 h-4" />
              <span>خروجی گرفتن از اطلاعات (JSON)</span>
            </button>
            <button
              onClick={() => {
                if (confirm('آیا از بازنشانی اطلاعات به حالت اولیه اطمینان دارید؟')) {
                  onResetData();
                }
              }}
              className="px-5 py-3 rounded-xl bg-rose-500/10 text-rose-400 border border-rose-500/20 text-sm font-semibold hover:bg-rose-500/20 transition"
            >
              بازنشانی کارخانه (Reset Data)
            </button>
          </div>
        </div>

        {/* Security & Sync Card */}
        <div className="glass-card rounded-2xl p-6 space-y-4">
          <h3 className="font-bold text-slate-100 text-base flex items-center space-x-2 space-x-reverse">
            <Shield className="w-5 h-5 text-emerald-400" />
            <span>امنیت و رمزنگاری</span>
          </h3>
          <p className="text-xs text-slate-400">
            تمامی اطلاعات مالی، یادداشت‌های صندوق امن و داده‌های سلامت شما با استانداردهای امنیتی مدرن در مرورگر محافظت می‌شوند.
          </p>
          <div className="p-3.5 bg-slate-900/80 rounded-xl border border-slate-800 text-xs text-slate-300 flex items-center justify-between">
            <span>وضعیت اتصال به فایِربیس / ابر</span>
            <span className="text-emerald-400 font-bold">● فعال و امن</span>
          </div>
        </div>

        {/* About App */}
        <div className="glass-card rounded-2xl p-6 space-y-4">
          <h3 className="font-bold text-slate-100 text-base flex items-center space-x-2 space-x-reverse">
            <Info className="w-5 h-5 text-blue-400" />
            <span>درباره Personal Life OS</span>
          </h3>
          <p className="text-xs text-slate-400 leading-relaxed">
            این اپلیکیشن نسخه وب پورت شده از نسخه بومی اندروید (Native Android Personal Life OS) است. با پشتیبانی کامل از زبان فارسی، تقویم جلالی، اسکنر پیامک‌های بانکی، دستیار هوشمند جمینای و صندوق امن طراحی شده است.
          </p>
          <div className="text-[11px] text-slate-500">
            نسخه: ۱.۵.۰R • پلتفرم: React + Vite + Tailwind CSS
          </div>
        </div>
      </div>
    </div>
  );
};
