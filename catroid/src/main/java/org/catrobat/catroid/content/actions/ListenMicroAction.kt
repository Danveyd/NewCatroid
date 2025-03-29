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
import androidx.core.app.ActivityCompat
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.R
import org.catrobat.catroid.content.RecogController
import org.catrobat.catroid.content.FrequencyController

import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.content.VolumeManager
import org.catrobat.catroid.formulaeditor.Formula
import java.util.ArrayList
import android.os.CountDownTimer

class ListenMicroAction() : TemporalAction() {
    private var recogController: RecogController = RecogController(CatroidApplication.getAppContext())
    private var freqController: FrequencyController = FrequencyController(CatroidApplication.getAppContext())
    private var countDownTimer: CountDownTimer? = null
    var scope: Scope? = null
    var time: Formula? = null

    override fun update(percent: Float) {
        Log.d("ListemMicro", "Started")
        var value = time?.interpretObject(scope) ?: ""
        var value2 = value.toString().toLong()

        //val activity = CatroidApplication.getAppContext() as? Activity
        val activity = StageActivity.activeStageActivity.get()
        activity?.runOnUiThread {
            recogController.start(activity) // Передаем Activity здесь
            freqController.start()
            countDownTimer = object : CountDownTimer(60_000, value2) {
                override fun onTick(p0: Long) {
                    val volume = recogController.getVolume()
                    val frequency = freqController.getFrequency()
                    VolumeManager.volume = volume
                    VolumeManager.frequency = frequency
                }

                override fun onFinish() {
                }
            }.apply {
                start()
            }
            Log.d("ListemMicro", "Ended")
        }
    }

    fun stop() {
        recogController.stop()
    }
}
