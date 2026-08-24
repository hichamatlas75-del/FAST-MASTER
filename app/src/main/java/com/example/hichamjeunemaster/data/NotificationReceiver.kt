package com.example.hichamjeunemaster.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import com.example.hichamjeunemaster.R

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val prefsManager = PreferencesManager(context.applicationContext)

        if (action == "com.fastmaster.app.ACTION_AUTO_START") {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val plan = prefsManager.selectedPlan.first()
                    val isActive = prefsManager.isFastingActive.first()
                    
                    // Bug Fix: On ne saute que le SAMEDI pour le plan Flexible.
                    // Le Dimanche soir, le jeûne doit reprendre pour le Lundi matin.
                    val now = Calendar.getInstance()
                    val dayOfWeek = now.get(Calendar.DAY_OF_WEEK)
                    val isFlexibleSkip = plan == FastingPlan.PLAN_FLEXIBLE_16_8 && dayOfWeek == Calendar.SATURDAY

                    if (!isFlexibleSkip) {
                        if (isActive) {
                            // Bug Fix: Si une session est déjà active (ex: celle d'hier), 
                            // on la clôture proprement avant de lancer la nouvelle.
                            prefsManager.stopFasting(true)
                        }
                        prefsManager.startFasting(plan)
                    }
                    
                    // Programmer la prochaine alarme
                    prefsManager.scheduleAutoStartAlarm()
                } finally {
                    pendingResult.finish()
                }
            }
        } else if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            // Recalculer l'alarme au redémarrage ou mise à jour
            prefsManager.scheduleAutoStartAlarm()
        } else {
            // Notification standard (fin de jeûne) ou Rappel
            val title = intent.getStringExtra("title") ?: context.getString(R.string.default_notification_title)
            val message = intent.getStringExtra("message") ?: ""
            val isReminder = intent.getBooleanExtra("is_reminder", false)
            val reminderType = intent.getStringExtra("reminder_type")
            
            // Bug Fix: Ne pas envoyer de rappel "fin de repas" le Samedi pour le plan Flexible
            if (isReminder && reminderType == "eating_end") {
                val now = Calendar.getInstance()
                val dayOfWeek = now.get(Calendar.DAY_OF_WEEK)
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    val plan = prefsManager.selectedPlan.first()
                    if (plan == FastingPlan.PLAN_FLEXIBLE_16_8 && dayOfWeek == Calendar.SATURDAY) {
                        pendingResult.finish()
                        return@launch 
                    }
                    
                    val helper = NotificationHelper(context)
                    helper.showNotification(title, message, NotificationHelper.NOTIFICATION_ID)

                    // Reprogrammer pour demain
                    prefsManager.scheduleAutoStartAlarm()
                    pendingResult.finish()
                }
            } else {
                val helper = NotificationHelper(context)
                helper.showNotification(title, message, NotificationHelper.NOTIFICATION_ID)
            }
        }
    }
}
