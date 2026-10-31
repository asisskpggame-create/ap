package com.example.personallifeos.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@Database(
    entities = [
        TaskEntity::class,
        TopicEntity::class,
        ReminderEntity::class,
        HabitEntity::class,
        ExpenseEntity::class,
        DebtEntity::class,
        HealthLogEntity::class,
        VaultNoteEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun lifeOsDao(): LifeOsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private fun migrateToVersion7(db: SupportSQLiteDatabase) {
            safeExec(db, "ALTER TABLE tasks ADD COLUMN topicId TEXT")
            safeExec(db, "ALTER TABLE tasks ADD COLUMN description TEXT NOT NULL DEFAULT ''")
            safeExec(db, "ALTER TABLE tasks ADD COLUMN status TEXT NOT NULL DEFAULT 'TODO'")
            safeExec(db, "ALTER TABLE tasks ADD COLUMN subtasksSerialized TEXT NOT NULL DEFAULT ''")
            safeExec(db, "ALTER TABLE tasks ADD COLUMN reminderTime TEXT")
            safeExec(db, "ALTER TABLE tasks ADD COLUMN reminderEpochMillis INTEGER")
            safeExec(db, "ALTER TABLE tasks ADD COLUMN reminderRepeatMode TEXT NOT NULL DEFAULT 'ONCE'")
            safeExec(db, "ALTER TABLE tasks ADD COLUMN reminderBeforeMinutes INTEGER NOT NULL DEFAULT 0")
            safeExec(db, "ALTER TABLE tasks ADD COLUMN isMajorMission INTEGER NOT NULL DEFAULT 0")
            safeExec(db, "ALTER TABLE tasks ADD COLUMN completedAt TEXT")
            safeExec(db, "ALTER TABLE tasks ADD COLUMN activityLogSerialized TEXT NOT NULL DEFAULT ''")
            safeExec(db, "ALTER TABLE tasks ADD COLUMN linkedDebtId TEXT")

            safeExec(db, "ALTER TABLE expenses ADD COLUMN accountName TEXT DEFAULT 'حساب پیش‌فرض'")
            safeExec(db, "ALTER TABLE expenses ADD COLUMN idempotencyKey TEXT")
            safeExec(db, "ALTER TABLE expenses ADD COLUMN type TEXT NOT NULL DEFAULT 'EXPENSE'")

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `topics` (
                    `id` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `description` TEXT NOT NULL,
                    `iconEmoji` TEXT NOT NULL,
                    `colorHex` INTEGER NOT NULL,
                    `startDate` TEXT NOT NULL,
                    `endDate` TEXT,
                    `priority` TEXT NOT NULL,
                    `tagOrCategory` TEXT NOT NULL,
                    `isActive` INTEGER NOT NULL,
                    `isArchived` INTEGER NOT NULL,
                    `reminderEnabled` INTEGER NOT NULL,
                    `reminderTime` TEXT,
                    `topicNote` TEXT NOT NULL,
                    `createdAt` TEXT NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `reminders` (
                    `id` TEXT NOT NULL,
                    `entityType` TEXT NOT NULL,
                    `entityId` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `triggerTime` TEXT NOT NULL,
                    `isSnoozed` INTEGER NOT NULL,
                    `isCompleted` INTEGER NOT NULL,
                    `sensitiveAmountToman` INTEGER,
                    `topicId` TEXT,
                    `topicTitle` TEXT,
                    `repeatMode` TEXT NOT NULL,
                    `snoozeIntervalMinutes` INTEGER NOT NULL,
                    `triggerEpochMillis` INTEGER,
                    `isEnabled` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) = migrateToVersion7(db)
        }

        val MIGRATION_5_7 = object : Migration(5, 7) {
            override fun migrate(db: SupportSQLiteDatabase) = migrateToVersion7(db)
        }

        val MIGRATION_4_7 = object : Migration(4, 7) {
            override fun migrate(db: SupportSQLiteDatabase) = migrateToVersion7(db)
        }

        val MIGRATION_3_7 = object : Migration(3, 7) {
            override fun migrate(db: SupportSQLiteDatabase) = migrateToVersion7(db)
        }

        val MIGRATION_2_7 = object : Migration(2, 7) {
            override fun migrate(db: SupportSQLiteDatabase) = migrateToVersion7(db)
        }

        val MIGRATION_1_7 = object : Migration(1, 7) {
            override fun migrate(db: SupportSQLiteDatabase) = migrateToVersion7(db)
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) = migrateToVersion7(db)
        }

        val MIGRATION_2_4 = object : Migration(2, 4) {
            override fun migrate(db: SupportSQLiteDatabase) = migrateToVersion7(db)
        }

        val MIGRATION_1_4 = object : Migration(1, 4) {
            override fun migrate(db: SupportSQLiteDatabase) = migrateToVersion7(db)
        }

        private fun safeExec(db: SupportSQLiteDatabase, sql: String) {
            try {
                db.execSQL(sql)
            } catch (_: Exception) {
                // Column or table already exists
            }
        }

        private fun migrateLegacyDatabaseFileIfNeeded(context: Context) {
            try {
                val oldDbFile = context.getDatabasePath("personal_life_os_cache.db")
                val newDbFile = context.getDatabasePath("personal_life_os.db")
                if (oldDbFile.exists() && !newDbFile.exists()) {
                    newDbFile.parentFile?.mkdirs()
                    oldDbFile.copyTo(newDbFile, overwrite = true)

                    val oldWal = File(oldDbFile.parentFile, "personal_life_os_cache.db-wal")
                    val newWal = File(newDbFile.parentFile, "personal_life_os.db-wal")
                    if (oldWal.exists()) oldWal.copyTo(newWal, overwrite = true)

                    val oldShm = File(oldDbFile.parentFile, "personal_life_os_cache.db-shm")
                    val newShm = File(newDbFile.parentFile, "personal_life_os.db-shm")
                    if (oldShm.exists()) oldShm.copyTo(newShm, overwrite = true)

                    Log.i("AppDatabase", "Successfully migrated legacy personal_life_os_cache.db to personal_life_os.db")
                }
            } catch (e: Exception) {
                Log.e("AppDatabase", "Error migrating legacy database files: ${e.message}", e)
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                migrateLegacyDatabaseFileIfNeeded(context.applicationContext)
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "personal_life_os.db"
                )
                    .addMigrations(
                        MIGRATION_6_7,
                        MIGRATION_5_7,
                        MIGRATION_4_7,
                        MIGRATION_3_7,
                        MIGRATION_2_7,
                        MIGRATION_1_7,
                        MIGRATION_3_4,
                        MIGRATION_2_4,
                        MIGRATION_1_4
                    )
                    .addCallback(DatabaseSeedCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseSeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                val database = INSTANCE ?: return@launch
                val dao = database.lifeOsDao()

                // Seed Topics (Topics & Mission Management System)
                dao.insertTopics(
                    listOf(
                        TopicEntity(
                            id = "top_uni",
                            title = "دانشگاه و پایان‌نامه ارشد",
                            description = "مدیریت پروژه‌های درسی، مقالات پژوهشی و جلسات استاد راهنما",
                            iconEmoji = "SVG_UNIVERSITY",
                            colorHex = 0xFF06B6D4,
                            startDate = "۱۴۰۵/۰۷/۰۱",
                            endDate = "۱۴۰۵/۱۱/۳۰",
                            priority = "URGENT",
                            tagOrCategory = "تحصیلی و دانشگاه",
                            isActive = true,
                            isArchived = false,
                            reminderEnabled = true,
                            reminderTime = "هر روز • ساعت ۱۰:۰۰",
                            topicNote = "تمرکز اصلی این ترم روی فصل ۳ پایان‌نامه و شبیه‌سازی الگوریتم‌هاست.",
                            createdAt = "۱۴۰۵/۰۸/۰۱"
                        ),
                        TopicEntity(
                            id = "top_film",
                            title = "پروژه‌های فیلم‌برداری و مستند",
                            description = "پیش‌تولید، آفیش تجهیزات، فیلم‌برداری میدانی و تدوین نهایی",
                            iconEmoji = "SVG_CINEMA",
                            colorHex = 0xFFF59E0B,
                            startDate = "۱۴۰۵/۰۸/۰۱",
                            endDate = "۱۴۰۵/۰۹/۱۵",
                            priority = "HIGH",
                            tagOrCategory = "تولید و رسانه",
                            isActive = true,
                            isArchived = false,
                            reminderEnabled = true,
                            reminderTime = "هر روز • ساعت ۱۶:۰۰",
                            topicNote = "لیست لنزهای پرایم و میکروفون‌های یقه برای آفیش پنجشنبه چک شود.",
                            createdAt = "۱۴۰۵/۰۸/۰۲"
                        ),
                        TopicEntity(
                            id = "top_biz",
                            title = "کار و توسعه محصول نرم‌افزاری",
                            description = "مدیریت اسپرینت‌ها، بررسی کد، جلسات فنی و انتشار نسخه‌ها",
                            iconEmoji = "SVG_BUSINESS",
                            colorHex = 0xFF10B981,
                            startDate = "۱۴۰۵/۰۸/۰۱",
                            endDate = null,
                            priority = "HIGH",
                            tagOrCategory = "شغلی و کسب‌وکار",
                            isActive = true,
                            isArchived = false,
                            reminderEnabled = true,
                            reminderTime = "هر روز • ساعت ۰۹:۰۰",
                            topicNote = "اولویت این هفته پایداری کامل دیتابیس و موتور اعلان‌هاست.",
                            createdAt = "۱۴۰۵/۰۸/۰۳"
                        )
                    )
                )

                // Seed Tasks (Both Independent Tasks & Topic-linked Missions with Subtasks)
                dao.insertTasks(
                    listOf(
                        TaskEntity(
                            id = "1",
                            title = "بررسی و تایید گزارش مالی شرکت",
                            category = "WORK",
                            priority = "URGENT",
                            completed = false,
                            dueDate = "۱۴۰۵/۰۸/۰۶",
                            createdAt = "۱۴۰۵/۰۸/۰۱",
                            topicId = "top_biz",
                            description = "ممیزی کامل صورت‌حساب‌ها و تطبیق با تراز آزمایشی",
                            status = "IN_PROGRESS",
                            subtasksSerialized = "s1::بررسی فاکتورهای مهرماه::true;;s2::تایید نهایی جدول بودجه::false",
                            reminderTime = "۱۴۰۵/۰۸/۰۶ • ساعت ۱۱:۳۰",
                            isMajorMission = true,
                            activityLogSerialized = "ایجاد مأموریت در تاپیک کار و توسعه||تغییر وضعیت به در حال انجام"
                        ),
                        TaskEntity(
                            id = "2",
                            title = "ورزش هوازی و بدنسازی (۴۵ دقیقه)",
                            category = "HEALTH",
                            priority = "HIGH",
                            completed = true,
                            dueDate = "۱۴۰۵/۰۸/۰۵",
                            createdAt = "۱۴۰۵/۰۸/۰۱",
                            topicId = null, // Independent task
                            description = "تمرین هوازی صبحگاهی و حرکات کششی",
                            status = "COMPLETED",
                            completedAt = "۱۴۰۵/۰۸/۰۵ • ساعت ۰۸:۳۰"
                        ),
                        TaskEntity(
                            id = "3",
                            title = "نگارش فصل سوم پایان‌نامه و استخراج نمودارها",
                            category = "STUDY",
                            priority = "URGENT",
                            completed = false,
                            dueDate = "۱۴۰۵/۰۸/۰۸",
                            createdAt = "۱۴۰۵/۰۸/۰۲",
                            topicId = "top_uni",
                            description = "تکمیل بخش روش‌پژوهش و جدول مقایسه نتایج",
                            status = "IN_PROGRESS",
                            subtasksSerialized = "u1::گردآوری داده‌های آزمایش::true;;u2::رسم نمودارهای مقایسه::true;;u3::نگارش متن تحلیل::false",
                            reminderTime = "۱۴۰۵/۰۸/۰۸ • ساعت ۱۰:۰۰",
                            isMajorMission = true
                        ),
                        TaskEntity(
                            id = "4",
                            title = "پرداخت قسط ۱ وام تجهیزات مهندسی",
                            category = "FINANCE",
                            priority = "URGENT",
                            completed = false,
                            dueDate = "۱۴۰۵/۰۸/۰۷",
                            createdAt = "۱۴۰۵/۰۸/۰۳",
                            linkedDebtId = "d1",
                            topicId = null, // Independent linked debt task
                            status = "TODO"
                        ),
                        TaskEntity(
                            id = "5",
                            title = "تدوین راف‌کات مستند صنعتی و اصلاح رنگ",
                            category = "WORK",
                            priority = "HIGH",
                            completed = false,
                            dueDate = "۱۴۰۵/۰۸/۰۹",
                            createdAt = "۱۴۰۵/۰۸/۰۳",
                            topicId = "top_film",
                            description = "سینک صدا و تصویر، انتخاب موسیقی متن و خروجی تست",
                            status = "TODO",
                            subtasksSerialized = "f1::سینک پلان‌های دوربین A و B::true;;f2::اصلاح رنگ و نور (Color Grading)::false",
                            reminderTime = "۱۴۰۵/۰۸/۰۹ • ساعت ۱۶:۰۰",
                            isMajorMission = true
                        ),
                        TaskEntity(
                            id = "6",
                            title = "تایید لوکیشن و چک‌لیست تجهیزات نورپردازی",
                            category = "WORK",
                            priority = "MEDIUM",
                            completed = true,
                            dueDate = "۱۴۰۵/۰۸/۰۴",
                            createdAt = "۱۴۰۵/۰۸/۰۲",
                            topicId = "top_film",
                            description = "بازدید لوکیشن فیلم‌برداری و تست باتری‌ها",
                            status = "COMPLETED",
                            completedAt = "۱۴۰۵/۰۸/۰۴ • ساعت ۱۹:۱۵"
                        )
                    )
                )

                // Seed Persistent Reminders
                dao.insertReminders(
                    listOf(
                        ReminderEntity(
                            id = "rem_TASK_1",
                            entityType = "TASK",
                            entityId = "1",
                            title = "بررسی و تایید گزارش مالی شرکت",
                            triggerTime = "۱۴۰۵/۰۸/۰۶ • ساعت ۱۱:۳۰",
                            topicId = "top_biz",
                            topicTitle = "کار و توسعه محصول نرم‌افزاری",
                            repeatMode = "ONCE",
                            snoozeIntervalMinutes = 30
                        ),
                        ReminderEntity(
                            id = "rem_TASK_3",
                            entityType = "TASK",
                            entityId = "3",
                            title = "نگارش فصل سوم پایان‌نامه و استخراج نمودارها",
                            triggerTime = "۱۴۰۵/۰۸/۰۸ • ساعت ۱۰:۰۰",
                            topicId = "top_uni",
                            topicTitle = "دانشگاه و پایان‌نامه ارشد",
                            repeatMode = "DAILY",
                            snoozeIntervalMinutes = 60
                        ),
                        ReminderEntity(
                            id = "rem_DEBT_d1",
                            entityType = "DEBT",
                            entityId = "d1",
                            title = "سررسید قسط وام تجهیزات مهندسی (بانک رسالت)",
                            triggerTime = "۱۴۰۵/۰۸/۰۷ • ساعت ۰۹:۰۰",
                            sensitiveAmountToman = 3_000_000L
                        )
                    )
                )

                // Seed Habits
                dao.insertHabits(
                    listOf(
                        HabitEntity("h1", "مطالعه ۳۰ دقیقه روزانه", 7, true, "STUDY"),
                        HabitEntity("h2", "ورزش و نرمش صبحگاهی", 5, false, "HEALTH"),
                        HabitEntity("h3", "نوشیدن ۸ لیوان آب", 12, true, "HEALTH")
                    )
                )

                // Seed Expenses, Incomes & Transfers
                dao.insertExpenses(
                    listOf(
                        ExpenseEntity("e0", "واریز حقوق و پاداش ماهانه", 28500000, "حقوق و دستمزد", "۱۴۰۵/۰۸/۰۱", true, "بانک ملی ایران", "INCOME", "بانک ملی ایران"),
                        ExpenseEntity("e1", "خرید از فروشگاه رفاه", 1250000, "سوپرمارکت و روزمره", "۱۴۰۵/۰۸/۰۵", true, "بانک ملت", "EXPENSE", "بانک ملت"),
                        ExpenseEntity("e2", "کافه و میان‌وعده کاری", 350000, "کافه و دخانیات", "۱۴۰۵/۰۸/۰۴", false, null, "EXPENSE", "بلوبانک"),
                        ExpenseEntity("e3", "شارژ کارت مترو و اسنپ", 180000, "حمل و نقل", "۱۴۰۵/۰۸/۰۳", true, "بانک سامان", "EXPENSE", "بانک سامان"),
                        ExpenseEntity("e4", "انتقال به کارت پس‌انداز بلوبانک", 3000000, "انتقال داخلی", "۱۴۰۵/۰۸/۰۲", true, "بلوبانک", "TRANSFER", "بلوبانک")
                    )
                )

                // Seed Debts & Receivables
                dao.insertDebts(
                    listOf(
                        DebtEntity("d1", "وام تجهیزات مهندسی (۴ قسط)", "DEBT", 12000000, 3000000, "۱۴۰۵/۰۸/۱۵", false, "بانک رسالت"),
                        DebtEntity("d2", "طلب بابت مشاوره معماری سیستم", "RECEIVABLE", 8500000, 2500000, "۱۴۰۵/۰۸/۲۰", false, "شرکت آوا پردازش")
                    )
                )

                // Seed Health
                dao.insertOrUpdateHealthLog(
                    HealthLogEntity(
                        id = "hl1",
                        date = "۱۴۰۵/۰۸/۰۵",
                        sleepHours = 7.5f,
                        sleepQuality = "good",
                        waterGlasses = 6,
                        exerciseMinutes = 45,
                        exerciseType = "بدنسازی و هوازی",
                        caloriesBurned = 480,
                        dailyCalorieIntake = 1850,
                        lastScannedFoodSummary = "سینه مرغ گریل و سالاد کینوا (۴۲۰ کالری • ۳۸ گرم پروتئین)"
                    )
                )

                // Seed Vault Notes
                dao.insertVaultNotes(
                    listOf(
                        VaultNoteEntity("v1", "رمز کیف پول سخت‌افزاری", "Seed: apple banana cherry ... secret pin: 9942", "حساب‌های بانکی", "۱۴۰۵/۰۸/۰۱", true),
                        VaultNoteEntity("v2", "یادداشت محرمانه بورسی", "رمز عبور کارگزاری مفید و حساب‌های صرافی", "رمزهای عبور", "۱۴۰۵/۰۸/۰۳", true)
                    )
                )
            }
        }
    }
}
