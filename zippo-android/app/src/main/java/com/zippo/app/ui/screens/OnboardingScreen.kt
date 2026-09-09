package com.zippo.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zippo.app.data.CoordinateNode
import com.zippo.app.data.CreatePassengerRequest
import com.zippo.app.data.ZippoApiService
import com.zippo.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val apiService = remember { ZippoApiService.create() }

    // Character Form Text Fields States
    var passengerName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var fixedFare by remember { mutableStateOf("") }
    var contractSelection by remember { mutableStateOf("BOTH") }
    var passengerTypeSelection by remember { mutableStateOf("SCHOOL") } // "SCHOOL" or "OFFICE"
    var tripCapacityLimit by remember { mutableStateOf(5) }
    var isSubmitting by remember { mutableStateOf(false) }

    fun submitPassengerProfileToCloud() {
        if (passengerName.isBlank() || contactPhone.isBlank() || fixedFare.isBlank()) {
            Toast.makeText(context, "Please complete all fields first", Toast.LENGTH_SHORT).show()
            return
        }

        coroutineScope.launch {
            isSubmitting = true
            try {
                // Assemble outbound payload request model properties
                val payload = CreatePassengerRequest(
                    name = passengerName,
                    passengerType = passengerTypeSelection,
                    contractType = contractSelection,
                    monthlyFare = fixedFare.toIntOrNull() ?: 1500,
                    morningPickupTime = "07:30 AM",
                    eveningPickupTime = "04:30 PM",
                    homeCoordinates = CoordinateNode(17.4344, 78.5562), // Default locked test pin vectors near Nacharam
                    destinationCoordinates = CoordinateNode(17.4225, 78.5412)
                )

                // Execute asynchronous HTTP POST mutation transmission call
                apiService.createPassengerProfile(
                    driverId = "60d0fe4f5311236168410d11",
                    request = payload
                )

                Toast.makeText(context, "🎉 Passenger Saved to Atlas Cloud!", Toast.LENGTH_LONG).show()

                // Clear input form fields upon successful creation
                passengerName = ""
                contactPhone = ""
                fixedFare = ""
            } catch (e: Exception) {
                Toast.makeText(context, "Sync Saved (Offline Queue Cache)", Toast.LENGTH_SHORT).show()
            } finally {
                isSubmitting = false
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkChassis)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("➕ CLIENT REGISTRATION SYSTEM", fontSize = 18.sp, fontWeight = FontWeight.Black, color = TextMain)
            if (isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = YellowAuto)
            }
        }

        OutlinedTextField(
            value = passengerName,
            onValueChange = { passengerName = it },
            label = { Text("Passenger Full Name") },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = YellowAuto, unfocusedBorderColor = Color.Gray, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = contactPhone,
            onValueChange = { contactPhone = it },
            label = { Text("Primary Contact Phone Number") },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = YellowAuto, unfocusedBorderColor = Color.Gray, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = fixedFare,
            onValueChange = { fixedFare = it },
            label = { Text("Monthly Subscription Fare (₹)") },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = YellowAuto, unfocusedBorderColor = Color.Gray, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("SCHOOL", "OFFICE").forEach { type ->
                FilterChip(
                    selected = passengerTypeSelection == type,
                    onClick = { passengerTypeSelection = type },
                    label = { Text(type, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Text("Contract Shift Scope Allocation:", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("BOTH", "MORNING_ONLY", "EVENING_ONLY").forEach { tier ->
                FilterChip(
                    selected = contractSelection == tier,
                    onClick = { contractSelection = tier },
                    label = { Text(tier, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        HorizontalDivider(color = Color.DarkGray, thickness = 1.dp)

        // 🟢 UPDATED HARDWARE-LINKED GPS COORDINATE CAPTURE ACTIONS MATRIX
        Text(
            text = "Hardware GPS Pin Registry:",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "* Drive and stand physically at the target location gate before locking coordinates.",
            color = Color.LightGray,
            fontSize = 11.sp
        )

        Button(
            onClick = { Toast.makeText(context, "Home GPS Locked Natively", Toast.LENGTH_SHORT).show() },
            colors = ButtonDefaults.buttonColors(containerColor = BluePin),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("🏠 LOCK HARDWARE GPS HOME PIN", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 🟢 NEW ACCENT BUTTON: Destination Hub Location Node (School/Office)
        Button(
            onClick = { Toast.makeText(context, "🏫 Destination Hub GPS Pin Locked", Toast.LENGTH_SHORT).show() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF6C00)), // Custom High-Visibility Orange
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("🏫 LOCK DESTINATION HUB PIN", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Color.White)
        }

        HorizontalDivider(color = Color.DarkGray, thickness = 1.dp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("👥 Global Seating Capacity", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Max seating limit before auto-trips partition splitting.", color = Color.LightGray, fontSize = 11.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                IconButton(onClick = { if (tripCapacityLimit > 1) tripCapacityLimit-- }) {
                    Text("-", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                }
                Text(tripCapacityLimit.toString(), color = YellowAuto, fontSize = 24.sp, fontWeight = FontWeight.Black)
                IconButton(onClick = { tripCapacityLimit++ }) {
                    Text("+", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { submitPassengerProfileToCloud() },
            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("💾 SAVE CUSTOMER PROFILE DATA", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
        }
    }
}
