package com.zippo.app.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ZippoApiService {

    // POST endpoint: /trips/build-roster
    @POST("trips/build-roster")
    suspend fun buildDailyRoster(
        @Header("x-driver-id") driverId: String,
        @Body request: BuildRosterRequest
    ): RosterApiResponse

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
