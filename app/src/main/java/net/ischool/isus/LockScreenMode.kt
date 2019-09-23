package net.ischool.isus

/**
 * 锁屏模式
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2019-09-23
 */
class LockScreenMode {
    companion object {
        @JvmField val NONE = 0      // 不锁屏
        @JvmField val PASSWORD = 1  // 密码解锁
        @JvmField val CARD = 2      // 刷卡解锁
    }
}