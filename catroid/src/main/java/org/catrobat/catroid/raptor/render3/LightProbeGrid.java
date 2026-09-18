package org.catrobat.catroid.raptor.render3;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback;
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld;

public class LightProbeGrid {

    public static final int GRID_X = 8;
    public static final int GRID_Y = 4;
    public static final int GRID_Z = 8;
    public static final int TOTAL_PROBES = GRID_X * GRID_Y * GRID_Z;

    public final Vector3 gridMin = new Vector3(-35f, -2f, -35f);
    public final Vector3 gridMax = new Vector3(35f, 18f, 35f);
    public final Vector3 cellSize = new Vector3();

    public final float[] shData = new float[TOTAL_PROBES * 12];

    private int updateCursor = 0;
    private static final int PROBES_PER_FRAME = 16;

    private final Vector3 tmpProbePos = new Vector3();
    private final Vector3 tmpRayEnd = new Vector3();
    private final Vector3 tmpHitPoint = new Vector3();

    public LightProbeGrid() {
        recalculateCellSize();
        resetToDefaultAmbient();
    }

    public void recalculateCellSize() {
        cellSize.set(
                (gridMax.x - gridMin.x) / Math.max(1, GRID_X - 1),
                (gridMax.y - gridMin.y) / Math.max(1, GRID_Y - 1),
                (gridMax.z - gridMin.z) / Math.max(1, GRID_Z - 1)
        );
    }

    public void updateGridCenter(Vector3 cameraPosition) {
        float halfX = (gridMax.x - gridMin.x) * 0.5f;
        float halfZ = (gridMax.z - gridMin.z) * 0.5f;

        gridMin.x = cameraPosition.x - halfX;
        gridMax.x = cameraPosition.x + halfX;

        gridMin.z = cameraPosition.z - halfZ;
        gridMax.z = cameraPosition.z + halfZ;

        recalculateCellSize();
    }

    public void resetToDefaultAmbient() {
        Color sky = new Color(0.25f, 0.45f, 0.85f, 1.0f);
        Color ground = new Color(0.12f, 0.08f, 0.05f, 1.0f);

        for (int i = 0; i < TOTAL_PROBES; i++) {
            int offset = i * 12;

            shData[offset + 0] = (sky.r + ground.r) * 0.5f;
            shData[offset + 1] = (sky.g + ground.g) * 0.5f;
            shData[offset + 2] = (sky.b + ground.b) * 0.5f;

            shData[offset + 3] = (sky.r - ground.r) * 0.4f;
            shData[offset + 4] = (sky.g - ground.g) * 0.4f;
            shData[offset + 5] = (sky.b - ground.b) * 0.4f;

            shData[offset + 6] = 0f; shData[offset + 7] = 0f; shData[offset + 8] = 0f;
            shData[offset + 9] = 0f; shData[offset + 10] = 0f; shData[offset + 11] = 0f;
        }
    }

    public void updateBounces(btDiscreteDynamicsWorld dynamicsWorld, Vector3 sunDir, Color sunColor, float sunIntensity) {
        if (dynamicsWorld == null) return;

        final Vector3[] directions = {
                new Vector3(0, -1, 0),
                new Vector3(0, 1, 0),
                new Vector3(1, 0, 0),
                new Vector3(-1, 0, 0),
                new Vector3(0, 0, 1),
                new Vector3(0, 0, -1)
        };

        for (int step = 0; step < PROBES_PER_FRAME; step++) {
            int pIndex = (updateCursor + step) % TOTAL_PROBES;

            int gz = pIndex / (GRID_X * GRID_Y);
            int gy = (pIndex % (GRID_X * GRID_Y)) / GRID_X;
            int gx = pIndex % GRID_X;

            tmpProbePos.set(
                    gridMin.x + gx * cellSize.x,
                    gridMin.y + gy * cellSize.y,
                    gridMin.z + gz * cellSize.z
            );

            int offset = pIndex * 12;

            tmpRayEnd.set(tmpProbePos).add(0, -cellSize.y * 2.0f, 0);
            ClosestRayResultCallback callback = new ClosestRayResultCallback(tmpProbePos, tmpRayEnd);
            dynamicsWorld.rayTest(tmpProbePos, tmpRayEnd, callback);

            if (callback.hasHit()) {
                callback.getHitPointWorld(tmpHitPoint);
                float dist = tmpProbePos.dst(tmpHitPoint);

                float nDotL = Math.max(0f, -sunDir.y);
                float bounceFactor = (1.0f / (1.0f + dist * 0.5f)) * nDotL * (sunIntensity * 0.08f);

                shData[offset + 3] += (sunColor.r * bounceFactor - shData[offset + 3]) * 0.1f;
                shData[offset + 4] += (sunColor.g * bounceFactor - shData[offset + 4]) * 0.1f;
                shData[offset + 5] += (sunColor.b * bounceFactor - shData[offset + 5]) * 0.1f;
            }
            callback.dispose();
        }

        updateCursor = (updateCursor + PROBES_PER_FRAME) % TOTAL_PROBES;
    }
}
