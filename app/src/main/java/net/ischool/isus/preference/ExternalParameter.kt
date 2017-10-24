package net.ischool.isus.preference

import net.ischool.isus.ISUS
import net.ischool.isus.R

/**
 * 额外参数
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2017/9/21
 */
class ExternalParameter {
    companion object {
        val EXP_FACE_SERVICE_INT    = "faceServerInt"   /** 人脸识别服务器内网IP **/
        val EXP_FACE_SERVICE_EXT    = "faceServerExt"   /** 人脸识别服务器公网IP **/
        val EXP_VOIP_GATEWAY        = "VoIPGW"          /** 语音网关 **/
        val EXP_VOIP_STUN           = "VoIPSTUN"        /** 语音网关转发设备 **/
        val EXP_SELF_LAN_IP         = "selfLanIP"       /** 设备内网IP **/
        val EXP_SELF_WAN_IP         = "selfWanIP"       /** 设备公网IP **/
        val EXP_SELF_M_KEY          = "selfMKey"        /** 用于管理员登录设备的密码 **/
        val EXP_SELF_S_KEY          = "selfSKey"        /** 用于API签名的密钥**/

        /**
         * 获取额外参数的名称
         *
         * @param key   额外参数key字段
         */
        fun getEXPName(key: String): String = with(ISUS.instance.context) {
            when (key) {
                EXP_FACE_SERVICE_INT    -> ISUS.instance.context.getString(R.string.external_face_server_int)
                EXP_FACE_SERVICE_EXT    -> getString(R.string.external_face_server_ext)
                EXP_VOIP_GATEWAY        -> getString(R.string.external_voip_gateway)
                EXP_VOIP_STUN           -> getString(R.string.external_voip_stun)
                EXP_SELF_LAN_IP         -> getString(R.string.external_self_lan_ip)
                EXP_SELF_WAN_IP         -> getString(R.string.external_self_wan_ip)
                EXP_SELF_M_KEY          -> getString(R.string.external_m_key)
                EXP_SELF_S_KEY          -> getString(R.string.external_s_key)
                else -> ""
            }
        }
    }
}