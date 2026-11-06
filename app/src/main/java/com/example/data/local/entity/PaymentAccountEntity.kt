package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payment_accounts",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"])]
)
data class PaymentAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val userId: Long,
    val accountType: String, // "UPI" or "BANK"
    val upiId: String,
    val accountHolderName: String,
    val bankName: String = "UPI Handle",
    val accountNumberMasked: String = "",
    val ifscCode: String = "",
    val isPrimary: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
