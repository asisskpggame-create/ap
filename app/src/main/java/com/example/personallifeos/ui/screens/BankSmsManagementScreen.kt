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
    var recentlyDeletedBank by remember { mutableStateOf<BankUserConfig?>(null) }
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

                // Instant Undo Banner for recently deleted bank connection
                recentlyDeletedBank?.let { deletedCfg ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AmberWarning.copy(alpha = 0.14f))
                            .border(1.dp, AmberWarning.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "اتصال «${deletedCfg.customBankName ?: deletedCfg.bank.faName}» حذف شد.",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = {
                                    onSaveBankConfig(
                                        deletedCfg.bank,
                                        deletedCfg.senderNumber,
                                        deletedCfg.requireSenderMatch,
                                        deletedCfg.customBankName
                                    )
                                    recentlyDeletedBank = null
                                }
                            ) {
                                Text("بازگردانی", color = CyanPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                            }
                            IconButton(
                                onClick = { recentlyDeletedBank = null },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "بستن", tint = TextMuted, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
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
                        recentlyDeletedBank = configuredBanks.find { it.bank == bank }
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
internal fun KpiMetricCard(
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

