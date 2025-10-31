package org.catrobat.catroid.utils

import android.util.Log

object NativeLibraryManager {

    private const val TAG = "NativeLibManager"

    enum class Feature {
        CORE,
        PYTHON,
        VNC,
        ONNX
    }

    private val featureStatus = mutableMapOf<Feature, Boolean>()


    fun initialize() {
        Log.i(TAG, "Initializing native libraries...")

        featureStatus[Feature.CORE] = try {
            System.loadLibrary("catroid")
            Log.d(TAG, "'catroid' loaded.")
            true
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load 'catroid' library!", e)
            false
        }

        featureStatus[Feature.VNC] = try {
            System.loadLibrary("native-vnc")
            Log.d(TAG, "'native-vnc' loaded.")
            true
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load 'native-vnc' library!", e)
            false
        }

        featureStatus[Feature.PYTHON] = try {
            System.loadLibrary("crypto")
            System.loadLibrary("ssl")
            System.loadLibrary("z")
            System.loadLibrary("expat")
            System.loadLibrary("openblas")
            System.loadLibrary("jpeg")
            System.loadLibrary("png")

            System.loadLibrary("python3.12")
            Log.d(TAG, "All Python libraries loaded.")
            true
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "A Python-related library failed to load. Python feature disabled.", e)
            false
        }

        featureStatus[Feature.ONNX] = isLoaded(Feature.CORE)
    }

    fun isLoaded(feature: Feature): Boolean {
        return featureStatus[feature] ?: false
    }
}