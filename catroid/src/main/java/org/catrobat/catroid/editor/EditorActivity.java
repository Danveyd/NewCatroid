package org.catrobat.catroid.editor;

import android.annotation.SuppressLint;
import androidx.appcompat.app.AlertDialog;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.hardware.input.InputManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.GestureDetector;
import android.view.InputDevice;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GestureDetectorCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.android.AndroidFragmentApplication;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class EditorActivity extends AppCompatActivity implements AndroidFragmentApplication.Callbacks, HierarchyAdapter.OnItemClickListener {

    private DrawerLayout drawerLayout;
    private SceneManager sceneManager;
    private InspectorManager inspectorManager;
    private ListView hierarchyListView;
    private RecyclerView hierarchyRecyclerView;
    private HierarchyAdapter hierarchyAdapter;
    private final List<HierarchyAdapter.HierarchyItem> hierarchyItems = new ArrayList<>();
    private final List<GameObject> hierarchyObjects = new ArrayList<>();

    public EditorListener editorListener;
    private Gizmo gizmo;

    private ItemTouchHelper touchHelper;

    private GameObject currentSelectedObject = null;
    private View quickActions;
    private UndoManager undoManager;

    private Thread.UncaughtExceptionHandler defaultCrashHandler;
    private static final String AUTOSAVE_FILE_NAME = "_recovery_autosave.rscene";

    private final InputManager.InputDeviceListener inputDeviceListener = new InputManager.InputDeviceListener() {
        @Override
        public void onInputDeviceAdded(int deviceId) {
            runOnUiThread(() -> updateCameraControlsVisibility());
        }

        @Override
        public void onInputDeviceRemoved(int deviceId) {
            runOnUiThread(() -> updateCameraControlsVisibility());
        }

        @Override
        public void onInputDeviceChanged(int deviceId) {
            runOnUiThread(() -> updateCameraControlsVisibility());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupCrashHandler();
        setContentView(R.layout.editor_activity);

        setupToolbarAndDrawer();

        if (savedInstanceState == null) {
            EditorFragment fragment = new EditorFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commitNow();
            this.editorListener = fragment.getListener();
        }

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
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

    @Override
    protected void onStart() {
        super.onStart();
        InputManager inputManager = (InputManager) getSystemService(Context.INPUT_SERVICE);
        if (inputManager != null) {
            inputManager.registerInputDeviceListener(inputDeviceListener, null);
        }
        updateCameraControlsVisibility();
    }

    @Override
    protected void onStop() {
        super.onStop();
        InputManager inputManager = (InputManager) getSystemService(Context.INPUT_SERVICE);
        if (inputManager != null) {
            inputManager.unregisterInputDeviceListener(inputDeviceListener);
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateCameraControlsVisibility();
    }

    public void onEditorReady(SceneManager manager, ThreeDManager TDmanager) {
        this.sceneManager = manager;
        this.threeDManager = TDmanager;
        this.inspectorManager = new InspectorManager(this, sceneManager, threeDManager);

        this.undoManager = new UndoManager(this);

        findViewById(R.id.btn_undo).setOnClickListener(v -> {
            undoManager.undo();
            if (editorListener != null && editorListener.getGizmo() != null) {
                onObjectSelected(currentSelectedObject, false);
            }
        });

        findViewById(R.id.btn_redo).setOnClickListener(v -> {
            undoManager.redo();
            if (editorListener != null) {
                onObjectSelected(currentSelectedObject, false);
            }
        });

        EditorFragment fragment = (EditorFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment != null) {
            this.editorListener = fragment.getListener();
            this.gizmo = editorListener.getGizmo();
            if (this.gizmo != null) {
                this.inspectorManager.setGizmo(this.gizmo);
            }
        }

        runOnUiThread(() -> {
            setupUI();

            checkForRecovery();
        });
    }

    public void onObjectSelected(GameObject go) {
        onObjectSelected(go, true);
    }

    public void onObjectSelected(GameObject go, boolean showInspector) {
        runOnUiThread(() -> {
            if (inspectorManager != null && currentSelectedObject != go) {
                inspectorManager.populateInspector(go);
                currentSelectedObject = go;
            }

            quickActions.setVisibility(go != null ? View.VISIBLE : View.GONE);

            if (go != null && showInspector && drawerLayout != null) {
                drawerLayout.openDrawer(GravityCompat.END);
            }

            if (hierarchyAdapter != null) {
                hierarchyAdapter.setSelectedObject(go);
            }
            if (editorListener.getGizmo() != null) {
                editorListener.getGizmo().setSelectedObject(go);
            }
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
        hierarchyRecyclerView = findViewById(R.id.hierarchy_recyclerview);
        DraggableLinearLayoutManager layoutManager = new DraggableLinearLayoutManager(this);
        hierarchyRecyclerView.setLayoutManager(layoutManager);
        hierarchyAdapter = new HierarchyAdapter(this);
        hierarchyRecyclerView.setAdapter(hierarchyAdapter);

        quickActions = findViewById(R.id.quick_actions_panel);


        ItemTouchHelper.Callback callback = new HierarchyDragCallback(
                hierarchyAdapter, sceneManager, this, layoutManager);

        this.touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(hierarchyRecyclerView);

        updateCameraControlsVisibility();

        hierarchyRecyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {

            private final GestureDetectorCompat gestureDetector = new GestureDetectorCompat(hierarchyRecyclerView.getContext(),
                    new GestureDetector.SimpleOnGestureListener() {
                        @Override
                        public void onLongPress(MotionEvent e) {

                            View child = hierarchyRecyclerView.findChildViewUnder(e.getX(), e.getY());
                            if (child != null) {
                                RecyclerView.ViewHolder vh = hierarchyRecyclerView.getChildViewHolder(child);
                                if (vh != null) {


                                    drawerLayout.requestDisallowInterceptTouchEvent(true);

                                    touchHelper.startDrag(vh);
                                }
                            }
                        }
                    });

            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {

                gestureDetector.onTouchEvent(e);


                if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL) {
                    drawerLayout.requestDisallowInterceptTouchEvent(false);
                }

                return false;
            }

            @Override
            public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {}

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {}
        });

        findViewById(R.id.btn_add_empty).setOnClickListener(v -> {
            GameObject newGo = sceneManager.createGameObject("Empty");
            updateAndSelect(newGo);
        });

        findViewById(R.id.btn_add_cube).setOnClickListener(v -> {
            GameObject newGo = sceneManager.createPrimitive("cube");
            if (newGo != null) {
                updateAndSelect(newGo);
            }
        });

        findViewById(R.id.btn_add_sphere).setOnClickListener(v -> {
            GameObject newGo = sceneManager.createPrimitive("sphere");
            if (newGo != null) {
                updateAndSelect(newGo);
            }
        });

        updateHierarchy();

        setupCameraButton(R.id.btn_cam_w, 0, 0, 1);
        setupCameraButton(R.id.btn_cam_s, 0, 0, -1);
        setupCameraButton(R.id.btn_cam_a, -1, 0, 0);
        setupCameraButton(R.id.btn_cam_d, 1, 0, 0);
        setupCameraButton(R.id.btn_cam_q, 0, -1, 0);
        setupCameraButton(R.id.btn_cam_e, 0, 1, 0);

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

        EditText searchBar = findViewById(R.id.hierarchy_search);
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (hierarchyAdapter != null) {
                    hierarchyAdapter.filter(s.toString());
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        findViewById(R.id.btn_quick_inspector).setOnClickListener(v -> {
            if (currentSelectedObject != null) {
                drawerLayout.openDrawer(GravityCompat.END);
            }
        });

        ImageButton btnGridSnap = findViewById(R.id.btn_quick_grid_snap);

        btnGridSnap.setOnClickListener(v -> {
            if (gizmo != null) {
                gizmo.snapEnabled = !gizmo.snapEnabled;
                int color = gizmo.snapEnabled ? android.graphics.Color.GREEN : android.graphics.Color.WHITE;
                btnGridSnap.setColorFilter(color);
                Toast.makeText(this, gizmo.snapEnabled ? "Grid Snap: ON (" + gizmo.snapSize + "m)" : "Grid Snap: OFF", Toast.LENGTH_SHORT).show();
            }
        });

        btnGridSnap.setOnLongClickListener(v -> {
            final EditText input = new EditText(this);
            input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
            input.setHint(R.string.editor_3d_grid_snap_hint);

            if (gizmo != null) {
                input.setText(String.valueOf(gizmo.snapSize));
                input.setSelection(input.getText().length());
            }

            android.widget.FrameLayout container = new android.widget.FrameLayout(this);
            int paddingPx = (int) (20 * getResources().getDisplayMetrics().density);
            container.setPadding(paddingPx, paddingPx / 2, paddingPx, 0);
            container.addView(input);

            new AlertDialog.Builder(this, R.style.Theme_NewCatroid_Dialog)
                    .setTitle(R.string.editor_3d_grid_snap_dialog_title)
                    .setMessage(R.string.editor_3d_grid_snap_dialog_msg)
                    .setView(container)
                    .setPositiveButton(R.string.apply, (dialog, which) -> {
                        String inputText = input.getText().toString().trim();
                        try {
                            float newSize = Float.parseFloat(inputText);
                            if (newSize <= 0f) {
                                Toast.makeText(this, R.string.editor_3d_grid_snap_error_positive, Toast.LENGTH_SHORT).show();
                                return;
                            }

                            if (gizmo != null) {
                                gizmo.snapSize = newSize;
                                gizmo.snapEnabled = true;
                                btnGridSnap.setColorFilter(android.graphics.Color.GREEN);
                                Toast.makeText(this, getString(R.string.editor_3d_grid_snap_success, String.valueOf(newSize)), Toast.LENGTH_SHORT).show();
                            }
                        } catch (NumberFormatException e) {
                            Toast.makeText(this, R.string.editor_3d_grid_snap_error_number, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();

            return true;
        });

        findViewById(R.id.btn_mass_delete).setOnClickListener(v -> {
            String query = ((EditText)findViewById(R.id.hierarchy_search)).getText().toString().trim();
            if (query.isEmpty()) return;

            new AlertDialog.Builder(this, R.style.Theme_NewCatroid_Dialog)
                    .setTitle(R.string.editor_3d_mass_delete_title)
                    .setMessage(getString(R.string.editor_3d_mass_delete_msg, query))
                    .setPositiveButton(R.string.editor_3d_delete_all, (dialog, which) -> {
                        Gdx.app.postRunnable(() -> {
                            Commands.CompositeCommand compositeDelete = new Commands.CompositeCommand();
                            List<GameObject> toRemove = new ArrayList<>();

                            for (GameObject go : sceneManager.getAllGameObjects().values()) {
                                if (go.name.toLowerCase().contains(query.toLowerCase())) {
                                    toRemove.add(go);
                                }
                            }

                            for (GameObject go : toRemove) {
                                compositeDelete.addCommand(new Commands.DeleteCommand(sceneManager, go));
                                sceneManager.removeGameObject(go);
                            }

                            if (!compositeDelete.isEmpty()) {
                                undoManager.pushCommand(compositeDelete);
                            }

                            runOnUiThread(() -> {
                                updateHierarchy();
                                onObjectSelected(null, false);
                                Toast.makeText(this, getString(R.string.editor_3d_removed_objects_toast, toRemove.size()), Toast.LENGTH_SHORT).show();
                            });
                        });
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });

        findViewById(R.id.btn_quick_rotate_to_cam).setOnClickListener(v -> {
            if (currentSelectedObject != null && threeDManager != null) {
                Vector3 startPos = new Vector3(currentSelectedObject.transform.position);
                Quaternion startRot = new Quaternion(currentSelectedObject.transform.rotation);
                Vector3 startScale = new Vector3(currentSelectedObject.transform.scale);

                Vector3 camDir = threeDManager.getCamera().direction.cpy();
                currentSelectedObject.transform.rotation.setFromCross(new Vector3(0,0,-1), camDir);
                sceneManager.rebuildGameObject(currentSelectedObject);

                if (undoManager != null) {
                    undoManager.pushCommand(new Commands.TransformCommand(
                            sceneManager, currentSelectedObject, startPos, startRot, startScale
                    ));
                }

                Toast.makeText(this, R.string.editor_3d_aligned_to_cam, Toast.LENGTH_SHORT).show();
                onObjectSelected(currentSelectedObject, false);
            }
        });

        findViewById(R.id.btn_quick_duplicate).setOnLongClickListener(v -> {
            if (currentSelectedObject == null) return false;

            View viewDialog = getLayoutInflater().inflate(R.layout.dialog_array_duplicate, null);
            EditText inputAmount = viewDialog.findViewById(R.id.edit_array_count);
            EditText inputDist = viewDialog.findViewById(R.id.edit_array_step);
            Spinner spinAxis = viewDialog.findViewById(R.id.spinner_array_axis);

            String[] axesOptions = {
                    getString(R.string.editor_3d_axis_x),
                    getString(R.string.editor_3d_axis_y),
                    getString(R.string.editor_3d_axis_z),
                    getString(R.string.editor_3d_axis_cam_forward)
            };
            ArrayAdapter<String> spinAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, axesOptions);
            spinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinAxis.setAdapter(spinAdapter);

            new AlertDialog.Builder(this, R.style.Theme_NewCatroid_Dialog)
                    .setView(viewDialog)
                    .setPositiveButton(R.string.editor_3d_clone, (dialog, which) -> {
                        try {
                            int count = Integer.parseInt(inputAmount.getText().toString());
                            float step = Float.parseFloat(inputDist.getText().toString());
                            int axisIdx = spinAxis.getSelectedItemPosition();
                            GameObject source = currentSelectedObject;

                            Gdx.app.postRunnable(() -> {
                                Commands.CompositeCommand bulkAdd = new Commands.CompositeCommand();
                                Vector3 offset = new Vector3();

                                if (axisIdx == 0) offset.set(step, 0, 0);
                                else if (axisIdx == 1) offset.set(0, step, 0);
                                else if (axisIdx == 2) offset.set(0, 0, step);
                                else offset.set(threeDManager.getCamera().direction).scl(step);

                                GameObject lastAdded = source;
                                for (int i = 0; i < count; i++) {
                                    GameObject copy = sceneManager.cloneGameObject(lastAdded, null);
                                    if (copy != null) {
                                        copy.transform.position.add(offset);
                                        sceneManager.updateWorldTransforms();

                                        bulkAdd.addCommand(new Commands.AddCommand(sceneManager, copy));
                                        lastAdded = copy;
                                    }
                                }

                                if (!bulkAdd.isEmpty()) {
                                    undoManager.pushCommand(bulkAdd);
                                }

                                runOnUiThread(() -> {
                                    updateHierarchy();
                                    Toast.makeText(this, getString(R.string.editor_3d_created_objects_toast, count), Toast.LENGTH_SHORT).show();
                                });
                            });

                        } catch (Exception e) {
                            Toast.makeText(this, R.string.editor_3d_error_inputs, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            return true;
        });

        findViewById(R.id.btn_quick_move_to_cam).setOnClickListener(v -> {
            if (currentSelectedObject != null && threeDManager != null) {
                Camera cam = threeDManager.getCamera();
                Vector3 newPos = cam.position.cpy();

                currentSelectedObject.transform.position.set(newPos);
                sceneManager.rebuildGameObject(currentSelectedObject);

                Toast.makeText(this, R.string.editor_3d_moved_to_view, Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btn_quick_duplicate).setOnClickListener(v -> {
            GameObject original = currentSelectedObject;
            if (original != null) {
                GameObject copy = sceneManager.cloneGameObject(original, null);
                if (copy != null && copy.transform != null) {
                    sceneManager.updateWorldTransforms();

                    if (undoManager != null) {
                        undoManager.pushCommand(new Commands.AddCommand(sceneManager, copy));
                    }

                    updateHierarchy();
                    onObjectSelected(copy, false);
                    Toast.makeText(this, R.string.editor_3d_duplicated, Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.btn_quick_focus).setOnClickListener(v -> {
            if (currentSelectedObject != null && threeDManager != null) {
                Vector3 pos = currentSelectedObject.transform.worldTransform.getTranslation(new Vector3());
                Camera cam = threeDManager.getCamera();

                cam.position.set(pos.x + 7, pos.y + 7, pos.z + 7);
                cam.up.set(0, 1, 0);
                cam.lookAt(pos);

                cam.update();

                Toast.makeText(this, getString(R.string.editor_3d_focused_on, currentSelectedObject.name), Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btn_quick_delete).setOnClickListener(v -> {
            if (currentSelectedObject != null) {
                undoManager.pushCommand(new Commands.DeleteCommand(sceneManager, currentSelectedObject));
                sceneManager.removeGameObject(currentSelectedObject);
                onObjectSelected(null);
                updateHierarchy();
            }
        });
    }

    @Override
    public void onItemClick(GameObject gameObject) {
        gizmo.setSelectedObject(gameObject);
        onObjectSelected(gameObject);
        drawerLayout.closeDrawer(GravityCompat.START);
    }

    private boolean hasPhysicalKeyboardOrMouse() {
        Configuration config = getResources().getConfiguration();
        boolean isHardKeyboardActive = (config.keyboard == Configuration.KEYBOARD_QWERTY)
                && (config.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO);

        if (isHardKeyboardActive) {
            return true;
        }

        InputManager inputManager = (InputManager) getSystemService(Context.INPUT_SERVICE);
        if (inputManager != null) {
            int[] deviceIds = inputManager.getInputDeviceIds();
            for (int id : deviceIds) {
                InputDevice device = inputManager.getInputDevice(id);
                if (device == null || device.isVirtual()) continue;

                int sources = device.getSources();

                boolean isPhysicalKeyboard = (sources & InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD
                        && device.getKeyboardType() == InputDevice.KEYBOARD_TYPE_ALPHABETIC;

                boolean isMouse = (sources & InputDevice.SOURCE_MOUSE) == InputDevice.SOURCE_MOUSE
                        || (sources & InputDevice.SOURCE_TRACKBALL) == InputDevice.SOURCE_TRACKBALL;

                if (isPhysicalKeyboard || isMouse) {
                    return true;
                }
            }
        }

        return false;
    }

    public void updateCameraControlsVisibility() {
        boolean pcModeEnabled = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("pref_pc_mode_enabled", false);

        boolean shouldHideButtons = pcModeEnabled && hasPhysicalKeyboardOrMouse();

        int visibility = shouldHideButtons ? View.GONE : View.VISIBLE;

        View cameraControls = findViewById(R.id.camera_controls);
        View btnShift = findViewById(R.id.btn_cam_shift);

        if (cameraControls != null) cameraControls.setVisibility(visibility);
        if (btnShift != null) btnShift.setVisibility(visibility);
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

        hierarchyItems.clear();


        List<GameObject> rootObjects = new ArrayList<>();
        for (GameObject go : sceneManager.getAllGameObjects().values()) {
            if (go.parentId == null) {
                rootObjects.add(go);
            }
        }
        rootObjects.sort(Comparator.comparing(go -> go.name.toLowerCase()));


        for (GameObject root : rootObjects) {
            addGameObjectToHierarchyList(root, 0);
        }


        hierarchyAdapter.updateData(hierarchyItems);
    }

    private void addGameObjectToHierarchyList(GameObject go, int depth) {
        String prefix = String.join("", Collections.nCopies(depth, "    "));
        String displayName = prefix + go.name;

        hierarchyItems.add(new HierarchyAdapter.HierarchyItem(go, displayName));

        if (!go.childrenIds.isEmpty()) {
            List<GameObject> children = new ArrayList<>();
            for (String childId : go.childrenIds) {
                GameObject child = sceneManager.findGameObject(childId);
                if (child != null) children.add(child);
            }
            children.sort(Comparator.comparing(child -> child.name.toLowerCase()));

            for (GameObject child : children) {
                addGameObjectToHierarchyList(child, depth + 1);
            }
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
            new AlertDialog.Builder(this, R.style.Theme_NewCatroid_Dialog)
                    .setTitle(R.string.editor_3d_clear_scene_title)
                    .setMessage(R.string.editor_3d_clear_scene_msg)
                    .setPositiveButton(R.string.editor_3d_clear_btn, (dialog, which) -> {
                        EditorStateManager.clearCache();
                        if (editorListener != null) {
                            editorListener.resetEngine();
                        }
                        Toast.makeText(this, R.string.editor_3d_cleared_toast, Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            return true;
        } else if (id == R.id.action_exit) {
            showExitConfirmationDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
        TextView skyboxPathText = dialogView.findViewById(R.id.text_skybox_path);
        Button selectSkyboxButton = dialogView.findViewById(R.id.btn_select_skybox);
        ImageButton clearSkyboxButton = dialogView.findViewById(R.id.btn_clear_skybox);

        EditText shadowDistEdit = dialogView.findViewById(R.id.edit_shadow_distance);
        android.widget.Spinner shadowResSpinner = dialogView.findViewById(R.id.spinner_shadow_resolution);
        Button applyShadowsBtn = dialogView.findViewById(R.id.btn_apply_shadows);

        android.widget.CheckBox csmCheckBox = dialogView.findViewById(R.id.checkbox_csm);
        android.view.View csmLayout = dialogView.findViewById(R.id.layout_csm_factor);
        EditText csmFactorEdit = dialogView.findViewById(R.id.edit_csm_factor);
        android.widget.TextView csmHintText = dialogView.findViewById(R.id.text_csm_hint);


        String[] resolutions = {"2", "64", "128", "256", "512", "1024", "2048", "4096", "8192", "16384", "32768"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.simple_spinner_item_white_text, resolutions);
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_white_text);
        shadowResSpinner.setAdapter(adapter);


        if (threeDManager != null) {
            shadowDistEdit.setText(String.valueOf(threeDManager.getShadowSize()));
            shadowDistEdit.setText(String.valueOf(threeDManager.getShadowSize()));
            csmCheckBox.setChecked(threeDManager.isCSMEnabled());
            csmFactorEdit.setText(String.valueOf(threeDManager.getCsmSplitFactor()));

            String currentRes = String.valueOf((int) threeDManager.getShadowResolution());
            for (int i = 0; i < resolutions.length; i++) {
                if (resolutions[i].equals(currentRes)) {
                    shadowResSpinner.setSelection(i);
                    break;
                }
            }
        }

        Runnable updateCsmVisibility = () -> {
            boolean checked = csmCheckBox.isChecked();
            csmLayout.setVisibility(checked ? View.VISIBLE : View.GONE);
            csmHintText.setVisibility(checked ? View.VISIBLE : View.GONE);
        };
        updateCsmVisibility.run();
        csmCheckBox.setOnCheckedChangeListener((btn, isChecked) -> updateCsmVisibility.run());

        applyShadowsBtn.setOnClickListener(v -> {
            try {
                float size = Float.parseFloat(shadowDistEdit.getText().toString());
                int res = Integer.parseInt(shadowResSpinner.getSelectedItem().toString());
                boolean enableCsm = csmCheckBox.isChecked();
                float csmFactor = Float.parseFloat(csmFactorEdit.getText().toString());

                if (threeDManager != null) {
                    threeDManager.setShadowSettings(size, res, enableCsm, csmFactor);

                    if (sceneManager != null && sceneManager.getCurrentSceneData() != null) {
                        sceneManager.getCurrentSceneData().useCSM = enableCsm;
                        sceneManager.getCurrentSceneData().csmSplitFactor = csmFactor;
                    }

                    Toast.makeText(this, "Shadow settings applied.", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Invalid shadow values.", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        });


        float currentIntensity = sceneManager.ambientIntensity;
        com.badlogic.gdx.graphics.Color skyColor = new Color(sceneManager.skyR, sceneManager.skyG, sceneManager.skyB, 1f);
        int initialAndroidColor = libGdxColorToAndroidColor(skyColor);

        colorButton.setBackgroundColor(initialAndroidColor);

        skyboxPathText.setText(sceneManager.skyboxPath != null && !sceneManager.skyboxPath.isEmpty() ? sceneManager.skyboxPath : "None");
        clearSkyboxButton.setVisibility(sceneManager.skyboxPath != null && !sceneManager.skyboxPath.isEmpty() ? View.VISIBLE : View.GONE);

        selectSkyboxButton.setOnClickListener(v -> showSkyboxPicker(skyboxPathText, clearSkyboxButton));
        clearSkyboxButton.setOnClickListener(v -> {
            sceneManager.setSkybox(null);
            skyboxPathText.setText("None");
            clearSkyboxButton.setVisibility(View.GONE);
        });

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

        EditText pointLightsEditor = dialogView.findViewById(R.id.edit_point_lights);
        EditText spotLightsEditor = dialogView.findViewById(R.id.edit_spot_lights);
        EditText dirLightsEditor = dialogView.findViewById(R.id.edit_dir_lights);
        EditText bonesEditor = dialogView.findViewById(R.id.edit_bones);
        Button applyButton = dialogView.findViewById(R.id.btn_apply_performance);


        final ThreeDManager.SceneSettings currentSettings = threeDManager.getSceneSettings();

        pointLightsEditor.setText(String.valueOf(currentSettings.numPointLights));
        spotLightsEditor.setText(String.valueOf(currentSettings.numSpotLights));
        dirLightsEditor.setText(String.valueOf(currentSettings.numDirectionalLights));
        bonesEditor.setText(String.valueOf(currentSettings.numBones));

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_NewCatroid_Dialog)
                .setTitle("Scene Settings")
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .create();

        applyButton.setOnClickListener(v -> {
            try {
                ThreeDManager.SceneSettings newSettings = new ThreeDManager.SceneSettings();
                newSettings.numPointLights = Integer.parseInt(pointLightsEditor.getText().toString());
                newSettings.numSpotLights = Integer.parseInt(spotLightsEditor.getText().toString());
                newSettings.numDirectionalLights = Integer.parseInt(dirLightsEditor.getText().toString());
                newSettings.numBones = Math.min(110, Integer.parseInt(bonesEditor.getText().toString()));

                new AlertDialog.Builder(this, R.style.Theme_NewCatroid_Dialog)
                        .setTitle("Restart 3D Engine")
                        .setMessage("Applying these settings requires reloading the 3D editor. Unsaved changes might be lost. Continue?")
                        .setPositiveButton("Restart", (d, which) -> {
                            if (editorListener != null) {
                                editorListener.resetEngine(null, newSettings);
                                Toast.makeText(this, "3D Engine restarted with new settings.", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter valid numbers.", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void showSkyboxPicker(TextView pathTextView, View clearButton) {
        File projectFilesDir = ProjectManager.getInstance().getCurrentProject().getFilesDir();
        File[] allFiles = projectFilesDir.listFiles();
        if (allFiles == null) {
            Toast.makeText(this, "Cannot access project files.", Toast.LENGTH_SHORT).show();
            return;
        }

        final List<String> textureFiles = new ArrayList<>();
        for (File file : allFiles) {
            String name = file.getName().toLowerCase();
            if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".hdr")) {
                textureFiles.add(file.getName());
            }
        }

        if (textureFiles.isEmpty()) {
            Toast.makeText(this, "No suitable image files found in project.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this, R.style.Theme_NewCatroid_Dialog)
                .setTitle("Select Skybox Texture")
                .setItems(textureFiles.toArray(new String[0]), (dialog, which) -> {
                    String selectedFile = textureFiles.get(which);
                    sceneManager.skyboxPath = selectedFile;
                    sceneManager.setSkybox(selectedFile);
                    pathTextView.setText(selectedFile);
                    clearButton.setVisibility(View.VISIBLE);
                })
                .show();
    }

    private void showExitConfirmationDialog() {
        new AlertDialog.Builder(this, R.style.Theme_NewCatroid_Dialog)
                .setTitle(R.string.editor_3d_exit_title)
                .setMessage(R.string.editor_3d_exit_msg)
                .setPositiveButton(R.string.editor_3d_save_exit, (dialog, which) -> {
                    showSaveSceneDialog(this::finish);
                })
                .setNeutralButton(R.string.editor_3d_exit_no_save, (dialog, which) -> {
                    finish();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showSaveSceneDialog(Runnable onSaveComplete) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.Theme_NewCatroid_Dialog);

        final EditText input = new EditText(builder.getContext());
        input.setHint("my_level_1");

        builder.setTitle(R.string.editor_3d_save_scene_as)
                .setMessage(R.string.editor_3d_enter_file_name)
                .setView(input)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String fileName = input.getText().toString();
                    if (fileName.isEmpty()) {
                        Toast.makeText(this, R.string.editor_3d_file_name_empty, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String fileNameWithExt = fileName + ".rscene";

                    File projectFile = new File(ProjectManager.getInstance().getCurrentProject().getFilesDir(), fileNameWithExt);
                    FileHandle fileHandle = Gdx.files.absolute(projectFile.getAbsolutePath());

                    sceneManager.saveScene(fileHandle);
                    Toast.makeText(this, getString(R.string.editor_3d_scene_saved, fileNameWithExt), Toast.LENGTH_SHORT).show();

                    if (onSaveComplete != null) {
                        onSaveComplete.run();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showLoadSceneDialog() {
        File projectFilesDir = ProjectManager.getInstance().getCurrentProject().getFilesDir();
        File[] allFiles = projectFilesDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".rscene"));

        if (allFiles == null || allFiles.length == 0) {
            Toast.makeText(this, R.string.editor_3d_no_scenes_found, Toast.LENGTH_SHORT).show();
            return;
        }

        String[] sceneNames = new String[allFiles.length];
        for(int i = 0; i < allFiles.length; i++) {
            sceneNames[i] = allFiles[i].getName();
        }

        new AlertDialog.Builder(this, R.style.Theme_NewCatroid_Dialog)
                .setTitle(R.string.editor_3d_load_scene)
                .setItems(sceneNames, (dialog, which) -> {
                    File selectedFile = allFiles[which];
                    FileHandle fileHandle = Gdx.files.absolute(selectedFile.getAbsolutePath());

                    EditorFragment fragment = (EditorFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                    if (fragment != null && fragment.getListener() != null) {
                        ThreeDManager.SceneSettings settings = new ThreeDManager.SceneSettings();
                        try {
                            String sceneJson = fileHandle.readString();
                            Json json = new Json();
                            SceneData sceneData = json.fromJson(SceneData.class, sceneJson);
                            if (sceneData != null && sceneData.renderSettings != null) {
                                settings = sceneData.renderSettings;
                            }
                        } catch (Exception e) {
                            Gdx.app.error("EditorActivity", "Could not parse scene settings, using defaults.", e);
                        }

                        fragment.getListener().resetEngine(fileHandle, settings);

                        Gdx.app.postRunnable(() -> {
                            runOnUiThread(() -> {
                                updateHierarchy();
                                onObjectSelected(null);
                                Toast.makeText(EditorActivity.this, getString(R.string.editor_3d_loading_scene, selectedFile.getName()), Toast.LENGTH_SHORT).show();
                            });
                        });
                    }
                })
                .show();
    }

    private void checkForRecovery() {
        File projectDir = ProjectManager.getInstance().getCurrentProject().getFilesDir();
        File recoveryFile = new File(projectDir, AUTOSAVE_FILE_NAME);

        if (recoveryFile.exists()) {
            new AlertDialog.Builder(this, R.style.Theme_NewCatroid_Dialog)
                    .setTitle(R.string.editor_3d_crash_recovery_title)
                    .setMessage(R.string.editor_3d_crash_recovery_msg)
                    .setCancelable(false)
                    .setPositiveButton(R.string.editor_3d_recover_btn, (dialog, which) -> {
                        try {
                            FileHandle fileHandle = Gdx.files.absolute(recoveryFile.getAbsolutePath());
                            sceneManager.loadScene(fileHandle);

                            updateHierarchy();
                            onObjectSelected(null, false);
                            Toast.makeText(this, R.string.editor_3d_recovered_success, Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Toast.makeText(this, R.string.editor_3d_recovered_failed, Toast.LENGTH_SHORT).show();
                        } finally {
                            recoveryFile.delete();
                        }
                    })
                    .setNegativeButton(R.string.editor_3d_discard_btn, (dialog, which) -> {
                        recoveryFile.delete();
                    })
                    .show();
        }
    }

    public UndoManager getUndoManager() {
        return undoManager;
    }

    private ThreeDManager threeDManager;

    @Override
    protected void onPause() {
        cacheCurrentScene();
        super.onPause();
    }

    @Override
    public void exit() {}

    private void setupCrashHandler() {
        defaultCrashHandler = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
            if (sceneManager != null) {
                try {
                    Log.e("EditorCrash", "FATAL CRASH DETECTED! Attempting emergency save...");

                    File projectDir = ProjectManager.getInstance().getCurrentProject().getFilesDir();
                    File recoveryFile = new File(projectDir, AUTOSAVE_FILE_NAME);

                    SceneData sceneData = sceneManager.getCurrentSceneData();
                    String sceneJson = sceneManager.getJson().toJson(sceneData);

                    try (java.io.FileWriter writer = new java.io.FileWriter(recoveryFile)) {
                        writer.write(sceneJson);
                    }

                    Log.e("EditorCrash", "Emergency save SUCCESSFUL: " + recoveryFile.getAbsolutePath());
                } catch (Exception e) {
                    Log.e("EditorCrash", "Emergency save FAILED!", e);
                }
            }

            if (defaultCrashHandler != null) {
                defaultCrashHandler.uncaughtException(thread, exception);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (defaultCrashHandler != null) {
            Thread.setDefaultUncaughtExceptionHandler(defaultCrashHandler);
        }
    }
}
