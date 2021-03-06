package net.ischool.isus

/**
 * 考勤模式（班牌）
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2019/4/17
 */
class AttendModel {
    companion object {
        const val NONE = 0           /** 未设置 **/
        const val BILLBOARD = 1      /** 班牌考勤 **/
        const val GATE = 2          /** 校门考勤 **/
    }
}