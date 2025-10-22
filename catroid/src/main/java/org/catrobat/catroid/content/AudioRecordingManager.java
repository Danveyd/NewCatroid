package org.catrobat.catroid.content;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;

import org.catrobat.catroid.io.StorageOperations;

import java.io.File;
import java.io.IOException;

public class AudioRecordingManager {
    private static final String TAG = "AudioRecordingManager";
    private static final AudioRecordingManager INSTANCE = new AudioRecordingManager();

    private MediaRecorder recorder;
    private File tempAudioFile;

    private AudioRecordingManager() {}

    public static AudioRecordingManager getInstance() {
        return INSTANCE;
    }

    public boolean startRecording(Context context) {
        if (recorder != null) {
            Log.w(TAG, "Recording is already in progress. Please stop the current one first.");
            return false;
        }

        try {
            tempAudioFile = File.createTempFile("recording_temp", ".m4a", context.getCacheDir());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                recorder = new MediaRecorder(context);
            } else {
                recorder = new MediaRecorder();
            }

            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setOutputFile(tempAudioFile.getAbsolutePath());

            recorder.prepare();
            recorder.start();
            Log.i(TAG, "Recording started to temporary file: " + tempAudioFile.getAbsolutePath());
            return true;
        } catch (IOException | IllegalStateException e) {
            Log.e(TAG, "Failed to start recording", e);
            if (recorder != null) {
                recorder.release();
                recorder = null;
            }
            return false;
        }
    }

    public boolean stopRecordingAndSave(File finalFile) {
        if (recorder == null) {
            Log.w(TAG, "No active recording to stop.");
            return false;
        }

        try {
            recorder.stop();
            recorder.release();
            Log.i(TAG, "Recording stopped.");

            if (tempAudioFile != null && tempAudioFile.exists()) {
                try {
                    StorageOperations.copyFile(tempAudioFile, finalFile);
                    Log.i(TAG, "Recording saved to: " + finalFile.getAbsolutePath());
                    return true;
                } catch (IOException e) {
                    Log.e(TAG, "Failed to copy temp file to final destination.", e);
                    return false;
                } finally {
                    tempAudioFile.delete();
                }
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, "Failed to stop recording", e);
        } finally {
            recorder = null;
            tempAudioFile = null;
        }
        return false;
    }
}