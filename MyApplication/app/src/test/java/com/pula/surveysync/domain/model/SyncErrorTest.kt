package com.pula.surveysync.domain.model

import org.junit.Assert.*
import org.junit.Test
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class SyncErrorTest {

    @Test
    fun `fromException maps UnknownHostException to NoConnection`() {
        // Given
        val exception = UnknownHostException("No network")

        // When
        val error = SyncError.fromException(exception)

        // Then
        assertTrue(error is SyncError.NoConnection)
        assertTrue(error.isRetryable)
    }

    @Test
    fun `fromException maps SocketTimeoutException to Timeout`() {
        // Given
        val exception = SocketTimeoutException("Connection timeout")

        // When
        val error = SyncError.fromException(exception)

        // Then
        assertTrue(error is SyncError.Timeout)
        assertTrue(error.isRetryable)
    }

    @Test
    fun `fromException maps IOException with timeout message to Timeout`() {
        // Given
        val exception = java.io.IOException("Read timeout")

        // When
        val error = SyncError.fromException(exception)

        // Then
        assertTrue(error is SyncError.Timeout)
        assertTrue(error.isRetryable)
    }

    @Test
    fun `fromException maps unknown exception to Unknown`() {
        // Given
        val exception = RuntimeException("Something went wrong")

        // When
        val error = SyncError.fromException(exception)

        // Then
        assertTrue(error is SyncError.Unknown)
        assertEquals("Something went wrong", error.message)
        assertTrue(error.isRetryable)
    }

    @Test
    fun `ServerError with 5xx is retryable`() {
        // Given
        val error = SyncError.ServerError(500, "Internal server error")

        // Then
        assertTrue(error.isRetryable)
    }

    @Test
    fun `ClientError with 4xx is not retryable`() {
        // Given
        val error = SyncError.ClientError(400, "Bad request")

        // Then
        assertFalse(error.isRetryable)
    }

    @Test
    fun `ValidationError is not retryable`() {
        // Given
        val error = SyncError.ValidationError("Invalid data")

        // Then
        assertFalse(error.isRetryable)
    }
}

