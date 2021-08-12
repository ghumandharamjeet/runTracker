package com.app.runtracker.others

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import com.app.runtracker.services.polyLines
import java.sql.Time
import java.util.concurrent.TimeUnit
import kotlin.math.min

class TrackingUtility {

    fun checkLocationPermissions(context: Context) : Boolean{

        var permList = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.Q)
            permList.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)

        return arePermissionsGranted(context, permList)
    }

    fun arePermissionsGranted(context: Context, permList: MutableList<String>) : Boolean {
        return permList.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED}
    }

    fun getFormattedTime(timeInMs: Long): String{

        var time:Long = timeInMs

        var hours = TimeUnit.MILLISECONDS.toHours(time)
        time -= TimeUnit.HOURS.toMillis(hours)

        var minutes = TimeUnit.MILLISECONDS.toMinutes(time)
        time -= TimeUnit.MINUTES.toMillis(minutes)

        var seconds = TimeUnit.MILLISECONDS.toSeconds(time)
        time -= TimeUnit.SECONDS.toMillis(seconds)

        return "${if(hours < 10) "0" else ""}$hours:" +
                "${if(minutes < 10) "0" else ""}$minutes:" +
                "${if(seconds < 10) "0" else ""}$seconds"
    }

    fun getTotalDistanceFromPolyline(polyLines: polyLines): Float{

        var distance = 0f
        for(points in polyLines){
            for(index in 0 until points.size - 2){
                var result = FloatArray(1)

                Location.distanceBetween(points[index].latitude, points[index].longitude,
                        points[index+1].latitude, points[index+1].longitude, result)

                distance += result[0]
            }
        }

        return distance
    }
}