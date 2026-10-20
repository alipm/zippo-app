package com.zippo.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
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
import kotlinx.coroutines.launch

@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit
) {

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val apiService = remember {
        ZippoApiService.create()
    }

    var email by remember {
        mutableStateOf("")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text("Forgot Password")

        Spacer(
            modifier = Modifier.height(20.dp)
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
            ),
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Button(
            onClick = {

                coroutineScope.launch {

                    try {

                        apiService.forgotPassword(
                            ForgotPasswordRequest(
                                email = email
                            )
                        )

                        Toast.makeText(
                            context,
                            "Reset email sent",
                            Toast.LENGTH_LONG
                        ).show()

                        onBack()

                    } catch (e: Exception) {

                        Toast.makeText(
                            context,
                            "Failed",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        ) {
            Text("SEND EMAIL")
        }

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        TextButton(
            onClick = {
                onBack()
            }
        ) {
            Text("Back To Login")
        }
    }
}