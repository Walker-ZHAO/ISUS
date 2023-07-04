package net.ischool.isus.command

import android.content.Context
import android.lamy.display.screen.Screen
import com.walker.anke.framework.reboot

/**
 * 大华专用命令执行器
 *
 * Author: walker
 * Email: zhaocework@gmail.com
 * Date: 2023/7/4
 */
class CommandProcessDaHua(context: Context): CommandProcessorCommon(context) {
    /**
     * 休眠
     *
     * Note：需要系统签名
     */
    override fun sleep(remoteUUID: String) {
        Screen.getScreen(0).turnOffBackLight()
        // 需要禁用触屏，否则触摸事件会下发至应用
        execRuntimeProcess("su & rm -rf /dev/input/event2")
        finish(CommandResult(ICommand.COMMAND_SLEEP), remoteUUID)
    }

    /**
     * 唤醒
     *
     * Note：需要系统签名
     */
    override fun wakeup(remoteUUID: String) {
        // 重启设备以唤醒
        finish(CommandResult(ICommand.COMMAND_WAKEUP), remoteUUID)
        context.reboot(null)
    }
}