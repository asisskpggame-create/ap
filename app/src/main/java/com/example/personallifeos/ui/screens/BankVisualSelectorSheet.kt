package com.example.personallifeos.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.personallifeos.sms.BankNotificationItem
import com.example.personallifeos.sms.BankType
import com.example.personallifeos.sms.IranianBankSmsEngine
import com.example.personallifeos.sms.ReceivedSmsSenderItem

data class VisualBankOption(
    val bankType: BankType,
    val labelFa: String,
    val cardTitleFa: String,
    val badgeText: String,
    val badgeColor: Color,
    val defaultSender: String
)

/**
 * Primary 8 banks in the exact 2x4 order of the Mirza screenshot
 */
val PRIMARY_MIRZA_BANKS = listOf(
    VisualBankOption(BankType.MELLI, "ملی", "بانک ملی", "ملی", Color(0xFFB45309), "98300017"),
    VisualBankOption(BankType.BLUBANK, "بلو بانک", "بلو بانک", "b", Color(0xFF3B82F6), "982000"),
    VisualBankOption(BankType.BLUBANK_POL, "بلوبانک حالت پل", "بلوبانک حالت پل", "b", Color(0xFF2563EB), "30009601"),
    VisualBankOption(BankType.GARDESHGARI, "گردشگری", "بانک گردشگری", "گ", Color(0xFFDC2626), "98200064"),
    VisualBankOption(BankType.SADERAT, "صادرات", "بانک صادرات", "ص", Color(0xFF1E3A8A), "98300060"),
    VisualBankOption(BankType.MELLAT, "ملت", "بانک ملت", "ملت", Color(0xFFE11D48), "98200033"),
    VisualBankOption(BankType.TEJARAT, "تجارت", "بانک تجارت", "ت", Color(0xFF1D4ED8), "98200070"),
    VisualBankOption(BankType.KESHAVARZI, "کشاورزی", "بانک کشاورزی", "ک", Color(0xFF15803D), "98200077")
)

/**
 * Complete catalog of ALL 34 Iranian banks and financial institutions + Custom Bank option
 */
val ALL_IRANIAN_VISUAL_BANKS = listOf(
    // 8 Primary banks
    VisualBankOption(BankType.MELLI, "ملی", "بانک ملی", "ملی", Color(0xFFB45309), "98300017"),
    VisualBankOption(BankType.BLUBANK, "بلو بانک", "بلو بانک", "b", Color(0xFF3B82F6), "982000"),
    VisualBankOption(BankType.BLUBANK_POL, "بلوبانک حالت پل", "بلوبانک حالت پل", "b", Color(0xFF2563EB), "30009601"),
    VisualBankOption(BankType.GARDESHGARI, "گردشگری", "بانک گردشگری", "گ", Color(0xFFDC2626), "98200064"),
    VisualBankOption(BankType.SADERAT, "صادرات", "بانک صادرات", "ص", Color(0xFF1E3A8A), "98300060"),
    VisualBankOption(BankType.MELLAT, "ملت", "بانک ملت", "ملت", Color(0xFFE11D48), "98200033"),
    VisualBankOption(BankType.TEJARAT, "تجارت", "بانک تجارت", "ت", Color(0xFF1D4ED8), "98200070"),
    VisualBankOption(BankType.KESHAVARZI, "کشاورزی", "بانک کشاورزی", "ک", Color(0xFF15803D), "98200077"),
    // All other Iranian banks & institutions
    VisualBankOption(BankType.SEPAH, "سپه", "بانک سپه", "سپه", Color(0xFFD97706), "98300015"),
    VisualBankOption(BankType.SAMAN, "سامان", "بانک سامان", "س", Color(0xFF0284C7), "9820000"),
    VisualBankOption(BankType.PASARGAD, "پاسارگاد", "بانک پاسارگاد", "پ", Color(0xFFB45309), "982000500"),
    VisualBankOption(BankType.VEPOD, "ویپاد", "ویپاد (پاسارگاد)", "v", Color(0xFF10B981), "Wepod"),
    VisualBankOption(BankType.REFAH, "رفاه", "بانک رفاه کارگران", "ر", Color(0xFF0D9488), "98300007"),
    VisualBankOption(BankType.MASKAN, "مسکن", "بانک مسکن", "م", Color(0xFFEA580C), "98300014"),
    VisualBankOption(BankType.RESALAT, "رسالت", "بانک قرض‌الحسنه رسالت", "رسالت", Color(0xFF2563EB), "982000010"),
    VisualBankOption(BankType.MEHR_IRAN, "مهر ایران", "بانک مهر ایران", "مهر", Color(0xFF0D9488), "98300019"),
    VisualBankOption(BankType.PARSIAN, "پارسیان", "بانک پارسیان", "پارسیان", Color(0xFF7C3AED), "98300003"),
    VisualBankOption(BankType.AYANDEH, "آینده", "بانک آینده", "آینده", Color(0xFF9333EA), "98300035"),
    VisualBankOption(BankType.SHAHR, "شهر", "بانک شهر", "شهر", Color(0xFFDC2626), "98200080"),
    VisualBankOption(BankType.SINA, "سینا", "بانک سینا", "سینا", Color(0xFF0284C7), "98300055"),
    VisualBankOption(BankType.EGHTESAD_NOVIN, "اقتصاد نوین", "بانک اقتصاد نوین", "نوین", Color(0xFF9333EA), "98300045"),
    VisualBankOption(BankType.DAY, "دی", "بانک دی", "دی", Color(0xFFEA580C), "98300073"),
    VisualBankOption(BankType.KARAFARIN, "کارآفرین", "بانک کارآفرین", "کار", Color(0xFF16A34A), "98300050"),
    VisualBankOption(BankType.IRAN_ZAMIN, "ایران زمین", "بانک ایران زمین", "زمین", Color(0xFF0D9488), "98300088"),
    VisualBankOption(BankType.SARMAYE, "سرمایه", "بانک سرمایه", "سرمایه", Color(0xFF2563EB), "98300021"),
    VisualBankOption(BankType.POST_BANK, "پست بانک", "پست بانک ایران", "پست", Color(0xFF059669), "98300067"),
    VisualBankOption(BankType.TOSEE_TAAVON, "توسعه تعاون", "بانک توسعه تعاون", "تعاون", Color(0xFF0D9488), "98300082"),
    VisualBankOption(BankType.SANAT_MADAN, "صنعت و معدن", "بانک صنعت و معدن", "صنعت", Color(0xFF6366F1), "98300040"),
    VisualBankOption(BankType.TOSEE_SADERAT, "توسعه صادرات", "بانک توسعه صادرات", "توسعه", Color(0xFF4F46E5), "98300095"),
    VisualBankOption(BankType.KHAVARMIANEH, "خاورمیانه", "بانک خاورمیانه", "خاور", Color(0xFF475569), "98300091"),
    VisualBankOption(BankType.MELAL, "ملل", "مؤسسه اعتباری ملل", "ملل", Color(0xFF059669), "98300025"),
    VisualBankOption(BankType.KOSAR, "کوثر", "مؤسسه اعتباری کوثر", "کوثر", Color(0xFFF59E0B), "98300030"),
    VisualBankOption(BankType.NOOR, "نور", "مؤسسه اعتباری نور", "نور", Color(0xFFEAB308), "98300038"),
    VisualBankOption(BankType.CUSTOM, "سایر / سفارشی", "بانک یا حساب سفارشی", "+", Color(0xFF6366F1), "")
)

val MIRZA_VISUAL_BANKS = ALL_IRANIAN_VISUAL_BANKS

@Composable
fun BankVisualSelectorDialog(
    isOpen: Boolean,
    liveNotifications: List<BankNotificationItem> = emptyList(),
    initialBank: BankType? = null,
    initialSender: String? = null,
    onDismiss: () -> Unit,
    onSaveBankConfig: (BankType, String, Boolean, String?) -> Unit
) {
    if (!isOpen) return

    val context = LocalContext.current
    val initialOption = remember(initialBank) {
        ALL_IRANIAN_VISUAL_BANKS.firstOrNull { it.bankType == initialBank } ?: ALL_IRANIAN_VISUAL_BANKS.first()
    }
    var selectedOption by remember(initialBank) { mutableStateOf(initialOption) }
    var senderInput by remember(initialBank, initialSender) { mutableStateOf(initialSender ?: "") }
    var customBankNameInput by remember { mutableStateOf("") }
    var requireSenderMatch by remember { mutableStateOf(true) }
    var showDeviceSmsSearchModal by remember { mutableStateOf(false) }
    var showAllBanksView by remember { mutableStateOf(false) }
    var bankSearchQuery by remember { mutableStateOf("") }

    // Filter banks based on search query or view mode
    val displayedBanks = remember(showAllBanksView, bankSearchQuery) {
        if (bankSearchQuery.isNotBlank()) {
            val q = bankSearchQuery.trim().lowercase()
            ALL_IRANIAN_VISUAL_BANKS.filter {
                it.labelFa.lowercase().contains(q) ||
                it.cardTitleFa.lowercase().contains(q) ||
                it.defaultSender.lowercase().contains(q)
            }
        } else if (showAllBanksView) {
            ALL_IRANIAN_VISUAL_BANKS
        } else {
            PRIMARY_MIRZA_BANKS
        }
    }

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
                val cardDisplayTitle = if (selectedOption.bankType == BankType.CUSTOM && customBankNameInput.isNotBlank()) {
                    customBankNameInput.trim()
                } else {
                    selectedOption.cardTitleFa
                }

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
                                text = cardDisplayTitle,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                            if (senderInput.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFF86EFAC),
                                        modifier = Modifier.size(11.dp)
                                    )
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

                // Section Title & Bank Search / All Banks Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "انتخاب بانک (${displayedBanks.size} بانک)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569)
                    )
                    Text(
                        text = if (showAllBanksView) "نمایش ۸ بانک اصلی" else "همه بانک‌ها (۳۴ مورد)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0284C7),
                        modifier = Modifier
                            .clickable {
                                showAllBanksView = !showAllBanksView
                                if (!showAllBanksView) bankSearchQuery = ""
                            }
                            .padding(4.dp)
                            .testTag("toggle_all_banks_btn")
                    )
                }

                // Search Bar for Banks when browsing all banks or searching
                AnimatedVisibility(visible = showAllBanksView || bankSearchQuery.isNotBlank()) {
                    OutlinedTextField(
                        value = bankSearchQuery,
                        onValueChange = { bankSearchQuery = it },
                        placeholder = { Text("جستجو در بین تمام بانک‌ها...", fontSize = 12.sp, color = Color(0xFF64748B)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (bankSearchQuery.isNotBlank()) {
                                IconButton(onClick = { bankSearchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "پاک کردن", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color(0xFF0284C7),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
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
                    if (displayedBanks.isEmpty()) {
                        Text(
                            text = "بانکی با این نام یافت نشد. می‌توانید از گزینه «سایر / سفارشی» استفاده کنید.",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }

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
                                            if (senderInput.isNotBlank() && ALL_IRANIAN_VISUAL_BANKS.any { it.defaultSender == senderInput }) {
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
                            // Fill remaining spaces in the row if rowBanks < 4
                            for (i in 0 until (4 - rowBanks.size)) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                // Custom Bank Name Field (when CUSTOM is chosen)
                AnimatedVisibility(
                    visible = selectedOption.bankType == BankType.CUSTOM,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFEEF2FF))
                            .border(1.dp, Color(0xFFC7D2FE), RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "نام اختصاصی بانک یا حساب:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3730A3)
                        )
                        OutlinedTextField(
                            value = customBankNameInput,
                            onValueChange = { customBankNameInput = it },
                            placeholder = { Text("مثلاً: بانک خاورمیانه، صندوق قرض‌الحسنه و...", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // SMS Sender Input Section ("سرشماره پیامک")
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        if (selectedOption.defaultSender.isNotBlank()) {
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
                    }

                    // In RTL: TextField on the right, SMS Inbox button on the left (matching user screenshot)
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
                                    text = if (selectedOption.defaultSender.isNotBlank()) "مثلاً: ${selectedOption.defaultSender}" else "مثلاً: 98300017",
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

                        // Light-blue square button with Green Inbox icon (matching screenshot)
                        IconButton(
                            onClick = { showDeviceSmsSearchModal = true },
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFDCEEFF))
                                .border(1.dp, Color(0xFFBAE6FD), RoundedCornerShape(14.dp))
                                .testTag("sender_list_picker_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inbox,
                                contentDescription = "انتخاب و جستجو در سرشماره‌های پیامک‌های گوشی",
                                tint = Color(0xFF1E6F30),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Direct Action Link to Search SMS
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFEFF6FF))
                            .clickable { showDeviceSmsSearchModal = true }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Sms, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp))
                            Text(
                                text = "جستجو در شماره‌های داخل پیامک‌های گوشی من",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0369A1)
                            )
                        }
                        Text(
                            text = "جستجو و انتخاب ❯",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0284C7)
                        )
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

                // Bottom Action Buttons: "انصراف" on right, "+ ثبت بانک" on left
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
                            val customName = if (selectedOption.bankType == BankType.CUSTOM) customBankNameInput.trim().ifBlank { null } else null
                            onSaveBankConfig(
                                selectedOption.bankType,
                                finalSender,
                                requireSenderMatch,
                                customName
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

    // Comprehensive Device SMS Search & Pick Dialog
    if (showDeviceSmsSearchModal) {
        DeviceSmsSearchDialog(
            isOpen = true,
            liveNotifications = liveNotifications,
            selectedBank = selectedOption.bankType,
            onDismiss = { showDeviceSmsSearchModal = false },
            onSelectSender = { senderNumber, suggestedBank ->
                senderInput = senderNumber
                requireSenderMatch = true
                if (suggestedBank != BankType.UNKNOWN) {
                    val matching = ALL_IRANIAN_VISUAL_BANKS.firstOrNull { it.bankType == suggestedBank }
                    if (matching != null) {
                        selectedOption = matching
                    }
                }
                showDeviceSmsSearchModal = false
            }
        )
    }
}

/**
 * Backwards-compatible overload with 3-arg callback
 */
@Composable
fun BankVisualSelectorDialog(
    isOpen: Boolean,
    liveNotifications: List<BankNotificationItem> = emptyList(),
    initialBank: BankType? = null,
    initialSender: String? = null,
    onDismiss: () -> Unit,
    onSaveBankConfig: (BankType, String, Boolean) -> Unit
) {
    BankVisualSelectorDialog(
        isOpen = isOpen,
        liveNotifications = liveNotifications,
        initialBank = initialBank,
        initialSender = initialSender,
        onDismiss = onDismiss,
        onSaveBankConfig = { bank, sender, req, _ -> onSaveBankConfig(bank, sender, req) }
    )
}

/**
 * Dedicated Modal for searching and selecting actual SMS sender numbers from the phone's SMS inbox.
 * Allows filtering by sender number, bank name, or message snippet.
 */
@Composable
fun DeviceSmsSearchDialog(
    isOpen: Boolean,
    liveNotifications: List<BankNotificationItem> = emptyList(),
    selectedBank: BankType = BankType.UNKNOWN,
    onDismiss: () -> Unit,
    onSelectSender: (senderNumber: String, suggestedBank: BankType) -> Unit
) {
    if (!isOpen) return

    val context = LocalContext.current
    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasSmsPermission = isGranted
    }

    var searchQuery by remember { mutableStateOf("") }
    var senderList by remember(hasSmsPermission, liveNotifications) {
        mutableStateOf(IranianBankSmsEngine.readDeviceSmsInboxSenders(context, liveNotifications))
    }

    // Refresh senders when permission changes
    LaunchedEffect(hasSmsPermission) {
        senderList = IranianBankSmsEngine.readDeviceSmsInboxSenders(context, liveNotifications)
    }

    val filteredSenders = remember(senderList, searchQuery, selectedBank) {
        val q = searchQuery.trim().lowercase()
        val list = if (q.isBlank()) {
            senderList
        } else {
            senderList.filter {
                it.senderNumber.lowercase().contains(q) ||
                it.bankTitleFa.lowercase().contains(q) ||
                it.lastMessagePreview.lowercase().contains(q)
            }
        }
        // Sort items so that items matching the currently selected bank or having highest messages are top
        list.sortedWith(
            compareByDescending<ReceivedSmsSenderItem> { it.suggestedBank == selectedBank }
                .thenByDescending { it.messageCount }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(26.dp))
                .testTag("device_sms_search_dialog"),
            color = Color(0xFFF8FAFC)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "جستجو در پیامک‌های من",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "سرشماره‌های بانکی و متنی از صندوق پیامک‌ها",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن", tint = Color(0xFF475569))
                    }
                }

                // Permission Warning & Request Card (if not yet granted)
                if (!hasSmsPermission) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFFEF3C7))
                            .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Sms, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(18.dp))
                            Text(
                                text = "دسترسی به پیامک‌های گوشی برای خواندن سرشماره‌ها",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                        }
                        Text(
                            text = "برای اسکن مستقیم و استخراج خودکار شماره تمام بانک‌ها از صندوق پیامک‌های شما، دسترسی را تایید کنید:",
                            fontSize = 11.sp,
                            color = Color(0xFF78350F)
                        )
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.READ_SMS) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                        ) {
                            Text("تایید دسترسی به پیامک‌ها", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Live Search Input Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "جستجو با شماره سرشماره، نام بانک، یا متن پیامک...",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "پاک کردن", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFF0284C7),
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("sms_search_input")
                )

                // Stats Banner
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تعداد سرشماره‌های یافت‌شده: ${filteredSenders.size} شماره",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569)
                    )
                    if (searchQuery.isNotBlank()) {
                        Text(
                            text = "فیلتر شده با: \"$searchQuery\"",
                            fontSize = 11.sp,
                            color = Color(0xFF0284C7)
                        )
                    }
                }

                // List of Detected Senders from Device & History
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (filteredSenders.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.Inbox,
                                        contentDescription = null,
                                        tint = Color(0xFFCBD5E1),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "سرشماره‌ای مطابق با جستجوی شما یافت نشد",
                                        fontSize = 13.sp,
                                        color = Color(0xFF64748B),
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "می‌توانید شماره را مستقیماً به‌صورت دستی در کادر وارد کنید.",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredSenders, key = { it.senderNumber }) { item ->
                            val isBankMatch = item.suggestedBank == selectedBank
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isBankMatch) Color(0xFFF0FDF4) else Color.White)
                                    .border(
                                        width = if (isBankMatch) 1.5.dp else 1.dp,
                                        color = if (isBankMatch) Color(0xFF22C55E) else Color(0xFFE2E8F0),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable {
                                        onSelectSender(item.senderNumber, item.suggestedBank)
                                    }
                                    .padding(14.dp)
                                    .testTag("sms_sender_item_${item.senderNumber}")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        // Sender Number pill
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFE0F2FE))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = item.senderNumber,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF0369A1)
                                            )
                                        }

                                        if (item.messageCount > 1) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFFF1F5F9))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "${item.messageCount} پیامک",
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF475569),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    // Suggested Bank Badge
                                    if (item.suggestedBank != BankType.UNKNOWN) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFFDCFCE7))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = item.bankTitleFa,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF15803D)
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = item.receivedTimeFa,
                                            fontSize = 10.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Last SMS Snippet
                                Text(
                                    text = "متن پیامک: ${item.lastMessagePreview}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF334155),
                                    lineHeight = 16.sp
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "انتخاب این سرشماره ❯",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E6F30)
                                    )
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
 * Renders the distinctive visual emblem for each Iranian bank inside the white circular badge,
 * matching the aesthetic of the user's screenshot.
 */
@Composable
private fun BankEmblemBadge(option: VisualBankOption, sizeDp: Int) {
    when (option.bankType) {
        BankType.BLUBANK, BankType.BLUBANK_POL -> {
            Box(
                modifier = Modifier
                    .size(sizeDp.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF4B92E3)),
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
        BankType.SEPAH -> {
            Box(
                modifier = Modifier
                    .size(sizeDp.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color(0xFFD97706), CircleShape)
                    .background(Color(0xFFFFFBEB)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "سپه",
                    color = Color(0xFFB45309),
                    fontWeight = FontWeight.Black,
                    fontSize = (sizeDp * 0.35f).sp
                )
            }
        }
        BankType.SAMAN -> {
            Box(
                modifier = Modifier
                    .size(sizeDp.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0F2FE)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "س",
                    color = Color(0xFF0369A1),
                    fontWeight = FontWeight.Black,
                    fontSize = (sizeDp * 0.45f).sp
                )
            }
        }
        BankType.PASARGAD, BankType.VEPOD -> {
            Box(
                modifier = Modifier
                    .size(sizeDp.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFEF3C7)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (option.bankType == BankType.VEPOD) "v" else "پ",
                    color = Color(0xFFB45309),
                    fontWeight = FontWeight.Black,
                    fontSize = (sizeDp * 0.45f).sp
                )
            }
        }
        BankType.CUSTOM -> {
            Box(
                modifier = Modifier
                    .size(sizeDp.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEEF2FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "سایر بانک‌ها",
                    tint = Color(0xFF4F46E5),
                    modifier = Modifier.size((sizeDp * 0.55f).dp)
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
                    fontSize = if (option.badgeText.length == 1) (sizeDp * 0.48f).sp else (sizeDp * 0.30f).sp
                )
            }
        }
    }
}
