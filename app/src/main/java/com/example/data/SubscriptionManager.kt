package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class SubscriptionDetails(
    val userId: String,
    val planType: PlanType,
    val status: SubscriptionStatus,
    val providerName: String = "Foundry Billing Integration Boundary",
    val autoRenews: Boolean = true,
    val expiresAt: Long? = null,
    val isManageable: Boolean = true
)

class SubscriptionManager(
    private val userRepository: UserRepository
) {
    fun getSubscriptionDetails(userId: String): Flow<SubscriptionDetails> {
        return userRepository.activeUserPlanFlow.map { userPlan ->
            SubscriptionDetails(
                userId = userId,
                planType = userPlan.plan,
                status = userPlan.subscriptionStatus,
                providerName = if (userPlan.isProActive) "Stripe / Google Play Billing Boundary" else "Free Plan",
                autoRenews = userPlan.isProActive,
                expiresAt = userPlan.expiresAt,
                isManageable = userPlan.isProActive
            )
        }
    }

    suspend fun openBillingPortal(context: Context): Result<String> {
        // Integration Boundary for Phase 4:
        // Returns a message indicating subscription management readiness for connected payment providers.
        return Result.success("Billing Management Boundary active. When a payment provider (such as Stripe or Google Play Billing) is connected, this opens the native billing portal.")
    }

    suspend fun cancelSubscription(userId: String): Result<Unit> {
        // Securely sets subscription status to INACTIVE_PRO or cancels auto-renewal
        return userRepository.setPlan(userId, PlanType.PRO, SubscriptionStatus.INACTIVE_PRO)
    }

    suspend fun resumeSubscription(userId: String): Result<Unit> {
        // Securely resumes active PRO subscription
        return userRepository.setPlan(userId, PlanType.PRO, SubscriptionStatus.ACTIVE_PRO)
    }
}
