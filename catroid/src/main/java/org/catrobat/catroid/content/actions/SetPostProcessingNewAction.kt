package org.catrobat.catroid.content.actions

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.raptor.PostProcessingComponent
import org.catrobat.catroid.raptor.PostProcessingData
import org.catrobat.catroid.stage.StageActivity
import kotlin.math.max

class SetPostProcessingNewAction : TemporalAction() {
    var scope: Scope? = null
    var effectIndex: Int = 0
    var paramIndex: Int = 0
    var valueFormula: Formula? = null

    override fun update(percent: Float) {
        val stageListener = StageActivity.getActiveStageListener() ?: return
        val threeDManager = stageListener.threeDManager ?: return
        val config = threeDManager.currentConfig ?: return

        val floatVal = valueFormula?.interpretFloat(scope) ?: 0f
        val boolVal = floatVal > 0.5f

        if (effectIndex == 0) { // Global
            when (paramIndex) {
                0 -> config.isActive = boolVal
                1 -> config.qualityScale = floatVal.coerceIn(0.01f, 1.0f)
            }
        } else {
            val effect = getEffectByEffectIndex(config, effectIndex) ?: return
            applyParamExhaustive(effect, paramIndex, floatVal, boolVal)
        }

        threeDManager.updatePostProcessing(config)
    }

    private fun getEffectByEffectIndex(config: PostProcessingComponent, idx: Int): PostProcessingData? {
        val clazz = when (idx) {
            1 -> PostProcessingData.Bloom::class.java
            2 -> PostProcessingData.Vignette::class.java
            3 -> PostProcessingData.Levels::class.java
            4 -> PostProcessingData.Grain::class.java
            5 -> PostProcessingData.Fxaa::class.java
            6 -> PostProcessingData.Chromatic::class.java
            7 -> PostProcessingData.RadialBlur::class.java
            8 -> PostProcessingData.OldTv::class.java
            9 -> PostProcessingData.Crt::class.java
            10 -> PostProcessingData.Fisheye::class.java
            11 -> PostProcessingData.Water::class.java
            12 -> PostProcessingData.MotionBlur::class.java
            13 -> PostProcessingData.LensFlare::class.java
            14 -> PostProcessingData.Gaussian::class.java
            15 -> PostProcessingData.Zoom::class.java
            16 -> PostProcessingData.ACES::class.java
            17 -> PostProcessingData.EyeAdaptation::class.java
            18 -> PostProcessingData.RayTracing::class.java
            19 -> PostProcessingData.SSAO::class.java
            20 -> PostProcessingData.HeightFog::class.java
            21 -> PostProcessingData.DepthOfField::class.java
            22 -> PostProcessingData.GodRays::class.java
            23 -> PostProcessingData.VolumetricFog::class.java
            24 -> PostProcessingData.Upscaler::class.java
            25 -> PostProcessingData.SSGI::class.java
            else -> null
        } ?: return null

        var effect = config.effects.find { clazz.isInstance(it) }
        if (effect == null) {
            try {
                effect = clazz.newInstance()
                config.effects.add(effect)
            } catch (e: Exception) { e.printStackTrace() }
        }
        return effect
    }

    private fun applyParamExhaustive(data: PostProcessingData, pIdx: Int, fVal: Float, bVal: Boolean) {
        if (pIdx == 0) {
            data.isEnabled = bVal
            return
        }

        when (data) {
            is PostProcessingData.Bloom -> when (pIdx) {
                1 -> data.intensity = fVal
                2 -> data.threshold = fVal
                3 -> data.blurAmount = fVal
                4 -> data.size = fVal
                5 -> data.blurPasses = max(1, fVal.toInt())
            }
            is PostProcessingData.Vignette -> when (pIdx) {
                1 -> data.intensity = fVal
                2 -> data.saturation = fVal
            }
            is PostProcessingData.Levels -> when (pIdx) {
                1 -> data.contrast = fVal
                2 -> data.saturation = fVal
                3 -> data.gamma = fVal
            }
            is PostProcessingData.Grain -> when (pIdx) {
                1 -> data.amount = fVal
            }
            is PostProcessingData.Chromatic -> when (pIdx) {
                1 -> data.strength = fVal
                2 -> data.maxDistortion = fVal
            }
            is PostProcessingData.RadialBlur -> when (pIdx) {
                1 -> data.strength = fVal
                2 -> data.size = fVal
                3 -> data.blurPasses = max(1, fVal.toInt())
            }
            is PostProcessingData.OldTv -> when (pIdx) {
                1 -> data.strength = fVal
            }
            is PostProcessingData.Crt -> when (pIdx) {
                1 -> data.distortion = fVal
                2 -> data.zoom = fVal
            }
            is PostProcessingData.Fisheye -> when (pIdx) {
                1 -> data.intensity = fVal
            }
            is PostProcessingData.Water -> when (pIdx) {
                1 -> data.amount = fVal
                2 -> data.speed = fVal
            }
            is PostProcessingData.MotionBlur -> when (pIdx) {
                1 -> data.blurOpacity = fVal.coerceIn(0f, 0.99f)
            }
            is PostProcessingData.LensFlare -> when (pIdx) {
                1 -> data.intensity = fVal
                2 -> data.threshold = fVal
                3 -> data.dispersal = fVal
                4 -> data.size = fVal
            }
            is PostProcessingData.Gaussian -> when (pIdx) {
                1 -> data.amount = fVal
                2 -> data.size = fVal
                3 -> data.passes = max(1, fVal.toInt())
            }
            is PostProcessingData.Zoom -> when (pIdx) {
                1 -> data.zoom = fVal
                2 -> data.originX = fVal
                3 -> data.originY = fVal
            }
            is PostProcessingData.EyeAdaptation -> when (pIdx) {
                1 -> data.targetLuminance = fVal
                2 -> data.speed = fVal
                3 -> data.minExposure = fVal
                4 -> data.maxExposure = fVal
            }
            is PostProcessingData.RayTracing -> when (pIdx) {
                1 -> data.reflectivity = fVal
                2 -> data.steps = max(1, fVal.toInt())
                3 -> data.thickness = fVal
                4 -> data.maxDistance = fVal
                5 -> data.stride = fVal
                6 -> data.jitter = fVal
                7 -> data.edgeFade = fVal
            }
            is PostProcessingData.SSAO -> when (pIdx) {
                1 -> data.intensity = fVal
                2 -> data.radius = fVal
                3 -> data.bias = fVal
            }
            is PostProcessingData.SSGI -> when (pIdx) {
                1 -> data.intensity = fVal
                2 -> data.radius = fVal
                3 -> data.bias = fVal
                4 -> data.ssaoStrength = fVal
                5 -> data.baseAlbedoR = fVal.coerceIn(0f, 1f)
                6 -> data.baseAlbedoG = fVal.coerceIn(0f, 1f)
                7 -> data.baseAlbedoB = fVal.coerceIn(0f, 1f)
                8 -> data.flipDepth = bVal
            }
            is PostProcessingData.HeightFog -> when (pIdx) {
                1 -> data.density = fVal
                2 -> data.falloff = fVal
                3 -> data.height = fVal
                4 -> data.color.r = fVal
                5 -> data.color.g = fVal
                6 -> data.color.b = fVal
            }
            is PostProcessingData.DepthOfField -> when (pIdx) {
                1 -> data.focusDistance = fVal
                2 -> data.focusRange = fVal
                3 -> data.blurSize = fVal
                4 -> data.transition = fVal
                5 -> data.autoFocus = bVal
                6 -> data.autoFocusSpeed = fVal
            }
            is PostProcessingData.GodRays -> when (pIdx) {
                1 -> data.exposure = fVal
                2 -> data.decay = fVal
                3 -> data.density = fVal
                4 -> data.weight = fVal
            }
            is PostProcessingData.VolumetricFog -> when (pIdx) {
                1 -> data.density = fVal
                2 -> data.scattering = fVal
                3 -> data.steps = max(1, fVal.toInt())
                4 -> data.maxDistance = fVal
            }
            is PostProcessingData.Upscaler -> when (pIdx) {
                1 -> data.sharpness = fVal
            }
            else -> {}
        }
    }
}
