package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [Index(value = ["mobile"], unique = true)]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val fullName: String,
    val mobile: String,
    val email: String = "",
    val passwordHash: String,
    val primaryUpiId: String = "",
    val walletBalance: Double = 0.0,
    val totalCommissionEarned: Double = 0.0,
    val isAdmin: Boolean = false,
    val isBlocked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
