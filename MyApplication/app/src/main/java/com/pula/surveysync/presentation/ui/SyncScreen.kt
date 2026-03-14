package com.pula.surveysync.presentation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pula.surveysync.domain.model.SyncPhase
import com.pula.surveysync.presentation.viewmodel.SyncUiState
import com.pula.surveysync.presentation.viewmodel.SyncViewModel

/**
 * Sample UI demonstrating sync engine integration
 * Shows real-time progress and sync status
 */
@Composable
fun SyncScreen(
    modifier: Modifier = Modifier,
    onNewSurvey: () -> Unit = {},
    viewModel: SyncViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncProgress by viewModel.syncProgress.collectAsState()
    val pendingCount by viewModel.pendingCount.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Survey Sync Engine Demo",
            style = MaterialTheme.typography.headlineMedium
        )

        // Pending surveys badge
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$pendingCount",
                    style = MaterialTheme.typography.displayMedium
                )
                Text(
                    text = "Pending Surveys",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Sync progress
        syncProgress?.let { progress ->
            if (isSyncing) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = progress.displayText,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        LinearProgressIndicator(
                            progress = { progress.percentage / 100f },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = when (progress.phase) {
                                SyncPhase.PREPARING -> "Preparing upload..."
                                SyncPhase.UPLOADING -> "Uploading surveys..."
                                SyncPhase.FINALIZING -> "Finalizing..."
                                SyncPhase.COMPLETED -> "Complete!"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Status messages
        when (val state = uiState) {
            is SyncUiState.Idle -> {}
            is SyncUiState.Syncing -> {
                CircularProgressIndicator()
            }
            is SyncUiState.Success -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Text(
                        text = "✓ ${state.message}",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            is SyncUiState.Error -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "✗ ${state.message}",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            is SyncUiState.PartialSuccess -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "⚠ Partial Success",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "✓ ${state.successCount} succeeded",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "✗ ${state.failureCount} failed",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Action buttons
        Button(
            onClick = onNewSurvey,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("+ New Survey")
        }

        Button(
            onClick = { viewModel.startSync() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSyncing
        ) {
            Text(if (isSyncing) "Syncing..." else "Start Manual Sync")
        }

        OutlinedButton(
            onClick = { viewModel.createSampleSurvey() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSyncing
        ) {
            Text("Create Sample Survey (Testing)")
        }

        if (uiState !is SyncUiState.Idle && uiState !is SyncUiState.Syncing) {
            TextButton(
                onClick = { viewModel.resetState() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clear Status")
            }
        }
    }
}

