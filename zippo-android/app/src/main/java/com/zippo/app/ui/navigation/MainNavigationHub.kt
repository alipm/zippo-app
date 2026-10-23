package com.zippo.app.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zippo.app.ui.screens.ForgotPasswordScreen
import com.zippo.app.ui.screens.HomeScreen
import com.zippo.app.ui.screens.LedgerScreen
import com.zippo.app.ui.screens.LoginScreen
import com.zippo.app.ui.screens.OnboardingScreen
import com.zippo.app.ui.screens.RegisterScreen
import com.zippo.app.ui.screens.ResetPasswordScreen
import com.zippo.app.ui.theme.*
import com.zippo.app.utils.SessionManager

@Composable
fun MainNavigationHub() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    var loggedIn by remember { mutableStateOf(sessionManager.isLoggedIn()) }
    var showForgotPassword by remember { mutableStateOf(false) }
    val isServerWakingUp by ServerStateManager.isServerWakingUp.collectAsState()
    var showRegisterScreen by remember { mutableStateOf(false) }
    var showResetPassword by remember { mutableStateOf(false) }

    if (!loggedIn) {

        if (showResetPassword) {
            ResetPasswordScreen(
                onBackToLogin = {
                    showResetPassword = false
                }
            )
        } else if (showForgotPassword) {

            ForgotPasswordScreen(
                onBack = {
                    showForgotPassword = false
                },
                onOpenResetPassword = {
                    showForgotPassword = false
                    showResetPassword = true
                }
            )
        } else if (showRegisterScreen) {
            RegisterScreen(
                onRegistrationSuccess = {
                    showRegisterScreen = false
                },
                onBack = {
                    showRegisterScreen = false
                }
            )
        } else {
            LoginScreen(
                onLoginSuccess = {
                    loggedIn = true
                },
                onRegisterClick = {
                    showRegisterScreen = true
                },
                onForgotPasswordClick = {
                    showForgotPassword = true
                }
            )
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                bottomBar = {
                    // HIGH-CONTRAST CHASSIS FOOTER HUB (56dp Height layout bar structure)
                    Surface(
                        color = SurfaceGray,
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val items = listOf(Screen.Home, Screen.Ledger, Screen.Onboarding)

                            items.forEach { screen ->
                                val isSelected = currentScreen == screen

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable { currentScreen = screen }
                                        .padding(vertical = 4.dp)
                                ) {
                                    // High Visibility Indicator Typography & Emojis
                                    Text(
                                        text = screen.icon,
                                        fontSize = 20.sp
                                    )
                                    Text(
                                        text = screen.title,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = if (isSelected) YellowAuto else Color.LightGray
                                    )
                                }
                            }
                        }
                    }
                },
                containerColor = DarkChassis
            ) { innerPadding ->

                // 🟢 FIXED: The NavHost container now enforces explicit innerPadding boundaries down to your screen views.
                // This ensures no inner dashboard elements or ledger button matrices get hidden underneath the footer tabs!
                Box(
                    modifier = Modifier
                        .padding(innerPadding) // Safely accounts for the 64.dp height allocation of the footer hub
                        .fillMaxSize()
                        .background(DarkChassis)
                ) {
                    when (currentScreen) {
                        is Screen.Home -> HomeScreen(onLogout = {
                            loggedIn = false
                            currentScreen = Screen.Home
                        })
                        is Screen.Ledger -> LedgerScreen()
                        is Screen.Onboarding -> OnboardingScreen()
                    }
                }
            }
        }
        // 🟢 FLOATING TOP BANNER OVERLAY: Shows up anywhere in Zippo if a request hangs > 2s
        AnimatedVisibility(
            visible = isServerWakingUp,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)) // Optional dark tint (dim background effect)
                    .clickable(
                        enabled = true,
                        onClick = { /* 🟢 INTENTIONALLY BLANK: Consumes touches so user cannot click elements underneath! */ })
            ) {
                Surface(
                    color = SurfaceGray, // Styled to match your high-contrast theme
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .statusBarsPadding() // Safely shifts content beneath the Android status bar icons
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = YellowAuto,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = """Starting ZIPPO services... Free hosting may take 30–60 seconds to wake up. Please wait...""".trimIndent(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                    }
                }
            }
        }
    }
}