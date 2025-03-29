package org.catrobat.catroid.content.actions

import android.webkit.WebView
import android.webkit.WebViewClient
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.UserVariable
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.stage.StageActivity.IntentListener
import android.util.Log
import androidx.core.app.ActivityCompat
import org.catrobat.catroid.R
import android.widget.FrameLayout
import org.catrobat.catroid.content.MyActivityManager
import org.catrobat.catroid.content.WebViewController

class CreateWebFileAction : TemporalAction() {
    //private lateinit var rootLayout: FrameLayout // Контейнер для WebView
    var scope: Scope? = null
    var file: Formula? = null
    var name: Formula? = null
    var posX: Formula? = null
    var posY: Formula? = null
    var width: Formula? = null
    var height: Formula? = null

    override fun update(percent: Float) {
        //rootLayout = FrameLayout(CatroidApplication.getAppContext())
        var filev = file?.interpretObject(scope)?.toString() ?: ""
        var namev = name?.interpretObject(scope)?.toString() ?: ""
        var posXv = posX?.interpretObject(scope)?.toString()?.toDoubleOrNull()?.toInt() ?: 0
        var posYv = posY?.interpretObject(scope)?.toString()?.toDoubleOrNull()?.toInt() ?: 0

        // Преобразование для width и height
        var widthv = width?.interpretObject(scope)?.toString()?.toDoubleOrNull()?.toInt() ?: 0
        var heightv = height?.interpretObject(scope)?.toString()?.toDoubleOrNull()?.toInt() ?: 0

        //val activity = StageActivity.activeStageActivity.get()
        //activity?.runOnUiThread {
        //MyActivityManager.stage_activity?.let { activity ->
        //    WebViewController.loadHtmlIntoWebView(activity, namev, filev, posXv, posYv, widthv, heightv)
        //}
    }
}