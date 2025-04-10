package net.ischool.isus

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.fragment.app.FragmentActivity
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import com.seewo.udsservice.client.core.UDSCallback
import com.seewo.udsservice.client.core.UDSSDK
import com.tbruyelle.rxpermissions3.RxPermissions
import com.walker.anke.framework.packageVersionName
import com.walker.anke.framework.toast
import com.ys.rkapi.MyManager
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import net.ischool.isus.broadcast.USBReceiver
import net.ischool.isus.command.CommandParser
import net.ischool.isus.command.ICommand
import net.ischool.isus.db.ObjectBox
import net.ischool.isus.io.IBeaconAdvertiser
import net.ischool.isus.network.APIService
import net.ischool.isus.preference.PreferenceManager
import net.ischool.isus.service.AlarmService
import net.ischool.isus.service.RabbitMQService
import net.ischool.isus.service.SSEService
import net.ischool.isus.service.StatusPostService
import net.ischool.isus.service.UDPService
import net.ischool.isus.service.WatchDogService
import net.ischool.isus.util.setupCustomIME
import java.io.InputStream
import java.util.concurrent.TimeUnit

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

        private val usbReceiver = USBReceiver()

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
            instance = ISUS(context, securityEnhance, iam, certificate, clientCert, clientCertPwd)
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

            try {
                // 初始化希沃 SDK
                UDSSDK.INSTANCE.init(context, object : UDSCallback(true) {
                    override fun onConnectCompleted() { }
                })

                // 初始化触沃 SDK
                MyManager.getInstance(context).bindAIDLService(context)
            } catch (e: Throwable) { e.printStackTrace() }

        }
    }

    /**
     * 开启统一推送服务
     */
    @SuppressLint("CheckResult")
    fun startService() {
        // 安全增强模式下直连公网，依然使用RabbitMQ做消息推送
        if (instance.se) {
            RabbitMQService.start(context.applicationContext)
        } else {
            // 与边缘云链接模式下，使用EventSource(SSE)做消息推送
            SSEService.start(context.applicationContext)
        }
        // 启动网络监控服务
        WatchDogService.start(context.applicationContext)
        // 监听U盘插拔事件
        context.applicationContext.registerReceiver(usbReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_MEDIA_MOUNTED)
            addAction(Intent.ACTION_MEDIA_UNMOUNTED)
            addAction(Intent.ACTION_MEDIA_EJECT)
            addDataScheme("file")
        })
        // 触发定期状态检测
        AlarmService.alarmInfos
        // 初始化iBeacon配置
        IBeaconAdvertiser.init(context)
        // 如果启用iBeacon，则进行iBeacon广播
        if (PreferenceManager.instance.getIBeacon()) {
            // 延迟广播
            Observable.timer(10, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { IBeaconAdvertiser.instance.startAdvertise(context) }
        }
        // 设置自定义输入法
        context.setupCustomIME()
    }

    /**
     * 停止统一推送服务
     */
    fun stopService() {
        // 关闭网络监控服务
        WatchDogService.stop(context)
        if (instance.se) {
            RabbitMQService.stop(context)
        } else {
            SSEService.stop(context)
        }
        try {
            context.unregisterReceiver(usbReceiver)
        } catch (e: Exception) { e.printStackTrace() }

    }

    /**
     * 销毁相关资源
     */
    fun destroy() {
        ObjectBox.destroy()
        stopService()
        UDPService.stop()
        APIService.cancel()
        try {
            MyManager.getInstance(context).unBindAIDLService(context)
        } catch (e: Error) { e.printStackTrace() }
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