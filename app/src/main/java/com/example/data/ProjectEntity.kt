package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey
    val projectId: String,
    val ownerId: String,
    val title: String,
    val description: String = "",
    val originalIdea: String = "",
    val problem: String = "",
    val goal: String = "",
    val status: String = "ACTIVE",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    // Future-ready fields for Phase 3 extension
    val associatedConversationIds: String = "",
    val aiAnalysis: String = "",
    val decisionsJson: String = "[]",
    val requirementsJson: String = "[]",
    val nextActionsJson: String = "[]"
)
