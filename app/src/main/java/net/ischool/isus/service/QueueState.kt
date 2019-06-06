package net.ischool.isus.service

/**
 * RabbitMQ 连接状态枚举
 *
 * Author: Walker
 * Email: zhaocework@gmail.com
 * Date: 2019-06-06
 */
enum class QueueState {
    STATE_BLOCK,    // 未连接
    STATE_STANDBY,  // 已连接
}