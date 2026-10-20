package com.zippo.app.ui.screens

// 🟢 ADD this import line to your file header list:
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zippo.app.data.BuildRosterRequest
import com.zippo.app.data.DriverLocation
import com.zippo.app.data.GenerateAssignmentCodeRequest
import com.zippo.app.data.JoinTripByCodeRequest
import com.zippo.app.data.LocalPassengerEntity
import com.zippo.app.data.MarkAttendanceRequest
import com.zippo.app.data.PassengerResponse
import com.zippo.app.data.SwapPassengerRequest
import com.zippo.app.data.ZippoApiService
import com.zippo.app.data.ZippoRoomDatabase
import com.zippo.app.services.LocationService
import com.zippo.app.ui.theme.*
import com.zippo.app.utils.AppConstants
import com.zippo.app.utils.SessionManager
import com.zippo.app.utils.Utils.decodeBase64ToBitmap
import com.zippo.app.utils.Utils.generateTripSizeMap
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val apiService = remember { ZippoApiService.create() }

    val database = remember { ZippoRoomDatabase.getDatabase(context) }
    val passengerDao = remember { database.passengerCacheDao() }

    var isMorningShift by remember { mutableStateOf(true) }
    var selectedTripTab by remember { mutableStateOf(1) }
    var isLoading by remember { mutableStateOf(false) }
    var isTrackingActive by remember { mutableStateOf(false) }
    var tripStarted by remember { mutableStateOf(false) }
    var activeRosterList by remember { mutableStateOf<List<PassengerResponse>>(emptyList()) }
    var totalPassengersInTrip by remember { mutableStateOf(0) }
    var totalActivePassengers by remember { mutableStateOf(0) }
    var absentPassengerIds by remember { mutableStateOf(setOf<String>()) }
    val nextPassenger = activeRosterList.firstOrNull()
    var completedCount by remember { mutableStateOf(0) }
    var vehicleCapacity by remember { mutableIntStateOf(5) }
    var absentCount by remember { mutableStateOf(0) }
    var dynamicAvailableTrips by remember { mutableStateOf<List<Int>>(listOf(1)) }
    var expandedNotes by remember { mutableStateOf(false) }
    var selectedPassengerForMove by remember { mutableStateOf<PassengerResponse?>(null) }
    val totalTrips = dynamicAvailableTrips.size

    var showJoinTripDialog by remember { mutableStateOf(false) }
    var assignmentCodeInput by remember { mutableStateOf("") }

    var generatedAssignmentCode by remember { mutableStateOf<String?>(null) }
    val sessionManager = remember { SessionManager(context) }
    val userRole = sessionManager.getDriverRole() ?: "DRIVER"

//    val parentAlertReceiver = remember {
//        object : android.content.BroadcastReceiver() {
//            override fun onReceive(context: Context?, intent: Intent?) {
//                val passengerName = intent?.getStringExtra("PASSENGER_NAME") ?: "Passenger"
//                sendFreeWhatsAppIntent(
//                    context = context!!,
//                    phoneNumber = nextPassenger?.phoneNumber ?: AppConstants.DEFAULT_PHONE_NUMBER,
//                    messageText = """Hello, Your pickup vehicle is within 1 KM. Passenger: $passengerName Please be ready. - Zippo""".trimIndent()
//                )
//            }
//        }
//    }

    fun toggleBackgroundLocationTracking() {
        val serviceIntent = Intent(context, LocationService::class.java)

        if (!isTrackingActive) {
            // 🟢 ADD: Extract the active stop from your live database-backed roster list
            val nextStop = activeRosterList.firstOrNull()
            val activeCoords =
                if (isMorningShift) nextStop?.homeCoordinates else nextStop?.destinationCoordinates

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
            Toast.makeText(
                context,
                "Tracking Geofence set for: ${nextStop?.name ?: "First Stop"}",
                Toast.LENGTH_SHORT
            ).show()
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
                val uniqueDeviceDriverId = sessionManager.getDriverId() ?: ""
                val tripSizeMap = generateTripSizeMap(
                    totalPassengers =
                    if (totalActivePassengers > 0)
                        totalActivePassengers
                    else
                        5,
                    capacity = vehicleCapacity
                )
                val requestPayload = BuildRosterRequest(
                    date = LocalDate.now().toString(),//"2026-09-09",
                    shift = if (isMorningShift) "MORNING" else "EVENING",
                    currentDriverLocation = DriverLocation(17.4344, 78.5562),
                    customTripSizes = tripSizeMap
                )

                val response = apiService.buildDailyRoster(
                    driverId = uniqueDeviceDriverId,
                    request = requestPayload
                )

                if (response.success && response.data.isNotEmpty()) {
                    totalActivePassengers = response.data.sumOf { it.passengerIds.size }
                    // 🟢 ADD: Extracts exact list of trips calculated by your backend service
                    dynamicAvailableTrips = response.data.map { it.tripNumber }.distinct().sorted()
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
                                    profileImageBase64 = profile.profileImageBase64,
                                    address = if ((profile.passengerType
                                            ?: "SCHOOL") == "SCHOOL"
                                    ) "School Pickup Node" else "Office Transit Node",
                                    remarks = profile.remarks ?: "",
                                )
                            }
                        } else emptyList()
                    }
                    activeRosterList = incomingList
                    totalPassengersInTrip = incomingList.size
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
                                    address = p.address ?: "",
                                    profileImageBase64 = p.profileImageBase64 ?: "",
                                    remarks = p.remarks ?: "",
                                )
                            }
                            passengerDao.insertPassengerBatch(entitiesToCache)
                        } catch (dbEx: Exception) {
                            dbEx.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Network Offline. Loading local cache...",
                    Toast.LENGTH_SHORT
                ).show()

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
                                homeCoordinates = com.zippo.app.data.LocationCoordinate(
                                    entity.homeLat,
                                    entity.homeLng
                                ),
                                destinationCoordinates = com.zippo.app.data.LocationCoordinate(
                                    entity.destLat,
                                    entity.destLng
                                ),
                                phoneNumber = entity.phoneNumber,
                                address = entity.address,
                                profileImageBase64 = entity.profileImageBase64,
                                remarks = entity.remarks,
                            )
                        }
                    }
                } catch (cacheEx: Exception) {
                    Toast.makeText(
                        context,
                        "Local Disk Read Failure: ${cacheEx.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } finally {
                isLoading = false
            }
        }
    }

    fun saveAttendance(
        passengerId: String,
        status: String
    ) {
        coroutineScope.launch {
            try {
                apiService.markAttendance(
                    driverId = sessionManager.getDriverId() ?: "",
                    request = MarkAttendanceRequest(
                        passengerId = passengerId,
                        date = LocalDate.now().toString(),//"2026-10-06", // temporary
                        shift =
                        if (isMorningShift)
                            "MORNING"
                        else
                            "EVENING",
                        status = status
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun restartTrackingForNextPassenger() {

        if (!tripStarted) return

        toggleBackgroundLocationTracking()
        toggleBackgroundLocationTracking()
    }

    fun swapPassengerToTrip(
        passengerId: String,
        destinationTrip: Int
    ) {
        coroutineScope.launch {
            try {
                apiService.swapPassenger(
                    driverId = sessionManager.getDriverId() ?: "",
                    request = SwapPassengerRequest(
                        sourceTripNumber = selectedTripTab,
                        destinationTripNumber = destinationTrip,
                        passengerId = passengerId,
                        date = LocalDate.now().toString(),
                        shift =
                        if (isMorningShift)
                            "MORNING"
                        else
                            "EVENING"
                    )
                )
                fetchLiveRosterFromBackend()
                Toast.makeText(context, "Passenger moved successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Move failed", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    fun generateTripCode() {
        coroutineScope.launch {
            try {
                val response = apiService.generateAssignmentCode(
                    driverId = sessionManager.getDriverId() ?: "",
                    request = GenerateAssignmentCodeRequest(
                        tripNumber = selectedTripTab,
                        date = LocalDate.now().toString(),
                        shift = if (isMorningShift)
                            "MORNING"
                        else
                            "EVENING"
                    )
                )
                generatedAssignmentCode = response.code
            } catch (e: Exception) {
                Toast.makeText(context, "Failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(isMorningShift) {
        fetchLiveRosterFromBackend()
    }

    LaunchedEffect(selectedTripTab) {
        fetchLiveRosterFromBackend()
    }

//    DisposableEffect(Unit) {
//        val filter = android.content.IntentFilter("ZIPPO_PARENT_ALERT")
//        context.registerReceiver(parentAlertReceiver, filter)
//        onDispose {
//            context.unregisterReceiver(parentAlertReceiver)
//        }
//    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkChassis)
    ) {
        Surface(
            color = YellowAuto,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    "Route Planner",
                    fontWeight = FontWeight.Black,
                    color = Color.Black,
                    fontSize = 18.sp
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Driver: ${sessionManager.getDriverName()}", color = Color.White
            )
            Text(
                text = "Role: ${sessionManager.getDriverRole()}", color = YellowAuto
            )
            TextButton(
                onClick = {
                    sessionManager.clearSession()
                }
            ) {
                Text("Logout")
            }
            if (tripStarted) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceGray)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = "📍 NEXT PICKUP",
                            color = YellowAuto,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Trip $selectedTripTab",
                            color = YellowAuto,
                            fontSize = 12.sp
                        )
                        val passengerBitmap =
                            decodeBase64ToBitmap(
                                nextPassenger?.profileImageBase64
                            )
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (passengerBitmap != null) {
                                Image(
                                    bitmap = passengerBitmap.asImageBitmap(),
                                    contentDescription = "Passenger Photo",
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (nextPassenger == null) "Trip Completed" else nextPassenger?.name
                                        ?: "No Passenger Available",
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (nextPassenger != null) {
                                    Text(
                                        text = if (isMorningShift)
                                            "⏰ ${nextPassenger?.morningPickupTime ?: "--"}"
                                        else
                                            "⏰ ${nextPassenger?.eveningPickupTime ?: "--"}",
                                        color = YellowAuto
                                    )
                                }
                                Text(
                                    text = "Stops: ${activeRosterList.size}",
                                    color = Color.LightGray
                                )
                                Text(
                                    text = "Done: $completedCount | Absent: $absentCount | Total: $totalPassengersInTrip",
                                    color = SuccessGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        if (!nextPassenger?.remarks.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF2A2A2A)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(6.dp)
                                ) {
                                    Text(
                                        text = "📝 Notes",
                                        color = YellowAuto,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = nextPassenger?.remarks ?: "",
                                        color = Color.LightGray,
                                        fontSize = 12.sp,
                                        maxLines = if (expandedNotes) 10 else 3
                                    )
                                    if ((nextPassenger?.remarks?.length ?: 0) > 80) {
                                        TextButton(
                                            onClick = {
                                                expandedNotes = !expandedNotes
                                            }
                                        ) {
                                            Text(
                                                text = if (expandedNotes)
                                                    "Show Less"
                                                else
                                                    "Show More",
                                                color = YellowAuto
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (nextPassenger != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        val coords =
                                            if (isMorningShift)
                                                nextPassenger?.homeCoordinates
                                            else
                                                nextPassenger?.destinationCoordinates
                                        launchGoogleMapsIntent(
                                            context,
                                            coords?.lat,
                                            coords?.lng
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BluePin
                                    )
                                ) {
                                    Text("NAV")
                                }
                                Button(
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        triggerNativeDialerIntent(
                                            context,
                                            nextPassenger?.phoneNumber
                                                ?: AppConstants.DEFAULT_PHONE_NUMBER
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.DarkGray
                                    )
                                ) {
                                    Text("CALL")
                                }
                                Button(
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        if (activeRosterList.isNotEmpty()) {
                                            val completedPassenger =
                                                activeRosterList.first()
                                            saveAttendance(
                                                passengerId = completedPassenger.id ?: "",
                                                status = "PICKED_UP"
                                            )
                                            activeRosterList = activeRosterList.drop(1)
                                            completedCount++
                                            restartTrackingForNextPassenger()
                                            Toast.makeText(
                                                context,
                                                "Passenger Picked Up",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SuccessGreen
                                    )
                                ) {
                                    Text("DONE")
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            TextButton(
                                onClick = {
                                    if (activeRosterList.size > 1) {
                                        val skippedPassenger =
                                            activeRosterList.first()
                                        activeRosterList =
                                            activeRosterList.drop(1) + skippedPassenger
                                        Toast.makeText(
                                            context,
                                            "${skippedPassenger.name} moved to end",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "⏭ Skip Passenger",
                                    color = Color(0xFFFF9800),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            TextButton(
                                onClick = {
                                    if (activeRosterList.isNotEmpty()) {
                                        val absentPassenger =
                                            activeRosterList.first()
                                        saveAttendance(
                                            passengerId = absentPassenger.id ?: "",
                                            status = "ABSENT"
                                        )
                                        activeRosterList = activeRosterList.drop(1)
                                        absentCount++
                                        restartTrackingForNextPassenger()
                                        Toast.makeText(
                                            context,
                                            "${absentPassenger.name} marked absent",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "🚫 Mark Current Passenger Absent",
                                    color = AlertRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            if (activeRosterList.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = SuccessGreen
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp),
                    ) {
                        Text(
                            text =
                            if (selectedTripTab < totalTrips)
                                "✅ TRIP $selectedTripTab COMPLETED"
                            else
                                "🎉 ALL TRIPS COMPLETED",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Picked: $completedCount",
                            color = Color.White
                        )
                        Text(
                            text = "Absent: $absentCount",
                            color = Color.White
                        )
                        if (selectedTripTab < totalTrips) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    selectedTripTab++
                                    completedCount = 0
                                    absentCount = 0
                                    tripStarted = true
                                    fetchLiveRosterFromBackend()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("START TRIP ${selectedTripTab + 1}")
                            }
                        }
                    }
                }
            }
            if (tripStarted) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = SurfaceGray
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "📋 UPCOMING STOPS",
                            color = YellowAuto,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        activeRosterList
                            .drop(1)
                            .take(5)
                            .forEachIndexed { index, passenger ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${index + 2}. ${passenger.name}",
                                        color = Color.White
                                    )
                                    if (absentPassengerIds.contains(passenger.id ?: "")) {
                                        Text(
                                            text = "🚫 ABSENT",
                                            color = AlertRed,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Button(
                                            onClick = {
                                                absentPassengerIds =
                                                    absentPassengerIds + (passenger.id ?: "")
                                                saveAttendance(
                                                    passengerId = passenger.id ?: "",
                                                    status = "ABSENT"
                                                )
                                                fetchLiveRosterFromBackend()
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = AlertRed
                                            )
                                        ) {
                                            Text("Absent")
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                        if (activeRosterList.size <= 1) {
                            Text(
                                text = "No upcoming passengers",
                                color = Color.LightGray
                            )
                        }
                    }
                }
            }
//            val estimatedTrips =
//                kotlin.math.ceil(activeRosterList.size.toDouble() / vehicleCapacity.toDouble())
//                    .toInt()
            if (!tripStarted) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { isMorningShift = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isMorningShift) YellowAuto else SurfaceGray,
                            contentColor = if (isMorningShift) Color.Black else Color.White
                        )
                    ) {
                        Text("MORNING", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { isMorningShift = false },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (!isMorningShift) YellowAuto else SurfaceGray,
                            contentColor = if (!isMorningShift) Color.Black else Color.White
                        )
                    ) {
                        Text("EVENING", fontWeight = FontWeight.Bold)
                    }
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = SurfaceGray
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Vehicle Capacity",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        if (vehicleCapacity > 1)
                                            vehicleCapacity--
                                    }
                                ) {
                                    Text("-")
                                }
                                Text(
                                    text = vehicleCapacity.toString(),
                                    color = YellowAuto,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                                TextButton(
                                    onClick = {
                                        vehicleCapacity++
                                    }
                                ) {
                                    Text("+")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Passengers: ${activeRosterList.size}",
                            color = Color.LightGray
                        )
                        Text(
                            text = "Estimated Trips: ${dynamicAvailableTrips.size}",
                            color = YellowAuto,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Button(
                    onClick = {
                        fetchLiveRosterFromBackend()
                        Toast.makeText(
                            context,
                            "Route regenerated using capacity $vehicleCapacity",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuccessGreen
                    )
                ) {
                    Text(
                        "Create Trips",
                        fontWeight = FontWeight.Bold
                    )
                }
                Button(
                    onClick = {
                        generateTripCode()
                    }
                ) {
                    Text("Generate Trip Code")
                }
                Button(
                    onClick = {
                        showJoinTripDialog = true
                    }
                ) {
                    Text("Join Assigned Trip")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 🟢 WITH THIS DYNAMIC LOOP MATRIX:
                    dynamicAvailableTrips.forEach { tripNum ->
                        FilterChip(
                            selected = selectedTripTab == tripNum,
                            onClick = { selectedTripTab = tripNum },
                            label = { Text("Trip $tripNum", fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }
            Button(
                onClick = {
                    tripStarted = !tripStarted
                    toggleBackgroundLocationTracking()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTrackingActive) AlertRed else SuccessGreen
                )
            ) {
                Text(
                    text = if (tripStarted) "🛑 STOP TRIP" else "🚀 START TRIP",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
            if (!tripStarted) {
                LazyColumn(
                    modifier = Modifier.height(400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(activeRosterList) { passenger ->
                        Card(colors = CardDefaults.cardColors(containerColor = SurfaceGray)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        val pickupTime =
                                            if (isMorningShift) passenger.morningPickupTime else passenger.eveningPickupTime
                                        Text(
                                            text = pickupTime ?: "07:30 AM",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Black,
                                            color = YellowAuto
                                        )
                                        Text(
                                            text = passenger.name ?: "Unknown Candidate",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextMain
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            val activeCoords =
                                                if (isMorningShift) passenger.homeCoordinates else passenger.destinationCoordinates
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

                                // 🟢 FIXED: Removed manual 1km alert. Dials the actual database-stored passenger number directly!
                                Button(
                                    onClick = {
                                        triggerNativeDialerIntent(
                                            context,
                                            passenger.phoneNumber
                                                ?: AppConstants.DEFAULT_PHONE_NUMBER
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        "📞 CALL PARENT CONTACT",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        selectedPassengerForMove = passenger
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("🔄 Move To Another Trip")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedPassengerForMove?.let { passenger ->
        AlertDialog(
            onDismissRequest = {
                selectedPassengerForMove = null
            },
            title = { Text("Move Passenger") },
            text = {
                Column {
                    Text(text = passenger.name ?: "")
                    Spacer(modifier = Modifier.height(12.dp))
                    dynamicAvailableTrips.filter { it != selectedTripTab }.forEach { trip ->
                        Button(
                            onClick = {
                                swapPassengerToTrip(
                                    passengerId = passenger.id ?: "",
                                    destinationTrip = trip
                                )
                                selectedPassengerForMove = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Move To Trip $trip")
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            },
            confirmButton = {}
        )
    }

    generatedAssignmentCode?.let { code ->
        AlertDialog(
            onDismissRequest = { generatedAssignmentCode = null },
            title = { Text("Trip Assignment Code") },
            text = {
                Text(code)
            },
            confirmButton = {
                Row {
                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(
                                Context.CLIPBOARD_SERVICE
                            ) as ClipboardManager
                            clipboard.setPrimaryClip(
                                ClipData.newPlainText(
                                    "Trip Code",
                                    code
                                )
                            )
                            Toast.makeText(context, "Code Copied", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("COPY")
                    }
                    TextButton(
                        onClick = {
                            val shareIntent = Intent(
                                Intent.ACTION_SEND
                            ).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    """Trip Assignment Code $code Open ZIPPO → Join Assigned Trip → Enter this code""".trimIndent()
                                )
                            }
                            context.startActivity(
                                Intent.createChooser(shareIntent, "Share Code")
                            )
                        }
                    ) {
                        Text("SHARE")
                    }
                    TextButton(
                        onClick = {
                            generatedAssignmentCode = null
                        }
                    ) {
                        Text("CLOSE")
                    }
                }
            }
        )
    }
    if (showJoinTripDialog) {
        AlertDialog(
            onDismissRequest = { showJoinTripDialog = false },
            title = { Text("Join Assigned Trip") },
            text = {
                OutlinedTextField(
                    value = assignmentCodeInput,
                    onValueChange = {
                        assignmentCodeInput = it.uppercase()
                    },
                    label = {
                        Text("Assignment Code")
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                val response = apiService.joinTripByCode(
                                    driverId = sessionManager.getDriverId() ?: "",
                                    request = JoinTripByCodeRequest(assignmentCodeInput)
                                )
                                Toast.makeText(
                                    context,
                                    "Joined Trip ${response.tripNumber}",
                                    Toast.LENGTH_LONG
                                ).show()
                                showJoinTripDialog = false
                            } catch (e: Exception) {
                                Toast.makeText(context, "Invalid Code", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("JOIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinTripDialog = false }
                ) {
                    Text("CANCEL")
                }
            }
        )
    }
}

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
        val formattedNumber = phoneNumber.replace("+", "")
        val whatsappIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(
                "https://wa.me/$formattedNumber?text=${
                    URLEncoder.encode(
                        messageText,
                        "UTF-8"
                    )
                }"
            )
        )
        whatsappIntent.setPackage("com.whatsapp")
        context.startActivity(whatsappIntent)
    } catch (e: Exception) {
        Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
    }

}
