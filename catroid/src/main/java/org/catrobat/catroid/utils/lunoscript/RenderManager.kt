package org.catrobat.catroid.utils.lunoscript

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Matrix4
import org.catrobat.catroid.utils.lunoscript.LunoValue
import java.util.concurrent.ConcurrentLinkedQueue

// Обертка для задачи на отрисовку. Хранит и саму функцию, и интерпретатор для ее вызова.
data class RenderTask(val function: LunoValue.LunoFunction, val interpreter: Interpreter)

// Наш менеджер, который будет жить в единственном экземпляре
object RenderManager {
    // Потокобезопасная очередь, чтобы скрипты из разных потоков могли добавлять задачи
    private val tasks = ConcurrentLinkedQueue<RenderTask>()
    private val shapeRenderer = ShapeRenderer()

    private var fbo: FrameBuffer? = null
    private var fboRegion: TextureRegion? = null
    private var batch: SpriteBatch? = null

    // Инициализация. Вызывать один раз из StageListener.create()
    fun initialize(width: Int, height: Int) {
        if (fbo != null) fbo?.dispose()
        fbo = FrameBuffer(Pixmap.Format.RGBA8888, width, height, false)
        fboRegion = TextureRegion(fbo!!.colorBufferTexture)
        fboRegion!!.flip(false, true)

        // --- ИЗМЕНЕНИЕ ---
        // Если batch был уничтожен, создаем его заново
        if (batch != null) batch?.dispose()
        batch = SpriteBatch()
    }

    fun dispose() {
        fbo?.dispose()
        batch?.dispose() // Безопасный вызов
        fbo = null
        batch = null
    }

    // LunoScript будет вызывать этот метод, чтобы зарегистрировать свою функцию-отрисовщик
    fun addTask(task: RenderTask) {
        tasks.add(task)
    }

    fun deleteTask(task: RenderTask) {
        tasks.remove(task)
    }

    fun getWidth(): Int {
        return fbo?.width ?: 0
    }

    fun getHeight(): Int {
        return fbo?.height ?: 0
    }

    // Этот метод будет вызываться из главного рендер-цикла Catroid (из StageListener)
    private fun executeTasks() {
        if (tasks.isEmpty()) return

        fbo!!.begin()
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // --- НАЧАЛО РЕШАЮЩЕГО ИЗМЕНЕНИЯ ---
        // Мы БОЛЬШЕ не используем матрицу из StageListener.
        // Мы устанавливаем свою, простую, где координаты = пиксели.
        shapeRenderer.projectionMatrix.setToOrtho2D(0f, 0f, fbo!!.width.toFloat(), fbo!!.height.toFloat())
        // --- КОНЕЦ РЕШАЮЩЕГО ИЗМЕНЕНИЯ ---

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        for (task in tasks) {
            try {
                val rendererArg = LunoValue.NativeObject(shapeRenderer)
                task.function.call(task.interpreter, listOf(rendererArg), Token(TokenType.EOF, "", null, -1, -1))
            } catch (e: LunoRuntimeError) { /* ... */ }
        }

        shapeRenderer.end()
        fbo!!.end()
    }

    // Теперь render() больше не принимает матрицу
    fun render() {
        // Мы больше не устанавливаем матрицу здесь
        // shapeRenderer.projectionMatrix = projectionMatrix

        executeTasks()

        // Код отрисовки FBO на экран остается без изменений
        batch?.projectionMatrix?.setToOrtho2D(0f, 0f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        batch?.begin()
        batch?.draw(fboRegion, 0f, 0f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        batch?.end()
    }
}