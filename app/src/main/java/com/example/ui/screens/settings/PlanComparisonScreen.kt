package com.example.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.EntitlementManager
import com.example.data.FeatureId
import com.example.data.UserRepository
import com.example.data.UserPlan
import com.example.ui.components.FoundryCard
import com.example.ui.components.FoundryPrimaryButton
import com.example.ui.components.FoundrySecondaryButton
import com.example.ui.components.ProUpgradePromptDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanComparisonScreen(
    userRepository: UserRepository,
    onNavigateBack: () -> Unit
) {
    val userPlan by userRepository.activeUserPlanFlow.collectAsState(initial = null)
    var showCheckoutBoundaryDialog by remember { mutableStateOf(false) }
    var selectedPromptFeature by remember { mutableStateOf<FeatureId?>(null) }

    val isPro = userPlan?.isProActive == true

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Free vs Pro Comparison", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Current Plan Status Banner
            AnimatedVisibility(
                visible = userPlan != null,
                enter = fadeIn() + slideInVertically()
            ) {
                FoundryCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = if (isPro) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    borderColor = if (isPro) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = if (isPro) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .height(36.dp)
                                .width(36.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isPro) "Active Plan: Foundry PRO" else "Active Plan: Free Tier",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isPro) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.padding(start = 4.dp)
                                ) {
                                    Text(
                                        text = if (isPro) "VERIFIED PRO" else "CURRENT PLAN",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPro) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isPro) "You have full access to strategic AI analysis, decision logs, and priority execution."
                                else "Upgrade to unlock unlimited projects, deep AI intelligence, and priority Gemini response speed.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Two-Side Plan Comparison Section (Responsive layout)
            Text(
                text = "Choose Your Plan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            PlanCardsSection(
                userPlan = userPlan,
                isPro = isPro,
                onUpgradeClick = { showCheckoutBoundaryDialog = true }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Comprehensive Capability Breakdown Table
            Text(
                text = "Centralized Feature Entitlements",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            DetailedFeatureComparisonTable(
                userPlan = userPlan,
                onLockedFeatureClick = { feature ->
                    selectedPromptFeature = feature
                }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Contextual upgrade prompt dialog when tapping a locked feature
    if (selectedPromptFeature != null) {
        ProUpgradePromptDialog(
            feature = selectedPromptFeature,
            userPlan = userPlan,
            onDismiss = { selectedPromptFeature = null },
            onNavigateToUpgrade = {
                selectedPromptFeature = null
                showCheckoutBoundaryDialog = true
            }
        )
    }

    // Payment Integration Boundary Notice Dialog
    if (showCheckoutBoundaryDialog) {
        AlertDialog(
            onDismissRequest = { showCheckoutBoundaryDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "Upgrade Flow Boundary",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "The Free vs Pro comparison and secure entitlement architecture is fully verified.\n\nPayment provider checkout (Stripe/Google Pay) will be connected in Phase 4 Step 3.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Start
                )
            },
            confirmButton = {
                TextButton(onClick = { showCheckoutBoundaryDialog = false }) {
                    Text("Understood", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun PlanCardsSection(
    userPlan: UserPlan?,
    isPro: Boolean,
    onUpgradeClick: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth > 600.dp) {
            // Wide Screen: Side-by-Side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    FreePlanCard(isCurrent = !isPro)
                }
                Box(modifier = Modifier.weight(1f)) {
                    ProPlanCard(
                        isCurrent = isPro,
                        onUpgradeClick = onUpgradeClick
                    )
                }
            }
        } else {
            // Compact Screen: Stacked
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FreePlanCard(isCurrent = !isPro)
                ProPlanCard(
                    isCurrent = isPro,
                    onUpgradeClick = onUpgradeClick
                )
            }
        }
    }
}

@Composable
private fun FreePlanCard(isCurrent: Boolean) {
    FoundryCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        borderColor = if (isCurrent) MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FREE",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isCurrent) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "CURRENT PLAN",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Ideal for exploring ideas and launching initial projects",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            FeatureItemCheck("Up to 3 active projects", included = true)
            FeatureItemCheck("Standard Gemini AI model access", included = true)
            FeatureItemCheck("Standard AI personality modes", included = true)
            FeatureItemCheck("Up to 5 file attachments per project", included = true)
            FeatureItemCheck("Advanced strategic AI analysis", included = false)
            FeatureItemCheck("Export decision logs & technical workflows", included = false)

            Spacer(modifier = Modifier.height(20.dp))

            FoundrySecondaryButton(
                text = if (isCurrent) "Current Plan" else "Free Plan Included",
                onClick = { },
                enabled = false
            )
        }
    }
}

@Composable
private fun ProPlanCard(
    isCurrent: Boolean,
    onUpgradeClick: () -> Unit
) {
    FoundryCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
        borderColor = MaterialTheme.colorScheme.primary
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "PRO",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.height(20.dp).width(20.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        text = if (isCurrent) "ACTIVE PRO" else "RECOMMENDED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Unlocks full project intelligence, exports, and priority speed",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            FeatureItemCheck("Unlimited active projects", included = true, isProFeature = true)
            FeatureItemCheck("Priority high-speed Gemini processing", included = true, isProFeature = true)
            FeatureItemCheck("In-depth AI strategic problem-solving", included = true, isProFeature = true)
            FeatureItemCheck("Export decision logs & technical workflows", included = true, isProFeature = true)
            FeatureItemCheck("Unlimited project file attachments", included = true, isProFeature = true)
            FeatureItemCheck("Custom AI personality selection", included = true, isProFeature = true)

            Spacer(modifier = Modifier.height(20.dp))

            if (!isCurrent) {
                FoundryPrimaryButton(
                    text = "Upgrade to Pro",
                    onClick = onUpgradeClick
                )
            } else {
                FoundrySecondaryButton(
                    text = "Active Subscription",
                    onClick = { },
                    enabled = false
                )
            }
        }
    }
}

@Composable
private fun FeatureItemCheck(
    text: String,
    included: Boolean,
    isProFeature: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (included) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            tint = if (included) {
                if (isProFeature) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            },
            modifier = Modifier.height(18.dp).width(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (included) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun DetailedFeatureComparisonTable(
    userPlan: UserPlan?,
    onLockedFeatureClick: (FeatureId) -> Unit
) {
    val features = FeatureId.entries.toList()
    val isProUser = userPlan?.isProActive == true

    FoundryCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Capability",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1.8f)
                )
                Text(
                    text = "Free",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Pro",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }

            features.forEach { feature ->
                val entitlement = userPlan?.let { EntitlementManager.getEntitlement(it, feature) }
                val isAllowedOnUserPlan = entitlement?.isAllowed == true

                val limitFree = EntitlementManager.getUsageLimit(
                    userPlan = UserPlan(userId = "", plan = com.example.data.PlanType.FREE),
                    feature = feature
                )
                val limitPro = EntitlementManager.getUsageLimit(
                    userPlan = UserPlan(userId = "", plan = com.example.data.PlanType.PRO, subscriptionStatus = com.example.data.SubscriptionStatus.ACTIVE_PRO),
                    feature = feature
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (!isProUser && !isAllowedOnUserPlan) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                        .clickable {
                            if (!isProUser && !isAllowedOnUserPlan) {
                                onLockedFeatureClick(feature)
                            }
                        }
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1.8f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = feature.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (!isProUser && !isAllowedOnUserPlan) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked on Free Plan",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.height(14.dp).width(14.dp)
                                    )
                                }
                            }
                            Text(
                                text = feature.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // FREE Column
                        Text(
                            text = if (limitFree.isUnlimited) "Unlimited" else if (limitFree.maxLimit == 0) "—" else "${limitFree.maxLimit}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )

                        // PRO Column
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Included in Pro",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.height(18.dp).width(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (limitPro.isUnlimited) "Unlimited" else "${limitPro.maxLimit}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
