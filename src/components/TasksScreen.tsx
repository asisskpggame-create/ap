import React, { useState } from 'react';
import {
  CheckSquare,
  Plus,
  Trash2,
  Flame,
  Calendar,
  Tag,
  CheckCircle2,
  Circle,
  Sparkles,
} from 'lucide-react';
import { TaskItem, HabitItem, TaskCategory, TaskPriority } from '../types';
import { formatPersianNumber } from '../utils/persianUtils';

interface TasksScreenProps {
  tasks: TaskItem[];
  habits: HabitItem[];
  onAddTask: (task: Omit<TaskItem, 'id' | 'createdAt'>) => void;
  onToggleTask: (id: string) => void;
  onDeleteTask: (id: string) => void;
  onToggleHabit: (id: string) => void;
  onAddHabit: (title: string) => void;
}

export const TasksScreen: React.FC<TasksScreenProps> = ({
  tasks,
  habits,
  onAddTask,
  onToggleTask,
  onDeleteTask,
  onToggleHabit,
  onAddHabit,
}) => {
  const [newTaskTitle, setNewTaskTitle] = useState('');
  const [newTaskCategory, setNewTaskCategory] = useState<TaskCategory>('work');
  const [newTaskPriority, setNewTaskPriority] = useState<TaskPriority>('medium');
  const [newTaskDueDate, setNewTaskDueDate] = useState('۱۴۰۵/۰۸/۱۰');
  const [newHabitTitle, setNewHabitTitle] = useState('');
  const [activeFilter, setActiveFilter] = useState<'all' | 'pending' | 'completed'>('all');

  const handleCreateTask = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newTaskTitle.trim()) return;
    onAddTask({
      title: newTaskTitle.trim(),
      category: newTaskCategory,
      priority: newTaskPriority,
      completed: false,
      dueDate: newTaskDueDate,
    });
    setNewTaskTitle('');
  };

  const handleCreateHabit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newHabitTitle.trim()) return;
    onAddHabit(newHabitTitle.trim());
    setNewHabitTitle('');
  };

  const filteredTasks = tasks.filter((t) => {
    if (activeFilter === 'pending') return !t.completed;
    if (activeFilter === 'completed') return t.completed;
    return true;
  });

  const priorityBadge = (priority: TaskPriority) => {
    switch (priority) {
      case 'urgent':
        return 'bg-rose-500/20 text-rose-300 border-rose-500/30';
      case 'high':
        return 'bg-amber-500/20 text-amber-300 border-amber-500/30';
      case 'medium':
        return 'bg-cyan-500/20 text-cyan-300 border-cyan-500/30';
      default:
        return 'bg-slate-700/50 text-slate-300 border-slate-600/40';
    }
  };

  const priorityLabel = (priority: TaskPriority) => {
    switch (priority) {
      case 'urgent':
        return 'فوری';
      case 'high':
        return 'بالا';
      case 'medium':
        return 'متوسط';
      default:
        return 'عادی';
    }
  };

  const categoryLabel = (category: TaskCategory) => {
    switch (category) {
      case 'work':
        return 'کاری';
      case 'finance':
        return 'مالی';
      case 'health':
        return 'سلامت';
      case 'study':
        return 'مطالعه';
      default:
        return 'شخصی';
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white flex items-center gap-2">
            <CheckSquare className="w-7 h-7 text-cyan-400" />
            اهداف، وظایف و عادت‌های روزانه
          </h1>
          <p className="text-slate-400 text-sm mt-1">
            مدیریت متمرکز کارها، اقساط مرتبط و زنجیره عادت‌های روزانه
          </p>
        </div>
        <div className="flex items-center gap-2 bg-slate-900/80 p-1 rounded-xl border border-slate-800">
          {(['all', 'pending', 'completed'] as const).map((f) => (
            <button
              key={f}
              onClick={() => setActiveFilter(f)}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${
                activeFilter === f
                  ? 'bg-cyan-500 text-slate-950 font-bold'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              {f === 'all' ? 'همه' : f === 'pending' ? 'در انتظار' : 'انجام‌شده'}
            </button>
          ))}
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Tasks Section */}
        <div className="lg:col-span-2 space-y-4">
          <form
            onSubmit={handleCreateTask}
            className="glass-card rounded-2xl p-4 border border-slate-800/80 space-y-3"
          >
            <div className="flex flex-col sm:flex-row gap-2">
              <input
                type="text"
                value={newTaskTitle}
                onChange={(e) => setNewTaskTitle(e.target.value)}
                placeholder="عنوان کار یا هدف جدید..."
                className="flex-1 bg-slate-900/90 border border-slate-700/70 rounded-xl px-4 py-2.5 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-cyan-500"
              />
              <button
                type="submit"
                className="bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 text-slate-950 font-bold px-5 py-2.5 rounded-xl text-sm flex items-center justify-center gap-1.5 transition-all"
              >
                <Plus className="w-4 h-4" />
                افزودن کار
              </button>
            </div>
            <div className="flex flex-wrap items-center gap-2 text-xs">
              <select
                value={newTaskCategory}
                onChange={(e) => setNewTaskCategory(e.target.value as TaskCategory)}
                className="bg-slate-900 border border-slate-700/70 rounded-lg px-3 py-1.5 text-slate-200"
              >
                <option value="work">کاری</option>
                <option value="personal">شخصی</option>
                <option value="finance">مالی / قسط</option>
                <option value="health">سلامت</option>
                <option value="study">مطالعه</option>
              </select>
              <select
                value={newTaskPriority}
                onChange={(e) => setNewTaskPriority(e.target.value as TaskPriority)}
                className="bg-slate-900 border border-slate-700/70 rounded-lg px-3 py-1.5 text-slate-200"
              >
                <option value="low">اولویت عادی</option>
                <option value="medium">اولویت متوسط</option>
                <option value="high">اولویت بالا</option>
                <option value="urgent">فوری</option>
              </select>
              <input
                type="text"
                value={newTaskDueDate}
                onChange={(e) => setNewTaskDueDate(e.target.value)}
                placeholder="سررسید (مثلاً ۱۴۰۵/۰۸/۱۰)"
                className="bg-slate-900 border border-slate-700/70 rounded-lg px-3 py-1.5 text-slate-200 w-36"
              />
            </div>
          </form>

          <div className="space-y-2.5">
            {filteredTasks.map((task) => (
              <div
                key={task.id}
                className={`glass-card rounded-2xl p-4 border transition-all flex items-center justify-between gap-3 ${
                  task.completed
                    ? 'border-slate-800/50 opacity-60'
                    : 'border-slate-800 hover:border-cyan-500/30'
                }`}
              >
                <div className="flex items-center gap-3 min-w-0">
                  <button
                    onClick={() => onToggleTask(task.id)}
                    className="text-cyan-400 hover:text-cyan-300 shrink-0"
                  >
                    {task.completed ? (
                      <CheckCircle2 className="w-5 h-5 text-emerald-400" />
                    ) : (
                      <Circle className="w-5 h-5" />
                    )}
                  </button>
                  <div className="min-w-0">
                    <p
                      className={`text-sm font-medium truncate ${
                        task.completed ? 'line-through text-slate-500' : 'text-slate-100'
                      }`}
                    >
                      {task.title}
                    </p>
                    <div className="flex flex-wrap items-center gap-2 mt-1 text-xs text-slate-400">
                      <span className="inline-flex items-center gap-1">
                        <Tag className="w-3 h-3" />
                        {categoryLabel(task.category)}
                      </span>
                      <span className="inline-flex items-center gap-1">
                        <Calendar className="w-3 h-3" />
                        {task.dueDate}
                      </span>
                      <span
                        className={`px-2 py-0.5 rounded-full border text-[11px] ${priorityBadge(
                          task.priority
                        )}`}
                      >
                        {priorityLabel(task.priority)}
                      </span>
                    </div>
                  </div>
                </div>
                <button
                  onClick={() => onDeleteTask(task.id)}
                  className="text-slate-500 hover:text-rose-400 p-1.5 rounded-lg transition-colors"
                  title="حذف"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            ))}
          </div>
        </div>

        {/* Habits Section */}
        <div className="space-y-4">
          <div className="glass-card rounded-2xl p-5 border border-slate-800 space-y-4">
            <div className="flex items-center justify-between">
              <h2 className="text-base font-bold text-white flex items-center gap-2">
                <Flame className="w-5 h-5 text-amber-400" />
                زنجیره عادت‌های روزانه
              </h2>
              <Sparkles className="w-4 h-4 text-amber-400" />
            </div>

            <form onSubmit={handleCreateHabit} className="flex gap-2">
              <input
                type="text"
                value={newHabitTitle}
                onChange={(e) => setNewHabitTitle(e.target.value)}
                placeholder="عادت جدید..."
                className="flex-1 bg-slate-900 border border-slate-700/70 rounded-xl px-3 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-amber-400"
              />
              <button
                type="submit"
                className="bg-amber-500 hover:bg-amber-400 text-slate-950 font-bold px-3 py-2 rounded-xl text-xs"
              >
                افزودن
              </button>
            </form>

            <div className="space-y-2.5">
              {habits.map((habit) => (
                <div
                  key={habit.id}
                  onClick={() => onToggleHabit(habit.id)}
                  className={`p-3.5 rounded-xl border cursor-pointer transition-all flex items-center justify-between ${
                    habit.completedToday
                      ? 'bg-emerald-500/10 border-emerald-500/30'
                      : 'bg-slate-900/60 border-slate-800 hover:border-slate-700'
                  }`}
                >
                  <div className="flex items-center gap-2.5">
                    {habit.completedToday ? (
                      <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />
                    ) : (
                      <Circle className="w-5 h-5 text-slate-500 shrink-0" />
                    )}
                    <span className="text-sm font-medium text-slate-200">
                      {habit.title}
                    </span>
                  </div>
                  <span className="inline-flex items-center gap-1 text-xs font-bold text-amber-400 bg-amber-500/10 px-2.5 py-1 rounded-full">
                    <Flame className="w-3.5 h-3.5" />
                    {formatPersianNumber(habit.streak)} روز
                  </span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
