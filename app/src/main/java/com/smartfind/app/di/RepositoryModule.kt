package com.smartfind.app.di

import com.smartfind.app.data.repository.DetectionRepository
import com.smartfind.app.data.repository.DetectionRepositoryImpl
import com.smartfind.app.data.repository.ReminderRepository
import com.smartfind.app.data.repository.ReminderRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing repository implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindDetectionRepository(
        detectionRepositoryImpl: DetectionRepositoryImpl
    ): DetectionRepository

    @Binds
    @Singleton
    abstract fun bindReminderRepository(
        reminderRepositoryImpl: ReminderRepositoryImpl
    ): ReminderRepository
}
