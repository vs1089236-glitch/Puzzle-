package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.MoneyPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MoneyPlanDao {
    @Query(
        "SELECT * FROM money_plans WHERE status != 'SOLD' " +
        "AND sellerId IN (SELECT id FROM users WHERE isBlocked = 0) " +
        "ORDER BY CASE status WHEN 'PENDING_VERIFICATION' THEN 0 ELSE 1 END, amount ASC, id ASC"
    )
    fun observeActiveMarketplacePlans(): Flow<List<MoneyPlanEntity>>

    @Query("SELECT * FROM money_plans WHERE sellerId = :sellerId ORDER BY updatedAt DESC, id DESC")
    fun observePlansBySeller(sellerId: Long): Flow<List<MoneyPlanEntity>>

    @Query("SELECT * FROM money_plans WHERE status = 'PENDING_VERIFICATION' ORDER BY updatedAt DESC")
    fun observeAllPendingVerifications(): Flow<List<MoneyPlanEntity>>

    @Query(
        "SELECT * FROM money_plans WHERE sellerId = :sellerId AND status = 'PENDING_VERIFICATION' " +
        "ORDER BY updatedAt DESC"
    )
    fun observePendingVerificationsForSeller(sellerId: Long): Flow<List<MoneyPlanEntity>>

    @Query("SELECT * FROM money_plans WHERE id = :planId")
    suspend fun getPlanById(planId: Long): MoneyPlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: MoneyPlanEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlans(plans: List<MoneyPlanEntity>)

    @Update
    suspend fun updatePlan(plan: MoneyPlanEntity)

    @Query("UPDATE money_plans SET sellerUpiId = :newUpi WHERE sellerId = :sellerId AND status = 'AVAILABLE'")
    suspend fun updateSellerUpiOnAvailablePlans(sellerId: Long, newUpi: String)

    @Query(
        "UPDATE money_plans SET sellerName = :newName, sellerUpiId = :newUpi " +
        "WHERE sellerId = :sellerId AND status = 'AVAILABLE'"
    )
    suspend fun updateSellerDetailsOnAvailablePlans(sellerId: Long, newName: String, newUpi: String)

    @Query("DELETE FROM money_plans WHERE id = :planId AND sellerId = :sellerId AND status = 'AVAILABLE'")
    suspend fun deleteAvailablePlanBySeller(planId: Long, sellerId: Long): Int

    @Query("DELETE FROM money_plans WHERE sellerId = :sellerId AND status != 'SOLD'")
    suspend fun deleteUnsoldPlansBySeller(sellerId: Long)
}
