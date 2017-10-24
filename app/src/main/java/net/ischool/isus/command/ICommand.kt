package net.ischool.isus.command

import java.io.IOException

/**
 * 命令接口
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2017/9/14
 */
interface ICommand {

    companion object {
        val COMMAND_PING    =   "ping"
        val COMMAND_CONFIG  =   "config"
        val COMMAND_RESET   =   "reset"
        val COMMAND_REBOOT  =   "reboot"
        val COMMAND_QUIT    =   "quit"
        val COMMAND_UPDATE  =   "update"
    }

    /**
     * 调用/eqptapi/pong响应
     */
    fun ping()

    /**
     * 进入设置界面
     */
    fun config()

    /**
     * 重置APP
     */
    fun reset()

    /**
     * 重启设备
     */
    fun reboot()

    /**
     * 退出到桌面
     */
    fun quit()

    /**
     * 更新
     */
    fun update(url: String?)

    /**
     * 执行cmd命令
     */
    fun execRuntimeProcess(cmd: String): Process? {
        var p: Process? = null
        try {
            p = Runtime.getRuntime().exec(cmd);
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return p;
    }
}