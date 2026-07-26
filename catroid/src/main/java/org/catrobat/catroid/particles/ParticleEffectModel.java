package org.catrobat.catroid.particles;

import java.io.Serializable;

public class ParticleEffectModel implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum EmitterMode {
        GRAVITY,
        RADIUS
    }

    public String effectId = "default_particle";
    public EmitterMode mode = EmitterMode.GRAVITY;

    public int maxParticles = 300;
    public float duration = -1.0f;
    public float lifetime = 2.0f;
    public float lifetimeVariance = 0.5f;
    public float emissionRate = 50.0f;
    public float respawnDelay = 0.0f;
    public float fadeInTime = 0.1f;
    public float fadeOutTime = 0.3f;

    public float posVarX = 10.0f;
    public float posVarY = 10.0f;

    public float speed = 150.0f;
    public float speedVariance = 30.0f;
    public float angle = 90.0f;
    public float angleVariance = 15.0f;
    public float gravityX = 0.0f;
    public float gravityY = -200.0f;
    public float radialAccel = 0.0f;
    public float radialAccelVariance = 0.0f;
    public float tangentialAccel = 0.0f;
    public float tangentialAccelVariance = 0.0f;
    public float friction = 0.0f;

    public float startRadius = 100.0f;
    public float startRadiusVariance = 10.0f;
    public float endRadius = 0.0f;
    public float endRadiusVariance = 0.0f;
    public float rotatePerSecond = 180.0f;
    public float rotatePerSecondVariance = 30.0f;

    public ParticleCurve sizeCurve = new ParticleCurve(32.0f, 0.0f, 5.0f, org.catrobat.catroid.content.EasingFunctions.EasingType.QUAD_OUT);
    public ParticleCurve spinCurve = new ParticleCurve(0.0f, 360.0f, 45.0f, org.catrobat.catroid.content.EasingFunctions.EasingType.LINEAR);

    public float startR = 1.0f, startG = 0.5f, startB = 0.1f, startA = 1.0f;
    public float startColorVariance = 0.1f;
    public float endR = 1.0f, endG = 0.0f, endB = 0.0f, endA = 0.0f;
    public float endColorVariance = 0.0f;
    public ParticleCurve alphaCurve = new ParticleCurve(1.0f, 0.0f, 0.0f, org.catrobat.catroid.content.EasingFunctions.EasingType.QUAD_IN);

    public String textureFileName = "";
    public boolean isAdditive = true;
    public String targetBufferName = "";

    public ParticleEffectModel() {}
}
