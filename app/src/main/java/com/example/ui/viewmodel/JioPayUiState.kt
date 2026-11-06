package com.example.ui.viewmodel

import com.example.data.local.entity.MoneyPlanEntity
import com.example.data.local.entity.PaymentAccountEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.UserEntity

enum class AuthMode {
    OTP,
    PASSWORD,
    REGISTER
}

enum class PlanFilter(val label: String, val amountMatch: Double?) {
    ALL("All", null),
    PLAN_350("₹350", 350.0),
    PLAN_500("₹500", 500.0),
    PLAN_700("₹700", 700.0),
    PLAN_850("₹850", 850.0),
    PLAN_1000("₹1000", 1000.0)
}

enum class TxnFilter(val label: String, val statusMatch: String?) {
    ALL("All", null),
    SUCCESS("Success", "SUCCESS"),
    PENDING("Pending", "PENDING"),
    FAILED("Failed", "FAILED")
}

data class OtpSessionState(
    val mobile: String = "",
    val generatedOtp: String = "",
    val isOtpSent: Boolean = false,
    val isNewUser: Boolean = false,
    val secondsRemaining: Int = 0,
    val enteredOtp: String = "",
    val newUserName: String = "",
    val newUserUpi: String = ""
)

data class JioPayUiState(
    val isSessionLoading: Boolean = true,
    val currentUser: UserEntity? = null,
    val allUsers: List<UserEntity> = emptyList(),
    val paymentAccounts: List<PaymentAccountEntity> = emptyList(),
    val marketplacePlans: List<MoneyPlanEntity> = emptyList(),
    val sellerPlans: List<MoneyPlanEntity> = emptyList(),
    val sellerPendingVerifications: List<MoneyPlanEntity> = emptyList(),
    val allPendingVerifications: List<MoneyPlanEntity> = emptyList(),
    val userTransactions: List<TransactionEntity> = emptyList(),
    val allTransactions: List<TransactionEntity> = emptyList(),
    val selectedPlanFilter: PlanFilter = PlanFilter.ALL,
    val selectedUserTxnFilter: TxnFilter = TxnFilter.ALL,
    val selectedAdminTxnFilter: TxnFilter = TxnFilter.ALL,
    val authMode: AuthMode = AuthMode.OTP,
    val otpSession: OtpSessionState = OtpSessionState(),
    val activeCheckoutPlan: MoneyPlanEntity? = null,
    val isBusy: Boolean = false,
    val statusBanner: String? = null,
    val errorBanner: String? = null
)
