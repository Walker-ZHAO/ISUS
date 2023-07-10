package net.ischool.isus.command

import android.content.Context
import android.os.Environment
import com.hikvision.dmb.display.InfoDisplayApi
import com.hikvision.dmb.system.InfoSystemApi
import com.hikvision.dmb.util.InfoUtilApi
import com.orhanobut.logger.Logger
import net.ischool.isus.SYSLOG_CATEGORY_RABBITMQ
import net.ischool.isus.log.Syslog
import net.ischool.isus.network.APIService
import net.ischool.isus.network.callback.StringCallback
import okhttp3.Request
import java.io.IOException

/**
 * 海康威视专用命令执行器
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2020/6/16
 */
class CommandProcessorHik(context: Context): CommandProcessorCommon(context) {

    /**
     * 设备重启
     */
    override fun reboot(remoteUUID: String) {
        finish(CommandResult(ICommand.COMMAND_REBOOT), remoteUUID)
        InfoSystemApi.reboot()
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
                    InfoSystemApi.execCommand("pm install -r $string")
                    finish(result, remoteUUID)
                }

                override fun onFailure(request: Request, e: IOException) {
                    Logger.w(e.message ?: "")
                    Syslog.logE("Update file download failure", SYSLOG_CATEGORY_RABBITMQ)
                    result.fail(e.message)
                    finish(result, remoteUUID)
                }
            })
    }

    /**
     * 命令方式进入指定页面
     */
    override fun launchPage(component: String?, remoteUUID: String) {
        val result = CommandResult(ICommand.COMMAND_LAUNCH_PAGE)
        if (component == null) {
            result.fail("Component is invalid")
            finish(result, remoteUUID)
            return
        }

        val config = component.split("/")
        if (config.size > 1) {
            InfoUtilApi.startUp(config[0], config[1])
            finish(result, remoteUUID)
        } else {
            result.fail("Component is invalid")
            finish(result, remoteUUID)
        }
    }

    /**
     * 从当前页返回
     */
    override fun backPage(remoteUUID: String) {
        InfoSystemApi.execCommand("input keyevent BACK")
        finish(CommandResult(ICommand.COMMAND_BACK), remoteUUID)
    }

    /**
     * 开启ADB
     */
    override fun openAdb(remoteUUID: String) {
        InfoSystemApi.openAdb()
        finish(CommandResult(ICommand.COMMAND_ADB), remoteUUID)
    }
}