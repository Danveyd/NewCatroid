package org.catrobat.catroid.utils

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import androidx.core.graphics.drawable.IconCompat
import androidx.preference.PreferenceManager
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.stage.StageActivity
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

object ChEngineNotificationManager {

    val savedReplies = ConcurrentHashMap<String, String>()

    val pendingActions = ConcurrentLinkedQueue<String>()

    class NotificationActionState(
        val actionId: String,
        val text: String,
        val iconPath: String,
        val behavior: Int,
        val hasInput: Boolean,
        val inputHint: String,
        val autoClose: Boolean
    )

    class NotificationState(val id: String) {
        var channelName: String = "App Notifications"
        var title: String = ""
        var text: String = ""
        var largeIconPath: String = ""
        var importance: Int = 1
        var isOngoing: Boolean = false
        val actions = mutableListOf<NotificationActionState>()
    }

    @JvmStatic
    fun cleanStringId(id: String): String {
        val doubleValue = id.toDoubleOrNull()
        if (doubleValue != null && doubleValue == doubleValue.toLong().toDouble()) {
            return doubleValue.toLong().toString()
        }
        return id
    }

    val activeDrafts = ConcurrentHashMap<String, NotificationState>()

    fun configureDraft(
        id: String, channelName: String, title: String, text: String,
        largeIconPath: String, importance: Int, isOngoing: Boolean
    ) {
        val state = activeDrafts.getOrPut(id) { NotificationState(id) }
        state.actions.clear()
        state.channelName = channelName
        state.title = title
        state.text = text
        state.largeIconPath = largeIconPath
        state.importance = importance
        state.isOngoing = isOngoing
    }

    fun addActionButton(
        id: String, actionId: String, text: String, iconPath: String,
        behavior: Int, hasInput: Boolean, inputHint: String, autoClose: Boolean
    ) {
        val state = activeDrafts[id] ?: return
        state.actions.removeAll { it.actionId == actionId }
        state.actions.add(NotificationActionState(actionId, text, iconPath, behavior, hasInput, inputHint, autoClose))
    }

    fun cancelNotification(id: String) {
        val context = CatroidApplication.getAppContext() ?: return
        val cleanId = cleanStringId(id)

        activeDrafts.remove(cleanId)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(cleanId.hashCode())
    }

    fun showOrSchedule(id: String, delaySeconds: Double) {
        val context = CatroidApplication.getAppContext() ?: return
        val state = activeDrafts[id] ?: return

        if (delaySeconds <= 0.0) {
            buildAndShow(context, state)
        } else {
            schedule(context, state, delaySeconds)
        }
    }

    fun buildAndShow(context: Context, state: NotificationState) {
        val notifyId = state.id.hashCode()
        val chanId = "catroid_chan_${state.channelName.hashCode()}"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importanceLevel = when (state.importance) {
                0 -> NotificationManager.IMPORTANCE_LOW
                2 -> NotificationManager.IMPORTANCE_HIGH
                else -> NotificationManager.IMPORTANCE_DEFAULT
            }
            val channel = NotificationChannel(chanId, state.channelName, importanceLevel)
            notificationManager.createNotificationChannel(channel)
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val isFreeStageEnabled = prefs.getBoolean("pref_workspace_stage", false)
        val targetClass = if (isFreeStageEnabled) {
            try { Class.forName("org.catrobat.catroid.stage.StageWorkspaceActivity") }
            catch (e: Exception) { StageActivity::class.java }
        } else {
            StageActivity::class.java
        }

        val launchIntent = Intent(context, targetClass).apply {
            putExtra("NOTIFICATION_CLICKED_ID", state.id)
            val projectPath = ProjectManager.getInstance().currentProject?.filesDir?.parentFile?.absolutePath
            if (projectPath != null) {
                putExtra(StageActivity.EXTRA_PROJECT_PATH, projectPath)
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context, notifyId, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val builder = NotificationCompat.Builder(context, chanId)
            .setContentTitle(state.title)
            .setContentText(state.text)
            .setSmallIcon(context.applicationInfo.icon)
            .setAutoCancel(!state.isOngoing)
            .setOngoing(state.isOngoing)
            .setContentIntent(pendingIntent)

        builder.priority = when (state.importance) {
            0 -> NotificationCompat.PRIORITY_LOW
            2 -> NotificationCompat.PRIORITY_HIGH
            else -> NotificationCompat.PRIORITY_DEFAULT
        }

        if (state.largeIconPath.isNotEmpty()) {
            val file = File(state.largeIconPath)
            if (file.exists()) {
                try {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    if (bitmap != null) builder.setLargeIcon(bitmap)
                } catch (e: Exception) { e.printStackTrace() }
            }
        }

        state.actions.forEach { actionState ->
            val requestCode = (state.id + actionState.actionId).hashCode()
            val actionIntent: Intent
            val pendingIntent: PendingIntent

            if (actionState.behavior == 1) {
                actionIntent = Intent(context, targetClass).apply {
                    putExtra("NOTIFICATION_ACTION_ID", actionState.actionId)

                    putExtra("NOTIF_ID", notifyId)
                    putExtra("NOTIF_DRAFT_ID", state.id)
                    putExtra("AUTO_CLOSE", actionState.autoClose)

                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                pendingIntent = PendingIntent.getActivity(
                    context, requestCode, actionIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
                )
            } else {
                actionIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                    action = "org.catrobat.catroid.ACTION_NOTIF_CLICK"
                    data = Uri.parse("custom://notification/${state.id}/${actionState.actionId}")

                    putExtra("ACTION_ID", actionState.actionId)
                    putExtra("NOTIF_ID", notifyId)
                    putExtra("NOTIF_DRAFT_ID", state.id)
                    putExtra("AUTO_CLOSE", actionState.autoClose)
                }
                pendingIntent = PendingIntent.getBroadcast(
                    context, requestCode, actionIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
                )
            }

            val iconCompat = if (actionState.iconPath.isNotEmpty() && File(actionState.iconPath).exists()) {
                try {
                    val bitmap = BitmapFactory.decodeFile(actionState.iconPath)
                    if (bitmap != null) {
                        IconCompat.createWithBitmap(bitmap)
                    } else null
                } catch (e: Exception) { null }
            } else null

            val actionBuilder = NotificationCompat.Action.Builder(iconCompat, actionState.text, pendingIntent)

            if (actionState.hasInput) {
                val remoteInput = RemoteInput.Builder("KEY_REPLY")
                    .setLabel(actionState.inputHint)
                    .build()
                actionBuilder.addRemoteInput(remoteInput)
            }

            builder.addAction(actionBuilder.build())
        }

        notificationManager.notify(notifyId, builder.build())
    }

    private fun schedule(context: Context, state: NotificationState, delaySeconds: Double) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationPublisherReceiver::class.java).apply {
            putExtra("id", state.id)
            putExtra("channelName", state.channelName)
            putExtra("title", state.title)
            putExtra("text", state.text)
            putExtra("largeIconPath", state.largeIconPath)
            putExtra("importance", state.importance)
            putExtra("isOngoing", state.isOngoing)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, state.id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val triggerTime = System.currentTimeMillis() + (delaySeconds * 1000).toLong()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }
}
