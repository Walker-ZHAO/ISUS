package net.ischool.isus.command

import android.content.Context
import android.content.Intent
import com.orhanobut.logger.Logger
import eu.chainfire.libsuperuser.Shell
import io.reactivex.schedulers.Schedulers
import net.ischool.isus.activity.ConfigActivity
import net.ischool.isus.network.APIService
import net.ischool.isus.network.callback.StringCallback
import okhttp3.Request
import org.jetbrains.anko.startActivity
import java.io.IOException
import android.support.v4.content.ContextCompat.startActivity
import android.provider.Settings;



/**
 * Description
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
                .subscribe()
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
        execRuntimeProcess("pm clear ${context.packageName}");
    }

    /**
     * 设备重启
     *
     * Note：需要设备已root，并且授予应用root权限
     */
    override fun reboot() {
        if (Shell.SU.available())
            Shell.SU.run("reboot")
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
     * Note：需要设备已root，并且授予应用root权限
     */
    override fun update(url: String?) {
        url?.let {
            APIService.downloadAsync(it, "/sdcard", object : StringCallback {
                override fun onResponse(string: String) {
                    if (Shell.SU.available())
                        Shell.SU.run("pm install -r $string")
                }

                override fun onFailure(request: Request, e: IOException) {
                    Logger.w(e.message)
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
}