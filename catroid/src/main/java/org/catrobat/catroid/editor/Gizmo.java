package org.catrobat.catroid.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import org.catrobat.catroid.raptor.ColliderShapeData;
import org.catrobat.catroid.raptor.GameObject;
import org.catrobat.catroid.raptor.KeyframeData;
import org.catrobat.catroid.raptor.PhysicsComponent;
import org.catrobat.catroid.raptor.SceneManager;

public class Gizmo {

    private enum Axis { NONE, X, Y, Z }

    private final SceneManager sceneManager;
    private final Camera camera;
    private final EditorActivity activity;

    private ModelInstance gizmoTranslateX, gizmoTranslateY, gizmoTranslateZ;
    private ModelInstance gizmoRotateX, gizmoRotateY, gizmoRotateZ;
    private ModelInstance gizmoScaleBoxX, gizmoScaleBoxY, gizmoScaleBoxZ;
    private ModelInstance gizmoScaleLineX, gizmoScaleLineY, gizmoScaleLineZ;
    private final BoundingBox boxX = new BoundingBox(), boxY = new BoundingBox(), boxZ = new BoundingBox();

    private EditorTool currentTool = EditorTool.TRANSLATE;
    private Axis selectedAxis = Axis.NONE;


    private GameObject selectedObject = null;
    private ColliderShapeData selectedCollider = null;
    private KeyframeData selectedKeyframe = null;

    private final Vector3 intersectionPoint = new Vector3();
    private final Plane dragPlane = new Plane();
    private final Vector3 dragStartPoint = new Vector3();
    private final Vector3 dragCurrentPoint = new Vector3();
    private final Quaternion lastObjectRotation = new Quaternion();

    private final Vector3 dragStartPos = new Vector3();
    private final Quaternion dragStartRot = new Quaternion();
    private final Vector3 dragStartScale = new Vector3();
    private boolean isTransforming = false;

    public boolean snapEnabled = false;
    public float snapSize = 1.0f;
    public float rotationSnapSize = 15.0f;

    private final Vector3 initialObjectPos = new Vector3();
    private final Quaternion initialObjectRot = new Quaternion();
    private final Vector3 initialObjectScale = new Vector3();
    private final Vector3 initialTouchPoint = new Vector3();

    private float snap(float value, float step) {
        if (!snapEnabled || step <= 0.0001f) return value;
        return Math.round(value / step) * step;
    }


    public Gizmo(EditorActivity activity, SceneManager sceneManager, Camera camera) {
        this.activity = activity;
        this.sceneManager = sceneManager;
        this.camera = camera;
        createModels();
    }

    private void createModels() {
        ModelBuilder modelBuilder = new ModelBuilder();
        long usage = Usage.Position | Usage.Normal;

        Model arrowXModel = modelBuilder.createArrow(0, 0, 0, 1, 0, 0, 0.25f, 0.1f, 10, GL20.GL_TRIANGLES, new Material(ColorAttribute.createDiffuse(Color.RED)), usage);
        Model arrowYModel = modelBuilder.createArrow(0, 0, 0, 0, 1, 0, 0.25f, 0.1f, 10, GL20.GL_TRIANGLES, new Material(ColorAttribute.createDiffuse(Color.GREEN)), usage);
        Model arrowZModel = modelBuilder.createArrow(0, 0, 0, 0, 0, 1, 0.25f, 0.1f, 10, GL20.GL_TRIANGLES, new Material(ColorAttribute.createDiffuse(Color.BLUE)), usage);
        gizmoTranslateX = new ModelInstance(arrowXModel);
        gizmoTranslateY = new ModelInstance(arrowYModel);
        gizmoTranslateZ = new ModelInstance(arrowZModel);

        Model ringXModel = modelBuilder.createCylinder(2, 0.04f, 2, 32, new Material(ColorAttribute.createDiffuse(Color.RED)), usage);
        gizmoRotateX = new ModelInstance(ringXModel);

        Model ringYModel = modelBuilder.createCylinder(2, 0.04f, 2, 32, new Material(ColorAttribute.createDiffuse(Color.GREEN)), usage);
        gizmoRotateY = new ModelInstance(ringYModel);

        Model ringZModel = modelBuilder.createCylinder(2, 0.04f, 2, 32, new Material(ColorAttribute.createDiffuse(Color.BLUE)), usage);
        gizmoRotateZ = new ModelInstance(ringZModel);

        Model cubeModel = modelBuilder.createBox(0.2f, 0.2f, 0.2f, new Material(), usage);
        gizmoScaleBoxX = new ModelInstance(cubeModel);
        gizmoScaleBoxX.materials.get(0).set(ColorAttribute.createDiffuse(Color.RED));
        gizmoScaleBoxY = new ModelInstance(cubeModel);
        gizmoScaleBoxY.materials.get(0).set(ColorAttribute.createDiffuse(Color.GREEN));
        gizmoScaleBoxZ = new ModelInstance(cubeModel);
        gizmoScaleBoxZ.materials.get(0).set(ColorAttribute.createDiffuse(Color.BLUE));

        Model stickModel = modelBuilder.createCylinder(0.05f, 1f, 0.05f, 8, new Material(ColorAttribute.createDiffuse(Color.WHITE)), usage);
        gizmoScaleLineX = new ModelInstance(stickModel);
        gizmoScaleLineX.materials.get(0).set(ColorAttribute.createDiffuse(Color.RED));
        gizmoScaleLineY = new ModelInstance(stickModel);
        gizmoScaleLineY.materials.get(0).set(ColorAttribute.createDiffuse(Color.GREEN));
        gizmoScaleLineZ = new ModelInstance(stickModel);
        gizmoScaleLineZ.materials.get(0).set(ColorAttribute.createDiffuse(Color.BLUE));
    }


    public void setSelectedObject(GameObject go) {
        this.selectedObject = go;
        this.selectedCollider = null;
        this.selectedKeyframe = null;
    }

    public void setSelected(GameObject go, ColliderShapeData collider) {
        this.selectedObject = go;
        this.selectedCollider = collider;
        this.selectedKeyframe = null;
    }

    public void setSelectedKeyframe(GameObject owner, KeyframeData frame) {
        this.selectedObject = owner;
        this.selectedCollider = null;
        this.selectedKeyframe = frame;
    }

    public KeyframeData getSelectedKeyframe() {
        return selectedKeyframe;
    }

    public void setCurrentTool(EditorTool tool) {
        this.currentTool = tool;
    }

    public boolean isDragging() {
        return selectedAxis != Axis.NONE;
    }



    private Vector3 getGizmoPosition() {
        if (selectedKeyframe != null) {
            return selectedKeyframe.position;
        }
        if (selectedObject == null) {
            return Vector3.Zero;
        }

        Vector3 objectWorldPosition = selectedObject.transform.worldTransform.getTranslation(new Vector3());

        if (selectedCollider != null) {
            Vector3 localOffset = selectedCollider.centerOffset;
            Quaternion worldRotation = selectedObject.transform.worldTransform.getRotation(new Quaternion());
            Vector3 rotatedOffset = worldRotation.transform(new Vector3(localOffset));

            return objectWorldPosition.add(rotatedOffset);
        }

        return objectWorldPosition;
    }

    public void render(ModelBatch batch) {
        if (selectedObject == null || currentTool == EditorTool.HAND) return;

        Vector3 pos = getGizmoPosition();
        Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
        batch.begin(camera);
        float distance = camera.position.dst(pos);
        float scale = distance * 0.15f;

        switch (currentTool) {
            case TRANSLATE:
                renderAxis(batch, gizmoTranslateX, pos, scale);
                renderAxis(batch, gizmoTranslateY, pos, scale);
                renderAxis(batch, gizmoTranslateZ, pos, scale);
                break;
            case ROTATE:
                float ringScale = scale * 0.6f;
                gizmoRotateX.transform.setToTranslation(pos).scl(ringScale).rotate(Vector3.Z, 90);
                gizmoRotateY.transform.setToTranslation(pos).scl(ringScale);
                gizmoRotateZ.transform.setToTranslation(pos).scl(ringScale).rotate(Vector3.X, 90);
                batch.render(gizmoRotateX);
                batch.render(gizmoRotateY);
                batch.render(gizmoRotateZ);
                break;
            case SCALE:
                gizmoScaleLineX.transform.setToTranslation(pos).scl(scale).rotate(Vector3.Z, -90).translate(0, 0.5f, 0);
                gizmoScaleLineY.transform.setToTranslation(pos).scl(scale).translate(0, 0.5f, 0);
                gizmoScaleLineZ.transform.setToTranslation(pos).scl(scale).rotate(Vector3.X, 90).translate(0, 0.5f, 0);
                batch.render(gizmoScaleLineX);
                batch.render(gizmoScaleLineY);
                batch.render(gizmoScaleLineZ);
                gizmoScaleBoxX.transform.setToTranslation(pos).translate(scale, 0, 0).scl(scale);
                gizmoScaleBoxY.transform.setToTranslation(pos).translate(0, scale, 0).scl(scale);
                gizmoScaleBoxZ.transform.setToTranslation(pos).translate(0, 0, scale).scl(scale);
                batch.render(gizmoScaleBoxX);
                batch.render(gizmoScaleBoxY);
                batch.render(gizmoScaleBoxZ);
                break;
        }
        batch.end();
    }

    private void renderAxis(ModelBatch batch, ModelInstance instance, Vector3 position, float scale) {
        instance.transform.setToTranslation(position).scl(scale);
        batch.render(instance);
    }

    public void touchDragged(Ray pickRay) {
        if (selectedAxis == Axis.NONE || selectedObject == null) return;
        if (!Intersector.intersectRayPlane(pickRay, dragPlane, dragCurrentPoint)) return;

        Vector3 dragVector = dragCurrentPoint.cpy().sub(dragStartPoint);

        Vector3 totalDragVector = dragCurrentPoint.cpy().sub(initialTouchPoint);

        Vector3 axisVector = new Vector3();
        if (selectedAxis == Axis.X) axisVector.set(1, 0, 0);
        if (selectedAxis == Axis.Y) axisVector.set(0, 1, 0);
        if (selectedAxis == Axis.Z) axisVector.set(0, 0, 1);

        if (selectedCollider != null) {
            PhysicsComponent physics = selectedObject.getComponent(PhysicsComponent.class);
            if (physics == null) return;

            switch (currentTool) {
                case TRANSLATE: {
                    float projection = dragVector.dot(axisVector);
                    Vector3 worldTranslation = axisVector.cpy().scl(projection);

                    Quaternion invRot = selectedObject.transform.worldTransform.getRotation(new Quaternion()).conjugate();
                    invRot.transform(worldTranslation);

                    selectedCollider.centerOffset.add(worldTranslation);
                    break;
                }
                case SCALE: {
                    float projection = dragVector.dot(axisVector);
                    Vector3 scaleAmount = axisVector.cpy().scl(projection * 0.5f);

                    if (selectedCollider.type == ColliderShapeData.ShapeType.SPHERE || selectedCollider.type == ColliderShapeData.ShapeType.CAPSULE) {
                        float amount = scaleAmount.len() * Math.signum(scaleAmount.dot(axisVector));
                        selectedCollider.radius = Math.max(0.01f, selectedCollider.radius + amount);
                        if (selectedCollider.type == ColliderShapeData.ShapeType.CAPSULE) {
                            selectedCollider.size.y = Math.max(0.01f, selectedCollider.size.y + amount * 2);
                        }
                    } else {
                        selectedCollider.size.add(scaleAmount);
                        selectedCollider.size.x = Math.max(0.01f, selectedCollider.size.x);
                        selectedCollider.size.y = Math.max(0.01f, selectedCollider.size.y);
                        selectedCollider.size.z = Math.max(0.01f, selectedCollider.size.z);
                    }
                    break;
                }
                case ROTATE:
                    break;
            }
            // sceneManager.setPhysicsComponent(selectedObject, physics);

        } else if (selectedKeyframe != null) {
            switch (currentTool) {
                case TRANSLATE: {
                    float projection = dragVector.dot(axisVector);
                    selectedKeyframe.position.add(axisVector.cpy().scl(projection));
                    break;
                }
                case SCALE: {
                    float projection = dragVector.dot(axisVector);
                    float scaleAmount = projection * 0.1f;
                    selectedKeyframe.scale.add(axisVector.cpy().scl(scaleAmount));
                    break;
                }
                case ROTATE: {
                    Vector3 currentVec = dragCurrentPoint.cpy().sub(getGizmoPosition());
                    Vector3 startVec = dragStartPoint.cpy().sub(getGizmoPosition());

                    Vector3 planeNormal = axisVector;
                    Vector3 projectedStart = startVec.cpy().sub(planeNormal.cpy().scl(startVec.dot(planeNormal)));
                    Vector3 projectedCurrent = currentVec.cpy().sub(planeNormal.cpy().scl(currentVec.dot(planeNormal)));

                    projectedStart.nor();
                    projectedCurrent.nor();

                    float angle = (float) Math.toDegrees(Math.acos(projectedStart.dot(projectedCurrent)));

                    if (Float.isNaN(angle) || angle < 0.01f) break;

                    Vector3 cross = projectedStart.crs(projectedCurrent);
                    float sign = Math.signum(cross.dot(axisVector));

                    Quaternion deltaRotation = new Quaternion(axisVector, angle * sign);
                    selectedKeyframe.rotation.mulLeft(deltaRotation);
                    break;
                }
            }
        } else {
            switch (currentTool) {
                case TRANSLATE: {
                    float projection = totalDragVector.dot(axisVector);
                    Vector3 worldTranslation = axisVector.cpy().scl(projection);

                    if (selectedObject.parentId != null) {
                        GameObject parent = sceneManager.findGameObject(selectedObject.parentId);
                        if (parent != null) {
                            Quaternion parentInverseRotation = parent.transform.worldTransform.getRotation(new Quaternion()).conjugate();
                            parentInverseRotation.transform(worldTranslation);
                        }
                    }

                    Vector3 targetPos = initialObjectPos.cpy().add(worldTranslation);
                    if (snapEnabled) {
                        targetPos.x = snap(targetPos.x, snapSize);
                        targetPos.y = snap(targetPos.y, snapSize);
                        targetPos.z = snap(targetPos.z, snapSize);
                    }
                    selectedObject.transform.position.set(targetPos);
                    break;
                }
                case SCALE: {
                    float projection = totalDragVector.dot(axisVector);
                    float scaleAmount = projection * 0.5f;
                    Vector3 scaleVec = axisVector.cpy().scl(scaleAmount);

                    Vector3 targetScale = initialObjectScale.cpy().add(scaleVec);
                    if (snapEnabled) {
                        targetScale.x = Math.max(0.01f, snap(targetScale.x, snapSize));
                        targetScale.y = Math.max(0.01f, snap(targetScale.y, snapSize));
                        targetScale.z = Math.max(0.01f, snap(targetScale.z, snapSize));
                    }
                    selectedObject.transform.scale.set(targetScale);
                    break;
                }
                case ROTATE: {
                    Vector3 currentVec = dragCurrentPoint.cpy().sub(getGizmoPosition());
                    Vector3 startVec = initialTouchPoint.cpy().sub(getGizmoPosition());

                    Vector3 planeNormal = axisVector;
                    Vector3 projectedStart = startVec.cpy().sub(planeNormal.cpy().scl(startVec.dot(planeNormal)));
                    Vector3 projectedCurrent = currentVec.cpy().sub(planeNormal.cpy().scl(currentVec.dot(planeNormal)));

                    projectedStart.nor();
                    projectedCurrent.nor();

                    float angle = (float) Math.toDegrees(Math.acos(projectedStart.dot(projectedCurrent)));
                    if (Float.isNaN(angle) || angle < 0.01f) break;

                    Vector3 cross = projectedStart.crs(projectedCurrent);
                    float sign = Math.signum(cross.dot(axisVector));

                    float finalAngle = angle * sign;
                    if (snapEnabled) {
                        finalAngle = snap(finalAngle, rotationSnapSize);
                    }

                    Quaternion deltaRotation = new Quaternion(axisVector, finalAngle);
                    selectedObject.transform.rotation.set(initialObjectRot).mulLeft(deltaRotation);
                    break;
                }
            }
        }
        dragStartPoint.set(dragCurrentPoint);
    }

    public boolean touchDown(Ray pickRay) {
        if (selectedObject == null || currentTool == EditorTool.HAND) return false;

        selectedAxis = Axis.NONE;
        float closestDist = Float.MAX_VALUE;

        ModelInstance handleX, handleY, handleZ;
        switch (currentTool) {
            case TRANSLATE: handleX = gizmoTranslateX; handleY = gizmoTranslateY; handleZ = gizmoTranslateZ; break;
            case ROTATE:    handleX = gizmoRotateX;    handleY = gizmoRotateY;    handleZ = gizmoRotateZ;    break;
            case SCALE:
                handleX = gizmoScaleBoxX;     handleY = gizmoScaleBoxY;     handleZ = gizmoScaleBoxZ;
                break;
            default: return false;
        }

        if (currentTool == EditorTool.ROTATE) {
            lastObjectRotation.set(selectedObject.transform.rotation);
        }

        dragStartPos.set(selectedObject.transform.position);
        dragStartRot.set(selectedObject.transform.rotation);
        dragStartScale.set(selectedObject.transform.scale);
        isTransforming = true;

        handleX.calculateBoundingBox(boxX).mul(handleX.transform);
        handleY.calculateBoundingBox(boxY).mul(handleY.transform);
        handleZ.calculateBoundingBox(boxZ).mul(handleZ.transform);

        float dist;
        if ((dist = intersect(pickRay, boxX)) < closestDist) { closestDist = dist; selectedAxis = Axis.X; }
        if ((dist = intersect(pickRay, boxY)) < closestDist) { closestDist = dist; selectedAxis = Axis.Y; }
        if ((dist = intersect(pickRay, boxZ)) < closestDist) { closestDist = dist; selectedAxis = Axis.Z; }

        if (selectedAxis != Axis.NONE) {
            setupDragPlane(getGizmoPosition());

            Intersector.intersectRayPlane(pickRay, dragPlane, initialTouchPoint);
            initialObjectPos.set(selectedObject.transform.position);
            initialObjectRot.set(selectedObject.transform.rotation);
            initialObjectScale.set(selectedObject.transform.scale);

            if (currentTool == EditorTool.ROTATE) {
                lastObjectRotation.set(selectedObject.transform.rotation);
            }
            return true;
        }
        return false;
    }

    private float intersect(Ray ray, BoundingBox box) {
        if (Intersector.intersectRayBounds(ray, box, intersectionPoint)) {
            return ray.origin.dst2(intersectionPoint);
        }
        return Float.MAX_VALUE;
    }

    private void setupDragPlane(Vector3 origin) {
        Vector3 planeNormal = new Vector3(camera.direction).scl(-1);
        dragPlane.set(origin, planeNormal);
    }

    public void touchUp() {
        if (isTransforming && selectedObject != null) {
            isTransforming = false;

            if (!dragStartPos.epsilonEquals(selectedObject.transform.position, 0.001f) ||
                    !dragStartRot.equals(selectedObject.transform.rotation) ||
                    !dragStartScale.epsilonEquals(selectedObject.transform.scale, 0.001f)) {

                activity.getUndoManager().pushCommand(
                        new Commands.TransformCommand(sceneManager, selectedObject, dragStartPos, dragStartRot, dragStartScale)
                );
            }
        }

        if (currentTool == EditorTool.ROTATE && selectedObject != null){
            lastObjectRotation.set(selectedObject.transform.rotation);
        }

        if (selectedObject != null && selectedAxis != Axis.NONE) {
            if (selectedCollider != null) {
                PhysicsComponent physics = selectedObject.getComponent(PhysicsComponent.class);
                if (physics != null) {
                    sceneManager.setPhysicsComponent(selectedObject, physics);
                }
            }
            activity.runOnUiThread(() -> {
                if (activity.getInspectorManager() != null) {
                    activity.getInspectorManager().populateInspector(selectedObject);
                }
            });
        }
        selectedAxis = Axis.NONE;
    }

    public GameObject getSelectedObject() {
        return selectedObject;
    }
}
