SHADERS (visão geral)

Este documento descreve a implementação inicial de um sistema de Shaders no repositório NewCatroid.

O que foi adicionado nesta PR/branch (apenas shaders — LAYERS serão adicionadas depois da sua confirmação)

- src/main/java/org/newcatroid/graphics/Shader.kt
  - Modelo de dados para shaders (id, name, language, code, defaultUniforms)
- src/main/java/org/newcatroid/graphics/ShaderManager.kt
  - Registro, consulta, listagem e remoção simples de shaders
- src/main/java/org/newcatroid/scene/Renderable.kt
  - Interface mínima para objetos renderizáveis com suporte a aplicar shaders por objeto
- shaders/color_bleed.glsl
  - Exemplo de fragment shader "Color-Bleed"

Observações e próximos passos (após esta implementação)

- Integração com o pipeline de render: precisamos ligar o ShaderManager com o renderer (OpenGL/Canvas) e
  executar bind/compilation e setUniforms no momento adequado.
- Persistência: considerar salvar shaders em JSON dentro do projeto para edição pelo usuário.
- Layers: conforme solicitado, as classes e managers de Layer (Layer.kt e LayerManager.kt) NÃO foram adicionadas a
  esta alteração; eu só as adicionarei quando você confirmar que posso prosseguir.
- Fallback Canvas: se o projeto não usar OpenGL, podemos implementar filtros CPU (CPU_FILTER) para simular efeitos.

Como testar rapidamente

1) Carregue o shader de exemplo com ShaderManager.register(...).
2) No seu objeto que implementa Renderable, adicione o id do shader à lista shaderIds.
3) No seu renderer, antes de desenhar o objeto, consulte ShaderManager.get(id) e aplique o código/compilado.

Se quiser que eu já conecte isso ao render backend (GL/Canvas) ou converta para Java, diga qual backend você usa
ou confirme que posso prosseguir com a integração. Quando você confirmar, eu adicionarei também as LAYERS.
