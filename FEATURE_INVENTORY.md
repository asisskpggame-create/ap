# فهرست جامع قابلیت‌ها و وضعیت واقعی پیاده‌سازی (`FEATURE_INVENTORY.md`)

**مخزن:** `Personal Life OS` (`com.example`)  
**نسخه کنونی:** `v1.5.0R` (`versionCode = 6`)  
**هدف سند:** ثبت دقیق وضعیت هر قابلیت بر اساس کد واقعی و تست‌های اجراشده، بدون ادعای اثبات‌نشده.

---

## ۱. جدول تطبیق قابلیت‌ها، محل در معماری و سطح واقعی تست

| ردیف | نام قابلیت | وضعیت پیاده‌سازی | سطح تست واقعی | کلاس‌ها و فایل‌های مسئول | توضیحات فنی و محدودیت‌های شفاف |
|---|---|---|---|---|---|
| ۱ | **ناوبری ۴ بخشی (مالی، اهداف و مسیر، داشبورد، AI)** | `IMPLEMENTED` | `INTEGRATION_TESTED` (Robolectric Compose) | `MainActivity.kt` (`AppSection`, `AnimatedGlassBottomBar`) | حفظ کامل کلیدهای مسیر (`DAILY_LOG`, `GOALS_AND_HABITS`, `HOME`, `AI_ASSISTANT`) |
| ۲ | **ثبت تراکنش مالی و دقت اعشاری `BigDecimal`** | `IMPLEMENTED` | `UNIT_TESTED` + `INTEGRATION_TESTED` | `PersianDateAndUnitUtils.kt`, `LifeOsModels.kt`, `LifeOsRepository.kt` | محاسبه با `BigDecimal` (`RoundingMode.HALF_UP`, scale 2)؛ تفکیک کامل `TOMAN`, `HEZAR_TOMAN`, `MILLION_TOMAN`, `IRR` بدون ضریب ناخواسته ۱۰۰۰ |
| ۳ | **حساب‌های مالی و انتقال بین حساب‌های شخصی (`TRANSFER`)** | `IMPLEMENTED` | `UNIT_TESTED` + `INTEGRATION_TESTED` | `PersianDateAndUnitUtils.kt` (`aggregateAccountBalances`, `aggregateNetBalanceByCurrency`) | عدم شمارش `TRANSFER` در درآمد یا هزینه خالص؛ محاسبه تراز هر حساب از روی تگ حساب |
| ۴ | **مدیریت بدهی، طلب، قسط‌بندی و پرداخت جزئی** | `IMPLEMENTED` | `UNIT_TESTED` + `INTEGRATION_TESTED` | `LifeOsRepository.kt`, `DailyLogAndHealthScreen.kt`, `LifeOsModels.kt` | اتصال بدهی به تسک‌های مسیر (`linkedDebtId`)، کسر دقیق `paidAmount` از `remainingAmount` در کارت خلاصه مطالبات و بدهی‌ها |
| ۵ | **موتور واحد پیامک بانکی ایران (`com.example.sms`)** | `IMPLEMENTED` | `UNIT_TESTED` + `INTEGRATION_TESTED` | `BankSmsNormalizer.kt`, `BankSmsPatternRegistry.kt`, `BankSmsParser.kt`, `BankSmsParseResult.kt`, `BankSmsScannerUtility.kt`, `SmartBankSmsAndScannerSheet.kt` | پشتیبانی از ۱۸+ بانک ایرانی، جداکننده‌های اسلش/نقطه/ویرگول، فیلتر قطعی OTP/ناموفق/مانده‌گیری/تبلیغاتی، مدیریت ابهام واحد پول (`NEEDS_REVIEW`) و سرشماره مشترک `982000` |
| ۶ | **ضدتکرار دائمی تراکنش‌های پیامک (`Persistent Idempotency`)** | `IMPLEMENTED` | `INTEGRATION_TESTED` (Room SQLite) | `LifeOsDatabase.kt` (`getExpenseByIdempotencyKey`), `LifeOsRepository.kt`, `LifeOsViewModel.kt` | ذخیره و بررسی `idempotencyKey` (اثرانگشت SHA-256) در جدول `cached_expenses`؛ مقاوم در برابر بستن و باز کردن مجدد برنامه |
| ۷ | **دریافت خودکار پیامک در پس‌زمینه (`BroadcastReceiver`)** | `NOT_IMPLEMENTED` | `NOT_TESTED` | `AndroidManifest.xml` | گیرنده پس‌زمینه (`SMS_RECEIVED`) پیاده‌سازی نشده و مجوز بلااستفاده `RECEIVE_SMS` حذف شد. ثبت پیامک از طریق **Paste دستی** و **اسکن صریح صندوق ورودی (`READ_SMS`)** انجام می‌شود |
| ۸ | **موتور تشخیص قصد تراکنش فارسی (`TransactionIntentEngine`)** | `IMPLEMENTED` | `UNIT_TESTED` (۳۲ سناریو) | `com.example.ai.TransactionIntentEngine` | تشخیص محلی و بلادرنگ `INCOME`, `EXPENSE`, `TRANSFER`, `RECEIVABLE`, `DEBT` و `AMBIGUOUS` |
| ۹ | **مهاجرت بدون حذف داده دیتابیس (`MIGRATION_1_2` تا `MIGRATION_5_6`)** | `IMPLEMENTED` | `INTEGRATION_TESTED` (SQLite واقعی) | `LifeOsDatabase.kt` | حذف کامل `fallbackToDestructiveMigration(true)` و حفظ ۱۰۰٪ داده‌های نسخه‌های ۱ تا ۶ |
| ۱۰ | **موتور یادآوری مشترک (`Unified Reminder Engine`)** | `IMPLEMENTED` | `INTEGRATION_TESTED` (Robolectric) | `ReminderReceiver.kt`, `ReminderScheduler` | کلید یکتای `(entityType, entityId)`، پشتیبانی از Snooze، تغییر موعد و لغو خودکار پس از تکمیل/تسویه |
| ۱۱ | **بکاپ محلی رمزنگاری‌شده (`AES-256-GCM`)** | `IMPLEMENTED` | `INTEGRATION_TESTED` (Round-Trip) | `LifeOsDatabase.kt` (`EncryptedBackupManager`) | رمزنگاری PBKDF2WithHmacSHA256 + AES/GCM/NoPadding و بازیابی کامل رکوردها |
| ۱۲ | **تبدیل گفتار به متن فارسی (`SpeechRecognizer`)** | `IMPLEMENTED` | `BLOCKED` (نیازمند دستگاه فیزیکی دارای بسته صوتی `fa-IR`) | `PersianSpeechController.kt`, `VoiceCaptureSheet.kt` | استفاده از `SpeechRecognizer` اندروید با `EXTRA_PREFER_OFFLINE`؛ کارکرد آفلاین نیازمند نصب بسته زبان فارسی در تنظیمات سیستم‌عامل گوشی است |
