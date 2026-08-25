SHADERS AND LAYERS (atualizado)

Nesta atualização implementei o sistema básico de LAYERS além do sistema de SHADERS já existente.
O objetivo foi seguir a maneira mais rápida e eficiente de implementação com o mínimo de acoplamento
ao render backend.

Arquivos adicionados/alterados nesta branch

- src/main/java/org/newcatroid/graphics/Layer.kt
  - Modelo leve de Layer (id, name, visible, zOrder, shaderIds)
- src/main/java/org/newcatroid/graphics/LayerManager.kt
  - Criação rápida de layers, lookup O(1), listagem com ordenação por zOrder
- src/main/java/org/newcatroid/scene/Renderable.kt
  - Atualizado para incluir layerId e suportar shaderIds por objeto
- (anteriormente adicionados)
  - Shader.kt, ShaderManager.kt, shaders/color_bleed.glsl

Design/Decisões para velocidade e eficiência

- Mantive os managers como estruturas simples (maps + incremental id) para operações rápidas.
- Evitei manter referências diretas de objetos da cena dentro do LayerManager — cada objeto
  possui um layerId. O renderer agrupa objetos por layerId durante a fase de desenho.
- Deferi ordenação até a chamada de list() para reduzir custo em operações de criação/modificação.
  Em jogos com poucas layers (o padrão) essa abordagem é mais rápida do que manter uma estrutura
  sempre ordenada.
- Shaders podem ser aplicados tanto à layer quanto a objetos individuais. A ordem de aplicação
  deve ser: shaders da layer (na ordem em layer.shaderIds) e depois shaders do objeto (object.shaderIds).

Integração sugerida com o pipeline de render (exemplo rápido, pseudo-Kotlin)

```kotlin
// collect renderables from scene
val allRenderables: List<Renderable> = scene.getAllRenderables()

// group by layerId
val grouped: Map<Int, List<Renderable>> = allRenderables.groupBy { it.layerId }

// iterate layers in order
for (layer in LayerManager.list()) {
    if (!layer.visible) continue
    val listForLayer = grouped[layer.id] ?: continue

    // bind/apply layer shaders first
    for (shaderId in layer.shaderIds) {
        val shader = ShaderManager.get(shaderId)
        // compile/bind shader depending on backend
    }

    // draw objects in this layer
    for (obj in listForLayer) {
        // apply object-specific shaders (may override or be concatenated)
        for (shaderId in obj.shaderIds) {
            val shader = ShaderManager.get(shaderId)
            // bind/apply per-object shader
        }
        obj.render(renderer)
    }
}

// draw objects without a layer (layerId == 0)
val noLayer = grouped[0]
noLayer?.forEach { it.render(renderer) }
```

Boas práticas e performance

- Minimize trocas de shader/programa: agrupe objetos que usam o mesmo shaders para reduzir binds.
- Prefira shaders simples para objetos pequenos e reserve efeitos pesados para layers inteiras
  (por exemplo post-processing) sempre que possível.
- Em Canvas 2D, implemente CPU_FILTER shaders com Bitmap post-processing e use texture caching
  para reduzir recomputação.

Próximos passos realizados e que posso executar agora

- Posso integrar a compilação e bind dos shaders diretamente ao render backend (OpenGL ES / libGDX)
  e adicionar fallback para Canvas 2D.
- Posso adicionar testes unitários e exemplos no projeto demonstrando uso de layers e shaders.
- Posso abrir um PR final com todas as mudanças (já commitadas no branch feature/shaders-layers).

Diga qual integração de backend prefere que eu faça agora (OpenGL ES / libGDX ou Canvas 2D),
ou se quer que eu apenas gere o PR e deixe a integração fina para depois.