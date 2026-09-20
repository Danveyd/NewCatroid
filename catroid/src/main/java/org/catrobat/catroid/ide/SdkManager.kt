package org.catrobat.catroid.ide

import android.content.Context
import org.catrobat.catroid.R
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.FileOutputStream
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

object SdkManager {
    private const val REPO_XML_URL = "https://dl.google.com/android/repository/repository2-1.xml"
    private const val BASE_URL = "https://dl.google.com/android/repository"

    fun downloadPlatform(context: Context, apiLevel: Int, onProgress: (String, DownloadStatus) -> Unit): Boolean {
        var tempFile: File? = null
        try {
            onProgress(context.getString(R.string.sdk_searching_api, apiLevel), DownloadStatus.Connecting)

            val platformUrl = getPlatformUrlFromRepositoryXml(apiLevel)
            if (platformUrl == null) {
                onProgress(context.getString(R.string.sdk_api_not_found, apiLevel), DownloadStatus.Error("404 Not Found"))
                return false
            }

            tempFile = File(context.cacheDir, "temp_sdk.zip")

            if (tempFile.exists()) tempFile.delete()


            if (!downloadFile(context, platformUrl, tempFile, onProgress)) {
                return false
            }


            onProgress(context.getString(R.string.sdk_unpacking), DownloadStatus.Downloading(1f))

            val sdkDir = IdeSettings.getSdkDir(context)
            val targetDir: File = File(sdkDir, "platforms/android-$apiLevel")


            if (targetDir.exists()) targetDir.deleteRecursively()

            unzipPlatform(tempFile, targetDir)


            if (File(targetDir, "android.jar").exists()) {
                onProgress(context.getString(R.string.sdk_install_done), DownloadStatus.Success)
                return true
            } else {
                targetDir.deleteRecursively()
                onProgress(context.getString(R.string.sdk_no_android_jar), DownloadStatus.Error("Invalid SDK structure"))
                return false
            }

        } catch (e: Exception) {
            e.printStackTrace()
            onProgress(context.getString(R.string.common_error_with_message, e.message), DownloadStatus.Error(e.message ?: "Unknown Error"))
            return false
        } finally {


            try {
                if (tempFile?.exists() == true) {
                    tempFile.delete()
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun getPlatformUrlFromRepositoryXml(apiLevel: Int): URL? {

        try {
            val xmlContent = URL(REPO_XML_URL).readText()
            val parser = XmlPullParserFactory.newInstance().newPullParser()
            parser.setInput(StringReader(xmlContent))

            var eventType = parser.eventType
            var inTargetPlatform = false
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {

                        if (parser.name == "remotePackage" &&
                            parser.getAttributeValue(null, "path") == "platforms;android-$apiLevel") {
                            inTargetPlatform = true
                        }


                        if (inTargetPlatform && parser.name == "url") {
                            val urlPath = parser.nextText()
                            return URL("$BASE_URL/$urlPath")
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "remotePackage") {
                            inTargetPlatform = false
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun downloadFile(context: Context, url: URL, dest: File, onProgress: (String, DownloadStatus) -> Unit): Boolean {
        try {
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.connect()

            if (conn.responseCode != 200) {
                onProgress(context.getString(R.string.sdk_server_error, conn.responseCode), DownloadStatus.Error("HTTP ${conn.responseCode}"))
                return false
            }

            val totalBytes = conn.contentLengthLong
            val totalMb = totalBytes / 1024f / 1024f

            var downloadedBytes = 0L

            conn.inputStream.use { input ->
                FileOutputStream(dest).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var read: Int


                    var lastUpdate = 0L

                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloadedBytes += read

                        val currentTime = System.currentTimeMillis()

                        if (currentTime - lastUpdate > 100 || downloadedBytes == totalBytes) {
                            lastUpdate = currentTime

                            if (totalBytes > 0) {
                                val currentMb = downloadedBytes / 1024f / 1024f
                                val progress = downloadedBytes.toFloat() / totalBytes.toFloat()
                                onProgress(context.getString(R.string.sdk_downloading_mb, currentMb, totalMb), DownloadStatus.Downloading(progress))
                            } else {
                                val currentMb = downloadedBytes / 1024f / 1024f
                                onProgress(context.getString(R.string.sdk_downloaded_mb, currentMb), DownloadStatus.Connecting)
                            }
                        }
                    }
                }
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            onProgress(context.getString(R.string.sdk_download_failed, e.message), DownloadStatus.Error(e.message ?: "Network Error"))
            return false
        }
    }

    private fun unzipPlatform(zipFile: File, destDir: File) {

        if (!destDir.exists()) destDir.mkdirs()

        ZipInputStream(zipFile.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val name = entry.name
                val slashIndex = name.indexOf('/')
                if (slashIndex != -1) {
                    val cleanName = name.substring(slashIndex + 1)
                    if (cleanName.isNotEmpty() && !entry.isDirectory) {
                        val targetFile = File(destDir, cleanName)
                        targetFile.parentFile?.mkdirs()
                        FileOutputStream(targetFile).use { fos ->
                            val buffer = ByteArray(4096)
                            var len: Int
                            while (zis.read(buffer).also { len = it } > 0) {
                                fos.write(buffer, 0, len)
                            }
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
}
