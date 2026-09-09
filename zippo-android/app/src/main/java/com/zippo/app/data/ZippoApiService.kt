package com.zippo.app.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST

// 🟢 PASSENGER REGISTRATION REQUEST PAYLOAD SKELETON DEFINITION
data class CreatePassengerRequest(
    val name: String,
    val passengerType: String, // "SCHOOL" or "OFFICE"
    val contractType: String,  // "BOTH", "MORNING_ONLY", "EVENING_ONLY"
    val monthlyFare: Int,
    val morningPickupTime: String,
    val eveningPickupTime: String,
    val homeCoordinates: CoordinateNode,
    val destinationCoordinates: CoordinateNode
)

data class CoordinateNode(val lat: Double, val lng: Double)

interface ZippoApiService {

    // POST endpoint: /trips/build-roster
    @POST("trips/build-roster")
    suspend fun buildDailyRoster(
        @Header("x-driver-id") driverId: String,
        @Body request: BuildRosterRequest
    ): RosterApiResponse

    // 🟢 NEW ENDPOINT 1: Fetch consolidated monthly invoicing arrays
    @GET("payments/ledger-summary")
    suspend fun fetchFinancialLedger(
        @Header("x-driver-id") driverId: String
    ): LedgerSummaryResponse

    // 🟢 NEW ENDPOINT 2: Submit out-of-band payment status settlements
    @PATCH("payments/update-status")
    suspend fun clearInvoiceBalance(
        @Header("x-driver-id") driverId: String,
        @Body request: UpdatePaymentStatusRequest
    ): Map<String, Any>

    // 🟢 NEW ENDPOINT PATHWAY: Create and persist new passenger profiles into Atlas
    @POST("passengers/create")
    suspend fun createPassengerProfile(
        @Header("x-driver-id") driverId: String,
        @Body request: CreatePassengerRequest
    ): Map<String, Any>

    companion object {
        // Points to your local machine IP or cloud server gateway link
        // 10.0.2.2 is the official default internal proxy alias mapping straight to your laptop's localhost inside an Android Emulator
        private const val BASE_URL = "http://10.0.2.2:3000/"

        fun create(): ZippoApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ZippoApiService::class.java)
        }
    }
}
