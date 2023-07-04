package net.ischool.isus.command

import android.content.Context
import com.walker.anke.framework.reboot
import com.ys.rkapi.MyManager

/**
 * 触沃专用命令执行器
 *
 * Author: walker
 * Email: zhaocework@gmail.com
 * Date: 2023/7/4
 */
class CommandProcessTouchWo(context: Context): CommandProcessorCommon(context) {
    /**
     * 休眠
     *
     * Note：需要系统签名
     */
    override fun sleep(remoteUUID: String) {
        MyManager.getInstance(context).turnOffBackLight()
        // 需要禁用触屏，否则触摸事件会导致背光重新开启
        execRuntimeProcess("su & rm -rf /dev/input/event1")
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