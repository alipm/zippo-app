package com.zippo.app.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream

object Utils {

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

    fun formatMonth(month: String): String {
        val parts = month.split("-")
        if (parts.size != 2) return month
        val year = parts[0]
        val monthNumber = parts[1]
        val monthName = when (monthNumber) {
            "01" -> "January"
            "02" -> "February"
            "03" -> "March"
            "04" -> "April"
            "05" -> "May"
            "06" -> "June"
            "07" -> "July"
            "08" -> "August"
            "09" -> "September"
            "10" -> "October"
            "11" -> "November"
            "12" -> "December"
            else -> monthNumber
        }
        return "$monthName $year"
    }

    fun formatPaymentDate(
        paymentDate: String?
    ): String {

        if (paymentDate.isNullOrBlank()) {
            return ""
        }

        return try {

            val datePart =
                paymentDate.substring(0, 10)

            val parts =
                datePart.split("-")

            "Paid on ${parts[2]}-${parts[1]}-${parts[0]}"

        } catch (e: Exception) {
            ""
        }
    }
}