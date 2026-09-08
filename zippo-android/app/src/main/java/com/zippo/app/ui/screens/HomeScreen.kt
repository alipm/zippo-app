package com.zippo.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import com.zippo.app.ui.theme.*
import java.net.URLEncoder

data class PassengerItem(
    val id: String,
    val name: String,
    val time: String,
    val address: String,
    val phoneNumber: String,
    val latitude: Double,
    val longitude: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    var isMorningShift by remember { mutableStateOf(true) }
    var selectedTripTab by remember { mutableStateOf(1) }

    // Simulated Active Batch Sorted by the NestJS AI Agent
    // Demonstrating the 3 + 3 + 11 uneven capacity balancing scenario
    val sampleTrip1Passengers = listOf(
        PassengerItem("1", "Ali's Child", "07:15 AM", "Nacharam X Road", "9849011223", 17.4344, 78.5562),
        PassengerItem("2", "Vijay Kumar", "07:25 AM", "Mallapur Pipe Line", "9849044556", 17.4421, 78.5645),
        PassengerItem("3", "Rohan", "07:35 AM", "HMT Nagar Gate", "9849077889", 17.4312, 78.5491)
    )

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
            // 1. SHIFT FILTER TOGGLE ROW (Oversized touch targets for driving comfort)
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

            // 2. CAPACITY & DYNAMIC MULTI-TRIP SPLITTING TABS
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceGray),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                    Text("Active Load Balancing Matrix Mode", color = Color.LightGray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 2, 3).forEach { tripNum ->
                            FilterChip(
                                selected = selectedTripTab == tripNum,
                                onClick = { selectedTripTab = tripNum },
                                label = { Text("Trip $tripNum", fontWeight = FontWeight.Bold) },
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

            // 3. CHRONOLOGICAL ROUTE TIMELINE FEED
            Text("OPTIMIZED PICKUP TIMELINE:", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sampleTrip1Passengers) { passenger ->
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

                                // Native Intent Maps Launch Link
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

                            // Driver Operational Action Target Buttons
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

            // 4. SERVER-SIDE TRAFFIC BROADCST WEBHOOK SYSTEM TRIGGER
            Button(
                onClick = { /* Will issue background API network request to NestJS server */ },
                colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("⚠️ BROADCAST TRAFFIC DELAY (10 MINS)", fontSize = 15.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

// ================= NATIVE ANDROID SYSTEM INTENTS (100% FREE) =================

fun launchGoogleMapsIntent(context: Context, lat: Double, lng: Double) {
    try {
        val mapsUri = Uri.parse("google.navigation:q=$lat,$lng&mode=d")
        val mapIntent = Intent(Intent.ACTION_VIEW, mapsUri).apply {
            setPackage("com.google.android.apps.maps")
        }
        context.startActivity(mapIntent)
    } catch (e: Exception) {
        // Fallback: If Google Maps app is missing, opens in web browser container
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
        // Formats the Indian mobile country code node (+91) parameter correctly
        val formattedNumber = if (!phoneNumber.startsWith("91")) "91$phoneNumber" else phoneNumber
        val urlEncodedMessage = URLEncoder.encode(messageText, "UTF-8")
        val whatsappUri = Uri.parse("https://whatsapp.com")

        val chatIntent = Intent(Intent.ACTION_VIEW, whatsappUri).apply {
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chatIntent)
    } catch (e: Exception) {
        // Fallback Matrix: If WhatsApp app container is missing, launch standard system SMS view
        val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phoneNumber")).apply {
            putExtra("sms_body", messageText)
        }
        context.startActivity(smsIntent)
    }
}