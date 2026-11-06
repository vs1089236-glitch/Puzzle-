package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.MoneyPlanEntity
import com.example.data.local.entity.PaymentAccountEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.UserEntity
import com.example.data.preferences.SessionPreferences
import com.example.data.repository.JioPayRepository
import com.example.util.Formatters
import com.example.util.SecurityUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class JioPayViewModel(
    private val repository: JioPayRepository,
    private val sessionPreferences: SessionPreferences
) : ViewModel() {

    private val authModeFlow = MutableStateFlow(AuthMode.OTP)
    private val otpSessionFlow = MutableStateFlow(OtpSessionState())
    private val planFilterFlow = MutableStateFlow(PlanFilter.ALL)
    private val userTxnFilterFlow = MutableStateFlow(TxnFilter.ALL)
    private val adminTxnFilterFlow = MutableStateFlow(TxnFilter.ALL)
    private val activeCheckoutPlanFlow = MutableStateFlow<MoneyPlanEntity?>(null)
    private val isBusyFlow = MutableStateFlow(false)
    private val statusBannerFlow = MutableStateFlow<String?>(null)
    private val errorBannerFlow = MutableStateFlow<String?>(null)

    private var otpCountdownJob: Job? = null

    private data class UserScopedStreams(
        val user: UserEntity?,
        val accounts: List<PaymentAccountEntity>,
        val sellerPlans: List<MoneyPlanEntity>,
        val sellerPending: List<MoneyPlanEntity>,
        val userTransactions: List<TransactionEntity>
    )

    private data class PlatformStreams(
        val allUsers: List<UserEntity>,
        val marketplacePlans: List<MoneyPlanEntity>,
        val allPending: List<MoneyPlanEntity>,
        val allTransactions: List<TransactionEntity>
    )

    private data class LocalControlState(
        val authMode: AuthMode,
        val otpSession: OtpSessionState,
        val planFilter: PlanFilter,
        val userTxnFilter: TxnFilter,
        val adminTxnFilter: TxnFilter,
        val activeCheckoutPlan: MoneyPlanEntity?,
        val isBusy: Boolean,
        val statusBanner: String?,
        val errorBanner: String?
    )

    private val userScopedFlow: Flow<UserScopedStreams> =
        sessionPreferences.currentUserIdFlow.flatMapLatest { userId ->
            if (userId == null) {
                flowOf(
                    UserScopedStreams(
                        user = null,
                        accounts = emptyList(),
                        sellerPlans = emptyList(),
                        sellerPending = emptyList(),
                        userTransactions = emptyList()
                    )
                )
            } else {
                combine(
                    repository.observeUser(userId),
                    repository.observePaymentAccounts(userId),
                    repository.observeSellerPlans(userId),
                    repository.observePendingVerificationsForSeller(userId),
                    repository.observeUserTransactions(userId)
                ) { user, accounts, sellerPlans, sellerPending, userTxns ->
                    if (user != null && user.isBlocked) {
                        viewModelScope.launch {
                            sessionPreferences.clearSession()
                            errorBannerFlow.value = "Your account has been blocked by Admin."
                        }
                    }
                    UserScopedStreams(
                        user = user,
                        accounts = accounts,
                        sellerPlans = sellerPlans,
                        sellerPending = sellerPending,
                        userTransactions = userTxns
                    )
                }
            }
        }

    private val platformStreamsFlow: Flow<PlatformStreams> = combine(
        repository.observeAllUsers(),
        repository.observeMarketplacePlans(),
        repository.observeAllPendingVerifications(),
        repository.observeAllTransactions()
    ) { users, market, pending, txns ->
        PlatformStreams(
            allUsers = users,
            marketplacePlans = market,
            allPending = pending,
            allTransactions = txns
        )
    }

    private val localControlsFlow: Flow<LocalControlState> = combine(
        combine(authModeFlow, otpSessionFlow, planFilterFlow) { a, o, p -> Triple(a, o, p) },
        combine(userTxnFilterFlow, adminTxnFilterFlow, activeCheckoutPlanFlow) { u, ad, c -> Triple(u, ad, c) },
        combine(isBusyFlow, statusBannerFlow, errorBannerFlow) { b, s, e -> Triple(b, s, e) }
    ) { (authMode, otp, planFilter), (userTxnFilter, adminTxnFilter, checkout), (busy, status, err) ->
        LocalControlState(
            authMode = authMode,
            otpSession = otp,
            planFilter = planFilter,
            userTxnFilter = userTxnFilter,
            adminTxnFilter = adminTxnFilter,
            activeCheckoutPlan = checkout,
            isBusy = busy,
            statusBanner = status,
            errorBanner = err
        )
    }

    val uiState: StateFlow<JioPayUiState> = combine(
        userScopedFlow,
        platformStreamsFlow,
        localControlsFlow
    ) { scoped, platform, controls ->
        val activeUser = scoped.user?.takeIf { !it.isBlocked }
        JioPayUiState(
            isSessionLoading = false,
            currentUser = activeUser,
            allUsers = if (activeUser?.isAdmin == true) platform.allUsers else emptyList(),
            paymentAccounts = scoped.accounts,
            marketplacePlans = platform.marketplacePlans,
            sellerPlans = scoped.sellerPlans,
            sellerPendingVerifications = scoped.sellerPending,
            allPendingVerifications = if (activeUser?.isAdmin == true) platform.allPending else emptyList(),
            userTransactions = scoped.userTransactions,
            allTransactions = if (activeUser?.isAdmin == true) platform.allTransactions else emptyList(),
            selectedPlanFilter = controls.planFilter,
            selectedUserTxnFilter = controls.userTxnFilter,
            selectedAdminTxnFilter = controls.adminTxnFilter,
            authMode = controls.authMode,
            otpSession = controls.otpSession,
            activeCheckoutPlan = controls.activeCheckoutPlan,
            isBusy = controls.isBusy,
            statusBanner = controls.statusBanner,
            errorBanner = controls.errorBanner
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = JioPayUiState(isSessionLoading = true)
    )

    fun clearBanners() {
        statusBannerFlow.value = null
        errorBannerFlow.value = null
    }

    fun setAuthMode(mode: AuthMode) {
        clearBanners()
        authModeFlow.value = mode
    }

    fun updateOtpMobile(mobile: String) {
        val digitsOnly = mobile.filter { it.isDigit() }.take(10)
        otpSessionFlow.update {
            it.copy(
                mobile = digitsOnly,
                isOtpSent = if (digitsOnly != it.mobile) false else it.isOtpSent,
                enteredOtp = if (digitsOnly != it.mobile) "" else it.enteredOtp
            )
        }
    }

    fun updateEnteredOtp(otp: String) {
        val digitsOnly = otp.filter { it.isDigit() }.take(6)
        otpSessionFlow.update { it.copy(enteredOtp = digitsOnly) }
    }

    fun updateOtpNewUserName(name: String) {
        otpSessionFlow.update { it.copy(newUserName = name) }
    }

    fun updateOtpNewUserUpi(upi: String) {
        otpSessionFlow.update { it.copy(newUserUpi = upi) }
    }

    fun autoFillGeneratedOtp() {
        val code = otpSessionFlow.value.generatedOtp
        if (code.isNotEmpty()) {
            otpSessionFlow.update { it.copy(enteredOtp = code) }
        }
    }

    fun sendOtp() {
        clearBanners()
        val mobile = otpSessionFlow.value.mobile.trim()
        if (!mobile.matches(Regex("^[6-9][0-9]{9}$"))) {
            errorBannerFlow.value = "Enter a valid 10-digit Indian mobile number starting with 6-9."
            return
        }
        viewModelScope.launch {
            val existing = repository.findUserByMobile(mobile)
            val code = SecurityUtils.generateSixDigitOtp()
            otpSessionFlow.update {
                it.copy(
                    generatedOtp = code,
                    isOtpSent = true,
                    isNewUser = existing == null,
                    secondsRemaining = 120,
                    enteredOtp = ""
                )
            }
            startOtpCountdown()
        }
    }

    private fun startOtpCountdown() {
        otpCountdownJob?.cancel()
        otpCountdownJob = viewModelScope.launch {
            while (otpSessionFlow.value.secondsRemaining > 0) {
                delay(1_000L)
                otpSessionFlow.update { state ->
                    state.copy(secondsRemaining = (state.secondsRemaining - 1).coerceAtLeast(0))
                }
            }
        }
    }

    fun verifyOtpAndLogin() {
        clearBanners()
        val otpState = otpSessionFlow.value
        if (!otpState.isOtpSent) {
            errorBannerFlow.value = "Tap 'Send 6-Digit OTP' first."
            return
        }
        if (otpState.secondsRemaining <= 0) {
            errorBannerFlow.value = "OTP expired. Please resend a fresh OTP."
            return
        }
        if (otpState.enteredOtp != otpState.generatedOtp) {
            errorBannerFlow.value = "Invalid 6-digit OTP. Check the verification code and try again."
            return
        }
        viewModelScope.launch {
            isBusyFlow.value = true
            val result = repository.loginOrCreateWithOtp(
                mobile = otpState.mobile,
                newUserName = otpState.newUserName,
                newUserUpi = otpState.newUserUpi
            )
            isBusyFlow.value = false
            result.onSuccess { user ->
                otpCountdownJob?.cancel()
                otpSessionFlow.value = OtpSessionState()
                sessionPreferences.setCurrentUserId(user.id)
                statusBannerFlow.value = "Welcome to Jio Pay, ${user.fullName}!"
            }.onFailure { err ->
                errorBannerFlow.value = err.message ?: "OTP login failed."
            }
        }
    }

    fun loginWithPassword(identifier: String, password: String) {
        clearBanners()
        viewModelScope.launch {
            isBusyFlow.value = true
            val result = repository.loginWithPassword(identifier, password)
            isBusyFlow.value = false
            result.onSuccess { user ->
                sessionPreferences.setCurrentUserId(user.id)
                statusBannerFlow.value = "Signed in as ${user.fullName}"
            }.onFailure { err ->
                errorBannerFlow.value = err.message ?: "Login failed."
            }
        }
    }

    fun registerUser(
        fullName: String,
        mobile: String,
        email: String,
        password: String,
        upiId: String
    ) {
        clearBanners()
        viewModelScope.launch {
            isBusyFlow.value = true
            val result = repository.registerUser(fullName, mobile, email, password, upiId)
            isBusyFlow.value = false
            result.onSuccess { user ->
                sessionPreferences.setCurrentUserId(user.id)
                statusBannerFlow.value = "Account created! Welcome, ${user.fullName}."
            }.onFailure { err ->
                errorBannerFlow.value = err.message ?: "Registration failed."
            }
        }
    }

    fun switchUserSession(user: UserEntity) {
        val current = uiState.value.currentUser
        if (current == null || !current.isAdmin) {
            errorBannerFlow.value = "Unauthorized: Only Admin can switch accounts."
            return
        }
        clearBanners()
        viewModelScope.launch {
            otpCountdownJob?.cancel()
            otpSessionFlow.value = OtpSessionState()
            activeCheckoutPlanFlow.value = null
            sessionPreferences.setCurrentUserId(user.id)
            statusBannerFlow.value = "Switched to ${user.fullName} (${user.primaryUpiId.ifBlank { user.mobile }})"
        }
    }

    fun signOut() {
        clearBanners()
        viewModelScope.launch {
            otpCountdownJob?.cancel()
            otpSessionFlow.value = OtpSessionState()
            activeCheckoutPlanFlow.value = null
            sessionPreferences.clearSession()
        }
    }

    fun selectPlanFilter(filter: PlanFilter) {
        planFilterFlow.value = filter
    }

    fun selectUserTxnFilter(filter: TxnFilter) {
        userTxnFilterFlow.value = filter
    }

    fun selectAdminTxnFilter(filter: TxnFilter) {
        adminTxnFilterFlow.value = filter
    }

    fun openBuyCheckout(plan: MoneyPlanEntity) {
        clearBanners()
        val user = uiState.value.currentUser ?: return
        if (plan.sellerId == user.id) {
            errorBannerFlow.value = "This is your own sell listing. Manage or cancel it from the Sell tab."
            return
        }
        if (plan.status != "AVAILABLE") {
            errorBannerFlow.value = "This plan is currently awaiting seller payment verification."
            return
        }
        activeCheckoutPlanFlow.value = plan
    }

    fun dismissBuyCheckout() {
        activeCheckoutPlanFlow.value = null
    }

    fun submitBuyPayment(planId: Long, utrNumber: String) {
        clearBanners()
        val buyerId = uiState.value.currentUser?.id ?: return
        viewModelScope.launch {
            isBusyFlow.value = true
            val result = repository.submitBuyOrderPayment(planId, buyerId, utrNumber)
            isBusyFlow.value = false
            result.onSuccess {
                activeCheckoutPlanFlow.value = null
                statusBannerFlow.value =
                    "Payment submitted! Once the seller verifies it from their account, your wallet receives the amount + 8% profit."
            }.onFailure { err ->
                errorBannerFlow.value = err.message ?: "Could not submit payment."
            }
        }
    }

    fun reportFailedPaymentCheckout(planId: Long, reason: String) {
        clearBanners()
        val buyerId = uiState.value.currentUser?.id ?: return
        viewModelScope.launch {
            repository.recordFailedBuyAttempt(planId, buyerId, reason)
            activeCheckoutPlanFlow.value = null
            errorBannerFlow.value = "Payment marked as failed and logged in Transaction History & Admin Panel."
        }
    }

    fun verifyAndTransferOrder(planId: Long) {
        clearBanners()
        val verifierId = uiState.value.currentUser?.id ?: return
        viewModelScope.launch {
            isBusyFlow.value = true
            val result = repository.verifyAndCompleteOrder(planId, verifierId)
            isBusyFlow.value = false
            result.onSuccess {
                statusBannerFlow.value =
                    "Payment verified! Deducted from seller wallet & credited buyer wallet (+8% commission)."
            }.onFailure { err ->
                errorBannerFlow.value = err.message ?: "Verification failed."
            }
        }
    }

    fun rejectOrderAsFailed(planId: Long, reason: String) {
        clearBanners()
        val verifierId = uiState.value.currentUser?.id ?: return
        viewModelScope.launch {
            isBusyFlow.value = true
            val result = repository.rejectOrFailOrder(planId, verifierId, reason)
            isBusyFlow.value = false
            result.onSuccess {
                statusBannerFlow.value = "Order rejected and logged under Failed Transactions."
            }.onFailure { err ->
                errorBannerFlow.value = err.message ?: "Could not reject order."
            }
        }
    }

    fun addPaymentAccount(
        accountType: String,
        upiId: String,
        holderName: String,
        bankName: String,
        accountNumber: String,
        ifscCode: String,
        makePrimary: Boolean,
        onComplete: () -> Unit
    ) {
        clearBanners()
        val userId = uiState.value.currentUser?.id ?: return
        viewModelScope.launch {
            isBusyFlow.value = true
            val result = repository.addPaymentAccount(
                userId = userId,
                accountType = accountType,
                upiId = upiId,
                holderName = holderName,
                bankName = bankName,
                accountNumber = accountNumber,
                ifscCode = ifscCode,
                makePrimary = makePrimary
            )
            isBusyFlow.value = false
            result.onSuccess {
                statusBannerFlow.value = "Saved $upiId to your Jio Pay account!"
                onComplete()
            }.onFailure { err ->
                errorBannerFlow.value = err.message ?: "Failed to add account."
            }
        }
    }

    fun setPrimaryAccount(accountId: Long) {
        clearBanners()
        val userId = uiState.value.currentUser?.id ?: return
        viewModelScope.launch {
            repository.setPrimaryPaymentAccount(userId, accountId)
                .onSuccess {
                    statusBannerFlow.value = "Primary UPI ID updated for receiving payments."
                }
                .onFailure { err ->
                    errorBannerFlow.value = err.message ?: "Failed to update primary account."
                }
        }
    }

    fun deleteAccount(accountId: Long) {
        clearBanners()
        val userId = uiState.value.currentUser?.id ?: return
        viewModelScope.launch {
            repository.deletePaymentAccount(userId, accountId)
                .onSuccess {
                    statusBannerFlow.value = "Linked account removed."
                }
                .onFailure { err ->
                    errorBannerFlow.value = err.message ?: "Could not remove account."
                }
        }
    }

    fun addMoneyToWallet(
        amountText: String,
        sourceLabel: String,
        utr: String,
        onComplete: () -> Unit
    ) {
        clearBanners()
        val userId = uiState.value.currentUser?.id ?: return
        val amount = amountText.trim().toDoubleOrNull()
        if (amount == null) {
            errorBannerFlow.value = "Enter a valid numeric amount."
            return
        }
        viewModelScope.launch {
            isBusyFlow.value = true
            val result = repository.depositToWallet(userId, amount, sourceLabel, utr)
            isBusyFlow.value = false
            result.onSuccess {
                statusBannerFlow.value = "${Formatters.formatCompactInr(amount)} added to your wallet!"
                onComplete()
            }.onFailure { err ->
                errorBannerFlow.value = err.message ?: "Deposit failed."
            }
        }
    }

    fun withdrawMoney(
        amountText: String,
        destination: PaymentAccountEntity?,
        onComplete: () -> Unit
    ) {
        clearBanners()
        val userId = uiState.value.currentUser?.id ?: return
        val amount = amountText.trim().toDoubleOrNull()
        if (amount == null) {
            errorBannerFlow.value = "Enter a valid withdrawal amount."
            return
        }
        viewModelScope.launch {
            isBusyFlow.value = true
            val result = repository.withdrawFromWallet(userId, amount, destination)
            isBusyFlow.value = false
            result.onSuccess {
                statusBannerFlow.value =
                    "Withdrawal of ${Formatters.formatCompactInr(amount)} sent to ${destination?.upiId}!"
                onComplete()
            }.onFailure { err ->
                errorBannerFlow.value = err.message ?: "Withdrawal failed."
            }
        }
    }

    fun createSellOrders(unitAmountText: String, quantity: Int, inlineUpiId: String = "") {
        clearBanners()
        val sellerId = uiState.value.currentUser?.id ?: return
        val unitAmount = unitAmountText.trim().toDoubleOrNull()
        if (unitAmount == null) {
            errorBannerFlow.value = "Enter a valid plan amount."
            return
        }
        viewModelScope.launch {
            isBusyFlow.value = true
            val result = repository.createSellPlans(sellerId, unitAmount, quantity, inlineUpiId)
            isBusyFlow.value = false
            result.onSuccess {
                statusBannerFlow.value =
                    "Published $quantity × ${Formatters.formatCompactInr(unitAmount)} sell plan(s) to the Marketplace!"
            }.onFailure { err ->
                errorBannerFlow.value = err.message ?: "Could not publish sell plan."
            }
        }
    }

    fun cancelSellPlan(planId: Long) {
        clearBanners()
        val sellerId = uiState.value.currentUser?.id ?: return
        viewModelScope.launch {
            repository.cancelSellPlan(planId, sellerId)
                .onSuccess {
                    statusBannerFlow.value = "Sell listing removed from Marketplace."
                }
                .onFailure { err ->
                    errorBannerFlow.value = err.message ?: "Could not cancel sell listing."
                }
        }
    }

    // --- Exclusive Admin User & Account Controls ---

    fun adminToggleBlockUser(targetUser: UserEntity) {
        clearBanners()
        val admin = uiState.value.currentUser
        if (admin == null || !admin.isAdmin) {
            errorBannerFlow.value = "Unauthorized: Admin access only."
            return
        }
        val nextBlocked = !targetUser.isBlocked
        viewModelScope.launch {
            isBusyFlow.value = true
            val result = repository.adminSetUserBlocked(admin.id, targetUser.id, nextBlocked)
            isBusyFlow.value = false
            result.onSuccess {
                statusBannerFlow.value = if (nextBlocked) {
                    "${targetUser.fullName} has been BLOCKED."
                } else {
                    "${targetUser.fullName} has been UNBLOCKED."
                }
            }.onFailure { err ->
                errorBannerFlow.value = err.message ?: "Could not update user status."
            }
        }
    }

    fun adminAdjustUserWallet(
        targetUserId: Long,
        mode: String,
        amountText: String,
        reason: String,
        onComplete: () -> Unit
    ) {
        clearBanners()
        val admin = uiState.value.currentUser
        if (admin == null || !admin.isAdmin) {
            errorBannerFlow.value = "Unauthorized: Admin access only."
            return
        }
        val amount = amountText.trim().toDoubleOrNull()
        if (amount == null) {
            errorBannerFlow.value = "Enter a valid numeric amount."
            return
        }
        viewModelScope.launch {
            isBusyFlow.value = true
            val result = repository.adminAdjustUserWallet(admin.id, targetUserId, mode, amount, reason)
            isBusyFlow.value = false
            result.onSuccess {
                statusBannerFlow.value = "Wallet balance updated successfully!"
                onComplete()
            }.onFailure { err ->
                errorBannerFlow.value = err.message ?: "Could not update wallet balance."
            }
        }
    }

    fun adminUpdateUserAccount(
        targetUserId: Long,
        fullName: String,
        mobile: String,
        email: String,
        primaryUpiId: String,
        newPassword: String?,
        onComplete: () -> Unit
    ) {
        clearBanners()
        val admin = uiState.value.currentUser
        if (admin == null || !admin.isAdmin) {
            errorBannerFlow.value = "Unauthorized: Admin access only."
            return
        }
        viewModelScope.launch {
            isBusyFlow.value = true
            val result = repository.adminUpdateUserAccount(
                adminId = admin.id,
                targetUserId = targetUserId,
                fullName = fullName,
                mobile = mobile,
                email = email,
                primaryUpiId = primaryUpiId,
                newPassword = newPassword
            )
            isBusyFlow.value = false
            result.onSuccess {
                statusBannerFlow.value = "Account details updated for $fullName!"
                onComplete()
            }.onFailure { err ->
                errorBannerFlow.value = err.message ?: "Could not update account."
            }
        }
    }

    fun adminDeleteUserAccount(targetUser: UserEntity, onComplete: () -> Unit) {
        clearBanners()
        val admin = uiState.value.currentUser
        if (admin == null || !admin.isAdmin) {
            errorBannerFlow.value = "Unauthorized: Admin access only."
            return
        }
        viewModelScope.launch {
            isBusyFlow.value = true
            val result = repository.adminDeleteUserAccount(admin.id, targetUser.id)
            isBusyFlow.value = false
            result.onSuccess {
                statusBannerFlow.value = "Deleted account: ${targetUser.fullName}"
                onComplete()
            }.onFailure { err ->
                errorBannerFlow.value = err.message ?: "Could not delete account."
            }
        }
    }
}
