package org.catrobat.catroid.utils.lunoscript

import android.content.Context

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

    data class List(val elements: MutableList<LunoValue>) : LunoValue() {
        override fun toString(): kotlin.String = elements.joinToString(prefix = "[", postfix = "]", separator = ", ") { it.toString() }
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


    // Для функций, определенных в LunoScript
    abstract class Callable : LunoValue() {
        abstract fun arity(): IntRange
        abstract fun call(interpreter: Interpreter, arguments: kotlin.collections.List<LunoValue>, callSiteToken: Token): LunoValue
    }

    // Для пользовательских функций LunoScript
    data class LunoFunction(
        val declaration: FunDeclarationStatement,
        val closure: Scope // Замыкание, где функция была определена
    ) : Callable() {
        override fun arity(): IntRange = declaration.params.size..declaration.params.size
        override fun call(interpreter: Interpreter, arguments: kotlin.collections.List<LunoValue>, callSiteToken: Token): LunoValue {
            val environment = Scope(closure) // Новая область видимости для функции, наследующая от замыкания
            for (i in declaration.params.indices) {
                environment.define(declaration.params[i].lexeme, arguments.getOrElse(i) { Null })
            }
            try {
                interpreter.executeBlock(declaration.body.statements, environment)
            } catch (returnValue: ReturnSignal) {
                return returnValue.value
            }
            return Null // Неявный return null, если нет явного return
        }
        override fun toString(): kotlin.String = "<fun ${declaration.name.lexeme}>"
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
            val instance = LunoObject(this)
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

    // Обертка для вызываемых нативных функций (замена typealias)
    data class NativeCallable(val callable: CallableNativeLunoFunction) : Callable() {
        override fun arity(): IntRange = callable.arity
        override fun call(interpreter: Interpreter, arguments: kotlin.collections.List<LunoValue>, callSiteToken: Token): LunoValue {
            // Вызываем function из объекта callable, а не несуществующий call
            if (arguments.size !in callable.arity) {
                val expectedArity = if (callable.arity.first == callable.arity.last) callable.arity.first.toString() else "${callable.arity.first}..${callable.arity.last}"
                throw LunoRuntimeError(
                    "Native function '${callable.name}' expected $expectedArity arguments, but got ${arguments.size}.",
                    callSiteToken.line
                )
            }
            try {
                return callable.function.invoke(interpreter, arguments)
            } catch (e: LunoRuntimeError) {
                throw e
            } catch (e: Exception) {
                throw LunoRuntimeError("Error in native function '${callable.name}': ${e.message ?: e.javaClass.simpleName}", callSiteToken.line, e)
            }
        }
        override fun toString(): kotlin.String = "<native fun ${callable.name}>"
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