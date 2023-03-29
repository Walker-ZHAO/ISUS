package net.ischool.isus.scheme

import java.io.UnsupportedEncodingException
import java.net.URLDecoder

/**
 * 协议相关扩展方法
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2021/1/7
 */

/**
 * 判断字符串是否是ischool协议
 *
 * @return true:是；false：否
 */
fun String.isCustomScheme() = isNotEmpty() && startsWith(ISCHOOL_SCHEME)

/**
 * 判断字符串是否是ischool Function协议
 *
 * @return true：是；false：否
 */
fun String.isFunction() = isNotEmpty() && startsWith("$ISCHOOL_SCHEME$ISCHOOL_SCHEME_FUNCTION/")

/**
 * 将字符串进行UTF-8解码
 *
 * @return 解码后的字符串
 */
fun String.urlDecode(): String {
    try {
        return URLDecoder.decode(this, "utf-8")
    } catch (e: UnsupportedEncodingException) {
        e.printStackTrace()
    }
    return this
}

/**
 * 将字符串按协议规范解析成键值对
 *
 * @return 解析后的键值对
 */
fun String.urlParamsToMap(): Map<String, String> {
    val kvs = split("&")
    val mapArgs = mutableMapOf<String, String>()
    kvs.forEach {
        val index = it.indexOf('=')
        val key = it.substring(0, index)
        val value = it.substring(index + 1)
        mapArgs[key] = value.urlDecode()
    }
    return mapArgs
}

// H5 Scheme相关
const val ISCHOOL_SCHEME = "ischool://"
const val ISCHOOL_SCHEME_FUNCTION = "function"