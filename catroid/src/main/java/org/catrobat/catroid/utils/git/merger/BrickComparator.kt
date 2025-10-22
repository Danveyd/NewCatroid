package org.catrobat.catroid.utils.git.merger

import org.catrobat.catroid.content.bricks.*
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.FormulaElement.ElementType
import org.catrobat.catroid.utils.git.Conflict

/**
 * Интерфейс для классов, которые умеют сравнивать и сливать
 * один конкретный тип кирпичиков.
 */
interface BrickComparator {
    fun compare(mergedBrick: Brick, baseBrick: Brick, remoteBrick: Brick, conflicts: MutableList<Conflict>)
}

/**
 * Вспомогательная функция для безопасного получения простого значения из формулы.
 * Возвращает значение, только если формула - это простое число или строка.
 * Если это сложная формула (например, "x_pos + 10"), возвращает null.
 */
fun getLiteralValue(formula: Formula?): String? {
    if (formula == null) return null
    val root = formula.formulaTree
    return if (root.elementType == ElementType.NUMBER || root.elementType == ElementType.STRING) {
        root.value
    } else {
        null
    }
}

class SpeakBrickComparator : BrickComparator {
    override fun compare(mergedBrick: Brick, baseBrick: Brick, remoteBrick: Brick, conflicts: MutableList<Conflict>) {
        if (mergedBrick !is SpeakBrick || baseBrick !is SpeakBrick || remoteBrick !is SpeakBrick) return

        val baseValue = getLiteralValue(baseBrick.formulas[0])
        val localValue = getLiteralValue(mergedBrick.formulas[0])
        val remoteValue = getLiteralValue(remoteBrick.formulas[0])

        if (baseValue != null && localValue != null && remoteValue != null) {
            if (remoteValue != baseValue && localValue == baseValue) {
                mergedBrick.formulas[0]?.root?.value = remoteValue
            } else if (remoteValue != baseValue && localValue != baseValue && remoteValue != localValue) {
                conflicts.add(Conflict(mergedBrick.brickID.toString(), "SpeakBrick Text", baseValue, localValue, remoteValue))
            }
        }
    }
}

class MoveNStepsBrickComparator : BrickComparator {
    override fun compare(mergedBrick: Brick, baseBrick: Brick, remoteBrick: Brick, conflicts: MutableList<Conflict>) {
        if (mergedBrick !is MoveNStepsBrick || baseBrick !is MoveNStepsBrick || remoteBrick !is MoveNStepsBrick) return

        val baseValue = getLiteralValue(baseBrick.formulas[0])
        val localValue = getLiteralValue(mergedBrick.formulas[0])
        val remoteValue = getLiteralValue(remoteBrick.formulas[0])

        if (baseValue != null && localValue != null && remoteValue != null) {
            if (remoteValue != baseValue && localValue == baseValue) {
                mergedBrick.formulas[0]?.root?.value = remoteValue
            } else if (remoteValue != baseValue && localValue != baseValue && remoteValue != localValue) {
                conflicts.add(Conflict(mergedBrick.brickID.toString(), "MoveNSteps Value", baseValue, localValue, remoteValue))
            }
        }
    }
}


//Ov23liKoq3h0cTgAbVYA
//936da4332f8a31ebed1bc97aa5d2f89a989a56d2