package com.zippo.app.data

import com.zippo.app.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

// 🟢 PASSENGER REGISTRATION REQUEST PAYLOAD SKELETON DEFINITION
data class CreatePassengerRequest(
    val name: String,
    val passengerType: String, // "SCHOOL" or "OFFICE"
    val contractType: String,  // "BOTH", "MORNING_ONLY", "EVENING_ONLY"
    val monthlyFare: Int,
    val morningPickupTime: String,
    val eveningPickupTime: String,
    val homeCoordinates: CoordinateNode,
    val destinationCoordinates: CoordinateNode,
    val profileImageBase64: String? = null
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
    ): CreatePassengerResponse

    // 🟢 ADD: Dispatches voice strings straight up to the NestJS server AI core router
    @POST("ai/voice-khata")
    suspend fun processVoiceKhataEntry(
        @Body request: VoiceKhataRequest
    ): VoiceKhataResponse

    @GET("passengers/list")
    suspend fun fetchAllPassengers(
        @Header("x-driver-id") driverId: String
    ): List<PassengerResponse>

    @DELETE("passengers/{id}")
    suspend fun deletePassengerProfile(
        @Header("x-driver-id") driverId: String,
        @Path("id") passengerId: String
    ): Map<String, Any>

    @PUT("passengers/{id}")
    suspend fun updatePassengerProfile(
        @Header("x-driver-id") driverId: String,
        @Path("id") passengerId: String,
        @Body request: CreatePassengerRequest
    ): Map<String, Any>

    companion object {

        fun create(): ZippoApiService {
            return Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ZippoApiService::class.java)
        }
    }
}
