package com.example.ui.viewmodel

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.LifeOsDatabase
import com.example.data.local.task.TaskDao
import com.example.data.local.task.TaskEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class TasksViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var database: LifeOsDatabase
    private lateinit var taskDao: TaskDao
    private lateinit var viewModel: TasksViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, LifeOsDatabase::class.java)
            .setQueryExecutor(testDispatcher.asExecutor())
            .setTransactionExecutor(testDispatcher.asExecutor())
            .allowMainThreadQueries()
            .build()
        taskDao = database.taskDao()
        viewModel = TasksViewModel(taskDao = taskDao, userId = "test_user")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        database.close()
    }

    @Test
    fun addTask_insertsTaskAndUpdatesStateFlow() = testScope.runTest {
        viewModel.addTask(
            title = "طراحی رابط کاربری شیشه‌ای",
            description = "پیاده‌سازی گلس‌مورفیسم در کامپوز",
            category = "کاری",
            priority = "HIGH"
        )
        advanceUntilIdle()

        val taskList = viewModel.tasks.first { it.isNotEmpty() }
        assertEquals(1, taskList.size)
        val task = taskList.first()
        assertEquals("طراحی رابط کاربری شیشه‌ای", task.title)
        assertEquals("کاری", task.category)
        assertEquals("HIGH", task.priority)
        assertEquals("PENDING", task.status)
    }

    @Test
    fun toggleTaskCompletion_togglesStatusBetweenCompletedAndPending() = testScope.runTest {
        val initialTask = TaskEntity.create(
            title = "ورزش روزانه",
            userId = "test_user"
        )
        taskDao.insertTask(initialTask)
        advanceUntilIdle()

        val tasksBeforeToggle = viewModel.tasks.first { it.isNotEmpty() }
        assertEquals("PENDING", tasksBeforeToggle.first().status)

        // Toggle to COMPLETED
        viewModel.toggleTaskCompletion(initialTask.id)
        advanceUntilIdle()

        val completedTasksList = viewModel.tasks.first { list ->
            list.any { it.id == initialTask.id && it.status == "COMPLETED" }
        }
        val taskAfterFirst = completedTasksList.first { it.id == initialTask.id }
        assertEquals("COMPLETED", taskAfterFirst.status)
        assertTrue(taskAfterFirst.completedAtMillis > 0L)

        // Toggle back to PENDING
        viewModel.toggleTaskCompletion(initialTask.id)
        advanceUntilIdle()

        val pendingTasksList = viewModel.tasks.first { list ->
            list.any { it.id == initialTask.id && it.status == "PENDING" }
        }
        val taskAfterSecond = pendingTasksList.first { it.id == initialTask.id }
        assertEquals("PENDING", taskAfterSecond.status)
        assertEquals(-1L, taskAfterSecond.completedAtMillis)
    }

    @Test
    fun deleteTask_removesTaskFromDatabaseAndStateFlow() = testScope.runTest {
        val task = TaskEntity.create(
            title = "تسک تستی برای حذف",
            userId = "test_user"
        )
        taskDao.insertTask(task)
        advanceUntilIdle()

        val beforeDelete = viewModel.tasks.first { it.isNotEmpty() }
        assertEquals(1, beforeDelete.size)

        viewModel.deleteTask(task.id)
        advanceUntilIdle()

        val afterDelete = viewModel.tasks.first { it.isEmpty() }
        assertEquals(0, afterDelete.size)
        val inDb = taskDao.getTaskById(task.id)
        assertEquals(null, inDb)
    }

    @Test
    fun searchAndCategoryFilter_filtersTasksCorrectly() = testScope.runTest {
        val task1 = TaskEntity.create(
            title = "خرید قهوه ترک",
            category = "شخصی",
            userId = "test_user"
        )
        val task2 = TaskEntity.create(
            title = "ارسال گزارش مالی",
            category = "مالی",
            userId = "test_user"
        )
        taskDao.insertTasks(listOf(task1, task2))
        advanceUntilIdle()

        // Filter by category "مالی"
        viewModel.setCategoryFilter("مالی")
        advanceUntilIdle()

        val financeTasks = viewModel.tasks.first { it.size == 1 }
        assertEquals("ارسال گزارش مالی", financeTasks.first().title)

        // Reset category and search "قهوه"
        viewModel.setCategoryFilter("ALL")
        viewModel.setSearchQuery("قهوه")
        advanceUntilIdle()

        val coffeeTasks = viewModel.tasks.first { it.size == 1 && it.first().title == "خرید قهوه ترک" }
        assertEquals("خرید قهوه ترک", coffeeTasks.first().title)
    }
}
