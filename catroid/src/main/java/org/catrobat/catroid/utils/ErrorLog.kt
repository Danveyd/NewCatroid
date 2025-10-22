package org.catrobat.catroid.utils

import org.catrobat.catroid.stage.StageActivity
import org.luaj.vm2.ast.Str
import java.io.File
import java.io.FileOutputStream
import java.util.ArrayList

object ErrorLog {
    fun log(error1: String?) {
        val error = error1 ?: "**пустая ошибка (попробуйте еще раз)**"
        val params = ArrayList<Any>(listOf("Произошла ошибка. Лог сохранен в NewCatroidError.txt"))
        StageActivity.messageHandler.obtainMessage(StageActivity.SHOW_TOAST, params).sendToTarget()
        val logFileName = "NewCatroidError.txt"
        try {
            if (android.os.Environment.getExternalStorageState() == android.os.Environment.MEDIA_MOUNTED) {
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                val logFile = File(downloadsDir, logFileName)
                FileOutputStream(logFile, false).use {
                    it.write(error.toByteArray())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace(System.err)
        }
    }
}