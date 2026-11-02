package com.example.personallifeos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personallifeos.data.model.TaskItem
import com.example.personallifeos.ui.components.AppCard
import com.example.personallifeos.ui.components.AppFilterChip
import com.example.personallifeos.ui.components.AppSectionHeader
import com.example.personallifeos.ui.theme.AmberWarning
import com.example.personallifeos.ui.theme.CyanPrimary
import com.example.personallifeos.util.PersianUtils

@Composable
fun CalendarScreen(
    tasks: List<TaskItem>,
    onBack: () -> Unit,
    onOpenTask: (TaskItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCalendarTab by remember { mutableStateOf("MONTHLY") } // MONTHLY, WEEKLY, DAILY
    var selectedDayNumber by remember { mutableIntStateOf(5) }

    val daysOfWeekFa = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
    val daysInMonth = (1..30).toList()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward, // Back in RTL
                        contentDescription = "بازگشت",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "تقویم",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // View Tabs (ماهانه, هفتگی, روزانه)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppFilterChip(
                    text = "روزانه",
                    isSelected = selectedCalendarTab == "DAILY",
                    onClick = { selectedCalendarTab = "DAILY" }
                )
                AppFilterChip(
                    text = "هفتگی",
                    isSelected = selectedCalendarTab == "WEEKLY",
                    onClick = { selectedCalendarTab = "WEEKLY" }
                )
                AppFilterChip(
                    text = "ماهانه",
                    isSelected = selectedCalendarTab == "MONTHLY",
                    onClick = { selectedCalendarTab = "MONTHLY" }
                )
            }
        }

        // Jalali Month Selector (< آبان ۱۴۰۳ >)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "ماه بعد", tint = MaterialTheme.colorScheme.primary)
                }

                Text(
                    text = "آبان ۱۴۰۵",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                IconButton(onClick = {}) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "ماه قبل", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Month Grid Card
        item {
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                backgroundColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Day headers (ش تا ج)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        daysOfWeekFa.forEach { dayName ->
                            Text(
                                text = dayName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Month days rows
                    daysInMonth.chunked(7).forEach { week ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            week.forEach { dayNum ->
                                val isSelected = dayNum == selectedDayNumber
                                val isToday = dayNum == 5
                                val bg = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                val textCol = if (isSelected) Color.Black else (if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(bg)
                                        .clickable { selectedDayNumber = dayNum },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = PersianUtils.formatPersianNumber(dayNum.toLong()),
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                        color = textCol
                                    )
                                }
                            }
                            // Fill empty slots if last week has < 7 days
                            if (week.size < 7) {
                                repeat(7 - week.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Today's Events
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AppSectionHeader(
                    title = "رویدادهای ${PersianUtils.formatPersianNumber(selectedDayNumber.toLong())} آبان",
                    actionText = "مشاهده همه",
                    onActionClick = {}
                )

                AppCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    backgroundColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        EventItemRow(
                            time = "۱۰:۰۰",
                            title = "جلسه هماهنگی و بررسی پروژه",
                            category = "کاری",
                            dotColor = CyanPrimary
                        )
                        EventItemRow(
                            time = "۱۴:۰۰",
                            title = "مطالعه و نگارش پایان‌نامه",
                            category = "تحصیلی",
                            dotColor = CyanPrimary
                        )
                        EventItemRow(
                            time = "۱۹:۰۰",
                            title = "ورزش بدنسازی و تمرین هوازی",
                            category = "سلامت",
                            dotColor = AmberWarning
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun EventItemRow(
    time: String,
    title: String,
    category: String,
    dotColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = time,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )

        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = category,
            fontSize = 10.sp,
            color = dotColor,
            fontWeight = FontWeight.Bold
        )
    }
}
