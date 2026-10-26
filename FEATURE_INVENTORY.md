# فهرست جامع قابلیت‌های فعلی و تطبیق با نقشه راه (`FEATURE_INVENTORY.md`)

**مخزن:** `Personal Life OS` (`com.example`)  
**هدف سند:** ثبت وضعیت واقعی و خط‌به‌خط سورس فعلی پیش از اجرای فازهای بازطراحی `APP_ROADMAP_FA.md` (فاز صفر — ممیزی قبل از تغییر).  
**قاعده کلیدی:** هیچ قابلیتی حذف یا از نو بازنویسی نمی‌شود؛ داده‌های ذخیره‌شده در Room، DataStore و Firestore و مسیرهای ناوبری حفظ می‌گردند.

---

## ۱. جدول تطبیق قابلیت‌ها، محل فعلی، محل مقصد، وضعیت تست و ریسک

| ردیف | نام قابلیت | وضعیت فعلی در سورس | محل فعلی در UI / ناوبری | محل مقصد طبق نقشه راه | فایل‌ها و کلاس‌های مسئول | وضعیت تست | ریسک مهاجرت / تغییر |
|---|---|---|---|---|---|---|---|
| ۱ | **ناوبری اصلی و نوار پایین (Bottom Bar & Rail)** | پیاده‌سازی‌شده (ترتیب فعلی: امور مالی، روندها، هدف و عادت، دستیار + آیکون‌های بالا: گزارش‌ها، تاریخچه، قفل، تنظیمات) | `MainActivity.kt` (`AppSection`) | منوی پایین ۴ بخشی به ترتیب: **۱. مالی (`DAILY_LOG`)، ۲. اهداف و مسیر (`GOALS_AND_HABITS`)، ۳. داشبورد (`HOME`)، ۴. AI (`AI_ASSISTANT`)** | `MainActivity.kt` (`AppSection`, `AnimatedGlassBottomBar`, `AuthenticatedLifeOsShell`) | نیاز به بروزرسانی تگ‌های تست در `ExampleRobolectricTest.kt` | **پایین** — حفظ کلیدهای `route` (`DAILY_LOG`, `GOALS_AND_HABITS`, `HOME`, `AI_ASSISTANT`) برای جلوگیری از شکستن Deep Linkها و تست‌ها |
| ۲ | **ثبت درآمد، هزینه و دسته‌بندی‌های سفارشی** | پیاده‌سازی‌شده کامل | بخش `DAILY_LOG` ← تب «ورودی و خروجی‌ها» | بخش ۱: **مالی** (خانه مالی و تراکنش‌ها) | `DailyLogAndHealthScreen.kt`, `LifeOsViewModel.kt`, `LifeOsRepository.kt`, `LifeOsModels.kt` (`ExpenseEntry`) | تست واحد + Robolectric | **پایین** — مدل `ExpenseEntry` و جدول `cached_expenses` حفظ می‌شوند |
| ۳ | **حساب‌های مالی و انتقال بین حساب‌ها** | حساب‌های سفارشی در `UserPreferencesDoc.financialAccounts` وجود دارد؛ انتقال بین حساب شخصی بدون شمارش در درآمد/هزینه نیاز به تکمیل دارد | بخش `DAILY_LOG` | بخش ۱: **مالی** (تراکنش‌ها و حساب‌ها؛ قانون عدم شمارش انتقال شخصی در درآمد/هزینه) | `DailyLogAndHealthScreen.kt`, `PersianDateAndUnitUtils.kt`, `LifeOsModels.kt` | نیاز به تست سناریوی `TRANSFER` در ماتریس تست | **متوسط** — افزودن پشتیبانی از `TRANSFER` در محاسبات خالص (`aggregateExpensesByCurrency` / `aggregateIncomesByCurrency`) تا درآمد/هزینه مصنوعی ایجاد نشود |
| ۴ | **مدیریت بدهی، طلب، پرداخت جزئی و تسویه** | ثبت `DEBT` و `RECEIVABLE` و تسویه کامل (`DEBT_SETTLED`, `RECEIVABLE_SETTLED`) موجود است؛ پرداخت جزئی، مانده پویا، برنامه اقساط و اتصال به تسک‌های مسیر نیاز به تکمیل دارد | بخش `DAILY_LOG` ← تب «طلب و بدهی» | بخش ۱: **مالی** + اتصال مستقیم به بخش ۲ (**اهداف و مسیر**) | `LifeOsModels.kt` (`ExpenseEntry`, `Task`), `LifeOsDatabase.kt`, `LifeOsRepository.kt`, `DailyLogAndHealthScreen.kt` | نیاز به تست پرداخت جزئی، تسویه و عدم ثبت تکراری | **متوسط** — نیازمند افزودن فیلدهای غیرمخرب (با مقدار پیش‌فرض) یا متادیتای ساختاری و Migration نسخه‌بندی‌شده در Room بدون پاک کردن دیتابیس |
| ۵ | **اسکنر پیامک بانکی ایران (Permission-based SMS Parser)** | پیاده‌سازی‌شده کامل با فیلتر OTP، تشخیص بانک، تبدیل ریال به تومان و پیش‌نمایش قبل از ثبت | بخش `DAILY_LOG` (`readBankSmsInbox`, `SmartBankSmsParserModalSheet`, `BankSmsScannerUtility`) | بخش ۱: **مالی** (همراه با جلوگیری از ثبت تکراری پیامک + دستی) | `BankSmsScannerUtility.kt`, `DailyLogAndHealthScreen.kt`, `HistoryReportsSettingsScreens.kt` | دارای تست در `ExampleRobolectricTest.kt` | **پایین** — افزودن کلید ضدتکرار (Idempotency) برای پیامک‌های ثبت‌شده |
| ۶ | **محاسبه Safe-to-Spend و بودجه ماهانه** | پیاده‌سازی‌شده (`WorldClassOsUtils.calculateDailySafeToSpend`) | بخش `DAILY_LOG` و `HOME` | بخش ۱: **مالی** و کارت خلاصه در **داشبورد** | `WorldClassOsUtils.kt`, `DailyLogAndHealthScreen.kt`, `HomeDashboardScreen.kt` | تست واحد | **پایین** — شفاف‌سازی فرمول محاسبه و تفکیک از موجودی کل و پیش‌بینی پایان ماه |
| ۷ | **اهداف، تسک‌ها، ماتریس آیزنهاور، زیرتسک‌ها و تکرار** | پیاده‌سازی‌شده کامل (`Task`, `SubtaskItem`, `recurrenceRule`, اولویت‌ها، آرشیو، تغییر موعد) | بخش `GOALS_AND_HABITS` ← تب ۰ («اهداف و تسک‌ها») | بخش ۲: **اهداف و مسیر** (همراه با ارجاع اختیاری به بدهی اصلی بدون ساخت بدهی تکراری) | `GoalsAndHabitsCombinedScreen.kt`, `TasksScreen.kt`, `LifeOsRepository.kt`, `LifeOsViewModel.kt` | تست واحد و Robolectric | **پایین** — حفظ کامل جدول `cached_tasks` و افزودن ارجاع `linkedDebtId` برای ارتباط با بدهی |
| ۸ | **عادت‌های روزانه/هفتگی و تایمر تمرکز پومودورو** | پیاده‌سازی‌شده کامل (`Habit`, `HabitRecord`, Streak، هیت‌مپ، `FloatingFocusTimerBar`) | بخش `GOALS_AND_HABITS` ← تب ۱ («عادت‌های روزانه») + نوار شناور تمرکز در `MainActivity` | بخش ۲: **اهداف و مسیر** (زیربخش عادت‌ها و تمرکز) | `HabitsAndAssistantScreens.kt`, `GoalsAndHabitsCombinedScreen.kt`, `MainActivity.kt`, `LifeOsViewModel.kt` | تست واحد | **پایین** — بدون تغییر مخرب در مدل عادت‌ها |
| ۹ | **سلامت و سبک زندگی (آب، تغذیه/کالری، خواب، ورزش)** | پیاده‌سازی‌شده کامل (`ConsumptionEntry`, `SleepRecord`, `ExerciseRecord`, `CustomItem`) | بخش `GOALS_AND_HABITS` ← تب ۲ («سلامت و روتین») + خلاصه در `HOME` | بخش ۲: **اهداف و مسیر** ← زیربخش «سلامت و سبک زندگی» + خلاصه در **داشبورد** | `GoalsAndHabitsCombinedScreen.kt`, `LifeOsRepository.kt`, `HomeDashboardScreen.kt` | تست واحد | **پایین** — در حال حاضر در زیربخش سوم `GOALS_AND_HABITS` قرار دارد و خلاصه آن در داشبورد نمایش داده می‌شود |
| ۱۰ | **داشبورد وضعیت امروز (Executive Status Dashboard)** | پیاده‌سازی‌شده در `HomeDashboardScreen.kt`؛ نیاز به تکمیل قابلیت مخفی/نمایش کارت‌ها، تفکیک داده ناموجود از صفر، و نمایش هشدارهای اولویت‌دار بدون تکرار دارد | بخش `HOME` | بخش ۳: **داشبورد** | `HomeDashboardScreen.kt`, `UserPreferencesDoc` (`dashboardCardOrder`) | تست Robolectric | **پایین** — استفاده از `dashboardCardOrder` موجود در `UserPreferencesDoc` برای مخفی/نمایش کارت‌ها |
| ۱۱ | **دستیار هوشمند فارسی و ورودی صوتی آفلاین/آنلاین** | پیاده‌سازی‌شده (`AiAssistantScreen`, `VoiceCaptureSheet`, `GeminiAssistantService`, `PersianOfflineSpeechHelper`) با پیش‌نمایش و تأیید عملیات مالی | بخش `AI_ASSISTANT` + دکمه شناور `voice_capture_fab` + کاشی سریع `QuickVoiceTileService` | بخش ۴: **AI** (پرسش تکمیلی در ابهام، تحلیل واقعی، عدم اجرای مستقیم بدون تأیید، ضدتکرار در retry) | `GeminiAssistantService.kt`, `VoiceCaptureSheet.kt`, `HabitsAndAssistantScreens.kt`, `LifeOsViewModel.kt` | دارای تست در `ExampleRobolectricTest.kt` | **پایین** — ارتقای تشخیص ابهام و تحلیل سوالات مالی/مسیر بر اساس داده‌های واقعی |
| ۱۲ | **سیستم یادآوری و زمان‌بندی (Unified Reminder Engine)** | یادآور روزانه هزینه (`scheduleDailyExpenseReminder`)، زمان‌بند عمومی (`scheduleReminder`)، Snooze ده دقیقه‌ای و Sync شش‌ساعته در `ReminderReceiver.kt` موجود است؛ نیاز به موتور یادآوری واحد برای بدهی/قسط، تسک، عادت، هدف و سلامت با لغو خودکار پس از تکمیل/تسویه دارد | `ReminderReceiver.kt`, `ReminderScheduler` | **موتور یادآوری واحد** (`ReminderScheduler` / `UnifiedReminderEngine`) متصل به مالی، مسیر، اهداف و سلامت | `ReminderReceiver.kt`, `LifeOsViewModel.kt`, `LifeOsRepository.kt` | نیاز به تست تغییر موعد، تکمیل کار، تسویه بدهی و Snooze | **متوسط** — یکپارچه‌سازی شناسه‌های یکتا (`notificationId` مبتنی بر نوع و شناسه موجودیت) و لغو خودکار هنگام تکمیل تسک یا تسویه بدهی |
| ۱۳ | **همگام‌سازی Local-First (Room + Firestore) و بکاپ AES-256-GCM** | پیاده‌سازی‌شده کامل با صف آفلاین `SyncQueueEntity` (شامل `DELETE` tombstone)، مهاجرت حساب مهمان به Google Sign-In و خروجی JSON/AES-256-GCM | `LifeOsRepository.kt`, `LifeOsDatabase.kt`, `SettingsScreen` | زیرساخت داده مشترک تمام چهار بخش | `LifeOsRepository.kt`, `LifeOsDatabase.kt`, `PreferencesDataStore.kt`, `WorldClassOsUtils.kt` | دارای تست در `ExampleRobolectricTest.kt` | **پایین** — حفظ کامل سازگاری با نسخه ۵ دیتابیس و افزودن Migration رسمی در صورت ارتقا به نسخه ۶ |

---

## ۲. بررسی معماری، دیتابیس و نقاط اتصال حساس

1. **وضعیت دیتابیس محلی (Room):**
   - فایل: `app/src/main/java/com/example/data/local/LifeOsDatabase.kt`
   - نسخه فعلی: `version = 5`
   - جداول موجود: `cached_tasks`, `cached_consumption`, `cached_custom_items`, `cached_expenses`, `cached_habits`, `cached_habit_records`, `cached_sleep_records`, `cached_exercise_records`, `cached_activity_events`, `cached_conversations`, `sync_queue`, `tasks`.
   - مهاجرت‌های موجود: `MIGRATION_2_3`, `MIGRATION_3_4`.
   - **الزام نقشه راه:** هرگونه تغییر در ستون‌های جداول (مثلاً برای اتصال بدهی و تسک یا پرداخت جزئی) باید با `MIGRATION_5_6` صریح و غیرمخرب انجام شود و داده‌های قبلی کاملاً حفظ شوند.

2. **وضعیت همگام‌سازی ابری و قوانین امنیتی (Firestore):**
   - فایل قوانین: `firestore.rules` (Zero-Trust مبتنی بر مالکیت `isOwner(userId)` و اعتبارسنجی دقیق فیلدها).
   - فایل ریپازیتوری: `app/src/main/java/com/example/data/repository/LifeOsRepository.kt`.
   - **الزام نقشه راه:** فیلدهای ارسالی در `toCreateMap()` باید با `firestore.rules` سازگار بمانند تا خطای `PERMISSION_DENIED` رخ ندهد.

3. **وضعیت ناوبری و Deep Linkها:**
   - اکشن‌های ورودی به `MainActivity`:
     - `QuickVoiceTileService.ACTION_QUICK_VOICE` ← باز کردن شیت ثبت صوتی.
     - `ReminderReceiver.ACTION_OPEN_EXPENSE_LOG` ← هدایت مستقیم به بخش مالی (`DAILY_LOG`).
   - مسیرهای داخلی:
     - `DAILY_LOG` (مالی)، `GOALS_AND_HABITS` (اهداف و مسیر با زیرتب‌های `0`, `1`, `2`)، `HOME` (داشبورد)، `AI_ASSISTANT` (AI)، `REPORTS` (گزارش‌ها)، `HISTORY` (تاریخچه)، `SETTINGS` (تنظیمات).
