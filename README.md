<div dir="rtl" align="center">

# 💎 Personal Life OS — Liquid Glass Edition (v1.1.0)
### سیستم‌عامل جامع مدیریت زندگی شخصی، سلامت، مالی و دستیار هوشمند فارسی

[![Android CI/CD — Build & Release APK](https://img.shields.io/badge/GitHub_Actions-Auto_Release_APK-10B981?style=for-the-badge&logo=githubactions&logoColor=white)](./.github/workflows/android-apk.yml)
[![Platform](https://img.shields.io/badge/Android-SDK_24%E2%80%9336-38BDF8?style=for-the-badge&logo=android&logoColor=white)](#)
[![UI Theme](https://img.shields.io/badge/Design-Liquid_Glass_(visionOS_%2F_iOS_18)-A855F7?style=for-the-badge)](#طراحی-و-تم-liquid-glass)
[![S24 Ultra Ready](https://img.shields.io/badge/Galaxy_S24_Ultra-Tested_%26_Crash_Free-F59E0B?style=for-the-badge&logo=samsung&logoColor=white)](#پایداری-و-رفع-کرش-روی-samsung-galaxy-s24-ultra-نسخه-v110)

</div>

---

<div dir="rtl">

## 📖 معرفی پروژه

**Personal Life OS** یک اپلیکیشن بومی اندروید (نوشته‌شده با **Kotlin** و **Jetpack Compose**) برای مدیریت یکپارچه تمام ابعاد زندگی روزانه با پشتیبانی کامل از **زبان فارسی، تقویم جلالی (شمسی)، اعداد فارسی و واحد پولی تومان** است.

این برنامه با معماری **Local-First (اول آفلاین)** طراحی شده است؛ یعنی بدون نیاز اجباری به اینترنت یا حساب کاربری، تمام اطلاعات شما روی حافظه امن گوشی (دیتابیس `Room` + پشتیبان رمزنگاری‌شده `AES-256-GCM`) ذخیره می‌شود و در صورت ورود با حساب گوگل، به‌صورت خودکار با **Firebase Cloud Firestore** همگام‌سازی می‌گردد.

---

## 📥 دانلود و نصب مستقیم فایل APK (از بخش Releases)

با هر بار Push شدن کد در مخزن گیت‌هاب، پایپ‌لاین خودکار **GitHub Actions** اجرا شده و دو نسخه APK در بخش **[Releases](../../releases)** منتشر می‌کند:

| نام فایل APK | کاربرد و ویژگی‌ها | پیشنهاد برای نصب |
| :--- | :--- | :--- |
| **`Personal-Life-OS-v1.1.0-S24Ultra-Fixed-Debug.apk`** | امضاشده با کلید اصلی پروژه (دارای گواهی SHA-1 ثبت‌شده در Firebase برای ورود با گوگل / Google Sign-In) | ⭐ **پیشنهادی برای نصب روزمره و تست کامل قابلیت‌های ابری** |
| **`Personal-Life-OS-v1.1.0-S24Ultra-Fixed-Release.apk`** | نسخه ریلیز امضاشده با کلید انتشار (`my-upload-key.jks`) برای توزیع عمومی | مناسب استفاده در حالت آفلاین/لوکال و انتشار |

### مراحل نصب روی گوشی (از جمله Samsung Galaxy S24 Ultra):
1. وارد بخش **[Releases](../../releases)** در همین مخزن گیت‌هاب شوید.
2. آخرین نسخه (`v1.1.x`) را باز کرده و فایل **`Personal-Life-OS-v1.1.0-S24Ultra-Fixed-Debug.apk`** را دانلود کنید.
3. فایل را روی گوشی باز کرده و گزینه **Install / Update** را بزنید (به دلیل ارتقای `versionCode = 2`، بدون نیاز به حذف نسخه قبلی روی آن نصب می‌شود).

---

## ✨ امکانات و ماژول‌های اصلی برنامه

### ۱. 🏠 داشبورد هوشمند خانه (`HomeDashboardScreen`)
- **کارت قهرمان شیشه‌ای (3D AI Sphere Hero Card):** نمایش وضعیت کلی روز، درصد بهره‌وری، و گوی نوری سه‌بعدی رندرشده با شیدرهای گرافیکی `Canvas`.
- **نوار ثبت سریع یک‌لمسی (1-Tap Quick Actions):** ثبت فوری ۱ لیوان آب (۲۵۰ میلی‌لیتر)، ثبت قهوه، ثبت تسک جدید، ثبت هزینه و باز کردن دستیار صوتی.
- **کارت بودجه امن روزانه (Safe-to-Spend Engine):** محاسبه هوشمند سقف مجاز خرج روزانه بر اساس بودجه ماهانه، روزهای باقی‌مانده از ماه شمسی و هزینه‌های ثبت‌شده امروز.
- **نمودار حلقه‌ای هزینه‌های ماهانه (`MonthlyVisualExpensesGlassCard`):** تفکیک بصری هزینه‌های ماه جاری بر اساس دسته‌بندی با انیمیشن نرم و نمایش سهم هر دسته.
- **خلاصه سلامت روزانه:** نمایش پیشرفت مصرف آب، ساعت خواب شب گذشته و دقیقه ورزش روزانه در قالب کارت‌های شیشه‌ای.

### ۲. ✅ مدیریت پیشرفته تسک‌ها و تایمر فوکوس (`TasksScreen`)
- **سطوح اولویت و وضعیت:** پشتیبانی از اولویت‌های `فوری (URGENT)`، `بالا (HIGH)`، `متوسط (MEDIUM)` و `پایین (LOW)` و وضعیت‌های `در انتظار`، `در حال انجام` و `انجام‌شده`.
- **تایمر فوکوس و پومودورو:** امکان شروع تایمر تمرکز روی هر تسک و ثبت خودکار زمان واقعی صرف‌شده (`actualDurationMinutes`).
- **تسک‌های تکرارشونده (Recurring Tasks):** تنظیم تکرار خودکار روزانه، هفتگی یا بازه دلخواه.
- **یادآورهای هوشمند (`ReminderScheduler`):** تنظیم آلارم و نوتیفیکیشن با قابلیت **تعویق ۱۰ دقیقه‌ای (Snooze)** و **رد کردن (Dismiss)** مستقیم از روی نوتیفیکیشن.

### ۳. 🍎 ثبت روزانه، سلامت، تغذیه و مالی (`DailyLogAndHealthScreen`)
- **تغذیه و کالری‌شمار:** ثبت وعده‌های غذایی (صبحانه، ناهار، شام، میان‌وعده) همراه با محاسبه کالری، پروتئین، کربوهیدرات و چربی.
- **کاتالوگ آیتم‌های سفارشی (`CustomItems`):** امکان تعریف خوراکی‌ها و نوشیدنی‌های دلخواه با مقادیر تغذیه‌ای اختصاصی برای ثبت یک‌لمسی.
- **پایش آب هیدراتاسیون:** ثبت سریع آب مصرفی با دکمه‌های آماده (+۲۵۰، +۵۰۰ میلی‌لیتر) و نوار پیشرفت هدف روزانه.
- **ثبت خواب و ریکاوری (`SleepRecords`):** ثبت ساعت خواب، بیداری، مدت زمان و امتیاز کیفیت خواب (۱ تا ۵ ستاره).
- **ثبت فعالیت ورزشی (`ExerciseRecords`):** ثبت نوع ورزش، مدت زمان (دقیقه)، شدت تمرین و کالری سوزانده‌شده.
- **مدیریت هزینه‌ها (`ExpenseEntries`):** ثبت مبالغ به تومان در دسته‌بندی‌های مختلف (خوراک، حمل‌ونقل، قبوض، خرید، سلامت، تفریح و...).

### ۴. 🔄 عادت‌ها و زنجیره تداوم (`HabitsScreen` & `HabitTrackingGlassModule`)
- ساخت عادت‌های روزانه و هفتگی با رنگ و آیکون اختصاصی.
- محاسبه خودکار **زنجیره تداوم فعلی (Current Streak)** و **بهترین رکورد (Best Streak)**.
- نمایش ماتریس ۷ روز گذشته برای بررسی پایداری عادت‌ها در یک نگاه.

### ۵. 🎙️ دستیار هوشمند و ثبت صوتی فارسی (`AiAssistantScreen` & `VoiceCaptureSheet`)
- **تبدیل گفتار به نوشتار زنده (Real-time Speech-to-Text):** پشتیبانی از گفتار فارسی با نمایش موج صوتی زنده (`Waveform Canvas`).
- **موتور تحلیل هوشمند دستورات فارسی:** تشخیص خودکار منظور کاربر از متن یا صدای ضبط‌شده:
  - مثال: *«امروز ۱۵۰ هزار تومان بابت ناهار هزینه کردم»* ← استخراج خودکار به عنوان **هزینه (۱۵۰,۰۰۰ تومان - خوراک)**.
  - مثال: *«فردا ساعت ۱۰ صبح جلسه پروژه دارم»* ← استخراج خودکار به عنوان **تسک جدید با زمان سررسید**.
  - مثال: *«۲ لیوان آب خوردم و ۴۵ دقیقه دویدم»* ← استخراج هم‌زمان **مصرف آب** و **فعالیت ورزشی**.
- **پیش‌نمایش قبل از ثبت (`StructuredOperationPreviewCard`):** نمایش کارت‌های شیشه‌ای عملیات استخراج‌شده برای تایید نهایی کاربر قبل از ذخیره در دیتابیس.

### ۶. 📊 تاریخچه، گزارش‌های تحلیلی و تنظیمات (`HistoryReportsSettingsScreens`)
- **تایم‌لاین فعالیت‌ها (`HistoryScreen`):** ثبت دقیق تمام رویدادهای سیستم (`ActivityEvents`) با برچسب منبع ورودی (`MANUAL`, `VOICE`, `AI_PARSED`, `SYSTEM_6H`).
- **گزارش‌های تحلیلی (`ReportsScreen`):** آمار جامع عملکرد هفتگی و ماهانه در حوزه‌های بهره‌وری، تغذیه، سلامت و مالی.
- **تنظیمات و پشتیبان‌گیری (`SettingsScreen`):**
  - تغییر پالت رنگی نئونی شیشه‌ای (`OBSIDIAN_NEON`, `EMERALD_AURORA`, `ROYAL_AMETHYST`, `SUNSET_CORAL`).
  - تنظیم اهداف روزانه (آب، کالری، خواب، ورزش و بودجه ماهانه).
  - خروجی و ورودی پشتیبان رمزنگاری‌شده (`AES-256-GCM`) و همگام‌سازی دستی/خودکار ۶ ساعته.

---

## 🎨 طراحی و تم Liquid Glass

رابط کاربری برنامه به‌صورت ۱۰۰٪ بر پایه سیستم طراحی **Multi-Layered Liquid Glass** (الهام‌گرفته از **Apple visionOS** و **iOS 18**) پیاده‌سازی شده است:

- **`LiquidGlassBackdrop`:** پس‌زمینه عمیق ابسیدین (`#050811`) همراه با ۴ گوی نوری حجمی (`Volumetric Radial Glow Orbs`) که در لایه پشتی رندر می‌شوند.
- **`LiquidGlassCard` و `GlassSurface`:** کارت‌های شیشه‌ای نیمه‌شفاف با گرادیان شکست نور (`Refractive Tint`)، هایلایت براق بالایی (`Top Specular Sheen`) و حاشیه سه‌بعدی نوری (`1.dp Specular Border`).
- **`LiquidGlassPill`:** کپسول‌های شیشه‌ای برای تب‌ها، فیلترها، وضعیت‌ها و دکمه‌های سریع.
- **`GlassBottomSheetContainer` و `GlassDialogContainer`:** تمام شیت‌های پایینی (Bottom Sheets) و پنجره‌های گفتگو (Dialogs) دارای قاب شیشه‌ای یکپارچه با درخشش نئونی هستند.

---

## 🛡️ پایداری و رفع کرش روی Samsung Galaxy S24 Ultra (نسخه `v1.1.0`)

در نسخه **`v1.1.0` (`versionCode = 2`)** تمامی مشکلات و کرش‌های گزارش‌شده روی پرچم‌دار **Samsung Galaxy S24 Ultra** (با رابط کاربری One UI و نمایشگر با تراکم فوق‌العاده بالای `xxxhdpi` / 505 PPI) به‌صورت ریشه‌ای برطرف شده‌اند:

1. **مقداردهی اولیه ضدکرش Firebase (`ensureFirebaseInitialized`):**
   - در بیلدهای مستقل گیت‌هاب، حتی اگر فایل `google-services.json` در زمان اکسپورت حذف شده باشد، تابع `ensureFirebaseInitialized(context)` به‌طور خودکار `FirebaseApp` را با تنظیمات fallback مقداردهی اولیه می‌کند و جریان‌های `safeAuthStateFlow` و `safeCurrentUser` مانع از پرتاب خطای `IllegalStateException: Default FirebaseApp is not initialized` در بدو ورود به برنامه می‌شوند.
   - دیتابیس محلی (`LifeOsRepository`) به‌طور کامل در برابر عدم دسترسی به سرویس‌های ابری مقاوم‌سازی شده است.
2. **ایمن‌سازی شیدرهای گرافیکی `RadialGradient` روی رندرر سخت‌افزاری Vulkan/Skia:**
   - روی پردازنده گرافیکی S24 Ultra، پاس دادن شعاع `0f` به `Brush.radialGradient` در فریم اول انیمیشن (زمانی که ابعاد `Canvas` هنوز صفر است) باعث خطای `IllegalArgumentException: radius must be > 0` می‌شد.
   - در تمام کامپوننت‌های گرافیکی (`LiquidGlassBackdrop`، گوی سه‌بعدی `HomeDashboardScreen` و نمودار `MonthlyVisualExpensesGlassCard`) گارد بررسی ابعاد (`canvasWidth <= 1f`) و حداقل شعاع `.coerceAtLeast(1f)` اعمال شده است.
3. **ایمن‌سازی بازه `coerceIn` در موج صوتی (`VoiceCaptureSheet`):**
   - در نمایشگرهای `xxxhdpi` که `6.dp.toPx()` معادل ۲۴ پیکسل است، هنگام باز شدن انیمیشنی شیت صوتی، مقدار `minBarHeight` با `.coerceAtMost(size.height)` محدود شده تا هرگز خطای `Cannot coerce value to an empty range` رخ ندهد.
4. **محافظت از سرویس‌های سیستمی (`CredentialManager` و `SpeechRecognizer`):**
   - تمامی فراخوانی‌های سرویس‌های تشخیص گفتار و مدیریت اعتبار گوگل در بلوک‌های ایمن `try / catch (Throwable)` قرار گرفته‌اند.

---

## 🏗️ معماری فنی و ساختار پروژه

```text
├── .github/workflows/
│   └── android-apk.yml                 # پایپ‌لاین CI/CD ساخت خودکار APK و انتشار در GitHub Releases
├── app/src/main/java/com/example/
│   ├── MainActivity.kt                 # نقطه ورود اصلی، ناوبری شیشه‌ای و مدیریت وضعیت کاربر
│   ├── ai/
│   │   └── GeminiAssistantService.kt   # موتور هوش مصنوعی Gemini + تحلیلگر دستورات فارسی آفلاین/آنلاین
│   ├── data/
│   │   ├── firebase/
│   │   │   └── AppCheckAndAuth.kt      # راه‌اندازی ایمن Firebase، AppCheck و احراز هویت گوگل
│   │   ├── local/
│   │   │   ├── LifeOsDatabase.kt       # دیتابیس محلی Room، جداول (Entities) و DAO
│   │   │   └── PreferencesDataStore.kt # ذخیره تنظیمات، تم و اهداف روزانه با Jetpack DataStore
│   │   ├── model/
│   │   │   └── LifeOsModels.kt         # مدل‌های دامنه (Task, Expense, Habit, Sleep, Exercise, ...)
│   │   └── repository/
│   │       └── LifeOsRepository.kt     # مخزن داده Local-First + موتور همگام‌سازی ۶ ساعته ابری
│   ├── notifications/
│   │   ├── LifeOsQuickWidgetProvider.kt# ویجت صفحه اصلی اندروید
│   │   ├── QuickVoiceTileService.kt    # کاشی تنظیمات سریع (Quick Settings Tile) برای ثبت صوتی
│   │   └── ReminderReceiver.kt         # مدیریت آلارم‌ها، نوتیفیکیشن‌ها و سینک دوره‌ای ۶ ساعته
│   ├── ui/
│   │   ├── components/                 # کامپوننت‌های ماژولار شیشه‌ای (نمودار هزینه، ماژول عادت، EmptyState)
│   │   ├── screens/                    # صفحات اصلی برنامه (خانه، تسک‌ها، سلامت، عادت‌ها، هوش مصنوعی، گزارش‌ها)
│   │   ├── theme/                      # سیستم طراحی Liquid Glass (GlassTheme, Glassmorphism, Color, Type)
│   │   └── viewmodel/
│   │       └── LifeOsViewModel.kt      # مدیریت یکپارچه StateFlowها و منطق تجاری رابط کاربری
│   └── util/
│       ├── PersianDateAndUnitUtils.kt  # مبدل تاریخ شمسی (جلالی)، اعداد فارسی و فرمت مبالغ تومان
│       └── WorldClassOsUtils.kt        # موتور بودجه امن، پشتیبان رمزنگاری‌شده AES-256-GCM و NLP فارسی
└── firestore.rules                     # قوانین امنیتی Zero-Trust دیتابیس Cloud Firestore
```

---

## ⚙️ راهنمای بیلد محلی و تنظیم کلیدهای API

### پیش‌نیازها
- **JDK 17**
- **Android SDK 36**
- **Gradle 9.3.1**

### تنظیم کلید هوش مصنوعی Gemini (اختیاری)
برنامه دارای موتور تحلیلگر فارسی داخلی است و بدون کلید API نیز تمام دستورات صوتی و متنی را پردازش می‌کند. برای فعال‌سازی پاسخ‌های ابری **Gemini 2.5 Flash**:
1. یک فایل `.env` در ریشه پروژه بسازید (یا از `.env.example` کپی بگیرید):
   ```env
   GEMINI_API_KEY=YOUR_ACTUAL_GEMINI_API_KEY
   ```
2. برای بیلد کردن نسخه‌های Debug و Release:
   ```bash
   gradle assembleDebug assembleRelease
   ```
3. برای اجرای تست‌های واحد و Robolectric:
   ```bash
   gradle :app:testDebugUnitTest --tests "com.example.ExampleRobolectricTest"
   ```

</div>
