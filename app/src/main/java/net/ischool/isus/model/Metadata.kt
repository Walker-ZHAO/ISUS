package net.ischool.isus.model

import com.google.gson.annotations.SerializedName

/**
 * 元数据
 *
 * 对应/eqptapi/init
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2017/9/13
 */
data class Metadata(
    val token: String,
    val APIServer: String,
    val protocal: String,
    @SerializedName(value = "PlatformAPI") val platformApi: String,
    @SerializedName(value = "PlatformMQ") val platformMq: String,
)