package org.catrobat.catroid.codeanalysis

import android.content.Context
import org.catrobat.catroid.content.Script
import org.catrobat.catroid.content.bricks.Brick
import org.catrobat.catroid.content.bricks.CompositeBrick
import org.catrobat.catroid.content.bricks.TryCatchFinallyBrick

class CodeAnalyzer(context: Context) {
    private val rules = listOf<AnalysisRule>(
        ErrorRule(context), EmptyLoopRule(), ConstantConditionRule(context), UnreachableCodeRule(), RedundantBlockRule(context),
    )

    fun analyzeScript(script: Script): Map<Brick, AnalysisResult> {
        val results = mutableMapOf<Brick, AnalysisResult>()
        analyzeBrickList(script.brickList, results)
        return results
    }

    private fun analyzeBrickList(brickList: List<Brick>, results: MutableMap<Brick, AnalysisResult>) {
        for (brick in brickList) {
            for (rule in rules) {
                val result = rule.analyze(brick)
                if (result != null) {
                    results[brick] = result
                    break
                }
            }

            if (brick is CompositeBrick) {
                brick.nestedBricks?.let { analyzeBrickList(it, results) }

                if (brick.hasSecondaryList()) {
                    brick.secondaryNestedBricks?.let { analyzeBrickList(it, results) }
                }

                if (brick is TryCatchFinallyBrick) {
                    brick.thirdNestedBricks?.let { analyzeBrickList(it, results) }
                }
            }
        }
    }
}