package com.zippo.app.ui.navigation

sealed class Screen(val route: String, val title: String, val icon: String) {
    object Home : Screen("home", "Home", "🧭")
    object Ledger : Screen("ledger", "Ledger", "📖")
    object Onboarding : Screen("onboarding", "Setup", "⚙️")
}