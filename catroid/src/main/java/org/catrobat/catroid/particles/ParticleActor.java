package org.catrobat.catroid.particles;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;

public class ParticleActor extends Actor {
    private final ParticleInstance particleInstance;

    public ParticleActor(ParticleInstance particleInstance) {
        this.particleInstance = particleInstance;
    }

    public ParticleInstance getParticleInstance() {
        return particleInstance;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (particleInstance == null) return;

        if (particleInstance.bufferMode == ParticleInstance.BufferRenderMode.BUFFER_ONLY
                && particleInstance.targetBufferName != null
                && !particleInstance.targetBufferName.trim().isEmpty()) {
            return;
        }

        particleInstance.draw(batch);
    }
}
