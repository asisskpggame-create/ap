package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SettingsVoice
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.LaunchedEffect
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
import com.example.ui.theme.GlassTheme
import com.example.ui.theme.MutedViolet
import com.example.util.PersianDateAndUnitUtils
import com.example.util.PersianOfflineSpeechHelper

/**
 * High-Precision Persian (`fa-IR`) Speech-To-Text Modal Sheet with Offline Capability.
 * Supports on-device & offline speech recognition (`EXTRA_PREFER_OFFLINE`), continuous dictation,
 * spoken Persian number normalization, and deterministic 100% offline NLU execution.
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

    val isDeviceCurrentlyOnline = remember { PersianOfflineSpeechHelper.isDeviceOnline(context) }
    val isOnDeviceEngineSupported = remember { PersianOfflineSpeechHelper.isOnDeviceSpeechSupported(context) }

    var transcriptText by remember { mutableStateOf("") }
    var livePartialText by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    // Offline-First Persian STT enabled by default so voice-to-text works without internet
    var preferOfflinePersian by remember { mutableStateOf(true) }
    var appendDictationMode by remember { mutableStateOf(false) }
    var convertSpokenNumbers by remember { mutableStateOf(true) }
    var instantAutoSave by remember { mutableStateOf(false) }
    var showOfflinePackGuide by remember { mutableStateOf(false) }
    var rmsLevel by remember { mutableFloatStateOf(0.2f) }
    var statusMessage by remember {
        mutableStateOf(
            if (!isDeviceCurrentlyOnline) {
                "حالت آفلاین فارسی فعال است (بدون نیاز به اینترنت) • آماده دریافت صدا"
            } else {
                "تبدیل گفتار به متن فارسی (آفلاین / هیبرید) • آماده ضبط"
            }
        )
    }
    var permissionDeniedWarning by remember { mutableStateOf(false) }
    var selectedTargetMode by remember { mutableStateOf("AUTO") }

    // Create SpeechRecognizer instance (standard recognizer honors EXTRA_PREFER_OFFLINE on all Android versions)
    val speechRecognizer = remember {
        PersianOfflineSpeechHelper.createSpeechRecognizer(
            context = context,
            useDedicatedOnDeviceRecognizer = false
        )
    }

    fun applyRecognizedSentence(rawSentence: String, isFinalResult: Boolean) {
        val normalized = PersianOfflineSpeechHelper.normalizePersianTranscript(
            rawText = rawSentence,
            convertSpokenNumbers = convertSpokenNumbers
        )
        if (normalized.isBlank()) return

        if (isFinalResult) {
            livePartialText = ""
            val updatedTranscript = if (appendDictationMode && transcriptText.isNotBlank()) {
                "${transcriptText.trim()} $normalized"
            } else {
                normalized
            }
            transcriptText = updatedTranscript

            if (selectedTargetMode == "AUTO" && instantAutoSave && !appendDictationMode) {
                statusMessage = "ثبت آنی آفلاین: «$updatedTranscript»"
                onInstantExecuteTranscript(updatedTranscript)
            } else if (selectedTargetMode == "AUTO" && !appendDictationMode) {
                statusMessage = "متن فارسی با موفقیت تبدیل شد. می‌توانید ویرایش یا ثبت کنید."
                onAnalyzeTranscript(updatedTranscript)
            } else {
                statusMessage = "به متن دیکته اضافه شد. برای ادامه دوباره میکروفون را بزنید."
            }
        } else {
            livePartialText = normalized
            if (!appendDictationMode) {
                transcriptText = normalized
            }
        }
    }

    DisposableEffect(
        speechRecognizer,
        preferOfflinePersian,
        appendDictationMode,
        convertSpokenNumbers,
        instantAutoSave,
        selectedTargetMode
    ) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isRecording = true
                livePartialText = ""
                statusMessage = if (preferOfflinePersian) {
                    "گوش می‌دهم (موتور فارسی آفلاین fa-IR)... واضح صحبت کنید"
                } else {
                    "گوش می‌دهم (تشخیص گفتار فارسی fa-IR)..."
                }
            }

            override fun onBeginningOfSpeech() {
                isRecording = true
                statusMessage = "در حال دریافت و تبدیل صدای شما به متن فارسی..."
            }

            override fun onRmsChanged(rmsdB: Float) {
                rmsLevel = ((rmsdB + 2f) / 12f).coerceIn(0.1f, 1.0f)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isRecording = false
                statusMessage = "در حال نهایی‌سازی متن فارسی..."
            }

            override fun onError(error: Int) {
                isRecording = false
                livePartialText = ""
                val (msgFa, needsOfflinePack) = PersianOfflineSpeechHelper.describeSpeechErrorFa(
                    errorCode = error,
                    isOfflinePreferred = preferOfflinePersian
                )
                statusMessage = msgFa
                if (needsOfflinePack) {
                    showOfflinePackGuide = true
                    if (!preferOfflinePersian) {
                        preferOfflinePersian = true
                    }
                }
            }

            override fun onResults(results: Bundle?) {
                isRecording = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val best = matches?.firstOrNull().orEmpty()
                if (best.isNotBlank()) {
                    applyRecognizedSentence(best, isFinalResult = true)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                if (!partial.isNullOrBlank()) {
                    applyRecognizedSentence(partial, isFinalResult = false)
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

    // System Speech Intent Dialog Fallback (works even when background RecognitionService is restricted)
    val systemSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        val best = matches?.firstOrNull().orEmpty()
        if (best.isNotBlank()) {
            applyRecognizedSentence(best, isFinalResult = true)
        }
    }

    fun launchSystemSpeechDialog() {
        val intent = PersianOfflineSpeechHelper.buildPersianSpeechIntent(
            preferOffline = preferOfflinePersian,
            continuousDictation = appendDictationMode,
            promptText = if (preferOfflinePersian) {
                "تبدیل گفتار به متن فارسی (آفلاین) - صحبت کنید..."
            } else {
                "بگویید: مثلاً ۵۰۰ سیگار یا فردا ساعت ۱۰ جلسه..."
            }
        )
        try {
            systemSpeechLauncher.launch(intent)
        } catch (_: Exception) {
            statusMessage = "سرویس گفتار در دسترس نیست؛ بسته صوتی فارسی را بررسی کنید."
            showOfflinePackGuide = true
        }
    }

    fun startVoiceListening() {
        if (speechRecognizer == null) {
            launchSystemSpeechDialog()
            return
        }

        val effectiveOffline = preferOfflinePersian || !PersianOfflineSpeechHelper.isDeviceOnline(context)
        val intent = PersianOfflineSpeechHelper.buildPersianSpeechIntent(
            preferOffline = effectiveOffline,
            continuousDictation = appendDictationMode
        )
        try {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            speechRecognizer.startListening(intent)
        } catch (_: Exception) {
            isRecording = false
            launchSystemSpeechDialog()
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
            statusMessage = "مجوز میکروفون فعال نیست؛ دسترسی را تایید کنید یا متن را بنویسید."
        }
    }

    // Auto-start listening immediately when sheet opens if permission is already granted
    LaunchedEffect(Unit) {
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPerm && speechRecognizer != null) {
            startVoiceListening()
        } else if (!hasPerm) {
            statusMessage = "برای شروع تبدیل گفتار فارسی به متن، روی دکمه میکروفون بزنید."
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header with Title & Offline Status Badge
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
                        tint = ElectricTeal,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "تبدیل گفتار به متن فارسی (آفلاین)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = if (preferOfflinePersian) {
                                if (isOnDeviceEngineSupported) "موتور آفلاین روی دستگاه (fa-IR On-Device)" else "اولویت پردازش آفلاین فارسی (fa-IR)"
                            } else {
                                "حالت ترکیبی (آفلاین + آنلاین فارسی)"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldSuccess
                        )
                    }
                }

                // Offline / Online Mode Primary Chip
                FilterChip(
                    selected = preferOfflinePersian,
                    onClick = {
                        preferOfflinePersian = !preferOfflinePersian
                        statusMessage = if (preferOfflinePersian) {
                            "حالت آفلاین فارسی فعال شد (تشخیص صدا و پردازش بدون اینترنت)."
                        } else {
                            "حالت ترکیبی (آفلاین + آنلاین) فعال شد."
                        }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (preferOfflinePersian) Icons.Default.OfflineBolt else Icons.Default.SettingsVoice,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = {
                        Text(
                            text = if (preferOfflinePersian) "آفلاین فارسی: روشن" else "حالت ترکیبی",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    modifier = Modifier.testTag("voice_offline_mode_toggle")
                )
            }

            // Secondary Dictation Controls Row: Continuous Append Mode + Spoken Number Conversion + Instant Execute
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = appendDictationMode,
                    onClick = { appendDictationMode = !appendDictationMode },
                    label = {
                        Text(
                            text = if (appendDictationMode) "دیکته پیوسته (افزودن به متن)" else "جایگزینی متن در هر ضبط",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    modifier = Modifier.testTag("voice_append_mode_toggle")
                )

                FilterChip(
                    selected = convertSpokenNumbers,
                    onClick = { convertSpokenNumbers = !convertSpokenNumbers },
                    label = {
                        Text(
                            text = "تبدیل اعداد گفتاری (پانصد ← ۵۰۰)",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )

                FilterChip(
                    selected = instantAutoSave,
                    onClick = { instantAutoSave = !instantAutoSave },
                    label = {
                        Text(
                            text = if (instantAutoSave) "ثبت خودکار آنی: روشن" else "پیش‌نمایش قبل از ثبت",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
            }

            // Status / Live Feedback Banner
            Surface(
                color = Color(0xFF15221C),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    if (permissionDeniedWarning) AmberWarning.copy(alpha = 0.6f)
                    else ElectricTeal.copy(alpha = 0.35f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (!isDeviceCurrentlyOnline) {
                            Icon(
                                imageVector = Icons.Default.WifiOff,
                                contentDescription = null,
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (permissionDeniedWarning) AmberWarning else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    TextButton(
                        onClick = { showOfflinePackGuide = !showOfflinePackGuide },
                        modifier = Modifier.testTag("toggle_offline_guide_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DownloadForOffline,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("بسته آفلاین", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Collapsible Offline Persian Pack Guide & System Recognizer Fallback Card
            AnimatedVisibility(visible = showOfflinePackGuide) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    accentBorderColor = AmberWarning
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "راهنمای فعال‌سازی تشخیص گفتار آفلاین فارسی (بدون اینترنت)",
                            style = MaterialTheme.typography.labelLarge,
                            color = AmberWarning,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "۱. برای کارکرد ۱۰۰٪ آفلاین روی گوشی، بسته زبان «فارسی (ایران) / Persian» را در بخش Offline Speech Recognition سیستم اندروید دانلود کنید.\n۲. پس از ثبت متن، موتور هوشمند برنامه نیز تمام دستورات مالی، تسک‌ها و یادداشت‌ها را کاملاً آفلاین پردازش و ذخیره می‌کند.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    PersianOfflineSpeechHelper.openOfflineSpeechSettings(context)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("open_offline_speech_settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DownloadForOffline,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تنظیمات بسته آفلاین فارسی", style = MaterialTheme.typography.labelSmall)
                            }

                            OutlinedButton(
                                onClick = { launchSystemSpeechDialog() },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("system_speech_dialog_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SettingsVoice,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("دیالوگ صوتی سیستم", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

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

            // Live partial preview badge when in continuous append dictation mode
            if (appendDictationMode && livePartialText.isNotBlank()) {
                Text(
                    text = "در حال شنیدن: «$livePartialText»",
                    style = MaterialTheme.typography.bodySmall,
                    color = ElectricTeal,
                    fontWeight = FontWeight.Bold
                )
            }

            // Editable Recognized Persian Text Input + Copy & Clear Toolbar
            OutlinedTextField(
                value = transcriptText,
                onValueChange = { transcriptText = it },
                label = { Text("متن فارسی تشخیص‌داده‌شده (قابل ویرایش یا تایپ)") },
                placeholder = {
                    Text("مثلاً: ۵۰۰ سیگار | ۳۰۰ از علی طلبکارم | فردا ساعت ۱۰ جلسه")
                },
                trailingIcon = {
                    if (transcriptText.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    clipboard?.setPrimaryClip(ClipData.newPlainText("PersianVoiceText", transcriptText))
                                    statusMessage = "متن فارسی در حافظه کپی شد."
                                },
                                modifier = Modifier.testTag("copy_voice_transcript_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "کپی متن فارسی",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    transcriptText = ""
                                    livePartialText = ""
                                    statusMessage = "متن پاک شد. آماده ضبط جدید."
                                },
                                modifier = Modifier.testTag("clear_voice_transcript_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "پاک کردن متن",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("voice_transcript_input"),
                minLines = 2,
                maxLines = 4
            )

            // Primary Fast Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val normalized = PersianOfflineSpeechHelper.normalizePersianTranscript(
                            transcriptText,
                            convertSpokenNumbers = convertSpokenNumbers
                        )
                        transcriptText = normalized
                        onInstantExecuteTranscript(normalized)
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
                    Text("ثبت آنی آفلاین (۱ لمسی)", fontWeight = FontWeight.Black)
                }

                OutlinedButton(
                    onClick = {
                        val normalized = PersianOfflineSpeechHelper.normalizePersianTranscript(
                            transcriptText,
                            convertSpokenNumbers = convertSpokenNumbers
                        )
                        transcriptText = normalized
                        onAnalyzeTranscript(normalized)
                    },
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
                            val cleanText = PersianOfflineSpeechHelper.normalizePersianTranscript(
                                transcriptText,
                                convertSpokenNumbers = convertSpokenNumbers
                            )
                            val taskOp = ProposedAiOperation(
                                operationType = "CreateTask",
                                title = cleanText,
                                category = "عمومی",
                                humanReadablePreview = "ایجاد تسک: «$cleanText»"
                            )
                            onConfirmOperations(listOf(taskOp), cleanText)
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
                            val cleanText = PersianOfflineSpeechHelper.normalizePersianTranscript(
                                transcriptText,
                                convertSpokenNumbers = convertSpokenNumbers
                            )
                            val journalOp = ProposedAiOperation(
                                operationType = "CreateTask",
                                title = cleanText.take(80),
                                category = "یادداشت روزانه",
                                priority = "LOW",
                                humanReadablePreview = "ثبت یادداشت: «$cleanText»"
                            )
                            onConfirmOperations(listOf(journalOp), cleanText)
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
                    text = "نمونه‌های سریع مالی و بدهی/طلب (پردازش ۱۰۰٪ آفلاین):",
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
                contentDescription = if (isRecording) "توقف ضبط صدا" else "شروع ضبط گفتار فارسی آفلاین",
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
                                        val updatedPreview = if (op.operationType == "AddExpense") {
                                            "ثبت تراکنش: ${PersianDateAndUnitUtils.formatCurrencyAmount(op.amount, op.currency.ifBlank { "TOMAN" })} ($newTitle)"
                                        } else {
                                            "${op.operationType}: $newTitle"
                                        }
                                        list[idx] = op.copy(
                                            title = newTitle,
                                            humanReadablePreview = updatedPreview
                                        )
                                    }
                                },
                                label = { Text("عنوان / آیتم") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (op.operationType == "AddExpense") {
                                val curr = op.currency.ifBlank { "TOMAN" }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        "EXPENSE" to "پرداخت (−)",
                                        "INCOME" to "دریافت (+)",
                                        "TRANSFER" to "انتقال",
                                        "RECEIVABLE" to "طلب",
                                        "DEBT" to "بدهی"
                                    ).forEach { (typeCode, typeLbl) ->
                                        FilterChip(
                                            selected = op.transactionType.equals(typeCode, ignoreCase = true),
                                            onClick = {
                                                editableOps = editableOps.toMutableList().also { list ->
                                                    list[idx] = op.copy(
                                                        transactionType = typeCode,
                                                        humanReadablePreview = "ثبت $typeLbl: ${PersianDateAndUnitUtils.formatCurrencyAmount(op.amount, curr)} (${op.title})"
                                                    )
                                                }
                                            },
                                            label = { Text(typeLbl, style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = op.amount.toLong().toString(),
                                    onValueChange = { newAmtStr ->
                                        val parsedAmt = PersianDateAndUnitUtils.toEnglishDigits(newAmtStr)
                                            .replace(",", "")
                                            .toDoubleOrNull() ?: 0.0
                                        editableOps = editableOps.toMutableList().also { list ->
                                            list[idx] = op.copy(
                                                amount = parsedAmt,
                                                humanReadablePreview = "ثبت تراکنش: ${PersianDateAndUnitUtils.formatCurrencyAmount(parsedAmt, curr)} (${op.title})"
                                            )
                                        }
                                    },
                                    label = { Text("مبلغ دقیق ($curr)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else {
                            if (op.amount > 0.0) {
                                Text(
                                    text = "نوع تراکنش را برای مبلغ ${PersianDateAndUnitUtils.formatCurrencyAmount(op.amount, op.currency.ifBlank { "TOMAN" })} انتخاب کنید:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AmberWarning,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        "INCOME" to "دریافت (+)",
                                        "EXPENSE" to "پرداخت (−)",
                                        "RECEIVABLE" to "طلب دارم",
                                        "DEBT" to "بدهکارم"
                                    ).forEach { (resolvedType, resolvedLabel) ->
                                        AssistChip(
                                            onClick = {
                                                editableOps = editableOps.toMutableList().also { list ->
                                                    val safeCurr = op.currency.ifBlank { "TOMAN" }
                                                    val safeTitle = op.title.ifBlank { "تراکنش" }
                                                    list[idx] = op.copy(
                                                        operationType = "AddExpense",
                                                        transactionType = resolvedType,
                                                        title = safeTitle,
                                                        currency = safeCurr,
                                                        humanReadablePreview = "ثبت $resolvedLabel: ${PersianDateAndUnitUtils.formatCurrencyAmount(op.amount, safeCurr)} ($safeTitle)"
                                                    )
                                                }
                                            },
                                            label = { Text(resolvedLabel, style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = op.title,
                                onValueChange = { clarifiedTitle ->
                                    editableOps = editableOps.toMutableList().also { list ->
                                        list[idx] = op.copy(
                                            operationType = if (op.amount > 0.0) "AddExpense" else "CreateTask",
                                            title = clarifiedTitle,
                                            humanReadablePreview = if (op.amount > 0.0) {
                                                "ثبت تراکنش: ${PersianDateAndUnitUtils.formatCurrencyAmount(op.amount, op.currency.ifBlank { "TOMAN" })} ($clarifiedTitle)"
                                            } else {
                                                "ایجاد تسک: «$clarifiedTitle»"
                                            }
                                        )
                                    }
                                },
                                label = { Text("عنوان دقیق را وارد کنید تا ثبت شود") },
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
