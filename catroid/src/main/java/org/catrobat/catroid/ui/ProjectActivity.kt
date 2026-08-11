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
package org.catrobat.catroid.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.danvexteam.lunoscript_annotations.LunoClass
import org.catrobat.catroid.BuildConfig
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.R
import org.catrobat.catroid.common.Constants
import org.catrobat.catroid.common.Constants.DEFAULT_IMAGE_EXTENSION
import org.catrobat.catroid.common.Constants.TMP_IMAGE_FILE_NAME
import org.catrobat.catroid.common.FlavoredConstants
import org.catrobat.catroid.common.FlavoredConstants.LIBRARY_BACKGROUNDS_URL_LANDSCAPE
import org.catrobat.catroid.common.FlavoredConstants.LIBRARY_BACKGROUNDS_URL_PORTRAIT
import org.catrobat.catroid.common.FlavoredConstants.LIBRARY_LOOKS_URL
import org.catrobat.catroid.common.FlavoredConstants.LIBRARY_SOUNDS_URL
import org.catrobat.catroid.common.LookData
import org.catrobat.catroid.common.SharedPreferenceKeys
import org.catrobat.catroid.common.SoundInfo
import org.catrobat.catroid.content.MyActivityManager
import org.catrobat.catroid.content.Project
import org.catrobat.catroid.content.StartScript
import org.catrobat.catroid.content.bricks.Brick
import org.catrobat.catroid.content.bricks.PlaceAtBrick
import org.catrobat.catroid.content.bricks.PointInDirectionBrick
import org.catrobat.catroid.content.bricks.SetSizeToBrick
import org.catrobat.catroid.content.bricks.VisualPlacementBrick
import org.catrobat.catroid.databinding.ActivityRecyclerBinding
import org.catrobat.catroid.databinding.DialogNewActorBinding
import org.catrobat.catroid.databinding.ProgressBarBinding
import org.catrobat.catroid.editor.EditorActivity
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.UserData
import org.catrobat.catroid.formulaeditor.UserList
import org.catrobat.catroid.formulaeditor.UserVariable
import org.catrobat.catroid.io.StorageOperations
import org.catrobat.catroid.io.asynctask.ProjectSaver
import org.catrobat.catroid.libraries.LibraryManager
import org.catrobat.catroid.merge.ImportProjectHelper
import org.catrobat.catroid.pocketmusic.PocketMusicActivity
import org.catrobat.catroid.soundrecorder.SoundRecorderActivity
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.stage.TestResult
import org.catrobat.catroid.ui.BottomBar.showBottomBar
import org.catrobat.catroid.ui.SpriteActivity.LOOK_CAMERA
import org.catrobat.catroid.ui.SpriteActivity.LOOK_FILE
import org.catrobat.catroid.ui.SpriteActivity.LOOK_LIBRARY
import org.catrobat.catroid.ui.SpriteActivity.LOOK_POCKET_PAINT
import org.catrobat.catroid.ui.SpriteActivity.SOUND_FILE
import org.catrobat.catroid.ui.SpriteActivity.SOUND_LIBRARY
import org.catrobat.catroid.ui.SpriteActivity.SOUND_RECORD
import org.catrobat.catroid.ui.controller.ActorsAndObjectsManager
import org.catrobat.catroid.ui.controller.BackpackListManager
import org.catrobat.catroid.ui.dialogs.LegoSensorConfigInfoDialog
import org.catrobat.catroid.ui.fragment.ProjectFilesFragment
import org.catrobat.catroid.ui.fragment.ProjectLibsFragment
import org.catrobat.catroid.ui.fragment.ProjectOptionsFragment
import org.catrobat.catroid.ui.recyclerview.backpack.ActorAndObjectActivity
import org.catrobat.catroid.ui.recyclerview.backpack.BackpackActivity
import org.catrobat.catroid.ui.recyclerview.controller.SceneController
import org.catrobat.catroid.ui.recyclerview.dialog.NewSpriteDialogFragment
import org.catrobat.catroid.ui.recyclerview.dialog.TextInputDialog
import org.catrobat.catroid.ui.recyclerview.dialog.textwatcher.DuplicateInputTextWatcher
import org.catrobat.catroid.ui.recyclerview.fragment.ProjectListFragment
import org.catrobat.catroid.ui.recyclerview.fragment.ProjectListFragment.Companion
import org.catrobat.catroid.ui.recyclerview.fragment.RecyclerViewFragment
import org.catrobat.catroid.ui.recyclerview.fragment.SceneListFragment
import org.catrobat.catroid.ui.recyclerview.fragment.SpriteListFragment
import org.catrobat.catroid.ui.recyclerview.util.UniqueNameProvider
import org.catrobat.catroid.ui.settingsfragments.SettingsFragment
import org.catrobat.catroid.utils.ToastUtil
import org.catrobat.catroid.utils.Utils
import org.catrobat.catroid.utils.setVisibleOrGone
import org.catrobat.catroid.visualplacement.VisualPlacementActivity
import org.koin.android.ext.android.inject
import java.io.File
import java.io.IOException

@LunoClass
class ProjectActivity : BaseCastActivity() {

    companion object {
        const val EXTRA_FRAGMENT_POSITION = "fragmentPosition"

        const val FRAGMENT_SCENES = 0
        const val FRAGMENT_SPRITES = 1

        const val SPRITE_POCKET_PAINT = 0
        const val SPRITE_LIBRARY = 1
        const val SPRITE_FILE = 2
        const val SPRITE_CAMERA = 3
        const val SPRITE_OBJECT = 4
        const val SPRITE_FROM_LOCAL = 5
    }

    private lateinit var binding: ActivityRecyclerBinding
    private val projectManager: ProjectManager by inject()

    private var workspaceLayout: org.catrobat.catroid.ui.workspace.WorkspaceLayout? = null
    private var generatedVariableName: String? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (projectManager.currentProject == null) {
            Log.e("ProjectActivity", "Project is null during onCreate. Redirecting to ProjectListActivity.")
            val intent = Intent(this, ProjectListActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            finish()
            return
        }

        binding = ActivityRecyclerBinding.inflate(layoutInflater)
        if (isFinishing) {
            return
        }
        setContentView(binding.root)

        workspaceLayout = findViewById(R.id.workspace_layout)
        val isWorkspaceEnabled = PreferenceManager.getDefaultSharedPreferences(this)
            .getBoolean("pref_workspace_project_level", false)

        if (isWorkspaceEnabled) {
            if (projectManager.currentlyEditedScene == null) {
                projectManager.currentlyEditedScene = projectManager.currentProject?.defaultScene
            }
            if (projectManager.currentSprite == null) {
                projectManager.currentSprite = projectManager.currentlyEditedScene?.backgroundSprite
            }

            binding.fragmentContainer.visibility = View.GONE
            binding.bottomBar.root.visibility = View.GONE
            binding.toolbar.toolbar.visibility = View.GONE
            workspaceLayout?.visibility = View.VISIBLE

            val creators = mapOf(
                SceneListFragment.TAG to Pair(getString(R.string.tab_scenes), { SceneListFragment() }),
                SpriteListFragment.TAG to Pair(getString(R.string.tab_sprites), { SpriteListFragment() }),
                org.catrobat.catroid.ui.recyclerview.fragment.ScriptFragment.TAG to Pair(getString(R.string.tab_scripts), { org.catrobat.catroid.ui.recyclerview.fragment.ScriptFragment() }),
                org.catrobat.catroid.ui.recyclerview.fragment.LookListFragment.TAG to Pair(getString(R.string.tab_looks), { org.catrobat.catroid.ui.recyclerview.fragment.LookListFragment() }),
                org.catrobat.catroid.ui.recyclerview.fragment.SoundListFragment.TAG to Pair(getString(R.string.tab_sounds), { org.catrobat.catroid.ui.recyclerview.fragment.SoundListFragment() })
            )

            workspaceLayout?.restoreLayoutState(creators)

            workspaceLayout?.post {
                val prefs = getSharedPreferences("workspace_prefs", Context.MODE_PRIVATE)
                val active = prefs.getString("active_tags", "") ?: ""
                if (active.isEmpty()) {
                    workspaceLayout?.openWindow(SpriteListFragment.TAG, getString(R.string.tab_sprites)) { SpriteListFragment() }
                }
            }

        } else {
            workspaceLayout?.visibility = View.GONE
            setSupportActionBar(binding.toolbar.toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            setSupportActionBar(binding.toolbar.toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)

            var fragmentPosition = FRAGMENT_SCENES
            val bundle = intent.extras
            if (bundle != null) {
                fragmentPosition = bundle.getInt(EXTRA_FRAGMENT_POSITION, FRAGMENT_SCENES)
            }
            loadFragment(fragmentPosition)
        }
        showWarningForSuspiciousBricksOnce(this)
        showLegoSensorConfigInfo()
        binding.bottomBar.apply {
            buttonAdd.setOnClickListener {
                handleAddButton()
            }
            buttonPlay.setOnClickListener {
                handlePlayButton()
            }
            //buttonAiAssist.setOnClickListener {
            //    handleAiButton()
            //}
        }
        projectManager.currentProject.checkIfSpriteNameEqualBackground(this)
        MyActivityManager.project_activity = this

        val loadedProject = projectManager.currentProject
        if (loadedProject != null) {
            Log.i(ProjectListFragment.TAG, "Проект '${loadedProject.name}' загружен, запускаю синхронизацию библиотек...")
            LibraryManager.syncAndLoadLibraries(loadedProject)

            if (projectManager.isNeedsPhysicsCacheWarning) {
                showPhysicsCacheWarningDialog()
                projectManager.isNeedsPhysicsCacheWarning = false
            }
        } else {
            Log.w(ProjectListFragment.TAG, "Проект успешно загружен, но projectManager.currentProject равен null. Синхронизация пропущена.")
        }
    }

    private fun loadFragment(fragmentPosition: Int) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        when (fragmentPosition) {
            FRAGMENT_SCENES -> fragmentTransaction.replace(
                R.id.fragment_container,
                SceneListFragment(),
                SceneListFragment.TAG
            )
            FRAGMENT_SPRITES -> fragmentTransaction.replace(
                R.id.fragment_container,
                SpriteListFragment(),
                SpriteListFragment.TAG
            )
            else -> throw IllegalArgumentException("Invalid fragmentPosition in Activity.")
        }
        fragmentTransaction.commit()
    }

    private val currentFragment: Fragment?
        get() {
            val workspace = workspaceLayout
            if (workspace != null && workspace.visibility == View.VISIBLE) {
                val spriteFragment = supportFragmentManager.findFragmentByTag(SpriteListFragment.TAG)
                if (spriteFragment != null) {
                    return spriteFragment
                }
                val sceneFragment = supportFragmentManager.findFragmentByTag(SceneListFragment.TAG)
                if (sceneFragment != null) {
                    return sceneFragment
                }
            }

            return supportFragmentManager.findFragmentById(R.id.fragment_container)
        }

    fun setShowProgressBar(show: Boolean) {
        ProgressBarBinding.inflate(layoutInflater).progressBar.setVisibleOrGone(show)
        binding.fragmentContainer.setVisibleOrGone(!show)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_project_activity, menu)
        menu.findItem(R.id.from_library).isVisible = false
        menu.findItem(R.id.from_local).isVisible = false
        menu.findItem(R.id.edit).isVisible = false
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val workspace = workspaceLayout
        val isWorkspaceMode = workspace != null && workspace.visibility == View.VISIBLE

        when (item.itemId) {
            R.id.new_scene -> handleAddSceneButton()
            R.id.project_options ->
                if (isWorkspaceMode) {
                    workspace?.openWindow(ProjectOptionsFragment.TAG, getString(R.string.tab_project_options)) { ProjectOptionsFragment() }
                } else {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, ProjectOptionsFragment(), ProjectOptionsFragment.TAG)
                        .addToBackStack(ProjectOptionsFragment.TAG).commit()
                }
            R.id.project_files ->
                if (isWorkspaceMode) {
                    workspace?.openWindow(ProjectFilesFragment.TAG, getString(R.string.tab_project_files)) { ProjectFilesFragment() }
                } else {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, ProjectFilesFragment(), ProjectFilesFragment.TAG)
                        .addToBackStack(ProjectFilesFragment.TAG).commit()
                }
            R.id.project_libs ->
                if (isWorkspaceMode) {
                    workspace?.openWindow(ProjectLibsFragment.TAG, getString(R.string.tab_libraries)) { ProjectLibsFragment() }
                } else {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, ProjectLibsFragment(), ProjectLibsFragment.TAG)
                        .addToBackStack(ProjectLibsFragment.TAG).commit()
                }
            R.id.editor3d -> {
                val intent = Intent(this, EditorActivity::class.java)
                startActivity(intent)
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onPause() {
        super.onPause()
        if (workspaceLayout?.visibility == View.VISIBLE) {
            workspaceLayout?.saveLayoutState()
        }
        saveProject(projectManager.currentProject)
    }

    override fun onBackPressed() {
        if (workspaceLayout?.visibility == View.VISIBLE) {
            if (workspaceLayout?.handleBackPressed() == true) {
                return
            }
        }

        val currentProject = projectManager.currentProject
        if (currentProject == null) {
            finish()
            return
        }
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (currentFragment !is ProjectOptionsFragment) {
            saveProject(currentProject)
        }
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
            showBottomBar(this)
            return
        } else {
            LibraryManager.unloadAllLibraries()

            projectManager.resetProjectManager()
        }
        val multiSceneProject = projectManager.currentProject.sceneList.size > 1
        if (currentFragment is SpriteListFragment && multiSceneProject) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SceneListFragment(), SceneListFragment.TAG)
                .commit()
        } else {
            super.onBackPressed()
        }
    }

    private fun saveProject(currentProject: Project?) {
        if (currentProject == null) {
            Utils.setLastUsedProjectName(applicationContext, null)
            return
        }
        ProjectSaver(currentProject, applicationContext).saveProjectAsync()
        Utils.setLastUsedProjectName(applicationContext, currentProject.name)
    }

    private fun updateBrickFromVisualPlacement(extras: Bundle?) {
        if (extras == null) return

        val xCoordinate = extras.getInt(VisualPlacementActivity.X_COORDINATE_BUNDLE_ARGUMENT)
        val yCoordinate = extras.getInt(VisualPlacementActivity.Y_COORDINATE_BUNDLE_ARGUMENT)
        val rotation = extras.getFloat(VisualPlacementActivity.ROTATION_ANGLE_BUNDLE_ARGUMENT)
        val size = extras.getFloat(VisualPlacementActivity.SIZE_PERCENT_BUNDLE_ARGUMENT)
        val brickHash = extras.getInt(SpriteActivity.EXTRA_BRICK_HASH, -1)

        var fragment: Fragment? = supportFragmentManager.findFragmentByTag(org.catrobat.catroid.ui.fragment.FormulaEditorFragment.FORMULA_EDITOR_FRAGMENT_TAG)
        if (fragment == null) {
            fragment = supportFragmentManager.findFragmentByTag(org.catrobat.catroid.ui.recyclerview.fragment.ScriptFragment.TAG)
        }

        var brick: Brick? = null
        if (fragment is org.catrobat.catroid.ui.fragment.FormulaEditorFragment) {
            brick = fragment.formulaBrick
        } else if (fragment is org.catrobat.catroid.ui.recyclerview.fragment.ScriptFragment) {
            brick = fragment.findBrickByHash(brickHash)
        }

        if (brick is VisualPlacementBrick) {
            val visualBrick = brick as VisualPlacementBrick
            val parentScript = visualBrick.script

            visualBrick.setCoordinates(xCoordinate, yCoordinate)

            val currentSprite = projectManager.currentSprite
            val isNotBackground = currentSprite != projectManager.currentlyEditedScene.backgroundSprite

            if (isNotBackground && parentScript != null) {
                val isVariable = extras.getBoolean(VisualPlacementActivity.EXTRA_IS_VARIABLE, false)
                val rotationChanged = extras.getBoolean(VisualPlacementActivity.EXTRA_ROTATION_CHANGED, false)
                val sizeChanged = extras.getBoolean(VisualPlacementActivity.EXTRA_SIZE_CHANGED, false)

                if (!isVariable) {
                    var existingDirectionBrick: PointInDirectionBrick? = null
                    var existingSizeBrick: SetSizeToBrick? = null

                    for (b in parentScript.brickList) {
                        if (b is PointInDirectionBrick) {
                            existingDirectionBrick = b
                        }
                        if (b is SetSizeToBrick) {
                            existingSizeBrick = b
                        }
                    }

                    var insertionPoint = parentScript.brickList.indexOf(visualBrick) + 1

                    if (existingDirectionBrick != null) {
                        if (rotationChanged) {
                            val index = parentScript.brickList.indexOf(existingDirectionBrick)
                            parentScript.brickList.set(index, PointInDirectionBrick(Formula(rotation + 90)))
                        }
                    } else {
                        if (rotationChanged) {
                            parentScript.addBrick(insertionPoint++, PointInDirectionBrick(Formula(rotation + 90)))
                        }
                    }

                    if (existingSizeBrick != null) {
                        if (sizeChanged) {
                            val index = parentScript.brickList.indexOf(existingSizeBrick)
                            parentScript.brickList.set(index, SetSizeToBrick(Formula(size)))
                        }
                    } else {
                        if (sizeChanged) {
                            parentScript.addBrick(insertionPoint, SetSizeToBrick(Formula(size)))
                        }
                    }
                }
            }

            if (fragment is org.catrobat.catroid.ui.recyclerview.fragment.ScriptFragment) {
                fragment.notifyDataSetChanged()
            } else if (fragment is org.catrobat.catroid.ui.fragment.FormulaEditorFragment) {
                fragment.updateFragmentAfterVisualPlacement()
            }
        }
    }

    private fun addBackgroundFromUri(uri: Uri?, imageExtension: String = DEFAULT_IMAGE_EXTENSION) {
        if (uri == null) return
        val currentSprite = projectManager.currentSprite ?: return
        val currentScene = projectManager.currentlyEditedScene ?: return
        val resolvedFileName = StorageOperations.resolveFileName(contentResolver, uri)
        val lookDataName: String
        val lookFileName: String

        val useSpriteName = resolvedFileName == null ||
                StorageOperations.getSanitizedFileName(resolvedFileName) == TMP_IMAGE_FILE_NAME

        if (useSpriteName) {
            lookDataName = currentSprite.name
            lookFileName = lookDataName + imageExtension
        } else {
            lookDataName = StorageOperations.getSanitizedFileName(resolvedFileName)
            lookFileName = resolvedFileName
        }

        val finalLookDataName = UniqueNameProvider().getUniqueNameInNameables(lookDataName, currentSprite.lookList)

        try {
            val imageDirectory = File(currentScene.directory, Constants.IMAGE_DIRECTORY_NAME)
            val file = StorageOperations.copyUriToDir(contentResolver, uri, imageDirectory, lookFileName)
            Utils.removeExifData(imageDirectory, lookFileName)
            val look = LookData(finalLookDataName, file)
            currentSprite.lookList.add(look)
            look.collisionInformation?.calculate()

            val lookFragment = supportFragmentManager.findFragmentByTag(org.catrobat.catroid.ui.recyclerview.fragment.LookListFragment.TAG)
                    as? org.catrobat.catroid.ui.recyclerview.fragment.LookListFragment
            lookFragment?.notifyDataSetChanged()
        } catch (e: IOException) {
            Log.e("ProjectActivity", Log.getStackTraceString(e))
        }
    }

    private fun addLookFromUri(uri: Uri?, imageExtension: String = DEFAULT_IMAGE_EXTENSION) {
        if (uri == null) return
        val currentSprite = projectManager.currentSprite ?: return
        val currentScene = projectManager.currentlyEditedScene ?: return
        val resolvedFileName = StorageOperations.resolveFileName(contentResolver, uri)
        val lookDataName: String
        val lookFileName: String

        val useSpriteName = resolvedFileName == null ||
                StorageOperations.getSanitizedFileName(resolvedFileName) == TMP_IMAGE_FILE_NAME

        if (useSpriteName) {
            lookDataName = currentSprite.name
            lookFileName = lookDataName + imageExtension
        } else {
            lookDataName = StorageOperations.getSanitizedFileName(resolvedFileName)
            lookFileName = resolvedFileName
        }

        val finalLookDataName = UniqueNameProvider().getUniqueNameInNameables(lookDataName, currentSprite.lookList)

        try {
            val imageDirectory = File(currentScene.directory, Constants.IMAGE_DIRECTORY_NAME)
            val file = StorageOperations.copyUriToDir(contentResolver, uri, imageDirectory, lookFileName)
            Utils.removeExifData(imageDirectory, lookFileName)
            val look = LookData(finalLookDataName, file)
            currentSprite.lookList.add(look)
            look.collisionInformation?.calculate()

            val lookFragment = supportFragmentManager.findFragmentByTag(org.catrobat.catroid.ui.recyclerview.fragment.LookListFragment.TAG)
                    as? org.catrobat.catroid.ui.recyclerview.fragment.LookListFragment
            lookFragment?.notifyDataSetChanged()
        } catch (e: java.io.IOException) {
            Log.e("ProjectActivity", Log.getStackTraceString(e))
        }
    }

    private fun addSoundFromUri(uri: Uri?) {
        if (uri == null) return
        val currentSprite = projectManager.currentSprite ?: return
        val currentScene = projectManager.currentlyEditedScene ?: return
        val resolvedFileName = StorageOperations.resolveFileName(contentResolver, uri)
        val soundInfoName: String
        val soundFileName: String

        val useSpriteName = resolvedFileName == null

        if (useSpriteName) {
            soundInfoName = currentSprite.name
            soundFileName = soundInfoName + Constants.DEFAULT_SOUND_EXTENSION
        } else {
            soundInfoName = StorageOperations.getSanitizedFileName(resolvedFileName)
            soundFileName = resolvedFileName
        }

        val finalSoundInfoName = UniqueNameProvider().getUniqueNameInNameables(soundInfoName, currentSprite.soundList)

        try {
            val soundDirectory = File(currentScene.directory, Constants.SOUND_DIRECTORY_NAME)
            val file = StorageOperations.copyUriToDir(contentResolver, uri, soundDirectory, soundFileName)
            val sound = SoundInfo(finalSoundInfoName, file)
            currentSprite.soundList.add(sound)

            val soundFragment = supportFragmentManager.findFragmentByTag(org.catrobat.catroid.ui.recyclerview.fragment.SoundListFragment.TAG)
                    as? org.catrobat.catroid.ui.recyclerview.fragment.SoundListFragment
            soundFragment?.notifyDataSetChanged()
        } catch (e: java.io.IOException) {
            Log.e("ProjectActivity", Log.getStackTraceString(e))
        }
    }

    @Suppress("ComplexMethod")
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == TestResult.STAGE_ACTIVITY_TEST_SUCCESS ||
            resultCode == TestResult.STAGE_ACTIVITY_TEST_FAIL
        ) {
            val message = data?.getStringExtra(TestResult.TEST_RESULT_MESSAGE)
            ToastUtil.showError(this, message)
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val testResult = ClipData.newPlainText(
                "TestResult",
                "${projectManager.currentProject.name} $message".trimIndent()
            )
            clipboard.setPrimaryClip(testResult)
        }
        if (resultCode != RESULT_OK) {
            if (requestCode == SPRITE_POCKET_PAINT) {
                addEmptySpriteObject()
            }
            return
        }
        val uri: Uri?
        when (requestCode) {
            SPRITE_POCKET_PAINT -> {
                uri = ImportFromPocketPaintLauncher(this).getPocketPaintCacheUri()
                addSpriteFromUri(uri)
            }
            SPRITE_LIBRARY -> {
                uri = Uri.fromFile(File(data!!.getStringExtra(WebViewActivity.MEDIA_FILE_PATH)))
                addSpriteFromUri(uri)
            }
            SPRITE_OBJECT -> {
                uri = Uri.fromFile(File(data!!.getStringExtra(WebViewActivity.MEDIA_FILE_PATH)))
                addObjectFromUri(uri)
            }
            SPRITE_FILE -> {
                uri = data?.data
                addSpriteFromUri(uri, Constants.JPEG_IMAGE_EXTENSION)
            }
            SPRITE_CAMERA -> {
                uri = ImportFromCameraLauncher(this).getCacheCameraUri()
                addSpriteFromUri(uri, Constants.JPEG_IMAGE_EXTENSION)
            }

            SpriteActivity.BACKGROUND_POCKET_PAINT -> {
                uri = ImportFromPocketPaintLauncher(this).getPocketPaintCacheUri()
                addBackgroundFromUri(uri)
            }
            SpriteActivity.BACKGROUND_LIBRARY -> {
                uri = Uri.fromFile(File(data!!.getStringExtra(WebViewActivity.MEDIA_FILE_PATH)))
                addBackgroundFromUri(uri)
            }
            SpriteActivity.BACKGROUND_FILE -> {
                uri = data?.data
                addBackgroundFromUri(uri, Constants.JPEG_IMAGE_EXTENSION)
            }
            SpriteActivity.BACKGROUND_CAMERA -> {
                uri = ImportFromCameraLauncher(this).getCacheCameraUri()
                addBackgroundFromUri(uri, Constants.JPEG_IMAGE_EXTENSION)
            }

            LOOK_POCKET_PAINT -> {
                uri = ImportFromPocketPaintLauncher(this).getPocketPaintCacheUri()
                addLookFromUri(uri)
            }
            LOOK_LIBRARY -> {
                uri = Uri.fromFile(File(data!!.getStringExtra(WebViewActivity.MEDIA_FILE_PATH)))
                addLookFromUri(uri)
            }
            LOOK_FILE -> {
                uri = data?.data
                addLookFromUri(uri, Constants.JPEG_IMAGE_EXTENSION)
            }
            LOOK_CAMERA -> {
                uri = ImportFromCameraLauncher(this).getCacheCameraUri()
                addLookFromUri(uri, Constants.JPEG_IMAGE_EXTENSION)
            }

            SOUND_RECORD, SOUND_FILE -> {
                uri = data?.data
                addSoundFromUri(uri)
            }
            SOUND_LIBRARY -> {
                uri = Uri.fromFile(File(data!!.getStringExtra(WebViewActivity.MEDIA_FILE_PATH)))
                addSoundFromUri(uri)
            }

            SpriteActivity.REQUEST_CODE_VISUAL_PLACEMENT -> {
                val extras = data?.extras ?: return
                val brickHash = extras.getInt(SpriteActivity.EXTRA_BRICK_HASH, -1)

                if (brickHash != -1) {
                    updateBrickFromVisualPlacement(extras)
                } else {
                    val xCoordinate = extras.getInt(VisualPlacementActivity.X_COORDINATE_BUNDLE_ARGUMENT)
                    val yCoordinate = extras.getInt(VisualPlacementActivity.Y_COORDINATE_BUNDLE_ARGUMENT)
                    val placeAtBrick = PlaceAtBrick(xCoordinate, yCoordinate)
                    val currentSprite = projectManager.currentSprite
                    val startScript = StartScript()
                    currentSprite.prependScript(startScript)
                    startScript.addBrick(placeAtBrick)
                }
            }
            SPRITE_FROM_LOCAL ->
                if (data != null && data.hasExtra(ProjectListActivity.IMPORT_LOCAL_INTENT)) {
                    uri = Uri.fromFile(
                        File(data.getStringExtra(ProjectListActivity.IMPORT_LOCAL_INTENT))
                    )
                    addObjectFromUri(uri)
                }
        }
    }

    private fun addSpriteFromUri(uri: Uri?, imageExtension: String = DEFAULT_IMAGE_EXTENSION) {
        addSpriteObjectFromUri(uri, imageExtension, false)
    }

    fun addObjectFromUri(uri: Uri?) {
        addSpriteObjectFromUri(uri, Constants.CATROBAT_EXTENSION, true)
    }

    private fun addSpriteObjectFromUri(uri: Uri?, extension: String, isObject: Boolean) {
        val currentScene = projectManager.currentlyEditedScene
        val resolvedName: String
        val resolvedFileName = StorageOperations.resolveFileName(contentResolver, uri)
        val lookFileName: String
        val useDefaultSpriteName = resolvedFileName == null ||
            StorageOperations.getSanitizedFileName(resolvedFileName) == TMP_IMAGE_FILE_NAME
        if (useDefaultSpriteName) {
            resolvedName = getString(R.string.default_sprite_name)
            lookFileName = resolvedName + extension
        } else {
            resolvedName = StorageOperations.getSanitizedFileName(resolvedFileName)
            lookFileName = resolvedFileName
        }
        var lookDataName = UniqueNameProvider().getUniqueNameInNameables(
            resolvedName,
            currentScene.spriteList
        )
        var importProjectHelper: ImportProjectHelper? = null
        if (isObject) {
            importProjectHelper = ImportProjectHelper(
                lookFileName, currentScene, this
            )
            if (!importProjectHelper.checkForConflicts()) {
                return
            }
            lookDataName = UniqueNameProvider().getUniqueNameInNameables(
                importProjectHelper.getSpriteToAddName(),
                currentScene.spriteList
            )
        }
        NewSpriteDialogFragment(
            false,
            lookDataName,
            lookFileName,
            contentResolver,
            uri,
            currentFragment!!,
            isObject,
            importProjectHelper
        ).show(supportFragmentManager, NewSpriteDialogFragment.TAG)
    }

    private fun addEmptySpriteObject() {
        val currentScene = projectManager.currentlyEditedScene
        val lookDataName = UniqueNameProvider().getUniqueNameInNameables(
            getString(R.string.default_sprite_name),
            currentScene.spriteList
        )
        NewSpriteDialogFragment(
            true,
            lookDataName,
            currentFragment!!
        ).show(supportFragmentManager, NewSpriteDialogFragment.TAG)
    }

    private fun handleAddButton() {
        val workspace = workspaceLayout
        if (workspace != null && workspace.visibility == View.VISIBLE) {
            val focusedTag = workspace.focusedWindowTag

            when (focusedTag) {
                org.catrobat.catroid.ui.recyclerview.fragment.ScriptFragment.TAG -> workspace.openBrickCategoryWindow()
                org.catrobat.catroid.ui.recyclerview.fragment.LookListFragment.TAG -> handleAddLookButton()
                org.catrobat.catroid.ui.recyclerview.fragment.SoundListFragment.TAG -> handleAddSoundButton()
                org.catrobat.catroid.ui.recyclerview.fragment.DataListFragment.TAG -> handleAddUserDataButton()
                SceneListFragment.TAG -> handleAddSceneButton()
                SpriteListFragment.TAG -> handleAddSpriteButton()
                else -> workspace.openBrickCategoryWindow()
            }
            return
        }

        if (currentFragment is SceneListFragment) {
            handleAddSceneButton()
            return
        }
        if (currentFragment is SpriteListFragment) {
            handleAddSpriteButton()
        }
    }

    fun handleAddLookButton() {
        val root = layoutInflater.inflate(R.layout.dialog_new_look, null)
        val alertDialog = AlertDialog.Builder(this)
            .setTitle(R.string.new_look_dialog_title)
            .setView(root)
            .create()

        val mediaLibraryUrl = if (projectManager.currentSprite == projectManager.currentlyEditedScene.backgroundSprite) {
            if (projectManager.isCurrentProjectLandscapeMode) LIBRARY_BACKGROUNDS_URL_LANDSCAPE else LIBRARY_BACKGROUNDS_URL_PORTRAIT
        } else {
            LIBRARY_LOOKS_URL
        }

        root.findViewById<View>(R.id.dialog_new_look_paintroid).setOnClickListener {
            ImportFromPocketPaintLauncher(this).startActivityForResult(LOOK_POCKET_PAINT)
            alertDialog.dismiss()
        }
        root.findViewById<View>(R.id.dialog_new_look_media_library).setOnClickListener {
            ImportFormMediaLibraryLauncher(this, mediaLibraryUrl).startActivityForResult(LOOK_LIBRARY)
            alertDialog.dismiss()
        }
        root.findViewById<View>(R.id.dialog_new_look_gallery).setOnClickListener {
            ImportFromFileLauncher(this, "image/*", getString(R.string.select_look_from_gallery)).startActivityForResult(LOOK_FILE)
            alertDialog.dismiss()
        }
        root.findViewById<View>(R.id.dialog_new_look_camera).setOnClickListener {
            ImportFromCameraLauncher(this).startActivityForResult(LOOK_CAMERA)
            alertDialog.dismiss()
        }
        alertDialog.show()
    }

    fun handleAddSoundButton() {
        val root = layoutInflater.inflate(R.layout.dialog_new_sound, null)
        val alertDialog = AlertDialog.Builder(this)
            .setTitle(R.string.new_sound_dialog_title)
            .setView(root)
            .create()

        root.findViewById<View>(R.id.dialog_new_sound_recorder).setOnClickListener {
            startActivityForResult(Intent(this, SoundRecorderActivity::class.java), SOUND_RECORD)
            alertDialog.dismiss()
        }
        root.findViewById<View>(R.id.dialog_new_sound_media_library).setOnClickListener {
            ImportFormMediaLibraryLauncher(this, LIBRARY_SOUNDS_URL).startActivityForResult(SOUND_LIBRARY)
            alertDialog.dismiss()
        }
        root.findViewById<View>(R.id.dialog_new_sound_gallery).setOnClickListener {
            ImportFromFileLauncher(this, "audio/*", getString(R.string.sound_select_source)).startActivityForResult(SOUND_FILE)
            alertDialog.dismiss()
        }
        if (BuildConfig.FEATURE_POCKETMUSIC_ENABLED) {
            root.findViewById<View>(R.id.dialog_new_sound_pocketmusic).visibility = View.VISIBLE
            root.findViewById<View>(R.id.dialog_new_sound_pocketmusic).setOnClickListener {
                startActivity(Intent(this, PocketMusicActivity::class.java))
                alertDialog.dismiss()
            }
        }
        alertDialog.show()
    }

    fun handleAddUserDataButton() {
        val view = layoutInflater.inflate(R.layout.dialog_new_user_data, null)

        val makeListCheckBox = view.findViewById<android.widget.CheckBox>(R.id.make_list)
        makeListCheckBox.visibility = View.VISIBLE

        val multiplayerRadioButton = view.findViewById<android.widget.RadioButton>(R.id.multiplayer)
        if (SettingsFragment.isMultiplayerVariablesPreferenceEnabled(applicationContext)) {
            multiplayerRadioButton.visibility = View.VISIBLE
            multiplayerRadioButton.setOnCheckedChangeListener { _, isChecked ->
                makeListCheckBox.isEnabled = !isChecked
            }
        }

        val addToProjectUserDataRadioButton = view.findViewById<android.widget.RadioButton>(R.id.global)

        val variables = mutableListOf<UserData<*>>()
        val currentProject = projectManager.currentProject
        val currentSprite = projectManager.currentSprite

        variables.addAll(currentProject.userVariables)
        variables.addAll(currentProject.multiplayerVariables)
        variables.addAll(currentSprite.userVariables)

        val lists = mutableListOf<UserData<*>>()
        lists.addAll(currentProject.userLists)
        lists.addAll(currentSprite.userLists)

        val textWatcher = DuplicateInputTextWatcher(variables)
        val builder = TextInputDialog.Builder(this)
        val uniqueVariableNameProvider = builder.createUniqueNameProvider(R.string.default_variable_name)
        val uniqueListNameProvider = builder.createUniqueNameProvider(R.string.default_list_name)
        generatedVariableName = uniqueVariableNameProvider.getUniqueName(getString(R.string.default_variable_name), null)

        builder.setTextWatcher(textWatcher)
            .setText(generatedVariableName)
            .setPositiveButton(getString(R.string.ok), TextInputDialog.OnClickListener { _, textInput ->
                val addToProjectUserData = addToProjectUserDataRadioButton.isChecked
                val addToMultiplayerData = multiplayerRadioButton.isChecked

                PreferenceManager.getDefaultSharedPreferences(this).edit()
                    .putBoolean(SharedPreferenceKeys.INDEXING_VARIABLE_PREFERENCE_KEY, false).apply()

                if (makeListCheckBox.isChecked) {
                    val userList = UserList(textInput)
                    if (addToProjectUserData) {
                        currentProject.addUserList(userList)
                    } else {
                        currentSprite.addUserList(userList)
                    }
                } else {
                    val userVariable = UserVariable(textInput)
                    if (addToMultiplayerData) {
                        currentProject.addMultiplayerVariable(userVariable)
                    } else if (addToProjectUserData) {
                        currentProject.addUserVariable(userVariable)
                    } else {
                        currentSprite.addUserVariable(userVariable)
                    }
                }

                val dataFragment = supportFragmentManager.findFragmentByTag(org.catrobat.catroid.ui.recyclerview.fragment.DataListFragment.TAG)
                        as? org.catrobat.catroid.ui.recyclerview.fragment.DataListFragment
                dataFragment?.let {
                    it.notifyDataSetChanged()
                    it.indexAndSort()
                }
                org.catrobat.catroid.ui.recyclerview.fragment.DataListFragment.refreshActiveInstance();
            })

        val alertDialog = builder.setTitle(R.string.formula_editor_variable_dialog_title)
            .setView(view)
            .setNegativeButton(getString(R.string.cancel), null)
            .create()

        makeListCheckBox.setOnCheckedChangeListener { _, checked ->
            val textInputEditText = alertDialog.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.input_edit_text)
            val currentName = textInputEditText?.text.toString()
            if (checked) {
                alertDialog.setTitle(getString(R.string.formula_editor_list_dialog_title))
                textWatcher.setOriginalScope(lists)
                if (currentName == generatedVariableName) {
                    generatedVariableName = uniqueListNameProvider.getUniqueName(getString(R.string.default_list_name), null)
                    textInputEditText?.setText(generatedVariableName)
                }
            } else {
                alertDialog.setTitle(getString(R.string.formula_editor_variable_dialog_title))
                textWatcher.setOriginalScope(variables)
                if (currentName == generatedVariableName) {
                    generatedVariableName = uniqueVariableNameProvider.getUniqueName(getString(R.string.default_variable_name), null)
                    textInputEditText?.setText(generatedVariableName)
                }
            }
            multiplayerRadioButton.isEnabled = !checked
        }

        alertDialog.show()
    }

    fun handleAddSceneButton() {
        val currentProject = projectManager.currentProject
        val defaultSceneName = UniqueNameProvider().getUniqueNameInNameables(
            resources.getString(R.string.default_scene_name),
            currentProject.sceneList
        )
        val builder = TextInputDialog.Builder(this)
        builder.setHint(getString(R.string.scene_name_label))
            .setText(defaultSceneName)
            .setTextWatcher(DuplicateInputTextWatcher(currentProject.sceneList))
            .setPositiveButton(
                getString(R.string.ok)
            ) { _: DialogInterface?, textInput: String? ->
                val scene = SceneController
                    .newSceneWithBackgroundSprite(
                        textInput,
                        getString(R.string.background),
                        currentProject
                    )
                currentProject.addScene(scene)
                if (currentFragment is SceneListFragment) {
                    (currentFragment as RecyclerViewFragment<*>).notifyDataSetChanged()
                } else {
                    val intent = Intent(this, ProjectActivity::class.java)
                    intent.putExtra(EXTRA_FRAGMENT_POSITION, FRAGMENT_SCENES)
                    startActivity(intent)
                    finish()
                }
            }
        builder.setTitle(R.string.new_scene_dialog)
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun handleAddSpriteButton() {
        val dialogNewActorBinding = DialogNewActorBinding.inflate(layoutInflater)
        val alertDialog = AlertDialog.Builder(this)
            .setTitle(R.string.new_sprite_dialog_title)
            .setView(dialogNewActorBinding.root)
            .create()

        dialogNewActorBinding.dialogNewLookPaintroid.setOnClickListener {
            ImportFromPocketPaintLauncher(this)
                .startActivityForResult(SPRITE_POCKET_PAINT)
            alertDialog.dismiss()
        }
        dialogNewActorBinding.dialogNewLookMediaLibrary.setOnClickListener {
            ImportFormMediaLibraryLauncher(this, FlavoredConstants.LIBRARY_LOOKS_URL)
                .startActivityForResult(SPRITE_LIBRARY)
            alertDialog.dismiss()
        }

        dialogNewActorBinding.dialogNewLookObjectLibrary.setOnClickListener {
            ImportFormMediaLibraryLauncher(this, FlavoredConstants.LIBRARY_OBJECT_URL)
                .startActivityForResult(SPRITE_OBJECT)
            alertDialog.dismiss()
        }
        dialogNewActorBinding.dialogNewLookGallery.setOnClickListener {
            ImportFromFileLauncher(this, "image/*", getString(R.string.select_look_from_gallery))
                .startActivityForResult(SPRITE_FILE)
            alertDialog.dismiss()
        }
        dialogNewActorBinding.dialogNewLookCamera.setOnClickListener {
            ImportFromCameraLauncher(this)
                .startActivityForResult(SPRITE_CAMERA)
            alertDialog.dismiss()
        }
        dialogNewActorBinding.dialogNewLookBackpack.setOnClickListener {
            if (BackpackListManager.getInstance().sprites.isNotEmpty()) {
                val intent = Intent(this, BackpackActivity::class.java)
                intent.putExtra(
                    BackpackActivity.EXTRA_FRAGMENT_POSITION,
                    BackpackActivity.FRAGMENT_SPRITES
                )
                startActivity(intent)
            } else {
                ToastUtil.showError(this, R.string.backpack_empty)
            }
            alertDialog.dismiss()
        }
        dialogNewActorBinding.dialogNewLookFromActorsAndObjects.setOnClickListener {
            if (ActorsAndObjectsManager.getInstance().sprites.isNotEmpty()) {
                val intent = Intent(this, ActorAndObjectActivity::class.java)
                intent.putExtra(
                    ActorAndObjectActivity.EXTRA_FRAGMENT_POSITION,
                    ActorAndObjectActivity.FRAGMENT_SPRITES
                )
                startActivity(intent)
            } else {
                ToastUtil.showError(this, R.string.backpack_empty)
            }
            alertDialog.dismiss()
        }
        dialogNewActorBinding.dialogNewLookFromLocal.setOnClickListener {
            ImportFromLocalProjectListLauncher(
                this,
                getString(R.string.import_sprite_from_project_launcher)
            )
                .startActivityForResult(SPRITE_FROM_LOCAL)
            alertDialog.dismiss()
        }
        dialogNewActorBinding.dialogNewLookEmptyObject.setOnClickListener {
            addEmptySpriteObject()
            alertDialog.dismiss()
        }
        alertDialog.show()
    }

    private fun handlePlayButton() {
        StageActivity.handlePlayButton(projectManager, this)
    }

    private fun handleAiButton() {
        StageActivity.handleAiButton()
    }

    private fun showLegoSensorConfigInfo() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val nxtDialogDisabled = preferences.getBoolean(
            SettingsFragment.SETTINGS_MINDSTORMS_NXT_SHOW_SENSOR_INFO_BOX_DISABLED,
            false
        )
        val ev3DialogDisabled = preferences.getBoolean(
            SettingsFragment.SETTINGS_MINDSTORMS_EV3_SHOW_SENSOR_INFO_BOX_DISABLED,
            false
        )
        val resourcesSet = projectManager.currentProject.requiredResources
        if (!nxtDialogDisabled && resourcesSet.contains(Brick.BLUETOOTH_LEGO_NXT)) {
            val dialog: DialogFragment = LegoSensorConfigInfoDialog.newInstance(Constants.NXT)
            dialog.show(supportFragmentManager, LegoSensorConfigInfoDialog.DIALOG_FRAGMENT_TAG)
        }
        if (!ev3DialogDisabled && resourcesSet.contains(Brick.BLUETOOTH_LEGO_EV3)) {
            val dialog: DialogFragment = LegoSensorConfigInfoDialog.newInstance(Constants.EV3)
            dialog.show(supportFragmentManager, LegoSensorConfigInfoDialog.DIALOG_FRAGMENT_TAG)
        }
    }


    private fun showPhysicsCacheWarningDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.physics_warning_title)
            .setMessage(R.string.physics_warning_message)
            .setPositiveButton(R.string.physics_warning_update_now) { _, _ ->
                recalculatePhysicsCache()
            }
            .setNegativeButton(R.string.physics_warning_later, null)
            .show()
    }

    private fun recalculatePhysicsCache() {
        val progressDialog = android.app.ProgressDialog(this).apply {
            setMessage(getString(R.string.physics_warning_updating))
            setCancelable(false)
            show()
        }

        Thread {
            try {
                val project = projectManager.currentProject
                if (project != null && project.sceneList != null) {
                    for (scene in project.sceneList) {
                        if (scene.spriteList == null) continue
                        for (sprite in scene.spriteList) {
                            if (sprite.lookList == null) continue
                            for (look in sprite.lookList) {
                                look.collisionInformation?.forceRecalculateAndSave()
                            }
                        }
                    }
                    org.catrobat.catroid.io.XstreamSerializer.getInstance().saveProject(project)
                }

                runOnUiThread {
                    progressDialog.dismiss()
                    ToastUtil.showSuccess(this, getString(R.string.physics_warning_success))
                    val fragment = currentFragment
                    if (fragment is RecyclerViewFragment<*>) {
                        fragment.notifyDataSetChanged()
                    }
                }
            } catch (e: Exception) {
                Log.e("ProjectActivity", "Failed to recalculate physics cache", e)
                runOnUiThread {
                    progressDialog.dismiss()
                    ToastUtil.showError(this, getString(R.string.physics_warning_error))
                }
            }
        }.start()
    }
}
