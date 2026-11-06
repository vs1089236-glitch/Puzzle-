package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query(
        "SELECT * FROM transactions " +
        "WHERE (type = 'BUY_ORDER' AND buyerId = :userId) " +
        "   OR (type = 'SELL_TRANSFER' AND sellerId = :userId) " +
        "   OR (type IN ('WALLET_DEPOSIT', 'WITHDRAWAL', 'ADMIN_CREDIT', 'ADMIN_DEBIT') AND buyerId = :userId) " +
        "ORDER BY createdAt DESC, id DESC"
    )
    fun observeUserTransactions(userId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE type != 'SELL_TRANSFER' ORDER BY createdAt DESC, id DESC")
    fun observeAllTransactions(): Flow<List<TransactionEntity>>

    @Query(
        "SELECT * FROM transactions WHERE planId = :planId AND type = 'BUY_ORDER' AND status = 'PENDING' " +
        "ORDER BY id DESC LIMIT 1"
    )
    suspend fun getPendingBuyTransactionForPlan(planId: Long): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Query(
        "UPDATE transactions SET status = :newStatus, note = :note, updatedAt = :updatedAt " +
        "WHERE id = :transactionId"
    )
    suspend fun updateTransactionStatus(
        transactionId: Long,
        newStatus: String,
        note: String,
        updatedAt: Long
    )
}
