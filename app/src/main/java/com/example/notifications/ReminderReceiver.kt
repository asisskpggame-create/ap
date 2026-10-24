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
import com.example.data.local.PreferencesDataStore
import com.example.data.repository.LifeOsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

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

        val title = intent.getStringExtra(EXTRA_TITLE) ?: "یادآوری Personal Life OS"
        val body = intent.getStringExtra(EXTRA_BODY) ?: "زمان انجام فعالیت فرا رسیده است"
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 1001)

        if (action == ACTION_SNOOZE) {
            NotificationManagerCompat.from(context).cancel(notificationId)
            ReminderScheduler.scheduleReminder(
                context = context,
                notificationId = notificationId,
                title = title,
                body = "$body (تعویق ۱۰ دقیقه‌ای)",
                triggerAtMillis = System.currentTimeMillis() + 10 * 60_000L
            )
            return
        }

        if (action == ACTION_DISMISS) {
            NotificationManagerCompat.from(context).cancel(notificationId)
            return
        }

        ReminderScheduler.ensureChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
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

        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(0, "تعویق ۱۰ دقیقه", snoozePending)
            .addAction(0, "رد کردن", dismissPending)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    companion object {
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_BODY = "extra_body"
        const val ACTION_SNOOZE = "com.example.notifications.ACTION_SNOOZE"
        const val ACTION_DISMISS = "com.example.notifications.ACTION_DISMISS"
        const val ACTION_SIX_HOUR_SYNC = "com.example.notifications.ACTION_SIX_HOUR_SYNC"
    }
}

object ReminderScheduler {
    const val CHANNEL_ID = "personal_life_os_reminders"
    const val SIX_HOURS_MILLIS = 6 * 60 * 60 * 1000L
    private const val SIX_HOUR_SYNC_REQUEST_CODE = 900600

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "یادآورها و هشدارهای هوشمند",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "اعلان‌های مربوط به تسک‌ها، عادت‌ها و برنامه روزانه"
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
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
        ensureChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_BODY, body)
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
