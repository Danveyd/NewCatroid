package org.catrobat.catroid.ide

import android.content.Context
import org.catrobat.catroid.R
import com.android.apksig.ApkSigner
import com.reandroid.apk.ApkModule
import com.reandroid.archive.FileInputSource
import com.reandroid.arsc.chunk.xml.ResXmlElement
import com.reandroid.arsc.container.SpecTypePair
import com.reandroid.arsc.model.ResourceEntry
import com.reandroid.arsc.value.Entry
import com.reandroid.arsc.value.ResValue
import java.io.File
import java.io.FileOutputStream
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.zip.ZipFile

object ApkBuilder {

    data class BuildConfig(
        val appName: String,
        val packageName: String,
        val versionName: String,
        val versionCode: Int,
        val permissions: List<String>,
        val orientation: String,
        val iconFile: File?,
        val signing: SigningConfig?,
        val minSdk: Int,
        val targetSdk: Int
    )

    data class BuildResult(
        val apkFile: File?,
        val error: Throwable? = null
    )

    fun build(
        context: Context,
        projectPath: String,
        dexFiles: List<File>,
        config: BuildConfig,
        extraAssets: List<File> = emptyList(),
        onProgress: (String) -> Unit
    ): BuildResult {
        val projectDir = File(projectPath)
        val buildDir = File(projectDir, "build")
        if (!buildDir.exists()) buildDir.mkdirs()

        val tempApk = File(context.cacheDir, "temp_template.apk")
        val unsignedApk = File(buildDir, "app-unsigned.apk")
        val finalApk = File(buildDir, "app-release.apk")

        var apkModule: ApkModule? = null

        try {

            onProgress(context.getString(R.string.apk_preparing_template))
            context.assets.open("template.apk").use { input ->
                FileOutputStream(tempApk).use { output -> input.copyTo(output) }
            }

            apkModule = ApkModule.loadApkFile(tempApk)


            onProgress(context.getString(R.string.apk_injecting_code))


            val existingDexFiles = apkModule!!.listDexFiles()
            var maxDexNumber = 1

            for (dex in existingDexFiles) {


                val num = dex.dexNumber
                if (num > maxDexNumber) maxDexNumber = num
            }





            var nextIndex = maxDexNumber + 1

            for (dexFile in dexFiles) {
                val dexName = if (nextIndex == 1) "classes.dex" else "classes$nextIndex.dex"
                apkModule!!.add(FileInputSource(dexFile, dexName))
                nextIndex++
            }



            val entriesToRemove = ArrayList<String>()
            for (entry in apkModule!!.zipEntryMap.listInputSources()) {
                if (entry.name.startsWith("META-INF/")) {
                    entriesToRemove.add(entry.name)
                }
            }
            for (name in entriesToRemove) {
                apkModule!!.zipEntryMap.remove(name)
            }



            onProgress(context.getString(R.string.apk_configuring_manifest))
            val manifest = apkModule!!.androidManifestBlock
            manifest.packageName = config.packageName
            manifest.versionName = config.versionName
            manifest.versionCode = config.versionCode

            val mainActivity = manifest.mainActivity
            if (mainActivity != null) {
                val orientationAttr = mainActivity.getOrCreateAndroidAttribute("screenOrientation", 0x0101001e)


                val orientationValue = when (config.orientation) {
                    "landscape" -> 0
                    "portrait" -> 1
                    "user" -> 2
                    "behind" -> 3
                    "sensor" -> 4
                    "nosensor" -> 5
                    "sensorLandscape" -> 6
                    "sensorPortrait" -> 7
                    else -> -1
                }

                if (orientationValue != -1) {

                    orientationAttr.setValueAsDecimal(orientationValue)
                } else {

                    mainActivity.removeAttribute(orientationAttr)
                }

                val configChangesAttr = mainActivity.getOrCreateAndroidAttribute("configChanges", 0x0101001f)

                configChangesAttr.setValueAsDecimal(1184)
            }

            val appElem = manifest.applicationElement
            // 0x01010001 = android:label
            val labelAttr = appElem.getOrCreateAndroidAttribute("label", 0x01010001)
            labelAttr.valueAsString = config.appName

            val netConfigId = 0x0101052b
            val existingNetConfig = appElem.searchAttributeByResourceId(netConfigId)
            if (existingNetConfig != null) {
                appElem.removeAttribute(existingNetConfig)
            }

            var extractAttr = appElem.searchAttributeByName("extractNativeLibs")
            if (extractAttr == null) {


                extractAttr = appElem.getOrCreateAndroidAttribute("extractNativeLibs", 0x01010281)
            }
            extractAttr.setValueAsBoolean(true)

            val manifestRoot: ResXmlElement = manifest.manifestElement
                ?: throw RuntimeException("Manifest root element not found")

            val declaredPermissions = manifestRoot.listElements("permission")

            for (permElement in declaredPermissions) {
                // 0x01010003 - android:name
                val nameAttr = permElement.searchAttributeByResourceId(0x01010003)
                val oldName = nameAttr?.valueAsString ?: ""

                if (oldName.contains("DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")) {
                    val suffix = "DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
                    val newName = "${config.packageName}.$suffix"
                    nameAttr?.valueAsString = newName
                }
            }

            val debugAttr = appElem.searchAttributeByResourceId(0x0101000f)
            if (debugAttr != null) {
                appElem.removeAttribute(debugAttr)
            }

            val testOnlyAttr = appElem.searchAttributeByResourceId(0x01010272)
            if (testOnlyAttr != null) {
                appElem.removeAttribute(testOnlyAttr)
            }

            val usesPermissions = manifestRoot.listElements("uses-permission")

            for (permElement in usesPermissions) {
                val nameAttr = permElement.searchAttributeByResourceId(0x01010003)
                val oldName = nameAttr?.valueAsString ?: ""

                if (oldName.contains("DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")) {
                    val suffix = "DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
                    val newName = "${config.packageName}.$suffix"
                    nameAttr?.valueAsString = newName
                }
            }

            val androidNameId = 0x01010003

            onProgress(context.getString(R.string.apk_configuring_sdk))

            var usesSdk = manifest.manifestElement.getElement("uses-sdk")
            if (usesSdk == null) {
                usesSdk = manifest.manifestElement.createChildElement("uses-sdk")
            }

            val minSdkAttr = usesSdk.getOrCreateAndroidAttribute("minSdkVersion", 0x0101020c)
            minSdkAttr.setValueAsDecimal(config.minSdk)

            val targetSdkAttr = usesSdk.getOrCreateAndroidAttribute("targetSdkVersion", 0x01010270)
            targetSdkAttr.setValueAsDecimal(config.targetSdk)

            manifest.refresh()

            onProgress(context.getString(R.string.apk_optimizing_manifest))

            val elementsToRemove = ArrayList<ResXmlElement>()


            val iterator = appElem.elements
            while (iterator.hasNext()) {
                val element = iterator.next()


                if (element.name == "provider") {

                    val nameAttr = element.searchAttributeByResourceId(0x01010003)
                    val nameValue = nameAttr?.valueAsString


                    if (nameValue != null && (nameValue.contains("androidx.startup") || nameValue.contains("androidx.profileinstaller"))) {
                        elementsToRemove.add(element)
                    }
                }


                if (element.name == "receiver") {
                    val nameAttr = element.searchAttributeByResourceId(0x01010003)
                    if (nameAttr?.valueAsString?.contains("androidx.profileinstaller") == true) {
                        elementsToRemove.add(element)
                    }
                }
            }


            for (el in elementsToRemove) {


                try {
                    el.removeSelf()
                } catch (e: Exception) {


                }
            }

            if (config.iconFile != null && config.iconFile.exists()) {
                onProgress(context.getString(R.string.apk_updating_graphics))
                try {
                    val tableBlock = apkModule!!.tableBlock
                    val iconPathsToReplace = mutableListOf<String>()
                    val xmlPathsToRemove = mutableListOf<String>()


                    if (config.iconFile != null && config.iconFile.exists()) {
                        onProgress(context.getString(R.string.apk_updating_graphics))
                        try {
                            val tableBlock = apkModule!!.tableBlock
                            val iconPathsToReplace = mutableListOf<String>()
                            val xmlPathsToRemove = mutableListOf<String>()


                            tableBlock.listPackages().forEach { pkg ->


                                pkg.listSpecTypePairs().forEach { specPair: SpecTypePair ->


                                    if (specPair.typeName == "mipmap") {


                                        val resIterator = specPair.resources
                                        while (resIterator.hasNext()) {
                                            val resEntry: ResourceEntry = resIterator.next()
                                            val resName = resEntry.name ?: ""

                                            if (resName.contains("ic_launcher")) {



                                                val entryIterator = resEntry.iterator()
                                                while (entryIterator.hasNext()) {
                                                    val entry: Entry = entryIterator.next()
                                                    val resValue: ResValue? = entry.resValue
                                                    val path = resValue?.valueAsString

                                                    if (path != null && path.contains("res/")) {
                                                        if (path.endsWith(".xml")) {

                                                            resValue.type = 0x00.toByte()
                                                            resValue.data = 0
                                                            xmlPathsToRemove.add(path)
                                                        } else if (path.endsWith(".png") || path.endsWith(".webp")) {
                                                            iconPathsToReplace.add(path)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }


                            xmlPathsToRemove.distinct().forEach { path ->
                                apkModule!!.zipEntryMap.remove(path)
                            }

                            iconPathsToReplace.distinct().forEach { path ->
                                apkModule!!.zipEntryMap.remove(path)
                                apkModule!!.add(FileInputSource(config.iconFile, path))
                            }

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }


                    xmlPathsToRemove.forEach { path ->
                        apkModule!!.zipEntryMap.remove(path)
                    }


                    iconPathsToReplace.forEach { path ->
                        apkModule!!.zipEntryMap.remove(path)
                        apkModule!!.add(FileInputSource(config.iconFile, path))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // --- ASSETS ---
            onProgress(context.getString(R.string.apk_packing_resources))

            if (extraAssets.isNotEmpty()) {
                for (file in extraAssets) {
                    apkModule!!.add(FileInputSource(file, "assets/${file.name}"))
                }
            }

            val assetsDir = File(projectDir, "assets")
            if (assetsDir.exists()) {
                addAssets(apkModule!!, assetsDir, "assets")
            }

            // --- NATIVE LIBS (.so) ---
            onProgress(context.getString(R.string.apk_packing_native_libs))
            val libsDir = File(projectPath, "libs")

            if (libsDir.exists()) {
                val tempLibDir = File(context.cacheDir, "temp_libs_apk")
                tempLibDir.deleteRecursively()
                tempLibDir.mkdirs()

                libsDir.listFiles()?.filter { it.extension == "jar" }?.forEach { jarFile ->
                    try {

                        var archFromJarName: String? = null
                        if (jarFile.name.contains("arm64-v8a")) archFromJarName = "arm64-v8a"
                        else if (jarFile.name.contains("armeabi-v7a")) archFromJarName = "armeabi-v7a"
                        else if (jarFile.name.contains("x86_64")) archFromJarName = "x86_64"
                        else if (jarFile.name.contains("x86")) archFromJarName = "x86"

                        ZipFile(jarFile).use { zip ->
                            val entries = zip.entries()
                            while (entries.hasMoreElements()) {
                                val entry = entries.nextElement()
                                val name = entry.name


                                if (name.endsWith(".so")) {


                                    var arch = archFromJarName
                                    if (name.contains("lib/arm64-v8a")) arch = "arm64-v8a"
                                    else if (name.contains("lib/armeabi-v7a")) arch = "armeabi-v7a"


                                    if (arch != null) {

                                        val fileName = File(name).name
                                        val apkPath = "lib/$arch/$fileName"


                                        val tempSo = File(tempLibDir, "${arch}_$fileName")
                                        zip.getInputStream(entry).use { input ->
                                            FileOutputStream(tempSo).use { output -> input.copyTo(output) }
                                        }



                                        apkModule!!.zipEntryMap.remove(apkPath)
                                        apkModule!!.add(FileInputSource(tempSo, apkPath))
                                    }
                                }
                            }
                        }
                    } catch (_: Exception) {

                    }
                }
            }

            val jniLibsDir = File(projectDir, "jniLibs")
            if (jniLibsDir.exists()) {
                jniLibsDir.walkTopDown().forEach { file ->
                    if (file.isFile && file.extension == "so") {
                        val relativePath = file.toRelativeString(jniLibsDir)
                        val apkPath = "lib/$relativePath"

                        apkModule!!.zipEntryMap.remove(apkPath)
                        apkModule!!.add(FileInputSource(file, apkPath))
                    }
                }
            }

            // --- BUILD ---
            onProgress(context.getString(R.string.apk_building))
            apkModule!!.writeApk(unsignedApk)
            apkModule!!.close()

            // --- SIGN ---
            onProgress(context.getString(R.string.apk_signing))

            try {
                val signing = config.signing


                val (privateKey, cert) = if (signing != null) {

                    val ks = KeyStore.getInstance("PKCS12")
                    File(projectPath, signing.keystorePath).inputStream().use {
                        ks.load(it, signing.keystorePass.toCharArray())
                    }
                    val key = ks.getKey(signing.keyAlias, signing.keyPass.toCharArray()) as PrivateKey
                    val cert = ks.getCertificate(signing.keyAlias) as X509Certificate
                    Pair(key, cert)
                } else {



                    throw RuntimeException(context.getString(R.string.apk_keystore_required))
                }


                val signer = ApkSigner.Builder(listOf(
                    ApkSigner.SignerConfig.Builder(
                        "CERT",
                        privateKey,
                        listOf(cert)
                    ).build()
                ))
                    .setInputApk(unsignedApk)
                    .setOutputApk(finalApk)
                    .setV1SigningEnabled(true)
                    .setV2SigningEnabled(true)
                    .build()

                signer.sign()

                onProgress(context.getString(R.string.apk_done_v2))

            } catch (e: Exception) {
                e.printStackTrace()
                onProgress(context.getString(R.string.apk_signing_error, e.message))
                return BuildResult(null, e)
            } finally {
                unsignedApk.delete()
            }

            return BuildResult(finalApk)
        } catch (e: Exception) {
            e.printStackTrace()
            onProgress(context.getString(R.string.common_error_with_message, e.message))
            try { apkModule?.close() } catch(ex: Exception){}
            return BuildResult(null, e)
        }
    }

    private fun addAssets(module: ApkModule, folder: File, parentPath: String) {
        folder.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                addAssets(module, file, "$parentPath/${file.name}")
            } else {
                module.add(FileInputSource(file, "$parentPath/${file.name}"))
            }
        }
    }
}
