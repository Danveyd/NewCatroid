package org.catrobat.catroid.utils.git

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.MergeCommand
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.api.MergeResult as JGitMergeResult
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.ObjectReader
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.eclipse.jgit.treewalk.TreeWalk
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets

//Ov23liKoq3h0cTgAbVYA
//936da4332f8a31ebed1bc97aa5d2f89a989a56d2

/**
 * Основной контроллер для управления Git-репозиторием проекта.
 * Предоставляет высокоуровневый API для UI-слоя.
 *
 * ВАЖНО: Все методы этого класса выполняют файловые и/или сетевые операции
 * и должны вызываться из фонового потока (например, с помощью корутин).
 *
 * @param projectDir Корневая папка проекта Catroid, где будет находиться .git.
 */
class GitController(private val projectDir: File) {

    private val projectMerger = ProjectMerger()

    val TAG = "GitController"

    /**
     * Инициализирует новый Git-репозиторий в папке проекта.
     * @return Успех или ошибка.
     */
    fun initializeRepository(): GitResult<Unit> = runCatching {
        Git.init().setDirectory(projectDir).call().close()
        GitResult.Success(Unit)
    }.getOrElse { GitResult.Error("Failed to initialize repository", it) }

    /**
     * Клонирует репозиторий в УКАЗАННУЮ папку.
     * @param remoteUrl URL репозитория.
     * @param authToken OAuth токен.
     * @param targetDir Папка, куда будет произведено клонирование.
     * @return Успех или ошибка.
     */
    fun cloneRepository(remoteUrl: String, authToken: String, targetDir: File): GitResult<Unit> =
        runCatching {
            Git.cloneRepository()
                .setURI(remoteUrl)
                .setDirectory(targetDir)
                .setCredentialsProvider(UsernamePasswordCredentialsProvider(authToken, ""))
                .call()
                .close()
            GitResult.Success(Unit)
        }.getOrElse {
            it.printStackTrace()
            GitResult.Error("Failed to clone repository: ${it.message}", it)
        }


    /**
     * Делает коммит и отправляет все изменения на сервер.
     * @param authToken OAuth токен пользователя.
     * @return Успех или ошибка.
     */
    fun pullAndMerge(authToken: String): GitResult<MergeResult> {
        Git.open(projectDir).use { git ->
            var stashRef: RevCommit? = null
            try {
                if (!git.repository.repositoryState.canCommit()) {
                    Log.w(TAG, "Repository was in a conflicting state. Resetting merge state...")
                    git.reset().setMode(ResetCommand.ResetType.MERGE).call()
                }

                var resultData: MergeResultData? = null
                if (!git.status().call().isClean) {
                    Log.d(TAG, "Uncommitted local changes detected. Stashing...")
                    stashRef = git.stashCreate().call()
                }

                Log.d(TAG, "Fetching from remote...")
                git.fetch().setCredentialsProvider(UsernamePasswordCredentialsProvider(authToken, "")).call()

                val remoteRef = git.repository.findRef("refs/remotes/origin/main")
                    ?: git.repository.findRef("refs/remotes/origin/master")
                    ?: throw Exception("Remote branch not found.")
                val localRef = git.repository.findRef("HEAD") ?: throw Exception("Local 'HEAD' not found.")

                if (remoteRef.objectId == localRef.objectId) {
                    Log.d(TAG, "Already up-to-date.")
                    if (stashRef != null) git.stashApply().call()
                    val project = XStreamUtilGit.fromXML(File(projectDir, "code.xml").readText(StandardCharsets.UTF_8))
                    return GitResult.Success(MergeResult(project, emptyList()))
                }

                val baseCommit = findMergeBase(git, localRef.objectId, remoteRef.objectId)
                    ?: throw Exception("Could not find common ancestor.")

                val hasLocalCommits = localRef.objectId != baseCommit
                val hasRemoteCommits = remoteRef.objectId != baseCommit

                var mergedProject: org.catrobat.catroid.content.Project

                if (!hasLocalCommits && hasRemoteCommits) {
                    Log.d(TAG, "Performing fast-forward update.")
                    git.merge().include(remoteRef).setFastForward(MergeCommand.FastForwardMode.FF_ONLY).call()
                    mergedProject = XStreamUtilGit.fromXML(File(projectDir, "code.xml").readText(StandardCharsets.UTF_8))

                    if (stashRef != null) {
                        Log.d(TAG, "Applying stashed changes on top of fast-forward...")
                        try {
                            git.stashApply().call()
                            mergedProject = XStreamUtilGit.fromXML(File(projectDir, "code.xml").readText(StandardCharsets.UTF_8))
                        } catch (e: Exception) {
                            throw Exception("Conflict applying stash on top of remote changes. Please resolve manually.", e)
                        }
                    }

                } else {
                    Log.d(TAG, "Diverged history. Performing 3-way semantic merge.")
                    val reader = git.repository.newObjectReader()
                    val baseXml = getFileContent(reader, baseCommit, "code.xml")
                    val localXml = getFileContent(reader, localRef.objectId, "code.xml")
                    val remoteXml = getFileContent(reader, remoteRef.objectId, "code.xml")

                    resultData = projectMerger.merge(
                        XStreamUtilGit.fromXML(baseXml),
                        XStreamUtilGit.fromXML(localXml),
                        XStreamUtilGit.fromXML(remoteXml)
                    )

                    mergedProject = resultData.mergedProject

                    val mergedXmlString = XStreamUtilGit.toXML(mergedProject)
                    Log.d("MERGE_RESULT_XML", "---- MERGED XML START ----")
                    mergedXmlString.chunked(4000).forEach { Log.d("MERGE_RESULT_XML", it) }
                    Log.d("MERGE_RESULT_XML", "---- MERGED XML END ----")

                    git.merge().include(remoteRef).setCommit(false).call()

                    Log.d(TAG, "Writing semantically merged code.xml to disk.")
                    File(projectDir, "code.xml").writeText(mergedXmlString, StandardCharsets.UTF_8)

                    git.add().addFilepattern("code.xml").call()
                    git.commit().setMessage("Semantic merge of remote branch").call()
                    Log.d(TAG, "Merge commit created successfully.")

                    if (stashRef != null) {
                        Log.d(TAG, "Dropping stash as it was included in the semantic merge.")
                        git.stashDrop().call()
                    }
                }

                return GitResult.Success(MergeResult(mergedProject, resultData?.conflicts ?: emptyList()))

            } catch (e: Exception) {
                Log.e(TAG, "Pull and merge failed", e)
                try {
                    git.reset().setMode(ResetCommand.ResetType.HARD).setRef("HEAD").call()
                } catch (resetError: Exception) {
                    Log.e(TAG, "Failed to reset after error", resetError)
                }
                if (stashRef != null) {
                    try {
                        git.stashApply().call()
                    } catch (applyError: Exception) {
                        Log.e(TAG, "Could not restore stashed changes after error.", applyError)
                    }
                }
                return if (e is MergeConflictException) {
                    Log.e(TAG, "Semantic merge resulted in ${e.conflicts.size} conflicts:")
                    e.conflicts.forEach { c -> Log.e(TAG, "  - ${c.fieldName}: '${c.baseValue}' vs ${c.localValue}' vs '${c.remoteValue}'") }
                    GitResult.MergeConflict(e.conflicts)
                } else {
                    GitResult.Error("Merge failed: ${e.message}", e)
                }
            }
        }
    }


    /**
     * Усиленная версия с дополнительным логированием для отладки.
     */
    fun commitAndPush(commitMessage: String, authorName: String, authorEmail: String, authToken: String): GitResult<Unit> = runCatching {
        Git.open(projectDir).use { git ->
            git.add().addFilepattern(".").call()

            val status = git.status().call()
            if (status.isClean && status.untracked.isEmpty()) {
                Log.d("GitController", "Nothing to commit, working tree clean.")
                return GitResult.Success(Unit)
            }

            val revCommit = git.commit()
                .setMessage(commitMessage)
                .setAuthor(authorName, authorEmail)
                .call()
            Log.d("GitController", "Created commit: ${revCommit.shortMessage}")

            Log.d("GitController", "Pushing to remote...")
            git.push()
                .setCredentialsProvider(UsernamePasswordCredentialsProvider(authToken, ""))
                .call()
            Log.d("GitController", "Push successful.")
        }
        GitResult.Success(Unit)
    }.getOrElse {
        it.printStackTrace()
        if (it.message?.contains("non-fast-forward") == true) {
            return GitResult.Error("Push rejected. Please pull the latest changes first.", it)
        }
        GitResult.Error("Failed to commit and push: ${it.message}", it)
    }

    private fun findMergeBase(git: Git, commit1: ObjectId, commit2: ObjectId): ObjectId? {
        RevWalk(git.repository).use { walk ->
            val rev1 = walk.parseCommit(commit1)
            val rev2 = walk.parseCommit(commit2)
            walk.revFilter = org.eclipse.jgit.revwalk.filter.RevFilter.MERGE_BASE
            walk.markStart(rev1)
            walk.markStart(rev2)
            return walk.next()
        }
    }

    private fun getFileContent(reader: ObjectReader, commitId: ObjectId, filePath: String): String {
        RevWalk(reader).use { walk ->
            val commit = walk.parseCommit(commitId)
            val tree = commit.tree
            TreeWalk.forPath(reader, filePath, tree)?.use { treeWalk ->
                val objectId = treeWalk.getObjectId(0)
                val loader = reader.open(objectId)
                return String(loader.bytes, StandardCharsets.UTF_8)
            }
        }
        return ""
    }

    /**
     * Создает новый репозиторий на GitHub, инициализирует локальный репозиторий,
     * делает первый коммит и отправляет его на сервер.
     */
    fun initializeAndPushNewRepository(authToken: String, repoName: String, isPrivate: Boolean): GitResult<String> = runCatching {
        val remoteRepoUrl = createGitHubRepository(authToken, repoName, isPrivate)
            ?: return@runCatching GitResult.Error("Failed to create GitHub repository")

        val git = Git.init().setDirectory(projectDir).call()

        git.use {
            it.add().addFilepattern(".").call()

            it.commit().setMessage("Initial commit").call()

            it.remoteAdd()
                .setName("origin")
                .setUri(URIish(remoteRepoUrl))
                .call()

            it.push()
                .setCredentialsProvider(UsernamePasswordCredentialsProvider(authToken, ""))
                .call()
        }
        GitResult.Success(remoteRepoUrl)
    }.getOrElse {
        it.printStackTrace()
        GitResult.Error("Failed to initialize and push repository", it) }


    private fun createGitHubRepository(token: String, repoName: String, isPrivate: Boolean): String? {
        val client = OkHttpClient()
        val json = JSONObject()
        json.put("name", repoName)
        json.put("private", isPrivate)

        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url("https://api.github.com/user/repos")
            .header("Authorization", "Bearer $token")
            .post(body)
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    System.err.println("GitHub API Error: ${response.code} ${response.body?.string()}")
                    return null
                }
                val responseBody = response.body?.string()
                responseBody?.let { JSONObject(it).optString("clone_url", null) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}