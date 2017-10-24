package net.ischool.isus.network.callback

import okhttp3.Request
import java.io.IOException

/**
 * 字符串格式的回调
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2017/9/18
 */
interface StringCallback {
    fun onResponse(string: String)
    fun onFailure(request: Request, e: IOException)
}