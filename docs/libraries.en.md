# NewCatroid Libraries (.newlib)

A library lets you create your own **bricks** and **formulas** for NewCatroid projects using [LunoScript](https://github.com/Danveyd/LunoScript). Libraries are distributed as `.newlib` files.

> Other languages: [Русский](libraries.ru.md) • [Português (pt-BR)](libraries.pt-BR.md)

## What is a .newlib file?

A `.newlib` file is a **ZIP archive** containing up to three entries:

| Entry | Content | Required |
|---|---|---|
| `code.txt` | LunoScript source code with your `fun` definitions | yes |
| `formulas.xml` | Custom formula definitions | no |
| `bricks.xml` | Custom brick definitions | no |

### code.txt

Plain LunoScript. Every formula and brick points to a function defined here. The default editor template looks like this:

```lunoscript
fun sum(a, b) {
    return Number(a) + Number(b);
}

fun toast(sprite, text) {
    MakeToast("Text is: " + String(text));
}
```

Native LunoScript helpers such as `Number()` and `MakeToast()` are available out of the box.

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

- `id` — unique identifier.
- `function` — name of a `fun` defined in `code.txt`.
- `displayName` — name shown in the formula editor.
- Each `<param>` has `type` (`NUMBER`, `STRING`, `USER_VARIABLE`, `USER_LIST`) and `default` (default value used in the editor).

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

- `id` — unique identifier.
- `function` — name of a `fun` defined in `code.txt`.
- `header` — text shown on the brick; use `{0}`, `{1}`, ... as placeholders for parameters.
- Each `<param>` has `type` (currently only `TEXT_FIELD` is honored) and `name` (the internal name passed to the LunoScript function).

## Using a library in a project

1. Open a project and go to its **files → Libs** section.
2. Copy / select the `.newlib` file into the project's `libs` folder.
3. The library is loaded automatically.
4. Your custom bricks appear in the **Libraries** category of the brick palette.
5. Your custom formulas appear under the **Libraries** header in the formula editor.

Under the hood, each library runs in its own LunoScript interpreter, and the library file name (including the `.newlib` extension) is used as the library id.

## Creating a library

Use the built-in library editor (menu item **Create library** in the project list). It has three tabs: **Code**, **Formulas** and **Bricks**.

- **Code** — LunoScript editor with syntax highlighting and live syntax-error detection.
- **Formulas** — add formulas (unique id, display name, function name from code, parameters with type and default value). All fields are required and the function must exist in `code.txt` (`fun <name>`).
- **Bricks** — add bricks (unique id, header text such as `Show {0}`, function name from code, parameters with type and internal name). Bricks support up to 8 parameters.

Use **Export** to save the current library as a `.newlib` file, **Import** to load one, and **Clear** to reset the editor session.

> Note: the library editor autosaves your session locally (`last_session.json`), but this autosave is only used by the editor — the exported `.newlib` archive contains only `code.txt`, `formulas.xml` and `bricks.xml`.

## Where libraries are stored

- Project libraries: `<app internal files dir>/<project name>/libs/`
- Editor autosave: `<app internal files dir>/library_drafts/last_session.json`
