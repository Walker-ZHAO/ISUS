package net.ischool.isus.command

import android.content.Context
import android.content.Intent
import android.os.Environment
import com.orhanobut.logger.Logger
import com.seewo.sdk.SDKSystemHelper
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
    /**
     * 设备重启
     */
    override fun reboot(remoteUUID: String) {
        finish(CommandResult(ICommand.COMMAND_REBOOT), remoteUUID)
        context.sendBroadcast(Intent("android.intent.action.reboot"))
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
                    context.sendBroadcast(Intent("com.android.lango.installapp").apply {
                        putExtra("apppath", string)
                    })
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
}