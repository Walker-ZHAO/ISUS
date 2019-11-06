package net.ischool.isus

/**
 * 显示模式（安全控制台）
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2019/2/13
 */
class DisplayModel {
    companion object {
        @JvmField val LEGACY = "1"  /** 单屏显示，初版大屏UI **/
        @JvmField val PAD = "2"     /** 单屏显示，Pad适配UI **/
        @JvmField val ASSIST = "3"  /** 双屏显示，主屏：Pad适配UI；副屏：Pad适配对应副屏UI **/
        @JvmField val PAD_SINGLE_GATE = "4" /** 单屏显示，Pad适配UI（纯闸机通道） **/
    }
}