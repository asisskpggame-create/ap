<div dir="rtl">

# 📋 تاریخچه تغییرات (Changelog) — Personal Life OS

تمامی تغییرات، بهبودهای ظاهری و رفع باگ‌های مهم پروژه در این فایل مستند می‌شوند.

---

## [v1.1.0] — نسخه رفع کرش Samsung Galaxy S24 Ultra و تکمیل تم Liquid Glass (`versionCode = 2`)

### 🛠️ رفع باگ‌ها و پایداری روی دستگاه‌های واقعی (S24 Ultra / One UI)
- **رفع کرش راه‌اندازی اولیه Firebase در بیلدهای مستقل گیت‌هاب:**
  - افزودن تابع `ensureFirebaseInitialized(context)` در `AppCheckAndAuth.kt` برای مقداردهی اولیه خودکار `FirebaseApp` حتی در صورت عدم وجود فایل `google-services.json`.
  - جایگزینی فراخوانی‌های مستقیم `Firebase.auth` در `MainActivity.kt` با `safeAuthStateFlow(context)` و `safeCurrentUser(context)`.
  - مقاوم‌سازی کامل `LifeOsRepository` برای کارکرد ۱۰۰٪ آفلاین/لوکال در صورت در دسترس نبودن سرویس‌های ابری.
- **رفع کرش شیدرهای گرافیکی `RadialGradient` روی رندرر Vulkan/Skia:**
  - افزودن شرط بررسی ابعاد (`canvasWidth <= 1f || canvasHeight <= 1f`) و حداقل شعاع `.coerceAtLeast(1f)` در `LiquidGlassBackdrop` (`Glassmorphism.kt`)، گوی سه‌بعدی هوش مصنوعی (`HomeDashboardScreen.kt`) و نمودار حلقه‌ای هزینه‌ها (`MonthlyVisualExpensesGlassCard.kt`).
- **رفع کرش `coerceIn` در نمایشگرهای تراکم بالای `xxxhdpi` (505 PPI):**
  - ایمن‌سازی بازه ارتفاع موج‌های صوتی در `VoiceCaptureSheet.kt` هنگام انیمیشن باز و بسته شدن شیت ضبط صدا.
- **محافظت از سرویس‌های سیستمی:**
  - قرار دادن `CredentialManager.create(context)` و `SpeechRecognizer.createSpeechRecognizer(context)` در بلوک‌های محافظت‌شده `try / catch (Throwable)`.

### 🎨 بهبودهای رابط کاربری (Liquid Glass Design System)
- یکپارچه‌سازی کامل تمام صفحات، کارت‌ها، شیت‌های پایینی (`GlassBottomSheetContainer`) و دیالوگ‌ها (`GlassDialogContainer`) با تم **Liquid Glass**.
- افزودن کامپوننت `MonthlyVisualExpensesGlassCard` برای نمایش بصری هزینه‌های ماهانه با نمودار حلقه‌ای شیشه‌ای.
- افزودن کامپوننت `HabitTrackingGlassModule` برای پیگیری زنجیره تداوم عادت‌ها.

### 🚀 زیرساخت انتشار و CI/CD
- ارتقای `versionCode` به `2` و `versionName` به `"1.1.0"` جهت پشتیبانی از آپدیت مستقیم روی گوشی.
- ارتقای ورک‌فلو `.github/workflows/android-apk.yml` برای تولید خودکار تنظیمات Firebase و انتشار هم‌زمان نسخه‌های Debug و Release در **GitHub Releases**.

---

## [v1.0.0] — انتشار اولیه Personal Life OS (`versionCode = 1`)
- پیاده‌سازی معماری **Local-First** با دیتابیس محلی `Room` و همگام‌سازی خودکار ۶ ساعته با `Cloud Firestore`.
- پیاده‌سازی کامل صفحات خانه، تسک‌ها، ثبت روزانه و سلامت (آب، کالری، خواب، ورزش، هزینه‌ها)، عادت‌ها، دستیار هوشمند Gemini و گزارش‌ها.
- پشتیبانی کامل از تقویم شمسی (جلالی)، اعداد فارسی، چیدمان راست‌به‌چپ (RTL) و پشتیبان‌گیری رمزنگاری‌شده `AES-256-GCM`.

</div>
