package org.catrobat.catroid.raptor;

import android.app.Activity;
import android.util.Log;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.editor.EditorActivity;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.raptor.GameObject;
import org.catrobat.catroid.raptor.LightComponent;
import org.catrobat.catroid.raptor.PhysicsComponent;
import org.catrobat.catroid.raptor.RenderComponent;
import org.catrobat.catroid.raptor.TransformComponent;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public class SceneManager {

    private final Queue<GameObject> loadingQueue = new LinkedList<>();

    private static final long MAX_LOADING_TIME_PER_FRAME_MS = 8;

    public final ThreeDManager engine;
    private final Map<String, GameObject> gameObjects = new ConcurrentHashMap<>();

    private boolean isEditorMode = false;

    private GameObject mainCameraObject = null;

    private boolean isPlaying = false;

    private final Vector3 tmpPos = new Vector3();
    private final Quaternion tmpRot = new Quaternion();
    private final Vector3 tmpScale = new Vector3();
    private final Matrix4 tmpMat1 = new Matrix4();
    private final Matrix4 tmpMat2 = new Matrix4();
    private final Matrix4 tmpMat3 = new Matrix4();

    public String skyboxPath;
    private FogComponent cachedFogComponent = null;

    private static class BoneAttachment {
        String childId;
        String parentModelId;
        String boneName;


        Vector3 localPosOffset = new Vector3();
        Quaternion localRotOffset = new Quaternion();

        public BoneAttachment(String child, String parent, String bone) {
            this.childId = child;
            this.parentModelId = parent;
            this.boneName = bone;
        }
    }


    private final List<BoneAttachment> activeAttachments = new ArrayList<>();



    public void attachObjectToBone(String childId, String parentId, String boneName, float offsetX, float offsetY, float offsetZ) {

        detachObject(childId);

        BoneAttachment attachment = new BoneAttachment(childId, parentId, boneName);
        attachment.localPosOffset.set(offsetX, offsetY, offsetZ);

        activeAttachments.add(attachment);
    }


    public void detachObject(String childId) {
        activeAttachments.removeIf(a -> a.childId.equals(childId));
    }

    public void setEditorMode(boolean isEditor) {
        this.isEditorMode = isEditor;
    }

    public void findAndSetMainCamera() {
        mainCameraObject = null;
        for (GameObject go : gameObjects.values()) {
            CameraComponent camComp = go.getComponent(CameraComponent.class);
            if (camComp != null && camComp.isMainCamera) {
                mainCameraObject = go;

                return;
            }
        }
    }


    public SceneManager(ThreeDManager lowLevelEngine) {
        this.engine = lowLevelEngine;
        this.engine.setSceneManager(this);

        json.setIgnoreUnknownFields(true);
        json.setUsePrototypes(false);

        json.addClassTag("ParticleSystem3D", ParticleSystem3DComponent.class);
        json.addClassTag("MinMaxCurve", ParticleSystem3DComponent.MinMaxCurve.class);
        json.addClassTag("MinMaxGradient", ParticleSystem3DComponent.MinMaxGradient.class);
        json.addClassTag("Burst", ParticleSystem3DComponent.Burst.class);
        json.addClassTag("EmissionModule", ParticleSystem3DComponent.EmissionModule.class);
        json.addClassTag("ShapeModule", ParticleSystem3DComponent.ShapeModule.class);
        json.addClassTag("CollisionPlane", ParticleSystem3DComponent.CollisionPlane.class);
        json.addClassTag("SubEmitterEntry", ParticleSystem3DComponent.SubEmitterEntry.class);
    }

    private void synchronizeTransformsFromEngine() {
        for (GameObject go : gameObjects.values()) {
            btRigidBody body = engine.getPhysicsBody(go.id);

            if (body == null || body.isStaticObject()) {
                continue;
            }

            Matrix4 bodyTransform = body.getWorldTransform();

            bodyTransform.getTranslation(tmpPos);
            bodyTransform.getRotation(tmpRot, true);

            if (go.parentId == null) {
                go.transform.position.set(tmpPos);
                go.transform.rotation.set(tmpRot);
            } else {
                GameObject parent = findGameObject(go.parentId);
                if (parent != null) {
                    if (Math.abs(parent.transform.worldTransform.det()) < 0.000001f) {
                        continue;
                    }

                    tmpMat1.set(parent.transform.worldTransform).inv();
                    tmpMat2.set(tmpPos, tmpRot, go.transform.scale);

                    // tmpMat3 = parentInverse * childWorld
                    tmpMat3.set(tmpMat1).mul(tmpMat2);

                    tmpMat3.getTranslation(go.transform.position);
                    tmpMat3.getRotation(go.transform.rotation, true);
                    // tmpMat3.getScale(go.transform.scale);
                }
            }
        }
    }

    public void update(float delta) {
        processLoadingQueue();

        if (!isEditorMode) {
            updateKeyframeAnimations(delta);
            synchronizeTransformsFromEngine();
        }

        if (!cameraAttachments.isEmpty()) {
            com.badlogic.gdx.graphics.Camera cam = engine.getCamera();
            if (cam != null) {
                Matrix4 camWorld = new Matrix4(cam.view).inv();
                for (CameraAttachment att : cameraAttachments) {
                    GameObject child = findGameObject(att.childId);
                    if (child != null) {
                        child.transform.worldTransform.set(camWorld).mul(att.localOffsetMat);

                        if (child.parentId == null) {
                            child.transform.worldTransform.getTranslation(child.transform.position);
                            child.transform.worldTransform.getRotation(child.transform.rotation, true);
                        }
                    }
                }
            }
        }

        updateWorldTransforms();

        for (GameObject go : gameObjects.values()) {
            applyTransformToEngine(go);
            if (go.hasComponent(LightComponent.class)) {
                applyLightAndTransform(go);
            }
        }

        if (mainCameraObject != null) {
            Matrix4 cameraWorldTransform = mainCameraObject.transform.worldTransform;
            cameraWorldTransform.getTranslation(tmpPos);
            cameraWorldTransform.getRotation(tmpRot, true);
            engine.setCameraPosition(tmpPos.x, tmpPos.y, tmpPos.z);
            engine.setCameraRotation(tmpRot);
        }

        for (BoneAttachment att : activeAttachments) {
            GameObject child = findGameObject(att.childId);
            if (child == null) continue;

            ModelInstance parentModel = engine.getModelInstance(att.parentModelId);
            if (parentModel == null) continue;

            com.badlogic.gdx.graphics.g3d.model.Node bone = parentModel.getNode(att.boneName, true);
            if (bone != null) {
                child.transform.worldTransform.set(parentModel.transform);
                child.transform.worldTransform.mul(bone.globalTransform);
                child.transform.worldTransform.translate(att.localPosOffset);
                child.transform.worldTransform.scl(child.transform.scale);

                engine.setWorldTransform(child.id, child.transform.worldTransform);

                if (child.parentId != null) {
                    GameObject parent = findGameObject(child.parentId);
                    if (parent != null) {
                        Matrix4 parentInv = new Matrix4(parent.transform.worldTransform).inv();
                        Matrix4 localMat = parentInv.mul(child.transform.worldTransform);
                        localMat.getTranslation(child.transform.position);
                        localMat.getRotation(child.transform.rotation, true);
                    } else {
                        child.transform.worldTransform.getTranslation(child.transform.position);
                        child.transform.worldTransform.getRotation(child.transform.rotation, true);
                    }
                } else {
                    child.transform.worldTransform.getTranslation(child.transform.position);
                    child.transform.worldTransform.getRotation(child.transform.rotation, true);
                }

                updateTransformRecursive(child, findGameObject(child.parentId));
                applyTransformToEngineRecursive(child);
            } else {
                Log.e("SceneManager", "ОШИБКА ПРИВЯЗКИ: Кость '" + att.boneName + "' не найдена в модели!");
            }
        }
    }

    private final Vector3 tmpKeyframePos = new Vector3();
    private final Quaternion tmpKeyframeRot = new Quaternion();
    private final Vector3 tmpKeyframeScale = new Vector3();

    private void updateKeyframeAnimations(float delta) {
        for (GameObject go : gameObjects.values()) {
            KeyframeComponent anim = go.getComponent(KeyframeComponent.class);
            if (anim == null || !anim.isPlaying || anim.keyframes.size() < 2) {
                continue;
            }


            anim.currentTime += delta;
            float duration = anim.getDuration();


            if (anim.currentTime > duration) {
                if (anim.looping) {
                    anim.currentTime = anim.currentTime % duration;
                } else {
                    anim.isPlaying = false;
                    anim.currentTime = duration;
                }
            }


            KeyframeData startFrame = null;
            KeyframeData endFrame = null;
            for (KeyframeData frame : anim.keyframes) {
                if (frame.time <= anim.currentTime) {
                    startFrame = frame;
                } else {
                    endFrame = frame;
                    break;
                }
            }


            if (endFrame == null) {
                go.transform.position.set(startFrame.position);
                go.transform.rotation.set(startFrame.rotation);
                go.transform.scale.set(startFrame.scale);
                continue;
            }


            float frameDuration = endFrame.time - startFrame.time;
            float timeIntoFrame = anim.currentTime - startFrame.time;
            float alpha = (frameDuration > 0.0001f) ? MathUtils.clamp(timeIntoFrame / frameDuration, 0f, 1f) : 1f;


            float easedAlpha = org.catrobat.catroid.content.EasingFunctions.calculate(
                    startFrame.easingToNext, alpha, 1.0f, 0.0f, 1.0f
            );


            tmpKeyframePos.set(startFrame.position).lerp(endFrame.position, easedAlpha);
            tmpKeyframeRot.set(startFrame.rotation).slerp(endFrame.rotation, easedAlpha);
            tmpKeyframeScale.set(startFrame.scale).lerp(endFrame.scale, easedAlpha);

            go.transform.position.set(tmpKeyframePos);
            go.transform.rotation.set(tmpKeyframeRot);
            go.transform.scale.set(tmpKeyframeScale);
        }
    }



    public void playKeyframeAnimation(String objectId) {
        GameObject go = findGameObject(objectId);
        if (go == null) return;
        KeyframeComponent anim = go.getComponent(KeyframeComponent.class);
        if (anim != null) {
            anim.isPlaying = true;
        }
    }

    public void stopKeyframeAnimation(String objectId) {
        GameObject go = findGameObject(objectId);
        if (go == null) return;
        KeyframeComponent anim = go.getComponent(KeyframeComponent.class);
        if (anim != null) {
            anim.isPlaying = false;
        }
    }

    public void setKeyframeAnimationTime(String objectId, float time) {
        GameObject go = findGameObject(objectId);
        if (go == null) return;
        KeyframeComponent anim = go.getComponent(KeyframeComponent.class);
        if (anim != null) {
            anim.currentTime = time;

            updateKeyframeAnimations(0);
            updateWorldTransforms();
        }
    }

    private void processLoadingQueue() {
        if (loadingQueue.isEmpty()) return;

        long startTime = System.currentTimeMillis();


        while (!loadingQueue.isEmpty()) {

            if (System.currentTimeMillis() - startTime > MAX_LOADING_TIME_PER_FRAME_MS) {
                break;
            }

            GameObject go = loadingQueue.poll();
            if (go != null) {

                gameObjects.put(go.id, go);

                rebuildGameObject_internal(go);
            }
        }
    }

    public void updateWorldTransforms() {
        for (GameObject go : gameObjects.values()) {
            if (go.parentId == null) {
                updateTransformRecursive(go, null);
            }
        }
    }

    private void updateTransformRecursive(GameObject current, GameObject parent) {
        current.transform.worldTransform.set(
                current.transform.position,
                current.transform.rotation,
                current.transform.scale
        );

        if (parent != null) {
            current.transform.worldTransform.mulLeft(parent.transform.worldTransform);
        }

        for (int i = 0; i < current.childrenIds.size(); i++) {
            GameObject child = findGameObject(current.childrenIds.get(i));
            if (child != null) {
                updateTransformRecursive(child, current);
            }
        }
    }

    private void loadPrefabForObject(GameObject rootGo, PrefabComponent prefabComp) {
        if (prefabComp.spawnedInstances != null) {
            for (String id : prefabComp.spawnedInstances) {
                GameObject old = findGameObject(id);
                if (old != null) {
                    rootGo.childrenIds.remove(id);
                    removeGameObject(old);
                }
            }
            prefabComp.spawnedInstances.clear();
        } else {
            prefabComp.spawnedInstances = new ArrayList<>();
        }

        if (prefabComp.prefabFilePath == null || prefabComp.prefabFilePath.isEmpty()) return;

        File file = ProjectManager.getInstance().getCurrentProject().getFile(prefabComp.prefabFilePath);
        if (file == null || !file.exists()) {
            Gdx.app.error("Prefab", "Prefab file not found: " + prefabComp.prefabFilePath);
            return;
        }

        try {
            String sceneJson = Gdx.files.absolute(file.getAbsolutePath()).readString();
            Json tempJson = new Json();
            tempJson.setUsePrototypes(false);
            tempJson.setIgnoreUnknownFields(true);
            SceneData prefabData = tempJson.fromJson(SceneData.class, sceneJson);

            if (prefabData == null || prefabData.gameObjects == null) return;

            java.util.Map<String, String> idMap = new java.util.HashMap<>();
            List<GameObject> roots = new ArrayList<>();

            for (GameObject go : prefabData.gameObjects) {
                String newId = rootGo.id + "_prefab_" + go.id;
                idMap.put(go.id, newId);
                go.id = newId;
                go.name = "[Prefab] " + go.name;
                go.isPrefabInstance = true;

                if (go.parentId == null) roots.add(go);
            }

            for (GameObject go : prefabData.gameObjects) {
                if (go.parentId != null) {
                    go.parentId = idMap.get(go.parentId);
                } else {
                    go.parentId = rootGo.id;
                }

                List<String> newChildren = new ArrayList<>();
                for (String child : go.childrenIds) {
                    newChildren.add(idMap.get(child));
                }
                go.childrenIds = newChildren;

                gameObjects.put(go.id, go);
                prefabComp.spawnedInstances.add(go.id);
            }

            for (GameObject r : roots) {
                if (!rootGo.childrenIds.contains(r.id)) {
                    rootGo.childrenIds.add(r.id);
                }
            }

            for (GameObject go : prefabData.gameObjects) {
                rebuildGameObject_internal(go);
            }
        } catch (Exception e) {
            Gdx.app.error("Prefab", "Error loading prefab", e);
        }
    }

    private String generateUniqueName(String baseName) {
        String finalName = baseName.replaceAll(" \\(\\d+\\)$", "");
        if (!gameObjects.containsKey(finalName)) {
            return finalName;
        }
        int counter = 1;
        while (gameObjects.containsKey(finalName + " (" + counter + ")")) {
            counter++;
        }
        return finalName + " (" + counter + ")";
    }

    private void applyTransformToEngine(GameObject go) {
        if (go == null) return;


        Quaternion tempRotation = new Quaternion();
        go.transform.worldTransform.getRotation(tempRotation, true);


        if (Float.isNaN(tempRotation.x) || Float.isNaN(tempRotation.y) || Float.isNaN(tempRotation.z) || Float.isNaN(tempRotation.w)) {
            Log.e("SceneManager_Apply", "CRITICAL: NaN rotation detected for GameObject '" + go.id + "'. Aborting transform update for this frame to prevent engine corruption.");
            return;
        }

        engine.setWorldTransform(go.id, go.transform.worldTransform);

        if (go.hasComponent(ParticleComponent.class)) {
            engine.updateParticleTransform(go.id, go.transform.worldTransform);
        }

        if (go.hasComponent(ParticleSystem3DComponent.class)) {
            engine.updateParticleTransform3D(go.id, go.transform.worldTransform);
        }
    }

    public void setPosition(GameObject go, Vector3 position) {
        go.transform.position.set(position);
    }

    public void setRotation(GameObject go, Quaternion rotation) {
        go.transform.rotation.set(rotation);
    }

    public void setScale(GameObject go, Vector3 scale) {
        go.transform.scale.set(scale);
    }


    public void loadAndReplaceScene(FileHandle fileHandle) {
        Gdx.app.postRunnable(() -> {
            if (engine == null || engine.isDisposed()) return;
            clearScene_internal();
            if (fileHandle == null || !fileHandle.exists()) {
                Gdx.app.error("SceneManager", "Scene file handle is null or does not exist.");
                return;
            }
            String sceneJson = fileHandle.readString();
            json.setUsePrototypes(false);
            json.setIgnoreUnknownFields(true);
            SceneData sceneData = json.fromJson(SceneData.class, sceneJson);

            if (sceneData == null) { return; }
            setBackgroundLightIntensity(sceneData.ambientIntensity);
            setSkyColor(sceneData.skyR, sceneData.skyG, sceneData.skyB);
            float size = (sceneData.shadowSize > 0) ? sceneData.shadowSize : 100f;
            float res = (sceneData.shadowResolution > 0) ? sceneData.shadowResolution : 2048f;
            float csmFactor = (sceneData.csmSplitFactor >= 1f) ? sceneData.csmSplitFactor : 4f;

            engine.setShadowSettings(size, (int) res, sceneData.useCSM, csmFactor);

            if (sceneData.gameObjects == null) { return; }


            for (GameObject go : sceneData.gameObjects) {
                gameObjects.put(go.id, go);
            }


            if (engine == null || engine.isDisposed()) {
                Log.i("SceneManager", "Aborted scene rebuilding: ThreeDManager is disposed.");
                return;
            }

            for (GameObject go : sceneData.gameObjects) {
                rebuildGameObject_internal(go);
            }

            findAndSetMainCamera();

            Gdx.app.log("SceneManager", "Applying loaded skybox: " + this.skyboxPath);
            this.skyboxPath = sceneData.skyboxPath;
            setSkybox(this.skyboxPath);
        });
    }

    private void createRenderableForGameObject(GameObject go) {
        RenderComponent render = go.getComponent(RenderComponent.class);
        if (render != null && render.modelFileName != null && !render.modelFileName.isEmpty()) {
            String modelName = render.modelFileName;
            if (modelName.startsWith("primitive:")) {
                if (modelName.equals("primitive:cube")) engine.createCube(go.id);
                else if (modelName.equals("primitive:sphere")) engine.createSphere(go.id);
            } else {
                String absolutePath;
                if (modelName.startsWith("assets://")) {
                    absolutePath = modelName.substring("assets://".length());
                } else {
                    File modelFile = ProjectManager.getInstance().getCurrentProject().getFile(modelName);
                    if (modelFile != null && modelFile.exists()) {
                        absolutePath = modelFile.getAbsolutePath();
                    } else {
                        Gdx.app.error("SceneManager", "Rebuild failed: Model file not found: " + modelName);
                        return;
                    }
                }
                if (!engine.createObject(go.id, absolutePath)) {
                    Gdx.app.error("SceneManager", "Rebuild failed: Could not create render object for " + go.id);
                }
            }
        }
    }

    private void rebuildComponentsForGameObject(GameObject go) {
        engine.setWorldTransform(go.id, go.transform.worldTransform);

        Log.d("PhysicsDebug", "Rebuilding components for '" + go.name + "' with final World Transform:\n" + go.transform.worldTransform);

        PhysicsComponent physics = go.getComponent(PhysicsComponent.class);
        if (physics != null) {
            if (physics.colliders != null && !physics.colliders.isEmpty()) {
                engine.setPhysicsStateFromComponent(go.id, physics, go.transform.worldTransform);
            } else {
                engine.setPhysicsState(go.id, physics.state, physics.shape, physics.mass, go.transform.worldTransform);
            }
            engine.setFriction(go.id, physics.friction);
            engine.setRestitution(go.id, physics.restitution);
        }

        LightComponent light = go.getComponent(LightComponent.class);
        if (light != null) {
            engine.createEditorProxy(go.id);
            applyLightAndTransform(go);
        }

        CameraComponent camera = go.getComponent(CameraComponent.class);
        if (camera != null) {
            engine.createCameraProxy(go.id);
            if (camera.isMainCamera) {
                applyCameraComponentToEngine(go, camera);
            }
        }

        playAnimationFromComponent(go);

        MaterialComponent material = go.getComponent(MaterialComponent.class);
        if (material != null) {
            engine.applyPBRMaterial(go.id, material);
        }
    }



    public GameObject createGameObject(String baseName) {
        String finalName = baseName;
        int counter = 1;

        while (gameObjects.containsKey(finalName)) {
            finalName = baseName + " (" + counter + ")";
            counter++;
        }

        GameObject go = new GameObject(finalName);
        gameObjects.put(go.id, go);
        return go;
    }


    public boolean renameGameObject(GameObject go, String newName) {
        if (go == null || newName == null || newName.isEmpty() || gameObjects.containsKey(newName)) {
            return false;
        }
        String oldId = go.id;

        if (oldId.equals(newName)) {
            return true;
        }

        if (go.parentId != null) {
            GameObject parent = findGameObject(go.parentId);
            if (parent != null) {
                parent.childrenIds.remove(oldId);
                parent.childrenIds.add(newName);
            }
        }

        for (String childId : go.childrenIds) {
            GameObject child = findGameObject(childId);
            if (child != null) {
                child.parentId = newName;
            }
        }

        gameObjects.remove(oldId);

        go.id = newName;
        go.name = newName;

        gameObjects.put(go.id, go);

        engine.renameObject(oldId, newName);

        return true;
    }


    public GameObject duplicateGameObject(GameObject original) {
        if (original == null) return null;

        List<GameObject> newObjects = new ArrayList<>();
        Map<String, String> oldIdToNewId = new HashMap<>();

        GameObject newRoot = cloneRecursive(original, newObjects, oldIdToNewId);

        for (GameObject copy : newObjects) {
            if (copy.parentId != null) {
                String newParentId = oldIdToNewId.get(copy.parentId);
                if (newParentId != null) {
                    copy.parentId = newParentId;
                }
            }
        }

        if (newRoot != null) {
            newRoot.parentId = original.parentId;
        }

        for (GameObject copy : newObjects) {
            gameObjects.put(copy.id, copy);
            rebuildGameObject_internal(copy);
        }

        if (newRoot != null && newRoot.parentId != null) {
            GameObject parent = findGameObject(newRoot.parentId);
            if (parent != null && !parent.childrenIds.contains(newRoot.id)) {
                parent.childrenIds.add(newRoot.id);
            }
        }

        updateWorldTransforms();
        return newRoot;
    }

    private GameObject cloneRecursive(GameObject original, List<GameObject> copies, Map<String, String> idMapping) {
        String objectAsJson = json.toJson(original);
        GameObject copy = json.fromJson(GameObject.class, objectAsJson);

        copy.name = generateUniqueName(original.name);
        copy.id = copy.name;

        copies.add(copy);
        idMapping.put(original.id, copy.id);

        List<String> originalChildrenIds = new ArrayList<>(copy.childrenIds);
        copy.childrenIds.clear();

        for (String childId : originalChildrenIds) {
            GameObject originalChild = findGameObject(childId);
            if (originalChild != null) {
                GameObject newChild = cloneRecursive(originalChild, copies, idMapping);
                copy.childrenIds.add(newChild.id);
            }
        }
        return copy;
    }

    private GameObject duplicateRecursive(GameObject original, Map<String, String> idMapping) {
        String objectAsJson = json.toJson(original);
        GameObject copy = json.fromJson(GameObject.class, objectAsJson);

        String newName = generateUniqueName(original.name);
        copy.name = newName;
        copy.id = newName;

        idMapping.put(original.id, copy.id);

        gameObjects.put(copy.id, copy);

        List<String> originalChildrenIds = new ArrayList<>(copy.childrenIds);
        copy.childrenIds.clear();

        for (String originalChildId : originalChildrenIds) {
            GameObject originalChild = findGameObject(originalChildId);
            if (originalChild != null) {
                GameObject newChild = duplicateRecursive(originalChild, idMapping);
                newChild.parentId = copy.id;
                copy.childrenIds.add(newChild.id);
            }
        }

        rebuildGameObject(copy);

        return copy;
    }


    private final Matrix4 tmpMatInv = new Matrix4();
    private final Ray tmpRayLocal = new Ray();
    private final Vector3 tmpIntersectionLocal = new Vector3();
    private final Vector3 tmpIntersectionWorld = new Vector3();
    private final BoundingBox tmpBounds = new BoundingBox();

    public GameObject getObjectByRaycast(Ray ray) {
        GameObject bestCandidate = null;
        float closestDistance = Float.MAX_VALUE;

        for (GameObject go : gameObjects.values()) {
            if (!go.isActive) continue;

            ModelInstance instance = engine.getModelInstance(go.id);
            if (instance == null) {
                if (go.hasComponent(LightComponent.class) || go.hasComponent(CameraComponent.class) || go.hasComponent(ParticleComponent.class)) {
                    instance = engine.getEditorProxies().get(go.id);
                }
            }

            if (instance == null) continue;

            float dist = checkRayIntersection(ray, instance);

            if (dist >= 0 && dist < closestDistance) {
                closestDistance = dist;
                bestCandidate = go;
            }
        }

        return bestCandidate;
    }

    private float checkRayIntersection(Ray ray, ModelInstance instance) {
        instance.calculateBoundingBox(tmpBounds);

        tmpMatInv.set(instance.transform).inv();

        tmpRayLocal.set(ray).mul(tmpMatInv);

        if (Intersector.intersectRayBounds(tmpRayLocal, tmpBounds, tmpIntersectionLocal)) {
            tmpIntersectionWorld.set(tmpIntersectionLocal).mul(instance.transform);

            return ray.origin.dst(tmpIntersectionWorld);
        }

        return -1f;
    }


    public void rotate(GameObject go, Quaternion deltaRotation) {
        if (go == null) return;

        TransformComponent transform = go.transform;

        Vector3 originalScale = transform.scale.cpy();

        engine.setScale(go.id, 1, 1, 1);

        transform.rotation.mulLeft(deltaRotation);
        engine.setRotation(go.id, transform.rotation);

        engine.setScale(go.id, originalScale.x, originalScale.y, originalScale.z);

        transform.scale.set(originalScale);

        applyLightAndTransform(go);
    }


    public SceneData getCurrentSceneData() {
        SceneData sceneData = new SceneData();
        for (GameObject go : gameObjects.values()) {
            if (go.isPrefabInstance) continue;
            String jsonStr = json.toJson(go);
            GameObject saveGo = json.fromJson(GameObject.class, jsonStr);

            PrefabComponent p = saveGo.getComponent(PrefabComponent.class);
            if (p != null && p.spawnedInstances != null) {
                saveGo.childrenIds.removeAll(p.spawnedInstances);
            }

            sceneData.gameObjects.add(go);
        }
        sceneData.skyR = skyR;
        sceneData.skyG = skyG;
        sceneData.skyB = skyB;
        sceneData.skyboxPath = skyboxPath;
        sceneData.ambientIntensity = ambientIntensity;
        sceneData.shadowSize = engine.getShadowSize();
        sceneData.shadowResolution = engine.getShadowResolution();
        sceneData.renderSettings = engine.getSceneSettings();
        return sceneData;
    }


    public void loadSceneFromData(SceneData sceneData) {
        if (sceneData == null) return;

        clearScene_internal();

        setBackgroundLightIntensity(sceneData.ambientIntensity);
        setSkyColor(sceneData.skyR, sceneData.skyG, sceneData.skyB);
        float size = (sceneData.shadowSize > 0) ? sceneData.shadowSize : 100f;
        float res = (sceneData.shadowResolution > 0) ? sceneData.shadowResolution : 2048f;
        float csmFactor = (sceneData.csmSplitFactor >= 1f) ? sceneData.csmSplitFactor : 4f;

        engine.setShadowSettings(size, (int) res, sceneData.useCSM, csmFactor);

        if (sceneData.gameObjects == null) return;

        for (GameObject go : sceneData.gameObjects) {
            gameObjects.put(go.id, go);
            rebuildGameObject_internal(go);
        }
        findAndSetMainCamera();

        Gdx.app.log("SceneManager", "Scene loaded from cache.");

        Gdx.app.log("SceneManager", "Applying loaded skybox: " + this.skyboxPath);
        this.skyboxPath = sceneData.skyboxPath;
        setSkybox(this.skyboxPath);
    }

    public void setSkybox(String texturePath) {
        this.skyboxPath = texturePath;
        Gdx.app.postRunnable(() -> {
            if (engine == null || engine.isDisposed()) {
                Log.i("SceneManager", "Aborted setting skybox: ThreeDManager is disposed.");
                return;
            }
            if (texturePath != null && !texturePath.isEmpty()) {
                File textureFile = ProjectManager.getInstance().getCurrentProject().getFile(texturePath);
                if (textureFile != null && textureFile.exists()) {
                    engine.setSkybox(textureFile.getAbsolutePath());
                } else {
                    Gdx.app.error("SceneManager", "Skybox texture not found, clearing: " + texturePath);
                    engine.setSkybox(null);
                }
            } else {
                engine.setSkybox(null);
            }
        });
    }

    public void removeGameObject(GameObject go) {
        if (go == null) return;
        if (Gdx.app != null) {
            Gdx.app.postRunnable(() -> removeGameObjectInternal(go));
        } else {
            removeGameObjectInternal(go);
        }
    }

    private void removeGameObjectInternal(GameObject go) {
        if (go == null || engine == null || engine.isDisposed()) return;

        List<String> childrenIdsCopy = new ArrayList<>(go.childrenIds);
        for (String childId : childrenIdsCopy) {
            GameObject child = findGameObject(childId);
            if (child != null) {
                removeGameObjectInternal(child);
            }
        }

        if (go.parentId != null) {
            GameObject parent = findGameObject(go.parentId);
            if (parent != null) {
                parent.childrenIds.remove(go.id);
            }
        }

        gameObjects.remove(go.id);
        engine.removeObject(go.id);

        if (go.hasComponent(LightComponent.class)) {
            engine.removePBRLight(go.id);
            engine.removeEditorProxy(go.id);
        }

        if (go.hasComponent(ParticleComponent.class)) {
            engine.removeParticleEffect(go.id);
            engine.removeEditorProxy(go.id);
        }

        if (go.hasComponent(ParticleSystem3DComponent.class)) {
            engine.removeParticleEffect3D(go.id);
            engine.removeEditorProxy(go.id);
        }
    }

    public void setRenderComponent(GameObject go, String modelFileName) {
        Gdx.app.postRunnable(() -> {
            if (engine == null || engine.isDisposed()) return;
            RenderComponent render = go.getComponent(RenderComponent.class);
            if (render == null) {
                render = new RenderComponent();
                go.addComponent(render);
            }
            render.modelFileName = modelFileName;

            if (engine.objectExists(go.id)) {
                engine.removeModelOnly(go.id);
            }

            rebuildGameObject_internal(go);
        });
    }

    public GameObject findGameObject(String id) {
        if (id == null) {
            return null;
        }
        return gameObjects.get(id);
    }

    public Map<String, GameObject> getAllGameObjects() {
        return gameObjects;
    }

    public void setAnimationComponent(GameObject go, AnimationComponent animComponent) {
        if (go == null) return;

        go.components.removeIf(c -> c instanceof AnimationComponent);

        if (animComponent != null) {
            Log.d("SceneManager", "Is added: " + go.addComponent(animComponent));
        }

        playAnimationFromComponent(go);
    }



    public void setPhysicsComponent(GameObject go, ThreeDManager.PhysicsState state, ThreeDManager.PhysicsShape shape, float mass) {
        Gdx.app.postRunnable(() -> {
            if (engine == null || engine.isDisposed()) return;
            PhysicsComponent physics = go.getComponent(PhysicsComponent.class);
            if (physics == null) {
                physics = new PhysicsComponent();
                go.addComponent(physics);
            }

            physics.state = state;
            physics.mass = mass;
            physics.colliders.clear();
            if (state == ThreeDManager.PhysicsState.STATIC || state == ThreeDManager.PhysicsState.DYNAMIC) {
                ColliderShapeData singleCollider = new ColliderShapeData();
                switch (shape) {
                    case SPHERE:
                        singleCollider.type = ColliderShapeData.ShapeType.SPHERE;
                        break;
                    case CAPSULE:
                        singleCollider.type = ColliderShapeData.ShapeType.CAPSULE;
                        break;
                    case BOX:
                    default:
                        singleCollider.type = ColliderShapeData.ShapeType.BOX;
                        break;
                }



                physics.colliders.add(singleCollider);
            }



            updateWorldTransforms();


            engine.setPhysicsState(go.id, state, shape, mass, go.transform.worldTransform);
        });
    }


    public void setPhysicsComponent(GameObject go, PhysicsComponent component) {
        Gdx.app.postRunnable(() -> {
            if (engine == null || engine.isDisposed()) return;
            go.components.removeIf(c -> c instanceof PhysicsComponent);
            go.addComponent(component);


            if (component.colliders != null && !component.colliders.isEmpty()) {
                engine.setPhysicsStateFromComponent(go.id, component);
            } else {
                updateWorldTransforms();


                engine.setPhysicsState(go.id, component.state, component.shape, component.mass, go.transform.worldTransform);
            }
        });
    }


    public void setLightComponent(GameObject go, LightComponent lightData) {
        Gdx.app.postRunnable(() -> {
            if (engine == null || engine.isDisposed()) return;
            boolean wasLightBefore = go.hasComponent(LightComponent.class);
            go.components.removeIf(c -> c instanceof LightComponent);
            go.addComponent(lightData);

            if (!wasLightBefore) {
                engine.createEditorProxy(go.id);
            }

            applyLightAndTransform(go);
        });
    }

    public void setCameraComponent(GameObject go, CameraComponent cameraData) {
        Gdx.app.postRunnable(() -> {
            if (engine == null || engine.isDisposed()) return;
            boolean wasCameraBefore = go.hasComponent(CameraComponent.class);
            go.components.removeIf(c -> c instanceof CameraComponent);
            go.addComponent(cameraData);

            if (!wasCameraBefore) {
                engine.createCameraProxy(go.id);
            }


            if (cameraData.isMainCamera) {
                applyCameraComponentToEngine(go, cameraData);
            }

            findAndSetMainCamera();
        });
    }


    private void applyCameraComponentToEngine(GameObject go, CameraComponent camComp) {
        engine.setCameraFov(camComp.fieldOfView, camComp.nearPlane, camComp.farPlane);
        engine.setCameraPosition(go.transform.position.x, go.transform.position.y, go.transform.position.z);
        engine.setCameraRotation(go.transform.rotation);
    }







    public void playAnimation(GameObject go, String animationName, int loops, float speed, float transitionTime) {
        engine.playAnimation(go.id, animationName, loops, speed, transitionTime);
    }

    public void stopAnimation(GameObject go) {
        engine.stopAnimation(go.id);
    }



    public void castRay(String rayName, Vector3 from, Vector3 direction) {
        engine.castRay(rayName, from, direction);
    }

    public GameObject getRaycastHitObject(String rayName) {
        String hitId = engine.getRaycastHitObjectId(rayName);
        if (hitId != null && !hitId.isEmpty()) {
            return findGameObject(hitId);
        }
        return null;
    }

    public float getRaycastDistance(String rayName) {
        return engine.getRaycastDistance(rayName);
    }

    public float skyR = 0.1f;
    public float skyG = 0.2f;
    public float skyB = 0.3f;
    public float ambientIntensity = 1f;

    public void setBackgroundLightIntensity(float ambientIntensity) {
        this.ambientIntensity = ambientIntensity;
        engine.setBackgroundLightIntensity(ambientIntensity);
    }

    public void setSkyColor(float r, float g, float b) {
        skyR = r;
        skyG = g;
        skyB = b;
        engine.setSkyColor(skyR, skyG, skyB);
    }


    public GameObject findObjectByName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        for (GameObject go : gameObjects.values()) {
            if (name.equalsIgnoreCase(go.name)) {
                return go;
            }
        }
        return null;
    }


    public List<GameObject> findObjectsByName(String name) {
        List<GameObject> foundObjects = new ArrayList<>();
        if (name == null || name.isEmpty()) {
            return foundObjects;
        }
        for (GameObject go : gameObjects.values()) {
            if (name.equalsIgnoreCase(go.name)) {
                foundObjects.add(go);
            }
        }
        return foundObjects;
    }



    private final Json json = new Json();


    public void saveScene(FileHandle fileHandle) {
        SceneData sceneData = getCurrentSceneData();

        json.setOutputType(JsonWriter.OutputType.json);
        json.setUsePrototypes(false);
        json.setIgnoreUnknownFields(true);

        String sceneJson = json.prettyPrint(sceneData);

        fileHandle.writeString(sceneJson, false);
        Gdx.app.log("SceneManager", "Scene saved to " + fileHandle.path());
    }


    public void loadScene(FileHandle fileHandle) {
        Gdx.app.postRunnable(() -> {
            if (engine == null || engine.isDisposed()) return;
            if (fileHandle == null || !fileHandle.exists()) {
                Gdx.app.error("SceneManager", "Scene file handle is null or does not exist.");
                return;
            }

            String sceneJson = fileHandle.readString();
            json.setUsePrototypes(false);
            json.setIgnoreUnknownFields(true);
            SceneData sceneData = json.fromJson(SceneData.class, sceneJson);


            setBackgroundLightIntensity(sceneData.ambientIntensity);
            setSkyColor(sceneData.skyR, sceneData.skyG, sceneData.skyB);
            float size = (sceneData.shadowSize > 0) ? sceneData.shadowSize : 100f;
            float res = (sceneData.shadowResolution > 0) ? sceneData.shadowResolution : 2048f;
            float csmFactor = (sceneData.csmSplitFactor >= 1f) ? sceneData.csmSplitFactor : 4f;

            engine.setShadowSettings(size, (int) res, sceneData.useCSM, csmFactor);

            if (sceneData.gameObjects == null) {
                return;
            }

            for (GameObject go : sceneData.gameObjects) {
                gameObjects.put(go.id, go);
                rebuildGameObject(go);
            }

            findAndSetMainCamera();

            Gdx.app.log("SceneManager", "Scene build commands issued.");

            Gdx.app.log("SceneManager", "Applying loaded skybox: " + this.skyboxPath);
            this.skyboxPath = sceneData.skyboxPath;
            setSkybox(this.skyboxPath);
        });
    }

    private void rebuildGameObject_internal(GameObject go) {
        if (engine == null || engine.isDisposed()) {
            return;
        }

        Log.d("PhysicsDebug", "============================================================");
        Log.d("PhysicsDebug", "=== Rebuilding GameObject: '" + go.name + "' (ID: " + go.id + ")");
        Log.d("PhysicsDebug", "============================================================");

        RenderComponent render = go.getComponent(RenderComponent.class);
        if (render != null && render.modelFileName != null && !render.modelFileName.isEmpty()) {
            String modelName = render.modelFileName;
            if (modelName.startsWith("primitive:")) {
                if (modelName.equals("primitive:cylinder")) {
                    engine.createCylinder(go.id);
                } else if (modelName.equals("primitive:cube")) {
                    engine.createCube(go.id);
                } else if (modelName.equals("primitive:sphere")) {
                    engine.createSphere(go.id);
                }
            } else {
                String absolutePath;
                if (modelName.startsWith("assets://")) {
                    absolutePath = modelName.substring("assets://".length());
                } else {
                    File modelFile = ProjectManager.getInstance().getCurrentProject().getFile(modelName);
                    if (modelFile != null && modelFile.exists()) {
                        absolutePath = modelFile.getAbsolutePath();
                    } else {
                        Gdx.app.error("SceneManager", "Rebuild failed: Model file not found: " + modelName);
                        return;
                    }
                }
                if (!engine.createObject(go.id, absolutePath)) {
                    Gdx.app.error("SceneManager", "Rebuild failed: Could not create render object for " + go.id);
                    return;
                }
            }
        }

        updateWorldTransforms();

        engine.setWorldTransform(go.id, go.transform.worldTransform);

        Log.d("PhysicsDebug", "Transform for '" + go.name + "':");
        Log.d("PhysicsDebug", "  - Local Position: " + go.transform.position);
        Log.d("PhysicsDebug", "  - Local Rotation: " + go.transform.rotation);
        Log.d("PhysicsDebug", "  - Local Scale:    " + go.transform.scale);
        Log.d("PhysicsDebug", "  - World Transform Matrix:\n" + go.transform.worldTransform);
        if (go.parentId != null) {
            GameObject parent = findGameObject(go.parentId);
            if (parent != null) {
                Log.d("PhysicsDebug", "  - Parent ('"+parent.name+"') World Transform:\n" + parent.transform.worldTransform);
            }
        }

        PhysicsComponent physics = go.getComponent(PhysicsComponent.class);
        if (physics != null) {
            Matrix4 physicsTransform = new Matrix4(go.transform.worldTransform);

            if (physics.state == ThreeDManager.PhysicsState.MESH_STATIC) {
                Log.d("PhysicsDebug", "MESH_STATIC detected. Using rotation/scale only for physics baking.");
                physicsTransform.setTranslation(0, 0, 0);
            }
            if (physics.colliders != null && !physics.colliders.isEmpty()) {
                engine.setPhysicsStateFromComponent(go.id, physics, physicsTransform);
            }
            else {
                engine.setPhysicsState(go.id, physics.state, physics.shape, physics.mass, physicsTransform);
            }

            engine.setFriction(go.id, physics.friction);
            engine.setRestitution(go.id, physics.restitution);
        }

        LightComponent light = go.getComponent(LightComponent.class);
        if (light != null) {
            engine.createEditorProxy(go.id);
            applyLightAndTransform(go);
        }

        CameraComponent camera = go.getComponent(CameraComponent.class);
        if (camera != null) {
            engine.createCameraProxy(go.id);
            if (camera.isMainCamera) {
                Gdx.app.log("SceneManager", "MainCamera found: " + go.name + ". Applying its transform.");
                applyCameraComponentToEngine(go, camera);
            }
        }

        playAnimationFromComponent(go);

        MaterialComponent material = go.getComponent(MaterialComponent.class);
        if (material != null) {
            engine.applyPBRMaterial(go.id, material);
        }



        PostProcessingComponent pp = go.getComponent(PostProcessingComponent.class);
        if (pp != null) {
            engine.updatePostProcessing(pp);
        }

        ParticleSystem3DComponent newParticle = go.getComponent(ParticleSystem3DComponent.class);


        if (newParticle != null) {
            engine.createParticleProxy(go.id);
            engine.updateParticleEffect3D(go.id, newParticle, go.transform.worldTransform);
        }


        ParticleComponent oldParticle = go.getComponent(ParticleComponent.class);
        if (oldParticle != null && newParticle == null) {
            engine.createParticleProxy(go.id);
            engine.updateParticleEffect(go.id, oldParticle, go.transform.worldTransform);
        }

        KeyframeComponent anim = go.getComponent(KeyframeComponent.class);
        if (anim != null && !anim.keyframes.isEmpty()) {

            KeyframeData firstFrame = anim.keyframes.get(0);


            go.transform.position.set(firstFrame.position);
            go.transform.rotation.set(firstFrame.rotation);
            go.transform.scale.set(firstFrame.scale);


            engine.setWorldTransform(go.id, go.transform.toMatrix());


            if (anim.autoStart && !isEditorMode) {
                anim.isPlaying = true;
                anim.currentTime = 0f;
            }
        }

        PrefabComponent prefab = go.getComponent(PrefabComponent.class);
        if (prefab != null) {
            loadPrefabForObject(go, prefab);
        }
    }

    public void repositionObjectAndChildren(GameObject root, Vector3 newWorldPosition) {
        if (root == null) return;

        Vector3 currentWorldPos = root.transform.worldTransform.getTranslation(new Vector3());
        Vector3 delta = new Vector3(newWorldPosition).sub(currentWorldPos);

        repositionRecursive(root, delta);

        updateWorldTransforms();
    }

    private void applyTransformToEngineRecursive(GameObject go) {
        if (go == null) return;

        engine.setWorldTransform(go.id, go.transform.worldTransform);

        PhysicsComponent physics = go.getComponent(PhysicsComponent.class);
        if (physics != null && (physics.state == ThreeDManager.PhysicsState.STATIC || physics.state == ThreeDManager.PhysicsState.MESH_STATIC)) {

            Log.d("SceneManager", "Re-creating static physics body for '" + go.name + "' after move.");

            engine.removePhysicsBody(go.id);

            Matrix4 physicsTransform = new Matrix4(go.transform.worldTransform);
            if (physics.state == ThreeDManager.PhysicsState.MESH_STATIC) {
                physicsTransform.setTranslation(0, 0, 0);
            }

            if (physics.colliders != null && !physics.colliders.isEmpty()) {
                engine.setPhysicsStateFromComponent(go.id, physics, physicsTransform);
            } else {
                engine.setPhysicsState(go.id, physics.state, physics.shape, physics.mass, physicsTransform);
            }
        }

        for (String childId : go.childrenIds) {
            GameObject child = findGameObject(childId);
            if (child != null) {
                applyTransformToEngineRecursive(child);
            }
        }
    }

    private void repositionRecursive(GameObject current, Vector3 delta) {
        current.transform.position.add(delta);

        updateTransformRecursive(current, findGameObject(current.parentId));

        engine.setWorldTransform(current.id, current.transform.worldTransform);

        PhysicsComponent physics = current.getComponent(PhysicsComponent.class);
        if (physics != null && (physics.state == ThreeDManager.PhysicsState.STATIC || physics.state == ThreeDManager.PhysicsState.MESH_STATIC)) {
            engine.removePhysicsBody(current.id);

            Matrix4 physicsTransform = new Matrix4(current.transform.worldTransform);
            if (physics.state == ThreeDManager.PhysicsState.MESH_STATIC) {
                physicsTransform.setTranslation(0, 0, 0);
            }

            if (physics.colliders != null && !physics.colliders.isEmpty()) {
                engine.setPhysicsStateFromComponent(current.id, physics, physicsTransform);
            } else {
                engine.setPhysicsState(current.id, physics.state, physics.shape, physics.mass, physicsTransform);
            }
        }

        for (String childId : current.childrenIds) {
            GameObject child = findGameObject(childId);
            if (child != null) {
                repositionRecursive_Child(child, current);
            }
        }
    }

    private void repositionRecursive_Child(GameObject current, GameObject newParent) {
        updateTransformRecursive(current, newParent);
        engine.setWorldTransform(current.id, current.transform.worldTransform);

        PhysicsComponent physics = current.getComponent(PhysicsComponent.class);
        if (physics != null && (physics.state == ThreeDManager.PhysicsState.STATIC || physics.state == ThreeDManager.PhysicsState.MESH_STATIC)) {
            engine.removePhysicsBody(current.id);
            Matrix4 physicsTransform = new Matrix4(current.transform.worldTransform);
            if (physics.state == ThreeDManager.PhysicsState.MESH_STATIC) {
                physicsTransform.setTranslation(0, 0, 0);
            }
            if (physics.colliders != null && !physics.colliders.isEmpty()) {
                engine.setPhysicsStateFromComponent(current.id, physics, physicsTransform);
            } else {
                engine.setPhysicsState(current.id, physics.state, physics.shape, physics.mass, physicsTransform);
            }
        }

        for (String childId : current.childrenIds) {
            GameObject child = findGameObject(childId);
            if (child != null) {
                repositionRecursive_Child(child, current);
            }
        }
    }


    public void rebuildGameObject(GameObject go) {
        Gdx.app.postRunnable(() -> rebuildGameObject_internal(go));
    }

    private void clearScene_internal() {
        if (engine == null || engine.isDisposed()) return;
        engine.clearScene();
        gameObjects.clear();
        cameraAttachments.clear();
        activeAttachments.clear();
        loadingQueue.clear();
        this.skyboxPath = null;
        this.cachedFogComponent = null;
    }


    public void clearScene() {
        Gdx.app.postRunnable(this::clearScene_internal);
    }

    public void playAnimationFromComponent(GameObject go) {
        AnimationComponent anim = go.getComponent(AnimationComponent.class);
        if (anim == null || anim.animationName == null) {
            Gdx.app.postRunnable(() -> engine.stopAnimation(go.id));
            return;
        }
        Gdx.app.postRunnable(() -> {
            if (engine == null || engine.isDisposed()) return;
            engine.playAnimation(go.id, anim.animationName, anim.loops, anim.speed, anim.transitionTime);
        });
    }

    public Array<String> getAnimationNames(GameObject go) {
        return engine.getAnimationNames(go.id);
    }




    private void applyTransform(GameObject go) {
        if (go == null) return;
        TransformComponent t = go.transform;


        if (!engine.objectExists(go.id)) return;

        engine.setPosition(go.id, t.position.x, t.position.y, t.position.z);

        engine.setRotation(go.id, t.rotation);

        engine.setScale(go.id, t.scale.x, t.scale.y, t.scale.z);
    }


    public void removeRenderComponent(GameObject go) {
        if (go == null || !go.hasComponent(RenderComponent.class)) return;

        go.components.removeIf(c -> c instanceof RenderComponent);
        engine.removeObject(go.id);
    }


    public void removePhysicsComponent(GameObject go) {
        if (go == null || !go.hasComponent(PhysicsComponent.class)) return;

        go.components.removeIf(c -> c instanceof PhysicsComponent);
        engine.removePhysicsBody(go.id);
    }


    public void removeLightComponent(GameObject go) {
        if (go == null) return;

        go.components.removeIf(c -> c instanceof LightComponent);
        engine.removePBRLight(go.id);
        engine.removeEditorProxy(go.id);
    }



    private void applyLightAndTransform(GameObject go) {
        if (go == null) return;

        if (go.hasComponent(LightComponent.class)) {

            Vector3 worldPosition = go.transform.worldTransform.getTranslation(new Vector3());
            engine.updateEditorProxyPosition(go.id, worldPosition);
        }

        LightComponent light = go.getComponent(LightComponent.class);
        if (light == null) return;

        if (!isObjectActiveInHierarchy(go)) {
            engine.removePBRLight(go.id);
            return;
        }

        go.transform.worldTransform.getTranslation(tmpPos);
        go.transform.worldTransform.getRotation(tmpRot, true);

        float r = light.color.r, g = light.color.g, b = light.color.b;

        switch (light.type) {
            case SPOT:
                Vector3 spotDir = tmpScale;
                spotDir.set(0, 0, -1);
                tmpRot.transform(spotDir);
                engine.setSpotLight(go.id, tmpPos.x, tmpPos.y, tmpPos.z, spotDir.x, spotDir.y, spotDir.z,
                        r, g, b, light.intensity, light.cutoffAngle, light.exponent, light.range);
                break;
            case POINT:
                engine.setPointLight(go.id, tmpPos.x, tmpPos.y, tmpPos.z, r, g, b, light.intensity, light.range);
                break;
            case DIRECTIONAL:
                engine.removePBRLight(go.id);
                Vector3 sunDir = tmpScale;
                sunDir.set(0, 0, -1);
                tmpRot.transform(sunDir);
                engine.setRealisticSunLight(sunDir.x, sunDir.y, sunDir.z, light.intensity);
                engine.setSunLightColor(r, g, b);
                light.direction.set(sunDir);
                break;
        }
    }

    public void setRestitution(String id, float restitution) {
        engine.setRestitution(id, restitution);
    }

    public void setFriction(String id, float friction) {
        engine.setFriction(id, friction);
    }

    public Json getJson() {
        return json;
    }

    public GameObject createPrimitive(String type) {
        String baseName = type.substring(0, 1).toUpperCase() + type.substring(1);
        GameObject go = createGameObject(baseName);

        boolean success = true;


        if (success) {
            RenderComponent render = new RenderComponent();
            render.modelFileName = "primitive:" + type;
            go.addComponent(render);

            MaterialComponent material = new MaterialComponent();
            go.addComponent(material);

            go.transform.scale = new Vector3(0.1f, 0.1f, 0.1f);

            applyTransform(go);

            engine.applyPBRMaterial(go.id, material);

            rebuildGameObject(go);

            return go;
        } else {
            gameObjects.remove(go.id);
            return null;
        }
    }

    public boolean isLoading() {
        return !loadingQueue.isEmpty();
    }

    public void setMaterialComponent(GameObject go, MaterialComponent component) {
        Gdx.app.postRunnable(() -> {
            if (engine == null || engine.isDisposed()) return;
            go.components.removeIf(c -> c instanceof MaterialComponent);
            go.addComponent(component);
            engine.applyPBRMaterial(go.id, component);
        });
    }

    public boolean isObjectActiveInHierarchy(GameObject go) {
        if (go == null) return false;
        if (!go.isActive) return false;

        if (go.parentId != null) {
            return isObjectActiveInHierarchy(findGameObject(go.parentId));
        }

        return true;
    }

    public void setObjectActive(GameObject go, boolean active) {
        if (go == null || go.isActive == active) return;
        go.isActive = active;

        updateVisibilityRecursive(go);

        updateLightStateRecursive(go);
    }

    private void updateLightStateRecursive(GameObject go) {
        if (go == null) return;

        if (go.hasComponent(LightComponent.class)) {
            applyLightAndTransform(go);
        }

        for (String childId : go.childrenIds) {
            updateLightStateRecursive(findGameObject(childId));
        }
    }

    private void updateVisibilityRecursive(GameObject go) {
        if (go == null) return;

        boolean shouldBeVisible = isObjectActiveInHierarchy(go);

        engine.setObjectVisibility(go.id, shouldBeVisible);

        for (String childId : go.childrenIds) {
            updateVisibilityRecursive(findGameObject(childId));
        }
    }

    public void setParent(GameObject child, GameObject parent) {
        Gdx.app.postRunnable(() -> {
            if (engine == null || engine.isDisposed()) return;
            setParentInternal(child, parent);
        });
    }

    public void removeParent(GameObject child) {
        Gdx.app.postRunnable(() -> {
            if (engine == null || engine.isDisposed()) return;
            setParentInternal(child, null);
        });
    }

    public void setParentInternal(GameObject child, GameObject parent) {
        if (child == null || child == parent) return;

        if (parent != null && isDescendant(parent, child)) {
            Log.e("SceneManager", "Cannot set parent: would create a cycle in hierarchy.");
            return;
        }

        updateWorldTransforms();
        Matrix4 childWorldTransform = new Matrix4(child.transform.worldTransform);
        Vector3 worldPos = new Vector3();
        Quaternion worldRot = new Quaternion();
        Vector3 worldScale = new Vector3();
        child.transform.worldTransform.getTranslation(worldPos);
        child.transform.worldTransform.getRotation(worldRot, true);
        child.transform.worldTransform.getScale(worldScale);

        if (child.parentId != null) {
            GameObject oldParent = findGameObject(child.parentId);
            if (oldParent != null) {
                oldParent.childrenIds.remove(child.id);
            }
        }
        child.parentId = null;

        if (parent != null) {
            child.parentId = parent.id;
            if (!parent.childrenIds.contains(child.id)) {
                parent.childrenIds.add(child.id);
            }
            Matrix4 parentWorldTransform = parent.transform.worldTransform;
            if (Math.abs(parentWorldTransform.det()) < 0.000001f) {
                Log.e("SceneManager", "Cannot attach to a parent with zero scale.");
                return;
            }
            Matrix4 parentWorldInverse = new Matrix4(parentWorldTransform).inv();
            Matrix4 newLocalTransform = parentWorldInverse.mul(childWorldTransform);
            newLocalTransform.getTranslation(child.transform.position);
            newLocalTransform.getRotation(child.transform.rotation, true);
            newLocalTransform.getScale(child.transform.scale);
        } else {
            child.transform.position.set(worldPos);
            child.transform.rotation.set(worldRot);
            child.transform.scale.set(worldScale);
        }
        updateWorldTransforms();
    }

    private boolean isDescendant(GameObject potentialDescendant, GameObject ancestor) {
        if (potentialDescendant.parentId == null) {
            return false;
        }
        if (potentialDescendant.parentId.equals(ancestor.id)) {
            return true;
        }
        GameObject nextParent = findGameObject(potentialDescendant.parentId);
        if (nextParent == null) {
            return false;
        }
        return isDescendant(nextParent, ancestor);
    }

    public GameObject cloneGameObject(GameObject original, String newName) {
        if (newName == null) {
            newName = generateUniqueName(original.name);
        }
        if (original == null || newName.isEmpty() || gameObjects.containsKey(newName)) {
            return null;
        }

        List<GameObject> newObjects = new ArrayList<>();
        Map<String, String> oldToNewId = new HashMap<>();

        GameObject newRoot = cloneRecursiveInternal(original, newObjects, oldToNewId, newName);

        for (GameObject go : newObjects) {
            gameObjects.put(go.id, go);
        }

        updateWorldTransforms();

        Gdx.app.postRunnable(() -> {
            if (engine == null || engine.isDisposed()) return;
            for (GameObject go : newObjects) {
                rebuildGameObject_internal(go);

                MaterialComponent mat = go.getComponent(MaterialComponent.class);
                if (mat != null) {
                    engine.applyPBRMaterial(go.id, mat);
                }
            }
        });

        return newRoot;
    }

    private GameObject cloneRecursiveInternal(GameObject original, List<GameObject> list, Map<String, String> idMap, String newName) {
        String jsonStr = json.toJson(original);
        GameObject copy = json.fromJson(GameObject.class, jsonStr);

        copy.id = (newName != null) ? newName : generateUniqueName(original.name);
        copy.name = copy.id;
        copy.parentId = null;

        list.add(copy);
        idMap.put(original.id, copy.id);

        List<String> oldChildren = new ArrayList<>(copy.childrenIds);
        copy.childrenIds.clear();

        for (String oldChildId : oldChildren) {
            GameObject oldChild = findGameObject(oldChildId);
            if (oldChild != null) {
                GameObject newChild = cloneRecursiveInternal(oldChild, list, idMap, null);
                newChild.parentId = copy.id;
                copy.childrenIds.add(newChild.id);
            }
        }
        return copy;
    }


    public void loadAndAddScene(FileHandle fileHandle) {
        Gdx.app.postRunnable(() -> {
            if (engine == null || engine.isDisposed()) return;
            if (fileHandle == null || !fileHandle.exists()) {
                Gdx.app.error("SceneManager", "Additive scene file not found.");
                return;
            }
            String sceneJson = fileHandle.readString();
            json.setUsePrototypes(false);
            json.setIgnoreUnknownFields(true);
            SceneData sceneData = json.fromJson(SceneData.class, sceneJson);
            if (sceneData == null || sceneData.gameObjects == null) { return; }

            List<GameObject> newObjects = new ArrayList<>();
            Map<String, String> oldIdToNewId = new HashMap<>();


            for (GameObject go : sceneData.gameObjects) {
                String originalId = go.id;
                String newId = generateUniqueName(originalId);
                go.id = newId;
                go.name = newId;
                newObjects.add(go);
                oldIdToNewId.put(originalId, newId);
            }

            for (GameObject go : newObjects) {
                if (go.parentId != null) go.parentId = oldIdToNewId.get(go.parentId);
                ArrayList<String> newChildrenIds = new ArrayList<>();
                for (String oldChildId : go.childrenIds) {
                    String newChildId = oldIdToNewId.get(oldChildId);
                    if (newChildId != null) newChildrenIds.add(newChildId);
                }
                go.childrenIds = newChildrenIds;
            }

            for (GameObject go : newObjects) {
                gameObjects.put(go.id, go);
            }

            for (GameObject go : newObjects) {
                rebuildGameObject_internal(go);
            }
        });
    }

    public void bakeByPrefix(String prefix, String newId) {
        engine.bakeObjectsByPrefix(prefix, newId);

        List<String> toRemove = new ArrayList<>();
        for (String id : gameObjects.keySet()) {
            if (id.startsWith(prefix) && !id.equals("player") && !id.equals(newId)) {
                toRemove.add(id);
            }
        }

        for (String id : toRemove) {
            gameObjects.remove(id);
        }

        if (!gameObjects.containsKey(newId)) {
            GameObject bakedGo = new GameObject(newId);
            gameObjects.put(newId, bakedGo);
        }

        updateWorldTransforms();
    }

    private void syncCameraAttachmentsToGameObjects() {
        for (CameraAttachment att : cameraAttachments) {
            GameObject go = findGameObject(att.childId);
            if (go == null) continue;

            ModelInstance inst = engine.getModelInstance(att.childId);
            if (inst != null) {
                go.transform.worldTransform.set(inst.transform);
                if (go.parentId == null) {
                    go.transform.worldTransform.getTranslation(go.transform.position);
                    go.transform.worldTransform.getRotation(go.transform.rotation, true);
                }
            }
        }
    }

    public void promoteLightToGameObject(String lightId) {
        if (gameObjects.containsKey(lightId)) return;

        boolean found = false;
        LightComponent lightComp = new LightComponent();
        Vector3 pos = new Vector3();
        Quaternion rot = new Quaternion();

        if (engine.getPointLights().containsKey(lightId)) {
            lightComp.type = LightComponent.LightType.POINT;
            net.mgsx.gltf.scene3d.lights.PointLightEx pl = engine.getPointLights().get(lightId);
            if (pl != null) {
                lightComp.color.set(pl.color);
                lightComp.intensity = pl.intensity;
                lightComp.range = pl.range != null ? pl.range : 0f;
                pos.set(pl.position);
                found = true;
            }
        } else if (engine.getSpotLights().containsKey(lightId)) {
            lightComp.type = LightComponent.LightType.SPOT;
            net.mgsx.gltf.scene3d.lights.SpotLightEx sl = engine.getSpotLights().get(lightId);
            if (sl != null) {
                lightComp.color.set(sl.color);
                lightComp.intensity = sl.intensity;
                lightComp.range = sl.range != null ? sl.range : 0f;
                lightComp.cutoffAngle = (sl.cutoffAngle + 1000f) * (360f / 2000f);
                lightComp.exponent = sl.exponent / 1000f;
                pos.set(sl.position);

                Vector3 forward = new Vector3(0, 0, -1);
                Vector3 dir = sl.direction.cpy().nor();
                if (forward.isCollinearOpposite(dir, 0.01f)) {
                    rot.setEulerAngles(0, 180, 0);
                } else if (forward.isCollinear(dir, 0.01f)) {
                    rot.idt();
                } else {
                    rot.setFromCross(forward, dir);
                }
                found = true;
            }
        }

        if (!found) return;

        GameObject go = new GameObject(lightId);
        go.transform.position.set(pos);
        go.transform.rotation.set(rot);
        go.transform.scale.set(1, 1, 1);
        go.transform.worldTransform.set(pos, rot, go.transform.scale);

        go.addComponent(lightComp);

        RenderComponent renderComp = new RenderComponent();
        renderComp.modelFileName = "primitive:sphere";
        go.addComponent(renderComp);

        go.transform.scale.set(0.01f, 0.01f, 0.01f);

        MaterialComponent matComp = new MaterialComponent();
        matComp.baseColor = new Color(0, 0, 0, 0);
        matComp.metallic = 0f;
        matComp.roughness = 1f;
        go.addComponent(matComp);

        gameObjects.put(lightId, go);

        rebuildGameObject_internal(go);
    }

    public void applyShaderToImage(String filename, String vertexCode, String fragmentCode) {
        engine.applyShaderToImage(filename, vertexCode, fragmentCode);
    }

    private static class CameraAttachment {
        String childId;
        Matrix4 localOffsetMat = new Matrix4();

        public CameraAttachment(String id, Matrix4 offset) {
            this.childId = id;
            this.localOffsetMat.set(offset);
        }
    }
    private final List<CameraAttachment> cameraAttachments = new ArrayList<>();

    public void attachObjectToCamera(String objectId) {
        if (gameObjects.containsKey(objectId)) {
            detachObjectFromCamera(objectId);
            GameObject go = findGameObject(objectId);
            com.badlogic.gdx.graphics.Camera cam = engine.getCamera();
            if (go != null && cam != null) {
                Matrix4 camWorld = new Matrix4(cam.view).inv();
                Matrix4 offset = new Matrix4(camWorld).inv().mul(go.transform.worldTransform);
                cameraAttachments.add(new CameraAttachment(objectId, offset));
            }
        } else {
            engine.attachObjectToCamera(objectId);
        }
    }

    public void detachObjectFromCamera(String objectId) {
        cameraAttachments.removeIf(a -> a.childId.equals(objectId));
        engine.detachObjectFromCamera(objectId);
    }

    public void setCameraTracking(String targetId, int mode, float px, float py, float pz, float yaw, float pitch, float roll) {
        engine.setCameraTracking(targetId, mode, px, py, pz, yaw, pitch, roll);
    }

    public void removeObjectsByPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) return;

        Gdx.app.postRunnable(() -> {
            if (engine == null || engine.isDisposed()) return;
            java.util.List<GameObject> toRemove = new java.util.ArrayList<>();
            for (java.util.Map.Entry<String, GameObject> entry : gameObjects.entrySet()) {
                if (entry.getKey().startsWith(prefix)) {
                    toRemove.add(entry.getValue());
                }
            }

            for (GameObject go : toRemove) {
                removeGameObject(go);
            }

            engine.removeObjectsByPrefix(prefix);
        });
    }
}
