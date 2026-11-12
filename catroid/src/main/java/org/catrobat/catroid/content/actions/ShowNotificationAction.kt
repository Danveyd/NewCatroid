package org.catrobat.catroid.content.actions

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.stage.StageWorkspaceActivity
import org.catrobat.catroid.utils.ChEngineNotificationManager

class ShowNotificationAction : TemporalAction() {
    var scope: Scope? = null
    var notificationId: Formula? = null
    var channelName: Formula? = null
    var title: Formula? = null
    var text: Formula? = null
    var largeIconFile: Formula? = null

    var importanceSelection: Int = 1
    var isOngoing: Boolean = false

    override fun update(percent: Float) {
        val context = CatroidApplication.getAppContext() ?: return

        val idStr = ChEngineNotificationManager.cleanStringId(notificationId?.interpretString(scope) ?: "")
        val notifyId = idStr.hashCode()

        val chanName = channelName?.interpretString(scope) ?: "App Notifications"
        val chanId = "chengine_chan_" + chanName.hashCode()

        val titleStr = title?.interpretString(scope) ?: ""
        val textStr = text?.interpretString(scope) ?: ""
        val iconFileName = largeIconFile?.interpretString(scope) ?: ""

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = when (importanceSelection) {
                0 -> NotificationManager.IMPORTANCE_LOW
                2 -> NotificationManager.IMPORTANCE_HIGH
                else -> NotificationManager.IMPORTANCE_DEFAULT
            }
            val channel = NotificationChannel(chanId, chanName, importance).apply {
                description = "Notifications triggered from Ch-engine"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        val isFreeStageEnabled = prefs.getBoolean("pref_workspace_stage", false)

        val targetClass = if (isFreeStageEnabled) {
            try {
                StageWorkspaceActivity::class.java
            } catch (e: Exception) {
                StageActivity::class.java
            }
        } else {
           StageActivity::class.java
        }

        val intent = Intent(context, targetClass).apply {
            val projectPath = scope?.project?.directory?.absolutePath
            putExtra(StageActivity.EXTRA_PROJECT_PATH, projectPath)
            putExtra("NOTIFICATION_CLICKED_ID", idStr)

            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notifyId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val builder = NotificationCompat.Builder(context, chanId)
            .setContentTitle(titleStr)
            .setContentText(textStr)
            .setAutoCancel(!isOngoing)
            .setOngoing(isOngoing)
            .setSmallIcon(context.applicationInfo.icon)

        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent)
        }

        builder.priority = when (importanceSelection) {
            0 -> NotificationCompat.PRIORITY_LOW
            2 -> NotificationCompat.PRIORITY_HIGH
            else -> NotificationCompat.PRIORITY_DEFAULT
        }

        if (iconFileName.isNotEmpty()) {
            val projectFile = scope?.project?.getFile(iconFileName)
            if (projectFile != null && projectFile.exists()) {
                try {
                    val bitmap = BitmapFactory.decodeFile(projectFile.absolutePath)
                    if (bitmap != null) {
                        builder.setLargeIcon(bitmap)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        try {
            notificationManager.notify(notifyId, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun restart() {
        super.restart()
    }

    override fun reset() {
        super.reset()
        scope = null
        notificationId = null
        channelName = null
        title = null
        text = null
        largeIconFile = null
        importanceSelection = 1
        isOngoing = false
    }
}
