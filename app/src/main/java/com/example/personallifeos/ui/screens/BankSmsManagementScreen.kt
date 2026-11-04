package com.example.personallifeos.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.personallifeos.data.local.SmsProcessingHistoryEntity
import com.example.personallifeos.data.model.TransactionType
import com.example.personallifeos.sms.BankNotificationItem
import com.example.personallifeos.sms.BankSmsParsedResult
import com.example.personallifeos.sms.BankSmsProcessingEngine
import com.example.personallifeos.sms.BankType
import com.example.personallifeos.sms.BankUserConfig
import com.example.personallifeos.sms.DetectedCurrencyUnit
import com.example.personallifeos.sms.SmsProcessingChannel
import com.example.personallifeos.sms.SmsProcessingResult
import com.example.personallifeos.sms.SmsSenderSource
import com.example.personallifeos.sms.SmsTransactionDirection
import com.example.personallifeos.sms.SmsVerificationStatus
import com.example.personallifeos.ui.components.SmsScanner
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

enum class SmsManagementSubscreen {
    DASHBOARD,
    HISTORY,
    REPORTING
}

/**
 * مدیریت پیامک‌های بانکی (Iranian Bank SMS Processing Engine Management)
 * Comprehensive, professional control center for managing connected banks,
 * real-time receiver, persistent history, pending review queue, performance reports, and safe test bench.
 */
@Composable
fun BankSmsManagementScreen(
    configuredBanks: List<BankUserConfig>,
    processingHistory: List<SmsProcessingHistoryEntity>,
    pendingReviewSms: BankSmsParsedResult? = null,
    isEngineActive: Boolean = true,
    onToggleEngineActive: (Boolean) -> Unit = {},
    onToggleBankConfig: (BankType, Boolean) -> Unit = { _, _ -> },
    onSaveBankConfig: (BankType, String, Boolean, String?) -> Unit,
    onDeleteBankConfig: (BankType) -> Unit,
    onConfirmReview: (SmsTransactionDirection, DetectedCurrencyUnit, BankType?, Long?, String?) -> Unit,
    onDismissReview: () -> Unit,
    onClearHistory: () -> Unit,
    onDeleteHistoryItem: (String) -> Unit,
    onAddExpense: (String, Long, String, String?, Boolean, TransactionType, String?, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentSubscreen by remember { mutableStateOf(SmsManagementSubscreen.DASHBOARD) }

    // Intercept back navigation when on History or Reporting subscreen
    BackHandler(enabled = currentSubscreen != SmsManagementSubscreen.DASHBOARD) {
        currentSubscreen = SmsManagementSubscreen.DASHBOARD
    }

    var isLiveReceiverEnabled by remember { mutableStateOf(true) }
    var editingBankConfig by remember { mutableStateOf<BankUserConfig?>(null) }
    var bankToDelete by remember { mutableStateOf<BankType?>(null) }
    var showDiagnosticsDialog by remember { mutableStateOf(false) }

    // Modals
    var showScannerDialog by remember { mutableStateOf(false) }
    var showBankSelectorDialog by remember { mutableStateOf(false) }
    var showReviewQueueDialog by remember { mutableStateOf(false) }
    var showTestBenchDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Subscreen History State
    var selectedHistoryTab by remember { mutableIntStateOf(0) }
    var historySearchQuery by remember { mutableStateOf("") }
    var selectedItemDetail by remember { mutableStateOf<SmsProcessingHistoryEntity?>(null) }

    // Subscreen Reporting State
    var selectedReportingRangeIndex by remember { mutableIntStateOf(2) }

    // Metrics computed strictly from real database history & configs
    val totalProcessed = processingHistory.size
    val autoRegisteredCount = processingHistory.count { it.processingResult == SmsProcessingResult.AUTO_REGISTERED.name }
    val manualConfirmedCount = processingHistory.count { it.processingResult == SmsProcessingResult.MANUALLY_CONFIRMED.name }
    val pendingReviewCount = processingHistory.count { it.processingResult == SmsProcessingResult.NEEDS_REVIEW.name }
    val duplicateCount = processingHistory.count { it.processingResult == SmsProcessingResult.DUPLICATE.name }
    val errorCount = processingHistory.count { it.processingResult == SmsProcessingResult.PROCESSING_ERROR.name }
    val connectedBanksCount = configuredBanks.size

    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasSmsPermission = granted
        if (!granted) {
            Toast.makeText(context, "مجوز دسترسی به پیامک داده نشد", Toast.LENGTH_SHORT).show()
        }
    }

    when (currentSubscreen) {
        SmsManagementSubscreen.HISTORY -> {
            // Dedicated History Subscreen
            val historyTabs = listOf("همه", "ثبت خودکار", "تأیید دستی", "نیازمند بررسی", "رد شده", "تکراری")
            val filteredHistory = remember(processingHistory, selectedHistoryTab, historySearchQuery) {
                val byTab = when (selectedHistoryTab) {
                    0 -> processingHistory
                    1 -> processingHistory.filter { it.processingResult == SmsProcessingResult.AUTO_REGISTERED.name }
                    2 -> processingHistory.filter { it.processingResult == SmsProcessingResult.MANUALLY_CONFIRMED.name }
                    3 -> processingHistory.filter { it.processingResult == SmsProcessingResult.NEEDS_REVIEW.name }
                    4 -> processingHistory.filter { it.processingResult == SmsProcessingResult.REJECTED.name }
                    5 -> processingHistory.filter { it.processingResult == SmsProcessingResult.DUPLICATE.name }
                    else -> processingHistory
                }
                if (historySearchQuery.isBlank()) byTab
                else byTab.filter {
                    it.bankFaName.contains(historySearchQuery, ignoreCase = true) ||
                    it.senderAddress.contains(historySearchQuery, ignoreCase = true) ||
                    (it.trackingCode?.contains(historySearchQuery) == true) ||
                    (it.reviewReasonFa?.contains(historySearchQuery) == true)
                }
            }

            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with Back Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { currentSubscreen = SmsManagementSubscreen.DASHBOARD },
                            modifier = Modifier.testTag("history_subscreen_back")
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت به پیشخوان", tint = CyanPrimary)
                        }
                        Column {
                            Text(
                                text = "تاریخچه پردازش پیامک‌ها",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "سوابق ممیزی و وضعیت ثبت پیامک‌ها در دیتابیس Room",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                    if (processingHistory.isNotEmpty()) {
                        IconButton(onClick = onClearHistory) {
                            Icon(Icons.Default.Delete, contentDescription = "پاک کردن تاریخچه", tint = RoseDanger)
                        }
                    }
                }

                // Filter Tabs
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(historyTabs.indices.toList()) { idx ->
                        FilterChip(
                            selected = selectedHistoryTab == idx,
                            onClick = { selectedHistoryTab = idx },
                            label = { Text(historyTabs[idx], fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanPrimary,
                                selectedLabelColor = Color.Black,
                                containerColor = SurfaceCard,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = historySearchQuery,
                    onValueChange = { historySearchQuery = it },
                    placeholder = { Text("جستجو در بانک، سرشماره، کد پیگیری...", fontSize = 11.sp, color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CyanPrimary) },
                    trailingIcon = {
                        if (historySearchQuery.isNotBlank()) {
                            IconButton(onClick = { historySearchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "پاک کردن", tint = TextMuted)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark
                    )
                )

                // List
                if (filteredHistory.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Inbox, contentDescription = null, tint = TextMuted, modifier = Modifier.size(44.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("هیچ رکوردی در تاریخچه پردازش یافت نشد", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        filteredHistory.forEach { item ->
                            HistoryRowCard(
                                item = item,
                                onClick = { selectedItemDetail = item },
                                onDelete = { onDeleteHistoryItem(item.id) }
                            )
                        }
                    }
                }
            }
        }

        SmsManagementSubscreen.REPORTING -> {
            // Dedicated Reporting Subscreen
            val rangeLabels = listOf("امروز", "۷ روز گذشته", "۳۰ روز گذشته", "کل دوران")
            val now = System.currentTimeMillis()
            val filteredByDate = remember(processingHistory, selectedReportingRangeIndex) {
                val cutoff = when (selectedReportingRangeIndex) {
                    0 -> now - (24 * 60 * 60 * 1000L)
                    1 -> now - (7 * 24 * 60 * 60 * 1000L)
                    2 -> now - (30 * 24 * 60 * 60 * 1000L)
                    else -> 0L
                }
                processingHistory.filter { it.timestampMillis >= cutoff }
            }

            val totalRptCount = filteredByDate.size
            val autoRptCount = filteredByDate.count { it.processingResult == SmsProcessingResult.AUTO_REGISTERED.name }
            val manualRptCount = filteredByDate.count { it.processingResult == SmsProcessingResult.MANUALLY_CONFIRMED.name }
            val reviewRptCount = filteredByDate.count { it.processingResult == SmsProcessingResult.NEEDS_REVIEW.name }
            val rejectedRptCount = filteredByDate.count { it.processingResult == SmsProcessingResult.REJECTED.name }
            val duplicateRptCount = filteredByDate.count { it.processingResult == SmsProcessingResult.DUPLICATE.name }

            val financialRptCount = totalRptCount - rejectedRptCount
            val successRate = if (financialRptCount > 0) {
                ((autoRptCount + manualRptCount).toFloat() / financialRptCount * 100).toInt()
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

            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with Back Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { currentSubscreen = SmsManagementSubscreen.DASHBOARD },
                            modifier = Modifier.testTag("report_subscreen_back")
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت به پیشخوان", tint = CyanPrimary)
                        }
                        Column {
                            Text(
                                text = "گزارش عملکرد و دقت پردازش پیامک",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "دقت تشخیص، تفکیک مبالغ و تحلیل انتقالات بانکی",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Range Selector
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    rangeLabels.forEachIndexed { idx, label ->
                        FilterChip(
                            selected = selectedReportingRangeIndex == idx,
                            onClick = { selectedReportingRangeIndex = idx },
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

                // Success Rate Gauge Card
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

                // Volume Breakdown
                Text("تفکیک وضعیت پردازش:", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KpiMetricCard("کل پیامک‌ها", "$totalRptCount", TextPrimary, Modifier.weight(1f))
                    KpiMetricCard("ثبت خودکار", "$autoRptCount", EmeraldAccent, Modifier.weight(1f))
                    KpiMetricCard("تأیید دستی", "$manualRptCount", CyanPrimary, Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KpiMetricCard("نیازمند بررسی", "$reviewRptCount", AmberWarning, Modifier.weight(1f))
                    KpiMetricCard("رد شده/غیرمالی", "$rejectedRptCount", Color(0xFF94A3B8), Modifier.weight(1f))
                    KpiMetricCard("تکراری مسدود", "$duplicateRptCount", Color(0xFF64748B), Modifier.weight(1f))
                }

                // Financial Breakdown (TRANSFERS STRICTLY SEGREGATED)
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
                            Text("انتقال بین حساب‌ها (تفکیک‌شده از درآمد/هزینه):", color = CyanPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("${PersianUtils.formatPersianNumber(transferSum)} تومان", color = CyanPrimary, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }

        SmsManagementSubscreen.DASHBOARD -> {
            // Main Bank SMS Management Dashboard
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Status & Subsystem Branding Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF0C2440), Color(0xFF07182C))
                            )
                        )
                        .border(1.dp, CyanPrimary.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                        .padding(18.dp)
                        .testTag("sms_engine_status_header")
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Title row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(CyanPrimary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Sms,
                                        contentDescription = null,
                                        tint = CyanPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "مدیریت پیامک‌های بانکی",
                                        color = TextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "موتور هوشمند پردازش پیامک بانکی (BankSmsProcessingEngine)",
                                        color = CyanPrimary,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Engine state indicator pill
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isEngineActive) EmeraldAccent.copy(alpha = 0.15f) else AmberWarning.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isEngineActive) EmeraldAccent else AmberWarning)
                                )
                                Text(
                                    text = if (isEngineActive) "موتور فعال" else "موتور متوقف",
                                    color = if (isEngineActive) EmeraldAccent else AmberWarning,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Engine State Toggle Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.Black.copy(alpha = 0.25f))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        if (isEngineActive) Icons.Default.Shield else Icons.Default.PowerSettingsNew,
                                        contentDescription = null,
                                        tint = if (isEngineActive) EmeraldAccent else AmberWarning,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "پایش و دریافت خودکار پیامک‌ها",
                                            color = TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (isEngineActive) "موتور در پس‌زمینه پیامک‌ها را پایش و ثبت می‌کند" else "موتور پایش موقتاً متوقف شده است",
                                            color = TextMuted,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                                Switch(
                                    checked = isEngineActive,
                                    onCheckedChange = onToggleEngineActive,
                                    modifier = Modifier.testTag("engine_master_toggle"),
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.Black,
                                        checkedTrackColor = EmeraldAccent
                                    )
                                )
                            }
                        }

                        // Permission & Diagnostics Status Rows
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            // SMS Permission Card
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (hasSmsPermission) EmeraldAccent.copy(alpha = 0.08f) else AmberWarning.copy(alpha = 0.12f))
                                    .border(1.dp, if (hasSmsPermission) EmeraldAccent.copy(alpha = 0.25f) else AmberWarning.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            if (hasSmsPermission) Icons.Default.CheckCircle else Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = if (hasSmsPermission) EmeraldAccent else AmberWarning,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = if (hasSmsPermission) "مجوز خواندن پیامک‌های دستگاه: تأییدشده" else "مجوز خواندن پیامک گوشی داده نشده است",
                                            color = if (hasSmsPermission) EmeraldAccent else AmberWarning,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (!hasSmsPermission) {
                                        Button(
                                            onClick = { permissionLauncher.launch(Manifest.permission.READ_SMS) },
                                            modifier = Modifier.height(32.dp).testTag("request_sms_permission_button"),
                                            colors = ButtonDefaults.buttonColors(containerColor = AmberWarning, contentColor = Color.Black),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                        ) {
                                            Text("درخواست مجوز", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            // Diagnostics & Error Logging Quick Bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceDark)
                                    .clickable { showDiagnosticsDialog = true }
                                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .testTag("engine_diagnostics_button")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.BugReport, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(16.dp))
                                        Text(
                                            text = if (errorCount == 0) "سلامت موتور: پایدار (بدون خطای سیستمی)" else "تعداد $errorCount خطای پردازش ثبت‌شده",
                                            color = if (errorCount == 0) TextSecondary else AmberWarning,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Text(
                                        text = "مشاهده لاگ و عیب‌یابی ←",
                                        color = CyanPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Summary KPI Metrics Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            KpiMetricCard(
                                title = "تراکنش ثبت‌شده",
                                value = "${autoRegisteredCount + manualConfirmedCount}",
                                color = EmeraldAccent,
                                modifier = Modifier.weight(1f)
                            )
                            KpiMetricCard(
                                title = "نیازمند بررسی",
                                value = "$pendingReviewCount",
                                color = AmberWarning,
                                modifier = Modifier.weight(1f),
                                onClick = { showReviewQueueDialog = true }
                            )
                            KpiMetricCard(
                                title = "پیامک تکراری",
                                value = "$duplicateCount",
                                color = CyanPrimary,
                                modifier = Modifier.weight(1f),
                                onClick = { currentSubscreen = SmsManagementSubscreen.HISTORY }
                            )
                            KpiMetricCard(
                                title = "بانک‌های متصل",
                                value = "$connectedBanksCount",
                                color = Color(0xFFA78BFA),
                                modifier = Modifier.weight(1f),
                                onClick = { showBankSelectorDialog = true }
                            )
                        }
                    }
                }

                // 2. Action Tiles Grid (With Dedicated Subscreen Navigation)
                Text(
                    text = "عملیات و ابزارهای موتور پیامکی",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        EngineActionTile(
                            title = "تاریخچه پردازش",
                            subtitle = "سوابق کامل پیامک‌ها و ردگیری Room",
                            icon = Icons.Default.History,
                            tint = EmeraldAccent,
                            modifier = Modifier.weight(1f),
                            testTag = "action_tile_history",
                            badge = if (totalProcessed > 0) "$totalProcessed" else null,
                            onClick = { currentSubscreen = SmsManagementSubscreen.HISTORY }
                        )
                        EngineActionTile(
                            title = "گزارش عملکرد",
                            subtitle = "آمار دقت، تفکیک مبالغ و انتقال",
                            icon = Icons.Default.Assessment,
                            tint = CyanPrimary,
                            modifier = Modifier.weight(1f),
                            testTag = "action_tile_report",
                            onClick = { currentSubscreen = SmsManagementSubscreen.REPORTING }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        EngineActionTile(
                            title = "اسکن صندوق پیامک",
                            subtitle = "خواندن پیامک‌های واقعی دستگاه",
                            icon = Icons.Default.Inbox,
                            tint = CyanPrimary,
                            modifier = Modifier.weight(1f),
                            testTag = "action_tile_scan_sms",
                            onClick = { showScannerDialog = true }
                        )
                        EngineActionTile(
                            title = "بررسی و تأیید",
                            subtitle = "پیامک‌های مبهم و نیازمند بررسی",
                            icon = Icons.Default.Rule,
                            tint = AmberWarning,
                            modifier = Modifier.weight(1f),
                            testTag = "action_tile_review_queue",
                            badge = if (pendingReviewCount > 0) "$pendingReviewCount" else null,
                            onClick = { showReviewQueueDialog = true }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        EngineActionTile(
                            title = "ثبت / اتصال بانک",
                            subtitle = "انتخاب بانک و سرشماره مطمئن",
                            icon = Icons.Default.AccountBalance,
                            tint = Color(0xFFA78BFA),
                            modifier = Modifier.weight(1f),
                            testTag = "action_tile_connect_bank",
                            onClick = { showBankSelectorDialog = true }
                        )
                        EngineActionTile(
                            title = "میز آزمایش پیامک",
                            subtitle = "تست متن بدون ثبت در دفتر مالی",
                            icon = Icons.Default.Science,
                            tint = Color(0xFFF472B6),
                            modifier = Modifier.weight(1f),
                            testTag = "action_tile_test_bench",
                            onClick = { showTestBenchDialog = true }
                        )
                    }

                    EngineActionTile(
                        title = "تنظیمات موتور و حریم خصوصی",
                        subtitle = "کنترل دریافت زنده، دسترسی‌ها و تضمین پردازش ۱۰۰٪ محلی",
                        icon = Icons.Default.Tune,
                        tint = TextSecondary,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "action_tile_settings",
                        onClick = { showSettingsDialog = true }
                    )
                }

                // 3. Connected Banks Persistent List with inline actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "بانک‌های متصل و مدیریت سرشماره‌ها (${PersianUtils.formatPersianNumber(configuredBanks.size.toLong())})",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "+ اتصال بانک جدید",
                        color = CyanPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { showBankSelectorDialog = true }
                    )
                }

                if (configuredBanks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceCard)
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("هنوز بانکی متصل نشده است", color = TextSecondary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = { showBankSelectorDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = Color.Black)
                            ) {
                                Text("اتصال اولین بانک", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        configuredBanks.forEach { cfg ->
                            ConnectedBankCard(
                                config = cfg,
                                onToggle = { enabled -> onToggleBankConfig(cfg.bank, enabled) },
                                onEdit = { editingBankConfig = cfg },
                                onDelete = { bankToDelete = cfg.bank }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal 1: Real Device SMS Scanner
    if (showScannerDialog) {
        val existingKeys = remember(processingHistory) {
            processingHistory.mapNotNull { it.idempotencyKey }.toSet()
        }
        SmsScanner(
            isOpen = true,
            onDismiss = { showScannerDialog = false },
            existingIdempotencyKeys = existingKeys,
            handleAddExpense = { title, amount, category, bankName, isSms, type, accountName, key ->
                onAddExpense(title, amount, category, bankName, isSms, type, accountName, key)
            }
        )
    }

    // Modal 2: Connect Bank / Sender Selector Sheet
    if (showBankSelectorDialog) {
        BankVisualSelectorDialog(
            isOpen = true,
            onDismiss = { showBankSelectorDialog = false },
            onSaveBankConfig = { bank, sender, req, customName ->
                onSaveBankConfig(bank, sender, req, customName)
                showBankSelectorDialog = false
            }
        )
    }

    // Modal 3: Pending Review Queue
    if (showReviewQueueDialog) {
        SmsPendingReviewModal(
            isOpen = true,
            pendingSms = pendingReviewSms,
            historyQueue = processingHistory.filter { it.processingResult == SmsProcessingResult.NEEDS_REVIEW.name },
            onDismiss = { showReviewQueueDialog = false },
            onConfirm = { dir, cur, bank, amt, title ->
                onConfirmReview(dir, cur, bank, amt, title)
                showReviewQueueDialog = false
            },
            onDismissItem = {
                onDismissReview()
                showReviewQueueDialog = false
            }
        )
    }

    // Modal 4: Test Bench
    if (showTestBenchDialog) {
        SmsTestBenchModal(
            isOpen = true,
            onDismiss = { showTestBenchDialog = false }
        )
    }

    // Modal 5: Settings & Privacy
    if (showSettingsDialog) {
        SmsEngineSettingsModal(
            isOpen = true,
            isLiveReceiverEnabled = isLiveReceiverEnabled,
            onToggleLiveReceiver = { isLiveReceiverEnabled = it },
            hasPermission = hasSmsPermission,
            onDismiss = { showSettingsDialog = false },
            onClearHistory = onClearHistory
        )
    }

    // Modal 6: Engine Diagnostics & Error Logging
    if (showDiagnosticsDialog) {
        SmsDiagnosticsModal(
            isOpen = true,
            onDismiss = { showDiagnosticsDialog = false },
            isEngineActive = isEngineActive,
            hasPermission = hasSmsPermission,
            historyList = processingHistory
        )
    }

    // Dialog: Edit Bank Config
    editingBankConfig?.let { cfg ->
        BankEditDialog(
            config = cfg,
            onDismiss = { editingBankConfig = null },
            onSave = { bank, sender, req, customName ->
                onSaveBankConfig(bank, sender, req, customName)
                editingBankConfig = null
            }
        )
    }

    // Dialog: Delete Bank Confirmation
    bankToDelete?.let { bank ->
        AlertDialog(
            onDismissRequest = { bankToDelete = null },
            title = { Text("حذف اتصال ${bank.faName}", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Text(
                    "آیا از حذف اتصال این بانک اطمینان دارید؟ با حذف، سیاست‌های سرشماره و ارتباط آن با موتور پردازش پیامک لغو می‌شود.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteBankConfig(bank)
                        bankToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseDanger, contentColor = Color.White)
                ) {
                    Text("حذف اتصال")
                }
            },
            dismissButton = {
                TextButton(onClick = { bankToDelete = null }) {
                    Text("انصراف", color = TextMuted)
                }
            }
        )
    }

    // Detail Dialog for History items
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
private fun KpiMetricCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceCard)
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = PersianUtils.formatPersianNumber(value),
                color = color,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                color = TextSecondary,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EngineActionTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    testTag: String = "",
    badge: String? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCard)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (badge != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(tint.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(text = badge, color = tint, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = TextMuted,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
private fun ConnectedBankCard(
    config: BankUserConfig,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val brandColor = Color(config.bank.brandColorHex)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCard)
            .border(
                1.dp,
                if (config.isEnabled) CyanPrimary.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f),
                RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
            .testTag("connected_bank_${config.bank.name}")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(brandColor.copy(alpha = 0.2f))
                            .border(1.dp, brandColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = brandColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = config.customBankName ?: config.bank.faName,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            if (config.customBankName != null) {
                                Text(
                                    text = "(${config.bank.shortNameFa})",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (config.isEnabled) EmeraldAccent.copy(alpha = 0.15f) else AmberWarning.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (config.isEnabled) "فعال در پایش" else "متوقف‌شده",
                                    color = if (config.isEnabled) EmeraldAccent else AmberWarning,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (config.requireSenderMatch) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(CyanPrimary.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(10.dp))
                                        Text("قفل روی سرشماره", color = CyanPrimary, fontSize = 9.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Inline toggle switch
                Switch(
                    checked = config.isEnabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.testTag("toggle_bank_${config.bank.name}"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = CyanPrimary
                    )
                )
            }

            // Sender number & Action buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("سرشماره مجاز:", color = TextMuted, fontSize = 11.sp)
                    Text(
                        text = config.senderNumber.ifBlank { "عمومی (تمام سرشماره‌ها)" },
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp).testTag("edit_bank_${config.bank.name}")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "ویرایش تنظیمات بانک", tint = CyanPrimary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp).testTag("delete_bank_${config.bank.name}")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف اتصال بانک", tint = RoseDanger, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

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

/**
 * Modal 3: Persistent Processing History
 */
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
                // Header
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

                // Filter Tabs
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

                // History List
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

    // Detail Dialog
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
private fun HistoryRowCard(
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
                        Text(item.processingResult, color = statusColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Clear, contentDescription = "حذف رکورد", tint = TextMuted, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextSecondary, fontSize = 11.sp)
        Text(text = value, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * Modal 4: Pending Review Queue with field correction and currency recalculation
 */
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
            text = { Text("در حال حاضر هیچ پیامک مشکوک یا نیازمند بررسی در سامانه وجود ندارد.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("بسیار خوب", color = CyanPrimary) }
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

    // Live recalculated amount in Tomans
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
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("بررسی و تأیید تراکنش مبهم", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("رفع ابهام جهت و واحد پول قبل از درج در دفتر مالی", color = AmberWarning, fontSize = 11.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "بستن", tint = TextMuted)
                    }
                }

                // Reason for Review Banner
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
                            text = item.reviewReasonFa ?: "نوع یا واحد پول تراکنش نیاز به تأیید کاربر دارد",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                // 1. Transaction Direction Selector
                Text("۱. تعیین جهت تراکنش (واریز / برداشت / انتقال):", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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

                // 2. Currency Selector with recalculation preview
                Text("۲. واحد پول استخراج‌شده (تغییر واحد، مبلغ را مجدد محاسبه می‌کند):", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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

                // Live Amount Preview Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceCard)
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("مبلغ نهایی محاسبه‌شده برای ثبت:", color = TextSecondary, fontSize = 11.sp)
                        Text(
                            text = "${PersianUtils.formatPersianNumber(computedAmountToman)} تومان",
                            color = EmeraldAccent,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "عدد خام استخراج‌شده: ${PersianUtils.formatPersianNumber(item.rawExtractedAmount)}",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                // Optional Manual Adjustments
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
                    label = { Text("عنوان تراکنش (اختیاری)", fontSize = 11.sp) },
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

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismissItem,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text("رد کردن", color = TextMuted)
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
                        modifier = Modifier.weight(2f).height(44.dp).testTag("confirm_review_btn")
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

/**
 * Modal 5: Performance & Quality Reporting with Date Range Filters
 */
@Composable
fun SmsEngineReportModal(
    isOpen: Boolean,
    historyList: List<SmsProcessingHistoryEntity>,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    var selectedRangeIndex by remember { mutableIntStateOf(2) } // 0: Today, 1: 7D, 2: 30D, 3: All
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

    // Accurate calculation: exclude OTP/rejected from failure rate
    val financialCount = totalCount - rejectedCount
    val successRate = if (financialCount > 0) {
        ((autoCount + manualCount).toFloat() / financialCount * 100).toInt()
    } else {
        100
    }

    // Financial breakdown (Transfers strictly segregated!)
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
                // Header
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

                // Date Range Chips
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

                // Success Rate Banner
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

                // Volume Breakdown Cards
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

                // Financial Breakdown (TRANSFERS SEPARATED)
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

/**
 * Modal 6: Safe Test Bench (Simulates parser without writing to ledger)
 */
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
                // Header
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

                // Safety Assurance Banner
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

                // Inputs
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
                    modifier = Modifier.fillMaxWidth().height(44.dp).testTag("run_test_parse_btn")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("آزمایش و تحلیل پیامک", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                // Test Results Display
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

/**
 * Modal 7: Settings & Local Privacy
 */
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

    val context = LocalContext.current
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
                // Header
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

                // Toggle Live Receiver
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
                        Text("با دریافت هر پیامک بانکی، تراکنش ثبت و اعلان داده می‌شود.", color = TextMuted, fontSize = 10.sp)
                    }
                    Switch(
                        checked = isLiveReceiverEnabled,
                        onCheckedChange = onToggleLiveReceiver,
                        colors = SwitchDefaults.colors(checkedThumbColor = CyanPrimary)
                    )
                }

                // Privacy Guarantee Card
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
                            Text("• کلیه فرآیندهای تحلیل پیامک مستقیماً روی پردازنده گوشی شما اجرا می‌شود.", color = TextMuted, fontSize = 11.sp, lineHeight = 16.sp)
                            Text("• متن پیامک‌ها و ارقام حساب به هیچ هوش مصنوعی ابری یا سرور خارجی ارسال نمی‌گردد.", color = TextMuted, fontSize = 11.sp, lineHeight = 16.sp)
                        }
                    }
                }

                // Clear History Button
                OutlinedButton(
                    onClick = { showConfirmClearDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseDanger),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RoseDanger.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
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
            text = { Text("آیا مطمئن هستید؟ این عمل سوابق ممیزی پیامک‌ها را پاک می‌کند اما تراکنش‌های ثبت‌شده در دفتر مالی حفظ خواهند شد.", color = TextSecondary) },
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
