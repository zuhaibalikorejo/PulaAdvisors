package com.pula.surveysync.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.pula.surveysync.data.local.converter.DateConverter
import java.util.Date
import java.util.UUID

@Entity(tableName = "survey_responses")
@TypeConverters(DateConverter::class)
data class SurveyResponseEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val surveyId: String,
    val agentId: String,
    val farmerId: String? = null, // Link to farmer entity
    val createdAt: Date,
    val updatedAt: Date,
    val syncStatus: SyncStatus,
    val syncAttempts: Int = 0,
    val lastSyncAttempt: Date? = null,
    val syncError: String? = null,
    val estimatedSizeBytes: Long = 0 // For storage management
)

enum class SyncStatus {
    PENDING,      // Not yet synced
    SYNCING,      // Currently being synced
    SYNCED,       // Successfully synced
    FAILED        // Failed to sync (will retry)
}

