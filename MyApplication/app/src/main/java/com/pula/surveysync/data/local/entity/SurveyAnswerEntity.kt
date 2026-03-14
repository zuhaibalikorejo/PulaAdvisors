package com.pula.surveysync.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.pula.surveysync.data.local.converter.AnswerValueConverter
import java.util.UUID

/**
 * Represents a single answer to a question in a survey response.
 * Supports repeating sections through repetitionIndex.
 */
@Entity(
    tableName = "survey_answers",
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
@TypeConverters(AnswerValueConverter::class)
data class SurveyAnswerEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val responseId: String,
    val questionId: String,
    val sectionId: String? = null,
    val repetitionIndex: Int? = null, // For repeating sections (e.g., farm 1, farm 2, farm 3) - null for non-repeating
    val repetitionGroupId: String? = null, // e.g., "farm_1", "farm_2" for grouping related repeating answers
    val value: AnswerValue
)

/**
 * Sealed class to handle different answer types
 */
sealed class AnswerValue {
    data class TextAnswer(val text: String) : AnswerValue()
    data class NumberAnswer(val number: Double) : AnswerValue()
    data class BooleanAnswer(val boolean: Boolean) : AnswerValue()
    data class DateAnswer(val date: Long) : AnswerValue()
    data class MultipleChoiceAnswer(val choices: List<String>) : AnswerValue() // Used for both single and multiple choice
    data class MediaAnswer(val mediaIds: List<String>) : AnswerValue() // References to MediaAttachmentEntity
}

