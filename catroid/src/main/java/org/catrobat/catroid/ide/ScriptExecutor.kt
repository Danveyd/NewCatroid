package org.catrobat.catroid.ide

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import com.android.tools.r8.D8
import com.android.tools.r8.D8Command
import com.android.tools.r8.OutputMode
import com.android.tools.r8.R8
import com.android.tools.r8.R8Command
import dalvik.system.DexClassLoader
import org.catrobat.catroid.R
import java.io.*
import java.lang.reflect.InvocationTargetException
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

data class CompilationResult(
    val dexFiles: List<File>,
    val extraAssets: List<File>,
    val error: Throwable? = null
)

object ScriptExecutor {

    private var inputWriter: PipedOutputStream? = null
    var lastCompiledDex: File? = null

    private var bootClasspath: Path? = null

    fun sendInput(text: String) {
        try {
            inputWriter?.write((text + "\n").toByteArray())
            inputWriter?.flush()
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun runD8(
        context: Context,
        inputFiles: List<File>,
        libraries: List<File>,
        outputFolder: File,
        minApi: Int = 21,
        isRelease: Boolean = false,
        targetApi: Int
    ) {

        val bootJar = getBootJar(context, targetApi)

        val mode = if (isRelease) com.android.tools.r8.CompilationMode.RELEASE
        else com.android.tools.r8.CompilationMode.DEBUG

        val builder = D8Command.builder()
            .setMode(mode)
            .setMinApiLevel(minApi)
            .setIntermediate(false)
            .setOutput(outputFolder.toPath(), OutputMode.DexIndexed)
            .addLibraryFiles(bootJar.toPath())

        inputFiles.forEach { file ->
            if (file.exists()) builder.addProgramFiles(file.toPath())
        }

        libraries.forEach { lib ->
            if (lib.exists()) builder.addClasspathFiles(lib.toPath())
        }

        D8.run(builder.build())
    }


    fun runScript(
        context: Context,
        projectPath: String,
        layoutContainer: FrameLayout,
        onPrint: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        Thread {
            try {
                System.gc()
                val projectDir = File(projectPath)
                val srcDir = File(projectDir, "src")
                val libraries = DependencyManager.getLibraries(projectPath)
                val cacheDir = context.codeCacheDir
                val uniqueId = System.currentTimeMillis()

                val classesDir = File(cacheDir, "classes_$uniqueId")
                val optimizedDir = File(cacheDir, "optimized_$uniqueId")
                val processedLibsDir = File(projectDir, "libs_processed")
                val nativeLibsDir = File(cacheDir, "native_libs_$uniqueId")

                classesDir.mkdirs(); optimizedDir.mkdirs(); nativeLibsDir.mkdirs()
                if (!processedLibsDir.exists()) processedLibsDir.mkdirs()

                val logStream = object : OutputStream() {
                    private val buffer = ByteArrayOutputStream()
                    override fun write(b: Int) { buffer.write(b); if (b == '\n'.code) flushLine() }
                    override fun write(b: ByteArray, off: Int, len: Int) {
                        buffer.write(b, off, len); if (len > 0 && b[off + len - 1] == '\n'.code.toByte()) flushLine()
                    }
                    private fun flushLine() {
                        val text = buffer.toString("UTF-8").trimEnd()
                        if (text.isNotEmpty()) onPrint(text)
                        buffer.reset()
                    }
                }
                val printStream = PrintStream(logStream, true, "UTF-8")
                val pipeIn = PipedInputStream()
                inputWriter = PipedOutputStream(pipeIn)

                val oldOut = System.out; val oldErr = System.err; val oldIn = System.`in`
                System.setOut(printStream); System.setErr(printStream); System.setIn(pipeIn)

                try {
                    val config = ProjectManager.loadConfig(projectPath)

                    val classpath = ArrayList<String>()

                    if (libraries.isNotEmpty()) {
                        onPrint("> Processing libraries...")
                        for (lib in libraries) {
                            try {
                                val processedJar = processLibrary(context, lib, processedLibsDir, nativeLibsDir, config.targetSdk)
                                if (processedJar.exists()) classpath.add(processedJar.absolutePath)
                            } catch (e: Exception) { onPrint("! Error lib ${lib.name}: ${e.message}") }
                        }
                    }

                    val projectJniLibs = File(projectDir, "jniLibs")
                    if (projectJniLibs.exists()) {
                        onPrint("> Loading native libs...")
                        val supportedAbis = Build.SUPPORTED_ABIS
                        var foundAbiFolder: File? = null
                        for (abi in supportedAbis) {
                            val f = File(projectJniLibs, abi)
                            if (f.exists()) { foundAbiFolder = f; break }
                        }
                        foundAbiFolder?.listFiles()?.filter { it.extension == "so" }?.forEach {
                            it.copyTo(File(nativeLibsDir, it.name), true)
                        }
                    }


                    onPrint("> Compiling Java...")
                    val bootJar = getBootJar(context, config.targetSdk)
                    val stubsJar = getLambdaStubs(context)

                    val classBytesMap = AndroidECJ.compileDirectory(srcDir, libraries, bootJar, stubsJar)

                    if (classBytesMap.isEmpty()) throw RuntimeException("No compiled classes found.")

                    for ((name, bytes) in classBytesMap) {
                        val classFile = File(classesDir, name)
                        classFile.parentFile?.mkdirs()
                        classFile.writeBytes(bytes)
                    }


                    onPrint("> Linking...")
                    val dexOutDir = File(optimizedDir, "dex_temp")
                    dexOutDir.mkdirs()

                    val classFiles = classesDir.walkTopDown().filter { it.extension == "class" }.toList()
                    runD8(context, classFiles, libraries, dexOutDir, minApi = 26, isRelease = false, targetApi = config.targetSdk)

                    val userDexFile = File(dexOutDir, "classes.dex")
                    val userFinalJar = File(optimizedDir, "app_run.jar")

                    createJarFromClasses(classesDir, userFinalJar)
                    addFileToJar(userFinalJar, userDexFile, "classes.dex")
                    userFinalJar.setReadOnly()
                    classpath.add(0, userFinalJar.absolutePath)


                    onPrint("> Loading...")
                    val classLoader = DexClassLoader(
                        classpath.joinToString(File.pathSeparator),
                        optimizedDir.absolutePath,
                        nativeLibsDir.absolutePath,
                        context.classLoader
                    )


                    val mainClassEntry = classBytesMap.keys.find { it.endsWith("Main.class") }
                        ?: throw RuntimeException("Class 'Main' not found.")


                    val mainClassName = mainClassEntry.removeSuffix(".class").replace(File.separatorChar, '.').replace('/', '.')

                    val mainClass = classLoader.loadClass(mainClassName)
                    val instance = mainClass.newInstance()

                    var methodInvoked = false
                    val signatures = listOf(
                        arrayOf(Context::class.java, ViewGroup::class.java) to arrayOf(context, layoutContainer),
                        arrayOf(Context::class.java) to arrayOf(context),
                        emptyArray<Class<*>>() to emptyArray<Any>()
                    )

                    for ((paramTypes, args) in signatures) {
                        try {
                            val method = mainClass.getMethod("onStart", *paramTypes)
                            if (paramTypes.contains(ViewGroup::class.java)) {
                                Handler(Looper.getMainLooper()).post {
                                    layoutContainer.removeAllViews()
                                    try { method.invoke(instance, *args) }
                                    catch (e: Exception) { e.printStackTrace(); onPrint("UI Error: ${e.cause?.message}") }
                                }
                            } else {
                                method.invoke(instance, *args)
                            }
                            methodInvoked = true
                            break
                        } catch (e: NoSuchMethodException) {}
                    }
                    if (!methodInvoked) throw RuntimeException("Method 'onStart' not found.")

                } finally {
                    System.setOut(oldOut); System.setErr(oldErr); System.setIn(oldIn); inputWriter = null
                }
            } catch (e: Exception) {
                val cause = if (e is InvocationTargetException) e.cause else e
                onError("Runtime Error: ${cause?.message}")
                cause?.printStackTrace()
            }
        }.start()
    }


    fun compileForApk(
        context: Context,
        projectPath: String,
        packageName: String,
        onStatus: (String) -> Unit
    ): CompilationResult {
        val projectDir = File(projectPath)
        val originalSrcDir = File(projectDir, "src")
        val cacheDir = context.codeCacheDir
        val libraries = DependencyManager.getLibraries(projectPath)
        val config = ProjectManager.loadConfig(projectPath)
        val stubsJar = getLambdaStubs(context)
        val bootJar = getBootJar(context, config.targetSdk)

        val buildDir = File(cacheDir, "apk_build_${System.currentTimeMillis()}")
        val classesDir = File(buildDir, "classes")
        classesDir.mkdirs()

        val resultDexFiles = ArrayList<File>()
        val extraAssets = ArrayList<File>()

        try {
            var compileSrcDir = originalSrcDir

            if (config.isProtected) {
                onStatus(context.getString(R.string.se_protection_preparing))
                val tempSrc = File(buildDir, "src_temp")
                originalSrcDir.copyRecursively(tempSrc, true)
                val mainFile = File(tempSrc, "game/Main.java")
                if (mainFile.exists()) {
                    var content = mainFile.readText()

                    content = content.replace("class Main", "class SecretMain")
                    content = content.replace("public Main(", "public SecretMain(")
                    content = content.replace("Main.this", "SecretMain.this")
                    content = content.replace(Regex("\\bMain\\."), "SecretMain.")
                    content = content.replace(Regex("\\bMain::"), "SecretMain::")

                    mainFile.delete()
                    File(tempSrc, "game/SecretMain.java").writeText(content)
                }
                compileSrcDir = tempSrc
            }

            onStatus(context.getString(R.string.se_compiling))
            val classBytesMap = AndroidECJ.compileDirectory(compileSrcDir, libraries, bootJar, stubsJar)
            if (classBytesMap.isEmpty()) throw RuntimeException(context.getString(R.string.se_no_compiled_classes))

            for ((name, bytes) in classBytesMap) {
                val classFile = File(classesDir, name)
                classFile.parentFile?.mkdirs()
                classFile.writeBytes(bytes)
            }

            onStatus(context.getString(R.string.se_generating_dex))
            val userDexDir = File(buildDir, "user_dex")
            userDexDir.mkdirs()
            val classFiles = classesDir.walkTopDown().filter { it.extension == "class" }.toList()

            val finalUserDexSource: File

            if (config.isObfuscated) {
                onStatus(context.getString(R.string.se_obfuscating))

                val rulesFile = File(projectPath, "proguard-rules.pro")
                if (!rulesFile.exists()) {

                    val mainClass = if(config.isProtected) "game.SecretMain" else "game.Main"
                    rulesFile.writeText("""
            # Правила ProGuard для R8.
            
            -keep public class game.Main {
                public <init>();

                public void onStart(...);
            }
            
            -keep public class game.SecretMain {
                public <init>();

                public void onStart(...);
            }
                """.trimIndent())
                    onStatus(context.getString(R.string.se_proguard_created))
                }

                val command = R8Command.builder()
                    .setMode(com.android.tools.r8.CompilationMode.RELEASE)
                    .setMinApiLevel(21)
                    .addProgramFiles(classFiles.map { it.toPath() })
                    .addLibraryFiles(bootJar.toPath())
                    .addClasspathFiles(libraries.map { it.toPath() })
                    .setOutput(userDexDir.toPath(), OutputMode.DexIndexed)
                    .addProguardConfigurationFiles(rulesFile.toPath())
                    .build()

                R8.run(command)

            } else {
                onStatus(context.getString(R.string.se_generating_dex))
                runD8(context, classFiles, libraries, userDexDir, minApi = 21, isRelease = true, targetApi = config.targetSdk)
            }

            finalUserDexSource = File(userDexDir, "classes.dex")
            if (!finalUserDexSource.exists()) throw RuntimeException("D8/R8 failed to produce classes.dex")


            if (libraries.isNotEmpty()) {
                onStatus(context.getString(R.string.se_processing_libs))
                var dexCounter = 2
                for (lib in libraries) {
                    try {
                        if (lib.length() < 100) continue
                        val libHash = lib.name.hashCode().toString()
                        val cachedDex = File(context.codeCacheDir, "lib_d8_$libHash.dex")

                        if (!cachedDex.exists()) {
                            val tempOut = File(buildDir, "temp_lib_$dexCounter")
                            tempOut.mkdirs()
                            runD8(context, listOf(lib), emptyList(), tempOut, minApi = 21, isRelease = true, targetApi = config.targetSdk)
                            File(tempOut, "classes.dex").copyTo(cachedDex)
                            tempOut.deleteRecursively()
                        }

                        val targetDex = File(buildDir, "classes$dexCounter.dex")
                        cachedDex.copyTo(targetDex, true)
                        resultDexFiles.add(targetDex)
                        dexCounter++
                    } catch (e: Exception) {}
                }
            }

            return CompilationResult(resultDexFiles, extraAssets)

        } catch (e: Exception) {
            e.printStackTrace()
            onStatus(context.getString(R.string.common_error_with_message, e.message))
            return CompilationResult(emptyList(), emptyList(), e)
        }
    }


    fun processLibrary(context: Context, originLib: File, outputDir: File, nativeDir: File, targetApi: Int = 33): File {
        try {
            ZipFile(originLib).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.name.endsWith(".so")) extractNativeLib(entry.name, zip.getInputStream(entry), nativeDir)
                }
            }
        } catch (e: Exception) {}

        val finalJar = File(outputDir, originLib.name)
        if (finalJar.exists()) return finalJar

        val tempDexDir = File(outputDir, "dex_${originLib.name.hashCode()}")
        tempDexDir.mkdirs()

        runD8(context, listOf(originLib), emptyList(), tempDexDir, minApi = 26, isRelease = false, targetApi = targetApi)

        originLib.copyTo(finalJar, true)
        addFileToJar(finalJar, File(tempDexDir, "classes.dex"), "classes.dex")
        tempDexDir.deleteRecursively()

        finalJar.setReadOnly()

        return finalJar
    }

    private fun extractNativeLib(name: String, input: InputStream, outputDir: File) {
        val abis = Build.SUPPORTED_ABIS.toList()
        var isCompatible = true
        val knownAbis = listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")

        for (known in knownAbis) {
            if (name.contains(known) && !abis.contains(known)) {
                isCompatible = false
                break
            }
        }

        if (isCompatible) {
            val dest = File(outputDir, File(name).name)

            if (!dest.exists() || dest.length() <= 0) {
                input.use { i -> dest.outputStream().use { o -> i.copyTo(o) } }
            }
        }
    }

    private fun addFileToJar(jarFile: File, fileToAdd: File, entryName: String) {
        val tempJar = File(jarFile.parent, jarFile.name + ".tmp")
        if (!fileToAdd.exists()) return

        ZipFile(jarFile).use { zipIn ->
            ZipOutputStream(FileOutputStream(tempJar)).use { zipOut ->
                val entries = zipIn.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.name == entryName) continue
                    zipOut.putNextEntry(ZipEntry(entry.name))
                    zipIn.getInputStream(entry).copyTo(zipOut)
                    zipOut.closeEntry()
                }
                zipOut.putNextEntry(ZipEntry(entryName))
                FileInputStream(fileToAdd).copyTo(zipOut)
                zipOut.closeEntry()
            }
        }
        if (jarFile.exists()) jarFile.delete()
        tempJar.renameTo(jarFile)
    }

    private fun createJarFromClasses(classesDir: File, outputJar: File) {
        ZipOutputStream(FileOutputStream(outputJar)).use { zipOut ->
            classesDir.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val relativePath = file.toRelativeString(classesDir)
                    zipOut.putNextEntry(ZipEntry(relativePath))
                    FileInputStream(file).copyTo(zipOut)
                    zipOut.closeEntry()
                }
            }
        }
    }

    private fun getBootJar(context: Context, targetApi: Int): File {
        val jar = IdeSettings.getAndroidJar(context, targetApi)

        if (!jar.exists()) {
            throw RuntimeException(context.getString(R.string.se_sdk_not_found, targetApi))
        }
        return jar
    }

    private fun getLambdaStubs(context: Context): File {
        val destFile = File(context.filesDir, "core-lambda-stubs.jar")
        if (!destFile.exists()) {
            try {
                context.assets.open("core-lambda-stubs.jar").use { input ->
                    destFile.outputStream().use { output -> input.copyTo(output) }
                }
            } catch (e: IOException) {
                throw RuntimeException(context.getString(R.string.se_stubs_not_found))
            }
        }
        return destFile
    }
}
