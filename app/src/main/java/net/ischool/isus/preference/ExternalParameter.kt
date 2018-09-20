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
        const val EXP_SYSLOG              = "syslog"          /** syslog上传地址 **/
        const val EXP_SCHOOL_NAME         = "schoolName"      /** 学校名 **/
        const val EXP_VOIP_GATEWAY        = "VoIPGW"          /** 语音网关 **/
        const val EXP_VOIP_STUN           = "VoIPSTUN"        /** 语音网关转发设备 **/
        const val EXP_SELF_LAN_IP         = "selfLanIP"       /** 设备内网IP **/
        const val EXP_SELF_WAN_IP         = "selfWanIP"       /** 设备公网IP **/
        const val EXP_SELF_M_KEY          = "selfMKey"        /** 用于管理员登录设备的密码 **/
        const val EXP_SELF_S_KEY          = "selfSKey"        /** 用于API签名的密钥**/

        /**
         * 获取额外参数的名称
         *
         * @param key   额外参数key字段
         */
        fun getEXPName(key: String): String? = with(ISUS.instance.context) {
            when (key) {
                EXP_SYSLOG              -> ISUS.instance.context.getString(R.string.external_syslog)
                EXP_SCHOOL_NAME         -> ISUS.instance.context.getString(R.string.external_school_name)
                EXP_VOIP_GATEWAY        -> getString(R.string.external_voip_gateway)
                EXP_VOIP_STUN           -> getString(R.string.external_voip_stun)
                EXP_SELF_LAN_IP         -> getString(R.string.external_self_lan_ip)
                EXP_SELF_WAN_IP         -> getString(R.string.external_self_wan_ip)
                EXP_SELF_M_KEY          -> getString(R.string.external_m_key)
                EXP_SELF_S_KEY          -> getString(R.string.external_s_key)
                else -> null
            }
        }
    }
}