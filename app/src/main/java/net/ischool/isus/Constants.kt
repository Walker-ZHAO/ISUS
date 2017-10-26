@file:JvmName("Constants")
package net.ischool.isus

/**
 * Description
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2017/10/23
 */

// 默认的服务器地址
val END_POINT = "https://www.i-school.net/"

// 网络访问结果
val RESULT_OK = 0

// 广播的command字段
val EXTRA_VERSION = "version"
val EXTRA_CMD = "cmd"
val EXTRA_CMDB_ID = "cmdbid"
val EXTRA_ARGS = "args"

// 广播的command的action
val ACTION_COMMAND = "net.ischool.isus.command"

// Syslog 的 UDP 端口
val UDP_PORT = 514