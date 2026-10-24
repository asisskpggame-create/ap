package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.model.SubtaskItem
import com.example.data.model.Task
import com.example.notifications.ReminderScheduler
import com.example.ui.theme.ElectricTeal
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MutedViolet
import com.example.ui.theme.RoseError
import com.example.util.PersianDateAndUnitUtils
import com.example.util.WorldClassOsUtils
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TasksScreen(
    tasks: List<Task>,
    calendarDisplay: String,
    activeFocusTaskId: String? = null,
    onCreateTask: (
        title: String,
        description: String,
        category: String,
        tags: List<String>,
        priority: String,
        dueDateEpochDay: Long,
        dueTimeMinutes: Int,
        reminderAtMillis: Long,
        estimatedDurationMinutes: Int,
        subtasksJson: String,
        recurrenceRule: String,
        recurrenceIntervalDays: Int,
        notes: String
    ) -> Unit,
    onCompleteTask: (Task) -> Unit,
    onBulkComplete: (List<Task>) -> Unit,
    onUpdateTask: (Task) -> Unit,
    onRescheduleTask: (Task, Long) -> Unit,
    onArchiveOrRestore: (Task, Boolean) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onStartFocusTimer: (Task) -> Unit = {}
) {
    val context = LocalContext.current
    val todayEpochDay = PersianDateAndUnitUtils.currentLocalEpochDay()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedPriorityFilter by remember { mutableStateOf("ALL") }
    var showCreateOrEditDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<Task?>(null) }

    val tabs = listOf(
        "امروز",
        "آینده",
        "همه",
        "عقب‌افتاده",
        "تکمیل‌شده",
        "آرشیو"
    )

    val filteredTasks = tasks.filter { task ->
        val matchesTab = when (selectedTabIndex) {
            0 -> task.status != "ARCHIVED" && task.status != "COMPLETED" && (task.dueDateEpochDay == todayEpochDay || task.dueDateEpochDay < 0)
            1 -> task.status != "ARCHIVED" && task.status != "COMPLETED" && task.dueDateEpochDay > todayEpochDay
            2 -> task.status != "ARCHIVED"
            3 -> task.status != "ARCHIVED" && task.status != "COMPLETED" && task.dueDateEpochDay in 0 until todayEpochDay
            4 -> task.status == "COMPLETED"
            5 -> task.status == "ARCHIVED"
            else -> true
        }
        val matchesSearch = searchQuery.isBlank() ||
            task.title.contains(searchQuery, ignoreCase = true) ||
            task.description.contains(searchQuery, ignoreCase = true) ||
            task.category.contains(searchQuery, ignoreCase = true) ||
            task.tags.any { it.contains(searchQuery, ignoreCase = true) }

        val matchesPriority = selectedPriorityFilter == "ALL" || task.priority == selectedPriorityFilter
        matchesTab && matchesSearch && matchesPriority
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Header + Search + New Task Button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مدیریت پیشرفته تسک‌ها",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val completable = filteredTasks.filter { it.status != "COMPLETED" && it.status != "ARCHIVED" }
                    if (completable.size >= 2) {
                        OutlinedButton(
                            onClick = { onBulkComplete(completable) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تکمیل گروهی", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Button(
                        onClick = {
                            editingTask = null
                            showCreateOrEditDialog = true
                        },
                        modifier = Modifier.testTag("open_create_task_dialog_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تسک جدید")
                    }
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "جستجو") },
                placeholder = { Text("جستجو در عنوان، دسته‌بندی، برچسب‌ها یا یادداشت...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_search_input"),
                shape = RoundedCornerShape(14.dp)
            )

            // Priority filter chips
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "ALL" to "همه اولویت‌ها",
                    "CRITICAL" to "بحرانی",
                    "HIGH" to "بالا",
                    "MEDIUM" to "متوسط",
                    "LOW" to "کم"
                ).forEach { (code, label) ->
                    FilterChip(
                        selected = selectedPriorityFilter == code,
                        onClick = { selectedPriorityFilter = code },
                        label = { Text(label) }
                    )
                }
            }
        }

        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTabIndex == idx,
                    onClick = { selectedTabIndex = idx },
                    text = { Text(title) }
                )
            }
        }

        if (filteredTasks.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "تسکی در این بخش وجود ندارد",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "برای افزودن تسک جدید دکمه «تسک جدید» یا میکروفون صوتی را لمس کنید.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("tasks_lazy_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredTasks, key = { it.id }) { task ->
                    TaskItemCard(
                        task = task,
                        todayEpochDay = todayEpochDay,
                        calendarDisplay = calendarDisplay,
                        isFocusing = activeFocusTaskId == task.id,
                        onComplete = {
                            WorldClassOsUtils.HapticSignature.crispTick(context)
                            onCompleteTask(task)
                        },
                        onStartFocus = {
                            WorldClassOsUtils.HapticSignature.crispTick(context)
                            onStartFocusTimer(task)
                        },
                        onEdit = {
                            editingTask = task
                            showCreateOrEditDialog = true
                        },
                        onToggleSubtask = { updatedSubtasksJson ->
                            WorldClassOsUtils.HapticSignature.crispTick(context)
                            onUpdateTask(task.copy(subtasksJson = updatedSubtasksJson))
                        },
                        onMoveOrder = { delta ->
                            onUpdateTask(task.copy(sortOrder = task.sortOrder + delta))
                        },
                        onRescheduleTomorrow = {
                            val base = if (task.dueDateEpochDay >= todayEpochDay) task.dueDateEpochDay else todayEpochDay
                            onRescheduleTask(task, base + 1L)
                        },
                        onArchiveOrRestore = {
                            onArchiveOrRestore(task, task.status != "ARCHIVED")
                        },
                        onDelete = { onDeleteTask(task) }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    if (showCreateOrEditDialog) {
        TaskEditorDialog(
            initialTask = editingTask,
            todayEpochDay = todayEpochDay,
            onDismiss = { showCreateOrEditDialog = false },
            onSave = { title, desc, cat, tags, prio, dueDay, dueTime, enableReminder, estDur, subtasksJson, recRule, recInterval, notes ->
                val reminderMillis = if (enableReminder && dueTime >= 0) {
                    System.currentTimeMillis() + 60_000L // Schedules reminder notification
                } else {
                    -1L
                }
                if (enableReminder) {
                    ReminderScheduler.scheduleReminder(
                        context = context,
                        notificationId = title.hashCode(),
                        title = "یادآور تسک: $title",
                        body = desc.ifBlank { "دسته: $cat" },
                        triggerAtMillis = System.currentTimeMillis() + 60_000L
                    )
                }
                if (editingTask == null) {
                    onCreateTask(
                        title, desc, cat, tags, prio, dueDay, dueTime,
                        reminderMillis, estDur, subtasksJson, recRule, recInterval, notes
                    )
                } else {
                    onUpdateTask(
                        editingTask!!.copy(
                            title = title,
                            description = desc,
                            category = cat,
                            tags = tags,
                            priority = prio,
                            dueDateEpochDay = dueDay,
                            dueTimeMinutes = dueTime,
                            reminderAtMillis = reminderMillis,
                            estimatedDurationMinutes = estDur,
                            subtasksJson = subtasksJson,
                            recurrenceRule = recRule,
                            recurrenceIntervalDays = recInterval,
                            notes = notes
                        )
                    )
                }
                showCreateOrEditDialog = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskItemCard(
    task: Task,
    todayEpochDay: Long,
    calendarDisplay: String,
    isFocusing: Boolean = false,
    onComplete: () -> Unit,
    onStartFocus: () -> Unit = {},
    onEdit: () -> Unit,
    onToggleSubtask: (String) -> Unit,
    onMoveOrder: (Int) -> Unit,
    onRescheduleTomorrow: () -> Unit,
    onArchiveOrRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val isCompleted = task.status == "COMPLETED"
    val isOverdue = !isCompleted && task.dueDateEpochDay in 0 until todayEpochDay
    val subtasks = remember(task.subtasksJson) { parseSubtasksJson(task.subtasksJson) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_card_${task.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(
            width = 1.dp,
            color = when {
                isFocusing -> ElectricTeal
                isOverdue -> RoseError.copy(alpha = 0.6f)
                isCompleted -> EmeraldSuccess.copy(alpha = 0.4f)
                else -> MaterialTheme.colorScheme.outline
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onComplete,
                    enabled = !isCompleted,
                    modifier = Modifier.testTag("complete_task_btn_${task.id}")
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "تکمیل تسک",
                        tint = if (isCompleted) EmeraldSuccess else MaterialTheme.colorScheme.primary
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                    if (task.description.isNotBlank()) {
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (!isCompleted) {
                    IconButton(
                        onClick = onStartFocus,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("focus_task_btn_${task.id}")
                    ) {
                        Icon(
                            imageVector = if (isFocusing) Icons.Default.Timer else Icons.Default.PlayArrow,
                            contentDescription = "شروع تایمر تمرکز و پومودورو",
                            tint = if (isFocusing) ElectricTeal else MutedViolet
                        )
                    }
                }

                PriorityBadge(priority = task.priority)
            }

            // Metadata Badges (Date, Time, Category, Recurrence, Duration, Actual Focus)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MetaChip(text = "📁 ${task.category}")
                if (task.dueDateEpochDay >= 0) {
                    val dateLabel = PersianDateAndUnitUtils.formatEpochDay(task.dueDateEpochDay, calendarDisplay)
                    val timeLabel = if (task.dueTimeMinutes >= 0) " • ${PersianDateAndUnitUtils.formatTimeMinutes(task.dueTimeMinutes)}" else ""
                    MetaChip(
                        text = "📅 $dateLabel$timeLabel",
                        isAlert = isOverdue
                    )
                }
                if (task.recurrenceRule != "NONE") {
                    val recText = when (task.recurrenceRule) {
                        "DAILY" -> "تکرار روزانه"
                        "WEEKLY" -> "تکرار هفتگی"
                        "MONTHLY" -> "تکرار ماهانه"
                        else -> "تکرار هر ${PersianDateAndUnitUtils.toPersianDigits(task.recurrenceIntervalDays.toString())} روز"
                    }
                    MetaChip(text = "🔁 $recText")
                }
                if (task.estimatedDurationMinutes > 0) {
                    MetaChip(text = "⏱ تخمین: ${PersianDateAndUnitUtils.toPersianDigits(task.estimatedDurationMinutes.toString())} دقیقه")
                }
                if (task.actualDurationMinutes > 0) {
                    MetaChip(text = "🎯 تمرکز واقعی: ${PersianDateAndUnitUtils.toPersianDigits(task.actualDurationMinutes.toString())} دقیقه")
                }
                task.tags.forEach { tag ->
                    MetaChip(text = "#$tag")
                }
            }

            // Checklist Subtasks
            if (subtasks.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    subtasks.forEachIndexed { idx, sub ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = sub.completed,
                                onCheckedChange = { checked ->
                                    val updated = subtasks.toMutableList().also {
                                        it[idx] = sub.copy(completed = checked)
                                    }
                                    onToggleSubtask(encodeSubtasksJson(updated))
                                },
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = sub.title,
                                style = MaterialTheme.typography.bodySmall,
                                textDecoration = if (sub.completed) TextDecoration.LineThrough else TextDecoration.None
                            )
                        }
                    }
                }
            }

            // Action Row: Focus, Reschedule, Reorder, Edit, Archive, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isCompleted) {
                    TextButton(onClick = onStartFocus) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp), tint = ElectricTeal)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isFocusing) "در حال تمرکز" else "تمرکز ۲۵د", color = ElectricTeal)
                    }
                    TextButton(onClick = onRescheduleTomorrow) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+۱ روز")
                    }
                }
                IconButton(onClick = { onMoveOrder(-1) }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "بالا", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { onMoveOrder(1) }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "پایین", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "ویرایش", tint = ElectricTeal, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onArchiveOrRestore, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = if (task.status == "ARCHIVED") Icons.Default.Unarchive else Icons.Default.Archive,
                        contentDescription = "آرشیو",
                        tint = MutedViolet,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = RoseError, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun MetaChip(text: String, isAlert: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isAlert) RoseError.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (isAlert) RoseError else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun TaskEditorDialog(
    initialTask: Task?,
    todayEpochDay: Long,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        description: String,
        category: String,
        tags: List<String>,
        priority: String,
        dueDateEpochDay: Long,
        dueTimeMinutes: Int,
        enableReminder: Boolean,
        estimatedDurationMinutes: Int,
        subtasksJson: String,
        recurrenceRule: String,
        recurrenceIntervalDays: Int,
        notes: String
    ) -> Unit
) {
    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var description by remember { mutableStateOf(initialTask?.description ?: "") }
    var category by remember { mutableStateOf(initialTask?.category ?: "کاری") }
    var tagsText by remember { mutableStateOf(initialTask?.tags?.joinToString(", ") ?: "") }
    var priority by remember { mutableStateOf(initialTask?.priority ?: "MEDIUM") }
    var dateOffset by remember {
        mutableIntStateOf(
            if (initialTask != null && initialTask.dueDateEpochDay >= 0) {
                (initialTask.dueDateEpochDay - todayEpochDay).toInt()
            } else 0
        )
    }
    var dueHourText by remember {
        mutableStateOf(
            if (initialTask != null && initialTask.dueTimeMinutes >= 0) (initialTask.dueTimeMinutes / 60).toString() else "9"
        )
    }
    var enableReminder by remember { mutableStateOf((initialTask?.reminderAtMillis ?: -1L) > 0) }
    var durationText by remember { mutableStateOf((initialTask?.estimatedDurationMinutes ?: 30).toString()) }
    var subtasksRaw by remember {
        mutableStateOf(
            parseSubtasksJson(initialTask?.subtasksJson ?: "[]").joinToString("\n") { it.title }
        )
    }
    var recurrenceRule by remember { mutableStateOf(initialTask?.recurrenceRule ?: "NONE") }
    var customIntervalText by remember { mutableStateOf((initialTask?.recurrenceIntervalDays ?: 3).toString()) }
    var notes by remember { mutableStateOf(initialTask?.notes ?: "") }

    // Live In-Line Smart Persian NLP Parser
    val liveNlp = remember(title) { WorldClassOsUtils.parseInlinePersianTask(title) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (initialTask == null) "ایجاد تسک هوشمند (شیت پایین‌صفحه)" else "ویرایش تسک",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = title,
                onValueChange = { newText ->
                    title = newText
                    // Automatically apply detected inline NLP tokens while typing
                    val parsed = WorldClassOsUtils.parseInlinePersianTask(newText)
                    parsed.detectedDateOffsetDays?.let { dateOffset = it }
                    parsed.detectedTimeMinutes?.let { dueHourText = (it / 60).toString() }
                    parsed.detectedPriority?.let { priority = it }
                    parsed.detectedDurationMinutes?.let { durationText = it.toString() }
                    parsed.detectedCategory?.let { category = it }
                },
                visualTransformation = WorldClassOsUtils.PersianNlpVisualTransformation,
                label = { Text("عنوان تسک * (مثلاً: فردا ساعت ۵ عصر جلسه با علی #فوری)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_dialog_title_input")
            )

            if (liveNlp.detectedTokensSummary.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ElectricTeal.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, ElectricTeal.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = ElectricTeal,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "تشخیص هوشمند حین تایپ: ${liveNlp.detectedTokensSummary.joinToString(" • ")}",
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricTeal,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("توضیحات") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("دسته‌بندی") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = PersianDateAndUnitUtils.toEnglishDigits(it) },
                    label = { Text("مدت (دقیقه)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Text("اولویت:", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("LOW" to "کم", "MEDIUM" to "متوسط", "HIGH" to "بالا", "CRITICAL" to "بحرانی").forEach { (code, lbl) ->
                    FilterChip(
                        selected = priority == code,
                        onClick = { priority = code },
                        label = { Text(lbl) }
                    )
                }
            }

            Text("زمان انجام:", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0 to "امروز", 1 to "فردا", 2 to "پس‌فردا", 7 to "هفته آینده").forEach { (offset, lbl) ->
                    FilterChip(
                        selected = dateOffset == offset,
                        onClick = { dateOffset = offset },
                        label = { Text(lbl) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = dueHourText,
                    onValueChange = { dueHourText = PersianDateAndUnitUtils.toEnglishDigits(it) },
                    label = { Text("ساعت (۰ تا ۲۳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = enableReminder,
                    onClick = { enableReminder = !enableReminder },
                    label = { Text("اعلان یادآور") }
                )
            }

            Text("تکرار (Recurrence):", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "NONE" to "بدون تکرار",
                    "DAILY" to "روزانه",
                    "WEEKLY" to "هفتگی",
                    "MONTHLY" to "ماهانه",
                    "CUSTOM" to "سفارشی"
                ).forEach { (rule, lbl) ->
                    FilterChip(
                        selected = recurrenceRule == rule,
                        onClick = { recurrenceRule = rule },
                        label = { Text(lbl) }
                    )
                }
            }

            if (recurrenceRule == "CUSTOM") {
                OutlinedTextField(
                    value = customIntervalText,
                    onValueChange = { customIntervalText = PersianDateAndUnitUtils.toEnglishDigits(it) },
                    label = { Text("فاصله تکرار (چند روز یک‌بار؟)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = subtasksRaw,
                onValueChange = { subtasksRaw = it },
                label = { Text("چک‌لیست زیرتسک‌ها (هر خط یک مورد)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            OutlinedTextField(
                value = tagsText,
                onValueChange = { tagsText = it },
                label = { Text("برچسب‌ها (با ویرگول جدا کنید)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("یادداشت تکمیلی") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("انصراف")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            val tags = tagsText.split(",", "،").map { it.trim() }.filter { it.isNotBlank() }
                            val hour = dueHourText.toIntOrNull()?.coerceIn(0, 23) ?: 9
                            val subItems = subtasksRaw.lines()
                                .map { it.trim() }
                                .filter { it.isNotBlank() }
                                .mapIndexed { i, s -> SubtaskItem(id = "sub_$i", title = s, completed = false) }
                            val cleanSavedTitle = liveNlp.cleanedTitle.ifBlank { title.trim() }
                            onSave(
                                cleanSavedTitle,
                                description.trim(),
                                category.trim().ifBlank { "عمومی" },
                                tags,
                                priority,
                                todayEpochDay + dateOffset,
                                hour * 60,
                                enableReminder,
                                durationText.toIntOrNull() ?: 30,
                                encodeSubtasksJson(subItems),
                                recurrenceRule,
                                customIntervalText.toIntOrNull() ?: 1,
                                notes.trim()
                            )
                        }
                    },
                    enabled = title.isNotBlank(),
                    modifier = Modifier.testTag("save_task_dialog_button")
                ) {
                    Text("ذخیره تسک")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun parseSubtasksJson(json: String): List<SubtaskItem> {
    if (json.isBlank() || json == "[]") return emptyList()
    return try {
        val arr = JSONArray(json)
        val list = mutableListOf<SubtaskItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                SubtaskItem(
                    id = obj.optString("id", "sub_$i"),
                    title = obj.optString("title", ""),
                    completed = obj.optBoolean("completed", false)
                )
            )
        }
        list
    } catch (e: Exception) {
        emptyList()
    }
}

private fun encodeSubtasksJson(items: List<SubtaskItem>): String {
    val arr = JSONArray()
    for (item in items) {
        arr.put(
            JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("completed", item.completed)
            }
        )
    }
    return arr.toString()
}
