package org.catrobat.catroid.raptor;

import org.catrobat.catroid.raptor.ThreeDManager.PhysicsShape;
import org.catrobat.catroid.raptor.ThreeDManager.PhysicsState;

import java.util.ArrayList;
import java.util.List;

public class PhysicsComponent implements Component {
    public PhysicsState state = PhysicsState.NONE;
    @Deprecated public PhysicsShape shape = PhysicsShape.BOX;
    public float mass = 1.0f;
    public float friction = 0.5f;
    public float restitution = 0.1f;

    public List<ColliderShapeData> colliders = new ArrayList<>();

    public PhysicsComponent() {}
}