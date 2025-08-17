// org/catrobat/catroid/content/actions/CustomAction.kt
package org.catrobat.catroid.content.actions

import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.libraries.CustomBrickDefinition
import org.catrobat.catroid.libraries.LibraryManager
import org.catrobat.catroid.utils.lunoscript.*

class CustomAction : TemporalAction() {
    // Используем lateinit, т.к. эти поля будут установлены через сеттеры из ActionFactory
    lateinit var scope: Scope
    lateinit var definition: CustomBrickDefinition
    lateinit var parameterFormulas: List<Formula>

    // Метод, который выполняет основную логику.
    // percent от 0 до 1. Для одномоментного действия он выполнится один раз.
    override fun update(percent: Float) {
        // Находим нужную библиотеку и ее интерпретатор
        val library = LibraryManager.getLoadedLibrary(definition.ownerLibraryId)
        if (library == null) {
            // Можно показать Toast или записать в лог
            Log.e("CustomAction", "Библиотека ${definition.ownerLibraryId} не найдена для блока ${definition.id}")
            return
        }
        val interpreter = library.interpreter

        try {
            // 1. Ищем Luno-функцию
            val funcToken = Token(TokenType.IDENTIFIER, definition.lunoFunctionName, null, -1, -1)
            val funcValue = interpreter.globals.get(funcToken) // Используем get() из Scope
            val lunoFunction = funcValue as? LunoValue.Callable
                ?: throw LunoRuntimeError("Функция '${definition.lunoFunctionName}' не найдена в библиотеке")

            // 2. Готовим аргументы
            val lunoArgs = mutableListOf<LunoValue>()

            // Первым всегда передаем текущий спрайт
            lunoArgs.add(LunoValue.NativeObject(scope.sprite))

            // Вычисляем формулы параметров и конвертируем их в LunoValue
            parameterFormulas.forEach { formula ->
                val result = formula.interpretObject(scope) // Вычисляем формулу в текущем scope
                lunoArgs.add(LunoValue.fromKotlin(result))
            }

            // 3. Вызываем Luno-функцию!
            Thread {
                val eofToken = Token(TokenType.EOF, "", null, -1, -1)
                lunoFunction.call(interpreter, lunoArgs, eofToken)
            }

        } catch (e: PauseExecutionSignal) {
            // Если Luno-функция вызвала delay(), выполнение прервется здесь.
            // Catroid продолжит выполнение других Action, а затем вернется к нашему.
            // ВАЖНО: нужно, чтобы наш Action не завершился сразу.
            // Мы можем добавить логику, чтобы Action ждал завершения асинхронной операции.
            // Для этого Luno-функция могла бы возвращать специальный объект-ожидание.
            // Но для начала простого выполнения этого достаточно.
        } catch (e: LunoRuntimeError) {
            Log.e("CustomAction", "Ошибка выполнения скрипта из блока ${definition.id}: ${e.message}", e)
            // TODO: Показать Toast с ошибкой
        }
    }
}