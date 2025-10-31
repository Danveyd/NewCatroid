package org.catrobat.catroid.codeanalysis

import android.content.Context
import org.catrobat.catroid.content.bricks.*
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.UserData

class RedundantBlockRule(private val context: Context) : AnalysisRule {
    override fun analyze(brick: Brick): AnalysisResult? {
        if (brick is CompositeBrick) {
            return null
        }

        val parent = brick.parent ?: return null
        val siblingBricks = parent.dragAndDropTargetList ?: return null
        val brickIndex = siblingBricks.indexOf(brick)

        if (brickIndex > 0) {
            val previousBrick = siblingBricks[brickIndex - 1]

            if (brick.javaClass != previousBrick.javaClass) {
                return null
            }

            if (areBricksEffectivelyEqual(brick, previousBrick)) {
                return AnalysisResult(
                    Severity.WARNING,
                    "Этот блок выполняет то же самое действие с теми же параметрами, что и предыдущий. Возможно, он лишний."
                )
            }
        }
        return null
    }

    private fun areBricksEffectivelyEqual(brick1: Brick, brick2: Brick): Boolean {
        if (brick1 is FormulaBrick && brick2 is FormulaBrick) {
            val formulas1 = brick1.allFormulaFieldsWithFormulas
            val formulas2 = brick2.allFormulaFieldsWithFormulas

            if (formulas1.size != formulas2.size) return false
            if (formulas1.isEmpty()) return true

            return formulas1.all { (field, formula1) ->
                val formula2 = formulas2[field]
                formula2 != null && formula1.getTrimmedFormulaString(context) == formula2.getTrimmedFormulaString(context)
            }
        }

        if (brick1 is UserDataBrick && brick2 is UserDataBrick) {
            val data1 = brick1.allBrickDataWithValues
            val data2 = brick2.allBrickDataWithValues

            if (data1.size != data2.size) return false
            if (data1.isEmpty()) return true

            return data1.all { (field, value1) ->
                val value2 = data2[field]
                val name1 = (value1 as? UserData)?.name
                val name2 = (value2 as? UserData)?.name
                (name1 == null && name2 == null) || (name1 != null && name1 == name2)
            }
        }

        if (brick1 is SetLookBrick && brick2 is SetLookBrick) {
            return brick1.look?.name == brick2.look?.name
        }

        return false
    }
}