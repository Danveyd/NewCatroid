package org.catrobat.catroid.python

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.content.Project
import org.catrobat.catroid.utils.git.TokenManager
import java.io.File
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.TransportException
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider

interface CommandOutputListener {
    fun onOutput(output: String)
    fun onComplete()
    fun onOpenEditor(file: File)
}

class PythonCommandManager(
    private val pythonEngine: PythonEngine,
    private val project: Project
) {
    private val defaultLibsPath: String = project.filesDir.absolutePath
    var outputListener: CommandOutputListener? = null
    var currentWorkingDirectory: File = project.filesDir
    private val mainThreadHandler = Handler(Looper.getMainLooper())
    private val aliases = mutableMapOf<String, String>()

    fun executeCommandForResult(commandLine: String, onResult: (String) -> Unit) {
        val trimmedCommand = commandLine.trim()
        if (trimmedCommand.isEmpty()) {
            mainThreadHandler.post { onResult("") }
            return
        }

        val outputBuilder = StringBuilder()
        var isFirstOutput = true


        this.outputListener = object : CommandOutputListener {
            override fun onOutput(output: String) {

                if (isFirstOutput && output == "\n> $trimmedCommand\n") {
                    isFirstOutput = false
                    return
                }
                outputBuilder.append(output)
            }

            override fun onComplete() {

                mainThreadHandler.post {
                    onResult(outputBuilder.toString().trim())
                }

                this@PythonCommandManager.outputListener = null
            }

            override fun onOpenEditor(file: File) {

                mainThreadHandler.post {
                    onResult("Error: Interactive editor (nano) is not supported in block execution.")
                }
                this@PythonCommandManager.outputListener = null
            }
        }


        processCommand(trimmedCommand)
    }

    fun processCommand(commandLine: String) {
        var trimmedCommand = commandLine.trim()
        if (trimmedCommand.isEmpty() || trimmedCommand.startsWith("#")) {
            outputListener?.onComplete()
            return
        }

        val parts = trimmedCommand.split("\\s+".toRegex())
        val originalCmd = parts.first().lowercase()
        val originalArgs = parts.drop(1)


        if (aliases.containsKey(originalCmd)) {
            trimmedCommand = aliases[originalCmd]!! + if (originalArgs.isNotEmpty()) " " + originalArgs.joinToString(" ") else ""
        }

        val finalParts = trimmedCommand.split("\\s+".toRegex())
        val cmd = finalParts.first().lowercase()
        val args = finalParts.drop(1)

        outputListener?.onOutput("\n> $trimmedCommand\n")


        when (cmd) {
            "pip" -> handlePipCommand(args)
            "clear" -> {
                pythonEngine.clearEnvironment()
                printOutput("Python environment reset.")
            }
            "pwd" -> printOutput(currentWorkingDirectory.absolutePath)
            "cd" -> handleCd(args)
            "ls", "dir" -> handleLs(args)
            "mkdir" -> handleMkdir(args)
            "rm" -> handleRm(args)
            "cp" -> handleCp(args)
            "mv" -> handleMv(args)
            "touch" -> handleTouch(args)
            "cat" -> handleCat(args)
            "echo" -> handleEcho(trimmedCommand)
            "nano", "edit", "vi" -> handleNano(args)
            "wget", "curl" -> handleWget(args)
            "grep" -> handleGrep(args)
            "find" -> handleFind(args)
            "unzip" -> handleUnzip(args)
            "sysinfo", "neofetch", "uname" -> handleSysInfo()
            "top", "free" -> handleMemoryInfo()
            "ml", "pocketensor", "pt" -> handleMlCommands(args)
            "vm", "qemu" -> handleVmCommands(args)
            "sh", "run", "./" -> handleSh(args, trimmedCommand)
            "alias" -> handleAlias(args)
            "logcat" -> handleLogcat(args)
            "xxd", "hexdump" -> handleHexdump(args)
            "git" -> handleGit(args)
            "help", "?" -> handleHelp()
            else -> {
                if (cmd.startsWith("./")) {
                    handleSh(listOf(cmd.removePrefix("./")), trimmedCommand)
                } else {
                    executePythonScript(trimmedCommand)
                }
            }
        }
    }

    private fun handleHelp() {
        val helpText = """
            |=== Terminal ===
            |
            | FILE SYSTEM:
            |  ls, dir     - List directory contents
            |  cd          - Change the current directory
            |  pwd         - Print working directory
            |  mkdir       - Create a directory
            |  rm          - Remove files/directories (use -r for recursive)
            |  cp, mv      - Copy or move files
            |  touch       - Create an empty file
            |  cat         - Print file content
            |  echo        - Print text or write to file (>, >>)
            |  find        - Find files by name
            |  unzip       - Extract zip archives
            |
            | TEXT & SYSTEM:
            |  nano, vi    - Open text editor
            |  grep        - Search text in a file
            |  xxd         - View file in HEX format
            |  sh, ./      - Execute a shell script
            |  alias       - Create aliases (e.g., alias ll="ls -l")
            |  wget, curl  - Download files
            |  logcat      - View internal app logs
            |  sysinfo     - Display device information
            |  free        - Display memory usage
            |  clear       - Clear python environment & terminal
            |
            | ML, VM & PYTHON:
            |  pip         - Install Python packages
            |
            | GIT:
            |  git         - Version control (init, clone, status, add, commit, log, pull, push, remote)
            |
            | NOTE: Any unknown command will be evaluated as a Python script
        """.trimMargin()

        printOutput(helpText)
    }

    private fun getCredentialsProvider(): UsernamePasswordCredentialsProvider? {
        val token = TokenManager.getToken(CatroidApplication.getAppContext())
        return if (!token.isNullOrEmpty()) {

            UsernamePasswordCredentialsProvider(token, "")
        } else null
    }

    private fun getGitInstance(): Git? {
        return try {
            val builder = FileRepositoryBuilder()
                .findGitDir(currentWorkingDirectory)

            if (builder.gitDir == null) return null

            val repository = builder.setMustExist(true).build()
            Git(repository)
        } catch (e: Exception) {
            null
        }
    }

    private fun handleGit(args: List<String>) {
        if (args.isEmpty()) {
            return printError("usage: git <command> [<args>]\nCommands: init, clone, status, add, commit, log, pull, push, remote")
        }

        val gitCommand = args[0].lowercase()
        val gitArgs = args.drop(1)

        Thread {
            try {
                when (gitCommand) {
                    "init" -> executeGitInit()
                    "clone" -> executeGitClone(gitArgs)
                    "status" -> executeGitStatus()
                    "add" -> executeGitAdd(gitArgs)
                    "commit" -> executeGitCommit(gitArgs)
                    "log" -> executeGitLog()
                    "pull" -> executeGitPull()
                    "push" -> executeGitPush()
                    "remote" -> executeGitRemote(gitArgs)
                    "login" -> executeGitLogin(gitArgs)
                    "logout" -> executeGitLogout()
                    else -> printError("git: '$gitCommand' is not a supported git command.")
                }
            } catch (e: Exception) {
                if (e is TransportException && e.message?.contains("not authorized", ignoreCase = true) == true) {
                    printError("Git Authentication failed.\nTip: You need to log in. Use: git login <your_github_token>")
                } else {
                    printError("git $gitCommand error: ${e.message}")
                }
            }
        }.start()
    }

    private fun executeGitLogin(args: List<String>) {
        if (args.isEmpty()) {
            val currentToken = TokenManager.getToken(CatroidApplication.getAppContext())
            if (!currentToken.isNullOrEmpty()) {
                return printOutput("You are currently logged in. (Token: ${currentToken.take(4)}...${currentToken.takeLast(4)})")
            }
            return printError("Usage: git login <github_personal_access_token>\nYou can generate it at https://github.com/settings/tokens")
        }

        val token = args[0]
        TokenManager.saveToken(CatroidApplication.getAppContext(), token)
        printOutput("Successfully logged in! Token saved securely in EncryptedSharedPreferences.")
    }

    private fun executeGitLogout() {
        TokenManager.clearToken(CatroidApplication.getAppContext())
        printOutput("Logged out successfully. Token cleared.")
    }



    private fun executeGitPull() {
        val git = getGitInstance() ?: return printError("fatal: not a git repository")
        mainThreadHandler.post { outputListener?.onOutput("Pulling from remote...\n") }

        git.use {
            val pullCmd = it.pull()
            getCredentialsProvider()?.let { cp -> pullCmd.setCredentialsProvider(cp) }

            val result = pullCmd.call()
            if (result.isSuccessful) {
                printOutput("Successfully pulled from remote.")
            } else {
                printOutput("Pull completed. (Could be already up-to-date or have conflicts)")
            }
        }
    }

    private fun executeGitPush() {
        val git = getGitInstance() ?: return printError("fatal: not a git repository")
        mainThreadHandler.post { outputListener?.onOutput("Pushing to remote...\n") }

        git.use {
            val pushCmd = it.push()
            getCredentialsProvider()?.let { cp -> pushCmd.setCredentialsProvider(cp) }

            val results = pushCmd.call()
            val sb = StringBuilder()
            for (res in results) {
                for (update in res.remoteUpdates) {
                    sb.append("${update.remoteName} : ${update.status}\n")
                }
            }
            printOutput(if (sb.isEmpty()) "Everything up-to-date" else sb.toString().trim())
        }
    }

    private fun executeGitRemote(args: List<String>) {
        val git = getGitInstance() ?: return printError("fatal: not a git repository")

        git.use {

            if (args.isEmpty() || args[0] == "-v") {
                val config = it.repository.config
                val url = config.getString("remote", "origin", "url") ?: "No origin set"
                printOutput("origin\t$url")
                return
            }


            if (args.size == 3 && args[0] == "set-url" && args[1] == "origin") {
                val config = it.repository.config
                config.setString("remote", "origin", "url", args[2])
                config.save()
                printOutput("Origin URL updated.")
            } else {
                printError("Usage: git remote -v OR git remote set-url origin <URL>")
            }
        }
    }

    private fun executeGitInit() {
        val gitDir = File(currentWorkingDirectory, ".git")
        if (gitDir.exists()) {
            printOutput("Git repository already exists in ${currentWorkingDirectory.absolutePath}")
            return
        }
        Git.init().setDirectory(currentWorkingDirectory).call().use {
            printOutput("Initialized empty Git repository in ${File(currentWorkingDirectory, ".git").absolutePath}")
        }
    }

    private fun executeGitClone(args: List<String>) {
        if (args.isEmpty()) return printError("fatal: You must specify a repository to clone.")
        val url = args[0]
        val folderName = args.getOrNull(1) ?: url.substringAfterLast("/").removeSuffix(".git")
        val targetDir = File(currentWorkingDirectory, folderName)

        mainThreadHandler.post { outputListener?.onOutput("Cloning into '$folderName'...\n") }

        val cloneCmd = Git.cloneRepository()
            .setURI(url)
            .setDirectory(targetDir)


        getCredentialsProvider()?.let { cloneCmd.setCredentialsProvider(it) }

        cloneCmd.call().use {
            printOutput("Successfully cloned to ${targetDir.absolutePath}")
        }
    }

    private fun executeGitStatus() {
        val git = getGitInstance() ?: return printError("fatal: not a git repository (or any of the parent directories): .git")

        git.use {
            val status = it.status().call()
            val sb = StringBuilder()

            sb.append("On branch ${it.repository.branch}\n\n")

            if (status.isClean) {
                sb.append("nothing to commit, working tree clean\n")
            } else {
                if (status.added.isNotEmpty() || status.changed.isNotEmpty() || status.removed.isNotEmpty()) {
                    sb.append("Changes to be committed:\n")
                    status.added.forEach { f -> sb.append("  (new file)  $f\n") }
                    status.changed.forEach { f -> sb.append("  (modified)  $f\n") }
                    status.removed.forEach { f -> sb.append("  (deleted)   $f\n") }
                    sb.append("\n")
                }

                if (status.modified.isNotEmpty() || status.missing.isNotEmpty()) {
                    sb.append("Changes not staged for commit:\n")
                    status.modified.forEach { f -> sb.append("  (modified)  $f\n") }
                    status.missing.forEach { f -> sb.append("  (deleted)   $f\n") }
                    sb.append("\n")
                }

                if (status.untracked.isNotEmpty()) {
                    sb.append("Untracked files:\n")
                    status.untracked.forEach { f -> sb.append("  $f\n") }
                }
            }
            printOutput(sb.toString().trimEnd())
        }
    }

    private fun executeGitAdd(args: List<String>) {
        val git = getGitInstance() ?: return printError("fatal: not a git repository: .git")
        if (args.isEmpty()) return printError("Nothing specified, nothing added.")

        git.use {
            val addCmd = it.add()
            for (arg in args) {
                addCmd.addFilepattern(arg)
            }
            addCmd.call()
            printComplete()
        }
    }

    private fun executeGitCommit(args: List<String>) {
        val git = getGitInstance() ?: return printError("fatal: not a git repository: .git")


        val msgIndex = args.indexOf("-m")
        if (msgIndex == -1 || msgIndex + 1 >= args.size) {
            return printError("error: message is required. Use: git commit -m \"message\"")
        }


        val message = args.drop(msgIndex + 1).joinToString(" ").removeSurrounding("\"").removeSurrounding("'")

        git.use {
            val commit = it.commit()
                .setMessage(message)
                .setAuthor(PersonIdent("CatroidUser", "user@catroid.org"))
                .call()
            printOutput("[${it.repository.branch} ${commit.name.take(7)}] $message")
        }
    }

    private fun executeGitLog() {
        val git = getGitInstance() ?: return printError("fatal: not a git repository: .git")

        git.use {
            val logs = it.log().setMaxCount(10).call()
            val sb = StringBuilder()
            for (commit in logs) {
                sb.append("commit ${commit.name}\n")
                sb.append("Author: ${commit.authorIdent.name} <${commit.authorIdent.emailAddress}>\n")
                sb.append("Date:   ${commit.authorIdent.`when`}\n\n")
                sb.append("    ${commit.fullMessage}\n\n")
            }
            if (sb.isEmpty()) {
                printOutput("No commits yet.")
            } else {
                printOutput(sb.toString().trimEnd())
            }
        }
    }

    private fun handleSh(args: List<String>, fullCommand: String) {
        val fileName = if (fullCommand.startsWith("./")) fullCommand.removePrefix("./").trim() else args.firstOrNull()
        if (fileName.isNullOrEmpty()) return printError("Usage: sh <script.sh> or ./<script.sh>")

        val file = getFile(fileName)
        if (!file.exists() || file.isDirectory) return printError("sh: $fileName: No such file")

        try {
            val lines = file.readLines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }
            if (lines.isEmpty()) return printComplete()

            executeCommandSequence(lines)
        } catch (e: Exception) {
            printError("sh: Failed to read script - ${e.message}")
        }
    }


    private fun executeCommandSequence(commands: List<String>) {
        var currentIndex = 0
        val originalListener = this.outputListener


        val sequenceListener = object : CommandOutputListener {
            override fun onOutput(output: String) {
                originalListener?.onOutput(output)
            }
            override fun onOpenEditor(file: File) {
                originalListener?.onOpenEditor(file)
            }
            override fun onComplete() {
                currentIndex++
                if (currentIndex < commands.size) {
                    processNext()
                } else {

                    this@PythonCommandManager.outputListener = originalListener
                    originalListener?.onComplete()
                }
            }

            fun processNext() {
                val nextCmd = commands[currentIndex]


                processCommand(nextCmd)
            }
        }


        this.outputListener = sequenceListener
        sequenceListener.processNext()
    }

    private fun handleAlias(args: List<String>) {
        if (args.isEmpty()) {
            val output = aliases.map { "${it.key}='${it.value}'" }.joinToString("\n")
            return printOutput(if (output.isEmpty()) "No aliases defined." else output)
        }

        val assignment = args.joinToString(" ")
        if (!assignment.contains("=")) return printError("Usage: alias name=\"command\"")

        val parts = assignment.split("=", limit = 2)
        val name = parts[0].trim()
        val command = parts[1].trim().removeSurrounding("\"").removeSurrounding("'")

        aliases[name] = command
        printComplete()
    }

    private fun handleLogcat(args: List<String>) {
        val linesCount = args.firstOrNull()?.toIntOrNull() ?: 50
        val pid = android.os.Process.myPid()

        Thread {
            try {

                val process = Runtime.getRuntime().exec("logcat -d -v threadtime --pid=$pid")
                val logText = process.inputStream.bufferedReader().useLines { lines ->
                    lines.toList().takeLast(linesCount).joinToString("\n")
                }
                printOutput("--- Last $linesCount logcat lines ---\n$logText")
            } catch (e: Exception) {
                printError("logcat failed: ${e.message}")
            }
        }.start()
    }

    private fun handleHexdump(args: List<String>) {
        if (args.isEmpty()) return printError("Usage: hexdump <file>")
        val file = getFile(args[0])
        if (!file.exists() || file.isDirectory) return printError("hexdump: ${args[0]}: No such file")

        Thread {
            try {
                val bytes = file.readBytes()
                val limit = minOf(bytes.size, 256)
                val sb = java.lang.StringBuilder()

                sb.append("File: ${file.name} (showing first $limit bytes)\n")

                for (i in 0 until limit step 16) {
                    val row = bytes.sliceArray(i until minOf(i + 16, limit))

                    sb.append(String.format("%08X  ", i))


                    row.forEach { b -> sb.append(String.format("%02X ", b)) }
                    for (pad in row.size until 16) sb.append("   ")

                    sb.append(" |")

                    row.forEach { b ->
                        val c = b.toInt().toChar()
                        sb.append(if (c in ' '..'~') c else '.')
                    }
                    sb.append("|\n")
                }
                printOutput(sb.toString())
            } catch (e: Exception) {
                printError("hexdump failed: ${e.message}")
            }
        }.start()
    }


    private fun getFile(path: String): File {
        return if (path.startsWith("/")) File(path) else File(currentWorkingDirectory, path)
    }

    private fun handleCd(args: List<String>) {
        val path = args.firstOrNull() ?: project.filesDir.absolutePath
        val newDir = getFile(path)

        if (newDir.exists() && newDir.isDirectory) {
            currentWorkingDirectory = newDir.canonicalFile
            printComplete()
        } else {
            printError("cd: $path: No such file or directory")
        }
    }

    private fun handleLs(args: List<String>) {
        val target = if (args.isNotEmpty()) getFile(args[0]) else currentWorkingDirectory
        if (!target.exists() || !target.isDirectory) {
            return printError("ls: cannot access '${args.firstOrNull()}': No such file or directory")
        }
        val files = target.listFiles()?.sortedBy { it.name }
        if (files.isNullOrEmpty()) {
            printOutput("")
        } else {
            val output = files.joinToString("\n") { if (it.isDirectory) "${it.name}/" else it.name }
            printOutput(output)
        }
    }

    private fun handleMkdir(args: List<String>) {
        if (args.isEmpty()) return printError("mkdir: missing operand")
        val dir = getFile(args[0])
        if (dir.mkdirs()) printOutput("Created directory: ${dir.name}")
        else printError("mkdir: cannot create directory '${args[0]}': File exists or permission denied")
    }

    private fun handleRm(args: List<String>) {
        if (args.isEmpty()) return printError("rm: missing operand")
        val isRecursive = args.contains("-r") || args.contains("-rf")
        val targetPath = args.last()
        val file = getFile(targetPath)

        if (!file.exists()) return printError("rm: cannot remove '$targetPath': No such file or directory")

        if (file.isDirectory && !isRecursive) {
            printError("rm: cannot remove '$targetPath': Is a directory (use -r)")
        } else {
            if (file.deleteRecursively()) printOutput("Removed '$targetPath'")
            else printError("rm: failed to remove '$targetPath'")
        }
    }

    private fun handleCp(args: List<String>) {
        if (args.size < 2) return printError("cp: missing file operand")
        val src = getFile(args[0])
        val dest = getFile(args[1])
        if (!src.exists()) return printError("cp: cannot stat '${args[0]}': No such file")

        try {
            src.copyRecursively(dest, overwrite = true)
            printOutput("Copied '${args[0]}' to '${args[1]}'")
        } catch (e: Exception) {
            printError("cp: ${e.message}")
        }
    }

    private fun handleMv(args: List<String>) {
        if (args.size < 2) return printError("mv: missing file operand")
        val src = getFile(args[0])
        val dest = getFile(args[1])
        if (!src.exists()) return printError("mv: cannot stat '${args[0]}': No such file")

        if (src.renameTo(dest)) printOutput("Moved '${args[0]}' to '${args[1]}'")
        else printError("mv: failed to move file")
    }

    private fun handleTouch(args: List<String>) {
        if (args.isEmpty()) return printError("touch: missing operand")
        val file = getFile(args[0])
        if (file.exists()) file.setLastModified(System.currentTimeMillis())
        else file.createNewFile()
        printComplete()
    }

    private fun handleCat(args: List<String>) {
        if (args.isEmpty()) return printError("cat: missing operand")
        val file = getFile(args[0])
        if (!file.exists()) return printError("cat: ${args[0]}: No such file")
        if (file.isDirectory) return printError("cat: ${args[0]}: Is a directory")
        try {
            printOutput(file.readText())
        } catch (e: Exception) {
            printError("cat: ${e.message}")
        }
    }

    private fun handleEcho(fullCommand: String) {
        val withoutCmd = fullCommand.removePrefix("echo").trim()
        try {
            when {
                withoutCmd.contains(">>") -> {
                    val parts = withoutCmd.split(">>", limit = 2)
                    val text = parts[0].trim().removeSurrounding("\"").removeSurrounding("'")
                    getFile(parts[1].trim()).appendText(text + "\n")
                    printComplete()
                }
                withoutCmd.contains(">") -> {
                    val parts = withoutCmd.split(">", limit = 2)
                    val text = parts[0].trim().removeSurrounding("\"").removeSurrounding("'")
                    getFile(parts[1].trim()).writeText(text + "\n")
                    printComplete()
                }
                else -> {
                    val text = withoutCmd.removeSurrounding("\"").removeSurrounding("'")
                    printOutput(text)
                }
            }
        } catch (e: Exception) {
            printError("echo: ${e.message}")
        }
    }

    private fun handleNano(args: List<String>) {
        if (args.isEmpty()) return printError("nano: missing filename")
        val file = getFile(args[0])
        if (file.isDirectory) return printError("nano: ${args[0]} is a directory")


        mainThreadHandler.post {
            outputListener?.onOpenEditor(file)
        }
    }

    private fun handleWget(args: List<String>) {
        if (args.isEmpty()) return printError("wget: missing URL")
        val urlString = args[0]
        val fileName = args.getOrNull(1) ?: urlString.substringAfterLast("/", "downloaded_file")
        val destFile = getFile(fileName)

        mainThreadHandler.post { outputListener?.onOutput("Downloading $urlString to $fileName...\n") }


        Thread {
            try {
                val connection = java.net.URL(urlString).openConnection()
                connection.connect()
                val inputStream = connection.getInputStream()
                destFile.outputStream().use { output ->
                    inputStream.copyTo(output)
                }
                printOutput("Saved: ${destFile.absolutePath}")
            } catch (e: Exception) {
                printError("wget failed: ${e.message}")
            }
        }.start()
    }

    private fun handleGrep(args: List<String>) {
        if (args.size < 2) return printError("Usage: grep <pattern> <file>")
        val pattern = args[0]
        val file = getFile(args[1])

        if (!file.exists() || file.isDirectory) return printError("grep: ${args[1]}: No such file")

        try {
            val regex = pattern.toRegex(RegexOption.IGNORE_CASE)
            val matchedLines = file.useLines { lines ->
                lines.filter { it.contains(regex) }.joinToString("\n")
            }
            if (matchedLines.isNotEmpty()) printOutput(matchedLines)
            else printComplete()
        } catch (e: Exception) {
            printError("grep error: ${e.message}")
        }
    }

    private fun handleFind(args: List<String>) {
        val pattern = args.firstOrNull() ?: return printError("Usage: find <name_part>")
        try {
            val foundFiles = currentWorkingDirectory.walkTopDown()
                .filter { it.name.contains(pattern, ignoreCase = true) }
                .joinToString("\n") { it.absolutePath.removePrefix(project.filesDir.absolutePath).ifEmpty { "/" } }

            if (foundFiles.isNotEmpty()) printOutput(foundFiles)
            else printOutput("No files found matching '$pattern'")
        } catch (e: Exception) {
            printError("find error: ${e.message}")
        }
    }

    private fun handleUnzip(args: List<String>) {
        if (args.isEmpty()) return printError("Usage: unzip <file.zip>")
        val zipFile = getFile(args[0])
        if (!zipFile.exists()) return printError("unzip: cannot find ${args[0]}")

        Thread {
            try {
                java.util.zip.ZipInputStream(zipFile.inputStream()).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val newFile = File(currentWorkingDirectory, entry.name)
                        if (entry.isDirectory) {
                            newFile.mkdirs()
                        } else {
                            newFile.parentFile?.mkdirs()
                            newFile.outputStream().use { zis.copyTo(it) }
                        }
                        mainThreadHandler.post { outputListener?.onOutput("  inflating: ${entry.name}\n") }
                        entry = zis.nextEntry
                    }
                }
                printOutput("Archive extracted successfully.")
            } catch (e: Exception) {
                printError("unzip error: ${e.message}")
            }
        }.start()
    }


    private fun handleSysInfo() {
        val osName = android.os.Build.VERSION.RELEASE
        val device = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"
        val arch = android.os.Build.SUPPORTED_ABIS.joinToString(", ")

        val logo = """
            |    /\_/\    OS: Android $osName
            |   ( o.o )   Device: $device
            |    > ^ <    Arch: $arch
            |             Engine: ChEngine
        """.trimMargin()
        printOutput(logo)
    }

    private fun handleMemoryInfo() {
        val runtime = Runtime.getRuntime()
        val totalMb = runtime.totalMemory() / (1024 * 1024)
        val freeMb = runtime.freeMemory() / (1024 * 1024)
        val maxMb = runtime.maxMemory() / (1024 * 1024)
        val usedMb = totalMb - freeMb

        printOutput("""
            |              total        used        free      max
            |Mem(MB):       $totalMb          $usedMb          $freeMb        $maxMb
        """.trimMargin())
    }

    private fun handleMlCommands(args: List<String>) {
        if (args.isEmpty()) return printOutput("ML Engine Commands: ml info, ml mode <train/eval>")
        when(args[0]) {
            "mode" -> {
                val mode = args.getOrNull(1) == "train"

                printOutput("ML Training mode set to: $mode")
            }
            "info" -> printOutput("Pocketensor Engine Active. Use Python to interact with tensors.")
            else -> printError("Unknown ml command")
        }
    }

    private fun handleVmCommands(args: List<String>) {
        if (args.isEmpty()) return printOutput("VM Commands: vm stop <name>")
        when(args[0]) {
            "stop" -> {
                val vmName = args.getOrNull(1) ?: return printError("Missing VM name")

                printOutput("Send STOP signal to VM: $vmName")
            }
            else -> printError("Unknown vm command")
        }
    }

    private fun printOutput(text: String) {
        mainThreadHandler.post {
            if (text.isNotEmpty()) outputListener?.onOutput("$text\n")
            outputListener?.onComplete()
        }
    }

    private fun printError(text: String) {
        mainThreadHandler.post {
            outputListener?.onOutput("Error: $text\n")
            outputListener?.onComplete()
        }
    }

    private fun printComplete() {
        mainThreadHandler.post { outputListener?.onComplete() }
    }

    private fun handlePipCommand(args: List<String>) {
        val pipScript = buildPipScript(args)
        if (pipScript.startsWith("Error:")) printError(pipScript.removePrefix("Error: "))
        else executePythonScript(pipScript)
    }

    private fun buildPipScript(args: List<String>): String {
        val showOnlyFiles = args.contains("--files")
        val filteredArgs = args.filter { it != "--files" }
        val command = filteredArgs.firstOrNull()
        val packageName = if (filteredArgs.size > 1) filteredArgs[1] else return "Error: No package name specified."

        if (command != "install" && command != "download") {
            return "Error: use 'pip install <package> [--files]'."
        }

        val targetDir = currentWorkingDirectory.absolutePath
        val chaquopyIndex = "https://chaquo.com/pypi-13.1/"
        val pypiIndex = "https://pypi.org/simple"

        var scriptTemplate = if (showOnlyFiles) {
            """
    import os, sys, io
    
    target_dir = '$targetDir'
    files_before = set(os.listdir(target_dir))
    
    original_stdout = sys.stdout
    original_stderr = sys.stderr
    sys.stdout = io.StringIO()
    sys.stderr = io.StringIO()
    
    try:
        from pip._internal.cli.main import main as pip_main
        cli_args = [
            'download', '--only-binary=:all:', '-d', target_dir,
            '--find-links', '$defaultLibsPath', '--index-url', '$chaquopyIndex',
            '--extra-index-url', '$pypiIndex', '$packageName'
        ]
        result = pip_main(cli_args)
    finally:
        sys.stdout = original_stdout
        sys.stderr = original_stderr
        
    if result == 0:
        files_after = set(os.listdir(target_dir))
        new_files = sorted(list(files_after - files_before))
        if new_files:
            print('\\n'.join(new_files))

    """.trimIndent()
        } else {
            """
    import os, sys, io
    from pip._internal.cli.main import main as pip_main

    stdout_capture = io.StringIO()
    stderr_capture = io.StringIO()
    
    original_stdout = sys.stdout
    original_stderr = sys.stderr
    
    sys.stdout = stdout_capture
    sys.stderr = stderr_capture
    
    result = -1
    try:
        cli_args = [
            'download', '--only-binary=:all:', '-d', '$targetDir',
            '--find-links', '$defaultLibsPath', '--index-url', '$chaquopyIndex',
            '--extra-index-url', '$pypiIndex', '$packageName'
        ]
        
        result = pip_main(cli_args)
        
    finally:
        sys.stdout = original_stdout
        sys.stderr = original_stderr

    pip_stdout = stdout_capture.getvalue()
    pip_stderr = stderr_capture.getvalue()

    if pip_stdout:
        print(pip_stdout, end='')
        
    if pip_stderr:
        print(pip_stderr, file=sys.stderr, end='')

    if result == 0:
        print(f"\n--- Pip finished successfully (Code: {result}) ---")
    else:
        print(f"\n--- Pip finished with an error (Code: {result}) ---")
        
    """.trimIndent()
        }

        return scriptTemplate
            .replace("\$targetDir", targetDir)
            .replace("\$defaultLibsPath", defaultLibsPath)
            .replace("\$packageName", packageName)
    }

    private fun executePythonScript(script: String) {
        val wrappedScript = """
import os, sys
try:
    os.chdir('${currentWorkingDirectory.absolutePath.replace("\\", "\\\\")}')
${script.prependIndent("    ")}
except Exception as e:
    import traceback
    traceback.print_exc(file=sys.stdout)
    """.trimIndent()

        pythonEngine.runScriptAsync(wrappedScript) { output ->
            mainThreadHandler.post {
                outputListener?.onOutput(output)
                outputListener?.onComplete()
            }
        }
    }
}