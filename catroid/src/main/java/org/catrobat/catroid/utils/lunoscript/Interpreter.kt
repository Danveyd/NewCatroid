package org.catrobat.catroid.utils.lunoscript

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.math.Rectangle
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.common.LookData
import org.catrobat.catroid.content.ActionFactory
import org.catrobat.catroid.content.EventWrapper
import org.catrobat.catroid.content.Look
import org.catrobat.catroid.content.Project
import org.catrobat.catroid.content.Scene
import org.catrobat.catroid.content.Script
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.content.UserVarsManager
import org.catrobat.catroid.content.XmlHeader
import org.catrobat.catroid.content.actions.AskSpeechAction
import org.catrobat.catroid.content.actions.ScriptSequenceAction
import org.catrobat.catroid.content.bricks.AddEditBrick
import org.catrobat.catroid.content.bricks.AddItemToUserListBrick
import org.catrobat.catroid.content.bricks.AddRadioBrick
import org.catrobat.catroid.content.bricks.ArduinoSendDigitalValueBrick
import org.catrobat.catroid.content.bricks.ArduinoSendPWMValueBrick
import org.catrobat.catroid.content.bricks.AskBrick
import org.catrobat.catroid.content.bricks.AskGPTBrick
import org.catrobat.catroid.content.bricks.AskGemini2Brick
import org.catrobat.catroid.content.bricks.AskGeminiBrick
import org.catrobat.catroid.content.bricks.AskSpeechBrick
import org.catrobat.catroid.content.bricks.Brick
import org.catrobat.catroid.content.bricks.CloneBrick
import org.catrobat.catroid.content.bricks.ShowToastBlock
import org.catrobat.catroid.content.bricks.UserDefinedBrick
import org.catrobat.catroid.formulaeditor.CustomFormula
import org.catrobat.catroid.formulaeditor.CustomFormulaManager
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.InternTokenType
import org.catrobat.catroid.formulaeditor.UserList
import org.catrobat.catroid.formulaeditor.UserVariable
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.userbrick.UserDefinedBrickData
import java.io.File
import java.util.ArrayList // For LunoValue.List elements
import kotlin.math.pow

inline fun <reified T : Any> LunoValue.asSpecificKotlinType(
    functionNameForError: String, // Для понятных сообщений об ошибках
    argPositionForError: Int      // Для понятных сообщений об ошибках
): T {
    if (this is LunoValue.NativeObject && this.obj is T) {
        return this.obj // Компилятор сам сделает умный каст к T
    }
    val gotType = if (this is LunoValue.NativeObject) this.obj?.javaClass?.name ?: "null native object" else this::class.simpleName
    throw LunoRuntimeError(
        "$functionNameForError: Argument $argPositionForError expected to contain a Kotlin type '${T::class.java.simpleName}', but got '$gotType'.",
        -1
    )
}

// Тип для нативных функций остается здесь для использования в Interpreter,
// но LunoValue.NativeCallable будет хранить CallableNativeLunoFunction
typealias RawNativeLunoFunction = (interpreter: Interpreter, arguments: List<LunoValue>) -> LunoValue

class Interpreter(
    private val androidContext: Context? = null, // For Android specific native functions
    private val projectScope: org.catrobat.catroid.content.Scope? = null
) {
    val globals = Scope()
    private var currentScope: Scope = globals
    private var loopDepth = 0 // Для отслеживания вложенности циклов (для break/continue)

    // Для 'this' в методах класса
    private val locals: MutableMap<AstNode, Int> = mutableMapOf() // Для разрешения переменных (пока не используется полностью)


    init {
        // Встроенные функции
        defineNative("GetAppContext", 0..0) { _, _ ->
            LunoValue.NativeObject(CatroidApplication.getAppContext())
        }

        defineNative("MakeToast", 1..1) { _, args -> // <--- ИЗМЕНЕНО НА 1..1 (один аргумент)
            val messageLunoValue = args[0] // args теперь содержит только один элемент
            if (messageLunoValue !is LunoValue.String) {
                throw LunoRuntimeError("MakeToast first argument must be a String.", -1)
            }

            val actualMessageString = messageLunoValue.value // Получаем чистую Kotlin строку
            val params = ArrayList<Any>(listOf(actualMessageString))

            // Предполагается, что StageActivity.messageHandler и StageActivity.SHOW_TOAST доступны статически
            // или через какой-то другой механизм, не требующий передачи context из LunoScript.
            StageActivity.messageHandler.obtainMessage(StageActivity.SHOW_TOAST, params).sendToTarget()
            LunoValue.Null
        }

        defineNative("print", 0..Int.MAX_VALUE) { _, args ->
            Log.d("LunoScript", args.joinToString(" ") { lunoValueToString(it, humanReadable = true) } )
            LunoValue.Null
        }

        defineNative("len", 1..1) { _, args ->
            when (val arg = args[0]) {
                is LunoValue.String -> LunoValue.Number(arg.value.length.toDouble())
                is LunoValue.List -> LunoValue.Number(arg.elements.size.toDouble())
                is LunoValue.LunoObject -> LunoValue.Number(arg.fields.size.toDouble()) // Размер объекта = кол-во полей
                else -> throw LunoRuntimeError("Unsupported type for len(): ${arg::class.simpleName}", -1)
            }
        }

        defineNative("typeof", 1..1) { _, args ->
            when (val arg = args[0]) {
                is LunoValue.String -> LunoValue.String("String")
                is LunoValue.List -> LunoValue.String("List")
                is LunoValue.LunoObject -> LunoValue.String("Object")
                is LunoValue.Number -> LunoValue.String("Number")
                is LunoValue.Boolean -> LunoValue.String("Boolean")
                is LunoValue.LunoClass -> LunoValue.String("Class")
                is LunoValue.Callable -> LunoValue.String("Callable")
                is LunoValue.LunoFunction -> LunoValue.String("Function")
                is LunoValue.Null -> LunoValue.String("Null")
                is LunoValue.NativeObject -> LunoValue.String("NativeObject")
                is LunoValue.NativeCallable -> LunoValue.String("NativeCallable")
                else -> LunoValue.Null
            }
        }

        defineNative("parseInt", 1..2) { _, args ->
            val strValue = args[0]
            if (strValue !is LunoValue.String) {
                throw LunoRuntimeError("parseInt expects a string as the first argument.", -1)
            }

            val radixLuno = if (args.size > 1) args[1] else LunoValue.Number(10.0)
            if (radixLuno !is LunoValue.Number) {
                throw LunoRuntimeError("parseInt radix (second argument) must be a number.", -1)
            }

            val radix = radixLuno.value.toInt()
            if (radix < 2 || radix > 36) {
                throw LunoRuntimeError("parseInt radix must be between 2 and 36.", -1)
            }

            try {
                val parsedInt = strValue.value.toInt(radix)
                LunoValue.Number(parsedInt.toDouble())
            } catch (e: NumberFormatException) {
                // В JavaScript parseInt("abc") возвращает NaN. Мы можем вернуть Null или Number(Double.NaN)
                LunoValue.Number(Double.NaN) // или LunoValue.Null
            }
        }

        defineNative("parseFloat", 1..1) { _, args ->
            val argValue = args[0]

            val stringToParse: kotlin.String = when (argValue) {
                is LunoValue.String -> argValue.value
                is LunoValue.Number -> {
                    // Если уже число, просто возвращаем его
                    return@defineNative argValue // Используем return@defineNative для выхода из лямбды
                }
                // Для других типов преобразуем их в строку, как это делает JS parseFloat
                else -> lunoValueToString(argValue, humanReadable = true)
            }

            // Логика парсинга строки, как в JavaScript parseFloat:
            // 1. Убрать начальные пробелы.
            // 2. Определить знак (+/-).
            // 3. Прочитать число (целую и/или дробную часть).
            // 4. Обработать "Infinity" и "NaN".
            // 5. Игнорировать всё после валидного числа.

            val trimmedString = stringToParse.trimStart()
            if (trimmedString.isEmpty()) {
                return@defineNative LunoValue.Number(Double.NaN)
            }

            // Обработка "Infinity", "-Infinity", "NaN"
            if (trimmedString.equals("Infinity", ignoreCase = true) || trimmedString.equals("+Infinity", ignoreCase = true)) {
                return@defineNative LunoValue.Number(Double.POSITIVE_INFINITY)
            }
            if (trimmedString.equals("-Infinity", ignoreCase = true)) {
                return@defineNative LunoValue.Number(Double.NEGATIVE_INFINITY)
            }
            if (trimmedString.equals("NaN", ignoreCase = true)) {
                return@defineNative LunoValue.Number(Double.NaN)
            }

            val stringBuilder = StringBuilder()
            var hasDecimalPoint = false
            var hasExponent = false
            var hasSign = false
            var hasDigit = false

            for ((index, char) in trimmedString.withIndex()) {
                when {
                    char == '+' || char == '-' -> {
                        if (index == 0 && !hasDigit && !hasSign) { // Знак только в начале
                            stringBuilder.append(char)
                            hasSign = true
                        } else if ((stringBuilder.lastOrNull()?.equals('e', ignoreCase = true) == true) && !hasDigit) { // Знак после 'e'/'E'
                            stringBuilder.append(char)
                        }
                        else {
                            break // Невалидный знак
                        }
                    }
                    char.isDigit() -> {
                        stringBuilder.append(char)
                        hasDigit = true
                    }
                    char == '.' -> {
                        if (!hasDecimalPoint && !hasExponent) { // Только одна точка, и до экспоненты
                            stringBuilder.append(char)
                            hasDecimalPoint = true
                        } else {
                            break // Вторая точка или точка после экспоненты
                        }
                    }
                    char.equals('e', ignoreCase = true) -> {
                        if (!hasExponent && hasDigit) { // Только одна экспонента и после цифры
                            stringBuilder.append(char)
                            hasExponent = true
                            hasDigit = false // После 'e' снова могут быть знаки и цифры
                            hasSign = false // Для знака экспоненты
                        } else {
                            break
                        }
                    }
                    else -> {
                        // Любой другой символ прерывает парсинг числа
                        break
                    }
                }
            }

            val numberString = stringBuilder.toString()

            // Проверка, что строка не пустая и не состоит только из знака или точки
            if (numberString.isEmpty() || numberString == "." || numberString == "+" || numberString == "-") {
                return@defineNative LunoValue.Number(Double.NaN)
            }
            if ((numberString.startsWith('e', ignoreCase = true) || numberString.endsWith('e', ignoreCase = true)) && numberString.length == 1) { // "e" or "E"
                return@defineNative LunoValue.Number(Double.NaN)
            }
            if (numberString.endsWith('+') || numberString.endsWith('-')) { // "1e+"
                return@defineNative LunoValue.Number(Double.NaN)
            }


            try {
                val result = numberString.toDouble()
                LunoValue.Number(result)
            } catch (e: NumberFormatException) {
                LunoValue.Number(Double.NaN)
            }
        }

        defineNative("isNaN", 1..1) { _, args ->
            val value = args[0]
            val result = when (value) {
                is LunoValue.Number -> value.value.isNaN()
                else -> false // Только LunoValue.Number может быть NaN
            }
            LunoValue.Boolean(result)
        }

        defineNative("isFinite", 1..1) { _, args ->
            val value = args[0]
            val result = when (value) {
                is LunoValue.Number -> value.value.isFinite()
                else -> false // Только LunoValue.Number может быть конечным/бесконечным
            }
            LunoValue.Boolean(result)
        }

        defineNative("currentTimeMillis", 0..0) { _, _ ->
            LunoValue.Number(System.currentTimeMillis().toDouble())
        }

        defineNative("sqrt", 1..1) { _, args ->
            val num = args[0] as? LunoValue.Number ?: throw LunoRuntimeError("sqrt expects a number argument.", -1)
            if (num.value < 0) LunoValue.Number(Double.NaN) // sqrt из отрицательного числа - NaN
            else LunoValue.Number(kotlin.math.sqrt(num.value))
        }

        defineNative("abs", 1..1) { _, args ->
            val num = args[0] as? LunoValue.Number ?: throw LunoRuntimeError("abs expects a number argument.", -1)
            LunoValue.Number(kotlin.math.abs(num.value))
        }

        defineNative("round", 1..1) { _, args ->
            val num = args[0] as? LunoValue.Number ?: throw LunoRuntimeError("round expects a number argument.", -1)
            LunoValue.Number(kotlin.math.round(num.value))
        }

        defineNative("floor", 1..1) { _, args ->
            val num = args[0] as? LunoValue.Number ?: throw LunoRuntimeError("floor expects a number argument.", -1)
            LunoValue.Number(kotlin.math.floor(num.value))
        }

        defineNative("ceil", 1..1) { _, args ->
            val num = args[0] as? LunoValue.Number ?: throw LunoRuntimeError("ceil expects a number argument.", -1)
            LunoValue.Number(kotlin.math.ceil(num.value))
        }

        defineNative("random", 0..0) { _, _ ->
            LunoValue.Number(kotlin.random.Random.nextDouble()) // Случайное число от 0.0 (включительно) до 1.0 (исключительно)
        }

        defineNative("String", 1..1) { _, args ->
            // Используем lunoValueToString, так как он уже обрабатывает разные LunoValue
            // humanReadable = true, чтобы не было лишних кавычек для строк
            LunoValue.String(lunoValueToString(args[0], humanReadable = true))
        }

        defineNative("Number", 1..1) { _, args ->
            val value = args[0]
            when (value) {
                is LunoValue.Number -> value // Уже число
                is LunoValue.String -> {
                    // Пытаемся распарсить строку как Double, если не получается - NaN
                    LunoValue.Number(value.value.toDoubleOrNull() ?: Double.NaN)
                }
                is LunoValue.Boolean -> LunoValue.Number(if (value.value) 1.0 else 0.0)
                is LunoValue.Null -> LunoValue.Number(0.0)
                else -> LunoValue.Number(Double.NaN) // Не можем преобразовать другие типы
            }
        }

        defineNative("Boolean", 1..1) { _, args ->
            LunoValue.Boolean(args[0].isTruthy())
        }

        defineNative("Formula", 1..1) { _, args ->
            LunoValue.NativeObject(Formula(args[0].toString()))
        }

        defineNative("RegisterFormula", 5..5) { interpreter, args -> // Ожидаем 5 аргументов
            // Аргумент 0: uniqueName (String)
            val uniqueNameLuno = args[0]
            if (uniqueNameLuno is LunoValue.Null) { // Правильная проверка на null
                // Если uniqueName не может быть null, можно выбросить ошибку или просто ничего не делать
                println("RegisterFormula: uniqueName is null, skipping.")
                return@defineNative LunoValue.Null // Используем return с меткой для лямбды
            }
            if (uniqueNameLuno !is LunoValue.String) {
                throw LunoRuntimeError("RegisterFormula: uniqueName (arg 0) must be a String.", -1)
            }
            val uniqueName = uniqueNameLuno.value

            // Аргумент 1: displayName (String)
            val displayNameLuno = args[1]
            if (displayNameLuno !is LunoValue.String) {
                throw LunoRuntimeError("RegisterFormula: displayName (arg 1) must be a String.", -1)
            }
            val displayName = displayNameLuno.value

            // Аргумент 2: defaultParamValues (List of Strings)
            val defaultValuesLunoList = args[2]
            if (defaultValuesLunoList !is LunoValue.List) {
                throw LunoRuntimeError("RegisterFormula: defaultParamValues (arg 2) must be a List.", -1)
            }
            val defaultParamValues = defaultValuesLunoList.elements.map {
                if (it !is LunoValue.String) {
                    throw LunoRuntimeError("RegisterFormula: all elements in defaultParamValues must be Strings.", -1)
                }
                it.value // Получаем Kotlin String
            }

            // Вычисляем paramCount
            val paramCount = defaultParamValues.size

            // Аргумент 3: defaultParamTypes (List of Strings, которые нужно смапить в InternTokenType)
            val defaultTypesLunoList = args[3]
            if (defaultTypesLunoList !is LunoValue.List) {
                throw LunoRuntimeError("RegisterFormula: defaultParamTypes (arg 3) must be a List.", -1)
            }
            if (defaultTypesLunoList.elements.size != paramCount) {
                throw LunoRuntimeError("RegisterFormula: defaultParamTypes list must have the same size ($paramCount) as defaultParamValues list.", -1)
            }
            val defaultParamTypes = defaultTypesLunoList.elements.map { typeNameLuno ->
                if (typeNameLuno !is LunoValue.String) {
                    throw LunoRuntimeError("RegisterFormula: all elements in defaultParamTypes must be Strings representing type names.", -1)
                }
                // Функция для маппинга строки в InternTokenType
                mapStringToInternTokenType(typeNameLuno.value)
            }

            // Аргумент 4: jsCode (String)
            val jsCodeLuno = args[4]
            if (jsCodeLuno !is LunoValue.String) {
                throw LunoRuntimeError("RegisterFormula: jsCode (arg 4) must be a String.", -1)
            }
            val jsCode = jsCodeLuno.value

            // Создаем и добавляем формулу
            CustomFormulaManager.addFormula(
                CustomFormula(
                    uniqueName = uniqueName,
                    displayName = displayName,
                    paramCount = paramCount,
                    defaultParamValues = defaultParamValues,
                    defaultParamTypes = defaultParamTypes,
                    jsCode = jsCode
                )
            )

            LunoValue.Null
        }

        // --- String Functions ---
        defineNative("toUpperCase", 1..1) { _, args ->
            val str = args[0] as? LunoValue.String ?: throw LunoRuntimeError("toUpperCase expects a string argument.", -1)
            LunoValue.String(str.value.toUpperCase())
        }

        defineNative("toLowerCase", 1..1) { _, args ->
            val str = args[0] as? LunoValue.String ?: throw LunoRuntimeError("toLowerCase expects a string argument.", -1)
            LunoValue.String(str.value.toLowerCase())
        }

        defineNative("trim", 1..1) { _, args ->
            val str = args[0] as? LunoValue.String ?: throw LunoRuntimeError("trim expects a string argument.", -1)
            LunoValue.String(str.value.trim())
        }

        defineNative("startsWith", 2..2) { _, args ->
            val str = args[0] as? LunoValue.String ?: throw LunoRuntimeError("startsWith expects a string as first argument.", -1)
            val prefix = args[1] as? LunoValue.String ?: throw LunoRuntimeError("startsWith expects a string as second argument (prefix).", -1)
            LunoValue.Boolean(str.value.startsWith(prefix.value))
        }

        defineNative("endsWith", 2..2) { _, args ->
            val str = args[0] as? LunoValue.String ?: throw LunoRuntimeError("endsWith expects a string as first argument.", -1)
            val suffix = args[1] as? LunoValue.String ?: throw LunoRuntimeError("endsWith expects a string as second argument (suffix).", -1)
            LunoValue.Boolean(str.value.endsWith(suffix.value))
        }

        defineNative("stringContains", 2..2) { _, args -> // "contains" может конфликтовать с listContains
            val str = args[0] as? LunoValue.String ?: throw LunoRuntimeError("stringContains expects a string as first argument.", -1)
            val substring = args[1] as? LunoValue.String ?: throw LunoRuntimeError("stringContains expects a string as second argument (substring).", -1)
            LunoValue.Boolean(str.value.contains(substring.value))
        }

        defineNative("replace", 3..3) { _, args ->
            val str = args[0] as? LunoValue.String ?: throw LunoRuntimeError("replace expects a string as first argument.", -1)
            val oldValue = args[1] as? LunoValue.String ?: throw LunoRuntimeError("replace expects a string as second argument (oldValue).", -1)
            val newValue = args[2] as? LunoValue.String ?: throw LunoRuntimeError("replace expects a string as third argument (newValue).", -1)
            LunoValue.String(str.value.replace(oldValue.value, newValue.value))
        }

        defineNative("split", 2..2) { _, args ->
            val str = args[0] as? LunoValue.String ?: throw LunoRuntimeError("split expects a string as first argument.", -1)
            val delimiter = args[1] as? LunoValue.String ?: throw LunoRuntimeError("split expects a string as second argument (delimiter).", -1)
            val parts = str.value.split(delimiter.value).map { LunoValue.String(it) }
            LunoValue.List(parts.toMutableList())
        }

        defineNative("substring", 2..3) { _, args ->
            val str = args[0] as? LunoValue.String ?: throw LunoRuntimeError("substring expects a string as first argument.", -1)
            val startIndexLuno = args[1] as? LunoValue.Number ?: throw LunoRuntimeError("substring expects a number as second argument (startIndex).", -1)
            val startIndex = startIndexLuno.value.toInt()

            val endIndex = if (args.size > 2) {
                val endIndexLuno = args[2] as? LunoValue.Number ?: throw LunoRuntimeError("substring expects a number as third argument (endIndex) if provided.", -1)
                endIndexLuno.value.toInt()
            } else {
                str.value.length
            }

            try {
                LunoValue.String(str.value.substring(startIndex, endIndex))
            } catch (e: IndexOutOfBoundsException) {
                throw LunoRuntimeError("substring: index out of bounds. Start: $startIndex, End: $endIndex, Length: ${str.value.length}", -1)
            }
        }

        // --- List Functions ---
        defineNative("listPush", 2..Int.MAX_VALUE) { _, args ->
            val list = args[0] as? LunoValue.List ?: throw LunoRuntimeError("listPush expects a list as first argument.", -1)
            for (i in 1 until args.size) {
                list.elements.add(args[i])
            }
            LunoValue.Number(list.elements.size.toDouble()) // Возвращает новую длину
        }

        defineNative("listPop", 1..1) { _, args ->
            val list = args[0] as? LunoValue.List ?: throw LunoRuntimeError("listPop expects a list as first argument.", -1)
            if (list.elements.isEmpty()) LunoValue.Null // Или ошибка
            else list.elements.removeAt(list.elements.size - 1)
        }

        defineNative("listShift", 1..1) { _, args ->
            val list = args[0] as? LunoValue.List ?: throw LunoRuntimeError("listShift expects a list as first argument.", -1)
            if (list.elements.isEmpty()) LunoValue.Null
            else list.elements.removeAt(0)
        }

        defineNative("listUnshift", 2..Int.MAX_VALUE) { _, args ->
            val list = args[0] as? LunoValue.List ?: throw LunoRuntimeError("listUnshift expects a list as first argument.", -1)
            for (i in args.size - 1 downTo 1) { // Добавляем в начало, поэтому идем с конца аргументов
                list.elements.add(0, args[i])
            }
            LunoValue.Number(list.elements.size.toDouble())
        }

        defineNative("listJoin", 1..2) { _, args ->
            val list = args[0] as? LunoValue.List ?: throw LunoRuntimeError("listJoin expects a list as first argument.", -1)
            val separatorLuno = if (args.size > 1) args[1] else LunoValue.String(",")
            val separator = (separatorLuno as? LunoValue.String)?.value ?: ","
            LunoValue.String(list.elements.joinToString(separator) { lunoValueToString(it, humanReadable = true) })
        }

        defineNative("listSlice", 2..3) { _, args ->
            val list = args[0] as? LunoValue.List ?: throw LunoRuntimeError("listSlice expects a list as first argument.", -1)
            val startLuno = args[1] as? LunoValue.Number ?: throw LunoRuntimeError("listSlice startIndex must be a number.", -1)
            var start = startLuno.value.toInt()

            var end = if (args.size > 2) {
                (args[2] as? LunoValue.Number)?.value?.toInt() ?: list.elements.size
            } else {
                list.elements.size
            }

            // Обработка отрицательных индексов как в JS slice
            if (start < 0) start += list.elements.size
            if (end < 0) end += list.elements.size
            start = start.coerceIn(0, list.elements.size)
            end = end.coerceIn(0, list.elements.size)

            if (start >= end) LunoValue.List(mutableListOf())
            else LunoValue.List(list.elements.subList(start, end).toMutableList()) // toMutableList для новой копии
        }

        defineNative("listReverse", 1..1) { _, args ->
            val list = args[0] as? LunoValue.List ?: throw LunoRuntimeError("listReverse expects a list argument.", -1)
            list.elements.reverse()
            list // Возвращает измененный список
        }

        // --- Object Functions ---
        defineNative("ObjectKeys", 1..1) { _, args ->
            val obj = args[0] as? LunoValue.LunoObject ?: throw LunoRuntimeError("ObjectKeys expects an object argument.", -1)
            LunoValue.List(obj.fields.keys.map { LunoValue.String(it) }.toMutableList())
        }

        defineNative("ObjectValues", 1..1) { _, args ->
            val obj = args[0] as? LunoValue.LunoObject ?: throw LunoRuntimeError("ObjectValues expects an object argument.", -1)
            LunoValue.List(obj.fields.values.toMutableList()) // values уже LunoValue
        }

        defineNative("ObjectHasProperty", 2..2) { _, args ->
            val obj = args[0]
            val keyLuno = args[1] as? LunoValue.String ?: throw LunoRuntimeError("ObjectHasProperty expects a string as key argument.", -1)
            val key = keyLuno.value
            val result = when (obj) {
                is LunoValue.LunoObject -> obj.fields.containsKey(key)
                is LunoValue.NativeObject -> if (obj.obj is Map<*, *>) (obj.obj as Map<*, *>).containsKey(key) else false
                else -> false
            }
            LunoValue.Boolean(result)
        }


        // --- JSON Functions --- (Базовая реализация)
        // Для полноценного JSON.parse лучше использовать библиотеку org.json или kotlinx.serialization
        // Это очень упрощенная версия для простых случаев.
        defineNative("JSON_parse", 1..1) { interpreter, args -> // interpreter может понадобиться для рекурсии
            val jsonString = (args[0] as? LunoValue.String)?.value ?: throw LunoRuntimeError("JSON.parse expects a string argument.", -1)
            try {
                // Внимание: это ОЧЕНЬ упрощенный парсер. Не для продакшена.
                // Он не будет обрабатывать вложенные структуры, массивы в объектах и т.д. корректно.
                // Лучше заменить на вызов org.json.JSONObject или kotlinx.serialization.json.Json
                fun parseJsonValue(value: Any?): LunoValue {
                    return when (value) {
                        null -> LunoValue.Null
                        is kotlin.String -> LunoValue.String(value)
                        is kotlin.Number -> LunoValue.Number(value.toDouble())
                        is kotlin.Boolean -> LunoValue.Boolean(value)
                        is kotlin.collections.List<*> -> LunoValue.List(value.map { parseJsonValue(it) }.toMutableList())
                        is Map<*, *> -> {
                            val fields = mutableMapOf<String, LunoValue>()
                            value.forEach { (k, v) -> fields[k.toString()] = parseJsonValue(v) }
                            LunoValue.LunoObject(null, fields)
                        }
                        else -> throw LunoRuntimeError("Unsupported type in JSON parse: ${value?.javaClass?.simpleName}", -1)
                    }
                }

                // Пример использования org.json (если бы была подключена библиотека)
                // val jsonTokener = org.json.JSONTokener(jsonString)
                // val root = jsonTokener.nextValue()
                // return parseJsonValue(root)

                // Заглушка, так как нет библиотеки JSON здесь:
                if (jsonString.trim().startsWith("{") && jsonString.trim().endsWith("}")) {
                    LunoValue.LunoObject(null, mutableMapOf("parsed" to LunoValue.String("Object (basic)")))
                } else if (jsonString.trim().startsWith("[") && jsonString.trim().endsWith("]")) {
                    LunoValue.List(mutableListOf(LunoValue.String("Array (basic)")))
                } else {
                    throw LunoRuntimeError("Basic JSON.parse expects a simple object or array structure like {} or [] or simple values.", -1)
                }
                // Для реального использования эту функцию нужно заменить!

            } catch (e: Exception) { // Например, org.json.JSONException
                throw LunoRuntimeError("Failed to parse JSON string: ${e.message}", -1)
            }
        }

        defineNative("JSON_stringify", 1..1) { interpreter, args ->
            val value = args[0]
            // Рекурсивная функция для преобразования LunoValue в строку JSON
            fun stringifyValue(v: LunoValue): String {
                return when (v) {
                    is LunoValue.Null -> "null"
                    is LunoValue.Boolean -> v.value.toString()
                    is LunoValue.Number -> {
                        if (v.value.isNaN() || v.value.isInfinite()) "null" // JSON не поддерживает NaN/Infinity
                        else v.toString() // LunoValue.Number.toString уже форматирует числа
                    }
                    is LunoValue.String -> "\"${v.value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t")}\"" // Экранирование
                    is LunoValue.List -> v.elements.joinToString(prefix = "[", postfix = "]", separator = ",") { stringifyValue(it) }
                    is LunoValue.LunoObject -> {
                        // Если это экземпляр класса, возможно, нужно его сериализовать особым образом или только поля
                        v.fields.entries.joinToString(prefix = "{", postfix = "}", separator = ",") { (key, fieldValue) ->
                            "\"$key\":${stringifyValue(fieldValue)}"
                        }
                    }
                    is LunoValue.NativeObject -> "\"<NativeObject: ${v.obj::class.simpleName}>\"" // Или пытаться сериализовать obj, если возможно
                    is LunoValue.Callable -> "\"<Callable>\"" // Функции обычно не сериализуются в JSON
                    // Добавьте обработку других LunoValue по необходимости
                    else -> "\"<Unsupported LunoValue for JSON>\""
                }
            }
            LunoValue.String(stringifyValue(value))
        }


        // --- Math Functions & Constants ---
        defineNative("min", 1..Int.MAX_VALUE) { _, args ->
            if (args.isEmpty()) throw LunoRuntimeError("MathMin requires at least one argument.", -1)
            var minVal = (args[0] as? LunoValue.Number)?.value ?: throw LunoRuntimeError("MathMin arguments must be numbers.", -1)
            for (i in 1 until args.size) {
                val currentVal = (args[i] as? LunoValue.Number)?.value ?: throw LunoRuntimeError("MathMin arguments must be numbers.", -1)
                minVal = kotlin.math.min(minVal, currentVal)
            }
            LunoValue.Number(minVal)
        }

        defineNative("max", 1..Int.MAX_VALUE) { _, args ->
            if (args.isEmpty()) throw LunoRuntimeError("MathMax requires at least one argument.", -1)
            var maxVal = (args[0] as? LunoValue.Number)?.value ?: throw LunoRuntimeError("MathMax arguments must be numbers.", -1)
            for (i in 1 until args.size) {
                val currentVal = (args[i] as? LunoValue.Number)?.value ?: throw LunoRuntimeError("MathMax arguments must be numbers.", -1)
                maxVal = kotlin.math.max(maxVal, currentVal)
            }
            LunoValue.Number(maxVal)
        }

        defineNative("pow", 2..2) { _, args ->
            val baseLuno = args[0] as? LunoValue.Number ?: throw LunoRuntimeError("MathPow base must be a number.", -1)
            val expLuno = args[1] as? LunoValue.Number ?: throw LunoRuntimeError("MathPow exponent must be a number.", -1)

            val base = baseLuno.value
            val exp = expLuno.value

            LunoValue.Number(base.pow(exp))
        }

        // Добавление константы PI
        globals.define("PI", LunoValue.Number(kotlin.math.PI))
        globals.define("E", LunoValue.Number(kotlin.math.E)) // Константа E

        // --- Local Variables ---

        defineNative("SetLocalVar", 2..2) { _, args ->
            UserVarsManager.setVar(args[0].toString(), args[1].toString())
            LunoValue.Null
        }

        defineNative("GetLocalVar", 1..1) { _, args ->
            val value = UserVarsManager.getVar(args[0].toString()) ?: "null"
            LunoValue.String(value)
        }

        defineNative("DeleteLocalVar", 1..1) { _, args ->
            UserVarsManager.delVar(args[0].toString())
            LunoValue.Null
        }

        defineNative("DeleteLocalVars", 0..0) { _, args ->
            UserVarsManager.clearVars()
            LunoValue.Null
        }

        defineNative("GetLocalVarName", 1..1) { _, args ->
            val value = UserVarsManager.getVarName(getKotlinIntFromLunoValue(args[0], "GetLocalVarName", 1)) ?: "null"
            LunoValue.String(value)
        }

        defineNative("GetLocalVarValue", 1..1) { _, args ->
            val value = UserVarsManager.getVarValue(getKotlinIntFromLunoValue(args[0], "GetLocalVarName", 1)) ?: "null"
            LunoValue.String(value)
        }

        // --- Project ---
        defineNative("GetScope", 0..0) { _, args ->
            LunoValue.NativeObject(projectScope ?: "")
        }

        defineNative("GetSprite", 1..1) { _, args ->
            val lunoArg = args[0] // Это LunoValue

            // Шаг 1: Проверяем, что это LunoValue.NativeObject
            if (lunoArg is LunoValue.NativeObject) {
                // Шаг 2: Извлекаем обернутый Kotlin-объект
                val wrappedKotlinObject: Any = lunoArg.obj

                // Шаг 3: Проверяем, является ли обернутый объект экземпляром твоего класса Scope
                // Замени org.catrobat.catroid.content.Scope на реальный импортированный класс Scope
                if (wrappedKotlinObject is org.catrobat.catroid.content.Scope) { // Убедись, что Scope импортирован или указано полное имя
                    // Шаг 4: Безопасно приводим тип
                    val scopeInstance = wrappedKotlinObject as org.catrobat.catroid.content.Scope

                    // Теперь у тебя есть scopeInstance типа org.catrobat.catroid.content.Scope
                    // Получаем его свойство sprite
                    val spriteObject = scopeInstance.sprite // Предположим, sprite - это свойство класса Scope

                    // Оборачиваем результат обратно в LunoValue
                    // Если spriteObject может быть null:
                    if (spriteObject != null) {
                        return@defineNative LunoValue.NativeObject(spriteObject)
                    } else {
                        // Реши, что возвращать, если спрайта нет: LunoValue.Null или ошибку
                        return@defineNative LunoValue.Null
                    }
                } else {
                    // Ошибка: обернутый объект не того типа, который мы ожидали
                    throw LunoRuntimeError(
                        "GetSprite: Argument 0's native object is not a Scope. Got type: ${wrappedKotlinObject?.javaClass?.name ?: "null"}.",
                        -1 // Номер строки -1 для нативных функций, если не можем определить точнее
                    )
                }
            } else {
                // Ошибка: аргумент не является LunoValue.NativeObject
                throw LunoRuntimeError(
                    "GetSprite: Argument 0 must be a NativeObject containing a Scope. Got type: ${lunoArg::class.simpleName}.",
                    -1
                )
            }
        }

        defineNative("GetProject", 1..1) { _, args ->
            val lunoArg = args[0] // Это LunoValue

            // Шаг 1: Проверяем, что это LunoValue.NativeObject
            if (lunoArg is LunoValue.NativeObject) {
                // Шаг 2: Извлекаем обернутый Kotlin-объект
                val wrappedKotlinObject: Any = lunoArg.obj

                // Шаг 3: Проверяем, является ли обернутый объект экземпляром твоего класса Scope
                // Замени org.catrobat.catroid.content.Scope на реальный импортированный класс Scope
                if (wrappedKotlinObject is org.catrobat.catroid.content.Scope) { // Убедись, что Scope импортирован или указано полное имя
                    // Шаг 4: Безопасно приводим тип
                    val scopeInstance = wrappedKotlinObject as org.catrobat.catroid.content.Scope

                    // Теперь у тебя есть scopeInstance типа org.catrobat.catroid.content.Scope
                    // Получаем его свойство sprite
                    val spriteObject = scopeInstance.project // Предположим, sprite - это свойство класса Scope

                    // Оборачиваем результат обратно в LunoValue
                    // Если spriteObject может быть null:
                    if (spriteObject != null) {
                        return@defineNative LunoValue.NativeObject(spriteObject)
                    } else {
                        // Реши, что возвращать, если спрайта нет: LunoValue.Null или ошибку
                        return@defineNative LunoValue.Null
                    }
                } else {
                    // Ошибка: обернутый объект не того типа, который мы ожидали
                    throw LunoRuntimeError(
                        "GetSprite: Argument 0's native object is not a Scope. Got type: ${wrappedKotlinObject?.javaClass?.name ?: "null"}.",
                        -1 // Номер строки -1 для нативных функций, если не можем определить точнее
                    )
                }
            } else {
                // Ошибка: аргумент не является LunoValue.NativeObject
                throw LunoRuntimeError(
                    "GetSprite: Argument 0 must be a NativeObject containing a Scope. Got type: ${lunoArg::class.simpleName}.",
                    -1
                )
            }
        }

        defineNative("ProjectGetDirectory", 1..1) { _, args ->
            val proj = getProjectFromLunoValue(args[0])
            if(proj == null) {
                LunoValue.Null
            } else {
                LunoValue.String(proj.directory.toString())
            }
        }

        defineNative("ProjectSetDirectory", 1..1) { _, args ->
            val proj = getProjectFromLunoValue(args[0])
            if(proj == null) {
                LunoValue.Null
            } else {
                if (args[1] !is LunoValue.String) LunoValue.Null
                proj.directory = File(args[1].toString())
                LunoValue.Null
            }
        }

        defineNative("ProjectGetSceneList", 1..1) { _, args ->
            val proj = getProjectFromLunoValue(args[0])
            if(proj == null) {
                LunoValue.Null
            } else {
                LunoValue.List(
                    proj.sceneList.map { scene ->
                        LunoValue.NativeObject(scene) // Оборачиваем каждую сцену
                    }.toMutableList()
                )
            }
        }

        defineNative("ProjectGetSceneNames", 1..1) { _, args ->
            val proj = getProjectFromLunoValue(args[0])
            if(proj == null) {
                LunoValue.Null
            } else {
                LunoValue.List(
                    proj.sceneNames.map { scene ->
                        LunoValue.String(scene) // Оборачиваем каждую сцену
                    }.toMutableList()
                )
            }
        }

        defineNative("ProjectAddScene", 2..2) { _, args ->
            val proj = getProjectFromLunoValue(args[0])
            if(proj == null) {
                LunoValue.Null
            } else {
                val lunoArg = args[1]

                if (lunoArg !is LunoValue.NativeObject) {
                    throw LunoRuntimeError("AddProjectScene: Argument 1 must be a NativeObject.", -1)
                }
                if (lunoArg.obj is Scene) {
                    proj.addScene(lunoArg.obj) // listElementLuno.obj уже Scene благодаря smart cast
                } else {
                    // Ошибка: элемент списка не того типа
                    val gotType = lunoArg.obj.javaClass.name
                    throw LunoRuntimeError(
                        "AddProjectScene: Argument 1 was not a Scene. Got '$gotType'.",
                        -1
                    )
                }
                LunoValue.Null
            }
        }

        defineNative("ProjectRemoveScene", 2..2) { _, args ->
            val proj = getProjectFromLunoValue(args[0])
            if(proj == null) {
                LunoValue.Null
            } else {
                val lunoArg = args[1]

                if (lunoArg !is LunoValue.NativeObject) {
                    throw LunoRuntimeError("ProjectRemoveScene: Argument 1 must be a NativeObject.", -1)
                }
                if (lunoArg.obj is Scene) {
                    proj.removeScene(lunoArg.obj) // listElementLuno.obj уже Scene благодаря smart cast
                } else {
                    // Ошибка: элемент списка не того типа
                    val gotType = lunoArg.obj.javaClass.name
                    throw LunoRuntimeError(
                        "ProjectRemoveScene: Argument 1 was not a Scene. Got '$gotType'.",
                        -1
                    )
                }
                LunoValue.Null
            }
        }

        defineNative("ProjectHasScene", 1..1) { _, args ->
            val proj = getProjectFromLunoValue(args[0])
            if(proj == null) {
                LunoValue.Null
            } else {
                LunoValue.Boolean(proj.hasScene())
            }
        }

        defineNative("ProjectGetDefaultScene", 1..1) { _, args ->
            val proj = getProjectFromLunoValue(args[0])
            if(proj == null) {
                LunoValue.Null
            } else {
                LunoValue.NativeObject(proj.defaultScene)
            }
        }

        defineNative("ProjectGetUserVariables", 1..1) { _, args ->
            val proj = getProjectFromLunoValue(args[0])
            if(proj == null) {
                LunoValue.Null
            } else {
                LunoValue.List(
                    proj.userVariables.map { scene ->
                        LunoValue.NativeObject(scene) // Оборачиваем каждую сцену
                    }.toMutableList()
                )
            }
        }

        defineNative("ProjectGetUserVariablesCopy", 1..1) { _, args ->
            val proj = getProjectFromLunoValue(args[0])
            if(proj == null) {
                LunoValue.Null
            } else {
                LunoValue.List(
                    proj.userVariablesCopy.map { scene ->
                        LunoValue.NativeObject(scene) // Оборачиваем каждую сцену
                    }.toMutableList()
                )
            }
        }

        defineNative("ProjectGetUserVariable", 2..2) { _, args ->
            val proj = getProjectFromLunoValue(args[0])
            if(proj == null) {
                LunoValue.Null
            } else {
                LunoValue.NativeObject(proj.getUserVariable(args[1].toString()))
            }
        }

        defineNative("ProjectAddUserVariable", 2..2) { _, args ->
            val proj = getProjectFromLunoValue(args[0])
            if(proj == null) {
                LunoValue.Null
            } else {
                val lunoArg = args[1]
                if (lunoArg !is LunoValue.NativeObject) {
                    throw LunoRuntimeError("ProjectAddUserVariable: Argument 1 must be a NativeObject.", -1)
                }
                if (lunoArg.obj is UserVariable) {
                    proj.addUserVariable(lunoArg.obj) // listElementLuno.obj уже Scene благодаря smart cast
                } else {
                    // Ошибка: элемент списка не того типа
                    val gotType = lunoArg.obj.javaClass.name
                    throw LunoRuntimeError(
                        "ProjectAddUserVariable: Argument 1 in the list was not a UserVariable. Got '$gotType'.",
                        -1
                    )
                }
                LunoValue.Null
            }
        }

        defineNative("ProjectRemoveUserVariable", 2..2) { _, args ->
            val proj = getProjectFromLunoValue(args[0])
            if(proj == null) {
                LunoValue.Null
            } else {
                proj.removeUserVariable(args[1].toString())
                LunoValue.Null
            }
        }

        defineNative("UserVariable", 0..2) { _, args ->
            if(args.size == 0) {
                LunoValue.NativeObject(UserVariable())
            } else if(args.size == 1) {
                LunoValue.NativeObject(UserVariable(args[0].toString()))
            } else {
                LunoValue.NativeObject(UserVariable(args[0].toString(), args[1].toString()))
            }
        }

        defineNative("ProjectGetUserLists", 1..1) { _, args ->
            val proj = getProjectFromLunoValue(args[0])
            if(proj == null) {
                LunoValue.Null
            } else {
                LunoValue.List(
                    proj.userLists.map { scene ->
                        LunoValue.NativeObject(scene) // Оборачиваем каждую сцену
                    }.toMutableList()
                )
            }
        }

        defineNative("ProjectGetUserListsCopy", 1..1) { _, args ->
            val proj = getProjectFromLunoValue(args[0])
            if(proj == null) {
                LunoValue.Null
            } else {
                LunoValue.List(
                    proj.userListsCopy.map { scene ->
                        LunoValue.NativeObject(scene) // Оборачиваем каждую сцену
                    }.toMutableList()
                )
            }
        }

        defineNative("ProjectGetUserList", 2..2) { _, args ->
            val proj = getProjectFromLunoValue(args[0])
            if(proj == null) {
                LunoValue.Null
            } else {
                LunoValue.NativeObject(proj.getUserList(args[1].toString()))
            }
        }

        defineNative("ProjectAddUserList", 2..2) { _, args ->
            val proj = getProjectFromLunoValue(args[0])
            if(proj == null) {
                LunoValue.Null
            } else {
                val lunoArg = args[1]
                if (lunoArg !is LunoValue.NativeObject) {
                    throw LunoRuntimeError("ProjectAddUserList: Argument 1 must be a NativeObject.", -1)
                }
                if (lunoArg.obj is UserList) {
                    proj.addUserList(lunoArg.obj) // listElementLuno.obj уже Scene благодаря smart cast
                } else {
                    // Ошибка: элемент списка не того типа
                    val gotType = lunoArg.obj.javaClass.name
                    throw LunoRuntimeError(
                        "ProjectAddUserList: Argument 1 in the list was not a UserVariable. Got '$gotType'.",
                        -1
                    )
                }
                LunoValue.Null
            }
        }

        defineNative("ProjectRemoveUserList", 2..2) { _, args ->
            val proj = getProjectFromLunoValue(args[0])
            if(proj == null) {
                LunoValue.Null
            } else {
                proj.removeUserList(args[1].toString())
                LunoValue.Null
            }
        }

        defineNative("UserList", 0..2) { _, args ->
            if(args.size == 0) {
                LunoValue.NativeObject(UserList())
            } else if(args.size == 1) {
                LunoValue.NativeObject(UserList(args[0].toString()))
            } else {
                val lunoArg = args[1]

                if (lunoArg !is LunoValue.List) {
                    throw LunoRuntimeError("UserList: Argument 1 must be a List.", -1)
                }

                val newKotlinSceneList = mutableListOf<Any?>() // Используй правильный тип Scene
                for ((index, listElementLuno) in lunoArg.elements.withIndex()) {
                    // Каждый элемент должен быть NativeObject, содержащим Scene
                    if(listElementLuno is LunoValue.NativeObject) {
                        newKotlinSceneList.add(listElementLuno.obj)
                    } else if(listElementLuno is LunoValue.String){
                        newKotlinSceneList.add(listElementLuno.value)
                    } else if(listElementLuno is LunoValue.Number){
                        newKotlinSceneList.add(listElementLuno.value)
                    } else if(listElementLuno is LunoValue.Null){
                        newKotlinSceneList.add(null)
                    } else {
                        newKotlinSceneList.add(listElementLuno.toString())
                    }
                }
                LunoValue.NativeObject(UserList(args[0].toString(), newKotlinSceneList))
            }
        }

        defineNative("ProjectResetUserData", 1..1) { _, args ->
            val proj = getProjectFromLunoValue(args[0])
            if(proj == null) {
                LunoValue.Null
            } else {
                proj.resetUserData()
                LunoValue.Null
            }
        }

        defineNative("ProjectGetSpriteListWithClones", 1..1) { _, args ->
            val proj = getProjectFromLunoValue(args[0])
            if(proj == null) {
                LunoValue.Null
            } else {
                LunoValue.List(
                    proj.spriteListWithClones.map { scene ->
                        LunoValue.NativeObject(scene) // Оборачиваем каждую сцену
                    }.toMutableList()
                )
            }
        }

        defineNative("ProjectGetName", 1..1) { _, args ->
            val proj = getProjectFromLunoValue(args[0])
            if(proj == null) {
                LunoValue.Null
            } else {
                LunoValue.String(proj.name)
            }
        }

        defineNative("ProjectSetName", 2..2) { _, args ->
            val proj = getProjectFromLunoValue(args[0])
            if(proj == null) {
                LunoValue.Null
            } else {
                proj.name = args[1].toString()
                LunoValue.Null
            }
        }

        defineNative("ProjectGetDescription", 1..1) { _, args ->
            val proj = getProjectFromLunoValue(args[0])
            if(proj == null) {
                LunoValue.Null
            } else {
                LunoValue.String(proj.description)
            }
        }

        defineNative("ProjectSetDescription", 2..2) { _, args ->
            val proj = getProjectFromLunoValue(args[0])
            if(proj == null) {
                LunoValue.Null
            } else {
                proj.description = args[1].toString()
                LunoValue.Null
            }
        }

        defineNative("ProjectGetNotesAndCredits", 1..1) { _, args ->
            val proj = getProjectFromLunoValue(args[0])
            if(proj == null) {
                LunoValue.Null
            } else {
                LunoValue.String(proj.notesAndCredits)
            }
        }

        defineNative("ProjectSetNotesAndCredits", 2..2) { _, args ->
            val proj = getProjectFromLunoValue(args[0])
            if(proj == null) {
                LunoValue.Null
            } else {
                proj.notesAndCredits = args[1].toString()
                LunoValue.Null
            }
        }

        defineNative("ProjectGetCatrobatLanguageVersion", 1..1) { _, args ->
            val proj = getProjectFromLunoValue(args[0])
            if(proj == null) {
                LunoValue.Null
            } else {
                LunoValue.String(proj.catrobatLanguageVersion.toString())
            }
        }

        defineNative("ProjectGetXmlHeader", 1..1) { _, args ->
            val proj = getProjectFromLunoValue(args[0])
            if(proj == null) {
                LunoValue.Null
            } else {
                LunoValue.NativeObject(proj.xmlHeader)
            }
        }

        defineNative("ProjectGetFilesDir", 1..1) { _, args ->
            val proj = getProjectFromLunoValue(args[0])
            if(proj == null) {
                LunoValue.Null
            } else {
                LunoValue.String(proj.filesDir.toString())
            }
        }

        defineNative("ProjectGetLibsDir", 1..1) { _, args ->
            val proj = getProjectFromLunoValue(args[0])
            if(proj == null) {
                LunoValue.Null
            } else {
                LunoValue.String(proj.libsDir.toString())
            }
        }

        defineNative("ProjectGetFile", 2..2) { _, args ->
            val proj = getProjectFromLunoValue(args[0])
            if(proj == null) {
                LunoValue.Null
            } else {
                LunoValue.String(proj.getFile(args[1].toString()).toString())
            }
        }

        defineNative("ProjectGetLib", 2..2) { _, args ->
            val proj = getProjectFromLunoValue(args[0])
            if(proj == null) {
                LunoValue.Null
            } else {
                LunoValue.String(proj.getLib(args[1].toString()).toString())
            }
        }

        defineNative("ProjectCheckExtension", 3..3) { _, args ->
            val proj = getProjectFromLunoValue(args[0])
            if(proj == null) {
                LunoValue.Null
            } else {
                LunoValue.String(proj.checkExtension(args[1].toString(), args[2].toString()))
            }
        }

        defineNative("ProjectAddFile", 2..2) { _, args ->
            val proj = getProjectFromLunoValue(args[0])
            if(proj == null) {
                LunoValue.Null
            } else {
                proj.addFile(File(args[1].toString()))
                LunoValue.Null
            }
        }

        defineNative("ProjectDeleteFile", 2..2) { _, args ->
            val proj = getProjectFromLunoValue(args[0])
            if(proj == null) {
                LunoValue.Null
            } else {
                if(args[1].toString().contains("/") || args[1].toString().contains("\\")) {
                    proj.deleteFile(File(args[1].toString()))
                } else {
                    proj.deleteFile(args[1].toString())
                }
                LunoValue.Null
            }
        }

        defineNative("File", 1..2) { _, args ->
            if(args.size == 1) {
                LunoValue.NativeObject(File(args[0].toString()))
            } else {
                LunoValue.NativeObject(File(args[0].toString(), args[1].toString()))
            }
        }

        defineNative("FilePath", 1..1) { _, args ->
            val lunoArg = args[0]

            if (lunoArg !is LunoValue.NativeObject) {
                throw LunoRuntimeError("FilePath: Argument 0 must be a NativeObject.", -1)
            }

            if (lunoArg.obj is File) {
                LunoValue.String(lunoArg.obj.absolutePath)
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = lunoArg.obj.javaClass.name
                throw LunoRuntimeError(
                    "FilePath: Argument 0 was not a File. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("throw", 1..1) { _, args ->
            throw LunoRuntimeError(args[0].toString(), -1)
        }

        defineNative("Rectangle", 4..4) { _, args ->
            val arg0 = args[0]
            val arg1 = args[1]
            val arg2 = args[2]
            val arg3 = args[3]
            if(arg0 is LunoValue.Number && arg1 is LunoValue.Number && arg2 is LunoValue.Number && arg3 is LunoValue.Number) {
                val rect = Rectangle(arg0.value.toFloat(), arg1.value.toFloat(), arg2.value.toFloat(), arg3.value.toFloat())
                LunoValue.NativeObject(rect)
            } else {
                throw LunoRuntimeError(
                    "Rectangle: Arguments (0, 1, 2, 3) must be a Number.",
                    -1
                )
            }
        }

        defineNative("ProjectGetScreenRectangle", 1..1) { _, args ->
            val proj = getProjectFromLunoValue(args[0])
            if(proj == null) {
                LunoValue.Null
            } else {
                LunoValue.NativeObject(proj.screenRectangle)
            }
        }

        defineNative("ProjectSetCatrobatLanguageVersion", 2..2) { _, args ->
            val proj = getProjectFromLunoValue(args[0])
            if(proj == null) {
                LunoValue.Null
            } else {
                try {
                    proj.catrobatLanguageVersion = args[1].toString().toDouble()
                } catch (e: Exception) {
                    throw LunoRuntimeError(
                        "ProjectSetCatrobatLanguageVersion: Failed to convert String to Double. ${e.message}",
                        -1
                    )
                }
                LunoValue.Null
            }
        }

        defineNative("ProjectGetTags", 1..1) { _, args ->
            val proj = getProjectFromLunoValue(args[0])
            if(proj == null) {
                LunoValue.Null
            } else {
                LunoValue.List(
                    proj.tags.map { scene ->
                        LunoValue.NativeObject(scene) // Оборачиваем каждую сцену
                    }.toMutableList()
                )
            }
        }

        defineNative("ProjectSetTags", 2..2) { _, args ->
            val proj = getProjectFromLunoValue(args[0])
            if(proj == null) {
                LunoValue.Null
            } else {
                val lunoArg = args[1]

                if (lunoArg !is LunoValue.List) {
                    throw LunoRuntimeError("ProjectSetTags: Argument 1 must be a List.", -1)
                }

                val newKotlinSceneList = mutableListOf<String>() // Используй правильный тип Scene
                for ((index, listElementLuno) in lunoArg.elements.withIndex()) {
                    // Каждый элемент должен быть NativeObject, содержащим Scene
                    if (listElementLuno is LunoValue.NativeObject && listElementLuno.obj is String) {
                        newKotlinSceneList.add(listElementLuno.obj) // listElementLuno.obj уже Scene благодаря smart cast
                    } else {
                        // Ошибка: элемент списка не того типа
                        val gotType = if (listElementLuno is LunoValue.NativeObject) listElementLuno.obj?.javaClass?.name ?: "null native object" else listElementLuno::class.simpleName
                        throw LunoRuntimeError(
                            "ProjectSetTags: Element at index $index in the list was not a String. Got '$gotType'.",
                            -1
                        )
                    }
                }
                proj.tags = newKotlinSceneList
                LunoValue.Null
            }
        }

        defineNative("ProjectGetSceneByName", 2..2) { _, args ->
            val proj = getProjectFromLunoValue(args[0])
            if(proj == null) {
                LunoValue.Null
            } else {
                LunoValue.NativeObject(proj.getSceneByName(args[1].toString()))
            }
        }

        defineNative("ProjectGetSceneById", 2..2) { _, args ->
            val proj = getProjectFromLunoValue(args[0])
            if(proj == null) {
                LunoValue.Null
            } else {
                try {
                    LunoValue.NativeObject(proj.getSceneById(args[1].toString().toInt()))
                } catch (e: Exception) {
                    throw LunoRuntimeError(
                        "ProjectGetSceneById: Failed to convert String to Int. ${e.message}",
                        -1
                    )
                }
            }
        }

        defineNative("ProjectSetXmlHeader", 2..2) { _, args ->
            val proj = getProjectFromLunoValue(args[0])
            if(proj == null) {
                LunoValue.Null
            } else {
                val lunoArg = args[1]

                if (lunoArg !is LunoValue.NativeObject) {
                    throw LunoRuntimeError("ProjectSetXmlHeader: Argument 1 must be a NativeObject.", -1)
                }

                if (lunoArg.obj is XmlHeader) {
                    proj.xmlHeader = lunoArg.obj // listElementLuno.obj уже Scene благодаря smart cast
                } else {
                    // Ошибка: элемент списка не того типа
                    val gotType = lunoArg.obj.javaClass.name
                    throw LunoRuntimeError(
                        "ProjectSetXmlHeader: Argument 1 was not a Xmlheader. Got '$gotType'.",
                        -1
                    )
                }
                LunoValue.Null
            }
        }

        defineNative("SpriteGetName", 1..1) { _, args ->
            val lunoArg = args[0]

            if (lunoArg !is LunoValue.NativeObject) {
                throw LunoRuntimeError("SpriteGetName: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (lunoArg.obj is Sprite) {
                LunoValue.String(lunoArg.obj.name)
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = lunoArg.obj.javaClass.name
                throw LunoRuntimeError(
                    "SpriteGetName: Argument 0 was not a Sprite. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("Sprite", 1..2) { _, args ->
            if(args.size == 1) {
                LunoValue.NativeObject(Sprite(args[0].toString()))
            } else {
                val sprite = args[0]

                if (sprite !is LunoValue.NativeObject) {
                    throw LunoRuntimeError("Sprite: Argument 0 must be a NativeObject.", -1)
                }

                // Каждый элемент должен быть NativeObject, содержащим Scene
                if (sprite.obj is Sprite) {
                    val scene = args[0]

                    if (scene !is LunoValue.NativeObject) {
                        throw LunoRuntimeError("Sprite: Argument 1 must be a NativeObject.", -1)
                    }

                    // Каждый элемент должен быть NativeObject, содержащим Scene
                    if (scene.obj is Scene) {
                        LunoValue.NativeObject(Sprite(sprite.obj, scene.obj))
                    } else {
                        // Ошибка: элемент списка не того типа
                        val gotType = scene.obj.javaClass.name
                        throw LunoRuntimeError(
                            "Sprite: Argument 1 was not a Scene. Got '$gotType'.",
                            -1
                        )
                    }
                } else {
                    // Ошибка: элемент списка не того типа
                    val gotType = sprite.obj.javaClass.name
                    throw LunoRuntimeError(
                        "Sprite: Argument 0 was not a Sprite. Got '$gotType'.",
                        -1
                    )
                }
            }
        }

        defineNative("Look", 1..1) { _, args ->
            val sprite = args[0]

            if (sprite !is LunoValue.NativeObject) {
                throw LunoRuntimeError("Look: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (sprite.obj is Sprite) {
                LunoValue.NativeObject(Look(sprite.obj))
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = sprite.obj.javaClass.name
                throw LunoRuntimeError(
                    "Look: Argument 0 was not a Sprite. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("SpriteGetLook", 1..1) { _, args ->
            val sprite = args[0]

            if (sprite !is LunoValue.NativeObject) {
                throw LunoRuntimeError("SpriteGetLook: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (sprite.obj is Sprite) {
                LunoValue.NativeObject(sprite.obj.look)
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = sprite.obj.javaClass.name
                throw LunoRuntimeError(
                    "SpriteGetLook: Argument 0 was not a Sprite. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("LookRemove", 1..1) { _, args ->
            val look = args[0]

            if (look !is LunoValue.NativeObject) {
                throw LunoRuntimeError("LookRemove: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (look.obj is Look) {
                LunoValue.NativeObject(look.obj.remove())
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = look.obj.javaClass.name
                throw LunoRuntimeError(
                    "LookRemove: Argument 0 was not a Sprite. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("LookGetLookData", 1..1) { _, args ->
            val look = args[0]

            if (look !is LunoValue.NativeObject) {
                throw LunoRuntimeError("LookGetLookData: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (look.obj is Look) {
                LunoValue.NativeObject(look.obj.lookData)
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = look.obj.javaClass.name
                throw LunoRuntimeError(
                    "LookGetLookData: Argument 0 was not a Look. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("LookGetLookData2", 1..1) { _, args ->
            val look = args[0]

            if (look !is LunoValue.NativeObject) {
                throw LunoRuntimeError("LookGetLookData2: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (look.obj is Look) {
                LunoValue.NativeObject(look.obj.lookData2)
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = look.obj.javaClass.name
                throw LunoRuntimeError(
                    "LookGetLookData2: Argument 0 was not a Look. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("LookData", 2..2) { _, args ->
            LunoValue.NativeObject(LookData(args[0].toString(), File(args[1].toString())))
        }

        defineNative("LookDataGetName", 1..1) { _, args ->
            val look = args[0]

            if (look !is LunoValue.NativeObject) {
                throw LunoRuntimeError("LookDataGetName: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (look.obj is LookData) {
                LunoValue.NativeObject(look.obj.name)
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = look.obj.javaClass.name
                throw LunoRuntimeError(
                    "LookDataGetName: Argument 0 was not a LookData. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("LookDataSetName", 2..2) { _, args ->
            val look = args[0]

            if (look !is LunoValue.NativeObject) {
                throw LunoRuntimeError("LookDataSetName: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (look.obj is LookData) {
                look.obj.name = args[1].toString()
                LunoValue.Null
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = look.obj.javaClass.name
                throw LunoRuntimeError(
                    "LookDataSetName: Argument 0 was not a LookData. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("LookDataGetFile", 1..1) { _, args ->
            val look = args[0]

            if (look !is LunoValue.NativeObject) {
                throw LunoRuntimeError("LookDataGetFile: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (look.obj is LookData) {
                LunoValue.NativeObject(look.obj.file)
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = look.obj.javaClass.name
                throw LunoRuntimeError(
                    "LookDataGetFile: Argument 0 was not a LookData. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("LookDataSetFile", 2..2) { _, args ->
            val look = args[0]

            if (look !is LunoValue.NativeObject) {
                throw LunoRuntimeError("LookDataSetFile: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (look.obj is LookData) {
                look.obj.file = File(args[1].toString())
                LunoValue.Null
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = look.obj.javaClass.name
                throw LunoRuntimeError(
                    "LookDataSetFile: Argument 0 was not a LookData. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("LookSetLookData", 2..2) { _, args ->
            val look = args[0]

            if (look !is LunoValue.NativeObject) {
                throw LunoRuntimeError("LookSetLookData: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (look.obj is Look) {
                val lookd = args[1]

                if (lookd !is LunoValue.NativeObject) {
                    throw LunoRuntimeError("LookSetLookData: Argument 1 must be a NativeObject.", -1)
                }

                // Каждый элемент должен быть NativeObject, содержащим Scene
                if (lookd.obj is LookData) {
                    look.obj.lookData = lookd.obj
                    LunoValue.Null
                } else {
                    // Ошибка: элемент списка не того типа
                    val gotType = lookd.obj.javaClass.name
                    throw LunoRuntimeError(
                        "LookSetLookData: Argument 1 was not a LookData. Got '$gotType'.",
                        -1
                    )
                }
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = look.obj.javaClass.name
                throw LunoRuntimeError(
                    "LookSetLookData: Argument 0 was not a Look. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("LookSetLookData2", 2..2) { _, args ->
            val look = args[0]

            if (look !is LunoValue.NativeObject) {
                throw LunoRuntimeError("LookSetLookData2: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (look.obj is Look) {
                val lookd = args[1]

                if (lookd !is LunoValue.NativeObject) {
                    throw LunoRuntimeError("LookSetLookData2: Argument 1 must be a NativeObject.", -1)
                }

                // Каждый элемент должен быть NativeObject, содержащим Scene
                if (lookd.obj is LookData) {
                    look.obj.lookData2 = lookd.obj
                    LunoValue.Null
                } else {
                    // Ошибка: элемент списка не того типа
                    val gotType = lookd.obj.javaClass.name
                    throw LunoRuntimeError(
                        "LookSetLookData2: Argument 1 was not a LookData. Got '$gotType'.",
                        -1
                    )
                }
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = look.obj.javaClass.name
                throw LunoRuntimeError(
                    "LookSetLookData2: Argument 0 was not a Look. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("LookGetX", 1..1) { _, args ->
            val look = args[0]

            if (look !is LunoValue.NativeObject) {
                throw LunoRuntimeError("LookGetX: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (look.obj is Look) {
                LunoValue.Number(look.obj.x.toDouble())
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = look.obj.javaClass.name
                throw LunoRuntimeError(
                    "LookGetX: Argument 0 was not a Look. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("LookGetY", 1..1) { _, args ->
            val look = args[0]

            if (look !is LunoValue.NativeObject) {
                throw LunoRuntimeError("LookGetY: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (look.obj is Look) {
                LunoValue.Number(look.obj.y.toDouble())
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = look.obj.javaClass.name
                throw LunoRuntimeError(
                    "LookGetY: Argument 0 was not a Look. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("LookGetWidth", 1..1) { _, args ->
            val look = args[0]

            if (look !is LunoValue.NativeObject) {
                throw LunoRuntimeError("LookGetWidth: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (look.obj is Look) {
                LunoValue.Number(look.obj.width.toDouble())
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = look.obj.javaClass.name
                throw LunoRuntimeError(
                    "LookGetWidth: Argument 0 was not a Look. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("LookGetHeight", 1..1) { _, args ->
            val look = args[0]

            if (look !is LunoValue.NativeObject) {
                throw LunoRuntimeError("LookGetHeight: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (look.obj is Look) {
                LunoValue.Number(look.obj.height.toDouble())
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = look.obj.javaClass.name
                throw LunoRuntimeError(
                    "LookGetHeight: Argument 0 was not a Look. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("LookGetRotation", 1..1) { _, args ->
            val look = args[0]

            if (look !is LunoValue.NativeObject) {
                throw LunoRuntimeError("LookGetRotation: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (look.obj is Look) {
                LunoValue.Number(look.obj.rotation.toDouble())
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = look.obj.javaClass.name
                throw LunoRuntimeError(
                    "LookGetRotation: Argument 0 was not a Look. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("LookGetAlpha", 1..1) { _, args ->
            val look = args[0]

            if (look !is LunoValue.NativeObject) {
                throw LunoRuntimeError("LookGetAlpha: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (look.obj is Look) {
                LunoValue.Number(look.obj.alpha.toDouble())
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = look.obj.javaClass.name
                throw LunoRuntimeError(
                    "LookGetAlpha: Argument 0 was not a Look. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("LookGetBrightness", 1..1) { _, args ->
            val look = args[0]

            if (look !is LunoValue.NativeObject) {
                throw LunoRuntimeError("LookGetBrightness: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (look.obj is Look) {
                LunoValue.Number(look.obj.brightness.toDouble())
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = look.obj.javaClass.name
                throw LunoRuntimeError(
                    "LookGetBrightness: Argument 0 was not a Look. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("LookGetHue", 1..1) { _, args ->
            val look = args[0]

            if (look !is LunoValue.NativeObject) {
                throw LunoRuntimeError("LookGetHue: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (look.obj is Look) {
                LunoValue.Number(look.obj.colorInUserInterfaceDimensionUnit.toDouble())
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = look.obj.javaClass.name
                throw LunoRuntimeError(
                    "LookGetHue: Argument 0 was not a Look. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("LookGetVisible", 1..1) { _, args ->
            val look = args[0]

            if (look !is LunoValue.NativeObject) {
                throw LunoRuntimeError("LookGetVisible: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (look.obj is Look) {
                LunoValue.Boolean(look.obj.isVisible)
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = look.obj.javaClass.name
                throw LunoRuntimeError(
                    "LookGetVisible: Argument 0 was not a Look. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("LookGetLookVisible", 1..1) { _, args ->
            val look = args[0]

            if (look !is LunoValue.NativeObject) {
                throw LunoRuntimeError("LookGetLookVisible: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (look.obj is Look) {
                LunoValue.Boolean(look.obj.isLookVisible)
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = look.obj.javaClass.name
                throw LunoRuntimeError(
                    "LookGetLookVisible: Argument 0 was not a Look. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("LookSetX", 2..2) { _, args ->
            val look = args[0]

            if (look !is LunoValue.NativeObject) {
                throw LunoRuntimeError("LookSetX: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (look.obj is Look) {
                try {
                    look.obj.x = args[1].toString().toFloat()
                } catch(e: Exception) {
                    throw LunoRuntimeError(
                        "LookSetX: Failed to convert String to Float. ${e.message}",
                        -1
                    )
                }
                LunoValue.Null
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = look.obj.javaClass.name
                throw LunoRuntimeError(
                    "LookSetX: Argument 0 was not a Look. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("LookSetY", 2..2) { _, args ->
            val look = args[0]

            if (look !is LunoValue.NativeObject) {
                throw LunoRuntimeError("LookSetY: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (look.obj is Look) {
                try {
                    look.obj.y = args[1].toString().toFloat()
                } catch(e: Exception) {
                    throw LunoRuntimeError(
                        "LookSetY: Failed to convert String to Float. ${e.message}",
                        -1
                    )
                }
                LunoValue.Null
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = look.obj.javaClass.name
                throw LunoRuntimeError(
                    "LookSetY: Argument 0 was not a Look. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("LookSetWidth", 2..2) { _, args ->
            val look = args[0]

            if (look !is LunoValue.NativeObject) {
                throw LunoRuntimeError("LookSetWidth: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (look.obj is Look) {
                try {
                    look.obj.width = args[1].toString().toFloat()
                } catch(e: Exception) {
                    throw LunoRuntimeError(
                        "LookSetWidth: Failed to convert String to Float. ${e.message}",
                        -1
                    )
                }
                LunoValue.Null
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = look.obj.javaClass.name
                throw LunoRuntimeError(
                    "LookSetWidth: Argument 0 was not a Look. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("LookSetHeight", 2..2) { _, args ->
            val look = args[0]

            if (look !is LunoValue.NativeObject) {
                throw LunoRuntimeError("LookSetHeight: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (look.obj is Look) {
                try {
                    look.obj.height = args[1].toString().toFloat()
                } catch(e: Exception) {
                    throw LunoRuntimeError(
                        "LookSetHeight: Failed to convert String to Float. ${e.message}",
                        -1
                    )
                }
                LunoValue.Null
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = look.obj.javaClass.name
                throw LunoRuntimeError(
                    "LookSetHeight: Argument 0 was not a Look. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("LookSetRotation", 2..2) { _, args ->
            val look = args[0]

            if (look !is LunoValue.NativeObject) {
                throw LunoRuntimeError("LookSetRotation: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (look.obj is Look) {
                try {
                    look.obj.rotation = args[1].toString().toFloat()
                } catch(e: Exception) {
                    throw LunoRuntimeError(
                        "LookSetRotation: Failed to convert String to Float. ${e.message}",
                        -1
                    )
                }
                LunoValue.Null
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = look.obj.javaClass.name
                throw LunoRuntimeError(
                    "LookSetRotation: Argument 0 was not a Look. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("LookSetAlpha", 2..2) { _, args ->
            val look = args[0]

            if (look !is LunoValue.NativeObject) {
                throw LunoRuntimeError("LookSetAlpha: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (look.obj is Look) {
                try {
                    look.obj.transparencyInUserInterfaceDimensionUnit = 100f - (args[1].toString().toFloat() * 100f)
                } catch(e: Exception) {
                    throw LunoRuntimeError(
                        "LookSetAlpha: Failed to convert String to Float. ${e.message}",
                        -1
                    )
                }
                LunoValue.Null
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = look.obj.javaClass.name
                throw LunoRuntimeError(
                    "LookSetAlpha: Argument 0 was not a Look. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("LookSetBrightness", 2..2) { _, args ->
            val look = args[0]

            if (look !is LunoValue.NativeObject) {
                throw LunoRuntimeError("LookSetBrightness: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (look.obj is Look) {
                try {
                    look.obj.brightnessInUserInterfaceDimensionUnit = (args[1].toString().toFloat() * 100f)
                } catch(e: Exception) {
                    throw LunoRuntimeError(
                        "LookSetBrightness: Failed to convert String to Float. ${e.message}",
                        -1
                    )
                }
                LunoValue.Null
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = look.obj.javaClass.name
                throw LunoRuntimeError(
                    "LookSetBrightness: Argument 0 was not a Look. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("LookSetHue", 2..2) { _, args ->
            val look = args[0]

            if (look !is LunoValue.NativeObject) {
                throw LunoRuntimeError("LookSetHue: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (look.obj is Look) {
                try {
                    look.obj.colorInUserInterfaceDimensionUnit = args[1].toString().toFloat()
                } catch(e: Exception) {
                    throw LunoRuntimeError(
                        "LookSetHue: Failed to convert String to Float. ${e.message}",
                        -1
                    )
                }
                LunoValue.Null
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = look.obj.javaClass.name
                throw LunoRuntimeError(
                    "LookSetHue: Argument 0 was not a Look. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("LookSetVisible", 2..2) { _, args ->
            val look = args[0]

            if (look !is LunoValue.NativeObject) {
                throw LunoRuntimeError("LookSetVisible: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (look.obj is Look) {
                try {
                    look.obj.isVisible = args[1].isTruthy()
                } catch(e: Exception) {
                    throw LunoRuntimeError(
                        "LookSetVisible: Failed to convert String to Float. ${e.message}",
                        -1
                    )
                }
                LunoValue.Null
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = look.obj.javaClass.name
                throw LunoRuntimeError(
                    "LookSetVisible: Argument 0 was not a Look. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("LookSetLookVisible", 2..2) { _, args ->
            val look = args[0]

            if (look !is LunoValue.NativeObject) {
                throw LunoRuntimeError("LookSetLookVisible: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (look.obj is Look) {
                try {
                    look.obj.isLookVisible = args[1].isTruthy()
                } catch(e: Exception) {
                    throw LunoRuntimeError(
                        "LookSetLookVisible: Failed to convert String to Float. ${e.message}",
                        -1
                    )
                }
                LunoValue.Null
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = look.obj.javaClass.name
                throw LunoRuntimeError(
                    "LookSetLookVisible: Argument 0 was not a Look. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("SpriteGetScriptList", 1..1) { _, args ->
            val sprite = args[0]

            if (sprite !is LunoValue.NativeObject) {
                throw LunoRuntimeError("SpriteGetScriptList: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (sprite.obj is Sprite) {
                LunoValue.List(
                    sprite.obj.scriptList.map { scene ->
                        LunoValue.NativeObject(scene) // Оборачиваем каждую сцену
                    }.toMutableList()
                )
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = sprite.obj.javaClass.name
                throw LunoRuntimeError(
                    "SpriteGetScriptList: Argument 0 was not a Sprite. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("SpriteGetLookList", 1..1) { _, args ->
            val sprite = args[0]

            if (sprite !is LunoValue.NativeObject) {
                throw LunoRuntimeError("SpriteGetLookList: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (sprite.obj is Sprite) {
                LunoValue.List(
                    sprite.obj.lookList.map { scene ->
                        LunoValue.NativeObject(scene) // Оборачиваем каждую сцену
                    }.toMutableList()
                )
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = sprite.obj.javaClass.name
                throw LunoRuntimeError(
                    "SpriteGetLookList: Argument 0 was not a Sprite. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("SpriteGetSoundList", 1..1) { _, args ->
            val sprite = args[0]

            if (sprite !is LunoValue.NativeObject) {
                throw LunoRuntimeError("SpriteGetSoundList: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (sprite.obj is Sprite) {
                LunoValue.List(
                    sprite.obj.soundList.map { scene ->
                        LunoValue.NativeObject(scene) // Оборачиваем каждую сцену
                    }.toMutableList()
                )
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = sprite.obj.javaClass.name
                throw LunoRuntimeError(
                    "SpriteGetSoundList: Argument 0 was not a Sprite. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("SpriteGetUserVariables", 1..1) { _, args ->
            val sprite = args[0]

            if (sprite !is LunoValue.NativeObject) {
                throw LunoRuntimeError("SpriteGetUserVariables: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (sprite.obj is Sprite) {
                LunoValue.List(
                    sprite.obj.userVariables.map { scene ->
                        LunoValue.NativeObject(scene) // Оборачиваем каждую сцену
                    }.toMutableList()
                )
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = sprite.obj.javaClass.name
                throw LunoRuntimeError(
                    "SpriteGetUserVariables: Argument 0 was not a Sprite. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("SpriteGetUserLists", 1..1) { _, args ->
            val sprite = args[0]

            if (sprite !is LunoValue.NativeObject) {
                throw LunoRuntimeError("SpriteGetUserLists: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (sprite.obj is Sprite) {
                LunoValue.List(
                    sprite.obj.userLists.map { scene ->
                        LunoValue.NativeObject(scene) // Оборачиваем каждую сцену
                    }.toMutableList()
                )
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = sprite.obj.javaClass.name
                throw LunoRuntimeError(
                    "SpriteGetUserLists: Argument 0 was not a Sprite. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("ActionFactory", 0..0) { _, args ->
            LunoValue.NativeObject(ActionFactory())
        }

        defineNative("SpriteGetActionFactory", 1..1) { _, args ->
            val sprite = args[0]

            if (sprite !is LunoValue.NativeObject) {
                throw LunoRuntimeError("SpriteGetActionFactory: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (sprite.obj is Sprite) {
                LunoValue.NativeObject(sprite.obj.actionFactory)
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = sprite.obj.javaClass.name
                throw LunoRuntimeError(
                    "SpriteGetActionFactory: Argument 0 was not a Sprite. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("SpriteIsClone", 1..1) { _, args ->
            val sprite = args[0]

            if (sprite !is LunoValue.NativeObject) {
                throw LunoRuntimeError("SpriteIsClone: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (sprite.obj is Sprite) {
                LunoValue.Boolean(sprite.obj.isClone)
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = sprite.obj.javaClass.name
                throw LunoRuntimeError(
                    "SpriteIsClone: Argument 0 was not a Sprite. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("SpriteOriginal", 1..1) { _, args ->
            val sprite = args[0]

            if (sprite !is LunoValue.NativeObject) {
                throw LunoRuntimeError("SpriteOriginal: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (sprite.obj is Sprite) {
                LunoValue.NativeObject(sprite.obj.myOriginal)
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = sprite.obj.javaClass.name
                throw LunoRuntimeError(
                    "SpriteOriginal: Argument 0 was not a Sprite. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("SpriteIsGliding", 1..1) { _, args ->
            val sprite = args[0]

            if (sprite !is LunoValue.NativeObject) {
                throw LunoRuntimeError("SpriteIsClone: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (sprite.obj is Sprite) {
                LunoValue.Boolean(sprite.obj.isGliding)
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = sprite.obj.javaClass.name
                throw LunoRuntimeError(
                    "SpriteIsClone: Argument 0 was not a Sprite. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("SpriteGetAllBricks", 1..1) { _, args ->
            val sprite = args[0]

            if (sprite !is LunoValue.NativeObject) {
                throw LunoRuntimeError("SpriteGetAllBricks: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (sprite.obj is Sprite) {
                LunoValue.List(
                    sprite.obj.allBricks.map { scene ->
                        LunoValue.NativeObject(scene) // Оборачиваем каждую сцену
                    }.toMutableList()
                )
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = sprite.obj.javaClass.name
                throw LunoRuntimeError(
                    "SpriteGetAllBricks: Argument 0 was not a Sprite. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("SpriteGetUserVariable", 2..2) { _, args ->
            val sprite = args[0]

            if (sprite !is LunoValue.NativeObject) {
                throw LunoRuntimeError("SpriteGetUserVariable: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (sprite.obj is Sprite) {
                LunoValue.NativeObject(sprite.obj.getUserVariable(args[1].toString()))
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = sprite.obj.javaClass.name
                throw LunoRuntimeError(
                    "SpriteGetUserVariable: Argument 0 was not a Sprite. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("SpriteAddUserVariable", 2..2) { _, args ->
            val sprite = args[0]

            if (sprite !is LunoValue.NativeObject) {
                throw LunoRuntimeError("SpriteAddUserVariable: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (sprite.obj is Sprite) {
                val variable = args[1]

                if (variable !is LunoValue.NativeObject) {
                    throw LunoRuntimeError("SpriteAddUserVariable: Argument 1 must be a NativeObject.", -1)
                }

                // Каждый элемент должен быть NativeObject, содержащим Scene
                if (variable.obj is UserVariable) {
                    sprite.obj.addUserVariable(variable.obj)
                    LunoValue.Null
                } else {
                    // Ошибка: элемент списка не того типа
                    val gotType = sprite.obj.javaClass.name
                    throw LunoRuntimeError(
                        "SpriteAddUserVariable: Argument 1 was not a UserVariable. Got '$gotType'.",
                        -1
                    )
                }
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = sprite.obj.javaClass.name
                throw LunoRuntimeError(
                    "SpriteIsClone: Argument 0 was not a Sprite. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("SpriteAddUserList", 2..2) { _, args ->
            val sprite = args[0]

            if (sprite !is LunoValue.NativeObject) {
                throw LunoRuntimeError("SpriteAddUserList: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (sprite.obj is Sprite) {
                val variable = args[0]

                if (variable !is LunoValue.NativeObject) {
                    throw LunoRuntimeError("SpriteAddUserList: Argument 1 must be a NativeObject.", -1)
                }

                // Каждый элемент должен быть NativeObject, содержащим Scene
                if (variable.obj is UserList) {
                    sprite.obj.addUserList(variable.obj)
                    LunoValue.Null
                } else {
                    // Ошибка: элемент списка не того типа
                    val gotType = sprite.obj.javaClass.name
                    throw LunoRuntimeError(
                        "SpriteAddUserList: Argument 1 was not a UserList. Got '$gotType'.",
                        -1
                    )
                }
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = sprite.obj.javaClass.name
                throw LunoRuntimeError(
                    "SpriteAddUserList: Argument 0 was not a Sprite. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("SpriteGetUserList", 2..2) { _, args ->
            val sprite = args[0]

            if (sprite !is LunoValue.NativeObject) {
                throw LunoRuntimeError("SpriteGetUserList: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (sprite.obj is Sprite) {
                LunoValue.NativeObject(sprite.obj.getUserList(args[1].toString()))
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = sprite.obj.javaClass.name
                throw LunoRuntimeError(
                    "SpriteGetUserList: Argument 0 was not a Sprite. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("SpriteAddScript", 2..3) { _, args ->
            val sprite = args[0]

            if (sprite !is LunoValue.NativeObject) {
                throw LunoRuntimeError("SpriteAddScript: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (sprite.obj is Sprite) {
                val variable = args[1]

                if (variable !is LunoValue.NativeObject) {
                    throw LunoRuntimeError("SpriteAddScript: Argument 1 must be a NativeObject.", -1)
                }

                // Каждый элемент должен быть NativeObject, содержащим Scene
                if (variable.obj is Script) {
                    if(args.size == 2) {
                        sprite.obj.addScript(variable.obj)
                    } else {
                        try {
                            sprite.obj.addScript(args[2].toString().toInt(), variable.obj)
                        } catch (e: Exception) {
                            throw LunoRuntimeError("SpriteAddScript: Failed to convert String to Int.", -1)
                        }
                    }
                    LunoValue.Null
                } else {
                    // Ошибка: элемент списка не того типа
                    val gotType = sprite.obj.javaClass.name
                    throw LunoRuntimeError(
                        "SpriteAddScript: Argument 1 was not a Script. Got '$gotType'.",
                        -1
                    )
                }
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = sprite.obj.javaClass.name
                throw LunoRuntimeError(
                    "SpriteAddScript: Argument 0 was not a Sprite. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("SpriteGetScript", 2..2) { _, args ->
            val sprite = args[0]

            if (sprite !is LunoValue.NativeObject) {
                throw LunoRuntimeError("SpriteGetScript: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (sprite.obj is Sprite) {
                try {
                    LunoValue.NativeObject(sprite.obj.getScript(args[1].toString().toInt()))
                } catch (e: Exception) {
                    throw LunoRuntimeError("SpriteGetScript: Failed to convert String to Int.", -1)
                }
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = sprite.obj.javaClass.name
                throw LunoRuntimeError(
                    "SpriteGetScript: Argument 0 was not a Sprite. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("SpriteRemoveAllScripts", 1..1) { _, args ->
            val sprite = args[0]

            if (sprite !is LunoValue.NativeObject) {
                throw LunoRuntimeError("SpriteRemoveAllScripts: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (sprite.obj is Sprite) {
                sprite.obj.removeAllScripts()
                LunoValue.Null
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = sprite.obj.javaClass.name
                throw LunoRuntimeError(
                    "SpriteRemoveAllScripts: Argument 0 was not a Sprite. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("SpriteRemoveScript", 2..2) { _, args ->
            val sprite = args[0]

            if (sprite !is LunoValue.NativeObject) {
                throw LunoRuntimeError("SpriteRemoveScript: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (sprite.obj is Sprite) {
                val script = args[1]

                if (script !is LunoValue.NativeObject) {
                    throw LunoRuntimeError("SpriteRemoveScript: Argument 1 must be a NativeObject.", -1)
                }

                // Каждый элемент должен быть NativeObject, содержащим Scene
                if (script.obj is Script) {
                    sprite.obj.removeScript(script.obj)
                    LunoValue.Null
                } else {
                    // Ошибка: элемент списка не того типа
                    val gotType = sprite.obj.javaClass.name
                    throw LunoRuntimeError(
                        "SpriteRemoveScript: Argument 1 was not a Script. Got '$gotType'.",
                        -1
                    )
                }
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = sprite.obj.javaClass.name
                throw LunoRuntimeError(
                    "SpriteRemoveScript: Argument 0 was not a Sprite. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("SpriteIsBackground", 1..1) { _, args ->
            val sprite = args[0]

            if (sprite !is LunoValue.NativeObject) {
                throw LunoRuntimeError("SpriteRemoveScript: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (sprite.obj is Sprite) {
                LunoValue.Boolean(sprite.obj.isBackgroundSprite)
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = sprite.obj.javaClass.name
                throw LunoRuntimeError(
                    "SpriteRemoveScript: Argument 0 was not a Sprite. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("ScriptGetBrickList", 1..1) { _, args ->
            val script = args[0]

            if (script !is LunoValue.NativeObject) {
                throw LunoRuntimeError("ScriptGetBrickList: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (script.obj is Script) {
                LunoValue.List(
                    script.obj.brickList.map { scene ->
                        LunoValue.NativeObject(scene) // Оборачиваем каждую сцену
                    }.toMutableList()
                )
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = script.obj.javaClass.name
                throw LunoRuntimeError(
                    "ScriptGetBrickList: Argument 0 was not a Script. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("ScriptRun", 3..3) { _, args ->
            val script = args[0]

            if (script !is LunoValue.NativeObject) {
                throw LunoRuntimeError("ScriptGetBrickList: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (script.obj is Script) {
                val sprite = args[1]

                if (sprite !is LunoValue.NativeObject) {
                    throw LunoRuntimeError("ScriptGetBrickList: Argument 1 must be a NativeObject.", -1)
                }

                // Каждый элемент должен быть NativeObject, содержащим Scene
                if (sprite.obj is Sprite) {
                    val sequence = args[2]

                    if (sequence !is LunoValue.NativeObject) {
                        throw LunoRuntimeError("ScriptGetBrickList: Argument 2 must be a NativeObject.", -1)
                    }

                    // Каждый элемент должен быть NativeObject, содержащим Scene
                    if (sequence.obj is ScriptSequenceAction) {
                        script.obj.run(sprite.obj, sequence.obj)
                        LunoValue.Null
                    } else {
                        // Ошибка: элемент списка не того типа
                        val gotType = sequence.obj.javaClass.name
                        throw LunoRuntimeError(
                            "ScriptGetBrickList: Argument 2 was not a ScriptSequenceAction. Got '$gotType'.",
                            -1
                        )
                    }
                } else {
                    // Ошибка: элемент списка не того типа
                    val gotType = sprite.obj.javaClass.name
                    throw LunoRuntimeError(
                        "ScriptGetBrickList: Argument 1 was not a Sprite. Got '$gotType'.",
                        -1
                    )
                }
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = script.obj.javaClass.name
                throw LunoRuntimeError(
                    "ScriptGetBrickList: Argument 0 was not a Script. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("GetSequence", 1..1) { _, args ->
            val script = args[0]

            if (script !is LunoValue.NativeObject) {
                throw LunoRuntimeError("GetSequence: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (script.obj is org.catrobat.catroid.content.Scope) {
                if(script.obj.sequence != null) {
                    LunoValue.NativeObject(script.obj.sequence)
                } else {
                    LunoValue.Null
                }
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = script.obj.javaClass.name
                throw LunoRuntimeError(
                    "GetSequence: Argument 0 was not a Scope. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("ScriptSequenceAction", 1..1) { _, args ->
            val script = args[0]

            if (script !is LunoValue.NativeObject) {
                throw LunoRuntimeError("ScriptSequenceAction: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (script.obj is Script) {
                LunoValue.NativeObject(ScriptSequenceAction(script.obj))
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = script.obj.javaClass.name
                throw LunoRuntimeError(
                    "ScriptSequenceAction: Argument 0 was not a Script. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("ScriptAddBrick", 2..3) { _, args ->
            val script = args[0]

            if (script !is LunoValue.NativeObject) {
                throw LunoRuntimeError("ScriptAddBrick: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (script.obj is Script) {
                val sprite = args[1]

                if (sprite !is LunoValue.NativeObject) {
                    throw LunoRuntimeError("ScriptAddBrick: Argument 1 must be a NativeObject.", -1)
                }

                // Каждый элемент должен быть NativeObject, содержащим Scene
                if (sprite.obj is Brick) {
                    if(args.size == 2) {
                        script.obj.addBrick(sprite.obj)
                    } else {
                        try {
                            script.obj.addBrick(args[2].toString().toInt(), sprite.obj)
                        } catch (e: Exception) {
                            throw LunoRuntimeError(
                                "ScriptAddBrick: Failed to convert String to Int.",
                                -1
                            )
                        }
                    }
                    LunoValue.Null
                } else {
                    // Ошибка: элемент списка не того типа
                    val gotType = sprite.obj.javaClass.name
                    throw LunoRuntimeError(
                        "ScriptAddBrick: Argument 1 was not a Brick. Got '$gotType'.",
                        -1
                    )
                }
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = script.obj.javaClass.name
                throw LunoRuntimeError(
                    "ScriptAddBrick: Argument 0 was not a Script. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("ScriptRemoveBrick", 2..2) { _, args ->
            val script = args[0]

            if (script !is LunoValue.NativeObject) {
                throw LunoRuntimeError("ScriptRemoveBrick: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (script.obj is Script) {
                val sprite = args[1]

                if (sprite !is LunoValue.NativeObject) {
                    throw LunoRuntimeError("ScriptRemoveBrick: Argument 1 must be a NativeObject.", -1)
                }

                // Каждый элемент должен быть NativeObject, содержащим Scene
                if (sprite.obj is Brick) {
                    script.obj.removeBrick(sprite.obj)
                    LunoValue.Null
                } else {
                    // Ошибка: элемент списка не того типа
                    val gotType = sprite.obj.javaClass.name
                    throw LunoRuntimeError(
                        "ScriptRemoveBrick: Argument 1 was not a Brick. Got '$gotType'.",
                        -1
                    )
                }
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = script.obj.javaClass.name
                throw LunoRuntimeError(
                    "ScriptRemoveBrick: Argument 0 was not a Script. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("ScriptGetBrick", 2..2) { _, args ->
            val script = args[0]

            if (script !is LunoValue.NativeObject) {
                throw LunoRuntimeError("ScriptGetBrick: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (script.obj is Script) {
                try {
                    LunoValue.NativeObject(script.obj.getBrick(args[1].toString().toInt()))
                } catch (e: Exception) {
                    throw LunoRuntimeError(
                        "ScriptGetBrick: Failed to convert String to Int.",
                        -1
                    )
                }
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = script.obj.javaClass.name
                throw LunoRuntimeError(
                    "ScriptGetBrick: Argument 0 was not a Script. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("UserVariableGetValue", 1..1) { _, args ->
            val script = args[0]

            if (script !is LunoValue.NativeObject) {
                throw LunoRuntimeError("UserVariableGetValue: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (script.obj is UserVariable) {
                LunoValue.String(script.obj.value.toString())
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = script.obj.javaClass.name
                throw LunoRuntimeError(
                    "UserVariableGetValue: Argument 0 was not a UserVariable. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("UserVariableSetValue", 2..2) { _, args ->
            val script = args[0]

            if (script !is LunoValue.NativeObject) {
                throw LunoRuntimeError("UserVariableSetValue: Argument 0 must be a NativeObject.", -1)
            }

            // Каждый элемент должен быть NativeObject, содержащим Scene
            if (script.obj is UserVariable) {
                script.obj.value = args[1].toString()
                LunoValue.Null
            } else {
                // Ошибка: элемент списка не того типа
                val gotType = script.obj.javaClass.name
                throw LunoRuntimeError(
                    "UserVariableSetValue: Argument 0 was not a UserVariable. Got '$gotType'.",
                    -1
                )
            }
        }

        defineNative("RunBrick", 3..3) { _, args ->
            val brickArgLuno = args[0]   // Аргумент для Brick
            val spriteArgLuno = args[1]  // Аргумент для Sprite
            val sequenceArgLuno = args[2] // Аргумент для ScriptSequenceAction

            // Проверка и извлечение Brick
            if (brickArgLuno !is LunoValue.NativeObject) {
                throw LunoRuntimeError("RunBrick: Argument 0 (brick) must be a NativeObject.", -1)
            }
            // Проверяем, что обернутый объект является экземпляром Brick (или его наследником)
            if (brickArgLuno.obj is Brick) { // Используй свой базовый класс Brick
                val brickInstance = brickArgLuno.obj // Smart cast к Brick

                // Проверка и извлечение Sprite
                if (spriteArgLuno !is LunoValue.NativeObject) {
                    throw LunoRuntimeError("RunBrick: Argument 1 (sprite) must be a NativeObject.", -1)
                }
                // Проверяем, что обернутый объект является экземпляром Sprite
                if (spriteArgLuno.obj is Sprite) { // Используй свой класс Sprite
                    val spriteInstance = spriteArgLuno.obj // Smart cast к Sprite

                    // Проверка и извлечение ScriptSequenceAction
                    if (sequenceArgLuno !is LunoValue.NativeObject) {
                        throw LunoRuntimeError("RunBrick: Argument 2 (sequence) must be a NativeObject.", -1)
                    }
                    // Проверяем, что обернутый объект является экземпляром ScriptSequenceAction
                    if (sequenceArgLuno.obj is ScriptSequenceAction) { // Используй свой класс ScriptSequenceAction
                        val sequenceInstance = sequenceArgLuno.obj // Smart cast

                        // Теперь все типы проверены и объекты извлечены
                        try {
                            brickInstance.addActionToSequence(spriteInstance, sequenceInstance)
                            return@defineNative LunoValue.Null
                        } catch (e: Exception) {
                            throw LunoRuntimeError("RunBrick: Error during addActionToSequence for '${brickInstance::class.simpleName}': ${e.message}", -1, e)
                        }

                    } else {
                        val gotType = sequenceArgLuno.obj?.javaClass?.name ?: "null"
                        throw LunoRuntimeError("RunBrick: Argument 2 (sequence) was not a ScriptSequenceAction. Got '$gotType'.", -1)
                    }
                } else {
                    val gotType = spriteArgLuno.obj?.javaClass?.name ?: "null"
                    // Вот здесь была твоя ошибка в сообщении, должно быть "Argument 1 was not a Sprite"
                    throw LunoRuntimeError("RunBrick: Argument 1 (sprite) was not a Sprite. Got '$gotType'.", -1)
                }
            } else {
                val gotType = brickArgLuno.obj?.javaClass?.name ?: "null"
                throw LunoRuntimeError("RunBrick: Argument 0 (brick) was not a Brick. Got '$gotType'.", -1)
            }
        }

        defineNative("Brick", 2..2) { _, args -> // Ожидаем 2 аргумента: имя блока (String) и параметры (List)
            val brickNameLuno = args[0]
            val paramsLunoList = args[1] // Это LunoValue.List, содержащий параметры для конструктора блока

            if (brickNameLuno !is LunoValue.String) {
                throw LunoRuntimeError("Brick: First argument (brick name) must be a String.", -1)
            }
            val brickName = brickNameLuno.value.toLowerCase() // Получаем Kotlin String и приводим к нижнему регистру

            if (paramsLunoList !is LunoValue.List) {
                throw LunoRuntimeError("Brick: Second argument (parameters) must be a List.", -1)
            }
            val constructorArgs: List<LunoValue> = paramsLunoList.elements // Список LunoValue аргументов

            // --- Логика создания конкретных блоков ---
            val brickInstance: Brick = when (brickName) {
                "showtoastblock" -> { // Сравниваем чистое имя
                    if (constructorArgs.isEmpty() || constructorArgs[0] !is LunoValue.String) {
                        throw LunoRuntimeError("ShowToastBlock expects a String message as its first parameter.", -1)
                    }
                    val message = (constructorArgs[0] as LunoValue.String).value
                    ShowToastBlock(message) // Предполагаем, что такой конструктор есть
                }
                "addeditbrick" -> {
                    val name = (constructorArgs[0] as LunoValue.String).value
                    val text = (constructorArgs[1] as LunoValue.String).value

                    AddEditBrick(name, text)
                }
                "additemtouserlistbrick" -> {
                    val name = (constructorArgs[0] as LunoValue.Number).value

                    AddItemToUserListBrick(name)
                }
                "addradiobrick" -> {
                    val name = (constructorArgs[0] as LunoValue.String).value
                    val text = (constructorArgs[1] as LunoValue.String).value

                    AddRadioBrick(name, text)
                }
                "arduinosenddigitalvaluebrick" -> {
                    val name = (constructorArgs[0] as LunoValue.Number).value.toInt()
                    val text = (constructorArgs[1] as LunoValue.Number).value.toInt()

                    ArduinoSendDigitalValueBrick(name, text)
                }
                "arduinosendpwmvaluebrick" -> {
                    val name = (constructorArgs[0] as LunoValue.Number).value.toInt()
                    val text = (constructorArgs[1] as LunoValue.Number).value.toInt()

                    ArduinoSendPWMValueBrick(name, text)
                }
                "askbrick" -> {
                    val text = (constructorArgs[0] as LunoValue.String).value
                    val uservar = (constructorArgs[1] as UserVariable)

                    AskBrick(Formula(text), uservar)
                }
                "askgemini2brick" -> {
                    val name = (constructorArgs[0] as LunoValue.String).value
                    val text = (constructorArgs[1] as LunoValue.String).value
                    val uservar = (constructorArgs[2] as UserVariable)

                    AskGemini2Brick(Formula(name), Formula(text), uservar)
                }
                "askgeminibrick" -> {
                    val text = (constructorArgs[0] as LunoValue.String).value
                    val uservar = (constructorArgs[1] as UserVariable)

                    AskGeminiBrick(Formula(text), uservar)
                }
                "askgptbrick" -> {
                    //brick now is broken
                    AskGPTBrick()
                }
                "askspeechbrick" -> {
                    CloneBrick()
                }
                "clonebrick" -> {
                    val text = (constructorArgs[0] as LunoValue.String).value
                    val uservar = (constructorArgs[1] as UserVariable)

                    AskGeminiBrick(Formula(text), uservar)
                }
                else -> {
                    throw LunoRuntimeError("Brick: Unknown Brick ID: '${brickNameLuno.value}'.", -1)
                }
            }

            LunoValue.NativeObject(brickInstance)
        }
    }

    private fun toKotlinType(value: LunoValue): Any? {
        return when (value) {
            is LunoValue.Null -> null
            is LunoValue.Number -> value.value    // LunoValue.Number -> Double
            is LunoValue.String -> value.value    // LunoValue.String -> String
            is LunoValue.Boolean -> value.value   // LunoValue.Boolean -> Boolean
            is LunoValue.List -> value.elements.map { toKotlinType(it) }.toMutableList() // LunoValue.List -> MutableList<Any?>
            is LunoValue.LunoObject -> value.fields.mapValues { toKotlinType(it.value) }.toMutableMap() // LunoValue.LunoObject -> MutableMap<String, Any?>
            is LunoValue.NativeObject -> value.obj // LunoValue.NativeObject -> Any (исходный обернутый объект)
            is LunoValue.Callable -> value.toString() // Или другое представление, т.к. Callable не имеет прямого Kotlin-эквивалента для данных
        }
    }

    // Вспомогательная функция для маппинга (можно разместить в Interpreter или в компаньоне InternTokenType)
    fun mapStringToInternTokenType(typeName: String): InternTokenType {
        return when (typeName.toUpperCase()) { // Приводим к верхнему регистру для нечувствительности к регистру
            "STRING" -> InternTokenType.STRING
            "NUMBER" -> InternTokenType.NUMBER
            "BOOLEAN" -> InternTokenType.STRING
            // Добавь другие типы по необходимости
            else -> {
                println("Warning: Unknown parameter type name '$typeName', defaulting to UNKNOWN.")
                InternTokenType.STRING // или throw LunoRuntimeError(...)
            }
        }
    }

    fun getKotlinIntFromLunoValue(lunoVal: LunoValue, functionNameForError: String, argPositionForError: Int): Int {
        if (lunoVal is LunoValue.Number) {
            val doubleValue = lunoVal.value
            // Проверка, не выходит ли значение за пределы Int (опционально, но хорошо для надежности)
            if (doubleValue < Int.MIN_VALUE || doubleValue > Int.MAX_VALUE) {
                throw LunoRuntimeError("$functionNameForError: Number value $doubleValue for argument $argPositionForError is out of Int range.", -1)
            }
            return doubleValue.toInt() // Отбрасывает дробную часть
        } else {
            throw LunoRuntimeError("$functionNameForError: Argument $argPositionForError must be a Number to be converted to Int. Got ${lunoVal::class.simpleName}.", -1)
        }
    }

    fun getProjectFromLunoValue(lunoArg: LunoValue): Project? {

        // Шаг 1: Проверяем, что это LunoValue.NativeObject
        if (lunoArg is LunoValue.NativeObject) {
            // Шаг 2: Извлекаем обернутый Kotlin-объект
            val wrappedKotlinObject: Any = lunoArg.obj

            // Шаг 3: Проверяем, является ли обернутый объект экземпляром твоего класса Scope
            // Замени org.catrobat.catroid.content.Scope на реальный импортированный класс Scope
            if (wrappedKotlinObject is org.catrobat.catroid.content.Project) { // Убедись, что Scope импортирован или указано полное имя
                // Шаг 4: Безопасно приводим тип
                val scopeInstance = wrappedKotlinObject as org.catrobat.catroid.content.Project

                return scopeInstance
            } else {
                // Ошибка: обернутый объект не того типа, который мы ожидали
                throw LunoRuntimeError(
                    "GetSprite: Argument 0's native object is not a Scope. Got type: ${wrappedKotlinObject?.javaClass?.name ?: "null"}.",
                    -1 // Номер строки -1 для нативных функций, если не можем определить точнее
                )
                return null
            }
        } else {
            // Ошибка: аргумент не является LunoValue.NativeObject
            throw LunoRuntimeError(
                "GetSprite: Argument 0 must be a NativeObject containing a Scope. Got type: ${lunoArg::class.simpleName}.",
                -1
            )
            return null
        }
    }

    private fun defineNative(name: String, arity: IntRange, function: RawNativeLunoFunction) {
        globals.define(name, LunoValue.NativeCallable(CallableNativeLunoFunction(name, arity, function)))
    }

    fun interpret(program: ProgramNode) {
        /*try {
            program.statements.forEach { execute(it) }
        } catch (error: LunoRuntimeError) {
            System.err.println("Runtime Error (LunoScript): ${error.message} on line ${error.line}")
            error.cause?.let { System.err.println("Caused by: ${it.localizedMessage}") }
            // throw error // re-throw for the engine to catch if needed
        } catch (ret: ReturnSignal) {
            System.err.println("Runtime Error (LunoScript): 'return' outside of function.")
        } catch (br: BreakSignal) {
            System.err.println("Runtime Error (LunoScript): 'break' outside of loop.")
        } catch (cont: ContinueSignal) {
            System.err.println("Runtime Error (LunoScript): 'continue' outside of loop.")
        }*/
        program.statements.forEach { execute(it) }
    }

    private fun execute(statement: Statement) {
        // Проверка прерываний (для отладки, если нужно)
        // if (Thread.currentThread().isInterrupted) throw InterruptedException("LunoScript execution interrupted")

        when (statement) {
            is ExpressionStatement -> evaluate(statement.expression)
            is VarDeclarationStatement -> executeVarDeclaration(statement)
            is AssignmentStatement -> executeAssignment(statement)
            is BlockStatement -> executeBlock(statement.statements, Scope(currentScope))
            is IfStatement -> executeIfStatement(statement)
            is WhileStatement -> executeWhileStatement(statement)
            is ForInStatement -> executeForInStatement(statement)
            is SwitchStatement -> executeSwitchStatement(statement)
            is FunDeclarationStatement -> executeFunDeclaration(statement)
            is ReturnStatement -> executeReturnStatement(statement)
            is BreakStatement -> executeBreakStatement(statement)
            is ContinueStatement -> executeContinueStatement(statement)
            is ClassDeclarationStatement -> executeClassDeclaration(statement)
        }
    }

    fun executeBlock(statements: List<Statement>, environment: Scope) {
        val previousScope = this.currentScope
        try {
            this.currentScope = environment
            for (statement in statements) {
                execute(statement)
            }
        } finally {
            this.currentScope = previousScope
        }
    }

    private fun executeVarDeclaration(stmt: VarDeclarationStatement) {
        val value = stmt.initializer?.let { evaluate(it) } ?: LunoValue.Null
        currentScope.define(stmt.name.lexeme, value)
    }

    private fun executeAssignment(stmt: AssignmentStatement) {
        val target = stmt.target
        val valueToAssign: LunoValue

        // Handle compound assignment operators (+=, -=, etc.)
        if (stmt.operatorToken.type != TokenType.ASSIGN) {
            val currentValue = when (target) {
                is VariableExpr -> lookUpVariable(target.name, target)
                is GetExpr -> evaluateGetExpr(target, allowUndefined = false) // Ensure property/index exists for read
                is IndexAccessExpr -> evaluateIndexAccessExpr(target, allowUndefined = false)
                else -> throw LunoRuntimeError("Invalid target for compound assignment.", stmt.line)
            }
            val rightValue = evaluate(stmt.value)
            valueToAssign = evaluateCompoundAssignment(currentValue, stmt.operatorToken, rightValue, stmt.line)
        } else {
            valueToAssign = evaluate(stmt.value)
        }

        when (target) {
            is VariableExpr -> currentScope.assign(target.name, valueToAssign)
            is GetExpr -> { // obj.property = value
                val objValue = evaluate(target.obj)
                if (objValue is LunoValue.LunoObject) {
                    objValue.fields[target.name.lexeme] = valueToAssign
                } else if (objValue is LunoValue.NativeObject && objValue.obj is MutableMap<*,*>) {
                    try {
                        @Suppress("UNCHECKED_CAST")
                        (objValue.obj as MutableMap<String, Any?>)[target.name.lexeme] = toKotlinType(valueToAssign)
                    } catch (e: Exception) {
                        throw LunoRuntimeError("Failed to set property on native map: ${e.message}", target.line)
                    }
                }
                else {
                    throw LunoRuntimeError("Can only assign properties to Luno objects or native mutable maps. Got ${objValue::class.simpleName}.", target.line)
                }
            }
            is IndexAccessExpr -> { // list[index] = value
                val collection = evaluate(target.callee)
                val indexVal = evaluate(target.index)

                when (collection) {
                    is LunoValue.List -> {
                        if (indexVal is LunoValue.Number) {
                            val idx = indexVal.value.toInt()
                            if (idx >= 0 && idx < collection.elements.size) {
                                collection.elements[idx] = valueToAssign
                            } else if (idx == collection.elements.size) { // Allow append
                                collection.elements.add(valueToAssign)
                            }
                            else {
                                throw LunoRuntimeError("Index $idx out of bounds for list of size ${collection.elements.size}.", target.line)
                            }
                        } else {
                            throw LunoRuntimeError("List index must be a number.", target.line)
                        }
                    }
                    is LunoValue.LunoObject -> { // map["key"] = value
                        val key = lunoValueToString(indexVal, humanReadable = false) // Use raw string for keys
                        collection.fields[key] = valueToAssign
                    }
                    is LunoValue.NativeObject -> { // if native object is a map
                        if (collection.obj is MutableMap<*,*>) {
                            try {
                                @Suppress("UNCHECKED_CAST")
                                val nativeMap = collection.obj as MutableMap<Any?, Any?>
                                nativeMap[toKotlinType(indexVal)] = toKotlinType(valueToAssign)
                            } catch (e: Exception) {
                                throw LunoRuntimeError("Failed to set value in native map by index/key: ${e.message}", target.line)
                            }
                        } else {
                            throw LunoRuntimeError("Native object does not support indexed assignment.", target.line)
                        }
                    }
                    else -> throw LunoRuntimeError("Target for indexed assignment must be a list, map or supported native collection.", target.line)
                }
            }
            else -> throw LunoRuntimeError("Invalid assignment target.", stmt.line)
        }
    }


    private fun evaluateCompoundAssignment(current: LunoValue, op: Token, right: LunoValue, line: Int): LunoValue {
        return when(op.type) {
            TokenType.PLUS_ASSIGN -> operateBinary(current, Token(TokenType.PLUS, "+", null, line, 0), right, line)
            TokenType.MINUS_ASSIGN -> operateBinary(current, Token(TokenType.MINUS, "-", null, line, 0), right, line)
            TokenType.MULTIPLY_ASSIGN -> operateBinary(current, Token(TokenType.MULTIPLY, "*", null, line, 0), right, line)
            TokenType.DIVIDE_ASSIGN -> operateBinary(current, Token(TokenType.DIVIDE, "/", null, line, 0), right, line)
            TokenType.MODULO_ASSIGN -> operateBinary(current, Token(TokenType.MODULO, "%", null, line, 0), right, line)
            else -> throw LunoRuntimeError("Unsupported compound assignment operator: ${op.lexeme}", line)
        }
    }


    private fun executeIfStatement(stmt: IfStatement) {
        if (evaluate(stmt.condition).isTruthy()) {
            execute(stmt.thenBranch)
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch)
        }
    }

    private fun executeWhileStatement(stmt: WhileStatement) {
        loopDepth++
        try {
            while (evaluate(stmt.condition).isTruthy()) {
                try {
                    execute(stmt.body)
                } catch (_: ContinueSignal) {
                    // Continue to next iteration
                }
            }
        } catch (_: BreakSignal) {
            // Break out of loop
        } finally {
            loopDepth--
        }
    }

    private fun executeForInStatement(stmt: ForInStatement) {
        loopDepth++
        val iterableValue = evaluate(stmt.iterable)
        val itemsToIterate: Collection<LunoValue> = when (iterableValue) {
            is LunoValue.List -> iterableValue.elements
            is LunoValue.String -> iterableValue.value.map { LunoValue.String(it.toString()) }
            // TODO: Add iteration for LunoObject fields (keys, values, items) or native maps
            else -> throw LunoRuntimeError("'for-in' loop requires an iterable (list, string). Got ${iterableValue::class.simpleName}.", stmt.forToken.line)
        }

        try {
            for (item in itemsToIterate) {
                // Create a new scope for each iteration or just define the variable in the current loop's scope.
                // For simplicity, let's define in current scope for this iteration.
                // A new inner scope would be cleaner: val loopVarScope = Scope(currentScope)
                currentScope.define(stmt.variable.lexeme, item) // Define loop variable
                try {
                    execute(stmt.body)
                } catch (_: ContinueSignal) { /* Continue to next item */ }
            }
        } catch (_: BreakSignal) { /* Break out of loop */ }
        finally {
            loopDepth--
        }
    }

    private fun executeSwitchStatement(stmt: SwitchStatement) {
        val switchExprValue = evaluate(stmt.expression)
        var matched = false

        for (caseClause in stmt.cases) {
            if (matched && !caseClause.isDefault) continue // After a match, only look for break or end of switch unless fall-through is implemented

            var currentCaseMatches = false
            if (caseClause.isDefault) {
                currentCaseMatches = true // Default always matches if no prior match or if it's the one being checked
            } else {
                for (caseValueExpr in caseClause.valueExpressions!!) { // Not null if not default
                    if (isEqual(switchExprValue, evaluate(caseValueExpr))) {
                        currentCaseMatches = true
                        break
                    }
                }
            }

            if (currentCaseMatches) {
                matched = true
                try {
                    // LunoScript switch does not have implicit fall-through like C. Each case is distinct.
                    // If fall-through is desired, it would need to be explicit or 'break' would be optional.
                    // For now, execute the body of the matched case and that's it for this switch.
                    execute(caseClause.body)
                    return // Exit switch statement after executing the first matched case body
                } catch (s: BreakSignal) {
                    return // 'break' exits the switch
                }
                // No implicit fall-through, so if a case body completes without break, switch ends.
            }
        }
    }


    private fun executeFunDeclaration(stmt: FunDeclarationStatement) {
        val function = LunoValue.LunoFunction(stmt, currentScope) // Capture current scope as closure
        currentScope.define(stmt.name.lexeme, function)
    }

    private fun executeReturnStatement(stmt: ReturnStatement) {
        val value = stmt.value?.let { evaluate(it) } ?: LunoValue.Null
        throw ReturnSignal(value)
    }

    private fun executeBreakStatement(stmt: BreakStatement) {
        if (loopDepth == 0) throw LunoRuntimeError("'break' is not inside a loop.", stmt.line)
        throw BreakSignal
    }

    private fun executeContinueStatement(stmt: ContinueStatement) {
        if (loopDepth == 0) throw LunoRuntimeError("'continue' is not inside a loop.", stmt.line)
        throw ContinueSignal
    }

    private fun executeClassDeclaration(stmt: ClassDeclarationStatement) {
        val methods = stmt.methods.associate {
            it.name.lexeme to LunoValue.LunoFunction(it, currentScope) // Methods capture class scope
        }
        val lunoClass = LunoValue.LunoClass(stmt.name.lexeme, methods)
        currentScope.define(stmt.name.lexeme, lunoClass)
    }


    // --- Evaluation of Expressions ---
    private fun evaluate(expr: Expression): LunoValue {
        return when (expr) {
            is LiteralExpr -> expr.value
            is VariableExpr -> lookUpVariable(expr.name, expr)
            is ThisExpr -> lookUpVariable(expr.keyword, expr) // 'this' is looked up like a variable
            is BinaryExpr -> operateBinary(evaluate(expr.left), expr.operator, evaluate(expr.right), expr.line)
            is LogicalExpr -> evaluateLogicalExpr(expr)
            is UnaryExpr -> evaluateUnaryExpr(expr)
            is CallExpr -> evaluateCallExpr(expr)
            is GetExpr -> evaluateGetExpr(expr)
            is IndexAccessExpr -> evaluateIndexAccessExpr(expr)
            is ListLiteralExpr -> LunoValue.List(expr.elements.map { evaluate(it) }.toMutableList())
            is MapLiteralExpr -> {
                val mapFields = expr.entries.mapValues { evaluate(it.value) }.toMutableMap()
                val fieldsWithStringKeys = mapFields.mapKeys {
                    // Если ключ был STRING_LITERAL, берем его значение, иначе lexeme IDENTIFIER
                    if (it.key.type == TokenType.STRING_LITERAL) (it.key.literal as kotlin.String) else it.key.lexeme
                }.toMutableMap()
                LunoValue.LunoObject(null, fieldsWithStringKeys)
            }

            // SetExpr is not evaluated directly, it's part of AssignmentStatement logic
            else -> throw LunoRuntimeError("Cannot evaluate AST node of type ${expr::class.simpleName}", expr.line)
        }
    }

    // Placeholder for variable lookup, potentially using resolved distances
    private fun lookUpVariable(nameToken: Token, exprNode: AstNode): LunoValue {
        // For 'this', it must be defined in the current or an enclosing function/method scope
        if (nameToken.lexeme == "this") {
            // Search for 'this' defined by a method call scope
            var scope: Scope? = currentScope
            while (scope != null) {
                if (scope.values.containsKey("this")) return scope.values["this"]!!
                scope = scope.enclosing
            }
            throw LunoRuntimeError("'this' can only be used inside a method.", nameToken.line)
        }
        return currentScope.get(nameToken) // Simple lexical scoping for now
    }


    private fun operateBinary(left: LunoValue, op: Token, right: LunoValue, line: Int): LunoValue {
        // Type Coercion and Operation Logic
        return when (op.type) {
            // Arithmetic
            TokenType.PLUS -> when {
                left is LunoValue.Number && right is LunoValue.Number -> LunoValue.Number(left.value + right.value)
                left is LunoValue.String || right is LunoValue.String -> LunoValue.String(lunoValueToString(left) + lunoValueToString(right))
                left is LunoValue.List && right is LunoValue.List -> LunoValue.List((left.elements + right.elements).toMutableList())
                else -> throw LunoRuntimeError("Operands for '+' must be numbers, strings, or lists. Got ${left::class.simpleName} and ${right::class.simpleName}", line)
            }
            TokenType.MINUS -> numOp(left, right, line, Double::minus)
            TokenType.MULTIPLY -> when {
                left is LunoValue.Number && right is LunoValue.Number -> LunoValue.Number(left.value * right.value)
                left is LunoValue.String && right is LunoValue.Number -> LunoValue.String(left.value.repeat(right.value.toInt().coerceAtLeast(0)))
                left is LunoValue.Number && right is LunoValue.String -> LunoValue.String(right.value.repeat(left.value.toInt().coerceAtLeast(0)))
                left is LunoValue.List && right is LunoValue.Number -> {
                    val repeatedList = mutableListOf<LunoValue>()
                    repeat(right.value.toInt().coerceAtLeast(0)) { repeatedList.addAll(left.elements) }
                    LunoValue.List(repeatedList)
                }
                else -> throw LunoRuntimeError("Operands for '*' must be two numbers, or string/list and number. Got ${left::class.simpleName} and ${right::class.simpleName}", line)
            }
            TokenType.DIVIDE -> {
                val r = right as? LunoValue.Number ?: throw LunoRuntimeError("Right operand for '/' must be a number.", line)
                if (r.value == 0.0) throw LunoRuntimeError("Division by zero.", line)
                numOp(left, right, line, Double::div)
            }
            TokenType.MODULO -> {
                val rVal = right as? LunoValue.Number ?: throw LunoRuntimeError("Right operand for '%' must be a number.", line)
                if (rVal.value == 0.0) throw LunoRuntimeError("Modulo by zero.", line)
                numOp(left, right, line, Double::rem)
            }

            // Comparison
            TokenType.GT -> cmpOp(left, right, line) { l, r -> l > r }
            TokenType.GTE -> cmpOp(left, right, line) { l, r -> l >= r }
            TokenType.LT -> cmpOp(left, right, line) { l, r -> l < r }
            TokenType.LTE -> cmpOp(left, right, line) { l, r -> l <= r }
            TokenType.EQ -> LunoValue.Boolean(isEqual(left, right))
            TokenType.NEQ -> LunoValue.Boolean(!isEqual(left, right))

            else -> throw LunoRuntimeError("Unknown binary operator: ${op.lexeme}", line)
        }
    }

    private fun evaluateLogicalExpr(expr: LogicalExpr): LunoValue {
        val left = evaluate(expr.left)
        if (expr.operator.type == TokenType.OR) {
            if (left.isTruthy()) return left // Short-circuit OR
        } else { // AND
            if (!left.isTruthy()) return left // Short-circuit AND
        }
        return evaluate(expr.right)
    }


    private fun evaluateUnaryExpr(expr: UnaryExpr): LunoValue {
        val right = evaluate(expr.right)
        return when (expr.operator.type) {
            TokenType.BANG -> LunoValue.Boolean(!right.isTruthy())
            TokenType.MINUS -> {
                if (right is LunoValue.Number) LunoValue.Number(-right.value)
                else throw LunoRuntimeError("Operand for unary '-' must be a number. Got ${right::class.simpleName}", expr.line)
            }
            else -> throw LunoRuntimeError("Unknown unary operator: ${expr.operator.lexeme}", expr.line)
        }
    }

    private fun evaluateCallExpr(expr: CallExpr): LunoValue {
        val callee = evaluate(expr.callee)
        // ДОБАВЬ ЛОГ ЗДЕСЬ:
        android.util.Log.d("LunoCallExpr", "Calling ${expr.callee}, evaluated callee type: ${callee::class.simpleName}, value: $callee")
        val arguments = expr.arguments.map { evaluate(it) }

        if (callee !is LunoValue.Callable) {
            android.util.Log.e("LunoCallExpr", "ERROR: Callee is not LunoValue.Callable! It is: $callee of type ${callee::class.simpleName}")
            throw LunoRuntimeError("Can only call functions, methods, or classes. Got ${callee::class.simpleName}.", expr.line)
        }

        // Arity check is now inside LunoValue.Callable types (LunoFunction, NativeCallable, LunoClass)
        // We pass expr.parenToken to use its line number for arity error reporting if needed
        // For LunoFunction or LunoClass (constructor), 'this' binding happens within their 'call'
        val result = callee.call(this, arguments, expr.parenToken)
        // ДОБАВЬ ЛОГ ЗДЕСЬ:
        android.util.Log.d("LunoCallExpr", "Call to ${expr.callee} returned: $result (type: ${result::class.simpleName})")
        return result
    }


    private fun evaluateGetExpr(expr: GetExpr, allowUndefined: Boolean = true): LunoValue {
        val objValue = evaluate(expr.obj)
        when (objValue) {
            is LunoValue.LunoObject -> {
                // Field access
                if (objValue.fields.containsKey(expr.name.lexeme)) {
                    return objValue.fields[expr.name.lexeme]!!
                }
                // Method access (bind 'this')
                objValue.klass?.findMethod(expr.name.lexeme)?.let { method ->
                    val boundMethod = method.copy(closure = Scope(method.closure).apply { define("this", objValue) })
                    return boundMethod
                }
                if (allowUndefined) return LunoValue.Null // Or throw error: "Undefined property or method..."
                throw LunoRuntimeError("Undefined property or method '${expr.name.lexeme}' on object.", expr.line)
            }
            is LunoValue.String -> { // String methods/properties
                return when(expr.name.lexeme) {
                    "length" -> LunoValue.Number(objValue.value.length.toDouble())
                    // "toUpperCase", "toLowerCase", "substring" etc. could be native functions taking string as first arg
                    // or special bound methods.
                    else -> if (allowUndefined) LunoValue.Null else throw LunoRuntimeError("Undefined property '${expr.name.lexeme}' on String.", expr.line)
                }
            }
            is LunoValue.List -> { // List methods/properties
                return when(expr.name.lexeme) {
                    "length" -> LunoValue.Number(objValue.elements.size.toDouble())
                    // "add", "remove", "get" could be methods
                    else -> if (allowUndefined) LunoValue.Null else throw LunoRuntimeError("Undefined property '${expr.name.lexeme}' on List.", expr.line)
                }
            }
            is LunoValue.NativeObject -> { // Accessing properties of native objects (e.g. from a Map)
                if (objValue.obj is Map<*,*>) {
                    @Suppress("UNCHECKED_CAST")
                    val map = objValue.obj as Map<String, Any?>
                    return LunoValue.fromKotlin(map[expr.name.lexeme])
                }
                // Could use reflection for Java object properties, but that's complex and slow.
                if (allowUndefined) return LunoValue.Null
                throw LunoRuntimeError("Cannot get property '${expr.name.lexeme}' from native object of type ${objValue.obj::class.simpleName}.", expr.line)
            }
            else -> throw LunoRuntimeError("Can only access properties or methods on objects, strings, or lists. Got ${objValue::class.simpleName}.", expr.line)
        }
    }

    private fun evaluateIndexAccessExpr(expr: IndexAccessExpr, allowUndefined: Boolean = true): LunoValue {
        val collection = evaluate(expr.callee)
        val indexVal = evaluate(expr.index)

        return when (collection) {
            is LunoValue.List -> {
                if (indexVal is LunoValue.Number) {
                    val idx = indexVal.value.toInt()
                    collection.elements.getOrNull(idx) ?: if (allowUndefined) LunoValue.Null else throw LunoRuntimeError("Index $idx out of bounds.", expr.line)
                } else {
                    throw LunoRuntimeError("List index must be a number.", expr.line)
                }
            }
            is LunoValue.String -> { // string[index]
                if (indexVal is LunoValue.Number) {
                    val idx = indexVal.value.toInt()
                    collection.value.getOrNull(idx)?.let { LunoValue.String(it.toString()) } ?: LunoValue.Null
                } else {
                    throw LunoRuntimeError("String index must be a number.", expr.line)
                }
            }
            is LunoValue.LunoObject -> { // map["key"]
                val key = lunoValueToString(indexVal, humanReadable = false)
                collection.fields[key] ?: if (allowUndefined) LunoValue.Null else throw LunoRuntimeError("Key '$key' not found in object.", expr.line)
            }
            is LunoValue.NativeObject -> { // if native object is a map or list
                when (collection.obj) {
                    is kotlin.collections.List<*> -> {
                        if (indexVal is LunoValue.Number) {
                            val idx = indexVal.value.toInt()
                            LunoValue.fromKotlin(collection.obj.getOrNull(idx))
                        } else throw LunoRuntimeError("Native list index must be a number.", expr.line)
                    }
                    is Map<*,*> -> {
                        @Suppress("UNCHECKED_CAST")
                        val nativeMap = collection.obj as Map<Any?, Any?>
                        LunoValue.fromKotlin(nativeMap[toKotlinType(indexVal)])
                    }
                    else -> if (allowUndefined) LunoValue.Null else throw LunoRuntimeError("Native object does not support indexed access.", expr.line)
                }
            }
            else -> throw LunoRuntimeError("Can only perform index access on lists, strings, or map-like objects. Got ${collection::class.simpleName}.", expr.line)
        }
    }


    // --- Helpers for operations ---
    private fun numOp(l: LunoValue, r: LunoValue, line: Int, op: (Double, Double) -> Double): LunoValue.Number {
        val leftN = l as? LunoValue.Number ?: throw LunoRuntimeError("Left operand must be a number. Got ${l::class.simpleName}", line)
        val rightN = r as? LunoValue.Number ?: throw LunoRuntimeError("Right operand must be a number. Got ${r::class.simpleName}", line)
        return LunoValue.Number(op(leftN.value, rightN.value))
    }

    private fun cmpOp(l: LunoValue, r: LunoValue, line: Int, op: (Double, Double) -> Boolean): LunoValue.Boolean {
        // For now, only compare numbers. String comparison could be added.
        val leftN = l as? LunoValue.Number ?: throw LunoRuntimeError("Left operand for comparison must be a number. Got ${l::class.simpleName}", line)
        val rightN = r as? LunoValue.Number ?: throw LunoRuntimeError("Right operand for comparison must be a number. Got ${r::class.simpleName}", line)
        return LunoValue.Boolean(op(leftN.value, rightN.value))
    }

    private fun isEqual(a: LunoValue, b: LunoValue): Boolean {
        return when {
            a is LunoValue.Null && b is LunoValue.Null -> true
            // If one is null and the other isn't, they are not equal (unless a === b handles this, but explicit is clearer)
            a is LunoValue.Null || b is LunoValue.Null -> false
            a is LunoValue.Number && b is LunoValue.Number -> a.value == b.value
            a is LunoValue.String && b is LunoValue.String -> a.value == b.value
            a is LunoValue.Boolean && b is LunoValue.Boolean -> a.value == b.value
            // For lists, objects, callables: reference equality by default. Deep equality is more complex.
            a is LunoValue.List && b is LunoValue.List -> a.elements == b.elements // Simple deep compare for lists of simple values
            a is LunoValue.LunoObject && b is LunoValue.LunoObject -> a.fields == b.fields // Simple deep compare for objects
            // NativeObjects are compared by their wrapped object's equality or reference.
            // For simple native types from fromKotlin, '==' might work if their 'equals' is well-defined.
            a is LunoValue.NativeObject && b is LunoValue.NativeObject -> a.obj == b.obj
            // Callables (functions, classes, native callables) are typically compared by reference.
            a is LunoValue.Callable && b is LunoValue.Callable -> a === b
            else -> false // Different LunoValue types are generally not equal
        }
    }

    private fun lunoValueToString(value: LunoValue?, humanReadable: Boolean = false): String {
        return when (value) {
            null -> "null"
            is LunoValue.Null -> "null"
            is LunoValue.Number -> value.toString()
            is LunoValue.String -> if (humanReadable) value.value else value.toLunoScriptString()
            is LunoValue.Boolean -> value.value.toString()
            is LunoValue.List -> value.elements.joinToString(
                prefix = "[",
                postfix = "]",
                separator = ", "
            ) { lunoValueToString(it, humanReadable) }

            is LunoValue.LunoObject -> {
                value.klass?.name?.let { "<$it instance>" } ?: value.fields.entries.joinToString(
                    prefix = "{",
                    postfix = "}",
                    separator = ", "
                ) {
                    "${it.key}: ${lunoValueToString(it.value, humanReadable)}"
                }
            }

            is LunoValue.Callable -> value.toString() // LunoFunction, LunoClass, NativeCallable имеют свой toString
            is LunoValue.NativeObject -> "<NativeObject: ${value.obj::class.simpleName}>"
        }
    }



}