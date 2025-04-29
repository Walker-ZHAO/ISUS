package net.ischool.isus.io

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
    val deviceName: String,
    // 学校 ID
    val schoolId: String,
    // 学校名称
    val schoolName: String,
    // 班级 ID
    val classId: String,
    // 班级名称
    val className: String,
    // 设备 ID
    val cmdbId: String,
    // 设备 IP
    val ip: String,
    // 设备的自诊断状态
    val diagState: Int,
    // 设备是否处于休眠中
    val isSleep: Boolean,
)