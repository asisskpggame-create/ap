import React, { useState } from 'react';
import { LifeOsProvider, useLifeOs } from './context/LifeOsContext';
import { AppHeader } from './components/layout/AppHeader';
import { GlassBottomBar } from './components/layout/GlassBottomBar';
import { FloatingFocusTimer } from './components/layout/FloatingFocusTimer';
import { HomeDashboardScreen } from './components/dashboard/HomeDashboardScreen';
import { DailyLogScreen } from './components/finance/DailyLogScreen';
import { GoalsAndHabitsScreen } from './components/goals/GoalsAndHabitsScreen';
import { AssistantScreen } from './components/assistant/AssistantScreen';

// Modals
import { AddExpenseModal } from './components/finance/AddExpenseModal';
import { AddTaskModal } from './components/goals/AddTaskModal';
import { BankSmsScannerModal } from './components/finance/BankSmsScannerModal';
import { VoiceCaptureModal } from './components/assistant/VoiceCaptureModal';
import { SettingsModal } from './components/settings/SettingsModal';
import { VaultLockModal } from './components/common/VaultLockModal';

const AppContent: React.FC = () => {
  const { activeSection, preferences } = useLifeOs();

  const [isAddExpenseOpen, setIsAddExpenseOpen] = useState(false);
  const [isAddTaskOpen, setIsAddTaskOpen] = useState(false);
  const [isSmsScannerOpen, setIsSmsScannerOpen] = useState(false);
  const [isVoiceOpen, setIsVoiceOpen] = useState(false);
  const [isSettingsOpen, setIsSettingsOpen] = useState(false);

  const handleQuickAdd = () => {
    if (activeSection === 'GOALS_AND_HABITS') {
      setIsAddTaskOpen(true);
    } else {
      setIsAddExpenseOpen(true);
    }
  };

  return (
    <div className={`min-h-screen text-slate-100 flex flex-col bg-[#0b0f17] font-sans antialiased selection:bg-teal-500/30 selection:text-teal-200 theme-${preferences.themePalette.toLowerCase()}`}>
      {/* App Header */}
      <AppHeader
        onOpenSettings={() => setIsSettingsOpen(true)}
        onQuickAdd={handleQuickAdd}
        onOpenVoice={() => setIsVoiceOpen(true)}
      />

      {/* Main Content Area */}
      <main className="flex-1 w-full max-w-7xl mx-auto px-4 sm:px-6 pt-5">
        {activeSection === 'HOME' && (
          <HomeDashboardScreen
            onOpenAddExpense={() => setIsAddExpenseOpen(true)}
            onOpenAddTask={() => setIsAddTaskOpen(true)}
            onOpenSmsScanner={() => setIsSmsScannerOpen(true)}
            onOpenVoice={() => setIsVoiceOpen(true)}
          />
        )}

        {activeSection === 'DAILY_LOG' && (
          <DailyLogScreen
            onOpenAddExpense={() => setIsAddExpenseOpen(true)}
            onOpenSmsScanner={() => setIsSmsScannerOpen(true)}
          />
        )}

        {activeSection === 'GOALS_AND_HABITS' && (
          <GoalsAndHabitsScreen
            onOpenAddTask={() => setIsAddTaskOpen(true)}
          />
        )}

        {activeSection === 'AI_ASSISTANT' && (
          <AssistantScreen />
        )}
      </main>

      {/* Floating Pomodoro Focus Timer */}
      <FloatingFocusTimer />

      {/* Glass Bottom Navigation Bar */}
      <GlassBottomBar />

      {/* Modals & Dialogs */}
      <AddExpenseModal
        isOpen={isAddExpenseOpen}
        onClose={() => setIsAddExpenseOpen(false)}
      />

      <AddTaskModal
        isOpen={isAddTaskOpen}
        onClose={() => setIsAddTaskOpen(false)}
      />

      <BankSmsScannerModal
        isOpen={isSmsScannerOpen}
        onClose={() => setIsSmsScannerOpen(false)}
      />

      <VoiceCaptureModal
        isOpen={isVoiceOpen}
        onClose={() => setIsVoiceOpen(false)}
      />

      <SettingsModal
        isOpen={isSettingsOpen}
        onClose={() => setIsSettingsOpen(false)}
      />

      {/* Vault PIN Lock overlay */}
      <VaultLockModal />
    </div>
  );
};

export default function App() {
  return (
    <LifeOsProvider>
      <AppContent />
    </LifeOsProvider>
  );
}
