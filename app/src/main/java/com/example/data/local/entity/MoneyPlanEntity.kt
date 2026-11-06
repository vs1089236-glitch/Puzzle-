package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "money_plans",
    indices = [Index(value = ["status"]), Index(value = ["sellerId"]), Index(value = ["amount"])]
)
data class MoneyPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val planCode: String,
    val amount: Double,
    val commissionPercent: Double = 8.0,
    val commissionAmount: Double,
    val totalReceiveAmount: Double,
    val sellerId: Long,
    val sellerName: String,
    val sellerUpiId: String,
    val status: String, // "AVAILABLE", "PENDING_VERIFICATION", "SOLD"
    val pendingBuyerId: Long? = null,
    val pendingBuyerName: String? = null,
    val pendingBuyerUpi: String? = null,
    val pendingUtr: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
