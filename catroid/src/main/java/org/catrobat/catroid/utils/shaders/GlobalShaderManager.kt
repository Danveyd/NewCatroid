package org.catrobat.catroid.utils.shaders

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import org.catrobat.catroid.content.EasingFunctions
import org.catrobat.catroid.stage.StageActivity
import java.util.concurrent.ConcurrentHashMap

object GlobalShaderManager {
    // Центральный Пул всех созданных эффектов
    private val effectPool = ConcurrentHashMap<String, ShaderEffectInstance>()

    // Активные экранные эффекты
    private val activeScreenEffects = ConcurrentHashMap<String, ShaderEffectInstance>()

    private var screenTexture: Texture? = null
    private var maskFbo: FrameBuffer? = null
    private var pingFbo: FrameBuffer? = null
    private var pongFbo: FrameBuffer? = null
    private val screenQuadCamera = OrthographicCamera()
    private var accumulatedTime: Float = 0f

    @JvmStatic
    fun hasActiveScreenShaders(): Boolean = activeScreenEffects.isNotEmpty()

    @JvmStatic
    fun getAccumulatedTime(): Float = accumulatedTime

    // 1. СОЗДАТЬ ЭФФЕКТ (Регистрация в пуле)
    @JvmStatic
    fun createEffect(instanceId: String, presetIndex: Int): ShaderEffectInstance? {
        effectPool[instanceId]?.dispose()

        val fragmentShader = when (presetIndex) {
            0 -> ShaderPresets.SHOCKWAVE_FRAGMENT
            1 -> ShaderPresets.GLITCH_FRAGMENT
            2 -> ShaderPresets.BULGE_FRAGMENT
            3 -> ShaderPresets.BLOOM_FRAGMENT
            4 -> ShaderPresets.VOLUMETRIC_FOG_2D_FRAGMENT
            5 -> ShaderPresets.PIXELATE_FRAGMENT
            6 -> ShaderPresets.COLOR_EDIT_FRAGMENT
            7 -> ShaderPresets.VIGNETTE_FRAGMENT
            8 -> ShaderPresets.CRT_FRAGMENT
            9 -> ShaderPresets.RADIAL_BLUR_FRAGMENT
            else -> ShaderPresets.SHOCKWAVE_FRAGMENT
        }

        val program = ShaderProgram(ShaderPresets.COMMON_VERTEX_SHADER, fragmentShader)
        if (!program.isCompiled) {
            Gdx.app.error("GlobalShaderManager", "Error compiling shader $presetIndex: ${program.log}")
            program.dispose()
            return null
        }

        val instance = ShaderEffectInstance(instanceId, presetIndex, program)
        setupDefaultUniforms(instance, presetIndex)

        effectPool[instanceId] = instance
        return instance
    }

    private fun setupDefaultUniforms(instance: ShaderEffectInstance, presetIndex: Int) {
        instance.setUniform("u_center", Vector2(0.5f, 0.5f))
        when (presetIndex) {
            0 -> { // Shockwave
                instance.setUniform("u_radius", 0.0f)
                instance.setUniform("u_thickness", 0.1f)
                instance.setUniform("u_strength", 0.3f)
                instance.setUniform("u_invert", 0)
            }
            1 -> { // Glitch
                instance.setUniform("u_speed", 5.0f)
                instance.setUniform("u_strength", 0.5f)
                instance.setUniform("u_sliceHeight", 0.05f)
                instance.setUniform("u_maxSliceXOff", 0.05f)
                instance.setUniform("u_rgbOff", 0.02f)
            }
            2 -> { // Bulge
                instance.setUniform("u_radius", 0.35f)
                instance.setUniform("u_strength", 0.5f)
            }
            3 -> { // Bloom
                instance.setUniform("u_threshold", 0.5f)
                instance.setUniform("u_intensity", 1.0f)
            }
            4 -> { // Fog
                instance.setUniform("u_speed", 0.2f)
                instance.setUniform("u_density", 0.5f)
                instance.setUniform("u_scale", 3.0f)
                instance.setUniform("u_fogColor", Color.WHITE)
            }
            5 -> { // Pixelate
                instance.setUniform("u_pixelSize", Vector2(0.01f, 0.01f))
            }
            6 -> { // Color Edit
                instance.setUniform("u_grayscale", 1.0f)
                instance.setUniform("u_sepia", 0.0f)
                instance.setUniform("u_hue", 0.0f)
                instance.setUniform("u_colorTint", Vector3(1.0f, 1.0f, 1.0f))
            }
            7 -> { // Vignette
                instance.setUniform("u_radius", 0.75f)
                instance.setUniform("u_softness", 0.45f)
            }
            8 -> { // CRT
                instance.setUniform("u_linesCount", 400.0f)
            }
            9 -> { // Radial Blur
                instance.setUniform("u_strength", 1.0f)
            }
        }
    }

    // 2. ПРИМЕНИТЬ ЭФФЕКТ К ЦЕЛИ
    @JvmStatic
    fun applyEffectToTarget(instanceId: String, targetTypeIndex: Int, targetName: String) {
        val instance = effectPool[instanceId] ?: return
        val target = ShaderTarget.create(targetTypeIndex, targetName)
        instance.currentTarget = target

        when (target) {
            is ShaderTarget.Screen -> {
                activeScreenEffects[instanceId] = instance
            }
            is ShaderTarget.SpriteTarget -> {
                activeScreenEffects.remove(instanceId)
                val stageListener = StageActivity.getActiveStageListener()
                val sprite = stageListener?.spritesFromStage?.firstOrNull {
                    it.name.equals(target.name, ignoreCase = true) || it.runtimeName.equals(target.name, ignoreCase = true)
                }
                sprite?.look?.let { look ->
                    if (!look.shaderChain.contains(instance)) {
                        look.shaderChain.add(instance)
                    }
                }
            }
            is ShaderTarget.BufferTarget -> {
                activeScreenEffects.remove(instanceId)
                org.catrobat.catroid.content.RenderTextureManager.setBufferShader(
                    target.name,
                    ShaderPresets.COMMON_VERTEX_SHADER,
                    instance.shaderProgram.fragmentShaderSource
                )
            }
            else -> {}
        }
    }

    // 3. ИСКЛЮЧИТЬ ОБЪЕКТ ИЗ ЭФФЕКТА
    @JvmStatic
    fun excludeObjectFromEffect(objectId: String, instanceId: String) {
        val instance = effectPool[instanceId] ?: return
        val cleanObjId = objectId.trim()

        // Если это локальный шейдер спрайта — отвязываем его
        val stageListener = StageActivity.getActiveStageListener()
        val sprite = stageListener?.spritesFromStage?.firstOrNull {
            it.name.equals(cleanObjId, ignoreCase = true) || it.runtimeName.equals(cleanObjId, ignoreCase = true)
        }
        sprite?.look?.shaderChain?.remove(instance)

        // Добавляем в маску исключения экранного эффекта
        instance.excludedObjects.add(cleanObjId)
    }

    // 4. УДАЛИТЬ ЭФФЕКТ
    @JvmStatic
    fun deleteEffect(instanceId: String, duration: Float, easingSelection: Int) {
        val instance = effectPool[instanceId] ?: return
        val easingType = EasingFunctions.EasingType.values().getOrNull(easingSelection)
            ?: EasingFunctions.EasingType.LINEAR

        if (duration <= 0f) {
            disposeAndRemoveInstanceImmediately(instanceId)
        } else {
            instance.startDeletion(duration, easingType)
        }
    }

    @JvmStatic
    fun disposeAndRemoveInstanceImmediately(instanceId: String) {
        val instance = effectPool.remove(instanceId) ?: return
        activeScreenEffects.remove(instanceId)

        // Удаляем из всех спрайтов
        val stageListener = StageActivity.getActiveStageListener()
        stageListener?.spritesFromStage?.forEach { sprite ->
            sprite.look?.shaderChain?.remove(instance)
        }

        instance.dispose()
    }

    @JvmStatic
    fun getEffect(instanceId: String): ShaderEffectInstance? = effectPool[instanceId]

    @JvmStatic
    fun update(delta: Float) {
        accumulatedTime = (accumulatedTime + delta) % 3600f

        val iterator = effectPool.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val shouldDispose = entry.value.update(delta)
            if (shouldDispose) {
                activeScreenEffects.remove(entry.key)
                val stageListener = StageActivity.getActiveStageListener()
                stageListener?.spritesFromStage?.forEach { sprite ->
                    sprite.look?.shaderChain?.remove(entry.value)
                }
                entry.value.dispose()
                iterator.remove()
            }
        }
    }

    // РЕНДЕРИНГ ЭКРАННЫХ ЭФФЕКТОВ С ЗАХВАТОМ VRAM И МАСКОЙ ИСКЛЮЧЕНИЙ
    @JvmStatic
    fun captureAndRenderPostProcessing(batch: SpriteBatch) {
        if (activeScreenEffects.isEmpty()) return

        val bw = Gdx.graphics.backBufferWidth
        val bh = Gdx.graphics.backBufferHeight
        if (bw <= 0 || bh <= 0) return

        if (screenTexture == null || screenTexture!!.width != bw || screenTexture!!.height != bh) {
            screenTexture?.dispose()
            screenTexture = Texture(bw, bh, Pixmap.Format.RGBA8888)
        }

        // Захватом VRAM копируем экран в текстуру
        screenTexture!!.bind()
        Gdx.gl.glCopyTexSubImage2D(GL20.GL_TEXTURE_2D, 0, 0, 0, 0, 0, bw, bh)

        if (pingFbo == null || pingFbo!!.width != bw || pingFbo!!.height != bh) {
            pingFbo?.dispose()
            pongFbo?.dispose()
            maskFbo?.dispose()
            pingFbo = FrameBuffer(Pixmap.Format.RGBA8888, bw, bh, false)
            pongFbo = FrameBuffer(Pixmap.Format.RGBA8888, bw, bh, false)
            maskFbo = FrameBuffer(Pixmap.Format.RGBA8888, bw, bh, false)
        }

        Gdx.gl.glViewport(0, 0, bw, bh)
        screenQuadCamera.setToOrtho(false, bw.toFloat(), bh.toFloat())
        screenQuadCamera.update()

        var currentTexture = screenTexture!!
        var writeFbo = pingFbo!!

        val oldShader = batch.shader
        val oldProj = batch.projectionMatrix

        batch.projectionMatrix = screenQuadCamera.combined

        val effectsList = activeScreenEffects.values.toList()
        for (i in effectsList.indices) {
            val effect = effectsList[i]
            val isLast = (i == effectsList.size - 1)

            if (!isLast) {
                writeFbo.begin()
                Gdx.gl.glViewport(0, 0, bw, bh)
                Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
            }

            batch.shader = effect.shaderProgram
            batch.disableBlending()
            batch.begin()

            // Настройка Маски Исключений
            if (effect.excludedObjects.isNotEmpty()) {
                renderMaskForExcludedObjects(effect, bw, bh)
                maskFbo!!.colorBufferTexture.bind(1)
                Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0)
                effect.shaderProgram.setUniformi("u_maskTexture", 1)
                effect.shaderProgram.setUniformi("u_hasMask", 1)
            } else {
                effect.shaderProgram.setUniformi("u_hasMask", 0)
            }

            effect.applyUniformsToShader()

            batch.draw(
                currentTexture,
                0f, 0f,
                bw.toFloat(), bh.toFloat(),
                0, 0,
                currentTexture.width, currentTexture.height,
                false, true
            )
            batch.end()
            batch.enableBlending()

            if (!isLast) {
                writeFbo.end()
                currentTexture = writeFbo.colorBufferTexture
                writeFbo = if (writeFbo == pingFbo) pongFbo!! else pingFbo!!
            }
        }

        batch.shader = oldShader
        batch.projectionMatrix = oldProj
    }

    private fun renderMaskForExcludedObjects(effect: ShaderEffectInstance, bw: Int, bh: Int) {
        val mask = maskFbo ?: return
        mask.begin()
        Gdx.gl.glViewport(0, 0, bw, bh)
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // Отрисовка силуэтов исключенных актеров белым цветом (1,1,1,1)
        val stageListener = StageActivity.getActiveStageListener()
        val stage = stageListener?.stage
        if (stage != null) {
            val batch = stage.batch
            batch.setProjectionMatrix(stage.camera.combined)
            batch.begin()

            stage.root.children.forEach { actor ->
                if (actor != null && actor.isVisible) {
                    val isExcluded = effect.excludedObjects.any { name ->
                        actor.name?.equals(name, ignoreCase = true) == true
                    }
                    if (isExcluded) {
                        actor.draw(batch, 1.0f)
                    }
                }
            }
            batch.end()
        }
        mask.end()
    }

    @JvmStatic
    fun clear() {
        for (effect in effectPool.values) {
            effect.dispose()
        }
        effectPool.clear()
        activeScreenEffects.clear()

        screenTexture?.dispose()
        pingFbo?.dispose()
        pongFbo?.dispose()
        maskFbo?.dispose()

        screenTexture = null
        pingFbo = null
        pongFbo = null
        maskFbo = null
        accumulatedTime = 0f
    }

    @JvmStatic
    fun dispose() {
        clear()
    }
}
