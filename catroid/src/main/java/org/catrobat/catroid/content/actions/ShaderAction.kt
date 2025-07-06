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

import android.widget.Toast
import android.content.Context
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import android.app.Activity
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.stage.StageActivity.IntentListener
import android.util.Log
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.R

import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.utils.ErrorLog
import org.catrobat.catroid.utils.GlobalShaderManager
import java.util.ArrayList

class ShaderAction() : TemporalAction() {
    var scope: Scope? = null
    var formula: Formula? = null
    var vertex: Formula? = null


    override fun update(percent: Float) {
       val code: String = formula?.interpretString(scope)?.trimIndent() ?: """
        // Код для вашего фрагментного шейдера
#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_texCoords;
uniform sampler2D u_texture;

void main() {
    // 1. Пытаемся получить цвет из текстуры FBO
    vec4 sceneColor = texture2D(u_texture, v_texCoords);

    // 2. Генерируем "контрольный" цвет-градиент, который не зависит от текстуры.
    // Он доказывает, что сам шейдер и его координаты работают.
    vec4 gradientColor = vec4(v_texCoords.x, v_texCoords.y, 0.0, 1.0);

    // 3. Смешиваем их. 50% от сцены, 50% от градиента.
    gl_FragColor = mix(sceneColor, gradientColor, 0.5);
}
    """.trimIndent()
        val vert: String = vertex?.interpretString(scope) ?: """
    attribute vec4 a_position;
    attribute vec2 a_texCoord0;
    uniform mat4 u_projTrans; // <--- ЭТО ВАЖНО
    varying vec2 v_texCoords;

    void main() {
        v_texCoords = a_texCoord0;
        gl_Position = u_projTrans * a_position;
    }
""".trimIndent()

        val errorMessage = GlobalShaderManager.setCustomShader(code, vert)

        if (errorMessage != null) {
            ErrorLog.log(errorMessage)
        }
    }
}
