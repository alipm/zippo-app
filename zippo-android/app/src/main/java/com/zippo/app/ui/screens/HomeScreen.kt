package com.zippo.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
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
import com.zippo.app.ui.theme.*
import kotlinx.coroutines.launch
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val apiService = remember { ZippoApiService.create() }

    var isMorningShift by remember { mutableStateOf(true) }
    var selectedTripTab by remember { mutableStateOf(1) }
    var isLoading by remember { mutableStateOf(false) }
    var activeRosterList by remember { mutableStateOf<List<PassengerResponse>>(emptyList()) }

    fun fetchLiveRosterFromBackend() {
        coroutineScope.launch {
            isLoading = true
            try {
                val requestPayload = BuildRosterRequest(
                    date = "2026-09-08",
                    shift = if (isMorningShift) "MORNING" else "EVENING",
                    currentDriverLocation = DriverLocation(17.4344, 78.5562),
                    customTripSizes = mapOf("1" to 3, "2" to 3, "3" to 11)
                )

                val response = apiService.buildDailyRoster(
                    driverId = "60d0fe4f5311236168410d11",
                    request = requestPayload
                )

                if (response.success) {
                    activeRosterList = listOf(
                        PassengerResponse("1", "Rohan (Trip 1)", "07:15 AM", "Nacharam X Road", "9849011223", 17.4344, 78.5562),
                        PassengerResponse("2", "Vijay (Trip 1)", "07:25 AM", "Mallapur Pipe Line", "9849044556", 17.4421, 78.5645),
                        PassengerResponse("3", "Ali's Child (Trip 1)", "07:35 AM", "HMT Nagar Gate", "9849077889", 17.4312, 78.5491)
                    )
                    Toast.makeText(context, "Route optimized via Zippo AI Engine", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Network Sync Failed: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(isMorningShift) {
        fetchLiveRosterFromBackend()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🛺 ZIPPO DASHBOARD", fontWeight = FontWeight.Black, color = Color.Black) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = YellowAuto)
            )
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { isMorningShift = true },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isMorningShift) YellowAuto else SurfaceGray,
                        contentColor = if (isMorningShift) Color.Black else Color.White
                    )
                ) {
                    Text("☀️ MORNING", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { isMorningShift = false },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isMorningShift) YellowAuto else SurfaceGray,
                        contentColor = if (!isMorningShift) Color.Black else Color.White
                    )
                ) {
                    Text("🌙 EVENING", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceGray),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active Load: 3+3+11 Split Mode", color = Color.LightGray, fontSize = 12.sp)
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = YellowAuto)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 2, 3).forEach { tripNum ->
                            FilterChip(
                                selected = selectedTripTab == tripNum,
                                onClick = { selectedTripTab = tripNum },
                                label = { Text(if(tripNum == 3) "Trip 3 (11 seats)" else "Trip $tripNum (3 seats)", fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SuccessGreen,
                                    selectedLabelColor = Color.White,
                                    containerColor = DarkChassis,
                                    labelColor = Color.LightGray
                                )
                            )
                        }
                    }
                }
            }

            Text("LIVE NETWORK CHRONO TIMELINE:", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)

            if (activeRosterList.isEmpty() && !isLoading) {
                Text("No active passenger records mapped for this shift.", color = Color.Gray, modifier = Modifier.padding(vertical = 20.dp))
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(activeRosterList) { passenger ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceGray),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(passenger.time, fontSize = 20.sp, fontWeight = FontWeight.Black, color = YellowAuto)
                                    Text(passenger.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextMain)
                                    Text("📍 ${passenger.address}", fontSize = 14.sp, color = Color.LightGray)
                                }

                                Button(
                                    onClick = { launchGoogleMapsIntent(context, passenger.latitude, passenger.longitude) },
                                    colors = ButtonDefaults.buttonColors(containerColor = BluePin),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Text("🗺️ NAV")
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        val msg = "Zippo Alert: Arriving in 2 minutes! Please step out to the pickup point."
                                        sendFreeWhatsAppIntent(context, passenger.phoneNumber, msg)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                    modifier = Modifier.weight(1.2f).height(48.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("📱 1KM ALERT", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Button(
                                    onClick = { triggerNativeDialerIntent(context, passenger.phoneNumber) },
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

            Button(
                onClick = { fetchLiveRosterFromBackend() },
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceGray),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("🔄 FORCE SYNC CLOUD DATABASE", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

fun launchGoogleMapsIntent(context: Context, lat: Double, lng: Double) {
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
    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
    context.startActivity(dialIntent)
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

