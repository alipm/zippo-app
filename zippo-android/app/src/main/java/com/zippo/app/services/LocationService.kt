package com.zippo.app.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class LocationService : Service() {

    override fun onCreate() {
        super.onCreate()
        // Creates a background persistent tray alert to prevent the Android OS from killing your 1 Km geofence process
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "zippo_tracking",
                "Zippo Route Active Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification: Notification = NotificationCompat.Builder(this, "zippo_tracking")
            .setContentTitle("Zippo Active Shift Operations")
            .setContentText("Monitoring live distance thresholds to upcoming stops...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()

        startForeground(1, notification)

        // This background worker loop will connect to your NestJS server coordinate engine down the line
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
