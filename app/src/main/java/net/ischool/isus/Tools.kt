package net.ischool.isus

import android.annotation.SuppressLint
import android.app.smdt.SmdtManager
import android.app.smdt.SmdtManagerNew
import android.content.Intent
import android.lamy.system.Magicbox
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.hikvision.dmb.system.InfoSystemApi
import com.seewo.sdk.OpenSDK
import com.seewo.udsservice.client.plugins.system.UDSSystemHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ischool.isus.preference.PreferenceManager
import java.lang.reflect.Method
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

val DEFAULT_HOST: String by lazy { Uri.parse(PreferenceManager.instance.getPlatformApi()).host ?: "" }

/**
 * 解析域名对应IP
 */
suspend fun parseHostGetIPAddress(host: String = DEFAULT_HOST): String {
    return withContext(Dispatchers.IO) back@{
        try {
            val ipAddressArr = mutableListOf<String>()
            InetAddress.getAllByName(host)?.forEach {
                ipAddressArr.add(it.hostAddress ?: "")
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
        InfoSystemApi.getMotherboardType() ?: throw Exception()
        return true
    } catch (e: Throwable) { }
    return false
}

/**
 * 是否是希沃设备
 */
fun isSeeWoDevice(): Boolean {
    return isSeeWo03Device() || isSeeWo06Device();
}

/**
 * 是否是希沃SK03设备
 */
fun isSeeWo03Device(): Boolean {
    try {
        if (!OpenSDK.getInstance().isConnected)
            OpenSDK.getInstance().connect(ISUS.instance.context)
        return OpenSDK.getInstance().isConnected
    } catch (e: Throwable) { e.printStackTrace() }
    return false
}

/**
 * 是否是希沃SK06/SK07设备
 */
fun isSeeWo06Device(): Boolean {
    try {
        val systemHelper = UDSSystemHelper()
        val serialCode = systemHelper.serialCode
        systemHelper.release()
        return serialCode != null
    } catch (e: Throwable) { e.printStackTrace() }
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
 * 是否是明博华瑞安设备
 */
@SuppressLint("QueryPermissionsNeeded")
fun isMingboDevice(): Boolean {
    val intent = Intent("com.hra.setAutoShutdown")
    val info = ISUS.instance.context.packageManager.queryBroadcastReceivers(intent, 0)
    return info != null
}

/**
 * 是否是大华设备
 */
fun isDhDevice(): Boolean {
    return try {
        Magicbox.getApiVersion()
        true
    } catch (e: Throwable) {
        false
    }
}

/**
 * 是否是大华32寸定制设备
 */
fun isDh32Device(): Boolean {
    return try {
        SmdtManager.create(ISUS.instance.context).smdtGetAPIVersion()
        true
    } catch (e: Throwable) {
        false
    }
}

/**
 * 是否是视美泰设备
 */
fun isSmtDevice(): Boolean {
    return try {
        !SmdtManagerNew.getInstance(ISUS.instance.context).info_getApiVersion().isNullOrEmpty()
    } catch (e: Throwable) {
        false
    }
}

/**
 * 判断是否是鸿合设备
 */
fun isHongHeDevice(): Boolean {
    return getProperty("ro.product.customer.model", "").contains("HONGHE")
}

fun getProperty(key: String, defaultValue: String): String {
    var value = defaultValue
    try {
        val c = Class.forName("android.os.SystemProperties")
        val get: Method = c.getMethod("get", String::class.java, String::class.java)
        value = get.invoke(c, key, "0") as String
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return value
}

/**
 * 获取设备ID
 */
@SuppressLint("HardwareIds")
fun getDeviceID(): String = Settings.Secure.getString(
    ISUS.instance.context.contentResolver,
    Settings.Secure.ANDROID_ID
).uppercase(Locale.getDefault())

/**
 * 获取设备信息
 */
fun getDeviceInfo(): String = "${Build.PRODUCT}-${Build.TYPE} ${Build.VERSION.RELEASE} ${Build.ID} ${Build.VERSION.INCREMENTAL} ${Build.TAGS}"