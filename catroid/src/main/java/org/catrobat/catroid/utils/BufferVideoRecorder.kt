package org.catrobat.catroid.utils

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import com.badlogic.gdx.utils.ScreenUtils
import org.catrobat.catroid.content.RenderTexture
import org.catrobat.catroid.content.RenderTextureManager
import java.io.File
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

object BufferVideoRecorder {
    @Volatile private var isRecording = false
    private var activeBufferName: String? = null

    private var width = 0
    private var height = 0
    private var fps = 30
    private var bitrate = 2_000_000

    private var mediaCodec: MediaCodec? = null
    private var mediaMuxer: MediaMuxer? = null
    private var videoTrackIndex = -1
    private var isMuxerStarted = false

    private val frameQueue = LinkedBlockingQueue<ByteArray>()
    @Volatile private var isEncodingFinished = true

    private var lastFrameTimeNs: Long = 0
    private var frameIntervalNs: Long = 0
    private var frameCount = 0

    fun startRecording(bufferName: String, file: File, targetFps: Int, targetBitrate: Int) {
        if (isRecording) return

        val target = RenderTextureManager.renderTextures[bufferName] ?: return

        isRecording = true
        isEncodingFinished = false
        activeBufferName = bufferName
        width = target.width
        height = target.height
        fps = targetFps
        bitrate = targetBitrate

        frameIntervalNs = 1_000_000_000L / fps
        lastFrameTimeNs = 0
        frameCount = 0
        frameQueue.clear()

        try {
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar)
                setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC).apply {
                configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                start()
            }

            mediaMuxer = MediaMuxer(file.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        } catch (e: Exception) {
            e.printStackTrace()
            releaseResources()
            isEncodingFinished = true
            isRecording = false
            return
        }

        thread(start = true, name = "ChEngineVideoEncoder") {
            runEncoderLoop()
        }
    }

    fun onFrameRendered(bufferName: String, target: RenderTexture) {
        if (!isRecording || activeBufferName != bufferName) return

        val currentTimeNs = System.nanoTime()
        if (lastFrameTimeNs == 0L || (currentTimeNs - lastFrameTimeNs) >= frameIntervalNs) {
            lastFrameTimeNs = currentTimeNs

            target.fbo.begin()
            val pixels = ScreenUtils.getFrameBufferPixels(0, 0, target.width, target.height, true)
            target.fbo.end()

            frameQueue.offer(pixels)
        }
    }

    private fun runEncoderLoop() {
        val bufferInfo = MediaCodec.BufferInfo()
        val yuvBuffer = ByteArray(width * height * 3 / 2)

        try {
            while (isRecording || !frameQueue.isEmpty()) {
                val rgbaFrame = frameQueue.poll(50, TimeUnit.MILLISECONDS) ?: continue

                convertRGBAToYUV420SP(rgbaFrame, width, height, yuvBuffer)

                val codec = mediaCodec ?: break
                val inputBufferIndex = codec.dequeueInputBuffer(10000)
                if (inputBufferIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputBufferIndex) ?: continue
                    inputBuffer.clear()
                    inputBuffer.put(yuvBuffer)

                    val presentationTimeUs = (frameCount * 1_000_000L) / fps
                    codec.queueInputBuffer(inputBufferIndex, 0, yuvBuffer.size, presentationTimeUs, 0)
                    frameCount++
                }

                drainEncoder(bufferInfo)
            }

            val codec = mediaCodec
            if (codec != null) {
                val inputBufferIndex = codec.dequeueInputBuffer(10000)
                if (inputBufferIndex >= 0) {
                    codec.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                }
                drainEncoder(bufferInfo)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            releaseResources()
            isEncodingFinished = true
        }
    }

    private fun drainEncoder(bufferInfo: MediaCodec.BufferInfo) {
        val codec = mediaCodec ?: return
        val muxer = mediaMuxer ?: return

        while (true) {
            val outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
            if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                break
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (isMuxerStarted) {
                    throw RuntimeException("Format changed twice")
                }
                val newFormat = codec.outputFormat
                videoTrackIndex = muxer.addTrack(newFormat)
                muxer.start()
                isMuxerStarted = true
            } else if (outputBufferIndex >= 0) {
                val outputBuffer = codec.getOutputBuffer(outputBufferIndex) ?: continue

                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                    bufferInfo.size = 0
                }

                if (bufferInfo.size != 0) {
                    if (!isMuxerStarted) {
                        throw RuntimeException("Muxer not started")
                    }
                    outputBuffer.position(bufferInfo.offset)
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                    muxer.writeSampleData(videoTrackIndex, outputBuffer, bufferInfo)
                }

                codec.releaseOutputBuffer(outputBufferIndex, false)

                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    break
                }
            }
        }
    }

    fun stopRecordingAndWait() {
        if (!isRecording) return
        isRecording = false

        while (!isEncodingFinished) {
            try {
                Thread.sleep(10)
            } catch (e: InterruptedException) {
                break
            }
        }
    }

    fun isEncodingFinished(): Boolean = isEncodingFinished

    private fun releaseResources() {
        try {
            mediaCodec?.stop()
            mediaCodec?.release()
        } catch (e: Exception) { e.printStackTrace() }
        mediaCodec = null

        try {
            if (isMuxerStarted) {
                mediaMuxer?.stop()
            }
            mediaMuxer?.release()
        } catch (e: Exception) { e.printStackTrace() }
        mediaMuxer = null
        isMuxerStarted = false
        videoTrackIndex = -1
        activeBufferName = null
    }

    private fun convertRGBAToYUV420SP(rgba: ByteArray, width: Int, height: Int, yuv: ByteArray) {
        val frameSize = width * height
        var yIndex = 0
        var uvIndex = frameSize

        for (j in 0 until height) {
            for (i in 0 until width) {
                val index = (j * width + i) * 4
                val r = rgba[index].toInt() and 0xFF
                val g = rgba[index + 1].toInt() and 0xFF
                val b = rgba[index + 2].toInt() and 0xFF

                val y = ((66 * r + 129 * g + 25 * b + 128) shr 8) + 16
                val u = ((-38 * r - 74 * g + 112 * b + 128) shr 8) + 128
                val v = ((112 * r - 94 * g - 18 * b + 128) shr 8) + 128

                yuv[yIndex++] = y.coerceIn(0, 255).toByte()
                if (j % 2 == 0 && i % 2 == 0) {
                    yuv[uvIndex++] = u.coerceIn(0, 255).toByte()
                    yuv[uvIndex++] = v.coerceIn(0, 255).toByte()
                }
            }
        }
    }
}
