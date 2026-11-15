# Bibliotecas do NewCatroid (.newlib)

Uma biblioteca permite criar seus próprios **blocos** e **fórmulas** para projetos NewCatroid usando [LunoScript](https://github.com/Danveyd/LunoScript). As bibliotecas são distribuídas como arquivos `.newlib`.

> Outros idiomas: [English](libraries.en.md) • [Русский](libraries.ru.md)

## O que é um arquivo .newlib?

Um arquivo `.newlib` é um **arquivo ZIP** contendo até três entradas:

| Entrada | Conteúdo | Obrigatória |
|---|---|---|
| `code.txt` | Código-fonte LunoScript com suas definições de `fun` | sim |
| `formulas.xml` | Definições de fórmulas personalizadas | não |
| `bricks.xml` | Definições de blocos personalizados | não |

### code.txt

LunoScript puro. Cada fórmula e cada bloco aponta para uma função definida aqui. O modelo padrão do editor é assim:

```lunoscript
fun sum(a, b) {
    return Number(a) + Number(b);
}

fun toast(sprite, text) {
    MakeToast("Text is: " + String(text));
}
```

Funções nativas do LunoScript, como `Number()` e `MakeToast()`, já estão disponíveis.

### formulas.xml

```xml
<formulas>
  <formula id="my_formula" function="sum" displayName="Sum">
    <params>
      <param type="NUMBER" default="0"/>
    </params>
  </formula>
</formulas>
```

- `id` — identificador único.
- `function` — nome de um `fun` definido em `code.txt`.
- `displayName` — nome exibido no editor de fórmulas.
- Cada `<param>` tem `type` (`NUMBER`, `STRING`, `USER_VARIABLE`, `USER_LIST`) e `default` (valor padrão usado no editor).

### bricks.xml

```xml
<bricks>
  <brick id="my_brick" function="toast" header="Show {0}">
    <params>
      <param type="TEXT_FIELD" name="text"/>
    </params>
  </brick>
</bricks>
```

- `id` — identificador único.
- `function` — nome de um `fun` definido em `code.txt`.
- `header` — texto exibido no bloco; use `{0}`, `{1}`, ... como espaços reservados para os parâmetros.
- Cada `<param>` tem `type` (atualmente apenas `TEXT_FIELD` é usado) e `name` (o nome interno passado para a função LunoScript).

## Usando uma biblioteca em um projeto

1. Abra um projeto e vá para a seção **arquivos → Bibliotecas**.
2. Copie / selecione o arquivo `.newlib` para a pasta `libs` do projeto.
3. A biblioteca é carregada automaticamente.
4. Seus blocos personalizados aparecem na categoria **Bibliotecas** da paleta de blocos.
5. Suas fórmulas personalizadas aparecem no editor de fórmulas sob o cabeçalho **Bibliotecas**.

Por baixo dos panos, cada biblioteca roda em seu próprio interpretador LunoScript e o nome do arquivo (incluindo a extensão `.newlib`) é usado como id da biblioteca.

## Criando uma biblioteca

Use o editor de bibliotecas integrado (item de menu **Criar biblioteca** na lista de projetos). Ele tem três abas: **Código**, **Fórmulas** e **Blocos**.

- **Código** — editor LunoScript com destaque de sintaxe e detecção de erros em tempo real.
- **Fórmulas** — adiciona fórmulas (id único, nome de exibição, nome da função no código, parâmetros com tipo e valor padrão). Todos os campos são obrigatórios e a função deve existir em `code.txt` (`fun <nome>`).
- **Blocos** — adiciona blocos (id único, texto do cabeçalho como `Show {0}`, nome da função no código, parâmetros com tipo e nome interno). Os blocos suportam até 8 parâmetros.

Use **Exportar** para salvar a biblioteca atual como um arquivo `.newlib`, **Importar** para carregar um e **Limpar** para redefinir a sessão do editor.

> Observação: o editor de bibliotecas salva a sessão localmente (`last_session.json`), mas esse salvamento é usado apenas pelo editor — o arquivo `.newlib` exportado contém somente `code.txt`, `formulas.xml` e `bricks.xml`.

## Onde as bibliotecas são armazenadas

- Bibliotecas do projeto: `<armazenamento interno do app>/<nome do projeto>/libs/`
- Autosave do editor: `<armazenamento interno do app>/library_drafts/last_session.json`
