package org.catrobat.catroid.ide

import android.app.*
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import org.catrobat.catroid.R
import android.os.Build
import android.os.IBinder
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipInputStream

class BuildMonitorService : Service() {

    private val CHANNEL_ID = "BuildMonitorChannel"
    private val PROGRESS_NOTIFY_ID = 1337
    private val DONE_NOTIFY_ID = 1338

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val token = intent?.getStringExtra("TOKEN") ?: return START_NOT_STICKY
        val login = intent.getStringExtra("LOGIN") ?: return START_NOT_STICKY
        val branch = intent.getStringExtra("BRANCH") ?: return START_NOT_STICKY
        val featureName = intent.getStringExtra("FEATURE_NAME") ?: "Feature"

        createNotificationChannel()
        startForeground(PROGRESS_NOTIFY_ID, createProgressNotification(getString(R.string.build_connecting), 0))

        serviceScope.launch {
            try {
                monitorBuild(token, login, branch, featureName)
            } catch (e: Exception) {
                e.printStackTrace()
                showFinalNotification(getString(R.string.build_failed_title), e.message ?: getString(R.string.build_unknown_error), null)
                stopForeground(true)
                stopSelf()
            }
        }

        return START_STICKY
    }

    private suspend fun monitorBuild(token: String, login: String, branch: String, featureName: String) {
        val client = OkHttpClient()
        var runId: String? = null

        delay(5000)

        val runsUrl = "https://api.github.com/repos/$login/NewCatroid/actions/runs?branch=$branch"

        android.util.Log.d("IDE_SERVICE", "Начинаем поиск активной сборки...")

        var attempts = 0
        while (runId == null && attempts < 10) {
            val req = Request.Builder().url(runsUrl).header("Authorization", "Bearer $token").build()
            client.newCall(req).execute().use { response ->
                val json = JSONObject(response.body?.string() ?: "")
                val runs = json.optJSONArray("workflow_runs")

                if (runs != null && runs.length() > 0) {
                    val latestRun = runs.getJSONObject(0)
                    val status = latestRun.getString("status")
                    val createdAt = latestRun.getString("created_at")

                    if (status != "completed") {
                        runId = latestRun.getString("id")
                        android.util.Log.d("IDE_SERVICE", "Найдена новая сборка: $runId")
                    } else {
                        android.util.Log.d("IDE_SERVICE", "Найдена только старая сборка, ждем...")
                    }
                }
            }
            if (runId == null) {
                attempts++
                delay(3000)
            }
        }

        if (runId == null) {
            showFinalNotification(getString(R.string.error), getString(R.string.build_github_timeout), null)
            stopForeground(true)
            stopSelf()
            return
        }

        var isCompleted = false
        var isSuccess = false
        while (!isCompleted) {
            val checkReq = Request.Builder().url("https://api.github.com/repos/$login/NewCatroid/actions/runs/$runId")
                .header("Authorization", "Bearer $token").build()

            client.newCall(checkReq).execute().use { response ->
                val json = JSONObject(response.body?.string() ?: "")
                val status = json.getString("status")
                val progress = json.optInt("progress", 0)

                updateProgress(getString(R.string.build_on_github), getString(R.string.build_status_prefix, status))

                if (status == "completed") {
                    isCompleted = true
                    isSuccess = json.getString("conclusion") == "success"
                }
            }
            if (!isCompleted) delay(15000)
        }

        if (!isSuccess) {
            showFinalNotification(getString(R.string.build_failed_title), getString(R.string.build_check_syntax), null)
            stopForeground(true)
            stopSelf()
            return
        }


        updateProgress(getString(R.string.build_success), getString(R.string.build_looking_for_artifact))
        val artUrl = "https://api.github.com/repos/$login/NewCatroid/actions/runs/$runId/artifacts"
        var downloadUrl: String? = null

        client.newCall(Request.Builder().url(artUrl).header("Authorization", "Bearer $token").build()).execute().use {
            val json = JSONObject(it.body?.string() ?: "")
            val arts = json.optJSONArray("artifacts")
            if (arts != null && arts.length() > 0) {
                downloadUrl = arts.getJSONObject(0).getString("archive_download_url")
            }
        }

        if (downloadUrl != null) {
            downloadWithProgress(client, downloadUrl!!, token, featureName)
        } else {
            showFinalNotification(getString(R.string.error), getString(R.string.build_artifact_not_found), null)
            stopForeground(true)
            stopSelf()
        }
    }

    private fun downloadWithProgress(client: OkHttpClient, url: String, token: String, featureName: String) {
        val request = Request.Builder().url(url).header("Authorization", "Bearer $token").build()

        client.newCall(request).execute().use { response ->
            val body = response.body ?: return
            val totalBytes = body.contentLength()
            val inputStream = body.byteStream()

            val tempZip = File(cacheDir, "temp_build.zip")
            val outputStream = tempZip.outputStream()

            val buffer = ByteArray(8 * 1024)
            var bytesRead: Long = 0
            var read: Int

            while (inputStream.read(buffer).also { read = it } != -1) {
                outputStream.write(buffer, 0, read)
                bytesRead += read
                val percent = ((bytesRead * 100) / totalBytes).toInt()
                updateProgress(getString(R.string.build_downloading_apk), "$percent%", percent)
            }
            outputStream.close()


            updateProgress(getString(R.string.build_unpacking), getString(R.string.please_wait))
            val apkUri = saveApkToDownloads(tempZip, featureName)
            tempZip.delete()

            if (apkUri != null) {
                showFinalNotification(getString(R.string.build_ready_title), getString(R.string.build_tap_to_send), apkUri)
            } else {
                showFinalNotification(getString(R.string.error), getString(R.string.build_save_to_downloads_failed), null)
            }

            stopForeground(true)
            stopSelf()
        }
    }

    private fun saveApkToDownloads(zipFile: File, featureName: String): Uri? {
        val fileName = "NewCatroid_${featureName}_${System.currentTimeMillis() / 1000}.apk"
        val resolver = contentResolver

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.android.package-archive")
                put(MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues) ?: return null
            try {
                resolver.openOutputStream(uri)?.use { out ->
                    extractApkFromZip(zipFile, out)
                }
                uri
            } catch (e: Exception) { null }
        } else {

            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val destFile = File(downloadsDir, fileName)
            try {
                destFile.outputStream().use { out ->
                    extractApkFromZip(zipFile, out)
                }

                FileProvider.getUriForFile(this, "${applicationContext.packageName}.provider", destFile)
            } catch (e: Exception) { null }
        }
    }


    private fun extractApkFromZip(zipFile: File, outputStream: java.io.OutputStream) {
        ZipInputStream(zipFile.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name.endsWith(".apk")) {
                    zis.copyTo(outputStream)
                    break
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    private fun showFinalNotification(title: String, message: String, fileUri: Uri?) {
        val intent = if (fileUri != null) {
            Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else null

        val pendingIntent = if (intent != null) {
            val chooser = Intent.createChooser(intent, getString(R.string.build_open_apk))
            PendingIntent.getActivity(this, 0, chooser, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        } else null

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        if (pendingIntent != null) builder.setContentIntent(pendingIntent)

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(DONE_NOTIFY_ID, builder.build())
    }

    private fun updateProgress(title: String, text: String, percent: Int = -1) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(PROGRESS_NOTIFY_ID, createProgressNotification(title, percent, text))
    }

    private fun createProgressNotification(title: String, percent: Int, text: String = ""): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, if (percent >= 0) percent else 0, percent < 0)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Build Status", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
