package net.ischool.isus.command

import android.content.Context
import android.os.Environment
import com.orhanobut.logger.Logger
import com.ys.rkapi.MyManager
import net.ischool.isus.SYSLOG_CATEGORY_RABBITMQ
import net.ischool.isus.log.Syslog
import net.ischool.isus.network.APIService
import net.ischool.isus.network.callback.StringCallback
import okhttp3.Request
import java.io.IOException

/**
 * 大华32寸设备专用命令执行器
 *
 * Author: walker
 * Email: zhaocework@gmail.com
 * Date: 2023/11/10
 */
class CommandProcessorDh32(context: Context): CommandProcessorCommon(context) {

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
                    if (!MyManager.getInstance(context).silentInstallApk(string)) {
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
}