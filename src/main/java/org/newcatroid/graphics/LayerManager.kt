package org.newcatroid.graphics

/**
 * Simple Layer manager. Designed for minimal footprint and fast operations.
 * - create(): O(1)
 * - get/list: O(n log n) for sorting on list() but typical layer counts are small
 *
 * Strategy for speed/efficiency:
 * - Keep an incremental id generator for fast creation.
 * - Store layers in a map for O(1) lookup by id.
 * - Defer sorting (by zOrder) until list() is called so creation/modification is fast.
 */
object LayerManager {
    private val layers = mutableMapOf<Int, Layer>()
    private var nextId = 1

    fun create(name: String, zOrder: Int = 0): Layer {
        val layer = Layer(id = nextId++, name = name, zOrder = zOrder)
        layers[layer.id] = layer
        return layer
    }

    fun remove(id: Int) { layers.remove(id) }

    fun get(id: Int): Layer? = layers[id]

    // Return layers sorted by zOrder (ascending). Callers should cache if they call frequently.
    fun list(): List<Layer> = layers.values.sortedWith(compareBy({ it.zOrder }, { it.id }))

    fun toggleVisibility(id: Int) { layers[id]?.apply { visible = !visible } }

    fun setVisibility(id: Int, visible: Boolean) { layers[id]?.apply { this.visible = visible } }

    fun applyShaderToLayer(layerId: Int, shaderId: String) {
        layers[layerId]?.let { if (!it.shaderIds.contains(shaderId)) it.shaderIds.add(shaderId) }
    }

    fun removeShaderFromLayer(layerId: Int, shaderId: String) { layers[layerId]?.shaderIds?.remove(shaderId) }

    fun clear() { layers.clear(); nextId = 1 }
}
