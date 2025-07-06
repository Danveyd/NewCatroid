/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
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
package org.catrobat.catroid.ui.fragment

import android.Manifest.permission
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.text.Editable
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.R
import org.catrobat.catroid.common.Constants
import org.catrobat.catroid.common.Nameable
import org.catrobat.catroid.common.ProjectData
import org.catrobat.catroid.common.ScreenModes
import org.catrobat.catroid.content.Project
import org.catrobat.catroid.databinding.FragmentProjectOptionsBinding
import org.catrobat.catroid.io.StorageOperations
import org.catrobat.catroid.io.XstreamSerializer
import org.catrobat.catroid.io.asynctask.ProjectExportTask
import org.catrobat.catroid.io.asynctask.loadProject
import org.catrobat.catroid.io.asynctask.ProjectSaver
import org.catrobat.catroid.io.asynctask.renameProject
import org.catrobat.catroid.io.asynctask.saveProjectSerial
import org.catrobat.catroid.merge.NewProjectNameTextWatcher
import org.catrobat.catroid.ui.BottomBar.hideBottomBar
import org.catrobat.catroid.ui.PROJECT_DIR
import org.catrobat.catroid.ui.ProjectUploadActivity
import org.catrobat.catroid.ui.runtimepermissions.RequiresPermissionTask
import org.catrobat.catroid.utils.ToastUtil
import org.catrobat.catroid.utils.Utils
import org.catrobat.catroid.utils.notifications.StatusBarNotificationManager
import org.koin.android.ext.android.inject
import java.io.File
import java.io.IOException
import android.content.Context
import android.view.ContextThemeWrapper
import org.catrobat.catroid.BuildConfig
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.ContextWrapper
import android.preference.PreferenceManager
import android.text.Html
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ForegroundColorSpan
import android.view.KeyEvent
import android.view.MenuItem
import androidx.core.text.HtmlCompat
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.cast.CastManager
import org.catrobat.catroid.common.FlavoredConstants
import org.catrobat.catroid.common.FlavoredConstants.CATROBAT_HELP_URL
import org.catrobat.catroid.common.SharedPreferenceKeys
import org.catrobat.catroid.common.Survey
import org.catrobat.catroid.databinding.ActivityMainMenuBinding
import org.catrobat.catroid.databinding.ActivityMainMenuSplashscreenBinding
import org.catrobat.catroid.databinding.DeclinedTermsOfUseAndServiceAlertViewBinding
import org.catrobat.catroid.databinding.PrivacyPolicyViewBinding
import org.catrobat.catroid.databinding.ProgressBarBinding
import org.catrobat.catroid.io.ZipArchiver
import org.catrobat.catroid.io.asynctask.ProjectLoader
import org.catrobat.catroid.io.asynctask.ProjectLoader.ProjectLoadListener
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.ui.dialogs.TermsOfUseDialogFragment
import org.catrobat.catroid.ui.recyclerview.dialog.AboutDialogFragment
import org.catrobat.catroid.ui.recyclerview.fragment.MainMenuFragment
import org.catrobat.catroid.ui.settingsfragments.SettingsFragment
import org.catrobat.catroid.utils.FileMetaDataExtractor
import org.catrobat.catroid.utils.ScreenValueHandler
import org.catrobat.catroid.utils.setVisibleOrGone
import org.koin.android.ext.android.inject
import java.io.OutputStream
import com.android.apksig.ApkSigner
import com.android.apksig.internal.x509.Certificate
import org.catrobat.catroid.content.XmlHeader
import org.catrobat.catroid.utils.ErrorLog
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.ArrayList
import kotlin.random.Random

class ProjectOptionsFragment : Fragment() {

    private val projectManager: ProjectManager by inject()
    private var _binding: FragmentProjectOptionsBinding? = null
    private val binding get() = _binding!!
    private var project: Project? = null
    private var sceneName: String? = null
    private var projectInZip: File? = null
    private var buildFilename: String? = null
    private var zipTempDir: File? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProjectOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        (requireActivity() as AppCompatActivity).supportActionBar?.setTitle(R.string.project_options)

        project = projectManager.currentProject
        sceneName = projectManager.currentlyEditedScene.name

        setupNameInputLayout()
        setupPhysicsInputLayout()
        setupDescriptionInputLayout()
        setupNotesAndCreditsInputLayout()
        addTags()
        setupProjectAspectRatio()
        setupCustomResolution()
        setupProjectUpload()
        setupProjectSaveExternal()
        //setupProjectSaveApk()
        setupClearVars()
        setupChangeIcon()
        setupChangeOrientation()
        setupProjectMoreDetails()
        setupProjectOptionDelete()
        setupMishkFrede()

        hideBottomBar(requireActivity())
    }

    private fun setupMishkFrede() {
        /*binding.projectOptionsMishkFrede.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Мы подозреваем, что вы не кушаете огурцы")
                .setMessage("Нам нужно удостоверится в этом, ответьте на вопрос. Едите ли вы огурцы?")
                .setPositiveButton("ДА") { _: DialogInterface?, _: Int ->
                    AlertDialog.Builder(requireContext())
                        .setTitle("Мы так и знали! Вы - Фуфулшмерц")
                        .setMessage("Грейпфрут")
                        .setPositiveButton("Отмена") { _: DialogInterface?, _: Int ->
                            AlertDialog.Builder(requireContext())
                                .setTitle("Черепица")
                                .setPositiveButton("Лом") { _: DialogInterface?, _: Int ->
                                    AlertDialog.Builder(requireContext())
                                        .setTitle("Громоотвод")
                                        .setPositiveButton("Гора") { _: DialogInterface?, _: Int ->
                                            AlertDialog.Builder(requireContext())
                                                .setTitle("Угол")
                                                .setPositiveButton("Ъ") { _: DialogInterface?, _: Int ->
                                                    AlertDialog.Builder(requireContext())
                                                        .setTitle("Я щас проект удалю")
                                                        .setPositiveButton("Нинада") { _: DialogInterface?, _: Int ->
                                                            AlertDialog.Builder(requireContext())
                                                                .setTitle("Ок")
                                                                .setPositiveButton("Ок") { _: DialogInterface?, _: Int ->

                                                                }
                                                                .setCancelable(false)
                                                                .show()
                                                        }
                                                        .setCancelable(false)
                                                        .show()
                                                }
                                                .setCancelable(false)
                                                .show()
                                        }
                                        .setCancelable(false)
                                        .show()
                                }
                                .setCancelable(false)
                                .show()
                        }
                        .setNegativeButton("Ок", null)
                        .setCancelable(false)
                        .show()
                }
                .setNegativeButton("Нет") { _: DialogInterface?, _: Int ->
                    AlertDialog.Builder(requireContext())
                        .setTitle("Сейчас к вам залезет Андрей через окно")
                        .setMessage("не бойтесь, он проверит огурцы в холодильнике")
                        .setPositiveButton("...") { _: DialogInterface?, _: Int ->
                            AlertDialog.Builder(requireContext())
                                .setTitle("...")
                                .setPositiveButton("...") { _: DialogInterface?, _: Int ->
                                    AlertDialog.Builder(requireContext())
                                        .setTitle(".")
                                        .setPositiveButton("...") { _: DialogInterface?, _: Int ->
                                            AlertDialog.Builder(requireContext())
                                                .setTitle("Ладно, Андрей уснул, мы вам поверим наслово")
                                                .setPositiveButton("Ок") { _: DialogInterface?, _: Int ->

                                                }
                                                .setCancelable(false)
                                                .show()
                                        }
                                        .setCancelable(false)
                                        .show()
                                }
                                .setCancelable(false)
                                .show()
                        }
                        .setCancelable(false)
                        .show()
                }
                .setCancelable(false)
                .show()
        }*/
    }

    private fun setupNameInputLayout() {
        binding.projectOptionsNameLayout.editText?.apply {
            setText(project?.name)
            addTextChangedListener(object : NewProjectNameTextWatcher<Nameable>() {
                override fun afterTextChanged(s: Editable?) {
                    val error = if (s.toString() != project!!.name) {
                        validateInput(s.toString(), getContext())
                    } else {
                        null
                    }
                    binding.projectOptionsNameLayout.error = error
                }
            })
        }
    }

    private fun setupPhysicsInputLayout() {
        val xml: XmlHeader? = project?.xmlHeader
        binding.projectOptionsPhysicsWidthLayout.editText?.apply {
            setText(xml?.getPhysicsWidthArea().toString())
            addTextChangedListener(object : NewProjectNameTextWatcher<Nameable>() {
                override fun afterTextChanged(s: Editable?) {
                    val error = if (s.toString() != project!!.name) {
                        validatePhysicsInput(s.toString(), getContext())
                    } else {
                        null
                    }
                    binding.projectOptionsPhysicsWidthLayout.error = error
                }
            })
        }
        binding.projectOptionsPhysicsHeightLayout.editText?.apply {
            setText(xml?.getPhysicsHeightArea().toString())
            addTextChangedListener(object : NewProjectNameTextWatcher<Nameable>() {
                override fun afterTextChanged(s: Editable?) {
                    val error = if (s.toString() != project!!.name) {
                        validatePhysicsInput(s.toString(), getContext())
                    } else {
                        null
                    }
                    binding.projectOptionsPhysicsHeightLayout.error = error
                }
            })
        }
    }

    private fun setupDescriptionInputLayout() {
        binding.projectOptionsDescriptionLayout.editText?.setText(project?.description)
    }

    private fun setupNotesAndCreditsInputLayout() {
        binding.projectOptionsNotesAndCreditsLayout.editText?.setText(project?.notesAndCredits)
    }

    private fun addTags() {
        binding.chipGroupTags.removeAllViews()
        val tags = project!!.tags

        if (tags.size == 1 && tags[0].isEmpty()) {
            binding.tags.visibility = View.GONE
            return
        }
        binding.tags.visibility = View.VISIBLE
        for (tag in tags) {
            val chip = Chip(context)
            chip.text = tag
            chip.isClickable = false
            binding.chipGroupTags.addView(chip)
        }
    }

    private fun setupProjectAspectRatio() {
        binding.projectOptionsAspectRatio.apply {
            isChecked = project?.screenMode == ScreenModes.MAXIMIZE
            setOnCheckedChangeListener { _, isChecked ->
                handleAspectRatioChecked(isChecked)
            }
        }
    }

    private fun setupCustomResolution() {
        binding.projectOptionsCustomResolution.apply {
            isChecked = project?.xmlHeader?.customResolution == true
            setOnCheckedChangeListener { _, isChecked ->
                handleCustomResolutionChecked(isChecked)
            }
        }
    }

    private fun setupProjectUpload() {
        //binding.projectOptionsUpload.setOnClickListener {
        //    projectUpload()
        //}
    }

    private fun setupProjectSaveExternal() {
        binding.projectOptionsSaveExternal.setOnClickListener {
            exportProject()
        }
    }

    private fun setupClearVars() {
        binding.projectOptionsClearVars.setOnClickListener {
            clearVars()
        }
    }

    private fun setupChangeIcon() {
        binding.projectOptionsChangeIcon.setOnClickListener {
            changeIcon()
        }
    }

    private fun setupChangeOrientation() {
        binding.projectOptionsChangeOrientation.setOnClickListener {
            changeOrientation()
        }
    }

    /*private fun setupProjectSaveApk() {
        binding.projectOptionsSaveApk.setOnClickListener {
            buildApk()
        }
    }*/

    private fun setupProjectMoreDetails() {
        binding.projectOptionsMoreDetails.setOnClickListener {
            moreDetails()
        }
    }

    private fun setupProjectOptionDelete() {
        binding.projectOptionsDelete.setOnClickListener {
            handleDeleteButtonPressed()
        }
    }

    private fun handleAspectRatioChecked(checked: Boolean) {
        project?.screenMode = if (checked) {
            ScreenModes.MAXIMIZE
        } else {
            ScreenModes.STRETCH
        }
    }

    private fun handleCustomResolutionChecked(checked: Boolean) {
        project?.xmlHeader?.setCustomResolution(checked)
    }

    private fun handleDeleteButtonPressed() {
        project ?: return

        val projectData = ProjectData(
            project!!.name,
            project!!.directory,
            project!!.catrobatLanguageVersion,
            project!!.hasScene()
        )
        AlertDialog.Builder(requireContext())
            .setTitle(resources.getQuantityString(R.plurals.delete_projects, 1))
            .setMessage(R.string.dialog_confirm_delete)
            .setPositiveButton(R.string.yes) { _: DialogInterface?, _: Int ->
                deleteProject(
                    projectData
                )
            }
            .setNegativeButton(R.string.no, null)
            .setCancelable(false)
            .show()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        for (index in 0 until menu.size()) {
            menu.getItem(index).isVisible = false
        }
        super.onPrepareOptionsMenu(menu)
    }

    override fun onPause() {
        saveProject()
        super.onPause()
    }

    private fun saveProject() {
        project ?: return
        setProjectName()
        setPhysicsArea()
        saveDescription()
        saveCreditsAndNotes()
        saveProjectSerial(project, requireContext())
    }

    override fun onResume() {
        super.onResume()

        projectManager.currentProject = project
        binding.projectOptionsNameLayout.editText?.setText(project?.name)
        setupDescriptionInputLayout()
        setupNotesAndCreditsInputLayout()

        addTags()
        hideBottomBar(requireActivity())
    }

    private fun setProjectName() {
        val name = binding.projectOptionsNameLayout.editText?.text.toString().trim()
        project ?: return

        if (project!!.name != name) {
            XstreamSerializer.getInstance().saveProject(project)
            val renamedDirectory = renameProject(project!!.directory, name)
            if (renamedDirectory == null) {
                Log.e(TAG, "Creating renamed directory failed!")
                return
            }
            loadProject(renamedDirectory, requireContext().applicationContext)
            project = projectManager.currentProject
            projectManager.currentlyEditedScene = project!!.getSceneByName(sceneName)
        }
    }

    private fun setPhysicsArea() {
        val width = binding.projectOptionsPhysicsWidthLayout.editText?.text.toString().toFloat()
        val height = binding.projectOptionsPhysicsHeightLayout.editText?.text.toString().toFloat()
        project ?: return

        val xml = project?.xmlHeader
        xml?.setPhysicsWidthArea(width)
        xml?.setPhysicsHeightArea(height)
    }

    fun saveDescription() {
        val description = binding.projectOptionsDescriptionLayout.editText?.text.toString().trim()
        if (project?.description == null || project?.description != description) {
            project?.description = description
            if (!XstreamSerializer.getInstance().saveProject(project)) {
                ToastUtil.showError(activity, R.string.error_set_description)
            }
        }
    }

    fun saveCreditsAndNotes() {
        val notesAndCredits = binding.projectOptionsNotesAndCreditsLayout.editText
            ?.text.toString().trim()
        if (project?.notesAndCredits == null || project?.notesAndCredits != notesAndCredits) {
            project?.notesAndCredits = notesAndCredits
            if (!XstreamSerializer.getInstance().saveProject(project)) {
                ToastUtil.showError(requireContext(), R.string.error_set_notes_and_credits)
            }
        }
    }

    fun projectUpload() {
        val currentProject = projectManager.currentProject
        ProjectSaver(currentProject, requireContext())
            .saveProjectAsync({ onSaveProjectComplete() })
        Utils.setLastUsedProjectName(requireContext(), currentProject.name)
    }

    private fun onSaveProjectComplete() {
        val currentProject = projectManager.currentProject

        if (Utils.isDefaultProject(currentProject, activity)) {
            binding.root.apply {
                Snackbar.make(binding.root, R.string.error_upload_default_project, Snackbar.LENGTH_LONG).show()
            }
            return
        }

        val intent = Intent(requireContext(), ProjectUploadActivity::class.java)
        intent.putExtra(PROJECT_DIR, currentProject.directory)

        startActivity(intent)
    }

    /*fun createApkFromTemplate(context: Context, projectZipFile: File): File {
        // Путь к шаблону
        val assetFile = "apk_template.zip"

        // Создаем временную папку
        val tempDir = File(context.cacheDir, "apk_temp")
        tempDir.mkdirs()

        // Извлекаем шаблон
        unzip(context.assets.open(assetFile), tempDir)

        // Заменяем project.zip на новый проект
        val projectFile = File(tempDir, "assets/project.zip")
        projectZipFile.copyTo(projectFile, overwrite = true)

        // Создаем новый APK-файл
        val newApkFile = File(context.cacheDir, "project_build.apk")
        ZipOutputStream(FileOutputStream(newApkFile)).use { zos ->
            zipDirectory3(tempDir, zos)
        }

        // Удаляем временные файлы
        tempDir.deleteRecursively()

        // Возвращаем путь к созданному APK
        return newApkFile
    }*/

    fun copyInputStreamToFile(context: Context, inputStream: InputStream, outputFile: File) {
        val outputStream = FileOutputStream(outputFile)
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
    }

    fun createApkFromTemplate(context: Context, projectZipFile: File): File {
        // Путь к шаблону APK
        val assetFile = "apk_template.zip"

        // Создаем временную папку
        val tempDir = File(context.cacheDir, "apk_temp")
        tempDir.mkdirs()

        // Извлекаем шаблон APK
        unzip(context.assets.open(assetFile), tempDir)

        // Заменяем project.zip на новый проект
        val projectFile = File(tempDir, "assets/project.zip")
        projectZipFile.copyTo(projectFile, overwrite = true)

        // Создаем unsigned APK
        val unsignedApkFile = File(context.cacheDir, "unsigned_project_build.apk")
        ZipOutputStream(FileOutputStream(unsignedApkFile)).use { zos ->
            zipDirectory3(tempDir, zos)
        }

        // Удаляем временные файлы
        tempDir.deleteRecursively()

        // Подписываем APK
        val signedApkFile = File(context.cacheDir, "signed_project_build.apk")
        val keystoreInputStream = CatroidApplication.getAppContext().assets.open("debug.jks")
        val outputFile = File(context.filesDir, "debug.jks") // Путь к файлу в директории приложения

        copyInputStreamToFile(CatroidApplication.getAppContext(), keystoreInputStream, outputFile)
        signApkWithApksig(context, unsignedApkFile, signedApkFile, "debug.p12", "keystore", "dbg", "keystore")

        // Удаляем unsigned APK
        unsignedApkFile.delete()

        return signedApkFile
    }

    fun copyKeystoreFromAssets(context: Context, keystoreFileName: String, destFile: File) {
        context.assets.open(keystoreFileName).use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    fun signApkWithApksig(
        context: Context,
        unsignedApk: File,
        signedApk: File,
        keystoreAssetName: String,
        keystorePassword: String,
        keyAlias: String,
        keyPassword: String
    ) {
        try {
            // Копируем .p12 keystore из assets
            val keystoreFile = File(context.filesDir, keystoreAssetName)
            if (!keystoreFile.exists()) {
                copyKeystoreFromAssets(context, keystoreAssetName, keystoreFile)
            }

            // Загружаем хранилище ключей PKCS#12
            val keyStore = KeyStore.getInstance("PKCS12").apply {
                load(FileInputStream(keystoreFile), keystorePassword.toCharArray())
            }

            // Получаем приватный ключ
            val privateKey = keyStore.getKey(keyAlias, keyPassword.toCharArray()) as PrivateKey

            // Преобразуем цепочку сертификатов
            val certificates = keyStore.getCertificateChain(keyAlias)
                .map { it as X509Certificate }
                .toList()

            // Настраиваем APK Signer
            val signerConfig = ApkSigner.SignerConfig.Builder(
                keyAlias,
                privateKey,
                certificates
            ).build()

            ApkSigner.Builder(listOf(signerConfig))
                .setInputApk(unsignedApk)
                .setOutputApk(signedApk)
                .build()
                .sign()
        } catch (e: Exception) {
            //ErrorLog.log(e.message?: "**message not provided :(**")
            throw RuntimeException("Ошибка при подписании APK: ${e.message}", e)
        }
    }

    /*fun signApkWithApksig(
        unsignedApk: File,
        signedApk: File,
        keystore: File,
        keystorePassword: String,
        keyAlias: String,
        keyPassword: String
    ) {
        try {
            // Загружаем keystore
            val keyStore = KeyStore.getInstance("JKS").apply {
                load(FileInputStream(keystore), keystorePassword.toCharArray())
            }

            // Получаем приватный ключ
            val privateKey = keyStore.getKey(keyAlias, keyPassword.toCharArray()) as PrivateKey

            // Получаем сертификаты и преобразуем их в список X509Certificate
            val certificates = keyStore.getCertificateChain(keyAlias)
                .map { it as X509Certificate }
                .toList()

            // Настраиваем подписчик APK
            val signerConfig = ApkSigner.SignerConfig.Builder(
                keyAlias,
                privateKey,
                certificates
            ).build()

            // Создаем подписчик и подписываем APK
            ApkSigner.Builder(listOf(signerConfig))
                .setInputApk(unsignedApk)
                .setOutputApk(signedApk)
                .build()
                .sign()
        } catch (e: Exception) {
            throw RuntimeException("Ошибка при подписании APK: ${e.message}", e)
        }
    }*/

    // Функция подписи APK с помощью ZipSigner
    /*fun signApkWithZipSigner(context: Context, unsignedApk: File, signedApk: File) {
        try {
            // Используем ZipSigner
            val zipSigner = ZipSigner()

            // Указываем режим подписи (используем стандартный тестовый ключ)
            zipSigner.setKeymode("testkey") // Или customkey, если у тебя есть собственный

            // Подписываем APK
            zipSigner.signZip(unsignedApk.absolutePath, signedApk.absolutePath)
        } catch (e: Exception) {
            throw RuntimeException("Ошибка подписи APK: ${e.message}", e)
        }
    }*/

// Вспомогательные функции unzip и zipDirectory3 остаются без изменений

    fun unzip(inputStream: InputStream, outDir: File) {
        ZipInputStream(inputStream).use { zis ->
            var entry: ZipEntry?
            while (zis.nextEntry.also { entry = it } != null) {
                val file = File(outDir, entry!!.name)
                if (entry!!.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile.mkdirs()
                    FileOutputStream(file).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                zis.closeEntry()
            }
        }
    }

    fun zipDirectory3(dir: File, zos: ZipOutputStream, basePath: String = "") {
        // Добавляем запись для самой директории
        val dirEntry = ZipEntry(basePath)
        zos.putNextEntry(dirEntry)
        zos.closeEntry()

        dir.listFiles()?.forEach { file ->
            val filePath = basePath + file.name
            if (file.isDirectory) {
                // Рекурсивно добавляем поддиректории
                zipDirectory3(file, zos, "$filePath/")
            } else {
                FileInputStream(file).use { fis ->
                    zos.putNextEntry(ZipEntry(filePath))
                    fis.copyTo(zos)
                    zos.closeEntry()
                }
            }
        }
    }

    //dbg: keystore


    private fun exportProject() {
        saveProject()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            exportUsingSystemFilePicker()
        } else {
            exportToExternalMemory()
        }
    }

    fun showToast(toast: String) {
        if (StageActivity.messageHandler != null) {
            val params = ArrayList<Any>(listOf(toast))
            StageActivity.messageHandler.obtainMessage(StageActivity.SHOW_TOAST, params).sendToTarget()
        } else {
            // Обработка ситуации, когда messageHandler равно null
            Log.e("ShowToast", "messageHandler is null!")
        }
    }

    fun getRandomMessage(): String {
        val messages = listOf(
            "Готово!",
            "Сделано!",
            "Успех!",
            "Завершено!",
            "Готово к использованию!",
            "Задача выполнена!",
            "Отличная работа!",
            "Все готово!",
            "Яйцо или курица..?",
            "Готово! Проверяй!",
            "Поехали!",
            "Вроде сделано..",
            "Проверяй, начальник э!",
            "Готово. Удачи с проектом!",
            "Работа завершена, как кофе на утро!",
            "Готово! Как будто я маг, а не программист!",
            "Все сделано! Как раз вовремя перед обедом.",
            "Все завершено! Можно идти за пирожками!",
            "Задача выполнена! Теперь можно отдохнуть и посмотреть котиков.",
            "Готово! Даже не успел заметить, как это произошло.",
            "Сделано! Осталось только отпраздновать с танцами.",
            "Готово! Минутка успокоения перед новыми приключениями.",
            "Отличная работа! Ты как супергерой, только без плаща.",
            "Готово! Наконец-то смогу отвлечься на онлайн-шопинг.",
            "Как сказать: «Сделай это» и получить: «Сделано!»? Вот так!",
            "Все готово! Теперь можем заниматься более важными делами.",
            "Задача выполнена! Как хорошая книга – не отпускает до последней страницы.",
            "Готово! Можно отдыхать, как будто мы все это сделали за пятюню.",
            "Сделано! Готовы к новым подвигам?"
        )

        // Генерируем случайный индекс
        val randomIndex = Random.nextInt(messages.size)
        // Возвращаем случайное сообщение
        return messages[randomIndex]
    }

    fun getRandomError(): String {
        val errorMessages = listOf(
            "Произошла ошибка! Кажется, я не тот алгоритм заказывал.",
            "Упс! Что-то пошло не так. Как будто кошка пробежала по клавиатуре.",
            "Произошла ошибка! Может, система решила немного отдохнуть?",
            "Ой! Похоже, произошла ошибка. Возможно, это программистская шутка?",
            "Произошла ошибка! Да кто придумал обновлять программу перед дедлайном?",
            "Упс! Ошибка. Наверное, мой код тоже решил поспать.",
            "Произошла ошибка! Как бы я ни старался, выводы не совпали.",
            "Ой-ой! Ошибка! Это как раз то, что нам нужно было избежать.",
            "Произошла ошибка! По всей видимости, сервер тоже устал.",
            "Упс! Ошибка. Это как забыть о важной встрече.",
            "Произошла ошибка! Может, стоит заказывать пиццу вместо кода?",
            "Ой! Ошибка. Обычно говорят, что все дороги ведут к Риму, но не сегодня.",
            "Произошла ошибка! Это не то, что я хотел об этом напомнить.",
            "Упс! Ошибка! Возможно, машина решила, что у нее выходной.",
            "Произошла ошибка! Я попытался угостить код печеньками и вот что вышло!",
            "Ой-ой! Ошибка. Наверное, в коде слишком много любопытных переменных.",
            "Произошла ошибка! Извините, не я такой - жизнь такая!",
            "Упс! Произошла ошибка. Код сам по себе иногда делает капризы.",
            "Ой! Произошла ошибка! Как будто интернет пошел на пикник без меня.",
            "Произошла ошибка! И тут, конечно, глюк всегда оказывается виноват.",
            "Упс! Ошибка. Вы знаете, прощать - это тоже искусство."
        )

        val randomIndex = Random.nextInt(errorMessages.size)
        // Возвращаем случайное сообщение
        return errorMessages[randomIndex]
    }

    private fun changeOrientation() {
        saveProject()
        val width = project?.xmlHeader?.getVirtualScreenWidth() ?: 800
        val height = project?.xmlHeader?.getVirtualScreenHeight() ?: 1080
        project?.xmlHeader?.setVirtualScreenWidth(height)
        project?.xmlHeader?.setVirtualScreenHeight(width)
        showToast(getRandomMessage())
    }

    private fun changeIcon() {
        saveProject()
        project?.let { proj ->
            val directory: File = proj.directory

            // Создаем файл для нового значка
            val oldIconFile = File(directory, "automatic_screenshot.png")
            val newIconFile = File(directory, "manual_screenshot.png")

            // Удаляем старый файл, если он существует
            if (oldIconFile.exists()) {
                oldIconFile.delete()
            }

            // Открываем выбор изображения
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, REQUEST_SELECT_IMAGE)
        } ?: run {
            showToast(getRandomError())
        }
    }


    private fun clearVars() {
        saveProject()
        project?.let {
            val directory: File = it.directory

            val deviceVariablesFile = File(directory, "DeviceVariables.json")
            val deviceListsFile = File(directory, "DeviceLists.json")

            // Удаляем файлы, если они существуют
            val variablesDeleted = deviceVariablesFile.delete()
            val listsDeleted = deviceListsFile.delete()

            // Проверяем, были ли файлы успешно удалены
            if (variablesDeleted || listsDeleted) {
                showToast(getRandomMessage())
            } else {
                showToast(getRandomMessage())
            }
        } ?: run {
            showToast(getRandomError())
        }
    }


    private fun buildApk() {
        saveProject()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            buildUsingSystemFilePicker()
        } else {
            buildToExternalMemory()
            //Log.e("BUILD", "Version SDK is not supported")
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun exportUsingSystemFilePicker() {
        val fileName = project?.name + Constants.CATROBAT_EXTENSION
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.putExtra(Intent.EXTRA_TITLE, fileName)
        intent.type = "*/*"
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOWNLOADS)
        val title = requireContext().getString(R.string.export_project)
        startActivityForResult(Intent.createChooser(intent, title), REQUEST_EXPORT_PROJECT)
    }

    fun zipDirectory(sourceDir: File, zipFile: File): File {
        ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
            sourceDir.walk().forEach { file ->
                // Создаем запись zip для каждого файла
                if(file.name != "undo_code.xml") {
                    val zipEntry = ZipEntry(file.relativeTo(sourceDir).path)
                    zipOut.putNextEntry(zipEntry)

                    // Если это файл, то копируем его содержимое в zip
                    if (file.isFile) {
                        FileInputStream(file).use { fis ->
                            fis.copyTo(zipOut)
                        }
                    }
                    // Закрываем запись для данного zipEntry
                    zipOut.closeEntry()
                }
            }
        }
        // Возвращаем созданный zip файл
        return zipFile
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun buildUsingSystemFilePicker() {
        try {
            Log.d("BUILD", project?.directory?.name.toString())
            Log.d("BUILD", project?.directory.toString())
            val projectDirectory = project?.directory ?: return

            val tempDir = File(CatroidApplication.getAppContext().cacheDir, "for_build")
            tempDir.mkdirs()
            zipTempDir = tempDir

            // Определяем имя zip-файла
            val zipFile = File(tempDir, "project.zip")

            // Создаем zip-файл
            projectInZip = zipDirectory(projectDirectory, zipFile)

            Log.d("BUILD", "Zip файл успешно создан: ${zipFile.absolutePath}")
            val fileName = project?.name + Constants.APK_EXTENSION
            buildFilename = fileName
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.putExtra(Intent.EXTRA_TITLE, fileName)
            intent.type = "*/*"
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, Environment.DIRECTORY_DOWNLOADS)
            val title = requireContext().getString(R.string.build_apk)
            startActivityForResult(Intent.createChooser(intent, title), REQUEST_BUILD_PROJECT)
        } catch (e: IOException) {
            Log.e("BUILD", "Can't start build: ", e)
        }
    }

    private fun buildToExternalMemory() {
        object : RequiresPermissionTask(
            PERMISSIONS_REQUEST_EXPORT_TO_EXTERNAL_STORAGE,
            listOf(permission.WRITE_EXTERNAL_STORAGE, permission.READ_EXTERNAL_STORAGE),
            R.string.runtime_permission_general
        ) {
            override fun task() {
                Log.d("BUILD", project?.directory?.name.toString())
                Log.d("BUILD", project?.directory.toString())
                val projectDirectory = project?.directory ?: return

                val tempDir = File(CatroidApplication.getAppContext().cacheDir, "for_build")
                tempDir.mkdirs()
                zipTempDir = tempDir

                // Определяем имя zip-файла
                val zipFile = File(tempDir, "project.zip")

                // Создаем zip-файл
                projectInZip = zipDirectory(projectDirectory, zipFile)

                Log.d("BUILD", "Zip файл успешно создан: ${zipFile.absolutePath}")
                val fileName = project?.name + Constants.APK_EXTENSION
                buildFilename = fileName
                val projectZip = File(Constants.DOWNLOAD_DIRECTORY, fileName)
                Constants.DOWNLOAD_DIRECTORY.mkdirs()
                if (!Constants.DOWNLOAD_DIRECTORY.isDirectory) {
                    return
                }
                if (projectZip.exists()) {
                    projectZip.delete()
                }
                val projectDestination = Uri.fromFile(projectZip)
                startAsyncProjectBuild(projectDestination)
            }
        }.execute(requireActivity())
    }

    private fun exportToExternalMemory() {
        object : RequiresPermissionTask(
            PERMISSIONS_REQUEST_EXPORT_TO_EXTERNAL_STORAGE,
            listOf(permission.WRITE_EXTERNAL_STORAGE, permission.READ_EXTERNAL_STORAGE),
            R.string.runtime_permission_general
        ) {
            override fun task() {
                val fileName = project?.name + Constants.CATROBAT_EXTENSION
                val projectZip = File(Constants.DOWNLOAD_DIRECTORY, fileName)
                Constants.DOWNLOAD_DIRECTORY.mkdirs()
                if (!Constants.DOWNLOAD_DIRECTORY.isDirectory) {
                    return
                }
                if (projectZip.exists()) {
                    projectZip.delete()
                }
                val projectDestination = Uri.fromFile(projectZip)
                startAsyncProjectExport(projectDestination)
            }
        }.execute(requireActivity())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        data ?: return
        if (requestCode == REQUEST_EXPORT_PROJECT && resultCode == Activity.RESULT_OK) {
            val projectDestination = data.data ?: return
            startAsyncProjectExport(projectDestination)
        }
        if (requestCode == REQUEST_BUILD_PROJECT && resultCode == Activity.RESULT_OK) {
            val projectDestination = data.data ?: return
            startAsyncProjectBuild(projectDestination)
        }
        if (requestCode == REQUEST_SELECT_IMAGE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                try {
                    val contentResolver: ContentResolver = CatroidApplication.getAppContext().contentResolver
                    // Получаем путь к выбранному изображению
                    val inputStream = contentResolver.openInputStream(uri)
                    val outputStream = FileOutputStream(File(project?.directory, "manual_screenshot.png"))

                    // Копируем данные из inputStream в outputStream
                    inputStream?.copyTo(outputStream)

                    inputStream?.close()
                    outputStream.close()

                    showToast(getRandomMessage())
                } catch (e: Exception) {
                    showToast("Ошибка при сохранении изображения: ${e.message}")
                }
            } ?: showToast(getRandomError())
        }
    }

    fun copyFileToUri(context: Context, sourceFile: File, directoryUri: Uri, fileName: String) {
        // Получаем ContentResolver
        val resolver: ContentResolver = context.contentResolver

        // Создаем Uri для нового файла
        val fileUri = Uri.withAppendedPath(directoryUri, fileName)

        // Открываем выходной поток для записи в новый файл
        resolver.openOutputStream(fileUri)?.use { outputStream: OutputStream ->
            // Открываем входной поток для чтения из исходного файла
            FileInputStream(sourceFile).use { inputStream ->
                // Копируем данные из входного потока в выходной
                inputStream.copyTo(outputStream)
            }
        } ?: run {
            // Обработка ошибки, если выходной поток не удалось открыть
            println("Ошибка: Не удалось создать файл в указанной директории.")
        }
    }

    private fun copyFileToUri2(context: Context, sourceFile: File, destinationUri: Uri, fileName: String) {
        context.contentResolver.openOutputStream(destinationUri).use { outputStream ->
            FileInputStream(sourceFile).use { inputStream ->
                outputStream?.let {
                    inputStream.copyTo(it)
                } ?: Log.e("BUILD", "Ошибка открытия OutputStream для $destinationUri")
            }
        }
    }


    private fun startAsyncProjectExport(projectDestination: Uri) {
        project?.let {
            val notificationData = StatusBarNotificationManager(requireContext())
                .createSaveProjectToExternalMemoryNotification(
                    requireContext(),
                    projectDestination,
                    it.name
                )
            ProjectExportTask(it.directory, projectDestination, notificationData, requireContext())
                .execute()
        }
    }

    private fun startAsyncProjectBuild(projectDestination: Uri) {
        buildFilename?.let { fileName ->
            projectInZip?.let { zip ->
                project?.let {
                    val notificationData = StatusBarNotificationManager(requireContext())
                        .createBuildProjectToExternalMemoryNotification(
                            requireContext(),
                            projectDestination,
                            it.name
                        )
                    //ProjectExportTask(it.directory, projectDestination, notificationData, requireContext())
                    //    .execute()
                    if (zip.exists()) {
                        Log.d("BUILD", "Project directory: ${zip.absolutePath}")
                        val builded_apk = createApkFromTemplate(CatroidApplication.getAppContext(), zip)
                        requireContext().grantUriPermission(requireActivity().packageName, projectDestination, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        copyFileToUri2(CatroidApplication.getAppContext(), builded_apk, projectDestination, fileName)
                        builded_apk.delete()
                        zipTempDir?.deleteRecursively()
                        StatusBarNotificationManager(context).showOrUpdateNotification(
                            context, notificationData, 100, null)
                    } else {
                        Log.e("BUILD", "Файл project.zip не существует по состоянию на момент копирования!")
                    }
                }
            }
        }
    }

    private fun moreDetails() {
        val fragment = ProjectDetailsFragment()
        val args = Bundle()
        project?.let {
            val projectData = ProjectData(
                it.name,
                it.directory,
                it.catrobatLanguageVersion,
                it.hasScene()
            )
            args.putSerializable(ProjectDetailsFragment.SELECTED_PROJECT_KEY, projectData)
        }
        fragment.arguments = args
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment, ProjectDetailsFragment.TAG)
            .addToBackStack(ProjectDetailsFragment.TAG).commit()
    }

    private fun deleteProject(selectedProject: ProjectData) {
        try {
            StorageOperations.deleteDir(selectedProject.directory)
        } catch (exception: IOException) {
            Log.e(TAG, Log.getStackTraceString(exception))
        }
        ToastUtil.showSuccess(
            requireContext(),
            resources.getQuantityString(R.plurals.deleted_projects, 1, 1)
        )
        project = null
        projectManager.currentProject = project
        requireActivity().onBackPressed()
    }

    companion object {
        val TAG: String = ProjectOptionsFragment::class.java.simpleName

        private const val PERMISSIONS_REQUEST_EXPORT_TO_EXTERNAL_STORAGE = 802
        private const val REQUEST_EXPORT_PROJECT = 10
        private const val REQUEST_BUILD_PROJECT = 11
        private const val REQUEST_SELECT_IMAGE = 12
    }
}
