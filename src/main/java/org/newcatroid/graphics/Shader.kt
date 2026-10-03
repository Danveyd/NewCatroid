package org.newcatroid.graphics

/**
 * Represents a shader resource.
 * - id: unique identifier (ex: "color_bleed")
 * - name: human friendly name
 * - language: GLSL, CPU_FILTER, etc.
 * - code: the shader source (GLSL) or filter code
 * - defaultUniforms: default float uniforms for the shader
 */

data class Shader(
    val id: String,
    val name: String,
    val language: ShaderLanguage = ShaderLanguage.GLSL,
    val code: String,
    val defaultUniforms: Map<String, Float> = emptyMap()
)

enum class ShaderLanguage { GLSL, HLSL, CPU_FILTER }
