package com.example.hichamjeunemaster.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.res.stringResource
import com.example.hichamjeunemaster.R
import com.example.hichamjeunemaster.data.FastingPlan
import com.example.hichamjeunemaster.data.PreferencesManager
import com.example.hichamjeunemaster.data.WeightEntry
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.io.File
import java.io.FileOutputStream
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCropActivity

@Composable
fun ProfileScreen(prefsManager: PreferencesManager, onNavigateToWeight: () -> Unit) {
    val scope = rememberCoroutineScope()
    val userName by prefsManager.userName.collectAsState(initial = "")
    val photoUri by prefsManager.photoUri.collectAsState(initial = "")
    val plan by prefsManager.selectedPlan.collectAsState(initial = FastingPlan.PLAN_16_8)
    val sessions by prefsManager.sessions.collectAsState(initial = emptyList())
    val weight by prefsManager.userWeight.collectAsState(initial = 0f)
    val height by prefsManager.userHeight.collectAsState(initial = 0)
    val weightHistory by prefsManager.weightHistory.collectAsState(initial = emptyList())

    var showNameDialog by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }
    var showWeightDialog by remember { mutableStateOf(false) }
    var weightInput by remember { mutableStateOf("") }
    var showHeightDialog by remember { mutableStateOf(false) }
    var heightInput by remember { mutableStateOf("") }
    
    // État local pour forcer le rafraîchissement de l'image (contourne le cache Coil)
    var imageRefreshKey by remember { mutableStateOf(System.currentTimeMillis()) }

    val context = LocalContext.current

    // Launcher pour le rognage (uCrop)
    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val resultUri = result.data?.let { UCrop.getOutput(it) }
            resultUri?.let { uri ->
                scope.launch {
                    try {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val file = File(context.filesDir, "profile_photo.jpg")
                        val outputStream = FileOutputStream(file)
                        inputStream?.copyTo(outputStream)
                        inputStream?.close()
                        outputStream.close()
                        
                        prefsManager.setPhotoUri(file.absolutePath)
                        imageRefreshKey = System.currentTimeMillis()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { sourceUri ->
            val destinationUri = Uri.fromFile(File(context.cacheDir, "temp_crop_image.jpg"))
            val uCrop = UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(1f, 1f)
                .withMaxResultSize(800, 800)
                .withOptions(UCrop.Options().apply {
                    setCircleDimmedLayer(true) // Calque circulaire
                    setShowCropGrid(false)
                    setCompressionFormat(android.graphics.Bitmap.CompressFormat.JPEG)
                    setCompressionQuality(90)
                    setHideBottomControls(false)
                    setFreeStyleCropEnabled(false)
                    setAllowedGestures(UCropActivity.SCALE, UCropActivity.ROTATE, UCropActivity.ALL)
                    // Personnalisation des couleurs
                    setToolbarColor(0xFF2ECC71.toInt())
                    setStatusBarColor(0xFF2ECC71.toInt())
                    setActiveControlsWidgetColor(0xFF2ECC71.toInt())
                })
            
            cropLauncher.launch(uCrop.getIntent(context))
        }
    }

    val totalFasts = sessions.size
    val completedFasts = sessions.count { it.isCompleted }
    val totalHours = sessions.sumOf { it.durationMillis } / 3600000

    // Calcul IMC
    val bmi = if (weight > 0 && height > 0) {
        val heightM = height / 100f
        weight / (heightM * heightM)
    } else 0f

    val bmiCategory = when {
        bmi <= 0 -> ""
        bmi < 18.5f -> stringResource(R.string.bmi_underweight)
        bmi < 25f -> stringResource(R.string.bmi_normal)
        bmi < 30f -> stringResource(R.string.bmi_overweight)
        else -> stringResource(R.string.bmi_obese)
    }

    val bmiColor = when {
        bmi <= 0 -> Color.Gray
        bmi < 18.5f -> Color(0xFF42A5F5) // Bleu
        bmi < 25f -> Color(0xFF2ECC71) // Vert
        bmi < 30f -> Color(0xFFFF9100) // Orange
        else -> Color(0xFFE74C3C) // Rouge
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.my_profile),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Profile photo
        Box(
            modifier = Modifier.size(130.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            if (photoUri.isNotEmpty()) {
                key(imageRefreshKey) {
                    AsyncImage(
                        model = File(photoUri),
                        contentDescription = "Photo de profil",
                        modifier = Modifier
                            .size(130.dp)
                            .clip(CircleShape)
                            .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            .clickable { photoPickerLauncher.launch("image/*") },
                        contentScale = ContentScale.Crop
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                                )
                            )
                        )
                        .border(3.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
                        .clickable { photoPickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CameraAlt,
                        contentDescription = "Ajouter photo",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Edit badge
            Surface(
                modifier = Modifier.size(38.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Modifier",
                        tint = Color.White,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { photoPickerLauncher.launch("image/*") }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Name
        if (userName.isNotEmpty()) {
            Text(
                text = userName,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = (-1).sp
            )
        }

        TextButton(onClick = {
            nameInput = userName
            showNameDialog = true
        }) {
            Text(
                text = if (userName.isEmpty()) "✏️ " + stringResource(R.string.edit_name) else "✏️ " + stringResource(R.string.edit_name),
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Current plan chip
        AssistChip(
            onClick = {},
            label = { Text(stringResource(R.string.plan_name_format, stringResource(plan.displayNameRes)) + " • " + stringResource(plan.difficultyRes)) },
            shape = RoundedCornerShape(20.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ═══════════════════════════════════════
        // SECTION MON CORPS
        // ═══════════════════════════════════════
        Text(
            text = stringResource(R.string.my_body),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Weight card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        weightInput = if (weight > 0) weight.toInt().toString() else ""
                        showWeightDialog = true
                    },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MonitorWeight,
                        contentDescription = null,
                        tint = Color(0xFF42A5F5),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (weight > 0) String.format(Locale.getDefault(), "%.1f kg", weight) else "— kg",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.weight),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Height card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        heightInput = if (height > 0) height.toString() else ""
                        showHeightDialog = true
                    },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Height,
                        contentDescription = null,
                        tint = Color(0xFF7C4DFF),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (height > 0) "$height cm" else "— cm",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.height),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // IMC Card
        if (bmi > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = bmiColor.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.bmi_title),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f", bmi),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = bmiColor
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.bmi_label),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = bmiColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = bmiCategory,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = bmiColor
                        )
                    }
                }
            }
        }

        val sweepBrush = Brush.sweepGradient(
            listOf(
                Color(0xFF00C853),
                Color(0xFF64FFDA),
                Color(0xFF00E5FF),
                Color(0xFF00C853)
            )
        )

        // Weight History (mini)
        if (weightHistory.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.weight_evolution),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        TextButton(onClick = onNavigateToWeight) {
                            Text(stringResource(R.string.view_all), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (weightHistory.size >= 2) {
                        val recentEntries = weightHistory.take(5).reversed()
                        val dateFormat = remember { SimpleDateFormat("dd/MM", Locale.FRANCE) }

                        recentEntries.forEachIndexed { index, entry ->
                            val previousWeight = if (index > 0) recentEntries[index - 1].weightKg else entry.weightKg
                            val diff = entry.weightKg - previousWeight
                            val diffColor = when {
                                diff < -0.1f -> Color(0xFF2ECC71) // Perte = vert
                                diff > 0.1f -> Color(0xFFE74C3C) // Gain = rouge
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            val diffText = when {
                                diff < -0.1f -> "▼ ${String.format(Locale.getDefault(), "%.1f", -diff)} kg"
                                diff > 0.1f -> "▲ +${String.format(Locale.getDefault(), "%.1f", diff)} kg"
                                else -> stringResource(R.string.stable)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dateFormat.format(Date(entry.dateMillis)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${entry.weightKg} kg",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = diffText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = diffColor
                                )
                            }

                            if (index < recentEntries.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.weight_evolution_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ═══════════════════════════════════════
        // SECTION STATISTIQUES
        // ═══════════════════════════════════════
        Text(
            text = stringResource(R.string.statistics),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Stats cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProfileStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Timer,
                value = "$totalFasts",
                label = stringResource(R.string.stat_total_fasts),
                color = MaterialTheme.colorScheme.primary
            )
            ProfileStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.LocalFireDepartment,
                value = "$completedFasts",
                label = stringResource(R.string.stat_completed),
                color = Color(0xFFFF9100)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "\u23F1\uFE0F",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "${totalHours}h",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.total_fasting_hours),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // ═══════════════════════════════════════
    // DIALOGS
    // ═══════════════════════════════════════

    // Name edit dialog
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text(stringResource(R.string.your_name)) },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text(stringResource(R.string.name)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { prefsManager.setUserName(nameInput) }
                    showNameDialog = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    // Weight dialog
    if (showWeightDialog) {
        AlertDialog(
            onDismissRequest = { showWeightDialog = false },
            title = { Text("⚖️ " + stringResource(R.string.your_weight)) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.enter_weight_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { newVal ->
                            // Accepter uniquement les chiffres et un point
                            if (newVal.isEmpty() || newVal.matches(Regex("^\\d{0,3}\\.?\\d{0,1}$"))) {
                                weightInput = newVal
                            }
                        },
                        label = { Text(stringResource(R.string.weight) + " (" + stringResource(R.string.unit_kg) + ")") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        suffix = { Text(stringResource(R.string.unit_kg)) }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val w = weightInput.toFloatOrNull()
                    if (w != null && w > 0) {
                        scope.launch { prefsManager.setWeight(w) }
                    }
                    showWeightDialog = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showWeightDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    // Height dialog
    if (showHeightDialog) {
        AlertDialog(
            onDismissRequest = { showHeightDialog = false },
            title = { Text("📏 " + stringResource(R.string.your_height)) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.enter_height_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = heightInput,
                        onValueChange = { newVal ->
                            if (newVal.isEmpty() || newVal.matches(Regex("^\\d{0,3}$"))) {
                                heightInput = newVal
                            }
                        },
                        label = { Text(stringResource(R.string.height) + " (" + stringResource(R.string.unit_cm) + ")") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        suffix = { Text(stringResource(R.string.unit_cm)) }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val h = heightInput.toIntOrNull()
                    if (h != null && h > 0) {
                        scope.launch { prefsManager.setHeight(h) }
                    }
                    showHeightDialog = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showHeightDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun ProfileStatCard(
    modifier: Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
