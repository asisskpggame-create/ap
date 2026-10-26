import React, { useState } from 'react';
import { Shield, Lock, Unlock, AlertCircle } from 'lucide-react';
import { useLifeOs } from '../../context/LifeOsContext';
import { toPersianDigits } from '../../utils/persianDateUtils';

export const VaultLockModal: React.FC = () => {
  const { isVaultLocked, unlockVault } = useLifeOs();
  const [pinInput, setPinInput] = useState('');
  const [errorMsg, setErrorMsg] = useState('');

  if (!isVaultLocked) return null;

  const handleKeyPress = (num: string) => {
    if (pinInput.length < 4) {
      const next = pinInput + num;
      setPinInput(next);
      setErrorMsg('');
      if (next.length === 4) {
        const ok = unlockVault(next);
        if (!ok) {
          setErrorMsg('کد پین اشتباه است.');
          setPinInput('');
        }
      }
    }
  };

  const handleBackspace = () => {
    setPinInput(prev => prev.slice(0, -1));
    setErrorMsg('');
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-[#0b0f17]/95 backdrop-blur-2xl">
      <div className="glass-panel w-full max-w-sm rounded-3xl border border-teal-500/30 p-6 text-center space-y-5 shadow-2xl">
        <div className="w-16 h-16 rounded-3xl bg-teal-500/10 text-teal-300 border border-teal-500/20 mx-auto flex items-center justify-center">
          <Lock className="w-8 h-8" />
        </div>

        <div className="space-y-1">
          <h2 className="text-lg font-bold text-slate-100">
            گاوصندوق امنیتی Personal Life OS
          </h2>
          <p className="text-xs text-slate-400">
            لطفاً برای دسترسی به تراکنش‌ها و اهداف، رمز ۴ رقمی را وارد کنید.
          </p>
        </div>

        {/* PIN Dots */}
        <div className="flex items-center justify-center gap-3 py-2">
          {[0, 1, 2, 3].map(idx => (
            <div
              key={idx}
              className={`w-3.5 h-3.5 rounded-full transition-all ${
                idx < pinInput.length
                  ? 'bg-teal-400 scale-125 shadow-[0_0_8px_#2dd4bf]'
                  : 'bg-slate-800 border border-white/10'
              }`}
            />
          ))}
        </div>

        {errorMsg && (
          <p className="text-xs text-rose-400 font-semibold animate-shake">
            {errorMsg}
          </p>
        )}

        {/* Number Keypad */}
        <div className="grid grid-cols-3 gap-2.5 max-w-[240px] mx-auto pt-2">
          {['1', '2', '3', '4', '5', '6', '7', '8', '9'].map(n => (
            <button
              key={n}
              onClick={() => handleKeyPress(n)}
              className="h-12 rounded-2xl bg-slate-800/80 hover:bg-slate-700 text-base font-bold text-slate-100 border border-white/5 transition active:scale-95 cursor-pointer"
            >
              {toPersianDigits(n)}
            </button>
          ))}
          <div />
          <button
            onClick={() => handleKeyPress('0')}
            className="h-12 rounded-2xl bg-slate-800/80 hover:bg-slate-700 text-base font-bold text-slate-100 border border-white/5 transition active:scale-95 cursor-pointer"
          >
            {toPersianDigits('0')}
          </button>
          <button
            onClick={handleBackspace}
            className="h-12 rounded-2xl bg-slate-800/40 hover:bg-slate-700 text-xs font-semibold text-slate-400 border border-white/5 transition active:scale-95 cursor-pointer flex items-center justify-center"
          >
            حذف
          </button>
        </div>
      </div>
    </div>
  );
};
