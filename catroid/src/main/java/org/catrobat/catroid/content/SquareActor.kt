package org.catrobat.catroid.content

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer

class SquareActor(
    private val x: Float,
    private val y: Float,
    private val width: Float,
    private val height: Float,
    colorHex: String,
    private val rotation: Float = 0f, // Добавляем угол поворота
    transparency: Float = 1f, // Добавляем прозрачность
    private val cornerRadius: Float = 0f // Добавляем закругление углов
) : Actor() {
    private val shapeRenderer: ShapeRenderer = ShapeRenderer()
    private val color: Color = Color.valueOf(colorHex).cpy().apply { a = transparency } // Устанавливаем прозрачность

    init {
        setBounds(x, y, width, height)
    }

    override fun draw(batch: Batch?, parentAlpha: Float) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = color

        val xOffset = x
        val yOffset = y

        shapeRenderer.identity()
        shapeRenderer.translate(xOffset + width / 2, yOffset + height / 2, 0f)
        shapeRenderer.rotate(0f, 0f, 1f, rotation) // Устанавливаем угол поворота

        // Рисуем квадрат с закругленными углами (если radius > 0)
        if (cornerRadius > 0) {
            drawRoundedRectangle(shapeRenderer, -width / 2, -height / 2, width, height, cornerRadius)
            // Здесь реализуйте логику для рисования закругленного квадрата, если необходимо
        } else {
            shapeRenderer.rect(-width / 2, -height / 2, width, height)
        }

        shapeRenderer.end()
    }

    private fun drawRoundedRectangle(
        shapeRenderer: ShapeRenderer,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        radius: Float
    ) {
        // Рисуем основной прямоугольник
        shapeRenderer.rect(x + radius, y, width - 2 * radius, height)
        shapeRenderer.rect(x, y + radius, width, height - 2 * radius)

        // Рисуем закругленные углы
        shapeRenderer.arc(x + radius, y + radius, radius, 180f, 90f)
        shapeRenderer.arc(x + width - radius, y + radius, radius, 270f, 90f)
        shapeRenderer.arc(x + radius, y + height - radius, radius, 90f, 90f)
        shapeRenderer.arc(x + width - radius, y + height - radius, radius, 0f, 90f)
    }
}
