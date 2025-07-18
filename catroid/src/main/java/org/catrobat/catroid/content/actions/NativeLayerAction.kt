package org.catrobat.catroid.content.actions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.UserVariable
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.utils.ErrorLog
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class NativeLayerAction : TemporalAction() {
    var scope: Scope? = null
    var layer: Int = 1 // По умолчанию - все файлы

    override fun update(percent: Float) {
        val activity: StageActivity = StageActivity.activeStageActivity.get() ?: return

        when (layer) {
            0 -> activity.setNativesForeground()
            1 -> {
                activity.setNativesBackground()
                activity.debugLayoutHierarchy()
                //activity.createDebugView("test", Color.MAGENTA, 50, 50, 200, 300)
            }
        }
    }
}
