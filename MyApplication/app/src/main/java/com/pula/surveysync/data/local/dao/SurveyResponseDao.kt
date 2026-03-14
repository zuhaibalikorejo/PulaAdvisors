package com.pula.surveysync.data.local.dao

import androidx.room.*
import com.pula.surveysync.data.local.entity.MediaAttachmentEntity
import com.pula.surveysync.data.local.entity.SurveyAnswerEntity
import com.pula.surveysync.data.local.entity.SurveyResponseEntity
import com.pula.surveysync.data.local.entity.SyncStatus
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface SurveyResponseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(response: SurveyResponseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(responses: List<SurveyResponseEntity>)

    @Update
    suspend fun update(response: SurveyResponseEntity)

    @Delete
    suspend fun delete(response: SurveyResponseEntity)

    @Query("SELECT * FROM survey_responses WHERE id = :id")
    suspend fun getById(id: String): SurveyResponseEntity?

    @Query("SELECT * FROM survey_responses WHERE id = :id")
    fun getByIdFlow(id: String): Flow<SurveyResponseEntity?>

    @Query("SELECT * FROM survey_responses ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<SurveyResponseEntity>>

    @Query("SELECT * FROM survey_responses WHERE syncStatus = :status ORDER BY createdAt ASC")
    suspend fun getByStatus(status: SyncStatus): List<SurveyResponseEntity>

    @Query("SELECT * FROM survey_responses WHERE syncStatus IN (:statuses) ORDER BY createdAt ASC")
    suspend fun getByStatuses(statuses: List<SyncStatus>): List<SurveyResponseEntity>

    @Query("SELECT * FROM survey_responses WHERE syncStatus = :status ORDER BY createdAt ASC")
    fun getByStatusFlow(status: SyncStatus): Flow<List<SurveyResponseEntity>>

    @Query("UPDATE survey_responses SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: SyncStatus)

    @Query("UPDATE survey_responses SET syncStatus = :status, lastSyncAttempt = :lastAttempt, syncAttempts = syncAttempts + 1 WHERE id = :id")
    suspend fun updateSyncAttempt(id: String, status: SyncStatus, lastAttempt: Date)

    @Query("UPDATE survey_responses SET syncStatus = :status, syncError = :error, lastSyncAttempt = :lastAttempt WHERE id = :id")
    suspend fun updateSyncError(id: String, status: SyncStatus, error: String, lastAttempt: Date)

    @Query("SELECT COUNT(*) FROM survey_responses WHERE syncStatus = :status")
    suspend fun countByStatus(status: SyncStatus): Int

    @Query("SELECT COUNT(*) FROM survey_responses WHERE syncStatus = :status")
    fun countByStatusFlow(status: SyncStatus): Flow<Int>

    @Query("SELECT SUM(estimatedSizeBytes) FROM survey_responses")
    suspend fun getTotalStorageUsed(): Long?

    @Query("DELETE FROM survey_responses WHERE syncStatus = :status AND createdAt < :beforeDate")
    suspend fun deleteOldSyncedResponses(status: SyncStatus, beforeDate: Date): Int

    @Transaction
    @Query("SELECT * FROM survey_responses WHERE id = :id")
    suspend fun getResponseWithDetails(id: String): SurveyResponseWithDetails?
}

data class SurveyResponseWithDetails(
    @Embedded val response: SurveyResponseEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "responseId"
    )
    val answers: List<SurveyAnswerEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "responseId"
    )
    val mediaAttachments: List<MediaAttachmentEntity>
)

