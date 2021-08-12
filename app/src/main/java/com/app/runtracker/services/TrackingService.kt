package com.app.runtracker.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.app.runtracker.R
import com.app.runtracker.others.RunningConstants.ACTION_PAUSE_SERVICE
import com.app.runtracker.others.RunningConstants.ACTION_START_OR_RESUME_SERVICE
import com.app.runtracker.others.RunningConstants.ACTION_START_TRACKING_FRAGMENT
import com.app.runtracker.others.RunningConstants.ACTION_STOP_SERVICE
import com.app.runtracker.others.RunningConstants.FASTEST_LOCATION_UPDATE_INTERVAL
import com.app.runtracker.others.RunningConstants.LOCATION_UPDATE_INTERVAL
import com.app.runtracker.others.RunningConstants.NOTIFICATION_CHANNEL_ID
import com.app.runtracker.others.RunningConstants.NOTIFICATION_CHANNEL_NAME
import com.app.runtracker.others.RunningConstants.NOTIFICATION_ID
import com.app.runtracker.others.TrackingUtility
import com.app.runtracker.ui.MainActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

typealias polyLine = MutableList<LatLng>
typealias polyLines = MutableList<polyLine>

@AndroidEntryPoint
class TrackingService: LifecycleService() {

    @Inject
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private var timeInMs = 0L
    private var isFirst = true
    private var isServiceKilled = false

    @Inject
    lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var notificationManager: NotificationManager

    companion object {
        var isTracking = MutableLiveData<Boolean>()
        var pathPoints = MutableLiveData<polyLines>()
        var runningTime = MutableLiveData<Long>()
        var runStartTime = 0L
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        intent?.let {

            when(it.action){

                ACTION_START_OR_RESUME_SERVICE -> {
                    startTimer()
                    if(isFirst){

                        isFirst = !isFirst
                        startForegroundNotificationService()
                    }
                }

                ACTION_STOP_SERVICE -> killService()
                ACTION_PAUSE_SERVICE -> pauseService()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun pauseService() {
        isTracking.postValue(false)
    }

    override fun onCreate() {
        super.onCreate()

        postInitialValues()

        isTracking.observe(this, {
            updateLocationTracking(it)

            if(!isServiceKilled)
                trackNotificationStatus()
        })

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private fun postInitialValues() {
        isTracking.postValue(false)
        pathPoints.postValue(mutableListOf())
        runningTime.postValue(0L)
    }

    private fun startTimer(){

        isTracking.postValue(true)
        CoroutineScope(Dispatchers.Main).launch {
            while (isTracking.value!!){
                timeInMs += 1000
                runningTime.postValue(timeInMs)
                Log.d("time posted", timeInMs.toString())
                notificationBuilder.setContentText(TrackingUtility().getFormattedTime(timeInMs))
                notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())

                delay(1000L)
            }
        }
    }

    private fun killService(){

        isServiceKilled = true
        isFirst = true
        postInitialValues()
        pauseService()
        stopForeground(true)
        notificationManager.cancel(NOTIFICATION_ID)
        stopSelf()
    }

    private val locationCallback = object: LocationCallback(){

        override fun onLocationResult(result: LocationResult?) {
            super.onLocationResult(result)

            if(isTracking.value!!){
                result?.locations?.let { locations ->
                    for(location in locations){
                        val position = LatLng(location.latitude, location.longitude)
                        pathPoints.value?.apply {
                            last()?.add(position)
                            pathPoints.postValue(this)
                        }
                        Log.d("position", "lat:${location.latitude}, lng: ${location.longitude}")
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun updateLocationTracking(isTracking: Boolean){

        if(isTracking) {
            val locationRequest = LocationRequest().setInterval(LOCATION_UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_LOCATION_UPDATE_INTERVAL)
                .setPriority(PRIORITY_HIGH_ACCURACY)

            if (TrackingUtility().checkLocationPermissions(this))
                fusedLocationProviderClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper())
        }
        else{
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        }
    }

    private fun startForegroundNotificationService(){

        isTracking.postValue(true)

        runStartTime = System.currentTimeMillis()

        insertEmptyPolyline()

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            createNotification(notificationManager)
        }

        startForeground(NOTIFICATION_ID, notificationBuilder.build())
    }

    private fun insertEmptyPolyline() {

        pathPoints.value?.apply {
            add(mutableListOf())
            pathPoints.postValue(this)
        } ?: pathPoints.postValue(mutableListOf(mutableListOf()))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotification(notificationManager: NotificationManager){

        notificationManager.createNotificationChannel(NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW))
    }

    private fun trackNotificationStatus(){

        notificationBuilder.clearActions()

        if(isTracking.value!!){
            var pendingIntent = PendingIntent.getService(this, 2,
                    Intent(this, TrackingService::class.java).also {
                           it.action = ACTION_PAUSE_SERVICE
                    }, FLAG_UPDATE_CURRENT)

            notificationBuilder.addAction(R.drawable.ic_run, "Pause", pendingIntent)
        }
        else{
            var pendingIntent = PendingIntent.getService(this, 3,
                    Intent(this, TrackingService::class.java).also {
                        it.action = ACTION_START_OR_RESUME_SERVICE
                    }, FLAG_UPDATE_CURRENT)

            notificationBuilder.addAction(R.drawable.ic_run, "Resume", pendingIntent)
        }

        notificationBuilder.javaClass.getDeclaredField("mActions").apply {
            isAccessible = true
        }

        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }
}