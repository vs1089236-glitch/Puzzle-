package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["status"]),
        Index(value = ["buyerId"]),
        Index(value = ["sellerId"]),
        Index(value = ["createdAt"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val referenceId: String,
    val planId: Long? = null,
    val planCode: String = "",
    val type: String, // "BUY_ORDER", "SELL_TRANSFER", "WALLET_DEPOSIT", "WITHDRAWAL"
    val status: String, // "PENDING", "SUCCESS", "FAILED"
    val baseAmount: Double,
    val commissionAmount: Double = 0.0,
    val netAmount: Double,
    val buyerId: Long? = null,
    val buyerName: String = "",
    val buyerUpi: String = "",
    val sellerId: Long? = null,
    val sellerName: String = "",
    val sellerUpi: String = "",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
