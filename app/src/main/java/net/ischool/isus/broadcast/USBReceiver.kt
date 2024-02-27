package net.ischool.isus.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.io.File

/**
 * U盘插入监听
 *
 * Author: walker
 * Email: zhaocework@gmail.com
 * Date: 2024/2/27
 */
class USBReceiver: BroadcastReceiver() {
    private var usbMounted = false
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_MEDIA_MOUNTED && !usbMounted) {
            usbMounted = true
            // 获取U盘挂载点路径
            val mountPath = intent.data?.path ?: ""
            val usbDrive = File(mountPath)
            if (usbDrive.exists() && usbDrive.isDirectory) {
                val files = usbDrive.listFiles()
                files?.forEach {
                    Log.i("Walker", "file name: ${it.name}")
                }
            }
        }
        if (intent?.action == Intent.ACTION_MEDIA_UNMOUNTED || intent?.action == Intent.ACTION_MEDIA_EJECT) {
            usbMounted = false
        }
    }
}