package com.zippo.app.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import kotlin.math.*

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    // Mock coordinates setup representing an upcoming passenger gate point near Nacharam
    // Real coordinates array strings will be loaded from your NestJS trips/build-roster pipeline
    private val targetPassengerLat = 17.4421
    private val targetPassengerLng = 78.5645
    private var hasTriggeredAlert = false

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "zippo_tracking",
                "Zippo Route Active Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        initializeLocationTrackingCallback()
    }
    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification: Notification = NotificationCompat.Builder(this, "zippo_tracking")
            .setContentTitle("Zippo Active Shift Tracking")
            .setContentText("Monitoring live distance thresholds to upcoming gates...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()

        startForeground(1, notification)

        // Request high-accuracy updates every 5 seconds to maximize battery longevity on the road
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(3000)
            .build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        return START_STICKY
    }

    private fun initializeLocationTrackingCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    val distanceInMeters = calculateHaversineDistance(
                        location.latitude, location.longitude,
                        targetPassengerLat, targetPassengerLng
                    )

                    // 1,000-METER BOUNDARY GEOMETRIC TRIGGER POINT
                    if (distanceInMeters <= 1000.0 && !hasTriggeredAlert) {
                        hasTriggeredAlert = true

                        // Fire a background text alert broadcast loop to warn the driver context
                        Toast.makeText(
                            applicationContext,
                            "🚨 ZIPPO ALERT: Within 1 Km of next stop! Open app to trigger alert.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    // High precision mathematical Haversine algorithm tracking distance vectors safely
    private fun calculateHaversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Earth radius constant mapped in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
