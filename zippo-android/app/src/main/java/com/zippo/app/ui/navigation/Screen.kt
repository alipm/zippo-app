package com.zippo.app.ui.navigation

sealed class Screen(val route: String, val title: String, val icon: String) {
    object Home : Screen("home", "Dashboard", "🛺")
    object Ledger : Screen("ledger", "Khata", "📊")
    object Onboarding : Screen("onboarding", "Setup", "⚙️")
}
