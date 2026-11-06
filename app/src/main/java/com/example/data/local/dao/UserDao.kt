package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :userId")
    fun observeUserById(userId: Long): Flow<UserEntity?>

    @Query("SELECT * FROM users ORDER BY isAdmin DESC, id ASC")
    fun observeAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: Long): UserEntity?

    @Query("SELECT * FROM users WHERE mobile = :mobile LIMIT 1")
    suspend fun getUserByMobile(mobile: String): UserEntity?

    @Query("SELECT * FROM users WHERE mobile = :identifier OR LOWER(email) = LOWER(:identifier) LIMIT 1")
    suspend fun getUserByMobileOrEmail(identifier: String): UserEntity?

    @Query("SELECT * FROM users WHERE isAdmin = 1 LIMIT 1")
    suspend fun getAdminUser(): UserEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET primaryUpiId = :upiId WHERE id = :userId")
    suspend fun updatePrimaryUpi(userId: Long, upiId: String)

    @Query("UPDATE users SET walletBalance = walletBalance + :delta WHERE id = :userId")
    suspend fun adjustWalletBalance(userId: Long, delta: Double)

    @Query(
        "UPDATE users SET walletBalance = walletBalance + :creditAmount, " +
        "totalCommissionEarned = totalCommissionEarned + :commissionEarned WHERE id = :userId"
    )
    suspend fun creditBuyerWalletWithCommission(
        userId: Long,
        creditAmount: Double,
        commissionEarned: Double
    )

    @Query("UPDATE users SET isBlocked = :blocked WHERE id = :userId AND isAdmin = 0")
    suspend fun setUserBlocked(userId: Long, blocked: Boolean)

    @Query("UPDATE users SET walletBalance = :newBalance WHERE id = :userId")
    suspend fun setWalletBalance(userId: Long, newBalance: Double)

    @Query("DELETE FROM users WHERE id = :userId AND isAdmin = 0")
    suspend fun deleteNonAdminUser(userId: Long)
}
