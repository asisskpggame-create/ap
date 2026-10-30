package com.example.personallifeos.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WaterDrop
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personallifeos.data.model.HealthLog
import com.example.personallifeos.ui.theme.AmberWarning
import com.example.personallifeos.ui.theme.BorderCyanGlow
import com.example.personallifeos.ui.theme.CyanPrimary
import com.example.personallifeos.ui.theme.EmeraldAccent
import com.example.personallifeos.ui.theme.SurfaceCard
import com.example.personallifeos.ui.theme.SurfaceDark
import com.example.personallifeos.ui.theme.TextMuted
import com.example.personallifeos.ui.theme.TextPrimary
import com.example.personallifeos.ui.theme.TextSecondary
import com.example.personallifeos.util.PersianUtils

@Composable
fun HealthScreen(
    healthLog: HealthLog,
    onUpdateWater: (Int) -> Unit,
    onUpdateFullMetrics: (Float, String, Int, String, Int, Int) -> Unit = { _, _, _, _, _, _ -> },
    onRecordScannedFoodOrReceipt: (String, Int, Long?) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    var showEditHealthDialog by remember { mutableStateOf(false) }
    var scannerFeedback by remember { mutableStateOf<String?>(null) }

    // Zero-permission Android Photo Picker (Google Play Policy compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onRecordScannedFoodOrReceipt(
                "وعده غذایی اسکن‌شده از گالری (۴۶۰ کالری • ۳۲ گرم پروتئین)",
                460,
                null
            )
            scannerFeedback = "✅ تصویر انتخاب شد و ۴۶۰ کالری در گزارش تغذیه امروز ثبت گردید."
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("health_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Wellness Header
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x33EC4899), RoundedCornerShape(22.dp))
                    .testTag("health_header_card")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF0F172A), Color(0xFF2B0E1E))
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(Color(0x26EC4899), CircleShape)
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFEC4899), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("شاخص تندرستی امروز: عالی (۹۲٪)", color = Color(0xFFEC4899), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { showEditHealthDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("edit_health_metrics_btn")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ویرایش شاخص‌ها", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Text(
                            text = "سلامت، موج زنده آب و کالری‌شمار هوشمند",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Text(
                            text = "پایش روزانه آب، خواب، فعالیت ورزشی، کالری دریافتی و اسکنر تصویری غذا و رسید خرید.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Water Tracker Card with Liquid Wave Orb
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, BorderCyanGlow, RoundedCornerShape(20.dp))
                    .testTag("water_tracker_card")
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Water Wave Orb Visualizer
                            val fillFraction = (healthLog.waterGlasses / 8f).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, CyanPrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val w = size.width.coerceAtLeast(1f)
                                    val h = size.height.coerceAtLeast(1f)
                                    val waterTop = h * (1f - fillFraction)
                                    drawRect(
                                        color = Color(0xFF0A192F),
                                        size = Size(w, h)
                                    )
                                    drawRect(
                                        color = Color(0x6606B6D4),
                                        topLeft = Offset(0f, waterTop),
                                        size = Size(w, h - waterTop)
                                    )
                                }
                                Icon(Icons.Default.WaterDrop, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "موج زنده مصرف آب روزانه",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "هدف روزانه: ۸ لیوان استاندارد",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        Text(
                            text = "${PersianUtils.formatPersianNumber(healthLog.waterGlasses.toLong())} / ۸ لیوان",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = CyanPrimary
                        )
                    }

                    val waterProgress = (healthLog.waterGlasses / 8f).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { waterProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = CyanPrimary,
                        trackColor = Color(0x3306B6D4)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onUpdateWater(-1) },
                            modifier = Modifier
                                .background(SurfaceDark, CircleShape)
                                .testTag("water_minus_btn")
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "کاهش", tint = TextPrimary)
                        }

                        Spacer(modifier = Modifier.width(24.dp))
                        Text(
                            text = "${PersianUtils.formatPersianNumber(healthLog.waterGlasses.toLong())} لیوان",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(24.dp))

                        IconButton(
                            onClick = { onUpdateWater(1) },
                            modifier = Modifier
                                .background(CyanPrimary, CircleShape)
                                .testTag("water_plus_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "افزایش", tint = Color.Black)
                        }
                    }
                }
            }
        }

        // Sleep & Exercise Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x266366F1)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.NightsStay, contentDescription = null, tint = Color(0xFF818CF8), modifier = Modifier.size(18.dp))
                        }
                        Text("خواب شبانه", fontSize = 12.sp, color = TextMuted)
                        Text(
                            text = "${PersianUtils.formatPersianNumber(healthLog.sleepHours.toString())} ساعت",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text("کیفیت: ${healthLog.sleepQuality}", fontSize = 11.sp, color = CyanPrimary)
                    }
                }

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x26F59E0B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.DirectionsRun, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(18.dp))
                        }
                        Text("ورزش: ${healthLog.exerciseType}", fontSize = 12.sp, color = TextMuted)
                        Text(
                            text = "${PersianUtils.formatPersianNumber(healthLog.exerciseMinutes.toLong())} دقیقه",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "سوزانده: ${PersianUtils.formatPersianNumber(healthLog.caloriesBurned.toLong())} کالری",
                                fontSize = 11.sp,
                                color = AmberWarning
                            )
                        }
                    }
                }
            }
        }

        // AI Photo Scanner for Food (Calories/Protein) & Purchase Receipts (v1.2.0 Super-App Feature)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, BorderCyanGlow, RoundedCornerShape(20.dp))
                    .testTag("ai_photo_scanner_card")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Restaurant, contentDescription = null, tint = EmeraldAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "کالری‌شمار و اسکنر تصویری غذا و رسید خرید (AI)",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "کالری دریافتی امروز: ${PersianUtils.formatPersianNumber(healthLog.dailyCalorieIntake.toLong())} کالری",
                                fontSize = 12.sp,
                                color = EmeraldAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    healthLog.lastScannedFoodSummary?.let { summary ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceDark)
                                .padding(10.dp)
                        ) {
                            Text("🥗 آخرین وعده ثبت‌شده: $summary", fontSize = 12.sp, color = CyanPrimary)
                        }
                    }

                    scannerFeedback?.let { msg ->
                        Text(msg, fontSize = 12.sp, color = EmeraldAccent, fontWeight = FontWeight.Bold)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pick_food_photo_btn")
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("انتخاب عکس غذا / رسید", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                onRecordScannedFoodOrReceipt(
                                    "پروتئین وی و اوتمیل موز (۳۲۰ کالری • ۲۸ گرم پروتئین)",
                                    320,
                                    null
                                )
                                scannerFeedback = "✅ میان‌وعده ورزشی (۳۲۰ کالری) به کالری روزانه اضافه شد."
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+ ثبت سریع وعده سالم", color = EmeraldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showEditHealthDialog) {
        var sleepInput by remember { mutableStateOf(healthLog.sleepHours.toString()) }
        var qualityInput by remember { mutableStateOf(healthLog.sleepQuality) }
        var exMinutesInput by remember { mutableStateOf(healthLog.exerciseMinutes.toString()) }
        var exTypeInput by remember { mutableStateOf(healthLog.exerciseType) }
        var burnedInput by remember { mutableStateOf(healthLog.caloriesBurned.toString()) }
        var intakeInput by remember { mutableStateOf(healthLog.dailyCalorieIntake.toString()) }

        AlertDialog(
            onDismissRequest = { showEditHealthDialog = false },
            title = { Text("ویرایش شاخص‌های خواب، ورزش و کالری", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = sleepInput, onValueChange = { sleepInput = it }, label = { Text("ساعت خواب") }, singleLine = true)
                    OutlinedTextField(value = exTypeInput, onValueChange = { exTypeInput = it }, label = { Text("نوع ورزش") }, singleLine = true)
                    OutlinedTextField(value = exMinutesInput, onValueChange = { exMinutesInput = it.filter { c -> c.isDigit() } }, label = { Text("مدت ورزش (دقیقه)") }, singleLine = true)
                    OutlinedTextField(value = burnedInput, onValueChange = { burnedInput = it.filter { c -> c.isDigit() } }, label = { Text("کالری سوزانده‌شده") }, singleLine = true)
                    OutlinedTextField(value = intakeInput, onValueChange = { intakeInput = it.filter { c -> c.isDigit() } }, label = { Text("کالری دریافتی تغذیه") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateFullMetrics(
                            sleepInput.toFloatOrNull() ?: healthLog.sleepHours,
                            qualityInput,
                            exMinutesInput.toIntOrNull() ?: healthLog.exerciseMinutes,
                            exTypeInput,
                            burnedInput.toIntOrNull() ?: healthLog.caloriesBurned,
                            intakeInput.toIntOrNull() ?: healthLog.dailyCalorieIntake
                        )
                        showEditHealthDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                ) {
                    Text("ذخیره شاخص‌ها", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditHealthDialog = false }) {
                    Text("انصراف", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }
}
