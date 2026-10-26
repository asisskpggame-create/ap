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
import com.example.ui.theme.LiquidGlassPill
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
    onInstantExecuteTranscript: (String) -> Unit = { onAnalyzeTranscript(it) },
    onConfirmOperations: (List<ProposedAiOperation>, String) -> Unit,
    onRejectPreview: (String) -> Unit,
    onDismissSheet: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var transcriptText by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var instantAutoSave by remember { mutableStateOf(true) }
    var rmsLevel by remember { mutableFloatStateOf(0.2f) }
    var statusMessage by remember { mutableStateOf("در حال آماده‌سازی میکروفون... (یا متن را بنویسید)") }
    var permissionDeniedWarning by remember { mutableStateOf(false) }
    var selectedTargetMode by remember { mutableStateOf("AUTO") } // AUTO, TASK, JOURNAL

    // SpeechRecognizer instance
    val speechRecognizer = remember {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                SpeechRecognizer.createSpeechRecognizer(context)
            } else {
                null
            }
        } catch (_: Throwable) {
            null
        }
    }

    DisposableEffect(speechRecognizer, instantAutoSave, selectedTargetMode) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isRecording = true
                statusMessage = "گوش می‌دهم... بگویید (مثلاً: ۵۰۰ سیگار، ۱۲۰ اسنپ، ۳۰۰ طلب از علی)"
            }

            override fun onBeginningOfSpeech() {
                isRecording = true
                statusMessage = "در حال دریافت صدای شما..."
            }

            override fun onRmsChanged(rmsdB: Float) {
                rmsLevel = ((rmsdB + 2f) / 12f).coerceIn(0.1f, 1.0f)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isRecording = false
                statusMessage = "در حال پردازش..."
            }

            override fun onError(error: Int) {
                isRecording = false
                statusMessage = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "صدایی تشخیص داده نشد. دوباره لمس کنید یا بنویسید."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "دسترسی میکروفون داده نشده است."
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "خطای شبکه گفتار؛ می‌توانید همین‌جا تایپ کنید."
                    else -> "می‌توانید روی میکروفون بزنید یا دستور را تایپ کنید."
                }
            }

            override fun onResults(results: Bundle?) {
                isRecording = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val best = matches?.firstOrNull().orEmpty()
                if (best.isNotBlank()) {
                    transcriptText = best
                    if (selectedTargetMode == "AUTO" && instantAutoSave) {
                        statusMessage = "ثبت آنی: «$best»"
                        onInstantExecuteTranscript(best)
                    } else if (selectedTargetMode == "AUTO") {
                        statusMessage = "تشخیص داده شد. پیش‌نمایش آماده است."
                        onAnalyzeTranscript(best)
                    } else {
                        statusMessage = "تشخیص داده شد."
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
            if (selectedTargetMode == "AUTO" && instantAutoSave) {
                onInstantExecuteTranscript(best)
            } else if (selectedTargetMode == "AUTO") {
                onAnalyzeTranscript(best)
            }
        }
    }

    fun startVoiceListening() {
        if (speechRecognizer == null) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "بگویید: مثلاً ۵۰۰ سیگار یا فردا ساعت ۱۰ جلسه...")
            }
            try {
                systemSpeechLauncher.launch(intent)
            } catch (e: Exception) {
                statusMessage = "سرویس گفتار در دسترس نیست؛ متن را تایپ کنید."
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
            statusMessage = "برای شروع ضبط روی دکمه میکروفون بزنید."
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
            statusMessage = "مجوز میکروفون فعال نیست؛ می‌توانید دستور را تایپ کنید."
        }
    }

    // Auto-start listening immediately when sheet opens if permission is already granted
    androidx.compose.runtime.LaunchedEffect(Unit) {
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPerm && speechRecognizer != null) {
            startVoiceListening()
        } else if (!hasPerm) {
            statusMessage = "برای ضبط صوتی روی میکروفون بزنید یا دستور را بنویسید."
        }
    }

    val sampleFinanceCommands = listOf(
        "۵۰۰ سیگار",
        "۱۲۰ اسنپ",
        "۲۵۰ ناهار",
        "۳۰۰ از علی طلبکارم",
        "۲۰۰ به رضا بدهکارم"
    )

    val sampleTaskCommands = listOf(
        "فردا ساعت ۱۰ جلسه با تیم",
        "پرداخت قبض برق تا ساعت ۶ عصر",
        "۴۰۰ میلی‌لیتر آب خوردم"
    )

    ModalBottomSheet(
        onDismissRequest = onDismissSheet,
        sheetState = sheetState,
        shape = GlassTheme.tokens.bottomSheetShape,
        containerColor = Color(0xFF0F1612).copy(alpha = 0.96f),
        scrimColor = Color.Black.copy(alpha = 0.65f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with Live Mic Indicator & Instant Auto-Save Toggle
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
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "ثبت صوتی و هوشمند",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Black
                    )
                }

                FilterChip(
                    selected = instantAutoSave,
                    onClick = { instantAutoSave = !instantAutoSave },
                    label = {
                        Text(
                            text = if (instantAutoSave) "ثبت خودکار آنی: روشن" else "پیش‌نمایش قبل از ثبت",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
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

            // Editable Recognized Text Input
            OutlinedTextField(
                value = transcriptText,
                onValueChange = { transcriptText = it },
                label = { Text("بگویید یا تایپ کنید (خرج، طلب، بدهی، تسک، آب...)") },
                placeholder = {
                    Text("مثلاً: ۵۰۰ سیگار | ۳۰۰ از علی طلبکارم | فردا ساعت ۱۰ جلسه")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("voice_transcript_input"),
                minLines = 2,
                maxLines = 3
            )

            // Primary Fast Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onInstantExecuteTranscript(transcriptText)
                    },
                    enabled = transcriptText.isNotBlank() && !isProcessing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldSuccess,
                        contentColor = Color(0xFF06130C)
                    ),
                    modifier = Modifier
                        .weight(1.4f)
                        .testTag("instant_execute_voice_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ثبت آنی (۱ لمسی)", fontWeight = FontWeight.Black)
                }

                OutlinedButton(
                    onClick = { onAnalyzeTranscript(transcriptText) },
                    enabled = transcriptText.isNotBlank() && !isProcessing,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("analyze_voice_command_button")
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("پیش‌نمایش")
                    }
                }
            }

            if (transcriptText.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val taskOp = ProposedAiOperation(
                                operationType = "CreateTask",
                                title = transcriptText.trim(),
                                category = "عمومی",
                                humanReadablePreview = "ایجاد تسک: «${transcriptText.trim()}»"
                            )
                            onConfirmOperations(listOf(taskOp), transcriptText)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_save_task_voice_button")
                    ) {
                        Text("ثبت مستقیم تسک", style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val journalOp = ProposedAiOperation(
                                operationType = "CreateTask",
                                title = transcriptText.trim().take(80),
                                category = "یادداشت روزانه",
                                priority = "LOW",
                                humanReadablePreview = "ثبت یادداشت: «${transcriptText.trim()}»"
                            )
                            onConfirmOperations(listOf(journalOp), transcriptText)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_save_journal_voice_button")
                    ) {
                        Text("ثبت یادداشت روزانه", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Quick Sample Chips for 1-Tap Testing (Finance + Tasks)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "نمونه‌های سریع مالی و بدهی/طلب (لمس برای ثبت آنی):",
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldSuccess,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sampleFinanceCommands.forEach { cmd ->
                        AssistChip(
                            onClick = {
                                transcriptText = cmd
                                if (instantAutoSave) {
                                    onInstantExecuteTranscript(cmd)
                                } else {
                                    onAnalyzeTranscript(cmd)
                                }
                            },
                            label = { Text(text = cmd, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Text(
                    text = "نمونه‌های تسک و روزانه:",
                    style = MaterialTheme.typography.labelSmall,
                    color = ElectricTeal,
                    fontWeight = FontWeight.Bold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sampleTaskCommands.forEach { cmd ->
                        AssistChip(
                            onClick = {
                                transcriptText = cmd
                                if (instantAutoSave) {
                                    onInstantExecuteTranscript(cmd)
                                } else {
                                    onAnalyzeTranscript(cmd)
                                }
                            },
                            label = { Text(text = cmd, style = MaterialTheme.typography.labelSmall) }
                        )
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

            Spacer(modifier = Modifier.height(14.dp))
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
            if (size.width <= 2f || size.height <= 2f) return@Canvas
            val barCount = 7
            val spacing = size.width / barCount
            val minBarHeight = 6.dp.toPx().coerceAtMost(size.height)
            for (i in 0 until barCount) {
                val factor = if (isRecording) ((i % 3 + 1) * 0.25f * pulse * (0.6f + rmsLevel)) else 0.18f
                val barHeight = (size.height * factor).coerceIn(minBarHeight, size.height)
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
            if (size.width <= 2f || size.height <= 2f) return@Canvas
            val barCount = 7
            val spacing = size.width / barCount
            val minBarHeight = 6.dp.toPx().coerceAtMost(size.height)
            for (i in 0 until barCount) {
                val factor = if (isRecording) (((barCount - i) % 3 + 1) * 0.25f * pulse * (0.6f + rmsLevel)) else 0.18f
                val barHeight = (size.height * factor).coerceIn(minBarHeight, size.height)
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
                GlassCard(
                    shape = RoundedCornerShape(14.dp),
                    baseColor = Color(0xFF141F1A),
                    alpha = 0.7f,
                    accentBorderColor = MutedViolet,
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
