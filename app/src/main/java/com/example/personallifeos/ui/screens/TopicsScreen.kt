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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personallifeos.data.model.TaskItem
import com.example.personallifeos.data.model.TaskStatus
import com.example.personallifeos.data.model.TopicItem
import com.example.personallifeos.ui.components.AppCard
import com.example.personallifeos.ui.components.AppCircularProgressRing
import com.example.personallifeos.ui.components.AppFilterChip
import com.example.personallifeos.ui.components.AppSvgIcons
import com.example.personallifeos.ui.theme.CyanPrimary
import com.example.personallifeos.util.PersianUtils

@Composable
fun TopicsScreen(
    topics: List<TopicItem>,
    tasks: List<TaskItem>,
    onOpenTopic: (String) -> Unit,
    onAddNewTopicClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, ACTIVE, ARCHIVED

    val filteredTopics = topics.filter { topic ->
        val matchesSearch = topic.title.contains(searchQuery, ignoreCase = true) ||
                topic.description.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (selectedFilter) {
            "ACTIVE" -> topic.isActive && !topic.isArchived
            "ARCHIVED" -> topic.isArchived
            else -> true
        }
        matchesSearch && matchesFilter
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 12.dp)
                .testTag("topics_screen"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onAddNewTopicClick,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "افزودن تاپیک",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Text(
                        text = "تاپیک‌ها و پروژه‌ها",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    IconButton(
                        onClick = { /* toggle search bar focus */ },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "جستجو",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("جستجو در بین تاپیک‌ها و پروژه‌ها...", fontSize = 12.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    singleLine = true
                )
            }

            // Filter Chips (همه, فعال, آرشیو شده)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppFilterChip(
                        text = "همه",
                        isSelected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        badgeCount = topics.size
                    )
                    AppFilterChip(
                        text = "فعال",
                        isSelected = selectedFilter == "ACTIVE",
                        onClick = { selectedFilter = "ACTIVE" },
                        badgeCount = topics.count { it.isActive && !it.isArchived }
                    )
                    AppFilterChip(
                        text = "آرشیو شده",
                        isSelected = selectedFilter == "ARCHIVED",
                        onClick = { selectedFilter = "ARCHIVED" },
                        badgeCount = topics.count { it.isArchived }
                    )
                }
            }

            // Topic Cards List
            if (filteredTopics.isEmpty()) {
                item {
                    AppCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "تاپیکی در این بخش یافت نشد",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "برای دسته‌بندی پروژه‌ها و کارها یک تاپیک جدید اضافه کنید.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredTopics, key = { it.id }) { topic ->
                    val linkedTasks = tasks.filter { it.topicId == topic.id }
                    val doneCount = linkedTasks.count { it.completed || it.status == TaskStatus.COMPLETED }
                    val totalCount = linkedTasks.size
                    val progressPercent = if (totalCount > 0) ((doneCount.toFloat() / totalCount.toFloat()) * 100).toInt() else 0

                    TopicCardItem(
                        topic = topic,
                        missionsCount = totalCount,
                        progressPercent = progressPercent,
                        onClick = { onOpenTopic(topic.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onAddNewTopicClick,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.Black,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomStart) // Bottom right in RTL
                .padding(24.dp)
                .size(56.dp)
                .testTag("fab_add_topic")
        ) {
            Icon(Icons.Default.Add, contentDescription = "تاپیک جدید", modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun TopicCardItem(
    topic: TopicItem,
    missionsCount: Int,
    progressPercent: Int,
    onClick: () -> Unit
) {
    val topicColor = Color(topic.colorHex)

    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        backgroundColor = MaterialTheme.colorScheme.surface,
        borderColor = topicColor.copy(alpha = 0.35f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon + Title & Details
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(topicColor.copy(alpha = 0.15f))
                        .border(1.dp, topicColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    AppSvgIcons.TopicSvgIcon(
                        iconKey = topic.iconEmoji,
                        modifier = Modifier.size(24.dp),
                        tint = topicColor
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = topic.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${PersianUtils.formatPersianNumber(missionsCount.toLong())} مأموریت • ${PersianUtils.formatPersianNumber(progressPercent.toLong())}٪ تکمیل",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Progress Ring
            AppCircularProgressRing(
                progressPercent = progressPercent,
                size = 46.dp,
                strokeWidth = 4.5.dp,
                ringColor = topicColor
            ) {
                Text(
                    text = "${PersianUtils.formatPersianNumber(progressPercent.toLong())}٪",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
