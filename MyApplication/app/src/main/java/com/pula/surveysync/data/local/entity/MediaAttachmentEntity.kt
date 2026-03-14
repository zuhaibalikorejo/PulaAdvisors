package com.pula.surveysync.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.pula.surveysync.data.local.converter.DateConverter
import java.util.Date
import java.util.UUID

/**
 * Represents a media attachment (photo, video, etc.) associated with a survey response.
 * Tracks sync status independently from parent response for partial upload handling.
 */
@Entity(
    tableName = "media_attachments",
    foreignKeys = [
        ForeignKey(
            entity = SurveyResponseEntity::class,
            parentColumns = ["id"],
            childColumns = ["responseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["responseId"])]
)
@TypeConverters(DateConverter::class)
data class MediaAttachmentEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val responseId: String,
    val questionId: String,
    val filePath: String, // Local file path
    val fileType: MediaType,
    val fileSizeBytes: Long,
    val createdAt: Date,
    val syncStatus: SyncStatus,
    val uploadUrl: String? = null // Server URL after successful upload
)

enum class MediaType {
    PHOTO,
    VIDEO,
    AUDIO,
    DOCUMENT
}

