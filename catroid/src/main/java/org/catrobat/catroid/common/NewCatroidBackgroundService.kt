package org.catrobat.catroid.common

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import org.catrobat.catroid.R
import org.catrobat.catroid.stage.StageActivity
import kotlin.concurrent.thread

class NewCatroidBackgroundService : Service() {

    private var isTicking = false
    private var tickThread: Thread? = null

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val title = intent?.getStringExtra("TITLE") ?: getString(R.string.bgs_default_title)
        val text = intent?.getStringExtra("TEXT") ?: getString(R.string.bgs_default_text)
        val channelId = "catroid_bg_service_channel"

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, getString(R.string.bgs_channel_name), NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = if (launchIntent != null) {
            val piFlags = PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            PendingIntent.getActivity(this, 0, launchIntent, piFlags)
        } else null

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(applicationContext.applicationInfo.icon)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(1998, notification)

        startLogicTicker()

        return START_STICKY
    }

    private fun startLogicTicker() {
        if (isTicking) return
        isTicking = true

        tickThread = thread(start = true) {
            var lastTime = System.nanoTime()

            while (isTicking) {
                val currentTime = System.nanoTime()
                val delta = (currentTime - lastTime) / 1_000_000_000f
                lastTime = currentTime

                try {
                    if (StageActivity.isAppPaused) {
                        val stageListener = StageActivity.getActiveStageListener()

                        if (stageListener != null && stageListener.isBackgroundModeEnabled && !stageListener.isFinished) {
                            val safeDelta = if (delta > 0.1f) 0.016f else delta
                            stageListener.backgroundTick(safeDelta)
                        }
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                }

                try {
                    Thread.sleep(16)
                } catch (e: InterruptedException) {
                    break
                }
            }
        }
    }

    override fun onDestroy() {
        isTicking = false
        tickThread?.interrupt()
        tickThread = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
