package org.catrobat.catroid.editor;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.android.AndroidFragmentApplication;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Json;
import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.raptor.GameObject;
import org.catrobat.catroid.raptor.SceneData;
import org.catrobat.catroid.raptor.SceneManager;
import org.catrobat.catroid.raptor.ThreeDManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EditorActivity extends AppCompatActivity implements AndroidFragmentApplication.Callbacks {

    private DrawerLayout drawerLayout;
    private SceneManager sceneManager;
    private InspectorManager inspectorManager;
    private ListView hierarchyListView;
    private ArrayAdapter<String> hierarchyAdapter;
    private final List<GameObject> hierarchyObjects = new ArrayList<>();

    private EditorListener editorListener;
    private Gizmo gizmo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.editor_activity);

        setupToolbarAndDrawer();

        if (savedInstanceState == null) {
            EditorFragment fragment = new EditorFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commitNow();
            this.editorListener = fragment.getListener();
        }

        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    drawerLayout.closeDrawer(GravityCompat.END);
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    public void onEditorReady(SceneManager manager, ThreeDManager TDmanager) {
        this.sceneManager = manager;
        this.threeDManager = TDmanager;
        this.inspectorManager = new InspectorManager(this, sceneManager, threeDManager);

        EditorFragment fragment = (EditorFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment != null) {
            this.editorListener = fragment.getListener();
            this.gizmo = editorListener.getGizmo();
            if (this.gizmo != null) {
                this.inspectorManager.setGizmo(this.gizmo);
            }
        }

        runOnUiThread(this::setupUI);
    }

    public void onObjectSelected(GameObject go) {
        runOnUiThread(() -> {
            if (inspectorManager != null) {
                inspectorManager.populateInspector(go);
            }
            if (go != null && drawerLayout != null && !drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.openDrawer(GravityCompat.END);
            }

            updateHierarchy();
        });
    }

    private void setupToolbarAndDrawer() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);

        drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
                final View mainContent = findViewById(R.id.main_content_coordinator);
                final View leftDrawer = findViewById(R.id.left_drawer);

                if (mainContent != null && drawerView != null) {
                    if (drawerView.getId() == leftDrawer.getId()) {
                        mainContent.setTranslationX(drawerView.getWidth() * slideOffset);
                    } else {
                        mainContent.setTranslationX(-drawerView.getWidth() * slideOffset);
                    }
                }
            }
            @Override
            public void onDrawerOpened(@NonNull View drawerView) {
                drawerView.requestLayout();
            }
            @Override public void onDrawerClosed(@NonNull View drawerView) {}
            @Override public void onDrawerStateChanged(int newState) {}
        });

        toggle.syncState();
    }

    private void setupUI() {
        hierarchyListView = findViewById(R.id.hierarchy_listview);


        if (hierarchyListView == null) {
            Gdx.app.error("EditorActivity", "FATAL: hierarchyListView is null in setupUI!");
            return;
        }

        hierarchyAdapter = new ArrayAdapter<>(this, R.layout.simple_list_item_1_white_text, new ArrayList<>());
        hierarchyListView.setAdapter(hierarchyAdapter);
        hierarchyListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        hierarchyListView.setOnItemClickListener((parent, view, position, id) -> {
            onObjectSelected(hierarchyObjects.get(position));
            drawerLayout.closeDrawer(GravityCompat.START);
        });

        findViewById(R.id.btn_add_empty).setOnClickListener(v -> {
            GameObject newGo = sceneManager.createGameObject("Empty");
            updateAndSelect(newGo);
        });

        updateHierarchy();

        setupCameraButton(R.id.btn_cam_w, 0, 0, 1); // Вперед
        setupCameraButton(R.id.btn_cam_s, 0, 0, -1);  // Назад
        setupCameraButton(R.id.btn_cam_a, -1, 0, 0); // Влево
        setupCameraButton(R.id.btn_cam_d, 1, 0, 0);  // Вправо
        setupCameraButton(R.id.btn_cam_q, 0, -1, 0); // Вниз
        setupCameraButton(R.id.btn_cam_e, 0, 1, 0);  // Вверх

        Button shiftButton = findViewById(R.id.btn_cam_shift);
        shiftButton.setOnTouchListener((v, event) -> {
            if (editorListener == null) return false;

            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                editorListener.onCameraAccelerate(true);
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                editorListener.onCameraAccelerate(false);
            }
            return true;
        });
    }

    private void setupCameraButton(int buttonId, float vx, float vy, float vz) {
        Button button = findViewById(buttonId);
        button.setOnTouchListener((v, event) -> {
            if (editorListener == null) return false;

            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                editorListener.onCameraMove(vx, vy, vz);
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                editorListener.onCameraMove(-vx, -vy, -vz);
            }
            return true;
        });
    }

    private void updateAndSelect(GameObject go) {
        updateHierarchy();
        onObjectSelected(go);
        drawerLayout.closeDrawer(GravityCompat.START);
    }

    public InspectorManager getInspectorManager() {
        return inspectorManager;
    }

    public void updateHierarchy() {
        if (sceneManager == null || hierarchyAdapter == null) return;

        GameObject selectedObject = (inspectorManager != null) ? inspectorManager.getSelectedObject() : null;

        hierarchyAdapter.clear();
        hierarchyObjects.clear();

        List<GameObject> allObjects = new ArrayList<>(sceneManager.getAllGameObjects().values());
        allObjects.sort(Comparator.comparing(go -> go.name.toLowerCase()));

        int selectedIndex = -1;
        for (int i = 0; i < allObjects.size(); i++) {
            GameObject go = allObjects.get(i);
            hierarchyAdapter.add(go.name);
            hierarchyObjects.add(go);
            if (go == selectedObject) {
                selectedIndex = i;
            }
        }
        hierarchyAdapter.notifyDataSetChanged();

        if (hierarchyListView != null && selectedIndex != -1) {
            hierarchyListView.setItemChecked(selectedIndex, true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.editor_tools_menu, menu);
        getMenuInflater().inflate(R.menu.editor_toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (editorListener != null) {
            if (id == R.id.tool_hand) {
                editorListener.setCurrentTool(EditorTool.HAND);
                return true;
            } else if (id == R.id.tool_translate) {
                editorListener.setCurrentTool(EditorTool.TRANSLATE);
                return true;
            } else if (id == R.id.tool_rotate) {
                editorListener.setCurrentTool(EditorTool.ROTATE);
                return true;
            } else if (id == R.id.tool_scale) {
                editorListener.setCurrentTool(EditorTool.SCALE);
                return true;
            }
        }
        if (id == R.id.action_save_scene) {
            showSaveSceneDialog();
            return true;
        } else if (id == R.id.action_load_scene) {
            showLoadSceneDialog();
            return true;
        } else if (id == R.id.action_scene_settings) {
            showSceneSettingsDialog();
            return true;
        } else if (id == R.id.action_clear_scene) {
            EditorStateManager.clearCache();
            if (editorListener != null) {
                editorListener.resetEngine();
            }
            Toast.makeText(this, "Scene Cleared", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_exit) {
            showExitConfirmationDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showExitConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Exit Editor")
                .setMessage("Do you want to save your changes before exiting?")
                .setPositiveButton("Save & Exit", (dialog, which) -> {
                    showSaveSceneDialog(this::finish);
                })
                .setNeutralButton("Exit without Saving", (dialog, which) -> {
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void cacheCurrentScene() {
        if (sceneManager != null) {
            SceneData currentSceneData = sceneManager.getCurrentSceneData();
            Json json = sceneManager.getJson();
            String sceneJson = json.toJson(currentSceneData);
            EditorStateManager.cacheScene(sceneJson);
            Gdx.app.log("EditorActivity", "Scene JSON cached.");
        }
    }

    private void showSaveSceneDialog() {
        showSaveSceneDialog(null);
    }

    private void showSaveSceneDialog(Runnable onSaveComplete) {
        final EditText input = new EditText(this);
        input.setHint("my_level_1");

        new AlertDialog.Builder(this)
                .setTitle("Save Scene As...")
                .setMessage("Enter a file name (without extension):")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String fileName = input.getText().toString();
                    if (fileName.isEmpty()) {
                        Toast.makeText(this, "File name cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String fileNameWithExt = fileName + ".rscene";

                    File projectFile = new File(ProjectManager.getInstance().getCurrentProject().getFilesDir(), fileNameWithExt);
                    FileHandle fileHandle = Gdx.files.absolute(projectFile.getAbsolutePath());

                    sceneManager.saveScene(fileHandle);
                    Toast.makeText(this, "Scene saved to " + fileNameWithExt, Toast.LENGTH_SHORT).show();

                    if (onSaveComplete != null) {
                        onSaveComplete.run();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    private void showLoadSceneDialog() {
        File projectFilesDir = ProjectManager.getInstance().getCurrentProject().getFilesDir();
        File[] allFiles = projectFilesDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".rscene"));

        if (allFiles == null || allFiles.length == 0) {
            Toast.makeText(this, "No saved scenes (.rscene) found.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] sceneNames = new String[allFiles.length];
        for(int i = 0; i < allFiles.length; i++) {
            sceneNames[i] = allFiles[i].getName();
        }

        new AlertDialog.Builder(this)
                .setTitle("Load Scene")
                .setItems(sceneNames, (dialog, which) -> {
                    File selectedFile = allFiles[which];
                    FileHandle fileHandle = Gdx.files.absolute(selectedFile.getAbsolutePath());

                    EditorFragment fragment = (EditorFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                    if (fragment != null && fragment.getListener() != null) {
                        fragment.getListener().resetEngine(fileHandle);

                        updateHierarchy();
                        onObjectSelected(null);
                        Toast.makeText(this, "Loading scene: " + selectedFile.getName(), Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    public void onEngineReset(SceneManager manager, ThreeDManager engine) {
        this.sceneManager = manager;
        this.threeDManager = engine;
        this.inspectorManager = new InspectorManager(this, sceneManager, threeDManager);

        EditorFragment fragment = (EditorFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment != null && fragment.getListener() != null) {
            this.gizmo = fragment.getListener().getGizmo();
            if (this.gizmo != null) {
                this.inspectorManager.setGizmo(this.gizmo);
            }
        }

        runOnUiThread(this::setupUI);
    }

    private int libGdxColorToAndroidColor(com.badlogic.gdx.graphics.Color gdxColor) {
        int r = (int)(gdxColor.r * 255);
        int g = (int)(gdxColor.g * 255);
        int b = (int)(gdxColor.b * 255);
        int a = (int)(gdxColor.a * 255);
        return android.graphics.Color.argb(a, r, g, b);
    }

    private void showSceneSettingsDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_scene_settings, null);
        Button colorButton = dialogView.findViewById(R.id.btn_sky_color);
        SeekBar ambientSeekBar = dialogView.findViewById(R.id.seekbar_ambient_light);
        TextView ambientValueText = dialogView.findViewById(R.id.text_ambient_value);


        float currentIntensity = sceneManager.ambientIntensity;
        com.badlogic.gdx.graphics.Color skyColor = new Color(sceneManager.skyR, sceneManager.skyG, sceneManager.skyB, 1f);
        int initialAndroidColor = libGdxColorToAndroidColor(skyColor);

        colorButton.setBackgroundColor(initialAndroidColor);

        ambientSeekBar.setProgress((int)(currentIntensity * 100));
        ambientValueText.setText(String.format("%.2f", currentIntensity));

        ambientSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float intensity = progress / 100.0f;
                ambientValueText.setText(String.format("%.2f", intensity));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                float intensity = seekBar.getProgress() / 100.0f;
                Gdx.app.postRunnable(() -> sceneManager.setBackgroundLightIntensity(intensity));
            }
        });

        colorButton.setOnClickListener(v -> {
            ColorPickerDialogBuilder
                    .with(this)
                    .setTitle("Choose Sky Color")
                    .initialColor(initialAndroidColor)
                    .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                    .density(12)
                    .setPositiveButton("OK", (dialog, selectedColor, allColors) -> {
                        colorButton.setBackgroundColor(selectedColor);

                        int a = android.graphics.Color.alpha(selectedColor);
                        int r = android.graphics.Color.red(selectedColor);
                        int g = android.graphics.Color.green(selectedColor);
                        int b = android.graphics.Color.blue(selectedColor);

                        final float libgdx_r = r / 255f;
                        final float libgdx_g = g / 255f;
                        final float libgdx_b = b / 255f;

                        Gdx.app.postRunnable(() -> sceneManager.setSkyColor(libgdx_r, libgdx_g, libgdx_b));})
                    .setNegativeButton("Cancel", null)
                    .build()
                    .show();
        });

        new AlertDialog.Builder(this)
                .setTitle("Scene Settings")
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .show();
    }

    private ThreeDManager threeDManager;

    @Override
    protected void onPause() {
        cacheCurrentScene();
        super.onPause();
    }

    @Override
    public void exit() {}
}