package org.catrobat.catroid.editor;

import androidx.appcompat.app.AlertDialog;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.raptor.AnimationComponent;
import org.catrobat.catroid.raptor.AudioComponent;
import org.catrobat.catroid.raptor.CameraComponent;
import org.catrobat.catroid.raptor.ColliderShapeData;
import org.catrobat.catroid.raptor.FogComponent;
import org.catrobat.catroid.raptor.GameObject;
import org.catrobat.catroid.raptor.KeyframeComponent;
import org.catrobat.catroid.raptor.KeyframeData;
import org.catrobat.catroid.raptor.LightComponent;
import org.catrobat.catroid.raptor.MaterialComponent;
import org.catrobat.catroid.raptor.ParticleComponent;
import org.catrobat.catroid.raptor.ParticleCurvePoint;
import org.catrobat.catroid.raptor.ParticleSystem3DComponent;
import org.catrobat.catroid.raptor.PhysicsComponent;
import org.catrobat.catroid.raptor.PostProcessingComponent;
import org.catrobat.catroid.raptor.PostProcessingData;
import org.catrobat.catroid.raptor.PrefabComponent;
import org.catrobat.catroid.raptor.RenderComponent;
import org.catrobat.catroid.raptor.SceneManager;
import org.catrobat.catroid.raptor.ScriptComponent;
import org.catrobat.catroid.raptor.ThreeDManager;
import org.catrobat.catroid.raptor.TransformComponent;
import org.catrobat.catroid.raptor.postprocessing.SsrRayTracingEffect;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InspectorManager {

    private final EditorActivity activity;
    private final LayoutInflater inflater;
    private SceneManager sceneManager;
    private final LinearLayout container;
    private final TextView inspectorTitle;

    private GameObject selectedObject;
    private ColliderShapeData selectedCollider = null;
    private Gizmo gizmo;

    private final ThreeDManager threeDManager;

    public InspectorManager(EditorActivity activity, SceneManager sceneManager, ThreeDManager threeDManager) {
        this.activity = activity;
        this.inflater = activity.getLayoutInflater();
        this.sceneManager = sceneManager;
        this.threeDManager = threeDManager;
        this.container = activity.findViewById(R.id.inspector_container);
        this.inspectorTitle = activity.findViewById(R.id.inspector_title);
    }

    public void setGizmo(Gizmo gizmo) {
        this.gizmo = gizmo;
    }

    public ColliderShapeData getSelectedCollider() {
        return selectedCollider;
    }

    public void updateSceneManager(SceneManager newManager) {
        this.sceneManager = newManager;
    }

    public GameObject getSelectedObject() {
        return selectedObject;
    }

    public void populateInspector(GameObject go) {
        this.selectedObject = go;
        //if (container == null) return;
        container.removeAllViews();

        if (go == null) {
            inspectorTitle.setText("Inspector");
            return;
        }

        try {
            inspectorTitle.setText(go.name);

            View headerView = inflater.inflate(R.layout.inspector_active_toggle, container, false);

            CheckBox activeCheckbox = headerView.findViewById(R.id.checkbox_is_active);
            activeCheckbox.setChecked(go.isActive);
            activeCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (selectedObject != null) {
                    sceneManager.setObjectActive(selectedObject, isChecked);
                    activity.updateHierarchy();
                }
            });

            EditText nameEditor = headerView.findViewById(R.id.edit_object_name);
            nameEditor.setText(go.name);
            nameEditor.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus && selectedObject != null) {
                    String newName = nameEditor.getText().toString();
                    String oldName = selectedObject.name;

                    if (newName.equals(oldName)) return;

                    if (sceneManager.renameGameObject(selectedObject, newName)) {

                        activity.updateHierarchy();
                    } else {
                        Toast.makeText(activity, "Invalid or duplicate name!", Toast.LENGTH_SHORT).show();
                        nameEditor.setText(oldName);
                    }
                }
            });

            container.addView(headerView);

            createTransformView(go);
            if (go.hasComponent(RenderComponent.class)) createRenderView(go);
            if (go.hasComponent(PhysicsComponent.class)) createPhysicsView(go);
            if (go.hasComponent(LightComponent.class)) createLightView(go);
            if (go.hasComponent(AudioComponent.class)) createAudioView(go);
            if (go.hasComponent(AnimationComponent.class)) createAnimationView(go);
            if (go.hasComponent(CameraComponent.class)) createCameraView(go);
            if (go.hasComponent(MaterialComponent.class)) createMaterialView(go);
            if (go.hasComponent(PostProcessingComponent.class)) createPostProcessingView(go);
            if (go.hasComponent(ParticleComponent.class)) createParticleView(go);
            if (go.hasComponent(ParticleSystem3DComponent.class)) createParticleSystem3DView(go);
            if (go.hasComponent(FogComponent.class)) createFogView(go);
            if (go.hasComponent(KeyframeComponent.class)) createKeyframeView(go);
            if (go.hasComponent(PrefabComponent.class)) createPrefabView(go);
            List<ScriptComponent> scripts = go.getComponents(ScriptComponent.class);
            for (ScriptComponent script : scripts) {
                createScriptView(go, script);
            }

            View footerView = inflater.inflate(R.layout.inspector_footer, container, false);
            footerView.findViewById(R.id.btn_add_component).setOnClickListener(v -> showAddComponentDialog(go));
            container.addView(footerView);

            View divider = new View(activity);
            int marginPx = (int) (16 * activity.getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
            params.setMargins(0, marginPx, 0, marginPx);
            divider.setLayoutParams(params);
            divider.setBackgroundColor(0x40FFFFFF);
            container.addView(divider);

            Button duplicateButton = new Button(activity, null, 0, R.style.Widget_App_Button_Outlined);
            duplicateButton.setText(R.string.editor_3d_clone);
            duplicateButton.setOnClickListener(v -> {
                GameObject newObject = sceneManager.duplicateGameObject(go);
                if (newObject != null) {
                    activity.onObjectSelected(newObject);
                    activity.updateHierarchy();
                }
            });
            container.addView(duplicateButton);


            Button deleteObjectButton = new Button(activity);
            LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            deleteParams.setMargins(0, (int) (8 * activity.getResources().getDisplayMetrics().density), 0, 0);
            deleteObjectButton.setLayoutParams(deleteParams);

            deleteObjectButton.setText(R.string.editor_3d_delete_object);
            deleteObjectButton.setTextColor(Color.parseColor("#FF5252"));

            deleteObjectButton.setOnClickListener(v -> {
                new AlertDialog.Builder(activity)
                        .setTitle(R.string.editor_3d_delete_object)
                        .setMessage(activity.getString(R.string.editor_3d_delete_object_confirm, go.name))
                        .setPositiveButton(R.string.delete, (dialog, which) -> {
                            sceneManager.removeGameObject(go);
                            activity.onObjectSelected(null);
                            activity.updateHierarchy();
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            });
            container.addView(deleteObjectButton);
        } catch (Exception e) {
            Log.e("InspectorManager", "Error populating inspector for: " + go.name, e);
        }
    }

    private void createPrefabView(GameObject go) {
        addComponentHeader(R.string.component_prefab, true, false, () -> {
            PrefabComponent p = go.getComponent(PrefabComponent.class);
            if (p != null && p.spawnedInstances != null) {
                for(String id : p.spawnedInstances) {
                    GameObject child = sceneManager.findGameObject(id);
                    if (child != null) sceneManager.removeGameObject(child);
                }
                go.childrenIds.removeAll(p.spawnedInstances);
            }
            go.components.removeIf(c -> c instanceof PrefabComponent);
            populateInspector(go);
            activity.updateHierarchy();
        });

        View view = inflater.inflate(R.layout.inspector_script, container, false);
        setWhiteTextToAllChildren((ViewGroup) view);

        TextView pathText = view.findViewById(R.id.text_script_path);
        Button selectButton = view.findViewById(R.id.btn_select_script);

        PrefabComponent prefab = go.getComponent(PrefabComponent.class);
        pathText.setText(prefab.prefabFilePath != null ? prefab.prefabFilePath : "None (Select .rscene)");

        selectButton.setOnClickListener(v -> showPrefabPicker(prefab, go));

        container.addView(view);
    }

    private void showPrefabPicker(PrefabComponent prefab, GameObject rootGo) {
        File projectFilesDir = ProjectManager.getInstance().getCurrentProject().getFilesDir();
        File[] allFiles = projectFilesDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".rscene"));

        if (allFiles == null || allFiles.length == 0) {
            Toast.makeText(activity, "No .rscene files found", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] names = new String[allFiles.length];
        for (int i = 0; i < allFiles.length; i++) names[i] = allFiles[i].getName();

        new android.app.AlertDialog.Builder(activity)
                .setTitle("Select Prefab")
                .setItems(names, (dialog, which) -> {
                    prefab.prefabFilePath = names[which];
                    sceneManager.rebuildGameObject(rootGo);
                    populateInspector(rootGo);
                    activity.updateHierarchy();
                })
                .show();
    }

    private void setWhiteTextToAllChildren(ViewGroup vg) {
        for (int i = 0; i < vg.getChildCount(); i++) {
            View child = vg.getChildAt(i);
            if (child instanceof TextView) {
                ((TextView) child).setTextColor(0xFFFFFFFF);
                ((TextView) child).setHintTextColor(0xFFCCCCCC);
            } else if (child instanceof ViewGroup) {
                setWhiteTextToAllChildren((ViewGroup) child);
            }
        }
    }

    private final Vector3 tempPosition = new Vector3();
    private final Quaternion tempRotation = new Quaternion();
    private final Vector3 tempScale = new Vector3(1, 1, 1);
    private boolean isPreviewingAnimation = false;

    private void createKeyframeView(GameObject go) {
        addComponentHeader(R.string.component_keyframe, true, false, () -> {
            go.components.removeIf(c -> c instanceof KeyframeComponent);
            populateInspector(go);
        });

        View view = inflater.inflate(R.layout.inspector_keyframe_component, container, false);
        KeyframeComponent anim = go.getComponent(KeyframeComponent.class);


        CheckBox autoStartCheck = view.findViewById(R.id.keyframe_autostart);
        CheckBox loopCheck = view.findViewById(R.id.keyframe_loop);
        autoStartCheck.setChecked(anim.autoStart);
        loopCheck.setChecked(anim.looping);
        autoStartCheck.setOnCheckedChangeListener((b, isChecked) -> anim.autoStart = isChecked);
        loopCheck.setOnCheckedChangeListener((b, isChecked) -> anim.looping = isChecked);


        ImageButton playBtn = view.findViewById(R.id.btn_keyframe_play);
        ImageButton stopBtn = view.findViewById(R.id.btn_keyframe_stop);
        SeekBar timelineSlider = view.findViewById(R.id.keyframe_timeline_slider);


        playBtn.setOnClickListener(v -> {
            if (!isPreviewingAnimation) {

                KeyframeData kf = go.getComponent(KeyframeComponent.class).keyframes.get(0);
                tempPosition.set(kf.position);
                tempRotation.set(kf.rotation);
                tempScale.set(kf.scale);
                isPreviewingAnimation = true;
            }
            anim.isPlaying = true;
            anim.currentTime = 0;
            sceneManager.setEditorMode(false);
        });

        stopBtn.setOnClickListener(v -> {
            anim.currentTime = 0;
            anim.isPlaying = false;
            sceneManager.setEditorMode(true);
            if (isPreviewingAnimation) {

                go.transform.position.set(tempPosition);
                go.transform.rotation.set(tempRotation);
                go.transform.scale.set(tempScale);
                isPreviewingAnimation = false;
                populateInspector(go);
            }
        });

        timelineSlider.setMax(1000);
        timelineSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    float time = (progress / 1000f) * anim.getDuration();
                    sceneManager.setKeyframeAnimationTime(go.id, time);
                    populateInspector(go);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });


        LinearLayout listContainer = view.findViewById(R.id.keyframe_list_container);
        listContainer.removeAllViews();

        ArrayAdapter<String> easingAdapter = new ArrayAdapter<>(activity,
                R.layout.simple_spinner_item_white_text,
                activity.getResources().getStringArray(R.array.brick_easing_types));

        easingAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_white_text);

        for (int i = 0; i < anim.keyframes.size(); i++) {
            KeyframeData frame = anim.keyframes.get(i);
            View item = inflater.inflate(R.layout.inspector_keyframe_item, listContainer, false);

            EditText timeEdit = item.findViewById(R.id.edit_keyframe_time);
            Spinner easingSpinner = item.findViewById(R.id.spinner_keyframe_easing);
            ImageButton deleteBtn = item.findViewById(R.id.btn_delete_keyframe);

            timeEdit.setText(String.format(Locale.US, "%.2f", frame.time));


            timeEdit.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    try {
                        float newTime = Float.parseFloat(timeEdit.getText().toString());

                        frame.time = Math.max(0, newTime);
                        anim.sortKeyframes();

                        activity.runOnUiThread(() -> populateInspector(go));
                    } catch (Exception e) {
                        timeEdit.setText(String.format(Locale.US, "%.2f", frame.time));
                    }
                }
            });

            timeEdit.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    timeEdit.clearFocus();
                    return false;
                }
                return false;
            });
            easingSpinner.setAdapter(easingAdapter);
            easingSpinner.setSelection(frame.easingToNext.ordinal());


            if (i == 0) deleteBtn.setVisibility(View.GONE);

            easingSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                    frame.easingToNext = org.catrobat.catroid.content.EasingFunctions.EasingType.values()[pos];
                }
                @Override public void onNothingSelected(AdapterView<?> parent) {}
            });

            deleteBtn.setOnClickListener(v -> {
                anim.keyframes.remove(frame);
                populateInspector(go);
            });

            item.setOnClickListener(v -> {

                if (gizmo != null) {
                    gizmo.setSelectedKeyframe(go, frame);
                }

                populateInspector(go);
            });

            if (gizmo != null && gizmo.getSelectedKeyframe() == frame) {
                item.setBackgroundColor(0x559999FF);
            }

            listContainer.addView(item);
        }


        Button addBtn = view.findViewById(R.id.btn_add_keyframe);
        addBtn.setOnClickListener(v -> {
            KeyframeData newFrame = new KeyframeData();

            newFrame.position.set(go.transform.position);
            newFrame.rotation.set(go.transform.rotation);
            newFrame.scale.set(go.transform.scale);

            newFrame.time = anim.getDuration() + 1.0f;

            anim.keyframes.add(newFrame);
            anim.sortKeyframes();
            populateInspector(go);
        });

        container.addView(view);
    }

    private int libGdxColorToAndroidColor(com.badlogic.gdx.graphics.Color gdxColor) {
        int r = (int)(gdxColor.r * 255);
        int g = (int)(gdxColor.g * 255);
        int b = (int)(gdxColor.b * 255);
        int a = (int)(gdxColor.a * 255);
        return android.graphics.Color.argb(a, r, g, b);
    }

    private void createAnimationView(GameObject go) {
        addComponentHeader(R.string.component_animation, true,false, () -> {
            go.components.removeIf(c -> c instanceof AnimationComponent);
            sceneManager.playAnimationFromComponent(go);
            populateInspector(go);
        });

        View view = inflater.inflate(R.layout.inspector_animation, container, false);
        setWhiteTextToAllChildren((ViewGroup) view);

        Spinner animSpinner = view.findViewById(R.id.spinner_animations);
        EditText speedEditor = view.findViewById(R.id.edit_anim_speed);
        EditText transitionEditor = view.findViewById(R.id.edit_anim_transition);
        CheckBox loopCheckbox = view.findViewById(R.id.checkbox_anim_loop);

        Array<String> animationNamesGdx = sceneManager.getAnimationNames(go);
        List<String> animationNames = new ArrayList<>();
        animationNames.add("None");
        for(String name : animationNamesGdx) { animationNames.add(name); }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, R.layout.simple_spinner_item_white_text, animationNames);
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_white_text);
        animSpinner.setAdapter(adapter);

        AnimationComponent animComponent = go.getComponent(AnimationComponent.class);
        if (animComponent == null) {
            animComponent = new AnimationComponent();
            go.addComponent(animComponent);
        }

        if (animComponent.animationName != null) {
            int selection = animationNames.indexOf(animComponent.animationName);
            animSpinner.setSelection(Math.max(0, selection));
        } else {
            animSpinner.setSelection(0);
        }
        speedEditor.setText(String.valueOf(animComponent.speed));
        transitionEditor.setText(String.valueOf(animComponent.transitionTime));
        loopCheckbox.setChecked(animComponent.loops == -1);

        final AnimationComponent finalAnimComponent = animComponent;

        animSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = (String) parent.getItemAtPosition(position);
                finalAnimComponent.animationName = selected.equals("None") ? null : selected;
                sceneManager.playAnimationFromComponent(go);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        addSimpleTextListener(speedEditor, s -> {
            try { finalAnimComponent.speed = Float.parseFloat(s); } catch (Exception e) {}
            sceneManager.playAnimationFromComponent(go);
        });

        addSimpleTextListener(transitionEditor, s -> {
            try { finalAnimComponent.transitionTime = Float.parseFloat(s); } catch (Exception e) {}
            sceneManager.playAnimationFromComponent(go);
        });

        loopCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            finalAnimComponent.loops = isChecked ? -1 : 1;
            sceneManager.playAnimationFromComponent(go);
        });

        container.addView(view);
    }

    private void createCameraView(GameObject go) {
        addComponentHeader(R.string.component_camera, true, false, new Runnable() {
            @Override
            public void run() {
                go.components.removeIf(c -> c instanceof CameraComponent);
                sceneManager.engine.removeEditorProxy(go.id);
                populateInspector(go);
            }
        });

        View view = inflater.inflate(R.layout.inspector_camera, container, false);
        setWhiteTextToAllChildren((ViewGroup) view);
        CameraComponent camComp = go.getComponent(CameraComponent.class);

        CheckBox mainCamCheck = view.findViewById(R.id.check_main_camera);
        EditText fovEditor = view.findViewById(R.id.edit_cam_fov);
        EditText nearEditor = view.findViewById(R.id.edit_cam_near);
        EditText farEditor = view.findViewById(R.id.edit_cam_far);

        mainCamCheck.setChecked(camComp.isMainCamera);
        fovEditor.setText(String.valueOf(camComp.fieldOfView));
        nearEditor.setText(String.valueOf(camComp.nearPlane));
        farEditor.setText(String.valueOf(camComp.farPlane));

        mainCamCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                for (GameObject otherGo : sceneManager.getAllGameObjects().values()) {
                    CameraComponent otherCam = otherGo.getComponent(CameraComponent.class);
                    if (otherGo != go && otherCam != null) {
                        otherCam.isMainCamera = false;
                    }
                }
            }
            camComp.isMainCamera = isChecked;
            sceneManager.setCameraComponent(go, camComp);
            if(isChecked) populateInspector(go);
        });

        TextWatcher watcher = new DelayedTextWatcher(() -> {
            try {
                camComp.fieldOfView = Float.parseFloat(fovEditor.getText().toString());
                camComp.nearPlane = Float.parseFloat(nearEditor.getText().toString());
                camComp.farPlane = Float.parseFloat(farEditor.getText().toString());
                sceneManager.setCameraComponent(go, camComp);
            } catch (Exception e) {}
        });

        fovEditor.addTextChangedListener(watcher);
        nearEditor.addTextChangedListener(watcher);
        farEditor.addTextChangedListener(watcher);

        container.addView(view);
    }

    private void createTransformView(GameObject go) {
        addComponentHeader(R.string.component_transform, false, false, null);
        View view = inflater.inflate(R.layout.inspector_transform, container, false);
        if (view instanceof ViewGroup) {
            setWhiteTextToAllChildren((ViewGroup) view);
        }

        ImageButton alignPosBtn = view.findViewById(R.id.btn_align_position_with_view);
        ImageButton alignRotBtn = view.findViewById(R.id.btn_align_rotation_with_view);

        alignPosBtn.setOnClickListener(v -> {
            if (threeDManager != null && selectedObject != null) {
                Vector3 cameraPos = threeDManager.getCameraPosition();
                selectedObject.transform.position.set(cameraPos);
                populateInspector(selectedObject);
            }
        });

        alignRotBtn.setOnClickListener(v -> {
            if (threeDManager != null && selectedObject != null) {
                Quaternion cameraRot = new Quaternion();
                threeDManager.getCamera().view.getRotation(cameraRot, true).conjugate();

                selectedObject.transform.rotation.set(cameraRot);
                populateInspector(selectedObject);
            }
        });

        TransformComponent t = go.transform;

        updateVector3Fields(view, R.id.edit_pos_x, R.id.edit_pos_y, R.id.edit_pos_z, t.position);
        updateEulerFields(view, R.id.edit_rot_x, R.id.edit_rot_y, R.id.edit_rot_z, t.rotation);
        updateVector3Fields(view, R.id.edit_scale_x, R.id.edit_scale_y, R.id.edit_scale_z, t.scale);

        addVector3Listener(go, view, R.id.edit_pos_x, R.id.edit_pos_y, R.id.edit_pos_z, sceneManager::setPosition);
        addEulerListener(go, view, R.id.edit_rot_x, R.id.edit_rot_y, R.id.edit_rot_z, sceneManager::setRotation);
        addVector3Listener(go, view, R.id.edit_scale_x, R.id.edit_scale_y, R.id.edit_scale_z, sceneManager::setScale);

        container.addView(view);
    }

    private void createRenderView(GameObject go) {
        addComponentHeader(R.string.component_render, true, false, () -> {
            sceneManager.removeRenderComponent(go);
            populateInspector(go);
        });
        View view = inflater.inflate(R.layout.inspector_render, container, false);
        setWhiteTextToAllChildren((ViewGroup) view);

        RenderComponent render = go.getComponent(RenderComponent.class);
        TextView pathText = view.findViewById(R.id.text_model_path);
        Button selectButton = view.findViewById(R.id.btn_select_model);

        String modelName = render.modelFileName != null ? render.modelFileName : "No model selected";

        if (modelName.startsWith("primitive:")) {
            String shapeName = modelName.substring("primitive:".length());
            shapeName = shapeName.substring(0, 1).toUpperCase() + shapeName.substring(1);
            pathText.setText("Primitive Shape (" + shapeName + ")");
            selectButton.setVisibility(View.GONE);
        }
        else {
            pathText.setText(modelName);
            selectButton.setOnClickListener(v -> showModelPicker(go));
        }

        container.addView(view);
    }

    private void createMaterialView(GameObject go) {
        MaterialComponent material = go.getComponent(MaterialComponent.class);
        if (material == null) return;

        addComponentHeader(R.string.component_material, true, false, () -> {
            go.components.removeIf(c -> c instanceof MaterialComponent);

            if (go.hasComponent(RenderComponent.class)) {
                threeDManager.applyPBRMaterial(go.id, new MaterialComponent());
            }

            populateInspector(go);
        });
        View view = inflater.inflate(R.layout.inspector_material, container, false);
        setWhiteTextToAllChildren((ViewGroup) view);

        setupColorPicker(go, view.findViewById(R.id.btn_material_color), material.baseColor,
                newColor -> material.baseColor.set(newColor));
        setupTextureSlot(go, view.findViewById(R.id.slot_base_color_texture), material.baseColorTexturePath,
                newPath -> material.baseColorTexturePath = newPath);


        setupSlider(go, view.findViewById(R.id.slider_metallic), material.metallic,
                newValue -> material.metallic = newValue);
        setupSlider(go, view.findViewById(R.id.slider_roughness), material.roughness,
                newValue -> material.roughness = newValue);


        setupColorPicker(go, view.findViewById(R.id.btn_emissive_color), material.emissiveColor,
                newColor -> material.emissiveColor.set(newColor));
        setupTextureSlot(go, view.findViewById(R.id.slot_emissive_texture), material.emissiveTexturePath,
                newPath -> material.emissiveTexturePath = newPath);


        setupTextureSlot(go, view.findViewById(R.id.slot_normal_texture), material.normalTexturePath,
                newPath -> material.normalTexturePath = newPath);
        setupTextureSlot(go, view.findViewById(R.id.slot_mr_texture), material.metallicRoughnessTexturePath,
                newPath -> material.metallicRoughnessTexturePath = newPath);
        setupTextureSlot(go, view.findViewById(R.id.slot_occlusion_texture), material.occlusionTexturePath,
                newPath -> material.occlusionTexturePath = newPath);

        LinearLayout paramsContainer = view.findViewById(R.id.material_params_container);
        if (paramsContainer == null && view instanceof LinearLayout) paramsContainer = (LinearLayout) view;

        if (paramsContainer != null) {
            addSimpleFloatInput(paramsContainer, "UV Scale X", material.uvScaleX, v -> {
                material.uvScaleX = v;
                sceneManager.setMaterialComponent(go, material);
            });
            addSimpleFloatInput(paramsContainer, "UV Scale Y", material.uvScaleY, v -> {
                material.uvScaleY = v;
                sceneManager.setMaterialComponent(go, material);
            });
        }

        container.addView(view);
    }

    private void createFogView(GameObject go) {
        addComponentHeader(R.string.component_fog, true, false, () -> {
            go.components.removeIf(c -> c instanceof FogComponent);
            populateInspector(go);
        });

        View view = inflater.inflate(R.layout.inspector_fog, container, false);
        setWhiteTextToAllChildren((ViewGroup) view);
        FogComponent fog = go.getComponent(FogComponent.class);

        CheckBox enabledCheck = view.findViewById(R.id.fog_enabled);
        Spinner typeSpinner = view.findViewById(R.id.spinner_fog_type);
        Button colorButton = view.findViewById(R.id.btn_fog_color);
        View expLayout = view.findViewById(R.id.layout_fog_exp);
        EditText densityEdit = view.findViewById(R.id.edit_fog_density);
        View linearLayout = view.findViewById(R.id.layout_fog_linear);
        EditText startEdit = view.findViewById(R.id.edit_fog_start);
        EditText endEdit = view.findViewById(R.id.edit_fog_end);
        EditText heightEdit = view.findViewById(R.id.edit_fog_height);

        enabledCheck.setChecked(fog.isEnabled);

        ArrayAdapter<FogComponent.FogType> adapter = new ArrayAdapter<>(activity, R.layout.simple_spinner_item_white_text, FogComponent.FogType.values());
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_white_text);
        typeSpinner.setAdapter(adapter);
        typeSpinner.setSelection(fog.type.ordinal());

        colorButton.setBackgroundColor(libGdxColorToAndroidColor(fog.color));
        densityEdit.setText(String.valueOf(fog.density));
        startEdit.setText(String.valueOf(fog.startDistance));
        endEdit.setText(String.valueOf(fog.endDistance));
        heightEdit.setText(String.valueOf(fog.heightFalloff));

        Runnable updateVisibility = () -> {
            FogComponent.FogType type = fog.type;
            expLayout.setVisibility(type == FogComponent.FogType.EXPONENTIAL || type == FogComponent.FogType.EXPONENTIAL_SQUARED ? View.VISIBLE : View.GONE);
            linearLayout.setVisibility(type == FogComponent.FogType.LINEAR ? View.VISIBLE : View.GONE);
        };
        updateVisibility.run();

        enabledCheck.setOnCheckedChangeListener((v, isChecked) -> fog.isEnabled = isChecked);

        typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                fog.type = FogComponent.FogType.values()[position];
                updateVisibility.run();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        final int initialAndroidColor = libGdxColorToAndroidColor(fog.color);
        colorButton.setBackgroundColor(initialAndroidColor);

        colorButton.setOnClickListener(v -> {
            ColorPickerDialogBuilder
                    .with(activity)
                    .setTitle("Choose color")
                    .initialColor(initialAndroidColor)
                    .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                    .density(12)
                    .setPositiveButton("OK", (dialog, selectedColor, allColors) -> {
                        colorButton.setBackgroundColor(selectedColor);

                        int r = android.graphics.Color.red(selectedColor);
                        int g = android.graphics.Color.green(selectedColor);
                        int b = android.graphics.Color.blue(selectedColor);

                        fog.color.set(r / 255f, g / 255f, b / 255f, 1.0f);
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> {})
                    .build()
                    .show();
        });

        addSimpleTextListener(densityEdit, s -> { try { fog.density = Float.parseFloat(s); } catch (Exception e) {} });
        addSimpleTextListener(startEdit, s -> { try { fog.startDistance = Float.parseFloat(s); } catch (Exception e) {} });
        addSimpleTextListener(endEdit, s -> { try { fog.endDistance = Float.parseFloat(s); } catch (Exception e) {} });
        addSimpleTextListener(heightEdit, s -> { try { fog.heightFalloff = Float.parseFloat(s); } catch (Exception e) {} });

        container.addView(view);
    }

    private void createPostProcessingView(GameObject go) {
        PostProcessingComponent pp = go.getComponent(PostProcessingComponent.class);


        addComponentHeader(R.string.component_postprocessing, true, false, () -> {
            go.components.remove(pp);

            if (threeDManager != null) {
                threeDManager.postprocessingEnabled = false;

                pp.isActive = false;
                threeDManager.updatePostProcessing(pp);
            }
            populateInspector(go);
        });

        View view = inflater.inflate(R.layout.inspector_postprocessing_main, container, false);
        setWhiteTextToAllChildren((ViewGroup) view);


        CheckBox activeCheck = view.findViewById(R.id.check_pp_active);
        activeCheck.setChecked(pp.isActive);
        activeCheck.setOnCheckedChangeListener((v, isChecked) -> {
            pp.isActive = isChecked;
            threeDManager.updatePostProcessing(pp);
        });

        EditText qualityEdit = view.findViewById(R.id.edit_pp_quality);
        qualityEdit.setText(String.format(Locale.US, "%.2f", pp.qualityScale));
        addSimpleTextListener(qualityEdit, s -> {
            try {
                pp.qualityScale = Float.parseFloat(s);
                threeDManager.updatePostProcessing(pp);
            } catch(Exception e){}
        });


        LinearLayout effectsContainer = view.findViewById(R.id.container_effects_list);
        effectsContainer.removeAllViews();


        for (int i = 0; i < pp.effects.size(); i++) {
            PostProcessingData effectData = pp.effects.get(i);
            addEffectUiItem(effectsContainer, effectData, pp, go);
        }


        Button addEffectBtn = view.findViewById(R.id.btn_add_effect);
        addEffectBtn.setOnClickListener(v -> showAddEffectDialog(pp, go));

        container.addView(view);
    }

    private void addEffectColorParam(LinearLayout parent, String label, com.badlogic.gdx.graphics.Color initialColor, ColorConsumer onUpdate) {
        View view = inflater.inflate(R.layout.inspector_param_color, parent, false);
        ((TextView)view.findViewById(R.id.text_param_name)).setText(label);

        Button colorBtn = view.findViewById(R.id.btn_color_picker);
        colorBtn.setBackgroundColor(libGdxColorToAndroidColor(initialColor));

        colorBtn.setOnClickListener(v -> {
            ColorPickerDialogBuilder
                    .with(activity)
                    .setTitle("Choose " + label)
                    .initialColor(libGdxColorToAndroidColor(initialColor))
                    .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                    .density(12)
                    .setPositiveButton("OK", (dialog, selectedColor, allColors) -> {
                        colorBtn.setBackgroundColor(selectedColor);
                        float r = android.graphics.Color.red(selectedColor) / 255f;
                        float g = android.graphics.Color.green(selectedColor) / 255f;
                        float b = android.graphics.Color.blue(selectedColor) / 255f;
                        float a = android.graphics.Color.alpha(selectedColor) / 255f;
                        onUpdate.accept(new com.badlogic.gdx.graphics.Color(r, g, b, a));
                    })
                    .setNegativeButton("Cancel", null)
                    .build()
                    .show();
        });

        parent.addView(view);
    }

    private void addEffectUiItem(LinearLayout container, PostProcessingData data, PostProcessingComponent pp, GameObject go) {
        View effectView = inflater.inflate(R.layout.inspector_effect_item, container, false);

        TextView title = effectView.findViewById(R.id.text_effect_name);
        title.setText(data.getType());


        android.widget.ImageButton deleteBtn = effectView.findViewById(R.id.btn_delete_effect);
        deleteBtn.setOnClickListener(v -> {
            pp.effects.remove(data);
            threeDManager.updatePostProcessing(pp);

            populateInspector(go);
        });


        CheckBox enableCheck = effectView.findViewById(R.id.check_effect_enable);
        enableCheck.setChecked(data.isEnabled);
        enableCheck.setOnCheckedChangeListener((v, isChecked) -> {
            data.isEnabled = isChecked;
            threeDManager.updatePostProcessing(pp);
        });

        LinearLayout paramsContainer = effectView.findViewById(R.id.container_effect_params);


        if (data instanceof PostProcessingData.Bloom) {
            PostProcessingData.Bloom b = (PostProcessingData.Bloom) data;
            addFloatParam(paramsContainer, "Threshold", b.threshold, v -> { b.threshold = v; updatePP(pp); });
            addFloatParam(paramsContainer, "Intensity", b.intensity, v -> { b.intensity = v; updatePP(pp); });
            addFloatParam(paramsContainer, "Blur Amount", b.blurAmount, v -> { b.blurAmount = v; updatePP(pp); });
            addFloatParam(paramsContainer, "Blur Size", b.size, v -> { b.size = v; updatePP(pp); });
            addFloatParam(paramsContainer, "Blur Passes", b.blurPasses, v -> { int v2; if ((int) v > 0) { v2 = (int) v; } else {v2 = 1;}; b.blurPasses = v2; updatePP(pp); });

        }
        else if (data instanceof PostProcessingData.Levels) {
            PostProcessingData.Levels l = (PostProcessingData.Levels) data;
            addFloatParam(paramsContainer, "Contrast", l.contrast, v -> { l.contrast = v; updatePP(pp); });
            addFloatParam(paramsContainer, "Saturation", l.saturation, v -> { l.saturation = v; updatePP(pp); });
            addFloatParam(paramsContainer, "Gamma", l.gamma, v -> { l.gamma = v; updatePP(pp); });
        }
        else if (data instanceof PostProcessingData.RayTracing) {
            PostProcessingData.RayTracing rt = (PostProcessingData.RayTracing) data;
            addFloatParam(paramsContainer, "Reflectivity Multi", rt.reflectivity, v -> { rt.reflectivity = v; updatePP(pp); });
            addFloatParam(paramsContainer, "Edge Fade", rt.edgeFade, v -> { rt.edgeFade = v; updatePP(pp); });
            addFloatParam(paramsContainer, "Thickness", rt.thickness, v -> { rt.thickness = v; updatePP(pp); });
            addFloatParam(paramsContainer, "Distance", rt.maxDistance, v -> { rt.maxDistance = v; updatePP(pp); });
            addFloatParam(paramsContainer, "Stride", rt.stride, v -> { rt.stride = v; updatePP(pp); });
            addFloatParam(paramsContainer, "Steps", rt.steps, v -> { rt.steps = (int)v; updatePP(pp); });
        }
        else if (data instanceof PostProcessingData.VolumetricFog) {
            PostProcessingData.VolumetricFog vf = (PostProcessingData.VolumetricFog) data;
            addFloatParam(paramsContainer, "Density", vf.density, v -> { vf.density = v; updatePP(pp); });
            addFloatParam(paramsContainer, "Scattering", vf.scattering, v -> { vf.scattering = v; updatePP(pp); });
            addFloatParam(paramsContainer, "Max Distance", vf.maxDistance, v -> { vf.maxDistance = v; updatePP(pp); });
            addFloatParam(paramsContainer, "Steps", vf.steps, v -> { vf.steps = (int)v; updatePP(pp); });
        }
        else if (data instanceof PostProcessingData.Upscaler) {
            PostProcessingData.Upscaler up = (PostProcessingData.Upscaler) data;
            addFloatParam(paramsContainer, "Sharpness", up.sharpness, v -> { up.sharpness = v; updatePP(pp); });
        }
        else if (data instanceof PostProcessingData.HeightFog) {
            PostProcessingData.HeightFog fog = (PostProcessingData.HeightFog) data;
            addFloatParam(paramsContainer, "Density", fog.density, v -> { fog.density = v; updatePP(pp); });
            addFloatParam(paramsContainer, "Falloff", fog.falloff, v -> { fog.falloff = v; updatePP(pp); });
            addFloatParam(paramsContainer, "Height Offset", fog.height, v -> { fog.height = v; updatePP(pp); });

            addEffectColorParam(paramsContainer, "Fog Color", fog.color, newColor -> {
                fog.color.set(newColor);
                updatePP(pp);
            });
        }
        else if (data instanceof PostProcessingData.DepthOfField) {
            PostProcessingData.DepthOfField dof = (PostProcessingData.DepthOfField) data;

            CheckBox autoFocusCheck = new CheckBox(activity);
            autoFocusCheck.setText("Auto Focus (Center Screen)");
            autoFocusCheck.setTextColor(android.graphics.Color.WHITE);
            autoFocusCheck.setChecked(dof.autoFocus);
            paramsContainer.addView(autoFocusCheck);

            LinearLayout speedContainer = new LinearLayout(activity);
            speedContainer.setOrientation(LinearLayout.VERTICAL);
            paramsContainer.addView(speedContainer);
            addFloatParam(speedContainer, "Auto Focus Speed", dof.autoFocusSpeed, v -> { dof.autoFocusSpeed = v; updatePP(pp); });

            LinearLayout distanceContainer = new LinearLayout(activity);
            distanceContainer.setOrientation(LinearLayout.VERTICAL);
            paramsContainer.addView(distanceContainer);
            addFloatParam(distanceContainer, "Manual Focus Dist", dof.focusDistance, v -> { dof.focusDistance = v; updatePP(pp); });
            Runnable updateDofVisibility = () -> {
                speedContainer.setVisibility(dof.autoFocus ? View.VISIBLE : View.GONE);
                distanceContainer.setAlpha(dof.autoFocus ? 0.4f : 1.0f);
                for(int i = 0; i < distanceContainer.getChildCount(); i++) {
                    distanceContainer.getChildAt(i).setEnabled(!dof.autoFocus);
                    distanceContainer.getChildAt(i).setClickable(!dof.autoFocus);
                }
            };
            updateDofVisibility.run();

            autoFocusCheck.setOnCheckedChangeListener((v, isChecked) -> {
                dof.autoFocus = isChecked;
                updateDofVisibility.run();
                updatePP(pp);
            });
            addFloatParam(paramsContainer, "Focus Range", dof.focusRange, v -> { dof.focusRange = v; updatePP(pp); });
            addFloatParam(paramsContainer, "Transition", dof.transition, v -> { dof.transition = v; updatePP(pp); });
            addFloatParam(paramsContainer, "Blur Size", dof.blurSize, v -> { dof.blurSize = v; updatePP(pp); });
        }
        else if (data instanceof PostProcessingData.GodRays) {
            PostProcessingData.GodRays gr = (PostProcessingData.GodRays) data;
            addFloatParam(paramsContainer, "Exposure", gr.exposure, v -> { gr.exposure = v; updatePP(pp); });
            addFloatParam(paramsContainer, "Decay", gr.decay, v -> { gr.decay = v; updatePP(pp); });
            addFloatParam(paramsContainer, "Density", gr.density, v -> { gr.density = v; updatePP(pp); });
            addFloatParam(paramsContainer, "Weight", gr.weight, v -> { gr.weight = v; updatePP(pp); });
        }
        else if (data instanceof PostProcessingData.SSAO) {
            PostProcessingData.SSAO ssao = (PostProcessingData.SSAO) data;
            addFloatParam(paramsContainer, "Bias", ssao.bias, v -> { ssao.bias = v; updatePP(pp); });
            addFloatParam(paramsContainer, "Intensity", ssao.intensity, v -> { ssao.intensity = v; updatePP(pp); });
            addFloatParam(paramsContainer, "Radius", ssao.radius, v -> { ssao.radius = v; updatePP(pp); });
        }
        else if (data instanceof PostProcessingData.Vignette) {
            PostProcessingData.Vignette v = (PostProcessingData.Vignette) data;
            addFloatParam(paramsContainer, "Intensity", v.intensity, val -> { v.intensity = val; updatePP(pp); });
            addFloatParam(paramsContainer, "Saturation", v.saturation, val -> { v.saturation = val; updatePP(pp); });
        }
        else if (data instanceof PostProcessingData.Grain) {
            PostProcessingData.Grain g = (PostProcessingData.Grain) data;
            addFloatParam(paramsContainer, "Amount", g.amount, val -> { g.amount = val; updatePP(pp); });
        }
        else if (data instanceof PostProcessingData.Chromatic) {
            PostProcessingData.Chromatic c = (PostProcessingData.Chromatic) data;
            addFloatParam(paramsContainer, "Max Distort", c.maxDistortion, val -> { c.maxDistortion = val; updatePP(pp); });
            addFloatParam(paramsContainer, "Strength", c.strength, val -> { c.strength = val; updatePP(pp); });
        }
        else if (data instanceof PostProcessingData.RadialBlur) {
            PostProcessingData.RadialBlur r = (PostProcessingData.RadialBlur) data;
            addFloatParam(paramsContainer, "Blur Passes", r.blurPasses, v -> { int v2; if ((int) v > 0) { v2 = (int) v; } else {v2 = 1;}; r.blurPasses = v2; updatePP(pp); });
            addFloatParam(paramsContainer, "Strength", r.strength, v -> { r.strength = v; updatePP(pp); });
            addFloatParam(paramsContainer, "Blur Size", r.size, v -> { r.size = v; updatePP(pp); });
        }
        else if (data instanceof PostProcessingData.OldTv) {
            PostProcessingData.OldTv tv = (PostProcessingData.OldTv) data;
            addFloatParam(paramsContainer, "Noise", tv.strength, v -> { tv.strength = v; updatePP(pp); });
        }
        else if (data instanceof PostProcessingData.Crt) {
        }
        else if (data instanceof PostProcessingData.Fisheye) {
        }
        else if (data instanceof PostProcessingData.Water) {
            PostProcessingData.Water w = (PostProcessingData.Water) data;
            addFloatParam(paramsContainer, "Speed", w.speed, v -> { w.speed = v; updatePP(pp); });
            addFloatParam(paramsContainer, "Amount", w.amount, v -> { w.amount = v; updatePP(pp); });
        }
        else if (data instanceof PostProcessingData.MotionBlur) {
            PostProcessingData.MotionBlur mb = (PostProcessingData.MotionBlur) data;

            addFloatParam(paramsContainer, "Blur Opacity", mb.blurOpacity, v -> {
                mb.blurOpacity = Math.max(0f, Math.min(0.99f, v));
                updatePP(pp);
            });
        }
        else if (data instanceof PostProcessingData.LensFlare) {
            PostProcessingData.LensFlare lf = (PostProcessingData.LensFlare) data;
            addFloatParam(paramsContainer, "Intensity", lf.intensity, v -> { lf.intensity = v; updatePP(pp); });
            addFloatParam(paramsContainer, "Threshold", lf.threshold, v -> { lf.threshold = v; updatePP(pp); });
            addFloatParam(paramsContainer, "Dispersal", lf.dispersal, v -> { lf.dispersal = v; updatePP(pp); });
            addFloatParam(paramsContainer, "Size", lf.size, v -> { lf.size = v; updatePP(pp); });

        }
        else if (data instanceof PostProcessingData.Gaussian) {
            PostProcessingData.Gaussian g = (PostProcessingData.Gaussian) data;
            addFloatParam(paramsContainer, "Amount", g.amount, v -> { g.amount = v; updatePP(pp); });
            addFloatParam(paramsContainer, "Blur Passes", g.passes, v -> { int v2; if ((int) v > 0) { v2 = (int) v; } else {v2 = 1;}; g.passes = v2; updatePP(pp); });
            addFloatParam(paramsContainer, "Blur Size", g.size, v -> { g.size = v; updatePP(pp); });
        }
        else if (data instanceof PostProcessingData.Zoom) {
            PostProcessingData.Zoom z = (PostProcessingData.Zoom) data;
            addFloatParam(paramsContainer, "Strength", z.zoom, v -> { z.zoom = v; updatePP(pp); });
            addFloatParam(paramsContainer, "Origin X", z.originX, v -> { z.originX = v; updatePP(pp); });
            addFloatParam(paramsContainer, "Origin Y", z.originY, v -> { z.originY = v; updatePP(pp); });
        }
        else if (data instanceof PostProcessingData.EyeAdaptation) {
            PostProcessingData.EyeAdaptation ea = (PostProcessingData.EyeAdaptation) data;
            addFloatParam(paramsContainer, "Target Luminance", ea.targetLuminance, v -> { ea.targetLuminance = v; /* No need to call update here */ });
            addFloatParam(paramsContainer, "Speed", ea.speed, v -> { ea.speed = v; });
            addFloatParam(paramsContainer, "Min Exposure", ea.minExposure, v -> { ea.minExposure = v; });
            addFloatParam(paramsContainer, "Max Exposure", ea.maxExposure, v -> { ea.maxExposure = v; });
        }

        container.addView(effectView);
    }

    private void updatePP(PostProcessingComponent pp) {
        if (threeDManager != null) {
            threeDManager.updatePostProcessing(pp);
        }
    }


    private void addFloatParam(LinearLayout parent, String name, float value, FloatConsumer onChange) {
        View view = inflater.inflate(R.layout.inspector_param_float, parent, false);
        ((TextView)view.findViewById(R.id.text_param_name)).setText(name);
        EditText edit = view.findViewById(R.id.edit_param_value);
        edit.setText(String.format(Locale.US, "%.3f", value));

        addSimpleTextListener(edit, s -> {
            try { onChange.accept(Float.parseFloat(s)); } catch(Exception e){}
        });
        parent.addView(view);
    }

    private void setupFloatParam(View parent, int viewId, String label, float initialValue, FloatConsumer onUpdate) {
        View paramView = parent.findViewById(viewId);
        ((TextView) paramView.findViewById(R.id.text_param_name)).setText(label);
        EditText editText = paramView.findViewById(R.id.edit_param_value);
        editText.setText(String.format(Locale.US, "%.2f", initialValue));
        addSimpleTextListener(editText, s -> {
            try { onUpdate.accept(Float.parseFloat(s)); } catch (Exception ignored) {}
        });
    }

    private void createParticleSystem3DView(GameObject go) {
        ParticleSystem3DComponent ps = go.getComponent(ParticleSystem3DComponent.class);
        if (ps == null) return;

        addComponentHeader(R.string.component_particle_3d, true, false, () -> {
            go.components.removeIf(c -> c instanceof ParticleSystem3DComponent);
            if (threeDManager != null) threeDManager.removeParticleEffect3D(go.id);
            if (threeDManager != null) threeDManager.removeEditorProxy(go.id);
            populateInspector(go);
        });

        LinearLayout mainLayout = new LinearLayout(activity);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int)(8 * activity.getResources().getDisplayMetrics().density);
        mainLayout.setPadding(pad, pad, pad, pad);
        container.addView(mainLayout);

        addSectionHeader(mainLayout, activity.getString(R.string.particle_main));

        addCheckbox(mainLayout, activity.getString(R.string.particle_looping), ps.looping, v -> { ps.looping = v; updatePS3DImmediate(go); });
        addCheckbox(mainLayout, activity.getString(R.string.particle_prewarm), ps.prewarm, v -> { ps.prewarm = v; updatePS3DImmediate(go); });
        addSimpleFloatInput(mainLayout, activity.getString(R.string.particle_duration), ps.duration, v -> { ps.duration = v; updatePS3D(go); });
        addSimpleFloatInput(mainLayout, activity.getString(R.string.particle_max_particles), ps.maxParticles, v -> { ps.maxParticles = (int)v; updatePS3D(go); });

        addMinMaxCurveEditor(mainLayout, activity.getString(R.string.particle_start_lifetime), ps.startLifetime, go);
        addMinMaxCurveEditor(mainLayout, activity.getString(R.string.particle_start_speed), ps.startSpeed, go);
        addMinMaxCurveEditor(mainLayout, activity.getString(R.string.particle_start_size), ps.startSize, go);
        addMinMaxCurveEditor(mainLayout, activity.getString(R.string.particle_gravity_modifier), ps.gravityModifier, go);

        addSpinnerEnum(mainLayout, activity.getString(R.string.particle_sim_space), ParticleSystem3DComponent.SimulationSpace.values(),
                ps.simulationSpace.ordinal(), v -> { ps.simulationSpace = v; updatePS3DImmediate(go); });


        addModuleSection(mainLayout, activity.getString(R.string.particle_emission), ps.emission.enabled, v -> { ps.emission.enabled = v; updatePS3D(go); }, () -> {
            LinearLayout emLayout = new LinearLayout(activity);
            emLayout.setOrientation(LinearLayout.VERTICAL);
            addMinMaxCurveEditor(emLayout, activity.getString(R.string.particle_rate_over_time), ps.emission.rateOverTime, go);
            addMinMaxCurveEditor(emLayout, activity.getString(R.string.particle_rate_over_distance), ps.emission.rateOverDistance, go);

            addSectionHeader(emLayout, activity.getString(R.string.particle_bursts));
            for (int i = 0; i < ps.emission.bursts.size(); i++) {
                ParticleSystem3DComponent.Burst burst = ps.emission.bursts.get(i);
                int idx = i;
                LinearLayout burstRow = new LinearLayout(activity);
                burstRow.setOrientation(LinearLayout.HORIZONTAL);

                addSmallFloatInput(burstRow, activity.getString(R.string.particle_time), burst.time, v -> { burst.time = v; updatePS3D(go); });
                addSmallFloatInput(burstRow, activity.getString(R.string.particle_count), burst.count.constantMax, v -> { burst.count = new ParticleSystem3DComponent.MinMaxCurve(v); updatePS3D(go); });
                addSmallFloatInput(burstRow, activity.getString(R.string.particle_prob), burst.probability, v -> { burst.probability = v; updatePS3D(go); });

                Button delBurst = new Button(activity, null, 0, android.R.style.Widget_Material_Button_Small);
                delBurst.setText("X");
                delBurst.setTextColor(android.graphics.Color.RED);
                delBurst.setOnClickListener(v -> { ps.emission.bursts.remove(idx); updatePS3D(go); populateInspector(go); });
                burstRow.addView(delBurst);

                emLayout.addView(burstRow);
            }
            Button addBurst = new Button(activity, null, 0, android.R.style.Widget_Material_Button_Small);
            addBurst.setText("+");
            addBurst.setOnClickListener(v -> { ps.emission.bursts.add(new ParticleSystem3DComponent.Burst(0, 30)); updatePS3D(go); populateInspector(go); });
            emLayout.addView(addBurst);

            return emLayout;
        });

        addModuleSection(mainLayout, activity.getString(R.string.particle_shape), ps.shape.enabled, v -> { ps.shape.enabled = v; updatePS3D(go); }, () -> {
            LinearLayout shLayout = new LinearLayout(activity);
            shLayout.setOrientation(LinearLayout.VERTICAL);

            addSpinnerEnum(shLayout, activity.getString(R.string.particle_shape_type), ParticleSystem3DComponent.ShapeType.values(),
                    ps.shape.type.ordinal(), v -> { ps.shape.type = v; updatePS3D(go); populateInspector(go); });

            switch (ps.shape.type) {
                case CONE:
                    addSimpleFloatInput(shLayout, activity.getString(R.string.particle_angle), ps.shape.coneAngle, v -> { ps.shape.coneAngle = v; updatePS3D(go); });
                    addSimpleFloatInput(shLayout, activity.getString(R.string.particle_radius), ps.shape.coneRadius, v -> { ps.shape.coneRadius = v; updatePS3D(go); });
                    addSimpleFloatInput(shLayout, activity.getString(R.string.particle_length), ps.shape.coneLength, v -> { ps.shape.coneLength = v; updatePS3D(go); });
                    break;
                case SPHERE: case HEMISPHERE:
                    addSimpleFloatInput(shLayout, activity.getString(R.string.particle_radius), ps.shape.sphereRadius, v -> { ps.shape.sphereRadius = v; updatePS3D(go); });
                    break;
                case BOX:
                    addSimpleFloatInput(shLayout, "X", ps.shape.boxSize.x, v -> { ps.shape.boxSize.x = v; updatePS3D(go); });
                    addSimpleFloatInput(shLayout, "Y", ps.shape.boxSize.y, v -> { ps.shape.boxSize.y = v; updatePS3D(go); });
                    addSimpleFloatInput(shLayout, "Z", ps.shape.boxSize.z, v -> { ps.shape.boxSize.z = v; updatePS3D(go); });
                    break;
                case CIRCLE:
                    addSimpleFloatInput(shLayout, activity.getString(R.string.particle_radius), ps.shape.circleRadius, v -> { ps.shape.circleRadius = v; updatePS3D(go); });
                    addSimpleFloatInput(shLayout, activity.getString(R.string.particle_arc), ps.shape.circleArc, v -> { ps.shape.circleArc = v; updatePS3D(go); });
                    break;
                case EDGE:
                    addSimpleFloatInput(shLayout, activity.getString(R.string.particle_length), ps.shape.edgeLength, v -> { ps.shape.edgeLength = v; updatePS3D(go); });
                    break;
            }

            addSpinnerEnum(shLayout, activity.getString(R.string.particle_emit_from), ParticleSystem3DComponent.EmitFrom.values(),
                    ps.shape.emitFrom.ordinal(), v -> { ps.shape.emitFrom = v; updatePS3D(go); });

            return shLayout;
        });

        addModuleSection(mainLayout, activity.getString(R.string.particle_velocity_over_lifetime), ps.velocityOverLifetime.enabled,
                v -> { ps.velocityOverLifetime.enabled = v; updatePS3D(go); }, () -> {
                    LinearLayout vLayout = new LinearLayout(activity);
                    vLayout.setOrientation(LinearLayout.VERTICAL);
                    addMinMaxCurveEditor(vLayout, activity.getString(R.string.particle_linear_x), ps.velocityOverLifetime.x, go);
                    addMinMaxCurveEditor(vLayout, activity.getString(R.string.particle_linear_y), ps.velocityOverLifetime.y, go);
                    addMinMaxCurveEditor(vLayout, activity.getString(R.string.particle_linear_z), ps.velocityOverLifetime.z, go);
                    addMinMaxCurveEditor(vLayout, activity.getString(R.string.particle_orbital_y), ps.velocityOverLifetime.orbitalY, go);
                    addMinMaxCurveEditor(vLayout, activity.getString(R.string.particle_radial), ps.velocityOverLifetime.radial, go);
                    addMinMaxCurveEditor(vLayout, activity.getString(R.string.particle_speed_modifier), ps.velocityOverLifetime.speedModifier, go);
                    return vLayout;
                });

        addModuleSection(mainLayout, activity.getString(R.string.particle_force_over_lifetime), ps.forceOverLifetime.enabled,
                v -> { ps.forceOverLifetime.enabled = v; updatePS3D(go); }, () -> {
                    LinearLayout fLayout = new LinearLayout(activity);
                    fLayout.setOrientation(LinearLayout.VERTICAL);
                    addMinMaxCurveEditor(fLayout, activity.getString(R.string.particle_force_x), ps.forceOverLifetime.x, go);
                    addMinMaxCurveEditor(fLayout, activity.getString(R.string.particle_force_y), ps.forceOverLifetime.y, go);
                    addMinMaxCurveEditor(fLayout, activity.getString(R.string.particle_force_z), ps.forceOverLifetime.z, go);
                    return fLayout;
                });

        addModuleSection(mainLayout, activity.getString(R.string.particle_color_over_lifetime), ps.colorOverLifetime.enabled,
                v -> { ps.colorOverLifetime.enabled = v; updatePS3D(go); }, () -> {
                    LinearLayout cLayout = new LinearLayout(activity);
                    cLayout.setOrientation(LinearLayout.VERTICAL);
                    addMinMaxGradientEditor(cLayout, activity.getString(R.string.particle_color), ps.colorOverLifetime.color, go);
                    return cLayout;
                });

        addModuleSection(mainLayout, activity.getString(R.string.particle_size_over_lifetime), ps.sizeOverLifetime.enabled,
                v -> { ps.sizeOverLifetime.enabled = v; updatePS3D(go); }, () -> {
                    LinearLayout sLayout = new LinearLayout(activity);
                    sLayout.setOrientation(LinearLayout.VERTICAL);

                    addCheckbox(sLayout, activity.getString(R.string.particle_separate_axes), ps.sizeOverLifetime.separateAxes, v -> {
                        ps.sizeOverLifetime.separateAxes = v; updatePS3D(go); populateInspector(go);
                    });

                    if (ps.sizeOverLifetime.separateAxes) {
                        addMinMaxCurveEditor(sLayout, "X", ps.sizeOverLifetime.sizeX, go);
                        addMinMaxCurveEditor(sLayout, "Y", ps.sizeOverLifetime.sizeY, go);
                        addMinMaxCurveEditor(sLayout, "Z", ps.sizeOverLifetime.sizeZ, go);
                    } else {
                        addMinMaxCurveEditor(sLayout, activity.getString(R.string.particle_size), ps.sizeOverLifetime.size, go);
                    }
                    return sLayout;
                });

        addModuleSection(mainLayout, activity.getString(R.string.particle_rotation_over_lifetime), ps.rotationOverLifetime.enabled,
                v -> { ps.rotationOverLifetime.enabled = v; updatePS3D(go); }, () -> {
                    LinearLayout rLayout = new LinearLayout(activity);
                    rLayout.setOrientation(LinearLayout.VERTICAL);

                    addCheckbox(rLayout, activity.getString(R.string.particle_separate_axes), ps.rotationOverLifetime.separateAxes, v -> {
                        ps.rotationOverLifetime.separateAxes = v; updatePS3D(go); populateInspector(go);
                    });

                    if (ps.rotationOverLifetime.separateAxes) {
                        addMinMaxCurveEditor(rLayout, "X", ps.rotationOverLifetime.angularVelocityX, go);
                        addMinMaxCurveEditor(rLayout, "Y", ps.rotationOverLifetime.angularVelocityY, go);
                        addMinMaxCurveEditor(rLayout, "Z", ps.rotationOverLifetime.angularVelocityZ, go);
                    } else {
                        addMinMaxCurveEditor(rLayout, activity.getString(R.string.particle_angular_velocity), ps.rotationOverLifetime.angularVelocity, go);
                    }
                    return rLayout;
                });

        addModuleSection(mainLayout, activity.getString(R.string.particle_noise), ps.noise.enabled,
                v -> { ps.noise.enabled = v; updatePS3D(go); }, () -> {
                    LinearLayout nLayout = new LinearLayout(activity);
                    nLayout.setOrientation(LinearLayout.VERTICAL);
                    addSimpleFloatInput(nLayout, activity.getString(R.string.particle_strength), ps.noise.strength, v -> { ps.noise.strength = v; updatePS3D(go); });
                    addSimpleFloatInput(nLayout, activity.getString(R.string.particle_frequency), ps.noise.frequency, v -> { ps.noise.frequency = v; updatePS3D(go); });
                    addSimpleFloatInput(nLayout, activity.getString(R.string.particle_octaves), ps.noise.octaves, v -> { ps.noise.octaves = Math.max(1, (int)v); updatePS3D(go); });
                    addSimpleFloatInput(nLayout, activity.getString(R.string.particle_scroll_speed), ps.noise.scrollSpeed, v -> { ps.noise.scrollSpeed = v; updatePS3D(go); });
                    addCheckbox(nLayout, activity.getString(R.string.particle_damping), ps.noise.damping, v -> { ps.noise.damping = v; updatePS3D(go); });

                    addCheckbox(nLayout, activity.getString(R.string.particle_separate_axes), ps.noise.separateAxes, v -> {
                        ps.noise.separateAxes = v; updatePS3D(go); populateInspector(go);
                    });
                    if (ps.noise.separateAxes) {
                        addSimpleFloatInput(nLayout, "X", ps.noise.strengthX, v -> { ps.noise.strengthX = v; updatePS3D(go); });
                        addSimpleFloatInput(nLayout, "Y", ps.noise.strengthY, v -> { ps.noise.strengthY = v; updatePS3D(go); });
                        addSimpleFloatInput(nLayout, "Z", ps.noise.strengthZ, v -> { ps.noise.strengthZ = v; updatePS3D(go); });
                    }
                    return nLayout;
                });

        addModuleSection(mainLayout, activity.getString(R.string.particle_collision), ps.collision.enabled,
                v -> { ps.collision.enabled = v; updatePS3D(go); }, () -> {
                    LinearLayout colLayout = new LinearLayout(activity);
                    colLayout.setOrientation(LinearLayout.VERTICAL);

                    addSpinnerEnum(colLayout, activity.getString(R.string.particle_mode), ParticleSystem3DComponent.CollisionMode.values(),
                            ps.collision.mode.ordinal(), v -> { ps.collision.mode = v; updatePS3D(go); populateInspector(go); });

                    addSimpleFloatInput(colLayout, activity.getString(R.string.particle_bounce), ps.collision.bounce, v -> { ps.collision.bounce = v; updatePS3D(go); });
                    addSimpleFloatInput(colLayout, activity.getString(R.string.particle_dampen), ps.collision.dampen, v -> { ps.collision.dampen = v; updatePS3D(go); });
                    addSimpleFloatInput(colLayout, activity.getString(R.string.particle_lifetime_loss), ps.collision.lifetimeLoss, v -> { ps.collision.lifetimeLoss = v; updatePS3D(go); });
                    addSimpleFloatInput(colLayout, activity.getString(R.string.particle_min_kill_speed), ps.collision.minKillSpeed, v -> { ps.collision.minKillSpeed = v; updatePS3D(go); });
                    addSimpleFloatInput(colLayout, activity.getString(R.string.particle_radius_scale), ps.collision.radiusScale, v -> { ps.collision.radiusScale = v; updatePS3D(go); });

                    addSpinnerEnum(colLayout, activity.getString(R.string.particle_quality), ParticleSystem3DComponent.CollisionQuality.values(),
                            ps.collision.quality.ordinal(), v -> { ps.collision.quality = v; updatePS3D(go); });

                    if (ps.collision.mode == ParticleSystem3DComponent.CollisionMode.PLANES) {
                        addSectionHeader(colLayout, activity.getString(R.string.particle_collision_planes));
                        for (int i = 0; i < ps.collision.planes.size(); i++) {
                            ParticleSystem3DComponent.CollisionPlane plane = ps.collision.planes.get(i);
                            int planeIdx = i;
                            LinearLayout planeRow = new LinearLayout(activity);
                            planeRow.setOrientation(LinearLayout.VERTICAL);
                            addSimpleFloatInput(planeRow, "Point Y", plane.point.y, v -> { plane.point.y = v; updatePS3D(go); });
                            addSimpleFloatInput(planeRow, "Normal Y", plane.normal.y, v -> { plane.normal.y = v; updatePS3D(go); });

                            Button delPlane = new Button(activity, null, 0, android.R.style.Widget_Material_Button_Small);
                            delPlane.setText("X");
                            delPlane.setTextColor(android.graphics.Color.RED);
                            delPlane.setOnClickListener(v -> { ps.collision.planes.remove(planeIdx); updatePS3D(go); populateInspector(go); });
                            planeRow.addView(delPlane);
                            colLayout.addView(planeRow);
                        }
                        Button addPlane = new Button(activity, null, 0, android.R.style.Widget_Material_Button_Small);
                        addPlane.setText("+");
                        addPlane.setOnClickListener(v -> { ps.collision.planes.add(new ParticleSystem3DComponent.CollisionPlane()); updatePS3D(go); populateInspector(go); });
                        colLayout.addView(addPlane);
                    }

                    return colLayout;
                });

        addModuleSection(mainLayout, activity.getString(R.string.particle_sub_emitters), ps.subEmitters.enabled,
                v -> { ps.subEmitters.enabled = v; updatePS3D(go); }, () -> {
                    LinearLayout subLayout = new LinearLayout(activity);
                    subLayout.setOrientation(LinearLayout.VERTICAL);

                    for (int i = 0; i < ps.subEmitters.entries.size(); i++) {
                        ParticleSystem3DComponent.SubEmitterEntry entry = ps.subEmitters.entries.get(i);
                        int idx = i;
                        LinearLayout entryRow = new LinearLayout(activity);
                        entryRow.setOrientation(LinearLayout.VERTICAL);

                        addSpinnerEnum(entryRow, activity.getString(R.string.particle_trigger), ParticleSystem3DComponent.SubEmitterTrigger.values(),
                                entry.trigger.ordinal(), v -> { entry.trigger = v; updatePS3D(go); });

                        Button pickObj = new Button(activity, null, 0, android.R.style.Widget_Material_Button_Small);
                        pickObj.setText(entry.subEmitterObjectId != null && !entry.subEmitterObjectId.isEmpty()
                                ? entry.subEmitterObjectId : "Select Object...");
                        pickObj.setTextColor(android.graphics.Color.WHITE);
                        pickObj.setAllCaps(false);
                        pickObj.setOnClickListener(v -> {
                            List<String> candidates = new ArrayList<>();
                            for (GameObject candidate : sceneManager.getAllGameObjects().values()) {
                                if (candidate.hasComponent(ParticleSystem3DComponent.class) && !candidate.id.equals(go.id)) {
                                    candidates.add(candidate.id);
                                }
                            }
                            if (candidates.isEmpty()) {
                                Toast.makeText(activity, "No other objects with Particle System 3D found", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            new AlertDialog.Builder(activity, androidx.appcompat.R.style.Theme_AppCompat_DayNight_Dialog_Alert)
                                    .setTitle("Select Sub Emitter")
                                    .setItems(candidates.toArray(new String[0]), (dialog, which) -> {
                                        entry.subEmitterObjectId = candidates.get(which);
                                        pickObj.setText(entry.subEmitterObjectId);
                                        updatePS3DImmediate(go);
                                    })
                                    .show();
                        });
                        entryRow.addView(pickObj);
                        addSimpleFloatInput(entryRow, activity.getString(R.string.particle_probability), entry.probability, v -> { entry.probability = v; updatePS3D(go); });
                        addSimpleFloatInput(entryRow, activity.getString(R.string.particle_emit_count), entry.emitCount, v -> {
                            entry.emitCount = Math.max(0, (int) v);
                            updatePS3D(go);
                        });

                        Button delEntry = new Button(activity, null, 0, android.R.style.Widget_Material_Button_Small);
                        delEntry.setText("X");
                        delEntry.setTextColor(android.graphics.Color.RED);
                        delEntry.setOnClickListener(v -> { ps.subEmitters.entries.remove(idx); updatePS3D(go); populateInspector(go); });
                        entryRow.addView(delEntry);
                        subLayout.addView(entryRow);
                    }

                    Button addEntry = new Button(activity, null, 0, android.R.style.Widget_Material_Button_Small);
                    addEntry.setText("+");
                    addEntry.setOnClickListener(v -> { ps.subEmitters.entries.add(new ParticleSystem3DComponent.SubEmitterEntry()); updatePS3D(go); populateInspector(go); });
                    subLayout.addView(addEntry);

                    return subLayout;
                });

        addModuleSection(mainLayout, activity.getString(R.string.particle_trails), ps.trails.enabled,
                v -> { ps.trails.enabled = v; updatePS3D(go); }, () -> {
                    LinearLayout tLayout = new LinearLayout(activity);
                    tLayout.setOrientation(LinearLayout.VERTICAL);
                    addSimpleFloatInput(tLayout, activity.getString(R.string.particle_ratio), ps.trails.ratio, v -> { ps.trails.ratio = v; updatePS3D(go); });
                    addSimpleFloatInput(tLayout, activity.getString(R.string.particle_lifetime), ps.trails.lifetime, v -> { ps.trails.lifetime = v; updatePS3D(go); });
                    addSimpleFloatInput(tLayout, activity.getString(R.string.particle_min_vertex_dist), ps.trails.minimumVertexDistance, v -> { ps.trails.minimumVertexDistance = v; updatePS3D(go); });
                    addCheckbox(tLayout, activity.getString(R.string.particle_world_space), ps.trails.worldSpace, v -> { ps.trails.worldSpace = v; updatePS3D(go); });
                    addCheckbox(tLayout, activity.getString(R.string.particle_die_with_particles), ps.trails.dieWithParticles, v -> { ps.trails.dieWithParticles = v; updatePS3D(go); });
                    addCheckbox(tLayout, activity.getString(R.string.particle_inherit_color), ps.trails.inheritParticleColor, v -> { ps.trails.inheritParticleColor = v; updatePS3D(go); });
                    addMinMaxCurveEditor(tLayout, activity.getString(R.string.particle_width_over_trail), ps.trails.widthOverTrail, go);
                    return tLayout;
                });

        addModuleSection(mainLayout, activity.getString(R.string.particle_tex_animation), ps.textureSheetAnimation.enabled,
                v -> { ps.textureSheetAnimation.enabled = v; updatePS3D(go); }, () -> {
                    LinearLayout tsLayout = new LinearLayout(activity);
                    tsLayout.setOrientation(LinearLayout.VERTICAL);
                    addSimpleFloatInput(tsLayout, activity.getString(R.string.particle_tiles_x), ps.textureSheetAnimation.tilesX, v -> { ps.textureSheetAnimation.tilesX = Math.max(1, (int)v); updatePS3D(go); });
                    addSimpleFloatInput(tsLayout, activity.getString(R.string.particle_tiles_y), ps.textureSheetAnimation.tilesY, v -> { ps.textureSheetAnimation.tilesY = Math.max(1, (int)v); updatePS3D(go); });
                    addSimpleFloatInput(tsLayout, activity.getString(R.string.particle_cycles), ps.textureSheetAnimation.cycles, v -> { ps.textureSheetAnimation.cycles = Math.max(1, (int)v); updatePS3D(go); });
                    addMinMaxCurveEditor(tsLayout, activity.getString(R.string.particle_frame_over_time), ps.textureSheetAnimation.frameOverTime, go);
                    return tsLayout;
                });

        addSectionHeader(mainLayout, activity.getString(R.string.particle_renderer));

        ParticleSystem3DComponent.RenderMode[] supportedModes = {
                ParticleSystem3DComponent.RenderMode.BILLBOARD,
                ParticleSystem3DComponent.RenderMode.STRETCHED_BILLBOARD,
                ParticleSystem3DComponent.RenderMode.HORIZONTAL_BILLBOARD,
                ParticleSystem3DComponent.RenderMode.VERTICAL_BILLBOARD,
                ParticleSystem3DComponent.RenderMode.MESH,
        };
        int currentModeIdx = 0;
        for (int i = 0; i < supportedModes.length; i++) {
            if (supportedModes[i] == ps.renderer.renderMode) { currentModeIdx = i; break; }
        }
        addSpinnerEnum(mainLayout, activity.getString(R.string.particle_render_mode), supportedModes, currentModeIdx,
                v -> { ps.renderer.renderMode = v; updatePS3DImmediate(go); populateInspector(go); });

        if (ps.renderer.renderMode == ParticleSystem3DComponent.RenderMode.STRETCHED_BILLBOARD) {
            addSimpleFloatInput(mainLayout, activity.getString(R.string.particle_length_scale), ps.renderer.lengthScale, v -> { ps.renderer.lengthScale = v; updatePS3D(go); });
            addSimpleFloatInput(mainLayout, activity.getString(R.string.particle_speed_scale), ps.renderer.speedScale, v -> { ps.renderer.speedScale = v; updatePS3D(go); });
        }

        if (ps.renderer.renderMode == ParticleSystem3DComponent.RenderMode.MESH) {

            ParticleSystem3DComponent.MeshType[] meshTypes = {
                    ParticleSystem3DComponent.MeshType.CUBE,
                    ParticleSystem3DComponent.MeshType.SPHERE_LOW,
                    ParticleSystem3DComponent.MeshType.CYLINDER_LOW,
                    ParticleSystem3DComponent.MeshType.CUSTOM,
            };
            int currentMeshTypeIdx = 0;
            for (int i = 0; i < meshTypes.length; i++) {
                if (meshTypes[i] == ps.renderer.meshType) { currentMeshTypeIdx = i; break; }
            }
            addSpinnerEnum(mainLayout, activity.getString(R.string.particle_mesh_shape), meshTypes, currentMeshTypeIdx,
                    v -> { ps.renderer.meshType = v; updatePS3DImmediate(go); populateInspector(go); });


            if (ps.renderer.meshType == ParticleSystem3DComponent.MeshType.CUSTOM) {
                LinearLayout meshRow = new LinearLayout(activity);
                meshRow.setOrientation(LinearLayout.HORIZONTAL);
                meshRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

                TextView meshLabel = new TextView(activity);
                meshLabel.setText((ps.renderer.meshPath != null ? ps.renderer.meshPath : "None"));
                meshLabel.setTextColor(android.graphics.Color.WHITE);
                meshLabel.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                meshRow.addView(meshLabel);

                Button selectMesh = new Button(activity, null, 0, android.R.style.Widget_Material_Button_Small);
                selectMesh.setText("Set .glb");
                selectMesh.setOnClickListener(v -> {
                    File projectFilesDir = ProjectManager.getInstance().getCurrentProject().getFilesDir();
                    File[] allFiles = projectFilesDir.listFiles();
                    if (allFiles == null) return;

                    List<String> modelFiles = new ArrayList<>();
                    for (File file : allFiles) {
                        String name = file.getName().toLowerCase();
                        if (name.endsWith(".glb") || name.endsWith(".gltf") || name.endsWith(".obj")) {
                            modelFiles.add(file.getName());
                        }
                    }
                    if (modelFiles.isEmpty()) {
                        Toast.makeText(activity, "No 3D models found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    new AlertDialog.Builder(activity, androidx.appcompat.R.style.Theme_AppCompat_DayNight_Dialog_Alert)
                            .setTitle(R.string.particle_mesh_shape)
                            .setItems(modelFiles.toArray(new String[0]), (dialog, which) -> {
                                ps.renderer.meshPath = modelFiles.get(which);
                                updatePS3DImmediate(go);
                                populateInspector(go);
                            })
                            .show();
                });
                meshRow.addView(selectMesh);
                mainLayout.addView(meshRow);
            }

            addCheckbox(mainLayout, activity.getString(R.string.particle_align_to_velocity), ps.renderer.alignToVelocity,
                    v -> { ps.renderer.alignToVelocity = v; updatePS3DImmediate(go); });
        }

        addCheckbox(mainLayout, activity.getString(R.string.particle_additive_blend), ps.renderer.isAdditive, v -> { ps.renderer.isAdditive = v; updatePS3DImmediate(go); });

        LinearLayout texRow = new LinearLayout(activity);
        texRow.setOrientation(LinearLayout.HORIZONTAL);
        texRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView texLabel = new TextView(activity);
        texLabel.setText((ps.renderer.texturePath != null ? ps.renderer.texturePath : "Default"));
        texLabel.setTextColor(android.graphics.Color.WHITE);
        texLabel.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        texRow.addView(texLabel);

        Button selectTex = new Button(activity, null, 0, android.R.style.Widget_Material_Button_Small);
        selectTex.setText("Set");
        selectTex.setOnClickListener(v -> showTexturePicker(fileName -> {
            ps.renderer.texturePath = fileName;
            updatePS3DImmediate(go);
            populateInspector(go);
        }));
        texRow.addView(selectTex);

        ImageButton clearTex = new ImageButton(activity);
        clearTex.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        clearTex.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        clearTex.setOnClickListener(v -> { ps.renderer.texturePath = null; updatePS3DImmediate(go); populateInspector(go); });
        texRow.addView(clearTex);

        mainLayout.addView(texRow);
    }



    private android.os.Handler ps3dUpdateHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable pendingPS3DUpdate = null;

    private void updatePS3D(GameObject go) {
        if (pendingPS3DUpdate != null) {
            ps3dUpdateHandler.removeCallbacks(pendingPS3DUpdate);
        }
        pendingPS3DUpdate = () -> {
            if (threeDManager != null && go != null) {
                ParticleSystem3DComponent ps = go.getComponent(ParticleSystem3DComponent.class);
                if (ps != null) {
                    Gdx.app.postRunnable(() -> {
                        threeDManager.updateParticleEffect3D(go.id, ps, go.transform.worldTransform);
                    });
                }
            }
        };

        ps3dUpdateHandler.postDelayed(pendingPS3DUpdate, 300);
    }


    private void updatePS3DImmediate(GameObject go) {
        if (threeDManager != null && go != null) {
            ParticleSystem3DComponent ps = go.getComponent(ParticleSystem3DComponent.class);
            if (ps != null) {
                Gdx.app.postRunnable(() -> {
                    threeDManager.updateParticleEffect3D(go.id, ps, go.transform.worldTransform);
                });
            }
        }
    }



    private interface BoolConsumer { void accept(boolean value); }
    private interface ModuleContentBuilder { LinearLayout build(); }
    private interface EnumConsumer<T> { void accept(T value); }

    private void addSectionHeader(LinearLayout parent, String title) {
        TextView header = new TextView(activity);
        header.setText(title);
        header.setTextColor(android.graphics.Color.WHITE);
        header.setTextSize(13);
        header.setTypeface(null, android.graphics.Typeface.BOLD);
        header.setPadding(0, 20, 0, 8);
        parent.addView(header);
    }

    private void addCheckbox(LinearLayout parent, String label, boolean initialValue, BoolConsumer onChange) {
        CheckBox cb = new CheckBox(activity);
        cb.setText(label);
        cb.setTextColor(android.graphics.Color.WHITE);
        cb.setChecked(initialValue);
        cb.setOnCheckedChangeListener((v, isChecked) -> onChange.accept(isChecked));
        parent.addView(cb);
    }

    private void addStringInput(LinearLayout parent, String label, String initialValue, StringConsumer onChange) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView tv = new TextView(activity);
        tv.setText(label + ": ");
        tv.setTextColor(android.graphics.Color.LTGRAY);
        tv.setTextSize(12);
        row.addView(tv);

        EditText et = new EditText(activity);
        et.setText(initialValue != null ? initialValue : "");
        et.setTextColor(android.graphics.Color.WHITE);
        et.setTextSize(12);
        et.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        addSimpleTextListener(et, s -> onChange.accept(s));
        row.addView(et);

        parent.addView(row);
    }

    private void addSmallFloatInput(LinearLayout parent, String label, float value, FloatConsumer onChange) {
        LinearLayout col = new LinearLayout(activity);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(4, 0, 4, 0);
        col.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tv = new TextView(activity);
        tv.setText(label);
        tv.setTextColor(android.graphics.Color.LTGRAY);
        tv.setTextSize(10);
        col.addView(tv);

        EditText et = new EditText(activity);
        et.setText(String.format(java.util.Locale.US, "%.2f", value));
        et.setTextColor(android.graphics.Color.WHITE);
        et.setTextSize(12);
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        addSimpleTextListener(et, s -> { try { onChange.accept(Float.parseFloat(s)); } catch(Exception e){} });
        col.addView(et);

        parent.addView(col);
    }

    @SuppressWarnings("unchecked")
    private <T extends Enum<T>> void addSpinnerEnum(LinearLayout parent, String label,
                                                    T[] values, int selectedIdx,
                                                    EnumConsumer<T> onChange) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(0, 4, 0, 4);

        TextView tv = new TextView(activity);
        tv.setText(label + ": ");
        tv.setTextColor(android.graphics.Color.LTGRAY);
        tv.setTextSize(12);
        row.addView(tv);

        Spinner spinner = new Spinner(activity);
        ArrayAdapter<T> adapter = new ArrayAdapter<>(activity,
                R.layout.simple_spinner_item_white_text, values);
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_white_text);
        spinner.setAdapter(adapter);
        spinner.setSelection(Math.min(selectedIdx, values.length - 1));

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private boolean isFirstCall = true;

            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                if (isFirstCall) {
                    isFirstCall = false;
                    return;
                }
                onChange.accept(values[pos]);
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        spinner.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(spinner);

        parent.addView(row);
    }


    private void addModuleSection(LinearLayout parent, String title, boolean enabled,
                                  BoolConsumer onToggle, ModuleContentBuilder contentBuilder) {
        LinearLayout section = new LinearLayout(activity);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setPadding(0, 8, 0, 8);

        LinearLayout headerRow = new LinearLayout(activity);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        headerRow.setPadding(0, 12, 0, 4);

        CheckBox enableCb = new CheckBox(activity);
        enableCb.setChecked(enabled);

        enableCb.setTag("initializing");
        headerRow.addView(enableCb);

        TextView titleTv = new TextView(activity);
        titleTv.setText(title + (enabled ? " ▼" : " ▶"));
        titleTv.setTextColor(enabled ? android.graphics.Color.WHITE : android.graphics.Color.GRAY);
        titleTv.setTextSize(14);
        titleTv.setTypeface(null, android.graphics.Typeface.BOLD);
        titleTv.setPadding(8, 0, 0, 0);
        headerRow.addView(titleTv);

        section.addView(headerRow);

        LinearLayout contentContainer = new LinearLayout(activity);
        contentContainer.setOrientation(LinearLayout.VERTICAL);
        contentContainer.setPadding(16, 0, 0, 0);
        contentContainer.setVisibility(enabled ? View.VISIBLE : View.GONE);
        section.addView(contentContainer);


        if (enabled) {
            LinearLayout content = contentBuilder.build();
            if (content != null) {
                contentContainer.addView(content);
            }
        }


        View divider = new View(activity);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
        divider.setBackgroundColor(0x30FFFFFF);
        section.addView(divider);


        enableCb.setTag(null);
        enableCb.setOnCheckedChangeListener((v, isChecked) -> {
            if ("initializing".equals(v.getTag())) return;
            onToggle.accept(isChecked);

            titleTv.setText(title + (isChecked ? " ▼" : " ▶"));
            titleTv.setTextColor(isChecked ? android.graphics.Color.WHITE : android.graphics.Color.GRAY);
            contentContainer.removeAllViews();
            if (isChecked) {
                LinearLayout content = contentBuilder.build();
                if (content != null) {
                    contentContainer.addView(content);
                }
            }
            contentContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });


        titleTv.setOnClickListener(v -> {
            if (enableCb.isChecked()) {
                boolean visible = contentContainer.getVisibility() == View.VISIBLE;
                contentContainer.setVisibility(visible ? View.GONE : View.VISIBLE);
                titleTv.setText(title + (visible ? " ▶" : " ▼"));
            }
        });

        parent.addView(section);
    }


    private void addMinMaxCurveEditor(LinearLayout parent, String label,
                                      ParticleSystem3DComponent.MinMaxCurve curve, GameObject go) {
        LinearLayout wrapper = new LinearLayout(activity);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setPadding(0, 4, 0, 4);


        LinearLayout headerRow = new LinearLayout(activity);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView labelTv = new TextView(activity);
        labelTv.setText(label);
        labelTv.setTextColor(android.graphics.Color.LTGRAY);
        labelTv.setTextSize(12);
        labelTv.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        headerRow.addView(labelTv);

        Spinner modeSpinner = new Spinner(activity);
        String[] modes = {"Constant", "Random 2 Const", "Curve", "Random 2 Curves"};
        ArrayAdapter<String> modeAdapter = new ArrayAdapter<>(activity,
                R.layout.simple_spinner_item_white_text, modes);
        modeAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_white_text);
        modeSpinner.setAdapter(modeAdapter);
        modeSpinner.setSelection(curve.mode.ordinal());
        modeSpinner.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        headerRow.addView(modeSpinner);

        wrapper.addView(headerRow);


        LinearLayout contentArea = new LinearLayout(activity);
        contentArea.setOrientation(LinearLayout.VERTICAL);
        wrapper.addView(contentArea);

        Runnable rebuildContent = () -> {
            contentArea.removeAllViews();

            switch (curve.mode) {
                case CONSTANT:
                    addSimpleFloatInput(contentArea, "Value", curve.constantMax, v -> {
                        curve.constantMax = v;
                        curve.constantMin = v;
                        updatePS3D(go);
                    });
                    break;

                case RANDOM_BETWEEN_TWO_CONSTANTS:
                    LinearLayout twoConstRow = new LinearLayout(activity);
                    twoConstRow.setOrientation(LinearLayout.HORIZONTAL);
                    addSmallFloatInput(twoConstRow, "Min", curve.constantMin, v -> {
                        curve.constantMin = v;
                        updatePS3D(go);
                    });
                    addSmallFloatInput(twoConstRow, "Max", curve.constantMax, v -> {
                        curve.constantMax = v;
                        updatePS3D(go);
                    });
                    contentArea.addView(twoConstRow);
                    break;

                case CURVE:
                    addSimpleFloatInput(contentArea, "Multiplier", curve.multiplier, v -> {
                        curve.multiplier = v;
                        updatePS3D(go);
                    });
                    buildCurveGraphForMinMaxCurve(contentArea, curve.curve, go);
                    break;

                case RANDOM_BETWEEN_TWO_CURVES:
                    addSimpleFloatInput(contentArea, "Multiplier", curve.multiplier, v -> {
                        curve.multiplier = v;
                        updatePS3D(go);
                    });
                    TextView maxLabel = new TextView(activity);
                    maxLabel.setText("Max Curve:");
                    maxLabel.setTextColor(android.graphics.Color.WHITE);
                    maxLabel.setTextSize(11);
                    contentArea.addView(maxLabel);
                    buildCurveGraphForMinMaxCurve(contentArea, curve.curve, go);

                    TextView minLabel = new TextView(activity);
                    minLabel.setText("Min Curve:");
                    minLabel.setTextColor(android.graphics.Color.WHITE);
                    minLabel.setTextSize(11);
                    contentArea.addView(minLabel);
                    buildCurveGraphForMinMaxCurve(contentArea, curve.curveMin, go);
                    break;
            }
        };

        rebuildContent.run();

        modeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private boolean isFirstCall = true;

            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                if (isFirstCall) {
                    isFirstCall = false;
                    return;
                }
                ParticleSystem3DComponent.CurveMode newMode =
                        ParticleSystem3DComponent.CurveMode.values()[pos];
                if (newMode != curve.mode) {
                    curve.mode = newMode;

                    if ((newMode == ParticleSystem3DComponent.CurveMode.CURVE ||
                            newMode == ParticleSystem3DComponent.CurveMode.RANDOM_BETWEEN_TWO_CURVES)
                            && curve.curve.isEmpty()) {
                        curve.curve.add(new ParticleCurvePoint<Float>(0f, curve.constantMax));
                        curve.curve.add(new ParticleCurvePoint<Float>(1f, curve.constantMax));
                    }
                    if (newMode == ParticleSystem3DComponent.CurveMode.RANDOM_BETWEEN_TWO_CURVES
                            && curve.curveMin.isEmpty()) {
                        curve.curveMin.add(new ParticleCurvePoint<Float>(0f, curve.constantMin));
                        curve.curveMin.add(new ParticleCurvePoint<Float>(1f, curve.constantMin));
                    }

                    updatePS3DImmediate(go);
                    rebuildContent.run();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        parent.addView(wrapper);
    }


    private void buildCurveGraphForMinMaxCurve(LinearLayout container,
                                               List<ParticleCurvePoint<Float>> curvePoints,
                                               GameObject go) {
        float computedMin = 0f, computedMax = 1f;
        for (ParticleCurvePoint<Float> p : curvePoints) {
            if (p.value < computedMin) computedMin = p.value;
            if (p.value > computedMax) computedMax = p.value;
        }
        float range = computedMax - computedMin;
        if (range < 0.1f) range = 1f;
        computedMin -= range * 0.1f;
        computedMax += range * 0.1f;


        final float finalMinVal = computedMin;
        final float finalMaxVal = computedMax;

        CurveEditorView graphView = new CurveEditorView(activity);
        LinearLayout.LayoutParams graphParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (120 * activity.getResources().getDisplayMetrics().density));
        graphParams.setMargins(0, 4, 0, 4);
        graphView.setLayoutParams(graphParams);
        graphView.setData(curvePoints, finalMinVal, finalMaxVal, () -> updatePS3D(go));
        container.addView(graphView);


        LinearLayout btnRow = new LinearLayout(activity);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setGravity(android.view.Gravity.END);

        Button addPt = new Button(activity, null, 0, android.R.style.Widget_Material_Button_Small);
        addPt.setText("+ Point");
        addPt.setTextSize(10);
        addPt.setOnClickListener(v -> {
            float newTime = curvePoints.isEmpty() ? 0.5f :
                    Math.min(1f, curvePoints.get(curvePoints.size() - 1).time + 0.2f);
            float newVal = curvePoints.isEmpty() ? 1f :
                    curvePoints.get(curvePoints.size() - 1).value;
            curvePoints.add(new ParticleCurvePoint<>(newTime, newVal));
            java.util.Collections.sort(curvePoints,
                    (a, b) -> Float.compare(a.time, b.time));
            graphView.setData(curvePoints, finalMinVal, finalMaxVal, () -> updatePS3D(go));
            updatePS3D(go);
        });
        btnRow.addView(addPt);

        Button delPt = new Button(activity, null, 0, android.R.style.Widget_Material_Button_Small);
        delPt.setText("Del Sel");
        delPt.setTextSize(10);
        delPt.setOnClickListener(v -> {
            if (graphView.deleteSelectedPoint()) {
                updatePS3D(go);
            }
        });
        btnRow.addView(delPt);

        Button clearPts = new Button(activity, null, 0, android.R.style.Widget_Material_Button_Small);
        clearPts.setText("Clear");
        clearPts.setTextSize(10);
        clearPts.setTextColor(android.graphics.Color.parseColor("#FF8A80"));
        clearPts.setOnClickListener(v -> {
            curvePoints.clear();
            graphView.setData(curvePoints, 0, 1, () -> updatePS3D(go));
            updatePS3D(go);
        });
        btnRow.addView(clearPts);

        container.addView(btnRow);
    }


    private void addMinMaxGradientEditor(LinearLayout parent, String label,
                                         ParticleSystem3DComponent.MinMaxGradient gradient,
                                         GameObject go) {
        LinearLayout wrapper = new LinearLayout(activity);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setPadding(0, 4, 0, 4);


        LinearLayout headerRow = new LinearLayout(activity);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView labelTv = new TextView(activity);
        labelTv.setText(label);
        labelTv.setTextColor(android.graphics.Color.LTGRAY);
        labelTv.setTextSize(12);
        labelTv.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        headerRow.addView(labelTv);

        Spinner modeSpinner = new Spinner(activity);
        String[] modes = {"Color", "Random 2 Colors", "Gradient", "Random 2 Gradients"};
        ArrayAdapter<String> modeAdapter = new ArrayAdapter<>(activity,
                R.layout.simple_spinner_item_white_text, modes);
        modeAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_white_text);
        modeSpinner.setAdapter(modeAdapter);
        modeSpinner.setSelection(gradient.mode.ordinal());
        headerRow.addView(modeSpinner);

        wrapper.addView(headerRow);

        LinearLayout contentArea = new LinearLayout(activity);
        contentArea.setOrientation(LinearLayout.VERTICAL);
        wrapper.addView(contentArea);

        Runnable rebuildContent = () -> {
            contentArea.removeAllViews();

            switch (gradient.mode) {
                case COLOR:
                    addEffectColorParam(contentArea, "Color", gradient.colorMax, c -> {
                        gradient.colorMax.set(c);
                        gradient.colorMin.set(c);
                        updatePS3D(go);
                    });
                    break;

                case RANDOM_BETWEEN_TWO_COLORS:
                    addEffectColorParam(contentArea, "Color Min", gradient.colorMin, c -> {
                        gradient.colorMin.set(c);
                        updatePS3D(go);
                    });
                    addEffectColorParam(contentArea, "Color Max", gradient.colorMax, c -> {
                        gradient.colorMax.set(c);
                        updatePS3D(go);
                    });
                    break;

                case GRADIENT:
                    buildGradientEditor(contentArea, gradient.gradient, go);
                    break;

                case RANDOM_BETWEEN_TWO_GRADIENTS:
                    TextView maxLbl = new TextView(activity);
                    maxLbl.setText("Max Gradient:");
                    maxLbl.setTextColor(android.graphics.Color.WHITE);
                    maxLbl.setTextSize(11);
                    contentArea.addView(maxLbl);
                    buildGradientEditor(contentArea, gradient.gradient, go);

                    TextView minLbl = new TextView(activity);
                    minLbl.setText("Min Gradient:");
                    minLbl.setTextColor(android.graphics.Color.WHITE);
                    minLbl.setTextSize(11);
                    contentArea.addView(minLbl);
                    buildGradientEditor(contentArea, gradient.gradientMin, go);
                    break;
            }
        };

        rebuildContent.run();

        modeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            boolean firstCall = true;
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                if (firstCall) { firstCall = false; return; }
                ParticleSystem3DComponent.GradientMode newMode =
                        ParticleSystem3DComponent.GradientMode.values()[pos];
                if (newMode != gradient.mode) {
                    gradient.mode = newMode;

                    if ((newMode == ParticleSystem3DComponent.GradientMode.GRADIENT ||
                            newMode == ParticleSystem3DComponent.GradientMode.RANDOM_BETWEEN_TWO_GRADIENTS)
                            && gradient.gradient.isEmpty()) {
                        gradient.gradient.add(new ParticleCurvePoint<com.badlogic.gdx.graphics.Color>(0f, new com.badlogic.gdx.graphics.Color(gradient.colorMax)));
                        gradient.gradient.add(new ParticleCurvePoint<com.badlogic.gdx.graphics.Color>(1f, new com.badlogic.gdx.graphics.Color(gradient.colorMax)));
                    }
                    if (newMode == ParticleSystem3DComponent.GradientMode.RANDOM_BETWEEN_TWO_GRADIENTS
                            && gradient.gradientMin.isEmpty()) {
                        gradient.gradientMin.add(new ParticleCurvePoint<com.badlogic.gdx.graphics.Color>(0f, new com.badlogic.gdx.graphics.Color(gradient.colorMin)));
                        gradient.gradientMin.add(new ParticleCurvePoint<com.badlogic.gdx.graphics.Color>(1f, new com.badlogic.gdx.graphics.Color(gradient.colorMin)));
                    }

                    updatePS3D(go);
                    rebuildContent.run();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        parent.addView(wrapper);
    }

    private void buildGradientEditor(LinearLayout container,
                                     List<ParticleCurvePoint<com.badlogic.gdx.graphics.Color>> gradientPoints,
                                     GameObject go) {
        LinearLayout listLayout = new LinearLayout(activity);
        listLayout.setOrientation(LinearLayout.VERTICAL);
        container.addView(listLayout);

        Runnable refreshList = new Runnable() {
            @Override
            public void run() {
                listLayout.removeAllViews();
                java.util.Collections.sort(gradientPoints,
                        (a, b) -> Float.compare(a.time, b.time));

                for (int i = 0; i < gradientPoints.size(); i++) {
                    final ParticleCurvePoint<com.badlogic.gdx.graphics.Color> point = gradientPoints.get(i);
                    LinearLayout row = new LinearLayout(activity);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                    row.setPadding(0, 2, 0, 2);


                    EditText timeEdit = new EditText(activity);
                    timeEdit.setText(String.format(java.util.Locale.US, "%.2f", point.time));
                    timeEdit.setTextColor(android.graphics.Color.WHITE);
                    timeEdit.setTextSize(11);
                    timeEdit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER |
                            android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    timeEdit.setLayoutParams(new LinearLayout.LayoutParams(0,
                            LinearLayout.LayoutParams.WRAP_CONTENT, 0.3f));
                    addSimpleTextListener(timeEdit, s -> {
                        try {
                            point.time = Math.max(0, Math.min(1, Float.parseFloat(s)));
                            updatePS3D(go);
                        } catch (Exception e) {}
                    });
                    row.addView(timeEdit);


                    Button colorBtn = new Button(activity);
                    com.badlogic.gdx.graphics.Color gdxCol = point.value;
                    int androidColor = android.graphics.Color.argb(
                            (int)(gdxCol.a * 255),
                            (int)(gdxCol.r * 255),
                            (int)(gdxCol.g * 255),
                            (int)(gdxCol.b * 255));
                    colorBtn.setBackgroundColor(androidColor);
                    colorBtn.setLayoutParams(new LinearLayout.LayoutParams(
                            (int)(48 * activity.getResources().getDisplayMetrics().density),
                            (int)(32 * activity.getResources().getDisplayMetrics().density)));

                    final Runnable refreshRef = this;
                    colorBtn.setOnClickListener(v -> {
                        ColorPickerDialogBuilder.with(activity)
                                .setTitle("Pick Color")
                                .initialColor(androidColor)
                                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                                .density(12)
                                .showAlphaSlider(true)
                                .setPositiveButton("OK", (d, col, all) -> {
                                    point.value.set(
                                            android.graphics.Color.red(col) / 255f,
                                            android.graphics.Color.green(col) / 255f,
                                            android.graphics.Color.blue(col) / 255f,
                                            android.graphics.Color.alpha(col) / 255f
                                    );
                                    colorBtn.setBackgroundColor(col);
                                    updatePS3D(go);
                                })
                                .setNegativeButton("Cancel", null)
                                .build()
                                .show();
                    });
                    row.addView(colorBtn);


                    TextView alphaLabel = new TextView(activity);
                    alphaLabel.setText(String.format(java.util.Locale.US, " a:%.0f%%", gdxCol.a * 100));
                    alphaLabel.setTextColor(android.graphics.Color.GRAY);
                    alphaLabel.setTextSize(10);
                    row.addView(alphaLabel);


                    ImageButton delBtn = new ImageButton(activity);
                    delBtn.setImageResource(android.R.drawable.ic_delete);
                    delBtn.setBackgroundColor(android.graphics.Color.TRANSPARENT);
                    delBtn.setOnClickListener(v -> {
                        gradientPoints.remove(point);
                        updatePS3D(go);
                        refreshRef.run();
                    });
                    row.addView(delBtn);

                    listLayout.addView(row);
                }
            }
        };

        refreshList.run();


        Button addBtn = new Button(activity, null, 0, android.R.style.Widget_Material_Button_Small);
        addBtn.setText("+ Add Color Key");
        addBtn.setOnClickListener(v -> {
            float newTime = gradientPoints.isEmpty() ? 0f :
                    Math.min(1f, gradientPoints.get(gradientPoints.size() - 1).time + 0.25f);
            com.badlogic.gdx.graphics.Color lastCol = gradientPoints.isEmpty()
                    ? new com.badlogic.gdx.graphics.Color(1, 1, 1, 1)
                    : new com.badlogic.gdx.graphics.Color(gradientPoints.get(gradientPoints.size() - 1).value);
            gradientPoints.add(new ParticleCurvePoint<>(newTime, lastCol));
            updatePS3D(go);
            refreshList.run();
        });
        container.addView(addBtn);
    }

    private void createParticleView(GameObject go) {
        addComponentHeader(R.string.component_particle_legacy, true, false, () -> {
            go.components.removeIf(c -> c instanceof ParticleComponent);
            if (threeDManager != null) threeDManager.removeParticleEffect(go.id);
            if (threeDManager != null) threeDManager.removeEditorProxy(go.id);
            populateInspector(go);
        });

        LinearLayout mainLayout = new LinearLayout(activity);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        container.addView(mainLayout);

        ParticleComponent p = go.getComponent(ParticleComponent.class);
        p.migrateOldDataIfNeeded();

        View basicView = inflater.inflate(R.layout.inspector_particle, mainLayout, false);
        setWhiteTextToAllChildren((ViewGroup) basicView);
        mainLayout.addView(basicView);

        CheckBox loopingCheck = basicView.findViewById(R.id.p_looping);
        loopingCheck.setText(R.string.particle_looping);
        loopingCheck.setChecked(p.looping);
        loopingCheck.setOnCheckedChangeListener((v, isChecked) -> { p.looping = isChecked; updateParticles(go); });

        setupFloatParam(basicView, R.id.p_duration, activity.getString(R.string.particle_duration), p.duration, v -> { p.duration = v; updateParticles(go); });
        setupFloatParam(basicView, R.id.p_start_lifetime, activity.getString(R.string.particle_start_lifetime), p.startLifetime, v -> { p.startLifetime = v; updateParticles(go); });
        setupFloatParam(basicView, R.id.p_max_particles, activity.getString(R.string.particle_max_particles), p.maxParticles, v -> { p.maxParticles = (int)v; updateParticles(go); });
        setupFloatParam(basicView, R.id.p_emission_rate, activity.getString(R.string.particle_emission_rate), p.emissionRate, v -> { p.emissionRate = v; updateParticles(go); });

        hideOldFields(basicView);

        setupSpawnShapeUI(mainLayout, p, go);

        setupFloatParam(mainLayout, R.id.p_start_size, activity.getString(R.string.particle_base_size_mult), p.baseSize, v -> {p.baseSize = v; updateParticles(go);});
        addSimpleFloatInput(mainLayout, activity.getString(R.string.particle_base_size), p.baseSize, v -> { p.baseSize = v; updateParticles(go); });

        setupFloatGraphEditor(mainLayout, activity.getString(R.string.particle_size_over_lifetime), p.sizeGraph, go, 0f, 3f);
        setupFloatGraphEditor(mainLayout, "Speed (Along Shape)", p.speedGraph, go, 0, 10f);
        setupFloatParam(basicView, R.id.p_cone_angle, activity.getString(R.string.particle_spread_angle), p.coneAngle, v -> { p.coneAngle = v; updateParticles(go); });

        setupFloatGraphEditor(mainLayout, activity.getString(R.string.particle_gravity_y), p.gravityGraph, go, -10f, 10f);
        setupFloatGraphEditor(mainLayout, activity.getString(R.string.particle_vortex), p.vortexGraph, go, -10f, 10f);
        setupFloatGraphEditor(mainLayout, activity.getString(R.string.particle_turbulence), p.turbulenceGraph, go, 0f, 10f);
        setupFloatGraphEditor(mainLayout, activity.getString(R.string.particle_rotation_deg), p.rotationGraph, go, -180f, 180f);

        setupColorGraphEditor(mainLayout, activity.getString(R.string.particle_color_over_lifetime), p.colorGraph, go);

        TextView pathText = basicView.findViewById(R.id.text_texture_path);
        Button selectButton = basicView.findViewById(R.id.btn_select_texture);
        ImageButton clearButton = basicView.findViewById(R.id.btn_clear_texture);
        CheckBox additiveCheck = basicView.findViewById(R.id.p_is_additive);

        pathText.setText(p.texturePath != null ? p.texturePath : "Default");
        selectButton.setOnClickListener(v -> showTexturePicker(fileName -> { p.texturePath = fileName; updateParticles(go); populateInspector(go); }));
        clearButton.setOnClickListener(v -> { p.texturePath = null; updateParticles(go); populateInspector(go); });

        additiveCheck.setText(R.string.particle_additive_blend);
        additiveCheck.setChecked(p.isAdditive);
        additiveCheck.setOnCheckedChangeListener((v, isChecked) -> { p.isAdditive = isChecked; updateParticles(go); });
    }


    private void hideOldFields(View view) {
        int[] ids = {R.id.p_start_speed, R.id.p_start_size, R.id.p_gravity, R.id.p_end_size, R.id.p_start_rotation, R.id.p_rotation_over_lifetime, R.id.p_cone_radius};
        for (int id : ids) {
            View v = view.findViewById(id);
            if(v != null) v.setVisibility(View.GONE);
        }
        View colorStart = view.findViewById(R.id.p_start_color);
        if(colorStart != null && colorStart.getParent() instanceof View) {
            ((View)colorStart.getParent()).setVisibility(View.GONE);
        }
    }


    private void addSimpleFloatInput(LinearLayout parent, String label, float val, FloatConsumer onChange) {
        View view = inflater.inflate(R.layout.inspector_param_float, parent, false);
        setWhiteTextToAllChildren((ViewGroup) view);
        ((TextView)view.findViewById(R.id.text_param_name)).setText(label);
        EditText edit = view.findViewById(R.id.edit_param_value);
        edit.setText(String.format(Locale.US, "%.2f", val));
        addSimpleTextListener(edit, s -> {
            try { onChange.accept(Float.parseFloat(s)); } catch(Exception e){}
        });
        parent.addView(view);
    }


    private void setupSpawnShapeUI(LinearLayout container, ParticleComponent p, GameObject go) {
        TextView header = new TextView(activity);
        header.setText("Spawn Shape");
        header.setTextColor(Color.WHITE);
        header.setTextSize(14);
        header.setPadding(0, 20, 0, 5);
        container.addView(header);


        Spinner shapeSpinner = new Spinner(activity);
        ArrayAdapter<ParticleComponent.SpawnShape> adapter = new ArrayAdapter<>(activity, R.layout.simple_spinner_item_white_text, ParticleComponent.SpawnShape.values());
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_white_text);
        shapeSpinner.setAdapter(adapter);
        shapeSpinner.setSelection(p.spawnShape.ordinal());

        shapeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                p.spawnShape = ParticleComponent.SpawnShape.values()[position];
                updateParticles(go);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        container.addView(shapeSpinner);


        View vec3View = inflater.inflate(R.layout.inspector_transform, null);



        addSimpleFloatInput(container, "Size X (Radius/Width)", p.spawnSize.x, v -> { p.spawnSize.x = v; updateParticles(go); });
        addSimpleFloatInput(container, "Size Y (Height)", p.spawnSize.y, v -> { p.spawnSize.y = v; updateParticles(go); });
        addSimpleFloatInput(container, "Size Z (Depth)", p.spawnSize.z, v -> { p.spawnSize.z = v; updateParticles(go); });


        CheckBox surfaceCheck = new CheckBox(activity);
        surfaceCheck.setText("Spawn on Surface Only");
        surfaceCheck.setTextColor(Color.WHITE);
        surfaceCheck.setChecked(p.spawnOnSurface);
        surfaceCheck.setOnCheckedChangeListener((v, c) -> { p.spawnOnSurface = c; updateParticles(go); });
        container.addView(surfaceCheck);
    }


    private void setupFloatGraphEditor(LinearLayout container, String title, List<org.catrobat.catroid.raptor.ParticleCurvePoint<Float>> graph, GameObject go, float defaultMin, float defaultMax) {

        LinearLayout headerLayout = new LinearLayout(activity);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        headerLayout.setPadding(0, 24, 0, 8);


        TextView header = new TextView(activity);
        header.setText(title);
        header.setTextColor(Color.WHITE);
        header.setTextSize(14);
        header.setTypeface(null, android.graphics.Typeface.BOLD);

        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        header.setLayoutParams(headerParams);
        headerLayout.addView(header);


        android.widget.LinearLayout.LayoutParams labelParams = new android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        labelParams.setMargins(16, 0, 4, 0);


        TextView minLabel = new TextView(activity);
        minLabel.setText("Min:");
        minLabel.setTextColor(Color.GRAY);
        minLabel.setTextSize(10);
        minLabel.setLayoutParams(labelParams);
        headerLayout.addView(minLabel);

        EditText minEdit = new EditText(activity);
        minEdit.setText(String.format(Locale.US, "%.1f", defaultMin));
        minEdit.setTextColor(Color.WHITE);
        minEdit.setTextSize(12);
        minEdit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        minEdit.setMinWidth(100);
        headerLayout.addView(minEdit);


        TextView maxLabel = new TextView(activity);
        maxLabel.setText("Max:");
        maxLabel.setTextColor(Color.GRAY);
        maxLabel.setTextSize(10);
        maxLabel.setLayoutParams(labelParams);
        headerLayout.addView(maxLabel);

        EditText maxEdit = new EditText(activity);
        maxEdit.setText(String.format(Locale.US, "%.1f", defaultMax));
        maxEdit.setTextColor(Color.WHITE);
        maxEdit.setTextSize(12);
        maxEdit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        maxEdit.setMinWidth(100);
        headerLayout.addView(maxEdit);

        container.addView(headerLayout);


        CurveEditorView graphView = new CurveEditorView(activity);
        LinearLayout.LayoutParams graphParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (int) (150 * activity.getResources().getDisplayMetrics().density));
        graphParams.setMargins(0, 0, 0, 8);
        graphView.setLayoutParams(graphParams);


        Runnable updateRange = () -> {
            try {
                float min = Float.parseFloat(minEdit.getText().toString());
                float max = Float.parseFloat(maxEdit.getText().toString());

                if (min >= max) max = min + 0.1f;
                graphView.setRange(min, max);
            } catch (Exception e) {}
        };

        addSimpleTextListener(minEdit, s -> updateRange.run());
        addSimpleTextListener(maxEdit, s -> updateRange.run());


        graphView.setData(graph, defaultMin, defaultMax, () -> updateParticles(go));
        container.addView(graphView);


        LinearLayout btnLayout = new LinearLayout(activity);
        btnLayout.setOrientation(LinearLayout.HORIZONTAL);
        btnLayout.setGravity(android.view.Gravity.END);


        Button clearBtn = new Button(activity, null, 0, android.R.style.Widget_Material_Button_Small);
        clearBtn.setText("Clear");
        clearBtn.setTextColor(Color.parseColor("#FF8A80"));
        clearBtn.setOnClickListener(v -> {
            graph.clear();
            float currentMin = 0f, currentMax = 1f;
            try {
                currentMin = Float.parseFloat(minEdit.getText().toString());
                currentMax = Float.parseFloat(maxEdit.getText().toString());
            } catch(Exception e){}

            graphView.setData(graph, currentMin, currentMax, () -> updateParticles(go));
            updateParticles(go);
        });


        Button delSelBtn = new Button(activity, null, 0, android.R.style.Widget_Material_Button_Small);
        delSelBtn.setText("Del Selected");
        delSelBtn.setOnClickListener(v -> {
            boolean deleted = graphView.deleteSelectedPoint();
            if (!deleted) {
                Toast.makeText(activity, "Select a point on graph first", Toast.LENGTH_SHORT).show();
            } else {
                updateParticles(go);
            }
        });


        Button addBtn = new Button(activity, null, 0, android.R.style.Widget_Material_Button_Small);
        addBtn.setText("Add Point");
        addBtn.setOnClickListener(v -> {
            float currentMin = 0f, currentMax = 1f;
            try {
                currentMin = Float.parseFloat(minEdit.getText().toString());
                currentMax = Float.parseFloat(maxEdit.getText().toString());
            } catch(Exception e){}


            float newTime = graph.isEmpty() ? 0.5f : graph.get(graph.size()-1).time + 0.2f;
            if (newTime > 1f) newTime = 1f;


            float midVal = (currentMin + currentMax) / 2f;

            float newVal = graph.isEmpty() ? midVal : graph.get(graph.size()-1).value;

            graph.add(new org.catrobat.catroid.raptor.ParticleCurvePoint<>(newTime, newVal));


            graphView.setData(graph, currentMin, currentMax, () -> updateParticles(go));
            updateParticles(go);
        });

        btnLayout.addView(clearBtn);

        View spacer1 = new View(activity); spacer1.setLayoutParams(new LinearLayout.LayoutParams(16, 1)); btnLayout.addView(spacer1);
        btnLayout.addView(delSelBtn);
        View spacer2 = new View(activity); spacer2.setLayoutParams(new LinearLayout.LayoutParams(16, 1)); btnLayout.addView(spacer2);
        btnLayout.addView(addBtn);

        container.addView(btnLayout);
    }


    private void setupColorGraphEditor(LinearLayout container, String title, List<org.catrobat.catroid.raptor.ParticleCurvePoint<com.badlogic.gdx.graphics.Color>> graph, GameObject go) {
        TextView header = new TextView(activity);
        header.setText(title);
        header.setTextColor(Color.WHITE);
        header.setTextSize(14);
        header.setPadding(0, 20, 0, 5);
        container.addView(header);

        LinearLayout listLayout = new LinearLayout(activity);
        listLayout.setOrientation(LinearLayout.VERTICAL);
        container.addView(listLayout);

        Button addBtn = new Button(activity, null, 0, android.R.style.Widget_Material_Button_Small);
        addBtn.setText("+ Add Color Keyframe");
        container.addView(addBtn);

        Runnable refreshList = new Runnable() {
            @Override
            public void run() {
                listLayout.removeAllViews();
                go.getComponent(ParticleComponent.class).sortGraphs();

                for (int i = 0; i < graph.size(); i++) {
                    final org.catrobat.catroid.raptor.ParticleCurvePoint<com.badlogic.gdx.graphics.Color> point = graph.get(i);
                    View row = inflater.inflate(R.layout.inspector_graph_row_color, listLayout, false);
                    setWhiteTextToAllChildren((ViewGroup)row);

                    EditText timeEdit = row.findViewById(R.id.edit_time);
                    Button colorBtn = row.findViewById(R.id.btn_color);
                    ImageButton delBtn = row.findViewById(R.id.btn_delete);

                    timeEdit.setText(String.format(Locale.US, "%.2f", point.time));

                    com.badlogic.gdx.graphics.Color gdxCol = point.value;
                    int androidColor = android.graphics.Color.argb(
                            (int)(gdxCol.a * 255),
                            (int)(gdxCol.r * 255),
                            (int)(gdxCol.g * 255),
                            (int)(gdxCol.b * 255));

                    colorBtn.setBackgroundColor(androidColor);

                    addSimpleTextListener(timeEdit, s -> {
                        try { point.time = Math.max(0, Math.min(1, Float.parseFloat(s))); updateParticles(go); } catch(Exception e){}
                    });

                    colorBtn.setOnClickListener(v -> {
                        ColorPickerDialogBuilder.with(activity).setTitle("Pick Color")
                                .initialColor(androidColor)
                                .setPositiveButton("OK", (d, col, all) -> {
                                    point.value.set(
                                            android.graphics.Color.red(col) / 255f,
                                            android.graphics.Color.green(col) / 255f,
                                            android.graphics.Color.blue(col) / 255f,
                                            android.graphics.Color.alpha(col) / 255f
                                    );
                                    colorBtn.setBackgroundColor(col);
                                    updateParticles(go);
                                }).build().show();
                    });

                    delBtn.setOnClickListener(v -> {
                        graph.remove(point);
                        updateParticles(go);
                        run();
                    });

                    listLayout.addView(row);
                }
            }
        };
        refreshList.run();

        addBtn.setOnClickListener(v -> {
            float newTime = graph.isEmpty() ? 0f : (graph.get(graph.size()-1).time >= 1f ? 1f : graph.get(graph.size()-1).time + 0.2f);
            if(newTime > 1f) newTime = 1f;

            com.badlogic.gdx.graphics.Color lastCol = graph.isEmpty()
                    ? new com.badlogic.gdx.graphics.Color(1,1,1,1)
                    : new com.badlogic.gdx.graphics.Color(graph.get(graph.size()-1).value);

            graph.add(new org.catrobat.catroid.raptor.ParticleCurvePoint<>(newTime, lastCol));
            updateParticles(go);
            refreshList.run();
        });
    }

    private void updateParticles(GameObject go) {
        if (threeDManager != null) {
            threeDManager.updateParticleEffect(go.id, go.getComponent(ParticleComponent.class), go.transform.worldTransform);
        }
    }

    private void showAddEffectDialog(PostProcessingComponent pp, GameObject go) {
        String[] effects = {
                activity.getString(R.string.pp_effect_bloom),
                activity.getString(R.string.pp_effect_vignette),
                activity.getString(R.string.pp_effect_levels),
                activity.getString(R.string.pp_effect_grain),
                activity.getString(R.string.pp_effect_fxaa),
                activity.getString(R.string.pp_effect_chromatic),
                activity.getString(R.string.pp_effect_radial_blur),
                activity.getString(R.string.pp_effect_old_tv),
                activity.getString(R.string.pp_effect_crt),
                activity.getString(R.string.pp_effect_fisheye),
                activity.getString(R.string.pp_effect_water),
                activity.getString(R.string.pp_effect_motion_blur),
                activity.getString(R.string.pp_effect_lens_flare),
                activity.getString(R.string.pp_effect_gaussian),
                activity.getString(R.string.pp_effect_zoom),
                activity.getString(R.string.pp_effect_aces),
                activity.getString(R.string.pp_effect_eye_adaptation),
                activity.getString(R.string.pp_effect_ssr),
                activity.getString(R.string.pp_effect_ssao),
                activity.getString(R.string.pp_effect_height_fog),
                activity.getString(R.string.pp_effect_dof),
                activity.getString(R.string.pp_effect_god_rays),
                activity.getString(R.string.pp_effect_volumetric_fog),
                activity.getString(R.string.pp_effect_fsr)
        };
        new AlertDialog.Builder(activity)
                .setTitle("Add Effect")
                .setItems(effects, (dialog, which) -> {
                    switch(which) {
                        case 0: pp.effects.add(new PostProcessingData.Bloom()); break;
                        case 1: pp.effects.add(new PostProcessingData.Vignette()); break;
                        case 2: pp.effects.add(new PostProcessingData.Levels()); break;
                        case 3: pp.effects.add(new PostProcessingData.Grain()); break;
                        case 4: pp.effects.add(new PostProcessingData.Fxaa()); break;
                        case 5: pp.effects.add(new PostProcessingData.Chromatic()); break;
                        case 6: pp.effects.add(new PostProcessingData.RadialBlur()); break;
                        case 7: pp.effects.add(new PostProcessingData.OldTv()); break;
                        case 8: pp.effects.add(new PostProcessingData.Crt()); break;
                        case 9: pp.effects.add(new PostProcessingData.Fisheye()); break;
                        case 10: pp.effects.add(new PostProcessingData.Water()); break;
                        case 11: pp.effects.add(new PostProcessingData.MotionBlur()); break;
                        case 12: pp.effects.add(new PostProcessingData.LensFlare()); break;
                        case 13: pp.effects.add(new PostProcessingData.Gaussian()); break;
                        case 14: pp.effects.add(new PostProcessingData.Zoom()); break;
                        case 15: pp.effects.add(new PostProcessingData.ACES()); break;
                        case 16: pp.effects.add(new PostProcessingData.EyeAdaptation()); break;
                        case 17: pp.effects.add(new PostProcessingData.RayTracing()); break;
                        case 18: pp.effects.add(new PostProcessingData.SSAO()); break;
                        case 19: pp.effects.add(new PostProcessingData.HeightFog()); break;
                        case 20: pp.effects.add(new PostProcessingData.DepthOfField()); break;
                        case 21: pp.effects.add(new PostProcessingData.GodRays()); break;
                        case 22: pp.effects.add(new PostProcessingData.VolumetricFog()); break;
                        case 23: pp.effects.add(new PostProcessingData.Upscaler()); break;
                    }
                    threeDManager.updatePostProcessing(pp);
                    populateInspector(go);
                })
                .show();
    }



    private void showModelPicker(GameObject go) {
        File projectFilesDir = ProjectManager.getInstance().getCurrentProject().getFilesDir();
        File[] allFiles = projectFilesDir.listFiles();

        if (allFiles == null) {
            Toast.makeText(activity, "Could not read project files.", Toast.LENGTH_SHORT).show();
            return;
        }

        final List<File> modelFiles = new ArrayList<>();
        for (File file : allFiles) {
            String name = file.getName().toLowerCase();
            if (name.endsWith(".obj") || name.endsWith(".gltf") || name.endsWith(".glb")) {
                modelFiles.add(file);
            }
        }

        if (modelFiles.isEmpty()) {
            Toast.makeText(activity, "No 3D models (.obj, .gltf, .glb) found in project files.", Toast.LENGTH_LONG).show();
            return;
        }

        String[] modelNames = new String[modelFiles.size()];
        for (int i = 0; i < modelFiles.size(); i++) {
            modelNames[i] = modelFiles.get(i).getName();
        }

        new AlertDialog.Builder(activity)
                .setTitle("Select a 3D Model")
                .setItems(modelNames, (dialog, which) -> {
                    String selectedFileName = modelNames[which];
                    sceneManager.setRenderComponent(go, selectedFileName);
                    populateInspector(go);
                })
                .show();
    }

    private void createPhysicsView(GameObject go) {
        addComponentHeader(R.string.component_physics, true, true, () -> {
            go.components.removeIf(c -> c instanceof PhysicsComponent);
            sceneManager.removePhysicsComponent(go);
            populateInspector(go);
        });

        View view = inflater.inflate(R.layout.inspector_physics, container, false);
        setWhiteTextToAllChildren((ViewGroup) view);
        PhysicsComponent physics = go.getComponent(PhysicsComponent.class);

        Spinner stateSpinner = view.findViewById(R.id.spinner_physics_state);
        View massLayout = view.findViewById(R.id.layout_physics_mass);
        String[] states = activity.getResources().getStringArray(R.array.editor_3d_physics_states);
        ArrayAdapter<String> stateAdapter = new ArrayAdapter<>(activity, R.layout.simple_spinner_item_white_text, states);
        stateAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_white_text);
        stateSpinner.setAdapter(stateAdapter);
        stateSpinner.setSelection(physics.state.ordinal());

        stateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                physics.state = ThreeDManager.PhysicsState.values()[position];
                massLayout.setVisibility(physics.state == ThreeDManager.PhysicsState.DYNAMIC ? View.VISIBLE : View.GONE);
                sceneManager.setPhysicsComponent(go, physics);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        LinearLayout collidersContainer = view.findViewById(R.id.colliders_container);
        collidersContainer.removeAllViews();
        for (ColliderShapeData collider : physics.colliders) {
            View colliderItemView = inflater.inflate(R.layout.inspector_collider_item, collidersContainer, false);
            populateColliderItemView(go, colliderItemView, collider);
            collidersContainer.addView(colliderItemView);
        }

        view.findViewById(R.id.btn_add_box_collider).setOnClickListener(v -> {
            ColliderShapeData newCollider = new ColliderShapeData();
            newCollider.type = ColliderShapeData.ShapeType.BOX;
            newCollider.size.set(1, 1, 1);
            physics.colliders.add(newCollider);
            sceneManager.setPhysicsComponent(go, physics);
            populateInspector(go);
        });

        view.findViewById(R.id.btn_add_capsule_collider).setOnClickListener(v -> {
            ColliderShapeData newCollider = new ColliderShapeData();
            newCollider.type = ColliderShapeData.ShapeType.CAPSULE;
            newCollider.radius = 0.5f;
            newCollider.size.set(0.5f, 2.0f, 0.5f);
            physics.colliders.add(newCollider);
            sceneManager.setPhysicsComponent(go, physics);
            populateInspector(go);
        });

        EditText massEditor = view.findViewById(R.id.edit_physics_mass);
        EditText frictionEditor = view.findViewById(R.id.edit_physics_friction);
        EditText restitutionEditor = view.findViewById(R.id.edit_physics_restitution);

        massLayout.setVisibility(physics.state == ThreeDManager.PhysicsState.DYNAMIC ? View.VISIBLE : View.GONE);
        massEditor.setText(String.valueOf(physics.mass));
        frictionEditor.setText(String.valueOf(physics.friction));
        restitutionEditor.setText(String.valueOf(physics.restitution));

        addSimpleTextListener(massEditor, s -> { try { physics.mass = Float.parseFloat(s); sceneManager.setPhysicsComponent(go, physics); } catch (Exception e) {} });
        addSimpleTextListener(frictionEditor, s -> { try { physics.friction = Float.parseFloat(s); sceneManager.setFriction(go.id, physics.friction); } catch (Exception e) {} });
        addSimpleTextListener(restitutionEditor, s -> { try { physics.restitution = Float.parseFloat(s); sceneManager.setRestitution(go.id, physics.restitution); } catch (Exception e) {} });

        container.addView(view);
    }

    public void setSelectedCollider(ColliderShapeData collider) {
        this.selectedCollider = collider;
    }

    private void populateColliderItemView(GameObject go, View itemView, ColliderShapeData collider) {
        PhysicsComponent physics = go.getComponent(PhysicsComponent.class);

        TextView title = itemView.findViewById(R.id.collider_title);
        View contentLayout = itemView.findViewById(R.id.collider_content_layout);
        ImageButton duplicateButton = itemView.findViewById(R.id.btn_duplicate_collider);
        ImageButton deleteButton = itemView.findViewById(R.id.btn_delete_collider);

        EditText cx = itemView.findViewById(R.id.edit_collider_cx);
        EditText cy = itemView.findViewById(R.id.edit_collider_cy);
        EditText cz = itemView.findViewById(R.id.edit_collider_cz);

        View sizeLayout = itemView.findViewById(R.id.layout_collider_size);
        EditText sx = itemView.findViewById(R.id.edit_collider_sx);
        EditText sy = itemView.findViewById(R.id.edit_collider_sy);
        EditText sz = itemView.findViewById(R.id.edit_collider_sz);
        TextView labelSx = itemView.findViewById(R.id.label_collider_sx);
        TextView labelSz = itemView.findViewById(R.id.label_collider_sz);

        View radiusLayout = itemView.findViewById(R.id.layout_collider_radius);
        EditText radiusEditor = itemView.findViewById(R.id.edit_collider_radius);

        contentLayout.setOnClickListener(v -> {
            selectedCollider = collider;
            if (gizmo != null) {
                gizmo.setSelected(go, collider);
            }
            populateInspector(go);
        });

        if (collider == selectedCollider) {
            itemView.setBackgroundColor(0x559999FF);
        } else {
            itemView.setBackgroundColor(0xFF404040);
        }

        duplicateButton.setOnClickListener(v -> {
            ColliderShapeData newCollider = new ColliderShapeData();
            newCollider.type = collider.type;
            newCollider.centerOffset.set(collider.centerOffset).add(0.1f, 0, 0);
            newCollider.size.set(collider.size);
            newCollider.radius = collider.radius;
            physics.colliders.add(newCollider);
            sceneManager.setPhysicsComponent(go, physics);
            populateInspector(go);
        });

        deleteButton.setOnClickListener(v -> {
            physics.colliders.remove(collider);
            if (selectedCollider == collider) {
                selectedCollider = null;
                gizmo.setSelected(go, null);
            }
            sceneManager.setPhysicsComponent(go, physics);
            populateInspector(go);
        });
        title.setText(collider.type.toString() + " Collider");
        cx.setText(String.format(Locale.US, "%.3f", collider.centerOffset.x));
        cy.setText(String.format(Locale.US, "%.3f", collider.centerOffset.y));
        cz.setText(String.format(Locale.US, "%.3f", collider.centerOffset.z));

        if (collider.type == ColliderShapeData.ShapeType.BOX) {
            sizeLayout.setVisibility(View.VISIBLE);
            radiusLayout.setVisibility(View.GONE);
            sx.setVisibility(View.VISIBLE); sx.setText(String.format(Locale.US, "%.3f", collider.size.x));
            sy.setVisibility(View.VISIBLE); sy.setText(String.format(Locale.US, "%.3f", collider.size.y));
            sz.setVisibility(View.VISIBLE); sz.setText(String.format(Locale.US, "%.3f", collider.size.z));
            labelSx.setVisibility(View.VISIBLE);
            labelSz.setVisibility(View.VISIBLE);
        } else if (collider.type == ColliderShapeData.ShapeType.SPHERE) {
            sizeLayout.setVisibility(View.GONE);
            radiusLayout.setVisibility(View.VISIBLE);
            radiusEditor.setText(String.format(Locale.US, "%.3", collider.radius));
        } else {
            sizeLayout.setVisibility(View.VISIBLE);
            radiusLayout.setVisibility(View.VISIBLE);
            radiusEditor.setText(String.format(Locale.US, "%.3f", collider.radius));
            sy.setText(String.format(Locale.US, "%.3f", collider.size.y));
            sx.setVisibility(View.GONE);
            sz.setVisibility(View.GONE);
            labelSx.setVisibility(View.GONE);
            labelSz.setVisibility(View.GONE);
        }

        Runnable updateAction = () -> {
            try {
                collider.centerOffset.set(
                        Float.parseFloat(cx.getText().toString()),
                        Float.parseFloat(cy.getText().toString()),
                        Float.parseFloat(cz.getText().toString())
                );
                switch (collider.type) {
                    case BOX:
                        collider.size.set(
                                Float.parseFloat(sx.getText().toString()),
                                Float.parseFloat(sy.getText().toString()),
                                Float.parseFloat(sz.getText().toString())
                        );
                        break;
                    case SPHERE:
                        collider.radius = Float.parseFloat(radiusEditor.getText().toString());
                        break;
                    case CAPSULE:
                        collider.radius = Float.parseFloat(radiusEditor.getText().toString());
                        collider.size.y = Float.parseFloat(sy.getText().toString());
                        break;
                }
                sceneManager.setPhysicsComponent(go, physics);
            } catch (NumberFormatException e) {}
        };

        TextWatcher watcher = new DelayedTextWatcher(updateAction);
        cx.addTextChangedListener(watcher);
        cy.addTextChangedListener(watcher);
        cz.addTextChangedListener(watcher);
        sx.addTextChangedListener(watcher);
        sy.addTextChangedListener(watcher);
        sz.addTextChangedListener(watcher);
        radiusEditor.addTextChangedListener(watcher);
    }

    private void createAudioView(GameObject go) {
        addComponentHeader(R.string.component_audio, true, false, () -> {
            sceneManager.stopAudioComponentSound(go);
            go.components.removeIf(c -> c instanceof AudioComponent);
            sceneManager.engine.removeEditorProxy(go.id);
            populateInspector(go);
        });

        View view = inflater.inflate(R.layout.inspector_audio, container, false);
        AudioComponent audio = go.getComponent(AudioComponent.class);

        TextView pathText = view.findViewById(R.id.text_audio_path);
        Button selectButton = view.findViewById(R.id.btn_select_audio);
        ImageButton clearButton = view.findViewById(R.id.btn_clear_audio);
        Button playPreviewBtn = view.findViewById(R.id.btn_audio_preview_play);
        Button stopPreviewBtn = view.findViewById(R.id.btn_audio_preview_stop);

        CheckBox playOnAwakeCheck = view.findViewById(R.id.check_audio_play_on_awake);
        CheckBox loopCheck = view.findViewById(R.id.check_audio_loop);
        CheckBox is3DCheck = view.findViewById(R.id.check_audio_is_3d);
        CheckBox isMusicCheck = view.findViewById(R.id.check_audio_is_music);

        View distanceLayout = view.findViewById(R.id.layout_audio_distance);

        EditText delayEdit = view.findViewById(R.id.edit_audio_delay);
        EditText volumeEdit = view.findViewById(R.id.edit_audio_volume);
        EditText pitchEdit = view.findViewById(R.id.edit_audio_pitch);
        EditText distanceEdit = view.findViewById(R.id.edit_audio_distance);

        pathText.setText(audio.soundFileName != null ? audio.soundFileName : activity.getString(R.string.editor_3d_audio_no_clip));
        clearButton.setVisibility(audio.soundFileName != null ? View.VISIBLE : View.GONE);

        playOnAwakeCheck.setChecked(audio.playOnAwake);
        loopCheck.setChecked(audio.loop);
        is3DCheck.setChecked(audio.is3D);
        isMusicCheck.setChecked(audio.isMusic);

        distanceLayout.setVisibility(audio.is3D ? View.VISIBLE : View.GONE);

        delayEdit.setText(String.format(Locale.US, "%.2f", audio.startDelay));
        volumeEdit.setText(String.format(Locale.US, "%.2f", audio.volume));
        pitchEdit.setText(String.format(Locale.US, "%.2f", audio.pitch));
        distanceEdit.setText(String.format(Locale.US, "%.2f", audio.maxDistance));

        selectButton.setOnClickListener(v -> showAudioPicker(audio, pathText, clearButton, go));

        clearButton.setOnClickListener(v -> {
            sceneManager.stopAudioComponentSound(go);
            audio.soundFileName = null;
            pathText.setText(activity.getString(R.string.editor_3d_audio_no_clip));
            clearButton.setVisibility(View.GONE);
        });

        playPreviewBtn.setOnClickListener(v -> {
            sceneManager.stopAudioComponentSound(go);
            sceneManager.playAudioComponentSound(go, audio);
        });

        stopPreviewBtn.setOnClickListener(v -> {
            sceneManager.stopAudioComponentSound(go);
        });

        playOnAwakeCheck.setOnCheckedChangeListener((v, isChecked) -> audio.playOnAwake = isChecked);
        loopCheck.setOnCheckedChangeListener((v, isChecked) -> audio.loop = isChecked);

        is3DCheck.setOnCheckedChangeListener((v, isChecked) -> {
            audio.is3D = isChecked;
            distanceLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            sceneManager.stopAudioComponentSound(go);
            sceneManager.playAudioComponentSound(go, audio);
        });

        isMusicCheck.setOnCheckedChangeListener((v, isChecked) -> audio.isMusic = isChecked);

        addSimpleTextListener(delayEdit, s -> {
            try { audio.startDelay = Math.max(0f, Float.parseFloat(s)); } catch (Exception ignored) {}
        });

        addSimpleTextListener(volumeEdit, s -> {
            try {
                audio.volume = Math.max(0f, Math.min(1f, Float.parseFloat(s)));
                threeDManager.setSoundInstanceVolume(go.id, audio.volume * 100f);
            } catch (Exception ignored) {}
        });

        addSimpleTextListener(pitchEdit, s -> {
            try {
                audio.pitch = Math.max(0.1f, Math.min(3.0f, Float.parseFloat(s)));
                threeDManager.setSoundInstancePitch(go.id, audio.pitch);
            } catch (Exception ignored) {}
        });

        addSimpleTextListener(distanceEdit, s -> {
            try {
                audio.maxDistance = Math.max(0.1f, Float.parseFloat(s));
                threeDManager.set3DSoundMaxDistance(go.id, audio.maxDistance);
            } catch (Exception ignored) {}
        });

        container.addView(view);
    }

    private void showAudioPicker(AudioComponent audio, TextView pathText, ImageButton clearBtn, GameObject go) {
        File projectFilesDir = ProjectManager.getInstance().getCurrentProject().getFilesDir();
        File[] allFiles = projectFilesDir.listFiles();
        if (allFiles == null) return;

        List<String> audioFiles = new ArrayList<>();
        for (File file : allFiles) {
            String name = file.getName().toLowerCase();
            if (name.endsWith(".mp3") || name.endsWith(".ogg") || name.endsWith(".wav")) {
                audioFiles.add(file.getName());
            }
        }

        if (audioFiles.isEmpty()) {
            Toast.makeText(activity, R.string.editor_3d_audio_no_files, Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(activity)
                .setTitle(R.string.editor_3d_audio_select_title)
                .setItems(audioFiles.toArray(new String[0]), (dialog, which) -> {
                    String selected = audioFiles.get(which);
                    audio.soundFileName = selected;
                    pathText.setText(selected);
                    clearBtn.setVisibility(View.VISIBLE);
                    sceneManager.engine.prepareAudio(selected, selected, audio.isMusic);
                    populateInspector(go);
                })
                .show();
    }

    private void createLightView(GameObject go) {
        addComponentHeader(R.string.component_light, true, false, () -> {
            go.components.removeIf(c -> c instanceof LightComponent);
            sceneManager.removeLightComponent(go);
            populateInspector(go);
        });
        View view = inflater.inflate(R.layout.inspector_light, container, false);
        if (view instanceof ViewGroup) {
            setWhiteTextToAllChildren((ViewGroup) view);
        }
        LightComponent light = go.getComponent(LightComponent.class);

        Spinner typeSpinner = view.findViewById(R.id.spinner_light_type);

        String[] lightTypes = {
                activity.getString(R.string.light_type_directional),
                activity.getString(R.string.light_type_point),
                activity.getString(R.string.light_type_spot)
        };

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(activity, R.layout.simple_spinner_item_white_text, lightTypes);
        typeAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_white_text);
        typeSpinner.setAdapter(typeAdapter);
        typeSpinner.setSelection(light.type.ordinal());
        EditText intensityEditor = view.findViewById(R.id.edit_light_intensity);
        EditText rangeEditor = view.findViewById(R.id.edit_light_range);
        EditText angleEditor = view.findViewById(R.id.edit_light_angle);
        View rangeLayout = view.findViewById(R.id.layout_light_range);
        View spotLayout = view.findViewById(R.id.layout_light_spot);
        EditText exponentEditor = view.findViewById(R.id.edit_light_exponent);
        Button colorButton = view.findViewById(R.id.btn_light_color);

        exponentEditor.setText(String.valueOf(light.exponent));

        typeSpinner.setSelection(light.type.ordinal());
        intensityEditor.setText(String.valueOf(light.intensity));
        rangeEditor.setText(String.valueOf(light.range));
        angleEditor.setText(String.valueOf(light.cutoffAngle));

        Runnable updateVisibility = () -> {
            int position = typeSpinner.getSelectedItemPosition();
            LightComponent.LightType type = LightComponent.LightType.values()[position];
            rangeLayout.setVisibility(type == LightComponent.LightType.POINT || type == LightComponent.LightType.SPOT ? View.VISIBLE : View.GONE);
            spotLayout.setVisibility(type == LightComponent.LightType.SPOT ? View.VISIBLE : View.GONE);
        };
        updateVisibility.run();

        typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                light.type = LightComponent.LightType.values()[position];
                sceneManager.setLightComponent(go, light);
                updateVisibility.run();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        final int initialAndroidColor = libGdxColorToAndroidColor(light.color);
        colorButton.setBackgroundColor(initialAndroidColor);

        colorButton.setOnClickListener(v -> {
            ColorPickerDialogBuilder
                    .with(activity)
                    .setTitle("Choose color")
                    .initialColor(initialAndroidColor)
                    .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                    .density(12)
                    .setPositiveButton("OK", (dialog, selectedColor, allColors) -> {
                        colorButton.setBackgroundColor(selectedColor);

                        int r = android.graphics.Color.red(selectedColor);
                        int g = android.graphics.Color.green(selectedColor);
                        int b = android.graphics.Color.blue(selectedColor);

                        light.color.set(r / 255f, g / 255f, b / 255f, 1.0f);

                        sceneManager.setLightComponent(go, light);})
                    .setNegativeButton("Cancel", (dialog, which) -> {})
                    .build()
                    .show();
        });

        addSimpleTextListener(exponentEditor, s -> {
            try {
                light.exponent = Float.parseFloat(s);
                sceneManager.setLightComponent(go, light);
            } catch (Exception e) {}
        });


        addSimpleTextListener(intensityEditor, s -> { try { light.intensity = Float.parseFloat(s); sceneManager.setLightComponent(go, light); } catch (Exception e) {} });
        addSimpleTextListener(rangeEditor, s -> { try { light.range = Float.parseFloat(s); sceneManager.setLightComponent(go, light); } catch (Exception e) {} });
        addSimpleTextListener(angleEditor, s -> { try { light.cutoffAngle = Float.parseFloat(s); sceneManager.setLightComponent(go, light); } catch (Exception e) {} });

        container.addView(view);
    }

    private void createScriptView(GameObject go, ScriptComponent scriptComp) {
        addComponentHeader(R.string.component_script, true, false, () -> {
            go.components.remove(scriptComp);
            populateInspector(go);
        });

        View view = inflater.inflate(R.layout.inspector_script, container, false);
        setWhiteTextToAllChildren((ViewGroup) view);

        TextView pathText = view.findViewById(R.id.text_script_path);
        Button selectButton = view.findViewById(R.id.btn_select_script);

        if (scriptComp.scriptPath != null && !scriptComp.scriptPath.isEmpty()) {
            pathText.setText(scriptComp.scriptPath);
        } else {
            pathText.setText("None (Select a script)");
        }

        selectButton.setOnClickListener(v -> showScriptPicker(scriptComp));

        container.addView(view);
    }

    private void showScriptPicker(ScriptComponent scriptComp) {
        File projectFilesDir = ProjectManager.getInstance().getCurrentProject().getFilesDir();
        if (!projectFilesDir.exists()) {
            projectFilesDir.mkdirs();
        }

        File[] allFiles = projectFilesDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".kts"));

        if (allFiles == null || allFiles.length == 0) {
            Toast.makeText(activity, "No script files (.kts) found in 'scripts' folder.", Toast.LENGTH_LONG).show();
            return;
        }

        String[] scriptNames = new String[allFiles.length];
        for (int i = 0; i < allFiles.length; i++) {
            scriptNames[i] = allFiles[i].getName();
        }

        new AlertDialog.Builder(activity)
                .setTitle("Select a Script")
                .setItems(scriptNames, (dialog, which) -> {
                    String selectedScriptPath = scriptNames[which];
                    scriptComp.scriptPath = selectedScriptPath;
                    populateInspector(selectedObject);
                })
                .show();
    }

    private void showAddComponentDialog(GameObject go) {
        String[] components = {
                activity.getString(R.string.component_render),
                activity.getString(R.string.component_physics),
                activity.getString(R.string.component_light),
                activity.getString(R.string.component_animation),
                activity.getString(R.string.component_camera),
                activity.getString(R.string.component_material),
                activity.getString(R.string.component_postprocessing),
                activity.getString(R.string.component_particle_legacy),
                activity.getString(R.string.component_particle_3d),
                activity.getString(R.string.component_keyframe),
                activity.getString(R.string.component_prefab),
                activity.getString(R.string.component_audio)
        };

        new AlertDialog.Builder(activity)
                .setTitle("Add Component")
                .setItems(components, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            if (!go.hasComponent(RenderComponent.class)) {
                                sceneManager.setRenderComponent(go, "myModel.glb");
                            }
                            break;
                        case 1:
                            if (!go.hasComponent(PhysicsComponent.class)) {
                                sceneManager.setPhysicsComponent(go, ThreeDManager.PhysicsState.STATIC, ThreeDManager.PhysicsShape.BOX, 1.0f);
                            }
                            break;
                        case 2:
                            if (!go.hasComponent(LightComponent.class)) {
                                sceneManager.setLightComponent(go, new LightComponent());
                            }
                            break;
                        case 3:
                            if (!go.hasComponent(AnimationComponent.class)) {
                                sceneManager.setAnimationComponent(go, new AnimationComponent());
                            }
                            break;
                        case 4:
                            if (!go.hasComponent(CameraComponent.class)) {
                                sceneManager.setCameraComponent(go, new CameraComponent());
                            }
                            break;
                        case 5:
                            if (!go.hasComponent(MaterialComponent.class)) {
                                sceneManager.setMaterialComponent(go, new MaterialComponent());
                            }
                            break;
                        case 6:
                            if (!go.hasComponent(PostProcessingComponent.class)) {
                                PostProcessingComponent pp = new PostProcessingComponent();
                                go.addComponent(pp);
                                sceneManager.engine.updatePostProcessing(pp);
                            }
                            break;
                        case 7:
                            if (!go.hasComponent(ParticleComponent.class)) {
                                ParticleComponent p = new ParticleComponent();
                                go.addComponent(p);
                                sceneManager.engine.createParticleProxy(go.id);
                                updateParticles(go);
                            }
                            break;
                        case 8:
                            if (!go.hasComponent(ParticleSystem3DComponent.class)) {
                                ParticleSystem3DComponent ps3d = new ParticleSystem3DComponent();
                                go.addComponent(ps3d);
                                sceneManager.engine.createParticleProxy(go.id);
                                sceneManager.engine.updateParticleEffect3D(go.id, ps3d, go.transform.worldTransform);
                            }
                            break;
                        case 9:
                            if (!go.hasComponent(KeyframeComponent.class)) {
                                go.addComponent(new KeyframeComponent());
                            }
                            break;
                        case 10:
                            if (!go.hasComponent(PrefabComponent.class)) {
                                go.addComponent(new PrefabComponent());
                            }
                            break;
                        case 11:
                            if (!go.hasComponent(AudioComponent.class)) {
                                go.addComponent(new AudioComponent());
                            }
                            break;

                    }
                    populateInspector(go);
                })
                .show();
    }

    private boolean isShowingColliders = false;

    private void addComponentHeader(int titleResId, boolean canBeDeleted, boolean hasVisibilityToggle, Runnable onDelete) {
        View headerView = inflater.inflate(R.layout.inspector_component_header, container, false);
        TextView titleView = headerView.findViewById(R.id.header_title);
        ImageButton deleteButton = headerView.findViewById(R.id.btn_delete_component);
        ImageButton visibilityButton = headerView.findViewById(R.id.btn_toggle_visibility);

        titleView.setText(activity.getString(titleResId));

        if (hasVisibilityToggle) {
            visibilityButton.setVisibility(View.VISIBLE);
            visibilityButton.setOnClickListener(v -> {
                EditorFragment fragment = (EditorFragment) activity.getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if (fragment != null && fragment.getListener() != null) {
                    isShowingColliders = !isShowingColliders;
                    fragment.getListener().setColliderVisibility(isShowingColliders);
                    visibilityButton.setAlpha(isShowingColliders ? 1.0f : 0.5f);
                }
            });
        }

        if (canBeDeleted) {
            deleteButton.setVisibility(View.VISIBLE);
            deleteButton.setOnClickListener(v -> onDelete.run());
        } else {
            deleteButton.setVisibility(View.GONE);
        }

        container.addView(headerView);
    }

    private interface Vector3Updater { void update(GameObject go, Vector3 vec); }
    private interface QuaternionUpdater { void update(GameObject go, Quaternion quat); }

    private void addVector3Listener(GameObject go, View parent, int xId, int yId, int zId, Vector3Updater updater) {
        EditText x = parent.findViewById(xId);
        EditText y = parent.findViewById(yId);
        EditText z = parent.findViewById(zId);
        TextWatcher watcher = new DelayedTextWatcher(() -> {
            if (selectedObject == null || !x.hasFocus() && !y.hasFocus() && !z.hasFocus()) return;
            try {
                Vector3 v = new Vector3(
                        Float.parseFloat(x.getText().toString()),
                        Float.parseFloat(y.getText().toString()),
                        Float.parseFloat(z.getText().toString())
                );
                updater.update(go, v);
            } catch (NumberFormatException e) {}
        });
        x.addTextChangedListener(watcher);
        y.addTextChangedListener(watcher);
        z.addTextChangedListener(watcher);
    }

    private void addEulerListener(GameObject go, View parent, int pId, int yId, int rId, QuaternionUpdater updater) {
        EditText p = parent.findViewById(pId);
        EditText y = parent.findViewById(yId);
        EditText r = parent.findViewById(rId);
        TextWatcher watcher = new DelayedTextWatcher(() -> {
            if (selectedObject == null || !p.hasFocus() && !y.hasFocus() && !r.hasFocus()) return;
            try {
                Quaternion q = new Quaternion().setEulerAngles(
                        Float.parseFloat(y.getText().toString()),
                        Float.parseFloat(p.getText().toString()),
                        Float.parseFloat(r.getText().toString())
                );
                updater.update(go, q);
            } catch (NumberFormatException e) {}
        });
        p.addTextChangedListener(watcher);
        y.addTextChangedListener(watcher);
        r.addTextChangedListener(watcher);
    }

    private void updateVector3Fields(View parent, int xId, int yId, int zId, Vector3 vec) {
        ((EditText)parent.findViewById(xId)).setText(String.format(Locale.US, "%.3f", vec.x));
        ((EditText)parent.findViewById(yId)).setText(String.format(Locale.US, "%.3f", vec.y));
        ((EditText)parent.findViewById(zId)).setText(String.format(Locale.US, "%.3f", vec.z));
    }

    private void updateEulerFields(View parent, int pId, int yId, int rId, Quaternion q) {
        ((EditText)parent.findViewById(pId)).setText(String.format(Locale.US, "%.1f", q.getPitch()));
        ((EditText)parent.findViewById(yId)).setText(String.format(Locale.US, "%.1f", q.getYaw()));
        ((EditText)parent.findViewById(rId)).setText(String.format(Locale.US, "%.1f", q.getRoll()));
    }

    private void addSimpleTextListener(EditText editText, StringUpdater updater) {
        editText.addTextChangedListener(new DelayedTextWatcher(() -> {
            if (editText.hasFocus()) {
                updater.update(editText.getText().toString());
            }
        }));
    }

    private interface StringUpdater { void update(String value); }

    private static class DelayedTextWatcher implements TextWatcher {
        private final Runnable action;
        public DelayedTextWatcher(Runnable action) { this.action = action; }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(Editable s) { action.run(); }
    }

    private interface FloatConsumer { void accept(float value); }
    private interface StringConsumer { void accept(String value); }
    private interface ColorConsumer { void accept(com.badlogic.gdx.graphics.Color value); }

    private void setupSlider(GameObject go, View sliderLayout, float initialValue, FloatConsumer onUpdate) {
        SeekBar seekBar = sliderLayout.findViewById(R.id.seekbar);
        TextView valueText = sliderLayout.findViewById(R.id.text_value);

        seekBar.setProgress((int)(initialValue * 100));
        valueText.setText(String.format(Locale.US, "%.2f", initialValue));

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    valueText.setText(String.format(Locale.US, "%.2f", progress / 100f));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                float newValue = seekBar.getProgress() / 100f;
                onUpdate.accept(newValue);
                sceneManager.setMaterialComponent(go, go.getComponent(MaterialComponent.class));
            }
        });
    }

    private void setupColorPicker(GameObject go, Button colorButton, com.badlogic.gdx.graphics.Color initialColor, ColorConsumer onUpdate) {
        colorButton.setBackgroundColor(libGdxColorToAndroidColor(initialColor));

        colorButton.setOnClickListener(v -> {
            ColorPickerDialogBuilder
                    .with(activity)
                    .setTitle("Choose Color")
                    .initialColor(libGdxColorToAndroidColor(initialColor))
                    .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                    .density(12)
                    .setPositiveButton("OK", (dialog, selectedColor, allColors) -> {
                        colorButton.setBackgroundColor(selectedColor);

                        int r = android.graphics.Color.red(selectedColor);
                        int g = android.graphics.Color.green(selectedColor);
                        int b = android.graphics.Color.blue(selectedColor);
                        int a = android.graphics.Color.alpha(selectedColor);

                        com.badlogic.gdx.graphics.Color newGdxColor = new com.badlogic.gdx.graphics.Color(
                                r / 255f, g / 255f, b / 255f, a / 255f
                        );

                        onUpdate.accept(newGdxColor);
                        sceneManager.setMaterialComponent(go, go.getComponent(MaterialComponent.class));
                    })
                    .setNegativeButton("Cancel", null)
                    .build()
                    .show();
        });
    }

    private void setupTextureSlot(GameObject go, View textureSlotLayout, String currentPath, StringConsumer onUpdate) {
        if (textureSlotLayout == null) return;

        TextView pathText = textureSlotLayout.findViewById(R.id.text_texture_path);
        Button selectButton = textureSlotLayout.findViewById(R.id.btn_select_texture);
        ImageButton clearButton = textureSlotLayout.findViewById(R.id.btn_clear_texture);

        if (pathText == null) return;

        if (currentPath != null && !currentPath.isEmpty()) {
            try {
                File file = ProjectManager.getInstance().getCurrentProject().getFile(currentPath);
                if (file == null || !file.exists()) {
                    pathText.setText(currentPath + " (File Missing)");
                    pathText.setTextColor(Color.RED);
                } else {
                    pathText.setText(currentPath);
                    pathText.setTextColor(Color.WHITE);
                }
            } catch (Exception e) {
                pathText.setText(currentPath);
                pathText.setTextColor(Color.WHITE);
            }
            if (clearButton != null) clearButton.setVisibility(View.VISIBLE);
        } else {
            pathText.setText("None");
            pathText.setTextColor(Color.GRAY);
            if (clearButton != null) clearButton.setVisibility(View.GONE);
        }

        if (selectButton != null) {
            selectButton.setOnClickListener(v -> showTexturePicker(fileName -> {
                onUpdate.accept(fileName);
                if (go.hasComponent(MaterialComponent.class)) {
                    sceneManager.setMaterialComponent(go, go.getComponent(MaterialComponent.class));
                }
                populateInspector(go);
            }));
        }

        if (clearButton != null) {
            clearButton.setOnClickListener(v -> {
                onUpdate.accept(null);
                if (go.hasComponent(MaterialComponent.class)) {
                    sceneManager.setMaterialComponent(go, go.getComponent(MaterialComponent.class));
                }
                populateInspector(go);
            });
        }
    }

    private void showTexturePicker(StringConsumer onTextureSelected) {
        File projectFilesDir = ProjectManager.getInstance().getCurrentProject().getFilesDir();
        File[] allFiles = projectFilesDir.listFiles();
        if (allFiles == null) return;

        final List<String> textureFiles = new ArrayList<>();
        for (File file : allFiles) {
            String name = file.getName().toLowerCase();
            if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")) {
                textureFiles.add(file.getName());
            }
        }

        if (textureFiles.isEmpty()) {
            Toast.makeText(activity, "No image files (.png, .jpg) found in project.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(activity)
                .setTitle("Select Texture")
                .setItems(textureFiles.toArray(new String[0]), (dialog, which) -> {
                    onTextureSelected.accept(textureFiles.get(which));
                })
                .show();
    }
}
