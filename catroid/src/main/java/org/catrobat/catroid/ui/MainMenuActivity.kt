/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2021 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.ui

import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.Html
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.text.HtmlCompat
import androidx.lifecycle.lifecycleScope
import com.danvexteam.lunoscript_annotations.LunoClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.catrobat.catroid.BuildConfig
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.R
import org.catrobat.catroid.cast.CastManager
import org.catrobat.catroid.common.Constants
import org.catrobat.catroid.common.FlavoredConstants
import org.catrobat.catroid.common.SharedPreferenceKeys
import org.catrobat.catroid.common.Survey
import org.catrobat.catroid.databinding.ActivityLoadingBinding
import org.catrobat.catroid.databinding.ActivityMainMenuBinding
import org.catrobat.catroid.databinding.ActivityMainMenuSplashscreenBinding
import org.catrobat.catroid.databinding.DeclinedTermsOfUseAndServiceAlertViewBinding
import org.catrobat.catroid.databinding.PrivacyPolicyViewBinding
import org.catrobat.catroid.databinding.ProgressBarBinding
import org.catrobat.catroid.editor.EditorActivity
import org.catrobat.catroid.io.ZipArchiver
import org.catrobat.catroid.io.asynctask.ProjectLoader
import org.catrobat.catroid.io.asynctask.ProjectLoader.ProjectLoadListener
import org.catrobat.catroid.io.asynctask.ProjectSaver
import org.catrobat.catroid.python.PythonEngine
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.ui.recyclerview.dialog.AboutDialogFragment
import org.catrobat.catroid.ui.recyclerview.fragment.MainMenuFragment
import org.catrobat.catroid.ui.settingsfragments.SettingsFragment
import org.catrobat.catroid.utils.FileMetaDataExtractor
import org.catrobat.catroid.utils.NativeLibraryManager
import org.catrobat.catroid.utils.ScreenValueHandler
import org.catrobat.catroid.utils.ToastUtil
import org.catrobat.catroid.utils.Utils
import org.catrobat.catroid.utils.setVisibleOrGone
import org.koin.android.ext.android.inject
import java.io.File
import java.io.IOException
import kotlin.random.Random


private const val SDK_VERSION = 24

@LunoClass
class MainMenuActivity : BaseCastActivity(), ProjectLoadListener {

    private lateinit var privacyPolicyBinding: PrivacyPolicyViewBinding
    private lateinit var declinedTermsOfUseViewBinding: DeclinedTermsOfUseAndServiceAlertViewBinding
    private lateinit var mainMenuBinding: ActivityMainMenuBinding
    private val projectManager: ProjectManager by inject()
    private var oldPrivacyPolicy = 0
    private lateinit var loadingBinding: ActivityLoadingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SettingsFragment.setToChosenLanguage(this)

        loadingBinding = ActivityLoadingBinding.inflate(layoutInflater)
        setContentView(loadingBinding.root)

        lifecycleScope.launch {
            val factJob = launch { showRandomFacts() }

            withContext(Dispatchers.IO) {
                heavyInitialization()
            }

            factJob.cancel()

            loadFinalContent()
        }

        /*PreferenceManager.setDefaultValues(this, R.xml.preferences, true)
        PreferenceManager.setDefaultValues(this, R.xml.nxt_preferences, true)
        PreferenceManager.setDefaultValues(this, R.xml.ev3_preferences, true)
        ScreenValueHandler.updateScreenWidthAndHeight(this)

        oldPrivacyPolicy = PreferenceManager.getDefaultSharedPreferences(this)
            .getInt(SharedPreferenceKeys.AGREED_TO_PRIVACY_POLICY_VERSION, 0)

        loadContent()

        if (oldPrivacyPolicy != Constants.CATROBAT_TERMS_OF_USE_ACCEPTED) {
            showTermsOfUseDialog()
        }

        surveyCampaign = Survey(this)
        surveyCampaign?.showSurvey(this)

        if (BuildConfig.FEATURE_APK_GENERATOR_ENABLED) {
            prepareStandaloneProject()
        }

        pythonEngine = PythonEngine(applicationContext)*/
    }

    private fun showRandomFacts() {
        val facts = resources.getStringArray(R.array.loading_facts)
        val randomFact = facts[Random.nextInt(facts.size)]
        loadingBinding.factTextView.text = randomFact
    }

    private fun heavyInitialization() {
        NativeLibraryManager.initialize()
        Thread.sleep(2000)
        PreferenceManager.setDefaultValues(this, R.xml.preferences, true)
        PreferenceManager.setDefaultValues(this, R.xml.nxt_preferences, true)
        PreferenceManager.setDefaultValues(this, R.xml.ev3_preferences, true)
        ScreenValueHandler.updateScreenWidthAndHeight(this)

        pythonEngine = PythonEngine(applicationContext)
    }

    private fun loadFinalContent() {
        if (BuildConfig.FEATURE_APK_GENERATOR_ENABLED) {
            val mainMenuSplashscreenBinding =
                ActivityMainMenuSplashscreenBinding.inflate(layoutInflater)
            setContentView(mainMenuSplashscreenBinding.root)
            oldPrivacyPolicy = PreferenceManager.getDefaultSharedPreferences(this)
                .getInt(SharedPreferenceKeys.AGREED_TO_PRIVACY_POLICY_VERSION, 0)
            if (oldPrivacyPolicy == Constants.CATROBAT_TERMS_OF_USE_ACCEPTED) {
                prepareStandaloneProject()
            }
        } else {
            mainMenuBinding = ActivityMainMenuBinding.inflate(layoutInflater)
            setContentView(mainMenuBinding.root)
            setSupportActionBar(findViewById(R.id.toolbar))
            supportActionBar?.setIcon(R.drawable.pc_toolbar_icon)
            supportActionBar?.setTitle(R.string.app_name)

            if (SettingsFragment.isCastSharedPreferenceEnabled(this)) {
                CastManager.getInstance().initializeCast(this)
            }
            loadFragment()
        }

        oldPrivacyPolicy = PreferenceManager.getDefaultSharedPreferences(this)
            .getInt(SharedPreferenceKeys.AGREED_TO_PRIVACY_POLICY_VERSION, 0)
        if (oldPrivacyPolicy != Constants.CATROBAT_TERMS_OF_USE_ACCEPTED) {
            showTermsOfUseDialog()
        }

        surveyCampaign = Survey(this)
        surveyCampaign?.showSurvey(this)
    }

    /*private fun testPython(): String {
        Log.d("MainMenuActivity", "Running DIAGNOSTIC Python script...")

        val script = """
    # Шаг 1: Проверяем, что Python видит наши пути
    import sys
    print("--- sys.path ---")
    for p in sys.path:
        print(p)
    print("----------------")
    
    # Шаг 2: Проверяем базовый импорт из стандартной библиотеки
    try:
        import os
        print("Successfully imported 'os' module")
        print(f"OS name: {os.name}")
    except Exception as e:
        print(f"FAILED to import 'os': {e}")

    # Шаг 3: Проверяем ГЛАВНОГО ПОДОЗРЕВАЕМОГО - модуль SSL
    try:
        import ssl
        print("Successfully imported 'ssl' module")
        print(f"SSL version: {ssl.OPENSSL_VERSION}")
    except Exception as e:
        print(f"FAILED to import 'ssl': {e}")
        # Выводим полный traceback ошибки, если она случилась здесь
        import traceback
        traceback.print_exc()

    print("--- Diagnostic script finished ---")
    """.trimIndent()

        return pythonEngine.runScript(script)
    }*/

    private fun showTermsOfUseDialog() {
        /*privacyPolicyBinding = PrivacyPolicyViewBinding.inflate(layoutInflater)
        val view = privacyPolicyBinding.root
        val termsOfUseUrlTextView = privacyPolicyBinding.dialogPrivacyPolicyTextViewUrl

        termsOfUseUrlTextView.movementMethod = LinkMovementMethod.getInstance()

        val termsOfUseUrlStringText = getString(R.string.main_menu_terms_of_use)
        val termsOfUseUrl = getString(
            R.string.terms_of_use_link_template,
            Constants.CATROBAT_TERMS_OF_USE_URL +
                Constants.CATROBAT_TERMS_OF_USE_TOKEN_FLAVOR_URL + BuildConfig.FLAVOR +
                Constants.CATROBAT_TERMS_OF_USE_TOKEN_VERSION_URL + BuildConfig.VERSION_CODE,
            termsOfUseUrlStringText
        )

        termsOfUseUrlTextView.text = if (Build.VERSION.SDK_INT >= SDK_VERSION) {
            Html.fromHtml(termsOfUseUrl, HtmlCompat.FROM_HTML_MODE_LEGACY)
        } else {
            Html.fromHtml(termsOfUseUrl)
        }

        AlertDialog.Builder(this)
            .setNegativeButton(R.string.decline) { _, _ -> handleDeclinedPrivacyPolicyButton() }
            .setPositiveButton(R.string.accept) { _, _ -> handleAgreedToPrivacyPolicyButton() }
            .setCancelable(false)
            .setOnKeyListener { _, keyCode: Int, _ ->
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    finish()
                    return@setOnKeyListener true
                }
                false
            }
            .setView(view)
            .show()*/
    }

    fun handleAgreedToPrivacyPolicyButton() {
        PreferenceManager.getDefaultSharedPreferences(this)
            .edit()
            .putInt(
                SharedPreferenceKeys.AGREED_TO_PRIVACY_POLICY_VERSION,
                Constants.CATROBAT_TERMS_OF_USE_ACCEPTED
            )
            .apply()
        if (BuildConfig.FEATURE_APK_GENERATOR_ENABLED) {
            prepareStandaloneProject()
        }
    }

    fun handleDeclinedPrivacyPolicyButton() {
        declinedTermsOfUseViewBinding =
            DeclinedTermsOfUseAndServiceAlertViewBinding.inflate(layoutInflater)
        val dialogView = declinedTermsOfUseViewBinding.root

        val linkString = getString(
            R.string.about_link_template,
            Constants.BASE_APP_URL_HTTPS,
            getString(R.string.share_website_text)
        )

        val linkTextView = declinedTermsOfUseViewBinding.shareWebsiteView
        linkTextView.movementMethod = LinkMovementMethod.getInstance()
        linkTextView.text = if (Build.VERSION.SDK_INT >= SDK_VERSION) {
            Html.fromHtml(linkString, HtmlCompat.FROM_HTML_MODE_LEGACY)
        } else {
            Html.fromHtml(linkString)
        }

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton(R.string.ok) { _, _ -> showTermsOfUseDialog() }
            .setCancelable(false)
            .setOnKeyListener { dialog: DialogInterface, keyCode: Int, _ ->
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    dialog.cancel()
                    showTermsOfUseDialog()
                    return@setOnKeyListener true
                }
                false
            }
            .show()
    }

    private fun loadContent() {
        if (BuildConfig.FEATURE_APK_GENERATOR_ENABLED) {
            val mainMenuSplashscreenBinding =
                ActivityMainMenuSplashscreenBinding.inflate(layoutInflater)
            setContentView(mainMenuSplashscreenBinding.root)
            if (oldPrivacyPolicy == Constants.CATROBAT_TERMS_OF_USE_ACCEPTED) {
                prepareStandaloneProject()
            }
            return
        }
        mainMenuBinding = ActivityMainMenuBinding.inflate(layoutInflater)
        setContentView(mainMenuBinding.root)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setIcon(R.drawable.pc_toolbar_icon)
        supportActionBar?.setTitle(R.string.app_name)

        setShowProgressBar(true)

        if (SettingsFragment.isCastSharedPreferenceEnabled(this)) {
            CastManager.getInstance().initializeCast(this)
        }
        loadFragment()
    }

    override fun onResume() {
        super.onResume()
        if (SettingsFragment.isCastSharedPreferenceEnabled(this)) {
            // А вот этот вызов ЗАПУСКАЕТ сканирование. И он единственный.
            CastManager.getInstance().addCallback()
        }
    }

    private fun loadFragment() {
        supportFragmentManager.beginTransaction()
            .replace(
                mainMenuBinding.fragmentContainer.id, MainMenuFragment(),
                MainMenuFragment.TAG
            )
            .commit()
        setShowProgressBar(false)

        val intent = intent
        if (intent.action != null && intent.action == "android.intent.action.VIEW" && intent.data != null) {
            val shareUri = intent.data
            val webIntent = Intent(this, WebViewActivity::class.java)
            webIntent.putExtra(WebViewActivity.INTENT_PARAMETER_URL, shareUri.toString())
            startActivity(webIntent)
        }
    }

    private fun setShowProgressBar(show: Boolean) {
        val progressBarBinding = ProgressBarBinding.inflate(layoutInflater)
        progressBarBinding.root.setVisibleOrGone(show)
        mainMenuBinding.fragmentContainer.setVisibleOrGone(!show)
    }

    public override fun onPause() {
        super.onPause()
        if (SettingsFragment.isCastSharedPreferenceEnabled(this)) {
            CastManager.getInstance().removeCallback();
        }
        val currentProject = projectManager.currentProject
        if (currentProject != null) {
            ProjectSaver(currentProject, applicationContext).saveProjectAsync()
            Utils.setLastUsedProjectName(applicationContext, currentProject.name)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main_menu, menu)
        //val scratchConverter = getString(R.string.main_menu_scratch_converter)
        //val scratchConverterBeta = SpannableString(
        //    scratchConverter + " " + getString(R.string.beta)
        //)

        //scratchConverterBeta.setSpan(
       //     ForegroundColorSpan(resources.getColor(R.color.beta_label_color, theme)),
        //    scratchConverter.length, scratchConverterBeta.length,
        //    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        //)
        //menu.findItem(R.id.menu_scratch_converter).title = scratchConverterBeta
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        //menu.findItem(R.id.menu_login).isVisible =
        //    !Utils.isUserLoggedIn(this)
        //menu.findItem(R.id.menu_logout).isVisible =
        //    Utils.isUserLoggedIn(this)
        if (!BuildConfig.FEATURE_SCRATCH_CONVERTER_ENABLED) {
            //menu.removeItem(R.id.menu_scratch_converter)
        }
        return true
    }

    private fun copyAssets(assetPath: String, destDir: File) {
        try {
            val assetManager = this.assets
            val assets = assetManager.list(assetPath)
            if (assets.isNullOrEmpty()) {
                // Если список пуст, это может быть пустая папка или файл.
                // Но наша логика вызывает рекурсию только для папок с содержимым,
                // поэтому сюда мы попадем только для пустых папок.
                // На всякий случай создадим ее.
                if (!destDir.exists()) {
                    destDir.mkdirs()
                }
                return
            }

            // Убедимся, что директория назначения существует
            if (!destDir.exists()) {
                destDir.mkdirs()
            }

            for (assetName in assets) {
                val sourcePath = if (assetPath.isEmpty()) assetName else "$assetPath/$assetName"
                val destFile = File(destDir, assetName)

                // ---> НОВАЯ, НАДЕЖНАЯ ЛОГИКА ПРОВЕРКИ <---
                // Если мы можем получить список дочерних элементов, это точно папка.
                val isDir = assetManager.list(sourcePath)?.isNotEmpty() == true

                if (isDir) {
                    // Если это папка, создаем ее и запускаем рекурсию
                    destFile.mkdirs()
                    copyAssets(sourcePath, destFile)
                } else {
                    // Если это файл, просто копируем его
                    assetManager.open(sourcePath).use { inputStream ->
                        java.io.FileOutputStream(destFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ловим любые исключения, чтобы увидеть, если что-то пойдет не так
            Log.e("PythonEngine", "FATAL ERROR in copyAssets for path: $assetPath", e)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_rate_app -> if (Utils.checkIsNetworkAvailableAndShowErrorMessage(this)) {
                try {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://www.rustore.ru/catalog/app/org.DanVexTeam.NewCatroid")
                        )
                    )
                } catch (e: ActivityNotFoundException) {
                    Log.e(TAG, "onOptionsItemSelected: ", e)
                    ToastUtil.showError(this, R.string.main_menu_play_store_not_installed)
                }
            }
            /*R.id.menu_terms_of_use -> TermsOfUseDialogFragment().show(
                supportFragmentManager,
                TermsOfUseDialogFragment.TAG
            )*/
            R.id.menu_privacy_policy -> {
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(FlavoredConstants.PRIVACY_POLICY_URL)
                )
                startActivity(browserIntent)
                /*Toast.makeText(this, "Starting Python script...", Toast.LENGTH_SHORT).show()
                Thread {
                    // Внутри фонового потока Thread { ... }

                    try {
                        // Определяем папку нашего тестового проекта
                        val projectDir = File(applicationContext.filesDir, "projects/MyNumpyProject")
                        projectDir.mkdirs()

                        // Копируем библиотеки для этого проекта (эмуляция "установки")
                        copyAssets("numpy_test_pylibs", File(projectDir, "pylibs"))
                        copyAssets("numpy_test_pylibs_native", File(projectDir, "pylibs_native"))

                        // Инициализируем Python именно для этого проекта
                        pythonEngine.initialize(projectDir)

                        // Запускаем тестовый скрипт
                        val output = testPython() // Ваш тестовый скрипт для NumPy
                        Log.d("PythonThread", "--- NUMPY OUTPUT ---")
                        Log.d("PythonThread", output)
                        Log.d("PythonThread", "--------------------")

                    } catch (e: Exception) {
                        Log.e("PythonThread", "Error in python thread", e)
                    }
                }.start()*/
            }
            R.id.menu_about -> AboutDialogFragment().show(
                supportFragmentManager,
                AboutDialogFragment.TAG
            )
            //R.id.menu_scratch_converter -> if (Utils.checkIsNetworkAvailableAndShowErrorMessage(this)) {
            //    startActivity(Intent(this, ScratchConverterActivity::class.java))
            //}
            R.id.settings -> startActivity(Intent(this, SettingsActivity::class.java))
            //R.id.menu_login -> startActivity(Intent(this, SignInActivity::class.java))
            //R.id.menu_logout -> {
            //    Utils.logoutUser(this)
             //   ToastUtil.showSuccess(this, R.string.logout_successful)
            //}
            //R.id.menu_help -> startActivity(
            //    Intent(
            //        Intent.ACTION_VIEW,
            //        Uri.parse(CATROBAT_HELP_URL)
            //    )
            //)
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun prepareStandaloneProject() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        // Вместо флага "first_run" будем хранить версию приложения,
        // при которой была последняя распаковка.
        val lastUnpackedVersion = prefs.getInt("standalone_project_version", -1)
        val currentVersion = BuildConfig.VERSION_CODE

        // Если текущая версия приложения > сохраненной, нужна перераспаковка.
        if (lastUnpackedVersion >= currentVersion) {
            Log.d("STANDALONE", "Project version ($lastUnpackedVersion) is up to date. Loading from storage.")
            val projectDir = File(
                FlavoredConstants.DEFAULT_ROOT_DIRECTORY,
                FileMetaDataExtractor.encodeSpecialCharsForFileSystem(BuildConfig.PROJECT_NAME)
            )
            ProjectLoader(projectDir, this)
                .setListener(this)
                .loadProjectAsync()
            return
        }

        try {
            Log.d("STANDALONE", "New version detected (current: $currentVersion, last: $lastUnpackedVersion). Unpacking project from assets...")
            val projectDir = File(
                FlavoredConstants.DEFAULT_ROOT_DIRECTORY,
                FileMetaDataExtractor.encodeSpecialCharsForFileSystem(BuildConfig.PROJECT_NAME)
            )

            // Перед распаковкой на всякий случай удалим старую папку
            if (projectDir.exists()) {
                projectDir.deleteRecursively()
            }

            val inputStream = assets.open(BuildConfig.START_PROJECT + ".zip")
            ZipArchiver().unzip(inputStream, projectDir)

            ProjectLoader(projectDir, this)
                .setListener(this)
                .loadProjectAsync()

            // После успешной распаковки сохраняем НОВУЮ версию.
            prefs.edit().putInt("standalone_project_version", currentVersion).apply()
            Log.d("STANDALONE", "Unpack complete. Saved new version: $currentVersion")

        } catch (e: IOException) {
            Log.e("STANDALONE", "Cannot unpack standalone project: ", e)
        }
    }

    override fun onLoadFinished(success: Boolean) {
        if (BuildConfig.FEATURE_APK_GENERATOR_ENABLED && success) {
            startActivityForResult(
                Intent(this, StageActivity::class.java), StageActivity.REQUEST_START_STAGE
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (BuildConfig.FEATURE_APK_GENERATOR_ENABLED) {
            if (requestCode == StageActivity.REQUEST_START_STAGE) {
                finish()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    companion object {
        val TAG = MainMenuActivity::class.java.simpleName

        lateinit var pythonEngine: PythonEngine

        @JvmField
        var surveyCampaign: Survey? = null

        fun toast(text: String, duration: Int) {
            val toast = Toast(CatroidApplication.getAppContext())
            toast.setText(text)
            toast.duration = duration
            toast.show()
        }

        fun getCpuArchitecture(): String {
            // Build.SUPPORTED_ABIS возвращает массив типа ["arm64-v8a", "armeabi-v7a", "armeabi"]
            // Первый элемент - это основная архитектура.
            val abis = Build.SUPPORTED_ABIS

            return if (abis != null && abis.isNotEmpty()) {
                abis[0]
            } else {
                // Очень старый и редкий случай, когда SUPPORTED_ABIS недоступен.
                // Можно использовать устаревшее свойство как запасной вариант.
                @Suppress("DEPRECATION")
                Build.CPU_ABI
            }
        }
    }
}
