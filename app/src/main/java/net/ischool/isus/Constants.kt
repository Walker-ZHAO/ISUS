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
val END_POINT by lazy { "${if (ISUS.instance.se) "https://cc" else "http://cdn.schools"}.${ISUS.instance.domain}/${if (ISUS.instance.se) "www/" else ""}" }

// RSA密钥部分口令
val CONFIG_RSA_PASS = "6f7084632667a86cbcba2ff128d46440"

// 网络访问结果
const val RESULT_OK = 0

// 广播的command字段
const val EXTRA_VERSION = "version"
const val EXTRA_CMD = "cmd"
const val EXTRA_CMDB_ID = "cmdbid"
const val EXTRA_ARGS = "args"

// 广播的command的action
const val ACTION_COMMAND = "net.ischool.isus.command"
// 广播RabbitMQ连接状态的action
const val ACTION_QUEUE_STATE_CHANGE = "net.ischool.isus.queue_state"

// Syslog 的 UDP 端口
const val UDP_PORT = 514

// RabbitMQ 相关配置
// 通用配置
const val MQ_VHOST = "/"
// 非安全增强模式配置
val MQ_DOMAIN by lazy { "cdn.schools.${ISUS.instance.domain}" }
const val MQ_PORT = 5672
const val MQ_USERNAME = "equipment"
const val MQ_PASSWORD = "1835ac0a6b749651efa42dd4e09e625a"
const val MQ_EXCHANGE_NAME = "equipment"
const val MQ_EXCHANGE_TYPE = "topic"
const val MQ_ROUTING_KEY_PREFIX = "$MQ_EXCHANGE_NAME.cmdb"

// 安全增强模式配置
val MQ_DOMAIN_SE by lazy { "cdnmq.i-school.net" }
const val MQ_PORT_SE = 5671
const val MQ_ROUTING_KEY_SE = "sync.schools.#"
const val MQ_ROUTING_KEY_USER = "sync.schools.user"
const val MQ_ROUTING_KEY_COMET = "sync.schools.sys.comet"

// 头像缓存目录
val AVATAR_CACHE_DIR = "${ISUS.instance.context.filesDir.absolutePath}/img"

// 仅供ISUS内部输出日志时，使用的TAG
internal const val LOG_TAG = "ISUS"

// Syslog日志上报分类
// RabbitMQ
internal const val SYSLOG_CATEGORY_RABBITMQ = "RabbitMQ"