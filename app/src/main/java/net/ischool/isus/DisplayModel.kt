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
        const val LEGACY = "1"  /** 单屏显示，初版大屏UI；**/
        const val PAD = "2"     /** 单屏显示，Pad适配UI **/
        const val ASSIST = "3"  /** 双屏显示，主屏：Pad适配UI；副屏：Pad适配对应副屏UI **/
        const val PAD_SINGLE_GATE = "4" /** 单屏显示，Pad适配UI（纯闸机通道） **/
        const val HYBRID = "5" /** 单屏显示，左侧栏：校园信息及时间；主页：Web页面 **/

        /** 班牌显示模式 **/
        const val NORMAL = "1"  /** 传统模式：主页+多模块可配置入口 **/
        const val HUGE_SCREEN = "2" /** 单页面展示学校通知公告+学生家长留言 **/
    }
}