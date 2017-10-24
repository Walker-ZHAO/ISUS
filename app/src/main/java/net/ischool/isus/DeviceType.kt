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
        @JvmField val ROUTER       = 3     /** 路由器 **/
        @JvmField val SERVER       = 4     /** 服务器 **/
        @JvmField val CAMERA       = 5     /** 摄像头 **/
        @JvmField val ATTENDANCE   = 6     /** 宿舍考勤机 **/
        @JvmField val SIP_GATEWAY  = 7     /** 语音网关 **/
        @JvmField val VISION_PHONE = 8     /** 可视电话 **/
        @JvmField val SELFIE       = 9     /** 自拍机 **/
        @JvmField val SECURITY     = 12    /** 保安室警报 **/

        /**
         * 获取设备名称
         *
         * @param type  设备类型
         */
        @JvmStatic fun getDeviceName(type: Int): String = with(ISUS.instance.context) {
            when (type) {
                ROUTER -> getString(R.string.device_type_router)
                SERVER -> getString(R.string.device_type_server)
                CAMERA -> getString(R.string.device_type_camera)
                ATTENDANCE -> getString(R.string.device_type_attendance)
                SIP_GATEWAY -> getString(R.string.device_type_sip_gateway)
                VISION_PHONE -> getString(R.string.device_type_vision_phone)
                SELFIE -> getString(R.string.device_type_selfie)
                SECURITY -> getString(R.string.device_type_security)
                else -> ""
            }
        }
    }
}