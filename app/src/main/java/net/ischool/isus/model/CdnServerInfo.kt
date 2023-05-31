package net.ischool.isus.model

import com.google.gson.annotations.SerializedName

/**
 * 边缘云服务器信息
 *
 * Author: walker
 * Email: zhaocework@gmail.com
 * Date: 2023/5/30
 */
data class CdnServerInfo(
    val preset: String?,
    val version: String?,
    val domain: String?,
    val licence: String?,
    @SerializedName("school_id")val schoolId: String?,
    @SerializedName("cmdb_id") val cmdbId: String?,
    val storage: String?,
)