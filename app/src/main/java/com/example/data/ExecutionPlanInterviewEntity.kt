package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "execution_plan_interviews")
data class ExecutionPlanInterviewEntity(
    @PrimaryKey
    val sessionId: String,
    val userId: String,
    val projectId: String? = null,
    val status: String = "NOT_STARTED", // "NOT_STARTED", "IN_PROGRESS", "COMPLETED"
    val currentQuestionIndex: Int = 0, // 0 to 9
    val investment: String = "",
    val team: String = "",
    val weeklyAvailability: String = "",
    val deepFocusTime: String = "",
    val focusCapacity: String = "",
    val skills: String = "",
    val resources: String = "",
    val experience: String = "",
    val targetTimeline: String = "",
    val biggestConstraint: String = "",
    val generatedPlan: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getAnswer(index: Int): String {
        return when (index) {
            0 -> investment
            1 -> team
            2 -> weeklyAvailability
            3 -> deepFocusTime
            4 -> focusCapacity
            5 -> skills
            6 -> resources
            7 -> experience
            8 -> targetTimeline
            9 -> biggestConstraint
            else -> ""
        }
    }

    fun setAnswer(index: Int, answer: String): ExecutionPlanInterviewEntity {
        val now = System.currentTimeMillis()
        return when (index) {
            0 -> copy(investment = answer, updatedAt = now)
            1 -> copy(team = answer, updatedAt = now)
            2 -> copy(weeklyAvailability = answer, updatedAt = now)
            3 -> copy(deepFocusTime = answer, updatedAt = now)
            4 -> copy(focusCapacity = answer, updatedAt = now)
            5 -> copy(skills = answer, updatedAt = now)
            6 -> copy(resources = answer, updatedAt = now)
            7 -> copy(experience = answer, updatedAt = now)
            8 -> copy(targetTimeline = answer, updatedAt = now)
            9 -> copy(biggestConstraint = answer, updatedAt = now)
            else -> this
        }
    }

    fun formattedSummary(): String {
        return """
            USER PERSONALIZATION CONTEXT (FROM 10-QUESTION INTERVIEW):
            - 1. Initial Investment: ${investment.ifBlank { "Unspecified" }}
            - 2. Team Size & Roles: ${team.ifBlank { "Unspecified" }}
            - 3. Weekly Free Time: ${weeklyAvailability.ifBlank { "Unspecified" }}
            - 4. Peak Deep-Focus Time: ${deepFocusTime.ifBlank { "Unspecified" }}
            - 5. Focus Period Hours: ${focusCapacity.ifBlank { "Unspecified" }}
            - 6. Existing Skills: ${skills.ifBlank { "Unspecified" }}
            - 7. Accessible Resources: ${resources.ifBlank { "Unspecified" }}
            - 8. Industry Experience: ${experience.ifBlank { "Unspecified" }}
            - 9. Target Timeframe & Milestone: ${targetTimeline.ifBlank { "Unspecified" }}
            - 10. Biggest Constraint/Obstacle: ${biggestConstraint.ifBlank { "Unspecified" }}
        """.trimIndent()
    }

    companion object {
        val QUESTIONS = listOf(
            "How much are you realistically willing to invest in this business initially?",
            "How many people are currently on your team, and what roles or skills do they have?",
            "How much free time can you realistically dedicate to this business each week?",
            "When during the day do you usually have your best deep-focus time?",
            "During your best focus period, how many hours can you realistically focus on meaningful work?",
            "What skills do you already have that could help you build or grow this business?",
            "What resources do you already have access to that could help you build this business?",
            "How much experience do you currently have in this industry or with this type of business?",
            "What is your realistic target timeframe for getting this business to your first meaningful milestone?",
            "What is the biggest constraint or obstacle you think could prevent you from making this business work right now?"
        )
    }
}
