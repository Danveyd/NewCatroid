package org.newcatroid.graphics

/**
 * Represents a render Layer which groups renderables together.
 * Layers are lightweight and intended to be used by the scene/renderer to
 * batch rendering, control visibility and apply layer-level shaders.
 */

data class Layer(
    val id: Int,
    var name: String,
    var visible: Boolean = true,
    var zOrder: Int = 0,
    val shaderIds: MutableList<String> = mutableListOf()
)
