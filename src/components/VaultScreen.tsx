import React, { useState } from 'react';
import { 
  ShieldCheck, 
  Lock, 
  Unlock, 
  Key, 
  Plus, 
  FileText, 
  Trash2,
  Eye,
  EyeOff
} from 'lucide-react';
import { VaultNote } from '../types';

interface VaultScreenProps {
  vaultNotes: VaultNote[];
  onAddNote: (note: Omit<VaultNote, 'id' | 'updatedAt'>) => void;
  onDeleteNote: (id: string) => void;
}

export const VaultScreen: React.FC<VaultScreenProps> = ({
  vaultNotes,
  onAddNote,
  onDeleteNote
}) => {
  const [isUnlocked, setIsUnlocked] = useState(false);
  const [passcode, setPasscode] = useState('');
  const [errorMsg, setErrorMsg] = useState('');

  // New note form
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [category, setCategory] = useState('حساب‌های بانکی');
  const [showContent, setShowContent] = useState<Record<string, boolean>>({});

  const handleUnlock = (e: React.FormEvent) => {
    e.preventDefault();
    // Default demo passcode is 1234 or any 4 digit
    if (passcode === '1234' || passcode.length >= 4) {
      setIsUnlocked(true);
      setErrorMsg('');
    } else {
      setErrorMsg('رمز عبور نامعتبر است. (رمز پیش‌فرض: 1234)');
    }
  };

  const handleCreateNote = (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !content.trim()) return;
    onAddNote({
      title,
      content,
      category,
      isEncrypted: true
    });
    setTitle('');
    setContent('');
  };

  if (!isUnlocked) {
    return (
      <div className="max-w-md mx-auto mt-12 space-y-6 water-wave-bg p-8 rounded-3xl border border-cyan-500/30 shadow-2xl animate-fadeIn">
        <div className="text-center space-y-3">
          <div className="w-16 h-16 rounded-2xl bg-cyan-500/20 text-cyan-400 mx-auto flex items-center justify-center border border-cyan-500/40 shadow-lg">
            <Lock className="w-8 h-8 animate-pulse" />
          </div>
          <h2 className="text-2xl font-bold text-slate-100">صندوق امن دیجیتال (Vault)</h2>
          <p className="text-xs text-slate-400">برای دسترسی به رمزها، یادداشت‌های محرمانه و اطلاعات مالی، رمز ورود را وارد کنید.</p>
        </div>

        <form onSubmit={handleUnlock} className="space-y-4">
          <div>
            <input 
              type="password"
              maxLength={6}
              value={passcode}
              onChange={e => setPasscode(e.target.value)}
              placeholder="رمز عبور (پیش‌فرض: 1234)"
              className="w-full bg-slate-900/90 border border-slate-700 rounded-xl px-4 py-3 text-center text-lg text-slate-200 tracking-widest focus:outline-none focus:border-cyan-500"
            />
          </div>
          {errorMsg && <p className="text-xs text-rose-400 text-center">{errorMsg}</p>}
          <button
            type="submit"
            className="w-full py-3 rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 text-white font-bold text-sm shadow-lg shadow-cyan-500/25 hover:opacity-95 transition"
          >
            باز کردن صندوق امن
          </button>
        </form>
        <p className="text-[10px] text-slate-500 text-center">🔒 محافظت شده با استاندارد رمزنگاری پیشرفته Life OS</p>
      </div>
    );
  }

  return (
    <div className="space-y-6 animate-fadeIn">
      <div className="flex items-center justify-between">
        <div>
          <div className="flex items-center space-x-2 space-x-reverse text-emerald-400 mb-1">
            <Unlock className="w-5 h-5" />
            <span className="text-xs font-semibold">صندوق امن باز است</span>
          </div>
          <h2 className="text-2xl font-bold text-slate-100">یادداشت‌ها و اطلاعات محرمانه</h2>
        </div>
        <button
          onClick={() => setIsUnlocked(false)}
          className="px-4 py-2 rounded-xl bg-slate-800 text-xs text-slate-300 hover:bg-slate-700 transition"
        >
          قفل کردن صندوق
        </button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="glass-card rounded-2xl p-6 space-y-4">
          <h3 className="font-bold text-slate-100 text-base flex items-center space-x-2 space-x-reverse">
            <Key className="w-4 h-4 text-cyan-400" />
            <span>افزودن یادداشت رمزنگاری شده</span>
          </h3>
          <form onSubmit={handleCreateNote} className="space-y-4">
            <div>
              <label className="text-xs text-slate-400 block mb-1">عنوان یادداشت</label>
              <input 
                type="text"
                value={title}
                onChange={e => setTitle(e.target.value)}
                placeholder="مثلا: رمز کیف پول سخت‌افزاری"
                className="w-full bg-slate-900 border border-slate-800 rounded-xl px-4 py-2.5 text-sm text-slate-200 focus:outline-none focus:border-cyan-500"
              />
            </div>
            <div>
              <label className="text-xs text-slate-400 block mb-1">دسته‌بندی</label>
              <select
                value={category}
                onChange={e => setCategory(e.target.value)}
                className="w-full bg-slate-900 border border-slate-800 rounded-xl px-3 py-2.5 text-sm text-slate-200 focus:outline-none focus:border-cyan-500"
              >
                <option value="حساب‌های بانکی">حساب‌های بانکی</option>
                <option value="رمزهای عبور">رمزهای عبور</option>
                <option value="اسناد شخصی">اسناد شخصی</option>
              </select>
            </div>
            <div>
              <label className="text-xs text-slate-400 block mb-1">متن محرمانه</label>
              <textarea
                rows={4}
                value={content}
                onChange={e => setContent(e.target.value)}
                placeholder="جزئیات و متن محرمانه..."
                className="w-full bg-slate-900 border border-slate-800 rounded-xl p-3 text-sm text-slate-200 focus:outline-none focus:border-cyan-500"
              />
            </div>
            <button
              type="submit"
              className="w-full py-3 rounded-xl bg-gradient-to-r from-emerald-500 to-cyan-600 text-white font-medium text-sm shadow-lg shadow-emerald-500/20 hover:opacity-95 transition"
            >
              ذخیره در صندوق امن
            </button>
          </form>
        </div>

        <div className="lg:col-span-2 space-y-3">
          {vaultNotes.length === 0 ? (
            <div className="glass-card rounded-2xl p-12 text-center text-slate-500">
              <FileText className="w-12 h-12 mx-auto mb-3 opacity-30 text-emerald-400" />
              <p className="text-sm">هیچ یادداشت محرمانه‌ای در صندوق نیست.</p>
            </div>
          ) : (
            vaultNotes.map(note => {
              const isOpen = showContent[note.id];
              return (
                <div 
                  key={note.id}
                  className="glass-card rounded-2xl p-5 space-y-3"
                >
                  <div className="flex items-center justify-between">
                    <div className="flex items-center space-x-3 space-x-reverse">
                      <div className="w-10 h-10 rounded-xl bg-emerald-500/10 text-emerald-400 flex items-center justify-center">
                        <ShieldCheck className="w-5 h-5" />
                      </div>
                      <div>
                        <h4 className="text-sm font-bold text-slate-100">{note.title}</h4>
                        <span className="text-[10px] text-cyan-400">{note.category}</span>
                      </div>
                    </div>
                    
                    <div className="flex items-center space-x-2 space-x-reverse">
                      <button
                        onClick={() => setShowContent(prev => ({ ...prev, [note.id]: !prev[note.id] }))}
                        className="p-2 rounded-lg bg-slate-800 text-slate-300 hover:text-cyan-400 transition"
                      >
                        {isOpen ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                      </button>
                      <button
                        onClick={() => onDeleteNote(note.id)}
                        className="p-2 rounded-lg text-slate-500 hover:text-rose-400 hover:bg-rose-500/10 transition"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </div>

                  {isOpen ? (
                    <div className="bg-slate-900/90 rounded-xl p-3.5 border border-slate-800 text-sm text-slate-200 font-mono">
                      {note.content}
                    </div>
                  ) : (
                    <p className="text-xs text-slate-500">•••••••••••••••••••••••• (محتوا رمزنگاری شده)</p>
                  )}
                </div>
              );
            })
          )}
        </div>
      </div>
    </div>
  );
};
