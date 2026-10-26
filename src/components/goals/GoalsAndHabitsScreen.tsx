import React, { useState } from 'react';
import {
  CheckSquare,
  Sparkles,
  Flame,
  Droplets,
  Moon,
  Activity,
  Plus,
  Trash2,
  Calendar,
  Clock,
  ChevronDown,
  ChevronUp,
  Repeat,
  CheckCircle2,
  Circle,
  Star,
  Award
} from 'lucide-react';
import { useLifeOs } from '../../context/LifeOsContext';
import { TaskPriority, Task } from '../../types/lifeOs';
import { formatEpochDay, currentLocalEpochDay, toPersianDigits } from '../../utils/persianDateUtils';

interface GoalsAndHabitsScreenProps {
  onOpenAddTask: () => void;
}

export const GoalsAndHabitsScreen: React.FC<GoalsAndHabitsScreenProps> = ({
  onOpenAddTask
}) => {
  const {
    tasks,
    toggleTaskCompleted,
    toggleSubtask,
    deleteTask,
    habits,
    habitRecords,
    toggleHabitToday,
    addHabit,
    deleteHabit,
    consumption,
    addConsumption,
    sleepRecords,
    addSleepRecord,
    exerciseRecords,
    addExerciseRecord,
    preferences
  } = useLifeOs();

  const [activeTab, setActiveTab] = useState<'TASKS' | 'HABITS' | 'HEALTH'>('TASKS');
  const [priorityFilter, setPriorityFilter] = useState<'ALL' | TaskPriority>('ALL');
  const [expandedTaskIds, setExpandedTaskIds] = useState<Record<string, boolean>>({});

  // Habit modal state
  const [newHabitTitle, setNewHabitTitle] = useState('');
  const [isAddingHabit, setIsAddingHabit] = useState(false);

  // Health modal / input states
  const [sleepHours, setSleepHours] = useState('7.5');
  const [sleepQuality, setSleepQuality] = useState(5);
  const [exerciseType, setExerciseType] = useState('پیاده‌روی');
  const [exerciseMinutes, setExerciseMinutes] = useState('30');

  const todayEpoch = currentLocalEpochDay();

  // Water calculation for today
  const todayWaterMl = consumption
    .filter(c => c.category === 'WATER' && c.localEpochDay === todayEpoch)
    .reduce((sum, c) => sum + c.quantity, 0);

  const waterTarget = preferences.dailyWaterTargetMl || 2500;
  const waterPercent = Math.min(100, Math.round((todayWaterMl / waterTarget) * 100));

  const handleQuickAddWater = (ml: number) => {
    addConsumption({
      itemName: 'آب آشامیدنی',
      category: 'WATER',
      quantity: ml,
      unit: 'ml',
      timestampMillis: Date.now(),
      localEpochDay: todayEpoch,
      notes: 'ثبت سریع لیوان آب',
      inputSource: 'QUICK_ADD'
    });
  };

  const handleSaveSleep = (e: React.FormEvent) => {
    e.preventDefault();
    const duration = Math.round(parseFloat(sleepHours) * 60) || 450;
    addSleepRecord({
      sleepStartMillis: Date.now() - duration * 60000,
      wakeUpMillis: Date.now(),
      durationMinutes: duration,
      localEpochDay: todayEpoch,
      qualityRating: sleepQuality,
      notes: 'ثبت دستی خواب دیشب',
      inputSource: 'MANUAL'
    });
    alert('خواب با موفقیت ثبت شد!');
  };

  const handleSaveExercise = (e: React.FormEvent) => {
    e.preventDefault();
    const duration = parseInt(exerciseMinutes) || 30;
    addExerciseRecord({
      activityType: exerciseType,
      durationMinutes: duration,
      distanceMeters: duration * 80,
      intensity: 'MODERATE',
      timestampMillis: Date.now(),
      localEpochDay: todayEpoch,
      notes: 'ثبت تمرین امروز',
      inputSource: 'MANUAL'
    });
    alert('تمرین با موفقیت ثبت شد!');
  };

  const handleCreateHabit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newHabitTitle.trim()) return;
    addHabit({
      title: newHabitTitle.trim(),
      category: 'شخصی',
      scheduleType: 'DAILY',
      scheduleDays: [1, 2, 3, 4, 5, 6, 7],
      targetPerPeriod: 1,
      archived: false
    });
    setNewHabitTitle('');
    setIsAddingHabit(false);
  };

  // Filter tasks
  const filteredTasks = tasks.filter(t => {
    if (priorityFilter === 'ALL') return true;
    return t.priority === priorityFilter;
  });

  const toggleTaskExpansion = (id: string) => {
    setExpandedTaskIds(prev => ({ ...prev, [id]: !prev[id] }));
  };

  return (
    <div className="space-y-5 pb-24 max-w-7xl mx-auto">
      {/* Subtabs Bar */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3">
        <div className="flex rounded-xl bg-slate-900/80 p-1 border border-white/5 text-xs overflow-x-auto">
          <button
            onClick={() => setActiveTab('TASKS')}
            className={`px-3 py-1.5 rounded-lg whitespace-nowrap transition cursor-pointer flex items-center gap-1.5 ${
              activeTab === 'TASKS'
                ? 'bg-teal-500/20 text-teal-300 font-bold border border-teal-500/30'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <CheckSquare className="w-3.5 h-3.5" />
            اهداف و تسک‌ها ({toPersianDigits(tasks.filter(t => t.status !== 'COMPLETED').length)})
          </button>
          <button
            onClick={() => setActiveTab('HABITS')}
            className={`px-3 py-1.5 rounded-lg whitespace-nowrap transition cursor-pointer flex items-center gap-1.5 ${
              activeTab === 'HABITS'
                ? 'bg-amber-500/20 text-amber-300 font-bold border border-amber-500/30'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Flame className="w-3.5 h-3.5" />
            عادت‌های روزانه ({toPersianDigits(habits.length)})
          </button>
          <button
            onClick={() => setActiveTab('HEALTH')}
            className={`px-3 py-1.5 rounded-lg whitespace-nowrap transition cursor-pointer flex items-center gap-1.5 ${
              activeTab === 'HEALTH'
                ? 'bg-cyan-500/20 text-cyan-300 font-bold border border-cyan-500/30'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Droplets className="w-3.5 h-3.5" />
            سلامت و روتین (آب، خواب، ورزش)
          </button>
        </div>

        {activeTab === 'TASKS' && (
          <button
            onClick={onOpenAddTask}
            className="px-4 py-2 rounded-xl glass-button-primary text-xs font-bold flex items-center justify-center gap-1.5 cursor-pointer shrink-0"
          >
            <Plus className="w-4 h-4" />
            <span>تعریف تسک جدید</span>
          </button>
        )}

        {activeTab === 'HABITS' && (
          <button
            onClick={() => setIsAddingHabit(true)}
            className="px-4 py-2 rounded-xl glass-button-primary text-xs font-bold flex items-center justify-center gap-1.5 cursor-pointer shrink-0"
          >
            <Plus className="w-4 h-4" />
            <span>عادت جدید</span>
          </button>
        )}
      </div>

      {/* Tab 1: Tasks (Eisenhower Matrix) */}
      {activeTab === 'TASKS' && (
        <div className="space-y-4">
          {/* Priority filter buttons (Eisenhower mapping) */}
          <div className="flex flex-wrap items-center gap-1.5 text-xs">
            <span className="text-slate-400 text-[11px] ml-1">فیلتر اولویت:</span>
            <button
              onClick={() => setPriorityFilter('ALL')}
              className={`px-2.5 py-1 rounded-lg transition cursor-pointer ${
                priorityFilter === 'ALL'
                  ? 'bg-teal-500/20 text-teal-300 font-bold border border-teal-500/30'
                  : 'bg-slate-800/40 text-slate-400 hover:text-slate-200'
              }`}
            >
              همه ({toPersianDigits(tasks.length)})
            </button>
            <button
              onClick={() => setPriorityFilter('CRITICAL')}
              className={`px-2.5 py-1 rounded-lg transition cursor-pointer ${
                priorityFilter === 'CRITICAL'
                  ? 'bg-rose-500/20 text-rose-300 font-bold border border-rose-500/30'
                  : 'bg-slate-800/40 text-slate-400 hover:text-slate-200'
              }`}
            >
              فوری و مهم
            </button>
            <button
              onClick={() => setPriorityFilter('HIGH')}
              className={`px-2.5 py-1 rounded-lg transition cursor-pointer ${
                priorityFilter === 'HIGH'
                  ? 'bg-amber-500/20 text-amber-300 font-bold border border-amber-500/30'
                  : 'bg-slate-800/40 text-slate-400 hover:text-slate-200'
              }`}
            >
              مهم
            </button>
            <button
              onClick={() => setPriorityFilter('MEDIUM')}
              className={`px-2.5 py-1 rounded-lg transition cursor-pointer ${
                priorityFilter === 'MEDIUM'
                  ? 'bg-teal-500/20 text-teal-300 font-bold border border-teal-500/30'
                  : 'bg-slate-800/40 text-slate-400 hover:text-slate-200'
              }`}
            >
              فوری
            </button>
            <button
              onClick={() => setPriorityFilter('LOW')}
              className={`px-2.5 py-1 rounded-lg transition cursor-pointer ${
                priorityFilter === 'LOW'
                  ? 'bg-slate-700 text-slate-200 font-bold'
                  : 'bg-slate-800/40 text-slate-400 hover:text-slate-200'
              }`}
            >
              عادی
            </button>
          </div>

          {/* Tasks List */}
          {filteredTasks.length === 0 ? (
            <div className="glass-panel p-10 rounded-2xl text-center space-y-2 border border-white/5">
              <CheckSquare className="w-10 h-10 text-slate-500 mx-auto" />
              <p className="text-sm font-semibold text-slate-300">تسک یا هدفی در این بخش نیست</p>
              <p className="text-xs text-slate-500">با دکمه «تعریف تسک جدید» اهداف امروز خود را مشخص کنید.</p>
            </div>
          ) : (
            <div className="space-y-2.5">
              {filteredTasks.map(task => {
                const isCompleted = task.status === 'COMPLETED';
                const isExpanded = !!expandedTaskIds[task.id];
                const completedSubtasks = task.subtasks.filter(st => st.completed).length;

                return (
                  <div
                    key={task.id}
                    className={`glass-panel glass-card-hover p-4 rounded-xl border transition-all ${
                      isCompleted
                        ? 'border-emerald-500/20 bg-emerald-950/10 opacity-75'
                        : task.priority === 'CRITICAL'
                        ? 'border-rose-500/30'
                        : 'border-white/5'
                    }`}
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div className="flex items-start gap-3">
                        {/* Checkbox */}
                        <button
                          onClick={() => toggleTaskCompleted(task.id)}
                          className="mt-0.5 text-slate-400 hover:text-teal-400 transition cursor-pointer"
                        >
                          {isCompleted ? (
                            <CheckCircle2 className="w-5 h-5 text-emerald-400 fill-emerald-400/20" />
                          ) : (
                            <Circle className="w-5 h-5 text-slate-500 hover:text-teal-400" />
                          )}
                        </button>

                        {/* Title & info */}
                        <div className="space-y-1">
                          <div className="flex flex-wrap items-center gap-2">
                            <h3
                              className={`text-xs sm:text-sm font-bold ${
                                isCompleted ? 'line-through text-slate-500' : 'text-slate-100'
                              }`}
                            >
                              {task.title}
                            </h3>

                            {/* Priority badge */}
                            <span
                              className={`text-[10px] px-2 py-0.2 rounded-full font-semibold ${
                                task.priority === 'CRITICAL'
                                  ? 'bg-rose-500/20 text-rose-300 border border-rose-500/30'
                                  : task.priority === 'HIGH'
                                  ? 'bg-amber-500/20 text-amber-300 border border-amber-500/30'
                                  : task.priority === 'MEDIUM'
                                  ? 'bg-teal-500/20 text-teal-300 border border-teal-500/30'
                                  : 'bg-slate-800 text-slate-400'
                              }`}
                            >
                              {task.priority === 'CRITICAL' && 'فوری و مهم'}
                              {task.priority === 'HIGH' && 'مهم'}
                              {task.priority === 'MEDIUM' && 'فوری'}
                              {task.priority === 'LOW' && 'عادی'}
                            </span>

                            {/* Category badge */}
                            <span className="text-[10px] px-1.5 py-0.2 rounded bg-slate-800 text-slate-400 border border-white/5">
                              {task.category}
                            </span>

                            {/* Recurrence rule */}
                            {task.recurrenceRule !== 'NONE' && (
                              <span className="text-[10px] px-1.5 py-0.2 rounded bg-purple-500/10 text-purple-300 border border-purple-500/20 flex items-center gap-1">
                                <Repeat className="w-3 h-3" />
                                {task.recurrenceRule === 'DAILY' && 'روزانه'}
                                {task.recurrenceRule === 'WEEKLY' && 'هفتگی'}
                                {task.recurrenceRule === 'MONTHLY' && 'ماهانه'}
                              </span>
                            )}
                          </div>

                          {task.description && (
                            <p className="text-xs text-slate-400 leading-relaxed">
                              {task.description}
                            </p>
                          )}

                          <div className="flex items-center gap-3 text-[11px] text-slate-500 pt-1">
                            <span className="flex items-center gap-1">
                              <Calendar className="w-3 h-3" />
                              {formatEpochDay(task.dueDateEpochDay, preferences.calendarDisplay)}
                            </span>
                            {task.subtasks.length > 0 && (
                              <button
                                onClick={() => toggleTaskExpansion(task.id)}
                                className="text-teal-400 hover:text-teal-300 flex items-center gap-1 cursor-pointer font-medium"
                              >
                                {toPersianDigits(completedSubtasks)} از {toPersianDigits(task.subtasks.length)} زیرتسک
                                {isExpanded ? <ChevronUp className="w-3 h-3" /> : <ChevronDown className="w-3 h-3" />}
                              </button>
                            )}
                            {task.linkedDebtId && (
                              <span className="text-amber-400">
                                🔗 متصل به بدهی
                              </span>
                            )}
                          </div>
                        </div>
                      </div>

                      {/* Actions */}
                      <button
                        onClick={() => deleteTask(task.id)}
                        className="p-1.5 text-slate-500 hover:text-rose-400 transition cursor-pointer"
                        title="حذف تسک"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>

                    {/* Subtasks checklist if expanded */}
                    {isExpanded && task.subtasks.length > 0 && (
                      <div className="mt-3 pt-3 border-t border-white/5 space-y-1.5 pl-8 pr-2">
                        {task.subtasks.map(st => (
                          <div
                            key={st.id}
                            className="flex items-center gap-2 text-xs text-slate-300 hover:text-white"
                          >
                            <input
                              type="checkbox"
                              checked={st.completed}
                              onChange={() => toggleSubtask(task.id, st.id)}
                              className="rounded border-slate-700 text-teal-500 focus:ring-0 cursor-pointer"
                            />
                            <span className={st.completed ? 'line-through text-slate-500' : ''}>
                              {st.title}
                            </span>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}

      {/* Tab 2: Habits Tracker */}
      {activeTab === 'HABITS' && (
        <div className="space-y-4">
          {/* Add Habit Form Modal/Panel */}
          {isAddingHabit && (
            <form onSubmit={handleCreateHabit} className="glass-panel p-4 rounded-xl border border-teal-500/30 space-y-3">
              <h4 className="text-xs font-bold text-teal-300">تعریف عادت جدید</h4>
              <input
                type="text"
                required
                value={newHabitTitle}
                onChange={e => setNewHabitTitle(e.target.value)}
                placeholder="عنوان عادت (مثال: مطالعه روزانه، ۲۰ دقیقه مدیتیشن)..."
                className="w-full rounded-lg bg-slate-900 border border-white/10 p-2 text-xs text-slate-100"
              />
              <div className="flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setIsAddingHabit(false)}
                  className="px-3 py-1 text-xs text-slate-400 hover:text-white"
                >
                  انصراف
                </button>
                <button
                  type="submit"
                  className="px-4 py-1.5 rounded-lg glass-button-primary text-xs font-semibold"
                >
                  ذخیره عادت
                </button>
              </div>
            </form>
          )}

          {/* Habits Cards */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {habits.map(habit => {
              const isDoneToday = habitRecords.some(
                r => r.habitId === habit.id && r.localEpochDay === todayEpoch
              );

              // Calculate streak (consecutive days)
              let streak = 0;
              let checkDay = isDoneToday ? todayEpoch : todayEpoch - 1;
              while (habitRecords.some(r => r.habitId === habit.id && r.localEpochDay === checkDay)) {
                streak++;
                checkDay--;
              }

              return (
                <div
                  key={habit.id}
                  className="glass-panel p-4 rounded-xl border border-white/5 space-y-3"
                >
                  <div className="flex items-center justify-between">
                    <div className="space-y-0.5">
                      <div className="flex items-center gap-2">
                        <h4 className="text-xs sm:text-sm font-bold text-slate-100">
                          {habit.title}
                        </h4>
                        <span className="text-[10px] px-1.5 py-0.2 rounded bg-slate-800 text-slate-400">
                          {habit.category}
                        </span>
                      </div>
                      <div className="flex items-center gap-2 text-[11px] text-amber-400 font-semibold">
                        <Flame className="w-3.5 h-3.5 fill-amber-400" />
                        <span>زنجیره پیوستگی: {toPersianDigits(streak)} روز متوالی</span>
                      </div>
                    </div>

                    <button
                      onClick={() => toggleHabitToday(habit.id)}
                      className={`w-10 h-10 rounded-xl flex items-center justify-center transition-all cursor-pointer ${
                        isDoneToday
                          ? 'bg-emerald-500 text-slate-950 font-bold shadow-lg shadow-emerald-500/30'
                          : 'bg-slate-800 text-slate-400 hover:bg-slate-700 hover:text-white'
                      }`}
                      title={isDoneToday ? 'تکمیل شده برای امروز' : 'کلیک برای ثبت انجام امروز'}
                    >
                      <CheckCircle2 className="w-5 h-5" />
                    </button>
                  </div>

                  {/* 7-Day Dots Indicator */}
                  <div className="flex items-center justify-between pt-1">
                    <span className="text-[10px] text-slate-500">وضعیت ۷ روز گذشته:</span>
                    <div className="flex items-center gap-1.5">
                      {[6, 5, 4, 3, 2, 1, 0].map(offset => {
                        const dEpoch = todayEpoch - offset;
                        const wasDone = habitRecords.some(
                          r => r.habitId === habit.id && r.localEpochDay === dEpoch
                        );
                        return (
                          <div
                            key={offset}
                            title={offset === 0 ? 'امروز' : `${toPersianDigits(offset)} روز پیش`}
                            className={`w-3.5 h-3.5 rounded-full transition ${
                              wasDone
                                ? 'bg-teal-400 shadow-[0_0_6px_#2dd4bf]'
                                : 'bg-slate-800 border border-white/5'
                            }`}
                          />
                        );
                      })}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Tab 3: Health & Wellness (Water wave, Sleep, Exercise) */}
      {activeTab === 'HEALTH' && (
        <div className="space-y-4">
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
            {/* 1. Water Wave Glass Card */}
            <div className="glass-panel p-5 rounded-2xl border border-teal-500/20 relative overflow-hidden flex flex-col justify-between h-80">
              {/* Animated wave background */}
              <div
                className="absolute inset-x-0 bottom-0 bg-teal-500/20 transition-all duration-700 rounded-b-2xl overflow-hidden"
                style={{ height: `${waterPercent}%` }}
              >
                <div className="absolute inset-0 bg-gradient-to-t from-teal-500/30 to-teal-400/10" />
              </div>

              {/* Card Header */}
              <div className="relative z-10 space-y-1">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-teal-300 flex items-center gap-1.5">
                    <Droplets className="w-4 h-4 text-teal-400" />
                    پایش نوشیدن آب (Water Wave)
                  </span>
                  <span className="text-xs font-bold text-teal-200">
                    {toPersianDigits(waterPercent)}٪
                  </span>
                </div>
                <p className="text-[11px] text-slate-400">
                  هدف روزانه: {toPersianDigits(waterTarget)} میلی‌لیتر
                </p>
              </div>

              {/* Center Amount */}
              <div className="relative z-10 text-center py-4">
                <div className="text-3xl font-extrabold text-slate-100 font-mono">
                  {toPersianDigits(todayWaterMl)}
                  <span className="text-xs text-slate-400 font-sans mr-1">ml</span>
                </div>
                <span className="text-[11px] text-teal-300/80 font-medium block mt-1">
                  مانده تا هدف: {toPersianDigits(Math.max(0, waterTarget - todayWaterMl))} ml
                </span>
              </div>

              {/* Quick Action Buttons */}
              <div className="relative z-10 grid grid-cols-2 gap-2">
                <button
                  onClick={() => handleQuickAddWater(250)}
                  className="py-2 px-3 rounded-xl bg-slate-900/80 hover:bg-slate-800 text-xs font-semibold text-teal-200 border border-teal-500/30 transition cursor-pointer flex items-center justify-center gap-1"
                >
                  <Plus className="w-3.5 h-3.5" />
                  +۲۵۰ ml (لیوان)
                </button>
                <button
                  onClick={() => handleQuickAddWater(500)}
                  className="py-2 px-3 rounded-xl bg-teal-600/30 hover:bg-teal-600/50 text-xs font-semibold text-white border border-teal-500/40 transition cursor-pointer flex items-center justify-center gap-1"
                >
                  <Plus className="w-3.5 h-3.5" />
                  +۵۰۰ ml (بطری)
                </button>
              </div>
            </div>

            {/* 2. Sleep Tracking Card */}
            <div className="glass-panel p-5 rounded-2xl border border-purple-500/20 flex flex-col justify-between space-y-3">
              <div className="space-y-1">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-purple-300 flex items-center gap-1.5">
                    <Moon className="w-4 h-4 text-purple-400" />
                    پایش کیفیت و مدت خواب
                  </span>
                  <span className="text-[11px] text-purple-200">
                    هدف: ۸ ساعت
                  </span>
                </div>
                <p className="text-[11px] text-slate-400">
                  ثبت ساعات خواب دیشب برای ارزیابی ریکاوری ذهنی
                </p>
              </div>

              <form onSubmit={handleSaveSleep} className="space-y-3">
                <div>
                  <label className="text-[11px] text-slate-300 block mb-1">مدت خواب (ساعت):</label>
                  <input
                    type="number"
                    step="0.5"
                    min="1"
                    max="14"
                    value={sleepHours}
                    onChange={e => setSleepHours(e.target.value)}
                    className="w-full rounded-xl bg-slate-900/80 border border-white/10 px-3 py-1.5 text-xs text-slate-100"
                  />
                </div>

                <div>
                  <label className="text-[11px] text-slate-300 block mb-1">کیفیت خواب (امتیاز ۱ تا ۵):</label>
                  <div className="flex items-center gap-2">
                    {[1, 2, 3, 4, 5].map(star => (
                      <button
                        key={star}
                        type="button"
                        onClick={() => setSleepQuality(star)}
                        className="text-amber-400 cursor-pointer"
                      >
                        <Star className={`w-5 h-5 ${star <= sleepQuality ? 'fill-amber-400' : 'text-slate-600'}`} />
                      </button>
                    ))}
                  </div>
                </div>

                <button
                  type="submit"
                  className="w-full py-2 rounded-xl bg-purple-600/30 hover:bg-purple-600/50 text-xs font-semibold text-purple-200 border border-purple-500/30 transition cursor-pointer"
                >
                  ثبت خواب دیشب
                </button>
              </form>

              {sleepRecords.length > 0 && (
                <div className="text-[11px] text-slate-400 pt-1 border-t border-white/5">
                  آخرین ثبت: {toPersianDigits((sleepRecords[0].durationMinutes / 60).toFixed(1))} ساعت با {toPersianDigits(sleepRecords[0].qualityRating)} ستاره
                </div>
              )}
            </div>

            {/* 3. Exercise & Workout Card */}
            <div className="glass-panel p-5 rounded-2xl border border-emerald-500/20 flex flex-col justify-between space-y-3">
              <div className="space-y-1">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-emerald-300 flex items-center gap-1.5">
                    <Activity className="w-4 h-4 text-emerald-400" />
                    ورزش و فعالیت بدنی
                  </span>
                  <span className="text-[11px] text-emerald-200">
                    هدف: ۳۰ دقیقه
                  </span>
                </div>
                <p className="text-[11px] text-slate-400">
                  ثبت پیاده‌روی، تمرینات هوازی یا بدنسازی
                </p>
              </div>

              <form onSubmit={handleSaveExercise} className="space-y-3">
                <div>
                  <label className="text-[11px] text-slate-300 block mb-1">نوع فعالیت:</label>
                  <select
                    value={exerciseType}
                    onChange={e => setExerciseType(e.target.value)}
                    className="w-full rounded-xl bg-slate-900/80 border border-white/10 px-3 py-1.5 text-xs text-slate-100"
                  >
                    <option value="پیاده‌روی تند">پیاده‌روی تند</option>
                    <option value="دویدن">دویدن</option>
                    <option value="تمرین با وزنه / بدنسازی">تمرین با وزنه / بدنسازی</option>
                    <option value="دوچرخه‌سواری">دوچرخه‌سواری</option>
                    <option value="شنا">شنا</option>
                    <option value="یوگا و کشش">یوگا و کشش</option>
                  </select>
                </div>

                <div>
                  <label className="text-[11px] text-slate-300 block mb-1">مدت زمان (دقیقه):</label>
                  <input
                    type="number"
                    min="5"
                    max="300"
                    value={exerciseMinutes}
                    onChange={e => setExerciseMinutes(e.target.value)}
                    className="w-full rounded-xl bg-slate-900/80 border border-white/10 px-3 py-1.5 text-xs text-slate-100"
                  />
                </div>

                <button
                  type="submit"
                  className="w-full py-2 rounded-xl bg-emerald-600/30 hover:bg-emerald-600/50 text-xs font-semibold text-emerald-200 border border-emerald-500/30 transition cursor-pointer"
                >
                  ثبت فعالیت ورزشی
                </button>
              </form>

              {exerciseRecords.length > 0 && (
                <div className="text-[11px] text-slate-400 pt-1 border-t border-white/5">
                  آخرین تمرین: {exerciseRecords[0].activityType} ({toPersianDigits(exerciseRecords[0].durationMinutes)} دقیقه)
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
