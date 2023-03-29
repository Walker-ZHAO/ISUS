package net.ischool.isus.model

/**
 * 扫码后的解析信息
 *
 * Author: walker
 * Email: zhaocework@gmail.com
 * Date: 2023/3/29
 */
data class QRInfo(
    // 学校ID
    val sid: Int,
    // 设备ID
    val cmdbid: Int,
    // 授权码
    val code: String,
    // 服务器地址
    val server: String,
    // 证书下载地址
    val certificate: String?
)