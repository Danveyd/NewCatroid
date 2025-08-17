// org/catrobat/catroid/libraries/CustomBrickManager.kt
package org.catrobat.catroid.libraries

import android.util.Log

object CustomBrickManager {
    private val definitions = mutableMapOf<String, CustomBrickDefinition>()

    fun registerBrick(definition: CustomBrickDefinition) {
        if (definitions.containsKey(definition.id)) {
            Log.w("CustomBrickManager", "Переопределение блока: ${definition.id}")
        }
        definitions[definition.id] = definition
    }

    fun removeBricksByOwner(libraryId: String) {
        val idsToRemove = definitions.filter { it.value.ownerLibraryId == libraryId }.keys
        idsToRemove.forEach { definitions.remove(it) }
        Log.i("CustomBrickManager", "Удалены блоки, принадлежащие $libraryId")
    }

    fun findDefinitionById(id: String): CustomBrickDefinition? = definitions[id]

    fun getAllDefinitions(): List<CustomBrickDefinition> = definitions.values.toList()
}