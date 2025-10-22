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

import WebViewManager
import android.content.Context
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction

import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import android.widget.LinearLayout

class CreateWebAction() : TemporalAction() {
    private var contextt: Context? = null
    var scope: Scope? = null
    var name: Formula? = null
    var url: Formula? = null
    var posX: Formula? = null
    var posY: Formula? = null
    var width: Formula? = null
    var height: Formula? = null

    private lateinit var webViewManager: WebViewManager
    private lateinit var viewContainer: LinearLayout

    override fun update(percent: Float) {
        val nameT = name?.interpretObject(scope).toString() ?: ""
        val urlT = url?.interpretObject(scope).toString() ?: ""
        val posXT = posX?.interpretObject(scope) ?: ""
        val posYT = posY?.interpretObject(scope) ?: ""
        val widthT = width?.interpretObject(scope) ?: ""
        val heightT = height?.interpretObject(scope) ?: ""
    }
}
