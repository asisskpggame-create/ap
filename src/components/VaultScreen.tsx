import React, { useState } from 'react';
import { Shield, Lock, Plus, Trash2, Eye, EyeOff, KeyRound } from 'lucide-react';
import { VaultNote } from '../types';

interface VaultScreenProps {
  vaultNotes: VaultNote[];
  onAddNote: (note: Omit<VaultNote, 'id' | 'updatedAt'>) => void;
  onDeleteNote: (id: string) => void;
}

export const VaultScreen: React.FC<VaultScreenProps> = ({
  vaultNotes,
  onAddNote,
  onDeleteNote,
}) => {
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [category, setCategory] = useState('حساب‌های بانکی');
  const [revealedIds, setRevealedIds] = useState<Record<string, boolean>>({});

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !content.trim()) return;
    onAddNote({
      title: title.trim(),
      content: content.trim(),
      category,
      isEncrypted: true,
    });
    setTitle('');
    setContent('');
  };

  const toggleReveal = (id: string) => {
    setRevealedIds((prev) => ({ ...prev, [id]: !prev[id] }));
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-white flex items-center gap-2">
          <Shield className="w-7 h-7 text-purple-400" />
          صندوق امن و یادداشت‌های رمزنگاری‌شده (Vault)
        </h1>
        <p className="text-slate-400 text-sm mt-1">
          نگهداری امن اطلاعات حساس، شماره شبا/کارت و یادداشت‌های خصوصی در حافظه محلی
        </p>
      </div>

      <form
        onSubmit={handleSubmit}
        className="glass-card rounded-2xl p-5 border border-purple-500/20 space-y-4"
      >
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <input
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="عنوان یادداشت امن..."
            className="sm:col-span-2 bg-slate-900 border border-slate-700/80 rounded-xl px-4 py-2.5 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-purple-400"
          />
          <select
            value={category}
            onChange={(e) => setCategory(e.target.value)}
            className="bg-slate-900 border border-slate-700/80 rounded-xl px-3 py-2.5 text-sm text-slate-200"
          >
            <option value="حساب‌های بانکی">حساب‌های بانکی</option>
            <option value="رمزهای عبور">رمزهای عبور</option>
            <option value="اسناد مهم">اسناد مهم</option>
          </select>
        </div>
        <textarea
          value={content}
          onChange={(e) => setContent(e.target.value)}
          rows={3}
          placeholder="محتوای محرمانه..."
          className="w-full bg-slate-900 border border-slate-700/80 rounded-xl px-4 py-2.5 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-purple-400"
        />
        <button
          type="submit"
          className="bg-gradient-to-r from-purple-500 to-indigo-600 hover:from-purple-400 hover:to-indigo-500 text-white font-bold px-5 py-2.5 rounded-xl text-sm inline-flex items-center gap-2"
        >
          <Plus className="w-4 h-4" />
          ذخیره در صندوق امن
        </button>
      </form>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {vaultNotes.map((note) => {
          const isRevealed = !!revealedIds[note.id];
          return (
            <div
              key={note.id}
              className="glass-card rounded-2xl p-5 border border-slate-800 space-y-3"
            >
              <div className="flex items-start justify-between gap-2">
                <div className="flex items-center gap-2">
                  <KeyRound className="w-4 h-4 text-purple-400 shrink-0" />
                  <h3 className="font-bold text-white text-sm">{note.title}</h3>
                </div>
                <span className="text-[11px] px-2.5 py-0.5 rounded-full bg-purple-500/15 text-purple-300 border border-purple-500/30">
                  {note.category}
                </span>
              </div>

              <div className="bg-slate-950/80 border border-slate-800/80 rounded-xl p-3 font-mono text-xs text-slate-300 break-all">
                {isRevealed ? note.content : '••••••••••••••••••••••••••••••••'}
              </div>

              <div className="flex items-center justify-between text-xs text-slate-500 pt-1">
                <span className="inline-flex items-center gap-1">
                  <Lock className="w-3 h-3 text-emerald-400" />
                  بروزرسانی: {note.updatedAt}
                </span>
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => toggleReveal(note.id)}
                    className="text-slate-400 hover:text-white p-1.5 rounded-lg bg-slate-900 border border-slate-800"
                    title={isRevealed ? 'مخفی کردن' : 'نمایش'}
                  >
                    {isRevealed ? (
                      <EyeOff className="w-4 h-4" />
                    ) : (
                      <Eye className="w-4 h-4" />
                    )}
                  </button>
                  <button
                    onClick={() => onDeleteNote(note.id)}
                    className="text-slate-400 hover:text-rose-400 p-1.5 rounded-lg bg-slate-900 border border-slate-800"
                    title="حذف"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};
