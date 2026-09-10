package com.zippo.app.data

// Matches the incoming passenger JSON structure from MongoDB Atlas
data class PassengerResponse(
    val id: String? = "",
    val name: String? = "Unknown Candidate",
    val passengerType: String? = "SCHOOL",
    val contractType: String? = "BOTH",
    val monthlyFare: Int? = 1500,
    val morningPickupTime: String? = "07:30 AM",
    val eveningPickupTime: String? = "04:30 PM",
    val homeCoordinates: LocationCoordinate? = LocationCoordinate(17.4344, 78.5562),
    val destinationCoordinates: LocationCoordinate? = LocationCoordinate(17.4225, 78.5412),
    val phoneNumber: String? = "9849011223",
    val address: String? = "Collection Stop Node"
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