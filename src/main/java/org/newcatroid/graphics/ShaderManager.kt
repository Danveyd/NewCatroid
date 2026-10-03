package org.newcatroid.graphics

/**
 * Simple manager for shaders. Thread-unsafe for simplicity — adapt synchronization if needed.
 */
object ShaderManager {
    private val shaders = mutableMapOf<String, Shader>()

    fun register(shader: Shader) {
        shaders[shader.id] = shader
        // Optional: validate or compile depending on render backend
    }

    fun unregister(id: String) {
        shaders.remove(id)
    }

    fun get(id: String): Shader? = shaders[id]

    fun listAll(): List<Shader> = shaders.values.toList()

    fun clear() = shaders.clear()
}
