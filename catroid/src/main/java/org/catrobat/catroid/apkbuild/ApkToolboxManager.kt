package org.catrobat.catroid.apkbuild

import android.content.Context
import android.util.Log
import com.android.apksig.ApkSigner
import com.reandroid.apk.ApkModule
import com.reandroid.archive.FileInputSource
import com.reandroid.arsc.chunk.xml.ResXmlElement
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Security
import java.security.cert.X509Certificate
import java.util.Date
import javax.security.auth.x500.X500Principal

object ApkToolboxManager {

    private const val TAG = "ApkToolbox"

    init {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    data class ManifestConfig(
        val appName: String? = null,
        val packageName: String? = null,
        val versionCode: Int? = null,
        val versionName: String? = null,
        val minSdkVersion: Int? = null,
        val targetSdkVersion: Int? = null,
        val permissionsToAdd: List<String>? = null,
        val permissionsToRemove: List<String>? = null,
        val debuggable: Boolean? = null
    )

    data class BuildOptions(
        val appName: String,
        val packageName: String,
        val versionName: String,
        val versionCode: Int,
        val orientation: String,
        val iconFile: File?,
        val projectZipFile: File,
        val excludedArches: List<String>,
        val excludedLibraries: List<String>,
        val allowedPermissions: List<String>
    )

    fun customizeApk(apkPath: String, options: BuildOptions): Boolean {
        return modifyApk(apkPath) { module ->
            val manifest = module.androidManifestBlock
            val oldPackage = manifest.packageName
            val newPackage = options.packageName

            makeClassNamesAbsolute(manifest.manifestElement, oldPackage)
            manifest.packageName = newPackage
            fixManifestAttributes(manifest.manifestElement, oldPackage, newPackage)

            manifest.versionCode = options.versionCode
            manifest.versionName = options.versionName

            val appElem = manifest.applicationElement
            val labelAttr = appElem.getOrCreateAndroidAttribute("label", 0x01010001)
            labelAttr.valueAsString = options.appName

            val mainActivity = manifest.mainActivity
            if (mainActivity != null) {
                val orientationAttr = mainActivity.getOrCreateAndroidAttribute("screenOrientation", 0x0101001e)
                val orientationValue = when (options.orientation) {
                    "landscape" -> 0
                    "portrait" -> 1
                    "sensor" -> 4
                    else -> -1
                }
                if (orientationValue != -1) {
                    orientationAttr.setValueAsDecimal(orientationValue)
                } else {
                    mainActivity.removeAttribute(orientationAttr)
                }
            }

            val root = manifest.manifestElement
            val usesPermissions = root.listElements("uses-permission")
            val toRemovePermissions = mutableListOf<ResXmlElement>()

            for (permElem in usesPermissions) {
                val nameAttr = permElem.searchAttributeByResourceId(0x01010003) // android:name
                val permName = nameAttr?.valueAsString ?: ""

                if (permName.isNotEmpty() && !options.allowedPermissions.contains(permName)) {
                    if (permName.startsWith("android.permission.")) {
                        toRemovePermissions.add(permElem)
                    }
                }
            }

            toRemovePermissions.forEach { root.remove(it) }

            val usesFeatures = root.listElements("uses-feature")
            val toRemoveFeatures = mutableListOf<ResXmlElement>()

            for (featureElem in usesFeatures) {
                val nameAttr = featureElem.searchAttributeByResourceId(0x01010003) // android:name
                val featureName = nameAttr?.valueAsString ?: ""

                if (featureName.isNotEmpty()) {
                    val isCameraRemoved = !options.allowedPermissions.contains("android.permission.CAMERA")
                    val isLocationRemoved = !options.allowedPermissions.contains("android.permission.ACCESS_FINE_LOCATION")
                    val isNfcRemoved = !options.allowedPermissions.contains("android.permission.NFC")
                    val isBluetoothRemoved = !options.allowedPermissions.contains("android.permission.BLUETOOTH")

                    if (isCameraRemoved && featureName.contains("camera")) {
                        toRemoveFeatures.add(featureElem)
                    }
                    if (isLocationRemoved && featureName.contains("location")) {
                        toRemoveFeatures.add(featureElem)
                    }
                    if (isNfcRemoved && featureName.contains("nfc")) {
                        toRemoveFeatures.add(featureElem)
                    }
                    if (isBluetoothRemoved && featureName.contains("bluetooth")) {
                        toRemoveFeatures.add(featureElem)
                    }
                }
            }

            toRemoveFeatures.forEach { root.remove(it) }

            if (module.hasTableBlock()) {
                val tableBlock = module.tableBlock
                tableBlock.listPackages().forEach { pkg ->
                    if (pkg.name == oldPackage) {
                        pkg.name = newPackage
                    }
                }

                if (options.iconFile != null && options.iconFile.exists()) {
                    val xmlPathsToRemove = mutableListOf<String>()

                    tableBlock.listPackages().forEach { pkg ->
                        pkg.listSpecTypePairs().forEach { specPair ->
                            if (specPair.typeName == "mipmap" || specPair.typeName == "drawable") {
                                val resIterator = specPair.resources
                                while (resIterator.hasNext()) {
                                    val resEntry = resIterator.next()
                                    val resName = resEntry.name ?: ""

                                    if (resName == "ic_launcher_foreground" || resName == "ic_launcher_round_foreground") {
                                        val entryIterator = resEntry.iterator()
                                        while (entryIterator.hasNext()) {
                                            val entry = entryIterator.next()
                                            val resValue = entry.resValue
                                            val path = resValue?.valueAsString

                                            if (path != null && path.contains("res/")) {
                                                entry.setValueAsString("res/mipmap-xxxhdpi/ic_launcher_foreground.png")
                                                if (path.endsWith(".xml")) {
                                                    xmlPathsToRemove.add(path)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    xmlPathsToRemove.distinct().forEach { path ->
                        module.zipEntryMap.remove(path)
                    }
                }
            }

            if (options.iconFile != null && options.iconFile.exists()) {
                val iconPathsToReplace = mutableListOf<String>()

                module.zipEntryMap.listInputSources().forEach { entry ->
                    val name = entry.name
                    if ((name.endsWith(".png") || name.endsWith(".webp")) && name.contains("ic_launcher")) {
                        iconPathsToReplace.add(name)
                    }
                }

                iconPathsToReplace.add("res/mipmap-xxxhdpi/ic_launcher_foreground.png")

                iconPathsToReplace.distinct().forEach { path ->
                    module.zipEntryMap.remove(path)
                    module.add(FileInputSource(options.iconFile, path))
                }
            }

            module.zipEntryMap.remove("assets/project.zip")
            module.add(FileInputSource(options.projectZipFile, "assets/project.zip"))

            options.excludedArches.forEach { arch ->
                val toRemove = module.zipEntryMap.listInputSources().filter { it.name.startsWith("lib/$arch/") }
                toRemove.forEach { module.zipEntryMap.remove(it.name) }
            }

            val excludePython = options.excludedLibraries.contains("python")
            val excludeQemu = options.excludedLibraries.contains("qemu")
            val excludeCompiler = options.excludedLibraries.contains("compiler")
            val excludeOnnx = options.excludedLibraries.contains("onnx")
            val excludeMnn = options.excludedLibraries.contains("mnn")
            val excludeMkit = options.excludedLibraries.contains("mkit")

            val junkFiles = listOf(
                "assets/test.png",
                "assets/xz9.jpg",
                "assets/xz10.jpg",
                "assets/veryimportant.txt",
                "assets/rtx6090_download_without_registration_free.sh",
                "assets/stage/xz4.jpg",
                "assets/stage/xz5.jpg",
                "assets/stage/xz8.jpg",
                "assets/qemu_x86_64/xz12.jpg",
                "assets/gta_6_source_code_leak.cpp",
                "assets/stub_project.zip",
                "assets/trustedDomains.json",
                "assets/nolb_config.xml",
                "assets/hmsincas.nks",
                "assets/hmsincas.bks",
                "assets/hmsrootcas.bks",
                "assets/updatesdkcas.bks",
                "assets/grs_sp.bks",
                "assets/penguin.webp",
                "resources/new_api_database.ser"
            )

            val compilerAssets = listOf(
                "assets/activity.jar",
                "assets/activity-1.7.2.aar",
                "assets/annotation-1.2.0.jar",
                "assets/annotation-experimental-1.1.0.aar",
                "assets/annotations-13.0.jar",
                "assets/app-classes.jar",
                "assets/collection-1.1.0.jar",
                "assets/concurrent-futures-1.1.0.jar",
                "assets/core.jar",
                "assets/core-1.8.0.aar",
                "assets/core-common-2.2.0.jar",
                "assets/core-ktx-1.2.0.aar",
                "assets/core-lambda-stubs.jar",
                "assets/core-runtime-2.2.0.aar",
                "assets/customview-1.0.0.aar",
                "assets/fragment.jar",
                "assets/gdx.jar",
                "assets/koin-core.jar",
                "assets/kotlin-stdlib-1.8.20.jar",
                "assets/kotlin-stdlib-common-1.8.20.jar",
                "assets/kotlin-stdlib-jdk7-1.6.21.jar",
                "assets/kotlin-stdlib-jdk8-1.6.21.jar",
                "assets/kotlinx-coroutines-android-1.6.4.jar",
                "assets/kotlinx-coroutines-core-jvm-1.6.4.jar",
                "assets/lifecycle-common-2.6.1.jar",
                "assets/lifecycle-livedata-2.6.1.aar",
                "assets/lifecycle-livedata-core-2.6.1.aar",
                "assets/lifecycle-runtime-2.6.1.aar",
                "assets/lifecycle-viewmodel-2.6.1.aar",
                "assets/lifecycle-viewmodel-savedstate-2.6.1.aar",
                "assets/listenablefuture-1.0.jar",
                "assets/loader-1.0.0.aar",
                "assets/profileinstaller-1.3.0.aar",
                "assets/savedstate-1.2.1.aar",
                "assets/startup-runtime-1.1.1.aar",
                "assets/tracing-1.0.0.aar",
                "assets/versionedparcelable-1.1.1.aar",
                "assets/viewpager-1.0.0.aar",
                "assets/xmlpull-1.1.3.1.jar",
                "assets/xpp3_min-1.1.4c.jar",
                "assets/xstream.jar"
            )

            val toRemove = mutableListOf<String>()

            module.zipEntryMap.listInputSources().forEach { entry ->
                val name = entry.name

                if (name in junkFiles ||
                    name.startsWith("keys/") ||
                    name.startsWith("license/") ||
                    name.startsWith("assets/catblocks/") ||
                    name.startsWith("frameworks/") ||
                    name.startsWith("assets/dexopt/") ||
                    name.contains("grs_sdk") ||
                    name.contains("libcrashlytics") ||
                    name.contains("libexample_core.so")
                ) {
                    toRemove.add(name)
                    return@forEach
                }

                if (excludeCompiler && name in compilerAssets) {
                    toRemove.add(name)
                    return@forEach
                }

                if (excludeCompiler && name.contains("libquickjs.so")) {
                    toRemove.add(name)
                    return@forEach
                }

                if (excludePython) {
                    if (name.startsWith("assets/python3.12/") || name.startsWith("assets/default_pylibs/")) {
                        toRemove.add(name)
                        return@forEach
                    }
                    if (name.contains("libpython3.12.so") || name.contains("libopenblas.so") || name.contains("libllm.so")) {
                        toRemove.add(name)
                        return@forEach
                    }
                }

                if (excludeQemu) {
                    if (name.startsWith("assets/qemu_x86_64/")) {
                        toRemove.add(name)
                        return@forEach
                    }
                    if (name.contains("vnc")) {
                        toRemove.add(name)
                        return@forEach
                    }
                }

                if (excludePython && excludeQemu) {
                    val sharedLibs = listOf(
                        "libcrypto.so", "libssl.so", "libz.so",
                        "libexpat.so", "libjpeg.so", "libpng.so"
                    )
                    if (sharedLibs.any { name.contains(it) }) {
                        toRemove.add(name)
                        return@forEach
                    }
                }

                if (excludeOnnx) {
                    if (name.contains("libonnxruntime.so") || name.contains("libonnxruntime4j_jni.so")) {
                        toRemove.add(name)
                        return@forEach
                    }
                }

                if (excludeMnn) {
                    val mnnLibs = listOf(
                        "libMNN.so", "libMNN_CL.so", "libMNN_Express.so",
                        "libMNN_Vulkan.so", "libMNNAudio.so", "libmnncore.so", "libMNNOpenCV.so"
                    )
                    if (mnnLibs.any { name.contains(it) }) {
                        toRemove.add(name)
                        return@forEach
                    }
                }

                if (excludeMkit) {
                    if (name.startsWith("assets/mlkit_odt_default_classifier/") ||
                        name.startsWith("assets/mlkit_odt_localizer/") ||
                        name.startsWith("assets/mlkit_pose/") ||
                        name.startsWith("assets/kpms-mlkit/") ||
                        name == "assets/langid_model.smfb.jpg" ||
                        name == "assets/tts_sdk_model_config.json"
                    ) {
                        toRemove.add(name)
                        return@forEach
                    }

                    val mlkitLibs = listOf(
                        "libxeno_native.so", "libmlkitcommonpipeline.so",
                        "liblanguage_id_jni.so", "libml-vadenergy.so"
                    )
                    if (mlkitLibs.any { name.contains(it) }) {
                        toRemove.add(name)
                        return@forEach
                    }
                }
            }

            toRemove.distinct().forEach { path ->
                module.zipEntryMap.remove(path)
            }
        }
    }

    fun recompressApk(inputFile: File, outputFile: File): Boolean {
        val tempOutput = File("${outputFile.absolutePath}.tmp")
        tempOutput.delete()

        return try {
            FileInputStream(inputFile).use { fis ->
                java.util.zip.ZipInputStream(fis).use { zis ->
                    FileOutputStream(tempOutput).use { fos ->
                        java.util.zip.ZipOutputStream(fos).use { zos ->
                            zos.setLevel(java.util.zip.Deflater.BEST_COMPRESSION)

                            var entry: java.util.zip.ZipEntry?
                            while (zis.nextEntry.also { entry = it } != null) {
                                val currentEntry = entry!!
                                val newEntry = java.util.zip.ZipEntry(currentEntry.name)

                                val name = currentEntry.name
                                val shouldCompress = name.endsWith(".dex") ||
                                        name.endsWith(".so") ||
                                        name.endsWith(".xml") ||
                                        name.startsWith("assets/")

                                if (shouldCompress) {
                                    newEntry.method = java.util.zip.ZipEntry.DEFLATED
                                } else {
                                    newEntry.method = currentEntry.method
                                    if (currentEntry.method == java.util.zip.ZipEntry.STORED) {
                                        newEntry.size = currentEntry.size
                                        newEntry.compressedSize = currentEntry.compressedSize
                                        newEntry.crc = currentEntry.crc
                                    }
                                }

                                zos.putNextEntry(newEntry)
                                zis.copyTo(zos)
                                zos.closeEntry()
                                zis.closeEntry()
                            }
                        }
                    }
                }
            }

            if (tempOutput.exists()) {
                outputFile.delete()
                tempOutput.renameTo(outputFile)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to recompress APK with Level 9 Deflate", e)
            tempOutput.delete()
            false
        }
    }

    fun updateManifest(apkPath: String, config: ManifestConfig): Boolean {
        return modifyApk(apkPath) { module ->
            val manifest = module.androidManifestBlock
            val oldPackage = manifest.packageName
            val newPackage = config.packageName ?: oldPackage

            makeClassNamesAbsolute(manifest.manifestElement, oldPackage)

            if (config.packageName != null) {
                manifest.packageName = newPackage
                fixManifestAttributes(manifest.manifestElement, oldPackage, newPackage)

                if (module.hasTableBlock()) {
                    module.tableBlock.listPackages().forEach { pkg ->
                        if (pkg.name == oldPackage) {
                            pkg.name = newPackage
                        }
                    }
                    module.tableBlock.refresh()
                }
            }

            if (config.versionCode != null) manifest.versionCode = config.versionCode
            if (config.versionName != null) manifest.versionName = config.versionName
            if (config.minSdkVersion != null) manifest.minSdkVersion = config.minSdkVersion
            if (config.targetSdkVersion != null) manifest.targetSdkVersion = config.targetSdkVersion

            if (config.appName != null) {
                val appElem = manifest.applicationElement
                val labelAttr = appElem.getOrCreateAndroidAttribute("label", 0x01010001)
                labelAttr.valueAsString = config.appName
            }

            if (config.debuggable != null) {
                val appElem = manifest.applicationElement
                val debugAttr = appElem.getOrCreateAndroidAttribute("debuggable", 0x0101000f)
                debugAttr.setValueAsBoolean(config.debuggable)
            }

            config.permissionsToAdd?.forEach { perm ->
                manifest.addUsesPermission(perm)
            }

            config.permissionsToRemove?.forEach { permToRemove ->
                val root = manifest.manifestElement
                val permissions = root.listElements("uses-permission")
                val toDelete = mutableListOf<ResXmlElement>()

                for (permElem in permissions) {
                    val nameAttr = permElem.searchAttributeByResourceId(0x01010003)
                    if (nameAttr?.valueAsString == permToRemove) {
                        toDelete.add(permElem)
                    }
                }
                toDelete.forEach { root.remove(it) }
            }
        }
    }

    fun addFileToApk(apkPath: String, sourceFile: File, pathInsideApk: String): Boolean {
        if (!sourceFile.exists() || sourceFile.isDirectory) return false
        return modifyApk(apkPath) { module ->
            module.zipEntryMap.remove(pathInsideApk)
            module.add(FileInputSource(sourceFile, pathInsideApk))
        }
    }

    fun addFolderToApk(apkPath: String, sourceFolder: File, destPathInApk: String): Boolean {
        if (!sourceFolder.exists() || !sourceFolder.isDirectory) return false
        return modifyApk(apkPath) { module ->
            sourceFolder.walk().forEach { file ->
                if (file.isFile) {
                    val relativePath = file.toRelativeString(sourceFolder)
                    val finalPath = if (destPathInApk.isEmpty()) {
                        relativePath
                    } else {
                        val cleanDest = destPathInApk.trimEnd('/')
                        "$cleanDest/$relativePath"
                    }
                    module.zipEntryMap.remove(finalPath)
                    module.add(FileInputSource(file, finalPath))
                }
            }
        }
    }

    fun deleteFromApk(apkPath: String, pathPattern: String): Boolean {
        return modifyApk(apkPath) { module ->
            val cleanPattern = pathPattern.replace("\\", "/")
            val toRemove = ArrayList<String>()

            for (entry in module.zipEntryMap.listInputSources()) {
                val entryName = entry.name
                val isDirectoryMatch = entryName.startsWith("$cleanPattern/")
                val isFileMatch = entryName == cleanPattern

                if (isFileMatch || isDirectoryMatch) {
                    toRemove.add(entryName)
                }
            }

            for (name in toRemove) {
                module.zipEntryMap.remove(name)
            }
        }
    }

    fun extractFileFromApk(apkPath: String, pathInsideApk: String, outputLocalPath: String): Boolean {
        var module: ApkModule? = null
        try {
            module = ApkModule.loadApkFile(File(apkPath))
            val entry = module.zipEntryMap.getInputSource(pathInsideApk)
            return if (entry != null) {
                File(outputLocalPath).parentFile?.mkdirs()
                entry.openStream().use { input ->
                    FileOutputStream(outputLocalPath).use { output ->
                        input.copyTo(output)
                    }
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        } finally {
            try { module?.close() } catch (e: Exception) {}
        }
    }

    private fun makeClassNamesAbsolute(element: ResXmlElement, oldPkg: String) {
        val tagName = element.name
        val componentTags = listOf("activity", "service", "receiver", "provider", "application")

        if (componentTags.contains(tagName)) {
            val nameAttr = element.searchAttributeByResourceId(0x01010003) // android:name
            val value = nameAttr?.valueAsString
            if (value != null) {
                if (value.startsWith(".")) {
                    nameAttr.valueAsString = oldPkg + value
                } else if (!value.contains(".")) {
                    nameAttr.valueAsString = "$oldPkg.$value"
                }
            }
        }
        element.listElements().forEach { child ->
            makeClassNamesAbsolute(child, oldPkg)
        }
    }

    private fun fixManifestAttributes(element: ResXmlElement, oldPkg: String, newPkg: String) {
        val tagName = element.name
        val componentTags = listOf("activity", "service", "receiver", "provider", "application", "activity-alias")
        val componentClassAttributes = listOf("name", "targetActivity", "manageSpaceActivity")

        element.attributes.forEach { attr ->
            val value = attr.valueAsString
            if (value != null) {
                val attrName = attr.name
                val isComponentClassName = componentTags.contains(tagName) && componentClassAttributes.contains(attrName)

                if (!isComponentClassName) {
                    attr.valueAsString = renameAppIdentityPrefixes(value, newPkg)
                }
            }
        }
        element.listElements().forEach { child ->
            fixManifestAttributes(child, oldPkg, newPkg)
        }
    }

    private fun renameAppIdentityPrefixes(value: String, newPkg: String): String {
        val prefixes = listOf(
            "org.DanVexTeam.ChEngine",
            "org.danvexteam.chengine",
            "chengine",
            "org.catrobat.catroid"
        )
        var result = value
        for (prefix in prefixes) {
            if (result.contains(prefix)) {
                result = result.replace(prefix, newPkg)
            }
        }
        return result
    }

    fun signApk(
        context: Context,
        inputApkPath: String,
        outputApkPath: String,
        keyStorePath: String?,
        keyAlias: String?,
        keyPass: String?
    ): Boolean {
        return try {
            val input = File(inputApkPath)
            val output = File(outputApkPath)
            val signerBuilder: ApkSigner.Builder

            if (keyStorePath != null && File(keyStorePath).exists()) {
                val ks = KeyStore.getInstance("PKCS12")
                FileInputStream(keyStorePath).use { ks.load(it, keyPass?.toCharArray()) }

                val alias = keyAlias ?: ks.aliases().nextElement()
                val privateKey = ks.getKey(alias, keyPass?.toCharArray()) as PrivateKey
                val cert = ks.getCertificate(alias) as X509Certificate

                val config = com.android.apksig.ApkSigner.SignerConfig.Builder("CERT", privateKey, listOf(cert)).build()
                signerBuilder = ApkSigner.Builder(listOf(config))
            } else {
                val tempKey = File(context.cacheDir, "debug_auto.jks")
                if (!tempKey.exists()) {
                    generateKeyStore(tempKey.absolutePath, "debug", "android", "Debug User")
                }
                return signApk(context, inputApkPath, outputApkPath, tempKey.absolutePath, "debug", "android")
            }

            signerBuilder
                .setInputApk(input)
                .setOutputApk(output)
                .setV1SigningEnabled(true)
                .setV2SigningEnabled(true)
                .build()
                .sign()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun generateKeyStore(outputPath: String, alias: String, pass: String, commonName: String): Boolean {
        return try {
            val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
            keyPairGenerator.initialize(2048)
            val keyPair = keyPairGenerator.generateKeyPair()

            val notBefore = Date(System.currentTimeMillis() - 1000L * 60 * 60 * 24)
            val notAfter = Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 365 * 25)
            val serialNumber = BigInteger.valueOf(System.currentTimeMillis())
            val owner = X500Principal("CN=$commonName, OU=CatroidBuilder, O=ChEngine, C=WW")

            val certBuilder = JcaX509v3CertificateBuilder(
                owner,
                serialNumber,
                notBefore,
                notAfter,
                owner,
                keyPair.public
            )

            val contentSigner = JcaContentSignerBuilder("SHA256WithRSA")
                .build(keyPair.private)

            val certHolder = certBuilder.build(contentSigner)
            val cert = JcaX509CertificateConverter().getCertificate(certHolder)

            val ks = KeyStore.getInstance("PKCS12")
            ks.load(null, null)
            ks.setKeyEntry(alias, keyPair.private, pass.toCharArray(), arrayOf(cert))

            File(outputPath).parentFile?.mkdirs()
            FileOutputStream(outputPath).use { ks.store(it, pass.toCharArray()) }

            Log.d(TAG, "Successfully generated KeyStore: $outputPath")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Key generation failed", e)
            false
        }
    }

    private fun modifyApk(apkPath: String, action: (ApkModule) -> Unit): Boolean {
        var module: ApkModule? = null
        val tempFile = File("$apkPath.tmp")
        val originalFile = File(apkPath)

        return try {
            if (!originalFile.exists()) return false

            module = ApkModule.loadApkFile(originalFile)
            module.setLoadDefaultFramework(false)

            if (module.hasTableBlock()) {
                module.tableBlock.stringPool.setFlagSorted(false)
                module.tableBlock.stringPool.styleArray.clear()
            }

            action(module)

            if (module.hasTableBlock()) {
                module.tableBlock.refresh()
            }
            module.androidManifestBlock.refresh()

            module.writeApk(tempFile)
            module.close()

            if (originalFile.delete()) {
                tempFile.renameTo(originalFile)
            } else {
                tempFile.copyTo(originalFile, overwrite = true)
                tempFile.delete()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            try { module?.close() } catch (ignored: Exception) {}
            tempFile.delete()
            false
        }
    }
}
