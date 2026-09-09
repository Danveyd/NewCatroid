package org.catrobat.catroid.ai

import android.util.Log
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.content.bricks.Brick
import org.catrobat.catroid.content.bricks.CompositeBrick
import org.catrobat.catroid.content.bricks.FormulaBrick
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.FormulaElement
import org.catrobat.catroid.formulaeditor.UserVariable
import org.catrobat.catroid.common.LookData
import org.catrobat.catroid.common.SoundInfo
import org.catrobat.catroid.content.bricks.ElseIfBranch
import org.catrobat.catroid.content.bricks.IfLogicBeginBrick
import org.catrobat.catroid.content.bricks.TryCatchFinallyBrick
import org.catrobat.catroid.formulaeditor.UserList
import java.lang.reflect.Modifier

enum class FormulaElementType {
    OPERATOR, FUNCTION, NUMBER, SENSOR, USER_VARIABLE, USER_LIST, USER_DEFINED_BRICK_INPUT, BRACKET, STRING, COLLISION_FORMULA
}

data class ParsedFormulaElement(
    val type: FormulaElementType,
    val value: String,
    val leftChild: ParsedFormulaElement? = null,
    val rightChild: ParsedFormulaElement? = null,
    val additionalChildren: List<ParsedFormulaElement> = emptyList()
)

data class ParsedElseIf(
    val condition: ParsedFormulaElement,
    val body: List<ParsedBrick>
)

sealed class ParsedBrick {
    data class Simple(
        val name: String,
        val arguments: Map<String, ParsedFormulaElement>
    ) : ParsedBrick()

    data class If(
        val condition: ParsedFormulaElement,
        val thenBranch: List<ParsedBrick>,
        val elseIfBranches: List<ParsedElseIf> = emptyList(),
        val elseBranch: List<ParsedBrick>? = null
    ) : ParsedBrick()

    data class Composite(
        val name: String,
        val arguments: Map<String, ParsedFormulaElement>,
        val body: List<ParsedBrick>
    ) : ParsedBrick()

    data class TryCatch(
        val tryBranch: List<ParsedBrick>,
        val catchVar: String?,
        val catchBranch: List<ParsedBrick>,
        val finallyBranch: List<ParsedBrick>?
    ) : ParsedBrick()
}

object KoveModelConverter {

    private const val TAG = "KoveModelConverter"

    fun buildBricks(parsedList: List<ParsedBrick>, sprite: Sprite): List<Brick> {
        val result = mutableListOf<Brick>()
        for (pb in parsedList) {
            val brick = convertSingleBrick(pb, sprite)
            if (brick != null) {
                result.add(brick)
            }
        }
        return result
    }

    fun convertSingleBrick(parsed: ParsedBrick, sprite: Sprite): Brick? {
        return when (parsed) {
            is ParsedBrick.Simple -> {
                val brick = instantiateBrickByName(parsed.name)
                if (brick != null) {
                    populateBrickArguments(brick, parsed.arguments, sprite)
                    applySmartDefaults(brick, sprite)
                }
                brick
            }

            is ParsedBrick.If -> {
                val hasBranches = parsed.elseIfBranches.isNotEmpty() || parsed.elseBranch != null
                val targetName = if (hasBranches) "if_logic_begin" else "if_then_logic_begin"
                val ifBrick = instantiateBrickByName(targetName)

                if (ifBrick != null && ifBrick is CompositeBrick) {
                    val condFormula = buildFormula(parsed.condition)
                    setFormulaOnBrick(ifBrick, Brick.BrickField.IF_CONDITION, condFormula)

                    val thenBranch = buildBricks(parsed.thenBranch, sprite)
                    for (b in thenBranch) {
                        b.setParent(ifBrick)
                        if (ifBrick is IfLogicBeginBrick) ifBrick.addBrickToIfBranch(b)
                        else (ifBrick.nestedBricks as? MutableList<Brick>)?.add(b)
                    }

                    if (ifBrick is IfLogicBeginBrick) {
                        for (elif in parsed.elseIfBranches) {
                            val branchCondition = buildFormula(elif.condition)
                            val branch = ElseIfBranch(branchCondition)
                            val branchBricks = buildBricks(elif.body, sprite)
                            val separator = branch.getSeparatorBrick(ifBrick)
                            separator.setParentIfBrick(ifBrick)

                            for (b in branchBricks) {
                                b.setParent(separator)
                                branch.branchBricks.add(b)
                            }
                            ifBrick.elseIfBranches.add(branch)
                        }

                        if (parsed.elseBranch != null) {
                            val elseBricks = buildBricks(parsed.elseBranch, sprite)
                            val elsePart = ifBrick.allParts.firstOrNull { it is IfLogicBeginBrick.ElseBrick } ?: ifBrick
                            for (b in elseBricks) {
                                b.setParent(elsePart)
                                ifBrick.addBrickToElseBranch(b)
                            }
                        }
                    }
                }
                ifBrick
            }

            is ParsedBrick.Composite -> {
                val compositeBrick = instantiateBrickByName(parsed.name)
                if (compositeBrick != null && compositeBrick is CompositeBrick) {
                    populateBrickArguments(compositeBrick, parsed.arguments, sprite)
                    applySmartDefaults(compositeBrick, sprite)

                    val body = buildBricks(parsed.body, sprite)
                    for (b in body) {
                        b.setParent(compositeBrick)
                    }

                    val targetList = compositeBrick.nestedBricks as? MutableList<Brick>
                    targetList?.addAll(body)
                }
                compositeBrick
            }

            is ParsedBrick.TryCatch -> {
                val tryBrick = instantiateBrickByName("try_catch_finally")
                if (tryBrick != null && tryBrick is TryCatchFinallyBrick) {
                    val tryBody = buildBricks(parsed.tryBranch, sprite)
                    for (b in tryBody) b.setParent(tryBrick)
                    tryBrick.tryBricks.addAll(tryBody)

                    val catchPart = tryBrick.allParts.firstOrNull { it is TryCatchFinallyBrick.CatchBrick } as? TryCatchFinallyBrick.CatchBrick
                    parsed.catchVar?.let { varName ->
                        bindVariableToBrick(catchPart ?: tryBrick, varName, sprite)
                    }

                    val catchBody = buildBricks(parsed.catchBranch, sprite)
                    for (b in catchBody) b.setParent(catchPart ?: tryBrick)
                    tryBrick.catchBricks.addAll(catchBody)

                    if (parsed.finallyBranch != null) {
                        val finallyPart = tryBrick.allParts.firstOrNull { it is TryCatchFinallyBrick.FinallyBrick } ?: tryBrick
                        val finallyBody = buildBricks(parsed.finallyBranch, sprite)
                        for (b in finallyBody) b.setParent(finallyPart)
                        tryBrick.finallyBricks.addAll(finallyBody)
                    }
                }
                tryBrick
            }
        }
    }

    fun convertFormula(parsed: ParsedFormulaElement): Formula {
        return buildFormula(parsed)
    }

    private fun buildFormula(parsed: ParsedFormulaElement, parent: FormulaElement? = null): Formula {
        val rootElement = buildFormulaElement(parsed, parent)
        return Formula(rootElement)
    }

    private fun buildFormulaElement(parsed: ParsedFormulaElement, parent: FormulaElement? = null): FormulaElement {
        val type = FormulaElement.ElementType.valueOf(parsed.type.name)
        val cleanValue = parsed.value.trim('"', '\'')
        val element = FormulaElement(type, cleanValue, parent)

        parsed.leftChild?.let {
            val child = buildFormulaElement(it, element)
            element.setLeftChild(child)
        }
        parsed.rightChild?.let {
            val child = buildFormulaElement(it, element)
            element.setRightChild(child)
        }
        parsed.additionalChildren.forEach {
            val child = buildFormulaElement(it, element)
            element.addAdditionalChild(child)
        }

        return element
    }

    private val dslClassAliases = mapOf(
        "set_size" to listOf("SetSizeToBrick", "SetSizeBrick"),
        "set_size_to" to listOf("SetSizeToBrick", "SetSizeBrick"),
        "change_size" to listOf("ChangeSizeByNBrick", "ChangeSizeBrick"),
        "change_size_by" to listOf("ChangeSizeByNBrick", "ChangeSizeBrick"),
        "set_var" to listOf("SetVariableBrick", "SetVarBrick"),
        "set_variable" to listOf("SetVariableBrick"),
        "change_var" to listOf("ChangeVariableBrick", "ChangeVarBrick"),
        "change_variable" to listOf("ChangeVariableBrick"),
        "change_x" to listOf("ChangeXByNBrick", "ChangeXBrick"),
        "change_y" to listOf("ChangeYByNBrick", "ChangeYBrick"),
        "set_x" to listOf("SetXBrick"),
        "set_y" to listOf("SetYBrick"),
        "play_sound_wait" to listOf("PlaySoundAndWaitBrick", "PlaySoundWaitBrick"),
        "play_sound_and_wait" to listOf("PlaySoundAndWaitBrick"),
        "start_scene" to listOf("SceneStartBrick", "StartSceneBrick"),
        "scene_start" to listOf("SceneStartBrick", "StartSceneBrick"),
        "show_toast" to listOf("ShowToastBrick", "ShowTextBrick"),
        "show_toast_block" to listOf("ShowToastBrick", "ShowTextBrick"),
        "turn_left" to listOf("TurnLeftBrick"),
        "turn_right" to listOf("TurnRightBrick"),
        "point_in_direction" to listOf("PointInDirectionBrick"),
        "point_to" to listOf("PointToBrick"),
        "go_to" to listOf("PlaceAtBrick", "GoToBrick"),
        "forever" to listOf("ForeverBrick"),
        "repeat" to listOf("RepeatBrick"),
        "repeat_until" to listOf("RepeatUntilBrick"),
        "async_repeat" to listOf("AsyncRepeatBrick"),
        "interval_repeat" to listOf("IntervalRepeatBrick"),
        "for_variable" to listOf("ForVariableFromToBrick"),
        "for_item" to listOf("ForItemInUserListBrick"),
        "instant" to listOf("InstantBrick"),
        "spawn_thread" to listOf("SpawnThreadBrick"),
        "run_as_sprite" to listOf("RunAsSpriteBrick"),
        "try_catch_finally" to listOf("TryCatchFinallyBrick"),
        "if_logic_begin" to listOf("IfLogicBeginBrick"),
        "if_then_logic_begin" to listOf("IfThenLogicBeginBrick"),
        "if" to listOf("IfThenLogicBeginBrick"),
    )

    private fun instantiateBrickByName(dslName: String): Brick? {
        val lower = dslName.lowercase()
        val ignoredSuffixes = listOf("_end", "end", "endbrick", "_part", "part", "script")
        if (ignoredSuffixes.any { lower.endsWith(it) } || lower == "if_logic_end" || lower == "loop_end") {
            return null
        }

        val candidateClassNames = mutableListOf<String>()

        dslClassAliases[lower]?.forEach { className ->
            candidateClassNames.add("org.catrobat.catroid.content.bricks.$className")
        }

        val camelCaseName = dslName.split("_").joinToString("") { part ->
            part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
        val trimmedBlockName = camelCaseName.removeSuffix("Block")

        candidateClassNames.add("org.catrobat.catroid.content.bricks.${camelCaseName}Brick")
        candidateClassNames.add("org.catrobat.catroid.content.bricks.${camelCaseName}")
        candidateClassNames.add("org.catrobat.catroid.content.bricks.${trimmedBlockName}Brick")
        candidateClassNames.add("org.catrobat.catroid.content.bricks.${trimmedBlockName}")
        candidateClassNames.add("org.catrobat.catroid.content.bricks.${camelCaseName}ToBrick")
        candidateClassNames.add("org.catrobat.catroid.content.bricks.${camelCaseName}ByNBrick")

        for (className in candidateClassNames) {
            try {
                val clazz = Class.forName(className)
                if (Modifier.isAbstract(clazz.modifiers) || clazz.isInterface) {
                    continue
                }

                val constructor = clazz.getDeclaredConstructor()
                constructor.isAccessible = true
                val brick = constructor.newInstance() as? Brick ?: continue

                if (isValidDisplayableBrick(brick)) {
                    return brick
                }
            } catch (ignored: Exception) {}
        }

        Log.w(TAG, "Failed to instantiate brick for DSL name: '$dslName'. Tried: $candidateClassNames")
        return null
    }

    private fun isValidDisplayableBrick(brick: Brick): Boolean {
        var clazz: Class<*>? = brick.javaClass
        while (clazz != null && clazz != Any::class.java) {
            try {
                val field = clazz.getDeclaredField("viewResource")
                field.isAccessible = true
                val resId = field.getInt(brick)
                return resId != 0
            } catch (ignored: Exception) {}

            try {
                val method = clazz.getDeclaredMethod("getViewResource")
                method.isAccessible = true
                val resId = method.invoke(brick) as? Int ?: 0
                return resId != 0
            } catch (ignored: Exception) {}

            clazz = clazz.superclass
        }
        return true
    }

    private fun populateBrickArguments(brick: Brick, arguments: Map<String, ParsedFormulaElement>, sprite: Sprite) {
        val supportedFormulas: Set<Brick.BrickField> = if (brick is FormulaBrick) {
            brick.allFormulaFieldsWithFormulas?.keys ?: emptySet()
        } else emptySet()

        val brickClassName = brick.javaClass.simpleName

        for ((argName, parsedFormula) in arguments) {
            val formula = buildFormula(parsedFormula)
            val cleanArg = argName.lowercase().replace("_", "")
            var matchedField: Brick.BrickField? = null

            for (field in supportedFormulas) {
                val fieldNameClean = field.name.lowercase().replace("_", "")
                val shortName = getShortParamName(field.name).lowercase().replace("_", "")

                if (cleanArg == shortName || cleanArg == fieldNameClean) {
                    matchedField = field; break
                }
                if (cleanArg == "x" && (field == Brick.BrickField.X_POSITION || field == Brick.BrickField.X_DESTINATION || field == Brick.BrickField.POSX || field == Brick.BrickField.X)) {
                    matchedField = field; break
                }
                if (cleanArg == "y" && (field == Brick.BrickField.Y_POSITION || field == Brick.BrickField.Y_DESTINATION || field == Brick.BrickField.POSY || field == Brick.BrickField.Y)) {
                    matchedField = field; break
                }
                if ((cleanArg == "size" || cleanArg == "textsize") && (field == Brick.BrickField.SIZE || field == Brick.BrickField.TEXTSIZE || field == Brick.BrickField.SIZE_CHANGE)) {
                    matchedField = field; break
                }
                if ((cleanArg == "color" || cleanArg == "textcolor") && (field == Brick.BrickField.COLOR || field == Brick.BrickField.TEXTCOLOR || field == Brick.BrickField.BGCOLOR)) {
                    matchedField = field; break
                }
                if ((cleanArg == "text" || cleanArg == "string") && (field == Brick.BrickField.STRING || field == Brick.BrickField.TEXT || field == Brick.BrickField.STRING_VALUE)) {
                    matchedField = field; break
                }
                if ((cleanArg == "duration" || cleanArg == "time") && (field == Brick.BrickField.TIME_TO_WAIT_IN_SECONDS || field == Brick.BrickField.DURATION_IN_SECONDS)) {
                    matchedField = field; break
                }
            }

            if (matchedField == null && (cleanArg == "val" || cleanArg == "val1") && supportedFormulas.isNotEmpty()) {
                matchedField = supportedFormulas.firstOrNull { it != Brick.BrickField.VARIABLE } ?: supportedFormulas.first()
            }

            if (matchedField != null) {
                setFormulaOnBrick(brick, matchedField, formula)
            } else {
                val constantValue = extractConstantFromFormula(parsedFormula) ?: parsedFormula.value
                val rawStr = constantValue.toString().trim('"', '\'')

                if (brickClassName.contains("Broadcast") || cleanArg == "message" || cleanArg == "msg" || cleanArg == "broadcast") {
                    setBroadcastMessageOnBrick(brick, rawStr)
                }

                if (cleanArg == "val" || cleanArg == "look" || cleanArg == "lookname") {
                    val look = sprite.lookList.find { it.name.equals(rawStr, ignoreCase = true) } ?: sprite.lookList.firstOrNull()
                    if (look != null) setLookOnBrick(brick, look)
                }

                if (cleanArg == "val" || cleanArg == "sound" || cleanArg == "soundname") {
                    val sound = sprite.soundList.find { it.name.equals(rawStr, ignoreCase = true) } ?: sprite.soundList.firstOrNull()
                    if (sound != null) setSoundOnBrick(brick, sound)
                }

                setPrimitiveFieldOnBrick(brick, argName, rawStr, sprite)
            }

            if (parsedFormula.type == FormulaElementType.USER_VARIABLE || cleanArg == "var" || cleanArg == "variable") {
                bindVariableToBrick(brick, parsedFormula.value, sprite)
            }
            if (parsedFormula.type == FormulaElementType.USER_LIST || cleanArg == "list") {
                bindListToBrick(brick, parsedFormula.value, sprite)
            }
        }
    }

    private fun setBroadcastMessageOnBrick(brick: Brick, message: String) {
        val cleanMsg = message.trim('"', '\'')
        if (cleanMsg.isEmpty()) return

        val project = ProjectManager.getInstance().currentProject
        project?.broadcastMessageContainer?.let { container ->
            registerBroadcastMessageInContainer(container, cleanMsg)
        }

        var clazz: Class<*>? = brick.javaClass
        while (clazz != null && clazz.name.startsWith("org.catrobat.catroid")) {
            for (field in clazz.declaredFields) {
                if (field.name.equals("broadcastMessage", ignoreCase = true) || field.name.equals("message", ignoreCase = true)) {
                    field.isAccessible = true
                    try { field.set(brick, cleanMsg); return } catch (ignored: Exception) {}
                }
            }
            for (method in clazz.declaredMethods) {
                if ((method.name.equals("setBroadcastMessage", ignoreCase = true) || method.name.equals("setMessage", ignoreCase = true)) &&
                    method.parameterCount == 1 && method.parameterTypes[0] == String::class.java) {
                    method.isAccessible = true
                    try { method.invoke(brick, cleanMsg); return } catch (ignored: Exception) {}
                }
            }
            clazz = clazz.superclass
        }
    }

    private fun registerBroadcastMessageInContainer(container: Any, messageName: String) {
        try {
            val method = container.javaClass.getMethod("addBroadcastMessage", String::class.java)
            method.invoke(container, messageName)
            return
        } catch (ignored: Exception) {}
        try {
            val method = container.javaClass.getMethod("add", String::class.java)
            method.invoke(container, messageName)
            return
        } catch (ignored: Exception) {}
        for (field in container.javaClass.declaredFields) {
            if (java.util.Collection::class.java.isAssignableFrom(field.type)) {
                field.isAccessible = true
                try {
                    @Suppress("UNCHECKED_CAST")
                    val col = field.get(container) as? MutableCollection<String>
                    col?.add(messageName)
                    break
                } catch (ignored: Exception) {}
            }
        }
    }

    private fun bindVariableToBrick(brick: Brick, variableName: String, sprite: Sprite) {
        val cleanName = variableName.trim('"', '\'')
        val project = ProjectManager.getInstance().currentProject

        val userVar = if (cleanName.isNotEmpty()) {
            project?.userVariables?.find { it.name == cleanName }
                ?: sprite.userVariables.find { it.name == cleanName }
                ?: UserVariable(cleanName)
        } else {
            project?.userVariables?.firstOrNull() ?: sprite.userVariables.firstOrNull()
        }

        if (userVar == null) return

        var clazz: Class<*>? = brick.javaClass
        while (clazz != null && clazz.name.startsWith("org.catrobat.catroid")) {
            for (field in clazz.declaredFields) {
                if (UserVariable::class.java.isAssignableFrom(field.type)) {
                    field.isAccessible = true
                    try { field.set(brick, userVar); return } catch (ignored: Exception) {}
                }
            }
            for (method in clazz.declaredMethods) {
                if (method.name.equals("setUserVariable", ignoreCase = true) && method.parameterCount == 1) {
                    method.isAccessible = true
                    try { method.invoke(brick, userVar); return } catch (ignored: Exception) {}
                }
            }
            clazz = clazz.superclass
        }
    }

    private fun bindListToBrick(brick: Brick, listName: String, sprite: Sprite) {
        val cleanName = listName.trim('"', '\'')
        val project = ProjectManager.getInstance().currentProject

        val userList = if (cleanName.isNotEmpty()) {
            project?.userLists?.find { it.name == cleanName }
                ?: sprite.userLists.find { it.name == cleanName }
                ?: UserList(cleanName)
        } else {
            project?.userLists?.firstOrNull() ?: sprite.userLists.firstOrNull()
        }

        if (userList == null) return

        var clazz: Class<*>? = brick.javaClass
        while (clazz != null && clazz.name.startsWith("org.catrobat.catroid")) {
            for (field in clazz.declaredFields) {
                if (UserList::class.java.isAssignableFrom(field.type)) {
                    field.isAccessible = true
                    try { field.set(brick, userList); return } catch (ignored: Exception) {}
                }
            }
            for (method in clazz.declaredMethods) {
                if (method.name.equals("setUserList", ignoreCase = true) && method.parameterCount == 1) {
                    method.isAccessible = true
                    try { method.invoke(brick, userList); return } catch (ignored: Exception) {}
                }
            }
            clazz = clazz.superclass
        }
    }

    fun registerNewEntities(bricks: List<Brick>, sprite: Sprite, project: org.catrobat.catroid.content.Project) {
        val container = project.broadcastMessageContainer

        for (brick in bricks) {
            extractBroadcastMessageFromBrick(brick)?.let { msg ->
                if (msg.isNotEmpty()) {
                    registerBroadcastMessageInContainer(container, msg)
                }
            }

            extractUserVariableFromBrick(brick)?.let { uVar ->
                ensureVariableRegistered(uVar.name, sprite, project)
            }

            extractUserListFromBrick(brick)?.let { uList ->
                ensureListRegistered(uList.name, sprite, project)
            }

            if (brick is FormulaBrick) {
                brick.allFormulaFieldsWithFormulas?.values?.forEach { formula ->
                    val varsInFormula = mutableSetOf<String>()
                    val listsInFormula = mutableSetOf<String>()
                    collectEntitiesFromFormulaTree(formula.formulaTree, varsInFormula, listsInFormula)

                    varsInFormula.forEach { ensureVariableRegistered(it, sprite, project) }
                    listsInFormula.forEach { ensureListRegistered(it, sprite, project) }
                }
            }
        }

        try {
            org.catrobat.catroid.content.Sprite.resolveSpriteReferences(sprite, project)
            container.update()
        } catch (ignored: Exception) {}
    }

    private fun ensureVariableRegistered(name: String?, sprite: Sprite, project: org.catrobat.catroid.content.Project) {
        if (name.isNullOrEmpty()) return
        val existsInSprite = sprite.userVariables.any { it.name == name }
        val existsInProject = project.userVariables.any { it.name == name }
        if (!existsInSprite && !existsInProject) {
            sprite.addUserVariable(UserVariable(name))
            Log.i(TAG, "Auto-created new UserVariable: '$name' for sprite ${sprite.name}")
        }
    }

    private fun ensureListRegistered(name: String?, sprite: Sprite, project: org.catrobat.catroid.content.Project) {
        if (name.isNullOrEmpty()) return
        val existsInSprite = sprite.userLists.any { it.name == name }
        val existsInProject = project.userLists.any { it.name == name }
        if (!existsInSprite && !existsInProject) {
            sprite.addUserList(org.catrobat.catroid.formulaeditor.UserList(name))
            Log.i(TAG, "Auto-created new UserList: '$name' for sprite ${sprite.name}")
        }
    }

    private fun collectEntitiesFromFormulaTree(
        element: FormulaElement?,
        variables: MutableSet<String>,
        lists: MutableSet<String>
    ) {
        if (element == null) return
        if (element.elementType == FormulaElement.ElementType.USER_VARIABLE) {
            element.value?.let { if (it.isNotEmpty()) variables.add(it) }
        }
        if (element.elementType == FormulaElement.ElementType.USER_LIST) {
            element.value?.let { if (it.isNotEmpty()) lists.add(it) }
        }
        collectEntitiesFromFormulaTree(element.leftChild, variables, lists)
        collectEntitiesFromFormulaTree(element.rightChild, variables, lists)
        element.additionalChildren?.forEach { collectEntitiesFromFormulaTree(it, variables, lists) }
    }

    private fun extractBroadcastMessageFromBrick(brick: Brick): String? {
        var clazz: Class<*>? = brick.javaClass
        while (clazz != null && clazz.name.startsWith("org.catrobat.catroid")) {
            for (field in clazz.declaredFields) {
                if (field.name.equals("broadcastMessage", ignoreCase = true) || field.name.equals("message", ignoreCase = true)) {
                    field.isAccessible = true
                    try { return field.get(brick) as? String } catch (ignored: Exception) {}
                }
            }
            clazz = clazz.superclass
        }
        return null
    }

    private fun extractUserVariableFromBrick(brick: Brick): UserVariable? {
        var clazz: Class<*>? = brick.javaClass
        while (clazz != null && clazz.name.startsWith("org.catrobat.catroid")) {
            for (field in clazz.declaredFields) {
                if (UserVariable::class.java.isAssignableFrom(field.type)) {
                    field.isAccessible = true
                    try { return field.get(brick) as? UserVariable } catch (ignored: Exception) {}
                }
            }
            clazz = clazz.superclass
        }
        return null
    }

    private fun extractUserListFromBrick(brick: Brick): UserList? {
        var clazz: Class<*>? = brick.javaClass
        while (clazz != null && clazz.name.startsWith("org.catrobat.catroid")) {
            for (field in clazz.declaredFields) {
                if (UserList::class.java.isAssignableFrom(field.type)) {
                    field.isAccessible = true
                    try { return field.get(brick) as? UserList } catch (ignored: Exception) {}
                }
            }
            clazz = clazz.superclass
        }
        return null
    }

    private fun setFormulaOnBrick(brick: Brick, field: Brick.BrickField, formula: Formula): Boolean {
        if (brick is FormulaBrick) {
            try {
                brick.setFormulaWithBrickField(field, formula)
                return true
            } catch (e: Exception) {
                Log.w(TAG, "Cannot set formula $field directly on ${brick.javaClass.simpleName}: ${e.message}")
            }
        }

        var clazz: Class<*>? = brick.javaClass
        while (clazz != null && clazz.name.startsWith("org.catrobat.catroid")) {
            for (method in clazz.declaredMethods) {
                if (method.name == "setFormulaWithBrickField" && method.parameterCount == 2) {
                    method.isAccessible = true
                    try {
                        method.invoke(brick, field, formula)
                        return true
                    } catch (ignored: Exception) {}
                }
            }
            clazz = clazz.superclass
        }
        return false
    }

    private fun setLookOnBrick(brick: Brick, look: LookData) {
        var clazz: Class<*>? = brick.javaClass
        while (clazz != null && clazz.name.startsWith("org.catrobat.catroid")) {
            for (field in clazz.declaredFields) {
                if (LookData::class.java.isAssignableFrom(field.type)) {
                    field.isAccessible = true
                    try { field.set(brick, look); return } catch (ignored: Exception) {}
                }
            }
            for (method in clazz.declaredMethods) {
                if (method.parameterCount == 1 && LookData::class.java.isAssignableFrom(method.parameterTypes[0])) {
                    method.isAccessible = true
                    try { method.invoke(brick, look); return } catch (ignored: Exception) {}
                }
            }
            clazz = clazz.superclass
        }
    }

    private fun setSoundOnBrick(brick: Brick, sound: SoundInfo) {
        var clazz: Class<*>? = brick.javaClass
        while (clazz != null && clazz.name.startsWith("org.catrobat.catroid")) {
            for (field in clazz.declaredFields) {
                if (SoundInfo::class.java.isAssignableFrom(field.type)) {
                    field.isAccessible = true
                    try { field.set(brick, sound); return } catch (ignored: Exception) {}
                }
            }
            for (method in clazz.declaredMethods) {
                if (method.parameterCount == 1 && SoundInfo::class.java.isAssignableFrom(method.parameterTypes[0])) {
                    method.isAccessible = true
                    try { method.invoke(brick, sound); return } catch (ignored: Exception) {}
                }
            }
            clazz = clazz.superclass
        }
    }

    fun extractUserVariableName(brick: Brick): String {
        return extractUserVariableFromBrick(brick)?.name ?: ""
    }

    private fun applySmartDefaults(brick: Brick, sprite: Sprite) {
        if (brick is FormulaBrick) {
            val formulas = brick.allFormulaFieldsWithFormulas ?: return
            for ((field, formula) in formulas) {
                if (isFormulaZeroOrEmpty(formula)) {
                    val defaultFormula = getSmartDefaultFormulaForField(field, brick, sprite)
                    if (defaultFormula != null) {
                        setFormulaOnBrick(brick, field, defaultFormula)
                    }
                }
            }
        }

        val currentVarName = extractUserVariableName(brick)
        if (currentVarName.isEmpty()) {
            val brickName = brick.javaClass.simpleName
            if (brickName.contains("Variable", ignoreCase = true)) {
                bindVariableToBrick(brick, "", sprite)
            }
        }

        val currentList = extractUserListFromBrick(brick)
        if (currentList == null || currentList.name.isNullOrEmpty()) {
            val brickName = brick.javaClass.simpleName
            if (brickName.contains("List", ignoreCase = true) && !brickName.contains("ListItem")) {
                bindListToBrick(brick, "", sprite)
            }
        }
    }

    private fun isFormulaZeroOrEmpty(formula: Formula?): Boolean {
        if (formula == null) return true
        val tree = formula.formulaTree ?: return true
        if (tree.elementType == FormulaElement.ElementType.NUMBER && (tree.value == "0" || tree.value == "0.0" || tree.value.isNullOrEmpty())) {
            return true
        }
        return false
    }

    private fun getSmartDefaultFormulaForField(field: Brick.BrickField, brick: Brick, sprite: Sprite): Formula? {
        val project = ProjectManager.getInstance().currentProject

        return when (field) {
            Brick.BrickField.SIZE, Brick.BrickField.TEXTSIZE -> Formula(100)
            Brick.BrickField.TIME_TO_WAIT_IN_SECONDS, Brick.BrickField.DURATION_IN_SECONDS -> Formula(1.0)
            Brick.BrickField.VARIABLE_CHANGE -> Formula(1)
            Brick.BrickField.COLOR, Brick.BrickField.TEXTCOLOR -> Formula("#FFFFFF")
            Brick.BrickField.STRING, Brick.BrickField.TEXT -> {
                val activeVar = project?.userVariables?.firstOrNull()?.name ?: "1"
                Formula(FormulaElement(FormulaElement.ElementType.USER_VARIABLE, activeVar, null))
            }
            Brick.BrickField.X_POSITION, Brick.BrickField.Y_POSITION -> Formula(0)
            Brick.BrickField.STEPS -> Formula(10)
            Brick.BrickField.DEGREES -> Formula(15)
            else -> null
        }
    }

    private fun setPrimitiveFieldOnBrick(brick: Brick, fieldName: String, value: Any, sprite: Sprite) {
        val cleanTarget = fieldName.replace("_", "").lowercase()
        var clazz: Class<*>? = brick.javaClass

        while (clazz != null && clazz.name.startsWith("org.catrobat.catroid")) {
            for (field in clazz.declaredFields) {
                if (field.name.replace("_", "").equals(cleanTarget, ignoreCase = true)) {
                    field.isAccessible = true
                    try {
                        val converted = convertValueToType(value, field.type, sprite)
                        if (converted != null) {
                            field.set(brick, converted)
                            return
                        }
                    } catch (ignored: Exception) {}
                }
            }
            clazz = clazz.superclass
        }
    }

    private fun convertValueToType(value: Any, type: Class<*>, sprite: Sprite): Any? {
        return try {
            val str = value.toString().trim('"', '\'')
            when {
                type == Int::class.javaPrimitiveType || type == Int::class.java -> str.toDouble().toInt()
                type == Double::class.javaPrimitiveType || type == Double::class.java -> str.toDouble()
                type == Float::class.javaPrimitiveType || type == Float::class.java -> str.toFloat()
                type == Boolean::class.javaPrimitiveType || type == Boolean::class.java -> str.toBoolean()
                type == String::class.java -> str
                type.isEnum -> {
                    val constants = type.enumConstants
                    constants?.firstOrNull { it.toString().equals(str, ignoreCase = true) }
                        ?: if (str.toIntOrNull() != null) constants?.getOrNull(str.toInt()) else constants?.firstOrNull()
                }
                type.name.contains("LookData") -> sprite.lookList.find { it.name.equals(str, ignoreCase = true) } ?: sprite.lookList.firstOrNull()
                type.name.contains("SoundInfo") -> sprite.soundList.find { it.name.equals(str, ignoreCase = true) } ?: sprite.soundList.firstOrNull()
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun addNestedBricksToComposite(composite: CompositeBrick, bricks: List<Brick>, secondary: Boolean) {
        for (b in bricks) {
            b.setParent(composite)
        }

        val methodName = if (secondary) "addSecondaryBrick" else "addBrick"
        try {
            val method = composite.javaClass.getMethod(methodName, Brick::class.java)
            for (b in bricks) {
                method.invoke(composite, b)
            }
            return
        } catch (ignored: Exception) {}

        val targetList = if (secondary) composite.secondaryNestedBricks else composite.nestedBricks
        (targetList as? MutableList<Brick>)?.addAll(bricks)
    }

    private fun extractConstantFromFormula(parsed: ParsedFormulaElement): Any? {
        if (parsed.type == FormulaElementType.NUMBER) return parsed.value
        if (parsed.type == FormulaElementType.STRING) return parsed.value
        return null
    }

    private fun getShortParamName(fieldName: String): String {
        val categoryMap = mapOf(
            "VALUE" to "val", "VALUE_1" to "val1", "VALUE_2" to "val2", "VALUE_3" to "val3",
            "TIMES_TO_REPEAT" to "times", "INTERVAL" to "interval", "X_POSITION" to "x", "Y_POSITION" to "y",
            "SIZE" to "size", "COLOR" to "color", "STEPS" to "steps", "DEGREES" to "degrees",
            "IF_CONDITION" to "cond", "REPEAT_UNTIL_CONDITION" to "cond", "VARIABLE_CHANGE" to "val",
            "VARIABLE" to "val", "OPEN_URL" to "url", "OPEN_URL_STRING" to "url", "DURATION" to "duration", "VOLUME" to "volume"
        )
        return categoryMap[fieldName.uppercase()] ?: fieldName.lowercase()
    }
}
