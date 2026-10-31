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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.personallifeos.sms.BankNotificationItem
import com.example.personallifeos.sms.BankType
import com.example.personallifeos.sms.IranianBankSmsEngine

data class VisualBankOption(
    val bankType: BankType,
    val labelFa: String,
    val cardTitleFa: String,
    val badgeText: String,
    val badgeColor: Color,
    val defaultSender: String
)

// Primary 8 banks in the exact 2x4 order of the Mirza screenshot, followed by secondary banks
val MIRZA_VISUAL_BANKS = listOf(
    VisualBankOption(BankType.MELLI, "ملی", "بانک ملی", "ملی", Color(0xFFB45309), "98300017"),
    VisualBankOption(BankType.BLUBANK, "بلو بانک", "بلو بانک", "b", Color(0xFF3B82F6), "982000"),
    VisualBankOption(BankType.BLUBANK_POL, "بلوبانک حالت پل", "بلوبانک حالت پل", "b", Color(0xFF2563EB), "30009601"),
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

@Composable
fun BankVisualSelectorDialog(
    isOpen: Boolean,
    liveNotifications: List<BankNotificationItem> = emptyList(),
    initialBank: BankType? = null,
    initialSender: String? = null,
    onDismiss: () -> Unit,
    onSaveBankConfig: (BankType, String, Boolean) -> Unit
) {
    if (!isOpen) return

    val initialOption = remember(initialBank) {
        MIRZA_VISUAL_BANKS.firstOrNull { it.bankType == initialBank } ?: MIRZA_VISUAL_BANKS.first()
    }
    var selectedOption by remember(initialBank) { mutableStateOf(initialOption) }
    // Starts blank (showing "••••••••" on the card and "مثلاً: 98300017" in the input placeholder, matching the screenshot)
    var senderInput by remember(initialBank, initialSender) { mutableStateOf(initialSender ?: "") }
    var requireSenderMatch by remember { mutableStateOf(true) }
    var showSenderListPicker by remember { mutableStateOf(false) }
    var showMoreBanks by remember { mutableStateOf(false) }

    val receivedInboxSenders = remember(liveNotifications) {
        IranianBankSmsEngine.getReceivedSmsInboxSenders(liveNotifications)
    }

    val displayedBanks = if (showMoreBanks) MIRZA_VISUAL_BANKS else MIRZA_VISUAL_BANKS.take(8)

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
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header (Right-aligned in RTL)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "افزودن بانک جدید",
                        fontSize = 21.sp,
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
                        .height(192.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF061B36), Color(0xFF0B315C), Color(0xFF0F3E73))
                            )
                        )
                        .testTag("mirza_bank_card_preview")
                ) {
                    // Decorative background circles inside the card matching screenshot
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.06f),
                            radius = size.height * 0.72f,
                            center = Offset(size.width * 0.85f, size.height * 0.15f)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = 0.05f),
                            radius = size.height * 0.65f,
                            center = Offset(size.width * 0.12f, size.height * 0.92f)
                        )
                    }

                    // Top-Right (Start in RTL): Circular White Emblem + Bank Title
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            BankEmblemBadge(option = selectedOption, sizeDp = 36)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = selectedOption.cardTitleFa,
                                color = Color.White,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            if (senderInput.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF86EFAC), modifier = Modifier.size(11.dp))
                                    Text(
                                        text = "قفل روی سرشماره اختصاصی",
                                        color = Color(0xFF86EFAC),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Center-Left (End in RTL): Realistic Golden EMV Chip with metallic lines
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 24.dp)
                            .size(width = 48.dp, height = 36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFFDE68A), Color(0xFFD97706), Color(0xFFF59E0B))
                                )
                            )
                            .border(1.dp, Color(0xFFB45309), RoundedCornerShape(8.dp))
                    ) {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            val strokeColor = Color(0xFF92400E).copy(alpha = 0.55f)
                            // Horizontal chip contact lines
                            drawLine(
                                color = strokeColor,
                                start = Offset(0f, size.height * 0.35f),
                                end = Offset(size.width, size.height * 0.35f),
                                strokeWidth = 1.5f
                            )
                            drawLine(
                                color = strokeColor,
                                start = Offset(0f, size.height * 0.65f),
                                end = Offset(size.width, size.height * 0.65f),
                                strokeWidth = 1.5f
                            )
                            // Vertical chip contact lines
                            drawLine(
                                color = strokeColor,
                                start = Offset(size.width * 0.35f, 0f),
                                end = Offset(size.width * 0.35f, size.height),
                                strokeWidth = 1.5f
                            )
                            drawLine(
                                color = strokeColor,
                                start = Offset(size.width * 0.65f, 0f),
                                end = Offset(size.width * 0.65f, size.height),
                                strokeWidth = 1.5f
                            )
                            drawRoundRect(
                                color = strokeColor,
                                topLeft = Offset(size.width * 0.35f, size.height * 0.28f),
                                size = Size(size.width * 0.3f, size.height * 0.44f),
                                cornerRadius = CornerRadius(4f, 4f),
                                style = Stroke(width = 1.5f)
                            )
                        }
                    }

                    // Bottom-Right (BottomStart in RTL): "سرشماره پیامک" & "• • • • • • • •" or actual sender number
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 22.dp, bottom = 18.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "سرشماره پیامک",
                            color = Color(0xFFCBD5E1),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (senderInput.isBlank()) "• • • • • • • •" else senderInput,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.testTag("card_preview_sender_text")
                        )
                    }
                }

                // Section Title: انتخاب بانک
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "انتخاب بانک",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569)
                    )
                    Text(
                        text = if (showMoreBanks) "نمایش ۸ بانک اصلی" else "سایر بانک‌ها (+۴)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0284C7),
                        modifier = Modifier
                            .clickable { showMoreBanks = !showMoreBanks }
                            .padding(4.dp)
                    )
                }

                // 4-Column Grid of Banks inside light gray rounded container (#F1F5F9)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(vertical = 14.dp, horizontal = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    displayedBanks.chunked(4).forEach { rowBanks ->
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
                                            // If user already picked a previous bank's default sender, update or keep blank for clean selection
                                            if (senderInput.isNotBlank() && MIRZA_VISUAL_BANKS.any { it.defaultSender == senderInput }) {
                                                senderInput = item.defaultSender
                                            }
                                        }
                                        .padding(vertical = 4.dp)
                                        .testTag("bank_grid_item_${item.bankType.name}")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                            .border(
                                                width = if (isSelected) 2.5.dp else 1.dp,
                                                color = if (isSelected) Color(0xFF0B315C) else Color(0xFFE2E8F0),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        BankEmblemBadge(option = item, sizeDp = 36)
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

                // SMS Sender Input Section ("سرشماره پیامک")
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "سرشماره پیامک",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569)
                        )
                        Text(
                            text = "درج سرشماره پیش‌فرض (${selectedOption.defaultSender})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E6F30),
                            modifier = Modifier
                                .clickable { senderInput = selectedOption.defaultSender }
                                .padding(2.dp)
                                .testTag("fill_default_sender_btn")
                        )
                    }

                    // In RTL: First item is on the Right (TextField), Second item is on the Left (Light-blue Inbox Button)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = senderInput,
                            onValueChange = { senderInput = it },
                            placeholder = {
                                Text(
                                    text = "مثلاً: ${selectedOption.defaultSender}",
                                    color = Color(0xFF64748B),
                                    fontSize = 14.sp
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF1F5F9),
                                unfocusedContainerColor = Color(0xFFF1F5F9),
                                focusedBorderColor = Color(0xFF0B315C),
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("bank_sender_input")
                        )

                        // Light-blue square button with Green Inbox icon (exact match to screenshot)
                        IconButton(
                            onClick = { showSenderListPicker = !showSenderListPicker },
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFDCEEFF))
                                .testTag("sender_list_picker_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inbox,
                                contentDescription = "انتخاب سرشماره از پیامک‌های دریافتی",
                                tint = Color(0xFF1E6F30),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Text(
                        text = "می‌توانید از دکمه لیست، سرشماره‌های دریافت‌شده را انتخاب کنید",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }

                // Expandable SMS Inbox Sender Picker (with last SMS preview & recommended bank match)
                if (showSenderListPicker) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFEFF6FF))
                            .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(16.dp))
                            .padding(12.dp)
                            .testTag("received_sms_senders_list"),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Inbox, contentDescription = null, tint = Color(0xFF1E3A8A), modifier = Modifier.size(14.dp))
                                Text(
                                    text = "سرشماره‌های دریافت‌شده در پیامک‌های شما:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF1E3A8A)
                                )
                            }
                            Text(
                                text = "بستن",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0284C7),
                                modifier = Modifier.clickable { showSenderListPicker = false }
                            )
                        }

                        // Sort so the sender matching selectedOption.bankType is shown first
                        val sortedSenders = remember(receivedInboxSenders, selectedOption) {
                            receivedInboxSenders.sortedByDescending { it.suggestedBank == selectedOption.bankType }
                        }

                        sortedSenders.forEach { item ->
                            val isRecommended = item.suggestedBank == selectedOption.bankType
                            val isChosen = senderInput == item.senderNumber
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isRecommended) Color(0xFFF0FDF4) else Color.White)
                                    .border(
                                        width = if (isRecommended || isChosen) 1.5.dp else 0.5.dp,
                                        color = when {
                                            isChosen -> Color(0xFF1E6F30)
                                            isRecommended -> Color(0xFF4ADE80)
                                            else -> Color(0xFFE2E8F0)
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        senderInput = item.senderNumber
                                        requireSenderMatch = true
                                        showSenderListPicker = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 9.dp)
                                    .testTag("sms_sender_option_${item.senderNumber}")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = item.bankTitleFa,
                                            fontSize = 12.sp,
                                            color = Color(0xFF0F172A),
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (isRecommended) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFFDCFCE7))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "پیشنهادی این بانک",
                                                    fontSize = 9.sp,
                                                    color = Color(0xFF166534),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = item.senderNumber,
                                            fontSize = 13.sp,
                                            color = Color(0xFF1E6F30),
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        if (isChosen) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF1E6F30),
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "آخرین پیامک: ${item.lastMessagePreview}",
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Strict Sender-Lock Guarantee Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF0FDF4))
                        .border(1.dp, Color(0xFFBBF7D0), RoundedCornerShape(12.dp))
                        .clickable { requireSenderMatch = !requireSenderMatch }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = requireSenderMatch,
                        onCheckedChange = { requireSenderMatch = it },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF1E6F30))
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF166534),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "فقط پیامک‌های ارسالی از همین سرشماره خوانده شود",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF14532D)
                            )
                        }
                        Text(
                            text = "جلوگیری ۱۰۰٪ از ثبت اشتباه ورودی و خروجی از شماره‌های متفرقه یا بانک‌های دیگر",
                            fontSize = 10.sp,
                            color = Color(0xFF166534)
                        )
                    }
                }

                // Bottom Action Buttons:
                // In RTL, First item is on the Right ("انصراف"), Second item is on the Left (Green "+ ثبت بانک")
                // matching Screenshot_20261030_075647_ .jpg!
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(0.85f)
                            .height(52.dp)
                            .testTag("cancel_add_bank_btn")
                    ) {
                        Text(
                            text = "انصراف",
                            color = Color(0xFF475569),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            val finalSender = senderInput.trim().ifBlank { selectedOption.defaultSender }
                            onSaveBankConfig(
                                selectedOption.bankType,
                                finalSender,
                                requireSenderMatch
                            )
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF257330)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.35f)
                            .height(52.dp)
                            .testTag("confirm_add_bank_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ثبت بانک",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Renders the distinctive visual emblem for each Iranian bank inside the white circular badge,
 * matching the aesthetic of the user's screenshot.
 */
@Composable
private fun BankEmblemBadge(option: VisualBankOption, sizeDp: Int) {
    when (option.bankType) {
        BankType.BLUBANK, BankType.BLUBANK_POL -> {
            // Rounded blue square with lowercase 'b' inside (exact BluBank icon style)
            Box(
                modifier = Modifier
                    .size(sizeDp.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background( Color(0xFF4B92E3)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "b",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = (sizeDp * 0.62f).sp
                )
            }
        }
        BankType.MELLI -> {
            // Golden crest ring with red center emblem (Bank Melli style)
            Box(
                modifier = Modifier
                    .size(sizeDp.dp)
                    .clip(CircleShape)
                    .border(2.5.dp, Color(0xFFD97706), CircleShape)
                    .background(Color(0xFFFFFBEB)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ملی",
                    color = Color(0xFFB91C1C),
                    fontWeight = FontWeight.Black,
                    fontSize = (sizeDp * 0.34f).sp
                )
            }
        }
        BankType.GARDESHGARI -> {
            // Red geometric hexagon with cyan core (Gardeshgari style)
            Box(
                modifier = Modifier
                    .size(sizeDp.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(2.5.dp, Color(0xFFB91C1C), RoundedCornerShape(10.dp))
                    .background(Color(0xFFFEF2F2)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size((sizeDp * 0.32f).dp)
                        .clip(CircleShape)
                        .background(Color(0xFF06B6D4))
                )
            }
        }
        BankType.SADERAT -> {
            // Deep navy pillar emblem (Saderat style)
            Box(
                modifier = Modifier
                    .size(sizeDp.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E295B)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size((sizeDp * 0.35f).dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }
        BankType.MELLAT -> {
            // Crimson 3-diamond emblem with warm gold core (Mellat style)
            Box(
                modifier = Modifier
                    .size(sizeDp.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFBE123C)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size((sizeDp * 0.36f).dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFBBF24))
                )
            }
        }
        BankType.TEJARAT -> {
            // Royal blue sunburst emblem (Tejarat style)
            Box(
                modifier = Modifier
                    .size(sizeDp.dp)
                    .clip(CircleShape)
                    .border(2.5.dp, Color(0xFF1D4ED8), CircleShape)
                    .background(Color(0xFFEFF6FF)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ت",
                    color = Color(0xFF1E3A8A),
                    fontWeight = FontWeight.Black,
                    fontSize = (sizeDp * 0.45f).sp
                )
            }
        }
        BankType.KESHAVARZI -> {
            // Olive/Gold wheat crest (Keshavarzi style)
            Box(
                modifier = Modifier
                    .size(sizeDp.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color(0xFFA16207), CircleShape)
                    .background(Color(0xFFFEFCE8)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ک",
                    color = Color(0xFF3F6212),
                    fontWeight = FontWeight.Black,
                    fontSize = (sizeDp * 0.45f).sp
                )
            }
        }
        else -> {
            Box(
                modifier = Modifier
                    .size(sizeDp.dp)
                    .clip(CircleShape)
                    .background(option.badgeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option.badgeText,
                    color = option.badgeColor,
                    fontWeight = FontWeight.Black,
                    fontSize = if (option.badgeText.length == 1) (sizeDp * 0.48f).sp else (sizeDp * 0.32f).sp
                )
            }
        }
    }
}
