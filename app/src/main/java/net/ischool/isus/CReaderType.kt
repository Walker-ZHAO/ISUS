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
        const val NONE = -1             /** 无内置刷卡器 **/
        const val AUTO = 0              /** 自动选择（兼容老版本），新版无效 **/
        const val T_U1 = 1              /** USB，纯IC,模拟键盘10进制卡号（0-9） **/
        const val T_U2 = 2              /** USB，身份证IC二合一，华大HD-100 **/
        const val T_U3 = 3              /** USB，身份证，因纳伟盛刷卡器 **/
        const val T_U4 = 8              /** USB，纵横六合，M1卡 **/
        const val T_U5 = 9              /** 海康威视读卡器 **/
        const val T_S1 = 4              /** 串口，纯IC，ttyS1，115220波特率，8字节长度:0x02 四字节卡号（ASCII） 0x0D 0x0A 0x03(绵竹) **/
        const val T_S2 = 5              /** 串口，纯IC，ttyS0，19200波特率，12字节长度，7-10字节为卡号（木兰） **/
        const val T_S3 = 6              /** 串口，纯IC，纵横六合 **/
        const val T_S4 = 7              /** 串口，纯IC，ttyS3，9600波特率，世纪凯城 **/
        const val T_S5 = 11             /** 串口，纯IC，ttyS3，9600波特率，华瑞安T6/C6（明博），7字节长度: 0x55 0xAA 四字节卡号（一字节对应两位字符串） 数据校验位（卡号异或和） **/
        const val T_S6 = 12             /** 串口，纯IC，ttyS3，9600波特率，触沃（龙江路乐培定制），4字节长度，字节反转后为卡号 **/
        const val T_S7 = 13             /** 串口，纯IC，/dev/ttyS3 19200波特率，鸿合21.5，7字节长度:  0xAA 0x55 四字节卡号（字节反转后为卡号） 数据校验位（卡号异或和） **/
        const val T_S8 = 14             /** 串口，纯IC，/dev/ttyS3，9600波特率，4字节长度，字节反转后为卡号，大华32寸 **/
        const val T_S9 = 15             /** 串口，纯IC，/dev/ttyS3，9600波特率，4字节长度，字节反转后为卡号，视美泰（成都元素科技） **/
        const val T_N1 = 10             /** NFC，M1卡，MifareClassic协议，希沃 **/
    }
}