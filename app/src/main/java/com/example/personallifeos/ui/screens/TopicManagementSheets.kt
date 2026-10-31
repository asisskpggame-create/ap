package com.example.personallifeos.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.personallifeos.data.model.DEFAULT_TOPIC_PRESET_TEMPLATES
import com.example.personallifeos.data.model.ReminderRepeatMode
import com.example.personallifeos.data.model.TaskCategory
import com.example.personallifeos.data.model.TaskItem
import com.example.personallifeos.data.model.TaskPriority
import com.example.personallifeos.data.model.TaskStatus
import com.example.personallifeos.data.model.TopicDashboardStats
import com.example.personallifeos.data.model.TopicItem
import com.example.personallifeos.data.model.TopicPresetTemplate
import com.example.personallifeos.notification.SmartReminderManager
import com.example.personallifeos.ui.components.AppSvgIcons
import com.example.personallifeos.ui.theme.AmberWarning
import com.example.personallifeos.ui.theme.BgDark
import com.example.personallifeos.ui.theme.BorderCyanGlow
import com.example.personallifeos.ui.theme.CyanPrimary
import com.example.personallifeos.ui.theme.EmeraldAccent
import com.example.personallifeos.ui.theme.RoseDanger
import com.example.personallifeos.ui.theme.SurfaceCard
import com.example.personallifeos.ui.theme.SurfaceDark
import com.example.personallifeos.ui.theme.TextMuted
import com.example.personallifeos.ui.theme.TextPrimary
import com.example.personallifeos.ui.theme.TextSecondary
import com.example.personallifeos.util.PersianUtils

private val TOPIC_COLOR_OPTIONS = listOf(
    0xFF06B6D4L, // Cyan
    0xFF10B981L, // Emerald
    0xFFF43F5EL, // Rose
    0xFFF59E0BL, // Amber
    0xFF8B5CF6L, // Violet
    0xFF3B82F6L  // Blue
)

/**
 * 1. Modern Add Button Bottom-Sheet / Modal Chooser using pure Vector SVG icons:
 * Displays two visually distinct options when tapping the '+' button in «اهداف و مسیر»:
 * - گزینه اول: ایجاد وظیفه جدید (New Task)
 * - گزینه دوم: ایجاد تاپیک جدید (New Topic)
 */
@Composable
fun AddTaskOrTopicChooserSheet(
    isOpen: Boolean,
    topics: List<TopicItem>,
    initialSelectedTopicId: String? = null,
    initialMode: Int = 0, // 0 = Chooser, 1 = New Task Form, 2 = New Topic Form
    onDismiss: () -> Unit,
    onCreateTask: (
        title: String,
        category: TaskCategory,
        priority: TaskPriority,
        dueDate: String,
        description: String,
        topicId: String?,
        isMajorMission: Boolean,
        reminderDateTime: String,
        reminderEpochMillis: Long,
        reminderRepeatMode: ReminderRepeatMode,
        subtaskTitles: List<String>
    ) -> Unit,
    onCreateTopic: (
        name: String,
        description: String,
        iconEmoji: String,
        colorHex: Long,
        startDate: String,
        endDate: String,
        priority: TaskPriority,
        categoryTag: String,
        reminderEnabled: Boolean,
        reminderTime: String
    ) -> Unit,
    onCreateTopicFromTemplate: (TopicPresetTemplate) -> Unit
) {
    if (!isOpen) return

    val context = LocalContext.current
    var mode by remember(isOpen, initialMode) { mutableIntStateOf(initialMode) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    // Task form states
    var taskTitle by remember(isOpen) { mutableStateOf("") }
    var taskDescription by remember(isOpen) { mutableStateOf("") }
    var taskPriority by remember(isOpen) { mutableStateOf(TaskPriority.HIGH) }
    var taskCategory by remember(isOpen) { mutableStateOf(TaskCategory.WORK) }
    var taskDueDate by remember(isOpen) { mutableStateOf("۱۴۰۵/۰۸/۰۷") }
    var selectedTopicId by remember(isOpen, initialSelectedTopicId) { mutableStateOf(initialSelectedTopicId) }
    var isMajorMission by remember(isOpen) { mutableStateOf(false) }
    var enableTaskReminder by remember(isOpen) { mutableStateOf(true) }
    var taskReminderTime by remember(isOpen) { mutableStateOf("۱۴۰۵/۰۸/۰۷ - ۱۸:۰۰") }
    var taskReminderDelayMinutes by remember(isOpen) { mutableIntStateOf(15) }
    var taskRepeatMode by remember(isOpen) { mutableStateOf(ReminderRepeatMode.ONCE) }
    var subtaskInput by remember(isOpen) { mutableStateOf("") }
    val subtaskDrafts = remember(isOpen) { mutableStateListOf<String>() }

    // Topic form states (using SVG key instead of emoji)
    var topicName by remember(isOpen) { mutableStateOf("") }
    var topicDesc by remember(isOpen) { mutableStateOf("") }
    var topicIconKey by remember(isOpen) { mutableStateOf("SVG_UNIVERSITY") }
    var topicColor by remember(isOpen) { mutableLongStateOf(0xFF06B6D4L) }
    var topicStartDate by remember(isOpen) { mutableStateOf("۱۴۰۵/۰۸/۰۵") }
    var topicEndDate by remember(isOpen) { mutableStateOf("۱۴۰۵/۱۲/۲۹") }
    var topicPriority by remember(isOpen) { mutableStateOf(TaskPriority.HIGH) }
    var topicTag by remember(isOpen) { mutableStateOf("پروژه و مسیر") }
    var topicReminderEnabled by remember(isOpen) { mutableStateOf(true) }
    var topicReminderTime by remember(isOpen) { mutableStateOf("هر روز ۲۰:۰۰") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f))
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = BgDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderCyanGlow, RoundedCornerShape(28.dp))
                    .testTag("add_task_or_topic_chooser_sheet")
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = when (mode) {
                                        0 -> "افزودن به اهداف و مسیر"
                                        1 -> "ایجاد وظیفه و مأموریت جدید"
                                        else -> "ساخت تاپیک و فضای موضوعی جدید"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = when (mode) {
                                        0 -> "نوع آیتم مورد نظر خود را انتخاب کنید"
                                        1 -> "ثبت کار مستقل یا متصل به تاپیک همراه با یادآوری هوشمند"
                                        else -> "ایجاد فضای مستقل برای مدیریت پروژه‌ها، مأموریت‌ها و چک‌لیست‌ها"
                                    },
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "بستن", tint = TextSecondary)
                            }
                        }
                    }

                    // =========================================================
                    // MODE 0: TWO VISUALLY DISTINCT CHOOSER CARDS
                    // =========================================================
                    if (mode == 0) {
                        item {
                            // Option 1: New Task
                            Card(
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.5.dp, CyanPrimary, RoundedCornerShape(22.dp))
                                    .clickable { mode = 1 }
                                    .testTag("choose_new_task_card")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(Color(0x2606B6D4), SurfaceCard)
                                            )
                                        )
                                        .padding(18.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(CyanPrimary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.TaskAlt,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "ایجاد وظیفه جدید (New Task)",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "ثبت یک کار مستقل یا مأموریت روزانه با اولویت، زیرمجموعه (Subtasks) و اعلان واقعی اندروید.",
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            // Option 2: New Topic
                            Card(
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.5.dp, EmeraldAccent, RoundedCornerShape(22.dp))
                                    .clickable { mode = 2 }
                                    .testTag("choose_new_topic_card")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(Color(0x2610B981), SurfaceCard)
                                            )
                                        )
                                        .padding(18.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(EmeraldAccent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.FolderSpecial,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "ایجاد تاپیک جدید (New Topic)",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "ساخت یک فضای موضوعی مستقل (دانشگاه، فیلم‌برداری، کسب‌وکار و...) با آیکون وکتور SVG و داشبورد پیشرفت واقعی.",
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // =========================================================
                    // MODE 1: CREATE NEW TASK (INDEPENDENT OR TOPIC-LINKED)
                    // =========================================================
                    if (mode == 1) {
                        item {
                            OutlinedTextField(
                                value = taskTitle,
                                onValueChange = { taskTitle = it },
                                label = { Text("عنوان وظیفه یا مأموریت") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("new_task_title_input"),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyanPrimary,
                                    unfocusedBorderColor = Color(0x33FFFFFF)
                                )
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = taskDescription,
                                onValueChange = { taskDescription = it },
                                label = { Text("توضیحات و جزئیات اجرایی (اختیاری)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        // Topic Attachment Selector (Independent vs Topic with SVG Icon)
                        item {
                            Text(
                                text = "فضای موضوعی (تاپیک):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (selectedTopicId == null) CyanPrimary else SurfaceCard)
                                            .border(0.5.dp, CyanPrimary, RoundedCornerShape(10.dp))
                                            .clickable { selectedTopicId = null }
                                            .padding(horizontal = 12.dp, vertical = 7.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.PushPin,
                                            contentDescription = null,
                                            tint = if (selectedTopicId == null) Color.Black else CyanPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "وظیفه مستقل (بدون تاپیک)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedTopicId == null) Color.Black else TextPrimary
                                        )
                                    }
                                }
                                items(topics.filter { !it.isArchived }) { topic ->
                                    val isSelected = selectedTopicId == topic.id
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) Color(topic.colorHex) else SurfaceCard)
                                            .border(0.5.dp, Color(topic.colorHex), RoundedCornerShape(10.dp))
                                            .clickable { selectedTopicId = topic.id }
                                            .padding(horizontal = 12.dp, vertical = 7.dp)
                                    ) {
                                        AppSvgIcons.TopicSvgIcon(
                                            iconKey = topic.iconEmoji,
                                            tint = if (isSelected) Color.Black else Color(topic.colorHex),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = topic.name,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.Black else TextPrimary
                                        )
                                    }
                                }
                            }
                        }

                        // Priority & Due Date
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = taskDueDate,
                                    onValueChange = { taskDueDate = it },
                                    label = { Text("مهلت انجام (شمسی)") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isMajorMission) Color(0x33F59E0B) else SurfaceCard)
                                        .clickable { isMajorMission = !isMajorMission }
                                        .padding(horizontal = 10.dp, vertical = 12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (isMajorMission) AmberWarning else TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "وظیفه اصلی",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isMajorMission) AmberWarning else TextSecondary
                                    )
                                }
                            }
                        }

                        item {
                            Text("تعیین اولویت:", fontSize = 12.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TaskPriority.entries.forEach { p ->
                                    Button(
                                        onClick = { taskPriority = p },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (taskPriority == p) CyanPrimary else SurfaceCard
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = p.titleFa,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (taskPriority == p) Color.Black else TextPrimary
                                        )
                                    }
                                }
                            }
                        }

                        // Subtasks / Checklist Builder
                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Checklist, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "زیرمجموعه‌ها و چک‌لیست (Subtasks)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CyanPrimary
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = subtaskInput,
                                            onValueChange = { subtaskInput = it },
                                            placeholder = { Text("عنوان زیرمجموعه...", fontSize = 11.sp) },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true
                                        )
                                        Button(
                                            onClick = {
                                                if (subtaskInput.isNotBlank()) {
                                                    subtaskDrafts.add(subtaskInput.trim())
                                                    subtaskInput = ""
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("+ افزودن", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    subtaskDrafts.forEachIndexed { idx, sub ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(SurfaceDark)
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${PersianUtils.formatPersianNumber((idx + 1).toLong())}. $sub",
                                                fontSize = 11.sp,
                                                color = TextPrimary
                                            )
                                            IconButton(
                                                onClick = { subtaskDrafts.removeAt(idx) },
                                                modifier = Modifier.size(22.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = null, tint = RoseDanger, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Smart Reminder Configuration
                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "یادآوری هوشمند و اعلان واقعی اندروید",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                        }
                                        Switch(
                                            checked = enableTaskReminder,
                                            onCheckedChange = { checked ->
                                                enableTaskReminder = checked
                                                if (checked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                                    !SmartReminderManager.hasPostNotificationPermission(context)
                                                ) {
                                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                }
                                            },
                                            colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = EmeraldAccent)
                                        )
                                    }

                                    if (enableTaskReminder) {
                                        OutlinedTextField(
                                            value = taskReminderTime,
                                            onValueChange = { taskReminderTime = it },
                                            label = { Text("تاریخ و ساعت یادآوری") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                        Text("زمان‌بندی آلارم واقعی سیستم و الگوی تکرار:", fontSize = 11.sp, color = TextMuted)
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            items(listOf(1 to "۱ دقیقه دیگر", 15 to "۱۵ دقیقه دیگر", 60 to "۱ ساعت دیگر", 1440 to "فردا همین ساعت")) { (mins, label) ->
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(if (taskReminderDelayMinutes == mins) AmberWarning else SurfaceDark)
                                                        .clickable { taskReminderDelayMinutes = mins }
                                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                                ) {
                                                    Text(
                                                        text = label,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (taskReminderDelayMinutes == mins) Color.Black else TextSecondary
                                                    )
                                                }
                                            }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            ReminderRepeatMode.entries.forEach { rep ->
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(if (taskRepeatMode == rep) CyanPrimary else SurfaceDark)
                                                        .clickable { taskRepeatMode = rep }
                                                        .padding(vertical = 6.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = rep.titleFa,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (taskRepeatMode == rep) Color.Black else TextSecondary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Action Buttons for Mode 1
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { if (initialMode == 0) mode = 0 else onDismiss() },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (initialMode == 0) "بازگشت" else "انصراف", color = TextSecondary)
                                }
                                Button(
                                    onClick = {
                                        if (taskTitle.isNotBlank()) {
                                            val epoch = if (enableTaskReminder) {
                                                System.currentTimeMillis() + taskReminderDelayMinutes * 60_000L
                                            } else 0L
                                            onCreateTask(
                                                taskTitle.trim(),
                                                taskCategory,
                                                taskPriority,
                                                taskDueDate.trim(),
                                                taskDescription.trim(),
                                                selectedTopicId,
                                                isMajorMission,
                                                if (enableTaskReminder) taskReminderTime.trim() else "",
                                                epoch,
                                                taskRepeatMode,
                                                subtaskDrafts.toList()
                                            )
                                            onDismiss()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1.5f)
                                        .testTag("confirm_add_task_btn")
                                ) {
                                    Text("ثبت وظیفه", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }

                    // =========================================================
                    // MODE 2: CREATE NEW TOPIC (CUSTOM OR PRESET TEMPLATE WITH SVG)
                    // =========================================================
                    if (mode == 2) {
                        // Quick Preset Templates Row with SVG Vector Icons
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "ساخت سریع از قالب آماده استاندارد:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldAccent
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(DEFAULT_TOPIC_PRESET_TEMPLATES) { tpl ->
                                    Card(
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                        modifier = Modifier
                                            .border(1.dp, Color(tpl.colorHex), RoundedCornerShape(14.dp))
                                            .clickable {
                                                onCreateTopicFromTemplate(tpl)
                                                onDismiss()
                                            }
                                            .testTag("preset_topic_${tpl.iconEmoji}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(tpl.colorHex).copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                AppSvgIcons.TopicSvgIcon(
                                                    iconKey = tpl.iconEmoji,
                                                    tint = Color(tpl.colorHex),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(
                                                    text = tpl.name,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    text = "${PersianUtils.formatPersianNumber(tpl.defaultMissions.size.toLong())} مأموریت آماده • ساخت آنی",
                                                    fontSize = 10.sp,
                                                    color = Color(tpl.colorHex)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            OutlinedTextField(
                                value = topicName,
                                onValueChange = { topicName = it },
                                label = { Text("نام تاپیک (مثلاً: دانشگاه، پروژه فیلم‌برداری)") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("new_topic_name_input"),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmeraldAccent,
                                    unfocusedBorderColor = Color(0x33FFFFFF)
                                )
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = topicDesc,
                                onValueChange = { topicDesc = it },
                                label = { Text("توضیحات و هدف تاپیک") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        // SVG Vector Icon Picker (Replacing Emoji Picker)
                        item {
                            Text("انتخاب آیکون وکتور اختصاصی (SVG):", fontSize = 12.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(AppSvgIcons.TOPIC_SVG_OPTIONS) { svgOpt ->
                                    val isSelected = topicIconKey == svgOpt.key
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.clickable { topicIconKey = svgOpt.key }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isSelected) Color(topicColor) else SurfaceCard)
                                                .border(
                                                    width = if (isSelected) 2.dp else 0.5.dp,
                                                    color = if (isSelected) Color.White else Color(0x33FFFFFF),
                                                    shape = RoundedCornerShape(12.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AppSvgIcons.TopicSvgIcon(
                                                iconKey = svgOpt.key,
                                                tint = if (isSelected) Color.Black else Color(topicColor),
                                                contentDescription = svgOpt.labelFa,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Text(
                                            text = svgOpt.labelFa,
                                            fontSize = 9.sp,
                                            color = if (isSelected) Color(topicColor) else TextMuted
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Text("انتخاب رنگ اختصاصی تاپیک:", fontSize = 12.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                TOPIC_COLOR_OPTIONS.forEach { hex ->
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(Color(hex))
                                            .border(
                                                width = if (topicColor == hex) 2.5.dp else 0.dp,
                                                color = if (topicColor == hex) Color.White else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable { topicColor = hex }
                                    )
                                }
                            }
                        }

                        // Start Date, End Date, Category Tag
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = topicStartDate,
                                    onValueChange = { topicStartDate = it },
                                    label = { Text("تاریخ شروع") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = topicEndDate,
                                    onValueChange = { topicEndDate = it },
                                    label = { Text("تاریخ پایان (اختیاری)") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                        }

                        item {
                            OutlinedTextField(
                                value = topicTag,
                                onValueChange = { topicTag = it },
                                label = { Text("دسته‌بندی یا برچسب (مثلاً: تحصیلی، سینمایی، شغلی)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        item {
                            Text("اولویت تاپیک:", fontSize = 12.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TaskPriority.entries.forEach { p ->
                                    Button(
                                        onClick = { topicPriority = p },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (topicPriority == p) Color(topicColor) else SurfaceCard
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = p.titleFa,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (topicPriority == p) Color.Black else TextPrimary
                                        )
                                    }
                                }
                            }
                        }

                        // Topic Reminder Settings
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceCard)
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("یادآوری اختصاصی تاپیک", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                    Text("ارسال اعلان مرور مأموریت‌های این تاپیک ($topicReminderTime)", fontSize = 10.sp, color = TextMuted)
                                }
                                Switch(
                                    checked = topicReminderEnabled,
                                    onCheckedChange = { topicReminderEnabled = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = EmeraldAccent)
                                )
                            }
                        }

                        // Action Buttons for Mode 2
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { if (initialMode == 0) mode = 0 else onDismiss() },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (initialMode == 0) "بازگشت" else "انصراف", color = TextSecondary)
                                }
                                Button(
                                    onClick = {
                                        if (topicName.isNotBlank()) {
                                            onCreateTopic(
                                                topicName.trim(),
                                                topicDesc.trim().ifBlank { "مدیریت متمرکز مأموریت‌های ${topicName.trim()}" },
                                                topicIconKey,
                                                topicColor,
                                                topicStartDate.trim(),
                                                topicEndDate.trim(),
                                                topicPriority,
                                                topicTag.trim().ifBlank { "عمومی" },
                                                topicReminderEnabled,
                                                topicReminderTime.trim()
                                            )
                                            onDismiss()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1.5f)
                                        .testTag("confirm_add_topic_btn")
                                ) {
                                    Text("ایجاد تاپیک جدید", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3 & 4. Dedicated Topic Workspace & Real-Time Topic Mini-Dashboard (using pure SVG vector icons)
 */
@Composable
fun TopicWorkspaceDialog(
    topic: TopicItem,
    allTopics: List<TopicItem>,
    topicTasks: List<TaskItem>,
    onDismiss: () -> Unit,
    onOpenNewTaskForTopic: (String) -> Unit,
    onUpdateTaskStatus: (TaskItem, TaskStatus) -> Unit,
    onToggleMajorMission: (TaskItem) -> Unit,
    onAddSubtask: (TaskItem, String) -> Unit,
    onToggleSubtask: (TaskItem, String) -> Unit,
    onMoveTaskToTopic: (TaskItem, String?) -> Unit,
    onTriggerRealNotificationForTask: (TaskItem) -> Unit,
    onDeleteTask: (String) -> Unit,
    onToggleTopicReminder: (TopicItem) -> Unit,
    onToggleTopicArchive: (TopicItem) -> Unit,
    onDuplicateTopicAsTemplate: (TopicItem) -> Unit,
    onUpdateTopicNote: (TopicItem, String) -> Unit,
    onDeleteTopicConfirmed: (TopicItem, Boolean) -> Unit
) {
    val context = LocalContext.current
    val brandColor = Color(topic.colorHex)
    val stats = remember(topic.id, topicTasks) {
        TopicDashboardStats.compute(topic.id, topicTasks)
    }

    var workspaceTab by remember { mutableIntStateOf(0) } // 0 = Missions, 1 = Topic Note & History
    var statusFilter by remember { mutableStateOf<TaskStatus?>(null) }
    var sortByPriority by remember { mutableStateOf(true) }
    var noteDraft by remember(topic.topicNote) { mutableStateOf(topic.topicNote) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var taskToMove by remember { mutableStateOf<TaskItem?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    val filteredMissions = remember(topicTasks, statusFilter, sortByPriority) {
        val list = topicTasks.filter { statusFilter == null || it.status == statusFilter }
        if (sortByPriority) {
            list.sortedWith(
                compareByDescending<TaskItem> { it.isMajorMission }
                    .thenBy { it.priority.ordinal }
            )
        } else {
            list.sortedBy { it.dueDate }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BgDark)
                .testTag("topic_workspace_dialog_${topic.id}")
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // =============================================================
                // 4. DEDICATED TOPIC MINI-DASHBOARD CARD
                // =============================================================
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, brandColor, RoundedCornerShape(24.dp))
                            .testTag("topic_dashboard_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(brandColor.copy(alpha = 0.18f), SurfaceCard)
                                    )
                                )
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Top Bar inside Dashboard with SVG Icon
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(brandColor.copy(alpha = 0.25f))
                                            .border(1.dp, brandColor, RoundedCornerShape(14.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AppSvgIcons.TopicSvgIcon(
                                            iconKey = topic.iconEmoji,
                                            tint = brandColor,
                                            contentDescription = topic.name,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = topic.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = TextPrimary
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(brandColor.copy(alpha = 0.2f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = topic.categoryTag,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = brandColor
                                                )
                                            }
                                        }
                                        Text(
                                            text = topic.description,
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                                IconButton(onClick = onDismiss) {
                                    Icon(Icons.Default.Close, contentDescription = "بستن تاپیک", tint = TextPrimary)
                                }
                            }

                            // Real Progress Bar (Strictly calculated from actual tasks)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "درصد پیشرفت واقعی (محاسبه‌شده از مأموریت‌ها):",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "${PersianUtils.formatPersianNumber(stats.progressPercent.toLong())}٪ (${PersianUtils.formatPersianNumber(stats.completedTasks.toLong())} از ${PersianUtils.formatPersianNumber(stats.totalTasks.toLong())})",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = brandColor,
                                        modifier = Modifier.testTag("topic_real_progress_text")
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { stats.progressPercent / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(5.dp)),
                                    color = brandColor,
                                    trackColor = SurfaceDark
                                )
                            }

                            // 4-Metric Grid in Topic Dashboard
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TopicStatBox(
                                    label = "کل وظایف",
                                    value = PersianUtils.formatPersianNumber(stats.totalTasks.toLong()),
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                TopicStatBox(
                                    label = "تکمیل‌شده",
                                    value = PersianUtils.formatPersianNumber(stats.completedTasks.toLong()),
                                    color = EmeraldAccent,
                                    modifier = Modifier.weight(1f)
                                )
                                TopicStatBox(
                                    label = "باقی‌مانده",
                                    value = PersianUtils.formatPersianNumber(stats.remainingTasks.toLong()),
                                    color = CyanPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                TopicStatBox(
                                    label = "عقب‌افتاده",
                                    value = PersianUtils.formatPersianNumber(stats.overdueTasks.toLong()),
                                    color = if (stats.overdueTasks > 0) RoseDanger else TextMuted,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SurfaceDark)
                                        .padding(10.dp)
                                ) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "نزدیک‌ترین مهلت: ${stats.nearestDeadline ?: "ثبت نشده"}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AmberWarning
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SurfaceDark)
                                        .padding(10.dp)
                                ) {
                                    Icon(Icons.Default.PriorityHigh, contentDescription = null, tint = RoseDanger, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "مهم و فوری: ${PersianUtils.formatPersianNumber(stats.urgentOrMajorTasks.size.toLong())} مورد",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = RoseDanger
                                    )
                                }
                            }

                            // Topic Action Toolbar
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item {
                                    Button(
                                        onClick = { onOpenNewTaskForTopic(topic.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("افزودن مأموریت جدید", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                item {
                                    OutlinedButton(
                                        onClick = { onToggleTopicReminder(topic) },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            if (topic.reminderEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                            contentDescription = null,
                                            tint = if (topic.reminderEnabled) EmeraldAccent else TextMuted,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (topic.reminderEnabled) "یادآوری فعال (${topic.reminderTime ?: "روزانه"})" else "یادآوری غیرفعال",
                                            fontSize = 11.sp,
                                            color = TextPrimary
                                        )
                                    }
                                }
                                item {
                                    OutlinedButton(
                                        onClick = { onDuplicateTopicAsTemplate(topic) },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("کپی به‌عنوان الگو", fontSize = 11.sp, color = TextPrimary)
                                    }
                                }
                                item {
                                    OutlinedButton(
                                        onClick = { onToggleTopicArchive(topic) },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            if (topic.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                            contentDescription = null,
                                            tint = AmberWarning,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (topic.isArchived) "بازیابی از آرشیو" else "آرشیو تاپیک",
                                            fontSize = 11.sp,
                                            color = AmberWarning
                                        )
                                    }
                                }
                                item {
                                    OutlinedButton(
                                        onClick = { showDeleteConfirm = true },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = RoseDanger, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("حذف تاپیک", fontSize = 11.sp, color = RoseDanger)
                                    }
                                }
                            }
                        }
                    }
                }

                // Sub-navigation inside Topic Workspace (using SVG Icons)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { workspaceTab = 0 },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (workspaceTab == 0) brandColor else SurfaceCard
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Checklist,
                                contentDescription = null,
                                tint = if (workspaceTab == 0) Color.Black else brandColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "مأموریت‌ها و چک‌لیست (${PersianUtils.formatPersianNumber(topicTasks.size.toLong())})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (workspaceTab == 0) Color.Black else TextPrimary
                            )
                        }
                        Button(
                            onClick = { workspaceTab = 1 },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (workspaceTab == 1) brandColor else SurfaceCard
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.EditNote,
                                contentDescription = null,
                                tint = if (workspaceTab == 1) Color.Black else brandColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "یادداشت تاپیک و تاریخچه",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (workspaceTab == 1) Color.Black else TextPrimary
                            )
                        }
                    }
                }

                if (workspaceTab == 0) {
                    // Status Filter & Sort Bar
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item {
                                CategoryChip(
                                    title = "همه (${PersianUtils.formatPersianNumber(topicTasks.size.toLong())})",
                                    selected = statusFilter == null,
                                    onClick = { statusFilter = null }
                                )
                            }
                            items(TaskStatus.entries) { st ->
                                val count = topicTasks.count { it.status == st }
                                CategoryChip(
                                    title = "${st.titleFa} (${PersianUtils.formatPersianNumber(count.toLong())})",
                                    selected = statusFilter == st,
                                    onClick = { statusFilter = if (statusFilter == st) null else st }
                                )
                            }
                            item {
                                CategoryChip(
                                    title = if (sortByPriority) "مرتب‌سازی: اولویت" else "مرتب‌سازی: مهلت انجام",
                                    selected = true,
                                    onClick = { sortByPriority = !sortByPriority }
                                )
                            }
                        }
                    }

                    if (filteredMissions.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("هنوز مأموریتی با این فیلتر در این تاپیک ثبت نشده است.", color = TextSecondary, fontSize = 13.sp)
                                    Button(
                                        onClick = { onOpenNewTaskForTopic(topic.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = brandColor)
                                    ) {
                                        Text("+ ثبت اولین مأموریت در ${topic.name}", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        items(filteredMissions, key = { it.id }) { mission ->
                            var newSubtaskText by remember(mission.id) { mutableStateOf("") }
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = if (mission.isMajorMission) 1.dp else 0.5.dp,
                                        color = if (mission.isMajorMission) AmberWarning else Color(0x22FFFFFF),
                                        shape = RoundedCornerShape(18.dp)
                                    )
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    val nextStatus = if (mission.status == TaskStatus.COMPLETED) TaskStatus.TODO else TaskStatus.COMPLETED
                                                    onUpdateTaskStatus(mission, nextStatus)
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = if (mission.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                    contentDescription = null,
                                                    tint = if (mission.completed) EmeraldAccent else brandColor
                                                )
                                            }
                                            Column {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    if (mission.isMajorMission) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(Icons.Default.Star, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(13.dp))
                                                            Spacer(modifier = Modifier.width(2.dp))
                                                            Text("وظیفه اصلی", fontSize = 10.sp, color = AmberWarning, fontWeight = FontWeight.ExtraBold)
                                                        }
                                                    }
                                                    Text(
                                                        text = mission.title,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        color = if (mission.completed) TextMuted else TextPrimary,
                                                        textDecoration = if (mission.completed) TextDecoration.LineThrough else TextDecoration.None
                                                    )
                                                }
                                                if (mission.description.isNotBlank()) {
                                                    Text(mission.description, fontSize = 11.sp, color = TextSecondary)
                                                }
                                                Text(
                                                    text = "اولویت: ${mission.priority.titleFa} • مهلت: ${mission.dueDate}" +
                                                        (if (!mission.reminderDateTime.isNullOrBlank()) " • یادآوری: ${mission.reminderDateTime}" else ""),
                                                    fontSize = 11.sp,
                                                    color = TextMuted
                                                )
                                            }
                                        }

                                        Row {
                                            IconButton(
                                                onClick = { onToggleMajorMission(mission) },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Star,
                                                    contentDescription = "وظیفه اصلی",
                                                    tint = if (mission.isMajorMission) AmberWarning else TextMuted,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { taskToMove = mission },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.DriveFileMove,
                                                    contentDescription = "انتقال به تاپیک دیگر",
                                                    tint = CyanPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                                        !SmartReminderManager.hasPostNotificationPermission(context)
                                                    ) {
                                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                                    }
                                                    onTriggerRealNotificationForTask(mission)
                                                },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.NotificationsActive,
                                                    contentDescription = "ارسال اعلان واقعی",
                                                    tint = EmeraldAccent,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { onDeleteTask(mission.id) },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "حذف",
                                                    tint = RoseDanger,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }

                                    // 4 Independent Statuses Selector Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        TaskStatus.entries.forEach { st ->
                                            val isSelected = mission.status == st
                                            val stColor = when (st) {
                                                TaskStatus.TODO -> TextSecondary
                                                TaskStatus.IN_PROGRESS -> CyanPrimary
                                                TaskStatus.COMPLETED -> EmeraldAccent
                                                TaskStatus.CANCELLED -> RoseDanger
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) stColor.copy(alpha = 0.22f) else SurfaceDark)
                                                    .border(
                                                        width = if (isSelected) 1.dp else 0.dp,
                                                        color = if (isSelected) stColor else Color.Transparent,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable { onUpdateTaskStatus(mission, st) }
                                                    .padding(vertical = 5.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = st.titleFa,
                                                    fontSize = 10.sp,
                                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                                                    color = if (isSelected) stColor else TextMuted
                                                )
                                            }
                                        }
                                    }

                                    // Subtasks Checklist inside Mission Card
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(SurfaceDark)
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val doneSub = mission.subtasks.count { it.isCompleted }
                                        Text(
                                            text = "چک‌لیست و زیرمجموعه‌ها (${PersianUtils.formatPersianNumber(doneSub.toLong())} از ${PersianUtils.formatPersianNumber(mission.subtasks.size.toLong())}):",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CyanPrimary
                                        )
                                        mission.subtasks.forEach { sub ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { onToggleSubtask(mission, sub.id) }
                                                    .padding(vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = if (sub.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                    contentDescription = null,
                                                    tint = if (sub.isCompleted) EmeraldAccent else TextMuted,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = sub.title,
                                                    fontSize = 11.sp,
                                                    color = if (sub.isCompleted) TextMuted else TextPrimary,
                                                    textDecoration = if (sub.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                                )
                                            }
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = newSubtaskText,
                                                onValueChange = { newSubtaskText = it },
                                                placeholder = { Text("افزودن زیرمجموعه جدید...", fontSize = 10.sp) },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true
                                            )
                                            Button(
                                                onClick = {
                                                    if (newSubtaskText.isNotBlank()) {
                                                        onAddSubtask(mission, newSubtaskText.trim())
                                                        newSubtaskText = ""
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text("+ ثبت", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // =========================================================
                    // TAB 1: DEDICATED TOPIC NOTE & ACTIVITY LOG
                    // =========================================================
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.EditNote, contentDescription = null, tint = brandColor, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "یادداشت اختصاصی تاپیک (${topic.name})",
                                        fontWeight = FontWeight.Bold,
                                        color = brandColor,
                                        fontSize = 14.sp
                                    )
                                }
                                OutlinedTextField(
                                    value = noteDraft,
                                    onValueChange = { noteDraft = it },
                                    label = { Text("نکات کلیدی، لینک‌ها، منابع یا برنامه کلی این تاپیک") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                )
                                Button(
                                    onClick = { onUpdateTopicNote(topic, noteDraft) },
                                    colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("ذخیره یادداشت تاپیک", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.History, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "تاریخچه فعالیت‌ها و تغییر وضعیت مأموریت‌ها",
                                        fontWeight = FontWeight.Bold,
                                        color = CyanPrimary,
                                        fontSize = 14.sp
                                    )
                                }
                                val allLogs = topicTasks.flatMap { t ->
                                    t.activityLog.map { log -> "${t.title} ← $log" }
                                }
                                if (allLogs.isEmpty()) {
                                    Text("هنوز تغییر وضعیتی ثبت نشده است.", fontSize = 12.sp, color = TextMuted)
                                } else {
                                    allLogs.forEach { entry ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(SurfaceDark)
                                                .padding(10.dp)
                                        ) {
                                            Text(text = "• $entry", fontSize = 11.sp, color = TextSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Move Task to Another Topic Dialog (with SVG Icons)
    taskToMove?.let { movingTask ->
        AlertDialog(
            onDismissRequest = { taskToMove = null },
            title = { Text("انتقال وظیفه «${movingTask.title}»", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("مقصد جدید این وظیفه را انتخاب کنید:", fontSize = 12.sp, color = TextSecondary)
                    OutlinedButton(
                        onClick = {
                            onMoveTaskToTopic(movingTask, null)
                            taskToMove = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PushPin, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تبدیل به وظیفه مستقل (خارج از تاپیک)", color = CyanPrimary)
                    }
                    allTopics.filter { it.id != topic.id && !it.isArchived }.forEach { targetTopic ->
                        Button(
                            onClick = {
                                onMoveTaskToTopic(movingTask, targetTopic.id)
                                taskToMove = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceCard),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AppSvgIcons.TopicSvgIcon(
                                iconKey = targetTopic.iconEmoji,
                                tint = Color(targetTopic.colorHex),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("انتقال به «${targetTopic.name}»", color = TextPrimary)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { taskToMove = null }) {
                    Text("بستن", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Delete Topic Confirmation Dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("تأیید حذف تاپیک «${topic.name}»", fontWeight = FontWeight.Bold, color = RoseDanger) },
            text = {
                Text(
                    text = "این تاپیک دارای ${PersianUtils.formatPersianNumber(topicTasks.size.toLong())} مأموریت است. آیا می‌خواهید مأموریت‌های داخل آن به‌عنوان «وظایف مستقل» حفظ شوند یا همراه با تاپیک حذف گردند؟",
                    fontSize = 12.sp,
                    color = TextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTopicConfirmed(topic, false)
                        showDeleteConfirm = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                ) {
                    Text("حذف تاپیک (حفظ وظایف به‌صورت مستقل)", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(
                        onClick = {
                            onDeleteTopicConfirmed(topic, true)
                            showDeleteConfirm = false
                            onDismiss()
                        }
                    ) {
                        Text("حذف کامل با وظایف", color = RoseDanger, fontSize = 11.sp)
                    }
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("انصراف", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            },
            containerColor = SurfaceDark
        )
    }
}

@Composable
private fun TopicStatBox(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = value, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = color)
            Text(text = label, fontSize = 10.sp, color = TextMuted)
        }
    }
}
