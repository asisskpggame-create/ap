package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ConsumptionEntry
import com.example.data.model.CustomItem
import com.example.data.model.ExerciseRecord
import com.example.data.model.ExpenseEntry
import com.example.data.model.SleepRecord
import com.example.data.model.UserPreferencesDoc
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ElectricTeal
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MutedViolet
import com.example.ui.theme.RoseError
import com.example.ui.theme.SkyInfo
import com.example.util.PersianDateAndUnitUtils
import com.example.util.WorldClassOsUtils

@OptIn(ExperimentalLayoutApi::class)
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
    onDeleteExpense: (ExpenseEntry) -> Unit,
    onAddSleep: (durationMinutes: Int, qualityRating: Int, notes: String) -> Unit,
    onAddExercise: (activityType: String, durationMinutes: Int, intensity: String, distanceMeters: Double, repetitions: Int, notes: String) -> Unit,
    onUpdatePrefs: (UserPreferencesDoc) -> Unit = {}
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    val todayEpochDay = PersianDateAndUnitUtils.currentLocalEpochDay()

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = { Text("ثبت روزانه سریع") },
                modifier = Modifier.testTag("tab_daily_quick_log")
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = { Text("خرج و درآمد") },
                modifier = Modifier.testTag("tab_expenses_income")
            )
            Tab(
                selected = selectedSubTab == 2,
                onClick = { selectedSubTab = 2 },
                text = { Text("خواب و ورزش") },
                modifier = Modifier.testTag("tab_sleep_exercise")
            )
        }

        when (selectedSubTab) {
            0 -> ConsumptionTrackerSubScreen(
                prefs = prefs,
                todayEpochDay = todayEpochDay,
                consumption = consumption,
                customItems = customItems,
                onQuickAdd = onQuickAddConsumption,
                onDelete = onDeleteConsumption,
                onCreateCustomItem = onCreateCustomItem,
                onUpdateCustomItem = onUpdateCustomItem,
                onDeleteCustomItem = onDeleteCustomItem,
                onSwitchToExpensesTab = { selectedSubTab = 1 }
            )
            1 -> ExpenseTrackerSubScreen(
                prefs = prefs,
                todayEpochDay = todayEpochDay,
                expenses = expenses,
                onAddExpense = onAddExpense,
                onDeleteExpense = onDeleteExpense,
                onUpdatePrefs = onUpdatePrefs
            )
            2 -> SleepAndExerciseSubScreen(
                prefs = prefs,
                todayEpochDay = todayEpochDay,
                sleepRecords = sleepRecords,
                exerciseRecords = exerciseRecords,
                onAddSleep = onAddSleep,
                onAddExercise = onAddExercise
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConsumptionTrackerSubScreen(
    prefs: UserPreferencesDoc,
    todayEpochDay: Long,
    consumption: List<ConsumptionEntry>,
    customItems: List<CustomItem>,
    onQuickAdd: (String, String, Double, String, String, Double, String) -> Unit,
    onDelete: (ConsumptionEntry) -> Unit,
    onCreateCustomItem: (String, String, String, Double, Double, Double) -> Unit,
    onUpdateCustomItem: (CustomItem) -> Unit,
    onDeleteCustomItem: (CustomItem) -> Unit,
    onSwitchToExpensesTab: () -> Unit
) {
    var itemName by remember { mutableStateOf("آب") }
    var category by remember { mutableStateOf("WATER") }
    var quantityText by remember { mutableStateOf("250") }
    var unit by remember { mutableStateOf("ml") }
    var linkedPriceText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // Dialog states for editing an existing preset or creating a new preset button
    var editingPreset by remember { mutableStateOf<CustomItem?>(null) }
    var showNewPresetDialog by remember { mutableStateOf(false) }

    val todayEntries = consumption.filter { it.localEpochDay == todayEpochDay }
    val todayWaterMl = PersianDateAndUnitUtils.sumCompatibleConsumption(
        todayEntries.filter { it.category == "WATER" },
        "ml"
    )
    val todayWaterLiters = PersianDateAndUnitUtils.convertUnit(todayWaterMl, "ml", "liters") ?: 0.0

    // Edit / Create Preset Modal Dialog
    if (editingPreset != null || showNewPresetDialog) {
        val currentEditing = editingPreset
        var dialogName by remember(currentEditing, showNewPresetDialog) {
            mutableStateOf(currentEditing?.name ?: "")
        }
        var dialogPriceText by remember(currentEditing, showNewPresetDialog) {
            mutableStateOf(
                if (currentEditing != null && currentEditing.defaultPrice > 0.0) {
                    currentEditing.defaultPrice.toLong().toString()
                } else ""
            )
        }
        var dialogQtyText by remember(currentEditing, showNewPresetDialog) {
            mutableStateOf(currentEditing?.defaultQuantity?.toInt()?.toString() ?: "1")
        }
        var dialogUnit by remember(currentEditing, showNewPresetDialog) {
            mutableStateOf(currentEditing?.unit ?: "pieces")
        }
        var dialogCategory by remember(currentEditing, showNewPresetDialog) {
            mutableStateOf(currentEditing?.category ?: "COFFEE")
        }

        AlertDialog(
            onDismissRequest = {
                editingPreset = null
                showNewPresetDialog = false
            },
            title = {
                Text(
                    text = if (currentEditing != null) "ویرایش نام و قیمت دکمه سریع" else "افزودن دکمه ثبت سریع جدید",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "با زدن این دکمه در صفحه ثبت روزانه، هم مصرف ثبت می‌شود و هم مبلغ آن خودکار به خرج‌های روزانه، هفتگی، ماهانه و سالانه اضافه می‌گردد.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = dialogName,
                        onValueChange = { dialogName = it },
                        label = { Text("نام دکمه (مثلاً: دبل، سیگار، لاته)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("preset_name_input")
                    )
                    OutlinedTextField(
                        value = dialogPriceText,
                        onValueChange = { dialogPriceText = PersianDateAndUnitUtils.toEnglishDigits(it) },
                        label = { Text("قیمت هر بار ثبت (${prefs.defaultCurrency})") },
                        placeholder = { Text("مثلاً 90000 یا 150000") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("preset_price_input")
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
                            label = { Text("×۱,۰۰۰ (تبدیل به هزار تومان)") }
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
                        listOf(
                            "COFFEE" to "قهوه و کافه",
                            "CUSTOM" to "سیگار / سفارشی",
                            "FOOD" to "غذا و خوراکی",
                            "WATER" to "آب و نوشیدنی"
                        ).forEach { (catCode, lbl) ->
                            FilterChip(
                                selected = dialogCategory == catCode,
                                onClick = { dialogCategory = catCode },
                                label = { Text(lbl) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanName = dialogName.trim()
                        val price = dialogPriceText.toDoubleOrNull() ?: 0.0
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
                            editingPreset = null
                            showNewPresetDialog = false
                        }
                    },
                    modifier = Modifier.testTag("save_preset_button")
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
                                editingPreset = null
                            }
                        ) {
                            Text("حذف دکمه", color = RoseError)
                        }
                    }
                    TextButton(
                        onClick = {
                            editingPreset = null
                            showNewPresetDialog = false
                        }
                    ) {
                        Text("انصراف")
                    }
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Editable 1-Tap Priced Preset Buttons Card ("دبل ۹۰ تومن / سیگار ۱۵۰ تومن")
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.5.dp, ElectricTeal.copy(alpha = 0.6f))
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ثبت سریع روزانه (با یک دکمه)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ElectricTeal
                            )
                            Text(
                                text = "نام و قیمت هر دکمه قابل ویرایش است • ثبت خودکار در خرج روزانه/هفتگی/ماهانه/سالانه",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = { showNewPresetDialog = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("add_new_preset_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("دکمه جدید", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    if (customItems.isEmpty()) {
                        Text(
                            text = "هیچ دکمه سریعی وجود ندارد. با زدن «+ دکمه جدید» آیتم‌هایی مثل دبل ۹۰ تومن یا سیگار ۱۵۰ تومن را بسازید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            customItems.forEach { preset ->
                                val todayCountForPreset = todayEntries.count {
                                    it.itemName.equals(preset.name, ignoreCase = true)
                                }
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = preset.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (todayCountForPreset > 0) {
                                                    Surface(
                                                        shape = RoundedCornerShape(50),
                                                        color = ElectricTeal.copy(alpha = 0.16f)
                                                    ) {
                                                        Text(
                                                            text = "امروز: ${PersianDateAndUnitUtils.toPersianDigits(todayCountForPreset.toString())}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = ElectricTeal,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = if (preset.defaultPrice > 0.0) {
                                                    "قیمت: ${PersianDateAndUnitUtils.formatCurrencyAmount(preset.defaultPrice, preset.defaultCurrency)} (ثبت خودکار در خرج‌ها)"
                                                } else {
                                                    "بدون هزینه مالی (${PersianDateAndUnitUtils.toPersianDigits(preset.defaultQuantity.toInt().toString())} ${preset.unit})"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (preset.defaultPrice > 0.0) AmberWarning else MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = if (preset.defaultPrice > 0.0) FontWeight.SemiBold else FontWeight.Normal
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            IconButton(
                                                onClick = { editingPreset = preset },
                                                modifier = Modifier.testTag("edit_preset_${preset.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "ویرایش نام و قیمت ${preset.name}",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Button(
                                                onClick = {
                                                    onQuickAdd(
                                                        preset.name,
                                                        preset.category,
                                                        preset.defaultQuantity,
                                                        preset.unit,
                                                        "",
                                                        preset.defaultPrice,
                                                        preset.defaultCurrency
                                                    )
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                                modifier = Modifier.testTag("quick_tap_preset_${preset.id}")
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("ثبت", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onSwitchToExpensesTab) {
                            Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("مشاهده در بخش خرج و درآمد (روزانه / هفتگی / ماهانه / سالانه) ←")
                        }
                    }
                }
            }
        }

        // 2. Hydration Progress Summary with Unit Conversion
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, SkyInfo.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("هیدراتاسیون و نوشیدنی امروز", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${PersianDateAndUnitUtils.toPersianDigits(todayWaterMl.toInt().toString())} ml (${String.format("%.2f", todayWaterLiters)} L)",
                            style = MaterialTheme.typography.titleMedium,
                            color = SkyInfo,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    LinearProgressIndicator(
                        progress = { (todayWaterMl.toFloat() / prefs.dailyWaterTargetMl.coerceAtLeast(1)).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = SkyInfo
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AssistChip(
                            onClick = { onQuickAdd("آب", "WATER", 250.0, "ml", "", 0.0, prefs.defaultCurrency) },
                            leadingIcon = { Icon(Icons.Default.LocalDrink, null, tint = SkyInfo, modifier = Modifier.size(16.dp)) },
                            label = { Text("+۲۵۰ ml آب") }
                        )
                        AssistChip(
                            onClick = { onQuickAdd("آب", "WATER", 1.0, "cups", "", 0.0, prefs.defaultCurrency) },
                            leadingIcon = { Icon(Icons.Default.LocalDrink, null, tint = SkyInfo, modifier = Modifier.size(16.dp)) },
                            label = { Text("+۱ لیوان آب") }
                        )
                        AssistChip(
                            onClick = { onQuickAdd("چای", "TEA", 1.0, "cups", "", 0.0, prefs.defaultCurrency) },
                            leadingIcon = { Icon(Icons.Default.Coffee, null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp)) },
                            label = { Text("+۱ فنجان چای") }
                        )
                        AssistChip(
                            onClick = { onQuickAdd("مکمل روزانه", "SUPPLEMENT", 1.0, "tablets", "", 0.0, prefs.defaultCurrency) },
                            leadingIcon = { Icon(Icons.Default.Medication, null, tint = MutedViolet, modifier = Modifier.size(16.dp)) },
                            label = { Text("+۱ قرص مکمل") }
                        )
                    }
                }
            }
        }

        // 3. Detailed Manual Consumption Form
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("ثبت مصرف موردی / دلخواه", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "WATER" to "آب",
                            "COFFEE" to "قهوه",
                            "TEA" to "چای",
                            "FOOD" to "غذا و میان‌وعده",
                            "SUPPLEMENT" to "مکمل و ویتامین",
                            "CUSTOM" to "سفارشی"
                        ).forEach { (catCode, lbl) ->
                            FilterChip(
                                selected = category == catCode,
                                onClick = {
                                    category = catCode
                                    itemName = lbl
                                    unit = when (catCode) {
                                        "WATER" -> "ml"
                                        "COFFEE", "TEA" -> "cups"
                                        "SUPPLEMENT" -> "tablets"
                                        "FOOD" -> "servings"
                                        else -> "pieces"
                                    }
                                },
                                label = { Text(lbl) }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = itemName,
                            onValueChange = { itemName = it },
                            label = { Text("نام آیتم") },
                            singleLine = true,
                            modifier = Modifier.weight(1.2f)
                        )
                        OutlinedTextField(
                            value = quantityText,
                            onValueChange = { quantityText = PersianDateAndUnitUtils.toEnglishDigits(it) },
                            label = { Text("مقدار") },
                            singleLine = true,
                            modifier = Modifier.weight(0.8f)
                        )
                    }

                    OutlinedTextField(
                        value = linkedPriceText,
                        onValueChange = { linkedPriceText = PersianDateAndUnitUtils.toEnglishDigits(it) },
                        label = { Text("قیمت خرید اختیاری (${prefs.defaultCurrency}) — ثبت همزمان در خرج‌ها") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                val q = quantityText.toDoubleOrNull() ?: 1.0
                                val p = linkedPriceText.toDoubleOrNull() ?: 0.0
                                if (itemName.isNotBlank()) {
                                    onCreateCustomItem(itemName, category, unit, q, 0.0, p)
                                }
                            }
                        ) {
                            Text("+ ذخیره به عنوان دکمه سریع")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val qty = quantityText.toDoubleOrNull() ?: 0.0
                                val price = linkedPriceText.toDoubleOrNull() ?: 0.0
                                if (qty > 0.0 && itemName.isNotBlank()) {
                                    onQuickAdd(itemName, category, qty, unit, notes, price, prefs.defaultCurrency)
                                    linkedPriceText = ""
                                }
                            },
                            modifier = Modifier.testTag("submit_consumption_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ثبت مصرف")
                        }
                    }
                }
            }
        }

        // 4. Today's Consumption Entries List
        item {
            Text(
                text = "رکوردهای ثبت روزانه امروز (${PersianDateAndUnitUtils.toPersianDigits(todayEntries.size.toString())})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(todayEntries, key = { it.id }) { entry ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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
                            text = "${entry.itemName} • ${PersianDateAndUnitUtils.toPersianDigits(entry.quantity.toInt().toString())} ${entry.unit}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "دسته: ${entry.category}" +
                                (if (entry.linkedExpenseId.isNotBlank()) " • ✅ ثبت‌شده در خرج‌های روزانه/ماهانه" else ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (entry.linkedExpenseId.isNotBlank()) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onDelete(entry) }) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = RoseError)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

private enum class FinancialPeriodFilter(val labelFa: String, val daysCount: Int) {
    DAILY("روزانه (امروز)", 1),
    WEEKLY("هفتگی (۷ روز)", 7),
    MONTHLY("ماهانه (۳۰ روز)", 30),
    YEARLY("سالانه (۱ سال)", 365),
    ALL("همه زمان‌ها", 36500)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExpenseTrackerSubScreen(
    prefs: UserPreferencesDoc,
    todayEpochDay: Long,
    expenses: List<ExpenseEntry>,
    onAddExpense: (title: String, category: String, amount: Double, currency: String, notes: String, transactionType: String) -> Unit,
    onDeleteExpense: (ExpenseEntry) -> Unit,
    onUpdatePrefs: (UserPreferencesDoc) -> Unit = {}
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, ElectricTeal.copy(alpha = 0.6f))
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

    var transactionType by remember { mutableStateOf("EXPENSE") } // EXPENSE or INCOME
    var title by remember { mutableStateOf("") }
    var showTitleError by remember { mutableStateOf(false) }
    var showAmountError by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf("FOOD") }
    var amountText by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(prefs.defaultCurrency) }
    var notes by remember { mutableStateOf("") }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var budgetInputText by remember(prefs.monthlyBudgetTarget) {
        mutableStateOf(prefs.monthlyBudgetTarget.toLong().toString())
    }

    var selectedPeriod by remember { mutableStateOf(FinancialPeriodFilter.MONTHLY) }
    var selectedTypeFilter by remember { mutableStateOf("ALL") } // ALL, EXPENSE, INCOME

    val safeToSpend = remember(prefs.monthlyBudgetTarget, prefs.defaultCurrency, todayEpochDay, expenses) {
        WorldClassOsUtils.calculateDailySafeToSpend(
            monthlyBudget = prefs.monthlyBudgetTarget,
            defaultCurrency = prefs.defaultCurrency,
            todayEpochDay = todayEpochDay,
            expenses = expenses
        )
    }

    if (showBudgetDialog) {
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
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

    // Compute 4-period summaries (Daily, Weekly, Monthly, Yearly)
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

    val filteredTransactions = remember(periodEntries, selectedTypeFilter) {
        periodEntries.filter { entry ->
            when (selectedTypeFilter) {
                "EXPENSE" -> !entry.isIncome
                "INCOME" -> entry.isIncome
                else -> true
            }
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 0. Live Daily Safe-to-Spend & Burn Rate Gauge
        item {
            val safeStatusColor = when {
                safeToSpend.isOverDailyLimit -> RoseError
                safeToSpend.burnRateRatio > 0.75f -> AmberWarning
                else -> EmeraldSuccess
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("expense_safe_to_spend_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, safeStatusColor.copy(alpha = 0.55f))
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
                                text = "سهم مجاز خرج امروز (Safe-to-Spend)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = safeStatusColor
                            )
                            Text(
                                text = "${PersianDateAndUnitUtils.toPersianDigits(safeToSpend.remainingDaysInMonth.toString())} روز تا پایان ماه شمسی • بودجه ماه: ${PersianDateAndUnitUtils.formatCurrencyAmount(safeToSpend.monthlyBudget, prefs.defaultCurrency)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { showBudgetDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تغییر سقف بودجه", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (safeToSpend.remainingTodayAllowance >= 0) {
                                "باقی‌مانده مجاز امروز: ${PersianDateAndUnitUtils.formatCurrencyAmount(safeToSpend.remainingTodayAllowance, prefs.defaultCurrency)}"
                            } else {
                                "عبور از سهم امروز: ${PersianDateAndUnitUtils.formatCurrencyAmount(-safeToSpend.remainingTodayAllowance, prefs.defaultCurrency)}"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = safeStatusColor
                        )
                        Text(
                            text = "سهم پایه روزانه: ${PersianDateAndUnitUtils.formatCurrencyAmount(safeToSpend.dailyAllowanceBeforeToday, prefs.defaultCurrency)}",
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
                    text = "خلاصه خرج و درآمد (روزانه • هفتگی • ماهانه • سالانه)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PeriodSummaryBox(
                        modifier = Modifier.weight(1f),
                        title = "امروز (روزانه)",
                        expenseText = formatCurrMap(PersianDateAndUnitUtils.aggregateExpensesByCurrency(dailyEntries)),
                        incomeText = formatCurrMap(PersianDateAndUnitUtils.aggregateIncomesByCurrency(dailyEntries)),
                        isSelected = selectedPeriod == FinancialPeriodFilter.DAILY,
                        onClick = { selectedPeriod = FinancialPeriodFilter.DAILY }
                    )
                    PeriodSummaryBox(
                        modifier = Modifier.weight(1f),
                        title = "هفتگی (۷ روز)",
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
                        title = "ماهانه (۳۰ روز)",
                        expenseText = formatCurrMap(PersianDateAndUnitUtils.aggregateExpensesByCurrency(monthlyEntries)),
                        incomeText = formatCurrMap(PersianDateAndUnitUtils.aggregateIncomesByCurrency(monthlyEntries)),
                        isSelected = selectedPeriod == FinancialPeriodFilter.MONTHLY,
                        onClick = { selectedPeriod = FinancialPeriodFilter.MONTHLY }
                    )
                    PeriodSummaryBox(
                        modifier = Modifier.weight(1f),
                        title = "سالانه (۱ سال)",
                        expenseText = formatCurrMap(PersianDateAndUnitUtils.aggregateExpensesByCurrency(yearlyEntries)),
                        incomeText = formatCurrMap(PersianDateAndUnitUtils.aggregateIncomesByCurrency(yearlyEntries)),
                        isSelected = selectedPeriod == FinancialPeriodFilter.YEARLY,
                        onClick = { selectedPeriod = FinancialPeriodFilter.YEARLY }
                    )
                }
            }
        }

        // 2. Add Expense or Income Card (Mandatory Title/Reason + Optional Description)
        item {
            val isIncome = transactionType == "INCOME"
            val accentColor = if (isIncome) EmeraldSuccess else RoseError

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "ثبت خرج یا درآمد جدید (با ذکر دلیل اجباری)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Toggle between EXPENSE ("ثبت خرج") and INCOME ("ثبت درآمد")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                transactionType = "EXPENSE"
                                category = "FOOD"
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isIncome) RoseError else MaterialTheme.colorScheme.surface,
                                contentColor = if (!isIncome) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("select_expense_type_button")
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ثبت خرج (هزینه)", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                transactionType = "INCOME"
                                category = "OTHER"
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isIncome) EmeraldSuccess else MaterialTheme.colorScheme.surface,
                                contentColor = if (isIncome) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("select_income_type_button")
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ثبت درآمد (واریزی)", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Mandatory Title / Reason Field
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            if (it.isNotBlank()) showTitleError = false
                        },
                        isError = showTitleError,
                        label = {
                            Text(if (isIncome) "تیتر / منبع درآمد (اجباری *)" else "تیتر / دلیل خرج (اجباری *)")
                        },
                        placeholder = {
                            Text(if (isIncome) "مثلاً: حقوق ماهانه، پاداش، فروش پروژه..." else "مثلاً: اسپرسو دبل، سیگار، ناهار، اسنپ...")
                        },
                        supportingText = if (showTitleError) {
                            { Text("⚠️ وارد کردن تیتر / دلیل اجباری است", color = RoseError) }
                        } else null,
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("financial_title_input")
                    )

                    // Amount Field + Quick ×1000 helper
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
                            placeholder = { Text("مثلاً 90000 یا 150000") },
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

                    // Optional Description / Notes Field
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("توضیحات تکمیلی (اختیاری)") },
                        placeholder = { Text("اگر توضیح یا جزئیات بیشتری مد نظر دارید بنویسید...") },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("financial_notes_input")
                    )

                    // Category Chips
                    Text("دسته‌بندی:", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val categories = if (isIncome) {
                            listOf(
                                "OTHER" to "حقوق و دستمزد / درآمد",
                                "SHOPPING" to "فروش و کارمزد",
                                "BILLS" to "سود و سرمایه‌گذاری"
                            )
                        } else {
                            listOf(
                                "COFFEE" to "قهوه، کافه و سیگار",
                                "FOOD" to "غذا و خوراکی",
                                "TRANSPORT" to "حمل‌ونقل",
                                "SHOPPING" to "خرید روزمره",
                                "BILLS" to "قبوض و اجاره",
                                "HEALTH" to "سلامت و دارو",
                                "OTHER" to "سایر هزینه‌ها"
                            )
                        }
                        categories.forEach { (code, lbl) ->
                            FilterChip(
                                selected = category == code,
                                onClick = { category = code },
                                label = { Text(lbl) }
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val cleanTitle = title.trim()
                            val amt = amountText.toDoubleOrNull() ?: 0.0
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
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("submit_financial_entry_button")
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isIncome) "ثبت درآمد با ذکر دلیل" else "ثبت خرج با ذکر دلیل",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. Period & Type Filter Bar + Selected Period Balance Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "مشاهده بر اساس بازه زمانی:",
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

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "ALL" to "همه (${PersianDateAndUnitUtils.toPersianDigits(periodEntries.size.toString())})",
                            "EXPENSE" to "فقط خرج‌ها (${PersianDateAndUnitUtils.toPersianDigits(periodEntries.count { !it.isIncome }.toString())})",
                            "INCOME" to "فقط درآمدها (${PersianDateAndUnitUtils.toPersianDigits(periodEntries.count { it.isIncome }.toString())})"
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

        // 4. List of All Recorded Expenses & Incomes in the Selected Period
        if (filteredTransactions.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "در بازه «${selectedPeriod.labelFa}» هیچ رکوردی ثبت نشده است.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        }

        items(filteredTransactions, key = { it.id }) { exp ->
            val isInc = exp.isIncome
            val badgeColor = if (isInc) EmeraldSuccess else RoseError
            val cleanDesc = exp.cleanNotes

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f)),
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
                                shape = RoundedCornerShape(6.dp),
                                color = badgeColor.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = if (isInc) "درآمد" else "خرج",
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
                            text = (if (isInc) "+ " else "- ") +
                                PersianDateAndUnitUtils.formatCurrencyAmount(exp.amount, exp.currency),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )

                        if (cleanDesc.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
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
                    IconButton(onClick = { onDeleteExpense(exp) }) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف رکورد مالی", tint = RoseError)
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
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) ElectricTeal else MaterialTheme.colorScheme.outline
        )
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SleepAndExerciseSubScreen(
    prefs: UserPreferencesDoc,
    todayEpochDay: Long,
    sleepRecords: List<SleepRecord>,
    exerciseRecords: List<ExerciseRecord>,
    onAddSleep: (Int, Int, String) -> Unit,
    onAddExercise: (String, Int, String, Double, Int, String) -> Unit
) {
    var sleepHoursText by remember { mutableStateOf("7.5") }
    var sleepQuality by remember { mutableIntStateOf(4) }
    var sleepNotes by remember { mutableStateOf("") }

    var exerciseType by remember { mutableStateOf("پیاده‌روی") }
    var exerciseDurationText by remember { mutableStateOf("45") }
    var exerciseIntensity by remember { mutableStateOf("MODERATE") }
    var exerciseDistanceText by remember { mutableStateOf("") }
    var exerciseNotes by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MutedViolet.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, MutedViolet.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "ℹ️ توجه: داده‌های خواب و فعالیت ورزشی صرفاً اطلاعات ثبت‌شده توسط کاربر جهت مدیریت شخصی هستند و تشخیص یا توصیه پزشکی محسوب نمی‌شوند.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Sleep Logger Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MutedViolet.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bedtime, contentDescription = null, tint = MutedViolet)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ثبت خواب شبانه", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    OutlinedTextField(
                        value = sleepHoursText,
                        onValueChange = { sleepHoursText = it },
                        label = { Text("مدت خواب (ساعت، مثلاً 7.5)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("کیفیت خواب (۱ تا ۵ ستاره):", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (1..5).forEach { star ->
                            FilterChip(
                                selected = sleepQuality == star,
                                onClick = { sleepQuality = star },
                                leadingIcon = { Icon(Icons.Default.Star, null, modifier = Modifier.size(16.dp)) },
                                label = { Text(PersianDateAndUnitUtils.toPersianDigits(star.toString())) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = sleepNotes,
                        onValueChange = { sleepNotes = it },
                        label = { Text("یادداشت خواب (اختیاری)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val hours = sleepHoursText.toDoubleOrNull() ?: 7.0
                            val mins = (hours * 60).toInt().coerceIn(15, 1440)
                            onAddSleep(mins, sleepQuality, sleepNotes)
                            sleepNotes = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ذخیره رکورد خواب")
                    }
                }
            }
        }

        // Exercise Logger Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, ElectricTeal.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DirectionsRun, contentDescription = null, tint = ElectricTeal)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ثبت فعالیت ورزشی", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("پیاده‌روی", "دویدن", "تمرین قدرتی", "یوگا و کششی", "دوچرخه‌سواری", "شنا").forEach { act ->
                            FilterChip(
                                selected = exerciseType == act,
                                onClick = { exerciseType = act },
                                label = { Text(act) }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = exerciseDurationText,
                            onValueChange = { exerciseDurationText = it },
                            label = { Text("مدت (دقیقه)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = exerciseDistanceText,
                            onValueChange = { exerciseDistanceText = it },
                            label = { Text("مسافت (متر - اختیاری)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text("شدت تمرین:", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("LOW" to "سبک", "MODERATE" to "متوسط", "HIGH" to "سنگین").forEach { (code, lbl) ->
                            FilterChip(
                                selected = exerciseIntensity == code,
                                onClick = { exerciseIntensity = code },
                                label = { Text(lbl) }
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val dur = exerciseDurationText.toIntOrNull()?.coerceIn(1, 1440) ?: 30
                            val dist = exerciseDistanceText.toDoubleOrNull() ?: 0.0
                            onAddExercise(exerciseType, dur, exerciseIntensity, dist, 0, exerciseNotes)
                            exerciseDistanceText = ""
                            exerciseNotes = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ذخیره رکورد ورزش")
                    }
                }
            }
        }

        // Recent Sleep & Exercise History
        item {
            Text("تاریخچه اخیر خواب و ورزش", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        items(sleepRecords.take(5), key = { "slp_${it.id}" }) { s ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("🌙 خواب: ${PersianDateAndUnitUtils.toPersianDigits((s.durationMinutes / 60).toString())} ساعت و ${PersianDateAndUnitUtils.toPersianDigits((s.durationMinutes % 60).toString())} دقیقه")
                    Text("کیفیت: ${PersianDateAndUnitUtils.toPersianDigits(s.qualityRating.toString())}/۵", color = MutedViolet)
                }
            }
        }

        items(exerciseRecords.take(5), key = { "exr_${it.id}" }) { ex ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("🏃 ${ex.activityType}: ${PersianDateAndUnitUtils.toPersianDigits(ex.durationMinutes.toString())} دقیقه")
                    Text(ex.intensity, color = ElectricTeal)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}
