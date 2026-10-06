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
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    // 🟢 ADD: Persistent WebSocket client interface pointer instance
    private var mSocket: Socket? = null

    // Mock coordinates setup representing an upcoming passenger gate point near Nacharam
    // Real coordinates array strings will be loaded from your NestJS trips/build-roster pipeline
    private var targetPassengerLat: Double = 17.4344 // Defaults to Nacharam Staging point
    private var targetPassengerLng: Double = 78.5562
    private var targetPassengerName: String = "Upcoming Stop"
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

        // 🟢 ADD: Initialize loopback connection socket mapping directly to your NestJS server gateway namespace
        try {
            val options = IO.Options().apply {
                forceNew = true
                reconnection = true
            }
            // Connects straight out of the virtual phone space right into your laptop's localhost port 3000
//            mSocket = IO.socket("https://zippo-backend.onrender.com", options)
            mSocket = IO.socket("http://10.0.2", options)
            mSocket?.connect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 🟢 ADD: Extract dynamic coordinate payloads passed from the UI thread intent bundle
        intent?.let {
            targetPassengerLat = it.getDoubleExtra("TARGET_LAT", 17.4344)
            targetPassengerLng = it.getDoubleExtra("TARGET_LNG", 78.5562)
            targetPassengerName = it.getStringExtra("TARGET_NAME") ?: "Upcoming Stop"
            hasTriggeredAlert = false // Reset trigger flag for this specific target stop boundary
        }

        // Keep your existing notification configuration and fusedLocationClient updates completely unchanged!
        val notification: Notification = NotificationCompat.Builder(this, "zippo_tracking")
            .setContentTitle("Zippo Active Shift Tracking")
            .setContentText("Monitoring live distance thresholds to $targetPassengerName...") // 🟢 UPDATE to read dynamic name
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

                    // 1,000-METER BOUNDARY GEOMETRIC TRIGGER POINT ;later change 100 to 1000
                    if (distanceInMeters <= 100.0 && !hasTriggeredAlert) {
                        hasTriggeredAlert = true
//                        notifyArrival()
                        val alertIntent = Intent("ZIPPO_PARENT_ALERT")
                        alertIntent.putExtra("PASSENGER_NAME", targetPassengerName)
                        sendBroadcast(alertIntent)
                        Toast.makeText(applicationContext, "🚨 $targetPassengerName is within 1 KM", Toast.LENGTH_LONG).show()
                        android.util.Log.d(
                            "ZIPPO_GEOFENCE",
                            "$targetPassengerName reached 1 KM radius"
                        )
                    }

                    // 🟢 ADD: Emit Real-Time Telemetry Payload Blocks straight over the socket highway
                    try {
                        val telemetryData = JSONObject().apply {
                            put(
                                "driverId",
                                "60d0fe4f5311236168410d11"
                            ) // Matches your synced testing ID token profile context
                            put("lat", location.latitude)
                            put("lng", location.longitude)
                            put(
                                "speed",
                                location.speed * 3.6
                            ) // Converts raw meters/sec values to Km/h for the driver's display matrix
                        }
                        // Fires the packet over the persistent duplex WebSocket connection channel natively
                        mSocket?.emit("update-location", telemetryData)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

//    private fun notifyArrival() {
//        Toast.makeText(applicationContext, "$targetPassengerName is within 1 KM", Toast.LENGTH_LONG)
//            .show()
//    }

    // High precision mathematical Haversine algorithm tracking distance vectors safely
    private fun calculateHaversineDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val r = 6371000.0 // Earth radius constant mapped in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a =
            sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(
                2
            )
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)

        // 🟢 ADD: Disconnect stream to prevent memory leakage logs inside your emulator
        mSocket?.disconnect()
        mSocket?.off()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
