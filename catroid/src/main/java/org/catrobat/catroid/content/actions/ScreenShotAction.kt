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
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import com.badlogic.gdx.utils.ScreenUtils
import kotlinx.coroutines.GlobalScope
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.common.LookData
import org.catrobat.catroid.common.ScreenValues
import org.catrobat.catroid.content.MyActivityManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.io.StorageOperations
import org.catrobat.catroid.stage.ScreenshotSaver
import org.catrobat.catroid.stage.ScreenshotSaverCallback
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

open class ScreenShotAction : TemporalAction() {
    var scope: Scope? = null
    var name: String? = null

    fun getScreenshotPath(): String {
        val scene = ProjectManager.getInstance().currentlyPlayingScene
        return scene.getDirectory().getAbsolutePath() + "/"
    }

    override fun update(percent: Float) {
        //val activity = StageActivity.activeStageActivity.get()
            /*val test = CatroidApplication.getAppContext().applicationContext
        val activityManager = test.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val tasks = activityManager.getRunningTasks(1)
        val currentActivity = tasks[0].topActivity

        val screen = takeScreenshot(currentActivity)
        val filename = name ?: ""
        val file = createFile(filename, ".png", screen)
        setLook(file)*/
        //MyActivityManager.stage_activity?.window?.decorView?.viewTreeObserver?.addOnGlobalLayoutListener {
        Gdx.app.postRunnable {
            val project = ProjectManager.getInstance().currentProject
            val header = project.xmlHeader
            var width = MyActivityManager.stage_activity?.window?.decorView?.measuredWidth ?: 0
            var height = MyActivityManager.stage_activity?.window?.decorView?.measuredHeight ?: 0
            var screenshotWidth = width //header.getVirtualScreenWidth() //ScreenValues.getResolutionForProject(project).width
            var screenshotHeight = height//header.getVirtualScreenHeight() //ScreenValues.getResolutionForProject(project).height
            var screenshotX = 0 //ScreenValues.getResolutionForProject(project).offsetX
            var screenshotY = 0 //ScreenValues.getResolutionForProject(project).offsetY
            val screenshotSaver = ScreenshotSaver(
                Gdx.files, getScreenshotPath(), screenshotWidth,
                screenshotHeight
            )
            Log.d("Screenshot", "Width: $width, Height: $height")
            val screenshot = ScreenUtils.getFrameBufferPixels(screenshotX, screenshotY, screenshotWidth, screenshotHeight, true)
            Log.d("Screenshot", "Screenshot data size: ${screenshot.size}")
            val screen = screenshotSaver.getScreenshot(screenshot) //byteArrayToInputStream(screenshot)
            val filename = name ?: "screenshot"
            val file = createFile(filename, ".png", screen)
            setLook(file)
        }
        //}
        /*val screen = screenshotSaver.getScreenshot(
            screenshot,
            //name ?: "screenshot",
        )
        val filename = name ?: "screenshot"
        val file = createFile(filename, ".png", screen)
        setLook(file)*/
        /*val activity = MyActivityManager.actor_activity
        activity?.let { activity2 ->
            val screen = takeScreenshot(activity2)
            val filename = name ?: "screenshot"
            val file = createFile(filename, ".png", screen)
            setLook(file)
        } ?: run {
            Log.e("Screenshot", "Activity is null...")
        }*/
        /*activity?.postRunnable {
            val screen = takeScreenshot(activity)
            val filename = name ?: "screenshot"
            val file = createFile(filename, ".png", screen)
            setLook(file)
        }*/
    }

    fun byteArrayToInputStream(byteArray: ByteArray): InputStream {
        // Создаем InputStream из ByteArray
        return ByteArrayInputStream(byteArray)
    }

    fun takeScreenshot(activity: Activity): InputStream {
        // Создаем Bitmap для активности
        val bitmap = Bitmap.createBitmap(activity.window.decorView.width,
            activity.window.decorView.height,
            Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        activity.window.decorView.draw(canvas) // Рисуем содержимое окна на канвасе

        // Сохраняем Bitmap в ByteArrayOutputStream
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)

        // Получаем byteArray и создаем InputStream
        val byteArray = byteArrayOutputStream.toByteArray()
        return ByteArrayInputStream(byteArray)
    }


    fun setLook(look: LookData) {
        look?.apply {
            updateLookListIndex()
            scope?.sprite?.look?.lookData = this
            collisionInformation?.collisionPolygonCalculationThread?.join()
            file?.delete()
            isWebRequest = true
        }
    }

    fun createFile(fileName: String, fileExtension: String, file: InputStream): LookData {
        val lookFile = File.createTempFile(fileName, fileExtension)
        StorageOperations.copyStreamToFile(file, lookFile)
        LookData(fileName, lookFile).apply {
            collisionInformation.calculate()
            return this
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
