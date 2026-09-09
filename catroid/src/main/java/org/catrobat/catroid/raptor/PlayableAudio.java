package org.catrobat.catroid.raptor;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;

public interface PlayableAudio extends Disposable {
    void play();
    void stop();
    boolean isPlaying();

    void setInstanceName(String name);
    String getInstanceName();

    void setPosition(Vector3 pos);
    Vector3 getPosition();

    void setAttachedObjectId(String id);
    String getAttachedObjectId();

    void set3D(boolean is3D);
    boolean is3D();

    void update3D(float volume, float pan);
    void setVolume(float volume);
    void setPitch(float pitch);
    void setBaseVolume(float volume);
    float getBaseVolume();
    void setMaxDistance(float maxDistance);
    float getMaxDistance();
}
