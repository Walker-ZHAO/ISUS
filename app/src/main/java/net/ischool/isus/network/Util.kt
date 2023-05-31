package net.ischool.isus.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException

/**
 * 网络相关工具
 *
 * Author: walker
 * Email: zhaocework@gmail.com
 * Date: 2023/5/31
 */

fun getIpAddress(context: Context): String {
    val info =
        (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo
    if (info != null && info.isConnected) {
        when (info.type) {
            ConnectivityManager.TYPE_MOBILE -> { //当前使用2G/3G/4G网络
                try {
                    val en = NetworkInterface.getNetworkInterfaces()
                    while (en.hasMoreElements()) {
                        val intf = en.nextElement()
                        val enumIpAddr = intf.inetAddresses
                        while (enumIpAddr.hasMoreElements()) {
                            val inetAddress = enumIpAddr.nextElement()
                            if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                                return inetAddress.getHostAddress()
                            }
                        }
                    }
                } catch (e: SocketException) {
                    e.printStackTrace()
                }
            }
            ConnectivityManager.TYPE_WIFI -> { //当前使用无线网络
                val wifiManager =
                    context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val wifiInfo = wifiManager.connectionInfo
                return intIP2StringIP(wifiInfo.ipAddress)
            }
            ConnectivityManager.TYPE_ETHERNET -> {
                // 有线网络
                return getLocalIP()
            }
        }
    }
    return "0.0.0.0"
}

fun intIP2StringIP(ip: Int): String {
    return "${ip and 0xFF}.${ip shr 8 and 0xFF}.${ip shr 16 and 0xFF}.${ip shr 24 and 0xFF}"
}

fun getLocalIP(): String {
    try {
        val en = NetworkInterface.getNetworkInterfaces()
        while (en.hasMoreElements()) {
            val intf = en.nextElement()
            val enumIpAddr = intf.inetAddresses
            while (enumIpAddr.hasMoreElements()) {
                val inetAddress = enumIpAddr.nextElement()
                if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                    return inetAddress.getHostAddress()
                }
            }
        }
    } catch (_: SocketException) {
    }
    return "0.0.0.0"
}