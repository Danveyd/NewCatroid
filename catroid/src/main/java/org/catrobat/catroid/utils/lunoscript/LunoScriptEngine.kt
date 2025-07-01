package org.catrobat.catroid.utils.lunoscript

import android.content.Context
import android.util.Log
import android.widget.Toast
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.stage.StageActivity
import java.io.File
import java.io.FileOutputStream
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
            if (android.os.Environment.getExternalStorageState() == android.os.Environment.MEDIA_MOUNTED) {
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                val logFile = File(downloadsDir, logFileName)
                FileOutputStream(logFile, true).use {
                    it.write(logMessage.toByteArray())
                }

                val finalToastMsg = "$toastMessagePrefix. Log saved to Downloads/$logFileName"
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    toast(finalToastMsg)
                }

            } else {
                System.err.println("LunoScript Error: External storage not available. Cannot save log.")
                val finalToastMsg = "$toastMessagePrefix (Log not saved as storage is unavailable)"
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    toast(finalToastMsg)
                }
            }
        } catch (e: Exception) {
            System.err.println("LunoScript Error: Failed to write to log file or show toast: ${e.message}")
            e.printStackTrace(System.err)
            // Попытка показать Toast хотя бы об основной ошибке, если запись в лог не удалась
            val fallbackToastMsg = "$toastMessagePrefix (Failed to save log)"
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                toast(fallbackToastMsg)
            }
        }
    }
}