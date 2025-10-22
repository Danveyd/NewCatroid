package org.catrobat.catroid.content

object UserVarsManager {
    private val variables = mutableMapOf<String, String>()

    fun setVar(name: kotlin.String, value: kotlin.String) {
        variables[name] = value
    }

    fun getVar(name: String): String? {
        return variables[name]
    }

    fun clearVars() {
        variables.clear()
    }

    fun delVar(name: String) {
        variables.remove(name)
    }

    fun getVarName(id: Int): String? {
        return variables.keys.elementAtOrNull(id)
    }

    fun getVarValue(id: Int): String? {
        val key = getVarName(id)
        return if (key != null) {
            variables[key]
        } else {
            null
        }
    }
}

