package com.example.hichamjeunemaster

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.hichamjeunemaster.data.PreferencesManager
import com.example.hichamjeunemaster.ui.navigation.AppNavigation
import com.example.hichamjeunemaster.ui.theme.JeuneMasterTheme
import com.example.hichamjeunemaster.data.AdsManager

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AdsManager.initialize(this)
        
        requestNotificationPermission()
        
        val prefsManager = PreferencesManager(applicationContext)

        setContent {
            val darkMode by prefsManager.darkMode.collectAsState(initial = false)

            JeuneMasterTheme(darkTheme = darkMode) {
                AppNavigation(prefsManager = prefsManager)
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }
}