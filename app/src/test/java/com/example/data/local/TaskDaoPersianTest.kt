package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.task.PersianTextNormalizer
import com.example.data.local.task.TaskDao
import com.example.data.local.task.TaskEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class TaskDaoPersianTest {

    private lateinit var database: LifeOsDatabase
    private lateinit var taskDao: TaskDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, LifeOsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        taskDao = database.taskDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetTaskById_returnsCorrectEntity() = runBlocking {
        val task = TaskEntity.create(
            title = "ورزش صبحگاهی و دویدن",
            category = "سلامت",
            priority = "HIGH",
            dueDateEpochDay = 20000L
        )

        taskDao.insertTask(task)

        val retrieved = taskDao.getTaskById(task.id)
        assertNotNull(retrieved)
        assertEquals("ورزش صبحگاهی و دویدن", retrieved?.title)
        assertEquals("سلامت", retrieved?.category)
        assertEquals("HIGH", retrieved?.priority)
        assertEquals("بالا", retrieved?.priorityFa)
        assertEquals("در انتظار", retrieved?.statusFa)
    }

    @Test
    fun updateTaskStatus_updatesStatusAndCompletedAt() = runBlocking {
        val task = TaskEntity.create(
            title = "ارسال گزارش هفتگی",
            category = "کاری"
        )
        taskDao.insertTask(task)

        val completedTime = System.currentTimeMillis()
        val rowsAffected = taskDao.updateTaskStatus(
            taskId = task.id,
            newStatus = "COMPLETED",
            completedAtMillis = completedTime
        )

        assertEquals(1, rowsAffected)
        val updated = taskDao.getTaskById(task.id)
        assertEquals("COMPLETED", updated?.status)
        assertEquals("تکمیل‌شده", updated?.statusFa)
        assertEquals(completedTime, updated?.completedAtMillis)
    }

    @Test
    fun deleteTaskById_removesEntity() = runBlocking {
        val task = TaskEntity.create(
            title = "جلسه کاری با تیم فنی"
        )
        taskDao.insertTask(task)
        assertNotNull(taskDao.getTaskById(task.id))

        val deletedCount = taskDao.deleteTaskById(task.id)
        assertEquals(1, deletedCount)
        assertNull(taskDao.getTaskById(task.id))
    }

    @Test
    fun persianSearch_matchesWithArabicYehAndKafVariants() = runBlocking {
        val task = TaskEntity.create(
            title = "خرید چای و قهوه ترک",
            description = "از سوپرمارکت محله تهیه شود"
        )
        taskDao.insertTask(task)

        // Search with Arabic Yeh 'ي' (\u064A) instead of Persian 'ی' (\u06CC)
        val arabicYehQuery = "چاي"
        val resultsYeh = taskDao.searchTasks(userId = "local_device_user", query = arabicYehQuery).first()
        assertEquals(1, resultsYeh.size)
        assertEquals(task.id, resultsYeh.first().id)

        // Search with Arabic Kaf 'ك' (\u0643) instead of Persian 'ک' (\u06A9)
        val arabicKafQuery = "ترك"
        val resultsKaf = taskDao.searchTasks(userId = "local_device_user", query = arabicKafQuery).first()
        assertEquals(1, resultsKaf.size)
        assertEquals(task.id, resultsKaf.first().id)
    }

    @Test
    fun persianSearch_matchesWithAndWithoutZwnjHalfSpace() = runBlocking {
        // Created with ZWNJ (نیم‌فاصله \u200C)
        val task = TaskEntity.create(
            title = "مطالعه‌ی کتاب‌های برنامه‌نویسی",
            notes = "فصل ۳ و ۴"
        )
        taskDao.insertTask(task)

        // Search without ZWNJ (plain space or joined)
        val queryWithoutZwnj = "کتابهای برنامه نویسی"
        val results = taskDao.searchTasks(userId = "local_device_user", query = queryWithoutZwnj).first()
        assertEquals(1, results.size)
        assertEquals(task.id, results.first().id)
    }

    @Test
    fun persianSearch_matchesPersianAndEnglishNumerals() = runBlocking {
        val task = TaskEntity.create(
            title = "پرداخت فاکتور شماره ۱۲۳۴۵",
            category = "مالی"
        )
        taskDao.insertTask(task)

        // Search with Latin digits "12345"
        val latinQuery = "12345"
        val results = taskDao.searchTasks(userId = "local_device_user", query = latinQuery).first()
        assertEquals(1, results.size)
        assertEquals(task.id, results.first().id)
    }

    @Test
    fun observeTasksForEpochDay_filtersByJalaliCompatibleDate() = runBlocking {
        val targetDay = 20350L // Specific epoch day
        val task1 = TaskEntity.create(title = "تسک روز هدف", dueDateEpochDay = targetDay)
        val task2 = TaskEntity.create(title = "تسک روز دیگر", dueDateEpochDay = targetDay + 2L)

        taskDao.insertTasks(listOf(task1, task2))

        val dayTasks = taskDao.observeTasksForEpochDay(userId = "local_device_user", epochDay = targetDay).first()
        assertEquals(1, dayTasks.size)
        assertEquals(task1.id, dayTasks.first().id)
    }

    @Test
    fun observeOverdueTasks_returnsOnlyPastPendingTasks() = runBlocking {
        val currentDay = 20500L
        val overdueTask = TaskEntity.create(title = "تسک دیروز", dueDateEpochDay = currentDay - 1L)
        val todayTask = TaskEntity.create(title = "تسک امروز", dueDateEpochDay = currentDay)
        val futureTask = TaskEntity.create(title = "تسک فردا", dueDateEpochDay = currentDay + 1L)

        taskDao.insertTasks(listOf(overdueTask, todayTask, futureTask))

        val overdueList = taskDao.observeOverdueTasks(userId = "local_device_user", currentEpochDay = currentDay).first()
        assertEquals(1, overdueList.size)
        assertEquals(overdueTask.id, overdueList.first().id)
        assertTrue(overdueTask.isOverdue(currentDay))
    }
}
