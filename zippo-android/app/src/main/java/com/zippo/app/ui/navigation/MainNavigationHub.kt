package com.zippo.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zippo.app.ui.screens.HomeScreen
import com.zippo.app.ui.screens.LedgerScreen
import com.zippo.app.ui.screens.OnboardingScreen
import com.zippo.app.ui.theme.*

@Composable
fun MainNavigationHub() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    Scaffold(
        bottomBar = {
            // HIGH-CONTRAST CHASSIS FOOTER HUB (56dp Height layout bar structure)
            Surface(
                color = SurfaceGray,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth().height(64.dp)
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
                is Screen.Home -> HomeScreen()
                is Screen.Ledger -> LedgerScreen()
                is Screen.Onboarding -> OnboardingScreen()
            }
        }
    }
}
