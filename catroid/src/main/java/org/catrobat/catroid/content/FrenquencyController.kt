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

package org.catrobat.catroid.content

import android.media.AudioRecord
import android.media.AudioFormat
import android.media.MediaRecorder
import kotlin.math.abs
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

import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import java.util.ArrayList
import java.lang.Exception
import java.io.File

import android.Manifest
import kotlin.math.abs
import android.content.pm.PackageManager

class FrequencyController(private val context: Context) {

    private var audioRecord: AudioRecord? = null
    private val sampleRate = 44100
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate,
                                                          AudioFormat.CHANNEL_IN_MONO,
                                                          AudioFormat.ENCODING_PCM_16BIT)

    fun start() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Permission not granted for recording audio")
            return
        }

        audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC,
                                  sampleRate,
                                  AudioFormat.CHANNEL_IN_MONO,
                                  AudioFormat.ENCODING_PCM_16BIT,
                                  bufferSize).apply {
            startRecording()
        }
    }

    fun stop() {
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    fun getFrequency(): Float {
        val buffer = ShortArray(bufferSize)
        audioRecord?.read(buffer, 0, bufferSize)

        // Преобразовать buffer в массив Double
        val normalized = DoubleArray(bufferSize)
        for (i in buffer.indices) {
            normalized[i] = buffer[i].toDouble() / Short.MAX_VALUE // Нормализация
        }

        return calculateFrequency(normalized).toFloat()
    }

    private fun calculateFrequency(signal: DoubleArray): Double {
        var maxLag = 0
        var maxCorrelation = 0.0

        val length = signal.size

        for (lag in 1 until length / 2) {
            var correlation = 0.0
            for (i in 0 until length - lag) {
                correlation += signal[i] * signal[i + lag]
            }

            correlation = abs(correlation)

            if (correlation > maxCorrelation) {
                maxCorrelation = correlation
                maxLag = lag
            }
        }

        // Расчет частоты
        return if (maxLag > 0) {
            sampleRate.toDouble() / maxLag // Возвращаем частоту
        } else {
            0.0  // Возврат 0.0 если колебаний не обнаружено
        }
    }

    private companion object {
        private const val TAG = "FrequencyController"
    }
}


