package com.example.hichamjeunemaster.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.hichamjeunemaster.R

enum class BottomNavItem(val labelRes: Int, val icon: ImageVector, val route: String) {
    HOME(R.string.nav_home, Icons.Rounded.Home, "home"),
    PLANS(R.string.nav_plans, Icons.Rounded.Restaurant, "plans"),
    PROFILE(R.string.nav_profile, Icons.Rounded.Person, "profile"),
    HISTORY(R.string.nav_history, Icons.Rounded.History, "history"),
    SETTINGS(R.string.nav_settings, Icons.Rounded.Settings, "settings")
}

@Composable
fun BottomNavBar(
    currentRoute: String,
    onItemClick: (BottomNavItem) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
        )
    ) {
        NavigationBar(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            tonalElevation = 0.dp
        ) {
            BottomNavItem.entries.forEach { item ->
                val selected = currentRoute == item.route
                
                NavigationBarItem(
                    selected = selected,
                    onClick = { onItemClick(item) },
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = stringResource(item.labelRes),
                            modifier = Modifier.size(if (selected) 28.dp else 24.dp),
                            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(item.labelRes),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = androidx.compose.ui.graphics.Color.Transparent
                    )
                )
            }
        }
    }
}
