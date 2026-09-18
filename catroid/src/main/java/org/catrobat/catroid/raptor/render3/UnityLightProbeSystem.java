package org.catrobat.catroid.raptor.render3;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback;
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld;
import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute;

import java.util.Map;

public class UnityLightProbeSystem {

    private final Vector3 probePos = new Vector3();
    private final Vector3 rayDown = new Vector3();
    private final Vector3 hitPoint = new Vector3();
    private final BoundingBox tmpBounds = new BoundingBox();

    private final Color colorDown = new Color();
    private final Color colorUp = new Color();
    private final Color finalAmbient = new Color();

    public void updateProbesForObjects(Map<String, ModelInstance> sceneObjects,
                                       Map<String, BoundingBox> cachedBounds,
                                       btDiscreteDynamicsWorld dynamicsWorld,
                                       Color skyColor,
                                       Vector3 sunDir,
                                       Color sunColor,
                                       float sunIntensity) {

        for (Map.Entry<String, ModelInstance> entry : sceneObjects.entrySet()) {
            ModelInstance inst = entry.getValue();

            inst.transform.getTranslation(probePos);

            colorUp.set(skyColor).mul(0.4f).add(sunColor.r * 0.1f, sunColor.g * 0.1f, sunColor.b * 0.1f, 1f);

            colorDown.set(0.05f, 0.05f, 0.05f, 1f);
            boolean hitFound = false;
            float distToFloor = 10.0f;
            Color floorColor = null;

            if (dynamicsWorld != null) {
                rayDown.set(probePos).add(0, -15.0f, 0);
                ClosestRayResultCallback callback = new ClosestRayResultCallback(probePos, rayDown);
                dynamicsWorld.rayTest(probePos, rayDown, callback);

                if (callback.hasHit()) {
                    callback.getHitPointWorld(hitPoint);
                    distToFloor = probePos.dst(hitPoint);
                    hitFound = true;
                }
                callback.dispose();
            }

            if (!hitFound) {
                float closestY = -9999f;
                for (Map.Entry<String, BoundingBox> other : cachedBounds.entrySet()) {
                    if (other.getKey().equals(entry.getKey())) continue;

                    BoundingBox box = other.getValue();
                    if (probePos.x >= box.min.x && probePos.x <= box.max.x &&
                            probePos.z >= box.min.z && probePos.z <= box.max.z) {

                        if (box.max.y <= probePos.y && box.max.y > closestY) {
                            closestY = box.max.y;
                            distToFloor = probePos.y - closestY;
                            floorColor = extractMaterialColor(sceneObjects.get(other.getKey()));
                            hitFound = true;
                        }
                    }
                }
            }

            if (hitFound) {
                float bounceStrength = (1.0f / (1.0f + distToFloor * 0.3f)) * Math.max(0.1f, -sunDir.y) * (sunIntensity * 0.8f);
                if (floorColor != null) {
                    colorDown.set(floorColor).mul(bounceStrength);
                } else {
                    colorDown.set(0.1f, 0.85f, 0.15f, 1.0f).mul(bounceStrength); // Зеленый по умолчанию
                }
            }

            finalAmbient.set(
                    (colorUp.r + colorDown.r) * 0.5f,
                    (colorUp.g + colorDown.g) * 0.5f,
                    (colorUp.b + colorDown.b) * 0.5f,
                    1.0f
            );

            for (Material mat : inst.materials) {
                ColorAttribute attr = (ColorAttribute) mat.get(ColorAttribute.AmbientLight);
                if (attr == null) {
                    mat.set(ColorAttribute.createAmbient(finalAmbient));
                } else {
                    attr.color.set(finalAmbient);
                }
            }
        }
    }

    private Color extractMaterialColor(ModelInstance instance) {
        if (instance != null && instance.materials.size > 0) {
            Material mat = instance.materials.get(0);
            if (mat.has(PBRColorAttribute.BaseColorFactor)) return ((PBRColorAttribute) mat.get(PBRColorAttribute.BaseColorFactor)).color;
            if (mat.has(ColorAttribute.Diffuse)) return ((ColorAttribute) mat.get(ColorAttribute.Diffuse)).color;
        }
        return null;
    }
}
