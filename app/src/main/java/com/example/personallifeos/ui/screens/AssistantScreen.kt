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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personallifeos.data.model.ChatMessage
import com.example.personallifeos.ui.theme.AmberWarning
import com.example.personallifeos.ui.theme.CyanPrimary
import com.example.personallifeos.ui.theme.EmeraldAccent
import com.example.personallifeos.ui.theme.RoseDanger
import com.example.personallifeos.ui.theme.SurfaceCard
import com.example.personallifeos.ui.theme.SurfaceDark
import com.example.personallifeos.ui.theme.TextMuted
import com.example.personallifeos.ui.theme.TextPrimary
import com.example.personallifeos.ui.theme.TextSecondary

@Composable
fun AssistantScreen(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    onSendMessage: (String) -> Unit,
    onConfirmDraft: (String) -> Unit = {},
    onCancelDraft: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }

    val suggestions = listOf(
        "این ماه بیشترین هزینه‌ام برای چی بوده؟",
        "تا آخر ماه چقدر پول قابل‌خرج دارم؟",
        "۵۰۰ تومن سیگار خریدم",
        "۱۲۰ تومن اسنپ دادم",
        "۳ میلیون به علی قرض دادم",
        "یادم بنداز فردا گزارش مالی رو ارسال کنم"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("assistant_screen")
    ) {
        // Chat Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0x2606B6D4)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.SmartToy, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "دستیار هوشمند جمینای (اجرای کنترل‌شده + فهم محاوره‌ای)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "تحلیل ۳۰ روزه، تبدیل تومان محاوره‌ای و کارت پیش‌نمایش قبل از ثبت در دیتابیس",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }

        // Suggestions Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            items(suggestions) { s ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceCard)
                        .border(0.5.dp, Color(0x3306B6D4), RoundedCornerShape(10.dp))
                        .clickable { onSendMessage(s) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(text = s, color = CyanPrimary, fontSize = 11.sp)
                }
            }
        }

        // Messages Box
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(SurfaceDark)
                .border(0.5.dp, Color(0x1AFFFFFF), RoundedCornerShape(18.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isUser = msg.sender == "user"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isUser) 16.dp else 4.dp,
                                    bottomEnd = if (isUser) 4.dp else 16.dp
                                )
                            )
                            .background(if (isUser) Color(0xFF1E3A8A) else SurfaceCard)
                            .border(
                                width = if (msg.requiresConfirmation) 1.5.dp else 0.5.dp,
                                color = when {
                                    msg.requiresConfirmation -> AmberWarning
                                    msg.isClarification -> CyanPrimary
                                    isUser -> Color(0x333B82F6)
                                    else -> Color(0x3306B6D4)
                                },
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isUser) "شما" else "جمینای AI",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isUser) Color(0xFF93C5FD) else CyanPrimary
                                )
                                if (msg.requiresConfirmation) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Security, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("نیازمند تأیید کاربر", fontSize = 10.sp, color = AmberWarning, fontWeight = FontWeight.Bold)
                                    }
                                } else if (msg.isClarification) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.HelpOutline, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("پرسش رفع ابهام", fontSize = 10.sp, color = CyanPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Text(
                                text = msg.text,
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary
                            )

                            // Controlled Execution Confirmation Buttons (Section 7.3 of Roadmap)
                            if (msg.requiresConfirmation && msg.actionDraft != null && !msg.actionDraft.isExecuted && !msg.actionDraft.isCancelled) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(top = 6.dp)
                                ) {
                                    Button(
                                        onClick = { onConfirmDraft(msg.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("ai_confirm_draft_${msg.id}")
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Black, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("تأیید و ثبت نهایی", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { onCancelDraft(msg.id) },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("ai_cancel_draft_${msg.id}")
                                    ) {
                                        Text("لغو", color = RoseDanger, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Text(
                                text = msg.timestamp,
                                fontSize = 10.sp,
                                color = TextMuted,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = CyanPrimary, strokeWidth = 2.dp)
                        Text(text = "در حال تحلیل قصد تراکنش و داده‌های واقعی...", fontSize = 11.sp, color = TextMuted)
                    }
                }
            }
        }

        // Input bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("مثلاً: ۵۰۰ تومن سیگار خریدم یا بیشترین هزینه‌ام چی بوده؟", fontSize = 12.sp, color = TextMuted) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceCard,
                    unfocusedContainerColor = SurfaceCard,
                    focusedBorderColor = CyanPrimary,
                    unfocusedBorderColor = Color(0x26FFFFFF)
                ),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        onSendMessage(inputText)
                        inputText = ""
                    }
                },
                modifier = Modifier
                    .size(50.dp)
                    .background(CyanPrimary, RoundedCornerShape(14.dp))
                    .testTag("send_chat_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "ارسال", tint = Color.Black)
            }
        }
    }
}
