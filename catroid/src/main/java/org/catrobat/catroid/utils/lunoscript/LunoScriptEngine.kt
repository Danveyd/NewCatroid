package org.catrobat.catroid.utils.lunoscript

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.stage.StageActivity
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.Locale
import kotlin.script.dependencies.Environment

class LunoScriptEngine(
    private val androidContext: Context?, // Nullable if context is not always needed
    private val scope: Scope? = null
    // customNativeFunctions: Map<String, CallableNativeLunoFunction> - можно добавить позже
) {
    private val interpreter = Interpreter(androidContext, scope)

    // Если нужно регистрировать доп. нативные функции извне:
    fun registerNativeFunction(name: String, arity: IntRange, func: RawNativeLunoFunction) {
        interpreter.globals.define(name, LunoValue.NativeCallable(CallableNativeLunoFunction(name, arity, func)))
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun execute(script: String) {
        try {
            val lexer = Lexer(script)
            val tokens = lexer.scanTokens()
            val parser = Parser(tokens)
            val programAst = parser.parse()
            interpreter.interpret(programAst)
        } catch (e: LunoSyntaxError) {
            System.err.println("LunoScript Syntax Error (Engine): ${e.message} (Line: ${e.line}, Pos: ${e.position})")
            handleLunoError(e, "LunoSyntaxError: ${e.message}")
        } catch (e: LunoRuntimeError) {
            System.err.println("LunoScript Runtime Error (Engine): ${e.message} (Line: ${e.line})")
            e.cause?.printStackTrace(System.err)
            handleLunoError(e, "LunoRuntimeError: ${e.message}")
        } catch (t: Throwable) { // Ловим ВСЕ остальное, включая Error
            System.err.println("Unexpected LunoScript Engine Error (Throwable): ${t.javaClass.simpleName} - ${t.localizedMessage}")
            t.printStackTrace(System.err)
            handleLunoError(t, "LunoEngineError: ${t.javaClass.simpleName} - ${t.localizedMessage}")
        } catch (e: Exception) {
            handleLunoError(e, "LunoException: ${e.message}")
        }
    }

    private fun toast(msg: String) {
        val params = ArrayList<Any>(listOf(msg))
        StageActivity.messageHandler.obtainMessage(StageActivity.SHOW_TOAST, params).sendToTarget()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun handleLunoError(throwable: Throwable, toastMessagePrefix: String) {
        android.util.Log.d("LunoEngine", "handleLunoError CALLED for: $toastMessagePrefix, Exception: ${throwable.javaClass.simpleName}")
        val appContext = CatroidApplication.getAppContext() // Получаем контекст здесь

        if (appContext == null) { // Добавим проверку на null на всякий случай
            Log.d("LunoScriptEngine", "LunoScript Error: CatroidApplication.getAppContext() returned null. Cannot save log or show toast.")
            return
        }

        // Формируем сообщение для лога и Toast
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        var logMessage = "$timestamp - $toastMessagePrefix\n"
        logMessage += "Details: ${throwable.message}\n"

        if (throwable is LunoSyntaxError) {
            logMessage += "Location: Line ${throwable.line}, Position ${throwable.position}\n"
        } else if (throwable is LunoRuntimeError) {
            logMessage += "Location: Line ${throwable.line}\n"
        }

        logMessage += "Stack Trace:\n${throwable.stackTraceToString()}\n"
        throwable.cause?.let {
            logMessage += "\nCaused by: ${it.javaClass.simpleName} - ${it.localizedMessage}\n${it.stackTraceToString()}\n"
        }
        logMessage += "-------------------------------------------------\n"

        // 1. Сохранение в файл
        val logFileName = "LunoLog.txt"
        try {
            val resolver = appContext.contentResolver
            var outputStream: OutputStream? = null
            var uri: Uri? = null

            // --- НАЧАЛО ИЗМЕНЕНИЙ ---

            // 1. ИЩЕМ СУЩЕСТВУЮЩИЙ ФАЙЛ (только для Android Q и выше)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                // Какие колонки нам нужны из базы данных MediaStore
                val projection = arrayOf(MediaStore.Downloads._ID)
                // Условие поиска: имя файла И путь
                val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ? AND ${MediaStore.Downloads.RELATIVE_PATH} = ?"
                val selectionArgs = arrayOf(logFileName, android.os.Environment.DIRECTORY_DOWNLOADS + "/")

                // Выполняем запрос
                resolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
                    // Если курсор не пустой и в нем есть хотя бы одна запись
                    if (cursor.moveToFirst()) {
                        // Получаем ID найденного файла
                        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID))
                        // Создаем Uri для этого конкретного файла
                        uri = ContentUris.withAppendedId(collection, id)
                    }
                }
            }

            // 2. ЕСЛИ ФАЙЛ НЕ НАЙДЕН (uri == null), ТО СОЗДАЕМ НОВЫЙ
            if (uri == null) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, logFileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                    }
                }
                uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            }

            if (uri == null) {
                throw Exception("Failed to find or create MediaStore record.")
            }

            // 3. ОТКРЫВАЕМ ПОТОК ДЛЯ ЗАПИСИ (он автоматически перезапишет файл)
            outputStream = resolver.openOutputStream(uri!!)

            // --- КОНЕЦ ИЗМЕНЕНИЙ ---

            if (outputStream == null) {
                throw Exception("Failed to open output stream for URI: $uri")
            }

            // 4. Пишем в файл
            outputStream.use { stream ->
                stream.write(logMessage.toByteArray())
            }

            // 5. Показываем сообщение об успехе
            val finalToastMsg = "$toastMessagePrefix. Log saved to Downloads/$logFileName"
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                toast(finalToastMsg)
            }

        } catch (e: Exception) {
            System.err.println("LunoScript Error: Failed to write to log file: ${e.message}")
            e.printStackTrace(System.err)
            // Показываем сообщение о неудаче
            val finalToastMsg = "$toastMessagePrefix (Failed to save log)"
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                toast(finalToastMsg)
            }
        }
    }
}