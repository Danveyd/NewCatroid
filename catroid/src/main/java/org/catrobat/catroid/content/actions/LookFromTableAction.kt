package org.catrobat.catroid.content.actions

import android.graphics.Bitmap
import android.graphics.Color
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.common.LookData
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.content.TableManager
import org.catrobat.catroid.formulaeditor.Formula
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class LookFromTableAction : TemporalAction() {
    var scope: Scope? = null
    var r_table: Formula? = null
    var g_table: Formula? = null
    var b_table: Formula? = null
    var a_table: Formula? = null

    override fun update(percent: Float) {
        scope?.let {
            val r_name = r_table?.interpretString(scope) ?: "rTable"
            val g_name = g_table?.interpretString(scope) ?: "gTable"
            val b_name = b_table?.interpretString(scope) ?: "bTable"
            val a_name = a_table?.interpretString(scope) ?: "aTable"

            val width = TableManager.getTableXSize(r_name)
            val height = TableManager.getTableYSize(r_name)

            // Создаем пустой Bitmap
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

            // Восстанавливаем значения пикселей из таблиц
            for (x in 1..width) {
                for (y in 1..height) {
                    val rValue = TableManager.getElementValue(r_name, x, y)?.toInt() ?: 0
                    val gValue = TableManager.getElementValue(g_name, x, y)?.toInt() ?: 0
                    val bValue = TableManager.getElementValue(b_name, x, y)?.toInt() ?: 0
                    val aValue = TableManager.getElementValue(a_name, x, y)?.toInt() ?: 255 // Если A отсутствует, то ставим 255

                    // Устанавливаем пиксель в Bitmap
                    val pixel = Color.argb(aValue, rValue, gValue, bValue)
                    bitmap.setPixel(x - 1, y - 1, pixel) // Индексы здесь начинаются с 0
                }
            }

            // Обновляем look с новым изображением
            val file = saveBitmapToTempFile(bitmap)
            val look = createlook(file)
            setLook(look)
        }
    }

    private fun saveBitmapToTempFile(bitmap: Bitmap): File {
        val tempFile = File(CatroidApplication.getAppContext().cacheDir, "temp_image.png")
        try {
            FileOutputStream(tempFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return tempFile
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
