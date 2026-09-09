package org.catrobat.catroid.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.input.GestureDetector.GestureListener;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class EditorCameraController implements GestureListener {

    public Camera camera;
    public boolean enabled = true;
    public boolean isPcMode = false;

    public float rotateSpeed = 0.2f;

    public float baseMoveSpeed = 5f;
    public float maxMoveSpeed = 100f;
    public float acceleration = 30f;
    private float currentMoveSpeed = baseMoveSpeed;

    public boolean isAccelerating = false;
    public boolean isButtonAccelerating = false;

    public final Vector3 buttonVelocity = new Vector3();
    public final Vector3 keyboardVelocity = new Vector3();
    private final Vector3 totalVelocity = new Vector3();
    private final Vector3 tmp = new Vector3();

    public EditorCameraController(Camera camera) {
        this.camera = camera;
    }

    public void onCameraMove(float vx, float vy, float vz) {
        buttonVelocity.add(vx, vy, vz);
    }

    public void onCameraAccelerate(boolean active) {
        isButtonAccelerating = active;
    }

    public void update(float delta) {
        if (!enabled) return;

        if (isPcMode) {
            handlePcInputs();
        } else {
            keyboardVelocity.setZero();
        }

        isAccelerating = isButtonAccelerating || (isPcMode && Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT));

        if (isAccelerating) {
            currentMoveSpeed = Math.min(currentMoveSpeed + acceleration * delta, maxMoveSpeed);
        } else {
            currentMoveSpeed = Math.max(currentMoveSpeed - acceleration * delta * 2, baseMoveSpeed);
        }

        totalVelocity.set(buttonVelocity).add(keyboardVelocity);

        if (totalVelocity.isZero()) {
            camera.update();
            return;
        }

        float finalSpeed = currentMoveSpeed * delta;

        if (totalVelocity.z != 0) {
            tmp.set(camera.direction).nor().scl(finalSpeed * totalVelocity.z);
            camera.position.add(tmp);
        }
        if (totalVelocity.x != 0) {
            tmp.set(camera.direction).crs(camera.up).nor().scl(finalSpeed * totalVelocity.x);
            camera.position.add(tmp);
        }
        if (totalVelocity.y != 0) {
            tmp.set(camera.up).nor().scl(finalSpeed * totalVelocity.y);
            camera.position.add(tmp);
        }
        camera.update();
    }

    private void handlePcInputs() {
        keyboardVelocity.setZero();

        if (Gdx.input.isKeyPressed(Input.Keys.W)) keyboardVelocity.z += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) keyboardVelocity.z -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) keyboardVelocity.x -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) keyboardVelocity.x += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.E)) keyboardVelocity.y += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.Q)) keyboardVelocity.y -= 1;

        if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT) || Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            float deltaX = -Gdx.input.getDeltaX() * rotateSpeed;
            float deltaY = -Gdx.input.getDeltaY() * rotateSpeed;

            if (deltaX != 0) {
                camera.rotate(Vector3.Y, deltaX);
            }
            if (deltaY != 0) {
                tmp.set(camera.direction).crs(camera.up).nor();
                camera.rotate(tmp, deltaY);
            }
        } else {
            Gdx.input.getDeltaX();
            Gdx.input.getDeltaY();
        }
    }

    @Override
    public boolean pan(float x, float y, float deltaX, float deltaY) {
        if (!enabled) return false;
        if (isPcMode) return false;

        camera.rotate(Vector3.Y, -deltaX * rotateSpeed);
        tmp.set(camera.direction).crs(camera.up).nor();
        camera.rotate(tmp, -deltaY * rotateSpeed);
        camera.update();
        return true;
    }

    @Override public boolean touchDown(float x, float y, int pointer, int button) { return enabled; }
    @Override public boolean panStop(float x, float y, int pointer, int button) { return false; }
    @Override public boolean pinch(Vector2 p1, Vector2 p2, Vector2 p3, Vector2 p4) { return false; }
    @Override public void pinchStop() {}
    @Override public boolean longPress(float x, float y) { return false; }
    @Override public boolean fling(float vX, float vY, int b) { return false; }
    @Override public boolean zoom(float initialDistance, float distance) { return false; }
    @Override public boolean tap(float x, float y, int count, int button) { return false; }
}
