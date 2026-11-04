package com.example.personallifeos.data.repository

import com.example.personallifeos.data.local.DebtEntity
import com.example.personallifeos.data.local.ExpenseEntity
import com.example.personallifeos.data.local.HabitEntity
import com.example.personallifeos.data.local.HealthLogEntity
import com.example.personallifeos.data.local.LifeOsDao
import com.example.personallifeos.data.local.ReminderEntity
import com.example.personallifeos.data.local.TaskEntity
import com.example.personallifeos.data.local.TopicEntity
import com.example.personallifeos.data.local.VaultNoteEntity
import com.example.personallifeos.data.model.DebtItem
import com.example.personallifeos.data.model.ExpenseItem
import com.example.personallifeos.data.model.HabitItem
import com.example.personallifeos.data.model.HealthLog
import com.example.personallifeos.data.model.ReminderItem
import com.example.personallifeos.data.model.TaskItem
import com.example.personallifeos.data.model.TopicItem
import com.example.personallifeos.data.model.VaultNote
import com.example.personallifeos.data.local.BankConfigEntity
import com.example.personallifeos.data.local.SmsProcessingHistoryEntity
import com.example.personallifeos.data.model.TransactionType
import com.example.personallifeos.sms.BankSmsParsedResult
import com.example.personallifeos.sms.BankSmsProcessingEngine
import com.example.personallifeos.sms.BankUserConfig
import com.example.personallifeos.sms.SmsProcessingChannel
import com.example.personallifeos.sms.SmsProcessingResult
import com.example.personallifeos.sms.SmsSenderSource
import com.example.personallifeos.sms.SmsTransactionDirection
import com.example.personallifeos.sms.SmsVerificationStatus
import com.example.personallifeos.util.PersianUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class LifeOsRepository(private val dao: LifeOsDao) {

    val allBankConfigs: Flow<List<BankUserConfig>> = dao.getAllBankConfigs().map { list ->
        list.map { it.toDomain() }
    }

    val allProcessingHistory: Flow<List<SmsProcessingHistoryEntity>> = dao.getAllProcessingHistory()

    val pendingReviewHistory: Flow<List<SmsProcessingHistoryEntity>> = dao.getPendingReviewHistory()

    val allTasks: Flow<List<TaskItem>> = dao.getAllTasks().map { list ->
        list.map { it.toDomain() }
    }

    val allTopics: Flow<List<TopicItem>> = dao.getAllTopics().map { list ->
        list.map { it.toDomain() }
    }

    val allReminders: Flow<List<ReminderItem>> = dao.getAllReminders().map { list ->
        list.map { it.toDomain() }
    }

    val allHabits: Flow<List<HabitItem>> = dao.getAllHabits().map { list ->
        list.map { it.toDomain() }
    }

    val allExpenses: Flow<List<ExpenseItem>> = dao.getAllExpenses().map { list ->
        list.map { it.toDomain() }
    }

    val allDebts: Flow<List<DebtItem>> = dao.getAllDebts().map { list ->
        list.map { it.toDomain() }
    }

    val healthLog: Flow<HealthLog?> = dao.getHealthLog().map { it?.toDomain() }

    val allVaultNotes: Flow<List<VaultNote>> = dao.getAllVaultNotes().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun insertTask(task: TaskItem) {
        dao.insertTask(TaskEntity.fromDomain(task))
    }

    suspend fun insertTasks(tasks: List<TaskItem>) {
        dao.insertTasks(tasks.map { TaskEntity.fromDomain(it) })
    }

    suspend fun updateTask(task: TaskItem) {
        dao.updateTask(TaskEntity.fromDomain(task))
    }

    suspend fun deleteTask(taskId: String) {
        dao.deleteTask(taskId)
    }

    suspend fun deleteTasksByTopicId(topicId: String) {
        dao.deleteTasksByTopicId(topicId)
    }

    suspend fun detachTasksFromTopic(topicId: String) {
        dao.detachTasksFromTopic(topicId)
    }

    suspend fun insertTopic(topic: TopicItem) {
        dao.insertTopic(TopicEntity.fromDomain(topic))
    }

    suspend fun updateTopic(topic: TopicItem) {
        dao.updateTopic(TopicEntity.fromDomain(topic))
    }

    suspend fun deleteTopic(topicId: String, deleteChildTasks: Boolean = false) {
        if (deleteChildTasks) {
            dao.deleteTasksByTopicId(topicId)
        } else {
            dao.detachTasksFromTopic(topicId)
        }
        dao.deleteRemindersByTopic(topicId)
        dao.deleteTopic(topicId)
    }

    suspend fun upsertReminder(reminder: ReminderItem) {
        dao.insertReminder(ReminderEntity.fromDomain(reminder))
    }

    suspend fun deleteReminder(reminderId: String) {
        dao.deleteReminder(reminderId)
    }

    suspend fun deleteReminderByEntity(entityType: String, entityId: String) {
        dao.deleteReminderByEntity(entityType, entityId)
    }

    suspend fun insertHabit(habit: HabitItem) {
        dao.insertHabit(HabitEntity.fromDomain(habit))
    }

    suspend fun updateHabit(habit: HabitItem) {
        dao.updateHabit(HabitEntity.fromDomain(habit))
    }

    suspend fun insertExpense(expense: ExpenseItem): Long {
        return dao.insertExpense(ExpenseEntity.fromDomain(expense))
    }

    suspend fun updateExpense(expense: ExpenseItem) {
        dao.updateExpense(ExpenseEntity.fromDomain(expense))
    }

    suspend fun getExpenseByIdempotencyKey(key: String): ExpenseItem? {
        return dao.getExpenseByIdempotencyKey(key)?.toDomain()
    }

    suspend fun deleteExpense(expenseId: String) {
        dao.deleteExpense(expenseId)
    }

    suspend fun insertDebt(debt: DebtItem) {
        dao.insertDebt(DebtEntity.fromDomain(debt))
    }

    suspend fun updateDebt(debt: DebtItem) {
        dao.updateDebt(DebtEntity.fromDomain(debt))
    }

    suspend fun deleteDebt(debtId: String) {
        dao.deleteDebt(debtId)
    }

    suspend fun updateHealthLog(log: HealthLog) {
        dao.insertOrUpdateHealthLog(HealthLogEntity.fromDomain(log))
    }

    suspend fun insertVaultNote(note: VaultNote) {
        dao.insertVaultNote(VaultNoteEntity.fromDomain(note))
    }

    suspend fun deleteVaultNote(noteId: String) {
        dao.deleteVaultNote(noteId)
    }

    // Bank Configurations Persistence
    suspend fun saveBankConfig(config: BankUserConfig): Long {
        BankSmsProcessingEngine.configureBankSenderPolicy(
            bank = config.bank,
            senderNumber = config.senderNumber,
            requireSenderMatch = config.requireSenderMatch,
            verifyDefaultTemplate = false,
            customBankName = config.customBankName,
            isEnabled = config.isEnabled
        )
        return dao.insertBankConfig(BankConfigEntity.fromDomain(config))
    }

    suspend fun toggleBankConfig(bank: com.example.personallifeos.sms.BankType, isEnabled: Boolean) {
        val existing = dao.getBankConfigByKey(bank.name)
        if (existing != null) {
            val updated = existing.copy(isEnabled = isEnabled, updatedAt = System.currentTimeMillis())
            dao.insertBankConfig(updated)
            BankSmsProcessingEngine.toggleBankConfig(bank, isEnabled)
        }
    }

    suspend fun deleteBankConfig(bankNameKey: String) {
        val bank = try { com.example.personallifeos.sms.BankType.valueOf(bankNameKey) } catch (_: Exception) { null }
        if (bank != null) {
            BankSmsProcessingEngine.removeConfiguredBank(bank)
        }
        dao.deleteBankConfig(bankNameKey)
    }

    // SMS Processing History
    suspend fun insertProcessingHistory(record: SmsProcessingHistoryEntity): Long {
        return dao.insertProcessingHistory(record)
    }

    suspend fun clearSmsProcessingHistory() {
        dao.clearProcessingHistory()
    }

    suspend fun deleteSmsProcessingHistory(id: String) {
        dao.deleteProcessingHistory(id)
    }

    data class SmsRegistrationResult(
        val isSuccess: Boolean,
        val isDuplicate: Boolean,
        val assignedExpenseId: String? = null,
        val errorMessageFa: String? = null,
        val status: SmsProcessingResult
    )

    /**
     * Central Transaction Registration Service (Audit Section 6).
     * Single authority for verifying duplicates in Room, inserting into expenses,
     * recording processing history, and updating engine idempotency states.
     */
    suspend fun registerSmsTransaction(
        parsed: BankSmsParsedResult,
        channel: SmsProcessingChannel,
        senderSource: SmsSenderSource = SmsSenderSource.REAL_DEVICE_INBOX,
        overrideType: TransactionType? = null,
        overrideAmountToman: Long? = null,
        overrideTitle: String? = null
    ): SmsRegistrationResult {
        val key = parsed.idempotencyKey

        // 1. Check persistent duplicate in expenses
        val existingExpense = dao.getExpenseByIdempotencyKey(key)
        if (existingExpense != null || BankSmsProcessingEngine.isDuplicateKey(key)) {
            val historyId = UUID.randomUUID().toString()
            dao.insertProcessingHistory(
                SmsProcessingHistoryEntity(
                    id = historyId,
                    timestampMillis = System.currentTimeMillis(),
                    receivedTimeFa = PersianUtils.getPersianDateString(),
                    senderAddress = parsed.senderAddress ?: "نامشخص",
                    senderSource = senderSource.name,
                    detectedBank = parsed.bank.name,
                    bankFaName = parsed.bank.faName,
                    transactionDirection = parsed.direction.name,
                    rawExtractedAmount = parsed.rawExtractedAmount,
                    finalAmountToman = parsed.amountToman,
                    detectedCurrency = parsed.detectedCurrency.name,
                    templateId = parsed.templateId,
                    templateVersion = parsed.templateVersion,
                    verificationStatus = SmsVerificationStatus.DUPLICATE.name,
                    reviewReasonFa = "این تراکنش قبلاً در دفتر مالی یا تاریخچه ثبت شده است",
                    processingResult = SmsProcessingResult.DUPLICATE.name,
                    errorDetails = null,
                    idempotencyKey = key,
                    linkedExpenseId = existingExpense?.id,
                    processingChannel = channel.name,
                    maskedCardOrAccount = parsed.accountOrCard?.let { PersianUtils.maskCardNumber(it) },
                    trackingCode = parsed.trackingCode
                )
            )
            return SmsRegistrationResult(
                isSuccess = false,
                isDuplicate = true,
                errorMessageFa = "این تراکنش قبلاً ثبت شده است (مسدود شده توسط کلید ضدتکرار)",
                status = SmsProcessingResult.DUPLICATE
            )
        }

        // 2. Strict Direction Check: UNKNOWN must never become an expense
        val finalType = overrideType ?: when (parsed.direction) {
            SmsTransactionDirection.DEPOSIT -> TransactionType.INCOME
            SmsTransactionDirection.TRANSFER_CARD,
            SmsTransactionDirection.TRANSFER_PAYA,
            SmsTransactionDirection.TRANSFER_POL -> TransactionType.TRANSFER
            SmsTransactionDirection.WITHDRAWAL,
            SmsTransactionDirection.PURCHASE -> TransactionType.EXPENSE
            SmsTransactionDirection.UNKNOWN -> return SmsRegistrationResult(
                isSuccess = false,
                isDuplicate = false,
                errorMessageFa = "نوع تراکنش نامشخص است و نمی‌تواند به عنوان هزینه ثبت شود",
                status = SmsProcessingResult.NEEDS_REVIEW
            )
        }

        val finalAmount = overrideAmountToman ?: parsed.amountToman
        val autoTitle = overrideTitle ?: when {
            !parsed.merchantOrTerminal.isNullOrBlank() -> "خرید از ${parsed.merchantOrTerminal}"
            !parsed.destination.isNullOrBlank() -> "انتقال به ${parsed.destination}"
            finalType == TransactionType.INCOME -> "واریز به ${parsed.bank.shortNameFa}"
            finalType == TransactionType.TRANSFER -> "انتقال وجه (${parsed.bank.shortNameFa})"
            else -> "برداشت از ${parsed.bank.shortNameFa}"
        }

        val expenseId = UUID.randomUUID().toString()
        val expenseEntity = ExpenseEntity(
            id = expenseId,
            title = autoTitle,
            amount = finalAmount,
            category = parsed.suggestedCategory,
            date = PersianUtils.getPersianDateString(),
            isSmsParsed = true,
            bankName = parsed.bank.faName,
            type = finalType.name,
            accountName = parsed.bank.faName,
            idempotencyKey = key
        )

        val insertedRow = dao.insertExpense(expenseEntity)
        if (insertedRow == -1L) {
            return SmsRegistrationResult(
                isSuccess = false,
                isDuplicate = true,
                errorMessageFa = "تراکنش به دلیل قید یکتایی در دیتابیس درج نشد",
                status = SmsProcessingResult.DUPLICATE
            )
        }

        // Successfully inserted into database -> mark in engine
        BankSmsProcessingEngine.markIdempotencyKeyProcessed(key)

        val outcomeResult = if (channel == SmsProcessingChannel.LIVE_RECEIVER) {
            SmsProcessingResult.AUTO_REGISTERED
        } else {
            SmsProcessingResult.MANUALLY_CONFIRMED
        }

        val historyId = UUID.randomUUID().toString()
        dao.insertProcessingHistory(
            SmsProcessingHistoryEntity(
                id = historyId,
                timestampMillis = System.currentTimeMillis(),
                receivedTimeFa = PersianUtils.getPersianDateString(),
                senderAddress = parsed.senderAddress ?: "نامشخص",
                senderSource = senderSource.name,
                detectedBank = parsed.bank.name,
                bankFaName = parsed.bank.faName,
                transactionDirection = parsed.direction.name,
                rawExtractedAmount = parsed.rawExtractedAmount,
                finalAmountToman = finalAmount,
                detectedCurrency = parsed.detectedCurrency.name,
                templateId = parsed.templateId,
                templateVersion = parsed.templateVersion,
                verificationStatus = parsed.verificationStatus.name,
                reviewReasonFa = parsed.reviewReasonFa,
                processingResult = outcomeResult.name,
                errorDetails = null,
                idempotencyKey = key,
                linkedExpenseId = expenseId,
                processingChannel = channel.name,
                maskedCardOrAccount = parsed.accountOrCard?.let { PersianUtils.maskCardNumber(it) },
                trackingCode = parsed.trackingCode,
                userConfirmedTimestamp = if (outcomeResult == SmsProcessingResult.MANUALLY_CONFIRMED) System.currentTimeMillis() else null
            )
        )

        return SmsRegistrationResult(
            isSuccess = true,
            isDuplicate = false,
            assignedExpenseId = expenseId,
            status = outcomeResult
        )
    }

    suspend fun resetAllData() {
        dao.clearTasks()
        dao.clearTopics()
        dao.clearReminders()
        dao.clearHabits()
        dao.clearExpenses()
        dao.clearDebts()
        dao.clearVaultNotes()
        dao.clearBankConfigs()
        dao.clearProcessingHistory()
    }
}
