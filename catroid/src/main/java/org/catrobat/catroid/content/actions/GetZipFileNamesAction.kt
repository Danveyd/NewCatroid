package org.catrobat.catroid.content.actions

import android.widget.Toast
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.R
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.UserVariable
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import java.util.zip.ZipFile
import java.io.IOException

class GetZipFileNamesAction : TemporalAction() {
    var scope: Scope? = null
    var zipFileName: Formula? = null
    var userVariable: UserVariable? = null

    override fun update(percent: Float) {
        val project = scope?.project
        val variable = userVariable
        if (project == null || variable == null) {
            return
        }

        val context = CatroidApplication.getAppContext()
        val fileName = zipFileName?.interpretString(scope)

        if (fileName.isNullOrEmpty()) {
            variable.value = "Error: ZIP file name is empty"
            return
        }

        try {
            val zipFile = project.getFile(fileName)
            if (!zipFile.exists()) {
                variable.value = "Error: File not found: $fileName"
                return
            }

            val zf = ZipFile(zipFile)
            val fileNames = zf.entries().asSequence()
                .map { it.name }
                .joinToString(",")

            zf.close()

            variable.value = fileNames

        } catch (e: IOException) {
            variable.value = "Error: Failed to read ZIP file: ${e.message}"
        }
    }
}