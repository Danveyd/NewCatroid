package org.catrobat.catroid.ai.ui

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import org.catrobat.catroid.R
import org.catrobat.catroid.ai.KoveAutocompleteController
import org.catrobat.catroid.content.Script
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.content.actions.ScriptSequenceAction
import org.catrobat.catroid.content.bricks.Brick
import org.catrobat.catroid.content.bricks.BrickBaseType

class KoveSuggestionBrick(
    val suggestedBricks: List<Brick>,
    private val targetScript: Script
) : BrickBaseType() {

    private var cachedView: View? = null

    init {
        isPhantom = true
    }

    override fun getViewResource(): Int = R.layout.view_kove_glass_card

    @SuppressLint("MissingSuperCall")
    @Suppress("MissingSuperCall")
    override fun getView(context: Context): View {
        cachedView?.let { return it }

        super.getView(context)

        val swipeContainer = KoveSwipeContainer(context)
        val glassCard = KoveGlassCardView(context)

        glassCard.bindBricks(suggestedBricks)

        glassCard.onRegenerateClicked = {
            KoveAutocompleteController.getInstance().regenerateSuggestion()
        }

        swipeContainer.addView(glassCard)

        swipeContainer.swipeListener = object : KoveSwipeContainer.SwipeListener {
            override fun onAccepted() {
                KoveAutocompleteController.getInstance().acceptSuggestions()
            }

            override fun onDismissed() {
                KoveAutocompleteController.getInstance().dismissSuggestions()
            }

            override fun onSwipeProgress(progress: Float) {}
        }

        this.view = swipeContainer
        this.cachedView = swipeContainer
        return swipeContainer
    }

    override fun getPrototypeView(context: Context): View = getView(context)

    override fun addActionToSequence(sprite: Sprite?, sequence: ScriptSequenceAction?) {}

    override fun getScript(): Script = targetScript

    override fun clone(): Brick {
        val cloned = KoveSuggestionBrick(suggestedBricks, targetScript)
        cloned.isPhantom = this.isPhantom
        return cloned
    }
}
