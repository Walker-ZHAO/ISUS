package net.ischool.isus

/**
 * 设备类型
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2017/9/13
 */
class DeviceType {
    companion object {
        @JvmField val SECURITY     = 2     /** 校园安全控制台 **/
        @JvmField val BADGE        = 11    /** 班牌 **/
        @JvmField val VISION_PHONE = 12    /** 可视电话 **/
        @JvmField val SELFIE       = 13    /** 自拍机 **/

        /**
         * 获取设备名称
         *
         * @param type  设备类型
         */
        @JvmStatic fun getDeviceName(type: Int): String = with(ISUS.instance.context) {
            when (type) {
                VISION_PHONE -> getString(R.string.device_type_vision_phone)
                SELFIE -> getString(R.string.device_type_selfie)
                BADGE -> getString(R.string.device_type_badge)
                SECURITY -> getString(R.string.device_type_security)
                else -> ""
            }
        }
    }
}