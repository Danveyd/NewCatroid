package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.NN.OnnxSessionManager

import org.catrobat.catroid.R
import org.catrobat.catroid.content.FloatArrayManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.UserVariable
import org.catrobat.catroid.utils.ToastUtil
import java.util.concurrent.Future

class PredictNNAction() : TemporalAction() {
    var scope: Scope? = null
    var input: Formula? = null
    var variable: UserVariable? = null

    private var hasStarted = false

    override fun act(delta: Float): Boolean {
        if (hasStarted) return true
        hasStarted = true

        val arrayName = input?.interpretString(scope)
        val safeVariable = variable

        if (arrayName.isNullOrBlank() || safeVariable == null) {
            return true
        }

        val inputArray = FloatArrayManager.getArray(arrayName)

        if (inputArray != null && inputArray.isNotEmpty()) {
            OnnxSessionManager.predict(inputArray) { rawResult ->
                if (rawResult != null) {
                    val bestIndex = FloatArrayManager.findMaxIndex(rawResult)
                    val resultString = rawResult.joinToString(",")
                    safeVariable.value = "$bestIndex\n$resultString"

                } else {
                    safeVariable.value = "ERROR"
                }
            }
        } else {
            safeVariable.value = "ARRAY_ERROR"
        }

        return true
    }

    override fun update(percent: Float) {

    }

    override fun restart() {
        super.restart()
        hasStarted = false
    }
}