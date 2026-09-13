
package org.catrobat.catroid.ui.settingsfragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreferenceCompat;

import org.catrobat.catroid.R;
import org.catrobat.catroid.plugins.PluginEventBus;
import org.catrobat.catroid.plugins.PluginInfo;
import org.catrobat.catroid.plugins.PluginManager;
import org.catrobat.catroid.utils.ThemeEngine;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

public class PluginSettingsFragment extends PreferenceFragmentCompat {

    public static final String SHARED_PREFS_PREFIX = "plugin_settings_";

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        String packageName = getArguments().getString("plugin_package_name");
        if (packageName == null) return;

        PluginManager pluginManager = PluginManager.getInstance(getContext());
        PluginInfo plugin = pluginManager.getPluginByPackageName(packageName);
        if (plugin == null || !plugin.hasSettings()) return;

        boolean isThemePlugin = hasSharedSettings(plugin);


        String prefsName = isThemePlugin ? ThemeEngine.THEME_PREFS_NAME : SHARED_PREFS_PREFIX + packageName;
        getPreferenceManager().setSharedPreferencesName(prefsName);

        PreferenceScreen preferenceScreen = getPreferenceManager().createPreferenceScreen(getContext());
        setPreferenceScreen(preferenceScreen);

        try {
            FileInputStream fis = new FileInputStream(plugin.settingsFile);
            byte[] data = new byte[(int) plugin.settingsFile.length()];
            fis.read(data);
            fis.close();
            String json = new String(data, StandardCharsets.UTF_8);
            JSONArray settingsArray = new JSONArray(json);

            for (int i = 0; i < settingsArray.length(); i++) {
                JSONObject setting = settingsArray.getJSONObject(i);
                String key = setting.getString("key");
                String type = setting.getString("type");
                String title = setting.getString("title");

                Preference preference = null;

                switch (type) {
                    case "boolean":
                        SwitchPreferenceCompat switchPref = new SwitchPreferenceCompat(getContext());
                        switchPref.setDefaultValue(setting.optBoolean("defaultValue", false));
                        preference = switchPref;
                        break;
                    case "string":
                        EditTextPreference editPref = new EditTextPreference(getContext());
                        editPref.setDefaultValue(setting.optString("defaultValue", ""));
                        preference = editPref;
                        break;
                    case "list":
                        ListPreference listPref = new ListPreference(getContext());
                        JSONArray entriesJson = setting.getJSONArray("entries");
                        JSONArray valuesJson = setting.getJSONArray("entryValues");
                        CharSequence[] entries = new CharSequence[entriesJson.length()];
                        CharSequence[] entryValues = new CharSequence[valuesJson.length()];
                        for (int j=0; j<entriesJson.length(); j++) {
                            entries[j] = entriesJson.getString(j);
                            entryValues[j] = valuesJson.getString(j);
                        }
                        listPref.setEntries(entries);
                        listPref.setEntryValues(entryValues);
                        listPref.setDefaultValue(setting.optString("defaultValue", ""));
                        preference = listPref;
                        break;
                    case "slider":
                        SeekBarPreference seekBarPref = new SeekBarPreference(getContext());
                        seekBarPref.setDefaultValue(setting.optInt("defaultValue", 50));
                        seekBarPref.setMin(setting.optInt("min", 0));
                        seekBarPref.setMax(setting.optInt("max", 100));
                        seekBarPref.setShowSeekBarValue(true);
                        preference = seekBarPref;
                        break;
                    case "button":
                        Preference button = new Preference(getContext());
                        button.setOnPreferenceClickListener(pref -> {
                            String action = setting.optString("action", "");
                            if (!action.isEmpty()) {
                                Log.d("PluginSettings", "Button clicked, dispatching action: " + action);
                                PluginEventBus.getInstance().dispatch("Settings.onButtonAction", packageName, action);
                                Toast.makeText(getContext(), setting.optString("toast", getString(R.string.psf_action_done)), Toast.LENGTH_SHORT).show();
                            }
                            return true;
                        });
                        preference = button;
                        break;
                }

                if (preference != null) {
                    preference.setKey(key);
                    preference.setTitle(title);
                    preference.setSummary(setting.optString("summary", ""));

                    getPreferenceScreen().addPreference(preference);
                }
            }
        } catch (Exception e) {
            Log.e("PluginSettingsFragment", "ERROR", e);
        }
    }

    private boolean hasSharedSettings(PluginInfo plugin) {
        try {
            FileInputStream fis = new FileInputStream(plugin.settingsFile);
            byte[] data = new byte[(int) plugin.settingsFile.length()];
            fis.read(data);
            fis.close();
            String json = new String(data, StandardCharsets.UTF_8);
            JSONArray settingsArray = new JSONArray(json);
            for (int i = 0; i < settingsArray.length(); i++) {
                if ("shared".equals(settingsArray.getJSONObject(i).optString("storage"))) {
                    return true;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();

        String packageName = getArguments().getString("plugin_package_name");
        PluginInfo plugin = PluginManager.getInstance(getContext()).getPluginByPackageName(packageName);
        if (plugin != null) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(getString(R.string.psf_settings_for, plugin.name));
        }
    }
}