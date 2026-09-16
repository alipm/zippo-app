package com.zippo.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zippo.app.data.BuildRosterRequest
import com.zippo.app.data.DriverLocation
import com.zippo.app.data.PassengerResponse
import com.zippo.app.data.ZippoApiService
import com.zippo.app.services.LocationService
import com.zippo.app.ui.theme.*
import kotlinx.coroutines.launch
import java.net.URLEncoder
import com.zippo.app.data.ZippoRoomDatabase
import com.zippo.app.data.LocalPassengerEntity
// 🟢 ADD this import line to your file header list:
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val apiService = remember { ZippoApiService.create() }

    // 🟢 ADD: Instantiate the local SQLite DAO holder controller profile
    val database = remember { ZippoRoomDatabase.getDatabase(context) }
    val passengerDao = remember { database.passengerCacheDao() }

    var isMorningShift by remember { mutableStateOf(true) }
    var selectedTripTab by remember { mutableStateOf(1) }
    var isLoading by remember { mutableStateOf(false) }
    var isTrackingActive by remember { mutableStateOf(false) }
    var activeRosterList by remember { mutableStateOf<List<PassengerResponse>>(emptyList()) }

    fun toggleBackgroundLocationTracking() {
        val serviceIntent = Intent(context, LocationService::class.java)

        if (!isTrackingActive) {
            // 🟢 ADD: Extract the active stop from your live database-backed roster list
            val nextStop = activeRosterList.firstOrNull()
            val activeCoords = if (isMorningShift) nextStop?.homeCoordinates else nextStop?.destinationCoordinates

            // Bind the real database parameters to the outbound intent payload wrapper
            serviceIntent.putExtra("TARGET_LAT", activeCoords?.lat ?: 17.4344)
            serviceIntent.putExtra("TARGET_LNG", activeCoords?.lng ?: 78.5562)
            serviceIntent.putExtra("TARGET_NAME", nextStop?.name ?: "First Passenger")

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            isTrackingActive = true
            Toast.makeText(context, "Tracking Geofence set for: ${nextStop?.name ?: "First Stop"}", Toast.LENGTH_SHORT).show()
        } else {
            context.stopService(serviceIntent)
            isTrackingActive = false
            Toast.makeText(context, "GPS Proximity Service Stopped", Toast.LENGTH_SHORT).show()
        }
    }


    fun fetchLiveRosterFromBackend() {
        coroutineScope.launch {
            isLoading = true
            try {
                val requestPayload = BuildRosterRequest(
                    date = "2026-09-09",
                    shift = if (isMorningShift) "MORNING" else "EVENING",
                    currentDriverLocation = DriverLocation(17.4344, 78.5562),
                    customTripSizes = mapOf("1" to 3, "2" to 3, "3" to 11)
                )

                val response = apiService.buildDailyRoster(
                    driverId = "60d0fe4f5311236168410d11",
                    request = requestPayload
                )

                if (response.success && response.data.isNotEmpty()) {
                    val incomingList = response.data.flatMap { segment ->
                        if (segment.tripNumber == selectedTripTab) {
                            segment.passengerIds.map { profile ->
                                PassengerResponse(
                                    id = profile.id ?: "",
                                    name = profile.name ?: "Unknown Candidate",
                                    passengerType = profile.passengerType ?: "SCHOOL",
                                    contractType = profile.contractType ?: "BOTH",
                                    monthlyFare = profile.monthlyFare ?: 1500,
                                    morningPickupTime = profile.morningPickupTime ?: "07:30 AM",
                                    eveningPickupTime = profile.eveningPickupTime ?: "04:30 PM",
                                    homeCoordinates = profile.homeCoordinates,
                                    destinationCoordinates = profile.destinationCoordinates,
                                    phoneNumber = profile.phoneNumber ?: "9849011223",
                                    address = if ((profile.passengerType ?: "SCHOOL") == "SCHOOL") "School Pickup Node" else "Office Transit Node"
                                )
                            }
                        } else emptyList()
                    }

                    activeRosterList = incomingList
                    Toast.makeText(context, "Cloud sync complete!", Toast.LENGTH_SHORT).show()

                    // 🟢 ADD: Asynchronously persist the fresh network batch right onto local SQLite disk cache tables
                    coroutineScope.launch {
                        try {
                            passengerDao.clearLocalCacheTable() // Wipes out stale day configurations
                            val entitiesToCache = incomingList.map { p ->
                                LocalPassengerEntity(
                                    id = p.id ?: "",
                                    name = p.name ?: "",
                                    passengerType = p.passengerType ?: "",
                                    contractType = p.contractType ?: "",
                                    monthlyFare = p.monthlyFare ?: 1500,
                                    morningPickupTime = p.morningPickupTime ?: "",
                                    eveningPickupTime = p.eveningPickupTime ?: "",
                                    homeLat = p.homeCoordinates?.lat ?: 17.4344,
                                    homeLng = p.homeCoordinates?.lng ?: 78.5562,
                                    destLat = p.destinationCoordinates?.lat ?: 17.4225,
                                    destLng = p.destinationCoordinates?.lng ?: 78.5412,
                                    phoneNumber = p.phoneNumber ?: "",
                                    address = p.address ?: ""
                                )
                            }
                            passengerDao.insertPassengerBatch(entitiesToCache)
                        } catch (dbEx: Exception) {
                            dbEx.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                // 🟢 FIXED: Gracefully intercepts network blocks and switches instantly to offline local data files!
                Toast.makeText(context, "Network Offline. Loading local cache...", Toast.LENGTH_SHORT).show()

                try {
                    val cachedEntities = passengerDao.getAllCachedPassengers()
                    if (cachedEntities.isNotEmpty()) {
                        activeRosterList = cachedEntities.map { entity ->
                            PassengerResponse(
                                id = entity.id,
                                name = entity.name,
                                passengerType = entity.passengerType,
                                contractType = entity.contractType,
                                monthlyFare = entity.monthlyFare,
                                morningPickupTime = entity.morningPickupTime,
                                eveningPickupTime = entity.eveningPickupTime,
                                homeCoordinates = com.zippo.app.data.LocationCoordinate(entity.homeLat, entity.homeLng),
                                destinationCoordinates = com.zippo.app.data.LocationCoordinate(entity.destLat, entity.destLng),
                                phoneNumber = entity.phoneNumber,
                                address = entity.address
                            )
                        }
                    }
                } catch (cacheEx: Exception) {
                    Toast.makeText(context, "Local Disk Read Failure: ${cacheEx.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                isLoading = false
            }

        }
    }

    LaunchedEffect(isMorningShift) {
        fetchLiveRosterFromBackend()
    }
    Scaffold(
        // 🟢 FIXED: Removed the auto-rickshaw emoji icon and significantly reduced layout height dimensions
        topBar = {
            Surface(
                color = YellowAuto,
                modifier = Modifier.fillMaxWidth().height(56.dp) // Force sleek, thin toolbar sizing layout bounds
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "ZIPPO DASHBOARD",
                        fontWeight = FontWeight.Black,
                        color = Color.Black,
                        fontSize = 18.sp
                    )
                }
            }
        },
        containerColor = DarkChassis
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // 🟢 ADD: High-Visibility Real-Time Map Radar Interface Component Container
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp), // Dedicated display matrix height layer
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceGray)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Renders structural spatial tracking visual anchors inside your staging environment
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text("🗺️ LIVE RADAR: NACHARAM SECTOR ACTIVE", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Dynamic location status text based on your background service states
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(if (isTrackingActive) SuccessGreen else AlertRed, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isTrackingActive) "Streaming Telemetry: 10.0.2.2" else "GPS Stream Offline",
                                color = Color.LightGray,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        // Visual tracking nodes representing your active database coordinates structure
                        Text("🎯 Target Gate: ${activeRosterList.firstOrNull()?.name ?: "No Stop Active"}", color = YellowAuto, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("📐 Radius Geofence Bound: 100 Meters Active", color = Color.LightGray, fontSize = 12.sp)
                    }
                }
            }
            // 🟢 HARDWARE TRACKING MASTER BUTTON COMMAND TARGET
            Button(
                onClick = { toggleBackgroundLocationTracking() },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTrackingActive) AlertRed else SuccessGreen
                )
            ) {
                Text(
                    text = if (isTrackingActive) "🛑 STOP DRIVING TRACKING" else "🚀 START ROUTE TRACKING",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            // Shift Selection Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { isMorningShift = true },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isMorningShift) YellowAuto else SurfaceGray,
                        contentColor = if (isMorningShift) Color.Black else Color.White
                    )
                ) {
                    Text("☀️ MORNING", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { isMorningShift = false },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isMorningShift) YellowAuto else SurfaceGray,
                        contentColor = if (!isMorningShift) Color.Black else Color.White
                    )
                ) {
                    Text("🌙 EVENING", fontWeight = FontWeight.Bold)
                }
            }

            // Capacity tabs indicator matrix
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceGray)) {
                Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                    Text("Active Load Balancing: 3+3+11 Split Mode", color = Color.LightGray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 2, 3).forEach { tripNum ->
                            FilterChip(
                                selected = selectedTripTab == tripNum,
                                onClick = { selectedTripTab = tripNum },
                                label = { Text(if(tripNum == 3) "Trip 3 (11)" else "Trip $tripNum (3)", fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                }
            }

            Text("LIVE NETWORK CHRONO TIMELINE:", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(activeRosterList) { passenger ->
                    Card(colors = CardDefaults.cardColors(containerColor = SurfaceGray)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    val pickupTime = if (isMorningShift) passenger.morningPickupTime else passenger.eveningPickupTime
                                    Text(text = pickupTime ?: "07:30 AM",
                                        fontSize = 20.sp, fontWeight = FontWeight.Black, color = YellowAuto)
                                    Text(text = passenger.name ?: "Unknown Candidate", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextMain)
                                }
                                Button(
                                    onClick = {
                                        val activeCoords = if (isMorningShift) passenger.homeCoordinates else passenger.destinationCoordinates
                                        // 🟢 FIXED: Pulls double parameters with zero-risk fallback definitions
                                        val targetLat = activeCoords?.lat ?: 17.4344
                                        val targetLng = activeCoords?.lng ?: 78.5562
                                        launchGoogleMapsIntent(context, targetLat, targetLng)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BluePin),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("🗺️ NAV")
                                }

                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            // 🟢 BOTTOM ACTION TARGET BUTTONS ROW
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val msg = "Zippo Alert: Arriving in 2 minutes! Please step out to the pickup point."
                                        sendFreeWhatsAppIntent(context, passenger.phoneNumber ?: "9849011223", msg)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                    modifier = Modifier.weight(1.2f).height(48.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("📱 1KM ALERT", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Button(
                                    onClick = { triggerNativeDialerIntent(context, passenger.phoneNumber ?: "9849011223") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                    modifier = Modifier.weight(0.8f).height(48.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("📞 CALL", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            // 🟢 CLOUD DATABASE MANUAL REFRESH BUTTON
            Button(
                onClick = { fetchLiveRosterFromBackend() },
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceGray),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("🔄 FORCE SYNC CLOUD DATABASE", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ================= NATIVE ANDROID SYSTEM INTENTS (100% FREE) =================

fun launchGoogleMapsIntent(context: Context, lat: Double?, lng: Double?) {
    try {
        val mapsUri = Uri.parse("google.navigation:q=$lat,$lng&mode=d")
        val mapIntent = Intent(Intent.ACTION_VIEW, mapsUri).apply {
            setPackage("com.google.android.apps.maps")
        }
        context.startActivity(mapIntent)
    } catch (e: Exception) {
        val webMapsUri = Uri.parse("https://google.com")
        val webIntent = Intent(Intent.ACTION_VIEW, webMapsUri)
        context.startActivity(webIntent)
    }
}

fun triggerNativeDialerIntent(context: Context, phoneNumber: String) {
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
    context.startActivity(intent)
}

fun sendFreeWhatsAppIntent(context: Context, phoneNumber: String, messageText: String) {
    try {
        val formattedNumber = if (!phoneNumber.startsWith("91")) "91$phoneNumber" else phoneNumber
        val urlEncodedMessage = URLEncoder.encode(messageText, "UTF-8")

        val whatsappUri = Uri.parse("https://whatsapp.com")

        val chatIntent = Intent(Intent.ACTION_VIEW, whatsappUri).apply {
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chatIntent)
    } catch (e: Exception) {
        val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phoneNumber")).apply {
            putExtra("sms_body", messageText)
        }
        context.startActivity(smsIntent)
    }
}
