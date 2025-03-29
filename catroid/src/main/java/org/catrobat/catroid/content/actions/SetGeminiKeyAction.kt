package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.GeminiManager
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import android.widget.Toast
import android.content.Context
import android.app.Activity
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.stage.StageActivity.IntentListener
import android.util.Log
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.R
import java.util.ArrayList

class SetGeminiKeyAction(): TemporalAction() {
    var scope: Scope? = null
    var key: Formula? = null

    override fun update(percent: Float) {
        var keyVal = key?.interpretObject(scope) ?: ""
        var keyStr = keyVal.toString()

        GeminiManager.api_key = keyStr
    }
}