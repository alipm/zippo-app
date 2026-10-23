package com.zippo.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zippo.app.data.ForgotPasswordRequest
import com.zippo.app.data.ZippoApiService
import com.zippo.app.ui.theme.SurfaceGray
import com.zippo.app.utils.Utils
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler

@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    onOpenResetPassword: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    val apiService = remember { ZippoApiService.create() }
    var email by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text("Forgot Password")

        Spacer(modifier = Modifier.height(20.dp))

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
            ),
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            enabled = !isLoading,
            onClick = {
                if (!Utils.isInternetAvailable(context)) {
                    Toast.makeText(context, "No Internet Connection", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (email.isBlank()) {
                    Toast.makeText(context, "Please enter email", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(context, "Please enter valid email", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                coroutineScope.launch {
                    isLoading = true
                    try {
                        apiService.forgotPassword(
                            ForgotPasswordRequest(
                                email = email
                            )
                        )
                        Toast.makeText(context, "If the email exists, password reset instructions have been sent.", Toast.LENGTH_LONG).show()
                        onOpenResetPassword()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed", Toast.LENGTH_LONG).show()
                    } finally {
                        isLoading = false
                    }
                }
            }
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text("SEND EMAIL")
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = {
                    onBack()
                }
            ) {
                Text(
                    text = "Back To Login",
                    color = SurfaceGray
                )
            }
        }
    }
}