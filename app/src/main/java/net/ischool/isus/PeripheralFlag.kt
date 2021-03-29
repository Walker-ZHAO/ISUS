package net.ischool.isus

/**
 * 外设标志位（班牌）
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2019/4/17
 */
class PeripheralFlag {
    companion object {
        const val NONE = 0      /** 无外设 **/
        const val RF = 1        /** 读卡器 **/
        const val FACE_ID = 2  /** 人脸识别 **/
    }
}