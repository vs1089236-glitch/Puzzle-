package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.JioPayDatabase
import com.example.data.local.entity.MoneyPlanEntity
import com.example.data.local.entity.PaymentAccountEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.UserEntity
import com.example.util.Formatters
import com.example.util.SecurityUtils
import kotlinx.coroutines.flow.Flow

class JioPayRepository(private val database: JioPayDatabase) {
    private val userDao = database.userDao()
    private val accountDao = database.paymentAccountDao()
    private val planDao = database.moneyPlanDao()
    private val transactionDao = database.transactionDao()

    fun observeUser(userId: Long): Flow<UserEntity?> = userDao.observeUserById(userId)

    fun observeAllUsers(): Flow<List<UserEntity>> = userDao.observeAllUsers()

    fun observePaymentAccounts(userId: Long): Flow<List<PaymentAccountEntity>> =
        accountDao.observeAccountsForUser(userId)

    fun observeMarketplacePlans(): Flow<List<MoneyPlanEntity>> =
        planDao.observeActiveMarketplacePlans()

    fun observeSellerPlans(sellerId: Long): Flow<List<MoneyPlanEntity>> =
        planDao.observePlansBySeller(sellerId)

    fun observePendingVerificationsForSeller(sellerId: Long): Flow<List<MoneyPlanEntity>> =
        planDao.observePendingVerificationsForSeller(sellerId)

    fun observeAllPendingVerifications(): Flow<List<MoneyPlanEntity>> =
        planDao.observeAllPendingVerifications()

    fun observeUserTransactions(userId: Long): Flow<List<TransactionEntity>> =
        transactionDao.observeUserTransactions(userId)

    fun observeAllTransactions(): Flow<List<TransactionEntity>> =
        transactionDao.observeAllTransactions()

    suspend fun findUserByMobile(mobile: String): UserEntity? =
        userDao.getUserByMobile(mobile.trim())

    suspend fun loginWithPassword(identifier: String, rawPassword: String): Result<UserEntity> {
        val cleanId = identifier.trim()
        if (cleanId.isBlank() || rawPassword.isBlank()) {
            return Result.failure(IllegalArgumentException("Enter your mobile/email and password."))
        }
        val user = userDao.getUserByMobileOrEmail(cleanId)
            ?: return Result.failure(IllegalArgumentException("Account not found. Check your mobile/email or register."))
        val expectedHash = SecurityUtils.hashPassword(rawPassword, user.mobile)
        if (user.passwordHash != expectedHash) {
            return Result.failure(IllegalArgumentException("Incorrect password. Please try again."))
        }
        if (user.isBlocked) {
            return Result.failure(IllegalArgumentException("Your account has been blocked by Admin."))
        }
        return Result.success(user)
    }

    suspend fun loginOrCreateWithOtp(
        mobile: String,
        newUserName: String,
        newUserUpi: String
    ): Result<UserEntity> {
        val cleanMobile = mobile.trim()
        if (!cleanMobile.matches(Regex("^[6-9][0-9]{9}$"))) {
            return Result.failure(IllegalArgumentException("Enter a valid 10-digit Indian mobile number."))
        }
        val existing = userDao.getUserByMobile(cleanMobile)
        if (existing != null) {
            if (existing.isBlocked) {
                return Result.failure(IllegalArgumentException("Your account has been blocked by Admin."))
            }
            return Result.success(existing)
        }
        val displayName = newUserName.trim().ifBlank { "JioPay User ${cleanMobile.takeLast(4)}" }
        val cleanUpi = newUserUpi.trim()
        if (cleanUpi.isNotEmpty() && !SecurityUtils.isValidUpiId(cleanUpi)) {
            return Result.failure(IllegalArgumentException("Enter a valid UPI ID (e.g. name@upi) or leave blank."))
        }
        return database.withTransaction {
            val userId = userDao.insertUser(
                UserEntity(
                    fullName = displayName,
                    mobile = cleanMobile,
                    email = "",
                    passwordHash = SecurityUtils.hashPassword("otp-$cleanMobile", cleanMobile),
                    primaryUpiId = cleanUpi,
                    walletBalance = 0.0,
                    totalCommissionEarned = 0.0,
                    isAdmin = false
                )
            )
            if (cleanUpi.isNotEmpty()) {
                accountDao.insertAccount(
                    PaymentAccountEntity(
                        userId = userId,
                        accountType = "UPI",
                        upiId = cleanUpi,
                        accountHolderName = displayName,
                        bankName = "Primary UPI ID",
                        isPrimary = true
                    )
                )
            }
            val created = userDao.getUserById(userId)
                ?: throw IllegalStateException("Failed to create user profile")
            Result.success(created)
        }
    }

    suspend fun registerUser(
        fullName: String,
        mobile: String,
        email: String,
        rawPassword: String,
        upiId: String
    ): Result<UserEntity> {
        val cleanName = fullName.trim()
        val cleanMobile = mobile.trim()
        val cleanEmail = email.trim()
        val cleanUpi = upiId.trim()

        if (cleanName.length < 2) {
            return Result.failure(IllegalArgumentException("Enter your full name."))
        }
        if (!cleanMobile.matches(Regex("^[6-9][0-9]{9}$"))) {
            return Result.failure(IllegalArgumentException("Enter a valid 10-digit Indian mobile number."))
        }
        if (rawPassword.length < 4) {
            return Result.failure(IllegalArgumentException("Password must be at least 4 characters."))
        }
        if (cleanUpi.isNotEmpty() && !SecurityUtils.isValidUpiId(cleanUpi)) {
            return Result.failure(IllegalArgumentException("Enter a valid UPI ID (e.g. rahul@okicici) or leave blank."))
        }
        if (userDao.getUserByMobile(cleanMobile) != null) {
            return Result.failure(IllegalArgumentException("Mobile number already registered. Login via OTP or Password."))
        }

        return database.withTransaction {
            val userId = userDao.insertUser(
                UserEntity(
                    fullName = cleanName,
                    mobile = cleanMobile,
                    email = cleanEmail,
                    passwordHash = SecurityUtils.hashPassword(rawPassword, cleanMobile),
                    primaryUpiId = cleanUpi,
                    walletBalance = 0.0,
                    totalCommissionEarned = 0.0,
                    isAdmin = false
                )
            )
            if (cleanUpi.isNotEmpty()) {
                accountDao.insertAccount(
                    PaymentAccountEntity(
                        userId = userId,
                        accountType = "UPI",
                        upiId = cleanUpi,
                        accountHolderName = cleanName,
                        bankName = "Primary UPI ID",
                        isPrimary = true
                    )
                )
            }
            val user = userDao.getUserById(userId)
                ?: throw IllegalStateException("Failed to load created account")
            Result.success(user)
        }
    }

    suspend fun addPaymentAccount(
        userId: Long,
        accountType: String,
        upiId: String,
        holderName: String,
        bankName: String,
        accountNumber: String,
        ifscCode: String,
        makePrimary: Boolean
    ): Result<Unit> {
        val cleanUpi = upiId.trim()
        val cleanHolder = holderName.trim()
        if (!SecurityUtils.isValidUpiId(cleanUpi)) {
            return Result.failure(IllegalArgumentException("Enter a valid UPI ID (e.g. yourname@upi or yourname@ybl)."))
        }
        if (cleanHolder.length < 2) {
            return Result.failure(IllegalArgumentException("Enter the account holder name."))
        }
        val cleanAccNum = accountNumber.trim()
        val cleanIfsc = ifscCode.trim().uppercase()
        if (accountType == "BANK") {
            if (cleanAccNum.length < 6) {
                return Result.failure(IllegalArgumentException("Enter a valid bank account number."))
            }
            if (!cleanIfsc.matches(Regex("^[A-Z]{4}0[A-Z0-9]{6}$"))) {
                return Result.failure(IllegalArgumentException("Enter a valid 11-character IFSC code (e.g. HDFC0001234)."))
            }
        }

        val caller = userDao.getUserById(userId)
            ?: return Result.failure(IllegalArgumentException("Account not found."))
        if (caller.isBlocked) {
            return Result.failure(IllegalArgumentException("Your account has been blocked by Admin."))
        }

        database.withTransaction {
            val existingAccounts = accountDao.getAccountsForUser(userId)
            val shouldBePrimary = makePrimary || existingAccounts.isEmpty()
            if (shouldBePrimary) {
                accountDao.clearPrimaryForUser(userId)
            }
            val masked = if (cleanAccNum.length >= 4) {
                "•••• ${cleanAccNum.takeLast(4)}"
            } else {
                ""
            }
            accountDao.insertAccount(
                PaymentAccountEntity(
                    userId = userId,
                    accountType = accountType,
                    upiId = cleanUpi,
                    accountHolderName = cleanHolder,
                    bankName = if (accountType == "BANK") bankName.trim().ifBlank { "Linked Bank" } else "UPI Handle",
                    accountNumberMasked = masked,
                    ifscCode = cleanIfsc,
                    isPrimary = shouldBePrimary
                )
            )
            if (shouldBePrimary) {
                userDao.updatePrimaryUpi(userId, cleanUpi)
                planDao.updateSellerUpiOnAvailablePlans(userId, cleanUpi)
            }
        }
        return Result.success(Unit)
    }

    suspend fun setPrimaryPaymentAccount(userId: Long, accountId: Long): Result<Unit> {
        val caller = userDao.getUserById(userId)
            ?: return Result.failure(IllegalArgumentException("Account not found."))
        if (caller.isBlocked) {
            return Result.failure(IllegalArgumentException("Your account has been blocked by Admin."))
        }
        val account = accountDao.getAccountById(accountId)
            ?: return Result.failure(IllegalArgumentException("Account not found."))
        if (account.userId != userId) {
            return Result.failure(IllegalArgumentException("Unauthorized account access."))
        }
        database.withTransaction {
            accountDao.clearPrimaryForUser(userId)
            accountDao.markPrimary(userId, accountId)
            userDao.updatePrimaryUpi(userId, account.upiId)
            planDao.updateSellerUpiOnAvailablePlans(userId, account.upiId)
        }
        return Result.success(Unit)
    }

    suspend fun deletePaymentAccount(userId: Long, accountId: Long): Result<Unit> {
        val caller = userDao.getUserById(userId)
            ?: return Result.failure(IllegalArgumentException("Account not found."))
        if (caller.isBlocked) {
            return Result.failure(IllegalArgumentException("Your account has been blocked by Admin."))
        }
        val account = accountDao.getAccountById(accountId)
            ?: return Result.failure(IllegalArgumentException("Account not found."))
        if (account.userId != userId) {
            return Result.failure(IllegalArgumentException("Unauthorized account access."))
        }
        database.withTransaction {
            accountDao.deleteAccount(userId, accountId)
            val remaining = accountDao.getAccountsForUser(userId)
            if (account.isPrimary) {
                val nextPrimary = remaining.firstOrNull()
                if (nextPrimary != null) {
                    accountDao.markPrimary(userId, nextPrimary.id)
                    userDao.updatePrimaryUpi(userId, nextPrimary.upiId)
                    planDao.updateSellerUpiOnAvailablePlans(userId, nextPrimary.upiId)
                } else {
                    userDao.updatePrimaryUpi(userId, "")
                }
            }
        }
        return Result.success(Unit)
    }

    suspend fun depositToWallet(
        userId: Long,
        amount: Double,
        sourceLabel: String,
        utr: String
    ): Result<Unit> {
        if (amount < 100.0 || amount > 500_000.0) {
            return Result.failure(IllegalArgumentException("Enter an amount between ₹100 and ₹5,00,000."))
        }
        val cleanUtr = utr.trim().ifBlank { Formatters.generateUtr() }
        return database.withTransaction {
            val user = userDao.getUserById(userId)
                ?: return@withTransaction Result.failure(IllegalStateException("User not found"))
            if (user.isBlocked) {
                return@withTransaction Result.failure(IllegalArgumentException("Your account has been blocked by Admin."))
            }
            userDao.adjustWalletBalance(userId, amount)
            transactionDao.insertTransaction(
                TransactionEntity(
                    referenceId = cleanUtr,
                    planCode = "WALLET-ADD",
                    type = "WALLET_DEPOSIT",
                    status = "SUCCESS",
                    baseAmount = amount,
                    commissionAmount = 0.0,
                    netAmount = amount,
                    buyerId = user.id,
                    buyerName = user.fullName,
                    buyerUpi = user.primaryUpiId.ifBlank { "${user.mobile}@upi" },
                    sellerName = sourceLabel.ifBlank { "UPI Instant Add" },
                    sellerUpi = sourceLabel.ifBlank { "UPI" },
                    note = "Added ${Formatters.formatCompactInr(amount)} to Jio Pay wallet"
                )
            )
            Result.success(Unit)
        }
    }

    suspend fun withdrawFromWallet(
        userId: Long,
        amount: Double,
        destinationAccount: PaymentAccountEntity?
    ): Result<Unit> {
        if (destinationAccount == null) {
            return Result.failure(IllegalArgumentException("Please add and select a UPI ID or Bank Account first."))
        }
        if (amount < 100.0) {
            return Result.failure(IllegalArgumentException("Minimum withdrawal amount is ₹100."))
        }
        return database.withTransaction {
            val user = userDao.getUserById(userId)
                ?: return@withTransaction Result.failure(IllegalStateException("User not found."))
            if (user.isBlocked) {
                return@withTransaction Result.failure(IllegalArgumentException("Your account has been blocked by Admin."))
            }
            if (user.walletBalance < amount) {
                // Record failed withdrawal attempt so Admin & User logs track failed transactions accurately
                transactionDao.insertTransaction(
                    TransactionEntity(
                        referenceId = Formatters.generateUtr(),
                        planCode = "WITHDRAW",
                        type = "WITHDRAWAL",
                        status = "FAILED",
                        baseAmount = amount,
                        commissionAmount = 0.0,
                        netAmount = amount,
                        buyerId = user.id,
                        buyerName = user.fullName,
                        buyerUpi = user.primaryUpiId.ifBlank { "${user.mobile}@upi" },
                        sellerName = destinationAccount.bankName,
                        sellerUpi = destinationAccount.upiId,
                        note = "Withdrawal failed: Insufficient wallet balance (${Formatters.formatCompactInr(user.walletBalance)})"
                    )
                )
                return@withTransaction Result.failure(
                    IllegalArgumentException("Insufficient wallet balance. Available: ${Formatters.formatInr(user.walletBalance)}")
                )
            }
            userDao.adjustWalletBalance(userId, -amount)
            val destLabel = if (destinationAccount.accountType == "BANK") {
                "${destinationAccount.bankName} ${destinationAccount.accountNumberMasked} (${destinationAccount.upiId})"
            } else {
                destinationAccount.upiId
            }
            transactionDao.insertTransaction(
                TransactionEntity(
                    referenceId = Formatters.generateUtr(),
                    planCode = "WITHDRAW",
                    type = "WITHDRAWAL",
                    status = "SUCCESS",
                    baseAmount = amount,
                    commissionAmount = 0.0,
                    netAmount = amount,
                    buyerId = user.id,
                    buyerName = user.fullName,
                    buyerUpi = user.primaryUpiId.ifBlank { "${user.mobile}@upi" },
                    sellerName = destinationAccount.accountHolderName,
                    sellerUpi = destLabel,
                    note = "Withdrawn ${Formatters.formatCompactInr(amount)} to $destLabel"
                )
            )
            Result.success(Unit)
        }
    }

    suspend fun submitBuyOrderPayment(
        planId: Long,
        buyerId: Long,
        utrNumber: String
    ): Result<Unit> {
        val cleanUtr = utrNumber.trim()
        if (cleanUtr.length < 6) {
            return Result.failure(IllegalArgumentException("Enter a valid UTR / UPI Reference ID (min 6 characters)."))
        }
        return database.withTransaction {
            val plan = planDao.getPlanById(planId)
                ?: return@withTransaction Result.failure(IllegalArgumentException("Plan not found."))
            val buyer = userDao.getUserById(buyerId)
                ?: return@withTransaction Result.failure(IllegalArgumentException("Buyer account not found."))
            val seller = userDao.getUserById(plan.sellerId)

            if (buyer.isBlocked) {
                return@withTransaction Result.failure(IllegalArgumentException("Your account has been blocked by Admin."))
            }
            if (seller?.isBlocked == true) {
                return@withTransaction Result.failure(IllegalArgumentException("This seller's account is currently blocked."))
            }
            if (plan.sellerId == buyer.id) {
                return@withTransaction Result.failure(IllegalArgumentException("You cannot buy your own sell listing."))
            }
            if (plan.status != "AVAILABLE") {
                return@withTransaction Result.failure(IllegalArgumentException("This plan is no longer available."))
            }

            val buyerUpi = buyer.primaryUpiId.ifBlank { "${buyer.mobile}@upi" }
            val now = System.currentTimeMillis()
            planDao.updatePlan(
                plan.copy(
                    status = "PENDING_VERIFICATION",
                    pendingBuyerId = buyer.id,
                    pendingBuyerName = buyer.fullName,
                    pendingBuyerUpi = buyerUpi,
                    pendingUtr = cleanUtr,
                    updatedAt = now
                )
            )

            transactionDao.insertTransaction(
                TransactionEntity(
                    referenceId = cleanUtr,
                    planId = plan.id,
                    planCode = plan.planCode,
                    type = "BUY_ORDER",
                    status = "PENDING",
                    baseAmount = plan.amount,
                    commissionAmount = plan.commissionAmount,
                    netAmount = plan.totalReceiveAmount,
                    buyerId = buyer.id,
                    buyerName = buyer.fullName,
                    buyerUpi = buyerUpi,
                    sellerId = plan.sellerId,
                    sellerName = plan.sellerName,
                    sellerUpi = plan.sellerUpiId,
                    note = "Paid ${Formatters.formatCompactInr(plan.amount)} to ${plan.sellerUpiId} • Awaiting seller verification",
                    createdAt = now,
                    updatedAt = now
                )
            )
            Result.success(Unit)
        }
    }

    suspend fun verifyAndCompleteOrder(
        planId: Long,
        verifierUserId: Long
    ): Result<Unit> {
        return database.withTransaction {
            val plan = planDao.getPlanById(planId)
                ?: return@withTransaction Result.failure(IllegalArgumentException("Order not found."))
            val verifier = userDao.getUserById(verifierUserId)
                ?: return@withTransaction Result.failure(IllegalArgumentException("Verifier account not found."))

            if (verifier.isBlocked) {
                return@withTransaction Result.failure(IllegalArgumentException("Your account has been blocked by Admin."))
            }
            if (plan.status != "PENDING_VERIFICATION" || plan.pendingBuyerId == null) {
                return@withTransaction Result.failure(IllegalArgumentException("Order is not pending verification."))
            }
            if (verifier.id != plan.sellerId && !verifier.isAdmin) {
                return@withTransaction Result.failure(IllegalArgumentException("Only the seller (${plan.sellerUpiId}) or Admin can verify this payment."))
            }

            val seller = userDao.getUserById(plan.sellerId)
                ?: return@withTransaction Result.failure(IllegalArgumentException("Seller account not found."))
            val buyer = userDao.getUserById(plan.pendingBuyerId)
                ?: return@withTransaction Result.failure(IllegalArgumentException("Buyer account not found."))

            val pendingTxn = transactionDao.getPendingBuyTransactionForPlan(plan.id)
            val now = System.currentTimeMillis()

            if (seller.walletBalance < plan.amount) {
                // Fail transaction due to insufficient seller wallet balance
                if (pendingTxn != null) {
                    transactionDao.updateTransactionStatus(
                        transactionId = pendingTxn.id,
                        newStatus = "FAILED",
                        note = "Transfer failed: Seller (${seller.primaryUpiId}) has insufficient wallet balance",
                        updatedAt = now
                    )
                }
                planDao.updatePlan(
                    plan.copy(
                        status = "AVAILABLE",
                        pendingBuyerId = null,
                        pendingBuyerName = null,
                        pendingBuyerUpi = null,
                        pendingUtr = null,
                        updatedAt = now
                    )
                )
                return@withTransaction Result.failure(
                    IllegalArgumentException("Seller wallet balance (${Formatters.formatInr(seller.walletBalance)}) is less than ${Formatters.formatInr(plan.amount)}.")
                )
            }

            // Atomic transfer:
            // 1. Deduct base plan amount from seller's wallet
            userDao.adjustWalletBalance(seller.id, -plan.amount)
            // 2. Credit base amount + 8% commission profit to buyer's wallet
            userDao.creditBuyerWalletWithCommission(
                userId = buyer.id,
                creditAmount = plan.totalReceiveAmount,
                commissionEarned = plan.commissionAmount
            )

            // 3. Mark plan as SOLD
            planDao.updatePlan(
                plan.copy(
                    status = "SOLD",
                    updatedAt = now
                )
            )

            val utr = plan.pendingUtr ?: Formatters.generateUtr()
            // 4. Mark buyer's BUY_ORDER transaction as SUCCESS
            if (pendingTxn != null) {
                transactionDao.updateTransactionStatus(
                    transactionId = pendingTxn.id,
                    newStatus = "SUCCESS",
                    note = "Verified by ${seller.primaryUpiId} • ${Formatters.formatCompactInr(plan.amount)} + ${Formatters.formatCompactInr(plan.commissionAmount)} (8% Profit) added to wallet",
                    updatedAt = now
                )
            } else {
                transactionDao.insertTransaction(
                    TransactionEntity(
                        referenceId = utr,
                        planId = plan.id,
                        planCode = plan.planCode,
                        type = "BUY_ORDER",
                        status = "SUCCESS",
                        baseAmount = plan.amount,
                        commissionAmount = plan.commissionAmount,
                        netAmount = plan.totalReceiveAmount,
                        buyerId = buyer.id,
                        buyerName = buyer.fullName,
                        buyerUpi = plan.pendingBuyerUpi ?: buyer.primaryUpiId,
                        sellerId = seller.id,
                        sellerName = seller.fullName,
                        sellerUpi = seller.primaryUpiId,
                        note = "Verified by ${seller.primaryUpiId} • ${Formatters.formatCompactInr(plan.amount)} + ${Formatters.formatCompactInr(plan.commissionAmount)} (8% Profit) added to wallet",
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }

            // 5. Record seller debit transfer log
            transactionDao.insertTransaction(
                TransactionEntity(
                    referenceId = "$utr-S",
                    planId = plan.id,
                    planCode = plan.planCode,
                    type = "SELL_TRANSFER",
                    status = "SUCCESS",
                    baseAmount = plan.amount,
                    commissionAmount = 0.0,
                    netAmount = plan.amount,
                    buyerId = buyer.id,
                    buyerName = buyer.fullName,
                    buyerUpi = plan.pendingBuyerUpi ?: buyer.primaryUpiId,
                    sellerId = seller.id,
                    sellerName = seller.fullName,
                    sellerUpi = seller.primaryUpiId,
                    note = "Confirmed & transferred ${Formatters.formatCompactInr(plan.amount)} from ${seller.primaryUpiId} to ${buyer.fullName}",
                    createdAt = now,
                    updatedAt = now
                )
            )

            Result.success(Unit)
        }
    }

    suspend fun rejectOrFailOrder(
        planId: Long,
        verifierUserId: Long,
        reason: String
    ): Result<Unit> {
        return database.withTransaction {
            val plan = planDao.getPlanById(planId)
                ?: return@withTransaction Result.failure(IllegalArgumentException("Order not found."))
            val verifier = userDao.getUserById(verifierUserId)
                ?: return@withTransaction Result.failure(IllegalArgumentException("Verifier account not found."))

            if (verifier.isBlocked) {
                return@withTransaction Result.failure(IllegalArgumentException("Your account has been blocked by Admin."))
            }
            if (plan.status != "PENDING_VERIFICATION") {
                return@withTransaction Result.failure(IllegalArgumentException("Order is not pending verification."))
            }
            if (verifier.id != plan.sellerId && !verifier.isAdmin) {
                return@withTransaction Result.failure(IllegalArgumentException("Unauthorized verification action."))
            }

            val pendingTxn = transactionDao.getPendingBuyTransactionForPlan(plan.id)
            val now = System.currentTimeMillis()
            val cleanReason = reason.trim().ifBlank { "Payment not received / UTR verification failed" }

            if (pendingTxn != null) {
                transactionDao.updateTransactionStatus(
                    transactionId = pendingTxn.id,
                    newStatus = "FAILED",
                    note = cleanReason,
                    updatedAt = now
                )
            }

            planDao.updatePlan(
                plan.copy(
                    status = "AVAILABLE",
                    pendingBuyerId = null,
                    pendingBuyerName = null,
                    pendingBuyerUpi = null,
                    pendingUtr = null,
                    updatedAt = now
                )
            )
            Result.success(Unit)
        }
    }

    suspend fun recordFailedBuyAttempt(
        planId: Long,
        buyerId: Long,
        reason: String
    ) {
        database.withTransaction {
            val plan = planDao.getPlanById(planId) ?: return@withTransaction
            val buyer = userDao.getUserById(buyerId) ?: return@withTransaction
            val now = System.currentTimeMillis()
            transactionDao.insertTransaction(
                TransactionEntity(
                    referenceId = Formatters.generateUtr(),
                    planId = plan.id,
                    planCode = plan.planCode,
                    type = "BUY_ORDER",
                    status = "FAILED",
                    baseAmount = plan.amount,
                    commissionAmount = plan.commissionAmount,
                    netAmount = plan.totalReceiveAmount,
                    buyerId = buyer.id,
                    buyerName = buyer.fullName,
                    buyerUpi = buyer.primaryUpiId.ifBlank { "${buyer.mobile}@upi" },
                    sellerId = plan.sellerId,
                    sellerName = plan.sellerName,
                    sellerUpi = plan.sellerUpiId,
                    note = reason.ifBlank { "UPI payment cancelled or timed out before submission" },
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }

    suspend fun createSellPlans(
        sellerId: Long,
        unitAmount: Double,
        quantity: Int,
        inlineUpiId: String = ""
    ): Result<Unit> {
        if (unitAmount < 100.0 || unitAmount > 100_000.0) {
            return Result.failure(IllegalArgumentException("Plan amount must be between ₹100 and ₹1,00,000."))
        }
        if (quantity !in 1..25) {
            return Result.failure(IllegalArgumentException("Quantity must be between 1 and 25 plans."))
        }

        return database.withTransaction {
            val seller = userDao.getUserById(sellerId)
                ?: return@withTransaction Result.failure(IllegalArgumentException("Seller account not found."))
            if (seller.isBlocked) {
                return@withTransaction Result.failure(IllegalArgumentException("Your account has been blocked by Admin."))
            }

            val cleanInlineUpi = inlineUpiId.trim()
            val resolvedUpi = when {
                seller.primaryUpiId.isNotBlank() -> seller.primaryUpiId
                cleanInlineUpi.isNotEmpty() -> {
                    if (!SecurityUtils.isValidUpiId(cleanInlineUpi)) {
                        return@withTransaction Result.failure(
                            IllegalArgumentException("Enter a valid UPI ID (e.g. yourname@upi) so buyers can pay you.")
                        )
                    }
                    accountDao.clearPrimaryForUser(seller.id)
                    accountDao.insertAccount(
                        PaymentAccountEntity(
                            userId = seller.id,
                            accountType = "UPI",
                            upiId = cleanInlineUpi,
                            accountHolderName = seller.fullName,
                            bankName = "Primary UPI ID",
                            isPrimary = true
                        )
                    )
                    userDao.updatePrimaryUpi(seller.id, cleanInlineUpi)
                    cleanInlineUpi
                }
                else -> {
                    return@withTransaction Result.failure(
                        IllegalArgumentException("Add your UPI ID first so buyers can scan & pay you directly.")
                    )
                }
            }

            val totalRequired = unitAmount * quantity
            if (seller.walletBalance < totalRequired) {
                return@withTransaction Result.failure(
                    IllegalArgumentException(
                        "Insufficient wallet balance (${Formatters.formatInr(seller.walletBalance)}) to list $quantity × ${Formatters.formatCompactInr(unitAmount)} (${Formatters.formatCompactInr(totalRequired)})."
                    )
                )
            }

            val commission = unitAmount * 0.08
            val receive = unitAmount + commission
            val now = System.currentTimeMillis()
            val prefix = seller.fullName.take(2).uppercase().ifBlank { "JP" }
            val newPlans = (1..quantity).map { idx ->
                val suffix = ((now % 1000) + idx).toString().padStart(3, '0')
                MoneyPlanEntity(
                    planCode = "$prefix-${unitAmount.toInt()}-$suffix",
                    amount = unitAmount,
                    commissionPercent = 8.0,
                    commissionAmount = commission,
                    totalReceiveAmount = receive,
                    sellerId = seller.id,
                    sellerName = seller.fullName,
                    sellerUpiId = resolvedUpi,
                    status = "AVAILABLE",
                    createdAt = now,
                    updatedAt = now
                )
            }
            planDao.insertPlans(newPlans)
            Result.success(Unit)
        }
    }

    suspend fun cancelSellPlan(planId: Long, sellerId: Long): Result<Unit> {
        val seller = userDao.getUserById(sellerId)
            ?: return Result.failure(IllegalArgumentException("Seller account not found."))
        if (seller.isBlocked) {
            return Result.failure(IllegalArgumentException("Your account has been blocked by Admin."))
        }
        val deleted = planDao.deleteAvailablePlanBySeller(planId, sellerId)
        return if (deleted > 0) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("Only available sell listings can be cancelled."))
        }
    }

    // --- Exclusive Admin User & Account Control Methods ---

    suspend fun adminSetUserBlocked(
        adminId: Long,
        targetUserId: Long,
        blocked: Boolean
    ): Result<Unit> {
        val admin = userDao.getUserById(adminId)
        if (admin == null || !admin.isAdmin) {
            return Result.failure(IllegalArgumentException("Unauthorized: Only Admin can block or unblock accounts."))
        }
        val target = userDao.getUserById(targetUserId)
            ?: return Result.failure(IllegalArgumentException("User account not found."))
        if (target.isAdmin) {
            return Result.failure(IllegalArgumentException("Admin account cannot be blocked."))
        }
        userDao.setUserBlocked(targetUserId, blocked)
        return Result.success(Unit)
    }

    suspend fun adminAdjustUserWallet(
        adminId: Long,
        targetUserId: Long,
        mode: String, // "CREDIT", "DEBIT", "SET"
        amount: Double,
        reason: String
    ): Result<Unit> {
        val admin = userDao.getUserById(adminId)
        if (admin == null || !admin.isAdmin) {
            return Result.failure(IllegalArgumentException("Unauthorized: Only Admin can modify user wallet balances."))
        }
        if (amount.isNaN() || amount < 0.0) {
            return Result.failure(IllegalArgumentException("Enter a valid non-negative amount."))
        }
        if ((mode == "CREDIT" || mode == "DEBIT") && amount <= 0.0) {
            return Result.failure(IllegalArgumentException("Enter an amount greater than ₹0."))
        }

        return database.withTransaction {
            val target = userDao.getUserById(targetUserId)
                ?: return@withTransaction Result.failure(IllegalArgumentException("User account not found."))

            val newBalance = when (mode) {
                "CREDIT" -> target.walletBalance + amount
                "DEBIT" -> {
                    if (target.walletBalance < amount) {
                        return@withTransaction Result.failure(
                            IllegalArgumentException(
                                "User wallet (${Formatters.formatInr(target.walletBalance)}) has less than ${Formatters.formatInr(amount)}."
                            )
                        )
                    }
                    target.walletBalance - amount
                }
                "SET" -> amount
                else -> return@withTransaction Result.failure(IllegalArgumentException("Invalid balance mode."))
            }

            val delta = newBalance - target.walletBalance
            userDao.setWalletBalance(target.id, newBalance)

            if (delta != 0.0) {
                val isCredit = delta > 0.0
                val absDelta = kotlin.math.abs(delta)
                val cleanNote = reason.trim().ifBlank {
                    if (isCredit) {
                        "Admin credited ${Formatters.formatCompactInr(absDelta)} to ${target.fullName}"
                    } else {
                        "Admin deducted ${Formatters.formatCompactInr(absDelta)} from ${target.fullName}"
                    }
                }
                transactionDao.insertTransaction(
                    TransactionEntity(
                        referenceId = Formatters.generateUtr(),
                        planCode = "ADMIN-ADJ",
                        type = if (isCredit) "ADMIN_CREDIT" else "ADMIN_DEBIT",
                        status = "SUCCESS",
                        baseAmount = absDelta,
                        commissionAmount = 0.0,
                        netAmount = absDelta,
                        buyerId = target.id,
                        buyerName = target.fullName,
                        buyerUpi = target.primaryUpiId.ifBlank { "${target.mobile}@upi" },
                        sellerId = admin.id,
                        sellerName = "Admin (${admin.fullName})",
                        sellerUpi = admin.primaryUpiId,
                        note = cleanNote
                    )
                )
            }
            Result.success(Unit)
        }
    }

    suspend fun adminUpdateUserAccount(
        adminId: Long,
        targetUserId: Long,
        fullName: String,
        mobile: String,
        email: String,
        primaryUpiId: String,
        newPassword: String?
    ): Result<Unit> {
        val admin = userDao.getUserById(adminId)
        if (admin == null || !admin.isAdmin) {
            return Result.failure(IllegalArgumentException("Unauthorized: Only Admin can edit user accounts."))
        }
        val cleanName = fullName.trim()
        val cleanMobile = mobile.trim()
        val cleanEmail = email.trim()
        val cleanUpi = primaryUpiId.trim()
        val cleanNewPassword = newPassword?.trim().orEmpty()

        if (cleanName.length < 2) {
            return Result.failure(IllegalArgumentException("Full name must be at least 2 characters."))
        }
        if (!cleanMobile.matches(Regex("^[6-9][0-9]{9}$"))) {
            return Result.failure(IllegalArgumentException("Enter a valid 10-digit Indian mobile number."))
        }
        if (cleanUpi.isNotEmpty() && !SecurityUtils.isValidUpiId(cleanUpi)) {
            return Result.failure(IllegalArgumentException("Enter a valid UPI ID (e.g. name@upi) or leave blank."))
        }
        if (cleanNewPassword.isNotEmpty() && cleanNewPassword.length < 4) {
            return Result.failure(IllegalArgumentException("New password must be at least 4 characters."))
        }

        return database.withTransaction {
            val target = userDao.getUserById(targetUserId)
                ?: return@withTransaction Result.failure(IllegalArgumentException("User account not found."))

            val existingWithMobile = userDao.getUserByMobile(cleanMobile)
            if (existingWithMobile != null && existingWithMobile.id != targetUserId) {
                return@withTransaction Result.failure(IllegalArgumentException("Mobile number already belongs to another account."))
            }
            if (cleanMobile != target.mobile && cleanNewPassword.isEmpty()) {
                return@withTransaction Result.failure(
                    IllegalArgumentException("Enter a new password when changing a user's mobile number.")
                )
            }

            val updatedHash = if (cleanNewPassword.isNotEmpty()) {
                SecurityUtils.hashPassword(cleanNewPassword, cleanMobile)
            } else {
                target.passwordHash
            }

            userDao.updateUser(
                target.copy(
                    fullName = cleanName,
                    mobile = cleanMobile,
                    email = cleanEmail,
                    primaryUpiId = cleanUpi,
                    passwordHash = updatedHash
                )
            )

            if (cleanUpi.isNotEmpty() && cleanUpi != target.primaryUpiId) {
                accountDao.clearPrimaryForUser(target.id)
                val existingAccounts = accountDao.getAccountsForUser(target.id)
                val matchingAccount = existingAccounts.firstOrNull { it.upiId.equals(cleanUpi, ignoreCase = true) }
                if (matchingAccount != null) {
                    accountDao.markPrimary(target.id, matchingAccount.id)
                } else {
                    accountDao.insertAccount(
                        PaymentAccountEntity(
                            userId = target.id,
                            accountType = "UPI",
                            upiId = cleanUpi,
                            accountHolderName = cleanName,
                            bankName = "Admin Linked UPI",
                            isPrimary = true
                        )
                    )
                }
            }

            planDao.updateSellerDetailsOnAvailablePlans(target.id, cleanName, cleanUpi)
            Result.success(Unit)
        }
    }

    suspend fun adminDeleteUserAccount(
        adminId: Long,
        targetUserId: Long
    ): Result<Unit> {
        val admin = userDao.getUserById(adminId)
        if (admin == null || !admin.isAdmin) {
            return Result.failure(IllegalArgumentException("Unauthorized: Only Admin can delete user accounts."))
        }
        val target = userDao.getUserById(targetUserId)
            ?: return Result.failure(IllegalArgumentException("User account not found."))
        if (target.isAdmin) {
            return Result.failure(IllegalArgumentException("Admin account cannot be deleted."))
        }
        database.withTransaction {
            planDao.deleteUnsoldPlansBySeller(targetUserId)
            userDao.deleteNonAdminUser(targetUserId)
        }
        return Result.success(Unit)
    }
}
