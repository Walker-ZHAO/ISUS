package net.ischool.isus.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException

/**
 * 网络工具
 *
 * Author: walker
 * Email: zhaocework@gmail.com
 * Date: 2024/12/18
 */
object NetworkUtil {
    private const val DEFAULT_ADDRESS = "0.0.0.0"

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
                                    return inetAddress.getHostAddress() ?: DEFAULT_ADDRESS
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
        return DEFAULT_ADDRESS
    }

    private fun intIP2StringIP(ip: Int): String {
        return "${ip and 0xFF}.${(ip shr 8) and 0xFF}.${(ip shr 16) and 0xFF}.${(ip shr 24) and 0xFF}"
    }

    private fun getLocalIP(): String {
        try {
            val en = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val intf = en.nextElement()
                val enumIpAddr = intf.inetAddresses
                while (enumIpAddr.hasMoreElements()) {
                    val inetAddress = enumIpAddr.nextElement()
                    if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                        return inetAddress.getHostAddress() ?: DEFAULT_ADDRESS
                    }
                }
            }
        } catch (_: SocketException) {
        }
        return DEFAULT_ADDRESS
    }
}