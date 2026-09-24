package com.zippo.app.ui.screens

import DetailRow
import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.Context
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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.zippo.app.utils.Utils
import kotlinx.coroutines.launch
import java.util.Calendar

// State navigation mode tracks within the setup window space
enum class ManagementMode { LIST_VIEW, VIEW_DETAILS, ENTRY_FORM }

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
    var formState by remember { mutableStateOf(PassengerFormState()) }
    var registeredList by remember { mutableStateOf<List<PassengerResponse>>(emptyList()) }
    var isLoaderActive by remember { mutableStateOf(false) }
    var passengerToDelete by remember { mutableStateOf<PassengerResponse?>(null) }
    var selectedPassenger by remember { mutableStateOf<PassengerResponse?>(null) }

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

        fun showValidation(message: String) {
            Toast.makeText(
                context, message, Toast.LENGTH_SHORT
            ).show()
        }

        when {
            formState.passengerName.isBlank() -> return showValidation("Please enter person name")

            formState.phoneNumber.isBlank() -> return showValidation("Please enter mobile number")

            formState.fixedFare.isBlank() -> return showValidation("Please enter monthly fee")

            formState.phoneNumber.length != 10 -> return showValidation("Please enter a valid 10-digit mobile number")

            formState.fixedFare.toIntOrNull() == null || formState.fixedFare.toInt() <= 0 -> return showValidation(
                "Please enter a valid monthly fee"
            )

            formState.contractSelection == "BOTH" && formState.morningTimeInput.isBlank() -> return showValidation(
                "Please select pickup time"
            )

            formState.contractSelection == "BOTH" && formState.eveningTimeInput.isBlank() -> return showValidation(
                "Please select drop time"
            )

            formState.contractSelection == "MORNING" && formState.morningTimeInput.isBlank() -> return showValidation(
                "Please select pickup time"
            )

            formState.contractSelection == "EVENING" && formState.eveningTimeInput.isBlank() -> return showValidation(
                "Please select drop time"
            )
//            formState.homeLatInput.isBlank() ||
//                    formState.homeLngInput.isBlank() ->
//                return showValidation("Please capture home location")
//            formState.destLatInput.isBlank() ||
//                    formState.destLngInput.isBlank() ->
//                return showValidation("Please capture destination location")
        }

        val base64ImageString = Utils.convertBitmapToBase64(formState.capturedPhotoPreview)
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
                    remarks = formState.remarks
                )

                if (editingPassengerId == null) {
                    repository.createPassenger(
                        driverId = AppConstants.DEMO_DRIVER_ID, request = payload
                    )
                } else {
                    repository.updatePassenger(
                        driverId = AppConstants.DEMO_DRIVER_ID,
                        passengerId = editingPassengerId!!,
                        request = payload
                    )
                }

                Toast.makeText(context, "Passenger record saved", Toast.LENGTH_SHORT).show()
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
                    driverId = AppConstants.DEMO_DRIVER_ID, passengerId = passengerId
                )
                Toast.makeText(context, "Passenger deleted", Toast.LENGTH_SHORT).show()
                loadRegisteredPassengersFromCloud()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to delete passenger", Toast.LENGTH_SHORT).show()
                loadRegisteredPassengersFromCloud()
            }
        }
    }

    fun openTimePicker(
        context: Context, onTimeSelected: (String) -> Unit
    ) {
        val calendar = Calendar.getInstance()
        TimePickerDialog(
            context, { _, hour, minute ->
                val amPm = if (hour >= 12) "PM" else "AM"
                val displayHour = when {
                    hour == 0 -> 12
                    hour > 12 -> hour - 12
                    else -> hour
                }
                val formattedTime = String.format("%02d:%02d %s", displayHour, minute, amPm)
                onTimeSelected(formattedTime)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false
        ).show()
    }

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
                            context.contentResolver, uri
                        )
                    )
                } else {
                    @Suppress("DEPRECATION") MediaStore.Images.Media.getBitmap(
                        context.contentResolver, uri
                    )
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
                            "Passenger",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        if (isLoaderActive) CircularProgressIndicator(
                            modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = YellowAuto
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
                                    Utils.decodeBase64ToBitmap(passenger.profileImageBase64)

                                PassengerCard(passenger = passenger,
                                    passengerBitmap = passengerBitmap,
                                    onView = {
                                        selectedPassenger = passenger
                                        currentMode = ManagementMode.VIEW_DETAILS
                                    },
                                    onEdit = {
                                        editingPassengerId = passenger.id
                                        formState = formState.copy(
                                            passengerName = passenger.name ?: "",
                                            phoneNumber = passenger.phoneNumber
                                                ?: AppConstants.DEFAULT_PHONE_NUMBER,
                                            fixedFare = passenger.monthlyFare?.toString()
                                                ?: AppConstants.DEFAULT_FARE,
                                            contractSelection = passenger.contractType
                                                ?: "BOTH",
                                            passengerTypeSelection = passenger.passengerType
                                                ?: "SCHOOL",
                                            homeLatInput = passenger.homeCoordinates?.lat?.toString()
                                                ?: "",
                                            homeLngInput = passenger.homeCoordinates?.lng?.toString()
                                                ?: "",
                                            destLatInput = passenger.destinationCoordinates?.lat?.toString()
                                                ?: "",
                                            destLngInput = passenger.destinationCoordinates?.lng?.toString()
                                                ?: "",
                                            morningTimeInput = passenger.morningPickupTime ?: "",
                                            eveningTimeInput = passenger.eveningPickupTime ?: "",
                                            capturedPhotoPreview = Utils.decodeBase64ToBitmap(
                                                passenger.profileImageBase64
                                            ),
                                            remarks = passenger.remarks ?: ""
                                        )
                                        currentMode = ManagementMode.ENTRY_FORM
                                    },
                                    onDelete = {
                                        passengerToDelete = passenger
                                    })
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
                            "Add Passenger", fontWeight = FontWeight.Black, color = Color.White
                        )
                    }
                }
            }
        }

        ManagementMode.VIEW_DETAILS -> {

            val passengerBitmap =
                Utils.decodeBase64ToBitmap(
                    selectedPassenger?.profileImageBase64
                )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkChassis)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // =========================================================
                // HEADER PROFILE CARD
                // =========================================================

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = SurfaceGray
                    )
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        if (passengerBitmap != null) {

                            Image(
                                bitmap = passengerBitmap.asImageBitmap(),
                                contentDescription = "Passenger Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = 3.dp,
                                        color = YellowAuto,
                                        shape = CircleShape
                                    )
                            )

                        } else {

                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(CircleShape)
                                    .background(Color.DarkGray),
                                contentAlignment = Alignment.Center
                            ) {

                                Text(
                                    "👤",
                                    fontSize = 40.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = selectedPassenger?.name ?: "Passenger",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = selectedPassenger?.passengerType ?: "-",
                            color = YellowAuto,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // =========================================================
                // CONTACT INFO
                // =========================================================

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = SurfaceGray
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        Text(
                            "📞 Contact Information",
                            color = YellowAuto,
                            fontWeight = FontWeight.Bold
                        )

                        DetailRow(
                            "Phone Number",
                            selectedPassenger?.phoneNumber ?: "-"
                        )
                    }
                }

                // =========================================================
                // TRANSPORT DETAILS
                // =========================================================

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = SurfaceGray
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        Text(
                            "🚌 Transport Details",
                            color = YellowAuto,
                            fontWeight = FontWeight.Bold
                        )

                        DetailRow(
                            "Monthly Fare",
                            "₹${selectedPassenger?.monthlyFare ?: 0}"
                        )

                        DetailRow(
                            "Contract Type",
                            selectedPassenger?.contractType ?: "-"
                        )

                        DetailRow(
                            "Pickup Time",
                            selectedPassenger?.morningPickupTime ?: "-"
                        )

                        DetailRow(
                            "Drop Time",
                            selectedPassenger?.eveningPickupTime ?: "-"
                        )
                    }
                }

                // =========================================================
                // LOCATION STATUS
                // =========================================================

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = SurfaceGray
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {

                        Text(
                            "📍 Location Status",
                            color = YellowAuto,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = if (
                                selectedPassenger?.homeCoordinates != null
                            ) {
                                "✅ Pickup Location Saved"
                            } else {
                                "❌ Pickup Location Not Saved"
                            },
                            color = Color.White
                        )

                        Text(
                            text = if (
                                selectedPassenger?.destinationCoordinates != null
                            ) {
                                "✅ Drop Location Saved"
                            } else {
                                "❌ Drop Location Not Saved"
                            },
                            color = Color.White
                        )
                    }
                }

                // =========================================================
                // NOTES
                // =========================================================

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = SurfaceGray
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {

                        Text(
                            "📝 Additional Notes",
                            color = YellowAuto,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )

                        Text(
                            text =
                            selectedPassenger?.remarks
                                ?.takeIf { it.isNotBlank() }
                                ?: "No additional notes available.",
                            color = Color.White,
                            lineHeight = 22.sp
                        )
                    }
                }

                // =========================================================
                // BACK BUTTON
                // =========================================================

                Button(
                    onClick = {
                        currentMode = ManagementMode.LIST_VIEW
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BluePin
                    )
                ) {

                    Text(
                        "⬅ Back to Passenger List",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(
                    modifier = Modifier.height(12.dp)
                )
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
                        if (editingPassengerId != null) "Edit Passenger" else "Add Passenger",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = TextMain
                    )
                    Text("Cancel",
                        color = Color.LightGray,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { currentMode = ManagementMode.LIST_VIEW })
                }

                OutlinedTextField(
                    value = formState.passengerName,
                    onValueChange = {
                        if (it.all { char -> char.isLetter() || char.isWhitespace() }) {
                            formState = formState.copy(passengerName = it)
                        }
                    },
                    label = { Text("Passenger Name") },
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
                                "Passenger Photo",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                "Click below Camera or Gallery to add.",
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
                                        "📸 CAMERA", fontSize = 10.sp, fontWeight = FontWeight.Black
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
                    onValueChange = {
                        if (it.length <= 10 && it.all(Char::isDigit)) {
                            formState = formState.copy(phoneNumber = it)
                        }
                    },
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
                    onValueChange = {
                        if (it.all(Char::isDigit)) {
                            formState = formState.copy(fixedFare = it)
                        }
                    },
                    label = { Text("Monthly Subscription Fare (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = YellowAuto,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = formState.remarks,
                    onValueChange = {
                        if (it.length <= 250) {
                            formState = formState.copy(
                                remarks = it
                            )
                        }
                    },
                    label = {
                        Text("Additional Notes")
                    },
                    placeholder = {
                        Text("School, class, pickup notes...")
                    },
                    minLines = 3,
                    maxLines = 5,
                    supportingText = {
                        Text("${formState.remarks.length}/250")
                    },
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
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = YellowAuto,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                Text(
                    "Contract Allocation",
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
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = YellowAuto,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                if (formState.contractSelection == "BOTH" || formState.contractSelection == "MORNING") {
                    Button(
                        onClick = {
                            openTimePicker(context) { selectedTime ->
                                formState = formState.copy(
                                    morningTimeInput = selectedTime
                                )
                            }
                        }, modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (formState.morningTimeInput.isBlank()) "🕒 Select Pickup Time"
                            else "🕒 Pickup: ${formState.morningTimeInput}"
                        )
                    }
                }

                if (formState.contractSelection == "BOTH" || formState.contractSelection == "EVENING") {
                    Button(
                        onClick = {
                            openTimePicker(context) { selectedTime ->
                                formState = formState.copy(
                                    eveningTimeInput = selectedTime
                                )
                            }
                        }, modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (formState.eveningTimeInput.isBlank()) "🕒 Select Drop Time"
                            else "🕒 Drop: ${formState.eveningTimeInput}"
                        )
                    }
                }

                HorizontalDivider(color = Color.DarkGray, thickness = 1.dp)

                Text(
                    text = "📍 Pickup & Drop Locations:",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = {
                        readHardwareGpsCoordinates { lat, lng ->
                            formState = formState.copy(
                                homeLatInput = lat.toString(), homeLngInput = lng.toString()
                            )
                            Toast.makeText(context, "🏠 Pickup Location Locked", Toast.LENGTH_SHORT)
                                .show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BluePin),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("🏠 Save Pickup Location", fontWeight = FontWeight.Bold)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = formState.homeLatInput,
                        onValueChange = { formState = formState.copy(homeLatInput = it) },
                        readOnly = true,
                        label = { Text("Pickup Latitude") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White
                        )
                    )
                    OutlinedTextField(
                        value = formState.homeLngInput,
                        onValueChange = { formState = formState.copy(homeLngInput = it) },
                        readOnly = true,
                        label = { Text("Pickup Longitude") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White
                        )
                    )
                }

                Button(
                    onClick = {
                        readHardwareGpsCoordinates { lat, lng ->
                            formState = formState.copy(
                                destLatInput = lat.toString(), destLngInput = lng.toString()
                            )
                            Toast.makeText(
                                context, "🏫 Drop Location Locked", Toast.LENGTH_SHORT
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
                        "🏫 Save Drop Location", fontWeight = FontWeight.Bold, color = Color.White
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = formState.destLatInput,
                        onValueChange = { formState = formState.copy(destLatInput = it) },
                        readOnly = true,
                        label = { Text("Drop Latitude") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White
                        )
                    )
                    OutlinedTextField(
                        value = formState.destLngInput,
                        onValueChange = { formState = formState.copy(destLngInput = it) },
                        readOnly = true,
                        label = { Text("Drop Longitude") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White
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
                        if (editingPassengerId == null) "💾 Save Passenger"
                        else "✏️ Update Passenger",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
    }

    if (passengerToDelete != null) {

        AlertDialog(onDismissRequest = {
            passengerToDelete = null
        }, title = {
            Text("Delete Passenger")
        }, text = {
            Text("Are you sure you want to delete ${passengerToDelete?.name} ?")
        }, confirmButton = {
            TextButton(onClick = {
                triggerProfileDelete(passengerToDelete?.id ?: "")
                passengerToDelete = null
            }) {
                Text(
                    "Delete", color = Color.Red
                )
            }
        },

            dismissButton = {
                TextButton(onClick = {
                    passengerToDelete = null
                }) {
                    Text("Cancel")
                }
            })
    }
}
