package com.example.ui.components

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ElectricTeal
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.LiquidGlassBackdrop
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.MutedViolet
import com.example.ui.theme.RoseError
import com.example.ui.theme.SkyInfo
import com.example.util.PersianDateAndUnitUtils
import com.example.util.WorldClassOsUtils
import kotlin.math.PI
import kotlin.math.sin

/**
 * Full-screen Liquid Glass Biometric & PIN Vault Lock Overlay.
 * Supports ultrasonic fingerprint / device credential unlock (via KeyguardManager)
 * and a tactile 4-digit PIN pad with haptic feedback.
 */
@Composable
fun VaultBiometricLockOverlay(
    expectedPin: String,
    onUnlocked: () -> Unit,
    onSetupNewPin: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val isSetupMode = expectedPin.length < 4

    val keyguardLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            WorldClassOsUtils.HapticSignature.successThud(context)
            onUnlocked()
        }
    }

    fun triggerBiometricOrDeviceCredential() {
        try {
            val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            @Suppress("DEPRECATION")
            val intent = km?.createConfirmDeviceCredentialIntent(
                "بازگشایی قفل Personal Life OS",
                "از اثر انگشت اولتراسونیک یا قفل صفحه گوشی استفاده کنید"
            )
            if (intent != null) {
                keyguardLauncher.launch(intent)
            } else {
                errorMessage = "قفل بیومتریک در تنظیمات گوشی فعال نیست؛ رمز ۴ رقمی را وارد کنید."
            }
        } catch (_: Throwable) {
            errorMessage = "لطفاً رمز ۴ رقمی را وارد کنید."
        }
    }

    fun onDigitPressed(digit: String) {
        if (enteredPin.length < 4) {
            WorldClassOsUtils.HapticSignature.crispTick(context)
            enteredPin += digit
            errorMessage = null

            if (enteredPin.length == 4) {
                if (isSetupMode) {
                    WorldClassOsUtils.HapticSignature.successThud(context)
                    onSetupNewPin(enteredPin)
                    onUnlocked()
                } else if (enteredPin == expectedPin) {
                    WorldClassOsUtils.HapticSignature.successThud(context)
                    onUnlocked()
                } else {
                    WorldClassOsUtils.HapticSignature.heavyImpact(context)
                    errorMessage = "رمز واردشده نادرست است"
                    enteredPin = ""
                }
            }
        }
    }

    LiquidGlassBackdrop(
        modifier = Modifier
            .fillMaxSize()
            .testTag("vault_biometric_lock_overlay"),
        primaryGlow = ElectricTeal,
        secondaryGlow = MutedViolet
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            LiquidGlassCard(
                shape = RoundedCornerShape(28.dp),
                accentBorderColor = ElectricTeal,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = ElectricTeal.copy(alpha = 0.16f),
                        border = BorderStroke(1.5.dp, ElectricTeal),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "قفل امنیتی",
                                tint = ElectricTeal,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    Text(
                        text = if (isSetupMode) "تعیین رمز ۴ رقمی گاوصندوق" else "قفل امنیتی Personal Life OS",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = if (isSetupMode) {
                            "یک رمز ۴ رقمی برای محافظت از اطلاعات مالی و شخصی خود وارد کنید"
                        } else {
                            "رمز ۴ رقمی خود را وارد کنید یا از حسگر اثر انگشت استفاده نمایید"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 4 PIN Dots Indicator
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until 4) {
                            val filled = i < enteredPin.length
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (filled) ElectricTeal else Color.White.copy(alpha = 0.15f)
                                    )
                            )
                        }
                    }

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.labelMedium,
                            color = RoseError,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 3x4 Numeric Glass Keypad
                    val rows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        rows.forEach { rowDigits ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                rowDigits.forEach { d ->
                                    PinPadGlassButton(
                                        label = PersianDateAndUnitUtils.toPersianDigits(d),
                                        onClick = { onDigitPressed(d) },
                                        testTag = "pin_key_$d"
                                    )
                                }
                            }
                        }

                        // Bottom Row: Biometric | 0 | Backspace
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MutedViolet.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, MutedViolet.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .clickable { triggerBiometricOrDeviceCredential() }
                                    .testTag("pin_biometric_btn")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "بازگشایی با اثر انگشت",
                                        tint = MutedViolet,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            PinPadGlassButton(
                                label = "۰",
                                onClick = { onDigitPressed("0") },
                                testTag = "pin_key_0"
                            )

                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        if (enteredPin.isNotEmpty()) {
                                            WorldClassOsUtils.HapticSignature.crispTick(context)
                                            enteredPin = enteredPin.dropLast(1)
                                        }
                                    }
                                    .testTag("pin_backspace_btn")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                                        contentDescription = "پاک کردن رقم",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { triggerBiometricOrDeviceCredential() },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, tint = ElectricTeal)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تایید هویت با اثر انگشت / سنسور بیومتریک گوشی")
                    }
                }
            }
        }
    }
}

@Composable
private fun PinPadGlassButton(
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Animated Liquid Wave Progress Indicator for Hydration Cards.
 * Renders a smooth sine-wave fluid level that rises as water consumption increases.
 * S24 Ultra Vulkan/Skia safe (guards against zero dimensions).
 */
@Composable
fun AnimatedWaterWaveBox(
    progress: Float,
    modifier: Modifier = Modifier,
    waveColor: Color = SkyInfo,
    content: @Composable () -> Unit
) {
    val clampedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0.05f, 1f),
        animationSpec = tween(durationMillis = 650),
        label = "water_fill_level"
    )
    val infiniteTransition = rememberInfiniteTransition(label = "water_wave_phase")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase_rad"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            if (w <= 2f || h <= 2f) return@Canvas

            val waterTopY = h * (1f - clampedProgress)
            val amplitude = (h * 0.045f).coerceIn(2f, 10f)

            val path = Path().apply {
                moveTo(0f, h)
                lineTo(0f, waterTopY)
                val steps = 24
                val stepX = w / steps
                for (i in 0..steps) {
                    val x = i * stepX
                    val angle = (i.toFloat() / steps) * (2f * PI.toFloat()) + phase
                    val y = (waterTopY + sin(angle) * amplitude).coerceIn(0f, h)
                    lineTo(x, y)
                }
                lineTo(w, h)
                close()
            }

            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        waveColor.copy(alpha = 0.28f),
                        waveColor.copy(alpha = 0.10f)
                    ),
                    startY = waterTopY.coerceAtLeast(0f),
                    endY = h.coerceAtLeast(1f)
                )
            )
        }
        content()
    }
}
