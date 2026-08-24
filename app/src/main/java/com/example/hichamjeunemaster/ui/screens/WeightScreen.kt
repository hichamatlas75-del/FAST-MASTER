package com.example.hichamjeunemaster.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.hichamjeunemaster.R
import com.example.hichamjeunemaster.data.PreferencesManager
import com.example.hichamjeunemaster.data.WeightEntry
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightScreen(prefsManager: PreferencesManager, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val weightHistory by prefsManager.weightHistory.collectAsState(initial = emptyList())
    val currentWeight by prefsManager.userWeight.collectAsState(initial = 0f)

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var weightInput by remember { mutableStateOf("") }
    
    var calendarMonth by remember { mutableStateOf(Calendar.getInstance()) }

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
    ) {
        // --- Top Bar ---
        CenterAlignedTopAppBar(
            title = { Text(stringResource(R.string.weight), fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.cancel))
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.Transparent
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            // --- Summary Card ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.MonitorWeight, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.width(20.dp))
                        Column {
                            Text(
                                text = if (currentWeight > 0) "$currentWeight kg" else "— kg",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = stringResource(R.string.current_weight),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        FloatingActionButton(
                            onClick = {
                                selectedDate = System.currentTimeMillis()
                                weightInput = if (currentWeight > 0) currentWeight.toString() else ""
                                showAddDialog = true
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.save))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // --- Calendar Section ---
            item {
                Text(
                    text = stringResource(R.string.weight_calendar),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                WeightCalendar(
                    month = calendarMonth,
                    history = weightHistory,
                    onMonthChange = { calendarMonth = it },
                    onDateClick = { date ->
                        selectedDate = date
                        val existing = weightHistory.find { isSameDay(it.dateMillis, date) }
                        weightInput = existing?.weightKg?.toString() ?: currentWeight.toString()
                        showAddDialog = true
                    }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
            }

            // --- History List ---
            item {
                Text(
                    text = stringResource(R.string.weight_history),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (weightHistory.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.no_sessions), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(weightHistory) { entry ->
                    WeightHistoryItem(
                        entry = entry,
                        onDelete = {
                            scope.launch { prefsManager.deleteWeightEntry(entry.dateMillis) }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }

    // --- Add/Edit Dialog ---
    if (showAddDialog) {
        val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()) }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(stringResource(R.string.weight_on_date, dateFormat.format(Date(selectedDate)))) },
            text = {
                Column {
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d{0,3}\\.?\\d{0,1}$"))) weightInput = it },
                        label = { Text(stringResource(R.string.weight) + " (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        suffix = { Text(stringResource(R.string.unit_kg)) }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val w = weightInput.toFloatOrNull()
                    if (w != null && w > 0) {
                        scope.launch { prefsManager.setWeight(w, selectedDate) }
                    }
                    showAddDialog = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
fun WeightCalendar(
    month: Calendar,
    history: List<WeightEntry>,
    onMonthChange: (Calendar) -> Unit,
    onDateClick: (Long) -> Unit
) {
    val monthYearFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val daysOfWeek = listOf(
        stringResource(R.string.day_mon),
        stringResource(R.string.day_tue),
        stringResource(R.string.day_wed),
        stringResource(R.string.day_thu),
        stringResource(R.string.day_fri),
        stringResource(R.string.day_sat),
        stringResource(R.string.day_sun)
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Month Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val newMonth = month.clone() as Calendar
                    newMonth.add(Calendar.MONTH, -1)
                    onMonthChange(newMonth)
                }) {
                    Icon(Icons.Rounded.ChevronLeft, contentDescription = stringResource(R.string.cancel))
                }
                Text(
                    text = monthYearFormat.format(month.time).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = {
                    val newMonth = month.clone() as Calendar
                    newMonth.add(Calendar.MONTH, 1)
                    onMonthChange(newMonth)
                }) {
                    Icon(Icons.Rounded.ChevronRight, contentDescription = stringResource(R.string.save))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Days of Week Header
            Row(modifier = Modifier.fillMaxWidth()) {
                daysOfWeek.forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Days Grid
            val days = getDaysInMonth(month)
            val rows = days.chunked(7)
            
            rows.forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { date ->
                        if (date == null) {
                            Spacer(modifier = Modifier.weight(1f))
                        } else {
                            val isToday = isSameDay(date.timeInMillis, System.currentTimeMillis())
                            val hasWeight = history.any { isSameDay(it.dateMillis, date.timeInMillis) }
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (hasWeight) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        else if (isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                        else Color.Transparent
                                    )
                                    .clickable { onDateClick(date.timeInMillis) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = date.get(Calendar.DAY_OF_MONTH).toString(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (hasWeight) MaterialTheme.colorScheme.primary 
                                               else if (isToday) MaterialTheme.colorScheme.primary
                                               else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (isToday || hasWeight) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (hasWeight) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    // Fill the last row if it's not complete
                    if (week.size < 7) {
                        repeat(7 - week.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
fun WeightHistoryItem(entry: WeightEntry, onDelete: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dateFormat.format(Date(entry.dateMillis)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${entry.weightKg} kg",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.stop_fasting), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
            }
        }
    }
}

private fun getDaysInMonth(month: Calendar): List<Calendar?> {
    val cal = month.clone() as Calendar
    cal.set(Calendar.DAY_OF_MONTH, 1)
    
    val firstDayOfWeek = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Lundi = 0
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    
    val list = mutableListOf<Calendar?>()
    repeat(firstDayOfWeek) { list.add(null) }
    
    for (i in 1..daysInMonth) {
        val day = cal.clone() as Calendar
        day.set(Calendar.DAY_OF_MONTH, i)
        list.add(day)
    }
    
    return list
}

private fun isSameDay(t1: Long, t2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = t1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = t2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
