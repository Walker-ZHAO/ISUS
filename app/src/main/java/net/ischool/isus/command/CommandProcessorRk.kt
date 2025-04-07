package net.ischool.isus.command

import android.content.Context
import android.os.Environment
import com.orhanobut.logger.Logger
import com.ys.rkapi.MyManager
import net.ischool.isus.SYSLOG_CATEGORY_RABBITMQ
import net.ischool.isus.isDh32Device
import net.ischool.isus.log.Syslog
import net.ischool.isus.network.APIService
import net.ischool.isus.network.callback.StringCallback
import okhttp3.Request
import java.io.IOException

/**
 * RK通用设备专用命令执行器
 *
 * 适用于触沃、大华非标32寸设备
 *
 * Author: walker
 * Email: zhaocework@gmail.com
 * Date: 2025/1/10
 */
open class CommandProcessorRk(context: Context): CommandProcessorCommon(context) {

    /**
     * 设备重启
     */
    override fun reboot(remoteUUID: String) {
        finish(CommandResult(ICommand.COMMAND_REBOOT), remoteUUID)
        MyManager.getInstance(context).reboot()
    }

    /**
     * 静默安装
     */
    override fun update(url: String?, remoteUUID: String) {
        val model = MyManager.getInstance(context).androidModle
        // 触沃老版本设备，不支持 SDK 静默安装，使用通用方法安装升级
        if (!(model.contains("rk3568") || isDh32Device())) {
            super.update(url, remoteUUID)
            return
        }

        // rk3568_r 与 大华32寸设备，使用 SDK 方式升级
        val result = CommandResult(ICommand.COMMAND_UPDATE)
        if (url == null) {
            result.fail("Update url is invalid")
            finish(result, remoteUUID)
            return
        }

        Syslog.logI("Update start download")
        APIService.downloadAsync(
            url,
            Environment.getExternalStorageDirectory().path,
            callback = object :
                StringCallback {
                override fun onResponse(string: String) {
                    Syslog.logI("Update download success, start install")
                    if (!MyManager.getInstance(context).silentInstallApk(string, true)) {
                        result.fail("install failed")
                    }
                    finish(result, remoteUUID)

                }

                override fun onFailure(request: Request, e: IOException) {
                    Logger.w(e.message ?: "")
                    Syslog.logE("Update file download failure: ${e.message}", category = SYSLOG_CATEGORY_RABBITMQ)
                    result.fail(e.message)
                    finish(result, remoteUUID)
                }
            })
    }

    /**
     * 开启ADB
     */
    override fun openAdb(remoteUUID: String) {
        finish(CommandResult(ICommand.COMMAND_ADB), remoteUUID)
        MyManager.getInstance(context).apply {
            setADBOpen(true)
            setNetworkAdb(true)
        }
    }
}