package org.catrobat.catroid.NN

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import org.catrobat.catroid.content.TableManager
import org.catrobat.catroid.utils.ErrorLog
import java.io.File
import java.io.FileOutputStream

object ImageProcessingManager {

    fun loadBitmapFromFile(imageFile: File?): Bitmap? {
        if (imageFile == null) return null
        if (!imageFile.exists()) return null
        return BitmapFactory.decodeFile(imageFile.absolutePath)
    }


    fun resizeBitmap(originalBitmap: Bitmap?, newWidth: Int?, newHeight: Int?): Bitmap? {
        if (originalBitmap == null || newWidth == null || newHeight == null) return null
        return Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
    }


    fun convertToGrayscale(originalBitmap: Bitmap?): Bitmap? {
        if (originalBitmap == null) return null
        val width = originalBitmap.width
        val height = originalBitmap.height
        val grayscaleBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = originalBitmap.getPixel(x, y)
                // Стандартная формула для преобразования в Ч/Б (учитывает восприятие глазом)
                val gray = (pixel.red * 0.299 + pixel.green * 0.587 + pixel.blue * 0.114).toInt()
                grayscaleBitmap.setPixel(x, y, Color.rgb(gray, gray, gray))
            }
        }
        return grayscaleBitmap
    }


    fun normalizeToTables(bitmap: Bitmap?, rTableName: String, gTableName: String, bTableName: String) {
        if (bitmap == null) return
        val width = bitmap.width
        val height = bitmap.height


        TableManager.createTable(rTableName, width, height)
        TableManager.createTable(gTableName, width, height)
        TableManager.createTable(bTableName, width, height)

        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = bitmap.getPixel(x, y)
                val r = pixel.red / 255.0f
                val g = pixel.green / 255.0f
                val b = pixel.blue / 255.0f

                TableManager.insertElement(rTableName, r, x + 1, y + 1)
                TableManager.insertElement(gTableName, g, x + 1, y + 1)
                TableManager.insertElement(bTableName, b, x + 1, y + 1)
            }
        }
    }

    fun saveBitmapToProject(file: File?, bitmapToSave: Bitmap): File? {
        if (file == null) {
            return null
        }

        val format = Bitmap.CompressFormat.PNG
        val quality = 100

        return try {
            FileOutputStream(file).use { out ->
                bitmapToSave.compress(format, quality, out)
            }

            file
        } catch (e: Exception) {
            ErrorLog.log(e.message?: "**message not provided :(**")
            null
        }
    }
}