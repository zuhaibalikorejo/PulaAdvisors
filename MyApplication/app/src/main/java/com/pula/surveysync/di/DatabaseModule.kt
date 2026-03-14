package com.pula.surveysync.di

import android.content.Context
import androidx.room.Room
import com.pula.surveysync.data.local.SurveyDatabase
import com.pula.surveysync.data.local.dao.FarmerDao
import com.pula.surveysync.data.local.dao.MediaAttachmentDao
import com.pula.surveysync.data.local.dao.SurveyAnswerDao
import com.pula.surveysync.data.local.dao.SurveyResponseDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideSurveyDatabase(
        @ApplicationContext context: Context
    ): SurveyDatabase {
        return Room.databaseBuilder(
            context,
            SurveyDatabase::class.java,
            "survey_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideFarmerDao(database: SurveyDatabase): FarmerDao {
        return database.farmerDao()
    }

    @Provides
    fun provideSurveyResponseDao(database: SurveyDatabase): SurveyResponseDao {
        return database.surveyResponseDao()
    }

    @Provides
    fun provideSurveyAnswerDao(database: SurveyDatabase): SurveyAnswerDao {
        return database.surveyAnswerDao()
    }

    @Provides
    fun provideMediaAttachmentDao(database: SurveyDatabase): MediaAttachmentDao {
        return database.mediaAttachmentDao()
    }
}

