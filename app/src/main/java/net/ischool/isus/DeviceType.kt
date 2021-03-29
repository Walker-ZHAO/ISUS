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
        const val SECURITY     = 2     /** 校园安全控制台 **/
        const val BADGE        = 11    /** 班牌 **/
        const val VISION_PHONE = 12    /** 可视电话 **/
        const val SELFIE       = 13    /** 自拍机 **/
        const val SWIPE        = 14    /** 刷卡机 **/

        const val BADGE_ID = "10016"        /** 班牌标识，用于状态上报 **/
        const val SECURITY_ID = "10017"     /** 校园安全控制台标识，用于状态上报 **/
        const val VISION_PHONE_ID = "10018" /** 可视电话标识，用于状态上报 **/
        const val SELFIE_ID = "10021"       /** 自拍机 **/

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
                SWIPE -> getString(R.string.device_type_swipe)
                else -> ""
            }
        }

        @JvmStatic fun getDeviceTypeId(type: Int): String = when (type) {
            VISION_PHONE -> VISION_PHONE_ID
            BADGE -> BADGE_ID
            SECURITY -> SECURITY_ID
            SELFIE -> SELFIE_ID
            else -> ""
        }
    }
}