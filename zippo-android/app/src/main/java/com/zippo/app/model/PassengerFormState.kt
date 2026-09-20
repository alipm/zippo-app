package com.zippo.app.model

import android.graphics.Bitmap
import com.zippo.app.utils.AppConstants

data class PassengerFormState(

    val passengerName: String = "",

    val contactPhone: String = "",

    val fixedFare: String = AppConstants.DEFAULT_FARE,

    val contractSelection: String = AppConstants.DEFAULT_CONTRACT_TYPE,

    val passengerTypeSelection: String = AppConstants.DEFAULT_TYPE,

    val morningTimeInput: String = AppConstants.DEFAULT_MORNING_TIME,

    val eveningTimeInput: String = AppConstants.DEFAULT_EVENING_TIME,

    val homeLatInput: String = AppConstants.DEFAULT_HOME_LAT,

    val homeLngInput: String = AppConstants.DEFAULT_HOME_LNG,

    val destLatInput: String = AppConstants.DEFAULT_DEST_LAT,

    val destLngInput: String = AppConstants.DEFAULT_DEST_LNG,

    val capturedPhotoPreview: Bitmap? = null
)