package com.example.personallifeos.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TaskEntity::class,
        HabitEntity::class,
        ExpenseEntity::class,
        DebtEntity::class,
        HealthLogEntity::class,
        VaultNoteEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun lifeOsDao(): LifeOsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "personal_life_os.db"
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

                // Seed Tasks (with linked debt)
                dao.insertTasks(
                    listOf(
                        TaskEntity("1", "بررسی و تایید گزارش مالی شرکت", "WORK", "URGENT", false, "۱۴۰۵/۰۸/۰۶", "۱۴۰۵/۰۸/۰۱"),
                        TaskEntity("2", "ورزش هوازی و بدنسازی (۴۵ دقیقه)", "HEALTH", "HIGH", true, "۱۴۰۵/۰۸/۰۵", "۱۴۰۵/۰۸/۰۱"),
                        TaskEntity("3", "مطالعه کتاب معماری نرم‌افزار", "STUDY", "MEDIUM", false, "۱۴۰۵/۰۸/۰۸", "۱۴۰۵/۰۸/۰۲"),
                        TaskEntity("4", "پرداخت قسط ۱ وام تجهیزات مهندسی", "FINANCE", "URGENT", false, "۱۴۰۵/۰۸/۰۷", "۱۴۰۵/۰۸/۰۳", "d1")
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
