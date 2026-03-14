package com.pula.surveysync.di

import com.pula.surveysync.domain.strategy.DefaultDeviceAwareSyncStrategy
import com.pula.surveysync.domain.strategy.DeviceAwareSyncStrategy
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StrategyModule {

    @Binds
    @Singleton
    abstract fun bindDeviceAwareSyncStrategy(
        impl: DefaultDeviceAwareSyncStrategy
    ): DeviceAwareSyncStrategy
}

