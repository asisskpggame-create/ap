package com.example.personallifeos.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.personallifeos.MainActivity
import com.example.personallifeos.data.model.ReminderItem
import com.example.personallifeos.data.model.ReminderRepeatMode

object SmartReminderManager {

    const val CHANNEL_ID = "personal_life_os_missions_channel"
    const val CHANNEL_NAME = "یادآوری هوشمند مأموریت‌ها و تاپیک‌ها"

    const val ACTION_TRIGGER_REMINDER = "com.example.personallifeos.ACTION_TRIGGER_REMINDER"
    const val ACTION_SNOOZE_REMINDER = "com.example.personallifeos.ACTION_SNOOZE_REMINDER"
    const val ACTION_COMPLETE_TASK = "com.example.personallifeos.ACTION_COMPLETE_TASK_FROM_NOTIFICATION"

    const val EXTRA_REMINDER_ID = "extra_reminder_id"
    const val EXTRA_ENTITY_ID = "extra_entity_id"
    const val EXTRA_ENTITY_TYPE = "extra_entity_type"
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_TRIGGER_TIME = "extra_trigger_time"
    const val EXTRA_TOPIC_ID = "extra_topic_id"
    const val EXTRA_TOPIC_TITLE = "extra_topic_title"
    const val EXTRA_REPEAT_MODE = "extra_repeat_mode"
    const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"

    const val EXTRA_OPEN_TOPIC_ID = "extra_open_topic_id"
    const val EXTRA_OPEN_TASK_ID = "extra_open_task_id"

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "اعلان‌های زمان‌بندی‌شده برای مأموریت‌ها، وظایف، تاپیک‌ها و اقساط"
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun hasPostNotificationPermission(context: Context): Boolean = hasNotificationPermission(context)

    /**
     * Schedules an Android system alarm for a ReminderItem using AlarmManager.setAndAllowWhileIdle
     * so it works reliably across Android versions without requiring SCHEDULE_EXACT_ALARM special approval.
     */
    fun scheduleReminderAlarm(context: Context, reminder: ReminderItem) {
        if (!reminder.isEnabled || reminder.isCompleted) {
            cancelReminderAlarm(context, reminder.id)
            return
        }
        ensureNotificationChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val triggerAtMillis = reminder.triggerEpochMillis?.takeIf { it > System.currentTimeMillis() }
            ?: (System.currentTimeMillis() + 60_000L)

        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_TRIGGER_REMINDER
            putExtra(EXTRA_REMINDER_ID, reminder.id)
            putExtra(EXTRA_ENTITY_ID, reminder.entityId)
            putExtra(EXTRA_ENTITY_TYPE, reminder.entityType.name)
            putExtra(EXTRA_TITLE, reminder.title)
            putExtra(EXTRA_TRIGGER_TIME, reminder.triggerTime)
            putExtra(EXTRA_TOPIC_ID, reminder.topicId)
            putExtra(EXTRA_TOPIC_TITLE, reminder.topicTitle)
            putExtra(EXTRA_REPEAT_MODE, reminder.repeatMode.name)
            putExtra(EXTRA_SNOOZE_MINUTES, reminder.snoozeIntervalMinutes)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } catch (_: SecurityException) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    /**
     * Cancels both the scheduled AlarmManager PendingIntent and any active status-bar notification
     * when a task/topic reminder is completed, disabled, or deleted.
     */
    fun cancelReminderAlarm(context: Context, reminderId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_TRIGGER_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager?.cancel(pendingIntent)
        try {
            NotificationManagerCompat.from(context).cancel(reminderId.hashCode())
        } catch (_: Exception) {
        }
    }

    /**
     * Displays a real Android notification showing Topic Name, Task Title, Due Time,
     * deep-link tap action to open the Task/Topic directly, plus Snooze and Complete actions.
     */
    fun showReminderSystemNotification(
        context: Context,
        reminderId: String,
        entityId: String,
        entityType: String,
        title: String,
        triggerTime: String,
        topicId: String?,
        topicTitle: String?,
        snoozeMinutes: Int = 60
    ) {
        ensureNotificationChannel(context)
        if (!hasNotificationPermission(context)) return

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_TOPIC_ID, topicId)
            putExtra(EXTRA_OPEN_TASK_ID, entityId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            reminderId.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze Action
        val snoozeIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_SNOOZE_REMINDER
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_SNOOZE_MINUTES, snoozeMinutes)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (reminderId + "_snooze").hashCode(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Complete Task Action
        val completeIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_COMPLETE_TASK
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_ENTITY_ID, entityId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            (reminderId + "_complete").hashCode(),
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val headerTopic = topicTitle?.takeIf { it.isNotBlank() } ?: "مأموریت مستقل • اهداف و مسیر"
        val contentText = "⏰ زمان انجام: $triggerTime"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setSubText(headerTopic)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle("$headerTopic — $title")
                    .bigText("$title\nزمان مقرر: $triggerTime\nبرای مشاهده جزئیات مأموریت لمس کنید.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                android.R.drawable.ic_lock_idle_alarm,
                "تعویق ($snoozeMinutes دقیقه)",
                snoozePendingIntent
            )

        if (entityType == "TASK") {
            builder.addAction(
                android.R.drawable.checkbox_on_background,
                "انجام شد ✓",
                completePendingIntent
            )
        }

        try {
            NotificationManagerCompat.from(context).notify(reminderId.hashCode(), builder.build())
        } catch (_: SecurityException) {
        }
    }

    fun nextRepeatTriggerMillis(currentMillis: Long, repeatMode: ReminderRepeatMode): Long? {
        return when (repeatMode) {
            ReminderRepeatMode.ONCE -> null
            ReminderRepeatMode.DAILY -> currentMillis + 24L * 60L * 60L * 1000L
            ReminderRepeatMode.WEEKLY -> currentMillis + 7L * 24L * 60L * 60L * 1000L
            ReminderRepeatMode.MONTHLY -> currentMillis + 30L * 24L * 60L * 60L * 1000L
        }
    }
}
