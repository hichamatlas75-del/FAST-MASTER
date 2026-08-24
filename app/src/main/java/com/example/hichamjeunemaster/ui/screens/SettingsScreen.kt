package com.example.hichamjeunemaster.ui.screens

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hichamjeunemaster.data.DriveServiceHelper
import com.example.hichamjeunemaster.data.FastingPlan
import com.example.hichamjeunemaster.data.PreferencesManager
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.example.hichamjeunemaster.R
import java.util.Locale

private const val TAG = "SettingsScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(prefsManager: PreferencesManager) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val darkMode by prefsManager.darkMode.collectAsState(initial = false)
    val scheduleHour by prefsManager.referenceScheduleHour.collectAsState(initial = 20)
    val scheduleMinute by prefsManager.referenceScheduleMinute.collectAsState(initial = 0)
    val autoStart by prefsManager.autoStartEnabled.collectAsState(initial = false)
    val plan by prefsManager.selectedPlan.collectAsState(initial = FastingPlan.PLAN_16_8)

    var showBackupSuccess by remember { mutableStateOf(false) }
    var showRestoreSuccess by remember { mutableStateOf(false) }
    var googleAccount by remember { mutableStateOf(GoogleSignIn.getLastSignedInAccount(context)) }
    var signInError by remember { mutableStateOf<String?>(null) }
    var isSigningIn by remember { mutableStateOf(false) }
    var isBackingUp by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // Configuration Google Sign-In
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope("https://www.googleapis.com/auth/drive.appdata"))
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .build()
    }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isSigningIn = false
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
                googleAccount = account
                signInError = null
                Log.d(TAG, "Connexion réussie: ${account.email}")
            } catch (e: ApiException) {
                val errorMessage = when (e.statusCode) {
                    GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> context.getString(R.string.google_error_cancelled)
                    GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS -> context.getString(R.string.google_error_already_in_progress)
                    GoogleSignInStatusCodes.SIGN_IN_FAILED -> context.getString(R.string.google_error_failed)
                    GoogleSignInStatusCodes.NETWORK_ERROR -> context.getString(R.string.google_error_network)
                    12501 -> context.getString(R.string.google_error_cancelled)
                    12502 -> context.getString(R.string.google_error_already_in_progress)
                    10 -> context.getString(R.string.google_error_dev)
                    else -> context.getString(R.string.error_generic, "Google (code ${e.statusCode})")
                }
                signInError = errorMessage
                Log.e(TAG, "Erreur Google Sign-In [code=${e.statusCode}]: ${e.message}", e)
            }
        } else {
            if (result.resultCode == Activity.RESULT_CANCELED) {
                signInError = context.getString(R.string.google_error_cancelled)
            } else {
                signInError = context.getString(R.string.error_generic, result.resultCode.toString())
            }
        }
    }

    // Calculer l'heure de fin estimée
    val endHour = (scheduleHour + plan.fastingHours) % 24
    val endMinute = scheduleMinute

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.nav_settings),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ═══════════════════════════════════════
        // PROGRAMMATION
        // ═══════════════════════════════════════
        Text(
            text = stringResource(R.string.programming),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Auto-start toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoMode,
                        contentDescription = null,
                        tint = if (autoStart) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.auto_start),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = stringResource(R.string.auto_start_desc),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoStart,
                        onCheckedChange = { scope.launch { prefsManager.setAutoStart(it) } }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Schedule time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.fasting_start_time_title),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = stringResource(R.string.estimated_end, String.format(Locale.getDefault(), "%02d:%02d", endHour, endMinute), stringResource(plan.displayNameRes)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = { showTimePicker = true },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = String.format(Locale.getDefault(), "%02d:%02d", scheduleHour, scheduleMinute),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ═══════════════════════════════════════
        // APPARENCE
        // ═══════════════════════════════════════
        Text(
            text = stringResource(R.string.appearance),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.DarkMode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.dark_mode),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Switch(
                    checked = darkMode,
                    onCheckedChange = { scope.launch { prefsManager.setDarkMode(it) } }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ═══════════════════════════════════════
        // GOOGLE DRIVE
        // ═══════════════════════════════════════
        Text(
            text = stringResource(R.string.google_drive_backup),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Account status
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Cloud,
                        contentDescription = null,
                        tint = if (googleAccount != null) Color(0xFF2ECC71) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = googleAccount?.email ?: stringResource(R.string.google_not_connected),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (googleAccount != null) stringResource(R.string.google_connected_desc) else stringResource(R.string.google_login_desc),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Error display
                AnimatedVisibility(visible = signInError != null) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE74C3C).copy(alpha = 0.1f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = Color(0xFFE74C3C),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = signInError ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE74C3C)
                            )
                        }
                    }
                }

                if (googleAccount == null) {
                    Button(
                        onClick = {
                            signInError = null
                            isSigningIn = true
                            val client = GoogleSignIn.getClient(context, gso)
                            client.signOut().addOnCompleteListener {
                                signInLauncher.launch(client.signInIntent)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isSigningIn
                    ) {
                        if (isSigningIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.google_signing_in))
                        } else {
                            Icon(Icons.Rounded.Login, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.google_sign_in_with))
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    isBackingUp = true
                                    try {
                                        val credential = GoogleAccountCredential.usingOAuth2(
                                            context, listOf("https://www.googleapis.com/auth/drive.appdata")
                                        )
                                        credential.selectedAccount = googleAccount?.account
                                        val helper = DriveServiceHelper(DriveServiceHelper.getDriveService(credential))
                                        
                                        val jsonData = prefsManager.exportAllData()
                                        val success = helper.backupData(jsonData)
                                        if (success) {
                                            showBackupSuccess = true
                                            signInError = null
                                        }
                                    } catch (e: Exception) {
                                        signInError = "Erreur: ${e.message ?: "Inconnue"}"
                                        e.printStackTrace()
                                    } finally {
                                        isBackingUp = false
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isBackingUp && !isRestoring
                        ) {
                            if (isBackingUp) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.google_backup))
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    isRestoring = true
                                    try {
                                        val credential = GoogleAccountCredential.usingOAuth2(
                                            context, listOf("https://www.googleapis.com/auth/drive.appdata")
                                        )
                                        credential.selectedAccount = googleAccount?.account
                                        val helper = DriveServiceHelper(DriveServiceHelper.getDriveService(credential))
                                        
                                        val jsonData = helper.restoreData()
                                        if (jsonData != null) {
                                            prefsManager.importData(jsonData)
                                            showRestoreSuccess = true
                                            signInError = null
                                        } else {
                                            signInError = context.getString(R.string.error_restore_failed)
                                        }
                                    } catch (e: Exception) {
                                        signInError = context.getString(R.string.error_generic, e.message ?: "")
                                    } finally {
                                        isRestoring = false
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isBackingUp && !isRestoring
                        ) {
                            if (isRestoring) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.google_restore))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = {
                            GoogleSignIn.getClient(context, gso).signOut()
                            googleAccount = null
                            signInError = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.google_logout), color = Color(0xFFE74C3C))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ═══════════════════════════════════════
        // À PROPOS
        // ═══════════════════════════════════════
        Text(
            text = stringResource(R.string.about),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingRow(icon = Icons.Rounded.Info, text = stringResource(R.string.version_format, "1.0"))
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SettingRow(icon = Icons.Rounded.Favorite, text = stringResource(R.string.app_name_text))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Snackbars
    if (showBackupSuccess) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(2000)
            showBackupSuccess = false
        }
    }

    // TimePicker Dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = scheduleHour,
            initialMinute = scheduleMinute,
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = {
                Text("⏰ " + stringResource(R.string.fasting_start_time_title), fontWeight = FontWeight.Bold)
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.start_time_question),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        prefsManager.setReferenceFastingSchedule(timePickerState.hour, timePickerState.minute)
                    }
                    showTimePicker = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun SettingRow(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
