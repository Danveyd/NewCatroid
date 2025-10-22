package org.catrobat.catroid.raptor;

public class AnimationComponent implements Component {
    public String animationName = null;
    public int loops = -1;
    public float speed = 1;
    public float transitionTime = 0.2f;

    public AnimationComponent() {}
}