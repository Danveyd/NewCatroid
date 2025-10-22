package org.catrobat.catroid.utils.git

import org.catrobat.catroid.content.Project
import org.catrobat.catroid.io.XstreamSerializer

object XStreamUtilGit {
    private const val XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>\n"
    private val xstream = XstreamSerializer.getInstance().xstream

    fun fromXML(xml: String): Project {
        return xstream.fromXML(xml) as Project
    }

    fun toXML(project: Project): String {
        val projectXml = xstream.toXML(project)
        return XML_HEADER + projectXml
    }

    fun toXMLAny(obj: Any?): String? {
        if (obj == null) return null
        return xstream.toXML(obj)
    }

    /**
     * УНИВЕРСАЛЬНАЯ ГЛУБОКАЯ КОПИЯ
     * Теперь может копировать любой объект, который XStream умеет сериализовать.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> deepCopy(obj: T): T {
        return xstream.fromXML(xstream.toXML(obj)) as T
    }
}