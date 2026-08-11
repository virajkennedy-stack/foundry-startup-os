package com.example.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import com.example.data.UserRepository
import com.example.ui.components.FoundryCard
import com.example.ui.components.FoundryPrimaryButton
import com.example.ui.components.FoundrySymbol
import com.example.ui.components.FoundryTextField
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    userRepository: UserRepository,
    onOnboardingComplete: () -> Unit
) {
    val activeUser by userRepository.activeUserFlow.collectAsState(initial = null)
    var step by remember { mutableIntStateOf(1) }

    var displayName by remember { mutableStateOf(activeUser?.displayName ?: "") }
    var selectedPersonality by remember { mutableStateOf(activeUser?.personalityPreference ?: "BALANCED") }
    var selectedAppearance by remember { mutableStateOf(activeUser?.appearancePreference ?: "DARK") }

    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FoundrySymbol(size = 36.dp)
                Text(
                    text = "Step $step of 3",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            AnimatedContent(
                targetState = step,
                modifier = Modifier.weight(1f),
                label = "OnboardingSteps"
            ) { currentStep ->
                when (currentStep) {
                    1 -> StepDisplayName(
                        name = displayName,
                        onNameChange = { displayName = it }
                    )
                    2 -> StepPersonality(
                        selected = selectedPersonality,
                        onSelect = { selectedPersonality = it }
                    )
                    3 -> StepAppearance(
                        selected = selectedAppearance,
                        onSelect = { selectedAppearance = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            FoundryPrimaryButton(
                text = if (step < 3) "Continue" else "Finish Setup",
                onClick = {
                    if (step < 3) {
                        step++
                    } else {
                        coroutineScope.launch {
                            userRepository.completeOnboarding(
                                displayName = displayName,
                                personality = selectedPersonality,
                                appearance = selectedAppearance
                            )
                            onOnboardingComplete()
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StepDisplayName(
    name: String,
    onNameChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "What should we call you?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Your display name will be visible across your Foundry workspaces.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        FoundryTextField(
            value = name,
            onValueChange = onNameChange,
            label = "Your Name / Handle",
            placeholder = "e.g. Alex Vance"
        )
    }
}

@Composable
private fun StepPersonality(
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Choose Assistant Vibe",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Select how Foundry's AI co-pilot communicates with you.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        PersonalityOptionCard(
            title = "Relaxed & Supporting",
            desc = "Gentle prompts, low pressure, encouraging updates.",
            key = "CHILL",
            isSelected = selected == "CHILL",
            onClick = { onSelect("CHILL") }
        )

        Spacer(modifier = Modifier.height(12.dp))

        PersonalityOptionCard(
            title = "Balanced & Direct",
            desc = "Clear, concise insights, timely deadlines, structured action items.",
            key = "BALANCED",
            isSelected = selected == "BALANCED",
            onClick = { onSelect("BALANCED") }
        )

        Spacer(modifier = Modifier.height(12.dp))

        PersonalityOptionCard(
            title = "High-Intensity Challenger",
            desc = "Strict sprint goals, aggressive efficiency tips, direct feedback.",
            key = "CHALLENGER",
            isSelected = selected == "CHALLENGER",
            onClick = { onSelect("CHALLENGER") }
        )
    }
}

@Composable
private fun PersonalityOptionCard(
    title: String,
    desc: String,
    key: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant

    FoundryCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        containerColor = containerColor,
        borderColor = borderColor
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StepAppearance(
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Preferred Theme",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = "Select your default appearance mode.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppearanceOption(
                label = "Dark Mode",
                key = "DARK",
                isSelected = selected == "DARK",
                onSelect = { onSelect("DARK") },
                modifier = Modifier.weight(1f)
            )

            AppearanceOption(
                label = "Light Mode",
                key = "LIGHT",
                isSelected = selected == "LIGHT",
                onSelect = { onSelect("LIGHT") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AppearanceOption(
    label: String,
    key: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onSelect() }
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
