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
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.stage.StageActivity.IntentListener
import android.util.Log
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.R
import org.catrobat.catroid.camera.CameraManager
import org.catrobat.catroid.common.LookData

import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import java.io.File
import java.util.ArrayList

class PhotoAction() : TemporalAction() {
    var scope: Scope? = null
    var toast: Formula? = null

    override fun update(percent: Float) {
        StageActivity.getActiveCameraManager().takePicture2 { success, file ->
            if (success) {
                if (file != null) {
                    Gdx.app.postRunnable {
                        Log.d("PhotoAction", "Running on libGDX thread...")

                        // 1. Создаем LookData. Он еще "пустой" внутри.
                        val look = LookData(file.name, file)

                        // 2. САМЫЙ ВАЖНЫЙ ШАГ: Принудительно заставляем его загрузить Pixmap.
                        //    Это происходит в правильном потоке (GL Thread), поэтому должно сработать.
                        val loadedPixmap = look.pixmap
                        if (loadedPixmap != null && loadedPixmap.width > 1) { // Проверяем, что загрузилось нечто большее, чем 1x1 пиксель
                            // 3. Теперь, когда LookData "заряжен", устанавливаем его спрайту.
                            setLook(look)
                            Log.d("PhotoAction", "Look successfully prepared and set!")
                        } else {
                            Log.e("PhotoAction", "Failed to load Pixmap from file. Look remains unchanged.")
                        }
                    }
                }
            }
        }
    }

    private fun createlook(file: File): LookData {
        LookData(file.name, file).apply {
            collisionInformation.calculate()
            return this
        }
    }

    fun setLook(look: LookData) {
        look.apply {
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
