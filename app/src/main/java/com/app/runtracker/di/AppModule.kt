package com.app.runtracker.di

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.room.Room
import com.app.runtracker.db.RunningDatabase
import com.app.runtracker.others.RunningConstants
import com.app.runtracker.others.RunningConstants.RUNNING_DATABASE_NAME
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(ApplicationComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideRunningDatabase(
        @ApplicationContext app: Context
    ) =
        Room.databaseBuilder(
            app,
            RunningDatabase::class.java,
            RUNNING_DATABASE_NAME
        ).build()


    @Singleton
    @Provides
    fun provideRunDAO(runningDatabase: RunningDatabase) = runningDatabase.getRunDAO()

    @Singleton
    @Provides
    fun provideTrackingSharedPreferences(@ApplicationContext app: Context) =
            app.getSharedPreferences(RunningConstants.SHARED_PREFERENCE_NAME, MODE_PRIVATE)

    @Singleton
    @Provides
    fun provideFirstTime(trackingSharedPreferences: SharedPreferences) =
            trackingSharedPreferences.getBoolean(RunningConstants.KEY_FIRST_TIME, true)

    @Singleton
    @Provides
    fun provideName(trackingSharedPreferences: SharedPreferences) =
            trackingSharedPreferences.getString(RunningConstants.KEY_NAME, "") ?: ""

    @Singleton
    @Provides
    fun provideWeight(trackingSharedPreferences: SharedPreferences) =
            trackingSharedPreferences.getFloat(RunningConstants.KEY_WEIGHT,1f)
}