package com.example.data

import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class FirestoreService {

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Throwable) {
            null
        }
    }

    suspend fun saveSession(session: ChatSessionEntity) {
        val fs = firestore ?: return
        try {
            val docRef = fs.collection("users")
                .document(session.userId)
                .collection("conversations")
                .document(session.sessionId)

            val data = mapOf(
                "sessionId" to session.sessionId,
                "userId" to session.userId,
                "ownerId" to session.userId,
                "title" to session.title,
                "isPinned" to session.isPinned,
                "createdAt" to session.createdAt,
                "updatedAt" to session.updatedAt
            )

            awaitTask { docRef.set(data) }
        } catch (e: Exception) {
            // Ignore if offline/unconfigured
        }
    }

    suspend fun updateSessionTitle(userId: String, sessionId: String, title: String, updatedAt: Long) {
        val fs = firestore ?: return
        try {
            val docRef = fs.collection("users")
                .document(userId)
                .collection("conversations")
                .document(sessionId)

            val updates = mapOf(
                "title" to title,
                "updatedAt" to updatedAt
            )

            awaitTask { docRef.update(updates) }
        } catch (e: Exception) {
            // Ignore if offline/unconfigured
        }
    }

    suspend fun deleteSession(userId: String, sessionId: String) {
        val fs = firestore ?: return
        try {
            val docRef = fs.collection("users")
                .document(userId)
                .collection("conversations")
                .document(sessionId)

            awaitTask { docRef.delete() }
        } catch (e: Exception) {
            // Ignore if offline/unconfigured
        }
    }

    suspend fun saveMessage(userId: String, message: ChatMessageEntity) {
        val fs = firestore ?: return
        try {
            val docRef = fs.collection("users")
                .document(userId)
                .collection("conversations")
                .document(message.sessionId)
                .collection("messages")
                .document(message.messageId)

            val data = mapOf(
                "messageId" to message.messageId,
                "sessionId" to message.sessionId,
                "ownerId" to userId,
                "sender" to message.sender,
                "content" to message.content,
                "timestamp" to message.timestamp
            )

            awaitTask { docRef.set(data) }

            // Update parent conversation timestamp
            val convRef = fs.collection("users")
                .document(userId)
                .collection("conversations")
                .document(message.sessionId)

            awaitTask { convRef.update("updatedAt", message.timestamp) }
        } catch (e: Exception) {
            // Ignore if offline/unconfigured
        }
    }

    suspend fun deleteMessagesAfterTimestamp(userId: String, sessionId: String, timestamp: Long) {
        val fs = firestore ?: return
        try {
            val snapshot = awaitTask {
                fs.collection("users")
                    .document(userId)
                    .collection("conversations")
                    .document(sessionId)
                    .collection("messages")
                    .whereGreaterThan("timestamp", timestamp)
                    .get()
            }
            for (doc in snapshot.documents) {
                awaitTask { doc.reference.delete() }
            }
        } catch (e: Exception) {
            // Ignore if offline/unconfigured
        }
    }

    suspend fun fetchSessionsForUser(userId: String): List<ChatSessionEntity> {
        val fs = firestore ?: return emptyList()
        return try {
            val snapshot = awaitTask {
                fs.collection("users")
                    .document(userId)
                    .collection("conversations")
                    .orderBy("updatedAt", Query.Direction.DESCENDING)
                    .get()
            }

            snapshot.documents.mapNotNull { doc ->
                val sessionId = doc.getString("sessionId") ?: doc.id
                val title = doc.getString("title") ?: "New Chat"
                val isPinned = doc.getBoolean("isPinned") ?: false
                val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                ChatSessionEntity(
                    sessionId = sessionId,
                    userId = userId,
                    title = title,
                    isPinned = isPinned,
                    createdAt = createdAt,
                    updatedAt = updatedAt
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun fetchMessagesForSession(userId: String, sessionId: String): List<ChatMessageEntity> {
        val fs = firestore ?: return emptyList()
        return try {
            val snapshot = awaitTask {
                fs.collection("users")
                    .document(userId)
                    .collection("conversations")
                    .document(sessionId)
                    .collection("messages")
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .get()
            }

            snapshot.documents.mapNotNull { doc ->
                val messageId = doc.getString("messageId") ?: doc.id
                val sender = doc.getString("sender") ?: "USER"
                val content = doc.getString("content") ?: ""
                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                ChatMessageEntity(
                    messageId = messageId,
                    sessionId = sessionId,
                    sender = sender,
                    content = content,
                    timestamp = timestamp
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveProject(project: ProjectEntity) {
        val fs = firestore ?: return
        try {
            val docRef = fs.collection("users")
                .document(project.ownerId)
                .collection("projects")
                .document(project.projectId)

            val data = mapOf(
                "projectId" to project.projectId,
                "ownerId" to project.ownerId,
                "userId" to project.ownerId,
                "title" to project.title,
                "description" to project.description,
                "originalIdea" to project.originalIdea,
                "problem" to project.problem,
                "goal" to project.goal,
                "status" to project.status,
                "createdAt" to project.createdAt,
                "updatedAt" to project.updatedAt,
                "associatedConversationIds" to project.associatedConversationIds,
                "aiAnalysis" to project.aiAnalysis,
                "decisionsJson" to project.decisionsJson,
                "requirementsJson" to project.requirementsJson,
                "nextActionsJson" to project.nextActionsJson
            )

            awaitTask { docRef.set(data) }
        } catch (e: Exception) {
            // Ignore if offline/unconfigured
        }
    }

    suspend fun deleteProject(ownerId: String, projectId: String) {
        val fs = firestore ?: return
        try {
            val docRef = fs.collection("users")
                .document(ownerId)
                .collection("projects")
                .document(projectId)

            awaitTask { docRef.delete() }
        } catch (e: Exception) {
            // Ignore if offline/unconfigured
        }
    }

    suspend fun fetchProjectsForUser(ownerId: String): List<ProjectEntity> {
        val fs = firestore ?: return emptyList()
        return try {
            val snapshot = awaitTask {
                fs.collection("users")
                    .document(ownerId)
                    .collection("projects")
                    .orderBy("updatedAt", Query.Direction.DESCENDING)
                    .get()
            }

            snapshot.documents.mapNotNull { doc ->
                val projectId = doc.getString("projectId") ?: doc.id
                val title = doc.getString("title") ?: ""
                val description = doc.getString("description") ?: ""
                val originalIdea = doc.getString("originalIdea") ?: ""
                val problem = doc.getString("problem") ?: ""
                val goal = doc.getString("goal") ?: ""
                val status = doc.getString("status") ?: "ACTIVE"
                val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                val associatedConversationIds = doc.getString("associatedConversationIds") ?: ""
                val aiAnalysis = doc.getString("aiAnalysis") ?: ""
                val decisionsJson = doc.getString("decisionsJson") ?: "[]"
                val requirementsJson = doc.getString("requirementsJson") ?: "[]"
                val nextActionsJson = doc.getString("nextActionsJson") ?: "[]"

                ProjectEntity(
                    projectId = projectId,
                    ownerId = ownerId,
                    title = title,
                    description = description,
                    originalIdea = originalIdea,
                    problem = problem,
                    goal = goal,
                    status = status,
                    createdAt = createdAt,
                    updatedAt = updatedAt,
                    associatedConversationIds = associatedConversationIds,
                    aiAnalysis = aiAnalysis,
                    decisionsJson = decisionsJson,
                    requirementsJson = requirementsJson,
                    nextActionsJson = nextActionsJson
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun fetchProjectById(ownerId: String, projectId: String): ProjectEntity? {
        val fs = firestore ?: return null
        return try {
            val doc = awaitTask {
                fs.collection("users")
                    .document(ownerId)
                    .collection("projects")
                    .document(projectId)
                    .get()
            }

            if (!doc.exists()) return null

            val title = doc.getString("title") ?: ""
            val description = doc.getString("description") ?: ""
            val originalIdea = doc.getString("originalIdea") ?: ""
            val problem = doc.getString("problem") ?: ""
            val goal = doc.getString("goal") ?: ""
            val status = doc.getString("status") ?: "ACTIVE"
            val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
            val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
            val associatedConversationIds = doc.getString("associatedConversationIds") ?: ""
            val aiAnalysis = doc.getString("aiAnalysis") ?: ""
            val decisionsJson = doc.getString("decisionsJson") ?: "[]"
            val requirementsJson = doc.getString("requirementsJson") ?: "[]"
            val nextActionsJson = doc.getString("nextActionsJson") ?: "[]"

            ProjectEntity(
                projectId = projectId,
                ownerId = ownerId,
                title = title,
                description = description,
                originalIdea = originalIdea,
                problem = problem,
                goal = goal,
                status = status,
                createdAt = createdAt,
                updatedAt = updatedAt,
                associatedConversationIds = associatedConversationIds,
                aiAnalysis = aiAnalysis,
                decisionsJson = decisionsJson,
                requirementsJson = requirementsJson,
                nextActionsJson = nextActionsJson
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveUserPlan(userId: String, userPlan: UserPlan) {
        val fs = firestore ?: return
        try {
            val userDocRef = fs.collection("users").document(userId)
            val planData = mapOf(
                "userId" to userId,
                "plan" to userPlan.plan.value,
                "subscriptionStatus" to userPlan.subscriptionStatus.value,
                "expiresAt" to userPlan.expiresAt,
                "updatedAt" to userPlan.updatedAt
            )
            awaitTask { userDocRef.set(planData, com.google.firebase.firestore.SetOptions.merge()) }
        } catch (e: Exception) {
            // Ignore if offline or write fails due to security rules
        }
    }

    suspend fun fetchUserPlan(userId: String): UserPlan? {
        val fs = firestore ?: return null
        return try {
            val doc = awaitTask {
                fs.collection("users").document(userId).get()
            }
            if (!doc.exists()) return null

            val planStr = doc.getString("plan")
            val statusStr = doc.getString("subscriptionStatus")
            val expiresAt = doc.getLong("expiresAt")
            val updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()

            UserPlan(
                userId = userId,
                plan = PlanType.fromString(planStr),
                subscriptionStatus = SubscriptionStatus.fromString(statusStr),
                expiresAt = expiresAt,
                updatedAt = updatedAt
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveUsage(userId: String, featureKey: String, count: Int) {
        val fs = firestore ?: return
        try {
            val usageDocRef = fs.collection("users").document(userId).collection("usage").document(featureKey)
            val data = mapOf(
                "userId" to userId,
                "featureKey" to featureKey,
                "count" to count,
                "lastUpdated" to System.currentTimeMillis()
            )
            awaitTask { usageDocRef.set(data, com.google.firebase.firestore.SetOptions.merge()) }
        } catch (e: Exception) {
            // Ignore offline or security failure
        }
    }

    suspend fun incrementUsageAtomic(userId: String, featureKey: String) {
        val fs = firestore ?: return
        try {
            val usageDocRef = fs.collection("users").document(userId).collection("usage").document(featureKey)
            val data = mapOf(
                "userId" to userId,
                "featureKey" to featureKey,
                "count" to com.google.firebase.firestore.FieldValue.increment(1),
                "lastUpdated" to System.currentTimeMillis()
            )
            awaitTask { usageDocRef.set(data, com.google.firebase.firestore.SetOptions.merge()) }
        } catch (e: Exception) {
            // Ignore offline or security failure
        }
    }

    suspend fun fetchUsageForUser(userId: String): Map<String, Int> {
        val fs = firestore ?: return emptyMap()
        return try {
            val snapshot = awaitTask {
                fs.collection("users").document(userId).collection("usage").get()
            }
            val resultMap = mutableMapOf<String, Int>()
            for (doc in snapshot.documents) {
                val featureKey = doc.id
                val count = doc.getLong("count")?.toInt() ?: 0
                resultMap[featureKey] = count
            }
            resultMap
        } catch (e: Exception) {
            emptyMap()
        }
    }

    suspend fun saveInterview(interview: ExecutionPlanInterviewEntity) {
        val fs = firestore ?: return
        try {
            val docRef = fs.collection("users")
                .document(interview.userId)
                .collection("conversations")
                .document(interview.sessionId)
                .collection("interview")
                .document("state")

            val data = mapOf(
                "sessionId" to interview.sessionId,
                "userId" to interview.userId,
                "projectId" to (interview.projectId ?: ""),
                "status" to interview.status,
                "currentQuestionIndex" to interview.currentQuestionIndex,
                "investment" to interview.investment,
                "team" to interview.team,
                "weeklyAvailability" to interview.weeklyAvailability,
                "deepFocusTime" to interview.deepFocusTime,
                "focusCapacity" to interview.focusCapacity,
                "skills" to interview.skills,
                "resources" to interview.resources,
                "experience" to interview.experience,
                "targetTimeline" to interview.targetTimeline,
                "biggestConstraint" to interview.biggestConstraint,
                "generatedPlan" to interview.generatedPlan,
                "createdAt" to interview.createdAt,
                "updatedAt" to interview.updatedAt
            )

            awaitTask { docRef.set(data, com.google.firebase.firestore.SetOptions.merge()) }
        } catch (e: Exception) {
            // Ignore if offline
        }
    }

    suspend fun fetchInterviewForSession(userId: String, sessionId: String): ExecutionPlanInterviewEntity? {
        val fs = firestore ?: return null
        return try {
            val doc = awaitTask {
                fs.collection("users")
                    .document(userId)
                    .collection("conversations")
                    .document(sessionId)
                    .collection("interview")
                    .document("state")
                    .get()
            }
            if (!doc.exists()) return null

            ExecutionPlanInterviewEntity(
                sessionId = doc.getString("sessionId") ?: sessionId,
                userId = doc.getString("userId") ?: userId,
                projectId = doc.getString("projectId"),
                status = doc.getString("status") ?: "NOT_STARTED",
                currentQuestionIndex = doc.getLong("currentQuestionIndex")?.toInt() ?: 0,
                investment = doc.getString("investment") ?: "",
                team = doc.getString("team") ?: "",
                weeklyAvailability = doc.getString("weeklyAvailability") ?: "",
                deepFocusTime = doc.getString("deepFocusTime") ?: "",
                focusCapacity = doc.getString("focusCapacity") ?: "",
                skills = doc.getString("skills") ?: "",
                resources = doc.getString("resources") ?: "",
                experience = doc.getString("experience") ?: "",
                targetTimeline = doc.getString("targetTimeline") ?: "",
                biggestConstraint = doc.getString("biggestConstraint") ?: "",
                generatedPlan = doc.getString("generatedPlan") ?: "",
                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun <T> awaitTask(taskProvider: () -> Task<T>): T =
        suspendCancellableCoroutine { continuation ->
            taskProvider()
                .addOnSuccessListener { result -> continuation.resume(result) }
                .addOnFailureListener { exception -> continuation.resumeWith(Result.failure(exception)) }
        }
}
