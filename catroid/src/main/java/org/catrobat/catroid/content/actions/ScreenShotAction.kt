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

import android.util.Log
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import com.badlogic.gdx.utils.ScreenUtils
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.common.LookData
import org.catrobat.catroid.content.MyActivityManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.io.StorageOperations
import org.catrobat.catroid.stage.ScreenshotSaver
import java.io.File
import java.io.InputStream

@Suppress("DEPRECATION")
open class ScreenShotAction : TemporalAction() {
    var scope: Scope? = null
    var name: String? = null

    companion object {
        private const val TAG = "ScreenShotAction"
        private const val DEFAULT_FILENAME = "screenshot"
        private const val FILE_EXTENSION = ".png"
    }

    override fun update(percent: Float) {
        Gdx.app.postRunnable {
            captureAndSetLook()
        }
    }

    private fun captureAndSetLook() {
        val activity = MyActivityManager.stage_activity ?: run {
            Log.e(TAG, "StageActivity is null, cannot take screenshot.")
            return
        }

        val view = activity.window.decorView
        val width = view.measuredWidth
        val height = view.measuredHeight

        if (width <= 0 || height <= 0) {
            Log.e(TAG, "Invalid view dimensions, skipping screenshot.")
            return
        }

        val pixels = ScreenUtils.getFrameBufferPixels(0, 0, width, height, true)
        Log.d(TAG, "Screenshot data size: ${pixels.size}")

        val screenshotInputStream = getScreenshotStream(pixels, width, height)

        val lookData = createLookDataFromFile(screenshotInputStream)

        setLook(lookData)
    }

    private fun getScreenshotStream(pixels: ByteArray, width: Int, height: Int): InputStream {
        val screenshotPath = getScreenshotPath()
        val screenshotSaver = ScreenshotSaver(Gdx.files, screenshotPath, width, height)
        return screenshotSaver.getScreenshot(pixels)
    }

    private fun getScreenshotPath(): String {
        val scene = ProjectManager. getInstance().currentlyPlayingScene
        return scene.directory.absolutePath + "/"
    }

    private fun createLookDataFromFile(fileStream: InputStream): LookData {
        val finalFileName = name ?: DEFAULT_FILENAME
        val tempFile = File.createTempFile(finalFileName, FILE_EXTENSION)
        StorageOperations.copyStreamToFile(fileStream, tempFile)

        return LookData(finalFileName, tempFile).apply {
            collisionInformation.calculate()
        }
    }

    private fun setLook(lookData: LookData) {
        val sprite = scope?.sprite ?: return

        lookData.apply {
            updateLookListIndex()
            sprite.look.lookData = this
            collisionInformation?.collisionPolygonCalculationThread?.join()
            file?.delete()
            isWebRequest = true
        }
    }

    private fun updateLookListIndex() {
        val currentLook = scope?.sprite?.look
        if (currentLook != null && currentLook.lookListIndexBeforeLookRequest <= -1) {
            val lookList = scope?.sprite?.lookList
            currentLook.lookListIndexBeforeLookRequest = lookList?.indexOf(currentLook.lookData) ?: -1
        }
    }
}
