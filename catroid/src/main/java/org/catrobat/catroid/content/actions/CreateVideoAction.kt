/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2024 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.catrobat.catroid.content.actions

import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.stage.StageActivity
import java.io.File

class CreateVideoAction() : TemporalAction() {
    var scope: Scope? = null
    var name: Formula? = null
    var file: Formula? = null
    var posX: Formula? = null
    var posY: Formula? = null
    var width: Formula? = null
    var height: Formula? = null
    var loop: Formula? = null
    var controls: Formula? = null

    override fun update(percent: Float) {
        if (scope == null) {
            Log.d("VideoPlayerAction", "scope is null")
            return
        }
        val activity: StageActivity? = StageActivity.activeStageActivity.get();
        if (activity == null) {
            Log.d("VideoPlayerAction", "activity is null")
            return
        }

        val nameT = name?.interpretString(scope) ?: ""
        val fileT = file?.interpretString(scope) ?: ""
        val loopT = loop?.interpretBoolean(scope) ?: false
        val controlsT = controls?.interpretBoolean(scope) ?: false
        val posXT = posX?.interpretInteger(scope) ?: 0
        val posYT = posY?.interpretInteger(scope) ?: 0
        val widthT = width?.interpretInteger(scope) ?: 0
        val heightT = height?.interpretInteger(scope) ?: 0

        val projFile: File? = scope!!.project?.getFile(fileT)
        if (projFile == null) {
            Log.d("VideoPlayerAction", "project file is null")
            return
        }

        activity.runOnUiThread {
            activity.createVideoPlayer(
                nameT,
                projFile.absolutePath,  // <--- Передаем абсолютный путь
                posXT, posYT, widthT, heightT,
                controlsT, loopT
            )
        }
        Log.d("VideoPlayerAction", "showed")
    }
}
