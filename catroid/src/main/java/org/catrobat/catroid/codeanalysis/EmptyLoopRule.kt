package org.catrobat.catroid.codeanalysis

import org.catrobat.catroid.content.bricks.Brick
import org.catrobat.catroid.content.bricks.CompositeBrick
import org.catrobat.catroid.content.bricks.LoopEndBrick
import org.catrobat.catroid.content.bricks.ForeverBrick
import org.catrobat.catroid.content.bricks.IfLogicBeginBrick
import org.catrobat.catroid.content.bricks.IfThenLogicBeginBrick
import org.catrobat.catroid.content.bricks.RepeatBrick
import org.catrobat.catroid.content.bricks.TryCatchFinallyBrick

class EmptyLoopRule : AnalysisRule {
    override fun analyze(brick: Brick): AnalysisResult? {
        if (brick !is CompositeBrick) {
            return null
        }

        if (brick is IfLogicBeginBrick) {
            val ifBranchEmpty = brick.nestedBricks.isEmpty()
            val elseBranchEmpty = brick.secondaryNestedBricks.isEmpty()

            if (ifBranchEmpty && elseBranchEmpty) {
                return AnalysisResult(Severity.WARNING, "Блок 'Если-Иначе' не содержит действий ни в одной из веток.")
            }

            if (ifBranchEmpty && !elseBranchEmpty) {
                return AnalysisResult(
                    Severity.WARNING,
                    "Оптимизация: Этот блок можно заменить на простой 'Если', инвертировав (поменяв на противоположное) условие."
                )
            }

            if (!ifBranchEmpty && elseBranchEmpty) {
                return AnalysisResult(
                    Severity.WARNING,
                    "Оптимизация: Ветка 'Иначе' пуста. Этот блок можно заменить на более простой 'Если'."
                )
            }

            return null
        }

        if (brick is TryCatchFinallyBrick) {
            val tryBranchEmpty = brick.nestedBricks.isEmpty()
            val catchBranchEmpty = brick.secondaryNestedBricks.isEmpty()
            val finallyBranchEmpty = brick.thirdNestedBricks.isEmpty()

            if (tryBranchEmpty && catchBranchEmpty && finallyBranchEmpty) {
                return AnalysisResult(Severity.WARNING, "Блок 'Try-Catch-Finally' полностью пуст.")
            }
            return null
        }

        if (brick.nestedBricks.isEmpty()) {
            val brickName = brick.javaClass.simpleName
                .replace("Brick", "")
                .replace("LogicBegin", "")
            return AnalysisResult(Severity.WARNING, "Блок '$brickName' пуст и не выполняет никаких действий.")
        }
        return null
    }
}