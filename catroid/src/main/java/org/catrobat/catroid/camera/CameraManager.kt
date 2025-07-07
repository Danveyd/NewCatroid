/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.camera

import android.content.pm.PackageManager
import android.util.Log
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.annotation.UiThread
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.R
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.utils.MobileServiceAvailability
import org.catrobat.catroid.utils.ToastUtil
import org.koin.ext.scope
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import org.koin.java.KoinJavaComponent.get
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class CameraManager(private val stageActivity: StageActivity) : LifecycleOwner {
    private val cameraProvider = ProcessCameraProvider.getInstance(stageActivity).get()
    override val lifecycle = LifecycleRegistry(this)
    val previewView = PreviewView(stageActivity).apply {
        visibility = View.INVISIBLE
    }

    private val previewUseCase = Preview.Builder().build()
    private val analysisUseCase = ImageAnalysis.Builder().build()

    private val imageCaptureUseCase = ImageCapture.Builder().build()

    private var currentCamera: Camera? = null
    private val defaultCameraSelector: CameraSelector
    private var currentCameraSelector: CameraSelector

    val hasFrontCamera = cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
    val hasBackCamera = cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
    val hasFlash = stageActivity.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)

    var previewVisible = false
        private set

    var detectionOn = false
        private set

    var flashOn = false
        private set

    companion object {
        private val TAG = CameraManager::class.java.simpleName
    }

    init {
        if (hasFrontCamera || hasBackCamera) {
            stageActivity.addContentView(
                previewView,
                FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            )
        }

        defaultCameraSelector = if (hasFrontCamera) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        currentCameraSelector = defaultCameraSelector
        lifecycle.currentState = Lifecycle.State.CREATED
    }

    val isCameraFacingFront: Boolean
        get() = currentCameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA

    val isCameraActive: Boolean
        get() = lifecycle.currentState in listOf(Lifecycle.State.STARTED, Lifecycle.State.RESUMED) &&
            (cameraProvider.isBound(previewUseCase) || cameraProvider.isBound(analysisUseCase))

    @Synchronized
    fun reset() {
        flashOn = false
        previewVisible = false
        detectionOn = false
        unbindPreview()
        switchToDefaultCamera()
    }

    @Synchronized
    fun destroy() {
        lifecycle.currentState = Lifecycle.State.DESTROYED
    }

    @Synchronized
    fun pause() {
        lifecycle.currentState = Lifecycle.State.CREATED
    }

    @Synchronized
    fun resume() {
        lifecycle.currentState = Lifecycle.State.RESUMED
        currentCamera?.cameraControl?.enableTorch(flashOn)
    }

    @Synchronized
    fun switchToFrontCamera() {
        if (hasFrontCamera) {
            runInMainThreadAndWait(Runnable { switchCamera(CameraSelector.DEFAULT_FRONT_CAMERA) })
        }
    }

    @Synchronized
    fun switchToBackCamera() {
        if (hasBackCamera) {
            runInMainThreadAndWait(Runnable { switchCamera(CameraSelector.DEFAULT_BACK_CAMERA) })
        }
    }

    private fun switchToDefaultCamera() = switchCamera(defaultCameraSelector)

    private fun switchCamera(cameraSelector: CameraSelector): Boolean {
        if (currentCameraSelector != cameraSelector) {
            currentCameraSelector = cameraSelector
            currentCamera = null
            cameraProvider.unbindAll()
            bindPreview()
            bindFaceAndTextDetector()
            return true
        }
        return false
    }

    @Synchronized
    fun startPreview() {
        if (previewVisible.not()) {
            previewVisible = true
            runInMainThreadAndWait(Runnable {
                previewView.visibility = View.VISIBLE
                if (cameraProvider.isBound(previewUseCase).not()) {
                    bindPreview()
                }
            })
        }
    }

    @Synchronized
    fun stopPreview() {
        if (previewVisible) {
            previewVisible = false
            runInMainThreadAndWait(Runnable {
                if (flashOn.not()) {
                    unbindPreview()
                }
                previewView.visibility = View.INVISIBLE
            })
        }
    }

    @Synchronized
    fun enableFlash() {
        if (flashOn.not()) {
            flashOn = true
            if (currentCamera?.cameraInfo?.hasFlashUnit()?.not() != false && isCameraFacingFront) {
                switchToBackCamera()
            }
            if (cameraProvider.isBound(previewUseCase).not()) {
                runInMainThreadAndWait(Runnable { bindPreview() })
            } else {
                currentCamera?.cameraControl?.enableTorch(true)
            }
        }
    }

    @Synchronized
    fun disableFlash() {
        if (flashOn) {
            flashOn = false
            currentCamera?.cameraControl?.enableTorch(false)
            if (previewVisible.not()) {
                runInMainThreadAndWait(Runnable { unbindPreview() })
            }
        }
    }

    @Synchronized
    fun startDetection(): Boolean {
        if (detectionOn.not()) {
            detectionOn = true
            bindFaceAndTextDetector()
        }
        return true
    }

    private fun bindPreview(): Boolean {
        previewView.visibility = View.VISIBLE

        // ИЗМЕНЕНО: теперь мы привязываем и превью, и захват изображения
        return bindUseCase(previewUseCase, imageCaptureUseCase).also {
            previewUseCase.setSurfaceProvider(previewView.createSurfaceProvider())
            if (previewVisible.not()) {
                previewView.visibility = View.INVISIBLE
            }
        }
    }

    @UiThread
    private fun unbindPreview() {
        cameraProvider.unbind(previewUseCase)
        if (cameraProvider.isBound(analysisUseCase).not()) {
            currentCamera = null
        }
    }

    /**
     * Делает снимок и сохраняет его в указанный файл.
     *
     * @param outputFile Файл, в который будет сохранено изображение. Убедитесь, что у приложения есть права на запись в это место!
     * @param callback Колбэк, который будет вызван по завершении (в основном потоке).
     */
    fun takePicture(outputFile: File, callback: (success: Boolean) -> Unit) {
        // 1. Создаем опции для сохранения, используя переданный файл.
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

        // 2. Вызываем асинхронный метод съемки.
        imageCaptureUseCase.takePicture(
            outputOptions,
            Executors.newSingleThreadExecutor(), // Выполняем в фоновом потоке
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    // Успех! Вызываем колбэк в основном потоке.
                    stageActivity.runOnUiThread {
                        val msg = "Photo capture succeeded: ${outputFile.absolutePath}"
                        //ToastUtil.showSuccess(stageActivity, msg)
                        Log.d(TAG, msg)
                        callback(true) // Теперь просто возвращаем успех
                    }
                }

                override fun onError(exc: ImageCaptureException) {
                    // Ошибка! Вызываем колбэк в основном потоке.
                    stageActivity.runOnUiThread {
                        val msg = "Photo capture failed: ${exc.message}"
                        //ToastUtil.showError(stageActivity, msg)
                        Log.e(TAG, msg, exc)
                        callback(false)
                    }
                }
            }
        )
    }

    fun takePicture2(callback: (success: Boolean, file: File?) -> Unit) {
        if (ProjectManager.getInstance().currentProject == null) {
            Log.e("CameraManager", "Project is null, cannot save photo.")
            callback(false, null) // Не забываем вернуть ошибку
            return
        }

        // 1. Получаем директорию проекта.
        val projectDir = ProjectManager.getInstance().currentProject.filesDir

        // --- НАЧАЛО ИСПРАВЛЕНИЯ ---
        // 2. Убеждаемся, что все родительские папки существуют.
        //    Метод mkdirs() создаст все недостающие папки по пути.
        if (!projectDir.exists()) {
            projectDir.mkdirs()
        }
        // --- КОНЕЦ ИСПРАВЛЕНИЯ ---

        // 3. Теперь безопасно создаем файл.
        //    Имя файла должно быть уникальным, чтобы не затирать старые фото.
        val photoFile = File(
            projectDir,
            "photo.png" // Используем .jpg и уникальное имя
        )

        // Остальной код остается без изменений...
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCaptureUseCase.takePicture(
            outputOptions,
            Executors.newSingleThreadExecutor(),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    stageActivity.runOnUiThread {
                        val msg = "Photo capture succeeded: ${photoFile.absolutePath}"
                        Log.d(TAG, msg)
                        callback(true, photoFile)
                    }
                }
                override fun onError(exc: ImageCaptureException) {
                    stageActivity.runOnUiThread {
                        val msg = "Photo capture failed: ${exc.message}"
                        Log.e(TAG, msg, exc)
                        callback(false, null)
                    }
                }
            }
        )
    }

    @UiThread
    private fun bindFaceAndTextDetector() = bindUseCase(analysisUseCase, imageCaptureUseCase).also {
        val mobileServiceAvailability = get(MobileServiceAvailability::class.java)
        if (mobileServiceAvailability.isGmsAvailable(stageActivity)) {
            CatdroidImageAnalyzer.setActiveDetectorsWithContext(this.stageActivity.context)
            analysisUseCase.setAnalyzer(Executors.newSingleThreadExecutor(), CatdroidImageAnalyzer)
        } else if (mobileServiceAvailability.isHmsAvailable(stageActivity)) {
            analysisUseCase.setAnalyzer(Executors.newSingleThreadExecutor(), FaceTextPoseDetectorHuawei)
        }
    }

    @UiThread
    private fun bindUseCase(vararg useCases: UseCase): Boolean { // ИЗМЕНЕНО: принимаем массив UseCase
        // Сначала отвязываем все, чтобы избежать конфликтов
        // cameraProvider.unbindAll() // Лучше отвязывать конкретные, но для простоты можно и так

        return try {
            // Отвязываем старые use cases перед привязкой новых
            cameraProvider.unbind(*useCases)

            currentCamera = cameraProvider.bindToLifecycle(
                this,
                currentCameraSelector,
                *useCases // ИЗМЕНЕНО: привязываем все переданные use cases
            )
            currentCamera?.cameraControl?.enableTorch(flashOn)
            lifecycle.currentState = Lifecycle.State.STARTED
            true
        } catch (exception: Exception) { // Ловим более общие исключения
            Log.e(TAG, "Could not bind use case(s).", exception)
            handleError()
            false
        }
    }

    private fun runInMainThreadAndWait(runnable: Runnable) {
        val executionLatch = CountDownLatch(1)
        stageActivity.runOnUiThread {
            runnable.run()
            executionLatch.countDown()
        }
        executionLatch.await()
    }

    private fun handleError() {
        ToastUtil.showError(stageActivity, stageActivity.getString(R.string.camera_error_generic))
        destroy()
    }

    //override fun getLifecycle() = lifecycle
}
