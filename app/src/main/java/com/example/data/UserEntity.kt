package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val userId: String,
    val email: String,
    val passwordHash: String,
    val displayName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isOnboardingComplete: Boolean = false,
    val personalityPreference: String = "BALANCED", // "CHILL", "BALANCED", "CHALLENGER"
    val appearancePreference: String = "DARK",      // "SYSTEM", "LIGHT", "DARK"
    val profilePhotoUri: String? = null,
    val plan: String = "FREE",                      // "FREE", "PRO"
    val subscriptionStatus: String = "FREE",        // "FREE", "ACTIVE_PRO", "INACTIVE_PRO", "EXPIRED_PRO"
    val subscriptionExpiresAt: Long? = null
)
