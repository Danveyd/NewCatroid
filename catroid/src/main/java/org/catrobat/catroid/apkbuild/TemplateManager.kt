package org.catrobat.catroid.apkbuild

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

object TemplateManager {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    data class TemplateRelease(
        val tagName: String,
        val fileName: String,
        val downloadUrl: String
    )

    suspend fun fetchAvailableTemplates(): List<TemplateRelease> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://raw.githubusercontent.com/Danveyd/ChEngine-Templates/main/releases.json")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("Accept", "application/json")
            .build()

        val list = mutableListOf<TemplateRelease>()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return@use
                    val releasesJson = JSONArray(body)
                    for (i in 0 until releasesJson.length()) {
                        val release = releasesJson.getJSONObject(i)
                        list.add(
                            TemplateRelease(
                                tagName = release.getString("tag_name"),
                                fileName = release.getString("file_name"),
                                downloadUrl = release.getString("download_url")
                            )
                        )
                    }
                } else {
                    throw IOException("HTTP error code: ${response.code}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("TemplateManager", "Failed to fetch templates safely: ${e.message}", e)
        }
        return@withContext list
    }

    suspend fun downloadTemplate(
        context: Context,
        downloadUrl: String,
        fileName: String,
        onProgress: (Int) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        val destFile = File(context.cacheDir, "templates/$fileName")
        if (destFile.exists() && destFile.length() > 0) {
            return@withContext destFile
        }
        destFile.parentFile?.mkdirs()

        val request = Request.Builder()
            .url(downloadUrl)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("Accept", "*/*")
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body ?: return@withContext null
                val totalBytes = body.contentLength()

                body.byteStream().use { input ->
                    FileOutputStream(destFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalRead = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            if (totalBytes > 0) {
                                val progress = ((totalRead * 100) / totalBytes).toInt()
                                onProgress(progress)
                            }
                        }
                    }
                }
                return@withContext destFile
            }
        } catch (e: Exception) {
            android.util.Log.e("TemplateManager", "Failed to download template", e)
            destFile.delete()
            return@withContext null
        }
    }
}
