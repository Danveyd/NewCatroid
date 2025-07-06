// Файл ImageProcessingManager.kt
package org.catrobat.catroid.NN // Положите его рядом с OnnxSessionManager

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

    /**
     * Загружает Bitmap из файла проекта.
     * @return Bitmap или null, если файл не найден или не является картинкой.
     */
    fun loadBitmapFromFile(imageFile: File?): Bitmap? {
        if (imageFile == null) return null
        if (!imageFile.exists()) return null
        return BitmapFactory.decodeFile(imageFile.absolutePath)
    }

    /**
     * Изменяет размер изображения.
     * @param originalBitmap Исходный Bitmap.
     * @param newWidth Новая ширина.
     * @param newHeight Новая высота.
     * @return Новый Bitmap с измененным размером.
     */
    fun resizeBitmap(originalBitmap: Bitmap?, newWidth: Int?, newHeight: Int?): Bitmap? {
        if (originalBitmap == null || newWidth == null || newHeight == null) return null
        return Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
    }

    /**
     * Преобразует изображение в черно-белое.
     * @param originalBitmap Исходный Bitmap.
     * @return Новый черно-белый Bitmap.
     */
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

    /**
     * Нормализует изображение и сохраняет цветовые каналы в таблицы.
     * @param bitmap Исходный Bitmap (может быть цветным или Ч/Б).
     * @param rTableName Имя таблицы для красного канала.
     * @param gTableName Имя таблицы для зеленого канала.
     * @param bTableName Имя таблицы для синего канала.
     */
    fun normalizeToTables(bitmap: Bitmap?, rTableName: String, gTableName: String, bTableName: String) {
        if (bitmap == null) return
        val width = bitmap.width
        val height = bitmap.height

        // Создаем таблицы нужного размера
        TableManager.createTable(rTableName, width, height)
        TableManager.createTable(gTableName, width, height)
        TableManager.createTable(bTableName, width, height)
        // Для альфа-канала пока не делаем, он редко нужен

        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = bitmap.getPixel(x, y)
                // Нормализуем каждый канал в диапазон [0.0, 1.0]
                val r = pixel.red / 255.0f
                val g = pixel.green / 255.0f
                val b = pixel.blue / 255.0f

                // Сохраняем в таблицы. Координаты в TableManager начинаются с 1.
                TableManager.insertElement(rTableName, r, x + 1, y + 1)
                TableManager.insertElement(gTableName, g, x + 1, y + 1)
                TableManager.insertElement(bTableName, b, x + 1, y + 1)
            }
        }
    }

    /**
     * Пример того, как может выглядеть метод сохранения внутри вашего Action.
     *
     * @param project Экземпляр текущего проекта (из scope.project).
     * @param bitmapToSave Bitmap, который нужно сохранить.
     * @param preferredName Имя файла, которое хочет пользователь (например, "resized_image.png").
     * @return Объект File, указывающий на сохраненный файл, или null в случае ошибки.
     */
    fun saveBitmapToProject(file: File?, bitmapToSave: Bitmap): File? {
        // 1. Проверяем, что файл вообще передан.
        if (file == null) {
            return null // Правильно: если файла нет, нечего и делать.
        }

        // 2. Устанавливаем формат и качество. Все верно.
        val format = Bitmap.CompressFormat.PNG
        val quality = 100

        // 3. Используем try-catch для безопасной работы с файлами. Отлично.
        return try {
            // 4. Открываем FileOutputStream для УКАЗАННОГО файла.
            // Если файл уже существует, он будет ПЕРЕЗАПИСАН.
            // Это именно то поведение, которое вам нужно.
            FileOutputStream(file).use { out ->
                // 5. Сжимаем и записываем данные из Bitmap в поток.
                bitmapToSave.compress(format, quality, out)
            }

            // 6. Возвращаем тот же самый файл, чтобы подтвердить успех.
            file
        } catch (e: Exception) {
            // 7. В случае любой ошибки (например, нет прав на запись)
            // безопасно завершаемся и возвращаем null.
            ErrorLog.log(e.message?: "**message not provided :(**")
            //e.printStackTrace()
            null
        }
    }
}