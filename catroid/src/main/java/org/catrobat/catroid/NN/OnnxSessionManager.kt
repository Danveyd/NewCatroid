package org.catrobat.catroid.NN

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.catrobat.catroid.utils.NativeBridge
import org.catrobat.catroid.utils.NativeLibraryManager

object OnnxSessionManager {

    var isWorking = true

    private var isModelLoaded = false
    private val executor = Executors.newSingleThreadExecutor()

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)


    fun loadModel(modelPath: String): Boolean {
        unloadModel()

        val result = loadModelJNI(modelPath)
        isModelLoaded = (result == 0)
        return isModelLoaded
    }

    fun predict(inputData: FloatArray, onResult: (result: FloatArray?) -> Unit) {
        if (!isModelLoaded) {
            onResult(null)
            return
        }

        scope.launch {
            val result = runInferenceJNI(inputData)

            withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    }


    fun unloadModel() {
        if (isModelLoaded) {
            unloadModelJNI()
            isModelLoaded = false
        }
    }

    fun loadModelAsync(modelPath: String): Future<Boolean> {
        return executor.submit<Boolean> {
            unloadModel()
            val result = loadModelJNI(modelPath)
            isModelLoaded = (result == 0)
            isModelLoaded
        }
    }

    fun predictAsync(inputData: FloatArray): Future<FloatArray?>? {
        if (!isModelLoaded) return null
        return executor.submit<FloatArray?> {
            runInferenceJNI(inputData)
        }
    }

    fun unloadModelAsync(): Future<Unit> {
        return executor.submit<Unit> {
            if (isModelLoaded) {
                unloadModelJNI()
                isModelLoaded = false
            }
        }
    }

    private external fun loadModelJNI(modelPath: String): Int
    private external fun runInferenceJNI(inputData: FloatArray): FloatArray?
    private external fun unloadModelJNI()

    init {
        if (!NativeLibraryManager.isLoaded(NativeLibraryManager.Feature.CORE)) {
            OnnxSessionManager.isWorking = false
        }
    }
}