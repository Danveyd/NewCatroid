package org.catrobat.catroid.editor;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.physics.bullet.DebugDrawer;

import org.catrobat.catroid.raptor.AudioComponent;
import org.catrobat.catroid.raptor.CameraComponent;
import org.catrobat.catroid.raptor.ColliderShapeData;
import org.catrobat.catroid.raptor.GameObject;
import org.catrobat.catroid.raptor.KeyframeComponent;
import org.catrobat.catroid.raptor.KeyframeData;
import org.catrobat.catroid.raptor.PhysicsComponent;
import org.catrobat.catroid.raptor.SceneManager;
import org.catrobat.catroid.raptor.ThreeDManager;

import java.util.Map;

public class EditorListener extends ApplicationAdapter {

    private final EditorActivity activity;
    private ThreeDManager threeDManager;
    private SceneManager sceneManager;
    private GestureDetector gestureDetector;
    private EditorCameraController cameraController;

    private Gizmo gizmo;
    private ModelBatch gizmoBatch;
    private boolean showColliders = false;

    private Model keyframeProxyModel;
    private ModelInstance keyframeProxyInstance;
    private ModelBatch debugBatch;

    private boolean isPcMode = false;

    public EditorListener(EditorActivity activity) {
        this.activity = activity;
    }

    public void setPcMode(boolean isPcMode) {
        this.isPcMode = isPcMode;
        if (cameraController != null) {
            cameraController.isPcMode = isPcMode;
        }
    }

    @Override
    public void create() {
        resetEngine();
        activity.onEditorReady(sceneManager, threeDManager);

        debugBatch = new ModelBatch();

        ModelBuilder modelBuilder = new ModelBuilder();



        Material cyanMaterial = new Material(ColorAttribute.createDiffuse(Color.CYAN));
        long usage = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal;
        float size = 1f;
        int divisions = 4;

        modelBuilder.begin();


        modelBuilder.part("top", GL20.GL_TRIANGLES, usage, cyanMaterial)
                .cone(size, size, size, divisions);


        Matrix4 transform = new Matrix4().setToTranslation(0, -size, 0).rotate(Vector3.X, 180);
        com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder prt = modelBuilder.part("bottom", GL20.GL_TRIANGLES, usage, cyanMaterial);
        prt.cone(size, size, size, divisions);
        prt.setVertexTransform(transform);

        keyframeProxyModel = modelBuilder.end();

        keyframeProxyInstance = new ModelInstance(keyframeProxyModel);
    }

    @Override
    public void resize(int width, int height) {
        if (threeDManager != null) {
            threeDManager.resize(width, height);
        }
        super.resize(width, height);
    }

    public void resetEngine() {
        resetEngine(null, new ThreeDManager.SceneSettings());
    }

    public void resetEngine(FileHandle sceneToLoad) {
        resetEngine(sceneToLoad, new ThreeDManager.SceneSettings());
    }

    public ThreeDManager getThreeDManager() {
        return threeDManager;
    }

    public void resetEngine(FileHandle sceneToLoad, ThreeDManager.SceneSettings settings) {
        Gdx.app.postRunnable(() -> {
            if (threeDManager != null) {
                threeDManager.dispose();
            }
            if (gizmoBatch != null) {
                gizmoBatch.dispose();
            }

            threeDManager = new ThreeDManager();
            threeDManager.init(settings);
            threeDManager.enableRealisticRendering(true);
            threeDManager.setSkyColor(0.1f, 0.2f, 0.3f);
            threeDManager.createGrid(100, 100);
            threeDManager.setEditorMode(true);
            sceneManager = new SceneManager(threeDManager);
            sceneManager.setEditorMode(true);

            gizmoBatch = new ModelBatch();

            Camera editorCamera = threeDManager.getCamera();
            editorCamera.position.set(10, 10, 10);
            editorCamera.lookAt(0, 0, 0);

            cameraController = new EditorCameraController(editorCamera);
            cameraController.isPcMode = this.isPcMode;
            gizmo = new Gizmo(activity, sceneManager, editorCamera);

            setupInputProcessor();

            activity.onEngineReset(sceneManager, threeDManager);

            if (sceneToLoad != null) {
                sceneManager.loadScene(sceneToLoad);

                activity.runOnUiThread(activity::updateHierarchy);
            }

            Gdx.app.log("EditorListener", "3D Engine has been reset.");
        });
    }

    public void onCameraMove(float vx, float vy, float vz) {
        if (cameraController != null) {
            cameraController.onCameraMove(vx, vy, vz);
        }
    }

    public void onCameraAccelerate(boolean active) {
        if (cameraController != null) {
            cameraController.onCameraAccelerate(active);
        }
    }

    private void setupInputProcessor() {
        gestureDetector = new GestureDetector(new GestureDetector.GestureAdapter() {
            @Override
            public boolean touchDown(float x, float y, int pointer, int button) {
                if (isPcMode && button != com.badlogic.gdx.Input.Buttons.LEFT) {
                    return false;
                }

                if (gizmo.touchDown(threeDManager.getCamera().getPickRay(x, y))) {
                    cameraController.enabled = false;
                    return true;
                }
                cameraController.enabled = true;
                return cameraController.touchDown(x, y, pointer, button);
            }

            @Override
            public boolean pan(float x, float y, float deltaX, float deltaY) {
                if (gizmo.isDragging()) {
                    gizmo.touchDragged(threeDManager.getCamera().getPickRay(x, y));
                    return true;
                }
                return cameraController.pan(x, y, deltaX, deltaY);
            }

            @Override
            public boolean tap(float x, float y, int count, int button) {
                if (gizmo.isDragging()) {
                    return true;
                }

                Ray pickRay = threeDManager.getCamera().getPickRay(x, y);
                GameObject hitObject = sceneManager.getObjectByRaycast(pickRay);

                if (hitObject == null) {

                    ColliderShapeData currentCollider = activity.getInspectorManager().getSelectedCollider();
                    GameObject currentObject = gizmo.getSelectedObject();

                    if (currentCollider != null) {
                        activity.getInspectorManager().setSelectedCollider(null);
                        gizmo.setSelected(currentObject, null);
                        activity.onObjectSelected(currentObject, false);
                    } else {
                        gizmo.setSelected(null, null);
                        activity.onObjectSelected(null, false);
                    }
                } else {
                    activity.getInspectorManager().setSelectedCollider(null);
                    gizmo.setSelected(hitObject, null);
                    activity.onObjectSelected(hitObject, false);
                }
                return true;
            }

            @Override
            public boolean panStop(float x, float y, int pointer, int button) {
                if(gizmo.isDragging()) {
                    gizmo.touchUp();
                }
                cameraController.enabled = true;
                return true;
            }

            @Override
            public boolean pinch(Vector2 initialPointer1, Vector2 initialPointer2, Vector2 pointer1, Vector2 pointer2) {
                return cameraController.pinch(initialPointer1, initialPointer2, pointer1, pointer2);
            }

            @Override
            public void pinchStop() {
                cameraController.pinchStop();
            }
        });
        InputAdapter pcInputAdapter = new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        View currentFocus = activity.getCurrentFocus();
                        if (currentFocus != null) {
                            currentFocus.clearFocus();

                            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
                        }

                        View fragmentContainer = activity.findViewById(org.catrobat.catroid.R.id.fragment_container);
                        if (fragmentContainer != null) {
                            fragmentContainer.requestFocus();
                        }
                    });
                }

                return false;
            }

            @Override
            public boolean scrolled(float amountX, float amountY) {
                if (isPcMode && cameraController != null) {
                    cameraController.baseMoveSpeed -= amountY * 2.0f;
                    if (cameraController.baseMoveSpeed < 0.5f) cameraController.baseMoveSpeed = 0.5f;
                    if (cameraController.baseMoveSpeed > 50f) cameraController.baseMoveSpeed = 50f;
                    return true;
                }
                return false;
            }
        };

        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(pcInputAdapter);
        multiplexer.addProcessor(gestureDetector);

        Gdx.input.setInputProcessor(multiplexer);
    }

    private void selectObjectAt(int screenX, int screenY) {
        Ray pickRay = threeDManager.getCamera().getPickRay(screenX, screenY);
        GameObject selectedObject = sceneManager.getObjectByRaycast(pickRay);
        gizmo.setSelectedObject(selectedObject);
        activity.onObjectSelected(selectedObject, false);
    }

    public void setColliderVisibility(boolean visible) {
        this.showColliders = visible;
    }

    public void setCurrentTool(EditorTool tool) {
        if (gizmo != null) {
            gizmo.setCurrentTool(tool);
        }
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0.5f, 0.5f, 0.5f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        if (cameraController != null) {
            cameraController.update(Gdx.graphics.getDeltaTime());
        }

        if (sceneManager != null) {
            sceneManager.update(Gdx.graphics.getDeltaTime());
        }

        threeDManager.update(Gdx.graphics.getDeltaTime());
        threeDManager.render();

        renderKeyframeVisuals();

        DebugDrawer dd = threeDManager.getDebugDrawer();
        if (dd != null) {
            dd.begin(threeDManager.getCamera());

            for (GameObject go : sceneManager.getAllGameObjects().values()) {
                CameraComponent camComp = go.getComponent(CameraComponent.class);
                if (camComp != null) {
                    threeDManager.renderCameraFrustum(go.transform.toMatrix(), camComp.fieldOfView, camComp.farPlane);
                }
            }

            dd.end();
        }

        Map<String, ModelInstance> proxies = threeDManager.getEditorProxies();
        if (!proxies.isEmpty()) {
            for (Map.Entry<String, ModelInstance> entry : proxies.entrySet()) {
                GameObject owner = sceneManager.findGameObject(entry.getKey());
                if (owner != null) {
                    ModelInstance proxyInstance = entry.getValue();
                    proxyInstance.transform.set(owner.transform.position, owner.transform.rotation);
                }
            }

            gizmoBatch.begin(threeDManager.getCamera());
            for (ModelInstance proxy : proxies.values()) {
                gizmoBatch.render(proxy);
            }
            gizmoBatch.end();
        }

        GameObject selectedObject = gizmo.getSelectedObject();

        if (selectedObject != null && selectedObject.hasComponent(AudioComponent.class)) {
            AudioComponent audio = selectedObject.getComponent(AudioComponent.class);
            if (audio.is3D) {
                Vector3 pos = selectedObject.transform.worldTransform.getTranslation(new Vector3());
                threeDManager.getWireframeBatch().begin(threeDManager.getCamera());
                threeDManager.renderSoundRadius(pos, audio.maxDistance, Color.ORANGE);
                threeDManager.getWireframeBatch().end();
            }
        }

        if (showColliders && selectedObject != null) {
            PhysicsComponent physics = selectedObject.getComponent(PhysicsComponent.class);
            ModelInstance modelInstance = threeDManager.getModelInstance(selectedObject.id);

            if (physics != null && modelInstance != null && !physics.colliders.isEmpty()) {
                ColliderShapeData selectedCollider = activity.getInspectorManager().getSelectedCollider();

                threeDManager.getWireframeBatch().begin(threeDManager.getCamera());

                for (ColliderShapeData colliderData : physics.colliders) {
                    Color color = (colliderData == selectedCollider) ? Color.YELLOW : Color.GREEN;
                    threeDManager.renderWireframeShape(threeDManager.getWireframeBatch(), colliderData, modelInstance.transform, color);
                }

                threeDManager.getWireframeBatch().end();
            }
        }

        if (gizmo != null) {
            gizmo.render(gizmoBatch);
        }
    }

    private Vector3 tempVec3 = new Vector3();

    private void renderKeyframeVisuals() {
        GameObject selectedObject = gizmo.getSelectedObject();
        if (selectedObject == null || !selectedObject.hasComponent(KeyframeComponent.class)) {
            return;
        }

        KeyframeComponent anim = selectedObject.getComponent(KeyframeComponent.class);

        synchronized (anim.keyframes) {
            if (anim.keyframes.isEmpty()) {
                return;
            }


            debugBatch.begin(cameraController.camera);
            for (KeyframeData frame : anim.keyframes) {
                keyframeProxyInstance.transform.setToTranslation(frame.position).scale(0.5f, 0.5f, 0.5f);
                debugBatch.render(keyframeProxyInstance);
            }
            debugBatch.end();


            DebugDrawer debugDrawer = threeDManager.getDebugDrawer();
            if (debugDrawer == null) return;

            for (int i = 0; i < anim.keyframes.size() - 1; i++) {
                Vector3 start = anim.keyframes.get(i).position;
                Vector3 end = anim.keyframes.get(i + 1).position;


                tempVec3.set(1f, 1f, 1f);
                debugDrawer.begin(cameraController.camera);
                debugDrawer.drawLine(start, end, tempVec3);
                debugDrawer.end();
            }
        }
    }

    @Override
    public void dispose() {
        if (threeDManager != null) {
            threeDManager.dispose();
        }
        if (gizmoBatch != null) {
            gizmoBatch.dispose();
        }
        if (debugBatch != null) debugBatch.dispose();
        if (keyframeProxyModel != null) keyframeProxyModel.dispose();
    }

    public SceneManager getSceneManager() {
        return sceneManager;
    }

    public Gizmo getGizmo() {
        return gizmo;
    }
}
