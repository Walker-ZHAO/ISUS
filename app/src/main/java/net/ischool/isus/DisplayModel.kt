package net.ischool.isus

/**
 * 显示模式（安全控制台&班牌）
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2019/2/13
 */
class DisplayModel {
    companion object {
        /** 校园控制台显示模式 **/
        @JvmField val LEGACY = "1"  /** 单屏显示，初版大屏UI；**/
        @JvmField val PAD = "2"     /** 单屏显示，Pad适配UI **/
        @JvmField val ASSIST = "3"  /** 双屏显示，主屏：Pad适配UI；副屏：Pad适配对应副屏UI **/
        @JvmField val PAD_SINGLE_GATE = "4" /** 单屏显示，Pad适配UI（纯闸机通道） **/

        /** 班牌显示模式 **/
        @JvmField val NORMAL = "1"  /** 传统模式：主页+多模块可配置入口 **/
        @JvmField val HUGE_SCREEN = "2" /** 单页面展示学校通知公告+学生家长留言 **/
    }
}