package com.example.personallifeos.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.personallifeos.sms.BankNotificationItem
import com.example.personallifeos.sms.SmsTransactionDirection
import com.example.personallifeos.sms.SmsVerificationStatus
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

data class NotificationSettingsState(
    val liveReceiptListenerEnabled: Boolean = true,
    val floatingBannerEnabled: Boolean = true,
    val soundAndTtsEnabled: Boolean = true,
    val maskSensitiveAmounts: Boolean = false
)

@Composable
fun FloatingReceiptNotificationBanner(
    activeNotification: BankNotificationItem?,
    maskSensitiveAmounts: Boolean,
    onDismiss: () -> Unit,
    onOpenNotificationCenter: () -> Unit
) {
    AnimatedVisibility(
        visible = activeNotification != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        activeNotification?.let { item ->
            val parsed = item.parsedResult
            val isNeedsReview = parsed.verificationStatus == SmsVerificationStatus.NEEDS_REVIEW
            val accentColor = if (isNeedsReview) AmberWarning else EmeraldAccent

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .border(1.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                    .clickable { onOpenNotificationCenter() }
                    .testTag("floating_receipt_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                            )
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isNeedsReview) Icons.Default.Warning else Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "فیش زنده ${parsed.bank.faName}",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(accentColor.copy(alpha = 0.18f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = parsed.verificationStatus.titleFa,
                                        color = accentColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (maskSensitiveAmounts) {
                                    "${parsed.direction.faName} • مبلغ مخفی شده (حالت حریم خصوصی)"
                                } else {
                                    "${parsed.direction.faName}: ${PersianUtils.formatToman(parsed.amountToman)}"
                                },
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "بستن بنر",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCenterDialog(
    isOpen: Boolean,
    notifications: List<BankNotificationItem>,
    settings: NotificationSettingsState,
    onUpdateSettings: (NotificationSettingsState) -> Unit,
    onSimulateIncomingSms: (String, String) -> Unit,
    onConfirmNeedsReview: (BankNotificationItem, SmsTransactionDirection) -> Unit,
    onDismissNotification: (String) -> Unit,
    onMarkAllRead: () -> Unit,
    onOpenBankSelector: () -> Unit,
    onClose: () -> Unit
) {
    if (!isOpen) return

    var activeTab by remember { mutableStateOf("ALL") }

    val filteredList = remember(notifications, activeTab) {
        when (activeTab) {
            "AUTO" -> notifications.filter { it.parsedResult.verificationStatus == SmsVerificationStatus.AUTO_ACCEPTED }
            "REVIEW" -> notifications.filter { it.parsedResult.verificationStatus == SmsVerificationStatus.NEEDS_REVIEW }
            else -> notifications
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(26.dp))
                .border(1.dp, BorderCyanGlow, RoundedCornerShape(26.dp))
                .testTag("notification_center_dialog"),
            color = SurfaceDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CyanPrimary.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = CyanPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "مرکز اعلان و فیش‌خوان زنده",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "شنود هوشمند پیامک‌های بانکی و مدیریت تراکنش‌های مشکوک",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row {
                        IconButton(
                            onClick = onMarkAllRead,
                            modifier = Modifier.testTag("mark_all_notifications_read_btn")
                        ) {
                            Icon(Icons.Default.DoneAll, contentDescription = "خوانده شدن همه", tint = CyanPrimary)
                        }
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = "بستن", tint = TextSecondary)
                        }
                    }
                }

                // Live Simulator Bar
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚡ شبیه‌ساز دریافت زنده فیش بانکی:",
                                color = CyanPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "+ افزودن بانک (میرزا)",
                                color = EmeraldAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable {
                                        onClose()
                                        onOpenBankSelector()
                                    }
                                    .padding(4.dp)
                            )
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                SimulatorChip("فیش بانک ملی", EmeraldAccent) {
                                    onSimulateIncomingSms(
                                        "98300017",
                                        "بانک ملی ایران\nبرداشت: ۴۵۰,۰۰۰ ریال\nاز: ۰۱*****۱۲۳۴\nمانده: ۲۵,۴۰۰,۰۰۰ ریال\nپیگیری: ۷۷۴۴۱۱"
                                    )
                                }
                            }
                            item {
                                SimulatorChip("فیش بلوبانک", CyanPrimary) {
                                    onSimulateIncomingSms(
                                        "982000",
                                        "بلو\nبرداشت پول\nعلیرضا عزیز، 900,000 ریال از حساب شما پرید.\nموجودی: 64,161,452 ریال"
                                    )
                                }
                            }
                            item {
                                SimulatorChip("فیش مبهم (نیازمند بررسی)", AmberWarning) {
                                    onSimulateIncomingSms(
                                        "98200033",
                                        "بانک ملت\nحساب ۶۱۰۴۳۳******۱۲۳۴\nمبلغ: ۱,۲۰۰,۰۰۰ ریال\nمانده: ۹,۵۰۰,۰۰۰ ریال"
                                    )
                                }
                            }
                        }
                    }
                }

                // Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val tabs = listOf(
                        "ALL" to "همه (${PersianUtils.formatPersianNumber(notifications.size.toLong())})",
                        "AUTO" to "ثبت خودکار",
                        "REVIEW" to "نیازمند بررسی",
                        "SETTINGS" to "تنظیمات"
                    )
                    tabs.forEach { (id, label) ->
                        val selected = activeTab == id
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) CyanPrimary else SurfaceCard)
                                .clickable { activeTab = id }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (selected) Color.Black else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                if (activeTab == "SETTINGS") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        NotificationSettingToggleRow(
                            title = "سرویس فیش‌خوان زنده بانکی",
                            subtitle = "دریافت و پارس لحظه‌ای پیامک‌های واریز و برداشت بانک‌های ایران",
                            checked = settings.liveReceiptListenerEnabled,
                            onCheckedChange = { onUpdateSettings(settings.copy(liveReceiptListenerEnabled = it)) }
                        )
                        NotificationSettingToggleRow(
                            title = "بنر شناور بالای صفحه (Heads-Up)",
                            subtitle = "نمایش کارت خلاصه فیش بلافاصله پس از دریافت پیامک",
                            checked = settings.floatingBannerEnabled,
                            onCheckedChange = { onUpdateSettings(settings.copy(floatingBannerEnabled = it)) }
                        )
                        NotificationSettingToggleRow(
                            title = "اعلان صوتی و خوانش مبلغ",
                            subtitle = "پخش هشدار صوتی هنگام ثبت خودکار یا نیاز به بررسی فیش",
                            checked = settings.soundAndTtsEnabled,
                            onCheckedChange = { onUpdateSettings(settings.copy(soundAndTtsEnabled = it)) }
                        )
                        NotificationSettingToggleRow(
                            title = "پوشش حریم خصوصی مبالغ (Privacy Mask)",
                            subtitle = "مخفی کردن مبلغ تراکنش در اعلان‌های شناور",
                            checked = settings.maskSensitiveAmounts,
                            onCheckedChange = { onUpdateSettings(settings.copy(maskSensitiveAmounts = it)) }
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (filteredList.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "اعلانی در این بخش وجود ندارد.",
                                        color = TextMuted,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        } else {
                            items(filteredList, key = { it.id }) { item ->
                                val parsed = item.parsedResult
                                val isReview = parsed.verificationStatus == SmsVerificationStatus.NEEDS_REVIEW
                                val badgeColor = when (parsed.verificationStatus) {
                                    SmsVerificationStatus.AUTO_ACCEPTED -> EmeraldAccent
                                    SmsVerificationStatus.NEEDS_REVIEW -> AmberWarning
                                    SmsVerificationStatus.REJECTED, SmsVerificationStatus.DUPLICATE -> RoseDanger
                                }

                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(0.5.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = parsed.bank.faName,
                                                    color = TextPrimary,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "(${item.sender})",
                                                    color = TextMuted,
                                                    fontSize = 11.sp
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(badgeColor.copy(alpha = 0.16f))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = parsed.verificationStatus.titleFa,
                                                    color = badgeColor,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Text(
                                            text = if (settings.maskSensitiveAmounts) {
                                                "مبلغ: ••••••• تومان (${parsed.direction.faName})"
                                            } else {
                                                "${parsed.direction.faName}: ${PersianUtils.formatToman(parsed.amountToman)}"
                                            },
                                            color = CyanPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )

                                        parsed.reviewReasonFa?.let { reason ->
                                            Text(
                                                text = "⚠️ علت: $reason",
                                                color = AmberWarning,
                                                fontSize = 11.sp
                                            )
                                        }

                                        if (isReview && !item.isConfirmedOrDismissed) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        onConfirmNeedsReview(item, SmsTransactionDirection.WITHDRAWAL)
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = AmberWarning),
                                                    shape = RoundedCornerShape(10.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("ثبت به‌عنوان هزینه", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Button(
                                                    onClick = {
                                                        onConfirmNeedsReview(item, SmsTransactionDirection.DEPOSIT)
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                                                    shape = RoundedCornerShape(10.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("ثبت به‌عنوان واریز", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                                OutlinedButton(
                                                    onClick = { onDismissNotification(item.id) },
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Text("رد", color = RoseDanger, fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SimulatorChip(
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.16f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun NotificationSettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceCard)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(text = subtitle, color = TextMuted, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = CyanPrimary)
        )
    }
}
