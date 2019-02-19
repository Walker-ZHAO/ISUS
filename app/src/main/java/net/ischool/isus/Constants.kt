@file:JvmName("Constants")
package net.ischool.isus

/**
 * Description
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2017/10/23
 */

const val DEFAULT_DOMAIN = "i-school.net"

// 默认的服务器地址
val END_POINT by lazy { "http://cdn.schools.${ISUS.instance.domain}/" }

// 网络访问结果
const val RESULT_OK = 0

// 广播的command字段
const val EXTRA_VERSION = "version"
const val EXTRA_CMD = "cmd"
const val EXTRA_CMDB_ID = "cmdbid"
const val EXTRA_ARGS = "args"

// 广播的command的action
const val ACTION_COMMAND = "net.ischool.isus.command"

// Syslog 的 UDP 端口
const val UDP_PORT = 514

// RabbitMQ 相关配置
val MQ_DOMAIN by lazy { "cdn.schools.${ISUS.instance.domain}" }
const val MQ_PORT = 5672
const val MQ_USERNAME = "equipment"
const val MQ_PASSWORD = "1835ac0a6b749651efa42dd4e09e625a"
const val MQ_VHOST = "/"
const val MQ_EXCHANGE_NAME = "equipment"
const val MQ_EXCHANGE_TYPE = "topic"
const val MQ_ROUTING_KEY_PREFIX = "$MQ_EXCHANGE_NAME.cmdb"