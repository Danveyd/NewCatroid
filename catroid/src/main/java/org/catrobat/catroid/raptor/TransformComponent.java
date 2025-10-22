package org.catrobat.catroid.raptor;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;

public class TransformComponent implements Component {
    public Vector3 position = new Vector3();
    public Quaternion rotation = new Quaternion();
    public Vector3 scale = new Vector3(1, 1, 1);

    public TransformComponent() {}

    public Matrix4 toMatrix() {
        return new Matrix4().set(position, rotation, scale);
    }
}