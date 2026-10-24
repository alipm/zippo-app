package com.zippo.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.zippo.app.data.DriverLoginRequest
import com.zippo.app.data.ZippoApiService
import com.zippo.app.ui.theme.SurfaceGray
import com.zippo.app.ui.theme.YellowAuto
import com.zippo.app.utils.SessionManager
import com.zippo.app.utils.Utils
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onRegisterClick: () -> Unit,
    onForgotPasswordClick: () -> Unit
) {

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val apiService = remember { ZippoApiService.create() }
    val sessionManager = remember { SessionManager(context) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showPasswordSupportDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ZIPPO",
                fontSize = 40.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Smart Transport Management",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
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

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it.trim()
            },
            label = {
                Text("Password")
            },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = SurfaceGray,
                unfocusedTextColor = SurfaceGray,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !isLoading,
            onClick = {

                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(context, "Please enter valid email", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (password.isBlank()) {
                    Toast.makeText(context, "Please enter password", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (!Utils.isInternetAvailable(context)) {
                    Toast.makeText(context, "No Internet Connection", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                coroutineScope.launch {
                    isLoading = true
                    try {
                        val response = apiService.loginDriver(
                            DriverLoginRequest(
                                email = email,
                                password = password
                            )
                        )

                        sessionManager.saveDriverSession(
                            driverId = response.data._id,
                            driverName = response.data.name
                        )

                        Toast.makeText(context, "Welcome ${response.data.name}", Toast.LENGTH_SHORT)
                            .show()

                        onLoginSuccess()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Invalid Email or Password", Toast.LENGTH_SHORT)
                            .show()
                    } finally {
                        isLoading = false
                    }
                }
            }
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("LOGIN")
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
                    onRegisterClick()
                }
            ) {
                Text(
                    text = "Register New Driver",
                    color = SurfaceGray
                )
            }

            Text(
                text = "Password Reset Support",
                color = YellowAuto,
                modifier = Modifier.clickable {
                    showPasswordSupportDialog = true
                }
            )
//            TextButton(
//                onClick = {
//                    onForgotPasswordClick()
//                }
//            ) {
//                Text(
//                    text = "Password Reset Support",
//                    color = SurfaceGray
//                )
//            }
        }
    }

    if (showPasswordSupportDialog) {

        AlertDialog(
            onDismissRequest = {
                showPasswordSupportDialog = false
            },

            title = {
                Text(
                    text = "Password Reset Support"
                )
            },

            text = {
                Text(
                    """
Password reset via email is currently unavailable.

For assistance, please contact:

📧 Email:
masoom117@gmail.com

📞 Phone:
+91 8121242633

ZIPPO Support Team
                """.trimIndent()
                )
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        showPasswordSupportDialog = false
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
}
