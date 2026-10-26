package com.example.sms

data class BankPatternRule(
    val templateId: String,
    val transactionType: BankTransactionType,
    val regex: Regex,
    val defaultCategory: String,
    val isRial: Boolean = true,
    val confidence: Double = 0.95,
    val titleExtractRegex: Regex? = null
)

data class BankDefinition(
    val bankCode: String,
    val bankName: String,
    val knownSenders: List<String>,
    val bodyKeywords: List<String>,
    val patterns: List<BankPatternRule>
)

object BankSmsPatternRegistry {

    val OTP_KEYWORDS = listOf(
        "رمز پویا", "رمز دوم", "کد تایید", "کد ورود", "کد فعالسازی",
        "کد امنیتی", "کد یکبار مصرف", "otp", "verification code"
    )

    val FAILED_KEYWORDS = listOf(
        "تراکنش ناموفق", "عدم موفقیت", "تراکنش نامعتبر", "موجودی ناکافی",
        "خطای تراکنش", "ناموفق", "لغو تراکنش"
    )

    val PROMOTIONAL_KEYWORDS = listOf(
        "جشنواره", "تسهیلات ویژه", "وام فوری", "قرعه کشی", "افتتاح حساب آنلاین",
        "کارت هدیه", "امتیاز باشگاه"
    )

    val BALANCE_ONLY_KEYWORDS = listOf(
        "اعلام مانده", "استعلام موجودی", "گزارش مانده"
    )

    private val BANKS: List<BankDefinition> = listOf(
        // --- 1. بانک ملت ---
        BankDefinition(
            bankCode = "MELLAT",
            bankName = "بانک ملت",
            knownSenders = listOf("mellat", "bankmellat", "981000", "+981000", "982000", "+982000"),
            bodyKeywords = listOf("بانک ملت", "ملت"),
            patterns = listOf(
                BankPatternRule(
                    templateId = "MELLAT_WITHDRAWAL_V1",
                    transactionType = BankTransactionType.EXPENSE,
                    regex = Regex("""برداشت(?:\s+مبلغ)?\s*[:\s]*([0-9,،\.]+)\s*(ریال|تومان)?"""),
                    defaultCategory = "خرید و خدمات"
                ),
                BankPatternRule(
                    templateId = "MELLAT_DEPOSIT_V1",
                    transactionType = BankTransactionType.INCOME,
                    regex = Regex("""واریز(?:\s+مبلغ)?\s*[:\s]*([0-9,،\.]+)\s*(ریال|تومان)?"""),
                    defaultCategory = "درآمد و حقوق"
                ),
                BankPatternRule(
                    templateId = "MELLAT_POS_V1",
                    transactionType = BankTransactionType.EXPENSE,
                    regex = Regex("""خرید(?:\s+با\s+کارت)?(?:\s+[0-9\*]+)?\s*[\n\r]*\s*مبلغ\s*[:\s]*([0-9,،\.]+)\s*(ریال|تومان)?"""),
                    defaultCategory = "خرید"
                )
            )
        ),

        // --- 2. بانک ملی ایران ---
        BankDefinition(
            bankCode = "MELLI",
            bankName = "بانک ملی ایران",
            knownSenders = listOf("melli", "bankmelli", "bmi", "982000", "+982000"),
            bodyKeywords = listOf("بانک ملی", "ملی ایران", "ملی"),
            patterns = listOf(
                BankPatternRule(
                    templateId = "MELLI_WITHDRAWAL_V1",
                    transactionType = BankTransactionType.EXPENSE,
                    regex = Regex("""(?:برداشت|خرید)(?:\s+مبلغ)?\s*[:\s]*([0-9,،\.]+)\s*(ریال|تومان)?"""),
                    defaultCategory = "خرید"
                ),
                BankPatternRule(
                    templateId = "MELLI_DEPOSIT_V1",
                    transactionType = BankTransactionType.INCOME,
                    regex = Regex("""واریز(?:\s+مبلغ)?\s*[:\s]*([0-9,،\.]+)\s*(ریال|تومان)?"""),
                    defaultCategory = "درآمد"
                ),
                BankPatternRule(
                    templateId = "MELLI_TRANSFER_V1",
                    transactionType = BankTransactionType.TRANSFER,
                    regex = Regex("""انتقال(?:\s+وجه)?\s*[:\s]*([0-9,،\.]+)\s*(ریال|تومان)?"""),
                    defaultCategory = "انتقال بین حسابی"
                )
            )
        ),

        // --- 3. بلوبانک سامان (BluBank) ---
        BankDefinition(
            bankCode = "BLUBANK",
            bankName = "بلوبانک سامان",
            knownSenders = listOf("blubank", "bluebank", "blu", "989000", "+989000"),
            bodyKeywords = listOf("بلوبانک", "بلو بانک", "blubank"),
            patterns = listOf(
                BankPatternRule(
                    templateId = "BLU_EXPENSE_V1",
                    transactionType = BankTransactionType.EXPENSE,
                    regex = Regex("""برداشت\s+([0-9,،\.]+)\s*(ریال|تومان)?(?:\s+بابت\s+([^\n]+))?"""),
                    defaultCategory = "خرید روزمره"
                ),
                BankPatternRule(
                    templateId = "BLU_INCOME_V1",
                    transactionType = BankTransactionType.INCOME,
                    regex = Regex("""واریز\s+([0-9,،\.]+)\s*(ریال|تومان)?(?:\s+بابت\s+([^\n]+))?"""),
                    defaultCategory = "درآمد"
                ),
                BankPatternRule(
                    templateId = "BLU_TRANSFER_V1",
                    transactionType = BankTransactionType.TRANSFER,
                    regex = Regex("""انتقال(?:\s+وجه)?\s+([0-9,،\.]+)\s*(ریال|تومان)?"""),
                    defaultCategory = "انتقال حساب"
                )
            )
        ),

        // --- 4. بانک سامان ---
        BankDefinition(
            bankCode = "SAMAN",
            bankName = "بانک سامان",
            knownSenders = listOf("saman", "banksaman", "sb"),
            bodyKeywords = listOf("بانک سامان", "سامان"),
            patterns = listOf(
                BankPatternRule(
                    templateId = "SAMAN_DEPOSIT_V1",
                    transactionType = BankTransactionType.INCOME,
                    regex = Regex("""واریز(?:\s+به\s+حساب)?[\n\r\s]*مبلغ\s*[:\s]*([0-9,،\.]+)\s*(ریال|تومان)?"""),
                    defaultCategory = "درآمد"
                ),
                BankPatternRule(
                    templateId = "SAMAN_WITHDRAWAL_V1",
                    transactionType = BankTransactionType.EXPENSE,
                    regex = Regex("""برداشت(?:\s+از\s+حساب)?[\n\r\s]*مبلغ\s*[:\s]*([0-9,،\.]+)\s*(ریال|تومان)?"""),
                    defaultCategory = "خرید و مصارف"
                )
            )
        ),

        // --- 5. بانک پاسارگاد و ویپاد ---
        BankDefinition(
            bankCode = "PASARGAD",
            bankName = "بانک پاسارگاد",
            knownSenders = listOf("pasargad", "bankpasargad", "bpi", "wepod", "vipod"),
            bodyKeywords = listOf("بانک پاسارگاد", "پاسارگاد", "ویپاد"),
            patterns = listOf(
                BankPatternRule(
                    templateId = "PASARGAD_EXPENSE_V1",
                    transactionType = BankTransactionType.EXPENSE,
                    regex = Regex("""(?:برداشت|خرید)(?:\s+از\s+حساب|\s+با\s+کارت)?(?:\s+[0-9\.\*]+)?[\n\r\s]*مبلغ\s*[:\s]*([0-9,،\.]+)\s*(ریال|تومان)?"""),
                    defaultCategory = "خرید"
                ),
                BankPatternRule(
                    templateId = "PASARGAD_INCOME_V1",
                    transactionType = BankTransactionType.INCOME,
                    regex = Regex("""واریز(?:\s+به\s+حساب)?(?:\s+[0-9\.\*]+)?[\n\r\s]*مبلغ\s*[:\s]*([0-9,،\.]+)\s*(ریال|تومان)?"""),
                    defaultCategory = "درآمد"
                )
            )
        ),

        // --- 6. بانک تجارت ---
        BankDefinition(
            bankCode = "TEJARAT",
            bankName = "بانک تجارت",
            knownSenders = listOf("tejarat", "banktejarat"),
            bodyKeywords = listOf("بانک تجارت", "تجارت"),
            patterns = listOf(
                BankPatternRule(
                    templateId = "TEJARAT_WITHDRAWAL_V1",
                    transactionType = BankTransactionType.EXPENSE,
                    regex = Regex("""برداشت(?:\s+مبلغ)?\s*[:\s]*([0-9,،\.]+)\s*(ریال|تومان)?"""),
                    defaultCategory = "خرید"
                ),
                BankPatternRule(
                    templateId = "TEJARAT_DEPOSIT_V1",
                    transactionType = BankTransactionType.INCOME,
                    regex = Regex("""واریز(?:\s+مبلغ)?\s*[:\s]*([0-9,،\.]+)\s*(ریال|تومان)?"""),
                    defaultCategory = "درآمد"
                )
            )
        ),

        // --- 7. بانک سپه ---
        BankDefinition(
            bankCode = "SEPAH",
            bankName = "بانک سپه",
            knownSenders = listOf("sepah", "banksepah", "ansar", "ghavamin"),
            bodyKeywords = listOf("بانک سپه", "سپه", "انصار", "قوامین"),
            patterns = listOf(
                BankPatternRule(
                    templateId = "SEPAH_WITHDRAWAL_V1",
                    transactionType = BankTransactionType.EXPENSE,
                    regex = Regex("""برداشت\s*[:\s]*([0-9,،\.]+)\s*(ریال|تومان)?"""),
                    defaultCategory = "خرید"
                ),
                BankPatternRule(
                    templateId = "SEPAH_DEPOSIT_V1",
                    transactionType = BankTransactionType.INCOME,
                    regex = Regex("""واریز\s*[:\s]*([0-9,،\.]+)\s*(ریال|تومان)?"""),
                    defaultCategory = "درآمد"
                )
            )
        ),

        // --- 8. بانک صادرات ایران ---
        BankDefinition(
            bankCode = "SADERAT",
            bankName = "بانک صادرات ایران",
            knownSenders = listOf("saderat", "banksaderat", "bsi"),
            bodyKeywords = listOf("بانک صادرات", "صادرات"),
            patterns = listOf(
                BankPatternRule(
                    templateId = "SADERAT_WITHDRAWAL_V1",
                    transactionType = BankTransactionType.EXPENSE,
                    regex = Regex("""برداشت(?:\s+مبلغ)?\s*[:\s]*([0-9,،\.]+)\s*(ریال|تومان)?"""),
                    defaultCategory = "خرید"
                ),
                BankPatternRule(
                    templateId = "SADERAT_DEPOSIT_V1",
                    transactionType = BankTransactionType.INCOME,
                    regex = Regex("""واریز(?:\s+مبلغ)?\s*[:\s]*([0-9,،\.]+)\s*(ریال|تومان)?"""),
                    defaultCategory = "درآمد"
                )
            )
        ),

        // --- 9. بانک پارسیان ---
        BankDefinition(
            bankCode = "PARSIAN",
            bankName = "بانک پارسیان",
            knownSenders = listOf("parsian", "bankparsian"),
            bodyKeywords = listOf("بانک پارسیان", "پارسیان"),
            patterns = listOf(
                BankPatternRule(
                    templateId = "PARSIAN_EXPENSE_V1",
                    transactionType = BankTransactionType.EXPENSE,
                    regex = Regex("""برداشت\s+([0-9,،\.]+)\s*(ریال|تومان)?"""),
                    defaultCategory = "خرید"
                ),
                BankPatternRule(
                    templateId = "PARSIAN_INCOME_V1",
                    transactionType = BankTransactionType.INCOME,
                    regex = Regex("""واریز\s+([0-9,،\.]+)\s*(ریال|تومان)?"""),
                    defaultCategory = "درآمد"
                )
            )
        ),

        // --- 10. بانک قرض‌الحسنه رسالت ---
        BankDefinition(
            bankCode = "RESALAT",
            bankName = "بانک قرض‌الحسنه رسالت",
            knownSenders = listOf("resalat", "rqbank"),
            bodyKeywords = listOf("بانک قرض الحسنه رسالت", "بانک رسالت", "رسالت"),
            patterns = listOf(
                BankPatternRule(
                    templateId = "RESALAT_EXPENSE_V1",
                    transactionType = BankTransactionType.EXPENSE,
                    regex = Regex("""برداشت(?:\s+مبلغ)?\s*[:\s]*([0-9,،\.]+)\s*(ریال|تومان)?"""),
                    defaultCategory = "خرید"
                ),
                BankPatternRule(
                    templateId = "RESALAT_INCOME_V1",
                    transactionType = BankTransactionType.INCOME,
                    regex = Regex("""واریز(?:\s+مبلغ)?\s*[:\s]*([0-9,،\.]+)\s*(ریال|تومان)?"""),
                    defaultCategory = "درآمد"
                )
            )
        )
    )

    fun getAllBanks(): List<BankDefinition> = BANKS

    fun findBank(senderAddress: String, body: String): BankDefinition? {
        val lowerSender = senderAddress.lowercase()
        val lowerBody = body.lowercase()
        return BANKS.firstOrNull { bank ->
            bank.knownSenders.any { lowerSender.contains(it) } ||
                bank.bodyKeywords.any { lowerBody.contains(it) }
        }
    }
}
