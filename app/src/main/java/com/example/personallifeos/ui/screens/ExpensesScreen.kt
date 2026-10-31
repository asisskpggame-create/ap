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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personallifeos.data.model.CategoryBudget
import com.example.personallifeos.data.model.DebtItem
import com.example.personallifeos.data.model.DebtType
import com.example.personallifeos.data.model.ExpenseItem
import com.example.personallifeos.data.model.QuickPreset
import com.example.personallifeos.data.model.TransactionType
import com.example.personallifeos.sms.BankSmsParsedResult
import com.example.personallifeos.sms.BankUserConfig
import com.example.personallifeos.sms.DetectedCurrencyUnit
import com.example.personallifeos.sms.IranianBankSmsEngine
import com.example.personallifeos.sms.SmsTransactionDirection
import com.example.personallifeos.ui.components.AppSvgIcons
import com.example.personallifeos.ui.components.SmsScanner
import com.example.personallifeos.ui.theme.AmberWarning
import com.example.personallifeos.ui.theme.BorderCyanGlow
import com.example.personallifeos.ui.theme.CyanPrimary
import com.example.personallifeos.ui.theme.EmeraldAccent
import com.example.personallifeos.ui.theme.RoseDanger
import com.example.personallifeos.ui.theme.SurfaceCard
import com.example.personallifeos.ui.theme.SurfaceDark
import com.example.personallifeos.ui.theme.TextMuted
import com.example.personallifeos.ui.theme.TextPrimary
import com.example.personallifeos.ui.theme.TextSecondary
import com.example.personallifeos.util.CurrencyInputUnit
import com.example.personallifeos.util.PersianUtils

@Composable
fun ExpensesScreen(
    expenses: List<ExpenseItem>,
    debts: List<DebtItem> = emptyList(),
    quickPresets: List<QuickPreset> = emptyList(),
    categoryBudgets: List<CategoryBudget> = emptyList(),
    configuredBanks: List<BankUserConfig> = emptyList(),
    pendingReviewSms: BankSmsParsedResult? = null,
    onAddExpense: (String, Long, String, String?, Boolean, TransactionType, String?) -> Unit,
    onUpdateExpense: (ExpenseItem) -> Unit = {},
    onDeleteExpense: (String) -> Unit,
    onTriggerQuickPreset: (QuickPreset) -> Unit = {},
    onAddQuickPreset: (String, Long, String, String) -> Unit = { _, _, _, _ -> },
    onAddDebt: (String, String, Long, String, DebtType, Int) -> Unit = { _, _, _, _, _, _ -> },
    onRecordDebtPayment: (DebtItem, Long) -> Unit = { _, _ -> },
    onCreateDebtPlan: (DebtItem, Int) -> Unit = { _, _ -> },
    onSettleDebt: (DebtItem) -> Unit = {},
    onDeleteDebt: (String) -> Unit = {},
    onParseSms: (String, String?) -> String,
    onConfirmReview: (SmsTransactionDirection, DetectedCurrencyUnit, Long?) -> String = { _, _, _ -> "" },
    onDismissReview: () -> Unit = {},
    onOpenBankSelector: () -> Unit = {},
    onOpenNotificationCenter: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedFinanceTab by remember { mutableIntStateOf(0) }
    val clipboardManager = LocalClipboardManager.current

    // Manual transaction form states
    var manualTitle by remember { mutableStateOf("") }
    var manualAmount by remember { mutableStateOf("") }
    var manualCategory by remember { mutableStateOf("خرید عمومی") }
    var selectedTxType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedCurrencyUnit by remember { mutableStateOf(CurrencyInputUnit.TOMAN) }
    var selectedAccountName by remember { mutableStateOf("بانک ملت") }
    var searchQuery by remember { mutableStateOf("") }
    var editingExpense by remember { mutableStateOf<ExpenseItem?>(null) }
    var showAddPresetDialog by remember { mutableStateOf(false) }

    // Debt form states
    var debtTitle by remember { mutableStateOf("") }
    var debtPerson by remember { mutableStateOf("") }
    var debtAmount by remember { mutableStateOf("") }
    var debtDueDate by remember { mutableStateOf("۱۴۰۵/۰۸/۲۰") }
    var selectedDebtType by remember { mutableStateOf(DebtType.DEBT) }
    var debtInstallmentsCount by remember { mutableStateOf("4") }
    var activePaymentDebt by remember { mutableStateOf<DebtItem?>(null) }

    // SMS states
    var smsInput by remember { mutableStateOf("") }
    var senderInput by remember { mutableStateOf("") }
    var smsFeedback by remember { mutableStateOf<String?>(null) }
    var isSmsScannerOpen by remember { mutableStateOf(false) }
    var csvExportPreview by remember { mutableStateOf<String?>(null) }

    // BigDecimal Accounting Summaries
    val totalSpent = PersianUtils.aggregateExpensesToman(expenses)
    val totalIncome = PersianUtils.aggregateIncomesToman(expenses)
    val totalTransfer = PersianUtils.aggregateTransfersToman(expenses)
    val netBalance = PersianUtils.aggregateNetBalanceToman(expenses)
    val safeToSpendSummary = PersianUtils.calculateSafeToSpend(25_000_000L, expenses, debts)
    val accountBalances = PersianUtils.aggregateAccountBalances(expenses)
    val totalDebtsRemaining = PersianUtils.aggregateTotalDebtsRemainingToman(debts)
    val totalReceivablesRemaining = PersianUtils.aggregateTotalReceivablesRemainingToman(debts)

    // Colloquial Toman live preview
    val colloquialPreview = remember(manualAmount, manualTitle, selectedCurrencyUnit) {
        if (selectedCurrencyUnit == CurrencyInputUnit.TOMAN && manualAmount.isNotBlank()) {
            PersianUtils.normalizeColloquialTomanAmount("$manualAmount $manualTitle")
        } else null
    }

    val filteredExpenses = remember(expenses, searchQuery) {
        if (searchQuery.isBlank()) expenses
        else {
            val q = PersianUtils.normalizePersianText(searchQuery)
            expenses.filter {
                PersianUtils.normalizePersianText(it.title).contains(q) ||
                    PersianUtils.normalizePersianText(it.category).contains(q) ||
                    PersianUtils.normalizePersianText(it.bankName ?: "").contains(q)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("expenses_screen")
    ) {
        // 4-Section Finance Pro Sub-Navigation
        ScrollableTabRow(
            selectedTabIndex = selectedFinanceTab,
            containerColor = SurfaceDark,
            contentColor = CyanPrimary,
            edgePadding = 12.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedFinanceTab]),
                    color = CyanPrimary
                )
            }
        ) {
            listOf(
                "۱. ورودی و خروجی‌ها",
                "۲. طلب، بدهی و اقساط (${PersianUtils.formatPersianNumber(debts.size.toLong())})",
                "۳. بودجه، حساب‌ها و گزارش",
                "۴. پیامک بانکی و میرزا"
            ).forEachIndexed { idx, title ->
                Tab(
                    selected = selectedFinanceTab == idx,
                    onClick = { selectedFinanceTab = idx },
                    text = {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (selectedFinanceTab == idx) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedFinanceTab == idx) CyanPrimary else TextSecondary
                        )
                    },
                    modifier = Modifier.testTag("finance_subtab_$idx")
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Financial Overview Card (Always visible at top of Finance Hub)
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0x33F59E0B), RoundedCornerShape(22.dp))
                        .testTag("expenses_hero_card")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "تراز مالی، درآمد و Safe-to-Spend",
                                    color = AmberWarning,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF1E6F30))
                                            .clickable { onOpenBankSelector() }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                            .testTag("open_mirza_bank_selector_btn")
                                    ) {
                                        Text("+ افزودن بانک", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(CyanPrimary.copy(alpha = 0.2f))
                                            .border(1.dp, CyanPrimary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                            .clickable { onOpenNotificationCenter() }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                            .testTag("open_notif_center_from_expenses_btn")
                                    ) {
                                        Text("فیش‌خوان زنده", color = CyanPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // 3-Column Financial Summary
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SurfaceCard)
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text("هزینه خالص", fontSize = 10.sp, color = TextMuted)
                                        Text(
                                            text = PersianUtils.formatToman(totalSpent),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RoseDanger
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SurfaceCard)
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text("مجموع درآمد", fontSize = 10.sp, color = TextMuted)
                                        Text(
                                            text = PersianUtils.formatToman(totalIncome),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldAccent
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SurfaceCard)
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text("تراز خالص", fontSize = 10.sp, color = TextMuted)
                                        Text(
                                            text = PersianUtils.formatToman(netBalance),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (netBalance >= 0) CyanPrimary else RoseDanger
                                        )
                                    }
                                }
                            }

                            // Safe-to-Spend Box with transparent formula explanation
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x1A06B6D4))
                                    .border(0.5.dp, CyanPrimary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .padding(10.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("مبلغ قابل‌خرج تا پایان ماه (Safe-to-Spend):", fontSize = 11.sp, color = CyanPrimary, fontWeight = FontWeight.Bold)
                                        Text(PersianUtils.formatToman(safeToSpendSummary.safeToSpendToman), fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.ExtraBold)
                                    }
                                    Text(safeToSpendSummary.formulaExplanationFa, fontSize = 10.sp, color = TextSecondary)
                                    if (totalTransfer > 0) {
                                        Text(
                                            text = "ℹ️ انتقال بین حساب‌های شخصی (${PersianUtils.formatToman(totalTransfer)}) طبق قانون حسابداری در درآمد یا هزینه شمرده نشده است.",
                                            fontSize = 10.sp,
                                            color = AmberWarning
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Audited Manual Review Card if any NEEDS_REVIEW SMS exists
            pendingReviewSms?.let { pending ->
                item {
                    NeedsReviewSmsCard(
                        pending = pending,
                        onConfirm = { dir, curr, amt -> smsFeedback = onConfirmReview(dir, curr, amt) },
                        onDismiss = {
                            onDismissReview()
                            smsFeedback = "پیامک مبهم توسط کاربر رد شد."
                        }
                    )
                }
            }

            // =================================================================
            // TAB 0: ورودی و خروجی‌ها (1-Tap Presets, Colloquial Entry, Search, Edit)
            // =================================================================
            if (selectedFinanceTab == 0) {
                // 1-Tap Quick Presets Row
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        modifier = Modifier.fillMaxWidth()
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
                                Text(
                                    text = "ثبت سریع با ۱ لمس (Quick Presets)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "+ افزودن دکمه سریع",
                                    fontSize = 11.sp,
                                    color = CyanPrimary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { showAddPresetDialog = true }
                                )
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(quickPresets, key = { it.id }) { preset ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(SurfaceDark)
                                            .border(0.5.dp, CyanPrimary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                            .clickable { onTriggerQuickPreset(preset) }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                            .testTag("quick_preset_${preset.id}")
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = AppSvgIcons.resolveQuickPresetVector(preset.iconEmoji, preset.category),
                                                contentDescription = preset.title,
                                                tint = CyanPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(preset.title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                                Text(PersianUtils.formatToman(preset.defaultAmount), fontSize = 10.sp, color = CyanPrimary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Multi-Currency & Colloquial Transaction Form
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "ثبت تراکنش جدید (هزینه، درآمد، انتقال بین حساب‌ها)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            // Transaction Type Selector
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TransactionType.entries.forEach { tType ->
                                    val active = selectedTxType == tType
                                    val activeColor = when (tType) {
                                        TransactionType.EXPENSE -> RoseDanger
                                        TransactionType.INCOME -> EmeraldAccent
                                        TransactionType.TRANSFER -> CyanPrimary
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (active) activeColor else SurfaceDark)
                                            .clickable { selectedTxType = tType }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = tType.titleFa,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (active) Color.Black else TextSecondary
                                        )
                                    }
                                }
                            }

                            // Multi-Currency Selector
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(CurrencyInputUnit.entries) { unit ->
                                    val active = selectedCurrencyUnit == unit
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (active) AmberWarning else SurfaceDark)
                                            .clickable { selectedCurrencyUnit = unit }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = unit.labelFa,
                                            fontSize = 11.sp,
                                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                            color = if (active) Color.Black else TextSecondary
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = manualTitle,
                                    onValueChange = { manualTitle = it },
                                    placeholder = { Text("عنوان (مثلاً: سیگار، اسنپ، حقوق)", fontSize = 12.sp) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("manual_expense_title"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = manualAmount,
                                    onValueChange = { manualAmount = it },
                                    placeholder = { Text("مبلغ (${selectedCurrencyUnit.labelFa})", fontSize = 12.sp) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("manual_expense_amount"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            // Colloquial Toman Live Preview Banner (v1.2.0 feature)
                            colloquialPreview?.let { cp ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0x2610B981))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = if (cp.wasColloquialMultiplied)
                                            "💡 معادل محاوره‌ای تشخیص‌داده‌شده: ${cp.formattedPreview} • دسته پیشنهادی: ${cp.suggestedCategory}"
                                        else
                                            "💡 مبلغ نهایی: ${cp.formattedPreview} • دسته: ${cp.suggestedCategory}",
                                        fontSize = 11.sp,
                                        color = EmeraldAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Account & Category Selector Row
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(listOf("بانک ملت", "بلوبانک", "بانک ملی ایران", "بانک سامان", "کیف پول نقد")) { acc ->
                                    val isSel = selectedAccountName == acc
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) CyanPrimary.copy(alpha = 0.25f) else SurfaceDark)
                                            .border(0.5.dp, if (isSel) CyanPrimary else Color.Transparent, RoundedCornerShape(8.dp))
                                            .clickable { selectedAccountName = acc }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text("حساب: $acc", fontSize = 11.sp, color = if (isSel) CyanPrimary else TextMuted)
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    val finalAmountToman = if (selectedCurrencyUnit == CurrencyInputUnit.TOMAN && colloquialPreview != null) {
                                        colloquialPreview.amountToman
                                    } else {
                                        PersianUtils.convertToTomanBigDecimal(manualAmount, selectedCurrencyUnit)
                                    }
                                    val finalTitle = manualTitle.ifBlank { colloquialPreview?.extractedTitle ?: selectedTxType.titleFa }
                                    val finalCat = if (manualTitle.isNotBlank()) PersianUtils.inferCategoryFromText(manualTitle) else manualCategory

                                    if (finalTitle.isNotBlank() && finalAmountToman > 0) {
                                        onAddExpense(
                                            finalTitle,
                                            finalAmountToman,
                                            finalCat,
                                            selectedAccountName,
                                            false,
                                            selectedTxType,
                                            selectedAccountName
                                        )
                                        manualTitle = ""
                                        manualAmount = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("manual_expense_submit")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("ثبت در دفتر کل مالی (${selectedTxType.titleFa})", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Live Search in Transactions
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("جستجوی زنده در عنوان، دسته‌بندی یا نام بانک...", fontSize = 12.sp, color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CyanPrimary) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("expense_search_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceCard,
                            unfocusedContainerColor = SurfaceCard,
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = Color(0x26FFFFFF)
                        )
                    )
                }

                items(filteredExpenses, key = { it.id }) { expense ->
                    TransactionRowCard(
                        expense = expense,
                        onEdit = { editingExpense = expense },
                        onDelete = { onDeleteExpense(expense.id) }
                    )
                }
            }

            // =================================================================
            // TAB 1: طلب، بدهی و اقساط (Debt & Receivable + Goal Link)
            // =================================================================
            if (selectedFinanceTab == 1) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("بدهی باقی‌مانده من", fontSize = 11.sp, color = TextMuted)
                                Text(
                                    text = PersianUtils.formatToman(totalDebtsRemaining),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RoseDanger
                                )
                            }
                        }
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("مطالبات باز (طلب من)", fontSize = 11.sp, color = TextMuted)
                                Text(
                                    text = PersianUtils.formatToman(totalReceivablesRemaining),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldAccent
                                )
                            }
                        }
                    }
                }

                // Add New Debt / Receivable Card
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "ثبت بدهی یا طلب جدید + اتصال به اهداف و مسیر",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 14.sp
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                DebtType.entries.forEach { dType ->
                                    val active = selectedDebtType == dType
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (active) CyanPrimary else SurfaceDark)
                                            .clickable { selectedDebtType = dType }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dType.titleFa,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (active) Color.Black else TextSecondary
                                        )
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = debtTitle,
                                    onValueChange = { debtTitle = it },
                                    placeholder = { Text("عنوان (مثلاً: وام خودرو)", fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = debtPerson,
                                    onValueChange = { debtPerson = it },
                                    placeholder = { Text("طرف حساب / بانک", fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = debtAmount,
                                    onValueChange = { debtAmount = it.filter { ch -> ch.isDigit() } },
                                    placeholder = { Text("مبلغ کل (تومان)", fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = debtInstallmentsCount,
                                    onValueChange = { debtInstallmentsCount = it.filter { ch -> ch.isDigit() } },
                                    placeholder = { Text("تعداد اقساط در مسیر", fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            Button(
                                onClick = {
                                    val amt = debtAmount.toLongOrNull() ?: 0L
                                    val count = debtInstallmentsCount.toIntOrNull() ?: 0
                                    if (debtTitle.isNotBlank() && amt > 0) {
                                        onAddDebt(
                                            debtTitle,
                                            debtPerson.ifBlank { "طرف حساب" },
                                            amt,
                                            debtDueDate,
                                            selectedDebtType,
                                            count
                                        )
                                        debtTitle = ""
                                        debtPerson = ""
                                        debtAmount = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("add_debt_submit_btn")
                            ) {
                                Text("ثبت و ساخت برنامه اقساط در مسیر 🔗", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                items(debts, key = { it.id }) { debt ->
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                0.5.dp,
                                if (debt.isSettled) EmeraldAccent else Color(0x33FFFFFF),
                                RoundedCornerShape(18.dp)
                            )
                            .testTag("debt_card_${debt.id}")
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
                                Column {
                                    Text(
                                        text = "${debt.title} (${debt.personName})",
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "${debt.type.titleFa} • سررسید: ${debt.dueDate}",
                                        fontSize = 11.sp,
                                        color = if (debt.type == DebtType.DEBT) RoseDanger else EmeraldAccent
                                    )
                                }
                                IconButton(onClick = { onDeleteDebt(debt.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = TextMuted)
                                }
                            }

                            val progress = if (debt.totalAmount > 0) {
                                (debt.paidAmount.toFloat() / debt.totalAmount.toFloat()).coerceIn(0f, 1f)
                            } else 1f

                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (debt.isSettled) EmeraldAccent else CyanPrimary,
                                trackColor = SurfaceDark
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "پرداخت‌شده: ${PersianUtils.formatToman(debt.paidAmount)}",
                                    fontSize = 11.sp,
                                    color = EmeraldAccent
                                )
                                Text(
                                    text = "مانده فعلی: ${PersianUtils.formatToman(debt.remainingAmount)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (debt.remainingAmount == 0L) EmeraldAccent else AmberWarning
                                )
                            }

                            if (!debt.isSettled) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = { activePaymentDebt = debt },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("+ ثبت قسط / پرداخت جزئی", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { onCreateDebtPlan(debt, 4) },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Link, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("اتصال به مسیر", color = CyanPrimary, fontSize = 11.sp)
                                    }
                                    OutlinedButton(
                                        onClick = { onSettleDebt(debt) },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("تسویه ✓", color = EmeraldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(14.dp))
                                    Text("این مورد به طور کامل تسویه شده و یادآوری‌های آینده آن لغو گردیده است.", fontSize = 11.sp, color = EmeraldAccent)
                                }
                            }
                        }
                    }
                }
            }

            // =================================================================
            // TAB 2: بودجه، حساب‌ها و گزارش (Accounts, Budgets, Charts, CSV)
            // =================================================================
            if (selectedFinanceTab == 2) {
                // Account Balances Card
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "تراز تفکیکی حساب‌های بانکی و کیف پول",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                            accountBalances.forEach { (accName, bal) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SurfaceDark)
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(accName, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    }
                                    Text(
                                        text = PersianUtils.formatToman(bal),
                                        color = if (bal >= 0) EmeraldAccent else RoseDanger,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Category Budgets & Alerts
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "بودجه‌بندی ماهانه دسته‌ها و هشدار سقف مصرف",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                            val spentByCat = expenses
                                .filter { it.type == TransactionType.EXPENSE }
                                .groupBy { it.category }
                                .mapValues { e -> e.value.sumOf { it.amount } }

                            categoryBudgets.forEach { budget ->
                                val spent = spentByCat[budget.category] ?: 0L
                                val ratio = (spent.toFloat() / budget.monthlyLimitToman.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
                                val isNearLimit = ratio >= 0.8f
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(budget.category, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                                        Text(
                                            text = "${PersianUtils.formatToman(spent)} از ${PersianUtils.formatToman(budget.monthlyLimitToman)}",
                                            fontSize = 11.sp,
                                            color = if (isNearLimit) RoseDanger else TextSecondary
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { ratio },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(7.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = when {
                                            ratio >= 0.9f -> RoseDanger
                                            ratio >= 0.7f -> AmberWarning
                                            else -> EmeraldAccent
                                        },
                                        trackColor = SurfaceDark
                                    )
                                }
                            }
                        }
                    }
                }

                // CSV / Excel Report Export Card
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("خروجی اکسل (CSV) و گزارش جامع مالی", fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(
                                text = "شامل تمام تراکنش‌ها، انتقال‌ها، بدهی‌ها و مطالبات با فرمت استاندارد CSV جهت باز کردن در Excel یا Google Sheets.",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            Button(
                                onClick = {
                                    val csv = PersianUtils.generateCsvReport(expenses, debts)
                                    clipboardManager.setText(AnnotatedString(csv))
                                    csvExportPreview = csv
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("export_csv_btn")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تولید و کپی خروجی CSV در کلیپ‌بورد", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                            csvExportPreview?.let {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(14.dp))
                                    Text("گزارش CSV در حافظه کلیپ‌بورد کپی شد!", fontSize = 12.sp, color = EmeraldAccent)
                                }
                            }
                        }
                    }
                }
            }

            // =================================================================
            // TAB 3: پیامک بانکی و میرزا (Mirza Visual Banks & SMS Parser)
            // =================================================================
            if (selectedFinanceTab == 3) {
                // SmsScanner Launch Banner
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF0C2440), Color(0xFF07182C))
                                )
                            )
                            .border(1.dp, CyanPrimary.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                            .clickable { isSmsScannerOpen = true }
                            .padding(16.dp)
                            .testTag("launch_sms_scanner_card")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(CyanPrimary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Sms, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(24.dp))
                                }
                                Column {
                                    Text(
                                        text = "اسکنر پیامک‌های بانکی (SmsScanner)",
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "اسکن زنده پیامک‌ها، استخراج نام بانک، مبلغ و ثبت هوشمند در هزینه‌ها",
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { isSmsScannerOpen = true },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(38.dp).testTag("open_sms_scanner_btn")
                            ) {
                                Text("اسکن پیامک", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }

                if (configuredBanks.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "بانک‌ها و سرشماره‌های اختصاصی (${PersianUtils.formatPersianNumber(configuredBanks.size.toLong())})",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "+ افزودن بانک جدید (میرزا)",
                                    color = EmeraldAccent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier
                                        .clickable { onOpenBankSelector() }
                                        .testTag("open_mirza_bank_selector_btn")
                                )
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(configuredBanks, key = { it.bank.name }) { cfg ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(Color(0xFF071E3D), Color(0xFF0E3762))
                                                )
                                            )
                                            .border(1.dp, CyanPrimary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                            .clickable { senderInput = cfg.senderNumber }
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(cfg.bank.faName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                if (cfg.requireSenderMatch) {
                                                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF86EFAC), modifier = Modifier.size(12.dp))
                                                }
                                                Text(
                                                    text = if (cfg.requireSenderMatch) "فقط سرشماره: ${cfg.senderNumber}" else "سرشماره: ${cfg.senderNumber}",
                                                    color = Color(0xFF86EFAC),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(0.5.dp, BorderCyanGlow, RoundedCornerShape(20.dp))
                            .testTag("sms_parser_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0x2606B6D4)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Message, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "فیش‌خوان هوشمند با قفل سرشماره اختصاصی",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "هر بانک فقط پیامک‌های ارسالی از سرشماره ثبت‌شده خودش را می‌خواند تا خطای ورودی/خروجی رخ ندهد:",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("الگوهای شبکه بانکی (تست با ۱ لمس به همراه سرشماره بانک):", fontSize = 11.sp, color = TextMuted)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(IranianBankSmsEngine.SAMPLE_SMS_TEMPLATES.keys.toList()) { bankName ->
                                        val sampleText = IranianBankSmsEngine.SAMPLE_SMS_TEMPLATES[bankName] ?: ""
                                        val isSelected = smsInput == sampleText
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) CyanPrimary else SurfaceDark)
                                                .border(0.5.dp, if (isSelected) CyanPrimary else Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                                                .clickable {
                                                    smsInput = sampleText
                                                    val detectedBank = IranianBankSmsEngine.detectBank(IranianBankSmsEngine.normalizeDigits(sampleText))
                                                    val matchedCfg = configuredBanks.firstOrNull { it.bank == detectedBank }
                                                    senderInput = matchedCfg?.senderNumber
                                                        ?: detectedBank.defaultSenders.firstOrNull()
                                                        ?: ""
                                                    smsFeedback = null
                                                }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = bankName,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) Color.Black else TextSecondary
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = senderInput,
                                    onValueChange = { senderInput = it },
                                    placeholder = {
                                        Text("سرشماره فرستنده (مثلاً 98300017)", color = TextMuted, fontSize = 12.sp)
                                    },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("sms_sender_field"),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedButton(
                                    onClick = { senderInput = "+989120000000" },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("تست شماره متفرقه", color = AmberWarning, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedTextField(
                                value = smsInput,
                                onValueChange = { smsInput = it },
                                placeholder = {
                                    Text("متن پیامک بانکی را وارد یا از کلیپ‌بورد پیست کنید...", color = TextMuted, fontSize = 12.sp)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("sms_input_field"),
                                minLines = 3,
                                maxLines = 4,
                                shape = RoundedCornerShape(14.dp)
                            )

                            smsFeedback?.let { msg ->
                                val isWarning = msg.contains("نیازمند بررسی") || msg.contains("مسدود") || msg.contains("رد شد")
                                val feedbackColor = if (isWarning) AmberWarning else EmeraldAccent
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(feedbackColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                        .padding(10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isWarning) Icons.Default.Warning else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = feedbackColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = msg, color = feedbackColor, fontSize = 12.sp)
                                }
                            }

                            Button(
                                onClick = {
                                    if (smsInput.isNotBlank()) {
                                        smsFeedback = onParseSms(smsInput, senderInput.takeIf { it.isNotBlank() })
                                        smsInput = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("parse_sms_btn")
                            ) {
                                Text("تحلیل و ثبت خودکار تراکنش", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Partial Debt Payment Dialog
    activePaymentDebt?.let { debt ->
        var payInput by remember(debt.id) { mutableStateOf((debt.remainingAmount / 2).coerceAtLeast(100_000L).toString()) }
        AlertDialog(
            onDismissRequest = { activePaymentDebt = null },
            title = { Text("ثبت پرداخت جزئی / قسط", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("عنوان: ${debt.title} (${debt.personName})", fontSize = 12.sp, color = TextSecondary)
                    Text("مانده فعلی: ${PersianUtils.formatToman(debt.remainingAmount)}", fontSize = 13.sp, color = AmberWarning, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = payInput,
                        onValueChange = { payInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("مبلغ قسط پرداختی (تومان)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = payInput.toLongOrNull() ?: 0L
                        if (amt > 0) {
                            onRecordDebtPayment(debt, amt)
                            activePaymentDebt = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent)
                ) {
                    Text("ثبت قسط و کسر از مانده", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { activePaymentDebt = null }) {
                    Text("انصراف", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Edit Transaction Dialog
    editingExpense?.let { exp ->
        var editTitle by remember(exp.id) { mutableStateOf(exp.title) }
        var editAmount by remember(exp.id) { mutableStateOf(exp.amount.toString()) }
        var editCat by remember(exp.id) { mutableStateOf(exp.category) }
        AlertDialog(
            onDismissRequest = { editingExpense = null },
            title = { Text("ویرایش تراکنش ثبت‌شده", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("عنوان تراکنش") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editAmount,
                        onValueChange = { editAmount = it.filter { ch -> ch.isDigit() } },
                        label = { Text("مبلغ (تومان)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editCat,
                        onValueChange = { editCat = it },
                        label = { Text("دسته‌بندی") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newAmt = editAmount.toLongOrNull() ?: exp.amount
                        onUpdateExpense(exp.copy(title = editTitle, amount = newAmt, category = editCat))
                        editingExpense = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                ) {
                    Text("ذخیره تغییرات", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingExpense = null }) {
                    Text("انصراف", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // SmsScanner Component Modal
    if (isSmsScannerOpen) {
        SmsScanner(
            isOpen = true,
            onDismiss = { isSmsScannerOpen = false },
            handleAddExpense = { title, amount, category, bankName, isSms, type, accountName ->
                onAddExpense(title, amount, category, bankName, isSms, type, accountName)
            }
        )
    }

    // Add 1-Tap Quick Preset Dialog
    if (showAddPresetDialog) {
        var pTitle by remember { mutableStateOf("") }
        var pAmount by remember { mutableStateOf("") }
        var pEmoji by remember { mutableStateOf("SVG_COFFEE") }
        AlertDialog(
            onDismissRequest = { showAddPresetDialog = false },
            title = { Text("افزودن دکمه ثبت سریع ۱-لمسی", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = pTitle, onValueChange = { pTitle = it }, label = { Text("عنوان (مثلاً: قهوه صبح)") }, singleLine = true)
                    OutlinedTextField(value = pAmount, onValueChange = { pAmount = it.filter { ch -> ch.isDigit() } }, label = { Text("مبلغ پیش‌فرض (تومان)") }, singleLine = true)
                    Text("انتخاب آیکون وکتور (SVG):", fontSize = 12.sp, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("SVG_COFFEE", "SVG_TAXI", "SVG_SHOP", "SVG_FOOD", "SVG_GAS", "SVG_BOLT").forEach { key ->
                            val selected = pEmoji == key
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) CyanPrimary.copy(alpha = 0.2f) else SurfaceCard)
                                    .border(1.dp, if (selected) CyanPrimary else Color(0x26FFFFFF), RoundedCornerShape(10.dp))
                                    .clickable { pEmoji = key },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = AppSvgIcons.resolveQuickPresetVector(key),
                                    contentDescription = key,
                                    tint = if (selected) CyanPrimary else TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = pAmount.toLongOrNull() ?: 0L
                        if (pTitle.isNotBlank() && amt > 0) {
                            onAddQuickPreset(pTitle, amt, PersianUtils.inferCategoryFromText(pTitle), pEmoji.ifBlank { "SVG_BOLT" })
                            showAddPresetDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                ) {
                    Text("افزودن دکمه", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPresetDialog = false }) {
                    Text("انصراف", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }
}

@Composable
private fun NeedsReviewSmsCard(
    pending: BankSmsParsedResult,
    onConfirm: (SmsTransactionDirection, DetectedCurrencyUnit, Long?) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedReviewDirection by remember(pending.idempotencyKey) {
        mutableStateOf(
            if (pending.direction == SmsTransactionDirection.UNKNOWN) SmsTransactionDirection.WITHDRAWAL
            else pending.direction
        )
    }
    var selectedReviewCurrency by remember(pending.idempotencyKey) {
        mutableStateOf(
            if (pending.detectedCurrency == DetectedCurrencyUnit.UNKNOWN) DetectedCurrencyUnit.RIAL
            else pending.detectedCurrency
        )
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, AmberWarning, RoundedCornerShape(20.dp))
            .testTag("needs_review_sms_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = AmberWarning)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "پیامک نیازمند بررسی دستی (NEEDS_REVIEW)",
                    color = AmberWarning,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Text(
                text = "علت توقف ثبت خودکار: ${pending.reviewReasonFa ?: "ابهام در نوع تراکنش یا واحد پول"}",
                color = TextSecondary,
                fontSize = 12.sp
            )
            Text(
                text = "بانک: ${pending.bank.faName} • مبلغ استخراج‌شده: ${PersianUtils.formatToman(pending.amountToman)}",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    SmsTransactionDirection.WITHDRAWAL to "برداشت / هزینه",
                    SmsTransactionDirection.DEPOSIT to "واریز / درآمد",
                    SmsTransactionDirection.TRANSFER_CARD to "انتقال کارت"
                ).forEach { (dir, label) ->
                    val active = selectedReviewDirection == dir
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (active) AmberWarning else SurfaceCard)
                            .clickable { selectedReviewDirection = dir }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (active) Color.Black else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onConfirm(selectedReviewDirection, selectedReviewCurrency, pending.amountToman) },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("confirm_review_sms_btn")
                ) {
                    Text("تأیید و ثبت نهایی", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("reject_review_sms_btn")
                ) {
                    Text("رد پیامک", color = RoseDanger, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun TransactionRowCard(
    expense: ExpenseItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val badgeColor = when (expense.type) {
        TransactionType.INCOME -> EmeraldAccent
        TransactionType.TRANSFER -> CyanPrimary
        TransactionType.EXPENSE -> AmberWarning
    }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("expense_item_${expense.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(badgeColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (expense.type) {
                            TransactionType.TRANSFER -> Icons.Default.SwapHoriz
                            TransactionType.INCOME -> Icons.Default.TrendingUp
                            TransactionType.EXPENSE -> if (expense.isSmsParsed) Icons.Default.AccountBalance else Icons.Default.Wallet
                        },
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = expense.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = expense.type.titleFa,
                            fontSize = 10.sp,
                            color = badgeColor,
                            fontWeight = FontWeight.Bold
                        )
                        Text("•", color = TextMuted, fontSize = 10.sp)
                        Text(
                            text = expense.accountName ?: expense.bankName ?: "حساب پیش‌فرض",
                            fontSize = 10.sp,
                            color = CyanPrimary
                        )
                        Text("•", color = TextMuted, fontSize = 10.sp)
                        Text(text = expense.category, fontSize = 10.sp, color = TextMuted)
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = PersianUtils.formatToman(expense.amount),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = when (expense.type) {
                        TransactionType.INCOME -> EmeraldAccent
                        TransactionType.TRANSFER -> CyanPrimary
                        TransactionType.EXPENSE -> TextPrimary
                    }
                )
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "ویرایش", tint = CyanPrimary, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = TextMuted, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
