package net.ischool.isus

/**
 * 硬件设备ID
 *
 * 详细取值参考文档：http://192.168.0.20/products/ZXHW.md
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2019-11-19
 */
class DeviceId {
    companion object {
        @JvmField val CW_TD32_ZX10B     = "HWEM001" // 触沃TD32-ZX10B; 32寸、HD-100身份证、IC二合一刷卡器，Android5.1，HDMI输出; 校园安全控制台
        @JvmField val CW_BD215_ZX10B_ID = "HWEM002" // 触沃BD215-ZX10B; 21.5寸、HD-100身份证刷卡器，Android5.1，HDMI输出; 校园安全控制台
        @JvmField val CW_BD215_ZX10B    = "HWEM003" // 触沃BD215-ZX10B; 21.5寸、IC卡刷卡器，Android5.1，HDMI输出; 校园安全控制台&电子班牌
        @JvmField val CW_BD215_ZX11B    = "HWEM004" // 触沃BD215-ZX11B; 21.5寸、IC卡刷卡器，Android5.1，200万像素; 考勤一体机
        @JvmField val ML_LSKQJ          = "HWEM005" // 木兰ML-LSKQJ; 非触屏、IC卡刷卡器，Android5.1; 考勤一体机
        @JvmField val DH_L22            = "HWEM006" // 大华DHL22; IC卡刷卡器，Android 5.1; 电子班牌
        @JvmField val ZHLW_HW           = "HWEM007" // 纵横六合; IC卡刷卡器; 电子班牌
        @JvmField val HK_DS_D6122TH_BI  = "HWEM008" // 海康DS-D6122TH-B/I; 身份证+IC卡; 校园安全控制台
        @JvmField val HK_DS_D6122TL_B   = "HWEM009" // 海康DS-D6122TL-B; IC卡刷卡器; 电子班牌
        @JvmField val SJKC_HW           = "HWEM010" // 世纪凯城; IC卡刷卡器; 电子班牌
        @JvmField val CW_GD133_ZX11B    = "HWEM011" // 触沃GD133-ZX11B; 13.3寸、IC刷卡器，Android5.1; 考勤一体机
        @JvmField val CW_BD215_ZX34B    = "HWEM012" // 触沃BD215-ZX34B; 21.5寸、HD-100身份证刷卡器，Android5.1，无HDMI; 校园安全控制台&电子班牌
        @JvmField val MI_PAD4           = "HWEM013" // 小米Pad4,8寸; 8寸,wifi,4GB,64GB; 校园安全控制台
        @JvmField val TB_TPS580         = "HWEM014" // 天波TPS580; 非触屏、IC卡刷卡器，Android5.1; 考勤一体机
    }
}