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
import com.zippo.app.utils.AppConstants
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.zippo.app.utils.SessionManager
import com.zippo.app.utils.Utils
import com.zippo.app.utils.Utils.formatMonth
import com.zippo.app.utils.Utils.formatPaymentDate
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val apiService = remember { ZippoApiService.create() }
    val sessionManager = remember { SessionManager(context) }
    val uniqueDeviceDriverId = sessionManager.getDriverId() ?: ""
    // 🟢 ADD: Dynamic data state containers
    var isLoading by remember { mutableStateOf(false) }
    var totalCollected by remember { mutableStateOf(0) }
    var pendingDues by remember { mutableStateOf(0) }
    var invoiceList by remember { mutableStateOf<List<LedgerInvoiceResponse>>(emptyList()) }
    var paidCount by remember { mutableStateOf(0) }
    var dueCount by remember { mutableStateOf(0) }
    val currentCalendar = Calendar.getInstance()
    val currentMonth = String.format(
        "%04d-%02d",
        currentCalendar.get(Calendar.YEAR),
        currentCalendar.get(Calendar.MONTH) + 1
    )
    var selectedMonth by remember { mutableStateOf(currentMonth) }
    var months by remember { mutableStateOf(listOf<String>()) }
    var monthDropdownExpanded by remember { mutableStateOf(false) }

    // 🟢 ADD: Cloud synchronizer function
    fun syncFinancialLedgerFromCloud() {
        coroutineScope.launch {
            isLoading = true
            try {
                val response = apiService.fetchFinancialLedger(uniqueDeviceDriverId, selectedMonth)
                if (response.success) {
                    totalCollected = response.totalCollected
                    pendingDues = response.pendingDues
                    invoiceList = response.invoices
                    paidCount = response.paidCount
                    dueCount = response.dueCount
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
        coroutineScope.launch {
            try {
                months =
                    apiService.fetchAvailableMonths(
                        uniqueDeviceDriverId
                    )
                if (months.isNotEmpty()) {
                    selectedMonth = months.first()
                }
                syncFinancialLedgerFromCloud()
            } catch (e: Exception) {
                Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
            }
        }
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
//        floatingActionButton = {
//            FloatingActionButton(
//                onClick = { triggerHardwareVoiceCapture() }, // 🟢 UPDATE: Replaces the placeholder toast notice
//                containerColor = YellowAuto,
//                contentColor = Color.Black,
//                shape = CircleShape,
//                modifier = Modifier.size(72.dp)
//            ) {
//                Text("🎙️", fontSize = 28.sp)
//            }
//        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = monthDropdownExpanded,
                onExpandedChange = {
                    monthDropdownExpanded =
                        !monthDropdownExpanded
                }
            ) {
                OutlinedTextField(
                    value = formatMonth(selectedMonth),
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    label = {
                        Text("Ledger Month")
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                ExposedDropdownMenu(
                    expanded = monthDropdownExpanded,
                    onDismissRequest = {
                        monthDropdownExpanded = false
                    }
                ) {
                    months.forEach { month ->
                        DropdownMenuItem(
                            text = {
                                Text(formatMonth(month))
                            },
                            onClick = {
                                selectedMonth = month
                                monthDropdownExpanded = false
                                syncFinancialLedgerFromCloud()
                            }
                        )
                    }
                }
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceGray),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 10.dp)
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
                            fontSize = 22.sp,
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
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = AlertRed
                        )
                    }
                }

                Divider(
                    color = Color.Gray.copy(alpha = 0.3f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = SuccessGreen.copy(alpha = 0.15f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Text(
                                "PAID",
                                fontSize = 11.sp,
                                color = Color.LightGray
                            )
                            Text(
                                "✅ $paidCount",
                                color = SuccessGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = AlertRed.copy(alpha = 0.15f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Text(
                                "PENDING",
                                fontSize = 11.sp,
                                color = Color.LightGray
                            )
                            Text(
                                "🔴 $dueCount",
                                color = AlertRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }
                }
            }
            Text(
                "Passenger:",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold
            )
            if (invoiceList.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = SurfaceGray
                    )
                ) {
                    Text(
                        text = "No invoices found for ${formatMonth(selectedMonth)}",
                        modifier = Modifier.padding(16.dp),
                        color = Color.White
                    )
                }
                return@Column
            }

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
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val passengerBitmap =
                                        Utils.decodeBase64ToBitmap(
                                            invoice.profileImageBase64
                                        )
                                    if (passengerBitmap != null) {
                                        Image(
                                            bitmap = passengerBitmap.asImageBitmap(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(CircleShape)
                                                .border(
                                                    2.dp,
                                                    YellowAuto,
                                                    CircleShape
                                                )
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(CircleShape)
                                                .background(Color.DarkGray),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "👤",
                                                fontSize = 24.sp
                                            )
                                        }
                                    }
                                    Spacer(
                                        modifier = Modifier.width(12.dp)
                                    )
                                    Column {
                                        Text(
                                            text = invoice.householdName,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextMain
                                        )
                                        Text(
                                            text = invoice.description,
                                            fontSize = 11.sp,
                                            color = Color.LightGray
                                        )
                                        Spacer(
                                            modifier = Modifier.height(4.dp)
                                        )
                                        Text(
                                            text = "₹${invoice.fareAmount}",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Black,
                                            color = YellowAuto
                                        )
                                    }
                                }
                                Column(
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Surface(
                                        color =
                                        if (invoice.isSettled)
                                            SuccessGreen.copy(alpha = 0.15f)
                                        else
                                            AlertRed.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text =
                                            if (invoice.isSettled)
                                                "PAID"
                                            else
                                                "DUE",
                                            color =
                                            if (invoice.isSettled)
                                                SuccessGreen
                                            else
                                                AlertRed,
                                            modifier = Modifier.padding(
                                                horizontal = 10.dp,
                                                vertical = 6.dp
                                            ),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                    if (
                                        invoice.isSettled &&
                                        invoice.paymentDate != null
                                    ) {
                                        Spacer(
                                            modifier = Modifier.height(4.dp)
                                        )
                                        Text(
                                            text = formatPaymentDate(
                                                invoice.paymentDate
                                            ),
                                            color = Color.LightGray,
                                            fontSize = 10.sp
                                        )
                                    }
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
                                            .height(42.dp)
                                    ) {
                                        Text(
                                            "💵 MARK CASH",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            val reminderMessage =
                                                "Reminder: ₹${invoice.fareAmount} transport fee for ${invoice.householdName} is pending. Kindly clear the dues at your earliest convenience. Thank you. - Zippo"
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
                                            .height(42.dp)
                                    ) {
                                        Text(
                                            "📱 REMIND UPI",
                                            fontSize = 12.sp,
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

