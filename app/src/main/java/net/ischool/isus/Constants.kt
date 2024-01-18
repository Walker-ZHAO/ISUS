@file:JvmName("Constants")
package net.ischool.isus

import android.net.Uri
import net.ischool.isus.preference.PreferenceManager

/**
 * Description
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2017/10/23
 */

// 默认的服务器地址（普通模式）
const val DEFAULT_API_HOST = "http://cdn.schools.i-school.net"
// 服务器访问路径（普通模式）
const val API_PATH = "/"

// 默认的服务器地址（安全增强模式）
const val DEFAULT_SE_API_HOST = "https://cc.i-school.net"
// 服务器访问路径（安全增强模式）
const val SE_API_PATH = "/www/"

// 默认的证书下载服务器地址
const val DEFAULT_PEM_DOWNLOAD_HOST = "http://update.i-school.net"
// 证书下载路径
const val PEM_DOWNLOAD_PATH = "/zxedu-system-images/schools/"

// 默认的附件上传服务器地址
const val  DEFAULT_ATT_HOST = "https://att.i-school.net/"

// 默认的Web页服务器地址
const val  DEFAULT_STATIC_HOST = "https://static.i-school.net/"

// 默认的服务器地址
val END_POINT by lazy { PreferenceManager.instance.getPlatformApi() }

// RSA密钥部分口令
const val CONFIG_RSA_PASS = "6f7084632667a86cbcba2ff128d46440"

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
// 通用配置，根据初始化时配置的MQ地址解析得到
val MQ_DOMAIN: String by lazy { Uri.parse(PreferenceManager.instance.getPlatformMq()).host ?: "" }
val MQ_PORT: Int by lazy { Uri.parse(PreferenceManager.instance.getPlatformMq()).port }
val MQ_VHOST: String by lazy { Uri.parse(PreferenceManager.instance.getPlatformMq()).pathSegments.joinToString("/").ifEmpty { "/" } }
val MQ_NEED_PEM: Boolean by lazy { Uri.parse(PreferenceManager.instance.getPlatformMq()).scheme?.endsWith('s') ?: false }
val MQ_USERNAME: String by lazy { Uri.parse(PreferenceManager.instance.getPlatformMq()).userInfo?.split(":")?.first() ?: MQ_DEFAULT_USERNAME }
val MQ_PASSWORD: String by lazy { Uri.parse(PreferenceManager.instance.getPlatformMq()).userInfo?.split(":")?.get(1) ?: MQ_DEFAULT_PASSWORD }

// 非安全增强模式配置
const val MQ_DEFAULT_DOMAIN = "cdn.schools.i-school.net"
const val MQ_DEFAULT_POST = 5672
const val MQ_DEFAULT_USERNAME = "equipment"
const val MQ_DEFAULT_PASSWORD = "1835ac0a6b749651efa42dd4e09e625a"
const val MQ_EXCHANGE_NAME = "equipment"
const val MQ_EXCHANGE_TYPE = "topic"
const val MQ_ROUTING_KEY_PREFIX = "$MQ_EXCHANGE_NAME.cmdb"

// 安全增强模式配置
const val MQ_DEFAULT_SE_DOMAIN = "cdnmq.i-school.net"
const val MQ_DEFAULT_SE_POST = 5671
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
// EventSource
internal const val SYSLOG_CATEGORY_SSE = "SSE"