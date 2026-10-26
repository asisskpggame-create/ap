import React, { useState, useEffect } from 'react';
import { Play, Pause, RotateCcw, Timer, ChevronUp, ChevronDown, CheckCircle2 } from 'lucide-react';
import { toPersianDigits } from '../../utils/persianDateUtils';

export const FloatingFocusTimer: React.FC = () => {
  const [isOpen, setIsOpen] = useState(false);
  const [mode, setMode] = useState<'WORK' | 'BREAK'>('WORK');
  const [timeLeft, setTimeLeft] = useState(25 * 60);
  const [isActive, setIsActive] = useState(false);

  useEffect(() => {
    let interval: any = null;
    if (isActive && timeLeft > 0) {
      interval = setInterval(() => {
        setTimeLeft(prev => prev - 1);
      }, 1000);
    } else if (timeLeft === 0 && isActive) {
      // Switch mode
      if (mode === 'WORK') {
        setMode('BREAK');
        setTimeLeft(5 * 60);
      } else {
        setMode('WORK');
        setTimeLeft(25 * 60);
      }
      setIsActive(false);
    }
    return () => clearInterval(interval);
  }, [isActive, timeLeft, mode]);

  const toggleTimer = () => setIsActive(!isActive);

  const resetTimer = () => {
    setIsActive(false);
    setTimeLeft(mode === 'WORK' ? 25 * 60 : 5 * 60);
  };

  const minutes = Math.floor(timeLeft / 60);
  const seconds = timeLeft % 60;
  const timeFormatted = `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;

  return (
    <div className="fixed bottom-20 left-4 z-30">
      <div className="glass-panel rounded-2xl border border-white/10 shadow-xl overflow-hidden transition-all duration-300">
        {!isOpen ? (
          <button
            onClick={() => setIsOpen(true)}
            className="flex items-center gap-2 px-3 py-2 text-xs font-medium text-teal-300 hover:text-teal-200 transition cursor-pointer"
          >
            <div className={`w-2.5 h-2.5 rounded-full ${isActive ? 'bg-teal-400 animate-ping' : 'bg-slate-400'}`} />
            <Timer className="w-4 h-4 text-teal-400" />
            <span className="font-mono text-xs">{toPersianDigits(timeFormatted)}</span>
            <ChevronUp className="w-3.5 h-3.5 text-slate-400" />
          </button>
        ) : (
          <div className="p-3 w-64 space-y-3">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Timer className="w-4 h-4 text-teal-400" />
                <span className="text-xs font-bold text-slate-200">
                  تایمر تمرکز پومودورو
                </span>
              </div>
              <button
                onClick={() => setIsOpen(false)}
                className="text-slate-400 hover:text-white cursor-pointer"
              >
                <ChevronDown className="w-4 h-4" />
              </button>
            </div>

            {/* Mode Switcher */}
            <div className="flex rounded-xl bg-slate-900/60 p-1 border border-white/5 text-xs">
              <button
                onClick={() => {
                  setMode('WORK');
                  setTimeLeft(25 * 60);
                  setIsActive(false);
                }}
                className={`flex-1 py-1 rounded-lg transition ${
                  mode === 'WORK'
                    ? 'bg-teal-500/20 text-teal-300 font-semibold border border-teal-500/30'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                کار عمیق (۲۵ د)
              </button>
              <button
                onClick={() => {
                  setMode('BREAK');
                  setTimeLeft(5 * 60);
                  setIsActive(false);
                }}
                className={`flex-1 py-1 rounded-lg transition ${
                  mode === 'BREAK'
                    ? 'bg-emerald-500/20 text-emerald-300 font-semibold border border-emerald-500/30'
                    : 'text-slate-400 hover:text-slate-200'
                }`}
              >
                استراحت (۵ د)
              </button>
            </div>

            {/* Time display */}
            <div className="text-center py-2">
              <div className="text-3xl font-extrabold font-mono text-teal-300 tracking-wider">
                {toPersianDigits(timeFormatted)}
              </div>
              <p className="text-[11px] text-slate-400 mt-1 flex items-center justify-center gap-1">
                {isActive ? (
                  <span>در حال ضبط زمان تمرکز...</span>
                ) : (
                  <span>آماده برای شروع</span>
                )}
              </p>
            </div>

            {/* Controls */}
            <div className="flex items-center justify-center gap-2">
              <button
                onClick={toggleTimer}
                className="flex-1 py-1.5 rounded-xl glass-button-primary flex items-center justify-center gap-1.5 text-xs font-semibold cursor-pointer"
              >
                {isActive ? <Pause className="w-4 h-4" /> : <Play className="w-4 h-4" />}
                <span>{isActive ? 'توقف' : 'شروع'}</span>
              </button>
              <button
                onClick={resetTimer}
                title="شروع مجدد"
                className="p-2 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white border border-white/5 transition cursor-pointer"
              >
                <RotateCcw className="w-4 h-4" />
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
