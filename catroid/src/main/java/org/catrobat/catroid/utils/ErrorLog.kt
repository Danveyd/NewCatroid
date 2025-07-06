package org.catrobat.catroid.utils

import org.luaj.vm2.ast.Str
import java.io.File
import java.io.FileOutputStream

object ErrorLog {
    fun log(error: String) {
        val logFileName = "NewCatroidError.txt"
        try {
            if (android.os.Environment.getExternalStorageState() == android.os.Environment.MEDIA_MOUNTED) {
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                val logFile = File(downloadsDir, logFileName)
                FileOutputStream(logFile, true).use {
                    it.write(error.toByteArray())
                }

            }
        } catch (e: Exception) {
            e.printStackTrace(System.err)
            // Попытка показать Toast хотя бы об основной ошибке, если запись в лог не удалась
        }
    }
}