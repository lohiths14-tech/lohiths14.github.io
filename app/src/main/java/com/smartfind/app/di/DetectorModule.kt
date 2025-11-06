package com.smartfind.app.di

import android.content.Context
import com.smartfind.app.domain.detector.ObjectDetector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing object detection dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object DetectorModule {

    @Provides
    @Singleton
    fun provideObjectDetector(
        @ApplicationContext context: Context
    ): ObjectDetector {
        return ObjectDetector(context)
    }
}
