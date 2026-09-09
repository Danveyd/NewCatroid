package org.catrobat.catroid.ai

import android.util.Log
import kotlinx.coroutines.*
import org.catrobat.catroid.ai.ui.KoveSuggestionBrick
import org.catrobat.catroid.ai.ui.KoveThinkingBrick
import org.catrobat.catroid.content.Script
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.content.bricks.Brick
import org.catrobat.catroid.content.bricks.CompositeBrick
import org.catrobat.catroid.content.bricks.IfLogicBeginBrick
import org.catrobat.catroid.content.bricks.TryCatchFinallyBrick
import org.catrobat.catroid.ui.recyclerview.adapter.BrickAdapter
import kotlin.coroutines.coroutineContext

class KoveAutocompleteController private constructor() {

    companion object {
        private const val TAG = "KOVE_CONTROLLER"
        private var instance: KoveAutocompleteController? = null

        @JvmStatic
        fun getInstance(): KoveAutocompleteController {
            if (instance == null) {
                instance = KoveAutocompleteController()
            }
            return instance!!
        }
    }

    private var activeSprite: Sprite? = null
    private var activeScript: Script? = null
    private var adapter: BrickAdapter? = null
    private var selectedIndex: Int = -1

    private var autocompleteJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var currentSuggestionBrick: KoveSuggestionBrick? = null
    private var currentThinkingBrick: KoveThinkingBrick? = null
    private var lastSuggestedDsl: String = ""

    private var targetBrick: Brick? = null
    private var targetBrickList: MutableList<Brick>? = null
    private var alternativeVariantIndex: Int = 0

    fun triggerAutocomplete(
        sprite: Sprite,
        script: Script,
        brickIndex: Int,
        adapter: BrickAdapter,
        delayMs: Long = 100L,
        clickedBrick: Brick? = null
    ) {
        cancelActiveJob()

        dismissSuggestions()

        this.activeSprite = sprite
        this.activeScript = script
        this.adapter = adapter
        this.selectedIndex = brickIndex
        this.targetBrick = clickedBrick
        this.alternativeVariantIndex = 0

        this.targetBrickList = if (clickedBrick != null) {
            findContainingList(script, clickedBrick) ?: script.brickList
        } else {
            script.brickList
        }

        autocompleteJob = scope.launch {
            if (delayMs > 0) delay(delayMs)

            val list = targetBrickList ?: script.brickList
            val thinkingBrick = KoveThinkingBrick(script)
            currentThinkingBrick = thinkingBrick

            val curIndex = if (clickedBrick != null) list.indexOf(clickedBrick) else selectedIndex
            val insertPos = (curIndex + 1).coerceIn(0, list.size)

            list.add(insertPos, thinkingBrick)
            adapter.updateItems(sprite)

            triggerInference(isAlternative = false)
        }
    }

    fun regenerateSuggestion() {
        val sprite = activeSprite ?: return
        val script = activeScript ?: return
        val currentAdapter = adapter ?: return
        val savedTarget = targetBrick
        val savedTargetList = targetBrickList

        cancelActiveJob()
        dismissSuggestions()

        this.targetBrick = savedTarget
        this.targetBrickList = savedTargetList ?: if (savedTarget != null) findContainingList(script, savedTarget) else script.brickList
        alternativeVariantIndex = if (alternativeVariantIndex == 1) 2 else 1

        autocompleteJob = scope.launch {
            val list = targetBrickList ?: script.brickList
            val thinkingBrick = KoveThinkingBrick(script)
            currentThinkingBrick = thinkingBrick

            val curIndex = if (targetBrick != null) list.indexOf(targetBrick) else selectedIndex
            val insertPos = (curIndex + 1).coerceIn(0, list.size)

            list.add(insertPos, thinkingBrick)
            currentAdapter.updateItems(sprite)

            triggerInference(isAlternative = true)
        }
    }

    private fun findContainingList(script: Script, target: Brick): MutableList<Brick>? {
        if (script.brickList.contains(target)) return script.brickList

        fun searchInComposite(composite: CompositeBrick): MutableList<Brick>? {
            val nested = composite.nestedBricks as? MutableList<Brick>
            if (nested != null) {
                if (nested.contains(target)) return nested
                for (b in nested) {
                    if (b is CompositeBrick) {
                        val found = searchInComposite(b)
                        if (found != null) return found
                    }
                }
            }

            if (composite.hasSecondaryList()) {
                val secondary = composite.secondaryNestedBricks as? MutableList<Brick>
                if (secondary != null) {
                    if (secondary.contains(target)) return secondary
                    for (b in secondary) {
                        if (b is CompositeBrick) {
                            val found = searchInComposite(b)
                            if (found != null) return found
                        }
                    }
                }
            }

            if (composite is IfLogicBeginBrick) {
                for (branch in composite.elseIfBranches) {
                    val branchList = branch.branchBricks as? MutableList<Brick>
                    if (branchList != null) {
                        if (branchList.contains(target)) return branchList
                        for (b in branchList) {
                            if (b is CompositeBrick) {
                                val found = searchInComposite(b)
                                if (found != null) return found
                            }
                        }
                    }
                }
            }

            if (composite is TryCatchFinallyBrick) {
                val third = composite.thirdNestedBricks as? MutableList<Brick>
                if (third != null) {
                    if (third.contains(target)) return third
                    for (b in third) {
                        if (b is CompositeBrick) {
                            val found = searchInComposite(b)
                            if (found != null) return found
                        }
                    }
                }
            }

            return null
        }

        for (brick in script.brickList) {
            if (brick is CompositeBrick) {
                val found = searchInComposite(brick)
                if (found != null) return found
            }
        }

        if (target is CompositeBrick) {
            return target.nestedBricks as? MutableList<Brick>
        }

        return null
    }

    private data class ProcessedDslResult(
        val dsl: String,
        val offsetFromCursor: Int
    )

    private suspend fun triggerInference(isAlternative: Boolean) {
        val sprite = activeSprite ?: return
        val script = activeScript ?: return
        val currentAdapter = adapter ?: return

        try {
            var currentVariant = if (isAlternative) alternativeVariantIndex else 0

            var (prefix, suffix) = withContext(Dispatchers.Default) {
                KoveContextGenerator.generateContext(sprite, script, targetBrick, selectedIndex, currentVariant)
            }

            if (!coroutineContext.isActive) return

            val targetTemperature = if (isAlternative) 0.50f else 0.20f
            Log.d(TAG, "Inference started (targetBrick=${targetBrick?.javaClass?.simpleName}, listSize=${targetBrickList?.size}, temp=$targetTemperature)")

            var resultRaw = withContext(Dispatchers.Default) {
                KoveManager.getAutocompleteSuggestion(prefix, suffix, targetTemperature)
            }

            if (!coroutineContext.isActive) return

            var processedResult = processModelOutput(resultRaw, suffix)

            if (isAlternative && processedResult.dsl.isNotEmpty() && processedResult.dsl == lastSuggestedDsl) {
                currentVariant = if (currentVariant == 1) 2 else 1
                val newContext = withContext(Dispatchers.Default) {
                    KoveContextGenerator.generateContext(sprite, script, targetBrick, selectedIndex, currentVariant)
                }
                prefix = newContext.first
                suffix = newContext.second

                resultRaw = withContext(Dispatchers.Default) {
                    KoveManager.getAutocompleteSuggestion(prefix, suffix, 0.55f)
                }
                if (!coroutineContext.isActive) return
                processedResult = processModelOutput(resultRaw, suffix)
            }

            Log.i("KOVE_DEBUG", "==================== KOVE RAW OUTPUT ====================\n$resultRaw")
            Log.i("KOVE_DEBUG", "==================== KOVE CLEANED DSL ====================\n${processedResult.dsl}")
            Log.i("KOVE_DEBUG", "Offset from cursor: +${processedResult.offsetFromCursor} blocks")

            if (processedResult.dsl.isEmpty() || processedResult.dsl.startsWith("ERROR")) {
                Log.w(TAG, "DSL is empty, safely removing thinking indicator.")
                removeThinkingIndicator()
                return
            }

            val newBricks = withContext(Dispatchers.Default) {
                val lexer = KoveLexer(processedResult.dsl)
                val tokens = lexer.tokenize()
                val parser = KoveParser(tokens)
                val parsedBricks = parser.parseBricks()

                KoveModelConverter.buildBricks(parsedBricks, sprite)
            }

            Log.i("KOVE_DEBUG", "Parsed bricks count: ${newBricks.size}")

            if (!coroutineContext.isActive || newBricks.isEmpty()) {
                Log.w(TAG, "No valid bricks built from DSL, removing thinking indicator.")
                removeThinkingIndicator()
                return
            }

            lastSuggestedDsl = processedResult.dsl

            val thinking = currentThinkingBrick
            val list = targetBrickList ?: script.brickList

            if (thinking != null) {
                removeBrickEverywhere(script, thinking)
                currentThinkingBrick = null
            }

            val curIndex = if (targetBrick != null) list.indexOf(targetBrick) else selectedIndex
            val targetPos = (curIndex + 1 + processedResult.offsetFromCursor).coerceIn(0, list.size)

            val suggestionBrick = KoveSuggestionBrick(newBricks, script)
            currentSuggestionBrick = suggestionBrick
            list.add(targetPos, suggestionBrick)

            currentAdapter.updateItems(sprite)
            Log.i(TAG, "Suggestion displayed inside target list at index $targetPos (count: ${newBricks.size})")

        } catch (e: Exception) {
            Log.e(TAG, "Error in autocomplete pipeline", e)
            removeThinkingIndicator()
        }
    }

    private fun processModelOutput(raw: String, suffix: String): ProcessedDslResult {
        var clean = raw

        val stopMarkers = listOf(
            "<|fim_prefix|>", "<|fim_suffix|>", "<|fim_middle|>", "<|endoftext|>",
            "<|file_sep|>", "<|im_start|>", "<|im_end|>", "@Sprite",
            "@StartScript", "@WhenScript", "@WhenConditionScript", "@BroadcastScript",
            "class ", "# end of"
        )

        for (marker in stopMarkers) {
            if (clean.contains(marker)) {
                clean = clean.substringBefore(marker)
            }
        }

        val rawLines = clean.lines()
        val validLines = mutableListOf<String>()
        var lastAddedLine = ""

        for (line in rawLines) {
            var trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            if (trimmed.startsWith("#") || trimmed == "pass") continue
            if (trimmed.startsWith("def ") || trimmed.startsWith("@") || trimmed.startsWith("class ")) continue
            if (trimmed.contains("var(var(")) continue

            val quoteCount = trimmed.count { it == '"' }
            if (quoteCount % 2 != 0) continue

            if (trimmed == lastAddedLine) continue
            lastAddedLine = trimmed

            trimmed = trimmed.replace(Regex("set_var\\(var\\(\"([^\"]+)\"\\s*,"), "set_var(var=\"$1\",")
            trimmed = trimmed.replace(Regex("change_var\\(var\\(\"([^\"]+)\"\\s*,"), "change_var(var=\"$1\",")

            val anyOp = "(\\+|\\-|\\*\\*|\\*|\\/|%|==|!=|>=|<=|>|<|and|or)"
            trimmed = trimmed.replace(Regex("\\)\\s*\\)\\s*$anyOp"), ") $1")
            trimmed = trimmed.replace(Regex("var\\(\"([^\"]+)\"\\)\\)+\\s*$anyOp"), "var(\"$1\") $2")
            trimmed = trimmed.replace(Regex("sensor\\(\"([^\"]+)\"\\)\\)+\\s*$anyOp"), "sensor(\"$1\") $2")
            trimmed = trimmed.replace(Regex("list\\(\"([^\"]+)\"\\)\\)+\\s*$anyOp"), "list(\"$1\") $2")

            trimmed = trimmed.replace(Regex("\\)\"\\)"), "))")
            trimmed = trimmed.replace(Regex("\\)\"\\s*$"), ")")
            trimmed = trimmed.replace(Regex("\"\\s*\\)+\\s*\"\\s*\\)"), "\"))")

            trimmed = trimmed.replace(Regex("\\),(\\s*[a-zA-Z0-9_]+\\s*=)"), ",$1")
            trimmed = trimmed.replace(Regex("\\)+\\s*:"), "):")
            trimmed = trimmed.replace(Regex("\\)+\\s*=="), ") ==")
            trimmed = trimmed.replace(Regex("\\)+\\s*$"), ")")
            trimmed = trimmed.replace(Regex("var\\(\"([^\"]+)\",\\s*val=([^\\)]+)\\)\\)"), "$2")

            val openCount = trimmed.count { it == '(' }
            var closeCount = trimmed.count { it == ')' }

            while (closeCount > openCount && trimmed.endsWith(")")) {
                trimmed = trimmed.substring(0, trimmed.length - 1).trimEnd()
                closeCount--
            }

            if (openCount > closeCount) {
                trimmed += ")".repeat(openCount - closeCount)
            }

            val leadingSpaces = line.takeWhile { it.isWhitespace() }
            validLines.add(leadingSpaces + trimmed)
        }

        if (validLines.isEmpty()) {
            return ProcessedDslResult("", 0)
        }

        val minIndent = validLines.filter { it.trim().isNotEmpty() }
            .minOfOrNull { it.indexOfFirst { char -> !char.isWhitespace() }.let { idx -> if (idx == -1) 0 else idx } } ?: 0

        val normalizedLines = validLines.map { line ->
            if (line.length >= minIndent) line.substring(minIndent) else line.trimStart()
        }

        val suffixLines = suffix.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith("@") && it != "pass" && !it.startsWith("class ") && !it.startsWith("def ") }

        var leadingSuffixMatches = 0
        while (leadingSuffixMatches < suffixLines.size &&
            leadingSuffixMatches < normalizedLines.size &&
            isDslLineMatching(normalizedLines[leadingSuffixMatches], suffixLines[leadingSuffixMatches])) {
            leadingSuffixMatches++
        }

        var linesToKeep = if (leadingSuffixMatches > 0) {
            normalizedLines.drop(leadingSuffixMatches)
        } else {
            normalizedLines
        }

        if (suffixLines.isNotEmpty() && linesToKeep.isNotEmpty()) {
            val firstTargetSuffix = suffixLines[0]
            val trailingSuffixIdx = linesToKeep.indexOfFirst { isDslLineMatching(it, firstTargetSuffix) }
            if (trailingSuffixIdx != -1) {
                linesToKeep = linesToKeep.take(trailingSuffixIdx)
            }
        }

        if (linesToKeep.isEmpty()) {
            return ProcessedDslResult("", 0)
        }

        val finalDsl = linesToKeep.joinToString("\n")
        return ProcessedDslResult(finalDsl, leadingSuffixMatches)
    }

    private fun isDslLineMatching(lineA: String, lineB: String): Boolean {
        val a = lineA.trim().replace(" ", "").lowercase()
        val b = lineB.trim().replace(" ", "").lowercase()
        if (a == b) return true

        val funcA = a.substringBefore("(")
        val funcB = b.substringBefore("(")
        if (funcA.isNotEmpty() && funcA == funcB) {
            if (a.contains("()") && b.contains("()")) return true
        }
        return false
    }

    private fun removeThinkingIndicator() {
        val script = activeScript ?: return
        val sprite = activeSprite ?: return
        val thinking = currentThinkingBrick ?: return

        currentThinkingBrick = null
        removeBrickEverywhere(script, thinking)
        adapter?.updateItems(sprite)
    }

    private fun removeBrickEverywhere(script: Script, brick: Brick) {
        targetBrickList?.remove(brick)
        script.brickList.remove(brick)
        for (b in script.brickList) {
            if (b is CompositeBrick) {
                b.removeChild(brick)
            }
        }
    }

    fun acceptSuggestions() {
        val sprite = activeSprite ?: return
        val script = activeScript ?: return
        val suggestion = currentSuggestionBrick ?: return
        val list = targetBrickList ?: script.brickList
        val project = org.catrobat.catroid.ProjectManager.getInstance().currentProject

        val insertPos = list.indexOf(suggestion)
        list.remove(suggestion)

        val acceptedList = ArrayList(suggestion.suggestedBricks)
        if (insertPos != -1) {
            list.addAll(insertPos, acceptedList)
        }

        val containerParent = targetBrick?.parent ?: script.scriptBrick
        for (b in acceptedList) {
            b.parent = containerParent
        }

        if (project != null) {
            KoveModelConverter.registerNewEntities(acceptedList, sprite, project)
        }

        currentSuggestionBrick = null
        targetBrick = null
        targetBrickList = null
        adapter?.updateItems(sprite)
        adapter?.highlightMaterializedBricks(acceptedList)
        cancelActiveJob()
    }

    fun dismissSuggestions() {
        val sprite = activeSprite ?: return
        val script = activeScript ?: return

        var changed = false
        currentThinkingBrick?.let {
            removeBrickEverywhere(script, it)
            currentThinkingBrick = null
            changed = true
        }
        currentSuggestionBrick?.let {
            removeBrickEverywhere(script, it)
            currentSuggestionBrick = null
            changed = true
        }

        targetBrick = null
        targetBrickList = null

        if (changed) {
            adapter?.updateItems(sprite)
        }
    }

    fun cancelActiveJob() {
        autocompleteJob?.cancel()
        autocompleteJob = null
    }
}
