package org.catrobat.catroid.libraryeditor.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.util.UUID

object LibraryStorageManager {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private const val DRAFTS_DIR = "library_drafts"
    private const val LAST_SESSION_FILENAME = "last_session.json"

    private fun getSessionFile(context: Context): File {
        val draftsDir = File(context.filesDir, DRAFTS_DIR)
        draftsDir.mkdirs()
        return File(draftsDir, LAST_SESSION_FILENAME)
    }

    /**
     * Загружает последнюю сессию. Если файл не найден или поврежден,
     * возвращает новый, пустой черновик.
     */
    fun loadLastSession(context: Context): LibraryDraft {
        val file = getSessionFile(context)
        if (!file.exists()) {
            return LibraryDraft(id = "last_session")
        }
        return try {
            gson.fromJson(file.readText(), LibraryDraft::class.java)
        } catch (e: Exception) {
            // В случае ошибки парсинга, возвращаем чистый проект
            LibraryDraft(id = "last_session")
        }
    }

    /**
     * Сохраняет текущую сессию.
     */
    fun saveSession(context: Context, draft: LibraryDraft) {
        val file = getSessionFile(context)
        file.writeText(gson.toJson(draft))
    }

    /**
     * Очищает последнюю сессию, удаляя файл.
     */
    fun clearSession(context: Context) {
        val file = getSessionFile(context)
        if (file.exists()) {
            file.delete()
        }
    }
}