package net.ischool.isus.command

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
     * 进入设置页
     */
    fun setting()

    /**
     * 启动指定页面
     */
    fun launchPage(component: String?)

    /**
     * 从当前页返回
     */
    fun backPage()

    /**
     * 开启adb调试(仅海康设备有效)
     */
    fun openAdb()

    /**
     * 更新配置信息
     */
    fun reload()

    /**
     * 执行cmd命令
     */
    fun execRuntimeProcess(cmd: String): Process? {
        var p: Process? = null
        try {
            p = Runtime.getRuntime().exec(cmd)
            p?.waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return p
    }
}