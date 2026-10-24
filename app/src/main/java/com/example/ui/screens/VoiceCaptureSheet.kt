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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.ui.theme.MutedViolet

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
    var statusMessage by remember { mutableStateOf("برای ضبط دستور صوتی فارسی، دکمه میکروفون را لمس کنید یا دستور را تایپ نمایید.") }
    var permissionDeniedWarning by remember { mutableStateOf(false) }

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
                    SpeechRecognizer.ERROR_NO_MATCH -> "گفتاری تشخیص داده نشد. می‌توانید مجدداً تلاش کنید یا متن را ویرایش نمایید."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "دسترسی میکروفون داده نشده است."
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "خطای شبکه در تشخیص گفتار. از ورودی متنی یا دستورات آماده زیر استفاده کنید."
                    else -> "سرویس تشخیص گفتار در این محیط در دسترس نیست؛ لطفاً متن دستور را در کادر زیر وارد یا انتخاب کنید."
                }
            }

            override fun onResults(results: Bundle?) {
                isRecording = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val best = matches?.firstOrNull().orEmpty()
                if (best.isNotBlank()) {
                    transcriptText = best
                    statusMessage = "متن تشخیص داده شد. در صورت نیاز ویرایش کرده و دکمه «تحلیل هوشمند» را بزنید."
                    onAnalyzeTranscript(best)
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

    fun startVoiceListening() {
        if (speechRecognizer == null) {
            statusMessage = "تشخیص گفتار سخت‌افزاری در این شبیه‌ساز فعال نیست؛ می‌توانید از دکمه‌های نمونه یا تایپ مستقیم استفاده کنید."
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        try {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            speechRecognizer.startListening(intent)
        } catch (e: Exception) {
            isRecording = false
            statusMessage = "خطا در راه‌اندازی میکروفون: ${e.localizedMessage ?: ""}"
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
            statusMessage = "مجوز میکروفون رد شد. همچنان می‌توانید دستور خود را به صورت متنی وارد و با هوش مصنوعی تحلیل کنید."
        }
    }

    val sampleCommands = listOf(
        "فردا ساعت ۹ قبض رو پرداخت کنم و نیم ساعت ورزش کنم.",
        "الان ۴۰۰ میلی‌لیتر آب خوردم.",
        "یه قهوه ۳ یورویی خریدم.",
        "دیشب ۷ ساعت خوابیدم.",
        "امروز ۴۵ دقیقه پیاده‌روی کردم.",
        "دو عدد مکمل مصرف کردم.",
        "هزینه امروز رو ۱۲ یورو افزایش بده.",
        "این تسک رو انجام‌شده کن."
    )

    ModalBottomSheet(
        onDismissRequest = onDismissSheet,
        sheetState = sheetState,
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
            Text(
                text = "ثبت صوتی و فرماندهی هوشمند (Voice-First)",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodySmall,
                color = if (permissionDeniedWarning) AmberWarning else MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Animated Microphone & Waveform Control
            VoiceWaveformHeader(
                isRecording = isRecording,
                rmsLevel = rmsLevel,
                onMicClick = {
                    if (isRecording) {
                        speechRecognizer?.stopListening()
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

            // Editable Recognized Text Preview & Text Input Fallback
            OutlinedTextField(
                value = transcriptText,
                onValueChange = { transcriptText = it },
                label = { Text("متن فرمان (قابل ویرایش پیش از پردازش و ذخیره)") },
                placeholder = { Text("مثلاً: فردا ساعت ۹ قبض رو پرداخت کنم و نیم ساعت ورزش کنم...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("voice_transcript_input"),
                minLines = 2,
                maxLines = 4
            )

            // Quick Sample Persian Commands for 1-Tap Testing
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "نمونه فرمان‌های صوتی فارسی (لمس برای آزمایش سریع):",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sampleCommands.forEach { cmd ->
                        AssistChip(
                            onClick = {
                                transcriptText = cmd
                                onAnalyzeTranscript(cmd)
                            },
                            label = {
                                Text(
                                    text = cmd,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        )
                    }
                }
            }

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
                        Text("تحلیل هوشمند Gemini")
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
            .padding(vertical = 8.dp),
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

        FilledIconButton(
            onClick = onMicClick,
            modifier = Modifier
                .size(72.dp)
                .testTag("mic_record_toggle_button"),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = if (isRecording) "توقف ضبط" else "شروع ضبط صوتی",
                modifier = Modifier.size(34.dp)
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.5.dp, if (preview.isAmbiguous) AmberWarning else ElectricTeal)
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
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("cancel_ai_operations_button")
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("لغو")
                }

                val canConfirm = editableOps.any { it.operationType != "AskClarification" && it.title.isNotBlank() }
                Button(
                    onClick = { onConfirm(editableOps) },
                    enabled = canConfirm,
                    modifier = Modifier
                        .weight(1.4f)
                        .testTag("confirm_ai_operations_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldSuccess,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تأیید و ذخیره")
                }
            }
        }
    }
}
