package org.catrobat.catroid.raptor;

import com.badlogic.gdx.math.Vector3;

public class ColliderShapeData {
    public enum ShapeType { BOX, SPHERE, CAPSULE }
    public ShapeType type = ShapeType.BOX;
    public Vector3 centerOffset = new Vector3();
    public Vector3 size = new Vector3(1, 1, 1);
    public float radius = 0.5f;
}
