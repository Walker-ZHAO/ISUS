package net.ischool.isus.command

import android.app.smdt.SmdtManagerNew
import android.content.Context
import android.os.Environment
import com.orhanobut.logger.Logger
import net.ischool.isus.SYSLOG_CATEGORY_RABBITMQ
import net.ischool.isus.log.Syslog
import net.ischool.isus.network.APIService
import net.ischool.isus.network.callback.StringCallback
import okhttp3.Request
import java.io.IOException

/**
 * 视美泰设备专用命令执行器
 *
 * 如成都元素科技的EC-WCB-EB设备
 *
 * Author: walker
 * Email: zhaocework@gmail.com
 * Date: 2025/4/25
 */
class CommandProcessorSmt(context: Context): CommandProcessorCommon(context) {
    /**
     * 设备重启
     */
    override fun reboot(remoteUUID: String) {
        finish(CommandResult(ICommand.COMMAND_REBOOT), remoteUUID)
        SmdtManagerNew.getInstance(context).sys_setReboot()
    }

    /**
     * 静默安装
     */
    override fun update(url: String?, remoteUUID: String) {
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

                    SmdtManagerNew.getInstance(context)
                        .sys_doSilentInstallApp(string, object : SmdtManagerNew.InstallCallback() {
                            override fun onInstallFinished(
                                packageName: String?,
                                resultCode: Int,
                                msg: String?
                            ) {
                                if (resultCode != 0) {
                                    result.fail("install failed: $msg")
                                }
                                finish(result, remoteUUID)
                            }
                        })
                }

                override fun onFailure(request: Request, e: IOException) {
                    Logger.w(e.message ?: "")
                    Syslog.logE(
                        "Update file download failure: ${e.message}",
                        category = SYSLOG_CATEGORY_RABBITMQ
                    )
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
        SmdtManagerNew.getInstance(context).apply {
            sys_setDeveloperOptions(true)
            sys_setAdbDebug(0, true)
            sys_setAdbDebug(1, true)
        }
    }
}