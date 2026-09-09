package org.catrobat.catroid.raptor;

public class AudioComponent implements Component {
    public String soundFileName = null;
    public float volume = 1.0f;
    public float pitch = 1.0f;
    public boolean loop = false;
    public boolean playOnAwake = true;
    public float startDelay = 0.0f;
    public boolean is3D = true;
    public float maxDistance = 30.0f;
    public boolean isMusic = false;

    public transient float delayTimer = 0.0f;
    public transient boolean hasStarted = false;
    public transient boolean isPreviewPlaying = false;

    public AudioComponent() {}
}
