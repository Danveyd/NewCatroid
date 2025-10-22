package org.catrobat.catroid.io;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.util.Log;
import java.util.HashMap;
import java.util.Map;

public class SoundCacheManager {
    private static final String TAG = "SoundCacheManager";
    private static final SoundCacheManager INSTANCE = new SoundCacheManager();

    private SoundPool soundPool;
    private Map<String, Integer> soundIdMap = new HashMap<>();
    private boolean isInitialized = false;

    private SoundCacheManager() {}

    public static SoundCacheManager getInstance() {
        return INSTANCE;
    }

    public void initialize() {
        if (isInitialized) return;
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(16) // Можно проигрывать до 16 звуков одновременно
                .setAudioAttributes(audioAttributes)
                .build();
        isInitialized = true;
        Log.i(TAG, "SoundPool initialized.");
    }

    public void loadSound(String cacheName, String filePath) {
        if (!isInitialized) initialize();
        if (soundIdMap.containsKey(cacheName)) {
            soundPool.unload(soundIdMap.get(cacheName));
        }
        int soundId = soundPool.load(filePath, 1);
        soundIdMap.put(cacheName, soundId);
        Log.i(TAG, "Sound loaded into cache: '" + cacheName + "' from " + filePath);
    }

    public void playSound(String cacheName) {
        if (!isInitialized) {
            Log.e(TAG, "SoundCacheManager not initialized. Cannot play sound.");
            return;
        }
        Integer soundId = soundIdMap.get(cacheName);
        if (soundId != null) {
            // play(soundID, leftVolume, rightVolume, priority, loop, rate)
            soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f);
        } else {
            Log.e(TAG, "Sound not found in cache: " + cacheName);
        }
    }

    public void release() {
        if (isInitialized) {
            soundPool.release();
            soundPool = null;
            soundIdMap.clear();
            isInitialized = false;
            Log.i(TAG, "SoundPool released.");
        }
    }
}