package net.ischool.isus

/**
 * 门禁控制器类型
 *
 * Author: walker
 * Email: zhaocework@gmail.com
 * Date: 2023/12/4
 */
object EntranceGuardType {
    const val NONE = 0;     /** 无门禁控制器 **/
    const val T_S1 = 1;     /** 串口，纯IC，/dev/ttyS3，9600波特率，大华32寸卡得读卡器 **/
}