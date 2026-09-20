package com.zippo.app.data.repository

import com.zippo.app.data.CreatePassengerRequest
import com.zippo.app.data.PassengerResponse
import com.zippo.app.data.ZippoApiService

class PassengerRepository(
    private val apiService: ZippoApiService
) {

    suspend fun getPassengers(
        driverId: String
    ): List<PassengerResponse> {

        return apiService.fetchAllPassengers(
            driverId = driverId
        )
    }

    suspend fun createPassenger(
        driverId: String,
        request: CreatePassengerRequest
    ) {

        apiService.createPassengerProfile(
            driverId = driverId,
            request = request
        )
    }

    suspend fun updatePassenger(
        driverId: String,
        passengerId: String,
        request: CreatePassengerRequest
    ) {

        apiService.updatePassengerProfile(
            driverId = driverId,
            passengerId = passengerId,
            request = request
        )
    }

    suspend fun deletePassenger(
        driverId: String,
        passengerId: String
    ) {

        apiService.deletePassengerProfile(
            driverId = driverId,
            passengerId = passengerId
        )
    }
}