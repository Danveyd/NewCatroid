package org.catrobat.catroid.raptor;

public class CameraComponent implements Component {

    public float fieldOfView = 67;
    public float nearPlane = 0.1f;
    public float farPlane = 300f;

    public boolean isMainCamera = false;

    public CameraComponent() {}
}