package com.smartfind.app.di

import android.content.Context
import androidx.room.Room
import com.smartfind.app.data.local.SmartFindDatabase
import com.smartfind.app.data.local.dao.DetectedObjectDao
import com.smartfind.app.data.local.dao.ObjectLocationDao
import com.smartfind.app.data.local.dao.ObjectReminderDao
import com.smartfind.app.data.local.dao.DetectionStatisticsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing database-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideSmartFindDatabase(
        @ApplicationContext context: Context
    ): SmartFindDatabase {
        return Room.databaseBuilder(
            context,
            SmartFindDatabase::class.java,
            "smartfind_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideDetectedObjectDao(database: SmartFindDatabase): DetectedObjectDao {
        return database.detectedObjectDao()
    }

    @Provides
    fun provideObjectLocationDao(database: SmartFindDatabase): ObjectLocationDao {
        return database.objectLocationDao()
    }

    @Provides
    fun provideObjectReminderDao(database: SmartFindDatabase): ObjectReminderDao {
        return database.objectReminderDao()
    }

    @Provides
    fun provideDetectionStatisticsDao(database: SmartFindDatabase): DetectionStatisticsDao {
        return database.detectionStatisticsDao()
    }
}
