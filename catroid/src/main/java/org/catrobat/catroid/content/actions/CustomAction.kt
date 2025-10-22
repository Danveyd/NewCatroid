package org.catrobat.catroid.content.actions

import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.libraries.CustomBrickDefinition
import org.catrobat.catroid.libraries.LibraryManager
import org.catrobat.catroid.utils.lunoscript.*

class CustomAction : TemporalAction() {
    lateinit var scope: Scope
    lateinit var definition: CustomBrickDefinition
    lateinit var parameterFormulas: List<Formula>

    override fun update(percent: Float) {
        val library = LibraryManager.getLoadedLibrary(definition.ownerLibraryId)
        if (library == null) {
            Log.e("CustomAction", "Библиотека ${definition.ownerLibraryId} не найдена для блока ${definition.id}")
            return
        }
        val interpreter = library.interpreter

        try {
            val funcToken = Token(TokenType.IDENTIFIER, definition.lunoFunctionName, null, -1, -1)
            val funcValue = interpreter.globals.get(funcToken)
            val lunoFunction = funcValue as? LunoValue.Callable
                ?: throw LunoRuntimeError("Функция '${definition.lunoFunctionName}' не найдена в библиотеке")

            val lunoArgs = mutableListOf<LunoValue>()

            lunoArgs.add(LunoValue.NativeObject(scope.sprite))

            parameterFormulas.forEach { formula ->
                val result = formula.interpretObject(scope)
                lunoArgs.add(LunoValue.fromKotlin(result))
            }

            Thread {
                val eofToken = Token(TokenType.EOF, "", null, -1, -1)
                lunoFunction.call(interpreter, lunoArgs, eofToken)
            }

        } catch (e: PauseExecutionSignal) {
            // TODO: Сделать паузы
        } catch (e: LunoRuntimeError) {
            Log.e("CustomAction", "Ошибка выполнения скрипта из блока ${definition.id}: ${e.message}", e)
        }
    }
}