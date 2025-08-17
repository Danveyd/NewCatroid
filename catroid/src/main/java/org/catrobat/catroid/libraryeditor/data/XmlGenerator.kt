package org.catrobat.catroid.libraryeditor.data

import android.util.Xml
import org.catrobat.catroid.libraryeditor.data.EditableBrick
import org.catrobat.catroid.libraryeditor.data.EditableFormula
import org.catrobat.catroid.libraryeditor.data.EditableParam
import java.io.StringWriter

object XmlGenerator {

    fun generateFormulasXml(formulas: List<EditableFormula>): String {
        val writer = StringWriter()
        val serializer = Xml.newSerializer()
        serializer.setOutput(writer)
        serializer.startDocument("UTF-8", true)
        serializer.startTag(null, "formulas")

        for (formula in formulas) {
            serializer.startTag(null, "formula")
            serializer.attribute(null, "id", formula.id)
            serializer.attribute(null, "function", formula.functionName)
            serializer.attribute(null, "displayName", formula.displayName)

            serializer.startTag(null, "params")
            for (param in formula.params) {
                serializer.startTag(null, "param")
                serializer.attribute(null, "type", param.type)
                serializer.attribute(null, "default", param.defaultValue ?: "")
                serializer.endTag(null, "param")
            }
            serializer.endTag(null, "params")
            serializer.endTag(null, "formula")
        }

        serializer.endTag(null, "formulas")
        serializer.endDocument()
        return writer.toString()
    }

    fun generateBricksXml(bricks: List<EditableBrick>): String {
        val writer = StringWriter()
        val serializer = Xml.newSerializer()
        serializer.setOutput(writer)
        serializer.startDocument("UTF-8", true)
        serializer.startTag(null, "bricks")

        for (brick in bricks) {
            serializer.startTag(null, "brick")
            serializer.attribute(null, "id", brick.id)
            serializer.attribute(null, "function", brick.functionName)
            serializer.attribute(null, "header", brick.headerText)

            serializer.startTag(null, "params")
            for (param in brick.params) {
                serializer.startTag(null, "param")
                serializer.attribute(null, "type", param.type)
                serializer.attribute(null, "name", param.name ?: "")
                serializer.endTag(null, "param")
            }
            serializer.endTag(null, "params")
            serializer.endTag(null, "brick")
        }

        serializer.endTag(null, "bricks")
        serializer.endDocument()
        return writer.toString()
    }
}