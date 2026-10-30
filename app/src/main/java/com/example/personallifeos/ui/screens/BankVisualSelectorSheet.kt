package com.example.personallifeos.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.personallifeos.sms.BankType

data class VisualBankOption(
    val bankType: BankType,
    val labelFa: String,
    val cardTitleFa: String,
    val badgeText: String,
    val badgeColor: Color,
    val defaultSender: String
)

val MIRZA_VISUAL_BANKS = listOf(
    VisualBankOption(BankType.MELLI, "ملی", "بانک ملی", "ملی", Color(0xFFB45309), "98300017"),
    VisualBankOption(BankType.BLUBANK, "بلو بانک", "بلو بانک", "b", Color(0xFF2563EB), "982000"),
    VisualBankOption(BankType.BLUBANK_POL, "بلوبانک حالت پل", "بلوبانک (پل)", "b", Color(0xFF3B82F6), "30009601"),
    VisualBankOption(BankType.GARDESHGARI, "گردشگری", "بانک گردشگری", "گ", Color(0xFFDC2626), "98200064"),
    VisualBankOption(BankType.SADERAT, "صادرات", "بانک صادرات", "ص", Color(0xFF1E3A8A), "98300060"),
    VisualBankOption(BankType.MELLAT, "ملت", "بانک ملت", "ملت", Color(0xFFE11D48), "98200033"),
    VisualBankOption(BankType.TEJARAT, "تجارت", "بانک تجارت", "ت", Color(0xFF1D4ED8), "98200070"),
    VisualBankOption(BankType.KESHAVARZI, "کشاورزی", "بانک کشاورزی", "ک", Color(0xFF15803D), "98200077"),
    VisualBankOption(BankType.SAMAN, "سامان", "بانک سامان", "س", Color(0xFF0284C7), "9820000"),
    VisualBankOption(BankType.PASARGAD, "پاسارگاد", "بانک پاسارگاد", "پ", Color(0xFFD97706), "982000500"),
    VisualBankOption(BankType.REFAH, "رفاه", "بانک رفاه", "ر", Color(0xFF0D9488), "98300007"),
    VisualBankOption(BankType.MASKAN, "مسکن", "بانک مسکن", "م", Color(0xFFEA580C), "98300014")
)

private val RECENT_DETECTED_SENDERS = listOf(
    "98300017" to "بانک ملی ایران (سداد)",
    "982000" to "بلوبانک / سامان",
    "98200033" to "بانک ملت (بهسازان)",
    "98300060" to "بانک صادرات (سپهر)",
    "98200070" to "بانک تجارت",
    "98200077" to "بانک کشاورزی",
    "98200064" to "بانک گردشگری",
    "98300007" to "بانک رفاه کارگران"
)

@Composable
fun BankVisualSelectorDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onSaveBankConfig: (BankType, String, Boolean) -> Unit
) {
    if (!isOpen) return

    var selectedOption by remember { mutableStateOf(MIRZA_VISUAL_BANKS.first()) }
    var senderInput by remember { mutableStateOf(MIRZA_VISUAL_BANKS.first().defaultSender) }
    var requireSenderMatch by remember { mutableStateOf(true) }
    var showSenderListPicker by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(28.dp))
                .testTag("mirza_bank_selector_dialog"),
            color = Color(0xFFF8FAFC)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "افزودن بانک جدید",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "بانک و سرشماره پیامک آن را انتخاب کنید",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                }

                // Realistic Mirza-style Navy Bank Card Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(188.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF08203E), Color(0xFF0F3B6C), Color(0xFF164E8A))
                            )
                        )
                        .testTag("mirza_bank_card_preview")
                ) {
                    // Decorative background circles inside the card
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.06f),
                            radius = size.height * 0.75f,
                            center = Offset(size.width * 0.2f, size.height * 0.15f)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.05f),
                            radius = size.height * 0.65f,
                            center = Offset(size.width * 0.85f, size.height * 0.9f)
                        )
                    }

                    // Top Row: Bank Logo + Name on Right (in RTL, Start is Right)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = selectedOption.badgeText,
                                color = selectedOption.badgeColor,
                                fontWeight = FontWeight.Black,
                                fontSize = if (selectedOption.badgeText.length == 1) 22.sp else 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = selectedOption.cardTitleFa,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "فیش‌خوان هوشمند فعال",
                                color = Color(0xFF93C5FD),
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Golden EMV Chip on the left side (End in RTL)
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 24.dp)
                            .size(width = 46.dp, height = 34.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFFDE68A), Color(0xFFD97706), Color(0xFFF59E0B))
                                )
                            )
                            .border(1.dp, Color(0xFFB45309), RoundedCornerShape(7.dp))
                    )

                    // Bottom Right (Start in RTL): SMS Sender Label & Dots/Value
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 22.dp, bottom = 18.dp)
                    ) {
                        Text(
                            text = "سرشماره پیامک",
                            color = Color(0xFFCBD5E1),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (senderInput.isBlank()) "• • • • • • • •" else senderInput,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // Section Title: انتخاب بانک
                Text(
                    text = "انتخاب بانک",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF334155)
                )

                // 4-Column Grid of Banks inside light gray rounded container
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MIRZA_VISUAL_BANKS.chunked(4).forEach { rowBanks ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            rowBanks.forEach { item ->
                                val isSelected = item.bankType == selectedOption.bankType
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            selectedOption = item
                                            senderInput = item.defaultSender
                                        }
                                        .padding(vertical = 6.dp)
                                        .testTag("bank_grid_item_${item.bankType.name}")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                            .border(
                                                width = if (isSelected) 2.5.dp else 1.dp,
                                                color = if (isSelected) Color(0xFF0F3B6C) else Color(0xFFE2E8F0),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(item.badgeColor.copy(alpha = 0.14f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = item.badgeText,
                                                color = item.badgeColor,
                                                fontWeight = FontWeight.Black,
                                                fontSize = if (item.badgeText.length == 1) 17.sp else 11.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = item.labelFa,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = if (isSelected) Color(0xFF0F172A) else Color(0xFF475569),
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // SMS Sender Input Section
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "سرشماره پیامک",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = senderInput,
                            onValueChange = { senderInput = it },
                            placeholder = {
                                Text("مثلاً: 98300017", color = Color(0xFF94A3B8), fontSize = 13.sp)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF1F5F9),
                                unfocusedContainerColor = Color(0xFFF1F5F9),
                                focusedBorderColor = Color(0xFF0F3B6C),
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("bank_sender_input")
                        )

                        IconButton(
                            onClick = { showSenderListPicker = !showSenderListPicker },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFE0F2FE))
                                .testTag("sender_list_picker_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inbox,
                                contentDescription = "انتخاب از لیست سرشماره‌ها",
                                tint = Color(0xFF0369A1)
                            )
                        }
                    }
                    Text(
                        text = "می‌توانید از دکمه لیست، سرشماره‌های دریافت‌شده را انتخاب کنید",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }

                // Expandable Detected Sender Picker
                if (showSenderListPicker) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFEFF6FF))
                            .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(14.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "سرشماره‌های بانکی شناسایی‌شده در دستگاه:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E3A8A)
                        )
                        RECENT_DETECTED_SENDERS.forEach { (num, title) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White)
                                    .clickable {
                                        senderInput = num
                                        showSenderListPicker = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(title, fontSize = 12.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.Medium)
                                Text(num, fontSize = 12.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Sender policy checkbox (Audit Fix #3)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF1F5F9))
                        .clickable { requireSenderMatch = !requireSenderMatch }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = requireSenderMatch,
                        onCheckedChange = { requireSenderMatch = it },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF1E6F30))
                    )
                    Column {
                        Text(
                            text = "الزام تطابق سرشماره برای ثبت خودکار",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "در صورت تفاوت سرشماره پیامک، تراکنش به بخش «نیازمند بررسی» هدایت می‌شود.",
                            fontSize = 10.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                // Bottom Action Buttons: Green "+ ثبت بانک" and "انصراف"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            onSaveBankConfig(
                                selectedOption.bankType,
                                senderInput.ifBlank { selectedOption.defaultSender },
                                requireSenderMatch
                            )
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E6F30)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(50.dp)
                            .testTag("confirm_add_bank_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ثبت بانک",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(0.8f)
                            .height(50.dp)
                            .testTag("cancel_add_bank_btn")
                    ) {
                        Text(
                            text = "انصراف",
                            color = Color(0xFF475569),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
