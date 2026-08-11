package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomPersonaDao {
    @Query("SELECT * FROM custom_personas WHERE userId = :userId OR userId = '' ORDER BY isDefaultPreset DESC, createdAt ASC")
    fun getPersonasFlow(userId: String): Flow<List<CustomPersonaEntity>>

    @Query("SELECT * FROM custom_personas WHERE userId = :userId OR userId = '' ORDER BY isDefaultPreset DESC, createdAt ASC")
    suspend fun getPersonas(userId: String): List<CustomPersonaEntity>

    @Query("SELECT * FROM custom_personas WHERE personaId = :personaId LIMIT 1")
    suspend fun getPersonaById(personaId: String): CustomPersonaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersona(persona: CustomPersonaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersonas(personas: List<CustomPersonaEntity>)

    @Query("DELETE FROM custom_personas WHERE personaId = :personaId AND isDefaultPreset = 0")
    suspend fun deletePersona(personaId: String)
}
