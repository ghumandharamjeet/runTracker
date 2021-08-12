package com.app.runtracker.db

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.room.TypeConverter
import java.io.ByteArrayOutputStream


class TypeConverter {

    @TypeConverter
    fun fromBitmapToByteArray(bitmap: Bitmap): ByteArray{

        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    @TypeConverter
    fun fromByteArrayToBitmap(byteArray: ByteArray): Bitmap{

        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }
}