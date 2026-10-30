package com.example.personallifeos.sms

enum class BankType(val faName: String, val brandColorHex: Long) {
    MELLI("بانک ملی ایران", 0xFFEAB308),
    MELLAT("بانک ملت", 0xFFE11D48),
    SADERAT("بانک صادرات ایران", 0xFF2563EB),
    TEJARAT("بانک تجارت", 0xFF0284C7),
    SEPAH("بانک سپه", 0xFFF59E0B),
    BLUBANK("بلوبانک", 0xFF06B6D4),
    VEPOD("ویپاد (پاسارگاد)", 0xFF10B981),
    MASKAN("بانک مسکن", 0xFFF97316),
    KESHAVARZI("بانک کشاورزی", 0xFF16A34A),
    TOSEE_TAAVON("بانک توسعه تعاون", 0xFF0D9488),
    SANAT_MADAN("بانک صنعت و معدن", 0xFF6366F1),
    TOSEE_SADERAT("بانک توسعه صادرات", 0xFF4F46E5),
    POST_BANK("پست بانک ایران", 0xFF059669),
    REFAH("بانک رفاه کارگران", 0xFF84CC16),
    MEHR_IRAN("بانک مهر ایران", 0xFF14B8A6),
    RESALAT("بانک قرض‌الحسنه رسالت", 0xFF3B82F6),
    PASARGAD("بانک پاسارگاد", 0xFFD97706),
    SAMAN("بانک سامان", 0xFF0284C7),
    PARSIAN("بانک پارسیان", 0xFF7C3AED),
    AYANDEH("بانک آینده", 0xFF9333EA),
    SHAHR("بانک شهر", 0xFFDC2626),
    GARDESHGARI("بانک گردشگری", 0xFFD97706),
    EGHTESAD_NOVIN("بانک اقتصاد نوین", 0xFF9333EA),
    SINA("بانک سینا", 0xFF0284C7),
    DAY("بانک دی", 0xFFEA580C),
    KARAFARIN("بانک کارآفرین", 0xFF16A34A),
    IRAN_ZAMIN("بانک ایران زمین", 0xFF0D9488),
    SARMAYE("بانک سرمایه", 0xFF2563EB),
    KHAVARMIANEH("بانک خاورمیانه", 0xFF475569),
    MELAL("مؤسسه اعتباری ملل", 0xFF059669),
    KOSAR("مؤسسه اعتباری کوثر", 0xFFF59E0B),
    NOOR("مؤسسه اعتباری نور", 0xFFEAB308),
    UNKNOWN("بانک ناشناس", 0xFF64748B)
}

enum class SmsTransactionDirection(val faName: String, val isExpense: Boolean) {
    WITHDRAWAL("برداشت", true),
    PURCHASE("خرید", true),
    DEPOSIT("واریز", false),
    TRANSFER_POL("انتقال پل", true),
    TRANSFER_PAYA("انتقال پایا", true),
    TRANSFER_CARD("انتقال کارت", true),
    UNKNOWN("نامشخص", true)
}

data class BankSmsParsedResult(
    val bank: BankType,
    val direction: SmsTransactionDirection,
    val amountRial: Long,
    val amountToman: Long,
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
    val summaryFa: String
)
