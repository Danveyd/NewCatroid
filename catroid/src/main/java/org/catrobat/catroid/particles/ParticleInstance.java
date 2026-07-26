package org.catrobat.catroid.particles;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.List;

public class ParticleInstance {

    public static class SingleParticle {
        public boolean active = false;
        public float time = 0f;
        public float lifeTime = 1f;

        public Vector2 position = new Vector2();
        public Vector2 velocity = new Vector2();

        public float radialAccel = 0f;
        public float tangentialAccel = 0f;

        public float radius = 0f;
        public float deltaRadius = 0f;
        public float angle = 0f;
        public float degreesPerSecond = 0f;

        public float startSizeVar = 0f;
        public float endSizeVar = 0f;
        public float startSpinVar = 0f;
        public float endSpinVar = 0f;

        public Color startColor = new Color();
        public Color endColor = new Color();
        public Color currentColor = new Color();
        public float currentSize = 10f;
        public float currentRotation = 0f;
    }

    public String instanceId;
    public ParticleEffectModel model;

    public float x = 0f;
    public float y = 0f;
    public float scaleX = 1f;
    public float scaleY = 1f;
    public float rotation = 0f;

    private SingleParticle[] particles;
    private int activeCount = 0;
    private float emissionTimer = 0f;
    private float systemTime = 0f;
    private boolean isStopped = false;

    private TextureRegion particleTextureRegion;

    public enum BufferRenderMode {
        SCREEN_AND_BUFFER,
        BUFFER_ONLY,
        SCREEN_ONLY
    }

    public BufferRenderMode bufferMode = BufferRenderMode.SCREEN_AND_BUFFER;
    public String targetBufferName = "";

    public ParticleInstance(String instanceId, ParticleEffectModel model, TextureRegion textureRegion) {
        this.instanceId = instanceId;
        this.model = model;
        this.particleTextureRegion = textureRegion;
        initParticles();
    }

    public void setTextureRegion(TextureRegion region) {
        this.particleTextureRegion = region;
    }

    private void initParticles() {
        int max = Math.max(1, model.maxParticles);
        particles = new SingleParticle[max];
        for (int i = 0; i < max; i++) {
            particles[i] = new SingleParticle();
        }
    }

    public void reset() {
        this.systemTime = 0f;
        this.emissionTimer = 0f;
        this.isStopped = false;
        this.activeCount = 0;
        if (particles != null) {
            for (SingleParticle p : particles) {
                p.active = false;
            }
        }
    }

    public void update(float delta) {
        systemTime += delta;

        if (!isStopped && (model.duration < 0 || systemTime <= model.duration)) {
            float rate = 1.0f / Math.max(0.001f, model.emissionRate);
            emissionTimer += delta;
            while (emissionTimer >= rate && activeCount < particles.length) {
                spawnParticle();
                emissionTimer -= rate;
            }
        }

        for (int i = 0; i < particles.length; i++) {
            SingleParticle p = particles[i];
            if (!p.active) continue;

            p.time += delta;
            if (p.time >= p.lifeTime) {
                p.active = false;
                activeCount--;
                continue;
            }

            float normalizedLife = p.time / p.lifeTime;

            if (model.mode == ParticleEffectModel.EmitterMode.GRAVITY) {
                Vector2 tmpRadial = new Vector2();
                Vector2 tmpTangential = new Vector2();

                Vector2 posOffset = new Vector2(p.position).sub(x, y);
                if (posOffset.len() > 0) tmpRadial.set(posOffset).nor();
                tmpTangential.set(-tmpRadial.y, tmpRadial.x);

                tmpRadial.scl(p.radialAccel);
                tmpTangential.scl(p.tangentialAccel);

                p.velocity.x += (model.gravityX + tmpRadial.x + tmpTangential.x) * delta;
                p.velocity.y += (model.gravityY + tmpRadial.y + tmpTangential.y) * delta;

                if (model.friction > 0f) {
                    p.velocity.scl(1.0f - Math.min(1.0f, model.friction * delta));
                }

                p.position.x += p.velocity.x * delta;
                p.position.y += p.velocity.y * delta;
            } else {
                p.angle += p.degreesPerSecond * delta;
                p.radius += p.deltaRadius * delta;

                p.position.x = x + (float) Math.cos(Math.toRadians(p.angle)) * p.radius;
                p.position.y = y + (float) Math.sin(Math.toRadians(p.angle)) * p.radius;
            }

            p.currentSize = model.sizeCurve.evaluate(normalizedLife, p.startSizeVar, p.endSizeVar);
            p.currentRotation = model.spinCurve.evaluate(normalizedLife, p.startSpinVar, p.endSpinVar);

            p.currentColor.r = MathUtils.lerp(p.startColor.r, p.endColor.r, normalizedLife);
            p.currentColor.g = MathUtils.lerp(p.startColor.g, p.endColor.g, normalizedLife);
            p.currentColor.b = MathUtils.lerp(p.startColor.b, p.endColor.b, normalizedLife);

            float baseAlpha = MathUtils.lerp(p.startColor.a, p.endColor.a, normalizedLife);
            float curveAlpha = model.alphaCurve.evaluate(normalizedLife, 0f, 0f);
            p.currentColor.a = Math.max(0f, Math.min(1f, baseAlpha * curveAlpha));
        }
    }

    private void spawnParticle() {
        for (int i = 0; i < particles.length; i++) {
            SingleParticle p = particles[i];
            if (p.active) continue;

            p.active = true;
            activeCount++;
            p.time = 0f;
            p.lifeTime = Math.max(0.01f, model.lifetime + randomVariance(model.lifetimeVariance));

            p.position.set(
                    x + randomVariance(model.posVarX),
                    y + randomVariance(model.posVarY)
            );

            if (model.mode == ParticleEffectModel.EmitterMode.GRAVITY) {
                float a = model.angle + randomVariance(model.angleVariance);
                float spd = model.speed + randomVariance(model.speedVariance);

                p.velocity.set(
                        (float) Math.cos(Math.toRadians(a)) * spd,
                        (float) Math.sin(Math.toRadians(a)) * spd
                );

                p.radialAccel = model.radialAccel + randomVariance(model.radialAccelVariance);
                p.tangentialAccel = model.tangentialAccel + randomVariance(model.tangentialAccelVariance);
            } else {
                p.angle = model.angle + randomVariance(model.angleVariance);
                p.degreesPerSecond = model.rotatePerSecond + randomVariance(model.rotatePerSecondVariance);

                float sRad = model.startRadius + randomVariance(model.startRadiusVariance);
                float eRad = model.endRadius + randomVariance(model.endRadiusVariance);

                p.radius = sRad;
                p.deltaRadius = (eRad - sRad) / p.lifeTime;
            }

            p.startSizeVar = randomVariance(5f);
            p.endSizeVar = randomVariance(5f);
            p.startSpinVar = randomVariance(10f);
            p.endSpinVar = randomVariance(10f);

            p.startColor.set(
                    clamp(model.startR + randomVariance(model.startColorVariance)),
                    clamp(model.startG + randomVariance(model.startColorVariance)),
                    clamp(model.startB + randomVariance(model.startColorVariance)),
                    clamp(model.startA)
            );

            p.endColor.set(
                    clamp(model.endR + randomVariance(model.endColorVariance)),
                    clamp(model.endG + randomVariance(model.endColorVariance)),
                    clamp(model.endB + randomVariance(model.endColorVariance)),
                    clamp(model.endA)
            );

            break;
        }
    }

    public void draw(Batch batch) {
        if (particleTextureRegion == null || activeCount == 0) return;

        int oldSrcFunc = batch.getBlendSrcFunc();
        int oldDstFunc = batch.getBlendDstFunc();

        if (model.isAdditive) {
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        } else {
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }

        for (int i = 0; i < particles.length; i++) {
            SingleParticle p = particles[i];
            if (!p.active || p.currentColor.a <= 0.001f) continue;

            batch.setColor(p.currentColor);

            float drawWidth = p.currentSize * scaleX;
            float drawHeight = p.currentSize * scaleY;
            float originX = drawWidth / 2f;
            float originY = drawHeight / 2f;

            batch.draw(
                    particleTextureRegion,
                    p.position.x - originX,
                    p.position.y - originY,
                    originX, originY,
                    drawWidth, drawHeight,
                    1f, 1f,
                    p.currentRotation + rotation
            );
        }

        batch.setBlendFunction(oldSrcFunc, oldDstFunc);
        batch.setColor(Color.WHITE);
    }

    public void burst(int count) {
        for (int i = 0; i < count; i++) {
            spawnParticle();
        }
    }

    public void stop(boolean immediate) {
        isStopped = true;
        if (immediate) {
            for (SingleParticle p : particles) {
                p.active = false;
            }
            activeCount = 0;
        }
    }

    private float randomVariance(float variance) {
        if (variance == 0f) return 0f;
        return (float) ((Math.random() * 2.0 - 1.0) * variance);
    }

    private float clamp(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}
