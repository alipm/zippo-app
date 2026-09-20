package com.zippo.app.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream

object ImageUtils {

    fun convertBitmapToBase64(
        bitmap: Bitmap?
    ): String? {

        if (bitmap == null) return null

        val outputStream = ByteArrayOutputStream()

        bitmap.compress(
            Bitmap.CompressFormat.JPEG, 70, outputStream
        )

        val byteArray = outputStream.toByteArray()

        return Base64.encodeToString(
            byteArray, Base64.DEFAULT
        )
    }

    fun decodeBase64ToBitmap(
        base64String: String?
    ): Bitmap? {

        return try {

            if (base64String.isNullOrEmpty()) {
                null
            } else {

                val decodedBytes = Base64.decode(
                    base64String, Base64.DEFAULT
                )

                BitmapFactory.decodeByteArray(
                    decodedBytes, 0, decodedBytes.size
                )
            }
        } catch (e: Exception) {

            null
        }
    }
}