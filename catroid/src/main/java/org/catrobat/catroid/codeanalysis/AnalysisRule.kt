package org.catrobat.catroid.codeanalysis

import org.catrobat.catroid.content.bricks.Brick

interface AnalysisRule {
    /**
     * @return AnalysisResult, если проблема найдена, иначе null.
     */
    fun analyze(brick: Brick): AnalysisResult?
}