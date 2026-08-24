package com.example.hichamjeunemaster.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.hichamjeunemaster.data.PreferencesManager
import com.example.hichamjeunemaster.ui.components.BottomNavBar
import com.example.hichamjeunemaster.ui.components.BottomNavItem
import com.example.hichamjeunemaster.ui.screens.*
import com.example.hichamjeunemaster.ui.components.BannerAdView
import androidx.compose.foundation.layout.Column

@Composable
fun AppNavigation(prefsManager: PreferencesManager) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"

    Scaffold(
        bottomBar = {
            Column {
                BannerAdView()
                BottomNavBar(
                    currentRoute = currentRoute,
                    onItemClick = { item ->
                        if (currentRoute != item.route) {
                            navController.navigate(item.route) {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") { HomeScreen(prefsManager) }
            composable("plans") { FastingPlanScreen(prefsManager) }
            composable("profile") { ProfileScreen(prefsManager, onNavigateToWeight = { navController.navigate("weight") }) }
            composable("history") { HistoryScreen(prefsManager) }
            composable("settings") { SettingsScreen(prefsManager) }
            composable("weight") { WeightScreen(prefsManager, onBack = { navController.popBackStack() }) }
        }
    }
}
