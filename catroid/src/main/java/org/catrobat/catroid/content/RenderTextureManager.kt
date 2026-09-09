package org.catrobat.catroid.content

import android.util.Log
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.Vector4
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.utils.ScreenUtils
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.stage.ShowTextActor
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.utils.BufferVideoRecorder

class RenderTexture(val width: Int, val height: Int) {
    var fbo: FrameBuffer = FrameBuffer(Pixmap.Format.RGBA8888, width, height, true)
    var camera2D = OrthographicCamera(width.toFloat(), height.toFloat())
    var camera3D = PerspectiveCamera(67f, width.toFloat(), height.toFloat())
    var textureRegion: TextureRegion

    val spritesToRender = mutableListOf<Sprite>()
    val actorsToRender = mutableListOf<Actor>()

    var autoUpdate: Boolean = true
    var needsUpdate: Boolean = true

    var render2D: Boolean = true
    var render3D: Boolean = false

    var shader: ShaderProgram? = null
    val customUniforms = mutableMapOf<String, Any>()
    var useMipmaps: Boolean = false

    var use3DPostProcessing: Boolean = false
    var vfxManager: com.crashinvaders.vfx.VfxManager? = null

    init {
        textureRegion = TextureRegion(fbo.colorBufferTexture).apply { flip(false, true) }
        camera3D.near = 0.1f
        camera3D.far = 2500f
    }

    fun dispose() {
        fbo.dispose()
        shader?.dispose()
        vfxManager?.dispose()
        spritesToRender.clear()
        actorsToRender.clear()
        customUniforms.clear()
    }
}

object RenderTextureManager {
    val renderTextures = mutableMapOf<String, RenderTexture>()

    private val activeTexturesList = ArrayList<RenderTexture>()

    var isRenderingToBuffer: Boolean = false
        private set

    @JvmStatic var isMain2DRenderEnabled: Boolean = true
    @JvmStatic var isMainFast2DRenderEnabled: Boolean = true
    @JvmStatic var isMain3DRenderEnabled: Boolean = true

    private const val GLOBAL_ROTATION_FIX = 90f
    private val tempMatrix = Matrix4()

    private var tempFbo: FrameBuffer? = null
    private val shaderCamera = OrthographicCamera()

    private var totalTime: Float = 0f

    fun createRenderTarget(name: String, width: Int, height: Int) {
        val existing = renderTextures[name]
        if (existing != null && existing.width == width && existing.height == height) return

        Gdx.app.postRunnable {
            renderTextures[name]?.dispose()
            val newTarget = RenderTexture(width, height)
            renderTextures[name] = newTarget
            rebuildActiveTexturesList()
        }
    }

    private fun rebuildActiveTexturesList() {
        activeTexturesList.clear()
        activeTexturesList.addAll(renderTextures.values)
    }

    @JvmStatic
    fun addVariableTextToTarget(bufferName: String, variableName: String) {
        val target = renderTextures[bufferName] ?: return
        val stageListener = StageActivity.getActiveStageListener() ?: return
        val actors = stageListener.stage?.actors ?: return

        for (i in 0 until actors.size) {
            val actor = actors.get(i)
            if (actor is ShowTextActor) {
                if (actor.variableNameToCompare.equals(variableName.trim(), ignoreCase = true)) {
                    if (!target.actorsToRender.contains(actor)) {
                        target.actorsToRender.add(actor)
                    }
                    break
                }
            }
        }
    }

    @JvmStatic
    fun setTextBufferOnly(variableName: String, onlyInBuffer: Boolean) {
        val stageListener = StageActivity.getActiveStageListener() ?: return
        val actors = stageListener.stage?.actors ?: return
        val trimmedName = variableName.trim()

        for (i in 0 until actors.size) {
            val actor = actors.get(i)
            if (actor is ShowTextActor) {
                if (actor.variableNameToCompare.equals(trimmedName, ignoreCase = true)) {
                    actor.setDrawOnlyInBuffer(onlyInBuffer)
                    break
                }
            }
        }
    }

    @JvmStatic
    fun removeVariableTextFromTarget(bufferName: String, variableName: String) {
        val target = renderTextures[bufferName] ?: return
        val iterator = target.actorsToRender.iterator()
        while (iterator.hasNext()) {
            val actor = iterator.next()
            if (actor is ShowTextActor) {
                if (actor.variableNameToCompare.equals(variableName.trim(), ignoreCase = true)) {
                    iterator.remove()
                    break
                }
            }
        }
    }

    fun addSpriteToTarget(name: String, sprite: Sprite) {
        val target = renderTextures[name] ?: return
        if (!target.spritesToRender.contains(sprite)) {
            target.spritesToRender.add(sprite)
        }
    }

    fun removeSpriteFromTarget(name: String, sprite: Sprite) {
        renderTextures[name]?.spritesToRender?.remove(sprite)
    }

    fun setBufferMode(name: String, r2d: Boolean, r3d: Boolean) {
        renderTextures[name]?.let {
            it.render2D = r2d
            it.render3D = r3d
        }
    }

    fun setBufferMipmapping(name: String, enabled: Boolean) {
        renderTextures[name]?.useMipmaps = enabled
    }

    fun setBufferShader(name: String, vertexCode: String?, fragmentCode: String?) {
        val target = renderTextures[name] ?: return
        Gdx.app.postRunnable {
            target.shader?.dispose()
            if (vertexCode.isNullOrEmpty() || fragmentCode.isNullOrEmpty()) {
                target.shader = null
            } else {
                val program = ShaderProgram(vertexCode, fragmentCode)
                if (program.isCompiled) {
                    target.shader = program
                } else {
                    Log.e("RenderTextureManager", "Ошибка шейдера буфера: ${program.log}")
                    program.dispose()
                    target.shader = null
                }
            }
        }
    }

    @JvmStatic
    fun setBufferShaderUniform(bufferName: String, name: String, value: Any) {
        val target = renderTextures[bufferName] ?: return
        target.customUniforms["u_$name"] = value
    }

    fun setBufferPostProcessing(name: String, enabled: Boolean) {
        val target = renderTextures[name] ?: return
        val tDM = StageActivity.getActiveStageListener()?.threeDManager ?: return

        Gdx.app.postRunnable {
            target.use3DPostProcessing = enabled
            tDM.setupBufferPipeline(target.fbo, target.width, target.height, enabled)
        }
    }

    fun setTargetCamera2D(name: String, x: Float, y: Float, zoom: Float, rotation: Float) {
        renderTextures[name]?.camera2D?.let {
            it.position.set(x, y, 0f)
            it.zoom = zoom
            it.up.set(0f, 1f, 0f)
            it.direction.set(0f, 0f, -1f)
            it.rotate(rotation + GLOBAL_ROTATION_FIX)
            it.update()
        }
    }

    fun setTargetCamera3D(name: String, x: Float, y: Float, z: Float, yaw: Float, pitch: Float, roll: Float, fov: Float) {
        renderTextures[name]?.camera3D?.let {
            it.position.set(x, y, z)
            it.fieldOfView = fov

            val rotation = com.badlogic.gdx.math.Quaternion().setEulerAngles(yaw, pitch, roll)
            it.direction.set(0f, 0f, -1f)
            rotation.transform(it.direction)
            it.up.set(0f, 1f, 0f)
            rotation.transform(it.up)

            it.update()
        }
    }

    fun setAutoUpdate(name: String, auto: Boolean) {
        renderTextures[name]?.autoUpdate = auto
    }

    fun saveBufferToFile(name: String, fileName: String) {
        Gdx.app.postRunnable {
            val target = renderTextures[name] ?: return@postRunnable

            target.fbo.begin()
            val pixels = ScreenUtils.getFrameBufferPixels(0, 0, target.width, target.height, true)
            target.fbo.end()

            Thread {
                val pixmap = Pixmap(target.width, target.height, Pixmap.Format.RGBA8888)
                val buffer = pixmap.pixels
                buffer.clear()
                buffer.put(pixels)
                buffer.position(0)

                val projectDir = ProjectManager.getInstance().currentProject.filesDir.absolutePath
                val file = Gdx.files.absolute("$projectDir/$fileName")

                PixmapIO.writePNG(file, pixmap)
                pixmap.dispose()
                Log.d("RenderTextureManager", "Сохранен скриншот буфера: ${file.path()}")
            }.start()
        }
    }

    fun getTextureRegion(name: String): TextureRegion? = renderTextures[name]?.textureRegion
    fun getWidth(name: String): Int = renderTextures[name]?.width ?: 0
    fun getHeight(name: String): Int = renderTextures[name]?.height ?: 0

    fun renderAllTargets(batch: Batch) {
        if (activeTexturesList.isEmpty()) return
        isRenderingToBuffer = true

        totalTime += Gdx.graphics.deltaTime

        tempMatrix.set(batch.projectionMatrix)
        val wasDrawing = batch.isDrawing
        if (wasDrawing) batch.end()

        for (idx in 0 until activeTexturesList.size) {
            val target = activeTexturesList[idx]
            if (!target.autoUpdate && !target.needsUpdate) continue

            if (target.render3D) {
                StageActivity.getActiveStageListener()?.threeDManager?.renderSceneForCustomCamera(target.camera3D, target.fbo, target.use3DPostProcessing)
            }

            if (target.render2D) {
                target.fbo.begin()
                if (!target.render3D) {
                    Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
                    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
                }

                target.camera2D.update()
                batch.projectionMatrix = target.camera2D.combined
                batch.begin()

                for (i in 0 until target.spritesToRender.size) {
                    target.spritesToRender[i].look?.draw(batch, 1.0f)
                }

                for (i in 0 until target.actorsToRender.size) {
                    target.actorsToRender[i].draw(batch, 1.0f)
                }

                val targetName = renderTextures.entries.firstOrNull { it.value == target }?.key ?: ""
                if (targetName.isNotEmpty()) {
                    org.catrobat.catroid.particles.ParticleManager.getInstance()
                        .renderForBuffer(targetName, batch)
                }

                batch.end()
                target.fbo.end()
            }

            if (target.shader != null) {
                applyShaderPostProcessing(target, batch)
            }

            if (target.useMipmaps) {
                target.fbo.colorBufferTexture.bind()
                Gdx.gl.glGenerateMipmap(GL20.GL_TEXTURE_2D)
                target.fbo.colorBufferTexture.setFilter(com.badlogic.gdx.graphics.Texture.TextureFilter.MipMapLinearLinear, com.badlogic.gdx.graphics.Texture.TextureFilter.Linear)
            }

            target.needsUpdate = false

            val targetName = renderTextures.entries.firstOrNull { it.value == target }?.key ?: ""
            if (targetName.isNotEmpty()) {
                BufferVideoRecorder.onFrameRendered(targetName, target)
            }
        }

        batch.projectionMatrix = tempMatrix
        if (wasDrawing) batch.begin()
        isRenderingToBuffer = false
    }

    private fun applyShaderPostProcessing(target: RenderTexture, batch: Batch) {
        val shader = target.shader ?: return

        if (tempFbo == null || tempFbo!!.width != target.width || tempFbo!!.height != target.height) {
            tempFbo?.dispose()
            tempFbo = FrameBuffer(Pixmap.Format.RGBA8888, target.width, target.height, true)
        }

        val temp = tempFbo!!

        temp.begin()
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        shaderCamera.setToOrtho(false, target.width.toFloat(), target.height.toFloat())
        shaderCamera.update()

        val oldShader = batch.shader
        val oldProj = batch.projectionMatrix

        batch.shader = shader
        batch.projectionMatrix = shaderCamera.combined
        batch.begin()

        if (shader.hasUniform("u_time")) {
            shader.setUniformf("u_time", totalTime)
        }
        if (shader.hasUniform("u_resolution")) {
            shader.setUniformf("u_resolution", target.width.toFloat(), target.height.toFloat())
        }

        for ((uniformName, value) in target.customUniforms) {
            when (value) {
                is Float -> shader.setUniformf(uniformName, value)
                is Int -> shader.setUniformi(uniformName, value)
                is Vector2 -> shader.setUniformf(uniformName, value.x, value.y)
                is Vector3 -> shader.setUniformf(uniformName, value.x, value.y, value.z)
                is Vector4 -> shader.setUniformf(uniformName, value.x, value.y, value.z, value.w)
                is Color -> shader.setUniformf(uniformName, value.r, value.g, value.b, value.a)
                is Matrix4 -> shader.setUniformMatrix(uniformName, value)
            }
        }

        batch.draw(target.fbo.colorBufferTexture, 0f, 0f, target.width.toFloat(), target.height.toFloat(),
            0, 0, target.width, target.height, false, true)

        batch.end()
        batch.shader = oldShader
        batch.projectionMatrix = oldProj
        temp.end()

        target.fbo.begin()
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        batch.projectionMatrix = shaderCamera.combined
        batch.begin()
        batch.draw(temp.colorBufferTexture, 0f, 0f, target.width.toFloat(), target.height.toFloat(),
            0, 0, target.width, target.height, false, true)
        batch.end()
        target.fbo.end()
    }

    fun clearAll() {
        val tDM = StageActivity.getActiveStageListener()?.threeDManager
        renderTextures.values.forEach {
            tDM?.removeBufferPipeline(it.fbo)
            it.dispose()
        }
        renderTextures.clear()
        activeTexturesList.clear()
        tempFbo?.dispose()
        tempFbo = null
        totalTime = 0f

        isMain2DRenderEnabled = true
        isMainFast2DRenderEnabled = true
        isMain3DRenderEnabled = true
    }
}
