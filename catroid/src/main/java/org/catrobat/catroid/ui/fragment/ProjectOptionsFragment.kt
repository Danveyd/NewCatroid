package org.catrobat.catroid.ui.fragment

import android.Manifest.permission
import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentResolver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.text.Editable
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.android.apksig.ApkSigner
import com.danvexteam.lunoscript_annotations.LunoClass
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.hcaptcha.sdk.HCaptcha
import com.hcaptcha.sdk.HCaptchaConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.buffer
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.R
import org.catrobat.catroid.apkbuild.ApkToolboxManager
import org.catrobat.catroid.apkbuild.ProjectScanner
import org.catrobat.catroid.apkbuild.TemplateManager
import org.catrobat.catroid.common.Constants
import org.catrobat.catroid.common.LookData
import org.catrobat.catroid.common.Nameable
import org.catrobat.catroid.common.ProjectData
import org.catrobat.catroid.common.ScreenModes
import org.catrobat.catroid.content.Project
import org.catrobat.catroid.content.XmlHeader
import org.catrobat.catroid.databinding.FragmentProjectOptionsBinding
import org.catrobat.catroid.io.StorageOperations
import org.catrobat.catroid.io.XstreamSerializer
import org.catrobat.catroid.io.asynctask.ProjectExportTask
import org.catrobat.catroid.io.asynctask.ProjectSaver
import org.catrobat.catroid.io.asynctask.loadProject
import org.catrobat.catroid.io.asynctask.renameProject
import org.catrobat.catroid.io.asynctask.saveProjectSerial
import org.catrobat.catroid.merge.NewProjectNameTextWatcher
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.ui.BottomBar.hideBottomBar
import org.catrobat.catroid.ui.PROJECT_DIR
import org.catrobat.catroid.ui.ProjectUploadActivity
import org.catrobat.catroid.ui.SettingsActivity
import org.catrobat.catroid.ui.runtimepermissions.RequiresPermissionTask
import org.catrobat.catroid.utils.ToastUtil
import org.catrobat.catroid.utils.Utils
import org.catrobat.catroid.utils.community.CommunityTokenManager
import org.catrobat.catroid.utils.git.GitController
import org.catrobat.catroid.utils.git.GitResult
import org.catrobat.catroid.utils.git.TokenManager
import org.catrobat.catroid.utils.lunoscript.baker.ProjectBaker
import org.catrobat.catroid.utils.notifications.StatusBarNotificationManager
import org.json.JSONArray
import org.json.JSONObject
import org.koin.android.ext.android.inject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.random.Random

@LunoClass
class ProjectOptionsFragment : Fragment() {

    private val projectManager: ProjectManager by inject()
    private var _binding: FragmentProjectOptionsBinding? = null
    private val binding get() = _binding!!
    private var project: Project? = null
    private var sceneName: String? = null
    private var projectInZip: File? = null
    private var buildFilename: String? = null
    private var zipTempDir: File? = null
    private lateinit var gitController: GitController
    private var progressDialog: AlertDialog? = null

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val genresList = listOf(
        "Action", "Adventure", "Arcade", "Casual", "Platformer", "Puzzle", "RPG", "Racing", "Shooter", "Simulation",
        "Sports", "Strategy", "Fighting", "Survival", "Stealth", "Roguelike", "Sandbox", "Tower Defense", "MMO", "MOBA",
        "Indie", "Retro", "Pixel Art", "VR", "Multiplayer", "Singleplayer", "Co-op", "Educational", "Music", "Card",
        "Fantasy", "Sci-Fi", "Horror", "Cyberpunk", "Post-apocalyptic", "Steampunk", "Anime", "Cartoon", "Realistic",
        "Mystery", "Romance", "Comedy", "Thriller", "Historical", "Drama", "Other"
    )

    private var selectedCustomKeystoreFile: File? = null
    private var tempGenAlias: String? = null
    private var tempGenPass: String? = null
    private var tempGenOwner: String? = null

    private var activeDialogView: View? = null

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

        gitController = GitController(project!!.directory)

        setupNameInputLayout()
        setupPhysicsInputLayout()
        setupDescriptionInputLayout()
        setupNotesAndCreditsInputLayout()
        addTags()
        setupProjectAspectRatio()
        setupCustomResolution()
        setupDisableScientificNotation()

        binding.projectOptionsUpload.visibility = View.VISIBLE
        binding.projectOptionsUpload.text = getString(R.string.community_publish_btn)
        binding.projectOptionsUpload.setOnClickListener {
            startCommunityPublishFlow()
        }

        setupProjectSaveExternal()
        setupRebuildCache()
        setupClearVars()
        setupChangeIcon()
        setupChangeOrientation()
        setupProjectMoreDetails()
        setupProjectOptionDelete()
        setupMishkFrede()

        setupGitButtons()
        setupBakeOption()

        val buildApkBtn = view.findViewById<android.widget.TextView>(R.id.project_options_build_apk)
        buildApkBtn?.setOnClickListener {
            showBuildDialog()
        }

        hideBottomBar(requireActivity())
    }

    override fun onDestroyView() {
        activeDialogView = null
        _binding = null
        super.onDestroyView()
    }

    private fun setupDisableScientificNotation() {
        binding.projectOptionsDisableScientificNotation.apply {
            isChecked = project?.isDisableScientificNotation ?: false
            setOnCheckedChangeListener { _, isChecked ->
                project?.setDisableScientificNotation(isChecked)
            }
        }
    }

    private fun showBuildDialog() {
        val context = requireContext()
        project ?: return

        val scanResult = ProjectScanner.scanProject(project!!.directory)
        val dialogView = layoutInflater.inflate(R.layout.dialog_apk_build_settings, null)
        activeDialogView = dialogView

        val tvStepSubtitle = dialogView.findViewById<TextView>(R.id.tvStepSubtitle)
        val stepProgressIndicator = dialogView.findViewById<ProgressBar>(R.id.stepProgressIndicator)

        val step1Container = dialogView.findViewById<LinearLayout>(R.id.step1Container)
        val step2Container = dialogView.findViewById<LinearLayout>(R.id.step2Container)
        val step3Container = dialogView.findViewById<LinearLayout>(R.id.step3Container)
        val step4Container = dialogView.findViewById<LinearLayout>(R.id.step4Container)

        val btnCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)
        val btnBack = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnBack)
        val btnNext = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnNext)

        val etAppName = dialogView.findViewById<TextInputEditText>(R.id.etBuildAppName)
        val etPackageName = dialogView.findViewById<TextInputEditText>(R.id.etBuildPackageName)
        val etVersionName = dialogView.findViewById<TextInputEditText>(R.id.etBuildVersionName)
        val etVersionCode = dialogView.findViewById<TextInputEditText>(R.id.etBuildVersionCode)
        val spinnerTemplates = dialogView.findViewById<AutoCompleteTextView>(R.id.etBuildTemplate)

        val toggleGroupOrientation = dialogView.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.toggleGroupOrientation)

        val switchPython = dialogView.findViewById<SwitchMaterial>(R.id.switchPython)
        val switchQemu = dialogView.findViewById<SwitchMaterial>(R.id.switchQemu)
        val switchCompiler = dialogView.findViewById<SwitchMaterial>(R.id.switchCompiler)
        val switchMkit = dialogView.findViewById<SwitchMaterial>(R.id.switchMkit)
        val switchOnnx = dialogView.findViewById<SwitchMaterial>(R.id.switchOnnx)
        val switchMnn = dialogView.findViewById<SwitchMaterial>(R.id.switchMnn)
        val cbArm64 = dialogView.findViewById<CheckBox>(R.id.cbArm64)
        val cbArmv7 = dialogView.findViewById<CheckBox>(R.id.cbArmv7)

        val cgDetectedPermissions = dialogView.findViewById<com.google.android.material.chip.ChipGroup>(R.id.cgDetectedPermissions)
        val switchCustomizePermissions = dialogView.findViewById<SwitchMaterial>(R.id.switchCustomizePermissions)
        val llManualPermissions = dialogView.findViewById<LinearLayout>(R.id.llManualPermissions)

        val switchPermInternet = dialogView.findViewById<SwitchMaterial>(R.id.switchPermInternet)
        val switchPermStorage = dialogView.findViewById<SwitchMaterial>(R.id.switchPermStorage)
        val switchPermCamera = dialogView.findViewById<SwitchMaterial>(R.id.switchPermCamera)
        val switchPermAudio = dialogView.findViewById<SwitchMaterial>(R.id.switchPermAudio)
        val switchPermLocation = dialogView.findViewById<SwitchMaterial>(R.id.switchPermLocation)
        val switchPermBluetooth = dialogView.findViewById<SwitchMaterial>(R.id.switchPermBluetooth)
        val switchPermNfc = dialogView.findViewById<SwitchMaterial>(R.id.switchPermNfc)
        val switchPermVibrate = dialogView.findViewById<SwitchMaterial>(R.id.switchPermVibrate)
        val switchPermOverlay = dialogView.findViewById<SwitchMaterial>(R.id.switchPermOverlay)
        val switchPermBackground = dialogView.findViewById<SwitchMaterial>(R.id.switchPermBackground)

        val rgKeystore = dialogView.findViewById<RadioGroup>(R.id.rgKeystore)
        val llGenerateKeystoreFields = dialogView.findViewById<LinearLayout>(R.id.llGenerateKeystoreFields)
        val llCustomKeystoreFields = dialogView.findViewById<LinearLayout>(R.id.llCustomKeystoreFields)

        val etGenAlias = dialogView.findViewById<TextInputEditText>(R.id.etGenAlias)
        val etGenPass = dialogView.findViewById<TextInputEditText>(R.id.etGenPass)
        val etGenOwner = dialogView.findViewById<TextInputEditText>(R.id.etGenOwner)
        val btnGenerateAndExportKeystore = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnGenerateAndExportKeystore)

        val btnSelectKeystore = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSelectKeystore)
        val tvSelectedKeystorePath = dialogView.findViewById<TextView>(R.id.tvSelectedKeystorePath)
        val etKeystoreAlias = dialogView.findViewById<TextInputEditText>(R.id.etKeystoreAlias)
        val etKeystorePass = dialogView.findViewById<TextInputEditText>(R.id.etKeystorePass)

        etAppName.setText(project!!.name)
        var cleanProjName = project!!.name.lowercase().replace(Regex("[^a-z0-9]"), "").ifEmpty { "game" }
        if (cleanProjName.isNotEmpty() && cleanProjName[0].isDigit()) {
            cleanProjName = "project_$cleanProjName"
        }
        etPackageName.setText("com.newcatroid.$cleanProjName")
        etVersionName.setText("1.0.0")
        etVersionCode.setText("1")

        switchPermInternet.isChecked = scanResult.needsInternet
        switchPermStorage.isChecked = scanResult.needsStorage
        switchPermCamera.isChecked = scanResult.needsCamera
        switchPermAudio.isChecked = scanResult.needsAudioRecord
        switchPermLocation.isChecked = scanResult.needsLocation
        switchPermBluetooth.isChecked = scanResult.needsBluetooth
        switchPermNfc.isChecked = scanResult.needsNfc
        switchPermVibrate.isChecked = scanResult.needsVibrate
        switchPermOverlay.isChecked = scanResult.needsOverlay
        switchPermBackground.isChecked = scanResult.needsBackground

        switchPython.isChecked = scanResult.hasPython
        switchQemu.isChecked = scanResult.hasQemu
        switchCompiler.isChecked = scanResult.hasCompiler
        switchMkit.isChecked = scanResult.hasMkit
        switchOnnx.isChecked = scanResult.hasOnnx
        switchMnn.isChecked = scanResult.hasMnn

        cgDetectedPermissions.removeAllViews()
        val addFriendlyPermissionChip = { text: String ->
            val chip = com.google.android.material.chip.Chip(context).apply {
                this.text = text
                isClickable = false
                isCheckable = false
                setChipBackgroundColorResource(R.color.button_background)
                setTextColor(context.resources.getColor(R.color.solid_white))
                setChipStrokeColorResource(R.color.accent)
                chipStrokeWidth = resources.displayMetrics.density * 1f
            }
            cgDetectedPermissions.addView(chip)
        }

        if (scanResult.needsInternet) addFriendlyPermissionChip(getString(R.string.perm_internet_short))
        if (scanResult.needsStorage) addFriendlyPermissionChip(getString(R.string.perm_storage_short))
        if (scanResult.needsCamera) addFriendlyPermissionChip(getString(R.string.perm_camera_short))
        if (scanResult.needsAudioRecord) addFriendlyPermissionChip(getString(R.string.perm_audio_short))
        if (scanResult.needsLocation) addFriendlyPermissionChip(getString(R.string.perm_location_short))
        if (scanResult.needsBluetooth) addFriendlyPermissionChip(getString(R.string.perm_bluetooth_short))
        if (scanResult.needsNfc) addFriendlyPermissionChip(getString(R.string.perm_nfc_short))
        if (scanResult.needsVibrate) addFriendlyPermissionChip(getString(R.string.perm_vibrate_short))
        if (scanResult.needsOverlay) addFriendlyPermissionChip(getString(R.string.perm_overlay_short))
        if (scanResult.needsBackground) addFriendlyPermissionChip(getString(R.string.perm_background_short))

        switchCustomizePermissions.setOnCheckedChangeListener { _, isChecked ->
            androidx.transition.TransitionManager.beginDelayedTransition(dialogView as ViewGroup, androidx.transition.AutoTransition())
            llManualPermissions.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        val isLandscape = project!!.xmlHeader.islandscapeMode()
        toggleGroupOrientation.check(if (isLandscape) R.id.btnLandscape else R.id.btnPortrait)

        fun updateOrientationTogglesVisuals() {
            val checkedId = toggleGroupOrientation.checkedButtonId
            val buttons = listOf(R.id.btnPortrait, R.id.btnLandscape, R.id.btnSensor)

            for (id in buttons) {
                val button = dialogView.findViewById<com.google.android.material.button.MaterialButton>(id) ?: continue
                if (id == checkedId) {
                    button.backgroundTintList = android.content.res.ColorStateList.valueOf(context.resources.getColor(R.color.accent))
                    button.setTextColor(context.resources.getColor(R.color.app_background_dark))
                } else {
                    button.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT)
                    button.setTextColor(context.resources.getColor(R.color.solid_white))
                }
            }
        }

        updateOrientationTogglesVisuals()

        toggleGroupOrientation.addOnButtonCheckedListener { _, _, _ ->
            updateOrientationTogglesVisuals()
        }

        var availableTemplates = listOf<TemplateManager.TemplateRelease>()
        var selectedTemplateIndex = -1

        lifecycleScope.launch(Dispatchers.Main) {
            showProgressDialog(getString(R.string.fetching_templates))
            try {
                availableTemplates = TemplateManager.fetchAvailableTemplates()
                if (availableTemplates.isEmpty()) {
                    ToastUtil.showError(context, getString(R.string.error_fetch_templates))
                } else {
                    val templateNames = availableTemplates.map { "${it.tagName} (${it.fileName})" }
                    val templateAdapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, templateNames)
                    spinnerTemplates.setAdapter(templateAdapter)

                    val currentVersionName = try {
                        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "2.2.0"
                    } catch (e: Exception) {
                        "2.2.0"
                    }

                    fun parseSemVer(versionStr: String): List<Int> {
                        val baseVersion = versionStr.substringBefore("-")
                        val clean = baseVersion.replace(Regex("[^0-9.]"), "")
                        return clean.split(".").mapNotNull { it.toIntOrNull() }
                    }

                    fun compareSemVer(v1: List<Int>, v2: List<Int>): Int {
                        val maxLen = maxOf(v1.size, v2.size)
                        for (i in 0 until maxLen) {
                            val part1 = v1.getOrNull(i) ?: 0
                            val part2 = v2.getOrNull(i) ?: 0
                            if (part1 != part2) {
                                return part1.compareTo(part2)
                            }
                        }
                        return 0
                    }

                    val parsedAppVersion = parseSemVer(currentVersionName)

                    var bestMatchIndex = -1
                    var bestMatchVersion: List<Int>? = null

                    for (index in availableTemplates.indices) {
                        val template = availableTemplates[index]
                        val parsedTemplateVersion = parseSemVer(template.tagName)

                        val comparison = compareSemVer(parsedTemplateVersion, parsedAppVersion)

                        if (comparison <= 0) {
                            if (bestMatchVersion == null || compareSemVer(parsedTemplateVersion, bestMatchVersion) > 0) {
                                bestMatchVersion = parsedTemplateVersion
                                bestMatchIndex = index
                            }
                        }
                    }

                    val defaultIndex = if (bestMatchIndex != -1) bestMatchIndex else 0

                    if (defaultIndex != -1) {
                        spinnerTemplates.setText(templateNames[defaultIndex], false)
                        selectedTemplateIndex = defaultIndex
                    } else {
                        spinnerTemplates.setText(templateNames[0], false)
                        selectedTemplateIndex = 0
                    }
                }
            } catch (e: Exception) {
                ToastUtil.showError(context, "${getString(R.string.error_fetch_templates)} ${e.localizedMessage}")
            } finally {
                hideProgressDialog()
            }
        }

        spinnerTemplates.setOnItemClickListener { _, _, position, _ ->
            selectedTemplateIndex = position
        }

        rgKeystore.setOnCheckedChangeListener { _, checkedId ->
            androidx.transition.TransitionManager.beginDelayedTransition(dialogView as ViewGroup, androidx.transition.AutoTransition())
            when (checkedId) {
                R.id.rbAutoDebug -> {
                    llGenerateKeystoreFields.visibility = View.GONE
                    llCustomKeystoreFields.visibility = View.GONE
                }
                R.id.rbGenerateNew -> {
                    llGenerateKeystoreFields.visibility = View.VISIBLE
                    llCustomKeystoreFields.visibility = View.GONE
                }
                R.id.rbCustomKeystore -> {
                    llGenerateKeystoreFields.visibility = View.GONE
                    llCustomKeystoreFields.visibility = View.VISIBLE
                }
            }
        }

        btnGenerateAndExportKeystore.setOnClickListener {
            val genAlias = etGenAlias.text.toString().trim()
            val genPass = etGenPass.text.toString().trim()
            val genOwner = etGenOwner.text.toString().trim()

            if (genAlias.isEmpty() || genPass.isEmpty() || genOwner.isEmpty()) {
                ToastUtil.showError(context, getString(R.string.error_fill_generation))
                return@setOnClickListener
            }

            tempGenAlias = genAlias
            tempGenPass = genPass
            tempGenOwner = genOwner

            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/x-pkcs12"
                putExtra(Intent.EXTRA_TITLE, "newcatroid_release_key.p12")
            }
            startActivityForResult(intent, REQUEST_CODE_GENERATE_KEYSTORE)
        }

        btnSelectKeystore.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(intent, REQUEST_CODE_SELECT_KEYSTORE)
        }
        tvSelectedKeystorePathTag = tvSelectedKeystorePath

        val builder = AlertDialog.Builder(context, R.style.Theme_NewCatroid_Dialog)
            .setView(dialogView)
        val dialog = builder.create()

        btnCancel.setOnClickListener { dialog.dismiss() }

        var currentStep = 1

        fun updateStepUI() {
            androidx.transition.TransitionManager.beginDelayedTransition(dialogView as ViewGroup, androidx.transition.AutoTransition())

            when (currentStep) {
                1 -> {
                    tvStepSubtitle.text = getString(R.string.step_format, 1, getString(R.string.step_basic))
                    stepProgressIndicator.progress = 25
                    step1Container.visibility = View.VISIBLE
                    step2Container.visibility = View.GONE
                    step3Container.visibility = View.GONE
                    step4Container.visibility = View.GONE

                    btnBack.visibility = View.INVISIBLE
                    btnNext.text = getString(R.string.btn_next)
                }
                2 -> {
                    tvStepSubtitle.text = getString(R.string.step_format, 2, getString(R.string.step_modules))
                    stepProgressIndicator.progress = 50
                    step1Container.visibility = View.GONE
                    step2Container.visibility = View.VISIBLE
                    step3Container.visibility = View.GONE
                    step4Container.visibility = View.GONE

                    btnBack.visibility = View.VISIBLE
                    btnNext.text = getString(R.string.btn_next)
                }
                3 -> {
                    tvStepSubtitle.text = getString(R.string.step_format, 3, getString(R.string.step_permissions))
                    stepProgressIndicator.progress = 75
                    step1Container.visibility = View.GONE
                    step2Container.visibility = View.GONE
                    step3Container.visibility = View.VISIBLE
                    step4Container.visibility = View.GONE

                    btnBack.visibility = View.VISIBLE
                    btnNext.text = getString(R.string.btn_next)
                }
                4 -> {
                    tvStepSubtitle.text = getString(R.string.step_format, 4, getString(R.string.step_signature))
                    stepProgressIndicator.progress = 100
                    step1Container.visibility = View.GONE
                    step2Container.visibility = View.GONE
                    step3Container.visibility = View.GONE
                    step4Container.visibility = View.VISIBLE

                    btnBack.visibility = View.VISIBLE
                    btnNext.text = getString(R.string.btn_build)
                }
            }
        }

        btnBack.setOnClickListener {
            if (currentStep > 1) {
                currentStep--
                updateStepUI()
            }
        }

        btnNext.setOnClickListener {
            when (currentStep) {
                1 -> {
                    val appName = etAppName.text.toString().trim()
                    val packageName = etPackageName.text.toString().trim()
                    val versionName = etVersionName.text.toString().trim()

                    if (appName.isEmpty() || packageName.isEmpty() || versionName.isEmpty()) {
                        ToastUtil.showError(context, getString(R.string.error_fill_required))
                        return@setOnClickListener
                    }

                    if (!isValidPackageName(packageName)) {
                        ToastUtil.showError(
                            context,
                            getString(R.string.apk_build_error_invalide_package)
                        )
                        return@setOnClickListener
                    }

                    currentStep = 2
                    updateStepUI()
                }
                2 -> {
                    if (!cbArm64.isChecked && !cbArmv7.isChecked) {
                        ToastUtil.showError(context, getString(R.string.error_no_arch))
                        return@setOnClickListener
                    }
                    currentStep = 3
                    updateStepUI()
                }
                3 -> {
                    currentStep = 4
                    updateStepUI()
                }
                4 -> {
                    val appName = etAppName.text.toString().trim()
                    val packageName = etPackageName.text.toString().trim()
                    val versionName = etVersionName.text.toString().trim()
                    val versionCode = etVersionCode.text.toString().toIntOrNull() ?: 1

                    val selectedOrientation = when (toggleGroupOrientation.checkedButtonId) {
                        R.id.btnLandscape -> "landscape"
                        R.id.btnSensor -> "sensor"
                        else -> "portrait"
                    }

                    val template = if (availableTemplates.isNotEmpty() && selectedTemplateIndex in availableTemplates.indices) {
                        availableTemplates[selectedTemplateIndex]
                    } else {
                        null
                    }

                    if (template == null) {
                        ToastUtil.showError(context, getString(R.string.error_select_template))
                        return@setOnClickListener
                    }

                    val isCustomKeystore = rgKeystore.checkedRadioButtonId == R.id.rbCustomKeystore
                    val isGenerateNewKeystore = rgKeystore.checkedRadioButtonId == R.id.rbGenerateNew
                    val customAlias = etKeystoreAlias.text.toString().trim()
                    val customPass = etKeystorePass.text.toString().trim()

                    if (isGenerateNewKeystore) {
                        ToastUtil.showError(context, getString(R.string.error_key_generation_needed))
                        return@setOnClickListener
                    }

                    if (isCustomKeystore && (selectedCustomKeystoreFile == null || customAlias.isEmpty() || customPass.isEmpty())) {
                        ToastUtil.showError(context, getString(R.string.error_fill_keystore))
                        return@setOnClickListener
                    }

                    dialog.dismiss()

                    val excludedLibraries = mutableListOf<String>()
                    if (!switchPython.isChecked) excludedLibraries.add("python")
                    if (!switchQemu.isChecked) excludedLibraries.add("qemu")
                    if (!switchCompiler.isChecked) excludedLibraries.add("compiler")
                    if (!switchMkit.isChecked) excludedLibraries.add("mkit")
                    if (!switchOnnx.isChecked) excludedLibraries.add("onnx")
                    if (!switchMnn.isChecked) excludedLibraries.add("mnn")

                    val excludedArches = mutableListOf<String>()
                    if (!cbArm64.isChecked) excludedArches.add("arm64-v8a")
                    if (!cbArmv7.isChecked) excludedArches.add("armeabi-v7a")
                    excludedArches.add("x86")
                    excludedArches.add("x86_64")

                    val useManual = switchCustomizePermissions.isChecked
                    val internet = if (useManual) switchPermInternet.isChecked else scanResult.needsInternet
                    val storage = if (useManual) switchPermStorage.isChecked else scanResult.needsStorage
                    val camera = if (useManual) switchPermCamera.isChecked else scanResult.needsCamera
                    val audio = if (useManual) switchPermAudio.isChecked else scanResult.needsAudioRecord
                    val location = if (useManual) switchPermLocation.isChecked else scanResult.needsLocation
                    val bluetooth = if (useManual) switchPermBluetooth.isChecked else scanResult.needsBluetooth
                    val nfc = if (useManual) switchPermNfc.isChecked else scanResult.needsNfc
                    val vibrate = if (useManual) switchPermVibrate.isChecked else scanResult.needsVibrate
                    val overlay = if (useManual) switchPermOverlay.isChecked else scanResult.needsOverlay
                    val background = if (useManual) switchPermBackground.isChecked else scanResult.needsBackground

                    val allowedPermissions = mutableListOf<String>().apply {
                        add("android.permission.POST_NOTIFICATIONS")
                        if (internet) {
                            add("android.permission.INTERNET")
                            add("android.permission.ACCESS_NETWORK_STATE")
                            add("android.permission.ACCESS_WIFI_STATE")
                        }
                        if (storage) {
                            add("android.permission.READ_EXTERNAL_STORAGE")
                            add("android.permission.WRITE_EXTERNAL_STORAGE")
                            add("android.permission.MANAGE_EXTERNAL_STORAGE")
                            add("android.permission.READ_MEDIA_AUDIO")
                            add("android.permission.READ_MEDIA_VIDEO")
                            add("android.permission.READ_MEDIA_IMAGES")
                        }
                        if (camera) add("android.permission.CAMERA")
                        if (audio) add("android.permission.RECORD_AUDIO")
                        if (location) {
                            add("android.permission.ACCESS_FINE_LOCATION")
                            add("android.permission.ACCESS_COARSE_LOCATION")
                        }
                        if (bluetooth) {
                            add("android.permission.BLUETOOTH")
                            add("android.permission.BLUETOOTH_ADMIN")
                            add("android.permission.BLUETOOTH_SCAN")
                            add("android.permission.BLUETOOTH_CONNECT")
                            add("android.permission.BLUETOOTH_ADVERTISE")
                            add("android.permission.NEARBY_WIFI_DEVICES")
                        }
                        if (nfc) add("android.permission.NFC")
                        if (vibrate) add("android.permission.VIBRATE")
                        if (overlay) add("android.permission.SYSTEM_ALERT_WINDOW")
                        if (background) {
                            add("android.permission.FOREGROUND_SERVICE")
                            add("android.permission.FOREGROUND_SERVICE_DATA_SYNC")
                        }
                    }

                    executeApkBuildProcess(
                        appName, packageName, versionName, versionCode, selectedOrientation,
                        template, excludedLibraries, excludedArches,
                        isCustomKeystore, selectedCustomKeystoreFile, customAlias, customPass,
                        allowedPermissions
                    )
                }
            }
        }

        dialog.show()
    }

    private fun isValidPackageName(packageName: String): Boolean {
        val regex = "^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$".toRegex()
        if (!packageName.matches(regex)) {
            return false
        }

        val javaKeywords = setOf(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
            "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
            "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
            "volatile", "while", "true", "false", "null"
        )

        val segments = packageName.split(".")
        for (segment in segments) {
            if (javaKeywords.contains(segment)) {
                return false
            }
        }
        return true
    }

    private var tvSelectedKeystorePathTag: TextView? = null
    private val REQUEST_CODE_SELECT_KEYSTORE = 1002

    private fun processProjectIcon(sourceFile: File, destFile: File): Boolean {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = false
            }
            val originalBitmap = BitmapFactory.decodeFile(sourceFile.absolutePath, options) ?: return false

            val width = originalBitmap.width
            val height = originalBitmap.height

            val squareSize = Math.min(width, height)
            val xOffset = (width - squareSize) / 2
            val yOffset = (height - squareSize) / 2

            val squareBitmap = Bitmap.createBitmap(originalBitmap, xOffset, yOffset, squareSize, squareSize)

            val targetSize = 512
            val resizedBitmap = Bitmap.createScaledBitmap(squareBitmap, targetSize, targetSize, true)

            FileOutputStream(destFile).use { out ->
                resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            if (originalBitmap != resizedBitmap) originalBitmap.recycle()
            if (squareBitmap != resizedBitmap) squareBitmap.recycle()
            resizedBitmap.recycle()
            true
        } catch (e: Exception) {
            android.util.Log.e("ApkBuilder", "Failed to process launcher icon", e)
            false
        }
    }

    private fun executeApkBuildProcess(
        appName: String,
        packageName: String,
        versionName: String,
        versionCode: Int,
        orientation: String,
        template: TemplateManager.TemplateRelease,
        excludedLibraries: List<String>,
        excludedArches: List<String>,
        useCustomKeystore: Boolean,
        keystoreFile: File?,
        keystoreAlias: String?,
        keystorePass: String?,
        allowedPermissions: List<String>
    ) {
        val context = requireContext()
        val projectDir = project!!.directory

        showProgressDialog(getString(R.string.downloading_template))

        lifecycleScope.launch(Dispatchers.Main) {
            val downloadedTemplateApk = TemplateManager.downloadTemplate(
                context, template.downloadUrl, template.fileName
            ) { progress ->
                updateProgressDialogMessage(getString(R.string.downloading_template_progress, progress))
            }

            if (downloadedTemplateApk == null || !downloadedTemplateApk.exists()) {
                hideProgressDialog()
                ToastUtil.showError(context, getString(R.string.error_download_template))
                return@launch
            }

            updateProgressDialogMessage(getString(R.string.preparing_project))

            val projectZipFile = withContext(Dispatchers.IO) {
                val file = File(context.cacheDir, "project_clean.zip")
                ZipOutputStream(FileOutputStream(file)).use { zos ->
                    zipProjectWithoutSaves(projectDir, zos)
                }
                file
            }

            updateProgressDialogMessage(getString(R.string.preparing_apk_structure))

            val unsignedApkFile = File(context.cacheDir, "build_unsigned.apk")
            val finalApkFile = File(context.cacheDir, "${appName}_build.apk")

            withContext(Dispatchers.IO) {
                downloadedTemplateApk.copyTo(unsignedApkFile, overwrite = true)
            }

            updateProgressDialogMessage(getString(R.string.customizing_apk))

            val rawIconFile = getProjectIconFile(projectDir)
            var iconFile: File? = null

            if (rawIconFile != null && rawIconFile.exists()) {
                val processedIcon = File(context.cacheDir, "processed_icon.png")
                if (processProjectIcon(rawIconFile, processedIcon)) {
                    iconFile = processedIcon
                }
            }

            val buildOptions = ApkToolboxManager.BuildOptions(
                appName = appName,
                packageName = packageName,
                versionName = versionName,
                versionCode = versionCode,
                orientation = orientation,
                iconFile = iconFile,
                projectZipFile = projectZipFile,
                excludedArches = excludedArches,
                excludedLibraries = excludedLibraries,
                allowedPermissions = allowedPermissions
            )

            val customizationSuccess = withContext(Dispatchers.IO) {
                ApkToolboxManager.customizeApk(unsignedApkFile.absolutePath, buildOptions)
            }

            projectZipFile.delete()

            if (!customizationSuccess) {
                hideProgressDialog()
                ToastUtil.showError(context, getString(R.string.error_build_optimization))
                unsignedApkFile.delete()
                return@launch
            }

            updateProgressDialogMessage(getString(R.string.signing_apk))

            val signSuccess = withContext(Dispatchers.IO) {
                if (useCustomKeystore && keystoreFile != null) {
                    ApkToolboxManager.signApk(
                        context,
                        unsignedApkFile.absolutePath,
                        finalApkFile.absolutePath,
                        keystoreFile.absolutePath,
                        keystoreAlias,
                        keystorePass
                    )
                } else {
                    ApkToolboxManager.signApk(
                        context,
                        unsignedApkFile.absolutePath,
                        finalApkFile.absolutePath,
                        null, null, null
                    )
                }
            }

            unsignedApkFile.delete()

            hideProgressDialog()

            if (signSuccess && finalApkFile.exists()) {
                ToastUtil.showSuccess(context, getString(R.string.success_build))
                shareApkFile(finalApkFile, appName)
            } else {
                ToastUtil.showError(context, getString(R.string.error_signing_apk))
                finalApkFile.delete()
            }
        }
    }

    private fun shareApkFile(file: File, appName: String) {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            requireContext(),
            requireContext().packageName + ".fileProvider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.android.package-archive"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share_apk_title, appName)))
    }

    private fun zipProjectWithoutSaves(dir: File, zos: ZipOutputStream, basePath: String = "") {
        val ignoredFiles = listOf("DeviceVariables.json", "DeviceLists.json", "undo_code.xml")

        dir.listFiles()?.forEach { file ->
            val filePath = if (basePath.isEmpty()) file.name else "$basePath/${file.name}"

            if (ignoredFiles.contains(file.name)) {
                return@forEach
            }

            if (file.isDirectory) {
                val dirEntry = ZipEntry("$filePath/")
                zos.putNextEntry(dirEntry)
                zos.closeEntry()
                zipProjectWithoutSaves(file, zos, filePath)
            } else {
                FileInputStream(file).use { fis ->
                    zos.putNextEntry(ZipEntry(filePath))
                    fis.copyTo(zos)
                    zos.closeEntry()
                }
            }
        }
    }

    private fun updateProgressDialogMessage(message: String) {
        activity?.runOnUiThread {
            progressDialog?.setMessage(message)
        }
    }

    private fun parseServerError(errorJson: String?): String {
        if (errorJson.isNullOrEmpty()) return getString(R.string.po_unknown_server_error)
        return try {
            val json = JSONObject(errorJson)
            json.optString("detail", json.optString("message", getString(R.string.po_server_error)))
        } catch (e: Exception) {
            getString(R.string.po_server_parse_error)
        }
    }

    private fun startCommunityPublishFlow() {
        val context = requireContext()

        if (!CommunityTokenManager.isLoggedIn(context)) {
            showLoginRequiredCommunityDialog()
            return
        }

        val currentProject = project ?: return

        val iconFile = getProjectIconFile(currentProject.directory)
        if (iconFile == null) {
            AlertDialog.Builder(context, R.style.Theme_NewCatroid_Dialog)
                .setTitle(R.string.community_publish_error_no_cover_title)
                .setMessage(R.string.community_publish_error_no_cover)
                .setPositiveButton(R.string.ok, null)
                .show()
            return
        }

        val rawDescription = currentProject.description ?: ""
        val cleanDescription = rawDescription.replace(Regex("""!\[.*?\]\(.*?\)"""), "").trim()

        val dialogView = layoutInflater.inflate(R.layout.dialog_publish_preparation, null)

        val dialog = AlertDialog.Builder(context, R.style.Theme_NewCatroid_Dialog)
            .setView(dialogView)
            .create()

        val etTitle = dialogView.findViewById<TextInputEditText>(R.id.etPublishTitle)
        val etDesc = dialogView.findViewById<TextInputEditText>(R.id.etPublishDescription)
        val chipGroup = dialogView.findViewById<ChipGroup>(R.id.chipGroupPublishGenres)
        val btnSubmit = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSubmitPublish)
        val tvRulesLink = dialogView.findViewById<TextView>(R.id.tvPublishRulesLink)

        etTitle.setText(currentProject.name)
        etDesc.setText(cleanDescription)

        tvRulesLink.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://newcatroid.sois.site/market/documents"))
            startActivity(browserIntent)
        }

        genresList.forEach { genreName ->
            val chip = Chip(context).apply {
                val resourceName = "genre_${genreName.replace(" ", "_").replace("-", "_")}"
                val resId = context.resources.getIdentifier(resourceName, "string", context.packageName)
                text = if (resId != 0) context.getString(resId) else genreName

                isCheckable = true
                setChipBackgroundColorResource(R.color.button_background)
                setChipStrokeColorResource(R.color.accent)
                chipStrokeWidth = resources.displayMetrics.density * 1f
                setTextColor(context.resources.getColor(R.color.solid_white))

                tag = genreName
            }
            chipGroup.addView(chip)
        }

        btnSubmit.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val desc = etDesc.text.toString().trim()

            if (title.isEmpty()) {
                ToastUtil.showError(context, getString(R.string.po_title_empty))
                return@setOnClickListener
            }
            if (title.length > 30) {
                ToastUtil.showError(context, getString(R.string.po_title_too_long))
                return@setOnClickListener
            }
            if (desc.isEmpty()) {
                ToastUtil.showError(context, getString(R.string.po_desc_empty))
                return@setOnClickListener
            }
            if (desc.length > 400) {
                ToastUtil.showError(context, getString(R.string.po_desc_too_long))
                return@setOnClickListener
            }

            val selectedGenres = mutableListOf<String>()
            for (i in 0 until chipGroup.childCount) {
                val chip = chipGroup.getChildAt(i) as? Chip
                if (chip?.isChecked == true) {
                    selectedGenres.add(chip.tag as String)
                }
            }

            dialog.dismiss()
            executePublicationStages(title, desc, selectedGenres)
        }

        dialog.show()
    }

    private fun showLoginRequiredCommunityDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.community_publish_auth_required)
            .setMessage(R.string.community_publish_auth_desc)
            .setPositiveButton(R.string.login) { _, _ ->
                startActivity(Intent(requireContext(), org.catrobat.catroid.ui.CommunityLoginActivity::class.java))
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun executePublicationStages(title: String, description: String, genres: List<String>) {
        val currentProject = project ?: return
        showPublishPill(getString(R.string.community_stage_zipping))

        lifecycleScope.launch(Dispatchers.Main) {
            try {
                saveProject()

                val tempZipFile = withContext(Dispatchers.IO) {
                    val file = File(requireContext().cacheDir, "publish_temp_${System.currentTimeMillis()}.zip")
                    ZipOutputStream(FileOutputStream(file)).use { zos ->
                        zipDirectory3(currentProject.directory, zos)
                    }
                    file
                }

                Log.d("UploadCommunity", "Создан ZIP архив публикации. Путь: ${tempZipFile.absolutePath} | Размер: ${tempZipFile.length()} байт")

                if (tempZipFile.length() <= 0L) {
                    throw IOException(getString(R.string.po_zero_byte_archive))
                }

                updatePublishPillStatus(getString(R.string.community_stage_captcha))
                val siteKey = withContext(Dispatchers.IO) { fetchCaptchaSiteKey() }
                if (siteKey == null) {
                    hidePublishPill()
                    ToastUtil.showError(requireContext(), getString(R.string.community_error_config_failed))
                    tempZipFile.delete()
                    return@launch
                }

                verifyCaptchaForUpload(siteKey) { captchaToken: String? ->
                    if (captchaToken != null) {
                        performChunkedUpload(tempZipFile, title, description, genres, captchaToken)
                    } else {
                        hidePublishPill()
                        ToastUtil.showError(requireContext(), getString(R.string.community_captcha_cancelled))
                        tempZipFile.delete()
                    }
                }

            } catch (e: Exception) {
                hidePublishPill()
                ToastUtil.showError(requireContext(), getString(R.string.po_packaging_error, e.message))
            }
        }
    }

    private fun getProjectIconFile(projectDir: File): File? {
        val manualScreen = File(projectDir, "manual_screenshot.png")
        val autoScreen = File(projectDir, "automatic_screenshot.png")

        if (manualScreen.exists() && manualScreen.length() > 0L) return manualScreen
        if (autoScreen.exists() && autoScreen.length() > 0L) return autoScreen
        return null
    }

    private fun getProjectIconBase64(iconFile: File): String {
        return try {
            val bytes = iconFile.readBytes()
            val base64String = Base64.encodeToString(bytes, Base64.NO_WRAP)
            "data:image/png;base64,$base64String"
        } catch (e: Exception) {
            ""
        }
    }

    private fun performChunkedUpload(zipFile: File, title: String, description: String, genres: List<String>, captchaToken: String) {
        val currentProject = project ?: return

        val iconFile = getProjectIconFile(currentProject.directory)
        if (iconFile == null) {
            zipFile.delete()

            hidePublishPill()
            ToastUtil.showError(requireContext(), getString(R.string.community_publish_error_no_cover))
            return
        }

        val imageBase64 = getProjectIconBase64(iconFile)
        val token = CommunityTokenManager.getToken(requireContext()) ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val fileSize = zipFile.length()
                val jsonGenres = JSONArray()
                genres.forEach { jsonGenres.put(it) }

                val initPayload = JSONObject().apply {
                    put("title", title)
                    put("description", description)
                    put("genres", jsonGenres)
                    put("file_size", fileSize)
                    put("file_name", "${project!!.name}.newtrobat")
                    put("image_b64", imageBase64)
                    put("h-captcha-response", captchaToken)
                    put("size", fileSize)
                    put("quest_size_bytes", fileSize)
                    put("total_size", fileSize)
                }

                val jsonString = initPayload.toString()
                val initUrl = "https://backend.sois.site/games/upload/init"
                val initRequest = Request.Builder()
                    .url(initUrl)
                    .header("Authorization", "Bearer $token")
                    .post(jsonString.toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()

                httpClient.newCall(initRequest).execute().use { response ->
                    val responseBody = response.body?.string()
                    Log.d("UploadCommunity", "Init Response: $responseBody")

                    if (!response.isSuccessful) {
                        val serverMessage = parseServerError(responseBody)
                        throw IOException(serverMessage)
                    }

                    val initResponse = JSONObject(responseBody!!)
                    val uploadId = initResponse.getString("upload_id")
                    val chunkSize = initResponse.getInt("chunk_size")
                    Log.d("UploadCommunity", "Инициализация успешна. ID сессии: $uploadId | Размер чанка: $chunkSize байт")

                    uploadChunks(zipFile, uploadId, chunkSize, token) { progress ->
                        updatePublishPillStatus(getString(R.string.community_stage_uploading, progress))
                    }

                    updatePublishPillStatus(getString(R.string.community_stage_finishing))
                    val finishUrl = "https://backend.sois.site/games/upload/finish"

                    val multipartBodyBuilder = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("upload_id", uploadId)
                        .addFormDataPart("image", iconFile.name, iconFile.asRequestBody("image/png".toMediaType()))

                    val finishRequestBody = multipartBodyBuilder.build()
                    val finishRequest = Request.Builder()
                        .url(finishUrl)
                        .header("Authorization", "Bearer $token")
                        .post(finishRequestBody)
                        .build()

                    httpClient.newCall(finishRequest).execute().use { finishResponse ->
                        val finishResponseBody = finishResponse.body?.string()
                        zipFile.delete()

                        if (finishResponse.isSuccessful) {
                            showPublishPillSuccess()
                        } else {
                            val serverMessage = parseServerError(finishResponseBody)
                            throw IOException(serverMessage)
                        }
                    }
                }
            } catch (e: Exception) {
                zipFile.delete()
                withContext(Dispatchers.Main) {
                    hidePublishPill()
                    ToastUtil.showError(requireContext(), getString(R.string.common_error_with_message, e.message))
                }
            }
        }
    }

    private fun uploadChunks(file: File, uploadId: String, chunkSize: Int, token: String, onProgress: (Int) -> Unit) {
        val fileLength = file.length()
        val totalChunks = Math.ceil(fileLength.toDouble() / chunkSize).toInt()
        var lastReportedProgress = -1

        for (chunkIndex in 0 until totalChunks) {
            val offset = chunkIndex.toLong() * chunkSize
            val remaining = fileLength - offset
            val currentChunkSize = if (remaining < chunkSize) remaining else chunkSize.toLong()

            val progressRequestBody = object : RequestBody() {
                override fun contentType() = "application/octet-stream".toMediaType()
                override fun contentLength() = currentChunkSize

                override fun writeTo(sink: okio.BufferedSink) {
                    file.inputStream().use { inputStream ->
                        inputStream.skip(offset)
                        val buffer = ByteArray(8192)
                        var bytesToWrite = currentChunkSize
                        var chunkBytesWritten = 0L

                        while (bytesToWrite > 0) {
                            val toRead = if (bytesToWrite > buffer.size) buffer.size else bytesToWrite.toInt()
                            val read = inputStream.read(buffer, 0, toRead)
                            if (read == -1) break

                            sink.write(buffer, 0, read)

                            bytesToWrite -= read
                            chunkBytesWritten += read

                            val overallProgress = (((offset + chunkBytesWritten).toFloat() / fileLength) * 100).toInt()

                            if (overallProgress != lastReportedProgress) {
                                lastReportedProgress = overallProgress
                                onProgress(overallProgress.coerceIn(0, 100))
                            }
                        }
                    }
                }
            }

            val chunkRequest = Request.Builder()
                .url("https://backend.sois.site/games/upload/chunk")
                .header("Authorization", "Bearer $token")
                .header("X-Upload-Id", uploadId)
                .header("X-Chunk-Index", chunkIndex.toString())
                .post(progressRequestBody)
                .build()

            httpClient.newCall(chunkRequest).execute().use { response ->
                val responseBody = response.body?.string()
                if (!response.isSuccessful) {
                    val serverMessage = parseServerError(responseBody)
                    throw IOException(getString(R.string.po_chunk_error, chunkIndex, serverMessage))
                }
            }
        }
    }

    private fun showPublishPill(status: String) {
        activity?.runOnUiThread {
            binding.tvPublishProgressTitle.text = getString(R.string.po_publishing_project)
            binding.tvPublishProgressStatus.text = status
            binding.publishProgressBar.visibility = View.VISIBLE
            binding.publishProgressBar.isIndeterminate = true
            binding.btnDismissPublishNotification.visibility = View.GONE

            if (binding.publishProgressPill.visibility != View.VISIBLE) {
                binding.publishProgressPill.translationY = 300f
                binding.publishProgressPill.alpha = 0f
                binding.publishProgressPill.visibility = View.VISIBLE

                binding.publishProgressPill.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(400)
                    .setInterpolator(android.view.animation.OvershootInterpolator(1.1f))
                    .start()
            }
        }
    }

    private fun updatePublishPillStatus(status: String) {
        activity?.runOnUiThread {
            if (status.contains("%")) {
                val percentString = status.substringAfterLast(": ").substringBefore("%")
                val percent = percentString.toIntOrNull() ?: 0
                binding.publishProgressBar.isIndeterminate = false
                binding.publishProgressBar.progress = percent
            } else {
                binding.publishProgressBar.isIndeterminate = true
            }

            binding.tvPublishProgressStatus.text = status
        }
    }

    private fun showPublishPillSuccess() {
        activity?.runOnUiThread {
            binding.tvPublishProgressTitle.text = getString(R.string.community_stage_success)
            binding.tvPublishProgressStatus.text = getString(R.string.community_stage_success_desc)
            binding.publishProgressBar.visibility = View.GONE
            binding.btnDismissPublishNotification.visibility = View.VISIBLE

            binding.btnDismissPublishNotification.setOnClickListener {
                hidePublishPill()
            }
        }
    }

    private fun hidePublishPill() {
        activity?.runOnUiThread {
            if (binding.publishProgressPill.visibility == View.VISIBLE) {
                binding.publishProgressPill.animate()
                    .translationY(300f)
                    .alpha(0f)
                    .setDuration(300)
                    .setInterpolator(android.view.animation.AccelerateInterpolator())
                    .withEndAction {
                        binding.publishProgressPill.visibility = View.GONE
                    }
                    .start()
            }
        }
    }

    private suspend fun fetchCaptchaSiteKey(): String? {
        val request = Request.Builder().url("https://backend.sois.site/config").get().build()
        return withContext(Dispatchers.IO) {
            try {
                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: return@use null
                        JSONObject(body).getString("hcaptcha_site_key")
                    } else null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun verifyCaptchaForUpload(siteKey: String, onResult: (String?) -> Unit) {
        val config = HCaptchaConfig.builder()
            .siteKey(siteKey)
            .build()
        HCaptcha.getClient(requireActivity()).verifyWithHCaptcha(config)
            .addOnSuccessListener { response -> onResult(response.tokenResult) }
            .addOnFailureListener { onResult(null) }
    }

    private fun setupBakeOption() {
        val bakeBtn = view?.findViewById<android.widget.TextView>(R.id.project_options_bake)
        bakeBtn?.setOnClickListener {
            exportBakedProject()
        }
    }

    private fun exportBakedProject() {
        saveProject()
        project ?: return

        showProgressDialog(getString(R.string.po_baking_project))

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val tempDir = File(requireContext().cacheDir, "bake_temp")
                tempDir.deleteRecursively()
                tempDir.mkdirs()

                val baker = ProjectBaker(requireContext())
                val lunoCode = baker.bake(project!!)

                val initFile = File(tempDir, "init.bin")
                //LunoSecurity.saveEncrypted(initFile, lunoCode)

                initFile.writeText(lunoCode)



                val imagesDir = File(tempDir, "images")
                val soundsDir = File(tempDir, "sounds")
                imagesDir.mkdirs()
                soundsDir.mkdirs()
                val sourceDir = project!!.directory

                val foldersToCopy = listOf("files")

                for (folderName in foldersToCopy) {
                    val src = File(sourceDir, folderName)
                    val dest = File(tempDir, folderName)

                    if (src.exists()) {
                        src.copyRecursively(dest, overwrite = true)
                    } else {
                        dest.mkdirs()
                    }
                }

                project!!.sceneList.forEach { scene ->
                    scene.spriteList.forEach { sprite ->
                        sprite.lookList.forEach { look ->
                            val src = look.file
                            if (src != null && src.exists()) {
                                src.copyTo(File(imagesDir, src.name), overwrite = true)
                            }
                        }
                        sprite.soundList.forEach { sound ->
                            val src = sound.file
                            if (src != null && src.exists()) {
                                src.copyTo(File(soundsDir, src.name), overwrite = true)
                            }
                        }
                    }
                }

                val zipFile = File(requireContext().cacheDir, "${project!!.name}_baked.zip")
                ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                    zipDirectory3(tempDir, zos)
                }

                tempDir.deleteRecursively()

                withContext(Dispatchers.Main) {
                    hideProgressDialog()
                    shareFile(zipFile)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    hideProgressDialog()
                    ToastUtil.showError(requireContext(), getString(R.string.common_error_with_message, e.message))
                    e.printStackTrace()
                }
            }
        }
    }

    private fun shareFile(file: File) {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            requireContext(),
            requireContext().packageName + ".fileProvider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "application/zip"
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(intent, getString(R.string.po_save_baked)))
    }

    private fun setupRebuildCache() {
        binding.projectOptionsRebuildCache.setOnClickListener {
            showRebuildConfirmationDialog()
        }
    }

    private fun showRebuildConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.po_rebuild_physics_title)
            .setMessage(R.string.po_rebuild_physics_msg)
            .setPositiveButton(R.string.yes) { _, _ ->
                startCacheRebuilding()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun startCacheRebuilding() {
        project ?: return

        val allLooks = mutableListOf<LookData>()
        project!!.sceneList.forEach { scene ->
            scene.spriteList.forEach { sprite ->
                allLooks.addAll(sprite.lookList)
            }
        }

        if (allLooks.isEmpty()) {
            ToastUtil.showInfoLong(requireContext(), getString(R.string.po_no_looks_to_process))
            return
        }

        val progressDialog = ProgressDialog(requireContext()).apply {
            setTitle(R.string.po_cache_update_title)
            setMessage(getString(R.string.po_processing_images))
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            max = allLooks.size
            progress = 0
            setCancelable(false)
            show()
        }

        lifecycleScope.launch(Dispatchers.IO) {
            var successCount = 0
            allLooks.forEachIndexed { index, lookData ->
                withContext(Dispatchers.Main) {
                    progressDialog.setMessage(getString(R.string.po_processing_look, lookData.name))
                }

                val success = lookData.collisionInformation.forceRecalculateAndSave()
                if (success) {
                    successCount++
                }

                withContext(Dispatchers.Main) {
                    progressDialog.progress = index + 1
                }
            }

            withContext(Dispatchers.Main) {
                progressDialog.dismiss()
                ToastUtil.showSuccess(requireContext(), getString(R.string.po_processing_done, successCount, allLooks.size))
            }
        }
    }

    private fun setupGitButtons() {
        updateGitButtonsVisibility()

        binding.gitConnectButton.setOnClickListener { handleGitConnect() }
        binding.gitPublishButton.setOnClickListener { showCommitMessageDialog() }
        //binding.gitUpdateButton.setOnClickListener { handleGitUpdate() }
    }

    private fun updateGitButtonsVisibility() {
        val remoteUrl = project?.xmlHeader?.gitRemoteUrl
        val isConnected = !remoteUrl.isNullOrEmpty()

        binding.gitConnectButton.visibility = if (isConnected) View.GONE else View.VISIBLE
        binding.gitPublishButton.visibility = if (isConnected) View.VISIBLE else View.GONE
        //binding.gitUpdateButton.visibility = if (isConnected) View.VISIBLE else View.GONE
    }

    private fun handleGitConnect() {
        if (TokenManager.getToken(requireContext()) == null) {
            showLoginRequiredDialog()
            return
        }
        showGitConnectDialog()
    }

    private fun showLoginRequiredDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.po_login_required)
            .setMessage(R.string.po_login_required_msg)
            .setPositiveButton(R.string.po_go_to_settings) { _, _ ->
                startActivity(Intent(requireContext(), SettingsActivity::class.java))
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showGitConnectDialog() {
        val options = arrayOf(getString(R.string.po_create_new_repo), getString(R.string.po_connect_existing_repo))
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.po_connect_git_title)
            .setItems(options) { _, which ->
                if (which == 0) {
                    showCreateRepoDialog()
                } else {
                    showCloneRepoDialog()
                }
            }
            .show()
    }

    private fun showCreateRepoDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_repo, null)
        val repoNameEditText = dialogView.findViewById<TextInputEditText>(R.id.repo_name_edit_text)
        val privateSwitch = dialogView.findViewById<com.google.android.material.switchmaterial.SwitchMaterial>(R.id.repo_private_switch)

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.po_create_repo_title)
            .setView(dialogView)
            .setPositiveButton(R.string.common_create) { _, _ ->
                val repoName = repoNameEditText.text.toString().trim()
                if (repoName.isEmpty()) {
                    ToastUtil.showError(requireContext(), getString(R.string.po_repo_name_empty))
                    return@setPositiveButton
                }
                val isPrivate = privateSwitch.isChecked
                val token = TokenManager.getToken(requireContext()) ?: return@setPositiveButton

                showProgressDialog(getString(R.string.po_creating_repo))
                lifecycleScope.launch(Dispatchers.IO) {
                    val result = gitController.initializeAndPushNewRepository(token, repoName, isPrivate)
                    withContext(Dispatchers.Main) {
                        hideProgressDialog()
                        when (result) {
                            is GitResult.Success -> {
                                ToastUtil.showSuccess(requireContext(), getString(R.string.po_project_published))
                                project?.xmlHeader?.gitRemoteUrl = result.data
                                saveProject()
                                updateGitButtonsVisibility()
                            }
                            is GitResult.Error -> ToastUtil.showError(requireContext(), getString(R.string.common_error_with_message, result.message))
                            else -> {}
                        }
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showCloneRepoDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_clone_repo, null)
        val repoUrlEditText = dialogView.findViewById<TextInputEditText>(R.id.repo_url_edit_text)

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.po_connect_repo_title)
            .setView(dialogView)
            .setMessage(R.string.po_connect_repo_warning)
            .setPositiveButton(R.string.po_connect) { _, _ ->
                val repoUrl = repoUrlEditText.text.toString().trim()
                if (!repoUrl.startsWith("https://") || !repoUrl.endsWith(".git")) {
                    ToastUtil.showError(requireContext(), getString(R.string.po_invalid_repo_url))
                    return@setPositiveButton
                }
                val token = TokenManager.getToken(requireContext()) ?: return@setPositiveButton
                val originalProjectDir = project?.directory ?: return@setPositiveButton

                val tempDir = File(requireContext().cacheDir, "git_clone_temp_${System.currentTimeMillis()}")
                tempDir.mkdirs()

                showProgressDialog(getString(R.string.po_cloning_project))
                lifecycleScope.launch(Dispatchers.IO) {

                    val result = gitController.cloneRepository(repoUrl, token, tempDir)

                    var finalResult: GitResult<Unit> = GitResult.Error("Operation failed before replacement.")
                    if (result is GitResult.Success) {
                        try {

                            originalProjectDir.deleteRecursively()
                            if (!tempDir.renameTo(originalProjectDir)) {
                                throw IOException("Failed to replace project directory.")
                            }



                            val clonedProject = XstreamSerializer.getInstance().loadProject(originalProjectDir, requireContext())
                                ?: throw IOException("Failed to load cloned project from disk.")


                            clonedProject.xmlHeader.gitRemoteUrl = repoUrl
                            XstreamSerializer.getInstance().saveProject(clonedProject)


                            projectManager.currentProject = clonedProject
                            project = clonedProject

                            clonedProject.xmlHeader.gitRemoteUrl = repoUrl
                            XstreamSerializer.getInstance().saveProject(clonedProject)

                            finalResult = GitResult.Success(Unit)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            tempDir.deleteRecursively()
                            finalResult = GitResult.Error("Failed to replace or load project: ${e.message}")
                        }
                    } else {
                        finalResult = result
                    }

                    withContext(Dispatchers.Main) {
                        hideProgressDialog()
                        when (finalResult) {
                            is GitResult.Success -> {
                                ToastUtil.showSuccess(requireContext(), getString(R.string.po_project_cloned))
                                projectManager.loadProject(originalProjectDir)


                                projectManager.currentProject.xmlHeader.gitRemoteUrl = repoUrl

                                project = projectManager.currentProject
                                shouldSaveOnPause = true
                                showProjectReloadDialog()
                            }
                            is GitResult.Error -> {
                                tempDir.deleteRecursively()
                                ToastUtil.showError(requireContext(), getString(R.string.common_error_with_message, finalResult.message))
                            }
                            else -> {}
                        }
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showCommitMessageDialog() {
        val editText = TextInputEditText(requireContext())
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.po_sync_title)
            .setMessage(R.string.po_commit_msg_hint)
            .setView(editText)
            .setPositiveButton(R.string.common_start) { _, _ ->
                val commitMessage = editText.text.toString().ifEmpty { "Update project" }
                handleGitPublish(commitMessage)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private var shouldSaveOnPause = true

    private fun handleGitPublish(commitMessage: String) {
        val token = TokenManager.getToken(requireContext()) ?: return
        showProgressDialog(getString(R.string.po_sync_and_publish))
        lifecycleScope.launch(Dispatchers.IO) {
            saveProject()
            val result = gitController.commitAndPush(commitMessage, "NewCatroid_user", "nc_user@email.com", token)
            withContext(Dispatchers.Main) {
                hideProgressDialog()
                when (result) {
                    is GitResult.Success -> ToastUtil.showSuccess(requireContext(), getString(R.string.po_sync_done))
                    is GitResult.Error -> ToastUtil.showError(requireContext(), getString(R.string.common_error_with_message, result.message))
                    else -> {}
                }
            }
        }
    }

    private fun handleGitUpdate() {
        val token = TokenManager.getToken(requireContext()) ?: return
        showProgressDialog(getString(R.string.po_updating_project))
        lifecycleScope.launch(Dispatchers.IO) {
            val result = gitController.pullAndMerge(token)
            withContext(Dispatchers.Main) {
                hideProgressDialog()
                when (result) {
                    is GitResult.Success -> {
                        ToastUtil.showSuccess(requireContext(), getString(R.string.po_project_updated))
                        shouldSaveOnPause = false




                        val mergedProject = result.data.mergedProject
                        project = mergedProject
                        projectManager.currentProject = mergedProject


                        if (result.data.conflicts.isNotEmpty()) {
                            val conflictsString = result.data.conflicts.joinToString("\n") { "- ${it.path}" }
                            AlertDialog.Builder(requireContext())
                                .setTitle(R.string.po_conflicts_detected_title)
                                .setMessage(getString(R.string.po_conflicts_detected_msg, conflictsString))
                                .setPositiveButton("OK") { _, _ ->
                                    showProjectReloadDialog()
                                }
                                .show()
                        } else {
                            showProjectReloadDialog()
                        }
                    }
                    is GitResult.Error -> ToastUtil.showError(requireContext(), getString(R.string.common_error_with_message, result.message))
                    is GitResult.MergeConflict -> {
                        val conflictsString = result.conflicts.joinToString("\n") { "- ${it.fieldName}" }
                        AlertDialog.Builder(requireContext())
                            .setTitle(R.string.po_merge_conflicts_title)
                            .setMessage(getString(R.string.po_merge_conflicts_msg, conflictsString))
                            .setPositiveButton("OK", null)
                            .show()
                    }
                }
            }
        }
    }

    private fun showProgressDialog(message: String) {
        progressDialog = AlertDialog.Builder(requireContext())
            .setCancelable(false)
            .setView(R.layout.dialog_progress)
            .setMessage(message)
            .show()
    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
    }

    private fun showProjectReloadDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.po_restart_required_title)
            .setMessage(R.string.po_restart_required_msg)
            .setPositiveButton(R.string.button_restart) { _, _ ->

                requireActivity().finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun setupMishkFrede() {
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

        binding.projectOptionsDescriptionLayout.setEndIconOnClickListener {
            showImagePickerDialog()
        }
    }

    private fun showImagePickerDialog() {
        val projectDir = project?.filesDir ?: return
        val imageFiles = projectDir.listFiles { file ->
            file.isFile && file.extension.lowercase() in listOf("png", "jpg", "jpeg", "webp", "gif")
        }

        if (imageFiles.isNullOrEmpty()) {
            ToastUtil.showInfoLong(requireContext(), getString(R.string.po_no_images_in_project))
            return
        }

        val imageFileNames = imageFiles.map { it.name }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.po_insert_image)
            .setItems(imageFileNames) { _, which ->
                val selectedFileName = imageFileNames[which]
                insertImageMarkdown(selectedFileName)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun insertImageMarkdown(fileName: String) {
        val editText = binding.projectOptionsDescriptionLayout.editText ?: return
        val markdownToInsert = "\n![${fileName}]($fileName)\n"

        val cursorPosition = editText.selectionStart
        editText.text?.insert(cursorPosition, markdownToInsert)
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
        binding.projectOptionsUpload.setOnClickListener {
            exportMatryoshkaForServer()
        }
    }

    private fun exportMatryoshkaForServer() {
        saveProject()
        val currentProject = project ?: return

        showProgressDialog(getString(R.string.po_build_matryoshka))

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val readyToUploadZip = org.catrobat.catroid.utils.MatryoshkaManager.packForUpload(requireContext(), currentProject)

                withContext(Dispatchers.Main) {
                    hideProgressDialog()
                    ToastUtil.showSuccess(requireContext(), getString(R.string.po_matryoshka_done))
                    shareFile(readyToUploadZip)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    hideProgressDialog()
                    ToastUtil.showError(requireContext(), getString(R.string.common_error_with_message, e.message))
                    e.printStackTrace()
                }
            }
        }
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
        val currentProject = project ?: return

        org.catrobat.catroid.utils.ProjectTrashManager.showDeleteProjectDialog(
            requireContext(),
            currentProject.directory
        ) {
            project = null
            projectManager.currentProject = null
            requireActivity().onBackPressed()
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        for (index in 0 until menu.size()) {
            menu.getItem(index).isVisible = false
        }
        super.onPrepareOptionsMenu(menu)
    }

    override fun onPause() {
        if (shouldSaveOnPause) {
            saveProject()
        }
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

    fun copyInputStreamToFile(context: Context, inputStream: InputStream, outputFile: File) {
        val outputStream = FileOutputStream(outputFile)
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
    }

    fun createApkFromTemplate(context: Context, projectZipFile: File): File {

        val assetFile = "apk_template.zip"


        val tempDir = File(context.cacheDir, "apk_temp")
        tempDir.mkdirs()


        unzip(context.assets.open(assetFile), tempDir)


        val projectFile = File(tempDir, "assets/project.zip")
        projectZipFile.copyTo(projectFile, overwrite = true)


        val unsignedApkFile = File(context.cacheDir, "unsigned_project_build.apk")
        ZipOutputStream(FileOutputStream(unsignedApkFile)).use { zos ->
            zipDirectory3(tempDir, zos)
        }


        tempDir.deleteRecursively()


        val signedApkFile = File(context.cacheDir, "signed_project_build.apk")
        val keystoreInputStream = CatroidApplication.getAppContext().assets.open("debug.jks")
        val outputFile = File(context.filesDir, "debug.jks")

        copyInputStreamToFile(CatroidApplication.getAppContext(), keystoreInputStream, outputFile)
        signApkWithApksig(context, unsignedApkFile, signedApkFile, "debug.p12", "keystore", "dbg", "keystore")


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

            val keystoreFile = File(context.filesDir, keystoreAssetName)
            if (!keystoreFile.exists()) {
                copyKeystoreFromAssets(context, keystoreAssetName, keystoreFile)
            }


            val keyStore = KeyStore.getInstance("PKCS12").apply {
                load(FileInputStream(keystoreFile), keystorePassword.toCharArray())
            }


            val privateKey = keyStore.getKey(keyAlias, keyPassword.toCharArray()) as PrivateKey


            val certificates = keyStore.getCertificateChain(keyAlias)
                .map { it as X509Certificate }
                .toList()


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
            throw RuntimeException(getString(R.string.po_apk_signing_error, e.message), e)
        }
    }


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



        dir.listFiles()?.forEach { file ->
            val filePath = if (basePath.isEmpty()) file.name else "$basePath/${file.name}"

            if (file.isDirectory) {


                val dirEntry = ZipEntry("$filePath/")
                zos.putNextEntry(dirEntry)
                zos.closeEntry()


                zipDirectory3(file, zos, filePath)
            } else {

                FileInputStream(file).use { fis ->
                    zos.putNextEntry(ZipEntry(filePath))
                    fis.copyTo(zos)
                    zos.closeEntry()
                }
            }
        }
    }


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

            Log.e("ShowToast", "messageHandler is null!")
        }
    }

    fun getRandomMessage(): String {
        val messages = resources.getStringArray(R.array.po_success_messages)
        val randomIndex = Random.nextInt(messages.size)
        return messages[randomIndex]
    }

    fun getRandomError(): String {
        val errorMessages = resources.getStringArray(R.array.po_error_messages)
        val randomIndex = Random.nextInt(errorMessages.size)
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


            val oldIconFile = File(directory, "automatic_screenshot.png")
            val newIconFile = File(directory, "manual_screenshot.png")


            if (oldIconFile.exists()) {
                oldIconFile.delete()
            }


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


            val variablesDeleted = deviceVariablesFile.delete()
            val listsDeleted = deviceListsFile.delete()


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

                if(file.name != "undo_code.xml") {
                    val zipEntry = ZipEntry(file.relativeTo(sourceDir).path)
                    zipOut.putNextEntry(zipEntry)


                    if (file.isFile) {
                        FileInputStream(file).use { fis ->
                            fis.copyTo(zipOut)
                        }
                    }

                    zipOut.closeEntry()
                }
            }
        }

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


            val zipFile = File(tempDir, "project.zip")


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


                val zipFile = File(tempDir, "project.zip")


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

                    val inputStream = contentResolver.openInputStream(uri)
                    val outputStream = FileOutputStream(File(project?.directory, "manual_screenshot.png"))


                    inputStream?.copyTo(outputStream)

                    inputStream?.close()
                    outputStream.close()

                    showToast(getRandomMessage())
                } catch (e: Exception) {
                    showToast(getString(R.string.po_image_save_error, e.message))
                }
            } ?: showToast(getRandomError())
        }
        if (requestCode == REQUEST_CODE_SELECT_KEYSTORE && resultCode == Activity.RESULT_OK) {
            data.data?.let { uri ->
                try {
                    val tempKeystore = File(requireContext().cacheDir, "custom_user_signing.jks")
                    tempKeystore.parentFile?.mkdirs()

                    requireContext().contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(tempKeystore).use { output ->
                            input.copyTo(output)
                        }
                    }

                    selectedCustomKeystoreFile = tempKeystore
                    tvSelectedKeystorePathTag?.text = getString(R.string.keystore_loaded)
                    ToastUtil.showSuccess(requireContext(), getString(R.string.keystore_loaded))
                } catch (e: Exception) {
                    tvSelectedKeystorePathTag?.text = getString(R.string.error_loading_key)
                    ToastUtil.showError(requireContext(), getString(R.string.error_load_keystore, e.message))
                }
            }
        }

        if (requestCode == REQUEST_CODE_GENERATE_KEYSTORE && resultCode == Activity.RESULT_OK) {
            val destinationUri = data.data ?: return
            val alias = tempGenAlias ?: "debug"
            val pass = tempGenPass ?: "android"
            val ownerName = tempGenOwner ?: "NewCatroid Owner"

            try {
                val tempGeneratedKeystore = File(requireContext().cacheDir, "user_generated_keystore.p12")
                if (tempGeneratedKeystore.exists()) tempGeneratedKeystore.delete()

                val generateSuccess = ApkToolboxManager.generateKeyStore(
                    tempGeneratedKeystore.absolutePath, alias, pass, ownerName
                )

                if (generateSuccess && tempGeneratedKeystore.exists()) {
                    requireContext().contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                        FileInputStream(tempGeneratedKeystore).use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                    selectedCustomKeystoreFile = tempGeneratedKeystore

                    activeDialogView?.let { view ->
                        val rgKeystore = view.findViewById<RadioGroup>(R.id.rgKeystore)
                        val tvSelectedKeystorePath = view.findViewById<TextView>(R.id.tvSelectedKeystorePath)
                        val etKeystoreAlias = view.findViewById<TextInputEditText>(R.id.etKeystoreAlias)
                        val etKeystorePass = view.findViewById<TextInputEditText>(R.id.etKeystorePass)

                        view.findViewById<TextInputEditText>(R.id.etGenAlias)?.setText("")
                        view.findViewById<TextInputEditText>(R.id.etGenPass)?.setText("")
                        view.findViewById<TextInputEditText>(R.id.etGenOwner)?.setText("")

                        etKeystoreAlias?.setText(alias)
                        etKeystorePass?.setText(pass)
                        tvSelectedKeystorePath?.text = getString(R.string.generated_path_label)

                        rgKeystore?.check(R.id.rbCustomKeystore)
                    }

                    ToastUtil.showSuccess(requireContext(), getString(R.string.success_key_generated))
                } else {
                    ToastUtil.showError(requireContext(), getString(R.string.error_key_failed))
                }
            } catch (e: Exception) {
                ToastUtil.showError(requireContext(), getString(R.string.common_error_with_message, e.message))
            } finally {
                tempGenAlias = null
                tempGenPass = null
                tempGenOwner = null
            }
        }
    }

    fun copyFileToUri(context: Context, sourceFile: File, directoryUri: Uri, fileName: String) {

        val resolver: ContentResolver = context.contentResolver


        val fileUri = Uri.withAppendedPath(directoryUri, fileName)


        resolver.openOutputStream(fileUri)?.use { outputStream: OutputStream ->

            FileInputStream(sourceFile).use { inputStream ->

                inputStream.copyTo(outputStream)
            }
        } ?: run {

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
        private const val REQUEST_CODE_GENERATE_KEYSTORE = 1003
    }
}
