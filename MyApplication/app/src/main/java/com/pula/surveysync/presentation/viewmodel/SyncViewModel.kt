package com.pula.surveysync.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pula.surveysync.domain.model.SyncProgress
import com.pula.surveysync.domain.model.SyncResult
import com.pula.surveysync.domain.model.TerminationReason
import com.pula.surveysync.domain.usecase.CreateSurveyResponseUseCase
import com.pula.surveysync.domain.usecase.ObservePendingSurveysUseCase
import com.pula.surveysync.domain.usecase.TriggerSyncUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel demonstrating UI integration with sync engine
 */
@HiltViewModel
class SyncViewModel @Inject constructor(
    private val triggerSyncUseCase: TriggerSyncUseCase,
    private val createSurveyResponseUseCase: CreateSurveyResponseUseCase,
    observePendingSurveysUseCase: ObservePendingSurveysUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<SyncUiState>(SyncUiState.Idle)
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    val isSyncing: StateFlow<Boolean> = triggerSyncUseCase.observeSyncStatus()
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val syncProgress: StateFlow<SyncProgress?> = triggerSyncUseCase.observeProgress()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val pendingCount: StateFlow<Int> = observePendingSurveysUseCase()
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    /**
     * Trigger manual sync
     */
    fun startSync() {
        viewModelScope.launch {
            _uiState.value = SyncUiState.Syncing

            val result = triggerSyncUseCase()

            _uiState.value = when {
                result.isFullySuccessful -> {
                    SyncUiState.Success(
                        message = "Successfully synced ${result.successCount} surveys"
                    )
                }
                result.terminationReason == TerminationReason.CONCURRENT_SYNC -> {
                    SyncUiState.Error("Sync already in progress")
                }
                result.terminationReason == TerminationReason.NETWORK_DEGRADED -> {
                    SyncUiState.Error("Network issues detected. Will retry later.")
                }
                result.terminationReason == TerminationReason.EMPTY_QUEUE -> {
                    SyncUiState.Success("No pending surveys to sync")
                }
                result.failureCount > 0 -> {
                    SyncUiState.PartialSuccess(
                        successCount = result.successCount,
                        failureCount = result.failureCount,
                        details = result
                    )
                }
                else -> {
                    SyncUiState.Error("Sync failed")
                }
            }
        }
    }

    /**
     * Create a sample survey response for testing
     */
    fun createSampleSurvey() {
        viewModelScope.launch {
            try {
                val responseId = createSurveyResponseUseCase(
                    surveyId = "sample_survey_${System.currentTimeMillis()}",
                    agentId = "agent_001",
                    answers = mapOf(
                        "farmer_name" to "John Doe",
                        "crop_type" to "Maize",
                        "farm_size" to "50"
                    )
                )
                _uiState.value = SyncUiState.Success("Survey created: $responseId")
            } catch (e: Exception) {
                _uiState.value = SyncUiState.Error("Failed to create survey: ${e.message}")
            }
        }
    }

    /**
     * Reset UI state
     */
    fun resetState() {
        _uiState.value = SyncUiState.Idle
    }
}

/**
 * UI State for sync operations
 */
sealed class SyncUiState {
    object Idle : SyncUiState()
    object Syncing : SyncUiState()
    data class Success(val message: String) : SyncUiState()
    data class Error(val message: String) : SyncUiState()
    data class PartialSuccess(
        val successCount: Int,
        val failureCount: Int,
        val details: SyncResult
    ) : SyncUiState()
}

