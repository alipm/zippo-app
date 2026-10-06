package com.zippo.app.data

import RenderColdStartInterceptor
import com.zippo.app.BuildConfig
import okhttp3.OkHttpClient
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
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// 🟢 PASSENGER REGISTRATION REQUEST PAYLOAD SKELETON DEFINITION
data class CreatePassengerRequest(
    val name: String,
    val passengerType: String, // "SCHOOL" or "OFFICE"
    val contractType: String,  // "BOTH", "MORNING", "EVENING"
    val monthlyFare: Int,
    val morningPickupTime: String,
    val eveningPickupTime: String,
    val homeCoordinates: CoordinateNode,
    val destinationCoordinates: CoordinateNode,
    val profileImageBase64: String? = null,
    val phoneNumber: String,
    val remarks: String? = null
)

data class CoordinateNode(val lat: Double, val lng: Double)

interface ZippoApiService {
    // POST endpoint: /trips/build-roster
    @POST("trips/build-roster")
    suspend fun buildDailyRoster(
        @Header("x-driver-id") driverId: String,
        @Body request: BuildRosterRequest
    ): RosterApiResponse

    @POST("trips/mark-attendance")
    suspend fun markAttendance(
        @Header("x-driver-id") driverId: String,
        @Body request: MarkAttendanceRequest
    ): AttendanceApiResponse

    // 🟢 NEW ENDPOINT 1: Fetch consolidated monthly invoicing arrays
    @GET("payments/ledger-summary")
    suspend fun fetchFinancialLedger(
        @Query("driverId") driverId: String,
        @Query("month") month: String
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

    @GET("payments/available-months")
    suspend fun fetchAvailableMonths(
        @Query("driverId")
        driverId: String
    ): List<String>

    companion object {

        fun create(): ZippoApiService {
            // 1. Build an OkHttpClient containing our cold-start countdown interceptor
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(
                    60,
                    TimeUnit.SECONDS
                ) // Keeps socket open long enough for Render to boot
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(RenderColdStartInterceptor()) // <-- Intercepts and monitors live request latencies
                .build()

            return Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ZippoApiService::class.java)
        }
    }
}
