package org.catrobat.catroid.raptor;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;

public class MusicWrapper implements PlayableAudio {
    private final Music music;
    private String instanceName;
    private final boolean loop;
    private boolean is3D = true;

    private final Vector3 position = new Vector3();
    private String attachedToObjectId;
    private float baseVolume = 1.0f;
    private float maxDistance = 250f;

    private boolean isLogicallyPlaying = false;

    public MusicWrapper(Music music, float volume, float pitch, boolean loop) {
        this.music = music;
        this.loop = loop;
        this.baseVolume = volume;
        music.setLooping(loop);
    }

    @Override
    public void play() {
        this.isLogicallyPlaying = true;
    }

    @Override
    public void stop() {
        this.isLogicallyPlaying = false;
        music.stop();
    }

    @Override
    public void dispose() {
        music.stop();
        music.dispose();
    }

    @Override
    public boolean isPlaying() {
        if (!loop && isLogicallyPlaying) {
            return music.isPlaying();
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

        if (!is3D) {
            if (!music.isPlaying()) music.play();
            music.setVolume(baseVolume);
            music.setPan(0f, baseVolume);
        } else {
            float targetVol = MathUtils.clamp(volume * baseVolume, 0f, 1f);

            if (targetVol <= 0.001f) {
                if (music.isPlaying()) {
                    music.pause();
                }
                return;
            }

            if (!music.isPlaying()) {
                music.play();
            }

            float targetPan = MathUtils.clamp(pan, -1f, 1f);
            music.setVolume(targetVol);
            music.setPan(targetPan, targetVol);
        }
    }

    @Override public void setVolume(float volume) { this.baseVolume = volume; music.setVolume(volume); }
    @Override public void setPitch(float pitch) {}
    @Override public void setBaseVolume(float volume) { this.baseVolume = volume; music.setVolume(volume); }
    @Override public float getBaseVolume() { return baseVolume; }
    @Override public void setMaxDistance(float maxDistance) { this.maxDistance = maxDistance; }
    @Override public float getMaxDistance() { return this.maxDistance; }
}
