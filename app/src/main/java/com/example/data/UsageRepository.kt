package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class UsageRepository(
    private val usageDao: UsageDao,
    private val firestoreService: FirestoreService
) {
    companion object {
        @Volatile
        private var INSTANCE: UsageRepository? = null

        fun getInstance(context: Context): UsageRepository {
            return INSTANCE ?: synchronized(this) {
                val db = FoundryDatabase.getDatabase(context)
                val instance = UsageRepository(
                    usageDao = db.usageDao(),
                    firestoreService = FirestoreService()
                )
                INSTANCE = instance
                instance
            }
        }
    }

    fun getUsageMap(userId: String): Flow<Map<FeatureId, Int>> {
        if (userId.isBlank()) return flowOf(emptyMap())
        return usageDao.getUsageForUser(userId).map { records ->
            val resultMap = mutableMapOf<FeatureId, Int>()
            for (record in records) {
                try {
                    val feature = FeatureId.valueOf(record.featureKey)
                    resultMap[feature] = record.count
                } catch (e: Exception) {
                    // Ignore unknown feature keys
                }
            }
            resultMap
        }
    }

    suspend fun getUsageCount(userId: String, feature: FeatureId): Int {
        if (userId.isBlank()) return 0
        return usageDao.getUsageRecord(userId, feature.name)?.count ?: 0
    }

    suspend fun canPerformAction(userId: String, userPlan: UserPlan, feature: FeatureId): Result<Boolean> {
        if (userId.isBlank()) return Result.failure(IllegalStateException("User is not authenticated"))

        val entitlement = EntitlementManager.getEntitlement(userPlan, feature)
        if (!entitlement.isAllowed) {
            return Result.success(false)
        }

        val currentCount = getUsageCount(userId, feature)
        val limitInfo = EntitlementManager.getUsageLimit(userPlan, feature, currentCount)

        if (!limitInfo.isUnlimited && limitInfo.remainingUsage <= 0) {
            return Result.success(false)
        }

        return Result.success(true)
    }

    suspend fun recordSuccessfulUsage(userId: String, feature: FeatureId): Result<Unit> {
        if (userId.isBlank()) return Result.failure(IllegalStateException("User is not authenticated"))

        return try {
            val existing = usageDao.getUsageRecord(userId, feature.name)
            val newCount = (existing?.count ?: 0) + 1
            val updatedRecord = UsageEntity(
                userId = userId,
                featureKey = feature.name,
                count = newCount,
                lastUpdated = System.currentTimeMillis()
            )
            usageDao.saveUsageRecord(updatedRecord)
            firestoreService.incrementUsageAtomic(userId, feature.name)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setUsageCount(userId: String, feature: FeatureId, count: Int): Result<Unit> {
        if (userId.isBlank()) return Result.failure(IllegalStateException("User is not authenticated"))

        return try {
            val record = UsageEntity(
                userId = userId,
                featureKey = feature.name,
                count = count,
                lastUpdated = System.currentTimeMillis()
            )
            usageDao.saveUsageRecord(record)
            firestoreService.saveUsage(userId, feature.name, count)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncUsageFromRemote(userId: String): Result<Unit> {
        if (userId.isBlank()) return Result.success(Unit)

        return try {
            val remoteMap = firestoreService.fetchUsageForUser(userId)
            val entities = remoteMap.map { (key, count) ->
                UsageEntity(
                    userId = userId,
                    featureKey = key,
                    count = count,
                    lastUpdated = System.currentTimeMillis()
                )
            }
            if (entities.isNotEmpty()) {
                usageDao.saveUsageRecords(entities)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
