package org.catrobat.catroid.formulaeditor

import android.util.Log
import com.danvexteam.lunoscript_annotations.LunoClass

// Убедитесь, что InternTokenType импортирован или доступен
// import org.catrobat.catroid.formulaeditor.InternTokenType

@LunoClass
data class CustomFormula(
    val uniqueName: String,
    val displayName: String,
    val paramCount: Int,
    val defaultParamValues: List<String>,
    val defaultParamTypes: List<InternTokenType>,

    // --- ЗАМЕНЯЕМ JS НА ЭТО ---
    val lunoFunctionName: String, // Имя функции в code.txt
    val ownerLibraryId: String    // ID библиотеки (имя файла .newlib)
) {
    init {
        require(defaultParamValues.size == paramCount) {
            "Размер defaultParamValues (${defaultParamValues.size}) должен соответствовать paramCount ($paramCount) для '$uniqueName'"
        }
        require(defaultParamTypes.size == paramCount) {
            "Размер defaultParamTypes (${defaultParamTypes.size}) должен соответствовать paramCount ($paramCount) для '$uniqueName'"
        }
    }
}

@LunoClass
object CustomFormulaManager {
    val formulas: MutableList<CustomFormula> = mutableListOf()

    fun initialize() {
        /*addFormula(
            CustomFormula(
                uniqueName = "JS_ADD",
                displayName = "JS Сложение",
                paramCount = 2,
                defaultParamValues = listOf("1", "2"),
                defaultParamTypes = listOf(InternTokenType.NUMBER, InternTokenType.NUMBER),
                jsCode = "p[0] + p[1]" // p - массив параметров
            )
        )
        addFormula(
            CustomFormula(
                uniqueName = "JS_CONCAT",
                displayName = "JS Конкатенация",
                paramCount = 2,
                defaultParamValues = listOf("Привет", "Мир"),
                defaultParamTypes = listOf(InternTokenType.STRING, InternTokenType.STRING),
                jsCode = "String(p[0]) + String(p[1]);"
            )
        )*/
    }

    fun addFormula(formula: CustomFormula) {
        if (formulas.any { it.uniqueName == formula.uniqueName }) {
            Log.w("CustomFormulaManager", "Попытка добавить дублирующуюся формулу: ${formula.uniqueName}")
            return
        }
        // Проверка, что имя не конфликтует с существующими функциями из Functions.java
        if (Functions.isFunction(formula.uniqueName)) {
            Log.e("CustomFormulaManager", "Имя кастомной формулы '${formula.uniqueName}' конфликтует с существующей стандартной функцией!")
            // Можно выбросить исключение или просто не добавлять
            return
        }
        formulas.add(formula)
        Log.i("CustomFormulaManager", "Добавлена кастомная формула: ${formula.uniqueName}")
    }

    fun getFormulaByUniqueName(name: String): CustomFormula? {
        return formulas.find { it.uniqueName == name }
    }

    fun removeFormulasByOwner(libraryId: String) {
        formulas.removeAll { it.ownerLibraryId == libraryId }
        Log.i("CustomFormulaManager", "Удалены формулы, принадлежащие $libraryId")
    }
}