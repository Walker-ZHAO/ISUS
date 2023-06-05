package net.ischool.isus.model

import com.google.gson.annotations.SerializedName

/**
 * 报警信息
 *
 * Author: walker
 * Email: zhaocework@gmail.com
 * Date: 2023/5/30
 */
data class AlarmInfo(
    // 报警类型，参考 ALARM_TYPE_*
    @SerializedName("element_id") val type: Int,
    // 报警时间戳，单位秒
    val ts: Long,
    // 报警原因
    @SerializedName("title") val reason: String,
    // 快速诊断
    @SerializedName("content") val diag: String,
    // 联系人
    @SerializedName("alert_contact") val contact: String,
) {
    // 展示优先级
    val priority: Int
        get() =
            when (type) {
                ALARM_TYPE_DISCONNECT -> 0
                ALARM_TYPE_NOT_MATCH -> 1
                ALARM_TYPE_MQ -> 2
                ALARM_TYPE_PLATFORM -> 3
                ALARM_TYPE_CAMPUSNG -> 4
                ALARM_TYPE_GATE -> 5
                ALARM_TYPE_UPGRADE -> 6
                else -> 9999
            }
}

// 设备无法连接边缘云
const val ALARM_TYPE_DISCONNECT = 30000001
// 边缘云版本
const val ALARM_TYPE_UPGRADE = 30000002
// 请求cmdbid和ip不匹配
const val ALARM_TYPE_NOT_MATCH = 30000003
// 边缘云到平台mq队列
const val ALARM_TYPE_MQ = 110211
// 边缘云到平台curl连通性
const val ALARM_TYPE_PLATFORM = 110005
// campusng服务
const val ALARM_TYPE_CAMPUSNG = 110107
// 闸机
const val ALARM_TYPE_GATE = 10006