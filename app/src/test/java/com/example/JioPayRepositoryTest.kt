package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.JioPayDatabase
import com.example.data.repository.JioPayRepository
import com.example.util.QrCodeGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class JioPayRepositoryTest {

    private lateinit var db: JioPayDatabase
    private lateinit var repository: JioPayRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, JioPayDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        db.close()
        context.deleteDatabase("jio_pay_database")
        db = JioPayDatabase.getInstance(context)
        repository = JioPayRepository(db)
    }

    @After
    fun tearDown() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db.close()
        val field = JioPayDatabase::class.java.getDeclaredField("INSTANCE")
        field.isAccessible = true
        field.set(null, null)
        context.deleteDatabase("jio_pay_database")
    }

    @Test
    fun seededDatabase_hasAdminVishalWith50kAndExact54Plans() = runBlocking {
        val users = repository.observeAllUsers().first()
        val vishal = users.firstOrNull { it.isAdmin }
        assertNotNull(vishal)
        assertEquals("vishalkumar90@upi", vishal!!.primaryUpiId)
        assertEquals(50000.0, vishal.walletBalance, 0.01)

        val plans = repository.observeMarketplacePlans().first()
        assertEquals(54, plans.size)
        assertEquals(10, plans.count { it.amount == 350.0 })
        assertEquals(10, plans.count { it.amount == 500.0 })
        assertEquals(5, plans.count { it.amount == 700.0 })
        assertEquals(9, plans.count { it.amount == 850.0 })
        assertEquals(20, plans.count { it.amount == 1000.0 })
    }

    @Test
    fun buyPlanAndSellerVerification_transfersWalletBalanceWith8PercentCommission() = runBlocking {
        val vishal = repository.loginWithPassword("9876543210", "vishal123").getOrThrow()
        val aarav = repository.loginWithPassword("9123456780", "aarav123").getOrThrow()

        val initialVishalBalance = vishal.walletBalance
        val initialAaravBalance = aarav.walletBalance

        val plan500 = repository.observeMarketplacePlans().first().first { it.amount == 500.0 }
        val submitRes = repository.submitBuyOrderPayment(
            planId = plan500.id,
            buyerId = aarav.id,
            utrNumber = "UTR998877665544"
        )
        assertTrue(submitRes.isSuccess)

        val verifyRes = repository.verifyAndCompleteOrder(
            planId = plan500.id,
            verifierUserId = vishal.id
        )
        assertTrue(verifyRes.isSuccess)

        val updatedVishal = repository.observeUser(vishal.id).first()!!
        val updatedAarav = repository.observeUser(aarav.id).first()!!

        // Seller deducted ₹500, Buyer credited ₹500 + 8% (₹40) = ₹540
        assertEquals(initialVishalBalance - 500.0, updatedVishal.walletBalance, 0.01)
        assertEquals(initialAaravBalance + 540.0, updatedAarav.walletBalance, 0.01)
    }

    @Test
    fun peerUserCanSellMoneyWithInlineUpiAndCancelOrVerify() = runBlocking {
        val otpUser = repository.loginOrCreateWithOtp(
            mobile = "9811122233",
            newUserName = "Rohan Das",
            newUserUpi = ""
        ).getOrThrow()

        // Deposit ₹2,000 so Rohan can sell money
        assertTrue(repository.depositToWallet(otpUser.id, 2000.0, "rohan.das@okaxis", "UTR112233445566").isSuccess)

        // Rohan lists 2 plans of ₹500 using inline UPI ID
        assertTrue(repository.createSellPlans(otpUser.id, 500.0, 2, inlineUpiId = "rohan.das@okaxis").isSuccess)
        val rohanPlans = repository.observeSellerPlans(otpUser.id).first()
        assertEquals(2, rohanPlans.size)
        assertEquals("rohan.das@okaxis", rohanPlans.first().sellerUpiId)

        // Cancel one plan
        assertTrue(repository.cancelSellPlan(rohanPlans[1].id, otpUser.id).isSuccess)
        assertEquals(1, repository.observeSellerPlans(otpUser.id).first().size)

        // Aarav buys Rohan's remaining plan, and Rohan rejects it -> logged as FAILED in Admin Panel
        val aarav = repository.loginWithPassword("9123456780", "aarav123").getOrThrow()
        assertTrue(repository.submitBuyOrderPayment(rohanPlans[0].id, aarav.id, "UTR000111222333").isSuccess)
        assertTrue(repository.rejectOrFailOrder(rohanPlans[0].id, otpUser.id, "UTR not received").isSuccess)

        val allTxns = repository.observeAllTransactions().first()
        assertTrue(allTxns.any { it.status == "FAILED" && it.referenceId == "UTR000111222333" })
    }

    @Test
    fun adminExclusiveControls_blockEditWalletProfileAndDeleteUsers() = runBlocking {
        val vishal = repository.loginWithPassword("9876543210", "vishal123").getOrThrow()
        val aarav = repository.loginWithPassword("9123456780", "aarav123").getOrThrow()
        val priya = repository.loginWithPassword("9988776655", "priya123").getOrThrow()

        // Non-admin cannot block or edit accounts
        assertTrue(repository.adminSetUserBlocked(aarav.id, priya.id, true).isFailure)
        assertTrue(repository.adminAdjustUserWallet(aarav.id, priya.id, "CREDIT", 500.0, "").isFailure)

        // Admin blocks Aarav -> Aarav cannot log in via password or OTP
        assertTrue(repository.adminSetUserBlocked(vishal.id, aarav.id, true).isSuccess)
        assertTrue(repository.loginWithPassword("9123456780", "aarav123").isFailure)
        assertTrue(repository.loginOrCreateWithOtp("9123456780", "", "").isFailure)

        // Admin unblocks Aarav -> Aarav can log in again
        assertTrue(repository.adminSetUserBlocked(vishal.id, aarav.id, false).isSuccess)
        assertTrue(repository.loginWithPassword("9123456780", "aarav123").isSuccess)

        // Admin credits ₹1,500 to Aarav's wallet
        val beforeBalance = repository.observeUser(aarav.id).first()!!.walletBalance
        assertTrue(repository.adminAdjustUserWallet(vishal.id, aarav.id, "CREDIT", 1500.0, "Bonus").isSuccess)
        assertEquals(beforeBalance + 1500.0, repository.observeUser(aarav.id).first()!!.walletBalance, 0.01)

        // Admin updates Aarav's UPI & password
        assertTrue(
            repository.adminUpdateUserAccount(
                adminId = vishal.id,
                targetUserId = aarav.id,
                fullName = "Aarav K Sharma",
                mobile = "9123456780",
                email = "aarav.new@jiopay.in",
                primaryUpiId = "aarav.new@ybl",
                newPassword = "newpass99"
            ).isSuccess
        )
        val updatedAarav = repository.loginWithPassword("9123456780", "newpass99").getOrThrow()
        assertEquals("Aarav K Sharma", updatedAarav.fullName)
        assertEquals("aarav.new@ybl", updatedAarav.primaryUpiId)

        // Admin deletes Priya's account
        assertTrue(repository.adminDeleteUserAccount(vishal.id, priya.id).isSuccess)
        assertNull(repository.findUserByMobile("9988776655"))
        assertFalse(repository.observeAllUsers().first().any { it.id == priya.id })
    }

    @Test
    fun qrCodeGenerator_encodesValidSquareMatrixForVishalUpi() {
        val matrix = QrCodeGenerator.encodeUpiQr(
            upiId = "vishalkumar90@upi",
            payeeName = "Vishal Kumar",
            amount = 500.0,
            note = "JioPay JP-500-01"
        )
        assertTrue(matrix.size >= 29)
        assertEquals(matrix.size * matrix.size, matrix.modules.size)
        assertTrue(matrix[0, 0])
    }
}
