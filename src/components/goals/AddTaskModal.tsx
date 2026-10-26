import React, { useState } from 'react';
import {
  X,
  Check,
  Plus,
  Trash2,
  Calendar,
  Clock,
  Repeat,
  AlertCircle
} from 'lucide-react';
import { useLifeOs } from '../../context/LifeOsContext';
import { TaskPriority, RecurrenceRule, SubtaskItem } from '../../types/lifeOs';
import { currentLocalEpochDay, toPersianDigits } from '../../utils/persianDateUtils';

interface AddTaskModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const AddTaskModal: React.FC<AddTaskModalProps> = ({
  isOpen,
  onClose
}) => {
  const { addTask, expenses } = useLifeOs();

  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [category, setCategory] = useState('کار و پروژه');
  const [priority, setPriority] = useState<TaskPriority>('MEDIUM');
  const [dueOffsetDays, setDueOffsetDays] = useState(0); // 0 = today, 1 = tomorrow
  const [dueTimeMinutes, setDueTimeMinutes] = useState(600); // 10:00 AM
  const [recurrence, setRecurrence] = useState<RecurrenceRule>('NONE');
  const [linkedDebtId, setLinkedDebtId] = useState('');
  const [newSubtaskTitle, setNewSubtaskTitle] = useState('');
  const [subtasks, setSubtasks] = useState<SubtaskItem[]>([]);

  if (!isOpen) return null;

  const todayEpoch = currentLocalEpochDay();

  const handleAddSubtask = () => {
    if (!newSubtaskTitle.trim()) return;
    const newItem: SubtaskItem = {
      id: `st_${Date.now()}_${Math.random().toString(36).substr(2, 4)}`,
      title: newSubtaskTitle.trim(),
      completed: false
    };
    setSubtasks(prev => [...prev, newItem]);
    setNewSubtaskTitle('');
  };

  const handleRemoveSubtask = (id: string) => {
    setSubtasks(prev => prev.filter(st => st.id !== id));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim()) return;

    addTask({
      title: title.trim(),
      description: description.trim(),
      category,
      tags: [category],
      priority,
      dueDateEpochDay: todayEpoch + dueOffsetDays,
      dueTimeMinutes,
      estimatedDurationMinutes: 30,
      status: 'PENDING',
      subtasks,
      recurrenceRule: recurrence,
      recurrenceIntervalDays: recurrence === 'DAILY' ? 1 : recurrence === 'WEEKLY' ? 7 : recurrence === 'MONTHLY' ? 30 : 0,
      notes: '',
      inputSource: 'MANUAL',
      sortOrder: 0,
      linkedDebtId: linkedDebtId || undefined
    });

    onClose();
  };

  // Find active debts for linkage
  const activeDebts = expenses.filter(
    e => (e.transactionType === 'DEBT' || e.transactionType === 'RECEIVABLE') &&
      e.amount > (e.paidAmount || 0)
  );

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-md">
      <div className="glass-panel w-full max-w-lg rounded-2xl border border-white/10 shadow-2xl overflow-hidden max-h-[92vh] flex flex-col">
        {/* Header */}
        <div className="px-5 py-3.5 border-b border-white/10 flex items-center justify-between">
          <h2 className="text-sm sm:text-base font-bold text-slate-100 flex items-center gap-2">
            <Plus className="w-4 h-4 text-teal-400" />
            تعریف تسک و هدف جدید
          </h2>
          <button
            onClick={onClose}
            className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-white/5 transition cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content Form */}
        <form onSubmit={handleSubmit} className="p-5 space-y-4 overflow-y-auto">
          {/* Title */}
          <div>
            <label className="text-xs font-semibold text-slate-300 block mb-1">
              عنوان تسک / هدف:
            </label>
            <input
              type="text"
              required
              value={title}
              onChange={e => setTitle(e.target.value)}
              placeholder="مثال: مطالعه فصل ۳ کتاب، پرداخت قسط، جلسه با تیم..."
              className="w-full rounded-xl bg-slate-900/80 border border-white/10 px-3 py-2 text-xs sm:text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:border-teal-500/50"
            />
          </div>

          {/* Description */}
          <div>
            <label className="text-xs font-semibold text-slate-300 block mb-1">
              توضیحات تکمیلی (اختیاری):
            </label>
            <textarea
              rows={2}
              value={description}
              onChange={e => setDescription(e.target.value)}
              placeholder="نکات مهم و جزئیات تسک..."
              className="w-full rounded-xl bg-slate-900/80 border border-white/10 p-2.5 text-xs text-slate-100 placeholder-slate-500 focus:outline-none focus:border-teal-500/50"
            />
          </div>

          {/* Priority (Eisenhower Matrix) */}
          <div>
            <label className="text-xs font-semibold text-slate-300 block mb-1.5">
              اولویت (ماتریس آیزنهاور):
            </label>
            <div className="grid grid-cols-4 gap-1.5 text-xs font-medium text-center">
              <button
                type="button"
                onClick={() => setPriority('CRITICAL')}
                className={`py-1.5 rounded-lg transition cursor-pointer ${
                  priority === 'CRITICAL'
                    ? 'bg-rose-500/25 text-rose-300 font-bold border border-rose-500/40'
                    : 'bg-slate-800/60 text-slate-400 hover:text-slate-200'
                }`}
              >
                فوری و مهم
              </button>
              <button
                type="button"
                onClick={() => setPriority('HIGH')}
                className={`py-1.5 rounded-lg transition cursor-pointer ${
                  priority === 'HIGH'
                    ? 'bg-amber-500/25 text-amber-300 font-bold border border-amber-500/40'
                    : 'bg-slate-800/60 text-slate-400 hover:text-slate-200'
                }`}
              >
                مهم
              </button>
              <button
                type="button"
                onClick={() => setPriority('MEDIUM')}
                className={`py-1.5 rounded-lg transition cursor-pointer ${
                  priority === 'MEDIUM'
                    ? 'bg-teal-500/25 text-teal-300 font-bold border border-teal-500/40'
                    : 'bg-slate-800/60 text-slate-400 hover:text-slate-200'
                }`}
              >
                فوری
              </button>
              <button
                type="button"
                onClick={() => setPriority('LOW')}
                className={`py-1.5 rounded-lg transition cursor-pointer ${
                  priority === 'LOW'
                    ? 'bg-slate-700 text-slate-200 font-bold border border-slate-600'
                    : 'bg-slate-800/60 text-slate-400 hover:text-slate-200'
                }`}
              >
                عادی
              </button>
            </div>
          </div>

          {/* Due date and Recurrence */}
          <div className="grid grid-cols-2 gap-2 text-xs">
            <div>
              <label className="text-slate-300 font-semibold block mb-1">موعد انجام:</label>
              <select
                value={dueOffsetDays}
                onChange={e => setDueOffsetDays(parseInt(e.target.value))}
                className="w-full rounded-xl bg-slate-900/80 border border-white/10 p-2 text-slate-200"
              >
                <option value={0}>امروز</option>
                <option value={1}>فردا</option>
                <option value={2}>۲ روز دیگر</option>
                <option value={7}>هفته آینده</option>
                <option value={30}>ماه آینده</option>
              </select>
            </div>

            <div>
              <label className="text-slate-300 font-semibold block mb-1">تکرار خودکار:</label>
              <select
                value={recurrence}
                onChange={e => setRecurrence(e.target.value as any)}
                className="w-full rounded-xl bg-slate-900/80 border border-white/10 p-2 text-slate-200"
              >
                <option value="NONE">بدون تکرار</option>
                <option value="DAILY">روزانه</option>
                <option value="WEEKLY">هفتگی</option>
                <option value="MONTHLY">ماهانه</option>
              </select>
            </div>
          </div>

          {/* Link to active debt if any */}
          {activeDebts.length > 0 && (
            <div>
              <label className="text-xs font-semibold text-slate-300 block mb-1">
                اتصال به پرداخت بدهی / قسط (اختیاری):
              </label>
              <select
                value={linkedDebtId}
                onChange={e => setLinkedDebtId(e.target.value)}
                className="w-full rounded-xl bg-slate-900/80 border border-white/10 p-2 text-xs text-slate-200"
              >
                <option value="">عدم اتصال به بدهی</option>
                {activeDebts.map(d => (
                  <option key={d.id} value={d.id}>
                    {d.title} (مانده: {Math.round(d.amount - (d.paidAmount || 0)).toLocaleString()} تومان)
                  </option>
                ))}
              </select>
            </div>
          )}

          {/* Subtasks checklist builder */}
          <div className="space-y-2">
            <label className="text-xs font-semibold text-slate-300 block">
              زیرتسک‌ها (Checklist):
            </label>
            <div className="flex gap-2">
              <input
                type="text"
                value={newSubtaskTitle}
                onChange={e => setNewSubtaskTitle(e.target.value)}
                onKeyDown={e => {
                  if (e.key === 'Enter') {
                    e.preventDefault();
                    handleAddSubtask();
                  }
                }}
                placeholder="عنوان گام یا مرحله جدید..."
                className="flex-1 rounded-xl bg-slate-900/80 border border-white/10 px-3 py-1.5 text-xs text-slate-100 placeholder-slate-500 focus:outline-none focus:border-teal-500/50"
              />
              <button
                type="button"
                onClick={handleAddSubtask}
                className="px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-xs font-semibold text-teal-300 border border-white/5 cursor-pointer"
              >
                افزودن
              </button>
            </div>

            {subtasks.length > 0 && (
              <div className="space-y-1 max-h-32 overflow-y-auto pt-1">
                {subtasks.map((st, idx) => (
                  <div
                    key={st.id}
                    className="flex items-center justify-between p-2 rounded-lg bg-slate-900/40 border border-white/5 text-xs text-slate-200"
                  >
                    <span>{idx + 1}. {st.title}</span>
                    <button
                      type="button"
                      onClick={() => handleRemoveSubtask(st.id)}
                      className="text-slate-500 hover:text-rose-400 cursor-pointer"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Footer Submit */}
          <div className="pt-2 flex items-center justify-end gap-2 border-t border-white/10">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-xs text-slate-400 hover:text-white transition cursor-pointer"
            >
              انصراف
            </button>
            <button
              type="submit"
              className="px-5 py-2 rounded-xl glass-button-primary text-xs font-semibold flex items-center gap-1.5 cursor-pointer"
            >
              <Check className="w-4 h-4" />
              <span>ذخیره تسک</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
