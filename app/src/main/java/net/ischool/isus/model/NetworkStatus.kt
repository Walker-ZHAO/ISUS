package net.ischool.isus.model

/**
 * 服务器返回的网络状态
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2020/6/24
 */
data class NetworkStatus(val cdn: String, val protocal: String, val status: Int, val sids: List<String>)