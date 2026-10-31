package com.example.personallifeos.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.personallifeos.data.local.AppDatabase
import com.example.personallifeos.data.model.ReminderRepeatMode
import com.example.personallifeos.util.PersianUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getInstance(context).lifeOsDao()

                when (action) {
                    Intent.ACTION_BOOT_COMPLETED -> {
                        val activeReminders = dao.getActiveRemindersList()
                        activeReminders.forEach { entity ->
                            SmartReminderManager.scheduleReminderAlarm(context, entity.toDomain())
                        }
                    }

                    SmartReminderManager.ACTION_TRIGGER_REMINDER -> {
                        val reminderId = intent.getStringExtra(SmartReminderManager.EXTRA_REMINDER_ID) ?: return@launch
                        val entityId = intent.getStringExtra(SmartReminderManager.EXTRA_ENTITY_ID) ?: ""
                        val entityType = intent.getStringExtra(SmartReminderManager.EXTRA_ENTITY_TYPE) ?: "TASK"
                        val title = intent.getStringExtra(SmartReminderManager.EXTRA_TITLE) ?: "یادآوری مأموریت"
                        val triggerTime = intent.getStringExtra(SmartReminderManager.EXTRA_TRIGGER_TIME) ?: "اکنون"
                        val topicId = intent.getStringExtra(SmartReminderManager.EXTRA_TOPIC_ID)
                        val topicTitle = intent.getStringExtra(SmartReminderManager.EXTRA_TOPIC_TITLE)
                        val repeatName = intent.getStringExtra(SmartReminderManager.EXTRA_REPEAT_MODE) ?: "ONCE"
                        val snoozeMins = intent.getIntExtra(SmartReminderManager.EXTRA_SNOOZE_MINUTES, 60)

                        // Verify task isn't already completed or topic reminders disabled
                        if (entityType == "TASK" && entityId.isNotBlank()) {
                            val task = dao.getTaskById(entityId)
                            if (task != null && (task.completed || task.status == "COMPLETED" || task.status == "CANCELLED")) {
                                dao.deleteReminder(reminderId)
                                return@launch
                            }
                        }
                        if (!topicId.isNullOrBlank()) {
                            val topic = dao.getTopicById(topicId)
                            if (topic != null && !topic.reminderEnabled) {
                                return@launch
                            }
                        }

                        SmartReminderManager.showReminderSystemNotification(
                            context = context,
                            reminderId = reminderId,
                            entityId = entityId,
                            entityType = entityType,
                            title = title,
                            triggerTime = triggerTime,
                            topicId = topicId,
                            topicTitle = topicTitle,
                            snoozeMinutes = snoozeMins
                        )

                        // Reschedule if recurring (DAILY, WEEKLY, MONTHLY)
                        val repeatMode = ReminderRepeatMode.entries.find { it.name == repeatName } ?: ReminderRepeatMode.ONCE
                        val nextEpoch = SmartReminderManager.nextRepeatTriggerMillis(System.currentTimeMillis(), repeatMode)
                        if (nextEpoch != null) {
                            val activeList = dao.getActiveRemindersList()
                            val existing = activeList.find { it.id == reminderId }
                            if (existing != null) {
                                val updated = existing.copy(triggerEpochMillis = nextEpoch)
                                dao.insertReminder(updated)
                                SmartReminderManager.scheduleReminderAlarm(context, updated.toDomain())
                            }
                        }
                    }

                    SmartReminderManager.ACTION_SNOOZE_REMINDER -> {
                        val reminderId = intent.getStringExtra(SmartReminderManager.EXTRA_REMINDER_ID) ?: return@launch
                        val snoozeMins = intent.getIntExtra(SmartReminderManager.EXTRA_SNOOZE_MINUTES, 60).coerceAtLeast(5)
                        SmartReminderManager.cancelReminderAlarm(context, reminderId)

                        val activeList = dao.getActiveRemindersList()
                        val existing = activeList.find { it.id == reminderId }
                        if (existing != null) {
                            val nextEpoch = System.currentTimeMillis() + snoozeMins * 60_000L
                            val updated = existing.copy(
                                isSnoozed = true,
                                snoozeIntervalMinutes = snoozeMins,
                                triggerTime = "تعویق‌شده (+${PersianUtils.formatPersianNumber(snoozeMins.toLong())} دقیقه دیگر)",
                                triggerEpochMillis = nextEpoch
                            )
                            dao.insertReminder(updated)
                            SmartReminderManager.scheduleReminderAlarm(context, updated.toDomain())
                        }
                    }

                    SmartReminderManager.ACTION_COMPLETE_TASK -> {
                        val reminderId = intent.getStringExtra(SmartReminderManager.EXTRA_REMINDER_ID) ?: ""
                        val entityId = intent.getStringExtra(SmartReminderManager.EXTRA_ENTITY_ID) ?: ""
                        if (reminderId.isNotBlank()) {
                            SmartReminderManager.cancelReminderAlarm(context, reminderId)
                            dao.deleteReminder(reminderId)
                        }
                        if (entityId.isNotBlank()) {
                            val task = dao.getTaskById(entityId)
                            if (task != null) {
                                dao.updateTask(
                                    task.copy(
                                        completed = true,
                                        status = "COMPLETED",
                                        completedAt = PersianUtils.getPersianDateString()
                                    )
                                )
                            }
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
