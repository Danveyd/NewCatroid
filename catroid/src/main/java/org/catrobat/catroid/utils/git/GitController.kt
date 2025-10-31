package org.catrobat.catroid.utils.git

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.catrobat.catroid.content.Project
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.MergeCommand
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.lib.CommitBuilder
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.api.MergeResult as JGitMergeResult
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.ObjectReader
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.RefUpdate
import org.eclipse.jgit.lib.RepositoryState
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.RemoteRefUpdate
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.eclipse.jgit.treewalk.TreeWalk
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets

//Ov23liKoq3h0cTgAbVYA
//936da4332f8a31ebed1bc97aa5d2f89a989a56d2

class GitController(private val projectDir: File) {

    private val projectMerger = ProjectMerger()

    val TAG = "GitController"

    fun initializeRepository(): GitResult<Unit> = runCatching {
        Git.init().setDirectory(projectDir).call().close()
        GitResult.Success(Unit)
    }.getOrElse { GitResult.Error("Failed to initialize repository", it) }


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


    /*fun pullAndMerge(authToken: String): GitResult<MergeResult> {
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
    }*/

    /*
    val currentState = git.repository.repositoryState
                if (currentState == RepositoryState.MERGING || currentState == RepositoryState.MERGING_RESOLVED) {
                    Log.w(TAG, "Repository was in a stale merging state ($currentState). Resetting merge state before proceeding.")
                    // Отменяем "зависшее" слияние
                    git.reset().setMode(ResetCommand.ResetType.MERGE).call()
                }
     */

    fun pullAndMerge(authToken: String): GitResult<MergeResult> {
        try {
            Git.open(projectDir).use { git ->
                val currentState = git.repository.repositoryState
                if (currentState == RepositoryState.MERGING || currentState == RepositoryState.MERGING_RESOLVED) {
                    Log.w(TAG, "Repository was in a stale merging state ($currentState). Resetting merge state before proceeding.")
                    git.reset().setMode(ResetCommand.ResetType.MERGE).call()
                }

                val status = git.status().call()
                if (!status.isClean) {
                    Log.d(TAG, "Uncommitted local changes detected. Creating a temporary local commit.")
                    git.add().addFilepattern(".").call()
                    git.commit().setMessage("Local work-in-progress").call()
                }

                val localRefBeforePull = git.repository.findRef("HEAD") ?: return GitResult.Error("Local HEAD not found")

                Log.d(TAG, "Fetching from remote...")
                git.fetch().setCredentialsProvider(UsernamePasswordCredentialsProvider(authToken, "")).call()

                val remoteRef = git.repository.findRef("refs/remotes/origin/main")
                    ?: git.repository.findRef("refs/remotes/origin/master")
                    ?: return GitResult.Error("Remote branch not found")

                if (localRefBeforePull.objectId == remoteRef.objectId) {
                    Log.d(TAG, "Already up-to-date.")
                    val project = XStreamUtilGit.fromXML(File(projectDir, "code.xml").readText(StandardCharsets.UTF_8))
                    return GitResult.Success(MergeResult(project, emptyList()))
                }

                val mergeResult = git.merge().include(remoteRef).call()

                if (mergeResult.mergeStatus.isSuccessful) {
                    Log.d(TAG, "Standard merge successful with status: ${mergeResult.mergeStatus}")
                    val project = XStreamUtilGit.fromXML(File(projectDir, "code.xml").readText(StandardCharsets.UTF_8))
                    return GitResult.Success(MergeResult(project, emptyList()))
                }

                Log.w(TAG, "Git merge resulted in conflicts. Starting semantic merge as conflict resolution.")
                git.reset().setMode(ResetCommand.ResetType.HARD).setRef(localRefBeforePull.name).call()

                val baseCommit = findMergeBase(git, localRefBeforePull.objectId, remoteRef.objectId) ?: return GitResult.Error("No common ancestor found")
                val reader = git.repository.newObjectReader()
                val baseXml = getFileContent(reader, baseCommit, "code.xml")
                val localXml = getFileContent(reader, localRefBeforePull.objectId, "code.xml")
                val remoteXml = getFileContent(reader, remoteRef.objectId, "code.xml")

                val semanticResult = projectMerger.merge(
                    XStreamUtilGit.fromXML(baseXml),
                    XStreamUtilGit.fromXML(localXml),
                    XStreamUtilGit.fromXML(remoteXml)
                )

                File(projectDir, "code.xml").writeText(XStreamUtilGit.toXML(semanticResult.mergedProject), StandardCharsets.UTF_8)
                git.add().addFilepattern("code.xml").call()
                git.commit().setMessage("Semantic merge of remote changes").call()

                Log.d(TAG, "Semantic merge complete. Merge commit created.")
                return GitResult.Success(MergeResult(semanticResult.mergedProject, semanticResult.conflicts))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Pull and merge failed", e)
            try {
                Git.open(projectDir).use { git ->
                    git.reset().setMode(ResetCommand.ResetType.HARD).call()
                }
            } catch (resetEx: Exception) {
                Log.e(TAG, "Hard reset also failed", resetEx)
            }
            return GitResult.Error("Merge failed: ${e.message}", e)
        }
    }


    /*fun commitAndPush(commitMessage: String, authorName: String, authorEmail: String, authToken: String): GitResult<Unit> = runCatching {
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
    }*/

    fun commitAndPush(commitMessage: String, authorName: String, authorEmail: String, authToken: String): GitResult<Unit> {
        try {
            Git.open(projectDir).use { git ->
                if (!git.status().call().isClean) {
                    Log.d(TAG, "Uncommitted changes found. Committing local work.")
                    git.add().addFilepattern(".").call()
                    git.commit().setMessage(commitMessage).setAuthor(authorName, authorEmail).call()
                } else {
                    Log.d(TAG, "Working tree is clean. Nothing new to commit locally.")
                }
            }

            var needsMerge = false
            var mergeResult: JGitMergeResult? = null

            Git.open(projectDir).use { git ->
                Log.d(TAG, "Fetching from remote repository...")
                git.fetch().setCredentialsProvider(UsernamePasswordCredentialsProvider(authToken, "")).call()

                val localCommit = git.repository.resolve(Constants.HEAD)
                val remoteCommit = git.repository.resolve("refs/remotes/origin/main") ?: git.repository.resolve("refs/remotes/origin/master")
                ?: return GitResult.Error("Remote branch not found after fetch.")

                if (localCommit != remoteCommit) {
                    Log.d(TAG, "Local and remote history has diverged. Starting merge process...")
                    needsMerge = true
                    mergeResult = git.merge().include(remoteCommit).setCommit(false).call()
                } else {
                    Log.d(TAG, "Local history is up-to-date. No merge needed.")
                }
            }

            if (needsMerge) {
                val result = mergeResult ?: throw IllegalStateException("Merge was needed but mergeResult is null.")

                if (result.mergeStatus.isSuccessful) {
                    Git.open(projectDir).use { git ->
                        Log.d(TAG, "Standard merge successful. Committing.")
                        git.commit().setMessage("Merge remote changes").call()
                    }
                } else if (result.mergeStatus == JGitMergeResult.MergeStatus.CONFLICTING) {
                    Log.w(TAG, "Standard Git merge resulted in conflicts. Resolving...")

                    val conflicts = result.conflicts ?: throw IllegalStateException("Conflict status but no conflicts map.")
                    if (conflicts.containsKey("code.xml")) {
                        Log.d(TAG, "Resolving 'code.xml' semantically...")
                        Git.open(projectDir).use { readOnlyGit ->
                            val repo = readOnlyGit.repository
                            val reader = repo.newObjectReader()
                            val localHead = repo.resolve(Constants.HEAD)
                            val remoteHead = repo.resolve(Constants.MERGE_HEAD) ?: throw IllegalStateException("MERGE_HEAD not found")
                            val baseCommit = findMergeBase(readOnlyGit, localHead, remoteHead) ?: return GitResult.Error("No common ancestor.")

                            val baseXml = getFileContent(reader, baseCommit, "code.xml")
                            val localXml = getFileContent(reader, localHead, "code.xml")
                            val remoteXml = getFileContent(reader, remoteHead, "code.xml")
                            val semanticResult = projectMerger.merge(
                                XStreamUtilGit.fromXML(baseXml),
                                XStreamUtilGit.fromXML(localXml),
                                XStreamUtilGit.fromXML(remoteXml)
                            )
                            File(projectDir, "code.xml").writeText(XStreamUtilGit.toXML(semanticResult.mergedProject), StandardCharsets.UTF_8)
                        }
                    }

                    Git.open(projectDir).use { gitForResolution ->
                        Log.d(TAG, "Adding resolved files to index...")
                        val addCommand = gitForResolution.add()
                        conflicts.keys.forEach { path -> addCommand.addFilepattern(path) }
                        addCommand.call()

                        Log.d(TAG, "All conflicts resolved. Creating merge commit...")
                        val repo = gitForResolution.repository
                        val inserter = repo.newObjectInserter()
                        val newCommitId: ObjectId
                        try {
                            val index = repo.lockDirCache()
                            val treeId = try { index.writeTree(inserter) } finally { index.unlock() }

                            val headId = repo.resolve(Constants.HEAD)
                            val mergeHeadId = repo.resolve(Constants.MERGE_HEAD) ?: throw IllegalStateException("MERGE_HEAD not found.")

                            val commitBuilder = CommitBuilder()
                            commitBuilder.setTreeId(treeId)
                            commitBuilder.setParentIds(headId, mergeHeadId)
                            commitBuilder.author = PersonIdent(authorName, authorEmail)
                            commitBuilder.committer = PersonIdent(authorName, authorEmail)
                            commitBuilder.message = "Semantic merge of remote changes"
                            newCommitId = inserter.insert(commitBuilder)
                            inserter.flush()

                            val headUpdate = repo.updateRef(Constants.HEAD)
                            headUpdate.setNewObjectId(newCommitId)
                            headUpdate.setExpectedOldObjectId(headId)
                            val updateResult = headUpdate.update()
                            if (updateResult != RefUpdate.Result.FAST_FORWARD && updateResult != RefUpdate.Result.NEW) {
                                throw java.io.IOException("Failed to update HEAD. Result: $updateResult")
                            }
                            repo.writeMergeCommitMsg(null)
                            repo.writeMergeHeads(null)
                        } finally {
                            inserter.close()
                        }
                        Log.d(TAG, "Merge commit created: ${newCommitId.name()}")
                    }
                } else {
                    throw Exception("Merge failed with unexpected status: ${result.mergeStatus}")
                }
            }

            Git.open(projectDir).use { git ->
                Log.d(TAG, "Attempting to push merged history to remote...")
                git.push().setCredentialsProvider(UsernamePasswordCredentialsProvider(authToken, "")).call()
                Log.d(TAG, "Push successful.")
            }

            return GitResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "The entire commit-and-push process failed", e)
            try {
                Git.open(projectDir).use { git ->
                    Log.w(TAG, "Attempting to hard reset HEAD due to a critical failure.")
                    val lockFile = File(git.repository.directory, "index.lock")
                    if (lockFile.exists()) {
                        Log.w(TAG, "Deleting stale index.lock file.")
                        lockFile.delete()
                    }
                    git.reset().setMode(ResetCommand.ResetType.HARD).call()
                }
            } catch (resetEx: Exception) {
                Log.e(TAG, "Failed to reset repository after a critical error", resetEx)
            }
            return GitResult.Error("Publication failed: ${e.message}", e)
        }
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