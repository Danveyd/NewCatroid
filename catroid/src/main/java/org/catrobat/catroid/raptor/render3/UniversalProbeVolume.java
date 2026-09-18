package org.catrobat.catroid.raptor.render3;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute;
import net.mgsx.gltf.scene3d.scene.SceneManager;

import java.util.Map;

public class UniversalProbeVolume {

    private final Ray testRay = new Ray();
    private final Color hitColor = new Color();
    private final Color bounceAmbient = new Color();

    public void updateBake(Map<String, ModelInstance> sceneObjects,
                           Map<String, BoundingBox> rawBounds,
                           SceneManager sceneManager,
                           Color skyColor, Vector3 sunDir, Color sunColor, float sunIntensity) {

        if (sceneObjects.isEmpty() || sceneManager == null) return;

        ModelInstance floorInst = null;
        float lowestY = Float.MAX_VALUE;

        for (Map.Entry<String, ModelInstance> entry : sceneObjects.entrySet()) {
            BoundingBox box = rawBounds.get(entry.getKey());
            if (box == null) continue;
            if (box.min.y < lowestY) {
                lowestY = box.min.y;
                floorInst = entry.getValue();
            }
        }

        Color groundColor = new Color(0.1f, 0.1f, 0.1f, 1f);
        if (floorInst != null) {
            extractColor(floorInst, hitColor);
            float bounceStrength = Math.max(0.15f, -sunDir.y) * Math.max(1.0f, sunIntensity);
            groundColor.set(hitColor).mul(bounceStrength * 0.4f);
        }

        bounceAmbient.set(
                Math.max(0.2f, skyColor.r * 0.4f + groundColor.r),
                Math.max(0.2f, skyColor.g * 0.4f + groundColor.g),
                Math.max(0.2f, skyColor.b * 0.4f + groundColor.b),
                1.0f
        );

        sceneManager.environment.set(ColorAttribute.createAmbient(bounceAmbient));
    }

    private void extractColor(ModelInstance inst, Color out) {
        if (inst != null && inst.materials.size > 0) {
            Material m = inst.materials.get(0);
            if (m.has(PBRColorAttribute.BaseColorFactor)) {
                out.set(((PBRColorAttribute) m.get(PBRColorAttribute.BaseColorFactor)).color);
                return;
            }
            if (m.has(ColorAttribute.Diffuse)) {
                out.set(((ColorAttribute) m.get(ColorAttribute.Diffuse)).color);
                return;
            }
        }
        out.set(Color.WHITE);
    }
}
