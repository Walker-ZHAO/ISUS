package net.ischool.isus

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.lamy.display.screen.Screen
import android.os.Build
import com.hikvision.dmb.display.InfoDisplayApi
import com.hikvision.dmb.system.InfoSystemApi
import com.seewo.sdk.SDKSystemHelper
import com.seewo.udsservice.client.plugins.device.UDSDeviceHelper
import com.walker.anke.framework.reboot
import com.xbh.sdk3.Picture.PictureHelper
import com.xbh.sdk3.System.SystemHelper
import com.ys.rkapi.MyManager
import net.ischool.isus.preference.PreferenceManager
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStreamReader

/**
 * 扩展方法
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2020/5/26
 */

/**
 * 启动设备端的ZeroConf广播服务
 */
fun Context.startZXBS() {
    try {
        val PACKAGE_NAME = "net.ischool.zxbs"
        val SERVICE_NAME = "net.ischool.zxbs.MDNSService"
        val EXTRA_SCHOOL_ID = "school_id"
        val EXTRA_CMDB_ID = "cmdb_id"
        val EXTRA_VERSION_INFO = "version_info"
        val info = packageManager.getPackageInfo(packageName, 0)
        val label =
            if (info.applicationInfo!!.labelRes != 0) getString(info.applicationInfo!!.labelRes) else packageName
        val version = "${label}:v${info.versionName}"
        val intent = Intent().apply {
            component = ComponentName(PACKAGE_NAME, SERVICE_NAME)
            if (PreferenceManager.instance.getInitialized()) {
                putExtra(EXTRA_SCHOOL_ID, PreferenceManager.instance.getSchoolId())
                putExtra(EXTRA_CMDB_ID, PreferenceManager.instance.getCMDB())
                putExtra(EXTRA_VERSION_INFO, version)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.startForegroundService(intent)
        } else {
            applicationContext.startService(intent)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * 休眠
 *
 * 息屏 + 禁触屏
 */
fun Context.sleep() {
    when {
        isHikDevice() -> {
            InfoDisplayApi.disableBacklight()
            // 需要禁用触屏，否则触摸事件会下发至应用
            val device = getSystemProperty("sys.hik.dev_type")
            // 根据不同设备型号，删除不同触屏挂载点
            if (device.contains("DS-D6122TH-B/I")) {
                InfoSystemApi.execCommand("su & rm -rf /dev/input/event2")
                // 使CPU进入节能模式
                InfoSystemApi.execCommand("su & echo powersave > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor")
            } else if (device.contains("DS-D6122TH-B/C")) {
                InfoSystemApi.execCommand("su & rm -rf /dev/input/event4")
                // 使CPU进入节能模式
                InfoSystemApi.execCommand("su & echo powersave > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor")
            } else if (device.contains("DS-D6122TL-B/C")) {
                InfoSystemApi.execCommand("su & rm -rf /dev/input/event3")
            } else if (device.contains("DS-D6122TL-D/C")) {
                InfoSystemApi.execCommand("su & rm -rf /dev/input/event1")
                // 使CPU进入节能模式
                InfoSystemApi.execCommand("su & echo powersave > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor")
            }
        }

        isTouchWoDevice() || isDh32Device() -> {
            // 关闭屏幕背光
            MyManager.getInstance(this).turnOffBackLight()
            // 兼容 3368 设备的背光关闭
            MyManager.getInstance(this).execSuCmd("echo 0 > /sys/devices/fb.11/graphics/fb0/pwr_bl")

            // 使CPU进入节能模式
            MyManager.getInstance(this)
                .execSuCmd("echo powersave > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor")

            val model = MyManager.getInstance(this).androidModle
            if (model.contains("rk3568") || isDh32Device()) {
                // 3568_r 或 大华32寸，关闭背光后，无法通过触屏唤醒屏幕，因此不需要删除设备节点
                // 3568_r 下删除设备节点，会导致应用直接被系统 kill 掉
                return
            }

            // 需要禁用触屏，否则触摸事件会导致背光重新开启
            MyManager.getInstance(this).execSuCmd("rm -rf /dev/input/event1")
            // 兼容触沃5.1的3288设备
            MyManager.getInstance(this).execSuCmd("rm -rf /dev/input/event4")
            MyManager.getInstance(this).execSuCmd("rm -rf /dev/input/event2")
            // 兼容触沃5.1的3368设备
            MyManager.getInstance(this).execSuCmd("rm -rf /dev/input/event3")
        }

        isDhDevice() -> {
            Screen.getScreen(0).turnOffBackLight()
            // 需要禁用触屏，否则触摸事件会下发至应用
            execRuntimeProcess("su & rm -rf /dev/input/event2")
            // 使CPU进入节能模式
            execRuntimeProcess(
                "echo powersave > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor",
                needEvn = true
            )
        }

        isHongHeDevice() -> {
            PictureHelper().gotoSleep()
            SystemHelper().apply {
                // 需要禁用触屏，否则触摸事件会下发至应用
                executeCommand("su & rm -rf /dev/input/event2")
                // 使CPU进入节能模式
                executeCommand("su & echo powersave > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor")
            }
        }

        isSeeWoDevice() -> {
            UDSDeviceHelper().setScreenStatus(false)
            // 需要禁用触屏，否则触摸事件会下发至应用
            SDKSystemHelper.I.executeCommandWithRoot("rm -rf /dev/input/event5")
            // 希沃设备无法进入节能模式
            SDKSystemHelper.I.executeCommandWithRoot("echo powersave > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor")
        }
    }
}

/**
 * 是否处于休眠模式
 */
fun Context.inSleep(): Boolean {
    return when {
        isHikDevice() -> {
            execRuntimeProcess("cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor").contains(
                "powersave"
            )
        }

        isTouchWoDevice() || isDh32Device() -> {
            // 兼容 3368 设备
            execRuntimeProcess("cat /sys/devices/fb.11/graphics/fb0/pwr_bl").contains(
                "0"
            ) || !MyManager.getInstance(this).isBacklightOn
        }

        isDhDevice() -> {
            execRuntimeProcess("cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor").contains(
                "powersave"
            )
        }

        isHongHeDevice() -> {
            SystemHelper().executeCommand("cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor")
                .contains("powersave")
        }

        isSeeWoDevice() -> {
            !UDSDeviceHelper().isScreenOn
        }

        else -> false
    }
}

/**
 * 唤醒
 *
 * 重启
 */
fun Context.wakeup() {
    when {
        isHikDevice() -> {
            InfoSystemApi.reboot()
        }

        isTouchWoDevice() || isDh32Device() -> {
            MyManager.getInstance(this).reboot()
        }

        isDhDevice() -> {
            reboot(null)
        }

        isHongHeDevice() -> {
            SystemHelper().reboot()
        }

        isSeeWoDevice() -> {
            SDKSystemHelper.I.reboot()
        }
    }
}

/**
 * 执行cmd命令
 */
fun execRuntimeProcess(cmd: String, needEvn: Boolean = false): String {
    val p: Process?
    val output = StringBuilder()
    try {
        if (!needEvn) {
            p = Runtime.getRuntime().exec(cmd)
        } else {
            p = Runtime.getRuntime().exec("sh")
            DataOutputStream(p.outputStream).use {
                it.writeBytes("$cmd\n")
                it.flush()
            }
        }
        p?.waitFor()
        val reader = BufferedReader(InputStreamReader(p.inputStream))
        var line: String?
        reader.use {
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return output.toString()
}

/**
 * 是否处于64位运行模式
 */
fun isArch64(): Boolean {
    return System.getProperty("os.arch")?.contains("64") ?: false
}

/**
 * 静默安装 APK
 * 该方法需要系统签名权限
 * @param apkFile APK 文件
 * @param callback 安装结果回调
 */
fun Context.silentInstallApk(apkFile: File, callback: (Boolean, String) -> Unit) {
    if (!apkFile.exists()) {
        callback(false, "APK 文件不存在")
        return
    }

    try {
        val packageInstaller = packageManager.packageInstaller

        // 创建安装会话
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setInstallLocation(PackageInfo.INSTALL_LOCATION_AUTO)
        }

        // 获取会话ID
        val sessionId = packageInstaller.createSession(params)
        val session = packageInstaller.openSession(sessionId)

        // 写入APK文件
        session.openWrite("package", 0, apkFile.length()).use { outputStream ->
            apkFile.inputStream().use { inputStream ->
                inputStream.copyTo(outputStream)
                session.fsync(outputStream)
            }
        }

        // 注册安装广播接收器
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
                    PackageInstaller.STATUS_SUCCESS -> {
                        callback(true, "安装成功")
                    }
                    PackageInstaller.STATUS_FAILURE,
                    PackageInstaller.STATUS_FAILURE_ABORTED -> {
                        val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "安装失败"
                        callback(false, msg)
                    }
                }
                context?.unregisterReceiver(this)
            }
        }

        // 注册广播
        val intentFilter = IntentFilter(INSTALL_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(receiver, intentFilter)
        }

        // 创建 PendingIntent
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            sessionId,
            Intent(INSTALL_ACTION),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            else PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 提交安装
        session.commit(pendingIntent.intentSender)
        session.close()

    } catch (e: Exception) {
        callback(false, "安装失败：${e.message}")
    }
}

/**
 * 用于接收应用安装结果的广播Action
 */
private const val INSTALL_ACTION = "net.ischool.isus.INSTALL_COMPLETE"

/**
 * 获取系统属性
 * @param propName
 * @return
 */
private fun getSystemProperty(propName: String): String {
    return try {
        val process = Runtime.getRuntime().exec("getprop $propName")
        val input = BufferedReader(InputStreamReader(process.inputStream), 1024)
        val line = input.readLine()
        input.close()
        line
    } catch (e: IOException) {
        e.printStackTrace()
        ""
    }
}