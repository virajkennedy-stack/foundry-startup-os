package com.example.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class CustomPersonaRepository private constructor(context: Context) {
    private val db = FoundryDatabase.getDatabase(context)
    private val personaDao = db.customPersonaDao()

    val defaultBrainstormingPresets = listOf(
        CustomPersonaEntity(
            personaId = "PRESET_VISIONARY",
            userId = "",
            name = "Creative Visionary (SCAMPER)",
            tagline = "Wild Ideation & Unconventional Thinking",
            systemPrompt = """
                ACTIVE PERSONA: CREATIVE VISIONARY (SCAMPER METHOD)
                - Goal: Drive high-concept, disruptive, and wildly imaginative brainstorming sessions.
                - Technique: Apply SCAMPER principles (Substitute, Combine, Adapt, Modify, Put to another use, Eliminate, Reverse).
                - Style: Enthusiastic, inventive, lateral thinker. Proactively propose 3 unexpected or unconventional twists to every concept discussed.
            """.trimIndent(),
            iconName = "Lightbulb",
            isDefaultPreset = true
        ),
        CustomPersonaEntity(
            personaId = "PRESET_DEVIL_ADVOCATE",
            userId = "",
            name = "Devil's Advocate",
            tagline = "Stress-Testing & Risk Analysis",
            systemPrompt = """
                ACTIVE PERSONA: DEVIL'S ADVOCATE & RISK ANALYST
                - Goal: Rigorously stress-test ideas, uncover hidden assumptions, and identify potential failure modes.
                - Technique: Highlight economic vulnerabilities, scaling bottlenecks, market saturation risks, and user adoption barriers.
                - Style: Direct, constructive, analytical, uncompromisingly honest. For every idea, point out the biggest vulnerability and ask how to fortify it.
            """.trimIndent(),
            iconName = "Psychology",
            isDefaultPreset = true
        ),
        CustomPersonaEntity(
            personaId = "PRESET_SOCRATIC",
            userId = "",
            name = "Socratic Facilitator",
            tagline = "Deep Probing Questions",
            systemPrompt = """
                ACTIVE PERSONA: SOCRATIC FACILITATOR
                - Goal: Guide the user to discover root insights through structured inquiry rather than spoon-feeding solutions.
                - Technique: Ask 2-3 deep, sequential probing questions in each response that challenge assumptions and force definition of core terms.
                - Style: Reflective, patient, intellectually rigorous, empowering.
            """.trimIndent(),
            iconName = "Quiz",
            isDefaultPreset = true
        ),
        CustomPersonaEntity(
            personaId = "PRESET_DESIGN_SPRINT",
            userId = "",
            name = "Design Sprint Facilitator",
            tagline = "Agile & 6 Thinking Hats",
            systemPrompt = """
                ACTIVE PERSONA: DESIGN SPRINT FACILITATOR (6 THINKING HATS)
                - Goal: Turn abstract ideas into structured user journeys, wireframe concepts, and rapid prototyping plans.
                - Technique: Structure brainstorming using 6 Thinking Hats (Data, Intuition, Caution, Optimism, Creativity, Process).
                - Style: Structured, action-oriented, prototype-driven, encouraging.
            """.trimIndent(),
            iconName = "Group",
            isDefaultPreset = true
        ),
        CustomPersonaEntity(
            personaId = "PRESET_TECH_ARCHITECT",
            userId = "",
            name = "Technical Architect",
            tagline = "System Feasibility & Tech Stacks",
            systemPrompt = """
                ACTIVE PERSONA: TECHNICAL ARCHITECT
                - Goal: Evaluate brainstorming ideas from an engineering, data architecture, and system complexity standpoint.
                - Technique: Outline concrete data models, API flows, cloud infrastructure choices, and scalability tradeoffs.
                - Style: Precise, pragmatic, modular, technically grounded.
            """.trimIndent(),
            iconName = "Build",
            isDefaultPreset = true
        )
    )

    suspend fun ensureDefaultPersonas(userId: String = "") = withContext(Dispatchers.IO) {
        val existing = personaDao.getPersonas(userId)
        if (existing.none { it.isDefaultPreset }) {
            personaDao.insertPersonas(defaultBrainstormingPresets)
        }
    }

    fun getPersonasFlow(userId: String): Flow<List<CustomPersonaEntity>> {
        return personaDao.getPersonasFlow(userId).map { personas ->
            val hasPresets = personas.any { it.isDefaultPreset }
            if (!hasPresets) {
                defaultBrainstormingPresets + personas
            } else {
                personas
            }
        }
    }

    suspend fun getPersonaById(personaId: String): CustomPersonaEntity? = withContext(Dispatchers.IO) {
        val dbPersona = personaDao.getPersonaById(personaId)
        dbPersona ?: defaultBrainstormingPresets.find { it.personaId == personaId }
    }

    suspend fun savePersona(persona: CustomPersonaEntity) = withContext(Dispatchers.IO) {
        personaDao.insertPersona(persona)
    }

    suspend fun deletePersona(personaId: String) = withContext(Dispatchers.IO) {
        personaDao.deletePersona(personaId)
    }

    suspend fun resolveSystemPrompt(personalityKey: String, userId: String): String = withContext(Dispatchers.IO) {
        val keyUpper = personalityKey.uppercase()
        when (keyUpper) {
            "CHILL" -> """
                ACTIVE PERSONALITY: CHILL
                - Tone & Style: Relaxed, friendly, casual, supportive, and easygoing.
                - Keep communication stress-free, conversational, and encouraging, while delivering solid practical value.
            """.trimIndent()
            "CHALLENGER" -> """
                ACTIVE PERSONALITY: CHALLENGER
                - Tone & Style: Direct, analytical, ambitious, high-energy, and challenging.
                - Proactively probe weak assumptions, highlight risks, question contradictions, and challenge the user to aim higher.
            """.trimIndent()
            "BALANCED" -> """
                ACTIVE PERSONALITY: BALANCED
                - Tone & Style: Clear, professional, pragmatic, direct, and well-structured.
                - Deliver practical guidance with a focus on executable steps, objective trade-offs, and balanced feedback.
            """.trimIndent()
            else -> {
                val custom = getPersonaById(personalityKey)
                if (custom != null) {
                    """
                        ACTIVE CUSTOM BRAINSTORMING PERSONA: ${custom.name.uppercase()} (${custom.tagline})
                        ${custom.systemPrompt}
                    """.trimIndent()
                } else if (personalityKey.isNotBlank()) {
                    """
                        ACTIVE CUSTOM BRAINSTORMING SYSTEM PROMPT:
                        $personalityKey
                    """.trimIndent()
                } else {
                    """
                        ACTIVE PERSONALITY: BALANCED
                        - Tone & Style: Clear, professional, pragmatic, direct, and well-structured.
                    """.trimIndent()
                }
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: CustomPersonaRepository? = null

        fun getInstance(context: Context): CustomPersonaRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = CustomPersonaRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
