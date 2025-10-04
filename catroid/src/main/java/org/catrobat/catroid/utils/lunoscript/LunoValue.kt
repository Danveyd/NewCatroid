package org.catrobat.catroid.utils.lunoscript

import android.annotation.SuppressLint
import android.content.Context
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import java.io.File

// Wrapper для всех значений в LunoScript
sealed class LunoValue {
    object Null : LunoValue() {
        override fun toString(): kotlin.String = "null"
    }

    data class Number(val value: Double) : LunoValue() {
        override fun toString(): kotlin.String = if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
    }

    data class String(val value: kotlin.String) : LunoValue() {
        override fun toString(): kotlin.String = value // Для print и MakeToast лучше без кавычек
        fun toLunoScriptString(): kotlin.String = "\"$value\"" // Для внутреннего представления
    }

    data class Boolean(val value: kotlin.Boolean) : LunoValue() {
        override fun toString(): kotlin.String = value.toString()
    }

    data class NativeClass(val klass: Class<*>) : Callable() {
        // Arity конструктора заранее неизвестен, поэтому разрешаем любое количество аргументов.
        // Более сложная версия могла бы проверять все доступные конструкторы.
        override fun arity(): IntRange = 0..Int.MAX_VALUE

        // Вызов этого значения (например, Pixmap(width, height)) - это вызов конструктора
        @SuppressLint("NewApi")
        override fun call(interpreter: Interpreter, arguments: kotlin.collections.List<LunoValue>, callSiteToken: Token): LunoValue {
            val constructors = klass.constructors

            val matchingConstructor = constructors.find { it.parameterCount == arguments.size }
                ?: throw LunoRuntimeError(
                    "No constructor for class '${klass.simpleName}' found with ${arguments.size} arguments.",
                    callSiteToken.line
                )

            try {
                // --- НОВОЕ: ИСПОЛЬЗУЕМ КОНВЕРТЕР ИНТЕРПРЕТАТОРА ---
                val kotlinArgs = arguments.mapIndexed { index, lunoValue ->
                    val targetType = matchingConstructor.parameterTypes[index]
                    // Вызываем метод интерпретатора для конвертации
                    interpreter.lunoValueToKotlin(lunoValue, targetType)
                }.toTypedArray()
                // --- КОНЕЦ ИЗМЕНЕНИЙ ---

                val newInstance = matchingConstructor.newInstance(*kotlinArgs)
                return NativeObject(newInstance)

            } catch (e: Exception) {
                throw LunoRuntimeError(
                    "Failed to construct '${klass.simpleName}': ${e.cause?.message ?: e.message}",
                    callSiteToken.line,
                    e
                )
            }
        }

        override fun toString(): kotlin.String = "<native class ${klass.name}>"
    }

    fun lunoValueToKotlin(value: LunoValue, targetClass: Class<*>): Any? {
        // 1. Сначала обрабатываем null.
        if (value is LunoValue.Null) {
            // Если параметр метода может быть null, рефлексия примет null.
            // Если не может - она сама выбросит ошибку, что корректно.
            return null
        }

        // 2. Быстрый путь: если у нас уже есть NativeObject с объектом нужного типа.
        if (value is LunoValue.NativeObject && targetClass.isInstance(value.obj)) {
            return value.obj
        }

        // 3. --- ГЛАВНОЕ ИСПРАВЛЕНИЕ ---
        // Специальная обработка для com.badlogic.gdx.files.FileHandle.
        // isAssignableFrom() проверяет, можно ли объекту типа targetClass присвоить FileHandle.
        if (FileHandle::class.java.isAssignableFrom(targetClass)) {
            return when (value) {
                // Если в скрипте передали строку-путь...
                is LunoValue.String -> Gdx.files.absolute(value.value)
                // Если в скрипте передали NativeObject, содержащий java.io.File...
                is LunoValue.NativeObject -> {
                    if (value.obj is File) {
                        Gdx.files.absolute(value.obj.absolutePath)
                    } else {
                        null // Не смогли конвертировать NativeObject в FileHandle
                    }
                }
                else -> null // Другие типы LunoValue не можем превратить в FileHandle
            }
        }

        // 4. Обработка для стандартного java.io.File (если понадобится)
        if (File::class.java.isAssignableFrom(targetClass)) {
            if (value is LunoValue.String) {
                return File(value.value)
            }
        }

        // 5. Обработка базовых типов, как и раньше.
        val result = when (targetClass.name) {
            "java.lang.String", "kotlin.String" -> (value as? LunoValue.String)?.value
            "int", "java.lang.Integer" -> (value as? LunoValue.Number)?.value?.toInt()
            "double", "java.lang.Double" -> (value as? LunoValue.Number)?.value
            "float", "java.lang.Float" -> (value as? LunoValue.Number)?.value?.toFloat()
            "boolean", "java.lang.Boolean" -> (value as? LunoValue.Boolean)?.value
            else -> null // Остальные типы пока не поддерживаем для авто-конвертации
        }

        if (result != null) {
            return result
        }

        // 6. Если ни одно из правил не сработало, выбрасываем понятную ошибку.
        val valueTypeName = if (value is LunoValue.NativeObject) value.obj::class.java.simpleName else value::class.simpleName
        throw LunoRuntimeError("Cannot convert LunoValue type `${valueTypeName}` to native type `${targetClass.simpleName}`")
    }

    data class List(val elements: MutableList<LunoValue>) : LunoValue() {
        override fun toString(): kotlin.String = elements.joinToString(prefix = "[", postfix = "]", separator = ", ") { it.toString() }
    }

    data class Float(val value: kotlin.Float) : LunoValue() {
        // Переопределяем toString, чтобы он выводил 'f' на конце для наглядности
        override fun toString(): kotlin.String = "${value}f"
    }

    abstract class Callable : LunoValue() {
        abstract fun arity(): IntRange
        abstract fun call(interpreter: Interpreter, arguments: kotlin.collections.List<LunoValue>, callSiteToken: Token): LunoValue
    }

    // Обертка для нативных функций (из вашего старого кода, но теперь точно соответствует Callable)
    data class NativeCallable(val callable: CallableNativeLunoFunction) : Callable() {
        override fun arity(): IntRange = callable.arity

        override fun call(interpreter: Interpreter, arguments: kotlin.collections.List<LunoValue>, callSiteToken: Token): LunoValue {
            if (arguments.size !in callable.arity) {
                val expected = if (callable.arity.first == callable.arity.last) callable.arity.first.toString() else "${callable.arity.first}..${callable.arity.last}"
                throw LunoRuntimeError("Native function '${callable.name}' expected $expected arguments, but got ${arguments.size}.", callSiteToken.line)
            }
            try {
                return callable.function.invoke(interpreter, arguments)
            } catch (e: Exception) {
                throw LunoRuntimeError("Error in native function '${callable.name}': ${e.cause?.message ?: e.message}", callSiteToken.line, e)
            }
        }
    }

    // Обертка для функций, определенных в самом LunoScript
    data class LunoFunction(val declaration: FunDeclarationStatement, val closure: Scope) : Callable() {
        override fun arity(): IntRange = declaration.params.size..declaration.params.size

        override fun call(interpreter: Interpreter, arguments: kotlin.collections.List<LunoValue>, callSiteToken: Token): LunoValue {
            val environment = Scope(closure)
            for (i in declaration.params.indices) {
                environment.define(declaration.params[i].lexeme, arguments.getOrElse(i) { Null })
            }
            try {
                interpreter.executeBlock(declaration.body.statements, environment)
            } catch (returnValue: ReturnSignal) {
                return returnValue.value
            }
            return Null
        }
    }

    // Обертка для "связанного" метода, который мы вызываем на объекте
    data class BoundMethod(val instance: Any, val methodName: String) : Callable() {
        // Количество аргументов заранее неизвестно, поэтому разрешаем любое
        override fun arity(): IntRange = 0..Int.MAX_VALUE

        override fun call(interpreter: Interpreter, arguments: kotlin.collections.List<LunoValue>, callSiteToken: Token): LunoValue {
            return interpreter.callNativeMethod(instance, methodName.toString(), arguments, callSiteToken.line)
        }
    }

    data class BoundMethod2(val receiver: LunoObject, val method: LunoFunction) : Callable() {
        override fun arity(): IntRange = method.arity()

        override fun call(interpreter: Interpreter, arguments: kotlin.collections.List<LunoValue>, callSiteToken: Token): LunoValue {
            // Создаем окружение для вызова, НО его родителем будет замыкание МЕТОДА
            val environment = Scope(method.closure)
            // ГЛАВНОЕ: определяем 'this' в этом окружении
            environment.define("this", receiver)

            // Остальная логика копируется из LunoFunction.call
            for (i in method.declaration.params.indices) {
                environment.define(method.declaration.params[i].lexeme, arguments.getOrElse(i) { Null })
            }
            try {
                interpreter.executeBlock(method.declaration.body.statements, environment)
            } catch (returnValue: ReturnSignal) {
                return returnValue.value
            }
            return Null
        }
    }

    // Для объектов LunoScript (экземпляров классов) и простых карт
    data class LunoObject(
        val klass: LunoClass?, // Ссылка на класс, если это экземпляр класса
        val fields: MutableMap<kotlin.String, LunoValue> = mutableMapOf()
    ) : LunoValue() {
        override fun toString(): kotlin.String {
            return klass?.name?.let { "$it instance" } ?: fields.toString()
        }
    }

    // Для классов LunoScript
    data class LunoClass(
        val name: kotlin.String,
        val methods: Map<kotlin.String, LunoFunction>, // Пока методы - это LunoFunction
        val superclass: LunoClass? = null // Для будущего наследования
    ) : Callable() { // Класс сам по себе вызываемый (как конструктор)
        override fun arity(): IntRange { // Arity конструктора (если есть init)
            methods["init"]?.let { return it.arity() }
            return 0..0 // По умолчанию конструктор без аргументов
        }
        override fun call(interpreter: Interpreter, arguments: kotlin.collections.List<LunoValue>, callSiteToken: Token): LunoValue {
            val instance = LunoObject(this) // this здесь - это LunoClass
            methods["init"]?.let { initMethod ->
                // Нужно создать временное окружение для вызова init, привязанное к instance
                val initClosure = Scope(initMethod.closure) // Используем замыкание метода init
                initClosure.define("this", instance) // Связываем 'this'
                // Вызов init-метода (LunoFunction)
                val environment = Scope(initClosure)
                for (i in initMethod.declaration.params.indices) {
                    environment.define(initMethod.declaration.params[i].lexeme, arguments.getOrElse(i) { Null })
                }
                try {
                    interpreter.executeBlock(initMethod.declaration.body.statements, environment)
                } catch (retSignal: ReturnSignal) {
                    // init не должен возвращать значение явно, но если вернет, оно игнорируется, кроме null.
                    if (retSignal.value !is Null) {
                        throw LunoRuntimeError("'init' method should not return a value.", initMethod.declaration.name.line)
                    }
                }

            }
            return instance
        }
        fun findMethod(name: kotlin.String): LunoFunction? {
            return methods[name] ?: superclass?.findMethod(name)
        }
        override fun toString(): kotlin.String = "<class $name>"
    }


    // Обертка для нативных Kotlin/Java объектов, как Context
    data class NativeObject(val obj: Any) : LunoValue() {
        override fun toString(): kotlin.String = "NativeObject(${obj::class.simpleName})"
    }


    fun isTruthy(): kotlin.Boolean {
        return when (this) {
            is Null -> false
            is Boolean -> this.value
            is Number -> this.value != 0.0
            is String -> this.value.isNotEmpty()
            is List -> this.elements.isNotEmpty()
            is LunoObject -> true // Объекты всегда truthy
            is Callable -> true // Функции/классы truthy
            is NativeObject -> true
            is BoundMethod -> true
            is Float -> this.value != 0f
        }
    }

    companion object {
        fun fromKotlin(value: Any?): LunoValue {
            return when (value) {
                null -> Null
                is kotlin.Number -> Number(value.toDouble())
                is kotlin.String -> String(value)
                is kotlin.Boolean -> Boolean(value)
                is LunoValue -> value // Уже LunoValue
                is Context -> NativeObject(value)
                is kotlin.collections.List<*> -> List(value.map { fromKotlin(it) }.toMutableList())
                is Map<*, *> -> {
                    val fields = value.entries.associate { (k, v) ->
                        k.toString() to fromKotlin(v)
                    }.toMutableMap()
                    LunoObject(null, fields)
                }
                is CallableNativeLunoFunction -> NativeCallable(value)
                else -> NativeObject(value)
            }
        }
    }
}