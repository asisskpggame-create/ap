package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.LifeOsDatabase
import com.example.data.local.PreferencesDataStore
import com.example.data.repository.LifeOsRepository
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        // Reschedule alarms on device reboot
        if (action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val prefsStore = PreferencesDataStore(context.applicationContext)
                    val prefs = prefsStore.observeLocalPreferences(LifeOsDatabase.LOCAL_DEVICE_USER_ID).first()
                    ReminderScheduler.scheduleSixHourPeriodicSync(context.applicationContext)
                    ReminderScheduler.scheduleDailyExpenseReminder(
                        context = context.applicationContext,
                        enabled = prefs.dailyExpenseReminderEnabled && prefs.notificationsEnabled,
                        hourOfDay = prefs.dailyExpenseReminderHour,
                        minute = prefs.dailyExpenseReminderMinute
                    )
                } catch (_: Exception) {
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        // 6-Hour Periodic Data Transfer Trigger
        if (action == ACTION_SIX_HOUR_SYNC) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repo = LifeOsRepository(context.applicationContext)
                    val prefsStore = PreferencesDataStore(context.applicationContext)
                    repo.performSixHourDataTransfer(context.applicationContext)
                    prefsStore.saveLastSyncMillis(System.currentTimeMillis())
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        // Scheduled Daily Expense Reminder Trigger
        if (action == ACTION_DAILY_EXPENSE_REMINDER) {
            val hour = intent.getIntExtra(EXTRA_REMINDER_HOUR, 21)
            val minute = intent.getIntExtra(EXTRA_REMINDER_MINUTE, 0)
            val isTest = intent.getBooleanExtra(EXTRA_IS_TEST, false)
            if (!isTest) {
                ReminderScheduler.scheduleDailyExpenseReminder(
                    context = context,
                    enabled = true,
                    hourOfDay = hour,
                    minute = minute
                )
            }
            showReminderNotification(
                context = context,
                notificationId = DAILY_EXPENSE_NOTIFICATION_ID,
                title = intent.getStringExtra(EXTRA_TITLE) ?: "یادآور ثبت هزینه‌های روزانه",
                body = intent.getStringExtra(EXTRA_BODY) ?: "وقت ثبت خرج و مخارج امروز است. برای ثبت سریع ضربه بزنید.",
                openExpenseTab = true
            )
            return
        }

        val title = intent.getStringExtra(EXTRA_TITLE) ?: "یادآوری Personal Life OS"
        val body = intent.getStringExtra(EXTRA_BODY) ?: "زمان انجام فعالیت فرا رسیده است"
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 1001)
        val entityType = intent.getStringExtra(EXTRA_ENTITY_TYPE) ?: "GENERAL"
        val entityId = intent.getStringExtra(EXTRA_ENTITY_ID) ?: ""
        val hideSensitive = intent.getBooleanExtra(EXTRA_HIDE_SENSITIVE, false)

        if (action == ACTION_SNOOZE) {
            ReminderScheduler.snoozeReminder(
                context = context,
                notificationId = notificationId,
                title = title,
                body = body,
                entityType = entityType,
                entityId = entityId,
                hideSensitiveContent = hideSensitive
            )
            return
        }

        if (action == ACTION_DISMISS) {
            ReminderScheduler.dismissReminder(context, notificationId)
            return
        }

        val displayBody = if (hideSensitive && (entityType == "DEBT" || entityType == "DAILY_EXPENSE")) {
            "یادآور مالی ثبت‌شده — برای مشاهده جزئیات وارد برنامه شوید."
        } else {
            body
        }

        showReminderNotification(
            context = context,
            notificationId = notificationId,
            title = title,
            body = displayBody,
            openExpenseTab = notificationId == DAILY_EXPENSE_NOTIFICATION_ID || entityType == "DEBT" || entityType == "DAILY_EXPENSE"
        )
    }

    private fun showReminderNotification(
        context: Context,
        notificationId: Int,
        title: String,
        body: String,
        openExpenseTab: Boolean
    ) {
        ReminderScheduler.ensureChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            if (openExpenseTab) {
                this.action = ACTION_OPEN_EXPENSE_LOG
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
            this.action = ACTION_SNOOZE
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_BODY, body)
        }
        val snoozePending = PendingIntent.getBroadcast(
            context,
            notificationId + 10000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, ReminderReceiver::class.java).apply {
            this.action = ACTION_DISMISS
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val dismissPending = PendingIntent.getBroadcast(
            context,
            notificationId + 20000,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)

        if (openExpenseTab) {
            builder.addAction(0, "ثبت هزینه", contentPendingIntent)
        }
        builder
            .addAction(0, "تعویق ۱۰ دقیقه", snoozePending)
            .addAction(0, "بستن", dismissPending)

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }

    companion object {
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_BODY = "extra_body"
        const val EXTRA_ENTITY_TYPE = "extra_entity_type"
        const val EXTRA_ENTITY_ID = "extra_entity_id"
        const val EXTRA_HIDE_SENSITIVE = "extra_hide_sensitive"
        const val EXTRA_REMINDER_HOUR = "extra_reminder_hour"
        const val EXTRA_REMINDER_MINUTE = "extra_reminder_minute"
        const val EXTRA_IS_TEST = "extra_is_test"
        const val DAILY_EXPENSE_NOTIFICATION_ID = 2001
        const val ACTION_SNOOZE = "com.example.notifications.ACTION_SNOOZE"
        const val ACTION_DISMISS = "com.example.notifications.ACTION_DISMISS"
        const val ACTION_SIX_HOUR_SYNC = "com.example.notifications.ACTION_SIX_HOUR_SYNC"
        const val ACTION_DAILY_EXPENSE_REMINDER = "com.example.notifications.ACTION_DAILY_EXPENSE_REMINDER"
        const val ACTION_OPEN_EXPENSE_LOG = "com.example.notifications.ACTION_OPEN_EXPENSE_LOG"
    }
}

data class ScheduledReminderRecord(
    val notificationId: Int,
    val entityType: String,
    val entityId: String,
    val title: String,
    val body: String,
    val triggerAtMillis: Long,
    val hideSensitiveContent: Boolean = false,
    val snoozeCount: Int = 0
)

object ReminderScheduler {
    const val CHANNEL_ID = "personal_life_os_reminders"
    const val SIX_HOURS_MILLIS = 6 * 60 * 60 * 1000L
    private const val SIX_HOUR_SYNC_REQUEST_CODE = 900600
    private const val DAILY_EXPENSE_REMINDER_REQUEST_CODE = 900700

    private val activeReminders = java.util.concurrent.ConcurrentHashMap<Int, ScheduledReminderRecord>()

    fun notificationIdForEntity(entityType: String, entityId: String): Int {
        val key = "${entityType.uppercase().trim()}:${entityId.trim()}"
        return (key.hashCode() and 0x7FFFFFFF).coerceAtLeast(3000)
    }

    fun getActiveReminders(): List<ScheduledReminderRecord> =
        activeReminders.values.sortedBy { it.triggerAtMillis }

    fun getActiveReminderForEntity(entityType: String, entityId: String): ScheduledReminderRecord? =
        activeReminders[notificationIdForEntity(entityType, entityId)]

    fun clearAllTrackedRemindersForTest() {
        activeReminders.clear()
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "یادآورها و هشدارهای هوشمند",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "اعلان‌های مربوط به ثبت هزینه‌های روزانه، اقساط بدهی، تسک‌ها، اهداف و عادت‌ها"
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    /**
     * Schedules or cancels the daily expense logging notification at a specific hour and minute of the day.
     */
    fun scheduleDailyExpenseReminder(
        context: Context,
        enabled: Boolean,
        hourOfDay: Int,
        minute: Int
    ): Boolean {
        ensureChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        val safeHour = hourOfDay.coerceIn(0, 23)
        val safeMinute = minute.coerceIn(0, 59)

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_DAILY_EXPENSE_REMINDER
            putExtra(ReminderReceiver.EXTRA_REMINDER_HOUR, safeHour)
            putExtra(ReminderReceiver.EXTRA_REMINDER_MINUTE, safeMinute)
            putExtra(ReminderReceiver.EXTRA_TITLE, "یادآور ثبت هزینه‌های روزانه")
            putExtra(ReminderReceiver.EXTRA_BODY, "وقت ثبت خرج و مخارج امروز است. برای ثبت سریع ضربه بزنید.")
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DAILY_EXPENSE_REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (!enabled) {
            activeReminders.remove(ReminderReceiver.DAILY_EXPENSE_NOTIFICATION_ID)
            try {
                alarmManager.cancel(pendingIntent)
            } catch (_: Exception) {
            }
            return false
        }

        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, safeHour)
            set(Calendar.MINUTE, safeMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        activeReminders[ReminderReceiver.DAILY_EXPENSE_NOTIFICATION_ID] = ScheduledReminderRecord(
            notificationId = ReminderReceiver.DAILY_EXPENSE_NOTIFICATION_ID,
            entityType = "DAILY_EXPENSE",
            entityId = "daily_expense_log",
            title = "یادآور ثبت هزینه‌های روزانه",
            body = "وقت ثبت خرج و مخارج امروز است. برای ثبت سریع ضربه بزنید.",
            triggerAtMillis = calendar.timeInMillis
        )

        return try {
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                60_000L, // 1-minute precision window
                pendingIntent
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Immediately triggers the daily expense reminder notification so the user can test it.
     */
    fun triggerExpenseReminderNow(context: Context) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_DAILY_EXPENSE_REMINDER
            putExtra(ReminderReceiver.EXTRA_IS_TEST, true)
            putExtra(ReminderReceiver.EXTRA_TITLE, "یادآور ثبت هزینه‌های روزانه")
            putExtra(ReminderReceiver.EXTRA_BODY, "وقت ثبت خرج و مخارج امروز است. برای ثبت سریع ضربه بزنید.")
        }
        context.sendBroadcast(intent)
    }

    /**
     * Schedules a repeating 6-hour background data transfer alarm using AlarmManager.setInexactRepeating.
     */
    fun scheduleSixHourPeriodicSync(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_SIX_HOUR_SYNC
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            SIX_HOUR_SYNC_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + SIX_HOURS_MILLIS,
                SIX_HOURS_MILLIS,
                pendingIntent
            )
        } catch (_: Exception) {
        }
    }

    /**
     * Phase 3 Unified Reminder Engine: Schedules or updates a reminder for a specific entity
     * (TASK, DEBT, HABIT, GOAL, HEALTH) with automatic deduplication.
     */
    fun scheduleEntityReminder(
        context: Context?,
        entityType: String,
        entityId: String,
        title: String,
        body: String,
        triggerAtMillis: Long,
        hideSensitiveContent: Boolean = false
    ): Boolean {
        if (entityId.isBlank() || triggerAtMillis <= System.currentTimeMillis()) {
            cancelEntityReminder(context, entityType, entityId)
            return false
        }
        val notifId = notificationIdForEntity(entityType, entityId)
        val record = ScheduledReminderRecord(
            notificationId = notifId,
            entityType = entityType.uppercase().trim(),
            entityId = entityId.trim(),
            title = title,
            body = body,
            triggerAtMillis = triggerAtMillis,
            hideSensitiveContent = hideSensitiveContent
        )
        activeReminders[notifId] = record

        if (context == null) return true
        return scheduleAlarmInternal(
            context = context,
            notificationId = notifId,
            title = title,
            body = body,
            triggerAtMillis = triggerAtMillis,
            entityType = record.entityType,
            entityId = record.entityId,
            hideSensitiveContent = hideSensitiveContent
        )
    }

    /**
     * Reschedules an existing entity reminder when due date/time changes, cancelling the previous alarm first.
     */
    fun rescheduleEntityReminder(
        context: Context?,
        entityType: String,
        entityId: String,
        title: String,
        body: String,
        newTriggerAtMillis: Long,
        hideSensitiveContent: Boolean = false
    ): Boolean {
        cancelEntityReminder(context, entityType, entityId)
        return scheduleEntityReminder(
            context = context,
            entityType = entityType,
            entityId = entityId,
            title = title,
            body = body,
            triggerAtMillis = newTriggerAtMillis,
            hideSensitiveContent = hideSensitiveContent
        )
    }

    /**
     * Cancels any scheduled alarm and active notification for the given (entityType, entityId).
     * Called automatically when a task is completed/deleted or when a debt is settled.
     */
    fun cancelEntityReminder(
        context: Context?,
        entityType: String,
        entityId: String
    ) {
        val notifId = notificationIdForEntity(entityType, entityId)
        activeReminders.remove(notifId)
        if (context != null) {
            cancelAlarmAndNotificationById(context, notifId)
        }
    }

    /**
     * Cancels both the master debt reminder and any linked repayment task reminders upon full settlement.
     */
    fun cancelDebtAndLinkedTaskReminders(
        context: Context?,
        debtId: String,
        linkedTaskIds: List<String> = emptyList()
    ) {
        cancelEntityReminder(context, "DEBT", debtId)
        linkedTaskIds.forEach { taskId ->
            cancelEntityReminder(context, "TASK", taskId)
        }
    }

    /**
     * Snoozes a reminder by cancelling the current notification and replacing its schedule
     * with a single new trigger (default +10 minutes) without creating duplicate entries.
     */
    fun snoozeReminder(
        context: Context?,
        notificationId: Int,
        title: String = "یادآوری Personal Life OS",
        body: String = "زمان انجام فعالیت فرا رسیده است",
        entityType: String = "GENERAL",
        entityId: String = "",
        hideSensitiveContent: Boolean = false,
        snoozeDurationMillis: Long = 10 * 60_000L
    ): ScheduledReminderRecord {
        if (context != null) {
            try {
                NotificationManagerCompat.from(context).cancel(notificationId)
            } catch (_: Exception) {
            }
        }
        val existing = activeReminders[notificationId]
        val nextTrigger = System.currentTimeMillis() + snoozeDurationMillis.coerceAtLeast(60_000L)
        val baseBody = existing?.body ?: body.removeSuffix(" (تعویق ۱۰ دقیقه‌ای)")
        val snoozedBody = "$baseBody (تعویق ۱۰ دقیقه‌ای)"
        val updated = ScheduledReminderRecord(
            notificationId = notificationId,
            entityType = existing?.entityType ?: entityType,
            entityId = existing?.entityId ?: entityId.ifBlank { "id_$notificationId" },
            title = existing?.title ?: title,
            body = snoozedBody,
            triggerAtMillis = nextTrigger,
            hideSensitiveContent = existing?.hideSensitiveContent ?: hideSensitiveContent,
            snoozeCount = (existing?.snoozeCount ?: 0) + 1
        )
        activeReminders[notificationId] = updated
        if (context != null) {
            scheduleAlarmInternal(
                context = context,
                notificationId = notificationId,
                title = updated.title,
                body = updated.body,
                triggerAtMillis = nextTrigger,
                entityType = updated.entityType,
                entityId = updated.entityId,
                hideSensitiveContent = updated.hideSensitiveContent
            )
        }
        return updated
    }

    fun dismissReminder(context: Context?, notificationId: Int) {
        activeReminders.remove(notificationId)
        if (context != null) {
            cancelAlarmAndNotificationById(context, notificationId)
        }
    }

    private fun cancelAlarmAndNotificationById(context: Context, notificationId: Int) {
        try {
            NotificationManagerCompat.from(context).cancel(notificationId)
        } catch (_: Exception) {
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            try {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Schedules an inexact/windowed alarm respecting Android battery optimizations and background restrictions.
     */
    fun scheduleReminder(
        context: Context,
        notificationId: Int,
        title: String,
        body: String,
        triggerAtMillis: Long
    ): Boolean {
        if (triggerAtMillis <= System.currentTimeMillis()) return false
        activeReminders[notificationId] = ScheduledReminderRecord(
            notificationId = notificationId,
            entityType = "GENERAL",
            entityId = "id_$notificationId",
            title = title,
            body = body,
            triggerAtMillis = triggerAtMillis
        )
        return scheduleAlarmInternal(
            context = context,
            notificationId = notificationId,
            title = title,
            body = body,
            triggerAtMillis = triggerAtMillis,
            entityType = "GENERAL",
            entityId = "id_$notificationId",
            hideSensitiveContent = false
        )
    }

    private fun scheduleAlarmInternal(
        context: Context,
        notificationId: Int,
        title: String,
        body: String,
        triggerAtMillis: Long,
        entityType: String,
        entityId: String,
        hideSensitiveContent: Boolean
    ): Boolean {
        ensureChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_BODY, body)
            putExtra(ReminderReceiver.EXTRA_ENTITY_TYPE, entityType)
            putExtra(ReminderReceiver.EXTRA_ENTITY_ID, entityId)
            putExtra(ReminderReceiver.EXTRA_HIDE_SENSITIVE, hideSensitiveContent)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return try {
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                5 * 60_000L, // 5-minute OS battery-friendly window
                pendingIntent
            )
            true
        } catch (e: Exception) {
            false
        }
    }
}

