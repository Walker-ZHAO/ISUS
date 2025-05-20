package net.ischool.isus.io

import com.google.gson.annotations.SerializedName

/**
 * BLE设备信息
 *
 * 可供已连接的外围设备进行数据读取
 *
 * Author: walker
 * Email: zhaocework@gmail.com
 * Date: 2025/4/28
 */
data class BLEDeviceInfo(
    // 设备名称
    @SerializedName("dn")
    val deviceName: String,
    // 学校 ID
    @SerializedName("si")
    val schoolId: String,
    // 学校名称
    @SerializedName("sn")
    val schoolName: String,
    // 班级 ID
    @SerializedName("ci")
    val classId: String,
    // 班级名称
    @SerializedName("cn")
    val className: String,
    // 设备 ID
    @SerializedName("cmdb")
    val cmdbId: String,
    // 设备 IP
    val ip: String,
    // 设备的自诊断状态
    @SerializedName("diag")
    val diagState: Int,
    // 设备是否处于休眠中
    @SerializedName("sleep")
    val isSleep: Boolean,
)