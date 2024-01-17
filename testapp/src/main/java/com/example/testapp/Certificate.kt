package com.example.testapp

import android.content.Context
import java.io.InputStream
import java.security.MessageDigest

/**
 * SSL认证
 *
 * Author: walker
 * Email: zhaocework@gmail.com
 * Date: 2024/1/15
 */
const val trustCacheCAPath = "cert"                 // 服务端可信CA目录，用于缓存中间CA证书
const val trustCAFileName = "ischool-ca.cert.pem"   // 服务端可信根CA文件
const val clientCertFileName = "client.p12"     // 客户端证书文件
const val clientCertPassword = "i-school.net"   // 客户端证书文件密码

/**
 * 获取服务端CA证书流
 */
fun Context.getCertStream(): InputStream {
    return assets.open(trustCAFileName)
}

/**
 * 获取客户端证书
 */
fun Context.getClientCert(): Pair<String, InputStream> {
    return calculateMD5(clientCertPassword) to assets.open(clientCertFileName)
}

fun calculateMD5(input: String): String {
    try {
        // 创建 MessageDigest 实例，指定使用 MD5 算法
        val md = MessageDigest.getInstance("MD5")

        // 将输入的字符串转换为字节数组
        val byteInput = input.toByteArray()

        // 对字节数组进行 MD5 计算
        val byteHash = md.digest(byteInput)

        // 将计算结果转换为十六进制字符串
        val sb = StringBuilder()
        for (b in byteHash) {
            sb.append(String.format("%02x", b))
        }

        return sb.toString()
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return ""
}