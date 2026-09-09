package org.catrobat.catroid.utils.shaders

sealed class ShaderTarget {
    object Screen : ShaderTarget()
    data class SpriteTarget(val name: String) : ShaderTarget()
    data class BufferTarget(val name: String) : ShaderTarget()
    data class ParticlesTarget(val id: String) : ShaderTarget()
    data class TextTarget(val textOrVarName: String) : ShaderTarget()

    companion object {
        fun create(targetTypeIndex: Int, targetName: String): ShaderTarget {
            val cleanName = targetName.trim().replace("buffer://", "")
            return when (targetTypeIndex) {
                0 -> Screen
                1 -> SpriteTarget(cleanName)
                2 -> BufferTarget(cleanName)
                3 -> ParticlesTarget(cleanName)
                4 -> TextTarget(cleanName)
                else -> Screen
            }
        }
    }
}
