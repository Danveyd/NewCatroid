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
import org.catrobat.catroid.formulaeditor.UserVariable
import org.catrobat.catroid.stage.StageActivity


class CreateTextFieldAction() : TemporalAction() {
    var scope: Scope? = null
    var name: Formula? = null
    var variable: UserVariable? = null
    var posX: Formula? = null
    var posY: Formula? = null
    var width: Formula? = null
    var height: Formula? = null
    var initialText: Formula? = null

    var text_size: Formula? = null
    var text_color: Formula? = null
    var bg_color: Formula? = null
    var hint_text: Formula? = null
    var hint_color: Formula? = null
    var alignment_f: Formula? = null
    var font_path: Formula? = null
    var input_type: Formula? = null
    var is_password: Formula? = null
    var max_length: Formula? = null
    var corner_radius: Formula? = null

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
        val posXT = posX?.interpretInteger(scope) ?: 0
        val posYT = posY?.interpretInteger(scope) ?: 0
        val widthT = width?.interpretInteger(scope) ?: 0
        val heightT = height?.interpretInteger(scope) ?: 0

        val customStyles = HashMap<String, String>()
        customStyles[StageActivity.STYLE_TEXT_SIZE] = text_size?.interpretString(scope) ?: "22"
        customStyles[StageActivity.STYLE_TEXT_COLOR] = text_color?.interpretString(scope) ?: "#FFFFFF"
        customStyles[StageActivity.STYLE_BACKGROUND_COLOR] =
            bg_color?.interpretString(scope) ?: "#88000000"
        customStyles[StageActivity.STYLE_HINT_TEXT] = hint_text?.interpretString(scope) ?: "Enter value..."
        customStyles[StageActivity.STYLE_HINT_TEXT_COLOR] =
            hint_color?.interpretString(scope) ?: "#CCCCCC"
        customStyles[StageActivity.STYLE_TEXT_ALIGNMENT] =
            alignment_f?.interpretString(scope) ?: "left"
        font_path?.interpretString(scope)?.let {
            customStyles[StageActivity.STYLE_FONT_PATH] = scope!!.project?.getFile(it)?.absolutePath ?: it
        }
        input_type?.interpretString(scope)?.let { customStyles[StageActivity.STYLE_INPUT_TYPE] = it }
        max_length?.interpretString(scope)?.let { customStyles[StageActivity.STYLE_MAX_LENGTH] = it }
        corner_radius?.interpretString(scope)?.let { customStyles[StageActivity.STYLE_CORNER_RADIUS] = it }
        val isPasswordValue = is_password?.interpretBoolean(scope) ?: false
        customStyles[StageActivity.STYLE_IS_PASSWORD] = isPasswordValue.toString()

        activity.runOnUiThread {
            activity.createInputField(
                nameT,
                variable,
                initialText?.interpretString(scope) ?: "",
                posXT, posYT, widthT, heightT,
                customStyles
            );
        }
    }
}
