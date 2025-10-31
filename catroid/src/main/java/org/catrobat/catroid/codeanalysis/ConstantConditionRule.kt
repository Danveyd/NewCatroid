package org.catrobat.catroid.codeanalysis

import android.content.Context
import org.catrobat.catroid.content.bricks.Brick
import org.catrobat.catroid.content.bricks.IfLogicBeginBrick
import org.catrobat.catroid.content.bricks.IfThenLogicBeginBrick
import org.catrobat.catroid.content.bricks.RepeatUntilBrick
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.FormulaElement
import org.catrobat.catroid.formulaeditor.InterpretationException

class ConstantConditionRule(private val context: Context) : AnalysisRule {
    override fun analyze(brick: Brick): AnalysisResult? {
        val formula = when (brick) {
            is IfThenLogicBeginBrick -> brick.getFormulaWithBrickField(Brick.BrickField.IF_CONDITION)
            is IfLogicBeginBrick -> brick.getFormulaWithBrickField(Brick.BrickField.IF_CONDITION)
            is RepeatUntilBrick -> brick.getFormulaWithBrickField(Brick.BrickField.REPEAT_UNTIL_CONDITION)
            else -> return null
        }

        if (!isFormulaConstant(formula)) {
            return null
        }

        val evaluationResult: Boolean = try {
            formula.interpretBoolean(null)
        } catch (e: InterpretationException) {
            return null
        }

        val conditionText = formula.getTrimmedFormulaString(context)
        if (evaluationResult) {
            val message = when (brick) {
                is RepeatUntilBrick -> "Условие '$conditionText' всегда истинно. Этот цикл завершится сразу, не выполнив ни одного действия."
                else -> "Условие '$conditionText' всегда истинно. Код в ветке 'иначе' (если она есть) никогда не будет выполнен."
            }
            return AnalysisResult(Severity.WARNING, message)
        } else {
            val message = when (brick) {
                is RepeatUntilBrick -> "Условие '$conditionText' всегда ложно. Этот цикл будет бесконечным."
                else -> "Условие '$conditionText' всегда ложно. Код внутри этого блока никогда не будет выполнен."
            }
            return AnalysisResult(Severity.WARNING, message)
        }
    }

    private fun isFormulaConstant(formula: Formula?): Boolean {
        val root = formula?.root ?: return false
        return checkElementIsConstant(root)
    }

    private fun checkElementIsConstant(element: FormulaElement?): Boolean {
        if (element == null) {
            return true
        }

        when (element.type) {
            FormulaElement.ElementType.SENSOR,
            FormulaElement.ElementType.USER_VARIABLE,
            FormulaElement.ElementType.USER_LIST,
            FormulaElement.ElementType.COLLISION_FORMULA -> return false

            FormulaElement.ElementType.FUNCTION -> {
                if (element.value == "RAND") {
                    return false
                }
            }
            else -> {}
        }

        return checkElementIsConstant(element.leftChild) &&
                checkElementIsConstant(element.rightChild) &&
                element.additionalChildren.all { checkElementIsConstant(it) }
    }
}