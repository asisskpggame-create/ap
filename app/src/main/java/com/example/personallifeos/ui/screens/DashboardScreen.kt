package com.example.personallifeos.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personallifeos.data.model.ExpenseItem
import com.example.personallifeos.data.model.HabitItem
import com.example.personallifeos.data.model.HealthLog
import com.example.personallifeos.data.model.TaskItem
import com.example.personallifeos.ui.theme.AmberWarning
import com.example.personallifeos.ui.theme.BorderCyanGlow
import com.example.personallifeos.ui.theme.CyanPrimary
import com.example.personallifeos.ui.theme.EmeraldAccent
import com.example.personallifeos.ui.theme.SurfaceCard
import com.example.personallifeos.ui.theme.SurfaceDark
import com.example.personallifeos.ui.theme.TextMuted
import com.example.personallifeos.ui.theme.TextPrimary
import com.example.personallifeos.ui.theme.TextSecondary
import com.example.personallifeos.util.PersianUtils

@Composable
fun DashboardScreen(
    tasks: List<TaskItem>,
    habits: List<HabitItem>,
    expenses: List<ExpenseItem>,
    healthLog: HealthLog,
    onNavigate: (String) -> Unit,
    onOpenVoiceModal: () -> Unit,
    onToggleTask: (TaskItem) -> Unit,
    onToggleHabit: (HabitItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val pendingTasks = tasks.filter { !it.completed }
    val totalExpenseTomans = expenses.sumOf { it.amount }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Greeting Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderCyanGlow, RoundedCornerShape(24.dp))
                    .testTag("dashboard_hero_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SurfaceDark
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF0F172A),
                                    Color(0xFF092C3E)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(Color(0x2606B6D4), CircleShape)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = CyanPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "دستیار هوشمند زندگی فعال است",
                                color = CyanPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Text(
                            text = "روز به‌خیر، دستیار مالی و شخصی آماده است 🌟",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Text(
                            text = "امروز ۱۴۰۵/۰۸/۰۵ — حساب‌ها، وظایف و برنامه‌های سلامت شما هماهنگ هستند. پیامک بانکی را تحلیل کنید یا با دستیار صوتی یادداشت بردارید.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Button(
                                onClick = onOpenVoiceModal,
                                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.testTag("dashboard_voice_btn")
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("ضبط صوت / پیامک", color = Color.Black, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { onNavigate("assistant") },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.testTag("dashboard_assistant_btn")
                            ) {
                                Text("گفتگو با جمینای", color = TextPrimary)
                            }
                        }
                    }
                }
            }
        }

        // Metrics Grid (2x2)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "وظایف باقیمانده",
                        value = "${PersianUtils.formatPersianNumber(pendingTasks.size.toLong())} از ${PersianUtils.formatPersianNumber(tasks.size.toLong())}",
                        subtitle = "مشاهده لیست وظایف",
                        icon = Icons.Default.CheckCircle,
                        iconTint = CyanPrimary,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("metric_tasks")
                            .clickable { onNavigate("tasks") }
                    )

                    MetricCard(
                        title = "مجموع هزینه‌های ماه",
                        value = PersianUtils.formatToman(totalExpenseTomans),
                        subtitle = "پیامک‌ها و تراکنش‌ها",
                        icon = Icons.Default.Wallet,
                        iconTint = AmberWarning,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("metric_expenses")
                            .clickable { onNavigate("expenses") }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val habitCompletions = habits.count { it.completedToday }
                    MetricCard(
                        title = "عادت‌های امروز",
                        value = "${PersianUtils.formatPersianNumber(habitCompletions.toLong())} از ${PersianUtils.formatPersianNumber(habits.size.toLong())}",
                        subtitle = "پایش استمرار روزانه",
                        icon = Icons.Default.Star,
                        iconTint = EmeraldAccent,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("metric_habits")
                            .clickable { onNavigate("tasks") }
                    )

                    MetricCard(
                        title = "پایش سلامت و خواب",
                        value = "${PersianUtils.formatPersianNumber(healthLog.sleepHours.toString())} ساعت",
                        subtitle = "${PersianUtils.formatPersianNumber(healthLog.waterGlasses.toLong())} لیوان آب",
                        icon = Icons.Default.Favorite,
                        iconTint = Color(0xFFEC4899),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("metric_health")
                            .clickable { onNavigate("health") }
                    )
                }
            }
        }

        // Today's Priority Tasks
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "وظایف و اولویت‌های امروز",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "مشاهده همه",
                    color = CyanPrimary,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { onNavigate("tasks") }
                )
            }
        }

        items(tasks.take(3), key = { it.id }) { task ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleTask(task) }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (task.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (task.completed) EmeraldAccent else TextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = if (task.completed) TextMuted else TextPrimary
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = "دسته: ${task.category.titleFa}",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                            Text(
                                text = "موعد: ${task.dueDate}",
                                fontSize = 11.sp,
                                color = CyanPrimary
                            )
                        }
                    }
                }
            }
        }

        // Active Habits Quick Row
        item {
            Text(
                text = "استمرار عادت‌های فعال",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(habits, key = { it.id }) { habit ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0x2610B981)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = habit.title, color = TextPrimary, fontWeight = FontWeight.Medium)
                            Text(
                                text = "زنجیره: ${PersianUtils.formatPersianNumber(habit.streak.toLong())} روز متوالی",
                                color = EmeraldAccent,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Button(
                        onClick = { onToggleHabit(habit) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (habit.completedToday) EmeraldAccent else Color(0xFF1E293B)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (habit.completedToday) "تکمیل شد ✓" else "ثبت امروز",
                            color = if (habit.completedToday) Color.Black else TextPrimary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        modifier = modifier.border(0.5.dp, Color(0x1AFFFFFF), RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 12.sp, color = TextMuted)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
                }
            }
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = subtitle, fontSize = 11.sp, color = iconTint)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = iconTint, modifier = Modifier.size(10.dp))
            }
        }
    }
}
