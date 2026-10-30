import React from 'react';
import {
  HeartPulse,
  Droplets,
  Moon,
  Dumbbell,
  Flame,
  Plus,
  Minus,
} from 'lucide-react';
import { HealthLog } from '../types';
import { formatPersianNumber } from '../utils/persianUtils';

interface HealthScreenProps {
  healthLog: HealthLog;
  onUpdateHealth: (log: Partial<HealthLog>) => void;
}

export const HealthScreen: React.FC<HealthScreenProps> = ({
  healthLog,
  onUpdateHealth,
}) => {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-white flex items-center gap-2">
          <HeartPulse className="w-7 h-7 text-rose-400" />
          پایش سلامت، هیدراتاسیون و فعالیت روزانه
        </h1>
        <p className="text-slate-400 text-sm mt-1">
          ثبت میزان نوشیدن آب، کیفیت خواب، ورزش و کالری مصرفی امروز ({healthLog.date})
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Water Intake */}
        <div className="glass-card water-wave-bg rounded-2xl p-5 border border-cyan-500/20 space-y-4">
          <div className="flex items-center justify-between">
            <span className="text-sm font-medium text-cyan-300">مصرف آب روزانه</span>
            <Droplets className="w-5 h-5 text-cyan-400" />
          </div>
          <div className="text-3xl font-extrabold text-white">
            {formatPersianNumber(healthLog.waterGlasses)} <span className="text-sm font-normal text-slate-400">/ ۸ لیوان</span>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={() =>
                onUpdateHealth({
                  waterGlasses: Math.max(0, healthLog.waterGlasses - 1),
                })
              }
              className="p-2 rounded-xl bg-slate-900/80 border border-slate-700 text-slate-300 hover:text-white"
            >
              <Minus className="w-4 h-4" />
            </button>
            <button
              onClick={() =>
                onUpdateHealth({ waterGlasses: healthLog.waterGlasses + 1 })
              }
              className="flex-1 py-2 px-3 rounded-xl bg-cyan-500 hover:bg-cyan-400 text-slate-950 font-bold text-xs flex items-center justify-center gap-1"
            >
              <Plus className="w-4 h-4" />
              افزودن ۱ لیوان
            </button>
          </div>
        </div>

        {/* Sleep */}
        <div className="glass-card rounded-2xl p-5 border border-indigo-500/20 space-y-4">
          <div className="flex items-center justify-between">
            <span className="text-sm font-medium text-indigo-300">مدت زمان خواب</span>
            <Moon className="w-5 h-5 text-indigo-400" />
          </div>
          <div className="text-3xl font-extrabold text-white">
            {formatPersianNumber(healthLog.sleepHours)}{' '}
            <span className="text-sm font-normal text-slate-400">ساعت</span>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={() =>
                onUpdateHealth({
                  sleepHours: Math.max(0, Number((healthLog.sleepHours - 0.5).toFixed(1))),
                })
              }
              className="p-2 rounded-xl bg-slate-900/80 border border-slate-700 text-slate-300 hover:text-white"
            >
              <Minus className="w-4 h-4" />
            </button>
            <button
              onClick={() =>
                onUpdateHealth({
                  sleepHours: Number((healthLog.sleepHours + 0.5).toFixed(1)),
                })
              }
              className="flex-1 py-2 px-3 rounded-xl bg-indigo-500 hover:bg-indigo-400 text-white font-bold text-xs flex items-center justify-center gap-1"
            >
              <Plus className="w-4 h-4" />
              نیم ساعت
            </button>
          </div>
        </div>

        {/* Exercise */}
        <div className="glass-card rounded-2xl p-5 border border-emerald-500/20 space-y-4">
          <div className="flex items-center justify-between">
            <span className="text-sm font-medium text-emerald-300">ورزش و تحرک</span>
            <Dumbbell className="w-5 h-5 text-emerald-400" />
          </div>
          <div className="text-3xl font-extrabold text-white">
            {formatPersianNumber(healthLog.exerciseMinutes)}{' '}
            <span className="text-sm font-normal text-slate-400">دقیقه</span>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={() =>
                onUpdateHealth({
                  exerciseMinutes: Math.max(0, healthLog.exerciseMinutes - 10),
                })
              }
              className="p-2 rounded-xl bg-slate-900/80 border border-slate-700 text-slate-300 hover:text-white"
            >
              <Minus className="w-4 h-4" />
            </button>
            <button
              onClick={() =>
                onUpdateHealth({
                  exerciseMinutes: healthLog.exerciseMinutes + 10,
                })
              }
              className="flex-1 py-2 px-3 rounded-xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold text-xs flex items-center justify-center gap-1"
            >
              <Plus className="w-4 h-4" />
              ۱۰ دقیقه
            </button>
          </div>
        </div>

        {/* Calories */}
        <div className="glass-card rounded-2xl p-5 border border-amber-500/20 space-y-4">
          <div className="flex items-center justify-between">
            <span className="text-sm font-medium text-amber-300">کالری سوزانده‌شده</span>
            <Flame className="w-5 h-5 text-amber-400" />
          </div>
          <div className="text-3xl font-extrabold text-white">
            {formatPersianNumber(healthLog.caloriesBurned)}{' '}
            <span className="text-sm font-normal text-slate-400">کیلوکالری</span>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={() =>
                onUpdateHealth({
                  caloriesBurned: Math.max(0, healthLog.caloriesBurned - 50),
                })
              }
              className="p-2 rounded-xl bg-slate-900/80 border border-slate-700 text-slate-300 hover:text-white"
            >
              <Minus className="w-4 h-4" />
            </button>
            <button
              onClick={() =>
                onUpdateHealth({
                  caloriesBurned: healthLog.caloriesBurned + 50,
                })
              }
              className="flex-1 py-2 px-3 rounded-xl bg-amber-500 hover:bg-amber-400 text-slate-950 font-bold text-xs flex items-center justify-center gap-1"
            >
              <Plus className="w-4 h-4" />
              ۵۰ کالری
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
