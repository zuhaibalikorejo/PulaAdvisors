package com.pula.surveysync.data.local.dao

import androidx.room.*
import com.pula.surveysync.data.local.entity.MediaAttachmentEntity
import com.pula.surveysync.data.local.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaAttachmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attachment: MediaAttachmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(attachments: List<MediaAttachmentEntity>)

    @Update
    suspend fun update(attachment: MediaAttachmentEntity)

    @Delete
    suspend fun delete(attachment: MediaAttachmentEntity)

    @Query("SELECT * FROM media_attachments WHERE id = :id")
    suspend fun getById(id: String): MediaAttachmentEntity?

    @Query("SELECT * FROM media_attachments WHERE responseId = :responseId")
    suspend fun getByResponseId(responseId: String): List<MediaAttachmentEntity>

    @Query("SELECT * FROM media_attachments WHERE responseId = :responseId")
    fun getByResponseIdFlow(responseId: String): Flow<List<MediaAttachmentEntity>>

    @Query("SELECT * FROM media_attachments WHERE syncStatus = :status")
    suspend fun getByStatus(status: SyncStatus): List<MediaAttachmentEntity>

    @Query("UPDATE media_attachments SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: SyncStatus)

    @Query("UPDATE media_attachments SET syncStatus = :status, uploadUrl = :uploadUrl WHERE id = :id")
    suspend fun updateSyncStatusWithUrl(id: String, status: SyncStatus, uploadUrl: String)

    @Query("SELECT SUM(fileSizeBytes) FROM media_attachments WHERE responseId = :responseId")
    suspend fun getTotalSizeForResponse(responseId: String): Long?

    @Query("SELECT SUM(fileSizeBytes) FROM media_attachments")
    suspend fun getTotalMediaStorageUsed(): Long?

    @Query("DELETE FROM media_attachments WHERE responseId = :responseId")
    suspend fun deleteByResponseId(responseId: String)
}

