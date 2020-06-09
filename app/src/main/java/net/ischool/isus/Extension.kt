package net.ischool.isus

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
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