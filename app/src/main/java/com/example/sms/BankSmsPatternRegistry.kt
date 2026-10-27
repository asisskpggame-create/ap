package com.example.sms

/**
 * Defines a bank-specific or generic regex template for extracting financial fields from normalized SMS text.
 */
data class BankSmsTemplateRule(
    val ruleId: String,
    val transactionType: BankTransactionType,
    val amountRegex: Regex,
    val balanceRegex: Regex? = null,
    val defaultCurrencyIsRial: Boolean = true
)

/**
 * Defines an Iranian bank profile with sender identifiers, name keywords, and priority-ordered template rules.
 */
data class IranianBankDefinition(
    val bankId: String,
    val displayNameFa: String,
    val nameKeywords: List<String>,
    val knownSenders: List<String>,
    val templates: List<BankSmsTemplateRule>
)

/**
 * Extensible Registry of Iranian Bank SMS patterns.
 */
object BankSmsPatternRegistry {

    val otpExclusionKeywords: List<String> = listOf(
        "رمز پویا",
        "رمز دوم",
        "کد تایید",
        "کد تأیید",
        "کد امنیتی",
        "کد فعالسازی",
        "کد فعال سازی",
        "کد ورود",
        "کد یکبار مصرف",
        "رمز یکبار مصرف",
        "otp",
        "verification code",
        "activation code",
        "dynamic pin",
        "pin2"
    )

    val failedOrCancelledTransactionKeywords: List<String> = listOf(
        "تراکنش ناموفق",
        "عملیات ناموفق",
        "پرداخت ناموفق",
        "خرید ناموفق",
        "انتقال ناموفق",
        "ناموفق بود",
        "عدم موجودی",
        "موجودی کافی نیست",
        "خطا در تراکنش",
        "لغو شد",
        "رد شد",
        "برگشت خورده",
        "عدم موفقیت"
    )

    val nonFinancialAlertKeywords: List<String> = listOf(
        "ورود به همراه بانک",
        "ورود به اینترنت بانک",
        "ورود موفق به",
        "تغییر رمز",
        "جشنواره",
        "قرعه کشی",
        "قرعه‌کشی",
        "تبریک",
        "امتیاز باشگاه"
    )

    val refundReversalKeywords: List<String> = listOf(
        "برگشت خرید",
        "بازگشت وجه",
        "برگشت وجه",
        "رفع مغایرت",
        "اصلاحیه واریز",
        "واریز برگشتی",
        "استرداد وجه"
    )

    private const val AMOUNT_TOKEN_PATTERN = """([0-9]{1,3}(?:/[0-9]{3})+|[0-9]{1,3}(?:\.[0-9]{3}){2,}|[0-9,]+(?:\.[0-9]{1,2})?)"""

    private val commonBalanceRegex = Regex(
        """(?:مانده|موجودی|موجودي)(?:\s*قابل\s*برداشت|\s*حساب|\s*فعلی|\s*کل)?\s*[:：=-]?\s*$AMOUNT_TOKEN_PATTERN\s*(ریال|تومان|تومن|rial|irr|toman)?""",
        RegexOption.IGNORE_CASE
    )

    val feeAmountRegex = Regex(
        """(?:کارمزد|هزینه\s*کارمزد)\s*[:：=-]?\s*$AMOUNT_TOKEN_PATTERN\s*(ریال|تومان|تومن|rial|irr|toman)?""",
        RegexOption.IGNORE_CASE
    )

    private val standardDebitRegex = Regex(
        """(?<!برگشت\s)(?:برداشت|خرید|پرداخت|کاهش|کسر|حواله|انتقال)\s*(?:وجه|کارت|از\s*حساب|از\s*کارت|مبلغ)?\s*[:：=-]?\s*(?:مبلغ\s*[:：=-]?\s*)?$AMOUNT_TOKEN_PATTERN\s*(ریال|تومان|تومن|rial|irr|toman)""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

    private val standardCreditRegex = Regex(
        """(?:واریز|افزایش|بستانکار|دریافت|برگشت\s*خرید|بازگشت\s*وجه|اصلاحیه)\s*(?:به\s*حساب|به\s*کارت|وجه|سود|حقوق|مبلغ)?\s*[:：=-]?\s*(?:مبلغ\s*[:：=-]?\s*)?$AMOUNT_TOKEN_PATTERN\s*(ریال|تومان|تومن|rial|irr|toman)""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

    val banks: List<IranianBankDefinition> = listOf(
        // 1. BluBank (Saman) - Placed before Saman so "بلوبانک سامان" matches BluBank first
        IranianBankDefinition(
            bankId = "BLUBANK",
            displayNameFa = "بلوبانک سامان",
            nameKeywords = listOf("بلوبانک", "بلو بانک", "blubank", "blu bank"),
            knownSenders = listOf("blubank", "blu", "98200090"),
            templates = listOf(
                BankSmsTemplateRule(
                    ruleId = "blu_debit",
                    transactionType = BankTransactionType.EXPENSE,
                    amountRegex = Regex("""(?<!برگشت\s)(?:برداشت|خرید|پرداخت)\s*(?:مبلغ)?\s*[:：]?\s*$AMOUNT_TOKEN_PATTERN\s*(ریال|تومان|تومن)""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
                    balanceRegex = commonBalanceRegex
                ),
                BankSmsTemplateRule(
                    ruleId = "blu_credit",
                    transactionType = BankTransactionType.INCOME,
                    amountRegex = Regex("""(?:واریز|دریافت|افزایش| باکس |سود|برگشت\s*خرید|بازگشت\s*وجه)\s*(?:مبلغ)?\s*[:：]?\s*$AMOUNT_TOKEN_PATTERN\s*(ریال|تومان|تومن)""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
                    balanceRegex = commonBalanceRegex
                )
            )
        ),

        // 2. Bank Mellat
        IranianBankDefinition(
            bankId = "MELLAT",
            displayNameFa = "بانک ملت",
            nameKeywords = listOf("بانک ملت", "ملت", "mellat"),
            knownSenders = listOf("98200001", "200001", "mellat", "bankmellat"),
            templates = listOf(
                BankSmsTemplateRule(
                    ruleId = "mellat_debit",
                    transactionType = BankTransactionType.EXPENSE,
                    amountRegex = standardDebitRegex,
                    balanceRegex = commonBalanceRegex
                ),
                BankSmsTemplateRule(
                    ruleId = "mellat_credit",
                    transactionType = BankTransactionType.INCOME,
                    amountRegex = standardCreditRegex,
                    balanceRegex = commonBalanceRegex
                )
            )
        ),

        // 3. Bank Melli Iran
        IranianBankDefinition(
            bankId = "MELLI",
            displayNameFa = "بانک ملی ایران",
            nameKeywords = listOf("بانک ملی", "ملی ایران", "بام", "melli", "bmi"),
            knownSenders = listOf("98200011", "200011", "melli", "bmi", "bam"),
            templates = listOf(
                BankSmsTemplateRule(
                    ruleId = "melli_debit",
                    transactionType = BankTransactionType.EXPENSE,
                    amountRegex = standardDebitRegex,
                    balanceRegex = commonBalanceRegex
                ),
                BankSmsTemplateRule(
                    ruleId = "melli_credit",
                    transactionType = BankTransactionType.INCOME,
                    amountRegex = standardCreditRegex,
                    balanceRegex = commonBalanceRegex
                )
            )
        ),

        // 4. Bank Saman
        IranianBankDefinition(
            bankId = "SAMAN",
            displayNameFa = "بانک سامان",
            nameKeywords = listOf("بانک سامان", "سامان", "saman"),
            knownSenders = listOf("98200020", "200020", "saman", "sb24"),
            templates = listOf(
                BankSmsTemplateRule(
                    ruleId = "saman_debit",
                    transactionType = BankTransactionType.EXPENSE,
                    amountRegex = standardDebitRegex,
                    balanceRegex = commonBalanceRegex
                ),
                BankSmsTemplateRule(
                    ruleId = "saman_credit",
                    transactionType = BankTransactionType.INCOME,
                    amountRegex = standardCreditRegex,
                    balanceRegex = commonBalanceRegex
                )
            )
        ),

        // 5. Bank Pasargad & Wepod
        IranianBankDefinition(
            bankId = "PASARGAD",
            displayNameFa = "بانک پاسارگاد",
            nameKeywords = listOf("پاسارگاد", "ویپاد", "pasargad", "wepod", "bpi"),
            knownSenders = listOf("98200050", "200050", "pasargad", "wepod", "bpi"),
            templates = listOf(
                BankSmsTemplateRule(
                    ruleId = "pasargad_debit",
                    transactionType = BankTransactionType.EXPENSE,
                    amountRegex = standardDebitRegex,
                    balanceRegex = commonBalanceRegex
                ),
                BankSmsTemplateRule(
                    ruleId = "pasargad_credit",
                    transactionType = BankTransactionType.INCOME,
                    amountRegex = standardCreditRegex,
                    balanceRegex = commonBalanceRegex
                )
            )
        ),

        // 6. Bank Tejarat
        IranianBankDefinition(
            bankId = "TEJARAT",
            displayNameFa = "بانک تجارت",
            nameKeywords = listOf("بانک تجارت", "تجارت", "tejarat"),
            knownSenders = listOf("98200070", "200070", "tejarat"),
            templates = listOf(
                BankSmsTemplateRule(
                    ruleId = "tejarat_debit",
                    transactionType = BankTransactionType.EXPENSE,
                    amountRegex = standardDebitRegex,
                    balanceRegex = commonBalanceRegex
                ),
                BankSmsTemplateRule(
                    ruleId = "tejarat_credit",
                    transactionType = BankTransactionType.INCOME,
                    amountRegex = standardCreditRegex,
                    balanceRegex = commonBalanceRegex
                )
            )
        ),

        // 7. Bank Sepah
        IranianBankDefinition(
            bankId = "SEPAH",
            displayNameFa = "بانک سپه",
            nameKeywords = listOf("بانک سپه", "سپه", "امیدبانک", "sepah"),
            knownSenders = listOf("98200030", "200030", "sepah"),
            templates = listOf(
                BankSmsTemplateRule(
                    ruleId = "sepah_debit",
                    transactionType = BankTransactionType.EXPENSE,
                    amountRegex = standardDebitRegex,
                    balanceRegex = commonBalanceRegex
                ),
                BankSmsTemplateRule(
                    ruleId = "sepah_credit",
                    transactionType = BankTransactionType.INCOME,
                    amountRegex = standardCreditRegex,
                    balanceRegex = commonBalanceRegex
                )
            )
        ),

        // 8. Bank Saderat Iran
        IranianBankDefinition(
            bankId = "SADERAT",
            displayNameFa = "بانک صادرات",
            nameKeywords = listOf("بانک صادرات", "صادرات", "سپینو", "saderat", "bsi"),
            knownSenders = listOf("98200060", "200060", "saderat", "bsi"),
            templates = listOf(
                BankSmsTemplateRule(
                    ruleId = "saderat_debit",
                    transactionType = BankTransactionType.EXPENSE,
                    amountRegex = standardDebitRegex,
                    balanceRegex = commonBalanceRegex
                ),
                BankSmsTemplateRule(
                    ruleId = "saderat_credit",
                    transactionType = BankTransactionType.INCOME,
                    amountRegex = standardCreditRegex,
                    balanceRegex = commonBalanceRegex
                )
            )
        ),

        // 9. Bank Parsian
        IranianBankDefinition(
            bankId = "PARSIAN",
            displayNameFa = "بانک پارسیان",
            nameKeywords = listOf("بانک پارسیان", "پارسیان", "parsian"),
            knownSenders = listOf("98200040", "200040", "parsian"),
            templates = listOf(
                BankSmsTemplateRule(
                    ruleId = "parsian_debit",
                    transactionType = BankTransactionType.EXPENSE,
                    amountRegex = standardDebitRegex,
                    balanceRegex = commonBalanceRegex
                ),
                BankSmsTemplateRule(
                    ruleId = "parsian_credit",
                    transactionType = BankTransactionType.INCOME,
                    amountRegex = standardCreditRegex,
                    balanceRegex = commonBalanceRegex
                )
            )
        ),

        // 10. Bank Resalat
        IranianBankDefinition(
            bankId = "RESALAT",
            displayNameFa = "بانک رسالت",
            nameKeywords = listOf("بانک رسالت", "قرض الحسنه رسالت", "رسالت", "resalat"),
            knownSenders = listOf("98200080", "200080", "resalat"),
            templates = listOf(
                BankSmsTemplateRule(
                    ruleId = "resalat_debit",
                    transactionType = BankTransactionType.EXPENSE,
                    amountRegex = standardDebitRegex,
                    balanceRegex = commonBalanceRegex
                ),
                BankSmsTemplateRule(
                    ruleId = "resalat_credit",
                    transactionType = BankTransactionType.INCOME,
                    amountRegex = standardCreditRegex,
                    balanceRegex = commonBalanceRegex
                )
            )
        ),

        // 11. Additional major Iranian banks (Keshavarzi, Maskan, Ayandeh, Refah, Shahr, Karafarin, Sina, Eghtesad Novin, Mehr Iran, Gardeshgari, Post Bank, Dey)
        IranianBankDefinition(
            bankId = "KESHAVARZI",
            displayNameFa = "بانک کشاورزی",
            nameKeywords = listOf("بانک کشاورزی", "کشاورزی", "bki"),
            knownSenders = listOf("keshavarzi", "bki"),
            templates = listOf(
                BankSmsTemplateRule("keshavarzi_debit", BankTransactionType.EXPENSE, standardDebitRegex, commonBalanceRegex),
                BankSmsTemplateRule("keshavarzi_credit", BankTransactionType.INCOME, standardCreditRegex, commonBalanceRegex)
            )
        ),
        IranianBankDefinition(
            bankId = "MASKAN",
            displayNameFa = "بانک مسکن",
            nameKeywords = listOf("بانک مسکن", "مسکن", "maskan"),
            knownSenders = listOf("maskan", "bankmaskan"),
            templates = listOf(
                BankSmsTemplateRule("maskan_debit", BankTransactionType.EXPENSE, standardDebitRegex, commonBalanceRegex),
                BankSmsTemplateRule("maskan_credit", BankTransactionType.INCOME, standardCreditRegex, commonBalanceRegex)
            )
        ),
        IranianBankDefinition(
            bankId = "AYANDEH",
            displayNameFa = "بانک آینده",
            nameKeywords = listOf("بانک آینده", "آینده", "آبان‌تتر", "ayandeh"),
            knownSenders = listOf("ayandeh", "ba24"),
            templates = listOf(
                BankSmsTemplateRule("ayandeh_debit", BankTransactionType.EXPENSE, standardDebitRegex, commonBalanceRegex),
                BankSmsTemplateRule("ayandeh_credit", BankTransactionType.INCOME, standardCreditRegex, commonBalanceRegex)
            )
        ),
        IranianBankDefinition(
            bankId = "REFAH",
            displayNameFa = "بانک رفاه",
            nameKeywords = listOf("بانک رفاه", "رفاه کارگران", "رفاه", "refah"),
            knownSenders = listOf("refah", "refahbank"),
            templates = listOf(
                BankSmsTemplateRule("refah_debit", BankTransactionType.EXPENSE, standardDebitRegex, commonBalanceRegex),
                BankSmsTemplateRule("refah_credit", BankTransactionType.INCOME, standardCreditRegex, commonBalanceRegex)
            )
        ),
        IranianBankDefinition(
            bankId = "MEHR_IRAN",
            displayNameFa = "بانک مهر ایران",
            nameKeywords = listOf("مهر ایران", "قرض الحسنه مهر", "mehriran"),
            knownSenders = listOf("mehriran", "qmb"),
            templates = listOf(
                BankSmsTemplateRule("mehr_debit", BankTransactionType.EXPENSE, standardDebitRegex, commonBalanceRegex),
                BankSmsTemplateRule("mehr_credit", BankTransactionType.INCOME, standardCreditRegex, commonBalanceRegex)
            )
        ),
        IranianBankDefinition(
            bankId = "SHAHR",
            displayNameFa = "بانک شهر",
            nameKeywords = listOf("بانک شهر", "شهر", "shahr"),
            knownSenders = listOf("shahr", "shahrbank"),
            templates = listOf(
                BankSmsTemplateRule("shahr_debit", BankTransactionType.EXPENSE, standardDebitRegex, commonBalanceRegex),
                BankSmsTemplateRule("shahr_credit", BankTransactionType.INCOME, standardCreditRegex, commonBalanceRegex)
            )
        ),
        IranianBankDefinition(
            bankId = "EGHTESAD_NOVIN",
            displayNameFa = "بانک اقتصاد نوین",
            nameKeywords = listOf("اقتصاد نوین", "اقتصادنوین", "enbank"),
            knownSenders = listOf("enbank", "novin"),
            templates = listOf(
                BankSmsTemplateRule("en_debit", BankTransactionType.EXPENSE, standardDebitRegex, commonBalanceRegex),
                BankSmsTemplateRule("en_credit", BankTransactionType.INCOME, standardCreditRegex, commonBalanceRegex)
            )
        ),
        IranianBankDefinition(
            bankId = "KARAFARIN",
            displayNameFa = "بانک کارآفرین",
            nameKeywords = listOf("بانک کارآفرین", "کارآفرین", "karafarin"),
            knownSenders = listOf("karafarin"),
            templates = listOf(
                BankSmsTemplateRule("karafarin_debit", BankTransactionType.EXPENSE, standardDebitRegex, commonBalanceRegex),
                BankSmsTemplateRule("karafarin_credit", BankTransactionType.INCOME, standardCreditRegex, commonBalanceRegex)
            )
        ),
        IranianBankDefinition(
            bankId = "SINA",
            displayNameFa = "بانک سینا",
            nameKeywords = listOf("بانک سینا", "سینا", "sinabank"),
            knownSenders = listOf("sinabank", "sina"),
            templates = listOf(
                BankSmsTemplateRule("sina_debit", BankTransactionType.EXPENSE, standardDebitRegex, commonBalanceRegex),
                BankSmsTemplateRule("sina_credit", BankTransactionType.INCOME, standardCreditRegex, commonBalanceRegex)
            )
        ),
        IranianBankDefinition(
            bankId = "GARDESHGARI",
            displayNameFa = "بانک گردشگری",
            nameKeywords = listOf("بانک گردشگری", "گردشگری", "توبانک", "tobank"),
            knownSenders = listOf("gardeshgari", "tobank"),
            templates = listOf(
                BankSmsTemplateRule("gardeshgari_debit", BankTransactionType.EXPENSE, standardDebitRegex, commonBalanceRegex),
                BankSmsTemplateRule("gardeshgari_credit", BankTransactionType.INCOME, standardCreditRegex, commonBalanceRegex)
            )
        )
    )

    fun getCommonBalanceRegex(): Regex = commonBalanceRegex

    /**
     * Resolves the bank definition by checking explicit bank name keywords in the SMS body FIRST
     * (preventing shared sender prefix collisions such as 982000), and falling back to unique sender numbers.
     */
    fun findBank(normalizedText: String, senderNumber: String = ""): IranianBankDefinition? {
        val lowerText = normalizedText.lowercase()
        // 1. Check full/specific bank phrases in SMS body first (e.g. "بانک ملی", "بانک ملت", "بلوبانک")
        for (bank in banks) {
            if (bank.nameKeywords.any { kw -> kw.contains(" ") && lowerText.contains(kw.lowercase()) }) {
                return bank
            }
        }
        // 2. Check single-word bank keywords in SMS body
        for (bank in banks) {
            if (bank.nameKeywords.any { kw -> lowerText.contains(kw.lowercase()) }) {
                return bank
            }
        }
        // 3. Fallback to sender number only when body has no bank name
        val lowerSender = senderNumber.trim().lowercase().removePrefix("+")
        if (lowerSender.isNotBlank()) {
            // Shared prefix 982000 without sub-bank suffix is shared across Melli/Mellat/Saman gateways
            if (lowerSender == "982000" || lowerSender == "2000") {
                return null
            }
            val matchingSenderBanks = banks.filter { bank ->
                bank.knownSenders.any { lowerSender.contains(it.lowercase()) }
            }
            if (matchingSenderBanks.size == 1) {
                return matchingSenderBanks.first()
            }
        }
        return null
    }

    fun isAmbiguousSharedSender(normalizedText: String, senderNumber: String): Boolean {
        val lowerSender = senderNumber.trim().lowercase().removePrefix("+")
        if (lowerSender != "982000" && lowerSender != "2000") return false
        val lowerText = normalizedText.lowercase()
        return banks.none { bank -> bank.nameKeywords.any { kw -> lowerText.contains(kw.lowercase()) } }
    }
}
