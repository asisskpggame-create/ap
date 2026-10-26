import React, { useState } from 'react';
import {
  Mic,
  MicOff,
  Send,
  Sparkles,
  Bot,
  User,
  CheckCircle,
  XCircle,
  AlertCircle,
  ArrowDownLeft,
  ArrowUpRight,
  RefreshCw,
  Coins,
  HandCoins,
  CheckSquare,
  Droplets,
  RotateCcw
} from 'lucide-react';
import { useLifeOs } from '../../context/LifeOsContext';
import { TransactionIntentEngine } from '../../utils/transactionIntentEngine';
import { formatFinancialAmount, currentLocalEpochDay, toPersianDigits } from '../../utils/persianDateUtils';

export const AssistantScreen: React.FC = () => {
  const {
    chatMessages,
    addChatMessage,
    clearChat,
    addExpense,
    addTask,
    addConsumption,
    preferences
  } = useLifeOs();

  const [inputText, setInputText] = useState('');
  const [isProcessing, setIsProcessing] = useState(false);
  const [pendingConfirmation, setPendingConfirmation] = useState<{
    id: string;
    type: 'EXPENSE' | 'INCOME' | 'TRANSFER' | 'DEBT' | 'RECEIVABLE' | 'TASK' | 'WATER';
    data: any;
    summary: string;
  } | null>(null);

  const suggestedPrompts = [
    '۲۵۰ هزار تومن اسنپ دادم',
    'از علی ۱ میلیون تومن قرض گرفتم',
    '۶۰۰ تومن بابت فروش جمینای گرفتم',
    '۵ میلیون از کارت اصلی به بلوبانک کارت به کارت کردم',
    'تسک تحویل گزارش مالی را برای فردا ثبت کن',
    '۵۰۰ میلی‌لیتر آب نوشیدم'
  ];

  const handleSendMessage = (textToSend?: string) => {
    const text = (textToSend || inputText).trim();
    if (!text || isProcessing) return;

    // 1. Add user message
    addChatMessage({
      role: 'USER',
      messageText: text
    });
    setInputText('');
    setIsProcessing(true);

    setTimeout(() => {
      processAssistantResponse(text);
      setIsProcessing(false);
    }, 450);
  };

  const processAssistantResponse = (userText: string) => {
    const todayEpoch = currentLocalEpochDay();
    const lower = userText.toLowerCase();

    // 1. Check if user wants to add a Task
    if (lower.includes('تسک') || lower.includes('کار') || lower.includes('وظیفه') || lower.includes('یادآوری')) {
      const taskTitle = userText
        .replace(/تسک/g, '')
        .replace(/را برای فردا ثبت کن/g, '')
        .replace(/رو ثبت کن/g, '')
        .replace(/ثبت کن/g, '')
        .trim() || 'وظیفه جدید';

      setPendingConfirmation({
        id: `conf_${Date.now()}`,
        type: 'TASK',
        data: {
          title: taskTitle,
          priority: lower.includes('فوری') || lower.includes('مهم') ? 'HIGH' : 'MEDIUM',
          dueDateEpochDay: lower.includes('فردا') ? todayEpoch + 1 : todayEpoch,
          category: 'شخصی'
        },
        summary: `ثبت تسک: «${taskTitle}» با اولویت ${lower.includes('فوری') ? 'مهم' : 'معمولی'}`
      });

      addChatMessage({
        role: 'ASSISTANT',
        messageText: `یک تسک جدید بر اساس درخواست شما آماده شد. جهت رعایت نظم، لطفاً پیش‌نمایش زیر را تأیید فرمایید.`
      });
      return;
    }

    // 2. Check if user wants to log Water
    if (lower.includes('آب') && (lower.includes('میلی') || lower.includes('لیوان') || lower.includes('خوردم') || lower.includes('نوشیدم'))) {
      const mlMatch = lower.match(/([0-9]+)\s*(?:میلی|ml)/);
      const ml = mlMatch ? parseInt(mlMatch[1]) : lower.includes('لیوان') ? 250 : 500;

      setPendingConfirmation({
        id: `conf_${Date.now()}`,
        type: 'WATER',
        data: { ml },
        summary: `ثبت مصرف ${toPersianDigits(ml)} میلی‌لیتر آب آشامیدنی`
      });

      addChatMessage({
        role: 'ASSISTANT',
        messageText: `میزان آب نوشیده‌شده شناسایی شد. با تأیید شما در پایش سلامت ثبت می‌گردد.`
      });
      return;
    }

    // 3. Financial intent parsing via TransactionIntentEngine
    const intent = TransactionIntentEngine.detectIntent(userText, preferences.defaultCurrency);

    if (intent.type !== 'UNKNOWN' && intent.amount > 0) {
      const typeLabel =
        intent.type === 'INCOME'
          ? 'درآمد و واریز'
          : intent.type === 'TRANSFER'
          ? 'انتقال بین حسابی'
          : intent.type === 'DEBT'
          ? 'بدهی و استقراض'
          : intent.type === 'RECEIVABLE'
          ? 'طلبکاری و وام'
          : 'پرداخت هزینه';

      setPendingConfirmation({
        id: `conf_${Date.now()}`,
        type: intent.type as any,
        data: {
          title: intent.title || intent.category,
          amount: intent.amount,
          category: intent.category,
          transactionType: intent.type,
          currency: intent.currency,
          account: intent.account,
          person: intent.person
        },
        summary: `نوع: ${typeLabel} | مبلغ: ${formatFinancialAmount(intent.amount, 'TOMAN')} | دسته‌بندی: ${intent.category}`
      });

      addChatMessage({
        role: 'ASSISTANT',
        messageText: `قصد مالی شما با دقت بالا تشخیص داده شد (${toPersianDigits(Math.round(intent.confidence * 100))}٪ اطمینان).\n${intent.reasoning}\nجهت امنیت حساب‌های شما، این عملیات مستلزم تأیید نهایی است:`
      });
    } else {
      addChatMessage({
        role: 'ASSISTANT',
        messageText: `پیام شما دریافت شد. در صورتی که تمایل به ثبت تراکنش مالی، تسک جدید یا میزان آب دارید، می‌توانید جملاتی مانند «۲۵۰ هزار تومن اسنپ دادم» یا «از علی ۱ میلیون قرض گرفتم» را بیان کنید.`
      });
    }
  };

  const handleConfirmAction = () => {
    if (!pendingConfirmation) return;
    const { type, data } = pendingConfirmation;
    const todayEpoch = currentLocalEpochDay();

    if (type === 'TASK') {
      addTask({
        title: data.title,
        description: '',
        category: data.category || 'عمومی',
        tags: [data.category || 'عمومی'],
        priority: data.priority || 'MEDIUM',
        dueDateEpochDay: data.dueDateEpochDay || todayEpoch,
        dueTimeMinutes: 600,
        estimatedDurationMinutes: 30,
        status: 'PENDING',
        subtasks: [],
        recurrenceRule: 'NONE',
        recurrenceIntervalDays: 0,
        notes: 'ثبت شده توسط دستیار هوشمند',
        inputSource: 'AI_ASSISTANT',
        sortOrder: 0
      });
    } else if (type === 'WATER') {
      addConsumption({
        itemName: 'آب آشامیدنی',
        category: 'WATER',
        quantity: data.ml,
        unit: 'ml',
        timestampMillis: Date.now(),
        localEpochDay: todayEpoch,
        notes: 'ثبت از طریق دستیار صوتی',
        inputSource: 'AI_ASSISTANT'
      });
    } else {
      // Financial entry
      addExpense({
        title: data.title,
        category: data.category,
        amount: data.amount,
        currency: 'TOMAN',
        timestampMillis: Date.now(),
        localEpochDay: todayEpoch,
        notes: `[AI_ASSISTANT] ${data.person ? 'شخص: ' + data.person : ''} ${data.account ? 'حساب: ' + data.account : ''}`.trim(),
        inputSource: 'AI_ASSISTANT',
        transactionType: data.transactionType,
        paidAmount: data.transactionType === 'DEBT' || data.transactionType === 'RECEIVABLE' ? 0 : data.amount
      });
    }

    addChatMessage({
      role: 'ASSISTANT',
      messageText: `عملیات «${pendingConfirmation.summary}» با موفقیت ثبت شد و در داشبورد و سوابق اعمال گردید.`
    });

    setPendingConfirmation(null);
  };

  const handleRejectAction = () => {
    addChatMessage({
      role: 'ASSISTANT',
      messageText: 'عملیات لغو گردید و داده‌ای ذخیره نشد.'
    });
    setPendingConfirmation(null);
  };

  return (
    <div className="space-y-4 pb-24 max-w-4xl mx-auto flex flex-col h-[calc(100vh-140px)]">
      {/* Header bar */}
      <div className="glass-panel p-3.5 rounded-2xl border border-white/10 flex items-center justify-between shrink-0">
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-xl bg-teal-500/20 text-teal-300 flex items-center justify-center">
            <Bot className="w-4 h-4" />
          </div>
          <div>
            <h3 className="text-xs sm:text-sm font-bold text-slate-100 flex items-center gap-1.5">
              <span>دستیار هوشمند Personal Life OS</span>
              <span className="text-[10px] px-2 py-0.2 rounded-full bg-teal-500/10 text-teal-300 border border-teal-500/20">
                پایش امنیت تراکنش‌ها
              </span>
            </h3>
            <p className="text-[11px] text-slate-400">
              تشخیص قطعی قصد مالی بدون ذخیره ناخواسته، منطبق با موتور نیات فارسی
            </p>
          </div>
        </div>
        <button
          onClick={clearChat}
          title="پاک‌کردن تاریخچه گفت‌وگو"
          className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition cursor-pointer"
        >
          <RotateCcw className="w-4 h-4" />
        </button>
      </div>

      {/* Messages area */}
      <div className="flex-1 overflow-y-auto space-y-3 px-1 py-2">
        {chatMessages.map(msg => {
          const isUser = msg.role === 'USER';
          return (
            <div
              key={msg.id}
              className={`flex items-start gap-2.5 ${isUser ? 'flex-row-reverse' : ''}`}
            >
              <div
                className={`w-7 h-7 rounded-lg flex items-center justify-center shrink-0 ${
                  isUser
                    ? 'bg-teal-500/20 text-teal-300 border border-teal-500/30'
                    : 'bg-slate-800 text-teal-400 border border-white/10'
                }`}
              >
                {isUser ? <User className="w-4 h-4" /> : <Bot className="w-4 h-4" />}
              </div>
              <div
                className={`p-3.5 rounded-2xl max-w-[85%] text-xs sm:text-sm leading-relaxed whitespace-pre-wrap ${
                  isUser
                    ? 'bg-teal-600/20 border border-teal-500/30 text-teal-100 rounded-tr-none'
                    : 'glass-panel border border-white/10 text-slate-200 rounded-tl-none'
                }`}
              >
                {msg.messageText}
              </div>
            </div>
          );
        })}

        {isProcessing && (
          <div className="flex items-center gap-2 text-xs text-teal-400 animate-pulse px-3 py-1">
            <Sparkles className="w-3.5 h-3.5" />
            <span>در حال تحلیل قصد محاوره‌ای و ساختاردهی...</span>
          </div>
        )}

        {/* Pending Confirmation Card */}
        {pendingConfirmation && (
          <div className="glass-panel p-4 rounded-2xl border border-teal-500/40 bg-teal-950/30 space-y-3 animate-fade-in">
            <div className="flex items-center gap-2 text-teal-300 text-xs font-bold">
              <AlertCircle className="w-4 h-4 text-teal-400" />
              <span>پیش‌نمایش عملیات پیشنهادی (الزام تأیید جهت امنیت):</span>
            </div>
            <div className="p-3 rounded-xl bg-slate-900/80 border border-white/10 text-xs text-slate-200 space-y-1">
              <p className="font-semibold">{pendingConfirmation.summary}</p>
            </div>
            <div className="flex items-center justify-end gap-2 pt-1">
              <button
                onClick={handleRejectAction}
                className="px-3.5 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-medium transition cursor-pointer flex items-center gap-1"
              >
                <XCircle className="w-3.5 h-3.5" />
                لغو
              </button>
              <button
                onClick={handleConfirmAction}
                className="px-4 py-1.5 rounded-xl glass-button-primary text-xs font-bold transition cursor-pointer flex items-center gap-1.5"
              >
                <CheckCircle className="w-3.5 h-3.5" />
                تأیید و ذخیره نهایی
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Suggested Quick Prompts */}
      <div className="flex items-center gap-1.5 overflow-x-auto py-1 shrink-0">
        <span className="text-[10px] text-slate-500 whitespace-nowrap">پیشنهاد:</span>
        {suggestedPrompts.map((prompt, idx) => (
          <button
            key={idx}
            onClick={() => handleSendMessage(prompt)}
            className="px-2.5 py-1 rounded-lg bg-slate-900/80 hover:bg-slate-800 text-slate-300 text-[11px] border border-white/5 whitespace-nowrap transition cursor-pointer hover:border-teal-500/30"
          >
            {prompt}
          </button>
        ))}
      </div>

      {/* Input box */}
      <div className="glass-panel p-2 rounded-2xl border border-white/10 flex items-center gap-2 shrink-0">
        <input
          type="text"
          value={inputText}
          onChange={e => setInputText(e.target.value)}
          onKeyDown={e => {
            if (e.key === 'Enter') {
              e.preventDefault();
              handleSendMessage();
            }
          }}
          placeholder="هر فعالیتی را بنویسید (مثلاً: ۳۰۰ تومن اسنپ دادم، ۱ میلیون از علی قرض گرفتم)..."
          className="flex-1 bg-transparent px-3 py-1.5 text-xs sm:text-sm text-slate-100 placeholder-slate-500 focus:outline-none"
        />
        <button
          onClick={() => handleSendMessage()}
          disabled={!inputText.trim() || isProcessing}
          className={`p-2 rounded-xl transition cursor-pointer ${
            inputText.trim() && !isProcessing
              ? 'glass-button-primary'
              : 'bg-slate-800 text-slate-600'
          }`}
        >
          <Send className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
};
