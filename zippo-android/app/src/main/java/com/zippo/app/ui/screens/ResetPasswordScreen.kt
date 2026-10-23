package com.zippo.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.zippo.app.data.ResetPasswordRequest
import com.zippo.app.data.ZippoApiService
import com.zippo.app.ui.theme.SurfaceGray
import com.zippo.app.utils.Utils
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color

@Composable
fun ResetPasswordScreen(
    onBackToLogin: () -> Unit
) {

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val apiService = remember { ZippoApiService.create() }
    var token by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

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
            text = "Reset Password",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = token,
            onValueChange = {
                token = it.trim()
            },
            label = {
                Text("Verification Code")
            },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = SurfaceGray,
                unfocusedTextColor = SurfaceGray,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it.trim()
            },
            label = {
                Text("New Password")
            },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = SurfaceGray,
                unfocusedTextColor = SurfaceGray,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it.trim()
            },
            label = {
                Text("Confirm Password")
            },
            visualTransformation =
            PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = SurfaceGray,
                unfocusedTextColor = SurfaceGray,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            enabled = !isLoading,
            onClick = {
                if (token.isBlank()) {
                    Toast.makeText(context, "Enter Token", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (password.isBlank()) {

                    Toast.makeText(context, "Enter Password", Toast.LENGTH_SHORT).show()
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

                        apiService.resetPassword(
                            ResetPasswordRequest(
                                token = token,
                                newPassword = password
                            )
                        )

                        Toast.makeText(context, "Password Updated Successfully", Toast.LENGTH_LONG)
                            .show()
                        onBackToLogin()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Invalid Token", Toast.LENGTH_LONG).show()
                    } finally {
                        isLoading = false
                    }
                }
            }
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Text("RESET PASSWORD")
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
                    onBackToLogin()
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