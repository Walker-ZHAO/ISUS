package net.ischool.isus

import android.Manifest
import android.app.Activity
import android.content.Context
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import com.tbruyelle.rxpermissions2.RxPermissions
import net.ischool.isus.command.CommandParser
import net.ischool.isus.db.ObjectBox
import net.ischool.isus.network.APIService
import net.ischool.isus.preference.PreferenceManager
import net.ischool.isus.service.ISUSService
import net.ischool.isus.service.StatusPostService
import org.jetbrains.anko.toast

/**
 * 库入口类
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2017/9/18
 */
class ISUS(val context: Context, val domain: String, val se: Boolean) {

    companion object {

        @Volatile
        lateinit var instance: ISUS
            private set

        /**
         * 初始化
         */
        @JvmOverloads
        @Synchronized
        @JvmStatic fun init(context: Context, deviceType: Int, domain: String = DEFAULT_DOMAIN, securityEnhance: Boolean = false) {
            instance = ISUS(context.applicationContext, domain, securityEnhance)
            Logger.addLogAdapter(AndroidLogAdapter())
            PreferenceManager.initPreference(context, deviceType)
            ObjectBox.init(context)
            if (context is Activity) {
                val rxPermission = RxPermissions(context)
                val disposable = rxPermission.request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        .subscribe {
                            if (!it) {
                                context.toast(context.getString(R.string.get_permission_fail))
                            }
                        }
            }
            // 已初始化的设备，非SE模式下，启动状态上报服务
            if (PreferenceManager.instance.getInitialized() && !securityEnhance)
                StatusPostService.startService(context)
        }
    }

    /**
     * 开启统一推送服务
     */
    fun startService() {
        ISUSService.start(context)
    }

    /**
     * 停止统一推送服务
     */
    fun stopService() {
        ISUSService.stop(context)
    }

    /**
     * 销毁相关资源
     */
    fun destroy() {
        ObjectBox.destroy()
        stopService()
        APIService.cancel()
    }
}