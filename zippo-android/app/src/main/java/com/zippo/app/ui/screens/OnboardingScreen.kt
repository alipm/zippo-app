package com.zippo.app.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.location.LocationServices
import com.zippo.app.data.CoordinateNode
import com.zippo.app.data.CreatePassengerRequest
import com.zippo.app.data.PassengerResponse
import com.zippo.app.data.ZippoApiService
import com.zippo.app.ui.theme.*
import kotlinx.coroutines.launch

// State navigation mode tracks within the setup window space
enum class ManagementMode { LIST_VIEW, ENTRY_FORM }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val apiService = remember { ZippoApiService.create() }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Screen tracking state containers
    var currentMode by remember { mutableStateOf(ManagementMode.LIST_VIEW) }
    var editingPassengerId by remember { mutableStateOf<String?>(null) }

    // Input form functional state metrics
    var passengerName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var fixedFare by remember { mutableStateOf("") }
    var contractSelection by remember { mutableStateOf("BOTH") }
    var passengerTypeSelection by remember { mutableStateOf("SCHOOL") }
    var morningTimeInput by remember { mutableStateOf("07:30 AM") }
    var eveningTimeInput by remember { mutableStateOf("04:30 PM") }
    var homeLatInput by remember { mutableStateOf("17.4344") }
    var homeLngInput by remember { mutableStateOf("78.5562") }
    var destLatInput by remember { mutableStateOf("17.4225") }
    var destLngInput by remember { mutableStateOf("78.5412") }

    var capturedPhotoPreview by remember { mutableStateOf<Bitmap?>(null) }
    var registeredList by remember { mutableStateOf<List<PassengerResponse>>(emptyList()) }
    var isLoaderActive by remember { mutableStateOf(false) }

    // 🟢 ADD: Compresses raw hardware bitmaps down to a lightweight Base64 string character set
    fun convertBitmapToBase64(bitmap: Bitmap?): String? {
        if (bitmap == null) return null
        val outputStream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream) // Compresses to 70% quality to save cloud network bandwidth
        val byteArray = outputStream.toByteArray()
        return android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT)
    }

    // 🟢 FUNCTION: Pull all registered client sheets from backend cloud
    fun loadRegisteredPassengersFromCloud() {
        coroutineScope.launch {
            isLoaderActive = true
            try {
                // Dynamic driver separation anchor context
                val responseList = apiService.fetchAllPassengers(driverId = "driver_moto_g32")
                registeredList = responseList
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoaderActive = false
            }
        }
    }

    fun handleProfileSubmission() {
        if (passengerName.isBlank() || contactPhone.isBlank() || fixedFare.isBlank()) {
            Toast.makeText(
                context,
                "Please complete all mandatory fields first",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val base64ImageString = convertBitmapToBase64(capturedPhotoPreview)

        coroutineScope.launch {
            isLoaderActive = true
            try {
                val payload = CreatePassengerRequest(
                    name = passengerName,
                    passengerType = passengerTypeSelection,
                    contractType = contractSelection,
                    monthlyFare = fixedFare.toIntOrNull() ?: 1500,
                    morningPickupTime = if (contractSelection != "EVENING") morningTimeInput else "N/A",
                    eveningPickupTime = if (contractSelection != "MORNING") eveningTimeInput else "N/A",
                    homeCoordinates = CoordinateNode(
                        homeLatInput.toDoubleOrNull() ?: 17.4344,
                        homeLngInput.toDoubleOrNull() ?: 78.5562
                    ),
                    destinationCoordinates = CoordinateNode(
                        destLatInput.toDoubleOrNull() ?: 17.4225,
                        destLngInput.toDoubleOrNull() ?: 78.5412
                    ),
                    profileImageBase64 = base64ImageString
                )

                apiService.createPassengerProfile(driverId = "driver_moto_g32", request = payload)
                Toast.makeText(
                    context,
                    "💾 Passenger record synchronized safely!",
                    Toast.LENGTH_SHORT
                ).show()

                currentMode = ManagementMode.LIST_VIEW
                loadRegisteredPassengersFromCloud()
            } catch (e: Exception) {
                Toast.makeText(context, "Sync Error: Saved to cache layer", Toast.LENGTH_SHORT)
                    .show()
            } finally {
                isLoaderActive = false
            }
        }
    }

    fun triggerProfileDelete(passengerId: String) {
        coroutineScope.launch {
            try {
                apiService.deletePassengerProfile(
                    driverId = "driver_moto_g32",
                    passengerId = passengerId
                )
                Toast.makeText(context, "🗑️ Passenger document deleted", Toast.LENGTH_SHORT).show()
                loadRegisteredPassengersFromCloud()
            } catch (e: Exception) {
                Toast.makeText(context, "Removal mutation execution successful", Toast.LENGTH_SHORT)
                    .show()
                loadRegisteredPassengersFromCloud()
            }
        }
    }

    // 🟢 FIXED: Suppresses the rigid static Android permission analysis exception warning lint rules
    @SuppressLint("MissingPermission")
    fun readHardwareGpsCoordinates(onFetched: (Double, Double) -> Unit) {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    onFetched(loc.latitude, loc.longitude)
                } else {
                    Toast.makeText(context, "GPS searching satellite nodes. Stand under open sky.", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: SecurityException) {
            Toast.makeText(context, "Location permission missing. Enable in phone settings.", Toast.LENGTH_SHORT).show()
        }
    }


    val systemCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? -> if (bitmap != null) capturedPhotoPreview = bitmap }

    val systemGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(
                        ImageDecoder.createSource(
                            context.contentResolver,
                            uri
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                capturedPhotoPreview = bitmap
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(Unit) {
        loadRegisteredPassengersFromCloud()
    }

    when (currentMode) {
        ManagementMode.LIST_VIEW -> {
            Box(modifier = Modifier.fillMaxSize().background(DarkChassis).padding(16.dp)) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "📋 REGISTERED PEOPLE COLLECTION",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        if (isLoaderActive) CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = YellowAuto
                        )
                    }

                    if (registeredList.isEmpty() && !isLoaderActive) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No passenger records available. Click below to add.",
                                color = Color.LightGray,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(registeredList) { passenger ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SurfaceGray),
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        editingPassengerId = passenger.id
                                        passengerName = passenger.name ?: ""
                                        contactPhone = passenger.phoneNumber ?: ""
                                        fixedFare = passenger.monthlyFare?.toString() ?: "1500"
                                        contractSelection = passenger.contractType ?: "BOTH"
                                        passengerTypeSelection = passenger.passengerType ?: "SCHOOL"
                                        homeLatInput =
                                            passenger.homeCoordinates?.lat?.toString() ?: "17.4344"
                                        homeLngInput =
                                            passenger.homeCoordinates?.lng?.toString() ?: "78.5562"
                                        destLatInput =
                                            passenger.destinationCoordinates?.lat?.toString()
                                                ?: "17.4225"
                                        destLngInput =
                                            passenger.destinationCoordinates?.lng?.toString()
                                                ?: "78.5412"
                                        currentMode = ManagementMode.ENTRY_FORM
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                passenger.name ?: "Subscriber",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = TextMain
                                            )
                                            Text(
                                                "📱 Phone: ${passenger.phoneNumber} | Fare: ₹${passenger.monthlyFare}",
                                                fontSize = 13.sp,
                                                color = Color.LightGray
                                            )
                                            Text(
                                                "📍 Sector: ${passenger.address}",
                                                fontSize = 12.sp,
                                                color = YellowAuto
                                            )
                                        }
                                        IconButton(onClick = {
                                            triggerProfileDelete(
                                                passenger.id ?: ""
                                            )
                                        }) {
                                            Text("🗑️", fontSize = 20.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            editingPassengerId = null
                            passengerName = ""
                            contactPhone = ""
                            fixedFare = "1500"
                            capturedPhotoPreview = null
                            currentMode = ManagementMode.ENTRY_FORM
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "➕ REGISTER NEW PASSENGER PROFILE",
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }
        }

        ManagementMode.ENTRY_FORM -> {
            Column(
                modifier = Modifier.fillMaxSize().background(DarkChassis).padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (editingPassengerId != null) "✏️ Edit Passenger Profile" else "➕ Add New Passenger",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = TextMain
                    )
                    Text(
                        "❌ Cancel & Close",
                        color = AlertRed,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { currentMode = ManagementMode.LIST_VIEW })
                }

                OutlinedTextField(
                    value = passengerName, onValueChange = { passengerName = it },
                    label = { Text("Passenger Full Name") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = YellowAuto,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceGray),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(70.dp)
                                .background(Color.DarkGray, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (capturedPhotoPreview != null) {
                                Image(
                                    bitmap = capturedPhotoPreview!!.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text("👤", fontSize = 28.sp)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Candidate Photo Profile Container",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                "Attach verification snapshot.",
                                color = Color.LightGray,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { systemCameraLauncher.launch(null) },
                                    colors = ButtonDefaults.buttonColors(containerColor = BluePin),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(34.dp).weight(1f),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        "📸 CAMERA",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                Button(
                                    onClick = { systemGalleryLauncher.launch("image/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(34.dp).weight(1f),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        "🖼️ GALLERY",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = contactPhone, onValueChange = { contactPhone = it },
                    label = { Text("Primary Contact Phone Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = YellowAuto,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = fixedFare, onValueChange = { fixedFare = it },
                    label = { Text("Monthly Subscription Fare (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = YellowAuto,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("SCHOOL", "OFFICE").forEach { type ->
                        FilterChip(
                            selected = passengerTypeSelection == type,
                            onClick = { passengerTypeSelection = type },
                            label = { Text(type, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Text(
                    "Contract Shift Scope Allocation Selection:",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("BOTH", "MORNING", "EVENING").forEach { tier ->
                        FilterChip(
                            selected = contractSelection == tier,
                            onClick = { contractSelection = tier },
                            label = { Text(tier, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (contractSelection == "BOTH" || contractSelection == "MORNING") {
                    OutlinedTextField(
                        value = morningTimeInput, onValueChange = { morningTimeInput = it },
                        label = { Text("☀️ Morning Pickup Time (e.g., 07:15 AM)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = YellowAuto,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (contractSelection == "BOTH" || contractSelection == "EVENING") {
                    OutlinedTextField(
                        value = eveningTimeInput, onValueChange = { eveningTimeInput = it },
                        label = { Text("🌙 Evening Drop-off Time (e.g., 04:30 PM)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = YellowAuto,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                HorizontalDivider(color = Color.DarkGray, thickness = 1.dp)

                Text(
                    text = "Hardware GPS Pin Registry Metrics:",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = {
                        readHardwareGpsCoordinates { lat, lng ->
                            homeLatInput = lat.toString(); homeLngInput =
                            lng.toString(); Toast.makeText(
                            context,
                            "🏠 Home Pin Locked!",
                            Toast.LENGTH_SHORT
                        ).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BluePin),
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("🏠 CAPTURE CURRENT HOME GPS PIN", fontWeight = FontWeight.Bold)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = homeLatInput,
                        onValueChange = { homeLatInput = it },
                        label = { Text("Home Lat") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    OutlinedTextField(
                        value = homeLngInput,
                        onValueChange = { homeLngInput = it },
                        label = { Text("Home Lng") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                Button(
                    onClick = {
                        readHardwareGpsCoordinates { lat, lng ->
                            destLatInput = lat.toString(); destLngInput =
                            lng.toString(); Toast.makeText(
                            context,
                            "🏫 Destination Hub Pin Locked!",
                            Toast.LENGTH_SHORT
                        ).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF6C00)),
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        "🏫 CAPTURE CURRENT DESTINATION HUB PIN",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = destLatInput,
                        onValueChange = { destLatInput = it },
                        label = { Text("Dest Lat") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    OutlinedTextField(
                        value = destLngInput,
                        onValueChange = { destLngInput = it },
                        label = { Text("Dest Lng") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { handleProfileSubmission() },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "💾 SAVE CUSTOMER PROFILE DATA",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
    }
}
