package org.catrobat.catroid.utils.lunoscript // Замени на свой пакет

// Сделаем LunoException открытым
open class LunoException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

// Сделаем LunoSyntaxError открытым
open class LunoSyntaxError(
    message: String,
    val line: Int = -1,
    val position: Int = -1
) : LunoException("Syntax Error${if (line != -1) " at line $line, pos $position" else ""}: $message")

open class LunoRuntimeError( // Также сделаем open на всякий случай
    message: String,
    val line: Int = -1,
    cause: Throwable? = null
) : LunoException("Runtime Error${if (line != -1) " at line $line" else ""}: $message", cause)

// Эти остаются RuntimeException, т.к. мы их не наследуем, а только бросаем
class ReturnSignal(val value: LunoValue) : RuntimeException(null, null, false, false)
object BreakSignal : RuntimeException(null, null, false, false)
object ContinueSignal : RuntimeException(null, null, false, false)