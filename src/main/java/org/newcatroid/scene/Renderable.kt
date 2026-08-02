package org.newcatroid.scene

/**
 * Interface for renderable scene objects. Currently only exposes shaderIds: a list
 * of shader ids that should be applied to this object in the render pipeline.
 *
 * NOTE: layer support will be added later when confirmed by the project owner.
 */
interface Renderable {
    val shaderIds: MutableList<String>
    /**
     * Render the object using the provided renderer (backend-specific).
     * The renderer parameter is intentionally typed as Any to avoid coupling to a specific
     * rendering backend in this initial change. Integrations can cast it to the project
     * renderer type (Canvas, OpenGL, etc.) as needed.
     */
    fun render(renderer: Any)
}
