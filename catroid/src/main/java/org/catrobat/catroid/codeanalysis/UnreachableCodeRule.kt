package org.catrobat.catroid.codeanalysis

import org.catrobat.catroid.content.bricks.Brick
import org.catrobat.catroid.content.bricks.StopScriptBrick
import org.catrobat.catroid.content.bricks.ForeverBrick
import org.catrobat.catroid.content.bricks.CompositeBrick

class UnreachableCodeRule : AnalysisRule {
    override fun analyze(brick: Brick): AnalysisResult? {
        val parent = brick.parent ?: return null
        val siblingBricks = parent.dragAndDropTargetList ?: return null
        val brickIndex = siblingBricks.indexOf(brick)

        if (brickIndex > 0) {
            val previousBrick = siblingBricks[brickIndex - 1]

            if (previousBrick is StopScriptBrick) {
                return AnalysisResult(
                    Severity.ERROR,
                    "Этот блок никогда не будет выполнен, так как перед ним стоит блок, останавливающий скрипт."
                )
            }

            if (previousBrick is ForeverBrick) {
                if (!containsBreak(previousBrick)) {
                    return AnalysisResult(
                        Severity.WARNING,
                        "Этот блок никогда не будет выполнен, так как он находится после бесконечного цикла без блока 'остановить'."
                    )
                }
            }
        }
        return null
    }


    private fun containsBreak(compositeBrick: CompositeBrick): Boolean {
        for (nested in compositeBrick.nestedBricks) {
            if (nested is StopScriptBrick) {
                return true
            }
            if (nested is CompositeBrick) {
                if (containsBreak(nested)) {
                    return true
                }
            }
        }

        if (compositeBrick.hasSecondaryList()) {
            for (nested in compositeBrick.secondaryNestedBricks) {
                if (nested is StopScriptBrick) return true
                if (nested is CompositeBrick && containsBreak(nested)) return true
            }
        }

        return false
    }
}