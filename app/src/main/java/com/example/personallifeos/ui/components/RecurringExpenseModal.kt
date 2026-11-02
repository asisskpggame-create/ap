package com.example.personallifeos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tag
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.personallifeos.data.model.RecurringExpenseItem
import com.example.personallifeos.data.model.RecurringFrequency
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

/**
 * Modal component for defining recurring & scheduled expenses with amount, category,
 * frequency selector (daily, weekly, monthly), due date, and notes.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecurringExpenseModal(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onSaveRecurringExpense: (
        title: String,
        amount: Long,
        category: String,
        frequency: RecurringFrequency,
        nextDueDate: String,
        note: String
    ) -> Unit,
    initialItem: RecurringExpenseItem? = null
) {
    if (!isOpen) return

    var title by remember(initialItem) { mutableStateOf(initialItem?.title ?: "") }
    var amountText by remember(initialItem) { mutableStateOf(initialItem?.amount?.toString() ?: "") }
    var selectedFrequency by remember(initialItem) { mutableStateOf(initialItem?.frequency ?: RecurringFrequency.MONTHLY) }
    var selectedCategory by remember(initialItem) { mutableStateOf(initialItem?.category ?: "قبوض و اینترنت") }
    var nextDueDate by remember(initialItem) { mutableStateOf(initialItem?.nextDueDate ?: "۱۴۰۵/۰۸/۲۵") }
    var note by remember(initialItem) { mutableStateOf(initialItem?.note ?: "") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val presetCategories = listOf(
        "قبوض و اینترنت",
        "مسکن و اجاره",
        "قسط و بدهی",
        "بیمه و سلامت",
        "ورزش و تندرستی",
        "آموزش و یادگیری",
        "سوپرمارکت و روزمره",
        "شارژ ساختمان",
        "اشتراک نرم‌افزار",
        "سایر هزینه‌ها"
    )

    val quickAmounts = listOf(
        100_000L to "۱۰۰ هزار",
        300_000L to "۳۰۰ هزار",
        500_000L to "۵۰۰ هزار",
        1_000_000L to "۱ میلیون",
        2_500_000L to "۲.۵ میلیون",
        5_000_000L to "۵ میلیون"
    )

    val parsedAmount = remember(amountText) {
        PersianUtils.normalizePersianText(amountText).filter { it.isDigit() }.toLongOrNull() ?: 0L
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .clip(RoundedCornerShape(26.dp))
                    .border(1.dp, CyanPrimary.copy(alpha = 0.4f), RoundedCornerShape(26.dp))
                    .testTag("recurring_expense_modal"),
                color = SurfaceDark,
                tonalElevation = 10.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
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
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(CyanPrimary.copy(alpha = 0.25f), EmeraldAccent.copy(alpha = 0.25f))
                                        )
                                    )
                                    .border(1.dp, CyanPrimary.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Autorenew,
                                    contentDescription = null,
                                    tint = CyanPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "تعریف هزینه دوره‌ای و زمان‌بندی‌شده",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "مدیریت خودکار قبوض، اشتراک‌ها، اقساط و هزینه‌های تکرارشونده",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_recurring_expense_modal_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "بستن",
                                tint = TextSecondary
                            )
                        }
                    }

                    // Title Input
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "عنوان هزینه دوره‌ای *",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        OutlinedTextField(
                            value = title,
                            onValueChange = {
                                title = it
                                validationError = null
                            },
                            placeholder = { Text("مثلاً: اشتراک اینترنت فیبر نوری، اجاره خانه، قسط وام...", fontSize = 12.sp, color = TextMuted) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("recurring_title_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceCard,
                                unfocusedContainerColor = SurfaceCard,
                                focusedBorderColor = CyanPrimary,
                                unfocusedBorderColor = Color(0x33FFFFFF)
                            )
                        )
                    }

                    // Amount Input & Live Preview
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مبلغ به تومان *",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            if (parsedAmount > 0) {
                                Text(
                                    text = PersianUtils.formatToman(parsedAmount),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldAccent
                                )
                            }
                        }

                        OutlinedTextField(
                            value = amountText,
                            onValueChange = {
                                amountText = it
                                validationError = null
                            },
                            placeholder = { Text("مثلاً: ۴۵۰,۰۰۰", fontSize = 12.sp, color = TextMuted) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("recurring_amount_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceCard,
                                unfocusedContainerColor = SurfaceCard,
                                focusedBorderColor = CyanPrimary,
                                unfocusedBorderColor = Color(0x33FFFFFF)
                            )
                        )

                        // Quick Amount Chips
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(quickAmounts) { (amt, label) ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SurfaceCard)
                                        .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(10.dp))
                                        .clickable {
                                            amountText = amt.toString()
                                            validationError = null
                                        }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                        .testTag("quick_amount_chip_$amt")
                                ) {
                                    Text(label, fontSize = 11.sp, color = CyanPrimary)
                                }
                            }
                        }
                    }

                    // Frequency Selector (Daily, Weekly, Monthly)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "بازه تکرار و زمان‌بندی (Frequency) *",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("frequency_selector"),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RecurringFrequency.entries.forEach { freq ->
                                val isSelected = selectedFrequency == freq
                                val testTagKey = when (freq) {
                                    RecurringFrequency.DAILY -> "frequency_daily"
                                    RecurringFrequency.WEEKLY -> "frequency_weekly"
                                    RecurringFrequency.MONTHLY -> "frequency_monthly"
                                }

                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .border(
                                            width = if (isSelected) 1.5.dp else 0.5.dp,
                                            color = if (isSelected) CyanPrimary else Color(0x26FFFFFF),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable {
                                            selectedFrequency = freq
                                            // auto suggest due date
                                            nextDueDate = when (freq) {
                                                RecurringFrequency.DAILY -> "فردا • ساعت ۰۹:۰۰"
                                                RecurringFrequency.WEEKLY -> "۱۴۰۵/۰۸/۱۲ (هفته آینده)"
                                                RecurringFrequency.MONTHLY -> "۱۴۰۵/۰۸/۲۵"
                                            }
                                        }
                                        .testTag(testTagKey),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) CyanPrimary.copy(alpha = 0.15f) else SurfaceCard
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 10.dp, horizontal = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = CyanPrimary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                            Text(
                                                text = freq.titleFa,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) CyanPrimary else TextPrimary
                                            )
                                        }
                                        Text(
                                            text = freq.subtitleFa,
                                            fontSize = 10.sp,
                                            color = if (isSelected) TextPrimary else TextMuted
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Category Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "دسته‌بندی هزینه *",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            presetCategories.forEachIndexed { index, cat ->
                                val isSelected = selectedCategory == cat
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) EmeraldAccent else SurfaceCard)
                                        .border(
                                            width = if (isSelected) 1.dp else 0.5.dp,
                                            color = if (isSelected) EmeraldAccent else Color(0x26FFFFFF),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedCategory = cat }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                        .testTag("category_chip_$index")
                                ) {
                                    Text(
                                        text = cat,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.Black else TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Next Due Date Input
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "تاریخ اولین یا نزدیک‌ترین موعد سررسید",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        OutlinedTextField(
                            value = nextDueDate,
                            onValueChange = { nextDueDate = it },
                            placeholder = { Text("مثلاً: ۱۴۰۵/۰۸/۲۵", fontSize = 12.sp, color = TextMuted) },
                            leadingIcon = {
                                Icon(Icons.Default.DateRange, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(18.dp))
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("recurring_due_date_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceCard,
                                unfocusedContainerColor = SurfaceCard,
                                focusedBorderColor = CyanPrimary,
                                unfocusedBorderColor = Color(0x33FFFFFF)
                            )
                        )
                    }

                    // Note Input (Optional)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "توضیحات یا یادداشت (اختیاری)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            placeholder = { Text("مثلاً: پرداخت از کارت بلوبانک یا شماره حساب...", fontSize = 12.sp, color = TextMuted) },
                            leadingIcon = {
                                Icon(Icons.Default.Notes, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(18.dp))
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("recurring_note_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceCard,
                                unfocusedContainerColor = SurfaceCard,
                                focusedBorderColor = CyanPrimary,
                                unfocusedBorderColor = Color(0x33FFFFFF)
                            )
                        )
                    }

                    // Error banner
                    validationError?.let { err ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(RoseDanger.copy(alpha = 0.15f))
                                .border(1.dp, RoseDanger.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Text(text = err, fontSize = 11.sp, color = RoseDanger, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("cancel_recurring_expense_btn"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("انصراف", color = TextSecondary, fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                if (title.isBlank()) {
                                    validationError = "لطفاً عنوان هزینه دوره‌ای را وارد کنید."
                                    return@Button
                                }
                                if (parsedAmount <= 0) {
                                    validationError = "لطفاً مبلغ معتبری به تومان وارد کنید."
                                    return@Button
                                }
                                onSaveRecurringExpense(
                                    title.trim(),
                                    parsedAmount,
                                    selectedCategory.trim(),
                                    selectedFrequency,
                                    nextDueDate.trim(),
                                    note.trim()
                                )
                                onDismiss()
                            },
                            modifier = Modifier
                                .weight(1.5f)
                                .height(46.dp)
                                .testTag("save_recurring_expense_btn"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanPrimary,
                                contentColor = Color.Black
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ثبت هزینه زمان‌بندی‌شده",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
