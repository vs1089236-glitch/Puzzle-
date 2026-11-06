package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.PaymentAccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentAccountDao {
    @Query("SELECT * FROM payment_accounts WHERE userId = :userId ORDER BY isPrimary DESC, id DESC")
    fun observeAccountsForUser(userId: Long): Flow<List<PaymentAccountEntity>>

    @Query("SELECT * FROM payment_accounts WHERE id = :accountId")
    suspend fun getAccountById(accountId: Long): PaymentAccountEntity?

    @Query("SELECT * FROM payment_accounts WHERE userId = :userId ORDER BY isPrimary DESC, id DESC")
    suspend fun getAccountsForUser(userId: Long): List<PaymentAccountEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: PaymentAccountEntity): Long

    @Query("UPDATE payment_accounts SET isPrimary = 0 WHERE userId = :userId")
    suspend fun clearPrimaryForUser(userId: Long)

    @Query("UPDATE payment_accounts SET isPrimary = 1 WHERE id = :accountId AND userId = :userId")
    suspend fun markPrimary(userId: Long, accountId: Long)

    @Query("DELETE FROM payment_accounts WHERE id = :accountId AND userId = :userId")
    suspend fun deleteAccount(userId: Long, accountId: Long)
}
