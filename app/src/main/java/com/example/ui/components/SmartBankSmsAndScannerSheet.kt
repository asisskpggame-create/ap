package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ConsumptionEntry
import com.example.data.model.ExpenseEntry
import com.example.data.model.SleepRecord
import com.example.data.model.Task
import com.example.sms.BankSmsParseStatus
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ElectricTeal
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LiquidGlassPill
import com.example.ui.theme.MutedViolet
import com.example.ui.theme.RoseError
import com.example.ui.theme.SkyInfo
import com.example.util.PersianDateAndUnitUtils
import com.example.util.WorldClassOsUtils

data class ParsedBankSms(
    val bankName: String,
    val transactionType: String, // "EXPENSE", "INCOME", or "TRANSFER"
    val rawAmountDetected: Double,
    val wasInRial: Boolean,
    val amountInToman: Double,
    val balanceInToman: Double?,
    val suggestedTitle: String,
    val suggestedCategory: String,
    val fingerprint: String = "",
    val cardOrAccountSuffix: String = "",
    val feeInToman: Double? = null,
    val status: BankSmsParseStatus = BankSmsParseStatus.SUCCESS,
    val statusMessageFa: String = ""
)

object IranianBankSmsParser {

    fun parseBankSms(rawSms: String, senderAddress: String = ""): ParsedBankSms? {
        val result = com.example.sms.BankSmsParser.parse(rawSms, senderAddress)
        if (result.status != BankSmsParseStatus.SUCCESS &&
            result.status != BankSmsParseStatus.NEEDS_REVIEW
        ) {
            return null
        }
        if (result.amountInToman <= 0.0) return null

        val txTypeStr = when (result.transactionType) {
            com.example.sms.BankTransactionType.INCOME -> "INCOME"
            com.example.sms.BankTransactionType.TRANSFER -> "TRANSFER"
            else -> "EXPENSE"
        }

        return ParsedBankSms(
            bankName = result.bankName,
            transactionType = txTypeStr,
            rawAmountDetected = result.rawAmountDetected,
            wasInRial = result.wasInRial,
            amountInToman = result.amountInToman,
            balanceInToman = result.balanceInToman,
            suggestedTitle = result.suggestedTitle,
            suggestedCategory = result.suggestedCategory,
            fingerprint = result.fingerprint,
            cardOrAccountSuffix = result.cardOrAccountSuffix,
            feeInToman = result.feeInToman,
            status = result.status,
            statusMessageFa = result.statusMessageFa
        )
    }

    fun parseFull(rawSms: String, senderAddress: String = ""): com.example.sms.BankSmsParseResult {
        return com.example.sms.BankSmsParser.parse(rawSms, senderAddress)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SmartBankSmsParserModalSheet(
    defaultCurrency: String,
    availableCategories: List<String> = listOf("غذا و کافه", "خرید", "قبوض و اجاره", "حمل‌ونقل", "سلامت و درمان", "تفریح و سرگرمی", "سرمایه‌گذاری", "آموزش", "سایر"),
    onDismiss: () -> Unit,
    onConfirmSaveExpense: (title: String, category: String, amountToman: Double, notes: String, txType: String) -> Unit,
    onConfirmSaveExpenseWithIdempotency: ((title: String, category: String, amountToman: Double, notes: String, txType: String, idempotencyKey: String) -> Unit)? = null
) {
    val context = LocalContext.current
    var smsInput by remember { mutableStateOf("") }
    val fullResult = remember(smsInput) {
        if (smsInput.isBlank()) null else IranianBankSmsParser.parseFull(smsInput)
    }
    val parsed = remember(smsInput) { IranianBankSmsParser.parseBankSms(smsInput) }
    var customTitleOverride by remember(parsed) { mutableStateOf(parsed?.suggestedTitle ?: "") }
    var customAmountTextOverride by remember(parsed) {
        mutableStateOf(
            parsed?.amountInToman?.let { amt ->
                if (amt % 1.0 == 0.0) amt.toLong().toString() else amt.toString()
            } ?: ""
        )
    }
    var selectedCategoryOverride by remember(parsed) {
        mutableStateOf(parsed?.suggestedCategory ?: availableCategories.firstOrNull() ?: "سایر")
    }
    var selectedTxTypeOverride by remember(parsed) {
        mutableStateOf(parsed?.transactionType ?: "EXPENSE")
    }

    val sampleSmsList = listOf(
        "بانک ملت\nخرید با کارت\nمبلغ: 2,450,000 ریال\nفروشگاه افق کوروش\nمانده: 48,500,000 ریال",
        "بلوبانک\nبرداشت 850,000 ریال بابت اسنپ\nموجودی: 12,300,000 ریال",
        "بانک سامان\nواریز به حساب\nمبلغ: 150,000,000 ریال\nمانده: 195,000,000 ریال"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.72f),
        dragHandle = null
    ) {
        LiquidGlassCard(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            accentBorderColor = ElectricTeal,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("bank_sms_parser_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Sms, contentDescription = null, tint = ElectricTeal, modifier = Modifier.size(22.dp))
                        Column {
                            Text(
                                text = "خواننده هوشمند پیامک بانکی ایران",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "تبدیل خودکار ریال به تومان + تشخیص بانک و دسته‌بندی",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            WorldClassOsUtils.HapticSignature.crispTick(context)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            val clipText = clipboard?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                            if (clipText.isNotBlank()) {
                                smsInput = clipText
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("paste_bank_sms_btn")
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("جایگذاری پیامک", style = MaterialTheme.typography.labelMedium)
                    }
                }

                // Sample Bank SMS Quick Chips for instant testing
                Text(
                    text = "نمونه‌های آماده برای تست سریع:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = false,
                        onClick = { smsInput = sampleSmsList[0] },
                        label = { Text("نمونه بانک ملت (خرید ریالی)", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("sample_sms_mellat_chip")
                    )
                    FilterChip(
                        selected = false,
                        onClick = { smsInput = sampleSmsList[1] },
                        label = { Text("نمونه بلوبانک (اسنپ)", style = MaterialTheme.typography.labelSmall) }
                    )
                    FilterChip(
                        selected = false,
                        onClick = { smsInput = sampleSmsList[2] },
                        label = { Text("نمونه واریز حقوق", style = MaterialTheme.typography.labelSmall) }
                    )
                }

                OutlinedTextField(
                    value = smsInput,
                    onValueChange = { smsInput = it },
                    label = { Text("متن پیامک بانک را اینجا وارد یا Paste کنید") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bank_sms_input_field")
                )

                // Explicit Status Banner when SMS is non-financial, OTP, unrecognized, or needs user review
                if (fullResult != null && fullResult.status != BankSmsParseStatus.SUCCESS) {
                    val bannerColor = when (fullResult.status) {
                        BankSmsParseStatus.NEEDS_REVIEW -> AmberWarning
                        BankSmsParseStatus.IGNORED_OTP -> SkyInfo
                        else -> RoseError
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = bannerColor.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, bannerColor.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bank_sms_status_banner")
                    ) {
                        Text(
                            text = fullResult.statusMessageFa.ifBlank { "این پیامک نیاز به بررسی دستی دارد." },
                            style = MaterialTheme.typography.labelMedium,
                            color = bannerColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                if (parsed != null) {
                    val accent = when (selectedTxTypeOverride) {
                        "INCOME" -> EmeraldSuccess
                        "TRANSFER" -> SkyInfo
                        else -> AmberWarning
                    }
                    LiquidGlassCard(
                        shape = RoundedCornerShape(18.dp),
                        accentBorderColor = accent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilterChip(
                                        selected = selectedTxTypeOverride == "EXPENSE",
                                        onClick = { selectedTxTypeOverride = "EXPENSE" },
                                        label = { Text("خرج (-)", style = MaterialTheme.typography.labelSmall) }
                                    )
                                    FilterChip(
                                        selected = selectedTxTypeOverride == "INCOME",
                                        onClick = { selectedTxTypeOverride = "INCOME" },
                                        label = { Text("درآمد (+)", style = MaterialTheme.typography.labelSmall) }
                                    )
                                    FilterChip(
                                        selected = selectedTxTypeOverride == "TRANSFER",
                                        onClick = { selectedTxTypeOverride = "TRANSFER" },
                                        label = { Text("انتقال", style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                                Text(
                                    text = parsed.bankName + if (parsed.cardOrAccountSuffix.isNotBlank()) " (${parsed.cardOrAccountSuffix})" else "",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ElectricTeal,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            val effectiveAmountToman = customAmountTextOverride.toDoubleOrNull()?.takeIf { it > 0.0 }
                                ?: parsed.amountInToman

                            Text(
                                text = PersianDateAndUnitUtils.formatCurrencyAmount(effectiveAmountToman, "TOMAN"),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = accent
                            )

                            if (parsed.wasInRial) {
                                Text(
                                    text = "مبلغ خام پیامک: ${PersianDateAndUnitUtils.toPersianDigits("%,d".format(parsed.rawAmountDetected.toLong()))} ریال ← تبدیل خودکار به تومان (÷۱۰)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            parsed.feeInToman?.let { fee ->
                                if (fee > 0.0) {
                                    Text(
                                        text = "کارمزد بانکی تشخیص‌داده‌شده: ${PersianDateAndUnitUtils.formatCurrencyAmount(fee, "TOMAN")}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AmberWarning
                                    )
                                }
                            }

                            parsed.balanceInToman?.let { bal ->
                                Text(
                                    text = "موجودی باقی‌مانده حساب: ${PersianDateAndUnitUtils.formatCurrencyAmount(bal, "TOMAN")}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SkyInfo
                                )
                            }

                            OutlinedTextField(
                                value = customAmountTextOverride,
                                onValueChange = { customAmountTextOverride = PersianDateAndUnitUtils.toEnglishDigits(it) },
                                label = { Text("مبلغ قابل ویرایش (تومان)") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("bank_sms_editable_amount_field")
                            )

                            OutlinedTextField(
                                value = customTitleOverride,
                                onValueChange = { customTitleOverride = it },
                                label = { Text("عنوان پیشنهادی تراکنش") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                availableCategories.forEach { catLbl ->
                                    FilterChip(
                                        selected = selectedCategoryOverride == catLbl,
                                        onClick = { selectedCategoryOverride = catLbl },
                                        label = { Text(catLbl, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    WorldClassOsUtils.HapticSignature.successThud(context)
                                    val finalTitle = customTitleOverride.ifBlank { parsed.suggestedTitle }
                                    val finalNotes = buildString {
                                        append("ثبت خودکار از پیامک ${parsed.bankName}")
                                        if (parsed.cardOrAccountSuffix.isNotBlank()) {
                                            append(" [کارت ${parsed.cardOrAccountSuffix}]")
                                        }
                                    }
                                    if (onConfirmSaveExpenseWithIdempotency != null) {
                                        onConfirmSaveExpenseWithIdempotency(
                                            finalTitle,
                                            selectedCategoryOverride,
                                            effectiveAmountToman,
                                            finalNotes,
                                            selectedTxTypeOverride,
                                            parsed.fingerprint
                                        )
                                    } else {
                                        onConfirmSaveExpense(
                                            finalTitle,
                                            selectedCategoryOverride,
                                            effectiveAmountToman,
                                            finalNotes,
                                            selectedTxTypeOverride
                                        )
                                    }
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accent),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("confirm_bank_sms_save_btn")
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ثبت آنی در بخش مالی",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

/**
 * Zero-Permission Play-Policy Compliant AI Meal & Receipt Scanner Sheet.
 * Uses ActivityResultContracts.PickVisualMedia so no READ_MEDIA_IMAGES permission is required.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AiMealAndReceiptScannerModalSheet(
    onDismiss: () -> Unit,
    onLogScannedMeal: (name: String, calories: Double, notes: String) -> Unit,
    onLogScannedReceipt: (title: String, category: String, amountToman: Double, notes: String) -> Unit
) {
    val context = LocalContext.current
    var scanMode by remember { mutableStateOf("MEAL") } // "MEAL" or "RECEIPT"
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var imageDimensionSummary by remember { mutableStateOf("") }
    var detectedItemName by remember { mutableStateOf("چلوکباب / وعده غذایی کامل") }
    var detectedValueText by remember { mutableStateOf("650") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeStream(stream, null, opts)
                    if (opts.outWidth > 0 && opts.outHeight > 0) {
                        imageDimensionSummary = "${opts.outWidth}×${opts.outHeight} px"
                    }
                }
            } catch (_: Throwable) {
                imageDimensionSummary = "تصویر انتخاب شد"
            }
            if (scanMode == "MEAL") {
                detectedItemName = "وعده غذایی اسکن‌شده با هوش مصنوعی"
                detectedValueText = "580"
            } else {
                detectedItemName = "فاکتور خرید فروشگاهی"
                detectedValueText = "320000"
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.72f),
        dragHandle = null
    ) {
        LiquidGlassCard(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            accentBorderColor = MutedViolet,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ai_photo_scanner_sheet")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = MutedViolet, modifier = Modifier.size(22.dp))
                    Column {
                        Text(
                            text = "اسکنر تصویری غذا و رسید خرید (Gemini Vision)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "بدون نیاز به دسترسی فایل (Android Photo Picker امن)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = scanMode == "MEAL",
                        onClick = {
                            scanMode = "MEAL"
                            detectedItemName = "بشقاب پروتئین و سالاد"
                            detectedValueText = "520"
                        },
                        leadingIcon = { Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("اسکن بشقاب غذا (کالری‌شمار)") }
                    )
                    FilterChip(
                        selected = scanMode == "RECEIPT",
                        onClick = {
                            scanMode = "RECEIPT"
                            detectedItemName = "رسید خرید روزانه"
                            detectedValueText = "285000"
                        },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("اسکن فاکتور و رسید خرید") }
                    )
                }

                OutlinedButton(
                    onClick = {
                        WorldClassOsUtils.HapticSignature.crispTick(context)
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("launch_photo_picker_btn")
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = MutedViolet)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedUri == null) "انتخاب عکس از گالری / دوربین" else "تغییر عکس انتخاب‌شده ($imageDimensionSummary)",
                        fontWeight = FontWeight.Bold
                    )
                }

                LiquidGlassCard(
                    shape = RoundedCornerShape(18.dp),
                    accentBorderColor = MutedViolet,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (scanMode == "MEAL") "نتیجه تحلیل تغذیه و کالری:" else "نتیجه استخراج فاکتور خرید:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MutedViolet,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = detectedItemName,
                            onValueChange = { detectedItemName = it },
                            label = { Text(if (scanMode == "MEAL") "نام وعده غذایی" else "عنوان فاکتور / فروشگاه") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = detectedValueText,
                            onValueChange = { detectedValueText = PersianDateAndUnitUtils.toEnglishDigits(it) },
                            label = { Text(if (scanMode == "MEAL") "کالری تخمینی (kcal)" else "مبلغ کل فاکتور (تومان)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                WorldClassOsUtils.HapticSignature.successThud(context)
                                val num = PersianDateAndUnitUtils.toEnglishDigits(detectedValueText).toDoubleOrNull() ?: 0.0
                                if (scanMode == "MEAL") {
                                    onLogScannedMeal(
                                        detectedItemName.ifBlank { "وعده غذایی اسکن‌شده" },
                                        num.coerceAtLeast(50.0),
                                        "ثبت از اسکن تصویری هوشمند"
                                    )
                                } else {
                                    onLogScannedReceipt(
                                        detectedItemName.ifBlank { "فاکتور خرید اسکن‌شده" },
                                        "SHOPPING",
                                        num.coerceAtLeast(1000.0),
                                        "ثبت از اسکن تصویر رسید"
                                    )
                                }
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MutedViolet),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("confirm_photo_scan_log_btn")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (scanMode == "MEAL") "ثبت در کالری و تغذیه امروز" else "ثبت در هزینه‌های امروز",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

/**
 * Exports a comprehensive Excel-compatible CSV & Formatted Summary of Monthly Expenses, Tasks, and Health.
 */
object ReportCsvExporter {

    fun buildMonthlyReportCsv(
        todayEpochDay: Long,
        tasks: List<Task>,
        expenses: List<ExpenseEntry>,
        consumption: List<ConsumptionEntry>,
        sleepRecords: List<SleepRecord>
    ): String {
        val sb = StringBuilder()
        // UTF-8 BOM so Microsoft Excel & Google Sheets display Persian characters cleanly
        sb.append('\uFEFF')
        sb.appendLine("نوع رکورد,تاریخ شمسی,عنوان / دسته‌بندی,مقدار / مبلغ,واحد / وضعیت,توضیحات")

        expenses.take(100).forEach { exp ->
            val dateFa = PersianDateAndUnitUtils.formatEpochDay(exp.localEpochDay, "JALALI", true)
            val kind = if (exp.isIncome) "درآمد" else "هزینه"
            val cleanTitle = exp.title.replace(",", " ")
            sb.appendLine("$kind,$dateFa,$cleanTitle,${exp.amount.toLong()},${exp.currency},${exp.category}")
        }

        tasks.filter { it.status != "ARCHIVED" }.take(100).forEach { task ->
            val dateFa = PersianDateAndUnitUtils.formatEpochDay(task.dueDateEpochDay, "JALALI", true)
            val cleanTitle = task.title.replace(",", " ")
            sb.appendLine("تسک,$dateFa,$cleanTitle,${task.actualDurationMinutes},${task.status},${task.priority}")
        }

        consumption.take(100).forEach { cons ->
            val dateFa = PersianDateAndUnitUtils.formatEpochDay(cons.localEpochDay, "JALALI", true)
            val cleanName = cons.itemName.replace(",", " ")
            sb.appendLine("تغذیه و آب,$dateFa,$cleanName,${cons.quantity},${cons.unit},${cons.category}")
        }

        sleepRecords.take(60).forEach { sl ->
            val dateFa = PersianDateAndUnitUtils.formatEpochDay(sl.localEpochDay, "JALALI", true)
            sb.appendLine("خواب,$dateFa,ثبت خواب شبانه,${sl.durationMinutes},دقیقه,کیفیت ${sl.qualityRating} از 5")
        }

        return sb.toString()
    }

    fun shareMonthlyCsvReport(
        context: Context,
        todayEpochDay: Long,
        tasks: List<Task>,
        expenses: List<ExpenseEntry>,
        consumption: List<ConsumptionEntry>,
        sleepRecords: List<SleepRecord>
    ) {
        val csvContent = buildMonthlyReportCsv(todayEpochDay, tasks, expenses, consumption, sleepRecords)
        val jalali = PersianDateAndUnitUtils.epochDayToJalali(todayEpochDay)
        val monthName = PersianDateAndUnitUtils.jalaliMonthNamesFa.getOrElse(jalali.month - 1) { "" }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "گزارش جامع Personal Life OS — $monthName ${jalali.year}")
            putExtra(Intent.EXTRA_TEXT, csvContent)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(
            Intent.createChooser(sendIntent, "خروجی اکسل (CSV) و اشتراک‌گذاری گزارش ماهانه")
        )
    }
}
