package com.example.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.platform.testTag
import com.example.data.CustomPersonaEntity
import com.example.data.CustomPersonaRepository
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.EntitlementManager
import com.example.data.FeatureId
import com.example.data.SubscriptionManager
import com.example.data.UsageRepository
import com.example.data.UserPlan
import com.example.data.UserRepository
import com.example.ui.components.FoundryCard
import com.example.ui.components.FoundryPrimaryButton
import com.example.ui.components.FoundrySecondaryButton
import com.example.ui.components.FoundryTextField
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userRepository: UserRepository,
    onNavigateBack: () -> Unit,
    onNavigateToPlanComparison: () -> Unit = {},
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val usageRepository = remember { UsageRepository.getInstance(context) }
    val subscriptionManager = remember { SubscriptionManager(userRepository) }

    val activeUser by userRepository.activeUserFlow.collectAsState(initial = null)
    val activeUserPlan by userRepository.activeUserPlanFlow.collectAsState(
        initial = UserPlan(userId = "")
    )

    val userId = activeUser?.userId ?: ""
    val usageMap by usageRepository.getUsageMap(userId).collectAsState(initial = emptyMap())

    var isEditingName by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf(activeUser?.displayName ?: "") }
    var showSubscriptionDialog by remember { mutableStateOf(false) }
    var subDialogMessage by remember { mutableStateOf("") }

    LaunchedEffect(userId) {
        if (userId.isNotBlank()) {
            usageRepository.syncUsageFromRemote(userId)
        }
    }

    if (showSubscriptionDialog) {
        AlertDialog(
            onDismissRequest = { showSubscriptionDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "Subscription Management Boundary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Current Plan: ${activeUserPlan.plan.displayName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Status: ${activeUserPlan.subscriptionStatus.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = subDialogMessage.ifBlank {
                            "Billing provider management is active. When live payment providers (Stripe or Google Play) are connected, this boundary opens the native billing portal to manage billing methods, auto-renewal, or cancellations."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            confirmButton = {
                FoundryPrimaryButton(
                    text = "Close",
                    onClick = { showSubscriptionDialog = false },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Account", fontWeight = FontWeight.Bold) },
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
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 1. ACCOUNT SECTION
            Text(
                text = "Account Profile",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            FoundryCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Email",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = activeUser?.email.orEmpty().ifBlank { "Not signed in" },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isEditingName) {
                        FoundryTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = "Display Name"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            FoundrySecondaryButton(
                                text = "Save",
                                onClick = {
                                    coroutineScope.launch {
                                        userRepository.updateDisplayName(nameInput)
                                        isEditingName = false
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    nameInput = activeUser?.displayName ?: ""
                                    isEditingName = true
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Display Name",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = activeUser?.displayName.orEmpty().ifBlank { "Tap to set name" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "Edit",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. SUBSCRIPTION & PLAN SECTION
            Text(
                text = "Subscription & Plan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            FoundryCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (activeUserPlan.isProActive) {
                            coroutineScope.launch {
                                val result = subscriptionManager.openBillingPortal(context)
                                subDialogMessage = result.getOrDefault("")
                                showSubscriptionDialog = true
                            }
                        } else {
                            onNavigateToPlanComparison()
                        }
                    }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = if (activeUserPlan.isProActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = activeUserPlan.plan.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "• ${activeUserPlan.subscriptionStatus.displayName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (activeUserPlan.isProActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = if (activeUserPlan.isProActive) "Verified Pro Entitlements Active" else "Tap to view plan comparison & unlock Pro capabilities",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = if (activeUserPlan.isProActive) "Manage" else "Upgrade",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. CENTRALIZED USAGE METERING SECTION
            Text(
                text = "Resource Usage & Limits",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Project Creation Usage
                val projectUsage = usageMap[FeatureId.PROJECT_CREATION] ?: 0
                UsageMetricCard(
                    featureId = FeatureId.PROJECT_CREATION,
                    icon = Icons.Default.Folder,
                    userPlan = activeUserPlan,
                    currentUsage = projectUsage,
                    onUpgradeClick = onNavigateToPlanComparison
                )

                // File Attachments Usage
                val fileUsage = usageMap[FeatureId.FILE_ATTACHMENTS] ?: 0
                UsageMetricCard(
                    featureId = FeatureId.FILE_ATTACHMENTS,
                    icon = Icons.Default.UploadFile,
                    userPlan = activeUserPlan,
                    currentUsage = fileUsage,
                    onUpgradeClick = onNavigateToPlanComparison
                )

                // Advanced AI Analysis Usage
                val aiUsage = usageMap[FeatureId.ADVANCED_AI_ANALYSIS] ?: 0
                UsageMetricCard(
                    featureId = FeatureId.ADVANCED_AI_ANALYSIS,
                    icon = Icons.Default.Analytics,
                    userPlan = activeUserPlan,
                    currentUsage = aiUsage,
                    onUpgradeClick = onNavigateToPlanComparison
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. PREFERENCES
            Text(
                text = "Preferences",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            val customPersonaRepo = remember { CustomPersonaRepository.getInstance(context) }
            val customPersonas by customPersonaRepo.getPersonasFlow(userId).collectAsState(initial = emptyList())

            LaunchedEffect(userId) {
                if (userId.isNotBlank()) {
                    usageRepository.syncUsageFromRemote(userId)
                }
                customPersonaRepo.ensureDefaultPersonas(userId)
            }

            AiPersonalitySection(
                selectedPersonality = activeUser?.personalityPreference ?: "BALANCED",
                customPersonas = customPersonas,
                userId = userId,
                onSelectPersonality = { newPersonality ->
                    coroutineScope.launch {
                        userRepository.updatePersonality(newPersonality)
                    }
                },
                onSavePersona = { persona ->
                    coroutineScope.launch {
                        customPersonaRepo.savePersona(persona)
                    }
                },
                onDeletePersona = { personaId ->
                    coroutineScope.launch {
                        customPersonaRepo.deletePersona(personaId)
                        if (activeUser?.personalityPreference == personaId) {
                            userRepository.updatePersonality("BALANCED")
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            PreferenceTile(
                icon = Icons.Default.Palette,
                title = "Appearance Theme",
                value = activeUser?.appearancePreference ?: "DARK",
                onClick = {
                    val next = if (activeUser?.appearancePreference == "DARK") "LIGHT" else "DARK"
                    coroutineScope.launch { userRepository.updateAppearance(next) }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 5. DEVELOPER SUPPORT & REPORT
            Text(
                text = "Developer Support & Reports",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            FoundryCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val userEmail = activeUser?.email ?: "Unknown"
                        val userName = activeUser?.displayName ?: "User"
                        val regDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                            .format(Date(activeUser?.createdAt ?: System.currentTimeMillis()))

                        val subject = "[Foundry User Report] $userEmail"
                        val body = """
                            Hi Admin,
                            
                            Here is the user report details for:
                            - User Email: $userEmail
                            - Display Name: $userName
                            - Registered On: $regDate
                            - AI Personality: ${activeUser?.personalityPreference}
                            
                            Report / Contact Message:
                            (Type your report or message here)
                        """.trimIndent()

                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:virajkennedy@gmail.com")
                            putExtra(Intent.EXTRA_SUBJECT, subject)
                            putExtra(Intent.EXTRA_TEXT, body)
                        }
                        try {
                            context.startActivity(Intent.createChooser(intent, "Send Email Report"))
                        } catch (e: Exception) {
                            // fallback
                        }
                    }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Send Report / Contact Admin",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "virajkennedy@gmail.com",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // LOGOUT BUTTON
            FoundryCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        coroutineScope.launch {
                            userRepository.authSessionManager.logout()
                            onLogout()
                        }
                    },
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Sign Out",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun UsageMetricCard(
    featureId: FeatureId,
    icon: ImageVector,
    userPlan: UserPlan,
    currentUsage: Int,
    onUpgradeClick: () -> Unit
) {
    val limitInfo = EntitlementManager.getUsageLimit(userPlan, featureId, currentUsage)
    val isLimitReached = !limitInfo.isUnlimited && limitInfo.remainingUsage <= 0

    FoundryCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isLimitReached) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = featureId.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = featureId.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (limitInfo.isUnlimited) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Unlimited",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                } else if (isLimitReached) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Limit Reached",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!limitInfo.isUnlimited) {
                val progress = (currentUsage.toFloat() / limitInfo.maxLimit.toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (isLimitReached) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Usage: $currentUsage / ${limitInfo.maxLimit}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${limitInfo.remainingUsage} remaining",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isLimitReached && !userPlan.isProActive) {
                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(
                        onClick = onUpgradeClick,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = "Upgrade to Pro for Unlimited",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Usage: $currentUsage performed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "No plan limit applies",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun PreferenceTile(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    FoundryCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AiPersonalitySection(
    selectedPersonality: String,
    customPersonas: List<CustomPersonaEntity>,
    userId: String,
    onSelectPersonality: (String) -> Unit,
    onSavePersona: (CustomPersonaEntity) -> Unit,
    onDeletePersona: (String) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var personaToEdit by remember { mutableStateOf<CustomPersonaEntity?>(null) }
    var filterTab by remember { mutableStateOf("ALL") } // ALL, STANDARD, PRESETS, CUSTOM
    var expandedPromptPersonaId by remember { mutableStateOf<String?>(null) }

    val standardOptions = remember {
        listOf(
            CustomPersonaEntity(
                personaId = "CHILL",
                userId = "",
                name = "CHILL",
                tagline = "Relaxed & Friendly",
                systemPrompt = "Relaxed, friendly, casual, supportive, and easygoing. Keep communication stress-free, conversational, and encouraging, while delivering solid practical value.",
                isDefaultPreset = true
            ),
            CustomPersonaEntity(
                personaId = "BALANCED",
                userId = "",
                name = "BALANCED",
                tagline = "Clear & Pragmatic",
                systemPrompt = "Clear, professional, pragmatic, direct, and well-structured guidance with a focus on executable steps, objective trade-offs, and balanced feedback.",
                isDefaultPreset = true
            ),
            CustomPersonaEntity(
                personaId = "CHALLENGER",
                userId = "",
                name = "CHALLENGER",
                tagline = "Direct & High-Energy",
                systemPrompt = "Direct, analytical, ambitious, high-energy, and challenging. Proactively probe weak assumptions, highlight risks, question contradictions, and challenge the user to aim higher.",
                isDefaultPreset = true
            )
        )
    }

    val presetBrainstorming = customPersonas.filter { it.isDefaultPreset && !standardOptions.any { std -> std.personaId == it.personaId } }
    val userCustomPersonas = customPersonas.filter { !it.isDefaultPreset }

    val currentActivePersonaName = remember(selectedPersonality, customPersonas) {
        val stdMatch = standardOptions.find { it.personaId.equals(selectedPersonality, ignoreCase = true) }
        if (stdMatch != null) return@remember stdMatch.name
        val customMatch = customPersonas.find { it.personaId == selectedPersonality }
        customMatch?.name ?: selectedPersonality
    }

    if (showCreateDialog || personaToEdit != null) {
        CreateOrEditPersonaDialog(
            userId = userId,
            existingPersona = personaToEdit,
            onDismiss = {
                showCreateDialog = false
                personaToEdit = null
            },
            onSave = { persona, makeActive ->
                onSavePersona(persona)
                if (makeActive) {
                    onSelectPersonality(persona.personaId)
                }
                showCreateDialog = false
                personaToEdit = null
            }
        )
    }

    FoundryCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "AI Personas & System Prompts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Active: $currentActivePersonaName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Button(
                    onClick = {
                        personaToEdit = null
                        showCreateDialog = true
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("add_custom_persona_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "New Persona",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterTab == "ALL",
                    onClick = { filterTab = "ALL" },
                    label = { Text("All (${standardOptions.size + presetBrainstorming.size + userCustomPersonas.size})") }
                )
                FilterChip(
                    selected = filterTab == "STANDARD",
                    onClick = { filterTab = "STANDARD" },
                    label = { Text("Standard Tones") }
                )
                FilterChip(
                    selected = filterTab == "PRESETS",
                    onClick = { filterTab = "PRESETS" },
                    label = { Text("Brainstorming Presets (${presetBrainstorming.size})") }
                )
                FilterChip(
                    selected = filterTab == "CUSTOM",
                    onClick = { filterTab = "CUSTOM" },
                    label = { Text("Custom (${userCustomPersonas.size})") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Display List of Personas based on Filter
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (filterTab == "ALL" || filterTab == "STANDARD") {
                    if (filterTab == "ALL") {
                        Text(
                            text = "Standard Tones",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    standardOptions.forEach { std ->
                        PersonaItemCard(
                            persona = std,
                            isSelected = selectedPersonality.equals(std.personaId, ignoreCase = true),
                            isExpanded = expandedPromptPersonaId == std.personaId,
                            onSelect = { onSelectPersonality(std.personaId) },
                            onToggleExpand = {
                                expandedPromptPersonaId = if (expandedPromptPersonaId == std.personaId) null else std.personaId
                            }
                        )
                    }
                }

                if (filterTab == "ALL" || filterTab == "PRESETS") {
                    if (presetBrainstorming.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Brainstorming Presets",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        presetBrainstorming.forEach { preset ->
                            PersonaItemCard(
                                persona = preset,
                                isSelected = selectedPersonality == preset.personaId,
                                isExpanded = expandedPromptPersonaId == preset.personaId,
                                onSelect = { onSelectPersonality(preset.personaId) },
                                onToggleExpand = {
                                    expandedPromptPersonaId = if (expandedPromptPersonaId == preset.personaId) null else preset.personaId
                                }
                            )
                        }
                    }
                }

                if (filterTab == "ALL" || filterTab == "CUSTOM") {
                    if (userCustomPersonas.isNotEmpty() || filterTab == "CUSTOM") {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Your Custom AI Personas",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (userCustomPersonas.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "No custom AI Personas defined yet",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Create custom system prompts for your specific brainstorming methods",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            personaToEdit = null
                                            showCreateDialog = true
                                        }
                                    ) {
                                        Text("Create Custom Persona")
                                    }
                                }
                            }
                        } else {
                            userCustomPersonas.forEach { custom ->
                                PersonaItemCard(
                                    persona = custom,
                                    isSelected = selectedPersonality == custom.personaId,
                                    isExpanded = expandedPromptPersonaId == custom.personaId,
                                    onSelect = { onSelectPersonality(custom.personaId) },
                                    onToggleExpand = {
                                        expandedPromptPersonaId = if (expandedPromptPersonaId == custom.personaId) null else custom.personaId
                                    },
                                    onEdit = {
                                        personaToEdit = custom
                                    },
                                    onDelete = {
                                        onDeletePersona(custom.personaId)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonaItemCard(
    persona: CustomPersonaEntity,
    isSelected: Boolean,
    isExpanded: Boolean,
    onSelect: () -> Unit,
    onToggleExpand: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val cardBorder = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val cardBg = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surface

    val iconVector = when (persona.iconName) {
        "Lightbulb" -> Icons.Default.Lightbulb
        "Quiz" -> Icons.Default.Quiz
        "Group" -> Icons.Default.Group
        "Build" -> Icons.Default.Build
        "AutoAwesome" -> Icons.Default.AutoAwesome
        else -> Icons.Default.Psychology
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = cardBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .background(cardBg)
            .clickable { onSelect() }
            .padding(12.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.Top) {
                RadioButton(
                    selected = isSelected,
                    onClick = onSelect,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = persona.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            if (!persona.isDefaultPreset) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Custom",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Action icons
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (onEdit != null) {
                                IconButton(
                                    onClick = onEdit,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .testTag("edit_persona_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit persona",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            if (onDelete != null) {
                                IconButton(
                                    onClick = onDelete,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .testTag("delete_persona_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete persona",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = persona.tagline,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .clickable { onToggleExpand() }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isExpanded) "Hide System Prompt" else "Show System Prompt",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = persona.systemPrompt.trim(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateOrEditPersonaDialog(
    userId: String,
    existingPersona: CustomPersonaEntity?,
    onDismiss: () -> Unit,
    onSave: (CustomPersonaEntity, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(existingPersona?.name ?: "") }
    var tagline by remember { mutableStateOf(existingPersona?.tagline ?: "") }
    var systemPrompt by remember { mutableStateOf(existingPersona?.systemPrompt ?: "") }
    var iconName by remember { mutableStateOf(existingPersona?.iconName ?: "Psychology") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val templates = remember {
        listOf(
            Triple(
                "SCAMPER Ideator",
                "Lateral & Unconventional Thinking",
                "Act as a SCAMPER brainstorming facilitator. Use Substitute, Combine, Adapt, Modify, Put to another use, Eliminate, and Reverse techniques to generate 5 innovative concepts for any topic discussed."
            ),
            Triple(
                "Socratic Coach",
                "Root Analysis via Probing Questions",
                "Act as a Socratic thought partner. Instead of giving answers directly, ask 3 precise, sequential questions that force me to clarify assumptions and uncover non-obvious insights."
            ),
            Triple(
                "Devil's Advocate",
                "Stress-Testing & Failure Modes",
                "Act as a Devil's Advocate and risk analyst. Critique ideas ruthlessly, point out economic/scaling flaws, and challenge every assumption before recommending mitigations."
            ),
            Triple(
                "UX Design Thinker",
                "User Journey & Rapid Mockups",
                "Act as a Senior Product Designer. Structure brainstorming into user pain points, key user flows, screen concepts, and rapid paper-prototype wireframe specifications."
            ),
            Triple(
                "Growth Hacker",
                "Distribution & Viral Loops",
                "Act as a Startup Growth Hacker. Focus brainstorming on user acquisition channels, viral loops, monetization mechanics, retention triggers, and rapid experiment setups."
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (existingPersona == null) "Define Custom AI Persona" else "Edit AI Persona",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Specify custom system prompt instructions that will govern how Foundry AI behaves during your brainstorming sessions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Persona Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMessage = null },
                    label = { Text("Persona Name *") },
                    placeholder = { Text("e.g., Socratic Startup Pitch Coach") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("persona_name_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                // Persona Tagline
                OutlinedTextField(
                    value = tagline,
                    onValueChange = { tagline = it },
                    label = { Text("Tagline / Category") },
                    placeholder = { Text("e.g., Pitching & Strategic Planning") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("persona_tagline_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                // Template inspiration chips
                Text(
                    text = "Quick Template Starters:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    templates.forEach { (tName, tTagline, tPrompt) ->
                        AssistChip(
                            onClick = {
                                name = tName
                                tagline = tTagline
                                systemPrompt = tPrompt
                            },
                            label = { Text(tName, style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                            )
                        )
                    }
                }

                // System Prompt Input
                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = { systemPrompt = it; errorMessage = null },
                    label = { Text("Custom System Prompt / AI Instructions *") },
                    placeholder = { Text("Act as an expert... Use SCAMPER technique... Ask probing questions...") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("persona_prompt_input"),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            errorMessage = "Persona name cannot be empty."
                            return@Button
                        }
                        if (systemPrompt.isBlank()) {
                            errorMessage = "System prompt cannot be empty."
                            return@Button
                        }

                        val entity = (existingPersona ?: CustomPersonaEntity(
                            userId = userId,
                            name = name.trim(),
                            tagline = tagline.trim().ifBlank { "Custom Persona" },
                            systemPrompt = systemPrompt.trim(),
                            iconName = iconName,
                            isDefaultPreset = false
                        )).copy(
                            name = name.trim(),
                            tagline = tagline.trim().ifBlank { "Custom Persona" },
                            systemPrompt = systemPrompt.trim(),
                            iconName = iconName
                        )

                        onSave(entity, true)
                    },
                    modifier = Modifier.testTag("save_and_activate_persona_button")
                ) {
                    Text("Save & Activate")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

