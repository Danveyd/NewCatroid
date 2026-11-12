package org.catrobat.catroid.utils

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val actionId = intent.getStringExtra("ACTION_ID") ?: return
        val notifId = intent.getIntExtra("NOTIF_ID", -1)
        val draftId = intent.getStringExtra("NOTIF_DRAFT_ID") ?: ""
        val autoClose = intent.getBooleanExtra("AUTO_CLOSE", false)

        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        val replyText = remoteInput?.getCharSequence("KEY_REPLY")?.toString() ?: ""

        ChEngineNotificationManager.savedReplies[actionId] = replyText

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (autoClose) {
            if (notifId != -1) {
                notificationManager.cancel(notifId)
            }
        } else {
            val state = ChEngineNotificationManager.activeDrafts[draftId]
            if (state != null) {
                ChEngineNotificationManager.buildAndShow(context, state)
            } else {
                if (notifId != -1) notificationManager.cancel(notifId)
            }
        }

        ChEngineNotificationManager.pendingActions.add(actionId)
    }
}
