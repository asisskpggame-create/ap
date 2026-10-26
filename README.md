<div dir="rtl" align="center">

# 💎 Personal Life OS — Finance Pro & Liquid Glass Edition (v1.3.0)
### سیستم‌عامل جامع مدیریت مالی، اهداف و عادت‌ها، سلامت و دستیار هوشمند فارسی

[![Android CI/CD — Build & Release APK](https://img.shields.io/badge/GitHub_Actions-Auto_Release_APK-10B981?style=for-the-badge&logo=githubactions&logoColor=white)](./.github/workflows/android-apk.yml)
[![Platform](https://img.shields.io/badge/Android-SDK_24%E2%80%9336-38BDF8?style=for-the-badge&logo=android&logoColor=white)](#)
[![UI Theme](https://img.shields.io/badge/Design-Liquid_Glass_(visionOS_%2F_iOS_18)-A855F7?style=for-the-badge)](#طراحی-و-تم-liquid-glass)
[![S24 Ultra Ready](https://img.shields.io/badge/Galaxy_S24_Ultra-Tested_%26_Crash_Free-F59E0B?style=for-the-badge&logo=samsung&logoColor=white)](#پایداری-و-رفع-کرش-روی-samsung-galaxy-s24-ultra-نسخه-v110)

</div>

---

<div dir="rtl">

## 📖 معرفی پروژه

**Personal Life OS** یک اپلیکیشن بومی اندروید (نوشته‌شده با **Kotlin** و **Jetpack Compose**) برای مدیریت یکپارچه امور مالی، اهداف و عادت‌ها، سلامت روزانه و دستیار هوشمند با پشتیبانی کامل از **زبان فارسی، تقویم جلالی (شمسی)، اعداد فارسی و واحد پولی تومان (و ارزهای بین‌المللی)** است.

این برنامه با معماری **Local-First (اول آفلاین)** طراحی شده است؛ یعنی بدون نیاز اجباری به اینترنت یا حساب کاربری، تمام اطلاعات شما روی حافظه امن گوشی (دیتابیس `Room` + پشتیبان رمزنگاری‌شده `AES-256-GCM`) ذخیره می‌شود و در صورت ورود با حساب گوگل، به‌صورت خودکار با **Firebase Cloud Firestore** همگام‌سازی می‌گردد.

---

## 📥 دانلود و نصب مستقیم فایل APK (از بخش Releases)

با هر بار Push شدن کد در مخزن گیت‌هاب، پایپ‌لاین خودکار **GitHub Actions** اجرا شده و دو نسخه APK در بخش **[Releases](../../releases)** منتشر می‌کند:

| نام فایل APK | کاربرد و ویژگی‌ها | پیشنهاد برای نصب |
| :--- | :--- | :--- |
| **`Personal-Life-OS-v1.3.0-FinancePro-Debug.apk`** | امضاشده با کلید اصلی پروژه (دارای گواهی SHA-1 ثبت‌شده در Firebase برای ورود با گوگل / Google Sign-In) | ⭐ **پیشنهادی برای نصب روزمره و تست کامل قابلیت‌های ابری** |
| **`Personal-Life-OS-v1.3.0-FinancePro-Release.apk`** | نسخه ریلیز امضاشده با کلید انتشار (`my-upload-key.jks`) برای توزیع عمومی | مناسب استفاده در حالت آفلاین/لوکال و انتشار |

### مراحل نصب روی گوشی (از جمله Samsung Galaxy S24 Ultra):
1. وارد بخش **[Releases](../../releases)** در همین مخزن گیت‌هاب شوید.
2. آخرین نسخه (`v1.3.x`) را باز کرده و فایل **`Personal-Life-OS-v1.3.0-FinancePro-Debug.apk`** را دانلود کنید.
3. فایل را روی گوشی باز کرده و گزینه **Install / Update** را بزنید (به دلیل ارتقای `versionCode = 4`، بدون نیاز به حذف نسخه قبلی مستقیماً روی آن نصب می‌شود).

---

## ✨ امکانات و ۵ بخش اصلی برنامه (نسخه `v1.3.0`)

### ۱. 💳 امور مالی پیشرفته — اولویت اول برنامه (`DailyLogAndHealthScreen`)
- **تفکیک حرفه‌ای در ۲ تب مجزا:**
  1. **«ورودی و خروجی‌ها»:** ثبت و پایش خرج‌ها و درآمدها در بازه‌های روزانه، هفتگی، ماهانه و سالانه.
  2. **«طلب و بدهی»:** مدیریت اختصاصی طلبکاری‌ها و بدهکاری‌ها به همراه دکمه تسویه سریع (`تسویه شد ✓`).
- **اتصال زنده به پیامک‌های بانکی (`Auto-SMS Inbox Reader`):** خواندن مستقیم پیامک‌های بانکی گوشی (`READ_SMS`)، تشخیص خودکار ۱۷ بانک ایرانی، تبدیل ریال به تومان، استخراج **مانده حساب**، تشخیص هوشمند پذیرنده/دسته‌بندی و ثبت ۱-لمسی.
- **دکمه‌های ثبت سریع ۱-لمسی (`1-Tap Presets`):** تعریف، ویرایش نام و قیمت و حذف دکمه‌های آماده برای مخارج پرتکرار (قهوه، اسنپ، مترو و...).
- **مدیریت دسته‌بندی‌های سفارشی خرج و مخارج:** امکان تعریف دسته‌بندی‌های جدید همراه با آیکون اختصاصی.
- **موتور بودجه امن روزانه (`Safe-to-Spend`) + پشتیبانی چندارزی (تومان، دلار، یورو، درهم، ریال).**
- **نمودار خطی روند ۳۰ روزه خرج و درآمد (`MonthlySpendingTrendsLineChart`) + نمودار حلقه‌ای سهم دسته‌بندی‌ها (`MonthlyVisualExpensesGlassCard`) + کارت تراز دارایی خالص (`Net Worth`).**
- **مدیریت کامل تراکنش‌ها:** جستجوی زنده، ویرایش تراکنش‌های ثبت‌شده، **خروجی اکسل (`CSV`)** و **کپی گزارش متنی**.

### ۲. 📊 مرکز «نمودار و روند» (`HomeDashboardScreen`)
- **داشبورد تحلیلی و بصری:** نمایش متمرکز شاخص‌های عملکرد، نمودار روند مالی، نقشه حرارتی تقویم شمسی و وضعیت پیشرفت اهداف و سلامت بدون شلوغی فرم‌های ورودی.
- **کارت‌های ناوبری هوشمند:** دارای دکمه‌های انتقال سریع در پایین هر کارت برای ورود مستقیم به تب تخصصی مربوطه (**امور مالی**، **اهداف و عادت‌ها**، **سلامت و کالری**).

### ۳. 🎯 اهداف و عادت‌ها (`TasksScreen`)
- **ساختار یکپارچه دوگانه:** تفکیک تمیز بین **«اهداف بلندمدت و تسک‌ها»** و **«عادت‌های روتین روزانه»**.
- **مدیریت تسک‌ها و تایمر فوکوس:** پشتیبانی از اولویت‌ها، تسک‌های تکرارشونده، تایمر پومودورو و یادآورهای نوتیفیکیشن با قابلیت Snooze.
- **زنجیره تداوم عادت‌ها (`Habit Streak`):** پیگیری عادت‌های روزانه و نمایش ماتریس ۷ روزه پایداری.

### ۴. 🍎 سلامت، کالری، آب، خواب و ورزش (`HabitsScreen`)
- **پایش آب و هیدراتاسیون با موج زنده (`AnimatedWaterWaveBox`):** ثبت سریع مصرف آب روزانه و هدف‌گذاری.
- **تغذیه و کالری‌شمار + اسکنر تصویری غذا با هوش مصنوعی.**
- **ثبت خواب شبانه (`SleepRecords`) و فعالیت‌های ورزشی (`ExerciseRecords`).**

### ۵. 🎙️ دستیار هوشمند و ثبت صوتی فارسی (`AiAssistantScreen` & `VoiceCaptureSheet`)
- **تبدیل گفتار به نوشتار زنده (`Speech-to-Text`) + درک اعداد و مبالغ محاوره‌ای تومان** (مثلاً «۵۰۰ تومن» معادل `۵۰۰,۰۰۰ تومان`).
- **پیش‌نمایش عملیات استخراج‌شده (`StructuredOperationPreviewCard`)** برای تایید نهایی قبل از ثبت در دیتابیس.

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
