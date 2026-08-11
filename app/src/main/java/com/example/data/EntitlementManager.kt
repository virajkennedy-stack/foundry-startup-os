package com.example.data

enum class FeatureId(val displayName: String, val description: String) {
    PROJECT_CREATION("Project Management", "Create and maintain projects in Foundry"),
    ADVANCED_AI_ANALYSIS("Advanced AI Analysis", "In-depth project strategy, problem-solving, and requirements synthesis"),
    EXPORT_WORKFLOWS("Export Workflows", "Export project decisions and technical workflows"),
    CUSTOM_PERSONALITY("AI Personality Selection", "Customize AI behavior and feedback style"),
    PRIORITY_GEMINI("Priority AI Processing", "Faster response times and priority server access"),
    FILE_ATTACHMENTS("Project File Attachments", "Attach files and documents to project context")
}

data class FeatureEntitlement(
    val featureId: FeatureId,
    val isAllowed: Boolean,
    val requiredPlan: PlanType,
    val note: String
)

data class FeatureUsageLimit(
    val featureId: FeatureId,
    val isUnlimited: Boolean,
    val maxLimit: Int,
    val currentUsage: Int,
    val remainingUsage: Int
)

object EntitlementManager {

    fun getEntitlement(userPlan: UserPlan, feature: FeatureId): FeatureEntitlement {
        val isPro = userPlan.isProActive

        return when (feature) {
            FeatureId.PROJECT_CREATION -> FeatureEntitlement(
                featureId = feature,
                isAllowed = true, // Both Free and Pro can create projects!
                requiredPlan = PlanType.FREE,
                note = if (isPro) "Unlimited projects included in Pro" else "Up to 3 active projects on Free Tier"
            )

            FeatureId.ADVANCED_AI_ANALYSIS -> FeatureEntitlement(
                featureId = feature,
                isAllowed = isPro,
                requiredPlan = PlanType.PRO,
                note = if (isPro) "Full strategic AI synthesis unlocked" else "Requires Pro Plan for deep strategy breakdown"
            )

            FeatureId.EXPORT_WORKFLOWS -> FeatureEntitlement(
                featureId = feature,
                isAllowed = isPro,
                requiredPlan = PlanType.PRO,
                note = if (isPro) "Export technical decision logs unlocked" else "Requires Pro Plan for document export"
            )

            FeatureId.CUSTOM_PERSONALITY -> FeatureEntitlement(
                featureId = feature,
                isAllowed = true, // Free tier can use standard personalities
                requiredPlan = PlanType.FREE,
                note = "Access standard AI personality modes"
            )

            FeatureId.PRIORITY_GEMINI -> FeatureEntitlement(
                featureId = feature,
                isAllowed = isPro,
                requiredPlan = PlanType.PRO,
                note = if (isPro) "Priority high-speed Gemini pipeline active" else "Standard Gemini processing speed"
            )

            FeatureId.FILE_ATTACHMENTS -> FeatureEntitlement(
                featureId = feature,
                isAllowed = true,
                requiredPlan = PlanType.FREE,
                note = if (isPro) "Unlimited document attachments" else "Up to 5 file attachments per project"
            )
        }
    }

    fun canAccessFeature(userPlan: UserPlan, feature: FeatureId): Boolean {
        return getEntitlement(userPlan, feature).isAllowed
    }

    fun getUsageLimit(userPlan: UserPlan, feature: FeatureId, currentUsage: Int = 0): FeatureUsageLimit {
        val isPro = userPlan.isProActive

        val (isUnlimited, max) = when (feature) {
            FeatureId.PROJECT_CREATION -> if (isPro) Pair(true, Int.MAX_VALUE) else Pair(false, 3)
            FeatureId.FILE_ATTACHMENTS -> if (isPro) Pair(true, Int.MAX_VALUE) else Pair(false, 5)
            FeatureId.ADVANCED_AI_ANALYSIS -> if (isPro) Pair(true, Int.MAX_VALUE) else Pair(false, 0)
            FeatureId.EXPORT_WORKFLOWS -> if (isPro) Pair(true, Int.MAX_VALUE) else Pair(false, 0)
            FeatureId.PRIORITY_GEMINI -> Pair(isPro, if (isPro) Int.MAX_VALUE else 0)
            FeatureId.CUSTOM_PERSONALITY -> Pair(true, Int.MAX_VALUE)
        }

        val remaining = if (isUnlimited) Int.MAX_VALUE else (max - currentUsage).coerceAtLeast(0)

        return FeatureUsageLimit(
            featureId = feature,
            isUnlimited = isUnlimited,
            maxLimit = max,
            currentUsage = currentUsage,
            remainingUsage = remaining
        )
    }
}
