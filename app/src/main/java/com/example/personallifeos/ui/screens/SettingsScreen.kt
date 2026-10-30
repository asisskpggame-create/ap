package com.example.personallifeos.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personallifeos.ui.theme.BorderCyanGlow
import com.example.personallifeos.ui.theme.CyanPrimary
import com.example.personallifeos.ui.theme.EmeraldAccent
import com.example.personallifeos.ui.theme.RoseDanger
import com.example.personallifeos.ui.theme.SurfaceCard
import com.example.personallifeos.ui.theme.SurfaceDark
import com.example.personallifeos.ui.theme.TextMuted
import com.example.personallifeos.ui.theme.TextPrimary
import com.example.personallifeos.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    notificationSettings: NotificationSettingsState = NotificationSettingsState(),
    onUpdateNotificationSettings: (NotificationSettingsState) -> Unit = {},
    onOpenNotificationCenter: () -> Unit = {},
    onOpenBankSelector: () -> Unit = {},
    onResetData: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showResetConfirm by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "تنظیمات، اعلان‌ها و پشتیبان‌گیری",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "مدیریت فیش‌خوان زنده بانکی، سرشماره‌ها، امنیت و مشخصات Personal Life OS نسخه 1.6.0R",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
        }

        // Smart Notification & Bank Selector Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, BorderCyanGlow, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "مرکز اعلان هوشمند و فیش‌خوان بانکی (سبک میرزا)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("شنود زنده فیش‌های بانکی", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("پارس خودکار پیامک‌های بانک و نمایش بنر شناور", fontSize = 11.sp, color = TextSecondary)
                        }
                        Switch(
                            checked = notificationSettings.liveReceiptListenerEnabled,
                            onCheckedChange = {
                                onUpdateNotificationSettings(notificationSettings.copy(liveReceiptListenerEnabled = it))
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = CyanPrimary)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("پوشش حریم خصوصی مبالغ در اعلان", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("مخفی کردن مبلغ تراکنش روی صفحه قفل", fontSize = 11.sp, color = TextSecondary)
                        }
                        Switch(
                            checked = notificationSettings.maskSensitiveAmounts,
                            onCheckedChange = {
                                onUpdateNotificationSettings(notificationSettings.copy(maskSensitiveAmounts = it))
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = EmeraldAccent)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onOpenBankSelector,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E6F30)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_open_bank_selector_btn")
                        ) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("افزودن بانک جدید", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = onOpenNotificationCenter,
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_open_notif_center_btn")
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("مرکز اعلان‌ها", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "فضای ذخیره‌سازی محلی (Room Database)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Text(
                        text = "اطلاعات وظایف، تراکنش‌ها، یادداشت‌های محرمانه و عادت‌های شما در پایگاه داده آفلاین دستگاه با امنیت بالا نگهداری می‌شوند.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { showBackupDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("export_backup_btn")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("پشتیبان‌گیری (JSON)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { showResetConfirm = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("reset_data_btn")
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, tint = RoseDanger)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("بازنشانی داده‌ها", color = RoseDanger, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "امنیت و حریم خصوصی",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "یادداشت‌های صندوق امن با رمز ۴ رقمی محافظت شده و متن پیامک‌های بانکی بدون نیاز به سرور واسط، مستقیماً روی گوشی پردازش می‌شوند.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }

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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "مشخصات اپلیکیشن نیتیو اندروید",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Text(text = "نام نرم‌افزار: Personal Life OS", fontSize = 12.sp, color = TextSecondary)
                    Text(text = "نسخه: 1.6.0R (versionCode = 7)", fontSize = 12.sp, color = EmeraldAccent, fontWeight = FontWeight.Bold)
                    Text(text = "فریم‌ورک: Jetpack Compose + Material Design 3 + Room", fontSize = 12.sp, color = TextSecondary)
                    Text(text = "موتور هوشمند: فیش‌خوان زنده به سبک میرزا + موتور ممیزی‌شده پیامک بانکی", fontSize = 12.sp, color = CyanPrimary)
                }
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("بازنشانی اطلاعات به حالت کارخانه", fontWeight = FontWeight.Bold) },
            text = { Text("آیا از پاک کردن تمام داده‌ها و بازگشت به تنظیمات اولیه اطمینان دارید؟") },
            confirmButton = {
                Button(
                    onClick = {
                        onResetData()
                        showResetConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseDanger)
                ) {
                    Text("بله، بازنشانی شود", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("انصراف", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }

    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("پشتیبان‌گیری از اطلاعات (Backup)", fontWeight = FontWeight.Bold) },
            text = {
                Text("داده‌های شما آماده خروجی گرفتن هستند. ساختار JSON شامل وظایف، تراکنش‌های بانکی، یادداشت‌های محرمانه و گزارش سلامت می‌باشد.")
            },
            confirmButton = {
                Button(
                    onClick = { showBackupDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                ) {
                    Text("بستن", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = SurfaceDark
        )
    }
}
