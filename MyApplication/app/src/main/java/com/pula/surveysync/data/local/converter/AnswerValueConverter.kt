package com.pula.surveysync.data.local.converter

import androidx.room.TypeConverter
import com.pula.surveysync.data.local.entity.AnswerValue
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AnswerValueConverter {

    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromAnswerValue(value: AnswerValue?): String? {
        return value?.let {
            val serializable = when (it) {
                is AnswerValue.TextAnswer -> SerializableAnswer.Text(it.text)
                is AnswerValue.NumberAnswer -> SerializableAnswer.Number(it.number)
                is AnswerValue.BooleanAnswer -> SerializableAnswer.Boolean(it.boolean)
                is AnswerValue.DateAnswer -> SerializableAnswer.Date(it.date)
                is AnswerValue.MultipleChoiceAnswer -> SerializableAnswer.MultipleChoice(it.choices)
                is AnswerValue.MediaAnswer -> SerializableAnswer.Media(it.mediaIds)
            }
            json.encodeToString(serializable)
        }
    }

    @TypeConverter
    fun toAnswerValue(value: String?): AnswerValue? {
        return value?.let {
            val serializable = json.decodeFromString<SerializableAnswer>(it)
            when (serializable) {
                is SerializableAnswer.Text -> AnswerValue.TextAnswer(serializable.value)
                is SerializableAnswer.Number -> AnswerValue.NumberAnswer(serializable.value)
                is SerializableAnswer.Boolean -> AnswerValue.BooleanAnswer(serializable.value)
                is SerializableAnswer.Date -> AnswerValue.DateAnswer(serializable.value)
                is SerializableAnswer.MultipleChoice -> AnswerValue.MultipleChoiceAnswer(serializable.values)
                is SerializableAnswer.Media -> AnswerValue.MediaAnswer(serializable.mediaIds)
            }
        }
    }
}

@Serializable
private sealed class SerializableAnswer {
    @Serializable
    data class Text(val value: String) : SerializableAnswer()

    @Serializable
    data class Number(val value: Double) : SerializableAnswer()

    @Serializable
    data class Boolean(val value: kotlin.Boolean) : SerializableAnswer()

    @Serializable
    data class Date(val value: Long) : SerializableAnswer()

    @Serializable
    data class MultipleChoice(val values: List<String>) : SerializableAnswer()

    @Serializable
    data class Media(val mediaIds: List<String>) : SerializableAnswer()
}

