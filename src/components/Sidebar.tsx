import React from 'react';
import { 
  LayoutDashboard, 
  CheckSquare, 
  Wallet, 
  HeartPulse, 
  ShieldCheck, 
  Bot, 
  Settings as SettingsIcon,
  Sparkles,
  Menu,
  X
} from 'lucide-react';
import { TabType } from '../types';

interface SidebarProps {
  currentTab: TabType;
  onSelectTab: (tab: TabType) => void;
  isRtl: boolean;
  onToggleRtl: () => void;
  isOpenMobile: boolean;
  onToggleMobile: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({
  currentTab,
  onSelectTab,
  isRtl,
  onToggleRtl,
  isOpenMobile,
  onToggleMobile
}) => {
  const navItems: { id: TabType; label: string; icon: React.ReactNode; badge?: string }[] = [
    { id: 'dashboard', label: 'داشبورد اصلی', icon: <LayoutDashboard className="w-5 h-5" /> },
    { id: 'tasks', label: 'وظایف و عادت‌ها', icon: <CheckSquare className="w-5 h-5" />, badge: '۳ فعال' },
    { id: 'expenses', label: 'هزینه‌ها و پیامک بانک', icon: <Wallet className="w-5 h-5" /> },
    { id: 'health', label: 'سلامت و خواب', icon: <HeartPulse className="w-5 h-5" /> },
    { id: 'vault', label: 'صندوق امن (Vault)', icon: <ShieldCheck className="w-5 h-5" /> },
    { id: 'assistant', label: 'دستیار هوشمند جمینای', icon: <Bot className="w-5 h-5" />, badge: 'AI' },
    { id: 'settings', label: 'تنظیمات و پشتیبان‌گیری', icon: <SettingsIcon className="w-5 h-5" /> },
  ];

  return (
    <>
      {/* Mobile Top Header */}
      <div className="lg:hidden flex items-center justify-between bg-slate-900/80 backdrop-blur-md border-b border-slate-800 px-4 py-3 sticky top-0 z-45">
        <div className="flex items-center space-x-3 space-x-reverse">
          <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-cyan-500 to-blue-600 flex items-center justify-center shadow-lg shadow-cyan-500/20">
            <Sparkles className="w-5 h-5 text-white" />
          </div>
          <div>
            <h1 className="font-bold text-slate-100 text-sm">Personal Life OS</h1>
            <p className="text-xs text-cyan-400">سیستم عامل جامع زندگی</p>
          </div>
        </div>
        <button 
          onClick={onToggleMobile}
          className="p-2 rounded-lg bg-slate-800/80 text-slate-300 hover:text-white"
        >
          {isOpenMobile ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
        </button>
      </div>

      {/* Backdrop for Mobile */}
      {isOpenMobile && (
        <div 
          onClick={onToggleMobile} 
          className="lg:hidden fixed inset-0 bg-black/70 backdrop-blur-sm z-40"
        />
      )}

      {/* Sidebar Container */}
      <aside className={`
        fixed lg:static top-0 right-0 z-50 h-full w-72 bg-slate-900/95 backdrop-blur-xl border-l border-slate-800/80
        flex flex-col transition-transform duration-300 ease-in-out
        ${isOpenMobile ? 'translate-x-0' : 'translate-x-full lg:translate-x-0'}
      `}>
        {/* Logo / Brand Header */}
        <div className="p-6 border-b border-slate-800/60 hidden lg:flex items-center space-x-3 space-x-reverse">
          <div className="w-10 h-10 rounded-2xl bg-gradient-to-tr from-cyan-500 to-blue-600 flex items-center justify-center shadow-lg shadow-cyan-500/30">
            <Sparkles className="w-6 h-6 text-white animate-pulse" />
          </div>
          <div>
            <h1 className="font-bold text-slate-100 text-base tracking-tight">Personal Life OS</h1>
            <p className="text-xs text-cyan-400 font-medium">نسخه وب پیشرفته فارسی</p>
          </div>
        </div>

        {/* Navigation Items */}
        <nav className="flex-1 px-4 py-6 space-y-2 overflow-y-auto">
          {navItems.map((item) => {
            const isActive = currentTab === item.id;
            return (
              <button
                key={item.id}
                onClick={() => {
                  onSelectTab(item.id);
                  if (isOpenMobile) onToggleMobile();
                }}
                className={`
                  w-full flex items-center justify-between px-4 py-3 rounded-xl font-medium text-sm transition-all duration-200
                  ${isActive 
                    ? 'bg-gradient-to-r from-cyan-500/20 to-blue-600/20 text-cyan-400 border border-cyan-500/30 shadow-lg shadow-cyan-500/10' 
                    : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/50 border border-transparent'}
                `}
              >
                <div className="flex items-center space-x-3 space-x-reverse">
                  <span className={isActive ? 'text-cyan-400' : 'text-slate-400'}>
                    {item.icon}
                  </span>
                  <span>{item.label}</span>
                </div>
                {item.badge && (
                  <span className={`text-[10px] px-2 py-0.5 rounded-full font-semibold ${
                    isActive ? 'bg-cyan-500 text-slate-950' : 'bg-slate-800 text-slate-300'
                  }`}>
                    {item.badge}
                  </span>
                )}
              </button>
            );
          })}
        </nav>

        {/* Bottom User Card / RTL Toggle */}
        <div className="p-4 border-t border-slate-800/60 bg-slate-950/40">
          <div className="glass-card rounded-2xl p-4 flex items-center justify-between">
            <div className="flex items-center space-x-3 space-x-reverse">
              <div className="w-9 h-9 rounded-full bg-cyan-500/20 text-cyan-400 font-bold flex items-center justify-center border border-cyan-500/30">
                سی
              </div>
              <div>
                <p className="text-xs font-bold text-slate-200">کاربر گرامی</p>
                <p className="text-[10px] text-cyan-400">حساب فعال (امنیتی)</p>
              </div>
            </div>
            <button
              onClick={onToggleRtl}
              title="تغییر جهت زبان / Direction"
              className="px-2.5 py-1 rounded-lg bg-slate-800 text-xs text-slate-300 hover:text-cyan-400 transition"
            >
              {isRtl ? 'EN' : 'فا'}
            </button>
          </div>
        </div>
      </aside>
    </>
  );
};
