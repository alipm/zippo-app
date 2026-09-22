package com.zippo.app.data

import com.google.gson.annotations.SerializedName
import com.zippo.app.utils.AppConstants

// Matches the incoming passenger JSON structure from MongoDB Atlas
data class PassengerResponse(
    @SerializedName("_id")
    val id: String? = "",
    val name: String? = "Unknown Candidate",
    val passengerType: String? = AppConstants.DEFAULT_TYPE,
    val contractType: String? = AppConstants.DEFAULT_CONTRACT_TYPE,
    val monthlyFare: Int? = 1500,
    val morningPickupTime: String? = AppConstants.DEFAULT_MORNING_TIME,
    val eveningPickupTime: String? = AppConstants.DEFAULT_EVENING_TIME,
    val homeCoordinates: LocationCoordinate? = LocationCoordinate(AppConstants.DEFAULT_HOME_LAT.toDouble(), AppConstants.DEFAULT_HOME_LNG.toDouble()),
    val destinationCoordinates: LocationCoordinate? = LocationCoordinate(
        AppConstants.DEFAULT_DEST_LAT.toDouble(), AppConstants.DEFAULT_DEST_LNG.toDouble()),
    val phoneNumber: String? = AppConstants.DEFAULT_PHONE_NUMBER,
    val address: String? = "Collection Stop Node",
    val profileImageBase64: String? = "",
    val remarks: String? = ""
)

data class LocationCoordinate(
    val lat: Double? = 17.4344,
    val lng: Double? = 78.5562
)

// The request payload skeleton matching our BuildRosterDto contract on NestJS
data class BuildRosterRequest(
    val date: String, // Format: "YYYY-MM-DD"
    val shift: String, // "MORNING" or "EVENING"
    val currentDriverLocation: DriverLocation,
    val customTripSizes: Map<String, Int>? = null
)

data class DriverLocation(
    val lat: Double,
    val lng: Double
)

data class RosterApiResponse(
    val success: Boolean,
    val message: String,
    val data: List<TripSegmentResponse>
)

data class TripSegmentResponse(
    val id: String,
    val date: String,
    val shift: String,
    val tripNumber: Int,
    val passengerIds: List<PassengerResponse>,
    val status: String
)

// Matches your MongoDB family ledger billing collections format
data class LedgerInvoiceResponse(
    val id: String,
    val householdName: String,
    val description: String,
    val fareAmount: Int,
    val isSettled: Boolean,
    val parentPhoneNumber: String
)

data class LedgerSummaryResponse(
    val success: Boolean,
    val message: String,
    val totalCollected: Int,
    val pendingDues: Int,
    val invoices: List<LedgerInvoiceResponse>
)

data class UpdatePaymentStatusRequest(
    val invoiceId: String,
    val paymentType: String // "CASH" or "UPI"
)

// 🟢 ADD: Explicit parsing model for profile creations
data class CreatePassengerResponse(
    val success: Boolean,
    val message: String,
    val passengerId: String
)

// 🟢 ADD: Representation payload structure for voice ledger entries
data class VoiceKhataRequest(
    val transcript: String
)

data class VoiceKhataResponse(
    val success: Boolean,
    val message: String,
    val data: VoiceParsedPayload
)

data class VoiceParsedPayload(
    val targetFamily: String?,
    val amountCollected: Int?,
    val paymentType: String?
)
