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
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
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
import org.catrobat.catroid.content.TableManager
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.io.StorageOperations
import org.catrobat.catroid.stage.ScreenshotSaver
import org.catrobat.catroid.stage.ScreenshotSaverCallback
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.paintroid.common.PERMISSION_EXTERNAL_STORAGE_SAVE
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.jar.Manifest

open class LookToTableAction : TemporalAction() {
    var scope: Scope? = null
    var r_table: Formula? = null
    var g_table: Formula? = null
    var b_table: Formula? = null
    var a_table: Formula? = null

    override fun update(percent: Float) {
        scope?.let {
            val bitmap: Bitmap = BitmapFactory.decodeFile(it.sprite.look.lookData.file.absolutePath)
            val width = bitmap.width
            val height = bitmap.height

            val r_name = r_table?.interpretString(scope) ?: "rTable"
            val g_name = g_table?.interpretString(scope) ?: "gTable"
            val b_name = b_table?.interpretString(scope) ?: "bTable"
            val a_name = a_table?.interpretString(scope) ?: "aTable"

            TableManager.deleteTable(r_name)
            TableManager.deleteTable(g_name)
            TableManager.deleteTable(b_name)
            TableManager.deleteTable(a_name)

            TableManager.createTable(r_name, width, height)
            TableManager.createTable(g_name, width, height)
            TableManager.createTable(b_name, width, height)
            TableManager.createTable(a_name, width, height)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    val pixel = bitmap.getPixel(x, y)
                    val r = Color.red(pixel)
                    val g = Color.green(pixel)
                    val b = Color.blue(pixel)
                    val a = Color.alpha(pixel)

                    TableManager.insertElement(r_name, r, x + 1, y + 1)
                    TableManager.insertElement(g_name, g, x + 1, y + 1)
                    TableManager.insertElement(b_name, b, x + 1, y + 1)
                    TableManager.insertElement(a_name, a, x + 1, y + 1)
                }
            }
        }
    }

}
