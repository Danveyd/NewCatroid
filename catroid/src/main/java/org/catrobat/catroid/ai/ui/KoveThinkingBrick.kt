package org.catrobat.catroid.ai.ui

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import org.catrobat.catroid.R
import org.catrobat.catroid.content.Script
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.content.actions.ScriptSequenceAction
import org.catrobat.catroid.content.bricks.Brick
import org.catrobat.catroid.content.bricks.BrickBaseType

class KoveThinkingBrick(
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

        val thinkingView = KoveThinkingView(context)
        this.view = thinkingView
        this.cachedView = thinkingView
        return thinkingView
    }

    override fun getPrototypeView(context: Context): View = getView(context)
    override fun addActionToSequence(sprite: Sprite?, sequence: ScriptSequenceAction?) {}
    override fun getScript(): Script = targetScript

    override fun clone(): Brick {
        val cloned = KoveThinkingBrick(targetScript)
        cloned.isPhantom = true
        return cloned
    }
}
