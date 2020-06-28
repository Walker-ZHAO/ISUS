package net.ischool.isus.command

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.os.Process
import android.provider.Settings
import com.orhanobut.logger.Logger
import net.ischool.isus.network.APIService
import net.ischool.isus.network.callback.StringCallback
import okhttp3.Request
import java.io.IOException
import com.walker.anke.framework.reboot
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import net.ischool.isus.activity.ConfigActivity
import net.ischool.isus.log.Syslog
import org.jetbrains.anko.alarmManager
import java.io.File

/**
 * 通用命令执行器
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2017/9/14
 */
open class CommandProcessorCommon constructor(protected val context: Context) : ICommand {

    private val resultCallbackList = mutableListOf<(CommandResult, String) -> Unit>()

    override fun addResultCallback(callback: (CommandResult, String) -> Unit) = resultCallbackList.add(callback)
    override fun removeResultCallback(callback: (CommandResult, String) -> Unit) = resultCallbackList.remove(callback)

    /**
     * 执行结果通知
     */
    protected fun finish(result: CommandResult, remoteUUID: String) = resultCallbackList.forEach { it(result, remoteUUID) }


    /**
     * ping响应
     */
    override fun ping(remoteUUID: String) {
        val result = CommandResult(ICommand.COMMAND_PING)
        APIService.pong()
            .subscribeOn(Schedulers.io())
            .subscribeBy(
                onNext = {
                    finish(result, remoteUUID)
                },
                onError = {
                    Syslog.logE("Response ping command failure: ${it.message}")
                    result.fail(it.message)
                    finish(result, remoteUUID)
                }
            )
    }

    /**
     * 进入配置页
     */
    override fun config(remoteUUID: String) {
        val intent = Intent(context, ConfigActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(intent)
        finish(CommandResult(ICommand.COMMAND_CONFIG), remoteUUID)
    }

    /**
     * 重置
     */
    override fun reset(remoteUUID: String) {
        val result = CommandResult(ICommand.COMMAND_RESET)
        val appDir = File(context.cacheDir.parent)
        if (appDir.exists()) {
            appDir.list()
                .filter { it != "lib" }
                .forEach {
                    deleteDir(File(appDir, it))
                }
            Process.killProcess(Process.myPid())
            finish(result, remoteUUID)
        } else {
            result.fail("Can't find app")
            finish(result, remoteUUID)
        }
    }

    /**
     * 设备重启
     *
     * Note：需要系统签名
     */
    override fun reboot(remoteUUID: String) {
        finish(CommandResult(ICommand.COMMAND_REBOOT), remoteUUID)
        context.reboot(null)
    }

    /**
     * 回到桌面
     */
    override fun quit(remoteUUID: String) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        }
        context.startActivity(intent)
        finish(CommandResult(ICommand.COMMAND_QUIT), remoteUUID)
    }

    /**
     * 静默安装
     *
     * Note：需要系统签名
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
                    execRuntimeProcess("pm install -r $string")
                    finish(result, remoteUUID)
                }

                override fun onFailure(request: Request, e: IOException) {
                    Logger.w(e.message)
                    Syslog.logE("Update file download failure")
                    result.fail(e.message)
                    finish(result, remoteUUID)
                }
            })
    }

    /**
     * 进入系统设置页
     */
    override fun setting(remoteUUID: String) {
        val intent = Intent(Settings.ACTION_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(intent)
        finish(CommandResult(ICommand.COMMAND_SETTING), remoteUUID)
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
        execRuntimeProcess("am start -n $component")
        finish(CommandResult(ICommand.COMMAND_LAUNCH_PAGE), remoteUUID)
    }

    /**
     * 从当前页返回
     */
    override fun backPage(remoteUUID: String) {
        execRuntimeProcess("input keyevent BACK\n")
        finish(CommandResult(ICommand.COMMAND_BACK), remoteUUID)
    }

    /**
     * 开启ADB
     * 仅海康设备有效，默认的命令执行器不实现
     */
    override fun openAdb(remoteUUID: String) {
        finish(CommandResult(ICommand.COMMAND_ADB).apply { fail("Current device doesn't support") }, remoteUUID)
    }

    /**
     * 更新应用配置信息
     */
    override fun reload(remoteUUID: String) {
        val result = CommandResult(ICommand.COMMAND_RELOAD)
        APIService.getConfig()
            .subscribeOn(Schedulers.io())
            .subscribeBy(
                onNext = {
                    finish(result, remoteUUID)
                    reboot(remoteUUID)
                },
                onError = {
                    Syslog.logE("Reload config failure: ${it.message}")
                    result.fail(it.message)
                    finish(result, remoteUUID)
                }
            )
    }

    /**
     * 延时进入主目录
     * @param triggerAtMillis: 延迟时间，单位毫秒
     *
     */
    fun launchHome(triggerAtMillis: Long) {
        val startActivity = Intent(Intent.ACTION_MAIN)
        startActivity.addCategory(Intent.CATEGORY_HOME)
        startActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        val pendingIntent = PendingIntent.getActivity(context, 123456, startActivity, PendingIntent.FLAG_CANCEL_CURRENT)
        context.alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + triggerAtMillis, pendingIntent)
    }

    /**
     * 删除指定目录下的所有文件
     * @param dir: 指定的目录文件
     */
    private fun deleteDir(dir: File): Boolean {
        if (dir.isDirectory) {
            dir.list().forEach {
                val success = deleteDir(File(dir, it))
                if (!success)
                    return false
            }
        }
        return dir.delete()
    }
}