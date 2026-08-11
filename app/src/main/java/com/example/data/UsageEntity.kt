package com.example.data

import androidx.room.Entity

@Entity(tableName = "usage_records", primaryKeys = ["userId", "featureKey"])
data class UsageEntity(
    val userId: String,
    val featureKey: String,
    val count: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)
