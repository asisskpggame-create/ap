package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.Habit
import com.example.data.model.HabitRecord
import com.example.data.model.Task
import com.example.data.model.UserPreferencesDoc
import com.example.ui.theme.ElectricTeal
import com.example.util.WorldClassOsUtils

@Composable
fun GoalsAndHabitsCombinedScreen(
    prefs: UserPreferencesDoc,
    tasks: List<Task>,
    calendarDisplay: String,
    activeFocusTaskId: String?,
    onStartFocusTimer: (Task) -> Unit,
    onCreateTask: (title: String, desc: String, cat: String, tags: List<String>, prio: String, dueDay: Long, dueTime: Int, remMillis: Long, estDur: Int, subtasksJson: String, recRule: String, recInterval: Int, notes: String) -> Unit,
    onCompleteTask: (Task) -> Unit,
    onBulkComplete: (List<Task>) -> Unit,
    onUpdateTask: (Task) -> Unit,
    onRescheduleTask: (Task, Long) -> Unit,
    onArchiveOrRestore: (Task, Boolean) -> Unit,
    onDeleteTask: (Task) -> Unit,
    habits: List<Habit>,
    habitRecords: List<HabitRecord>,
    onCreateHabit: (title: String, category: String, scheduleType: String, scheduleDays: List<Int>, targetPerPeriod: Int) -> Unit,
    onToggleHabitDay: (Habit, Long) -> Unit
) {
    val context = LocalContext.current
    var selectedSectionIndex by remember { mutableIntStateOf(0) } // 0: Goals & Tasks, 1: Daily Habits

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Unified Header
        Text(
            text = "اهداف و عادت‌ها",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Segmented Section Selector (Unified 2-Part Switcher)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("goals_habits_section_switcher")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val sections = listOf(
                    0 to "اهداف و تسک‌ها",
                    1 to "عادت‌های روزانه"
                )

                sections.forEach { (index, title) ->
                    val isSelected = selectedSectionIndex == index
                    val activeBg = if (isSelected) {
                        ElectricTeal.copy(alpha = 0.22f)
                    } else {
                        Color.Transparent
                    }
                    val activeBorder = if (isSelected) ElectricTeal else Color.Transparent

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = activeBg,
                        border = BorderStroke(1.dp, activeBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                WorldClassOsUtils.HapticSignature.crispTick(context)
                                selectedSectionIndex = index
                            }
                            .testTag("goals_habits_tab_$index")
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) ElectricTeal else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Isolated Content Body per Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            AnimatedContent(
                targetState = selectedSectionIndex,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "goals_habits_section_transition"
            ) { targetIndex ->
                when (targetIndex) {
                    0 -> TasksScreen(
                        tasks = tasks,
                        calendarDisplay = calendarDisplay,
                        activeFocusTaskId = activeFocusTaskId,
                        onStartFocusTimer = onStartFocusTimer,
                        onCreateTask = onCreateTask,
                        onCompleteTask = onCompleteTask,
                        onBulkComplete = onBulkComplete,
                        onUpdateTask = onUpdateTask,
                        onRescheduleTask = onRescheduleTask,
                        onArchiveOrRestore = onArchiveOrRestore,
                        onDeleteTask = onDeleteTask
                    )
                    1 -> HabitsScreen(
                        prefs = prefs,
                        habits = habits,
                        habitRecords = habitRecords,
                        onCreateHabit = onCreateHabit,
                        onToggleHabitDay = onToggleHabitDay
                    )
                }
            }
        }
    }
}


