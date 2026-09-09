package org.catrobat.catroid.ai

import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.common.LookData
import org.catrobat.catroid.common.SoundInfo
import org.catrobat.catroid.content.Project
import org.catrobat.catroid.content.Scene
import org.catrobat.catroid.content.Script
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.content.bricks.Brick
import org.catrobat.catroid.content.bricks.CompositeBrick
import org.catrobat.catroid.content.bricks.FormulaBrick
import org.catrobat.catroid.content.bricks.IfLogicBeginBrick
import org.catrobat.catroid.content.bricks.TryCatchFinallyBrick
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.FormulaElement
import org.catrobat.catroid.formulaeditor.UserVariable
import java.lang.reflect.Modifier

object KoveContextGenerator {

    private class TraversalState(
        val targetBrick: Brick?,
        var isPastCursor: Boolean = false
    )

    fun generateContext(
        activeSprite: Sprite,
        activeScript: Script,
        targetBrick: Brick?,
        cursorBrickIndex: Int,
        variantIndex: Int = 0
    ): Pair<String, String> {
        val prefixBuilder = StringBuilder()
        val suffixBuilder = StringBuilder()

        val project: Project? = ProjectManager.getInstance().currentProject
        val currentScene: Scene? = ProjectManager.getInstance().currentlyEditedScene

        val projectName = project?.name ?: "Unnamed"
        val globalVars = project?.userVariables?.map { it.name } ?: emptyList()
        val globalLists = project?.userLists?.map { it.name } ?: emptyList()
        val sceneName = currentScene?.name ?: "DefaultScene"

        prefixBuilder.append("# Project: $projectName\n")
        prefixBuilder.append("# Global Variables: $globalVars\n")
        prefixBuilder.append("# Global Lists: $globalLists\n")

        prefixBuilder.append("# " + "-".repeat(50) + "\n")
        prefixBuilder.append("# Scene: $sceneName\n")
        prefixBuilder.append("# " + "-".repeat(50) + "\n\n")

        val spriteList = currentScene?.spriteList ?: listOf(activeSprite)
        val state = TraversalState(targetBrick)

        for (sprite in spriteList) {
            val spriteHeader = buildSpriteHeader(sprite)

            if (sprite != activeSprite) {
                val targetBuilder = if (!state.isPastCursor) prefixBuilder else suffixBuilder
                targetBuilder.append(spriteHeader)
                targetBuilder.append("    pass\n\n")
            } else {
                prefixBuilder.append(spriteHeader)
                for (script in sprite.scriptList) {
                    val scriptHeader = buildScriptHeader(script)

                    if (script == activeScript) {
                        prefixBuilder.append(scriptHeader)

                        val realBricks = script.brickList.filter {
                            !it.isPhantom && !it.javaClass.simpleName.startsWith("Kove")
                        }

                        serializeBricksHierarchy(
                            realBricks,
                            prefixBuilder,
                            suffixBuilder,
                            state,
                            indent = 8,
                            variantIndex = variantIndex
                        )

                        state.isPastCursor = true
                    } else {
                        val targetBuilder = if (!state.isPastCursor) prefixBuilder else suffixBuilder
                        targetBuilder.append(scriptHeader)
                        targetBuilder.append("        pass\n\n")
                    }
                }
            }
        }

        val prefix = prefixBuilder.toString()
        var suffix = suffixBuilder.toString()
        if (suffix.isEmpty()) suffix = "\n"

        return Pair(prefix, suffix)
    }

    private fun serializeBricksHierarchy(
        bricks: List<Brick>,
        prefixBuilder: StringBuilder,
        suffixBuilder: StringBuilder,
        state: TraversalState,
        indent: Int,
        variantIndex: Int
    ) {
        val spaces = " ".repeat(indent)

        for (brick in bricks) {
            if (brick.isCommentedOut || brick.isPhantom) continue
            if (brick.javaClass.simpleName.startsWith("Kove")) continue

            val brickType = brick.javaClass.simpleName
            val cleanName = cleanBrickName(brickType)

            fun currentBuilder() = if (!state.isPastCursor) prefixBuilder else suffixBuilder

            if (brickType.startsWith("IfLogicBegin") || brickType.startsWith("IfThenLogicBegin")) {
                val condFormula = getFormulaFromBrick(brick, Brick.BrickField.IF_CONDITION)
                val condStr = if (condFormula != null) serializeFormula(condFormula.formulaTree) else "True"

                currentBuilder().append(spaces).append("if $condStr:\n")
                if (brick == state.targetBrick) state.isPastCursor = true

                if (brick is CompositeBrick) {
                    serializeBricksHierarchy(brick.nestedBricks, prefixBuilder, suffixBuilder, state, indent + 4, variantIndex)

                    if (brick is IfLogicBeginBrick) {
                        for (branch in brick.elseIfBranches) {
                            val elifCond = serializeFormula(branch.condition.formulaTree)
                            currentBuilder().append(spaces).append("elif $elifCond:\n")
                            serializeBricksHierarchy(branch.branchBricks, prefixBuilder, suffixBuilder, state, indent + 4, variantIndex)
                        }

                        if (brick.hasSecondaryList() && brick.secondaryNestedBricks != null && brick.secondaryNestedBricks.isNotEmpty()) {
                            currentBuilder().append(spaces).append("else:\n")
                            serializeBricksHierarchy(brick.secondaryNestedBricks, prefixBuilder, suffixBuilder, state, indent + 4, variantIndex)
                        }
                    }
                }
                continue
            }

            if (brick is TryCatchFinallyBrick) {
                currentBuilder().append(spaces).append("try:\n")
                if (brick == state.targetBrick) state.isPastCursor = true
                serializeBricksHierarchy(brick.nestedBricks, prefixBuilder, suffixBuilder, state, indent + 4, variantIndex)

                val catchPart = brick.allParts.firstOrNull { it is TryCatchFinallyBrick.CatchBrick } as? TryCatchFinallyBrick.CatchBrick
                val errVar = catchPart?.userVariable?.name ?: "err"
                currentBuilder().append(spaces).append("except(var=\"$errVar\"):\n")
                serializeBricksHierarchy(brick.secondaryNestedBricks, prefixBuilder, suffixBuilder, state, indent + 4, variantIndex)

                if (brick.thirdNestedBricks != null && brick.thirdNestedBricks.isNotEmpty()) {
                    currentBuilder().append(spaces).append("finally:\n")
                    serializeBricksHierarchy(brick.thirdNestedBricks, prefixBuilder, suffixBuilder, state, indent + 4, variantIndex)
                }
                continue
            }

            if (brick is CompositeBrick) {
                val loopHeader = when (brickType) {
                    "ForeverBrick" -> "forever():"
                    "RepeatBrick" -> {
                        val times = getFormulaFromBrick(brick, Brick.BrickField.TIMES_TO_REPEAT)?.let { serializeFormula(it.formulaTree) } ?: "1"
                        "repeat($times):"
                    }
                    "RepeatUntilBrick" -> {
                        val cond = getFormulaFromBrick(brick, Brick.BrickField.REPEAT_UNTIL_CONDITION)?.let { serializeFormula(it.formulaTree) } ?: "False"
                        "repeat_until($cond):"
                    }
                    "AsyncRepeatBrick" -> {
                        val times = getFormulaFromBrick(brick, Brick.BrickField.TIMES_TO_REPEAT)?.let { serializeFormula(it.formulaTree) } ?: "1"
                        "async_repeat(times=$times):"
                    }
                    "IntervalRepeatBrick" -> {
                        val times = getFormulaFromBrick(brick, Brick.BrickField.TIMES_TO_REPEAT)?.let { serializeFormula(it.formulaTree) } ?: "1"
                        val interval = getFormulaFromBrick(brick, Brick.BrickField.INTERVAL)?.let { serializeFormula(it.formulaTree) } ?: "0.1"
                        "interval_repeat(times=$times, interval=$interval):"
                    }
                    "ForVariableFromToBrick" -> {
                        val varName = extractUserVariableName(brick)
                        val from = getFormulaFromBrick(brick, Brick.BrickField.FOR_LOOP_FROM)?.let { serializeFormula(it.formulaTree) } ?: "1"
                        val to = getFormulaFromBrick(brick, Brick.BrickField.FOR_LOOP_TO)?.let { serializeFormula(it.formulaTree) } ?: "10"
                        "for_variable(var=\"$varName\", from=$from, to=$to):"
                    }
                    "InstantBrick" -> "instant():"
                    "SpawnThreadBrick" -> {
                        val id = getFormulaFromBrick(brick, Brick.BrickField.IF_CONDITION)?.let { serializeFormula(it.formulaTree) } ?: "1"
                        "spawn_thread(id=$id):"
                    }
                    "RunAsSpriteBrick" -> {
                        val name = getFormulaFromBrick(brick, Brick.BrickField.NAME)?.let { serializeFormula(it.formulaTree) } ?: "\"Sprite\""
                        "run_as_sprite(name=$name):"
                    }
                    else -> "$cleanName():"
                }

                currentBuilder().append(spaces).append("$loopHeader\n")
                if (brick == state.targetBrick) state.isPastCursor = true
                serializeBricksHierarchy(brick.nestedBricks, prefixBuilder, suffixBuilder, state, indent + 4, variantIndex)
                continue
            }

            if (cleanName == "set_var" || cleanName == "change_var") {
                val varName = extractUserVariableName(brick)
                val valFormula = getAllFormulasFromBrick(brick).values.firstOrNull()?.let { serializeFormula(it.formulaTree) } ?: "0"
                if (varName.isNotEmpty()) {
                    currentBuilder().append(spaces).append("$cleanName(var=\"$varName\", val=$valFormula)\n")
                } else {
                    currentBuilder().append(spaces).append("$cleanName($valFormula)\n")
                }
                if (brick == state.targetBrick) state.isPastCursor = true
                continue
            }

            val formulas = getAllFormulasFromBrick(brick)
            val primitives = getPrimitiveFields(brick)
            val totalArgsCount = formulas.size + primitives.size

            if (totalArgsCount == 1) {
                val valOnly = if (formulas.isNotEmpty()) {
                    serializeFormula(formulas.values.first().formulaTree)
                } else {
                    val value = primitives.values.first()
                    if (value is String) "\"$value\"" else value.toString()
                }
                currentBuilder().append(spaces).append("$cleanName($valOnly)\n")
            } else {
                val args = mutableListOf<String>()
                formulas.forEach { (field, formula) ->
                    val paramName = getShortParamName(field.name)
                    args.add("$paramName=${serializeFormula(formula.formulaTree)}")
                }
                primitives.forEach { (name, value) ->
                    val paramName = toSnakeCase(name)
                    val valStr = when (value) {
                        is String -> "\"$value\""
                        is Boolean -> value.toString().replaceFirstChar { it.uppercase() }
                        else -> value.toString()
                    }
                    args.add("$paramName=$valStr")
                }
                currentBuilder().append(spaces).append("$cleanName(${args.joinToString(", ")})\n")
            }

            if (brick == state.targetBrick) {
                state.isPastCursor = true
            }
        }
    }

    private fun buildSpriteHeader(sprite: Sprite): String {
        val looksList = sprite.lookList.map { "'${it.name}'" }
        val soundsList = sprite.soundList.map { "'${it.name}'" }
        val classId = safeIdentifier(sprite.name)

        return "@Sprite(\"${sprite.name}\")\nclass $classId:\n    looks = $looksList\n    sounds = $soundsList\n\n"
    }

    private fun buildScriptHeader(script: Script): String {
        val scriptType = script.javaClass.simpleName
        val decoratorArg = getScriptDecoratorArg(script)
        val funcName = toSnakeCase(scriptType.replace("Script", ""))

        val decorator = if (decoratorArg.isNotEmpty()) "    @$scriptType($decoratorArg)\n" else "    @$scriptType\n"
        return "$decorator    def $funcName():\n"
    }

    private fun getScriptDecoratorArg(script: Script): String {
        return try {
            when (script.javaClass.simpleName) {
                "BroadcastScript" -> {
                    val field = script.javaClass.getDeclaredField("receivedMessage")
                    field.isAccessible = true
                    val msg = field.get(script) as? String
                    if (!msg.isNullOrEmpty()) "\"$msg\"" else ""
                }
                "WhenConditionScript" -> {
                    val field = script.javaClass.getDeclaredField("formula")
                    field.isAccessible = true
                    val formula = field.get(script) as? Formula
                    if (formula != null) serializeFormula(formula.formulaTree) else ""
                }
                else -> ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun serializeBricks(bricks: List<Brick>, builder: StringBuilder, indent: Int) {
        val spaces = " ".repeat(indent)
        for (brick in bricks) {
            if (brick.isCommentedOut || brick.isPhantom) continue
            if (brick.javaClass.simpleName.startsWith("Kove")) continue
            val brickType = brick.javaClass.simpleName
            val cleanName = cleanBrickName(brickType)

            if (brickType.startsWith("IfLogicBegin") || brickType.startsWith("IfThenLogicBegin")) {
                val condFormula = getFormulaFromBrick(brick, Brick.BrickField.IF_CONDITION)
                val condStr = if (condFormula != null) serializeFormula(condFormula.formulaTree) else "True"
                builder.append(spaces).append("if $condStr:\n")

                if (brick is CompositeBrick) {
                    serializeBricks(brick.nestedBricks, builder, indent + 4)
                    if (brick.hasSecondaryList() && brick.secondaryNestedBricks != null) {
                        builder.append(spaces).append("else:\n")
                        serializeBricks(brick.secondaryNestedBricks, builder, indent + 4)
                    }
                }
                continue
            }

            if (brick is CompositeBrick || brickType in listOf("AsyncRepeatBrick", "IntervalRepeatBrick", "RunAsSpriteBrick")) {
                if (brickType.startsWith("IfLogicBegin") || brickType.startsWith("IfThenLogicBegin")) {
                    val condFormula = getFormulaFromBrick(brick, Brick.BrickField.IF_CONDITION)
                    val condStr = if (condFormula != null) serializeFormula(condFormula.formulaTree) else "True"
                    builder.append(spaces).append("if $condStr:\n")

                    if (brick is CompositeBrick) {
                        serializeBricks(brick.nestedBricks, builder, indent + 4)

                        if (brick is IfLogicBeginBrick) {
                            for (branch in brick.elseIfBranches) {
                                val elifCond = serializeFormula(branch.condition.formulaTree)
                                builder.append(spaces).append("elif $elifCond:\n")
                                serializeBricks(branch.branchBricks, builder, indent + 4)
                            }

                            if (brick.hasSecondaryList() && brick.secondaryNestedBricks != null && brick.secondaryNestedBricks.isNotEmpty()) {
                                builder.append(spaces).append("else:\n")
                                serializeBricks(brick.secondaryNestedBricks, builder, indent + 4)
                            }
                        }
                    }
                    continue
                }

                if (brick is TryCatchFinallyBrick) {
                    builder.append(spaces).append("try:\n")
                    serializeBricks(brick.nestedBricks, builder, indent + 4)

                    val catchPart = brick.allParts.firstOrNull { it is TryCatchFinallyBrick.CatchBrick } as? TryCatchFinallyBrick.CatchBrick
                    val errVar = catchPart?.userVariable?.name ?: "err"
                    builder.append(spaces).append("except(var=\"$errVar\"):\n")
                    serializeBricks(brick.secondaryNestedBricks, builder, indent + 4)

                    if (brick.thirdNestedBricks != null && brick.thirdNestedBricks.isNotEmpty()) {
                        builder.append(spaces).append("finally:\n")
                        serializeBricks(brick.thirdNestedBricks, builder, indent + 4)
                    }
                    continue
                }

                if (brick is CompositeBrick) {
                    val loopHeader = when (brickType) {
                        "ForeverBrick" -> "forever():"
                        "RepeatBrick" -> {
                            val times = getFormulaFromBrick(brick, Brick.BrickField.TIMES_TO_REPEAT)?.let { serializeFormula(it.formulaTree) } ?: "1"
                            "repeat($times):"
                        }
                        "RepeatUntilBrick" -> {
                            val cond = getFormulaFromBrick(brick, Brick.BrickField.REPEAT_UNTIL_CONDITION)?.let { serializeFormula(it.formulaTree) } ?: "False"
                            "repeat_until($cond):"
                        }
                        "AsyncRepeatBrick" -> {
                            val times = getFormulaFromBrick(brick, Brick.BrickField.TIMES_TO_REPEAT)?.let { serializeFormula(it.formulaTree) } ?: "1"
                            "async_repeat(times=$times):"
                        }
                        "IntervalRepeatBrick" -> {
                            val times = getFormulaFromBrick(brick, Brick.BrickField.TIMES_TO_REPEAT)?.let { serializeFormula(it.formulaTree) } ?: "1"
                            val interval = getFormulaFromBrick(brick, Brick.BrickField.INTERVAL)?.let { serializeFormula(it.formulaTree) } ?: "0.1"
                            "interval_repeat(times=$times, interval=$interval):"
                        }
                        "ForVariableFromToBrick" -> {
                            val varName = extractUserVariableName(brick)
                            val from = getFormulaFromBrick(brick, Brick.BrickField.FOR_LOOP_FROM)?.let { serializeFormula(it.formulaTree) } ?: "1"
                            val to = getFormulaFromBrick(brick, Brick.BrickField.FOR_LOOP_TO)?.let { serializeFormula(it.formulaTree) } ?: "10"
                            "for_variable(var=\"$varName\", from=$from, to=$to):"
                        }
                        "InstantBrick" -> "instant():"
                        "SpawnThreadBrick" -> {
                            val id = getFormulaFromBrick(brick, Brick.BrickField.IF_CONDITION)?.let { serializeFormula(it.formulaTree) } ?: "1"
                            "spawn_thread(id=$id):"
                        }
                        "RunAsSpriteBrick" -> {
                            val name = getFormulaFromBrick(brick, Brick.BrickField.NAME)?.let { serializeFormula(it.formulaTree) } ?: "\"Sprite\""
                            "run_as_sprite(name=$name):"
                        }
                        else -> "$cleanName():"
                    }
                    builder.append(spaces).append("$loopHeader\n")
                    serializeBricks(brick.nestedBricks, builder, indent + 4)
                    continue
                }
            }

            if (cleanName == "set_var" || cleanName == "change_var") {
                val varName = extractUserVariableName(brick)
                val valFormula = getAllFormulasFromBrick(brick).values.firstOrNull()?.let { serializeFormula(it.formulaTree) } ?: "0"
                if (varName.isNotEmpty()) {
                    builder.append(spaces).append("$cleanName(var=\"$varName\", val=$valFormula)\n")
                } else {
                    builder.append(spaces).append("$cleanName($valFormula)\n")
                }
                continue
            }

            val formulas = getAllFormulasFromBrick(brick)
            val primitives = getPrimitiveFields(brick)
            val totalArgsCount = formulas.size + primitives.size

            if (totalArgsCount == 1) {
                val valOnly = if (formulas.isNotEmpty()) {
                    serializeFormula(formulas.values.first().formulaTree)
                } else {
                    val value = primitives.values.first()
                    if (value is String) "\"$value\"" else value.toString()
                }
                builder.append(spaces).append("$cleanName($valOnly)\n")
            } else {
                val args = mutableListOf<String>()
                formulas.forEach { (field, formula) ->
                    val paramName = getShortParamName(field.name)
                    args.add("$paramName=${serializeFormula(formula.formulaTree)}")
                }
                primitives.forEach { (name, value) ->
                    val paramName = toSnakeCase(name)
                    val valStr = when (value) {
                        is String -> "\"$value\""
                        is Boolean -> value.toString().replaceFirstChar { it.uppercase() }
                        else -> value.toString()
                    }
                    args.add("$paramName=$valStr")
                }
                builder.append(spaces).append("$cleanName(${args.joinToString(", ")})\n")
            }
        }
    }

    private fun extractUserVariableName(brick: Brick): String {
        var clazz: Class<*>? = brick.javaClass
        while (clazz != null && clazz.name.startsWith("org.catrobat.catroid")) {
            for (field in clazz.declaredFields) {
                if (UserVariable::class.java.isAssignableFrom(field.type)) {
                    field.isAccessible = true
                    val uVar = field.get(brick) as? UserVariable
                    if (uVar != null && !uVar.name.isNullOrEmpty()) {
                        return uVar.name
                    }
                }
            }
            clazz = clazz.superclass
        }
        return ""
    }

    fun serializeFormula(element: FormulaElement?): String {
        if (element == null) return ""
        return when (element.elementType) {
            FormulaElement.ElementType.NUMBER -> element.value ?: "0"
            FormulaElement.ElementType.STRING -> {
                val escaped = (element.value ?: "").replace("\"", "\\\"")
                "\"$escaped\""
            }
            FormulaElement.ElementType.USER_VARIABLE -> "var(\"${element.value}\")"
            FormulaElement.ElementType.USER_LIST -> "list(\"${element.value}\")"
            FormulaElement.ElementType.SENSOR -> "sensor(\"${element.value}\")"
            FormulaElement.ElementType.COLLISION_FORMULA -> "touches(\"${element.value}\")"
            FormulaElement.ElementType.USER_DEFINED_BRICK_INPUT -> "input(\"${element.value}\")"
            FormulaElement.ElementType.OPERATOR -> {
                val op = mapOperatorToSymbol(element.value)
                val left = serializeFormula(element.leftChild)
                val right = serializeFormula(element.rightChild)

                if (left.isNotEmpty() && right.isNotEmpty()) {
                    "($left $op $right)"
                } else if (left.isNotEmpty()) {
                    left
                } else if (right.isNotEmpty()) {
                    "($op$right)"
                } else {
                    "0"
                }
            }
            FormulaElement.ElementType.FUNCTION -> {
                val funcName = element.value.lowercase()
                val args = mutableListOf<String>()
                if (element.leftChild != null) args.add(serializeFormula(element.leftChild))
                if (element.rightChild != null) args.add(serializeFormula(element.rightChild))
                element.additionalChildren?.forEach { args.add(serializeFormula(it)) }
                "$funcName(${args.joinToString(", ")})"
            }
            else -> element.value ?: ""
        }
    }

    private fun getFormulaFromBrick(brick: Brick, field: Brick.BrickField): Formula? {
        if (brick is FormulaBrick) {
            return brick.allFormulaFieldsWithFormulas?.get(field)
        }
        return null
    }

    private fun getAllFormulasFromBrick(brick: Brick): Map<Brick.BrickField, Formula> {
        val result = mutableMapOf<Brick.BrickField, Formula>()
        if (brick is FormulaBrick) {
            val formulas = brick.allFormulaFieldsWithFormulas
            if (formulas != null) {
                result.putAll(formulas)
            }
        }
        return result
    }

    private fun getPrimitiveFields(brick: Brick): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        var clazz: Class<*>? = brick.javaClass
        val ignoredFields = setOf(
            "serialVersionUID", "view", "checkbox", "spinner", "parent", "endBrick", "loopBricks",
            "tryBricks", "catchBricks", "finallyBricks", "posX", "posY", "scriptId", "scriptBrick", "commentedOut"
        )
        while (clazz != null && clazz.name.startsWith("org.catrobat.catroid")) {
            for (field in clazz.declaredFields) {
                if (Modifier.isTransient(field.modifiers)) continue
                if (ignoredFields.contains(field.name)) continue

                val type = field.type
                if (type.isPrimitive || type == String::class.java || type.isEnum) {
                    field.isAccessible = true
                    val value = field.get(brick)
                    if (value != null) result[field.name] = value
                } else if (LookData::class.java.isAssignableFrom(type) || SoundInfo::class.java.isAssignableFrom(type)) {
                    field.isAccessible = true
                    val value = field.get(brick)
                    if (value != null) {
                        try {
                            val nameMethod = value.javaClass.getMethod("getName")
                            val name = nameMethod.invoke(value) as String
                            result[field.name] = name
                        } catch (e: Exception) {}
                    }
                }
            }
            clazz = clazz.superclass
        }
        return result
    }

    private fun safeIdentifier(name: String): String {
        var subbed = name.replace(Regex("[^a-zA-Zа-яА-Я0-9_]"), "_")
        if (subbed.isEmpty()) return "empty_id"
        if (subbed[0].isDigit()) subbed = "_$subbed"
        return subbed
    }

    private fun mapOperatorToSymbol(op: String): String {
        val map = mapOf(
            "PLUS" to "+", "MINUS" to "-", "MULT" to "*", "DIVIDE" to "/",
            "POW" to "**", "MOD" to "%", "EQUAL" to "==", "NOT_EQUAL" to "!=",
            "GREATER_THAN" to ">", "GREATER_OR_EQUAL" to ">=", "SMALLER_THAN" to "<",
            "SMALLER_OR_EQUAL" to "<=", "LOGICAL_AND" to "and", "LOGICAL_OR" to "or", "LOGICAL_NOT" to "not"
        )
        return map[op] ?: op
    }

    private fun cleanBrickName(className: String): String {
        var name = className
        if (name.endsWith("Brick")) name = name.substring(0, name.length - 5)
        name = toSnakeCase(name)
        val renameMap = mapOf(
            "set_size_to" to "set_size", "set_variable" to "set_var",
            "change_variable" to "change_var", "place_at" to "place_at",
            "play_sound_and_wait" to "play_sound_wait", "scene_start" to "start_scene"
        )
        return renameMap[name] ?: name
    }

    private fun getShortParamName(fieldName: String): String {
        val categoryMap = mapOf(
            "VALUE" to "val", "VALUE_1" to "val1", "VALUE_2" to "val2", "VALUE_3" to "val3",
            "TIMES_TO_REPEAT" to "times", "INTERVAL" to "interval", "X_POSITION" to "x", "Y_POSITION" to "y",
            "SIZE" to "size", "COLOR" to "color", "STEPS" to "steps", "DEGREES" to "degrees",
            "IF_CONDITION" to "cond", "REPEAT_UNTIL_CONDITION" to "cond", "VARIABLE_CHANGE" to "val",
            "VARIABLE" to "val", "OPEN_URL" to "url", "OPEN_URL_STRING" to "url", "DURATION" to "duration", "VOLUME" to "volume"
        )
        return categoryMap[fieldName.uppercase()] ?: toSnakeCase(fieldName)
    }

    private fun toSnakeCase(s: String): String {
        return s.replace(Regex("([a-z0-9])([A-Z])"), "$1_$2").lowercase()
    }
}
