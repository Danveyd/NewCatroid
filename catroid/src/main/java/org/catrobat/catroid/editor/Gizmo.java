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
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;

import org.catrobat.catroid.raptor.ColliderShapeData;
import org.catrobat.catroid.raptor.GameObject;
import org.catrobat.catroid.raptor.PhysicsComponent;
import org.catrobat.catroid.raptor.SceneManager;
import org.catrobat.catroid.raptor.TransformComponent;

public class Gizmo {

    private enum Axis {
        NONE, X, Y, Z
    }

    private final SceneManager sceneManager;
    private final Camera camera;

    private ModelInstance gizmoTranslateX, gizmoTranslateY, gizmoTranslateZ;
    private ModelInstance gizmoRotateX, gizmoRotateY, gizmoRotateZ;
    private ModelInstance gizmoScaleX, gizmoScaleY, gizmoScaleZ;
    private final BoundingBox boxX = new BoundingBox(), boxY = new BoundingBox(), boxZ = new BoundingBox();

    private EditorTool currentTool = EditorTool.TRANSLATE;
    private GameObject selectedObject = null;
    private Axis selectedAxis = Axis.NONE;

    private final Vector3 intersectionPoint = new Vector3();
    private final Plane dragPlane = new Plane();
    private final Vector3 dragStartPoint = new Vector3();
    private final Vector3 dragCurrentPoint = new Vector3();

    private final Quaternion identityQuaternion = new Quaternion();

    private final Quaternion baseRotX = new Quaternion(Vector3.Z, 90);
    private final Quaternion baseRotY = new Quaternion();
    private final Quaternion baseRotZ = new Quaternion(Vector3.X, 90);

    private ColliderShapeData selectedCollider = null;

    public Gizmo(SceneManager sceneManager, Camera camera) {
        this.sceneManager = sceneManager;
        this.camera = camera;
        createModels();
    }

    private void createModels() {
        ModelBuilder modelBuilder = new ModelBuilder();
        float axisLength = 1f;
        float arrowHeadSize = 0.25f;
        float arrowStemSize = 0.1f;
        float ringRadius = axisLength * 0.5f;
        float scaleHandleSize = 0.15f;

        Model arrowXModel = modelBuilder.createArrow(0, 0, 0, axisLength, 0, 0, arrowHeadSize, arrowStemSize, 10, GL20.GL_TRIANGLES, new Material(ColorAttribute.createDiffuse(Color.RED)), Usage.Position | Usage.Normal);
        Model arrowYModel = modelBuilder.createArrow(0, 0, 0, 0, axisLength, 0, arrowHeadSize, arrowStemSize, 10, GL20.GL_TRIANGLES, new Material(ColorAttribute.createDiffuse(Color.GREEN)), Usage.Position | Usage.Normal);
        Model arrowZModel = modelBuilder.createArrow(0, 0, 0, 0, 0, axisLength, arrowHeadSize, arrowStemSize, 10, GL20.GL_TRIANGLES, new Material(ColorAttribute.createDiffuse(Color.BLUE)), Usage.Position | Usage.Normal);
        gizmoTranslateX = new ModelInstance(arrowXModel);
        gizmoTranslateY = new ModelInstance(arrowYModel);
        gizmoTranslateZ = new ModelInstance(arrowZModel);

        Model ringModel = modelBuilder.createCylinder(ringRadius * 2, 0.02f, ringRadius * 2, 32, new Material(), Usage.Position | Usage.Normal);
        gizmoRotateX = new ModelInstance(ringModel);
        gizmoRotateX.materials.get(0).set(ColorAttribute.createDiffuse(Color.RED));
        gizmoRotateX.transform.rotate(Vector3.Z, 90);

        gizmoRotateY = new ModelInstance(ringModel);
        gizmoRotateY.materials.get(0).set(ColorAttribute.createDiffuse(Color.GREEN));

        gizmoRotateZ = new ModelInstance(ringModel);
        gizmoRotateZ.materials.get(0).set(ColorAttribute.createDiffuse(Color.BLUE));
        gizmoRotateZ.transform.rotate(Vector3.X, 90);


        Model cubeModel = modelBuilder.createBox(scaleHandleSize, scaleHandleSize, scaleHandleSize, new Material(), Usage.Position | Usage.Normal);
        gizmoScaleX = new ModelInstance(cubeModel);
        gizmoScaleX.materials.get(0).set(ColorAttribute.createDiffuse(Color.RED));

        gizmoScaleY = new ModelInstance(cubeModel);
        gizmoScaleY.materials.get(0).set(ColorAttribute.createDiffuse(Color.GREEN));

        gizmoScaleZ = new ModelInstance(cubeModel);
        gizmoScaleZ.materials.get(0).set(ColorAttribute.createDiffuse(Color.BLUE));
    }

    public void setSelected(GameObject go, ColliderShapeData collider) {
        this.selectedObject = go;
        this.selectedCollider = collider;
    }

    public void setSelectedObject(GameObject go) {
        this.selectedObject = go;
    }

    public void setCurrentTool(EditorTool tool) {
        this.currentTool = tool;
    }

    public boolean isDragging() {
        return selectedAxis != Axis.NONE;
    }

    public void render(ModelBatch batch) {
        if (selectedObject == null || currentTool == EditorTool.HAND) return;

        Vector3 pos = new Vector3();
        if (selectedCollider != null) {
            Vector3 worldOffset = selectedCollider.centerOffset.cpy();
            selectedObject.transform.rotation.transform(worldOffset);
            pos.set(selectedObject.transform.position).add(worldOffset);
        } else {
            pos.set(selectedObject.transform.position);
        }

        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        batch.begin(camera);

        float distance = camera.position.dst(pos);
        float scale = distance * 0.15f;

        switch (currentTool) {
            case TRANSLATE:
                gizmoTranslateX.transform.set(pos, identityQuaternion, new Vector3(scale, scale, scale));
                gizmoTranslateY.transform.set(pos, identityQuaternion, new Vector3(scale, scale, scale));
                gizmoTranslateZ.transform.set(pos, identityQuaternion, new Vector3(scale, scale, scale));
                batch.render(gizmoTranslateX);
                batch.render(gizmoTranslateY);
                batch.render(gizmoTranslateZ);
                break;
            case ROTATE:
                gizmoRotateX.transform.set(baseRotX).scl(scale).setTranslation(pos);
                gizmoRotateY.transform.set(baseRotY).scl(scale).setTranslation(pos);
                gizmoRotateZ.transform.set(baseRotZ).scl(scale).setTranslation(pos);
                batch.render(gizmoRotateX);
                batch.render(gizmoRotateY);
                batch.render(gizmoRotateZ);
                break;
            case SCALE:
                float handleOffset = 1f * scale;
                gizmoScaleX.transform.set(pos, identityQuaternion, new Vector3(scale, scale, scale)).translate(handleOffset, 0, 0);
                gizmoScaleY.transform.set(pos, identityQuaternion, new Vector3(scale, scale, scale)).translate(0, handleOffset, 0);
                gizmoScaleZ.transform.set(pos, identityQuaternion, new Vector3(scale, scale, scale)).translate(0, 0, handleOffset);
                batch.render(gizmoScaleX);
                batch.render(gizmoScaleY);
                batch.render(gizmoScaleZ);
                break;
        }
        batch.end();
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
    }

    private void renderAxis(ModelBatch batch, ModelInstance instance, Vector3 position, float scale) {
        Quaternion originalRotation = new Quaternion();
        instance.transform.getRotation(originalRotation);
        instance.transform.set(position, originalRotation, new Vector3(scale, scale, scale));
        batch.render(instance);
    }

    public boolean touchDown(Ray pickRay) {
        if (selectedObject == null || currentTool == EditorTool.HAND) return false;

        selectedAxis = Axis.NONE;
        float closestDist = Float.MAX_VALUE;

        ModelInstance handleX=null, handleY=null, handleZ=null;
        switch(currentTool){
            case TRANSLATE: handleX = gizmoTranslateX; handleY = gizmoTranslateY; handleZ = gizmoTranslateZ; break;
            case ROTATE: handleX = gizmoRotateX; handleY = gizmoRotateY; handleZ = gizmoRotateZ; break;
            case SCALE: handleX = gizmoScaleX; handleY = gizmoScaleY; handleZ = gizmoScaleZ; break;
            default: return false;
        }

        handleX.calculateBoundingBox(boxX).mul(handleX.transform);
        handleY.calculateBoundingBox(boxY).mul(handleY.transform);
        handleZ.calculateBoundingBox(boxZ).mul(handleZ.transform);

        float dist;
        if ((dist = intersect(pickRay, boxX)) < closestDist) { closestDist = dist; selectedAxis = Axis.X; }
        if ((dist = intersect(pickRay, boxY)) < closestDist) { closestDist = dist; selectedAxis = Axis.Y; }
        if ((dist = intersect(pickRay, boxZ)) < closestDist) { closestDist = dist; selectedAxis = Axis.Z; }

        if (selectedAxis != Axis.NONE) {
            setupDragPlane(selectedObject.transform.position);
            Intersector.intersectRayPlane(pickRay, dragPlane, dragStartPoint);
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

    public void touchDragged(Ray pickRay) {
        if (selectedAxis == Axis.NONE || selectedObject == null) return;

        if (!Intersector.intersectRayPlane(pickRay, dragPlane, dragCurrentPoint)) {
            return;
        }
        Vector3 dragVector = dragCurrentPoint.cpy().sub(dragStartPoint);

        Vector3 axisVector = new Vector3();
        if (selectedAxis == Axis.X) axisVector.set(1, 0, 0);
        if (selectedAxis == Axis.Y) axisVector.set(0, 1, 0);
        if (selectedAxis == Axis.Z) axisVector.set(0, 0, 1);

        if (selectedCollider != null) {
            switch(currentTool) {
                case TRANSLATE: {
                    float projection = dragVector.dot(axisVector);
                    Vector3 worldTranslation = axisVector.cpy().scl(projection);

                    Quaternion invRot = selectedObject.transform.rotation.cpy().conjugate();
                    invRot.transform(worldTranslation);

                    selectedCollider.centerOffset.add(worldTranslation);
                    break;
                }
                case SCALE: {
                    float projection = dragVector.dot(axisVector);
                    Vector3 scaleAmount = axisVector.cpy().scl(projection * 0.5f);

                    if (selectedCollider.type == ColliderShapeData.ShapeType.SPHERE || selectedCollider.type == ColliderShapeData.ShapeType.CAPSULE) {
                        float amount = scaleAmount.len() * Math.signum(scaleAmount.dot(axisVector));
                        selectedCollider.radius += amount;
                        if(selectedCollider.type == ColliderShapeData.ShapeType.CAPSULE){
                            selectedCollider.size.y += amount * 2;
                        }
                    } else {
                        selectedCollider.size.add(scaleAmount);
                    }
                    break;
                }
                case ROTATE:
                    break;
            }
            sceneManager.setPhysicsComponent(selectedObject, selectedObject.getComponent(PhysicsComponent.class));
        } else {
            switch(currentTool) {
                case TRANSLATE: {
                    float projection = dragVector.dot(axisVector);
                    Vector3 translation = axisVector.cpy().scl(projection);
                    Vector3 newPos = selectedObject.transform.position.cpy().add(translation);
                    sceneManager.setPosition(selectedObject, newPos);
                    break;
                }
                case SCALE: {
                    float projection = dragVector.dot(axisVector);
                    Vector3 translation = axisVector.cpy().scl(projection);
                    Vector3 newScale = selectedObject.transform.scale.cpy().add(translation.scl(0.5f));
                    sceneManager.setScale(selectedObject, newScale);
                    break;
                }
                case ROTATE: {
                    Vector3 center = selectedObject.transform.position;
                    Vector3 startVec = dragStartPoint.cpy().sub(center).nor();
                    Vector3 currentVec = dragCurrentPoint.cpy().sub(center).nor();
                    float angle = (float)Math.toDegrees(Math.acos(startVec.dot(currentVec)));

                    if (Float.isNaN(angle) || angle < 0.01f) break;

                    Vector3 cross = startVec.crs(currentVec);
                    float sign = Math.signum(cross.dot(axisVector));

                    Quaternion deltaRotation = new Quaternion(axisVector, angle * sign);
                    sceneManager.rotate(selectedObject, deltaRotation);
                    break;
                }
            }
        }

        dragStartPoint.set(dragCurrentPoint);
    }

    public void touchUp() {
        selectedAxis = Axis.NONE;
    }

    public GameObject getSelectedObject() {
        return selectedObject;
    }
}