package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.EntitlementManager
import com.example.data.FeatureId
import com.example.data.UserPlan

@Composable
fun ProUpgradePromptDialog(
    feature: FeatureId?,
    userPlan: UserPlan?,
    onDismiss: () -> Unit,
    onNavigateToUpgrade: () -> Unit
) {
    if (feature == null) return

    val entitlement = userPlan?.let { EntitlementManager.getEntitlement(it, feature) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.WorkspacePremium,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "${feature.displayName} is a Pro Feature",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = feature.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = entitlement?.note ?: "Foundry PRO provides full access to advanced AI intelligence, deep strategic analysis, and priority processing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            FoundryPrimaryButton(
                text = "Compare Plans",
                onClick = {
                    onDismiss()
                    onNavigateToUpgrade()
                },
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Maybe Later", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}
