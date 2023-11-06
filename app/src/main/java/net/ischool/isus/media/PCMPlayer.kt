package net.ischool.isus.media

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import java.io.FileInputStream
import java.io.IOException

/**
 * PCM播放器
 *
 * Author: walker
 * Email: zhaocework@gmail.com
 * Date: 2023/11/6
 */
object PCMPlayer {
    /**
     * 播放PCM音频文件
     *
     * @param filePath: PCM音频文件路径
     * @param sampleRate: PCM音频文件的采样率，默认16K
     * @param channelConfig: PCM音频文件的声道配置，默认单声道 [AudioFormat.CHANNEL_OUT_MONO]
     * @param audioFormat: PCM音频文件的音频格式，默认16BIT采样 [AudioFormat.ENCODING_PCM_16BIT]
     */
    @JvmOverloads
    fun playPCM(
        filePath: String,
        sampleRate: Int = 16000,
        channelConfig: Int = AudioFormat.CHANNEL_OUT_MONO,
        audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT,
    ) {
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
    }
}