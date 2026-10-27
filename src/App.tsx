import React, { useState, useEffect } from 'react';
import { Sidebar } from './components/Sidebar';
import { DashboardScreen } from './components/DashboardScreen';
import { TasksScreen } from './components/TasksScreen';
import { ExpensesScreen } from './components/ExpensesScreen';
import { HealthScreen } from './components/HealthScreen';
import { VaultScreen } from './components/VaultScreen';
import { AssistantScreen } from './components/AssistantScreen';
import { SettingsScreen } from './components/SettingsScreen';
import { VoiceCaptureModal } from './components/VoiceCaptureModal';
import {
  TabType,
  TaskItem,
  HabitItem,
  ExpenseItem,
  HealthLog,
  VaultNote,
  ChatMessage,
  BankProfile,
} from './types';
import { loadBankProfiles, saveBankProfiles } from './utils/persianUtils';

export default function App() {
  const [currentTab, setCurrentTab] = useState<TabType>('dashboard');
  const [isRtl, setIsRtl] = useState(true);
  const [isOpenMobile, setIsOpenMobile] = useState(false);
  const [isVoiceModalOpen, setIsVoiceModalOpen] = useState(false);

  // Persistent Bank Profiles
  const [bankProfiles, setBankProfiles] = useState<BankProfile[]>(() =>
    loadBankProfiles()
  );

  // Persistent State with localStorage
  const [tasks, setTasks] = useState<TaskItem[]>(() => {
    const saved = localStorage.getItem('life_os_tasks');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch {
        /* ignore */
      }
    }
    return [
      {
        id: '1',
        title: 'بررسی و تایید گزارش مالی شرکت',
        category: 'work',
        priority: 'urgent',
        completed: false,
        dueDate: '۱۴۰۵/۰۸/۰۶',
        createdAt: '1405/08/01',
      },
      {
        id: '2',
        title: 'ورزش هوازی و بدن‌سازی (۴۵ دقیقه)',
        category: 'health',
        priority: 'high',
        completed: true,
        dueDate: '۱۴۰۵/۰۸/۰۵',
        createdAt: '1405/08/01',
      },
      {
        id: '3',
        title: 'مطالعه کتاب معماری نرم‌افزار',
        category: 'study',
        priority: 'medium',
        completed: false,
        dueDate: '۱۴۰۵/۰۸/۰۸',
        createdAt: '1405/08/02',
      },
      {
        id: '4',
        title: 'پرداخت اقساط وام و بررسی حساب‌ها',
        category: 'finance',
        priority: 'urgent',
        completed: false,
        dueDate: '۱۴۰۵/۰۸/۰۷',
        createdAt: '1405/08/03',
      },
    ];
  });

  const [habits, setHabits] = useState<HabitItem[]>(() => {
    const saved = localStorage.getItem('life_os_habits');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch {
        /* ignore */
      }
    }
    return [
      {
        id: 'h1',
        title: 'مطالعه ۳۰ دقیقه روزانه',
        streak: 7,
        completedToday: true,
        category: 'study',
      },
      {
        id: 'h2',
        title: 'ورزش و نرمش صبحگاهی',
        streak: 5,
        completedToday: false,
        category: 'health',
      },
      {
        id: 'h3',
        title: 'نوشیدن ۸ لیوان آب',
        streak: 12,
        completedToday: true,
        category: 'health',
      },
    ];
  });

  const [expenses, setExpenses] = useState<ExpenseItem[]>(() => {
    const saved = localStorage.getItem('life_os_expenses');
    if (saved) {
      try {
        const parsed = JSON.parse(saved);
        if (Array.isArray(parsed)) {
          return parsed.filter(
            (item) =>
              item &&
              typeof item.title === 'string' &&
              typeof item.amount === 'number' &&
              Number.isFinite(item.amount)
          );
        }
      } catch {
        /* ignore */
      }
    }
    return [
      {
        id: 'e1',
        title: 'خرید از فروشگاه رفاه',
        amount: 1250000,
        category: 'خرید عمومی',
        date: '۱۴۰۵/۰۸/۰۵',
        type: 'expense',
        isSmsParsed: true,
        bankId: 'mellat',
        bankName: 'بانک ملت',
      },
      {
        id: 'e2',
        title: 'کافه و میان‌وعده کاری',
        amount: 350000,
        category: 'غذا و رستوران',
        date: '۱۴۰۵/۰۸/۰۴',
        type: 'expense',
        isSmsParsed: false,
      },
      {
        id: 'e3',
        title: 'شارژ کارت مترو و تاکسی',
        amount: 180000,
        category: 'حمل و نقل',
        date: '۱۴۰۵/۰۸/۰۳',
        type: 'expense',
        isSmsParsed: true,
        bankId: 'saman',
        bankName: 'بانک سامان',
      },
    ];
  });

  const [healthLog, setHealthLog] = useState<HealthLog>(() => {
    const saved = localStorage.getItem('life_os_health');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch {
        /* ignore */
      }
    }
    return {
      id: 'hl1',
      date: '۱۴۰۵/۰۸/۰۵',
      sleepHours: 7.5,
      sleepQuality: 'good',
      waterGlasses: 6,
      exerciseMinutes: 45,
      exerciseType: 'بدن‌سازی و هوازی',
      caloriesBurned: 480,
    };
  });

  const [vaultNotes, setVaultNotes] = useState<VaultNote[]>(() => {
    const saved = localStorage.getItem('life_os_vault');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch {
        /* ignore */
      }
    }
    return [
      {
        id: 'v1',
        title: 'رمز کیف پول سخت‌افزاری',
        content: 'Seed: apple banana cherry ... secret pin: 9942',
        category: 'حساب‌های بانکی',
        updatedAt: '۱۴۰۵/۰۸/۰۱',
        isEncrypted: true,
      },
      {
        id: 'v2',
        title: 'یادداشت محرمانه بورسی',
        content: 'رمز عبور کارگزاری مفید و حساب‌های صرافی',
        category: 'رمزهای عبور',
        updatedAt: '۱۴۰۵/۰۸/۰۳',
        isEncrypted: true,
      },
    ];
  });

  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      id: 'm1',
      sender: 'gemini',
      text: 'سلام! من دستیار هوشمند جمینای شما در Personal Life OS هستم. تمام داده‌ها و پروفایل‌های بانکی شما به صورت خودکار و امن در حافظه محلی ذخیره می‌شوند. چطور می‌توانم کمک کنم؟',
      timestamp: '۱۰:۰۰',
    },
  ]);

  // Save to localStorage whenever state changes
  useEffect(() => {
    saveBankProfiles(bankProfiles);
  }, [bankProfiles]);

  useEffect(() => {
    localStorage.setItem('life_os_tasks', JSON.stringify(tasks));
  }, [tasks]);

  useEffect(() => {
    localStorage.setItem('life_os_habits', JSON.stringify(habits));
  }, [habits]);

  useEffect(() => {
    localStorage.setItem('life_os_expenses', JSON.stringify(expenses));
  }, [expenses]);

  useEffect(() => {
    localStorage.setItem('life_os_health', JSON.stringify(healthLog));
  }, [healthLog]);

  useEffect(() => {
    localStorage.setItem('life_os_vault', JSON.stringify(vaultNotes));
  }, [vaultNotes]);

  // Handlers for Tasks
  const handleAddTask = (task: Omit<TaskItem, 'id' | 'createdAt'>) => {
    const newItem: TaskItem = {
      ...task,
      id: Date.now().toString(),
      createdAt: '۱۴۰۵/۰۸/۰۵',
    };
    setTasks((prev) => [newItem, ...prev]);
  };

  const handleToggleTask = (id: string) => {
    setTasks((prev) =>
      prev.map((t) => (t.id === id ? { ...t, completed: !t.completed } : t))
    );
  };

  const handleDeleteTask = (id: string) => {
    setTasks((prev) => prev.filter((t) => t.id !== id));
  };

  // Handlers for Habits
  const handleToggleHabit = (id: string) => {
    setHabits((prev) =>
      prev.map((h) =>
        h.id === id
          ? {
              ...h,
              completedToday: !h.completedToday,
              streak: !h.completedToday
                ? h.streak + 1
                : Math.max(0, h.streak - 1),
            }
          : h
      )
    );
  };

  const handleAddHabit = (title: string) => {
    const newHabit: HabitItem = {
      id: Date.now().toString(),
      title,
      streak: 1,
      completedToday: false,
      category: 'health',
    };
    setHabits((prev) => [newHabit, ...prev]);
  };

  // Handlers for Expenses (with idempotency check)
  const handleAddExpense = (expense: Omit<ExpenseItem, 'id'>) => {
    if (!expense || typeof expense.amount !== 'number' || expense.amount <= 0) {
      return;
    }
    setExpenses((prev) => {
      if (
        expense.messageFingerprint &&
        prev.some((e) => e.messageFingerprint === expense.messageFingerprint)
      ) {
        return prev;
      }
      const newItem: ExpenseItem = {
        ...expense,
        id: Date.now().toString() + '_' + Math.random().toString(36).slice(2, 5),
      };
      return [newItem, ...prev];
    });
  };

  const handleDeleteExpense = (id: string) => {
    setExpenses((prev) => prev.filter((e) => e.id !== id));
  };

  // Handlers for Health
  const handleUpdateHealth = (log: Partial<HealthLog>) => {
    setHealthLog((prev) => ({ ...prev, ...log }));
  };

  // Handlers for Vault
  const handleAddVaultNote = (note: Omit<VaultNote, 'id' | 'updatedAt'>) => {
    const newNote: VaultNote = {
      ...note,
      id: Date.now().toString(),
      updatedAt: '۱۴۰۵/۰۸/۰۵',
    };
    setVaultNotes((prev) => [newNote, ...prev]);
  };

  const handleDeleteVaultNote = (id: string) => {
    setVaultNotes((prev) => prev.filter((n) => n.id !== id));
  };

  // Handlers for Assistant Chat
  const handleSendMessage = (userText: string, aiText: string) => {
    const userMsg: ChatMessage = {
      id: Date.now().toString(),
      sender: 'user',
      text: userText,
      timestamp: new Date().toLocaleTimeString('fa-IR', {
        hour: '2-digit',
        minute: '2-digit',
      }),
    };
    const aiMsg: ChatMessage = {
      id: (Date.now() + 1).toString(),
      sender: 'gemini',
      text: aiText,
      timestamp: new Date().toLocaleTimeString('fa-IR', {
        hour: '2-digit',
        minute: '2-digit',
      }),
    };
    setMessages((prev) => [...prev, userMsg, aiMsg]);
  };

  // Export Data
  const handleExportData = () => {
    const data = {
      tasks,
      habits,
      expenses,
      healthLog,
      vaultNotes,
      bankProfiles,
    };
    const blob = new Blob([JSON.stringify(data, null, 2)], {
      type: 'application/json',
    });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'personal-life-os-backup.json';
    a.click();
  };

  const handleResetData = () => {
    localStorage.clear();
    window.location.reload();
  };

  return (
    <div
      className={`min-h-screen bg-slate-950 text-slate-100 flex flex-col lg:flex-row ${
        isRtl ? 'rtl' : 'ltr'
      }`}
      dir={isRtl ? 'rtl' : 'ltr'}
    >
      {/* Sidebar Navigation */}
      <Sidebar
        currentTab={currentTab}
        onSelectTab={setCurrentTab}
        isRtl={isRtl}
        onToggleRtl={() => setIsRtl(!isRtl)}
        isOpenMobile={isOpenMobile}
        onToggleMobile={() => setIsOpenMobile(!isOpenMobile)}
      />

      {/* Main Content Area */}
      <main className="flex-1 p-4 lg:p-8 overflow-y-auto max-w-7xl mx-auto w-full">
        {currentTab === 'dashboard' && (
          <DashboardScreen
            tasks={tasks}
            habits={habits}
            expenses={expenses}
            healthLog={healthLog}
            onOpenVoiceModal={() => setIsVoiceModalOpen(true)}
            onNavigate={setCurrentTab}
            onToggleTask={handleToggleTask}
            onToggleHabit={handleToggleHabit}
          />
        )}

        {currentTab === 'tasks' && (
          <TasksScreen
            tasks={tasks}
            habits={habits}
            onAddTask={handleAddTask}
            onToggleTask={handleToggleTask}
            onDeleteTask={handleDeleteTask}
            onToggleHabit={handleToggleHabit}
            onAddHabit={handleAddHabit}
          />
        )}

        {currentTab === 'expenses' && (
          <ExpensesScreen
            expenses={expenses}
            onAddExpense={handleAddExpense}
            onDeleteExpense={handleDeleteExpense}
            bankProfiles={bankProfiles}
            onUpdateBankProfiles={setBankProfiles}
          />
        )}

        {currentTab === 'health' && (
          <HealthScreen
            healthLog={healthLog}
            onUpdateHealth={handleUpdateHealth}
          />
        )}

        {currentTab === 'vault' && (
          <VaultScreen
            vaultNotes={vaultNotes}
            onAddNote={handleAddVaultNote}
            onDeleteNote={handleDeleteVaultNote}
          />
        )}

        {currentTab === 'assistant' && (
          <AssistantScreen
            messages={messages}
            onSendMessage={handleSendMessage}
          />
        )}

        {currentTab === 'settings' && (
          <SettingsScreen
            onExportData={handleExportData}
            onResetData={handleResetData}
          />
        )}
      </main>

      {/* Quick Voice / SMS Capture Modal */}
      <VoiceCaptureModal
        isOpen={isVoiceModalOpen}
        onClose={() => setIsVoiceModalOpen(false)}
        bankProfiles={bankProfiles}
        existingExpenses={expenses}
        onAddExpense={handleAddExpense}
        onAddQuickTask={(taskTitle) => {
          handleAddTask({
            title: taskTitle,
            category: 'personal',
            priority: 'medium',
            completed: false,
            dueDate: '۱۴۰۵/۰۸/۱۰',
          });
        }}
      />
    </div>
  );
}
