package com.zippo.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zippo.app.data.RegisterDriverRequest
import com.zippo.app.data.ZippoApiService
import com.zippo.app.ui.theme.SurfaceGray
import com.zippo.app.utils.Utils
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler


@Composable
fun RegisterScreen(
    onRegistrationSuccess: () -> Unit,
    onBack: () -> Unit
) {

    BackHandler { onBack() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val apiService = remember { ZippoApiService.create() }

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var vehicleNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        TextButton(
            onClick = {
                onBack()
            }
        ) {
            Text("← Back")
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Create Account",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it.trim()
            },
            label = {
                Text("Name")
            },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = SurfaceGray,
                unfocusedTextColor = SurfaceGray,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it.trim()
            },
            label = {
                Text("Email")
            },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = SurfaceGray,
                unfocusedTextColor = SurfaceGray,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = {
                phoneNumber = it.trim()
            },
            label = {
                Text("Phone Number")
            },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = SurfaceGray,
                unfocusedTextColor = SurfaceGray,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )

        OutlinedTextField(
            value = vehicleNumber,
            onValueChange = {
                vehicleNumber = it.trim()
            },
            label = {
                Text("Vehicle Number (Optional)")
            },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = SurfaceGray,
                unfocusedTextColor = SurfaceGray,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it.trim()
            },
            label = {
                Text("Password")
            },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = SurfaceGray,
                unfocusedTextColor = SurfaceGray,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            visualTransformation = PasswordVisualTransformation()
        )

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it.trim()
            },
            label = {
                Text("Confirm Password")
            },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = SurfaceGray,
                unfocusedTextColor = SurfaceGray,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            enabled = !isLoading,
            onClick = {
                if (name.isBlank()) {
                    Toast.makeText(context, "Enter name", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (email.isBlank()) {
                    Toast.makeText(context, "Enter email", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (phoneNumber.isBlank()) {
                    Toast.makeText(context, "Enter phone number", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (password.isBlank()) {
                    Toast.makeText(context, "Enter password", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (password != confirmPassword) {
                    Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (!Utils.isInternetAvailable(context)) {
                    Toast.makeText(context, "No Internet Connection", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                coroutineScope.launch {
                    isLoading = true
                    try {
                        apiService.registerDriver(
                            RegisterDriverRequest(
                                name = name,
                                phoneNumber = phoneNumber,
                                email = email,
                                vehicleNumber = vehicleNumber.ifBlank { null },
                                password = password
                            )
                        )

                        Toast.makeText(context, "Registration Successful", Toast.LENGTH_LONG).show()
                        onRegistrationSuccess()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Registration Failed", Toast.LENGTH_LONG).show()
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.width(180.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text("REGISTER")
            }
        }
    }
}