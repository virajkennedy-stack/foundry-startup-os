package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageDao {
    @Query("SELECT * FROM usage_records WHERE userId = :userId")
    fun getUsageForUser(userId: String): Flow<List<UsageEntity>>

    @Query("SELECT * FROM usage_records WHERE userId = :userId AND featureKey = :featureKey")
    suspend fun getUsageRecord(userId: String, featureKey: String): UsageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUsageRecord(record: UsageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUsageRecords(records: List<UsageEntity>)

    @Query("UPDATE usage_records SET count = count + 1, lastUpdated = :lastUpdated WHERE userId = :userId AND featureKey = :featureKey")
    suspend fun incrementUsage(userId: String, featureKey: String, lastUpdated: Long = System.currentTimeMillis())

    @Query("DELETE FROM usage_records WHERE userId = :userId")
    suspend fun clearUsageForUser(userId: String)
}
