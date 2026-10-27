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
import { TabType, TaskItem, HabitItem, ExpenseItem, HealthLog, VaultNote, ChatMessage } from './types';
import { GoogleGenAI } from '@google/genai';

export default function App() {
  const [currentTab, setCurrentTab] = useState<TabType>('dashboard');
  const [isRtl, setIsRtl] = useState(true);
  const [isOpenMobile, setIsOpenMobile] = useState(false);
  const [isVoiceModalOpen, setIsVoiceModalOpen] = useState(false);

  // Persistent State with localStorage
  const [tasks, setTasks] = useState<TaskItem[]>(() => {
    const saved = localStorage.getItem('life_os_tasks');
    if (saved) {
      try { return JSON.parse(saved); } catch (e) { /* ignore */ }
    }
    return [
      { id: '1', title: 'بررسی و تایید گزارش مالی شرکت', category: 'work', priority: 'urgent', completed: false, dueDate: '۱۴۰۵/۰۸/۰۶', createdAt: '1405/08/01' },
      { id: '2', title: 'ورزش هوازی و بدن‌سازی (۴۵ دقیقه)', category: 'health', priority: 'high', completed: true, dueDate: '۱۴۰۵/۰۸/۰۵', createdAt: '1405/08/01' },
      { id: '3', title: 'مطالعه کتاب معماری نرم‌افزار', category: 'study', priority: 'medium', completed: false, dueDate: '۱۴۰۵/۰۸/۰۸', createdAt: '1405/08/02' },
      { id: '4', title: 'پرداخت اقساط وام و بررسی حساب‌ها', category: 'finance', priority: 'urgent', completed: false, dueDate: '۱۴۰۵/۰۸/۰۷', createdAt: '1405/08/03' },
    ];
  });

  const [habits, setHabits] = useState<HabitItem[]>(() => {
    const saved = localStorage.getItem('life_os_habits');
    if (saved) {
      try { return JSON.parse(saved); } catch (e) { /* ignore */ }
    }
    return [
      { id: 'h1', title: 'مطالعه ۳۰ دقیقه روزانه', streak: 7, completedToday: true, category: 'study' },
      { id: 'h2', title: 'ورزش و نرمش صبحگاهی', streak: 5, completedToday: false, category: 'health' },
      { id: 'h3', title: 'نوشیدن ۸ لیوان آب', streak: 12, completedToday: true, category: 'health' },
    ];
  });

  const [expenses, setExpenses] = useState<ExpenseItem[]>(() => {
    const saved = localStorage.getItem('life_os_expenses');
    if (saved) {
      try { return JSON.parse(saved); } catch (e) { /* ignore */ }
    }
    return [
      { id: 'e1', title: 'خرید از فروشگاه رفاه', amount: 1250000, category: 'خرید عمومی', date: '۱۴۰۵/۰۸/۰۵', isSmsParsed: true, bankName: 'بانک ملت' },
      { id: 'e2', title: 'کافه و میان‌وعده کاری', amount: 350000, category: 'غذا و رستوران', date: '۱۴۰۵/۰۸/۰۴', isSmsParsed: false },
      { id: 'e3', title: 'شارژ کارت مترو و تاکسی', amount: 180000, category: 'حمل و نقل', date: '۱۴۰۵/۰۸/۰۳', isSmsParsed: true, bankName: 'بانک سامان' },
    ];
  });

  const [healthLog, setHealthLog] = useState<HealthLog>(() => {
    const saved = localStorage.getItem('life_os_health');
    if (saved) {
      try { return JSON.parse(saved); } catch (e) { /* ignore */ }
    }
    return {
      id: 'hl1',
      date: '۱۴۰۵/۰۸/۰۵',
      sleepHours: 7.5,
      sleepQuality: 'good',
      waterGlasses: 6,
      exerciseMinutes: 45,
      exerciseType: 'بدن‌سازی و هوازی',
      caloriesBurned: 480
    };
  });

  const [vaultNotes, setVaultNotes] = useState<VaultNote[]>(() => {
    const saved = localStorage.getItem('life_os_vault');
    if (saved) {
      try { return JSON.parse(saved); } catch (e) { /* ignore */ }
    }
    return [
      { id: 'v1', title: 'رمز کیف پول سخت‌افزاری', content: 'Seed: apple banana cherry ... secret pin: 9942', category: 'حساب‌های بانکی', updatedAt: '۱۴۰۵/۰۸/۰۱', isEncrypted: true },
      { id: 'v2', title: 'یادداشت محرمانه بورسی', content: 'رمز عبور کارگزاری مفید و حساب‌های صرافی', category: 'رمزهای عبور', updatedAt: '۱۴۰۵/۰۸/۰۳', isEncrypted: true },
    ];
  });

  const [messages, setMessages] = useState<ChatMessage[]>([
    { id: 'm1', sender: 'gemini', text: 'سلام! من دستیار هوشمند جمینای شما در Personal Life OS هستم. تمام داده‌های شما به صورت خودکار و امن در حافظه محلی ذخیره می‌شوند. چطور می‌توانم کمک کنم؟', timestamp: '۱۰:۰۰' }
  ]);

  // Save to localStorage whenever state changes
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
      createdAt: '۱۴۰۵/۰۸/۰۵'
    };
    setTasks(prev => [newItem, ...prev]);
  };

  const handleToggleTask = (id: string) => {
    setTasks(prev => prev.map(t => t.id === id ? { ...t, completed: !t.completed } : t));
  };

  const handleDeleteTask = (id: string) => {
    setTasks(prev => prev.filter(t => t.id !== id));
  };

  // Handlers for Habits
  const handleToggleHabit = (id: string) => {
    setHabits(prev => prev.map(h => h.id === id ? {
      ...h,
      completedToday: !h.completedToday,
      streak: !h.completedToday ? h.streak + 1 : Math.max(0, h.streak - 1)
    } : h));
  };

  const handleAddHabit = (title: string) => {
    const newHabit: HabitItem = {
      id: Date.now().toString(),
      title,
      streak: 1,
      completedToday: false,
      category: 'health'
    };
    setHabits(prev => [newHabit, ...prev]);
  };

  // Handlers for Expenses
  const handleAddExpense = (expense: Omit<ExpenseItem, 'id'>) => {
    const newItem: ExpenseItem = {
      ...expense,
      id: Date.now().toString()
    };
    setExpenses(prev => [newItem, ...prev]);
  };

  const handleDeleteExpense = (id: string) => {
    setExpenses(prev => prev.filter(e => e.id !== id));
  };

  // Handlers for Health
  const handleUpdateHealth = (log: Partial<HealthLog>) => {
    setHealthLog(prev => ({ ...prev, ...log }));
  };

  // Handlers for Vault
  const handleAddVaultNote = (note: Omit<VaultNote, 'id' | 'updatedAt'>) => {
    const newNote: VaultNote = {
      ...note,
      id: Date.now().toString(),
      updatedAt: '۱۴۰۵/۰۸/۰۵'
    };
    setVaultNotes(prev => [newNote, ...prev]);
  };

  const handleDeleteVaultNote = (id: string) => {
    setVaultNotes(prev => prev.filter(n => n.id !== id));
  };

  // Handlers for Assistant Chat with Real Gemini AI Integration
  const handleSendMessage = async (userText: string) => {
    const userMsg: ChatMessage = {
      id: Date.now().toString(),
      sender: 'user',
      text: userText,
      timestamp: new Date().toLocaleTimeString('fa-IR', { hour: '2-digit', minute: '2-digit' })
    };

    setMessages(prev => [...prev, userMsg]);

    let aiReply = "متوجه شدم. در حال بررسی درخواست شما در Personal Life OS هستم.";
    try {
      const ai = new GoogleGenAI();
      const response = await ai.models.generateContent({
        model: 'gemini-2.5-flash',
        contents: [
          {
            role: 'user',
            parts: [{ text: `You are Gemini AI assistant in Personal Life OS, a Persian productivity and life management app. Respond in natural Persian (Farsi) with helpful advice regarding tasks, finances, health, and habits. Current context: Total active tasks: ${tasks.filter(t=>!t.completed).length}, Total expenses: ${expenses.reduce((s,e)=>s+e.amount,0)} Tomans. User prompt: ${userText}` }]
          }
        ]
      });
      if (response.text) {
        aiReply = response.text;
      }
    } catch (err) {
      console.error("Gemini API generation error:", err);
      if (userText.includes("هزینه") || userText.includes("خرج")) {
        aiReply = `مجموع کل هزینه‌های ثبت شده شما ${expenses.reduce((s,e)=>s+e.amount,0).toLocaleString('fa-IR')} تومان است. پیشنهاد می‌کنم بودجه‌بندی هفتگی خود را بررسی کنید.`;
      } else if (userText.includes("سلام") || userText.includes("درود")) {
        aiReply = "سلام! دستیار هوشمند جمینای آماده کمک به شماست.";
      }
    }

    const aiMsg: ChatMessage = {
      id: (Date.now() + 1).toString(),
      sender: 'gemini',
      text: aiReply,
      timestamp: new Date().toLocaleTimeString('fa-IR', { hour: '2-digit', minute: '2-digit' })
    };

    setMessages(prev => [...prev, aiMsg]);
  };

  // Export Data
  const handleExportData = () => {
    const data = { tasks, habits, expenses, healthLog, vaultNotes };
    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
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
    <div className={`min-h-screen bg-slate-950 text-slate-100 flex flex-col lg:flex-row ${isRtl ? 'rtl' : 'ltr'}`} dir={isRtl ? 'rtl' : 'ltr'}>
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
            onAddExpense={(title, amount, category, date, bankName, isSms) => {
              handleAddExpense({ title, amount, category, date, bankName, isSmsParsed: isSms });
            }}
            onDeleteExpense={handleDeleteExpense}
            onParseSms={(smsText) => {
              // Simple Bank SMS Parser
              if (smsText.includes('ریال') || smsText.includes('تومان') || smsText.includes('برداشت') || smsText.includes('واریز')) {
                let bankName = 'بانک ملت/سامان';
                if (smsText.includes('ملت')) bankName = 'بانک ملت';
                else if (smsText.includes('سامان')) bankName = 'بانک سامان';
                else if (smsText.includes('ملی')) bankName = 'بانک ملی';

                const match = smsText.match(/([\d,]+)\s*(ریال|تومان)/);
                if (match) {
                  let rawAmt = match[1].replace(/,/g, '').replace(/٬/g, '');
                  let amt = parseInt(rawAmt, 10);
                  if (match[2] === 'ریال') amt = Math.floor(amt / 10);
                  
                  handleAddExpense({
                    title: smsText.includes('خرید') ? 'خرید کارت بانکی' : 'تراکنش بانکی',
                    amount: amt,
                    category: 'تراکنش بانکی',
                    date: '۱۴۰۵/۰۸/۰۵',
                    isSmsParsed: true,
                    bankName
                  });
                  return true;
                }
              }
              return false;
            }}
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
            onAddNote={(title, content, category, isEncrypted) => {
              handleAddVaultNote({ title, content, category, isEncrypted });
            }}
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
            isRtl={isRtl}
            onToggleRtl={() => setIsRtl(!isRtl)}
          />
        )}
      </main>

      {/* Quick Voice / SMS Capture Modal */}
      <VoiceCaptureModal
        isOpen={isVoiceModalOpen}
        onDismiss={() => setIsVoiceModalOpen(false)}
        onParseSms={(smsText) => {
          if (smsText.includes('ریال') || smsText.includes('تومان')) {
            let bankName = 'بانک ملت';
            if (smsText.includes('سامان')) bankName = 'بانک سامان';
            const match = smsText.match(/([\d,]+)\s*(ریال|تومان)/);
            if (match) {
              let rawAmt = match[1].replace(/,/g, '').replace(/٬/g, '');
              let amt = parseInt(rawAmt, 10);
              if (match[2] === 'ریال') amt = Math.floor(amt / 10);
              handleAddExpense({
                title: 'خرید از طریق پیامک',
                amount: amt,
                category: 'خرید بانکی',
                date: '۱۴۰۵/۰۸/۰۵',
                isSmsParsed: true,
                bankName
              });
              return true;
            }
          }
          return false;
        }}
        onAddQuickTask={(taskTitle) => {
          handleAddTask({
            title: taskTitle,
            category: 'personal',
            priority: 'medium',
            completed: false,
            dueDate: '۱۴۰۵/۰۸/۱۰'
          });
        }}
      />
    </div>
  );
}
