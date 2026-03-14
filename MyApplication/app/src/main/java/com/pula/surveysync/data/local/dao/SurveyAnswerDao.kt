package com.pula.surveysync.data.local.dao

import androidx.room.*
import com.pula.surveysync.data.local.entity.SurveyAnswerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SurveyAnswerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(answer: SurveyAnswerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(answers: List<SurveyAnswerEntity>)

    @Update
    suspend fun update(answer: SurveyAnswerEntity)

    @Delete
    suspend fun delete(answer: SurveyAnswerEntity)

    @Query("SELECT * FROM survey_answers WHERE responseId = :responseId ORDER BY questionId, repetitionIndex")
    suspend fun getByResponseId(responseId: String): List<SurveyAnswerEntity>

    @Query("SELECT * FROM survey_answers WHERE responseId = :responseId ORDER BY questionId, repetitionIndex")
    fun getByResponseIdFlow(responseId: String): Flow<List<SurveyAnswerEntity>>

    @Query("SELECT * FROM survey_answers WHERE responseId = :responseId AND sectionId = :sectionId AND repetitionIndex = :repetitionIndex")
    suspend fun getBySectionAndRepetition(
        responseId: String,
        sectionId: String,
        repetitionIndex: Int
    ): List<SurveyAnswerEntity>

    @Query("DELETE FROM survey_answers WHERE responseId = :responseId")
    suspend fun deleteByResponseId(responseId: String)
}

