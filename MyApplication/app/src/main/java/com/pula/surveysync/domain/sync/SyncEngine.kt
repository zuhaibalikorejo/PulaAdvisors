package com.pula.surveysync.domain.sync

import com.pula.surveysync.data.local.dao.MediaAttachmentDao
import com.pula.surveysync.data.local.dao.SurveyAnswerDao
import com.pula.surveysync.data.local.dao.SurveyResponseDao
import com.pula.surveysync.data.local.entity.SyncStatus
import com.pula.surveysync.data.remote.*
import com.pula.surveysync.domain.model.*
import com.pula.surveysync.domain.strategy.DeviceAwareSyncStrategy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Core sync engine that handles survey response synchronization
 *
 * Key features:
 * - Prevents concurrent sync operations
 * - Tracks sync progress in real-time
 * - Implements device-aware sync strategies
 * - Handles network degradation gracefully
 * - Provides detailed error reporting
 */
@Singleton
class SyncEngine @Inject constructor(
    private val surveyResponseDao: SurveyResponseDao,
    private val surveyAnswerDao: SurveyAnswerDao,
    private val mediaAttachmentDao: MediaAttachmentDao,
    private val apiService: SurveyApiService,
    private val deviceStrategy: DeviceAwareSyncStrategy
) {

    private val syncMutex = Mutex()
    private val _syncProgress = MutableStateFlow<SyncProgress?>(null)
    val syncProgress: Flow<SyncProgress?> = _syncProgress.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: Flow<Boolean> = _isSyncing.asStateFlow()

    companion object {
        private const val MAX_CONSECUTIVE_FAILURES = 3
    }

    /**
     * Initiates a sync operation
     * Returns immediately with CONCURRENT_SYNC if another sync is running
     */
    suspend fun sync(): SyncResult {
        // Try to acquire lock - if fails, another sync is running
        if (!syncMutex.tryLock()) {
            return SyncResult(
                totalItems = 0,
                successfulItems = emptyList(),
                failedItems = emptyList(),
                skippedItems = emptyList(),
                terminationReason = TerminationReason.CONCURRENT_SYNC
            )
        }

        return try {
            _isSyncing.value = true
            performSync()
        } finally {
            _isSyncing.value = false
            _syncProgress.value = null
            syncMutex.unlock()
        }
    }

    private suspend fun performSync(): SyncResult {
        // Check device conditions
        val syncDecision = deviceStrategy.shouldSync()
        if (!syncDecision.shouldProceed) {
            return SyncResult(
                totalItems = 0,
                successfulItems = emptyList(),
                failedItems = emptyList(),
                skippedItems = emptyList(),
                terminationReason = TerminationReason.NETWORK_DEGRADED
            )
        }

        // Get pending responses
        val pendingResponses = surveyResponseDao.getByStatuses(
            listOf(SyncStatus.PENDING, SyncStatus.FAILED)
        )

        if (pendingResponses.isEmpty()) {
            return SyncResult(
                totalItems = 0,
                successfulItems = emptyList(),
                failedItems = emptyList(),
                skippedItems = emptyList(),
                terminationReason = TerminationReason.EMPTY_QUEUE
            )
        }

        val maxBatchSize = deviceStrategy.getMaxBatchSize()
        val itemsToSync = pendingResponses.take(maxBatchSize)

        val successfulItems = mutableListOf<String>()
        val failedItems = mutableListOf<FailedItem>()
        var consecutiveFailures = 0
        var terminationReason = TerminationReason.COMPLETED

        itemsToSync.forEachIndexed { index, response ->
            // Update progress
            _syncProgress.value = SyncProgress(
                current = index + 1,
                total = itemsToSync.size,
                currentItemId = response.id,
                phase = SyncPhase.UPLOADING
            )

            // Update status to SYNCING
            surveyResponseDao.updateSyncStatus(response.id, SyncStatus.SYNCING)

            // Attempt upload
            val result = uploadResponse(response.id)

            result.fold(
                onSuccess = {
                    // Success - mark as synced
                    surveyResponseDao.updateSyncStatus(response.id, SyncStatus.SYNCED)
                    successfulItems.add(response.id)
                    consecutiveFailures = 0
                },
                onFailure = { exception ->
                    // Failure - handle error
                    val syncError = mapExceptionToSyncError(exception)

                    surveyResponseDao.updateSyncError(
                        id = response.id,
                        status = SyncStatus.FAILED,
                        error = syncError.message,
                        lastAttempt = Date()
                    )

                    failedItems.add(FailedItem(response.id, syncError))
                    consecutiveFailures++

                    // Check if we should stop due to network degradation
                    if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES &&
                        isNetworkError(syncError)) {
                        terminationReason = TerminationReason.NETWORK_DEGRADED
                        return@forEachIndexed // Stop processing
                    }
                }
            )

            // Early termination check
            if (terminationReason == TerminationReason.NETWORK_DEGRADED) {
                return@forEachIndexed
            }
        }

        // Calculate skipped items (if terminated early)
        val processedCount = successfulItems.size + failedItems.size
        val skippedItems = itemsToSync.drop(processedCount).map { it.id }

        return SyncResult(
            totalItems = itemsToSync.size,
            successfulItems = successfulItems,
            failedItems = failedItems,
            skippedItems = skippedItems,
            terminationReason = terminationReason
        )
    }

    private suspend fun uploadResponse(responseId: String): Result<UploadResponse> {
        // Get response with details
        val responseWithDetails = surveyResponseDao.getResponseWithDetails(responseId)
            ?: return Result.failure(Exception("Response not found: $responseId"))

        // Upload media first
        val mediaAttachments = responseWithDetails.mediaAttachments
        for (media in mediaAttachments) {
            if (media.syncStatus != SyncStatus.SYNCED) {
                val mediaResult = apiService.uploadMedia(media.id, media.filePath)
                if (mediaResult.isFailure) {
                    return Result.failure(
                        mediaResult.exceptionOrNull()
                            ?: Exception("Failed to upload media ${media.id}")
                    )
                }

                // Mark media as synced
                mediaAttachmentDao.updateSyncStatusWithUrl(
                    id = media.id,
                    status = SyncStatus.SYNCED,
                    uploadUrl = mediaResult.getOrNull()?.uploadUrl ?: ""
                )
            }
        }

        // Prepare upload data
        val uploadData = SurveyUploadData(
            surveyId = responseWithDetails.response.surveyId,
            agentId = responseWithDetails.response.agentId,
            answers = responseWithDetails.answers.map { answer ->
                AnswerData(
                    questionId = answer.questionId,
                    sectionId = answer.sectionId,
                    repetitionIndex = answer.repetitionIndex ?: 0,
                    value = answer.value.toString()
                )
            },
            mediaIds = mediaAttachments.map { it.id },
            timestamp = responseWithDetails.response.createdAt.time
        )

        // Upload response
        return apiService.uploadSurveyResponse(responseId, uploadData)
    }

    private fun mapExceptionToSyncError(exception: Throwable): SyncError {
        return when (exception) {
            is HttpException -> {
                if (exception.statusCode >= 500) {
                    SyncError.ServerError(exception.statusCode, exception.message)
                } else {
                    SyncError.ClientError(exception.statusCode, exception.message)
                }
            }
            is java.net.UnknownHostException -> SyncError.NoConnection()
            is java.net.SocketTimeoutException -> SyncError.Timeout()
            is java.io.IOException -> {
                if (exception.message?.contains("timeout", ignoreCase = true) == true) {
                    SyncError.Timeout()
                } else {
                    SyncError.NoConnection()
                }
            }
            else -> SyncError.Unknown(
                message = exception.message ?: "Unknown error",
                throwable = exception
            )
        }
    }

    private fun isNetworkError(error: SyncError): Boolean {
        return error is SyncError.NoConnection ||
               error is SyncError.Timeout
    }
}

