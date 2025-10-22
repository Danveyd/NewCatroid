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
package org.catrobat.catroid.content.actions

import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Environment
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import com.badlogic.gdx.utils.ScreenUtils
import kotlinx.coroutines.GlobalScope
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.common.LookData
import org.catrobat.catroid.common.ScreenValues
import org.catrobat.catroid.content.MyActivityManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.io.StorageOperations
import org.catrobat.catroid.stage.ScreenshotSaver
import org.catrobat.catroid.stage.ScreenshotSaverCallback
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.paintroid.common.PERMISSION_EXTERNAL_STORAGE_SAVE
import org.catrobat.paintroid.common.PERMISSION_EXTERNAL_STORAGE_SAVE_COPY
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

open class SetLookFilesAction : TemporalAction() {
    var scope: Scope? = null
    var name: Formula? = null

    override fun update(percent: Float) {
        val activity = StageActivity.activeStageActivity.get()
        activity?.runOnUiThread {
            if (ContextCompat.checkSelfPermission(
                    CatroidApplication.getAppContext(),
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                    PERMISSION_EXTERNAL_STORAGE_SAVE_COPY
                )
            }
        }
        scope?.project?.let { proj ->
            val filename = getName(name) ?: "my_actor.png"
            val file = File(proj.filesDir, filename)
            if(file.exists()) {
                Gdx.app.postRunnable {
                    Log.d("PhotoAction", "Running on libGDX thread...")
                    val look = LookData(file.name, file)

                    val loadedPixmap = look.pixmap
                    if (loadedPixmap != null) {
                        setLook(look)
                        Log.d("PhotoAction", "Look successfully prepared and set!")
                    } else {
                        Log.e("PhotoAction", "Failed to load Pixmap from file. Look remains unchanged.")
                    }
                }
            } else {
                Log.e("LookFile", "File has not exists")
            }
        }
    }

    fun getName(inputName: Formula?): String? {
        inputName?.let { inname ->
            var name = inname.interpretString(scope)
            val lastDotIndex = name.lastIndexOf('.')
            if(lastDotIndex <= 0 && lastDotIndex >= name.length - 1) {
                name += ".png"
            }
            return name
        }
        return null
    }

    fun createlook(file: File): LookData {
        LookData(file.name, file).apply {
            collisionInformation.calculate()
            return this
        }
    }

    fun setLook(look: LookData) {
        look?.apply {
            updateLookListIndex()
            scope?.sprite?.look?.lookData = this
            collisionInformation?.collisionPolygonCalculationThread?.join()
            isWebRequest = false
        }
    }

    private fun updateLookListIndex() {
        val currentLook = scope?.sprite?.look
        if (!(currentLook != null && currentLook.lookListIndexBeforeLookRequest > -1)) {
            scope?.sprite?.look?.lookListIndexBeforeLookRequest =
                scope?.sprite?.lookList?.indexOf(scope?.sprite?.look?.lookData) ?: -1
        }
    }
}
