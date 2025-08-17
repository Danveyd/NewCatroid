package org.catrobat.catroid.libraryeditor.data

import android.content.Context
import android.net.Uri
import android.util.Log
import android.util.Xml
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.R
import org.catrobat.catroid.libraryeditor.data.EditableBrick
import org.catrobat.catroid.libraryeditor.data.EditableFormula
import org.catrobat.catroid.libraryeditor.data.EditableParam
import org.catrobat.catroid.libraryeditor.data.LibraryDraft
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.util.zip.ZipInputStream

object NewlibImporter {

    fun import(context: Context, uri: Uri): LibraryDraft? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val draft = LibraryDraft(id = "last_session", name = CatroidApplication.getAppContext().getString(R.string.libs_newlib))
                val zipInputStream = ZipInputStream(inputStream)
                var entry = zipInputStream.nextEntry

                while (entry != null) {
                    when (entry.name) {
                        "code.txt" -> {
                            draft.code = zipInputStream.bufferedReader().readText()
                        }
                        "formulas.xml" -> {
                            draft.formulas.addAll(parseFormulasXml(zipInputStream))
                        }
                        "bricks.xml" -> {
                            draft.bricks.addAll(parseBricksXml(zipInputStream))
                        }
                    }
                    entry = zipInputStream.nextEntry
                }
                draft
            }
        } catch (e: Exception) {
            Log.e("NewlibImporter", "Failed to import .newlib file", e)
            null
        }
    }

    private fun parseFormulasXml(stream: InputStream): List<EditableFormula> {
        val formulas = mutableListOf<EditableFormula>()
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(stream, null)

        var currentFormula: EditableFormula? = null
        var eventType = parser.eventType

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "formula" -> {
                            val id = parser.getAttributeValue(null, "id") ?: ""
                            val function = parser.getAttributeValue(null, "function") ?: ""
                            val displayName = parser.getAttributeValue(null, "displayName") ?: ""
                            currentFormula = EditableFormula(id, function, displayName, mutableListOf())
                        }
                        "param" -> {
                            val type = parser.getAttributeValue(null, "type") ?: ""
                            val default = parser.getAttributeValue(null, "default") ?: ""
                            currentFormula?.params?.add(EditableParam(type, default, null))
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "formula" && currentFormula != null) {
                        formulas.add(currentFormula)
                        currentFormula = null
                    }
                }
            }
            eventType = parser.next()
        }
        return formulas
    }

    private fun parseBricksXml(stream: InputStream): List<EditableBrick> {
        val bricks = mutableListOf<EditableBrick>()
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(stream, null)

        var currentBrick: EditableBrick? = null
        var eventType = parser.eventType

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "brick" -> {
                            val id = parser.getAttributeValue(null, "id") ?: ""
                            val function = parser.getAttributeValue(null, "function") ?: ""
                            val header = parser.getAttributeValue(null, "header") ?: ""
                            currentBrick = EditableBrick(id, function, header, mutableListOf())
                        }
                        "param" -> {
                            val type = parser.getAttributeValue(null, "type") ?: ""
                            val name = parser.getAttributeValue(null, "name") ?: ""
                            currentBrick?.params?.add(EditableParam(type, null, name))
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "brick" && currentBrick != null) {
                        bricks.add(currentBrick)
                        currentBrick = null
                    }
                }
            }
            eventType = parser.next()
        }
        return bricks
    }
}