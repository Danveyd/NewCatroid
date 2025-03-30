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
import org.catrobat.catroid.content.SquareActor
import org.catrobat.catroid.content.SquareController
import org.catrobat.catroid.formulaeditor.Formula
import java.util.ArrayList

class SquareAction() : TemporalAction() {
    var scope: Scope? = null
    var x: Formula? = null
    var y: Formula? = null
    var w: Formula? = null
    var h: Formula? = null
    var color: Formula? = null
    var nam: Formula? = null
    var rot: Formula? = null
    var trans: Formula? = null
    var bord: Formula? = null

    override fun update(percent: Float) {
        val pos_x = x?.interpretFloat(scope) ?: 0f
        val pos_y = y?.interpretFloat(scope) ?: 0f
        val width = w?.interpretFloat(scope) ?: 100f
        val height = h?.interpretFloat(scope) ?: 100f
        val color_str = color?.interpretString(scope) ?: "#ff0000"
        val name = nam?.interpretString(scope) ?: "square"
        val rotation = rot?.interpretFloat(scope) ?: 0f
        val transparency = trans?.interpretFloat(scope) ?: 0f
        val borders = bord?.interpretFloat(scope) ?: 0f

        val stage = StageActivity.stageListener.stage

        scope?.project?.xmlHeader?.virtualScreenWidth?.div(2)?.let { scrX ->
            scope?.project?.xmlHeader?.getVirtualScreenHeight()?.div(2)?.let { scrY ->
                SquareController.instance.createOrUpdateSquare(
                    name,
                    pos_x + scrX,
                    pos_y + scrY,
                    width,
                    height,
                    color_str,
                    rotation,
                    transparency,
                    borders,
                    stage
                )
            }
        }
        //val squareActor = SquareActor(pos_x, pos_y, width, height, color_str)
    }
}
