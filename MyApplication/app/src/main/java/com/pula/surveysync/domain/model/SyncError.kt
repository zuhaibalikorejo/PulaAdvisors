package com.pula.surveysync.domain.model

/**
 * Consistent error model for sync operations
 */
sealed class SyncError(open val message: String, open val isRetryable: Boolean) {

    /**
     * No internet connection available
     */
    data class NoConnection(
        override val message: String = "No internet connection"
    ) : SyncError(message, isRetryable = true)

    /**
     * Network timeout occurred
     */
    data class Timeout(
        override val message: String = "Connection timeout"
    ) : SyncError(message, isRetryable = true)

    /**
     * Server returned an error
     */
    data class ServerError(
        val statusCode: Int,
        override val message: String
    ) : SyncError(message, isRetryable = statusCode >= 500) // 5xx errors are retryable

    /**
     * Client error (4xx) - usually not retryable
     */
    data class ClientError(
        val statusCode: Int,
        override val message: String
    ) : SyncError(message, isRetryable = false)

    /**
     * Data validation error
     */
    data class ValidationError(
        override val message: String
    ) : SyncError(message, isRetryable = false)

    /**
     * Unknown/unexpected error
     */
    data class Unknown(
        override val message: String,
        val throwable: Throwable? = null
    ) : SyncError(message, isRetryable = true)

    companion object {
        fun fromException(exception: Exception): SyncError {
            return when {
                exception is java.net.UnknownHostException -> NoConnection()
                exception is java.net.SocketTimeoutException -> Timeout()
                exception is java.io.IOException &&
                    exception.message?.contains("timeout", ignoreCase = true) == true -> Timeout()
                else -> Unknown(
                    message = exception.message ?: "Unknown error occurred",
                    throwable = exception
                )
            }
        }
    }
}

