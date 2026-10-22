package com.zippo.app.utils

import android.content.Context

class SessionManager(context: Context) {

    private val prefs =
        context.getSharedPreferences(
            "zippo_session",
            Context.MODE_PRIVATE
        )

    fun saveDriverSession(
        driverId: String,
        driverName: String
    ) {
        prefs.edit()
            .putString("driver_id", driverId)
            .putString("driver_name", driverName)
            .apply()
    }

    fun getDriverId(): String? {
        return prefs.getString(
            "driver_id",
            null
        )
    }

    fun getDriverName(): String? {
        return prefs.getString(
            "driver_name",
            null
        )
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean {
        return getDriverId() != null
    }
}