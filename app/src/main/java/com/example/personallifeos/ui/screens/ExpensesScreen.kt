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
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personallifeos.data.model.ExpenseItem
import com.example.personallifeos.data.model.TransactionType
import com.example.personallifeos.sms.BankSmsParsedResult
import com.example.personallifeos.sms.BankUserConfig
import com.example.personallifeos.sms.DetectedCurrencyUnit
import com.example.personallifeos.sms.IranianBankSmsEngine
import com.example.personallifeos.sms.SmsTransactionDirection
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
import com.example.personallifeos.util.PersianUtils

@Composable
fun ExpensesScreen(
    expenses: List<ExpenseItem>,
    configuredBanks: List<BankUserConfig> = emptyList(),
    pendingReviewSms: BankSmsParsedResult? = null,
    onAddExpense: (String, Long, String, String?, Boolean) -> Unit,
    onDeleteExpense: (String) -> Unit,
    onParseSms: (String, String?) -> String,
    onConfirmReview: (SmsTransactionDirection, DetectedCurrencyUnit, Long?) -> String = { _, _, _ -> "" },
    onDismissReview: () -> Unit = {},
    onOpenBankSelector: () -> Unit = {},
    onOpenNotificationCenter: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var smsInput by remember { mutableStateOf("") }
    var senderInput by remember { mutableStateOf("") }
    var smsFeedback by remember { mutableStateOf<String?>(null) }

    var manualTitle by remember { mutableStateOf("") }
    var manualAmount by remember { mutableStateOf("") }
    var manualCategory by remember { mutableStateOf("خرید عمومی") }

    val totalSpent = expenses.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("expenses_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Spending Overview Hero
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
                                listOf(Color(0xFF0F172A), Color(0xFF2E1A0A))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مجموع مخارج ماه آبان",
                                color = AmberWarning,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(Color(0x26F59E0B), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("نسخه 1.6.0R ممیزی‌شده", color = AmberWarning, fontSize = 11.sp)
                            }
                        }

                        Text(
                            text = PersianUtils.formatToman(totalSpent),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "تعداد کل تراکنش‌های ثبت شده: ${PersianUtils.formatPersianNumber(expenses.size.toLong())} مورد",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF1E6F30))
                                        .clickable { onOpenBankSelector() }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
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
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                        .testTag("open_notif_center_from_expenses_btn")
                                ) {
                                    Text("فیش‌خوان زنده", color = CyanPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Configured Banks Horizontal Bar (Mirza-inspired)
        if (configuredBanks.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "بانک‌ها و سرشماره‌های فعال (${PersianUtils.formatPersianNumber(configuredBanks.size.toLong())})",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "+ مدیریت / افزودن بانک جدید",
                            color = CyanPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onOpenBankSelector() }
                        )
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(configuredBanks, key = { it.bank.name }) { cfg ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF0A2540), Color(0xFF153E6B))
                                        )
                                    )
                                    .border(1.dp, CyanPrimary.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                                    .clickable { senderInput = cfg.senderNumber }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = cfg.bank.faName,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "سرشماره: ${cfg.senderNumber}",
                                        color = Color(0xFF93C5FD),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Audited Manual Review Card for NEEDS_REVIEW SMS (Audit Fix #1)
        pendingReviewSms?.let { pending ->
            item {
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
                                onClick = {
                                    smsFeedback = onConfirmReview(selectedReviewDirection, selectedReviewCurrency, pending.amountToman)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("confirm_review_sms_btn")
                            ) {
                                Text("تأیید و ثبت نهایی", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    onDismissReview()
                                    smsFeedback = "پیامک مبهم توسط کاربر رد شد."
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("reject_review_sms_btn")
                            ) {
                                Text("رد پیامک", color = RoseDanger, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Bank SMS Parser Card
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
                                text = "اسکنر هوشمند پیامک بانکی (موتور ممیزی‌شده)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "پشتیبانی از اعتبارسنجی سطح قالب، سرشماره فرستنده و تشخیص ابهام:",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }

                    // Bank sample chips for 1-tap testing
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("الگوهای شبکه بانکی (تست با ۱ لمس):", fontSize = 11.sp, color = TextMuted)
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

                    OutlinedTextField(
                        value = senderInput,
                        onValueChange = { senderInput = it },
                        placeholder = {
                            Text("سرشماره فرستنده (اختیاری - مثلاً 98300017)", color = TextMuted, fontSize = 12.sp)
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sms_sender_field"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = Color(0x26FFFFFF)
                        )
                    )

                    OutlinedTextField(
                        value = smsInput,
                        onValueChange = { smsInput = it },
                        placeholder = {
                            Text(
                                text = "مثلا: بانک ملت - برداشت خرید فروشگاه به مبلغ 850,000 ریال...",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sms_input_field"),
                        minLines = 3,
                        maxLines = 4,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = Color(0x26FFFFFF)
                        )
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
                        Text(
                            text = "تحلیل و ثبت خودکار تراکنش",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Manual Add Expense Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "ثبت دستی هزینه",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = manualTitle,
                            onValueChange = { manualTitle = it },
                            placeholder = { Text("عنوان (مثلا: رستوران)", fontSize = 12.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("manual_expense_title"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = manualAmount,
                            onValueChange = { manualAmount = it.filter { ch -> ch.isDigit() } },
                            placeholder = { Text("مبلغ (تومان)", fontSize = 12.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("manual_expense_amount"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Button(
                        onClick = {
                            val amountLong = manualAmount.toLongOrNull()
                            if (manualTitle.isNotBlank() && amountLong != null && amountLong > 0) {
                                onAddExpense(manualTitle, amountLong, manualCategory, null, false)
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
                        Text("افزودن هزینه", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Expenses List Title
        item {
            Text(
                text = "ریز تراکنش‌های ثبت شده",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        items(expenses, key = { it.id }) { expense ->
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (expense.isSmsParsed) Color(0x2606B6D4) else Color(0x26F59E0B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (expense.isSmsParsed) Icons.Default.AccountBalance else Icons.Default.Wallet,
                                contentDescription = null,
                                tint = if (expense.isSmsParsed) CyanPrimary else AmberWarning,
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
                                expense.bankName?.let { b ->
                                    Text(
                                        text = b,
                                        fontSize = 11.sp,
                                        color = CyanPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text("•", color = TextMuted, fontSize = 11.sp)
                                }
                                Text(
                                    text = expense.category,
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                                Text("•", color = TextMuted, fontSize = 11.sp)
                                Text(
                                    text = expense.date,
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = PersianUtils.formatToman(expense.amount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (expense.type == TransactionType.INCOME) EmeraldAccent else TextPrimary
                        )
                        IconButton(onClick = { onDeleteExpense(expense.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = TextMuted, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}
