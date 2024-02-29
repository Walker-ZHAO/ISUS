package net.ischool.isus.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.gson.Gson
import net.ischool.isus.model.Command
import java.io.File
import com.walker.anke.gson.fromJson
import net.ischool.isus.command.CommandParser
import net.ischool.isus.preference.PreferenceManager

/**
 * U盘插入监听
 *
 * Author: walker
 * Email: zhaocework@gmail.com
 * Date: 2024/2/27
 */
class USBReceiver: BroadcastReceiver() {
    companion object {
        private const val COMMAND_FILE = "commander.json"
    }
    private var usbMounted = false
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_MEDIA_MOUNTED && !usbMounted) {
            usbMounted = true
            try {
                // 获取U盘挂载点路径
                val mountPath = intent.data?.path ?: ""
                val usbDrive = File(mountPath)
                if (usbDrive.exists() && usbDrive.isDirectory) {
                    val commandFile = usbDrive.listFiles()?.firstOrNull { it.name == COMMAND_FILE }
                    val fileContent = commandFile?.readText(Charsets.UTF_8) ?: ""
                    // 解析文件内部命令列表
                    val commandList = Gson().fromJson<Array<Command>>(fileContent)
                    commandList.forEach {
                        CommandParser.instance.processCommand(it.copy(cmdbid = PreferenceManager.instance.getCMDB()))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (intent?.action == Intent.ACTION_MEDIA_UNMOUNTED || intent?.action == Intent.ACTION_MEDIA_EJECT) {
            usbMounted = false
        }
    }
}