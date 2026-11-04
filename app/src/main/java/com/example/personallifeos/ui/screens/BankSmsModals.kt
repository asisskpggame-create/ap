package com.example.personallifeos.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.personallifeos.data.local.SmsProcessingHistoryEntity
import com.example.personallifeos.sms.BankSmsParsedResult
import com.example.personallifeos.sms.BankSmsProcessingEngine
import com.example.personallifeos.sms.BankType
import com.example.personallifeos.sms.BankUserConfig
import com.example.personallifeos.sms.DetectedCurrencyUnit
import com.example.personallifeos.sms.SmsProcessingResult
import com.example.personallifeos.sms.SmsTransactionDirection
import com.example.personallifeos.ui.theme.AmberWarning
import com.example.personallifeos.ui.theme.CyanPrimary
import com.example.personallifeos.ui.theme.EmeraldAccent
import com.example.personallifeos.ui.theme.RoseDanger
import com.example.personallifeos.ui.theme.SurfaceCard
import com.example.personallifeos.ui.theme.SurfaceDark
import com.example.personallifeos.ui.theme.TextMuted
import com.example.personallifeos.ui.theme.TextPrimary
import com.example.personallifeos.ui.theme.TextSecondary
import com.example.personallifeos.util.PersianUtils

@Composable
fun BankEditDialog(
    config: BankUserConfig,
    onDismiss: () -> Unit,
    onSave: (BankType, String, Boolean, String?) -> Unit
) {
    var customName by remember { mutableStateOf(config.customBankName ?: "") }
    var senderNumber by remember { mutableStateOf(config.senderNumber) }
    var requireSenderMatch by remember { mutableStateOf(config.requireSenderMatch) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SurfaceDark,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ویرایش تنظیمات ${config.bank.faName}",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "بستن", tint = TextMuted)
                    }
                }

                OutlinedTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    label = { Text("نام دلخواه حساب / کارت (اختیاری)", fontSize = 12.sp) },
                    placeholder = { Text("مثلاً: حساب حقوق، کارت روزمره", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                    )
                )

                OutlinedTextField(
                    value = senderNumber,
                    onValueChange = { senderNumber = it.filter { ch -> ch.isDigit() || ch == '+' } },
                    label = { Text("سرشماره فرستنده پیامک", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                    )
                )

                if (config.bank.defaultSenders.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("سرشماره‌های استاندارد این بانک:", fontSize = 11.sp, color = TextMuted)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(config.bank.defaultSenders) { s ->
                                FilterChip(
                                    selected = senderNumber == s,
                                    onClick = { senderNumber = s },
                                    label = { Text(s, fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyanPrimary,
                                        selectedLabelColor = Color.Black,
                                        containerColor = SurfaceCard,
                                        labelColor = TextSecondary
                                    )
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("الزام تطابق دقیق سرشماره (قفل امنیتی)", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("فقط پیامک‌های ارسالی از این سرشماره بررسی شوند", color = TextMuted, fontSize = 10.sp)
                    }
                    Switch(
                        checked = requireSenderMatch,
                        onCheckedChange = { requireSenderMatch = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = CyanPrimary
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) {
                        Text("انصراف")
                    }
                    Button(
                        onClick = {
                            onSave(config.bank, senderNumber.trim(), requireSenderMatch, customName.trim().ifBlank { null })
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = Color.Black)
                    ) {
                        Text("ذخیره تغییرات", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SmsDiagnosticsModal(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    isEngineActive: Boolean,
    hasPermission: Boolean,
    historyList: List<SmsProcessingHistoryEntity>
) {
    if (!isOpen) return
    val errorList = historyList.filter {
        it.processingResult == SmsProcessingResult.PROCESSING_ERROR.name ||
        it.processingResult == SmsProcessingResult.REJECTED.name
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = SurfaceDark,
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f).padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.BugReport, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(24.dp))
                        Text("گزارش سلامت و عیب‌یابی موتور", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "بستن", tint = TextMuted)
                    }
                }

                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceCard).padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("وضعیت موتور هوشمند:", fontSize = 11.sp, color = TextMuted)
                                Text(if (isEngineActive) "فعال و آنلاین" else "متوقف‌شده توسط کاربر", fontSize = 11.sp, color = if (isEngineActive) EmeraldAccent else AmberWarning, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("مجوز دسترسی به پیامک گوشی:", fontSize = 11.sp, color = TextMuted)
                                Text(if (hasPermission) "تأیید شده (READ_SMS)" else "مجوز داده نشده", fontSize = 11.sp, color = if (hasPermission) EmeraldAccent else AmberWarning, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("مکانیزم ضدتکرار:", fontSize = 11.sp, color = TextMuted)
                                Text("کلید SHA-256 ترکیبی فعال", fontSize = 11.sp, color = CyanPrimary, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("حریم خصوصی داده‌ها:", fontSize = 11.sp, color = TextMuted)
                                Text("۱۰۰٪ پردازش محلی آفلاین", fontSize = 11.sp, color = EmeraldAccent, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Text("پیام‌ها و خطاهای اخیر (${errorList.size} مورد):", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                    if (errorList.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceCard).padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(20.dp))
                                Text("سیستم کاملاً پایدار است و هیچ خطایی ثبت نشده است.", fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            errorList.take(20).forEach { err ->
                                Box(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SurfaceCard).padding(10.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(err.bankFaName, fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                                            Text(err.receivedTimeFa, fontSize = 9.sp, color = TextMuted)
                                        }
                                        Text(err.reviewReasonFa ?: err.processingResult, fontSize = 10.sp, color = AmberWarning)
                                        Text(err.senderAddress, fontSize = 9.sp, color = TextMuted)
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = Color.Black)
                ) {
                    Text("بستن گزارش", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SmsProcessingHistoryModal(
    isOpen: Boolean,
    historyList: List<SmsProcessingHistoryEntity>,
    onDismiss: () -> Unit,
    onClearHistory: () -> Unit,
    onDeleteItem: (String) -> Unit
) {
    if (!isOpen) return

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("همه", "ثبت خودکار", "تأیید دستی", "نیازمند بررسی", "رد شده", "تکراری")

    val filtered = remember(historyList, selectedTab) {
        when (selectedTab) {
            0 -> historyList
            1 -> historyList.filter { it.processingResult == SmsProcessingResult.AUTO_REGISTERED.name }
            2 -> historyList.filter { it.processingResult == SmsProcessingResult.MANUALLY_CONFIRMED.name }
            3 -> historyList.filter { it.processingResult == SmsProcessingResult.NEEDS_REVIEW.name }
            4 -> historyList.filter { it.processingResult == SmsProcessingResult.REJECTED.name }
            5 -> historyList.filter { it.processingResult == SmsProcessingResult.DUPLICATE.name }
            else -> historyList
        }
    }

    var selectedItemDetail by remember { mutableStateOf<SmsProcessingHistoryEntity?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(26.dp))
                .testTag("sms_history_dialog"),
            color = SurfaceDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(EmeraldAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("تاریخچه پردازش پیامک‌های بانکی", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("سوابق ممیزی و وضعیت ثبت پیامک‌ها در دیتابیس Room", color = TextMuted, fontSize = 10.sp)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (historyList.isNotEmpty()) {
                            IconButton(onClick = onClearHistory) {
                                Icon(Icons.Default.Delete, contentDescription = "پاک کردن تاریخچه", tint = RoseDanger)
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "بستن", tint = TextMuted)
                        }
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(tabs.indices.toList()) { idx ->
                        FilterChip(
                            selected = selectedTab == idx,
                            onClick = { selectedTab = idx },
                            label = { Text(tabs[idx], fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanPrimary,
                                selectedLabelColor = Color.Black,
                                containerColor = SurfaceCard,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }

                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Inbox, contentDescription = null, tint = TextMuted, modifier = Modifier.size(44.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("هیچ رکوردی در تاریخچه پردازش یافت نشد", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtered, key = { it.id }) { item ->
                            HistoryRowCard(
                                item = item,
                                onClick = { selectedItemDetail = item },
                                onDelete = { onDeleteItem(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (selectedItemDetail != null) {
        val item = selectedItemDetail!!
        AlertDialog(
            onDismissRequest = { selectedItemDetail = null },
            title = { Text("جزئیات ممیزی پردازش پیامک", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DetailRow("شناسه رکورد", item.id.take(12) + "...")
                    DetailRow("زمان دریافت", item.receivedTimeFa)
                    DetailRow("فرستنده", item.senderAddress)
                    DetailRow("منبع سرشماره", item.senderSource)
                    DetailRow("بانک", item.bankFaName)
                    DetailRow("جهت تراکنش", item.transactionDirection)
                    DetailRow("مبلغ خام", "${PersianUtils.formatPersianNumber(item.rawExtractedAmount)} ${item.detectedCurrency}")
                    DetailRow("مبلغ نهایی (تومان)", "${PersianUtils.formatPersianNumber(item.finalAmountToman)} تومان")
                    DetailRow("قالب پارسر", "${item.templateId} (نسخه ${item.templateVersion})")
                    DetailRow("وضعیت اعتبارسنجی", item.verificationStatus)
                    DetailRow("نتیجه نهایی", item.processingResult)
                    if (!item.reviewReasonFa.isNullOrBlank()) {
                        DetailRow("دلیل بررسی/رد", item.reviewReasonFa)
                    }
                    if (!item.maskedCardOrAccount.isNullOrBlank()) {
                        DetailRow("کارت/حساب", item.maskedCardOrAccount)
                    }
                    if (!item.trackingCode.isNullOrBlank()) {
                        DetailRow("کد پیگیری", item.trackingCode)
                    }
                    DetailRow("کلید ضدتکرار SHA-256", item.idempotencyKey.take(16) + "..." + item.idempotencyKey.takeLast(8))
                    if (!item.linkedExpenseId.isNullOrBlank()) {
                        DetailRow("تراکنش مالی مرتبط", item.linkedExpenseId)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedItemDetail = null }) {
                    Text("بستن", color = CyanPrimary)
                }
            }
        )
    }
}

@Composable
internal fun HistoryRowCard(
    item: SmsProcessingHistoryEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = when (item.processingResult) {
        SmsProcessingResult.AUTO_REGISTERED.name -> EmeraldAccent
        SmsProcessingResult.MANUALLY_CONFIRMED.name -> CyanPrimary
        SmsProcessingResult.NEEDS_REVIEW.name -> AmberWarning
        SmsProcessingResult.DUPLICATE.name -> Color(0xFF64748B)
        else -> RoseDanger
    }

    val statusFa = when (item.processingResult) {
        SmsProcessingResult.AUTO_REGISTERED.name -> "ثبت خودکار"
        SmsProcessingResult.MANUALLY_CONFIRMED.name -> "تأیید دستی"
        SmsProcessingResult.NEEDS_REVIEW.name -> "نیازمند بررسی"
        SmsProcessingResult.DUPLICATE.name -> "تکراری"
        SmsProcessingResult.REJECTED.name -> "رد شده"
        else -> "خطای پردازش"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(item.bankFaName, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(statusFa, color = statusColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${item.receivedTimeFa} • سرشماره: ${item.senderAddress}",
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${PersianUtils.formatPersianNumber(item.finalAmountToman)} ت",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Clear, contentDescription = "حذف رکورد", tint = TextMuted, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
internal fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextSecondary, fontSize = 11.sp)
        Text(text = value, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SmsPendingReviewModal(
    isOpen: Boolean,
    pendingSms: BankSmsParsedResult?,
    historyQueue: List<SmsProcessingHistoryEntity>,
    onDismiss: () -> Unit,
    onConfirm: (SmsTransactionDirection, DetectedCurrencyUnit, BankType?, Long?, String?) -> Unit,
    onDismissItem: () -> Unit
) {
    if (!isOpen) return

    val item = pendingSms ?: historyQueue.firstOrNull()?.let { h ->
        BankSmsParsedResult(
            bank = try { BankType.valueOf(h.detectedBank) } catch (_: Exception) { BankType.UNKNOWN },
            direction = try { SmsTransactionDirection.valueOf(h.transactionDirection) } catch (_: Exception) { SmsTransactionDirection.UNKNOWN },
            amountRial = h.rawExtractedAmount,
            amountToman = h.finalAmountToman,
            rawExtractedAmount = h.rawExtractedAmount,
            detectedCurrency = try { DetectedCurrencyUnit.valueOf(h.detectedCurrency) } catch (_: Exception) { DetectedCurrencyUnit.RIAL },
            templateId = h.templateId,
            templateVersion = h.templateVersion,
            reviewReasonFa = h.reviewReasonFa,
            idempotencyKey = h.idempotencyKey,
            summaryFa = "${h.bankFaName} • ${h.finalAmountToman} تومان",
            suggestedCategory = "روزمره"
        )
    }

    if (item == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("صف بررسی پیامک‌ها", fontWeight = FontWeight.Bold) },
            text = { Text("در حال حاضر هیچ پیامک نیازمند بررسی در سامانه وجود ندارد.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("متوجه شدم", color = CyanPrimary) }
            }
        )
        return
    }

    var chosenDirection by remember(item.idempotencyKey) {
        mutableStateOf(if (item.direction != SmsTransactionDirection.UNKNOWN) item.direction else SmsTransactionDirection.WITHDRAWAL)
    }
    var chosenCurrency by remember(item.idempotencyKey) { mutableStateOf(item.detectedCurrency) }
    var chosenBank by remember(item.idempotencyKey) { mutableStateOf(item.bank) }
    var manualAmountInput by remember(item.idempotencyKey) { mutableStateOf("") }
    var customTitleInput by remember(item.idempotencyKey) { mutableStateOf("") }

    val computedAmountToman = remember(chosenCurrency, manualAmountInput, item.rawExtractedAmount) {
        if (manualAmountInput.isNotBlank()) {
            manualAmountInput.toLongOrNull() ?: item.amountToman
        } else {
            BankSmsProcessingEngine.recalculateAmountToman(item.rawExtractedAmount, chosenCurrency)
        }
    }

    val validationError = remember(chosenDirection, chosenCurrency, chosenBank, computedAmountToman) {
        BankSmsProcessingEngine.validateManualConfirmReview(
            parsed = item,
            overrideDirection = chosenDirection,
            overrideCurrency = chosenCurrency,
            overrideBank = chosenBank,
            overrideAmountToman = computedAmountToman
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(26.dp))
                .testTag("sms_review_queue_dialog"),
            color = SurfaceDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("بررسی و تأیید تراکنش مبهم", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("تعیین نوع تراکنش و واحد پول پیش از ثبت نهایی", color = AmberWarning, fontSize = 11.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن", tint = TextMuted)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AmberWarning.copy(alpha = 0.12f))
                        .border(1.dp, AmberWarning.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(20.dp))
                        Text(
                            text = item.reviewReasonFa ?: "نوع تراکنش یا واحد پول مشخص نیست؛ لطفاً گزینه‌های زیر را تأیید کنید.",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                Text("۱. نوع تراکنش:", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        SmsTransactionDirection.WITHDRAWAL to "برداشت / خرید",
                        SmsTransactionDirection.DEPOSIT to "واریز",
                        SmsTransactionDirection.TRANSFER_CARD to "انتقال وجه"
                    ).forEach { (dir, label) ->
                        val selected = chosenDirection == dir
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) CyanPrimary.copy(alpha = 0.2f) else SurfaceCard)
                                .border(1.dp, if (selected) CyanPrimary else Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                                .clickable { chosenDirection = dir }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = if (selected) CyanPrimary else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Text("۲. واحد پول (محاسبه خودکار مبلغ):", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        DetectedCurrencyUnit.RIAL to "ریال (تقسیم بر ۱۰)",
                        DetectedCurrencyUnit.TOMAN to "تومان (مستقیم)"
                    ).forEach { (cur, label) ->
                        val selected = chosenCurrency == cur
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) EmeraldAccent.copy(alpha = 0.2f) else SurfaceCard)
                                .border(1.dp, if (selected) EmeraldAccent else Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                                .clickable { chosenCurrency = cur }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = if (selected) EmeraldAccent else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceCard)
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("مبلغ نهایی قابل ثبت:", color = TextSecondary, fontSize = 11.sp)
                        Text(
                            text = "${PersianUtils.formatPersianNumber(computedAmountToman)} تومان",
                            color = EmeraldAccent,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "مبلغ خام پیامک: ${PersianUtils.formatPersianNumber(item.rawExtractedAmount)}",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                OutlinedTextField(
                    value = manualAmountInput,
                    onValueChange = { manualAmountInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text("اصلاح دستی مبلغ به تومان (اختیاری)", fontSize = 11.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceCard,
                        unfocusedContainerColor = SurfaceCard,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = customTitleInput,
                    onValueChange = { customTitleInput = it },
                    label = { Text("عنوان دلخواه تراکنش (اختیاری)", fontSize = 11.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceCard,
                        unfocusedContainerColor = SurfaceCard,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (validationError != null) {
                    Text(validationError, color = RoseDanger, fontSize = 11.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismissItem,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("نادیده گرفتن", color = TextMuted)
                    }

                    Button(
                        onClick = {
                            onConfirm(
                                chosenDirection,
                                chosenCurrency,
                                chosenBank,
                                computedAmountToman,
                                customTitleInput.takeIf { it.isNotBlank() }
                            )
                        },
                        enabled = validationError == null && computedAmountToman > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(2f).height(48.dp).testTag("confirm_review_btn")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تأیید و ثبت در دفتر مالی", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SmsEngineReportModal(
    isOpen: Boolean,
    historyList: List<SmsProcessingHistoryEntity>,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    var selectedRangeIndex by remember { mutableIntStateOf(2) }
    val rangeLabels = listOf("امروز", "۷ روز گذشته", "۳۰ روز گذشته", "کل دوران")

    val now = System.currentTimeMillis()
    val filteredByDate = remember(historyList, selectedRangeIndex) {
        val cutoff = when (selectedRangeIndex) {
            0 -> now - (24 * 60 * 60 * 1000L)
            1 -> now - (7 * 24 * 60 * 60 * 1000L)
            2 -> now - (30 * 24 * 60 * 60 * 1000L)
            else -> 0L
        }
        historyList.filter { it.timestampMillis >= cutoff }
    }

    val totalCount = filteredByDate.size
    val autoCount = filteredByDate.count { it.processingResult == SmsProcessingResult.AUTO_REGISTERED.name }
    val manualCount = filteredByDate.count { it.processingResult == SmsProcessingResult.MANUALLY_CONFIRMED.name }
    val reviewCount = filteredByDate.count { it.processingResult == SmsProcessingResult.NEEDS_REVIEW.name }
    val rejectedCount = filteredByDate.count { it.processingResult == SmsProcessingResult.REJECTED.name }
    val duplicateCount = filteredByDate.count { it.processingResult == SmsProcessingResult.DUPLICATE.name }

    val financialCount = totalCount - rejectedCount
    val successRate = if (financialCount > 0) {
        ((autoCount + manualCount).toFloat() / financialCount * 100).toInt()
    } else 100

    val incomeSum = filteredByDate
        .filter { it.transactionDirection == SmsTransactionDirection.DEPOSIT.name }
        .sumOf { it.finalAmountToman }
    val expenseSum = filteredByDate
        .filter { it.transactionDirection == SmsTransactionDirection.WITHDRAWAL.name || it.transactionDirection == SmsTransactionDirection.PURCHASE.name }
        .sumOf { it.finalAmountToman }
    val transferSum = filteredByDate
        .filter { it.transactionDirection.startsWith("TRANSFER") }
        .sumOf { it.finalAmountToman }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(26.dp))
                .testTag("sms_report_dialog"),
            color = SurfaceDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(CyanPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Assessment, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("گزارش عملکرد موتور پردازش پیامک", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("دقت تشخیص، گردش پیامک‌ها و تفکیک تراکنش‌ها", color = TextMuted, fontSize = 10.sp)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن", tint = TextMuted)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    rangeLabels.forEachIndexed { idx, label ->
                        FilterChip(
                            selected = selectedRangeIndex == idx,
                            onClick = { selectedRangeIndex = idx },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanPrimary,
                                selectedLabelColor = Color.Black,
                                containerColor = SurfaceCard,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF0C2B4E), Color(0xFF0A223E))
                            )
                        )
                        .border(1.dp, CyanPrimary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("نرخ موفقیت تشخیص و ثبت مالی", color = TextSecondary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${PersianUtils.formatPersianNumber(successRate.toLong())}٪",
                                color = EmeraldAccent,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text("پیامک‌های غیرمالی و رمز پویا از نرخ خطا مستثنی شده‌اند", color = TextMuted, fontSize = 10.sp)
                        }
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(44.dp))
                    }
                }

                Text("تفکیک وضعیت پردازش:", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KpiMetricCard("کل پیامک‌ها", "$totalCount", TextPrimary, Modifier.weight(1f))
                    KpiMetricCard("ثبت خودکار", "$autoCount", EmeraldAccent, Modifier.weight(1f))
                    KpiMetricCard("تأیید دستی", "$manualCount", CyanPrimary, Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KpiMetricCard("نیازمند بررسی", "$reviewCount", AmberWarning, Modifier.weight(1f))
                    KpiMetricCard("رد شده/غیرمالی", "$rejectedCount", Color(0xFF94A3B8), Modifier.weight(1f))
                    KpiMetricCard("تکراری مسدود", "$duplicateCount", Color(0xFF64748B), Modifier.weight(1f))
                }

                Text("گردش مالی استخراج‌شده (انتقال‌ها تفکیک‌شده‌اند):", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceCard)
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("مجموع واریزها (درآمد):", color = EmeraldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("${PersianUtils.formatPersianNumber(incomeSum)} تومان", color = EmeraldAccent, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("مجموع برداشت و خرید (هزینه):", color = RoseDanger, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("${PersianUtils.formatPersianNumber(expenseSum)} تومان", color = RoseDanger, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("مجموع انتقال‌ها (داخلی/کارت/پل):", color = CyanPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("${PersianUtils.formatPersianNumber(transferSum)} تومان", color = CyanPrimary, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SmsTestBenchModal(
    isOpen: Boolean,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var testSmsText by remember { mutableStateOf("") }
    var testSenderInput by remember { mutableStateOf("") }
    var parsedTestResult by remember { mutableStateOf<BankSmsParsedResult?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(26.dp))
                .testTag("sms_test_bench_dialog"),
            color = SurfaceDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF472B6).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Science, contentDescription = null, tint = Color(0xFFF472B6), modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text("میز آزمایش پیامک بانکی (Test Bench)", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("تست ایمن پارسر بدون هیچ‌گونه ثبت در دفتر مالی", color = EmeraldAccent, fontSize = 10.sp)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن", tint = TextMuted)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(EmeraldAccent.copy(alpha = 0.12f))
                        .border(1.dp, EmeraldAccent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(20.dp))
                        Text(
                            text = "محیط قرنطینه ایمن: هیچ تراکنشی در دیتابیس ثبت نشده و اعلانی ارسال نمی‌شود.",
                            color = TextPrimary,
                            fontSize = 11.sp
                        )
                    }
                }

                OutlinedTextField(
                    value = testSmsText,
                    onValueChange = { testSmsText = it },
                    label = { Text("متن پیامک بانکی را اینجا جای‌گذاری کنید...", fontSize = 12.sp) },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceCard,
                        unfocusedContainerColor = SurfaceCard,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("test_sms_input")
                )

                OutlinedTextField(
                    value = testSenderInput,
                    onValueChange = { testSenderInput = it },
                    label = { Text("سرشماره فرستنده (اختیاری - مثلاً 98300017)", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceCard,
                        unfocusedContainerColor = SurfaceCard,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        parsedTestResult = BankSmsProcessingEngine.parse(
                            rawSms = testSmsText,
                            senderAddress = testSenderInput.takeIf { it.isNotBlank() }
                        )
                    },
                    enabled = testSmsText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("run_test_parse_btn")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("آزمایش و تحلیل پیامک", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                if (parsedTestResult != null) {
                    val res = parsedTestResult!!
                    Text("گزارش تحلیلی موتور:", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceCard)
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            DetailRow("بانک شناسایی‌شده", res.bank.faName)
                            DetailRow("جهت تراکنش", res.direction.faName)
                            DetailRow("مبلغ استخراج‌شده (ریال)", "${PersianUtils.formatPersianNumber(res.amountRial)} ریال")
                            DetailRow("مبلغ نهایی (تومان)", "${PersianUtils.formatPersianNumber(res.amountToman)} تومان")
                            DetailRow("واحد پول شناسایی‌شده", res.detectedCurrency.titleFa)
                            DetailRow("قالب پارسر", "${res.templateId} (نسخه ${res.templateVersion})")
                            DetailRow("وضعیت اعتبارسنجی", res.verificationStatus.titleFa)
                            if (res.reviewReasonFa != null) {
                                DetailRow("دلیل نیازمندی به بررسی", res.reviewReasonFa!!)
                            }
                            DetailRow("کلید ضدتکرار SHA-256", res.idempotencyKey.take(16) + "...")

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = {
                                    val reportText = """
                                        === گزارش عیب‌یابی پارسر پیامک بانکی ===
                                        بانک: ${res.bank.faName}
                                        جهت: ${res.direction.faName}
                                        مبلغ: ${res.amountToman} تومان
                                        قالب: ${res.templateId}
                                        وضعیت: ${res.verificationStatus.titleFa}
                                        کلید ضدتکرار: ${res.idempotencyKey}
                                    """.trimIndent()
                                    clipboardManager.setText(AnnotatedString(reportText))
                                    Toast.makeText(context, "گزارش در حافظه کپی شد", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("کپی گزارش عیب‌یابی", color = CyanPrimary, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SmsEngineSettingsModal(
    isOpen: Boolean,
    isLiveReceiverEnabled: Boolean,
    onToggleLiveReceiver: (Boolean) -> Unit,
    hasPermission: Boolean,
    onDismiss: () -> Unit,
    onClearHistory: () -> Unit
) {
    if (!isOpen) return

    var showConfirmClearDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(26.dp))
                .testTag("sms_settings_dialog"),
            color = SurfaceDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("تنظیمات موتور و حریم خصوصی", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("سیاست‌های پردازش محلی و امنیتی", color = TextMuted, fontSize = 11.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن", tint = TextMuted)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceCard)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("دریافت زنده پیامک‌های بانکی", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("پایش خودکار پیامک‌های ورودی و ثبت در دفتر مالی", color = TextMuted, fontSize = 10.sp)
                    }
                    Switch(
                        checked = isLiveReceiverEnabled,
                        onCheckedChange = onToggleLiveReceiver,
                        colors = SwitchDefaults.colors(checkedThumbColor = CyanPrimary)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0F2942))
                        .border(1.dp, CyanPrimary.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(24.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("تضمین پردازش ۱۰۰٪ محلی (Local-First):", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("• کلیه تحلیل‌ها مستقیماً روی دستگاه شما انجام می‌شود.", color = TextMuted, fontSize = 11.sp, lineHeight = 16.sp)
                            Text("• هیچ اطلاعاتی به سرور یا سرویس ابری ارسال نمی‌گردد.", color = TextMuted, fontSize = 11.sp, lineHeight = 16.sp)
                        }
                    }
                }

                OutlinedButton(
                    onClick = { showConfirmClearDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseDanger),
                    border = BorderStroke(1.dp, RoseDanger.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = RoseDanger, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("پاک کردن تمام سوابق تاریخچه پردازش", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showConfirmClearDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmClearDialog = false },
            title = { Text("پاکسازی تاریخچه پردازش", fontWeight = FontWeight.Bold) },
            text = { Text("سوابق ممیزی پیامک‌ها پاک می‌شود، اما تراکنش‌های ثبت‌شده در دفتر مالی دست‌نخورده باقی می‌مانند.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    onClearHistory()
                    showConfirmClearDialog = false
                }) {
                    Text("بله، پاک شود", color = RoseDanger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClearDialog = false }) {
                    Text("انصراف", color = TextMuted)
                }
            }
        )
    }
}
