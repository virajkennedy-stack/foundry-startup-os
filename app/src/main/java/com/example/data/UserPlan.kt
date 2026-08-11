package com.example.data

enum class PlanType(val value: String, val displayName: String) {
    FREE("FREE", "Free Tier"),
    PRO("PRO", "Pro Plan");

    companion object {
        fun fromString(value: String?): PlanType {
            return entries.find { it.value.equals(value, ignoreCase = true) } ?: FREE
        }
    }
}

enum class SubscriptionStatus(val value: String, val displayName: String) {
    FREE("FREE", "Free Tier"),
    ACTIVE_PRO("ACTIVE_PRO", "Active Pro"),
    INACTIVE_PRO("INACTIVE_PRO", "Inactive Pro"),
    EXPIRED_PRO("EXPIRED_PRO", "Expired Pro");

    companion object {
        fun fromString(value: String?): SubscriptionStatus {
            return entries.find { it.value.equals(value, ignoreCase = true) } ?: FREE
        }
    }
}

data class UserPlan(
    val userId: String,
    val plan: PlanType = PlanType.FREE,
    val subscriptionStatus: SubscriptionStatus = SubscriptionStatus.FREE,
    val expiresAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isProActive: Boolean
        get() = (plan == PlanType.PRO || subscriptionStatus == SubscriptionStatus.ACTIVE_PRO) &&
                (expiresAt == null || expiresAt > System.currentTimeMillis())
}
