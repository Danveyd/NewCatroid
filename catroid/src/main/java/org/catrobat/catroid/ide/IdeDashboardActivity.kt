package org.catrobat.catroid.ide

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.catrobat.catroid.R
import org.catrobat.catroid.ide.DownloadStatus
import org.catrobat.catroid.utils.git.GitHubActionsApi
import org.catrobat.catroid.utils.git.GitController
import org.catrobat.catroid.utils.git.TokenManager
import org.json.JSONObject
import retrofit2.Retrofit
import java.io.File

class IdeDashboardActivity : AppCompatActivity() {

    private lateinit var loadingOverlay: LinearLayout
    private lateinit var tvLoadingText: TextView
    private lateinit var projectsList: ListView

    private val TARGET_SDK = 33
    private val UPSTREAM_OWNER = "Danveyd"
    private val UPSTREAM_REPO = "NewCatroid"

    // Папка, где будут лежать клонированные форки
    private val workspaceDir by lazy { File(filesDir, "IdeProjects").apply { mkdirs() } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ide_dashboard)

        loadingOverlay = findViewById(R.id.loading_overlay)
        tvLoadingText = findViewById(R.id.tv_loading_text)
        projectsList = findViewById(R.id.projects_list)

        setupButtons()

        // 1. При заходе сразу проверяем SDK
        checkAndInstallSdk()
    }

    private fun checkAndInstallSdk() {
        val sdkJar = IdeSettings.getAndroidJar(this, TARGET_SDK)
        if (!sdkJar.exists()) {
            showLoading(getString(R.string.ide_dash_preparing_env, TARGET_SDK))
            lifecycleScope.launch(Dispatchers.IO) {
                val success = SdkManager.downloadPlatform(this@IdeDashboardActivity, TARGET_SDK) { msg, status ->
                    runOnUiThread {
                        tvLoadingText.text = msg
                        if (status is DownloadStatus.Error) {
                            Toast.makeText(this@IdeDashboardActivity, getString(R.string.ide_dash_sdk_error, status.message), Toast.LENGTH_LONG).show()
                            hideLoading()
                        }
                    }
                }

                runOnUiThread {
                    hideLoading()
                    if (success) {
                        Toast.makeText(this@IdeDashboardActivity, getString(R.string.ide_dash_sdk_installed), Toast.LENGTH_SHORT).show()
                        refreshProjectsList()
                    }
                }
            }
        } else {
            // SDK уже есть, просто грузим список проектов
            refreshProjectsList()
        }
    }

    private fun setupButtons() {
        findViewById<Button>(R.id.btn_create_fork).setOnClickListener {
            val token = TokenManager.getToken(this)
            if (token == null) {
                Toast.makeText(this, getString(R.string.ide_dash_github_login_first), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            createForkAndClone(token)
        }

        findViewById<Button>(R.id.btn_clone_existing).setOnClickListener {
            Toast.makeText(this, getString(R.string.ide_dash_coming_soon), Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshProjectsList() {
        val projects = workspaceDir.listFiles()?.filter { it.isDirectory }?.map { it.name } ?: emptyList()

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, projects)
        projectsList.adapter = adapter

        projectsList.setOnItemClickListener { _, _, position, _ ->
            val projectName = projects[position]
            openIde(projectName)
        }

        projectsList.setOnItemLongClickListener { _, _, position, _ ->
            val projectName = projects[position]
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.ide_dash_delete_feature_title))
                .setMessage(getString(R.string.ide_dash_delete_feature_msg, projectName))
                .setPositiveButton(getString(R.string.delete)) { _, _ ->
                    File(workspaceDir, projectName).deleteRecursively()
                    Toast.makeText(this, getString(R.string.ide_dash_project_deleted), Toast.LENGTH_SHORT).show()
                    refreshProjectsList()
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
            true
        }
    }
    private fun createForkAndClone(token: String) {
        showLoading(getString(R.string.ide_dash_creating_fork))

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. Делаем API запрос на создание форка
                val retrofit = Retrofit.Builder().baseUrl("https://api.github.com/").build()
                val api = retrofit.create(GitHubActionsApi::class.java)

                val response = api.createFork("Bearer $token", owner = UPSTREAM_OWNER, repo = UPSTREAM_REPO)

                if (!response.isSuccessful && response.code() != 202) {
                    throw Exception(getString(R.string.ide_dash_github_api_error, response.code()))
                }

                val responseBody = response.body()?.string() ?: response.errorBody()?.string() ?: ""
                val cloneUrl = if (responseBody.isNotEmpty()) {
                    JSONObject(responseBody).optString("clone_url")
                } else {
                    throw Exception(getString(R.string.ide_dash_fork_link_error))
                }

                runOnUiThread { tvLoadingText.text = getString(R.string.ide_dash_cloning_source) }
                kotlinx.coroutines.delay(3000)

                // Генерируем уникальное имя для ветки и папки
                val featureId = System.currentTimeMillis().toString().takeLast(5)
                val projectName = "Feature_$featureId"
                val branchName = "feature-$featureId" // Имя новой ветки!

                val targetDir = File(workspaceDir, projectName)
                val gitController = GitController(targetDir)
                val result = gitController.downloadAndUnpack(cloneUrl, token, targetDir)

                withContext(Dispatchers.Main) {
                    if (result is org.catrobat.catroid.utils.git.GitResult.Success) {
                        hideLoading()
                        Toast.makeText(this@IdeDashboardActivity, getString(R.string.ide_dash_project_ready), Toast.LENGTH_SHORT).show()
                        refreshProjectsList()
                        openIde(projectName)
                    } else {
                        targetDir.deleteRecursively()
                        hideLoading()
                        tvLoadingText.text = getString(R.string.ide_dash_error)
                        Toast.makeText(this@IdeDashboardActivity, "Error", Toast.LENGTH_LONG).show()
                        refreshProjectsList()
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    hideLoading()
                    Toast.makeText(this@IdeDashboardActivity, getString(R.string.common_error_with_message, e.message), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun openIde(projectName: String) {
        val intent = Intent(this, IdeActivity::class.java)
        intent.putExtra("PROJECT_NAME", projectName)
        startActivity(intent)
    }

    private fun showLoading(text: String) {
        tvLoadingText.text = text
        loadingOverlay.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        loadingOverlay.visibility = View.GONE
    }
}
