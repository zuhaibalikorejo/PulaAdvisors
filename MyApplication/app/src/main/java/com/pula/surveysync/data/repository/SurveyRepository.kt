package com.pula.surveysync.data.repository

import com.pula.surveysync.data.local.dao.MediaAttachmentDao
import com.pula.surveysync.data.local.dao.SurveyAnswerDao
import com.pula.surveysync.data.local.dao.SurveyResponseDao
import com.pula.surveysync.data.local.dao.SurveyResponseWithDetails
import com.pula.surveysync.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing survey responses locally
 */
@Singleton
class SurveyRepository @Inject constructor(
    private val surveyResponseDao: SurveyResponseDao,
    private val surveyAnswerDao: SurveyAnswerDao,
    private val mediaAttachmentDao: MediaAttachmentDao
) {

    /**
     * Creates a new survey response with answers and media
     */
    suspend fun createSurveyResponse(
        surveyId: String,
        agentId: String,
        answers: List<SurveyAnswerEntity>,
        mediaAttachments: List<MediaAttachmentEntity> = emptyList()
    ): String {
        val now = Date()

        // Calculate estimated size
        val mediaSize = mediaAttachments.sumOf { it.fileSizeBytes }
        val dataSize = answers.size * 500L // Rough estimate: 500 bytes per answer
        val estimatedSize = mediaSize + dataSize

        // Create response entity
        val response = SurveyResponseEntity(
            surveyId = surveyId,
            agentId = agentId,
            createdAt = now,
            updatedAt = now,
            syncStatus = SyncStatus.PENDING,
            estimatedSizeBytes = estimatedSize
        )

        // Insert response
        surveyResponseDao.insert(response)

        // Insert answers with correct responseId
        val answersWithResponseId = answers.map { it.copy(responseId = response.id) }
        surveyAnswerDao.insertAll(answersWithResponseId)

        // Insert media attachments with correct responseId
        val mediaWithResponseId = mediaAttachments.map { it.copy(responseId = response.id) }
        mediaAttachmentDao.insertAll(mediaWithResponseId)

        return response.id
    }

    /**
     * Get all survey responses
     */
    fun getAllResponses(): Flow<List<SurveyResponseEntity>> {
        return surveyResponseDao.getAllFlow()
    }

    /**
     * Get responses by sync status
     */
    fun getResponsesByStatus(status: SyncStatus): Flow<List<SurveyResponseEntity>> {
        return surveyResponseDao.getByStatusFlow(status)
    }

    /**
     * Get pending responses count
     */
    fun getPendingCount(): Flow<Int> {
        return surveyResponseDao.countByStatusFlow(SyncStatus.PENDING)
    }

    /**
     * Get response with full details
     */
    suspend fun getResponseWithDetails(responseId: String): SurveyResponseWithDetails? {
        return surveyResponseDao.getResponseWithDetails(responseId)
    }

    /**
     * Delete old synced responses to free up space
     */
    suspend fun cleanupOldSyncedResponses(beforeDate: Date): Int {
        return surveyResponseDao.deleteOldSyncedResponses(SyncStatus.SYNCED, beforeDate)
    }

    /**
     * Get total storage used by responses
     */
    suspend fun getTotalStorageUsed(): Long {
        val responseStorage = surveyResponseDao.getTotalStorageUsed() ?: 0L
        val mediaStorage = mediaAttachmentDao.getTotalMediaStorageUsed() ?: 0L
        return responseStorage + mediaStorage
    }

    /**
     * Check if storage cleanup is needed
     */
    suspend fun shouldCleanupStorage(thresholdMB: Long): Boolean {
        val totalBytes = getTotalStorageUsed()
        val totalMB = totalBytes / (1024 * 1024)
        return totalMB > thresholdMB
    }
}

