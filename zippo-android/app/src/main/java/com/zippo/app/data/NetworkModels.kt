package com.zippo.app.data

// Matches the incoming passenger JSON structure from MongoDB Atlas
data class PassengerResponse(
    val id: String,
    val name: String,
    val time: String,
    val address: String,
    val phoneNumber: String,
    val latitude: Double,
    val longitude: Double
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
    val passengerIds: List<String>,
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
