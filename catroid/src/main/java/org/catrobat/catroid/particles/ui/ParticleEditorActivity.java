package org.catrobat.catroid.particles.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.backends.android.AndroidFragmentApplication;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.content.EasingFunctions;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.particles.ParticleCurve;
import org.catrobat.catroid.particles.ParticleEffectModel;
import org.catrobat.catroid.particles.ParticleManager;
import org.catrobat.paintroid.colorpicker.ColorPickerDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class ParticleEditorActivity extends AppCompatActivity implements AndroidFragmentApplication.Callbacks {

    private static final int REQUEST_GALLERY_BG = 1001;
    private static final int REQUEST_IMPORT_TEXTURE = 1002;

    private String fileName = "fire.particle";
    private ParticleEffectModel model;
    private ParticlePreviewListener previewListener;

    private org.catrobat.catroid.particles.ui.ParticleGraphView embeddedGraphView;
    private TextView tvActiveGraphTitle;
    private ParticleCurve activeCurve;

    private View cardPhysicsGravity, cardPhysicsRadius;

    private EditText etEmissionRate, etMaxParticles, etDuration, etRespawnDelay, etLifetime, etLifetimeVar, etFadeIn, etFadeOut;
    private EditText etSpeedVar, etAngleVar;
    private EditText etBufferName;

    private Spinner spinnerEmitterMode, spinnerEasing;
    private EditText etPointVariance;

    private Button btnStartColor, btnEndColor;
    private TextView tvCurrentTexture;

    private MaterialSliderView sliderPosVarX, sliderPosVarY;
    private MaterialSliderView sliderSpeed, sliderAngle, sliderGravityX, sliderGravityY;
    private MaterialSliderView sliderRadialAccel, sliderTangentialAccel, sliderFriction;
    private MaterialSliderView sliderStartRadius, sliderEndRadius, sliderRotatePerSec;

    private final Stack<String> undoStack = new Stack<>();
    private final Stack<String> redoStack = new Stack<>();
    private ImageButton btnUndo, btnRedo;

    private String lastSavedJson = "";

    public static class ParticlePreviewFragment extends AndroidFragmentApplication {
        private ParticlePreviewListener listener;

        public ParticlePreviewFragment() {}

        public static ParticlePreviewFragment newInstance(ParticlePreviewListener listener) {
            ParticlePreviewFragment fragment = new ParticlePreviewFragment();
            fragment.listener = listener;
            return fragment;
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
            config.useGL30 = false;
            config.useAccelerometer = false;
            config.useCompass = false;

            return initializeForView(listener, config);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_particle_editor);

        if (getIntent() != null && getIntent().hasExtra("PARTICLE_FILE_NAME")) {
            fileName = getIntent().getStringExtra("PARTICLE_FILE_NAME");
        }

        loadParticleModel();
        initLibGDXPreview();
        initUI();

        lastSavedJson = new Gson().toJson(model);
        saveSnapshot();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showExitConfirmationDialog();
            }
        });
    }

    private void showExitConfirmationDialog() {
        String currentJson = new Gson().toJson(model);

        if (currentJson.equals(lastSavedJson)) {
            finish();
            return;
        }

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_NewCatroid_Dialog)
                .setTitle(R.string.particle_exit_dialog_title)
                .setMessage(R.string.particle_exit_dialog_msg)
                .setPositiveButton(R.string.particle_exit_save, (d, which) -> {
                    saveParticleModel();
                    finish();
                })
                .setNeutralButton(R.string.particle_exit_discard, (d, which) -> finish())
                .setNegativeButton(R.string.particle_cancel, null)
                .create();

        dialog.show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(this, R.color.accent));
        dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setTextColor(ContextCompat.getColor(this, R.color.accent));
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(this, R.color.accent));
    }

    private void saveParticleModel() {
        try {
            String cleanName = fileName.replaceAll("[\"'\r\n\t]", "").trim();
            File file = ProjectManager.getInstance().getCurrentProject().getFile(cleanName);

            if (file != null) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String json = gson.toJson(model);

                FileWriter writer = new FileWriter(file, false);
                writer.write(json);
                writer.flush();
                writer.close();

                lastSavedJson = json;

                ParticleManager.getInstance().registerEffectTemplate(cleanName, model);
                Toast.makeText(this, R.string.particle_editor_saved, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Save error: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void exit() {
        finish();
    }

    private void saveSnapshot() {
        if (model == null) return;
        String json = new Gson().toJson(model);

        if (undoStack.isEmpty() || !undoStack.peek().equals(json)) {
            undoStack.push(json);
            redoStack.clear();
            updateUndoRedoButtonsState();
        }
    }

    private void undo() {
        if (undoStack.size() > 1) {
            redoStack.push(undoStack.pop());
            String prevJson = undoStack.peek();
            model = new Gson().fromJson(prevJson, ParticleEffectModel.class);
            if (previewListener != null) previewListener.model = model;
            rebindAllUI();
            updateUndoRedoButtonsState();
        }
    }

    private void redo() {
        if (!redoStack.isEmpty()) {
            String nextJson = redoStack.pop();
            undoStack.push(nextJson);
            model = new Gson().fromJson(nextJson, ParticleEffectModel.class);
            if (previewListener != null) previewListener.model = model;
            rebindAllUI();
            updateUndoRedoButtonsState();
        }
    }

    private void updateUndoRedoButtonsState() {
        if (btnUndo != null) btnUndo.setAlpha(undoStack.size() > 1 ? 1.0f : 0.3f);
        if (btnRedo != null) btnRedo.setAlpha(!redoStack.isEmpty() ? 1.0f : 0.3f);
    }

    private void loadParticleModel() {
        try {
            String cleanName = fileName.replaceAll("[\"'\r\n\t]", "").trim();
            Project project = ProjectManager.getInstance().getCurrentProject();
            if (project != null) {
                File file = project.getFile(cleanName);
                if (file != null && file.exists() && file.length() > 0) {
                    String json = com.google.common.io.Files.toString(file, StandardCharsets.UTF_8);
                    model = new Gson().fromJson(json, ParticleEffectModel.class);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (model == null) {
            model = new ParticleEffectModel();
            model.effectId = fileName;
        }
    }

    private void initLibGDXPreview() {
        previewListener = new ParticlePreviewListener(model);

        ParticlePreviewFragment previewFragment = ParticlePreviewFragment.newInstance(previewListener);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.preview_container, previewFragment)
                .commit();
    }

    private void initUI() {
        btnUndo = findViewById(R.id.btn_undo);
        btnRedo = findViewById(R.id.btn_redo);

        if (btnUndo != null) btnUndo.setOnClickListener(v -> undo());
        if (btnRedo != null) btnRedo.setOnClickListener(v -> redo());

        View replayBtn = findViewById(R.id.btn_replay_particle);
        if (replayBtn != null) {
            replayBtn.setOnClickListener(v -> {
                if (previewListener != null) {
                    previewListener.resetPreview();
                }
            });
        }

        View saveBtn = findViewById(R.id.btn_save_particle);
        if (saveBtn != null) saveBtn.setOnClickListener(v -> saveParticleModel());

        cardPhysicsGravity = findViewById(R.id.card_physics_gravity);
        cardPhysicsRadius = findViewById(R.id.card_physics_radius);

        FrameLayout graphContainer = findViewById(R.id.graph_view_container);
        if (graphContainer != null) {
            embeddedGraphView = new org.catrobat.catroid.particles.ui.ParticleGraphView(this);
            graphContainer.addView(embeddedGraphView);
        }

        tvActiveGraphTitle = findViewById(R.id.tv_active_graph_title);
        activeCurve = model.sizeCurve;

        if (embeddedGraphView != null) {
            embeddedGraphView.setCurve(activeCurve, -100f, 100f);
            embeddedGraphView.setOnCurveChangeListener(new org.catrobat.catroid.particles.ui.ParticleGraphView.OnCurveChangeListener() {
                @Override public void onCurveChanged(ParticleCurve curve) { saveSnapshot(); }
                @Override
                public void onPointSelected(int index, ParticleCurve.Keyframe keyframe) {
                    if (keyframe != null && etPointVariance != null && spinnerEasing != null) {
                        etPointVariance.setText(String.valueOf(keyframe.variance));
                        spinnerEasing.setSelection(keyframe.easingType.ordinal());
                    }
                }
            });
        }

        spinnerEmitterMode = findViewById(R.id.spinner_emitter_mode);
        if (spinnerEmitterMode != null) {
            ArrayAdapter<String> modeAdapter = new ArrayAdapter<>(
                    this, R.layout.spinner_item, new String[]{getString(R.string.particle_mode_gravity), getString(R.string.particle_mode_radius)}
            );
            modeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
            spinnerEmitterMode.setAdapter(modeAdapter);
            spinnerEmitterMode.setSelection(model.mode == ParticleEffectModel.EmitterMode.RADIUS ? 1 : 0);

            spinnerEmitterMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    model.mode = position == 1 ? ParticleEffectModel.EmitterMode.RADIUS : ParticleEffectModel.EmitterMode.GRAVITY;

                    if (previewListener != null) {
                        previewListener.resetPreview();
                    }

                    updateModeVisibility();
                    saveSnapshot();
                }
                @Override public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        sliderPosVarX = findViewById(R.id.slider_pos_var_x);
        sliderPosVarY = findViewById(R.id.slider_pos_var_y);

        sliderSpeed = findViewById(R.id.slider_speed);
        sliderAngle = findViewById(R.id.slider_angle);
        sliderGravityX = findViewById(R.id.slider_gravity_x);
        sliderGravityY = findViewById(R.id.slider_gravity_y);
        sliderRadialAccel = findViewById(R.id.slider_radial_accel);
        sliderTangentialAccel = findViewById(R.id.slider_tangential_accel);
        sliderFriction = findViewById(R.id.slider_friction);

        sliderStartRadius = findViewById(R.id.slider_start_radius);
        sliderEndRadius = findViewById(R.id.slider_end_radius);
        sliderRotatePerSec = findViewById(R.id.slider_rotate_per_sec);

        setupSlider(sliderPosVarX, getString(R.string.particle_slider_pos_x), 0f, 500f, model.posVarX, " px", v -> model.posVarX = v);
        setupSlider(sliderPosVarY, getString(R.string.particle_slider_pos_y), 0f, 500f, model.posVarY, " px", v -> model.posVarY = v);

        setupSlider(sliderSpeed, getString(R.string.particle_slider_speed), 0f, 1000f, model.speed, " px/s", v -> model.speed = v);
        setupSlider(sliderAngle, getString(R.string.particle_slider_angle), 0f, 360f, model.angle, "°", v -> model.angle = v);
        setupSlider(sliderGravityX, getString(R.string.particle_slider_gravity_x), -1000f, 1000f, model.gravityX, "", v -> model.gravityX = v);
        setupSlider(sliderGravityY, getString(R.string.particle_slider_gravity_y), -1000f, 1000f, model.gravityY, "", v -> model.gravityY = v);
        setupSlider(sliderRadialAccel, getString(R.string.particle_slider_radial_accel), -1000f, 1000f, model.radialAccel, "", v -> model.radialAccel = v);
        setupSlider(sliderTangentialAccel, getString(R.string.particle_slider_tangential_accel), -1000f, 1000f, model.tangentialAccel, "", v -> model.tangentialAccel = v);
        setupSlider(sliderFriction, getString(R.string.particle_slider_friction), 0f, 1f, model.friction, "", v -> model.friction = v);

        setupSlider(sliderStartRadius, getString(R.string.particle_slider_start_radius), 0f, 500f, model.startRadius, " px", v -> model.startRadius = v);
        setupSlider(sliderEndRadius, getString(R.string.particle_slider_end_radius), 0f, 500f, model.endRadius, " px", v -> model.endRadius = v);
        setupSlider(sliderRotatePerSec, getString(R.string.particle_slider_rotate_sec), -720f, 720f, model.rotatePerSecond, "°/s", v -> model.rotatePerSecond = v);

        btnStartColor = findViewById(R.id.btn_start_color);
        btnEndColor = findViewById(R.id.btn_end_color);

        updateColorButtonUI(btnStartColor, model.startR, model.startG, model.startB, model.startA);
        updateColorButtonUI(btnEndColor, model.endR, model.endG, model.endB, model.endA);

        if (btnStartColor != null) btnStartColor.setOnClickListener(v -> openColorPickerDialog(true));
        if (btnEndColor != null) btnEndColor.setOnClickListener(v -> openColorPickerDialog(false));

        tvCurrentTexture = findViewById(R.id.tv_current_texture);
        updateTextureLabel();

        View btnSelectProjectTex = findViewById(R.id.btn_select_project_texture);
        if (btnSelectProjectTex != null) btnSelectProjectTex.setOnClickListener(v -> openProjectTexturePickerDialog());

        View btnImportTex = findViewById(R.id.btn_import_texture);
        if (btnImportTex != null) btnImportTex.setOnClickListener(v -> openTextureImportPicker());

        View sizeBtn = findViewById(R.id.btn_edit_size_curve);
        View spinBtn = findViewById(R.id.btn_edit_spin_curve);
        View alphaBtn = findViewById(R.id.btn_edit_alpha_curve);

        if (sizeBtn != null) sizeBtn.setOnClickListener(v -> switchCurve(model.sizeCurve, R.string.particle_curve_size, -50f, 200f));
        if (spinBtn != null) spinBtn.setOnClickListener(v -> switchCurve(model.spinCurve, R.string.particle_curve_spin, -720f, 720f));
        if (alphaBtn != null) alphaBtn.setOnClickListener(v -> switchCurve(model.alphaCurve, R.string.particle_curve_alpha, 0f, 1f));

        View btnZoomIn = findViewById(R.id.btn_graph_zoom_in);
        View btnZoomOut = findViewById(R.id.btn_graph_zoom_out);
        View btnAddPoint = findViewById(R.id.btn_graph_add_point);
        View btnDeletePoint = findViewById(R.id.btn_graph_delete_point);

        if (btnZoomIn != null) btnZoomIn.setOnClickListener(v -> { if (embeddedGraphView != null) embeddedGraphView.zoomIn(); });
        if (btnZoomOut != null) btnZoomOut.setOnClickListener(v -> { if (embeddedGraphView != null) embeddedGraphView.zoomOut(); });
        if (btnAddPoint != null) btnAddPoint.setOnClickListener(v -> { if (embeddedGraphView != null) embeddedGraphView.addPointAtCenter(); });
        if (btnDeletePoint != null) btnDeletePoint.setOnClickListener(v -> { if (embeddedGraphView != null) embeddedGraphView.deleteSelectedKeyframe(); });

        spinnerEasing = findViewById(R.id.spinner_graph_easing);
        if (spinnerEasing != null) {
            ArrayAdapter<EasingFunctions.EasingType> easingAdapter = new ArrayAdapter<>(
                    this, R.layout.spinner_item, EasingFunctions.EasingType.values()
            );
            easingAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
            spinnerEasing.setAdapter(easingAdapter);

            spinnerEasing.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (embeddedGraphView != null) {
                        embeddedGraphView.setSelectedEasingType(EasingFunctions.EasingType.values()[position]);
                    }
                }
                @Override public void onNothingSelected(AdapterView<?> parent) {}
            });
        }

        etPointVariance = findViewById(R.id.et_point_variance);
        setupTextWatcher(etPointVariance, v -> { if (embeddedGraphView != null) embeddedGraphView.setSelectedVariance(v); });

        etEmissionRate = findViewById(R.id.et_emission_rate);
        etMaxParticles = findViewById(R.id.et_max_particles);
        etDuration = findViewById(R.id.et_duration);
        etRespawnDelay = findViewById(R.id.et_respawn_delay);
        etLifetime = findViewById(R.id.et_lifetime);
        etLifetimeVar = findViewById(R.id.et_lifetime_var);
        etFadeIn = findViewById(R.id.et_fade_in);
        etFadeOut = findViewById(R.id.et_fade_out);

        etSpeedVar = findViewById(R.id.et_speed_var);
        etAngleVar = findViewById(R.id.et_angle_var);
        etBufferName = findViewById(R.id.et_buffer_name);

        bindEditTexts();

        CheckBox cbAdditive = findViewById(R.id.cb_is_additive);
        if (cbAdditive != null) {
            cbAdditive.setChecked(model.isAdditive);
            cbAdditive.setOnCheckedChangeListener((b, checked) -> { model.isAdditive = checked; saveSnapshot(); });
        }

        CheckBox cbGravity = findViewById(R.id.cb_gizmo_gravity);
        CheckBox cbAngle = findViewById(R.id.cb_gizmo_angle);
        CheckBox cbPosVar = findViewById(R.id.cb_gizmo_posvar);

        if (cbGravity != null) cbGravity.setOnCheckedChangeListener((b, checked) -> { if (previewListener != null) previewListener.showGravityGizmo = checked; });
        if (cbAngle != null) cbAngle.setOnCheckedChangeListener((b, checked) -> { if (previewListener != null) previewListener.showAngleGizmo = checked; });
        if (cbPosVar != null) cbPosVar.setOnCheckedChangeListener((b, checked) -> { if (previewListener != null) previewListener.showPosVarGizmo = checked; });

        View galleryBtn = findViewById(R.id.btn_pick_gallery_bg);
        if (galleryBtn != null) galleryBtn.setOnClickListener(v -> openGalleryPicker());

        updateModeVisibility();
    }

    private void setupSlider(MaterialSliderView slider, String label, float min, float max, float current, String suffix, ValueSetter setter) {
        if (slider == null) return;
        slider.setParams(label, min, max, current, suffix);
        slider.setOnSliderChangeListener(new MaterialSliderView.OnSliderChangeListener() {
            @Override public void onValueChanged(float value) { setter.set(value); }
            @Override public void onValueChangeFinished() { saveSnapshot(); }
        });
    }

    private void switchCurve(ParticleCurve curve, int titleRes, float min, float max) {
        activeCurve = curve;
        if (tvActiveGraphTitle != null) tvActiveGraphTitle.setText(titleRes);
        if (embeddedGraphView != null) embeddedGraphView.setCurve(activeCurve, min, max);
    }

    private void bindEditTexts() {
        if (etEmissionRate != null) etEmissionRate.setText(String.valueOf(model.emissionRate));
        if (etMaxParticles != null) etMaxParticles.setText(String.valueOf(model.maxParticles));
        if (etDuration != null) etDuration.setText(String.valueOf(model.duration));
        if (etRespawnDelay != null) etRespawnDelay.setText(String.valueOf(model.respawnDelay));
        if (etLifetime != null) etLifetime.setText(String.valueOf(model.lifetime));
        if (etLifetimeVar != null) etLifetimeVar.setText(String.valueOf(model.lifetimeVariance));
        if (etFadeIn != null) etFadeIn.setText(String.valueOf(model.fadeInTime));
        if (etFadeOut != null) etFadeOut.setText(String.valueOf(model.fadeOutTime));

        if (etSpeedVar != null) etSpeedVar.setText(String.valueOf(model.speedVariance));
        if (etAngleVar != null) etAngleVar.setText(String.valueOf(model.angleVariance));
        if (etBufferName != null) etBufferName.setText(model.targetBufferName);

        setupTextWatcher(etEmissionRate, v -> model.emissionRate = v);
        setupTextWatcher(etMaxParticles, v -> model.maxParticles = (int) v);
        setupTextWatcher(etDuration, v -> model.duration = v);
        setupTextWatcher(etRespawnDelay, v -> model.respawnDelay = v);
        setupTextWatcher(etLifetime, v -> model.lifetime = v);
        setupTextWatcher(etLifetimeVar, v -> model.lifetimeVariance = v);
        setupTextWatcher(etFadeIn, v -> model.fadeInTime = v);
        setupTextWatcher(etFadeOut, v -> model.fadeOutTime = v);

        setupTextWatcher(etSpeedVar, v -> model.speedVariance = v);
        setupTextWatcher(etAngleVar, v -> model.angleVariance = v);
    }

    private void rebindAllUI() {
        if (sliderPosVarX != null) sliderPosVarX.setValue(model.posVarX);
        if (sliderPosVarY != null) sliderPosVarY.setValue(model.posVarY);

        if (sliderSpeed != null) sliderSpeed.setValue(model.speed);
        if (sliderAngle != null) sliderAngle.setValue(model.angle);
        if (sliderGravityX != null) sliderGravityX.setValue(model.gravityX);
        if (sliderGravityY != null) sliderGravityY.setValue(model.gravityY);
        if (sliderRadialAccel != null) sliderRadialAccel.setValue(model.radialAccel);
        if (sliderTangentialAccel != null) sliderTangentialAccel.setValue(model.tangentialAccel);
        if (sliderFriction != null) sliderFriction.setValue(model.friction);

        if (sliderStartRadius != null) sliderStartRadius.setValue(model.startRadius);
        if (sliderEndRadius != null) sliderEndRadius.setValue(model.endRadius);
        if (sliderRotatePerSec != null) sliderRotatePerSec.setValue(model.rotatePerSecond);

        bindEditTexts();
        updateColorButtonUI(btnStartColor, model.startR, model.startG, model.startB, model.startA);
        updateColorButtonUI(btnEndColor, model.endR, model.endG, model.endB, model.endA);
        updateTextureLabel();
        updateModeVisibility();

        if (spinnerEmitterMode != null) {
            spinnerEmitterMode.setSelection(model.mode == ParticleEffectModel.EmitterMode.RADIUS ? 1 : 0);
        }

        if (embeddedGraphView != null && activeCurve != null) {
            embeddedGraphView.setCurve(activeCurve, -100f, 100f);
        }
    }

    private void updateModeVisibility() {
        if (cardPhysicsGravity != null && cardPhysicsRadius != null) {
            if (model.mode == ParticleEffectModel.EmitterMode.RADIUS) {
                cardPhysicsGravity.setVisibility(View.GONE);
                cardPhysicsRadius.setVisibility(View.VISIBLE);
            } else {
                cardPhysicsGravity.setVisibility(View.VISIBLE);
                cardPhysicsRadius.setVisibility(View.GONE);
            }
        }
    }

    private void openColorPickerDialog(boolean isStartColor) {
        try {
            float r = isStartColor ? model.startR : model.endR;
            float g = isStartColor ? model.startG : model.endG;
            float b = isStartColor ? model.startB : model.endB;
            float a = isStartColor ? model.startA : model.endA;

            int currentColorInt = Color.argb((int)(a * 255f), (int)(r * 255f), (int)(g * 255f), (int)(b * 255f));

            ColorPickerDialog dialog = ColorPickerDialog.Companion.newInstance(currentColorInt, true, true);
            dialog.setStyle(androidx.fragment.app.DialogFragment.STYLE_NORMAL, R.style.AlertDialogWithTitle);

            dialog.addOnColorPickedListener(newColor -> {
                float newA = Color.alpha(newColor) / 255f;
                float newR = Color.red(newColor) / 255f;
                float newG = Color.green(newColor) / 255f;
                float newB = Color.blue(newColor) / 255f;

                if (isStartColor) {
                    model.startA = newA; model.startR = newR; model.startG = newG; model.startB = newB;
                    updateColorButtonUI(btnStartColor, newR, newG, newB, newA);
                } else {
                    model.endA = newA; model.endR = newR; model.endG = newG; model.endB = newB;
                    updateColorButtonUI(btnEndColor, newR, newG, newB, newA);
                }
                saveSnapshot();
            });

            dialog.show(getSupportFragmentManager(), "color_picker");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateColorButtonUI(Button btn, float r, float g, float b, float a) {
        if (btn == null) return;
        int colorInt = Color.argb((int)(a * 255f), (int)(r * 255f), (int)(g * 255f), (int)(b * 255f));
        btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(colorInt));
        btn.setTextColor((r * 0.299f + g * 0.587f + b * 0.114f) > 0.5f ? Color.BLACK : Color.WHITE);
    }

    private void openProjectTexturePickerDialog() {
        Project project = ProjectManager.getInstance().getCurrentProject();
        if (project == null) return;

        File filesDir = project.getFilesDir();
        File[] allFiles = filesDir.listFiles();

        List<String> imageFiles = new ArrayList<>();
        imageFiles.add(getString(R.string.particle_texture_default));

        if (allFiles != null) {
            for (File f : allFiles) {
                String name = f.getName().toLowerCase();
                if (f.isFile() && (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".webp"))) {
                    imageFiles.add(f.getName());
                }
            }
        }

        String[] items = imageFiles.toArray(new String[0]);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.particle_btn_project_files)
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        model.textureFileName = "";
                    } else {
                        model.textureFileName = items[which];
                    }
                    updateTextureLabel();
                    if (previewListener != null) previewListener.reloadParticleTexture();
                    saveSnapshot();
                })
                .show();
    }

    private void openTextureImportPicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, REQUEST_IMPORT_TEXTURE);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void updateTextureLabel() {
        if (tvCurrentTexture != null) {
            if (model.textureFileName == null || model.textureFileName.trim().isEmpty()) {
                tvCurrentTexture.setText(getString(R.string.particle_texture_current, getString(R.string.particle_texture_default)));
            } else {
                tvCurrentTexture.setText(getString(R.string.particle_texture_current, model.textureFileName));
            }
        }
    }

    private void openGalleryPicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, REQUEST_GALLERY_BG);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            if (requestCode == REQUEST_GALLERY_BG) {
                try {
                    Uri imageUri = data.getData();
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                    if (previewListener != null) previewListener.setCustomBackground(bitmap);
                } catch (Exception e) { e.printStackTrace(); }
            } else if (requestCode == REQUEST_IMPORT_TEXTURE) {
                try {
                    Uri imageUri = data.getData();
                    Project project = ProjectManager.getInstance().getCurrentProject();
                    if (project != null) {
                        String newFileName = "particle_" + System.currentTimeMillis() + ".png";
                        File targetFile = project.getFile(newFileName);

                        InputStream in = getContentResolver().openInputStream(imageUri);
                        OutputStream out = new FileOutputStream(targetFile);
                        byte[] buf = new byte[1024];
                        int len;
                        while ((len = in.read(buf)) > 0) { out.write(buf, 0, len); }
                        in.close(); out.close();

                        model.textureFileName = newFileName;
                        updateTextureLabel();
                        if (previewListener != null) previewListener.reloadParticleTexture();
                        saveSnapshot();
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
        }
    }

    private interface ValueSetter { void set(float val); }

    private void setupTextWatcher(EditText editText, ValueSetter setter) {
        if (editText == null) return;
        editText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                try {
                    float val = Float.parseFloat(s.toString());
                    setter.set(val);
                } catch (Exception ignored) {}
            }
        });
    }
}
