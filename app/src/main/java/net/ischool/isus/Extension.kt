package net.ischool.isus

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.lamy.display.screen.Screen
import android.os.Build
import com.hikvision.dmb.display.InfoDisplayApi
import com.hikvision.dmb.system.InfoSystemApi
import com.walker.anke.framework.reboot
import com.ys.rkapi.MyManager
import net.ischool.isus.preference.PreferenceManager
import java.lang.Exception

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
            InfoSystemApi.execCommand("su & rm -rf /dev/input/event2")
        }
        isTouchWoDevice() -> {
            MyManager.getInstance(this).turnOffBackLight()
            // 需要禁用触屏，否则触摸事件会导致背光重新开启
            execRuntimeProcess("su & rm -rf /dev/input/event1")
        }
        isDhDevice() -> {
            Screen.getScreen(0).turnOffBackLight()
            // 需要禁用触屏，否则触摸事件会下发至应用
            execRuntimeProcess("su & rm -rf /dev/input/event2")
        }
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
        isTouchWoDevice() -> {
            reboot(null)
        }
        isDhDevice() -> {
            reboot(null)
        }
    }
}

/**
 * 执行cmd命令
 */
private fun execRuntimeProcess(cmd: String): Process? {
    var p: Process? = null
    try {
        p = Runtime.getRuntime().exec(cmd)
        p?.waitFor()
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return p
}