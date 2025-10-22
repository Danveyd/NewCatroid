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

object OnnxSessionManager {

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


    /**
     * Выгружает текущую модель и освобождает ресурсы.
     */
    fun unloadModel() {
        if (isModelLoaded) {
            unloadModelJNI()
            isModelLoaded = false
        }
    }

    fun loadModelAsync(modelPath: String): Future<Boolean> {
        return executor.submit<Boolean> {
            unloadModel() // Выгружаем старую
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

    // --- Мост в C++ (JNI функции) ---

    private external fun loadModelJNI(modelPath: String): Int
    private external fun runInferenceJNI(inputData: FloatArray): FloatArray?
    private external fun unloadModelJNI()

    // Загружаем нашу C++ библиотеку
    init {
        System.loadLibrary("catroid") // Убедитесь, что имя совпадает с вашим
    }
}