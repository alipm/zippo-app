package com.zippo.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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

data class LedgerInvoiceItem(
    val id: String,
    val householdName: String, // Tracks parent groups or individual corporate employees
    val description: String,
    val fareAmount: Int,
    val isSettled: Boolean,
    val parentPhoneNumber: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen() {
    val context = LocalContext.current

    // Mock dataset showing household grouping aggregation (siblings under one parent billing profile)
    val sampleInvoices = listOf(
        LedgerInvoiceItem("1", "Ali's Household (2 Siblings)", "School Route - Both Shifts", 3000, false, "9849011223"),
        LedgerInvoiceItem("2", "Vijay Kumar", "School Route - Evening Only Contract", 1200, true, "9849044556"),
        LedgerInvoiceItem("3", "Suresh (Hitech City Block)", "Office Route - Both Shifts", 2000, false, "9849077889")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📊 FINANCIAL BAHI-KHATA", fontWeight = FontWeight.Black, color = Color.Black) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = YellowAuto)
            )
        },
        containerColor = DarkChassis,
        floatingActionButton = {
            // HANDS-FREE VOICE ENTRY INTERFACE TRIGGER (Large 72dp target box layout element)
            FloatingActionButton(
                onClick = {
                    // This action anchor triggers speech recording frames to pass raw transcripts to NestJS server /ai/voice-khata
                    val voiceMsg = "AutoRasta System Notice: Voice logging activated. Speak collection data now..."
                    sendFreeWhatsAppIntent(context, "919849011223", voiceMsg)
                },
                containerColor = YellowAuto,
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier.size(72.dp)
            ) {
                Text("🎙️", fontSize = 28.sp)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // 1. CONSOLIDATED REVENUE METRICS SUMMARY BANNER CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceGray),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("TOTAL COLLECTED", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("₹18,500", fontSize = 24.sp, fontWeight = FontWeight.Black, color = SuccessGreen)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("PENDING DUES", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("₹5,000", fontSize = 24.sp, fontWeight = FontWeight.Black, color = AlertRed)
                    }
                }
            }

            Text("SUBSCRIPTION ENTITY RECORDS:", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)

            // 2. DYNAMIC SCROLLABLE INVOICING LIST FEED
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(sampleInvoices) { invoice ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceGray),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(invoice.householdName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextMain)
                                    Text(invoice.description, fontSize = 13.sp, color = Color.LightGray)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Fare Target: ₹${invoice.fareAmount}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = YellowAuto)
                                }

                                // High Contrast State Pills (Paid vs Due)
                                Surface(
                                    color = if (invoice.isSettled) SuccessGreen.copy(alpha = 0.2f) else AlertRed.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (invoice.isSettled) "🟢 PAID" else "🔴 DUE",
                                        color = if (invoice.isSettled) SuccessGreen else AlertRed,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            // 3. MANUAL ZERO-COST WORKFLOW BUTTON CONTROLS
                            if (!invoice.isSettled) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { /* Will execute local Room DB state flip and patch to MongoDBAtlas */ },
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1.4f).height(48.dp)
                                    ) {
                                        Text("💵 MARK CASH", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = {
                                            // Manual out-of-band UPI reminder draft via free personal WhatsApp package channel link
                                            val reminderMessage = "Zippo Subscription Notice: Monthly transport fare balance of ₹${invoice.fareAmount} is outstanding for ${invoice.householdName}. Please clear using your preferred banking UPI app."
                                            sendFreeWhatsAppIntent(context, invoice.parentPhoneNumber, reminderMessage)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).height(48.dp)
                                    ) {
                                        Text("📱 REMIND UPI", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
