package net.ischool.isus

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import com.tbruyelle.rxpermissions3.RxPermissions
import com.walker.anke.framework.packageVersionName
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
import java.io.InputStream

/**
 * 库入口类
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2017/9/18
 */
class ISUS(
    val context: Context,
    val se: Boolean,
    val iam: String,
    // 服务端的自签CA证书
    val certificate: InputStream?,
    // 客户端证书，仅支持P12格式
    val clientCert: InputStream?,
    val clientCertPwd: String?,
) {

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
        @JvmStatic
        fun init(
            context: Context,
            deviceType: Int,
            minCdnVersion: String,
            securityEnhance: Boolean = false,
            iam: String = "",
            commandProcessor: ICommand? = null,
            certificate: InputStream? = null,
            clientCert: InputStream? = null,
            clientCertPwd: String? = null,
        ) {
            instance = ISUS(context.applicationContext, securityEnhance, iam, certificate, clientCert, clientCertPwd)
            CommandParser.init(commandProcessor)
            Logger.addLogAdapter(AndroidLogAdapter())
            PreferenceManager.initPreference(context, deviceType)
            PreferenceManager.instance.setMinCdnVersion(minCdnVersion)
            ObjectBox.init(context)
            if (context is FragmentActivity) {
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


    /**
     * 获取自定义UA
     */
    fun getUserAgent(): String {
        val deviceFields = "device/${PreferenceManager.instance.getDeviceType()}"
        val cardFields = "card/${PreferenceManager.instance.getCReaderType()}"
        val gateFields = "gate/${PreferenceManager.instance.getEntranceGuardType()}"
        val versionFields = "version/${instance.context.applicationContext.packageVersionName}"
        val seFields = "se/${instance.se}"
        val schoolFields = "school/${PreferenceManager.instance.getSchoolId()}"
        val cmdbFields = "cmdb/${PreferenceManager.instance.getCMDB()}"
        val classFields = "class/${PreferenceManager.instance.getClassId()}"
        return "iSchoolHTTP/1.0 $deviceFields $cardFields $gateFields $seFields $versionFields $schoolFields $cmdbFields $classFields"
    }
}