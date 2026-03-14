package com.pula.surveysync.di

import com.pula.surveysync.data.remote.MockSurveyApiService
import com.pula.surveysync.data.remote.SurveyApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideSurveyApiService(): SurveyApiService {
        // Using mock implementation for testing
        // In production, this would be a real Retrofit/Ktor implementation
        return MockSurveyApiService()
    }
}

