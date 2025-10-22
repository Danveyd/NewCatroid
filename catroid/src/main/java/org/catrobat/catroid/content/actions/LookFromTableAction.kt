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
        if (percent < 1.0f) {
            return
        }

        val currentScope = scope ?: return

        // --- НАЧАЛО БЛОКА ОПТИМИЗАЦИИ (все так же, как и раньше) ---

        val r_name = r_table?.interpretString(currentScope) ?: "rTable"
        val g_name = g_table?.interpretString(currentScope) ?: "gTable"
        val b_name = b_table?.interpretString(currentScope) ?: "bTable"
        val a_name = a_table?.interpretString(currentScope) ?: "aTable"

        val rMap = TableManager.getTable(r_name) ?: return
        val gMap = TableManager.getTable(g_name)
        val bMap = TableManager.getTable(b_name)
        val aMap = TableManager.getTable(a_name)

        val width = rMap.keys.maxOfOrNull { it.first } ?: 0
        val height = rMap.keys.maxOfOrNull { it.second } ?: 0

        if (width == 0 || height == 0) return

        val pixelArray = IntArray(width * height)

        for (y in 1..height) {
            for (x in 1..width) {
                val key = Pair(x, y)
                val rValue = (rMap[key] as? Number)?.toInt() ?: 0
                val gValue = (gMap?.get(key) as? Number)?.toInt() ?: 0
                val bValue = (bMap?.get(key) as? Number)?.toInt() ?: 0
                val aValue = (aMap?.get(key) as? Number)?.toInt() ?: 255
                val index = (y - 1) * width + (x - 1)
                pixelArray[index] = Color.argb(aValue, rValue, gValue, bValue)
            }
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixelArray, 0, width, 0, 0, width, height)

        // --- КОНЕЦ БЛОКА ОПТИМИЗАЦИИ ---

        // 7. Сохраняем наш быстро сгенерированный Bitmap во временный файл
        val tempFile = saveBitmapToTempFile(bitmap)

        // 8. Создаем и устанавливаем Look из этого файла
        setLookFromFile(tempFile)

        // 9. (Важно!) Освобождаем память, занятую созданным Bitmap, т.к. LookData его уже скопировал
        bitmap.recycle()
    }

    private fun saveBitmapToTempFile(bitmap: Bitmap): File {
        val tempFile = File(CatroidApplication.getAppContext().cacheDir, "generated_${System.nanoTime()}.png")
        try {
            FileOutputStream(tempFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return tempFile
    }

    private fun setLookFromFile(file: File) {
        val currentSprite = scope?.sprite ?: return

        val newLookData = LookData(file.name, file)
        newLookData.collisionInformation.calculate()

        updateLookListIndex()
        currentSprite.look.lookData = newLookData
    }

    private fun updateLookListIndex() {
        val currentLook = scope?.sprite?.look
        if (currentLook != null && currentLook.lookListIndexBeforeLookRequest <= -1) {
            currentLook.lookListIndexBeforeLookRequest =
                scope?.sprite?.lookList?.indexOf(scope?.sprite?.look?.lookData) ?: -1
        }
    }
}
