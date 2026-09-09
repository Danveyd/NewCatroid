package org.catrobat.catroid.content.actions

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.utils.shaders.GlobalShaderManager
import org.catrobat.catroid.utils.shaders.ShaderPresets

class ApplyShaderAction : TemporalAction() {
    var scope: Scope? = null
    var effectIdFormula: Formula? = null
    var presetSelection: Int = 0
    var targetFormula: Formula? = null
    var strengthFormula: Formula? = null

    override fun update(percent: Float) {
        /*try {
            val id = effectIdFormula?.interpretString(scope)?.ifEmpty { "shader1" } ?: "shader1"
            val targetStr = targetFormula?.interpretString(scope) ?: ""
            val strength = strengthFormula?.interpretFloat(scope) ?: 0.5f

            val fragmentShader = when (presetSelection) {
                0 -> ShaderPresets.SHOCKWAVE_FRAGMENT
                1 -> ShaderPresets.GLITCH_FRAGMENT
                2 -> ShaderPresets.BULGE_FRAGMENT
                3 -> ShaderPresets.BLOOM_FRAGMENT
                4 -> ShaderPresets.VOLUMETRIC_FOG_2D_FRAGMENT
                5 -> ShaderPresets.PIXELATE_FRAGMENT
                6 -> ShaderPresets.COLOR_EDIT_FRAGMENT
                else -> ShaderPresets.SHOCKWAVE_FRAGMENT
            }

            val instance = GlobalShaderManager.applyShader(
                id,
                targetStr,
                ShaderPresets.COMMON_VERTEX_SHADER,
                fragmentShader
            )

            instance?.let { eff ->
                eff.setUniform("u_center", Vector2(0.5f, 0.5f))

                when (presetSelection) {
                    0 -> { // Shockwave
                        eff.setUniform("u_speed", 1.0f)
                        eff.setUniform("u_thickness", 0.1f)
                        eff.setUniform("u_strength", strength * 0.5f)
                        eff.setUniform("u_maxSize", 1.5f)
                        eff.setUniform("u_invert", 0)
                    }
                    1 -> { // Glitch
                        eff.setUniform("u_speed", 5.0f)
                        eff.setUniform("u_strength", strength)
                        eff.setUniform("u_sliceHeight", 0.05f)
                        eff.setUniform("u_maxSliceXOff", 0.05f)
                        eff.setUniform("u_rgbOff", 0.02f)
                    }
                    2 -> { // Bulge
                        eff.setUniform("u_radius", 0.35f)
                        eff.setUniform("u_strength", strength)
                    }
                    3 -> { // Bloom
                        eff.setUniform("u_threshold", 0.5f)
                        eff.setUniform("u_intensity", strength * 2.0f)
                    }
                    4 -> { // Fog 2D
                        eff.setUniform("u_speed", 0.2f)
                        eff.setUniform("u_density", strength)
                        eff.setUniform("u_scale", 3.0f)
                        eff.setUniform("u_fogColor", Color.WHITE)
                    }
                    5 -> { // Pixelate
                        val px = maxOf(0.001f, 0.02f * strength)
                        eff.setUniform("u_pixelSize", Vector2(px, px))
                    }
                    6 -> { // Color / Sepia
                        eff.setUniform("u_grayscale", strength)
                        eff.setUniform("u_sepia", 0.0f)
                        eff.setUniform("u_hue", 0.0f)
                        eff.setUniform("u_colorTint", Vector3(1.0f, 1.0f, 1.0f))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }*/
    }
}
