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
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.stage.StageActivity.IntentListener
import android.util.Log
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.NN.ImageProcessingManager
import org.catrobat.catroid.R

import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.UserVariable
import java.util.ArrayList
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.JsePlatform
import java.io.File
import org.catrobat.catroid.content.Project
import java.io.FileOutputStream

class GrayscaleImgAction() : TemporalAction() {
    var scope: Scope? = null
    var file: Formula? = null

    override fun update(percent: Float) {
        val file_f: File? = scope?.project?.getFile(file?.interpretString(scope))
        file_f.let{
            val originalBitmap: Bitmap? = ImageProcessingManager.loadBitmapFromFile(it)
            originalBitmap.let { bmp ->
                val resized: Bitmap? = ImageProcessingManager.convertToGrayscale(bmp)
                if (resized == null) return
                ImageProcessingManager.saveBitmapToProject(file_f, resized)
            }
        }
    }
}
