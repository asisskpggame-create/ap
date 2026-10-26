package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import android.os.Build
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.example.notifications.ReminderScheduler
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.ConsumptionEntry
import com.example.data.model.CustomItem
import com.example.data.model.ExerciseRecord
import com.example.data.model.ExpenseEntry
import com.example.data.model.SleepRecord
import com.example.data.model.UserPreferencesDoc
import com.example.ui.components.AiMealAndReceiptScannerModalSheet
import com.example.ui.components.IranianBankSmsParser
import com.example.ui.components.MonthlySpendingTrendsLineChart
import com.example.ui.components.MonthlyVisualExpensesGlassCard
import com.example.ui.components.ReportCsvExporter
import com.example.ui.components.SmartBankSmsParserModalSheet
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ElectricTeal
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GlassCard
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.GlassmorphismDefaults
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LiquidGlassPill
import com.example.ui.theme.MutedViolet
import com.example.ui.theme.RoseError
import com.example.ui.theme.SkyInfo
import com.example.util.PersianDateAndUnitUtils
import com.example.util.WorldClassOsUtils

data class ParsedBankSms(
    val id: String,
    val bankName: String,
    val merchantTitle: String,
    val categoryFa: String,
    val amount: Double,
    val balanceInToman: Double?,
    val currency: String,
    val transactionType: String,
    val cardNumber: String,
    val rawBody: String,
    val timestampMillis: Long,
    val dateDisplay: String
)

private fun getCategoryIcon(cat: String): ImageVector {
    return when (cat) {
        "غذا و کافه" -> Icons.Default.Coffee
        "خرید" -> Icons.Default.ShoppingCart
        "قبوض و اجاره" -> Icons.Default.HomeWork
        "حمل‌ونقل" -> Icons.Default.DirectionsCar
        "سلامت و درمان" -> Icons.Default.MedicalServices
        "تفریح و سرگرمی" -> Icons.Default.SportsEsports
        "آموزش" -> Icons.Default.School
        else -> Icons.Default.Folder
    }
}

private fun readBankSmsInbox(context: Context): List<ParsedBankSms> {
    val results = mutableListOf<ParsedBankSms>()
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
        return emptyList()
    }

    try {
        val cursor: Cursor? = context.contentResolver.query(
            Uri.parse("content://sms/inbox"),
            arrayOf("_id", "address", "body", "date"),
            null,
            null,
            "date DESC"
        )

        cursor?.use { c ->
            val idIdx = c.getColumnIndex("_id")
            val bodyIdx = c.getColumnIndex("body")
            val dateIdx = c.getColumnIndex("date")

            var count = 0
            while (c.moveToNext() && count < 30) {
                val id = if (idIdx >= 0) c.getString(idIdx) else count.toString()
                val body = if (bodyIdx >= 0) c.getString(bodyIdx) ?: "" else ""
                val dateMillis = if (dateIdx >= 0) c.getLong(dateIdx) else System.currentTimeMillis()

                val lowerBody = body.lowercase()
                val isBankSms = body.contains("برداشت") || body.contains("واریز") ||
                        body.contains("خرید") || body.contains("مبلغ") ||
                        body.contains("مانده") || body.contains("تومان") || body.contains("ریال")

                if (isBankSms) {
                    val smartParsed = IranianBankSmsParser.parseBankSms(body)
                    val amountInToman = smartParsed?.amountInToman ?: 0.0
                    val txType = smartParsed?.transactionType ?: if (body.contains("واریز") || body.contains("بستانکار")) "INCOME" else "EXPENSE"
                    val bankName = smartParsed?.bankName ?: "پیامک بانکی"
                    val merchantTitle = smartParsed?.suggestedTitle ?: bankName
                    val categoryFa = smartParsed?.suggestedCategory ?: "سایر"

                    val cardRegex = Regex("""(?:\*+|\b)(\d{4})\b""")
                    val cardMatch = cardRegex.find(body)
                    val cardNum = if (cardMatch != null) "کارت " + cardMatch.groupValues[1] else ""

                    if (amountInToman > 0) {
                        results.add(
                            ParsedBankSms(
                                id = id,
                                bankName = bankName,
                                merchantTitle = merchantTitle,
                                categoryFa = categoryFa,
                                amount = amountInToman,
                                balanceInToman = smartParsed?.balanceInToman,
                                currency = "TOMAN",
                                transactionType = txType,
                                cardNumber = cardNum,
                                rawBody = body,
                                timestampMillis = dateMillis,
                                dateDisplay = PersianDateAndUnitUtils.formatEpochDay(dateMillis / 86400000L, "JALALI")
                            )
                        )
                        count++
                    }
                }
            }
        }
    } catch (_: Exception) {
    }

    return results
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DailyLogAndHealthScreen(
    prefs: UserPreferencesDoc,
    consumption: List<ConsumptionEntry>,
    customItems: List<CustomItem>,
    expenses: List<ExpenseEntry>,
    sleepRecords: List<SleepRecord>,
    exerciseRecords: List<ExerciseRecord>,
    onQuickAddConsumption: (itemName: String, category: String, quantity: Double, unit: String, notes: String, optionalPrice: Double, currency: String) -> Unit,
    onDeleteConsumption: (ConsumptionEntry) -> Unit,
    onCreateCustomItem: (name: String, category: String, unit: String, defaultQty: Double, dailyTarget: Double, defaultPrice: Double) -> Unit,
    onUpdateCustomItem: (CustomItem) -> Unit = {},
    onDeleteCustomItem: (CustomItem) -> Unit = {},
    onAddExpense: (title: String, category: String, amount: Double, currency: String, notes: String, transactionType: String) -> Unit,
    onSettleDebtOrReceivable: (ExpenseEntry) -> Unit = {},
    onDeleteExpense: (ExpenseEntry) -> Unit,
    onAddSleep: (durationMinutes: Int, qualityRating: Int, notes: String) -> Unit,
    onAddExercise: (activityType: String, durationMinutes: Int, intensity: String, distanceMeters: Double, repetitions: Int, notes: String) -> Unit,
    onUpdatePrefs: (UserPreferencesDoc) -> Unit = {}
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    val todayEpochDay = PersianDateAndUnitUtils.currentLocalEpochDay()

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            TabRow(
                selectedTabIndex = selectedSubTab,
                containerColor = Color.Transparent,
                contentColor = ElectricTeal,
                divider = {},
                indicator = { tabPositions ->
                    if (selectedSubTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedSubTab]),
                            color = ElectricTeal
                        )
                    }
                }
            ) {
                Tab(
                    selected = selectedSubTab == 0,
                    onClick = { selectedSubTab = 0 },
                    selectedContentColor = ElectricTeal,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(
                                text = "ورودی و خروجی‌ها",
                                fontWeight = if (selectedSubTab == 0) FontWeight.ExtraBold else FontWeight.Medium
                            )
                        }
                    },
                    modifier = Modifier.testTag("tab_financial_inout")
                )
                Tab(
                    selected = selectedSubTab == 1,
                    onClick = { selectedSubTab = 1 },
                    selectedContentColor = ElectricTeal,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(
                                text = "طلب و بدهی",
                                fontWeight = if (selectedSubTab == 1) FontWeight.ExtraBold else FontWeight.Medium
                            )
                        }
                    },
                    modifier = Modifier.testTag("tab_debt_receivable")
                )
            }
        }

        when (selectedSubTab) {
            0 -> ExpenseTrackerSubScreen(
                prefs = prefs,
                todayEpochDay = todayEpochDay,
                expenses = expenses,
                customItems = customItems,
                consumption = consumption,
                onAddExpense = onAddExpense,
                onQuickAddConsumption = onQuickAddConsumption,
                onCreateCustomItem = onCreateCustomItem,
                onUpdateCustomItem = onUpdateCustomItem,
                onDeleteCustomItem = onDeleteCustomItem,
                onSettleDebtOrReceivable = onSettleDebtOrReceivable,
                onDeleteExpense = onDeleteExpense,
                onUpdatePrefs = onUpdatePrefs,
                initialFilter = "ALL"
            )
            1 -> ExpenseTrackerSubScreen(
                prefs = prefs,
                todayEpochDay = todayEpochDay,
                expenses = expenses,
                customItems = customItems,
                consumption = consumption,
                onAddExpense = onAddExpense,
                onQuickAddConsumption = onQuickAddConsumption,
                onCreateCustomItem = onCreateCustomItem,
                onUpdateCustomItem = onUpdateCustomItem,
                onDeleteCustomItem = onDeleteCustomItem,
                onSettleDebtOrReceivable = onSettleDebtOrReceivable,
                onDeleteExpense = onDeleteExpense,
                onUpdatePrefs = onUpdatePrefs,
                initialFilter = "IOU"
            )
        }
    }
}

private enum class FinancialPeriodFilter(val labelFa: String, val daysCount: Int) {
    DAILY("امروز", 1),
    WEEKLY("۷ روز", 7),
    MONTHLY("۳۰ روز", 30),
    YEARLY("امسال", 365),
    ALL("همه", 36500)
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseTrackerSubScreen(
    prefs: UserPreferencesDoc,
    todayEpochDay: Long,
    expenses: List<ExpenseEntry>,
    customItems: List<CustomItem> = emptyList(),
    consumption: List<ConsumptionEntry> = emptyList(),
    onAddExpense: (title: String, category: String, amount: Double, currency: String, notes: String, transactionType: String) -> Unit,
    onQuickAddConsumption: (itemName: String, category: String, quantity: Double, unit: String, notes: String, optionalPrice: Double, currency: String) -> Unit = { _, _, _, _, _, _, _ -> },
    onCreateCustomItem: (name: String, category: String, unit: String, defaultQty: Double, dailyTarget: Double, defaultPrice: Double) -> Unit = { _, _, _, _, _, _ -> },
    onUpdateCustomItem: (CustomItem) -> Unit = {},
    onDeleteCustomItem: (CustomItem) -> Unit = {},
    onSettleDebtOrReceivable: (ExpenseEntry) -> Unit = {},
    onDeleteExpense: (ExpenseEntry) -> Unit,
    onUpdatePrefs: (UserPreferencesDoc) -> Unit = {},
    initialFilter: String = "ALL"
) {
    val context = LocalContext.current
    var isVaultUnlocked by remember(prefs.vaultLockEnabled, prefs.vaultPinCode) {
        mutableStateOf(!prefs.vaultLockEnabled || prefs.vaultPinCode.isBlank())
    }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    if (!isVaultUnlocked) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                accentBorderColor = ElectricTeal.copy(alpha = 0.65f)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = ElectricTeal, modifier = Modifier.size(36.dp))
                    Text(
                        text = "قفل حریم خصوصی بخش مالی",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "برای مشاهده تراکنش‌ها و تراز مالی، پین‌کد ۴ رقمی خود را وارد کنید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = {
                            enteredPin = PersianDateAndUnitUtils.toEnglishDigits(it)
                            pinError = false
                            if (enteredPin == prefs.vaultPinCode) {
                                WorldClassOsUtils.HapticSignature.crispTick(context)
                                isVaultUnlocked = true
                            }
                        },
                        isError = pinError,
                        label = { Text("پین‌کد امنیتی") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("vault_pin_input")
                    )
                    Button(
                        onClick = {
                            if (enteredPin == prefs.vaultPinCode) {
                                WorldClassOsUtils.HapticSignature.crispTick(context)
                                isVaultUnlocked = true
                            } else {
                                WorldClassOsUtils.HapticSignature.heavyImpact(context)
                                pinError = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("بازگشایی قفل مالی")
                    }
                }
            }
        }
        return
    }

    var transactionType by remember { mutableStateOf("EXPENSE") }
    var title by remember { mutableStateOf("") }
    var showTitleError by remember { mutableStateOf(false) }
    var showAmountError by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf("غذا و کافه") }
    var amountText by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(prefs.defaultCurrency) }
    var notes by remember { mutableStateOf("") }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showBankSmsSheetInExpenses by remember { mutableStateOf(false) }
    var showReceiptScannerInExpenses by remember { mutableStateOf(false) }
    var editingPresetInExpenses by remember { mutableStateOf<CustomItem?>(null) }
    var showNewPresetDialogInExpenses by remember { mutableStateOf(false) }
    var showManagePresetsSheetInExpenses by remember { mutableStateOf(false) }

    // State for Search, Edit Transaction & Text Report Export
    var searchQuery by remember { mutableStateOf("") }
    var editingTransaction by remember { mutableStateOf<ExpenseEntry?>(null) }
    var showExportReportDialog by remember { mutableStateOf(false) }

    // State for Custom Expense Categories Management
    var customExpenseCategories by remember {
        mutableStateOf(
            listOf("غذا و کافه", "خرید", "قبوض و اجاره", "حمل‌ونقل", "سلامت و درمان", "تفریح و سرگرمی", "سرمایه‌گذاری", "آموزش", "سایر")
        )
    }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryInputName by remember { mutableStateOf("") }

    // State for Bank SMS Auto-Inbox Reader Feed
    var bankSmsList by remember { mutableStateOf<List<ParsedBankSms>>(emptyList()) }
    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasSmsPermission = isGranted
        if (isGranted) {
            WorldClassOsUtils.HapticSignature.doublePulse(context)
            bankSmsList = readBankSmsInbox(context)
        }
    }

    LaunchedEffect(hasSmsPermission) {
        if (hasSmsPermission) {
            bankSmsList = readBankSmsInbox(context)
        }
    }

    var budgetInputText by remember(prefs.monthlyBudgetTarget) {
        mutableStateOf(prefs.monthlyBudgetTarget.toLong().toString())
    }

    var selectedPeriod by remember { mutableStateOf(FinancialPeriodFilter.MONTHLY) }
    var selectedTypeFilter by remember(initialFilter) {
        mutableStateOf(if (initialFilter == "IOU") "RECEIVABLE" else "ALL")
    }

    val safeToSpend = remember(prefs.monthlyBudgetTarget, prefs.defaultCurrency, todayEpochDay, expenses) {
        WorldClassOsUtils.calculateDailySafeToSpend(
            monthlyBudget = prefs.monthlyBudgetTarget,
            defaultCurrency = prefs.defaultCurrency,
            todayEpochDay = todayEpochDay,
            expenses = expenses
        )
    }

    if (showBankSmsSheetInExpenses) {
        SmartBankSmsParserModalSheet(
            defaultCurrency = prefs.defaultCurrency,
            onDismiss = { showBankSmsSheetInExpenses = false },
            onConfirmSaveExpense = { titleSms, catSms, amountToman, notesSms, txTypeSms ->
                onAddExpense(titleSms, catSms, amountToman, "TOMAN", notesSms, txTypeSms)
                showBankSmsSheetInExpenses = false
            }
        )
    }

    if (showReceiptScannerInExpenses) {
        AiMealAndReceiptScannerModalSheet(
            onDismiss = { showReceiptScannerInExpenses = false },
            onLogScannedMeal = { _, _, _ -> },
            onLogScannedReceipt = { titleRec, catRec, amountToman, notesRec ->
                onAddExpense(titleRec, catRec, amountToman, "TOMAN", notesRec, "EXPENSE")
                showReceiptScannerInExpenses = false
            }
        )
    }

    var showDailyReminderDialog by remember { mutableStateOf(false) }
    var reminderEnabledDraft by remember(prefs.dailyExpenseReminderEnabled) {
        mutableStateOf(prefs.dailyExpenseReminderEnabled)
    }
    var reminderHourDraft by remember(prefs.dailyExpenseReminderHour) {
        mutableStateOf(prefs.dailyExpenseReminderHour.toString().padStart(2, '0'))
    }
    var reminderMinuteDraft by remember(prefs.dailyExpenseReminderMinute) {
        mutableStateOf(prefs.dailyExpenseReminderMinute.toString().padStart(2, '0'))
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            WorldClassOsUtils.HapticSignature.doublePulse(context)
            ReminderScheduler.scheduleDailyExpenseReminder(
                context = context,
                enabled = prefs.dailyExpenseReminderEnabled,
                hourOfDay = prefs.dailyExpenseReminderHour,
                minute = prefs.dailyExpenseReminderMinute
            )
        }
    }

    if (showDailyReminderDialog) {
        AlertDialog(
            onDismissRequest = { showDailyReminderDialog = false },
            shape = RoundedCornerShape(26.dp),
            containerColor = Color(0xFF0F1612).copy(alpha = 0.96f),
            titleContentColor = ElectricTeal,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = AmberWarning)
                    Text("یادآور روزانه ثبت هزینه‌ها", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ارسال نوتیفیکیشن روزانه", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Switch(
                            checked = reminderEnabledDraft,
                            onCheckedChange = { reminderEnabledDraft = it },
                            modifier = Modifier.testTag("daily_expense_reminder_switch")
                        )
                    }

                    Text(
                        text = "انتخاب سریع ساعت یادآوری:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            20 to "۲۰:۰۰",
                            21 to "۲۱:۰۰",
                            22 to "۲۲:۰۰",
                            23 to "۲۳:۰۰"
                        ).forEach { (presetHour, label) ->
                            val isSelected = reminderHourDraft.toIntOrNull() == presetHour && (reminderMinuteDraft.toIntOrNull() ?: 0) == 0
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    reminderHourDraft = presetHour.toString().padStart(2, '0')
                                    reminderMinuteDraft = "00"
                                },
                                label = { Text(label) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = reminderHourDraft,
                            onValueChange = { reminderHourDraft = PersianDateAndUnitUtils.toEnglishDigits(it).take(2) },
                            label = { Text("ساعت (۰ تا ۲۳)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reminder_hour_input")
                        )
                        OutlinedTextField(
                            value = reminderMinuteDraft,
                            onValueChange = { reminderMinuteDraft = PersianDateAndUnitUtils.toEnglishDigits(it).take(2) },
                            label = { Text("دقیقه (۰ تا ۵۹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reminder_minute_input")
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                            ) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                WorldClassOsUtils.HapticSignature.doublePulse(context)
                                ReminderScheduler.triggerExpenseReminderNow(context)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_expense_reminder_button")
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تست فوری نوتیفیکیشن")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val safeH = (reminderHourDraft.toIntOrNull() ?: 21).coerceIn(0, 23)
                        val safeM = (reminderMinuteDraft.toIntOrNull() ?: 0).coerceIn(0, 59)
                        if (reminderEnabledDraft && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        onUpdatePrefs(
                            prefs.copy(
                                dailyExpenseReminderEnabled = reminderEnabledDraft,
                                dailyExpenseReminderHour = safeH,
                                dailyExpenseReminderMinute = safeM,
                                notificationsEnabled = if (reminderEnabledDraft) true else prefs.notificationsEnabled
                            )
                        )
                        ReminderScheduler.scheduleDailyExpenseReminder(
                            context = context,
                            enabled = reminderEnabledDraft,
                            hourOfDay = safeH,
                            minute = safeM
                        )
                        WorldClassOsUtils.HapticSignature.crispTick(context)
                        showDailyReminderDialog = false
                    },
                    modifier = Modifier.testTag("save_daily_reminder_button")
                ) {
                    Text("ذخیره زمان‌بندی")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDailyReminderDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    if (showBudgetDialog) {
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            shape = RoundedCornerShape(26.dp),
            containerColor = Color(0xFF0F1612).copy(alpha = 0.94f),
            titleContentColor = ElectricTeal,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("تنظیم سقف بودجه ماهانه (${prefs.defaultCurrency})") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "برنامه بر اساس روزهای باقی‌مانده از ماه شمسی، سهم مجاز خرج روزانه شما را زنده محاسبه می‌کند.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = budgetInputText,
                        onValueChange = { budgetInputText = PersianDateAndUnitUtils.toEnglishDigits(it) },
                        label = { Text("سقف بودجه ماهانه (${prefs.defaultCurrency})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newBudget = budgetInputText.toDoubleOrNull()?.coerceAtLeast(100_000.0) ?: prefs.monthlyBudgetTarget
                        onUpdatePrefs(prefs.copy(monthlyBudgetTarget = newBudget))
                        showBudgetDialog = false
                    }
                ) {
                    Text("ذخیره بودجه")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    // Modal Dialog to Edit Existing Transaction
    if (editingTransaction != null) {
        val tx = editingTransaction!!
        var editTitle by remember(tx) { mutableStateOf(tx.title) }
        var editAmountText by remember(tx) { mutableStateOf(tx.amount.toLong().toString()) }
        var editCategory by remember(tx) { mutableStateOf(tx.category) }
        var editNotes by remember(tx) { mutableStateOf(tx.cleanNotes) }

        AlertDialog(
            onDismissRequest = { editingTransaction = null },
            shape = RoundedCornerShape(26.dp),
            containerColor = Color(0xFF0F1612).copy(alpha = 0.94f),
            titleContentColor = ElectricTeal,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("ویرایش تراکنش مالی", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("عنوان تراکنش") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editAmountText,
                        onValueChange = { editAmountText = PersianDateAndUnitUtils.toEnglishDigits(it) },
                        label = { Text("مبلغ (${tx.currency})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("دسته‌بندی:", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        customExpenseCategories.forEach { catLbl ->
                            FilterChip(
                                selected = editCategory == catLbl,
                                onClick = { editCategory = catLbl },
                                label = { Text(catLbl) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        label = { Text("توضیحات (اختیاری)") },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newTitle = editTitle.trim()
                        val newAmt = editAmountText.toDoubleOrNull() ?: tx.amount
                        if (newTitle.isNotBlank() && newAmt > 0) {
                            onDeleteExpense(tx)
                            onAddExpense(
                                newTitle,
                                editCategory,
                                newAmt,
                                tx.currency,
                                editNotes,
                                tx.transactionType
                            )
                            WorldClassOsUtils.HapticSignature.doublePulse(context)
                            editingTransaction = null
                        }
                    }
                ) {
                    Text("ذخیره تغییرات")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingTransaction = null }) {
                    Text("انصراف")
                }
            }
        )
    }

    // Modal Dialog to Add New Expense Category
    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            shape = RoundedCornerShape(26.dp),
            containerColor = Color(0xFF0F1612).copy(alpha = 0.94f),
            titleContentColor = ElectricTeal,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("افزودن دسته‌بندی جدید مالی", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "عنوان دسته‌بندی جدید مانند «مسافرت»، «تعمیرات خودرو»، «هدیه» یا «پزشکی» را وارد کنید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newCategoryInputName,
                        onValueChange = { newCategoryInputName = it },
                        label = { Text("نام دسته‌بندی جدید") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_category_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanCat = newCategoryInputName.trim()
                        if (cleanCat.isNotBlank() && !customExpenseCategories.contains(cleanCat)) {
                            customExpenseCategories = customExpenseCategories + cleanCat
                            category = cleanCat
                            newCategoryInputName = ""
                            WorldClassOsUtils.HapticSignature.doublePulse(context)
                        }
                        showAddCategoryDialog = false
                    },
                    modifier = Modifier.testTag("save_new_category_button")
                ) {
                    Text("افزودن دسته‌بندی")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    // Manage Presets ModalBottomSheet
    if (showManagePresetsSheetInExpenses) {
        ModalBottomSheet(
            onDismissRequest = { showManagePresetsSheetInExpenses = false },
            containerColor = Color(0xFF0F1612).copy(alpha = 0.94f),
            scrimColor = Color.Black.copy(alpha = 0.65f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "مدیریت دکمه‌های ثبت سریع مالی",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedButton(
                        onClick = {
                            showManagePresetsSheetInExpenses = false
                            showNewPresetDialogInExpenses = true
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("دکمه جدید")
                    }
                }

                customItems.forEach { preset ->
                    GlassCard(
                        shape = RoundedCornerShape(14.dp),
                        baseColor = Color(0xFF141F1A),
                        alpha = 0.7f,
                        accentBorderColor = ElectricTeal,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = preset.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (preset.defaultPrice > 0.0) {
                                        PersianDateAndUnitUtils.formatCurrencyAmount(preset.defaultPrice, preset.defaultCurrency)
                                    } else {
                                        "${PersianDateAndUnitUtils.toPersianDigits(preset.defaultQuantity.toInt().toString())} ${preset.unit}"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = {
                                        showManagePresetsSheetInExpenses = false
                                        editingPresetInExpenses = preset
                                    },
                                    modifier = Modifier.testTag("edit_expense_preset_${preset.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "ویرایش ${preset.name}",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteCustomItem(preset) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "حذف ${preset.name}",
                                        tint = RoseError
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Edit / Create Preset Modal Dialog
    if (editingPresetInExpenses != null || showNewPresetDialogInExpenses) {
        val currentEditing = editingPresetInExpenses
        var dialogName by remember(currentEditing, showNewPresetDialogInExpenses) {
            mutableStateOf(currentEditing?.name ?: "")
        }
        var dialogPriceText by remember(currentEditing, showNewPresetDialogInExpenses) {
            mutableStateOf(
                if (currentEditing != null && currentEditing.defaultPrice > 0.0) {
                    currentEditing.defaultPrice.toLong().toString()
                } else ""
            )
        }
        var dialogQtyText by remember(currentEditing, showNewPresetDialogInExpenses) {
            mutableStateOf(currentEditing?.defaultQuantity?.toInt()?.toString() ?: "1")
        }
        var dialogUnit by remember(currentEditing, showNewPresetDialogInExpenses) {
            mutableStateOf(currentEditing?.unit ?: "pieces")
        }
        var dialogCategory by remember(currentEditing, showNewPresetDialogInExpenses) {
            mutableStateOf(currentEditing?.category ?: "غذا و کافه")
        }

        AlertDialog(
            onDismissRequest = {
                editingPresetInExpenses = null
                showNewPresetDialogInExpenses = false
            },
            shape = RoundedCornerShape(26.dp),
            containerColor = Color(0xFF0F1612).copy(alpha = 0.94f),
            titleContentColor = ElectricTeal,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = {
                Text(
                    text = if (currentEditing != null) "ویرایش نام و قیمت دکمه سریع" else "افزودن دکمه ثبت سریع جدید",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = dialogName,
                        onValueChange = { dialogName = it },
                        label = { Text("نام دکمه (مثلاً: قهوه، مترو، اسنپ، سیگار)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("expense_preset_name_input")
                    )
                    OutlinedTextField(
                        value = dialogPriceText,
                        onValueChange = { dialogPriceText = PersianDateAndUnitUtils.toEnglishDigits(it) },
                        label = { Text("قیمت هر بار ثبت (${prefs.defaultCurrency})") },
                        placeholder = { Text("مثلاً 90000 یا 150000") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("expense_preset_price_input")
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = {
                                val num = dialogPriceText.toDoubleOrNull() ?: 0.0
                                if (num > 0 && num < 10000) {
                                    dialogPriceText = (num * 1000).toLong().toString()
                                }
                            },
                            label = { Text("×۱,۰۰۰") }
                        )
                        AssistChip(
                            onClick = { dialogPriceText = "90000" },
                            label = { Text("۹۰ تومن") }
                        )
                        AssistChip(
                            onClick = { dialogPriceText = "150000" },
                            label = { Text("۱۵۰ تومن") }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = dialogQtyText,
                            onValueChange = { dialogQtyText = PersianDateAndUnitUtils.toEnglishDigits(it) },
                            label = { Text("تعداد پیش‌فرض") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = dialogUnit,
                            onValueChange = { dialogUnit = it },
                            label = { Text("واحد (cups/pieces)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        customExpenseCategories.take(4).forEach { catLbl ->
                            FilterChip(
                                selected = dialogCategory == catLbl,
                                onClick = { dialogCategory = catLbl },
                                label = { Text(catLbl) }
                            )
                        }
                    }
                    val rawPresetNum = dialogPriceText.toDoubleOrNull() ?: 0.0
                    if (rawPresetNum in 1.0..999.0 && prefs.defaultCurrency.equals("TOMAN", ignoreCase = true)) {
                        Text(
                            text = "معادل: ${PersianDateAndUnitUtils.formatCurrencyAmount(rawPresetNum * 1000.0, "TOMAN")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricTeal,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanName = dialogName.trim()
                        val rawPrice = dialogPriceText.toDoubleOrNull() ?: 0.0
                        val price = PersianDateAndUnitUtils.normalizeColloquialTomanAmount(rawPrice, prefs.defaultCurrency)
                        val qty = dialogQtyText.toDoubleOrNull()?.coerceAtLeast(0.1) ?: 1.0
                        if (cleanName.isNotBlank()) {
                            if (currentEditing != null) {
                                onUpdateCustomItem(
                                    currentEditing.copy(
                                        name = cleanName,
                                        category = dialogCategory,
                                        unit = dialogUnit.ifBlank { "pieces" },
                                        defaultQuantity = qty,
                                        defaultPrice = price,
                                        defaultCurrency = prefs.defaultCurrency
                                    )
                                )
                            } else {
                                onCreateCustomItem(
                                    cleanName,
                                    dialogCategory,
                                    dialogUnit.ifBlank { "pieces" },
                                    qty,
                                    0.0,
                                    price
                                )
                            }
                            editingPresetInExpenses = null
                            showNewPresetDialogInExpenses = false
                        }
                    },
                    modifier = Modifier.testTag("save_expense_preset_button")
                ) {
                    Text("ذخیره دکمه")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (currentEditing != null) {
                        TextButton(
                            onClick = {
                                onDeleteCustomItem(currentEditing)
                                editingPresetInExpenses = null
                            }
                        ) {
                            Text("حذف دکمه", color = RoseError)
                        }
                    }
                    TextButton(
                        onClick = {
                            editingPresetInExpenses = null
                            showNewPresetDialogInExpenses = false
                        }
                    ) {
                        Text("انصراف")
                    }
                }
            }
        )
    }

    val dailyEntries = remember(expenses, todayEpochDay) {
        expenses.filter { it.localEpochDay == todayEpochDay }
    }
    val weeklyEntries = remember(expenses, todayEpochDay) {
        expenses.filter { it.localEpochDay in (todayEpochDay - 6)..todayEpochDay }
    }
    val monthlyEntries = remember(expenses, todayEpochDay) {
        expenses.filter { it.localEpochDay in (todayEpochDay - 29)..todayEpochDay }
    }
    val yearlyEntries = remember(expenses, todayEpochDay) {
        expenses.filter { it.localEpochDay in (todayEpochDay - 364)..todayEpochDay }
    }

    val periodEntries = remember(expenses, todayEpochDay, selectedPeriod) {
        when (selectedPeriod) {
            FinancialPeriodFilter.DAILY -> dailyEntries
            FinancialPeriodFilter.WEEKLY -> weeklyEntries
            FinancialPeriodFilter.MONTHLY -> monthlyEntries
            FinancialPeriodFilter.YEARLY -> yearlyEntries
            FinancialPeriodFilter.ALL -> expenses
        }
    }

    val filteredTransactions = remember(periodEntries, selectedTypeFilter, searchQuery) {
        periodEntries.filter { entry ->
            val matchesType = when (selectedTypeFilter) {
                "EXPENSE" -> entry.isExpense
                "INCOME" -> entry.isIncome
                "RECEIVABLE" -> entry.isReceivable
                "DEBT" -> entry.isDebt
                else -> true
            }
            val q = searchQuery.trim().lowercase()
            val matchesQuery = q.isEmpty() ||
                    entry.title.lowercase().contains(q) ||
                    entry.category.lowercase().contains(q) ||
                    entry.cleanNotes.lowercase().contains(q) ||
                    entry.amount.toLong().toString().contains(q)

            matchesType && matchesQuery
        }
    }

    val periodExpenseByCurr = remember(periodEntries) {
        PersianDateAndUnitUtils.aggregateExpensesByCurrency(periodEntries)
    }
    val periodIncomeByCurr = remember(periodEntries) {
        PersianDateAndUnitUtils.aggregateIncomesByCurrency(periodEntries)
    }
    val periodNetByCurr = remember(periodEntries) {
        PersianDateAndUnitUtils.aggregateNetBalanceByCurrency(periodEntries)
    }

    fun formatCurrMap(map: Map<String, Double>): String {
        if (map.isEmpty()) return "۰ تومان"
        return map.entries.joinToString(" | ") {
            PersianDateAndUnitUtils.formatCurrencyAmount(it.value, it.key)
        }
    }

    // Modal Dialog to Export Text Report of Transactions
    if (showExportReportDialog) {
        val clipboardManager = LocalClipboardManager.current
        val reportText = buildString {
            appendLine("📊 گزارش تراکنش‌های مالی - Personal Life OS")
            appendLine("بازه زمانی: ${selectedPeriod.labelFa}")
            appendLine("تعداد تراکنش‌ها: ${PersianDateAndUnitUtils.toPersianDigits(filteredTransactions.size.toString())}")
            appendLine("----------------------------------------")
            appendLine("مجموع خرج: ${formatCurrMap(periodExpenseByCurr)}")
            appendLine("مجموع درآمد: ${formatCurrMap(periodIncomeByCurr)}")
            appendLine("تراز خالص: ${formatCurrMap(periodNetByCurr)}")
            appendLine("----------------------------------------")
            appendLine("لیست تراکنش‌ها:")
            filteredTransactions.forEachIndexed { i, t ->
                val typeStr = when {
                    t.isIncome -> "درآمد"
                    t.isReceivable -> "طلبکاری"
                    t.isDebt -> "بدهکاری"
                    else -> "خرج"
                }
                appendLine("${PersianDateAndUnitUtils.toPersianDigits((i + 1).toString())}. [${typeStr}] ${t.title} - ${PersianDateAndUnitUtils.formatCurrencyAmount(t.amount, t.currency)} (${t.category})")
            }
        }

        AlertDialog(
            onDismissRequest = { showExportReportDialog = false },
            shape = RoundedCornerShape(26.dp),
            containerColor = Color(0xFF0F1612).copy(alpha = 0.94f),
            titleContentColor = ElectricTeal,
            textContentColor = MaterialTheme.colorScheme.onSurface,
            title = { Text("گزارش متنی تراکنش‌ها", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "می‌توانید خلاصه و گزارش کامل این بازه را کپی کرده و ارسال نمایید:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF141F1A),
                        border = BorderStroke(1.dp, ElectricTeal.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        Text(
                            text = reportText,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(reportText))
                        WorldClassOsUtils.HapticSignature.doublePulse(context)
                        showExportReportDialog = false
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("کپی گزارش در حافظه")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportReportDialog = false }) {
                    Text("بستن")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 0.a Quick Smart Bank SMS, Receipt Scanner & Daily Expense Reminder Bar
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showBankSmsSheetInExpenses = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("expense_tab_bank_sms_button")
                    ) {
                        Icon(Icons.Default.Sms, contentDescription = null, tint = ElectricTeal, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("متن پیامک بانک", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { showReceiptScannerInExpenses = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("expense_tab_receipt_scanner_button")
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = MutedViolet, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("اسکن رسید خرید", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }

                // Scheduled Daily Expense Reminder Pill Bar
                val formattedReminderTime = remember(prefs.dailyExpenseReminderHour, prefs.dailyExpenseReminderMinute) {
                    val hStr = prefs.dailyExpenseReminderHour.toString().padStart(2, '0')
                    val mStr = prefs.dailyExpenseReminderMinute.toString().padStart(2, '0')
                    PersianDateAndUnitUtils.toPersianDigits("$hStr:$mStr")
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                    border = BorderStroke(
                        1.dp,
                        if (prefs.dailyExpenseReminderEnabled) AmberWarning.copy(alpha = 0.45f)
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDailyReminderDialog = true }
                        .testTag("daily_expense_reminder_bar")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = if (prefs.dailyExpenseReminderEnabled) AmberWarning else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "یادآور روزانه ثبت هزینه‌ها",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (prefs.dailyExpenseReminderEnabled) "فعال • هر شب ساعت $formattedReminderTime" else "غیرفعال • برای تنظیم ضربه بزنید",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (prefs.dailyExpenseReminderEnabled) AmberWarning else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        TextButton(
                            onClick = { showDailyReminderDialog = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "تنظیم ساعت",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = ElectricTeal
                            )
                        }
                    }
                }
            }
        }

        // 0.a2 Live Auto Bank SMS Reader Feed Card
        item {
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bank_sms_inbox_feed_card"),
                shape = RoundedCornerShape(20.dp),
                accentBorderColor = ElectricTeal.copy(alpha = 0.5f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Inbox, contentDescription = null, tint = ElectricTeal, modifier = Modifier.size(20.dp))
                            Text(
                                text = "پیامک‌های بانکی",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = {
                                if (hasSmsPermission) {
                                    bankSmsList = readBankSmsInbox(context)
                                    WorldClassOsUtils.HapticSignature.crispTick(context)
                                } else {
                                    smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
                                }
                            }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "بروزرسانی پیامک‌ها", tint = ElectricTeal)
                        }
                    }

                    if (!hasSmsPermission) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "تشخیص خودکار واریز، برداشت و مانده حساب از پیامک‌های بانکی",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = { smsPermissionLauncher.launch(Manifest.permission.READ_SMS) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("grant_sms_permission_button")
                                ) {
                                    Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("اتصال به پیامک‌های بانکی")
                                }
                            }
                        }
                    } else if (bankSmsList.isEmpty()) {
                        Text(
                            text = "پیامک بانکی جدیدی یافت نشد.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            bankSmsList.take(4).forEach { sms ->
                                val isExp = sms.transactionType == "EXPENSE"
                                val badgeColor = if (isExp) RoseError else EmeraldSuccess
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = badgeColor.copy(alpha = 0.18f)
                                                ) {
                                                    Text(
                                                        text = if (isExp) "برداشت" else "واریز",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = badgeColor,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Text(
                                                    text = sms.bankName + if (sms.cardNumber.isNotBlank()) " (${sms.cardNumber})" else "",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Text(
                                                text = PersianDateAndUnitUtils.formatCurrencyAmount(sms.amount, "TOMAN"),
                                                style = MaterialTheme.typography.titleMedium,
                                                color = badgeColor,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            if (sms.balanceInToman != null && sms.balanceInToman > 0) {
                                                Text(
                                                    text = "مانده: ${PersianDateAndUnitUtils.formatCurrencyAmount(sms.balanceInToman, "TOMAN")}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = ElectricTeal,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            Text(
                                                text = "${sms.dateDisplay} • ${sms.categoryFa}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                WorldClassOsUtils.HapticSignature.doublePulse(context)
                                                onAddExpense(
                                                    sms.merchantTitle.ifBlank { "${sms.bankName} ${sms.cardNumber}".trim() },
                                                    sms.categoryFa,
                                                    sms.amount,
                                                    "TOMAN",
                                                    sms.rawBody,
                                                    sms.transactionType
                                                )
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = badgeColor,
                                                contentColor = Color.White
                                            ),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("ثبت", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 0.a3 1-Tap Quick Expense Presets Bar
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                accentBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ثبت سریع مخارج",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                border = BorderStroke(1.dp, GlassmorphismDefaults.specularBorderBrush(topIntensity = 0.35f)),
                                modifier = Modifier
                                    .clickable { showManagePresetsSheetInExpenses = true }
                                    .testTag("open_manage_expense_presets_sheet_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "مدیریت دکمه‌ها",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "مدیریت",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = { showNewPresetDialogInExpenses = true },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, GlassmorphismDefaults.specularBorderBrush(MaterialTheme.colorScheme.primary, topIntensity = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("add_new_expense_preset_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("دکمه جدید", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    if (customItems.isEmpty()) {
                        Text(
                            text = "مخارج پرتکرار روزانه را برای ثبت ۱-لمسی اضافه کنید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            customItems.chunked(2).forEach { rowPresets ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    rowPresets.forEach { preset ->
                                        val todayCountForPreset = dailyEntries.count {
                                            it.title.equals(preset.name, ignoreCase = true)
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(18.dp),
                                            color = if (todayCountForPreset > 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
                                            border = BorderStroke(
                                                1.dp,
                                                GlassmorphismDefaults.specularBorderBrush(
                                                    accentColor = if (todayCountForPreset > 0) MaterialTheme.colorScheme.primary else null,
                                                    topIntensity = if (todayCountForPreset > 0) 0.65f else 0.35f
                                                )
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(18.dp))
                                                .combinedClickable(
                                                    onClick = {
                                                        WorldClassOsUtils.HapticSignature.doublePulse(context)
                                                        if (preset.defaultPrice > 0.0) {
                                                            onAddExpense(
                                                                preset.name,
                                                                preset.category,
                                                                preset.defaultPrice,
                                                                preset.defaultCurrency,
                                                                "",
                                                                "EXPENSE"
                                                            )
                                                            onQuickAddConsumption(
                                                                preset.name,
                                                                preset.category,
                                                                preset.defaultQuantity,
                                                                preset.unit,
                                                                "",
                                                                preset.defaultPrice,
                                                                preset.defaultCurrency
                                                            )
                                                        } else {
                                                            onQuickAddConsumption(
                                                                preset.name,
                                                                preset.category,
                                                                preset.defaultQuantity,
                                                                preset.unit,
                                                                "",
                                                                0.0,
                                                                preset.defaultCurrency
                                                            )
                                                        }
                                                    },
                                                    onLongClick = {
                                                        WorldClassOsUtils.HapticSignature.heavyImpact(context)
                                                        editingPresetInExpenses = preset
                                                    }
                                                )
                                                .testTag("expense_quick_tap_preset_${preset.id}")
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = preset.name,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Surface(
                                                        shape = RoundedCornerShape(50),
                                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                                    ) {
                                                        Text(
                                                            text = if (todayCountForPreset > 0) {
                                                                "×${PersianDateAndUnitUtils.toPersianDigits(todayCountForPreset.toString())}"
                                                            } else {
                                                                "+"
                                                            },
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }

                                                Text(
                                                    text = if (preset.defaultPrice > 0.0) {
                                                        PersianDateAndUnitUtils.formatCurrencyAmount(preset.defaultPrice, preset.defaultCurrency)
                                                    } else {
                                                        "${PersianDateAndUnitUtils.toPersianDigits(preset.defaultQuantity.toInt().toString())} ${preset.unit}"
                                                    },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (preset.defaultPrice > 0.0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                    if (rowPresets.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 0.b Live Daily Safe-to-Spend & Burn Rate Gauge
        item {
            val safeStatusColor = when {
                safeToSpend.isOverDailyLimit -> RoseError
                safeToSpend.burnRateRatio > 0.75f -> AmberWarning
                else -> EmeraldSuccess
            }
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("expense_safe_to_spend_card"),
                shape = RoundedCornerShape(18.dp),
                accentBorderColor = safeStatusColor.copy(alpha = 0.55f)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "سهم مجاز خرج امروز",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = safeStatusColor
                            )
                            Text(
                                text = "${PersianDateAndUnitUtils.toPersianDigits(safeToSpend.remainingDaysInMonth.toString())} روز تا پایان ماه • بودجه: ${PersianDateAndUnitUtils.formatCurrencyAmount(safeToSpend.monthlyBudget, prefs.defaultCurrency)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { showBudgetDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("بودجه ماهانه", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (safeToSpend.remainingTodayAllowance >= 0) {
                                "باقی‌مانده امروز: ${PersianDateAndUnitUtils.formatCurrencyAmount(safeToSpend.remainingTodayAllowance, prefs.defaultCurrency)}"
                            } else {
                                "عبور از سهم امروز: ${PersianDateAndUnitUtils.formatCurrencyAmount(-safeToSpend.remainingTodayAllowance, prefs.defaultCurrency)}"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = safeStatusColor
                        )
                        Text(
                            text = "سهم پایه: ${PersianDateAndUnitUtils.formatCurrencyAmount(safeToSpend.dailyAllowanceBeforeToday, prefs.defaultCurrency)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    LinearProgressIndicator(
                        progress = { safeToSpend.burnRateRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = safeStatusColor,
                        trackColor = MaterialTheme.colorScheme.surface
                    )
                }
            }
        }

        // 1. 4-Box Overview: Daily, Weekly, Monthly, Yearly Expenses & Incomes
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "خلاصه دوره‌ای",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PeriodSummaryBox(
                        modifier = Modifier.weight(1f),
                        title = "امروز",
                        expenseText = formatCurrMap(PersianDateAndUnitUtils.aggregateExpensesByCurrency(dailyEntries)),
                        incomeText = formatCurrMap(PersianDateAndUnitUtils.aggregateIncomesByCurrency(dailyEntries)),
                        isSelected = selectedPeriod == FinancialPeriodFilter.DAILY,
                        onClick = { selectedPeriod = FinancialPeriodFilter.DAILY }
                    )
                    PeriodSummaryBox(
                        modifier = Modifier.weight(1f),
                        title = "۷ روز اخیر",
                        expenseText = formatCurrMap(PersianDateAndUnitUtils.aggregateExpensesByCurrency(weeklyEntries)),
                        incomeText = formatCurrMap(PersianDateAndUnitUtils.aggregateIncomesByCurrency(weeklyEntries)),
                        isSelected = selectedPeriod == FinancialPeriodFilter.WEEKLY,
                        onClick = { selectedPeriod = FinancialPeriodFilter.WEEKLY }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PeriodSummaryBox(
                        modifier = Modifier.weight(1f),
                        title = "۳۰ روز اخیر",
                        expenseText = formatCurrMap(PersianDateAndUnitUtils.aggregateExpensesByCurrency(monthlyEntries)),
                        incomeText = formatCurrMap(PersianDateAndUnitUtils.aggregateIncomesByCurrency(monthlyEntries)),
                        isSelected = selectedPeriod == FinancialPeriodFilter.MONTHLY,
                        onClick = { selectedPeriod = FinancialPeriodFilter.MONTHLY }
                    )
                    PeriodSummaryBox(
                        modifier = Modifier.weight(1f),
                        title = "امسال",
                        expenseText = formatCurrMap(PersianDateAndUnitUtils.aggregateExpensesByCurrency(yearlyEntries)),
                        incomeText = formatCurrMap(PersianDateAndUnitUtils.aggregateIncomesByCurrency(yearlyEntries)),
                        isSelected = selectedPeriod == FinancialPeriodFilter.YEARLY,
                        onClick = { selectedPeriod = FinancialPeriodFilter.YEARLY }
                    )
                }
            }
        }

        // 2. Add Expense, Income, Receivable, or Debt Card
        item {
            val isIncome = transactionType == "INCOME"
            val isReceivable = transactionType == "RECEIVABLE"
            val isDebt = transactionType == "DEBT"
            val accentColor = when (transactionType) {
                "INCOME" -> EmeraldSuccess
                "RECEIVABLE" -> SkyInfo
                "DEBT" -> AmberWarning
                else -> RoseError
            }

            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                accentBorderColor = accentColor.copy(alpha = 0.6f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "ثبت مالی جدید",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Row 1: EXPENSE and INCOME
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isExpSelected = transactionType == "EXPENSE"
                        Button(
                            onClick = {
                                transactionType = "EXPENSE"
                                category = "غذا و کافه"
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isExpSelected) RoseError else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                            ),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isExpSelected) RoseError else MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                                contentColor = if (isExpSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("select_expense_type_button")
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("خرج", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                transactionType = "INCOME"
                                category = "سایر"
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isIncome) EmeraldSuccess else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                            ),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isIncome) EmeraldSuccess else MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                                contentColor = if (isIncome) Color.White else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("select_income_type_button")
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("درآمد", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Row 2: RECEIVABLE and DEBT
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                transactionType = "RECEIVABLE"
                                category = "سایر"
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isReceivable) SkyInfo else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                            ),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isReceivable) SkyInfo else MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                                contentColor = if (isReceivable) Color.Black else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("select_receivable_type_button")
                        ) {
                            Text("طلبکاری", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                transactionType = "DEBT"
                                category = "سایر"
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isDebt) AmberWarning else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                            ),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDebt) AmberWarning else MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                                contentColor = if (isDebt) Color.Black else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("select_debt_type_button")
                        ) {
                            Text("بدهکاری", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Mandatory Title
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            if (it.isNotBlank()) showTitleError = false
                        },
                        isError = showTitleError,
                        label = {
                            Text(
                                when (transactionType) {
                                    "RECEIVABLE" -> "نام شخص و موضوع طلب *"
                                    "DEBT" -> "نام شخص و موضوع بدهی *"
                                    "INCOME" -> "عنوان درآمد *"
                                    else -> "عنوان خرج *"
                                }
                            )
                        },
                        placeholder = {
                            Text(
                                when (transactionType) {
                                    "RECEIVABLE" -> "مثلاً: علی - قرض دستی..."
                                    "DEBT" -> "مثلاً: رضا - سهم شام..."
                                    "INCOME" -> "مثلاً: حقوق ماهانه، پروژه..."
                                    else -> "مثلاً: اسنپ، ناهار، قبض برق..."
                                }
                            )
                        },
                        supportingText = if (showTitleError) {
                            { Text("⚠️ وارد کردن عنوان الزامی است", color = RoseError) }
                        } else null,
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("financial_title_input")
                    )

                    // Amount Field
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = {
                                amountText = PersianDateAndUnitUtils.toEnglishDigits(it)
                                if (amountText.isNotBlank()) showAmountError = false
                            },
                            isError = showAmountError,
                            label = { Text("مبلغ ($currency) *") },
                            placeholder = { Text("مثلاً 500 یا 150000") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("financial_amount_input")
                        )
                        AssistChip(
                            onClick = {
                                val v = amountText.toDoubleOrNull() ?: 0.0
                                if (v > 0 && v < 100000) {
                                    amountText = (v * 1000).toLong().toString()
                                }
                            },
                            label = { Text("×۱,۰۰۰") }
                        )
                    }

                    // Multi-Currency Selector Row (from PersianDateAndUnitUtils)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("واحد پول:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        listOf(
                            "TOMAN" to "تومان",
                            "USD" to "دلار ($)",
                            "EUR" to "یورو (€)",
                            "AED" to "درهم",
                            "IRR" to "ریال"
                        ).forEach { (currCode, currLabel) ->
                            FilterChip(
                                selected = currency.equals(currCode, ignoreCase = true),
                                onClick = { currency = currCode },
                                label = { Text(currLabel, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    // Dynamic Expense Categories Selection
                    if (!isDebt && !isReceivable) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("دسته‌بندی مالی:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { showAddCategoryDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("دسته‌بندی جدید", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            customExpenseCategories.forEach { catLbl ->
                                FilterChip(
                                    selected = category == catLbl,
                                    onClick = { category = catLbl },
                                    label = { Text(catLbl) }
                                )
                            }
                        }
                    }

                    // Optional Description
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("توضیحات (اختیاری)") },
                        placeholder = { Text("توضیحات یا موعد تسویه...") },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("financial_notes_input")
                    )

                    val rawTypedAmt = amountText.toDoubleOrNull() ?: 0.0
                    if (rawTypedAmt in 1.0..999.0 && currency.equals("TOMAN", ignoreCase = true)) {
                        Text(
                            text = "معادل: ${PersianDateAndUnitUtils.formatCurrencyAmount(rawTypedAmt * 1000.0, "TOMAN")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricTeal,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            val cleanTitle = title.trim()
                            val rawAmt = amountText.toDoubleOrNull() ?: 0.0
                            val amt = PersianDateAndUnitUtils.normalizeColloquialTomanAmount(rawAmt, currency)
                            if (cleanTitle.isBlank()) {
                                WorldClassOsUtils.HapticSignature.heavyImpact(context)
                                showTitleError = true
                                return@Button
                            }
                            if (amt <= 0.0) {
                                WorldClassOsUtils.HapticSignature.heavyImpact(context)
                                showAmountError = true
                                return@Button
                            }
                            WorldClassOsUtils.HapticSignature.doublePulse(context)
                            onAddExpense(cleanTitle, category, amt, currency, notes.trim(), transactionType)
                            title = ""
                            amountText = ""
                            notes = ""
                            showTitleError = false
                            showAmountError = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = if (isReceivable || isDebt) Color.Black else Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("submit_financial_entry_button")
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (transactionType) {
                                "RECEIVABLE" -> "ثبت طلبکاری"
                                "DEBT" -> "ثبت بدهکاری"
                                "INCOME" -> "ثبت درآمد"
                                else -> "ثبت خرج"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2.b Monthly Spending & Income Trends Native Line Chart
        item {
            MonthlySpendingTrendsLineChart(
                expenses = expenses,
                defaultCurrency = prefs.defaultCurrency,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 2.b2 Animated Radial Donut Chart of Expense Categories (from MonthlyVisualExpensesGlassCard)
        item {
            MonthlyVisualExpensesGlassCard(
                expenses = monthlyEntries,
                defaultCurrency = prefs.defaultCurrency,
                onNavigateToExpenses = {},
                showNavigationButton = false
            )
        }

        // 2.b3 Net Worth, Assets & Open Claims Summary Card
        item {
            val openReceivables = remember(expenses) {
                expenses.filter { it.isReceivable && !it.isSettled }
            }
            val openDebts = remember(expenses) {
                expenses.filter { it.isDebt && !it.isSettled }
            }
            val totalReceivableMap = remember(openReceivables) {
                openReceivables.groupBy { it.currency }.mapValues { (_, list) -> list.sumOf { it.amount } }
            }
            val totalDebtMap = remember(openDebts) {
                openDebts.groupBy { it.currency }.mapValues { (_, list) -> list.sumOf { it.amount } }
            }
            val allTimeNetByCurr = remember(expenses) {
                PersianDateAndUnitUtils.aggregateNetBalanceByCurrency(expenses)
            }

            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                accentBorderColor = SkyInfo.copy(alpha = 0.55f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "تراز کل و مطالبات باز",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SkyInfo
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("تراز خالص کل:", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = formatCurrMap(allTimeNetByCurr),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = ElectricTeal
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("طلب‌های باز (${PersianDateAndUnitUtils.toPersianDigits(openReceivables.size.toString())}):", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = formatCurrMap(totalReceivableMap),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = SkyInfo
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("بدهی‌های باز (${PersianDateAndUnitUtils.toPersianDigits(openDebts.size.toString())}):", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = formatCurrMap(totalDebtMap),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = AmberWarning
                        )
                    }
                }
            }
        }

        // 2.c Dedicated Custom Expense Categories Management Card
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                accentBorderColor = ElectricTeal.copy(alpha = 0.5f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Category, contentDescription = null, tint = ElectricTeal)
                            Text(
                                text = "دسته‌بندی‌های مالی",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        OutlinedButton(
                            onClick = { showAddCategoryDialog = true },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("دسته‌بندی جدید")
                        }
                    }

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        customExpenseCategories.forEach { cat ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                border = BorderStroke(1.dp, ElectricTeal.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(getCategoryIcon(cat), contentDescription = null, tint = ElectricTeal, modifier = Modifier.size(14.dp))
                                    Text(cat, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Advanced Transactions List Section with Live Search & Export Report Button
        item {
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                accentBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تراکنش‌های ثبت‌شده (${PersianDateAndUnitUtils.toPersianDigits(filteredTransactions.size.toString())})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = {
                                    ReportCsvExporter.shareMonthlyCsvReport(
                                        context = context,
                                        todayEpochDay = todayEpochDay,
                                        tasks = emptyList(),
                                        expenses = filteredTransactions,
                                        consumption = consumption,
                                        sleepRecords = emptyList()
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("اکسل CSV", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = { showExportReportDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("گزارش متنی", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    // Search input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("جستجو در تراکنش‌ها...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ElectricTeal) },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "پاک کردن", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else null,
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("transaction_search_input")
                    )

                    Text(
                        text = "بازه زمانی:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FinancialPeriodFilter.entries.forEach { period ->
                            FilterChip(
                                selected = selectedPeriod == period,
                                onClick = { selectedPeriod = period },
                                label = { Text(period.labelFa) }
                            )
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "ALL" to "همه (${PersianDateAndUnitUtils.toPersianDigits(periodEntries.size.toString())})",
                            "EXPENSE" to "خرج‌ها (${PersianDateAndUnitUtils.toPersianDigits(periodEntries.count { it.isExpense }.toString())})",
                            "INCOME" to "درآمدها (${PersianDateAndUnitUtils.toPersianDigits(periodEntries.count { it.isIncome }.toString())})",
                            "RECEIVABLE" to "طلبکاری‌ها (${PersianDateAndUnitUtils.toPersianDigits(periodEntries.count { it.isReceivable }.toString())})",
                            "DEBT" to "بدهکاری‌ها (${PersianDateAndUnitUtils.toPersianDigits(periodEntries.count { it.isDebt }.toString())})"
                        ).forEach { (code, lbl) ->
                            FilterChip(
                                selected = selectedTypeFilter == code,
                                onClick = { selectedTypeFilter = code },
                                label = { Text(lbl) }
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("مجموع خرج (${selectedPeriod.labelFa}):", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = formatCurrMap(periodExpenseByCurr),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = RoseError
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("مجموع درآمد (${selectedPeriod.labelFa}):", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = formatCurrMap(periodIncomeByCurr),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldSuccess
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("تراز خالص دوره:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = formatCurrMap(periodNetByCurr),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = ElectricTeal
                        )
                    }
                }
            }
        }

        // 4. List of Recorded Financial Entries with Category Icons & Edit Capability
        if (filteredTransactions.isEmpty()) {
            item {
                LiquidGlassCard(
                    shape = RoundedCornerShape(16.dp),
                    accentBorderColor = ElectricTeal,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "تراکنشی با مشخصات «$searchQuery» یافت نشد." else "در بازه «${selectedPeriod.labelFa}» رکوردی ثبت نشده است.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        }

        items(filteredTransactions, key = { it.id }) { exp ->
            val badgeColor = when {
                exp.isReceivable -> SkyInfo
                exp.isDebt -> AmberWarning
                exp.isIncome -> EmeraldSuccess
                else -> RoseError
            }
            val badgeLabel = when {
                exp.isReceivable -> if (exp.isSettled) "طلب (تسویه‌شده)" else "طلبکاری"
                exp.isDebt -> if (exp.isSettled) "بدهی (تسویه‌شده)" else "بدهکاری"
                exp.isIncome -> "درآمد"
                else -> "خرج"
            }
            val cleanDesc = exp.cleanNotes
            val catIcon = getCategoryIcon(exp.category)

            LiquidGlassCard(
                shape = RoundedCornerShape(16.dp),
                accentBorderColor = badgeColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = badgeColor.copy(alpha = 0.16f)
                            ) {
                                Icon(
                                    imageVector = catIcon,
                                    contentDescription = null,
                                    tint = badgeColor,
                                    modifier = Modifier.padding(6.dp).size(16.dp)
                                )
                            }
                            LiquidGlassPill(
                                shape = RoundedCornerShape(6.dp),
                                accentGlow = badgeColor
                            ) {
                                Text(
                                    text = badgeLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = badgeColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = exp.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = (if (exp.isIncome || exp.isReceivable) "+ " else "- ") +
                                    PersianDateAndUnitUtils.formatCurrencyAmount(exp.amount, exp.currency),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )

                        if (cleanDesc.isNotBlank()) {
                            GlassSurface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF141F1A),
                                alpha = 0.6f,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "توضیحات: $cleanDesc",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = "${PersianDateAndUnitUtils.formatEpochDay(exp.localEpochDay, prefs.calendarDisplay)} • دسته: ${exp.category}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if ((exp.isDebt || exp.isReceivable) && !exp.isSettled) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = EmeraldSuccess.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.55f)),
                                modifier = Modifier
                                    .clickable {
                                        WorldClassOsUtils.HapticSignature.doublePulse(context)
                                        onSettleDebtOrReceivable(exp)
                                    }
                                    .testTag("daily_log_settle_iou_${exp.id}")
                            ) {
                                Text(
                                    text = "تسویه شد ✓",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldSuccess,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                        IconButton(onClick = { editingTransaction = exp }) {
                            Icon(Icons.Default.Edit, contentDescription = "ویرایش تراکنش", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { onDeleteExpense(exp) }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف رکورد مالی", tint = RoseError)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun PeriodSummaryBox(
    modifier: Modifier = Modifier,
    title: String,
    expenseText: String,
    incomeText: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    LiquidGlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        accentBorderColor = if (isSelected) ElectricTeal else null,
        borderWidth = if (isSelected) 1.5.dp else 1.dp,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) ElectricTeal else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "خرج: $expenseText",
                style = MaterialTheme.typography.bodySmall,
                color = RoseError,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "درآمد: $incomeText",
                style = MaterialTheme.typography.labelSmall,
                color = EmeraldSuccess,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
