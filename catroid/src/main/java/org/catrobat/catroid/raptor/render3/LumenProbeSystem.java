package org.catrobat.catroid.raptor.render3;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.math.Vector3;
import net.mgsx.gltf.scene3d.scene.SceneManager;

import java.util.Map;

public class LumenProbeSystem {

    private final LightProbeGrid grid = new LightProbeGrid();
    private final Vector3 tmpPos = new Vector3();
    private final Color probeColor = new Color();

    public void update(SceneManager sceneManager,
                       Map<String, ModelInstance> sceneObjects,
                       Vector3 sunDir, Color sunColor, float sunIntensity, Color skyColor) {

        grid.recalculateCellSize();

        for (ModelInstance inst : sceneObjects.values()) {
            inst.transform.getTranslation(tmpPos);

            float heightAboveGround = Math.max(0f, tmpPos.y);
            float groundBounce = Math.max(0f, 1.0f - (heightAboveGround / 6.0f));

            probeColor.set(
                    skyColor.r * 0.3f + sunColor.r * 0.2f * groundBounce,
                    skyColor.g * 0.3f + sunColor.g * 0.5f * groundBounce,
                    skyColor.b * 0.3f,
                    1.0f
            );

            for (Material mat : inst.materials) {
                ColorAttribute attr = (ColorAttribute) mat.get(ColorAttribute.AmbientLight);
                if (attr == null) {
                    mat.set(ColorAttribute.createAmbient(probeColor));
                } else {
                    attr.color.set(probeColor);
                }
            }
        }
    }
}
