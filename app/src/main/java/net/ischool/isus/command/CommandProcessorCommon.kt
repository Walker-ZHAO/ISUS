package net.ischool.isus.command

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.os.Process
import android.provider.Settings
import com.orhanobut.logger.Logger
import com.walker.anke.framework.alarmManager
import com.walker.anke.framework.reboot
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import net.ischool.isus.QueryType
import net.ischool.isus.RESULT_OK
import net.ischool.isus.SYSLOG_CATEGORY_RABBITMQ
import net.ischool.isus.activity.ConfigActivity
import net.ischool.isus.log.Syslog
import net.ischool.isus.network.APIService
import net.ischool.isus.network.callback.StringCallback
import net.ischool.isus.preference.PreferenceManager
import net.ischool.isus.service.RabbitMQService
import net.ischool.isus.service.QueueState
import net.ischool.isus.sleep
import net.ischool.isus.wakeup
import okhttp3.Request
import java.io.File
import java.io.IOException

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
    protected fun finish(result: CommandResult, remoteUUID: String) {
        Syslog.logI("command process result: $result")
        resultCallbackList.forEach { it(result, remoteUUID) }
    }


    /**
     * ping响应
     */
    @SuppressLint("CheckResult")
    override fun ping(remoteUUID: String) {
        val result = CommandResult(ICommand.COMMAND_PING)
        APIService.pong()
            .subscribeOn(Schedulers.io())
            .subscribeBy(
                onNext = {
                    finish(result, remoteUUID)
                },
                onError = {
                    Syslog.logE("Response ping command failure: ${it.message}", category = SYSLOG_CATEGORY_RABBITMQ)
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
     *
     * Note：需要系统签名
     */
    override fun reset(remoteUUID: String) {
        val result = CommandResult(ICommand.COMMAND_RESET)
        val appDir = File(context.cacheDir.parent ?: "")
        if (appDir.exists()) {
            appDir.list()
                ?.filter { it != "lib" }
                ?.forEach {
                    deleteDir(File(appDir, it))
                }
            finish(result, remoteUUID)
            reboot()
            Process.killProcess(Process.myPid())
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
                    execRuntimeProcess("pm install -r $string")
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
     * 仅海康&触沃设备有效，默认的命令执行器不实现
     */
    override fun openAdb(remoteUUID: String) {
        finish(CommandResult(ICommand.COMMAND_ADB).apply { fail("Current device doesn't support") }, remoteUUID)
    }

    /**
     * 更新应用配置信息
     */
    @SuppressLint("CheckResult")
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
                    Syslog.logE("Reload config failure: ${it.message}", category = SYSLOG_CATEGORY_RABBITMQ)
                    result.fail(it.message)
                    finish(result, remoteUUID)
                }
            )
    }

    @SuppressLint("CheckResult")
    override fun queryStatus(type: String?, remoteUUID: String) {
        val result = CommandResult(ICommand.COMMAND_QUERY_STATUS)
        if (type == null) {
            result.fail("Illegal Query Status Type: null")
            finish(result, remoteUUID)
            return
        }

        when (type.toInt()) {
            QueryType.QUERY_HTTP -> {
                APIService.getNetworkStatus()
                    .subscribeOn(Schedulers.io())
                    .subscribeBy(
                        onNext = {
                            val status = checkNotNull(it.body())
                            if (status.errno == RESULT_OK) {
                                if (status.data.sids.contains(PreferenceManager.instance.getSchoolId()))
                                    result.success("HTTP Status: Online")
                                else
                                    result.fail("HTTP Status: Offline[Can't match SchoolId]")
                                finish(result, remoteUUID)
                            } else {
                                result.fail("HTTP Status: Offline[${status.error}]")
                                finish(result, remoteUUID)
                            }
                        },
                        onError = {
                            result.fail("HTTP Status: Offline[${it.message}]")
                            finish(result, remoteUUID)
                        }
                    )
            }
            QueryType.QUERY_RABBITMQ -> {
                when (RabbitMQService.queueState) {
                    QueueState.STATE_BLOCK -> result.fail("RabbitMQ Status: Offline")
                    QueueState.STATE_STANDBY -> result.success("RabbitMQ Status: Online")
                }
                finish(result, remoteUUID)
            }
            else -> {
                result.fail("Unsupported Query Status Type: $type")
                finish(result, remoteUUID)
            }
        }
    }

    /**
     * 休眠
     *
     * Note：需要系统签名
     */
    override fun sleep(remoteUUID: String) {
        context.sleep()
        finish(CommandResult(ICommand.COMMAND_SLEEP), remoteUUID)
    }

    /**
     * 唤醒
     *
     * Note：需要系统签名
     */
    override fun wakeup(remoteUUID: String) {
        finish(CommandResult(ICommand.COMMAND_WAKEUP), remoteUUID)
        context.wakeup()
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
            dir.list()?.forEach {
                val success = deleteDir(File(dir, it))
                if (!success)
                    return false
            }
        }
        return dir.delete()
    }
}