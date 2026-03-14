package com.pula.surveysync.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pula.surveysync.data.local.entity.AnswerValue
import com.pula.surveysync.data.local.entity.MediaAttachmentEntity
import com.pula.surveysync.data.local.entity.MediaType
import com.pula.surveysync.data.local.entity.SurveyAnswerEntity
import com.pula.surveysync.data.local.entity.SyncStatus
import com.pula.surveysync.data.repository.SurveyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import javax.inject.Inject

sealed class SurveyUiState {
    object Idle : SurveyUiState()
    object Saving : SurveyUiState()
    data class Success(val responseId: String) : SurveyUiState()
    data class Error(val message: String) : SurveyUiState()
    data class ValidationError(val missingFields: List<String>) : SurveyUiState()
}

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val missingFields: List<String>) : ValidationResult()
}

data class Question(
    val id: String,
    val text: String,
    val type: QuestionType,
    val required: Boolean = true,
    val options: List<String> = emptyList(), // For multiple choice
    val isRepeating: Boolean = false // For repeating sections
)

enum class QuestionType {
    TEXT,
    NUMBER,
    DATE,
    SINGLE_CHOICE,
    MULTIPLE_CHOICE,
    PHOTO
}

@HiltViewModel
class SurveyViewModel @Inject constructor(
    private val repository: SurveyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SurveyUiState>(SurveyUiState.Idle)
    val uiState: StateFlow<SurveyUiState> = _uiState.asStateFlow()

    private val _answers = MutableStateFlow<Map<String, AnswerValue>>(emptyMap())
    val answers: StateFlow<Map<String, AnswerValue>> = _answers.asStateFlow()

    private val _photos = MutableStateFlow<List<File>>(emptyList())
    val photos: StateFlow<List<File>> = _photos.asStateFlow()

    private val _numberOfFarms = MutableStateFlow(1)
    val numberOfFarms: StateFlow<Int> = _numberOfFarms.asStateFlow()

    // Sample survey questions
    val questions = listOf(
        Question("q1", "How many farms do you have?", QuestionType.NUMBER),
        Question("q2", "What is the main crop?", QuestionType.SINGLE_CHOICE,
            options = listOf("Maize", "Rice", "Wheat", "Beans", "Coffee", "Tea")),
        Question("q3", "Farm size (hectares)", QuestionType.NUMBER, isRepeating = true),
        Question("q4", "Soil type", QuestionType.SINGLE_CHOICE, isRepeating = true,
            options = listOf("Sandy", "Clay", "Loam", "Rocky")),
        Question("q5", "Irrigation available?", QuestionType.SINGLE_CHOICE, isRepeating = true,
            options = listOf("Yes", "No")),
        Question("q6", "Take a photo of the farm", QuestionType.PHOTO, required = false, isRepeating = true),
        Question("q7", "Additional notes", QuestionType.TEXT, required = false)
    )

    fun updateAnswer(questionId: String, value: AnswerValue, repetitionIndex: Int? = null) {
        val key = if (repetitionIndex != null) {
            "${questionId}_rep_$repetitionIndex"
        } else {
            questionId
        }

        _answers.value = _answers.value + (key to value)

        // If answering number of farms question, update the count
        if (questionId == "q1" && value is AnswerValue.NumberAnswer) {
            _numberOfFarms.value = value.number.toInt()
        }
    }

    fun addPhoto(file: File) {
        _photos.value = _photos.value + file
    }

    fun removePhoto(file: File) {
        _photos.value = _photos.value - file
    }

    fun validateSurvey(): ValidationResult {
        val missingFields = mutableListOf<String>()

        // Check non-repeating required questions
        questions.filter { !it.isRepeating && it.required }.forEach { question ->
            if (!_answers.value.containsKey(question.id)) {
                missingFields.add("${question.text}")
            }
        }

        // Check repeating required questions for each farm
        if (_numberOfFarms.value > 0) {
            for (farmIndex in 0 until _numberOfFarms.value) {
                questions.filter { it.isRepeating && it.required }.forEach { question ->
                    val key = "${question.id}_rep_$farmIndex"
                    if (!_answers.value.containsKey(key)) {
                        missingFields.add("Farm ${farmIndex + 1}: ${question.text}")
                    }
                }
            }
        }

        return if (missingFields.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(missingFields)
        }
    }

    fun submitSurvey(farmerId: String, agentId: String = "agent_001") {
        viewModelScope.launch {
            // Validate before submitting
            val validation = validateSurvey()
            if (validation is ValidationResult.Error) {
                _uiState.value = SurveyUiState.ValidationError(validation.missingFields)
                return@launch
            }

            _uiState.value = SurveyUiState.Saving

            try {
                // Convert answers map to entities
                val answerEntities = mutableListOf<SurveyAnswerEntity>()
                _answers.value.forEach { (key, value) ->
                    // Parse key to get question ID and repetition index
                    val parts = key.split("_rep_")
                    val questionId = parts[0]
                    val repetitionIndex = if (parts.size > 1) parts[1].toIntOrNull() else null
                    val repetitionGroupId = if (repetitionIndex != null) "farm_$repetitionIndex" else null

                    answerEntities.add(
                        SurveyAnswerEntity(
                            responseId = "", // Will be set by repository
                            questionId = questionId,
                            value = value,
                            repetitionIndex = repetitionIndex,
                            repetitionGroupId = repetitionGroupId
                        )
                    )
                }

                // Convert photos to media entities
                val mediaEntities = _photos.value.mapIndexed { index, file ->
                    MediaAttachmentEntity(
                        responseId = "", // Will be set by repository
                        questionId = "q6", // Photo question
                        filePath = file.absolutePath,
                        fileType = MediaType.PHOTO,
                        fileSizeBytes = file.length(),
                        createdAt = Date(),
                        syncStatus = SyncStatus.PENDING
                    )
                }

                // Submit survey
                val responseId = repository.createSurveyResponse(
                    surveyId = "survey_2024",
                    agentId = agentId,
                    answers = answerEntities,
                    mediaAttachments = mediaEntities
                )

                _uiState.value = SurveyUiState.Success(responseId)
            } catch (e: Exception) {
                _uiState.value = SurveyUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun resetSurvey() {
        _answers.value = emptyMap()
        _photos.value = emptyList()
        _numberOfFarms.value = 1
        _uiState.value = SurveyUiState.Idle
    }

    fun clearValidationError() {
        if (_uiState.value is SurveyUiState.ValidationError) {
            _uiState.value = SurveyUiState.Idle
        }
    }
}

