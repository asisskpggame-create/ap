import React from 'react';
import {
  WalletCards,
  CheckSquare,
  LayoutDashboard,
  Bot
} from 'lucide-react';
import { AppSection } from '../../types/lifeOs';
import { useLifeOs } from '../../context/LifeOsContext';

export const GlassBottomBar: React.FC = () => {
  const { activeSection, setActiveSection, tasks } = useLifeOs();

  const pendingTasksCount = tasks.filter(t => t.status !== 'COMPLETED' && t.status !== 'ARCHIVED').length;

  const navItems: { id: AppSection; label: string; icon: React.ReactNode; badge?: number }[] = [
    {
      id: 'DAILY_LOG',
      label: 'مالی',
      icon: <WalletCards className="w-5 h-5" />
    },
    {
      id: 'GOALS_AND_HABITS',
      label: 'اهداف و مسیر',
      icon: <CheckSquare className="w-5 h-5" />,
      badge: pendingTasksCount > 0 ? pendingTasksCount : undefined
    },
    {
      id: 'HOME',
      label: 'داشبورد',
      icon: <LayoutDashboard className="w-5 h-5" />
    },
    {
      id: 'AI_ASSISTANT',
      label: 'دستیار AI',
      icon: <Bot className="w-5 h-5" />
    }
  ];

  return (
    <nav className="fixed bottom-0 left-0 right-0 z-40 p-2 sm:p-3 pointer-events-none flex justify-center">
      <div className="pointer-events-auto max-w-md w-full glass-panel rounded-2xl p-1.5 shadow-2xl shadow-black/60 border border-white/10 flex items-center justify-around gap-1 backdrop-blur-2xl">
        {navItems.map(item => {
          const isActive = activeSection === item.id;
          return (
            <button
              key={item.id}
              onClick={() => setActiveSection(item.id)}
              className={`relative flex-1 py-2 px-1 rounded-xl flex flex-col items-center justify-center gap-1 transition-all duration-200 cursor-pointer ${
                isActive
                  ? 'bg-teal-500/15 text-teal-300 font-semibold shadow-inner shadow-teal-500/20 border border-teal-500/30'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-white/5'
              }`}
            >
              <div className="relative">
                {item.icon}
                {item.badge !== undefined && (
                  <span className="absolute -top-1.5 -right-2 min-w-4 h-4 px-1 rounded-full bg-teal-500 text-[10px] text-slate-950 font-bold flex items-center justify-center">
                    {item.badge}
                  </span>
                )}
              </div>
              <span className="text-[11px] leading-tight select-none">
                {item.label}
              </span>
              {isActive && (
                <div className="absolute -bottom-1 w-6 h-0.5 rounded-full bg-teal-400 shadow-[0_0_8px_#2dd4bf]" />
              )}
            </button>
          );
        })}
      </div>
    </nav>
  );
};
