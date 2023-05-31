package net.ischool.isus.model

/**
 * 报警信息
 *
 * Author: walker
 * Email: zhaocework@gmail.com
 * Date: 2023/5/30
 */
data class AlarmInfo(val type: Int, val ts: Long, val reason: String, val diag: String, val contact: String)