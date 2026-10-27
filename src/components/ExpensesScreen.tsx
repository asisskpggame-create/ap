import React, { useState } from 'react';
import { 
  Wallet, 
  Plus, 
  MessageSquareText, 
  TrendingUp, 
  Trash2, 
  CheckCircle,
  Building2,
  AlertCircle,
  Sparkles
} from 'lucide-react';
import { ExpenseItem } from '../types';
import { formatToman, parseBankSms } from '../utils/persianUtils';

interface ExpensesScreenProps {
  expenses: ExpenseItem[];
  onAddExpense: (expense: Omit<ExpenseItem, 'id'>) => void;
  onDeleteExpense: (id: string) => void;
}

export const ExpensesScreen: React.FC<ExpensesScreenProps> = ({
  expenses,
  onAddExpense,
  onDeleteExpense
}) => {
  const [smsInput, setSmsInput] = useState('');
  const [parseResult, setParseResult] = useState<string | null>(null);

  // Manual expense form
  const [title, setTitle] = useState('');
  const [amount, setAmount] = useState('');
  const [category, setCategory] = useState('خرید عمومی');

  const totalSpent = expenses.reduce((acc, curr) => acc + curr.amount, 0);

  const handleParseSms = (e: React.FormEvent) => {
    e.preventDefault();
    if (!smsInput.trim()) return;

    const parsed = parseBankSms(smsInput);
    if (parsed) {
      onAddExpense({
        title: parsed.merchant,
        amount: parsed.amount,
        category: parsed.type === 'deposit' ? 'درآمد / واریزی' : 'بانکی / پیامک',
        date: '۱۴۰۵/۰۸/۰۵',
        isSmsParsed: true,
        bankName: parsed.bankName
      });
      setParseResult(`موفق: تراکنش ${formatToman(parsed.amount)} از ${parsed.bankName} (${parsed.type === 'deposit' ? 'واریز' : 'برداشت'}) ثبت شد.`);
      setSmsInput('');
    } else {
      setParseResult('خطا در تحلیل پیامک. الگوی مبلغ یا بانک شناسایی نشد.');
    }
  };

  const sampleSmsList = [
    { label: 'بلو (سامانه پُل)', text: 'بلو\nانتقال پل\nمبلغ: ۱,۵۰۰,۰۰۰ ریال\nبه: علیرضا یوسفی\nمانده: ۶۲,۶۶۱,۴۵۲ ریال\nکد پیگیری: ۹۸۷۶۵۴۳۲۱' },
    { label: 'بانک رفاه (قالب ویژه R)', text: 'رفاه حساب 62****13\nبرداشت1,960,000R کد2000\nخرید فروشگاهی\nبمانده1,231,509R' },
    { label: 'بانک ملت (بهسازان)', text: 'بانک ملت\nبرداشت از: ۶۱۰۴۳۳******۱۲۳۴\nمبلغ: ۵۰۰,۰۰۰ ریال\nمانده: ۸,۴۲۰,۰۰۰ ریال\nپیگیری: ۹۸۷۶۵۴' },
    { label: 'بانک صادرات (فشرده +)', text: '300325283197\n1,698,500+\n1403/8/5-11:43\nمانده:32,317,981' },
    { label: 'بانک تجارت (*تجارت*)', text: '*بانک تجارت*\nبرداشت: ۷۲۰,۰۰۰ ریال\nاز طریق: شتاب\nمانده: ۳۱,۴۰۱,۷۹۳ ریال' }
  ];

  const handleManualAdd = (e: React.FormEvent) => {
    e.preventDefault();
    const numAmount = parseInt(amount, 10);
    if (!title.trim() || isNaN(numAmount)) return;

    onAddExpense({
      title,
      amount: numAmount,
      category,
      date: '۱۴۰۵/۰۸/۰۵'
    });
    setTitle('');
    setAmount('');
  };

  return (
    <div className="space-y-6 animate-fadeIn">
      <div>
        <h2 className="text-2xl font-bold text-slate-100">مدیریت هزینه‌ها و پیامک بانکی</h2>
        <p className="text-xs text-slate-400">تحلیل هوشمند پیامک‌های واریز و برداشت بانک‌های ایران و ثبت هزینه‌ها</p>
      </div>

      {/* Top Banner: Total Spending */}
      <div className="glass-card rounded-3xl p-6 bg-gradient-to-l from-slate-900 to-amber-950/30 border border-amber-500/20 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <span className="text-xs text-amber-400 font-medium">مجموع هزینه‌های ثبت شده ماه آبان</span>
          <h3 className="text-3xl font-bold text-slate-100 mt-1">{formatToman(totalSpent)}</h3>
          <p className="text-xs text-slate-400 mt-1">تعداد کل تراکنش‌ها: {expenses.length} مورد</p>
        </div>
        <div className="flex items-center space-x-2 space-x-reverse bg-amber-500/10 text-amber-400 px-4 py-2.5 rounded-2xl border border-amber-500/20 text-xs font-semibold">
          <TrendingUp className="w-4 h-4" />
          <span>پایداری مالی مطلوب</span>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Left Column: Bank SMS Parser simulation */}
        <div className="glass-card rounded-2xl p-6 space-y-4">
          <div className="flex items-center space-x-2 space-x-reverse text-cyan-400">
            <MessageSquareText className="w-5 h-5" />
            <h3 className="font-bold text-slate-100 text-base">اسکنر هوشمند پیامک بانکی</h3>
          </div>
          <p className="text-xs text-slate-400">
            متن پیامک بانکی خود را وارد کنید یا برای تست سریع یکی از نمونه‌های زیر را انتخاب نمایید:
          </p>

          {/* Sample Buttons for testing */}
          <div className="flex flex-wrap gap-2">
            {sampleSmsList.map((sample, idx) => (
              <button
                key={idx}
                type="button"
                onClick={() => setSmsInput(sample.text)}
                className="text-[11px] bg-slate-800 text-cyan-300 hover:bg-slate-700 px-2.5 py-1.5 rounded-lg border border-cyan-500/20 transition flex items-center space-x-1 space-x-reverse"
              >
                <Sparkles className="w-3 h-3 text-cyan-400" />
                <span>{sample.label}</span>
              </button>
            ))}
          </div>

          <form onSubmit={handleParseSms} className="space-y-4">
            <textarea
              rows={4}
              value={smsInput}
              onChange={e => setSmsInput(e.target.value)}
              placeholder="متن پیامک بانکی..."
              className="w-full bg-slate-900 border border-slate-800 rounded-xl p-3 text-sm text-slate-200 focus:outline-none focus:border-cyan-500"
            />
            <button
              type="submit"
              className="w-full py-3 rounded-xl bg-gradient-to-r from-cyan-500 to-blue-600 text-white font-medium text-sm shadow-lg shadow-cyan-500/20 hover:opacity-95 transition"
            >
              استخراج و ثبت خودکار تراکنش
            </button>
          </form>

          {parseResult && (
            <div className={`p-3 rounded-xl text-xs flex items-center space-x-2 space-x-reverse ${
              parseResult.includes('موفق') ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30' : 'bg-rose-500/20 text-rose-300 border border-rose-500/30'
            }`}>
              {parseResult.includes('موفق') ? <CheckCircle className="w-4 h-4" /> : <AlertCircle className="w-4 h-4" />}
              <span>{parseResult}</span>
            </div>
          )}
        </div>

        {/* Right Column: Manual Expense Entry */}
        <div className="glass-card rounded-2xl p-6 space-y-4">
          <div className="flex items-center space-x-2 space-x-reverse text-amber-400">
            <Plus className="w-5 h-5" />
            <h3 className="font-bold text-slate-100 text-base">ثبت دستی هزینه</h3>
          </div>

          <form onSubmit={handleManualAdd} className="space-y-3">
            <div>
              <label className="text-xs text-slate-400 block mb-1">عنوان / بابت</label>
              <input 
                type="text"
                value={title}
                onChange={e => setTitle(e.target.value)}
                placeholder="مثلا: خرید قهوه و میان‌وعده"
                className="w-full bg-slate-900 border border-slate-800 rounded-xl px-4 py-2 text-sm text-slate-200 focus:outline-none focus:border-cyan-500"
              />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="text-xs text-slate-400 block mb-1">مبلغ (تومان)</label>
                <input 
                  type="number"
                  value={amount}
                  onChange={e => setAmount(e.target.value)}
                  placeholder="مثلا: 150000"
                  className="w-full bg-slate-900 border border-slate-800 rounded-xl px-4 py-2 text-sm text-slate-200 focus:outline-none focus:border-cyan-500"
                />
              </div>
              <div>
                <label className="text-xs text-slate-400 block mb-1">دسته‌بندی</label>
                <select
                  value={category}
                  onChange={e => setCategory(e.target.value)}
                  className="w-full bg-slate-900 border border-slate-800 rounded-xl px-3 py-2 text-sm text-slate-200 focus:outline-none focus:border-cyan-500"
                >
                  <option value="خرید عمومی">خرید عمومی</option>
                  <option value="غذا و رستوران">غذا و رستوران</option>
                  <option value="حمل و نقل">حمل و نقل</option>
                  <option value="قبوض و خدمات">قبوض و خدمات</option>
                  <option value="سایر">سایر</option>
                </select>
              </div>
            </div>
            <button
              type="submit"
              className="w-full py-2.5 rounded-xl bg-gradient-to-r from-amber-500 to-orange-600 text-white font-medium text-sm shadow-lg shadow-amber-500/20 hover:opacity-95 transition mt-2"
            >
              ثبت هزینه در لیست
            </button>
          </form>
        </div>
      </div>

      {/* Expenses History List */}
      <div className="glass-card rounded-2xl p-6 space-y-4">
        <h3 className="font-bold text-slate-100 text-base">تاریخچه تراکنش‌ها و هزینه‌ها</h3>
        
        <div className="space-y-3">
          {expenses.length === 0 ? (
            <p className="text-xs text-slate-500 text-center py-6">هیچ هزینه‌ای ثبت نشده است.</p>
          ) : (
            expenses.map(item => (
              <div 
                key={item.id}
                className="bg-slate-900/60 border border-slate-800 rounded-xl p-4 flex items-center justify-between hover:border-cyan-500/30 transition"
              >
                <div className="flex items-center space-x-3 space-x-reverse">
                  <div className="w-10 h-10 rounded-xl bg-cyan-500/10 text-cyan-400 flex items-center justify-center">
                    <Building2 className="w-5 h-5" />
                  </div>
                  <div>
                    <h4 className="text-sm font-bold text-slate-200">{item.title}</h4>
                    <div className="flex items-center space-x-2 space-x-reverse text-[10px] text-slate-400 mt-0.5">
                      <span className="text-cyan-400 font-medium">{item.category}</span>
                      <span>•</span>
                      <span>{item.date}</span>
                      {item.bankName && (
                        <>
                          <span>•</span>
                          <span className="text-amber-400 font-semibold">{item.bankName}</span>
                        </>
                      )}
                    </div>
                  </div>
                </div>

                <div className="flex items-center space-x-4 space-x-reverse">
                  <span className="text-sm font-bold text-slate-100">{formatToman(item.amount)}</span>
                  <button
                    onClick={() => onDeleteExpense(item.id)}
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
  );
};
