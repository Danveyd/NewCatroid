package org.catrobat.catroid.content

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Stage
import java.util.concurrent.ConcurrentHashMap

class SquareController private constructor() {
    private val squares: ConcurrentHashMap<String, SquareActor> = ConcurrentHashMap()

    companion object {
        val instance: SquareController by lazy { SquareController() }
    }

    fun createOrUpdateSquare(
        name: String,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        colorHex: String,
        rotation: Float = 0f,
        transparency: Float = 1f,
        cornerRadius: Float = 0f,
        stage: Stage
    ) {
        squares[name]?.remove()
        squares.remove(name)

        val squareActor = SquareActor(x, y, width, height, colorHex, rotation, transparency, cornerRadius)
        squares[name] = squareActor
        stage.addActor(squareActor)
    }

    fun removeSquare(name: String) {
        squares[name]?.let { square ->
            square.remove()
            squares.remove(name)
        }
    }

    fun clearSquares() {
        squares.values.forEach { it.remove() }
        squares.clear()
    }
}
