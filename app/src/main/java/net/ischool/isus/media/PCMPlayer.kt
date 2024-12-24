package net.ischool.isus.media

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import net.ischool.isus.ISUS
import java.io.FileInputStream
import java.io.IOException
import java.util.concurrent.Executors

/**
 * PCM播放器
 *
 * Author: walker
 * Email: zhaocework@gmail.com
 * Date: 2023/11/6
 */
object PCMPlayer {
    private val executor = Executors.newSingleThreadExecutor()

    /**
     * 播放PCM音频文件
     *
     * @param filePath: PCM音频文件路径
     * @param useMaxVolume: 是否使用最大音量播放，默认为false
     * @param sampleRate: PCM音频文件的采样率，默认16K
     * @param channelConfig: PCM音频文件的声道配置，默认单声道 [AudioFormat.CHANNEL_OUT_MONO]
     * @param audioFormat: PCM音频文件的音频格式，默认16BIT采样 [AudioFormat.ENCODING_PCM_16BIT]
     */
    @JvmOverloads
    fun playPCM(
        filePath: String,
        useMaxVolume: Boolean = false,
        sampleRate: Int = 16000,
        channelConfig: Int = AudioFormat.CHANNEL_OUT_MONO,
        audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT,
    ) {
        executor.execute {
            val audioManager = ISUS.instance.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val streamType = AudioManager.STREAM_MUSIC
            val volumeMax = audioManager.getStreamMaxVolume(streamType)
            val volumeCurrent = audioManager.getStreamVolume(streamType)

            // 使用最大音量播放
            if (useMaxVolume) audioManager.setStreamVolume(streamType, volumeMax, 0)
            val bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize,
                AudioTrack.MODE_STREAM
            )

            try {
                FileInputStream(filePath).use { fis ->
                    audioTrack.play()

                    val buffer = ByteArray(bufferSize)
                    var bytesRead: Int
                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        audioTrack.write(buffer, 0, bytesRead)
                    }
                }
            }  catch (e: IOException) {
                e.printStackTrace()
            }

            audioTrack.stop()
            audioTrack.release()

            // 恢复系统音量
            if (useMaxVolume) audioManager.setStreamVolume(streamType, volumeCurrent, 0)
        }
    }
}