package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_sessions WHERE userId = :userId ORDER BY isPinned DESC, updatedAt DESC")
    fun getSessionsForUserFlow(userId: String): Flow<List<ChatSessionEntity>>

    @Query("""
        SELECT DISTINCT s.* FROM chat_sessions s
        LEFT JOIN chat_messages m ON s.sessionId = m.sessionId
        WHERE s.userId = :userId AND (s.title LIKE '%' || :query || '%' OR m.content LIKE '%' || :query || '%')
        ORDER BY s.isPinned DESC, s.updatedAt DESC
    """)
    fun searchSessionsForUserFlow(userId: String, query: String): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: String): ChatSessionEntity?

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSessionFlow(sessionId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessagesForSessionList(sessionId: String): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSessionEntity)

    @Update
    suspend fun updateSession(session: ChatSessionEntity)

    @Query("DELETE FROM chat_sessions WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: String)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId AND timestamp > :timestamp")
    suspend fun deleteMessagesAfterTimestamp(sessionId: String, timestamp: Long)

    @Query("UPDATE chat_messages SET content = :newContent WHERE messageId = :messageId")
    suspend fun updateMessageContent(messageId: String, newContent: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)
}
