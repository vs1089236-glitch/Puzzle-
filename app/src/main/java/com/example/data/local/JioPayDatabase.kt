package com.example.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.MoneyPlanDao
import com.example.data.local.dao.PaymentAccountDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.MoneyPlanEntity
import com.example.data.local.entity.PaymentAccountEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.UserEntity
import com.example.util.SecurityUtils

@Database(
    entities = [
        UserEntity::class,
        PaymentAccountEntity::class,
        MoneyPlanEntity::class,
        TransactionEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class JioPayDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun paymentAccountDao(): PaymentAccountDao
    abstract fun moneyPlanDao(): MoneyPlanDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: JioPayDatabase? = null

        fun getInstance(context: Context): JioPayDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    JioPayDatabase::class.java,
                    "jio_pay_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .addCallback(SeedCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            db.beginTransaction()
            try {
                val now = System.currentTimeMillis()

                // 1. Seed Admin Account: Vishal Kumar (vishalkumar90@upi, ₹50,000 balance)
                val vishalId = db.insert(
                    "users",
                    SQLiteDatabase.CONFLICT_ABORT,
                    ContentValues().apply {
                        put("fullName", "Vishal Kumar")
                        put("mobile", "9876543210")
                        put("email", "vs1089236@gmail.com")
                        put("passwordHash", SecurityUtils.hashPassword("vishal123", "9876543210"))
                        put("primaryUpiId", "vishalkumar90@upi")
                        put("walletBalance", 50000.0)
                        put("totalCommissionEarned", 0.0)
                        put("isAdmin", 1)
                        put("isBlocked", 0)
                        put("createdAt", now - 86_400_000L * 3)
                    }
                )

                // 2. Seed Peer Buyer/Seller Accounts
                val aaravId = db.insert(
                    "users",
                    SQLiteDatabase.CONFLICT_ABORT,
                    ContentValues().apply {
                        put("fullName", "Aarav Sharma")
                        put("mobile", "9123456780")
                        put("email", "aarav@jiopay.in")
                        put("passwordHash", SecurityUtils.hashPassword("aarav123", "9123456780"))
                        put("primaryUpiId", "aarav.sharma@upi")
                        put("walletBalance", 12500.0)
                        put("totalCommissionEarned", 148.0)
                        put("isAdmin", 0)
                        put("isBlocked", 0)
                        put("createdAt", now - 86_400_000L * 2)
                    }
                )

                val priyaId = db.insert(
                    "users",
                    SQLiteDatabase.CONFLICT_ABORT,
                    ContentValues().apply {
                        put("fullName", "Priya Verma")
                        put("mobile", "9988776655")
                        put("email", "priya@jiopay.in")
                        put("passwordHash", SecurityUtils.hashPassword("priya123", "9988776655"))
                        put("primaryUpiId", "priya.verma@ybl")
                        put("walletBalance", 8400.0)
                        put("totalCommissionEarned", 80.0)
                        put("isAdmin", 0)
                        put("isBlocked", 0)
                        put("createdAt", now - 86_400_000L)
                    }
                )

                // 3. Seed Linked UPI & Bank Accounts
                db.insert(
                    "payment_accounts",
                    SQLiteDatabase.CONFLICT_REPLACE,
                    ContentValues().apply {
                        put("userId", vishalId)
                        put("accountType", "UPI")
                        put("upiId", "vishalkumar90@upi")
                        put("accountHolderName", "Vishal Kumar")
                        put("bankName", "Jio Payments Bank UPI")
                        put("accountNumberMasked", "")
                        put("ifscCode", "")
                        put("isPrimary", 1)
                        put("createdAt", now - 86_400_000L * 3)
                    }
                )
                db.insert(
                    "payment_accounts",
                    SQLiteDatabase.CONFLICT_REPLACE,
                    ContentValues().apply {
                        put("userId", vishalId)
                        put("accountType", "BANK")
                        put("upiId", "vishal.hdfc@okhdfcbank")
                        put("accountHolderName", "Vishal Kumar")
                        put("bankName", "HDFC Bank")
                        put("accountNumberMasked", "•••• 9012")
                        put("ifscCode", "HDFC0001824")
                        put("isPrimary", 0)
                        put("createdAt", now - 86_400_000L * 3)
                    }
                )
                db.insert(
                    "payment_accounts",
                    SQLiteDatabase.CONFLICT_REPLACE,
                    ContentValues().apply {
                        put("userId", aaravId)
                        put("accountType", "UPI")
                        put("upiId", "aarav.sharma@upi")
                        put("accountHolderName", "Aarav Sharma")
                        put("bankName", "State Bank of India UPI")
                        put("accountNumberMasked", "")
                        put("ifscCode", "")
                        put("isPrimary", 1)
                        put("createdAt", now - 86_400_000L * 2)
                    }
                )
                db.insert(
                    "payment_accounts",
                    SQLiteDatabase.CONFLICT_REPLACE,
                    ContentValues().apply {
                        put("userId", priyaId)
                        put("accountType", "UPI")
                        put("upiId", "priya.verma@ybl")
                        put("accountHolderName", "Priya Verma")
                        put("bankName", "ICICI Bank UPI")
                        put("accountNumberMasked", "")
                        put("ifscCode", "")
                        put("isPrimary", 1)
                        put("createdAt", now - 86_400_000L)
                    }
                )

                // 4. Seed Exact 54 Requested Plans:
                // 350 x 10, 500 x 10, 700 x 5, 850 x 9, 1000 x 20
                val planTiers = listOf(
                    350.0 to 10,
                    500.0 to 10,
                    700.0 to 5,
                    850.0 to 9,
                    1000.0 to 20
                )
                for ((amount, count) in planTiers) {
                    val commission = amount * 0.08
                    val receive = amount + commission
                    for (idx in 1..count) {
                        val code = "JP-${amount.toInt()}-${idx.toString().padStart(2, '0')}"
                        db.insert(
                            "money_plans",
                            SQLiteDatabase.CONFLICT_REPLACE,
                            ContentValues().apply {
                                put("planCode", code)
                                put("amount", amount)
                                put("commissionPercent", 8.0)
                                put("commissionAmount", commission)
                                put("totalReceiveAmount", receive)
                                put("sellerId", vishalId)
                                put("sellerName", "Vishal Kumar")
                                put("sellerUpiId", "vishalkumar90@upi")
                                put("status", "AVAILABLE")
                                putNull("pendingBuyerId")
                                putNull("pendingBuyerName")
                                putNull("pendingBuyerUpi")
                                putNull("pendingUtr")
                                put("createdAt", now - 3_600_000L)
                                put("updatedAt", now - 3_600_000L)
                            }
                        )
                    }
                }

                // 5. Seed Initial Transaction Logs (Success & Failed for User & Admin tracking)
                fun insertTxn(
                    ref: String,
                    planCode: String,
                    type: String,
                    status: String,
                    base: Double,
                    comm: Double,
                    net: Double,
                    buyerId: Long?,
                    buyerName: String,
                    buyerUpi: String,
                    sellerId: Long?,
                    sellerName: String,
                    sellerUpi: String,
                    note: String,
                    ts: Long
                ) {
                    db.insert(
                        "transactions",
                        SQLiteDatabase.CONFLICT_REPLACE,
                        ContentValues().apply {
                            put("referenceId", ref)
                            putNull("planId")
                            put("planCode", planCode)
                            put("type", type)
                            put("status", status)
                            put("baseAmount", base)
                            put("commissionAmount", comm)
                            put("netAmount", net)
                            if (buyerId != null) put("buyerId", buyerId) else putNull("buyerId")
                            put("buyerName", buyerName)
                            put("buyerUpi", buyerUpi)
                            if (sellerId != null) put("sellerId", sellerId) else putNull("sellerId")
                            put("sellerName", sellerName)
                            put("sellerUpi", sellerUpi)
                            put("note", note)
                            put("createdAt", ts)
                            put("updatedAt", ts)
                        }
                    )
                }

                insertTxn(
                    ref = "UTR402198340001",
                    planCode = "VAULT-INIT",
                    type = "WALLET_DEPOSIT",
                    status = "SUCCESS",
                    base = 50000.0,
                    comm = 0.0,
                    net = 50000.0,
                    buyerId = vishalId,
                    buyerName = "Vishal Kumar",
                    buyerUpi = "vishalkumar90@upi",
                    sellerId = null,
                    sellerName = "HDFC Bank •••• 9012",
                    sellerUpi = "HDFC0001824",
                    note = "Master seller vault capital deposited",
                    ts = now - 7_200_000L
                )

                insertTxn(
                    ref = "UTR402198341209",
                    planCode = "JP-1000-00",
                    type = "BUY_ORDER",
                    status = "SUCCESS",
                    base = 1000.0,
                    comm = 80.0,
                    net = 1080.0,
                    buyerId = aaravId,
                    buyerName = "Aarav Sharma",
                    buyerUpi = "aarav.sharma@upi",
                    sellerId = vishalId,
                    sellerName = "Vishal Kumar",
                    sellerUpi = "vishalkumar90@upi",
                    note = "Verified by vishalkumar90@upi • ₹1,000 + ₹80 (8% Profit) credited",
                    ts = now - 5_400_000L
                )

                insertTxn(
                    ref = "UTR402198349912",
                    planCode = "JP-850-00",
                    type = "BUY_ORDER",
                    status = "SUCCESS",
                    base = 850.0,
                    comm = 68.0,
                    net = 918.0,
                    buyerId = aaravId,
                    buyerName = "Aarav Sharma",
                    buyerUpi = "aarav.sharma@upi",
                    sellerId = vishalId,
                    sellerName = "Vishal Kumar",
                    sellerUpi = "vishalkumar90@upi",
                    note = "Verified by vishalkumar90@upi • ₹850 + ₹68 (8% Profit) credited",
                    ts = now - 3_600_000L
                )

                insertTxn(
                    ref = "UTR402198350114",
                    planCode = "JP-500-00",
                    type = "BUY_ORDER",
                    status = "FAILED",
                    base = 500.0,
                    comm = 40.0,
                    net = 540.0,
                    buyerId = priyaId,
                    buyerName = "Priya Verma",
                    buyerUpi = "priya.verma@ybl",
                    sellerId = vishalId,
                    sellerName = "Vishal Kumar",
                    sellerUpi = "vishalkumar90@upi",
                    note = "UPI UTR mismatch — payment not received at vishalkumar90@upi",
                    ts = now - 1_800_000L
                )

                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }
}
