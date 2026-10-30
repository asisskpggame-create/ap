package com.example.personallifeos.ui.screens

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personallifeos.data.model.TransactionType
import com.example.personallifeos.ui.theme.AmberWarning
import com.example.personallifeos.ui.theme.BorderCyanGlow
import com.example.personallifeos.ui.theme.CyanPrimary
import com.example.personallifeos.ui.theme.EmeraldAccent
import com.example.personallifeos.ui.theme.SurfaceCard
import com.example.personallifeos.ui.theme.SurfaceDark
import com.example.personallifeos.ui.theme.TextMuted
import com.example.personallifeos.ui.theme.TextSecondary
import com.example.personallifeos.util.PersianUtils

@Composable
fun VoiceCaptureDialog(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onAddExpenseFromSms: (String) -> String,
    onAddColloquialExpense: (String, Long, String, TransactionType) -> Unit = { _, _, _, _ -> }
) {
    if (!isOpen) return

    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isListening by remember { mutableStateOf(false) }

    val colloquialPreview = remember(inputText) {
        if (inputText.isNotBlank() && !inputText.contains("ریال") && !inputText.contains("مانده")) {
            PersianUtils.normalizeColloquialTomanAmount(inputText)
        } else null
    }

    val speechRecognizer = remember {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                SpeechRecognizer.createSpeechRecognizer(context)
            } else null
        } catch (t: Throwable) {
            null
        }
    }

    DisposableEffect(speechRecognizer) {
        onDispose {
            try {
                speechRecognizer?.destroy()
            } catch (_: Throwable) {
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x2606B6D4)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "گفتار فارسی آفلاین (fa-IR) و اسکنر پیامک",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "تبدیل محاوره‌ای تومان («۵۰۰ تومن سیگار خریدم») و پارس پیامک ۱۸+ بانک",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Offline/Online Persian SpeechRecognizer Trigger
                OutlinedButton(
                    onClick = {
                        if (speechRecognizer == null) {
                            feedbackMessage = "ℹ️ سرویس گفتار فارسی (fa-IR) روی این شبیه‌ساز فعال نیست؛ می‌توانید عبارت صوتی را در کادر زیر تایپ کنید."
                        } else {
                            try {
                                isListening = true
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
                                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                                }
                                speechRecognizer.setRecognitionListener(object : RecognitionListener {
                                    override fun onReadyForSpeech(params: Bundle?) {}
                                    override fun onBeginningOfSpeech() {}
                                    override fun onRmsChanged(rmsdB: Float) {}
                                    override fun onBufferReceived(buffer: ByteArray?) {}
                                    override fun onEndOfSpeech() { isListening = false }
                                    override fun onError(error: Int) {
                                        isListening = false
                                        feedbackMessage = "بسته زبان فارسی (fa-IR) یافت نشد؛ لطفاً متن را وارد کنید."
                                    }
                                    override fun onResults(results: Bundle?) {
                                        isListening = false
                                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                        if (!matches.isNullOrEmpty()) {
                                            inputText = matches[0]
                                        }
                                    }
                                    override fun onPartialResults(partialResults: Bundle?) {}
                                    override fun onEvent(eventType: Int, params: Bundle?) {}
                                })
                                speechRecognizer.startListening(intent)
                            } catch (t: Throwable) {
                                isListening = false
                                feedbackMessage = "خطا در راه‌اندازی میکروفن؛ لطفاً متن را تایپ کنید."
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = if (isListening) AmberWarning else CyanPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isListening) "در حال شنیدن گفتار فارسی..." else "🎙️ شروع تشخیص گفتار فارسی (SpeechRecognizer)",
                        color = if (isListening) AmberWarning else CyanPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = "پیامک بانکی یا عبارت محاوره‌ای را وارد کنید (مثلاً: «۵۰۰ تومن سیگار خریدم» یا پیامک بانک ملت...)",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("voice_modal_input"),
                    minLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = Color(0x26FFFFFF)
                    )
                )

                colloquialPreview?.let { cp ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x2610B981))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "💡 تشخیص محاوره‌ای: «${cp.extractedTitle}» • معادل: ${cp.formattedPreview} (${cp.suggestedCategory})",
                            fontSize = 11.sp,
                            color = EmeraldAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                feedbackMessage?.let { msg ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x2610B981), RoundedCornerShape(10.dp))
                            .padding(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = msg, color = EmeraldAccent, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (inputText.isNotBlank()) {
                        if (colloquialPreview != null && !inputText.contains("ریال") && !inputText.contains("بانک")) {
                            onAddColloquialExpense(
                                colloquialPreview.extractedTitle,
                                colloquialPreview.amountToman,
                                colloquialPreview.suggestedCategory,
                                TransactionType.EXPENSE
                            )
                            feedbackMessage = "تراکنش محاوره‌ای «${colloquialPreview.extractedTitle}» به مبلغ ${colloquialPreview.formattedPreview} ثبت شد ✓"
                            inputText = ""
                        } else {
                            feedbackMessage = onAddExpenseFromSms(inputText)
                            inputText = ""
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("voice_modal_submit")
            ) {
                Text("پردازش و ثبت هوشمند", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("بستن", color = TextSecondary)
            }
        },
        containerColor = SurfaceCard,
        modifier = Modifier.border(1.dp, BorderCyanGlow, RoundedCornerShape(24.dp))
    )
}
