package org.catrobat.catroid.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationPublisherReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("id") ?: return

        val state = ChEngineNotificationManager.NotificationState(id)
        state.channelName = intent.getStringExtra("channelName") ?: "App Notifications"
        state.title = intent.getStringExtra("title") ?: ""
        state.text = intent.getStringExtra("text") ?: ""
        state.largeIconPath = intent.getStringExtra("largeIconPath") ?: ""
        state.importance = intent.getIntExtra("importance", 1)
        state.isOngoing = intent.getBooleanExtra("isOngoing", false)

        ChEngineNotificationManager.buildAndShow(context, state)
    }
}
