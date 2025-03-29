package org.catrobat.catroid.content

// Публичный Singleton класс для управления пользовательскими переменными
object UserVarsManager {

    // Хранилище для переменных
    private val variables = mutableMapOf<String, String>()

    // Устанавливает значение переменной. Если она существует, обновляет её,
    // если нет - создает новую.
    fun setVar(name: String, value: String) {
        variables[name] = value
    }

    // Получает значение переменной по имени. Если переменная не найдена, возвращает null.
    fun getVar(name: String): String? {
        return variables[name]
    }

    // Очищает все переменные.
    fun clearVars() {
        variables.clear()
    }

    // Удаляет переменную по имени.
    fun delVar(name: String) {
        variables.remove(name)
    }

    // Получает название переменной с указанным индексом
    fun getVarName(id: Int): String? {
        return variables.keys.elementAtOrNull(id)
    }

    // Получает значение переменной с указанным индексом
    fun getVarValue(id: Int): String? {
        val key = getVarName(id)
        return if (key != null) {
            variables[key]
        } else {
            null
        }
    }
}

