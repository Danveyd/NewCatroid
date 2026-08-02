package org.newcatroid.scene

/**
 * Interface for renderable scene objects. Exposes:
 * - layerId: the id of the Layer this object belongs to (default 0 = no layer)
 * - shaderIds: list of shader ids applied to this object specifically
 * - render(renderer): draw using the provided renderer object (backend-specific)
 *
 * Notes:
 * - Layers are managed by LayerManager. During render, group objects by layer id
 *   and iterate layers in zOrder. Skip layers with visible == false.
 * - Keep Renderable lightweight: do not store heavy references to render resources.
 */
interface Renderable {
    var layerId: Int
    val shaderIds: MutableList<String>
    fun render(renderer: Any)
}
