package net.ischool.isus.command

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.os.Process
import android.provider.Settings
import com.hikvision.dmb.system.InfoSystemApi
import com.hikvision.dmb.util.InfoUtilApi
import com.orhanobut.logger.Logger
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import net.ischool.isus.activity.ConfigActivity
import net.ischool.isus.log.Syslog
import net.ischool.isus.network.APIService
import net.ischool.isus.network.callback.StringCallback
import okhttp3.Request
import java.io.File
import java.io.IOException

/**
 * 海康威视设备的命令处理实现
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2020/6/16
 */
class HikVisionCommandImpl(private val context: Context): ICommand {

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
        val intent = Intent(context, ConfigActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
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
     */
    override fun reboot() {
        InfoSystemApi.reboot()
    }

    /**
     * 回到桌面
     */
    override fun quit() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        }
        context.startActivity(intent)
    }

    /**
     * 静默安装
     */
    override fun update(url: String?) {
        url?.let {
            APIService.downloadAsync(it, Environment.getExternalStorageDirectory().path, callback = object :
                StringCallback {
                override fun onResponse(string: String) {
                    InfoSystemApi.execCommand("pm install -r $string")
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
        val intent = Intent(Settings.ACTION_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(intent)
    }

    /**
     * 命令方式进入指定页面
     */
    override fun launchPage(component: String?) {
        component?.let {
            val config = it.split("/")
            if (config.size > 1)
                InfoUtilApi.startUp(config[0], config[1])
        }
    }

    /**
     * 从当前页返回
     */
    override fun backPage() {
        InfoSystemApi.execCommand("input keyevent BACK")
    }

    /**
     * 开启ADB
     */
    override fun openAdb() {
        InfoSystemApi.openAdb()
    }

    /**
     * 更新应用配置信息
     */
    override fun reload() {
        val disposable  = APIService.getConfig()
            .subscribeOn(Schedulers.io())
            .subscribeBy(
                onNext = { reboot() },
                onError = { Syslog.logE("Reload config failure: ${it.message}") }
            )
    }

    /**
     * 从路径下获取文件名称
     */
    private fun getFileName(path: String): String {
        val separatorIndex = path.lastIndexOf("/")
        return if (separatorIndex < 0) path else path.substring(separatorIndex + 1, path.length)
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