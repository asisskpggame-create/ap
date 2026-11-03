package com.example.personallifeos.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.personallifeos.data.model.TransactionType
import com.example.personallifeos.sms.BankSmsParsedResult
import com.example.personallifeos.sms.BankType
import com.example.personallifeos.sms.IranianBankSmsEngine
import com.example.personallifeos.sms.SmsTransactionDirection
import com.example.personallifeos.sms.SmsVerificationStatus
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Model representing an extracted transaction message from device SMS.
 */
data class ScannedTransactionMessage(
    val id: String,
    val sender: String,
    val rawBody: String,
    val timestampMillis: Long,
    val dateStringFa: String,
    val bankName: String,
    val bankType: BankType,
    val amountToman: Long,
    val amountRial: Long,
    val direction: SmsTransactionDirection,
    val transactionType: TransactionType?, // STRICT: nullable when direction is UNKNOWN
    val title: String,
    val suggestedCategory: String,
    val accountOrCard: String?,
    val trackingCode: String?,
    val isFinancial: Boolean,
    val verificationStatus: SmsVerificationStatus,
    val idempotencyKey: String? = null
)

/**
 * Reads real SMS messages from the Android system inbox via ContentResolver if READ_SMS is granted.
 * Parses each message with the IranianBankSmsEngine to extract bank names, amounts, directions, and descriptions.
 * STRICT: NEVER supplies mock/fake transactions in production; returns empty list if inbox is empty.
 */
fun readDeviceTransactionSmsList(context: Context): List<ScannedTransactionMessage> {
    val resultList = mutableListOf<ScannedTransactionMessage>()
    val hasPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_SMS
    ) == PackageManager.PERMISSION_GRANTED

    if (hasPermission) {
        try {
            val uri = Uri.parse("content://sms/inbox")
            val projection = arrayOf("_id", "address", "body", "date")
            val cursor = context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "date DESC"
            )

            cursor?.use { c ->
                val idIdx = c.getColumnIndex("_id")
                val addressIdx = c.getColumnIndex("address")
                val bodyIdx = c.getColumnIndex("body")
                val dateIdx = c.getColumnIndex("date")

                var count = 0
                while (c.moveToNext() && count < 150) {
                    count++
                    val id = if (idIdx >= 0) c.getString(idIdx) ?: count.toString() else count.toString()
                    val address = if (addressIdx >= 0) c.getString(addressIdx) ?: "" else ""
                    val body = if (bodyIdx >= 0) c.getString(bodyIdx) ?: "" else ""
                    val dateMillis = if (dateIdx >= 0) c.getLong(dateIdx) else System.currentTimeMillis()

                    if (body.isBlank()) continue

                    // Parse SMS with the Iranian Bank SMS Engine
                    val parsed: BankSmsParsedResult = IranianBankSmsEngine.parse(body, address)
                    val isOtp = IranianBankSmsEngine.isOtpOrServiceSms(body) || parsed.isOtpOrServiceMessage
                    val isFailed = IranianBankSmsEngine.isFailedTransactionSms(body) || parsed.isFailedTransaction

                    // STRICT AUDIT REQUIREMENT 4: Discard OTP, service notifications, failed, and rejected messages
                    if (isOtp || isFailed || parsed.verificationStatus == SmsVerificationStatus.REJECTED) {
                        continue
                    }

                    // STRICT AUDIT REQUIREMENT 1: UNKNOWN direction must NEVER default to EXPENSE!
                    val txType: TransactionType? = when (parsed.direction) {
                        SmsTransactionDirection.DEPOSIT -> TransactionType.INCOME
                        SmsTransactionDirection.TRANSFER_POL,
                        SmsTransactionDirection.TRANSFER_PAYA,
                        SmsTransactionDirection.TRANSFER_CARD -> TransactionType.TRANSFER
                        SmsTransactionDirection.WITHDRAWAL,
                        SmsTransactionDirection.PURCHASE -> TransactionType.EXPENSE
                        SmsTransactionDirection.UNKNOWN -> null
                    }

                    val isFinancial = parsed.amountToman > 0 && !isOtp && !isFailed

                    val dateFa = try {
                        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
                        sdf.format(Date(dateMillis))
                    } catch (_: Exception) {
                        PersianUtils.getPersianDateString()
                    }

                    val autoTitle = when {
                        !parsed.merchantOrTerminal.isNullOrBlank() -> "خرید از ${parsed.merchantOrTerminal}"
                        !parsed.destination.isNullOrBlank() -> "انتقال به ${parsed.destination}"
                        parsed.direction.isExpense -> "برداشت از ${parsed.bank.shortNameFa}"
                        parsed.direction == SmsTransactionDirection.DEPOSIT -> "واریز به ${parsed.bank.shortNameFa}"
                        else -> "پیامک تراکنش ${parsed.bank.shortNameFa}"
                    }

                    val bankLabel = if (parsed.bank != BankType.UNKNOWN) parsed.bank.faName else "پیامک بانکی"

                    resultList.add(
                        ScannedTransactionMessage(
                            id = id,
                            sender = address,
                            rawBody = body,
                            timestampMillis = dateMillis,
                            dateStringFa = dateFa,
                            bankName = bankLabel,
                            bankType = parsed.bank,
                            amountToman = parsed.amountToman,
                            amountRial = parsed.amountRial,
                            direction = parsed.direction,
                            transactionType = txType,
                            title = autoTitle,
                            suggestedCategory = parsed.suggestedCategory,
                            accountOrCard = parsed.accountOrCard,
                            trackingCode = parsed.trackingCode,
                            isFinancial = isFinancial,
                            verificationStatus = parsed.verificationStatus,
                            idempotencyKey = parsed.idempotencyKey
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // Content resolver query failed or restricted
        }
    }

    // STRICT AUDIT REQUIREMENT 2: NEVER supply mock/fake transactions in production!
    return resultList
}

/**
 * SmsScanner Component
 * Accesses device SMS permissions (via Android API) to list recent transaction messages,
 * allowing the user to select specific messages to automatically extract bank names, amounts,
 * and descriptions, then passes this data to the 'handleAddExpense' handler.
 */
@Composable
fun SmsScanner(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    existingIdempotencyKeys: Set<String> = emptySet(),
    handleAddExpense: (
        title: String,
        amount: Long,
        category: String,
        bankName: String?,
        isSms: Boolean,
        type: TransactionType,
        accountName: String?,
        idempotencyKey: String?
    ) -> Unit
) {
    if (!isOpen) return

    val context = LocalContext.current
    var hasPermission by remember {
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
        hasPermission = isGranted
    }

    var scannedMessages by remember(hasPermission) {
        mutableStateOf(readDeviceTransactionSmsList(context))
    }

    LaunchedEffect(hasPermission) {
        scannedMessages = readDeviceTransactionSmsList(context)
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterIndex by remember { mutableStateOf(0) } // 0: All Financial, 1: Expenses/Withdrawal, 2: Incomes/Deposit, 3: All Messages
    val selectedMessageIds = remember { mutableStateListOf<String>() }
    val addedMessageIds = remember { mutableStateListOf<String>() }

    // Filter logic
    val filteredMessages = remember(scannedMessages, searchQuery, selectedFilterIndex) {
        scannedMessages.filter { msg ->
            val matchFilter = when (selectedFilterIndex) {
                0 -> msg.isFinancial
                1 -> msg.transactionType == TransactionType.EXPENSE && msg.isFinancial
                2 -> msg.transactionType == TransactionType.INCOME && msg.isFinancial
                3 -> true // All
                else -> true
            }

            val q = searchQuery.trim().lowercase()
            val matchQuery = if (q.isBlank()) true else {
                msg.title.lowercase().contains(q) ||
                msg.bankName.lowercase().contains(q) ||
                msg.rawBody.lowercase().contains(q) ||
                msg.amountToman.toString().contains(q) ||
                (msg.trackingCode?.contains(q) == true)
            }

            matchFilter && matchQuery
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(26.dp))
                .testTag("sms_scanner_dialog"),
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(CyanPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Sms,
                                contentDescription = null,
                                tint = CyanPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "اسکنر پیامک‌های بانکی (SmsScanner)",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "استخراج نام بانک، مبلغ و ثبت هوشمند در هزینه‌ها",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("sms_scanner_close_btn")) {
                        Icon(Icons.Default.Close, contentDescription = "بستن", tint = TextMuted)
                    }
                }

                if (!hasPermission) {
                    SmsScannerEmptyState(
                        hasPermission = false,
                        totalScannedCount = 0,
                        searchQuery = "",
                        onClearSearch = {},
                        onRequestPermission = { permissionLauncher.launch(Manifest.permission.READ_SMS) },
                        onOpenAppSettings = {
                            try {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        onRefresh = { scannedMessages = readDeviceTransactionSmsList(context) },
                        modifier = Modifier.weight(1f)
                    )
                } else {

                // Search Bar & Reload Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text("جستجو در پیامک‌ها (بانک، فروشگاه، مبلغ)...", fontSize = 12.sp, color = TextMuted)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "پاک کردن", tint = TextMuted, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceCard,
                            unfocusedContainerColor = SurfaceCard,
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("sms_scanner_search_input")
                    )

                    IconButton(
                        onClick = { scannedMessages = readDeviceTransactionSmsList(context) },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceCard)
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                            .testTag("refresh_sms_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "بروزرسانی پیامک‌ها", tint = CyanPrimary)
                    }
                }

                // Category Filter Chips
                val filterChips = listOf("تراکنش‌های مالی", "برداشت و خرید", "واریز", "همه پیامک‌ها")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filterChips.indices.toList()) { index ->
                        FilterChip(
                            selected = selectedFilterIndex == index,
                            onClick = { selectedFilterIndex = index },
                            label = { Text(filterChips[index], fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = CyanPrimary,
                                containerColor = SurfaceCard,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedFilterIndex == index,
                                selectedBorderColor = CyanPrimary,
                                borderColor = Color.White.copy(alpha = 0.1f)
                            )
                        )
                    }
                }

                // Multi-select Batch Action Bar (if items selected)
                AnimatedVisibility(visible = selectedMessageIds.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(CyanPrimary.copy(alpha = 0.15f))
                            .border(1.dp, CyanPrimary, RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${PersianUtils.formatPersianNumber(selectedMessageIds.size.toLong())} پیامک انتخاب شده",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { selectedMessageIds.clear() }) {
                                Text("لغو انتخاب", color = TextMuted, fontSize = 11.sp)
                            }
                            Button(
                                onClick = {
                                    val toAdd = scannedMessages.filter { selectedMessageIds.contains(it.id) }
                                    toAdd.forEach { msg ->
                                        val validTxType = msg.transactionType
                                        // STRICT AUDIT REQUIREMENT 1: Skip any item where direction is UNKNOWN
                                        if (validTxType != null) {
                                            handleAddExpense(
                                                msg.title,
                                                msg.amountToman,
                                                msg.suggestedCategory,
                                                msg.bankName,
                                                true,
                                                validTxType,
                                                msg.accountOrCard,
                                                msg.idempotencyKey
                                            )
                                            addedMessageIds.add(msg.id)
                                        }
                                    }
                                    selectedMessageIds.clear()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("batch_add_selected_sms_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("افزودن به هزینه‌ها", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // List of Scanned Transaction Messages
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("scanned_sms_lazy_list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (filteredMessages.isEmpty()) {
                        item {
                            SmsScannerEmptyState(
                                hasPermission = true,
                                totalScannedCount = scannedMessages.size,
                                searchQuery = searchQuery,
                                onClearSearch = {
                                    searchQuery = ""
                                    selectedFilterIndex = 0
                                },
                                onRequestPermission = { permissionLauncher.launch(Manifest.permission.READ_SMS) },
                                onOpenAppSettings = {
                                    try {
                                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.fromParts("package", context.packageName, null)
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                },
                                onRefresh = { scannedMessages = readDeviceTransactionSmsList(context) }
                            )
                        }
                    } else {
                        items(filteredMessages, key = { it.id }) { msg ->
                            val isSelected = selectedMessageIds.contains(msg.id)
                            val isAlreadyAdded = addedMessageIds.contains(msg.id) ||
                                (msg.idempotencyKey != null && (existingIdempotencyKeys.contains(msg.idempotencyKey) || IranianBankSmsEngine.isDuplicateKey(msg.idempotencyKey)))

                            TransactionMessageCard(
                                message = msg,
                                isSelected = isSelected,
                                isAlreadyAdded = isAlreadyAdded,
                                onToggleSelect = {
                                    if (isSelected) selectedMessageIds.remove(msg.id)
                                    else selectedMessageIds.add(msg.id)
                                },
                                onAddSingle = { chosenType ->
                                    handleAddExpense(
                                        msg.title,
                                        msg.amountToman,
                                        msg.suggestedCategory,
                                        msg.bankName,
                                        true,
                                        chosenType,
                                        msg.accountOrCard,
                                        msg.idempotencyKey
                                    )
                                    addedMessageIds.add(msg.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
}

/**
 * Backward-compatible 7-arg overload
 */
@Composable
fun SmsScanner(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    handleAddExpense: (
        title: String,
        amount: Long,
        category: String,
        bankName: String?,
        isSms: Boolean,
        type: TransactionType,
        accountName: String?
    ) -> Unit
) {
    SmsScanner(
        isOpen = isOpen,
        onDismiss = onDismiss,
        existingIdempotencyKeys = emptySet(),
        handleAddExpense = { title, amount, category, bankName, isSms, type, accountName, _ ->
            handleAddExpense(title, amount, category, bankName, isSms, type, accountName)
        }
    )
}

/**
 * Convenient overload matching standard 4-arg expense handler
 */
@Composable
fun SmsScanner(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    handleAddExpense: (title: String, amount: Long, category: String, type: TransactionType) -> Unit
) {
    SmsScanner(
        isOpen = isOpen,
        onDismiss = onDismiss,
        existingIdempotencyKeys = emptySet(),
        handleAddExpense = { title, amount, category, _, _, type, _, _ ->
            handleAddExpense(title, amount, category, type)
        }
    )
}

/**
 * Single Transaction Message Card displaying extracted bank, amount, description, and direct add action.
 */
@Composable
private fun TransactionMessageCard(
    message: ScannedTransactionMessage,
    isSelected: Boolean,
    isAlreadyAdded: Boolean,
    onToggleSelect: () -> Unit,
    onAddSingle: (TransactionType) -> Unit
) {
    var showRawSms by remember { mutableStateOf(false) }
    // STRICT AUDIT REQUIREMENT 1 & 5: When direction is UNKNOWN, require explicit selection!
    var chosenType by remember(message.id) { mutableStateOf(message.transactionType) }

    val dirColor = when (chosenType) {
        TransactionType.EXPENSE -> RoseDanger
        TransactionType.INCOME -> EmeraldAccent
        TransactionType.TRANSFER -> CyanPrimary
        null -> AmberWarning
    }

    val dirLabel = when (chosenType) {
        TransactionType.EXPENSE -> "برداشت / خرید"
        TransactionType.INCOME -> "واریز"
        TransactionType.TRANSFER -> "انتقال"
        null -> "جهت نامشخص ⚠️"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCard)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) CyanPrimary else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
            .testTag("sms_card_${message.id}")
    ) {
        // Top Row: Checkbox, Bank Badge, Direction Tag, Date
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    colors = CheckboxDefaults.colors(checkedColor = CyanPrimary)
                )

                // Bank Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F2942))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(13.dp))
                        Text(
                            text = message.bankName,
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Direction Tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(dirColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = dirLabel,
                        color = dirColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = message.dateStringFa,
                color = TextMuted,
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Middle Row: Amount & Extracted Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = message.title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                if (!message.accountOrCard.isNullOrBlank() || !message.trackingCode.isNullOrBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!message.accountOrCard.isNullOrBlank()) {
                            Text("کارت/حساب: ${message.accountOrCard}", color = TextSecondary, fontSize = 11.sp)
                        }
                        if (!message.trackingCode.isNullOrBlank()) {
                            Text("پیگیری: ${message.trackingCode}", color = TextMuted, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Amount Column
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${PersianUtils.formatPersianNumber(message.amountToman)} تومان",
                    color = dirColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                if (message.amountRial > 0) {
                    Text(
                        text = "${PersianUtils.formatPersianNumber(message.amountRial)} ریال",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // STRICT AUDIT REQUIREMENT 1: When direction is UNKNOWN, show direction selector chips
        if (chosenType == null) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0F1E33))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "⚠️ جهت تراکنش در پیامک تصریح نشده است؛ لطفاً تعیین کنید:",
                    color = AmberWarning,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        TransactionType.EXPENSE to "برداشت / هزینه",
                        TransactionType.INCOME to "واریز / درآمد",
                        TransactionType.TRANSFER to "انتقال بین حساب‌ها"
                    ).forEach { (t, lbl) ->
                        val active = chosenType == t
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) CyanPrimary else SurfaceCard)
                                .border(1.dp, if (active) CyanPrimary else Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .clickable { chosenType = t }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = lbl,
                                color = if (active) Color.Black else TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bottom Actions: Toggle raw text & Add Single Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (showRawSms) "بستن متن پیامک ▴" else "مشاهده متن پیامک ▾",
                color = CyanPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { showRawSms = !showRawSms }
                    .padding(vertical = 4.dp)
            )

            if (isAlreadyAdded) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(EmeraldAccent.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(14.dp))
                        Text("ثبت شد", color = EmeraldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Button(
                    onClick = { chosenType?.let { onAddSingle(it) } },
                    enabled = chosenType != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (chosenType) {
                            TransactionType.INCOME -> EmeraldAccent
                            TransactionType.TRANSFER -> CyanPrimary
                            TransactionType.EXPENSE -> CyanPrimary
                            null -> Color.Gray.copy(alpha = 0.3f)
                        },
                        disabledContainerColor = Color.Gray.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(36.dp).testTag("add_sms_single_btn_${message.id}")
                ) {
                    val btnText = when (chosenType) {
                        TransactionType.INCOME -> "افزودن به درآمدها"
                        TransactionType.TRANSFER -> "ثبت به عنوان انتقال"
                        TransactionType.EXPENSE -> "افزودن به هزینه‌ها"
                        null -> "تعیین جهت الزامی است"
                    }
                    Icon(Icons.Default.Add, contentDescription = null, tint = if (chosenType != null) Color.Black else TextMuted, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(btnText, color = if (chosenType != null) Color.Black else TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Expandable Raw SMS Text Body
        AnimatedVisibility(visible = showRawSms) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .padding(10.dp)
            ) {
                Text("سرشماره: ${message.sender}", color = CyanPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(message.rawBody, color = TextSecondary, fontSize = 11.sp, lineHeight = 16.sp)
            }
        }
    }
}

/**
 * Dedicated Empty State for the SMS Scanner Component.
 * STRICT: Provides clear, honest feedback when permission is denied, inbox is empty,
 * or no results match the filter, WITHOUT ever displaying fake or mock transactions.
 */
@Composable
fun SmsScannerEmptyState(
    hasPermission: Boolean,
    totalScannedCount: Int,
    searchQuery: String,
    onClearSearch: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            when {
                !hasPermission -> {
                    // State 1: Permission Denied / Not Granted
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(AmberWarning.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = AmberWarning,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Text(
                        text = "دسترسی به پیامک‌های دستگاه مسدود است",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "برای خواندن مستقیم و استخراج مبالغ تراکنش‌ها، برنامه نیازمند مجوز خواندن پیامک (READ_SMS) اندروید است.\n\nبه دلیل حفظ سلامت و واقعی بودن گزارش‌های مالی شما، هیچ تراکنش ساختگی یا تستی به سیستم اضافه نمی‌شود.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // Privacy Assurance Pill
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0F2942))
                            .border(1.dp, CyanPrimary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "تضمین حریم خصوصی: پیامک‌ها صرفاً محلی روی دستگاه شما پردازش شده و به هیچ سرور خارجی ارسال نمی‌شوند.",
                            color = TextMuted,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onRequestPermission,
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("empty_request_permission_btn")
                        ) {
                            Icon(Icons.Default.Sms, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("درخواست مجوز پیامک اندروید", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = onOpenAppSettings,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("empty_open_settings_btn")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("باز کردن تنظیمات برنامه", fontSize = 12.sp)
                        }
                    }
                }

                totalScannedCount == 0 -> {
                    // State 2: Permission Granted, but SMS Inbox has no bank transaction messages
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(CyanPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Inbox,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Text(
                        text = "صندوق پیامک‌های بانکی خالی است",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "هیچ پیامک بانکی تراکنشی معتبری (واریز، برداشت، خرید یا انتقال) در پیامک‌های این دستگاه یافت نشد.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    // Explanation Box
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceCard)
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(16.dp))
                            Text("درباره اسکن زنده پیامک‌ها:", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("• پیامک‌های رمز پویا، کد تأیید ورود و پیام‌های خدماتی غیرمالی طبق استاندارد امنیتی فیلتر شده‌اند.", color = TextMuted, fontSize = 11.sp, lineHeight = 16.sp)
                        Text("• سیستم به هیچ وجه تراکنش نمونه یا ساختگی بارگذاری نمی‌کند تا مانده و بودجه شما دقیق بماند.", color = TextMuted, fontSize = 11.sp, lineHeight = 16.sp)
                        Text("• با دریافت اولین پیامک واریز یا برداشت از بانک، بلافاصله در این لیست ظاهر خواهد شد.", color = TextMuted, fontSize = 11.sp, lineHeight = 16.sp)
                    }

                    Button(
                        onClick = onRefresh,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("empty_refresh_sms_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("اسکن مجدد صندوق پیامک", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                else -> {
                    // State 3: Active Filter or Search produced 0 results
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.06f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Text(
                        text = "پیامکی با این مشخصات یافت نشد",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    val queryNote = if (searchQuery.isNotBlank()) {
                        "پیامک بانکی منطبق با عبارت «$searchQuery» پیدا نشد."
                    } else {
                        "پیامکی در دسته‌بندی فیلتر انتخاب‌شده یافت نشد."
                    }

                    Text(
                        text = queryNote,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )

                    OutlinedButton(
                        onClick = onClearSearch,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("empty_clear_filter_btn")
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("پاک کردن فیلترها و نمایش همه", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
