# گزارش پیشرفت، ممیزی مهندسی و وضعیت واقعی پروژه (`ROADMAP_PROGRESS.md`)

**مخزن:** `Personal Life OS` (`https://github.com/asisskpggame-create/ap` — شاخه `main`)  
**نسخه رسمی:** `v1.5.0` (`versionCode = 6`)  
**شناسه بسته:** `com.aistudio.personallifeos.kxmpzq`  
**پشته فنی واقعی:** Android Gradle Plugin `9.0.0`, Gradle `9.3.1`, Kotlin `2.2.10`, KSP `2.3.2`, `compileSdk = 36`, `minSdk = 24`, Room `2.7.0` (`version = 6`), DataStore Preferences `1.1.3`, Firebase Auth + Firestore (`33.10.0`).  
**وضعیت بیلد (`compile_applet`):** `PASS`  
**وضعیت تست‌های واحد و یکپارچه (`gradle testDebugUnitTest`):** `51/51 PASSED, 0 FAILED` (`ExampleUnitTest`: 11, `ExampleRobolectricTest`: 13, `BankSmsEngineAndAccountingTest`: 9, `BankSmsEngineAuditTest`: 6, `TaskDaoPersianTest`: 8, `TasksViewModelTest`: 4؛ به‌علاوه ۳ تست `LifeOsRepositoryRuleTest` که ویژه شبیه‌ساز محلی Firestore بوده و در محیط بدون امولاتور `SKIPPED` می‌شوند)  

---

## ۱. طبقه‌بندی شفاف وضعیت پیاده‌سازی و تست (طبق بخش ۸ دستورالعمل مهندسی)

| سطح وضعیت | توضیح و مصداق در مخزن فعلی |
|---|---|
| `IMPLEMENTED` | پیاده‌سازی کامل در سورس کد بدون معماری موازی (`com.example.sms.*`, `LifeOsRepository`, `LifeOsDatabase`, `DailyLogAndHealthScreen`, `SmartBankSmsAndScannerSheet`). |
| `UNIT_TESTED` | تست‌شده در JVM / Robolectric (`ExampleUnitTest`, `BankSmsEngineAndAccountingTest` برای پارسر پیامک، نرمال‌ساز، دقت `BigDecimal` و تجمیع بدهی/طلب). |
| `INTEGRATION_TESTED` | تست‌شده با دیتابیس واقعی SQLite/Room در Robolectric (`ExampleRobolectricTest` و `BankSmsEngineAndAccountingTest` برای مهاجرت `v1 -> v6`، ضدتکرار دائمی `idempotencyKey` در دیتابیس، و زنجیره پرداخت اقساط بدهی). |
| `NOT_IMPLEMENTED` | دریافت خودکار پس‌زمینه پیامک از طریق `BroadcastReceiver` (`SMS_RECEIVED`). مجوز بلااستفاده `RECEIVE_SMS` از `AndroidManifest.xml` حذف شد؛ برنامه صرفاً از روش **Paste دستی** و **اسکن صریح صندوق ورودی (`READ_SMS` در صورت مجوز کاربر)** استفاده می‌کند. |
| `BLOCKED (نیازمند دستگاه فیزیکی)` | `DEVICE_TESTED` و `E2E_VERIFIED` روی دستگاه فیزیکی اندروید یا شبیه‌ساز دارای Google Play Services و سیم‌کارت واقعی (به دلیل اجرای در محیط لینوکس ابری بدون ADB متصل). |

---

## ۲. یافته‌های ممیزی فاز A و اصلاحات قطعی اعمال‌شده در فاز B

### ۱. حذف معماری موازی در تشخیص پیامک بانکی (`P0` — برطرف شد)
- **مشکل قبلی:** سه پیاده‌سازی مجزا برای پارس پیامک وجود داشت (`com.example.sms.BankSmsParser`، منطق regex تکراری در `com.example.util.BankSmsScannerUtility`، و فیلتر دستی در `DailyLogAndHealthScreen.readBankSmsInbox`).
- **اصلاح انجام‌شده:**
  - `com.example.sms.BankSmsParser` به عنوان **تنها مرجع حقیقت (Single Source of Truth)** تثبیت شد.
  - `BankSmsScannerUtility.parseBankSms`، `IranianBankSmsParser.parseBankSms` و `DailyLogAndHealthScreen.readBankSmsInbox` همگی مستقیماً به `com.example.sms.BankSmsParser` متصل شدند.

### ۲. مدیریت ابهام واحد پول و سرشماره‌های مشترک (`P0 / P1` — برطرف شد)
- **مشکل قبلی:** پیامک‌های فاقد واحد پول (`ریال` / `تومان`) به‌طور پیش‌فرض ریال فرض شده و تقسیم بر ۱۰ می‌شدند؛ همچنین سرشماره مشترک `982000` (مشترک میان ملت، ملی و سامان) می‌توانست بدون بررسی متن پیامک به اولین بانک لیست نسبت داده شود.
- **اصلاح انجام‌شده:**
  - اولویت تشخیص بانک ابتدا با **نام بانک در متن پیامک** و سپس سرشماره فرستنده است. در صورت تطابق صرفاً با سرشماره مشترک `982000` بدون نام بانک در متن، وضعیت `NEEDS_REVIEW` و فیلد نامطمئن `bankName` ثبت می‌شود.
  - در صورت نبود واحد پول در پیامک و عدم وجود واحد پیش‌فرض صریح برای بانک، فیلد `detectedCurrency = "UNKNOWN"`، وضعیت `NEEDS_REVIEW` و هشدار صریح در رابط کاربری نمایش داده می‌شود و هیچ تقسیم بر ۱۰ فرضی انجام نمی‌گیرد.
  - الگوهای جداکننده اعداد برای پشتیبانی از ممیز و اسلش هزارگان (`2/500/000`، `2.500.000`، `1,250.50`) و حذف تاریخ/ساعت/شماره کارت/کد پیگیری پیش از استخراج مبلغ ارتقا یافتند.

### ۳. ضدتکرار دائمی (`Persistent Idempotency`) در دیتابیس Room (`P0` — برطرف شد)
- **مشکل قبلی:** `LifeOsRepository.addExpense` فقط نقشه حافظه موقت (`recentIdempotencyKeys`) را بررسی می‌کرد و پس از بستن برنامه یا ساخت نمونه جدید از Repository، امکان ثبت مجدد همان پیامک وجود داشت. همچنین کلید ضدتکرار از شیت پیامک بانکی به `ViewModel` ارسال نمی‌شد.
- **اصلاح انجام‌شده:**
  - کوئری `@Query("SELECT * FROM cached_expenses WHERE userId = :userId AND idempotencyKey = :idempotencyKey AND idempotencyKey != '' LIMIT 1")` به `LifeOsDao` اضافه شد.
  - متد `LifeOsRepository.addExpense` پیش از درج رکورد جدید، وجود `idempotencyKey` را در جدول `cached_expenses` بررسی می‌کند.
  - اثرانگشت SHA-256 پیامک (`fingerprint`) از `SmartBankSmsParserModalSheet` و کارت صندوق پیامک در `DailyLogAndHealthScreen` از طریق `MainActivity` و `LifeOsViewModel.addExpense` تا دیتابیس منتقل شد.

### ۴. اصلاح محاسبه مانده بدهی و طلب پس از پرداخت جزئی (`P1` — برطرف شد)
- **مشکل قبلی:** کارت خلاصه «تراز مطالبات و بدهی‌ها» در `DailyLogAndHealthScreen.kt` (خطوط ۲۰۹۶ و ۲۰۹۹) به جای `remainingAmount`، مبلغ اولیه (`amount`) را جمع می‌زد و پرداخت‌های جزئی (`paidAmount`) در جمع کل کارت منعکس نمی‌شدند.
- **اصلاح انجام‌شده:** محاسبه `totalReceivableMap` و `totalDebtMap` به توابع استاندارد `PersianDateAndUnitUtils.aggregateReceivablesByCurrency` و `aggregateDebtsByCurrency` (مبتنی بر `remainingAmount` و `BigDecimal`) متصل شد.

### ۵. دقت حسابداری `BigDecimal` در تمام محاسبات مالی (`P0` — برطرف شد)
- تمامی توابع جمع هزینه، درآمد، بدهی، طلب، تراز خالص، مانده هر حساب (`aggregateAccountBalances`) و `ExpenseEntry.remainingAmount` با `BigDecimal` و `RoundingMode.HALF_UP` (دو رقم اعشار) پیاده‌سازی و تست شدند.

### ۶. حذف مجوز بلااستفاده `RECEIVE_SMS` و بهبود پایپ‌لاین CI (`P1` — برطرف شد)
- مجوز `android.permission.RECEIVE_SMS` از `AndroidManifest.xml` حذف شد تا اصل حداقل دسترسی رعایت شود.
- در فایل `.github/workflows/android-apk.yml`، گام اجرای تست‌های واحد (`gradle testDebugUnitTest --stacktrace`) پیش از ساخت APK اضافه شد و نام‌گذاری خروجی‌ها با نسخه `v1.5.0` (`versionCode = 6`) همگام گردید.

---

## ۳. ماتریس کامل تست‌های اجراشده (`21/21 PASS`)

| ردیف | کلاس تست | نام تست | سطح تست | وضعیت |
|---|---|---|---|---|
| ۱ | `ExampleUnitTest` | `addition_isCorrect` | `UNIT_TESTED` | `PASS` |
| ۲ | `ExampleRobolectricTest` | `testAppSectionTabSelectionAndNavigationRail` | `INTEGRATION_TESTED` | `PASS` |
| ۳ | `ExampleRobolectricTest` | `testEncryptedBackupRoundTrip` | `INTEGRATION_TESTED` | `PASS` |
| ۴ | `ExampleRobolectricTest` | `testPersianColloquialSpeechParsingAndCurrencyConversion` | `UNIT_TESTED` | `PASS` |
| ۵ | `ExampleRobolectricTest` | `testPhase2DebtRepaymentPlanAndTransfer` | `INTEGRATION_TESTED` | `PASS` |
| ۶ | `ExampleRobolectricTest` | `testPhase3UnifiedReminderEngine` | `INTEGRATION_TESTED` | `PASS` |
| ۷ | `ExampleRobolectricTest` | `testPhase4And5DashboardAndAiGuardrails` | `INTEGRATION_TESTED` | `PASS` |
| ۸ | `ExampleRobolectricTest` | `testAuditSuitePartA_CurrencyUnitsAndP0Multipliers` | `UNIT_TESTED` | `PASS` |
| ۹ | `ExampleRobolectricTest` | `testAuditSuitePartB_ColloquialIntent32Sentences` | `UNIT_TESTED` | `PASS` |
| ۱۰ | `ExampleRobolectricTest` | `verify true schema migration from v1 to v6 preserves existing user data without data loss` | `INTEGRATION_TESTED` | `PASS` |
| ۱۱ | `ExampleRobolectricTest` | `verify Iranian bank SMS extensible parser normalizer and template registry across banks` | `UNIT_TESTED` | `PASS` |
| ۱۲ | `ExampleRobolectricTest` | `verify Persian Transaction Intent Engine across income expense transfer receivable and debt` | `UNIT_TESTED` | `PASS` |
| ۱۳ | `ExampleRobolectricTest` | `verify Jalali month names and exact typo fix for Ordibehesht` | `UNIT_TESTED` | `PASS` |
| ۱۴ | `BankSmsEngineAndAccountingTest` | `test slash and dot thousands separators and decimal amounts in BankSmsNormalizer` | `UNIT_TESTED` | `PASS` |
| ۱۵ | `BankSmsEngineAndAccountingTest` | `test date time card and reference tokens are never confused with transaction amount` | `UNIT_TESTED` | `PASS` |
| ۱۶ | `BankSmsEngineAndAccountingTest` | `test missing currency unit marks result as NEEDS_REVIEW with UNKNOWN detectedCurrency` | `UNIT_TESTED` | `PASS` |
| ۱۷ | `BankSmsEngineAndAccountingTest` | `test shared sender 982000 disambiguates by body first and marks sender-only match as NEEDS_REVIEW` | `UNIT_TESTED` | `PASS` |
| ۱۸ | `BankSmsEngineAndAccountingTest` | `test OTP failed balance-only and promotional SMS are strictly rejected` | `UNIT_TESTED` | `PASS` |
| ۱۹ | `BankSmsEngineAndAccountingTest` | `test refund and reversal SMS is classified as INCOME` | `UNIT_TESTED` | `PASS` |
| ۲۰ | `BankSmsEngineAndAccountingTest` | `test delegation parity between BankSmsParser BankSmsScannerUtility and IranianBankSmsParser` | `UNIT_TESTED` | `PASS` |
| ۲۱ | `BankSmsEngineAndAccountingTest` | `test BigDecimal financial precision and IOU remainingAmount after partial payments` & `test persistent Room idempotencyKey deduplicates SMS across new repository instances` | `INTEGRATION_TESTED` | `PASS` |
