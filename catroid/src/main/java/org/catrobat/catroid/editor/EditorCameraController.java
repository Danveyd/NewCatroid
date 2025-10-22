package org.catrobat.catroid.editor;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.input.GestureDetector.GestureListener;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class EditorCameraController implements GestureListener {

    public Camera camera;
    public boolean enabled = true;
    public float rotateSpeed = 0.2f;
    public float moveSpeed = 5f;
    private int lastTouchedX, lastTouchedY;

    public float baseMoveSpeed = 5f;
    public float maxMoveSpeed = 100f;
    public float acceleration = 30f;
    private float currentMoveSpeed = baseMoveSpeed;
    public boolean isAccelerating = false;

    public final Vector3 velocity = new Vector3();
    private final Vector3 tmp = new Vector3();

    public EditorCameraController(Camera camera) {
        this.camera = camera;
    }

    public void update(float delta) {
        if (isAccelerating) {
            currentMoveSpeed = Math.min(currentMoveSpeed + acceleration * delta, maxMoveSpeed);
        } else {
            currentMoveSpeed = Math.max(currentMoveSpeed - acceleration * delta * 2, baseMoveSpeed);
        }

        if (!enabled || velocity.isZero()) {
            camera.update();
            return;
        }

        float finalSpeed = currentMoveSpeed * delta;

        if (velocity.z != 0) {
            tmp.set(camera.direction).nor().scl(finalSpeed * velocity.z);
            camera.position.add(tmp);
        }
        if (velocity.x != 0) {
            tmp.set(camera.direction).crs(camera.up).nor().scl(finalSpeed * velocity.x);
            camera.position.add(tmp);
        }
        if (velocity.y != 0) {
            tmp.set(camera.up).nor().scl(finalSpeed * velocity.y);
            camera.position.add(tmp);
        }
        camera.update();
    }

    @Override
    public boolean touchDown(float x, float y, int pointer, int button) {
        if (!enabled) return false;
        lastTouchedX = (int) x;
        lastTouchedY = (int) y;
        return true;
    }

    @Override
    public boolean pan(float x, float y, float deltaX, float deltaY) {
        if (!enabled) return false;

        camera.rotate(Vector3.Y, -deltaX * rotateSpeed);

        tmp.set(camera.direction).crs(camera.up).nor();
        camera.rotate(tmp, -deltaY * rotateSpeed);

        camera.update();
        return true;
    }

    @Override public boolean panStop(float x, float y, int pointer, int button) { return false; }
    @Override public boolean pinch(Vector2 p1, Vector2 p2, Vector2 p3, Vector2 p4) { return false; }
    @Override public void pinchStop() {}
    @Override public boolean longPress(float x, float y) { return false; }
    @Override public boolean fling(float vX, float vY, int b) { return false; }
    @Override public boolean zoom(float initialDistance, float distance) { return false; }
    @Override public boolean tap(float x, float y, int count, int button) { return false; }
}