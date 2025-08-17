package org.catrobat.catroid.utils.lunoscript

// Можно создать этот класс рядом с другими вашими Actions
import com.badlogic.gdx.scenes.scene2d.Action

// Специальный Action, который наш LunoScript-движок будет понимать
class LunoDelayAction(val durationInSeconds: Float) : Action() {
    private var passedTime = 0f

    override fun act(delta: Float): Boolean {
        passedTime += delta
        // Action завершится (и выполнение пойдет дальше), когда пройдет нужное время
        return passedTime >= durationInSeconds
    }
}