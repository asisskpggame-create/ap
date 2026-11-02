package com.example.personallifeos.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personallifeos.data.model.GoalItem
import com.example.personallifeos.data.model.TaskCategory
import com.example.personallifeos.ui.components.AppCard
import com.example.personallifeos.ui.components.AppCircularProgressRing
import com.example.personallifeos.ui.components.AppFilterChip
import com.example.personallifeos.ui.components.AppPrimaryButton
import com.example.personallifeos.ui.theme.CyanGlow
import com.example.personallifeos.ui.theme.CyanPrimary
import com.example.personallifeos.util.PersianUtils

@Composable
fun GoalsScreen(
    goals: List<GoalItem>,
    onBack: () -> Unit,
    onAddGoalClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedGoalTab by remember { mutableStateOf("ALL") } // ALL, SHORT, MID, LONG

    // Sample goals matching screenshot if goals list is empty
    val displayGoals = if (goals.isEmpty()) {
        listOf(
            GoalItem("g1", "تکمیل پایان‌نامه ارشد", "پژوهش و نگارش نهایی", "تا ۳۰ آذر", 70, "نگارش فصل سوم", TaskCategory.STUDY),
            GoalItem("g2", "رسیدن به تناسب اندام", "ورزش روزانه و تغذیه سالم", "تا پایان سال", 40, "تمرین هوازی", TaskCategory.HEALTH),
            GoalItem("g3", "پس‌انداز برای سفر", "تأمین بودجه سفر به شمال", "تا اسفند ماه", 60, "واریز هفتگی", TaskCategory.FINANCE)
        )
    } else goals

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
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "بازگشت",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "اهداف و دستاوردها",
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
                        imageVector = Icons.Default.TrackChanges,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Filter chips (همه, کوتاه‌مدت, میان‌مدت, بلندمدت)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppFilterChip(
                    text = "بلندمدت",
                    isSelected = selectedGoalTab == "LONG",
                    onClick = { selectedGoalTab = "LONG" }
                )
                AppFilterChip(
                    text = "میان‌مدت",
                    isSelected = selectedGoalTab == "MID",
                    onClick = { selectedGoalTab = "MID" }
                )
                AppFilterChip(
                    text = "کوتاه‌مدت",
                    isSelected = selectedGoalTab == "SHORT",
                    onClick = { selectedGoalTab = "SHORT" }
                )
                AppFilterChip(
                    text = "همه",
                    isSelected = selectedGoalTab == "ALL",
                    onClick = { selectedGoalTab = "ALL" }
                )
            }
        }

        // Goal Cards List
        items(displayGoals, key = { it.id }) { goal ->
            val progressPercent = goal.progressPercent.coerceIn(0, 100)

            AppCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                backgroundColor = MaterialTheme.colorScheme.surface,
                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = goal.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = goal.targetDate,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AppCircularProgressRing(
                        progressPercent = progressPercent,
                        size = 56.dp,
                        strokeWidth = 5.5.dp,
                        ringColor = CyanPrimary
                    ) {
                        Text(
                            text = "${PersianUtils.formatPersianNumber(progressPercent.toLong())}٪",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Bottom Add Goal Button
        item {
            AppPrimaryButton(
                text = "افزودن هدف جدید",
                icon = Icons.Default.Add,
                onClick = onAddGoalClick,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
