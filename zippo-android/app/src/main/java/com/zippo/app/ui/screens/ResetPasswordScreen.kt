package com.zippo.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.zippo.app.data.ResetPasswordRequest
import com.zippo.app.data.ZippoApiService
import kotlinx.coroutines.launch

@Composable
fun ResetPasswordScreen(
    onBackToLogin: () -> Unit
) {

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val apiService = remember {
        ZippoApiService.create()
    }

    var token by remember {
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
            text = "Reset Password",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = token,
            onValueChange = {
                token = it
            },
            label = {
                Text("Reset Token")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
            },
            label = {
                Text("New Password")
            },
            visualTransformation =
            PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
            },
            label = {
                Text("Confirm Password")
            },
            visualTransformation =
            PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {

                if (token.isBlank()) {

                    Toast.makeText(
                        context,
                        "Enter Token",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@Button
                }

                if (password.isBlank()) {

                    Toast.makeText(
                        context,
                        "Enter Password",
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

                        apiService.resetPassword(
                            ResetPasswordRequest(
                                token = token,
                                newPassword = password
                            )
                        )

                        Toast.makeText(
                            context,
                            "Password Updated Successfully",
                            Toast.LENGTH_LONG
                        ).show()

                        onBackToLogin()

                    } catch (e: Exception) {

                        Toast.makeText(
                            context,
                            "Invalid Token",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        ) {
            Text("RESET PASSWORD")
        }

        Spacer(modifier = Modifier.height(10.dp))

        TextButton(
            onClick = {
                onBackToLogin()
            }
        ) {
            Text("Back To Login")
        }
    }
}