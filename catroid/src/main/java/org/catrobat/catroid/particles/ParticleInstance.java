package org.catrobat.catroid.particles;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

public class ParticleInstance {

    public static class SingleParticle {
        public boolean active = false;
        public float time = 0f;
        public float lifeTime = 1f;

        public final Vector2 position = new Vector2();
        public final Vector2 velocity = new Vector2();

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

        public final Color startColor = new Color();
        public final Color endColor = new Color();
        public final Color currentColor = new Color();

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

    public float particleScaleX = 1f;
    public float particleScaleY = 1f;

    public boolean visible = true;

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

    private static final Vector2 tmpRadial = new Vector2();
    private static final Vector2 tmpTangential = new Vector2();
    private final float[] vertices = new float[20];

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

        if (!isStopped) {
            boolean shouldEmit = false;

            if (model.duration < 0) {
                shouldEmit = true;
            } else if (model.respawnDelay > 0) {
                float totalCycleTime = model.duration + model.respawnDelay;
                if (totalCycleTime > 0) {
                    float currentCycleTime = systemTime % totalCycleTime;
                    if (currentCycleTime <= model.duration) {
                        shouldEmit = true;
                    }
                }
            } else {
                if (systemTime <= model.duration) {
                    shouldEmit = true;
                }
            }

            if (shouldEmit) {
                float rate = 1.0f / Math.max(0.001f, model.emissionRate);
                emissionTimer += delta;
                while (emissionTimer >= rate && activeCount < particles.length) {
                    spawnParticle();
                    emissionTimer -= rate;
                }
            } else {
                emissionTimer = 0f;
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
                float len = p.position.len();
                if (len > 0.0001f) {
                    tmpRadial.set(p.position.x / len, p.position.y / len);
                } else {
                    tmpRadial.set(0f, 0f);
                }

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

                p.position.x = MathUtils.cosDeg(p.angle) * p.radius;
                p.position.y = MathUtils.sinDeg(p.angle) * p.radius;
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
                    randomVariance(model.posVarX),
                    randomVariance(model.posVarY)
            );

            if (model.mode == ParticleEffectModel.EmitterMode.GRAVITY) {
                float a = model.angle + randomVariance(model.angleVariance);
                float spd = model.speed + randomVariance(model.speedVariance);

                p.velocity.set(
                        MathUtils.cosDeg(a) * spd,
                        MathUtils.sinDeg(a) * spd
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

                p.position.x = MathUtils.cosDeg(p.angle) * p.radius;
                p.position.y = MathUtils.sinDeg(p.angle) * p.radius;
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
        if (particleTextureRegion == null || activeCount == 0 || !visible) return;

        int oldSrcFunc = batch.getBlendSrcFunc();
        int oldDstFunc = batch.getBlendDstFunc();

        if (model.isAdditive) {
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        } else {
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }

        float sysCos = MathUtils.cosDeg(rotation);
        float sysSin = MathUtils.sinDeg(rotation);

        float u = particleTextureRegion.getU();
        float v = particleTextureRegion.getV2();
        float u2 = particleTextureRegion.getU2();
        float v2 = particleTextureRegion.getV();

        Texture texture = particleTextureRegion.getTexture();

        for (int i = 0; i < particles.length; i++) {
            SingleParticle p = particles[i];
            if (!p.active || p.currentColor.a <= 0.001f) continue;

            float colorBits = p.currentColor.toFloatBits();

            float scaledLocalX = p.position.x * scaleX;
            float scaledLocalY = p.position.y * scaleY;

            float rotatedX = scaledLocalX * sysCos - scaledLocalY * sysSin;
            float rotatedY = scaledLocalX * sysSin + scaledLocalY * sysCos;

            float worldX = x + rotatedX;
            float worldY = y + rotatedY;

            float hw = (p.currentSize * particleScaleX) / 2f;
            float hh = (p.currentSize * particleScaleY) / 2f;

            float spinCos = MathUtils.cosDeg(p.currentRotation);
            float spinSin = MathUtils.sinDeg(p.currentRotation);

            float xA0 = -hw * spinCos + hh * spinSin;
            float yA0 = -hw * spinSin - hh * spinCos;
            float xB0 = xA0 * scaleX;
            float yB0 = yA0 * scaleY;
            float rx0 = worldX + (xB0 * sysCos - yB0 * sysSin);
            float ry0 = worldY + (xB0 * sysSin + yB0 * sysCos);

            float xA1 = -hw * spinCos - hh * spinSin;
            float yA1 = -hw * spinSin + hh * spinCos;
            float xB1 = xA1 * scaleX;
            float yB1 = yA1 * scaleY;
            float rx1 = worldX + (xB1 * sysCos - yB1 * sysSin);
            float ry1 = worldY + (xB1 * sysSin + yB1 * sysCos);

            float rx2 = 2f * worldX - rx0;
            float ry2 = 2f * worldY - ry0;

            float rx3 = 2f * worldX - rx1;
            float ry3 = 2f * worldY - ry1;

            vertices[0] = rx0;  vertices[1] = ry0;  vertices[2] = colorBits; vertices[3] = u;   vertices[4] = v;
            vertices[5] = rx1;  vertices[6] = ry1;  vertices[7] = colorBits; vertices[8] = u;   vertices[9] = v2;
            vertices[10] = rx2; vertices[11] = ry2; vertices[12] = colorBits; vertices[13] = u2; vertices[14] = v2;
            vertices[15] = rx3; vertices[16] = ry3; vertices[17] = colorBits; vertices[18] = u2; vertices[19] = v;

            batch.draw(texture, vertices, 0, 20);
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
        return MathUtils.random(-variance, variance);
    }

    private float clamp(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}
