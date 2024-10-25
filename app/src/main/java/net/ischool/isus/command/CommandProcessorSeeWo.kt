package net.ischool.isus.command

import android.content.Context
import android.os.Environment
import android.view.KeyEvent
import com.orhanobut.logger.Logger
import com.seewo.sdk.SDKSystemHelper
import net.ischool.isus.SYSLOG_CATEGORY_RABBITMQ
import net.ischool.isus.log.Syslog
import net.ischool.isus.network.APIService
import net.ischool.isus.network.callback.StringCallback
import okhttp3.Request
import java.io.IOException

/**
 * 希沃专用命令执行器
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2020/11/4
 */
class CommandProcessorSeeWo(context: Context): CommandProcessorCommon(context){
    /**
     * 设备重启
     */
    override fun reboot(remoteUUID: String) {
        finish(CommandResult(ICommand.COMMAND_REBOOT), remoteUUID)
        SDKSystemHelper.I.reboot()
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
                    if (!SDKSystemHelper.I.installAPKSilent(string))
                        result.fail("install failed")
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
     * 命令方式进入指定页面
     */
    override fun launchPage(component: String?, remoteUUID: String) {
        val result = CommandResult(ICommand.COMMAND_LAUNCH_PAGE)
        if (component == null) {
            result.fail("Component is invalid")
            finish(result, remoteUUID)
            return
        }
        SDKSystemHelper.I.executeCommandWithRoot("am start -n $component")
        finish(result, remoteUUID)
    }

    /**
     * 从当前页返回
     */
    override fun backPage(remoteUUID: String) {
        SDKSystemHelper.I.sendKeyEvent(KeyEvent.KEYCODE_BACK)
        finish(CommandResult(ICommand.COMMAND_BACK), remoteUUID)
    }
}