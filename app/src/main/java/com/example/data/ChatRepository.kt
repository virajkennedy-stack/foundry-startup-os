package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.util.UUID

class ChatRepository(context: Context) {
    private val db = FoundryDatabase.getDatabase(context)
    private val chatDao = db.chatDao()
    private val interviewDao = db.interviewDao()
    private val geminiService = GeminiService(context)
    private val firestoreService = FirestoreService()

    fun getInterviewFlow(sessionId: String?): Flow<ExecutionPlanInterviewEntity?> {
        if (sessionId.isNullOrEmpty()) return flowOf(null)
        return interviewDao.getInterviewBySessionIdFlow(sessionId)
    }

    suspend fun getInterviewBySessionId(sessionId: String): ExecutionPlanInterviewEntity? {
        if (sessionId.isBlank()) return null
        return interviewDao.getInterviewBySessionId(sessionId)
    }

    suspend fun saveInterview(interview: ExecutionPlanInterviewEntity) {
        interviewDao.insertOrUpdateInterview(interview)
        firestoreService.saveInterview(interview)
    }

    suspend fun resetInterview(sessionId: String, userId: String): ExecutionPlanInterviewEntity {
        val newInterview = ExecutionPlanInterviewEntity(
            sessionId = sessionId,
            userId = userId,
            status = "NOT_STARTED",
            currentQuestionIndex = 0
        )
        saveInterview(newInterview)
        return newInterview
    }

    fun getSessionsForUserFlow(userId: String?): Flow<List<ChatSessionEntity>> {
        if (userId.isNullOrEmpty()) return flowOf(emptyList())
        return chatDao.getSessionsForUserFlow(userId)
    }

    fun searchSessionsForUserFlow(userId: String?, query: String): Flow<List<ChatSessionEntity>> {
        if (userId.isNullOrEmpty()) return flowOf(emptyList())
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) {
            return chatDao.getSessionsForUserFlow(userId)
        }
        return chatDao.searchSessionsForUserFlow(userId, cleanQuery)
    }

    suspend fun syncFromFirestore(userId: String) {
        if (userId.isBlank()) return
        try {
            val remoteSessions = firestoreService.fetchSessionsForUser(userId)
            for (session in remoteSessions) {
                chatDao.insertSession(session)
                val remoteMessages = firestoreService.fetchMessagesForSession(userId, session.sessionId)
                for (msg in remoteMessages) {
                    chatDao.insertMessage(msg)
                }
            }
        } catch (e: Exception) {
            // Ignore if offline
        }
    }

    fun getMessagesForSessionFlow(sessionId: String?): Flow<List<ChatMessageEntity>> {
        if (sessionId.isNullOrEmpty()) return flowOf(emptyList())
        return chatDao.getMessagesForSessionFlow(sessionId)
    }

    suspend fun createSession(userId: String, initialTitle: String = "New Chat"): ChatSessionEntity {
        val session = ChatSessionEntity(
            sessionId = UUID.randomUUID().toString(),
            userId = userId,
            title = initialTitle,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        chatDao.insertSession(session)
        firestoreService.saveSession(session)
        return session
    }

    suspend fun updateSessionTitle(userId: String, sessionId: String, title: String) {
        val session = chatDao.getSessionById(sessionId) ?: return
        if (session.userId != userId) return
        val updated = session.copy(title = title, updatedAt = System.currentTimeMillis())
        chatDao.updateSession(updated)
        firestoreService.updateSessionTitle(userId, sessionId, title, updated.updatedAt)
    }

    suspend fun togglePinSession(userId: String, sessionId: String) {
        val session = chatDao.getSessionById(sessionId) ?: return
        if (session.userId != userId) return
        val updated = session.copy(isPinned = !session.isPinned, updatedAt = System.currentTimeMillis())
        chatDao.updateSession(updated)
        firestoreService.saveSession(updated)
    }

    suspend fun deleteSession(userId: String, sessionId: String) {
        val session = chatDao.getSessionById(sessionId) ?: return
        if (session.userId != userId) return
        chatDao.deleteMessagesForSession(sessionId)
        chatDao.deleteSession(sessionId)
        firestoreService.deleteSession(userId, sessionId)
    }

    suspend fun saveUserMessage(sessionId: String, userContent: String): ChatMessageEntity {
        val userMsg = ChatMessageEntity(
            messageId = UUID.randomUUID().toString(),
            sessionId = sessionId,
            sender = "USER",
            content = userContent.trim(),
            timestamp = System.currentTimeMillis()
        )
        chatDao.insertMessage(userMsg)

        val currentSession = chatDao.getSessionById(sessionId)
        if (currentSession != null) {
            val now = System.currentTimeMillis()
            val newTitle = if (currentSession.title == "New Chat" || currentSession.title.isBlank()) {
                val cleanContent = userContent.trim()
                if (cleanContent.length > 28) cleanContent.take(28).trim() + "..." else cleanContent
            } else {
                currentSession.title
            }
            val updated = currentSession.copy(title = newTitle, updatedAt = now)
            chatDao.updateSession(updated)
            firestoreService.saveSession(updated)
            firestoreService.saveMessage(currentSession.userId, userMsg)
        }

        return userMsg
    }

    suspend fun editUserMessageAndRegenerate(
        sessionId: String,
        messageId: String,
        newContent: String,
        personality: String = "BALANCED",
        userName: String? = null,
        projectName: String? = null,
        enableSearchGrounding: Boolean = false,
        locationContext: String? = null,
        enableDeepReasoning: Boolean = false
    ): Result<ChatMessageEntity> {
        val messages = chatDao.getMessagesForSessionList(sessionId)
        val targetMessage = messages.find { it.messageId == messageId }
            ?: return Result.failure(Exception("Message not found."))

        val trimmedNewContent = newContent.trim()
        if (trimmedNewContent.isBlank()) {
            return Result.failure(Exception("Message content cannot be empty."))
        }

        // Delete all messages created after this message
        chatDao.deleteMessagesAfterTimestamp(sessionId, targetMessage.timestamp)

        // Update the target message content
        val updatedMessage = targetMessage.copy(content = trimmedNewContent)
        chatDao.insertMessage(updatedMessage)

        val currentSession = chatDao.getSessionById(sessionId)
        if (currentSession != null) {
            firestoreService.deleteMessagesAfterTimestamp(currentSession.userId, sessionId, targetMessage.timestamp)
            firestoreService.saveMessage(currentSession.userId, updatedMessage)
        }

        // Generate new AI response based on updated conversation history up to this message
        return generateAndSaveAiResponse(
            sessionId = sessionId,
            personality = personality,
            userName = userName,
            projectName = projectName,
            enableSearchGrounding = enableSearchGrounding,
            locationContext = locationContext,
            enableDeepReasoning = enableDeepReasoning
        )
    }

    suspend fun generateAndSaveAiResponse(
        sessionId: String,
        personality: String = "BALANCED",
        userName: String? = null,
        projectName: String? = null,
        enableSearchGrounding: Boolean = false,
        locationContext: String? = null,
        enableDeepReasoning: Boolean = false
    ): Result<ChatMessageEntity> {
        val history = chatDao.getMessagesForSessionList(sessionId)
        if (history.isEmpty()) {
            return Result.failure(Exception("Cannot generate AI response for empty session."))
        }

        val currentSession = chatDao.getSessionById(sessionId)
            ?: return Result.failure(Exception("Session not found."))

        val lastUserMessage = history.lastOrNull { it.sender == "USER" }?.content?.trim() ?: ""
        val lastAssistantMessage = history.dropLast(1).lastOrNull { it.sender == "ASSISTANT" }?.content?.trim() ?: ""

        // Fetch local or remote interview state
        var interview = interviewDao.getInterviewBySessionId(sessionId)
        if (interview == null) {
            interview = firestoreService.fetchInterviewForSession(currentSession.userId, sessionId)
            if (interview != null) {
                interviewDao.insertOrUpdateInterview(interview)
            }
        }

        val lastUserLower = lastUserMessage.lowercase()

        // 1. Check if user wants to PAUSE/STOP an active interview
        if (interview?.status == "IN_PROGRESS" && (
            lastUserLower.contains("stop interview") ||
            lastUserLower.contains("cancel interview") ||
            lastUserLower.contains("pause interview") ||
            lastUserLower == "stop" ||
            lastUserLower == "pause"
        )) {
            val paused = interview.copy(status = "PAUSED", updatedAt = System.currentTimeMillis())
            saveInterview(paused)
            return saveAndReturnAiMessage(sessionId, currentSession.userId,
                "Interview paused. Your answers so far have been saved safely. Say 'resume interview' whenever you would like to continue where you left off."
            )
        }

        // 2. Check if user wants to RESUME a paused interview
        if (interview?.status == "PAUSED" && (
            lastUserLower.contains("resume") ||
            lastUserLower.contains("continue")
        )) {
            val idx = interview.currentQuestionIndex.coerceIn(0, 9)
            val resumed = interview.copy(status = "IN_PROGRESS", currentQuestionIndex = idx, updatedAt = System.currentTimeMillis())
            saveInterview(resumed)
            return saveAndReturnAiMessage(sessionId, currentSession.userId,
                "Resuming your execution plan interview!\n\nQuestion ${idx + 1} of 10:\n${ExecutionPlanInterviewEntity.QUESTIONS[idx]}"
            )
        }

        // 3. Check if user wants to CHANGE or REVISE a previous question answer
        if (interview?.status == "IN_PROGRESS" && (
            lastUserLower.contains("change my previous answer") ||
            lastUserLower.contains("change answer") ||
            lastUserLower.contains("correct my answer") ||
            lastUserLower.contains("change question")
        )) {
            var targetIdx = (interview.currentQuestionIndex - 1).coerceAtLeast(0)
            for (i in 0..9) {
                if (lastUserLower.contains("question ${i+1}") || lastUserLower.contains("q${i+1}")) {
                    targetIdx = i
                    break
                }
            }
            val newAnswerText = lastUserMessage.substringAfter("to ").ifBlank { lastUserMessage }
            val updated = interview.setAnswer(targetIdx, newAnswerText)
            saveInterview(updated)
            return saveAndReturnAiMessage(sessionId, currentSession.userId,
                "Updated Question ${targetIdx + 1} answer.\n\nContinuing interview:\nQuestion ${updated.currentQuestionIndex + 1} of 10:\n${ExecutionPlanInterviewEntity.QUESTIONS[updated.currentQuestionIndex]}"
            )
        }

        // 4. Handle ACTIVE IN_PROGRESS interview step
        if (interview?.status == "IN_PROGRESS") {
            val currentIdx = interview.currentQuestionIndex.coerceIn(0, 9)
            val updatedInterview = interview.setAnswer(currentIdx, lastUserMessage)
            val nextIdx = currentIdx + 1

            if (nextIdx < 10) {
                val nextState = updatedInterview.copy(currentQuestionIndex = nextIdx, updatedAt = System.currentTimeMillis())
                saveInterview(nextState)
                return saveAndReturnAiMessage(sessionId, currentSession.userId,
                    "Got it.\n\nQuestion ${nextIdx + 1} of 10:\n${ExecutionPlanInterviewEntity.QUESTIONS[nextIdx]}"
                )
            } else {
                // All 10 questions answered!
                val completedInterview = updatedInterview.copy(status = "COMPLETED", currentQuestionIndex = 10, updatedAt = System.currentTimeMillis())
                saveInterview(completedInterview)

                val planPrompt = """
                    You are generating the final Personalized Execution Plan for this user.
                    
                    ${completedInterview.formattedSummary()}

                    ${if (!projectName.isNullOrBlank()) "Project Context: $projectName" else ""}

                    Create a realistic, highly personalized, and structured execution plan.
                    You MUST follow this exact 12-section structure:
                    1. EXECUTION OBJECTIVE
                    2. CURRENT SITUATION
                    3. KEY CONSTRAINTS
                    4. PRIORITY STRATEGY
                    5. PHASED EXECUTION PLAN
                    6. ACTIONS
                    7. TIME ALLOCATION
                    8. RESOURCE ALLOCATION
                    9. TEAM RESPONSIBILITIES
                    10. MILESTONES
                    11. RISKS
                    12. NEXT ACTION

                    Ensure the execution plan strictly aligns with the user's stated team size, budget, weekly free time, peak deep focus hours, existing skills, resources, experience level, target timeline, and primary constraint. Do not make unrealistic assumptions or assign tasks to non-existent roles.
                """.trimIndent()

                val planMsg = ChatMessageEntity(
                    messageId = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    sender = "USER",
                    content = planPrompt,
                    timestamp = System.currentTimeMillis()
                )

                val planResult = geminiService.generateResponse(
                    messagesHistory = history + planMsg,
                    personality = personality,
                    userName = userName,
                    projectName = projectName,
                    enableSearchGrounding = enableSearchGrounding,
                    locationContext = locationContext,
                    enableDeepReasoning = true,
                    interviewContext = completedInterview.formattedSummary()
                )

                if (planResult.isFailure) {
                    val ex = planResult.exceptionOrNull() ?: Exception("Failed to generate plan from Gemini.")
                    return saveAndReturnAiMessage(sessionId, currentSession.userId,
                        "Foundry has saved all 10 of your answers. However, an error occurred while generating the plan: ${ex.localizedMessage}\n\nPlease reply 'retry' or type any message to generate your plan again."
                    )
                }

                val planText = planResult.getOrThrow()
                val finalInterview = completedInterview.copy(generatedPlan = planText, updatedAt = System.currentTimeMillis())
                saveInterview(finalInterview)

                val leadInText = "Foundry now has all 10 personalization inputs. Here is your personalized execution plan:\n\n" + planText
                return saveAndReturnAiMessage(sessionId, currentSession.userId, leadInText)
            }
        }

        // 5. Check if user triggers the start of a new interview
        val isExplicitTrigger = lastUserLower.contains("personalized execution plan") ||
                lastUserLower.contains("create execution plan") ||
                lastUserLower.contains("start interview") ||
                lastUserLower.contains("make an execution plan")

        val lastAssistantAskedOffer = lastAssistantMessage.contains("Would you like me to create a personalized execution plan for you?", ignoreCase = true)

        val isAffirmative = lastUserLower == "yes" || lastUserLower == "yes please" || lastUserLower == "sure" ||
                lastUserLower == "yeah" || lastUserLower == "ok" || lastUserLower == "please" ||
                lastUserLower.contains("yes, create") || lastUserLower.contains("i would like that") ||
                lastUserLower.contains("let's do it") || lastUserLower.contains("let's start")

        if (isExplicitTrigger || (lastAssistantAskedOffer && isAffirmative)) {
            val newInterview = ExecutionPlanInterviewEntity(
                sessionId = sessionId,
                userId = currentSession.userId,
                status = "IN_PROGRESS",
                currentQuestionIndex = 0,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            saveInterview(newInterview)
            return saveAndReturnAiMessage(sessionId, currentSession.userId,
                "Great! Let's create your personalized execution plan. I will ask you 10 quick questions, one at a time, to tailor the plan specifically to your resources, team, and time.\n\nQuestion 1 of 10:\n${ExecutionPlanInterviewEntity.QUESTIONS[0]}"
            )
        }

        // 6. Standard Gemini Call (or follow-up turn after completed plan)
        val interviewCtx = if (interview?.status == "COMPLETED") interview.formattedSummary() else null

        val geminiResult = geminiService.generateResponse(
            messagesHistory = history,
            personality = personality,
            userName = userName,
            projectName = projectName,
            enableSearchGrounding = enableSearchGrounding,
            locationContext = locationContext,
            enableDeepReasoning = enableDeepReasoning,
            interviewContext = interviewCtx
        )

        if (geminiResult.isFailure) {
            val ex = geminiResult.exceptionOrNull() ?: Exception("Failed to receive AI response from Gemini.")
            return Result.failure(ex)
        }

        val responseText = geminiResult.getOrThrow()
        return saveAndReturnAiMessage(sessionId, currentSession.userId, responseText)
    }

    private suspend fun saveAndReturnAiMessage(sessionId: String, userId: String, content: String): Result<ChatMessageEntity> {
        val aiMsg = ChatMessageEntity(
            messageId = UUID.randomUUID().toString(),
            sessionId = sessionId,
            sender = "ASSISTANT",
            content = content,
            timestamp = System.currentTimeMillis()
        )
        try {
            chatDao.insertMessage(aiMsg)
            val currentSession = chatDao.getSessionById(sessionId)
            if (currentSession != null) {
                val updated = currentSession.copy(updatedAt = System.currentTimeMillis())
                chatDao.updateSession(updated)
                firestoreService.saveSession(updated)
                firestoreService.saveMessage(userId, aiMsg)
            }
            return Result.success(aiMsg)
        } catch (e: Exception) {
            return Result.failure(Exception("Failed to save AI response: ${e.localizedMessage}"))
        }
    }
}

