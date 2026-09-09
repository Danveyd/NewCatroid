package org.catrobat.catroid.raptor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

public class SoundWrapper implements PlayableAudio {
    private final Sound sound;
    private long instanceId = -1;
    private String instanceName;
    private float baseVolume;
    private final float pitch;
    private final boolean loop;
    private boolean is3D = true;

    private final Vector3 position = new Vector3();
    private String attachedToObjectId;
    private float maxDistance = 250f;

    private float lastCalculatedVolume = -1f;
    private float lastCalculatedPan = -999f;

    private boolean isLogicallyPlaying = false;
    private float duration = 5.0f;
    private float elapsedTime = 0.0f;

    public SoundWrapper(Sound sound, float volume, float pitch, boolean loop) {
        this.sound = sound;
        this.baseVolume = volume;
        this.pitch = pitch;
        this.loop = loop;
    }

    public SoundWrapper(Sound sound, float volume, float pitch, boolean loop, float duration) {
        this(sound, volume, pitch, loop);
        this.duration = Math.max(0.1f, duration);
    }

    @Override
    public void play() {
        this.isLogicallyPlaying = true;
        this.elapsedTime = 0.0f;
    }

    private void startPhysicalStream() {
        if (instanceId == -1 && isLogicallyPlaying) {
            if (loop) {
                instanceId = sound.loop(baseVolume, pitch, 0);
            } else {
                instanceId = sound.play(baseVolume, pitch, 0);
            }
            lastCalculatedVolume = -1f;
            lastCalculatedPan = -999f;
        }
    }

    private void stopPhysicalStream() {
        if (instanceId != -1) {
            sound.stop(instanceId);
            instanceId = -1;
            lastCalculatedVolume = -1f;
            lastCalculatedPan = -999f;
        }
    }

    @Override
    public void stop() {
        isLogicallyPlaying = false;
        stopPhysicalStream();
    }

    @Override
    public void dispose() {
        stop();
    }

    @Override
    public boolean isPlaying() {
        if (!loop && isLogicallyPlaying) {
            if (elapsedTime >= duration) {
                stop();
                return false;
            }
        }
        return isLogicallyPlaying;
    }

    @Override public void setInstanceName(String name) { this.instanceName = name; }
    @Override public String getInstanceName() { return instanceName; }

    @Override public void setPosition(Vector3 pos) { this.position.set(pos); }
    @Override public Vector3 getPosition() { return position; }

    @Override public void setAttachedObjectId(String id) { this.attachedToObjectId = id; }
    @Override public String getAttachedObjectId() { return attachedToObjectId; }

    @Override public void set3D(boolean is3D) { this.is3D = is3D; }
    @Override public boolean is3D() { return is3D; }

    @Override
    public void update3D(float volume, float pan) {
        if (!isLogicallyPlaying) return;

        if (!loop) {
            elapsedTime += Gdx.graphics.getDeltaTime();
            if (elapsedTime >= duration) {
                stop();
                return;
            }
        }

        float targetVolume;
        float targetPan;

        if (!is3D) {
            targetVolume = MathUtils.clamp(baseVolume * volume, 0f, 1f);
            targetPan = 0f;
        } else {
            targetVolume = MathUtils.clamp(volume * baseVolume, 0f, 1f);
            targetPan = MathUtils.clamp(pan, -1f, 1f);
        }

        if (targetVolume <= 0.001f) {
            stopPhysicalStream();
            return;
        }

        if (instanceId == -1) {
            startPhysicalStream();
            if (instanceId == -1) return;
        }

        if (Math.abs(targetVolume - lastCalculatedVolume) > 0.005f ||
                Math.abs(targetPan - lastCalculatedPan) > 0.005f) {

            this.lastCalculatedVolume = targetVolume;
            this.lastCalculatedPan = targetPan;

            sound.setPan(instanceId, targetPan, targetVolume);
        }
    }

    @Override public void setVolume(float volume) { this.baseVolume = volume; this.lastCalculatedVolume = -1f; }
    @Override public void setPitch(float pitch) { if (instanceId != -1) sound.setPitch(instanceId, pitch); }
    @Override public void setBaseVolume(float volume) { this.baseVolume = volume; this.lastCalculatedVolume = -1f; }
    @Override public float getBaseVolume() { return baseVolume; }
    @Override public void setMaxDistance(float maxDistance) { this.maxDistance = maxDistance; }
    @Override public float getMaxDistance() { return this.maxDistance; }
}
