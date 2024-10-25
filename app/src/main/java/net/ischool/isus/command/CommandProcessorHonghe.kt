package net.ischool.isus.command

import android.content.Context
import android.os.Environment
import com.orhanobut.logger.Logger
import com.xbh.sdk3.AppComm.AppCommHelper
import com.xbh.sdk3.System.SystemHelper
import com.xbh.sdk3.client.UserAPI
import net.ischool.isus.SYSLOG_CATEGORY_RABBITMQ
import net.ischool.isus.log.Syslog
import net.ischool.isus.network.APIService
import net.ischool.isus.network.callback.StringCallback
import okhttp3.Request
import java.io.IOException

/**
 * 鸿合专用命令执行器
 *
 * Author: walker
 * Email: zhaocework@gmail.com
 * Date: 2022/9/29
 */
class CommandProcessorHonghe(context: Context): CommandProcessorCommon(context) {

    private val systemHelper = SystemHelper()
    private val appHelper = AppCommHelper()

    init {
        UserAPI.getInstance().init(context.applicationContext)
    }

    /**
     * 重置
     */
    override fun reset(remoteUUID: String) {
        val result = CommandResult(ICommand.COMMAND_RESET)
        appHelper.apply {
            clearApplicationCacheData(context.packageName)
            clearApplicationUserData(context.packageName)
        }
        finish(result, remoteUUID)
    }

    /**
     * 设备重启
     */
    override fun reboot(remoteUUID: String) {
        finish(CommandResult(ICommand.COMMAND_REBOOT), remoteUUID)
        systemHelper.reboot()
    }

    /**
     * 命令方式进入指定页面
     *
     * Note：需要系统签名
     */
    override fun launchPage(component: String?, remoteUUID: String) {
        val result = CommandResult(ICommand.COMMAND_LAUNCH_PAGE)
        if (component == null) {
            result.fail("Component is invalid")
            finish(result, remoteUUID)
            return
        }
        systemHelper.executeCommand("am start -n $component")
        finish(CommandResult(ICommand.COMMAND_LAUNCH_PAGE), remoteUUID)
    }

    /**
     * 从当前页返回
     */
    override fun backPage(remoteUUID: String) {
        systemHelper.executeCommand("input keyevent BACK")
        finish(CommandResult(ICommand.COMMAND_BACK), remoteUUID)
    }

    /**
     * 静默安装
     */
    override fun update(url: String?, remoteUUID: String) {
        val result = CommandResult(ICommand.COMMAND_UPDATE)
        if (url == null) {
            result.fail("URL is invalid")
            finish(result, remoteUUID)
            return
        }
        APIService.downloadAsync(
            url,
            Environment.getExternalStorageDirectory().path,
            callback = object :
                StringCallback {
                override fun onResponse(string: String) {
                    appHelper.silentInstallApp(string)
                    finish(result, remoteUUID)
                }

                override fun onFailure(request: Request, e: IOException) {
                    Logger.w(e.message ?: "")
                    Syslog.logE("Update file download failure", category = SYSLOG_CATEGORY_RABBITMQ)
                    result.fail(e.message)
                    finish(result, remoteUUID)
                }
            })
    }
}