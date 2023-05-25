package net.ischool.isus.model

import com.google.gson.annotations.SerializedName

/**
 * 学校信息
 *
 * 对应/platform/campusng/equipment/getSchool
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2018/9/6
 */
data class SchoolInfo(@SerializedName(value = "school_id", alternate = ["schoolid"])val schoolId: Int,
                      @SerializedName(value = "school_name", alternate = ["schoolname"]) val schoolName: String,
                      @SerializedName(value = "client_ip", alternate = ["clientip"]) val clientIp: String,
                      @SerializedName(value = "https_api") val httpsApi: String,
                      @SerializedName(value = "http_api") val httpApi: String,)