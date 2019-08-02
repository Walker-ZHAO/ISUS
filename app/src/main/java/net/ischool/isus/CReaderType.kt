package net.ischool.isus

/**
 * 读卡器类型(通用)
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2019-06-24
 */
class CReaderType {
    companion object {
        @JvmField val NONE = -1             /** 无内置刷卡器 **/
        @JvmField val AUTO = 0              /** 自动选择（兼容老版本），新版无效 **/
        @JvmField val T_U1 = 1              /** USB，纯IC,模拟键盘10进制卡号（0-9） **/
        @JvmField val T_U2 = 2              /** USB，身份证IC二合一，华大HD-100 **/
        @JvmField val T_U3 = 3              /** USB，身份证，因纳伟盛刷卡器 **/
        @JvmField val T_U4 = 8              /** USB，纵横六合，M1卡 **/
        @JvmField val T_S1 = 4              /** 串口，纯IC，ttyS1，115220波特率，8字节长度:0x02 四字节卡号（ASCII） 0x0D 0x0A 0x03(绵竹) **/
        @JvmField val T_S2 = 5              /** 串口，纯IC，ttyS0，19200波特率，12字节长度，7-10字节为卡号（木兰） **/
        @JvmField val T_S3 = 6              /** 串口，纯IC，纵横六合 **/
        @JvmField val T_S4 = 7              /** 串口，纯IC，ttyS3，9600波特率，世纪凯城 **/
    }
}