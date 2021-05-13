package net.ischool.isus

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.hikvision.dmb.system.InfoSystemApi
import com.seewo.sdk.OpenSDK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.*

/**
 * 工具类
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2020/5/8
 */

val DEFAULT_HOST by lazy { "${if (ISUS.instance.se) "cc" else "cdn.schools"}.${ISUS.instance.domain}" }

/**
 * 解析域名对应IP
 */
suspend fun parseHostGetIPAddress(host: String = DEFAULT_HOST): String {
    return withContext(Dispatchers.IO) back@{
        try {
            val ipAddressArr = mutableListOf<String>()
            InetAddress.getAllByName(host)?.forEach {
                ipAddressArr.add(it.hostAddress)
            }
            ipAddressArr.forEach { Log.i(LOG_TAG, it) }
            return@back if (ipAddressArr.isEmpty()) "" else ipAddressArr[0]
        } catch (e: UnknownHostException) {
            Log.e(LOG_TAG, "解析CDN IP失败: ${e.message}")
        }
        return@back ""
    }
}

/**
 * 判断是否是海康设备
 */
fun isHikDevice(): Boolean {
    try {   // 通过使用海康SDK获取主板信息判断是否为海康设备
        InfoSystemApi.getMotherboardType()
        return true
    } catch (e: Throwable) { }
    return false
}

/**
 * 判断是否是希沃设备
 */
fun isSeeWoDevice(): Boolean {
    try {
        if (!OpenSDK.getInstance().isConnected)
            OpenSDK.getInstance().connect(ISUS.instance.context)
        return OpenSDK.getInstance().isConnected
    } catch (e: Throwable) { }
    return false
}

/**
 * 判断是否是触沃设备
 */
fun isTouchWoDevice(): Boolean {
    val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
    val info = ISUS.instance.context.packageManager.queryIntentActivities(intent, 0).find { app ->
        app.activityInfo.packageName.contains("adtv")
    }
    return info != null
}

/**
 * 获取设备ID
 */
@SuppressLint("HardwareIds")
fun getDeviceID(): String = Settings.Secure.getString(
    ISUS.instance.context.contentResolver,
    Settings.Secure.ANDROID_ID
).toUpperCase(Locale.getDefault())

/**
 * 获取设备信息
 */
fun getDeviceInfo(): String = "${Build.PRODUCT}-${Build.TYPE} ${Build.VERSION.RELEASE} ${Build.ID} ${Build.VERSION.INCREMENTAL} ${Build.TAGS}"