package com.app.runtracker.db

import android.graphics.Bitmap
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "running_table")
data class Run(
    var img: Bitmap? = null,
    var runTimeStamp: Long = 0L,
    var caloriesBurned: Int = 0,
    var distanceInMeters: Int = 0,
    var timeInMillis: Long = 0L,
    var avgSpeedInKMH: Float = 0F
) {

    @PrimaryKey(autoGenerate = true)
    var id:Int? = null
}