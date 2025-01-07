package net.ischool.isus

import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
import net.ischool.isus.activity.BlackHoleActivity
import net.ischool.isus.preference.PreferenceManager
import java.io.BufferedReader
import java.io.DataOutputStream
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
        val label = if (info.applicationInfo.labelRes != 0) getString(info.applicationInfo.labelRes) else packageName
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
            MyManager.getInstance(this).turnOffBackLight()
            // 需要禁用触屏，否则触摸事件会导致背光重新开启
            execRuntimeProcess("su & rm -rf /dev/input/event1")
            // 兼容触沃5.1的3288设备
            execRuntimeProcess("su & rm -rf /dev/input/event2")
            execRuntimeProcess("su & rm -rf /dev/input/event4")
            // 兼容触沃5.1的3368设备
            execRuntimeProcess("su & rm -rf /dev/input/event3")
            // 使CPU进入节能模式
            execRuntimeProcess("su & echo powersave > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor")
        }
        isDhDevice() -> {
            Screen.getScreen(0).turnOffBackLight()
            // 需要禁用触屏，否则触摸事件会下发至应用
            execRuntimeProcess("su & rm -rf /dev/input/event2")
            // 使CPU进入节能模式
            execRuntimeProcess("echo powersave > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor", needEvn = true)
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
            // 希沃设备不支持删除触屏设备节点，使用其他实现方式
            val intent = Intent(this, BlackHoleActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            startActivity(intent)
        }
    }
}

/**
 * 是否处于休眠模式
 */
fun Context.inSleep(): Boolean {
    return when {
        isHikDevice() -> {
            execRuntimeProcess("cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor").contains("powersave")
        }
        isTouchWoDevice() || isDh32Device() -> {
            !MyManager.getInstance(this).isBacklightOn
        }
        isDhDevice() -> {
            execRuntimeProcess("cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor").contains("powersave")
        }
        isHongHeDevice() -> {
            SystemHelper().executeCommand("cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor").contains("powersave")
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
            reboot(null)
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