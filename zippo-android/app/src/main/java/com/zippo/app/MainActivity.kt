package com.zippo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.zippo.app.ui.navigation.MainNavigationHub
import com.zippo.app.ui.theme.ZippoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZippoTheme {
                MainNavigationHub()
            }
        }
    }
}
