package net.ischool.isus.command

import java.io.DataOutputStream

/**
 * 命令接口
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2017/9/14
 */
interface ICommand {

    companion object {
        const val COMMAND_PING    =   "ping"
        const val COMMAND_CONFIG  =   "config"
        const val COMMAND_RESET   =   "reset"
        const val COMMAND_REBOOT  =   "reboot"
        const val COMMAND_QUIT    =   "quit"
        const val COMMAND_UPDATE  =   "update"
        const val COMMAND_SETTING =   "setting"
        const val COMMAND_BACK =   "back"
        const val COMMAND_ADB =   "adb"
        const val COMMAND_RELOAD = "reload"
        const val COMMAND_LAUNCH_PAGE =   "amstart"
        const val COMMAND_QUERY_STATUS = "query_status"
        const val COMMAND_SLEEP = "sleep"
        const val COMMAND_WAKEUP = "wakeup"
        const val COMMAND_REFRESH = "refresh"   // 刷新页面，该命令不可以注册到命令列表中，需要转发给实际的宿主应用
    }

    /**
     * 命令结果回调管理
     */
    fun addResultCallback(callback: (CommandResult, String) -> Unit): Boolean
    fun removeResultCallback(callback: (CommandResult, String) -> Unit): Boolean

    /**
     * 调用/eqptapi/pong响应
     */
    fun ping(remoteUUID: String = "")

    /**
     * 进入设置界面
     */
    fun config(remoteUUID: String = "")

    /**
     * 重置APP
     */
    fun reset(remoteUUID: String = "")

    /**
     * 重启设备
     */
    fun reboot(remoteUUID: String = "")

    /**
     * 退出到桌面
     */
    fun quit(remoteUUID: String = "")

    /**
     * 更新
     */
    fun update(url: String?, remoteUUID: String = "")

    /**
     * 进入设置页
     */
    fun setting(remoteUUID: String = "")

    /**
     * 启动指定页面
     */
    fun launchPage(component: String?, remoteUUID: String = "")

    /**
     * 从当前页返回
     */
    fun backPage(remoteUUID: String = "")

    /**
     * 开启adb调试(仅海康设备有效)
     */
    fun openAdb(remoteUUID: String = "")

    /**
     * 更新配置信息
     */
    fun reload(remoteUUID: String = "")

    /**
     * 查询状态信息
     */
    fun queryStatus(type: String?, remoteUUID: String = "")

    /**
     * 进入休眠状态
     */
    fun sleep(remoteUUID: String = "")

    /**
     * 退出休眠状态（唤醒）
     */
    fun wakeup(remoteUUID: String = "")

    /**
     * 执行cmd命令
     */
    fun execRuntimeProcess(cmd: String, needEvn: Boolean = false): Process? {
        var p: Process? = null
        try {
            if (!needEvn) {
                p = Runtime.getRuntime().exec(cmd)
            } else {
                p = Runtime.getRuntime().exec("sh")
                DataOutputStream(p.outputStream).use {
                    it.writeBytes("$cmd\n")
                    it.flush()
                }
            }
            p?.waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return p
    }
}