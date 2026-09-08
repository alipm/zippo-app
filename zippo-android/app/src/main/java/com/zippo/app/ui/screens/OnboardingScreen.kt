package com.zippo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zippo.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen() {
    var passengerName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var fixedFare by remember { mutableStateOf("") }
    var contractSelection by remember { mutableStateOf("BOTH") } // "BOTH", "MORNING_ONLY", "EVENING_ONLY"
    var tripCapacityLimit by remember { mutableStateOf(5) } // Driver manual top-level setting baseline

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkChassis)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "➕ CLIENT REGISTRATION SYSTEM",
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = TextMain
        )

        // 1. CHARACTER INPUT TEXT FIELDS
        OutlinedTextField(
            value = passengerName,
            onValueChange = { passengerName = it },
            label = { Text("Passenger / Candidate Full Name") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = YellowAuto,
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = YellowAuto,
                unfocusedLabelColor = Color.LightGray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = contactPhone,
            onValueChange = { contactPhone = it },
            label = { Text("Primary Contact Phone Number") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = YellowAuto,
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = YellowAuto,
                unfocusedLabelColor = Color.LightGray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = fixedFare,
            onValueChange = { fixedFare = it },
            label = { Text("Monthly Subscription Fare (₹)") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = YellowAuto,
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = YellowAuto,
                unfocusedLabelColor = Color.LightGray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // 2. MULTI-TIER SHIFT CONTRACT RADIOS
        Text(
            text = "Contract Boundary Shift Scope:",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("BOTH", "MORNING_ONLY", "EVENING_ONLY").forEach { tier ->
                FilterChip(
                    selected = contractSelection == tier,
                    onClick = { contractSelection = tier },
                    label = { Text(tier, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = YellowAuto,
                        selectedLabelColor = Color.Black,
                        containerColor = SurfaceGray,
                        labelColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        HorizontalDivider(color = Color.DarkGray, thickness = 1.dp)

        // 3. HARDWARE-LINKED GPS COORDINATE CAPTURE COMMANDS
        Text(
            text = "Hardware GPS Pin Registry:",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "* Drive and stand physically at the target gate coordinate point before capturing.",
            color = Color.LightGray,
            fontSize = 11.sp
        )

        Button(
            onClick = { /* Invokes fused location provider hardware locks */ },
            colors = ButtonDefaults.buttonColors(containerColor = BluePin),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("🏠 CAPTURE HARDWARE GPS HOME PIN", fontWeight = FontWeight.ExtraBold)
        }

        Button(
            onClick = { /* Invokes fused location provider hardware locks */ },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF6C00)), // Custom Orange Hub Accent Color
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("🏫 CAPTURE HARDWARE GPS HUB PIN", fontWeight = FontWeight.ExtraBold)
        }

        HorizontalDivider(color = Color.DarkGray, thickness = 1.dp)

        // 4. ONE-TIME VEHICLE CAPACITY CONFIGURATION ROW MODIFIER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("👥 Global Seating Capacity", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Max limit threshold before AI splits shift feeds.", color = Color.LightGray, fontSize = 11.sp)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                IconButton(
                    onClick = { if (tripCapacityLimit > 1) tripCapacityLimit-- },
                    modifier = Modifier.background(SurfaceGray, CircleShape).size(36.dp)
                ) {
                    Text("-", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                }

                Text(
                    text = tripCapacityLimit.toString(),
                    color = YellowAuto,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )

                IconButton(
                    onClick = { tripCapacityLimit++ },
                    modifier = Modifier.background(SurfaceGray, CircleShape).size(36.dp)
                ) {
                    Text("+", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // 5. MASTER DATA SUBMIT STRATEGY CONTROL
        Button(
            onClick = { /* Packs onboarding DTO payload and ships directly up to NestJS /trips endpoints */ },
            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("💾 SAVE CUSTOMER PROFILE DATA", fontSize = 16.sp, fontWeight = FontWeight.Black)
        }
    }
}
