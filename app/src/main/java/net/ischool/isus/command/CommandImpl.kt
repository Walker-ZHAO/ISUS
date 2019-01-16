package net.ischool.isus.command

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.os.Process
import com.orhanobut.logger.Logger
import io.reactivex.schedulers.Schedulers
import net.ischool.isus.activity.ConfigActivity
import net.ischool.isus.network.APIService
import net.ischool.isus.network.callback.StringCallback
import okhttp3.Request
import java.io.IOException
import android.provider.Settings;
import com.walker.anke.framework.reboot
import io.reactivex.rxkotlin.subscribeBy
import net.ischool.isus.log.Syslog
import org.jetbrains.anko.alarmManager
import java.io.File


/**
 * 具体命令实现
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2017/9/14
 */
class CommandImpl constructor(private val context: Context) : ICommand {

    companion object {
        val TAG = this::class.java.simpleName
    }

    /**
     * ping响应
     */
    override fun ping() {
        APIService.pong()
                .subscribeOn(Schedulers.io())
                .subscribeBy(
                    onNext = {},
                    onError = { Syslog.logE("Response ping command failure: ${it.message}") }
                )
    }

    /**
     * 进入配置页
     */
    override fun config() {
        val intent = Intent(context, ConfigActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * 重置
     */
    override fun reset() {
        val appDir = File(context.cacheDir.parent)
        if (appDir.exists()) {
            appDir.list()
                    .filter { it != "lib" }
                    .forEach {
                        deleteDir(File(appDir, it))
                    }
            Process.killProcess(Process.myPid())
        }
    }

    /**
     * 设备重启
     *
     * Note：需要系统签名
     */
    override fun reboot() {
        context.reboot(null)
    }

    /**
     * 回到桌面
     */
    override fun quit() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        context.startActivity(intent)
    }

    /**
     * 静默安装
     *
     * Note：需要系统签名
     */
    override fun update(url: String?) {
        url?.let {
            APIService.downloadAsync(it, Environment.getExternalStorageDirectory().path, object : StringCallback {
                override fun onResponse(string: String) {
                    execRuntimeProcess("pm install -r $string");
                }

                override fun onFailure(request: Request, e: IOException) {
                    Logger.w(e.message)
                    Syslog.logE("Update file download failure")
                }
            })
        }
    }

    /**
     * 进入系统设置页
     */
    override fun setting() {
        val intent = Intent(Settings.ACTION_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
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