package com.zippo.app.model

import android.graphics.Bitmap
import com.zippo.app.utils.AppConstants

data class PassengerFormState(

    val passengerName: String = "",

    val phoneNumber: String = "",

    val fixedFare: String = "",

    val contractSelection: String = "BOTH",

    val passengerTypeSelection: String = "SCHOOL",

    val morningTimeInput: String = "",

    val eveningTimeInput: String = "",

    val homeLatInput: String = "",

    val homeLngInput: String = "",

    val destLatInput: String = "",

    val destLngInput: String = "",

    val capturedPhotoPreview: Bitmap? = null,

    val remarks: String = ""
)