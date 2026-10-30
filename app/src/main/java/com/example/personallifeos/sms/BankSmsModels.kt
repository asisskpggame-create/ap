package com.example.personallifeos.sms

enum class BankType(
    val faName: String,
    val shortNameFa: String,
    val brandColorHex: Long,
    val defaultSenders: List<String> = emptyList(),
    val requireSenderMatchByDefault: Boolean = false
) {
    MELLI("بانک ملی ایران", "ملی", 0xFF0F3D6E, listOf("986000", "98300017", "6000"), false),
    BLUBANK("بلوبانک", "بلو بانک", 0xFF2563EB, listOf("982000", "BLUBANK", "30009601"), false),
    BLUBANK_POL("بلوبانک (حالت پل)", "بلوبانک حالت پل", 0xFF3B82F6, listOf("982000", "BLUBANK", "30009601"), false),
    GARDESHGARI("بانک گردشگری", "گردشگری", 0xFFDC2626, listOf("98200064", "200064"), false),
    SADERAT("بانک صادرات ایران", "صادرات", 0xFF1E3A8A, listOf("98300060", "300060"), false),
    MELLAT("بانک ملت", "ملت", 0xFFE11D48, listOf("98200033", "200033"), false),
    TEJARAT("بانک تجارت", "تجارت", 0xFF1D4ED8, listOf("98200070", "200070"), false),
    KESHAVARZI("بانک کشاورزی", "کشاورزی", 0xFF15803D, listOf("98200077", "200077"), false),
    SAMAN("بانک سامان", "سامان", 0xFF0284C7, listOf("9820000", "20000"), false),
    PASARGAD("بانک پاسارگاد", "پاسارگاد", 0xFFD97706, listOf("982000500", "2000500"), false),
    REFAH("بانک رفاه کارگران", "رفاه", 0xFF0D9488, listOf("98300007", "300007"), false),
    MASKAN("بانک مسکن", "مسکن", 0xFFEA580C, listOf("98300014", "300014"), false),
    SEPAH("بانک سپه", "سپه", 0xFFF59E0B, listOf("98300015", "300015"), false),
    VEPOD("ویپاد (پاسارگاد)", "ویپاد", 0xFF10B981, listOf("Wepod", "982000500"), false),
    TOSEE_TAAVON("بانک توسعه تعاون", "توسعه تعاون", 0xFF0D9488),
    SANAT_MADAN("بانک صنعت و معدن", "صنعت و معدن", 0xFF6366F1),
    TOSEE_SADERAT("بانک توسعه صادرات", "توسعه صادرات", 0xFF4F46E5),
    POST_BANK("پست بانک ایران", "پست بانک", 0xFF059669),
    MEHR_IRAN("بانک مهر ایران", "مهر ایران", 0xFF14B8A6),
    RESALAT("بانک قرض‌الحسنه رسالت", "رسالت", 0xFF3B82F6),
    PARSIAN("بانک پارسیان", "پارسیان", 0xFF7C3AED),
    AYANDEH("بانک آینده", "آینده", 0xFF9333EA),
    SHAHR("بانک شهر", "شهر", 0xFFDC2626),
    EGHTESAD_NOVIN("بانک اقتصاد نوین", "اقتصاد نوین", 0xFF9333EA),
    SINA("بانک سینا", "سینا", 0xFF0284C7),
    DAY("بانک دی", "دی", 0xFFEA580C),
    KARAFARIN("بانک کارآفرین", "کارآفرین", 0xFF16A34A),
    IRAN_ZAMIN("بانک ایران زمین", "ایران زمین", 0xFF0D9488),
    SARMAYE("بانک سرمایه", "سرمایه", 0xFF2563EB),
    KHAVARMIANEH("بانک خاورمیانه", "خاورمیانه", 0xFF475569),
    MELAL("مؤسسه اعتباری ملل", "ملل", 0xFF059669),
    KOSAR("مؤسسه اعتباری کوثر", "کوثر", 0xFFF59E0B),
    NOOR("مؤسسه اعتباری نور", "نور", 0xFFEAB308),
    UNKNOWN("بانک ناشناس", "ناشناس", 0xFF64748B)
}

/**
 * Audit Fix #1 & #3:
 * UNKNOWN direction MUST NEVER default to an expense (isExpense = false).
 */
enum class SmsTransactionDirection(val faName: String, val isExpense: Boolean) {
    WITHDRAWAL("برداشت", true),
    PURCHASE("خرید", true),
    DEPOSIT("واریز", false),
    TRANSFER_POL("انتقال پل", true),
    TRANSFER_PAYA("انتقال پایا", true),
    TRANSFER_CARD("انتقال کارت", true),
    UNKNOWN("نامشخص", false)
}

enum class DetectedCurrencyUnit(val titleFa: String) {
    RIAL("ریال"),
    TOMAN("تومان"),
    UNKNOWN("نامشخص")
}

enum class SmsVerificationStatus(val titleFa: String) {
    AUTO_ACCEPTED("ثبت خودکار"),
    NEEDS_REVIEW("نیازمند بررسی کاربر"),
    REJECTED("رد شده / غیرمالی"),
    DUPLICATE("تکراری (مسدود شده)")
}

/**
 * Audit Fix #2 & #3:
 * Template-level verification and explicit sender policy profile.
 */
data class BankTemplateDefinition(
    val templateId: String,
    val bank: BankType,
    val version: Int,
    val titleFa: String,
    val patternDescription: String
)

data class BankUserConfig(
    val bank: BankType,
    val senderNumber: String,
    val requireSenderMatch: Boolean = true,
    val verifiedTemplateKeys: Set<String> = emptySet() // Format: "${templateId}@v${version}"
)

data class BankNotificationItem(
    val id: String,
    val sender: String,
    val rawText: String,
    val parsedResult: BankSmsParsedResult,
    val receivedAt: String,
    val isRead: Boolean = false,
    val isConfirmedOrDismissed: Boolean = false
)

data class BankSmsParsedResult(
    val bank: BankType,
    val direction: SmsTransactionDirection,
    val amountRial: Long,
    val amountToman: Long,
    val detectedCurrency: DetectedCurrencyUnit = DetectedCurrencyUnit.RIAL,
    val templateId: String = "GENERIC_V1",
    val templateVersion: Int = 1,
    val isTemplateVerified: Boolean = false,
    val senderAddress: String? = null,
    val isSenderMatched: Boolean = true,
    val verificationStatus: SmsVerificationStatus = SmsVerificationStatus.AUTO_ACCEPTED,
    val reviewReasonFa: String? = null,
    val balanceRial: Long? = null,
    val balanceToman: Long? = null,
    val accountOrCard: String? = null,
    val destination: String? = null,
    val trackingCode: String? = null,
    val dateTimeString: String? = null,
    val merchantOrTerminal: String? = null,
    val suggestedCategory: String,
    val idempotencyKey: String,
    val isOtpOrServiceMessage: Boolean = false,
    val isFailedTransaction: Boolean = false,
    val summaryFa: String
)
