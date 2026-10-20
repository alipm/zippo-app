package com.zippo.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
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
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    onRegistrationSuccess: () -> Unit
) {

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val apiService = remember {
        ZippoApiService.create()
    }

    var name by remember {
        mutableStateOf("")
    }

    var email by remember {
        mutableStateOf("")
    }

    var phoneNumber by remember {
        mutableStateOf("")
    }

    var vehicleNumber by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var confirmPassword by remember {
        mutableStateOf("")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
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
                name = it
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
                email = it
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
                phoneNumber = it
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
                vehicleNumber = it
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
                password = it
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
                confirmPassword = it
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
            onClick = {

                if (name.isBlank()) {
                    Toast.makeText(
                        context,
                        "Enter name",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@Button
                }

                if (email.isBlank()) {
                    Toast.makeText(
                        context,
                        "Enter email",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@Button
                }

                if (phoneNumber.isBlank()) {
                    Toast.makeText(
                        context,
                        "Enter phone number",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@Button
                }

                if (password.isBlank()) {
                    Toast.makeText(
                        context,
                        "Enter password",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@Button
                }

                if (password != confirmPassword) {
                    Toast.makeText(
                        context,
                        "Passwords do not match",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@Button
                }

                coroutineScope.launch {

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

                        Toast.makeText(
                            context,
                            "Registration Successful",
                            Toast.LENGTH_LONG
                        ).show()

                        onRegistrationSuccess()

                    } catch (e: Exception) {

                        Toast.makeText(
                            context,
                            "Registration Failed",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            },
            modifier = Modifier.width(180.dp)
        ) {
            Text("REGISTER")
        }
    }
}