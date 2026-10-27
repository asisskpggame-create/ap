import React, { useState } from 'react';
import { 
  HeartPulse, 
  Moon, 
  Droplets, 
  Flame, 
  Plus, 
  Activity,
  CheckCircle2
} from 'lucide-react';
import { HealthLog } from '../types';

interface HealthScreenProps {
  healthLog: HealthLog;
  onUpdateHealth: (log: Partial<HealthLog>) => void;
}

export const HealthScreen: React.FC<HealthScreenProps> = ({
  healthLog,
  onUpdateHealth
}) => {
  const [waterCount, setWaterCount] = useState(healthLog.waterGlasses);
  const [sleepHours, setSleepHours] = useState(healthLog.sleepHours);
  const [exerciseMins, setExerciseMins] = useState(healthLog.exerciseMinutes);

  const handleAddWater = () => {
    const next = waterCount + 1;
    setWaterCount(next);
    onUpdateHealth({ waterGlasses: next });
  };

  const handleMinusWater = () => {
    if (waterCount > 0) {
      const next = waterCount - 1;
      setWaterCount(next);
      onUpdateHealth({ waterGlasses: next });
    }
  };

  return (
    <div className="space-y-6 animate-fadeIn">
      <div>
        <h2 className="text-2xl font-bold text-slate-100">سلامت، خواب و تندرستی</h2>
        <p className="text-xs text-slate-400">پایش خواب شبانه، میزان مصرف آب، ورزش و کالری مصرفی روزانه</p>
      </div>

      {/* Top Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Sleep Card */}
        <div className="glass-card rounded-2xl p-5 space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-xs text-slate-400">خواب شبانه</span>
            <div className="w-8 h-8 rounded-xl bg-indigo-500/10 text-indigo-400 flex items-center justify-center">
              <Moon className="w-4 h-4" />
            </div>
          </div>
          <div className="text-2xl font-bold text-slate-100">{healthLog.sleepHours} ساعت</div>
          <p className="text-xs text-indigo-400">کیفیت: {healthLog.sleepQuality}</p>
        </div>

        {/* Water Hydration Card */}
        <div className="glass-card rounded-2xl p-5 space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-xs text-slate-400">آب مصرفی روزانه</span>
            <div className="w-8 h-8 rounded-xl bg-cyan-500/10 text-cyan-400 flex items-center justify-center">
              <Droplets className="w-4 h-4" />
            </div>
          </div>
          <div className="text-2xl font-bold text-slate-100">{waterCount} لیوان</div>
          <p className="text-xs text-cyan-400">هدف: ۸ لیوان در روز</p>
        </div>

        {/* Exercise Card */}
        <div className="glass-card rounded-2xl p-5 space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-xs text-slate-400">ورزش و تحرک</span>
            <div className="w-8 h-8 rounded-xl bg-rose-500/10 text-rose-400 flex items-center justify-center">
              <Activity className="w-4 h-4" />
            </div>
          </div>
          <div className="text-2xl font-bold text-slate-100">{healthLog.exerciseMinutes} دقیقه</div>
          <p className="text-xs text-rose-400">{healthLog.exerciseType}</p>
        </div>

        {/* Calories Card */}
        <div className="glass-card rounded-2xl p-5 space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-xs text-slate-400">کالری سوزانده شده</span>
            <div className="w-8 h-8 rounded-xl bg-amber-500/10 text-amber-400 flex items-center justify-center">
              <Flame className="w-4 h-4" />
            </div>
          </div>
          <div className="text-2xl font-bold text-slate-100">{healthLog.caloriesBurned} کیلوکالری</div>
          <p className="text-xs text-amber-400">عملکرد عالی</p>
        </div>
      </div>

      {/* Interactive Water & Sleep Controls */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Water Tracker Module */}
        <div className="glass-card rounded-2xl p-6 space-y-4">
          <div className="flex items-center space-x-2 space-x-reverse text-cyan-400">
            <Droplets className="w-5 h-5" />
            <h3 className="font-bold text-slate-100 text-base">مدیریت مصرف آب</h3>
          </div>
          <p className="text-xs text-slate-400">با هر لیوان آب مصرفی، دکمه زیر را لمس کنید تا هیدراته بمانید.</p>
          
          <div className="flex items-center justify-between bg-slate-900/80 p-4 rounded-xl border border-slate-800">
            <div>
              <span className="text-sm font-bold text-slate-200">تعداد لیوان آب امروز</span>
              <p className="text-2xl font-bold text-cyan-400 mt-1">{waterCount} لیوان</p>
            </div>
            <div className="flex items-center space-x-2 space-x-reverse">
              <button
                onClick={handleMinusWater}
                className="w-10 h-10 rounded-xl bg-slate-800 text-slate-300 font-bold hover:bg-slate-700 transition"
              >
                -
              </button>
              <button
                onClick={handleAddWater}
                className="w-10 h-10 rounded-xl bg-cyan-500 text-slate-950 font-bold hover:bg-cyan-400 transition shadow-lg shadow-cyan-500/20"
              >
                +
              </button>
            </div>
          </div>
        </div>

        {/* Sleep Log Settings */}
        <div className="glass-card rounded-2xl p-6 space-y-4">
          <div className="flex items-center space-x-2 space-x-reverse text-indigo-400">
            <Moon className="w-5 h-5" />
            <h3 className="font-bold text-slate-100 text-base">ثبت و ویرایش ساعات خواب</h3>
          </div>
          
          <div className="space-y-3">
            <div>
              <label className="text-xs text-slate-400 block mb-1">مدت زمان خواب (ساعت)</label>
              <input 
                type="number"
                value={sleepHours}
                onChange={e => {
                  const val = parseFloat(e.target.value) || 0;
                  setSleepHours(val);
                  onUpdateHealth({ sleepHours: val });
                }}
                className="w-full bg-slate-900 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-slate-200 focus:outline-none focus:border-indigo-500"
              />
            </div>

            <div>
              <label className="text-xs text-slate-400 block mb-1">کیفیت خواب</label>
              <select
                value={healthLog.sleepQuality}
                onChange={e => onUpdateHealth({ sleepQuality: e.target.value as any })}
                className="w-full bg-slate-900 border border-slate-800 rounded-xl px-3 py-2.5 text-sm text-slate-200 focus:outline-none focus:border-indigo-500"
              >
                <option value="poor">نیاز به بهبود (Poor)</option>
                <option value="fair">متوسط (Fair)</option>
                <option value="good">خوب (Good)</option>
                <option value="excellent">عالی و عمیق (Excellent)</option>
              </select>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
