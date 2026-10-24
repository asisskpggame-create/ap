package com.example.notifications

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.Toast
import com.example.MainActivity
import com.example.R
import com.example.data.repository.LifeOsRepository
import com.example.util.WorldClassOsUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Interactive Home Screen Widget for Personal Life OS:
 * Allows 1-tap background logging of "+آب (۲۵۰ml)", "+دبل (۹۰ تومن)", or launching Voice Capture
 * directly from the Android home screen without opening the full app.
 */
class LifeOsQuickWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            updateWidgetView(context, appWidgetManager, widgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_WIDGET_ADD_WATER -> {
                WorldClassOsUtils.HapticSignature.crispTick(context)
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val repo = LifeOsRepository(context.applicationContext)
                        repo.addConsumptionEntry(
                            itemName = "آب",
                            category = "WATER",
                            quantity = 250.0,
                            unit = "ml",
                            inputSource = "QUICK_ADD"
                        )
                    } finally {
                        pendingResult.finish()
                    }
                }
                Toast.makeText(context, "💧 +۲۵۰ میلی‌لیتر آب ثبت شد", Toast.LENGTH_SHORT).show()
            }

            ACTION_WIDGET_ADD_ESPRESSO -> {
                WorldClassOsUtils.HapticSignature.doublePulse(context)
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val repo = LifeOsRepository(context.applicationContext)
                        val expRes = repo.addExpense(
                            title = "خرید دبل (اسپرسو)",
                            category = "COFFEE",
                            amount = 90000.0,
                            currency = "TOMAN",
                            notes = "ثبت سریع از ویجت صفحه اصلی",
                            inputSource = "MANUAL",
                            transactionType = "EXPENSE"
                        )
                        repo.addConsumptionEntry(
                            itemName = "دبل (اسپرسو)",
                            category = "COFFEE",
                            quantity = 1.0,
                            unit = "cups",
                            linkedExpenseId = expRes.getOrDefault(""),
                            inputSource = "QUICK_ADD"
                        )
                    } finally {
                        pendingResult.finish()
                    }
                }
                Toast.makeText(context, "☕ دبل (۹۰,۰۰۰ تومان) در مصرف و خرج روزانه ثبت شد", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val ACTION_WIDGET_ADD_WATER = "com.example.widget.ACTION_ADD_WATER"
        const val ACTION_WIDGET_ADD_ESPRESSO = "com.example.widget.ACTION_ADD_ESPRESSO"

        fun updateWidgetView(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_life_os_quick)

            // 1. Water Button PendingIntent
            val waterIntent = Intent(context, LifeOsQuickWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_ADD_WATER
            }
            val waterPending = PendingIntent.getBroadcast(
                context,
                8801,
                waterIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_water, waterPending)

            // 2. Espresso 90k Button PendingIntent
            val espressoIntent = Intent(context, LifeOsQuickWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_ADD_ESPRESSO
            }
            val espressoPending = PendingIntent.getBroadcast(
                context,
                8802,
                espressoIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_espresso, espressoPending)

            // 3. Voice Command Button PendingIntent
            val voiceIntent = Intent(context, MainActivity::class.java).apply {
                action = QuickVoiceTileService.ACTION_QUICK_VOICE
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            val voicePending = PendingIntent.getActivity(
                context,
                8803,
                voiceIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_voice, voicePending)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun refreshAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, LifeOsQuickWidgetProvider::class.java))
            for (id in ids) {
                updateWidgetView(context, manager, id)
            }
        }
    }
}
