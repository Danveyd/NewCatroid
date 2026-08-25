package org.catrobat.catroid.ide

import android.content.Context
import org.catrobat.catroid.R
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.FileOutputStream
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipFile

sealed class DownloadStatus {
    object Connecting : DownloadStatus()
    data class Downloading(val progress: Float) : DownloadStatus()
    data class Error(val message: String) : DownloadStatus()
    object Success : DownloadStatus()
}

data class LogEntry(val text: String, val isError: Boolean = false)


object DependencyManager {


    private val defaultRepos = listOf(
        "https://repo1.maven.org/maven2",
        "https://maven.google.com"
    )

    data class LibInfo(val group: String, val artifact: String, val version: String, val classifier: String? = null) {
        override fun toString(): String {
            return if (classifier != null) "$group:$artifact:$version:$classifier"
            else "$group:$artifact:$version"
        }
    }

    fun parseLib(str: String): LibInfo? {
        val parts = str.split(":")
        return when (parts.size) {
            3 -> LibInfo(parts[0], parts[1], parts[2], null)
            4 -> LibInfo(parts[0], parts[1], parts[2], parts[3])
            else -> null
        }
    }


    fun downloadLibraryRecursive(
        context: Context,
        projectPath: String,
        libString: String,
        customRepos: List<String> = emptyList(),
        recursive: Boolean = true,

        onProgressUpdate: (String, DownloadStatus) -> Unit
    ): Boolean {
        val rootLib = parseLib(libString) ?: run {
            onProgressUpdate(libString, DownloadStatus.Error(context.getString(R.string.dm_invalid_id_format)))
            return false
        }

        val visited = HashSet<String>()
        val allRepos = (customRepos + defaultRepos).distinct()

        return downloadLibInternal(context, projectPath, rootLib, visited, allRepos, recursive, onProgressUpdate)
    }

    private fun downloadLibInternal(
        context: Context,
        projectPath: String,
        lib: LibInfo,
        visited: MutableSet<String>,
        repos: List<String>,
        recursive: Boolean,
        onProgressUpdate: (String, DownloadStatus) -> Unit
    ): Boolean {
        val key = "${lib.group}:${lib.artifact}:${lib.classifier ?: ""}"
        if (visited.contains(key)) return true
        visited.add(key)

        val success = downloadFile(context, projectPath, lib, repos, onProgressUpdate)

        if (!success) {

            return false
        }

        if (!recursive) return true

        val pomContent = downloadPom(lib, repos)

        if (pomContent != null && lib.classifier == null) {
            val dependencies = parseDependenciesFromPom(pomContent)
            for (dep in dependencies) {
                if (dep.scope != "test" && dep.scope != "provided" && !dep.optional) {
                    if (!dep.version.contains("$")) {
                        val depLib = LibInfo(dep.group, dep.artifact, dep.version)
                        downloadLibInternal(context, projectPath, depLib, visited, repos, true, onProgressUpdate)
                    }
                }
            }
        }
        return true
    }

    private fun downloadFile(
        context: Context,
        projectPath: String,
        lib: LibInfo,
        repos: List<String>,
        onProgressUpdate: (String, DownloadStatus) -> Unit
    ): Boolean {
        val libsDir = File(projectPath, "libs")
        if (!libsDir.exists()) libsDir.mkdirs()

        val fileName = if (lib.classifier != null) "${lib.artifact}-${lib.version}-${lib.classifier}.jar"
        else "${lib.artifact}-${lib.version}.jar"

        val destination = File(libsDir, fileName)


        if (destination.exists() && destination.length() > 0) {
            onProgressUpdate(fileName, DownloadStatus.Success)
            return true
        }

        for (repo in repos) {
            val cleanRepo = repo.trimEnd('/')
            val jarName = if (lib.classifier != null) "${lib.artifact}-${lib.version}-${lib.classifier}.jar"
            else "${lib.artifact}-${lib.version}.jar"

            val urlStr = "$cleanRepo/${lib.group.replace('.', '/')}/${lib.artifact}/${lib.version}/$jarName"


            val downloaded = tryDownload(urlStr, destination) { status ->
                onProgressUpdate(fileName, status)
            }

            if (downloaded) return true
        }

        if (lib.classifier == null) {
            val aarFile = File(context.cacheDir, "temp_${lib.artifact}.aar")

            for (repo in repos) {
                val cleanRepo = repo.trimEnd('/')
                val aarName = "${lib.artifact}-${lib.version}.aar"
                val urlStr = "$cleanRepo/${lib.group.replace('.', '/')}/${lib.artifact}/${lib.version}/$aarName"

                onProgressUpdate(aarName, DownloadStatus.Connecting)

                val downloaded = tryDownload(urlStr, aarFile) { status ->

                    onProgressUpdate(aarName, status)
                }

                if (downloaded) {
                    try {

                        onProgressUpdate(fileName, DownloadStatus.Connecting)

                        ZipFile(aarFile).use { zip ->
                            val entry = zip.getEntry("classes.jar")
                            if (entry != null) {
                                zip.getInputStream(entry).use { input ->
                                    FileOutputStream(destination).use { output -> input.copyTo(output) }
                                }
                                onProgressUpdate(fileName, DownloadStatus.Success)
                                aarFile.delete()
                                return true
                            }
                        }
                    } catch (e: Exception) {
                        onProgressUpdate(fileName, DownloadStatus.Error(context.getString(R.string.dm_aar_unpack_error)))
                    }
                    aarFile.delete()
                }
            }
        }

        onProgressUpdate(fileName, DownloadStatus.Error(context.getString(R.string.common_file_not_found)))
        return false
    }



    private fun tryDownload(
        urlStr: String,
        destination: File,
        onProgress: (DownloadStatus) -> Unit
    ): Boolean {
        try {
            onProgress(DownloadStatus.Connecting)

            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.connect()

            if (conn.responseCode == 200) {
                val totalSize = conn.contentLength

                conn.inputStream.use { input ->
                    FileOutputStream(destination).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalRead: Long = 0

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead


                            if (totalSize > 0) {
                                val percent = totalRead.toFloat() / totalSize.toFloat()
                                onProgress(DownloadStatus.Downloading(percent))
                            } else {

                                onProgress(DownloadStatus.Connecting)
                            }
                        }
                    }
                }
                onProgress(DownloadStatus.Success)
                return true
            }
        } catch (e: Exception) {
            onProgress(DownloadStatus.Error(e.message ?: "Unknown error"))
        }
        return false
    }

    private fun downloadPom(lib: LibInfo, repos: List<String>): String? {
        for (repo in repos) {
            val cleanRepo = repo.trimEnd('/')
            val urlStr = "$cleanRepo/${lib.group.replace('.', '/')}/${lib.artifact}/${lib.version}/${lib.artifact}-${lib.version}.pom"
            try {
                val text = URL(urlStr).readText()
                if (text.isNotEmpty()) return text
            } catch (e: Exception) { }
        }
        return null
    }


    private data class Dep(val group: String, val artifact: String, val version: String, val scope: String = "compile", val optional: Boolean = false)
    private fun parseDependenciesFromPom(pomXml: String): List<Dep> {
        val deps = ArrayList<Dep>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(pomXml))
            var eventType = parser.eventType
            var inDependencies = false
            var inDependency = false
            var group = ""; var artifact = ""; var version = ""; var scope = ""; var optional = ""
            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (tagName == "dependencies") inDependencies = true
                        if (inDependencies && tagName == "dependency") inDependency = true
                        if (inDependency) {
                            when (tagName) {
                                "groupId" -> group = parser.nextText()
                                "artifactId" -> artifact = parser.nextText()
                                "version" -> version = parser.nextText()
                                "scope" -> scope = parser.nextText()
                                "optional" -> optional = parser.nextText()
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (tagName == "dependencies") inDependencies = false
                        if (tagName == "dependency" && inDependencies) {
                            if (group.isNotEmpty() && artifact.isNotEmpty() && version.isNotEmpty()) {
                                deps.add(Dep(group, artifact, version, scope, optional == "true"))
                            }
                            inDependency = false
                            group = ""; artifact = ""; version = ""; scope = ""; optional = ""
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {}
        return deps
    }

    fun getLibraries(projectPath: String): List<File> {
        return File(projectPath, "libs").listFiles()?.filter { it.extension == "jar" }?.toList() ?: emptyList()
    }
}


object LibraryTracker {
    data class LibMeta(val mavenId: String, val fileName: String, val timestamp: Long)

    private fun getMetaFile(projectPath: String) = File(projectPath, "libs/necat_libs.json")

    fun saveLib(projectPath: String, mavenId: String, fileName: String) {
        val metaFile = getMetaFile(projectPath)
        val list = loadLibs(projectPath).toMutableList()
        list.removeIf { it.fileName == fileName || it.mavenId == mavenId }
        list.add(LibMeta(mavenId, fileName, System.currentTimeMillis()))


        val jsonArray = JSONArray()
        list.forEach { lib ->
            val obj = JSONObject()
            obj.put("id", lib.mavenId)
            obj.put("file", lib.fileName)
            obj.put("time", lib.timestamp)
            jsonArray.put(obj)
        }
        if (!metaFile.parentFile.exists()) metaFile.parentFile.mkdirs()
        metaFile.writeText(jsonArray.toString(4))
    }

    fun removeLib(projectPath: String, fileName: String) {
        val list = loadLibs(projectPath).filter { it.fileName != fileName }
        val metaFile = getMetaFile(projectPath)
        val jsonArray = JSONArray()
        list.forEach { lib ->
            val obj = JSONObject()
            obj.put("id", lib.mavenId)
            obj.put("file", lib.fileName)
            obj.put("time", lib.timestamp)
            jsonArray.put(obj)
        }
        metaFile.writeText(jsonArray.toString(4))
    }

    fun loadLibs(projectPath: String): List<LibMeta> {
        val metaFile = getMetaFile(projectPath)
        if (!metaFile.exists()) return emptyList()
        val result = ArrayList<LibMeta>()
        try {
            val arr = JSONArray(metaFile.readText())
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                result.add(LibMeta(obj.getString("id"), obj.getString("file"), obj.optLong("time")))
            }
        } catch (e: Exception) { e.printStackTrace() }
        return result
    }
}
