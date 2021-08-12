package com.app.runtracker.di

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.app.runtracker.R
import com.app.runtracker.others.RunningConstants
import com.app.runtracker.ui.MainActivity
import com.google.android.gms.location.FusedLocationProviderClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped

@InstallIn(ServiceComponent::class)
@Module
object ServiceModule {


    @Provides
    @ServiceScoped
    fun provideFusedLocationProviderClient(@ApplicationContext context: Context) =
        FusedLocationProviderClient(context)

    @Provides
    @ServiceScoped
    fun providePendingIntent(@ApplicationContext context: Context) =
            PendingIntent.getActivity(context, 0,
                    Intent(context, MainActivity::class.java).also {
                           it.action = RunningConstants.ACTION_START_TRACKING_FRAGMENT
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT)


    @Provides
    @ServiceScoped
    fun providesNotificationBuilder(@ApplicationContext context: Context, pendingIntent: PendingIntent) =
            NotificationCompat.Builder(context, RunningConstants.NOTIFICATION_CHANNEL_ID)
                    .setAutoCancel(true)
                    .setOngoing(true)
                    .setSmallIcon(R.drawable.ic_run)
                    .setContentTitle("Running")
                    .setContentText("00:00:00")
                    .setContentIntent(pendingIntent)


}