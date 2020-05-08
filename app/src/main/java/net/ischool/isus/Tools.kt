package net.ischool.isus

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.UnknownHostException

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
            ipAddressArr.forEach { Log.i("Walker", it) }
            return@back if (ipAddressArr.isEmpty()) "" else ipAddressArr[0]
        } catch (e: UnknownHostException) {
            Log.e("Walker", "解析CDN IP失败: ${e.message}")
        }
        return@back ""
    }
}