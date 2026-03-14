package com.pula.surveysync.data.remote

import com.pula.surveysync.domain.model.SyncError

/**
 * Mock API service that simulates server responses
 * Can be configured to fail on specific items or after N successful calls
 */
interface SurveyApiService {
    suspend fun uploadSurveyResponse(
        responseId: String,
        data: SurveyUploadData
    ): Result<UploadResponse>

    suspend fun uploadMedia(
        mediaId: String,
        filePath: String
    ): Result<MediaUploadResponse>
}

data class SurveyUploadData(
    val surveyId: String,
    val agentId: String,
    val answers: List<AnswerData>,
    val mediaIds: List<String>,
    val timestamp: Long
)

data class AnswerData(
    val questionId: String,
    val sectionId: String?,
    val repetitionIndex: Int,
    val value: String
)

data class UploadResponse(
    val success: Boolean,
    val responseId: String,
    val serverId: String
)

data class MediaUploadResponse(
    val success: Boolean,
    val mediaId: String,
    val uploadUrl: String
)

/**
 * Mock implementation of the API service for testing
 */
class MockSurveyApiService(
    private val config: MockConfig = MockConfig()
) : SurveyApiService {

    private var callCount = 0
    private val failedIds = config.failOnResponseIds.toMutableSet()

    override suspend fun uploadSurveyResponse(
        responseId: String,
        data: SurveyUploadData
    ): Result<UploadResponse> {
        // Simulate network delay
        kotlinx.coroutines.delay(config.networkDelayMs)

        callCount++

        // Check if this specific ID should fail
        if (responseId in failedIds) {
            return Result.failure(
                createException(config.errorType, "Failed to upload response $responseId")
            )
        }

        // Check if we should fail after N calls
        if (config.failAfterNCalls > 0 && callCount > config.failAfterNCalls) {
            return Result.failure(
                createException(config.errorType, "Server error after $callCount calls")
            )
        }

        return Result.success(
            UploadResponse(
                success = true,
                responseId = responseId,
                serverId = "server_${responseId}"
            )
        )
    }

    override suspend fun uploadMedia(
        mediaId: String,
        filePath: String
    ): Result<MediaUploadResponse> {
        kotlinx.coroutines.delay(config.networkDelayMs / 2)

        if (mediaId in failedIds) {
            return Result.failure(
                createException(config.errorType, "Failed to upload media $mediaId")
            )
        }

        return Result.success(
            MediaUploadResponse(
                success = true,
                mediaId = mediaId,
                uploadUrl = "https://example.com/media/$mediaId"
            )
        )
    }

    private fun createException(errorType: ErrorType, message: String): Exception {
        return when (errorType) {
            ErrorType.NETWORK_TIMEOUT -> java.net.SocketTimeoutException(message)
            ErrorType.NO_CONNECTION -> java.net.UnknownHostException(message)
            ErrorType.SERVER_ERROR -> HttpException(500, message)
            ErrorType.CLIENT_ERROR -> HttpException(400, message)
        }
    }

    fun reset() {
        callCount = 0
    }
}

data class MockConfig(
    val networkDelayMs: Long = 100,
    val failAfterNCalls: Int = 0,
    val failOnResponseIds: Set<String> = emptySet(),
    val errorType: ErrorType = ErrorType.SERVER_ERROR
)

enum class ErrorType {
    NETWORK_TIMEOUT,
    NO_CONNECTION,
    SERVER_ERROR,
    CLIENT_ERROR
}

class HttpException(val statusCode: Int, override val message: String) : Exception(message)

