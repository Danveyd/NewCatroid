package org.catrobat.catroid.utils.lunoscript

import android.content.Context
import org.catrobat.catroid.content.Scope

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
        // Сброс состояния интерпретатора перед новым выполнением (если нужно, но globals сохраняются)
        // interpreter.resetState() // Метод, который можно добавить в Interpreter

        try {
            val lexer = Lexer(script)
            val tokens = lexer.scanTokens()
            // println("LunoTokens: ${tokens.joinToString("\n")}") // Отладка

            val parser = Parser(tokens)
            val programAst = parser.parse()
            // println("LunoAST: $programAst") // Отладка

            interpreter.interpret(programAst)

        } catch (e: LunoSyntaxError) {
            // Уже выводится парсером или лексером, но можно добавить централизованный лог
            System.err.println("LunoScript Syntax Error (Engine): ${e.message} (Line: ${e.line}, Pos: ${e.position})")
        } catch (e: LunoRuntimeError) {
            // Уже выводится интерпретатором
            // System.err.println("LunoScript Runtime Error (Engine): ${e.message} (Line: ${e.line})")
        } catch (e: Exception) {
            System.err.println("Unexpected LunoScript Engine Error: ${e.javaClass.simpleName} - ${e.localizedMessage}")
            e.printStackTrace() // Для полной диагностики
        }
    }
}