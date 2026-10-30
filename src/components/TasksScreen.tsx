import React, { useState } from 'react';
import { 
  CheckSquare, 
  Plus, 
  Search, 
  Flame, 
  Calendar, 
  Tag,
  Trash2,
  CheckCircle2,
  Circle
} from 'lucide-react';
import { TaskItem, HabitItem, TaskCategory, TaskPriority } from '../types';
import { normalizePersianText } from '../utils/persianUtils';
import confetti from 'canvas-confetti';

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
  onAddHabit
}) => {
  const [activeTab, setActiveTab] = useState<'tasks' | 'habits'>('tasks');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<string>('all');
  
  // New task form state
  const [newTitle, setNewTitle] = useState('');
  const [newCategory, setNewCategory] = useState<TaskCategory>('work');
  const [newPriority, setNewPriority] = useState<TaskPriority>('medium');
  const [newDueDate, setNewDueDate] = useState('۱۴۰۵/۰۸/۱۰');

  // New habit title
  const [newHabitTitle, setNewHabitTitle] = useState('');

  const filteredTasks = tasks.filter(t => {
    const matchesSearch = normalizePersianText(t.title).includes(normalizePersianText(searchQuery));
    const matchesCategory = selectedCategory === 'all' || t.category === selectedCategory;
    return matchesSearch && matchesCategory;
  });

  const handleCreateTask = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newTitle.trim()) return;
    onAddTask({
      title: newTitle,
      category: newCategory,
      priority: newPriority,
      dueDate: newDueDate,
      completed: false
    });
    setNewTitle('');
    confetti({ particleCount: 40, spread: 60, origin: { y: 0.8 } });
  };

  const handleCreateHabit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newHabitTitle.trim()) return;
    onAddHabit(newHabitTitle);
    setNewHabitTitle('');
  };

  return (
    <div className="space-y-6 animate-fadeIn">
      {/* Header & Sub-tabs */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-slate-100">مدیریت وظایف و عادت‌ها</h2>
          <p className="text-xs text-slate-400">سازماندهی هوشمند امور روزانه و پیگیری مستمر زنجیره عادت‌ها</p>
        </div>
        <div className="flex items-center bg-slate-900 p-1 rounded-xl border border-slate-800">
          <button
            onClick={() => setActiveTab('tasks')}
            className={`px-4 py-2 rounded-lg text-xs font-medium transition ${
              activeTab === 'tasks' ? 'bg-cyan-500 text-slate-950 font-bold' : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            وظایف ({tasks.length})
          </button>
          <button
            onClick={() => setActiveTab('habits')}
            className={`px-4 py-2 rounded-lg text-xs font-medium transition ${
              activeTab === 'habits' ? 'bg-cyan-500 text-slate-950 font-bold' : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            عادت‌ها ({habits.length})
          </button>
        </div>
      </div>

      {activeTab === 'tasks' ? (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Left Column: Task Creation & Filters */}
          <div className="space-y-6">
            <div className="glass-card rounded-2xl p-6 space-y-4">
              <h3 className="font-bold text-slate-100 text-base flex items-center space-x-2 space-x-reverse">
                <Plus className="w-4 h-4 text-cyan-400" />
                <span>افزودن وظیفه جدید</span>
              </h3>
              <form onSubmit={handleCreateTask} className="space-y-4">
                <div>
                  <label className="text-xs text-slate-400 block mb-1">عنوان وظیفه</label>
                  <input 
                    type="text"
                    value={newTitle}
                    onChange={e => setNewTitle(e.target.value)}
                    placeholder="مثلا: بررسی گزارش مالی ماهانه..."
                    className="w-full bg-slate-900 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-slate-200 focus:outline-none focus:border-cyan-500"
                  />
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="text-xs text-slate-400 block mb-1">دسته‌بندی</label>
                    <select
                      value={newCategory}
                      onChange={e => setNewCategory(e.target.value as TaskCategory)}
                      className="w-full bg-slate-900 border border-slate-800 rounded-xl px-3 py-2.5 text-sm text-slate-200 focus:outline-none focus:border-cyan-500"
                    >
                      <option value="work">کاری (Work)</option>
                      <option value="personal">شخصی (Personal)</option>
                      <option value="finance">مالی (Finance)</option>
                      <option value="health">سلامت (Health)</option>
                      <option value="study">مطالعه (Study)</option>
                    </select>
                  </div>
                  <div>
                    <label className="text-xs text-slate-400 block mb-1">اولیت</label>
                    <select
                      value={newPriority}
                      onChange={e => setNewPriority(e.target.value as TaskPriority)}
                      className="w-full bg-slate-900 border border-slate-800 rounded-xl px-3 py-2.5 text-sm text-slate-200 focus:outline-none focus:border-cyan-500"
                    >
                      <option value="low">کم</option>
                      <option value="medium">متوسط</option>
                      <option value="high">مهم</option>
                      <option value="urgent">فوری 🚨</option>
                    </select>
                  </div>
                </div>
                <div>
                  <label className="text-xs text-slate-400 block mb-1">سررسید (جلالی)</label>
                  <input 
                    type="text"
                    value={newDueDate}
                    onChange={e => setNewDueDate(e.target.value)}
                    className="w-full bg-slate-900 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-slate-200 focus:outline-none focus:border-cyan-500"
                  />
                </div>
                <button
                  type="submit"
                  className="w-full py-3 rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 text-white font-medium text-sm shadow-lg shadow-cyan-500/20 hover:opacity-90 transition"
                >
                  ثبت وظیفه جدید
                </button>
              </form>
            </div>
          </div>

          {/* Right 2 Columns: Task List & Search */}
          <div className="lg:col-span-2 space-y-4">
            {/* Search & Category Filter */}
            <div className="glass-card rounded-2xl p-4 flex flex-col sm:flex-row items-center gap-3">
              <div className="relative flex-1 w-full">
                <Search className="w-4 h-4 text-slate-400 absolute right-3.5 top-3.5" />
                <input 
                  type="text"
                  value={searchQuery}
                  onChange={e => setSearchQuery(e.target.value)}
                  placeholder="جستجو در وظایف (پشتیبانی از نرمال‌سازی فارسی)..."
                  className="w-full bg-slate-900/80 border border-slate-800 rounded-xl pr-10 pl-4 py-2.5 text-sm text-slate-200 focus:outline-none focus:border-cyan-500"
                />
              </div>
              <div className="flex items-center gap-2 w-full sm:w-auto overflow-x-auto pb-1 sm:pb-0">
                {['all', 'work', 'personal', 'finance', 'health', 'study'].map(cat => (
                  <button
                    key={cat}
                    onClick={() => setSelectedCategory(cat)}
                    className={`px-3 py-2 rounded-xl text-xs font-medium whitespace-nowrap transition ${
                      selectedCategory === cat ? 'bg-cyan-500 text-slate-950 font-bold' : 'bg-slate-900 text-slate-400 hover:text-white'
                    }`}
                  >
                    {cat === 'all' ? 'همه' : cat}
                  </button>
                ))}
              </div>
            </div>

            {/* Task Cards */}
            <div className="space-y-3">
              {filteredTasks.length === 0 ? (
                <div className="glass-card rounded-2xl p-12 text-center text-slate-500">
                  <CheckSquare className="w-12 h-12 mx-auto mb-3 opacity-30 text-cyan-400" />
                  <p className="text-sm">هیچ وظیفه‌ای با این مشخصات یافت نشد.</p>
                </div>
              ) : (
                filteredTasks.map(task => (
                  <div
                    key={task.id}
                    className={`glass-card rounded-2xl p-4 flex items-center justify-between transition ${
                      task.completed ? 'opacity-60 bg-slate-950/40' : 'hover:border-cyan-500/40'
                    }`}
                  >
                    <div className="flex items-center space-x-3 space-x-reverse">
                      <button
                        onClick={() => {
                          onToggleTask(task.id);
                          if (!task.completed) {
                            confetti({ particleCount: 30, spread: 50, origin: { y: 0.7 } });
                          }
                        }}
                        className="text-cyan-400 hover:scale-110 transition"
                      >
                        {task.completed ? <CheckCircle2 className="w-6 h-6 text-cyan-400 fill-cyan-500/20" /> : <Circle className="w-6 h-6 text-slate-600" />}
                      </button>
                      <div>
                        <h4 className={`text-sm font-semibold ${task.completed ? 'line-through text-slate-500' : 'text-slate-100'}`}>
                          {task.title}
                        </h4>
                        <div className="flex items-center space-x-3 space-x-reverse text-xs text-slate-400 mt-1">
                          <span className="px-2 py-0.5 rounded bg-slate-800 text-[10px] text-cyan-400">{task.category}</span>
                          <span>سررسید: {task.dueDate}</span>
                        </div>
                      </div>
                    </div>

                    <div className="flex items-center space-x-2 space-x-reverse">
                      <span className={`text-[10px] px-2.5 py-1 rounded-full font-medium ${
                        task.priority === 'urgent' ? 'bg-rose-500/20 text-rose-400' :
                        task.priority === 'high' ? 'bg-amber-500/20 text-amber-400' : 'bg-slate-800 text-slate-300'
                      }`}>
                        {task.priority}
                      </span>
                      <button
                        onClick={() => onDeleteTask(task.id)}
                        className="p-2 rounded-lg text-slate-500 hover:text-rose-400 hover:bg-rose-500/10 transition"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      ) : (
        /* Habits Tracker Tab */
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="glass-card rounded-2xl p-6 space-y-4">
            <h3 className="font-bold text-slate-100 text-base flex items-center space-x-2 space-x-reverse">
              <Flame className="w-5 h-5 text-rose-400" />
              <span>افزودن عادت جدید</span>
            </h3>
            <form onSubmit={handleCreateHabit} className="space-y-4">
              <div>
                <label className="text-xs text-slate-400 block mb-1">عنوان عادت روزانه</label>
                <input 
                  type="text"
                  value={newHabitTitle}
                  onChange={e => setNewHabitTitle(e.target.value)}
                  placeholder="مثلا: مطالعه ۳۰ دقیقه کتاب..."
                  className="w-full bg-slate-900 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-slate-200 focus:outline-none focus:border-cyan-500"
                />
              </div>
              <button
                type="submit"
                className="w-full py-3 rounded-xl bg-gradient-to-r from-rose-500 to-amber-600 text-white font-medium text-sm shadow-lg shadow-rose-500/20 hover:opacity-90 transition"
              >
                ثبت عادت
              </button>
            </form>
          </div>

          <div className="lg:col-span-2 space-y-3">
            {habits.map(habit => (
              <div 
                key={habit.id}
                className="glass-card rounded-2xl p-4 flex items-center justify-between"
              >
                <div className="flex items-center space-x-3 space-x-reverse">
                  <div className="w-10 h-10 rounded-xl bg-rose-500/10 text-rose-400 flex items-center justify-center font-bold">
                    🔥 {habit.streak}
                  </div>
                  <div>
                    <h4 className="text-sm font-bold text-slate-100">{habit.title}</h4>
                    <p className="text-xs text-slate-400">زنجیره موفقیت پیوسته</p>
                  </div>
                </div>

                <button
                  onClick={() => {
                    onToggleHabit(habit.id);
                    confetti({ particleCount: 25, spread: 45, origin: { y: 0.7 } });
                  }}
                  className={`px-4 py-2 rounded-xl text-xs font-bold transition ${
                    habit.completedToday 
                      ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/40' 
                      : 'bg-slate-800 text-slate-300 hover:bg-slate-700'
                  }`}
                >
                  {habit.completedToday ? 'انجام شد ✓' : 'تکمیل امروز'}
                </button>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};
