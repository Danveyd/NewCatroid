package org.catrobat.catroid.editor;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.physics.bullet.DebugDrawer;

import org.catrobat.catroid.raptor.CameraComponent;
import org.catrobat.catroid.raptor.ColliderShapeData;
import org.catrobat.catroid.raptor.GameObject;
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

    public EditorListener(EditorActivity activity) {
        this.activity = activity;
    }

    @Override
    public void create() {
        resetEngine();
        activity.onEditorReady(sceneManager, threeDManager);
    }

    public void resetEngine() {
        resetEngine(null);
    }

    public void resetEngine(FileHandle sceneToLoad) {
        Gdx.app.postRunnable(() -> {
            if (threeDManager != null) {
                threeDManager.dispose();
            }
            if (gizmoBatch != null) {
                gizmoBatch.dispose();
            }

            threeDManager = new ThreeDManager();
            threeDManager.init();
            threeDManager.enableRealisticRendering(true);
            threeDManager.setSkyColor(0.1f, 0.2f, 0.3f);
            threeDManager.createGrid(100, 100);
            threeDManager.setEditorMode(true);
            sceneManager = new SceneManager(threeDManager);

            gizmoBatch = new ModelBatch();

            Camera editorCamera = threeDManager.getCamera();
            editorCamera.position.set(10, 10, 10);
            editorCamera.lookAt(0, 0, 0);

            cameraController = new EditorCameraController(editorCamera);
            gizmo = new Gizmo(sceneManager, editorCamera);

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
            cameraController.velocity.add(vx, vy, vz);
        }
    }

    private void setupInputProcessor() {
        gestureDetector = new GestureDetector(new GestureDetector.GestureAdapter() {
            @Override
            public boolean touchDown(float x, float y, int pointer, int button) {
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
                    } else {
                        gizmo.setSelected(null, null);
                    }
                } else {
                    activity.getInspectorManager().setSelectedCollider(null);
                    gizmo.setSelected(hitObject, null);
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
        Gdx.input.setInputProcessor(gestureDetector);
    }

    public void onCameraAccelerate(boolean accelerate) {
        if (cameraController != null) {
            cameraController.isAccelerating = accelerate;
        }
    }


    private void selectObjectAt(int screenX, int screenY) {
        Ray pickRay = threeDManager.getCamera().getPickRay(screenX, screenY);
        GameObject selectedObject = sceneManager.getObjectByRaycast(pickRay);
        gizmo.setSelectedObject(selectedObject);
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

        threeDManager.update(Gdx.graphics.getDeltaTime());
        threeDManager.render();

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

    @Override
    public void dispose() {
        if (threeDManager != null) {
            threeDManager.dispose();
        }
        if (gizmoBatch != null) {
            gizmoBatch.dispose();
        }
    }

    public SceneManager getSceneManager() {
        return sceneManager;
    }

    public Gizmo getGizmo() {
        return gizmo;
    }
}