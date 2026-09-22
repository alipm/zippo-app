package com.zippo.app.ui.screens

// 🟢 ADD these imports to the top of LedgerScreen.kt
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.zippo.app.data.LedgerInvoiceResponse
import com.zippo.app.data.UpdatePaymentStatusRequest
import com.zippo.app.data.VoiceKhataRequest
import com.zippo.app.data.ZippoApiService
import com.zippo.app.ui.theme.*
import kotlinx.coroutines.launch
import com.zippo.app.utils.AppConstants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val apiService = remember { ZippoApiService.create() }
    val uniqueDeviceDriverId = AppConstants.DEMO_DRIVER_ID
    // 🟢 ADD: Dynamic data state containers
    var isLoading by remember { mutableStateOf(false) }
    var totalCollected by remember { mutableStateOf(0) }
    var pendingDues by remember { mutableStateOf(0) }
    var invoiceList by remember { mutableStateOf<List<LedgerInvoiceResponse>>(emptyList()) }

    // 🟢 ADD: Cloud synchronizer function
    fun syncFinancialLedgerFromCloud() {
        coroutineScope.launch {
            isLoading = true
            try {
                val response = apiService.fetchFinancialLedger(uniqueDeviceDriverId)
                if (response.success) {
                    totalCollected = response.totalCollected
                    pendingDues = response.pendingDues
                    invoiceList = response.invoices
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Khata Sync Failed: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            } finally {
                isLoading = false
            }
        }
    }

    // 🟢 ADD: Payment execution mutation worker
    fun executePaymentSettlement(invoiceId: String, method: String) {
        coroutineScope.launch {
            try {
                apiService.clearInvoiceBalance(
                    driverId = uniqueDeviceDriverId,
                    request = UpdatePaymentStatusRequest(invoiceId, method)
                )
                Toast.makeText(context, "Payment Cleared Successfully!", Toast.LENGTH_SHORT).show()
                // 🟢 FIXED: Explicitly triggers an instant cloud re-sync to visually change state from DUE to PAID on the screen!
                syncFinancialLedgerFromCloud()
            } catch (e: Exception) {
                Toast.makeText(context, "Settlement failed: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    // 🟢 ADD: Native Speech Recognition Launcher Target
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data: Intent? = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.firstOrNull() ?: ""

            if (spokenText.isNotBlank()) {
                Toast.makeText(context, "Voice Heard: \"$spokenText\"", Toast.LENGTH_LONG).show()

                // 🚀 This text transcript can now be passed directly over the network transport
                // pipeline straight to your NestJS server endpoint: /ai/voice-khata
                coroutineScope.launch {
                    try {
                        // 🟢 ADD: Execute asynchronous HTTP POST transmission call to the NestJS AI Agent
                        val aiResponse =
                            apiService.processVoiceKhataEntry(VoiceKhataRequest(spokenText))

                        if (aiResponse.success) {
                            val entityData = aiResponse.data
                            // Pop an explicit high-visibility confirmation status dialog detailing extracted fields
                            Toast.makeText(
                                context,
                                "🤖 AI Logged: ${aiResponse.message}",
                                Toast.LENGTH_LONG
                            ).show()

                            // Re-sync your financial ledger list cards immediately to balance updated data metrics
                            syncFinancialLedgerFromCloud()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "AI Sync Error: ${e.message}", Toast.LENGTH_SHORT)
                            .show()
                    }
                }

            }
        }
    }

    // Helper utility method to prompt the standard hardware recording container overlay
    fun triggerHardwareVoiceCapture() {
        val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                "en-IN"
            ) // Mapped directly for Hyderabadi localized phrasing configurations
            putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                "Speak payment data clearly (e.g., Ali paid 1500 upi)"
            )
        }
        try {
            speechRecognizerLauncher.launch(speechIntent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Speech Engine missing on this device framework",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // 🟢 ADD: Lifecycle interceptor to trigger loading on screen mount
    LaunchedEffect(Unit) {
        syncFinancialLedgerFromCloud()
    }

    Scaffold(
        // 🟢 FIXED: Compressed financial top bar dimensions to maximize listing view visibility
        topBar = {
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
                        text = "Money Ledger",
                        fontWeight = FontWeight.Black,
                        color = Color.Black,
                        fontSize = 18.sp
                    )
                }
            }
        },
        containerColor = DarkChassis,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { triggerHardwareVoiceCapture() }, // 🟢 UPDATE: Replaces the placeholder toast notice
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
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceGray),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "TOTAL COLLECTED",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "₹$totalCollected",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = SuccessGreen
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "PENDING DUES",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "₹$pendingDues",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = AlertRed
                        )
                    }
                }
            }

            Text(
                "SUBSCRIPTION INVOICING RECORDS:",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(invoiceList) { invoice ->
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
                                    Text(
                                        invoice.householdName,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextMain
                                    )
                                    Text(
                                        invoice.description,
                                        fontSize = 13.sp,
                                        color = Color.LightGray
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Fare Target: ₹${invoice.fareAmount}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        color = YellowAuto
                                    )
                                }
                                Surface(
                                    color = if (invoice.isSettled) SuccessGreen.copy(alpha = 0.2f) else AlertRed.copy(
                                        alpha = 0.2f
                                    ),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (invoice.isSettled) "🟢 PAID" else "🔴 DUE",
                                        color = if (invoice.isSettled) SuccessGreen else AlertRed,
                                        modifier = Modifier.padding(
                                            horizontal = 8.dp,
                                            vertical = 4.dp
                                        ),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            if (!invoice.isSettled) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { executePaymentSettlement(invoice.id, "CASH") },
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .weight(1.4f)
                                            .height(48.dp)
                                    ) {
                                        Text(
                                            "💵 MARK CASH",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            val reminderMessage =
                                                "Zippo Notice: Monthly subscription transport fare balance of ₹${invoice.fareAmount} is outstanding for ${invoice.householdName}. Kindly clear via your banking app."
                                            sendFreeWhatsAppIntent(
                                                context,
                                                invoice.parentPhoneNumber,
                                                reminderMessage
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                    ) {
                                        Text(
                                            "📱 REMIND UPI",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
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

