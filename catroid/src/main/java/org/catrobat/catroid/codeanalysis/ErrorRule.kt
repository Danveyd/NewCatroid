package org.catrobat.catroid.codeanalysis

import android.content.Context
import android.util.Log
import android.widget.Toast
import org.catrobat.catroid.content.bricks.Brick
import org.catrobat.catroid.content.bricks.IfLogicBeginBrick
import org.catrobat.catroid.content.bricks.IfThenLogicBeginBrick
import org.catrobat.catroid.content.bricks.PostWebRequestBrick
import org.catrobat.catroid.content.bricks.RepeatUntilBrick
import org.catrobat.catroid.content.bricks.RunVMBrick
import org.catrobat.catroid.content.bricks.WriteBaseBrick
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.FormulaElement
import org.catrobat.catroid.formulaeditor.InterpretationException

class ErrorRule(private val context: Context) : AnalysisRule {
    override fun analyze(brick: Brick): AnalysisResult? {
        if (brick is WriteBaseBrick) {
            val formula = brick.getFormulaWithBrickField(Brick.BrickField.FIREBASE_ID)
            val idString = formula.getTrimmedFormulaString(context)
            Log.d("ErrorRule", idString)
            Log.d("ErrorRule", idString.isBlank().toString())
            Log.d("ErrorRule", idString.startsWith("'https://").toString())
            Log.d("ErrorRule", idString.endsWith(".firebaseio.com' ").toString())


            if (idString.isBlank() || !idString.startsWith("'https://") || !idString.endsWith(".firebaseio.com' ")) {
                return AnalysisResult(
                    Severity.ERROR,
                    "Некорректный ID базы данных Firebase. Он не может быть пустым и должен начинаться с 'https://' и заканчиваться 'firebaseio.com'. Это приведет к ошибке во время выполнения."
                )
            }
        }

        return null
    }
}