# Plugins do NewCatroid (.nplug)

Plugins permitem personalizar a interface e o comportamento principal do aplicativo. Um plugin é distribuído como um arquivo `.nplug`.

> Outros idiomas: [English](plugins.en.md) • [Русский](plugins.ru.md)

## O que é um arquivo .nplug?

Um arquivo `.nplug` é um **arquivo ZIP** com um manifesto `plugin.json` obrigatório na raiz:

```json
{
  "packageName": "com.example.myplugin",
  "name": "My Plugin",
  "version": "1.0",
  "description": "O que meu plugin faz"
}
```

- `packageName` — id único do plugin. A instalação de um plugin com `packageName` já existente é recusada.
- `name`, `version`, `description` — exibidos na lista de plugins.

### Partes opcionais do arquivo

- `src/` — código-fonte Java do plugin. A classe principal deve ser `plugin.Main` implementando a interface `PluginEntry` (`void onLoad(Context context)`). Os fontes são compilados **no dispositivo** (ECJ + D8) em `plugin.dex` na próxima inicialização do aplicativo.
- `settings.json` — **array** JSON de configurações para a tela de configuração do plugin. Chaves: `key`, `type`, `title`, `defaultValue`, `summary`, `entries`, `entryValues`, `min`, `max`, `action`, `toast`, `storage`. Tipos suportados: `boolean`, `string`, `list`, `slider`, `button`.

## Instalando um plugin

1. Abra **Configurações → Plugins**.
2. Conceda a permissão **"Desenhar sobre outros aplicativos"** (necessária para os recursos de overlay).
3. Toque em **Importar plugin** e selecione o arquivo `.nplug`.
4. **Reinicie o aplicativo** — os plugins são carregados na inicialização.

## O que um plugin pode fazer

- Assinar eventos do aplicativo através do barramento de eventos: `"Activity.onShow"`, `"Activity.onHide"`, `"Settings.onButtonAction"` (os ouvintes são funções LunoScript).
- Exibir overlays sobre a atividade atual (`PluginOverlayManager`).
- Fornecer uma tela de configuração através de `settings.json`.

### Modo seguro

Se a preferência `force_safe_mode` estiver habilitada, os plugins não são carregados.

## Onde os plugins são armazenados

Plugins instalados ficam em `<armazenamento interno do app>/plugins/<packageName>/`.
