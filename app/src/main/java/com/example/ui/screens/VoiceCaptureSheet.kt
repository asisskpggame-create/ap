package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ai.AiParseResult
import com.example.ai.ProposedAiOperation
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ElectricTeal
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GlassCard
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.GlassTheme
import com.example.ui.theme.MutedViolet

/**
 * High-Precision Speech-To-Text Modal Sheet with Liquid Glass Design.
 * Integrates real Android microphone recording via SpeechRecognizer and RecognizerIntent,
 * with fast one-tap creation for Tasks and Daily Journal Entries.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VoiceCaptureModalSheet(
    isProcessing: Boolean,
    pendingPreview: AiParseResult?,
    onAnalyzeTranscript: (String) -> Unit,
    onConfirmOperations: (List<ProposedAiOperation>, String) -> Unit,
    onRejectPreview: (String) -> Unit,
    onDismissSheet: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var transcriptText by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var rmsLevel by remember { mutableFloatStateOf(0.2f) }
    var statusMessage by remember { mutableStateOf("برای ضبط دستور یا یادداشت صوتی، دکمه میکروفون را لمس کنید.") }
    var permissionDeniedWarning by remember { mutableStateOf(false) }
    var selectedTargetMode by remember { mutableStateOf("AUTO") } // AUTO, TASK, JOURNAL

    // SpeechRecognizer instance
    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            null
        }
    }

    DisposableEffect(speechRecognizer) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isRecording = true
                statusMessage = "در حال گوش دادن (فارسی)... صحبت کنید"
            }

            override fun onBeginningOfSpeech() {
                isRecording = true
                statusMessage = "در حال ضبط صدای شما..."
            }

            override fun onRmsChanged(rmsdB: Float) {
                rmsLevel = ((rmsdB + 2f) / 12f).coerceIn(0.1f, 1.0f)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isRecording = false
                statusMessage = "در حال پردازش گفتار..."
            }

            override fun onError(error: Int) {
                isRecording = false
                statusMessage = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "گفتاری تشخیص داده نشد. می‌توانید مجدداً صحبت کنید یا متن را بنویسید."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "دسترسی میکروفون داده نشده است."
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "خطای اتصال اینترنت در سرویس گفتار. می‌توانید از ورودی متنی استفاده کنید."
                    SpeechRecognizer.ERROR_AUDIO -> "خطای ضبط صدا در دستگاه."
                    SpeechRecognizer.ERROR_SERVER -> "خطای سرور تشخیص گفتار."
                    else -> "سرویس تشخیص گفتار داخلی در دسترس نیست. می‌توانید متن را وارد یا از دیالوگ صوتی سیستم استفاده نمایید."
                }
            }

            override fun onResults(results: Bundle?) {
                isRecording = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val best = matches?.firstOrNull().orEmpty()
                if (best.isNotBlank()) {
                    transcriptText = best
                    statusMessage = "صدا تشخیص داده شد. می‌توانید تسک یا یادداشت را سریع ثبت کنید."
                    // If in AUTO mode, automatically trigger intelligent analysis
                    if (selectedTargetMode == "AUTO") {
                        onAnalyzeTranscript(best)
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!partial.isNullOrBlank()) {
                    transcriptText = partial
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
        speechRecognizer?.setRecognitionListener(listener)
        onDispose {
            try {
                speechRecognizer?.destroy()
            } catch (_: Exception) {
            }
        }
    }

    // System Speech Intent Dialog Fallback (supported on all Android versions)
    val systemSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        val best = matches?.firstOrNull().orEmpty()
        if (best.isNotBlank()) {
            transcriptText = best
            statusMessage = "صدا از دیالوگ سیستم دریافت شد."
            if (selectedTargetMode == "AUTO") {
                onAnalyzeTranscript(best)
            }
        }
    }

    fun startVoiceListening() {
        if (speechRecognizer == null) {
            // Launch standard system speech dialog fallback
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "فرمان، تسک یا یادداشت روزانه خود را بگویید...")
            }
            try {
                systemSpeechLauncher.launch(intent)
            } catch (e: Exception) {
                statusMessage = "برنامه تشخیص گفتار بر روی این دستگاه یافت نشد. متن را دستی وارد نمایید."
            }
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fa-IR")
            putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("fa-IR", "fa", "en-US"))
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        try {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            speechRecognizer.startListening(intent)
        } catch (e: Exception) {
            isRecording = false
            statusMessage = "خطا در اتصال به میکروفون: ${e.localizedMessage ?: ""}"
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionDeniedWarning = false
            startVoiceListening()
        } else {
            permissionDeniedWarning = true
            statusMessage = "مجوز میکروفون تأیید نشد. لطفاً مجوز ضبط صدا را در تنظیمات فعال کنید."
        }
    }

    val sampleTaskCommands = listOf(
        "فردا ساعت ۱۰ جلسه با تیم پروژه و بررسی فاز دوم",
        "تسک فوری: پرداخت قبوض برق و گاز تا ساعت ۶ عصر",
        "خرید میوه و نان تازه با اولویت بالا برای خانه",
        "مطالعه فصل پنجم کتاب مدیریت زمان برای فردا شب"
    )

    val sampleJournalCommands = listOf(
        "یادداشت روزانه: امروز فاز جدید پروژه شروع شد و با تیم به توافق رسیدیم.",
        "خاطره امروز: پیاده‌روی عصرگاهی در هوای تمیز بهاری و احساس سرزندگی عالی.",
        "یادداشت روزانه: ایده جذاب برای بازطراحی رابط کاربری شیشه‌ای ثبت شد."
    )

    ModalBottomSheet(
        onDismissRequest = onDismissSheet,
        sheetState = sheetState,
        shape = GlassTheme.tokens.bottomSheetShape,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with Live Mic Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "فرمان صوتی و ثبت هوشمند",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isRecording) MaterialTheme.colorScheme.error.copy(alpha = 0.16f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    border = BorderStroke(
                        1.dp,
                        GlassTheme.specularBorderBrush(if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    )
                ) {
                    Text(
                        text = if (isRecording) "● در حال ضبط" else "آماده ضبط",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodySmall,
                color = if (permissionDeniedWarning) AmberWarning else MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Animated Microphone & Multi-Band Waveform Control
            VoiceWaveformHeader(
                isRecording = isRecording,
                rmsLevel = rmsLevel,
                onMicClick = {
                    if (isRecording) {
                        try {
                            speechRecognizer?.stopListening()
                        } catch (_: Exception) {}
                        isRecording = false
                    } else {
                        val hasPerm = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPerm) {
                            startVoiceListening()
                        } else {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                }
            )

            // Target Mode Filter Pills (Auto AI / Task Focus / Journal Focus)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedTargetMode == "AUTO",
                    onClick = { selectedTargetMode = "AUTO" },
                    leadingIcon = { Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp)) },
                    label = { Text("هوشمند Gemini") }
                )
                FilterChip(
                    selected = selectedTargetMode == "TASK",
                    onClick = { selectedTargetMode = "TASK" },
                    leadingIcon = { Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp)) },
                    label = { Text("تسک سریع") }
                )
                FilterChip(
                    selected = selectedTargetMode == "JOURNAL",
                    onClick = { selectedTargetMode = "JOURNAL" },
                    leadingIcon = { Icon(Icons.Default.Book, null, modifier = Modifier.size(16.dp)) },
                    label = { Text("یادداشت روزانه") }
                )
            }

            // Editable Recognized Text Preview
            OutlinedTextField(
                value = transcriptText,
                onValueChange = { transcriptText = it },
                label = { Text("متن گفتار تشخیص داده شده (قابل ویرایش)") },
                placeholder = {
                    Text(
                        when (selectedTargetMode) {
                            "TASK" -> "مثلاً: فردا ساعت ۵ جلسه کاری با تیم دارم..."
                            "JOURNAL" -> "مثلاً: یادداشت روزانه: امروز بعدازظهر قدم زدم و حس خوبی داشتم..."
                            else -> "مثلاً: فردا ساعت ۹ قبض رو پرداخت کنم یا ۵۰۰ میل آب خوردم..."
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("voice_transcript_input"),
                minLines = 2,
                maxLines = 4
            )

            // Direct 1-Tap Execution Action Buttons (Task, Journal, AI)
            if (transcriptText.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val taskOp = ProposedAiOperation(
                                operationType = "CreateTask",
                                title = transcriptText.trim(),
                                category = if (selectedTargetMode == "JOURNAL") "یادداشت روزانه" else "عمومی",
                                humanReadablePreview = "ایجاد تسک صوتی: «${transcriptText.trim()}»"
                            )
                            onConfirmOperations(listOf(taskOp), transcriptText)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_save_task_voice_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ثبت تسک", style = MaterialTheme.typography.labelMedium)
                    }

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val journalOp = ProposedAiOperation(
                                operationType = "CreateTask",
                                title = transcriptText.trim().take(80),
                                category = "یادداشت روزانه",
                                priority = "LOW",
                                humanReadablePreview = "ثبت یادداشت روزانه: «${transcriptText.trim()}»"
                            )
                            onConfirmOperations(listOf(journalOp), transcriptText)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_save_journal_voice_button")
                    ) {
                        Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ثبت یادداشت", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // Quick Persian Sample Prompts for Instant Testing
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "نمونه‌های پرکاربرد (لمس برای پر کردن و آزمون سریع):",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Text("تسک‌ها و برنامه‌ریزی:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                ) {
                    sampleTaskCommands.forEach { cmd ->
                        AssistChip(
                            onClick = {
                                transcriptText = cmd
                                onAnalyzeTranscript(cmd)
                            },
                            label = { Text(text = cmd, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }

                Text("یادداشت و خاطرات روزانه:", style = MaterialTheme.typography.labelSmall, color = ElectricTeal)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    sampleJournalCommands.forEach { cmd ->
                        AssistChip(
                            onClick = {
                                transcriptText = cmd
                                onAnalyzeTranscript(cmd)
                            },
                            label = { Text(text = cmd, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }
            }

            // Primary Bottom Actions (Dismiss & Analyze)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismissSheet,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("بستن")
                }

                Button(
                    onClick = { onAnalyzeTranscript(transcriptText) },
                    enabled = transcriptText.isNotBlank() && !isProcessing,
                    modifier = Modifier
                        .weight(1.5f)
                        .testTag("analyze_voice_command_button")
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تحلیل هوشمند چندمنظوره")
                    }
                }
            }

            // Structured Operation Preview Card (Edit-Before-Save + Confirm / Cancel)
            if (pendingPreview != null) {
                StructuredOperationPreviewCard(
                    preview = pendingPreview,
                    rawTranscript = transcriptText,
                    onConfirm = { editedOps ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onConfirmOperations(editedOps, transcriptText)
                    },
                    onCancel = {
                        onRejectPreview(transcriptText)
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun VoiceWaveformHeader(
    isRecording: Boolean,
    rmsLevel: Float,
    onMicClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "voice_wave")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(650),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_pulse"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Left waveform bars
        Canvas(modifier = Modifier.size(width = 88.dp, height = 48.dp)) {
            val barCount = 7
            val spacing = size.width / barCount
            for (i in 0 until barCount) {
                val factor = if (isRecording) ((i % 3 + 1) * 0.25f * pulse * (0.6f + rmsLevel)) else 0.18f
                val barHeight = (size.height * factor).coerceIn(6.dp.toPx(), size.height)
                drawRoundRect(
                    color = if (isRecording) ElectricTeal else MutedViolet.copy(alpha = 0.4f),
                    topLeft = Offset(i * spacing + 4f, (size.height - barHeight) / 2f),
                    size = Size(6.dp.toPx(), barHeight),
                    cornerRadius = CornerRadius(8f, 8f)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Center Animated Mic Button with Specular Glow Border
        FilledIconButton(
            onClick = onMicClick,
            modifier = Modifier
                .size(76.dp)
                .border(
                    BorderStroke(
                        width = if (isRecording) 2.5.dp else 1.5.dp,
                        brush = GlassTheme.specularBorderBrush(
                            accentColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            topIntensity = if (isRecording) 0.9f else 0.6f
                        )
                    ),
                    CircleShape
                )
                .testTag("mic_record_toggle_button"),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = if (isRecording) "توقف ضبط صدا" else "شروع ضبط گفتار فارسی",
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Right waveform bars
        Canvas(modifier = Modifier.size(width = 88.dp, height = 48.dp)) {
            val barCount = 7
            val spacing = size.width / barCount
            for (i in 0 until barCount) {
                val factor = if (isRecording) (((barCount - i) % 3 + 1) * 0.25f * pulse * (0.6f + rmsLevel)) else 0.18f
                val barHeight = (size.height * factor).coerceIn(6.dp.toPx(), size.height)
                drawRoundRect(
                    color = if (isRecording) ElectricTeal else MutedViolet.copy(alpha = 0.4f),
                    topLeft = Offset(i * spacing + 4f, (size.height - barHeight) / 2f),
                    size = Size(6.dp.toPx(), barHeight),
                    cornerRadius = CornerRadius(8f, 8f)
                )
            }
        }
    }
}

@Composable
fun StructuredOperationPreviewCard(
    preview: AiParseResult,
    rawTranscript: String,
    onConfirm: (List<ProposedAiOperation>) -> Unit,
    onCancel: () -> Unit
) {
    var editableOps by remember(preview) { mutableStateOf(preview.operations) }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        accentBorderColor = if (preview.isAmbiguous) AmberWarning else ElectricTeal
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (preview.isAmbiguous) "⚠️ نیاز به شفاف‌سازی پیش از ثبت" else "🔍 پیش‌نمایش ساختاریافته عملیات (قابل ویرایش)",
                style = MaterialTheme.typography.titleMedium,
                color = if (preview.isAmbiguous) AmberWarning else ElectricTeal,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = preview.conversationalReply,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            editableOps.forEachIndexed { idx, op ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
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
                            Text(
                                text = "عملیات #${idx + 1}: ${op.operationType}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MutedViolet
                            )
                            Text(
                                text = if (op.requiresConfirmation) "نیازمند تأیید" else "اطلاعاتی",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldSuccess
                            )
                        }

                        if (op.operationType != "AskClarification") {
                            OutlinedTextField(
                                value = op.title,
                                onValueChange = { newTitle ->
                                    editableOps = editableOps.toMutableList().also { list ->
                                        list[idx] = op.copy(
                                            title = newTitle,
                                            humanReadablePreview = "${op.operationType}: $newTitle"
                                        )
                                    }
                                },
                                label = { Text("عنوان / آیتم") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            OutlinedTextField(
                                value = op.title,
                                onValueChange = { clarifiedTitle ->
                                    editableOps = editableOps.toMutableList().also { list ->
                                        list[idx] = op.copy(
                                            operationType = "CreateTask",
                                            title = clarifiedTitle,
                                            humanReadablePreview = "ایجاد تسک: «$clarifiedTitle»"
                                        )
                                    }
                                },
                                label = { Text("عنوان دقیق تسک را وارد کنید تا ثبت شود") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Text(
                            text = op.humanReadablePreview,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("انصراف")
                }
                Button(
                    onClick = { onConfirm(editableOps) },
                    modifier = Modifier.weight(1.5f)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تأیید و ذخیره نهایی")
                }
            }
        }
    }
}
