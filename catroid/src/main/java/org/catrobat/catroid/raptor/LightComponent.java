package org.catrobat.catroid.raptor;


import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;

public class LightComponent implements Component {
    public enum LightType {
        DIRECTIONAL,
        POINT,
        SPOT
    }

    public LightType type = LightType.POINT;
    public Color color = new Color(Color.WHITE);
    public float intensity = 100f;
    public Vector3 direction = new Vector3(0, -1, 0);
    public float cutoffAngle = 30f;
    public float exponent = 1f;
    public float range = -1f;

    public LightComponent() {}
}