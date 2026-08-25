# NewCatroid Plugins (.nplug)

Plugins let you customize the main app interface and behavior. A plugin is distributed as a `.nplug` file.

> Other languages: [Русский](plugins.ru.md) • [Português (pt-BR)](plugins.pt-BR.md)

## What is a .nplug file?

A `.nplug` file is a **ZIP archive** with a required `plugin.json` manifest at its root:

```json
{
  "packageName": "com.example.myplugin",
  "name": "My Plugin",
  "version": "1.0",
  "description": "What my plugin does"
}
```

- `packageName` — unique id of the plugin. Installing a plugin with an already existing `packageName` is refused.
- `name`, `version`, `description` — shown in the plugins list.

### Optional archive parts

- `src/` — Java sources of the plugin. The main class must be `plugin.Main` implementing the `PluginEntry` interface (`void onLoad(Context context)`). Sources are compiled **on the device** (ECJ + D8) into `plugin.dex` on the next app start.
- `settings.json` — JSON **array** of settings for the plugin's configuration screen. Keys: `key`, `type`, `title`, `defaultValue`, `summary`, `entries`, `entryValues`, `min`, `max`, `action`, `toast`, `storage`. Supported types: `boolean`, `string`, `list`, `slider`, `button`.

## Installing a plugin

1. Open **Settings → Plugins**.
2. Grant the **"Draw over other apps"** permission (required for the overlay features).
3. Tap **Import plugin** and select the `.nplug` file.
4. **Restart the app** — plugins are loaded at startup.

## What a plugin can do

- Subscribe to app events through the event bus: `"Activity.onShow"`, `"Activity.onHide"`, `"Settings.onButtonAction"` (listeners are LunoScript functions).
- Show overlay views on top of the current activity (`PluginOverlayManager`).
- Provide a configuration screen through `settings.json`.

### Safe mode

If the `force_safe_mode` preference is enabled, plugins are not loaded at all.

## Where plugins are stored

Installed plugins live at `<app internal files dir>/plugins/<packageName>/`.
