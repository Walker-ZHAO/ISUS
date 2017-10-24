package net.ischool.isus.model

/**
 * 网络Model映射
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2017/9/13
 */
data class Result<out T>(val errno: Int, val error: String, val ts: Long, val data: T, val list: List<T>)