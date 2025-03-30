/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
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
/*package org.catrobat.catroid.content.actions

import android.content.pm.PackageManager
import android.os.Environment
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.common.SoundInfo
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.io.SoundManager
import org.catrobat.catroid.io.StorageOperations
import org.catrobat.catroid.pocketmusic.mididriver.MidiSoundManager
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.paintroid.common.PERMISSION_EXTERNAL_STORAGE_SAVE_COPY
import java.io.File
import java.io.InputStream
import kotlin.random.Random

class SoundFilesWaitAction : TemporalAction() {
    var scope: Scope? = null
    var name: Formula? = null
    private var soundFilePath: String? = null

    override fun update(percent: Float) {
        val activity = StageActivity.activeStageActivity.get()
        activity?.runOnUiThread {
            // Проверяем разрешения для чтения файлов
            if (ContextCompat.checkSelfPermission(
                    CatroidApplication.getAppContext(),
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                    PERMISSION_EXTERNAL_STORAGE_SAVE_COPY
                )
            }
        }

        scope?.let { scop ->
            // Получаем имя файла
            val fileName = getName(name) ?: "my_sound.mp3"
            soundFilePath = File(scop.project?.filesDir, fileName).absolutePath

            // Проверяем, что звук всё еще воспроизводится
            if (isSoundPlaying(soundFilePath, scop.sprite)) {
                // Если звук воспроизводится, продолжаем ожидание
                return
            } else {
                // Если звук остановился, завершаем действие
                finish()
            }
        }
    }

    private fun isSoundPlaying(soundFilePath: String?, sprite: Sprite): Boolean {
        val soundManager = SoundManager.getInstance()
        val midiSoundManager = MidiSoundManager.getInstance()

        // Проверяем, воспроизводится ли звук с этим путём
        return soundManager.playingSoundBackups(sprite, soundFilePath) ||
                midiSoundManager.isSoundInSpritePlaying(sprite, soundFilePath)
    }

    fun generateRandomString(length: Int): String {
        val letters = ('a'..'z') + ('A'..'Z') // Учитываем строчные и заглавные буквы
        return (1..length)
            .map { letters.random() } // Генерируем случайные буквы из списка
            .joinToString("") // Собираем их в строку
    }

    fun getInputStreamFromFile(file: File): InputStream? {
        return try {
            file.inputStream()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun createTemporaryFile(fileName: String): File {
        // Получаем расширение файла, если оно есть
        val extension = fileName.substringAfterLast('.', "")
        // Создаем временный файл с уникальным именем
        val tempFile = File.createTempFile("temp_", ".$extension")

        // Возвращаем временный файл
        return tempFile
    }

    fun getName(inputName: Formula?): String? {
        inputName?.let { inname ->
            var name = inname.interpretString(scope)
            val lastDotIndex = name.lastIndexOf('.')
            if(lastDotIndex <= 0 && lastDotIndex >= name.length - 1) {
                name += ".png"
            }
            return name
        }
        return null
    }
}
*/