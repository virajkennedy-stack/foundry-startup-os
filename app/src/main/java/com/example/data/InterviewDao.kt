package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface InterviewDao {
    @Query("SELECT * FROM execution_plan_interviews WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getInterviewBySessionId(sessionId: String): ExecutionPlanInterviewEntity?

    @Query("SELECT * FROM execution_plan_interviews WHERE sessionId = :sessionId LIMIT 1")
    fun getInterviewBySessionIdFlow(sessionId: String): Flow<ExecutionPlanInterviewEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateInterview(interview: ExecutionPlanInterviewEntity)

    @Query("DELETE FROM execution_plan_interviews WHERE sessionId = :sessionId")
    suspend fun deleteInterviewForSession(sessionId: String)
}
