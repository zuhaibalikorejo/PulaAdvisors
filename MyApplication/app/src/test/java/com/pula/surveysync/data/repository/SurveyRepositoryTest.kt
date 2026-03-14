package com.pula.surveysync.data.repository

import com.pula.surveysync.data.local.dao.MediaAttachmentDao
import com.pula.surveysync.data.local.dao.SurveyAnswerDao
import com.pula.surveysync.data.local.dao.SurveyResponseDao
import com.pula.surveysync.data.local.entity.*
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Date

class SurveyRepositoryTest {

    private lateinit var repository: SurveyRepository
    private lateinit var surveyResponseDao: SurveyResponseDao
    private lateinit var surveyAnswerDao: SurveyAnswerDao
    private lateinit var mediaAttachmentDao: MediaAttachmentDao

    @Before
    fun setup() {
        surveyResponseDao = mockk(relaxed = true)
        surveyAnswerDao = mockk(relaxed = true)
        mediaAttachmentDao = mockk(relaxed = true)

        repository = SurveyRepository(
            surveyResponseDao,
            surveyAnswerDao,
            mediaAttachmentDao
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `createSurveyResponse saves response with answers and media`() = runTest {
        // Given
        val answers = listOf(
            SurveyAnswerEntity(
                responseId = "",
                questionId = "q1",
                value = AnswerValue.TextAnswer("answer1")
            )
        )
        val media = listOf(
            MediaAttachmentEntity(
                responseId = "",
                questionId = "q1",
                filePath = "/path/to/photo.jpg",
                fileType = MediaType.PHOTO,
                fileSizeBytes = 1024000,
                createdAt = Date(),
                syncStatus = SyncStatus.PENDING
            )
        )

        coEvery { surveyResponseDao.insert(any()) } returns 1L

        // When
        val responseId = repository.createSurveyResponse(
            surveyId = "survey1",
            agentId = "agent1",
            answers = answers,
            mediaAttachments = media
        )

        // Then
        assertNotNull(responseId)
        coVerify { surveyResponseDao.insert(any()) }
        coVerify { surveyAnswerDao.insertAll(any()) }
        coVerify { mediaAttachmentDao.insertAll(any()) }
    }

    @Test
    fun `getAllResponses returns flow of responses`() = runTest {
        // Given
        val responses = listOf(
            SurveyResponseEntity(
                id = "r1",
                surveyId = "s1",
                agentId = "a1",
                createdAt = Date(),
                updatedAt = Date(),
                syncStatus = SyncStatus.PENDING
            )
        )
        every { surveyResponseDao.getAllFlow() } returns flowOf(responses)

        // When
        val result = repository.getAllResponses().first()

        // Then
        assertEquals(1, result.size)
        assertEquals("r1", result[0].id)
    }

    @Test
    fun `getPendingCount returns correct count`() = runTest {
        // Given
        every { surveyResponseDao.countByStatusFlow(SyncStatus.PENDING) } returns flowOf(5)

        // When
        val count = repository.getPendingCount().first()

        // Then
        assertEquals(5, count)
    }

    @Test
    fun `getTotalStorageUsed sums response and media storage`() = runTest {
        // Given
        coEvery { surveyResponseDao.getTotalStorageUsed() } returns 1000000L
        coEvery { mediaAttachmentDao.getTotalMediaStorageUsed() } returns 5000000L

        // When
        val total = repository.getTotalStorageUsed()

        // Then
        assertEquals(6000000L, total)
    }

    @Test
    fun `shouldCleanupStorage returns true when threshold exceeded`() = runTest {
        // Given: 100 MB used
        coEvery { surveyResponseDao.getTotalStorageUsed() } returns 50L * 1024 * 1024
        coEvery { mediaAttachmentDao.getTotalMediaStorageUsed() } returns 50L * 1024 * 1024

        // When: Threshold is 50 MB
        val shouldCleanup = repository.shouldCleanupStorage(50L)

        // Then
        assertTrue(shouldCleanup)
    }

    @Test
    fun `cleanupOldSyncedResponses deletes old responses`() = runTest {
        // Given
        coEvery {
            surveyResponseDao.deleteOldSyncedResponses(any(), any())
        } returns 10

        // When
        val deletedCount = repository.cleanupOldSyncedResponses(Date())

        // Then
        assertEquals(10, deletedCount)
        coVerify { surveyResponseDao.deleteOldSyncedResponses(SyncStatus.SYNCED, any()) }
    }
}

