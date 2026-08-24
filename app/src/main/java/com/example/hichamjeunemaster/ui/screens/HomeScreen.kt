package com.example.hichamjeunemaster.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.hichamjeunemaster.R
import com.example.hichamjeunemaster.data.FastingPlan
import com.example.hichamjeunemaster.data.FastingStage
import com.example.hichamjeunemaster.data.FastingStages
import com.example.hichamjeunemaster.data.PreferencesManager
import com.example.hichamjeunemaster.ui.components.CircularTimer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import com.example.hichamjeunemaster.data.AdsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(prefsManager: PreferencesManager) {
    val activity = LocalContext.current as Activity
    val scope = rememberCoroutineScope()
    val plan by prefsManager.selectedPlan.collectAsState(initial = FastingPlan.PLAN_16_8)
    val isActive by prefsManager.isFastingActive.collectAsState(initial = false)
    val startTime by prefsManager.fastingStartTime.collectAsState(initial = 0L)
    val userName by prefsManager.userName.collectAsState(initial = "")
    val scheduleHour by prefsManager.fastingScheduleHour.collectAsState(initial = 20)
    val scheduleMinute by prefsManager.fastingScheduleMinute.collectAsState(initial = 0)
    val autoStart by prefsManager.autoStartEnabled.collectAsState(initial = false)
    val sessions by prefsManager.sessions.collectAsState(initial = emptyList())

    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showTimePicker by remember { mutableStateOf(false) }
    var expandedStageId by remember { mutableIntStateOf(-1) }

    // Tick every second always (to update eating countdown too)
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000L)
        }
    }

    // Auto-start logic handled by AlarmManager via PreferencesManager

    val elapsed = if (isActive && startTime > 0) currentTime - startTime else 0L
    val isFastingPhase = elapsed < plan.fastingMillis
    
    val lastSession = sessions.lastOrNull()
    val elapsedEating = if (!isActive && lastSession != null) currentTime - (lastSession.startTimeMillis + lastSession.durationMillis) else 0L

    val progress = if (isActive) {
        if (isFastingPhase) elapsed.toFloat() / plan.fastingMillis.toFloat()
        else (elapsed - plan.fastingMillis).toFloat() / plan.eatingMillis.toFloat()
    } else {
        val nowCal = Calendar.getInstance()
        val schedCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, scheduleHour)
            set(Calendar.MINUTE, scheduleMinute)
            set(Calendar.SECOND, 0)
        }
        if (schedCal.before(nowCal)) schedCal.add(Calendar.DAY_OF_YEAR, 1)
        val timeUntilNext = schedCal.timeInMillis - nowCal.timeInMillis
        
        if (lastSession != null) {
            val totalEatingWindow = schedCal.timeInMillis - (lastSession.startTimeMillis + lastSession.durationMillis)
            // S'assurer que le dénominateur est positif et cohérent
            if (totalEatingWindow > 0) {
                (elapsedEating.toFloat() / totalEatingWindow.toFloat()).coerceIn(0f, 1f)
            } else {
                1f - (timeUntilNext.toFloat() / plan.eatingMillis.toFloat()).coerceIn(0f, 1f)
            }
        } else {
            1f - (timeUntilNext.toFloat() / plan.eatingMillis.toFloat()).coerceIn(0f, 1f)
        }
    }

    val remaining = if (isActive) {
        if (isFastingPhase) plan.fastingMillis - elapsed
        else plan.eatingMillis - (elapsed - plan.fastingMillis)
    } else {
        val nowCal = Calendar.getInstance()
        val schedCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, scheduleHour)
            set(Calendar.MINUTE, scheduleMinute)
            set(Calendar.SECOND, 0)
        }
        if (schedCal.before(nowCal)) schedCal.add(Calendar.DAY_OF_YEAR, 1)
        schedCal.timeInMillis - nowCal.timeInMillis
    }
    
    val currentIsFasting = isActive && isFastingPhase

    // Weekend logic for Flexible 16:8 (Samedi et Dimanche)
    val isWeekendFreeMode = if (plan == FastingPlan.PLAN_FLEXIBLE_16_8) {
        val now = Calendar.getInstance()
        val day = now.get(Calendar.DAY_OF_WEEK)
        val hour = now.get(Calendar.HOUR_OF_DAY)
        
        when (day) {
            Calendar.SATURDAY -> true
            Calendar.SUNDAY -> hour < scheduleHour && !isActive
            else -> false
        }
    } else false

    val isSessionComplete = isActive && elapsed >= plan.fastingMillis

    // Auto-complete session
    LaunchedEffect(isSessionComplete) {
        if (isSessionComplete) {
            // Bug Fix: On clôture dès que le temps de jeûne est écoulé 
            // pour libérer la place pour le prochain cycle.
            prefsManager.stopFasting(true)
        }
    }

    val hours = (remaining / 3600000).toInt()
    val minutes = ((remaining % 3600000) / 60000).toInt()
    val seconds = ((remaining % 60000) / 1000).toInt()
    val timeText = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)

    val eHours = (elapsed / 3600000).toInt()
    val eMinutes = ((elapsed % 3600000) / 60000).toInt()
    val eSeconds = ((elapsed % 60000) / 1000).toInt()
    val elapsedTimeText = if (isActive) String.format(Locale.getDefault(), "%02d:%02d:%02d", eHours, eMinutes, eSeconds) else null

    // Fasting stages
    val elapsedHours = elapsed / 3600000f
    val stages = remember { FastingStages.getStages() }
    val currentStage = FastingStages.getCurrentStage(elapsedHours)

    LazyColumn(
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
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ─── Header ───
        item {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (userName.isNotEmpty()) stringResource(R.string.bonjour_user, userName) else stringResource(R.string.bonjour),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (currentIsFasting) stringResource(R.string.in_fasting)
                else if (lastSession != null || isActive) stringResource(R.string.eating_window)
                else stringResource(R.string.ready_to_start),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // ─── Timer ───
        item {
            CircularTimer(
                progress = if (isWeekendFreeMode) 1f else progress.coerceIn(0f, 1f),
                timeText = if (isWeekendFreeMode) stringResource(R.string.free_mode) else timeText,
                phaseText = if (isWeekendFreeMode) stringResource(R.string.weekend_rest) 
                           else if (currentIsFasting) stringResource(R.string.fasting_lock) 
                           else if (lastSession != null || isActive) stringResource(R.string.eating_meal) 
                           else stringResource(R.string.ready_pause),
                planName = if (isWeekendFreeMode) stringResource(R.string.weekend) else stringResource(R.string.plan_name_format, stringResource(plan.displayNameRes)),
                elapsedTimeText = elapsedTimeText,
                isFasting = !isWeekendFreeMode && currentIsFasting
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Heure programmée / Heure de début effective
            val startCal = remember(startTime, isActive) {
                Calendar.getInstance().apply { if (isActive && startTime > 0) timeInMillis = startTime }
            }
            val displayHour = if (isActive && startTime > 0) startCal.get(Calendar.HOUR_OF_DAY) else scheduleHour
            val displayMinute = if (isActive && startTime > 0) startCal.get(Calendar.MINUTE) else scheduleMinute

            Surface(
                modifier = Modifier.clickable { showTimePicker = true },
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isActive) Icons.Rounded.PlayCircle else Icons.Rounded.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${if (isActive) stringResource(R.string.started_at) else stringResource(R.string.scheduled_start)} : ${String.format(Locale.getDefault(), "%02d:%02d", displayHour, displayMinute)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = stringResource(R.string.edit_time),
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Start / Stop button
            if (!isWeekendFreeMode) {
                Button(
                    onClick = {
                        scope.launch {
                            if (isActive) {
                                prefsManager.stopFasting(false)
                                // Afficher l'interstitiel à la fin du jeûne
                                AdsManager.showInterstitial(activity)
                            } else {
                                prefsManager.startFasting(plan)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isActive) Color(0xFFE74C3C) else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = if (isActive) stringResource(R.string.stop_fasting) else stringResource(R.string.start_fasting),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                // Info text for weekend
                Card(
                    modifier = Modifier.fillMaxWidth(0.8f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF2ECC71).copy(alpha = 0.1f)
                    )
                ) {
                    Text(
                        text = stringResource(R.string.weekend_info, String.format("%02d:%02d", scheduleHour, scheduleMinute)),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF2ECC71),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Plan info card
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
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(label = stringResource(R.string.stat_fasting), value = "${plan.fastingHours}h")
                    StatItem(label = stringResource(R.string.stat_eating), value = "${plan.eatingHours}h")
                    StatItem(label = stringResource(R.string.stat_level), value = stringResource(plan.difficultyRes))
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ─── Section Étapes de jeûne ───
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.fasting_stages_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.fasting_stages_subtitle),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // ─── Fasting Stage Cards ───
        items(stages, key = { it.id }) { stage ->
            val isPast = isActive && elapsedHours >= stage.endHour
            val isCurrent = isActive && elapsedHours >= stage.startHour && elapsedHours < stage.endHour
            val isFuture = !isActive || elapsedHours < stage.startHour
            val isExpanded = expandedStageId == stage.id

            FastingStageCard(
                stage = stage,
                isPast = isPast,
                isCurrent = isCurrent,
                isFuture = isFuture,
                isExpanded = isExpanded,
                stageProgress = if (isCurrent) FastingStages.getStageProgress(elapsedHours, stage) else if (isPast) 1f else 0f,
                onToggleExpand = {
                    expandedStageId = if (isExpanded) -1 else stage.id
                }
            )

            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // ─── TimePicker Dialog ───
    if (showTimePicker) {
        val startCal = remember(startTime, isActive) {
            Calendar.getInstance().apply { if (isActive && startTime > 0) timeInMillis = startTime }
        }
        val initialHour = if (isActive && startTime > 0) startCal.get(Calendar.HOUR_OF_DAY) else scheduleHour
        val initialMinute = if (isActive && startTime > 0) startCal.get(Calendar.MINUTE) else scheduleMinute

        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = {
                Text(if (isActive) stringResource(R.string.edit_start_time) else stringResource(R.string.fasting_start_time_title), fontWeight = FontWeight.Bold)
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isActive) stringResource(R.string.adjust_start_time_desc) 
                               else stringResource(R.string.schedule_start_time_desc),
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
                        if (isActive) {
                            val newStartTime = Calendar.getInstance().apply {
                                timeInMillis = startTime
                                set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                                set(Calendar.MINUTE, timePickerState.minute)
                                set(Calendar.SECOND, 0)
                                // Si l'heure choisie est dans le futur par rapport à "maintenant", 
                                // on suppose que c'était hier si on est proche de minuit, ou on reste cohérent.
                                if (timeInMillis > System.currentTimeMillis()) {
                                    add(Calendar.DAY_OF_YEAR, -1)
                                }
                            }.timeInMillis
                            prefsManager.updateActiveFastingStartTime(newStartTime)
                        } else {
                            prefsManager.setOneTimeFastingSchedule(timePickerState.hour, timePickerState.minute)
                        }
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

// ═══════════════════════════════════════════
// Carte d'étape de jeûne — Style Fastic
// ═══════════════════════════════════════════

@Composable
private fun FastingStageCard(
    stage: FastingStage,
    isPast: Boolean,
    isCurrent: Boolean,
    isFuture: Boolean,
    isExpanded: Boolean,
    stageProgress: Float,
    onToggleExpand: () -> Unit
) {
    val stageColor = Color(stage.color)
    val animatedProgress by animateFloatAsState(
        targetValue = stageProgress,
        animationSpec = tween(800),
        label = "stageProgress"
    )

    // Pulse animation for current stage
    val pulseAnim = rememberInfiniteTransition(label = "pulse_${stage.id}")
    val pulseScale by pulseAnim.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val cardAlpha = when {
        isCurrent -> 1f
        isPast -> 0.7f
        else -> 0.5f
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(cardAlpha)
            .clickable { onToggleExpand() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent)
                stageColor.copy(alpha = 0.12f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isCurrent) 6.dp else 1.dp
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ─── Header Row ───
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Emoji + Status
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCurrent) stageColor.copy(alpha = 0.2f)
                            else if (isPast) Color(0xFF2ECC71).copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPast) {
                        Text("✅", fontSize = 20.sp)
                    } else {
                        Text(stage.emoji, fontSize = 22.sp)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(stage.titleRes),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isCurrent) stageColor else MaterialTheme.colorScheme.onBackground
                        )
                        if (isCurrent) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = stageColor.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = stringResource(R.string.in_progress),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = stageColor
                                )
                            }
                        }
                    }
                    Text(
                        text = "${stage.startHour}h → ${stage.endHour}h",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Expand arrow
                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = "Détails",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Short description
            Text(
                text = stringResource(stage.shortDescriptionRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                overflow = TextOverflow.Ellipsis
            )

            // Progress bar for current/past stages
            if (isCurrent || isPast) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = stageColor,
                    trackColor = stageColor.copy(alpha = 0.15f)
                )
            }

            // ─── Expanded content ───
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    // Detailed description
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Text(
                            text = stringResource(stage.detailedDescriptionRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(12.dp),
                            lineHeight = 20.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Body effects
                    Text(
                        text = stringResource(R.string.body_effects_title),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = stageColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    stage.bodyEffectsRes.forEach { effectRes ->
                        Text(
                            text = stringResource(effectRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(vertical = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Tip
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF3E0)
                        )
                    ) {
                        Text(
                            text = stringResource(stage.tipRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF5D4037),
                            modifier = Modifier.padding(12.dp),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
