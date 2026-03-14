package com.pula.surveysync.domain.usecase

import com.pula.surveysync.data.local.entity.AnswerValue
import com.pula.surveysync.data.local.entity.MediaAttachmentEntity
import com.pula.surveysync.data.local.entity.MediaType
import com.pula.surveysync.data.local.entity.SurveyAnswerEntity
import com.pula.surveysync.data.local.entity.SyncStatus
import com.pula.surveysync.data.repository.SurveyRepository
import com.pula.surveysync.domain.model.SyncProgress
import com.pula.surveysync.domain.model.SyncResult
import com.pula.surveysync.domain.sync.SyncEngine
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.util.Date
import javax.inject.Inject

/**
 * Use case for triggering manual sync from UI layer
 */
class TriggerSyncUseCase @Inject constructor(
    private val syncEngine: SyncEngine
) {
    /**
     * Trigger a sync operation
     * @return SyncResult with detailed information about what succeeded/failed
     */
    suspend operator fun invoke(): SyncResult {
        return syncEngine.sync()
    }

    /**
     * Observe sync progress for UI updates
     */
    fun observeProgress(): Flow<SyncProgress?> {
        return syncEngine.syncProgress
    }

    /**
     * Check if sync is currently running
     */
    fun observeSyncStatus(): Flow<Boolean> {
        return syncEngine.isSyncing
    }
}

/**
 * Use case for creating survey responses
 */
class CreateSurveyResponseUseCase @Inject constructor(
    private val repository: SurveyRepository
) {
    suspend operator fun invoke(
        surveyId: String,
        agentId: String,
        answers: Map<String, String>,
        mediaFiles: List<String> = emptyList()
    ): String {
        // Convert to entities and save
        val answerEntities = answers.map { (questionId, value) ->
            SurveyAnswerEntity(
                responseId = "",
                questionId = questionId,
                value = AnswerValue.TextAnswer(value)
            )
        }

        val mediaEntities = mediaFiles.map { filePath ->
            MediaAttachmentEntity(
                responseId = "",
                questionId = "media",
                filePath = filePath,
                fileType = MediaType.PHOTO,
                fileSizeBytes = File(filePath).length(),
                createdAt = Date(),
                syncStatus = SyncStatus.PENDING
            )
        }

        return repository.createSurveyResponse(
            surveyId = surveyId,
            agentId = agentId,
            answers = answerEntities,
            mediaAttachments = mediaEntities
        )
    }
}

/**
 * Use case for observing pending survey count
 */
class ObservePendingSurveysUseCase @Inject constructor(
    private val repository: SurveyRepository
) {
    operator fun invoke(): Flow<Int> {
        return repository.getPendingCount()
    }
}

