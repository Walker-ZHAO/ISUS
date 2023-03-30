package net.ischool.isus

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import com.tbruyelle.rxpermissions2.RxPermissions
import com.walker.anke.framework.toast
import net.ischool.isus.command.CommandParser
import net.ischool.isus.command.ICommand
import net.ischool.isus.db.ObjectBox
import net.ischool.isus.network.APIService
import net.ischool.isus.preference.PreferenceManager
import net.ischool.isus.service.ISUSService
import net.ischool.isus.service.StatusPostService
import net.ischool.isus.service.UDPService
import net.ischool.isus.service.WatchDogService

/**
 * 库入口类
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2017/9/18
 */
class ISUS(val context: Context, val domain: String, val se: Boolean) {

    // 服务器访问地址
    var apiHost = ""

    companion object {

        @Volatile
        lateinit var instance: ISUS
            private set

        /**
         * 初始化
         */
        @SuppressLint("CheckResult")
        @ExperimentalStdlibApi
        @JvmOverloads
        @Synchronized
        @JvmStatic fun init(context: Context, deviceType: Int, domain: String = DEFAULT_DOMAIN, securityEnhance: Boolean = false, commandProcessor: ICommand? = null) {
            instance = ISUS(context.applicationContext, domain, securityEnhance)
            CommandParser.init(commandProcessor)
            Logger.addLogAdapter(AndroidLogAdapter())
            PreferenceManager.initPreference(context, deviceType)
            ObjectBox.init(context)
            if (context is Activity) {
                val rxPermission = RxPermissions(context)
                rxPermission.request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        .subscribe {
                            if (!it) {
                                context.toast(context.getString(R.string.get_permission_fail))
                            }
                        }
            }
            // 已初始化的设备，非SE模式下，启动状态上报服务
            if (PreferenceManager.instance.getInitialized() && !securityEnhance)
                StatusPostService.startService(context)

            // 启动ZeroConf广播服务
            context.startZXBS()

            // 启动UDP监听
            UDPService.start()
        }
    }

    /**
     * 开启统一推送服务
     */
    fun startService() {
        ISUSService.start(context)
        // 启动网络监控服务
        WatchDogService.start(context)
    }

    /**
     * 停止统一推送服务
     */
    fun stopService() {
        // 关闭网络监控服务
        WatchDogService.stop(context)
        ISUSService.stop(context)
    }

    /**
     * 销毁相关资源
     */
    fun destroy() {
        ObjectBox.destroy()
        stopService()
        UDPService.stop()
        APIService.cancel()
    }
}