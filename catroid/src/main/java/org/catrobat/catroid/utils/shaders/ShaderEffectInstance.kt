package org.catrobat.catroid.utils.shaders

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.Vector4
import org.catrobat.catroid.content.EasingFunctions

class ShaderEffectInstance(
    val instanceId: String,
    val presetIndex: Int,
    val shaderProgram: ShaderProgram
) {
    val uniforms = mutableMapOf<String, Any>()
    private val activeTweens = mutableListOf<UniformTween>()

    // Список объектов, исключенных из этого эффекта
    val excludedObjects = HashSet<String>()

    // Текущая цель применения
    var currentTarget: ShaderTarget = ShaderTarget.Screen

    // Логика плавного удаления эффекта
    var isMarkedForDeletion = false
        private set
    private var deleteDuration = 0f
    private var deleteElapsed = 0f
    private var deleteEasing = EasingFunctions.EasingType.LINEAR
    private var initialDeleteStrength = 1.0f

    class UniformTween(
        val uniformName: String,
        val startVal: Float,
        val endVal: Float,
        val duration: Float,
        val easingType: EasingFunctions.EasingType
    ) {
        var elapsed: Float = 0f
        val isFinished: Boolean get() = elapsed >= duration
    }

    fun setUniform(name: String, value: Any) {
        uniforms[name] = value
    }

    fun animateUniform(
        name: String,
        targetValue: Float,
        duration: Float,
        easingType: EasingFunctions.EasingType
    ) {
        val startVal = (uniforms[name] as? Float) ?: 0f
        if (duration <= 0f) {
            uniforms[name] = targetValue
            return
        }
        activeTweens.removeAll { it.uniformName == name }
        activeTweens.add(UniformTween(name, startVal, targetValue, duration, easingType))
    }

    fun startDeletion(duration: Float, easingType: EasingFunctions.EasingType) {
        if (duration <= 0f) {
            GlobalShaderManager.disposeAndRemoveInstanceImmediately(instanceId)
            return
        }
        isMarkedForDeletion = true
        deleteDuration = duration
        deleteElapsed = 0f
        deleteEasing = easingType
        initialDeleteStrength = (uniforms["u_strength"] as? Float) ?: 1.0f
    }

    fun update(delta: Float): Boolean {
        // Если эффект удаляется — плавно сводим его силу до 0
        if (isMarkedForDeletion) {
            deleteElapsed += delta
            val newStrength = EasingFunctions.calculate(
                deleteEasing,
                deleteElapsed,
                deleteDuration,
                initialDeleteStrength,
                0f
            )
            uniforms["u_strength"] = newStrength
            uniforms["u_density"] = newStrength

            if (deleteElapsed >= deleteDuration) {
                return true // Сигнал для полного удаления из памяти
            }
        }

        // Обновляем анимированные параметры (Tweens)
        val iterator = activeTweens.iterator()
        while (iterator.hasNext()) {
            val tween = iterator.next()
            tween.elapsed += delta

            val currentVal = EasingFunctions.calculate(
                tween.easingType,
                tween.elapsed,
                tween.duration,
                tween.startVal,
                tween.endVal
            )
            uniforms[tween.uniformName] = currentVal

            if (tween.isFinished) {
                iterator.remove()
            }
        }
        return false
    }

    fun applyUniformsToShader() {
        if (!shaderProgram.isCompiled) return

        val currentTime = GlobalShaderManager.getAccumulatedTime()
        shaderProgram.setUniformf("u_time", currentTime)

        val bw = Gdx.graphics.backBufferWidth.toFloat()
        val bh = Gdx.graphics.backBufferHeight.toFloat()
        val aspect = if (bh > 0f) bw / bh else 1f
        shaderProgram.setUniformf("u_aspectRatio", aspect)

        for ((name, value) in uniforms) {
            val uniformName = if (name.startsWith("u_")) name else "u_$name"
            when (value) {
                is Float -> shaderProgram.setUniformf(uniformName, value)
                is Int -> shaderProgram.setUniformi(uniformName, value)
                is Vector2 -> shaderProgram.setUniformf(uniformName, value.x, value.y)
                is Vector3 -> shaderProgram.setUniformf(uniformName, value.x, value.y, value.z)
                is Vector4 -> shaderProgram.setUniformf(uniformName, value.x, value.y, value.z, value.w)
                is Color -> shaderProgram.setUniformf(uniformName, value.r, value.g, value.b, value.a)
                is Matrix4 -> shaderProgram.setUniformMatrix(uniformName, value)
            }
        }
    }

    fun dispose() {
        shaderProgram.dispose()
        uniforms.clear()
        activeTweens.clear()
        excludedObjects.clear()
    }
}
