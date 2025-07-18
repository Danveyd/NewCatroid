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
import org.catrobat.catroid.formulaeditor.UserVariable
import org.catrobat.catroid.utils.ErrorLog
import java.io.File
import java.io.IOException
import java.util.ArrayList

class WriteToFilesAction() : TemporalAction() {
    private var contextt: Context? = null
    var scope: Scope? = null
    var fileName: Formula? = null
    var variable: UserVariable? = null

    override fun update(percent: Float) {
        scope?.let {
            val fileName_str = fileName?.interpretString(it) ?: "variable.txt"
            val project = it.project
            val name = project?.checkExtension(fileName_str, "txt") ?: "variable.txt"
            val file = File(project?.filesDir, name)
            writeToFile(file, variable?.value.toString())
        }
    }

    fun writeToFile(file: File, content: String) {
        try {
            // Шаг 1: Убедимся, что родительские директории существуют.
            // Это важный шаг, так как writeText() выдаст ошибку, если папки для файла нет.
            // `?.` - безопасный вызов, сработает только если родитель не null (т.е. это не корневой файл)
            // `mkdirs()` - создает все недостающие папки в пути.
            file.parentFile?.mkdirs()

            // Шаг 2: Записываем текст в файл, полностью перезаписывая его.
            // Этот метод автоматически создает файл, если он не существует,
            // и полностью перезаписывает его, если он уже есть.
            // Явно указываем кодировку UTF-8 для надежности.
            file.writeText(content, Charsets.UTF_8)

        } catch (e: IOException) {
            // Хорошей практикой будет обработать возможные ошибки ввода-вывода
            println("Произошла ошибка при записи в файл ${file.path}: ${e.message}")
            ErrorLog.log("Произошла ошибка при записи в файл ${file.path}: ${e.message}")
            // В реальном приложении здесь может быть логирование или другая обработка.
        }
    }
}
