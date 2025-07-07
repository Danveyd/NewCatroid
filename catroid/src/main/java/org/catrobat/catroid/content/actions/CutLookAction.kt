/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2024 The Catrobat Team
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

import android.widget.Toast
import android.content.Context
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.stage.StageActivity.IntentListener
import android.util.Log
import com.badlogic.gdx.math.Vector2
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.R
import org.catrobat.catroid.common.LookData

import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.utils.ErrorLog
import org.catrobat.catroid.utils.GlobalShaderManager
import java.io.File
import java.io.FileOutputStream
import java.util.ArrayList
import kotlin.math.abs

class CutLookAction() : TemporalAction() {
    var scope: Scope? = null
    var x1: Formula? = null
    var y1: Formula? = null
    var x2: Formula? = null
    var y2: Formula? = null


    override fun update(percent: Float) {
        // Получаем координаты и проверяем, что они не null
        val x1_i: Int = x1?.interpretInteger(scope) ?: 0
        val y1_i: Int = y1?.interpretInteger(scope) ?: 0
        val x2_i: Int = x2?.interpretInteger(scope) ?: 0
        val y2_i: Int = y2?.interpretInteger(scope) ?: 0
        val lookData: LookData? = scope?.sprite?.look?.lookData

        lookData?.let {
            val file: File = it.file //png файл
            try {
                // 1. Декодируем исходный файл в Bitmap
                val originalBitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (originalBitmap == null) {
                    // Не удалось декодировать файл, выходим
                    return
                }

                // 2. Рассчитываем параметры для обрезки
                val cropX = minOf(x1_i, x2_i)
                val cropY = minOf(y1_i, y2_i)
                val width = abs(x2_i - x1_i)
                val height = abs(y2_i - y1_i)

                // Проверяем, чтобы область обрезки не выходила за пределы изображения
                if (width > 0 && height > 0 &&
                    cropX + width <= originalBitmap.width &&
                    cropY + height <= originalBitmap.height) {

                    // 3. Обрезаем Bitmap
                    val croppedBitmap = Bitmap.createBitmap(originalBitmap, cropX, cropY, width, height)

                    // 4. Сохраняем обрезанный Bitmap во временный файл (чтобы не изменять исходный)
                    val context = CatroidApplication.getAppContext()
                    val tempFile = File.createTempFile("cropped_look_", ".png", context?.cacheDir)
                    FileOutputStream(tempFile).use { out ->
                        croppedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }

                    // 5. Создаем новый LookData из временного файла и устанавливаем его
                    val newLook = createlook(tempFile)
                    setLook(newLook)

                    // 6. Освобождаем память, занятую битмапами
                    originalBitmap.recycle()
                    croppedBitmap.recycle()
                }

            } catch (e: Exception) {
                // Обработка возможных ошибок (например, OutOfMemoryError или ошибки чтения файла)
                ErrorLog.log(e.message)
                e.printStackTrace()
            }
        }
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
