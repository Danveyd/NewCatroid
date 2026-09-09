package org.catrobat.catroid.utils.community

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.catrobat.catroid.R
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class ModerationCheckWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    override suspend fun doWork(): Result {
        if (!CommunityTokenManager.isLoggedIn(context)) {
            return Result.success()
        }

        val token = CommunityTokenManager.getToken(context) ?: return Result.success()
        val username = CommunityTokenManager.getUsername(context) ?: return Result.success()

        try {
            val url = "https://backend.mindpy.dev/profiles/$username/games"
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful && response.body != null) {
                    val responseBody = response.body!!.string()
                    parseAndCheckModeration(responseBody)
                }
            }
        } catch (e: Exception) {
            Log.e("ModerationWorker", "Ошибка фоновой проверки модерации: ", e)
            return Result.retry()
        }

        return Result.success()
    }

    private fun parseAndCheckModeration(jsonResponse: String) {
        val json = JSONObject(jsonResponse)
        val items = json.optJSONArray("items") ?: return

        val trackerPrefs = context.getSharedPreferences("community_moderation_tracker", Context.MODE_PRIVATE)
        val editor = trackerPrefs.edit()

        for (i in 0 until items.length()) {
            val game = items.getJSONObject(i)
            val gameId = game.getString("id")
            val title = game.getString("title")
            val currentStatus = game.getString("status")
            val reason = game.optString("moderation_reason", "")

            val lastKnownStatus = trackerPrefs.getString(gameId, null)

            if (lastKnownStatus != null) {
                if (lastKnownStatus == "pending" && currentStatus != "pending") {
                    triggerStatusNotification(gameId, title, currentStatus, reason)
                }
            }

            editor.putString(gameId, currentStatus)
        }
        editor.apply()
    }

    private fun triggerStatusNotification(gameId: String, title: String, status: String, reason: String) {
        val channelId = "community_moderation_channel"
        val notificationId = gameId.hashCode()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = context.getString(R.string.notification_moderation_channel)
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, org.catrobat.catroid.ui.CommunityWebViewActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(context, notificationId, intent, pendingIntentFlags)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.pc_toolbar_icon)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        if (status == "approved") {
            builder.setContentTitle(context.getString(R.string.notification_moderation_approved_title))
            builder.setContentText(context.getString(R.string.notification_moderation_approved_body, title))
        } else if (status == "rejected") {
            builder.setContentTitle(context.getString(R.string.notification_moderation_rejected_title))
            builder.setContentText(context.getString(R.string.notification_moderation_rejected_body, title, reason))
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(
                context.getString(R.string.notification_moderation_rejected_body, title, reason)
            ))
        } else {
            return
        }

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    companion object {
        @JvmStatic
        fun schedulePeriodicCheck(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicWorkRequest = PeriodicWorkRequest.Builder(
                ModerationCheckWorker::class.java,
                1, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "CommunityUploadSync",
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWorkRequest
            )
        }
    }
}
