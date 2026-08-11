package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    fun getUserByIdFlow(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET displayName = :displayName, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateDisplayName(userId: String, displayName: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE users SET personalityPreference = :personality, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updatePersonality(userId: String, personality: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE users SET appearancePreference = :appearance, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateAppearance(userId: String, appearance: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE users SET profilePhotoUri = :photoUri, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateProfilePhoto(userId: String, photoUri: String?, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE users SET isOnboardingComplete = :isComplete, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateOnboardingComplete(userId: String, isComplete: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE users SET plan = :plan, subscriptionStatus = :status, subscriptionExpiresAt = :expiresAt, updatedAt = :updatedAt WHERE userId = :userId")
    suspend fun updateUserPlan(userId: String, plan: String, status: String, expiresAt: Long?, updatedAt: Long = System.currentTimeMillis())
}
