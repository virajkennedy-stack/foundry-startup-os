package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "custom_personas")
data class CustomPersonaEntity(
    @PrimaryKey
    val personaId: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val name: String,
    val tagline: String = "Custom Brainstorming Persona",
    val systemPrompt: String,
    val iconName: String = "Psychology",
    val isDefaultPreset: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
