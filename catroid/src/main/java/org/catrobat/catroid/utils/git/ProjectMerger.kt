package org.catrobat.catroid.utils.git

import android.util.Log
import org.catrobat.catroid.common.LookData
import org.catrobat.catroid.common.SoundInfo
import org.catrobat.catroid.content.*
import org.catrobat.catroid.content.bricks.Brick
import org.catrobat.catroid.content.bricks.NoteBrick
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.UserList
import org.catrobat.catroid.formulaeditor.UserVariable
import org.koin.ext.scope
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.*
import java.util.regex.Pattern

class ProjectMerger {
    companion object {
        private const val TAG = "ProjectMerger"
        private val NULL_ID = Any()
    }

    private val conflicts = mutableListOf<Conflict>()

    fun merge(baseProject: Project, localProject: Project, remoteProject: Project): MergeResultData {
        conflicts.clear()
        Log.d(TAG, "Starting V8 (conflict resolution merge)...")

        // Начинаем с глубокой копии локального проекта, чтобы сохранить все локальные изменения и ID
        val mergedProject = XStreamUtilGit.deepCopy(localProject)

        // Рекурсивно сливаем все объекты, изменяя mergedProject
        mergeObject("project", mergedProject, baseProject, remoteProject)

        Log.d(TAG, "Semantic merge finished. Found ${conflicts.size} conflicts to resolve.")

        // После основного слияния, применяем нашу новую логику разрешения конфликтов
        if (conflicts.isNotEmpty()) {
            applyConflictResolutions(mergedProject, conflicts, localProject)
        }

        return MergeResultData(mergedProject, conflicts)
    }

    private fun applyConflictResolutions(mergedProject: Project, conflicts: List<Conflict>, localProject: Project) {
        for (conflict in conflicts) {
            try {
                Log.d(TAG, "Attempting to resolve conflict at: ${conflict.path}")
                if (tryResolveBrickConflict(mergedProject, localProject, conflict)) continue
                if (tryResolveScriptConflict(mergedProject, localProject, conflict)) continue
                // Сюда можно будет добавить другие резолверы для других типов объектов
            } catch (e: Exception) {
                Log.e(TAG, "Failed to programmatically resolve conflict at ${conflict.path}", e)
            }
        }
    }

    /**
     * Разрешает конфликт в конкретном блоке.
     * Стратегия: в слитом проекте уже находится remote-версия блока. Мы находим
     * локальную версию в исходном локальном проекте, клонируем, комментируем
     * и вставляем рядом с remote-версией, обрамляя комментариями.
     */
    private fun tryResolveBrickConflict(mergedProject: Project, localProject: Project, conflict: Conflict): Boolean {
        // Убеждаемся, что это конфликт изменения блока
        if (conflict.localValue !is Brick || conflict.remoteValue !is Brick) return false

        // Находим родительский скрипт в УЖЕ СЛИТОМ проекте
        val parentScript = findObjectByPath(mergedProject, conflict.path.substringBeforeLast(".")) as? Script ?: return false
        val brickList = parentScript.brickList

        // Находим remote-версию блока (которая сейчас в списке)
        val remoteBrickInList = brickList.find { findId(it) == findId(conflict.remoteValue) } ?: return false
        val index = brickList.indexOf(remoteBrickInList)

        // Находим оригинальную локальную версию блока
        val originalLocalBrick = findObjectByPath(localProject, conflict.path) as? Brick ?: return false

        if (index != -1) {
            Log.i(TAG, "Resolving BRICK conflict for brick ${findId(originalLocalBrick)}. Duplicating local version.")
            val localCopy = XStreamUtilGit.deepCopy(originalLocalBrick)
            localCopy.isCommentedOut = true

            val startComment = NoteBrick("--- КОНФЛИКТ: ВАША ВЕРСИЯ НИЖЕ ---")
            val endComment = NoteBrick("--- КОНЕЦ КОНФЛИКТА ---")

            // Вставляем все в список после remote-версии
            brickList.add(index + 1, startComment)
            brickList.add(index + 2, localCopy)
            brickList.add(index + 3, endComment)
            return true
        }
        return false
    }

    /**
     * Разрешает конфликт на уровне всего скрипта (например, разный набор блоков).
     */
    private fun tryResolveScriptConflict(mergedProject: Project, localProject: Project, conflict: Conflict): Boolean {
        if (conflict.localValue !is Script || conflict.remoteValue !is Script) return false

        val parentSprite = findObjectByPath(mergedProject, conflict.path.substringBeforeLast(".")) as? Sprite ?: return false
        val scriptList = parentSprite.scriptList

        val remoteScriptInList = scriptList.find { findId(it) == findId(conflict.remoteValue) } ?: return false
        val index = scriptList.indexOf(remoteScriptInList)

        val originalLocalScript = findObjectByPath(localProject, conflict.path) as? Script ?: return false

        if (index != -1) {
            Log.i(TAG, "Resolving SCRIPT conflict for script ${findId(originalLocalScript)}. Duplicating local version.")
            val localCopy = XStreamUtilGit.deepCopy(originalLocalScript)
            localCopy.isCommentedOut = true // Комментируем весь скрипт-дубликат
            scriptList.add(index + 1, localCopy) // Вставляем его после оригинала
            return true
        }
        return false
    }

    /**
     * Находит объект в дереве проекта по его пути (например, "project.sceneList[...].spriteList[...]").
     */
    private val pathPattern: Pattern = Pattern.compile("(.*)\\[(.*)]")

    private fun findObjectByPath(root: Any, path: String): Any? {
        if (path == "project") return root

        var currentObject: Any? = root
        val segments = path.substringAfter(".").split('.')

        for (segment in segments) {
            if (currentObject == null) return null

            val matcher = pathPattern.matcher(segment)
            currentObject = if (matcher.matches()) {
                val listName = matcher.group(1)
                val itemId = matcher.group(2)

                val listField = getAllFields(currentObject.javaClass).find { it.name == listName } ?: return null
                listField.isAccessible = true
                val list = listField.get(currentObject) as? List<*> ?: return null

                list.find { findId(it).toString() == itemId }
            } else {
                val field = getAllFields(currentObject.javaClass).find { it.name == segment } ?: return null
                field.isAccessible = true
                field.get(currentObject)
            }
        }
        return currentObject
    }

    /*fun merge(baseProject: Project, localProject: Project, remoteProject: Project): MergeResultData {
        conflicts.clear()
        Log.d(TAG, "Starting V7 (deep list comparison) semantic merge...")
        val mergedProject = XStreamUtilGit.deepCopy(localProject)
        mergeObject("project", mergedProject, baseProject, remoteProject)
        Log.d(TAG, "Semantic merge finished. Conflicts found: ${conflicts.size}")
        /*if (conflicts.isNotEmpty()) {
            throw MergeConflictException(conflicts)
        }*/
        if (conflicts.isNotEmpty()) {
            applyConflictResolutions(mergedProject, conflicts, baseProject, localProject, remoteProject)
        }

        return MergeResultData(mergedProject, conflicts)
    }*/

    @Suppress("UNCHECKED_CAST")
    private fun mergeObject(path: String, merged: Any, base: Any, remote: Any) {
        if (merged.javaClass != base.javaClass || base.javaClass != remote.javaClass) return

        for (field in getAllFields(merged.javaClass)) {
            try {
                if (Modifier.isStatic(field.modifiers) || Modifier.isTransient(field.modifiers)) continue
                field.isAccessible = true

                val mergedValue = field.get(merged)
                val baseValue = field.get(base)
                val remoteValue = field.get(remote)

                if (areObjectsEqual(mergedValue, baseValue) && areObjectsEqual(baseValue, remoteValue)) {
                    continue
                }

                val currentPath = "$path.${field.name}"
                Log.d(TAG, "Difference found in field '$currentPath'")

                when {
                    mergedValue is MutableList<*> && baseValue is List<*> && remoteValue is List<*> -> {
                        Log.d(TAG, "-> Entering list merge for '$currentPath'")
                        mergeList(currentPath, mergedValue as MutableList<Any?>, baseValue as List<Any?>, remoteValue as List<Any?>)
                    }
                    isMergableObject(mergedValue, baseValue, remoteValue) -> {
                        Log.d(TAG, "-> Entering recursive merge for '$currentPath'")
                        mergeObject(currentPath, mergedValue!!, baseValue!!, remoteValue!!)
                    }
                    else -> {
                        val remoteChanged = !areObjectsEqual(baseValue, remoteValue)
                        val localChanged = !areObjectsEqual(baseValue, mergedValue)

                        Log.d(TAG, "  -> Path '$currentPath': remoteChanged[$remoteChanged], localChanged[$localChanged]")

                        if (remoteChanged && !localChanged) {
                            Log.i(TAG, "  -> APPLY: Applying remote value to '$currentPath'")
                            field.set(merged, remoteValue)
                        } else if (!remoteChanged && localChanged) {
                            Log.i(TAG, "  -> KEEP: Keeping local change for '$currentPath'")
                        } else if (remoteChanged && localChanged && !areObjectsEqual(mergedValue, remoteValue)) {
                            Log.w(TAG, "  -> CONFLICT: Value changed on both sides at '$currentPath'.")
                            conflicts.add(Conflict(currentPath, field.name, baseValue, mergedValue, remoteValue))
                            field.set(merged, remoteValue)
                        }
                    }
                }
            } catch (e: Exception) { Log.e(TAG, "Error merging field ${field.name} at path $path", e) }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun mergeList(path: String, mergedList: MutableList<Any?>, baseList: List<Any?>, remoteList: List<Any?>) {
        val idSelector: (Any?) -> Any? = { item -> findId(item) }

        val baseMap = baseList.associateBy(idSelector)
        val localMap = mergedList.associateBy(idSelector)
        val remoteMap = remoteList.associateBy(idSelector)

        val allIds = (baseMap.keys + localMap.keys + remoteMap.keys).distinct()

        val resultList = mutableListOf<Any?>()

        for (id in allIds) {
            val baseItem = baseMap[id]
            val localItem = localMap[id]
            val remoteItem = remoteMap[id]

            val inBase = baseItem != null
            val inLocal = localItem != null
            val inRemote = remoteItem != null

            when {
                // Кейс 1: Модификация существующего элемента
                inBase && inLocal && inRemote -> {
                    val remoteChanged = !areObjectsEqual(baseItem, remoteItem)
                    val localChanged = !areObjectsEqual(baseItem, localItem)

                    if (localChanged && !remoteChanged) resultList.add(localItem) // Только локальные изменения
                    else if (!localChanged && remoteChanged) resultList.add(remoteItem) // Только удаленные изменения
                    else if (localChanged && remoteChanged) {
                        if (areObjectsEqual(localItem, remoteItem)) {
                            resultList.add(localItem)
                        } else {
                            val mergedItem = XStreamUtilGit.deepCopy(localItem!!)
                            mergeObject("$path[$id]", mergedItem, baseItem!!, remoteItem!!)
                            resultList.add(mergedItem)
                        }
                    } else {
                        resultList.add(localItem) // Без изменений
                    }
                }

                // Кейс 2: Добавление
                !inBase && inLocal && !inRemote -> resultList.add(localItem) // Добавлено локально
                !inBase && !inRemote && inLocal -> resultList.add(remoteItem) // Добавлено удаленно
                !inBase && inLocal && inRemote -> { // Конфликт add/add
                    conflicts.add(Conflict(path, "add/add on item $id", null, localItem, remoteItem))
                    resultList.add(localItem)
                    if (!areObjectsEqual(localItem, remoteItem)) resultList.add(remoteItem)
                }

                // Кейс 3: Удаление
                inBase && !inLocal && inRemote -> { // Удалено локально
                    if (areObjectsEqual(baseItem, remoteItem)) { /* не добавляем в список */ }
                    else {
                        conflicts.add(Conflict(path, "delete/modify on item $id", baseItem, localItem, null))
                    }
                }
                inBase && inLocal && !inRemote -> { // Удалено удаленно
                    if (areObjectsEqual(baseItem, localItem)) { /* не добавляем в список */ }
                    else {
                        conflicts.add(Conflict(path, "modify/delete on item $id", baseItem, localItem, null))
                        resultList.add(localItem) // Сохраняем локальные изменения
                    }
                }
            }
        }

        mergedList.clear()
        mergedList.addAll(resultList)
    }

    private fun applyConflictResolutions(project: Project, conflicts: List<Conflict>, base: Project, local: Project, remote: Project) {
        for (conflict in conflicts) {
            try {
                Log.d(TAG, "Attempting to resolve conflict at: ${conflict.path}")
                if (tryResolveBrickConflict(project, conflict)) continue
                if (tryResolveScriptConflict(project, conflict)) continue
            } catch (e: Exception) {
                Log.e(TAG, "Failed to resolve conflict at ${conflict.path}", e)
            }
        }
    }

    private fun tryResolveScriptConflict(project: Project, conflict: Conflict): Boolean {
        val localScript = conflict.localValue as? Script ?: return false

        // Находим родительский спрайт и его scriptList в УЖЕ СЛИТОМ проекте
        val parentSprite = findObjectByPath(project, conflict.path.substringBeforeLast(".")) as? Sprite ?: return false
        val scriptList = parentSprite.getScriptList()
        val remoteScript = scriptList.find { findId(it) == findId(localScript) } ?: return false
        val index = scriptList.indexOf(remoteScript)

        if (index != -1) {
            Log.d(TAG, "Resolving SCRIPT conflict for script ${findId(localScript)}")
            val localCopy = localScript.clone() as Script
            localCopy.setCommentedOut(true)
            scriptList.add(index + 1, localCopy)
            return true
        }
        return false
    }

    private fun tryResolveBrickConflict(project: Project, conflict: Conflict): Boolean {
        // Проверяем, что конфликт - это модификация блока
        val localBrick = conflict.localValue as? Brick ?: return false
        val remoteBrick = conflict.remoteValue as? Brick ?: return false

        // Находим родительский скрипт в слитом проекте
        val parentScript = findObjectByPath(project, conflict.path.substringBeforeLast(".")) as? Script ?: return false
        val brickList = parentScript.getBrickList()

        // Находим remote-версию блока в списке
        val remoteBrickInList = brickList.find { findId(it) == findId(localBrick) } ?: return false
        val index = brickList.indexOf(remoteBrickInList)

        if (index != -1) {
            Log.d(TAG, "Resolving BRICK conflict for brick ${findId(localBrick)}")
            val localCopy = localBrick.clone() as Brick
            localCopy.setCommentedOut(true)

            val startComment = NoteBrick("--- КОНФЛИКТ --- (Ниже версия с сервера)")
            val midComment = NoteBrick("--- ВАША ВЕРСИЯ ---")
            val endComment = NoteBrick("--- КОНЕЦ КОНФЛИКТА ---")

            brickList.add(index, startComment)
            brickList.add(index + 2, midComment) // После remoteBrick
            brickList.add(index + 3, localCopy)
            brickList.add(index + 4, endComment)
            return true
        }
        return false
    }

    // --- ФУНКЦИЯ ПОИСКА ПО ПУТИ ---
    /*private val pathPattern = Pattern.compile("(.*)\\[(.*)]")

    private fun findObjectByPath(root: Any, path: String): Any? {
        if (path == "project") return root

        var currentObject: Any? = root
        val segments = path.split(".").drop(1)

        for (segment in segments) {
            if (currentObject == null) return null

            val matcher = pathPattern.matcher(segment)
            if (matcher.matches()) {
                val listName = matcher.group(1)
                val itemId = matcher.group(2)

                val listField = getAllFields(currentObject.javaClass).find { it.name == listName } ?: return null
                listField.isAccessible = true
                val list = listField.get(currentObject) as? List<*> ?: return null

                currentObject = list.find { findId(it) == itemId }
            } else {
                val field = getAllFields(currentObject.javaClass).find { it.name == segment } ?: return null
                field.isAccessible = true
                currentObject = field.get(currentObject)
            }
        }
        return currentObject
    }*/



    private fun areObjectsEqual(o1: Any?, o2: Any?): Boolean {
        if (o1 === o2) return true
        if (o1 == null || o2 == null || o1.javaClass != o2.javaClass) return false

        return areSemanticallyEqual(o1, o2)
    }

    /**
     * Эта функция — сердце мёрджера. Она рекурсивно сравнивает содержимое объектов,
     * игнорируя их ID-based .equals() методы.
     */
    private fun areSemanticallyEqual(o1: Any, o2: Any): Boolean {
        return when (o1) {
            is List<*> -> areListsSemanticallyEqual(o1, o2 as List<*>)

            is Project -> areFieldsEqual(o1, o2)
            is Scene -> areFieldsEqual(o1, o2)
            is Sprite -> areFieldsEqual(o1, o2)
            is Script -> areFieldsEqual(o1, o2)

            is LookData, is SoundInfo, is UserVariable, is UserList -> o1.equals(o2)

            is XmlHeader, is Brick, is Formula -> XStreamUtilGit.toXMLAny(o1) == XStreamUtilGit.toXMLAny(o2)

            else -> Objects.equals(o1, o2)
        }
    }

    private fun areListsSemanticallyEqual(l1: List<*>, l2: List<*>): Boolean {
        if (l1.size != l2.size) return false
        val sortedL1 = l1.sortedBy { findId(it).hashCode() }
        val sortedL2 = l2.sortedBy { findId(it).hashCode() }

        for (i in sortedL1.indices) {
            if (!areSemanticallyEqual(sortedL1[i]!!, sortedL2[i]!!)) {
                return false
            }
        }
        return true
    }

    /**
     * Общий метод для сравнения двух объектов по значениям их полей.
     */
    private fun areFieldsEqual(o1: Any, o2: Any): Boolean {
        for (field in getAllFields(o1.javaClass)) {
            if (Modifier.isStatic(field.modifiers) || Modifier.isTransient(field.modifiers)) continue
            field.isAccessible = true
            val v1 = field.get(o1)
            val v2 = field.get(o2)
            if (field.name.endsWith("Id")) continue

            if (!areObjectsEqual(v1, v2)) {
                return false
            }
        }
        return true
    }

    private fun findId(item: Any?): Any {
        if (item == null) return NULL_ID
        return when (item) {
            is Sprite -> item.spriteId
            is Scene -> item.sceneId
            is Script -> item.scriptId
            is Brick -> item.brickID
            is LookData -> item.lookId
            is SoundInfo -> item.soundId
            is UserVariable -> item.name
            is UserList -> item.name
            else -> item.hashCode()
        }
    }

    private fun isMergableObject(o1: Any?, o2: Any?, o3: Any?): Boolean {
        if (o1 == null || o2 == null || o3 == null) return false
        val isNotPrimitive = o1 !is String && o1 !is Number && o1 !is Boolean && o1 !is Enum<*> && o1 !is Class<*>
        val packageName = o1.javaClass.`package`?.name ?: ""
        return isNotPrimitive && packageName.startsWith("org.catrobat")
    }

    private fun getAllFields(startClass: Class<*>): List<Field> {
        val allFields = mutableListOf<Field>()
        var currentClass: Class<*>? = startClass
        while (currentClass != null && currentClass != Any::class.java) {
            allFields.addAll(currentClass.declaredFields)
            currentClass = currentClass.superclass
        }
        return allFields
    }
}