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
  X,
  BellRing,
} from 'lucide-react';
import { TabType } from '../types';

interface SidebarProps {
  currentTab: TabType;
  onSelectTab: (tab: TabType) => void;
  isRtl: boolean;
  onToggleRtl: () => void;
  isOpenMobile: boolean;
  onToggleMobile: () => void;
  unreadNotificationsCount?: number;
  pendingReceiptNotificationsCount?: number;
  onOpenNotificationCenter?: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({
  currentTab,
  onSelectTab,
  isRtl,
  onToggleRtl,
  isOpenMobile,
  onToggleMobile,
  unreadNotificationsCount = 0,
  pendingReceiptNotificationsCount = 0,
  onOpenNotificationCenter,
}) => {
  const navItems: {
    id: TabType;
    label: string;
    icon: React.ReactNode;
    badge?: string;
  }[] = [
    {
      id: 'dashboard',
      label: 'داشبورد اصلی',
      icon: <LayoutDashboard className="w-5 h-5" />,
    },
    {
      id: 'tasks',
      label: 'وظایف و عادت‌ها',
      icon: <CheckSquare className="w-5 h-5" />,
      badge: '۳ فعال',
    },
    {
      id: 'expenses',
      label: 'هزینه‌ها و پیامک بانک',
      icon: <Wallet className="w-5 h-5" />,
      badge:
        pendingReceiptNotificationsCount > 0
          ? `${pendingReceiptNotificationsCount} فیش`
          : undefined,
    },
    {
      id: 'health',
      label: 'سلامت و خواب',
      icon: <HeartPulse className="w-5 h-5" />,
    },
    {
      id: 'vault',
      label: 'صندوق امن (Vault)',
      icon: <ShieldCheck className="w-5 h-5" />,
    },
    {
      id: 'assistant',
      label: 'دستیار هوشمند جمینای',
      icon: <Bot className="w-5 h-5" />,
      badge: 'AI',
    },
    {
      id: 'settings',
      label: 'تنظیمات و نوتیفیکیشن',
      icon: <SettingsIcon className="w-5 h-5" />,
    },
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
            <h1 className="font-bold text-slate-100 text-sm">
              Personal Life OS
            </h1>
            <p className="text-xs text-cyan-400">سیستم عامل جامع زندگی</p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          {onOpenNotificationCenter && (
            <button
              type="button"
              onClick={onOpenNotificationCenter}
              className="relative p-2 rounded-xl bg-slate-800/90 border border-cyan-500/30 text-cyan-300 hover:text-white transition"
              aria-label="مرکز نوتیفیکیشن"
            >
              <BellRing className="w-5 h-5" />
              {unreadNotificationsCount > 0 && (
                <span className="absolute -top-1 -right-1 min-w-[18px] h-[18px] px-1 rounded-full bg-rose-500 text-white text-[10px] font-extrabold flex items-center justify-center">
                  {unreadNotificationsCount}
                </span>
              )}
            </button>
          )}

          <button
            onClick={onToggleMobile}
            className="p-2 rounded-lg bg-slate-800/80 text-slate-300 hover:text-white"
          >
            {isOpenMobile ? (
              <X className="w-6 h-6" />
            ) : (
              <Menu className="w-6 h-6" />
            )}
          </button>
        </div>
      </div>

      {/* Backdrop for Mobile */}
      {isOpenMobile && (
        <div
          onClick={onToggleMobile}
          className="lg:hidden fixed inset-0 bg-black/70 backdrop-blur-sm z-40"
        />
      )}

      {/* Sidebar Container */}
      <aside
        className={`
        fixed lg:static top-0 right-0 z-50 h-full w-72 bg-slate-900/95 backdrop-blur-xl border-l border-slate-800/80
        flex flex-col transition-transform duration-300 ease-in-out
        ${isOpenMobile ? 'translate-x-0' : 'translate-x-full lg:translate-x-0'}
      `}
      >
        {/* Logo / Brand Header */}
        <div className="p-6 border-b border-slate-800/60 hidden lg:flex items-center justify-between">
          <div className="flex items-center space-x-3 space-x-reverse">
            <div className="w-10 h-10 rounded-2xl bg-gradient-to-tr from-cyan-500 to-blue-600 flex items-center justify-center shadow-lg shadow-cyan-500/30">
              <Sparkles className="w-6 h-6 text-white animate-pulse" />
            </div>
            <div>
              <h1 className="font-bold text-slate-100 text-base tracking-tight">
                Personal Life OS
              </h1>
              <p className="text-xs text-cyan-400 font-medium">
                نسخه وب پیشرفته فارسی
              </p>
            </div>
          </div>

          {onOpenNotificationCenter && (
            <button
              type="button"
              onClick={onOpenNotificationCenter}
              title="مرکز نوتیفیکیشن و لاگ فیش‌ها"
              className="relative p-2.5 rounded-xl bg-slate-800/90 border border-cyan-500/30 text-cyan-300 hover:bg-cyan-500/20 transition"
            >
              <BellRing className="w-5 h-5" />
              {unreadNotificationsCount > 0 && (
                <span className="absolute -top-1.5 -right-1.5 min-w-[20px] h-5 px-1 rounded-full bg-rose-500 text-white text-[10px] font-extrabold flex items-center justify-center shadow">
                  {unreadNotificationsCount}
                </span>
              )}
            </button>
          )}
        </div>

        {/* Dedicated Quick Notification / Receipt Listener Button in Sidebar */}
        {onOpenNotificationCenter && (
          <div className="px-4 pt-4">
            <button
              type="button"
              onClick={() => {
                onOpenNotificationCenter();
                if (isOpenMobile) onToggleMobile();
              }}
              className="w-full px-4 py-3 rounded-2xl bg-gradient-to-l from-cyan-500/15 via-slate-900 to-emerald-500/15 border border-cyan-500/30 hover:border-cyan-400/60 transition flex items-center justify-between group"
            >
              <div className="flex items-center space-x-2.5 space-x-reverse">
                <BellRing className="w-4 h-4 text-cyan-400 group-hover:animate-bounce" />
                <div className="text-right">
                  <div className="text-xs font-extrabold text-slate-100">
                    نوتیفیکیشن و لاگ فیش‌ها
                  </div>
                  <div className="text-[10px] text-emerald-400">
                    ● شنونده نوتیفیکیشن بانکی فعال
                  </div>
                </div>
              </div>
              {unreadNotificationsCount > 0 ? (
                <span className="px-2 py-0.5 rounded-full bg-rose-500 text-white text-[10px] font-extrabold">
                  {unreadNotificationsCount} جدید
                </span>
              ) : (
                <span className="px-2 py-0.5 rounded-full bg-slate-800 text-slate-400 text-[10px]">
                  لاگ‌ها
                </span>
              )}
            </button>
          </div>
        )}

        {/* Navigation Items */}
        <nav className="flex-1 px-4 py-4 space-y-2 overflow-y-auto">
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
                  ${
                    isActive
                      ? 'bg-gradient-to-r from-cyan-500/20 to-blue-600/20 text-cyan-400 border border-cyan-500/30 shadow-lg shadow-cyan-500/10'
                      : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/50 border border-transparent'
                  }
                `}
              >
                <div className="flex items-center space-x-3 space-x-reverse">
                  <span
                    className={isActive ? 'text-cyan-400' : 'text-slate-400'}
                  >
                    {item.icon}
                  </span>
                  <span>{item.label}</span>
                </div>
                {item.badge && (
                  <span
                    className={`text-[10px] px-2 py-0.5 rounded-full font-semibold ${
                      isActive
                        ? 'bg-cyan-500 text-slate-950'
                        : 'bg-slate-800 text-slate-300'
                    }`}
                  >
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
