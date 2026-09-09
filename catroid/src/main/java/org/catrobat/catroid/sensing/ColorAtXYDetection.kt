/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2024 The Catrobat Team
 */
package org.catrobat.catroid.sensing

import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.SurfaceView
import android.widget.VideoView
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Matrix4
import com.danvexteam.lunoscript_annotations.LunoClass
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.content.Look
import org.catrobat.catroid.content.Project
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.common.Conversions.convertArgumentToDouble
import org.catrobat.catroid.stage.StageActivity
import org.catrobat.catroid.stage.StageListener
import kotlin.math.roundToInt

private const val COLOR_HEX_PREFIX = "#"
private const val RGBA_START_INDEX = 0
private const val RGBA_END_INDEX = 6
private const val ARGB_START_INDEX = 2
private const val ARGB_END_INDEX = 8
private const val HEX_COLOR_BLACK = "#000000"

@LunoClass
class ColorAtXYDetection(
    scope: Scope,
    stageListener: StageListener?
) : ColorDetection(scope, stageListener) {

    companion object {
        @JvmStatic
        fun disposeShared() {}

        // 1x1 Bitmap для считывания ровно 1 пикселя
        private val pixelBitmap: Bitmap by lazy {
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }
        private val srcRect = Rect()

        // Главный обработчик для системных колбэков SurfaceFlinger
        private val mainHandler: Handler by lazy {
            Handler(Looper.getMainLooper())
        }

        // Асинхронный кэш цвета (0 мс задержки для 60 FPS)
        @Volatile private var cachedColorHex: String = HEX_COLOR_BLACK
        @Volatile private var isCopyInProgress: Boolean = false
        @Volatile private var lastRequestTime: Long = 0L
    }

    private var xPosition: Int = 0
    private var yPosition: Int = 0

    @Suppress("TooGenericExceptionCaught")
    fun tryInterpretFunctionColorAtXY(x: Any?, y: Any?): String {
        setBufferParameters()

        val xPositionUnchecked = convertArgumentToDouble(x) ?: return "NaN"
        val yPositionUnchecked = convertArgumentToDouble(y) ?: return "NaN"

        if (xPositionUnchecked.isNaN() || yPositionUnchecked.isNaN()) {
            return "NaN"
        }

        xPosition = xPositionUnchecked.roundToInt()
        yPosition = yPositionUnchecked.roundToInt()

        // 1. Сначала проверяем обычные спрайты LibGDX (если они поверх видео)
        val listener = stageListener ?: StageActivity.getActiveStageListener()
        val spriteColor = getHexColorStringFromStagePixmap(listener)
        if (spriteColor != HEX_COLOR_BLACK) {
            return spriteColor
        }

        // 2. Запрашиваем считывание кадра с VideoView в фоне
        sampleVideoViewDirectly(xPosition, yPosition)

        // 3. Мгновенно возвращаем актуальный цвет видео (без блокировки потока рендера)
        return cachedColorHex
    }

    /**
     * Считывает пиксель напрямую из Surface видеодекодера VideoView
     */
    private fun sampleVideoViewDirectly(catroidX: Int, catroidY: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        // Защита от зависания очереди: если запрос выполняется дольше 100 мс, сбрасываем флаг
        val now = System.currentTimeMillis()
        if (isCopyInProgress) {
            if (now - lastRequestTime > 100) {
                isCopyInProgress = false
            } else {
                return
            }
        }

        val activity = StageActivity.activeStageActivity?.get() ?: return
        val videoView = findVideoPlayerSurfaceView(activity) ?: return

        val holder = videoView.holder ?: return
        val surface = holder.surface ?: return
        if (!surface.isValid) return

        // Получаем реальные размеры видеоповерхности
        val surfaceFrame = holder.surfaceFrame
        val surfW = if (surfaceFrame != null && surfaceFrame.width() > 0) surfaceFrame.width() else videoView.width
        val surfH = if (surfaceFrame != null && surfaceFrame.height() > 0) surfaceFrame.height() else videoView.height

        if (surfW <= 0 || surfH <= 0) return

        // Точный перевод координат Catroid (центр 0,0, Y вверх) в координаты видео (0,0 слева сверху, Y вниз)
        val vW = if (virtualWidth > 0) virtualWidth.toFloat() else surfW.toFloat()
        val vH = if (virtualHeight > 0) virtualHeight.toFloat() else surfH.toFloat()

        val normX = ((catroidX + vW / 2f) / vW).coerceIn(0f, 1f)
        val normY = ((vH / 2f - catroidY) / vH).coerceIn(0f, 1f)

        val px = (normX * (surfW - 1)).toInt().coerceIn(0, surfW - 1)
        val py = (normY * (surfH - 1)).toInt().coerceIn(0, surfH - 1)

        srcRect.set(px, py, px + 1, py + 1)

        isCopyInProgress = true
        lastRequestTime = now

        try {
            // Запрос PixelCopy напрямую к аппаратному Surface видео
            PixelCopy.request(
                surface,
                srcRect,
                pixelBitmap,
                { copyResult ->
                    if (copyResult == PixelCopy.SUCCESS) {
                        val pixel = pixelBitmap.getPixel(0, 0)
                        val r = (pixel shr 16) and 0xFF
                        val g = (pixel shr 8) and 0xFF
                        val b = pixel and 0xFF

                        val rHex = r.toString(16).padStart(2, '0')
                        val gHex = g.toString(16).padStart(2, '0')
                        val bHex = b.toString(16).padStart(2, '0')
                        cachedColorHex = "#$rHex$gHex$bHex"
                    }
                    isCopyInProgress = false
                },
                mainHandler
            )
        } catch (_: Exception) {
            isCopyInProgress = false
        }
    }

    /**
     * Находит именно видеоплеер, строго игнорируя gameView LibGDX
     */
    private fun findVideoPlayerSurfaceView(activity: StageActivity): SurfaceView? {
        // 1. Ищем в dynamicViews (куда VideoPlayer добавляется при создании)
        val dynamicViews = activity.dynamicViews
        if (dynamicViews != null) {
            for (view in dynamicViews.values) {
                if (view is VideoView) return view
                if (view is SurfaceView) return view
            }
        }

        // 2. Ищем в backgroundLayout
        val bgLayout = activity.backgroundLayout
        if (bgLayout != null) {
            for (i in bgLayout.childCount - 1 downTo 0) {
                val child = bgLayout.getChildAt(i)
                if (child is VideoView) return child
                if (child is SurfaceView) return child
            }
        }

        return null
    }

    private fun getHexColorStringFromStagePixmap(listener: StageListener?): String {
        val stListener = listener ?: return HEX_COLOR_BLACK
        val sprites = stListener.spritesFromStage ?: return HEX_COLOR_BLACK

        val queryX = xPosition.toFloat()
        val queryY = yPosition.toFloat()
        val localPos = com.badlogic.gdx.math.Vector2()

        for (i in sprites.indices.reversed()) {
            val sprite = sprites[i]
            val look = sprite.look ?: continue
            if (!look.isLookVisible || !look.isVisible) continue

            val polygons = look.currentCollisionPolygon
            var isInsidePolygon = false
            for (poly in polygons) {
                if (poly.contains(queryX, queryY)) {
                    isInsidePolygon = true
                    break
                }
            }

            if (isInsidePolygon) {
                val lookData = look.lookData ?: continue
                val pixmap = lookData.pixmap ?: continue

                localPos.set(queryX, queryY)
                look.stageToLocalCoordinates(localPos)

                val px = localPos.x.roundToInt()
                val py = pixmap.height - localPos.y.roundToInt()

                if (px in 0 until pixmap.width && py in 0 until pixmap.height) {
                    val pixelVal = pixmap.getPixel(px, py)
                    val alpha = pixelVal and 0xFF

                    if (alpha > 10) {
                        val r = (pixelVal shr 24) and 0xFF
                        val g = (pixelVal shr 16) and 0xFF
                        val b = (pixelVal shr 8) and 0xFF

                        val rHex = r.toString(16).padStart(2, '0')
                        val gHex = g.toString(16).padStart(2, '0')
                        val bHex = b.toString(16).padStart(2, '0')
                        return "#$rHex$gHex$bHex"
                    }
                }
            }
        }

        return HEX_COLOR_BLACK
    }

    override fun getLooksOfRelevantSprites(): MutableList<Look>? =
        stageListener?.let {
            ArrayList(it.spritesFromStage)
                .filter { s -> s.look.isLookVisible }
                .map { s -> s.look }
                .toMutableList()
        }

    override fun setBufferParameters() {
        bufferHeight = 1
        bufferWidth = 1
    }

    override fun isParameterInvalid(parameter: Any?): Boolean =
        convertArgumentToDouble(parameter) == null

    override fun createProjectionMatrix(project: Project): Matrix4 {
        val camera = OrthographicCamera(bufferWidth.toFloat(), bufferHeight.toFloat())
        val viewPort = createViewport(
            project,
            bufferWidth.toFloat(),
            bufferHeight.toFloat(),
            camera
        )
        viewPort.apply()
        camera.position.set(xPosition.toFloat(), yPosition.toFloat(), 0f)
        camera.rotate(-look.rotation)
        camera.update()
        return camera.combined
    }
}
