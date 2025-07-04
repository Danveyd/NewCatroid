package org.catrobat.catroid.NN

// Создаем файл OnnxSessionManager.kt

import android.content.Context // Может понадобиться в будущем
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
    // 2. Создаем Scope, привязанный к нашему Job и работающий в фоновом потоке по умолчанию.
    private val scope = CoroutineScope(Dispatchers.IO + job)

    /**
     * Загружает новую модель. Если старая модель была загружена, она будет выгружена.
     * @param modelPath Путь к файлу модели .onnx.
     * @return true в случае успеха, false в случае ошибки.
     */
    fun loadModel(modelPath: String): Boolean {
        // Сначала всегда выгружаем старую модель, чтобы освободить ресурсы
        unloadModel()

        val result = loadModelJNI(modelPath)
        isModelLoaded = (result == 0) // 0 - код успеха из нашего C++
        return isModelLoaded
    }

    /**
     * Выполняет предсказание на загруженной модели.
     * @param inputData Массив входных данных.
     * @return Массив с результатами или null, если модель не загружена или произошла ошибка.
     */
    fun predict(inputData: FloatArray, onResult: (result: FloatArray?) -> Unit) {
        if (!isModelLoaded) {
            onResult(null) // Если модель не загружена, сразу возвращаем null
            return
        }

        // Запускаем корутину в нашем фоновом scope
        scope.launch {
            // Выполняем тяжелую C++ операцию в фоне
            val result = runInferenceJNI(inputData)

            // Переключаемся обратно в главный поток, чтобы безопасно обновить UI/переменные
            withContext(Dispatchers.Main) {
                // Вызываем коллбэк с результатом
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