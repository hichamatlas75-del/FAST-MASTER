package com.example.hichamjeunemaster.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import android.util.Base64
import java.io.File
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.Calendar
import com.example.hichamjeunemaster.R

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "jeune_master_prefs")

class PreferencesManager(private val context: Context) {

    companion object {
        val USER_NAME = stringPreferencesKey("user_name")
        val PHOTO_URI = stringPreferencesKey("photo_uri")
        val SELECTED_PLAN = stringPreferencesKey("selected_plan")
        val IS_FASTING_ACTIVE = booleanPreferencesKey("is_fasting_active")
        val FASTING_START_TIME = longPreferencesKey("fasting_start_time")
        val FASTING_PLAN_ACTIVE = stringPreferencesKey("fasting_plan_active")
        val SESSIONS_JSON = stringPreferencesKey("sessions_json")
        val DARK_MODE = booleanPreferencesKey("dark_mode")

        // Nouvelles clés — Programmation horaire
        val FASTING_SCHEDULE_HOUR = intPreferencesKey("fasting_schedule_hour")
        val FASTING_SCHEDULE_MINUTE = intPreferencesKey("fasting_schedule_minute")
        val REFERENCE_SCHEDULE_HOUR = intPreferencesKey("reference_schedule_hour")
        val REFERENCE_SCHEDULE_MINUTE = intPreferencesKey("reference_schedule_minute")
        val AUTO_START_ENABLED = booleanPreferencesKey("auto_start_enabled")
        // Date (timestamp) du dernier démarrage pour sauter l'auto-start du jour en cours
        val LAST_MANUAL_START_DATE = longPreferencesKey("last_manual_start_date")

        // Nouvelles clés — Corps
        val USER_WEIGHT = floatPreferencesKey("user_weight")
        val USER_HEIGHT = intPreferencesKey("user_height")
        val WEIGHT_HISTORY_JSON = stringPreferencesKey("weight_history_json")
    }

    // ─── Existant ───
    val userName: Flow<String> = context.dataStore.data.map { it[USER_NAME] ?: "" }
    val photoUri: Flow<String> = context.dataStore.data.map { it[PHOTO_URI] ?: "" }

    val selectedPlan: Flow<FastingPlan> = context.dataStore.data.map {
        try { FastingPlan.valueOf(it[SELECTED_PLAN] ?: FastingPlan.PLAN_16_8.name) }
        catch (e: Exception) { FastingPlan.PLAN_16_8 }
    }

    val isFastingActive: Flow<Boolean> = context.dataStore.data.map { it[IS_FASTING_ACTIVE] ?: false }
    val fastingStartTime: Flow<Long> = context.dataStore.data.map { it[FASTING_START_TIME] ?: 0L }

    val activeFastingPlan: Flow<FastingPlan> = context.dataStore.data.map {
        try { FastingPlan.valueOf(it[FASTING_PLAN_ACTIVE] ?: FastingPlan.PLAN_16_8.name) }
        catch (e: Exception) { FastingPlan.PLAN_16_8 }
    }

    val darkMode: Flow<Boolean> = context.dataStore.data.map { it[DARK_MODE] ?: false }

    val sessions: Flow<List<FastingSession>> = context.dataStore.data.map { prefs ->
        parseSessionsJson(prefs[SESSIONS_JSON] ?: "[]")
    }

    // ─── Nouveau : Programmation horaire ───
    val fastingScheduleHour: Flow<Int> = context.dataStore.data.map { it[FASTING_SCHEDULE_HOUR] ?: 20 }
    val fastingScheduleMinute: Flow<Int> = context.dataStore.data.map { it[FASTING_SCHEDULE_MINUTE] ?: 0 }
    val referenceScheduleHour: Flow<Int> = context.dataStore.data.map { it[REFERENCE_SCHEDULE_HOUR] ?: it[FASTING_SCHEDULE_HOUR] ?: 20 }
    val referenceScheduleMinute: Flow<Int> = context.dataStore.data.map { it[REFERENCE_SCHEDULE_MINUTE] ?: it[FASTING_SCHEDULE_MINUTE] ?: 0 }
    val autoStartEnabled: Flow<Boolean> = context.dataStore.data.map { it[AUTO_START_ENABLED] ?: false }
    // Suppression de skipAutoStartToday (plus nécessaire en Flow)

    // ─── Nouveau : Corps ───
    val userWeight: Flow<Float> = context.dataStore.data.map { it[USER_WEIGHT] ?: 0f }
    val userHeight: Flow<Int> = context.dataStore.data.map { it[USER_HEIGHT] ?: 0 }
    val weightHistory: Flow<List<WeightEntry>> = context.dataStore.data.map { prefs ->
        parseWeightHistory(prefs[WEIGHT_HISTORY_JSON] ?: "[]")
    }

    // ─── Méthodes existantes ───

    suspend fun setUserName(name: String) {
        context.dataStore.edit { it[USER_NAME] = name }
    }

    suspend fun setPhotoUri(uri: String) {
        context.dataStore.edit { it[PHOTO_URI] = uri }
    }

    suspend fun setSelectedPlan(plan: FastingPlan) {
        context.dataStore.edit { 
            it[SELECTED_PLAN] = plan.name 
            // Bug Fix: Si un jeûne est actif, on met aussi à jour le plan de la session en cours
            // pour que les compteurs et notifications restent synchronisés.
            if (it[IS_FASTING_ACTIVE] == true) {
                it[FASTING_PLAN_ACTIVE] = plan.name
            }
        }
        
        // Reprogrammer les notifications si un jeûne est en cours
        val prefs = context.dataStore.data.first()
        if (prefs[IS_FASTING_ACTIVE] == true) {
            val startTime = prefs[FASTING_START_TIME] ?: 0L
            if (startTime > 0L) {
                cancelFastingNotifications()
                scheduleFastingNotifications(plan, startTime)
            }
        }
        
        // Mettre à jour l'alarme d'auto-start (utile si le plan change la durée de l'alimentation)
        scheduleAutoStartAlarm()
    }

    suspend fun startFasting(plan: FastingPlan) {
        val startTime = System.currentTimeMillis()
        context.dataStore.edit {
            it[IS_FASTING_ACTIVE] = true
            it[FASTING_START_TIME] = startTime
            it[FASTING_PLAN_ACTIVE] = plan.name
            // Enregistrer la date pour sauter l'auto‑start d'aujourd'hui
            it[LAST_MANUAL_START_DATE] = startTime
            
            // Bug Fix: Une fois commencé, on remet l'heure programmée à la valeur de référence
            // pour que l'override de la page d'accueil ne s'applique qu'une seule fois.
            it[FASTING_SCHEDULE_HOUR] = it[REFERENCE_SCHEDULE_HOUR] ?: it[FASTING_SCHEDULE_HOUR] ?: 20
            it[FASTING_SCHEDULE_MINUTE] = it[REFERENCE_SCHEDULE_MINUTE] ?: it[FASTING_SCHEDULE_MINUTE] ?: 0
        }
        // Annuler l'alarme auto‑start du jour en cours et reprogrammer pour demain
        cancelAutoStartAlarm()
        scheduleAutoStartAlarm()
        scheduleFastingNotifications(plan, startTime)
    }

    /** Démarrer un jeûne avec une heure de début personnalisée */
    suspend fun startFastingAt(plan: FastingPlan, startTimeMillis: Long) {
        context.dataStore.edit {
            it[IS_FASTING_ACTIVE] = true
            it[FASTING_START_TIME] = startTimeMillis
            it[FASTING_PLAN_ACTIVE] = plan.name
            it[LAST_MANUAL_START_DATE] = startTimeMillis
            
            // Bug Fix: Reset référence
            it[FASTING_SCHEDULE_HOUR] = it[REFERENCE_SCHEDULE_HOUR] ?: it[FASTING_SCHEDULE_HOUR] ?: 20
            it[FASTING_SCHEDULE_MINUTE] = it[REFERENCE_SCHEDULE_MINUTE] ?: it[FASTING_SCHEDULE_MINUTE] ?: 0
        }
        cancelAutoStartAlarm()
        scheduleAutoStartAlarm()
        scheduleFastingNotifications(plan, startTimeMillis)
    }

    suspend fun stopFasting(completed: Boolean) {
        context.dataStore.edit { prefs ->
            val startTime = prefs[FASTING_START_TIME] ?: 0L
            val planName = prefs[FASTING_PLAN_ACTIVE] ?: FastingPlan.PLAN_16_8.name
            val plan = try { FastingPlan.valueOf(planName) } catch (e: Exception) { FastingPlan.PLAN_16_8 }

            val sessions = JSONArray(prefs[SESSIONS_JSON] ?: "[]")
            sessions.put(JSONObject().apply {
                put("id", System.currentTimeMillis())
                put("plan", plan.name)
                put("startTime", startTime)
                put("endTime", System.currentTimeMillis())
                put("completed", completed)
            })
            prefs[SESSIONS_JSON] = sessions.toString()
            prefs[IS_FASTING_ACTIVE] = false
            prefs[FASTING_START_TIME] = 0L

        }
        cancelFastingNotifications()
        // Reprogrammer les alarmes (surtout si l'heure a changé ou pour le rappel de 30min)
        scheduleAutoStartAlarm()
    }

    private fun scheduleFastingNotifications(plan: FastingPlan, startTimeMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // 1. Notification immédiate de début
        val helper = NotificationHelper(context)
        helper.showNotification(
            context.getString(R.string.notif_fasting_start_title),
            context.getString(R.string.notif_fasting_start_msg, plan.fastingHours)
        )

        // 2. Notification de fin de jeûne (Début rupture)
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("title", context.getString(R.string.notif_eating_start_title))
            putExtra("message", context.getString(R.string.notif_eating_start_msg, plan.fastingHours))
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context, 1001, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = startTimeMillis + plan.fastingMillis
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }

        // 3. Rappel de fin de jeûne (30 min avant)
        val reminderTime = triggerTime - 30 * 60 * 1000L
        if (reminderTime > System.currentTimeMillis()) {
            val reminderIntent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("title", context.getString(R.string.notif_fasting_reminder_title))
                putExtra("message", context.getString(R.string.notif_fasting_reminder_msg))
                putExtra("is_reminder", true)
                putExtra("reminder_type", "fasting_end")
            }

            val reminderPendingIntent = PendingIntent.getBroadcast(
                context, 1002, reminderIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, reminderPendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTime, reminderPendingIntent)
            }
        }
    }


    private fun cancelFastingNotifications() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 1001, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        
        // Annuler aussi le rappel
        val reminderIntent = Intent(context, NotificationReceiver::class.java)
        val reminderPendingIntent = PendingIntent.getBroadcast(
            context, 1002, reminderIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(reminderPendingIntent)
    }


    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[DARK_MODE] = enabled }
    }

    // ─── Nouvelles méthodes : Programmation ───

    /** Met à jour l'heure de référence (depuis les réglages) */
    suspend fun setReferenceFastingSchedule(hour: Int, minute: Int) {
        context.dataStore.edit {
            it[REFERENCE_SCHEDULE_HOUR] = hour
            it[REFERENCE_SCHEDULE_MINUTE] = minute
            it[FASTING_SCHEDULE_HOUR] = hour
            it[FASTING_SCHEDULE_MINUTE] = minute
        }
        scheduleAutoStartAlarm()
    }

    /** Met à jour l'heure pour le prochain démarrage uniquement (depuis l'accueil) */
    suspend fun setOneTimeFastingSchedule(hour: Int, minute: Int) {
        context.dataStore.edit {
            it[FASTING_SCHEDULE_HOUR] = hour
            it[FASTING_SCHEDULE_MINUTE] = minute
        }
        scheduleAutoStartAlarm()
    }

    suspend fun setAutoStart(enabled: Boolean) {
        context.dataStore.edit { it[AUTO_START_ENABLED] = enabled }
        // On reprogramme toujours pour mettre à jour l'alarme d'auto-start (activée ou non)
        // tout en gardant le rappel de 30 min actif.
        scheduleAutoStartAlarm()
    }


    /** Met à jour l'heure de début du jeûne en cours */
    suspend fun updateActiveFastingStartTime(newStartTimeMillis: Long) {
        var plan: FastingPlan? = null
        context.dataStore.edit {
            if (it[IS_FASTING_ACTIVE] == true) {
                it[FASTING_START_TIME] = newStartTimeMillis
                val planName = it[FASTING_PLAN_ACTIVE] ?: FastingPlan.PLAN_16_8.name
                plan = try { FastingPlan.valueOf(planName) } catch (e: Exception) { FastingPlan.PLAN_16_8 }
            }
        }
        plan?.let { 
            cancelFastingNotifications()
            scheduleFastingNotifications(it, newStartTimeMillis)
        }
    }

    fun scheduleAutoStartAlarm() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = "com.fastmaster.app.ACTION_AUTO_START"
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context, 2002, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Récupérer l'heure programmée (on utilise des valeurs par défaut si non encore en DataStore, 
        GlobalScope.launch(Dispatchers.IO) {
            val prefs = context.dataStore.data.first()
            val hour = prefs[FASTING_SCHEDULE_HOUR] ?: 20
            val minute = prefs[FASTING_SCHEDULE_MINUTE] ?: 0
            val enabled = prefs[AUTO_START_ENABLED] ?: false
            
            val lastManualDate = prefs[LAST_MANUAL_START_DATE] ?: 0L
            val today = Calendar.getInstance()
            val lastManualCal = Calendar.getInstance().apply { timeInMillis = lastManualDate }
            val isLastManualToday = lastManualDate > 0L && 
                today.get(Calendar.YEAR) == lastManualCal.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == lastManualCal.get(Calendar.DAY_OF_YEAR)
            
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                
                // Si on a déjà démarré un jeûne aujourd'hui, on programme l'auto-start pour demain
                if (isLastManualToday) {
                    add(Calendar.DAY_OF_YEAR, 1)
                } else if (before(today)) {
                    // Si l'heure d'aujourd'hui est déjà passée, on programme pour demain
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            // 1. Action d'Auto-Démarrage (seulement si activé)
            if (enabled) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                }
            } else {
                // Bug Fix: Si désactivé, on s'assure d'annuler toute alarme 2002 existante
                alarmManager.cancel(pendingIntent)
            }

            // 2. Rappel de fin de repas (30 min avant le début du jeûne)
            val reminderTime = calendar.timeInMillis - 30 * 60 * 1000L
            // Bug Fix: Si on est déjà dans la fenêtre des 30 min avant le début du jeûne aujourd'hui, 
            // on planifie le rappel pour demain.
            val finalReminderTime = if (reminderTime <= System.currentTimeMillis()) {
                reminderTime + 24 * 60 * 60 * 1000L
            } else {
                reminderTime
            }

            val reminderIntent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("title", context.getString(R.string.notif_eating_reminder_title))
                putExtra("message", context.getString(R.string.notif_eating_reminder_msg))
                putExtra("is_reminder", true)
                putExtra("reminder_type", "eating_end")
            }

            val reminderPendingIntent = PendingIntent.getBroadcast(
                context, 2003, reminderIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, finalReminderTime, reminderPendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, finalReminderTime, reminderPendingIntent)
            }
        }
    }


    /** 
     * Annule l'auto-démarrage. 
     * Note: Le rappel reste géré par scheduleAutoStartAlarm qui décide de le garder ou non.
     */
    fun cancelAutoStartAlarm() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = "com.fastmaster.app.ACTION_AUTO_START"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 2002, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)

        // Annuler aussi le rappel (2003)
        val reminderIntent = Intent(context, NotificationReceiver::class.java)
        val reminderPendingIntent = PendingIntent.getBroadcast(
            context, 2003, reminderIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(reminderPendingIntent)
    }



    // ─── Nouvelles méthodes : Corps ───

    /** Enregistre le poids pour une date donnée (par défaut aujourd'hui) */
    suspend fun setWeight(kg: Float, dateMillis: Long = System.currentTimeMillis()) {
        context.dataStore.edit { prefs ->
            prefs[USER_WEIGHT] = kg
            
            val history = JSONArray(prefs[WEIGHT_HISTORY_JSON] ?: "[]")
            val newEntries = mutableListOf<JSONObject>()
            
            // On vérifie si une entrée existe déjà pour ce jour précis
            val calNew = Calendar.getInstance().apply { timeInMillis = dateMillis }
            var updated = false
            
            for (i in 0 until history.length()) {
                val obj = history.getJSONObject(i)
                val calOld = Calendar.getInstance().apply { timeInMillis = obj.getLong("date") }
                
                if (calOld.get(Calendar.YEAR) == calNew.get(Calendar.YEAR) &&
                    calOld.get(Calendar.DAY_OF_YEAR) == calNew.get(Calendar.DAY_OF_YEAR)) {
                    // Update existing
                    obj.put("weight", kg.toDouble())
                    obj.put("date", dateMillis) // Optionnel: garder la date originale ou mettre à jour
                    updated = true
                }
                newEntries.add(obj)
            }
            
            if (!updated) {
                newEntries.add(JSONObject().apply {
                    put("date", dateMillis)
                    put("weight", kg.toDouble())
                })
            }
            
            // Re-trier par date décroissante
            val sortedArray = JSONArray()
            newEntries.sortedByDescending { it.getLong("date") }.forEach { sortedArray.put(it) }
            
            prefs[WEIGHT_HISTORY_JSON] = sortedArray.toString()
        }
    }

    suspend fun deleteWeightEntry(dateMillis: Long) {
        context.dataStore.edit { prefs ->
            val history = JSONArray(prefs[WEIGHT_HISTORY_JSON] ?: "[]")
            val newHistory = JSONArray()
            val calTarget = Calendar.getInstance().apply { timeInMillis = dateMillis }
            
            for (i in 0 until history.length()) {
                val obj = history.getJSONObject(i)
                val calOld = Calendar.getInstance().apply { timeInMillis = obj.getLong("date") }
                
                if (!(calOld.get(Calendar.YEAR) == calTarget.get(Calendar.YEAR) &&
                    calOld.get(Calendar.DAY_OF_YEAR) == calTarget.get(Calendar.DAY_OF_YEAR))) {
                    newHistory.put(obj)
                }
            }
            prefs[WEIGHT_HISTORY_JSON] = newHistory.toString()
        }
    }

    suspend fun setHeight(cm: Int) {
        context.dataStore.edit { it[USER_HEIGHT] = cm }
    }

    suspend fun exportAllData(): String {
        var jsonString = ""
        context.dataStore.edit { prefs ->
            jsonString = exportDataJson(prefs)
        }
        return jsonString
    }

    fun exportDataJson(prefs: Preferences): String {
        return JSONObject().apply {
            put("userName", prefs[USER_NAME] ?: "")
            
            val photoUri = prefs[PHOTO_URI] ?: ""
            put("photoUri", photoUri)
            
            var base64Photo = ""
            if (photoUri.isNotEmpty()) {
                try {
                    val file = File(photoUri)
                    if (file.exists()) {
                        val bytes = file.readBytes()
                        base64Photo = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            put("photoBase64", base64Photo)
            put("selectedPlan", prefs[SELECTED_PLAN] ?: FastingPlan.PLAN_16_8.name)
            put("sessions", JSONArray(prefs[SESSIONS_JSON] ?: "[]"))
            put("darkMode", prefs[DARK_MODE] ?: false)
            put("weight", (prefs[USER_WEIGHT] ?: 0f).toDouble())
            put("height", prefs[USER_HEIGHT] ?: 0)
            put("scheduleHour", prefs[FASTING_SCHEDULE_HOUR] ?: 20)
            put("scheduleMinute", prefs[FASTING_SCHEDULE_MINUTE] ?: 0)
            put("autoStart", prefs[AUTO_START_ENABLED] ?: false)
            put("weightHistory", JSONArray(prefs[WEIGHT_HISTORY_JSON] ?: "[]"))
            put("isFastingActive", prefs[IS_FASTING_ACTIVE] ?: false)
            put("fastingStartTime", prefs[FASTING_START_TIME] ?: 0L)
            put("fastingPlanActive", prefs[FASTING_PLAN_ACTIVE] ?: FastingPlan.PLAN_16_8.name)
            put("lastManualStartDate", prefs[LAST_MANUAL_START_DATE] ?: 0L)
        }.toString()
    }

    suspend fun importData(jsonString: String) {
        val json = JSONObject(jsonString)
        context.dataStore.edit { prefs ->
            prefs[USER_NAME] = json.optString("userName", "")
            
            val base64Photo = json.optString("photoBase64", "")
            if (base64Photo.isNotEmpty()) {
                try {
                    val bytes = Base64.decode(base64Photo, Base64.NO_WRAP)
                    val file = File(context.filesDir, "profile_photo.jpg")
                    file.writeBytes(bytes)
                    prefs[PHOTO_URI] = file.absolutePath
                } catch (e: Exception) {
                    e.printStackTrace()
                    prefs[PHOTO_URI] = json.optString("photoUri", "")
                }
            } else {
                prefs[PHOTO_URI] = json.optString("photoUri", "")
            }
            prefs[SELECTED_PLAN] = json.optString("selectedPlan", FastingPlan.PLAN_16_8.name)
            prefs[SESSIONS_JSON] = json.optJSONArray("sessions")?.toString() ?: "[]"
            prefs[DARK_MODE] = json.optBoolean("darkMode", false)
            prefs[USER_WEIGHT] = json.optDouble("weight", 0.0).toFloat()
            prefs[USER_HEIGHT] = json.optInt("height", 0)
            prefs[FASTING_SCHEDULE_HOUR] = json.optInt("scheduleHour", 20)
            prefs[FASTING_SCHEDULE_MINUTE] = json.optInt("scheduleMinute", 0)
            prefs[AUTO_START_ENABLED] = json.optBoolean("autoStart", false)
            prefs[WEIGHT_HISTORY_JSON] = json.optJSONArray("weightHistory")?.toString() ?: "[]"
            prefs[IS_FASTING_ACTIVE] = json.optBoolean("isFastingActive", false)
            prefs[FASTING_START_TIME] = json.optLong("fastingStartTime", 0L)
            prefs[FASTING_PLAN_ACTIVE] = json.optString("fastingPlanActive", FastingPlan.PLAN_16_8.name)
            prefs[LAST_MANUAL_START_DATE] = json.optLong("lastManualStartDate", 0L)
        }
        // Bug Fix: Recalculer les alarmes après import
        scheduleAutoStartAlarm()
    }

    // ─── Parsers ───

    private fun parseSessionsJson(json: String): List<FastingSession> {
        val list = mutableListOf<FastingSession>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    FastingSession(
                        id = obj.getLong("id"),
                        plan = try { FastingPlan.valueOf(obj.getString("plan")) } catch (e: Exception) { FastingPlan.PLAN_16_8 },
                        startTimeMillis = obj.getLong("startTime"),
                        endTimeMillis = obj.getLong("endTime"),
                        isCompleted = obj.getBoolean("completed")
                    )
                )
            }
        } catch (_: Exception) { }
        return list.sortedByDescending { it.startTimeMillis }
    }

    private fun parseWeightHistory(json: String): List<WeightEntry> {
        val list = mutableListOf<WeightEntry>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(WeightEntry(
                    dateMillis = obj.getLong("date"),
                    weightKg = obj.getDouble("weight").toFloat()
                ))
            }
        } catch (_: Exception) { }
        return list.sortedByDescending { it.dateMillis }
    }
}

/** Entrée de pesée pour l'historique du poids */
data class WeightEntry(
    val dateMillis: Long,
    val weightKg: Float
)
