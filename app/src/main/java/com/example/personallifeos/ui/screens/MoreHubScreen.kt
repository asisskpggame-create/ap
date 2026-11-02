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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personallifeos.ui.components.AppCard
import com.example.personallifeos.ui.theme.AmberWarning
import com.example.personallifeos.ui.theme.CyanGlow
import com.example.personallifeos.ui.theme.CyanPrimary
import com.example.personallifeos.ui.theme.EmeraldAccent
import com.example.personallifeos.ui.theme.PurpleAccent
import com.example.personallifeos.ui.theme.RoseDanger

data class MoreHubItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun MoreHubScreen(
    onNavigateToSubscreen: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val hubItems = listOf(
        MoreHubItem(
            id = "calendar",
            title = "تقویم هوشمند",
            subtitle = "نمای ماهانه، هفتگی و رویدادها",
            icon = Icons.Default.CalendarMonth,
            color = CyanPrimary
        ),
        MoreHubItem(
            id = "goals",
            title = "اهداف و دستاوردها",
            subtitle = "کوتاه‌مدت، میان‌مدت و بلندمدت",
            icon = Icons.Default.TrackChanges,
            color = CyanGlow
        ),
        MoreHubItem(
            id = "habits",
            title = "عادات و تداوم",
            subtitle = "استریک روزانه و زنجیره موفقیت",
            icon = Icons.Default.Repeat,
            color = EmeraldAccent
        ),
        MoreHubItem(
            id = "assistant",
            title = "دستیار هوشمند (AI)",
            subtitle = "گفتگوی صوتی و هوش مصنوعی",
            icon = Icons.Default.SmartToy,
            color = PurpleAccent
        ),
        MoreHubItem(
            id = "health",
            title = "سلامت و روتین",
            subtitle = "پایش آب، خواب، ورزش و کالری",
            icon = Icons.Default.Favorite,
            color = RoseDanger
        ),
        MoreHubItem(
            id = "vault",
            title = "صندوق امن و یادداشت‌ها",
            subtitle = "رمز عبورها و یادداشت‌های AES",
            icon = Icons.Default.Lock,
            color = AmberWarning
        ),
        MoreHubItem(
            id = "reminders",
            title = "یادآورها و هشدارها",
            subtitle = "زمان‌بندی دقیق و آلارم‌ها",
            icon = Icons.Default.Notifications,
            color = CyanPrimary
        ),
        MoreHubItem(
            id = "analytics",
            title = "تحلیل و گزارشات",
            subtitle = "روند رشد فردی و مالی",
            icon = Icons.Default.Timeline,
            color = EmeraldAccent
        ),
        MoreHubItem(
            id = "settings",
            title = "تنظیمات و پروفایل",
            subtitle = "تم، شخصی‌سازی، پشتیبان‌گیری",
            icon = Icons.Default.Settings,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .testTag("more_hub_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(bottom = 6.dp)) {
                Text(
                    text = "سایر بخش‌ها و امکانات",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "دسترسی یکپارچه به تقویم، اهداف، دستیار هوش مصنوعی و تنظیمات",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(hubItems.chunked(2).size) { rowIndex ->
            val row = hubItems.chunked(2)[rowIndex]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { item ->
                    AppCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToSubscreen(item.id) },
                        shape = RoundedCornerShape(18.dp),
                        backgroundColor = MaterialTheme.colorScheme.surface,
                        borderColor = item.color.copy(alpha = 0.25f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(item.color.copy(alpha = 0.15f))
                                    .border(1.dp, item.color.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = item.color,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = item.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = item.subtitle,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
