package org.catrobat.catroid.raptor.culling;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;

public class Render3Culling {

    private static final Vector3 tmpCenter = new Vector3();
    private static final Vector3 tmpDimensions = new Vector3();
    private static final BoundingBox tmpBounds = new BoundingBox();

    public static boolean shouldRender(Camera camera, BoundingBox localBounds, Matrix4 worldTransform, float minScreenPixels) {
        if (localBounds == null) return true;

        tmpBounds.set(localBounds).mul(worldTransform);
        tmpBounds.getCenter(tmpCenter);
        tmpBounds.getDimensions(tmpDimensions);

        float radius = Math.max(tmpDimensions.x, Math.max(tmpDimensions.y, tmpDimensions.z)) * 0.5f;

        if (!camera.frustum.sphereInFrustum(tmpCenter, radius)) {
            return false;
        }

        if (!camera.frustum.boundsInFrustum(tmpBounds)) {
            return false;
        }

        float dist = camera.position.dst(tmpCenter);
        if (dist > 0.001f) {
            float fovFactor = getFovFactor(camera);
            float screenDiameterPixels = (radius * 2.0f / (dist * fovFactor)) * camera.viewportHeight;
            if (screenDiameterPixels < minScreenPixels) {
                return false;
            }
        }

        return true;
    }

    public static float calculateScreenCoverage(Camera camera, Vector3 worldPosition, float radius) {
        float dist = camera.position.dst(worldPosition);
        if (dist <= 0.001f) return 1.0f;
        float fovFactor = getFovFactor(camera);
        return (radius * 2.0f) / (dist * fovFactor);
    }

    private static float getFovFactor(Camera camera) {
        if (camera instanceof PerspectiveCamera) {
            return 2.0f * (float) Math.tan(Math.toRadians(((PerspectiveCamera) camera).fieldOfView * 0.5f));
        }
        float m11 = camera.projection.val[Matrix4.M11];
        return (m11 != 0.0f) ? (2.0f / m11) : 1.0f;
    }
}
