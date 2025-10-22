package org.catrobat.catroid.content.actions
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
class UnlockMouseAction : TemporalAction() {
    override fun update(percent: Float) {
        Gdx.input.isCursorCatched = false
    }
}