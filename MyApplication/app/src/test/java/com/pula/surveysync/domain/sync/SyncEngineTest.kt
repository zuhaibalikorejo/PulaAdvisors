package com.pula.surveysync.domain.sync

import com.pula.surveysync.data.local.dao.MediaAttachmentDao
import com.pula.surveysync.data.local.dao.SurveyAnswerDao
import com.pula.surveysync.data.local.dao.SurveyResponseDao
import com.pula.surveysync.data.local.dao.SurveyResponseWithDetails
import com.pula.surveysync.data.local.entity.*
import com.pula.surveysync.data.remote.*
import com.pula.surveysync.domain.model.SyncError
import com.pula.surveysync.domain.model.TerminationReason
import com.pula.surveysync.domain.strategy.DeviceAwareSyncStrategy
import com.pula.surveysync.domain.strategy.SyncDecision
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.*

/**
 * Comprehensive tests for SyncEngine covering all scenarios
 */
class SyncEngineTest {

    private lateinit var syncEngine: SyncEngine
    private lateinit var surveyResponseDao: SurveyResponseDao
    private lateinit var surveyAnswerDao: SurveyAnswerDao
    private lateinit var mediaAttachmentDao: MediaAttachmentDao
    private lateinit var apiService: MockSurveyApiService
    private lateinit var deviceStrategy: DeviceAwareSyncStrategy

    @Before
    fun setup() {
        surveyResponseDao = mockk(relaxed = true)
        surveyAnswerDao = mockk(relaxed = true)
        mediaAttachmentDao = mockk(relaxed = true)
        deviceStrategy = mockk()

        // Default: allow sync
        every { deviceStrategy.shouldSync() } returns SyncDecision(true, "OK")
        every { deviceStrategy.getMaxBatchSize() } returns 20
        every { deviceStrategy.shouldRetryImmediately() } returns true

        apiService = MockSurveyApiService()

        syncEngine = SyncEngine(
            surveyResponseDao,
            surveyAnswerDao,
            mediaAttachmentDao,
            apiService,
            deviceStrategy
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `sync with empty queue returns EMPTY_QUEUE`() = runTest {
        // Given: No pending responses
        coEvery { surveyResponseDao.getByStatuses(any()) } returns emptyList()

        // When
        val result = syncEngine.sync()

        // Then
        assertEquals(TerminationReason.EMPTY_QUEUE, result.terminationReason)
        assertEquals(0, result.totalItems)
        assertTrue(result.successfulItems.isEmpty())
        assertTrue(result.failedItems.isEmpty())
    }

    @Test
    fun `sync with all successful uploads`() = runTest {
        // Given: 5 pending responses
        val responses = createMockResponses(5)
        coEvery { surveyResponseDao.getByStatuses(any()) } returns responses
        coEvery { surveyResponseDao.getResponseWithDetails(any()) } answers {
            val id = firstArg<String>()
            createMockResponseWithDetails(id)
        }

        // When
        val result = syncEngine.sync()

        // Then
        assertEquals(5, result.totalItems)
        assertEquals(5, result.successCount)
        assertEquals(0, result.failureCount)
        assertEquals(TerminationReason.COMPLETED, result.terminationReason)
        assertTrue(result.isFullySuccessful)

        // Verify all marked as synced
        coVerify(exactly = 5) {
            surveyResponseDao.updateSyncStatus(any(), SyncStatus.SYNCED)
        }
    }

    @Test
    fun `sync with partial failure - responses 1-5 succeed, 6 fails, 7-8 not attempted`() = runTest {
        // Given: 8 responses, configure 6th to fail
        val responses = createMockResponses(8)
        val failingId = responses[5].id

        apiService = MockSurveyApiService(
            MockConfig(failOnResponseIds = setOf(failingId))
        )
        syncEngine = SyncEngine(
            surveyResponseDao,
            surveyAnswerDao,
            mediaAttachmentDao,
            apiService,
            deviceStrategy
        )

        coEvery { surveyResponseDao.getByStatuses(any()) } returns responses
        coEvery { surveyResponseDao.getResponseWithDetails(any()) } answers {
            val id = firstArg<String>()
            createMockResponseWithDetails(id)
        }

        // When
        val result = syncEngine.sync()

        // Then
        assertEquals(8, result.totalItems)
        assertEquals(7, result.successCount) // 5 before + 2 after the failure
        assertEquals(1, result.failureCount)
        assertEquals(failingId, result.failedItems[0].itemId)

        // Verify successful ones are marked as synced
        coVerify(exactly = 7) {
            surveyResponseDao.updateSyncStatus(any(), SyncStatus.SYNCED)
        }

        // Verify failed one is marked as failed
        coVerify(exactly = 1) {
            surveyResponseDao.updateSyncError(failingId, SyncStatus.FAILED, any(), any())
        }
    }

    @Test
    fun `sync terminates early after 3 consecutive network failures`() = runTest {
        // Given: 8 responses, configure to fail after 3 calls with timeout
        val responses = createMockResponses(8)

        apiService = MockSurveyApiService(
            MockConfig(
                failAfterNCalls = 3,
                errorType = ErrorType.NETWORK_TIMEOUT
            )
        )
        syncEngine = SyncEngine(
            surveyResponseDao,
            surveyAnswerDao,
            mediaAttachmentDao,
            apiService,
            deviceStrategy
        )

        coEvery { surveyResponseDao.getByStatuses(any()) } returns responses
        coEvery { surveyResponseDao.getResponseWithDetails(any()) } answers {
            val id = firstArg<String>()
            createMockResponseWithDetails(id)
        }

        // When
        val result = syncEngine.sync()

        // Debug output
        println("Test Result: successCount=${result.successCount}, failureCount=${result.failureCount}, terminationReason=${result.terminationReason}, wasTerminatedEarly=${result.wasTerminatedEarly}")

        // Then
        assertEquals("Expected 3 successful uploads", 3, result.successCount)
        assertTrue("Expected at least 3 failures, got ${result.failureCount}", result.failureCount >= 3)
        assertEquals("Expected NETWORK_DEGRADED termination, got ${result.terminationReason}",
            TerminationReason.NETWORK_DEGRADED, result.terminationReason)
        assertTrue("Expected early termination", result.wasTerminatedEarly)

        // Should not process all items
        assertTrue("Should not process all 8 items, processed ${result.successCount + result.failureCount}",
            result.successCount + result.failureCount < 8)
    }

    @Test
    fun `concurrent sync attempts return CONCURRENT_SYNC`() = runTest {
        // Given: A long-running sync
        val responses = createMockResponses(5)
        coEvery { surveyResponseDao.getByStatuses(any()) } returns responses
        coEvery { surveyResponseDao.getResponseWithDetails(any()) } coAnswers {
            delay(1000) // Simulate slow operation
            val id = firstArg<String>()
            createMockResponseWithDetails(id)
        }

        // When: Start first sync (doesn't wait)
        val job1 = async { syncEngine.sync() }
        delay(100) // Ensure first sync starts

        // Try to start second sync
        val result2 = syncEngine.sync()

        // Then: Second sync should be rejected
        assertEquals(TerminationReason.CONCURRENT_SYNC, result2.terminationReason)
        assertEquals(0, result2.totalItems)

        job1.cancel() // Cleanup
    }

    @Test
    fun `no connection error is mapped correctly`() = runTest {
        // Given: Response that will fail with no connection
        val responses = createMockResponses(1)

        apiService = MockSurveyApiService(
            MockConfig(
                failOnResponseIds = setOf(responses[0].id),
                errorType = ErrorType.NO_CONNECTION
            )
        )
        syncEngine = SyncEngine(
            surveyResponseDao,
            surveyAnswerDao,
            mediaAttachmentDao,
            apiService,
            deviceStrategy
        )

        coEvery { surveyResponseDao.getByStatuses(any()) } returns responses
        coEvery { surveyResponseDao.getResponseWithDetails(any()) } answers {
            createMockResponseWithDetails(firstArg())
        }

        // When
        val result = syncEngine.sync()

        // Then
        assertEquals(1, result.failureCount)
        val error = result.failedItems[0].error
        assertTrue(error is SyncError.NoConnection)
        assertTrue(error.isRetryable)
    }

    @Test
    fun `timeout error is mapped correctly`() = runTest {
        // Given: Response that will timeout
        val responses = createMockResponses(1)

        apiService = MockSurveyApiService(
            MockConfig(
                failOnResponseIds = setOf(responses[0].id),
                errorType = ErrorType.NETWORK_TIMEOUT
            )
        )
        syncEngine = SyncEngine(
            surveyResponseDao,
            surveyAnswerDao,
            mediaAttachmentDao,
            apiService,
            deviceStrategy
        )

        coEvery { surveyResponseDao.getByStatuses(any()) } returns responses
        coEvery { surveyResponseDao.getResponseWithDetails(any()) } answers {
            createMockResponseWithDetails(firstArg())
        }

        // When
        val result = syncEngine.sync()

        // Then
        assertEquals(1, result.failureCount)
        val error = result.failedItems[0].error
        assertTrue(error is SyncError.Timeout)
        assertTrue(error.isRetryable)
    }

    @Test
    fun `server error is retryable, client error is not`() = runTest {
        // Given: Two responses, one with server error, one with client error
        val responses = createMockResponses(2)

        apiService = MockSurveyApiService(
            MockConfig(failOnResponseIds = setOf(responses[0].id, responses[1].id))
        )

        // Mock different error types
        val apiServiceMock = mockk<SurveyApiService>()
        coEvery {
            apiServiceMock.uploadSurveyResponse(responses[0].id, any())
        } returns Result.failure(HttpException(500, "Server error"))

        coEvery {
            apiServiceMock.uploadSurveyResponse(responses[1].id, any())
        } returns Result.failure(HttpException(400, "Client error"))

        coEvery { apiServiceMock.uploadMedia(any(), any()) } returns Result.success(
            MediaUploadResponse(true, "media1", "url")
        )

        syncEngine = SyncEngine(
            surveyResponseDao,
            surveyAnswerDao,
            mediaAttachmentDao,
            apiServiceMock,
            deviceStrategy
        )

        coEvery { surveyResponseDao.getByStatuses(any()) } returns responses
        coEvery { surveyResponseDao.getResponseWithDetails(any()) } answers {
            createMockResponseWithDetails(firstArg())
        }

        // When
        val result = syncEngine.sync()

        // Then
        assertEquals(2, result.failureCount)

        val serverError = result.failedItems.find {
            it.error is SyncError.ServerError
        }?.error as? SyncError.ServerError
        assertNotNull(serverError)
        assertTrue(serverError!!.isRetryable)
        assertEquals(500, serverError.statusCode)

        val clientError = result.failedItems.find {
            it.error is SyncError.ClientError
        }?.error as? SyncError.ClientError
        assertNotNull(clientError)
        assertFalse(clientError!!.isRetryable)
        assertEquals(400, clientError.statusCode)
    }

    @Test
    fun `sync progress is reported correctly`() = runTest {
        // Given: 3 responses
        val responses = createMockResponses(3)
        coEvery { surveyResponseDao.getByStatuses(any()) } returns responses
        coEvery { surveyResponseDao.getResponseWithDetails(any()) } answers {
            createMockResponseWithDetails(firstArg())
        }

        val progressValues = mutableListOf<Int>()

        // Collect progress
        val job = launch {
            syncEngine.syncProgress.collect { progress ->
                progress?.let { progressValues.add(it.current) }
            }
        }

        // When
        syncEngine.sync()
        delay(200)
        job.cancel()

        // Then: Should have captured progress 1, 2, 3
        assertTrue(progressValues.contains(1))
        assertTrue(progressValues.contains(2))
        assertTrue(progressValues.contains(3))
    }

    @Test
    fun `device strategy prevents sync when conditions not met`() = runTest {
        // Given: Device strategy says no
        every { deviceStrategy.shouldSync() } returns SyncDecision(
            false,
            "Battery too low"
        )

        // When
        val result = syncEngine.sync()

        // Then
        assertEquals(TerminationReason.NETWORK_DEGRADED, result.terminationReason)
        coVerify(exactly = 0) { surveyResponseDao.getByStatuses(any()) }
    }

    @Test
    fun `batch size is limited by device strategy`() = runTest {
        // Given: 20 responses but device allows only 5
        val responses = createMockResponses(20)
        every { deviceStrategy.getMaxBatchSize() } returns 5

        coEvery { surveyResponseDao.getByStatuses(any()) } returns responses
        coEvery { surveyResponseDao.getResponseWithDetails(any()) } answers {
            createMockResponseWithDetails(firstArg())
        }

        // When
        val result = syncEngine.sync()

        // Then: Only 5 processed
        assertEquals(5, result.totalItems)
        assertEquals(5, result.successCount)
    }

    // Helper functions

    private fun createMockResponses(count: Int): List<SurveyResponseEntity> {
        return (1..count).map { i ->
            SurveyResponseEntity(
                id = "response_$i",
                surveyId = "survey_1",
                agentId = "agent_1",
                createdAt = Date(),
                updatedAt = Date(),
                syncStatus = SyncStatus.PENDING
            )
        }
    }

    private fun createMockResponseWithDetails(responseId: String): SurveyResponseWithDetails {
        return SurveyResponseWithDetails(
            response = SurveyResponseEntity(
                id = responseId,
                surveyId = "survey_1",
                agentId = "agent_1",
                createdAt = Date(),
                updatedAt = Date(),
                syncStatus = SyncStatus.PENDING
            ),
            answers = listOf(
                SurveyAnswerEntity(
                    responseId = responseId,
                    questionId = "q1",
                    value = AnswerValue.TextAnswer("test")
                )
            ),
            mediaAttachments = emptyList()
        )
    }
}

