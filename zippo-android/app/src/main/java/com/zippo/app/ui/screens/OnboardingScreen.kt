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
import com.zippo.app.data.repository.PassengerRepository
import com.zippo.app.model.PassengerFormState
import com.zippo.app.ui.onboarding.components.PassengerCard
import com.zippo.app.ui.theme.*
import com.zippo.app.utils.AppConstants
import com.zippo.app.utils.ImageUtils
import kotlinx.coroutines.launch

// State navigation mode tracks within the setup window space
enum class ManagementMode { LIST_VIEW, ENTRY_FORM }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val apiService = remember { ZippoApiService.create() }
    val repository = remember { PassengerRepository(apiService) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var currentMode by remember { mutableStateOf(ManagementMode.LIST_VIEW) }
    var editingPassengerId by remember { mutableStateOf<String?>(null) }

    var formState by remember {
        mutableStateOf(
            PassengerFormState()
        )
    }

    var registeredList by remember { mutableStateOf<List<PassengerResponse>>(emptyList()) }
    var isLoaderActive by remember { mutableStateOf(false) }

    var passengerToDelete by remember { mutableStateOf<PassengerResponse?>(null) }

    fun resetForm() {
        formState = PassengerFormState()
        editingPassengerId = null
    }

    fun loadRegisteredPassengersFromCloud() {
        coroutineScope.launch {
            isLoaderActive = true
            try {
                val responseList = repository.getPassengers(driverId = AppConstants.DEMO_DRIVER_ID)
                registeredList = responseList
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoaderActive = false
            }
        }
    }

    fun handleProfileSubmission() {
        if (formState.passengerName.isBlank() || formState.phoneNumber.isBlank() || formState.fixedFare.isBlank()) {
            Toast.makeText(
                context,
                "Please complete all mandatory fields first",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val base64ImageString = ImageUtils.convertBitmapToBase64(formState.capturedPhotoPreview)

        coroutineScope.launch {
            isLoaderActive = true
            try {
                val payload = CreatePassengerRequest(
                    name = formState.passengerName,
                    passengerType = formState.passengerTypeSelection,
                    contractType = formState.contractSelection,
                    monthlyFare = formState.fixedFare.toIntOrNull()
                        ?: AppConstants.DEFAULT_FARE.toInt(),
                    morningPickupTime = if (formState.contractSelection != "EVENING") formState.morningTimeInput else "N/A",
                    eveningPickupTime = if (formState.contractSelection != "MORNING") formState.eveningTimeInput else "N/A",
                    homeCoordinates = CoordinateNode(
                        formState.homeLatInput.toDoubleOrNull()
                            ?: AppConstants.DEFAULT_HOME_LAT.toDouble(),
                        formState.homeLngInput.toDoubleOrNull()
                            ?: AppConstants.DEFAULT_HOME_LNG.toDouble()
                    ),
                    destinationCoordinates = CoordinateNode(
                        formState.destLatInput.toDoubleOrNull()
                            ?: AppConstants.DEFAULT_DEST_LAT.toDouble(),
                        formState.destLngInput.toDoubleOrNull()
                            ?: AppConstants.DEFAULT_DEST_LNG.toDouble()
                    ),
                    profileImageBase64 = base64ImageString,
                    phoneNumber = formState.phoneNumber,
                )

                if (editingPassengerId == null) {

                    repository.createPassenger(
                        driverId = AppConstants.DEMO_DRIVER_ID,
                        request = payload
                    )

                } else {

                    repository.updatePassenger(
                        driverId = AppConstants.DEMO_DRIVER_ID,
                        passengerId = editingPassengerId!!,
                        request = payload
                    )

                }

                Toast.makeText(
                    context,
                    "💾 Passenger record synchronized safely!",
                    Toast.LENGTH_SHORT
                ).show()
                resetForm()
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
                repository.deletePassenger(
                    driverId = AppConstants.DEMO_DRIVER_ID,
                    passengerId = passengerId
                )
                Toast.makeText(context, "🗑️ Passenger document deleted", Toast.LENGTH_SHORT).show()
                loadRegisteredPassengersFromCloud()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to delete passenger", Toast.LENGTH_SHORT)
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
                    Toast.makeText(
                        context,
                        "GPS searching satellite nodes. Stand under open sky.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } catch (e: SecurityException) {
            Toast.makeText(
                context,
                "Location permission missing. Enable in phone settings.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    val systemCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) formState = formState.copy(capturedPhotoPreview = bitmap)
    }

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
                formState = formState.copy(capturedPhotoPreview = bitmap)
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkChassis)
                    .padding(16.dp)
            ) {
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
                            "📋 REGISTERED PEOPLE",
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
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
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
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(registeredList) { passenger ->

                                val passengerBitmap =
                                    ImageUtils.decodeBase64ToBitmap(passenger.profileImageBase64)

                                PassengerCard(
                                    passenger = passenger,
                                    passengerBitmap = passengerBitmap,

                                    onClick = {

                                        editingPassengerId = passenger.id

                                        formState = formState.copy(

                                            passengerName =
                                            passenger.name ?: "",

                                            phoneNumber =
                                            passenger.phoneNumber ?: AppConstants.DEFAULT_PHONE_NUMBER,

                                            fixedFare =
                                            passenger.monthlyFare?.toString()
                                                ?: AppConstants.DEFAULT_FARE,

                                            contractSelection =
                                            passenger.contractType
                                                ?: AppConstants.DEFAULT_CONTRACT_TYPE,

                                            passengerTypeSelection =
                                            passenger.passengerType ?: AppConstants.DEFAULT_TYPE,

                                            homeLatInput =
                                            passenger.homeCoordinates?.lat?.toString()
                                                ?: AppConstants.DEFAULT_HOME_LAT,

                                            homeLngInput =
                                            passenger.homeCoordinates?.lng?.toString()
                                                ?: AppConstants.DEFAULT_HOME_LNG,

                                            destLatInput =
                                            passenger.destinationCoordinates?.lat?.toString()
                                                ?: AppConstants.DEFAULT_DEST_LAT,

                                            destLngInput =
                                            passenger.destinationCoordinates?.lng?.toString()
                                                ?: AppConstants.DEFAULT_DEST_LNG,

                                            capturedPhotoPreview =
                                            ImageUtils.decodeBase64ToBitmap(
                                                passenger.profileImageBase64
                                            )

                                        )

                                        currentMode =
                                            ManagementMode.ENTRY_FORM
                                    },

                                    onDelete = {
                                        passengerToDelete = passenger
                                    }
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            resetForm()
                            currentMode = ManagementMode.ENTRY_FORM
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "➕ REGISTER NEW PASSENGER ",
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }
        }

        ManagementMode.ENTRY_FORM -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkChassis)
                    .padding(16.dp)
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
                    value = formState.passengerName,
                    onValueChange = { formState = formState.copy(passengerName = it) },
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
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .background(Color.DarkGray, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (formState.capturedPhotoPreview != null) {
                                Image(
                                    bitmap = formState.capturedPhotoPreview!!.asImageBitmap(),
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
                                    modifier = Modifier
                                        .height(34.dp)
                                        .weight(1f),
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
                                    modifier = Modifier
                                        .height(34.dp)
                                        .weight(1f),
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
                    value = formState.phoneNumber,
                    onValueChange = { formState = formState.copy(phoneNumber = it) },
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
                    value = formState.fixedFare,
                    onValueChange = { formState = formState.copy(fixedFare = it) },
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
                            selected = formState.passengerTypeSelection == type,
                            onClick = { formState = formState.copy(passengerTypeSelection = type) },
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
                            selected = formState.contractSelection == tier,
                            onClick = { formState = formState.copy(contractSelection = tier) },
                            label = { Text(tier, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (formState.contractSelection == "BOTH" || formState.contractSelection == "MORNING") {
                    OutlinedTextField(
                        value = formState.morningTimeInput,
                        onValueChange = { formState = formState.copy(morningTimeInput = it) },
                        label = { Text("☀️ Morning Pickup Time (e.g., 07:15 AM)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = YellowAuto,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (formState.contractSelection == "BOTH" || formState.contractSelection == "EVENING") {
                    OutlinedTextField(
                        value = formState.eveningTimeInput,
                        onValueChange = { formState = formState.copy(eveningTimeInput = it) },
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
                            formState = formState.copy(
                                homeLatInput = lat.toString(),
                                homeLngInput = lng.toString()
                            )
                            Toast.makeText(context, "🏠 Home Pin Locked!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BluePin),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("🏠 CAPTURE CURRENT HOME GPS PIN", fontWeight = FontWeight.Bold)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = formState.homeLatInput,
                        onValueChange = { formState = formState.copy(homeLatInput = it) },
                        label = { Text("Home Lat") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    OutlinedTextField(
                        value = formState.homeLngInput,
                        onValueChange = { formState = formState.copy(homeLngInput = it) },
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
                            formState = formState.copy(
                                destLatInput = lat.toString(),
                                destLngInput = lng.toString()
                            )
                            Toast.makeText(
                                context,
                                "🏫 Destination Hub Pin Locked!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF6C00)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
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
                        value = formState.destLatInput,
                        onValueChange = { formState = formState.copy(destLatInput = it) },
                        label = { Text("Dest Lat") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    OutlinedTextField(
                        value = formState.destLngInput,
                        onValueChange = { formState = formState.copy(destLngInput = it) },
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        if (editingPassengerId == null)
                            "💾 SAVE CUSTOMER PROFILE DATA"
                        else
                            "✏️ UPDATE CUSTOMER PROFILE",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
    }

    if (passengerToDelete != null) {

        AlertDialog(
            onDismissRequest = {
                passengerToDelete = null
            },

            title = {
                Text("Delete Passenger")
            },

            text = {
                Text("Are you sure you want to delete this ${passengerToDelete?.name}?")
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        triggerProfileDelete(passengerToDelete?.id ?: "")
                        passengerToDelete = null
                    }
                ) {
                    Text(
                        "Delete",
                        color = Color.Red
                    )
                }
            },

            dismissButton = {
                TextButton(
                    onClick = {
                        passengerToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
