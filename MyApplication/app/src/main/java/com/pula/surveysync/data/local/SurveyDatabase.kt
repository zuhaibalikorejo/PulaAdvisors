package com.pula.surveysync.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.pula.surveysync.data.local.converter.AnswerValueConverter
import com.pula.surveysync.data.local.converter.DateConverter
import com.pula.surveysync.data.local.dao.FarmerDao
import com.pula.surveysync.data.local.dao.MediaAttachmentDao
import com.pula.surveysync.data.local.dao.SurveyAnswerDao
import com.pula.surveysync.data.local.dao.SurveyResponseDao
import com.pula.surveysync.data.local.entity.FarmerEntity
import com.pula.surveysync.data.local.entity.MediaAttachmentEntity
import com.pula.surveysync.data.local.entity.SurveyAnswerEntity
import com.pula.surveysync.data.local.entity.SurveyResponseEntity

@Database(
    entities = [
        FarmerEntity::class,
        SurveyResponseEntity::class,
        SurveyAnswerEntity::class,
        MediaAttachmentEntity::class
    ],
    version = 3, // Incremented for schema changes (added repetitionGroupId)
    exportSchema = true
)
@TypeConverters(DateConverter::class, AnswerValueConverter::class)
abstract class SurveyDatabase : RoomDatabase() {
    abstract fun farmerDao(): FarmerDao
    abstract fun surveyResponseDao(): SurveyResponseDao
    abstract fun surveyAnswerDao(): SurveyAnswerDao
    abstract fun mediaAttachmentDao(): MediaAttachmentDao
}

